package com.sun.electric.tool.simulation.test;

/*
 * pst3202.java
 *
 * Created on May 25, 2004 
 */

/**
 * 
 * @author ac147373
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 */

public class f206 extends Equipment {

    String s = new String("null");

    /** Creates a new instance of power suppy */
    public f206(String name) {
        super(name);
    }

    void testConnection() {
        write("help");
        s = read(2000).trim();
        //s = s.substring(0,s.length()-1);
        System.out.println("help " + s);
        try { Thread.sleep(100); } catch (InterruptedException _) { }
        write("ini");
        try { Thread.sleep(100); } catch (InterruptedException _) { }
    }//end testConnection

    void initialize() {
        write("ini");
        try { Thread.sleep(100); } catch (InterruptedException _) { }
    }//end initialize

    void move(float x, float y, float z, float u, float v, float w) {
        write("mov x" + x + " y" + y + " z" + z + " u" + u + " v" + v + " w"
                + w);
        write("mov?");
        s = read(20).trim();
        System.out.println("move complete " + s);
    }//end moveY

    void moveZ(float val) {
        write("mov z " + val);
        write("mov?");
        s = read(2000).trim();
        System.out.println("move complete " + s);
    }//end moveZ

    void moveX(float val) {
        write("mov x " + val);
        write("mov?");
        s = read(20).trim();
        System.out.println("move complete " + s);
    }//end moveX

    void moveY(float val) {
        write("mov y " + val);
        write("mov?");
        s = read(20).trim();
        System.out.println("move complete " + s);
    }//end moveY

    void moveU(float val) {
        write("mov u " + val);
        write("mov?");
        s = read(20).trim();
        System.out.println("move complete " + s);
    }//end moveU

    void moveV(float val) {
        write("mov v " + val);
        try { Thread.sleep(100); } catch (InterruptedException _) { }
        write("mov?");
        s = read(2000).trim();
        System.out.println("move complete " + s);
        try { Thread.sleep(100); } catch (InterruptedException _) { }
    }//end moveV

    void moveW(float val) {
        write("mov y " + val);
        write("mov?");
        s = read(20).trim();
        System.out.println("move complete " + s);
    }//end moveW

    public static void main(String args[]) {
        f206 pos = new f206("f206");
        pos.testConnection();
        pos.move(1f, 1f, 1f, 3, 4, 0);
    }//end main

}//end class
