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
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Comparator;
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
		int       command;
		String [] parameters;
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
		TraceEnt   next;		/* doubly linked list of traces */
		TraceEnt   prev;
		String     name;		/* name stripped of path */
		Sim.Node   waveformdata;
		int        len;			/* length of name string */
		int        top, bot;	/* position of the trace */
		short      bdigit;		/* # of bits per digit for displaying */
		boolean    vector;		/* 1 if bit vector, 0 if node */
//		union {
		Sim.Node   nd;			/* what makes up this trace */
		Sim.Bits   vec;
//		} n;
		Cache    [] cache;

		TraceEnt(int size)
		{
			cache = new Cache[size];
			for(int i=0; i<size; i++)
				cache[i] = new Cache();
		}
	};
	
	
	static final int    DEF_STEPS         = 4;			/* default simulation steps per screen */
	static final double DEFIRSIMTIMERANGE = 10.0E-9f;	/* initial size of simulation window: 10ns */

	static int      irsim_numAdded;
	static Traces   irsim_traces = new Traces();
	static Times    irsim_tims = new Times();
	static long     irsim_lastStart;		/* last redisplay starting time */
	static WaveformWindow ww;
	
	static double [] irsim_tracetime;
	static short  [] irsim_tracestate;
	static int       irsim_tracetotal = 0;
	
	/*
	 * Routine to free all memory allocated in this module.
	 */
	static void irsim_freeanalyzermemory()
	{
//		SimVector *sv;
//		TraceEnt t;
//		INTBIG i;
//	
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
			for(TraceEnt t = irsim_traces.first; t != null; t = t.next)
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
			if (t.waveformdata.sig == null) continue;
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
    		String parameterFile = Simulation.getIRSIMParameterFile();
    		File pf = new File(parameterFile);
    		URL url;
    		if (pf != null && pf.exists())
    		{
    			url = TextUtils.makeURLToFile(parameterFile);
    		} else
    		{
        		url = LibFile.getLibFile(parameterFile);
    		}
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

    		// sort the signal names
			Collections.sort(Sim.Node.irsim_GetNodeList(), new NodesByName());

    		// convert the stimuli
    		Simulation.SimData sd = new Simulation.SimData();
    		sd.setSeparatorChar('/');
    		sd.setCell(cell);
    		for(Iterator it = Sim.Node.irsim_GetNodeList().iterator(); it.hasNext(); )
    		{
    			Sim.Node n = (Sim.Node)it.next();
    			if (n.nname.equalsIgnoreCase("vdd") || n.nname.equalsIgnoreCase("gnd")) continue;

    			// make a signal for it
				Simulation.SimDigitalSignal sig = new Simulation.SimDigitalSignal(null);
				n.sig = sig;
				int slashPos = n.nname.lastIndexOf('/');
				if (slashPos >= 0)
				{
					sig.setSignalName(n.nname.substring(slashPos+1));
					sig.setSignalContext(n.nname.substring(0, slashPos));
				} else
				{
					sig.setSignalName(n.nname);
				}
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
    	
    		// read signal values
    		irsim_UpdateWindow(Sched.irsim_cur_delta);
            return true;
        }
    }

	public static class NodesByName implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Sim.Node n1 = (Sim.Node)o1;
			Sim.Node n2 = (Sim.Node)o2;
			return n1.nname.compareToIgnoreCase(n2.nname);
		}
	}

	/**
	 * Main entry point to run an arbitrary IRSIM command.
	 */
	public static void doIRSIMCommand(String command)
	{
		if (command.equals("l") || command.equals("h") || command.equals("x"))
		{
			int veccmd = VECTORL;
			if (command.equals("h")) veccmd = VECTORH; else
				if (command.equals("x")) veccmd = VECTORX;
			List signals = ww.getHighlightedNetworkNames();
			String [] parameters = new String[1];
			for(Iterator it = signals.iterator(); it.hasNext(); )
			{
				Simulation.SimSignal sig = (Simulation.SimSignal)it.next();
				parameters[0] = sig.getFullName();
				irsim_newvector(veccmd, parameters, ww.getMainTimeCursor());
			}
			irsim_playvectors();
			return;
		}
		if (command.equals("clear"))
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
			return;
		}
		if (command.equals("save"))
		{
			irsim_savevectorfile();
			return;
		}
		if (command.equals("restore"))
		{
			irsim_loadvectorfile();
			return;
		}
	}

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
//			infstr = initinfstr();
//			formatinfstr(infstr, x_("! %s"), sim_window_gettracename(highsig));
//			RSim.irsim_issuecommand(returninfstr(infstr));
//			infstr = initinfstr();
//			formatinfstr(infstr, x_("? %s"), sim_window_gettracename(highsig));
//			RSim.irsim_issuecommand(returninfstr(infstr));
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
		RSim.irsim_issuecommand("back 0");
		RSim.irsim_issuecommand("flush");
	
		double curtime = 0;
		RSim.irsim_analyzerON = false;
		for(SimVector sv = irsim_firstvector; sv != null; sv = sv.nextsimvector)
		{
			if (sv.command == VECTORCOMMENT) continue;
			String infstr = irsim_commandname(sv.command);
			for(int i=0; i<sv.parameters.length; i++)
				infstr += " " + sv.parameters[i];
			RSim.irsim_issuecommand(infstr);
		}
		RSim.irsim_issuecommand("s");
		RSim.irsim_analyzerON = true;
		irsim_UpdateWindow(Sched.irsim_cur_delta);
	
		// update main cursor location if requested
		if (Simulation.isIRSIMAutoAdvance())
			ww.setMainTimeCursor(curtime + 10.0/1000000000.0);
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

	/*
	 * Routine to read simulation vectors from a file.
	 */
	static void irsim_loadvectorfile()
	{
        String fileName = OpenFile.chooseInputFile(FileType.IRSIMSTIM, null);
        if (fileName == null) return;

        URL url = TextUtils.makeURLToFile(fileName);
        try
        {
			URLConnection urlCon = url.openConnection();
			InputStreamReader is = new InputStreamReader(urlCon.getInputStream());
			LineNumberReader lineReader = new LineNumberReader(is);

			// remove all vectors
			while (irsim_firstvector != null)
			{
				SimVector sv = irsim_firstvector;
				irsim_firstvector = sv.nextsimvector;
			}
			irsim_lastvector = null;
			System.out.println("Reading " + fileName);
			for(;;)
			{
				String buf = lineReader.readLine();
				if (buf == null) break;
		
				// ignore comments
				if (buf.startsWith("|"))
				{
					String [] par = new String[1];
					par[0] = buf;
					irsim_newvector(VECTORCOMMENT, par, -1.0);
					continue;
				}
		
				// get the first keyword
				String [] targ = Sim.parse_line(buf, false);
		
				// ignore the "w" command
				if (targ[0].equals("w"))
				{
					String [] par = new String[1];
					par[0] = buf;
					irsim_newvector(VECTORCOMMENT, par, -1.0);
					continue;
				}
		
				// save the "print" command
				if (targ[0].equals("print"))
				{
					String [] par = new String[1];
					par[0] = buf;
					irsim_newvector(VECTORPRINT, par, -1.0);
					continue;
				}
		
				// handle level setting on signals
				if (targ[0].equals("l") || targ[0].equals("h") || targ[0].equals("x"))
				{
					int command = VECTORX;
					if (targ[0].equals("l")) command = VECTORL; else
						if (targ[0].equals("h")) command = VECTORH;
					String [] par = new String[1];
					for(int i=1; i<targ.length; i++)
					{
						par[0] = targ[i];
						irsim_newvector(command, par, -1.0);
					}
					continue;
				}
		
				// handle commands
				int command;
				if (targ[0].equals("clock")) command = VECTORCLOCK; else
				if (targ[0].equals("vector")) command = VECTORVECTOR; else
				if (targ[0].equals("c")) command = VECTORC; else
				if (targ[0].equals("assert")) command = VECTORASSERT; else
				if (targ[0].equals("stepsize")) command = VECTORSTEPSIZE; else
				if (targ[0].equals("s")) command = VECTORS; else
				if (targ[0].equals("!")) command = VECTOREXCL; else
				if (targ[0].equals("?")) command = VECTORQUESTION; else
				if (targ[0].equals("set")) command = VECTORSET; else
				{
					System.out.println("Unknown IRSIM command on line " + lineReader.getLineNumber() + ": " + buf);
					continue;
				}
				String [] justParams = new String[targ.length-1];
				for(int i=1; i<targ.length; i++) justParams[i-1] = targ[i];
				irsim_newvector(command, justParams, -1.0);
		
				// update the display if this is a "vector" command
				if (command == VECTORVECTOR)
				{
//					// make the bus
//					bussignals = (INTBIG *)emalloc((count-1) * SIZEOFINTBIG, el_tempcluster);
//					if (bussignals == 0) return;
//		
//					// if the bus name already exists, delete it
//					highsiglist = sim_window_findtrace(irsim_inputparameters[0], &highsigcount);
//					for(i=0; i<highsigcount; i++)
//						sim_window_killtrace(highsiglist[i]);
//		
//					j = 0;
//					for(i=1; i<count; i++)
//					{
//						highsiglist = sim_window_findtrace(irsim_inputparameters[i], &highsigcount);
//						if (highsigcount == 0)
//						{
//							System.out.println("Vector %s: cannot find signal '%s'"),
//								irsim_inputparameters[0], irsim_inputparameters[i]);
//						} else
//						{
//							bussignals[j++] = highsiglist[0];
//						}
//					}
//					if (j > 0)
//						(void)sim_window_makebus(j, bussignals, irsim_inputparameters[0]);
//					efree((CHAR *)bussignals);
				}
			}
			irsim_playvectors();
		
//			sim_window_gettimeextents(&min, &max);
//			ww.setDefaultTimeRange(min, max);
//			ww.setMainTimeCursor((max-min)/5.0*2.0+min);
//			ww.setExtensionTimeCursor((max-min)/5.0*3.0+min);
			irsim_UpdateWindow(Sched.irsim_cur_delta);

			lineReader.close();
		} catch (IOException e)
        {
        	System.out.println("Error reading " + fileName);
        	return;
        }
	}

	/*
	 * Routine to save simulation vectors to file "filename" (if zero, prompt for file).
	 */
	static void irsim_savevectorfile()
	{
        String fileName = OpenFile.chooseOutputFile(FileType.IRSIMSTIM, "IRSIM Vector file", Library.getCurrent().getName() + ".cmd");
        try
		{
        	PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
        	
    		for(SimVector sv = irsim_firstvector; sv != null; sv = sv.nextsimvector)
    		{
    			if (sv.command == VECTORCOMMENT)
    			{
    				printWriter.println(sv.parameters[0]);
    				continue;
    			}
    			printWriter.print(irsim_commandname(sv.command));
    			for(int i=0; i<sv.parameters.length; i++)
    				printWriter.print(" " + sv.parameters[i]);
    			printWriter.println();
    		}

    		printWriter.close();
    		System.out.println("Wrote " + fileName);
        } catch (IOException e)
		{
            System.out.println("Error writing " + fileName);
            return;
        }
	}

}
