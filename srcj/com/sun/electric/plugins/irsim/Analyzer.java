/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Analyzer.java
 * IRSIM simulator
 * Translated by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (C) 1988, 1990 Stanford University.
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies.  Stanford University
 * makes no representations about the suitability of this
 * software for any purpose.  It is provided "as is" without
 * express or implied warranty.
 */

package com.sun.electric.plugins.irsim;

import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.lib.LibFile;
import com.sun.electric.plugins.irsim.Sim.Node;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.WaveformWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

public class Analyzer
{
	/********** Simulation vectors **********/
	
	/* the meaning of SIMVECTOR.command */
	static final int VECTORCOMMENT    =  1;		/* a comment in the command file */
	static final int VECTORL          =  2;		/* the "l" command (set signal low) */
	static final int VECTORH          =  3;		/* the "h" command (set signal high) */
	static final int VECTORX          =  4;		/* the "x" command (set signal undefined) */
	static final int VECTORS          =  5;		/* the "s" command (advance time) */
	static final int VECTORASSERT     =  6;		/* the "assert" command (test signal value) */
	static final int VECTORCLOCK      =  7;		/* the "clock" command to declare stimuli */
	static final int VECTORC          =  8;		/* the "c" command to run a clock cycle */
	static final int VECTORVECTOR     =  9;		/* the "vector" command to group signals */
	static final int VECTORSTEPSIZE   = 10;		/* the "stepsize" command to set time advance */
	static final int VECTORPRINT      = 11;		/* the "print" command to display messages */
	static final int VECTORSET        = 12;		/* the "set" command to change vector values */
	static final int VECTOREXCL       = 13;		/* the "!" command to print gate info */
	static final int VECTORQUESTION   = 14;		/* the "?" command to print source/drain info */
	
	static class SimVector
	{
		int   command;
		String []   parameters;
		SimVector nextsimvector;
	};
	
	static SimVector irsim_firstvector = null;
	static SimVector irsim_lastvector = null;
	
	static class Times
	{
		long    first;
		long    last;
		long    start;
		long    steps;
		long    end;
	};

	static class Traces
	{
		int      disp;		/* number of traces displayed */
		TraceEnt    first;		/* ptr. to last trace displayed */
		TraceEnt    last;		/* list of traces */
	};
	
	
	static class Cache			/* Cache for history pointer */
	{
		Sim.HistEnt  wind;			/* window start */
		Sim.HistEnt  cursor;		/* cursor value */
	};
	
	static class TraceEnt
	{
		TraceEnt    next;		/* doubly linked list of traces */
		TraceEnt    prev;
		String     name;		/* name stripped of path */
		Sim.Node   waveformdata;
		int      len;		/* length of name string */
		int    top, bot;	/* position of the trace */
		short    bdigit;	/* # of bits per digit for displaying */
		boolean    vector;	/* 1 if bit vector, 0 if node */
//		union {
			Sim.Node    nd;		/* what makes up this trace */
			Sim.Bits    vec;
//		} n;
		Cache    [] cache;

		TraceEnt(int size)
		{
			cache = new Cache[size];
			for(int i=0; i<size; i++)
				cache[i] = new Cache();
		}
	};
	
	
	static final int DEF_STEPS      = 4;	/* default simulation steps per screen */
	static final double DEFIRSIMTIMERANGE  = 10.0E-9f;			/* initial size of simulation window: 10ns */

	static int      irsim_numAdded;
	static Traces   irsim_traces = new Traces();
	static Times    irsim_tims = new Times();
	static long irsim_lastStart;		/* last redisplay starting time */
	static WaveformWindow ww;
	
	static double  [] irsim_tracetime;
	static short  [] irsim_tracestate;
	static int   irsim_tracetotal = 0;
	
//	static CHAR     irsim_level_prefix[200];
//	static INTBIG   irsim_instnetnumber;
//	
//	/* working memory for "irsim_loadvectorfile()" */
//	static INTBIG   irsim_inputparametertotal = 0;
//	static CHAR   **irsim_inputparameters;
	
	/*
	 * Routine to free all memory allocated in this module.
	 */
	static void irsim_freeanalyzermemory()
	{
//		SimVector *sv;
//		TraceEnt t;
//		INTBIG i;
//	
//		for(i=0; i<irsim_inputparametertotal; i++)
//			efree((CHAR *)irsim_inputparameters[i]);
//		if (irsim_inputparametertotal > 0)
//			efree((CHAR *)irsim_inputparameters);
//		irsim_inputparametertotal = 0;
//		if (irsim_tracetotal > 0)
//		{
//			efree((CHAR *)irsim_tracetime);
//			efree((CHAR *)irsim_tracestate);
//			irsim_tracetotal = 0;
//		}
//		while (irsim_firstvector != null)
//		{
//			sv = irsim_firstvector;
//			irsim_firstvector = sv.nextsimvector;
//		}
//		while (irsim_traces.first != 0)
//		{
//			t = irsim_traces.first;
//			irsim_traces.first = t.next;
//			efree((CHAR *)t);
//		}
	}

	/************************** LOW-LEVEL ANALYZER **************************/
	
	static void irsim_addtrace(TraceEnt t)
	{
		if (irsim_traces.first == null)
		{
			t.next = t.prev = null;
			irsim_traces.first = irsim_traces.last = t;
		} else
		{
			t.next = null;
			t.prev = irsim_traces.last;
			irsim_traces.last.next = t;
			irsim_traces.last = t;
		}
		irsim_numAdded++;
	}

	static int irsim_AddNode(Sim.Node nd)
	{
		while ((nd.nflags & Sim.ALIAS) != 0)
			nd = nd.nlink;
	
		if ((nd.nflags & Sim.MERGED) != 0)
		{
			System.out.println("can't watch node " + nd.nname);
			return(1);
		}
		TraceEnt t = new TraceEnt(1);
		t.name = nd.nname;
		t.len = t.name.length();
		t.bdigit = 1;
		t.vector = false;
		t.nd = nd;
		nd.nflags &= ~Sim.HIST_OFF;
		t.cache[0].wind = t.cache[0].cursor = nd.head;
		irsim_addtrace(t);
		return(1);
	}

	int irsim_AddVector(Sim.Bits vec, int flag)
	{
		int n = vec.nbits;
		TraceEnt t = new TraceEnt(n - 1);
		t.name = vec.name;
		t.len = t.name.length();
		if (flag != 0)
			t.bdigit = (short)flag;
		else
			t.bdigit = (short)((n > 4) ? 4 : 1);
		t.vector = true;
		t.vec = vec;
		for(n--; n >= 0; n--)
		{
			vec.nodes[n].nflags &= ~Sim.HIST_OFF;	
			t.cache[n].wind = t.cache[n].cursor = vec.nodes[n].head;
		}
		irsim_addtrace(t);
		return(1);
	}

	static void irsim_DisplayTraces(boolean isMapped)
	{
		irsim_numAdded = 0;
		if (!isMapped)				// only the first time
		{
			irsim_FlushTraceCache();
		} else
		{
//			irsim_traces.disp = sim_window_getnumframes();
			irsim_DrawTraces(irsim_tims.start, irsim_tims.end);
		}
	}

	static void irsim_RestartAnalyzer(long first_time, long last_time, int same_hist)
	{
		for(TraceEnt t = irsim_traces.first; t != null; t = t.next)
		{
			if (t.vector)
			{
				for(int n = t.vec.nbits - 1; n >= 0; n--)
				{
					t.cache[n].wind = t.cache[n].cursor = 
						t.vec.nodes[n].head;
				}
			}
			else
				t.cache[0].wind = t.cache[0].cursor = t.nd.head;
		}
	
		// should set "last_time" to the width of the screen
		irsim_InitTimes(first_time, irsim_tims.steps / DEF_STEPS, last_time);
		if (same_hist != 0)
			irsim_UpdateTraceCache(0);
		else
			irsim_FlushTraceCache();
	}

