/* -*- tab-width: 4 -*-
 *
 * Net.java
 *
 * Created on November 11, 2003, 4:10 PM
 */

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.tool.logicaleffort.*;

import java.util.ArrayList;

/**
 *
 * NOTE: the only 'Electric' objects used are in LENetlister,
 * any objects referenced in this file are from the logicaleffort
 * package, although their names may imply otherwise.  Their names
 * are as such because their names match PNP's naming scheme.
 *
 * @author  gainsley
 */
public class Net {
    
	// --------------------- private variables -------------------------------

	/** name of net */                              private String name;
    /** list of pins on network */                  private ArrayList pins;
	///** list of nodes in wire network */            private ArrayList wireNodes;
	///** list of wires in wire network */            private ArrayList wireConnections;

	// --------------------- public variables --------------------------------


	// --------------------- protected and private methods -------------------
    
    /* 
	 * Create new Net.
	 * @param name name of Net
	 */
	protected Net(String name)
	{
		this.name = name;
        pins = new ArrayList();
	}

    /** Add a pin to the net */
    protected void addPin(Pin pin) { pins.add(pin); pin.setNet(this); }
    
    /** Get a list of pins attached to the net */
    protected ArrayList getAllPins() { return pins; }
    
    /** Get output pins */
    protected ArrayList getOutputPins() { return Pin.getOutputPins(pins); }
    
    /** Get input pins */
    protected ArrayList getInputPins() { return Pin.getInputPins(pins); }
    
    /** Get inout pins */
    protected ArrayList getInoutPins() { return Pin.getInoutPins(pins); }
 
    /** Get net name */
    protected String getName() { return name; }
    
    /** Set the list of WireConnections */
    //protected void setWireConnections(ArrayList wireConnections) { this.wireConnections = wireConnections; }

    /** Set the list of WireNodes */
    //protected void setWireNodes(ArrayList wireNodes) { this.wireNodes = wireNodes; }

}
