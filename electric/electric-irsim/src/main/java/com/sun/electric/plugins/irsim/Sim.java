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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

public class Sim implements SimAPI
{
	public static class Node implements SimAPI.Node
	{
		/** sundries list */									Node           nLink;
		/** charge sharing event */								Eval.Event     events;
		/** list of xtors w/ gates connected to this node */	List<Trans>    nGateList;
		/** list of xtors w/ src/drn connected to this node */	List<Trans>    nTermList;
		/** capacitance of node in pf */						float          nCap;
		/** low logic threshold for node, normalized units */	float          vLow;
		/** high logic threshold for node, normalized units */	float          vHigh;
		/** low to high transition time in DELTA's */			short          tpLH;
		/** high to low transition time in DELTA's */			short          tpHL;
//		/** signal in the waveform window (if displayed) */		Signal<DigitalSample>  sig;
		/** combines time, nindex, cap, and event */			private Object c;
		/** combines cause, punts, and tranT */					private Object t;
		/** combines thev, next, and tranN */					private Object n;

		/** current potential */								short          nPot;
		/** flag word (see defs below) */						long           nFlags;
		/** ascii name of node */								String         nName;

		/** first entry in transition history */				HistEnt        head;
		/** ptr. to current history entry */					HistEnt        curr;
		/** potential for pending AssertWhen */					short          awPot;
		/** pending asswertWhen list */							Runnable       awPending;
		/** index of this node (a unique value) */				int            index;
		/** Analyzer: window start */							HistEnt        wind;
		/** Analyzer: cursor value */							HistEnt        cursor;

		Node(Sim theSim)
		{
			head = new HistEnt();
			index = theSim.nodeIndexCounter++;
		}

		void setTime(long time) { c = new Long(time); }
		void setNIndex(long nIndex) { c = new Long(nIndex); }
		void setCap(float cap) { c = new Float(cap); }
		void setEvent(Eval.Event event) { c = event; }

		void setCause(Node cause) { t = cause; }
		void setPunts(HistEnt punts) { t = punts; }

		void setThev(Thev thev) { n = thev; }
		public void setNext(SimAPI.Node next) { n = (Node)next; }
		void setTrans(Trans trans) { n = trans; }

		public long getTime() { return ((Long)c).longValue(); }
		long getNIndex() { return ((Long)c).longValue(); }
		float getCap() { return ((Float)c).floatValue(); }
		Eval.Event getEvent() { return (Eval.Event)c; }

		public Node getCause() { return (Node)t; }
		HistEnt getPunts() { return (HistEnt)t; }

		Thev getThev() { return (Thev)n; }
		public Node getNext() { return (Node)n; }
		Trans getTrans() { return (Trans)n; }
        
        public String getName() { return nName; }
        public Node getLink() { return nLink; }
        public short getPot() { return nPot; }
        public char getPotChar() {
            return vChars.charAt(getPot());
        }
        public long getFlags() { return nFlags; }
        public long getFlags(long mask) { return nFlags & mask; }
        public void setFlags(long mask) { nFlags |= mask; }
        public void clearFlags(long mask) { nFlags &= ~mask; }
		/** first entry in transition history */
        public HistEnt getHead() { return head; }
		/** ptr. to current history entry */
        public HistEnt getCurr() { return curr; }
		/** Analyzer: window start */
        public HistEnt getWind() { return wind; }
		/** Analyzer: cursor value */
        public HistEnt getCursor() { return cursor; }
        public void setWind(SimAPI.HistEnt wind) { this.wind = (HistEnt)wind; }
        public void setCursor(SimAPI.HistEnt cursor) { this.cursor = (HistEnt)cursor; }
        
        public Collection<SimAPI.Trans> getGates() {
            return Collections.<SimAPI.Trans>unmodifiableCollection(nGateList);
        }
        public Collection<SimAPI.Trans> getTerms() {
            return Collections.<SimAPI.Trans>unmodifiableCollection(nTermList);
        }
        public String describeDelay() {
			String infstr = "";
			if (getFlags(INPUT) != 0)
				infstr += "[NOTE: node is an input] ";
			infstr += "(vl=" + vLow + " vh=" + vHigh + ") ";
			if (getFlags(USERDELAY) != 0)
				infstr += "(tpLH=" + tpLH + ", tpHL=" + tpHL + ") ";
			infstr += "(" + nCap + " pf) ";
            return infstr;
        }
        public String[] describePendingEvents() {
			if (events == null) return null;
            List<String> list = new ArrayList<String>();
            for(Eval.Event e = events; e != null; e = e.nLink) {
                list.add("   transition to " + Sim.vChars.charAt(e.eval) + " at " + Sim.deltaToNS(e.nTime)+ "ns");
			}
            return list.toArray(new String[list.size()]);
        }
        public Runnable getAssertWhen() {
            return awPending;
        }
        public void setAssertWhen(Runnable aw) {
            awPending = aw;
        }
        public void setAssertWhenPot(short pot) {
            awPot = pot;
        }
	};

	/**
	 * find node in network
	 */
	public Node findNode(String name)
	{
		return nodeHash.get(Electric.canonicString(name));
	}

	/**
	 * Get node structure.  If not found, create a new one.
	 * If create is TRUE a new node is created and NOT entered into the table.
	 */
	private Node getNode(String name)
	{
		Node n = findNode(name);
		if (n != null)
		{
			if (!name.equals(n.nName))
			{
				boolean skip = false;
				if (name.equalsIgnoreCase("vdd"))
				{
					skip = warnVdd; warnVdd = true;
				}
				if (name.equalsIgnoreCase("gnd"))
				{
					skip = warnGnd; warnGnd = true;
				}
				if (!skip)
					System.out.println("Warning: Aliasing nodes '" + name + "' and '" + n.nName + "'");
			}
			while ((n.nFlags & ALIAS) != 0)
				n = n.nLink;
			return n;
		}

		// allocate new node from free storage
		n = new Node(this);

		numNodes++;

		// insert node into hash table and list
		nodeHash.put(Electric.canonicString(name), n);
		nodeList.add(n);

		// initialize node entries
		n.nName = name;
		n.nGateList = new ArrayList<Trans>();
		n.nTermList = new ArrayList<Trans>();
		n.nFlags = 0;
		n.nCap = (float)MIN_CAP;
		n.vLow = (float)theConfig.lowThresh;
		n.vHigh = (float)theConfig.highThresh;
		n.setTime(0);
		n.tpLH = 0;
		n.tpHL = 0;
		n.setCause(null);
		n.nLink = null;
		n.events = null;
		n.nPot = X;
		n.awPending = null;

		n.head = new HistEnt();
		n.head.next = lastHist;
		n.head.hTime = 0;
		n.head.val = X;
		n.head.inp = false;
		n.head.punt = false;
		n.head.rTime = n.head.delay = 0;
		n.curr = n.head;

		return n;
	}