	static void RemoveTrace(TraceEnt t)
	{
		if (t == irsim_traces.first)
		{
			irsim_traces.first = t.next;
			if (t.next != null)
				t.next.prev = null;
			else
				irsim_traces.last = null;
		} else
		{
			t.prev.next = t.next;
			if (t.next != null)
				t.next.prev = t.prev;
			else
				irsim_traces.last = t.prev;
		}
	}

	static void irsim_ClearTraces()
	{
		while (irsim_traces.first != null)
			RemoveTrace(irsim_traces.first);
	
		irsim_DrawTraces(irsim_tims.start, irsim_tims.end);
	}

	static void UpdateWinRemove()
	{
		irsim_DrawTraces(irsim_tims.start, irsim_tims.end);
	}

	void irsim_RemoveVector(Sim.Bits b)
	{
		boolean i = false;
	
		for(TraceEnt t = irsim_traces.first; t != null;)
		{
			if (t.vector && (t.vec == b))
			{
				TraceEnt tmp = t.next;
				RemoveTrace(t);
				t = tmp;
				i = true;
			}
			else
				t = t.next;
		}
		if (i)
			UpdateWinRemove();
	}

	void irsim_RemoveNode(Sim.Node n)
	{
		boolean i = false;
	
		for(TraceEnt t = irsim_traces.first; t != null;)
		{
			if (!t.vector && t.nd == n)
			{
				TraceEnt tmp = t.next;
				RemoveTrace(t);
				t = tmp;
				i = true;
			} else
				t = t.next;
		}
		if (i) UpdateWinRemove();
	}

	static void irsim_RemoveAllDeleted()
	{
		boolean i = false;
	
		for(TraceEnt t = irsim_traces.first; t != null;)
		{
			if ((t.vector && (t.vec.traced & Sim.DELETED) != 0) ||
				(! t.vector && (t.nd.nflags & Sim.DELETED) != 0))
			{
				TraceEnt tmp = t.next;
				RemoveTrace(t);
				t = tmp;
				i = true;
			} else
				t = t.next;
		}
		if (i) UpdateWinRemove();
	}

	/*
	 * Initialize the windows and various other metrics.
	 */
	static boolean irsim_InitDisplay()
	{
		return true;
	}

	/*
	 * Initialize the display times so that when first called the last time is
	 * shown on the screen.  Default width is DEF_STEPS (simulation) steps.
	 */
	static void irsim_InitTimes(long firstT, long stepsize, long lastT)
	{
		irsim_tims.first = firstT;
		irsim_tims.last = lastT;
		irsim_tims.steps = 4 * stepsize;
	
		if (irsim_tims.start <= irsim_tims.first)
		{
			if (lastT < irsim_tims.steps)
			{
				irsim_tims.start = irsim_tims.first;
				irsim_tims.end = irsim_tims.start + irsim_tims.steps;
	
				// make it conform to the displayed range
				Iterator it = ww.getPanels();
				if (it.hasNext())
				{
					WaveformWindow.Panel wp = (WaveformWindow.Panel)it.next();
					double max = wp.getMaxTimeRange();
					long endtime = Sim.ns2d(max * 1e9);
					if (endtime > irsim_tims.end) irsim_tims.end = endtime;
				}
				double max = irsim_endtime();
				long endtime = Sim.ns2d(max * 1e9);
				if (endtime > irsim_tims.end) irsim_tims.end = endtime;
			} else
			{
				irsim_tims.end = lastT + 2 * stepsize;
				irsim_tims.start = irsim_tims.end - irsim_tims.steps;
				if (irsim_tims.start < irsim_tims.first)
				{
					stepsize = irsim_tims.first - irsim_tims.start;
					irsim_tims.start += stepsize;
					irsim_tims.end += stepsize;
				}
			}
		}
	}

	/*
	 * Update the cache (begining of window and cursor) for traces that just
	 * became visible (or were just added).
	 */
	static void irsim_UpdateTraceCache(int first_trace)
	{
		long startT = irsim_tims.start;
		long cursT = irsim_tims.first;
		int n = 0;
		for(TraceEnt t = irsim_traces.first; t != null; n++, t = t.next)
		{
			if (n < first_trace)
				continue;
	
			if (t.vector)
			{
				for(int i = t.vec.nbits - 1; i >= 0; i--)
				{
					Sim.HistEnt p = t.cache[i].wind;
					Sim.HistEnt h = t.cache[i].cursor;
					Sim.HistEnt nexth = Hist.NEXTH(h);
					if (h.htime > cursT || nexth.htime <= cursT)
					{
						if (p.htime <= cursT)	// whatever is closer
							t.cache[i].cursor = p;
						else
							t.cache[i].cursor = t.vec.nodes[i].head;
					}
					if (startT <= p.htime)			// go back
						p = t.vec.nodes[i].head;
	
					h = Hist.NEXTH(p);
					while ((long)h.htime < startT)
					{
						p = h;
						h = Hist.NEXTH(h);
					}
					t.cache[i].wind = p;
	
					p = t.cache[i].cursor;
					h = Hist.NEXTH(p);
					while ((long)h.htime <= cursT)
					{
						p = h;
						h = Hist.NEXTH(h);
					}
					t.cache[i].cursor = p;
				}
			} else
			{
				Sim.HistEnt p = t.cache[0].wind;
				Sim.HistEnt h = t.cache[0].cursor;
				Sim.HistEnt nexth = Hist.NEXTH(h);
				if (h.htime > cursT || nexth.htime <= cursT)
				{
					if (p.htime <= cursT)
						t.cache[0].cursor = p;
					else
						t.cache[0].cursor = t.nd.head;
				}
	
				if (startT <= p.htime)
					p = t.nd.head;
	
				h = Hist.NEXTH(p);
				while (h.htime < startT)
				{
					p = h;
					h = Hist.NEXTH(h);
				}
				t.cache[0].wind = p;
	
				p = t.cache[0].cursor;
				h = Hist.NEXTH(p);
				while (h.htime <= cursT)
				{
					p = h;
					h = Hist.NEXTH(h);
				}
				t.cache[0].cursor = p;
			}
		}
	}

	static void irsim_FlushTraceCache()
	{
		irsim_lastStart = Hist.irsim_max_time;
	}

