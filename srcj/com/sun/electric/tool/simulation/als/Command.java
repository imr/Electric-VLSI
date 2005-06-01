/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Command.java
 * Asynchronous Logic Simulator command handler
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

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.menus.MenuCommands;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WaveformWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;

public class Command
{
	private ALS als;

	static String [] simals_tnames = {"01", "10", "0Z", "Z1", "1Z", "Z0", "0X", "X1", "1X", "X0", "XZ", "ZX"};

	Command(ALS als)
	{
		this.als = als;
	}


//	void simals_com_comp(INTBIG count, CHAR *par[10])
//	{
//		CHAR *pp;
//		INTBIG l;
//	
//		l = estrlen(pp = par[0]);
//	
//		if (namesamen(pp, x_("clock"), l) == 0 && l >= 1)
//		{
//			simals_clock_command(count-1, &par[1]);
//			return;
//		}
//		if (namesamen(pp, x_("erase"), l) == 0 && l >= 1)
//		{
//			simals_erase_model();
//			return;
//		}
//		if (namesamen(pp, x_("go"), l) == 0 && l >= 1)
//		{
//			simals_go_command(count-1, &par[1]);
//			return;
//		}
//		if (namesamen(pp, x_("help"), l) == 0 && l >= 1)
//		{
//			simals_help_command();
//			return;
//		}
//		if (namesamen(pp, x_("print"), l) == 0 && l >= 1)
//		{
//			simals_print_command(count-1, &par[1]);
//			return;
//		}
//		if (namesamen(pp, x_("seed"), l) == 0 && l >= 3)
//		{
//			simals_seed_command(count-1, &par[1]);
//			return;
//		}
//		if (namesamen(pp, x_("set"), l) == 0 && l >= 3)
//		{
//			simals_set_command(count-1, &par[1]);
//			return;
//		}
//		if (namesamen(pp, x_("vector"), l) == 0 && l >= 1)
//		{
//			simals_vector_command(count-1, &par[1]);
//			return;
//		}
//		if (namesamen(pp, x_("annotate"), l) == 0 && l >= 1)
//		{
//			simals_annotate_command(count-1, &par[1]);
//			return;
//		}
//		if (namesamen(pp, x_("order"), l) == 0 && l >= 1)
//		{
//			simals_order_command(count-1, &par[1]);
//			return;
//		}
//		ttyputbadusage(x_("telltool simulation als"));
//	}

	/****************************** CLOCK ******************************/
	
	/**
	 * Method to enter a complex clock vector into the user defined
	 * event list.  The user is polled for the node name and timing parameters
	 * before any entry is made into the linklist.
	 */
	void simals_clock_command(String [] par)
	{
		WaveformWindow ww = WaveformWindow.findWaveformWindow(als.simals_mainproto);
		if (ww == null)
		{
			System.out.println("No simulator active");
			return;
		}
		if (par.length < 1)
		{
			System.out.println("telltool simulation als clock NODENAME (freq | period | custom)");
			return;
		}

		par[0] = par[0].toUpperCase();
		ALS.Node nodehead = als.simals_find_node(par[0]);
		if (nodehead == null)
		{
			System.out.println("ERROR: Unable to find node " + par[0]);
			return;
		}
	
		if (par.length < 2)
		{
//			count = sim_alsclockdlog(&par[1]) + 1;
			if (par.length < 2) return;
		}
	
		// see if there are frequency/period parameters
		if (par[1].startsWith("frequency") || par[1].startsWith("period"))
		{
			if (par.length < 3)
			{
				System.out.println("telltool simulation als clock NODENAME frequency/period PERIOD");
				return;
			}
			double time = TextUtils.atof(par[2]);
			if (time <= 0.0)
			{
				System.out.println("Clock timing parameter must be greater than 0");
				return;
			}
	
			if (par[1].startsWith("frequency")) time = 1.0f / time;
	
			ALS.Link vectptr2 = new ALS.Link();
			vectptr2.type = 'N';
			vectptr2.ptr = nodehead;
			vectptr2.state = Stimuli.LOGIC_HIGH;
			vectptr2.strength = Stimuli.VDD_STRENGTH;
			vectptr2.priority = 1;
			vectptr2.time = 0.0;
			vectptr2.right = null;
	
			ALS.Link vectptr1 = new ALS.Link();
			vectptr1.type = 'N';
			vectptr1.ptr = nodehead;
			vectptr1.state = Stimuli.LOGIC_LOW;
			vectptr1.strength = Stimuli.VDD_STRENGTH;
			vectptr1.priority = 1;
			vectptr1.time = time / 2.0f;
			vectptr1.right = vectptr2;
	
			ALS.Row clokhead = new ALS.Row();
//			clokhead.inptr = (ALS.IO)vectptr1;
			clokhead.outList = new ArrayList();
			clokhead.delta = (float)time;
			clokhead.linear = 0;
			clokhead.exp = 0;
			clokhead.abs = 0;
			clokhead.random = 0;
			clokhead.next = null;
			clokhead.delay = null;
	
			ALS.Link sethead = new ALS.Link();
			sethead.type = 'C';
			sethead.ptr = clokhead;
			sethead.state = 0;
			sethead.priority = 1;
			sethead.time = 0.0;
			sethead.right = null;
			als.getSim().simals_insert_set_list(sethead);
	
			als.getSim().simals_initialize_simulator(false);
			return;
		}
	
		if (par[1].startsWith("custom"))
		{
			System.out.println("telltool simulation als clock");
			return;
		}
	
		// handle custom clock specification
		if (par.length < 7)
		{
			System.out.println("telltool simulation als clock custom RAN STR CY (L D) *");
			return;
		}
	
		double linear = TextUtils.atof(par[2]);
		int strength = TextUtils.atoi(par[3])*2;
		int num = TextUtils.atoi(par[4]);
	
		double totaltime = 0.0;
		ALS.Link vectroot = null;
		ALS.Link vectptr2 = null;
		for(int i=5; i<par.length; i += 2)
		{
			vectptr2 = new ALS.Link();
			vectptr2.type = 'N';
			vectptr2.ptr = nodehead;
			vectptr2.state = als.simals_trans_state_to_number(par[i]);
			vectptr2.strength = strength;
			vectptr2.priority = 1;
			vectptr2.time = TextUtils.atof(par[i+1]);
			totaltime += vectptr2.time;
			vectptr2.right = vectroot;
			vectroot = vectptr2;
		}
		vectptr2.time = 0;
	
		ALS.Row clokhead = new ALS.Row();
		clokhead.inList = new ArrayList();
//		clokhead.inptr = (ALS.IO) vectroot;
		clokhead.outList = new ArrayList();
		clokhead.delta = (float)totaltime;
		clokhead.linear = (float)linear;
		clokhead.exp = 0;
		clokhead.abs = 0;
		clokhead.random = 0;
		clokhead.next = null;
		clokhead.delay = null;
	
		ALS.Link sethead = new ALS.Link();
		sethead.type = 'C';
		sethead.ptr = clokhead;
		sethead.state = num;
		sethead.priority = 1;
		sethead.time = 0;
		sethead.right = null;
		als.getSim().simals_insert_set_list(sethead);
	
		als.getSim().simals_initialize_simulator(false);
	}
	
