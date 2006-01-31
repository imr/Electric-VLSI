/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Generate.java
 * Generate VHDL from a circuit
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.simulation.Simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * This is the VHDL generation facility.
 */
public class GenerateVHDL
{
	private static final int MAXINPUTS = 30;

	/** special codes during VHDL generation */
	/** ordinary block */			private static final int BLOCKNORMAL   = 0;
	/** a MOS transistor */			private static final int BLOCKMOSTRAN  = 1;
	/** a buffer */					private static final int BLOCKBUFFER   = 2;
	/** an and, or, xor */			private static final int BLOCKPOSLOGIC = 3;
	/** an inverter */				private static final int BLOCKINVERTER = 4;
	/** a nand */					private static final int BLOCKNAND     = 5;
	/** a nor */					private static final int BLOCKNOR      = 6;
	/** an xnor */					private static final int BLOCKXNOR     = 7;
	/** a settable D flip-flop */	private static final int BLOCKFLOPDS   = 8;
	/** a resettable D flip-flop */	private static final int BLOCKFLOPDR   = 9;
	/** a settable T flip-flop */	private static final int BLOCKFLOPTS  = 10;
	/** a resettable T flip-flop */	private static final int BLOCKFLOPTR  = 11;
	/** a general flip-flop */		private static final int BLOCKFLOP    = 12;

	// make an array to hold the generated VHDL
	static List<String> vhdlStrings;