	/*
	 * Draw the traces horizontally from time1 to time2.
	 */
	static void irsim_DrawTraces(long t1, long t2)
	{
		if (irsim_tims.start != irsim_lastStart)		// Update history cache
		{
			long startT = irsim_tims.start;
			boolean begin = (startT < irsim_lastStart);
			int n = irsim_traces.disp;
			for(TraceEnt t = irsim_traces.first; t != null; n--, t = t.next)
			{
				if (t == null) break;
				if (t.vector)
				{
					for(int i = t.vec.nbits - 1; i >= 0; i--)
					{
						Sim.HistEnt p = begin ? t.vec.nodes[i].head : t.cache[i].wind;
						Sim.HistEnt h = Hist.NEXTH(p);
						while (h.htime < startT)
						{
							p = h;
							h = Hist.NEXTH(h);
						}
						t.cache[i].wind = p;
					}
				} else
				{
					Sim.HistEnt p = begin ? t.nd.head : t.cache[0].wind;
					Sim.HistEnt h = Hist.NEXTH(p);
					while (h.htime < startT)
					{
						p = h;
						h = Hist.NEXTH(h);
					}
					t.cache[0].wind = p;
				}
			}
			irsim_lastStart = irsim_tims.start;
		}
	
		for(TraceEnt t = irsim_traces.first; t != null; t = t.next)
		{
			if (!t.vector || t.vec.nbits <= 1)
			{
				if (t1 >= irsim_tims.last) continue;
				Sim.HistEnt h = t.cache[0].wind;
				int count = 0;
				long curt = 0;
				long endt = t2;
				while (curt < endt)
				{
					int val = h.val;
					while (h.htime < endt && h.val == val)
						h = Hist.NEXTH(h);
	
					// advance time
					long nextt;
					if (h.htime > endt)
					{
						nextt = endt;
					} else
					{
						nextt = h.htime;
					}

					// make sure there is room in the array
					if (count >= irsim_tracetotal)
					{
						int newtotal = irsim_tracetotal * 2;
						if (newtotal <= count) newtotal = count + 50;
						double [] newtime = new double[newtotal];
						short [] newstate = new short[newtotal];
						for(int i=0; i<count; i++)
						{
							newtime[i] = irsim_tracetime[i];
							newstate[i] = irsim_tracestate[i];
						}
						irsim_tracetime = newtime;
						irsim_tracestate = newstate;
						irsim_tracetotal = newtotal;
					}
	
					irsim_tracetime[count] = curt / 100000000000.0;
					switch(val)
					{
						case Sim.LOW:	// 0
							irsim_tracestate[count] = Simulation.SimData.LOGIC_LOW | Simulation.SimData.GATE_STRENGTH;
							break;
						case Sim.HIGH:	// 3
							irsim_tracestate[count] = Simulation.SimData.LOGIC_HIGH | Simulation.SimData.GATE_STRENGTH;
							break;
						case Sim.X:		// 1
							irsim_tracestate[count] = Simulation.SimData.LOGIC_X | Simulation.SimData.GATE_STRENGTH;
							break;
					}
					curt = nextt;
					count++;
				}
				double [] timeVector = new double[count];
				int [] stateVector = new int[count];
				for(int i=0; i<count; i++)
				{
					timeVector[i] = irsim_tracetime[i];
					stateVector[i] = irsim_tracestate[i];
				}
				t.waveformdata.sig.setTimeVector(timeVector);
				t.waveformdata.sig.setStateVector(stateVector);
			}
		}
		ww.repaint();
//		sim_window_redraw();
	}
	static void UpdateTraces(long start, long end)
	{
		irsim_DrawTraces(start, end);
	}

	/*
	 * Update the trace window so that endT is shown.  If the update fits in the
	 * window, simply draw the missing parts.  Otherwise scroll the traces,
	 * centered around endT.
	 */
	static void irsim_UpdateWindow(long endT)
	{
		long lastT = irsim_tims.last;
		irsim_tims.last = endT;
	
		for(TraceEnt t = irsim_traces.first; t != null; t = t.next)
		{
    		for(Iterator it = Sim.Node.irsim_GetNodeList().iterator(); it.hasNext(); )
    		{
    			Sim.Node n = (Sim.Node)it.next();
				if (t.name.equals(n.nname))
				{
					t.waveformdata = n;
					break;
				}
			}
		}
	
		if (endT <= irsim_tims.end)
		{
			if (lastT >= irsim_tims.start)
				UpdateTraces(lastT, endT);
			else if (endT > irsim_tims.start)
				UpdateTraces(irsim_tims.start, endT);
		} else					// endT > irsim_tims.end
		{
			if (lastT < irsim_tims.end)
				UpdateTraces(lastT, irsim_tims.end);
		}
	}

	/************************** ELECTRIC INTERFACE **************************/
	
	static void irsim_freememory()
	{
		Config.irsim_freeconfigmemory();
		Sim.irsim_freesimmemory();
		irsim_freeanalyzermemory();
	}

	static boolean first = true;

	/**
	 * Main entry point to start simulating a cell.
	 * @param cell the cell to simulate.
	 */
	public static void simulateCell(Cell cell)
	{	
		// first write the deck
		new StartIRSIM(cell);
	}

	/**
	 * Main entry point to set a value on a signal.
	 * @param level set current signal high (1), low (0), or undefined (-1).
	 */
	public static void setCurrentSignalLevel(Integer level)
	{
		int veccmd = VECTORL;
		switch (level.intValue())
		{
			case 0:		// low
				veccmd = VECTORL;
				break;
			case 1:		// high
				veccmd = VECTORH;
				break;
			case -1:		// undefined
				veccmd = VECTORX;
				break;
		}
		List signals = ww.getHighlightedNetworkNames();
		String [] parameters = new String[1];
		for(Iterator it = signals.iterator(); it.hasNext(); )
		{
			Simulation.SimSignal sig = (Simulation.SimSignal)it.next();
			parameters[0] = sig.getFullName();
			irsim_newvector(veccmd, parameters, ww.getMainTimeCursor());
		}
		irsim_playvectors();
	}

	/**
	 * Main entry point to clear all stimuli on a signal.
	 */
	public static void clearCurrentSignalStimuli()
	{
		List signals = ww.getHighlightedNetworkNames();
		if (signals.size() != 1)
		{
			System.out.println("Must select a single signal on which to clear stimuli");
			return;
		}
		Simulation.SimSignal sig = (Simulation.SimSignal)signals.get(0);
		String highsigname = sig.getFullName();

		SimVector lastsv = null;
		SimVector nextsv = null;
		for(SimVector sv = irsim_firstvector; sv != null; sv = nextsv)
		{
			nextsv = sv.nextsimvector;
			if (sv.command == VECTORL || sv.command == VECTORH || sv.command == VECTORX ||
				sv.command == VECTORASSERT || sv.command == VECTORSET)
			{
				if (sv.parameters[0].equals(highsigname))
				{
					if (lastsv == null)
						irsim_firstvector = sv.nextsimvector; else
							lastsv.nextsimvector = sv.nextsimvector;
					continue;
				}
			}
			lastsv = sv;
		}
		irsim_lastvector = lastsv;
		irsim_playvectors();
	}

	private static class StartIRSIM extends Job
    {
        Cell cell;