	/****************************** ERASE ******************************/
	
	void simals_erase_model()
	{
		// reset miscellaneous simulation variables
		als.simals_linkfront = null;
		als.simals_linkback = null;
	
		// delete all test vectors
		simals_clearallvectors(true);
	
		// delete all cells in flattened network
		als.simals_cellroot = null;
		als.simals_levelptr = null;
	
		// delete all nodes in flattened network
		als.simals_noderoot = null;
	
		// delete all primitives in flattened network
		als.simals_primroot = null;
	
		// delete each model/gate/function in hierarchical description
		als.simals_modroot = null;
	}
	
	/*
	 * Routine to clear all test vectors (even the power and ground vectors if "pwrgnd"
	 * is true).
	 */
	void simals_clearallvectors(boolean pwrgnd)
	{
		ALS.Link lastset = null;
		ALS.Link nextset = null;
		for(ALS.Link thisset = als.simals_setroot; thisset != null; thisset = nextset)
		{
			nextset = thisset.right;
			if (pwrgnd || thisset.strength != Stimuli.VDD_STRENGTH)
			{
				if (lastset == null) als.simals_setroot = nextset; else
					lastset.right= nextset;
			} else
			{
				lastset = thisset;
			}
		}
	}
	
	/****************************** GO ******************************/
	
	/**
	 * Method to parse the command line for the go command from the
	 * keyboard.  The maximum execution time must also be specified for the
	 * simulation run.
	 */
	void simals_go_command(String [] par)
	{
		WaveformWindow ww = WaveformWindow.findWaveformWindow(als.simals_mainproto);
		if (ww == null)
		{
			System.out.println("No simulator active");
			return;
		}
		if (par.length < 1)
		{
			System.out.println("Must specify simulation time");
			return;
		}
		double max = TextUtils.atof(par[0]);
		if (max <= 0)
		{
			System.out.println("Simulation time must be greater than 0 seconds");
			return;
		}
		for(Iterator it = ww.getPanels(); it.hasNext(); )
		{
			WaveformWindow.Panel wp = (WaveformWindow.Panel)it.next();
			wp.setTimeRange(0.0, max);
		}
	
		als.getSim().simals_initialize_simulator(true);
	}

	/****************************** LEVEL ******************************/
	
	/**
	 * Method to change the level of hierarchy up one level.
	 */
	void simals_level_up_command()
	{
		WaveformWindow ww = WaveformWindow.findWaveformWindow(als.simals_mainproto);
		if (ww == null)
		{
			System.out.println("No simulator active");
			return;
		}
	
		if (als.simals_levelptr == null)
		{
			System.out.println("No simulation is running");
			return;
		}
		ALS.Connect cellptr = als.simals_levelptr.parent;
		if (cellptr == null)
		{
			System.out.println("ERROR: Currently at top level of hierarchy");
			return;
		}
	
		als.simals_levelptr = cellptr;
	
		// determine the level title
		if (als.simals_title != null)
		{
			int lastDotPos = als.simals_title.lastIndexOf('.');
			if (lastDotPos >= 0) als.simals_title = als.simals_title.substring(0, lastDotPos); else
				als.simals_title = "";
		}
	
		// reinitialize simulator while preserving time information
		double maintime = ww.getMainTimeCursor();
		double exttime = ww.getExtensionTimeCursor();
//		if (Graph.simals_set_current_level()) return;
		ww.setMainTimeCursor(maintime);
		ww.setExtensionTimeCursor(exttime);
	
		int l = als.simals_levelptr.num_chn;
//		sim_window_setnumframes(l);
//		sim_window_settopvisframe(0);
		als.getSim().simals_initialize_simulator(true);
	}
	
