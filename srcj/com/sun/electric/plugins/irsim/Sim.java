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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.io.output.IRSIM;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class Sim
{
	public static class Node
	{
		/** sundries list */									Node     nlink;
		/** charge sharing event */								Event    events;
		/** list of xtors w/ gates connected to this node */	List     ngateList;
		/** list of xtors w/ src/drn connected to this node */	List     ntermList;
		/** link in hash bucket */								Node     hnext;
		/** capacitance of node in pf */						float    ncap;
		/** low logic threshold for node, normalized units */	float    vlow;
		/** high logic threshold for node, normalized units */	float    vhigh;
		/** low to high transition time in DELTA's */			short    tplh;
		/** high to low transition time in DELTA's */			short    tphl;
		/** signal in the waveform window (if displayed) */		Stimuli.DigitalSignal sig;
		/** combines time, nindex, cap, and event */			private Object   c;

		/** combines cause, punts, and tranT */					private Object   t;

		/** current potential */								short    npot;
		/** old potential (for incremental simulation). */		short    oldpot;
		/** flag word (see defs below) */						long     nflags;
		/** ascii name of node */								String   nname;

		/** combines thev, next, and tranN */					private Object   n;

		/** first entry in transition history */				HistEnt  head;
		/** ptr. to current history entry */					HistEnt  curr;
		/** special entry to avoid changing the history */		HistEnt  hchange;
		/** potential for pending AssertWhen */					short    awpot;
		/** pending asswertWhen list */							Analyzer.AssertWhen awpending;
		/** index of this node (a unique value) */				int index;

		Node(Sim theSim)
		{
			head = new HistEnt();
			hchange = new HistEnt();
			index = theSim.nodeIndexCounter++;
		}

		void setTime(long time) { c = new Long(time); }
		void setNIndex(long nIndex) { c = new Long(nIndex); }
		void setCap(float cap) { c = new Float(cap); }
		void setEvent(Event event) { c = event; }

		void setCause(Node cause) { t = cause; }
		void setPunts(HistEnt punts) { t = punts; }

		void setThev(Thev thev) { n = thev; }
		void setNext(Node next) { n = next; }
		void setTrans(Trans trans) { n = trans; }

		long getTime() { return ((Long)c).longValue(); }
		long getNIndex() { return ((Long)c).longValue(); }
		float getCap() { return ((Float)c).floatValue(); }
		Event getEvent() { return (Event)c; }

		Node getCause() { return (Node)t; }
		HistEnt getPunts() { return (HistEnt)t; }

		Thev getThev() { return (Thev)n; }
		Node getNext() { return (Node)n; }
		Trans getTrans() { return (Trans)n; }
	};

	/**
	 * find node in network
	 */
	public Node irsim_find(String name)
	{
		return (Node)nodeHash.get(name.toLowerCase());
	}

	/**
	 * Get node structure.  If not found, create a new one.
	 * If create is TRUE a new node is created and NOT entered into the table.
	 */
	public Node irsim_GetNode(String name)
	{
		Node n = irsim_find(name);
		if (n != null)
		{
			if (!name.equals(n.nname))
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
					System.out.println("Warning: Aliasing nodes '" + name + "' and '" + n.nname + "'");
			}
			while ((n.nflags & ALIAS) != 0)
				n = n.nlink;
			return n;
		}

		// allocate new node from free storage
		n = new Node(this);

		irsim_nnodes++;

		// insert node into hash table and list
		nodeHash.put(name.toLowerCase(), n);
		nodeList.add(n);

		// initialize node entries
		n.nname = name;
		n.ngateList = new ArrayList();
		n.ntermList = new ArrayList();
		n.nflags = 0;
		n.ncap = (float)MIN_CAP;
		n.vlow = (float)theConfig.irsim_LOWTHRESH;
		n.vhigh = (float)theConfig.irsim_HIGHTHRESH;
		n.setTime(0);
		n.tplh = 0;
		n.tphl = 0;
		n.setCause(null);
		n.nlink = null;
		n.events = null;
		n.npot = X;
		n.awpending = null;

		n.head = new HistEnt();
		n.head.next = irsim_last_hist;
		n.head.htime = 0;
		n.head.val = X;
		n.head.inp = false;
		n.head.punt = false;
		n.head.rtime = n.head.delay = 0;
		n.curr = n.head;

		return n;
	}

	/**
	 * Return a list of all nodes in the network.
	 */
	public List irsim_GetNodeList()
	{
		return nodeList;
	}

	public static class Trans
	{
		/** nodes to which trans is connected */	Object   gate;
		/** nodes to which trans is connected */	Node     source, drain;
		/** caches to remember src/drn values */	Object   sCache, dCache;
		/** type of transistor */					byte     ttype;
		/** cache to remember current state */		byte     state;
		/** transistor flags */						byte     tflags;
		/** index into parallel list */				byte     n_par;
		/** transistor resistances */				Resists  r;
		/** next txtor in position hash table */	Trans    tlink;
		/** position in the layout */				int      x, y;

		void setSThev(Thev r) { sCache = r; }
		void setDThev(Thev r) { dCache = r; }
		void setSTrans(Trans t) { sCache = t; }
		void setDTrans(Trans t) { dCache = t; }
		void setSI(int i) { sCache = new Integer(i); }
		void setDI(int i) { dCache = new Integer(i); }

		Thev getSThev() { return (Thev)sCache; }
		Thev getDThev() { return (Thev)dCache; }
		Trans getSTrans() { return (Trans)sCache; }
		Trans getDTrans() { return (Trans)dCache; }
		int getSI() { return ((Integer)sCache).intValue(); }
		int getDI() { return ((Integer)dCache).intValue(); }
	};

	private static class TranResist
	{
		/** dynamic resistances [R_LOW - R_MAX] */	float  [] dynres;
		/** static resistance of transistor */		float  rstatic;

		TranResist() { dynres = new float[2]; }
	};

	public static class Resists extends TranResist
	{
		/** transistor size in centimicrons */		long  width, length;
	};

	public static class HistEnt
	{
		/** next transition in history */			HistEnt  next;
		/** delay from input */						short    delay;
		/** rise/fall time */						short    rtime;
		/** punt time */							short    ptime;
		/** time of transition */					long     htime;
		/** 1 if node became an input */			boolean  inp;
		/** 1 if this event was punted */			boolean  punt;
		/** value: HIGH, LOW, or X */				byte     val;
	};

	/* resists are in ohms, caps in pf */
	public static class Thev
	{
														Object   link;
		/** flags defined above */						int      flags;
		/** capacitance charged low */					Range    Clow;
		/** capacitance charged high */					Range    Chigh;
		/** resistance pulling up to Vdd */				Range    Rup;
		/** resistance pulling down to GND */			Range    Rdown;
		/** resist. of present (parallel) xtor(s) */	Range    Req;
		/** normalized voltage range (0-1) */			Range    V;
		/** minimum resistance to any driver */			double   Rmin;
		/** minimum resistance to dominant driver */	double   Rdom;
		/** maximum resistance to dominant driver */	double   Rmax;
		/** Adjusted non-switching capacitance */		double   Ca;
		/** Adjusted total capacitance */				double   Cd;
		/** Elmore delay (psec) */						double   tauD;
		/** 1st order time-constant (psec) */			double   tauA;
		/** 2nd order time-constant (psec) */			double   tauP;
		/** input transition = (input_tau) * Rin */		double   Tin;
		/** user specified low->high delay (DELTA) */	short    tplh;
		/** user specified high->low delay (DELTA) */	short    tphl;
		/** steady-state value calculated (H, L, X) */	char     finall;
		/** if tau calculated, == dominant voltage */	char     tau_done;
		/** if tauP calculated, == dominant voltage */	char     taup_done;

		Thev()
		{
			Clow = new Range(0, 0);
			Chigh = new Range(0, 0);
			Rup = new Range(Sim.LARGE, Sim.LARGE);
			Rdown = new Range(Sim.LARGE, Sim.LARGE);
			Req = new Range(Sim.LARGE, Sim.LARGE);
			V = new Range(1, 0);
			setN(null);
			flags		= 0;
			Rmin		= Sim.LARGE;
			Rdom		= Sim.LARGE;
			Rmax		= Sim.LARGE;
			Ca			= 0.0;
			Cd			= 0.0;
			tauD		= 0.0;
			tauA		= 0.0;
			tauP		= 0.0;
			Tin			= Sim.SMALL;
			tplh		= 0;
			tphl		= 0;
			finall		= Sim.X;
			tau_done	= Sim.N_POTS;
			taup_done	= Sim.N_POTS;
		}

		Thev(Thev old)
		{
			link = old.link;
			flags = old.flags;
			Clow = new Range(old.Clow);
			Chigh = new Range(old.Chigh);
			Rup = new Range(old.Rup);
			Rdown = new Range(old.Rdown);
			Req = new Range(old.Req);
			V = new Range(old.V);
			Rmin = old.Rmin;
			Rdom = old.Rdom;
			Rmax = old.Rmax;
			Ca = old.Ca;
			Cd = old.Cd;
			tauD = old.tauD;
			tauA = old.tauA;
			tauP = old.tauP;
			Tin = old.Tin;
			tplh = old.tplh;
			tphl = old.tphl;
			finall = old.finall;
			tau_done = old.tau_done;
			taup_done = old.taup_done;
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

	public static class Event
	{
		/** pointers in doubly-linked list */			Event    flink, blink;
		/** link for list of events for this node */	Event    nlink;
		/** node this event is all about */				Node     enode;
														Node     cause;
		/** time, in DELTAs, of this event */			long     ntime;
		/** delay associated with this event */			long     delay;
		/** rise/fall time, in DELTAs */				short    rtime;
		/** new value */								byte     eval;
		/** type of event */							byte     type;
	};

	/* transistor types (ttype) */
	/** n-channel enhancement */					public static final int	NCHAN       = 0;
	/** p-channel enhancement */					public static final int	PCHAN       = 1;
	/** depletion */								public static final int	DEP         = 2;
	/** simple two-terminal resistor */				public static final int	RESIST      = 3;

	/** transistors not affected by gate logic */	public static final int	ALWAYSON	= 0x02;

	/** set if gate of xistor is a node list */		public static final int	GATELIST	= 0x08;
	/** result of or'ing parallel transistors */	public static final int	ORED		= 0x20;
	/** part of an or'ed transistor */				public static final int	ORLIST		= 0x40;
	/** transistor capacitor (source == drain) */	public static final int	TCAP		= 0x80;

	/* transistor states (state)*/
	/** non-conducting */							public static final int	OFF         = 0;
	/** conducting */								public static final int	ON          = 1;
	/** unknown */									public static final int	UNKNOWN     = 2;
	/** weak */										public static final int	WEAK        = 3;

	/* transistor temporary flags (tflags) */
	/** Mark for crossing a transistor */			public static final int	CROSSED		= 0x01;
	/** Mark a broken transistor to avoid loop */	public static final int BROKEN		= 0x02;
	/** Mark as broken a parallel transistor */		public static final int	PBROKEN		= 0x04;
	/** Mark as being a parallel transistor */		public static final int	PARALLEL	= 0x08;

	/* node potentials */
	/** low low */									public static final int	LOW         = 0;
	/** unknown, intermediate, ... value */			public static final int	X           = 1;
													public static final int	X_X         = 2;
	/** logic high */								public static final int	HIGH        = 3;
	/** number of potentials [LOW-HIGH] */			public static final int	N_POTS      = 4;

	/** waiting to decay to X (only in events) */	public static final int	DECAY       = 4;

	/* possible values for nflags */
	public static final int	POWER_RAIL     = 0x000002;
	public static final int	ALIAS          = 0x000004;
	public static final int	USERDELAY      = 0x000008;
	public static final int	INPUT          = 0x000010;
	public static final int	WATCHED        = 0x000020;
	public static final int	WATCHVECTOR    = 0x000040;
	public static final int	STOPONCHANGE   = 0x000080;
	public static final int	STOPVECCHANGE  = 0x000100;
	public static final int	VISITED        = 0x000200;

	/** node is whithin a txtor stack */			public static final int	MERGED		= 0x000400;

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

	/* resistance types */
	/** static resistance */						public static final int	STATIC		= 0;
	/** dynamic-high resistance */					public static final int	DYNHIGH 	= 1;
	/** dynamic-low resistance */					public static final int	DYNLOW  	= 2;
	/** resist. for power calculation (unused) */	public static final int	POWER		= 3;
	/** number of resistance contexts */			public static final int	R_TYPES		= 3;

	/** result of re-evaluation */					public static final int	REVAL		= 0x0;
	/** node is decaying to X */					public static final int	DECAY_EV	= 0x1;

	/** pending from last run */					public static final int	PENDING		= 0x4;

	/** minimum node capacitance (in pf) */			public static final double MIN_CAP	= 0.00001;

	/** dynamic low resiatance index */				public static final int	R_LOW		= 0;
	/** dynamic high resiatance index */			public static final int	R_HIGH		= 1;

	/** a huge time */								private static final long MAX_TIME	= 0x0FFFFFFFFFFFFFFFL;

	/** A small number */							public static final double SMALL	= 1E-15;
	/** A large number */							public static final double LARGE	= 1E15;
	/** R > LIMIT are considered infinite */		public static final double LIMIT	= 1E8;

	/** number of transistor types defined */		public static final int NTTYPES     = 4;
	static String [] irsim_ttype =
	{
		"n-channel",
		"p-channel",
		"depletion",
		"resistor"
	};

	/**
	 * table to convert transistor type and gate node value into switch state
	 * indexed by [transistor-type][gate-node-value].
	 */
	static int [][] irsim_switch_state = new int[][]
	{
		/** NCHAH */	new int[] {OFF,  UNKNOWN, UNKNOWN, ON},
		/** PCHAN */	new int[] {ON,   UNKNOWN, UNKNOWN, OFF},
		/** RESIST */	new int[] {WEAK, WEAK,    WEAK,    WEAK},
		/** DEP */		new int[] {WEAK, WEAK,    WEAK,    WEAK}
	};

	public static String    irsim_vchars = "0XX1";
	public static String [] states = { "OFF", "ON", "UKNOWN", "WEAK" };

	public static final int	MAX_ERRS	= 20;

	/** this is probably sufficient per stage */							private static final int	MAX_PARALLEL	= 30;

	/** power supply node */												public  Node    irsim_VDD_node;
	/** ground supply node */												public  Node    irsim_GND_node;

	/** number of actual nodes */											public  int     irsim_nnodes;
	/** number of aliased nodes */											public  int     irsim_naliases;
	/** number of txtors indexed by type */									private int []  irsim_ntrans = new int[NTTYPES];
	/** number of transistors "or"ed */										private int []  nored = new int[NTTYPES];

	/** list of capacitor-transistors */									public  Trans   irsim_tcap = null;

	/** # of erros found in sim file */										private int     nerrs = 0;

	/** list of transistors just read */									private List    rd_tlist;
	public  Trans [] irsim_parallel_xtors = new Trans[MAX_PARALLEL];

	/** pointer to dummy hist-entry that serves as tail for all nodes */	private HistEnt irsim_last_hist;
	public  int      irsim_num_edges;
	public  int      irsim_num_punted;
	public  int      irsim_num_cons_punted;
	public  long     irsim_max_time;

	/** current simulated time */											public  long    irsim_cur_delta;
	/** node that belongs to current event */								public  Node    irsim_cur_node;
	/** number of current event */											public  long    irsim_nevent;

	/** if nonzero, all transactions take this DELAY-units */				public  int     irsim_tunitdelay = 0;
	/** number of DELAY-units after which undriven nodes decay to X */		public  long    irsim_tdecay = 0;

	private boolean  parallelWarning = false;
	private HashMap  nodeHash;
	private List     nodeList;
	private int      nodeIndexCounter;
	private boolean  warnVdd, warnGnd;
	public  int      irsim_debug;
	public  int      irsim_treport = 0;

	private Eval     irsim_model;
	private Config   theConfig;

	public Sim(Analyzer analyzer)
	{
		irsim_debug = Simulation.getIRSIMDebugging();
    	theConfig = new Config();
    	String steppingModel = Simulation.getIRSIMStepModel();
    	if (steppingModel.equals("Linear")) irsim_model = new SStep(analyzer, this); else
    	{
    		if (!steppingModel.equals("RC"))
    			System.out.println("Unknown stepping model: " + steppingModel + " using RC");
    		irsim_model = new NewRStep(analyzer, this);
    	}
	}

	public Config getConfig() { return theConfig; }

	public Eval getModel() { return irsim_model; }

	private void BAD_ARGC(String fileName, LineNumberReader lineReader, String [] strings)
	{
		irsim_error(fileName, lineReader.getLineNumber(), "Wrong number of args for '" + strings[0] + "'");
		for(int i=0; i<strings.length; i++) System.out.print(" " + strings[i]);
		System.out.println();
		CheckErrs(fileName);
	}

	public static void irsim_error(String filename, int lineno, String msg)
	{
		System.out.println("(" + filename + "," + lineno + "): " + msg);
	}

	/**
	 * Returns TRUE if there have been too many errors and the activity should be stopped.
	 */
	private boolean CheckErrs(String fileName)
	{
		nerrs++;
		if (nerrs > MAX_ERRS)
		{
			System.out.println("Too many errors in sim file <" + fileName + ">");
			return true;
		}
		return false;
	}

	/* Conversion macros between various time units */
	static double d2ns(long d) { return d * 0.01; }	/* deltas to ns */
	static double d2ps(long d) { return d * 10.0; }	/* deltas to ps */

	static long ns2d(double d) { return Math.round(d * 100); }	/* ns to deltas */
	static long ps2d(double d) { return Math.round(d * 0.1); }	/* ps to deltas */

	static double ps2ns(double d) { return d * 0.001; }	/* ps to ns */

	/* figure what's on the *other* terminal node of a transistor */
	static Node other_node(Trans t, Node n) { return t.drain == n ? t.source : t.drain; }

	static int BASETYPE(int t) { return t & 0x07; }

	private int hash_terms(Trans t) { return t.source.index ^ t.drain.index; }

	static int INPUT_NUM(int flg) { return ((flg & INPUT_MASK) >> 12); }

	/* combine 2 resistors in parallel */
	static double COMBINE(double r1, double r2) { return (r1 * r2) / (r1 + r2); }

	/* combine 2 resistors in parallel, watch out for zero resistance */
	static double COMBINE_R(double a, double b) { return ((a + b <= SMALL) ? 0 : COMBINE(a, b)); }

	/**
	 * Traverse the transistor list and add the node connection-list.  We have
	 * to be careful with ALIASed nodes.  Note that transistors with source/drain
	 * connected VDD and GND nodes are not linked.
	 */
	private Node connect_txtors()
	{
		Node nd_list = null;

		for(Iterator it = rd_tlist.iterator(); it.hasNext(); )
		{
			Trans t = (Trans)it.next();
			Node gate = null, src = null, drn = null;
			for(gate = (Node)t.gate; (gate.nflags & ALIAS) != 0; gate = gate.nlink) ;
			for(src = t.source; (src.nflags & ALIAS) != 0; src = src.nlink) ;
			for(drn = t.drain; (drn.nflags & ALIAS) != 0; drn = drn.nlink) ;

			t.gate = gate;
			t.source = src;
			t.drain = drn;

			int type = t.ttype;
			t.state = (byte)((type & ALWAYSON) != 0 ? WEAK : UNKNOWN);
			t.tflags = 0;

			irsim_ntrans[type]++;
			if (src == drn || (src.nflags & drn.nflags & POWER_RAIL) != 0)
			{
				/*
				 * transistor is just a capacitor.
				 * Transistors that have their drain/source shorted are NOT connected
				 * to the network, they are instead linked as a doubly linked list
				 * using the scache/dcache fields.
				 */
				t.ttype |= TCAP;
				t.setDTrans(irsim_tcap);
				t.setSTrans(irsim_tcap.getSTrans());
				irsim_tcap.getSTrans().setDTrans(t);
				irsim_tcap.setSTrans(t);
				irsim_tcap.x++;
			} else
			{
				// do not connect gate if ALWAYSON since they do not matter
				if ((t.ttype & ALWAYSON) == 0)
				{
					((Node)t.gate).ngateList.add(t);
				}

				if ((src.nflags & POWER_RAIL) == 0)
				{
					src.ntermList.add(t);
					nd_list = LINK_TO_LIST(src, nd_list);
				}
				if ((drn.nflags & POWER_RAIL) == 0)
				{
					drn.ntermList.add(t);
					nd_list = LINK_TO_LIST(drn, nd_list);
				}
			}
		}

		return nd_list;
	}

	/**
	 * if VISITED is not set in in n.nflags, Link n to the head of list
	 * using the temporary entry in the node structure.  This is
	 * used during net read-in/change to build lists of affected nodes.
	 */
	private Node LINK_TO_LIST(Node n, Node list)
	{
		if ((n.nflags & VISITED) == 0)
		{
			n.nflags |= VISITED;
			n.setNext(list);
			list = n;
		}
		return list;
	}

	/**
	 * node area and perimeter info (N sim command).
	 */
	private void node_info(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length != 8)
			BAD_ARGC(fileName, lineReader, targ);

		Node n = irsim_GetNode(targ[1]);

		n.ncap += TextUtils.atof(targ[4]) * (theConfig.irsim_CMA * theConfig.irsim_LAMBDA2) +
			TextUtils.atof(targ[5]) * (theConfig.irsim_CPA * theConfig.irsim_LAMBDA2) +
			TextUtils.atof(targ[6]) * (theConfig.irsim_CDA * theConfig.irsim_LAMBDA2) +
			TextUtils.atof(targ[7]) * 2.0f * (theConfig.irsim_CDP * theConfig.irsim_LAMBDA);
	}

	/**
	 * new format node area and perimeter info (M sim command).
	 */
	private void nnode_info(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length != 14)
			BAD_ARGC(fileName, lineReader, targ);

		Node n = irsim_GetNode(targ[1]);

		n.ncap += TextUtils.atof(targ[4]) * (theConfig.irsim_CM2A * theConfig.irsim_LAMBDA2) +
			TextUtils.atof(targ[5]) * 2.0 * (theConfig.irsim_CM2P * theConfig.irsim_LAMBDA) +
			TextUtils.atof(targ[6]) * (theConfig.irsim_CMA * theConfig.irsim_LAMBDA2) +
			TextUtils.atof(targ[7]) * 2.0 * (theConfig.irsim_CMP * theConfig.irsim_LAMBDA) +
			TextUtils.atof(targ[8]) * (theConfig.irsim_CPA * theConfig.irsim_LAMBDA2) +
			TextUtils.atof(targ[9]) * 2.0 * (theConfig.irsim_CPP * theConfig.irsim_LAMBDA) +
			TextUtils.atof(targ[10]) * (theConfig.irsim_CDA * theConfig.irsim_LAMBDA) +
			TextUtils.atof(targ[11]) * 2.0 * (theConfig.irsim_CDP * theConfig.irsim_LAMBDA) +
			TextUtils.atof(targ[12]) * (theConfig.irsim_CPDA * theConfig.irsim_LAMBDA2) +
			TextUtils.atof(targ[13]) * 2.0 * (theConfig.irsim_CPDP * theConfig.irsim_LAMBDA);
	}

	/**
	 * new transistor.  Implant specifies type.
	 * AreaPos specifies the argument number that contains the area (if any).
	 */
	private void newtrans(int implant, String [] targ, String fileName, LineNumberReader lineReader)
	{
		// create new transistor
		Trans t = new Trans();
		t.ttype = (byte)implant;

		if (implant == RESIST)
		{
			if (targ.length != 4)
			{
				BAD_ARGC(fileName, lineReader, targ);
				return;
			}

			t.gate = irsim_VDD_node;
			t.source = irsim_GetNode(targ[1]);
			t.drain = irsim_GetNode(targ[2]);

			long length = (long)(TextUtils.atof(targ[3]) * theConfig.irsim_LAMBDACM);
			t.r = theConfig.irsim_requiv(implant, 0, length);

		} else
		{
			if (targ.length != 11)
			{
				BAD_ARGC(fileName, lineReader, targ);
				return;
			}

			t.gate = irsim_GetNode(targ[1]);
			t.source = irsim_GetNode(targ[2]);
			t.drain = irsim_GetNode(targ[3]);

			long length = (long)(TextUtils.atof(targ[4]) * theConfig.irsim_LAMBDACM);
			long width = (long)(TextUtils.atof(targ[5]) * theConfig.irsim_LAMBDACM);
			if (width <= 0 || length <= 0)
			{
				irsim_error(fileName, lineReader.getLineNumber(),
					"Bad transistor width=" + width + " or length=" + length);
				return;
			}
			((Node)t.gate).ncap += length * width * theConfig.irsim_CTGA;
			t.r = theConfig.irsim_requiv(implant, width, length);

			t.x = TextUtils.atoi(targ[6]);
			t.y = TextUtils.atoi(targ[7]);

			// parse area and perimeter
			for (int i = 8; i < targ.length; i++)
			{
				int aIsPos = targ[i].indexOf("A_");
				int pIsPos = targ[i].indexOf("P_");
				if (aIsPos >= 0 && pIsPos >= 0)
				{
					int a = TextUtils.atoi(targ[i].substring(aIsPos+2));
					int p = TextUtils.atoi(targ[i].substring(pIsPos+2));
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
						t.source.ncap += asrc * theConfig.irsim_LAMBDA * theConfig.irsim_LAMBDA * theConfig.irsim_CPDA +
							psrc * theConfig.irsim_LAMBDA * theConfig.irsim_CPDP;
						t.drain.ncap += adrn * theConfig.irsim_LAMBDA * theConfig.irsim_LAMBDA * theConfig.irsim_CPDA +
							pdrn * theConfig.irsim_LAMBDA * theConfig.irsim_CPDP;
					} else if (implant == NCHAN || implant == DEP)
					{
						t.source.ncap += asrc * theConfig.irsim_LAMBDA * theConfig.irsim_LAMBDA * theConfig.irsim_CDA +
							psrc * theConfig.irsim_LAMBDA * theConfig.irsim_CDP;
						t.drain.ncap += adrn * theConfig.irsim_LAMBDA * theConfig.irsim_LAMBDA * theConfig.irsim_CDA +
							pdrn * theConfig.irsim_LAMBDA * theConfig.irsim_CDP;
					}
				}
			}
		}

		// link it to the list
		rd_tlist.add(t);
	}

	/**
	 * accept a bunch of aliases for a node (= sim command).
	 */
	private void alias(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length < 3)
		{
			BAD_ARGC(fileName, lineReader, targ);
			return;
		}

		Node n = irsim_GetNode(targ[1]);

		for(int i = 2; i < targ.length; i++)
		{
			Node m = irsim_GetNode(targ[i]);
			if (m == n) continue;

			if ((m.nflags & POWER_RAIL) != 0)
			{
				Node swap = m;   m = n;   n = swap;
			}

			if ((m.nflags & POWER_RAIL) != 0)
			{
				irsim_error(fileName, lineReader.getLineNumber(), "Can't alias the power supplies");
				continue;
			}

			n.ncap += m.ncap;
			m.nlink = n;
			m.nflags |= ALIAS;
			m.ncap = 0;
			irsim_nnodes--;
			irsim_naliases++;
		}
	}

	/**
	 * node threshold voltages (t sim command).
	 */
	private void nthresh(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length != 4)
		{
			BAD_ARGC(fileName, lineReader, targ);
			return;
		}

		Node n = irsim_GetNode(targ[1]);
		n.vlow = (float)TextUtils.atof(targ[2]);
		n.vhigh = (float)TextUtils.atof(targ[3]);
	}

	/**
	 * User delay for a node (D sim command).
	 */
	private void ndelay(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length != 4)
		{
			BAD_ARGC(fileName, lineReader, targ);
			return;
		}

		Node n = irsim_GetNode(targ[1]);
		n.nflags |= USERDELAY;
		n.tplh = (short)ns2d(TextUtils.atof(targ[2]));
		n.tphl = (short)ns2d(TextUtils.atof(targ[3]));
	}

	/**
	 * add capacitance to a node (c sim command).
	 */
	private void ncap(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length == 3)
		{
			Node n = irsim_GetNode(targ[1]);
			n.ncap += (float)TextUtils.atof(targ[2]);
		} else if (targ.length == 4)
		{
			// two terminal caps
			float cap = (float)(TextUtils.atof(targ[3]) / 1000);		// ff to pf conversion
			Node n = irsim_GetNode(targ[1]);
			Node m = irsim_GetNode(targ[2]);
			if (n != m)
			{
				// add cap to both nodes
				if (m != irsim_GND_node)	m.ncap += cap;
				if (n != irsim_GND_node)	n.ncap += cap;
			} else if (n == irsim_GND_node)
			{
				// same node, only GND makes sense
				n.ncap += cap;
			}
		} else
		{
			BAD_ARGC(fileName, lineReader, targ);
		}
	}

	/**
	 * parse input line into tokens, filling up carg and setting targc
	 * @param expand true to expand iterators.  For example, the
	 * string "out.{1:10}" expands into ten arguments "out.1", ..., "out.10".
	 * The string can contain multiple iterators which will be expanded
	 * independently, e.g., "out{1:10}{1:20:2}" expands into 100 arguments.
	 */
	public static String [] parse_line(String line, boolean expand)
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
			List listOfStrings = new ArrayList();
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
				strings[i] = (String)listOfStrings.get(i);
		}
		return strings;
	}

	private static boolean expand(String arg, List expanded)
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
		int start = TextUtils.atoi(iterator);
		int stop = TextUtils.atoi(iterator.substring(firstColon+1));
		int step = 1;
		if (secondColon >= 0) step = TextUtils.atoi(iterator.substring(secondColon+1));

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

	private void input_sim(URL simFileURL, List components)
	{
		if (components != null)
		{
			// load the circuit from memory
			for(Iterator it = components.iterator(); it.hasNext(); )
			{
				IRSIM.ComponentInfo ci = (IRSIM.ComponentInfo)it.next();
				switch (ci.type)
				{
					case 'n':
						Trans t = new Trans();
						t.ttype = NCHAN;

						t.gate = irsim_GetNode(ci.netName1);
						t.source = irsim_GetNode(ci.netName2);
						t.drain = irsim_GetNode(ci.netName3);

						long length = (long)(ci.length * theConfig.irsim_LAMBDACM);
						long width = (long)(ci.width * theConfig.irsim_LAMBDACM);
						if (width <= 0 || length <= 0)
						{
							System.out.println("Bad transistor width=" + width + " or length=" + length);
							return;
						}
						((Node)t.gate).ncap += length * width * theConfig.irsim_CTGA;

						t.x = (int)ci.ni.getAnchorCenterX();
						t.y = (int)ci.ni.getAnchorCenterY();
						t.source.ncap += ci.sourceArea * theConfig.irsim_LAMBDA * theConfig.irsim_LAMBDA * theConfig.irsim_CDA +
							ci.sourcePerim * theConfig.irsim_LAMBDA * theConfig.irsim_CDP;
						t.drain.ncap += ci.drainArea * theConfig.irsim_LAMBDA * theConfig.irsim_LAMBDA * theConfig.irsim_CDA +
							ci.drainPerim * theConfig.irsim_LAMBDA * theConfig.irsim_CDP;
						t.r = theConfig.irsim_requiv(NCHAN, width, length);

						// link it to the list
						rd_tlist.add(t);
						break;
					case 'p':
						t = new Trans();
						t.ttype = PCHAN;

						t.gate = irsim_GetNode(ci.netName1);
						t.source = irsim_GetNode(ci.netName2);
						t.drain = irsim_GetNode(ci.netName3);

						length = (long)(ci.length * theConfig.irsim_LAMBDACM);
						width = (long)(ci.width * theConfig.irsim_LAMBDACM);
						if (width <= 0 || length <= 0)
						{
							System.out.println("Bad transistor width=" + width + " or length=" + length);
							return;
						}
						((Node)t.gate).ncap += length * width * theConfig.irsim_CTGA;

						t.x = (int)ci.ni.getAnchorCenterX();
						t.y = (int)ci.ni.getAnchorCenterY();
						t.source.ncap += ci.sourceArea * theConfig.irsim_LAMBDA * theConfig.irsim_LAMBDA * theConfig.irsim_CPDA +
							ci.sourcePerim * theConfig.irsim_LAMBDA * theConfig.irsim_CPDP;
						t.drain.ncap += ci.drainArea * theConfig.irsim_LAMBDA * theConfig.irsim_LAMBDA * theConfig.irsim_CPDA +
							ci.drainPerim * theConfig.irsim_LAMBDA * theConfig.irsim_CPDP;
						t.r = theConfig.irsim_requiv(PCHAN, width, length);

						// link it to the list
						rd_tlist.add(t);
						break;
					case 'r':
						t = new Trans();
						t.ttype = RESIST;
						t.gate = irsim_VDD_node;
						t.source = irsim_GetNode(ci.netName1);
						t.drain = irsim_GetNode(ci.netName2);
						t.r = theConfig.irsim_requiv(RESIST, 0, (long)(ci.rcValue * theConfig.irsim_LAMBDACM));

						// link it to the list
						rd_tlist.add(t);
						break;
					case 'C':
						float cap = (float)(ci.rcValue / 1000);		// ff to pf conversion
						Node n = irsim_GetNode(ci.netName1);
						Node m = irsim_GetNode(ci.netName2);
						if (n != m)
						{
							// add cap to both nodes
							if (m != irsim_GND_node)	m.ncap += cap;
							if (n != irsim_GND_node)	n.ncap += cap;
						} else if (n == irsim_GND_node)
						{
							// same node, only GND makes sense
							n.ncap += cap;
						}
						break;
				}
			}
			return;
		}

		// read the file
		boolean R_error = false;
		boolean A_error = false;
		String fileName = simFileURL.getFile();
		try
		{
			URLConnection urlCon = simFileURL.openConnection();
			InputStream inputStream = urlCon.getInputStream();
			InputStreamReader is = new InputStreamReader(inputStream);
			LineNumberReader lineReader = new LineNumberReader(is);
			for(;;)
			{
				String line = lineReader.readLine();
				if (line == null) break;
				String [] targ = parse_line(line, false);
				if (targ.length == 0) continue;
				char firstCh = targ[0].charAt(0);
				switch (firstCh)
				{
					case '|':
						if (lineReader.getLineNumber() > 1) break;
						if (targ.length >= 2)
						{
							double lmbd = TextUtils.atof(targ[2]) / 100.0;
							if (lmbd != theConfig.irsim_LAMBDA)
							{
								System.out.println("WARNING: sim file lambda (" + lmbd + "u) != config lambda (" +
									theConfig.irsim_LAMBDA + "u), using config lambda");
							}
						}
						if (targ.length >= 6)
						{
							if (theConfig.irsim_CDA == 0.0 || theConfig.irsim_CDP == 0.0 ||
								theConfig.irsim_CPDA == 0.0 || theConfig.irsim_CPDP == 0.0)
							{
								System.out.println("Warning: missing area/perim cap values are zero");
							}
						}
						break;
					case 'e':
					case 'n':
						newtrans(NCHAN, targ, fileName, lineReader);
						break;
					case 'p':
						newtrans(PCHAN, targ, fileName, lineReader);
						break;
					case 'd':
						newtrans(DEP, targ, fileName, lineReader);
						break;
					case 'r':
						newtrans(RESIST, targ, fileName, lineReader);
						break;
					case 'N':
						node_info(targ, fileName, lineReader);
						break;
					case 'M':
						nnode_info(targ, fileName, lineReader);
						break;
					case 'c':
					case 'C':
						ncap(targ, fileName, lineReader);
						break;
					case '=':
						alias(targ, fileName, lineReader);
						break;
					case 't':
						nthresh(targ, fileName, lineReader);
						break;
					case 'D':
						ndelay(targ, fileName, lineReader);
						break;
					case 'R':
						if (!R_error)	// only warn about this 1 time
						{
							System.out.println(fileName + "Ignoring lumped-resistance ('R' construct)");
							R_error = true;
						}
						break;
					case 'A':
						if (!A_error)	// only warn about this 1 time
						{
							System.out.println(fileName + "Ignoring attribute-line ('A' construct)");
							A_error = true;
						}
						break;
					default:
						irsim_error(fileName, lineReader.getLineNumber(), "Unrecognized input line (" + targ[0] + ")");
						if (CheckErrs(fileName)) return;
				}
			}
			inputStream.close();
		} catch (IOException e)
		{
			System.out.println("Error reading file");
		}
		System.out.println("Loaded circuit, lambda=" + theConfig.irsim_LAMBDA + "u");
	}

	public boolean irsim_rd_network(URL simFileURL, List components)
	{
		rd_tlist = new ArrayList();
		nodeHash = new HashMap();
		nodeList = new ArrayList();
		nodeIndexCounter = 1;
		warnVdd = warnGnd = false;
		irsim_max_time = MAX_TIME;

		// initialize counts
		for(int i = 0; i < NTTYPES; i++)
			irsim_ntrans[i] = 0;
		irsim_nnodes = irsim_naliases = 0;
		irsim_init_hist();

		// initialize globals
		irsim_num_edges = 0;
		irsim_num_punted = 0;
		irsim_num_cons_punted = 0;

		irsim_VDD_node = irsim_GetNode("Vdd");
		irsim_VDD_node.npot = HIGH;
		irsim_VDD_node.nflags |= (INPUT | POWER_RAIL);
		irsim_VDD_node.head.inp = true;
		irsim_VDD_node.head.val = HIGH;
		irsim_VDD_node.head.punt = false;
		irsim_VDD_node.head.htime = 0;
		irsim_VDD_node.head.rtime = irsim_VDD_node.head.delay = 0;
		irsim_VDD_node.head.next = irsim_last_hist;
		irsim_VDD_node.curr = irsim_VDD_node.head;

		irsim_GND_node = irsim_GetNode("Gnd");
		irsim_GND_node.npot = LOW;
		irsim_GND_node.nflags |= (INPUT | POWER_RAIL);
		irsim_GND_node.head.inp = true;
		irsim_GND_node.head.val = LOW;
		irsim_GND_node.head.punt = false;
		irsim_GND_node.head.htime = 0;
		irsim_GND_node.head.rtime = irsim_GND_node.head.delay = 0;
		irsim_GND_node.head.next = irsim_last_hist;
		irsim_GND_node.curr = irsim_GND_node.head;

		irsim_tcap = new Trans();
		irsim_tcap.source = null;
		irsim_tcap.drain = null;
		irsim_tcap.setSTrans(irsim_tcap);
		irsim_tcap.setDTrans(irsim_tcap);
		irsim_tcap.x = 0;

		nerrs = 0;
		input_sim(simFileURL, components);
		if (nerrs > 0) return true;

		// connect all txtors to corresponding nodes
		irsim_ConnectNetwork();

		// sort the signal names
		Collections.sort(irsim_GetNodeList(), new NodesByName());

		irsim_model.irsim_init_event();
		return false;
	}

	private static class NodesByName implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Node n1 = (Node)o1;
			Node n2 = (Node)o2;
			return n1.nname.compareToIgnoreCase(n2.nname);
		}
	}

	private void irsim_ConnectNetwork()
	{
		Node ndlist = connect_txtors();
		irsim_make_parallel(ndlist);

		// display information about circuit
		String infstr = irsim_nnodes + " nodes";
		if (irsim_naliases != 0)
			infstr += ", " + irsim_naliases + " aliases";
		for(int i = 0; i < NTTYPES; i++)
		{
			if (irsim_ntrans[i] == 0) continue;
			infstr += ", " + irsim_ntrans[i] + " " + irsim_ttype[i] + " transistors";
			if (nored[i] != 0)
			{
				infstr += " (" + nored[i] + " parallel)";
			}
		}
		if (irsim_tcap.x != 0)
			infstr += " (" + irsim_tcap.x + " shorted)";
		System.out.println(infstr);
	}

	public boolean      irsim_withdriven;		/* TRUE if stage is driven by some input */

	/**
	 * Build a linked-list of nodes (using nlink entry in Node structure)
	 * which are electrically connected to node 'n'.  No special order
	 * is required so tree walk is performed non-recursively by doing a
	 * breath-first traversal.  The value caches for each transistor we
	 * come across are reset here.  Loops are broken at an arbitrary point
	 * and parallel transistors are identified.
	 */
	public void irsim_BuildConnList(Node n)
	{
		int n_par = 0;

		n.nflags &= ~VISITED;
		irsim_withdriven = false;

		Node next = n;
		Node thisone = n.nlink = n;
		do
		{
			for(Iterator it = thisone.ntermList.iterator(); it.hasNext(); )
			{
				Trans t = (Trans)it.next();
				if (t.state == OFF) continue;
				if ((t.tflags & CROSSED) != 0)	// Each transistor is crossed twice
				{
					t.tflags &= ~CROSSED;
					continue;
				}
				t.setSThev(null);
				t.setDThev(null);

				Node other = other_node(t, thisone);
				if ((other.nflags & INPUT) != 0)
				{
					irsim_withdriven = true;
					continue;
				}

				t.tflags |= CROSSED;		// Crossing trans 1st time

				if (other.nlink == null)		// New node in this stage
				{
					other.nflags &= ~VISITED;
					other.nlink = n;
					next.nlink = other;
					next = other;
					other.setTrans(t);		// we reach other through t
				}
				else if (!(irsim_model instanceof NewRStep))
					continue;
				else if (hash_terms(other.getTrans()) == hash_terms(t))
				{					    // parallel transistors
					Trans tran = other.getTrans();
					if ((tran.tflags & PARALLEL) != 0)
						t.setDTrans(irsim_parallel_xtors[tran.n_par]);
					else
					{
						if (n_par >= MAX_PARALLEL)
						{
							if (!parallelWarning)
							{
								System.out.println("There are too many transistors in parallel (> " + MAX_PARALLEL + ")");
								System.out.println("Simulation results may be inaccurate, to fix this you may have to");
								System.out.println("increase 'MAX_PARALLEL' in 'Sim.java'.");
								System.out.println("Note: This condition often occurs when Vdd or Gnd are not connected to all cells.");
								if (thisone.nname != null && other.nname != null)
									System.out.println("      Check the vicinity of the following 2 nodes: " + thisone.nname + " " + other.nname);
								parallelWarning = true;
							}
							t.tflags |= PBROKEN;		// simply ignore it
							continue;
						}
						tran.n_par = (byte)(n_par++);
						tran.tflags |= PARALLEL;
					}
					irsim_parallel_xtors[tran.n_par] = t;
					t.tflags |= PBROKEN;
				} else
				{					// we have a loop, break it
					t.tflags |= BROKEN;
				}
			}
		}
		while((thisone = thisone.nlink) != n);

		next.nlink = null;			// terminate connection list
	}

	/********************************************** HISTORY *******************************************/

	private void irsim_init_hist()
	{
		HistEnt dummy = new HistEnt();
		irsim_last_hist = dummy;
		dummy.next = irsim_last_hist;
		dummy.htime = irsim_max_time;
		dummy.val = X;
		dummy.inp = true;
		dummy.punt = false;
		dummy.delay = dummy.rtime = 0;
	}

	/**
	 * Add a new entry to the history list.  Update curr to point to this change.
	 */
	public void irsim_AddHist(Node node, int value, boolean inp, long time, long delay, long rtime)
	{
		irsim_num_edges++;
		HistEnt curr = node.curr;

		while(curr.next.punt)		// skip past any punted events
			curr = curr.next;

		HistEnt newh = new HistEnt();
		if (newh == null) return;

		newh.next = curr.next;
		newh.htime = time;
		newh.val = (byte)value;
		newh.inp = inp;
		newh.punt = false;
		newh.delay = (short)delay;
		newh.rtime = (short)rtime;
		node.curr = curr.next = newh;
	}

	/**
	 * Add a punted event to the history list for the node.  Consecutive punted
	 * events are kept in punted-order, so that h.ptime < h.next.ptime.
	 * Adding a punted event does not change the current pointer, which always
	 * points to the last "effective" node change.
	 */
	public void irsim_AddPunted(Node node, Event ev, long tim)
	{
		HistEnt h = node.curr;

		irsim_num_punted++;

		HistEnt newp = new HistEnt();

		newp.htime = ev.ntime;
		newp.val = ev.eval;
		newp.inp = false;
		newp.punt = true;
		newp.delay = (short)ev.delay;
		newp.rtime = ev.rtime;
		newp.ptime = (short)(newp.htime - tim);

		if (h.next.punt)		// there are some punted events already
		{
			irsim_num_cons_punted++;
			do { h = h.next; } while(h.next.punt);
		}

		newp.next = h.next;
		h.next = newp;
	}

	public static HistEnt NEXTH(HistEnt p)
	{
		HistEnt h;
		for(h = p.next; h.punt; h = h.next) ;
		return h;
	}

	public void irsim_backToTime(Node nd)
	{
		if ((nd.nflags & (ALIAS | MERGED)) != 0) return;

		HistEnt h = nd.head;
		HistEnt p = NEXTH(h);
		while(p.htime < irsim_cur_delta)
		{
			h = p;
			p = NEXTH(p);
		}
		nd.curr = h;

		// queue pending events
		for(p = h, h = p.next; ; p = h, h = h.next)
		{
			long  qtime;

			if (h.punt)
			{
				// if already punted, skip it
				long puntTime = h.htime - h.ptime;
				if (puntTime < irsim_cur_delta) continue;

				qtime = h.htime - h.delay;	// pending, enqueue it
				if (qtime < irsim_cur_delta)
				{
					long tmp = irsim_cur_delta;
					irsim_cur_delta = qtime;
					irsim_model.irsim_enqueue_event(nd, h.val, h.delay, h.rtime);
					irsim_cur_delta = tmp;
				}
				p.next = h.next;
				h = p;
			} else
			{
				// time at which history entry was enqueued
				qtime = h.htime - h.delay;
				if (qtime < irsim_cur_delta)		// pending, enqueue it
				{
					long tmp = irsim_cur_delta;
					irsim_cur_delta = qtime;
					irsim_model.irsim_enqueue_event(nd, h.val, h.delay, h.rtime);
					irsim_cur_delta = tmp;

					p.next = h.next;		// and free it
					h = p;
				}
				else
					break;
			}
		}

		p.next = irsim_last_hist;
		p = h;
		// p now points to the 1st event in the future (to be deleted)
		if (p != irsim_last_hist)
		{
			while(h.next != irsim_last_hist)
				h = h.next;
		}

		h = nd.curr;
		nd.npot = h.val;
		nd.setTime(h.htime);
		if (h.inp)
			nd.nflags |= INPUT;

		if (nd.ngateList.size() != 0)		// recompute transistor states
		{
			for(Iterator it = nd.ngateList.iterator(); it.hasNext(); )
			{
				Trans t = (Trans)it.next();
				t.state = (byte)irsim_model.compute_trans_state(t);
			}
		}
	}

	/************************************ PARALLEL *******************************************/

	/**
	 * Run through the list of nodes, collapsing all transistors with the same
	 * gate/source/drain into a compound transistor.
	 */
	public void irsim_make_parallel(Node nlist)
	{
		for( ; nlist != null; nlist.nflags &= ~VISITED, nlist = nlist.getNext())
		{
			for(int l1 = 0; l1 < nlist.ntermList.size(); l1++)
			{
				Trans t1 = (Trans)nlist.ntermList.get(l1);
				int type = t1.ttype;
				if ((type & (GATELIST | ORED)) != 0)
					continue;	// ORED implies processed, so skip as well

				long hval = hash_terms(t1);
				for(int l2 = l1+1; l2 < nlist.ntermList.size(); l2++)
				{
					Trans t2 = (Trans)nlist.ntermList.get(l2);
					if (t1.gate != t2.gate || hash_terms(t2) != hval ||
						type != (t2.ttype & ~ORED))
							continue;

					if ((t1.ttype & ORED) == 0)
					{
						Trans t3 = new Trans();
						t3.r = new Resists();
						t3.r.dynres[R_LOW] = t1.r.dynres[R_LOW];
						t3.r.dynres[R_HIGH] = t1.r.dynres[R_HIGH];
						t3.r.rstatic = t1.r.rstatic;
						t3.gate = t1.gate;
						t3.source = t1.source;
						t3.drain = t1.drain;
						t3.ttype = (byte)((t1.ttype & ~ORLIST) | ORED);
						t3.state = t1.state;
						t3.tflags = t1.tflags;
						t3.tlink = t1;
						t1.setSTrans(null);
						t1.setDTrans(t3);
						int oldGateI = ((Node)t1.gate).ngateList.indexOf(t1);
						if (oldGateI >= 0) ((Node)t1.gate).ngateList.set(oldGateI, t3);
						int oldSourceI = t1.source.ntermList.indexOf(t1);
						if (oldSourceI >= 0) t1.source.ntermList.set(oldSourceI, t3);
						int oldDrainI = t1.drain.ntermList.indexOf(t1);
						if (oldDrainI >= 0) t1.drain.ntermList.set(oldDrainI, t3);
						t1.ttype |= ORLIST;
						t1 = t3;
						nored[BASETYPE(t1.ttype)]++;
					}

					Resists  r1 = t1.r, r2 = t2.r;
					r1.rstatic = (float)COMBINE(r1.rstatic, r2.rstatic);
					r1.dynres[R_LOW] = (float)COMBINE(r1.dynres[R_LOW], r2.dynres[R_LOW]);
					r1.dynres[R_HIGH] = (float)COMBINE(r1.dynres[R_HIGH], r2.dynres[R_HIGH]);

					((Node)t2.gate).ngateList.remove(t2);	// disconnect gate
					if (t2.source == nlist)		// disconnect term1
					{
						t2.drain.ntermList.remove(t2);
					} else
					{
						t2.source.ntermList.remove(t2);
					}

					// disconnect term2
					nlist.ntermList.remove(t2);

					if ((t2.ttype & ORED) != 0)
					{
						Trans  t;

						for(t = t2.tlink; t.getSTrans() != null; t = t.getSTrans())
							t.setDTrans(t1);
						t.setSTrans(t1.tlink);
						t1.tlink = t2.tlink;
					} else
					{
						t2.ttype |= ORLIST;	// mark as part of or
						t2.setDTrans(t1);		// this is the real txtor
						t2.setSTrans(t1.tlink);	// link unto t1 list
						t1.tlink = t2;
						nored[BASETYPE(t1.ttype)]++;
					}
				}
			}
		}
	}

}
