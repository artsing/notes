package com.demo.client;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Client
 * @author artsing
 */
public class Client implements Watcher {
    private static final Logger log = LoggerFactory.getLogger(Client.class);

    private ZooKeeper zk;

    public Client() throws IOException {
        startZK();
    }

    public void startZK() throws IOException {
        String connectString = "localhost";
        int sessionTimeout = 5000;
        zk = new ZooKeeper(connectString, sessionTimeout, this);
    }

    public String queueCommand(String command)
            throws InterruptedException, KeeperException.NodeExistsException {

        while (true) {
            try {
                return zk.create("/tasks/task-", command.getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
            } catch (KeeperException.NodeExistsException e) {
                throw e;
            } catch (KeeperException e) {
                // nothing to do
            }
        }
    }

    public void process(WatchedEvent event) {
        log.info(event.toString());
    }
}
