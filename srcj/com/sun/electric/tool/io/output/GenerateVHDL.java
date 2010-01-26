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
 * the Free Software Foundation; either version 3 of the License, or
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
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.PrefPackage;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.CompileVHDL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * This is the VHDL generation facility.
 */
public class GenerateVHDL extends Topology
{
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

	private static final String NORMALCONTINUATIONSTRING = "    ";
	private static final String COMMENTCONTINUATIONSTRING = "-- ";

    private final VHDLPreferences vp;

    public static class VHDLPreferences extends PrefPackage {
        private static final String KEY_VHDL = "SchematicVHDLStringFor";

        public Map<PrimitiveNode,String> vhdlNames = new HashMap<PrimitiveNode,String>();

        public VHDLPreferences(boolean factory)
        {
            super(factory);

            Preferences techPrefs = getPrefRoot().node(TECH_NODE);
            Schematics schTech = Schematics.tech();
            for (Iterator<PrimitiveNode> it = schTech.getNodes(); it.hasNext(); ) {
                PrimitiveNode pn = it.next();

                String key = KEY_VHDL + pn.getName();
                String factoryVhdl = schTech.getFactoryVHDLNames(pn);
                String vhdl = techPrefs.get(key, factoryVhdl);
                vhdlNames.put(pn, vhdl);
            }
        }

        /**
         * Store annotated option fields of the subclass into the speciefied Preferences subtree.
         * @param prefRoot the root of the Preferences subtree.
         * @param removeDefaults remove from the Preferences subtree options which have factory default value.
         */
        @Override
        public void putPrefs(Preferences prefRoot, boolean removeDefaults) {
            super.putPrefs(prefRoot, removeDefaults);
            Preferences techPrefs = prefRoot.node(TECH_NODE);
            Schematics schTech = Schematics.tech();
            for (Map.Entry<PrimitiveNode,String> e: vhdlNames.entrySet()) {
                PrimitiveNode pn = e.getKey();
                String key = KEY_VHDL + pn.getName();
                String factoryVhdl = schTech.getFactoryVHDLNames(pn);
                String vhdl = e.getValue();
                if (removeDefaults && vhdl.equals(factoryVhdl))
                    techPrefs.remove(key);
                else
                    techPrefs.put(key, vhdl);
            }
        }
    }

    private GenerateVHDL(VHDLPreferences vp) {
        this.vp = vp;
    }

	/**
	 * Method to convert a cell to a list of strings with VHDL in them.
	 * @param cell the Cell to convert.
	 * @return a list of strings with VHDL in them (null on error).
	 */
	public static List<String> convertCell(Cell cell, VHDLPreferences vp)
	{
		// cannot make VHDL for cell with no ports
		if (cell.getNumPorts() == 0)
		{
			System.out.println("Cannot convert " + cell.describe(false) + " to VHDL: it has no ports");
			return null;
		}

		GenerateVHDL out = new GenerateVHDL(vp);
		out.openStringsOutputStream();
		out.setOutputWidth(80, false);
		out.setContinuationString(NORMALCONTINUATIONSTRING);

		// generate the VHDL
		if (out.writeCell(cell, null)) return null;

		// return the array of strings with VHDL
		return out.closeStringsOutputStream();
	}

	/**
	 * Method to start the output.
	 * Writes the header.
	 */
	protected void start()
	{
		setContinuationString(COMMENTCONTINUATIONSTRING);
		writeWidthLimited("-- VHDL automatically generated by the Electric VLSI Design System, version " +
			Version.getVersion() + "\n");
		setContinuationString(NORMALCONTINUATIONSTRING);
	}

	protected void done() {}

