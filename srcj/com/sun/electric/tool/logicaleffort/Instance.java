/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Instance.java
 * Written by Jonathan Gainsley, Sun Microsystems.
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
 * Created on November 11, 2003, 4:00 PM
 */

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.VarContext;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * An Instance represents a logical effort node.
 *
 * <p>This should only be used in the context of the Logical Effort Tool.
 *
 * @author  gainsley
 */
public class Instance {

    /** name of Instance */                         private String name;
    /** type of LEGate */                           private Instance.Type type;
    /** step-up assigned to this gate */            private float leSU;
    /** size (drive strength) of this gate */       private float leX;
    /** used for levelizing the design */           private int level;
    /** all other pins of Instance */               private ArrayList<Pin> pins;

    /** Context */                                  private VarContext context;
    /** parallel group number (0 is no group) */    private int parallelGroup;
    /** m-factor */                                 private double mfactor;

    /** Nodable this instance derived from */       private Nodable no;

    /** Type is a typesafe enum class that describes the type of Instance this is */
    protected static class Type {
        private final String name;
        private Type(String name) { this.name = name; }
        public String toString() { return name; }

        /** NotSizeable */  protected static final Type STATICGATE = new Type("Static Gate");
        /** NotSizeable */  protected static final Type LOAD = new Type("Load");
        /** NotSizeable */  protected static final Type WIRE = new Type("Wire");
        /** LeGate */       protected static final Type LEGATE = new Type("LE Gate");
        /** LeKeeper */     protected static final Type LEKEEPER = new Type("LE Keeper");
        /** NotSizeable */  protected static final Type CAPACITOR = new Type("Capacitor");
    }
    
    /** Creates a new instance of Instance */
    protected Instance(String name, Instance.Type type, float leSU, float leX, Nodable no) {
        this.name = name;
        this.type = type;
        this.leSU = leSU;
        this.leX = leX;
        this.level = 0;
        this.no = no;
        this.parallelGroup = 0;
        this.mfactor = 1;
        pins = new ArrayList<Pin>();
    }
        
    /** Return list of bidirectional pins; */
    protected ArrayList<Pin> getAllPins() { return pins; }

    /** Get output pins */
    protected ArrayList<Pin> getOutputPins() { return Pin.getOutputPins(pins); }
        
    /** Get input pins */
    protected ArrayList getInputPins() { return Pin.getInputPins(pins); }
    
    /** Get inout pins */
    protected ArrayList getInoutPins() { return Pin.getInoutPins(pins); }
    
    /** True if this is a sizable gate */
    protected boolean isLeGate() {
        if (type == Type.LEGATE || type == Type.LEKEEPER)
            return true;
        return false;
    }

    /** True if this is a gate */
    protected boolean isGate() {
        if (type == Type.LEGATE || type == Type.LEKEEPER || type == Type.STATICGATE)
            return true;
        return false;
    }

    /** Get Type of leGate */
    protected Instance.Type getType() { return type; }

	/** Get name of gate */
	protected String getName() { return name; }

    /** Get the level of the gate */
    protected int getLevel() { return level; }
    /** Set the level of the gate */
    protected void setLevel(int level) { this.level = level; }

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

    /** Get the Nodable */
    protected Nodable getNodable() { return no; }

    /** Get parallelGroup */
    protected int getParallelGroup() { return parallelGroup; }
    /** Set parallelGroup */
    protected void setParallelGroup(int group) { parallelGroup = group; }

    /** Get mfactor */
    protected double getMfactor() { return mfactor; }
    /** Set mfactor */
    protected void setMfactor(double m) { mfactor = m; }

    /** Set the pin list */
    protected void setPins(ArrayList<Pin> pins) { 
        this.pins = pins;

        if (!isLeGate()) return;
        // run some checks if this is an LEGATE

        // check that there is only one output pin for an leGate
        int outputPinCount = 0;
        StringBuffer err = new StringBuffer("LETool leGate '"+name+"' error: more than one output pin: ");
        for (Pin p : pins) {
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

    protected void print() {
        System.out.println(type.toString()+": "+name);
        System.out.println("    Size    \t= "+leX);
        System.out.println("    Step-up \t= "+leSU);
        System.out.println("    Level   \t= "+level);
        System.out.println("    Parallel Group\t= "+parallelGroup);
        System.out.println("    M Factor\t= "+mfactor);
    }

    protected void printShortInfo() {
        if (mfactor > 1)
            System.out.println(type.toString()+": Size="+TextUtils.formatDouble(leX*mfactor, 2)+" (M="+
                    TextUtils.formatDouble(mfactor, 1)+"), "+name);
        else
            System.out.println(type.toString()+": Size="+TextUtils.formatDouble(leX, 2)+", "+name);
    }

    protected float printLoadInfo(Pin pin, float alpha) {
        StringBuffer buf = new StringBuffer();
        buf.append(type.toString());
        buf.append("\t"+name);
        buf.append("\tSize="+TextUtils.formatDouble(leX*mfactor, 2));
        buf.append("\tLE="+TextUtils.formatDouble(pin.getLE(), 2));
        buf.append("\tM="+TextUtils.formatDouble(mfactor, 2));
        float load;
        if (pin.getDir() == Pin.Dir.OUTPUT) {
            load = (float)(leX*pin.getLE()*mfactor*alpha);
            buf.append("\tAlpha="+alpha);
            buf.append("\tLoad="+TextUtils.formatDouble(load, 2));
        } else {
            load = (float)(leX*pin.getLE()*mfactor);
            buf.append("\tLoad="+TextUtils.formatDouble(load, 2));
        }
        System.out.println(buf.toString());
        return load;
    }

}