        public StartIRSIM(Cell cell)
        {
            super("Simulate cell "+cell.describe(), User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.cell = cell;
            startJob();
        }

        public boolean doIt()
        {
        	Output.writeCell(cell, null, "Electric.XXXXXX", FileType.IRSIM);
        	
    		irsim_freememory();

    		/* now initialize the simulator */
    		if (first)
    		{
    			NewRStep.irsim_InitThevs();
    		}
    		System.out.println("IRSIM " + Sim.irsim_version);

    		URL fileURL = TextUtils.makeURLToFile("Electric.XXXXXX");

    		// read the configuration file
//    		infstr = initinfstr();
//    		var = getval((INTBIG)sim_tool, VTOOL, VSTRING, x_("SIM_irsim_parameter_file"));
//    		if (var == NOVARIABLE)
//    			formatinfstr(infstr, x_("%s%s"), el_libdir, DEFIRSIMPARAMFILE); else
//    				addstringtoinfstr(infstr, (CHAR *)var.addr);
    		URL url = LibFile.getLibFile("scmos0.3.prm");
    		if (Config.irsim_config(url) != 0) return true;
    	
    		// Read network (.sim file)
    		if (Sim.irsim_rd_network(fileURL)) return true;

    		// remove the temporary network file
    		File f = new File("Electric.XXXXXX");
    		if (f != null) f.delete();
    	
    		Sim.irsim_ConnectNetwork();	// connect all txtors to corresponding nodes
    		if (first)
    			RSim.irsim_init_commands();	// set up command table
    		Sched.irsim_init_event();
    		first = false;
// display network
//System.out.println("XXXXXXXXXXXXXXXXXXXX THE NETWORK XXXXXXXXXXXXXXXXXXXXXXX");
//for(Iterator it = NSubrs.irsim_GetNodeList().iterator(); it.hasNext(); )
//{
//	Sim.Node n = (Sim.Node)it.next();
//	System.out.println("Node "+n.nname);
//	for(Sim.Tlist l = n.nterm; l != null; l = l.next)
//	{
//		Sim.Trans t = l.xtor;
//		System.out.println("   Has Transistor ("+t.x.pos+","+t.y.pos+")");
//		System.out.println("      Source: "+t.source.nname);
//		System.out.println("      Drain: "+t.drain.nname);
//		if (t.gate instanceof Sim.Node) System.out.println("      Gate: "+((Sim.Node)t.gate).nname); else
//			System.out.println("      Gate?: "+t.gate);
//	}
//}

    		// convert the stimuli
    		Simulation.SimData sd = new Simulation.SimData();
    		sd.setCell(cell);
    		for(Iterator it = Sim.Node.irsim_GetNodeList().iterator(); it.hasNext(); )
    		{
    			Sim.Node n = (Sim.Node)it.next();
				Simulation.SimDigitalSignal sig = new Simulation.SimDigitalSignal(null);
				n.sig = sig;
				sig.setSignalName(n.nname);
				sd.addSignal(sig);

				sig.buildTime(2);
    			sig.buildState(2);
				sig.setTime(0, 0);
				sig.setTime(1, 0.00000001);
				sig.setState(0, 0);
				sig.setState(1, 0);
    		}
			Simulation.showSimulationData(sd, null);
    		ww = sd.getWaveformWindow();

//    		// sort the signal names
//    		esort(sim_irsimnets, sim_irsimnetnumber, sizeof (IRSIMNETWORK), irsim_sortnetascending);
//    	
//    		// count the number of top-level signal names
//    		sigvalid = 0;
//    		for(i=0; i<sim_irsimnetnumber; i++)
//    		{
//    			if ((sim_irsimnets[i].flags&IRSIMNETVALID) == 0) continue;
//    			for(pt = sim_irsimnets[i].name; *pt != 0; pt++)
//    				if (*pt == '/') break;
//    			if (*pt != 0) continue;
//    			sigvalid++;
//    		}
//    		irsim_level_prefix[0] = 0;
//    	
//    		// show the waveform window
//    		irsim_firstvector = irsim_lastvector = null;
//    		oldsigcount = 0;
//    		i = sim_window_isactive(&np);
//    		if ((i&SIMWINDOWWAVEFORM) == 0)
//    		{
//    			if (i != 0 && np != cell)
//    			{
//    				// stop simulation of cell "np"
//    				sim_window_stopsimulation();
//    			}
//    	
//    			// remember signals that were last used with this cell
//    			sim_window_grabcachedsignalsoncell(cell);
//    	
//    			// show the simulation window
//    			if (sim_window_create(sigvalid, cell,
//    				((sim_window_state&SHOWWAVEFORM) != 0 ? irsim_charhandlerwave : 0),
//    					irsim_charhandlerschem, IRSIM)) return(TRUE);
//    			sim_window_state = (sim_window_state & ~SIMENGINECUR) | SIMENGINECURIRSIM;
//    			sim_window_killalltraces(TRUE);
    			irsim_showcurrentlevel(cell);
//    		}
    		ww.clearHighlighting();
    		
    		ww.setDefaultTimeRange(0.0, DEFIRSIMTIMERANGE);
    		ww.setMainTimeCursor(DEFIRSIMTIMERANGE/5.0*2.0);
    		ww.setExtensionTimeCursor(DEFIRSIMTIMERANGE/5.0*3.0);
    	
    		// tell the simulator to watch all signals
    		if (!RSim.irsim_analyzerON)
    		{
    			if (! irsim_InitDisplay()) return true;
    			irsim_InitTimes(RSim.irsim_sim_time0, 50000, Sched.irsim_cur_delta);
    		}

    		for(Iterator it = Sim.Node.irsim_GetNodeList().iterator(); it.hasNext(); )
    		{
    			Sim.Node n = (Sim.Node)it.next();
   				irsim_AddNode(n);
    		}
    		irsim_UpdateWindow(0);
    		irsim_DisplayTraces(RSim.irsim_analyzerON);		// pass 0 first time
    		RSim.irsim_analyzerON = true;
    	
    		// disable automatic display
    		boolean showcommand = false;
//    		var = getvalkey((INTBIG)sim_tool, VTOOL, VINTEGER, sim_irsim_statekey);
//    		if (var != NOVARIABLE && (var.addr&IRSIMSHOWCOMMANDS) != 0) showcommand = true;
    		RSim.irsim_issuecommand("display -automatic", showcommand);
    	
    		// read signal values
    		irsim_UpdateWindow(Sched.irsim_cur_delta);
            return true;
        }
    }

