/*
 * Instance.java
 *
 * Created on November 11, 2003, 4:00 PM
 */

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.tool.logicaleffort.*;

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
    
    /** Type is a typesafe enum class that describes the type of Instance this is */
    protected static class Type {
        private final String name;
        private Type(String name) { this.name = name; }
        public String toString() { return name; }

        /** NON-LE */       protected static final Type NONLE = new Type("nonLE");
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
            System.out.println("LETool leGate '"+name+"' error: no output pin");
        if (outputPinCount > 1)
            System.out.println(err.substring(0, err.length()-2));
    }

}