	/**
	 * Method to write one level of hierarchy.
	 */
	protected void writeCellTopology(Cell cell, String cellName, CellNetInfo cni, VarContext context, Topology.MyCellInfo info)
	{
		// write the header
		writeWidthLimited("\n");
		setContinuationString(COMMENTCONTINUATIONSTRING);
		writeWidthLimited("-------------------- Cell " + cell.describe(false) + " --------------------\n");
		setContinuationString(NORMALCONTINUATIONSTRING);
		Netlist nl = cni.getNetList();

		// write the entity section
		String properCellName = getSafeCellName(cell.getName());
		writeWidthLimited("entity " + addString(properCellName, null) + " is port(" + addPortList(cni) + ");\n");
		writeWidthLimited("  end " + addString(properCellName, null)  + ";\n");

		// write the "architecture" line
		writeWidthLimited("\n");
		writeWidthLimited("architecture " + addString(properCellName, null)  + "_BODY of " +
			addString(properCellName, null) + " is\n");

		// find all negated arcs
		int instNum = 1;
		Map<ArcInst,Integer> negatedHeads = new HashMap<ArcInst,Integer>();
		Map<ArcInst,Integer> negatedTails = new HashMap<ArcInst,Integer>();
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
			if (ai.getHead().isNegated()) negatedHeads.put(ai, new Integer(instNum++));
			if (ai.getTail().isNegated()) negatedTails.put(ai, new Integer(instNum++));
		}

		// write prototypes for each node; gather information for NAND, NOR, and XNOR
		Set<Integer> multiInputNAND = new HashSet<Integer>();
		Set<Integer> multiInputNOR = new HashSet<Integer>();
		Set<Integer> multiInputNXOR = new HashSet<Integer>();
		boolean gotInverters = false;
		Set<String> cellNamesWritten = new HashSet<String>();
		for(Iterator<Nodable> it = nl.getNodables(); it.hasNext(); )
		{
			Nodable no = it.next();
			AnalyzePrimitive ap = new AnalyzePrimitive(no, negatedHeads, negatedTails, vp);
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
				multiInputNAND.add(new Integer(TextUtils.atoi(pt.substring(4))));
				continue;
			}
			if (special == BLOCKNOR)
			{
				multiInputNOR.add(new Integer(TextUtils.atoi(pt.substring(3))));
				continue;
			}
			if (special == BLOCKXNOR)
			{
				multiInputNXOR.add(new Integer(TextUtils.atoi(pt.substring(4))));
				continue;
			}

			// ignore component with no ports
			if (no.getProto().getNumPorts() == 0) continue;

			pt = getSafeCellName(pt);
			if (cellNamesWritten.contains(pt)) continue;
			cellNamesWritten.add(pt);

