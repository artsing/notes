/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.server.quorum;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.UnresolvedAddressException;
import java.util.Enumeration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a connection manager for leader election using TCP. It
 * maintains one connection for every pair of servers. The tricky part is to
 * guarantee that there is exactly one connection for every pair of servers that
 * are operating correctly and that can communicate over the network.
 *
 * If two servers try to start a connection concurrently, then the connection
 * manager uses a very simple tie-breaking mechanism to decide which connection
 * to drop based on the IP addressed of the two parties.
 *
 * For every peer, the manager maintains a queue of messages to send. If the
 * connection to any particular peer drops, then the sender thread puts the
 * message back on the list. As this implementation currently uses a queue
 * implementation to maintain messages to send to another peer, we add the
 * message to the tail of the queue, thus changing the order of messages.
 * Although this is not a problem for the leader election, it could be a problem
 * when consolidating peer communication. This is to be verified, though.
 *
 */

class QuorumCnxManager {
    private static final Logger LOG = LoggerFactory.getLogger(QuorumCnxManager.class);

    /**
     * Maximum capacity of thread queues
     * 接收线程队列的最大容量
     */
    private static final int RECV_CAPACITY = 100;

    /**
     * Initialized to 1 to prevent sending
     * stale notifications to peers
     */
    private static final int SEND_CAPACITY = 1;

    /**
     * 上限报文大小
     */
    private static final int PACKET_MAX_SIZE = 1024 * 1024;

    /**
     * Negative counter for observer server ids.
     * 观察者计数器：统计observer数
     */
    private long observerCounter = -1;

    /**
     * Connection time out value in milliseconds
     * 连接超时时间：默认5秒
     */
    private int cnxTimeout = 5000;

    /**
     * Local IP address
     * 本地 server 自身：主要使用其 IP 地址
     */
    private final QuorumPeer self;

    /**
     * Mapping from Peer to Thread number
     * 发送线程  一个server一个线程，根据sid 获取发送队列，从中取数据发送
     */
    private final ConcurrentHashMap<Long, SendWorker> senderWorkerMap;

    /**
     * 发送队列  一个server对应一个队列
     */
    private final ConcurrentHashMap<Long, ArrayBlockingQueue<ByteBuffer>> queueSendMap;

    /**
     * 最后一次发送的数据 一个server记录一个最后一次发送的数据
     */
    private final ConcurrentHashMap<Long, ByteBuffer> lastMessageSent;

    /**
     * Reception queue
     * 接收队列，存放所有server发送过来的消息
     */
    private final ArrayBlockingQueue<Message> recvQueue;

    /**
     * Object to synchronize access to recvQueue
     */
    private final Object recvQLock = new Object();

    /**
     * Shutdown flag
     * 关闭标志
     */
    private volatile boolean shutdown = false;

    /**
     * Listener thread
     * 监听线程：监听本地端口，接收数据
     */
    private final Listener listener;

    /**
     * Counter to count worker threads
     * 计数器：统计当前工作线程数
     */
    private AtomicInteger threadCnt = new AtomicInteger(0);

    /**
     * 消息
     * sid server id
     * buffer 消息内容
     */
    static class Message {
        Message(ByteBuffer buffer, long sid) {
            this.buffer = buffer;
            this.sid = sid;
        }

        ByteBuffer buffer;
        long sid;
    }

    QuorumCnxManager(QuorumPeer self) {
        this.recvQueue = new ArrayBlockingQueue<>(RECV_CAPACITY);
        this.queueSendMap = new ConcurrentHashMap<>();
        this.senderWorkerMap = new ConcurrentHashMap<>();
        this.lastMessageSent = new ConcurrentHashMap<>();

        String cnxToValue = System.getProperty("zookeeper.cnxTimeout");
        if(cnxToValue != null){
            this.cnxTimeout = new Integer(cnxToValue);
        }

        this.self = self;

        // Starts listener thread that waits for connection requests
        listener = new Listener();
    }