	static void irsim_showcurrentlevel(Cell cell)
	{
//		INTBIG i, j, k, l, len, buscount, oldsigcount;
//		CHAR *pt, *opt, *start, save, *name, *subname;
//		CHAR **oldsignames, **bussigname;
//		void *infstr;
//		VARIABLE *var;
//		INTBIG *bussignals;
//		INTSML inistate[1];
//		double initime[1], lowtime, hightime;
//		Q_UNUSED( cell );
//	
//		// remember the previous time range
//		sim_window_getaveragetimerange(&lowtime, &hightime);
	
		// show the waveform window
		irsim_ClearTraces();
//		ww.clearHighlighting();
//		sim_window_killalltraces(FALSE);
//		for(i=0; i<sim_irsimnetnumber; i++)
//		{
//			sim_irsimnets[i].flags &= ~IRSIMNETSHOWN;
//			sim_irsimnets[i].signal = 0;
//		}
//		infstr = initinfstr();
//		if (irsim_level_prefix[0] == 0)
//			addstringtoinfstr(infstr, _("Top level")); else
//		{
//			len = estrlen(irsim_level_prefix);
//			irsim_level_prefix[len-1] = 0;
//			formatinfstr(infstr, _("level=%s"), irsim_level_prefix);
//			irsim_level_prefix[len-1] = '/';
//		}
//		sim_window_titleinfo(returninfstr(infstr));
//		sim_window_supresstraceprefix(irsim_level_prefix);
//	
//		initime[0] = 0.0;   inistate[0] = (LOGIC_X << 8) | GATE_STRENGTH;
//	
//		// get signals saved from last time
//		oldsigcount = sim_window_getcachedsignals(&oldsignames);
//	
//		// if the cell was from a different point in the hierarchy, ignore saved signals
//		var = getvalkey((INTBIG)cell, VNODEPROTO, VSTRING, sim_window_hierpos_key);
//		if (var != NOVARIABLE)
//		{
//			if (namesame((CHAR *)var.addr, irsim_level_prefix) != 0)
//			{
//				if (getvalkey((INTBIG)cell, VNODEPROTO, VSTRING|VISARRAY,
//					sim_window_signalorder_key) != NOVARIABLE)
//						delvalkey((INTBIG)cell, VNODEPROTO, sim_window_signalorder_key);
//				oldsigcount = 0;
//			}
//		}
//	
//		// remember this position in the hierarchy
//		setvalkey((INTBIG)cell, VNODEPROTO, sim_window_hierpos_key, (INTBIG)irsim_level_prefix, VSTRING);
//	
//		// show the signals saved from last time
//		for(j=0; j<oldsigcount; j++)
//		{
//			// see if the name is a bus
//			for(pt = oldsignames[j]; *pt != 0; pt++) if (*pt == '\t') break;
//			if (*pt == '\t')
//			{
//				// a bus
//				pt++;
//				sim_initbussignals();
//				for(;;)
//				{
//					for(start = pt; *pt != 0; pt++) if (*pt == '\t') break;
//					save = *pt;
//					*pt = 0;
//					opt = start;
//					if (*opt == '-') opt++;
//					for( ; *opt != 0; opt++)
//						if (!isdigit(*opt) || *opt == ':') break;
//					if (*opt == ':') start = opt+1;
//					for(i=0; i<sim_irsimnetnumber; i++)
//					{
//						if ((sim_irsimnets[i].flags&IRSIMNETVALID) == 0) continue;
//						if ((sim_irsimnets[i].flags&IRSIMNETSHOWN) != 0) continue;
//						name = sim_irsimnets[i].name;
//						if (namesame(name, start) != 0) continue;
//						sim_irsimnets[i].signal = sim_window_newtrace(-1, name, 0);
//						sim_window_loaddigtrace(sim_irsimnets[i].signal, 1, initime, inistate);
//						sim_irsimnets[i].flags |= IRSIMNETSHOWN;
//						sim_addbussignal(sim_irsimnets[i].signal);
//						break;
//					}
//					*pt++ = save;
//					if (save == 0) break;
//				}
//	
//				// create the bus in the waveform window
//				infstr = initinfstr();
//				for(pt = oldsignames[j]; *pt != 0; pt++)
//				{
//					if (*pt == '\t') break;
//					addtoinfstr(infstr, *pt);
//				}
//				pt = returninfstr(infstr);
//				buscount = sim_getbussignals(&bussignals);
//				(void)sim_window_makebus(buscount, bussignals, pt);
//	
//				// make the bus in IRSIM
//				bussigname = (CHAR **)emalloc((buscount+1) * (sizeof (CHAR *)), sim_tool.cluster);
//				if (bussigname == 0) return;
//				for(i=0; i<buscount; i++) bussigname[i+1] = sim_window_gettracename(bussignals[i]);
//				bussigname[0] = pt;
//				irsim_newvector(VECTORVECTOR, buscount+1, bussigname, 0.0);
//				efree((CHAR *)bussigname);
//			} else
//			{
//				// a single signal
//				pt = oldsignames[j];
//				if (*pt == '-') pt++;
//				for( ; *pt != 0; pt++)
//					if (!isdigit(*pt) || *pt == ':') break;
//				if (*pt == ':') pt++; else pt = oldsignames[j];
//				for(i=0; i<sim_irsimnetnumber; i++)
//				{
//					if ((sim_irsimnets[i].flags&IRSIMNETVALID) == 0) continue;
//					if ((sim_irsimnets[i].flags&IRSIMNETSHOWN) != 0) continue;
//					name = sim_irsimnets[i].name;
//					if (namesame(name, pt) != 0) continue;
//	
//					sim_irsimnets[i].signal = sim_window_newtrace(-1, name, (INTBIG)0);
//					sim_window_loaddigtrace(sim_irsimnets[i].signal, 1, initime, inistate);
//					sim_irsimnets[i].flags |= IRSIMNETSHOWN;
//					break;
//				}
//			}
//		}
//	
//		// show default signals if none cached
//		if (oldsigcount == 0)
//		{
//			len = estrlen(irsim_level_prefix);
//			for(i=0; i<sim_irsimnetnumber; i++)
//			{
//				if ((sim_irsimnets[i].flags&IRSIMNETVALID) == 0) continue;
//				if ((sim_irsimnets[i].flags&IRSIMNETSHOWN) != 0) continue;
//				name = sim_irsimnets[i].name;
//				if (len != 0)
//				{
//					// down a level: the prefix must match
//					if (namesamen(name, irsim_level_prefix, len) != 0) continue;
//				}
//	
//				// there must be no further hierarchy in the name
//				for(l=len; name[l] != 0; l++) if (name[l] == '/') break;
//				if (name[l] != 0) continue;
//	
//				// see if the signal is arrayed
//				for(l=0; name[l] != 0; l++) if (name[l] == '[') break;
//				if (name[l] == '[')
//				{
//					// found an arrayed signal: gather it into a bus
//					sim_initbussignals();
//					for(j=0; j<sim_irsimnetnumber; j++)
//					{
//						if ((sim_irsimnets[j].flags&IRSIMNETVALID) == 0) continue;
//						if ((sim_irsimnets[j].flags&IRSIMNETSHOWN) != 0) continue;
//						subname = sim_irsimnets[j].name;
//						if (namesamen(subname, name, l) != 0) continue;
//						if (subname[l] != 0 && subname[l] != '[') continue;
//						sim_addbussignal(j);
//					}
//					buscount = sim_getbussignals(&bussignals);
//					bussigname = (CHAR **)emalloc((buscount+1) * (sizeof (CHAR *)), sim_tool.cluster);
//					if (bussigname == 0) return;
//					esort(bussignals, buscount, SIZEOFINTBIG, irsim_sortbusnames);
//					for(j=0; j<buscount; j++)
//					{
//						k = bussignals[j];
//						subname = sim_irsimnets[k].name;
//						bussignals[j] = sim_window_newtrace(-1, subname, 0);
//						bussigname[j+1] = subname;
//						sim_window_loaddigtrace(bussignals[j], 1, initime, inistate);
//						sim_irsimnets[k].signal = bussignals[j];
//						sim_irsimnets[k].flags |= IRSIMNETSHOWN;
//					}
//	
//					// create the bus
//					if (buscount > 1)
//					{
//						name[l] = 0;
//						infstr = initinfstr();
//						addstringtoinfstr(infstr, name);
//						bussigname[0] = returninfstr(infstr);
//						name[l] = '[';
//						(void)sim_window_makebus(buscount, bussignals, bussigname[0]);
//						irsim_newvector(VECTORVECTOR, buscount+1, bussigname, 0.0);
//					}
//					efree((CHAR *)bussigname);
//				} else
//				{
//					sim_irsimnets[i].signal = sim_window_newtrace(-1, name, 0);
//					sim_irsimnets[i].flags |= IRSIMNETSHOWN;
//					sim_window_loaddigtrace(sim_irsimnets[i].signal, 1, initime, inistate);
//				}
//			}
//		}
//	
//		// now set the time range to the previous state
//		ww.setDefaultTimeRange(lowtime, hightime);
//		sim_window_redraw();
	}

//	/*
//	 * Helper routine for "esort" that makes bus signals be numerically ordered
//	 */
//	int irsim_sortbusnames(const void *e1, const void *e2)
//	{
//		CHAR *n1, *n2;
//		INTBIG i1, i2;
//	
//		i1 = *((INTBIG *)e1);
//		i2 = *((INTBIG *)e2);
//		n1 = sim_irsimnets[i1].name;
//		n2 = sim_irsimnets[i2].name;
//		if ((net_options&NETDEFBUSBASEDESC) != 0)
//			return(namesamenumeric(n2, n1));
//		return(namesamenumeric(n1, n2));
//	}

//	/*
//	 * Helper routine for "esort" that makes IRSIM network objects go in ascending name order.
//	 */
//	int irsim_sortnetascending(const void *e1, const void *e2)
//	{
//		IRSIMNETWORK *in1, *in2;
//	
//		in1 = (IRSIMNETWORK *)e1;
//		in2 = (IRSIMNETWORK *)e2;
//		return(namesame(in1.name, in2.name));
//	}

//	/*
//	 * The character handler for the waveform window of ALS simulation
//	 */
//	BOOLEAN irsim_charhandlerwave(WINDOWPART *w, INTSML chr, INTBIG special)
//	{
//		NODEPROTO *np;
//		INTBIG *highsigs, highsig, i, j, thispos, *bussigs,
//			trl, nexttr, prevtr, pos, bitoffset, veccmd, numbits, buscount;
//		INTBIG state, *tracelist, *theBits;
//		BOOLEAN showcommand, foundsig;
//		VARIABLE *var;
//		CHAR *msg, *par[30], setvalue[MAXSIMWINDOWBUSWIDTH+1];
//		SimVector *sv, *lastsv, *nextsv;
//		void *infstr;
//	
//		// if not simulating, don't handle any simulation commands
//		if (sim_window_isactive(&np) == 0)
//			return(us_charhandler(w, chr, special));
//		veccmd = 0;
//		numbits = 0;
//		switch (chr)
//		{
//			// convert busses
//			case 'b':
//				if (sim_window_buscommand()) return(FALSE);
//				highsigs = sim_window_gethighlighttraces();
//	#if 0	// should enter a "vector" command here
//				{
//					CHAR *bussigname[MAXSIMWINDOWBUSWIDTH+1];
//					bussigname[0] = pt;
//					irsim_newvector(VECTORVECTOR, buscount+1, bussigname, 0.0);
//				}
//	#endif
//				irsim_UpdateWindow(Sched.irsim_cur_delta);
//				return(FALSE);
//	
//			// different flavors of wide numeric values
//			case 'v':
//				numbits = sim_window_getwidevalue(&theBits);
//				if (numbits < 0) return(FALSE);
//				break;
//	
//			// signal clock setting, info, erasing, removing (all handled later)
//			case 'e':
//			case 'r':
//			case DELETEKEY:
//			case 'i':
//			case ' ':
//				break;
//	
//			default:
//				return(us_charhandler(w, chr, special));
//		}
//	
//		// the following commands demand a current trace...get it
//		highsigs = sim_window_gethighlighttraces();
//		if (highsigs[0] == 0)
//		{
//			System.out.println("Select a signal name first"));
//			return(FALSE);
//		}
//		if (chr == 'r' || chr == DELETEKEY)		// remove trace(s)
//		{
//			ww.clearHighlighting();
//	
//			// delete them
//			nexttr = prevtr = 0;
//			for(j=0; highsigs[j] != 0; j++)
//			{
//				highsig = highsigs[j];
//				thispos = sim_window_gettraceframe(highsig);
//				bussigs = sim_window_getbustraces(highsig);
//				for(buscount=0; bussigs[buscount] != 0; buscount++) ;
//				sim_window_inittraceloop();
//				nexttr = prevtr = 0;
//				for(;;)
//				{
//					trl = sim_window_nexttraceloop();
//					if (trl == 0) break;
//					pos = sim_window_gettraceframe(trl);
//					if (pos > thispos)
//					{
//						if (pos-1 == thispos) nexttr = trl;
//						pos = pos - 1 + buscount;
//						sim_window_settraceframe(trl, pos);
//					} else if (pos == thispos-1) prevtr = trl;
//				}
//				if (buscount > 0)
//				{
//					for(i=0; i<buscount; i++)
//						sim_window_settraceframe(bussigs[i], thispos+i);
//				}
//	
//				// remove from the simulator's list
//				for(i=0; i<sim_irsimnetnumber; i++)
//				{
//					if (sim_irsimnets[i].signal == highsig)
//					{
//						sim_irsimnets[i].signal = 0;
//						break;
//					}
//				}
//	
//				// kill trace
//				sim_window_killtrace(highsig);
//			}
//	
//			// redraw
//			if (nexttr != 0)
//			{
//				sim_window_addhighlighttrace(nexttr);
//			} else if (prevtr != 0)
//			{
//				sim_window_addhighlighttrace(prevtr);
//			}
//			sim_window_redraw();
//			return(FALSE);
//		}
//	
//		if (highsigs[1] != 0)
//		{
//			System.out.println("Select just one signal name first"));
//			return(FALSE);
//		}
//		highsig = highsigs[0];
//	
//		if (chr == 'i')		// print signal info
//		{
//			showcommand = FALSE;
//			var = getvalkey((INTBIG)sim_tool, VTOOL, VINTEGER, sim_irsim_statekey);
//			if (var != NOVARIABLE && (var.addr&IRSIMSHOWCOMMANDS) != 0) showcommand = TRUE;
//	
//			infstr = initinfstr();
//			formatinfstr(infstr, x_("! %s"), sim_window_gettracename(highsig));
//			RSim.irsim_issuecommand(returninfstr(infstr), showcommand);
//			infstr = initinfstr();
//			formatinfstr(infstr, x_("? %s"), sim_window_gettracename(highsig));
//			RSim.irsim_issuecommand(returninfstr(infstr), showcommand);
//			return(FALSE);
//		}
//		if (chr == 'e')		// clear signal vectors
//		{
//			tracelist = sim_window_getbustraces(highsig);
//			lastsv = null;
//			for(sv = irsim_firstvector; sv != null; sv = nextsv)
//			{
//				nextsv = sv.nextsimvector;
//				if (sv.command == VECTORL || sv.command == VECTORH || sv.command == VECTORX ||
//					sv.command == VECTORASSERT || sv.command == VECTORSET)
//				{
//					foundsig = FALSE;
//					if (namesame(sv.parameters[0], sim_window_gettracename(highsig)) == 0)
//						foundsig = TRUE; else
//					{
//						if (tracelist != 0 && tracelist[0] != 0)
//						{
//							for(i=0; tracelist[i] != 0; i++)
//							{
//								if (namesame(sv.parameters[0], sim_window_gettracename(tracelist[i])) == 0)
//								{
//									foundsig = TRUE;
//									break;
//								}
//							}
//						}
//					}
//					if (foundsig)
//					{
//						if (lastsv == null)
//							irsim_firstvector = sv.nextsimvector; else
//								lastsv.nextsimvector = sv.nextsimvector;
//						continue;
//					}
//				}
//				lastsv = sv;
//			}
//			irsim_lastvector = lastsv;
//			irsim_playvectors();
//			return(FALSE);
//		}
//	
//		// handle setting of values on signals
//		if (chr == 'v')
//		{
//			tracelist = sim_window_getbustraces(highsig);
//			if (tracelist == 0 || tracelist[0] == 0)
//			{
//				System.out.println("Select a bus signal before setting numeric values on it"));
//				return(FALSE);
//			}
//			for(i=0; tracelist[i] != 0; i++) ;
//			bitoffset = numbits - i;
//			for(i=0; tracelist[i] != 0; i++)
//			{
//				if (i+bitoffset < 0 || theBits[i+bitoffset] == 0) setvalue[i] = '0'; else
//					setvalue[i] = '1';
//			}
//			setvalue[i] = 0;
//			par[0] = sim_window_gettracename(highsig);
//			par[1] = setvalue;
//			irsim_newvector(VECTORSET, 2, par, sim_window_getmaincursor());
//		} else
//		{
//			tracelist = sim_window_getbustraces(highsig);
//			if (tracelist != 0 && tracelist[0] != 0)
//			{
//				System.out.println("Cannot set level on a bus, use 'v' to set a value"));
//				return(FALSE);
//			}
//			par[0] = sim_window_gettracename(highsig);
//			irsim_newvector(veccmd, 1, par, sim_window_getmaincursor());
//		}
//		irsim_playvectors();
//		return(FALSE);
//	}

