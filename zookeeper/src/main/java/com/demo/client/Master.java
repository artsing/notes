package com.demo.client;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;

import static org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE;

public class Master implements Watcher {
    private final static String MASTER_PATH = "/master";

    private String connectString = "localhost";
    private int sessionTimeout = 5000;

    private String serverId;
    private ZooKeeper zk;

    private boolean leader = false;

    public boolean isLeader() {
        return leader;
    }

    public void setLeader(boolean leader) {
        this.leader = leader;
    }

    public Master() {
        this("6d47aef1-470d-4732-b1ea-5f9672befe95");
    }

    public Master(String serverId) {
        this.serverId = serverId;

        try {
            zk = new ZooKeeper(connectString, sessionTimeout, this);
            runForMaster();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void runForMaster() throws InterruptedException {
        while (true) {
            try {
                zk.create(MASTER_PATH, serverId.getBytes(), OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                leader = true;
                break;
            } catch (KeeperException e) {
            }
            if (checkMaster()) {
                break;
            }
        }
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

    public void process(WatchedEvent event) {
        System.out.println(event);
    }
}
