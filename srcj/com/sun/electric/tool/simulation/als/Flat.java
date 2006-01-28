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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.als.ALS.IO;
import com.sun.electric.tool.simulation.als.ALS.Stat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class to flatten circuitry for the ALS Simulator.
 */
public class Flat
{
	private ALS       als;
	private ALS.Model primPtr2;

	Flat(ALS als)
	{
		this.als = als;
	}

	/**
	 * Method to call a series of routines which convert the hierarchical
	 * network description into a flattened database representation.  The actual
	 * simulation must take place on the flattened network.  Returns true on error.
	 */
	boolean flattenNetwork(Cell cell)
	{
		/*
		 * create a "dummy" level to use as a mixed signal destination for plotting and
		 * screen display.  This level should be bypassed for structure checking and general
		 * simulation, however, so in the following code, references to "als.cellRoot"
		 * have been changed to als.cellRoot.next (pointing to mainCell).
		 * Peter Gallant July 16, 1990
		 */
		als.cellRoot = new ALS.Connect();
		als.cellRoot.instName = "[MIXED_SIGNAL_LEVEL]";
		als.cellRoot.modelName = als.cellRoot.instName;
		als.cellRoot.exList = new ArrayList<ALS.ALSExport>();
		als.cellRoot.parent = null;
		als.cellRoot.child = null;
		als.cellRoot.next = null;
		ALS.Connect tempRoot = als.cellRoot;

		// get upper-case version of main proto
		String mainName = cell.getName().toUpperCase();

		als.cellRoot = new ALS.Connect();
		als.cellRoot.instName = mainName;
		als.cellRoot.modelName = als.cellRoot.instName;
		als.cellRoot.exList = new ArrayList<ALS.ALSExport>();
		als.cellRoot.parent = null;
		als.cellRoot.child = null;
		als.cellRoot.next = null;

		// these lines link the mixed level as the head followed by mainCell PJG
		tempRoot.next = als.cellRoot;		// shouldn't this be null? ... smr
		tempRoot.child = als.cellRoot;
		als.cellRoot = tempRoot;

		// this code checks to see if model mainCell is present in the netlist PJG
		ALS.Model modHead = findModel(mainName);
		if (modHead == null) return true;
		for(ALS.ALSExport exHead : modHead.exList)
		{
			findXRefEntry(als.cellRoot.next, (String)exHead.nodeName);
		}

		if (flattenModel(als.cellRoot.next)) return true;

		for(ALS.Node nodeHead : als.nodeList)
		{
			if (nodeHead.load < 1) nodeHead.load = 1;
		}
		return false;
	}

	/**
	 * Method to flatten a single model.  If other models are referenced
	 * in connection statements in the netlist, this routine is called recursively
	 * until a totally flat model is obtained.  Returns true on error.
	 *
	 * Calling Arguments:
	 *	cellHead = pointer to a data structure containing information about
	 *		  the model that is going to be flattened
	 */
	private boolean flattenModel(ALS.Connect cellHead)
	{
		ALS.Model modHead = findModel(cellHead.modelName);
		if (modHead == null) return true;
		switch (modHead.type)
		{
			case 'F':
				if (processFunction(cellHead, modHead)) return true;
				break;

			case 'G':
				processGate(cellHead, modHead);
				break;

			case 'M':
				if (processConnectList(cellHead, (ALS.Connect)modHead.ptr)) return true;
				for (ALS.Connect subCell = cellHead.child; subCell != null; subCell = subCell.next)
				{
					if (flattenModel(subCell)) return true;
				}
				break;
		}

		if (modHead.setList.size() != 0)
		{
			processSetEntry(cellHead, modHead.setList);
		}
		return false;
	}

