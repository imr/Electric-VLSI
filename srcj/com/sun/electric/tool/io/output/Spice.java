/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Spice.java
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

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyMerge;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.io.output.Topology;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Date;

/**
 * This is the Simulation Interface tool.
 */
public class Spice extends Topology
{
	/** key of Variable holding generic Spice templates. */		public static final Variable.Key SPICE_TEMPLATE_KEY = ElectricObject.newKey("ATTR_SPICE_template");
	/** key of Variable holding Spice 2 templates. */			public static final Variable.Key SPICE_2_TEMPLATE_KEY = ElectricObject.newKey("ATTR_SPICE_template_spice2");
	/** key of Variable holding Spice 3 templates. */			public static final Variable.Key SPICE_3_TEMPLATE_KEY = ElectricObject.newKey("ATTR_SPICE_template_spice3");
	/** key of Variable holding HSpice templates. */			public static final Variable.Key SPICE_H_TEMPLATE_KEY = ElectricObject.newKey("ATTR_SPICE_template_hspice");
	/** key of Variable holding PSpice templates. */			public static final Variable.Key SPICE_P_TEMPLATE_KEY = ElectricObject.newKey("ATTR_SPICE_template_pspice");
	/** key of Variable holding GnuCap templates. */			public static final Variable.Key SPICE_GC_TEMPLATE_KEY = ElectricObject.newKey("ATTR_SPICE_template_gnucap");
	/** key of Variable holding Smart Spice templates. */		public static final Variable.Key SPICE_SM_TEMPLATE_KEY = ElectricObject.newKey("ATTR_SPICE_template_smartspice");
	/** key of Variable holding SPICE code. */					public static final Variable.Key SPICE_CARD_KEY = ElectricObject.newKey("SIM_spice_card");
//	/** key of Variable holding SPICE code. */					public static final Variable.Key SPICE_CARD_KEY = ElectricObject.newKey("SPICE_Code");
	/** key of Variable holding SPICE model. */					public static final Variable.Key SPICE_MODEL_KEY = ElectricObject.newKey("SIM_spice_model");

	/** maximum subcircuit name length */						private static final int SPICEMAXLENSUBCKTNAME     = 70;
	/** legal characters in a spice deck */						private static final String SPICELEGALCHARS        = "!#$%*+-/<>[]_";
	/** legal characters in a CDL deck */						private static final String CDLNOBRACKETLEGALCHARS = "!#$%*+-/<>_";

	/** default Technology to use. */				private Technology layoutTechnology;
	/** Mask shrink factor (default =1) */			private double  maskScale;
	/** True to write CDL format */					private boolean useCDL;
	/** Legal characters */							private String legalSpiceChars;
	/** Template Key for current spice engine */	private Variable.Key preferedEgnineTemplateKey;
	/** Spice type: 2, 3, H, P, etc */				private int spiceEngine;

	private static class SpiceNet
	{
		/** network object associated with this */	JNetwork      network;
		/** merged geometry for this network */		PolyMerge     merge;
		/** area of diffusion */					double        diffArea;
		/** perimeter of diffusion */				double        diffPerim;
		/** amount of capacitance in non-diff */	float         nonDiffCapacitance;
		/** number of transistors on the net */		int           transistorCount;
	}

	/**
	 * The main entry point for Spice deck writing.
	 * @param cell the top-level cell to write.
	 * @param filePath the disk file to create with Spice.
	 */
	public static void writeSpiceFile(Cell cell, String filePath, boolean cdl)
	{
		Spice out = new Spice();
		out.useCDL = cdl;
		if (out.openTextOutputStream(filePath)) return;
		if (out.writeCell(cell)) return;
		if (out.closeTextOutputStream()) return;
		System.out.println(filePath + " written");

		// write CDL support file if requested
		if (out.useCDL)
		{
			// write the control files
			String templateFile = cell.getProtoName() + ".cdltemplate";
			if (out.openTextOutputStream(templateFile)) return;

			String deckFile = filePath;
			String deckPath = "";
			int lastDirSep = deckFile.lastIndexOf(File.pathSeparatorChar);
			if (lastDirSep > 0)
			{
				deckPath = deckFile.substring(0, lastDirSep);
				deckFile = deckFile.substring(lastDirSep+1);
			}
			String libName = Simulation.getCDLLibName();
			String libPath = Simulation.getCDLLibPath();
			out.printWriter.print("cdlInKeys = list(nil\n");
			out.printWriter.print("    'searchPath             \"" + deckFile + "");
			if (libPath.length() > 0)
				out.printWriter.print("\n                             " + libPath);
			out.printWriter.print("\"\n");
			out.printWriter.print("    'cdlFile                \"" + deckPath + "\"\n");
			out.printWriter.print("    'userSkillFile          \"\"\n");
			out.printWriter.print("    'opusLib                \"" + libName + "\"\n");
			out.printWriter.print("    'primaryCell            \"" + cell.getProtoName() + "\"\n");
			out.printWriter.print("    'caseSensitivity        \"preserve\"\n");
			out.printWriter.print("    'hierarchy              \"flatten\"\n");
			out.printWriter.print("    'cellTable              \"\"\n");
			out.printWriter.print("    'viewName               \"netlist\"\n");
			out.printWriter.print("    'viewType               \"\"\n");
			out.printWriter.print("    'pr                     nil\n");
			out.printWriter.print("    'skipDevice             nil\n");
			out.printWriter.print("    'schemaLib              \"sample\"\n");
			out.printWriter.print("    'refLib                 \"\"\n");
			out.printWriter.print("    'globalNodeExpand       \"full\"\n");
			out.printWriter.print(")\n");
			if (out.closeTextOutputStream()) return;
			System.out.println(templateFile + " written");
//			ttyputmsg(x_("Now type: exec nino CDLIN %s &"), templatefile);
		}

//		// run spice (if requested)
//		var = getvalkey((INTBIG)sim_tool, VTOOL, VINTEGER, sim_dontrunkey);
//		if (var != NOVARIABLE && var->addr != SIMRUNNO)
//		{
//			ttyputmsg(_("Running SPICE..."));
//			var = getvalkey((INTBIG)sim_tool, VTOOL, VSTRING, sim_spice_listingfilekey);
//			if (var == NOVARIABLE) sim_spice_execute(deckfile, x_(""), np); else
//				sim_spice_execute(deckfile, (CHAR *)var->addr, np);
//		}
	}

	/**
	 * Creates a new instance of Spice
	 */
	Spice()
	{
	}

