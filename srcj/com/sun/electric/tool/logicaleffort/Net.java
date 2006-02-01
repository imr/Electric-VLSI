/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Net.java
 * Written by: Jonathan Gainsley, Sun Microsystems.
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
 * Created on November 11, 2003, 4:10 PM
 */

package com.sun.electric.tool.logicaleffort;

import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * Stores information about a Net for the Logical Effort Tool.
 *
 * <p>This should only be used in the context of the Logical Effort Tool.
 *
 * @author  gainsley
 */
public class Net {
    
	// --------------------- private variables -------------------------------

	/** name of net */                              private String name;
    /** list of pins on network */                  private ArrayList<Pin> pins;
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
        pins = new ArrayList<Pin>();
	}

    /**
     * Returns true if this net is driven by a sizeable gate.
     */
    protected boolean isDrivenBySizeableGate() {
        for (Pin pin : pins) {
            Instance inst = pin.getInstance();
            if (pin.getDir() == Pin.Dir.OUTPUT) {
                if (inst.getType() == Instance.Type.LEGATE) return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this net is driven by a static gate (non-sizeable).
     */
    protected boolean isDrivenByStaticGate() {
        for (Pin pin : pins) {
            Instance inst = pin.getInstance();
            if (pin.getDir() == Pin.Dir.OUTPUT) {
                if (inst.getType() == Instance.Type.STATICGATE) return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this net is driven by a sizeable or
     * fixed size gate.
     */
    protected boolean isDrivenByGate() {
        for (Pin pin : pins) {
            Instance inst = pin.getInstance();
            if (pin.getDir() == Pin.Dir.OUTPUT) {
                if ((inst.getType() == Instance.Type.LEGATE) ||
                    (inst.getType() == Instance.Type.STATICGATE)) return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this net drives a sizeable gate.
     */
    protected boolean drivesSizableGate() {
        for (Pin pin : pins) {
            Instance inst = pin.getInstance();
            if (pin.getDir() == Pin.Dir.INPUT) {
                if (inst.getType() == Instance.Type.LEGATE) return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this net drives a static gate.
     */
    protected boolean drivesStaticGate() {
        for (Pin pin : pins) {
            Instance inst = pin.getInstance();
            if (pin.getDir() == Pin.Dir.INPUT) {
                if (inst.getType() == Instance.Type.STATICGATE) return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this net drives a load or a wire
     */ 
    protected boolean drivesLoad() {
        for (Pin pin : pins) {
            Instance inst = pin.getInstance();
            if (pin.getDir() == Pin.Dir.INPUT) {
                if ((inst.getType() == Instance.Type.LOAD) ||
                    (inst.getType() == Instance.Type.WIRE)) return true;
            }
        }
        return false;
    }


    /** Add a pin to the net */
    protected void addPin(Pin pin) { pins.add(pin); pin.setNet(this); }
    
    /** Get a list of pins attached to the net */
    protected ArrayList<Pin> getAllPins() { return pins; }
    
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
