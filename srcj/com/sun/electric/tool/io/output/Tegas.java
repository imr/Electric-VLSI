/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Tegas.java
 * Original C Code written by T.J.Goodman, University of Canterbury, N.Z.
 * Translated to Java by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.tool.io.output.Topology.CellNetInfo;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This is the netlister for Tegas.
 */
public class Tegas extends Topology
{
	private static final int MAXLENGTH    = 80;
	private static final int MAXNAMECHARS = 12;

	private HashMap<Nodable,Integer> nodeNames;
	private HashMap<ArcInst,Integer> implicitInverters;
	private Netlist netList;

	/**
	 * The main entry point for Tegas deck writing.
     * @param cell the top-level cell to write.
     * @param context the hierarchical context to the cell.
	 * @param filePath the disk file to create.
	 */
	public static void writeTegasFile(Cell cell, VarContext context, String filePath)
	{
		Tegas out = new Tegas();
		if (out.openTextOutputStream(filePath)) return;
		if (out.writeCell(cell, context)) return;
		if (out.closeTextOutputStream()) return;
		System.out.println(filePath + " written");
	}

	/**
	 * Creates a new instance of the Tegas netlister.
	 */
	Tegas()
	{
	}

	protected void start()
	{
		// parameters to the output-line-length limit and how to break long lines
		setOutputWidth(MAXLENGTH, true);

		writeWidthLimited("/* GENERATED BY THE ELECTRIC VLSI DESIGN SYSTEM */\n\n");
		writeWidthLimited("COMPILE  ;\n\n");
	
		// get library name, check validity
		String libname = getLibraryName(topCell);
	
		int length = libname.length();
		if (length > 12)
		{
			System.out.println("Library name exceeds 12 characters, The name used for the");
			System.out.println("TDL directory will be truncated to :- " + convertName(libname));
		}
	
		// check library name
		String str1 = convertName(libname);
		if (isReservedWord(str1))
		{
			System.out.println(str1 + " IS A RESERVED WORD, RENAME LIBRARY AND RE-RUN");
			return;
		}

		// write "directory line" to file
		writeWidthLimited("DIRECTORY:  " + str1 + " ;\n\n");
	
		writeWidthLimited("OPTIONS:   REPLACE  ;\n\n");
	}

	protected void done()
	{
		writeWidthLimited(" END COMPILE;\n\n ");
	}

