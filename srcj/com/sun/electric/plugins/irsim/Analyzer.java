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
import com.sun.electric.tool.io.output.IRSIM;
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
	static final String simVersion = "9.5j";

	// the meaning of SimVector.command
	/** a command that isn't interpreted */				private static final int VECTORCOMMAND    =  0;
	/** a comment in the command file */				private static final int VECTORCOMMENT    =  1;
	/** the "l" command (set signal low) */				private static final int VECTORL          =  2;
	/** the "h" command (set signal high) */			private static final int VECTORH          =  3;
	/** the "x" command (set signal undefined) */		private static final int VECTORX          =  4;
	/** the "s" command (advance time) */				private static final int VECTORS          =  5;
	/** the "assert" command (test signal value) */		private static final int VECTORASSERT     =  6;
	/** the "clock" command to declare stimuli */		private static final int VECTORCLOCK      =  7;
	/** the "c" command to run a clock cycle */			private static final int VECTORC          =  8;
	/** the "vector" command to group signals */		private static final int VECTORVECTOR     =  9;
	/** the "stepsize" command to set time advance */	private static final int VECTORSTEPSIZE   = 10;
	/** the "set" command to change vector values */	private static final int VECTORSET        = 11;
	/** the "!" command to print gate info */			private static final int VECTOREXCL       = 12;
	/** the "?" command to print source/drain info */	private static final int VECTORQUESTION   = 13;
	/** the "t" command to trace */						private static final int VECTORTRACE      = 14;

	/** default simulation steps per screen */			private static final int    DEF_STEPS         = 4;
	/** initial size of simulation window: 10ns */		private static final double DEFIRSIMTIMERANGE = 10.0E-9f;

	private static class SimVector
	{
		/** index of command */				int       command;
		/** parameters to the command */	String [] parameters;
		/** next in list of vectors */		SimVector next;
	};

	private static class TraceEnt
	{
		/** name stripped of path */		String      name;
											Sim.Node    waveformData;
		/** what makes up this trace */		Sim.Node    nd;
		/** window start */					Sim.HistEnt wind;
		/** cursor value */					Sim.HistEnt cursor;
	};

	private static class Bits
	{
		/** next bit vector in chain */				Bits     next;
		/** name of this vector of bits */			String   name;
		/** <>0 if this vector is being traced */	int      traced;
		/** number of bits in this vector */		int      nBits;
		/** pointers to the bits (nodes) */			Sim.Node nodes[];
	};

	private SimVector firstVector = null;
	private SimVector lastVector = null;
	private long      stepSize = 50000;

	private List      traceList;
	private long      firstTime;
	private long      lastTime;
	private long      startTime;
	private long      stepsTime;
	private long      endTime;
	private long      lastStart;		/* last redisplay starting time */

	private double [] traceTime;
	private short  [] traceState;
	private int       traceTotal = 0;

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

        	analyzer.firstVector = null;
        	analyzer.traceList = new ArrayList();

    		// now initialize the simulator
    		System.out.println("IRSIM, version " + simVersion);

    		List components = null;
    		URL fileURL = null;
    		if (fileName == null)
    		{
    			// generate the components directly
    			components = IRSIM.getIRSIMComponents(cell, context);
    		} else
    		{
    			// get a pointer to to the file with the network (.sim file)
    			fileURL = TextUtils.makeURLToFile(fileName);
    		}

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
	    			if (sim.getConfig().loadConfig(url)) return true;
	    		}
    		}

    		// Load network
    		analyzer.initRSim();
    		if (sim.readNetwork(fileURL, components)) return true;

    		// convert the stimuli
			Stimuli sd = new Stimuli();
    		sd.setSeparatorChar('/');
    		sd.setCell(cell);
    		for(Iterator it = sim.getNodeList().iterator(); it.hasNext(); )
    		{
    			Sim.Node n = (Sim.Node)it.next();
    			if (n.nName.equalsIgnoreCase("vdd") || n.nName.equalsIgnoreCase("gnd")) continue;

    			// make a signal for it
				Stimuli.DigitalSignal sig = new Stimuli.DigitalSignal(null);
				n.sig = sig;
				int slashPos = n.nName.lastIndexOf('/');
				if (slashPos >= 0)
				{
					sig.setSignalName(n.nName.substring(slashPos+1));
					sig.setSignalContext(n.nName.substring(0, slashPos));
				} else
				{
					sig.setSignalName(n.nName);
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
    		if (!analyzer.analyzerON)
    		{
    			analyzer.initTimes(0, 50000, sim.curDelta);
    		}

    		for(Iterator it = sim.getNodeList().iterator(); it.hasNext(); )
    		{
    			Sim.Node n = (Sim.Node)it.next();
    			analyzer.addNode(n);
    		}
    		analyzer.updateWindow(0);
    		analyzer.displayTraces(analyzer.analyzerON);
    		analyzer.analyzerON = true;

    		// read signal values
    		analyzer.updateWindow(sim.curDelta);
            return true;
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
				newVector(veccmd, parameters, ww.getMainTimeCursor());
			}
			if (Simulation.isIRSIMResimulateEach())
				playVectors();
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

			SimVector lastSV = null;
			SimVector nextSV = null;
			for(SimVector sv = firstVector; sv != null; sv = nextSV)
			{
				nextSV = sv.next;
				if (sv.command == VECTORL || sv.command == VECTORH || sv.command == VECTORX ||
					sv.command == VECTORASSERT || sv.command == VECTORSET)
				{
					if (sv.parameters[0].equals(highsigname))
					{
						if (lastSV == null)
							firstVector = sv.next; else
								lastSV.next = sv.next;
						continue;
					}
				}
				lastSV = sv;
			}
			lastVector = lastSV;
			if (Simulation.isIRSIMResimulateEach())
				playVectors();
			return;
		}
		if (command.equals("clearAll"))
		{
			if (Simulation.isIRSIMResimulateEach())
				clearAllVectors();
			return;
		}
		if (command.equals("update"))
		{
			playVectors();
			return;
		}
		if (command.equals("info"))
		{
			List signals = ww.getHighlightedNetworkNames();
			for(Iterator it = signals.iterator(); it.hasNext(); )
			{
				Stimuli.Signal sig = (Stimuli.Signal)it.next();
				issueCommand("! " + sig.getFullName().replace('.', '/'));
				issueCommand("? " + sig.getFullName().replace('.', '/'));
			}
			return;
		}
		if (command.equals("save"))
		{
			saveVectorFile();
			return;
		}
		if (command.equals("restore"))
		{
			loadVectorFile();
			return;
		}
	}

	/************************** LOW-LEVEL ANALYZER **************************/

	private void addNode(Sim.Node nd)
	{
		while ((nd.nFlags & Sim.ALIAS) != 0)
			nd = nd.nLink;

		if ((nd.nFlags & Sim.MERGED) != 0)
		{
			System.out.println("can't watch node " + nd.nName);
			return;
		}
		TraceEnt t = new TraceEnt();
		t.name = nd.nName;
		t.nd = nd;
		t.wind = t.cursor = nd.head;
		traceList.add(t);
	}

	private void displayTraces(boolean isMapped)
	{
		if (!isMapped)				// only the first time
		{
			flushTraceCache();
		} else
		{
			drawTraces(startTime, endTime);
		}
	}

	private void restartAnalyzer(long firstTime, long lastTime, int sameHist)
	{
		for(Iterator it = traceList.iterator(); it.hasNext(); )
		{
			TraceEnt t = (TraceEnt)it.next();
			t.wind = t.cursor = t.nd.head;
		}

		// should set "lastTime" to the width of the screen
		initTimes(firstTime, stepsTime / DEF_STEPS, lastTime);
		if (sameHist != 0)
			updateTraceCache(0);
		else
			flushTraceCache();
	}

	private void UpdateWinRemove()
	{
		drawTraces(startTime, endTime);
	}

	/**
	 * Initialize the display times so that when first called the last time is
	 * shown on the screen.  Default width is DEF_STEPS (simulation) steps.
	 */
	private void initTimes(long firstT, long stepSize, long lastT)
	{
		firstTime = firstT;
		lastTime = lastT;
		stepsTime = 4 * stepSize;

		if (startTime <= firstTime)
		{
			if (lastT < stepsTime)
			{
				startTime = firstTime;
				endTime = startTime + stepsTime;

				// make it conform to the displayed range
				Iterator it = ww.getPanels();
				if (it.hasNext())
				{
					WaveformWindow.Panel wp = (WaveformWindow.Panel)it.next();
					double max = wp.getMaxTimeRange();
					long endtime = Sim.nsToDelta(max * 1e9);
					if (endtime > endTime) endTime = endtime;
				}
				double max = getEndTime();
				long endT = Sim.nsToDelta(max * 1e9);
				if (endT > endTime) endTime = endT;
			} else
			{
				endTime = lastT + 2 * stepSize;
				startTime = endTime - stepsTime;
				if (startTime < firstTime)
				{
					stepSize = firstTime - startTime;
					startTime += stepSize;
					endTime += stepSize;
				}
			}
		}
	}

	/**
	 * Update the cache (begining of window and cursor) for traces that just
	 * became visible (or were just added).
	 */
	private void updateTraceCache(int firstTrace)
	{
		long startT = startTime;
		long cursT = firstTime;
		int n = 0;
		for(Iterator it = traceList.iterator(); it.hasNext(); )
		{
			TraceEnt t = (TraceEnt)it.next();
			if (n < firstTrace)
				continue;

			Sim.HistEnt p = t.wind;
			Sim.HistEnt h = t.cursor;
			Sim.HistEnt nextH = Sim.getNextHist(h);
			if (h.hTime > cursT || nextH.hTime <= cursT)
			{
				if (p.hTime <= cursT)
					t.cursor = p;
				else
					t.cursor = t.nd.head;
			}

			if (startT <= p.hTime)
				p = t.nd.head;

			h = Sim.getNextHist(p);
			while (h.hTime < startT)
			{
				p = h;
				h = Sim.getNextHist(h);
			}
			t.wind = p;

			p = t.cursor;
			h = Sim.getNextHist(p);
			while (h.hTime <= cursT)
			{
				p = h;
				h = Sim.getNextHist(h);
			}
			t.cursor = p;
		}
	}

	private void flushTraceCache()
	{
		lastStart = theSim.maxTime;
	}

	/**
	 * Draw the traces horizontally from time1 to time2.
	 */
	private void drawTraces(long t1, long t2)
	{
		if (startTime != lastStart)		// Update history cache
		{
			long startT = startTime;
			boolean begin = (startT < lastStart);
			for(Iterator it = traceList.iterator(); it.hasNext(); )
			{
				TraceEnt t = (TraceEnt)it.next();
				Sim.HistEnt p = begin ? t.nd.head : t.wind;
				Sim.HistEnt h = Sim.getNextHist(p);
				while (h.hTime < startT)
				{
					p = h;
					h = Sim.getNextHist(h);
				}
				t.wind = p;
			}
			lastStart = startTime;
		}

		for(Iterator it = traceList.iterator(); it.hasNext(); )
		{
			TraceEnt t = (TraceEnt)it.next();
			if (t.waveformData.sig == null) continue;

			if (t1 >= lastTime) continue;
			Sim.HistEnt h = t.wind;
			int count = 0;
			long curT = 0;
			long endT = t2;
			while (curT < endT)
			{
				int val = h.val;
				while (h.hTime < endT && h.val == val)
					h = Sim.getNextHist(h);

				// advance time
				long nextT;
				if (h.hTime > endT)
				{
					nextT = endT;
				} else
				{
					nextT = h.hTime;
				}

				// make sure there is room in the array
				if (count >= traceTotal)
				{
					int newTotal = traceTotal * 2;
					if (newTotal <= count) newTotal = count + 50;
					double [] newTime = new double[newTotal];
					short [] newState = new short[newTotal];
					for(int i=0; i<count; i++)
					{
						newTime[i] = traceTime[i];
						newState[i] = traceState[i];
					}
					traceTime = newTime;
					traceState = newState;
					traceTotal = newTotal;
				}

				traceTime[count] = curT / 100000000000.0;
				switch (val)
				{
					case Sim.LOW:
						traceState[count] = Stimuli.LOGIC_LOW | Stimuli.GATE_STRENGTH;
						break;
					case Sim.HIGH:
						traceState[count] = Stimuli.LOGIC_HIGH | Stimuli.GATE_STRENGTH;
						break;
					default:
						traceState[count] = Stimuli.LOGIC_X | Stimuli.GATE_STRENGTH;
						break;
				}
				curT = nextT;
				count++;
			}
			double [] timeVector = new double[count];
			int [] stateVector = new int[count];
			for(int i=0; i<count; i++)
			{
				timeVector[i] = traceTime[i];
				stateVector[i] = traceState[i];
			}
			t.waveformData.sig.setTimeVector(timeVector);
			t.waveformData.sig.setStateVector(stateVector);
		}
		ww.repaint();
	}

	private void UpdateTraces(long start, long end)
	{
		drawTraces(start, end);
	}

	/**
	 * Update the trace window so that endT is shown.  If the update fits in the
	 * window, simply draw the missing parts.  Otherwise scroll the traces,
	 * centered around endT.
	 */
	public void updateWindow(long endT)
	{
		long lastT = lastTime;
		lastTime = endT;

		for(Iterator it = traceList.iterator(); it.hasNext(); )
		{
			TraceEnt t = (TraceEnt)it.next();
    		for(Iterator nIt = theSim.getNodeList().iterator(); nIt.hasNext(); )
    		{
    			Sim.Node n = (Sim.Node)nIt.next();
				if (t.name.equals(n.nName))
				{
					t.waveformData = n;
					break;
				}
			}
		}

		if (endT <= endTime)
		{
			if (lastT >= startTime)
				UpdateTraces(lastT, endT);
			else if (endT > startTime)
				UpdateTraces(startTime, endT);
		} else					// endT > endTime
		{
			if (lastT < endTime)
				UpdateTraces(lastT, endTime);
		}
	}

	/************************** SIMULATION VECTORS **************************/

	/**
	 * Method to create a new simulation vector at time "time", on signal "sig",
	 * with state "state".  The vector is inserted into the play list in the proper
	 * order.
	 */
	private void newVector(int command, String [] params, double insertTime)
	{
		SimVector newsv = new SimVector();
		newsv.command = command;
		newsv.parameters = params;

		// insert the vector */
		SimVector lastSV = null;
		if (insertTime < 0.0)
		{
			lastSV = lastVector;
		} else
		{
			double defaultStepSize = 10.0 / 1000000000.0;
			double curTime = 0.0;
			int clockPhases = 0;
			for(SimVector sv = firstVector; sv != null; sv = sv.next)
			{
				switch (sv.command)
				{
					case VECTORCLOCK:
						clockPhases = sv.parameters.length - 1;
						break;
					case VECTORS:
						double stepSize = defaultStepSize;
						if (sv.parameters.length > 0)
							stepSize = TextUtils.atof(sv.parameters[0]) / 1000000000.0;
						if (GenMath.doublesLessThan(insertTime, curTime+stepSize))
						{
							// splitting step at "insertTime"
							sv.parameters = new String[1];
							sv.parameters[0] = TextUtils.formatDouble((insertTime-curTime) * 1000000000.0);

							// create second step to advance after this signal
							SimVector afterSV = new SimVector();
							afterSV.command = VECTORS;
							afterSV.parameters = new String[1];
							afterSV.parameters[0] = TextUtils.formatDouble((curTime+stepSize-insertTime) * 1000000000.0);
							afterSV.next = sv.next;
							sv.next = afterSV;
						}
						curTime += stepSize;
						break;
					case VECTORC:
						stepSize = defaultStepSize;
						if (sv.parameters.length > 0)
							stepSize = TextUtils.atof(sv.parameters[0]) / 1000000000.0;
						curTime += stepSize * clockPhases;
						break;
					case VECTORSTEPSIZE:
						if (sv.parameters.length > 0)
							defaultStepSize = TextUtils.atof(sv.parameters[0]) / 1000000000.0;
						break;
				}
				lastSV = sv;
				if (!GenMath.doublesLessThan(curTime, insertTime)) break;
			}
			if (GenMath.doublesLessThan(curTime, insertTime))
			{
				// create step to advance to the insertion time
				double thisStep = (insertTime-curTime) * 1000000000.0;
				if (thisStep > 0.005)
				{
					SimVector afterSV = new SimVector();
					afterSV.command = VECTORS;
					afterSV.parameters = new String[1];
					afterSV.parameters[0] = TextUtils.formatDouble(thisStep);
					if (lastSV == null)
					{
						afterSV.next = firstVector;
						firstVector = afterSV;
					} else
					{
						afterSV.next = lastSV.next;
						lastSV.next = afterSV;
					}
					lastSV = afterSV;
				}
			}
		}
		if (lastSV == null)
		{
			newsv.next = firstVector;
			firstVector = newsv;
		} else
		{
			newsv.next = lastSV.next;
			lastSV.next = newsv;
		}
		if (newsv.next == null)
		{
			lastVector = newsv;
		}
	}


	/**
	 * Method to examine the test vectors and determine the ending simulation time.
	 */
	private double getEndTime()
	{
		double defaultStepSize = 10.0 / 1000000000.0;
		double curTime = 0.0;
		int clockPhases = 0;
		for(SimVector sv = firstVector; sv != null; sv = sv.next)
		{
			switch (sv.command)
			{
				case VECTORCLOCK:
					clockPhases = sv.parameters.length - 1;
					break;
				case VECTORS:
					double stepsize = defaultStepSize;
					if (sv.parameters.length > 0)
						stepsize = TextUtils.atof(sv.parameters[0]) / 1000000000.0;
					curTime += stepsize;
					break;
				case VECTORC:
					stepsize = defaultStepSize;
					if (sv.parameters.length > 0)
						stepsize = TextUtils.atof(sv.parameters[0]) / 1000000000.0;
					curTime += stepsize * clockPhases;
					break;
				case VECTORSTEPSIZE:
					if (sv.parameters.length > 0)
						defaultStepSize = TextUtils.atof(sv.parameters[0]) / 1000000000.0;
					break;
			}
		}
		return curTime;
	}

	/**
	 * Method to play the simulation vectors into the simulator.
	 */
	private void playVectors()
	{
		issueCommand("back 0");
//		issueCommand("flush");

		double curTime = 0;
		analyzerON = false;
		for(SimVector sv = firstVector; sv != null; sv = sv.next)
		{
			if (sv.command == VECTORCOMMENT) continue;
			String infstr = commandName(sv.command);
			boolean addSpace = infstr.length() > 0;
			for(int i=0; i<sv.parameters.length; i++)
			{
				if (addSpace) infstr += " ";
				addSpace = true;
				infstr += sv.parameters[i];
			}
			issueCommand(infstr);
		}
		issueCommand("s");
		analyzerON = true;
		updateWindow(theSim.curDelta);

		// update main cursor location if requested
		if (Simulation.isIRSIMAutoAdvance())
			ww.setMainTimeCursor(curTime + 10.0/1000000000.0);
	}

	private String commandName(int command)
	{
		switch (command)
		{
			case VECTORCOMMAND:  return "";
			case VECTORCOMMENT:  return "|";
			case VECTORL:        return "l";
			case VECTORH:        return "h";
			case VECTORX:        return "x";
			case VECTORS:        return "s";
			case VECTORASSERT:   return "assert";
			case VECTORCLOCK:    return "clock";
			case VECTORC:        return "c";
			case VECTORVECTOR:   return "vector";
			case VECTORSTEPSIZE: return "stepsize";
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
	private void clearAllVectors()
	{
		firstVector = null;
		lastVector = null;
		playVectors();
	}

	/**
	 * Method to read simulation vectors from a file.
	 */
	private void loadVectorFile()
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
			while (firstVector != null)
			{
				SimVector sv = firstVector;
				firstVector = sv.next;
			}
			lastVector = null;
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
					newVector(VECTORCOMMENT, par, -1.0);
					continue;
				}

				// get the first keyword
				String [] targ = Sim.parseLine(buf, false);
				if (targ == null || targ.length <= 0) continue;

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
						newVector(command, par, -1.0);
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
					// ignore commands that need no interpretation
					newVector(VECTORCOMMAND, targ, -1.0);
					continue;
				}

				String [] justParams = new String[targ.length-1];
				for(int i=1; i<targ.length; i++) justParams[i-1] = targ[i];
				newVector(command, justParams, -1.0);

				// update the display if this is a "vector" command
				if (command == VECTORVECTOR)
				{
//					// make the bus
//					bussignals = (INTBIG *)emalloc((count-1) * SIZEOFINTBIG, el_tempcluster);
//					if (bussignals == 0) return;
//
//					// if the bus name already exists, delete it
//					highsiglist = sim_window_findtrace(inputParameters[0], &highsigcount);
//					for(i=0; i<highsigcount; i++)
//						sim_window_killtrace(highsiglist[i]);
//
//					j = 0;
//					for(i=1; i<count; i++)
//					{
//						highsiglist = sim_window_findtrace(inputParameters[i], &highsigcount);
//						if (highsigcount == 0)
//						{
//							System.out.println("Vector %s: cannot find signal '%s'"),
//								inputParameters[0], inputParameters[i]);
//						} else
//						{
//							bussignals[j++] = highsiglist[0];
//						}
//					}
//					if (j > 0)
//						(void)sim_window_makebus(j, bussignals, inputParameters[0]);
//					efree((CHAR *)bussignals);
				}
			}
			playVectors();