	/**
	 * Return a list of all nodes in the network.
	 */
	public List<Node> getNodeList()
	{
		return nodeList;
	}
    
    public List<SimAPI.Node> getNodes() {
        return Collections.<SimAPI.Node>unmodifiableList(nodeList);
    }
    
    public Node getGroundNode() { return groundNode; }
    public Node getPowerNode() { return powerNode; }
    public int getNumNodes() { return numNodes; }
    public int getNumAliases() { return numAliases; }
    public int getNumEdges() { return numEdges; }
    public int getNumPunted() { return numPunted; }
    public int getNumConsPunted() { return numConsPunted; }
    public long getNumEvents() { return nEvent; }
    
    public List<SimAPI.Trans> getShortedTransistors() {
        ArrayList<SimAPI.Trans> result = new ArrayList<SimAPI.Trans>();
		for(Trans t = tCap.getSTrans(); t != tCap; t = t.getSTrans()) {
            result.add(t);
		}
        return result;
    }
    
    public int getReport() { return tReport; }
    public void setReport(int mask) { tReport |= mask; }
    public void clearReport() { tReport = 0; }
    public long getMaxTime() { return maxTime; }

	public static class Trans implements SimAPI.Trans
	{
		/** nodes to which trans is connected */	Object   gate;
		/** nodes to which trans is connected */	Node     source, drain;
		/** caches to remember src/drn values */	Object   sCache, dCache;
		/** type of transistor */					byte     tType;
		/** cache to remember current state */		byte     state;
		/** transistor flags */						byte     tFlags;
		/** index into parallel list */				byte     nPar;
		/** transistor resistances */				Resists  r;
		/** next txtor in position hash table */	Trans    tLink;
		/** position in the layout */				int      x, y;

		void setSThev(Thev r) { sCache = r; }
		void setDThev(Thev r) { dCache = r; }
		void setSTrans(Trans t) { sCache = t; }
		void setDTrans(Trans t) { dCache = t; }
		void setSI(int i) { sCache = new Integer(i); }
		void setDI(int i) { dCache = new Integer(i); }

		Thev getSThev() { return (Thev)sCache; }
		Thev getDThev() { return (Thev)dCache; }
		public Trans getSTrans() { return (Trans)sCache; }
		Trans getDTrans() { return (Trans)dCache; }
		int getSI() { return ((Integer)sCache).intValue(); }
		int getDI() { return ((Integer)dCache).intValue(); }

		int hashTerms() { return source.index ^ drain.index; }
        
        public int getBaseType() { return tType & 0x07; }
        public String describeBaseType() { return transistorType[getBaseType()]; }
        public String describeState() { return states[state]; }
        public Node getGate() { return (Node)gate; }
        public Node getSource() { return source; }
        public Node getDrain() { return drain; }
        public int getX() { return x; }
        public int getY() { return y; }
        public double[] getResists() {
            return new double[] { r.rStatic, r.dynRes[R_HIGH], r.dynRes[R_LOW] };
        }
        public long getLength() { return r.length; }
        public long getWidth() { return r.width; }
        /**
         * figure what's on the *other* terminal node of a transistor
         */
        public Node getOtherNode(SimAPI.Node n) { return drain == n ? source : drain; }
        public Trans getLink() { return tLink; }
        public Collection<SimAPI.Trans> getGateList() {
            Collection<SimAPI.Trans> result = null;
    		if ((tType & GATELIST) != 0) {
                result = new ArrayList<SimAPI.Trans>();
    			for(Trans t = (Trans)gate; t != null; t = t.getSTrans())
                    result.add(t);
			}
            return result;
        }
	}
    
    /**
     * current simulated time
     */
    public long getCurDelta() { return curDelta; }
    public void setCurDelta(long curDelta) { this.curDelta = curDelta; }
    public void clearCurNode() { curNode = null; }
    
	/**
	 * Set the firstCall flags.  Used when moving back to time 0.
	 */
	public void reInit() {
        getModel().reInit();
    }

	/**
	 * Back the event queues up to time 'bTime'.  This is the opposite of
	 * advancing the simulation time.  Mark all pending events as PENDING,
	 * and re-enqueue them according to their creation-time (nTime - delay).
	 */
	public void backSimTime(long bTime, int isInc) {
        getModel().backSimTime(bTime, isInc);
    }
    
    public void printPendingEvents() {
        getModel().printPendingEvents();
    }

	public boolean step(long stopTime,
            Collection<SimAPI.Node> xInputs,
            Collection<SimAPI.Node> hInputs,
            Collection<SimAPI.Node> lInputs,
            Collection<SimAPI.Node> uInputs) {
        return getModel().step(stopTime, xInputs, hInputs, lInputs, uInputs);
    }
    
    public long getLambdaCM() {
        return getConfig().lambdaCM;
    }
    
    public long getDecay() { return tDecay; }
    public void setDecay(long decay) { tDecay = decay; }
    public int getUnitDelay() { return tUnitDelay; }
    public void setUnitDelay(int unitDelay) { tUnitDelay = unitDelay; }
    public void setDebug(int irDebug)  { this.irDebug = irDebug; }

	private static class TranResist
	{
		/** dynamic resistances [R_LOW - R_MAX] */	float  [] dynRes;
		/** static resistance of transistor */		float  rStatic;

		TranResist() { dynRes = new float[2]; }
	};

	public static class Resists extends TranResist
	{
		/** transistor size in centimicrons */		long  width, length;
	};

	public static class HistEnt implements SimAPI.HistEnt
	{
		/** next transition in history */			HistEnt  next;
		/** delay from input */						short    delay;
		/** rise/fall time */						short    rTime;
		/** punt time */							short    pTime;
		/** time of transition */					long     hTime;
		/** 1 if node became an input */			boolean  inp;
		/** 1 if this event was punted */			boolean  punt;
		/** value: HIGH, LOW, or X */				byte     val;

		public HistEnt getNextHist()
		{
			HistEnt h;
			HistEnt p = this;
			for(h = p.next; h.punt; h = h.next) ;
			return h;
		}
        
        public long getTime() { return hTime; }
        public byte getVal() { return val; }
	};

