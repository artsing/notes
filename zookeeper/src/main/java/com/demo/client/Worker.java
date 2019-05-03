package com.demo.client;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Worker
 * @author artsing
 */
public class Worker implements Watcher {
    private static final Logger log = LoggerFactory.getLogger(Worker.class);

    private ZooKeeper zk;
    private String status = "down";

    public Worker() throws IOException {
        String connectString = "localhost";
        int sessionTimeout = 5000;
        zk = new ZooKeeper(connectString, sessionTimeout, this);
        register();
        setStatus("up");
    }

    private void register() {
        zk.create("/workers/worker-" + 1, "Idle".getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, createWorkerCallback, null);
    }

    private AsyncCallback.StringCallback createWorkerCallback = new AsyncCallback.StringCallback() {
        public void processResult(int rc, String path, Object ctx, String name) {
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS: {
                    register();
                    break;
                }
                case OK: {
                    log.info("Registered successfully: 1");
                    break;
                }
                case NODEEXISTS: {
                    log.warn("Already registered: 1");
                    break;
                }
                default: {
                    log.error("error: " + KeeperException.
                            create(KeeperException.Code.get(rc), path));
                }
            }
        }
    };

    public synchronized void updateStatus(String status) {
        if (status == this.status) {
            zk.setData("/workers/worker-1", status.getBytes(),
                    -1, statusUpdateCallback, status);
        }
    }

    public void setStatus(String status) {
        this.status = status;
        updateStatus(status);
    }

    private AsyncCallback.StatCallback statusUpdateCallback = new AsyncCallback.StatCallback() {
        public void processResult(int rc, String path, Object ctx, Stat stat) {
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS: {
                    updateStatus((String) ctx);
                    return;
                }
                default: {
                    // nothing to do
                }
            }
        }
    };

    public void process(WatchedEvent event) {
        log.info(event.toString());
    }
}