    Listener getListener() {
        return listener;
    }

    /**
     * If this server has initiated the connection, then it gives up on the
     * connection if it loses challenge. Otherwise, it keeps the connection.
     * 如果这个服务器已经初始化连接，如果 sid 大于自身sid 放弃连接；否则保持连接
     */
    private void initiateConnection(Socket sock, Long sid) {
        DataOutputStream dataOut;
        try {
            // Sending id and challenge
            // 发送 sid 进行 pk， 根据 sid大小决定胜负
            dataOut = new DataOutputStream(sock.getOutputStream());
            dataOut.writeLong(self.getId());
            dataOut.flush();
        } catch (IOException e) {
            LOG.warn("Ignoring exception reading or writing challenge: ", e);
            closeSocket(sock);
            return;
        }

        // If lost the challenge, then drop the new connection
        // 如果 pk 失败
        if (sid > self.getId()) {
            LOG.info("Have smaller server identifier, so dropping the " +
                     "connection: (" + sid + ", " + self.getId() + ")");
            closeSocket(sock);
            // Otherwise proceed with the connection
        } else {
            // 初始化并启动发送线程和接收线程
            initAndStartWorkers(sock, sid);

        }
    }

    /**
     * If this server receives a connection request, then it gives up on the new
     * connection if it wins. Notice that it checks whether it has a connection
     * to this server already or not. If it does, then it sends the smallest
     * possible long value to lose the challenge.
     *
     */
    private void receiveConnection(Socket sock) {
        long sid;

        try {
            // Read server id
            DataInputStream din = new DataInputStream(sock.getInputStream());
            sid = din.readLong();
            if (sid == QuorumPeer.OBSERVER_ID) {
                /*
                 * Choose identifier at random. We need a value to identify
                 * the connection.
                 */

                sid = observerCounter--;
                LOG.info("Setting arbitrary identifier to observer: " + sid);
            }
        } catch (IOException e) {
            closeSocket(sock);
            LOG.warn("Exception reading or writing challenge: " + e.toString());
            return;
        }

        //If wins the challenge, then close the new connection.
        if (sid < self.getId()) {
            /*
             * This replica might still believe that the connection to sid is
             * up, so we have to shut down the workers before trying to open a
             * new connection.
             */
            SendWorker sw = senderWorkerMap.get(sid);
            if (sw != null) {
                sw.finish();
            }

            /*
             * Now we start a new connection
             */
            LOG.debug("Create new connection to server: " + sid);
            closeSocket(sock);
            connectOne(sid);

            // Otherwise start worker threads to receive data.
        } else {

            // 初始化并启动发送线程和接收线程
            initAndStartWorkers(sock, sid);
        }
    }

    /**
     * 初始化并启动发送线程和接收线程
     * @param sock socket
     * @param sid sever id
     */
    private void initAndStartWorkers(Socket sock, Long sid) {
        // 创建发送线程
        SendWorker sw = new SendWorker(sock, sid);
        // 创建接收线程
        RecvWorker rw = new RecvWorker(sock, sid, sw);
        sw.setRecv(rw);

        // 从map中获取发送线程
        SendWorker vsw = senderWorkerMap.get(sid);
        // 如果获取到发送线程则结束就线程
        if(vsw != null)
            vsw.finish();

        // 将新创建的发送线程放入map
        senderWorkerMap.put(sid, sw);

        // 如果发送队列没有创建则创建发送队列并放入map
        if (!queueSendMap.containsKey(sid)) {
            queueSendMap.put(sid, new ArrayBlockingQueue<>(
                    SEND_CAPACITY));
        }

        // 启动发送线程
        sw.start();
        // 启动接收线程
        rw.start();
    }