			if (no.isCellInstance())
			{
				String parameterizedName = parameterizedName(no, context);
				CellNetInfo subCni = getCellNetInfo(parameterizedName);
				if (subCni == null)
				{
					System.out.println("ERROR: no subcell information for: " + parameterizedName);
					continue;
				}
				writeWidthLimited("  component " + addString(pt, null) + " port(" + addPortList(subCni) + ");\n");
			} else
			{
				writeWidthLimited("  component " + addString(pt, null) + " port(" + addPortListPrim(no, special) + ");\n");
			}
			writeWidthLimited("    end component;\n");
		}

		// write prototype for multi-input NAND, NOR, and XNOR
		for(Integer i : multiInputNAND)
		{
			String compName = "nand" + i;
			cellNamesWritten.add(compName);
			writeWidthLimited("  component " + compName + " port(");
			for(int j=1; j<=i.intValue(); j++)
			{
				if (j > 1) writeWidthLimited(", ");
				writeWidthLimited("a" + j);
			}
			writeWidthLimited(": in BIT; y: out BIT);\n");
			writeWidthLimited("    end component;\n");
		}
		for(Integer i : multiInputNOR)
		{
			String compName = "nor" + i;
			cellNamesWritten.add(compName);
			writeWidthLimited("  component " + compName + " port(");
			for(int j=1; j<=i.intValue(); j++)
			{
				if (j > 1) writeWidthLimited(", ");
				writeWidthLimited("a" + j);
			}
			writeWidthLimited(": in BIT; y: out BIT);\n");
			writeWidthLimited("    end component;\n");
		}
		for(Integer i : multiInputNXOR)
		{
			String compName = "xnor" + i;
			cellNamesWritten.add(compName);
			writeWidthLimited("  component " + compName + " port(");
			for(int j=1; j<=i.intValue(); j++)
			{
				if (j > 1) writeWidthLimited(", ");
				writeWidthLimited("a" + j);
			}
			writeWidthLimited(": in BIT; y: out BIT);\n");
			writeWidthLimited("    end component;\n");
		}

		// write inverter prototype if applicable
		if (negatedHeads.size() > 0 || negatedTails.size() > 0) gotInverters = true;
		if (gotInverters)
		{
			cellNamesWritten.add("inverter");
			writeWidthLimited("  component inverter port(a: in BIT; y: out BIT);\n");
			writeWidthLimited("    end component;\n");
		}

		// write internal signals that were used
		SignalNameLine snl = new SignalNameLine();
		for(Iterator<CellSignal> it = cni.getCellSignals(); it.hasNext(); )
		{
			CellSignal cs = it.next();
			if (cs.getExport() != null) continue;
			if (!cs.getNetwork().getArcs().hasNext()) continue;
			String sigName = addString(cs.getName(), cell);
			snl.addSignalName(sigName);
		}
		for(ArcInst ai : negatedHeads.keySet())
		{
			Integer index = negatedHeads.get(ai);
			String sigName = "PINV" + index.intValue();
			snl.addSignalName(sigName);
		}
		for(ArcInst ai : negatedTails.keySet())
		{
			Integer index = negatedTails.get(ai);
			String sigName = "PINV" + index.intValue();
			snl.addSignalName(sigName);
		}
		snl.finish();

		// write the instances
		writeWidthLimited("\n");
		writeWidthLimited("begin\n");
		for(Iterator<Nodable> it = nl.getNodables(); it.hasNext(); )
		{
			Nodable no = it.next();

			// ignore component with no ports
			if (no.getProto().getNumPorts() == 0) continue;

			int special = BLOCKNORMAL;
			String pt = no.getProto().getName();
			if (!no.isCellInstance())
			{
				AnalyzePrimitive ap = new AnalyzePrimitive(no, negatedHeads, negatedTails, vp);
				pt = ap.getPrimName();
				if (pt == null) continue;
				special = ap.getSpecial();
			}

			String instname = getSafeCellName(no.getName());
			writeWidthLimited("  " + addString(instname, null));

			// make sure the instance name doesn't conflict with a prototype name
			if (cellNamesWritten.contains(instname)) writeWidthLimited("NV");

			if (no.isCellInstance())
			{
				String parameterizedName = parameterizedName(no, context);
				CellNetInfo subCni = getCellNetInfo(parameterizedName);
				if (subCni == null)
				{
					System.out.println("STILL NO SUBCELL INFORMATION FOR "+parameterizedName);
					continue;
				}
				writeWidthLimited(": " + addString(getSafeCellName(pt), null) + " port map(" +
					addRealPortsCell(subCni, no, negatedHeads, negatedTails, nl) + ");\n");
			} else
			{
				writeWidthLimited(": " + addString(getSafeCellName(pt), null) + " port map(" +
					addRealPortsPrim(no, special, negatedHeads, negatedTails, nl) + ");\n");
			}
		}

		// write pseudo-nodes for all negated arcs
		for(ArcInst ai : negatedHeads.keySet())
		{
			Integer index = negatedHeads.get(ai);
			writeWidthLimited("  PSEUDO_INVERT" + index.intValue() + ": inverter port map(");
			Network net = nl.getNetwork(ai, 0);
			if (ai.getHeadPortInst().getPortProto().getBasePort().getCharacteristic() == PortCharacteristic.OUT)
			{
				writeWidthLimited("PINV" + index.intValue() + ", " + addString(net.describe(false), cell));
			} else
			{
				writeWidthLimited(addString(net.describe(false), cell) + ", PINV" + index.intValue());
			}
			writeWidthLimited(");\n");
		}
		for(ArcInst ai : negatedTails.keySet())
		{
			Integer index = negatedTails.get(ai);
			writeWidthLimited("  PSEUDO_INVERT" + index.intValue() + ": inverter port map(");
			Network net = nl.getNetwork(ai, 0);
			if (ai.getTailPortInst().getPortProto().getBasePort().getCharacteristic() == PortCharacteristic.OUT)
			{
				writeWidthLimited("PINV" + index.intValue() + ", " + addString(net.describe(false), cell));
			} else
			{
				writeWidthLimited(addString(net.describe(false), cell) + ", PINV" + index.intValue());
			}
			writeWidthLimited(");\n");
		}

		// finish the cell
		writeWidthLimited("end " + addString(properCellName, null) + "_BODY;\n");
	}

	/****************************** METHODS TO WRITE LIST OF CELL PARAMETERS ******************************/

	/**
	 * Method to write actual signals that connect to a cell instance.
	 * @param cni signal information for the cell being instantiated.
	 * @param no the instance node.
	 * @param negatedHeads map of arcs with negated head ends.
	 * @param negatedTails map of arcs with negated tail ends.
	 * @param nl the Netlist for the Cell containing the instance.
	 * @return a string with the connection signals.
	 */
	private String addRealPortsCell(CellNetInfo cni, Nodable no, Map<ArcInst,Integer> negatedHeads,
		Map<ArcInst,Integer> negatedTails, Netlist nl)
	{
		Cell subCell = (Cell)no.getProto();
		Netlist subNL = cni.getNetList();
		boolean first = false;
		StringBuffer infstr = new StringBuffer();
		for(int pass = 0; pass < 5; pass++)
		{
			for(Iterator<Export> it = subCell.getExports(); it.hasNext(); )
			{
				Export e = it.next();
				if (!matchesPass(e.getCharacteristic(), pass)) continue;

				int exportWidth = subNL.getBusWidth(e);
				for(int i=0; i<exportWidth; i++)
				{
					Network net = nl.getNetwork(no, e, i);

					// get connection
					boolean portNamed = false;
					for(Iterator<Connection> cIt = no.getNodeInst().getConnections(); cIt.hasNext(); )
					{
						Connection con = cIt.next();
						PortProto otherPP = con.getPortInst().getPortProto();
						if (otherPP instanceof Export) otherPP = ((Export)otherPP).getEquivalent();
						if (otherPP == e)
						{
							ArcInst ai = con.getArc();
							if (ai.getProto().getFunction() != ArcProto.Function.NONELEC)
							{
								if (con.isNegated())
								{
									Integer index;
									if (con.getEndIndex() == ArcInst.HEADEND) index = negatedHeads.get(ai); else
										index = negatedTails.get(ai);
									if (index != null)
									{
										if (first) infstr.append(", ");   first = true;
										String sigName = "PINV" + index.intValue();
										infstr.append(sigName);
										continue;
									}
								}
							}
							break;
						}
					}
					if (portNamed) continue;

					// write connection
					String sigName = addString(net.getName(), null);
					if (!net.isExported() && !net.getArcs().hasNext()) sigName = "open";
					if (first) infstr.append(", ");   first = true;
					infstr.append(sigName);
				}
			}
		}
		return infstr.toString();
	}

	/**
	 * Method to write actual signals that connect to a primitive instance.
	 * @param no the primitive node.
	 * @param special a code describing special features of the primitive.
	 * @param negatedHeads map of arcs with negated head ends.
	 * @param negatedTails map of arcs with negated tail ends.
	 * @param nl the Netlist for the Cell containing the instance.
	 * @return a string with the connection signals.
	 */
	private String addRealPortsPrim(Nodable no, int special, Map<ArcInst,Integer> negatedHeads,
		Map<ArcInst,Integer> negatedTails, Netlist nl)
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
				if (np == Schematics.tech().transistor4Node)
				{
					if (pp.getName().equals("b")) continue;
				}
				if (!matchesPass(pp.getCharacteristic(), pass)) continue;

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
					if (!pp.getName().equals("a") && !pp.getName().equals("y")) continue;
				}
				if (special == BLOCKFLOPTS || special == BLOCKFLOPDS)
				{
					// ignore ports not named "i1", "ck", "preset", or "q"
					if (!pp.getName().equals("i1") && !pp.getName().equals("ck") &&
						!pp.getName().equals("preset") && !pp.getName().equals("q")) continue;
				}
				if (special == BLOCKFLOPTR || special == BLOCKFLOPDR)
				{
					// ignore ports not named "i1", "ck", "clear", or "q"
					if (!pp.getName().equals("i1") && !pp.getName().equals("ck") &&
						!pp.getName().equals("clear") && !pp.getName().equals("q")) continue;
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
							Integer index;
							if (con.getEndIndex() == ArcInst.HEADEND) index = negatedHeads.get(ai); else
								index = negatedTails.get(ai);
							if (index != null) sigName = "PINV" + index.intValue();
						}
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
					boolean aka = false;
					if (otherPP instanceof PrimitivePort && pp instanceof PrimitivePort)
					{
						if (((PrimitivePort)otherPP).getTopology() == ((PrimitivePort)pp).getTopology()) aka = true;
					}
					if (otherPP == pp || aka)
					{
						ArcInst ai = con.getArc();
						if (ai.getProto().getFunction() != ArcProto.Function.NONELEC)
						{
							if (con.isNegated())
							{
								Integer index;
								if (con.getEndIndex() == ArcInst.HEADEND) index = negatedHeads.get(ai); else
									index = negatedTails.get(ai);
								if (index != null)
								{
									if (first) infstr.append(", ");   first = true;
									String sigName = "PINV" + index.intValue();
									infstr.append(sigName);
									continue;
								}
							}

							int wid = nl.getBusWidth(ai);
							for(int i=0; i<wid; i++)
							{
								if (first) infstr.append(", ");   first = true;
								Network subNet = nl.getNetwork(ai, i);
								String subNetName = getOneNetworkName(subNet);
								String sigName = addString(subNetName, no.getParent());
								infstr.append(sigName);
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
						int wid = nl.getBusWidth(e);
						for(int i=0; i<wid; i++)
						{
							if (first) infstr.append(", ");   first = true;
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
			}
		}
		return infstr.toString();
	}

	/**
	 * Method to return a list of signals connected to a primitive.
	 * @param no the primitive Nodable being written.
	 * @param special special situation for that Nodable.
	 * If "special" is BLOCKPOSLOGIC, BLOCKBUFFER or BLOCKINVERTER, only include input port "a" and output port "y".
	 * If "special" is BLOCKFLOPTS or BLOCKFLOPDS, only include input ports "i1", "ck", "preset" and output port "q".
	 * If "special" is BLOCKFLOPTR or BLOCKFLOPDR, only include input ports "i1", "ck", "clear" and output port "q".
	 */
	private String addPortListPrim(Nodable no, int special)
	{
		// emit special flip-flop ports
		if (special == BLOCKFLOPTS || special == BLOCKFLOPDS)
			return "i1, ck, preset: in BIT; q: out BIT";
		if (special == BLOCKFLOPTR || special == BLOCKFLOPDR)
			return "i1, ck, clear: in BIT; q: out BIT";

		String before = "";
		StringBuffer infstr = new StringBuffer();
		PrimitiveNode pnp = (PrimitiveNode)no.getProto();
		for(int pass = 0; pass < 5; pass++)
		{
			boolean didsome = false;
			for(Iterator<PrimitivePort> it = pnp.getPrimitivePorts(); it.hasNext(); )
			{
				PrimitivePort pp = it.next();
				if (!matchesPass(pp.getCharacteristic(), pass)) continue;
				String portName = pp.getName();
				if (special == BLOCKPOSLOGIC || special == BLOCKBUFFER || special == BLOCKINVERTER)
				{
					// ignore ports not named "a" or "y"
					if (!portName.equals("a") && !portName.equals("y")) continue;
				}
				if (pp.getBasePort().isIsolated())
				{
					int inst = 1;
					for(Iterator<Connection> cIt = no.getNodeInst().getConnections(); cIt.hasNext(); )
					{
						Connection con = cIt.next();
						if (con.getPortInst().getPortProto() != pp) continue;
						infstr.append(before);   before = ", ";
						String exportName = addString(portName, null) + (inst++);
						infstr.append(exportName);
					}
				} else
				{
					infstr.append(before);   before = ", ";
					infstr.append(addString(portName, null));
				}
				didsome = true;
			}
			if (didsome)
			{
				if (pass == 0)
				{
					infstr.append(": in BIT");
				} else if (pass == 1 || pass == 2 || pass == 3)
				{
					infstr.append(": out BIT");
				} else
				{
					infstr.append(": inout BIT");
				}
				before = "; ";
			}
		}
		return infstr.toString();
	}

	/**
	 * Method to construct a list of export names for a cell.
	 * @param cni the cell information.
	 * @return a list of export names for the Cell.
	 */
	private String addPortList(CellNetInfo cni)
	{
		String before = "";
		StringBuffer infstr = new StringBuffer();
		for(int pass = 0; pass < 5; pass++)
		{
			boolean didsome = false;
			for(Iterator<CellSignal> it = cni.getCellSignals(); it.hasNext(); )
			{
				CellSignal cs = it.next();
				Export e = cs.getExport();
				if (e == null) continue;
				if (!matchesPass(e.getCharacteristic(), pass)) continue;
				infstr.append(before);   before = ", ";
				infstr.append(addString(cs.getName(), null));
				didsome = true;
			}
			if (didsome)
			{
				if (pass == 0)
				{
					infstr.append(": in BIT");
				} else if (pass == 1 || pass == 2 || pass == 3)
				{
					infstr.append(": out BIT");
				} else
				{
					infstr.append(": inout BIT");
				}
				before = "; ";
			}
		}
		return infstr.toString();
	}

	/****************************** SUPPORT ******************************/

	/**
	 * Method to determine whether a type of export goes in a particular pass of output.
	 * Ports are written in 5 passes: input, output, power, ground, and everything else.
	 * @param ch the PortCharacteristic of the port.
	 * @param pass the pass number (0-4).
	 * @return true of the given type of port goes in the given pass.
	 */
	private boolean matchesPass(PortCharacteristic ch, int pass)
	{
		switch (pass)
		{
			case 0:			// must be an input port
				return ch == PortCharacteristic.IN;
			case 1:			// must be an output port
				return ch == PortCharacteristic.OUT;
			case 2:			// must be a power port
				return ch == PortCharacteristic.PWR;
			case 3:			// must be a ground port
				return ch == PortCharacteristic.GND;
		}
		return ch != PortCharacteristic.IN && ch != PortCharacteristic.OUT &&
			ch != PortCharacteristic.PWR && ch != PortCharacteristic.GND;
	}

	/**
	 * Method to return a single name for a Network.
	 * Choose the first if there are more than one.
	 * @param net the Network to name.
	 * @return the name of the Network.
	 */
	private String getOneNetworkName(Network net)
	{
		Iterator<String> nIt = net.getNames();
		if (nIt.hasNext()) return nIt.next();
		return net.describe(false);
	}

	/**
	 * Class to determine the VHDL name and special factors for a node.
	 */
	private static class AnalyzePrimitive
	{
		private String primName;
		private int special;

		/**
		 * Method to get the name of this analyzed primitive node.
		 * @return the name of this analyzed primitive node.
		 */
		private String getPrimName() { return primName; }

		/**
		 * Method to return the special code for this analyzed primitive node:
		 * @return the special code for the analyzed primitive node:<BR>
		 * BLOCKNORMAL: no special port arrangements necessary.<BR>
		 * BLOCKMOSTRAN: only output ports that are not electrically connected.<BR>
		 * BLOCKBUFFER: only include input port "a" and output port "y".<BR>
		 * BLOCKPOSLOGIC: only include input port "a" and output port "y".<BR>
		 * BLOCKINVERTER: only include input port "a" and output port "y".<BR>
		 * BLOCKNAND: only include input port "a" and output port "y".<BR>
		 * BLOCKNOR: only include input port "a" and output port "y".<BR>
		 * BLOCKXNOR: only include input port "a" and output port "y".<BR>
		 * BLOCKFLOPTS: only include input ports "i1", "ck", "preset" and output port "q".<BR>
		 * BLOCKFLOPTR: only include input ports "i1", "ck", "clear" and output port "q".<BR>
		 * BLOCKFLOPDS: only include input ports "i1", "ck", "preset" and output port "q".<BR>
		 * BLOCKFLOPDR: only include input ports "i1", "ck", "clear" and output port "q".<BR>
		 * BLOCKFLOP: include input ports "i1", "i2", "ck", "preset", "clear", and output ports "q" and "qb".
		 */
		private int getSpecial() { return special; }

		/**
		 * Constructor which analyzes a primitive node.
		 * @param no the primitive node.
		 * @param negatedHeads map of arcs with negated head ends.
		 * @param negatedTails map of arcs with negated tail ends.
		 */
		private AnalyzePrimitive(Nodable no, Map<ArcInst,Integer> negatedHeads, Map<ArcInst,Integer> negatedTails, VHDLPreferences vp)
		{
			// cell instances are easy
			special = BLOCKNORMAL;
			if (no.isCellInstance()) { primName = no.getProto().getName();   return; }
			NodeInst ni = no.getNodeInst();

			// get the primitive function
			PrimitiveNode.Function k = ni.getFunction();
			primName = null;
			if (k == PrimitiveNode.Function.TRADMOS || k == PrimitiveNode.Function.TRA4DMOS)
			{
				primName = "DMOStran";
				special = BLOCKMOSTRAN;
			} else if (k.isNTypeTransistor())
			{
				primName = "nMOStran";
				Variable var = no.getVar(Simulation.WEAK_NODE_KEY);
				if (var != null) primName = "nMOStranWeak";
				special = BLOCKMOSTRAN;
			} else if (k.isPTypeTransistor())
			{
				primName = "PMOStran";
				Variable var = no.getVar(Simulation.WEAK_NODE_KEY);
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
				primName = vp.vhdlNames.get(Schematics.tech().bufferNode);
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
						if (con.getEndIndex() == ArcInst.HEADEND) negatedHeads.remove(con.getArc()); else
							negatedTails.remove(con.getArc());
						break;
					}
				}
				if (special == BLOCKBUFFER)
				{
					if (slashPos >= 0) primName = primName.substring(0, slashPos);
				}
			} else if (k == PrimitiveNode.Function.GATEAND)
			{
				primName = vp.vhdlNames.get(Schematics.tech().andNode);
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
					if (isNeg.getEndIndex() == ArcInst.HEADEND) negatedHeads.remove(isNeg.getArc()); else
						negatedTails.remove(isNeg.getArc());
				} else
				{
					if (slashPos >= 0) primName = primName.substring(0, slashPos);
					special = BLOCKPOSLOGIC;
				}
				primName += inPort;
			} else if (k == PrimitiveNode.Function.GATEOR)
			{
				primName = vp.vhdlNames.get(Schematics.tech().orNode);
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
					if (isNeg.getEndIndex() == ArcInst.HEADEND) negatedHeads.remove(isNeg.getArc()); else
						negatedTails.remove(isNeg.getArc());
				} else
				{
					if (slashPos >= 0) primName = primName.substring(0, slashPos);
					special = BLOCKPOSLOGIC;
				}
				primName += inPort;
			} else if (k == PrimitiveNode.Function.GATEXOR)
			{
				primName = vp.vhdlNames.get(Schematics.tech().xorNode);
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
					if (isNeg.getEndIndex() == ArcInst.HEADEND) negatedHeads.remove(isNeg.getArc()); else
						negatedTails.remove(isNeg.getArc());
				} else
				{
					if (slashPos >= 0) primName = primName.substring(0, slashPos);
					special = BLOCKPOSLOGIC;
				}
				primName += inPort;
			} else if (k == PrimitiveNode.Function.MUX)
			{
				primName = vp.vhdlNames.get(Schematics.tech().muxNode);
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
	 * Class to aggregate signal names and write them on length-limited lines.
	 */
	private class SignalNameLine
	{
		private boolean hasContent = false;

		public void addSignalName(String sigName)
		{
			if (!hasContent)
			{
				writeWidthLimited("\n");
				writeWidthLimited("  signal ");
			} else
			{
				writeWidthLimited(", ");
			}
			hasContent = true;
			writeWidthLimited(sigName);
		}

		public void finish()
		{
			if (hasContent)
				writeWidthLimited(": BIT;\n");
		}
	}

	/****************************** SUBCLASSED METHODS FOR THE TOPOLOGY ANALYZER ******************************/

	/**
	 * Method to adjust a cell name to be safe for Verilog output.
	 * @param name the cell name.
	 * @return the name, adjusted for Verilog output.
	 */
	protected String getSafeCellName(String name)
	{
		if (name.length() == 0) return name;
		char first = name.charAt(0);
		if (!Character.isLetter(first)) name = "E_" + name;
		for(int i=0; i<name.length(); i++)
		{
			char ch = name.charAt(i);
			if (Character.isLetterOrDigit(ch)) continue;
			if (ch == '_') continue;
			name = name.substring(0, i) + "_" + name.substring(i+1);
		}
		return name;
	}

	/** Method to tell the netlister to deal with all Cells. */
	protected boolean skipCellAndSubcells(Cell cell) { return false; }

	/** Method to return the proper name of Power (just use whatever name is there) */
	protected String getPowerName(Network net) { return net.getName(); }

	/** Method to return the proper name of Ground (just use whatever name is there) */
	protected String getGroundName(Network net) { return net.getName(); }

	/** Method to return the proper name of a Global signal */
	protected String getGlobalName(Global glob) { return "glbl." + glob.getName(); }

	/**
	 * Method to tell the netlister that export names DO take precedence over
	 * arc names when determining the name of the network.
	 */
	protected boolean isNetworksUseExportedNames() { return true; }

	/** Method to report that library names are not always prepended to cell names. */
	protected boolean isLibraryNameAlwaysAddedToCellName() { return false; }

	/** Method to report that aggregate names (busses) are not used. */
	protected boolean isAggregateNamesSupported() { return false; }

	/** Method to decide whether aggregate names (busses) can have gaps in their ranges. */
	protected boolean isAggregateNameGapsSupported() { return false; }

	/** Method to tell netlister not to separate input and output names (this module does more detailed separation). */
	protected boolean isSeparateInputAndOutput() { return false; }

	/** Method to tell the netlister to be case-sensitive. */
	protected boolean isCaseSensitive() { return true; }

	/** Method to tell the netlister how to short resistors */
	protected Netlist.ShortResistors getShortResistors() { return Netlist.ShortResistors.ALL; }

	/** Method to tell the netlister to mangle cell names that are parameterized. */
	protected boolean canParameterizeNames() { return true; }

	/**
	 * Method to adjust a network name to be safe for VHDL output.
	 */
	protected String getSafeNetName(String name, boolean bus)
	{
		return addString(name, null);
	}

	/**
	 * Method to add the string "orig" to the infinite string.
	 * If "environment" is not NONODEPROTO, it is the cell in which this signal is
	 * to reside, and if that cell has nodes with this name, the signal must be renamed.
	 */
	private String addString(String orig, Cell environment)
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
				sb.append('_');
				return sb.toString();
			}

			// "bit" isn't a keyword, but the compiler can't handle it
			if (orig.equalsIgnoreCase("bit"))
			{
				sb.append('_');
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
					sb.append('_');
					break;
				}
			}
		}
		return sb.toString();
	}
}