	/**
	 * Method to change the level of hierarchy to a specified new (lower) level.
	 */
	void simals_level_set_command(String instname)
	{
		WaveformWindow ww = WaveformWindow.findWaveformWindow(als.simals_mainproto);
		if (ww == null)
		{
			System.out.println("No simulator active");
			return;
		}
	
		if (instname != null)
		{
			int spacePos = instname.indexOf(' ');
			if (spacePos >= 0) instname = instname.substring(0, spacePos);
			instname = instname.toUpperCase();
			ALS.Connect cellptr = als.simals_find_level(instname);
			if (cellptr == null)
			{
				System.out.println("ERROR: Unable to find level " + instname);
				return;
			}
	
			als.simals_levelptr = cellptr;
	
			// determine the level title
			StringBuffer infstr = new StringBuffer();
			if (als.simals_title != null) infstr.append(als.simals_title + ".");
			infstr.append(als.simals_levelptr.inst_name);
			als.simals_title = infstr.toString();
		}
	
		// reinitialize simulator while preserving time information
		double maintime = ww.getMainTimeCursor();
		double exttime = ww.getExtensionTimeCursor();
//		if (Graph.simals_set_current_level()) return;
		ww.setMainTimeCursor(maintime);
		ww.setExtensionTimeCursor(exttime);
	
		int l = als.simals_levelptr.num_chn;
//		sim_window_setnumframes(l);
//		sim_window_settopvisframe(0);
		als.getSim().simals_initialize_simulator(true);
	}
	
	/****************************** PRINT ******************************/
	
	/**
	 * Method to print out the display screen status and information
	 */
	void simals_print_command(String [] par)
	{	
		if (par.length < 1)
		{
			System.out.println("telltool simulation als print OPTION");
			return;
		}
	
		if (par[0].equals("size"))
		{
			System.out.println("Number of Primitive Elements in Database = " + als.simals_pseq);
			System.out.println("Number of Nodes in Database = " + als.simals_nseq);
			return;
		}
	
		if (par[0].equals("vector"))
		{
			ALS.Link linkhead = als.simals_setroot;
			System.out.println("** VECTOR LINKLIST **");
			while (linkhead != null)
			{
				switch (linkhead.type)
				{
					case 'N':
						ALS.Node nodehead = (ALS.Node)linkhead.ptr;
						String s1 = als.simals_trans_number_to_state(((Integer)linkhead.state).intValue());
						System.out.println("***** vector: $N" + nodehead.num + ", state = " + s1 +
								", strength = " + simals_strengthstring(linkhead.strength) + ", time = " + linkhead.time +
								", priority = " + linkhead.priority);
						break;
					case 'F':
						ALS.Stat stathead = (ALS.Stat)linkhead.ptr;
						nodehead = stathead.nodeptr;
						s1 = als.simals_trans_number_to_state(((Integer)linkhead.state).intValue());
						System.out.println("***** function: $N" + nodehead.num + ", state = " + s1 +
							", strength = " + simals_strengthstring(linkhead.strength) + ", time = " + linkhead.time +
							", priority = " + linkhead.priority);
						break;
					case 'R':
						System.out.println("***** rowptr = " + linkhead.ptr + ", time = " + linkhead.time +
							", priority = " + linkhead.priority);
						break;
					case 'C':
						System.out.println("***** clokptr = " + linkhead.ptr + ", time = " + linkhead.time +
							", priority = " + linkhead.priority);
				}
				linkhead = linkhead.right;
			}
			return;
		}
	
		if (par[0].equals("netlist"))
		{
			System.out.println("** NETWORK DESCRIPTION **");
			for (ALS.Model primhead = als.simals_primroot; primhead != null; primhead = primhead.next)
			{
				switch (primhead.type)
				{
					case 'F':
						StringBuffer infstr = new StringBuffer();
						infstr.append("FUNCTION " + primhead.num + ": " + primhead.name + " (instance " +
							(primhead.level == null ? "null" : primhead.level) + ") [");
						for (ALS.ALSExport exhead = primhead.exptr; exhead != null; exhead=exhead.next)
						{
							if (exhead != primhead.exptr) infstr.append(", ");
							infstr.append("N" + exhead.nodeptr.num);
						}
						infstr.append("]");
						System.out.println(infstr.toString());
						infstr = new StringBuffer();
						infstr.append("  Event Driving Inputs:");
						ALS.Func funchead = (ALS.Func)primhead.ptr;
						for (ALS.ALSExport exhead = funchead.inptr; exhead != null; exhead=exhead.next)
							infstr.append(" N" + exhead.nodeptr.num);
						System.out.println(infstr.toString());
						infstr = new StringBuffer();
						infstr.append("  Output Ports:");
						for (ALS.ALSExport exhead = primhead.exptr; exhead != null; exhead=exhead.next)
						{
							if (exhead.node_name != null)
								infstr.append(" N" + ((ALS.Stat)exhead.node_name).nodeptr.num);
						}
						System.out.println(infstr.toString());
						System.out.println("  Timing: D=" + funchead.delta + ", L=" + funchead.linear + ", E=" + funchead.exp +
							", R=" + funchead.random + ", A=" + funchead.abs);
						System.out.println("  Firing Priority = " + primhead.priority);
						break;
					case 'G':
						System.out.println("GATE " + primhead.num + ": " + primhead.name + " (instance " +
							(primhead.level == null ? "null" : primhead.level) + ")");
						for (ALS.Row rowhead = (ALS.Row)primhead.ptr; rowhead != null; rowhead=rowhead.next)
						{
							System.out.println("  Timing: D=" + rowhead.delta + ", L=" + rowhead.linear + ", E=" + rowhead.exp +
								", R=" + rowhead.random + ", A=" + rowhead.abs);
							System.out.println("  Delay type: " + (rowhead.delay == null ? "null" : rowhead.delay));
							simals_print_in_entry(rowhead);
							simals_print_out_entry(rowhead);
						}
						System.out.println("  Firing Priority = " + primhead.priority);
						break;
					default:
						System.out.println("Illegal primitive type '" + primhead.type + "', database is bad");
						break;
				}
			}
			return;
		}
	
		if (par[0].equals("xref"))
		{
			System.out.println("** CROSS REFERENCE TABLE **");
			simals_print_xref_entry(als.simals_levelptr, 0);
			return;
		}
	
		if (par[0].equals("state"))
		{
			if (par.length < 2)
			{
				System.out.println("telltool simulation als print state NODENAME");
				return;
			}
			par[1] = par[1].toUpperCase();
			ALS.Node nodehead = als.simals_find_node(par[1]);
			if (nodehead == null)
			{
				System.out.println("ERROR: Unable to find node " + par[1]);
				return;
			}
	
			String s1 = als.simals_trans_number_to_state(((Integer)nodehead.new_state).intValue());
			System.out.println("Node " + par[1] + ": State = " + s1 + ", Strength = " + simals_strengthstring(nodehead.new_strength));
			ALS.Stat stathead = nodehead.statptr;
			while (stathead != null)
			{
				s1 = als.simals_trans_number_to_state(stathead.new_state);
				System.out.println("Primitive " + stathead.primptr.num + ":    State = " + s1 +
					", Strength = " + simals_strengthstring(stathead.new_strength));
				stathead = stathead.next;
			}
			return;
		}
	
		if (par[0].equals("instances"))
		{
			System.out.println("Instances at level: " + als.simals_compute_path_name(als.simals_levelptr));
			for (ALS.Connect cellhead = als.simals_levelptr.child; cellhead != null; cellhead = cellhead.next)
			{
				System.out.println("Name: " + cellhead.inst_name + ", Model: " + cellhead.model_name);
			}
			return;
		}
		System.out.println("telltool simulation als print");
	}
	
