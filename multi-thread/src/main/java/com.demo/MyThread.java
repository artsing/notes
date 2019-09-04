package com.demo;

public class MyThread extends Thread{
    private boolean flag;
    private MyObject myObject;

    public MyThread(boolean flag, MyObject myObject) {
        super();
        this.flag = flag;
        this.myObject = myObject;
    }

    @Override
    public void run() {
        while (true) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (flag) {
                myObject.addAge();
            } else {
                myObject.subAge();
            }
            myObject.print();
        }
    }
}
