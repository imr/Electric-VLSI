/* -*- tab-width: 4 -*-
 *
 * Pin.java
 *
 * Created on November 11, 2003, 4:09 PM
 */

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.tool.logicaleffort.*;

import java.util.ArrayList;

/**
 * A Pin is connection between a network and an instance.
 * A Pin's direction is relative to the node (Instance, etc) it
 * connects to, not the network it connects to.
 *
 * NOTE: the only 'Electric' objects used are in LENetlister,
 * any objects referenced in this file are from the logicaleffort
 * package, although their names may imply otherwise.  Their names
 * are as such because their names match PNP's naming scheme.
 *
 * @author  gainsley
 */
public class Pin {

    // attributes
    /** pin direction */                        private Dir dir;
    /** logical effort */                       private float le;
    /** name of pin */                          private String name;

    // connectivity
    /** reference to instance pin belongs to */ private Instance instance;
    /** reference to net pin is part of */      private Net net;
    /** name of net, used to create net */      private String netName;
    /** wireNode in net's network that pin is connected to */
    //                                            private WireNode wireNode;
    
    /** Dir is a typesafe enum class that describes the direction of the pin */
    protected static class Dir {
        private final String name;
        private Dir(String name) { this.name = name; }
        public String toString() { return name; }

        /** Input */
		protected static final Dir INPUT = new Dir("input");
        /** Output */
        protected static final Dir OUTPUT = new Dir("output");
        /** In-Out */
        protected static final Dir INOUT = new Dir("inout");
        /** no dir */
        protected static final Dir NODIR = new Dir("nodir");
    }

	/**
	 * Create new pin.
	 * @param name name of pin
	 * @param dir pin direction (Pin.INPUT, Pin.OUTPUT, Pin.INOUT, Pin.NODIR)
	 * @param net net pin is on
	 */
	protected Pin(String name, Dir dir, float le, String netName)
	{
		this.name = name;
		this.dir = dir;
        this.le = le;
        this.netName = netName;
	}
    
    /** Return the direction of the pin. */
    protected Dir getDir() { return dir; }

    /** Return the name of the pin. */
    protected String getName() { return name; }

    /** Return the logical effort of the pin. */
    protected float getLE() { return le; }

    /** Return the net name */
    protected String getNetName() { return netName; }

    /** Return the instance that is attached to the pin. */
    protected Instance getInstance() { return instance; }
    /** Set the instance to which the pin is attached. */
    protected void setInstance(Instance instance) { this.instance = instance; }
    
    /** Return the net attached to pin. */
    protected Net getNet() { return net; }
    /** Set the net attached to pin. */
    protected void setNet(Net net) { this.net = net; }
    
    //----------------------------------UTILITY FUNCTIONS---------------------------------

    /** Return list of specified pins */
    protected static ArrayList getPinListType(ArrayList pins, Pin.Dir dir) {
        ArrayList typepins = new ArrayList();
        for (int i=0; i<pins.size(); i++) {
            if ( ((Pin)pins.get(i)).getDir() == dir)
                typepins.add((Pin)pins.get(i));
        }
        return typepins;
    }
    
    /** Return list of bidirectional pins; */
    protected static ArrayList getInoutPins(ArrayList pins) {
		return getPinListType(pins, Pin.Dir.INOUT);
	}

    /** Return list of input pins; */
    protected static ArrayList getInputPins(ArrayList pins) {
		return getPinListType(pins, Pin.Dir.INPUT);
	}

    /** Return list of output pins; */
    protected static ArrayList getOutputPins(ArrayList pins) {
		return getPinListType(pins, Pin.Dir.OUTPUT);
	}

}