//			sim_window_gettimeextents(&min, &max);
//			ww.setDefaultTimeRange(min, max);
//			ww.setMainTimeCursor((max-min)/5.0*2.0+min);
//			ww.setExtensionTimeCursor((max-min)/5.0*3.0+min);
			updateWindow(theSim.curDelta);

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
	private void saveVectorFile()
	{
        String fileName = OpenFile.chooseOutputFile(FileType.IRSIMVECTOR, "IRSIM Vector file", Library.getCurrent().getName() + ".cmd");
        try
		{
        	PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));

    		for(SimVector sv = firstVector; sv != null; sv = sv.next)
    		{
    			String infstr = commandName(sv.command);
    			boolean addSpace = infstr.length() > 0;
    			for(int i=0; i<sv.parameters.length; i++)
    			{
    				if (addSpace) infstr += " ";
    				addSpace = true;
    				infstr += sv.parameters[i];
    			}
    			printWriter.println(infstr);
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
		Sequence next;			/* next vector in linked list */
		int      which;			/* 0 => node; 1 => vector */
	//	union {
		Sim.Node n;
		Bits     b;
	//	} ptr;					/* pointer to node/vector */
		int      vSize;			/* size of each value */
		int      nValues;		/* number of values specified */
		char  [] values;		/* array of values */
	};

	private Bits bList = null;				/* list of vectors */

	private Sequence sList = null;				/* list of sequences */
	private int		maxSequence = 0;			/* longest sequence defined */

	private Sequence xClock = null;				/* vectors which make up clock */
	private int		maxClock = 0;				/* longest clock sequence defined */

	private int		column = 0;					/* current output column */

	private final String potChars = "luxh.";			/* set of potential characters */

	private List  [] listTbl = new List[8];

	public boolean  analyzerON = false;	/* set when analyzer is running */

	public List  hInputs = new ArrayList();	/* list of nodes to be driven high */
	public List  lIinputs = new ArrayList();	/* list of nodes to be driven low */
	public List  uInputs = new ArrayList();	/* list of nodes to be driven X */
	public List  xInputs = new ArrayList();	/* list of nodes to be removed from input */

	private void initRSim()
	{
		bList = null;
		sList = null;
		maxSequence = 0;
		xClock = null;
		maxClock = 0;
		column = 0;
		analyzerON = false;
		for(int i = 0; i < 8; i++) listTbl[i] = null;
		listTbl[Sim.inputNumber(Sim.H_INPUT)] = hInputs;
		listTbl[Sim.inputNumber(Sim.L_INPUT)] = lIinputs;
		listTbl[Sim.inputNumber(Sim.U_INPUT)] = uInputs;
		listTbl[Sim.inputNumber(Sim.X_INPUT)] = xInputs;
	}

	private void issueCommand(String command)
	{
		if (Simulation.isIRSIMShowsCommands()) System.out.println("> " + command);
		String [] strings = Sim.parseLine(command, true);
		if (strings.length > 0) execCmd(strings);
	}

	/**
	 * Execute a builtin command or read commands from a '.cmd' file.
	 */
	private void execCmd(String [] args)
	{
		// search command table, dispatch to handler, if any
		String cmdName = args[0];
		if (cmdName.equals("!")) { quest(args);   return; }
		if (cmdName.equals("?")) { quest(args);   return; }
		if (cmdName.equals("activity")) { doActivity(args);   return; }
		if (cmdName.equals("alias")) { doPrintAlias(args);   return; }
		if (cmdName.equals("assert")) { doAssert(args);   return; }
		if (cmdName.equals("assertWhen")) { doAssertWhen(args);   return; }
		if (cmdName.equals("back")) { backTime(args);   return; }
		if (cmdName.equals("c")) { doClock(args);   return; }
		if (cmdName.equals("changes")) { doChanges(args);   return; }
		if (cmdName.equals("clock")) { setClock(args);   return; }
		if (cmdName.equals("debug")) { setDbg(args);   return; }
		if (cmdName.equals("decay")) { setDecay(args);   return; }
		if (cmdName.equals("h")) { setValue(args);   return; }
		if (cmdName.equals("l")) { setValue(args);   return; }
		if (cmdName.equals("u")) { setValue(args);   return; }
		if (cmdName.equals("x")) { setValue(args);   return; }
		if (cmdName.equals("inputs")) { inputs(args);   return; }
		if (cmdName.equals("p")) { doPhase(args);   return; }
		if (cmdName.equals("path")) { doPath(args);   return; }
		if (cmdName.equals("print")) { doMsg(args);   return; }
		if (cmdName.equals("printx")) { doPrintX(args);   return; }
		if (cmdName.equals("R")) { runSeq(args);   return; }
		if (cmdName.equals("report")) { setReport(args);   return; }
		if (cmdName.equals("s")) { doStep(args);   return; }
		if (cmdName.equals("set")) { setVector(args);   return; }
		if (cmdName.equals("stats")) { doStats(args);   return; }
		if (cmdName.equals("stepsize")) { setStep(args);   return; }
		if (cmdName.equals("stop")) { setStop(args);   return; }
		if (cmdName.equals("t")) { setTrace(args);   return; }
		if (cmdName.equals("tcap")) { printTCap(args);   return; }
		if (cmdName.equals("unitdelay")) { setUnit(args);   return; }
		if (cmdName.equals("until")) { doUntil(args);   return; }
		if (cmdName.equals("w")) { display(args);   return; }

		System.out.println("unrecognized command: " + cmdName);
	}


	static final int SETIN_CALL      = 1;
	static final int SETTRACE_CALL   = 2;
	static final int SETSTOP_CALL    = 3;
	static final int QUEST_CALL      = 4;
	static final int PATH_CALL       = 5;
	static final int FINDONE_CALL    = 6;
	static final int ASSERTWHEN_CALL = 7;
	static final int WATCH_CALL      = 8;

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
	private Object apply(int fun, int vFun, String[] args, int applyStart, int applyEnd)
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
			boolean pos = true;
			if (args[applyStart].equals("-"))
			{
				pos = false;
				applyStart++;
			}

			int found = 0;
