/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Silos.java
 * Input/output tool: Silos netlist output
 * Written by Steven M. Rubin, Sun Microsystems.
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
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.simulation.Simulation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

/**
 * This is the netlister for Silos.
 */
public class Silos extends Topology
{
	/** split lines into 80 characters. */			private static final int MAXSTR =  79;
	/** Maximum macro name length. */				private static final int MAXNAME =  12;

	/** key of Variable holding node name. */		public static final Variable.Key SILOS_NODE_NAME_KEY = Variable.newKey("SIM_silos_node_name");
	/** key of Variable holding global names. */	public static final Variable.Key SILOS_GLOBAL_NAME_KEY = Variable.newKey("SIM_silos_global_name");
	/** key of Variable holding behavior file. */	public static final Variable.Key SILOS_BEHAVIOR_FILE_KEY = Variable.newKey("SIM_silos_behavior_file");
	/** key of Variable holding model. */			public static final Variable.Key SILOS_MODEL_KEY = Variable.newKey("SC_silos");

	/**
	 * The main entry point for Silos deck writing.
     * @param cell the top-level cell to write.
     * @param context the hierarchical context to the cell.
	 * @param filePath the disk file to create.
	 */
	public static void writeSilosFile(Cell cell, VarContext context, String filePath)
	{
		Silos out = new Silos();
		if (out.openTextOutputStream(filePath)) return;
		if (out.writeCell(cell, context)) return;
		if (out.closeTextOutputStream()) return;
		System.out.println(filePath + " written");
	}

	/**
	 * Creates a new instance of the Silos netlister.
	 */
	Silos()
	{
	}