    /**
     * Processes invoke this message to queue a message to send. Currently,
     * only leader election uses it.
     */
    void toSend(Long sid, ByteBuffer b) {
        /*
         * If sending message to myself, then simply enqueue it (loopback).
         */
        if (self.getId() == sid) {
             b.position(0);
             addToRecvQueue(new Message(b.duplicate(), sid));
            /*
             * Otherwise send to the corresponding thread to send.
             */
        } else {
             /*
              * Start a new connection if doesn't have one already.
              */
             if (!queueSendMap.containsKey(sid)) {
                 ArrayBlockingQueue<ByteBuffer> bq = new ArrayBlockingQueue<>(
                         SEND_CAPACITY);
                 queueSendMap.put(sid, bq);
                 addToSendQueue(bq, b);

             } else {
                 ArrayBlockingQueue<ByteBuffer> bq = queueSendMap.get(sid);
                 if(bq != null){
                     addToSendQueue(bq, b);
                 } else {
                     LOG.error("No queue for server " + sid);
                 }
             }
             connectOne(sid);

        }
    }

    /**
     * Try to establish a connection to server with id sid.
     * 根据 server id 建立一个连接
     *  @param sid  server id
     */
    private synchronized void connectOne(long sid){
        if (senderWorkerMap.get(sid) == null){
            InetSocketAddress electionAddr;
            if (self.quorumPeers.containsKey(sid)) {
                electionAddr = self.quorumPeers.get(sid).electionAddr;
            } else {
                LOG.warn("Invalid server id: " + sid);
                return;
            }
            try {

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Opening channel to server " + sid);
                }
                Socket sock = new Socket();
                setSockOpts(sock);
                sock.connect(self.getView().get(sid).electionAddr, cnxTimeout);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Connected to server " + sid);
                }
                initiateConnection(sock, sid);
            } catch (UnresolvedAddressException e) {
                // Sun doesn't include the address that causes this
                // exception to be thrown, also UAE cannot be wrapped cleanly
                // so we log the exception in order to capture this critical
                // detail.
                LOG.warn("Cannot open channel to " + sid
                        + " at election address " + electionAddr, e);
                throw e;
            } catch (IOException e) {
                LOG.warn("Cannot open channel to " + sid
                        + " at election address " + electionAddr,
                        e);
            }
        } else {
            LOG.debug("There is a connection already for server " + sid);
        }
    }


    /**
     * Try to establish a connection with each server if one
     * doesn't exist.
     * 和全部 server 建立连接
     */
    void connectAll(){
        long sid;
        for(Enumeration<Long> en = queueSendMap.keys();
            en.hasMoreElements();){
            sid = en.nextElement();
            connectOne(sid);
        }
    }


    /**
     * Check if all queues are empty, indicating that all messages have been delivered.
     *
     */
    boolean haveDelivered() {
        for (ArrayBlockingQueue<ByteBuffer> queue : queueSendMap.values()) {
            LOG.debug("Queue size: " + queue.size());
            if (queue.size() == 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Flag that it is time to wrap up all activities and interrupt the listener.
     * 标记是时候结束所有活动并中断监听器。
     */
    void halt() {
        shutdown = true;
        LOG.debug("Halting listener");
        listener.halt();

        softHalt();
    }

    /**
     * A soft halt simply finishes workers.
     * 慢宕所有工作线程
     */
    private void softHalt() {
        for (SendWorker sw : senderWorkerMap.values()) {
            LOG.debug("Halting sender: " + sw);
            sw.finish();
        }
    }

    /**
     * Helper method to set socket options.
     *
     * @param sock
     *            Reference to socket
     */
    private void setSockOpts(Socket sock) throws SocketException {
        sock.setTcpNoDelay(true);
        sock.setSoTimeout(self.tickTime * self.syncLimit);
    }

    /**
     * Helper method to close a socket.
     *
     * @param sock
     *            Reference to socket
     */
    private void closeSocket(Socket sock) {
        try {
            sock.close();
        } catch (IOException ie) {
            LOG.error("Exception while closing", ie);
        }
    }

    /**
     * Thread to listen on some port
     */
    public class Listener extends Thread {

        volatile ServerSocket ss = null;

        /**
         * Sleeps on accept().
         */
        @Override
        public void run() {
            int numRetries = 0;
            while((!shutdown) && (numRetries < 3)){
                try {
                    ss = new ServerSocket();
                    ss.setReuseAddress(true);
                    int port = self.quorumPeers.get(self.getId()).electionAddr
                            .getPort();
                    InetSocketAddress addr = new InetSocketAddress(port);
                    LOG.info("My election bind port: " + addr.toString());
                    setName(self.quorumPeers.get(self.getId()).electionAddr
                            .toString());
                    ss.bind(addr);
                    while (!shutdown) {
                        Socket client = ss.accept();
                        setSockOpts(client);
                        LOG.info("Received connection request "
                                + client.getRemoteSocketAddress());
                        receiveConnection(client);
                        numRetries = 0;
                    }
                } catch (IOException e) {
                    LOG.error("Exception while listening", e);
                    numRetries++;
                    try {
                        ss.close();
                        Thread.sleep(1000);
                    } catch (IOException ie) {
                        LOG.error("Error closing server socket", ie);
                    } catch (InterruptedException ie) {
                        LOG.error("Interrupted while sleeping. " +
                                  "Ignoring exception", ie);
                    }
                }
            }
            LOG.info("Leaving listener");
            if (!shutdown) {
                LOG.error("As I'm leaving the listener thread, "
                        + "I won't be able to participate in leader "
                        + "election any longer: "
                        + self.quorumPeers.get(self.getId()).electionAddr);
            }
        }

        /**
         * Halts this listener thread.
         */
        void halt(){
            try{
                LOG.debug("Trying to close listener: " + ss);
                if(ss != null) {
                    LOG.debug("Closing listener: " + self.getId());
                    ss.close();
                }
            } catch (IOException e){
                LOG.warn("Exception when shutting down listener: " + e);
            }
        }
    }

    /**
     * Thread to send messages. Instance waits on a queue, and send a message as
     * soon as there is one available. If connection breaks, then opens a new
     * one.
     */
    class SendWorker extends Thread {
        Long sid;
        Socket sock;
        RecvWorker recvWorker;
        volatile boolean running = true;
        DataOutputStream outputStream;

        /**
         * An instance of this thread receives messages to send
         * through a queue and sends them to the server sid.
         *
         * @param sock
         *            Socket to remote peer
         * @param sid
         *            Server identifier of remote peer
         */
        SendWorker(Socket sock, Long sid) {
            super("SendWorker:" + sid);
            this.sid = sid;
            this.sock = sock;
            recvWorker = null;
            try {
                outputStream = new DataOutputStream(sock.getOutputStream());
            } catch (IOException e) {
                LOG.error("Unable to access socket output stream", e);
                closeSocket(sock);
                running = false;
            }
            LOG.debug("Address of remote peer: " + this.sid);
        }

        synchronized void setRecv(RecvWorker recvWorker) {
            this.recvWorker = recvWorker;
        }

        synchronized void finish() {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Calling finish for " + sid);
            }

            if(!running){
                /*
                 * Avoids running finish() twice.
                 */
                return;
            }

            running = false;
            closeSocket(sock);
            // channel = null;

            this.interrupt();
            if (recvWorker != null) {
                recvWorker.finish();
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Removing entry from senderWorkerMap sid=" + sid);
            }
            senderWorkerMap.remove(sid, this);
            threadCnt.decrementAndGet();
        }

        synchronized void send(ByteBuffer b) throws IOException {
            byte[] msgBytes = new byte[b.capacity()];
            try {
                b.position(0);
                b.get(msgBytes);
            } catch (BufferUnderflowException be) {
                LOG.error("BufferUnderflowException ", be);
                return;
            }
            outputStream.writeInt(b.capacity());
            outputStream.write(b.array());
            outputStream.flush();
        }

        @Override
        public void run() {
            threadCnt.incrementAndGet();
            try {
                /*
                 * If there is nothing in the queue to send, then we
                 * send the lastMessage to ensure that the last message
                 * was received by the peer. The message could be dropped
                 * in case self or the peer shutdown their connection
                 * (and exit the thread) prior to reading/processing
                 * the last message. Duplicate messages are handled correctly
                 * by the peer.
                 *
                 * If the send queue is non-empty, then we have a recent
                 * message than that stored in lastMessage. To avoid sending
                 * stale message, we should send the message in the send queue.
                 */
                ArrayBlockingQueue<ByteBuffer> bq = queueSendMap.get(sid);
                if (bq == null || isSendQueueEmpty(bq)) {
                   ByteBuffer b = lastMessageSent.get(sid);
                   if (b != null) {
                       LOG.debug("Attempting to send lastMessage to sid=" + sid);
                       send(b);
                   }
                }
            } catch (IOException e) {
                LOG.error("Failed to send last message. Shutting down thread.", e);
                this.finish();
            }

            try {
                while (running && !shutdown && sock != null) {

                    ByteBuffer b;
                    try {
                        ArrayBlockingQueue<ByteBuffer> bq = queueSendMap
                                .get(sid);
                        if (bq != null) {
                            b = pollSendQueue(bq);
                        } else {
                            LOG.error("No queue of incoming messages for " +
                                      "server " + sid);
                            break;
                        }

                        if(b != null){
                            lastMessageSent.put(sid, b);
                            send(b);
                        }
                    } catch (InterruptedException e) {
                        LOG.warn("Interrupted while waiting for message on queue",
                                e);
                    }
                }
            } catch (Exception e) {
                LOG.warn("Exception when using channel: for id " + sid + " my id = " +
                        self.getId() + " error = " + e);
            }
            this.finish();
            LOG.warn("Send worker leaving thread");
        }
    }

    /**
     * Thread to receive messages. Instance waits on a socket read. If the
     * channel breaks, then removes itself from the pool of receivers.
     */
    class RecvWorker extends Thread {
        Long sid;
        Socket sock;
        volatile boolean running = true;
        DataInputStream din;
        final SendWorker sw;

        RecvWorker(Socket sock, Long sid, SendWorker sw) {
            super("RecvWorker:" + sid);
            this.sid = sid;
            this.sock = sock;
            this.sw = sw;
            try {
                din = new DataInputStream(sock.getInputStream());
                // OK to wait until socket disconnects while reading.
                sock.setSoTimeout(0);
            } catch (IOException e) {
                LOG.error("Error while accessing socket for " + sid, e);
                closeSocket(sock);
                running = false;
            }
        }

        /**
         * Shuts down this worker
         *
         * @return void
         */
        synchronized void finish() {
            if(!running){
                /*
                 * Avoids running finish() twice.
                 */
                return;
            }
            running = false;

            this.interrupt();
            threadCnt.decrementAndGet();
        }

        @Override
        public void run() {
            threadCnt.incrementAndGet();
            try {
                while (running && !shutdown && sock != null) {
                    /*
                     * Reads the first int to determine the length of the
                     * message
                     */
                    int length = din.readInt();
                    if (length <= 0 || length > PACKET_MAX_SIZE) {
                        throw new IOException(
                                "Received packet with invalid packet: "
                                        + length);
                    }
                    /*
                     * Allocates a new ByteBuffer to receive the message
                     */
                    byte[] msgArray = new byte[length];
                    din.readFully(msgArray, 0, length);
                    ByteBuffer message = ByteBuffer.wrap(msgArray);
                    addToRecvQueue(new Message(message.duplicate(), sid));
                }
            } catch (Exception e) {
                LOG.warn("Connection broken for id " + sid + ", my id = " +
                        self.getId() + ", error = " , e);
            } finally {
                LOG.warn("Interrupting SendWorker");
                sw.finish();
                if (sock != null) {
                    closeSocket(sock);
                }
            }
        }
    }

    /**
     * Inserts an element in the specified queue. If the Queue is full, this
     * method removes an element from the head of the Queue and then inserts
     * the element at the tail. It can happen that the an element is removed
     * by another thread in {@link SendWorker #processMessage() processMessage}
     * method before this method attempts to remove an element from the queue.
     * This will cause {@link ArrayBlockingQueue#remove() remove} to throw an
     * exception, which is safe to ignore.
     *
     * Unlike {@link #addToRecvQueue(Message) addToRecvQueue} this method does
     * not need to be synchronized since there is only one thread that inserts
     * an element in the queue and another thread that reads from the queue.
     *
     * @param queue
     *          Reference to the Queue
     * @param buffer
     *          Reference to the buffer to be inserted in the queue
     */
    private void addToSendQueue(ArrayBlockingQueue<ByteBuffer> queue,
          ByteBuffer buffer) {
        if (queue.remainingCapacity() == 0) {
            try {
                queue.remove();
            } catch (NoSuchElementException ne) {
                // element could be removed by poll()
                LOG.debug("Trying to remove from an empty " +
                        "Queue. Ignoring exception " + ne);
            }
        }
        try {
            queue.add(buffer);
        } catch (IllegalStateException ie) {
            // This should never happen
            LOG.error("Unable to insert an element in the queue " + ie);
        }
    }

    /**
     * Returns true if queue is empty.
     * @param queue
     *          Reference to the queue
     * @return
     *      true if the specified queue is empty
     */
    private boolean isSendQueueEmpty(ArrayBlockingQueue<ByteBuffer> queue) {
        return queue.isEmpty();
    }

    /**
     * Retrieves and removes buffer at the head of this queue,
     * waiting up to the specified wait time if necessary for an element to
     * become available.
     *
     * {@link ArrayBlockingQueue#poll(long, TimeUnit)}
     */
    private ByteBuffer pollSendQueue(ArrayBlockingQueue<ByteBuffer> queue) throws InterruptedException {
       return queue.poll((long) 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * Inserts an element in the {@link #recvQueue}. If the Queue is full, this
     * methods removes an element from the head of the Queue and then inserts
     * the element at the tail of the queue.
     *
     * This method is synchronized to achieve fairness between two threads that
     * are trying to insert an element in the queue. Each thread checks if the
     * queue is full, then removes the element at the head of the queue, and
     * then inserts an element at the tail. This three-step process is done to
     * prevent a thread from blocking while inserting an element in the queue.
     * If we do not synchronize the call to this method, then a thread can grab
     * a slot in the queue created by the second thread. This can cause the call
     * to insert by the second thread to fail.
     * Note that synchronizing this method does not block another thread
     * from polling the queue since that synchronization is provided by the
     * queue itself.
     *
     * @param msg
     *          Reference to the message to be inserted in the queue
     */
    private void addToRecvQueue(Message msg) {
        synchronized(recvQLock) {
            if (recvQueue.remainingCapacity() == 0) {
                try {
                    recvQueue.remove();
                } catch (NoSuchElementException ne) {
                    // element could be removed by poll()
                     LOG.debug("Trying to remove from an empty " +
                         "recvQueue. Ignoring exception " + ne);
                }
            }
            try {
                recvQueue.add(msg);
            } catch (IllegalStateException ie) {
                // This should never happen
                LOG.error("Unable to insert element in the recvQueue " + ie);
            }
        }
    }

    /**
     * Retrieves and removes a message at the head of this queue,
     * waiting up to the specified wait time if necessary for an element to
     * become available.
     *
     * {@link ArrayBlockingQueue#poll(long, TimeUnit)}
     */
    Message pollRecvQueue()
       throws InterruptedException {
       return recvQueue.poll((long) 3000, TimeUnit.MILLISECONDS);
    }
}