	/**
	 * Method to write cellGeom
	 */
	protected void writeCellTopology(Cell cell, CellNetInfo cni, VarContext context, Topology.MyCellInfo info)
	{
		// MODULE
		writeWidthLimited("MODULE:  ");
		writeWidthLimited(convertName(cell.describe(false)));
		writeWidthLimited(";\n\n");

		netList = cni.getNetList();

		// INPUTS
		if (cell.getNumPorts() > 0)
		{
			StringBuffer infstr = new StringBuffer();
			infstr.append("INPUTS:\n");
			for(Iterator<CellSignal> it = cni.getCellSignals(); it.hasNext(); )
			{
				CellSignal cs = (CellSignal)it.next();
				if (!cs.isExported()) continue;
				Export e = cs.getExport();
				if (e.getCharacteristic() != PortCharacteristic.IN) continue;
				if (isReservedWord(convertName(e.getName())))
				{
					System.out.println("ERROR: " + convertName(e.getName()) + " IS A RESERVED WORD");
				}
				infstr.append("   " + convertName(e.getName()) + "\n");
			}
			infstr.append(";\n\n");
			writeWidthLimited(infstr.toString());
	
			// OUTPUTS
			infstr = new StringBuffer();
			infstr.append("OUTPUTS:\n");
				for(Iterator<CellSignal> it = cni.getCellSignals(); it.hasNext(); )
			{
				CellSignal cs = (CellSignal)it.next();
				if (!cs.isExported()) continue;
				Export e = cs.getExport();
				if (e.getCharacteristic() == PortCharacteristic.OUT)
				{
					infstr.append("   " + convertName(e.getName()) + "\n");
				} else if (e.getCharacteristic() != PortCharacteristic.IN)
				{
					System.out.println("EXPORT " + e.getName() + " MUST BE EITHER INPUT OR OUTPUT");
				}
			}
			infstr.append(";\n\n");
			writeWidthLimited(infstr.toString());
		}
	
		// USE
		Set<String> instanceDeclarations = new HashSet<String>();
		Set<String> gateDeclarations = new HashSet<String>();
		for(Iterator<Nodable> it = netList.getNodables(); it.hasNext(); )
		{
			Nodable no = (Nodable)it.next();
			if (no.getProto() instanceof Cell)
			{
				// This case can either be for an existing  user defined module in this
				// directory or for a module from the MASTER directory
				Cell subCell = (Cell)no.getProto();
				String instName = getNodeProtoName(no);
				instanceDeclarations.add(instName + " = " + instName + "///" + getLibraryName(subCell));
				continue;
			}

			NodeInst ni = (NodeInst)no;
			PrimitiveNode.Function fun = ni.getFunction();
			if (fun == PrimitiveNode.Function.GATEAND || fun == PrimitiveNode.Function.GATEOR || fun == PrimitiveNode.Function.GATEXOR)
			{
				// Count number of inputs
				String gateName = getGateName(ni);
				int gateWidth = getGateWidth(ni);
				gateDeclarations.add(gateWidth + "-" + gateName + " = " + gateName + "(" + gateWidth + ",1)");
				continue;
			}
	
			if (fun == PrimitiveNode.Function.SOURCE ||
                fun.isResistor() || // == PrimitiveNode.Function.RESIST ||
                fun.isCapacitor() || // == PrimitiveNode.Function.CAPAC ||
				fun == PrimitiveNode.Function.DIODE || fun == PrimitiveNode.Function.INDUCT || fun == PrimitiveNode.Function.METER)
			{
				System.out.println("CANNOT HANDLE " + ni.getProto().describe(true) + " NODES");
				continue;
			}
		}
		int numDecls = instanceDeclarations.size() + gateDeclarations.size();
		if (numDecls > 0) writeWidthLimited("USE:\n\n");
		int countDecls = 1;
		for(Iterator<String> it = instanceDeclarations.iterator(); it.hasNext(); )
		{
			String decl = (String)it.next();
			if (countDecls < numDecls) decl += ","; else decl += ";";
			countDecls++;
			writeWidthLimited("    " + decl + "\n");
		}
		for(Iterator<String> it = gateDeclarations.iterator(); it.hasNext(); )
		{
			String decl = (String)it.next();
			if (countDecls < numDecls) decl += ","; else decl += ";";
			countDecls++;
			writeWidthLimited("    " + decl + "\n");
		}
		if (numDecls > 0) writeWidthLimited("\n");

		// DEFINE
		writeWidthLimited("DEFINE:\n");

		// count no. of inverters (negated arcs not attached to logic primitives)
		implicitInverters = new HashMap<ArcInst,Integer>();
		int count = 1;
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			for(int i=0; i<2; i++)
			{
				if (!ai.isNegated(i)) continue;
				if (ai.getPortInst(i).getPortProto().getCharacteristic() == PortCharacteristic.OUT)
				{
					PrimitiveNode.Function fun = ai.getPortInst(i).getNodeInst().getFunction();
					if (fun == PrimitiveNode.Function.GATEAND || fun == PrimitiveNode.Function.GATEOR ||
						fun == PrimitiveNode.Function.GATEXOR || fun == PrimitiveNode.Function.BUFFER) continue;
				}
				implicitInverters.put(ai, new Integer(count));
				count++;
			}
		}

		// name every node
		nodeNames = new HashMap<Nodable,Integer>();
		int nodeCount = 1;
		for(Iterator<Nodable> it = netList.getNodables(); it.hasNext(); )
		{
			Nodable no = (Nodable)it.next();
			PrimitiveNode.Function fun = getNodableFunction(no);
			if (fun.isTransistor() || fun == PrimitiveNode.Function.GATEXOR ||
				fun == PrimitiveNode.Function.GATEAND || fun == PrimitiveNode.Function.GATEOR ||
				fun == PrimitiveNode.Function.BUFFER || fun.isFlipFlop() || no.getProto() instanceof Cell)
			{
				nodeNames.put(no, new Integer(count++));
				continue;
			}
		}

