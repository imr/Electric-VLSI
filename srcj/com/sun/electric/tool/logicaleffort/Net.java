/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Net.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
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