	static String simals_strengthstring(int strength)
	{
		if (strength == Stimuli.OFF_STRENGTH) return "off";
		if (strength <= Stimuli.NODE_STRENGTH) return "node";
		if (strength <= Stimuli.GATE_STRENGTH) return "gate";
		return "power";
	}
	
	/**
	 * Method to examine an input entry and prints out the condition
	 * that it represents.  It is possible for an input entry operand to represent
	 * a logic value, integer value, or another node address.
	 *
	 * Calling Arguments:
	 *	rowhead = pointer to the row being printed
	 */
	private void simals_print_in_entry(ALS.Row rowhead)
	{
		boolean flag = false;
		StringBuffer infstr = new StringBuffer();
		infstr.append("  Input: ");

		for(Iterator it = rowhead.inList.iterator(); it.hasNext(); )
		{
			ALS.IO iohead = (ALS.IO)it.next();
			if (flag) infstr.append("& ");
			flag = true;
	
			ALS.Node nodehead = (ALS.Node)iohead.nodeptr;
			infstr.append("N" + nodehead.num);
	
			if (iohead.operatr > 127)
			{
				int operatr = iohead.operatr - 128;
				nodehead = (ALS.Node)iohead.operand;
				infstr.append(operatr + "N" + nodehead.num);
				continue;
			}
	
			infstr.append(iohead.operatr);
	
			Integer num = (Integer)iohead.operand;
			String s2 = als.simals_trans_number_to_state(num.intValue());
			infstr.append(s2 + " ");
		}
		System.out.println(infstr.toString());
	}
	
	/**
	 * Method to examine an output entry and prints out the condition
	 * that it represents.  It is possible for an output entry operand to represent
	 * a logic value, integer value, or another node address.
	 *
	 * Calling Arguments:
	 *	rowhead = pointer to the row being printed
	 */
	private void simals_print_out_entry(ALS.Row rowhead)
	{
		boolean flag = false;
		StringBuffer infstr = new StringBuffer();
		infstr.append("  Output: ");

		for(Iterator it = rowhead.outList.iterator(); it.hasNext(); )
		{
			ALS.IO iohead = (ALS.IO)it.next();
			if (flag) infstr.append("& ");
			flag = true;
	
			ALS.Stat stathead = (ALS.Stat)iohead.nodeptr;
			ALS.Node nodehead = stathead.nodeptr;
			infstr.append("N" + nodehead.num);
	
			if (iohead.operatr > 127)
			{
				int operatr = iohead.operatr - 128;
				nodehead = (ALS.Node) iohead.operand;
				infstr.append(operatr + "N" + nodehead.num + "@" + ((iohead.strength+1)/2) + " ");
				continue;
			}
	
			infstr.append(iohead.operatr);
	
			Integer num = (Integer)iohead.operand;
			String s2 = als.simals_trans_number_to_state(num.intValue());
			infstr.append(s2 + "@" + ((iohead.strength+1)/2) + " ");
		}
		System.out.println(infstr.toString());
	}
	
	/**
	 * Method to print entries from the cross reference table that was
	 * generated to transform the hierarchical network description into a totally flat
	 * network description.  The calling arguments define the root of the reference
	 * table column and the level of indentation for the column.
	 *
	 * Calling Arguments:
	 *	cellhead = pointer to cross reference table
	 *	tab	= integer value indicating level of output indentation
	 */
	private void simals_print_xref_entry(ALS.Connect cellhead, int tab)
	{
		StringBuffer tabsp = new StringBuffer();
		for (int i = 0; i < tab; ++i) tabsp.append(' ');
		System.out.println(tabsp.toString() + "Level: " + als.simals_compute_path_name(cellhead) +
			", Model: " + cellhead.model_name);
	
		for (ALS.ALSExport exhead = cellhead.exptr; exhead != null; exhead = exhead.next)
		{
			StringBuffer infstr = null;
			for (int i=0; i<12; i++)
			{
				int delay = exhead.td[i];
				if (delay != 0)
				{
					if (infstr == null) infstr = new StringBuffer();
					infstr.append(simals_tnames[i] + "=" + delay + " ");
				}
			}
			if (infstr == null) System.out.println(tabsp.toString() + exhead.node_name + " -. N" + exhead.nodeptr.num); else
				System.out.println(tabsp.toString() + exhead.node_name + " -. N" + exhead.nodeptr.num + " (" + infstr.toString() + ")");
		}
	
		if (als.simals_instbuf[als.simals_instptr[1]] == 'X') return;
	
		for (ALS.Connect subcell = cellhead.child; subcell != null; subcell = subcell.next)
			simals_print_xref_entry(subcell, tab + 10);
	}
	
