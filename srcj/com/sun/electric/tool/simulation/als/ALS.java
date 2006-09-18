/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ALS.java
 * Asynchronous Logic Simulator main module
 * Original C Code written by Brent Serbin and Peter J. Gallant
 * Translated to Java by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.simulation.als;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.simulation.Analysis;
import com.sun.electric.tool.simulation.DigitalSignal;
import com.sun.electric.tool.simulation.Engine;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.waveform.Panel;
import com.sun.electric.tool.user.waveform.WaveSignal;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Class to control the ALS Simulator.
 */
public class ALS extends Engine
{
	/** initial size of simulation window */		private static final double DEFTIMERANGE = 0.0000002;

	/** the simulation engine */						Sim               theSim;
	/** the circuit flattener */						Flat              theFlat;
	/** the waveform window showing this simulator */	WaveformWindow    ww;
	/** the stimuli set currently being displayed */	Analysis          an;
	/** current time in the simulator */				double            timeAbs;
	/** saved list of stimuli when refreshing */		List<String>      stimuliList;

	List<Model>       modelList;
	List<Model>       primList = new ArrayList<Model>();
	IO                ioPtr2;
	Connect           cellRoot = null;
	ALSExport         exPtr2;
	List<Node>        nodeList = new ArrayList<Node>();
	Node              driveNode;
	Link              linkFront = null;
	Link              linkBack = null;
	Link              setRoot = null;
	List              ioPtr1;
	char           [] instBuf = null;
	int            [] instPtr = null;

	private int       iPtr;
	private String    delay;
	private Model     modPtr2;
	private String [] netlistStrings;
	private int       netlistStringPoint;
	private int       iBufSize;
	private int       iPtrSize;
	private double    deltaDef;
	private double    linearDef;
	private double    expDef;
	private double    randomDef;
	private double    absDef;

	/**
	 * DelayTypes is a typesafe enum class that describes types of delay.
	 */
	private static class DelayTypes
	{
		private DelayTypes() {}

		/**
		 * Returns a printable version of this DelayTypes.
		 * @return a printable version of this DelayTypes.
		 */
		public String toString() { return ""; }

		/** Describes a minimum delay. */		public static final DelayTypes DELAY_MIN = new DelayTypes();
		/** Describes a typical delay. */		public static final DelayTypes DELAY_TYP = new DelayTypes();
		/** Describes a maximum delay. */		public static final DelayTypes DELAY_MAX = new DelayTypes();
	}

	static class Model
	{
		String          name;
		char            type;
		Object          ptr;	/* may be Connect, Row, or Func */
		List<ALSExport> exList;
		List<IO>        setList;
		List<Load>      loadList;
		char            fanOut;
		int             priority;
		String          level;  /* hierarchical level */

		Model(String name, char type)
		{
			this.name = name;
			this.type = type;
			ptr = null;
			exList = new ArrayList<ALSExport>();
			setList = new ArrayList<IO>();
			loadList = new ArrayList<Load>();
			fanOut = 0;
			priority = 1;
		}
	};

	static class Row
	{
		List<Object> inList;
		List<Object> outList;
		double       delta;
		double       linear;
		double       exp;
		double       random;
		double       abs;    /* BA delay - SDF absolute port delay */
		Row          next;
		String       delay;  /* delay transition name (01, 10, etc) */
	};

	static class IO
	{
		Object  nodePtr;
		char    operatr;
		Object  operand;
		int     strength;
	};

	static class Connect
	{
		String          instName;
		String          modelName;
		List<ALSExport> exList;
		Connect         parent;
		Connect         child;
		Connect         next;
	};

	static class ALSExport
	{
		Object    nodeName;
		Node      nodePtr;
	};

	static class Load
	{
		Object ptr;
		double load;
	};

	static class Node
	{
		Connect       cellPtr;
		DigitalSignal sig;
		private int   num;
		int           sumState;
		int           sumStrength;
		Object        newState;
		int           newStrength;
		boolean       traceNode;
		List<Stat>    statList;
		List<Load>    pinList;
		double        load;
		int           visit;
		int           arrive;
		int           depart;
		double        tLast;

		private static int nSeq = 1;

		Node()
		{
			num = nSeq++;
		}

		int getIndex() { return num; }
	};

	static class Stat
	{
		Model   primPtr;
		Node    nodePtr;
		int     newState;
		int     newStrength;
		char    schedOp;
		Object  schedState;
		int     schedStrength;
	};

	static class Link
	{
		Link    left;
		Link    right;
		Link    up;
		Link    down;
		Object  ptr;
		char    type;
		char    operatr;
		Object  state;
		int     strength;
		int     priority;
		double  time;
		Model   primHead;
	};

	static class Trak
	{
		int      state;
		double   time;
	};

	static class Func
	{
		UserProc        procPtr;
		List<ALSExport> inList;
		double          delta;
		double          linear;
		double          exp;
		double          abs;    /* absolute delay for back annotation */
		double          random;
		Object          userPtr;
	};

	static class UserProc
	{
		protected ALS    als;
		private   String name;

		private static HashMap<String,UserProc> funcMap = new HashMap<String,UserProc>();

		void nameMe(ALS als, String name)
		{
			this.als = als;
			this.name = name;
			funcMap.put(name.toUpperCase(), this);
		}

		void simulate(Model primHead) {}

		/**
		 * Method to return the address of the function specified by
		 * the calling argument character string.  Each time a routine is added to the
		 * user library of event driven "C" functions an entry must be made into this
		 * procedure to include it in the known list of user routines.  It is highly
		 * recommended that all user defined procedures be named with capital letters.
		 * This is done for two reasons: 1) the text in the netlist is converted to
		 * upper case by the parser and this means caps must be used in the string
		 * compare statements below, and 2) the user defined function routines will
		 * be easily identifiable in source code listings.  Returns zero on error.
		 *
		 * Calling Arguments:
		 *	s1 = pointer to character string containing the name of the user
		 *	    defined function
		 */
		static UserProc getFunctionAddress(String s1)
		{
			UserProc ret = funcMap.get(s1);
			if (ret == null)
				System.out.println("ERROR: Unable to find user function " + s1 + " in library");
			return ret;
		}

		/**
		 * Method to insert a node updating task into the
		 * time scheduling link list.  The user must specify the node address, signal
		 * state, signal strength, and update time in the calling arguments.  If the
		 * user updates the value of a node manually without calling this procedure,
		 * the event driving algorithm will not be able to detect this change.  This means
		 * that the effects of the node update will not be propagated throughout the
		 * simulation circuit automatically.  By scheduling the event through the master
		 * link list, this problem can be avoided.  Returns true on error.
		 *
		 * Calling Arguments:
		 *	nodeHead = pointer to the node data structure to be updated
		 *	operatr  = char indicating operation to be performed on node
		 *	state    = integer value representing the state of the node
		 *	strength = integer value representing the logic stregth of the signal
		 *	time     = double value representing the time the change is to take place
		 */
		protected void scheduleNodeUpdate(Model primHead, ALSExport exHead, int operatr,
			Object state, int strength, double time)
		{
			Stat statHead = (Stat)exHead.nodeName;
			if (statHead.schedOp == operatr && statHead.schedState == state &&
				statHead.schedStrength == strength)
			{
				return;
			}
			if (als.getSim().tracing)
			{
				String s2 = als.computeNodeName(statHead.nodePtr);
				System.out.println("      Schedule(F) gate " + statHead.primPtr.name + statHead.primPtr.level +
					", net " + s2 + "  at " + TextUtils.convertToEngineeringNotation(time));
			}
			Link linkPtr2 = new Link();
			linkPtr2.type = 'G';
			linkPtr2.ptr = statHead;
			linkPtr2.operatr = statHead.schedOp = (char)operatr;
			linkPtr2.state = statHead.schedState = state;
			linkPtr2.strength = statHead.schedStrength = strength;
			linkPtr2.priority = 1;
			linkPtr2.time = time;
			linkPtr2.primHead = primHead;
			als.getSim().insertLinkList(linkPtr2);
		}

