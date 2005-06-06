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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.ui.WaveformWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.util.ArrayList;
import java.util.Iterator;

public class Command
{
	private ALS als;

	static String [] simals_tnames = {"01", "10", "0Z", "Z1", "1Z", "Z0", "0X", "X1", "1X", "X0", "XZ", "ZX"};

	Command(ALS als)
	{
		this.als = als;
	}

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
			vectptr2.state = new Integer(als.simals_trans_state_to_number(par[i]));
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
		sethead.state = new Integer(num);
		sethead.priority = 1;
		sethead.time = 0;
		sethead.right = null;
		als.getSim().simals_insert_set_list(sethead);

		als.getSim().simals_initialize_simulator(false);
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
								", strength = " + Stimuli.describeStrength(linkhead.strength) + ", time = " + linkhead.time +
								", priority = " + linkhead.priority);
						break;
					case 'F':
						ALS.Stat stathead = (ALS.Stat)linkhead.ptr;
						nodehead = stathead.nodeptr;
						s1 = als.simals_trans_number_to_state(((Integer)linkhead.state).intValue());
						System.out.println("***** function: $N" + nodehead.num + ", state = " + s1 +
							", strength = " + Stimuli.describeStrength(linkhead.strength) + ", time = " + linkhead.time +
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
		System.out.println("telltool simulation als print");
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

		if (par[0].equals("min")) simals_sdfdelaytype = ALS.DelayTypes.DELAY_MIN;
			else if (par[0].equals("typ")) simals_sdfdelaytype = ALS.DelayTypes.DELAY_TYP;
				else if (par[0].equals("max")) simals_sdfdelaytype = ALS.DelayTypes.DELAY_MAX;
					else
		{
			System.out.println("telltool simulation als annotate");
			return;
		}

		simals_sdfannotate(als.simals_cellroot);
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