	/****************************** SEED ******************************/
	
	/**
	 * Method to set a flag which tells the simulator if it is necessary
	 * to reseed the Random Number Generator each time a simulation is run.
	 */
	void simals_seed_command(String [] par)
	{
		if (par.length < 1)
		{
			System.out.println("telltool simulation als seed (reset | no-reset)");
			return;
		}
		if (par[0].equals("reset"))
			als.simals_seed_flag = false; else als.simals_seed_flag = true;
	}
	
	/****************************** SET ******************************/
	
	/**
	 * Method to set the specified node to the state that is indicated
	 * in the command line.
	*/
	void simals_set_command(String [] par)
	{
		WaveformWindow ww = WaveformWindow.findWaveformWindow(als.simals_mainproto);
		if (ww == null)
		{
			System.out.println("No simulator active");
			return;
		}
		if (par.length < 4)
		{
			System.out.println("telltool simulation als set NODE LEVEL STRENGTH TIME");
			return;
		}
		par[0] = par[0].toUpperCase();
		ALS.Node nodehead = als.simals_find_node(par[0]);
		if (nodehead == null)
		{
			System.out.println("ERROR: Unable to find node " + par[0]);
			return;
		}
	
		int state = als.simals_trans_state_to_number(par[1]);
		int strength = TextUtils.atoi(par[2])*2;
		double time = TextUtils.atof(par[3]);
	
		ALS.Link sethead = new ALS.Link();
		sethead.type = 'N';
		sethead.ptr = nodehead;
		sethead.state = state;
		sethead.strength = strength;
		sethead.priority = 2;
		sethead.time = time;
		sethead.right = null;
		als.getSim().simals_insert_set_list(sethead);
	
		System.out.println("Node '" + par[0] + "' scheduled, state = " + par[1] +
			", strength = " + simals_strengthstring(strength) + ", time = " + time);
		als.getSim().simals_initialize_simulator(false);
	}

	/****************************** VECTOR ******************************/
	