		/**
		 * Method to examine all the elements feeding into a node that is
		 * connected to the current bidirectional element and insures that there are no
		 * bidirectional "loops" found in the network paths.  If a loop is found the
		 * effects of the element are ignored from the node summing calculation.
		 *
		 * Calling Arguments:
		 *	primHead    = pointer to current bidirectional element
		 *	side[]      = pointers to nodes on each side of the bidir element
		 *  outStrength = output strength
		 */
		private Node  targetNode;
		private int   biDirClock = 0;

		protected void calculateBidirOutputs(Model primHead, ALSExport [] side, int outStrength)
		{
			for(int i=0; i<2; i++)
			{
				ALSExport thisSide = side[i];
				ALSExport otherSide = side[(i+1)%2];
				Node sumNode = thisSide.nodePtr;
				targetNode = otherSide.nodePtr;
				if (targetNode == als.driveNode) continue;
				int state = ((Integer)sumNode.newState).intValue();
				int strength = sumNode.newStrength;

				biDirClock++;
				for(Stat statHead : sumNode.statList)
				{
					if (statHead.primPtr == primHead) continue;

					sumNode.visit = biDirClock;

					int thisStrength = statHead.newStrength;
					int thisState = statHead.newState;

					if (thisStrength > strength)
					{
						state = thisState;
						strength = thisStrength;
						continue;
					}

					if (thisStrength == strength)
					{
						if (thisState != state) state = Stimuli.LOGIC_X;
					}
				}

				// make strength no more than maximum output strength
				if (strength > outStrength) strength = outStrength;

				Func funcHead = (Func)primHead.ptr;
				double time = als.timeAbs + (funcHead.delta * targetNode.load);
				scheduleNodeUpdate(primHead, otherSide, '=', new Integer(state), strength, time);
			}
		}
	}

	ALS()
	{
		theSim = new Sim(this);
		theFlat = new Flat(this);
	}

	Sim getSim() { return theSim; }

	/**
	 * Method to simulate the a Cell, given its context and the Cell with the real netlist.
	 * @param netlistCell the Cell with the real ALS netlist.
	 * @param cell the original Cell being simulated.
	 */
	public static void simulateNetlist(Cell netlistCell, Cell cell)
	{
		ALS theALS = new ALS();
		theALS.doSimulation(netlistCell, cell, null, null);
	}

	/**
	 * Method to restart a simulation and reload the circuit.
	 * @param netlistCell the cell with the netlist.
	 * @param cell the cell being simulated.
	 * @param prevALS the simulation that is being reloaded.
	 */
	public static void restartSimulation(Cell netlistCell, Cell cell, ALS prevALS)
	{
		WaveformWindow ww = prevALS.ww;
		List<String> stimuliList = prevALS.stimuliList;
		ALS theALS = new ALS();
		theALS.doSimulation(netlistCell, cell, ww, stimuliList);
	}

	/**
	 * Method to reload the circuit data.
	 */
	public void refresh()
	{
		// save existing stimuli in this simulator object
		stimuliList = getStimuliToSave();

		// restart everything
		Simulation.startSimulation(Simulation.ALS_ENGINE, false, an.getStimuli().getCell(), this);
	}

	/**
	 * Method to update the simulation (because some stimuli have changed).
	 */
	public void update()
	{
		theSim.initializeSimulator(true);
	}

	/**
	 * Method to set the currently-selected signal high at the current time.
	 */
	public void setSignalHigh()
	{
		makeThemThus(Stimuli.LOGIC_HIGH);
	}

	/**
	 * Method to set the currently-selected signal low at the current time.
	 */
	public void setSignalLow()
	{
		makeThemThus(Stimuli.LOGIC_LOW);
	}

	/**
	 * Method to set the currently-selected signal to have a clock with a given period.
	 */
	public void setClock(double period)
	{
		List<Signal> signals = ww.getHighlightedNetworkNames();
		if (signals.size() == 0)
		{
			Job.getUserInterface().showErrorMessage("Must select a signal before setting a Clock on it",
				"No Signals Selected");
			return;
		}
		String [] parameters = new String[1];
		for(Signal sig : signals)
		{
			String sigName = sig.getFullName();
			Node nodeHead = findNode(sigName);
			if (nodeHead == null)
			{
				System.out.println("ERROR: Unable to find node " + sigName);
				continue;
			}

			double time = ww.getMainXPositionCursor();
			Link vectPtr2 = new Link();
			vectPtr2.type = 'N';
			vectPtr2.ptr = nodeHead;
			vectPtr2.state = new Integer(Stimuli.LOGIC_HIGH);
			vectPtr2.strength = Stimuli.VDD_STRENGTH;
			vectPtr2.priority = 1;
			vectPtr2.time = time;
			vectPtr2.right = null;

			Link vectPtr1 = new Link();
			vectPtr1.type = 'N';
			vectPtr1.ptr = nodeHead;
			vectPtr1.state = new Integer(Stimuli.LOGIC_LOW);
			vectPtr1.strength = Stimuli.VDD_STRENGTH;
			vectPtr1.priority = 1;
			vectPtr1.time = period / 2.0;
			vectPtr1.right = vectPtr2;

			Row clokHead = new Row();
			clokHead.inList = new ArrayList<Object>();
			clokHead.inList.add(vectPtr1);
			clokHead.inList.add(vectPtr2);
			clokHead.outList = new ArrayList<Object>();
			clokHead.delta = period;
			clokHead.linear = 0;
			clokHead.exp = 0;
			clokHead.abs = 0;
			clokHead.random = 0;
			clokHead.next = null;
			clokHead.delay = null;

			Link setHead = new Link();
			setHead.type = 'C';
			setHead.ptr = clokHead;
			setHead.state = new Integer(0);
			setHead.priority = 1;
			setHead.time = time;
			setHead.right = null;
			insertSetList(setHead);
		}

		theSim.initializeSimulator(true);
	}

	/**
	 * Method to set the currently-selected signal undefined at the current time.
	 */
	public void setSignalX()
	{
		makeThemThus(Stimuli.LOGIC_X);
	}

	/**
	 * Method to show information about the currently-selected signal.
	 */
	public void showSignalInfo()
	{
		List<Signal> signals = ww.getHighlightedNetworkNames();
		if (signals.size() == 0)
		{
			Job.getUserInterface().showErrorMessage("Must select a signal before displaying it",
				"No Signals Selected");
			return;
		}
		for(Signal sig : signals)
		{
			Node nodeHead = findNode(sig.getFullName());
			if (nodeHead == null)
			{
				System.out.println("ERROR: Unable to find node " + sig.getFullName());
				continue;
			}

			String s1 = Stimuli.describeLevel(((Integer)nodeHead.newState).intValue());
			System.out.println("Node " + sig.getFullName() + ": State = " + s1 +
				", Strength = " + Stimuli.describeStrength(nodeHead.newStrength));
			for(Stat statHead : nodeHead.statList)
			{
				s1 = Stimuli.describeLevel(statHead.newState);
				System.out.println("Primitive " + statHead.primPtr.name + ":    State = " + s1 +
					", Strength = " + Stimuli.describeStrength(statHead.newStrength));
			}
		}
	}

