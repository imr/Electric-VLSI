package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.variable.VarContext;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintStream;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Aug 30, 2004
 * Time: 9:28:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class LENetwork {

    /** Name */                             private String name;
    /** List of pins on network */          private List pins;
    /** List of networks on network */      private List networks;

    protected LENetwork(String name) {
        this.name = name;
        pins = new ArrayList();
        networks = new ArrayList();
    }

    protected void add(LEPin pin) { pins.add(pin); }
    protected void add(LENetwork net) { networks.add(net); }

    protected String getName() { return name; }
    protected Iterator getSubNets() { return networks.iterator(); }

    protected List getAllPins() {
        List allpins = new ArrayList(pins);
        for (Iterator it = networks.iterator(); it.hasNext(); ) {
            LENetwork net = (LENetwork)it.next();
            allpins.addAll(net.getAllPins());
        }
        return allpins;
    }

    protected void print() {
        print("", System.out);
    }

    protected void print(String header, PrintStream out) {
        out.println(header+"Network "+name+", connects to: ");
        for (Iterator it = pins.iterator(); it.hasNext();) {
            LEPin pin = (LEPin)it.next();
            LENodable leno = pin.getInstance();
            out.println(header+"  "+leno.getName());
        }
        for (Iterator it = networks.iterator(); it.hasNext(); ) {
            LENetwork net = (LENetwork)it.next();
            net.print(header+"  ", out);
        }
    }
}
