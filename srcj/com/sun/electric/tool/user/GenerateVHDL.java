/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Generate.java
 * VHDL compiler tool: generate VHDL from a circuit
 * Written by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.user;

import com.sun.electric.tool.Listener;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.output.Sim;
import com.sun.electric.tool.io.output.Sim.Visitor;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.PrimitiveNode;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * This is the VHDL Compiler tool.
 */
public class GenerateVHDL
{

	private static final int MAXINPUTS = 30;

	/* special codes during VHDL generation */
	private static final int BLOCKNORMAL   = 0;		/* ordinary block */
	private static final int BLOCKMOSTRAN  = 1;		/* a MOS transistor */
	private static final int BLOCKBUFFER   = 2;		/* a buffer */
	private static final int BLOCKPOSLOGIC = 3;		/* an and, or, xor */
	private static final int BLOCKINVERTER = 4;		/* an inverter */
	private static final int BLOCKNAND     = 5;		/* a nand */
	private static final int BLOCKNOR      = 6;		/* a nor */
	private static final int BLOCKXNOR     = 7;		/* an xnor */
	private static final int BLOCKFLOPDS   = 8;		/* a settable D flip-flop */
	private static final int BLOCKFLOPDR   = 9;		/* a resettable D flip-flop */
	private static final int BLOCKFLOPTS  = 10;		/* a settable T flip-flop */
	private static final int BLOCKFLOPTR  = 11;		/* a resettable T flip-flop */
	private static final int BLOCKFLOP    = 12;		/* a general flip-flop */

	// make an array to hold the generated VHDL
	static List vhdlStrings;

