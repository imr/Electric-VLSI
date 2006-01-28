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

import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.simulation.als.ALS.Model;

import java.util.Iterator;

/**
 * Class to handle commands to the ALS Simulator.
 */
public class Command
{
	private ALS als;

	static String [] tNames = {"01", "10", "0Z", "Z1", "1Z", "Z0", "0X", "X1", "1X", "X0", "XZ", "ZX"};

	Command(ALS als)
	{
		this.als = als;
	}

	/****************************** PRINT ******************************/

	/**
	 * Method to print out the display screen status and information
	 */
	void printCommand(String [] par)
	{
		if (par.length < 1)
		{
			System.out.println("telltool simulation als print OPTION");
			return;
		}

		if (par[0].equals("vector"))
		{
			ALS.Link linkHead = als.setRoot;
			System.out.println("** VECTOR LINKLIST **");
			while (linkHead != null)
			{
				switch (linkHead.type)
				{
					case 'N':
						ALS.Node nodeHead = (ALS.Node)linkHead.ptr;
						String s1 = Stimuli.describeLevel(((Integer)linkHead.state).intValue());
						System.out.println("***** vector: $N" + nodeHead.getIndex() + ", state = " + s1 +
								", strength = " + Stimuli.describeStrength(linkHead.strength) + ", time = " + linkHead.time +
								", priority = " + linkHead.priority);
						break;
					case 'F':
						ALS.Stat statHead = (ALS.Stat)linkHead.ptr;
						nodeHead = statHead.nodePtr;
						s1 = Stimuli.describeLevel(((Integer)linkHead.state).intValue());
						System.out.println("***** function: $N" + nodeHead.getIndex() + ", state = " + s1 +
							", strength = " + Stimuli.describeStrength(linkHead.strength) + ", time = " + linkHead.time +
							", priority = " + linkHead.priority);
						break;
					case 'R':
						System.out.println("***** rowptr = " + linkHead.ptr + ", time = " + linkHead.time +
							", priority = " + linkHead.priority);
						break;
					case 'C':
						System.out.println("***** clokptr = " + linkHead.ptr + ", time = " + linkHead.time +
							", priority = " + linkHead.priority);
				}
				linkHead = linkHead.right;
			}
			return;
		}

		if (par[0].equals("netlist"))
		{
			System.out.println("** NETWORK DESCRIPTION **");
			for(Model primHead : als.primList)
			{
				switch (primHead.type)
				{
					case 'F':
						StringBuffer infstr = new StringBuffer();
						infstr.append("FUNCTION: " + primHead.name + " (instance " +
							(primHead.level == null ? "null" : primHead.level) + ") [");
						boolean first = true;
						for(ALS.ALSExport exHead : primHead.exList)
						{
							if (first) first = false; else
								infstr.append(", ");
							infstr.append("N" + exHead.nodePtr.getIndex());
						}
						infstr.append("]");
						System.out.println(infstr.toString());
						infstr = new StringBuffer();
						infstr.append("  Event Driving Inputs:");
						ALS.Func funcHead = (ALS.Func)primHead.ptr;
						for(ALS.ALSExport exHead : funcHead.inList)
						{
							infstr.append(" N" + exHead.nodePtr.getIndex());
						}
						System.out.println(infstr.toString());
						infstr = new StringBuffer();
						infstr.append("  Output Ports:");
						for(ALS.ALSExport exHead : primHead.exList)
						{
							if (exHead.nodeName != null)
								infstr.append(" N" + ((ALS.Stat)exHead.nodeName).nodePtr.getIndex());
						}
						System.out.println(infstr.toString());
						System.out.println("  Timing: D=" + funcHead.delta + ", L=" + funcHead.linear + ", E=" + funcHead.exp +
							", R=" + funcHead.random + ", A=" + funcHead.abs);
						System.out.println("  Firing Priority = " + primHead.priority);
						break;
					case 'G':
						System.out.println("GATE: " + primHead.name + " (instance " +
							(primHead.level == null ? "null" : primHead.level) + ")");
						for (ALS.Row rowHead = (ALS.Row)primHead.ptr; rowHead != null; rowHead=rowHead.next)
						{
							System.out.println("  Timing: D=" + rowHead.delta + ", L=" + rowHead.linear + ", E=" + rowHead.exp +
								", R=" + rowHead.random + ", A=" + rowHead.abs);
							System.out.println("  Delay type: " + (rowHead.delay == null ? "null" : rowHead.delay));
							printInEntry(rowHead);
							printOutEntry(rowHead);
						}
						System.out.println("  Firing Priority = " + primHead.priority);
						break;
					default:
						System.out.println("Illegal primitive type '" + primHead.type + "', database is bad");
						break;
				}
			}
			return;
		}
		System.out.println("telltool simulation als print");
	}

	/**
	 * Method to examine an input entry and prints out the condition
	 * that it represents.  It is possible for an input entry operand to represent
	 * a logic value, integer value, or another node address.
	 *
	 * Calling Arguments:
	 *	rowHead = pointer to the row being printed
	 */
	private void printInEntry(ALS.Row rowHead)
	{
		boolean flag = false;
		StringBuffer infstr = new StringBuffer();
		infstr.append("  Input: ");

		for(Object obj : rowHead.inList)
		{
			ALS.IO ioHead = (ALS.IO)obj;
			if (flag) infstr.append("& ");
			flag = true;

			ALS.Node nodeHead = (ALS.Node)ioHead.nodePtr;
			infstr.append("N" + nodeHead.getIndex());

			if (ioHead.operatr > 127)
			{
				int operatr = ioHead.operatr - 128;
				nodeHead = (ALS.Node)ioHead.operand;
				infstr.append(operatr + "N" + nodeHead.getIndex());
				continue;
			}

			infstr.append(ioHead.operatr);

			Integer num = (Integer)ioHead.operand;
			String s2 = Stimuli.describeLevel(num.intValue());
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
	 *	rowHead = pointer to the row being printed
	 */
	private void printOutEntry(ALS.Row rowHead)
	{
		boolean flag = false;
		StringBuffer infstr = new StringBuffer();
		infstr.append("  Output: ");

		for(Object obj : rowHead.outList)
		{
			ALS.IO ioHead = (ALS.IO)obj;
			if (flag) infstr.append("& ");
			flag = true;

			ALS.Stat statHead = (ALS.Stat)ioHead.nodePtr;
			ALS.Node nodeHead = statHead.nodePtr;
			infstr.append("N" + nodeHead.getIndex());

			if (ioHead.operatr > 127)
			{
				int operatr = ioHead.operatr - 128;
				nodeHead = (ALS.Node) ioHead.operand;
				infstr.append(operatr + "N" + nodeHead.getIndex() + "@" + ((ioHead.strength+1)/2) + " ");
				continue;
			}

			infstr.append(ioHead.operatr);

			Integer num = (Integer)ioHead.operand;
			String s2 = Stimuli.describeLevel(num.intValue());
			infstr.append(s2 + "@" + ((ioHead.strength+1)/2) + " ");
		}
		System.out.println(infstr.toString());
	}

}