	protected void start()
	{
		// find the proper technology to use if this is schematics
		layoutTechnology = Schematics.getDefaultSchematicTechnology();

		// make sure key is cached
		spiceEngine = Simulation.getSpiceEngine();
		preferedEgnineTemplateKey = SPICE_TEMPLATE_KEY;
		switch (spiceEngine)
		{
			case Simulation.SPICE_ENGINE_2: preferedEgnineTemplateKey = SPICE_2_TEMPLATE_KEY;   break;
			case Simulation.SPICE_ENGINE_3: preferedEgnineTemplateKey = SPICE_3_TEMPLATE_KEY;   break;
			case Simulation.SPICE_ENGINE_H: preferedEgnineTemplateKey = SPICE_H_TEMPLATE_KEY;   break;
			case Simulation.SPICE_ENGINE_P: preferedEgnineTemplateKey = SPICE_P_TEMPLATE_KEY;   break;
			case Simulation.SPICE_ENGINE_G: preferedEgnineTemplateKey = SPICE_GC_TEMPLATE_KEY;   break;
			case Simulation.SPICE_ENGINE_S: preferedEgnineTemplateKey = SPICE_SM_TEMPLATE_KEY;   break;
		}

		// get the mask scale
		maskScale = 1.0;
//		Variable scaleVar = layoutTechnology.getVar("SIM_spice_mask_scale");
//		if (scaleVar != null) maskScale = TextUtils.atof(scaleVar.getObject().toString());

		// setup the legal characters
		legalSpiceChars = SPICELEGALCHARS;

		// start writing the spice deck
		if (useCDL)
		{
			// setup bracket conversion for CDL
			if (Simulation.isCDLConvertBrackets())
				legalSpiceChars = CDLNOBRACKETLEGALCHARS;

			multiLinePrint(true, "* First line is ignored\n");
		} else
		{
			writeHeader(topCell);
		}

		// gather all global signal names (HSPICE and PSPICE only)
		Netlist netList = getNetlistForCell(topCell);
		Global.Set globals = netList.getGlobals();
		int globalSize = globals.size();
		if (!Simulation.isSpiceUseNodeNames() || spiceEngine != Simulation.SPICE_ENGINE_3)
		{
			if (globalSize > 0)
			{
				StringBuffer infstr = new StringBuffer();
				infstr.append("\n.global");
				for(int i=0; i<globalSize; i++)
				{
					Global global = (Global)globals.get(i);
					infstr.append(" " + global.getName());
				}
				infstr.append("\n");
				multiLinePrint(false, infstr.toString());
			}
		}
	}

	protected void done()
	{
		if (!useCDL)
		{
			writeTrailer(topCell);
			multiLinePrint(false, ".END\n");
		}
	}