	protected void start()
	{
		// parameters to the output-line-length limit and how to break long lines
		setOutputWidth(MAXSTR, true);
		setCommentChar('$');
		setContinuationString("+");

		// write header information
		writeWidthLimited("\n$ CELL " + topCell.describe(false) +
			" FROM LIBRARY " + topCell.getLibrary().getName() + "\n");
		emitCopyright("$ ", "");
		if (User.isIncludeDateAndVersionInOutput())
		{
			writeWidthLimited("$ CELL CREATED ON " + TextUtils.formatDate(topCell.getCreationDate()) + "\n");
			writeWidthLimited("$ VERSION " + topCell.getVersion() + "\n");
			writeWidthLimited("$ LAST REVISED " + TextUtils.formatDate(topCell.getRevisionDate()) + "\n");
			writeWidthLimited("$ SILOS netlist written by Electric VLSI Design System, version " + Version.getVersion() + "\n");
			writeWidthLimited("$ WRITTEN ON " + TextUtils.formatDate(new Date()) + "\n");
		} else
		{
			writeWidthLimited("$ SILOS netlist written by Electric VLSI Design System\n");
		}

		// First look for any global sources
		for(Iterator<NodeInst> it = topCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.isCellInstance()) continue;	// only real sources

			PrimitiveNode.Function nodetype = ni.getFunction();
			if (nodetype != PrimitiveNode.Function.SOURCE) continue;

			Variable var = ni.getVar(SILOS_GLOBAL_NAME_KEY);
			String name = "";
			if (var != null)	// is this a global source ?
			{
				name = var.getObject().toString();
				writeWidthLimited(".GLOBAL " + convertSpecialNames(name) + "\n");
			}

			// Get the source type
			var = ni.getVar(Spice.SPICE_MODEL_KEY);
			if (var == null)
			{
				System.out.println("Unspecified source:");
				writeWidthLimited("$$$$$ Unspecified source: \n");
			} else	// There is more
			{
				boolean clktype = false;	// Extra data required if variable there
				String msg = var.getObject().toString();
				String line = "";
				char lastChr = 0;
				for(int i=0; i<msg.length(); i++)
				{
					lastChr = msg.charAt(i);
					if (lastChr == '/') break;
					switch (lastChr)
					{
						case 'g':	// a global clock (THIS IS WRONG!!!)
							line = name + " .CLK ";
							clktype = true;
							break;
						case 'h':	// a fixed high source (THIS IS WRONG!!!)
							line = name + " .CLK 0 S1 $ HIGH LEVEL";
							break;
						case 'l':	// a fixed low source (THIS IS WRONG!!!)
							line = name + " .CLK 0 S0 $ LOW LEVEL";
							break;
					}
				}
				if (lastChr == '/' && clktype) line += msg.substring(msg.indexOf('/')+1);
				writeWidthLimited(line + "\n");
			}
		}
	}

	protected void done()
	{
	}

	/**
	 * Method to write cellGeom
	 */
	protected void writeCellTopology(Cell cell, CellNetInfo cni, VarContext context, Topology.MyCellInfo info)
	{
		// Read a behavior file if it is available
		for(Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); )
		{
			Cell oCell = (Cell)it.next();
			Variable var = oCell.getVar(SILOS_BEHAVIOR_FILE_KEY);
			if (var != null)
			{
				String headerPath = TextUtils.getFilePath(cell.getLibrary().getLibFile());
				String fileName = headerPath + var.getObject().toString();
				File test = new File(fileName);
				if (!test.exists())
				{
					System.out.println("Cannot find SILOS behavior file " + fileName + " on " + cell);
				} else
				{
					try
					{
						FileReader fr = new FileReader(test);
						BufferedReader br = new BufferedReader(fr);
						for(;;)
						{
							String line = br.readLine();
							if (line == null) break;
							writeWidthLimited(line + "\n");
						}
						br.close();
						fr.close();
					} catch (IOException e) {}
					return;
				}
			}
		}

		/*
		 * There was no behavior file...
		 * Get the SILOS model from the library if it exists
		 */
		for(Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); )
		{
			Cell oCell = (Cell)it.next();
			Variable var = oCell.getVar(SILOS_MODEL_KEY);
			if (var != null && var.getObject() instanceof String[])
			{
				String [] model = (String [])var.getObject();
				for(int i = 0; i < model.length; i++)
					writeWidthLimited(model[i] + "\n");
				writeWidthLimited("\n");
				return;
			}
		}

		// gather networks in the cell
		Netlist netList = cni.getNetList();

		// write the module header
		if (cell != topCell)
		{
			writeWidthLimited("\n");
			StringBuffer sb = new StringBuffer();
			String name = cni.getParameterizedName();
			if (name.length() > MAXNAME)
			{
				System.out.print(".MACRO name " + name + " is too long;");
				name = name.substring(0, MAXNAME);
				System.out.println(" truncated to " + name);
			}
			writeWidthLimited(".MACRO " + convertName(name));
			for(Iterator<CellAggregateSignal> it = cni.getCellAggregateSignals(); it.hasNext(); )
			{
				CellAggregateSignal cas = (CellAggregateSignal)it.next();
				if (cas.getExport() == null) continue;
				if (cas.isSupply()) continue;
				writeWidthLimited(" " + convertSubscripts(cas.getNameWithIndices()));
			}
			writeWidthLimited("\n");
		} else writeWidthLimited("\n");

		// write the cell instances
		for(Iterator<Nodable> nIt = netList.getNodables(); nIt.hasNext(); )
		{
			Nodable no = (Nodable)nIt.next();
			if (no.isCellInstance())
			{
				String nodeName = parameterizedName(no, context);
				CellNetInfo subCni = getCellNetInfo(nodeName);
				writeWidthLimited("(" + no.getName() + " " + convertName(nodeName));
				for(Iterator<CellAggregateSignal> sIt = subCni.getCellAggregateSignals(); sIt.hasNext(); )
				{
					CellAggregateSignal cas = (CellAggregateSignal)sIt.next();
					if (cas.isSupply()) continue;

					// ignore networks that aren't exported
					PortProto pp = cas.getExport();
					if (pp == null) continue;

					int low = cas.getLowIndex(), high = cas.getHighIndex();
					if (low > high)
					{
						// single signal
						Network net = netList.getNetwork(no, pp, cas.getExportIndex());
						CellSignal cs = cni.getCellSignal(net);
						writeWidthLimited(" " + cs.getName());
					} else
					{
						int total = high - low + 1;
						CellSignal [] outerSignalList = new CellSignal[total];
						for(int j=low; j<=high; j++)
						{
							CellSignal cInnerSig = cas.getSignal(j-low);
							Network net = netList.getNetwork(no, cas.getExport(), cInnerSig.getExportIndex());
							outerSignalList[j-low] = cni.getCellSignal(net);
						}
						writeBus(outerSignalList, low, high, cas.isDescending(),
							cas.getName(), cni.getPowerNet(), cni.getGroundNet());
					}
				}
				writeWidthLimited("\n");
			}
		}

		// write the primitives
		for(Iterator<Nodable> nIt = netList.getNodables(); nIt.hasNext(); )
		{
			Nodable no = (Nodable)nIt.next();

			// not interested in passive nodes (ports electrically connected)
			if (!no.isCellInstance())
			{
				NodeInst ni = (NodeInst)no;
				PrimitiveNode.Function nodeType = getPrimitiveType(ni);
				if (nodeType == PrimitiveNode.Function.UNKNOWN) continue;
				if (nodeType == PrimitiveNode.Function.TRANMOS || nodeType == PrimitiveNode.Function.TRAPMOS)
				{
					// Transistors need a part number
					writeWidthLimited(getNodeInstName(ni));
					writeWidthLimited(" ");
					writeWidthLimited(getPrimitiveName(ni, false));

					// write the names of the port(s)
					for(Iterator<PortProto> pIt = ni.getProto().getPorts(); pIt.hasNext(); )
					{
						PortProto pp = (PortProto)pIt.next();
						writeWidthLimited(getPortProtoName(cell == topCell, null, ni, pp, cell, netList, cni));
					}
					writeWidthLimited("\n");
					continue;
				}
				if (nodeType.isFlipFlop())
				{
					// flip-flops need a part number
					writeWidthLimited(getNodeInstName(ni));
					writeWidthLimited(" ");
					writeWidthLimited(getPrimitiveName(ni, false));

					// write the names of the port(s)
					writeFlipFlop(cell == topCell, ni, cell, nodeType, netList, cni);
					continue;
				}

				if (nodeType == PrimitiveNode.Function.METER || nodeType == PrimitiveNode.Function.SOURCE)
				{
					if (cell != topCell) System.out.println("WARNING: Global Clock in a sub-cell");
					continue;
				}

				if (nodeType == PrimitiveNode.Function.GATEAND || nodeType == PrimitiveNode.Function.GATEOR ||
					nodeType == PrimitiveNode.Function.GATEXOR || nodeType == PrimitiveNode.Function.BUFFER)
				{
					// Gates use their output port as a name
					PortProto outPP = null;
					for(Iterator<PortProto> pIt = ni.getProto().getPorts(); pIt.hasNext(); )
					{
						PortProto pp = (PortProto)pIt.next();
						if (pp.getCharacteristic() == PortCharacteristic.OUT)
						{
							// find the name of the output port
							writeWidthLimited(getPortProtoName(cell == topCell, null, ni, pp, cell, netList, cni));

							// record that we used it
							outPP = pp;

							// determine if this proto is negated
							Connection con = null;
							for(Iterator<Connection> aIt = ni.getConnections(); aIt.hasNext(); )
							{
								Connection c = (Connection)aIt.next();
								if (c.getPortInst().getPortProto() == pp) { con = c;   break; }
							}

							boolean negated = false;
							if (con != null && con.isNegated()) negated = true;
							writeWidthLimited(" " + getPrimitiveName(ni, negated));
							break;
						}
					}
					if (outPP == null)
						System.out.println("Could not find an output connection on " + ni.getProto().getName());

					// get the fall and rise times
					writeWidthLimited(getRiseTime(ni));
					writeWidthLimited(getFallTime(ni));

					// write the rest of the ports only if they're connected
					for(Iterator<Connection> aIt = ni.getConnections(); aIt.hasNext(); )
					{
						Connection con = (Connection)aIt.next();
						PortProto pp = con.getPortInst().getPortProto(); 
						if (pp == outPP) continue;
						writeWidthLimited(getPortProtoName(cell == topCell, con, ni, pp, cell, netList, cni));
					}
					writeWidthLimited("\n");
					continue;
				}

				if (nodeType == PrimitiveNode.Function.CAPAC)
				{
					// find a connected port for the node name
					for(Iterator<Connection> aIt = ni.getConnections(); aIt.hasNext(); )
					{
						Connection con = (Connection)aIt.next();

						// write port name as output
						PortProto pp = con.getPortInst().getPortProto();
						writeWidthLimited(getPortProtoName(cell == topCell, null, ni, pp, cell, netList, cni));
						writeWidthLimited(" " + getPrimitiveName(ni, false));
	
						double j = getCapacitanceInMicroFarads(ni, context);
						if (j >= 0)
						{
							writeWidthLimited(" " + TextUtils.formatDouble(j, 0));
						} else
						{
							System.out.println("Warning: capacitor with no value on " + ni);
						}
						writeWidthLimited("\n");
						break;
					}
					continue;
				}

				if (nodeType == PrimitiveNode.Function.RESIST)
				{
					// sorry! can't handle the resistive gate yet
					continue;
				}
			}
		}
		if (cell != topCell)
			writeWidthLimited(".EOM\n");
	}

	/**
	 * Method to add a bus of signals named "name" to the infinite string "infstr".  If "name" is zero,
	 * do not include the ".NAME()" wrapper.  The signals are in "outerSignalList" and range in index from
	 * "lowindex" to "highindex".  They are described by a bus with characteristic "tempval"
	 * (low bit is on if the bus descends).  Any unconnected networks can be numbered starting at
	 * "*unconnectednet".  The power and grounds nets are "pwrnet" and "gndnet".
	 */
	private void writeBus(CellSignal [] outerSignalList, int lowIndex, int highIndex, boolean descending,
		String name, Network pwrNet, Network gndNet)
	{
		// presume writing the bus as a whole
		boolean breakBus = false;

		// see if all of the nets on this bus are distinct
		int j = lowIndex+1;
		for( ; j<=highIndex; j++)
		{
			CellSignal cs = outerSignalList[j-lowIndex];
			int k = lowIndex;
			for( ; k<j; k++)
			{
				CellSignal oCs = outerSignalList[k-lowIndex];
				if (cs == oCs) break;
			}
			if (k < j) break;
		}
		if (j <= highIndex)
		{
			breakBus = true;
		} else
		{
			// bus entries must have the same root name and go in order
			String lastnetname = null;
			for(j=lowIndex; j<=highIndex; j++)
			{
				CellSignal wl = outerSignalList[j-lowIndex];
				String thisnetname = wl.getName();
				if (wl.getExport() != null)
				{
					if (wl.isDescending())
					{
						if (!descending) break;
					} else
					{
						if (descending) break;
					}
				}

				int openSquare = thisnetname.indexOf('[');
				if (openSquare < 0) break;
				if (j > lowIndex)
				{
					int li = 0;
					for( ; li < lastnetname.length(); li++)
					{
						if (thisnetname.charAt(li) != lastnetname.charAt(li)) break;
						if (lastnetname.charAt(li) == '[') break;
					}
					if (lastnetname.charAt(li) != '[' || thisnetname.charAt(li) != '[') break;
					int thisIndex = TextUtils.atoi(thisnetname.substring(li+1));
					int lastIndex = TextUtils.atoi(lastnetname.substring(li+1));
					if (thisIndex != lastIndex + 1) break;
				}
				lastnetname = thisnetname;
			}
			if (j <= highIndex) breakBus = true;
		}

		writeWidthLimited(" ");
		if (breakBus)
		{
			int start = lowIndex, end = highIndex;
			int order = 1;
			if (descending)
			{
				start = highIndex;
				end = lowIndex;
				order = -1;
			}
			for(int k=start; ; k += order)
			{
				if (k != start)
					writeWidthLimited("-");
				CellSignal cs = outerSignalList[k-lowIndex];
				writeWidthLimited(cs.getName());
				if (k == end) break;
			}
		} else
		{
			CellSignal lastCs = outerSignalList[0];
			String lastNetName = lastCs.getName();
			int openSquare = lastNetName.indexOf('[');
			CellSignal cs = outerSignalList[highIndex-lowIndex];
			String netName = cs.getName();
			int i = netName.indexOf('[');
			if (i < 0) writeWidthLimited(netName); else
			{
				writeWidthLimited(netName.substring(0, i));
				if (descending)
				{
					int first = TextUtils.atoi(netName.substring(i+1));
					int second = TextUtils.atoi(lastNetName.substring(openSquare+1));
					writeWidthLimited("[" + first + ":" + second + "]");
				} else
				{
					int first = TextUtils.atoi(netName.substring(i+1));
					int second = TextUtils.atoi(lastNetName.substring(openSquare+1));
					writeWidthLimited("[" + second + ":" + first + "]");
				}
			}
		}
	}

	/**
	 * Method to return a string describing the SILOS type of nodeinst "ni"
	 * if 'neg' is true, then the negated version is needed
	 */
	private String getPrimitiveName(NodeInst ni, boolean neg)
	{	
		PrimitiveNode.Function f = getPrimitiveType(ni);
		if (f == PrimitiveNode.Function.TRANMOS) return ".NMOS";
		if (f == PrimitiveNode.Function.TRAPMOS) return ".PMOS";

		if (f == PrimitiveNode.Function.BUFFER)
		{
			if (neg) return ".INV";
			return ".BUF";
		}
		if (f == PrimitiveNode.Function.GATEXOR)
		{
			if (neg) return ".XNOR";
			return ".XOR";
		}
		if (f == PrimitiveNode.Function.GATEAND)
		{
			if (neg) return ".NAND";
			return ".NAND";
		}
		if (f == PrimitiveNode.Function.GATEOR)
		{
			if (neg) return ".NOR";
			return ".OR";
		}

		if (f == PrimitiveNode.Function.RESIST) return ".RES";
		if (f == PrimitiveNode.Function.CAPAC) return ".CAP";

		if (f == PrimitiveNode.Function.FLIPFLOPRSMS || f == PrimitiveNode.Function.FLIPFLOPRSP) return ".SRPEFF";
		if (f == PrimitiveNode.Function.FLIPFLOPRSN) return ".SRNEFF";
		if (f == PrimitiveNode.Function.FLIPFLOPJKMS || f == PrimitiveNode.Function.FLIPFLOPJKP) return ".JKPEFF";
		if (f == PrimitiveNode.Function.FLIPFLOPJKN) return ".JKNEFF";
		if (f == PrimitiveNode.Function.FLIPFLOPDMS || f == PrimitiveNode.Function.FLIPFLOPDP) return ".DPEFF";
		if (f == PrimitiveNode.Function.FLIPFLOPDN) return ".DNEFF";
		if (f == PrimitiveNode.Function.FLIPFLOPTMS || f == PrimitiveNode.Function.FLIPFLOPTP) return ".TPEFF";
		if (f == PrimitiveNode.Function.FLIPFLOPTN) return ".TNEFF";

		return convertName(ni.getProto().getName());
	}

	/**
	 * Method to return the SILOS type of a node
	 * Read the contents of the added string, make it available to
	 * the caller
	 */
	private PrimitiveNode.Function getPrimitiveType(NodeInst ni)
	{
		if (ni.isCellInstance()) return null;

		PrimitiveNode.Function func = ni.getFunction();
		if (func == PrimitiveNode.Function.TRAPMOS || func == PrimitiveNode.Function.TRA4PMOS)
			return PrimitiveNode.Function.TRAPMOS;
		if (func == PrimitiveNode.Function.TRANMOS || func == PrimitiveNode.Function.TRA4NMOS)
			return PrimitiveNode.Function.TRANMOS;
		if (func == PrimitiveNode.Function.GATEAND || func == PrimitiveNode.Function.GATEOR ||
			func == PrimitiveNode.Function.GATEXOR || func == PrimitiveNode.Function.BUFFER ||
			func == PrimitiveNode.Function.RESIST || func == PrimitiveNode.Function.CAPAC ||
			func == PrimitiveNode.Function.SOURCE || func == PrimitiveNode.Function.METER ||
			func == PrimitiveNode.Function.FLIPFLOPRSMS || func == PrimitiveNode.Function.FLIPFLOPRSP || func == PrimitiveNode.Function.FLIPFLOPRSN ||
			func == PrimitiveNode.Function.FLIPFLOPJKMS || func == PrimitiveNode.Function.FLIPFLOPJKP || func == PrimitiveNode.Function.FLIPFLOPJKN ||
			func == PrimitiveNode.Function.FLIPFLOPDMS || func == PrimitiveNode.Function.FLIPFLOPDP || func == PrimitiveNode.Function.FLIPFLOPDN ||
			func == PrimitiveNode.Function.FLIPFLOPTMS || func == PrimitiveNode.Function.FLIPFLOPTP || func == PrimitiveNode.Function.FLIPFLOPTN)
				return func;
		return(PrimitiveNode.Function.UNKNOWN);
	}

	/**
	 * Method to return a string describing the SILOS part name of nodeinst
	 * "ni"
	 */
	private String getNodeInstName(NodeInst ni)
	{
		Variable var = ni.getVar(SILOS_NODE_NAME_KEY);
		if (var != null) return var.describe(-1);

		String name = ni.getName();
		if (name.length() > 0)
		{
			if (Character.isLetter(name.charAt(0))) return name;
			if (name.charAt(0) == '[') return convertSubscripts(name);
		}
		return "U" + name;
	}

	/**
	 * Find a name to write for the port prototype, pp, on the node instance, ni
	 * The node instance is located within the prototype, np
	 * If there is an arc connected to the port, use the net name, or NETn.
	 * If there are more than one arc (that are not electrically connected)
	 * on the port, concatenate the names (with spaces between them).
	 * If there is no arc, but the port is an export, use the exported name.
	 * If the port is a power or ground port, ignore it
	 * If this is not the top level cell (ie. a .macro) remove [] notation.
	 */
	private String getPortProtoName(boolean top, Connection con, NodeInst ni, PortProto pp, Cell np, Netlist netList, CellNetInfo cni)
	{
		if (pp.isPower() || pp.isGround()) return "";

		if (con == null)
		{
			for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
			{
				Connection c = (Connection)it.next();
				PortInst pi = c.getPortInst();
				if (pi.getPortProto() != pp) continue;
				con = c;
			}
		}
		boolean negated = false;
		if (con != null && con.isNegated() && pp.getCharacteristic() == PortCharacteristic.IN) negated = true;

		Network net = null;
		if (con != null)
		{
			net = netList.getNetwork(con.getArc(), 0);
		} else
		{
			PortInst pi = ni.findPortInstFromProto(pp);
			net = netList.getNetwork(pi);
		}
		if (net != null)
//		if (net != null && net.hasNames())
		{
			CellSignal cs = cni.getCellSignal(net);
			StringBuffer infstr = new StringBuffer();
			infstr.append(" ");
			if (negated) infstr.append("-");
			infstr.append(cs.getName());
			return infstr.toString();
		}

		// nothing connected to this port...leave a position
		return " .SKIP";
	}

	/**
	 * Method to check if a port of an instance is connected to one of
	 * the ports of the containing instance. If so, get rid of the '[]' format;
	 * replace '[' with '__', ignore ']'.
	 */
	private String adjustPortName(Cell np, String portName)
	{
		if (portName.indexOf('[') < 0) return portName;
		PortProto pp = np.findPortProto(portName);
		if (pp != null) return convertSubscripts(portName);
		return portName;
	}

	/**
	 * Method returns a string containing the rise time, as stored in
	 * the variable SIM_rise_delay on node instance ni.
	 * SIM_rise_delay can be multiple numbers (e.g. "rise_time,fanout")
	 * This function returns a string.
	 * A space is inserted as the first character in the string.
	 * Returns an empty string if no variable found.
	 */
	private String getRiseTime(NodeInst ni)
	{
		Variable var = ni.getVar(Simulation.RISE_DELAY_KEY);
		if (var != null) return var.describe(-1);
		return "";
	}

	/**
	 * Method returns a string containing the fall time, as stored in
	 * the variable SIM_fall_delay on node instance ni.
	 * SIM_fall_delay can be either an integer or a string
	 * (e.g. "fall_time,fanout")
	 * This function returns a string.
	 * A space is inserted as the first character in the string.
	 * Returns an empty string if no variable found.
	 */
	private String getFallTime(NodeInst ni)
	{
		Variable var = ni.getVar(Simulation.FALL_DELAY_KEY);
		if (var != null) return var.describe(-1);
		return "";
	}

	/**
	 * Method to return an integer as the capacitance defined
	 * by "SCHEM_capacitance" variable on an instance of a capacitor.
	 * The returned units are in microfarads (is this right?).
	 * Return -1 if nothing found.
	 */
	private double getCapacitanceInMicroFarads(NodeInst ni, VarContext context)
	{
		Variable var = ni.getVar(Schematics.SCHEM_CAPACITANCE);
		if (var != null)
		{
			String cap = context.evalVar(var).toString();
			char lastChar = 0;
			int len = cap.length();
			if (len > 0) lastChar = cap.charAt(len-1);
			if (lastChar == 'f' || lastChar == 'F') cap = cap.substring(0, len-1);
			double farads = VarContext.objectToDouble(cap, 0.0);
			double microFarads = farads * 1000000.0;
			return microFarads;
		}
		return -1;
	}

	/**
	 * Method to write the ports of a flip-flop;
	 * get them in the Electric order, then rewrite them
	 * 'ni' is the current NODEINST, found in 'np' cell
	 */
	private static final int JORD = 0;
	private static final int K	  = 1;
	private static final int Q	  = 2;
	private static final int QB   = 3;
	private static final int CK	  = 4;
	private static final int PRE  = 5;
	private static final int CLR  = 6;

	private void writeFlipFlop(boolean top, NodeInst ni, Cell np, PrimitiveNode.Function type, Netlist netList, CellNetInfo cni)
	{
		String [] portNames = new String[7];
		Iterator<PortProto> it = ni.getProto().getPorts();
		for(int i=0; i<7; i++)
		{
			if (!it.hasNext()) break;
			PortProto pp = (PortProto)it.next();
			portNames[i] = getPortProtoName(top, null, ni, pp, np, netList, cni);
		}
		if (portNames[PRE].equals(" .SKIP") && portNames[CLR].equals(" .SKIP"))
		{
			portNames[CLR] = "";		// If neither on, don't print
			portNames[PRE] = "";
		}
		if (type == PrimitiveNode.Function.FLIPFLOPDMS)
		{
			writeWidthLimited(portNames[CK] + portNames[JORD] + portNames[PRE] + portNames[CLR] + " /" + portNames[Q]+ portNames[QB]);
		} else
		{
			writeWidthLimited(portNames[CK] + portNames[JORD] + portNames[K] + portNames[PRE] + portNames[CLR] + " /" + portNames[Q] + portNames[QB]);
		}
	}

	/**
	 * Method to convert special names to SILOS format
	 */
	private String convertSpecialNames(String str)
	{
		if (str.equals("vdd")) return ".VDD";
		if (str.equals("vss")) return ".VSS";
		if (str.equals("vcc")) return ".VCC";
		if (str.equals("gnd")) return ".GND";
		if (str.equals("low")) return ".GND";
		if (str.equals("hig")) return ".VDD";
		return str;
	}

	/**
	 * replace subscripted name with __ format
	 */
	private String convertSubscripts(String string)
	{
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<string.length(); i++)
		{
			char chr = string.charAt(i);
			if (chr == '[') { sb.append("__"); continue; }
			if (chr == ']') continue;
			sb.append(chr);
		}
		return sb.toString();
	}

	/**
	 * routine to replace all non-printing characters
	 * in the string "p" with the letter "X" and return the string
	 * We will not permit a digit in the first location; replace it
	 * with '_'
	 */
	private String convertName(String p)
	{
		int len = p.length();
		if (len <= 0) return p;
		boolean defined = true;
		for(int i=0; i<len; i++) if (!Character.isDefined(p.charAt(i))) { defined = false;   break; }
		if (defined && !TextUtils.isDigit(p.charAt(0))) return p;

		StringBuffer sb = new StringBuffer();
		if (TextUtils.isDigit(p.charAt(0))) sb.append("_");
		for(int i=0; i<len; i++)
		{
			char t = p.charAt(i);
			if (Character.isDefined(t)) sb.append(t); else
				sb.append("_");
		}
		return sb.toString();
	}

	/****************************** SUBCLASSED METHODS FOR THE TOPOLOGY ANALYZER ******************************/

	/**
	 * Method to adjust a cell name to be safe for Silos output.
	 * @param name the cell name.
	 * @return the name, adjusted for Silos output.
	 */
	protected String getSafeCellName(String name) { return name; }

	/** Method to return the proper name of Power */
	protected String getPowerName(Network net) { return ".VDD"; }

	/** Method to return the proper name of Ground */
	protected String getGroundName(Network net) { return ".GND"; }

	/** Method to return the proper name of a Global signal */
	protected String getGlobalName(Global glob) { return glob.getName(); }

	/** Method to report that export names DO take precedence over
	 * arc names when determining the name of the network. */
	protected boolean isNetworksUseExportedNames() { return true; }

	/** Method to report that library names ARE always prepended to cell names. */
	protected boolean isLibraryNameAlwaysAddedToCellName() { return false; }

	/** Method to report that aggregate names (busses) ARE used. */
	protected boolean isAggregateNamesSupported() { return true; }

	/** Method to report whether input and output names are separated. */
	protected boolean isSeparateInputAndOutput() { return true; }

	/**
	 * Method to adjust a network name to be safe for Silos output.
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