//			if (wildCard[i])
//			{
//				for(Bits b = bList; b != null; b = b.next)
//					if (strMatch(p, b.name))
//				{
//					if (vFun != null)
//						((int(*)(Bits,CHAR*))(*vFun))(b, flag);
//					else
//						for(j = 0; j < b.nBits; j += 1)
//							((int(*)(Sim.Node,CHAR*))(*fun))(b.nodes[j], flag);
//					found = 1;
//				}
//				found += matchNet(p, (int(*)(Sim.Node, CHAR*))fun, flag);
//			} else
			{
				Sim.Node n = theSim.findNode(p);

				if (n != null)
				{
					switch (fun)
					{
						case SETIN_CALL:
							found += setIn(n, args[0]);
							break;
						case SETTRACE_CALL:
							found += xTrace(n, pos);
							break;
						case SETSTOP_CALL:
							found += nStop(n, pos);
							break;
						case QUEST_CALL:
							found += getInfo(n, args[0]);
							break;
						case PATH_CALL:
							found += doCPath(n);
							break;
						case FINDONE_CALL:
							f.node = n;
							f.num++;
							found = 1;
							break;
						case ASSERTWHEN_CALL:
							setupAssertWhen(n, args[0]);
							break;
						case WATCH_CALL:
							found += xWatch(n, pos);
							break;
					}
				} else
				{
					for(Bits b = bList; b != null; b = b.next)
						if (p.equalsIgnoreCase(b.name))
					{
						switch (fun)
						{
							case SETTRACE_CALL:
								vTrace(b, pos);
								break;
							case SETSTOP_CALL:
								found += vStop(b, pos);
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
	private int setIn(Sim.Node n, String which)
	{
		while((n.nFlags & Sim.ALIAS) != 0)
			n = n.nLink;

		char wChar = which.charAt(0);
		if ((n.nFlags & (Sim.POWER_RAIL | Sim.MERGED)) != 0)	// Gnd, Vdd, or merged node
		{
			String pots = "lxuh";
			if ((n.nFlags & Sim.MERGED) != 0 || pots.charAt(n.nPot) != wChar)
				System.out.println("Can't drive `" + n.nName + "' to `" + wChar + "'");
		} else
		{
			List list = listTbl[Sim.inputNumber((int)n.nFlags)];

			switch (wChar)
			{
				case 'h':
					if (list != null && list != hInputs)
					{
						n.nFlags = n.nFlags & ~Sim.INPUT_MASK;
						list.remove(n);
					}
					if (! (list == hInputs || wasInP(n, Sim.HIGH)))
					{
						n.nFlags = (n.nFlags & ~Sim.INPUT_MASK) | Sim.H_INPUT;
						hInputs.add(n);
					}
					break;

				case 'l':
					if (list != null && list != lIinputs)
					{
						n.nFlags = n.nFlags & ~Sim.INPUT_MASK;
						list.remove(n);
					}
					if (! (list == lIinputs || wasInP(n, Sim.LOW)))
					{
						n.nFlags = (n.nFlags & ~Sim.INPUT_MASK) | Sim.L_INPUT;
						lIinputs.add(n);
					}
					break;

				case 'u':
					if (list != null && list != uInputs)
					{
						n.nFlags = n.nFlags & ~Sim.INPUT_MASK;
						list.remove(n);
					}
					if (! (list == uInputs || wasInP(n, Sim.X)))
					{
						n.nFlags = (n.nFlags & ~Sim.INPUT_MASK) | Sim.U_INPUT;
						uInputs.add(n);
					}
					break;

				case 'x':
					if (list == xInputs)
						break;
					if (list != null)
					{
						n.nFlags = n.nFlags & ~Sim.INPUT_MASK;
						list.remove(n);
					}
					if ((n.nFlags & Sim.INPUT) != 0)
					{
						n.nFlags = (n.nFlags & ~Sim.INPUT_MASK) | Sim.X_INPUT;
						xInputs.add(n);
					}
					break;

				default:
					return 0;
			}
		}
		return 1;
	}

	private boolean wasInP(Sim.Node n, int p)
	{
		return (n.nFlags & Sim.INPUT) != 0 && n.nPot == p;
	}

	private int getInfo(Sim.Node n, String which)
	{
		if (n == null) return 0;

		String name = n.nName;
		while((n.nFlags & Sim.ALIAS) != 0)
			n = n.nLink;

		if ((n.nFlags & Sim.MERGED) != 0)
		{
			System.out.println(name + " => node is inside a transistor stack");
			return 1;
		}

		String infstr = "";
		infstr += pValue(name, n);
		if ((n.nFlags & Sim.INPUT) != 0)
			infstr += "[NOTE: node is an input] ";
		infstr += "(vl=" + n.vLow + " vh=" + n.vHigh + ") ";
		if ((n.nFlags & Sim.USERDELAY) != 0)
			infstr += "(tpLH=" + n.tpLH + ", tpHL=" + n.tpHL + ") ";
		infstr += "(" + n.nCap + " pf) ";
		System.out.println(infstr);

		infstr = "";
		if (which.startsWith("?"))
		{
			infstr += "is computed from:";
			for(Iterator it = n.nTermList.iterator(); it.hasNext(); )
			{
				Sim.Trans t = (Sim.Trans)it.next();
				infstr += "  ";
				if (theSim.irDebug == 0)
				{
					String drive = null;
					Sim.Node rail = (t.drain.nFlags & Sim.POWER_RAIL) != 0 ? t.drain : t.source;
					if (Sim.baseType(t.tType) == Sim.NCHAN && rail == theSim.groundNode)
						drive = "pulled down by ";
					else if (Sim.baseType(t.tType) == Sim.PCHAN && rail == theSim.powerNode)
						drive = "pulled up by ";
					else if (Sim.baseType(t.tType) == Sim.DEP && rail == theSim.powerNode &&
						Sim.otherNode(t, rail) == t.gate)
							drive = "pullup ";
					else
						infstr += pTrans(t);
	
					if (drive != null)
					{
						infstr += drive;
						infstr += pGValue(t);
						infstr += prTRes(t.r);
						if (t.tLink != t && (theSim.tReport & Sim.REPORT_TCOORD) != 0)
							infstr += " <" + t.x + "," + t.y + ">";
					}
				} else
				{
					infstr += pTrans(t);
				}
			}
		} else
		{
			infstr += "affects:";
			for(Iterator it = n.nGateList.iterator(); it.hasNext(); )
			{
				Sim.Trans t = (Sim.Trans)it.next();
				infstr += pTrans(t);
			}
		}
		System.out.println(infstr);

		if (n.events != null)
		{
			System.out.println("Pending events:");
			for(Eval.Event e = n.events; e != null; e = e.nLink)
				System.out.println("   transition to " + Sim.vChars.charAt(e.eval) + " at " + Sim.deltaToNS(e.nTime)+ "ns");
		}

		return 1;
	}

	private String pValue(String node_name, Sim.Node node)
	{
		char pot = 0;
		switch (node.nPot)
		{
			case 0: pot = '0';   break;
			case 1: pot = 'X';   break;
			case 2: pot = 'X';   break;
			case 3: pot = '1';   break;
		}
	    return node_name + "=" + pot + " ";
	}

	private String pGValue(Sim.Trans t)
	{
		String infstr = "";
		if (theSim.irDebug != 0)
			infstr += "[" + Sim.states[t.state] + "] ";
		if ((t.tType & Sim.GATELIST) != 0)
		{
			infstr += "(";
			for(t = (Sim.Trans) t.gate; t != null; t = t.getSTrans())
			{
				Sim.Node n = (Sim.Node)t.gate;
				infstr += pValue(n.nName, n);
			}

			infstr += ") ";
		} else
		{
			Sim.Node n = (Sim.Node)t.gate;
			infstr += pValue(n.nName, n);
		}
		return infstr;
	}

	private String prOneRes(double r)
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

	private String prTRes(Sim.Resists r)
	{
		String v1 = prOneRes(r.rStatic);
		String v2 = prOneRes(r.dynRes[Sim.R_HIGH]);
		String v3 = prOneRes(r.dynRes[Sim.R_LOW]);
		return "[" + v1 + ", " + v2 + ", " + v3 + "]";
	}

	private String pTrans(Sim.Trans t)
	{
		String infstr = Sim.transistorType[Sim.baseType(t.tType)] + " ";
		if (Sim.baseType(t.tType) != Sim.RESIST)
			infstr += pGValue(t);

		infstr += pValue(t.source.nName, t.source);
		infstr += pValue(t.drain.nName, t.drain);
		infstr += prTRes(t.r);
		if (t.tLink != t && (theSim.tReport & Sim.REPORT_TCOORD) != 0)
			infstr += " <" + t.x + "," + t.y + ">";
		return infstr;
	}

//	/* visit each node in network, calling function passed as arg with any node
//	 * whose name matches pattern
//	 */
//	private int matchNet(CHAR *pattern, int (*fun)(Sim.Node, CHAR*), CHAR *arg)
//	{
//		int   index;
//		Sim.Node  n;
//		int            total = 0;
//
//		for(index = 0; index < HASHSIZE; index++)
//			for(n = hash[index]; n; n = n.hnext)
//				if (strMatch(pattern, n.nName))
//					total += (*fun)(n, arg);
//
//		return total;
//	}

	/**
	 * compare pattern with string, case doesn't matter.  "*" wildcard accepted
	 */
	private boolean strMatch(String pStr, String sStr)
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
					if (strMatch(pStr.substring(p+1), sStr.substring(s)))
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
	private int chToPot(char ch)
	{
		String s = "0ux1lUXhLUXH";
		for(int i = 0; i < s.length(); i++)
			if (s.charAt(i) == ch)
				return i & (Sim.N_POTS - 1);

		System.out.println(ch + ": unknown node value");
		return Sim.N_POTS;
	}

	private Sim.Node unAlias(Sim.Node n)
	{
		while ((n.nFlags & Sim.ALIAS) != 0) n = n.nLink;
		return n;
	}

	/**
	 * Set value of a node/vector to the requested value (hlux).
	 */
	private void setValue(String [] args)
	{
		apply(SETIN_CALL, 0, args, 1, args.length);
	}

	/**
	 * Set watch of a node/vector.
	 */
	private void display(String [] args)
	{
		apply(WATCH_CALL, 0, args, 1, args.length);
	}

	/**
	 * display bit vector.
	 */
	private void dVec(Bits b)
	{
		int i = b.name.length() + 2 + b.nBits;
		if (column + i >= MAXCOL)
		{
			column = 0;
		}
		column += i;
		String bits = "";
		for(i = 0; i < b.nBits; i++)
			bits += Sim.vChars.charAt(b.nodes[i].nPot);

		System.out.println(b.name + "=" + bits + " ");
	}

	/**
	 * display node/vector values in display list
	 */
	private void pnWatchList()
	{
		theSim.getModel().printPendingEvents();
	}

	/**
	 * set/clear trace bit in node
	 */
	private int xTrace(Sim.Node n, boolean flag)
	{
		n = unAlias(n);

		if ((n.nFlags & Sim.MERGED) != 0)
		{
			System.out.println("can't trace " + n.nName);
			return 1;
		}

		if (flag)
			n.nFlags |= Sim.WATCHED;
		else if ((n.nFlags & Sim.WATCHED) != 0)
		{
			System.out.println(n.nName + " was watched; not any more");
			n.nFlags &= ~Sim.WATCHED;
		}

		return 1;
	}

	/**
	 * add/delete node to/from display list.
	 */
	private int xWatch(Sim.Node n, boolean flag)
	{
//		unAlias(n);
//
//		if ((n.nFlags & Sim.MERGED) == 0)
//		{
//			if (flag)
//				iinsert_once(n, &wlist);
//			else
//				wlist.remove(n);
//		}
		return 1;
	}

	/**
	 * set/clear trace bit in vector
	 */
	private void vTrace(Bits b, boolean flag)
	{
		if (flag)
			b.traced |= Sim.WATCHVECTOR;
		else
		{
			for(int i = 0; i < b.nBits; i += 1)
				b.nodes[i].nFlags &= ~Sim.WATCHVECTOR;
			b.traced &= ~Sim.WATCHVECTOR;
		}
	}

	/**
	 * just in case node appears in more than one bit vector, run through all
	 * the vectors being traced and make sure the flag is set for each node.
	 */
	private void setVecNodes(int flag)
	{
		for(Bits b = bList; b != null; b = b.next)
			if ((b.traced & flag) != 0)
				for(int i = 0; i < b.nBits; i += 1)
					b.nodes[i].nFlags |= flag;
	}

	/**
	 * set/clear stop bit in node
	 */
	private int nStop(Sim.Node n, boolean flag)
	{
		n = unAlias(n);

		if ((n.nFlags & Sim.MERGED) != 0)
			return 1;

		if (flag)
			n.nFlags &= ~Sim.STOPONCHANGE;
		else
			n.nFlags |= Sim.STOPONCHANGE;
		return 1;
	}

	/**
	 * set/clear stop bit in vector
	 */
	private int vStop(Bits b, boolean flag)
	{
		if (flag)
			b.traced |= Sim.STOPVECCHANGE;
		else
		{
			for(int i = 0; i < b.nBits; i += 1)
				b.nodes[i].nFlags &= ~Sim.STOPVECCHANGE;
			b.traced &= ~Sim.STOPVECCHANGE;
		}
		return 1;
	}

	/**
	 * mark nodes and vectors for tracing
	 */
	private void setTrace(String [] args)
	{
		apply(SETTRACE_CALL, 0, args, 1, args.length);
		setVecNodes(Sim.WATCHVECTOR);
	}

	/**
	 * mark nodes and vectors for stoping
	 */
	private void setStop(String [] args)
	{
		apply(SETSTOP_CALL, 0, args, 1, args.length);
		setVecNodes(Sim.STOPVECCHANGE);
	}

	/**
	 * set bit vector
	 */
	private void setVector(String [] args)
	{
//		CHAR           *val = targv[2];
//
//		// find vector
//		boolean found = false;
//		Bits b;
//		for(b = bList; b != null; b = b.next)
//		{
//			if (b.name.equalsIgnoreCase(args[1]))
//			{
//				found = true;
//				break;
//			}
//		}
//		if (!found)
//		{
//			Sim.reportError(filename, lineno, args[1] + ": No such vector");
//			return 0;
//		}
//
//		// set nodes
//		if (args[2].length() != b.nBits)
//		{
//			Sim.reportError(filename, lineno, "wrong number of bits for this vector");
//			return 0;
//		}
//		for(int i = 0; i < b.nBits; i++)
//		{
//			if ((val[i] = potChars.charAt(chToPot(val[i]))) == '.')
//				return 0;
//		}
//		for(int i = 0; i < b.nBits; i++)
//			setIn(b.nodes[i], val++);
	}


	private int compareVector(Sim.Node [] np, String name, int nBits, String mask, String value)
	{
		if (value.length() != nBits)
		{
			System.out.println("wrong number of bits for value");
			return 0;
		}
		if (mask != null && mask.length() != nBits)
		{
			System.out.println("wrong number of bits for mask");
			return 0;
		}

		for(int i = 0; i < nBits; i++)
		{
			if (mask != null && mask.charAt(i) != '0') continue;
			Sim.Node n = np[i];
			int val = chToPot(value.charAt(i));
			if (val >= Sim.N_POTS)
				return 0;
			if (val == Sim.X_X) val = Sim.X;
				if (n.nPot != val)
					return 1;
		}
		return 0;
	}

	private static class Find1Arg
	{
		Sim.Node node;
		Bits     vec;
		int      num;
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

		int comp = 0, nBits = 0;
		String name = null;
		Sim.Node [] nodes = null;
		if (f.num > 1)
			System.out.println(args[1] + " matches more than one node or vector");
		else if (f.node != null)
		{
			name = f.node.nName;
			f.node = unAlias(f.node);
			Sim.Node [] nodeList = new Sim.Node[1];
			nodeList[0] = f.node;
			comp = compareVector(nodeList, name, 1, mask, value.toString());
			nodes = nodeList;
			nBits = 1;
		}
		else if (f.vec != null)
		{
			comp = compareVector(f.vec.nodes, f.vec.name, f.vec.nBits, mask, value.toString());
			name = f.vec.name;
			nBits = f.vec.nBits;
			nodes = f.vec.nodes;
		}
		if (comp != 0)
		{
			String infstr = "";
			for(int i = 0; i < nBits; i++)
			{
				if (mask != null && i < mask.length() && mask.charAt(i) != '0')
				{
					infstr += "-";
					value.setCharAt(i, '-');
				}
				else
					infstr += Sim.vChars.charAt(nodes[i].nPot);
			}
			System.out.println("Assertion failed on '" + name + "': want (" + value + ") but got (" + infstr + ")");
		}
	}

	private void doUntil(String [] args)
	{
		String mask = null;
		StringBuffer value = null;
		int cCount = 0;
		if (args.length == 5)
		{
			mask = args[2];
			value = new StringBuffer(args[3]);
			cCount = TextUtils.atoi(args[4]);
		} else
		{
			mask = null;
			value = new StringBuffer(args[2]);
			cCount = TextUtils.atoi(args[3]);
		}

		Find1Arg f = (Find1Arg)apply(FINDONE_CALL, 0, args, 1, args.length);
		String name = null;
		int comp = 0;
		int nBits = 0;
		Sim.Node [] nodes = null;
		if (f.num > 1)
			System.out.println(args[1] + " matches more than one node or vector");
		else if (f.node != null)
		{
			name = f.node.nName;
			f.node = unAlias(f.node);
			Sim.Node [] nodeList = new Sim.Node[1];
			nodeList[0] = f.node;
			int cnt = 0;
			while ((cnt <= cCount) && (comp = compareVector(nodeList, name, 1, mask, value.toString())) != 0)
			{
				cnt++;
				clockIt(1);
			}
			nodes = new Sim.Node[1];
			nodes[0] = f.node;
			nBits = 1;
		}
		else if (f.vec != null)
		{
			int cnt = 0;
			while ((cnt <= cCount) && (comp = compareVector(f.vec.nodes, f.vec.name, f.vec.nBits, mask,
				value.toString())) != 0)
			{
				cnt++;
				clockIt(1);
			}
			name = f.vec.name;
			nBits = f.vec.nBits;
			nodes = f.vec.nodes;
		}
		if (comp != 0)
		{
			String infstr = "";
			for(int i = 0; i < nBits; i++)
			{
				if (mask != null && mask.charAt(i) != '0')
				{
					infstr += "-";
					value.setCharAt(i, '-');
				}
				else
					infstr += Sim.vChars.charAt(nodes[i].nPot);
			}
			System.out.println("Assertion failed on '" + name + ": want (" + value + ") but got (" + infstr + ")");
		}
	}

	private Sim.Node awTrig; /* keeps current AssertWhen trigger */
	private AssertWhen awP;   /* track pointer on the current AssertWhen list */

	private void setupAssertWhen(Sim.Node n, String val)
	{
		AssertWhen p = new AssertWhen();
		p.node = n;
		p.val  = val.charAt(0);
		p.nxt = null;

		if (awTrig.awPending == null)
		{
			// first time
			awTrig.awPending = p;
			awP = p;
		} else
		{
			// more than 1 matching nodes
			awP.nxt = p;
			awP = p;
		}
	}

	private void doAssertWhen(String [] args)
	{
		Find1Arg trig = (Find1Arg)apply(FINDONE_CALL, 0, args, 1, args.length);

		if (trig.num > 1)
			System.out.println(args[1] + " matches more than one node or vector");
		else if (trig.node != null)
		{
			trig.node = unAlias(trig.node);
			awTrig = trig.node;
			awTrig.awPot = (short)chToPot(args[2].charAt(0));
			apply(ASSERTWHEN_CALL, 0, args, 3, args.length);
		}
		else if (trig.vec != null)
			System.out.println("trigger to assertWhen " + args[1] + " can't be a vector");
	}

	public void evalAssertWhen(Sim.Node n)
	{
		for (AssertWhen p = n.awPending; p != null; )
		{
			String name = p.node.nName;
			StringBuffer sb = new StringBuffer();
			sb.append((char)p.val);
			Sim.Node [] nodes = new Sim.Node[1];
			nodes[0] = p.node;
			int comp = compareVector(nodes, name, 1, null, sb.toString());
			if (comp != 0)
				System.out.println("Assertion failed on '" + name + "'");
			p = p.nxt;
		}
		n.awPending = null;
	}

	private void collectInputs(Sim.Node n, Sim.Node [] inps)
	{
		if ((n.nFlags & (Sim.INPUT|Sim.ALIAS|Sim.POWER_RAIL|Sim.VISITED|Sim.INPUT_MASK)) == Sim.INPUT)
		{
			n.setNext(inps[n.nPot]);
			inps[n.nPot] = n;
			n.nFlags |= Sim.VISITED;
		}
	}

	/* display current inputs */
	private void inputs(String [] args)
	{
		Sim.Node [] inpTbl = new Sim.Node[Sim.N_POTS];

		inpTbl[Sim.HIGH] = inpTbl[Sim.LOW] = inpTbl[Sim.X] = null;
		for(Iterator it = theSim.getNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			collectInputs(n, inpTbl);
		}

		System.out.print("h inputs:");
		for(Iterator it = hInputs.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			System.out.print(" " + n.nName);
		}
		for(Sim.Node n = inpTbl[Sim.HIGH]; n != null; n.nFlags &= ~Sim.VISITED, n = n.getNext())
			System.out.print(" " + n.nName);
		System.out.println();

		System.out.print("l inputs:");
		for(Iterator it = lIinputs.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			System.out.print(" " + n.nName);
		}
		for(Sim.Node n = inpTbl[Sim.LOW]; n != null; n.nFlags &= ~Sim.VISITED, n = n.getNext())
			System.out.print(" " + n.nName);
		System.out.println();

		System.out.println("u inputs:");
		for(Iterator it = uInputs.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			System.out.println(" " + n.nName);
		}
		for(Sim.Node n = inpTbl[Sim.X]; n != null; n.nFlags &= ~Sim.VISITED, n = n.getNext())
			System.out.println(" " + n.nName);
		System.out.println();
	}

	/**
	 * set stepsize
	 */
	private void setStep(String [] args)
	{
		if (args.length == 1)
			System.out.println("stepsize = " + Sim.deltaToNS(stepSize));
		else if (args.length == 2)
		{
			double timeNS = TextUtils.atof(args[1]);
			long newSize = Sim.nsToDelta(timeNS);

			if (newSize <= 0)
			{
				System.out.println("Bad step size: " + TextUtils.formatDouble(timeNS*1000) + "psec (must be 10 psec or larger)");
			} else
				stepSize = newSize;
		}
	}

	/**
	 * Display traced vectors that just changed.  There should be at least one.
	 */
	public void dispWatchVec(long which)
	{
		which &= (Sim.WATCHVECTOR | Sim.STOPVECCHANGE);
		String temp = " @ " + Sim.deltaToNS(theSim.curDelta) + "ns ";
		System.out.println(temp);
		column = temp.length();
		for(Bits b = bList; b != null; b = b.next)
		{
			if ((b.traced & which) == 0)
				continue;
			int i;
			for(i = b.nBits - 1; i >= 0; i--)
				if (b.nodes[i].getTime() == theSim.curDelta)
					break;
			if (i >= 0)
				dVec(b);
		}
	}

	/**
	 * Settle network until the specified stop time is reached.
	 * Premature returns (before stop time) indicate that a node/vector whose
	 * stop-bit set has just changed value.
	 */
	private long relax(long stopTime)
	{
		while (theSim.getModel().step(stopTime)) ;

		return theSim.curDelta - stopTime;
	}

	/**
	 * relax network, optionally set stepsize
	 */
	private void doStep(String [] args)
	{
		long newSize = stepSize;
		if (args.length == 2)
		{
			double timeNS = TextUtils.atof(args[1]);
			newSize = Sim.nsToDelta(timeNS);
			if (newSize <= 0)
			{
				System.out.println("Bad step size: " + TextUtils.formatDouble(timeNS*1000) + "psec (must be 10 psec or larger)");
				return;
			}
		}

		relax(theSim.curDelta + newSize);
		pnWatchList();
	}

	/**
	 * destroy sequence for given node/vector: update sequence list and length.
	 * return -1 if we can't destroy the sequence (in stopped state).
	 */
	private Sequence undefSeq(Object p, Sequence list, GenMath.MutableInteger lmax)
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
				if (t.nValues > i) i = t.nValues;
					lmax.setValue(i);
		}
		return list;
	}

	/**
	 * process command line to yield a sequence structure.  first arg is the
	 * name of the node/vector for which the sequence is to be defined, second
	 * and following args are the values.
	 */
	private Sequence defSequence(String [] args, Sequence list, GenMath.MutableInteger lMax)
	{
		// if no arguments, get rid of all the sequences we have defined
		if (args.length == 1)
		{
			while (list != null)
				list = undefSeq(list.n, list, lMax);
			return list;
		}

		// see if we can determine if name is for node or vector
		boolean isOK = false;
		int which = 0, size = 0;
		Bits b = null;
		Sim.Node n = null;
		for(b = bList; b != null; b = b.next)
			if (b.name.equalsIgnoreCase(args[1]))
		{
			which = 1;    size = b.nBits;    isOK = true;   break;
		}
		if (!isOK)
		{
			n = theSim.findNode(args[1]);
			if (n == null)
			{
				System.out.println(args[0] + ": No such node or vector");
				return list;
			}
			n = unAlias(n);
			if ((n.nFlags & Sim.MERGED) != 0)
			{
				System.out.println(n.nName + " can't be part of a sequence");
				return list;
			}
			which = 0; size = 1;
		}

		if (args.length == 2)	// just destroy the given sequence
		{
            Object objB = b;
            Object objN = n;
			list = undefSeq((which != 0) ? objB : objN, list, lMax);
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
		s.vSize = size;
		s.nValues = args.length - 2;
		if (which != 0)	s.b = b;
			else s.n = n;

		// process each value specification saving results in sequence
		int q = 0;
		for(int i = 2; i < args.length; i += 1)
		{
			for(int p=0; p<args[i].length(); p++)
				if ((s.values[q++] = potChars.charAt(chToPot(args[i].charAt(p)))) == '.')
			{
				return list;
			}
		}

		// all done!  remove any old sequences for this node or vector.
		list = undefSeq(s.n, list, lMax);

		// insert result onto list
		s.next = list;
		list = s;
		if (s.nValues > lMax.intValue())
			lMax.setValue(s.nValues);
		return list;
	}

	/**
	 * set each node/vector in sequence list to its next value
	 */
	private void vecValue(Sequence list, int index)
	{
		for(; list != null; list = list.next)
		{
			int offset = list.vSize * (index % list.nValues);
//			Sim.Node n = (list.which == 0) ? list.n : list.b.nodes;
//			for(int i = 0; i < list.vSize; i++)
//				setIn(*n++, &list.values[offset++]);
		}
	}

	/**
	 * define clock sequences(s)
	 */
	private void setClock(String [] args)
	{
		// process sequence and add to clock list
		GenMath.MutableInteger mi = new GenMath.MutableInteger(maxClock);
		xClock = defSequence(args, xClock, mi);
		maxClock = mi.intValue();
	}

	private int whichPhase = 0;
	/**
	 * Step each clock node through one simulation step
	 */
	private int stepPhase()
	{
		vecValue(xClock, whichPhase);
		if (relax(theSim.curDelta + stepSize) != 0)
			return 1;
		whichPhase = (whichPhase + 1) % maxClock;
		return 0;
	}

	/* Do one simulation step */
	private void doPhase(String [] args)
	{
		stepPhase();
		pnWatchList();
	}

	/**
	 * clock circuit specified number of times
	 */
	private int clockIt(int n)
	{
		int  i = 0;

		if (xClock == null)
		{
			System.out.println("no clock nodes defined!");
		} else
		{
			/* run 'em by setting each clock node to successive values of its
			 * associated sequence until all phases have been run.
			 */
			while (n-- > 0)
			{
				for(i = 0; i < maxClock; i += 1)
				{
					if (stepPhase() != 0)
					{
						n = 0;
						break;
					}
				}
			}

			// finally display results if requested to do so
			pnWatchList();
		}
		return maxClock - i;
	}

	/**
	 * clock circuit through all the input vectors previously set up
	 */
	private void runSeq(String [] args)
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
		if (sList == null)
		{
			System.out.println("no input vectors defined!");
		} else
			while (n-- > 0)
				for(int i = 0; i < maxSequence; i += 1)
		{
			vecValue(sList, i);
			if (clockIt(1) != 0)
				return;
			pnWatchList();
		}
	}

	/**
	 * process "c" command line
	 */
	private void doClock(String [] args)
	{
		// calculate how many clock cycles to run
		int n = 1;
		if (args.length == 2)
		{
			n = TextUtils.atoi(args[1]);
			if (n <= 0)
				n = 1;
		}

		clockIt(n);		// do the hard work
	}

	/**
	 * output message to console/log file
	 */
	private void doMsg(String [] args)
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
	private void setDecay(String [] args)
	{
		if (args.length == 1)
		{
			if (theSim.tDecay == 0)
				System.out.println("decay = No decay");
			else
				System.out.println("decay = " + Sim.deltaToNS(theSim.tDecay) + "ns");
		} else
		{
			theSim.tDecay = Sim.nsToDelta(TextUtils.atof(args[1]));
			if (theSim.tDecay < 0)
				theSim.tDecay = 0;
		}
	}

	private void setDbg(String [] args)
	{
        if (args.length == 1) {
            theSim.irDebug = 0;
        } else {
            if (args[1].equalsIgnoreCase("ev")) theSim.irDebug |= Sim.DEBUG_EV; else
                if (args[1].equalsIgnoreCase("dc")) theSim.irDebug |= Sim.DEBUG_DC; else
                    if (args[1].equalsIgnoreCase("tau")) theSim.irDebug |= Sim.DEBUG_TAU; else
                        if (args[1].equalsIgnoreCase("taup")) theSim.irDebug |= Sim.DEBUG_TAUP; else
                            if (args[1].equalsIgnoreCase("spk")) theSim.irDebug |= Sim.DEBUG_SPK; else
                                if (args[1].equalsIgnoreCase("tw")) theSim.irDebug |= Sim.DEBUG_TW; else
                                    if (args[1].equalsIgnoreCase("off")) theSim.irDebug = 0;
        }
		System.out.print("Debugging");
		if (theSim.irDebug == 0) System.out.println(" OFF"); else
		{
			if ((theSim.irDebug&Sim.DEBUG_EV) != 0) System.out.print(" event-scheduling");
			if ((theSim.irDebug&Sim.DEBUG_DC) != 0) System.out.print(" final-value-computation");
			if ((theSim.irDebug&Sim.DEBUG_TAU) != 0) System.out.print(" tau/delay-computation");
			if ((theSim.irDebug&Sim.DEBUG_TAUP) != 0) System.out.print(" tauP-computation");
			if ((theSim.irDebug&Sim.DEBUG_SPK) != 0) System.out.print(" spike-analysis");
			if ((theSim.irDebug&Sim.DEBUG_TW) != 0) System.out.print(" tree-walk");
			System.out.println();
		}
	}

	/**
	 * set tReport parameter
	 */
	private void setReport(String [] args)
	{
		if (args[1].equalsIgnoreCase("decay")) theSim.tReport |= Sim.REPORT_DECAY; else
			if (args[1].equalsIgnoreCase("delay")) theSim.tReport |= Sim.REPORT_DELAY; else
				if (args[1].equalsIgnoreCase("tau")) theSim.tReport |= Sim.REPORT_TAU; else
					if (args[1].equalsIgnoreCase("tcoord")) theSim.tReport |= Sim.REPORT_TCOORD; else
						if (args[1].equalsIgnoreCase("none")) theSim.tReport = 0;
		System.out.print("Report");
		if (theSim.tReport == 0) System.out.println(" NONE"); else
		{
			if ((theSim.tReport&Sim.REPORT_DECAY) != 0) System.out.print(" decay");
			if ((theSim.tReport&Sim.REPORT_DELAY) != 0) System.out.print(" delay");
			if ((theSim.tReport&Sim.REPORT_TAU) != 0) System.out.print(" tau");
			if ((theSim.tReport&Sim.DEBUG_TAUP) != 0) System.out.print(" tauP");
			if ((theSim.tReport&Sim.REPORT_TCOORD) != 0) System.out.print(" tcoord");
			System.out.println();
		}
	}

	/**
	 * set unitdelay parameter
	 */
	private void setUnit(String [] args)
	{
		if (args.length == 1)
		{
			if (theSim.tUnitDelay == 0)
				System.out.println("unitdelay = OFF");
			else
				System.out.println("unitdelay = " + Sim.deltaToNS(theSim.tUnitDelay));
		} else
		{
			theSim.tUnitDelay = (int) Sim.nsToDelta(TextUtils.atof(args[1]));
			if (theSim.tUnitDelay < 0)
				theSim.tUnitDelay = 0;
		}
	}

	static long pTime;
	/**
	 * print traceback of node's activity and that of its ancestors
	 */
	private void cPath(Sim.Node n, int level)
	{
		// no last transition!
		if ((n.nFlags & Sim.MERGED) != 0 || n.getCause() == null)
		{
			System.out.println("  there is no previous transition!");
		}

		/* here if we come across a node which has changed more recently than
		 * the time reached during the backtrace.  We can't continue the
		 * backtrace in any reasonable fashion, so we stop here.
		 */
		else if (level != 0 && n.getTime() > pTime)
		{
			System.out.println("  transition of " + n.nName + ", which has since changed again");
		}
		/* here if there seems to be a cause for this node's transition.
		 * If the node appears to have 'caused' its own transition (n.t.cause
		 * == n), that means it was input.  Otherwise continue backtrace...
		 */
		else if (n.getCause() == n)
		{
			System.out.println("  " + n.nName + " . " + Sim.vChars.charAt(n.nPot) +
				" @ " + Sim.deltaToNS(n.getTime()) + "ns , node was an input");
		}
		else if ((n.getCause().nFlags & Sim.VISITED) != 0)
		{
			System.out.println("  ... loop in traceback");
		}
		else
		{
			long deltaT = n.getTime() - n.getCause().getTime();

			n.nFlags |= Sim.VISITED;
			pTime = n.getTime();
			cPath(n.getCause(), level + 1);
			n.nFlags &= ~Sim.VISITED;
			if (deltaT < 0)
				System.out.println("  " + n.nName + " . " + Sim.vChars.charAt(n.nPot) +
					" @ " + Sim.deltaToNS(n.getTime()) + "ns   (??)");
			else
				System.out.println("  " + n.nName + " . " + Sim.vChars.charAt(n.nPot) +
					" @ " + Sim.deltaToNS(n.getTime()) + "ns   (" + Sim.deltaToNS(deltaT) + "ns)");
		}
	}

	private int doCPath(Sim.Node n)
	{
		System.out.println("critical path for last transition of " + n.nName + ":");
		n = unAlias(n);
		cPath(n, 0);
		return 1;
	}

	/**
	 * discover and print critical path for node's last transistion
	 */
	private void doPath(String [] args)
	{
		apply(PATH_CALL, 0, args, 1, args.length);
	}

	/** number of buckets in histogram */	private static final int NBUCKETS = 20;

	/**
	 * print histogram of circuit activity in specified time interval
	 */
	private void doActivity(String [] args)
	{
		long begin = Sim.nsToDelta(TextUtils.atof(args[1]));
		long end = theSim.curDelta;
		if (args.length > 2)
			end = Sim.nsToDelta(TextUtils.atof(args[2]));
		if (end < begin)
		{
			long swp = end;   end = begin;   begin = swp;
		}

		// collect histogram info by walking the network
		long [] table = new long[NBUCKETS];
		for(int i = 0; i < NBUCKETS; table[i++] = 0);

		long size = (end - begin + 1) / NBUCKETS;
		if (size <= 0) size = 1;

		for(Iterator it = theSim.getNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			if ((n.nFlags & (Sim.ALIAS | Sim.MERGED | Sim.POWER_RAIL)) == 0)
			{
				if (n.getTime() >= begin && n.getTime() <= end)
					table[(int)((n.getTime() - begin) / size)] += 1;
			}
		}

		// print out what we found
		int total = 0;
		for(int i = 0; i < NBUCKETS; i++) total += table[i];

		System.out.println("Histogram of circuit activity: " + Sim.deltaToNS(begin) +
			" . " + Sim.deltaToNS(end) + "ns (bucket size = " + Sim.deltaToNS(size) + ")");

		for(int i = 0; i < NBUCKETS; i += 1)
			System.out.println(" " + Sim.deltaToNS(begin + (i * size)) + " -" + Sim.deltaToNS(begin + (i + 1) * size) + table[i]);
	}

	/**
	 * Print list of nodes which last changed value in specified time interval
	 */
	private void doChanges(String [] args)
	{
		long begin = Sim.nsToDelta(TextUtils.atof(args[1]));;
		long end = theSim.curDelta;
		if (args.length > 2)
			end = Sim.nsToDelta(TextUtils.atof(args[2]));

		column = 0;
		System.out.print("Nodes with last transition in interval " + Sim.deltaToNS(begin) + " . " + Sim.deltaToNS(end) + "ns:");

		for(Iterator it = theSim.getNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			n = unAlias(n);

			if ((n.nFlags & (Sim.MERGED | Sim.ALIAS)) != 0) return;

			if (n.getTime() >= begin && n.getTime() <= end)
			{
				int i = n.nName.length() + 2;
				if (column + i >= MAXCOL)
				{
					column = 0;
				}
				column += i;
				System.out.print("  " + n.nName);
			}
		}
		System.out.println();
	}

	/**
	 * Print list of nodes with undefined (X) value
	 */
	private void doPrintX(String [] args)
	{
		System.out.print("Nodes with undefined potential:");
		column = 0;
		for(Iterator it = theSim.getNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			n = unAlias(n);

			if ((n.nFlags & (Sim.MERGED | Sim.ALIAS)) == 0 && n.nPot == Sim.X)
			{
				int i = n.nName.length() + 2;
				if (column + i >= MAXCOL)
				{
					column = 0;
				}
				column += i;
				System.out.print("  " + n.nName);
			}
		}
		System.out.println();
	}

	/**
	 * Print nodes that are aliases
	 */
	private void doPrintAlias(String [] args)
	{
		if (theSim.numAliases == 0)
			System.out.println("there are no aliases");
		else
		{
			System.out.println("there are " + theSim.numAliases + " aliases:");
			for(Iterator it = theSim.getNodeList().iterator(); it.hasNext(); )
			{
				Sim.Node n = (Sim.Node)it.next();
				if ((n.nFlags & Sim.ALIAS) != 0)
				{
					n = unAlias(n);
					String is_merge = (n.nFlags & Sim.MERGED) != 0 ? " (part of a stack)" : "";
					System.out.println("  " + n.nName + " . " + n.nName + is_merge);
				}
			}
		}
	}

	/**
	 * Print list of transistors with src/drn shorted (or between power supplies).
	 */
	private void printTCap(String [] args)
	{
		if (theSim.tCap.getSTrans() == theSim.tCap)
			System.out.println("there are no shorted transistors");
		else
			System.out.println("shorted transistors:");
		for(Sim.Trans t = theSim.tCap.getSTrans(); t != theSim.tCap; t = t.getSTrans())
		{
			System.out.println(" " + Sim.transistorType[Sim.baseType(t.tType)] + " g=" + ((Sim.Node)t.gate).nName + " s=" +
				t.source.nName + " d=" + t.drain.nName + " (" +
				(t.r.length / theSim.getConfig().lambdaCM) + "x" + (t.r.width / theSim.getConfig().lambdaCM) + ")");
		}
	}

	/**
	 * Move back simulation time to specified time.
	 */
	private void backTime(String [] args)
	{
		long newT = Sim.nsToDelta(TextUtils.atof(args[1]));
		if (newT < 0 || newT > theSim.curDelta)
		{
			System.out.println(args[1] + ": invalid time");
			return;
		}

		theSim.curDelta = newT;
		clearInputs();
		theSim.getModel().backSimTime(theSim.curDelta, 0);
		theSim.curNode = null;			// fudge
		List nodes = theSim.getNodeList();
		for(Iterator it = nodes.iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			theSim.backToTime(n);
		}
		if (theSim.curDelta == 0)
			theSim.getModel().reInit();

		if (analyzerON)
			restartAnalyzer(0, theSim.curDelta, 1);

		pnWatchList();
	}

	private void clearInputs()
	{
		for(int i = 0; i < 5; i++)
		{
			if (listTbl[i] == null) continue;
			for(Iterator it = listTbl[i].iterator(); it.hasNext(); )
			{
				Sim.Node n = (Sim.Node)it.next();
				if ((n.nFlags & Sim.POWER_RAIL) == 0)
					n.nFlags &= ~(Sim.INPUT_MASK | Sim.INPUT);
			}
			listTbl[i] = null;
		}
		for(Iterator it = theSim.getNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			clearInput(n);
		}
	}

	private void clearInput(Sim.Node n)
	{
		if ((n.nFlags & Sim.POWER_RAIL) == 0)
			n.nFlags &= ~Sim.INPUT;
	}

	private int tranCntNSD = 0, tranCntNG = 0;

	/**
	 * Print event statistics.
	 */
	private void doStats(String [] args)
	{
		if (args.length == 2)
		{
			if (tranCntNG == 0 && tranCntNSD == 0)
			{
				for(Iterator it = theSim.getNodeList().iterator(); it.hasNext(); )
				{
					Sim.Node n = (Sim.Node)it.next();
					if ((n.nFlags & (Sim.ALIAS | Sim.POWER_RAIL)) == 0)
					{
						tranCntNG += n.nGateList.size();
						tranCntNSD += n.nTermList.size();
					}
				}
				System.out.println("avg: # gates/node = " + TextUtils.formatDouble(tranCntNG / theSim.numNodes) +
					",  # src-drn/node = " + TextUtils.formatDouble(tranCntNSD / theSim.numNodes));
			}
		}
		System.out.println("changes = " + theSim.numEdges);
		System.out.println("punts (cns) = " + theSim.numPunted + " (" + theSim.numConsPunted + ")");
		String n1 = "0.0";
		String n2 = "0.0";
		if (theSim.numPunted != 0)
		{
			n1 = TextUtils.formatDouble(100.0 / (theSim.numEdges / theSim.numPunted + 1.0));
			n2 = TextUtils.formatDouble(theSim.numConsPunted * 100.0 / theSim.numPunted);
		}
		System.out.println("punts = " + n1 + "%, cons_punted = " + n2 + "%");

		System.out.println("nevents = " + theSim.nEvent);
	}
}
