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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
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
		Simulation.SimDigitalSignal sig;
		Object   c;			/* combines time, nindex, cap, and event */

		Object   t;			/* combines cause, punts, and tranT */

		short    npot;		/* current potential */
		short    oldpot;	/* old potential (for incremental simulation). */
		long     nflags;	/* flag word (see defs below) */
		String   nname;		/* ascii name of node */

		Object   n;			/* combines thev, next, and tranN */

		HistEnt  head;		/* first entry in transition history */
		HistEnt  curr;		/* ptr. to current history entry */
		HistEnt  hchange;	/* special entry to avoid changing the history */ 
		short    awpot;		/* potential for pending AssertWhen */
		RSim.AssertWhen awpending;	/* pending asswertWhen list */
		int index;			/* index of this node (a unique value) */

		static HashMap hash;
		static List nodeList;
		static int indexCounter = 1;

		Node()
		{
			head = new HistEnt();
			hchange = new HistEnt();
			index = indexCounter++;
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

		/* find node in network */
		static Node irsim_find(String name)
		{
			return (Node)hash.get(name.toLowerCase());
		}
		
		/*
		 * Get node structure.  If not found, create a new one.
		 * If create is TRUE a new node is created and NOT entered into the table.
		 */
		static boolean warnVdd = false, warnGnd = false;
		
		static Node irsim_GetNode(String name)
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
			n = new Node();
		
			irsim_nnodes++;
		
			// insert node into hash table and list
			hash.put(name.toLowerCase(), n);
			nodeList.add(n);
		
			// initialize node entries
			n.nname = name;
			n.ngate = n.nterm = null;
			n.nflags = 0;
			n.ncap = (float)MIN_CAP;
			n.vlow = (float)Config.irsim_LOWTHRESH;
			n.vhigh = (float)Config.irsim_HIGHTHRESH;
			n.setTime(0);
			n.tplh = 0;
			n.tphl = 0;
			n.setCause(null);
			n.nlink = null;
			n.events = null;
			n.npot = X;
			n.awpending = null;

			n.head = new HistEnt();
			n.head.next = Hist.irsim_last_hist;
			n.head.htime = 0;
			n.head.val = X;
			n.head.inp = false;
			n.head.punt = false;
			n.head.rtime = n.head.delay = 0;
			n.curr = n.head;
		
			return n;
		}
		
		/*
		 * Return a list of all nodes in the network.
		 */
		static List irsim_GetNodeList()
		{
			return nodeList;
		}

		/* initialize hash table */
		static void irsim_init_hash()
		{
			hash = new HashMap();
			nodeList = new ArrayList();
		}
	};

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
	
	static class TranResist
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

	public static class Bits
	{
		Bits    next;		/* next bit vector in chain */
		String   name;		/* name of this vector of bits */
		int     traced;		/* <>0 if this vector is being traced */
		int     nbits;		/* number of bits in this vector */
		Node    nodes[];	/* pointers to the bits (nodes) */
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

	static int BASETYPE(int T) { return T & 0x07; }

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

	/* figure what's on the *other* terminal node of a transistor */
	static Node other_node(Trans t, Node n) { return t.drain == n ? t.source : t.drain; }

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

	/* Conversion macros between various time units */
	static double d2ns(long d) { return d * 0.01; }	/* deltas to ns */
	static double d2ps(long d) { return d * 10.0; }	/* deltas to ps */

	static long ns2d(double d) { return (long)(d * 100); }	/* ns to deltas */
	static long ps2d(double d) { return (long)(d * 0.1); }	/* ps to deltas */

	static double ps2ns(double d) { return (long)(d * 0.001); }	/* ps to ns */

	
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

	static Node   irsim_VDD_node;		/* power supply nodes */
	static Node   irsim_GND_node;
	
	static Tlist   irsim_on_trans;		/* always on transistors */
	
	static int    irsim_nnodes;			/* number of actual nodes */
	static int    irsim_naliases;		/* number of aliased nodes */
	static int [] irsim_ntrans = new int[NTTYPES];	/* number of txtors indexed by type */
	
	static Tlist   irsim_freeLinks = null;	/* free list of Tlist structs */	
	static Trans   irsim_tcap = null;		/* list of capacitor-transistors */
	
	static	int    nerrs = 0;		/* # of erros found in sim file */
	
	static	Trans   rd_tlist;		/* list of transistors just read */

	public static final int	MAX_ERRS	= 20;
	public static final int	FATAL		= (MAX_ERRS + 1);

	public static final int	MAX_PARALLEL	= 30;	/* this is probably sufficient per stage */
	static Trans  [] irsim_parallel_xtors = new Trans[MAX_PARALLEL];
	
	static int hash_terms(Trans t) { return t.source.index ^ t.drain.index; }
	static int INPUT_NUM(int flg) { return ((flg & INPUT_MASK) >> 12); }

	public static final int	LIN_MODEL	= 0;
	public static final int	SWT_MODEL	= 1;
	
	static final double SMALL		= 1E-15;	    /* A small number */
	static final double LARGE		= 1E15;	    /* A large number */
	static final double LIMIT		= 1E8;	    /* R > LIMIT are considered infinite */


	static void BAD_ARGC(String fileName, LineNumberReader lineReader, char cmd, String [] strings)
	{
		irsim_error(fileName, lineReader.getLineNumber(), "Wrong number of args for '" + cmd + "'");
		for(int i=0; i<strings.length; i++) System.out.print(" " + strings[i]);
		System.out.println();
		CheckErrs(fileName);
	}
	
	

	static void irsim_error(String filename, int lineno, String msg)
	{
		System.out.println("(" + filename + "," + lineno + "): " + msg);
	}

	/*
	 * Routine to free all memory allocated in this module.
	 */
	static void irsim_freesimmemory()
	{
//		Trans  t;
//		Tlist l;
//	
//		while (irsim_on_trans != 0)
//		{
//			l = irsim_on_trans;
//			irsim_on_trans = l.next;
//			efree((CHAR *)l);
//		}
//		while (rd_tlist != 0)
//		{
//			t = rd_tlist;
//			rd_tlist = t.scache.t;
//			irsim_freetransistor(t);
//		}
//	/*	if (irsim_tcap != 0) irsim_freetransistor(t); */
//		while (irsim_freeLinks != 0)
//		{
//			l = irsim_freeLinks;
//			irsim_freeLinks = l.next;
//			efree((CHAR *)l);
//		}
	}
	
	static void irsim_freetransistor(Trans t)
	{
//		Node gate, src, drn;
//		Tlist l;
//	
//		for(gate = t.gate; gate != 0; gate = gate.nlink)
//		{
//			if ((gate.nflags & ALIAS) != 0) continue;
//			while (gate.ngate != 0)
//			{
//				l = gate.ngate;
//				gate.ngate = l.next;
//				efree((CHAR *)l);
//			}
//		}
//		for(src = t.source; src != 0; src = src.nlink)
//		{
//			if ((src.nflags & ALIAS) != 0) continue;
//			while (src.nterm != 0)
//			{
//				l = src.nterm;
//				src.nterm = l.next;
//				efree((CHAR *)l);
//			}
//		}
//		for(drn = t.drain; drn != 0; drn = drn.nlink)
//		{
//			if ((drn.nflags & ALIAS) != 0) continue;
//			while (drn.nterm != 0)
//			{
//				l = drn.nterm;
//				drn.nterm = l.next;
//				efree((CHAR *)l);
//			}
//		}
//		efree((CHAR *)t);
	}
	
	
	/*
	 * Returns TRUE if there have been too many errors and the activity should be stopped.
	 */
	static boolean CheckErrs(String fileName)
	{
		nerrs++;
		if (nerrs > MAX_ERRS)
		{
			System.out.println("Too many errors in sim file <" + fileName + ">");
			return true;
		}
		return false;
	}
	
	
	
	/*
	 * Traverse the transistor list and add the node connection-list.  We have
	 * to be careful with ALIASed nodes.  Note that transistors with source/drain
	 * connected VDD and GND nodes are not linked.
	 */
	static Node connect_txtors()
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
	
		return(nd_list);
	}

	/*
	 * Transistors that have their drain/source shorted are NOT connected
	 * to the network, they are instead linked as a doubly linked list
	 * using the scache/dcache fields.
	 */
	static void LINK_TCAP(Trans t)
	{
		t.setDTrans(irsim_tcap);
		t.setSTrans(irsim_tcap.getSTrans());
		irsim_tcap.getSTrans().setDTrans(t);
		irsim_tcap.setSTrans(t);
		irsim_tcap.x++;
	}


	static void UNLINK_TCAP(Trans t)
	{
		t.getDTrans().setSTrans(t.getSTrans());
		t.getSTrans().setDTrans(t.getDTrans());
		t.ttype &= ~TCAP;
		irsim_tcap.x--;
	}

	/*
	 * Add transistor T to the list of transistors connected to that list.
	 * The transistor is added at the head of the list.
	 */
	static Tlist CONNECT(Tlist list, Trans t)
	{
		Tlist newl = new Tlist();
		newl.xtor = t;
		newl.next = list;
		return newl;
	}

	/*
	 * Remove the entry for transistor t from "list", and return it to the
	 * free pool.
	 */
	static Tlist DISCONNECT(Tlist list, Trans t)
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

	/*
	 * if FLAG is not set in in NODE->nflags, Link NODE to the head of LIST
	 * using the temporary entry (n.next) in the node structure.  This is
	 * used during net read-in/change to build lists of affected nodes.
	 */
	static Node LINK_TO_LIST(Node n, Node list, long FLAG)
	{
		if ((n.nflags & (FLAG)) == 0)
		{
			n.nflags |= (FLAG);
			n.setNext(list);
			list = n;
		}
		return list;
	}

	/*
	 * node area and perimeter info (N sim command).
	 */
	static void node_info(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length != 8)
			BAD_ARGC(fileName, lineReader, 'N', targ);
	
		Node n = Node.irsim_GetNode(targ[1]);
	
		n.ncap += TextUtils.atof(targ[4]) * (Config.irsim_CMA * Config.irsim_LAMBDA2) +
			TextUtils.atof(targ[5]) * (Config.irsim_CPA * Config.irsim_LAMBDA2) +
			TextUtils.atof(targ[6]) * (Config.irsim_CDA * Config.irsim_LAMBDA2) +
			TextUtils.atof(targ[7]) * 2.0f * (Config.irsim_CDP * Config.irsim_LAMBDA);
	}
	
	
	/*
	 * new format node area and perimeter info (M sim command).
	 */
	static void nnode_info(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length != 14)
			BAD_ARGC(fileName, lineReader, 'M', targ);
	
		Node n = Node.irsim_GetNode(targ[1]);
	
		n.ncap += TextUtils.atof(targ[4]) * (Config.irsim_CM2A * Config.irsim_LAMBDA2) +
			TextUtils.atof(targ[5]) * 2.0 * (Config.irsim_CM2P * Config.irsim_LAMBDA) +
			TextUtils.atof(targ[6]) * (Config.irsim_CMA * Config.irsim_LAMBDA2) +
			TextUtils.atof(targ[7]) * 2.0 * (Config.irsim_CMP * Config.irsim_LAMBDA) +
			TextUtils.atof(targ[8]) * (Config.irsim_CPA * Config.irsim_LAMBDA2) +
			TextUtils.atof(targ[9]) * 2.0 * (Config.irsim_CPP * Config.irsim_LAMBDA) +
			TextUtils.atof(targ[10]) * (Config.irsim_CDA * Config.irsim_LAMBDA) +
			TextUtils.atof(targ[11]) * 2.0 * (Config.irsim_CDP * Config.irsim_LAMBDA) +
			TextUtils.atof(targ[12]) * (Config.irsim_CPDA * Config.irsim_LAMBDA2) +
			TextUtils.atof(targ[13]) * 2.0 * (Config.irsim_CPDP * Config.irsim_LAMBDA);
	}
	
	
	static	boolean    AP_error = false;
	
	/*
	 * new transistor.  Implant specifies type.
	 * AreaPos specifies the argument number that contains the area (if any).
	 */
	static void newtrans(int implant, String [] targ, String fileName, LineNumberReader lineReader)
	{	
		Node gate = null, src = null, drn = null;
		long length = 0, width = 0, x = 0, y = 0;
		int asrc = 0, adrn = 0, psrc = 0, pdrn = 0;
		double cap = 0;
		boolean fetHasAP = false;
		if (implant == RESIST)
		{
			if (targ.length != 4) BAD_ARGC(fileName, lineReader, 'r', targ);
	
			gate = irsim_VDD_node;
			src = Node.irsim_GetNode(targ[1]);
			drn = Node.irsim_GetNode(targ[2]);
	
			length = (long)(TextUtils.atof(targ[3]) * Config.irsim_LAMBDACM);
		}
		else
		{
			if (targ.length < 4 || targ.length > 11)
				BAD_ARGC(fileName, lineReader, targ[0].charAt(0), targ);
	
			gate = Node.irsim_GetNode(targ[1]);
			src = Node.irsim_GetNode(targ[2]);
			drn = Node.irsim_GetNode(targ[3]);

			width = length = 2 * Config.irsim_LAMBDACM;
			if (targ.length > 5)
			{
				length = (long)(TextUtils.atof(targ[4]) * Config.irsim_LAMBDACM);
				width = (long)(TextUtils.atof(targ[5]) * Config.irsim_LAMBDACM);
				if (width <= 0 || length <= 0)
				{
					irsim_error(fileName, lineReader.getLineNumber(),
						"Bad transistor width=" + width + " or length=" + length);
					return;
				}
				if (targ.length > 7)
				{
					x = TextUtils.atoi(targ[6]);
					y = TextUtils.atoi(targ[7]);
				}
			}
	
			cap = length * width * Config.irsim_CTGA;
		}
	
		Trans t = new Trans();			// create new transistor
	
		t.ttype = (byte)implant;
		t.gate = gate;
		t.source = src;
		t.drain = drn;
	
		if (targ.length > 7)
		{
			t.x = (int)x;
			t.y = (int)y;
			if (targ.length >= 9)
			{
				// parse area and perimeter
				fetHasAP = true;
				for (int i = 8; i < targ.length; i++)
				{
					int aIsPos = targ[i].indexOf("A_");
					int pIsPos = targ[i].indexOf("P_");
					if (aIsPos >= 0 && pIsPos >= 0)
					{
						int a = TextUtils.atoi(targ[i].substring(aIsPos+2));
						int p = TextUtils.atoi(targ[i].substring(pIsPos+2));
						char type = targ[i].charAt(0);
						if (type == 's')
						{
							asrc = a;
							psrc = p;
						} else if (type == 'd')
						{
							adrn = a;
							pdrn = p;
						}
					}
				}
			}
		} else
		{
			irsim_error(fileName, lineReader.getLineNumber(), "no position, area/perim S/D attributes on fet");
			AP_error = true;
		}
	
		t.setSTrans(rd_tlist);		// link it to the list
		rd_tlist = t;
	
		t.r = Config.irsim_requiv(implant, width, length);
	
		// update node capacitances
		gate.ncap += (float)cap;
	
		double capsrc = 0, capdrn = 0;
		if (fetHasAP)
		{
			if (implant == PCHAN)
			{
				capsrc = asrc * Config.irsim_LAMBDA * Config.irsim_LAMBDA * Config.irsim_CPDA + psrc * Config.irsim_LAMBDA * Config.irsim_CPDP;
				capdrn = adrn * Config.irsim_LAMBDA * Config.irsim_LAMBDA * Config.irsim_CPDA + pdrn * Config.irsim_LAMBDA * Config.irsim_CPDP;
			} 
			else if (implant == NCHAN || implant == DEP)
			{
				capsrc = asrc * Config.irsim_LAMBDA * Config.irsim_LAMBDA * Config.irsim_CDA + psrc * Config.irsim_LAMBDA * Config.irsim_CDP;
				capdrn = adrn * Config.irsim_LAMBDA * Config.irsim_LAMBDA * Config.irsim_CDA + pdrn * Config.irsim_LAMBDA * Config.irsim_CDP;
			} 
		} else if (! AP_error )
		{
			System.out.println("Warning: Junction capacitances might be incorrect");
			AP_error = true;
		}
		src.ncap += (float)capsrc;
		drn.ncap += (float)capdrn;
	}

	/*
	 * accept a bunch of aliases for a node (= sim command).
	 */
	static void alias(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length < 3)
			BAD_ARGC(fileName, lineReader, '=', targ);
	
		Node n = Node.irsim_GetNode(targ[1]);
	
		for(int i = 2; i < targ.length; i++)
		{
			Node m = Node.irsim_GetNode(targ[i]);
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
	
	
	/*
	 * node threshold voltages (t sim command).
	 */
	static void nthresh(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length != 4)
			BAD_ARGC(fileName, lineReader, 't', targ);
	
		Node n = Node.irsim_GetNode(targ[1]);
		n.vlow = (float)TextUtils.atof(targ[2]);
		n.vhigh = (float)TextUtils.atof(targ[3]);
	}
	
	
	/*
	 * User delay for a node (D sim command).
	 */
	static void ndelay(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length != 4)
			BAD_ARGC(fileName, lineReader, 'D', targ);
	
		Node n = Node.irsim_GetNode(targ[1]);
		n.nflags |= USERDELAY;
		n.tplh = (short)ns2d(TextUtils.atof(targ[2]));
		n.tphl = (short)ns2d(TextUtils.atof(targ[3]));
	}
	
	
	/*
	 * add capacitance to a node (c sim command).
	 */
	static void ncap(String [] targ, String fileName, LineNumberReader lineReader)
	{
		if (targ.length == 3)
		{
			Node n = Node.irsim_GetNode(targ[1]);
			n.ncap += (float)TextUtils.atof(targ[2]);
		}
		else if (targ.length == 4)		// two terminal caps	*/
		{
			float cap = (float)(TextUtils.atof(targ[3]) / 1000);		// ff to pf conversion
			Node n = Node.irsim_GetNode(targ[1]);
			Node m = Node.irsim_GetNode(targ[2]);
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
	
	
	/*
	 * parse input line into tokens, filling up carg and setting targc
	 * @param expand true to expand iterators.  For example, the
	 * string "out.{1:10}" expands into ten arguments "out.1", ..., "out.10".
	 * The string can contain multiple iterators which will be expanded
	 * independently, e.g., "out{1:10}{1:20:2}" expands into 100 arguments.
	 */
	static String [] parse_line(String line, boolean expand)
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

	static boolean expand(String arg, List expanded)
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

	
	static	boolean    R_error = false;
	static	boolean    A_error = false;

	static void input_sim(URL simFileURL)
	{
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
		
							if (lmbd != Config.irsim_LAMBDA)
							{
								System.out.println("WARNING: sim file lambda (" + lmbd + "u) != config lambda (" +
										Config.irsim_LAMBDA + "u), using config lambda");
							}
						}
						if (targ.length >= 6)
						{
							if (Config.irsim_CDA == 0.0 || Config.irsim_CDP == 0.0 ||
								Config.irsim_CPDA == 0.0 || Config.irsim_CPDP == 0.0)
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
		System.out.println("Loaded circuit, lambda=" + Config.irsim_LAMBDA + "u");
	}
	
	static void init_counts()
	{	
		for(int i = 0; i < NTTYPES; i++)
			irsim_ntrans[i] = 0;
		irsim_nnodes = irsim_naliases = 0;
	}

	static boolean irsim_rd_network(URL simFileURL)
	{
		rd_tlist = null;
		Node.irsim_init_hash();
		init_counts();
		Eval.irsim_init_eval();
		Hist.irsim_init_hist();
		NewRStep.irsim_init_newrstep();
		RSim.irsim_init_rsim();
		Sched.irsim_init_sched();
	
		irsim_VDD_node = Node.irsim_GetNode("Vdd");
		irsim_VDD_node.npot = HIGH;
		irsim_VDD_node.nflags |= (INPUT | POWER_RAIL);
		irsim_VDD_node.head.inp = true;
		irsim_VDD_node.head.val = HIGH;
		irsim_VDD_node.head.punt = false;
		irsim_VDD_node.head.htime = 0;
		irsim_VDD_node.head.rtime = irsim_VDD_node.head.delay = 0;
		irsim_VDD_node.head.next = Hist.irsim_last_hist;
		irsim_VDD_node.curr = irsim_VDD_node.head;
	
		irsim_GND_node = Node.irsim_GetNode("Gnd");
		irsim_GND_node.npot = LOW;
		irsim_GND_node.nflags |= (INPUT | POWER_RAIL);
		irsim_GND_node.head.inp = true;
		irsim_GND_node.head.val = LOW;
		irsim_GND_node.head.punt = false;
		irsim_GND_node.head.htime = 0;
		irsim_GND_node.head.rtime = irsim_GND_node.head.delay = 0;
		irsim_GND_node.head.next = Hist.irsim_last_hist;
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

	static void irsim_pTotalNodes()
	{	
		String infstr = irsim_nnodes + " nodes";
		if (irsim_naliases != 0)
			infstr += ", " + irsim_naliases + " aliases";
		System.out.println(infstr);
	}
	
	
	static void irsim_pTotalTxtors()
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
	
	
	static void irsim_ConnectNetwork()
	{
		irsim_pTotalNodes();
	
		Node ndlist = connect_txtors();
	
		Parallel.irsim_make_parallel(ndlist);
	
		irsim_pTotalTxtors();
		Parallel.irsim_pParallelTxtors();
	}

	/*
	 * Return transistor record T to free pool.
	 */
	static void	FREE_TRANS(Sim.Trans t)
	{
//	    t.gate = (Sim.Node) Sim.irsim_freeTrans;
//	    Sim.irsim_freeTrans = t;
	}

	static boolean parallelWarning = false;

	/*
	 * Build a linked-list of nodes (using nlink entry in Node structure)
	 * which are electrically connected to node 'n'.  No special order
	 * is required so tree walk is performed non-recursively by doing a
	 * breath-first traversal.  The value caches for each transistor we
	 * come across are reset here.  Loops are broken at an arbitrary point
	 * and parallel transistors are identified.
	 */
	static void irsim_BuildConnList(Node n)
	{
		int n_par = 0;
	
		n.nflags &= ~VISITED;
		NewRStep.irsim_withdriven = false;
	
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
					NewRStep.irsim_withdriven = true;
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
				else if (Eval.irsim_model.irsim_model_num != LIN_MODEL)
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

}