	/**
	 * Method to remove all stimuli from the currently-selected signals.
	 */
	public void removeStimuliFromSignal()
	{
		List<Signal> signals = ww.getHighlightedNetworkNames();
		if (signals.size() == 0)
		{
			Job.getUserInterface().showErrorMessage("Must select a signal on which to clear stimuli",
				"No Signals Selected");
			return;
		}
		for(Signal sig : signals)
		{
			sig.clearControlPoints();

			Link lastSet = null;
			Link nextSet = null;
			for(Link thisSet = setRoot; thisSet != null; thisSet = nextSet)
			{
				nextSet = thisSet.right;
				boolean delete = false;
				if (thisSet.ptr instanceof Node)
				{
					Node node = (Node)thisSet.ptr;
					if (node.sig == sig) delete = true;
				} else if (thisSet.ptr instanceof Row)
				{
					Row clokHead = (Row)thisSet.ptr;
					Iterator<Object> cIt = clokHead.inList.iterator();
					if (cIt.hasNext())
					{
						Link vectHead = (Link)cIt.next();
						Node node = (Node)vectHead.ptr;
						if (node.sig == sig) delete = true;
					}
				}
				if (delete)
				{
					if (lastSet == null) setRoot = nextSet; else
						lastSet.right = nextSet;
				} else
				{
					lastSet = thisSet;
				}
			}
		}
		if (Simulation.isBuiltInResimulateEach())
		{
			theSim.initializeSimulator(true);
		}
	}

	/**
	 * Method to remove the selected stimuli.
	 */
	public void removeSelectedStimuli()
	{
		boolean found = false;
		for(Iterator<Panel> it = ww.getPanels(); it.hasNext(); )
		{
			Panel wp = it.next();
			for(WaveSignal ws : wp.getSignals())
			{
				if (!ws.isHighlighted()) continue;
				double [] selectedCPs = ws.getSelectedControlPoints();
				if (selectedCPs == null) continue;
				for(int i=0; i<selectedCPs.length; i++)
				{
					Signal sig = ws.getSignal();
					Link lastSet = null;
					Link nextSet = null;
					for(Link thisSet = setRoot; thisSet != null; thisSet = nextSet)
					{
						nextSet = thisSet.right;
						boolean delete = false;
						if (thisSet.time == selectedCPs[i])
						{
							if (thisSet.ptr instanceof Node)
							{
								Node node = (Node)thisSet.ptr;
								if (node.sig == sig) delete = true;
							} else if (thisSet.ptr instanceof Row)
							{
								Row clokHead = (Row)thisSet.ptr;
								Iterator<Object> cIt = clokHead.inList.iterator();
								if (cIt.hasNext())
								{
									Link vectHead = (Link)cIt.next();
									Node node = (Node)vectHead.ptr;
									if (node.sig == sig) delete = true;
								}
							}
						}
						if (delete)
						{
							sig.removeControlPoint(thisSet.time);
							if (lastSet == null) setRoot = nextSet; else
								lastSet.right= nextSet;
							found = true;
							break;
						} else
						{
							lastSet = thisSet;
						}
					}
				}
			}
		}
		if (!found)
		{
			System.out.println("There are no selected control points to remove");
			return;
		}
		if (Simulation.isBuiltInResimulateEach())
		{
			theSim.initializeSimulator(true);
		}
	}

	/**
	 * Method to remove all stimuli from the simulation.
	 */
	public void removeAllStimuli()
	{
		clearAllVectors(false);
		for(Iterator<Panel> it = ww.getPanels(); it.hasNext(); )
		{
			Panel wp = it.next();
			for(WaveSignal ws : wp.getSignals())
			{
				Signal sig = ws.getSignal();
				sig.clearControlPoints();
			}
		}
		if (Simulation.isBuiltInResimulateEach())
		{
			theSim.initializeSimulator(true);
		}
	}

