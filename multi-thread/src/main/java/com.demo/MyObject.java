package com.demo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author artsing
 */
public class MyObject {
    private int age;
    private static final Map<String, String> SERVERMAP = new LinkedHashMap<String, String>();

    synchronized public void addAge() {
        age++;
    }

    synchronized public void subAge() {
        age--;
    }

    synchronized public void print() {
        System.out.println(age);
    }
}