	void simals_vector_command(String [] par)
	{
//		INTBIG	 l, flag;
//		INTSML   strength;
//		CHAR     s1[256], *s2, *pt, **vectptr1, **backptr;
//		CHAR    *filename, *truename;
//		FILE    *vin, *vout;
//		LINKPTR  sethead, vecthead, vectptr2, nextvec;
//		ROWPTR   clokhead;
//		NODEPTR  nodehead;
//		double   time;
//		NODEPROTO *np;
//		REGISTER void *infstr;

		WaveformWindow ww = WaveformWindow.findWaveformWindow(als.simals_mainproto);
		if (ww == null)
		{
			System.out.println("No simulator active");
			return;
		}
		if (par.length < 1)
		{
			System.out.println("telltool simulation als vector OPTION");
			return;
		}
	
//		if (par[0].equals("load"))
//		{
//			if (count < 2)
//			{
//				par[1] = fileselect(_("ALS vector file"), sim_filetypealsvec, x_(""));
//				if (par[1] == 0) return;
//			}
//			vin = xopen(par[1], sim_filetypealsvec, x_(""), &filename);
//			if (! vin)
//			{
//				System.out.println("ERROR: Can't open %s"), par[1]);
//				return;
//			}
//	
//			// clear all vectors
//			while (simals_setroot)
//			{
//				simals_setroot = simals_setroot.right;
//			}
//	
//			flag = 1;
//			for(;;)
//			{
//				if (flag)
//				{
//					if (xfgets(s1, 255, vin))
//					{
//						xclose(vin);
//						Sim.simals_initialize_simulator(false);
//						break;
//					}
//					simals_convert_to_upper(s1);
//					if (simals_fragment_command(s1)) break;
//				}
//	
//				if (! estrcmp(simals_instbuf, x_("CLOCK")))
//				{
//					simals_convert_to_upper(&(simals_instbuf[simals_instptr[1]]));
//					nodehead = ALS.simals_find_node(&(simals_instbuf[simals_instptr[1]]));
//					if (! nodehead)
//					{
//						System.out.println("ERROR: Unable to find node %s"),
//							&(simals_instbuf[simals_instptr[1]]));
//						flag = 1;
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
//							flag = 0;
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
//				}
//	
//				if (! estrcmp(simals_instbuf, x_("SET")))
//				{
//					simals_convert_to_upper(&(simals_instbuf[simals_instptr[1]]));
//					nodehead = ALS.simals_find_node(&(simals_instbuf[simals_instptr[1]]));
//					if (! nodehead)
//					{
//						System.out.println("ERROR: Unable to find node %s"),
//							&(simals_instbuf[simals_instptr[1]]));
//						flag = 1;
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
//					flag = 1;
//				}
//			}
//			return;
//		}
	
		if (par[0].equals("new"))
		{
			// clear all vectors
			simals_clearallvectors(false);
			als.getSim().simals_initialize_simulator(false);
			return;
		}
	
//		if (par[0].equals("save"))
//		{
//			if (par.length < 2)
//			{
//				infstr = initinfstr();
//				formatinfstr(infstr, x_("%s.vec"), el_curlib.libname);
//				par[1] = fileselect(_("ALS vector file"), sim_filetypealsvec|FILETYPEWRITE,
//					returninfstr(infstr));
//				if (par[1] == 0) return;
//			}
//			vout = xcreate(par[1], sim_filetypealsvec, 0, &truename);
//			if (vout == 0)
//			{
//				if (truename != 0) System.out.println("ERROR: Can't create %s"), truename);
//				return;
//			}
//	
//			for (sethead = simals_setroot; sethead; sethead = sethead.right)
//			{
//				switch (sethead.type)
//				{
//					case 'C':
//						clokhead = (ROWPTR)sethead.ptr;
//						vecthead = (LINKPTR)clokhead.inptr;
//						simals_compute_node_name((NODEPTR)vecthead.ptr, s1);
//						xprintf(vout, x_("CLOCK %s D=%g L=%g E=%g "), s1, clokhead.delta,
//							clokhead.linear, clokhead.exp);
//						xprintf(vout, x_("STRENGTH=%d TIME=%g CYCLES=%ld\n"), vecthead.strength/2,
//							sethead.time, sethead.state);
//						for (; vecthead; vecthead = vecthead.right)
//						{
//							s2 = ALS.simals_trans_number_to_state(vecthead.state);
//							xprintf(vout, x_("  %s %g\n"), s2, vecthead.time);
//						}
//						break;
//					case 'N':
//						simals_compute_node_name((NODEPTR)sethead.ptr, s1);
//						s2 = ALS.simals_trans_number_to_state(sethead.state);
//						xprintf(vout, x_("SET %s=%s@%d TIME=%g\n"), s1, s2, sethead.strength/2,
//							sethead.time);
//				}
//			}
//			xclose(vout);
//			return;
//		}
	
		if (par[0].equals("delete"))
		{
			if (par.length < 3)
			{
				System.out.println("telltool simulation als vector delete NODE OPTIONS");
				return;
			}
			par[1] = par[1].toUpperCase();
			ALS.Node nodehead = als.simals_find_node(par[1]);
			if (nodehead == null)
			{
				System.out.println("ERROR: Unable to find node " + par[1]);
				return;
			}
	
//			backptr = (CHAR**) &ALS.simals_setroot;
			ALS.Link sethead = als.simals_setroot;
	
			if (par[2].charAt(0) == 'a')
			{
				while (sethead != null)
				{
					if (sethead.type == 'C')
					{
						ALS.Row clokhead = (ALS.Row)sethead.ptr;
						Iterator it = clokhead.inList.iterator();
						ALS.Link vecthead = (ALS.Link)it.next();
						if ((ALS.Node)vecthead.ptr == nodehead)
						{
//							*backptr = (CHAR *)sethead.right;
//							sethead = (ALS.Link)*backptr;
							continue;
						}
					} else
					{
						if ((ALS.Node)sethead.ptr == nodehead)
						{
//							*backptr = (CHAR *)sethead.right;
//							sethead = (ALS.Link)*backptr;
							continue;
						}
					}
	
//					backptr = (CHAR**) &(sethead.right);
					sethead = sethead.right;
				}
				als.getSim().simals_initialize_simulator(false);
				return;
			}
	
			if (par.length < 4)
			{
				System.out.println("telltool simulation als vector delete time TIME");
				return;
			}
			double time = TextUtils.atof(par[2]);
			while (sethead != null)
			{
				if (sethead.time == time)
				{
					if (sethead.type == 'C')
					{
						ALS.Row clokhead = (ALS.Row)sethead.ptr;
						Iterator it = clokhead.inList.iterator();
						ALS.Link vecthead = (ALS.Link)it.next();
						if ((ALS.Node)vecthead.ptr == nodehead)
						{
//							*backptr = (CHAR*)sethead.right;
							als.getSim().simals_initialize_simulator(false);
							return;
						}
					} else
					{
						if ((ALS.Node)sethead.ptr == nodehead)
						{
//							*backptr = (CHAR *)sethead.right;
							als.getSim().simals_initialize_simulator(false);
							return;
						}
					}
				}
	
//				backptr = (CHAR**) &(sethead.right);
				sethead = sethead.right;
			}
			return;
		}
	
		System.out.println("telltool simulation als vector");
	}
	
	/****************************** ANNOTATE ******************************/

	private ALS.DelayTypes simals_sdfdelaytype;

	/**
	 * Method to annotate node information onto corresponding schematic.
	 */
	void simals_annotate_command(String [] par)
	{
		if (par.length < 1)
		{
			System.out.println("telltool simulation als annotate [minimum | typical | maximum]");
			return;
		}
	
		if (als.simals_levelptr == null)
		{
			System.out.println("Must start simulator before annotating delay information");
			return;
		}
	
		if (par[0].equals("min")) simals_sdfdelaytype = ALS.DelayTypes.DELAY_MIN;
			else if (par[0].equals("typ")) simals_sdfdelaytype = ALS.DelayTypes.DELAY_TYP;
				else if (par[0].equals("max")) simals_sdfdelaytype = ALS.DelayTypes.DELAY_MAX;
					else
		{
			System.out.println("telltool simulation als annotate");
			return;
		}
	
		simals_sdfannotate(als.simals_levelptr);
		simals_update_netlist();
		System.out.println("Completed annotation of SDF " + par[0] + " delay values");
	}
	
	/**
	 * Method to annotate SDF port delay info onto ALS netlist.
	 */
	private void simals_sdfannotate(ALS.Connect cellhead)
	{
		String s1 = als.simals_compute_path_name(cellhead);
	
		NodeInst ni = simals_getcellinstance(cellhead.model_name, s1);
		if (ni != null)
		{
			simals_sdfportdelay(cellhead, ni, s1);
		}
	
		if (als.simals_instbuf[als.simals_instptr[1]] == 'X') return;
	
		for (ALS.Connect subcell = cellhead.child; subcell != null; subcell = subcell.next)
			simals_sdfannotate(subcell);
	}
	