		// write the nodes
		boolean wrotePower = false, wroteGround = false;
		for(Iterator<Nodable> it = netList.getNodables(); it.hasNext(); )
		{
			Nodable no = (Nodable)it.next();
			PrimitiveNode.Function fun = getNodableFunction(no);
			if (fun == PrimitiveNode.Function.PIN || fun == PrimitiveNode.Function.ART) continue;
			if (fun == PrimitiveNode.Function.CONPOWER)
			{
				if (wrotePower) continue;
				wrotePower = true;
				writeWidthLimited(getNameOfPower((NodeInst)no));
				continue;
			}
			if (fun == PrimitiveNode.Function.CONGROUND)
			{
				if (wroteGround) continue;
				wroteGround = true;
				writeWidthLimited(getNameOfPower((NodeInst)no));
				continue;
			}
	
			// handle nodeinst descriptions
			if (fun.isTransistor() || fun == PrimitiveNode.Function.GATEXOR ||
				fun == PrimitiveNode.Function.GATEAND || fun == PrimitiveNode.Function.GATEOR ||
				fun == PrimitiveNode.Function.BUFFER || fun.isFlipFlop() || no.getProto() instanceof Cell)
			{
				String str1 = getOutputSignals(no, context, cni) + " = " + getNodeProtoName(no) + getInputSignals(no, context, cni) + getGateDelay(no) + ";\n";
				writeWidthLimited(str1);
				continue;
			}
			System.out.println("NODETYPE " + no.getProto() + " NOT SUPPORTED");
		}
	