	/* resists are in ohms, caps in pf */
	public static class Thev
	{
														Object   link;
		/** flags defined above */						int      flags;
		/** capacitance charged low */					Range    cLow;
		/** capacitance charged high */					Range    cHigh;
		/** resistance pulling up to Vdd */				Range    rUp;
		/** resistance pulling down to GND */			Range    rDown;
		/** resist. of present (parallel) xtor(s) */	Range    req;
		/** normalized voltage range (0-1) */			Range    v;
		/** minimum resistance to any driver */			double   rMin;
		/** minimum resistance to dominant driver */	double   rDom;
		/** maximum resistance to dominant driver */	double   rMax;
		/** Adjusted non-switching capacitance */		double   cA;
		/** Adjusted total capacitance */				double   cD;
		/** Elmore delay (psec) */						double   tauD;
		/** 1st order time-constant (psec) */			double   tauA;
		/** 2nd order time-constant (psec) */			double   tauP;
		/** input transition = (input_tau) * Rin */		double   tIn;
		/** user specified low->high delay (DELTA) */	short    tpLH;
		/** user specified high->low delay (DELTA) */	short    tpHL;
		/** steady-state value calculated (H, L, X) */	char     finall;
		/** if tau calculated, == dominant voltage */	char     tauDone;
		/** if tauP calculated, == dominant voltage */	char     tauPDone;

		Thev()
		{
			cLow = new Range(0, 0);
			cHigh = new Range(0, 0);
			rUp = new Range(Sim.LARGE, Sim.LARGE);
			rDown = new Range(Sim.LARGE, Sim.LARGE);
			req = new Range(Sim.LARGE, Sim.LARGE);
			v = new Range(1, 0);
			setN(null);
			flags		= 0;
			rMin		= Sim.LARGE;
			rDom		= Sim.LARGE;
			rMax		= Sim.LARGE;
			cA			= 0.0;
			cD			= 0.0;
			tauD		= 0.0;
			tauA		= 0.0;
			tauP		= 0.0;
			tIn			= Sim.SMALL;
			tpLH		= 0;
			tpHL		= 0;
			finall		= X;
			tauDone		= N_POTS;
			tauPDone	= N_POTS;
		}

		Thev(Thev old)
		{
			link = old.link;
			flags = old.flags;
			cLow = new Range(old.cLow);
			cHigh = new Range(old.cHigh);
			rUp = new Range(old.rUp);
			rDown = new Range(old.rDown);
			req = new Range(old.req);
			v = new Range(old.v);
			rMin = old.rMin;
			rDom = old.rDom;
			rMax = old.rMax;
			cA = old.cA;
			cD = old.cD;
			tauD = old.tauD;
			tauA = old.tauA;
			tauP = old.tauP;
			tIn = old.tIn;
			tpLH = old.tpLH;
			tpHL = old.tpHL;
			finall = old.finall;
			tauDone = old.tauDone;
			tauPDone = old.tauPDone;
		}

		void setT(Thev t) { link = t; }
		void setN(Node n) { link = n; }

		Thev getT() { return (Thev)link; }
		Node getN() { return (Node)link; }
	};

	public static class Range
	{
		double  min;
		double  max;

		Range() {}

		Range(double min, double max)
		{
			this.min = min;
			this.max = max;
		}

		Range(Range r)
		{
			min = r.min;
			max = r.max;
		}
	};


	/** a huge time */								private static final long MAX_TIME	= 0x0FFFFFFFFFFFFFFFL;

	/** A small number */							public static final double SMALL	= 1E-15;
	/** A large number */							public static final double LARGE	= 1E15;
	/** R > LIMIT are considered infinite */		public static final double LIMIT	= 1E8;

	/** number of transistor types defined */		public static final int NTTYPES     = 4;
	static String [] transistorType =
	{
		"n-channel",
		"p-channel",
		"depletion",
		"resistor"
	};

	public static String    vChars = "0XX1";
	public static String [] states = { "OFF", "ON", "UKNOWN", "WEAK" };

	public static final int	MAX_ERRS	= 20;

	/** this is probably sufficient per stage */							private static final int	MAX_PARALLEL	= 30;

	/** power supply node */												public  Node    powerNode;
	/** ground supply node */												public  Node    groundNode;

	/** number of actual nodes */											public  int     numNodes;
	/** number of aliased nodes */											public  int     numAliases;
	/** number of txtors indexed by type */									private int []  numTrans = new int[NTTYPES];
	/** number of transistors "or"ed */										private int []  numOred = new int[NTTYPES];

	/** list of capacitor-transistors */									public  Trans   tCap = null;

	/** # of errors found in sim file */									private int     numErrors = 0;

	/** list of transistors just read */									private List<Trans> readTransistorList;
	public  Trans [] parallelTransistors = new Trans[MAX_PARALLEL];

	/** pointer to dummy hist-entry that serves as tail for all nodes */	private HistEnt lastHist;
	public  int      numEdges;
	public  int      numPunted;
	public  int      numConsPunted;
	public  long     maxTime;

	/** current simulated time */											public  long    curDelta;
	/** node that belongs to current event */								public  Node    curNode;
	/** number of current event */											public  long    nEvent;

	/** if nonzero, all transactions take this DELAY-units */				public  int     tUnitDelay = 0;
	/** number of DELAY-units after which undriven nodes decay to X */		public  long    tDecay = 0;

	private boolean  parallelWarning = false;
	private HashMap<String,Node>  nodeHash;
	private List<Node> nodeList;
	private int      nodeIndexCounter;
	private boolean  warnVdd, warnGnd;
	public  int      tReport = 0;

	private Eval     theModel;
	private Config   theConfig;
    SimAPI.Analyzer theAnalyzer;
    int irDebug;
    /** true if using the delayed X model, false if using the old fast-propagating X model. */
    boolean isDelayedX;


	public Sim(int irDebug, String steppingModel, URL parameterURL, boolean isDelayedX)
	{
        this.irDebug = irDebug;
        this.isDelayedX = isDelayedX;

		// initialize the model
		if (steppingModel.equals("Linear")) theModel = new SStep(this); else
		{
			if (!steppingModel.equals("RC"))
				System.out.println("Unknown stepping model: " + steppingModel + " using RC");
			theModel = new NewRStep(this);
		}

		// read the configuration file
		theConfig = new Config();
        if (parameterURL != null) {
            theConfig.loadConfig(parameterURL);
        }
        initNetwork();
	}
    
    public void setAnalyzer(SimAPI.Analyzer analyzer) {
        theAnalyzer = analyzer;
    }

	public Config getConfig() { return theConfig; }

	public Eval getModel() { return theModel; }

	public void setModel(boolean rc)
	{
		if (rc) theModel = new NewRStep(this); else
			theModel = new SStep(this);
		theModel.initEvent();
	}

	private void badArgCount(String fileName, LineNumberReader lineReader, String [] strings)
	{
		reportError(fileName, lineReader.getLineNumber(), "Wrong number of args for '" + strings[0] + "'");
		for(int i=0; i<strings.length; i++) System.out.print(" " + strings[i]);
		System.out.println();
		checkErrs(fileName);
	}

