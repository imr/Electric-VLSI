/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Instance.java
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
 * Instance.java
 *
 * Created on November 11, 2003, 4:00 PM
 */

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.tool.logicaleffort.*;
import com.sun.electric.database.variable.VarContext;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * An Instance represents a logical effort node.
 * Note that this object is protected, and should only 
 * be instantiated/used by other logical effort classes.
 *
 * NOTE: the only 'Electric' objects used are in LENetlister,
 * any objects referenced in this file are from the logicaleffort
 * package, although their names may imply otherwise.  Their names
 * are as such because their names match PNP's naming scheme.
 *
 * @author  gainsley
 */
public class Instance {

    /** name of Instance */                         private String name;
    /** type of LEGate */                           private Instance.Type type;
    /** step-up assigned to this gate */            private float leSU;
    /** size (drive strength) of this gate */       private float leX;
    /** used for levelizing the design */           private int leLevel;
    /** all other pins of Instance */               private ArrayList pins;
    /** if an LEGate */                             private boolean leGate;
    
    /** Context */                                  private VarContext context;
    
    /** Type is a typesafe enum class that describes the type of Instance this is */
    protected static class Type {
        private final String name;
        private Type(String name) { this.name = name; }
        public String toString() { return name; }

        /** NotSizeable */  protected static final Type NOTSIZEABLE = new Type("notSizable");
        /** LeGate */       protected static final Type LEGATE = new Type("leGate");
        /** LeKeeper */     protected static final Type LEKEEPER = new Type("leKeeper");
    }
    
    /** Creates a new instance of Instance */
    protected Instance(String name, Instance.Type type, float leSU, float leX) {
        this.name = name;
        this.type = type;
        this.leSU = leSU;
        this.leX = leX;
        pins = new ArrayList();
        leGate = false;
        if (type == Type.LEGATE || type == Type.LEKEEPER)
            leGate = true;
    }
        
    /** Return list of bidirectional pins; */
    protected ArrayList getAllPins() { return pins; }

    /** Get output pins */
    protected ArrayList getOutputPins() { return Pin.getOutputPins(pins); }
        
    /** Get input pins */
    protected ArrayList getInputPins() { return Pin.getInputPins(pins); }
    
    /** Get inout pins */
    protected ArrayList getInoutPins() { return Pin.getInoutPins(pins); }
    
    /** Get optimization flag */
    protected boolean getLeGate() { return leGate; }
    
    /** Get Type of leGate */
    protected Instance.Type getType() { return type; }

	/** Get name of gate */
	protected String getName() { return name; }

    /** Get the level of the gate */
    protected int getLeLevel() { return leLevel; }
    /** Set the level of the gate */
    protected void setLeLevel(int leLevel) { this.leLevel = leLevel; }

    /** Get the step-up/delay */
    protected float getLeSU() { return leSU; }
    /** Set the step-up/delay */
    protected void setLeSU(float leSU) { this.leSU = leSU; }

    /** Get the size of the gate */
    protected float getLeX() { return leX; }
    /** Set the size of the gate */
    protected void setLeX(float LeX) { this.leX = LeX; }

    /** Get VarContext */
    protected VarContext getContext() { return context; }
    /** Set VarContext */
    protected void setContext(VarContext context) { this.context = context; }
    
    /** Set the pin list */
    protected void setPins(ArrayList pins) { 
        this.pins = pins;
        if (!leGate) return;
        // check that there is only one output pin for an leGate
        int outputPinCount = 0;
        StringBuffer err = new StringBuffer("LETool leGate '"+name+"' error: more than one output pin: ");
        for (Iterator it = pins.iterator(); it.hasNext();) {
            Pin p = (Pin)it.next();
            if (p.getDir() == Pin.Dir.OUTPUT) {
                outputPinCount++;
                err.append(p.getName()+", ");
            }
        }
        if (outputPinCount == 0)
            System.out.println("LETool leGate '"+name+"' error: no output pin, or no 'le' logical effort defined on output pin");
        if (outputPinCount > 1)
            System.out.println(err.substring(0, err.length()-2));
    }

}
