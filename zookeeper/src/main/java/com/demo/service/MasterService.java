package com.demo.service;

import org.apache.zookeeper.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

import static org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE;

//@Service
public class MasterService implements Watcher {
    private ZooKeeper zk;

    public MasterService() {
        try {
            zk = new ZooKeeper("localhost", 5000, this);
            try {
                zk.create("/master", UUID.randomUUID().toString().getBytes(), OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void process(WatchedEvent event) {
        System.out.println(event);
    }
}
