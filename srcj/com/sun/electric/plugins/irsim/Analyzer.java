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
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.lib.LibFile;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.simulation.Engine;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.WaveformWindow;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * The Analyzer class is the top-level class for IRSIM simulation.
 * By creating an Analyzer object, all other simulation objects are defined.
 *
 * Analyzers have Sim objects in them.
 * Sim objects have Config and Eval objects in them.
 */
public class Analyzer extends Engine
{
	static final String irsim_version = "9.5j";

	/* the meaning of SIMVECTOR.command */
	private static final int VECTORCOMMENT    =  1;		/* a comment in the command file */
	private static final int VECTORL          =  2;		/* the "l" command (set signal low) */
	private static final int VECTORH          =  3;		/* the "h" command (set signal high) */
	private static final int VECTORX          =  4;		/* the "x" command (set signal undefined) */
	private static final int VECTORS          =  5;		/* the "s" command (advance time) */
	private static final int VECTORASSERT     =  6;		/* the "assert" command (test signal value) */
	private static final int VECTORCLOCK      =  7;		/* the "clock" command to declare stimuli */
	private static final int VECTORC          =  8;		/* the "c" command to run a clock cycle */
	private static final int VECTORVECTOR     =  9;		/* the "vector" command to group signals */
	private static final int VECTORSTEPSIZE   = 10;		/* the "stepsize" command to set time advance */
	private static final int VECTORPRINT      = 11;		/* the "print" command to display messages */
	private static final int VECTORSET        = 12;		/* the "set" command to change vector values */
	private static final int VECTOREXCL       = 13;		/* the "!" command to print gate info */
	private static final int VECTORQUESTION   = 14;		/* the "?" command to print source/drain info */
	private static final int VECTORTRACE      = 15;		/* the "t" command to trace */

	private static final int    DEF_STEPS         = 4;			/* default simulation steps per screen */
	private static final double DEFIRSIMTIMERANGE = 10.0E-9f;	/* initial size of simulation window: 10ns */

	private static class SimVector
	{
		int       command;
		String [] parameters;
		SimVector nextsimvector;
	};

	private static class Times
	{
		long    first;
		long    last;
		long    start;
		long    steps;
		long    end;
	};

	private static class TraceEnt
	{
		String      name;			/* name stripped of path */
		Sim.Node    waveformdata;
		Sim.Node    nd;				/* what makes up this trace */
		Sim.HistEnt wind;			/* window start */
		Sim.HistEnt cursor;			/* cursor value */
	};

	public static class Bits
	{
		Bits    next;		/* next bit vector in chain */
		String   name;		/* name of this vector of bits */
		int     traced;		/* <>0 if this vector is being traced */
		int     nbits;		/* number of bits in this vector */
		Sim.Node    nodes[];	/* pointers to the bits (nodes) */
	};

	private SimVector irsim_firstvector = null;
	private SimVector irsim_lastvector = null;
	private long      irsim_stepsize = 50000;

	private List      irsim_traces;
	private Times     irsim_tims = new Times();
	private long      irsim_lastStart;		/* last redisplay starting time */

	private double [] irsim_tracetime;
	private short  [] irsim_tracestate;
	private int       irsim_tracetotal = 0;

	// the simulation engine
	private Sim       theSim;
	private WaveformWindow ww;

	/************************** ELECTRIC INTERFACE **************************/

	Analyzer()
	{
		theSim = new Sim(this);
	}

	/**
	 * Main entry point to start simulating a cell.
	 * @param cell the cell to simulate.
	 * @param fileName the file with the input deck (null to generate one)
	 */
	public static void simulateCell(Cell cell, VarContext context, String fileName)
	{
    	Analyzer theAnalyzer = new Analyzer();
		new StartIRSIM(cell, context, fileName, theAnalyzer);
	}

	private static class StartIRSIM extends Job
    {
        private Cell cell;
        private VarContext context;
        private String fileName;
        private Analyzer analyzer;