	/**
	 * Method to ensure that cell "np" and all subcells have VHDL on them.
	 * If "force" is true, do conversion even if VHDL cell is newer.  Otherwise
	 * convert only if necessary.
	 */
	public static List vhdl_convertcell(Cell cell, boolean force)
	{
		// cannot make VHDL for cell with no ports
		if (cell.getNumPorts() == 0)
		{
			System.out.println("Cannot convert cell " + cell.describe() + " to VHDL: it has no ports");
			return null;
		}

		// make an array to hold the generated VHDL
		vhdlStrings = new ArrayList();

//		if (!force)
//		{
//			// determine most recent change to this or any subcell
//			dateoflayout = vhdl_getcelldate(cell);
//	
//			// if there is already VHDL that is newer, stop now
//			if (vhdlondisk)
//			{
//				// look for the disk file
//				f = xopen(cell.protoname, io_filetypevhdl, x_(""), &filename);
//				if (f != null)
//				{
//					xclose(f);
//					if (filedate(filename) >= dateoflayout) return;
//				}
//			} else
//			{
//				// look for the cell
//				npvhdl = anyview(cell, el_vhdlview);
//				if (npvhdl != NONODEPROTO)
//				{
//					// examine the VHDL to see if it really came from this cell
//					var = getvalkey((INTBIG)npvhdl, VNODEPROTO, VSTRING|VISARRAY, el_cell_message_key);
//					if (var != NOVARIABLE)
//					{
//						firstline = ((CHAR **)var.addr)[0];
//						desired = x_("-- VHDL automatically generated from cell ");
//						len = estrlen(desired);
//						if (estrncmp(firstline, desired, len) == 0)
//						{
//							if (estrcmp(&desired[len], describenodeproto(cell)) != 0)
//								dateoflayout = (time_t)(npvhdl.revisiondate + 1);
//						}
//					}
//	
//					if ((time_t)npvhdl.revisiondate >= dateoflayout) return;
//				}
//			}
//		}
	
		// recursively generate the VHDL
		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, null, new Visitor(cell));
		return vhdlStrings;
	}

	private static class Visitor extends HierarchyEnumerator.Visitor
	{
		private Cell cell;

		private Visitor(Cell cell)
		{
			this.cell = cell;
		}

		public boolean enterCell(HierarchyEnumerator.CellInfo info)
		{
			vhdl_generatevhdl(info);
			return true;
		}

		public void exitCell(HierarchyEnumerator.CellInfo info) {}

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) { return true; }
	}

	private static void vhdl_generatevhdl(HierarchyEnumerator.CellInfo info)
	{
		Cell np = info.getCell();

		// write the header
		vhdlStrings.add("-- VHDL automatically generated from cell " + np.describe());
		Netlist nl = info.getNetlist();

		// write the entity section
		vhdlStrings.add("entity " + vhdl_addstring(np.getName(), null) + " is port(" + vhdl_addportlist(null, np, 0) + ");");
		vhdlStrings.add("  end " + vhdl_addstring(np.getName(), null)  + ";");
	
		// now write the "architecture" line
		vhdlStrings.add("architecture " + vhdl_addstring(np.getName(), null)  + "_BODY of " +
			vhdl_addstring(np.getName(), null) + " is");
	
		// enumerate negated arcs
		int instnum = 1;
		HashMap negatedConnections = new HashMap();
		for(Iterator it = np.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			for(int i=0; i<2; i++)
			{
				Connection con = ai.getConnection(i);
				if (con.isNegated()) negatedConnections.put(con, new Integer(instnum++));
			}
		}
	
		// write prototypes for each node
		int [] gotnand = new int[MAXINPUTS+1];
		int [] gotnor = new int[MAXINPUTS+1];
		int [] gotxnor = new int[MAXINPUTS+1];
		List componentlist = new ArrayList();
		instnum = 1;
		boolean gotinverters = false;
		for(Iterator it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.isIconOfParent()) continue;
			AnalyzePrimitive ap = new AnalyzePrimitive(ni, negatedConnections);
			String pt = ap.getPrimName();
			if (pt == null) continue;
			int special = ap.getSpecial();
	
			// write only once per prototype
			if (special == BLOCKINVERTER)
			{
				gotinverters = true;
				continue;
			}
			if (special == BLOCKNAND)
			{
				int i = TextUtils.atoi(pt.substring(4));
				if (i <= MAXINPUTS) gotnand[i]++; else
					System.out.println("Cannot handle " + i + "-input NAND, limit is " + MAXINPUTS);
				continue;
			}
			if (special == BLOCKNOR)
			{
				int i = TextUtils.atoi(pt.substring(3));
				if (i <= MAXINPUTS) gotnor[i]++; else
					System.out.println("Cannot handle " + i + "-input NOR, limit is " + MAXINPUTS);
				continue;
			}
			if (special == BLOCKXNOR)
			{
				int i = TextUtils.atoi(pt.substring(4));
				if (i <= MAXINPUTS) gotxnor[i]++; else
					System.out.println("Cannot handle " + i + "-input XNOR, limit is " + MAXINPUTS);
				continue;
			}
	
			// ignore component with no ports
			if (ni.getProto().getNumPorts() == 0) continue;
	
			// see if this component is already written to the header
			if (componentlist.contains(pt)) continue;
	
			// new component: add to the list
			componentlist.add(pt);
	
			StringBuffer infstr = new StringBuffer();
			vhdlStrings.add("  component " + vhdl_addstring(pt, null) + " port(" + vhdl_addportlist(ni, ni.getProto(), special) + ");");
		}
	
		// write pseudo-prototype if there are any negated arcs
		if (negatedConnections.size() > 0) gotinverters = true;
		if (gotinverters)
		{
			vhdlStrings.add("  end " + vhdl_addstring(np.getName(), null)  + ";");
			vhdlStrings.add("  component inverter port(a: in BIT; y: out BIT);");
			vhdlStrings.add("    end component;");
		}
		for(int i=0; i<MAXINPUTS; i++)
		{
			if (gotnand[i] != 0)
			{
				StringBuffer multiDec = new StringBuffer();
				multiDec.append("  component nand" + i + " port(");
				for(int j=1; j<=i; j++)
				{
					if (j > 1) multiDec.append(", ");
					multiDec.append("a" + j);
				}
				multiDec.append(": in BIT; y: out BIT);");
				vhdlStrings.add(multiDec.toString());
				vhdlStrings.add("    end component;");
			}
			if (gotnor[i] != 0)
			{
				StringBuffer multiDec = new StringBuffer();
				multiDec.append("  component nor" + i + " port(");
				for(int j=1; j<=i; j++)
				{
					if (j > 1) multiDec.append(", ");
					multiDec.append("a" + j);
				}
				multiDec.append(": in BIT; y: out BIT);");
				vhdlStrings.add(multiDec.toString());
				vhdlStrings.add("    end component;");
			}
			if (gotxnor[i] != 0)
			{
				StringBuffer multiDec = new StringBuffer();
				multiDec.append("  component xnor" + i + " port(");
				for(int j=1; j<=i; j++)
				{
					if (j > 1) multiDec.append(", ");
					multiDec.append("a" + j);
				}
				multiDec.append(": in BIT; y: out BIT);");
				vhdlStrings.add(multiDec.toString());
				vhdlStrings.add("    end component;");
			}
		}
	
		// write internal nodes
		boolean first = false;
		int linelen = 0;
		StringBuffer infstr = new StringBuffer();
		for(Iterator it = nl.getNetworks(); it.hasNext(); )
		{
			Network net = (Network)it.next();
	
			// disallow if part of a bus export or already written
//			if (net.temp1 != 0) continue;
			String pt = net.toString();
			if (!first)
			{
				infstr.append("  signal ");
				linelen = 9;
			} else
			{
				infstr.append(", ");
				linelen += 2;
			}
			first = true;
			if (linelen + pt.length() > 80)
			{
				vhdlStrings.add(infstr.toString());
				infstr = new StringBuffer();
				infstr.append("    ");
				linelen = 4;
			}
			infstr.append(vhdl_addstring(pt, np));
			linelen += pt.length();
		}
		if (first)
		{
			infstr.append(": BIT;");
			vhdlStrings.add(infstr.toString());
		}
	
		// write pseudo-internal nodes for all negated arcs
		first = false;
		StringBuffer negNodes = new StringBuffer();
		for(Iterator it = negatedConnections.keySet().iterator(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			Integer index = (Integer)negatedConnections.get(con);
			if (!first)
			{
				negNodes.append("  signal ");
			} else negNodes.append(", ");
			negNodes.append("PINV" + index.intValue());
			first = true;
		}
		if (first)
		{
			negNodes.append(": BIT;");
			vhdlStrings.add(negNodes.toString());
		}
	
		// write the instances
		vhdlStrings.add("begin");
		for(Iterator it = nl.getNodables(); it.hasNext(); )
		{
			Nodable no = (Nodable)it.next();
			// ignore component with no ports
			if (no.getProto().getNumPorts() == 0) continue;

			int special = BLOCKNORMAL;
			String pt = no.getProto().getName();
			if (no instanceof NodeInst)
			{
				AnalyzePrimitive ap = new AnalyzePrimitive((NodeInst)no, negatedConnections);
				pt = ap.getPrimName();
				if (pt == null) continue;
				special = ap.getSpecial();
			}
	
			infstr = new StringBuffer();
			infstr.append("  ");
			String instname = no.getName();
			infstr.append(vhdl_addstring(instname, null));
	
			// make sure the instance name doesn't conflict with a prototype name
			if (componentlist.contains(instname)) infstr.append("NV");

			infstr.append(": " + vhdl_addstring(pt, null) + " port map(" + vhdl_addrealports(no, special, negatedConnections) + ");");
			vhdlStrings.add(infstr.toString());
		}
	
		// write pseudo-nodes for all negated arcs
		for(Iterator it = negatedConnections.keySet().iterator(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			Integer index = (Integer)negatedConnections.get(con);

			StringBuffer invertStr = new StringBuffer();
			invertStr.append("  PSEUDO_INVERT" + index.intValue() + ": inverter port map(");
			Network net = nl.getNetwork(con.getPortInst());
			if (con.getPortInst().getPortProto().getBasePort().getCharacteristic() == PortCharacteristic.OUT)
			{
				invertStr.append("PINV" + index.intValue() + ", " + vhdl_addstring(net.toString(), np));
			} else
			{
				invertStr.append(vhdl_addstring(net.toString(), np) + ", PINV" + index.intValue());
			}
			invertStr.append(");");
			vhdlStrings.add(invertStr.toString());
		}
	
		// write the end of the body
		vhdlStrings.add("end " + vhdl_addstring(np.getName(), null) + "_BODY;");
	}
	
	private static String vhdl_addrealports(Nodable no, int special, HashMap negatedConnections)
	{
		NodeProto np = no.getProto();
		boolean first = false;
		StringBuffer infstr = new StringBuffer();
		Netlist nl = no.getParent().getUserNetlist();
		for(int pass = 0; pass < 5; pass++)
		{
			for(Iterator it = np.getPorts(); it.hasNext(); )
			{
				PortProto pp = (PortProto)it.next();
//	#ifdef IGNORE4PORTTRANSISTORS
//				// ignore the bias port of 4-port transistors
//				if (ni.proto == sch_transistor4prim)
//				{
//					if (namesame(cpp.protoname, x_("b")) == 0) continue;
//				}
//	#endif
				if (pass == 0)
				{
					// must be an input port
					if (pp.getCharacteristic() != PortCharacteristic.IN) continue;
				}
				if (pass == 1)
				{
					// must be an output port
					if (pp.getCharacteristic() != PortCharacteristic.OUT) continue;
				}
				if (pass == 2)
				{
					// must be an output port
					if (pp.getCharacteristic() != PortCharacteristic.PWR) continue;
				}
				if (pass == 3)
				{
					// must be an output port
					if (pp.getCharacteristic() != PortCharacteristic.GND) continue;
				}
				if (pass == 4)
				{
					// any other port type
					if (pp.getCharacteristic() == PortCharacteristic.IN || pp.getCharacteristic() == PortCharacteristic.OUT ||
						pp.getCharacteristic() == PortCharacteristic.PWR || pp.getCharacteristic() == PortCharacteristic.GND)
							continue;
				}
	
				if (special == BLOCKMOSTRAN)
				{
					// ignore electrically connected ports
//					for(opp = np.firstportproto; opp != pp; opp = opp.nextportproto)
//						if (opp.network == pp.network) break;
//					if (opp != pp) continue;
				}
				if (special == BLOCKPOSLOGIC || special == BLOCKBUFFER || special == BLOCKINVERTER ||
					special == BLOCKNAND || special == BLOCKNOR || special == BLOCKXNOR)
				{
					// ignore ports not named "a" or "y"
					if (!pp.getName().equals("a") && !pp.getName().equals("y"))
					{
//						pp.temp1 = 1;
						continue;
					}
				}
				if (special == BLOCKFLOPTS || special == BLOCKFLOPDS)
				{
					// ignore ports not named "i1", "ck", "preset", or "q"
					if (!pp.getName().equals("i1") && !pp.getName().equals("ck") &&
						!pp.getName().equals("preset") && !pp.getName().equals("q"))
					{
//						pp.temp1 = 1;
						continue;
					}
				}
				if (special == BLOCKFLOPTR || special == BLOCKFLOPDR)
				{
					// ignore ports not named "i1", "ck", "clear", or "q"
					if (!pp.getName().equals("i1") && !pp.getName().equals("ck") &&
						!pp.getName().equals("clear") && !pp.getName().equals("q"))
					{
//						pp.temp1 = 1;
						continue;
					}
				}
	
				// if multiple connections, get them all
				if (pp.getBasePort().isIsolated())
				{
//					for(pi = ni.firstportarcinst; pi != NOPORTARCINST; pi = pi.nextportarcinst)
//					{
//						if (pi.proto != pp) continue;
//						ai = pi.conarcinst;
//						if (((ai.proto.userbits&AFUNCTION)>>AFUNCTIONSH) == APNONELEC) continue;
//						if (first != 0) addstringtoinfstr(infstr, x_(", "));   first = true;
//						if ((ai.userbits&ISNEGATED) != 0)
//						{
//							if ((ai.end[0].portarcinst == pi && (ai.userbits&REVERSEEND) == 0) ||
//								(ai.end[1].portarcinst == pi && (ai.userbits&REVERSEEND) != 0))
//							{
//								(void)esnprintf(line, 50, x_("PINV%ld"), ai.temp1);
//								addstringtoinfstr(infstr, line);
//								continue;
//							}
//						}
//						net = ai.network;
//						if (net.namecount > 0) vhdl_addstring(networkname(net, 0), ni.parent); else
//							addstringtoinfstr(infstr, describenetwork(net));
//					}
					continue;
				}
	
				// get connection
				Network net = nl.getNetwork(no, pp, 0);
				boolean portNamed = false;
				for(Iterator cIt = no.getNodeInst().getConnections(); cIt.hasNext(); )
				{
					Connection con = (Connection)cIt.next();
					Network cNet = nl.getNetwork(con.getPortInst());
					if (cNet == net)
					{
						ArcInst ai = con.getArc();
						if (ai.getProto().getFunction() != ArcProto.Function.NONELEC)
						{
							if (first) infstr.append(", ");   first = true;
							if (con.isNegated())
							{
								Integer index = (Integer)negatedConnections.get(con);
								if (index != null)
								{
									infstr.append("PINV" + index.intValue());
									continue;
								}
							}

							int wid = nl.getBusWidth(ai);
							for(int i=0; i<wid; i++)
							{
								if (i != 0) infstr.append(", ");
								Network subnet = nl.getNetwork(ai, i);
								infstr.append(vhdl_addstring(subnet.toString(), no.getParent()));
							}
							portNamed = true;
						}
						break;
					}
				}
				if (portNamed) continue;
	
				for(Iterator eIt = no.getNodeInst().getExports(); eIt.hasNext(); )
				{
					Export e = (Export)eIt.next();
					Network cNet = nl.getNetwork(e, 0);
					if (cNet == net)
					{
						if (first) infstr.append(", ");   first = true;
						vhdl_addstring(e.getName(), no.getParent());
						portNamed = true;
						break;
					}
				}
				if (portNamed) continue;
	
				// port is not connected or an export
				if (first) infstr.append(", ");   first = true;
				infstr.append("open");
				System.out.println("Warning: port " + pp.getName() + " of node " + no.toString() + " is not connected");
			}
		}
		return infstr.toString();
	}

	/**
	 * Class to return the VHDL name to use for node "ni".  Returns a "special" value
	 * that indicates the nature of the node.
	 *  BLOCKNORMAL: no special port arrangements necessary
	 *  BLOCKMOSTRAN: only output ports that are not electrically connected
	 *  BLOCKBUFFER: only include input port "a" and output port "y"
	 *  BLOCKPOSLOGIC: only include input port "a" and output port "y"
	 *  BLOCKINVERTER: only include input port "a" and output port "y"
	 *  BLOCKNAND: only include input port "a" and output port "y"
	 *  BLOCKNOR: only include input port "a" and output port "y"
	 *  BLOCKXNOR: only include input port "a" and output port "y"
	 *  BLOCKFLOPTS: only include input ports "i1", "ck", "preset" and output port "q"
	 *  BLOCKFLOPTR: only include input ports "i1", "ck", "clear" and output port "q"
	 *  BLOCKFLOPDS: only include input ports "i1", "ck", "preset" and output port "q"
	 *  BLOCKFLOPDR: only include input ports "i1", "ck", "clear" and output port "q"
	 *  BLOCKFLOP: include input ports "i1", "i2", "ck", "preset", "clear", and output ports "q" and "qb"
	 */
	private static class AnalyzePrimitive
	{
		private String primName;
		private int special;

		private String getPrimName() { return primName; }
		private int getSpecial() { return special; }

		private AnalyzePrimitive(NodeInst ni, HashMap negatedConnections)
		{
			// cell instances are easy
			int special = BLOCKNORMAL;
			if (ni.getProto() instanceof Cell) { primName = ni.getProto().getName();   return; }
		
			// get the primitive function
			PrimitiveNode.Function k = ni.getFunction();
			ArcInst ai = null;
			primName = null;
			if (k == PrimitiveNode.Function.TRANMOS || k == PrimitiveNode.Function.TRA4NMOS)
			{
				primName = "nMOStran";
				Variable var = ni.getVar(Simulation.WEAK_NODE_KEY);
				if (var != null) primName = "nMOStranWeak";
				special = BLOCKMOSTRAN;
			} else if (k == PrimitiveNode.Function.TRADMOS || k == PrimitiveNode.Function.TRA4DMOS)
			{
				primName = "DMOStran";
				special = BLOCKMOSTRAN;
			} else if (k == PrimitiveNode.Function.TRAPMOS || k == PrimitiveNode.Function.TRA4PMOS)
			{
				primName = "PMOStran";
				Variable var = ni.getVar(Simulation.WEAK_NODE_KEY);
				if (var != null) primName = "PMOStranWeak";
				special = BLOCKMOSTRAN;
			} else if (k == PrimitiveNode.Function.TRANPN || k == PrimitiveNode.Function.TRA4NPN)
			{
				primName = "NPNtran";
			} else if (k == PrimitiveNode.Function.TRAPNP || k == PrimitiveNode.Function.TRA4PNP)
			{
				primName = "PNPtran";
			} else if (k == PrimitiveNode.Function.TRANJFET || k == PrimitiveNode.Function.TRA4NJFET)
			{
				primName = "NJFET";
			} else if (k == PrimitiveNode.Function.TRAPJFET || k == PrimitiveNode.Function.TRA4PJFET)
			{
				primName = "PJFET";
			} else if (k == PrimitiveNode.Function.TRADMES || k == PrimitiveNode.Function.TRA4DMES)
			{
				primName = "DMEStran";
			} else if (k == PrimitiveNode.Function.TRAEMES || k == PrimitiveNode.Function.TRA4EMES)
			{
				primName = "EMEStran";
			} else if (k == PrimitiveNode.Function.FLIPFLOPRSMS || k == PrimitiveNode.Function.FLIPFLOPRSN || k == PrimitiveNode.Function.FLIPFLOPRSP)
			{
				primName = "rsff";
				special = BLOCKFLOP;
			} else if (k == PrimitiveNode.Function.FLIPFLOPJKMS || k == PrimitiveNode.Function.FLIPFLOPJKN || k == PrimitiveNode.Function.FLIPFLOPJKP)
			{
				primName = "jkff";
				special = BLOCKFLOP;
			} else if (k == PrimitiveNode.Function.FLIPFLOPDMS || k == PrimitiveNode.Function.FLIPFLOPDN || k == PrimitiveNode.Function.FLIPFLOPDP)
			{
				primName = "dsff";
				special = BLOCKFLOPDS;
				for(Iterator it = ni.getConnections(); it.hasNext(); )
				{
					Connection con = (Connection)it.next();
					if (con.getPortInst().getPortProto().getName().equals("clear"))
					{
						primName = "drff";
						special = BLOCKFLOPDR;
						break;
					}
				}
			} else if (k == PrimitiveNode.Function.FLIPFLOPTMS || k == PrimitiveNode.Function.FLIPFLOPTN || k == PrimitiveNode.Function.FLIPFLOPTP)
			{
				primName = "tsff";
				special = BLOCKFLOPTS;
				for(Iterator it = ni.getConnections(); it.hasNext(); )
				{
					Connection con = (Connection)it.next();
					if (con.getPortInst().getPortProto().getName().equals("clear"))
					{
						primName = "trff";
						special = BLOCKFLOPTR;
						break;
					}
				}
			} else if (k == PrimitiveNode.Function.BUFFER)
			{
				primName = "buffer/inverter";
//				var = getvalkey((INTBIG)el_curtech, VTECHNOLOGY, VSTRING|VISARRAY, tech_vhdl_names_key);
//				if (var != null) primName = ((CHAR **)var.addr)[ni.proto.primindex-1];
				int slashPos = primName.indexOf('/');
				special = BLOCKBUFFER;
				for(Iterator it = ni.getConnections(); it.hasNext(); )
				{
					Connection con = (Connection)it.next();
					if (!con.getPortInst().getPortProto().getName().equals("y")) continue;
					if (con.isNegated())
					{
						if (slashPos >= 0) primName = primName.substring(slashPos+1);
						special = BLOCKINVERTER;
						negatedConnections.remove(con);
						break;
					}
				}
				if (special == BLOCKBUFFER)
				{
					if (slashPos >= 0) primName = primName.substring(0, slashPos);
				}
			} else if (k == PrimitiveNode.Function.GATEAND)
			{
				primName = "and/nand";
//				var = getvalkey((INTBIG)el_curtech, VTECHNOLOGY, VSTRING|VISARRAY, tech_vhdl_names_key);
//				if (var != null) str = ((CHAR **)var.addr)[ni.proto.primindex-1];
				int slashPos = primName.indexOf('/');
				int inport = 0;
				Connection isneg = null;
				for(Iterator it = ni.getConnections(); it.hasNext(); )
				{
					Connection con = (Connection)it.next();
					if (con.getPortInst().getPortProto().getName().equals("a")) inport++;
					if (!con.getPortInst().getPortProto().getName().equals("y")) continue;
					if (con.isNegated()) isneg = con;
				}
				if (isneg != null)
				{
					if (slashPos >= 0) primName = primName.substring(slashPos+1);
					special = BLOCKNAND;
					negatedConnections.remove(isneg);
				} else
				{
					if (slashPos >= 0) primName = primName.substring(0, slashPos);				
					special = BLOCKPOSLOGIC;
				}
				primName += inport;
			} else if (k == PrimitiveNode.Function.GATEOR)
			{
				primName = "or/nor";
//				var = getvalkey((INTBIG)el_curtech, VTECHNOLOGY, VSTRING|VISARRAY, tech_vhdl_names_key);
//				if (var != null) str = ((CHAR **)var.addr)[ni.proto.primindex-1];
				int slashPos = primName.indexOf('/');
				int inport = 0;
				Connection isneg = null;
				for(Iterator it = ni.getConnections(); it.hasNext(); )
				{
					Connection con = (Connection)it.next();
					if (con.getPortInst().getPortProto().getName().equals("a")) inport++;
					if (!con.getPortInst().getPortProto().getName().equals("y")) continue;
					if (con.isNegated()) isneg = con;
				}
				if (isneg != null)
				{
					if (slashPos >= 0) primName = primName.substring(slashPos+1);
					special = BLOCKNOR;
					negatedConnections.remove(isneg);
				} else
				{
					if (slashPos >= 0) primName = primName.substring(0, slashPos);				
					special = BLOCKPOSLOGIC;
				}
			} else if (k == PrimitiveNode.Function.GATEXOR)
			{
				primName = "xor/xnor";
//				var = getvalkey((INTBIG)el_curtech, VTECHNOLOGY, VSTRING|VISARRAY, tech_vhdl_names_key);
//				if (var != null) str = ((CHAR **)var.addr)[ni.proto.primindex-1];
				int slashPos = primName.indexOf('/');
				int inport = 0;
				Connection isneg = null;
				for(Iterator it = ni.getConnections(); it.hasNext(); )
				{
					Connection con = (Connection)it.next();
					if (con.getPortInst().getPortProto().getName().equals("a")) inport++;
					if (!con.getPortInst().getPortProto().getName().equals("y")) continue;
					if (con.isNegated()) isneg = con;
				}
				if (isneg != null)
				{
					if (slashPos >= 0) primName = primName.substring(slashPos+1);
					special = BLOCKXNOR;
					negatedConnections.remove(isneg);
				} else
				{
					if (slashPos >= 0) primName = primName.substring(0, slashPos);				
					special = BLOCKPOSLOGIC;
				}
			} else if (k == PrimitiveNode.Function.MUX)
			{
				primName = "mux";
//				var = getvalkey((INTBIG)el_curtech, VTECHNOLOGY, VSTRING|VISARRAY, tech_vhdl_names_key);
//				if (var != null) str = ((CHAR **)var.addr)[ni.proto.primindex-1];
				int inport = 0;
				for(Iterator it = ni.getConnections(); it.hasNext(); )
				{
					Connection con = (Connection)it.next();
					if (con.getPortInst().getPortProto().getName().equals("a")) inport++;
				}
				primName += inport;
			} else if (k == PrimitiveNode.Function.CONPOWER)
			{
				primName = "power";
			} else if (k == PrimitiveNode.Function.CONGROUND)
			{
				primName = "ground";
			}
			if (primName != null)
			{
				// if the node has an export with power/ground, make it that
				for(Iterator it = ni.getExports(); it.hasNext(); )
				{
					Export e = (Export)it.next();
					if (e.isPower())
					{
						primName = "power";
						break;
					}
					if (e.isGround())
					{
						primName = "ground";
						break;
					}
				}
			}
		}
	}
	
	/**
	 * Method to add the string "orig" to the infinite string.
	 * If "environment" is not NONODEPROTO, it is the cell in which this signal is
	 * to reside, and if that cell has nodes with this name, the signal must be renamed.
	 */
	private static String vhdl_addstring(String orig, Cell environment)
	{
		// remove all nonVHDL characters while adding to current string
		StringBuffer sb = new StringBuffer();
		boolean nonalnum = false;
		for(int i=0; i<orig.length(); i++)
		{
			char chr = orig.charAt(i);
			if (Character.isLetterOrDigit(chr)) sb.append(chr); else
			{
				sb.append('_');
				nonalnum = true;
			}
		}
	
		// if there were nonalphanumeric characters, this cannot be a VHDL keyword
		if (!nonalnum)
		{
			// check for VHDL keyword clashes
			if (CompileVHDL.vhdl_iskeyword(orig) != null)
			{
				sb.append("NV");
				return sb.toString();
			}
	
			// "bit" isn't a keyword, but the compiler can't handle it
			if (orig.equalsIgnoreCase("bit"))
			{
				sb.append("NV");
				return sb.toString();
			}
		}
	
		// see if there is a name clash
		if (environment != null)
		{
			for(Iterator it = environment.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (!(ni.getProto() instanceof Cell)) continue;
				if (orig.equals(ni.getProto().getName()))
				{
					sb.append("NV");
					break;
				}
			}
		}
		return sb.toString();
	}
	
	/**
	 * Method to add, to the infinite string, VHDL for the ports on instance "ni"
	 * (which is of prototype "np").  If "ni" is NONODEINST, use only prototype information,
	 * otherwise, treat each connection on an isolated port as a separate port.
	 * If "special" is BLOCKMOSTRAN, only list ports that are not electrically connected.
	 * If "special" is BLOCKPOSLOGIC, BLOCKBUFFER or BLOCKINVERTER, only include input
	 *    port "a" and output port "y".
	 * If "special" is BLOCKFLOPTS or BLOCKFLOPDS, only include input ports "i1", "ck", "preset"
	 *    and output port "q".
	 * If "special" is BLOCKFLOPTR or BLOCKFLOPDR, only include input ports "i1", "ck", "clear"
	 *    and output port "q".
	 */
	private static String vhdl_addportlist(NodeInst ni, NodeProto np, int special)
	{
		if (special == BLOCKFLOPTS || special == BLOCKFLOPDS)
		{
			return "i1, ck, preset: in BIT; q: out BIT";
		}
		if (special == BLOCKFLOPTR || special == BLOCKFLOPDR)
		{
			return "i1, ck, clear: in BIT; q: out BIT";
		}
	
		// if this is an icon, use the contents
		if (np instanceof Cell && ((Cell)np).getView() == View.ICON)
		{
			Cell cnp = ((Cell)np).contentsView();
			if (cnp != null) np = cnp;
		}
	
		// flag important ports
		HashSet flaggedPorts = new HashSet();
		for(Iterator it = np.getPorts(); it.hasNext(); )
		{
			PortProto pp = (PortProto)it.next();
			if (special == BLOCKMOSTRAN)
			{
				// ignore ports that are electrically connected to previous ones
				boolean connected = false;
				for(Iterator oIt = np.getPorts(); it.hasNext(); )
				{
					PortProto oPp = (PortProto)oIt.next();
					if (oPp == pp) break;
//					if (opp.network == pp.network) { connected = true;   break; }
				}
				if (connected) { flaggedPorts.add(pp);   continue; }
			}
			if (special == BLOCKPOSLOGIC || special == BLOCKBUFFER || special == BLOCKINVERTER)
			{
				// ignore ports not named "a" or "y"
				if (!pp.getName().equals("a") && !pp.getName().equals("y"))
				{
					flaggedPorts.add(pp);
					continue;
				}
			}
		}
	
		String before = "";
		StringBuffer infstr = new StringBuffer();
		before = vhdl_addtheseports(infstr, ni, np, PortCharacteristic.IN, flaggedPorts, before);
		before = vhdl_addtheseports(infstr, ni, np, PortCharacteristic.OUT, flaggedPorts, before);
		before = vhdl_addtheseports(infstr, ni, np, PortCharacteristic.PWR, flaggedPorts, before);
		before = vhdl_addtheseports(infstr, ni, np, PortCharacteristic.GND, flaggedPorts, before);
		before = vhdl_addtheseports(infstr, ni, np, null, flaggedPorts, before);
		return infstr.toString();
	}
	
	private static String vhdl_addtheseports(StringBuffer infstr, NodeInst ni, NodeProto np, PortCharacteristic bits, HashSet flaggedPorts, String before)
	{
		boolean didsome = false;
		for(Iterator it = np.getPorts(); it.hasNext(); )
		{
			PortProto pp = (PortProto)it.next();
//	#ifdef IGNORE4PORTTRANSISTORS
//			if (np == sch_transistor4prim)
//			{
//				if (namesame(pp.protoname, x_("b")) == 0) continue;
//			}
//	#endif
			if (flaggedPorts.contains(pp)) continue;
			PortCharacteristic ch = pp.getCharacteristic();
			if (bits == null)
			{
				if (ch == PortCharacteristic.IN || ch == PortCharacteristic.OUT ||
					ch == PortCharacteristic.PWR || ch == PortCharacteristic.GND) continue;
			} else
			{
				if (ch != bits) continue;
			}
			if (ni != null)
			{
				// instance: dump names
				Netlist nl = ni.getParent().getUserNetlist();
				int inst = 1;
				for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
				{
					Connection con = (Connection)cIt.next();
					if (con.getPortInst().getPortProto() != pp) continue;
					ArcInst ai = con.getArc();
					int wid = nl.getBusWidth(ai);
					for(int i=0; i<wid; i++)
					{
						infstr.append(before);
						before = ", ";
						Network net = nl.getNetwork(ai, i);
						infstr.append(vhdl_addstring(net.toString(), ni.getParent()));
						if (pp.getBasePort().isIsolated()) infstr.append(inst);
						didsome = true;
					}
					if (!pp.getBasePort().isIsolated()) break;
					inst++;
				}
			} else
			{
				// prototype: dump port information
				Export e = (Export)pp;
				Cell cell = (Cell)np;
				Netlist nl = cell.getUserNetlist();
				int wid = nl.getBusWidth(e);
				for(int i=0; i<wid; i++)
				{
					infstr.append(before);
					before = ", ";
					Network net = nl.getNetwork(e, i);
					infstr.append(vhdl_addstring(net.toString(), cell));
					didsome = true;
				}
			}
		}
		if (didsome)
		{
			if (bits == PortCharacteristic.IN)
			{
				infstr.append(": in BIT");
			} else if (bits == PortCharacteristic.OUT || bits == PortCharacteristic.PWR || bits == PortCharacteristic.GND)
			{
				infstr.append(": out BIT");
			} else
			{
				infstr.append(": inout BIT");
			}
			before = "; ";
		}
		return before;
	}
}
