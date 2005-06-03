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
import com.sun.electric.tool.simulation.DigitalSignal;
import com.sun.electric.tool.simulation.Engine;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.CompileVHDL;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.TopLevel;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

public class ALS extends Engine
{
	Sim theSim;
	Flat theFlat;
	WaveformWindow ww;

	ALS()
	{
		theSim = new Sim(this);
		theFlat = new Flat(this);
	}

	Sim getSim() { return theSim; }

	/**
	 * Method to reload the circuit data.
	 */
	public void refresh()
	{
		System.out.println("CANNOT REFRESH CIRCUIT DATA YET");
	}

	/**
	 * Method to update the simulation (because some stimuli have changed).
	 */
	public void update()
	{
		theSim.simals_initialize_simulator(true);
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
		System.out.println("ALS CANNOT HANDLE CLOCKS YET");
	}

	/**
	 * Method to set the currently-selected signal undefined at the current time.
	 */
	public void setSignalX()
	{
		makeThemThus(Stimuli.LOGIC_X);
	}

	private void makeThemThus(int state)
	{
		List signals = ww.getHighlightedNetworkNames();
		String [] parameters = new String[1];
		for(Iterator it = signals.iterator(); it.hasNext(); )
		{
			Signal sig = (Signal)it.next();
			String sigName = sig.getFullName();
			Node nodehead = simals_find_node(sigName);
			if (nodehead == null)
			{
				System.out.println("ERROR: Unable to find node " + sigName);
				return;
			}

			int strength = Stimuli.NODE_STRENGTH;
			double time = ww.getMainTimeCursor();

			Link sethead = new Link();
			sethead.type = 'N';
			sethead.ptr = nodehead;
			sethead.state = new Integer(state);
			sethead.strength = strength;
			sethead.priority = 2;
			sethead.time = time;
			sethead.right = null;
			theSim.simals_insert_set_list(sethead);

//			System.out.println("Node '" + sigName + "' scheduled, state = " + state +
//				", strength = " + Command.simals_strengthstring(strength) + ", time = " + time);
		}

		if (Simulation.isIRSIMResimulateEach())
		{
			theSim.simals_initialize_simulator(true);
//			if ((sim_window_state&ADVANCETIME) != 0) sim_window_setmaincursor(endtime);
		}
	}

	/**
	 * Method to show information about the currently-selected signal.
	 */
	public void showSignalInfo()
	{
//		List signals = ww.getHighlightedNetworkNames();
//		for(Iterator it = signals.iterator(); it.hasNext(); )
//		{
//			Signal sig = (Signal)it.next();
//			SimVector excl = new SimVector();
//			excl.command = VECTOREXCL;
//			excl.sigs = new ArrayList();
//			excl.sigs.add(sig);
//			issueCommand(excl);
//
//			excl.command = VECTORQUESTION;
//			issueCommand(excl);
//		}
	}

	/**
	 * Method to remove all stimuli from the currently-selected signal.
	 */
	public void removeStimuliFromSignal()
	{
System.out.println("DOESN'T WORK YET");

		List signals = ww.getHighlightedNetworkNames();
		if (signals.size() != 1)
		{
			System.out.println("Must select a single signal on which to clear stimuli");
			return;
		}
		Signal sig = (Signal)signals.get(0);
		String highsigname = sig.getFullName();
		sig.clearControlPoints();

		Link lastset = null;
		Link nextset = null;
		for(Link thisset = simals_setroot; thisset != null; thisset = nextset)
		{
			nextset = thisset.right;
			boolean delete = false;
			if (thisset.ptr instanceof Node)
			{
				Node node = (Node)thisset.ptr;
				if (node.sig == sig) delete = true;
			}
			if (delete)
			{
				if (lastset == null) simals_setroot = nextset; else
					lastset.right= nextset;
			} else
			{
				lastset = thisset;
			}
		}
		if (Simulation.isIRSIMResimulateEach())
		{
			theSim.simals_initialize_simulator(true);
		}
	}