	/**
	 * Method to step through the connection list specified by the
	 * connection list pointer (conHead).  Values are entered into the cross
	 * reference table for the present level of hierarchy and new data structures
	 * are created for the lower level of hierarchy to store their cross
	 * reference tables.  Returns true on error.
	 *
	 * Calling Arguments:
	 *	cellHead = pointer to the cross reference data structure for the model
	 *		  that is going to be flattened
	 *	conHead  = pointer to a list of connection statements for the model
	 *		  that is being flattened by this procedure
	 */
	private boolean processConnectList(ALS.Connect cellHead, ALS.Connect conHead)
	{
		while (conHead != null)
		{
			ALS.Connect cellPtr2 = new ALS.Connect();
			cellPtr2.instName = conHead.instName;
			cellPtr2.modelName = conHead.modelName;
			cellPtr2.exList = new ArrayList<ALS.ALSExport>();
			cellPtr2.parent = cellHead;
			cellPtr2.child = null;
			cellPtr2.next = cellHead.child;
			cellHead.child = cellPtr2;

			ALS.Model modHead = findModel(conHead.modelName);
			if (modHead == null) return true;
			Iterator<ALS.ALSExport> it = modHead.exList.iterator();
			for(ALS.ALSExport exHead : conHead.exList)
			{
				if (!it.hasNext()) break;
				als.exPtr2 = it.next();
				ALS.ALSExport xRefHead = findXRefEntry(cellHead, (String)exHead.nodeName);

				if (als.exPtr2 == null)
				{
					System.out.println("Insufficient parameters declared for model '" + conHead.modelName + "' in netlist");
					return true;
				}

				for(ALS.ALSExport xRefPtr1 : cellPtr2.exList)
				{
					if (xRefPtr1.nodeName.equals(als.exPtr2.nodeName))
					{
						System.out.println("Node '" + als.exPtr2.nodeName + "' in model '" +
							conHead.modelName + "' connected more than once");
						return true;
					}
				}
				ALS.ALSExport xRefPtr2 = new ALS.ALSExport();
				xRefPtr2.nodeName = als.exPtr2.nodeName;
				xRefPtr2.nodePtr = xRefHead.nodePtr;
				cellPtr2.exList.add(xRefPtr2);
			}

			conHead = conHead.next;
		}
		return false;
	}