	/************************** SIMULATION VECTORS **************************/

	/*
	 * Routine to create a new simulation vector at time "time", on signal "sig",
	 * with state "state".  The vector is inserted into the play list in the proper
	 * order.
	 */
	static void irsim_newvector(int command, String [] params, double inserttime)
	{
		SimVector newsv = new SimVector();
		newsv.command = command;
		newsv.parameters = params;
	
		// insert the vector */
		SimVector lastsv = null;
		if (inserttime < 0.0)
		{
			lastsv = irsim_lastvector;
		} else
		{
			double defaultstepsize = 10.0 / 1000000000.0;
			double curtime = 0.0;
			int clockphases = 0;
			for(SimVector sv = irsim_firstvector; sv != null; sv = sv.nextsimvector)
			{
				switch (sv.command)
				{
					case VECTORCLOCK:
						clockphases = sv.parameters.length - 1;
						break;
					case VECTORS:
						double stepsize = defaultstepsize;
						if (sv.parameters.length > 0)
							stepsize = TextUtils.atof(sv.parameters[0]) / 1000000000.0;
						if (GenMath.doublesLessThan(inserttime, curtime+stepsize))
						{
							// splitting step at "inserttime"
							sv.parameters = new String[1];
							sv.parameters[0] = Double.toString((inserttime-curtime) * 1000000000.0);
	
							// create second step to advance after this signal
							SimVector aftersv = new SimVector();
							aftersv.command = VECTORS;
							aftersv.parameters = new String[1];
							aftersv.parameters[0] = Double.toString((curtime+stepsize-inserttime) * 1000000000.0);
							aftersv.nextsimvector = sv.nextsimvector;
							sv.nextsimvector = aftersv;
						}
						curtime += stepsize;
						break;
					case VECTORC:
						stepsize = defaultstepsize;
						if (sv.parameters.length > 0)
							stepsize = TextUtils.atof(sv.parameters[0]) / 1000000000.0;
						curtime += stepsize * clockphases;
						break;
					case VECTORSTEPSIZE:
						if (sv.parameters.length > 0)
							defaultstepsize = TextUtils.atof(sv.parameters[0]) / 1000000000.0;
						break;
				}
				lastsv = sv;
				if (!GenMath.doublesLessThan(curtime, inserttime)) break;
			}
			if (GenMath.doublesLessThan(curtime, inserttime))
			{
				// create step to advance to the insertion time
				double thisstep = (inserttime-curtime) * 1000000000.0;
				if (thisstep > 0.005)
				{
					SimVector aftersv = new SimVector();
					aftersv.command = VECTORS;
					aftersv.parameters = new String[1];
					aftersv.parameters[0] = Double.toString(thisstep);
					if (lastsv == null)
					{
						aftersv.nextsimvector = irsim_firstvector;
						irsim_firstvector = aftersv;
					} else
					{
						aftersv.nextsimvector = lastsv.nextsimvector;
						lastsv.nextsimvector = aftersv;
					}
					lastsv = aftersv;
				}
			}
		}
		if (lastsv == null)
		{
			newsv.nextsimvector = irsim_firstvector;
			irsim_firstvector = newsv;
		} else
		{
			newsv.nextsimvector = lastsv.nextsimvector;
			lastsv.nextsimvector = newsv;
		}
		if (newsv.nextsimvector == null)
		{
			irsim_lastvector = newsv;
		}
	}