	/**
	 * Method to save the current stimuli information to disk.
	 */
	public void saveStimuli()
	{
		String stimuliFileName = OpenFile.chooseOutputFile(FileType.ALSVECTOR, "ALS Vector file", an.getStimuli().getCell().getName() + ".vec");
		if (stimuliFileName ==  null) return;
		try
		{
			PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(stimuliFileName)));
			List<String> stimuliList = getStimuliToSave();
			for(String str : stimuliList)
				printWriter.println(str);
			printWriter.close();
		} catch (IOException e)
		{
			System.out.println("Error writing results");
			return;
		}
		System.out.println("Wrote " + stimuliFileName);
	}

	/**
	 * Method to restore the current stimuli information from disk.
	 */
	public void restoreStimuli()
	{
		String stimuliFileName = OpenFile.chooseInputFile(FileType.ALSVECTOR, "ALS Vector file");
		if (stimuliFileName == null) return;
		List<String> stimuliList = new ArrayList<String>();
		URL url = TextUtils.makeURLToFile(stimuliFileName);
		try
		{
			URLConnection urlCon = url.openConnection();
			InputStreamReader is = new InputStreamReader(urlCon.getInputStream());
			LineNumberReader lineReader = new LineNumberReader(is);
			for(;;)
			{
				String s1 = lineReader.readLine();
				if (s1 == null) break;
				stimuliList.add(s1);
			}
			lineReader.close();
		} catch (IOException e)
		{
			System.out.println("Error reading " + stimuliFileName);
			return;
		}

		processStimuliList(stimuliList);
	}

	/********************************** INTERFACE SUPPORT **********************************/

	private void init()
	{
		// create the user-defined functions
		new UserCom.PMOSTran(this);
		new UserCom.PMOSTranWeak(this);
		new UserCom.NMOSTran(this);
		new UserCom.NMOSTranWeak(this);
		new UserCom.JKFlop(this);
		new UserCom.DFFlop(this);
		new UserCom.BusToState(this);
		new UserCom.StateToBus(this);

		// not used:
//		new UserCom.Counter();
//		new UserCom.DelayCalc();
//		new UserCom.FIFO();
//		new UserCom.RXData();
//		new UserCom.AFRegisters();
//		new UserCom.ControlLogic();
//		new UserCom.Mod2Adder();
//		new UserCom.AboveAdder();
//		new UserCom.Bus12ToState();

		// allocate memory
		if (instBuf == null)
			instBuf = new char[iBufSize=100];
		if (instPtr == null)
			instPtr = new int[iPtrSize=100];
	}

	private void doSimulation(Cell netlistCell, Cell cell, WaveformWindow oldWW, List<String> stimuliList)
	{
		// initialize memory
		init();

		// read netlist
		eraseModel();
		if (readNetDesc(netlistCell)) return;
		if (theFlat.flattenNetwork(cell)) return;

		// initialize display
		an = getCircuit(cell);
		ww = oldWW;

		Simulation.showSimulationData(an.getStimuli(), ww);

		// make a waveform window
		if (ww == null)
			ww = an.getStimuli().getWaveformWindow();
		ww.setSimEngine(this);

		if (stimuliList != null) processStimuliList(stimuliList);

		// run simulation
		theSim.initializeSimulator(true);
	}

	private void makeThemThus(int state)
	{
		List<Signal> signals = ww.getHighlightedNetworkNames();
		if (signals.size() == 0)
		{
			Job.getUserInterface().showErrorMessage("Must select a signal on which to set stimuli",
				"No Signals Selected");
			return;
		}
		String [] parameters = new String[1];
		for(Signal sig : signals)
		{
			String sigName = sig.getFullName();
			Node nodeHead = findNode(sigName);
			if (nodeHead == null)
			{
				System.out.println("ERROR: Unable to find node " + sigName);
				return;
			}

			int strength = Stimuli.NODE_STRENGTH;
			double time = ww.getMainXPositionCursor();

			Link setHead = new Link();
			setHead.type = 'N';
			setHead.ptr = nodeHead;
			setHead.state = new Integer(state);
			setHead.strength = strength;
			setHead.priority = 2;
			setHead.time = time;
			setHead.right = null;
			sig.addControlPoint(time);
			insertSetList(setHead);

//			System.out.println("Node '" + sigName + "' scheduled, state = " + state +
//				", strength = " + Stimuli.describeStrength(strength) + ", time = " + time);
		}

		if (Simulation.isBuiltInResimulateEach())
		{
			double endTime = theSim.initializeSimulator(true);
			if (Simulation.isBuiltInAutoAdvance()) ww.setMainXPositionCursor(endTime);
		}
	}

	/**
	 * Method to insert a data element into a linklist that is sorted
	 * by time and then priority.  This link list is used to schedule events
	 * for the simulation.
	 *
	 * Calling Arguments:
	 *	linkHead = pointer to the data element that is going to be inserted
	 */
	void insertSetList(Link linkHead)
	{
		// linkPtr1Is: 0: ALS.setRoot  1: linkptr1.right
		int linkPtr1Is = 0;
		Link linkPtr1 = null;

		for(;;)
		{
			Link linkPtr2 = setRoot;
			if (linkPtr1Is == 1) linkPtr2 = linkPtr1.right;
			if (linkPtr2 == null)
			{
				if (linkPtr1Is == 0) setRoot = linkHead; else
					linkPtr1.right = linkHead;
				break;
			}
			if (linkPtr2.time > linkHead.time || (linkPtr2.time == linkHead.time &&
				linkPtr2.priority > linkHead.priority))
			{
				linkHead.right = linkPtr2;
				if (linkPtr1Is == 0) setRoot = linkHead; else
					linkPtr1.right = linkHead;
				break;
			}
			linkPtr1 = linkPtr2;
			linkPtr1Is = 1;
		}
	}

	private void eraseModel()
	{
		// reset miscellaneous simulation variables
		linkFront = null;
		linkBack = null;

		// delete all test vectors
		clearAllVectors(true);

		// delete all cells in flattened network
		cellRoot = null;

		// delete all nodes in flattened network
		nodeList = new ArrayList<Node>();

		// delete all primitives in flattened network
		primList = new ArrayList<Model>();

		// delete each model/gate/function in hierarchical description
		modelList = new ArrayList<Model>();
	}

	/**
	 * Method to clear all test vectors (even the power and ground vectors if "pwrGnd"
	 * is true).
	 */
	private void clearAllVectors(boolean pwrGnd)
	{
		Link lastSet = null;
		Link nextSet = null;
		for(Link thisSet = setRoot; thisSet != null; thisSet = nextSet)
		{
			nextSet = thisSet.right;
			if (pwrGnd || thisSet.strength != Stimuli.VDD_STRENGTH)
			{
				if (lastSet == null) setRoot = nextSet; else
					lastSet.right= nextSet;
			} else
			{
				lastSet = thisSet;
			}
		}
	}

	private Analysis getCircuit(Cell cell)
	{
		// convert the stimuli
		Stimuli sd = new Stimuli();
		sd.setDataType(FileType.ALS);
		sd.setEngine(this);
		Analysis an = new Analysis(sd, Analysis.ANALYSIS_SIGNALS);
		sd.setSeparatorChar('.');
		sd.setCell(cell);
		String topLevelName = cell.getName().toUpperCase();
		for(Connect cr = cellRoot; cr != null; cr = cr.next)
		{
			if (cr.modelName.equals(topLevelName))
			{
				addExports(cr, an, null);
				break;
			}
		}
		return an;
	}

	private void addExports(Connect cr, Analysis an, String context)
	{
		// determine type of model
		for(Model modPtr1 : modelList)
		{
			if (modPtr1.name.equals(cr.modelName))
			{
				if (modPtr1.type != 'M') return;
				break;
			}
		}
		for(ALSExport e : cr.exList)
		{
			if (e.nodePtr.sig != null) continue;
			DigitalSignal sig = new DigitalSignal(an);
			e.nodePtr.sig = sig;
			sig.setSignalContext(context);
			sig.setSignalName((String)e.nodeName);
			sig.buildTime(2);
			sig.buildState(2);
			sig.setTime(0, 0);
			sig.setTime(1, DEFTIMERANGE);
			sig.setState(0, 0);
			sig.setState(1, 0);
		}
		String subContext = context;
		if (subContext == null) subContext = ""; else subContext += ".";
		for(Connect child = cr.child; child != null; child = child.next)
		{
			addExports(child, an, subContext + child.instName);
		}
	}

	private List<String> getStimuliToSave()
	{
		List<String> stimuliList = new ArrayList<String>();
		for (Link setHead = setRoot; setHead != null; setHead = setHead.right)
		{
			switch (setHead.type)
			{
				case 'C':
					Row clokHead = (Row)setHead.ptr;
					List<Object> vectList = clokHead.inList;
					boolean first = true;
					for(Object obj : vectList)
					{
						Link vectHead = (Link)obj;
						if (first)
						{
							String s1 = computeNodeName((Node)vectHead.ptr);
							stimuliList.add("CLOCK " + s1 + " D=" + clokHead.delta + " L=" + clokHead.linear +
								" E=" + clokHead.exp + " STRENGTH=" + Stimuli.strengthToIndex(vectHead.strength) + " TIME=" + setHead.time + " CYCLES=" + setHead.state);
							first = false;
						}
						String s2 = Stimuli.describeLevelBriefly(((Integer)vectHead.state).intValue());
						stimuliList.add("  " + s2 + " " + vectHead.time);
					}
					break;
				case 'N':
					String s1 = computeNodeName((Node)setHead.ptr);
					String s2 = Stimuli.describeLevelBriefly(((Integer)setHead.state).intValue());
					stimuliList.add("SET " + s1 + "=" + s2 + "@" + Stimuli.strengthToIndex(setHead.strength) + " TIME=" + setHead.time);
			}
		}
		return stimuliList;
	}

	private void processStimuliList(List<String> stimuliList)
	{
		// clear all vectors
		while (setRoot != null)
		{
			setRoot = setRoot.right;
		}
		for(Iterator<Panel> it = ww.getPanels(); it.hasNext(); )
		{
			Panel wp = it.next();
			for(WaveSignal ws : wp.getSignals())
			{
				Signal sig = ws.getSignal();
				sig.clearControlPoints();
			}
		}

		boolean flag = true;
		String [] parts = null;
		Iterator<String> sIt = stimuliList.iterator();
		for(;;)
		{
			String s1 = null;
			if (flag)
			{
				if (!sIt.hasNext())
				{
					theSim.initializeSimulator(true);
					break;
				}
				s1 = sIt.next();
				s1 = s1.toUpperCase();
				parts = fragmentCommand(s1);
			}
			flag = true;
			if (parts == null || parts.length < 1) continue;

			String command = parts[0];
			if (command.equals("CLOCK"))
			{
				if (parts.length < 14)
				{
					System.out.println("Error: CLOCK stimuli line has only " + parts.length + " fields: " + s1);
					continue;
				}
				String nodeName = parts[1];
				Node nodeHead = findNode(nodeName);
				if (nodeHead == null)
				{
					System.out.println("ERROR: Unable to find node " + nodeName);
					continue;
				}
				int strength = Stimuli.indexToStrength(TextUtils.atoi(parts[9]));

				Link setHead = new Link();
				setHead.type = 'C';
				Row clokHead = new Row();
				setHead.ptr = clokHead;
				setHead.state = new Integer(TextUtils.atoi(parts[13]));
				setHead.priority = 1;
				setHead.time = TextUtils.atof(parts[11]);
				setHead.right = null;
				insertSetList(setHead);
				if (nodeHead.sig != null)
					nodeHead.sig.addControlPoint(setHead.time);

				clokHead.delta = TextUtils.atof(parts[3]);
				clokHead.linear = TextUtils.atof(parts[5]);
				clokHead.exp = TextUtils.atof(parts[7]);
				clokHead.abs = 0;
				clokHead.random = 0;
				clokHead.next = null;
				clokHead.delay = null;

				clokHead.inList = new ArrayList<Object>();
				for(;;)
				{
					if (!sIt.hasNext())
					{
						theSim.initializeSimulator(false);
						return;
					}
					s1 = sIt.next();
					parts = fragmentCommand(s1.toUpperCase());
					String com = parts[0];
					if (com.equals("CLOCK") || com.equals("SET"))
					{
						flag = false;
						break;
					}
					Link vectPtr2 = new Link();
					vectPtr2.type = 'N';
					vectPtr2.ptr = nodeHead;
					vectPtr2.state = new Integer(Stimuli.parseLevel(com));
					vectPtr2.strength = strength;
					vectPtr2.priority = 1;
					vectPtr2.time = TextUtils.atof(parts[1]);
					vectPtr2.right = null;
					clokHead.inList.add(vectPtr2);
				}
			}

			if (command.equals("SET"))
			{
				if (parts.length < 6)
				{
					System.out.println("Error: SET stimuli line has only " + parts.length + " fields: " + s1);
					continue;
				}
				String nodeName = parts[1];
				Node nodeHead = findNode(nodeName);
				if (nodeHead == null)
				{
					System.out.println("ERROR: Unable to find node " + nodeName);
					continue;
				}
				Link setHead = new Link();
				setHead.type = 'N';
				setHead.ptr = nodeHead;
				setHead.state = new Integer(Stimuli.parseLevel(parts[2]));
				setHead.strength = Stimuli.indexToStrength(TextUtils.atoi(parts[3]));
				setHead.priority = 2;
				setHead.time = TextUtils.atof(parts[5]);
				setHead.right = null;
				insertSetList(setHead);
				if (nodeHead.sig != null)
					nodeHead.sig.addControlPoint(setHead.time);
			}
		}
	}

	/**
	 * Method to processe the string specified by the calling argument
	 * and fragments it into a series of smaller character strings, each of which
	 * is terminated by a null character.  Returns true on error.
	 *
	 * Calling Arguments:
	 *	line = pointer to the character string to be fragmented
	 */
	private String [] fragmentCommand(String line)
	{
		List<String> fragments = new ArrayList<String>();
		line = line.trim();
		for(int i=0; i<line.length(); i++)
		{
			int spacePos = line.indexOf(' ', i);
			if (spacePos < 0) spacePos = line.length();
			int equalPos = line.indexOf('=', i);
			if (equalPos < 0) equalPos = line.length();
			int atPos = line.indexOf('@', i);
			if (atPos < 0) atPos = line.length();
			int pos = Math.min(Math.min(spacePos, equalPos), atPos);
			if (pos < 0)
			{
				fragments.add(line.substring(i));
				break;
			}
			fragments.add(line.substring(i, pos));
			i = pos;
		}
		String [] parts = new String[fragments.size()];
		for(int i=0; i<fragments.size(); i++) parts[i] = fragments.get(i);
		return parts;
	}

	/**
	 * Method to return a pointer to a structure in the cross reference
	 * table. The calling argument string contains information detailing the path
	 * name to the desired level in the cross reference table.
	 *
	 * Calling Argument:
	 *	sp = pointer to char string containing path name to level in xref table
	 */
	Connect findLevel(String sp)
	{
		Connect cellPtr = cellRoot;

		if (sp.startsWith(".")) sp = sp.substring(1);
		while (sp.length() > 0)
		{
			String part = sp;
			sp = "";
			int dotPos = part.indexOf('.');
			if (dotPos >= 0)
			{
				sp = part.substring(dotPos+1);
				part = part.substring(0, dotPos);
			}
			for( ; ; cellPtr = cellPtr.next)
			{
				if (cellPtr == null) return null;
				if (part.equals(cellPtr.instName))
				{
					if (sp.length() > 0)
						cellPtr = cellPtr.child;
					break;
				}
			}
		}
		return cellPtr;
	}

	/**
	 * Method to compose a character string which indicates the node name
	 * for the nodePtr specified in the calling argument.
	 *
	 * Calling Arguments:
	 *	nodeHead = pointer to desired node in database
	 *	sp	= pointer to char string where complete name is to be saved
	 */
	String computeNodeName(Node nodeHead)
	{
		Connect cellHead = nodeHead.cellPtr;
		String sp = computePathName(cellHead);

		for(ALSExport exHead : cellHead.exList)
		{
			if (nodeHead == exHead.nodePtr)
			{
				sp += "." + exHead.nodeName;
				return sp;
			}
		}
		return "";
	}

	/**
	 * Method to compose a character string which indicates the path name
	 * to the level of hierarchy specified in the calling argument.
	 *
	 * Calling Arguments:
	 *	cellHead = pointer to desired level of hierarchy
	 *	sp	= pointer to char string where path name is to be saved
	 */
	String computePathName(Connect cellHead)
	{
		StringBuffer infstr = new StringBuffer();
		for ( ; cellHead != null; cellHead = cellHead.parent)
			infstr.append("." + cellHead.instName);
		return infstr.toString();
	}

	/**
	 * Method to return a pointer to the calling routine which indicates
	 * the address of the node entry in the database.  The calling argument string
	 * contains information detailing the path name to the desired node.
	 *
	 * Calling Argument:
	 *	sp = pointer to char string containing path name to node
	 */
	private Node findNode(String sp)
	{
		if (sp.startsWith("$N"))
		{
			int i = TextUtils.atoi(sp.substring(2));
			for(Node nodeHead : nodeList)
			{
				if (nodeHead.num == i) return nodeHead;
			}
			return null;
		}

		int dotPos = sp.lastIndexOf('.');
		String s2 = sp;
		Connect cellPtr = findLevel(an.getStimuli().getCell().getName().toUpperCase());
		if (dotPos >= 0)
		{
			s2 = sp.substring(dotPos+1);
			cellPtr = findLevel(sp.substring(0, dotPos));
		}
		if (cellPtr == null) cellPtr = cellRoot;

		for(ALSExport exHead : cellPtr.exList)
		{
			if (exHead.nodeName.equals(s2)) return exHead.nodePtr;
		}

		return null;
	}

	/********************************** PARSING NETLISTS **********************************/

	/**
	 * Method to read a netlist description of the logic network
	 * to be analysed in other procedures.  Returns true on error.
	 */
	private boolean readNetDesc(Cell cell)
	{
		netlistStrings = cell.getTextViewContents();
		if (netlistStrings == null)
		{
			System.out.println("No netlist information found in " + cell);
			return true;
		}
		netlistStringPoint = 0;
		System.out.println("Simulating netlist in " + cell);

		instPtr[0] = -1;
		iPtr = 0;

		for(;;)
		{
			String s1 = getAString();
			if (s1 == null) break;

			if (s1.equals("GATE") || s1.equals("FUNCTION") || s1.equals("MODEL"))
			{
				if (parseStructHeader(s1.charAt(0))) return true;
				continue;
			}

			System.out.println("ERROR: String '" + s1 + "' invalid (expecting gate, function, or model)");
			return true;
		}
		return false;
	}

	/**
	 * Method to parse the input text used to describe the header for
	 * a top level structure (gate, function, model).  The structure name and
	 * argument list (exported node names) are entered into the database.  Returns
	 * nonzero on error.
	 * @param flag char representing the type of structure to be parsed
	 */
	private boolean parseStructHeader(char flag)
	{
		String s1 = getAName();
		if (s1 == null)
		{
			System.out.println("Structure declaration: EOF unexpectedly found");
			return true;
		}
		for(Model modPtr1 : modelList)
		{
			if (modPtr1.name.equals(s1))
			{
				System.out.println("ERROR: Structure " + s1 + " already defined");
				return true;
			}
		}
		modPtr2 = new Model(s1, flag);
		modPtr2.fanOut = 1;
		modelList.add(modPtr2);

		s1 = getAString();
		if (s1 == null)
		{
			System.out.println("Structure declaration: EOF unexpectedly found");
			return true;
		}
		if (!s1.startsWith("("))
		{
			System.out.println("Structure declaration: Expecting to find '(' in place of string '" + s1 + "'");
			return true;
		}

		for(;;)
		{
			s1 = getAName();
			if (s1 == null)
			{
				System.out.println("Structure declaration: EOF unexpectedly found");
				return true;
			}
			if (s1.startsWith(")")) break;

			for(ALSExport exPtr1 : modPtr2.exList)
			{
				if (exPtr1.nodeName.equals(s1))
				{
					System.out.println("Node " + s1 + " specified more than once in argument list");
					return true;
				}
			}
			exPtr2 = new ALSExport();
			exPtr2.nodeName = s1;
			exPtr2.nodePtr = null;
			modPtr2.exList.add(exPtr2);
		}

		switch (flag)
		{
			case 'G':
				if (parseGate()) return true;
				return false;
			case 'F':
				if (parseFunction()) return true;
				return false;
			case 'M':
				if (parseModel()) return true;
				return false;
		}
		System.out.println("Error in parser: invalid structure type");
		return true;
	}

	/**
	 * Method to parse the text used to describe a gate entity.
	 * The user specifies truth table entries, loading factors, and timing parameters
	 * in this region of the netlist.  Returns true on error.
	 */
	private boolean parseGate()
	{
		// init delay transition name
		delay = "XX";

		deltaDef = linearDef = expDef = randomDef = absDef = 0;
		Object last = modPtr2;
		Row rowPtr2 = null;
		for(;;)
		{
			String s1 = getAString();
			if (s1 == null || s1.equals("GATE") || s1.equals("FUNCTION") || s1.equals("MODEL"))
			{
				--iPtr;
				break;
			}

			if (s1.equals("I"))
			{
				rowPtr2 = new Row();
				rowPtr2.inList = new ArrayList<Object>();
				rowPtr2.outList = new ArrayList<Object>();
				rowPtr2.delta = deltaDef;
				rowPtr2.linear = linearDef;
				rowPtr2.exp = expDef;
				rowPtr2.random = randomDef;
				rowPtr2.abs = absDef;
				rowPtr2.delay = delay;
				delay = "XX";
				rowPtr2.next = null;
				if (last instanceof Row) ((Row)last).next = rowPtr2; else
					((Model)last).ptr = rowPtr2;
				last = rowPtr2;
				ioPtr1 = rowPtr2.inList;
				if (parseNode()) return true;
				continue;
			}

			if (s1.equals("O"))
			{
				ioPtr1 = rowPtr2.outList;
				if (parseNode()) return true;
				continue;
			}

			if (s1.equals("T"))
			{
				if (parseTiming()) return true;
				continue;
			}

			if (s1.equals("D"))
			{
				if (parseDelay()) return true;
				continue;
			}

			if (s1.equals("FANOUT"))
			{
				if (parseFanOut()) return true;
				continue;
			}

			if (s1.equals("LOAD"))
			{
				if (parseLoad()) return true;
				continue;
			}

			if (s1.equals("PRIORITY"))
			{
				Integer jj = getAnInt();
				if (jj == null)
				{
					System.out.println("Priority declaration: EOF unexpectedly found");
					return true;
				}
				modPtr2.priority = jj.intValue();
				continue;
			}

			if (s1.equals("SET"))
			{
				ioPtr1 = modPtr2.setList;
				if (parseNode()) return true;
				continue;
			}

			System.out.println("ERROR: String '" + s1 + "' invalid gate syntax");
			return true;
		}
		return false;
	}

	/**
	 * Method to create an entry in the database for one of the nodes
	 * that belong to a row entry or set state entry.  Returns true on error.
	 */
	private boolean parseNode()
	{
		for(;;)
		{
			String s1 = getAName();
			if (s1 == null || s1.equals("GATE") || s1.equals("FUNCTION") ||
				s1.equals("MODEL") || s1.equals("I") || s1.equals("O") ||
				s1.equals("T") || s1.equals("FANOUT") || s1.equals("LOAD") ||
				s1.equals("PRIORITY") || s1.equals("SET"))
			{
				--iPtr;
				break;
			}

			ioPtr2 = new IO();
			ioPtr2.nodePtr = s1;
			ioPtr2.strength = Stimuli.GATE_STRENGTH;
			ioPtr1.add(ioPtr2);

			s1 = getAString();
			if (s1 == null)
			{
				System.out.println("Node declaration: EOF unexpectedly found");
				return true;
			}
			switch (s1.charAt(0))
			{
				case '=':
				case '!':
				case '>':
				case '<':
				case '+':
				case '-':
				case '*':
				case '/':
				case '%':
					break;
				default:
					System.out.println("Gate declaration: Invalid Operator '" + s1 + "'");
					return true;
			}
			ioPtr2.operatr = s1.charAt(0);

			s1 = getAString();
			if (s1 == null)
			{
				System.out.println("Node declaration: EOF unexpectedly found");
				return true;
			}
			if (s1.equals("L") || s1.equals("X") || s1.equals("H"))
			{
				ioPtr2.operand = new Integer(Stimuli.parseLevel(s1));
			} else
			{
				--iPtr;
				if (s1.charAt(0) == '+' || s1.charAt(0) == '-' || TextUtils.isDigit(s1.charAt(0)))
				{
					Integer jj = getAnInt();
					if (jj == null)
					{
						System.out.println("Node declaration: EOF unexpectedly found");
						return true;
					}
					ioPtr2.operand = jj;
				} else
				{
					s1 = getAName();
					if (s1 == null)
					{
						System.out.println("Node declaration: EOF unexpectedly found");
						return true;
					}
					ioPtr2.operand = s1;
					ioPtr2.operatr += 128;
				}
			}

			s1 = getAString();
			if (s1 == null || !s1.startsWith("@"))
			{
				--iPtr;
				continue;
			}
			Integer jj = getAnInt();
			if (jj == null)
			{
				System.out.println("Node declaration: EOF unexpectedly found");
				return true;
			}
			ioPtr2.strength = Stimuli.indexToStrength(jj.intValue());
		}
		return false;
	}

	/**
	 * Method to insert timing values into the appropriate places in
	 * the database.  Returns true on error.
	 */
	private boolean parseTiming()
	{
		deltaDef = linearDef = expDef = randomDef = absDef = 0;

		for(;;)
		{
			String s1 = getAString();
			if (s1 == null)
			{
				System.out.println("Timing declaration: EOF unexpectedly found");
				return true;
			}

			String s2 = getAString();
			if (s2 == null)
			{
				System.out.println("Timing declaration: EOF unexpectedly found");
				return true;
			}
			if (!s2.startsWith("="))
			{
				System.out.println("Timing declaration: Invalid Operator '" + s2 + "' (expecting '=')");
				return true;
			}

			Double value = getADouble();
			if (value == null)
			{
				System.out.println("Timing declaration: EOF unexpectedly found");
				return true;
			}

			switch (s1.charAt(0))
			{
				case 'A':
					absDef = value.doubleValue();
					break;
				case 'D':
					deltaDef = value.doubleValue();
					break;
				case 'E':
					expDef = value.doubleValue();
					break;
				case 'L':
					linearDef = value.doubleValue();
					break;
				case 'R':
					randomDef = value.doubleValue();
					if (value.doubleValue() > 0.0) modPtr2.priority = 2;
					break;
				default:
					System.out.println("Invalid timing mode '" + s1 + "'");
					return true;
			}

			s1 = getAString();
			if (s1 == null || !s1.startsWith("+"))
			{
				--iPtr;
				break;
			}
		}
		return false;
	}

	/**
	 * Method to set the delay transition type for the current input state.
	 */
	private boolean parseDelay()
	{
		String s1 = getAString();
		if (s1 == null)
		{
			System.out.println("Timing declaration: EOF unexpectedly found");
			return true;
		}

		if (!s1.equals("01") && !s1.equals("10") && !s1.equals("OZ") && !s1.equals("Z1") &&
			!s1.equals("1Z") && !s1.equals("Z0") && !s1.equals("0X") && !s1.equals("X1") &&
			!s1.equals("1X") && !s1.equals("X0") && !s1.equals("XZ") && !s1.equals("ZX"))
		{
			System.out.println("Invalid delay transition name '" + s1 + "'");
			return true;
		}

		delay = s1;
		return false;
	}

	/**
	 * Method to set a flag in the model data structure regarding
	 * if fanout calculations are to be performed for this models output.
	 * If fanout calculations are required the model should have a single output.
	 * Returns true on error.
	 */
	private boolean parseFanOut()
	{
		String s1 = getAString();
		if (s1 == null)
		{
			System.out.println("Fanout declaration: EOF unexpectedly found");
			return true;
		}
		if (!s1.startsWith("="))
		{
			System.out.println("Fanout declaration: Invalid Operator '" + s1 + "' (expecting '=')");
			return true;
		}

		s1 = getAString();
		if (s1 == null)
		{
			System.out.println("Fanout declaration: EOF unexpectedly found");
			return true;
		}

		if (s1.equals("ON"))
		{
			modPtr2.fanOut = 1;
			return false;
		}

		if (s1.equals("OFF"))
		{
			modPtr2.fanOut = 0;
			return false;
		}

		System.out.println("Fanout declaration: Invalid option '" + s1 + "'");
		return true;
	}

	/**
	 * Method to enter the capacitive load rating (on per unit basis)
	 * into the database for the specified node.  Returns true on error.
	 */
	private boolean parseLoad()
	{
		for(;;)
		{
			String s1 = getAName();
			if (s1 == null || s1.equals("GATE") || s1.equals("FUNCTION") || s1.equals("MODEL") ||
				s1.equals("I") || s1.equals("O") || s1.equals("T") || s1.equals("FANOUT") ||
				s1.equals("LOAD") || s1.equals("PRIORITY") || s1.equals("SET"))
			{
				--iPtr;
				break;
			}

			String s2 = getAString();
			if (s2 == null)
			{
				System.out.println("Load declaration: EOF unexpectedly found");
				return true;
			}
			if (s2.charAt(0) != '=')
			{
				System.out.println("Load declaration: Invalid Operator '" + s2 + "' (expecting '=')");
				return true;
			}

			Double load = getADouble();
			if (load == null)
			{
				System.out.println("Load declaration: EOF unexpectedly found");
				return true;
			}

			Load loadPtr2 = new Load();
			loadPtr2.ptr = s1;
			loadPtr2.load = load.doubleValue();
			modPtr2.loadList.add(loadPtr2);
		}
		return false;
	}

	/**
	 * Method to parse the text used to describe a model entity.
	 * The user specifies the interconnection of lower level primitives (gates and
	 * functions) in this region of the netlist.  Returns true on error.
	 */
	private boolean parseModel()
	{
		for(;;)
		{
			String s1 = getAName();
			if (s1 == null || s1.equals("GATE") || s1.equals("FUNCTION") || s1.equals("MODEL"))
			{
				--iPtr;
				break;
			}

			if (s1.charAt(0) == '}') continue;

			if (s1.equals("SET"))
			{
				ioPtr1 = modPtr2.setList;
				if (parseNode()) return true;
				continue;
			}

			for(Object conptr1 = modPtr2.ptr; conptr1 != null; conptr1 = ((Connect)conptr1).next)
			{
				Connect cp1 = (Connect)conptr1;
				if (cp1.instName.equals(s1))
				{
					System.out.println("ERROR: Instance name '" + s1 + "' defined more than once");
					return true;
				}
			}
			Connect conPtr2 = new Connect();
			conPtr2.instName = s1;
			conPtr2.modelName = null;
			conPtr2.exList = new ArrayList<ALSExport>();
			conPtr2.next = (Connect)modPtr2.ptr;
			modPtr2.ptr = conPtr2;

			s1 = getAName();
			if (s1 == null)
			{
				System.out.println("Model declaration: EOF unexpectedly found");
				return true;
			}
			conPtr2.modelName = s1;

			s1 = getAString();
			if (s1 == null)
			{
				System.out.println("Model declaration: EOF unexpectedly found");
				return true;
			}
			if (s1.charAt(0) != '(')
			{
				System.out.println("Model declaration: Expecting to find '(' in place of string '" + s1 + "'");
				return true;
			}

			for(;;)
			{
				s1 = getAName();
				if (s1 == null)
				{
					System.out.println("Model declaration: EOF unexpectedly found");
					return true;
				}
				if (s1.charAt(0) == ')') break;
				exPtr2 = new ALSExport();
				exPtr2.nodePtr = null;
				exPtr2.nodeName = s1;
				conPtr2.exList.add(exPtr2);
			}
		}
		return false;
	}

	/**
	 * Method to parse the text used to describe a function entity.
	 * The user specifies input entries, loading factors, and timing parameters
	 * in this region of the netlist.
	 */
	private boolean parseFunction()
	{
		modPtr2.fanOut = 0;
		Func funcHead = new Func();
		modPtr2.ptr = funcHead;
		funcHead.procPtr = null;
		funcHead.inList = new ArrayList<ALSExport>();
		funcHead.delta = 0;
		funcHead.linear = 0;
		funcHead.exp = 0;
		funcHead.abs = 0;
		funcHead.random = 0;
		funcHead.userPtr = null;

		for(;;)
		{
			String s1 = getAString();
			if (s1 == null || s1.equals("GATE") || s1.equals("FUNCTION") || s1.equals("MODEL"))
			{
				--iPtr;
				break;
			}

			if (s1.equals("I"))
			{
				parseFuncInput(funcHead);
				continue;
			}

			if (s1.equals("O"))
			{
				if (parseFuncOutput()) return true;
				continue;
			}

			if (s1.equals("T"))
			{
				if (parseTiming()) return true;
				funcHead.delta = deltaDef;
				funcHead.linear = linearDef;
				funcHead.exp = expDef;
				funcHead.abs = absDef;
				funcHead.random = randomDef;
				continue;
			}

			if (s1.equals("LOAD"))
			{
				if (parseLoad()) return true;
				continue;
			}

			if (s1.equals("PRIORITY"))
			{
				Integer jj = getAnInt();
				if (jj == null)
				{
					System.out.println("Priority declaration: EOF unexpectedly found");
					return true;
				}
				modPtr2.priority = jj.intValue();
				continue;
			}

			if (s1.equals("SET"))
			{
				ioPtr1 = modPtr2.setList;
				parseNode();
				continue;
			}

			System.out.println("ERROR: String '" + s1 + "' invalid function syntax");
			return true;
		}
		return false;
	}

	/**
	 * Method to create a list of input nodes which are used for event
	 * driving the function.
	 */
	private void parseFuncInput(Func funcHead)
	{
		for(;;)
		{
			String s1 = getAName();
			if (s1 == null || s1.equals("GATE") || s1.equals("FUNCTION") || s1.equals("MODEL") ||
				s1.equals("I") || s1.equals("O") || s1.equals("T") || s1.equals("FANOUT") ||
				s1.equals("LOAD") || s1.equals("PRIORITY") || s1.equals("SET"))
			{
				--iPtr;
				break;
			}

			exPtr2 = new ALSExport();
			exPtr2.nodePtr = null;
			exPtr2.nodeName = s1;
			funcHead.inList.add(exPtr2);
		}
	}

	private Node dummyNode = new Node();

	/**
	 * Method to create a list of output nodes for the function.
	 */
	private boolean parseFuncOutput()
	{
		for(;;)
		{
			String s1 = getAName();
			if (s1 == null || s1.equals("GATE") || s1.equals("FUNCTION") || s1.equals("MODEL") ||
				s1.equals("I") || s1.equals("O") || s1.equals("T") || s1.equals("FANOUT") ||
				s1.equals("LOAD") || s1.equals("PRIORITY") || s1.equals("SET"))
			{
				--iPtr;
				break;
			}

			boolean found = false;
			for(Iterator<ALSExport> it = modPtr2.exList.iterator(); it.hasNext(); )
			{
				exPtr2 = it.next();
				if (s1.equals(exPtr2.nodeName))
				{
					exPtr2.nodePtr = dummyNode;
					found = true;
					break;
				}
			}
			if (!found)
			{
				System.out.println("ERROR: Unable to find node " + s1 + " in port list");
				return true;
			}
		}
		return false;
	}

	/**
	 * Method to get one string from the instruction buffer.
	 * The procedure returns a null value if End Of File is encountered.
	 */
	private String getAString()
	{
		while (instPtr[iPtr] < 0 || instBuf[instPtr[iPtr]] == '#')
		{
			if (netlistStringPoint >= netlistStrings.length)
			{
				++iPtr;
				return null;
			}
			String line = netlistStrings[netlistStringPoint++];

			fragmentLine(line);
			iPtr = 0;
		}

		StringBuffer sb = new StringBuffer();
		for(int i=instPtr[iPtr]; instBuf[i] != 0; i++)
			sb.append(instBuf[i]);
		++iPtr;
		return sb.toString();
	}

	/**
	 * Method to read in the required number of strings to compose an
	 * integer value.  It is possible to have a leading +/- sign before the actual
	 * integer value.
	 */
	private Integer getAnInt()
	{
		String s1 = getAString();
		if (s1 == null) return null;

		if (s1.startsWith("+") || s1.startsWith("-"))
		{
			String s2 = getAString();
			if (s2 == null) return null;
			s1 += s2;
		}

		return new Integer(TextUtils.atoi(s1));
	}

	/**
	 * Method to reads in the required number of strings to compose a
	 * float value.  It is possible to have a leading +/- sign before the actual
	 * float value combined with the chance that the number is entered in scientific
	 * notation.
	 */
	private Double getADouble()
	{
		String s1 = getAString();
		if (s1 == null) return null;

		if (s1.startsWith("+") || s1.startsWith("-"))
		{
			String s2 = getAString();
			if (s2 == null) return null;
			s1 += s2;
		}

		if (!s1.endsWith("E"))
		{
			return new Double(TextUtils.atof(s1));
		}

		String s2 = getAString();
		if (s2 == null) return null;
		s1 += s2;

		if (s2.startsWith("+") || s2.startsWith("-"))
		{
			String s3 = getAString();
			if (s3 == null) return null;
			s1 += s3;
		}

		return new Double(TextUtils.atof(s1));
	}

	/**
	 * Method to read in the required number of strings to compose a
	 * model/node name for the element. If array subscripting is used, the
	 * brackets and argument string is spliced to the node name.
	 */
	private String getAName()
	{
		String s1 = getAString();
		if (s1 == null) return null;

		String s2 = getAString();
		if (s2 == null || !s2.startsWith("["))
		{
			--iPtr;
			return s1;
		}

		s1 = s2;
		for(;;)
		{
			s2 = getAString();
			if (s2 == null) return null;
			s1 = s2;
			if (s2.startsWith("]")) break;
		}
		return s1;
	}

	/**
	 * Method to process the string specified by the calling argument
	 * and fragments it into a series of smaller character strings, each of which
	 * is terminated by a null character.
	 */
	private void fragmentLine(String line)
	{
		int j = 0, count = 0;
		instPtr[0] = 0;
		int k = 1;

		for (int i = 0; ; ++i)
		{
			if (j > iBufSize - 3)
			{
				int newSize = iBufSize * 5;
				char [] newBuf = new char[newSize];
				for(int x=0; x<iBufSize; x++) newBuf[x] = instBuf[x];
				instBuf = newBuf;
				iBufSize = newSize;
			}
			if (k > iPtrSize - 2)
			{
				int newSize = iPtrSize * 5;
				int [] newBuf = new int[newSize];
				for(int x=0; x<iPtrSize; x++) newBuf[x] = instPtr[x];
				instPtr = newBuf;
				iPtrSize = newSize;
			}

			if (i >= line.length())
			{
				if (count != 0)
				{
					instBuf[j] = 0;
					instPtr[k] = -1;
				} else
				{
					instPtr[k-1] = -1;
				}
				break;
			}

			char chr = line.charAt(i);
			switch (chr)
			{
				case ' ':
				case ',':
				case '\t':
				case ':':
					if (count != 0)
					{
						instBuf[j] = 0;
						instPtr[k] = j+1;
						++j;
						++k;
						count = 0;
					}
					break;

				case '(':
				case ')':
				case '{':
				case '}':
				case '[':
				case ']':
				case '=':
				case '!':
				case '>':
				case '<':
				case '+':
				case '-':
				case '*':
				case '/':
				case '%':
				case '@':
				case ';':
				case '#':
					if (count != 0)
					{
						instBuf[j] = 0;
						instBuf[j+1] = chr;
						instBuf[j+2] = 0;
						instPtr[k] = j+1;
						instPtr[k+1] = j+3;
						j += 3;
						k += 2;
						count = 0;
					} else
					{
						instBuf[j] = chr;
						instBuf[j+1] = 0;
						instPtr[k] = j+2;
						j += 2;
						++k;
					}
					break;

				default:
					instBuf[j] = Character.toUpperCase(chr);
					++j;
					++count;
			}
		}
	}
}