	/**
	 * Method to ensure that cell "np" and all subcells have VHDL on them.
	 */
	public static List<String> convertCell(Cell cell)
	{
		// cannot make VHDL for cell with no ports
		if (cell.getNumPorts() == 0)
		{
			System.out.println("Cannot convert " + cell + " to VHDL: it has no ports");
			return null;
		}

		// make an array to hold the generated VHDL
		vhdlStrings = new ArrayList<String>();

		// recursively generate the VHDL
		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, new Visitor(cell));
//		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, null, new Visitor(cell));
		return vhdlStrings;
	}

	private static class Visitor extends HierarchyEnumerator.Visitor
	{
		private Cell cell;
		private static HashSet<Cell> seenCells;

		private Visitor(Cell cell)
		{
			this.cell = cell;
			seenCells = new HashSet<Cell>();
		}

		public boolean enterCell(HierarchyEnumerator.CellInfo info)
		{
			Cell cell = info.getCell();
			if (seenCells.contains(cell)) return true;
			seenCells.add(cell);
			generateVHDL(info);
			return true;
		}

		public void exitCell(HierarchyEnumerator.CellInfo info) {}

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) { return true; }
	}

	private static void generateVHDL(HierarchyEnumerator.CellInfo info)
	{
		Cell cell = info.getCell();

		// write the header
		if (vhdlStrings.size() > 0)
		{
			vhdlStrings.add("");
			vhdlStrings.add("");
		}
		vhdlStrings.add("-- VHDL automatically generated from " + cell);
		Netlist nl = info.getNetlist();

		// write the entity section
		HashSet<String> exportNames = new HashSet<String>();
		vhdlStrings.add("entity " + addString(cell.getName(), null) + " is port(" + addPortList(null, cell, nl /*cell.getUserNetlist()*/, 0, exportNames) + ");");
		vhdlStrings.add("  end " + addString(cell.getName(), null)  + ";");

		// now write the "architecture" line
		vhdlStrings.add("");
		vhdlStrings.add("architecture " + addString(cell.getName(), null)  + "_BODY of " +
			addString(cell.getName(), null) + " is");

		// enumerate negated arcs
		int instNum = 1;
		HashMap<Connection,Integer> negatedConnections = new HashMap<Connection,Integer>();
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
			for(int i=0; i<2; i++)
			{
				if (ai.isNegated(i)) negatedConnections.put(ai.getConnection(i), new Integer(instNum++));
			}
		}

		// write prototypes for each node
		int [] gotNand = new int[MAXINPUTS+1];
		int [] gotNor = new int[MAXINPUTS+1];
		int [] gotXNor = new int[MAXINPUTS+1];
		List<String> componentList = new ArrayList<String>();
		boolean gotInverters = false;
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (ni.isIconOfParent()) continue;
			AnalyzePrimitive ap = new AnalyzePrimitive(ni, negatedConnections);
			String pt = ap.getPrimName();
			if (pt == null) continue;
			int special = ap.getSpecial();

			// write only once per prototype
			if (special == BLOCKINVERTER)
			{
				gotInverters = true;
				continue;
			}
			if (special == BLOCKNAND)
			{
				int i = TextUtils.atoi(pt.substring(4));
				if (i <= MAXINPUTS) gotNand[i]++; else
					System.out.println("Cannot handle " + i + "-input NAND, limit is " + MAXINPUTS);
				continue;
			}
			if (special == BLOCKNOR)
			{
				int i = TextUtils.atoi(pt.substring(3));
				if (i <= MAXINPUTS) gotNor[i]++; else
					System.out.println("Cannot handle " + i + "-input NOR, limit is " + MAXINPUTS);
				continue;
			}
			if (special == BLOCKXNOR)
			{
				int i = TextUtils.atoi(pt.substring(4));
				if (i <= MAXINPUTS) gotXNor[i]++; else
					System.out.println("Cannot handle " + i + "-input XNOR, limit is " + MAXINPUTS);
				continue;
			}

			// ignore component with no ports
			if (ni.getProto().getNumPorts() == 0) continue;

			// see if this component is already written to the header
			if (componentList.contains(pt)) continue;

			// new component: add to the list
			componentList.add(pt);

			StringBuffer infstr = new StringBuffer();
			vhdlStrings.add("  component " + addString(pt, null) + " port(" +
				addPortList(ni, ni.getProto(), nl /*ni.getParent().getUserNetlist()*/, special, null) + ");");
			vhdlStrings.add("    end component;");
		}

		// write pseudo-prototype if there are any negated arcs
		if (negatedConnections.size() > 0) gotInverters = true;
		if (gotInverters)
		{
			vhdlStrings.add("  component inverter port(a: in BIT; y: out BIT);");
			vhdlStrings.add("    end component;");
		}
		for(int i=0; i<MAXINPUTS; i++)
		{
			if (gotNand[i] != 0)
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
			if (gotNor[i] != 0)
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
			if (gotXNor[i] != 0)
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

		// write the instances
		HashSet<String> signalNames = new HashSet<String>();
		List<String> bodyStrings = new ArrayList<String>();
		for(Iterator<Nodable> it = nl.getNodables(); it.hasNext(); )
		{
			Nodable no = it.next();
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

			StringBuffer infstr = new StringBuffer();
			infstr.append("  ");
			String instname = no.getName();
			infstr.append(addString(instname, null));

			// make sure the instance name doesn't conflict with a prototype name
			if (componentList.contains(instname)) infstr.append("NV");

			infstr.append(": " + addString(pt, null) + " port map(" + addRealPorts(no, special, negatedConnections, signalNames, nl) + ");");
			bodyStrings.add(infstr.toString());
		}

		// write pseudo-nodes for all negated arcs
		for(Connection con : negatedConnections.keySet())
		{
			Integer index = negatedConnections.get(con);

			StringBuffer invertStr = new StringBuffer();
			invertStr.append("  PSEUDO_INVERT" + index.intValue() + ": inverter port map(");
			Network net = nl.getNetwork(con.getArc(), 0);
			if (con.getPortInst().getPortProto().getBasePort().getCharacteristic() == PortCharacteristic.OUT)
			{
				invertStr.append("PINV" + index.intValue() + ", " + addString(net.describe(false), cell));
			} else
			{
				invertStr.append(addString(net.describe(false), cell) + ", PINV" + index.intValue());
			}
			invertStr.append(");");
			bodyStrings.add(invertStr.toString());
		}

		// write the signals that were used
		vhdlStrings.add("");
		boolean first = false;
		int lineLen = 0;
		StringBuffer infstr = new StringBuffer();
		for(String signalName : signalNames)
		{
			if (exportNames.contains(signalName)) continue;
			if (!first)
			{
				infstr.append("  signal ");
				lineLen = 9;
			} else
			{
				infstr.append(", ");
				lineLen += 2;
			}
			first = true;
			if (lineLen + signalName.length() > 80)
			{
				vhdlStrings.add(infstr.toString());
				infstr = new StringBuffer();
				infstr.append("    ");
				lineLen = 4;
			}
			infstr.append(addString(signalName, cell));
			lineLen += signalName.length();
		}
		if (first)
		{
			infstr.append(": BIT;");
			vhdlStrings.add(infstr.toString());
		}

		// write the body
		vhdlStrings.add("");
		vhdlStrings.add("begin");
		for(String str : bodyStrings)
			vhdlStrings.add(str);
		vhdlStrings.add("end " + addString(cell.getName(), null) + "_BODY;");
	}

	private static String addRealPorts(Nodable no, int special, HashMap<Connection,Integer> negatedConnections,
		HashSet<String> signalNames, Netlist nl)
	{
		NodeProto np = no.getProto();
		boolean first = false;
		StringBuffer infstr = new StringBuffer();
		for(int pass = 0; pass < 5; pass++)
		{
			for(Iterator<PortProto> it = np.getPorts(); it.hasNext(); )
			{
				PortProto pp = it.next();

				// ignore the bias port of 4-port transistors
				if (np == Schematics.tech.transistor4Node)
				{
					if (pp.getName().equals("b")) continue;
				}
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
					boolean connected = false;
					for(Iterator<PortProto> oIt = np.getPorts(); oIt.hasNext(); )
					{
						PrimitivePort oPp = (PrimitivePort)oIt.next();
						if (oPp == pp) break;
						if (oPp.getTopology() == ((PrimitivePort)pp).getTopology()) { connected = true;   break; }
					}
					if (connected) continue;
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
					for(Iterator<Connection> cIt = no.getNodeInst().getConnections(); cIt.hasNext(); )
					{
						Connection con = cIt.next();
						if (con.getPortInst().getPortProto() != pp) continue;
						ArcInst ai = con.getArc();
						ArcProto.Function fun = ai.getProto().getFunction();
						if (fun == ArcProto.Function.NONELEC) continue;
						String sigName = "open";
						Network net = nl.getNetwork(ai, 0);
						if (net != null)
							sigName = addString(net.describe(false), no.getParent());
						if (con.isNegated())
						{
							Integer index = negatedConnections.get(con);
							if (index != null) sigName = "PINV" + index.intValue();
						}
						signalNames.add(sigName);
						if (first) infstr.append(", ");   first = true;
						infstr.append(sigName);
					}
					continue;
				}

				// get connection
				boolean portNamed = false;
				for(Iterator<Connection> cIt = no.getNodeInst().getConnections(); cIt.hasNext(); )
				{
					Connection con = cIt.next();
					PortProto otherPP = con.getPortInst().getPortProto();
					if (otherPP instanceof Export) otherPP = ((Export)otherPP).getEquivalent();
					if (otherPP == pp)
					{
						ArcInst ai = con.getArc();
						if (ai.getProto().getFunction() != ArcProto.Function.NONELEC)
						{
							if (first) infstr.append(", ");   first = true;
							if (con.isNegated())
							{
								Integer index = negatedConnections.get(con);
								if (index != null)
								{
									String sigName = "PINV" + index.intValue();
									infstr.append(sigName);
									signalNames.add(sigName);
									continue;
								}
							}

							int wid = nl.getBusWidth(ai);
							for(int i=0; i<wid; i++)
							{
								if (i != 0) infstr.append(", ");
								Network subNet = nl.getNetwork(ai, i);
								String subNetName = getOneNetworkName(subNet);
								String sigName = addString(subNetName, no.getParent());
								infstr.append(sigName);
								signalNames.add(sigName);
							}
							portNamed = true;
						}
						break;
					}
				}
				if (portNamed) continue;

				for(Iterator<Export> eIt = no.getNodeInst().getExports(); eIt.hasNext(); )
				{
					Export e = eIt.next();
					PortProto otherPP = e.getOriginalPort().getPortProto();
					if (otherPP instanceof Export) otherPP = ((Export)otherPP).getEquivalent();
					if (otherPP == pp)
					{
						if (first) infstr.append(", ");   first = true;
						int wid = nl.getBusWidth(e);
						for(int i=0; i<wid; i++)
						{
							if (i != 0) infstr.append(", ");
							Network subNet = nl.getNetwork(e, i);
							String subNetName = getOneNetworkName(subNet);
							infstr.append(addString(subNetName, no.getParent()));
						}
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

		private AnalyzePrimitive(NodeInst ni, HashMap<Connection,Integer> negatedConnections)
		{
			// cell instances are easy
			special = BLOCKNORMAL;
			if (ni.isCellInstance()) { primName = ni.getProto().getName();   return; }

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
				for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
				{
					Connection con = it.next();
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
				for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
				{
					Connection con = it.next();
					if (con.getPortInst().getPortProto().getName().equals("clear"))
					{
						primName = "trff";
						special = BLOCKFLOPTR;
						break;
					}
				}
			} else if (k == PrimitiveNode.Function.BUFFER)
			{
				primName = Schematics.getVHDLNames(Schematics.tech.bufferNode);
				int slashPos = primName.indexOf('/');
				special = BLOCKBUFFER;
				for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
				{
					Connection con = it.next();
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
				primName = Schematics.getVHDLNames(Schematics.tech.andNode);
				int slashPos = primName.indexOf('/');
				int inPort = 0;
				Connection isNeg = null;
				for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
				{
					Connection con = it.next();
					if (con.getPortInst().getPortProto().getName().equals("a")) inPort++;
					if (!con.getPortInst().getPortProto().getName().equals("y")) continue;
					if (con.isNegated()) isNeg = con;
				}
				if (isNeg != null)
				{
					if (slashPos >= 0) primName = primName.substring(slashPos+1);
					special = BLOCKNAND;
					negatedConnections.remove(isNeg);
				} else
				{
					if (slashPos >= 0) primName = primName.substring(0, slashPos);
					special = BLOCKPOSLOGIC;
				}
				primName += inPort;
			} else if (k == PrimitiveNode.Function.GATEOR)
			{
				primName = Schematics.getVHDLNames(Schematics.tech.orNode);
				int slashPos = primName.indexOf('/');
				int inPort = 0;
				Connection isNeg = null;
				for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
				{
					Connection con = it.next();
					if (con.getPortInst().getPortProto().getName().equals("a")) inPort++;
					if (!con.getPortInst().getPortProto().getName().equals("y")) continue;
					if (con.isNegated()) isNeg = con;
				}
				if (isNeg != null)
				{
					if (slashPos >= 0) primName = primName.substring(slashPos+1);
					special = BLOCKNOR;
					negatedConnections.remove(isNeg);
				} else
				{
					if (slashPos >= 0) primName = primName.substring(0, slashPos);
					special = BLOCKPOSLOGIC;
				}
				primName += inPort;
			} else if (k == PrimitiveNode.Function.GATEXOR)
			{
				primName = Schematics.getVHDLNames(Schematics.tech.xorNode);
				int slashPos = primName.indexOf('/');
				int inPort = 0;
				Connection isNeg = null;
				for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
				{
					Connection con = it.next();
					if (con.getPortInst().getPortProto().getName().equals("a")) inPort++;
					if (!con.getPortInst().getPortProto().getName().equals("y")) continue;
					if (con.isNegated()) isNeg = con;
				}
				if (isNeg != null)
				{
					if (slashPos >= 0) primName = primName.substring(slashPos+1);
					special = BLOCKXNOR;
					negatedConnections.remove(isNeg);
				} else
				{
					if (slashPos >= 0) primName = primName.substring(0, slashPos);
					special = BLOCKPOSLOGIC;
				}
				primName += inPort;
			} else if (k == PrimitiveNode.Function.MUX)
			{
				primName = Schematics.getVHDLNames(Schematics.tech.muxNode);
				int inPort = 0;
				for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
				{
					Connection con = it.next();
					if (con.getPortInst().getPortProto().getName().equals("a")) inPort++;
				}
				primName += inPort;
			} else if (k == PrimitiveNode.Function.CONPOWER)
			{
				primName = "power";
			} else if (k == PrimitiveNode.Function.CONGROUND)
			{
				primName = "ground";
			}
			if (primName == null)
			{
				// if the node has an export with power/ground, make it that
				for(Iterator<Export> it = ni.getExports(); it.hasNext(); )
				{
					Export e = it.next();
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
	private static String addString(String orig, Cell environment)
	{
		// remove all nonVHDL characters while adding to current string
		StringBuffer sb = new StringBuffer();
		boolean nonAlnum = false;
		for(int i=0; i<orig.length(); i++)
		{
			char chr = orig.charAt(i);
			if (Character.isLetterOrDigit(chr)) sb.append(chr); else
			{
				sb.append('_');
				nonAlnum = true;
			}
		}

		// if there were nonalphanumeric characters, this cannot be a VHDL keyword
		if (!nonAlnum)
		{
			// check for VHDL keyword clashes
			if (CompileVHDL.isKeyword(orig) != null)
			{
				sb.insert(0, "_");
				return sb.toString();
			}

			// "bit" isn't a keyword, but the compiler can't handle it
			if (orig.equalsIgnoreCase("bit"))
			{
				sb.insert(0, "_");
				return sb.toString();
			}
		}

		// see if there is a name clash
		if (environment != null)
		{
			for(Iterator<NodeInst> it = environment.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				if (!ni.isCellInstance()) continue;
				if (orig.equals(ni.getProto().getName()))
				{
					sb.insert(0, "_");
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
	private static String addPortList(NodeInst ni, NodeProto np, Netlist nl, int special, HashSet<String> exportNames)
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

		// get the right netlist for subcells
		if (ni != null && np instanceof Cell) nl = ((Cell)np).acquireUserNetlist();

		// flag important ports
		HashSet<PortProto> flaggedPorts = new HashSet<PortProto>();
		for(Iterator<PortProto> it = np.getPorts(); it.hasNext(); )
		{
			PortProto pp = it.next();
			if (special == BLOCKMOSTRAN)
			{
				// ignore ports that are electrically connected to previous ones
				boolean connected = false;
				for(Iterator<PortProto> oIt = np.getPorts(); it.hasNext(); )
				{
					PortProto oPp = oIt.next();
					if (oPp == pp) break;
					if (((PrimitivePort)oPp).getTopology() == ((PrimitivePort)pp).getTopology()) { connected = true;   break; }
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
		before = addThesePorts(infstr, ni, np, nl, PortCharacteristic.IN, flaggedPorts, exportNames, before);
		before = addThesePorts(infstr, ni, np, nl, PortCharacteristic.OUT, flaggedPorts, exportNames, before);
		before = addThesePorts(infstr, ni, np, nl, PortCharacteristic.PWR, flaggedPorts, exportNames, before);
		before = addThesePorts(infstr, ni, np, nl, PortCharacteristic.GND, flaggedPorts, exportNames, before);
		before = addThesePorts(infstr, ni, np, nl, null, flaggedPorts, exportNames, before);
		return infstr.toString();
	}

	private static String addThesePorts(StringBuffer infstr, NodeInst ni, NodeProto np, Netlist nl,
		PortCharacteristic bits, HashSet flaggedPorts, HashSet<String> exportNames, String before)
	{
		boolean didsome = false;
		HashSet<Network> networksFound = new HashSet<Network>();
		for(Iterator<PortProto> it = np.getPorts(); it.hasNext(); )
		{
			PortProto pp = it.next();
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

			Cell cell = null;
			if (ni != null) cell = ni.getParent(); else
				cell = (Cell)np;
			int wid = 1;
			if (pp instanceof Export) wid = nl.getBusWidth((Export)pp);
			for(int i=0; i<wid; i++)
			{
				String portName = pp.getName();
				if (pp instanceof Export)
				{
					Network net = nl.getNetwork((Export)pp, i);
					if (net != null)
					{
						if (networksFound.contains(net)) continue;
						networksFound.add(net);
						portName = getOneNetworkName(net);
					} else
						System.out.println("Cannot find network for export '" + pp.getName() + "' on " + np);
				}
				if (pp.getBasePort().isIsolated())
				{
					int inst = 1;
					for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
					{
						Connection con = cIt.next();
						if (con.getPortInst().getPortProto() != pp) continue;
						infstr.append(before);   before = ", ";
						String exportName = addString(portName, cell) + (inst++);
						infstr.append(exportName);
						if (exportNames != null) exportNames.add(exportName);
					}
				} else
				{
					infstr.append(before);   before = ", ";
					String exportName = addString(portName, cell);
					infstr.append(exportName);
					if (exportNames != null) exportNames.add(exportName);
				}
				didsome = true;
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

	private static String getOneNetworkName(Network net)
	{
		Iterator<String> nIt = net.getNames();
		if (nIt.hasNext()) return nIt.next();
		return net.describe(false);
	}
}