	/*
	 * Routine to examine the test vectors and determine the ending simulation time.
	 */
	static double irsim_endtime()
	{
		double defaultstepsize = 10.0 / 1000000000.0;
		double curtime = 0.0;
		int clockphases = 0;
		for(SimVector sv = irsim_firstvector; sv != null; sv = sv.nextsimvector)
		{
			switch (sv.command)
			{
				case VECTORCLOCK:
					clockphases = sv.parameters.length - 1;
					break;
				case VECTORS:
					double stepsize = defaultstepsize;
					if (sv.parameters.length > 0)
						stepsize = TextUtils.atof(sv.parameters[0]) / 1000000000.0;
					curtime += stepsize;
					break;
				case VECTORC:
					stepsize = defaultstepsize;
					if (sv.parameters.length > 0)
						stepsize = TextUtils.atof(sv.parameters[0]) / 1000000000.0;
					curtime += stepsize * clockphases;
					break;
				case VECTORSTEPSIZE:
					if (sv.parameters.length > 0)
						defaultstepsize = TextUtils.atof(sv.parameters[0]) / 1000000000.0;
					break;
			}
		}
		return(curtime);
	}

	/*
	 * Routine to play the simulation vectors into the simulator.
	 */
	static void irsim_playvectors()
	{
		// get IRSIM state
		boolean showcommand = false;
//		var = getvalkey((INTBIG)sim_tool, VTOOL, VINTEGER, sim_irsim_statekey);
//		if (var != NOVARIABLE && (var.addr&IRSIMSHOWCOMMANDS) != 0) showcommand = true;
	
		RSim.irsim_issuecommand("back 0", showcommand);
		RSim.irsim_issuecommand("flush", showcommand);
	
		double curtime = 0;
		RSim.irsim_analyzerON = false;
		for(SimVector sv = irsim_firstvector; sv != null; sv = sv.nextsimvector)
		{
			if (sv.command == VECTORCOMMENT) continue;
			String infstr = irsim_commandname(sv.command);
			for(int i=0; i<sv.parameters.length; i++)
				infstr += " " + sv.parameters[i];
			RSim.irsim_issuecommand(infstr, showcommand);
		}
		RSim.irsim_issuecommand("s", showcommand);
		RSim.irsim_analyzerON = true;
		irsim_UpdateWindow(Sched.irsim_cur_delta);
	
		// update main cursor location if requested
//		if ((sim_window_state&ADVANCETIME) != 0)
//			ww.setMainTimeCursor(curtime + 10.0/1000000000.0);
	}

	static String irsim_commandname(int command)
	{
		switch (command)
		{
			case VECTORL:        return("l");
			case VECTORH:        return("h");
			case VECTORX:        return("x");
			case VECTORS:        return("s");
			case VECTORASSERT:   return("assert");
			case VECTORCLOCK:    return("clock");
			case VECTORC:        return("c");
			case VECTORVECTOR:   return("vector");
			case VECTORSTEPSIZE: return("stepsize");
			case VECTORPRINT:    return("print");
			case VECTORSET:      return("set");
			case VECTOREXCL:     return("!");
			case VECTORQUESTION: return("?");
		}
		return "";
	}

