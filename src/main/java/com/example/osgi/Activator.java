package com.example.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    public void start(BundleContext context) throws Exception {
        System.out.println("加载模块");
    }

    public void stop(BundleContext context) throws Exception {
        System.out.println("停止模块");
    }
}
