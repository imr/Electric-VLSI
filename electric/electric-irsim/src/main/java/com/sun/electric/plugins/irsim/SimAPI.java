/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Sim.java
 * IRSIM simulator
 * Translated by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (C) 1988, 1990 Stanford University.
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 *  fee is hereby granted, provided that the above copyright
 * notice appear in all copies.  Stanford University
 * makes no representations about the suitability of this
 * software for any purpose.  It is provided "as is" without
 * express or implied warranty.
 */
package com.sun.electric.plugins.irsim;

import java.net.URL;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public interface SimAPI {
	// transistor types (tType)
	/** n-channel enhancement */					public static final int	NCHAN       = 0;
	/** p-channel enhancement */					public static final int	PCHAN       = 1;
	/** depletion */								public static final int	DEP         = 2;
	/** simple two-terminal resistor */				public static final int	RESIST      = 3;

	/** transistors not affected by gate logic */	public static final int	ALWAYSON	= 0x02;

	/** set if gate of xistor is a node list */		public static final int	GATELIST	= 0x08;
	/** result of or'ing parallel transistors */	public static final int	ORED		= 0x20;
	/** part of an or'ed transistor */				public static final int	ORLIST		= 0x40;
	/** transistor capacitor (source == drain) */	public static final int	TCAP		= 0x80;

	// transistor states (state
	/** non-conducting */							public static final int	OFF         = 0;
	/** conducting */								public static final int	ON          = 1;
	/** unknown */									public static final int	UNKNOWN     = 2;
	/** weak */										public static final int	WEAK        = 3;

	// transistor temporary flags (tFlags)
	/** Mark for crossing a transistor */			public static final int	CROSSED		= 0x01;
	/** Mark a broken transistor to avoid loop */	public static final int BROKEN		= 0x02;
	/** Mark as broken a parallel transistor */		public static final int	PBROKEN		= 0x04;
	/** Mark as being a parallel transistor */		public static final int	PARALLEL	= 0x08;

	// node potentials
	/** low low */									public static final int	LOW         = 0;
	/** unknown, intermediate, ... value */			public static final int	X           = 1;
													public static final int	X_X         = 2;
	/** logic high */								public static final int	HIGH        = 3;
	/** number of potentials [LOW-HIGH] */			public static final int	N_POTS      = 4;

	/** waiting to decay to X (only in events) */	public static final int	DECAY       = 4;

	// possible values for nFlags
	public static final int	POWER_RAIL     = 0x000002;
	public static final int	ALIAS          = 0x000004;
	public static final int	USERDELAY      = 0x000008;
	public static final int	INPUT          = 0x000010;
	public static final int	WATCHED        = 0x000020;
	public static final int	WATCHVECTOR    = 0x000040;
	public static final int	STOPONCHANGE   = 0x000080;
	public static final int	STOPVECCHANGE  = 0x000100;
	public static final int	VISITED        = 0x000200;

	/** node is within a txtor stack */				public static final int	MERGED		= 0x000400;

	/** node is in high input list */				public static final int	H_INPUT		= 0x001000;
	/** node is in low input list */				public static final int	L_INPUT		= 0x002000;
	/** node is in U input list */					public static final int	U_INPUT		= 0x003000;
	/** node is in X input list */					public static final int	X_INPUT		= 0x004000;

	public static final int INPUT_MASK	=	(H_INPUT | L_INPUT | X_INPUT | U_INPUT);

	/** event scheduling */							public static final int	DEBUG_EV	= 0x01;
	/** final value computation */					public static final int	DEBUG_DC	= 0x02;
	/** tau/delay computation */					public static final int	DEBUG_TAU	= 0x04;
	/** taup computation */							public static final int	DEBUG_TAUP	= 0x08;
	/** spike analysis */							public static final int	DEBUG_SPK	= 0x10;
	/** tree walk */								public static final int	DEBUG_TW	= 0x20;

	public static final int	REPORT_DECAY	= 0x01;
	public static final int	REPORT_DELAY	= 0x02;
	public static final int	REPORT_TAU		= 0x04;
	public static final int	REPORT_TCOORD	= 0x08;
	public static final int REPORT_CAP      = 0x10;

	// resistance types
	/** static resistance */						public static final int	STATIC		= 0;
	/** dynamic-high resistance */					public static final int	DYNHIGH 	= 1;
	/** dynamic-low resistance */					public static final int	DYNLOW  	= 2;
	/** resist. for power calculation (unused) */	public static final int	POWER		= 3;
	/** number of resistance contexts */			public static final int	R_TYPES		= 3;

	/** result of re-evaluation */					public static final int	REVAL		= 0x0;
	/** node is decaying to X */					public static final int	DECAY_EV	= 0x1;

	/** pending from last run */					public static final int	PENDING		= 0x4;

	/** minimum node capacitance (in pf) */			public static final double MIN_CAP	= 0.00001;

	/** dynamic low resistance index */				public static final int	R_LOW		= 0;
	/** dynamic high resistance index */			public static final int	R_HIGH		= 1;
    
	/** scale factor for resolution */              public static final long resolutionScale = 1000;
	/** 1 -> 1ns, 100 -> 0.01ns resolution, etc */

    // Set marameters
	public void setModel(boolean rc);
    public void setAnalyzer(SimAPI.Analyzer analyzer);
    public void setUnitDelay(int unitDelay);
    public void setDecay(long decay);
    public void setDebug(int irDebug);
    
    public void clearReport();
    public void setReport(int mask);

    // Get parameters
    public int getUnitDelay();
    public long getDecay();
    public long getLambdaCM();
    public int getReport();
    
    // Create Network
    /**
     * Put triansitor into the circuit
     * @param gateName name of transistor gate network
     * @param sourceName name of transistor gate network
     * @param drainName drain name of transistor gate network
     * @param gateLength gate length (lambda)
     * @param gateWidth gate width (lambda)
     * @param activeArea active area (lambda^2)
     * @param activePerim active perim (lambda^2)
     * @param centerX x-coordinate of center (lambda)
     * @param centerY y coordinate of cneter (lambda)
     * @param isNTypeTransistor true if this is N-type transistor
     */
    void putTransistor(String gateName, String sourceName, String drainName,
            double gateLength, double gateWidth,
            double activeArea, double activePerim,
            double centerX, double centerY,
            boolean isNTypeTransistor);
    
    /**
     * Put resistor into the circuit
     * @param net1 name of first terminal network
     * @param net2 name of second terminal network
     * @param resistance resistance (ohm)
     */
    public void putResistor(String net1, String net2, double resistance);

    /**
     * Put capacitor into the circuit
     * @param net1 name of first terminal network
     * @param net2 name of second terminal network
     * @param capacitance capacitance (pf)
     */
    public void putCapacitor(String net1, String net2, double capacitance);

	/**
	 * Load a .sim file into memory.
	 *
	 * A .sim file consists of a series of lines, each of which begins with a key letter.
	 * The key letter beginning a line determines how the remainder of the line is interpreted.
	 * The following are the list of key letters understood.
	 *
	 *   | units: s tech: tech format: MIT|LBL|SU
	 *     If present, this must be the first line in the .sim file.
	 *     It identifies the technology of this circuit as tech and gives a scale factor for units of linear dimension as s.
	 *     All linear dimensions appearing in the .sim file are multiplied by s to give centimicrons.
	 *     The format field signifies the sim variant. Electric only recognizes SU format. 
	 *   type g s d l w x y g=gattrs s=sattrs d=dattrs
	 *     Defines a transistor of type type. Currently, type may be e or d for NMOS, or p or n for CMOS.
	 *     The name of the node to which the gate, source, and drain of the transistor are connected are given by g, s, and d respectively.
	 *     The length and width of the transistor are l and w. The next two tokens, x and y, are optional.
	 *     If present, they give the location of a point inside the gate region of the transistor.
	 *     The last three tokens are the attribute lists for the transistor gate, source, and drain.
	 *     If no attributes are present for a particular terminal, the corresponding attribute list may be absent
	 *     (i.e, there may be no g= field at all).
	 *     The attribute lists gattrs, etc. are comma-separated lists of labels.
	 *     The label names should not include any spaces, although some tools can accept label names with
	 *     spaces if they are enclosed in double quotes. In version 6.4.5 and later the default format
	 *     produced by ext2sim is SU. In this format the attribute of the gate starting with S_ is the substrate node of the fet.
	 *     The attributes of the gate, and source and substrate starting with A_, P_ are the area and perimeter
	 *     (summed for that node only once) of the source and drain respectively. This addition to the format is backwards compatible. 
	 *   C n1 n2 cap
	 *     Defines a capacitor between nodes n1 and n2. The value of the capacitor is cap femtofarads.
	 *     NOTE: since many analysis tools compute transistor gate capacitance themselves from the
	 *     transistor's area and perimeter, the capacitance between a node and substrate (GND!)
	 *     normally does not include the capacitance from transistor gates connected to that node.
	 *     If the .sim file was produced by ext2sim(1), check the technology file that was used to
	 *     produce the original .ext files to see whether transistor gate capacitance is included or excluded;
	 *     see "Magic Maintainer's Manual 2 - The Technology File for details. 
	 *   R node res
	 *     Defines the lumped resistance of node node to be res ohms.
	 *   r node1 node2 res
	 *     Defines an explicit resistor between nodes node1 and node2 of resistance res ohms.
	 *   N node darea dperim parea pperim marea mperim
	 *     As an alternative to computed capacitances, some tools expect the total perimeter and area
	 *     of the polysilicon, diffusion, and metal in each node to be reported in the .sim file.
	 *     The N construct associates diffusion area darea (in square centimicrons) and diffusion
	 *     perimeter dperim (in centimicrons) with node node, polysilicon area parea and perimeter pperim,
	 *     and metal area marea and perimeter mperim. This construct is technology dependent and obsolete. 
	 *   = node1 node2
	 *     Each node in a .sim file is named implicitly by having it appear in a transistor definition.
	 *     All node names appearing in a .sim file are assumed to be distinct.
	 *     Some tools, such as esim(1), recognize aliases for node names.
	 *     The = construct allows the name node2 to be defined as an alias for the name node1.
	 *     Aliases defined by means of this construct may not appear anywhere else in the .sim file.
	 */
	public boolean inputSim(URL simFileURL);
    public void finishNetwork();

    // Explore Network
    public int getNumNodes();
    public int getNumAliases();
    public int getNumEdges();
    public List<Node> getNodes();
    public Node getGroundNode();
    public Node getPowerNode();
    public List<Trans> getShortedTransistors();
    
    // Simulation Control
  	/**
	 * Set the firstCall flags.  Used when moving back to time 0.
	 */
	public void reInit();
	/**
	 * Back the event queues up to time 'bTime'.  This is the opposite of
	 * advancing the simulation time.  Mark all pending events as PENDING,
	 * and re-enqueue them according to their creation-time (nTime - delay).
	 */
	public void backSimTime(long bTime, int isInc);
	public void backToTime(Node nd);
	public void printPendingEvents();
	public boolean step(long stopTime,
            Collection<SimAPI.Node> xInputs,
            Collection<SimAPI.Node> hInputs,
            Collection<SimAPI.Node> lInputs,
            Collection<SimAPI.Node> uInputs);
    /**
     * current simulated time
     */
    public void setCurDelta(long curDelta);
    public void clearCurNode();

  
    // Explore events
    public int getNumPunted();
    public int getNumConsPunted();
    public long getNumEvents();
    public long getMaxTime();
    public long getCurDelta();

    // Parsing utility
    public String[] parseLine(String line);

    // Node interface
    public interface Node {
        // Setters
        public void setFlags(long mask);
        public void clearFlags(long mask);
		public void setNext(Node next);
        public void setWind(HistEnt wind);
        public void setCursor(HistEnt wind);
        public void setAssertWhen(Runnable aw);
        public void setAssertWhenPot(short pot);
        
        // Getteras
        public long getTime();
        public Node getLink();
        public short getPot();
        public char getPotChar();
		public Node getCause();
		public Node getNext();
        public String getName();
        public long getFlags();
        public long getFlags(long mask);
		/** first entry in transition history */
        public HistEnt getHead();
		/** ptr. to current history entry */
        public HistEnt getCurr();
		/** Analyzer: window start */
        public HistEnt getWind();
		/** Analyzer: cursor value */
        public HistEnt getCursor();
        public Collection<Trans> getGates();
        public Collection<Trans> getTerms();
        public Runnable getAssertWhen();
        
        // Describers
        public String describeDelay();
        public String[] describePendingEvents();
    }
    
    public interface Trans {
        // Getters
        public int getBaseType();
        public String describeBaseType();
        public String describeState();
        public Node getGate();
        public Node getSource();
        public Node getDrain();
        public int getX();
        public int getY();
        public double[] getResists();
        public long getLength();
        public long getWidth();
        /**
        /**
         * figure what's on the *other* terminal node of a transistor
         */
        public Node getOtherNode(Node n);
        public Trans getLink();
        public Collection<Trans> getGateList();
   }
    
    public interface HistEnt {
        // Getters
        public long getTime();
        public byte getVal();
		public HistEnt getNextHist();
    }
    
    public interface Analyzer {
        // Redisplay hooks
    	public void updateWindowIfAnalyzerOn(long endT);
        public void dispWatchVec(long which);
    }
}