	public static void reportError(String filename, int lineno, String msg)
	{
		System.out.println("(" + filename + "," + lineno + "): " + msg);
	}

	/**
	 * Returns TRUE if there have been too many errors and the activity should be stopped.
	 */
	private boolean checkErrs(String fileName)
	{
		numErrors++;
		if (numErrors > MAX_ERRS)
		{
			System.out.println("Too many errors in sim file <" + fileName + ">");
			return true;
		}
		return false;
	}

	/**
	 * Convert deltas to ns.
	 */
	static double deltaToNS(long d) { return (double)d / (double)resolutionScale; }

	/**
	 * Convert deltas to ps.
	 */
	static double deltaToPS(long d) { return (double)(d * 1000) / resolutionScale; }

	/**
	 * Convert ns to deltas.
	 */
	static long nsToDelta(double d) { return (long)(d * resolutionScale); }

	/**
	 * Convert ps to deltas.
	 */
	static long psToDelta(double d) { return (long)(d / 1000 * resolutionScale); }
	/**
	 * Convert ps to ns
	 */
	static double psToNS(double d) { return d * 0.001; }

	/**
	 * figure what's on the *other* terminal node of a transistor
	 */
	static Node otherNode(Trans t, Node n) { return t.drain == n ? t.source : t.drain; }

	static int baseType(int t) { return t & 0x07; }

	static int inputNumber(int flg) { return ((flg & INPUT_MASK) >> 12); }

	/**
	 * combine 2 resistors in parallel
	 */
	static double combine(double r1, double r2) { return (r1 * r2) / (r1 + r2); }

	/**
	 * Traverse the transistor list and add the node connection-list.  We have
	 * to be careful with ALIASed nodes.  Note that transistors with source/drain
	 * connected VDD and GND nodes are not linked.
	 */
	private Node connectTransistors()
	{
		Node ndList = null;

		for(Trans t : readTransistorList)
		{
			Node gate = null, src = null, drn = null;
			for(gate = (Node)t.gate; (gate.nFlags & ALIAS) != 0; gate = gate.nLink) ;
			for(src = t.source; (src.nFlags & ALIAS) != 0; src = src.nLink) ;
			for(drn = t.drain; (drn.nFlags & ALIAS) != 0; drn = drn.nLink) ;

			t.gate = gate;
			t.source = src;
			t.drain = drn;

			int type = t.tType;
			t.state = (byte)((type & ALWAYSON) != 0 ? WEAK : UNKNOWN);
			t.tFlags = 0;

			numTrans[baseType(type)]++;
			if (src == drn || (src.nFlags & drn.nFlags & POWER_RAIL) != 0)
			{
				// transistor is just a capacitor.
				// Transistors that have their drain/source shorted are NOT connected
				// to the network, they are instead linked as a doubly linked list
				// using the scache/dcache fields.
				t.tType |= TCAP;
				t.setDTrans(tCap);
				t.setSTrans(tCap.getSTrans());
				tCap.getSTrans().setDTrans(t);
				tCap.setSTrans(t);
				tCap.x++;
			} else
			{
				// do not connect gate if ALWAYSON since they do not matter
				if ((t.tType & ALWAYSON) == 0)
				{
					((Node)t.gate).nGateList.add(t);
				}

				if ((src.nFlags & POWER_RAIL) == 0)
				{
					src.nTermList.add(t);
					ndList = linkToList(src, ndList);
				}
				if ((drn.nFlags & POWER_RAIL) == 0)
				{
					drn.nTermList.add(t);
					ndList = linkToList(drn, ndList);
				}
			}
		}

		return ndList;
	}

	/**
	 * if VISITED is not set in in n.nFlags, Link n to the head of list
	 * using the temporary entry in the node structure.  This is
	 * used during net read-in/change to build lists of affected nodes.
	 */
	private Node linkToList(Node n, Node list)
	{
		if ((n.nFlags & VISITED) == 0)
		{
			n.nFlags |= VISITED;
			n.setNext(list);
			list = n;
		}
		return list;
	}

	/**
	 * node area and perimeter info (N sim command).
	 */
	private void nodeInfo(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length != 8)
		{
			badArgCount(fileName, lineReader, targ);
			return;
		}

		Node n = getNode(targ[1]);

