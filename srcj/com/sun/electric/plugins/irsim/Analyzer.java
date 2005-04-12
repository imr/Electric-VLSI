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
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.output.IRSIM;
import com.sun.electric.tool.simulation.DigitalSignal;
import com.sun.electric.tool.simulation.Engine;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.WaveformWindow;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
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
	private static final String simVersion = "9.5j";

	// the meaning of SimVector.command
	/** a comment in the command file */				private static final int VECTORCOMMENT    =  1;
	/** the "!" command to print gate info */			private static final int VECTOREXCL       =  2;
	/** the "?" command to print source/drain info */	private static final int VECTORQUESTION   =  3;
	/** the "activity" command */						private static final int VECTORACTIVITY   =  4;
	/** the "alias" command */							private static final int VECTORALIAS      =  5;
	/** the "ana" command */							private static final int VECTORANALYZER   =  6;
	/** the "assert" command (test signal value) */		private static final int VECTORASSERT     =  7;
	/** the "assertWhen" command */						private static final int VECTORASSERTWHEN =  8;
	/** the "back" command */							private static final int VECTORBACK       =  9;
	/** the "c" command to run a clock cycle */			private static final int VECTORC          = 10;
	/** the "changes" command */						private static final int VECTORCHANGES    = 11;
	/** the "clock" command to declare stimuli */		private static final int VECTORCLOCK      = 12;
	/** the "debug" command */							private static final int VECTORDEBUG      = 13;
	/** the "decay" command */							private static final int VECTORDECAY      = 14;
	/** the "h" command (set signal high) */			private static final int VECTORH          = 15;
	/** the "inputs" command */							private static final int VECTORINPUTS     = 16;
	/** the "l" command (set signal low) */				private static final int VECTORL          = 17;
	/** the "model" command */							private static final int VECTORMODEL      = 18;
	/** the "p" command */								private static final int VECTORP          = 19;
	/** the "path" command */							private static final int VECTORPATH       = 20;
	/** the "print" command */							private static final int VECTORPRINT      = 21;
	/** the "printx" command */							private static final int VECTORPRINTX     = 22;
	/** the "R" command */								private static final int VECTORR          = 23;
	/** the "report" command */							private static final int VECTORREPORT     = 24;
	/** the "s" command (advance time) */				private static final int VECTORS          = 25;
	/** the "set" command to change vector values */	private static final int VECTORSET        = 26;
	/** the "stats" command */							private static final int VECTORSTATS      = 27;
	/** the "stepsize" command to set time advance */	private static final int VECTORSTEPSIZE   = 28;
	/** the "stop" command */							private static final int VECTORSTOP       = 29;
	/** the "t" command to trace */						private static final int VECTORT          = 30;
	/** the "tcap" command */							private static final int VECTORTCAP       = 31;
	/** the "u" command */								private static final int VECTORU          = 32;
	/** the "unitdelay" command */						private static final int VECTORUNITDELAY  = 33;
	/** the "until" command */							private static final int VECTORUNTIL      = 34;
	/** the "V" command */								private static final int VECTORV          = 35;
	/** the "vector" command to group signals */		private static final int VECTORVECTOR     = 36;
	/** the "x" command (set signal undefined) */		private static final int VECTORX          = 37;

	/** default simulation steps per screen */			private static final int    DEF_STEPS     = 4;
	/** initial size of simulation window: 10ns */		private static final double DEFIRSIMTIMERANGE = 10.0E-9f;
	/** number of buckets in histogram */				private static final int    NBUCKETS      = 20;
	/** maximum width of print line */					private static final int	MAXCOL        = 80;

	/** set of potential characters */					private static final String potChars = "luxh.";

	/**
	 * Class that defines a single low-level IRSIM control command.
	 */
	private static class SimVector
	{
		/** index of command */						int            command;
		/** parameters to the command */			String []      parameters;
		/** actual signals named in command */		List           sigs;
		/** negated signals named in command */		List           sigsNegated;
		/** duration of step, where appropriate */	double         value;
		/** next in list of vectors */				SimVector      next;
	};

	public static class AssertWhen
	{
		/** which node we will check */				Sim.Node       node;
		/** what value has the node */				char	       val;
		/** next in list of assertions */			AssertWhen     nxt;
	};

	private static class Sequence
	{
		/** signal to control */					Signal         sig;
		/** array of values */						String  []     values;
	};

	private SimVector firstVector = null;
	private SimVector lastVector = null;
	private long      stepSize = 50000;

	private long      firstTime;
	private long      lastTime;
	private long      startTime;
	private long      stepsTime;
	private long      endTime;
	private long      lastStart;					/** last redisplay starting time */

	private double [] traceTime;
	private short  [] traceState;
	private int       traceTotal = 0;

	/** vectors which make up clock */				private List      xClock;
	/** vectors which make up a sequence */			private List      sList;
	/** longest clock sequence defined */			private int		  maxClock = 0;
	/** current output column */					private int		  column = 0;

	private List  [] listTbl = new List[5];

	/** list of nodes to be driven high */			public List  hInputs = new ArrayList();
	/** list of nodes to be driven low */			public List  lIinputs = new ArrayList();
	/** list of nodes to be driven X */				public List  uInputs = new ArrayList();
	/** list of nodes to be removed from input */	public List  xInputs = new ArrayList();

	/** set when analyzer is running */				public boolean  analyzerON;

	/** the simulation engine */					private Sim            theSim;
	/** the waveform window */						private WaveformWindow ww;
    /** the cell being simulated */					private Cell           cell;
    /** the context for the cell being simulated */	private VarContext     context;
    /** the name of the file being simulated */		private String         fileName;
    /** the name of the last vector file read */	private String         vectorFileName;

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
		theAnalyzer.cell = cell;
		theAnalyzer.context = context;
		theAnalyzer.fileName = fileName;
		new StartIRSIM(theAnalyzer);
	}

	private static class StartIRSIM extends Job
	{
		private Analyzer analyzer;

		public StartIRSIM(Analyzer analyzer)
		{
			super("Simulate cell", User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.analyzer = analyzer;
			startJob();
		}

		public boolean doIt()
		{
			synchronized(analyzer)
			{
				System.out.println("IRSIM, version " + simVersion);

				// now initialize the simulator
				Sim sim = analyzer.theSim;
				analyzer.initRSim();

				// Load network
				if (analyzer.cell != null) System.out.println("Loading netlist for cell " + analyzer.cell.describe() + "..."); else
					System.out.println("Loading netlist for file " + analyzer.fileName + "...");
				Stimuli sd = analyzer.getCircuit();
	 			Simulation.showSimulationData(sd, null);

	 			// make a waveform window
	 			analyzer.ww = sd.getWaveformWindow();
				analyzer.ww.setSimEngine(analyzer);
				analyzer.ww.setDefaultTimeRange(0.0, DEFIRSIMTIMERANGE);
				analyzer.ww.setMainTimeCursor(DEFIRSIMTIMERANGE/5.0*2.0);
				analyzer.ww.setExtensionTimeCursor(DEFIRSIMTIMERANGE/5.0*3.0);
				analyzer.init();
			}
			return true;
		}
	}

	private void init()
	{
		// tell the simulator to watch all signals
		initTimes(0, 50000, theSim.curDelta);

		for(Iterator it = theSim.getNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			while ((n.nFlags & Sim.ALIAS) != 0)
				n = n.nLink;

			if ((n.nFlags & Sim.MERGED) != 0)
				System.out.println("can't watch node " + n.nName);
			n.wind = n.cursor = n.head;
		}
		updateWindow(0);
		lastStart = theSim.maxTime;
		analyzerON = true;

		// read signal values
		updateWindow(theSim.curDelta);
	}

	private Stimuli getCircuit()
	{
		// Load network
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
		if (theSim.readNetwork(fileURL, components)) return null;

		// convert the stimuli
		Stimuli sd = new Stimuli();
		sd.setSeparatorChar('/');
		sd.setCell(cell);
		for(Iterator it = theSim.getNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			if (n.nName.equalsIgnoreCase("vdd") || n.nName.equalsIgnoreCase("gnd")) continue;

			// make a signal for it
			DigitalSignal sig = new DigitalSignal(sd);
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
			sig.setAppObject(n);

			sig.buildTime(2);
			sig.buildState(2);
			sig.setTime(0, 0);
			sig.setTime(1, 0.00000001);
			sig.setState(0, 0);
			sig.setState(1, 0);
		}
		return sd;
	}

	/**
	 * Main entry point to run an arbitrary IRSIM command.
	 */
	public void doCommand(String com)
	{
		synchronized(this)
		{
			if (com.equals("l") || com.equals("h") || com.equals("x"))
			{
				int command = VECTORL;
				if (com.equals("h")) command = VECTORH; else
					if (com.equals("x")) command = VECTORX;
				List signals = ww.getHighlightedNetworkNames();
				String [] parameters = new String[1];
				for(Iterator it = signals.iterator(); it.hasNext(); )
				{
					Signal sig = (Signal)it.next();
					parameters[0] = sig.getFullName().replace('.', '/');
					newVector(command, parameters, ww.getMainTimeCursor(), false);
				}
				if (Simulation.isIRSIMResimulateEach())
					playVectors();
				return;
			}

			if (com.equals("clear"))
			{
				List signals = ww.getHighlightedNetworkNames();
				if (signals.size() != 1)
				{
					System.out.println("Must select a single signal on which to clear stimuli");
					return;
				}
				Signal sig = (Signal)signals.get(0);
				String highsigname = sig.getFullName();
				sig.clearControlPoints();

				SimVector lastSV = null;
				for(SimVector sv = firstVector; sv != null; sv = sv.next)
				{
					if (sv.command == VECTORL || sv.command == VECTORH || sv.command == VECTORX ||
						sv.command == VECTORASSERT || sv.command == VECTORSET)
					{
						if (sv.sigs.contains(sig))
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

			if (com.equals("clearSelected"))
			{
				boolean found = false;
				for(Iterator it = ww.getPanels(); it.hasNext(); )
				{
					WaveformWindow.Panel wp = (WaveformWindow.Panel)it.next();
					for(Iterator sIt = wp.getSignals().iterator(); sIt.hasNext(); )
					{
						WaveformWindow.WaveSignal ws = (WaveformWindow.WaveSignal)sIt.next();
						if (!ws.isSelected()) continue;
						double [] selectedCPs = ws.getSelectedControlPoints();
						if (selectedCPs == null) continue;
						for(int i=0; i<selectedCPs.length; i++)
						{
							if (clearControlPoint(ws.getSignal(), selectedCPs[i]))
								found = true;
						}
					}
				}
				if (!found)
				{
					System.out.println("There are no selected control points to remove");
					return;
				}

				// resimulate if requested
				if (Simulation.isIRSIMResimulateEach())
					playVectors();
				return;
			}

			if (com.equals("clearAll"))
			{
				for(Iterator it = ww.getPanels(); it.hasNext(); )
				{
					WaveformWindow.Panel wp = (WaveformWindow.Panel)it.next();
					for(Iterator sIt = wp.getSignals().iterator(); sIt.hasNext(); )
					{
						WaveformWindow.WaveSignal ws = (WaveformWindow.WaveSignal)sIt.next();
						ws.getSignal().clearControlPoints();
					}
				}

				if (Simulation.isIRSIMResimulateEach())
					clearAllVectors();
				return;
			}

			if (com.equals("update"))
			{
				playVectors();
				return;
			}

			if (com.equals("info"))
			{
				List signals = ww.getHighlightedNetworkNames();
				for(Iterator it = signals.iterator(); it.hasNext(); )
				{
					Signal sig = (Signal)it.next();
					SimVector excl = new SimVector();
					excl.command = VECTOREXCL;
					excl.sigs = new ArrayList();
					excl.sigs.add(sig);
					issueCommand(excl);
	
					excl.command = VECTORQUESTION;
					issueCommand(excl);
				}
				return;
			}

			if (com.equals("save"))
			{
				saveVectorFile();
				return;
			}

			if (com.equals("restore"))
			{
				vectorFileName = OpenFile.chooseInputFile(FileType.IRSIMVECTOR, "IRSIM Vector file");
				if (vectorFileName == null) return;
				loadVectorFile();
				return;
			}
		}
	}

	public void refresh()
	{
		// make a new simulation object
		theSim = new Sim(this);

		// now initialize the simulator
		initRSim();

		// Load network
		Stimuli sd = getCircuit();
		Simulation.showSimulationData(sd, ww);

		if (vectorFileName != null) loadVectorFile();
		init();

		playVectors();
	}

	/************************** SIMULATION VECTORS **************************/

	/**
	 * Method to play the simulation vectors into the simulator.
	 */
	private void playVectors()
	{
		SimVector back = new SimVector();
		back.command = VECTORBACK;
		back.value = 0;
		issueCommand(back);
//		issueCommand("flush", null);

		double curTime = 0;
		analyzerON = false;
		for(SimVector sv = firstVector; sv != null; sv = sv.next)
		{
			if (sv.command == VECTORCOMMENT) continue;
			issueCommand(sv);
		}
		SimVector step = new SimVector();
		step.command = VECTORS;
		step.value = Sim.deltaToNS(stepSize);
		issueCommand(step);
		analyzerON = true;
		updateWindow(theSim.curDelta);

		// update main cursor location if requested
		if (Simulation.isIRSIMAutoAdvance())
			ww.setMainTimeCursor(curTime + 10.0/1000000000.0);
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
		URL url = TextUtils.makeURLToFile(vectorFileName);
		try
		{
			URLConnection urlCon = url.openConnection();
			InputStreamReader is = new InputStreamReader(urlCon.getInputStream());
			LineNumberReader lineReader = new LineNumberReader(is);

			// remove all vectors
			firstVector = null;
			lastVector = null;
			for(Iterator it = ww.getPanels(); it.hasNext(); )
			{
				WaveformWindow.Panel wp = (WaveformWindow.Panel)it.next();
				for(Iterator sIt = wp.getSignals().iterator(); sIt.hasNext(); )
				{
					WaveformWindow.WaveSignal ws = (WaveformWindow.WaveSignal)sIt.next();
					ws.getSignal().clearControlPoints();
				}
			}
			System.out.println("Reading " + vectorFileName);
			double currentTime = 0;
			boolean anyAnalyzerCommands = false;
			for(;;)
			{
				String buf = lineReader.readLine();
				if (buf == null) break;

				// ignore comments
				if (buf.startsWith("|"))
				{
					String [] par = new String[1];
					par[0] = buf.substring(1);
					newVector(VECTORCOMMENT, par, currentTime, true);
					continue;
				}

				// get the first keyword
				String [] targ = Sim.parseLine(buf, false);
				if (targ == null || targ.length <= 0) continue;

				// handle commands
				int command;
				if (targ[0].equals("!")) command = VECTOREXCL; else
				if (targ[0].equals("?")) command = VECTORQUESTION; else
				if (targ[0].equals("activity")) command = VECTORACTIVITY; else
				if (targ[0].equals("alias")) command = VECTORALIAS; else
				if (targ[0].equals("ana") || targ[0].equals("analyzer")) command = VECTORANALYZER; else
				if (targ[0].equals("assert")) command = VECTORASSERT; else
				if (targ[0].equals("assertwhen")) command = VECTORASSERTWHEN; else
				if (targ[0].equals("back")) command = VECTORBACK; else
				if (targ[0].equals("c")) command = VECTORC; else
				if (targ[0].equals("changes")) command = VECTORCHANGES; else
				if (targ[0].equals("clock")) command = VECTORCLOCK; else
				if (targ[0].equals("debug")) command = VECTORDEBUG; else
				if (targ[0].equals("decay")) command = VECTORDECAY; else
				if (targ[0].equals("h")) command = VECTORH; else
				if (targ[0].equals("inputs")) command = VECTORINPUTS; else
				if (targ[0].equals("l")) command = VECTORL; else
				if (targ[0].equals("model")) command = VECTORMODEL; else
				if (targ[0].equals("p")) command = VECTORP; else
				if (targ[0].equals("path")) command = VECTORPATH; else
				if (targ[0].equals("print")) command = VECTORPRINT; else
				if (targ[0].equals("printx")) command = VECTORPRINTX; else
				if (targ[0].equals("R")) command = VECTORR; else
				if (targ[0].equals("report")) command = VECTORREPORT; else
				if (targ[0].equals("s")) command = VECTORS; else
				if (targ[0].equals("set")) command = VECTORSET; else
				if (targ[0].equals("stats")) command = VECTORSTATS; else
				if (targ[0].equals("stepsize")) command = VECTORSTEPSIZE; else
				if (targ[0].equals("stop")) command = VECTORSTOP; else
				if (targ[0].equals("t")) command = VECTORT; else
				if (targ[0].equals("tcap")) command = VECTORTCAP; else
				if (targ[0].equals("u")) command = VECTORU; else
				if (targ[0].equals("unitdelay")) command = VECTORUNITDELAY; else
				if (targ[0].equals("until")) command = VECTORUNTIL; else
				if (targ[0].equals("V")) command = VECTORV; else
				if (targ[0].equals("vector")) command = VECTORVECTOR; else
				if (targ[0].equals("x")) command = VECTORX; else
				{
					System.out.println("Unknown command: " + targ[0]);
					continue;
				}

				// store the vector
				String [] params = new String[targ.length-1];
				for(int i=1; i<targ.length; i++) params[i-1] = targ[i];
				SimVector sv = newVector(command, params, currentTime, true);

				// keep track of time
				if (command == VECTORS)
				{
					currentTime += sv.value / 1000000000.0;
					continue;
				}

				// handle changes to signals in the Waveform Window
				if (command == VECTORANALYZER)
				{
					if (!anyAnalyzerCommands)
					{
						// clear the stimuli on the first time
						anyAnalyzerCommands = true;
						ww.clearHighlighting();
						List allPanels = new ArrayList();
						for(Iterator it = ww.getPanels(); it.hasNext(); )
							allPanels.add(it.next());
						for(Iterator it = allPanels.iterator(); it.hasNext(); )
						{
							WaveformWindow.Panel wp = (WaveformWindow.Panel)it.next();
							wp.closePanel();
						}
					}
					List sigs = new ArrayList();
					getTargetNodes(targ, 1, sigs, null);
					for(Iterator it = sigs.iterator(); it.hasNext(); )
					{
						Signal sig = (Signal)it.next();
						WaveformWindow.Panel wp = new WaveformWindow.Panel(ww, false);
						wp.makeSelectedPanel();
						new WaveformWindow.WaveSignal(wp, sig);
					}
					continue;
				}

				// handle aggregation of signals into busses
				if (command == VECTORVECTOR)
				{
					// find this vector name in the list of vectors
					DigitalSignal busSig = null;
					Stimuli sd = ww.getSimData();
					for(Iterator it = sd.getBussedSignals().iterator(); it.hasNext(); )
					{
						DigitalSignal sig = (DigitalSignal)it.next();
						if (sig.getSignalName().equals(targ[1]))
						{
							busSig = sig;
							busSig.clearBussedSignalList();
							break;
						}
					}
					if (busSig == null)
					{
						busSig = new DigitalSignal(sd);
						busSig.setSignalName(targ[1]);
						busSig.buildBussedSignalList();
					}
					List sigs = new ArrayList();
					getTargetNodes(targ, 2, sigs, null);
					for(Iterator it = sigs.iterator(); it.hasNext(); )
					{
						Signal sig = (Signal)it.next();
						busSig.addToBussedSignalList(sig);
					}
					continue;
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
			System.out.println("Error reading " + vectorFileName);
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
				for(int i=0; i<sv.parameters.length; i++)
					infstr += " " + sv.parameters[i];
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

	private void issueCommand(SimVector sv)
	{
		if (Simulation.isIRSIMShowsCommands())
		{
			System.out.print("> " + commandName(sv.command));
			if (sv.parameters != null)
			{
				for(int i=0; i<sv.parameters.length; i++) System.out.print(" " + sv.parameters[i]);
			}
			System.out.println();
		}
		switch (sv.command)
		{
			case VECTOREXCL:       doInfo(sv);         break;
			case VECTORQUESTION:   doInfo(sv);         break;
			case VECTORACTIVITY:   doActivity(sv);     break;
			case VECTORALIAS:      doPrintAlias();     break;
			case VECTORANALYZER:                       break;
			case VECTORASSERT:     doAssert(sv);       break;
			case VECTORASSERTWHEN: doAssertWhen(sv);   break;
			case VECTORBACK:       doBack(sv);         break;
			case VECTORC:          doClock(sv);        break;
			case VECTORCHANGES:    doChanges(sv);      break;
			case VECTORCLOCK:      setClock(sv);       break;
			case VECTORDEBUG:      doDebug(sv);        break;
			case VECTORDECAY:      doDecay(sv);        break;
			case VECTORH:          doSetValue(sv);     break;
			case VECTORINPUTS:     doInputs();         break;
			case VECTORL:          doSetValue(sv);     break;
			case VECTORMODEL:      doModel(sv);        break;
			case VECTORP:          doPhase();          break;
			case VECTORPATH:       doPath(sv);         break;
			case VECTORPRINT:      doPrint(sv);        break;
			case VECTORPRINTX:     doPrintX();         break;
			case VECTORR:          doRunSeq(sv);       break;
			case VECTORREPORT:     doReport(sv);       break;
			case VECTORS:          doStep(sv);         break;
			case VECTORSET:        doSet(sv);          break;
			case VECTORSTATS:      doStats(sv);        break;
			case VECTORSTEPSIZE:   doStepSize(sv);     break;
			case VECTORSTOP:       doStop(sv);         break;
			case VECTORT:          doTrace(sv);        break;
			case VECTORTCAP:       doTCap();           break;
			case VECTORU:          doSetValue(sv);     break;
			case VECTORUNITDELAY:  doUnitDelay(sv);    break;
			case VECTORUNTIL:      doUntil(sv);        break;
			case VECTORV:          doV(sv);            break;
			case VECTORVECTOR:                         break;
			case VECTORX:          doSetValue(sv);     break;
		}
	}

	private String commandName(int command)
	{
		switch (command)
		{
			case VECTORCOMMENT:    return "|";
			case VECTOREXCL:       return "!";
			case VECTORQUESTION:   return "?";
			case VECTORACTIVITY:   return "activity";
			case VECTORALIAS:      return "alias";
			case VECTORANALYZER:   return "ana";
			case VECTORASSERT:     return "assert";
			case VECTORASSERTWHEN: return "assertWhen";
			case VECTORBACK:       return "back";
			case VECTORC:          return "c";
			case VECTORCHANGES:    return "changes";
			case VECTORCLOCK:      return "clock";
			case VECTORDEBUG:      return "debug";
			case VECTORDECAY:      return "decay";
			case VECTORH:          return "h";
			case VECTORINPUTS:     return "inputs";
			case VECTORL:          return "l";
			case VECTORMODEL:      return "model";
			case VECTORP:          return "p";
			case VECTORPATH:       return "path";
			case VECTORPRINT:      return "print";
			case VECTORPRINTX:     return "printx";
			case VECTORR:          return "R";
			case VECTORREPORT:     return "report";
			case VECTORS:          return "s";
			case VECTORSET:        return "set";
			case VECTORSTATS:      return "stats";
			case VECTORSTEPSIZE:   return "stepsize";
			case VECTORSTOP:       return "stop";
			case VECTORT:          return "t";
			case VECTORTCAP:       return "tcap";
			case VECTORU:          return "u";
			case VECTORUNITDELAY:  return "unitdelay";
			case VECTORUNTIL:      return "until";
			case VECTORV:          return "V";
			case VECTORVECTOR:     return "vector";
			case VECTORX:          return "x";
		}
		return "";
	}

	/**
	 * Method to create a new simulation vector at time "time", on signal "sig",
	 * with state "state".  The vector is inserted into the play list in the proper
	 * order.
	 */
	private SimVector newVector(int command, String [] params, double insertTime, boolean justAppend)
	{
		SimVector newsv = new SimVector();
		newsv.command = command;
		newsv.parameters = params;

		// precompute information that is appropriate to this command
		switch (command)
		{
			case VECTORS:
				double newSize = Sim.deltaToNS(stepSize);
				if (params.length > 0)
				{
					newSize = TextUtils.atof(params[0]);
					long lNewSize = Sim.nsToDelta(newSize);
					if (lNewSize <= 0)
					{
						System.out.println("Bad step size: " + TextUtils.formatDouble(newSize*1000) + "psec (must be 10 psec or larger)");
						return null;
					}
				}
				newsv.value = newSize;
				break;
			case VECTORSTEPSIZE:
				if (params.length > 0)
					stepSize = Sim.nsToDelta(TextUtils.atof(params[0]));
				break;

			case VECTORBACK:
				newsv.value = 0;
				if (params.length > 0)
				{
					newsv.value = TextUtils.atof(params[0]);
				}
				break;

			case VECTORL:
			case VECTORH:
			case VECTORX:
			case VECTORANALYZER:
			case VECTOREXCL:
			case VECTORQUESTION:
			case VECTORT:
			case VECTORPATH:
			case VECTORSTOP:
				newsv.sigs = new ArrayList();
				newsv.sigsNegated = null;
				if (command == VECTORT) newsv.sigsNegated = new ArrayList();
				getTargetNodes(params, 0, newsv.sigs, newsv.sigsNegated);

				if (command == VECTORL || command == VECTORH || command == VECTORX)
				{
					// add this moment in time to the control points for the signal
					for(Iterator it = newsv.sigs.iterator(); it.hasNext(); )
					{
						Signal sig = (Signal)it.next();
						sig.addControlPoint(insertTime);
					}
				}
				break;
		}

		// insert the vector */
		SimVector lastSV = null;
		if (justAppend || insertTime < 0.0)
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
					case VECTORS:
						double stepSze = sv.value / 1000000000.0;
						long ss = Sim.nsToDelta(((curTime + stepSze) - insertTime) * 1000000000.0);
						if (ss != 0 && GenMath.doublesLessThan(insertTime, curTime+stepSze))
						{
							// splitting step at "insertTime"
							sv.parameters = new String[1];
							sv.value = (insertTime-curTime) * 1000000000.0;
							sv.parameters[0] = TextUtils.formatDouble(sv.value);

							// create second step to advance after this signal
							SimVector afterSV = new SimVector();
							afterSV.command = VECTORS;
							afterSV.parameters = new String[1];
							afterSV.value = curTime + stepSze - insertTime * 1000000000.0;
							afterSV.parameters[0] = TextUtils.formatDouble(afterSV.value);
							afterSV.next = sv.next;
							sv.next = afterSV;
						}
						curTime += stepSze;
						break;
					case VECTORSTEPSIZE:
						if (sv.parameters.length > 0)
							defaultStepSize = TextUtils.atof(sv.parameters[0]) / 1000000000.0;
						break;
					case VECTORCLOCK:
						clockPhases = sv.parameters.length - 1;
						break;
					case VECTORC:
						int mult = 1;
						if (sv.parameters.length > 0)
							mult = TextUtils.atoi(sv.parameters[0]);
						curTime += defaultStepSize * clockPhases * mult;
						break;
					case VECTORP:
						curTime += defaultStepSize;
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
					afterSV.parameters[0] = TextUtils.formatDouble(thisStep * 1000000000.0);
					afterSV.value = thisStep;
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
		return newsv;
	}

	private boolean clearControlPoint(Signal sig, double insertTime)
	{
		double defaultStepSize = 10.0 / 1000000000.0;
		double curTime = 0.0;
		int clockPhases = 0;
		SimVector lastSV = null;
		boolean cleared = false;
		for(SimVector sv = firstVector; sv != null; sv = sv.next)
		{
			switch (sv.command)
			{
				case VECTORS:
					double stepSze = defaultStepSize;
					if (sv.value != 0) stepSze = sv.value / 1000000000.0;
					curTime += stepSze;
					break;
				case VECTORSTEPSIZE:
					if (sv.parameters.length > 0)
						defaultStepSize = TextUtils.atof(sv.parameters[0]) / 1000000000.0;
					break;
				case VECTORCLOCK:
					clockPhases = sv.parameters.length - 1;
					break;
				case VECTORC:
					int mult = 1;
					if (sv.parameters.length > 0)
						mult = TextUtils.atoi(sv.parameters[0]);
					curTime += defaultStepSize * clockPhases * mult;
					break;
				case VECTORP:
					curTime += defaultStepSize;
					break;
				case VECTORL:
				case VECTORH:
				case VECTORX:
					if (GenMath.doublesEqual(insertTime, curTime))
					{
						boolean found = false;
						for(Iterator it = sv.sigs.iterator(); it.hasNext(); )
						{
							Signal s = (Signal)it.next();
							if (s == sig)
							{
								found = true;
								sig.removeControlPoint(insertTime);
								break;
							}
						}
						if (found)
						{
							cleared = true;
							if (lastSV == null) firstVector = sv.next; else
								lastSV.next = sv.next;
							continue;
						}
					}
			}
			lastSV = sv;
			if (GenMath.doublesLessThan(insertTime, curTime)) break;
		}
		return cleared;
	}

	private void getTargetNodes(String [] params, int low, List normalList, List negatedList)
	{
		int size = params.length - low;
		for(int i=0; i<size; i++)
		{
			String name = params[i+low];
			boolean negated = false;
			if (name.startsWith("-"))
			{
				name = name.substring(1);
				if (negatedList != null) negated = true;
			}
			if (name.indexOf('*') >= 0)
			{
				for(Iterator it = ww.getSimData().getSignals().iterator(); it.hasNext(); )
				{
					Signal sig = (Signal)it.next();
					if (strMatch(name, sig.getFullName()))
					{
						if (negated) negatedList.add(sig); else
							normalList.add(sig);
					}
				}
			} else
			{
				// no wildcards: just find the name
				Signal sig = this.findName(name);
				if (sig == null)
				{
					System.out.println("Cannot find node named '" + name + "'");
					continue;
				}
				if (negated) negatedList.add(sig); else
					normalList.add(sig);
			}
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
				case VECTORS:
					double stepSze = defaultStepSize;
					if (sv.value != 0) stepSze = sv.value / 1000000000.0;
					curTime += stepSze;
					break;
				case VECTORSTEPSIZE:
					if (sv.parameters.length > 0)
						defaultStepSize = TextUtils.atof(sv.parameters[0]) / 1000000000.0;
					break;
				case VECTORCLOCK:
					clockPhases = sv.parameters.length - 1;
					break;
				case VECTORC:
					int mult = 1;
					if (sv.parameters.length > 0)
						mult = TextUtils.atoi(sv.parameters[0]);
					curTime += defaultStepSize * clockPhases * mult;
					break;
				case VECTORP:
					curTime += defaultStepSize;
					break;
			}
		}
		return curTime;
	}

	/******************************************** COMMANDS *****************************************/

	/**
	 * display info about a node
	 */
	private void doInfo(SimVector sv)
	{
		if (sv.sigs == null) return;
		for(Iterator sIt = sv.sigs.iterator(); sIt.hasNext(); )
		{
			Signal sig = (Signal)sIt.next();
			Sim.Node n = (Sim.Node)sig.getAppObject();
			if (n == null) continue;

			String name = n.nName;
			while((n.nFlags & Sim.ALIAS) != 0)
				n = n.nLink;

			if ((n.nFlags & Sim.MERGED) != 0)
			{
				System.out.println(name + " => node is inside a transistor stack");
				return;
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
			if (sv.command == VECTOREXCL)
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
		}
	}

	/**
	 * print histogram of circuit activity in specified time interval
	 */
	private void doActivity(SimVector sv)
	{
		long begin = Sim.nsToDelta(TextUtils.atof(sv.parameters[0]));
		long end = theSim.curDelta;
		if (sv.parameters.length > 1)
			end = Sim.nsToDelta(TextUtils.atof(sv.parameters[1]));
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
	 * Print nodes that are aliases
	 */
	private void doPrintAlias()
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
	 * Move back simulation time to specified time.
	 */
	private void doBack(SimVector sv)
	{
		long newT = Sim.nsToDelta(sv.value);
		if (newT < 0 || newT > theSim.curDelta)
		{
			System.out.println(sv.value + ": invalid time in BACK command");
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
		{
			for(Iterator it = theSim.getNodeList().iterator(); it.hasNext(); )
			{
				Sim.Node n = (Sim.Node)it.next();
				n.wind = n.cursor = n.head;
			}

			// should set "theSim.curDelta" to the width of the screen
			initTimes(0, stepsTime / DEF_STEPS, theSim.curDelta);
			updateTraceCache();
		}

		pnWatchList();
	}

	/**
	 * process "c" command line
	 */
	private void doClock(SimVector sv)
	{
		// calculate how many clock cycles to run
		int n = 1;
		if (sv.parameters.length == 1)
		{
			n = TextUtils.atoi(sv.parameters[0]);
			if (n <= 0) n = 1;
		}

		clockIt(n);		// do the hard work
	}

	/**
	 * Print list of nodes which last changed value in specified time interval
	 */
	private void doChanges(SimVector sv)
	{
		long begin = Sim.nsToDelta(TextUtils.atof(sv.parameters[0]));
		long end = theSim.curDelta;
		if (sv.parameters.length > 1)
			end = Sim.nsToDelta(TextUtils.atof(sv.parameters[1]));

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
	 * define clock sequences(s)
	 */
	private void setClock(SimVector sv)
	{
		// process sequence and add to clock list
		defSequence(sv.parameters, xClock);

		// compute the maximum clock size
		maxClock = 0;
		for(Iterator it = xClock.iterator(); it.hasNext(); )
		{
			Sequence t = (Sequence)it.next();
			if (t.values.length > maxClock) maxClock = t.values.length;
		}
	}

	private void doDebug(SimVector sv)
	{
		if (sv.parameters.length <= 0) theSim.irDebug = 0; else
		{
			for(int i=0; i<sv.parameters.length; i++)
			{
				if (sv.parameters[i].equalsIgnoreCase("ev"))
				{
					theSim.irDebug |= Sim.DEBUG_EV;
				} else if (sv.parameters[i].equalsIgnoreCase("dc"))
				{
					theSim.irDebug |= Sim.DEBUG_DC;
				} else if (sv.parameters[i].equalsIgnoreCase("tau"))
				{
					theSim.irDebug |= Sim.DEBUG_TAU;
				} else if (sv.parameters[i].equalsIgnoreCase("taup"))
				{
					theSim.irDebug |= Sim.DEBUG_TAUP;
				} else if (sv.parameters[i].equalsIgnoreCase("spk"))
				{
					theSim.irDebug |= Sim.DEBUG_SPK;
				} else if (sv.parameters[i].equalsIgnoreCase("tw"))
				{
					theSim.irDebug |= Sim.DEBUG_TW;
				} else if (sv.parameters[i].equalsIgnoreCase("all"))
				{
					theSim.irDebug = Sim.DEBUG_EV | Sim.DEBUG_DC | Sim.DEBUG_TAU | Sim.DEBUG_TAUP | Sim.DEBUG_SPK | Sim.DEBUG_TW;
				} else if (sv.parameters[i].equalsIgnoreCase("off"))
				{
					theSim.irDebug = 0;
				}
			}
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
	 * set decay parameter
	 */
	private void doDecay(SimVector sv)
	{
		if (sv.parameters.length == 0)
		{
			if (theSim.tDecay == 0)
				System.out.println("decay = No decay");
			else
				System.out.println("decay = " + Sim.deltaToNS(theSim.tDecay) + "ns");
		} else
		{
			theSim.tDecay = Sim.nsToDelta(TextUtils.atof(sv.parameters[0]));
			if (theSim.tDecay < 0)
				theSim.tDecay = 0;
		}
	}

	/**
	 * Set value of a node/vector to the requested value (hlux).
	 */
	private void doSetValue(SimVector sv)
	{
		if (sv.sigs == null) return;

		for(Iterator sIt = sv.sigs.iterator(); sIt.hasNext(); )
		{
			Signal sig = (Signal)sIt.next();
			Sim.Node n = (Sim.Node)sig.getAppObject();
			setIn(n, commandName(sv.command).charAt(0));
		}
	}

	/**
	 * display current inputs
	 */
	private void doInputs()
	{
		Sim.Node [] inpTbl = new Sim.Node[Sim.N_POTS];

		inpTbl[Sim.HIGH] = inpTbl[Sim.LOW] = inpTbl[Sim.X] = null;
		for(Iterator it = theSim.getNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			if ((n.nFlags & (Sim.INPUT|Sim.ALIAS|Sim.POWER_RAIL|Sim.VISITED|Sim.INPUT_MASK)) == Sim.INPUT)
			{
				n.setNext(inpTbl[n.nPot]);
				inpTbl[n.nPot] = n;
				n.nFlags |= Sim.VISITED;
			}
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

	private void doModel(SimVector sv)
	{
		if (sv.parameters.length < 1) return;
		if (sv.parameters[0].equals("switch")) theSim.setModel(false); else
			if (sv.parameters[0].equals("linear")) theSim.setModel(true); else
		{
			System.out.println("Unknown model: " + sv.parameters[0] + " (want either switch or linear)");
		}
	}

	/**
	 * Do one simulation step
	 */
	private void doPhase()
	{
		stepPhase();
		pnWatchList();
	}

	/**
	 * discover and print critical path for node's last transistion
	 */
	private void doPath(SimVector sv)
	{
		if (sv.sigs == null) return;
		for(Iterator sIt = sv.sigs.iterator(); sIt.hasNext(); )
		{
			Signal sig = (Signal)sIt.next();
			Sim.Node n = (Sim.Node)sig.getAppObject();
			System.out.println("Critical path for last transition of " + n.nName + ":");
			n = unAlias(n);
			cPath(n, 0);
		}
	}

	/**
	 * output message to console/log file
	 */
	private void doPrint(SimVector sv)
	{
		String infstr = "";
		for(int n=0; n<sv.parameters.length; n++)
		{
			if (n != 1) infstr += " ";
			infstr += sv.parameters[n];
		}
		System.out.println(infstr);
	}

	/**
	 * Print list of nodes with undefined (X) value
	 */
	private void doPrintX()
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
	 * clock circuit through all the input vectors previously set up
	 */
	private void doRunSeq(SimVector sv)
	{
		// calculate how many clock cycles to run
		int n = 1;
		if (sv.parameters.length == 1)
		{
			n = TextUtils.atoi(sv.parameters[0]);
			if (n <= 0) n = 1;
		}

		// run 'em by setting each input node to successive values of its associated sequence
		if (sList.size() == 0)
		{
			System.out.println("no input vectors defined!");
			return;
		}

		// determine the longest sequence
		int maxSeq = 0;
		for(Iterator it = sList.iterator(); it.hasNext(); )
		{
			Sequence cs = (Sequence)it.next();
			if (cs.values.length > maxSeq) maxSeq = cs.values.length;
		}

		// run it
		for(int cycle = 0; cycle < n; cycle++)
		{
			for(int i = 0; i < maxSeq; i++)
			{
				vecValue(i);
				if (clockIt(1)) return;
				pnWatchList();
			}
		}
	}

	/**
	 * set tReport parameter
	 */
	private void doReport(SimVector sv)
	{
		if (sv.parameters[0].equalsIgnoreCase("decay")) theSim.tReport |= Sim.REPORT_DECAY; else
			if (sv.parameters[0].equalsIgnoreCase("delay")) theSim.tReport |= Sim.REPORT_DELAY; else
				if (sv.parameters[0].equalsIgnoreCase("tau")) theSim.tReport |= Sim.REPORT_TAU; else
					if (sv.parameters[0].equalsIgnoreCase("tcoord")) theSim.tReport |= Sim.REPORT_TCOORD; else
						if (sv.parameters[0].equalsIgnoreCase("none")) theSim.tReport = 0;
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
	 * relax network, optionally set stepsize
	 */
	private void doStep(SimVector sv)
	{
		double newSize = sv.value;
		long lNewSize = Sim.nsToDelta(newSize);
		if (lNewSize <= 0) return;
		relax(theSim.curDelta + lNewSize);
		pnWatchList();
	}

	/**
	 * set bit vector
	 */
	private void doSet(SimVector sv)
	{
		Signal sig = findName(sv.parameters[0]);
		if (sig == null)
		{
			System.out.println("Cannot find signal: " + sv.parameters[0]);
			return;
		}

		List sigsOnBus = sig.getBussedSignals();
		if (sigsOnBus == null)
		{
			System.out.println("Signal: " + sv.parameters[0] + " is not a bus");
			return;
		}

		if (sigsOnBus.size() != sv.parameters[1].length())
		{
			System.out.println("Wrong number of bits for this vector");
			return;
		}
		for(int i = 0; i < sigsOnBus.size(); i++)
		{
			Sim.Node n = (Sim.Node)((Signal)sigsOnBus.get(i)).getAppObject();
			setIn(n, sv.parameters[1].charAt(i));
		}
	}

	private int tranCntNSD = 0, tranCntNG = 0;

	/**
	 * Print event statistics.
	 */
	private void doStats(SimVector sv)
	{
		if (sv.parameters.length == 1)
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

	/**
	 * set stepsize
	 */
	private void doStepSize(SimVector sv)
	{
		if (sv.parameters.length < 1)
		{
			System.out.println("stepsize = " + Sim.deltaToNS(stepSize));
			return;
		}

		double timeNS = TextUtils.atof(sv.parameters[0]);
		long newSize = Sim.nsToDelta(timeNS);
		if (newSize <= 0)
		{
			System.out.println("Bad step size: " + TextUtils.formatDouble(timeNS*1000) + "psec (must be 10 psec or larger)");
			return;
		}
		stepSize = newSize;
	}

	/**
	 * mark nodes and vectors for stoping
	 */
	private void doStop(SimVector sv)
	{
		if (sv.sigs != null)
		{
			for(Iterator sIt = sv.sigs.iterator(); sIt.hasNext(); )
			{
				Signal sig = (Signal)sIt.next();
				Sim.Node n = (Sim.Node)sig.getAppObject();
				n = unAlias(n);
				if ((n.nFlags & Sim.MERGED) != 0) continue;

				if (true)
					n.nFlags &= ~Sim.STOPONCHANGE;
				else
					n.nFlags |= Sim.STOPONCHANGE;
			}
		}
		setVecNodes(Sim.WATCHVECTOR);
		setVecNodes(Sim.STOPVECCHANGE);
	}

	/**
	 * mark nodes and vectors for tracing
	 */
	private void doTrace(SimVector sv)
	{
		if (sv.sigs != null)
		{
			for(Iterator sIt = sv.sigs.iterator(); sIt.hasNext(); )
			{
				Signal sig = (Signal)sIt.next();
				Sim.Node n = (Sim.Node)sig.getAppObject();
				n = unAlias(n);

				if ((n.nFlags & Sim.MERGED) != 0)
				{
					System.out.println("can't trace " + n.nName);
					continue;
				}

				n.nFlags |= Sim.WATCHED;
			}
		}
		if (sv.sigsNegated != null)
		{
			for(Iterator sIt = sv.sigsNegated.iterator(); sIt.hasNext(); )
			{
				Signal sig = (Signal)sIt.next();
				Sim.Node n = (Sim.Node)sig.getAppObject();
				n = unAlias(n);

				if ((n.nFlags & Sim.MERGED) != 0)
				{
					System.out.println("can't trace " + n.nName);
					continue;
				}

				if ((n.nFlags & Sim.WATCHED) != 0)
				{
					System.out.println(n.nName + " was watched; not any more");
					n.nFlags &= ~Sim.WATCHED;
				}

			}
		}
		setVecNodes(Sim.WATCHVECTOR);
	}

	/**
	 * Print list of transistors with src/drn shorted (or between power supplies).
	 */
	private void doTCap()
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
	 * set unitdelay parameter
	 */
	private void doUnitDelay(SimVector sv)
	{
		if (sv.parameters.length == 0)
		{
			if (theSim.tUnitDelay == 0)
				System.out.println("unitdelay = OFF");
			else
				System.out.println("unitdelay = " + Sim.deltaToNS(theSim.tUnitDelay));
			return;
		}

		theSim.tUnitDelay = (int)Sim.nsToDelta(TextUtils.atof(sv.parameters[0]));
		if (theSim.tUnitDelay < 0) theSim.tUnitDelay = 0;
	}

	private void doUntil(SimVector sv)
	{
		String mask = null;
		StringBuffer value = null;
		int cCount = 0;
		if (sv.parameters.length == 4)
		{
			mask = sv.parameters[1];
			value = new StringBuffer(sv.parameters[2]);
			cCount = TextUtils.atoi(sv.parameters[3]);
		} else
		{
			mask = null;
			value = new StringBuffer(sv.parameters[1]);
			cCount = TextUtils.atoi(sv.parameters[2]);
		}

		Signal sig = findName(sv.parameters[0]);
		if (sig == null)
		{
			System.out.println("UNTIL statement cannot find signal: " + sv.parameters[0]);
			return;
		}

		String name = null;
		int comp = 0;
		int nBits = 1;
		Sim.Node [] nodes = null;
		if (sig.getBussedSignals() == null)
		{
			Sim.Node n = (Sim.Node)sig.getAppObject();
			name = sig.getFullName();
			n = unAlias(n);
			Sim.Node [] nodeList = new Sim.Node[1];
			nodeList[0] = n;
			int cnt = 0;
			while ((cnt <= cCount) && (comp = compareVector(nodeList, name, 1, mask, value.toString())) != 0)
			{
				cnt++;
				clockIt(1);
			}
			nodes = new Sim.Node[1];
			nodes[0] = n;
		} else
		{
			List sigsOnBus = sig.getBussedSignals();
			Sim.Node [] nodeList = new Sim.Node[sigsOnBus.size()];
			for(int i=0; i<sigsOnBus.size(); i++) nodeList[i] = (Sim.Node)((Signal)sigsOnBus.get(i)).getAppObject();

			int cnt = 0;
			while ((cnt <= cCount) && (comp = compareVector(nodeList, sig.getFullName(), sigsOnBus.size(), mask, value.toString())) != 0)
			{
				cnt++;
				clockIt(1);
			}
			name = sig.getFullName();
			nBits = sigsOnBus.size();
			nodes = nodeList;
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

	private void doV(SimVector sv)
	{
		defSequence(sv.parameters, sList);
	}

	/************************** ASSERTION COMMANDS **************************/

	/**
	 * assert a bit vector
	 */
	private void doAssert(SimVector sv)
	{
		String mask = null;
		StringBuffer value = null;
		if (sv.parameters.length == 3)
		{
			mask = sv.parameters[1];
			value = new StringBuffer(sv.parameters[2]);
		} else
		{
			value = new StringBuffer(sv.parameters[1]);
		}

		Signal sig = findName(sv.parameters[0]);
		if (sig == null)
		{
			System.out.println("ASSERT statement cannot find signal: " + sv.parameters[0]);
			return;
		}

		int comp = 0, nBits = 1;
		String name = null;
		Sim.Node [] nodes = null;
		if (sig.getBussedSignals() == null)
		{
			Sim.Node n = (Sim.Node)sig.getAppObject();
			name = n.nName;
			n = unAlias(n);
			Sim.Node [] nodeList = new Sim.Node[1];
			nodeList[0] = n;
			comp = compareVector(nodeList, name, 1, mask, value.toString());
			nodes = nodeList;
		} else
		{
			List sigsOnBus = sig.getBussedSignals();
			Sim.Node [] nodeList = new Sim.Node[sigsOnBus.size()];
			for(int i=0; i<sigsOnBus.size(); i++) nodeList[i] = (Sim.Node)((Signal)sigsOnBus.get(i)).getAppObject();
			comp = compareVector(nodeList, sig.getSignalName(), sigsOnBus.size(), mask, value.toString());
			name = sig.getSignalName();
			nBits = sigsOnBus.size();
			nodes = nodeList;
		}
		if (comp != 0)
		{
			String infstr = "";
			for(int i = 0; i < nodes.length; i++)
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

	/** keeps current AssertWhen trigger */				private Sim.Node awTrig;
	/** track pointer on the current AssertWhen list */	private AssertWhen awP;

	private void doAssertWhen(SimVector sv)
	{
		Signal sig = findName(sv.parameters[0]);
		if (sig == null)
		{
			System.out.println("ASSERTWHEN statement cannot find signal: " + sv.parameters[0]);
			return;
		}

		if (sig.getBussedSignals() == null)
		{
			Sim.Node n = (Sim.Node)sig.getAppObject();
			n = unAlias(n);
			awTrig = n;
			awTrig.awPot = (short)chToPot(sv.parameters[1].charAt(0));

			Signal oSig = findName(sv.parameters[2]);
			if (oSig == null)
			{
				System.out.println("ASSERTWHEN statement cannot find other signal: " + sv.parameters[2]);
				return;
			}

			setupAssertWhen((Sim.Node)oSig.getAppObject(), commandName(sv.command));
		} else
		{
			System.out.println("trigger to assertWhen " + sv.parameters[0] + " can't be a vector");
		}
	}

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

	/************************** IRSIM INTERFACE **************************/

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

	/**
	 * Update the trace window so that endT is shown.  If the update fits in the
	 * window, simply draw the missing parts.  Otherwise scroll the traces,
	 * centered around endT.
	 */
	public void updateWindow(long endT)
	{
		long lastT = lastTime;
		lastTime = endT;

		if (endT <= endTime)
		{
			if (lastT >= startTime)
				drawTraces(lastT, endT);
			else if (endT > startTime)
				drawTraces(startTime, endT);
		} else					// endT > endTime
		{
			if (lastT < endTime)
				drawTraces(lastT, endTime);
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
		Stimuli sd = ww.getSimData();
		for(Iterator it = sd.getBussedSignals().iterator(); it.hasNext(); )
		{
			Signal sig = (Signal)it.next();
			if ((sig.flags & which) == 0) continue;
			int i;
			List sigsOnBus = sig.getBussedSignals();
			boolean found = false;
			for(Iterator sIt = sigsOnBus.iterator(); sIt.hasNext(); )
			{
				Signal bSig = (Signal)sIt.next();
				Sim.Node n = (Sim.Node)bSig.getAppObject();
				if (n.getTime() == theSim.curDelta)
					{ found = true;   break; }
			}
			if (found)
				dVec(sig);
		}
	}

	/************************** SUPPORT **************************/

	private void initRSim()
	{
		xClock = new ArrayList();
		maxClock = 0;
		column = 0;
		analyzerON = false;
		firstVector = null;
		for(int i = 0; i < 5; i++) listTbl[i] = null;
		listTbl[Sim.inputNumber(Sim.H_INPUT)] = hInputs;
		listTbl[Sim.inputNumber(Sim.L_INPUT)] = lIinputs;
		listTbl[Sim.inputNumber(Sim.U_INPUT)] = uInputs;
		listTbl[Sim.inputNumber(Sim.X_INPUT)] = xInputs;
	}

	/**
	 * Initialize the display times so that when first called the last time is
	 * shown on the screen.  Default width is DEF_STEPS (simulation) steps.
	 */
	private void initTimes(long firstT, long stepSze, long lastT)
	{
		firstTime = firstT;
		lastTime = lastT;
		stepsTime = 4 * stepSze;

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
				endTime = lastT + 2 * stepSze;
				startTime = endTime - stepsTime;
				if (startTime < firstTime)
				{
					stepSze = firstTime - startTime;
					startTime += stepSze;
					endTime += stepSze;
				}
			}
		}
	}

	/**
	 * Update the cache (begining of window and cursor) for traces that just
	 * became visible (or were just added).
	 */
	private void updateTraceCache()
	{
		long startT = startTime;
		long cursT = firstTime;
		for(Iterator it = theSim.getNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node nd = (Sim.Node)it.next();

			Sim.HistEnt p = nd.wind;
			Sim.HistEnt h = nd.cursor;
			Sim.HistEnt nextH = h.getNextHist();
			if (h.hTime > cursT || nextH.hTime <= cursT)
			{
				if (p.hTime <= cursT)
					nd.cursor = p;
				else
					nd.cursor = nd.head;
			}

			if (startT <= p.hTime)
				p = nd.head;

			h = p.getNextHist();
			while (h.hTime < startT)
			{
				p = h;
				h = h.getNextHist();
			}
			nd.wind = p;

			p = nd.cursor;
			h = p.getNextHist();
			while (h.hTime <= cursT)
			{
				p = h;
				h = h.getNextHist();
			}
			nd.cursor = p;
		}
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
			for(Iterator it = theSim.getNodeList().iterator(); it.hasNext(); )
			{
				Sim.Node nd = (Sim.Node)it.next();
				Sim.HistEnt p = begin ? nd.head : nd.wind;
				Sim.HistEnt h = p.getNextHist();
				while (h.hTime < startT)
				{
					p = h;
					h = h.getNextHist();
				}
				nd.wind = p;
			}
			lastStart = startTime;
		}

		for(Iterator it = theSim.getNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node nd = (Sim.Node)it.next();
			if (nd.sig == null) continue;

			if (t1 >= lastTime) continue;
			Sim.HistEnt h = nd.wind;
			if (h == null) continue;
			int count = 0;
			long curT = 0;
			long endT = t2;
			while (curT < endT)
			{
				int val = h.val;
				while (h.hTime < endT && h.val == val)
					h = h.getNextHist();

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
			nd.sig.setTimeVector(timeVector);
			nd.sig.setStateVector(stateVector);
		}
		ww.repaint();
	}

	/**
	 * set/clear input status of node and add/remove it to/from corresponding list.
	 */
	private void setIn(Sim.Node n, char wChar)
	{
		while((n.nFlags & Sim.ALIAS) != 0)
			n = n.nLink;

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
					return;
			}
		}
	}

	private boolean wasInP(Sim.Node n, int p)
	{
		return (n.nFlags & Sim.INPUT) != 0 && n.nPot == p;
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
	 * display node/vector values in display list
	 */
	private void pnWatchList()
	{
		theSim.getModel().printPendingEvents();
	}

	/**
	 * just in case node appears in more than one bit vector, run through all
	 * the vectors being traced and make sure the flag is set for each node.
	 */
	private void setVecNodes(int flag)
	{
		Stimuli sd = ww.getSimData();
		for(Iterator it = sd.getBussedSignals().iterator(); it.hasNext(); )
		{
			Signal sig = (Signal)it.next();
			if ((sig.flags & flag) != 0)
			{
				List sigsOnBus = sig.getBussedSignals();
				for(Iterator sIt = sigsOnBus.iterator(); sIt.hasNext(); )
				{
					Signal bSig = (Signal)sIt.next();
					Sim.Node n = (Sim.Node)bSig.getAppObject();
					n.nFlags |= flag;
				}
			}
		}
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

	private Signal findName(String name)
	{
		Stimuli sd = ww.getSimData();
		for(Iterator it = sd.getSignals().iterator(); it.hasNext(); )
		{
			Signal sig = (Signal)it.next();
			if (sig.getFullName().equals(name)) return sig;
		}
		return null;
	}

	/**
	 * display bit vector.
	 */
	private void dVec(Signal sig)
	{
		List sigsOnBus = sig.getBussedSignals();
		int i = sig.getSignalName().length() + 2 + sigsOnBus.size();
		if (column + i >= MAXCOL)
		{
			column = 0;
		}
		column += i;
		String bits = "";
		for(Iterator sIt = sigsOnBus.iterator(); sIt.hasNext(); )
		{
			Signal bSig = (Signal)sIt.next();
			Sim.Node n = (Sim.Node)bSig.getAppObject();
			bits += Sim.vChars.charAt(n.nPot);
		}

		System.out.println(sig.getSignalName() + "=" + bits + " ");
	}

	/**
	 * Settle network until the specified stop time is reached.
	 * Premature returns (before stop time) indicate that a node/vector whose
	 * stop-bit set has just changed value.
	 */
	private long relax(long stopTime)
	{
		for(;;)
		{
			boolean repeat = theSim.getModel().step(stopTime);
			if (!repeat) break;
		}

		return theSim.curDelta - stopTime;
	}

	/**
	 * set each node/vector in a clock sequence to its next value
	 */
	private void vecValue(int index)
	{
		for(Iterator it = xClock.iterator(); it.hasNext(); )
		{
			Sequence cs = (Sequence)it.next();
			String v = cs.values[index % cs.values.length];
			if (cs.sig.getBussedSignals() == null)
			{
				Sim.Node n = (Sim.Node)cs.sig.getAppObject();
				setIn(n, v.charAt(0));
			} else
			{
				List sigsOnBus = cs.sig.getBussedSignals();
				for(int i=0; i<sigsOnBus.size(); i++)
				{
					Sim.Node n = (Sim.Node)((Signal)sigsOnBus.get(i)).getAppObject();
					setIn(n, v.charAt(i));
				}
			}
		}
	}

	/**
	 * process command line to yield a sequence structure.  first arg is the
	 * name of the node/vector for which the sequence is to be defined, second
	 * and following args are the values.
	 */
	private void defSequence(String [] args, List list)
	{
		// if no arguments, get rid of all the sequences we have defined
		if (args.length == 0)
		{
			list.clear();
			return;
		}

		Signal sig = findName(args[0]);
		if (sig == null)
		{
			System.out.println(args[0] + ": No such node or vector");
			return;
		}
		int len = 1;
		if (sig.getBussedSignals() != null)
			len = sig.getBussedSignals().size();

		Sim.Node n = (Sim.Node)sig.getAppObject();
		if (sig.getBussedSignals() == null)
		{
			n = unAlias(n);
			if ((n.nFlags & Sim.MERGED) != 0)
			{
				System.out.println(n.nName + " can't be part of a sequence");
				return;
			}
		}

		if (args.length == 1)	// just destroy the given sequence
		{
			list.remove(sig);
			return;
		}

		// make sure each value specification is the right length
		for(int i = 1; i < args.length; i++)
			if (args[i].length() != len)
		{
			System.out.println("value \"" + args[i] + "\" is not compatible with size of " + args[0] + " (" + len + ")");
			return;
		}

		Sequence s = new Sequence();
		s.values = new String[args.length-1];
		s.sig = sig;
		sig.setAppObject(n);

		// process each value specification saving results in sequence
		for(int i = 1; i < args.length; i++)
		{
			StringBuffer sb = new StringBuffer();
			for(int p=0; p<args[i].length(); p++)
			{
				sb.append(potChars.charAt(chToPot(args[i].charAt(p))));
			}
			s.values[i-1] = sb.toString();
		}

		// all done!  remove any old sequences for this node or vector.
		list.remove(sig);

		// insert result onto list
		xClock.add(s);
	}

	private int whichPhase = 0;

	/**
	 * Step each clock node through one simulation step
	 */
	private boolean stepPhase()
	{
		vecValue(whichPhase++);
		if (relax(theSim.curDelta + stepSize) != 0) return true;
		return false;
	}

	/**
	 * clock circuit specified number of times
	 */
	private boolean clockIt(int n)
	{
		int  i = 0;

		if (xClock.size() == 0)
		{
			System.out.println("no clock nodes defined!");
			return false;
		}

		/* run 'em by setting each clock node to successive values of its
		 * associated sequence until all phases have been run.
		 */
		boolean interrupted = false;
		for(int cycle = 0; cycle < n; cycle++)
		{
			for(i = 0; i < maxClock; i += 1)
			{
				if (stepPhase())
				{
					interrupted = true;
					break;
				}
			}
			if (interrupted) break;
		}

		// finally display results if requested to do so
		pnWatchList();
		return interrupted;
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
			listTbl[i].clear();
		}
		for(Iterator it = theSim.getNodeList().iterator(); it.hasNext(); )
		{
			Sim.Node n = (Sim.Node)it.next();
			if ((n.nFlags & Sim.POWER_RAIL) == 0)
				n.nFlags &= ~Sim.INPUT;
		}
	}
}