	/**
	 * Method to remove the selected stimuli.
	 */
	public void removeSelectedStimuli()
	{
System.out.println("DOESN'T WORK YET");

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
					Signal sig = ws.getSignal();
					Link lastset = null;
					Link nextset = null;
					for(Link thisset = simals_setroot; thisset != null; thisset = nextset)
					{
						nextset = thisset.right;
						boolean delete = false;
						if (thisset.ptr instanceof Node)
						{
							Node node = (Node)thisset.ptr;
							if (node.sig == sig && thisset.time == selectedCPs[i]) delete = true;
						}
						if (delete)
						{
							if (lastset == null) simals_setroot = nextset; else
								lastset.right= nextset;
							found = true;
							break;
						} else
						{
							lastset = thisset;
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
		if (Simulation.isIRSIMResimulateEach())
		{
			theSim.simals_initialize_simulator(true);
		}
	}

	/**
	 * Method to remove all stimuli from the simulation.
	 */
	public void removeAllStimuli()
	{
		simals_clearallvectors(false);
		if (Simulation.isIRSIMResimulateEach())
		{
			theSim.simals_initialize_simulator(true);
		}
	}

	/**
	 * Method to save the current stimuli information to disk.
	 */
	public void saveStimuli()
	{
		String stimuliFileName = OpenFile.chooseOutputFile(FileType.ALSVECTOR, "ALS Vector file", simals_mainproto.getName() + ".vec");
		if (stimuliFileName ==  null) return;
		try
		{
			PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(stimuliFileName)));

			for (Link sethead = simals_setroot; sethead != null; sethead = sethead.right)
			{
				switch (sethead.type)
				{
//					case 'C':
//						Row clokhead = (Row)sethead.ptr;
//						Link vecthead = (Link)clokhead.inptr;
//						String s1 = simals_compute_node_name((Node)vecthead.ptr);
//						printWriter.println("CLOCK " + s1 + " D=" + clokhead.delta + " L=" + clokhead.linear +
//							" E=" + clokhead.exp + " STRENGTH=" + (vecthead.strength/2) + " TIME=" + sethead.time + " CYCLES=" + sethead.state);
//						for (; vecthead != null; vecthead = vecthead.right)
//						{
//							String s2 = simals_trans_number_to_state(vecthead.state);
//							printWriter.println("  " + s2 + " " + vecthead.time);
//						}
//						break;
					case 'N':
						String s1 = simals_compute_node_name((Node)sethead.ptr);
						String s2 = simals_trans_number_to_state(((Integer)sethead.state).intValue());
						printWriter.println("SET " + s1 + "=" + s2 + "@" + (sethead.strength/2) + " TIME=" + sethead.time);
				}
			}
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
		URL url = TextUtils.makeURLToFile(stimuliFileName);
		try
		{
			URLConnection urlCon = url.openConnection();
			InputStreamReader is = new InputStreamReader(urlCon.getInputStream());
			LineNumberReader lineReader = new LineNumberReader(is);

			// clear all vectors
			while (simals_setroot != null)
			{
				simals_setroot = simals_setroot.right;
			}

			boolean flag = true;
			for(;;)
			{
				if (flag)
				{
					String s1 = lineReader.readLine();
					if (s1 == null)
					{
						theSim.simals_initialize_simulator(false);
						break;
					}
					s1 = s1.toUpperCase();
					if (simals_fragment_command(s1)) break;
				}

				if (simals_instbuf.equals("CLOCK"))
				{
//					simals_convert_to_upper(&(simals_instbuf[simals_instptr[1]]));
//					nodehead = ALS.simals_find_node(&(simals_instbuf[simals_instptr[1]]));
//					if (! nodehead)
//					{
//						System.out.println("ERROR: Unable to find node %s"),
//							&(simals_instbuf[simals_instptr[1]]));
//						flag = true;
//						continue;
//					}
//					strength = eatoi(&(simals_instbuf[simals_instptr[9]]))*2;
//
//					sethead = new ALS.Link();
//					if (sethead == 0) return;
//					sethead.type = 'C';
//					sethead.ptr = (CHAR*)(clokhead = new ALS.Row());
//					sethead.state = eatoi(&(simals_instbuf[simals_instptr[13]]));
//					sethead.priority = 1;
//					sethead.time = eatof(&(simals_instbuf[simals_instptr[11]]));
//					sethead.right = 0;
//					Sim.simals_insert_set_list(sethead);
//
//					clokhead.delta = (float)eatof(&(simals_instbuf[simals_instptr[3]]));
//					clokhead.linear = (float)eatof(&(simals_instbuf[simals_instptr[5]]));
//					clokhead.exp = (float)eatof(&(simals_instbuf[simals_instptr[7]]));
//					clokhead.abs = 0.0;
//					clokhead.random = 0.0;
//					clokhead.next = 0;
//					clokhead.delay = 0;
//
//					vectptr1 = (CHAR**) &(clokhead.inptr);
//					for(;;)
//					{
//						if (xfgets(s1, 255, vin))
//						{
//							xclose(vin);
//							Sim.simals_initialize_simulator(false);
//							return;
//						}
//						simals_convert_to_upper(s1);
//						if (simals_fragment_command(s1)) return;
//						if (!estrcmp(simals_instbuf, x_("CLOCK")) || !estrcmp(simals_instbuf, x_("SET")))
//						{
//							flag = false;
//							break;
//						}
//						vectptr2 = new ALS.Link();
//						if (vectptr2 == 0) return;
//						vectptr2.type = 'N';
//						vectptr2.ptr = (CHAR *)nodehead;
//						vectptr2.state = ALS.simals_trans_state_to_number(simals_instbuf);
//						vectptr2.strength = strength;
//						vectptr2.priority = 1;
//						vectptr2.time = eatof(&(simals_instbuf[simals_instptr[1]]));
//						vectptr2.right = 0;
//						*vectptr1 = (CHAR*) vectptr2;
//						vectptr1 = (CHAR**) &(vectptr2.right);
//					}
				}

				if (simals_instbuf.equals("SET"))
				{
//					simals_convert_to_upper(&(simals_instbuf[simals_instptr[1]]));
//					nodehead = ALS.simals_find_node(&(simals_instbuf[simals_instptr[1]]));
//					if (! nodehead)
//					{
//						System.out.println("ERROR: Unable to find node %s"),
//							&(simals_instbuf[simals_instptr[1]]));
//						flag = true;
//						continue;
//					}
//
//					sethead = new ALS.Link();
//					if (sethead == 0) return;
//					sethead.type = 'N';
//					sethead.ptr = (CHAR *) nodehead;
//					sethead.state = ALS.simals_trans_state_to_number(&(simals_instbuf[simals_instptr[2]]));
//					sethead.strength = eatoi(&(simals_instbuf[simals_instptr[3]]))*2;
//					sethead.priority = 2;
//					sethead.time = eatof(&(simals_instbuf[simals_instptr[5]]));
//					sethead.right = 0;
//					Sim.simals_insert_set_list(sethead);
//					flag = true;
				}
			}
			lineReader.close();
		} catch (IOException e)
		{
			System.out.println("Error reading " + stimuliFileName);
			return;
		}
	}

	/**
	 * Method to simulate the a Cell, given its context and the Cell with the real netlist.
	 * @param netlistCell the Cell with the real ALS netlist.
	 * @param np the original Cell being simulated.
	 * @param context the context to the Cell being simulated.
	 */
	public static void simulateNetlist(Cell netlistCell, Cell np, VarContext context)
	{
		ALS theALS = new ALS();
		theALS.doSimulation(netlistCell, np, context);
	}

	private void doSimulation(Cell netlistCell, Cell cell, VarContext context)
	{
		// initialize memory
		simals_init();

		// read netlist
		simals_erase_model();
		if (simals_read_net_desc(netlistCell, cell)) return;
		if (theFlat.simals_flatten_network()) return;

		// initialize display
		Stimuli sd = getCircuit();
		Simulation.showSimulationData(sd, null);

		// make a waveform window
		ww = sd.getWaveformWindow();
		ww.setSimEngine(this);
		ww.setDefaultTimeRange(0.0, DEFIRSIMTIMERANGE);
		ww.setMainTimeCursor(DEFIRSIMTIMERANGE/5.0*2.0);
		ww.setExtensionTimeCursor(DEFIRSIMTIMERANGE/5.0*3.0);

		simals_init_display();
	}

	void simals_erase_model()
	{
		// reset miscellaneous simulation variables
		simals_linkfront = null;
		simals_linkback = null;

		// delete all test vectors
		simals_clearallvectors(true);

		// delete all cells in flattened network
		simals_cellroot = null;
		simals_levelptr = null;

		// delete all nodes in flattened network
		simals_noderoot = null;

		// delete all primitives in flattened network
		simals_primroot = null;

		// delete each model/gate/function in hierarchical description
		simals_modroot = null;
	}

	/**
	 * Method to clear all test vectors (even the power and ground vectors if "pwrgnd"
	 * is true).
	 */
	void simals_clearallvectors(boolean pwrgnd)
	{
		Link lastset = null;
		Link nextset = null;
		for(Link thisset = simals_setroot; thisset != null; thisset = nextset)
		{
			nextset = thisset.right;
			if (pwrgnd || thisset.strength != Stimuli.VDD_STRENGTH)
			{
				if (lastset == null) simals_setroot = nextset; else
					lastset.right= nextset;
			} else
			{
				lastset = thisset;
			}
		}
	}

	/** initial size of simulation window: 10ns */		private static final double DEFIRSIMTIMERANGE = 10.0E-9f;		// should be 0.0000005f

	private Stimuli getCircuit()
	{
		// convert the stimuli
		Stimuli sd = new Stimuli();
		sd.setSeparatorChar('.');
		sd.setCell(simals_mainproto);
		String topLevelName = simals_mainproto.getName().toUpperCase();
		for(Connect cr = simals_cellroot; cr != null; cr = cr.next)
		{
			if (cr.model_name.equals(topLevelName))
			{
				addExports(cr, sd, null);
				break;
			}
		}
		return sd;
	}

	private void addExports(Connect cr, Stimuli sd, String context)
	{
		// determine type of model
		for(Model modptr1 = simals_modroot; modptr1 != null; modptr1 = modptr1.next)
		{
			if (modptr1.name.equals(cr.model_name))
			{
				if (modptr1.type != 'M') return;
				break;
			}
		}
		for(ALSExport e = cr.exptr; e != null; e = e.next)
		{
			if (e.nodeptr.sig != null) continue;
			DigitalSignal sig = new DigitalSignal(sd);
			e.nodeptr.sig = sig;
			sig.setSignalContext(context);
			sig.setSignalName((String)e.node_name);
			sig.buildTime(2);
			sig.buildState(2);
			sig.setTime(0, 0);
			sig.setTime(1, 0.00000001);
			sig.setState(0, 0);
			sig.setState(1, 0);
		}
		String subContext = context;
		if (subContext == null) subContext = ""; else subContext += ".";
		for(Connect child = cr.child; child != null; child = child.next)
		{
			addExports(child, sd, subContext + child.inst_name);
		}
	}

	/********************************* THE HEADER FILE *********************************/

	/**
	 * DelayTypes is a typesafe enum class that describes types of delay.
	 */
	public static class DelayTypes
	{
		private DelayTypes() {}

		/**
		 * Returns a printable version of this DelayTypes.
		 * @return a printable version of this DelayTypes.
		 */
		public String toString() { return ""; }

		/** Describes a minimum delay. */		public static final DelayTypes DELAY_MIN    = new DelayTypes();
		/** Describes a typical delay. */		public static final DelayTypes DELAY_TYP    = new DelayTypes();
		/** Describes a maximum delay. */		public static final DelayTypes DELAY_MAX    = new DelayTypes();
	}

	static class Model
	{
		int       num;
		String    name;
		char      type;
		Object    ptr;	/* may be Connect, Row, or Func */
		ALSExport exptr;
		List      setList;
		Load      loadptr;
		char      fanout;
		int       priority;
		Model     next;
		String    level;  /* hierarchical level */
	};

	static class Row
	{
		List   inList;
		List   outList;
		float  delta;
		float  linear;
		float  exp;
		float  random;
		float  abs;    /* BA delay - SDF absolute port delay */
		Row    next;
		String delay;  /* delay transition name (01, 10, etc) */
	};

	static class IO
	{
		Object  nodeptr;
		char    operatr;
		Object  operand;
		int     strength;
	};

	static class Connect
	{
		String    inst_name;
		String    model_name;
		ALSExport exptr;
		Connect   parent;
		Connect   child;
		Connect   next;
//		Channel   display_page;  /* pointer to the display page */
		int       num_chn;       /* number of exported channels in this level */
	};

	static class ALSExport
	{
		Object    node_name;
		Node      nodeptr;
		ALSExport next;
		int []    td = new int[12];  /* transition delays */
	};

	static class Load
	{
		Object ptr;
		float  load;
		Load   next;
	};

	static class UserProc
	{
		protected ALS als;
		private String name;

		private static HashMap funcMap = new HashMap();

		void nameMe(ALS als, String name)
		{
			this.als = als;
			this.name = name;
			funcMap.put(name, this);
		}

		void simulate(Model primhead) {}

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
		static UserProc simals_get_function_address(String s1)
		{
			UserProc ret = (UserProc)funcMap.get(s1);
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
		 *	nodehead = pointer to the node data structure to be updated
		 *	operatr  = char indicating operation to be performed on node
		 *	state    = integer value representing the state of the node
		 *	strength = integer value representing the logic stregth of the signal
		 *	time     = double value representing the time the change is to take place
		 */
		protected boolean simals_schedule_node_update(Model primhead, ALSExport exhead, int operatr,
			Object state, int strength, double time)
		{
			Stat stathead = (Stat)exhead.node_name;
			if (stathead.sched_op == operatr && stathead.sched_state == state &&
				stathead.sched_strength == strength)
			{
				return false;
			}
			if (als.getSim().simals_tracing)
			{
				String s2 = als.simals_compute_node_name(stathead.nodeptr);
				System.out.println("      Schedule(F) gate " + stathead.primptr.name + stathead.primptr.level +
					", net " + s2 + "  at " + TextUtils.convertToEngineeringNotation(time));
			}
			Link linkptr2 = new Link();
			linkptr2.type = 'G';
			linkptr2.ptr = stathead;
			linkptr2.operatr = stathead.sched_op = (char)operatr;
			linkptr2.state = stathead.sched_state = state;
			linkptr2.strength = stathead.sched_strength = strength;
			linkptr2.priority = 1;
			linkptr2.time = time;
			linkptr2.primhead = primhead;
			als.getSim().simals_insert_link_list(linkptr2);
			return false;
		}

		/**
		 * Method to examine all the elements feeding into a node that is
		 * connected to the current bidirectional element and insures that there are no
		 * bidirectional "loops" found in the network paths.  If a loop is found the
		 * effects of the element are ignored from the node summing calculation.
		 *
		 * Calling Arguments:
		 *	primhead    = pointer to current bidirectional element
		 *	side[]      = pointers to nodes on each side of the bidir element
		 *  outstrength = output strength
		 */
		private Node  simals_target_node;
		private int   simals_bidirclock = 0;

		protected void simals_calculate_bidir_outputs(Model primhead, ALSExport [] side, int outstrength)
		{
			for(int i=0; i<2; i++)
			{
				ALSExport thisside = side[i];
				ALSExport otherside = side[(i+1)%2];
				Node sum_node = thisside.nodeptr;
				simals_target_node = otherside.nodeptr;
				if (simals_target_node == als.simals_drive_node) continue;
				int state = ((Integer)sum_node.new_state).intValue();
				int strength = sum_node.new_strength;

				simals_bidirclock++;
				for(Stat stathead = sum_node.statptr; stathead != null; stathead = stathead.next)
				{
					if (stathead.primptr == primhead) continue;

					sum_node.visit = simals_bidirclock;

					int thisstrength = stathead.new_strength;
					int thisstate = stathead.new_state;

					if (thisstrength > strength)
					{
						state = thisstate;
						strength = thisstrength;
						continue;
					}

					if (thisstrength == strength)
					{
						if (thisstate != state) state = Stimuli.LOGIC_X;
					}
				}

				// make strength no more than maximum output strength
				if (strength > outstrength) strength = outstrength;

				Func funchead = (Func)primhead.ptr;
				double time = als.simals_time_abs + (funchead.delta * simals_target_node.load);
				simals_schedule_node_update(primhead, otherside, '=', new Integer(state), strength, time);
			}
		}
	}

	static class Func
	{
		UserProc   procptr;
		ALSExport  inptr;
		float      delta;
		float      linear;
		float      exp;
		float      abs;    /* absolute delay for back annotation */
		float      random;
		Object     userptr;
		int        userint;
		float      userfloat;
	};

	static class Node
	{
		Connect  cellptr;
		DigitalSignal sig;
		int      num;
		int      sum_state;
		int      sum_strength;
		Object   new_state;
		int      new_strength;
		boolean  tracenode;
		Stat     statptr;
		Load     pinptr;
		float    load;
		int      visit;
		int      maxsize;
		int      arrive;
		int      depart;
		float    tk_sec;
		double   t_last;
		Node     next;
	};

	static class Stat
	{
		Model   primptr;
		Node    nodeptr;
		int     new_state;
		int     new_strength;
		char    sched_op;
		Object  sched_state;
		int     sched_strength;
		Stat    next;
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
		Model   primhead;
	};

	static class Trak
	{
		int      state;
		double   time;
	};

	/********************************* GLOBALS *********************************/

	Model      simals_modroot = null;
	Model      simals_modptr2;
	Model      simals_primroot = null;
	IO         simals_ioptr2;
	Connect    simals_levelptr = null;
	Connect    simals_cellroot = null;
	ALSExport  simals_exptr2;
	Node       simals_noderoot = null;
	Node       simals_drive_node;
	Link       simals_linkfront = null;
	Link       simals_linkback = null;
	Link       simals_setroot = null;
	Load       simals_chekroot;
	List       simals_ioptr1;
	char    [] simals_instbuf = null;
	int        simals_pseq;
	int        simals_nseq;
	int     [] simals_instptr = null;
	String  [] netlistStrings;
	int        netlistStringPoint;
	int        simals_ibufsize;
	int        simals_iptrsize;
	boolean    simals_trace_all_nodes = false;
	double     simals_time_abs;
	float      simals_delta_def;
	float      simals_linear_def;
	float      simals_exp_def;
	float      simals_random_def;
	float      simals_abs_def;
	Cell       simals_mainproto = null;

	/********************************* LOCALS *********************************/

	int     i_ptr;
	String  delay;

	/******************************************************************************/

	void simals_init()
	{
		// allocate memory
		if (simals_instbuf == null)
		{
			simals_instbuf = new char[simals_ibufsize=100];
		}
		if (simals_instptr == null)
		{
			simals_instptr = new int[simals_iptrsize=100];
		}

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
//		new UserCom.QCalc();
//		new UserCom.Stats();
//		new UserCom.Counter();
//		new UserCom.DelayCalc();
//		new UserCom.FIFO();
//		new UserCom.RXData();
//		new UserCom.AFRegisters();
//		new UserCom.ControlLogic();
//		new UserCom.Mod2Adder();
//		new UserCom.AboveAdder();
//		new UserCom.Bus12ToState();
	}

	/*
	 * routine to create a new window with simulation of cell "simals_mainproto"
	 */
	void simals_init_display()
	{
		if (simals_mainproto == null)
		{
			System.out.println("No cell to simulate");
			return;
		}

		// set top level
		simals_levelptr = simals_cellroot;
		String pt = simals_mainproto.getName().toUpperCase();

		// run simulation
		theSim.simals_initialize_simulator(true);
	}

	/**
	 * Method to get one string from the instruction buffer.
	 * The procedure returns a null value if End Of File is encountered.
	 */
	String simals_get_string()
	{
		while (simals_instptr[i_ptr] < 0 || simals_instbuf[simals_instptr[i_ptr]] == '#')
		{
			if (netlistStringPoint >= netlistStrings.length)
			{
				++i_ptr;
				return null;
			}
			String line = netlistStrings[netlistStringPoint++];

			simals_fragment_line(line);
			i_ptr = 0;
		}

		StringBuffer sb = new StringBuffer();
		for(int i=simals_instptr[i_ptr]; simals_instbuf[i] != 0; i++)
			sb.append(simals_instbuf[i]);
		++i_ptr;
		return sb.toString();
	}

	/**
	 * Method to read in the required number of strings to compose an
	 * integer value.  It is possible to have a leading +/- sign before the actual
	 * integer value.
	 */
	private Integer simals_get_int()
	{
		String s1 = simals_get_string();
		if (s1 == null) return null;

		if (s1.startsWith("+") || s1.startsWith("-"))
		{
			String s2 = simals_get_string();
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
	Float simals_get_float()
	{
		String s1 = simals_get_string();
		if (s1 == null) return null;

		if (s1.startsWith("+") || s1.startsWith("-"))
		{
			String s2 = simals_get_string();
			if (s2 == null) return null;
			s1 += s2;
		}

		if (!s1.endsWith("E"))
		{
			return new Float(TextUtils.atof(s1));
		}

		String s2 = simals_get_string();
		if (s2 == null) return null;
		s1 += s2;

		if (s2.startsWith("+") || s2.startsWith("-"))
		{
			String s3 = simals_get_string();
			if (s3 == null) return null;
			s1 += s3;
		}

		return new Float(TextUtils.atof(s1));
	}

	/**
	 * Method to read in the required number of strings to compose a
	 * model/node name for the element. If array subscripting is used, the
	 * brackets and argument string is spliced to the node name.
	 */
	private String simals_get_name()
	{
		String s1 = simals_get_string();
		if (s1 == null) return null;

		String s2 = simals_get_string();
		if (s2 == null || !s2.startsWith("["))
		{
			--i_ptr;
			return s1;
		}

		s1 = s2;
		for(;;)
		{
			s2 = simals_get_string();
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
	private void simals_fragment_line(String line)
	{
		int j = 0, count = 0;
		simals_instptr[0] = 0;
		int k = 1;

		for (int i = 0; ; ++i)
		{
			if (j > simals_ibufsize - 3)
			{
				int newSize = simals_ibufsize * 5;
				char [] newBuf = new char[newSize];
				for(int x=0; x<simals_ibufsize; x++) newBuf[x] = simals_instbuf[x];
				simals_instbuf = newBuf;
				simals_ibufsize = newSize;
			}
			if (k > simals_iptrsize - 2)
			{
				int newSize = simals_iptrsize * 5;
				int [] newBuf = new int[newSize];
				for(int x=0; x<simals_iptrsize; x++) newBuf[x] = simals_instptr[x];
				simals_instptr = newBuf;
				simals_iptrsize = newSize;
			}

			if (i >= line.length())
			{
				if (count != 0)
				{
					simals_instbuf[j] = 0;
					simals_instptr[k] = -1;
				} else
				{
					simals_instptr[k-1] = -1;
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
						simals_instbuf[j] = 0;
						simals_instptr[k] = j+1;
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
						simals_instbuf[j] = 0;
						simals_instbuf[j+1] = chr;
						simals_instbuf[j+2] = 0;
						simals_instptr[k] = j+1;
						simals_instptr[k+1] = j+3;
						j += 3;
						k += 2;
						count = 0;
					} else
					{
						simals_instbuf[j] = chr;
						simals_instbuf[j+1] = 0;
						simals_instptr[k] = j+2;
						j += 2;
						++k;
					}
					break;

				default:
					simals_instbuf[j] = Character.toUpperCase(chr);
					++j;
					++count;
			}
		}
	}

	/**
	 * Method to read a netlist description of the logic network
	 * to be analysed in other procedures.  Returns true on error.
	 */
	boolean simals_read_net_desc(Cell cell, Cell realCell)
	{
		simals_mainproto = realCell;
		netlistStrings = cell.getTextViewContents();
		netlistStringPoint = 0;
		System.out.println("Simulating netlist in " + cell.describe());

		simals_instptr[0] = -1;
		i_ptr = 0;

		for(;;)
		{
			String s1 = simals_get_string();
			if (s1 == null) break;

			if (s1.equals("GATE") || s1.equals("FUNCTION") || s1.equals("MODEL"))
			{
				if (simals_parse_struct_header(s1.charAt(0))) return true;
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
	boolean simals_parse_struct_header(char flag)
	{
		String s1 = simals_get_name();
		if (s1 == null)
		{
			System.out.println("Structure declaration: EOF unexpectedly found");
			return true;
		}
		for(Model modptr1 = simals_modroot; modptr1 != null; modptr1 = modptr1.next)
		{
			if (modptr1.name.equals(s1))
			{
				System.out.println("ERROR: Structure " + s1 + " already defined");
				return true;
			}
		}
		simals_modptr2 = new Model();
		simals_modptr2.name = s1;
		simals_modptr2.type = flag;
		simals_modptr2.ptr = null;
		simals_modptr2.exptr = null;
		simals_modptr2.setList = new ArrayList();
		simals_modptr2.loadptr = null;
		simals_modptr2.fanout = 1;
		simals_modptr2.priority = 1;
		simals_modptr2.next = simals_modroot;
		simals_modroot = simals_modptr2;

		s1 = simals_get_string();
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
			s1 = simals_get_name();
			if (s1 == null)
			{
				System.out.println("Structure declaration: EOF unexpectedly found");
				return true;
			}
			if (s1.startsWith(")")) break;

			for(ALSExport exptr1 = simals_modptr2.exptr; exptr1 != null; exptr1 = exptr1.next)
			{
				if (exptr1.node_name.equals(s1))
				{
					System.out.println("Node " + s1 + " specified more than once in argument list");
					return true;
				}
			}
			simals_exptr2 = new ALSExport();
			simals_exptr2.node_name = s1;
			simals_exptr2.nodeptr = null;
			simals_exptr2.next = simals_modptr2.exptr;
			simals_modptr2.exptr = simals_exptr2;
		}

		switch (flag)
		{
			case 'G':
				if (simals_parse_gate()) return true;
				return false;
			case 'F':
				if (simals_parse_function()) return true;
				return false;
			case 'M':
				if (simals_parse_model()) return true;
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
	private boolean simals_parse_gate()
	{
		// init delay transition name
		delay = "XX";

		simals_delta_def = simals_linear_def = simals_exp_def = simals_random_def = simals_abs_def = 0;
		Object last = simals_modptr2;
		Row simals_rowptr2 = null;
		for(;;)
		{
			String s1 = simals_get_string();
			if (s1 == null || s1.equals("GATE") || s1.equals("FUNCTION") || s1.equals("MODEL"))
			{
				--i_ptr;
				break;
			}

			if (s1.equals("I"))
			{
				simals_rowptr2 = new Row();
				simals_rowptr2.inList = new ArrayList();
				simals_rowptr2.outList = new ArrayList();
				simals_rowptr2.delta = simals_delta_def;
				simals_rowptr2.linear = simals_linear_def;
				simals_rowptr2.exp = simals_exp_def;
				simals_rowptr2.random = simals_random_def;
				simals_rowptr2.abs = simals_abs_def;
				simals_rowptr2.delay = delay;
				delay = "XX";
				simals_rowptr2.next = null;
				if (last instanceof Row) ((Row)last).next = simals_rowptr2; else
					((Model)last).ptr = simals_rowptr2;
				last = simals_rowptr2;
				simals_ioptr1 = simals_rowptr2.inList;
				if (simals_parse_node()) return true;
				continue;
			}

			if (s1.equals("O"))
			{
				simals_ioptr1 = simals_rowptr2.outList;
				if (simals_parse_node()) return true;
				continue;
			}

			if (s1.equals("T"))
			{
				if (simals_parse_timing()) return true;
				continue;
			}

			if (s1.equals("D"))
			{
				if (simals_parse_delay()) return true;
				continue;
			}

			if (s1.equals("FANOUT"))
			{
				if (simals_parse_fanout()) return true;
				continue;
			}

			if (s1.equals("LOAD"))
			{
				if (simals_parse_load()) return true;
				continue;
			}

			if (s1.equals("PRIORITY"))
			{
				Integer jj = simals_get_int();
				if (jj == null)
				{
					System.out.println("Priority declaration: EOF unexpectedly found");
					return true;
				}
				simals_modptr2.priority = jj.intValue();
				continue;
			}

			if (s1.equals("SET"))
			{
				simals_ioptr1 = simals_modptr2.setList;
				if (simals_parse_node()) return true;
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
	private boolean simals_parse_node()
	{
		for(;;)
		{
			String s1 = simals_get_name();
			if (s1 == null || s1.equals("GATE") || s1.equals("FUNCTION") ||
				s1.equals("MODEL") || s1.equals("I") || s1.equals("O") ||
				s1.equals("T") || s1.equals("FANOUT") || s1.equals("LOAD") ||
				s1.equals("PRIORITY") || s1.equals("SET"))
			{
				--i_ptr;
				break;
			}

			simals_ioptr2 = new IO();
			simals_ioptr2.nodeptr = s1;
			simals_ioptr2.strength = Stimuli.GATE_STRENGTH;
			simals_ioptr1.add(simals_ioptr2);

			s1 = simals_get_string();
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
			simals_ioptr2.operatr = s1.charAt(0);

			s1 = simals_get_string();
			if (s1 == null)
			{
				System.out.println("Node declaration: EOF unexpectedly found");
				return true;
			}
			if (s1.equals("L") || s1.equals("X") || s1.equals("H"))
			{
				simals_ioptr2.operand = new Integer(simals_trans_state_to_number(s1));
			} else
			{
				--i_ptr;
				if (s1.charAt(0) == '+' || s1.charAt(0) == '-' || TextUtils.isDigit(s1.charAt(0)))
				{
					Integer jj = simals_get_int();
					if (jj == null)
					{
						System.out.println("Node declaration: EOF unexpectedly found");
						return true;
					}
					simals_ioptr2.operand = jj;
				} else
				{
					s1 = simals_get_name();
					if (s1 == null)
					{
						System.out.println("Node declaration: EOF unexpectedly found");
						return true;
					}
					simals_ioptr2.operand = s1;
					simals_ioptr2.operatr += 128;
				}
			}

			s1 = simals_get_string();
			if (s1 == null || !s1.startsWith("@"))
			{
				--i_ptr;
				continue;
			}
			Integer jj = simals_get_int();
			if (jj == null)
			{
				System.out.println("Node declaration: EOF unexpectedly found");
				return true;
			}
			simals_ioptr2.strength = jj.intValue() * 2;
		}
		return false;
	}

	/**
	 * Method to translate a state representation (L, H, X) that is
	 * stored in a char to an integer value.
	 * @param s1 pointer to character string that contains state value
	 */
	public int simals_trans_state_to_number(String s1)
	{
		switch (s1.charAt(0))
		{
			case 'L': case 'l': return Stimuli.LOGIC_LOW;
			case 'X': case 'x': return Stimuli.LOGIC_X;
			case 'H': case 'h': return Stimuli.LOGIC_HIGH;
			default:            return TextUtils.atoi(s1);
		}
	}

	/**
	 * Method to insert timing values into the appropriate places in
	 * the database.  Returns true on error.
	 */
	private boolean simals_parse_timing()
	{
		simals_delta_def = simals_linear_def = simals_exp_def = simals_random_def = simals_abs_def = 0;

		for(;;)
		{
			String s1 = simals_get_string();
			if (s1 == null)
			{
				System.out.println("Timing declaration: EOF unexpectedly found");
				return true;
			}

			String s2 = simals_get_string();
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

			Float value = simals_get_float();
			if (value == null)
			{
				System.out.println("Timing declaration: EOF unexpectedly found");
				return true;
			}

			switch (s1.charAt(0))
			{
				case 'A':
					simals_abs_def = value.floatValue();
					break;
				case 'D':
					simals_delta_def = value.floatValue();
					break;
				case 'E':
					simals_exp_def = value.floatValue();
					break;
				case 'L':
					simals_linear_def = value.floatValue();
					break;
				case 'R':
					simals_random_def = value.floatValue();
					if (value.floatValue() > 0.0) simals_modptr2.priority = 2;
					break;
				default:
					System.out.println("Invalid timing mode '" + s1 + "'");
					return true;
			}

			s1 = simals_get_string();
			if (s1 == null || !s1.startsWith("+"))
			{
				--i_ptr;
				break;
			}
		}
		return false;
	}

	/**
	 * Method to set the delay transition type for the current input state.
	 */
	private boolean simals_parse_delay()
	{
		String s1 = simals_get_string();
		if (s1 == null)
		{
			System.out.println("Timing declaration: EOF unexpectedly found");
			return true;
		}

		if (!s1.equals("01") && !s1.equals("10") && !s1.equals("OZ") && !s1.equals("Z1")
			&& !s1.equals("1Z") && !s1.equals("Z0") && !s1.equals("0X") && !s1.equals("X1")
			&& !s1.equals("1X") && !s1.equals("X0") && !s1.equals("XZ") && !s1.equals("ZX"))
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
	private boolean simals_parse_fanout()
	{
		String s1 = simals_get_string();
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

		s1 = simals_get_string();
		if (s1 == null)
		{
			System.out.println("Fanout declaration: EOF unexpectedly found");
			return true;
		}

		if (s1.equals("ON"))
		{
			simals_modptr2.fanout = 1;
			return false;
		}

		if (s1.equals("OFF"))
		{
			simals_modptr2.fanout = 0;
			return false;
		}

		System.out.println("Fanout declaration: Invalid option '" + s1 + "'");
		return true;
	}

	/**
	 * Method to enter the capacitive load rating (on per unit basis)
	 * into the database for the specified node.  Returns true on error.
	 */
	private boolean simals_parse_load()
	{
		for(;;)
		{
			String s1 = simals_get_name();
			if (s1 == null || s1.equals("GATE") || s1.equals("FUNCTION") || s1.equals("MODEL") ||
				s1.equals("I") || s1.equals("O") || s1.equals("T") || s1.equals("FANOUT") ||
				s1.equals("LOAD") || s1.equals("PRIORITY") || s1.equals("SET"))
			{
				--i_ptr;
				break;
			}

			String s2 = simals_get_string();
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

			Float load = simals_get_float();
			if (load == null)
			{
				System.out.println("Load declaration: EOF unexpectedly found");
				return true;
			}

			Load loadptr2 = new Load();
			loadptr2.ptr = s1;
			loadptr2.load = load.floatValue();
			loadptr2.next = simals_modptr2.loadptr;
			simals_modptr2.loadptr = loadptr2;
		}
		return false;
	}

	/**
	 * Method to parse the text used to describe a model entity.
	 * The user specifies the interconnection of lower level primitives (gates and
	 * functions) in this region of the netlist.  Returns true on error.
	 */
	private boolean simals_parse_model()
	{
		for(;;)
		{
			String s1 = simals_get_name();
			if (s1 == null || s1.equals("GATE") || s1.equals("FUNCTION") || s1.equals("MODEL"))
			{
				--i_ptr;
				break;
			}

			if (s1.charAt(0) == '}') continue;

			if (s1.equals("SET"))
			{
				simals_ioptr1 = simals_modptr2.setList;
				if (simals_parse_node()) return true;
				continue;
			}

			for(Object conptr1 = simals_modptr2.ptr; conptr1 != null; conptr1 = ((Connect)conptr1).next)
			{
				Connect cp1 = (Connect)conptr1;
				if (cp1.inst_name.equals(s1))
				{
					System.out.println("ERROR: Instance name '" + s1 + "' defined more than once");
					return true;
				}
			}
			Connect simals_conptr2 = new Connect();
			simals_conptr2.inst_name = s1;
			simals_conptr2.model_name = null;
			simals_conptr2.exptr = null;
			simals_conptr2.next = (Connect)simals_modptr2.ptr;
			simals_modptr2.ptr = simals_conptr2;

			s1 = simals_get_name();
			if (s1 == null)
			{
				System.out.println("Model declaration: EOF unexpectedly found");
				return true;
			}
			simals_conptr2.model_name = s1;

			s1 = simals_get_string();
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
				s1 = simals_get_name();
				if (s1 == null)
				{
					System.out.println("Model declaration: EOF unexpectedly found");
					return true;
				}
				if (s1.charAt(0) == ')') break;
				simals_exptr2 = new ALSExport();
				simals_exptr2.nodeptr = null;
				simals_exptr2.node_name = s1;
				simals_exptr2.next = simals_conptr2.exptr;
				simals_conptr2.exptr = simals_exptr2;
			}
		}
		return false;
	}

	/**
	 * Method to parse the text used to describe a function entity.
	 * The user specifies input entries, loading factors, and timing parameters
	 * in this region of the netlist.
	 */
	private boolean simals_parse_function()
	{
		simals_modptr2.fanout = 0;
		Func funchead = new Func();
		simals_modptr2.ptr = funchead;
		funchead.procptr = null;
		funchead.inptr = null;
		funchead.delta = 0;
		funchead.linear = 0;
		funchead.exp = 0;
		funchead.abs = 0;
		funchead.random = 0;
		funchead.userptr = null;
		funchead.userint = 0;
		funchead.userfloat = 0;

		for(;;)
		{
			String s1 = simals_get_string();
			if (s1 == null || s1.equals("GATE") || s1.equals("FUNCTION") || s1.equals("MODEL"))
			{
				--i_ptr;
				break;
			}

			if (s1.equals("I"))
			{
				if (simals_parse_func_input(funchead)) return true;
				continue;
			}

			if (s1.equals("O"))
			{
				if (simals_parse_func_output()) return true;
				continue;
			}

			if (s1.equals("T"))
			{
				if (simals_parse_timing()) return true;
				funchead.delta = simals_delta_def;
				funchead.linear = simals_linear_def;
				funchead.exp = simals_exp_def;
				funchead.abs = simals_abs_def;
				funchead.random = simals_random_def;
				continue;
			}

			if (s1.equals("LOAD"))
			{
				if (simals_parse_load()) return true;
				continue;
			}

			if (s1.equals("PRIORITY"))
			{
				Integer jj = simals_get_int();
				if (jj == null)
				{
					System.out.println("Priority declaration: EOF unexpectedly found");
					return true;
				}
				simals_modptr2.priority = jj.intValue();
				continue;
			}

			if (s1.equals("SET"))
			{
				simals_ioptr1 = simals_modptr2.setList;
				simals_parse_node();
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
	private boolean simals_parse_func_input(Func funcHead)
	{
		for(;;)
		{
			String s1 = simals_get_name();
			if (s1 == null || s1.equals("GATE") || s1.equals("FUNCTION") || s1.equals("MODEL") ||
				s1.equals("I") || s1.equals("O") || s1.equals("T") || s1.equals("FANOUT") ||
				s1.equals("LOAD") || s1.equals("PRIORITY") || s1.equals("SET"))
			{
				--i_ptr;
				break;
			}

			simals_exptr2 = new ALSExport();
			simals_exptr2.nodeptr = null;
			simals_exptr2.node_name = s1;
			simals_exptr2.next = funcHead.inptr;
			funcHead.inptr = simals_exptr2;
		}
		return false;
	}

	/**
	 * Method to create a list of output nodes for the function.
	 */
	private boolean simals_parse_func_output()
	{
		for(;;)
		{
			String s1 = simals_get_name();
			if (s1 == null || s1.equals("GATE") || s1.equals("FUNCTION") || s1.equals("MODEL") ||
				s1.equals("I") || s1.equals("O") || s1.equals("T") || s1.equals("FANOUT") ||
				s1.equals("LOAD") || s1.equals("PRIORITY") || s1.equals("SET"))
			{
				--i_ptr;
				break;
			}

			for (simals_exptr2 = simals_modptr2.exptr; ; simals_exptr2 = simals_exptr2.next)
			{
				if (simals_exptr2 == null)
				{
					System.out.println("ERROR: Unable to find node " + s1 + " in port list");
					return true;
				}
				if (s1.equals(simals_exptr2.node_name))
				{
//					simals_exptr2.nodeptr = (Node)1;
					break;
				}
			}
		}
		return false;
	}

	/**
	 * Method to processe the string specified by the calling argument
	 * and fragments it into a series of smaller character strings, each of which
	 * is terminated by a null character.  Returns true on error.
	 *
	 * Calling Arguments:
	 *	line = pointer to the character string to be fragmented
	 */
	private boolean simals_fragment_command(String line)
	{
		int j = 0, count = 0;
		simals_instptr[0] = simals_instbuf[0] = 0;
		int k = 1;

		for (int i = 0; ; ++i)
		{
			if (j > (simals_ibufsize - 3))
			{
				char [] newBuf = new char[simals_ibufsize * 5];
				for(int x=0; x<simals_ibufsize; x++) newBuf[x] = simals_instbuf[x];
				simals_instbuf = newBuf;
			}
			if (k > (simals_iptrsize - 2))
			{
				int [] newBuf = new int[simals_iptrsize * 5];
				for(int x=0; x<simals_iptrsize; x++) newBuf[x] = simals_instptr[x];
				simals_instptr = newBuf;
			}

			char chr = line.charAt(i);
			if (chr == 0 || chr == '\n')
			{
				if (count != 0)
				{
					simals_instbuf[j] = 0;
					simals_instptr[k] = -1;
				} else
				{
					simals_instptr[k-1] = -1;
				}
				break;
			}
			switch (chr)
			{
				case ' ':
				case ',':
				case '\t':
				case '=':
				case '@':
					if (count != 0)
					{
						simals_instbuf[j] = 0;
						simals_instptr[k] = j+1;
						++j;
						++k;
						count = 0;
					}
					break;

				default:
					simals_instbuf[j] = chr;
					++j;
					++count;
			}
		}
		return false;
	}

	/**
	 * Method to return a pointer to a structure in the cross reference
	 * table. The calling argument string contains information detailing the path
	 * name to the desired level in the cross reference table.
	 *
	 * Calling Argument:
	 *	sp = pointer to char string containing path name to level in xref table
	 */
	Connect simals_find_level(String sp)
	{
		Connect cellptr = simals_cellroot;
		String [] levels = sp.split(".");

		while (sp.length() > 0)
		{
			String part = sp;
			sp = "";
			int dotPos = sp.indexOf('.');
			if (dotPos >= 0)
			{
				sp = part.substring(dotPos+1);
				part = part.substring(0, dotPos);
			}
			for( ; ; cellptr = cellptr.next)
			{
				if (cellptr == null) return null;
				if (part.equals(cellptr.inst_name))
				{
					if (sp.length() > 0)
						cellptr = cellptr.child;
					break;
				}
			}
		}
		return cellptr;
	}

	/**
	 * Method to translate an integer value that represents a state
	 * and returns a single character corresponding to the state.
	 * @param stateNum integer value that is to be converted to a character
	 */
	String simals_trans_number_to_state(int stateNum)
	{
		switch (stateNum)
		{
			case Stimuli.LOGIC_LOW:  return "L";
			case Stimuli.LOGIC_X:    return "X";
			case Stimuli.LOGIC_HIGH: return "H";
		}
		return "0x" + Integer.toHexString(stateNum);
	}

	/**
	 * Method to compose a character string which indicates the node name
	 * for the nodeptr specified in the calling argument.
	 *
	 * Calling Arguments:
	 *	nodehead = pointer to desired node in database
	 *	sp	= pointer to char string where complete name is to be saved
	 */
	String simals_compute_node_name(Node nodehead)
	{
		Connect cellhead = nodehead.cellptr;
		String sp = simals_compute_path_name(cellhead);

		for (ALSExport exhead = cellhead.exptr; ; exhead = exhead.next)
		{
			if (nodehead == exhead.nodeptr)
			{
				sp += "." + exhead.node_name;
				return sp;
			}
		}
	}

	/**
	 * Method to compose a character string which indicates the path name
	 * to the level of hierarchy specified in the calling argument.
	 *
	 * Calling Arguments:
	 *	cellhead = pointer to desired level of hierarchy
	 *	sp	= pointer to char string where path name is to be saved
	 */
	String simals_compute_path_name(Connect cellhead)
	{
		StringBuffer infstr = new StringBuffer();
		for ( ; cellhead != null; cellhead = cellhead.parent)
			infstr.append("." + cellhead.inst_name);
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
	Node simals_find_node(String sp)
	{
		if (sp.startsWith("$N"))
		{
			int i = TextUtils.atoi(sp.substring(2));
			for (Node nodehead = simals_noderoot; nodehead != null; nodehead = nodehead.next)
			{
				if (nodehead.num == i) return nodehead;
			}
			return null;
		}

		int dotPos = sp.lastIndexOf('.');
		String s2 = sp;
		Connect cellptr = simals_find_level(simals_mainproto.getName().toUpperCase());
		if (cellptr == null) cellptr = simals_cellroot;
		if (dotPos >= 0)
		{
			s2 = sp.substring(dotPos+1);
			cellptr = simals_find_level(sp.substring(0, dotPos));
		}

		for (ALSExport exhead = cellptr.exptr; exhead != null; exhead = exhead.next)
		{
			if (exhead.node_name.equals(s2)) return exhead.nodeptr;
		}

		return null;
	}

//	public static void doForJohn()
//	{
//		String addressFileName = OpenFile.chooseInputFile(FileType.TEXT, "address file");
//		if (addressFileName == null) return;
//		URL url = TextUtils.makeURLToFile(addressFileName);
//		List results = new ArrayList();
//		try
//		{
//			URLConnection urlCon = url.openConnection();
//			InputStreamReader is = new InputStreamReader(urlCon.getInputStream());
//			LineNumberReader lineReader = new LineNumberReader(is);
//			String buf = lineReader.readLine();
//			if (buf == null) return;
//			String [] titles = buf.split("\t");
//			for(int i=0; i<titles.length; i++)
//				System.out.println("Title "+i+" is "+titles[i]);
//			StringBuffer sb = new StringBuffer();
//			sb.append("\"First Name\", ");
//			sb.append("\"Last Name\", ");
//			sb.append("\"Prefix\", ");
//			sb.append("\"Suffix\", ");
//			sb.append("\"EMail\", ");
//			sb.append("\"Home Street 1\", ");
//			sb.append("\"Home Street 2\", ");
//			sb.append("\"Home City\", ");
//			sb.append("\"Home Zip\", ");
//			sb.append("\"Home State\", ");
//			sb.append("\"Home Country\", ");
//			sb.append("\"Home Phone\", ");
//			sb.append("\"Home Fax\", ");
//			sb.append("\"Cell Phone\", ");
//			sb.append("\"Website\", ");
//			sb.append("\"Work Street 1\", ");
//			sb.append("\"Work Street 2\", ");
//			sb.append("\"Work City\", ");
//			sb.append("\"Work Zip\", ");
//			sb.append("\"Work State\", ");
//			sb.append("\"Work Country\", ");
//			sb.append("\"Work Phone\", ");
//			sb.append("\"Pager\", ");
//			sb.append("\"Company\", ");
//			sb.append("\"Job Title\", ");
//			sb.append("\"Notes\"");
//			results.add(sb.toString());
//			for(;;)
//			{
//				buf = lineReader.readLine();
//				if (buf == null) break;
//				String [] fields = buf.split("\t");
//				if (fields.length < titles.length)
//				{
//					String [] newfields = new String[titles.length];
//					for(int i=0; i<fields.length; i++) newfields[i] = fields[i];
//					for(int i=fields.length; i<titles.length; i++) newfields[i] = "";
//					fields = newfields;
//				}
//				String firstName = fields[0];
//				String lastName = fields[1];
//				String prefix = fields[3];
//				String suffix = fields[4];
//				String title = fields[5];
//				String company = fields[6];
//				String department = fields[7];
//				String homeStreet2 = fields[9];
//				String homeStreet1 = fields[10];
//				String homeCity = fields[11];
//				String homeState = fields[12];
//				String homeZip = fields[13];
//				String homeCountry = fields[14];
//				String workStreet2 = fields[16];
//				String workStreet1 = fields[17];
//				String workCity = fields[18];
//				String workState = fields[19];
//				String workZip = fields[20];
//				String workCountry = fields[21];
//				String notes = fields[34];
//
//				// add "nickname", "birthday" and "division" to notes
//				String nickname = fields[2];
//				if (nickname.length() > 0)
//				{
//					if (notes.length() > 0) notes += ", ";
//					notes += "nickname: " + nickname;
//				}
//				String birthday = fields[35];
//				if (birthday.length() > 0)
//				{
//					if (notes.length() > 0) notes += ", ";
//					notes += "birthday: "+birthday;
//				}
//				String division = fields[7];
//				if (division.length() > 0)
//				{
//					if (notes.length() > 0) notes += ", ";
//					notes += "division: " + division;
//				}
//
//				// pick up phone numbers
//				String homePhone = "", workPhone = "", fax = "", cellPhone = "", pager = "";
//				for(int i=22; i<33; i += 3)
//				{
//					String type = fields[i];
//					String value = fields[i+1];
//					if (type.equals("Home")) homePhone = value; else
//						if (type.equals("Work")) workPhone = value; else
//							if (type.equals("Cellular")) cellPhone = value; else
//								if (type.equals("Pager")) pager = value; else
//									if (type.equals("Business")) workPhone = value; else
//										if (type.equals("Fax")) fax = value; else
//											if (type.equals("School")) workPhone = value; else
//											{
//												if (notes.length() > 0) notes += ", ";
//												if (type.length() == 0) type = "phone";
//												notes += type + ": " + value;
//											}
//				}
//				if (fields[49].length() > 0)
//				{
//					if (notes.length() > 0) notes += ", ";
//					notes += fields[49];
//				}
//				if (fields[50].length() > 0)
//				{
//					if (notes.length() > 0) notes += ", ";
//					notes += fields[50];
//				}
//				String eMail = fields[36];
//				if (fields[38].length() > 0)
//				{
//					if (eMail.length() > 0) eMail += ", ";
//					eMail += fields[38];
//				}
//				if (fields[39].length() > 0)
//				{
//					if (eMail.length() > 0) eMail += ", ";
//					eMail += fields[39];
//				}
//				String webPage = fields[37];
//				sb = new StringBuffer();
//				sb.append("\"" + firstName + "\", ");
//				sb.append("\"" + lastName + "\", ");
//				sb.append("\"" + prefix + "\", ");
//				sb.append("\"" + suffix + "\", ");
//				sb.append("\"" + eMail + "\", ");
//				sb.append("\"" + homeStreet1 + "\", ");
//				sb.append("\"" + homeStreet2 + "\", ");
//				sb.append("\"" + homeCity + "\", ");
//				sb.append("\"" + homeZip + "\", ");
//				sb.append("\"" + homeState + "\", ");
//				sb.append("\"" + homeCountry + "\", ");
//				sb.append("\"" + homePhone + "\", ");
//				sb.append("\"" + fax + "\", ");
//				sb.append("\"" + cellPhone + "\", ");
//				sb.append("\"" + webPage + "\", ");
//				sb.append("\"" + workStreet1 + "\", ");
//				sb.append("\"" + workStreet2 + "\", ");
//				sb.append("\"" + workCity + "\", ");
//				sb.append("\"" + workZip + "\", ");
//				sb.append("\"" + workState + "\", ");
//				sb.append("\"" + workCountry + "\", ");
//				sb.append("\"" + workPhone + "\", ");
//				sb.append("\"" + pager + "\", ");
//				sb.append("\"" + company + "\", ");
//				sb.append("\"" + title + "\", ");
//				sb.append("\"" + notes + "\"");
//				results.add(sb.toString());
//			}
//			lineReader.close();
//		} catch (IOException e)
//		{
//			System.out.println("Error reading " + addressFileName);
//			return;
//		}
//
//		try
//		{
//			PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter("C:\\temp\\result.txt")));
//
//			for(Iterator it = results.iterator(); it.hasNext(); )
//			{
//				String buf = (String)it.next();
//				printWriter.println(buf);
//			}
//
//			printWriter.close();
//		} catch (IOException e)
//		{
//			System.out.println("Error writing results");
//			return;
//		}
//	}
}