		// end module
		writeWidthLimited(" END MODULE;\n\n");
	}

	/****************************** MAIN BLOCKS ******************************/

	/**
	 * Method to write the output signals for a Nodable.
	 * @param no the Nodable to write.
	 * @param context the NodeInst's context down the hierarchy.
	 * @param cni the CellNetInfo for the node's parent cell.
	 * @return the output signals for the Nodable.
	 */
	private String getOutputSignals(Nodable no, VarContext context, CellNetInfo cni)
	{
		int nodeNumber = -1;
		Integer nodeNum = (Integer)nodeNames.get(no);
		if (nodeNum != null) nodeNumber = nodeNum.intValue();
		StringBuffer infstr = new StringBuffer();
		infstr.append("U" + nodeNumber + "(");
		PrimitiveNode.Function fun = getNodableFunction(no);

		boolean needComma = false;
		if (no.getProto() instanceof Cell)
		{
			CellNetInfo subCni = getCellNetInfo(parameterizedName(no, context));
			for(Iterator<CellSignal> it = subCni.getCellSignals(); it.hasNext(); )
			{
				CellSignal subCs = (CellSignal)it.next();
				if (!subCs.isExported()) continue;
				Export e = subCs.getExport();
				if (e.getCharacteristic() == PortCharacteristic.IN) continue;

				if (needComma) infstr.append(","); else needComma = true;
				Network net = netList.getNetwork(no, e, subCs.getExportIndex());
				CellSignal cs = cni.getCellSignal(net);
				infstr.append(convertName(cs.getName()));
			}
		} else
		{
			NodeInst ni = (NodeInst)no;
			for(Iterator<PortProto> it = no.getProto().getPorts(); it.hasNext(); )
			{
				PortProto pp = (PortProto)it.next();
				if (fun.isTransistor())
				{
					if (pp.getName().equals("g")) continue;
				} else
				{
					if (pp.getCharacteristic() == PortCharacteristic.IN) continue;
				}

				for(Iterator<Connection> aIt = ni.getConnections(); aIt.hasNext(); )
				{
					Connection con = (Connection)aIt.next();
					PortInst pi = con.getPortInst();
					if (pi.getPortProto() != pp) continue;
					ArcInst ai = con.getArc();
					if (needComma) infstr.append(","); else needComma = true;
					String nodeName = getConnectionName(con);
					infstr.append(nodeName);

					// if arc on port not negated or a description for an inverter for a negated
					// arc already exists simply write the source name of this port/arc inst.
					if (con.isNegated() && implicitInverters.get(ai) != null)
					{
						// if the negation is at this end (ie nearest this node) write an inverter
						// with the port name of the output as inverter input and the net name of the
						// arc as inverter output. The source name is the net name of the arc
						writeInverter(con, nodeName, "U" + nodeNumber + "." + convertName(pp.getName()));
					}
				}
			}
		}
		infstr.append(")");
		return infstr.toString();
	}

	/**
	 * Method to write the input signals for a Nodable.
	 * @param no the Nodable to write.
	 * @param context the NodeInst's context down the hierarchy.
	 * @param cni the CellNetInfo for the node's parent cell.
	 * @return the input signals for the Nodable.
	 */
	private String getInputSignals(Nodable no, VarContext context, CellNetInfo cni)
	{
		PrimitiveNode.Function fun = getNodableFunction(no);

		if (fun.isFlipFlop())
			return getFlipFlopInputSignals((NodeInst)no);

		if (no.getProto().getNumPorts() == 0) return "";
		StringBuffer infstr = new StringBuffer();
		infstr.append("(");
		boolean first = true;
		if (no.getProto() instanceof Cell)
		{
			CellNetInfo subCni = getCellNetInfo(parameterizedName(no, context));
			for(Iterator<CellSignal> it = subCni.getCellSignals(); it.hasNext(); )
			{
				CellSignal subCs = (CellSignal)it.next();
				if (!subCs.isExported()) continue;
				Export e = subCs.getExport();
				if (e.getCharacteristic() == PortCharacteristic.OUT) continue;

				if (first) first = false; else infstr.append(",");
				Network net = netList.getNetwork(no, e, subCs.getExportIndex());
				CellSignal cs = cni.getCellSignal(net);
				infstr.append(convertName(cs.getName()));
			}
		} else
		{
			NodeInst ni = (NodeInst)no;
			for(Iterator<PortProto> it = no.getProto().getPorts(); it.hasNext(); )
			{
				PrimitivePort pp = (PrimitivePort)it.next();
				if (pp.getCharacteristic() != PortCharacteristic.IN) continue;
	
				// The transistor "s" port is treated as an inport by electric but is used
				// as an outport by TDL
				if (fun.isTransistor() && pp.getName().startsWith("s")) continue;
	
				// Buffer primitive has a "c" port not used by TDL
				if (fun == PrimitiveNode.Function.BUFFER && pp.getName().equals("c")) continue;
	
				boolean portWired = false;
				for(Iterator<Connection> aIt = ni.getConnections(); aIt.hasNext(); )
				{
					Connection con = (Connection)aIt.next();
					if (con.getPortInst().getPortProto() == pp)
					{
						if (first) first = false; else infstr.append(",");
						infstr.append(getInvertedConnectionName(con));
						portWired = true;
	
						// if port is not isolated then write one signal only
						if (!pp.isIsolated()) break;
					}
				}
				if (!portWired)
				{
					System.out.println("UNWIRED PORT " + pp.getName());
					if (first) first = false; else infstr.append(",");
					infstr.append("NC");
				}
			}
		}
		infstr.append(")");
		String result = infstr.toString();
		return result;
	}

	/**
	 * Method to write the input signals for flip flops.
	 * @param ni the flip-flop NodeInst to convert.
	 * @return the input signals for the flip-flop.
	 */
	private String getFlipFlopInputSignals(NodeInst ni)
	{
		String [] signals = new String[5];
		int x = 0;
		for(Iterator<PortProto> it = ni.getProto().getPorts(); it.hasNext(); )
		{
			PortProto pp = (PortProto)it.next();
			if (pp.getCharacteristic() == PortCharacteristic.OUT) continue;
			String ptr = "NC";   // if no-connection write NC

			for(Iterator<Connection> pIt = ni.getConnections(); pIt.hasNext(); )
			{
				Connection con = (Connection)pIt.next();
				if (con.getPortInst().getPortProto() == pp)
				{
					ptr = getInvertedConnectionName(con);
					break;
				}
			}
			for(Iterator<Export> eIt = ni.getExports(); eIt.hasNext(); )
			{
				Export e = (Export)eIt.next();
				if (e.getOriginalPort().getPortProto() == pp)
				{
					ptr = convertName(e.getName());
					break;
				}
			}
			if (x >= 5) break;
			signals[x++] = ptr;
		}

		// We now have the signals in 5 arrays ready to be output in the correct
		// order which is 2,0,1,3,4.If the flip flop is a D or T type don't put
		// out array[1][].
		StringBuffer infstr = new StringBuffer();
		infstr.append("(" + signals[2] + "," + signals[0]);

		// JK and SR have one input more than D or T flip flops
		PrimitiveNode.Function fun = ni.getFunction();
		if (fun == PrimitiveNode.Function.FLIPFLOPRSMS || fun == PrimitiveNode.Function.FLIPFLOPRSP || fun == PrimitiveNode.Function.FLIPFLOPRSN ||
			fun == PrimitiveNode.Function.FLIPFLOPJKMS || fun == PrimitiveNode.Function.FLIPFLOPJKP || fun == PrimitiveNode.Function.FLIPFLOPJKN)
		{
			infstr.append("," + signals[1]);
		}

		infstr.append("," + signals[3] + "," + signals[4] + ")");
		return infstr.toString();
	}

	/****************************** HELPER METHODS ******************************/

	/**
	 * Method to determine whether a nodeinst has an output arc that is negated.
	 * @param ni the NodeInst that may be negated.
	 * @return true if the NodeInst is negated (its output is negated).
	 */
	private boolean isNegatedNode(NodeInst ni)
	{
		for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			PortInst pi = con.getPortInst();
			PortProto pp = pi.getPortProto();
			if (pp.getCharacteristic() != PortCharacteristic.OUT) continue;
			if (con.isNegated()) return true;
		}
		return false;
	}

	/**
	 * Method to return the TDL delay on a Nodable.
	 * @param no the Nodable to get delay information on.
	 * @return the node delay.  If no delay information is on the node,
	 * returns the default: "/1,1/".
	 */
	private String getGateDelay(Nodable no)
	{
		if (no.getProto() instanceof Cell) return "";
		NodeInst ni = (NodeInst)no;
		PrimitiveNode.Function fun = ni.getFunction();

		Variable var = ni.getVar(Simulation.RISE_DELAY_KEY);
		String str1 = "/1,";
		if (var != null) str1 = "/" + var.getPureValue(-1) + ",";
	
		var = ni.getVar(Simulation.FALL_DELAY_KEY);
		String str2 = "/1,";
		if (var != null) str2 = "/" + var.getPureValue(-1) + ",";
		return str1 + str2;
	}

	private int getGateWidth(NodeInst ni)
	{
		int inputs = 0;
		for(Iterator<Connection> iIt = ni.getConnections(); iIt.hasNext(); )
		{
			Connection con = (Connection)iIt.next();
			if (con.getPortInst().getPortProto().getCharacteristic() == PortCharacteristic.IN) inputs++;
		}
		if (inputs < 2)
			System.out.println("MUST HAVE AT LEAST TWO INPUTS ON " + ni);
		return inputs;
	}

	private String getGateName(NodeInst ni)
	{
		// Determine whether node should be NAND, NOR or NXOR
		boolean negated = false;
		if (isNegatedNode(ni)) negated = true;

		// Write USE description for current node
		PrimitiveNode.Function fun = ni.getFunction();
		if (fun == PrimitiveNode.Function.GATEAND)
		{
			if (negated) return "NAND";
			return "AND";
		}
		if (fun == PrimitiveNode.Function.GATEOR)
		{
			if (negated) return "NOR";
			return "OR";
		}
		if (fun == PrimitiveNode.Function.GATEXOR)
		{
			if (negated) return "NXOR";
			return "XOR";
		}
		return "";
	}
	
	/**
	 * Method to write out an inverter description for a negated Connection.
	 * @param con the Connection that is inverted.
	 * @param str1 the output signal name.
	 * @param str2 the input signal name.
	 */
	private void writeInverter(Connection con, String str1, String str2)
	{
		Integer index = (Integer)implicitInverters.get(con.getArc());
		if (index == null) return;
	
		writeWidthLimited("I" + index + "(" + convertName(str1) + ") = NOT(" + convertName(str2) + ");\n");
		implicitInverters.remove(con.getArc());  // write once only
	}

	/**
	 * Method to return the name of a Power/Ground node.
	 * @param ni the Power/Ground node.
	 * @return the node name (returns an empty string if not applicable).
	 */
	private String getNameOfPower(NodeInst ni)
	{
		// To prevent Un-connected power nodes
		if (ni.getNumConnections() == 0)
		{
			System.out.println("PWR / GND NODE UNCONNECTED");
			return "";
		}

		PrimitiveNode.Function fun = ni.getFunction();
		if (fun == PrimitiveNode.Function.CONGROUND || fun == PrimitiveNode.Function.CONPOWER)
		{
			if (ni.getNumConnections() > 0)
			{
				Connection con = (Connection)ni.getConnections().next();
				Network net = netList.getNetwork(con.getArc(), 0);
				String decl = net.describe(false) + " = ";
				if (fun == PrimitiveNode.Function.CONPOWER) decl += "PWR"; else
					decl += "GRND";
				return decl + ";\n";
			}
		}
	
		return "";
	}

	/**
	 * Method to return the name of the network on a given Connection.
	 * @param con the Connection to name.
	 * @return the name of that network.
	 */
	private String getConnectionName(Connection con)
	{
		Network net = netList.getNetwork(con.getArc(), 0);
		if (net != null) return convertName(net.describe(false));
		return "???";
	}

	/**
	 * Method to return the name of the signal on a Connection.
	 * @param con the Connection to name.
	 * @return the name of the Connection network.
	 * Also emits an implicit inverter if there is one.
	 */
	private String getInvertedConnectionName(Connection con)
	{
		String conName = getConnectionName(con);
		ArcInst ai = con.getArc();
		if (!con.isNegated()) return conName;

		// insert an inverter description if a negated arc attached to primitive
		// other than AND,OR,XOR
		Integer index = (Integer)implicitInverters.get(ai);
		if (index != null)
		{
			String str = "I" + index + ".O";
			writeInverter(con, str, conName);
			return str;
		}
		return conName;
	}

	private PrimitiveNode.Function getNodableFunction(Nodable no)
	{
		PrimitiveNode.Function fun = PrimitiveNode.Function.UNKNOWN;
		if (no.getProto() instanceof PrimitiveNode)
		{
			NodeInst ni = (NodeInst)no;
			fun = ni.getFunction();
		}
		return fun;
	}

	/**
	 * Method to return the TDL name of a Nodable.
	 * @param no the Nodable to report.
	 * @return the name of that Nodable.
	 */
	private String getNodeProtoName(Nodable no)
	{
		if (no.getProto() instanceof Cell)
		{
			return convertName(no.getProto().describe(false));
		}
		NodeInst ni = (NodeInst)no;
		PrimitiveNode.Function fun = ni.getFunction();
		if (fun == PrimitiveNode.Function.GATEAND || fun == PrimitiveNode.Function.GATEOR || fun == PrimitiveNode.Function.GATEXOR)
		{
			return getGateWidth(ni) + "-" + getGateName(ni);
		}
		if (fun == PrimitiveNode.Function.BUFFER)
		{
			if (isNegatedNode(ni)) return "NOT";
			return "DELAY";
		}
		if (fun.isFlipFlop()) return getFlipFlopName(ni);
		if (fun == PrimitiveNode.Function.TRANS) return "BDSWITCH";
		return "";
	}

	/**
	 * Method to limit a string to the max 12 chars and in upper case for TDL.
	 * @param str a name.
	 * @return the name in upper case and limited to 12 characters.
	 */
	private String convertName(String str)
	{
		if (str.length() > MAXNAMECHARS) str = str.substring(0, MAXNAMECHARS);
		return str.toUpperCase();
	}

	/**
	 * Method to return the library name to use, given a cell.
	 * @param cell the Cell whose library is desired.
	 * @return the library name of the cell.
	 */
	String getLibraryName(Cell cell)
	{
		return convertName(cell.getLibrary().getName());
	}
	
	/**
	 * Method to return the TDL primitive name of a flip-flop.
	 * @param ni the NodeInst that is a flip-flop.
	 * @return the proper TDL name for that type of flip-flop.
	 */
	private String getFlipFlopName(NodeInst ni)
	{
		PrimitiveNode.Function fun = ni.getFunction();
		if (fun == PrimitiveNode.Function.FLIPFLOPRSMS) return "SRMNE";
		if (fun == PrimitiveNode.Function.FLIPFLOPRSP)  return "SREPE";
		if (fun == PrimitiveNode.Function.FLIPFLOPRSN)  return "SRENE";

		if (fun == PrimitiveNode.Function.FLIPFLOPJKMS) return "JKMNE";
		if (fun == PrimitiveNode.Function.FLIPFLOPJKP)  return "JKEPE";
		if (fun == PrimitiveNode.Function.FLIPFLOPJKN)  return "JKENE";

		if (fun == PrimitiveNode.Function.FLIPFLOPDMS)  return "DMNE";
		if (fun == PrimitiveNode.Function.FLIPFLOPDP)   return "DEPE";
		if (fun == PrimitiveNode.Function.FLIPFLOPDN)   return "DENE";

		if (fun == PrimitiveNode.Function.FLIPFLOPTP || fun == PrimitiveNode.Function.FLIPFLOPTN)
			System.out.println("T TYPE FLIP-FLOP MUST BE MS");
		return "TMNE";
	}

	private List<String> reservedWords = null;

	/**
	 * Method that takes a string of max length 12 and checks to see if it is a reserved word.
	 * @param str the name to check.
	 * @return true if the word is reserved.
	 * Returns false if the word is not reserved.
	 */
	private boolean isReservedWord(String str)
	{
		if (reservedWords == null)
		{
			reservedWords = new ArrayList<String>();
			try
			{
				File f = new File(User.getWorkingDirectory() + File.separator + "reservedwords.dat");
				FileReader fr = new FileReader(f);
				BufferedReader br = new BufferedReader(fr);
				for(;;)
				{
					String line = br.readLine();
					if (line == null) break;
					reservedWords.add(line);
				}
				br.close();
				fr.close();
			} catch (IOException e)
			{
				return false;
			}
		}
		if (str.length() < 2) return false;
		for(Iterator<String> it = reservedWords.iterator(); it.hasNext(); )
		{
			String match = (String)it.next();
			if (match.startsWith(str)) return true;
		}
		return false;
	}

	/****************************** SUBCLASSED METHODS FOR THE TOPOLOGY ANALYZER ******************************/

	/**
	 * Method to adjust a cell name to be safe for Tegas output.
	 * @param name the cell name.
	 * @return the name, adjusted for Tegas output.
	 */
	protected String getSafeCellName(String name) { return name; }

	/** Method to return the proper name of Power */
	protected String getPowerName(Network net) { return ".VDD"; }

	/** Method to return the proper name of Ground */
	protected String getGroundName(Network net) { return "GRND"; }

	/** Method to return the proper name of a Global signal */
	protected String getGlobalName(Global glob) { return glob.getName(); }

	/** Method to report that export names DO take precedence over
	 * arc names when determining the name of the network. */
	protected boolean isNetworksUseExportedNames() { return true; }

	/** Method to report that library names ARE always prepended to cell names. */
	protected boolean isLibraryNameAlwaysAddedToCellName() { return false; }

	/** Method to report that aggregate names (busses) ARE used. */
	protected boolean isAggregateNamesSupported() { return false; }

	/** Method to report whether input and output names are separated. */
	protected boolean isSeparateInputAndOutput() { return true; }

	/**
	 * Method to adjust a network name to be safe for Tegas output.
	 */
	protected String getSafeNetName(String name, boolean bus) { return name; }

    /** Tell the Hierarchy enumerator whether or not to short parasitic resistors */
    protected boolean isShortResistors() { return true; }

    /** Tell the Hierarchy enumerator whether or not to short explicit (poly) resistors */
    protected boolean isShortExplicitResistors() { return true; }

	/**
	 * Method to tell whether the topological analysis should mangle cell names that are parameterized.
	 */
	protected boolean canParameterizeNames() { return true; }

}
