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
    private final static String WORKERS_PATH = "/workers";
    private final static String ASSIGN_PATH = "/assign";
    private final static String TASKS_PATH = "/tasks";
    private final static String STATUS_PATH = "/status";

    private String serverId;
    private ZooKeeper zk;

    private boolean leader = false;

    public Master(String serverId) throws IOException {
        this.serverId = serverId;

        String connectString = "localhost";
        int sessionTimeout = 5000;
        zk = new ZooKeeper(connectString, sessionTimeout, this);
        runForMaster();

        bootstrap();
    }

    private void runForMaster() {
        zk.create(MASTER_PATH, serverId.getBytes(),
                OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, masterCreateCallback, null);
    }

    private void checkMaster() {
        zk.getData(MASTER_PATH, false, masterCheckMasterCallback, null);
    }

    private AsyncCallback.StringCallback masterCreateCallback = new AsyncCallback.StringCallback() {
        public void processResult(int rc, String path, Object ctx, String name) {
            switch (KeeperException.Code.get(rc)) {
                case OK: {
                    leader = true;
                    break;
                }
                case CONNECTIONLOSS: {
                    checkMaster();
                    break;
                }
                default: {
                    leader = false;
                }
            }
        }
    };

    private AsyncCallback.DataCallback masterCheckMasterCallback = new AsyncCallback.DataCallback() {
        public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS: {
                    checkMaster();
                    break;
                }
                case NONODE: {
                    runForMaster();
                    break;
                }
                default: {
                    // nothing to do
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

    public void bootstrap() {
        createParent(WORKERS_PATH, new byte[0]);
        createParent(ASSIGN_PATH, new byte[0]);
        createParent(TASKS_PATH, new byte[0]);
        createParent(STATUS_PATH, new byte[0]);
    }

    private void createParent(String path, byte[] data) {
        zk.create(path, data, OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT, createParentCallback, data);
    }

    AsyncCallback.StringCallback createParentCallback = new AsyncCallback.StringCallback() {
        public void processResult(int rc, String path, Object ctx, String name) {
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS: {
                    createParent(path, (byte[]) ctx);
                    break;
                }
                case OK: {
                    // log
                    break;
                }
                case NODEEXISTS: {
                    // log
                    break;
                }
                default: {
                    // log
                }
            }
        }
    };
}