	/*
	 * Routine to clear all simulation vectors.
	 */
	void irsim_clearallvectors()
	{
		while (irsim_firstvector != null)
		{
			SimVector sv = irsim_firstvector;
			irsim_firstvector = sv.nextsimvector;
		}
		irsim_lastvector = null;
		irsim_playvectors();
	}

//	/*
//	 * Routine to read simulation vectors from file "filename" (if zero, prompt for file).
//	 */
//	void irsim_loadvectorfile(String filename)
//	{
//		CHAR *truename, buf[200], *pt, *keyword, *par[1], **newparamlist;
//		FILE *vin;
//		SimVector *sv;
//		double min, max;
//		INTBIG lineno, command, count, newtotal, i, j, *bussignals, *highsiglist;
//		INTBIG highsigcount;
//	
//		if (filename == 0)
//		{
//			filename = fileselect(_("IRSIM vector file"), sim_filetypeirsimcmd, x_(""));
//			if (filename == 0) return;
//		}
//		vin = xopen(filename, sim_filetypeirsimcmd, x_(""), &truename);
//		if (! vin)
//		{
//			System.out.println("ERROR: Can't open %s"), truename);
//			return;
//		}
//	
//		// remove all vectors
//		while (irsim_firstvector != null)
//		{
//			sv = irsim_firstvector;
//			irsim_firstvector = sv.nextsimvector;
//		}
//		irsim_lastvector = null;
//		System.out.println("Reading %s"), truename);
//		lineno = 0;
//		for(;;)
//		{
//			if (xfgets(buf, 200, vin)) break;
//			lineno++;
//	
//			// ignore comments
//			if (buf[0] == '|')
//			{
//				par[0] = buf;
//				irsim_newvector(VECTORCOMMENT, 1, par, -1.0);
//				continue;
//			}
//	
//			// get the first keyword
//			pt = buf;
//			keyword = getkeyword(&pt, x_(" "));
//			if (keyword == NOSTRING || *keyword == 0)
//			{
//				par[0] = buf;
//				irsim_newvector(VECTORCOMMENT, 1, par, -1.0);
//				continue;
//			}
//	
//			// ignore the "w" command
//			if (namesame(keyword, x_("w")) == 0)
//			{
//				par[0] = buf;
//				irsim_newvector(VECTORCOMMENT, 1, par, -1.0);
//				continue;
//			}
//	
//			// save the "print" command
//			if (namesame(keyword, x_("print")) == 0)
//			{
//				while (*pt == ' ') pt++;
//				par[0] = pt;
//				irsim_newvector(VECTORPRINT, 1, par, -1.0);
//				continue;
//			}
//	
//			// allow interruption
//			if (stopping(STOPREASONCOMFILE)) break;
//	
//			// handle level setting on signals
//			if (namesame(keyword, x_("l")) == 0 || namesame(keyword, x_("h")) == 0 ||
//				namesame(keyword, x_("x")) == 0)
//			{
//				if (namesame(keyword, x_("l")) == 0) command = VECTORL; else
//					if (namesame(keyword, x_("h")) == 0) command = VECTORH; else
//						command = VECTORX;
//				for(;;)
//				{
//					keyword = getkeyword(&pt, x_(" "));
//					if (keyword == NOSTRING || *keyword == 0) break;
//					par[0] = keyword;
//					irsim_newvector(command, 1, par, -1.0);
//				}
//				continue;
//			}
//	
//			// handle commands
//			if (namesame(keyword, x_("clock")) == 0) command = VECTORCLOCK; else
//			if (namesame(keyword, x_("vector")) == 0) command = VECTORVECTOR; else
//			if (namesame(keyword, x_("c")) == 0) command = VECTORC; else
//			if (namesame(keyword, x_("assert")) == 0) command = VECTORASSERT; else
//			if (namesame(keyword, x_("stepsize")) == 0) command = VECTORSTEPSIZE; else
//			if (namesame(keyword, x_("s")) == 0) command = VECTORS; else
//			if (namesame(keyword, x_("!")) == 0) command = VECTOREXCL; else
//			if (namesame(keyword, x_("?")) == 0) command = VECTORQUESTION; else
//			if (namesame(keyword, x_("set")) == 0) command = VECTORSET; else
//			{
//				System.out.println("Unknown IRSIM command on line %ld: %s"), lineno, buf);
//				continue;
//			}
//			count = 0;
//			for(;;)
//			{
//				keyword = getkeyword(&pt, x_(" "));
//				if (keyword == NOSTRING || *keyword == 0) break;
//				if (count >= irsim_inputparametertotal)
//				{
//					newtotal = irsim_inputparametertotal * 2;
//					if (count >= irsim_inputparametertotal)
//						newtotal = count + 5;
//					newparamlist = (CHAR **)emalloc(newtotal * (sizeof (CHAR *)), sim_tool.cluster);
//					if (newparamlist == 0) return;
//					for(i=0; i<irsim_inputparametertotal; i++)
//						newparamlist[i] = irsim_inputparameters[i];
//					for(i=irsim_inputparametertotal; i<newtotal; i++)
//						newparamlist[i] = 0;
//					if (irsim_inputparametertotal > 0) efree((CHAR *)irsim_inputparameters);
//					irsim_inputparameters = newparamlist;
//					irsim_inputparametertotal = newtotal;
//				}
//				if (irsim_inputparameters[count] != 0) efree((CHAR *)irsim_inputparameters[count]);
//				(void)allocstring(&irsim_inputparameters[count], keyword, sim_tool.cluster);
//				count++;
//			}
//			irsim_newvector(command, count, irsim_inputparameters, -1.0);
//	
//			// update the display if this is a "vector" command
//			if (command == VECTORVECTOR)
//			{
//				// make the bus
//				bussignals = (INTBIG *)emalloc((count-1) * SIZEOFINTBIG, el_tempcluster);
//				if (bussignals == 0) return;
//	
//				// if the bus name already exists, delete it
//				highsiglist = sim_window_findtrace(irsim_inputparameters[0], &highsigcount);
//				for(i=0; i<highsigcount; i++)
//					sim_window_killtrace(highsiglist[i]);
//	
//				j = 0;
//				for(i=1; i<count; i++)
//				{
//					highsiglist = sim_window_findtrace(irsim_inputparameters[i], &highsigcount);
//					if (highsigcount == 0)
//					{
//						System.out.println("Vector %s: cannot find signal '%s'"),
//							irsim_inputparameters[0], irsim_inputparameters[i]);
//					} else
//					{
//						bussignals[j++] = highsiglist[0];
//					}
//				}
//				if (j > 0)
//					(void)sim_window_makebus(j, bussignals, irsim_inputparameters[0]);
//				efree((CHAR *)bussignals);
//			}
//		}
//		irsim_playvectors();
//	
//		sim_window_gettimeextents(&min, &max);
//		ww.setDefaultTimeRange(min, max);
//		ww.setMainTimeCursor((max-min)/5.0*2.0+min);
//		ww.setExtensionTimeCursor((max-min)/5.0*3.0+min);
//		irsim_UpdateWindow(Sched.irsim_cur_delta);
//	
//		xclose(vin);
//	}

//	/*
//	 * Routine to save simulation vectors to file "filename" (if zero, prompt for file).
//	 */
//	void irsim_savevectorfile(String filename)
//	{
//		CHAR *truename;
//		INTBIG i;
//		FILE *vout;
//		SimVector *sv;
//		void *infstr;
//	
//		if (filename == 0)
//		{
//			infstr = initinfstr();
//			formatinfstr(infstr, x_("%s.cmd"), el_curlib.libname);
//			filename = fileselect(_("IRSIM vector file"), sim_filetypeirsimcmd|FILETYPEWRITE,
//				returninfstr(infstr));
//			if (filename == 0) return;
//		}
//		vout = xcreate(filename, sim_filetypeirsimcmd, 0, &truename);
//		if (vout == 0)
//		{
//			if (truename != 0) System.out.println("ERROR: Can't create %s"), truename);
//			return;
//		}
//	
//		for(sv = irsim_firstvector; sv != null; sv = sv.nextsimvector)
//		{
//			if (sv.command == VECTORCOMMENT)
//			{
//				efprintf(vout, x_("%s\n"), sv.parameters[0]);
//				continue;
//			}
//			efprintf(vout, x_("%s"), irsim_commandname(sv.command));
//			for(i=0; i<sv.parcount; i++)
//				efprintf(vout, x_(" %s"), sv.parameters[i]);
//			efprintf(vout, x_("\n"));
//		}
//	
//		xclose(vout);
//		System.out.println("Wrote %s"), truename);
//	}

//	/*
//	 * Routine to move down the hierarchy into instance "level" and show
//	 * signals at that level.
//	 */
//	void irsim_level_set(String level, Cell cell)
//	{
//		if (level != 0)
//		{
//			estrcat(irsim_level_prefix, level);
//			estrcat(irsim_level_prefix, x_("/"));
//		}
//	
//		// remember saved signals in this cell
//		sim_window_grabcachedsignalsoncell(cell);
//	
//		// ready to add: remove highlighting
//		ww.clearHighlighting();
//	
//		// setup signals for this cell
//		irsim_showcurrentlevel(cell);
//		irsim_UpdateWindow(Sched.irsim_cur_delta);
//		irsim_playvectors();
//	}

//	/*
//	 * Routine to move up one level of hierarchy in the display of signal names.
//	 */
//	void irsim_level_up(Cell cell)
//	{
//		INTBIG len, i;
//	
//		len = estrlen(irsim_level_prefix) - 2;
//		if (len > 0)
//		{
//			for(i=len; i>=0; i--)
//				if (irsim_level_prefix[i] == '/') break;
//			irsim_level_prefix[i+1] = 0;
//		}
//	
//		// remember saved signals in this cell
//		sim_window_grabcachedsignalsoncell(cell);
//	
//		// ready to add: remove highlighting
//		ww.clearHighlighting();
//	
//		// setup signals for this cell
//		irsim_showcurrentlevel(cell);
//		irsim_UpdateWindow(Sched.irsim_cur_delta);
//		irsim_playvectors();
//	}

//	void irsim_adddisplayedsignal(String sig)
//	{
//		INTBIG i;
//		INTSML inistate[1];
//		double initime[1];
//	
//		// find the signal
//		for(i=0; i<sim_irsimnetnumber; i++)
//		{
//			if ((sim_irsimnets[i].flags&IRSIMNETSHOWN) != 0) continue;
//			if ((sim_irsimnets[i].flags&IRSIMNETVALID) == 0) continue;
//			if (namesame(sig, sim_irsimnets[i].name) == 0) break;
//		}
//		if (i >= sim_irsimnetnumber) return;
//	
//		// ready to add: remove highlighting
//		ww.clearHighlighting();
//	
//		// create a new trace in the last slot
//		sim_irsimnets[i].signal = sim_window_newtrace(-1, sim_irsimnets[i].name, 0);
//		sim_window_addhighlighttrace(sim_irsimnets[i].signal);
//		initime[0] = 0.0;   inistate[0] = (LOGIC_X << 8) | GATE_STRENGTH;
//		sim_window_setnumframes(sim_window_getnumframes()+1);
//		sim_window_loaddigtrace(sim_irsimnets[i].signal, 1, initime, inistate);
//		
//		irsim_UpdateWindow(Sched.irsim_cur_delta);
//		sim_window_redraw();
//	}

}