		n.nCap += Electric.atof(targ[4]) * (theConfig.CMA * theConfig.lambdaSquared) +
			Electric.atof(targ[5]) * (theConfig.CPA * theConfig.lambdaSquared) +
			Electric.atof(targ[6]) * (theConfig.CDA * theConfig.lambdaSquared) +
			Electric.atof(targ[7]) * 2.0f * (theConfig.CDP * theConfig.lambda);
	}

	/**
	 * new format node area and perimeter info (M sim command).
	 */
	private void nNodeInfo(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length != 14)
		{
			badArgCount(fileName, lineReader, targ);
			return;
		}

		Node n = getNode(targ[1]);

		n.nCap += Electric.atof(targ[4]) * (theConfig.CM2A * theConfig.lambdaSquared) +
			Electric.atof(targ[5]) * 2.0 * (theConfig.CM2P * theConfig.lambda) +
			Electric.atof(targ[6]) * (theConfig.CMA * theConfig.lambdaSquared) +
			Electric.atof(targ[7]) * 2.0 * (theConfig.CMP * theConfig.lambda) +
			Electric.atof(targ[8]) * (theConfig.CPA * theConfig.lambdaSquared) +
			Electric.atof(targ[9]) * 2.0 * (theConfig.CPP * theConfig.lambda) +
			Electric.atof(targ[10]) * (theConfig.CDA * theConfig.lambda) +
			Electric.atof(targ[11]) * 2.0 * (theConfig.CDP * theConfig.lambda) +
			Electric.atof(targ[12]) * (theConfig.CPDA * theConfig.lambdaSquared) +
			Electric.atof(targ[13]) * 2.0 * (theConfig.CPDP * theConfig.lambda);
	}

	/**
	 * new transistor.  Implant specifies type.
	 * AreaPos specifies the argument number that contains the area (if any).
	 */
	private void newTrans(int implant, String [] targ, String fileName, LineNumberReader lineReader)
	{
		// create new transistor
		Trans t = new Trans();
		t.tType = (byte)implant;

		if (implant == RESIST)
		{
			if (targ.length != 4)
			{
				badArgCount(fileName, lineReader, targ);
				return;
			}

			t.gate = powerNode;
			t.source = getNode(targ[1]);
			t.drain = getNode(targ[2]);

			long length = (long)(Electric.atof(targ[3]) * theConfig.lambdaCM);
			t.r = theConfig.rEquiv(implant, 0, length);

		} else
		{
			if (targ.length != 11)
			{
				badArgCount(fileName, lineReader, targ);
				return;
			}

			t.gate = getNode(targ[1]);
			t.source = getNode(targ[2]);
			t.drain = getNode(targ[3]);

			long length = (long)(Electric.atof(targ[4]) * theConfig.lambdaCM);
			long width = (long)(Electric.atof(targ[5]) * theConfig.lambdaCM);
			if (width <= 0 || length <= 0)
			{
				reportError(fileName, lineReader.getLineNumber(),
					"Bad transistor width=" + width + " or length=" + length);
				return;
			}
			((Node)t.gate).nCap += length * width * theConfig.CTGA;
			t.r = theConfig.rEquiv(implant, width, length);

			t.x = Electric.atoi(targ[6]);
			t.y = Electric.atoi(targ[7]);

			// parse area and perimeter
			for (int i = 8; i < targ.length; i++)
			{
				int aIsPos = targ[i].indexOf("A_");
				int pIsPos = targ[i].indexOf("P_");
				if (aIsPos >= 0 && pIsPos >= 0)
				{
					int a = Electric.atoi(targ[i].substring(aIsPos+2));
					int p = Electric.atoi(targ[i].substring(pIsPos+2));
					char type = targ[i].charAt(0);
					int asrc = 0, adrn = 0, psrc = 0, pdrn = 0;
					if (type == 's')
					{
						asrc = a;
						psrc = p;
					} else if (type == 'd')
					{
						adrn = a;
						pdrn = p;
					}
					if (implant == PCHAN)
					{
						t.source.nCap += asrc * theConfig.lambda * theConfig.lambda * theConfig.CPDA +
							psrc * theConfig.lambda * theConfig.CPDP;
						t.drain.nCap += adrn * theConfig.lambda * theConfig.lambda * theConfig.CPDA +
							pdrn * theConfig.lambda * theConfig.CPDP;
					} else if (implant == NCHAN || implant == DEP)
					{
						t.source.nCap += asrc * theConfig.lambda * theConfig.lambda * theConfig.CDA +
							psrc * theConfig.lambda * theConfig.CDP;
						t.drain.nCap += adrn * theConfig.lambda * theConfig.lambda * theConfig.CDA +
							pdrn * theConfig.lambda * theConfig.CDP;
					}
				}
			}
		}

		// link it to the list
		readTransistorList.add(t);
	}

	/**
	 * accept a bunch of aliases for a node (= sim command).
	 */
	private void alias(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length < 3)
		{
			badArgCount(fileName, lineReader, targ);
			return;
		}

		Node n = getNode(targ[1]);

		for(int i = 2; i < targ.length; i++)
		{
			Node m = getNode(targ[i]);
			if (m == n) continue;

			if ((m.nFlags & POWER_RAIL) != 0)
			{
				Node swap = m;   m = n;   n = swap;
			}

			if ((m.nFlags & POWER_RAIL) != 0)
			{
				reportError(fileName, lineReader.getLineNumber(), "Can't alias the power supplies");
				continue;
			}

			n.nCap += m.nCap;
			m.nLink = n;
			m.nFlags |= ALIAS;
			m.nCap = 0;
			numNodes--;
			numAliases++;
		}
	}

	/**
	 * node threshold voltages (t sim command).
	 */
	private void nThresh(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length != 4)
		{
			badArgCount(fileName, lineReader, targ);
			return;
		}

		Node n = getNode(targ[1]);
		n.vLow = (float)Electric.atof(targ[2]);
		n.vHigh = (float)Electric.atof(targ[3]);
	}

	/**
	 * User delay for a node (D sim command).
	 */
	private void nDelay(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length != 4)
		{
			badArgCount(fileName, lineReader, targ);
			return;
		}

		Node n = getNode(targ[1]);
		n.nFlags |= USERDELAY;
		n.tpLH = (short)nsToDelta(Electric.atof(targ[2]));
		n.tpHL = (short)nsToDelta(Electric.atof(targ[3]));
	}

	/**
	 * add capacitance to a node (c sim command).
	 */
	private void nCap(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length == 3)
		{
			Node n = getNode(targ[1]);
			n.nCap += (float)Electric.atof(targ[2]);
		} else if (targ.length == 4)
		{
			// two terminal caps
			float cap = (float)(Electric.atof(targ[3]) / 1000);		// ff to pf conversion
			Node n = getNode(targ[1]);
			Node m = getNode(targ[2]);
			if (n != m)
			{
				// add cap to both nodes
				if (m != groundNode)	m.nCap += cap;
				if (n != groundNode)	n.nCap += cap;
			} else if (n == groundNode)
			{
				// same node, only GND makes sense
				n.nCap += cap;
			}
		} else
		{
			badArgCount(fileName, lineReader, targ);
		}
	}

    public String[] parseLine(String line) {
        return parseLine(line, false);
    }
    
	/**
	 * parse input line into tokens, filling up carg and setting targc
	 * @param expand true to expand iterators.  For example, the
	 * string "out.{1:10}" expands into ten arguments "out.1", ..., "out.10".
	 * The string can contain multiple iterators which will be expanded
	 * independently, e.g., "out{1:10}{1:20:2}" expands into 100 arguments.
	 */
	public static String [] parseLine(String line, boolean expand)
	{
		StringTokenizer st = new StringTokenizer(line, " \t");
		int total = st.countTokens();
		String [] strings = new String[total];
		boolean iteratorFound = false;
		for(int i=0; i<total; i++)
		{
			strings[i] = st.nextToken();
			if (strings[i].indexOf('{') >= 0) iteratorFound = true;
		}

		// expand iterated keywords if requested
		if (iteratorFound && expand)
		{
			// expand iterators
			List<String> listOfStrings = new ArrayList<String>();
			for(int i=0; i<total; i++)
			{
				if (expand(strings[i], listOfStrings))
				{
					System.out.println("Invalid iterator: " + strings[i]);
					listOfStrings.add(strings[i]);
					continue;
				}
			}
			strings = new String[listOfStrings.size()];
			for(int i=0; i<listOfStrings.size(); i++)
				strings[i] = listOfStrings.get(i);
		}
		return strings;
	}

	private static boolean expand(String arg, List<String> expanded)
	{
		int itStart = arg.indexOf('{');
		if (itStart < 0)
		{
			// no iterator here: just add the string
			expanded.add(arg);
			return false;
		}
		int itEnd = arg.indexOf('}', itStart);
		if (itEnd < 0) return true;

		// parse range of values
		String iterator = arg.substring(itStart+1, itEnd);
		int firstColon = iterator.indexOf(':');
		if (firstColon < 0) return true;
		int secondColon = iterator.indexOf(':', firstColon);
		int start = Electric.atoi(iterator);
		int stop = Electric.atoi(iterator.substring(firstColon+1));
		int step = 1;
		if (secondColon >= 0) step = Electric.atoi(iterator.substring(secondColon+1));

		// figure out correct step size
		if (step == 0) step = 1; else
			if (step < 0) step = -step;
		if (start > stop) step = -step;

		// expand the iterator
		String prefix = arg.substring(0, itStart);
		String suffix = arg.substring(itEnd+1);
		while ((step > 0 && start <= stop) || (step < 0 && start >= stop))
		{
			String full = prefix + start + suffix;
			if (expand(full, expanded)) return true;
			start += step;
		}
		return false;
	}
    
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
    public void putTransistor(String gateName, String sourceName, String drainName,
            double gateLength, double gateWidth,
            double activeArea, double activePerim,
            double centerX, double centerY,
            boolean isNTypeTransistor) {
        Trans t = new Trans();

        t.gate = getNode(gateName);
        t.source = getNode(sourceName);
        t.drain = getNode(drainName);
        long length = (long) (gateLength * theConfig.lambdaCM);
        long width = (long) (gateWidth * theConfig.lambdaCM);
        if (width <= 0 || length <= 0) {
            System.out.println("Bad transistor width=" + width + " or length=" + length);
            return;
        }
        t.x = (int) centerX;
        t.y = (int) centerY;

        ((Node) t.gate).nCap += length * width * theConfig.CTGA;

        if (isNTypeTransistor) {
            t.tType = NCHAN;

            t.source.nCap += activeArea * theConfig.lambda * theConfig.lambda * theConfig.CDA
                    + activePerim * theConfig.lambda * theConfig.CDP;
            t.drain.nCap += activeArea * theConfig.lambda * theConfig.lambda * theConfig.CDA
                    + activePerim * theConfig.lambda * theConfig.CDP;
            t.r = theConfig.rEquiv(NCHAN, width, length);
        } else {
            t.tType = PCHAN;

            t.source.nCap += activeArea * theConfig.lambda * theConfig.lambda * theConfig.CPDA
                    + activePerim * theConfig.lambda * theConfig.CPDP;
            t.drain.nCap += activeArea * theConfig.lambda * theConfig.lambda * theConfig.CPDA
                    + activePerim * theConfig.lambda * theConfig.CPDP;
            t.r = theConfig.rEquiv(PCHAN, width, length);
        }
        readTransistorList.add(t);

    }
    
    /**
     * Put resistor into the circuit
     * @param net1 name of first terminal network
     * @param net2 name of second terminal network
     * @param resistance resistance (ohm)
     */
    public void putResistor(String net1, String net2, double resistance) {
        Trans t = new Trans();
        t.tType = RESIST;
        t.gate = powerNode;
        t.source = getNode(net1);
        t.drain = getNode(net2);
        t.r = theConfig.rEquiv(RESIST, 0, (long) (resistance * theConfig.lambdaCM));

        // link it to the list
        readTransistorList.add(t);
    }

    /**
     * Put capacitor into the circuit
     * @param net1 name of first terminal network
     * @param net2 name of second terminal network
     * @param capacitance capacitance (pf)
     */
    public void putCapacitor(String net1, String net2, double capacitance) {
        float cap = (float) (capacitance / 1000);		// ff to pf conversion
        Node n = getNode(net1);
        Node m = getNode(net2);
        if (n != m) {
            // add cap to both nodes
            if (m != groundNode) {
                m.nCap += cap;
            }
            if (n != groundNode) {
                n.nCap += cap;
            }
        } else if (n == groundNode) {
            // same node, only GND makes sense
            n.nCap += cap;
        }
    }

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
     * @param simReader Reader of .sim file
     * @param fileName file name for error messages
     * @return number of errors
     */
    public int inputSim(Reader simReader, String fileName) throws IOException
	{
		// read the file
		boolean rError = false;
		boolean aError = false;
//		String fileName = simFileURL.getFile();
//		Electric.startProgressDialog("import", fileName);
//		try
//		{
//			URLConnection urlCon = simFileURL.openConnection();
//			String contentLength = urlCon.getHeaderField("content-length");
//			long fileLength = -1;
//			try {
//				fileLength = Long.parseLong(contentLength);
//			} catch (Exception e) {}
//			long readSoFar = 0;
//			InputStream inputStream = urlCon.getInputStream();
//			InputStreamReader is = new InputStreamReader(inputStream);
//			LineNumberReader lineReader = new LineNumberReader(is);
            LineNumberReader lineReader = new LineNumberReader(simReader);
			for(;;)
			{
				String line = lineReader.readLine();
				if (line == null) break;
//				readSoFar += line.length() + 1;
//				Electric.setProgressValue((int)(readSoFar * 100 / fileLength));
				String [] targ = parseLine(line, false);
				if (targ.length == 0) continue;
				char firstCh = targ[0].charAt(0);
				switch (firstCh)
				{
					case '|':
						if (lineReader.getLineNumber() > 1) break;
						if (targ.length >= 2)
						{
							double lmbd = Electric.atof(targ[2]) / 100.0;
							if (lmbd != theConfig.lambda)
							{
								System.out.println("WARNING: sim file lambda (" + lmbd + "u) != config lambda (" +
									theConfig.lambda + "u), using config lambda");
							}
						}
						if (targ.length >= 6)
						{
							if (theConfig.CDA == 0.0 || theConfig.CDP == 0.0 ||
								theConfig.CPDA == 0.0 || theConfig.CPDP == 0.0)
							{
								System.out.println("Warning: missing area/perim cap values are zero");
							}
						}
						break;
					case 'e':
					case 'n':
						newTrans(NCHAN, targ, fileName, lineReader);
						break;
					case 'p':
						newTrans(PCHAN, targ, fileName, lineReader);
						break;
					case 'd':
						newTrans(DEP, targ, fileName, lineReader);
						break;
					case 'r':
						newTrans(RESIST, targ, fileName, lineReader);
						break;
					case 'N':
						nodeInfo(targ, fileName, lineReader);
						break;
					case 'M':
						nNodeInfo(targ, fileName, lineReader);
						break;
					case 'c':
					case 'C':
						nCap(targ, fileName, lineReader);
						break;
					case '=':
						alias(targ, fileName, lineReader);
						break;
					case 't':
						nThresh(targ, fileName, lineReader);
						break;
					case 'D':
						nDelay(targ, fileName, lineReader);
						break;
					case 'R':
						if (!rError)	// only warn about this 1 time
						{
							System.out.println(fileName + "Ignoring lumped-resistance ('R' construct)");
							rError = true;
						}
						break;
					case 'A':
						if (!aError)	// only warn about this 1 time
						{
							System.out.println(fileName + "Ignoring attribute-line ('A' construct)");
							aError = true;
						}
						break;
					default:
						reportError(fileName, lineReader.getLineNumber(), "Unrecognized input line (" + targ[0] + ")");
						if (checkErrs(fileName)) return numErrors;
				}
			}
//			inputStream.close();
//		} catch (IOException e)
//		{
//			System.out.println("Error reading file");
//		}
//        Electric.stopProgressDialog();
		System.out.println("Loaded circuit, lambda=" + theConfig.lambda + "u");
        return numErrors;
	}

	private void initNetwork()
	{
		readTransistorList = new ArrayList<Trans>();
		nodeHash = new HashMap<String,Node>();
		nodeList = new ArrayList<Node>();
		nodeIndexCounter = 1;
		warnVdd = warnGnd = false;
		maxTime = MAX_TIME;

		// initialize counts
		for(int i = 0; i < NTTYPES; i++)
			numTrans[i] = 0;
		numNodes = numAliases = 0;
		initHist();

		// initialize globals
		numEdges = 0;
		numPunted = 0;
		numConsPunted = 0;

		powerNode = getNode("Vdd");
		powerNode.nPot = HIGH;
		powerNode.nFlags |= (INPUT | POWER_RAIL);
		powerNode.head.inp = true;
		powerNode.head.val = HIGH;
		powerNode.head.punt = false;
		powerNode.head.hTime = 0;
		powerNode.head.rTime = powerNode.head.delay = 0;
		powerNode.head.next = lastHist;
		powerNode.curr = powerNode.head;

		groundNode = getNode("Gnd");
		groundNode.nPot = LOW;
		groundNode.nFlags |= (INPUT | POWER_RAIL);
		groundNode.head.inp = true;
		groundNode.head.val = LOW;
		groundNode.head.punt = false;
		groundNode.head.hTime = 0;
		groundNode.head.rTime = groundNode.head.delay = 0;
		groundNode.head.next = lastHist;
		groundNode.curr = groundNode.head;

		tCap = new Trans();
		tCap.source = null;
		tCap.drain = null;
		tCap.setSTrans(tCap);
		tCap.setDTrans(tCap);
		tCap.x = 0;

		numErrors = 0;
    }
    
    public void finishNetwork() {

		// connect all txtors to corresponding nodes
		connectNetwork();

		// sort the signal names
		Collections.sort(getNodeList(), new NodesByName());

		theModel.initEvent();
	}

    /**
     * Get lambda value in nanometers
     * @return lambda in nanometers
     */
    public double getLambda() {
        return theConfig.lambda;
    }
    
	private static class NodesByName implements Comparator<Node>
	{
		public int compare(Node n1, Node n2)
		{
			return n1.nName.compareToIgnoreCase(n2.nName);
		}
	}

	private void connectNetwork()
	{
		Node ndList = connectTransistors();
		makeParallel(ndList);

		// display information about circuit
		String infstr = numNodes + " nodes";
		if (numAliases != 0)
			infstr += ", " + numAliases + " aliases";
		for(int i = 0; i < NTTYPES; i++)
		{
			if (numTrans[i] == 0) continue;
			infstr += ", " + numTrans[i] + " " + transistorType[i] + " transistors";
			if (numOred[i] != 0)
			{
				infstr += " (" + numOred[i] + " parallel)";
			}
		}
		if (tCap.x != 0)
			infstr += " (" + tCap.x + " shorted)";
		System.out.println(infstr);
	}

	public boolean      withDriven;		/* TRUE if stage is driven by some input */

	/**
	 * Build a linked-list of nodes (using nLink entry in Node structure)
	 * which are electrically connected to node 'n'.  No special order
	 * is required so tree walk is performed non-recursively by doing a
	 * breath-first traversal.  The value caches for each transistor we
	 * come across are reset here.  Loops are broken at an arbitrary point
	 * and parallel transistors are identified.
	 */
	public void buildConnList(Node n)
	{
		int nPar = 0;

		n.nFlags &= ~VISITED;
		withDriven = false;

		Node next = n;
		Node thisOne = n.nLink = n;
		do
		{
			for(Trans t : thisOne.nTermList)
			{
				if (t.state == OFF) continue;
				if ((t.tFlags & CROSSED) != 0)	// Each transistor is crossed twice
				{
					t.tFlags &= ~CROSSED;
					continue;
				}
				t.setSThev(null);
				t.setDThev(null);

				Node other = otherNode(t, thisOne);
				if ((other.nFlags & INPUT) != 0)
				{
					withDriven = true;
					continue;
				}

				t.tFlags |= CROSSED;		// Crossing trans 1st time

				if (other.nLink == null)		// New node in this stage
				{
					other.nFlags &= ~VISITED;
					other.nLink = n;
					next.nLink = other;
					next = other;
					other.setTrans(t);		// we reach other through t
				}
				else if (!(theModel instanceof NewRStep))
					continue;
				else if (other.getTrans().hashTerms() == t.hashTerms())
				{					    // parallel transistors
					Trans tran = other.getTrans();
					if ((tran.tFlags & PARALLEL) != 0)
						t.setDTrans(parallelTransistors[tran.nPar]);
					else
					{
						if (nPar >= MAX_PARALLEL)
						{
							if (!parallelWarning)
							{
								System.out.println("There are too many transistors in parallel (> " + MAX_PARALLEL + ")");
								System.out.println("Simulation results may be inaccurate, to fix this you may have to");
								System.out.println("increase 'MAX_PARALLEL' in 'Sim.java'.");
								System.out.println("Note: This condition often occurs when Vdd or Gnd are not connected to all cells.");
								if (thisOne.nName != null && other.nName != null)
									System.out.println("      Check the vicinity of the following 2 nodes: " + thisOne.nName + " " + other.nName);
								parallelWarning = true;
							}
							t.tFlags |= PBROKEN;		// simply ignore it
							continue;
						}
						tran.nPar = (byte)(nPar++);
						tran.tFlags |= PARALLEL;
					}
					parallelTransistors[tran.nPar] = t;
					t.tFlags |= PBROKEN;
				} else
				{					// we have a loop, break it
					t.tFlags |= BROKEN;
				}
			}
		}
		while((thisOne = thisOne.nLink) != n);

		next.nLink = null;			// terminate connection list
	}

	/********************************************** HISTORY *******************************************/

	private void initHist()
	{
		HistEnt dummy = new HistEnt();
		lastHist = dummy;
		dummy.next = lastHist;
		dummy.hTime = maxTime;
		dummy.val = X;
		dummy.inp = true;
		dummy.punt = false;
		dummy.delay = dummy.rTime = 0;
	}

	/**
	 * Add a new entry to the history list.  Update curr to point to this change.
	 */
	public void addHist(Node node, int value, boolean inp, long time, long delay, long rTime)
	{
		numEdges++;
		HistEnt curr = node.curr;

		while(curr.next.punt)		// skip past any punted events
			curr = curr.next;

		HistEnt newH = new HistEnt();
		if (newH == null) return;

		newH.next = curr.next;
		newH.hTime = time;
		newH.val = (byte)value;
		newH.inp = inp;
		newH.punt = false;
		newH.delay = (short)delay;
		newH.rTime = (short)rTime;
		node.curr = curr.next = newH;
	}

	/**
	 * Add a punted event to the history list for the node.  Consecutive punted
	 * events are kept in punted-order, so that h.pTime < h.next.pTime.
	 * Adding a punted event does not change the current pointer, which always
	 * points to the last "effective" node change.
	 */
	public void addPunted(Node node, Eval.Event ev, long tim)
	{
		HistEnt h = node.curr;

		numPunted++;

		HistEnt newP = new HistEnt();

		newP.hTime = ev.nTime;
		newP.val = ev.eval;
		newP.inp = false;
		newP.punt = true;
		newP.delay = (short)ev.delay;
		newP.rTime = ev.rTime;
		newP.pTime = (short)(newP.hTime - tim);

		if (h.next.punt)		// there are some punted events already
		{
			numConsPunted++;
			do { h = h.next; } while(h.next.punt);
		}

		newP.next = h.next;
		h.next = newP;
	}

	public void backToTime(SimAPI.Node nd_)
	{
        Node nd = (Node)nd_;
		if ((nd.nFlags & (ALIAS | MERGED)) != 0) return;

		HistEnt h = nd.head;
		HistEnt p = h.getNextHist();
		while(p.hTime < curDelta)
		{
			h = p;
			p = p.getNextHist();
		}
		nd.curr = h;

		// queue pending events
		for(p = h, h = p.next; ; p = h, h = h.next)
		{
			long qTime;

			if (h.punt)
			{
				// if already punted, skip it
				long puntTime = h.hTime - h.pTime;
				if (puntTime < curDelta) continue;

				qTime = h.hTime - h.delay;	// pending, enqueue it
				if (qTime < curDelta)
				{
					long tmp = curDelta;
					curDelta = qTime;
					theModel.enqueueEvent(nd, h.val, h.delay, h.rTime);
					curDelta = tmp;
				}
				p.next = h.next;
				h = p;
			} else
			{
				// time at which history entry was enqueued
				qTime = h.hTime - h.delay;
				if (qTime < curDelta)		// pending, enqueue it
				{
					long tmp = curDelta;
					curDelta = qTime;
					theModel.enqueueEvent(nd, h.val, h.delay, h.rTime);
					curDelta = tmp;

					p.next = h.next;		// and free it
					h = p;
				}
				else
					break;
			}
		}

		p.next = lastHist;
		p = h;
		// p now points to the 1st event in the future (to be deleted)
		if (p != lastHist)
		{
			while(h.next != lastHist)
				h = h.next;
		}

		h = nd.curr;
		nd.nPot = h.val;
		nd.setTime(h.hTime);
		if (h.inp)
			nd.nFlags |= INPUT;

		if (nd.nGateList.size() != 0)		// recompute transistor states
		{
			for(Trans t : nd.nGateList)
			{
				t.state = (byte)theModel.computeTransState(t);
			}
		}
	}

	/************************************ PARALLEL *******************************************/

	/**
	 * Run through the list of nodes, collapsing all transistors with the same
	 * gate/source/drain into a compound transistor.
	 */
	private void makeParallel(Node nList)
	{
		for( ; nList != null; nList.nFlags &= ~VISITED, nList = nList.getNext())
		{
			for(int l1 = 0; l1 < nList.nTermList.size(); l1++)
			{
				Trans t1 = nList.nTermList.get(l1);
				int type = t1.tType;
				if ((type & (GATELIST | ORED)) != 0)
					continue;	// ORED implies processed, so skip as well

				long hval = t1.hashTerms();
				for(int l2 = l1+1; l2 < nList.nTermList.size(); l2++)
				{
					Trans t2 = nList.nTermList.get(l2);
					if (t1.gate != t2.gate || t2.hashTerms() != hval ||
						type != (t2.tType & ~ORED))
							continue;

					if ((t1.tType & ORED) == 0)
					{
						Trans t3 = new Trans();
						t3.r = new Resists();
						t3.r.dynRes[R_LOW] = t1.r.dynRes[R_LOW];
						t3.r.dynRes[R_HIGH] = t1.r.dynRes[R_HIGH];
						t3.r.rStatic = t1.r.rStatic;
						t3.gate = t1.gate;
						t3.source = t1.source;
						t3.drain = t1.drain;
						t3.tType = (byte)((t1.tType & ~ORLIST) | ORED);
						t3.state = t1.state;
						t3.tFlags = t1.tFlags;
						t3.tLink = t1;
						t1.setSTrans(null);
						t1.setDTrans(t3);
						int oldGateI = ((Node)t1.gate).nGateList.indexOf(t1);
						if (oldGateI >= 0) ((Node)t1.gate).nGateList.set(oldGateI, t3);
						int oldSourceI = t1.source.nTermList.indexOf(t1);
						if (oldSourceI >= 0) t1.source.nTermList.set(oldSourceI, t3);
						int oldDrainI = t1.drain.nTermList.indexOf(t1);
						if (oldDrainI >= 0) t1.drain.nTermList.set(oldDrainI, t3);
						t1.tType |= ORLIST;
						t1 = t3;
						numOred[baseType(t1.tType)]++;
					}

					Resists r1 = t1.r, r2 = t2.r;
					r1.rStatic = (float)combine(r1.rStatic, r2.rStatic);
					r1.dynRes[R_LOW] = (float)combine(r1.dynRes[R_LOW], r2.dynRes[R_LOW]);
					r1.dynRes[R_HIGH] = (float)combine(r1.dynRes[R_HIGH], r2.dynRes[R_HIGH]);

					((Node)t2.gate).nGateList.remove(t2);	// disconnect gate
					if (t2.source == nList)		// disconnect term1
					{
						t2.drain.nTermList.remove(t2);
					} else
					{
						t2.source.nTermList.remove(t2);
					}

					// disconnect term2
					nList.nTermList.remove(t2);

					if ((t2.tType & ORED) != 0)
					{
						Trans  t;

						for(t = t2.tLink; t.getSTrans() != null; t = t.getSTrans())
							t.setDTrans(t1);
						t.setSTrans(t1.tLink);
						t1.tLink = t2.tLink;
					} else
					{
						t2.tType |= ORLIST;		// mark as part of or
						t2.setDTrans(t1);		// this is the real txtor
						t2.setSTrans(t1.tLink);	// link unto t1 list
						t1.tLink = t2;
						numOred[baseType(t1.tType)]++;
					}
				}
			}
		}
	}

}
