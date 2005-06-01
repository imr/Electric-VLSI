/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Flat.java
 * Asynchronous Logic Simulator network flattening
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

import com.sun.electric.database.text.TextUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Flat
{
	private ALS als;
	private ALS.Model simals_primptr2;
	private String simals_mainname = null;

	Flat(ALS als)
	{
		this.als = als;
	}

	/**
	 * Method to call a series of routines which convert the hierarchical
	 * network description into a flattened database representation.  The actual
	 * simulation must take place on the flattened network.  Returns true on error.
	 */
	boolean simals_flatten_network()
	{
		als.simals_nseq = als.simals_pseq = 0;
	
		/*
		 * create a "dummy" level to use as a mixed signal destination for plotting and
		 * screen display.  This level should be bypassed for structure checking and general
		 * simulation, however, so in the following code, references to "als.simals_cellroot"
		 * have been changed to als.simals_cellroot.next (pointing to simals_mainproto).
		 * Peter Gallant July 16, 1990
		 */
		als.simals_cellroot = new ALS.Connect();
		als.simals_cellroot.inst_name = "[MIXED_SIGNAL_LEVEL]";
		als.simals_cellroot.model_name = als.simals_cellroot.inst_name;
		als.simals_cellroot.exptr = null;
		als.simals_cellroot.parent = null;
		als.simals_cellroot.child = null;
		als.simals_cellroot.next = null;
		als.simals_cellroot.display_page = null;
		als.simals_cellroot.num_chn = 0;
		ALS.Connect temproot = als.simals_cellroot;
	
		// get upper-case version of main proto
		simals_mainname = als.simals_mainproto.getName().toUpperCase();
	
		als.simals_cellroot = new ALS.Connect();
		als.simals_cellroot.inst_name = simals_mainname;
		als.simals_cellroot.model_name = als.simals_cellroot.inst_name;
		als.simals_cellroot.exptr = null;
		als.simals_cellroot.parent = null;
		als.simals_cellroot.child = null;
		als.simals_cellroot.next = null;
		als.simals_cellroot.display_page = null;
		als.simals_cellroot.num_chn = 0;
	
		// these lines link the mixed level as the head followed by simals_mainproto PJG
		temproot.next = als.simals_cellroot;		// shouldn't this be null? ... smr
		temproot.child = als.simals_cellroot;
		als.simals_cellroot = temproot;
	
		// this code checks to see if model simals_mainproto is present in the netlist PJG
		ALS.Model modhead = simals_find_model(simals_mainname);
		if (modhead == null) return true;
		for (ALS.ALSExport exhead = modhead.exptr; exhead != null; exhead = exhead.next)
		{
			if (simals_find_xref_entry(als.simals_cellroot.next, (String)exhead.node_name) == null)
				return true;
		}
	
		if (simals_flatten_model(als.simals_cellroot.next)) return true;
	
		for (ALS.Node nodehead = als.simals_noderoot; nodehead != null; nodehead = nodehead.next)
		{
			if (nodehead.load < 1) nodehead.load = 1;
			nodehead.plot_node = 0;
		}
		return false;
	}
	
	/**
	 * Method to flatten a single model.  If other models are referenced
	 * in connection statements in the netlist, this routine is called recursively
	 * until a totally flat model is obtained.  Returns true on error.
	 *
	 * Calling Arguments:
	 *	cellhead = pointer to a data structure containing information about
	 *		  the model that is going to be flattened
	 */
	boolean simals_flatten_model(ALS.Connect cellhead)
	{
		ALS.Model modhead = simals_find_model(cellhead.model_name);
		if (modhead == null) return true;
		switch (modhead.type)
		{
			case 'F':
				if (simals_process_function(cellhead, modhead)) return true;
				break;
	
			case 'G':
				if (simals_process_gate(cellhead, modhead)) return true;
				break;
	
			case 'M':
				if (simals_process_connect_list(cellhead, (ALS.Connect)modhead.ptr)) return true;
				for (ALS.Connect subcell = cellhead.child; subcell != null; subcell = subcell.next)
				{
					if (simals_flatten_model(subcell)) return true;
				}
				break;
		}
	
		if (modhead.setList.size() != 0)
		{
			if (simals_process_set_entry(cellhead, modhead.setList)) return true;
		}
		return false;
	}
	
	/**
	 * Method to step through the connection list specified by the
	 * connection list pointer (conhead).  Values are entered into the cross
	 * reference table for the present level of hierarchy and new data structures
	 * are created for the lower level of hierarchy to store their cross
	 * reference tables.  Returns true on error.
	 *
	 * Calling Arguments:
	 *	cellhead = pointer to the cross reference data structure for the model
	 *		  that is going to be flattened
	 *	conhead  = pointer to a list of connection statements for the model
	 *		  that is being flattened by this procedure
	 */
	boolean simals_process_connect_list(ALS.Connect cellhead, ALS.Connect conhead)
	{
		while (conhead != null)
		{
			ALS.Connect cellptr2 = new ALS.Connect();
			cellptr2.inst_name = conhead.inst_name;
			cellptr2.model_name = conhead.model_name;
			cellptr2.exptr = null;
			cellptr2.parent = cellhead;
			cellptr2.child = null;
			cellptr2.display_page = null;
			cellptr2.next = cellhead.child;
			cellhead.child = cellptr2;
	
			ALS.Model modhead = simals_find_model(conhead.model_name);
			if (modhead == null) return true;
			als.simals_exptr2 = modhead.exptr;
			for (ALS.ALSExport exhead = conhead.exptr; exhead != null; exhead = exhead.next)
			{
				ALS.ALSExport xrefhead = simals_find_xref_entry(cellhead, (String)exhead.node_name);
				if (xrefhead == null) return true;
	
				if (als.simals_exptr2 == null)
				{
					System.out.println("Insufficient parameters declared for model '" + conhead.model_name + "' in netlist");
					return true;
				}
	
				for(ALS.ALSExport xrefptr1 = cellptr2.exptr; xrefptr1 != null; xrefptr1 = xrefptr1.next)
				{
					if (xrefptr1.node_name.equals(als.simals_exptr2.node_name))
					{
						System.out.println("Node '" + als.simals_exptr2.node_name + "' in model '" +
							conhead.model_name + "' connected more than once");
						return true;
					}
				}
				ALS.ALSExport xrefptr2 = new ALS.ALSExport();
				xrefptr2.node_name = als.simals_exptr2.node_name;
				xrefptr2.nodeptr = xrefhead.nodeptr;
				xrefptr2.next = cellptr2.exptr;
				cellptr2.exptr = xrefptr2;
	
				als.simals_exptr2 = als.simals_exptr2.next;
			}
	
			conhead = conhead.next;
		}
		return false;
	}
	
	/**
	 * Method to return a pointer to the model referenced by the
	 * calling argument character string.  Returns zero on error.
	 *
	 * Calling Arguments:
	 *	model_name = pointer to a string which contains the name of the model
	 *		    to be located by the search procedure
	 */
	ALS.Model simals_find_model(String model_name)
	{
		// convert to proper name
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<model_name.length(); i++)
		{
			char chr = model_name.charAt(i);
			if (!TextUtils.isLetterOrDigit(chr)) chr = '_';
			sb.append(chr);
		}
		String propername = sb.toString();
	
		ALS.Model modhead = als.simals_modroot;
		for(;;)
		{
			if (modhead == null)
			{
				System.out.println("ERROR: Model '" + propername + "' not found, simulation aborted");
				break;
			}
			if (modhead.name.equals(propername)) return modhead;
			modhead = modhead.next;
		}
		return null;
	}
	
	/**
	 * Method to return the flattened database node number for the
	 * specified model and node name.  Returns zero on error.
	 *
	 * Calling Arguments:
	 *	cellhead = pointer to the xref table for the model being processed
	 *	name    = pointer to a char string containing the node name
	 */
	ALS.ALSExport simals_find_xref_entry(ALS.Connect cellhead, String name)
	{
		for(ALS.ALSExport xrefptr1 = cellhead.exptr; xrefptr1 != null; xrefptr1 = xrefptr1.next)
		{
			if (xrefptr1.node_name.equals(name)) return xrefptr1;
		}
		ALS.ALSExport xrefptr2 = new ALS.ALSExport();
		xrefptr2.node_name = name;
		xrefptr2.next = cellhead.exptr;
		cellhead.exptr = xrefptr2;
	
		ALS.Node nodeptr2 = new ALS.Node();
		nodeptr2.cellptr = cellhead;
		nodeptr2.num = als.simals_nseq;
		++als.simals_nseq;
		nodeptr2.plot_node = 0;
		nodeptr2.statptr = null;
		nodeptr2.pinptr = null;
		nodeptr2.load = -1;
		nodeptr2.visit = 0;
		nodeptr2.tracenode = false;
		nodeptr2.next = als.simals_noderoot;
		xrefptr2.nodeptr = als.simals_noderoot = nodeptr2;
		return xrefptr2;
	}
	
	/**
	 * Method to step through the gate truth tables and examines all
	 * node references to insure that they have been included in the cross
	 * reference table for the model.  Returns true on error.
	 *
	 * Calling Arguments:
	 *	cellhead = pointer to the cross reference data structure for the model
	 *		  that is going to be flattened
	 *	modhead  = pointer to the dtat structure containing the hierarchical
	 *		  node references
	 */
	boolean simals_process_gate(ALS.Connect cellhead, ALS.Model modhead)
	{
		simals_primptr2 = new ALS.Model();
		simals_primptr2.num = als.simals_pseq;
		++als.simals_pseq;
		simals_primptr2.name = modhead.name;
		simals_primptr2.type = 'G';
		simals_primptr2.ptr = null;
		simals_primptr2.exptr = null;
		simals_primptr2.setList = new ArrayList();
		simals_primptr2.loadptr = null;
		simals_primptr2.fanout = modhead.fanout;
		simals_primptr2.priority = modhead.priority;
		simals_primptr2.level = als.simals_compute_path_name(cellhead);
		simals_primptr2.next = als.simals_primroot;
		als.simals_primroot = simals_primptr2;
	
		ALS.Row rowhead = (ALS.Row)modhead.ptr;
		while (rowhead != null)
		{
			ALS.Row simals_rowptr2 = new ALS.Row();
			simals_rowptr2.inList = new ArrayList();
			simals_rowptr2.outList = new ArrayList();
			simals_rowptr2.delta = rowhead.delta;
			simals_rowptr2.linear = rowhead.linear;
			simals_rowptr2.exp = rowhead.exp;
			simals_rowptr2.abs = rowhead.abs;
			simals_rowptr2.random = rowhead.random;
			simals_rowptr2.delay = rowhead.delay;
			if (rowhead.delay == null) simals_rowptr2.delay = null; else
				simals_rowptr2.delay = rowhead.delay;
			simals_rowptr2.next = (ALS.Row)simals_primptr2.ptr;
			simals_primptr2.ptr = simals_rowptr2;
	
			als.simals_ioptr1 = simals_rowptr2.inList;
			if (simals_process_io_entry(modhead, cellhead, rowhead.inList, 'I')) return true;
	
			als.simals_ioptr1 = simals_rowptr2.outList;
			if (simals_process_io_entry(modhead, cellhead, rowhead.outList, 'O')) return true;
	
			rowhead = rowhead.next;
		}
		return false;
	}
	
	/**
	 * Method to step through the node references contained within a
	 * row of a transition table and insures that they are included in the cross
	 * reference table in the event they were not previously specified in a
	 * connection statement.  Returns true on error.
	 *
	 * Calling Arguments:
	 *	modhead  = pointer to model that is being flattened
	 *	cellhead = pointer to the cross reference data structure for the model
	 *		  that is going to be flattened
	 *	iohead   = pointer to a row of node references to be checked for
	 *		  entry into the cross reference table
	 *	flag    = character indicating if the node is an input or output
	 */
	boolean simals_process_io_entry(ALS.Model modhead, ALS.Connect cellhead, List ioList, char flag)
	{
		for(Iterator it = ioList.iterator(); it.hasNext(); )
		{
			ALS.IO iohead = (ALS.IO)it.next();
			ALS.ALSExport xrefhead = simals_find_xref_entry(cellhead, (String)iohead.nodeptr);
			if (xrefhead == null) return true;
			als.simals_ioptr2 = new ALS.IO();
			als.simals_ioptr2.nodeptr = xrefhead.nodeptr;
			als.simals_ioptr2.operatr = iohead.operatr;
	
			if (als.simals_ioptr2.operatr > 127)
			{
				xrefhead = simals_find_xref_entry(cellhead, (String)iohead.operand);
				if (xrefhead == null) return true;
				als.simals_ioptr2.operand = xrefhead.nodeptr;
			} else
			{
				als.simals_ioptr2.operand = iohead.operand;
			}
	
			als.simals_ioptr2.strength = iohead.strength;
			als.simals_ioptr1.add(als.simals_ioptr2);
	
			switch (flag)
			{
				case 'I':
					if (simals_create_pin_entry(modhead, (String)iohead.nodeptr,
						(ALS.Node)als.simals_ioptr2.nodeptr)) return true;
					break;
				case 'O':
					als.simals_ioptr2.nodeptr = simals_create_stat_entry(modhead,
						(String)iohead.nodeptr, (ALS.Node)als.simals_ioptr2.nodeptr);
					if (als.simals_ioptr2.nodeptr == null) return true;
			}
	
			if (als.simals_ioptr2.operatr > 127)
			{
				if (simals_create_pin_entry(modhead, (String)iohead.operand,
					(ALS.Node)als.simals_ioptr2.operand)) return true;
			}
		}
		return false;
	}
	
	/**
	 * Method to make an entry into the primitive input table for the
	 * specified node.  This table keeps track of the primitives which use
	 * this node as an input for event driven simulation.  Returns true on error.
	 *
	 * Calling Arguments:
	 *	modhead   = pointer to the model structure from which the primitive
	 *		   is being created
	 *	node_name = pointer to a char string containing the name of the node
	 *		   whose input list is being updated
	 *	nodehead  = pointer to the node data structure allocated for this node
	 */
	boolean simals_create_pin_entry(ALS.Model modhead, String node_name, ALS.Node nodehead)
	{
		for(ALS.Load pinptr1 = nodehead.pinptr; pinptr1 != null; pinptr1 = pinptr1.next)
		{
			if (pinptr1.ptr == simals_primptr2) return false;
		}
		ALS.Load pinptr2 = new ALS.Load();
		pinptr2.ptr = simals_primptr2;
		pinptr2.next = nodehead.pinptr;
		nodehead.pinptr = pinptr2;
		nodehead.load += simals_find_load_value(modhead, node_name);
		return false;
	}
	
	/**
	 * Method to make an entry into the database for an output which
	 * is connected to the specified node.  Statistics are maintained for each output
	 * that is connected to a node.  Returns zero on error.
	 *
	 * Calling Arguments:
	 *	modhead   = pointer to the model structure from which the primitive
	 *		   is being created
	 *	node_name = pointer to a char string containing the name of the node
	 *		   whose output list is being updated
	 *	nodehead  = pointer to the node data structure allocated for this node
	 */
	ALS.Stat simals_create_stat_entry(ALS.Model modhead, String node_name, ALS.Node nodehead)
	{
		for(ALS.Stat statptr1 = nodehead.statptr; statptr1 != null; statptr1 = statptr1.next)
		{
			if (statptr1.primptr == simals_primptr2) return statptr1;
		}
		ALS.Stat statptr2 = new ALS.Stat();
		statptr2.primptr = simals_primptr2;
		statptr2.nodeptr = nodehead;
		statptr2.next = nodehead.statptr;
		nodehead.statptr = statptr2;
		nodehead.load += simals_find_load_value(modhead, node_name);
		return statptr2;
	}
	
	/**
	 * Method to return the loading factor for the specified node.  If
	 * the node can't be found in the load list it is assumed it has a default value
	 * of 1.0.
	 *
	 * Calling Arguments:
	 *	modhead   = pointer to the model structure from which the primitive
	 *		   is being created
	 *	node_name = pointer to a char string containing the name of the node
	 *		   whose load value is to be determined
	 */
	float simals_find_load_value(ALS.Model modhead, String node_name)
	{
		for (ALS.Load loadhead = modhead.loadptr; loadhead != null; loadhead = loadhead.next)
		{
			if (loadhead.ptr.equals(node_name)) return loadhead.load;
		}
	
		if (modhead.type == 'F') return 0;
		return 1;
	}
	
	/**
	 * Method to go through the set node list for the specified cell
	 * and generates vectors for the node.  These vectors are executed at t=0 by
	 * the simulator to initialize the node correctly.  Returns true on error.
	 *
	 * Calling Arguments:
	 *	cellhead = pointer to the cross reference table where the node locations
	 *		  are to be found
	 *	iohead   = pointer to the set list containing node names and state info
	 */
	boolean simals_process_set_entry(ALS.Connect cellhead, List ioList)
	{
		for(Iterator it = ioList.iterator(); it.hasNext(); )
		{
			ALS.IO iohead = (ALS.IO)it.next();
			ALS.ALSExport xrefhead = simals_find_xref_entry(cellhead, (String)iohead.nodeptr);
			if (xrefhead == null) return true;
	
			ALS.Link sethead = new ALS.Link();
			sethead.type = 'N';
			sethead.ptr = xrefhead.nodeptr;
			sethead.state = iohead.operand;
			sethead.strength = iohead.strength;
			sethead.priority = 2;
			sethead.time = 0.0;
			sethead.right = null;
			als.getSim().simals_insert_set_list(sethead);
		}
		return false;
	}
	
	/**
	 * Method to step through the event driving input list for a function
	 * and enters the function into the primitive input list for the particular node.
	 * In addition to this task the procedure sets up the calling argument node list
	 * for the function when it is called.  Returns true on error.
	 *
	 * Calling Arguments:
	 *	cellhead = pointer to the cross reference data structure for the model
	 *		  that is going to be flattened
	 *	modhead  = pointer to the data structure containing the hierarchical
	 *		  node references
	 */
	boolean simals_process_function(ALS.Connect cellhead, ALS.Model modhead)
	{
		simals_primptr2 = new ALS.Model();
		simals_primptr2.num = als.simals_pseq;
		++als.simals_pseq;
		simals_primptr2.name = modhead.name;
		simals_primptr2.type = 'F';
		simals_primptr2.ptr = new ALS.Func();
		simals_primptr2.exptr = null;
		simals_primptr2.setList = new ArrayList();
		simals_primptr2.loadptr = null;
		simals_primptr2.fanout = 0;
		simals_primptr2.priority = modhead.priority;
		simals_primptr2.level = als.simals_compute_path_name(cellhead);
		simals_primptr2.next = als.simals_primroot;
		als.simals_primroot = simals_primptr2;
	
		ALS.Func funchead = (ALS.Func)modhead.ptr;
		ALS.Func funcptr2 = (ALS.Func)simals_primptr2.ptr;
		funcptr2.procptr = ALS.UserProc.simals_get_function_address(modhead.name);
		if (funcptr2.procptr == null) return true;
		funcptr2.inptr = null;
		funcptr2.delta = funchead.delta;
		funcptr2.linear = funchead.linear;
		funcptr2.exp = funchead.exp;
		funcptr2.abs = funchead.abs;
		funcptr2.random = funchead.random;
		funcptr2.userptr = null;
		funcptr2.userint = 0;
		funcptr2.userfloat = 0;
	
		for (ALS.ALSExport exhead = modhead.exptr; exhead != null; exhead = exhead.next)
		{
			ALS.ALSExport xrefhead = simals_find_xref_entry(cellhead, (String)exhead.node_name);
			if (xrefhead == null) return true;
			als.simals_exptr2 = new ALS.ALSExport();
			if (exhead.nodeptr != null)
			{
				als.simals_exptr2.node_name = simals_create_stat_entry(modhead, (String)exhead.node_name, xrefhead.nodeptr);
				if (als.simals_exptr2.node_name == null) return true;
			} else
			{
				als.simals_exptr2.node_name = null;
			}
			als.simals_exptr2.nodeptr = xrefhead.nodeptr;
			als.simals_exptr2.next = simals_primptr2.exptr;
			simals_primptr2.exptr = als.simals_exptr2;
		}
	
		for (ALS.ALSExport exhead = funchead.inptr; exhead != null; exhead = exhead.next)
		{
			ALS.ALSExport xrefhead = simals_find_xref_entry(cellhead, (String)exhead.node_name);
			if (xrefhead == null) return true;
			als.simals_exptr2 = new ALS.ALSExport();
			als.simals_exptr2.nodeptr = xrefhead.nodeptr;
			als.simals_exptr2.next = simals_primptr2.exptr;
			simals_primptr2.exptr = als.simals_exptr2;
			if (simals_create_pin_entry(modhead, (String)exhead.node_name, xrefhead.nodeptr))
				return true;
		}
		return false;
	}
}