	/**
	 * Method to return a pointer to the model referenced by the
	 * calling argument character string.  Returns zero on error.
	 *
	 * Calling Arguments:
	 *	modelName = pointer to a string which contains the name of the model
	 *		    to be located by the search procedure
	 */
	private ALS.Model findModel(String modelName)
	{
		// convert to proper name
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<modelName.length(); i++)
		{
			char chr = modelName.charAt(i);
			if (!TextUtils.isLetterOrDigit(chr)) chr = '_';
			sb.append(chr);
		}
		String properName = sb.toString();
		for(ALS.Model modHead : als.modelList)
		{
			if (modHead.name.equals(properName)) return modHead;
		}
		System.out.println("ERROR: Model '" + properName + "' not found, simulation aborted");
		return null;
	}

	/**
	 * Method to return the flattened database node number for the
	 * specified model and node name.
	 *
	 * Calling Arguments:
	 *	cellHead = pointer to the xref table for the model being processed
	 *	name    = pointer to a char string containing the node name
	 */
	private ALS.ALSExport findXRefEntry(ALS.Connect cellHead, String name)
	{
		for(ALS.ALSExport xRefPtr1 : cellHead.exList)
		{
			if (xRefPtr1.nodeName.equals(name)) return xRefPtr1;
		}
		ALS.ALSExport xRefPtr2 = new ALS.ALSExport();
		xRefPtr2.nodeName = name;
		cellHead.exList.add(xRefPtr2);

		ALS.Node nodePtr2 = new ALS.Node();
		nodePtr2.cellPtr = cellHead;
		nodePtr2.statList = new ArrayList<Stat>();
		nodePtr2.pinList = new ArrayList<ALS.Load>();
		nodePtr2.load = -1;
		nodePtr2.visit = 0;
		nodePtr2.traceNode = false;
		als.nodeList.add(nodePtr2);
		xRefPtr2.nodePtr = nodePtr2;
		return xRefPtr2;
	}

	/**
	 * Method to step through the gate truth tables and examines all
	 * node references to insure that they have been included in the cross
	 * reference table for the model.  Returns true on error.
	 *
	 * Calling Arguments:
	 *	cellHead = pointer to the cross reference data structure for the model
	 *		  that is going to be flattened
	 *	modHead  = pointer to the dtat structure containing the hierarchical
	 *		  node references
	 */
	private void processGate(ALS.Connect cellHead, ALS.Model modHead)
	{
		primPtr2 = new ALS.Model(modHead.name, 'G');
		primPtr2.fanOut = modHead.fanOut;
		primPtr2.priority = modHead.priority;
		primPtr2.level = als.computePathName(cellHead);
		als.primList.add(primPtr2);

		ALS.Row rowHead = (ALS.Row)modHead.ptr;
		ALS.Row last = null;
		while (rowHead != null)
		{
			ALS.Row rowPtr2 = new ALS.Row();
			rowPtr2.inList = new ArrayList<Object>();
			rowPtr2.outList = new ArrayList<Object>();
			rowPtr2.delta = rowHead.delta;
			rowPtr2.linear = rowHead.linear;
			rowPtr2.exp = rowHead.exp;
			rowPtr2.abs = rowHead.abs;
			rowPtr2.random = rowHead.random;
			rowPtr2.delay = rowHead.delay;
			if (rowHead.delay == null) rowPtr2.delay = null; else
				rowPtr2.delay = rowHead.delay;

			rowPtr2.next = null;
			if (last == null)
			{
				primPtr2.ptr = rowPtr2;
			} else
			{
				last.next = rowPtr2;
			}
			last = rowPtr2;

			als.ioPtr1 = rowPtr2.inList;
			processIOEntry(modHead, cellHead, rowHead.inList, 'I');

			als.ioPtr1 = rowPtr2.outList;
			processIOEntry(modHead, cellHead, rowHead.outList, 'O');

			rowHead = rowHead.next;
		}
	}

	/**
	 * Method to step through the node references contained within a
	 * row of a transition table and insures that they are included in the cross
	 * reference table in the event they were not previously specified in a
	 * connection statement.  Returns true on error.
	 *
	 * Calling Arguments:
	 *	modHead  = pointer to model that is being flattened
	 *	cellHead = pointer to the cross reference data structure for the model
	 *		  that is going to be flattened
	 *	ioHead   = pointer to a row of node references to be checked for
	 *		  entry into the cross reference table
	 *	flag    = character indicating if the node is an input or output
	 */
	private void processIOEntry(ALS.Model modHead, ALS.Connect cellHead, List<Object> ioList, char flag)
	{
		for(Object obj : ioList)
		{
			ALS.IO ioHead = (ALS.IO)obj;
			ALS.ALSExport xRefHead = findXRefEntry(cellHead, (String)ioHead.nodePtr);
			als.ioPtr2 = new ALS.IO();
			als.ioPtr2.nodePtr = xRefHead.nodePtr;
			als.ioPtr2.operatr = ioHead.operatr;

			if (als.ioPtr2.operatr > 127)
			{
				xRefHead = findXRefEntry(cellHead, (String)ioHead.operand);
				als.ioPtr2.operand = xRefHead.nodePtr;
			} else
			{
				als.ioPtr2.operand = ioHead.operand;
			}

			als.ioPtr2.strength = ioHead.strength;
			als.ioPtr1.add(als.ioPtr2);

			switch (flag)
			{
				case 'I':
					createPinEntry(modHead, (String)ioHead.nodePtr, (ALS.Node)als.ioPtr2.nodePtr);
					break;
				case 'O':
					als.ioPtr2.nodePtr = createStatEntry(modHead,
						(String)ioHead.nodePtr, (ALS.Node)als.ioPtr2.nodePtr);
			}

			if (als.ioPtr2.operatr > 127)
			{
				createPinEntry(modHead, (String)ioHead.operand, (ALS.Node)als.ioPtr2.operand);
			}
		}
	}

	/**
	 * Method to make an entry into the primitive input table for the
	 * specified node.  This table keeps track of the primitives which use
	 * this node as an input for event driven simulation.  Returns true on error.
	 *
	 * Calling Arguments:
	 *	modHead   = pointer to the model structure from which the primitive
	 *		   is being created
	 *	nodeName = pointer to a char string containing the name of the node
	 *		   whose input list is being updated
	 *	nodeHead  = pointer to the node data structure allocated for this node
	 */
	private void createPinEntry(ALS.Model modHead, String nodeName, ALS.Node nodeHead)
	{
		for(ALS.Load pinPtr1 : nodeHead.pinList)
		{
			if (pinPtr1.ptr == primPtr2) return;
		}
		ALS.Load pinPtr2 = new ALS.Load();
		pinPtr2.ptr = primPtr2;
		nodeHead.pinList.add(pinPtr2);
		nodeHead.load += findLoadValue(modHead, nodeName);
	}

	/**
	 * Method to make an entry into the database for an output which
	 * is connected to the specified node.  Statistics are maintained for each output
	 * that is connected to a node.  Returns zero on error.
	 *
	 * Calling Arguments:
	 *	modHead   = pointer to the model structure from which the primitive
	 *		   is being created
	 *	nodeName = pointer to a char string containing the name of the node
	 *		   whose output list is being updated
	 *	nodeHead  = pointer to the node data structure allocated for this node
	 */
	private ALS.Stat createStatEntry(ALS.Model modHead, String nodeName, ALS.Node nodeHead)
	{
		for(Stat statPtr1 : nodeHead.statList)
		{
			if (statPtr1.primPtr == primPtr2) return statPtr1;
		}
		ALS.Stat statPtr2 = new ALS.Stat();
		statPtr2.primPtr = primPtr2;
		statPtr2.nodePtr = nodeHead;
		nodeHead.statList.add(statPtr2);
		nodeHead.load += findLoadValue(modHead, nodeName);
		return statPtr2;
	}

	/**
	 * Method to return the loading factor for the specified node.  If
	 * the node can't be found in the load list it is assumed it has a default value
	 * of 1.0.
	 *
	 * Calling Arguments:
	 *	modHead   = pointer to the model structure from which the primitive
	 *		   is being created
	 *	nodeName = pointer to a char string containing the name of the node
	 *		   whose load value is to be determined
	 */
	private double findLoadValue(ALS.Model modHead, String nodeName)
	{
		for(ALS.Load loadHead : modHead.loadList)
		{
			if (loadHead.ptr.equals(nodeName)) return loadHead.load;
		}

		if (modHead.type == 'F') return 0;
		return 1;
	}

	/**
	 * Method to go through the set node list for the specified cell
	 * and generates vectors for the node.  These vectors are executed at t=0 by
	 * the simulator to initialize the node correctly.  Returns true on error.
	 *
	 * Calling Arguments:
	 *	cellHead = pointer to the cross reference table where the node locations
	 *		  are to be found
	 *	ioHead   = pointer to the set list containing node names and state info
	 */
	private void processSetEntry(ALS.Connect cellHead, List<ALS.IO> ioList)
	{
		for(ALS.IO ioHead : ioList)
		{
			ALS.ALSExport xRefHead = findXRefEntry(cellHead, (String)ioHead.nodePtr);

			ALS.Link setHead = new ALS.Link();
			setHead.type = 'N';
			setHead.ptr = xRefHead.nodePtr;
			setHead.state = ioHead.operand;
			setHead.strength = ioHead.strength;
			setHead.priority = 2;
			setHead.time = 0.0;
			setHead.right = null;
			als.insertSetList(setHead);
		}
	}

	/**
	 * Method to step through the event driving input list for a function
	 * and enters the function into the primitive input list for the particular node.
	 * In addition to this task the procedure sets up the calling argument node list
	 * for the function when it is called.  Returns true on error.
	 *
	 * Calling Arguments:
	 *	cellHead = pointer to the cross reference data structure for the model
	 *		  that is going to be flattened
	 *	modHead  = pointer to the data structure containing the hierarchical
	 *		  node references
	 */
	private boolean processFunction(ALS.Connect cellHead, ALS.Model modHead)
	{
		primPtr2 = new ALS.Model(modHead.name, 'F');
		primPtr2.ptr = new ALS.Func();
		primPtr2.priority = modHead.priority;
		primPtr2.level = als.computePathName(cellHead);
		als.primList.add(primPtr2);

		ALS.Func funcHead = (ALS.Func)modHead.ptr;
		ALS.Func funcPtr2 = (ALS.Func)primPtr2.ptr;
		funcPtr2.procPtr = ALS.UserProc.getFunctionAddress(modHead.name);
		if (funcPtr2.procPtr == null) return true;
		funcPtr2.inList = new ArrayList<ALS.ALSExport>();
		funcPtr2.delta = funcHead.delta;
		funcPtr2.linear = funcHead.linear;
		funcPtr2.exp = funcHead.exp;
		funcPtr2.abs = funcHead.abs;
		funcPtr2.random = funcHead.random;
		funcPtr2.userPtr = null;
		for(ALS.ALSExport exHead : modHead.exList)
		{
			ALS.ALSExport xRefHead = findXRefEntry(cellHead, (String)exHead.nodeName);
			als.exPtr2 = new ALS.ALSExport();
			if (exHead.nodePtr != null)
			{
				als.exPtr2.nodeName = createStatEntry(modHead, (String)exHead.nodeName, xRefHead.nodePtr);
			} else
			{
				als.exPtr2.nodeName = null;
			}
			als.exPtr2.nodePtr = xRefHead.nodePtr;
			primPtr2.exList.add(als.exPtr2);
		}

		for(ALS.ALSExport exHead : funcHead.inList)
		{
			ALS.ALSExport xRefHead = findXRefEntry(cellHead, (String)exHead.nodeName);
			als.exPtr2 = new ALS.ALSExport();
			als.exPtr2.nodePtr = xRefHead.nodePtr;
			primPtr2.exList.add(als.exPtr2);
			createPinEntry(modHead, (String)exHead.nodeName, xRefHead.nodePtr);
		}
		return false;
	}
}
