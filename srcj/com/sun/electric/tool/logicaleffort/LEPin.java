/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LEPin.java
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
 * Created on November 11, 2003, 4:09 PM
 */

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.network.Network;

import java.util.ArrayList;
import java.util.List;

/**
 * A Pin is connection between a network and an instance.
 * A Pin's direction is relative to the node (Instance, etc) it
 * connects to, not the network it connects to.
 *
 * <p>This should only be used in the context of the Logical Effort Tool.
 *
 * @author  gainsley
 */
public class LEPin {

    // attributes
    /** pin direction */                        private Dir dir;
    /** logical effort */                       private float le;
    /** name of pin */                          private String name;
    /** Jnetwork this pin is attached to */     private Network net;

    // connectivity
    /** reference to instance pin belongs to */ private LENodable instance;

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
	 * @param instance the instance this belongs to
	 */
	protected LEPin(String name, Dir dir, float le, Network net, LENodable instance)
	{
		this.name = name;
		this.dir = dir;
        this.le = le;
        this.net = net;
        this.instance = instance;
	}
    
    /** Return the direction of the pin. */
    protected Dir getDir() { return dir; }

    /** Return the name of the pin. */
    protected String getName() { return name; }

    /** Return the logical effort of the pin. */
    protected float getLE() { return le; }

    /** Return the instance that is attached to the pin. */
    protected LENodable getInstance() { return instance; }

    /** Return the Network this pin is on */
    protected Network getNetwork() { return net; }

    //----------------------------------UTILITY FUNCTIONS---------------------------------

    /** Return list of specified pins */
    protected static ArrayList<LEPin> getPinListType(List<LEPin> pins, LEPin.Dir dir) {
        ArrayList<LEPin> typepins = new ArrayList<LEPin>();
        for (int i=0; i<pins.size(); i++) {
            if ( ((LEPin)pins.get(i)).getDir() == dir)
                typepins.add((LEPin)pins.get(i));
        }
        return typepins;
    }
    
    /** Return list of bidirectional pins; */
    protected static ArrayList<LEPin> getInoutPins(List<LEPin> pins) {
		return getPinListType(pins, LEPin.Dir.INOUT);
	}

    /** Return list of input pins; */
    protected static ArrayList<LEPin> getInputPins(List<LEPin> pins) {
		return getPinListType(pins, LEPin.Dir.INPUT);
	}

    /** Return list of output pins; */
    protected static ArrayList<LEPin> getOutputPins(List<LEPin> pins) {
		return getPinListType(pins, LEPin.Dir.OUTPUT);
	}

}
