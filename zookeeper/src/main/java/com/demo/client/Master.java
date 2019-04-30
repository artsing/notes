package com.demo.client;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;

import static org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE;

/**
 * Master
 * @author x17075
 */
public class Master implements Watcher {
    private final static String MASTER_PATH = "/master";

    private String connectString = "localhost";
    private int sessionTimeout = 5000;

    private String serverId;
    private ZooKeeper zk;

    private boolean leader = false;

    public Master(String serverId) {
        this.serverId = serverId;

        try {
            zk = new ZooKeeper(connectString, sessionTimeout, this);
            runForMaster();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runForMaster() {
        zk.create(MASTER_PATH, serverId.getBytes(),
                OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, masterCreateCallback, null);
    }

    private boolean checkMaster() throws InterruptedException {
        try {
            Stat stat = new Stat();
            byte data[] = zk.getData(MASTER_PATH, false, stat);
            leader = new String(data).equals(serverId);
            return true;
        } catch (KeeperException e) {
            return false;
        }
    }

    private AsyncCallback.StringCallback masterCreateCallback = new AsyncCallback.StringCallback() {
        public void processResult(int rc, String path, Object ctx, String name) {
            switch (KeeperException.Code.get(rc)) {
                case OK: {
                    leader = true;
                    break;
                }
                case CONNECTIONLOSS: {
                    try {
                        checkMaster();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                default: {
                    leader = false;
                }
            }
        }
    };

    public void process(WatchedEvent event) {
        System.out.println(event);
    }

    public boolean isLeader() {
        return leader;
    }

    public void setLeader(boolean leader) {
        this.leader = leader;
    }
}
