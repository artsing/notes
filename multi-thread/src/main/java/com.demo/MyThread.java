package com.demo;

import java.util.concurrent.atomic.AtomicInteger;

public class MyThread extends Thread{
    private AtomicInteger count = new AtomicInteger(10);

    public MyThread() {
        super();
    }

    @Override
    public void run() {
        long s = System.currentTimeMillis();
        System.out.println("id: " + getId() + " start" );
        for (int j=0; j<5; j++) {
            Integer i = count.get();
        }
        for (int m=0; m<1000; m++) {
            Integer i = 0;
            i++;
        }
        for (int k=0;k<5; k++) {
            Integer j = count.get();
        }
        long e = System.currentTimeMillis();

        System.out.println("id: "+ this.getId() + " end 耗时：" + (e - s));
    }
}