	/**
	 * Method to write cellGeom
	 */
	protected void writeCellTopology(Cell cell, CellNetInfo cni, VarContext context)
	{
		// look for a model file on the current cell
		Variable var = cell.getVar("SIM_spice_behave_file");
		if (var != null)
		{
			multiLinePrint(true, "* Cell " + cell.describe() + " is described in this file:\n");
			addIncludeFile(var.getObject().toString());
			return;
		}

		// gather networks in the cell
		Netlist netList = cni.getNetList();

		// make sure power and ground appear at the top level
		if (cell == topCell && !Simulation.isSpiceForceGlobalPwrGnd())
		{
			if (cni.getPowerNet() == null)
				System.out.println("WARNING: cannot find power at top level of circuit");
			if (cni.getGroundNet() == null)
				System.out.println("WARNING: cannot find ground at top level of circuit");
		}

		// create list of electrical nets in this cell
		HashMap spiceNetMap = new HashMap();

		// create SpiceNet objects for all networks in the cell
		for(Iterator it = netList.getNetworks(); it.hasNext(); )
		{
			JNetwork net = (JNetwork)it.next();

			// create a "SpiceNet" for the network
			SpiceNet spNet = new SpiceNet();
			spNet.network = net;
			spNet.transistorCount = 0;
			spNet.diffArea = 0;
			spNet.diffPerim = 0;
			spNet.nonDiffCapacitance = 0;
			spNet.merge = new PolyMerge();
			spiceNetMap.put(net, spNet);
		}

		// count the number of different transistor types
		int bipolarTrans = 0, nmosTrans = 0, pmosTrans = 0;
		for(Iterator aIt = cell.getNodes(); aIt.hasNext(); )
		{
			NodeInst ni = (NodeInst)aIt.next();
			addNodeInformation(netList, spiceNetMap, ni);
			NodeProto.Function fun = ni.getFunction();
			if (fun == NodeProto.Function.TRANPN || fun == NodeProto.Function.TRA4NPN ||
				fun == NodeProto.Function.TRAPNP || fun == NodeProto.Function.TRA4PNP ||
				fun == NodeProto.Function.TRANS) bipolarTrans++; else
			if (fun == NodeProto.Function.TRAEMES || fun == NodeProto.Function.TRA4EMES ||
				fun == NodeProto.Function.TRADMES || fun == NodeProto.Function.TRA4DMES ||
				fun == NodeProto.Function.TRADMOS || fun == NodeProto.Function.TRA4DMOS ||
				fun == NodeProto.Function.TRANMOS || fun == NodeProto.Function.TRA4NMOS) nmosTrans++; else
			if (fun == NodeProto.Function.TRAPMOS || fun == NodeProto.Function.TRA4PMOS) pmosTrans++;
		}

		// accumulate geometry of all arcs
		for(Iterator aIt = cell.getArcs(); aIt.hasNext(); )
		{
			ArcInst ai = (ArcInst)aIt.next();

			// don't count non-electrical arcs
			if (ai.getProto().getFunction() == ArcProto.Function.NONELEC) continue;

			// ignore busses
//			if (ai->network->buswidth > 1) continue;
			JNetwork net = netList.getNetwork(ai, 0);
			SpiceNet spNet = (SpiceNet)spiceNetMap.get(net);
			if (spNet == null) continue;

			addArcInformation(spNet.merge, ai);
		}

		// get merged polygons so far
		for(Iterator it = netList.getNetworks(); it.hasNext(); )
		{
			JNetwork net = (JNetwork)it.next();
			SpiceNet spNet = (SpiceNet)spiceNetMap.get(net);
			for(Iterator lIt = spNet.merge.getLayersUsed(); lIt.hasNext(); )
			{
				Layer layer = (Layer)lIt.next();
				List polyList = spNet.merge.getMergedPoints(layer);
				if (polyList == null) continue;
				for(Iterator pIt = polyList.iterator(); pIt.hasNext(); )
				{
					Poly poly = (Poly)pIt.next();
					Point2D [] pointList = poly.getPoints();
					int count = pointList.length;

					// compute perimeter and area
					double perim = 0;
					for(int i=0; i<count; i++)
					{
						int j = i - 1;
						if (j < 0) j = count - 1;
						perim += pointList[i].distance(pointList[j]);
					}
					double area = Poly.areaPoints(pointList);

					// accumulate this information
					if (layerIsDiff(layer))
					{
						spNet.diffArea += area * maskScale * maskScale;
						spNet.diffPerim += perim * maskScale;
					} else
					{
						spNet.nonDiffCapacitance += layer.getCapacitance() * area * maskScale * maskScale;
						spNet.nonDiffCapacitance += layer.getEdgeCapacitance() * perim * maskScale;
					}
				}
			}		
		}

		// make sure the ground net is number zero
		JNetwork groundNet = cni.getGroundNet();
		JNetwork powerNet = cni.getPowerNet();
		if (pmosTrans != 0 && powerNet == null)
		{
			String message = "WARNING: no power connection for P-transistor wells in cell " + cell.describe();
			dumpErrorMessage(message);
		}
		if (nmosTrans != 0 && groundNet == null)
		{
			String message = "WARNING: no ground connection for N-transistor wells in cell " + cell.describe();
			dumpErrorMessage(message);
		}

//		// use ground net for substrate
//		if (subnet == NOSPNET && sim_spice_gnd != NONETWORK)
//			subnet = (SPNET *)sim_spice_gnd->temp1;
//		if (bipolarTrans != 0 && subnet == NOSPNET)
//		{
//			infstr = initinfstr();
//			formatinfstr(infstr, _("WARNING: no explicit connection to the substrate in cell %s"),
//				describenodeproto(np));
//			dumpErrorMessage(infstr);
//			if (sim_spice_gnd != NONETWORK)
//			{
//				ttyputmsg(_("     A connection to ground will be used if necessary."));
//				subnet = (SPNET *)sim_spice_gnd->temp1;
//			}
//		}

		// generate header for subckt or top-level cell
		if (cell == topCell && !useCDL)
		{
			multiLinePrint(true, "\n*** TOP LEVEL CELL: " + cell.describe() + "\n");
		} else
		{
			String cellName = cni.getParameterizedName();
			multiLinePrint(false, "\n*** CELL: " + cell.describe() + "\n");
			StringBuffer infstr = new StringBuffer();
			infstr.append(".SUBCKT " + cellName);
			for(Iterator sIt = cni.getCellAggregateSignals(); sIt.hasNext(); )
			{
				CellAggregateSignal cas = (CellAggregateSignal)sIt.next();

				// ignore networks that aren't exported
				PortProto pp = cas.getExport();
				if (pp == null) continue;

				if (cas.isGlobal()) continue;
				if (useCDL)
				{
//					// if this is output and the last was input (or visa-versa), insert "/"
//					if (i > 0 && netlist[i-1]->temp2 != net->temp2)
//						infstr.append(" /");
				}

				int low = cas.getLowIndex(), high = cas.getHighIndex();
				if (low > high)
				{
					// single signal
					infstr.append(" " + cas.getName());
				} else
				{
					for(int j=low; j<=high; j++)
					{
						infstr.append(" " + cas.getName() + "[" + j + "]");
					}
				}
			}

			Global.Set globals = netList.getGlobals();
			int globalSize = globals.size();
			if (!Simulation.isSpiceUseNodeNames() || spiceEngine == Simulation.SPICE_ENGINE_3)
			{
				for(int i=0; i<globalSize; i++)
				{
					Global global = (Global)globals.get(i);
					int netIndex = netList.getNetIndex(global);
					JNetwork net = netList.getNetwork(netIndex);
					CellSignal cs = cni.getCellSignal(net);
					infstr.append(" " + cs.getName());
				}
			}
			if (!useCDL && Simulation.isSpiceUseCellParameters())
			{
				// add in parameters to this cell
				for(Iterator it = cell.getVariables(); it.hasNext(); )
				{
					Variable paramVar = (Variable)it.next();
					if (!paramVar.getTextDescriptor().isParam()) continue;
					infstr.append(" " + var.getTrueName() + "=" + var.getPureValue(-1, -1));
				}
			}
			infstr.append("\n");
			multiLinePrint(false, infstr.toString());

			// generate pin descriptions for reference (when not using node names)
			for(int i=0; i<globalSize; i++)
			{
				Global global = (Global)globals.get(i);
				int netIndex = netList.getNetIndex(global);
				JNetwork net = netList.getNetwork(netIndex);
				CellSignal cs = cni.getCellSignal(net);
				multiLinePrint(true, "** GLOBAL " + cs.getName() + "\n");
			}

			// write exports to this cell
			for(Iterator sIt = cni.getCellAggregateSignals(); sIt.hasNext(); )
			{
				CellAggregateSignal cas = (CellAggregateSignal)sIt.next();

				// ignore networks that aren't exported
				PortProto pp = cas.getExport();
				if (pp == null) continue;

				if (cas.isGlobal()) continue;

				int low = cas.getLowIndex(), high = cas.getHighIndex();
				if (low > high)
				{
					// single signal
					multiLinePrint(true, "** PORT " + cas.getName() + "\n");
				} else
				{
					multiLinePrint(true, "** PORT " + cas.getName() + "[" + low + ":" + high + "]\n");
				}
			}
		}

		// third pass through the node list, print it this time
		for(Iterator nIt = netList.getNodables(); nIt.hasNext(); )
		{
			Nodable no = (Nodable)nIt.next();
			NodeProto niProto = no.getProto();

			// handle sub-cell calls
			if (niProto instanceof Cell)
			{
				// look for a SPICE template on the prototype
				Variable varTemplate = niProto.getVar(preferedEgnineTemplateKey);
				if (varTemplate == null)
					varTemplate = niProto.getVar(SPICE_TEMPLATE_KEY);

				// handle self-defined models
				if (varTemplate != null)
				{
					String line = varTemplate.getObject().toString();
					StringBuffer infstr = new StringBuffer();
					for(int pt = 0; pt < line.length(); pt++)
					{
						char chr = line.charAt(pt);
						if (chr != '$' || pt+1 >= line.length() || line.charAt(pt+1) != '(')
						{
							infstr.append(chr);
							continue;
						}

						int start = pt + 2;
						for(pt = start; pt < line.length(); pt++)
							if (line.charAt(pt) == ')') break;
						String paramName = line.substring(start, pt);
						PortProto pp = niProto.findPortProto(paramName);
						if (pp != null)
						{
							// port name found: use its spice node
							JNetwork net = netList.getNetwork(no, pp, 0);
							CellSignal cs = cni.getCellSignal(net);
							infstr.append(cs.getName());
						} else if (paramName.equalsIgnoreCase("node_name"))
						{
							infstr.append(getSafeNetName(no.getName()));
						} else
						{
							// no port name found, look for variable name
							String varName = "ATTR_" + paramName;
							Variable attrVar = no.getVar(varName);
							if (attrVar == null) infstr.append("??"); else
							{
								infstr.append(attrVar.getPureValue(-1, -1));
							}
						}
					}
					infstr.append('\n');
					multiLinePrint(false, infstr.toString());
					continue;
				}

				// get the ports on this node (in proper order)
				CellNetInfo subCni = getCellNetInfo(parameterizedName(no, context));

				String modelChar = "X";
				if (no.getName() != null) modelChar += getSafeNetName(no.getName());
				StringBuffer infstr = new StringBuffer();
				infstr.append(modelChar);
				for(Iterator sIt = subCni.getCellAggregateSignals(); sIt.hasNext(); )
				{
					CellAggregateSignal cas = (CellAggregateSignal)sIt.next();

					// ignore networks that aren't exported
					PortProto pp = cas.getExport();
					if (pp == null) continue;

					int low = cas.getLowIndex(), high = cas.getHighIndex();
					if (low > high)
					{
						// single signal
						JNetwork net = netList.getNetwork(no, pp, 0);
						CellSignal cs = cni.getCellSignal(net);
						infstr.append(" " + cs.getName());
					} else
					{
						for(int j=low; j<=high; j++)
						{
							JNetwork net = netList.getNetwork(no, cas.getExport(), j-low);
							CellSignal cs = cni.getCellSignal(net);
							infstr.append(" " + cs.getName());
						}
					}
				}

				if (!Simulation.isSpiceUseNodeNames() || spiceEngine == Simulation.SPICE_ENGINE_3)
				{
					Global.Set globals = subCni.getNetList().getGlobals();
					int globalSize = globals.size();
					for(int i=0; i<globalSize; i++)
					{
						Global global = globals.get(i);
						infstr.append(" " + global.getName());
					}
				}
				infstr.append(" " + subCni.getParameterizedName());

				if (!useCDL && Simulation.isSpiceUseCellParameters())
				{
					// add in parameters to this instance
					for(Iterator it = niProto.getVariables(); it.hasNext(); )
					{
						Variable paramVar = (Variable)it.next();
						if (!paramVar.getTextDescriptor().isParam()) continue;
						Variable instVar = no.getVar(paramVar.getKey());
						String paramStr = "??";
						if (instVar != null) paramStr = instVar.describe(-1, -1);
						infstr.append(" " + paramStr);
					}
				}
				infstr.append("\n");
				multiLinePrint(false, infstr.toString());
				continue;
			}

			// get the type of this node
			NodeInst ni = (NodeInst)no;
			NodeProto.Function fun = ni.getFunction();

			// handle resistors, inductors, capacitors, and diodes
			if (fun == NodeProto.Function.RESIST || fun == NodeProto.Function.INDUCT ||
				fun == NodeProto.Function.CAPAC || fun == NodeProto.Function.ECAPAC ||
				fun == NodeProto.Function.DIODE || fun == NodeProto.Function.DIODEZ)
			{
				if (fun == NodeProto.Function.RESIST)
				{
					Variable resistVar = ni.getVar("sch_resistancekey");
					String extra = "";
					if (resistVar != null)
					{
						extra = resistVar.describe(-1, -1);
						if (TextUtils.isANumber(extra))
						{
							double pureValue = TextUtils.atof(extra);
							extra = TextUtils.displayedUnits(pureValue, TextDescriptor.Unit.RESISTANCE, TextUtils.UnitScale.NONE);
						}
					}
					writeTwoPort(ni, "R", extra, cni, netList);
				} else if (fun == NodeProto.Function.CAPAC || fun == NodeProto.Function.ECAPAC)
				{
					Variable capacVar = ni.getVar("sch_capacitancekey");
					String extra = "";
					if (capacVar != null)
					{
						extra = capacVar.describe(-1, -1);
						if (TextUtils.isANumber(extra))
						{
							double pureValue = TextUtils.atof(extra);
							extra = TextUtils.displayedUnits(pureValue, TextDescriptor.Unit.CAPACITANCE, TextUtils.UnitScale.NONE);
						}
					}
					writeTwoPort(ni, "C", extra, cni, netList);
				} else if (fun == NodeProto.Function.INDUCT)
				{
					Variable inductVar = ni.getVar("sch_inductancekey");
					String extra = "";
					if (inductVar != null)
					{
						extra = inductVar.describe(-1, -1);
						if (TextUtils.isANumber(extra))
						{
							double pureValue = TextUtils.atof(extra);
							extra = TextUtils.displayedUnits(pureValue, TextDescriptor.Unit.INDUCTANCE, TextUtils.UnitScale.NONE);
						}
					}
					writeTwoPort(ni, "L", extra, cni, netList);
				} else if (fun == NodeProto.Function.DIODE || fun == NodeProto.Function.DIODEZ)
				{
					Variable diodeVar = ni.getVar("sch_diodekey");
					String extra = "";
					if (diodeVar != null)
						extra = diodeVar.describe(-1, -1);
					if (extra.length() == 0) extra = "DIODE";
					writeTwoPort(ni, "D", extra, cni, netList);
				}
				continue;
			}

			// the default is to handle everything else as a transistor
			if (niProto.getGroupFunction() != NodeProto.Function.TRANS)
				continue;

			JNetwork gateNet = netList.getNetwork(ni.getTransistorGatePort());
			CellSignal gateCs = cni.getCellSignal(gateNet);
			JNetwork sourceNet = netList.getNetwork(ni.getTransistorSourcePort());
			CellSignal sourceCs = cni.getCellSignal(sourceNet);
			JNetwork drainNet = netList.getNetwork(ni.getTransistorDrainPort());
			CellSignal drainCs = cni.getCellSignal(drainNet);
			CellSignal biasCs = null;
			PortInst biasPort = ni.getTransistorBiasPort();
			if (biasPort != null)
			{
				biasCs = cni.getCellSignal(netList.getNetwork(biasPort));
			}

			// make sure transistor is connected to nets
			if (gateCs == null || sourceCs == null || drainCs == null)
			{
				String message = "WARNING: " + ni.describe() + " not fully connected in cell " + cell.describe();
				dumpErrorMessage(message);
			}

			// get model information
			String modelName = null;
			Variable modelVar = ni.getVar(SPICE_MODEL_KEY);
			if (modelVar != null) modelName = modelVar.getObject().toString();

			String modelChar = "";
			if (fun == NodeProto.Function.TRANSREF)					// self-referential transistor
			{
				modelChar = "X"; 
				biasCs = cni.getCellSignal(groundNet);
				modelName = niProto.getProtoName();
			} else if (fun == NodeProto.Function.TRANMOS)			// NMOS (Enhancement) transistor
			{
				modelChar = "M";
				biasCs = cni.getCellSignal(groundNet);
				if (modelName == null) modelName = "N";
			} else if (fun == NodeProto.Function.TRA4NMOS)			// NMOS (Complementary) 4-port transistor
			{
				modelChar = "M";
				if (modelName == null) modelName = "N";
			} else if (fun == NodeProto.Function.TRADMOS)			// DMOS (Depletion) transistor
			{
				modelChar = "M";
				biasCs = cni.getCellSignal(groundNet);
				if (modelName == null) modelName = "D";
			} else if (fun == NodeProto.Function.TRA4DMOS)			// DMOS (Depletion) 4-port transistor
			{
				modelChar = "M";
				if (modelName == null) modelName = "D";
			} else if (fun == NodeProto.Function.TRAPMOS)			// PMOS (Complementary) transistor
			{
				modelChar = "M";
				biasCs = cni.getCellSignal(powerNet);
				if (modelName == null) modelName = "P";
			} else if (fun == NodeProto.Function.TRA4PMOS)			// PMOS (Complementary) 4-port transistor
			{
				modelChar = "M";
				if (modelName == null) modelName = "P";
			} else if (fun == NodeProto.Function.TRANPN)			// NPN (Junction) transistor
			{
				modelChar = "Q";
//				biasn = subnet != NOSPNET ? subnet : 0;
				if (modelName == null) modelName = "NBJT";
			} else if (fun == NodeProto.Function.TRA4NPN)			// NPN (Junction) 4-port transistor
			{
				modelChar = "Q";
				if (modelName == null) modelName = "NBJT";
			} else if (fun == NodeProto.Function.TRAPNP)			// PNP (Junction) transistor
			{
				modelChar = "Q";
//				biasn = subnet != NOSPNET ? subnet : 0;
				if (modelName == null) modelName = "PBJT";
			} else if (fun == NodeProto.Function.TRA4PNP)			// PNP (Junction) 4-port transistor
			{
				modelChar = "Q";
				if (modelName == null) modelName = "PBJT";
			} else if (fun == NodeProto.Function.TRANJFET)			// NJFET (N Channel) transistor
			{
				modelChar = "J";
				biasCs = null;
				if (modelName == null) modelName = "NJFET";
			} else if (fun == NodeProto.Function.TRA4NJFET)			// NJFET (N Channel) 4-port transistor
			{
				modelChar = "J";
				if (modelName == null) modelName = "NJFET";
			} else if (fun == NodeProto.Function.TRAPJFET)			// PJFET (P Channel) transistor
			{
				modelChar = "J";
				biasCs = null;
				if (modelName == null) modelName = "PJFET";
			} else if (fun == NodeProto.Function.TRA4PJFET)			// PJFET (P Channel) 4-port transistor
			{
				modelChar = "J";
				if (modelName == null) modelName = "PJFET";
			} else if (fun == NodeProto.Function.TRADMES ||			// DMES (Depletion) transistor
				fun == NodeProto.Function.TRA4DMES)					// DMES (Depletion) 4-port transistor
			{
				modelChar = "Z";
				biasCs = null;
				modelName = "DMES";
			} else if (fun == NodeProto.Function.TRAEMES ||			// EMES (Enhancement) transistor
				fun == NodeProto.Function.TRA4EMES)					// EMES (Enhancement) 4-port transistor
			{
				modelChar = "Z";
				biasCs = null;
				modelName = "EMES";
			} else if (fun == NodeProto.Function.TRANS)				// special transistor
			{
				modelChar = "Q";
//				biasn = subnet != NOSPNET ? subnet : 0;
			}
			if (ni.getName() != null) modelChar += getSafeNetName(ni.getName());
			StringBuffer infstr = new StringBuffer();
			infstr.append(modelChar + " " + drainCs.getName() + " " + gateCs.getName() + " " + sourceCs.getName());
			if (biasCs != null) infstr.append(" " + biasCs.getName());
			if (modelName != null) infstr.append(" " + modelName);

			// compute length and width (or area for nonMOS transistors)
			Dimension size = ni.getTransistorSize(context);
			if (size.width > 0 || size.height > 0)
			{
				double w = maskScale * size.width;
				double l = maskScale * size.height;
				if (!Simulation.isSpiceWriteTransSizeInLambda())
				{
					// make into microns (convert to nanometers then divide by 1000)
					l *= layoutTechnology.getScale() / 1000.0;
					w *= layoutTechnology.getScale() / 1000.0;
				}

				if (fun == NodeProto.Function.TRANMOS  || fun == NodeProto.Function.TRA4NMOS ||
					fun == NodeProto.Function.TRAPMOS || fun == NodeProto.Function.TRA4PMOS ||
					fun == NodeProto.Function.TRADMOS || fun == NodeProto.Function.TRA4DMOS ||
					((fun == NodeProto.Function.TRANJFET || fun == NodeProto.Function.TRAPJFET ||
					  fun == NodeProto.Function.TRADMES || fun == NodeProto.Function.TRAEMES) &&
					  spiceEngine == Simulation.SPICE_ENGINE_H))
				{
					infstr.append(" L=" + TextUtils.formatDouble(l, 2) + "U");
					infstr.append(" W=" + TextUtils.formatDouble(w, 2) + "U");
				}
				if (fun != NodeProto.Function.TRANMOS && fun != NodeProto.Function.TRA4NMOS &&
					fun != NodeProto.Function.TRAPMOS && fun != NodeProto.Function.TRA4PMOS &&
					fun != NodeProto.Function.TRADMOS && fun != NodeProto.Function.TRA4DMOS)
				{
					infstr.append(" AREA=" + TextUtils.formatDouble(l*w, 2) + "P");
				}
			}

			// make sure transistor is connected to nets
			SpiceNet spNetGate = (SpiceNet)spiceNetMap.get(gateNet);
			SpiceNet spNetSource = (SpiceNet)spiceNetMap.get(sourceNet);
			SpiceNet spNetDrain = (SpiceNet)spiceNetMap.get(drainNet);
			if (spNetGate == null || spNetSource == null || spNetDrain == null) continue;

			// compute area of source and drain
			if (!useCDL)
			{
				if (fun == NodeProto.Function.TRANMOS || fun == NodeProto.Function.TRA4NMOS ||
					fun == NodeProto.Function.TRAPMOS || fun == NodeProto.Function.TRA4PMOS ||
					fun == NodeProto.Function.TRADMOS || fun == NodeProto.Function.TRA4DMOS)
				{
					double as = 0, ad = 0, ps = 0, pd = 0;
					if (spNetSource.transistorCount != 0)
					{
						as = spNetSource.diffArea / spNetSource.transistorCount;
						ps = spNetSource.diffPerim / spNetSource.transistorCount;
						if (!Simulation.isSpiceWriteTransSizeInLambda())
						{
							as *= layoutTechnology.getScale() * layoutTechnology.getScale() / 1000000.0;
							ps *= layoutTechnology.getScale() / 1000.0;
						}
					}
					if (spNetDrain.transistorCount != 0)
					{
						ad = spNetDrain.diffArea / spNetDrain.transistorCount;
						pd = spNetDrain.diffPerim / spNetDrain.transistorCount;
						if (!Simulation.isSpiceWriteTransSizeInLambda())
						{
							ad *= layoutTechnology.getScale() * layoutTechnology.getScale() / 1000000.0;
							pd *= layoutTechnology.getScale() / 1000.0;
						}
					}
					if (as > 0.0) infstr.append(" AS=" + TextUtils.formatDouble(as, 2) + "P");
					if (ad > 0.0) infstr.append(" AD=" + TextUtils.formatDouble(ad, 2) + "P");
					if (ps > 0.0) infstr.append(" PS=" + TextUtils.formatDouble(ps, 2) + "U");
					if (pd > 0.0) infstr.append(" PD=" + TextUtils.formatDouble(pd, 2) + "U");
				}
			}
			infstr.append("\n");
			multiLinePrint(false, infstr.toString());
		}

		// print resistances and capacitances
		if (!useCDL)
		{
			if (Simulation.isSpiceUseParasitics())
			{
				// print parasitic capacitances
				boolean first = true;
				int capacNum = 1;
				for(Iterator sIt = cni.getCellSignals(); sIt.hasNext(); )
				{
					CellSignal cs = (CellSignal)sIt.next();
					JNetwork net = cs.getNetwork();
					if (net == cni.getGroundNet()) continue;

					SpiceNet spNet = (SpiceNet)spiceNetMap.get(net);
					if (spNet.nonDiffCapacitance > layoutTechnology.getMinCapacitance())
					{
						if (first)
						{
							first = false;
							multiLinePrint(true, "** Extracted Parasitic Elements:\n");
						}
						multiLinePrint(false, "C" + capacNum + " " + cs.getName() + " 0 " + TextUtils.formatDouble(spNet.nonDiffCapacitance, 2) + "F\n");
						capacNum++;
					}
				}
			}
		}

		// write out any directly-typed SPICE cards
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() != Generic.tech.invisiblePinNode) continue;
			Variable cardVar = ni.getVar(SPICE_CARD_KEY);
			if (cardVar == null) continue;
			Object obj = cardVar.getObject();
			if (!(obj instanceof String) && !(obj instanceof String[])) continue;
			if (!cardVar.isDisplay()) continue;
			if (obj instanceof String)
			{
				multiLinePrint(false, (String)obj + "\n");
			} else
			{
				String [] strings = (String [])obj;
				for(int i=0; i<strings.length; i++)
					multiLinePrint(false, strings[i] + "\n");
			}
		}

		// now we're finished writing the subcircuit.
		if (cell != topCell || useCDL)
		{
			multiLinePrint(false, ".ENDS " + cni.getParameterizedName() + "\n");
		}
	}

	/****************************** SUBCLASSED METHODS FOR THE TOPOLOGY ANALYZER ******************************/

	/**
	 * Method to adjust a cell name to be safe for Spice output.
	 * @param name the cell name.
	 * @return the name, adjusted for Spice output.
	 */
	protected String getSafeCellName(String name)
	{
		return getSafeNetName(name);
	}

	/** Abstract method to return the proper name of Power */
	protected String getPowerName() { return "vdd"; }

	/** Abstract method to return the proper name of Ground */
	protected String getGroundName() { return "gnd"; }

	/** Abstract method to return the proper name of a Global signal */
	protected String getGlobalName(Global glob) { return glob.getName(); }

	/*
	 * Method to adjust a network name to be safe for Spice output.
	 * Spice has a list of legal punctuation characters that it allows.
	 */
	protected String getSafeNetName(String name)
	{
		// simple names are trivially accepted as is
		boolean allAlNum = true;
		int len = name.length();
		for(int i=0; i<len; i++)
		{
			if (!Character.isLetterOrDigit(name.charAt(i)))
			{
				allAlNum = false;
				break;
			}
		}
		if (allAlNum) return name;

		StringBuffer sb = new StringBuffer();
		for(int t=0; t<name.length(); t++)
		{
			char chr = name.charAt(t);
			boolean legalChar = Character.isLetterOrDigit(chr);
			if (!legalChar)
			{
				for(int j=0; j<legalSpiceChars.length(); j++)
				{
					char legalChr = legalSpiceChars.charAt(j);
					if (chr == legalChr) { legalChar = true;   break; }
				}
			}
			if (!legalChar) chr = '_';
			sb.append(chr);
		}
		return sb.toString();
	}

	/**
	 * Method to obtain Netlist information for a cell.
	 * This is pushed to the writer because each writer may have different requirements for resistor inclusion.
	 * Spice ignores resistors.
	 */
	protected Netlist getNetlistForCell(Cell cell)
	{
		// get network information about this cell
		boolean shortResistors = false;
		Netlist netList = cell.getNetlist(shortResistors);
		return netList;
	}

	/**
	 * Method to tell whether the topological analysis should mangle cell names that are parameterized.
	 */
	protected boolean canParameterizeNames() { return !useCDL; }

	/**
	 * Method to tell set a limit on the number of characters in a name.
	 * @return the limit to name size (SPICE limits to 32 character names?????). 
	 */
	protected int maxNameLength() { return SPICEMAXLENSUBCKTNAME; }

	/******************** DECK GENERATION SUPPORT ********************/

	/*
	 * write a header for "cell" to spice deck "sim_spice_file"
	 * The model cards come from a file specified by tech:~.SIM_spice_model_file
	 * or else tech:~.SIM_spice_header_level%ld
	 * The spice model file can be located in el_libdir
	 */
	private void writeHeader(Cell cell)
	{
		// Print the header line for SPICE 
		multiLinePrint(true, "*** SPICE deck for cell " + cell.noLibDescribe() +
			" from library " + cell.getLibrary().getLibName() + "\n");
		emitCopyright("*** ", "");
		if (User.isIncludeDateAndVersionInOutput())
		{
			SimpleDateFormat sdf = new SimpleDateFormat("EEE MMMM dd, yyyy HH:mm:ss");
			multiLinePrint(true, "*** Created on " + sdf.format(topCell.getCreationDate()) + "\n");
			multiLinePrint(true, "*** Last revised on " + sdf.format(topCell.getRevisionDate()) + "\n");
			multiLinePrint(true, "*** Written on " + sdf.format(new Date()) +
				" by Electric VLSI Design System, version " + Version.CURRENT + "\n");
		} else
		{
			multiLinePrint(true, "*** Written by Electric VLSI Design System\n");
		}

		multiLinePrint(true, "*** UC SPICE *** , MIN_RESIST " + layoutTechnology.getMinResistance() +
			", MIN_CAPAC " + layoutTechnology.getMinCapacitance() + "FF\n");
		multiLinePrint(false, ".OPTIONS NOMOD NOPAGE\n");

		// if sizes to be written in lambda, tell spice conversion factor
		if (Simulation.isSpiceWriteTransSizeInLambda())
		{
			double scale = layoutTechnology.getScale();
			multiLinePrint(false, "*** Lambda Conversion ***\n");
			multiLinePrint(false, ".opt scale=" + TextUtils.formatDouble(scale / 1000.0, 3) + "U\n\n");
		}

		// see if spice model/option cards from file if specified
		String headerFile = Simulation.getSpiceHeaderCardInfo();
		if (headerFile.length() > 0)
		{
			if (headerFile.startsWith(":::::"))
			{
				// extension specified: look for a file with the cell name and that extension
				String headerPath = TextUtils.getFilePath(cell.getLibrary().getLibFile());
				String fileName = headerPath + cell.getProtoName() + "." + headerFile.substring(5);
				File test = new File(fileName);
				if (test.exists())
				{
					multiLinePrint(true, "* Model cards are described in this file:\n");
					addIncludeFile(fileName);
					return;
				}
			} else
			{
				// normal header file specified
				File test = new File(headerFile);
				if (!test.exists())
					System.out.println("Warning: cannot find model file '" + headerFile + "'");
				multiLinePrint(true, "* Model cards are described in this file:\n");
				addIncludeFile(headerFile);
				return;
			}
		}

		// no header files: write predefined header for this level and technology
		int level = TextUtils.atoi(Simulation.getSpiceLevel());
		String [] header = null;
		switch (level)
		{
			case 1: header = layoutTechnology.getSpiceHeaderLevel1();   break;
			case 2: header = layoutTechnology.getSpiceHeaderLevel2();   break;
			case 3: header = layoutTechnology.getSpiceHeaderLevel3();   break;
		}
		if (header != null)
		{
			for(int i=0; i<header.length; i++)
				multiLinePrint(false, header[i] + "\n");
			return;
		}
		System.out.println("WARNING: no model cards for SPICE level " + level +
			" in " + layoutTechnology.getTechName() + " technology");
	}

	/*
	 * Write a trailer from an external file, defined as a variable on
	 * the current technology in this library: tech:~.SIM_spice_trailer_file
	 * if it is available.
	 */
	private void writeTrailer(Cell cell)
	{
		// get spice trailer cards from file if specified
		String trailerFile = Simulation.getSpiceTrailerCardInfo();
		if (trailerFile.length() > 0)
		{
			if (trailerFile.startsWith(":::::"))
			{
				// extension specified: look for a file with the cell name and that extension
				String trailerpath = TextUtils.getFilePath(cell.getLibrary().getLibFile());
				String fileName = trailerpath + cell.getProtoName() + "." + trailerFile.substring(5);
				File test = new File(fileName);
				if (test.exists())
				{
					multiLinePrint(true, "* Trailer cards are described in this file:\n");
					addIncludeFile(fileName);
				}
			} else
			{
				// normal trailer file specified
				multiLinePrint(true, "* Trailer cards are described in this file:\n");
				addIncludeFile(trailerFile);
			}
		}
	}

	/*
	 * Function to write a two port device to the file. Complain about any missing connections.
	 * Determine the port connections from the portprotos in the instance
	 * prototype. Get the part number from the 'part' number value;
	 * increment it. The type of device is declared in type; extra is the string
	 * data acquired before calling here.
	 * If the device is connected to the same net at both ends, do not
	 * write it. Is this OK?
	 */
	private void writeTwoPort(NodeInst ni, String partName, String extra, CellNetInfo cni, Netlist netList)
	{
		PortInst port0 = ni.getPortInst(0);
		PortInst port1 = ni.getPortInst(1);
		JNetwork net0 = netList.getNetwork(port0);
		JNetwork net1 = netList.getNetwork(port1);
		CellSignal cs0 = cni.getCellSignal(net0);
		CellSignal cs1 = cni.getCellSignal(net1);

		// make sure the component is connected to nets
		if (cs0 == null || cs1 == null)
		{
			String message = "WARNING: " + ni.describe() + " component not fully connected in cell " + ni.getParent().describe();
			dumpErrorMessage(message);
		}
		if (cs0 != null && cs1 != null && cs0 == cs1)
		{
			String message = "WARNING: " + ni.describe() + " component appears to be shorted on net " + net0.toString() +
				" in cell " + ni.getParent().describe();
			dumpErrorMessage(message);
			return;
		}

		if (ni.getName() != null) partName += getSafeNetName(ni.getName());
		multiLinePrint(false, partName + " " + cs1.getName() + " " + cs0.getName() + " " + extra + "\n");
	}

	/******************** PARASITIC CALCULATIONS ********************/

	/*
	 * Method to recursively determine the area of diffusion and capacitance
	 * associated with port "pp" of nodeinst "ni".  If the node is mult_layer, then
	 * determine the dominant capacitance layer, and add its area; all other
	 * layers will be added as well to the extra_area total.
	 * Continue out of the ports on a complex cell
	 */
	private void addNodeInformation(Netlist netList, HashMap spiceNets, NodeInst ni)
	{
		// cells have no area or capacitance (for now)
		NodeProto np = ni.getProto();
		if (np instanceof Cell) return;  // No area for complex nodes

		NodeProto.Function function = ni.getFunction();

		// initialize to examine the polygons on this node
		Technology tech = np.getTechnology();
		AffineTransform trans = ni.rotateOut();

		// make linked list of polygons
		Poly [] polyList = tech.getShapeOfNode(ni, null, true, true);
		int tot = polyList.length;
		for(int i=0; i<tot; i++)
		{
			Poly poly = polyList[i];

			// make sure this layer connects electrically to the desired port
			PortProto pp = poly.getPort();
			if (pp == null) continue;
			JNetwork net = netList.getNetwork(ni, pp, 0);

			// don't bother with layers without capacity
			Layer layer = poly.getLayer();
			if (!layerIsDiff(layer) && layer.getCapacitance() == 0.0) continue;
			if (layer.getTechnology() != Technology.getCurrent()) continue;

			// leave out the gate capacitance of transistors
			if (layer.getFunction() == Layer.Function.GATE) continue;

			SpiceNet spNet = (SpiceNet)spiceNets.get(net);
			if (spNet == null) continue;

			// get the area of this polygon
			poly.transform(trans);
			spNet.merge.addPolygon(layer, poly);

			// count the number of transistors on this net
			if (layerIsDiff(layer) && function.isTransistor()) spNet.transistorCount++;
		}
	}

	/*
	 * Method to recursively determine the area of diffusion, capacitance, (NOT
	 * resistance) on arc "ai". If the arc contains active device diffusion, then
	 * it will contribute to the area of sources and drains, and the other layers
	 * will be ignored. This is not quite the same as the rule used for
	 * contact (node) structures. Note: the earlier version of this
	 * function assumed that diffusion arcs would always have zero capacitance
	 * values for the other layers; this produces an error if any of these layers
	 * have non-zero values assigned for other reasons. So we will check for the
	 * function of the arc, and if it contains active device, we will ignore any
	 * other layers
	 */
	private void addArcInformation(PolyMerge merge, ArcInst ai)
	{
		boolean isDiffArc = arcIsDiff(ai);    // check arc function

		Technology tech = ai.getProto().getTechnology();
		Poly [] arcInstPolyList = tech.getShapeOfArc(ai);
		int tot = arcInstPolyList.length;
		for(int j=0; j<tot; j++)
		{
			Poly poly = arcInstPolyList[j];
			if (poly.getStyle().isText()) continue;

			Layer layer = poly.getLayer();
			if (layer.getTechnology() != Technology.getCurrent()) continue;
			if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;

			if (layerIsDiff(layer) ||
				(!isDiffArc && layer.getCapacitance() > 0.0))
					merge.addPolygon(layer, poly);
		}
	}

	/******************** TEXT METHODS ********************/

	/*
	 * Method to insert an "include" of file "filename" into the stream "io".
	 */
	private void addIncludeFile(String fileName)
	{
		if (spiceEngine == Simulation.SPICE_ENGINE_2 || spiceEngine == Simulation.SPICE_ENGINE_3 ||
			spiceEngine == Simulation.SPICE_ENGINE_G || spiceEngine == Simulation.SPICE_ENGINE_S)
		{
			multiLinePrint(false, ".include " + fileName + "\n");
		} else if (spiceEngine == Simulation.SPICE_ENGINE_H)
		{
			multiLinePrint(false, ".include '" + fileName + "'\n");
		} else if (spiceEngine == Simulation.SPICE_ENGINE_P)
		{
			multiLinePrint(false, ".INC " + fileName + "\n");
		}
	}

	/******************** SUPPORT ********************/

	/*
	 * Method to return nonzero if layer "layer" is on diffusion
	 * Return the type of the diffusion
	 */
	private boolean layerIsDiff(Layer layer)
	{
		int extras = layer.getFunctionExtras();
		if ((extras&Layer.Function.PSEUDO) == 0)
		{
			if (layer.getFunction().isDiff()) return true;
		}
		return false;
	}

	/*
	 * Method to return value if arc contains device active diffusion
	 */
	private boolean arcIsDiff(ArcInst ai)
	{
		ArcProto.Function fun = ai.getProto().getFunction();
		if (fun == ArcProto.Function.DIFFP || fun == ArcProto.Function.DIFFN || fun == ArcProto.Function.DIFF) return true;
		if (fun == ArcProto.Function.DIFFS || fun == ArcProto.Function.DIFFW) return true;
		return false;
	}

	/******************** LOW-LEVEL OUTPUT METHODS ********************/

	/*
	 * Method to report an error that is built in the infinite string.
	 * The error is sent to the messages window and also to the SPICE deck "f".
	 */
	private void dumpErrorMessage(String message)
	{
		multiLinePrint(true, "*** " + message + "\n");
		System.out.println(message);
	}

	/*
	 * Formatted output to file "stream".  All spice output is in upper case.
	 * The buffer can contain no more than 1024 chars including the newlinelastMoveTo
	 * and null characters.
	 * Doesn't return anything.
	 */
	private void multiLinePrint(boolean isComment, String str)
	{
		// put in line continuations, if over 78 chars long
		char contChar = '+';
		if (isComment) contChar = '*';
		int lastSpace = -1;
		int count = 0;
		boolean insideQuotes = false;
		int lineStart = 0;
		for (int pt = 0; pt < str.length(); pt++)
		{
			char chr = str.charAt(pt);
//			if (sim_spice_machine == SPICE2)
//			{
//				if (islower(*pt)) *pt = toupper(*pt);
//			}
			if (chr == '\n')
			{
				printWriter.print(str.substring(lineStart, pt+1));
				count = 0;
				lastSpace = -1;
				lineStart = pt+1;
			} else
			{
				if (chr == ' ' && !insideQuotes) lastSpace = pt;
				if (chr == '\'') insideQuotes = !insideQuotes;
				count++;
				if (count >= 78 && !insideQuotes)
				{
					if (lastSpace < 0) lastSpace = pt;
					String partial = str.substring(lineStart, lastSpace+1);
					printWriter.print(partial + "\n" + contChar);
					count = 1;
					lineStart = lastSpace+1;
					lastSpace = -1;
				}
			}
		}
		if (lineStart < str.length())
		{
			String partial = str.substring(lineStart);
			printWriter.print(partial);
		}
	}

}
