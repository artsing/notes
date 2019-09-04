package com.demo;

public class MainApplication {
    public static void main(String[] args) {
        MyObject myObject = new MyObject();

        MyThread myThread = new MyThread(true, myObject);
        myThread.start();

        MyThread myThread1 = new MyThread(false, myObject);
        myThread1.start();
    }
}