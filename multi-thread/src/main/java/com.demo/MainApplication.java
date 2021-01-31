package com.demo;

public class MainApplication {
    public static void main(String[] args) {
        for (int i=0; i<500; i++) {
            MyThread myThread = new MyThread();
            myThread.start();
        }


    }
}