	/**
	 * Method to get a NODEINST for specified cell instance.
	 */
	private NodeInst simals_getcellinstance(String celltype, String instance)
	{
		Cell np = WindowFrame.getCurrentCell();
		NodeInst ni = null;
	
		// separate out each hiearchy level - skip first level which is the top
		String [] instlist = instance.split(".");
		int count = instlist.length - 2;
		if (count == 0) return null;
	
		// find the NodeInst corresponding to bottom level of hierarchy
		for(int i=0; i<count; i++)
		{
			for(Iterator it = np.getNodes(); it.hasNext(); )
			{
				ni = (NodeInst)it.next();
				if (!ni.getName().equalsIgnoreCase(instlist[i])) continue;
				np = null;
				if (ni.getProto() instanceof Cell) np = (Cell)ni.getProto();
				break;
			}
			if (ni == null) break;
			if (np == null) break;
		}
		return ni;
	}
	
	/**
	 * Method to extract SDF port delay information and annotate it to ALS netlist.
	 */
	void simals_sdfportdelay(ALS.Connect cellhead, NodeInst ni, String path)
	{
		for (ALS.ALSExport exhead = cellhead.exptr; exhead != null; exhead = exhead.next)
		{
			for(Iterator it = ni.getPortInsts(); it.hasNext(); )
			{
				PortInst pi = (PortInst)it.next();
				if (pi.getPortProto().getName().equalsIgnoreCase((String)exhead.node_name))
				{
					Variable var = pi.getVar("SDF_absolute_port_delay");
					if (var != null && var.getObject() instanceof String[])
					{
						String [] delays = (String [])var.getObject();
						for (int i=0; i<delays.length; i++)
						{
							if (delays[i].startsWith(path))
							{
								for (int j=0; j<12; j++)
								{
									int delay = simals_getportdelayvalue(delays[i], simals_tnames[j], simals_sdfdelaytype);
									if (delay != -1) exhead.td[j] = delay; else
										exhead.td[j] = 0;
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Method to extract delay value from delay data string.
	 */
	private int simals_getportdelayvalue(String datastring, String transition, ALS.DelayTypes delaytype)
	{
		// split data string into separate pieces
		String [] instlist = datastring.split(" ");
	
		// get piece that corresponds to specified transition
		for (int i=0; i<instlist.length; i++)
		{
			if (instlist[i].startsWith(transition))
				return simals_getdval(instlist[i], delaytype);
		}
	
		return -1;
	}
	
	/**
	 * Method to get a delay value string from a transition string.
	 *     if tstring is '01(111:222:333)' and delaytype is DELAY_TYP return 222
	 */
	private int simals_getdval(String tstring, ALS.DelayTypes delaytype)
	{
		StringBuffer str = new StringBuffer();
		int stop = 0, start = 0;
		for(int i=0; i<tstring.length(); i++)
		{
			char chr = tstring.charAt(i);
			if (chr == ')') stop++;
			if (start != 0 && stop == 0) str.append(chr);
			if (chr == '(') start++;
		}
	
		String ts = str.toString();
	
		// delay string is not a triple, only one delay value implies typical
		if (ts.indexOf(':') < 0)
		{
			if (delaytype == ALS.DelayTypes.DELAY_TYP) return TextUtils.atoi(ts);
			return -1;
		}

		String [] t = ts.split(":");
		if (delaytype == ALS.DelayTypes.DELAY_MIN && t.length > 0) return TextUtils.atoi(t[0]);
		if (delaytype == ALS.DelayTypes.DELAY_TYP && t.length > 1) return TextUtils.atoi(t[1]);
		if (delaytype == ALS.DelayTypes.DELAY_MAX && t.length > 2) return TextUtils.atoi(t[2]);
		return -1;
	}
	
	/****************************** ORDER ******************************/
	
	/**
	 * Method to save/restore signal trace order for waveform display.
	 */
	void simals_order_command(String [] par)
	{
		if (par.length < 1)
		{
			System.out.println("telltool simulation als order [save | restore]");
			return;
		}
	
		if (par[0].startsWith("sav")) simals_order_save();
			else if (par[0].startsWith("res"))
		{
			if (par.length != 2)
			{
				System.out.println("telltool simulation als order restore OPTION");
				return;
			}
			simals_order_restore(par[1]);
		} else
		{
			System.out.println("telltool simulation als order");
			return;
		}
	}
	
	private void simals_order_save()
	{
//		INTBIG tr;
//		BOOLEAN first = FALSE;
//		CHAR str[256], *ts;
//		NODEPROTO *curcell;
//		REGISTER void *infstr = 0;
	
//		sim_window_inittraceloop();
//		while ((tr = sim_window_nexttraceloop()) != 0)
//		{
//			if (!first) infstr = initinfstr();
//			(void)esnprintf(str, 256, x_("%s:"), sim_window_gettracename(tr));
//			addstringtoinfstr(infstr, str);
//			first = TRUE;
//		}
//	
//		if (first)
//		{
//			ts = returninfstr(infstr);
//			ts[(estrlen(ts)-1)] = 0;  // chop off trailing ":"
//	
//			// save on current cell
//			curcell = getcurcell();
//			if (curcell != NULL) (void)setval((INTBIG)curcell, VNODEPROTO,
//				x_("SIM_als_trace_order"), (INTBIG)ts, VSTRING);
//		}
	}
	
	private void simals_order_restore(String list)
	{
//		INTBIG tc = 0, i, found, lines, thispos, pos, fromlib = 0;
//		INTBIG tr, trl;
//		NODEPTR node;
//		VARIABLE *var;
//		NODEPROTO *curcell;
//		CHAR *pt, *str, **tl, tmp[256];
	
//		if (namesame(list, x_("fromlib")) == 0)
//		{
//			curcell = getcurcell();
//			if (curcell != NONODEPROTO)
//			{
//				var = getval((INTBIG)curcell, VNODEPROTO, VSTRING, x_("SIM_als_trace_order"));
//				if (var != NOVARIABLE)
//				{
//					(void)esnprintf(tmp, 256, x_("%s"), (CHAR *)var.addr);
//					fromlib++;
//				}
//				else return;
//			}
//			else return;
//		}
//		else (void)esnprintf(tmp, 256, x_("%s"), list);
//	
//		// count number of traces and fill trace list array
//		for (pt = tmp; *pt != 0; pt++) if (*pt == ':') tc++;
//		if (tc == 0) return;
//		tc++;
//		tl = (CHAR **)emalloc(tc * (sizeof(CHAR *)), el_tempcluster);
//		pt = tmp;
//		for (i=0; i<tc; i++)
//		{
//			str = getkeyword(&pt, x_(":"));
//			(void)allocstring(&tl[i], str, el_tempcluster);
//			(void)tonextchar(&pt);
//		}
//	
//		// delete traces not in restored list
//		sim_window_cleartracehighlight();
//		sim_window_inittraceloop();
//		while ((tr = sim_window_nexttraceloop()) != 0)
//		{
//			found = 0;
//			for (i=0; i<tc; i++)
//			{
//				if (namesame(sim_window_gettracename(tr), tl[i]) == 0) found++;
//			}
//			if (!found)
//			{
//				thispos = sim_window_gettraceframe(tr);
//				sim_window_inittraceloop2();
//				for(;;)
//				{
//					trl = sim_window_nexttraceloop2();
//					if (trl == 0) break;
//					pos = sim_window_gettraceframe(trl);
//					if (pos > thispos) sim_window_settraceframe(trl, pos-1);
//				}
//				lines = sim_window_getnumframes();
//				if (lines > 1) sim_window_setnumframes(lines-1);
//	
//				// remove from the simulator's list
//				for(i=0; i<ALS.simals_levelptr.num_chn; i++)
//				{
//					node = ALS.simals_levelptr.display_page[i+1].nodeptr;
//					if (node == 0) continue;
//					if (ALS.simals_levelptr.display_page[i+1].displayptr == tr)
//					{
//						ALS.simals_levelptr.display_page[i+1].displayptr = 0;
//						break;
//					}
//				}
//	
//				// kill trace, redraw
//				sim_window_killtrace(tr);
//				sim_window_setnumframes(sim_window_getnumframes()-1);
//			}
//		}
//	
//		// order the traces
//		sim_window_setnumframes(tc);
//		sim_window_inittraceloop();
//		while ((tr = sim_window_nexttraceloop()) != 0)
//		{
//			for (i=0; i<tc; i++)
//			   if (namesame(tl[i], sim_window_gettracename(tr)) == 0) break;
//			if (fromlib) sim_window_settraceframe(tr, tc-i-1); else  // order from library save is bottom to top
//				sim_window_settraceframe(tr, i);
//		}
//	
//		sim_window_redraw();
	}
	
	/**
	 * Method to update the flattened netlist with the annotated delay values.
	 */
	void simals_update_netlist()
	{
		for (ALS.Model primhead = als.simals_primroot; primhead != null; primhead = primhead.next)
		{
			switch (primhead.type)
			{
				case 'F':
					break;
	
				case 'G':
					// cycle through all entries in table
					for (ALS.Row rowhead = (ALS.Row)primhead.ptr; rowhead != null; rowhead=rowhead.next)
					{
						// check for valid delay transition name for current entry
						if (rowhead.delay.equals("XX"))
						{
							// TESTING - get the max delay value of all input ports matching transition
							ALS.Connect cellhead = als.simals_find_level(simals_parent_level(primhead.level));
							int max_delay = 0;
							for (ALS.ALSExport exhead = cellhead.exptr; exhead != null; exhead = exhead.next)
							{
								int delay = exhead.td[simals_get_tdindex(rowhead.delay)];
								if (max_delay < delay) max_delay = delay;
							}
							if (max_delay != 0)
							{
								rowhead.abs = max_delay * 1.0e-12f;
							}
							System.out.println("*** DEBUG *** gate: " + primhead.name + ", level: " + primhead.level +
								", delay: " + (max_delay * 1.0e-12) + "(" + rowhead.delay + ")");
							System.out.println("  Timing: D=" + rowhead.delta + ", L=" + rowhead.linear +
								", E=" + rowhead.exp + ", R=" + rowhead.random + ", A=" + rowhead.abs);
							simals_print_in_entry(rowhead);
							simals_print_out_entry(rowhead);
						}
					}
					break;
	
				default:
					System.out.println("Illegal primitive type '" + primhead.type + "', database is bad");
					break;
			}
		}
	}
	
	/**
	 * Method to return index for transition delays given text name.
	 */
	private int simals_get_tdindex(String name)
	{
		for (int i=0; i<12; i++)
		{
			if (simals_tnames[i].equals(name)) return i;
		}
		return 0;  // return '01' index
	}
	
	/**
	 * Method to return the parent level of the given child.
	 *     if .TOP.NODE3.G1 is child, .TOP.NODE3 is parent
	 */
	private String simals_parent_level(String child)
	{
		// separate out each hiearchy level
		String [] instlist = child.split(".");
	
		// create the parent level name
		StringBuffer infstr = new StringBuffer();
		for (int i=0; i<instlist.length-1; i++)
		{
			infstr.append(instlist[i]);
			if (i != instlist.length-2) infstr.append(".");
		}
	
		return infstr.toString();
	}
}