        public StartIRSIM(Cell cell, VarContext context, String fileName, Analyzer analyzer)
        {
            super("Simulate cell", User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.cell = cell;
            this.context = context;
            this.fileName = fileName;
            this.analyzer = analyzer;
            startJob();
        }

        public boolean doIt()
        {
        	Sim sim = analyzer.theSim;

        	analyzer.irsim_firstvector = null;
        	analyzer.irsim_traces = new ArrayList();

    		/* now initialize the simulator */
    		System.out.println("IRSIM, version " + irsim_version);

    		String fileToUse = fileName;
    		if (fileName == null)
    		{
    			fileName = "Electric.XXXXXX";
    			Output.writeCell(cell, context, fileName, FileType.IRSIM);
    		}

    		URL fileURL = TextUtils.makeURLToFile(fileName);

    		// read the configuration file
    		String parameterFile = Simulation.getIRSIMParameterFile().trim();
    		if (parameterFile.length() > 0)
    		{
	    		File pf = new File(parameterFile);
	    		URL url;
	    		if (pf != null && pf.exists())
	    		{
	    			url = TextUtils.makeURLToFile(parameterFile);
	    		} else
	    		{
	        		url = LibFile.getLibFile(parameterFile);
	    		}
	    		if (url == null)
	    		{
	    			System.out.println("Cannot find parameter file: " + parameterFile);
	    		} else
	    		{
	    			if (sim.getConfig().irsim_config(url)) return true;
	    		}
    		}

    		// Read network (.sim file)
    		analyzer.irsim_init_rsim();
    		if (sim.irsim_rd_network(fileURL)) return true;

    		// remove the temporary network file
    		if (fileToUse == null)
    		{
	    		File f = new File(fileName);
	    		if (f != null) f.delete();
    		}

    		sim.irsim_ConnectNetwork();	// connect all txtors to corresponding nodes
    		sim.getModel().irsim_init_event();

    		// sort the signal names
			Collections.sort(sim.irsim_GetNodeList(), new NodesByName());

    		// convert the stimuli
			Stimuli sd = new Stimuli();
    		sd.setSeparatorChar('/');
    		sd.setCell(cell);
    		for(Iterator it = sim.irsim_GetNodeList().iterator(); it.hasNext(); )
    		{
    			Sim.Node n = (Sim.Node)it.next();
    			if (n.nname.equalsIgnoreCase("vdd") || n.nname.equalsIgnoreCase("gnd")) continue;

    			// make a signal for it
				Stimuli.DigitalSignal sig = new Stimuli.DigitalSignal(null);
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
			analyzer.ww = sd.getWaveformWindow();
			analyzer.ww.setSimEngine(analyzer);

			analyzer.ww.clearHighlighting();

			analyzer.ww.setDefaultTimeRange(0.0, DEFIRSIMTIMERANGE);
			analyzer.ww.setMainTimeCursor(DEFIRSIMTIMERANGE/5.0*2.0);
			analyzer.ww.setExtensionTimeCursor(DEFIRSIMTIMERANGE/5.0*3.0);

    		// tell the simulator to watch all signals
    		if (!analyzer.irsim_analyzerON)
    		{
    			analyzer.irsim_InitTimes(0, 50000, sim.irsim_cur_delta);
    		}

    		for(Iterator it = sim.irsim_GetNodeList().iterator(); it.hasNext(); )
    		{
    			Sim.Node n = (Sim.Node)it.next();
    			analyzer.irsim_AddNode(n);
    		}
    		analyzer.irsim_UpdateWindow(0);
    		analyzer.irsim_DisplayTraces(analyzer.irsim_analyzerON);
    		analyzer.irsim_analyzerON = true;

    		// read signal values
    		analyzer.irsim_UpdateWindow(sim.irsim_cur_delta);
            return true;
        }
    }

	private static class NodesByName implements Comparator
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
	public void doCommand(String command)
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
				Stimuli.Signal sig = (Stimuli.Signal)it.next();
				parameters[0] = sig.getFullName().replace('.', '/');
				irsim_newvector(veccmd, parameters, ww.getMainTimeCursor());
			}
			if (Simulation.isIRSIMResimulateEach())
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
			Stimuli.Signal sig = (Stimuli.Signal)signals.get(0);
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
			if (Simulation.isIRSIMResimulateEach())
				irsim_playvectors();
			return;
		}
		if (command.equals("clearAll"))
		{
			if (Simulation.isIRSIMResimulateEach())
				irsim_clearallvectors();
			return;
		}
		if (command.equals("update"))
		{
			irsim_playvectors();
			return;
		}
		if (command.equals("info"))
		{
			List signals = ww.getHighlightedNetworkNames();
			for(Iterator it = signals.iterator(); it.hasNext(); )
			{
				Stimuli.Signal sig = (Stimuli.Signal)it.next();
				irsim_issuecommand("! " + sig.getFullName().replace('.', '/'));
				irsim_issuecommand("? " + sig.getFullName().replace('.', '/'));
			}
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

	/************************** LOW-LEVEL ANALYZER **************************/

	private void irsim_AddNode(Sim.Node nd)
	{
		while ((nd.nflags & Sim.ALIAS) != 0)
			nd = nd.nlink;

		if ((nd.nflags & Sim.MERGED) != 0)
		{
			System.out.println("can't watch node " + nd.nname);
			return;
		}
		TraceEnt t = new TraceEnt();
		t.name = nd.nname;
		t.nd = nd;
		t.wind = t.cursor = nd.head;
		irsim_traces.add(t);
	}

	private void irsim_DisplayTraces(boolean isMapped)
	{
		if (!isMapped)				// only the first time
		{
			irsim_FlushTraceCache();
		} else
		{
			irsim_DrawTraces(irsim_tims.start, irsim_tims.end);
		}
	}

	private void irsim_RestartAnalyzer(long first_time, long last_time, int same_hist)
	{
		for(Iterator it = irsim_traces.iterator(); it.hasNext(); )
		{
			TraceEnt t = (TraceEnt)it.next();
			t.wind = t.cursor = t.nd.head;
		}

		// should set "last_time" to the width of the screen
		irsim_InitTimes(first_time, irsim_tims.steps / DEF_STEPS, last_time);
		if (same_hist != 0)
			irsim_UpdateTraceCache(0);
		else
			irsim_FlushTraceCache();
	}

	private void UpdateWinRemove()
	{
		irsim_DrawTraces(irsim_tims.start, irsim_tims.end);
	}

	/**
	 * Initialize the display times so that when first called the last time is
	 * shown on the screen.  Default width is DEF_STEPS (simulation) steps.
	 */
	private void irsim_InitTimes(long firstT, long stepsize, long lastT)
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

	/**
	 * Update the cache (begining of window and cursor) for traces that just
	 * became visible (or were just added).
	 */
	private void irsim_UpdateTraceCache(int first_trace)
	{
		long startT = irsim_tims.start;
		long cursT = irsim_tims.first;
		int n = 0;
		for(Iterator it = irsim_traces.iterator(); it.hasNext(); )
		{
			TraceEnt t = (TraceEnt)it.next();
			if (n < first_trace)
				continue;

			Sim.HistEnt p = t.wind;
			Sim.HistEnt h = t.cursor;
			Sim.HistEnt nexth = Sim.NEXTH(h);
			if (h.htime > cursT || nexth.htime <= cursT)
			{
				if (p.htime <= cursT)
					t.cursor = p;
				else
					t.cursor = t.nd.head;
			}

			if (startT <= p.htime)
				p = t.nd.head;

			h = Sim.NEXTH(p);
			while (h.htime < startT)
			{
				p = h;
				h = Sim.NEXTH(h);
			}
			t.wind = p;

			p = t.cursor;
			h = Sim.NEXTH(p);
			while (h.htime <= cursT)
			{
				p = h;
				h = Sim.NEXTH(h);
			}
			t.cursor = p;
		}
	}

	private void irsim_FlushTraceCache()
	{
		irsim_lastStart = theSim.irsim_max_time;
	}

	/**
	 * Draw the traces horizontally from time1 to time2.
	 */
	private void irsim_DrawTraces(long t1, long t2)
	{
		if (irsim_tims.start != irsim_lastStart)		// Update history cache
		{
			long startT = irsim_tims.start;
			boolean begin = (startT < irsim_lastStart);
			for(Iterator it = irsim_traces.iterator(); it.hasNext(); )
			{
				TraceEnt t = (TraceEnt)it.next();
				Sim.HistEnt p = begin ? t.nd.head : t.wind;
				Sim.HistEnt h = Sim.NEXTH(p);
				while (h.htime < startT)
				{
					p = h;
					h = Sim.NEXTH(h);
				}
				t.wind = p;
			}
			irsim_lastStart = irsim_tims.start;
		}

		for(Iterator it = irsim_traces.iterator(); it.hasNext(); )
		{
			TraceEnt t = (TraceEnt)it.next();
			if (t.waveformdata.sig == null) continue;

			if (t1 >= irsim_tims.last) continue;
			Sim.HistEnt h = t.wind;
			int count = 0;
			long curt = 0;
			long endt = t2;
			while (curt < endt)
			{
				int val = h.val;
				while (h.htime < endt && h.val == val)
					h = Sim.NEXTH(h);

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
				switch (val)
				{
					case Sim.LOW:
						irsim_tracestate[count] = Stimuli.LOGIC_LOW | Stimuli.GATE_STRENGTH;
						break;
					case Sim.HIGH:
						irsim_tracestate[count] = Stimuli.LOGIC_HIGH | Stimuli.GATE_STRENGTH;
						break;
					default:
						irsim_tracestate[count] = Stimuli.LOGIC_X | Stimuli.GATE_STRENGTH;
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
		ww.repaint();
	}

	private void UpdateTraces(long start, long end)
	{
		irsim_DrawTraces(start, end);
	}

	/**
	 * Update the trace window so that endT is shown.  If the update fits in the
	 * window, simply draw the missing parts.  Otherwise scroll the traces,
	 * centered around endT.
	 */
	public void irsim_UpdateWindow(long endT)
	{
		long lastT = irsim_tims.last;
		irsim_tims.last = endT;

		for(Iterator it = irsim_traces.iterator(); it.hasNext(); )
		{
			TraceEnt t = (TraceEnt)it.next();
    		for(Iterator nIt = theSim.irsim_GetNodeList().iterator(); nIt.hasNext(); )
    		{
    			Sim.Node n = (Sim.Node)nIt.next();
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

	/************************** SIMULATION VECTORS **************************/

	/**
	 * Method to create a new simulation vector at time "time", on signal "sig",
	 * with state "state".  The vector is inserted into the play list in the proper
	 * order.
	 */
	private void irsim_newvector(int command, String [] params, double inserttime)
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
							sv.parameters[0] = TextUtils.formatDouble((inserttime-curtime) * 1000000000.0);

							// create second step to advance after this signal
							SimVector aftersv = new SimVector();
							aftersv.command = VECTORS;
							aftersv.parameters = new String[1];
							aftersv.parameters[0] = TextUtils.formatDouble((curtime+stepsize-inserttime) * 1000000000.0);
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
					aftersv.parameters[0] = TextUtils.formatDouble(thisstep);
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


	/**
	 * Method to examine the test vectors and determine the ending simulation time.
	 */
	private double irsim_endtime()
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
		return curtime;
	}

	/**
	 * Method to play the simulation vectors into the simulator.
	 */
	private void irsim_playvectors()
	{
		irsim_issuecommand("back 0");
//		irsim_issuecommand("flush");

		double curtime = 0;
		irsim_analyzerON = false;
		for(SimVector sv = irsim_firstvector; sv != null; sv = sv.nextsimvector)
		{
			if (sv.command == VECTORCOMMENT) continue;
			String infstr = irsim_commandname(sv.command);
			for(int i=0; i<sv.parameters.length; i++)
				infstr += " " + sv.parameters[i];
			irsim_issuecommand(infstr);
		}
		irsim_issuecommand("s");
		irsim_analyzerON = true;
		irsim_UpdateWindow(theSim.irsim_cur_delta);

		// update main cursor location if requested
		if (Simulation.isIRSIMAutoAdvance())
			ww.setMainTimeCursor(curtime + 10.0/1000000000.0);
	}

	private String irsim_commandname(int command)
	{
		switch (command)
		{
			case VECTORL:        return "l";
			case VECTORH:        return "h";
			case VECTORX:        return "x";
			case VECTORS:        return "s";
			case VECTORASSERT:   return "assert";
			case VECTORCLOCK:    return "clock";
			case VECTORC:        return "c";
			case VECTORVECTOR:   return "vector";
			case VECTORSTEPSIZE: return "stepsize";
			case VECTORPRINT:    return "print";
			case VECTORSET:      return "set";
			case VECTOREXCL:     return "!";
			case VECTORQUESTION: return "?";
			case VECTORTRACE:    return "t";
		}
		return "";
	}

	/**
	 * Method to clear all simulation vectors.
	 */
	private void irsim_clearallvectors()
	{
		irsim_firstvector = null;
		irsim_lastvector = null;
		irsim_playvectors();
	}

	/**
	 * Method to read simulation vectors from a file.
	 */
	private void irsim_loadvectorfile()
	{
        String fileName = OpenFile.chooseInputFile(FileType.IRSIMVECTOR, "IRSIM Vector file");
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
				if (targ == null || targ.length <= 0) continue;

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
				if (targ[0].equals("t")) command = VECTORTRACE; else		
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
			irsim_UpdateWindow(theSim.irsim_cur_delta);

			lineReader.close();
		} catch (IOException e)
        {
        	System.out.println("Error reading " + fileName);
        	return;
        }
	}

	/**
	 * Method to save simulation vectors to file "filename" (if zero, prompt for file).
	 */
	private void irsim_savevectorfile()
	{
        String fileName = OpenFile.chooseOutputFile(FileType.IRSIMVECTOR, "IRSIM Vector file", Library.getCurrent().getName() + ".cmd");
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

	/******************************************** RSIM *****************************************/

	public static class AssertWhen
	{
		Sim.Node   node; /* which node we will check */
		char	   val;  /* what value has the node */
		AssertWhen nxt;
	};


	/* front end for mos simulator -- Chris Terman (6/84) */
	/* sunbstantial changes: Arturo Salz (88) */

	private static final int	MAXARGS      = 100;	/* maximum number of command-line arguments */
	private static final int	MAXCOL        = 80;	/* maximum width of print line */

	private static class Sequence
	{
		Sequence  next;			/* next vector in linked list */
		int       which;		/* 0 => node; 1 => vector */
	//	union
	//	{
			Sim.Node  n;
			Bits  b;
	//	} ptr;					/* pointer to node/vector */
		int     vsize;			/* size of each value */
		int     nvalues;		/* number of values specified */
		char    []values;		/* array of values */
	};

	private Bits blist = null;				/* list of vectors */

	private Sequence slist = null;				/* list of sequences */
	private int		maxsequence = 0;			/* longest sequence defined */

	private Sequence xclock = null;				/* vectors which make up clock */
	private int		maxclock = 0;				/* longest clock sequence defined */

	private int		column = 0;					/* current output column */

	private final String	potchars = "luxh.";			/* set of potential characters */
	public boolean  irsim_analyzerON = false;	/* set when analyzer is running */

	public List  irsim_hinputs = new ArrayList();	/* list of nodes to be driven high */
	public List  irsim_linputs = new ArrayList();	/* list of nodes to be driven low */
	public List  irsim_uinputs = new ArrayList();	/* list of nodes to be driven X */
	public List  irsim_xinputs = new ArrayList();	/* list of nodes to be removed from input */

	private List  [] irsim_listTbl = new List[8];

	private void irsim_init_rsim()
	{
		blist = null;
		slist = null;
		maxsequence = 0;
		xclock = null;
		maxclock = 0;
		column = 0;
		irsim_analyzerON = false;
		for(int i = 0; i < 8; i++) irsim_listTbl[i] = null;
		irsim_listTbl[Sim.INPUT_NUM(Sim.H_INPUT)] = irsim_hinputs;
		irsim_listTbl[Sim.INPUT_NUM(Sim.L_INPUT)] = irsim_linputs;
		irsim_listTbl[Sim.INPUT_NUM(Sim.U_INPUT)] = irsim_uinputs;
		irsim_listTbl[Sim.INPUT_NUM(Sim.X_INPUT)] = irsim_xinputs;
	}

	private void irsim_issuecommand(String command)
	{
		if (Simulation.isIRSIMShowsCommands()) System.out.println("> " + command);
		String [] strings = Sim.parse_line(command, true);
		if (strings.length > 0) exec_cmd(strings);
	}

	/**
	 * Execute a builtin command or read commands from a '.cmd' file.
	 */
	private void exec_cmd(String [] args)
	{
		// search command table, dispatch to handler, if any
		String cmdName = args[0];
		if (cmdName.equals("!")) { quest(args);   return; }
		if (cmdName.equals("?")) { quest(args);   return; }
		if (cmdName.equals("activity")) { doactivity(args);   return; }
		if (cmdName.equals("alias")) { doprintAlias(args);   return; }
		if (cmdName.equals("assert")) { doAssert(args);   return; }
		if (cmdName.equals("assertWhen")) { doAssertWhen(args);   return; }
		if (cmdName.equals("back")) { back_time(args);   return; }
		if (cmdName.equals("c")) { doclock(args);   return; }
		if (cmdName.equals("changes")) { dochanges(args);   return; }
		if (cmdName.equals("clock")) { setclock(args);   return; }
		if (cmdName.equals("decay")) { setdecay(args);   return; }
		if (cmdName.equals("h")) { setvalue(args);   return; }
		if (cmdName.equals("l")) { setvalue(args);   return; }
		if (cmdName.equals("u")) { setvalue(args);   return; }
		if (cmdName.equals("x")) { setvalue(args);   return; }
		if (cmdName.equals("inputs")) { inputs(args);   return; }
		if (cmdName.equals("p")) { dophase(args);   return; }
		if (cmdName.equals("path")) { dopath(args);   return; }
		if (cmdName.equals("print")) { domsg(args);   return; }
		if (cmdName.equals("printx")) { doprintX(args);   return; }
		if (cmdName.equals("R")) { runseq(args);   return; }
		if (cmdName.equals("s")) { dostep(args);   return; }
		if (cmdName.equals("set")) { setvector(args);   return; }
		if (cmdName.equals("setpath")) { do_stats(args);   return; }
		if (cmdName.equals("stepsize")) { setstep(args);   return; }
		if (cmdName.equals("stop")) { setstop(args);   return; }
		if (cmdName.equals("t")) { settrace(args);   return; }
		if (cmdName.equals("tcap")) { print_tcap(args);   return; }
		if (cmdName.equals("unitdelay")) { setunit(args);   return; }
		if (cmdName.equals("until")) { doUntil(args);   return; }

		System.out.println("unrecognized command: " + cmdName);
	}


	static final int SETIN_CALL      = 1;
	static final int SETTRACE_CALL   = 2;
	static final int SETSTOP_CALL    = 3;
	static final int QUEST_CALL      = 4;
	static final int PATH_CALL       = 5;
	static final int FINDONE_CALL    = 6;
	static final int ASSERTWHEN_CALL = 7;

	/**
	 * Apply given function to each argument on the command line.
	 * Arguments are checked first to ensure they are the name of a node or
	 * vector; wild-card patterns are allowed as names.
	 * Either 'fun' or 'vfunc' is called with the node/vector as 1st argument:
	 *	'fun' is called if name refers to a node.
	 *	'vfun' is called if name refers to a vector.  If 'vfun' is null
	 *	then 'fun' is called on each node of the vector.
	 * The parameter (2nd argument) passed to the specified function will be:
	 *	If 'arg' is the special constant '+' then
	 *	    if the name is preceded by a '-' pass a pointer to '-'
	 *	    otherwise pass a pointer to '+'.
	 *	else 'arg' is passed as is.
	 */
	private Object apply(int fun, int vfun, String[] args, int applyStart, int applyEnd)
	{
		Find1Arg f = null;
		if (fun == FINDONE_CALL)
		{
			f = new Find1Arg();
			f.num = 0;
			f.vec = null;
			f.node = null;
		}
		for(int i = applyStart; i < applyEnd; i += 1)
		{
			String p = args[i];
			String flag = args[0];
			if (args[0].equals("+"))
			{
				if (p.startsWith("-"))
				{
					flag = "-";
					p = p.substring(1);
				}
				else
					flag = "+";
			}

			int found = 0;
//			if (wildCard[i])
//			{
//				for(Bits b = blist; b != null; b = b.next)
//					if (irsim_str_match(p, b.name))
//				{
//					if (vfun != null)
//						((int(*)(Bits,CHAR*))(*vfun))(b, flag);
//					else
//						for(j = 0; j < b.nbits; j += 1)
//							((int(*)(Sim.Node,CHAR*))(*fun))(b.nodes[j], flag);
//					found = 1;
//				}
//				found += irsim_match_net(p, (int(*)(Sim.Node, CHAR*))fun, flag);
//			} else
			{
				Sim.Node n = theSim.irsim_find(p);

				if (n != null)
				{
					switch (fun)
					{
						case SETIN_CALL:
							found += irsim_setin(n, flag);
							break;
						case SETTRACE_CALL:
							found += xtrace(n, flag);
							break;
						case SETSTOP_CALL:
							found += nstop(n, flag);
							break;
						case QUEST_CALL:
							found += irsim_info(n, flag);
							break;
						case PATH_CALL:
							found += do_cpath(n);
							break;
						case FINDONE_CALL:
							f.node = n;
							f.num++;
							found = 1;
							break;
						case ASSERTWHEN_CALL:
							setupAssertWhen(n, flag);
							break;
					}
				} else
				{
					for(Bits b = blist; b != null; b = b.next)
						if (p.equalsIgnoreCase(b.name))
					{
						switch (fun)
						{
							case SETTRACE_CALL:
								vtrace(b, flag);
								break;
							case SETSTOP_CALL:
								found += vstop(b, flag);
								break;
							case FINDONE_CALL:
								f.vec = b;
								f.num++;
								break;
						}
						found = 1;
						break;
					}
				}
			}
			if (found == 0)
				System.out.println(p + ": No such node or vector");
		}
		return f;
	}

	/**
	 * set/clear input status of node and add/remove it to/from corresponding list.
	 */
	private int irsim_setin(Sim.Node n, String which)
	{
		while((n.nflags & Sim.ALIAS) != 0)
			n = n.nlink;

		char wChar = which.charAt(0);
		if ((n.nflags & (Sim.POWER_RAIL | Sim.MERGED)) != 0)	// Gnd, Vdd, or merged node
		{
			String pots = "lxuh";
			if ((n.nflags & Sim.MERGED) != 0 || pots.charAt(n.npot) != wChar)
				System.out.println("Can't drive `" + n.nname + "' to `" + wChar + "'");
		} else
		{
			List list = irsim_listTbl[Sim.INPUT_NUM((int)n.nflags)];

			switch(wChar)
			{
				case 'h':
					if (list != null && list != irsim_hinputs)
					{
						n.nflags = n.nflags & ~Sim.INPUT_MASK;
						irsim_idelete(n, list);
					}
					if (! (list == irsim_hinputs || WASINP(n, Sim.HIGH)))
					{
						n.nflags = (n.nflags & ~Sim.INPUT_MASK) | Sim.H_INPUT;
						irsim_iinsert(n, irsim_hinputs);
					}
					break;

				case 'l':
					if (list != null && list != irsim_linputs)
					{
						n.nflags = n.nflags & ~Sim.INPUT_MASK;
						irsim_idelete(n, list);
					}
					if (! (list == irsim_linputs || WASINP(n, Sim.LOW)))
					{
						n.nflags = (n.nflags & ~Sim.INPUT_MASK) | Sim.L_INPUT;
						irsim_iinsert(n, irsim_linputs);
					}
					break;

				case 'u':
					if (list != null && list != irsim_uinputs)
					{
						n.nflags = n.nflags & ~Sim.INPUT_MASK;
						irsim_idelete(n, list);
					}
					if (! (list == irsim_uinputs || WASINP(n, Sim.X)))
					{
						n.nflags = (n.nflags & ~Sim.INPUT_MASK) | Sim.U_INPUT;
						irsim_iinsert(n, irsim_uinputs);
					}
					break;

				case 'x':
					if (list == irsim_xinputs)
						break;
					if (list != null)
					{
						n.nflags = n.nflags & ~Sim.INPUT_MASK;
						irsim_idelete(n, list);
					}
					if ((n.nflags & Sim.INPUT) != 0)
					{
						n.nflags = (n.nflags & ~Sim.INPUT_MASK) | Sim.X_INPUT;
						irsim_iinsert(n, irsim_xinputs);
					}
					break;

				default:
					return 0;
			}
		}
		return 1;
	}

	private boolean WASINP(Sim.Node n, int p)
	{
		return (n.nflags & Sim.INPUT) != 0 && n.npot == p;
	}

	private void irsim_idelete(Sim.Node n, List list)
	{
		list.remove(n);
	}


	private void irsim_iinsert(Sim.Node n, List list)
	{
		list.add(n);
	}

	private int irsim_info(Sim.Node n, String which)
	{
		if (n == null)
			return 0;

		String name = n.nname;
		while((n.nflags & Sim.ALIAS) != 0)
			n = n.nlink;

		if ((n.nflags & Sim.MERGED) != 0)
		{
			System.out.println(name + " => node is inside a transistor stack");
			return 1;
		}

		String infstr = "";
		infstr += pvalue(name, n);
		if ((n.nflags & Sim.INPUT) != 0)
			infstr += "[NOTE: node is an input] ";
		infstr += "(vl=" + n.vlow + " vh=" + n.vhigh + ") ";
		if ((n.nflags & Sim.USERDELAY) != 0)
			infstr += "(tplh=" + n.tplh + ", tphl=" + n.tphl + ") ";
		infstr += "(" + n.ncap + " pf) ";
		System.out.println(infstr);

		infstr = "";
		if (which.startsWith("?"))
		{
			infstr += "is computed from:";
			for(Iterator it = n.ntermList.iterator(); it.hasNext(); )
			{
				Sim.Trans t = (Sim.Trans)it.next();
				infstr += "  ";

				String drive = null;
				Sim.Node rail = (t.drain.nflags & Sim.POWER_RAIL) != 0 ? t.drain : t.source;
				if (Sim.BASETYPE(t.ttype) == Sim.NCHAN && rail == theSim.irsim_GND_node)
					drive = "pulled down by ";
				else if (Sim.BASETYPE(t.ttype) == Sim.PCHAN && rail == theSim.irsim_VDD_node)
					drive = "pulled up by ";
				else if (Sim.BASETYPE(t.ttype) == Sim.DEP && rail == theSim.irsim_VDD_node &&
					Sim.other_node(t, rail) == t.gate)
						drive = "pullup ";
				else
					infstr += ptrans(t);

				if (drive != null)
				{
					infstr += drive;
					infstr += pgvalue(t);
					infstr += pr_t_res(t.r);
				}
			}
		} else
		{
			infstr += "affects:";
			for(Iterator it = n.ngateList.iterator(); it.hasNext(); )
			{
				Sim.Trans t = (Sim.Trans)it.next();
				infstr += ptrans(t);
			}
		}
		System.out.println(infstr);

		if (n.events != null)
		{
			System.out.println("Pending events:");
			for(Sim.Event e = n.events; e != null; e = e.nlink)
				System.out.println("   transition to " + Sim.irsim_vchars.charAt(e.eval) + " at " + Sim.d2ns(e.ntime)+ "ns");
		}

		return 1;
	}

	private String pvalue(String node_name, Sim.Node node)
	{
		char pot = 0;
		switch (node.npot)
		{
			case 0: pot = '0';   break;
			case 1: pot = 'X';   break;
			case 2: pot = 'X';   break;
			case 3: pot = '1';   break;
		}
	    return node_name + "=" + pot + " ";
	}

//	static final String [] states = { "OFF", "ON", "UKNOWN", "WEAK" };

	private String pgvalue(Sim.Trans t)
	{
		String infstr = "";
		if ((t.ttype & Sim.GATELIST) != 0)
		{
			infstr += "(";
			for(t = (Sim.Trans) t.gate; t != null; t = t.getSTrans())
			{
				Sim.Node n = (Sim.Node)t.gate;
				infstr += pvalue(n.nname, n);
			}

			infstr += ") ";
		} else
		{
			Sim.Node n = (Sim.Node)t.gate;
			infstr += pvalue(n.nname, n);
		}
		return infstr;
	}

	private String pr_one_res(double r)
	{
		String ret = TextUtils.formatDouble(r);
		if (r < 1e-9 || r > 100e9)
			return ret;

		int e = 3;
		if (r >= 1000.0)
			do { e++; r *= 0.001; } while(r >= 1000.0);
		else if (r < 1 && r > 0)
			do { e--; r *= 1000; } while(r < 1.0);
		switch (e)
		{
			case 0: ret += "n";   break;
			case 1: ret += "u";   break;
			case 2: ret += "m";   break;
			case 4: ret += "K";   break;
			case 5: ret += "M";   break;
			case 6: ret += "G";   break;
		}
		return ret;
	}

	private String pr_t_res(Sim.Resists r)
	{
		String v1 = pr_one_res(r.rstatic);
		String v2 = pr_one_res(r.dynres[Sim.R_HIGH]);
		String v3 = pr_one_res(r.dynres[Sim.R_LOW]);
		return "[" + v1 + ", " + v2 + ", " + v3 + "]";
	}

	private String ptrans(Sim.Trans t)
	{
		String infstr = Sim.irsim_ttype[Sim.BASETYPE(t.ttype)] + " ";
		if (Sim.BASETYPE(t.ttype) != Sim.RESIST)
			infstr += pgvalue(t);

		infstr += pvalue(t.source.nname, t.source);
		infstr += pvalue(t.drain.nname, t.drain);
		infstr += pr_t_res(t.r);
		return infstr;
	}

//	/* visit each node in network, calling function passed as arg with any node
//	 * whose name matches pattern
//	 */
//	private int irsim_match_net(CHAR *pattern, int (*fun)(Sim.Node, CHAR*), CHAR *arg)
//	{
//		int   index;
//		Sim.Node  n;
//		int            total = 0;
//
//		for(index = 0; index < HASHSIZE; index++)
//			for(n = hash[index]; n; n = n.hnext)
//				if (irsim_str_match(pattern, n.nname))
//					total += (*fun)(n, arg);
//
//		return total;
//	}

	/**
	 * compare pattern with string, case doesn't matter.  "*" wildcard accepted
	 */
	private boolean irsim_str_match(String pStr, String sStr)
	{
		int p = 0, s = 0;
		for(;;)
		{
			if (getCh(pStr, p) == '*')
			{
				// skip past multiple wildcards
				do
					p++;
				while(getCh(pStr, p) == '*');

				// if pattern ends with wild card, automatic match
				if (p >= pStr.length())
					return true;

				/* *p now points to first non-wildcard character, find matching
				 * character in string, then recursively match remaining pattern.
				 * if recursive match fails, assume current '*' matches more...
				 */
				while(s < sStr.length())
				{
					while(getCh(sStr, s) != getCh(pStr, p))
					{
						s++;
						if (s >= sStr.length()) return false;
					}
					s++;
					if (irsim_str_match(pStr.substring(p+1), sStr.substring(s)))
						return true;
				}

				// couldn't find matching character after '*', no match
				return false;
			}
			else if (p >= pStr.length())
				return s >= sStr.length();
			else if (getCh(pStr, p++) != getCh(sStr, s++))
				break;
		}
		return false;
	}

	private int getCh(String s, int index)
	{
		if (index >= s.length()) return 0;
		return Character.toLowerCase(s.charAt(index));
	}

	/**
	 * map a character into one of the potentials that a node can be set/compared
	 */
	private int ch2pot(char ch)
	{
		String s = "0ux1lUXhLUXH";
		for(int i = 0; i < s.length(); i++)
			if (s.charAt(i) == ch)
				return i & (Sim.N_POTS - 1);

		System.out.println(ch + ": unknown node value");
		return Sim.N_POTS;
	}

	private Sim.Node UnAlias(Sim.Node n)
	{
		while ((n.nflags & Sim.ALIAS) != 0) n = n.nlink;
		return n;
	}

	/**
	 * Set value of a node/vector to the requested value (hlux).
	 */
	private void setvalue(String [] args)
	{
		apply(SETIN_CALL, 0, args, 1, args.length);
	}

	/**
	 * display bit vector.
	 */
	private void dvec(Bits b)
	{
		int i = b.name.length() + 2 + b.nbits;
		if (column + i >= MAXCOL)
		{
			column = 0;
		}
		column += i;
		String bits = "";
		for(i = 0; i < b.nbits; i++)
			bits += Sim.irsim_vchars.charAt(b.nodes[i].npot);

		System.out.println(b.name + "=" + bits + " ");
	}

	/**
	 * display node/vector values in display list
	 */
	private void pnwatchlist()
	{
		theSim.getModel().printPendingEvents();
	}

	/**
	 * set/clear trace bit in node
	 */
	private int xtrace(Sim.Node n, String flag)
	{
		n = UnAlias(n);

		if ((n.nflags & Sim.MERGED) != 0)
		{
			System.out.println("can't trace " + n.nname);
			return 1;
		}

		if (flag.startsWith("+"))
			n.nflags |= Sim.WATCHED;
		else if ((n.nflags & Sim.WATCHED) != 0)
		{
			System.out.println(n.nname + " was watched; not any more");
			n.nflags &= ~Sim.WATCHED;
		}

		return 1;
	}

	/**
	 * set/clear trace bit in vector
	 */
	private void vtrace(Bits b, String flag)
	{
		if (flag.startsWith("+"))
			b.traced |= Sim.WATCHVECTOR;
		else
		{
			for(int i = 0; i < b.nbits; i += 1)
				b.nodes[i].nflags &= ~Sim.WATCHVECTOR;
			b.traced &= ~Sim.WATCHVECTOR;
		}
	}

	/**
	 * just in case node appears in more than one bit vector, run through all
	 * the vectors being traced and make sure the flag is set for each node.
	 */
	private void set_vec_nodes(int flag)
	{
		for(Bits b = blist; b != null; b = b.next)
			if ((b.traced & flag) != 0)
				for(int i = 0; i < b.nbits; i += 1)
					b.nodes[i].nflags |= flag;
	}

	/**
	 * set/clear stop bit in node
	 */
	private int nstop(Sim.Node n, String flag)
	{
		n = UnAlias(n);

		if ((n.nflags & Sim.MERGED) != 0)
			return 1;

		if (flag.startsWith("-"))
			n.nflags &= ~Sim.STOPONCHANGE;
		else
			n.nflags |= Sim.STOPONCHANGE;
		return 1;
	}

	/**
	 * set/clear stop bit in vector
	 */
	private int vstop(Bits b, String flag)
	{
		if (flag.startsWith("+"))
			b.traced |= Sim.STOPVECCHANGE;
		else
		{
			for(int i = 0; i < b.nbits; i += 1)
				b.nodes[i].nflags &= ~Sim.STOPVECCHANGE;
			b.traced &= ~Sim.STOPVECCHANGE;
		}
		return 1;
	}

	/**
	 * mark nodes and vectors for tracing
	 */
	private void settrace(String [] args)
	{
		apply(SETTRACE_CALL, 0, args, 1, args.length);
		set_vec_nodes(Sim.WATCHVECTOR);
	}

	/**
	 * mark nodes and vectors for stoping
	 */
	private void setstop(String [] args)
	{
		apply(SETSTOP_CALL, 0, args, 1, args.length);
		set_vec_nodes(Sim.STOPVECCHANGE);
	}

	/**
	 * set bit vector
	 */
	private void setvector(String [] args)
	{
//		CHAR           *val = targv[2];
//
//		// find vector
//		boolean found = false;
//		Bits b;
//		for(b = blist; b != null; b = b.next)
//		{
//			if (b.name.equalsIgnoreCase(args[1]))
//			{
//				found = true;
//				break;
//			}
//		}
//		if (!found)
//		{
//			Sim.irsim_error(filename, lineno, args[1] + ": No such vector");
//			return 0;
//		}
//
//		// set nodes
//		if (args[2].length() != b.nbits)
//		{
//			Sim.irsim_error(filename, lineno, "wrong number of bits for this vector");
//			return 0;
//		}
//		for(int i = 0; i < b.nbits; i++)
//		{
//			if ((val[i] = potchars.charAt(ch2pot(val[i]))) == '.')
//				return 0;
//		}
//		for(int i = 0; i < b.nbits; i++)
//			irsim_setin(b.nodes[i], val++);
	}


	private int CompareVector(Sim.Node [] np, String name, int nbits, String mask, String value)
	{
		if (value.length() != nbits)
		{
			System.out.println("wrong number of bits for value");
			return 0;
		}
		if (mask != null && mask.length() != nbits)
		{
			System.out.println("wrong number of bits for mask");
			return 0;
		}

		for(int i = 0; i < nbits; i++)
		{
			if (mask != null && mask.charAt(i) != '0') continue;
			Sim.Node n = np[i];
			int val = ch2pot(value.charAt(i));
			if (val >= Sim.N_POTS)
				return 0;
			if (val == Sim.X_X) val = Sim.X;
				if (n.npot != val)
					return 1;
		}
		return 0;
	}

	private static class Find1Arg
	{
		Sim.Node  node;
		Bits  vec;
		int   num;
	};

	/* assert a bit vector */
	private void doAssert(String [] args)
	{
		String mask = null;
		StringBuffer value = null;
		if (args.length == 4)
		{
			mask = args[2];
			value = new StringBuffer(args[3]);
		} else
		{
			value = new StringBuffer(args[2]);
		}

		Find1Arg f = (Find1Arg)apply(FINDONE_CALL, 0, args, 1, args.length-1);

		int comp = 0, nbits = 0;
		String name = null;
		Sim.Node [] nodes = null;
		if (f.num > 1)
			System.out.println(args[1] + " matches more than one node or vector");
		else if (f.node != null)
		{
			name = f.node.nname;
			f.node = UnAlias(f.node);
			Sim.Node [] nodeList = new Sim.Node[1];
			nodeList[0] = f.node;
			comp = CompareVector(nodeList, name, 1, mask, value.toString());
			nodes = nodeList;
			nbits = 1;
		}
		else if (f.vec != null)
		{
			comp = CompareVector(f.vec.nodes, f.vec.name, f.vec.nbits, mask, value.toString());
			name = f.vec.name;
			nbits = f.vec.nbits;
			nodes = f.vec.nodes;
		}
		if (comp != 0)
		{
			String infstr = "";
			for(int i = 0; i < nbits; i++)
			{
				if (mask != null && i < mask.length() && mask.charAt(i) != '0')
				{
					infstr += "-";
					value.setCharAt(i, '-');
				}
				else
					infstr += Sim.irsim_vchars.charAt(nodes[i].npot);
			}
			System.out.println("Assertion failed on '" + name + "': want (" + value + ") but got (" + infstr + ")");
		}
	}

	private void doUntil(String [] args)
	{
		String mask = null;
		StringBuffer value = null;
		int ccount = 0;
		if (args.length == 5)
		{
			mask = args[2];
			value = new StringBuffer(args[3]);
			ccount = TextUtils.atoi(args[4]);
		} else
		{
			mask = null;
			value = new StringBuffer(args[2]);
			ccount = TextUtils.atoi(args[3]);
		}

		Find1Arg f = (Find1Arg)apply(FINDONE_CALL, 0, args, 1, args.length);
		String name = null;
		int comp = 0;
		int nbits = 0;
		Sim.Node [] nodes = null;
		if (f.num > 1)
			System.out.println(args[1] + " matches more than one node or vector");
		else if (f.node != null)
		{
			name = f.node.nname;
			f.node = UnAlias(f.node);
			Sim.Node [] nodeList = new Sim.Node[1];
			nodeList[0] = f.node;
			int cnt = 0;
			while ((cnt <= ccount) && (comp = CompareVector(nodeList, name, 1, mask, value.toString())) != 0)
			{
				cnt++;
				clockit(1);
			}
			nodes = new Sim.Node[1];
			nodes[0] = f.node;
			nbits = 1;
		}
		else if (f.vec != null)
		{
			int cnt = 0;
			while ((cnt <= ccount) && (comp = CompareVector(f.vec.nodes, f.vec.name, f.vec.nbits, mask,
				value.toString())) != 0)
			{
				cnt++;
				clockit(1);
			}
			name = f.vec.name;
			nbits = f.vec.nbits;
			nodes = f.vec.nodes;
		}
		if (comp != 0)
		{
			String infstr = "";
			for(int i = 0; i < nbits; i++)
			{
				if (mask != null && mask.charAt(i) != '0')
				{
					infstr += "-";
					value.setCharAt(i, '-');
				}
				else
					infstr += Sim.irsim_vchars.charAt(nodes[i].npot);
			}
			System.out.println("Assertion failed on '" + name + ": want (" + value + ") but got (" + infstr + ")");
		}
	}

	private Sim.Node aw_trig; /* keeps current AssertWhen trigger */
	private AssertWhen aw_p;   /* track pointer on the current AssertWhen list */

	private void setupAssertWhen(Sim.Node n, String val)
	{
		AssertWhen p = new AssertWhen();
		p.node = n;
		p.val  = val.charAt(0);
		p.nxt = null;

		if (aw_trig.awpending == null)
		{
			// first time
			aw_trig.awpending = p;
			aw_p = p;
		} else
		{
			// more than 1 matching nodes
			aw_p.nxt = p;
			aw_p = p;
		}
	}

	private void doAssertWhen(String [] args)
	{
		Find1Arg trig = (Find1Arg)apply(FINDONE_CALL, 0, args, 1, args.length);

		if (trig.num > 1)
			System.out.println(args[1] + " matches more than one node or vector");
		else if (trig.node != null)
		{
			trig.node = UnAlias(trig.node);
			aw_trig = trig.node;
			aw_trig.awpot = (short)ch2pot(args[2].charAt(0));
			apply(ASSERTWHEN_CALL, 0, args, 3, args.length);
		}
		else if (trig.vec != null)
			System.out.println("trigger to assertWhen " + args[1] + " can't be a vector");
	}

	public void irsim_evalAssertWhen(Sim.Node n)
	{
		for (AssertWhen p = n.awpending; p != null; )
		{
			String name = p.node.nname;
			StringBuffer sb = new StringBuffer();
			sb.append((char)p.val);
			Sim.Node [] nodes = new Sim.Node[1];
			nodes[0] = p.node;
			int comp = CompareVector(nodes, name, 1, null, sb.toString());
			if (comp != 0)
				System.out.println("Assertion failed on '" + name + "'");
			p = p.nxt;
		}
		n.awpending = null;
	}

	private void collect_inputs(Sim.Node n, Sim.Node [] inps)
	{
		if ((n.nflags & (Sim.INPUT|Sim.ALIAS|Sim.POWER_RAIL|Sim.VISITED|Sim.INPUT_MASK)) == Sim.INPUT)
		{
			n.setNext(inps[n.npot]);
			inps[n.npot] = n;
			n.nflags |= Sim.VISITED;
		}
	}

	/* display current inputs */
	private void inputs(String [] args)
	{
		Sim.Node [] inptbl = new Sim.Node[Sim.N_POTS];

		inptbl[Sim.HIGH] = inptbl[Sim.LOW] = inptbl[Sim.X] = null;
		for(Iterator it = theSim.irsim_GetNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			collect_inputs(n, inptbl);
		}

		System.out.print("h inputs:");
		for(Iterator it = irsim_hinputs.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			System.out.print(" " + n.nname);
		}
		for(Sim.Node n = inptbl[Sim.HIGH]; n != null; n.nflags &= ~Sim.VISITED, n = n.getNext())
			System.out.print(" " + n.nname);
		System.out.println();

		System.out.print("l inputs:");
		for(Iterator it = irsim_linputs.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			System.out.print(" " + n.nname);
		}
		for(Sim.Node n = inptbl[Sim.LOW]; n != null; n.nflags &= ~Sim.VISITED, n = n.getNext())
			System.out.print(" " + n.nname);
		System.out.println();

		System.out.println("u inputs:");
		for(Iterator it = irsim_uinputs.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			System.out.println(" " + n.nname);
		}
		for(Sim.Node n = inptbl[Sim.X]; n != null; n.nflags &= ~Sim.VISITED, n = n.getNext())
			System.out.println(" " + n.nname);
		System.out.println();
	}

	/**
	 * set stepsize
	 */
	private void setstep(String [] args)
	{
		if (args.length == 1)
			System.out.println("stepsize = " + Sim.d2ns(irsim_stepsize));
		else if (args.length == 2)
		{
			double timeNS = TextUtils.atof(args[1]);
			long newsize = Sim.ns2d(timeNS);

			if (newsize <= 0)
			{
				System.out.println("Bad step size: " + TextUtils.formatDouble(timeNS*1000) + "psec (must be 10 psec or larger)");
			} else
				irsim_stepsize = newsize;
		}
	}

	/**
	 * Display traced vectors that just changed.  There should be at least one.
	 */
	public void irsim_disp_watch_vec(long which)
	{
		which &= (Sim.WATCHVECTOR | Sim.STOPVECCHANGE);
		String temp = " @ " + Sim.d2ns(theSim.irsim_cur_delta) + "ns ";
		System.out.println(temp);
		column = temp.length();
		for(Bits b = blist; b != null; b = b.next)
		{
			if ((b.traced & which) == 0)
				continue;
			int i;
			for(i = b.nbits - 1; i >= 0; i--)
				if (b.nodes[i].getTime() == theSim.irsim_cur_delta)
					break;
			if (i >= 0)
				dvec(b);
		}
	}

	/**
	 * Settle network until the specified stop time is reached.
	 * Premature returns (before stop time) indicate that a node/vector whose
	 * stop-bit set has just changed value.
	 */
	private long relax(long stoptime)
	{
		while (theSim.getModel().irsim_step(stoptime)) ;

		return theSim.irsim_cur_delta - stoptime;
	}

	/**
	 * relax network, optionally set stepsize
	 */
	private void dostep(String [] args)
	{
		long newsize = irsim_stepsize;
		if (args.length == 2)
		{
			double timeNS = TextUtils.atof(args[1]);
			newsize = Sim.ns2d(timeNS);
			if (newsize <= 0)
			{
				System.out.println("Bad step size: " + TextUtils.formatDouble(timeNS*1000) + "psec (must be 10 psec or larger)");
				return;
			}
		}

		relax(theSim.irsim_cur_delta + newsize);
		pnwatchlist();
	}

	/**
	 * destroy sequence for given node/vector: update sequence list and length.
	 * return -1 if we can't destroy the sequence (in stopped state).
	 */
	private Sequence undefseq(Object p, Sequence list, GenMath.MutableInteger lmax)
	{
		Sequence u, t;
		for(u=null, t = list; t != null; u = t, t = t.next)
			if (t.n == p)
				break;
		if (t != null)
		{
			if (u == null)
				list = t.next;
			else
				u.next = t.next;
			int i = 0;
			for(t = list; t != null; t = t.next)
				if (t.nvalues > i) i = t.nvalues;
					lmax.setValue(i);
		}
		return list;
	}

	/**
	 * process command line to yield a sequence structure.  first arg is the
	 * name of the node/vector for which the sequence is to be defined, second
	 * and following args are the values.
	 */
	private Sequence defsequence(String [] args, Sequence list, GenMath.MutableInteger lmax)
	{
		// if no arguments, get rid of all the sequences we have defined
		if (args.length == 1)
		{
			while (list != null)
				list = undefseq(list.n, list, lmax);
			return list;
		}

		// see if we can determine if name is for node or vector
		boolean isOK = false;
		int which = 0, size = 0;
		Bits b = null;
		Sim.Node n = null;
		for(b = blist; b != null; b = b.next)
			if (b.name.equalsIgnoreCase(args[1]))
		{
			which = 1;    size = b.nbits;    isOK = true;   break;
		}
		if (!isOK)
		{
			n = theSim.irsim_find(args[1]);
			if (n == null)
			{
				System.out.println(args[0] + ": No such node or vector");
				return list;
			}
			n = UnAlias(n);
			if ((n.nflags & Sim.MERGED) != 0)
			{
				System.out.println(n.nname + " can't be part of a sequence");
				return list;
			}
			which = 0; size = 1;
		}

		if (args.length == 2)	// just destroy the given sequence
		{
            Object objB = b;
            Object objN = n;
			list = undefseq((which != 0) ? objB : objN, list, lmax);
			return list;
		}

		// make sure each value specification is the right length
		for(int i = 2; i < args.length; i += 1)
			if (args[i].length() != size)
		{
			System.out.println("value \"" + args[i] + "\" is not compatible with size of " + args[2] + " (" + size + ")");
			return list;
		}

		Sequence s = new Sequence();
		s.values = new char[args.length-1];
		s.which = which;
		s.vsize = size;
		s.nvalues = args.length - 2;
		if (which != 0)	s.b = b;
			else	s.n = n;

		// process each value specification saving results in sequence
		int q = 0;
		for(int i = 2; i < args.length; i += 1)
		{
			for(int p=0; p<args[i].length(); p++)
				if ((s.values[q++] = potchars.charAt(ch2pot(args[i].charAt(p)))) == '.')
			{
				return list;
			}
		}

		// all done!  remove any old sequences for this node or vector.
		list = undefseq(s.n, list, lmax);

		// insert result onto list
		s.next = list;
		list = s;
		if (s.nvalues > lmax.intValue())
			lmax.setValue(s.nvalues);
		return list;
	}

	/**
	 * set each node/vector in sequence list to its next value
	 */
	private void vecvalue(Sequence list, int index)
	{
		for(; list != null; list = list.next)
		{
			int offset = list.vsize * (index % list.nvalues);
//			Sim.Node n = (list.which == 0) ? list.n : list.b.nodes;
//			for(int i = 0; i < list.vsize; i++)
//				irsim_setin(*n++, &list.values[offset++]);
		}
	}

	/**
	 * define clock sequences(s)
	 */
	private void setclock(String [] args)
	{
		// process sequence and add to clock list
		GenMath.MutableInteger mi = new GenMath.MutableInteger(maxclock);
		xclock = defsequence(args, xclock, mi);
		maxclock = mi.intValue();
	}

	/**
	 * Step each clock node through one simulation step
	 */
	private int which_phase = 0;
	private int step_phase()
	{
		vecvalue(xclock, which_phase);
		if (relax(theSim.irsim_cur_delta + irsim_stepsize) != 0)
			return 1;
		which_phase = (which_phase + 1) % maxclock;
		return 0;
	}

	/* Do one simulation step */
	private void dophase(String [] args)
	{
		step_phase();
		pnwatchlist();
	}

	/**
	 * clock circuit specified number of times
	 */
	private int clockit(int n)
	{
		int  i = 0;

		if (xclock == null)
		{
			System.out.println("no clock nodes defined!");
		} else
		{
			/* run 'em by setting each clock node to successive values of its
			 * associated sequence until all phases have been run.
			 */
			while (n-- > 0)
			{
				for(i = 0; i < maxclock; i += 1)
				{
					if (step_phase() != 0)
					{
						n = 0;
						break;
					}
				}
			}

			// finally display results if requested to do so
			pnwatchlist();
		}
		return maxclock - i;
	}

	/**
	 * clock circuit through all the input vectors previously set up
	 */
	private void runseq(String [] args)
	{
		// calculate how many clock cycles to run
		int n = 1;
		if (args.length == 2)
		{
			n = TextUtils.atoi(args[1]);
			if (n <= 0)
				n = 1;
		}

		/* run 'em by setting each input node to successive values of its
		 * associated sequence.
		 */
		if (slist == null)
		{
			System.out.println("no input vectors defined!");
		} else
			while (n-- > 0)
				for(int i = 0; i < maxsequence; i += 1)
		{
			vecvalue(slist, i);
			if (clockit(1) != 0)
				return;
			pnwatchlist();
		}
	}

	/**
	 * process "c" command line
	 */
	private void doclock(String [] args)
	{
		// calculate how many clock cycles to run
		int  n = 1;
		if (args.length == 2)
		{
			n = TextUtils.atoi(args[1]);
			if (n <= 0)
				n = 1;
		}

		clockit(n);		// do the hard work
	}

	/**
	 * output message to console/log file
	 */
	private void domsg(String [] args)
	{
		String infstr = "";
		for(int n=1; n<args.length; n++)
		{
			if (n != 1) infstr += " ";
			infstr += args[n];
		}
		System.out.println(infstr);
	}

	/**
	 * display info about a node
	 */
	private void quest(String [] args)
	{
		apply(QUEST_CALL, 0, args, 1, args.length);
	}

	/**
	 * set decay parameter
	 */
	private void setdecay(String [] args)
	{
		if (args.length == 1)
		{
			if (theSim.irsim_tdecay == 0)
				System.out.println("decay = No decay");
			else
				System.out.println("decay = " + Sim.d2ns(theSim.irsim_tdecay) + "ns");
		} else
		{
			theSim.irsim_tdecay = Sim.ns2d(TextUtils.atof(args[1]));
			if (theSim.irsim_tdecay < 0)
				theSim.irsim_tdecay = 0;
		}
	}

	/**
	 * set unitdelay parameter
	 */
	private void setunit(String [] args)
	{
		if (args.length == 1)
		{
			if (theSim.irsim_tunitdelay == 0)
				System.out.println("unitdelay = OFF");
			else
				System.out.println("unitdelay = " + Sim.d2ns(theSim.irsim_tunitdelay));
		} else
		{
			theSim.irsim_tunitdelay = (int) Sim.ns2d(TextUtils.atof(args[1]));
			if (theSim.irsim_tunitdelay < 0)
				theSim.irsim_tunitdelay = 0;
		}
	}

	static long ptime;
	/**
	 * print traceback of node's activity and that of its ancestors
	 */
	private void cpath(Sim.Node n, int level)
	{
		// no last transition!
		if ((n.nflags & Sim.MERGED) != 0 || n.getCause() == null)
		{
			System.out.println("  there is no previous transition!");
		}

		/* here if we come across a node which has changed more recently than
		 * the time reached during the backtrace.  We can't continue the
		 * backtrace in any reasonable fashion, so we stop here.
		 */
		else if (level != 0 && n.getTime() > ptime)
		{
			System.out.println("  transition of " + n.nname + ", which has since changed again");
		}
		/* here if there seems to be a cause for this node's transition.
		 * If the node appears to have 'caused' its own transition (n.t.cause
		 * == n), that means it was input.  Otherwise continue backtrace...
		 */
		else if (n.getCause() == n)
		{
			System.out.println("  " + n.nname + " . " + Sim.irsim_vchars.charAt(n.npot) +
				" @ " + Sim.d2ns(n.getTime()) + "ns , node was an input");
		}
		else if ((n.getCause().nflags & Sim.VISITED) != 0)
		{
			System.out.println("  ... loop in traceback");
		}
		else
		{
			long  delta_t = n.getTime() - n.getCause().getTime();

			n.nflags |= Sim.VISITED;
			ptime = n.getTime();
			cpath(n.getCause(), level + 1);
			n.nflags &= ~Sim.VISITED;
			if (delta_t < 0)
				System.out.println("  " + n.nname + " . " + Sim.irsim_vchars.charAt(n.npot) +
					" @ " + Sim.d2ns(n.getTime()) + "ns   (??)");
			else
				System.out.println("  " + n.nname + " . " + Sim.irsim_vchars.charAt(n.npot) +
					" @ " + Sim.d2ns(n.getTime()) + "ns   (" + Sim.d2ns(delta_t) + "ns)");
		}
	}

	private int do_cpath(Sim.Node n)
	{
		System.out.println("critical path for last transition of " + n.nname + ":");
		n = UnAlias(n);
		cpath(n, 0);
		return 1;
	}

	/**
	 * discover and print critical path for node's last transistion
	 */
	private void dopath(String [] args)
	{
		apply(PATH_CALL, 0, args, 1, args.length);
	}

	static final int NBUCKETS		= 20;	/* number of buckets in histogram */

	/**
	 * print histogram of circuit activity in specified time interval
	 */
	private void doactivity(String [] args)
	{
		long begin = Sim.ns2d(TextUtils.atof(args[1]));
		long end = theSim.irsim_cur_delta;
		if (args.length > 2)
			end = Sim.ns2d(TextUtils.atof(args[2]));
		if (end < begin)
		{
			long swp = end;   end = begin;   begin = swp;
		}

		// collect histogram info by walking the network
		long  [] table = new long[NBUCKETS];
		for(int i = 0; i < NBUCKETS; table[i++] = 0);

		long size = (end - begin + 1) / NBUCKETS;
		if (size <= 0) size = 1;

		for(Iterator it = theSim.irsim_GetNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			if ((n.nflags & (Sim.ALIAS | Sim.MERGED | Sim.POWER_RAIL)) == 0)
			{
				if (n.getTime() >= begin && n.getTime() <= end)
					table[(int)((n.getTime() - begin) / size)] += 1;
			}
		}

		// print out what we found
		int total = 0;
		for(int i = 0; i < NBUCKETS; i++) total += table[i];

		System.out.println("Histogram of circuit activity: " + Sim.d2ns(begin) +
			" . " + Sim.d2ns(end) + "ns (bucket size = " + Sim.d2ns(size) + ")");

		for(int i = 0; i < NBUCKETS; i += 1)
			System.out.println(" " + Sim.d2ns(begin + (i * size)) + " -" + Sim.d2ns(begin + (i + 1) * size) + table[i]);
	}

	/**
	 * Print list of nodes which last changed value in specified time interval
	 */
	private void dochanges(String [] args)
	{
		long begin = Sim.ns2d(TextUtils.atof(args[1]));;
		long end = theSim.irsim_cur_delta;
		if (args.length > 2)
			end = Sim.ns2d(TextUtils.atof(args[2]));

		column = 0;
		System.out.print("Nodes with last transition in interval " + Sim.d2ns(begin) + " . " + Sim.d2ns(end) + "ns:");

		for(Iterator it = theSim.irsim_GetNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			n = UnAlias(n);

			if ((n.nflags & (Sim.MERGED | Sim.ALIAS)) != 0)
				return;

			if (n.getTime() >= begin && n.getTime() <= end)
			{
				int i = n.nname.length() + 2;
				if (column + i >= MAXCOL)
				{
					column = 0;
				}
				column += i;
				System.out.print("  " + n.nname);
			}
		}
		System.out.println();
	}

	/**
	 * Print list of nodes with undefined (X) value
	 */
	private void doprintX(String [] args)
	{
		System.out.print("Nodes with undefined potential:");
		column = 0;
		for(Iterator it = theSim.irsim_GetNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			n = UnAlias(n);

			if ((n.nflags & (Sim.MERGED | Sim.ALIAS)) == 0 && n.npot == Sim.X)
			{
				int i = n.nname.length() + 2;
				if (column + i >= MAXCOL)
				{
					column = 0;
				}
				column += i;
				System.out.print("  " + n.nname);
			}
		}
		System.out.println();
	}

	/**
	 * Print nodes that are aliases
	 */
	private void doprintAlias(String [] args)
	{
		if (theSim.irsim_naliases == 0)
			System.out.println("there are no aliases");
		else
		{
			System.out.println("there are " + theSim.irsim_naliases + " aliases:");
			for(Iterator it = theSim.irsim_GetNodeList().iterator(); it.hasNext(); )
			{
				Sim.Node n = (Sim.Node)it.next();
				if ((n.nflags & Sim.ALIAS) != 0)
				{
					n = UnAlias(n);
					String is_merge = (n.nflags & Sim.MERGED) != 0 ? " (part of a stack)" : "";
					System.out.println("  " + n.nname + " . " + n.nname + is_merge);
				}
			}
		}
	}

	/**
	 * Print list of transistors with src/drn shorted (or between power supplies).
	 */
	private void print_tcap(String [] args)
	{
		if (theSim.irsim_tcap.getSTrans() == theSim.irsim_tcap)
			System.out.println("there are no shorted transistors");
		else
			System.out.println("shorted transistors:");
		for(Sim.Trans t = theSim.irsim_tcap.getSTrans(); t != theSim.irsim_tcap; t = t.getSTrans())
		{
			System.out.println(" " + Sim.irsim_ttype[Sim.BASETYPE(t.ttype)] + " g=" + ((Sim.Node)t.gate).nname + " s=" +
					t.source.nname + " d=" + t.drain.nname + " (" +
					(t.r.length / theSim.getConfig().irsim_LAMBDACM) + "x" + (t.r.width / theSim.getConfig().irsim_LAMBDACM) + ")");
		}
	}

	/**
	 * Move back simulation time to specified time.
	 */
	private void back_time(String [] args)
	{
		long newt = Sim.ns2d(TextUtils.atof(args[1]));
		if (newt < 0 || newt > theSim.irsim_cur_delta)
		{
			System.out.println(args[1] + ": invalid time");
			return;
		}

		theSim.irsim_cur_delta = newt;
		irsim_ClearInputs();
		theSim.getModel().irsim_back_sim_time(theSim.irsim_cur_delta, 0);
		theSim.irsim_cur_node = null;			// fudge
		List nodes = theSim.irsim_GetNodeList();
		for(Iterator it = nodes.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			theSim.irsim_backToTime(n);
		}
		if (theSim.irsim_cur_delta == 0)
			theSim.getModel().irsim_ReInit();

		if (irsim_analyzerON)
			irsim_RestartAnalyzer(0, theSim.irsim_cur_delta, 1);

		pnwatchlist();
	}

	private void irsim_ClearInputs()
	{
		for(int i = 0; i < 5; i++)
		{
			if (irsim_listTbl[i] == null)
				continue;
			for(Iterator it = irsim_listTbl[i].iterator(); it.hasNext(); )
			{
				Sim.Node n = (Sim.Node)it.next();
				if ((n.nflags & Sim.POWER_RAIL) == 0)
					n.nflags &= ~(Sim.INPUT_MASK | Sim.INPUT);
			}
			irsim_listTbl[i] = null;
		}
		for(Iterator it = theSim.irsim_GetNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			clear_input(n);
		}
	}

	private void clear_input(Sim.Node n)
	{
		if ((n.nflags & Sim.POWER_RAIL) == 0)
			n.nflags &= ~Sim.INPUT;
	}

	private int tranCntNSD = 0, tranCntNG = 0;

	/**
	 * Print event statistics.
	 */
	private void do_stats(String [] args)
	{
		if (args.length == 2)
		{
			if (tranCntNG == 0 && tranCntNSD == 0)
			{
				for(Iterator it = theSim.irsim_GetNodeList().iterator(); it.hasNext(); )
				{
					Sim.Node n = (Sim.Node)it.next();
					if ((n.nflags & (Sim.ALIAS | Sim.POWER_RAIL)) == 0)
					{
						tranCntNG += n.ngateList.size();
						tranCntNSD += n.ntermList.size();
					}
				}
				System.out.println("avg: # gates/node = " + TextUtils.formatDouble(tranCntNG / theSim.irsim_nnodes) +
					",  # src-drn/node = " + TextUtils.formatDouble(tranCntNSD / theSim.irsim_nnodes));
			}
		}
		System.out.println("changes = " + theSim.irsim_num_edges);
		System.out.println("punts (cns) = " + theSim.irsim_num_punted + " (" + theSim.irsim_num_cons_punted + ")");
		String n1 = "0.0";
		String n2 = "0.0";
		if (theSim.irsim_num_punted != 0)
		{
			n1 = TextUtils.formatDouble(100.0 / (theSim.irsim_num_edges / theSim.irsim_num_punted + 1.0));
			n2 = TextUtils.formatDouble(theSim.irsim_num_cons_punted * 100.0 / theSim.irsim_num_punted);
		}
		System.out.println("punts = " + n1 + "%, cons_punted = " + n2 + "%");

		System.out.println("nevents = " + theSim.irsim_nevent);
	}
}
