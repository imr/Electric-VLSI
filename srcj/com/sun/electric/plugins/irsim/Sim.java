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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class Sim
{
	public static class Node
	{
		Node     nlink;		/* sundries list */
		Event    events;	/* charge sharing event */
		Tlist    ngate;		/* list of xtors w/ gates connected to this node */
		Tlist    nterm;		/* list of xtors w/ src/drn connected to this node */
		Node     hnext;		/* link in hash bucket */
		float    ncap;		/* capacitance of node in pf */
		float    vlow;		/* low logic threshold for node, normalized units */
		float    vhigh;		/* high logic threshold for node, normalized units */
		short    tplh;		/* low to high transition time in DELTA's */
		short    tphl;		/* high to low transition time in DELTA's */
		Stimuli.DigitalSignal sig;
		private Object   c;			/* combines time, nindex, cap, and event */

		private Object   t;			/* combines cause, punts, and tranT */

		short    npot;		/* current potential */
		short    oldpot;	/* old potential (for incremental simulation). */
		long     nflags;	/* flag word (see defs below) */
		String   nname;		/* ascii name of node */

		private Object   n;			/* combines thev, next, and tranN */

		HistEnt  head;		/* first entry in transition history */
		HistEnt  curr;		/* ptr. to current history entry */
		HistEnt  hchange;	/* special entry to avoid changing the history */
		short    awpot;		/* potential for pending AssertWhen */
		Analyzer.AssertWhen awpending;	/* pending asswertWhen list */
		int index;			/* index of this node (a unique value) */

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

	/* find node in network */
	public Node irsim_find(String name)
	{
		return (Node)nodeHash.get(name.toLowerCase());
	}

	/*
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
		n.ngate = n.nterm = null;
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
		Object   gate;    			/* nodes to which trans is connected */
		Node     source, drain;		/* nodes to which trans is connected */
		Object   sCache, dCache;	/* caches to remember src/drn values */
		byte     ttype;				/* type of transistor */
		byte     state;				/* cache to remember current state */
		byte     tflags; 		    /* transistor flags */
		byte     n_par;				/* index into parallel list */
		Resists  r;					/* transistor resistances */
		Trans    tlink;				/* next txtor in position hash table */
		int      x, y;				/* position in the layout */

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
		float  [] dynres;		/* dynamic resistances [R_LOW - R_MAX] */
		float  rstatic;			/* static resistance of transistor */

		TranResist() { dynres = new float[2]; }
	};

	public static class Resists extends TranResist		/* same as Res_1 but indexed dynamic resists */
	{
		long  width, length;	/* transistor size in centimicrons */
	};

	public static class HistEnt
	{
		HistEnt  next;			/* next transition in history */
		short    delay;			/* delay from input */
		short    rtime;			/* rise/fall time */
		short    ptime;			/* punt time */
		long     htime;			/* time of transition */
		boolean  inp;			/* 1 if node became an input */
		boolean  punt;			/* 1 if this event was punted */
		byte     val;			/* value: HIGH, LOW, or X */
	};

	public static class Tlist
	{
		Tlist    next;			/* next list element */
		Trans    xtor;			/* txtor connected to this node */
	};

	/* resists are in ohms, caps in pf */
	public static class Thev
	{
		Object   link;
		int      flags;		/* flags defined above			    */
		Range    Clow;		/* capacitance charged low		    */
		Range    Chigh;		/* capacitance charged high		    */
		Range    Rup;		/* resistance pulling up to Vdd		    */
		Range    Rdown;		/* resistance pulling down to GND	    */
		Range    Req;		/* resist. of present (parallel) xtor(s)    */
		Range    V;			/* normalized voltage range (0-1)	    */
		double   Rmin;		/* minimum resistance to any driver	    */
		double   Rdom;		/* minimum resistance to dominant driver    */
		double   Rmax;		/* maximum resistance to dominant driver    */
		double   Ca;		/* Adjusted non-switching capacitance 	    */
		double   Cd;		/* Adjusted total capacitance		    */
		double   tauD;		/* Elmore delay	(psec)			    */
		double   tauA;		/* 1st order time-constant (psec)	    */
		double   tauP;		/* 2nd order time-constant (psec)	    */
		double   Tin;		/* input transition = (input_tau) * Rin	    */
		short    tplh;		/* user specified low->high delay (DELTA)   */
		short    tphl;		/* user specified high->low delay (DELTA)   */
		char     finall;		/* steady-state value calculated (H, L, X)  */
		char     tau_done;	/* if tau calculated, == dominant voltage   */
		char     taup_done;	/* if tauP calculated, == dominant voltage  */

		Thev()
		{
			Clow = new Range();
			Chigh = new Range();
			Rup = new Range();
			Rdown = new Range();
			Req = new Range();
			V = new Range();
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

		Range(Range r)
		{
			min = r.min;
			max = r.max;
		}
	};

	public static class Event
	{
		Event    flink, blink;	/* pointers in doubly-linked list */
		Event    nlink;			/* link for list of events for this node */
		Node     enode;			/* node this event is all about */
		Node     cause;
		long     ntime;			/* time, in DELTAs, of this event */
		long     delay;			/* delay associated with this event */
		short    rtime;			/* rise/fall time, in DELTAs */
		byte     eval;			/* new value */
		byte     type;			/* type of event (for incremental only) */
	};

	/* transistor types (ttype) */
	public static final int	NCHAN          = 0;	/* n-channel enhancement */
	public static final int	PCHAN          = 1;	/* p-channel enhancement */
	public static final int	DEP            = 2;	/* depletion */
	public static final int	RESIST         = 3;	/* simple two-terminal resistor */
	public static final int	TYPERESERVED   = 4;	/* reserved for future transistor types */
	public static final int	SUBCKT         = 5;	/* black box user code */

	public static final int	ALWAYSON	= 0x02;	/* transistors not affected by gate logic */

	public static final int	GATELIST	= 0x08;	/* set if gate of xistor is a node list */
	public static final int	STACKED		= 0x10;	/* transistor was stacked into gate list */
	public static final int	ORED		= 0x20;	/* result of or'ing parallel transistors */
	public static final int	ORLIST		= 0x40;	/* part of an or'ed transistor */
	public static final int	TCAP		= 0x80;	/* transistor capacitor (source == drain) */

	static final String    irsim_version = "version 9.5j";

	/* transistor states (state)*/
	public static final int	OFF            = 0;	/* non-conducting */
	public static final int	ON             = 1;	/* conducting */
	public static final int	UNKNOWN        = 2;	/* unknown */
	public static final int	WEAK           = 3;	/* weak */

	/* transistor temporary flags (tflags) */
	public static final int	CROSSED		= 0x01;	/* Mark for crossing a transistor */
	public static final int BROKEN		= 0x02;	/* Mark a broken transistor to avoid loop */
	public static final int	PBROKEN		= 0x04;	/* Mark as broken a parallel transistor */
	public static final int	PARALLEL	= 0x08;	/* Mark as being a parallel transistor */
	public static final int	ACTIVE_T	= 0x10;	/* incremental status of transistor */

	/* node potentials */
	public static final int	LOW            = 0;	/* low low */
	public static final int	X              = 1;	/* unknown, intermediate, ... value */
	public static final int	X_X            = 2;
	public static final int	HIGH           = 3;	/* logic high */
	public static final int	N_POTS         = 4;	/* number of potentials [LOW-HIGH] */

	public static final int	DECAY          = 4;	/* waiting to decay to X (only in events) */

	/* possible values for nflags */
	public static final int	DEVIATED       = 0x000001;	/* node's state differs from hist */
	public static final int	POWER_RAIL     = 0x000002;
	public static final int	ALIAS          = 0x000004;
	public static final int	USERDELAY      = 0x000008;
	public static final int	INPUT          = 0x000010;
	public static final int	WATCHED        = 0x000020;
	public static final int	WATCHVECTOR    = 0x000040;
	public static final int	STOPONCHANGE   = 0x000080;
	public static final int	STOPVECCHANGE  = 0x000100;
	public static final int	VISITED        = 0x000200;

	public static final int	MERGED		= 0x000400;	/* node is whithin a txtor stack */
	public static final int	DELETED		= 0x000800;	/* node was deleted */

	public static final int	H_INPUT		= 0x001000;	/* node is in high input list */
	public static final int	L_INPUT		= 0x002000;	/* node is in low input list */
	public static final int	U_INPUT		= 0x003000;	/* node is in U input list */
	public static final int	X_INPUT		= 0x004000;	/* node is in X input list */

	public static final int INPUT_MASK	=	(H_INPUT | L_INPUT | X_INPUT | U_INPUT);

	public static final int	CHANGED		= 0x008000;	/* node is affected by a net change */
	public static final int	STIM		= 0x010000;	/* node is used as stimuli */
	public static final int	ACTIVE_CL	= 0x020000;	/* node is in an active cluster */
	public static final int	WAS_ACTIVE	= 0x040000;	/* set if node was ever active */

	public static final int HIST_OFF        = 0x200000;        /* node don't save history */

	/* resistance types */
	public static final int	STATIC		= 0;	/* static resistance */
	public static final int	DYNHIGH 	= 1;	/* dynamic-high resistance */
	public static final int	DYNLOW  	= 2;	/* dynamic-low resistance */
	public static final int	POWER		= 3;	/* resist. for power calculation (unused) */
	public static final int	R_TYPES		= 3;	/* number of resistance contexts */

	/* Event Types (for incremental simulation only) */
	public static final int	IS_INPUT		= 0x1;		/* event makes node input */
	public static final int	IS_XINPUT		= 0x2;		/* event terminates input */

	public static final int	REVAL			= 0x0;		/* result of re-evaluation */
	public static final int	DECAY_EV		= 0x1;		/* node is decaying to X */
	public static final int	PUNTED			= 0x3;		/* previously punted event */

	/* events > THREAD are NOT threaded into node structure */
	public static final int	THREAD			= 0x3;

	public static final int	PENDING			= 0x4;		/* pending from last run */
	public static final int	STIMULI			= 0x8;		/* self-schedulled stimuli */
	public static final int	STIM_INP		= (STIMULI | IS_INPUT);
	public static final int	STIM_XINP		= (STIMULI | IS_XINPUT);

	public static final int	CHECK_PNT		= 0x10;		/* next change in history */
	public static final int	INP_EV			= (CHECK_PNT | IS_INPUT);
	public static final int	XINP_EV			= (CHECK_PNT | IS_XINPUT);
	public static final int	DELAY_CHK		= 0x20;		/* delayed CHECK_PNT */
	public static final int	DELAY_EV		= 0x40;		/* last REVAL was delayed */

	public static final int	CHNG_MODEL		= 0x80;		/* change evaluation model */

	public static final double MIN_CAP		= 0.00001;		/* minimum node capacitance (in pf) */

	public static final int	R_LOW		= 0;		/* dynamic low resiatance index */
	public static final int	R_HIGH		= 1;		/* dynamic high resiatance index */

	static final int REPORT_DECAY	= 0x01;
	static final int REPORT_DELAY	= 0x02;
	static final int REPORT_TAU		= 0x04;
	static final int REPORT_TCOORD	= 0x08;

	static final long MAX_TIME	= 0x0FFFFFFFFFFFFFFFL;	/* a huge time */

	static final double SMALL		= 1E-15;	/* A small number */
	static final double LARGE		= 1E15;	    /* A large number */
	static final double LIMIT		= 1E8;	    /* R > LIMIT are considered infinite */

	/* Conversion macros between various time units */
	static double d2ns(long d) { return d * 0.01; }	/* deltas to ns */
	static double d2ps(long d) { return d * 10.0; }	/* deltas to ps */

	static long ns2d(double d) { return Math.round(d * 100); }	/* ns to deltas */
	static long ps2d(double d) { return Math.round(d * 0.1); }	/* ps to deltas */

	static double ps2ns(double d) { return (long)(d * 0.001); }	/* ps to ns */

	/* figure what's on the *other* terminal node of a transistor */
	static Node other_node(Trans t, Node n) { return t.drain == n ? t.source : t.drain; }

	static int BASETYPE(int T) { return T & 0x07; }

	private int hash_terms(Trans t) { return t.source.index ^ t.drain.index; }

	static int INPUT_NUM(int flg) { return ((flg & INPUT_MASK) >> 12); }

	/*
	 * The routines in this file handle network input (from sim files).
	 * The input routine "irsim_rd_network" replaces the work formerly done by presim.
	 * This version differs from the former in the following:
	 *  1. voltage drops across transistors are ignored (assumes transistors
	 *     driven by nodes with voltage drops have the same resistance as those
	 *     driven by a full swing).
	 *  2. static power calculations not performed (only useful for nmos anyhow).
	 */

	static final int NTTYPES = 4;	/* number of transistor types defined */
	static String [] irsim_ttype =
	{
		"n-channel",
		"p-channel",
		"depletion",
		"resistor"
	};

	/* table to convert transistor type and gate node value into switch state
	 * indexed by irsim_switch_state[transistor-type][gate-node-value].
	 */
	static int [][] irsim_switch_state = new int[][]
	{
		new int[] {OFF,  UNKNOWN, UNKNOWN, ON},		/* NCHAH */
		new int[] {ON,   UNKNOWN, UNKNOWN, OFF},	/* PCHAN */
		new int[] {WEAK, WEAK,    WEAK,    WEAK},   /* RESIST */
		new int[] {WEAK, WEAK,    WEAK,    WEAK}    /* DEP */
	};

	static String   irsim_vchars = "0XX1";

	public static final int	MAX_ERRS	= 20;
	public static final int	FATAL		= (MAX_ERRS + 1);

	public static final int	MAX_PARALLEL	= 30;	/* this is probably sufficient per stage */


	public Node     irsim_VDD_node;		/* power supply nodes */
	public Node     irsim_GND_node;

	private Tlist    irsim_on_trans;		/* always on transistors */

	public int      irsim_nnodes;			/* number of actual nodes */
	public int      irsim_naliases;		/* number of aliased nodes */
	private int []   irsim_ntrans = new int[NTTYPES];	/* number of txtors indexed by type */

	private Tlist    irsim_freeLinks = null;	/* free list of Tlist structs */
	public Trans    irsim_tcap = null;		/* list of capacitor-transistors */

	private int     nerrs = 0;		/* # of erros found in sim file */

	private Trans    rd_tlist;		/* list of transistors just read */
	public Trans [] irsim_parallel_xtors = new Trans[MAX_PARALLEL];

	private HistEnt  irsim_last_hist;			/* pointer to dummy hist-entry that serves as tail for all nodes */
	public int      irsim_num_edges = 0;
	public int      irsim_num_punted = 0;
	public int      irsim_num_cons_punted = 0;
	public long     irsim_max_time;

	public long     irsim_cur_delta;	    /* current simulated time */
	public Node     irsim_cur_node;	        /* node that belongs to current event */
	public long     irsim_nevent;		    /* number of current event */
	public int      irsim_npending;         /* number of pending events */

	public int      irsim_tunitdelay = 0;	/* if <> 0, all transitions take 'irsim_tunitdelay' DELAY-units */
	public long     irsim_tdecay = 0;		/* if <> 0, undriven nodes decay to X after 'irsim_tdecay' DELAY-units */

	private boolean  parallelWarning = false;
	private HashMap nodeHash;
	private List nodeList;
	private int nodeIndexCounter;
	private boolean warnVdd, warnGnd;

	private Eval     irsim_model;
	private Config   theConfig;

	Sim(Analyzer analyzer)
	{
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

	private void BAD_ARGC(String fileName, LineNumberReader lineReader, char cmd, String [] strings)
	{
		irsim_error(fileName, lineReader.getLineNumber(), "Wrong number of args for '" + cmd + "'");
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

	/**
	 * Traverse the transistor list and add the node connection-list.  We have
	 * to be careful with ALIASed nodes.  Note that transistors with source/drain
	 * connected VDD and GND nodes are not linked.
	 */
	private Node connect_txtors()
	{
		long visited = VISITED;
		Node nd_list = null;

		Trans tnext = null;
		for(Trans t = rd_tlist; t != null; t = tnext)
		{
			tnext = t.getSTrans();
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
				t.ttype |= TCAP;		// transistor is just a capacitor
				LINK_TCAP(t);
			} else
			{
				// do not connect gate if ALWAYSON since they do not matter
				if ((t.ttype & ALWAYSON) != 0)
				{
					irsim_on_trans = CONNECT(irsim_on_trans, t);
				} else
				{
					((Node)t.gate).ngate = CONNECT(((Node)t.gate).ngate, t);
				}

				if ((src.nflags & POWER_RAIL) == 0)
				{
					src.nterm = CONNECT(src.nterm, t);
					nd_list = LINK_TO_LIST(src, nd_list, visited);
				}
				if ((drn.nflags & POWER_RAIL) == 0)
				{
					drn.nterm = CONNECT(drn.nterm, t);
					nd_list = LINK_TO_LIST(drn, nd_list, visited);
				}
			}
		}

		return nd_list;
	}

	/**
	 * Transistors that have their drain/source shorted are NOT connected
	 * to the network, they are instead linked as a doubly linked list
	 * using the scache/dcache fields.
	 */
	private void LINK_TCAP(Trans t)
	{
		t.setDTrans(irsim_tcap);
		t.setSTrans(irsim_tcap.getSTrans());
		irsim_tcap.getSTrans().setDTrans(t);
		irsim_tcap.setSTrans(t);
		irsim_tcap.x++;
	}

	private void UNLINK_TCAP(Trans t)
	{
		t.getDTrans().setSTrans(t.getSTrans());
		t.getSTrans().setDTrans(t.getDTrans());
		t.ttype &= ~TCAP;
		irsim_tcap.x--;
	}

	/**
	 * Add transistor T to the list of transistors connected to that list.
	 * The transistor is added at the head of the list.
	 */
	private Tlist CONNECT(Tlist list, Trans t)
	{
		Tlist newl = new Tlist();
		newl.xtor = t;
		newl.next = list;
		return newl;
	}

	/**
	 * Remove the entry for transistor t from "list", and return it to the
	 * free pool.
	 */
	private Tlist DISCONNECT(Tlist list, Trans t)
	{
		Tlist last = null;
		for(Tlist li = list; li != null; li = li.next)
		{
			if (li.xtor == t)
			{
				if (last != null)
				{
//					FREE_LINK(li);
					last.next = li.next;
					return list;
				} else
				{
//					FREE_LINK(li);
					return li.next;
				}
			}
			last = li;
		}
		return list;
	}

	/**
	 * if FLAG is not set in in NODE->nflags, Link NODE to the head of LIST
	 * using the temporary entry (n.next) in the node structure.  This is
	 * used during net read-in/change to build lists of affected nodes.
	 */
	private Node LINK_TO_LIST(Node n, Node list, long FLAG)
	{
		if ((n.nflags & (FLAG)) == 0)
		{
			n.nflags |= (FLAG);
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
			BAD_ARGC(fileName, lineReader, 'N', targ);

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
			BAD_ARGC(fileName, lineReader, 'M', targ);

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

		long length = 0, width = 0;
		if (implant == RESIST)
		{
			if (targ.length != 4)
			{
				BAD_ARGC(fileName, lineReader, 'r', targ);
				return;
			}

			t.gate = irsim_VDD_node;
			t.source = irsim_GetNode(targ[1]);
			t.drain = irsim_GetNode(targ[2]);

			length = (long)(TextUtils.atof(targ[3]) * theConfig.irsim_LAMBDACM);
		} else
		{
			if (targ.length != 11)
			{
				BAD_ARGC(fileName, lineReader, targ[0].charAt(0), targ);
				return;
			}

			t.gate = irsim_GetNode(targ[1]);
			t.source = irsim_GetNode(targ[2]);
			t.drain = irsim_GetNode(targ[3]);

			length = (long)(TextUtils.atof(targ[4]) * theConfig.irsim_LAMBDACM);
			width = (long)(TextUtils.atof(targ[5]) * theConfig.irsim_LAMBDACM);
			if (width <= 0 || length <= 0)
			{
				irsim_error(fileName, lineReader.getLineNumber(),
					"Bad transistor width=" + width + " or length=" + length);
				return;
			}
			((Node)t.gate).ncap += length * width * theConfig.irsim_CTGA;

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
						t.source.ncap += asrc * theConfig.irsim_LAMBDA * theConfig.irsim_LAMBDA * theConfig.irsim_CPDA + psrc * theConfig.irsim_LAMBDA * theConfig.irsim_CPDP;
						t.drain.ncap += adrn * theConfig.irsim_LAMBDA * theConfig.irsim_LAMBDA * theConfig.irsim_CPDA + pdrn * theConfig.irsim_LAMBDA * theConfig.irsim_CPDP;
					}
					else if (implant == NCHAN || implant == DEP)
					{
						t.source.ncap += asrc * theConfig.irsim_LAMBDA * theConfig.irsim_LAMBDA * theConfig.irsim_CDA + psrc * theConfig.irsim_LAMBDA * theConfig.irsim_CDP;
						t.drain.ncap += adrn * theConfig.irsim_LAMBDA * theConfig.irsim_LAMBDA * theConfig.irsim_CDA + pdrn * theConfig.irsim_LAMBDA * theConfig.irsim_CDP;
					}
				}
			}
		}

		// link it to the list
		t.setSTrans(rd_tlist);
		rd_tlist = t;

		t.r = theConfig.irsim_requiv(implant, width, length);
	}

	/**
	 * accept a bunch of aliases for a node (= sim command).
	 */
	private void alias(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length < 3)
			BAD_ARGC(fileName, lineReader, '=', targ);

		Node n = irsim_GetNode(targ[1]);

		for(int i = 2; i < targ.length; i++)
		{
			Node m = irsim_GetNode(targ[i]);
			if (m == n)
				continue;

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
			BAD_ARGC(fileName, lineReader, 't', targ);

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
			BAD_ARGC(fileName, lineReader, 'D', targ);

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
		}
		else if (targ.length == 4)		// two terminal caps	*/
		{
			float cap = (float)(TextUtils.atof(targ[3]) / 1000);		// ff to pf conversion
			Node n = irsim_GetNode(targ[1]);
			Node m = irsim_GetNode(targ[2]);
			if (n != m)			// add cap to both nodes
			{
				if (m != irsim_GND_node)	m.ncap += cap;
				if (n != irsim_GND_node)	n.ncap += cap;
			}
			else if (n == irsim_GND_node)	// same node, only GND makes sense
			n.ncap += cap;
		}
		else
			BAD_ARGC(fileName, lineReader, 'c', targ);
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

	private void input_sim(URL simFileURL)
	{
		boolean R_error = false;
		boolean A_error = false;

		String fileName = simFileURL.getFile();
		InputStream inputStream = null;
		try
		{
			URLConnection urlCon = simFileURL.openConnection();
			inputStream = urlCon.getInputStream();
			InputStreamReader is = new InputStreamReader(inputStream);
			LineNumberReader lineReader = new LineNumberReader(is);
			for(;;)
			{
				String line = lineReader.readLine();
				if (line == null) break;
				String [] targ = parse_line(line, false);
				if (targ.length == 0) continue;
				char firstCh = targ[0].charAt(0);
				switch(firstCh)
				{
					case '|':
						if (lineReader.getLineNumber() > 1) break;
						if (targ.length >= 2)
						{
							double lmbd = TextUtils.atof(targ[2])/100.0;

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
						if (! R_error)	// only warn about this 1 time
						{
							System.out.println(fileName + "Ignoring lumped-resistance ('R' construct)");
							R_error = true;
						}
						break;
					case 'A':
						if (! A_error)	// only warn about this 1 time
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

	public boolean irsim_rd_network(URL simFileURL)
	{
		rd_tlist = null;
		nodeHash = new HashMap();
		nodeList = new ArrayList();
		nodeIndexCounter = 1;
		warnVdd = warnGnd = false;

		// initialize counts
		for(int i = 0; i < NTTYPES; i++)
			irsim_ntrans[i] = 0;
		irsim_nnodes = irsim_naliases = 0;
		irsim_init_hist();

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

		input_sim(simFileURL);
		if (nerrs > 0) return true;
		return false;
	}

	private void irsim_pTotalNodes()
	{
		String infstr = irsim_nnodes + " nodes";
		if (irsim_naliases != 0)
			infstr += ", " + irsim_naliases + " aliases";
		System.out.println(infstr);
	}

	private void irsim_pTotalTxtors()
	{
		String infstr = "transistors:";
		for(int i = 0; i < NTTYPES; i++)
		{
			if (irsim_ntrans[i] == 0) continue;
			infstr += " " + irsim_ttype[i] + "=" + irsim_ntrans[i];
		}
		if (irsim_tcap.x != 0)
			infstr += " shorted=" + irsim_tcap.x;
		System.out.println(infstr);
	}

	public void irsim_ConnectNetwork()
	{
		irsim_pTotalNodes();

		Node ndlist = connect_txtors();

		irsim_make_parallel(ndlist);

		irsim_pTotalTxtors();
		irsim_pParallelTxtors();
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
			for(Tlist l = thisone.nterm; l != null; l = l.next)
			{
				Trans t = l.xtor;
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
					Trans  tran = other.getTrans();

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

	/* combine 2 resistors in parallel */
	static double COMBINE(double r1, double r2) { return (r1 * r2) / (r1 + r2); }

	/* combine 2 resistors in parallel, watch out for zero resistance */
	static double COMBINE_R(double a, double b) { return ((a + b <= SMALL) ? 0 : COMBINE(a, b)); }

	/********************************************** HISTORY *******************************************/

	private void irsim_init_hist()
	{
		irsim_max_time = MAX_TIME;

		HistEnt dummy = new HistEnt();
		irsim_last_hist = dummy;
		dummy.next = irsim_last_hist;
		dummy.htime = irsim_max_time;
		dummy.val = X;
		dummy.inp = true;
		dummy.punt = false;
		dummy.delay = dummy.rtime = 0;

		// initialize globals
		irsim_num_edges = 0;
		irsim_num_punted = 0;
		irsim_num_cons_punted = 0;
	}

	/**
	 * time at which history entry was enqueued
	 */
	private long QTIME(HistEnt h) { return h.htime - h.delay; }

	private long PuntTime(HistEnt h) { return h.htime - h.ptime; }

	/**
	 * Add a new entry to the history list.  Update curr to point to this change.
	 */
	public void irsim_AddHist(Node node, int value, boolean inp, long time, long delay, long rtime)
	{
		irsim_num_edges++;
		HistEnt curr = node.curr;

		if ((node.nflags & HIST_OFF) != 0)
		{
			// Old entries are deleted. Keep only last entry for delay calculation
			irsim_FreeHistList(node);
			curr = node.curr;
		}

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
		if ((node.nflags & HIST_OFF) != 0)
			return;

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

	/**
	 * Free up a node's history list
	 */
	private void irsim_FreeHistList(Node node)
	{
		HistEnt h = node.head.next;
		if (h == irsim_last_hist)		// nothing to do
			return;

		HistEnt next;
		while((next = h.next) != irsim_last_hist)		// find last entry
			h = next;

		node.head.next = irsim_last_hist;
		node.curr = node.head;
	}

	public static HistEnt NEXTH(HistEnt p)
	{
		HistEnt h;
		for(h = p.next; h.punt; h = h.next) ;
		return h;
	}

	public void irsim_FlushHist(long ftime)
	{
		for(Iterator it = irsim_GetNodeList().iterator(); it.hasNext(); )
		{
			Node n = (Node)it.next();
			HistEnt head = n.head;
			if (head.next == irsim_last_hist || (n.nflags & ALIAS) != 0)
				continue;
			HistEnt p = head;
			HistEnt h = NEXTH(p);
			while((long)h.htime < ftime)
			{
				p = h;
				h = NEXTH(h);
			}
			head.val = p.val;
			head.htime = p.htime;
			head.inp = p.inp;
			while (p.next != h)
				p = p.next;
			if (head.next != h)
			{
				head.next = h;
			}
			if ((long)n.curr.htime < ftime)
			{
				n.curr = head;
			}
		}
	}

	public int irsim_backToTime(Node nd)
	{
		if ((nd.nflags & (ALIAS | MERGED)) != 0)
			return 0;

		HistEnt h = nd.head;
		HistEnt p = NEXTH(h);
		while((long)p.htime < irsim_cur_delta)
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
				if (PuntTime(h) < irsim_cur_delta)	// already punted, skip it
					continue;

				qtime = (long)h.htime - (long)h.delay;	// pending, enqueue it
				if (qtime < irsim_cur_delta)
				{
					long tmp = irsim_cur_delta;
					irsim_cur_delta = qtime;
					irsim_model.irsim_enqueue_event(nd, (int) h.val, (long) h.delay, (long) h.rtime);
					irsim_cur_delta = tmp;
				}
				p.next = h.next;
				h = p;
			} else
			{
				qtime = QTIME(h);
				if (qtime < irsim_cur_delta)		// pending, enqueue it
				{
					long tmp = irsim_cur_delta;
					irsim_cur_delta = qtime;
					irsim_model.irsim_enqueue_event(nd, (int) h.val, (long) h.delay, (long) h.rtime);
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

		if (nd.ngate != null)		// recompute transistor states
		{
			for(Tlist l = nd.ngate; l != null; l = l.next)
			{
				Trans t = l.xtor;
				t.state = (byte)irsim_model.compute_trans_state(t);
			}
		}
		return 0;
	}

	/************************************ PARALLEL *******************************************/

	private int  []  nored = new int[NTTYPES];

	/**
	 * Run through the list of nodes, collapsing all transistors with the same
	 * gate/source/drain into a compound transistor.
	 */
	public void irsim_make_parallel(Node nlist)
	{
		long cl = VISITED;
		for(cl = ~cl; nlist != null; nlist.nflags &= cl, nlist = nlist.getNext())
		{
			for(Tlist l1 = nlist.nterm; l1 != null; l1 = l1.next)
			{
				Trans t1 = l1.xtor;
				int type = t1.ttype;
				if ((type & (GATELIST | ORED)) != 0)
					continue;	// ORED implies processed, so skip as well

				long hval = hash_terms(t1);
				Tlist prev = l1;
				for(Tlist l2 = l1.next; l2 != null; prev = l2, l2 = l2.next)
				{
					Trans t2 = l2.xtor;
					if (t1.gate != t2.gate || hash_terms(t2) != hval ||
						type != (t2.ttype & ~ORED))
							continue;

					if ((t1.ttype & ORED) == 0)
					{
						t2 = new Trans();
						t2.r = new Resists();
						t2.r.dynres[R_LOW] = t1.r.dynres[R_LOW];
						t2.r.dynres[R_HIGH] = t1.r.dynres[R_HIGH];
						t2.r.rstatic = t1.r.rstatic;
						t2.gate = t1.gate;
						t2.source = t1.source;
						t2.drain = t1.drain;
						t2.ttype = (byte)((t1.ttype & ~ORLIST) | ORED);
						t2.state = t1.state;
						t2.tflags = t1.tflags;
						t2.tlink = t1;
						t1.setSTrans(null);
						t1.setDTrans(t2);
						REPLACE(((Node)t1.gate).ngate, t1, t2);
						REPLACE(t1.source.nterm, t1, t2);
						REPLACE(t1.drain.nterm, t1, t2);
						t1.ttype |= ORLIST;
						t1 = t2;
						t2 = l2.xtor;
						nored[BASETYPE(t1.ttype)]++;
					}

					Resists  r1 = t1.r, r2 = t2.r;
					r1.rstatic = (float)COMBINE(r1.rstatic, r2.rstatic);
					r1.dynres[R_LOW] = (float)COMBINE(r1.dynres[R_LOW], r2.dynres[R_LOW]);
					r1.dynres[R_HIGH] = (float)COMBINE(r1.dynres[R_HIGH], r2.dynres[R_HIGH]);

					((Node)t2.gate).ngate = DISCONNECT(((Node)t2.gate).ngate, t2);	// disconnect gate
					if (t2.source == nlist)		// disconnect term1
						t2.drain.nterm = DISCONNECT(t2.drain.nterm, t2);
					else
						t2.source.nterm = DISCONNECT(t2.source.nterm, t2);

					prev.next = l2.next;			// disconnect term2
					FREE_LINK(l2);
					l2 = prev;

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

	/**
	 * Return "Tlist" pointer LP to free pool.
	 */
	private void FREE_LINK(Tlist lp)
	{
		lp.next = irsim_freeLinks;
		irsim_freeLinks = lp;
	}

	/**
	 * Replace the first ocurrence of transistor "oldT" by "newT" on "list".
	 */
	private void REPLACE(Tlist list, Trans oldT, Trans newT)
	{
		for(Tlist lp = list; lp != null; lp = lp.next)
		{
			if (lp.xtor == oldT)
			{
				lp.xtor = newT;
				break;
			}
		}
	}

	private void irsim_UnParallelTrans(Trans t)
	{
		if ((t.ttype & ORLIST) == 0) return;				// should never be

		Trans tor = t.getDTrans();
		if (tor.tlink == t)
			tor.tlink = t.getSTrans();
		else
		{
			Trans  tp;

			for(tp = tor.tlink; tp != null; tp = tp.getSTrans())
			{
				if (tp.getSTrans() == t)
				{
					tp.setSTrans(t.getSTrans());
					break;
				}
			}
		}

		if (tor.tlink == null)
		{
			REPLACE(((Node)tor.gate).ngate, tor, t);
			REPLACE(tor.source.nterm, tor, t);
			REPLACE(tor.drain.nterm, tor, t);
		} else
		{
			Resists ror = tor.r;
			Resists r = t.r;

			double dr = r.rstatic - ror.rstatic;
			ror.rstatic = (float)((ror.rstatic * r.rstatic) / dr);
			dr = r.dynres[R_LOW] - ror.dynres[R_LOW];
			ror.dynres[R_LOW] = (float)((ror.dynres[R_LOW] * r.dynres[R_LOW]) / dr);
			dr = r.dynres[R_HIGH] - ror.dynres[R_HIGH];
			ror.dynres[R_HIGH] = (float)((ror.dynres[R_HIGH] * r.dynres[R_HIGH]) / dr);

			if ((t.ttype & ALWAYSON) != 0)
			{
				irsim_on_trans = CONNECT(irsim_on_trans, t);
			} else
			{
				((Node)t.gate).ngate = CONNECT(((Node)t.gate).ngate, t);
			}
			if ((t.source.nflags & POWER_RAIL) == 0)
			{
				t.source.nterm = CONNECT(t.source.nterm, t);
			}
			if ((t.drain.nflags & POWER_RAIL) == 0)
			{
				t.drain.nterm = CONNECT(t.drain.nterm, t);
			}
		}
		t.ttype &= ~ORLIST;
		nored[BASETYPE(t.ttype)] -= 1;
	}

	private void irsim_pParallelTxtors()
	{
		String str = "Parallel txtors:";
		boolean any = false;
		for(int i = 0; i < NTTYPES; i++)
		{
			if (nored[i] != 0)
			{
				str += " " + irsim_ttype[i] + "=" + nored[i];
				any = true;
			}
		}
		if (any) System.out.println(str);
	}

}
