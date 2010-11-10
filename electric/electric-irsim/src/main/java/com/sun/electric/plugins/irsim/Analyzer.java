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

import com.sun.electric.tool.simulation.DigitalSample;
import com.sun.electric.tool.simulation.MutableSignal;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.DigitalSample.Strength;
import com.sun.electric.tool.simulation.DigitalSample.Value;
import com.sun.electric.tool.simulation.irsim.IAnalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The Analyzer class is the top-level class for IRSIM simulation.
 * By creating an Analyzer object, all other simulation objects are defined.
 *
 * Analyzers have Sim objects in them.
 * Sim objects have Config and Eval objects in them.
 */
public class Analyzer implements IAnalyzer.EngineIRSIM, SimAPI.Analyzer
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
	/** number of buckets in histogram */				private static final int    NBUCKETS      = 20;
	/** maximum width of print line */					private static final int	MAXCOL        = 80;

	/** set of potential characters */					private static final String potChars = "luxh.";
	/** time values in command file are in ns */		private static final double cmdFileUnits = 0.000000001;

	/**
	 * Class that defines a single low-level IRSIM control command.
	 */
	private static class SimVector
	{
		/** index of command */						int           command;
		/** parameters to the command */			String []     parameters;
		/** actual signals named in command */		List<Signal<DigitalSample>> sigs;
		/** negated signals named in command */		List<Signal<DigitalSample>> sigsNegated;
		/** duration of step, where appropriate */	double        value;
		/** next in list of vectors */				SimVector     next;
	}

	public class AssertWhen implements Runnable
	{
		/** which node we will check */				SimAPI.Node       node;
		/** what value has the node */				char	       val;
		/** next in list of assertions */			AssertWhen     nxt;
        
        public void run() {
            evalAssertWhen(node);
        }
	}

	private static class Sequence
	{
		/** signal to control */					Signal<DigitalSample>  sig;
		/** array of values */						String  []     values;
	}

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
	private DigitalSample [] traceState;
	private int       traceTotal = 0;

	/** vectors which make up clock */				private List<Sequence> xClock;
	/** vectors which make up a sequence */			private List<Sequence> sList;
	/** longest clock sequence defined */			private int		       maxClock = 0;
	/** current output column */					private int		       column = 0;

	private List<SimAPI.Node>  [] listTbl = new List[5];

	/** list of nodes to be driven high */			public List<SimAPI.Node>   hInputs = new ArrayList<SimAPI.Node>();
	/** list of nodes to be driven low */			public List<SimAPI.Node>   lIinputs = new ArrayList<SimAPI.Node>();
	/** list of nodes to be driven X */				public List<SimAPI.Node>   uInputs = new ArrayList<SimAPI.Node>();
	/** list of nodes to be removed from input */	public List<SimAPI.Node>   xInputs = new ArrayList<SimAPI.Node>();

	/** set when analyzer is running */				public boolean          analyzerON;
    /** irDebug preferneces */                      public int              irDebug;
    /** show IRSIM commands */                      public boolean          showCommands;

	/** the simulation engine */					private final SimAPI    theSim;
    /** the GUI interface */                        private final IAnalyzer.GUI gui;
	/** mapping from signals to nodes */			private HashMap<Signal<?>,SimAPI.Node> nodeMap;
	/** mapping from nodes to signals */			private HashMap<SimAPI.Node,MutableSignal<DigitalSample>> signalMap = new HashMap<SimAPI.Node,MutableSignal<DigitalSample>>();

	/************************** ELECTRIC INTERFACE **************************/

	Analyzer(IAnalyzer.GUI gui, SimAPI sim, int irDebug, boolean showCommands)
	{
        this.gui = gui;
        this.irDebug = irDebug;
        this.showCommands = showCommands;
		theSim = sim;
        theSim.setDebug(irDebug);
        theSim.setAnalyzer(this);
	}
 
    public static IAnalyzer getInstance() {
        return new IAnalyzer() {
            /**
             * Create IRSIM Simulation Engine to simulate a cell.
             * @param gui interface to GUI
             * @param steppingModel stepping model either "RC" or "Linear"
             * @param parameterURL URL of IRSIM parameter file
             * @param irDebug debug flags
             * @param showCommands tru to print issued IRSIM commands
             * @param isDelayedX true if using the delayed X model, false if using the old fast-propagating X model.
             */
            public EngineIRSIM createEngine(IAnalyzer.GUI gui, String steppingModel, URL parameterURL, int irDebug, boolean showCommands, boolean isDelayedX) {
                SimAPI sim = new Sim(irDebug, steppingModel, parameterURL, isDelayedX);
                Analyzer theAnalyzer = new Analyzer(gui, sim, irDebug, showCommands);
                
                System.out.println("IRSIM, version " + simVersion);
                // now initialize the simulator
                theAnalyzer.initRSim();
                return theAnalyzer;
            }
        };
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
        theSim.putTransistor(gateName, sourceName, drainName, gateLength, gateWidth, activeArea, activePerim, centerX, centerY, isNTypeTransistor);
    }
    
    /**
     * Put resistor into the circuit
     * @param net1 name of first terminal network
     * @param net2 name of second terminal network
     * @param resistance resistance (ohm)
     */
    public void putResistor(String net1, String net2, double resistance) {
        theSim.putResistor(net1, net2, resistance);
    }

    /**
     * Put capacitor into the circuit
     * @param net1 name of first terminal network
     * @param net2 name of second terminal network
     * @param capacitance capacitance (pf)
     */
    public void putCapacitor(String net1, String net2, double capacitance) {
        theSim.putCapacitor(net1, net2, capacitance);
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
    public int inputSim(Reader simReader, String fileName) throws IOException {
        return theSim.inputSim(simReader, fileName);
    }

    /**
     * Finish initialization of the circuit.
     */
    public void finishNetwork() {
        theSim.finishNetwork();
    }
    
    /**
     * Get lambda value in nanometers
     * @return lambda in nanometers
     */
    public double getLambda() {
        return theSim.getLambda();
    }
    
    /**
     * Finish initialization
     */
	public void init()
	{
		// tell the simulator to watch all signals
		initTimes(0, 50000, theSim.getCurDelta());

		for(SimAPI.Node n : theSim.getNodes())
		{
			while (n.getFlags(SimAPI.ALIAS) != 0)
				n = n.getLink();

			if (n.getFlags(SimAPI.MERGED) != 0)
				System.out.println("can't watch node " + n.getName());
            n.setCursor(n.getHead());
			n.setWind(n.getHead());
		}
		updateWindow(0);
		lastStart = theSim.getMaxTime();
		analyzerON = true;

		// read signal values
		updateWindow(theSim.getCurDelta());
	}

    /**
     * Finish initialization of the circuit and convert Stimuli.
     */
    public void convertStimuli() 
    {
		nodeMap = new HashMap<Signal<?>,SimAPI.Node>();
		List<Signal<?>> sigList = new ArrayList<Signal<?>>();
		for(SimAPI.Node n : theSim.getNodes())
		{
			if (n.getName().equalsIgnoreCase("vdd") || n.getName().equalsIgnoreCase("gnd")) continue;

			// make a signal for it
			MutableSignal<DigitalSample> sig = gui.makeSignal(n.getName());
            signalMap.put(n, sig);
//			n.sig = sig;
			sigList.add(sig);
			nodeMap.put(sig, n);

            sig.addSample(0, DigitalSample.LOGIC_0);
            sig.addSample(0.00000001, DigitalSample.LOGIC_0);
		}

		// make bus signals from individual ones found in the list
		gui.makeBusSignals(sigList);
	}

    public void newContolPoint(String signalName, double insertTime, DigitalSample.Value value) {
        int command;
        switch (value) {
            case HIGH:
                command = VECTORH;
                break;
            case LOW:
                command = VECTORL;
                break;
            default:
                command = VECTORX;
        }
        newVector(command, new String[] {signalName}, insertTime, false);
    }
    
    /**
     * Method to show information about the currently-selected signal.
     */
    public void showSignalInfo(Signal<?> sig) {
        SimVector excl = new SimVector();
        excl.command = VECTOREXCL;
        excl.sigs = new ArrayList<Signal<DigitalSample>>();
        excl.sigs.add((Signal<DigitalSample>) sig);
        issueCommand(excl);

        excl.command = VECTORQUESTION;
        issueCommand(excl);
    }
    
	/**
	 * Method to remove all stimuli from the currently-selected signal.
     * @param sig currently selected signal.
	 */
    public void clearContolPoints(Signal<?> sig) {
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
    }
    
//	/**
//	 * Method to reload the circuit data.
//	 */
//	public void refresh(IRSIMPreferences ip)
//	{
//		// make a new simulation object
//		theSim = new Sim(this);
//
//		// now initialize the simulator
//		initRSim();
//
//		// Load network
//		loadCircuit(ip);
//		WaveformWindow.refreshSimulationData(sd, ww);
//
//		if (vectorFileName != null) loadVectorFile();
//		init();
//
//		playVectors();
//	}

	/************************** SIMULATION VECTORS **************************/

	/**
	 * Method to play the simulation vectors into the simulator.
	 */
	public void playVectors()
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
		step.value = deltaToNS(stepSize);
		issueCommand(step);
		analyzerON = true;
		updateWindow(theSim.getCurDelta());

		// update main cursor location if requested
        gui.setMainXPositionCursor(curTime);
	}

	/**
	 * Method to clear all simulation vectors.
	 */
	public void clearAllVectors()
	{
		firstVector = null;
		lastVector = null;
	}

	/**
	 * Method to restore the current stimuli information from URL.
     * @param reader Reader with stimuli information
	 */
	public void restoreStimuli(Reader reader) throws IOException
	{
		LineNumberReader lineReader = new LineNumberReader(reader);
        // remove all vectors
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
            String [] targ = theSim.parseLine(buf);
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
                if (sv == null) continue;
                currentTime += sv.value * cmdFileUnits;
                continue;
            }

            // handle changes to signals in the Waveform Window
            if (command == VECTORANALYZER)
            {
                if (!anyAnalyzerCommands)
                {
                    // clear the stimuli on the first time
                    anyAnalyzerCommands = true;
                    gui.closePanels();
                }
                List<Signal<DigitalSample>> sigs = new ArrayList<Signal<DigitalSample>>();
                getTargetNodes(targ, 1, sigs, null);
                gui.openPanel(sigs);
                continue;
            }

            // handle aggregation of signals into busses
            if (command == VECTORVECTOR)
            {
                // find this vector name in the list of vectors
//					Signal<DigitalSample> busSig = null;
//					if (busSig == null)
//						busSig = DigitalSample.createSignal(sigCollection, sd, targ[1], null);
                List<Signal<DigitalSample>> sigs = new ArrayList<Signal<DigitalSample>>();
                getTargetNodes(targ, 2, sigs, null);
                Signal<DigitalSample>[] subsigs = new Signal[sigs.size()];
                for(int i=0; i<sigs.size(); i++) subsigs[i] = sigs.get(i);
                gui.createBus(targ[1], subsigs);
                continue;
            }
        }
        playVectors();
        updateWindow(theSim.getCurDelta());
	}

	/**
	 * Method to save the current stimuli information to disk.
     * @param stimuliFile file to save stimuli information
	 */
	public void saveStimuli(File stimuliFile) throws IOException 
	{
		if (stimuliFile == null)
            throw new NullPointerException();
		PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(stimuliFile)));
		try
		{

			for(SimVector sv = firstVector; sv != null; sv = sv.next)
			{
				String infstr = commandName(sv.command);
				for(int i=0; i<sv.parameters.length; i++)
					infstr += " " + sv.parameters[i];
				printWriter.println(infstr);
			}
		} finally
        {
            printWriter.close();
        }
	}

	private void issueCommand(SimVector sv)
	{
		if (showCommands)
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
			case VECTORCLOCK:      setAClock(sv);      break;
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
				double newSize = deltaToNS(stepSize);
				if (params.length > 0)
				{
					newSize = Electric.atof(params[0]);
					long lNewSize = nsToDelta(newSize);
					if (lNewSize <= 0)
					{
						System.out.println("Bad step size: " + Electric.formatDouble(newSize*1000) + "psec (must be 10 psec or larger), ignoring");
						return null;
					}
				}
				newsv.value = newSize;
				break;
			case VECTORSTEPSIZE:
				if (params.length > 0)
					stepSize = nsToDelta(Electric.atof(params[0]));
				break;

			case VECTORBACK:
				newsv.value = 0;
				if (params.length > 0)
				{
					newsv.value = Electric.atof(params[0]);
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
				newsv.sigs = new ArrayList<Signal<DigitalSample>>();
				newsv.sigsNegated = null;
				if (command == VECTORT) newsv.sigsNegated = new ArrayList<Signal<DigitalSample>>();
				getTargetNodes(params, 0, newsv.sigs, newsv.sigsNegated);

				if (command == VECTORL || command == VECTORH || command == VECTORX)
				{
					// add this moment in time to the control points for the signal
					for(Signal<?> sig : newsv.sigs)
					{
						sig.addControlPoint(insertTime);
					}
				}
				break;
		}

		// insert the vector
		SimVector lastSV = null;
		if (justAppend || insertTime < 0.0)
		{
			lastSV = lastVector;
		} else
		{
			double defaultStepSize = 10.0 * cmdFileUnits;
			double curTime = 0.0;
			int clockPhases = 0;
			for(SimVector sv = firstVector; sv != null; sv = sv.next)
			{
				switch (sv.command)
				{
					case VECTORS:
						double stepSze = sv.value * cmdFileUnits;
						long ss = nsToDelta(((curTime + stepSze) - insertTime) / cmdFileUnits);
						if (ss != 0 && Electric.doublesLessThan(insertTime, curTime+stepSze))
						{
							// splitting step at "insertTime"
							sv.parameters = new String[1];
							sv.value = (insertTime-curTime) / cmdFileUnits;
							sv.parameters[0] = Electric.formatDouble(sv.value);

							// create second step to advance after this signal
							SimVector afterSV = new SimVector();
							afterSV.command = VECTORS;
							afterSV.parameters = new String[1];
							afterSV.value = (curTime + stepSze - insertTime) / cmdFileUnits;
							afterSV.parameters[0] = Electric.formatDouble(afterSV.value);
							afterSV.next = sv.next;
							sv.next = afterSV;
						}
						curTime += stepSze;
						break;
					case VECTORSTEPSIZE:
						if (sv.parameters.length > 0)
							defaultStepSize = Electric.atof(sv.parameters[0]) * cmdFileUnits;
						break;
					case VECTORCLOCK:
						clockPhases = sv.parameters.length - 1;
						break;
					case VECTORC:
						int mult = 1;
						if (sv.parameters.length > 0)
							mult = Electric.atoi(sv.parameters[0]);
						curTime += defaultStepSize * clockPhases * mult;
						break;
					case VECTORP:
						curTime += defaultStepSize;
						break;
				}
				lastSV = sv;
				if (!Electric.doublesLessThan(curTime, insertTime)) break;
			}
			if (Electric.doublesLessThan(curTime, insertTime))
			{
				// create step to advance to the insertion time
				double thisStep = (insertTime-curTime) / cmdFileUnits;
				if (thisStep > 0.005)
				{
					SimVector afterSV = new SimVector();
					afterSV.command = VECTORS;
					afterSV.parameters = new String[1];
					afterSV.parameters[0] = Electric.formatDouble(thisStep);
//					afterSV.parameters[0] = Electric.formatDouble(thisStep / cmdFileUnits);
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

	/**
	 * Method to remove the selected stimuli.
	 * @return true if stimuli were deleted.
	 */
	public boolean clearControlPoint(Signal<?> sig, double insertTime)
	{
		double defaultStepSize = 10.0 * cmdFileUnits;
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
					if (sv.value != 0) stepSze = sv.value * cmdFileUnits;
					curTime += stepSze;
					break;
				case VECTORSTEPSIZE:
					if (sv.parameters.length > 0)
						defaultStepSize = Electric.atof(sv.parameters[0]) * cmdFileUnits;
					break;
				case VECTORCLOCK:
					clockPhases = sv.parameters.length - 1;
					break;
				case VECTORC:
					int mult = 1;
					if (sv.parameters.length > 0)
						mult = Electric.atoi(sv.parameters[0]);
					curTime += defaultStepSize * clockPhases * mult;
					break;
				case VECTORP:
					curTime += defaultStepSize;
					break;
				case VECTORL:
				case VECTORH:
				case VECTORX:
					if (Electric.doublesEqual(insertTime, curTime))
					{
						boolean found = false;
						for(Signal<?> s : sv.sigs)
						{
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
			if (Electric.doublesLessThan(insertTime, curTime)) break;
		}
		return cleared;
	}

	private void getTargetNodes(String [] params, int low, List<Signal<DigitalSample>> normalList, List<Signal<DigitalSample>> negatedList)
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
				for(Signal<?> sig : gui.getSignals())
				{
					if (strMatch(name, sig.getFullName()))
					{
						if (negated) negatedList.add((Signal<DigitalSample>)sig); else
							normalList.add((Signal<DigitalSample>)sig);
					}
				}
			} else
			{
				// no wildcards: just find the name
				Signal<DigitalSample> sig = this.findName(name);
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
		double defaultStepSize = 10.0 * cmdFileUnits;
		double curTime = 0.0;
		int clockPhases = 0;
		for(SimVector sv = firstVector; sv != null; sv = sv.next)
		{
			switch (sv.command)
			{
				case VECTORS:
					double stepSze = defaultStepSize;
					if (sv.value != 0) stepSze = sv.value * cmdFileUnits;
					curTime += stepSze;
					break;
				case VECTORSTEPSIZE:
					if (sv.parameters.length > 0)
						defaultStepSize = Electric.atof(sv.parameters[0]) * cmdFileUnits;
					break;
				case VECTORCLOCK:
					clockPhases = sv.parameters.length - 1;
					break;
				case VECTORC:
					int mult = 1;
					if (sv.parameters.length > 0)
						mult = Electric.atoi(sv.parameters[0]);
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
		for(Signal<?> sig : sv.sigs)
		{
			SimAPI.Node n = nodeMap.get(sig);
			if (n == null) continue;

			String name = n.getName();
			while(n.getFlags(SimAPI.ALIAS) != 0)
				n = n.getLink();

			if (n.getFlags(SimAPI.MERGED) != 0)
			{
				System.out.println(name + " => node is inside a transistor stack");
				return;
			}

			String infstr = "";
			infstr += pValue(name, n);
            infstr += n.describeDelay();
			System.out.println();

			infstr = "";
			if (sv.command == VECTOREXCL)
			{
				infstr += "is computed from:";
				for(SimAPI.Trans t : n.getTerms())
				{
					infstr += "  ";
					if (irDebug == 0)
					{
						String drive = null;
						SimAPI.Node rail = t.getDrain().getFlags(SimAPI.POWER_RAIL) != 0 ? t.getDrain() : t.getSource();
						if (t.getBaseType() == SimAPI.NCHAN && rail == theSim.getGroundNode())
							drive = "pulled down by ";
						else if (t.getBaseType() == SimAPI.PCHAN && rail == theSim.getPowerNode())
							drive = "pulled up by ";
						else if (t.getBaseType() == SimAPI.DEP && rail == theSim.getPowerNode() &&
							t.getOtherNode(rail) == t.getGate())
								drive = "pullup ";
						else
							infstr += pTrans(t);

						if (drive != null)
						{
							infstr += drive;
							infstr += pGValue(t);
							infstr += prTRes(t);
							if (t.getLink() != t && (theSim.getReport() & SimAPI.REPORT_TCOORD) != 0)
								infstr += " <" + t.getX() + "," + t.getY() + ">";
						}
					} else
					{
						infstr += pTrans(t);
					}
				}
			} else
			{
				infstr += "affects:";
				for(SimAPI.Trans t : n.getGates())
				{
					infstr += pTrans(t);
				}
			}
			System.out.println(infstr);

            String[] pendingEvents = n.describePendingEvents();
			if (pendingEvents != null)
			{
				System.out.println("Pending events:");
				for(String s: pendingEvents)
					System.out.println(s);
			}
		}
	}

	/**
	 * print histogram of circuit activity in specified time interval
	 */
	private void doActivity(SimVector sv)
	{
		long begin = nsToDelta(Electric.atof(sv.parameters[0]));
		long end = theSim.getCurDelta();
		if (sv.parameters.length > 1)
			end = nsToDelta(Electric.atof(sv.parameters[1]));
		if (end < begin)
		{
			long swp = end;   end = begin;   begin = swp;
		}

		// collect histogram info by walking the network
		long [] table = new long[NBUCKETS];
		for(int i = 0; i < NBUCKETS; table[i++] = 0);

		long size = (end - begin + 1) / NBUCKETS;
		if (size <= 0) size = 1;

		for(SimAPI.Node n : theSim.getNodes())
		{
			if (n.getFlags(SimAPI.ALIAS | SimAPI.MERGED | SimAPI.POWER_RAIL) == 0)
			{
				if (n.getTime() >= begin && n.getTime() <= end)
					table[(int)((n.getTime() - begin) / size)] += 1;
			}
		}

		// print out what we found
		int total = 0;
		for(int i = 0; i < NBUCKETS; i++) total += table[i];

		System.out.println("Histogram of circuit activity: " + deltaToNS(begin) +
			" . " + deltaToNS(end) + "ns (bucket size = " + deltaToNS(size) + ")");

		for(int i = 0; i < NBUCKETS; i += 1)
			System.out.println(" " + deltaToNS(begin + (i * size)) + " -" + deltaToNS(begin + (i + 1) * size) + table[i]);
	}

	/**
	 * Print nodes that are aliases
	 */
	private void doPrintAlias()
	{
		if (theSim.getNumAliases() == 0)
			System.out.println("there are no aliases");
		else
		{
			System.out.println("there are " + theSim.getNumAliases() + " aliases:");
			for(SimAPI.Node n : theSim.getNodes())
			{
				if (n.getFlags(SimAPI.ALIAS) != 0)
				{
					n = unAlias(n);
					String is_merge = n.getFlags(SimAPI.MERGED) != 0 ? " (part of a stack)" : "";
					System.out.println("  " + n.getName() + " . " + n.getName() + is_merge);
				}
			}
		}
	}

	/**
	 * Move back simulation time to specified time.
	 */
	private void doBack(SimVector sv)
	{
		long newT = nsToDelta(sv.value);
		if (newT < 0 || newT > theSim.getCurDelta())
		{
			System.out.println(sv.value + ": invalid time in BACK command");
			return;
		}

		theSim.setCurDelta(newT);
		clearInputs();
		theSim.backSimTime(theSim.getCurDelta(), 0);
		theSim.clearCurNode();			// fudge
		for(SimAPI.Node n : theSim.getNodes())
		{
			theSim.backToTime(n);
		}
		if (theSim.getCurDelta() == 0)
			theSim.reInit();

		if (analyzerON)
		{
			for(SimAPI.Node n : theSim.getNodes())
			{
                n.setCursor(n.getHead());
				n.setWind(n.getHead());
			}

			// should set "theSim.curDelta" to the width of the screen
			initTimes(0, stepsTime / DEF_STEPS, theSim.getCurDelta());
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
			n = Electric.atoi(sv.parameters[0]);
			if (n <= 0) n = 1;
		}

		clockIt(n);		// do the hard work
	}

	/**
	 * Print list of nodes which last changed value in specified time interval
	 */
	private void doChanges(SimVector sv)
	{
		long begin = nsToDelta(Electric.atof(sv.parameters[0]));
		long end = theSim.getCurDelta();
		if (sv.parameters.length > 1)
			end = nsToDelta(Electric.atof(sv.parameters[1]));

		column = 0;
		System.out.print("Nodes with last transition in interval " + deltaToNS(begin) + " . " + deltaToNS(end) + "ns:");

		for(SimAPI.Node n : theSim.getNodes())
		{
			n = unAlias(n);

			if (n.getFlags(SimAPI.MERGED | SimAPI.ALIAS) != 0) return;

			if (n.getTime() >= begin && n.getTime() <= end)
			{
				int i = n.getName().length() + 2;
				if (column + i >= MAXCOL)
				{
					column = 0;
				}
				column += i;
				System.out.print("  " + n.getName());
			}
		}
		System.out.println();
	}

	/**
	 * define clock sequences(s)
	 */
	private void setAClock(SimVector sv)
	{
		// process sequence and add to clock list
		defSequence(sv.parameters, xClock);

		// compute the maximum clock size
		maxClock = 0;
		for(Sequence t : xClock)
		{
			if (t.values.length > maxClock) maxClock = t.values.length;
		}
	}

	private void doDebug(SimVector sv)
	{
		if (sv.parameters.length <= 0) irDebug = 0; else
		{
			for(int i=0; i<sv.parameters.length; i++)
			{
				if (sv.parameters[i].equalsIgnoreCase("ev"))
				{
					irDebug |= SimAPI.DEBUG_EV;
				} else if (sv.parameters[i].equalsIgnoreCase("dc"))
				{
					irDebug |= SimAPI.DEBUG_DC;
				} else if (sv.parameters[i].equalsIgnoreCase("tau"))
				{
					irDebug |= SimAPI.DEBUG_TAU;
				} else if (sv.parameters[i].equalsIgnoreCase("taup"))
				{
					irDebug |= SimAPI.DEBUG_TAUP;
				} else if (sv.parameters[i].equalsIgnoreCase("spk"))
				{
					irDebug |= SimAPI.DEBUG_SPK;
				} else if (sv.parameters[i].equalsIgnoreCase("tw"))
				{
					irDebug |= SimAPI.DEBUG_TW;
				} else if (sv.parameters[i].equalsIgnoreCase("all"))
				{
					irDebug = SimAPI.DEBUG_EV | SimAPI.DEBUG_DC | SimAPI.DEBUG_TAU | SimAPI.DEBUG_TAUP | SimAPI.DEBUG_SPK | SimAPI.DEBUG_TW;
				} else if (sv.parameters[i].equalsIgnoreCase("off"))
				{
					irDebug = 0;
				}
			}
		}
        theSim.setDebug(irDebug);

		System.out.print("Debugging");
		if (irDebug == 0) System.out.println(" OFF"); else
		{
			if ((irDebug&SimAPI.DEBUG_EV) != 0) System.out.print(" event-scheduling");
			if ((irDebug&SimAPI.DEBUG_DC) != 0) System.out.print(" final-value-computation");
			if ((irDebug&SimAPI.DEBUG_TAU) != 0) System.out.print(" tau/delay-computation");
			if ((irDebug&SimAPI.DEBUG_TAUP) != 0) System.out.print(" tauP-computation");
			if ((irDebug&SimAPI.DEBUG_SPK) != 0) System.out.print(" spike-analysis");
			if ((irDebug&SimAPI.DEBUG_TW) != 0) System.out.print(" tree-walk");
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
			if (theSim.getDecay() == 0)
				System.out.println("decay = No decay");
			else
				System.out.println("decay = " + deltaToNS(theSim.getDecay()) + "ns");
		} else
		{
			theSim.setDecay(nsToDelta(Electric.atof(sv.parameters[0])));
			if (theSim.getDecay() < 0)
				theSim.setDecay(0);
		}
	}

	/**
	 * Set value of a node/vector to the requested value (hlux).
	 */
	private void doSetValue(SimVector sv)
	{
		if (sv.sigs == null) return;

		for(Signal<DigitalSample> sig : sv.sigs)
		{
			SimAPI.Node n = nodeMap.get(sig);
			setIn(n, commandName(sv.command).charAt(0));
		}
	}

	/**
	 * display current inputs
	 */
	private void doInputs()
	{
		SimAPI.Node [] inpTbl = new SimAPI.Node[SimAPI.N_POTS];

		inpTbl[SimAPI.HIGH] = inpTbl[SimAPI.LOW] = inpTbl[SimAPI.X] = null;
		for(SimAPI.Node n : theSim.getNodes())
		{
			if (n.getFlags(SimAPI.INPUT|SimAPI.ALIAS|SimAPI.POWER_RAIL|SimAPI.VISITED|SimAPI.INPUT_MASK) == SimAPI.INPUT)
			{
				n.setNext(inpTbl[n.getPot()]);
				inpTbl[n.getPot()] = n;
				n.setFlags(SimAPI.VISITED);
			}
		}

		System.out.print("h inputs:");
		for(SimAPI.Node n : hInputs)
		{
			System.out.print(" " + n.getName());
		}
		for(SimAPI.Node n = inpTbl[SimAPI.HIGH]; n != null; n.clearFlags(SimAPI.VISITED), n = n.getNext())
			System.out.print(" " + n.getName());
		System.out.println();

		System.out.print("l inputs:");
		for(SimAPI.Node n : lIinputs)
		{
			System.out.print(" " + n.getName());
		}
		for(SimAPI.Node n = inpTbl[SimAPI.LOW]; n != null; n.clearFlags(SimAPI.VISITED), n = n.getNext())
			System.out.print(" " + n.getName());
		System.out.println();

		System.out.println("u inputs:");
		for(SimAPI.Node n : uInputs)
		{
			System.out.println(" " + n.getName());
		}
		for(SimAPI.Node n = inpTbl[SimAPI.X]; n != null; n.clearFlags(SimAPI.VISITED), n = n.getNext())
			System.out.println(" " + n.getName());
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
	 * discover and print critical path for node's last transition
	 */
	private void doPath(SimVector sv)
	{
		if (sv.sigs == null) return;
		for(Signal<DigitalSample> sig : sv.sigs)
		{
			SimAPI.Node n = nodeMap.get(sig);
			System.out.println("Critical path for last transition of " + n.getName() + ":");
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
		for(SimAPI.Node n : theSim.getNodes())
		{
			n = unAlias(n);

			if (n.getFlags(SimAPI.MERGED | SimAPI.ALIAS) == 0 && n.getPot() == SimAPI.X)
			{
				int i = n.getName().length() + 2;
				if (column + i >= MAXCOL)
				{
					column = 0;
				}
				column += i;
				System.out.print("  " + n.getName());
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
			n = Electric.atoi(sv.parameters[0]);
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
		for(Sequence cs : sList)
		{
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
		if (sv.parameters[0].equalsIgnoreCase("decay")) theSim.setReport(SimAPI.REPORT_DECAY); else
			if (sv.parameters[0].equalsIgnoreCase("delay")) theSim.setReport(SimAPI.REPORT_DELAY); else
				if (sv.parameters[0].equalsIgnoreCase("tau")) theSim.setReport(SimAPI.REPORT_TAU); else
					if (sv.parameters[0].equalsIgnoreCase("tcoord")) theSim.setReport(SimAPI.REPORT_TCOORD); else
						if (sv.parameters[0].equalsIgnoreCase("none")) theSim.clearReport();
		System.out.print("Report");
		if (theSim.getReport() == 0) System.out.println(" NONE"); else
		{
			if ((theSim.getReport()&SimAPI.REPORT_DECAY) != 0) System.out.print(" decay");
			if ((theSim.getReport()&SimAPI.REPORT_DELAY) != 0) System.out.print(" delay");
			if ((theSim.getReport()&SimAPI.REPORT_TAU) != 0) System.out.print(" tau");
			if ((theSim.getReport()&SimAPI.DEBUG_TAUP) != 0) System.out.print(" tauP");
			if ((theSim.getReport()&SimAPI.REPORT_TCOORD) != 0) System.out.print(" tcoord");
			System.out.println();
		}
	}

	/**
	 * relax network, optionally set stepsize
	 */
	private void doStep(SimVector sv)
	{
		double newSize = sv.value;
		long lNewSize = nsToDelta(newSize);
		if (lNewSize <= 0) return;
		relax(theSim.getCurDelta() + lNewSize);
		pnWatchList();
	}

	/**
	 * set bit vector
	 */
	private void doSet(SimVector sv)
	{
		Signal<DigitalSample> sig = findName(sv.parameters[0]);
		if (sig == null)
		{
			System.out.println("Cannot find signal: " + sv.parameters[0]);
			return;
		}

		Signal<?>[] sigsOnBus = sig.getBusMembers();
		if (sigsOnBus == null)
		{
			System.out.println("Signal: " + sv.parameters[0] + " is not a bus");
			return;
		}
		if (sigsOnBus.length != sv.parameters[1].length())
		{
			System.out.println("Wrong number of bits for this vector");
			return;
		}
		for(int i = 0; i < sigsOnBus.length; i++)
		{
			SimAPI.Node n = nodeMap.get(sigsOnBus[i]);
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
				for(SimAPI.Node n : theSim.getNodes())
				{
					if (n.getFlags(SimAPI.ALIAS | SimAPI.POWER_RAIL) == 0)
					{
						tranCntNG += n.getGates().size();
						tranCntNSD += n.getTerms().size();
					}
				}
				System.out.println("avg: # gates/node = " + Electric.formatDouble(tranCntNG / theSim.getNumNodes()) +
					",  # src-drn/node = " + Electric.formatDouble(tranCntNSD / theSim.getNumNodes()));
			}
		}
		System.out.println("changes = " + theSim.getNumEdges());
		System.out.println("punts (cns) = " + theSim.getNumPunted() + " (" + theSim.getNumConsPunted() + ")");
		String n1 = "0.0";
		String n2 = "0.0";
		if (theSim.getNumPunted() != 0)
		{
			n1 = Electric.formatDouble(100.0 / (theSim.getNumEdges() / theSim.getNumPunted() + 1.0));
			n2 = Electric.formatDouble(theSim.getNumConsPunted() * 100.0 / theSim.getNumPunted());
		}
		System.out.println("punts = " + n1 + "%, cons_punted = " + n2 + "%");

		System.out.println("nevents = " + theSim.getNumEvents());
	}

	/**
	 * set stepsize
	 */
	private void doStepSize(SimVector sv)
	{
		if (sv.parameters.length < 1)
		{
			System.out.println("stepsize = " + deltaToNS(stepSize));
			return;
		}

		double timeNS = Electric.atof(sv.parameters[0]);
		long newSize = nsToDelta(timeNS);
		if (newSize <= 0)
		{
			System.out.println("Bad step size: " + Electric.formatDouble(timeNS*1000) + "psec (must be 10 psec or larger)");
			return;
		}
		stepSize = newSize;
	}

	/**
	 * mark nodes and vectors for stopping
	 */
	private void doStop(SimVector sv)
	{
		if (sv.sigs != null)
		{
			for(Signal<DigitalSample> sig : sv.sigs)
			{
				SimAPI.Node n = nodeMap.get(sig);
				n = unAlias(n);
				if (n.getFlags(SimAPI.MERGED) != 0) continue;

				if (true)
					n.clearFlags(SimAPI.STOPONCHANGE);
				else
					n.setFlags(SimAPI.STOPONCHANGE);
			}
		}
		setVecNodes(SimAPI.WATCHVECTOR);
		setVecNodes(SimAPI.STOPVECCHANGE);
	}

	/**
	 * mark nodes and vectors for tracing
	 */
	private void doTrace(SimVector sv)
	{
		if (sv.sigs != null)
		{
			for(Signal<DigitalSample> sig : sv.sigs)
			{
				SimAPI.Node n = nodeMap.get(sig);
				n = unAlias(n);

				if (n.getFlags(SimAPI.MERGED) != 0)
				{
					System.out.println("can't trace " + n.getName());
					continue;
				}

				n.setFlags(SimAPI.WATCHED);
			}
		}
		if (sv.sigsNegated != null)
		{
			for(Signal<DigitalSample> sig : sv.sigsNegated)
			{
				SimAPI.Node n = nodeMap.get(sig);
				n = unAlias(n);

				if (n.getFlags(SimAPI.MERGED) != 0)
				{
					System.out.println("can't trace " + n.getName());
					continue;
				}

				if (n.getFlags(SimAPI.WATCHED) != 0)
				{
					System.out.println(n.getName() + " was watched; not any more");
					n.clearFlags(SimAPI.WATCHED);
				}

			}
		}
		setVecNodes(SimAPI.WATCHVECTOR);
	}

	/**
	 * Print list of transistors with src/drn shorted (or between power supplies).
	 */
	private void doTCap()
	{
        Collection<SimAPI.Trans> shortedTransistors = theSim.getShortedTransistors();
		if (shortedTransistors.isEmpty())
			System.out.println("there are no shorted transistors");
		else
			System.out.println("shorted transistors:");
		for(SimAPI.Trans t: shortedTransistors)
		{
			System.out.println(" " + t.describeBaseType() + " g=" + t.getGate().getName() + " s=" +
				t.getSource().getName() + " d=" + t.getDrain().getName() + " (" +
				(t.getLength() / theSim.getLambdaCM()) + "x" + (t.getWidth() / theSim.getLambdaCM()) + ")");
		}
	}

	/**
	 * set unitdelay parameter
	 */
	private void doUnitDelay(SimVector sv)
	{
		if (sv.parameters.length == 0)
		{
			if (theSim.getUnitDelay() == 0)
				System.out.println("unitdelay = OFF");
			else
				System.out.println("unitdelay = " + deltaToNS(theSim.getUnitDelay()));
			return;
		}

		theSim.setUnitDelay((int)nsToDelta(Electric.atof(sv.parameters[0])));
		if (theSim.getUnitDelay() < 0) theSim.setUnitDelay(0);
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
			cCount = Electric.atoi(sv.parameters[3]);
		} else
		{
			mask = null;
			value = new StringBuffer(sv.parameters[1]);
			cCount = Electric.atoi(sv.parameters[2]);
		}

		Signal<DigitalSample> sig = findName(sv.parameters[0]);
		if (sig == null)
		{
			System.out.println("UNTIL statement cannot find signal: " + sv.parameters[0]);
			return;
		}

		String name = null;
		int comp = 0;
		int nBits = 1;
		SimAPI.Node [] nodes = null;
		Signal<?>[] sigsOnBus = sig.getBusMembers();
		if (sigsOnBus == null)
		{
			SimAPI.Node n = nodeMap.get(sig);
			name = sig.getFullName();
			n = unAlias(n);
			SimAPI.Node [] nodeList = new SimAPI.Node[1];
			nodeList[0] = n;
			int cnt = 0;
			while ((cnt <= cCount) && (comp = compareVector(nodeList, name, 1, mask, value.toString())) != 0)
			{
				cnt++;
				clockIt(1);
			}
			nodes = new SimAPI.Node[1];
			nodes[0] = n;
		} else
		{
			SimAPI.Node [] nodeList = new SimAPI.Node[sigsOnBus.length];
			for(int i=0; i<sigsOnBus.length; i++)
				nodeList[i] = nodeMap.get(sigsOnBus[i]);

			int cnt = 0;
			while ((cnt <= cCount) && (comp = compareVector(nodeList, sig.getFullName(), sigsOnBus.length, mask, value.toString())) != 0)
			{
				cnt++;
				clockIt(1);
			}
			name = sig.getFullName();
			nBits = sigsOnBus.length;
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
					infstr += nodes[i].getPotChar();
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

		Signal<DigitalSample> sig = findName(sv.parameters[0]);
		if (sig == null)
		{
			System.out.println("ASSERT statement cannot find signal: " + sv.parameters[0]);
			return;
		}

		int comp = 0;
		String name = null;
		SimAPI.Node [] nodes = null;
		Signal<?>[] sigsOnBus = sig.getBusMembers();
		if (sigsOnBus == null)
		{
			SimAPI.Node n = nodeMap.get(sig);
			name = n.getName();
			n = unAlias(n);
			SimAPI.Node [] nodeList = new SimAPI.Node[1];
			nodeList[0] = n;
			comp = compareVector(nodeList, name, 1, mask, value.toString());
			nodes = nodeList;
		} else
		{
			SimAPI.Node [] nodeList = new SimAPI.Node[sigsOnBus.length];
			for(int i=0; i<sigsOnBus.length; i++)
				nodeList[i] = nodeMap.get(sigsOnBus[i]);
			comp = compareVector(nodeList, sig.getSignalName(), sigsOnBus.length, mask, value.toString());
			name = sig.getSignalName();
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
					infstr += nodes[i].getPotChar();
			}
			System.out.println("Assertion failed on '" + name + "': want (" + value + ") but got (" + infstr + ")");
		}
	}

	/** keeps current AssertWhen trigger */				private SimAPI.Node awTrig;
	/** track pointer on the current AssertWhen list */	private AssertWhen awP;

	private void doAssertWhen(SimVector sv)
	{
		Signal<DigitalSample> sig = findName(sv.parameters[0]);
		if (sig == null)
		{
			System.out.println("ASSERTWHEN statement cannot find signal: " + sv.parameters[0]);
			return;
		}

		if (sig.getBusMembers() == null)
		{
			SimAPI.Node n = nodeMap.get(sig);
			n = unAlias(n);
			awTrig = n;
			awTrig.setAssertWhenPot((short)chToPot(sv.parameters[1].charAt(0)));

			Signal<DigitalSample> oSig = findName(sv.parameters[2]);
			if (oSig == null)
			{
				System.out.println("ASSERTWHEN statement cannot find other signal: " + sv.parameters[2]);
				return;
			}

			SimAPI.Node wN = nodeMap.get(oSig);
			setupAssertWhen(wN, commandName(sv.command));
		} else
		{
			System.out.println("trigger to assertWhen " + sv.parameters[0] + " can't be a vector");
		}
	}

	private void setupAssertWhen(SimAPI.Node n, String val)
	{
		AssertWhen p = new AssertWhen();
		p.node = n;
		p.val  = val.charAt(0);
		p.nxt = null;

		if (awTrig.getAssertWhen() == null)
		{
			// first time
			awTrig.setAssertWhen(p);
			awP = p;
		} else
		{
			// more than 1 matching nodes
			awP.nxt = p;
			awP = p;
		}
	}

	/************************** IRSIM INTERFACE **************************/

	public void evalAssertWhen(SimAPI.Node n)
	{
		for (AssertWhen p = (AssertWhen)n.getAssertWhen(); p != null; )
		{
			String name = p.node.getName();
			StringBuffer sb = new StringBuffer();
			sb.append(p.val);
			SimAPI.Node [] nodes = new SimAPI.Node[1];
			nodes[0] = p.node;
			int comp = compareVector(nodes, name, 1, null, sb.toString());
			if (comp != 0)
				System.out.println("Assertion failed on '" + name + "'");
			p = p.nxt;
		}
		n.setAssertWhen(null);
	}

	/**
	 * Update the trace window so that endT is shown.  If the update fits in the
	 * window, simply draw the missing parts.  Otherwise scroll the traces,
	 * centered around endT.
	 */
	public void updateWindowIfAnalyzerOn(long endT) {
        if (analyzerON)
            updateWindow(endT);
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
		which &= (SimAPI.WATCHVECTOR | SimAPI.STOPVECCHANGE);
		String temp = " @ " + deltaToNS(theSim.getCurDelta()) + "ns ";
		System.out.println(temp);
		column = temp.length();
		Collection<Signal<?>> sigs = gui.getSignals();
		for(Signal<?> sig : sigs)
		{
			Signal<?>[] sigsOnBus = sig.getBusMembers();
			if (sigsOnBus == null) continue;
			SimAPI.Node b = nodeMap.get(sig);
			if (b.getFlags(which) == 0) continue;
			boolean found = false;
			for(Signal<?> bSig : sigsOnBus)
			{
				SimAPI.Node bN = nodeMap.get(bSig);
				if (bN.getTime() == theSim.getCurDelta())
					{ found = true;   break; }
			}
			if (found)
				dVec(sig);
		}
	}

	/************************** SUPPORT **************************/

	private void initRSim()
	{
		xClock = new ArrayList<Sequence>();
		maxClock = 0;
		column = 0;
		analyzerON = false;
		firstVector = null;
		for(int i = 0; i < 5; i++) listTbl[i] = null;
		listTbl[inputNumber(SimAPI.H_INPUT)] = hInputs;
		listTbl[inputNumber(SimAPI.L_INPUT)] = lIinputs;
		listTbl[inputNumber(SimAPI.U_INPUT)] = uInputs;
		listTbl[inputNumber(SimAPI.X_INPUT)] = xInputs;
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
                double maxTime = gui.getMaxPanelTime();
				long endtime = nsToDelta(maxTime * 1e9);
				if (endtime > endTime) endTime = endtime;
				double max = getEndTime();
				long endT = nsToDelta(max * 1e9);
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
     * Get the resolution scale.  As this number increases the min resolution
     * decreases (becomes more accurate).  A value of 1 corresponds to 1ns resolution.
     * A value of 1000 corresponds to 1ps resolution.
     * @return resolution scale factor
     */
    protected static long getResolutionScale()
    {
        return SimAPI.resolutionScale;
    }

	/**
	 * Update the cache (beginning of window and cursor) for traces that just
	 * became visible (or were just added).
	 */
	private void updateTraceCache()
	{
		long startT = startTime;
		long cursT = firstTime;
		for(SimAPI.Node nd : theSim.getNodes())
		{
			SimAPI.HistEnt p = nd.getWind();
			SimAPI.HistEnt h = nd.getCursor();
			SimAPI.HistEnt nextH = h.getNextHist();
			if (h.getTime() > cursT || nextH.getTime() <= cursT)
			{
				if (p.getTime() <= cursT)
					nd.setCursor(p);
				else
					nd.setCursor(nd.getHead());
			}

			if (startT <= p.getTime())
				p = nd.getHead();

			h = p.getNextHist();
			while (h.getTime() < startT)
			{
				p = h;
				h = h.getNextHist();
			}
			nd.setWind(p);

			p = nd.getCursor();
			h = p.getNextHist();
			while (h.getTime() <= cursT)
			{
				p = h;
				h = h.getNextHist();
			}
			nd.setCursor(p);
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
			for(SimAPI.Node nd : theSim.getNodes())
			{
				SimAPI.HistEnt p = begin ? nd.getHead() : nd.getWind();
				SimAPI.HistEnt h = p.getNextHist();
				while (h.getTime() < startT)
				{
					p = h;
					h = h.getNextHist();
				}
				nd.setWind(p);
			}
			lastStart = startTime;
		}

		for(Map.Entry<SimAPI.Node,MutableSignal<DigitalSample>> e : signalMap.entrySet())
		{
            SimAPI.Node nd = e.getKey();
            MutableSignal<DigitalSample> sig = e.getValue();
			if (sig == null) continue;
//		for(SimConstants.Node nd : theSim.getNodeList())
//		{
//			if (nd.sig == null) continue;

			if (t1 >= lastTime) continue;
			SimAPI.HistEnt h = nd.getWind();
			if (h == null) continue;
			int count = 0;
			long curT = 0;
			long endT = t2;
			while (curT < endT)
			{
				int val = h.getVal();
				while (h.getTime() < endT && h.getVal() == val)
					h = h.getNextHist();

				// advance time
				long nextT;
				if (h.getTime() > endT)
				{
					nextT = endT;
				} else
				{
					nextT = h.getTime();
				}

				// make sure there is room in the array
				if (count >= traceTotal)
				{
					int newTotal = traceTotal * 2;
					if (newTotal <= count) newTotal = count + 50;
					double [] newTime = new double[newTotal];
					DigitalSample [] newState = new DigitalSample[newTotal];
					for(int i=0; i<count; i++)
					{
						newTime[i] = traceTime[i];
						newState[i] = traceState[i];
					}
					traceTime = newTime;
					traceState = newState;
					traceTotal = newTotal;
				}

				traceTime[count] = deltaToNS(curT) / 1000000000;
				switch (val)
				{
					case SimAPI.LOW:
						traceState[count] = DigitalSample.getSample(Value.LOW, Strength.LARGE_CAPACITANCE);
						break;
					case SimAPI.HIGH:
						traceState[count] = DigitalSample.getSample(Value.HIGH, Strength.LARGE_CAPACITANCE);
						break;
					default:
						traceState[count] = DigitalSample.getSample(Value.X, Strength.LARGE_CAPACITANCE);
						break;
				}
				curT = nextT;
				count++;
			}
			for(int i=0; i<count; i++)
                if (sig.getSample(traceTime[i])==null)
                    sig.addSample(traceTime[i], traceState[i]);
//                if (((MutableSignal<DigitalSample>)nd.sig).getSample(traceTime[i])==null)
//                    ((MutableSignal<DigitalSample>)nd.sig).addSample(traceTime[i], traceState[i]);
		}
		gui.repaint();
	}

	/**
	 * set/clear input status of node and add/remove it to/from corresponding list.
	 */
	private void setIn(SimAPI.Node n, char wChar)
	{
		while(n.getFlags(SimAPI.ALIAS) != 0)
			n = n.getLink();

		if (n.getFlags(SimAPI.POWER_RAIL | SimAPI.MERGED) != 0)	// Gnd, Vdd, or merged node
		{
			String pots = "lxuh";
			if (n.getFlags(SimAPI.MERGED) != 0 || pots.charAt(n.getPot()) != wChar)
				System.out.println("Can't drive `" + n.getName() + "' to `" + wChar + "'");
		} else
		{
			List<SimAPI.Node> list = listTbl[inputNumber((int)n.getFlags())];
			switch (wChar)
			{
				case 'h':   case '1':
					if (list != null && list != hInputs)
					{
						n.clearFlags(SimAPI.INPUT_MASK);
						list.remove(n);
					}
					if (! (list == hInputs || wasInP(n, SimAPI.HIGH)))
					{
                        n.clearFlags(SimAPI.INPUT_MASK);
						n.setFlags(SimAPI.H_INPUT);
						hInputs.add(n);
					}
					break;

				case 'l':   case '0':
					if (list != null && list != lIinputs)
					{
						n.clearFlags(SimAPI.INPUT_MASK);
						list.remove(n);
					}
					if (! (list == lIinputs || wasInP(n, SimAPI.LOW)))
					{
                        n.clearFlags(SimAPI.INPUT_MASK);
						n.setFlags(SimAPI.L_INPUT);
						lIinputs.add(n);
					}
					break;

				case 'u':
					if (list != null && list != uInputs)
					{
						n.clearFlags(SimAPI.INPUT_MASK);
						list.remove(n);
					}
					if (! (list == uInputs || wasInP(n, SimAPI.X)))
					{
                        n.clearFlags(SimAPI.INPUT_MASK);
						n.setFlags(SimAPI.U_INPUT);
						uInputs.add(n);
					}
					break;

				case 'x':
					if (list == xInputs)
						break;
					if (list != null)
					{
						n.clearFlags(SimAPI.INPUT_MASK);
						list.remove(n);
					}
					if (n.getFlags(SimAPI.INPUT) != 0)
					{
                        n.clearFlags(SimAPI.INPUT_MASK);
						n.setFlags(SimAPI.X_INPUT);
						xInputs.add(n);
					}
					break;

				default:
					return;
			}
		}
	}

	private boolean wasInP(SimAPI.Node n, int p)
	{
		return n.getFlags(SimAPI.INPUT) != 0 && n.getPot() == p;
	}

	private String pValue(String node_name, SimAPI.Node node)
	{
		char pot = 0;
		switch (node.getPot())
		{
			case 0: pot = '0';   break;
			case 1: pot = 'X';   break;
			case 2: pot = 'X';   break;
			case 3: pot = '1';   break;
		}
		return node_name + "=" + pot + " ";
	}

	private String pGValue(SimAPI.Trans t)
	{
		String infstr = "";
		if (irDebug != 0)
			infstr += "[" + t.describeState() + "] ";
        Collection<SimAPI.Trans> gateList = t.getGateList();
		if (gateList != null)
		{
			infstr += "(";
			for(SimAPI.Trans tg: gateList)
			{
				SimAPI.Node n = tg.getGate();
				infstr += pValue(n.getName(), n);
			}

			infstr += ") ";
		} else
		{
			SimAPI.Node n = t.getGate();
			infstr += pValue(n.getName(), n);
		}
		return infstr;
	}

	private String prOneRes(double r)
	{
		String ret = Electric.formatDouble(r);
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

	private String prTRes(SimAPI.Trans t)
	{
        double[] resists = t.getResists();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < resists.length; i++) {
            b.append(i == 0 ? "[" : ",").append(prOneRes(resists[i]));
        }
        return b.append("]").toString();
	}

//	private String prTRes(Sim.Resists r)
//	{
//		String v1 = prOneRes(r.rStatic);
//		String v2 = prOneRes(r.dynRes[Sim.R_HIGH]);
//		String v3 = prOneRes(r.dynRes[Sim.R_LOW]);
//		return "[" + v1 + ", " + v2 + ", " + v3 + "]";
//	}

	private String pTrans(SimAPI.Trans t)
	{
		String infstr = t.describeBaseType() + " ";
		if (t.getBaseType() != SimAPI.RESIST)
			infstr += pGValue(t);

		infstr += pValue(t.getSource().getName(), t.getSource());
		infstr += pValue(t.getDrain().getName(), t.getDrain());
		infstr += prTRes(t);
		if (t.getLink() != t && (theSim.getReport() & SimAPI.REPORT_TCOORD) != 0)
			infstr += " <" + t.getX() + "," + t.getY() + ">";
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
		return Electric.canonicChar(s.charAt(index));
	}

	/**
	 * map a character into one of the potentials that a node can be set/compared
	 */
	private int chToPot(char ch)
	{
		String s = "0ux1lUXhLUXH";
		for(int i = 0; i < s.length(); i++)
			if (s.charAt(i) == ch)
				return i & (SimAPI.N_POTS - 1);

		System.out.println(ch + ": unknown node value");
		return SimAPI.N_POTS;
	}

	private SimAPI.Node unAlias(SimAPI.Node n)
	{
		while (n.getFlags(SimAPI.ALIAS) != 0) n = n.getLink();
		return n;
	}

	/**
	 * display node/vector values in display list
	 */
	private void pnWatchList()
	{
		theSim.printPendingEvents();
	}

	/**
	 * just in case node appears in more than one bit vector, run through all
	 * the vectors being traced and make sure the flag is set for each node.
	 */
	private void setVecNodes(int flag)
	{
		Collection<Signal<?>> sigs = gui.getSignals();
		for(Signal<?> sig : sigs)
		{
			Signal<?>[] sigsOnBus = sig.getBusMembers();
			if (sigsOnBus == null) continue;
			SimAPI.Node b = nodeMap.get(sig);
			if (b.getFlags(flag) != 0)
			{
				for(Signal<?> bSig : sigsOnBus)
				{
					SimAPI.Node bN = nodeMap.get(bSig);
					bN.setFlags(flag);
				}
			}
		}
	}

	private int compareVector(SimAPI.Node [] np, String name, int nBits, String mask, String value)
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
			SimAPI.Node n = np[i];
			int val = chToPot(value.charAt(i));
			if (val >= SimAPI.N_POTS)
				return 0;
			if (val == SimAPI.X_X) val = SimAPI.X;
				if (n.getPot() != val)
					return 1;
		}
		return 0;
	}

	private Signal<DigitalSample> findName(String name)
	{
		for(Signal<?> sig : gui.getSignals())
		{
			if (sig.getFullName().equals(name)) return (Signal<DigitalSample>)sig;
		}
		return null;
	}

	/**
	 * display bit vector.
	 */
	private void dVec(Signal<?> sig)
	{
		Signal<?>[] sigsOnBus = sig.getBusMembers();
		int i = sig.getSignalName().length() + 2 + sigsOnBus.length;
		if (column + i >= MAXCOL)
		{
			column = 0;
		}
		column += i;
		String bits = "";
		for(Signal<?> bSig : sigsOnBus)
		{
			SimAPI.Node n = nodeMap.get(bSig);
			bits += n.getPotChar();
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
			boolean repeat = theSim.step(stopTime, xInputs, hInputs, lIinputs, uInputs);
			if (!repeat) break;
		}

		return theSim.getCurDelta() - stopTime;
	}

	/**
	 * set each node/vector in a clock sequence to its next value
	 */
	private void vecValue(int index)
	{
		for(Sequence cs : xClock)
		{
			String v = cs.values[index % cs.values.length];
			Signal<?>[] sigsOnBus = cs.sig.getBusMembers();
			if (sigsOnBus == null)
			{
				SimAPI.Node n = nodeMap.get(cs.sig);
				setIn(n, v.charAt(0));
			} else
			{
				for(int i=0; i<sigsOnBus.length; i++)
				{
					SimAPI.Node n = nodeMap.get(sigsOnBus[i]);
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
	private void defSequence(String [] args, List<Sequence> list)
	{
		// if no arguments, get rid of all the sequences we have defined
		if (args.length == 0)
		{
			list.clear();
			return;
		}

		Signal<DigitalSample> sig = findName(args[0]);
		if (sig == null)
		{
			System.out.println(args[0] + ": No such node or vector");
			return;
		}
		int len = 1;
		Signal<?>[] sigsOnBus = sig.getBusMembers();
		if (sigsOnBus != null)
			len = sigsOnBus.length;
		SimAPI.Node n = nodeMap.get(sig);
		if (sigsOnBus == null)
		{
			n = unAlias(n);
			if (n.getFlags(SimAPI.MERGED) != 0)
			{
				System.out.println(n.getName() + " can't be part of a sequence");
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
		nodeMap.put(sig, n);

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
		list.add(s);
//		xClock.add(s);
	}

	private int whichPhase = 0;

	/**
	 * Step each clock node through one simulation step
	 */
	private boolean stepPhase()
	{
		vecValue(whichPhase++);
		if (relax(theSim.getCurDelta() + stepSize) != 0) return true;
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
	private void cPath(SimAPI.Node n, int level)
	{
		// no last transition!
		if (n.getFlags(SimAPI.MERGED) != 0 || n.getCause() == null)
		{
			System.out.println("  there is no previous transition!");
		}

		// here if we come across a node which has changed more recently than
		// the time reached during the backtrace.  We can't continue the
		// backtrace in any reasonable fashion, so we stop here.
		else if (level != 0 && n.getTime() > pTime)
		{
			System.out.println("  transition of " + n.getName() + ", which has since changed again");
		}

		// here if there seems to be a cause for this node's transition.
		// If the node appears to have 'caused' its own transition (n.t.cause
		// == n), that means it was input.  Otherwise continue backtrace...
		else if (n.getCause() == n)
		{
			System.out.println("  " + n.getName() + " . " + n.getPotChar() +
				" @ " + deltaToNS(n.getTime()) + "ns , node was an input");
		}
		else if (n.getCause().getFlags(SimAPI.VISITED) != 0)
		{
			System.out.println("  ... loop in traceback");
		} else
		{
			long deltaT = n.getTime() - n.getCause().getTime();

			n.setFlags(SimAPI.VISITED);
			pTime = n.getTime();
			cPath(n.getCause(), level + 1);
			n.clearFlags(SimAPI.VISITED);
			if (deltaT < 0)
				System.out.println("  " + n.getName() + " . " + n.getPotChar() +
					" @ " + deltaToNS(n.getTime()) + "ns   (??)");
			else
				System.out.println("  " + n.getName() + " . " + n.getPotChar() +
					" @ " + deltaToNS(n.getTime()) + "ns   (" + deltaToNS(deltaT) + "ns)");
		}
	}

	private void clearInputs()
	{
		for(int i = 0; i < 5; i++)
		{
			if (listTbl[i] == null) continue;
			for(SimAPI.Node n : listTbl[i])
			{
				if (n.getFlags(SimAPI.POWER_RAIL) == 0)
					n.clearFlags(SimAPI.INPUT_MASK | SimAPI.INPUT);
			}
			listTbl[i].clear();
		}
		for(SimAPI.Node n : theSim.getNodes() )
		{
			if (n.getFlags(SimAPI.POWER_RAIL) == 0)
				n.clearFlags(SimAPI.INPUT);
		}
	}
    
	/**
	 * Convert deltas to ns.
	 */
	private static double deltaToNS(long d) { return (double)d / (double)SimAPI.resolutionScale; }
    
	/**
	 * Convert ns to deltas.
	 */
	private static long nsToDelta(double d) { return (long)(d * SimAPI.resolutionScale); }
    
    
	private static int inputNumber(int flg) { return ((flg & SimAPI.INPUT_MASK) >> 12); }
}
