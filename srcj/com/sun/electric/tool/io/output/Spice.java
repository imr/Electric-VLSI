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

import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyMerge;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.TransistorSize;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.io.input.Simulate;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.Exec;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.ExecDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WaveformWindow;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;
import java.util.*;

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
	/** key of Variable holding SPICE model file. */			public static final Variable.Key SPICE_MODEL_FILE_KEY = ElectricObject.newKey("SIM_spice_behave_file");
	/** Prefix for spice extension. */                          public static final String SPICE_EXTENSION_PREFIX = "Extension ";

	/** maximum subcircuit name length */						private static final int SPICEMAXLENSUBCKTNAME     = 70;
	/** legal characters in a spice deck */						private static final String SPICELEGALCHARS        = "!#$%*+-/<>[]_";
	/** legal characters in a CDL deck */						private static final String CDLNOBRACKETLEGALCHARS = "!#$%*+-/<>_";

	/** default Technology to use. */				private Technology layoutTechnology;
	/** Mask shrink factor (default =1) */			private double  maskScale;
	/** True to write CDL format */					private boolean useCDL;
	/** Legal characters */							private String legalSpiceChars;
	/** Template Key for current spice engine */	private Variable.Key preferedEgnineTemplateKey;
	/** Spice type: 2, 3, H, P, etc */				private int spiceEngine;

    /** map of "parameterized" cells that are not covered by Topology */    private Map uniquifyCells;
    /** uniqueID */                                                         private int uniqueID;
    /** map of shortened instance names */                                  private Map uniqueNames;

	private static class SpiceNet
	{
		/** network object associated with this */	Network      network;
		/** merged geometry for this network */		PolyMerge     merge;
		/** area of diffusion */					double        diffArea;
		/** perimeter of diffusion */				double        diffPerim;
		/** amount of capacitance in non-diff */	float         nonDiffCapacitance;
		/** number of transistors on the net */		int           transistorCount;
	}

    private static class SpiceFinishedListener implements Exec.FinishedListener {
        private Cell cell;
        private OpenFile.Type type;
        private String file;
        private SpiceFinishedListener(Cell cell, OpenFile.Type type, String file) {
            this.cell = cell;
            this.type = type;
            this.file = file;
        }
        public void processFinished(Exec.FinishedEvent e) {
            URL fileURL = TextUtils.makeURLToFile(file);

			// create a new waveform window
            WaveformWindow ww = WaveformWindow.findWaveformWindow(cell);

            Simulate.plotSimulationResults(type, cell, fileURL, ww);
        }
    }

	/**
	 * The main entry point for Spice deck writing.
	 * @param cell the top-level cell to write.
	 * @param filePath the disk file to create with Spice.
	 */
	public static void writeSpiceFile(Cell cell, VarContext context, String filePath, boolean cdl)
	{
		Spice out = new Spice();
		out.useCDL = cdl;
		if (out.openTextOutputStream(filePath)) return;
		if (out.writeCell(cell, context)) return;
		if (out.closeTextOutputStream()) return;
		System.out.println(filePath + " written");

		// write CDL support file if requested
		if (out.useCDL)
		{
			// write the control files
			String deckFile = filePath;
			String deckPath = "";
			int lastDirSep = deckFile.lastIndexOf(File.separatorChar);
			if (lastDirSep > 0)
			{
				deckPath = deckFile.substring(0, lastDirSep);
				deckFile = deckFile.substring(lastDirSep+1);
			}

			String templateFile = deckPath + File.separator + cell.getName() + ".cdltemplate";
			if (out.openTextOutputStream(templateFile)) return;

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
			out.printWriter.print("    'primaryCell            \"" + cell.getName() + "\"\n");
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

        String runSpice = Simulation.getSpiceRunChoice();
        if (!runSpice.equals(Simulation.spiceRunChoiceDontRun)) {
            String command = Simulation.getSpiceRunProgram() + " " + Simulation.getSpiceRunProgramArgs();

            // see if user specified custom dir to run process in
            String workdir = User.getWorkingDirectory();
            String rundir = workdir;
            if (Simulation.getSpiceUseRunDir()) {
                rundir = Simulation.getSpiceRunDir();
            }
            File dir = new File(rundir);

            int start = filePath.lastIndexOf(File.separator);
            if (start == -1) start = 0; else {
                start++;
                if (start > filePath.length()) start = filePath.length();
            }
            int end = filePath.lastIndexOf(".");
            if (end == -1) end = filePath.length();
            String filename_noext = filePath.substring(start, end);
            String filename = filePath.substring(start, filePath.length());

            // replace vars in command and args
            command = command.replaceAll("\\$\\{WORKING_DIR}", workdir);
            command = command.replaceAll("\\$\\{USE_DIR}", rundir);
            command = command.replaceAll("\\$\\{FILENAME}", filename);
            command = command.replaceAll("\\$\\{FILENAME_NO_EXT}", filename_noext);

            // set up run probe
            OpenFile.Type type = Simulate.getCurrentSpiceOutputType();
            String [] extensions = type.getExtensions();
            String outFile = rundir + File.separator + filename_noext + "." + extensions[0];
            Exec.FinishedListener l = new SpiceFinishedListener(cell, type, outFile);

            if (runSpice.equals(Simulation.spiceRunChoiceRunIgnoreOutput)) {
                Exec e = new Exec(command, null, dir, null, null);
                if (Simulation.getSpiceRunProbe()) e.addFinishedListener(l);
                e.start();
            }
            if (runSpice.equals(Simulation.spiceRunChoiceRunReportOutput)) {
                ExecDialog dialog = new ExecDialog(TopLevel.getCurrentJFrame(), false);
                if (Simulation.getSpiceRunProbe()) dialog.addFinishedListener(l);
                dialog.startProcess(command, null, dir);
            }
            System.out.println("Running spice command: "+command);

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

        // set up the parameterized cells
        uniquifyCells = new HashMap();
        uniqueID = 0;
        uniqueNames = new HashMap();
        checkIfParameterized(topCell);

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
					String name = global.getName();
					if (global == Global.power) name = getPowerName();
					if (global == Global.ground) name = getGroundName();
					infstr.append(" " + name);
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
     * To write M factor information into given string buffer
     * @param no Nodable representing the node
     * @param infstr Buffer where to write to
     */
    private void writeMFactor(VarContext context, Nodable no, StringBuffer infstr)
    {
        Variable mVar = no.getVar("ATTR_M");
        if (mVar == null) return;
        Object value = context.evalVar(mVar);

        // check for M=@M, and warn user that this is a bad idea, and we will not write it out
        if (mVar.getObject().toString().equals("@M") || (mVar.getObject().toString().equals("P(\"M\")"))) {
            System.out.println("Warning: M=@M [eval="+value+"] on "+no.getName()+" is a bad idea, not writing it out: "+context.push(no).getInstPath("."));
            return;
        }

        infstr.append(" M=" + formatParam(value.toString()));
    }

	/**
	 * Method to write cellGeom
	 */
	protected void writeCellTopology(Cell cell, CellNetInfo cni, VarContext context)
	{
		// look for a model file on the current cell
		Variable var = cell.getVar(SPICE_MODEL_FILE_KEY);
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
			Network net = (Network)it.next();

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
			PrimitiveNode.Function fun = ni.getFunction();
			if (fun == PrimitiveNode.Function.TRANPN || fun == PrimitiveNode.Function.TRA4NPN ||
				fun == PrimitiveNode.Function.TRAPNP || fun == PrimitiveNode.Function.TRA4PNP ||
				fun == PrimitiveNode.Function.TRANS) bipolarTrans++; else
			if (fun == PrimitiveNode.Function.TRAEMES || fun == PrimitiveNode.Function.TRA4EMES ||
				fun == PrimitiveNode.Function.TRADMES || fun == PrimitiveNode.Function.TRA4DMES ||
				fun == PrimitiveNode.Function.TRADMOS || fun == PrimitiveNode.Function.TRA4DMOS ||
				fun == PrimitiveNode.Function.TRANMOS || fun == PrimitiveNode.Function.TRA4NMOS) nmosTrans++; else
			if (fun == PrimitiveNode.Function.TRAPMOS || fun == PrimitiveNode.Function.TRA4PMOS) pmosTrans++;
		}

		// accumulate geometry of all arcs
		for(Iterator aIt = cell.getArcs(); aIt.hasNext(); )
		{
			ArcInst ai = (ArcInst)aIt.next();

			// don't count non-electrical arcs
			if (ai.getProto().getFunction() == ArcProto.Function.NONELEC) continue;

			// ignore busses
//			if (ai->network->buswidth > 1) continue;
			Network net = netList.getNetwork(ai, 0);
			SpiceNet spNet = (SpiceNet)spiceNetMap.get(net);
			if (spNet == null) continue;

			addArcInformation(spNet.merge, ai);
		}

		// get merged polygons so far
		for(Iterator it = netList.getNetworks(); it.hasNext(); )
		{
			Network net = (Network)it.next();
			SpiceNet spNet = (SpiceNet)spiceNetMap.get(net);
			for(Iterator lIt = spNet.merge.getKeyIterator(); lIt.hasNext(); )
			{
				Layer layer = (Layer)lIt.next();
				List polyList = spNet.merge.getMergedPoints(layer, true);
				if (polyList == null) continue;
				for(Iterator pIt = polyList.iterator(); pIt.hasNext(); )
				{
					PolyBase poly = (PolyBase)pIt.next();
					Point2D [] pointList = poly.getPoints();
					int count = pointList.length;

					// compute perimeter and area
					double perim = poly.getPerimeter();
					double area = poly.getArea();

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
		Network groundNet = cni.getGroundNet();
		Network powerNet = cni.getPowerNet();
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
			for(Iterator sIt = cni.getCellSignals(); sIt.hasNext(); )
			{
				CellSignal cs = (CellSignal)sIt.next();

				// ignore networks that aren't exported
				PortProto pp = cs.getExport();
				if (pp == null) continue;

				if (cs.isGlobal() && !cs.getNetwork().isExported()) continue;
				if (useCDL)
				{
//					// if this is output and the last was input (or visa-versa), insert "/"
//					if (i > 0 && netlist[i-1]->temp2 != net->temp2)
//						infstr.append(" /");
				}

				infstr.append(" " + cs.getName());
			}

			Global.Set globals = netList.getGlobals();
			int globalSize = globals.size();
			if (!Simulation.isSpiceUseNodeNames() || spiceEngine == Simulation.SPICE_ENGINE_3)
			{
				for(int i=0; i<globalSize; i++)
				{
					Global global = (Global)globals.get(i);
					Network net = netList.getNetwork(global);
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
					infstr.append(" " + paramVar.getTrueName() + "=" + paramVar.getPureValue(-1, -1));
				}
			}
            // Writing M factor
            //writeMFactor(cell, infstr);
			infstr.append("\n");
			multiLinePrint(false, infstr.toString());

			// generate pin descriptions for reference (when not using node names)
			for(int i=0; i<globalSize; i++)
			{
				Global global = (Global)globals.get(i);
				Network net = netList.getNetwork(global);
				CellSignal cs = cni.getCellSignal(net);
				multiLinePrint(true, "** GLOBAL " + cs.getName() + "\n");
			}

			// write exports to this cell
			for(Iterator sIt = cni.getCellSignals(); sIt.hasNext(); )
			{
				CellSignal cs = (CellSignal)sIt.next();

				// ignore networks that aren't exported
				PortProto pp = cs.getExport();
				if (pp == null) continue;

				if (cs.isGlobal()) continue;
				multiLinePrint(true, "** PORT " + cs.getName() + "\n");
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
				Cell subCell = (Cell)niProto;
				// look for a SPICE template on the prototype
				Variable varTemplate = subCell.getVar(preferedEgnineTemplateKey);
				if (varTemplate == null)
					varTemplate = subCell.getVar(SPICE_TEMPLATE_KEY);

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
						PortProto pp = subCell.findPortProto(paramName);
						if (pp != null)
						{
							// port name found: use its spice node
							Network net = netList.getNetwork(no, pp, 0);
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
                            if (attrVar == null) attrVar = no.getParameter(varName);
							if (attrVar == null) infstr.append("??"); else
							{
                                //if (attrVar.getCode() != Variable.Code.NONE)
                                    infstr.append(trimSingleQuotes(String.valueOf(context.evalVar(attrVar, no))));
                                //else
								//    infstr.append(trimSingleQuotes(attrVar.getPureValue(-1, -1)));
							}
						}
					}
                    // Writing MFactor if available. Not sure here
					writeMFactor(context, no, infstr);
					
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
				for(Iterator sIt = subCni.getCellSignals(); sIt.hasNext(); )
				{
					CellSignal subCS = (CellSignal)sIt.next();

					// ignore networks that aren't exported
					PortProto pp = subCS.getExport();
					if (pp == null) continue;

					Network net = netList.getNetwork(no, pp, subCS.getExportIndex());
					CellSignal cs = cni.getCellSignal(net);
					infstr.append(" " + cs.getName());
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
					for(Iterator it = subCell.getVariables(); it.hasNext(); )
					{
						Variable paramVar = (Variable)it.next();
						if (!paramVar.getTextDescriptor().isParam()) continue;
						Variable instVar = no.getVar(paramVar.getKey());
						String paramStr = "??";
						if (instVar != null) paramStr = formatParam(trimSingleQuotes(String.valueOf(context.evalVar(instVar))));
						infstr.append(" " + paramVar.getTrueName() + "=" + paramStr);
					}
				}
                // Writing MFactor if available.
                writeMFactor(context, no, infstr);

				infstr.append("\n");
				multiLinePrint(false, infstr.toString());
				continue;
			}

			// get the type of this node
			NodeInst ni = (NodeInst)no;
			PrimitiveNode.Function fun = ni.getFunction();

			// handle resistors, inductors, capacitors, and diodes
			if (fun == PrimitiveNode.Function.RESIST || fun == PrimitiveNode.Function.INDUCT ||
				fun == PrimitiveNode.Function.CAPAC || fun == PrimitiveNode.Function.ECAPAC ||
				fun == PrimitiveNode.Function.DIODE || fun == PrimitiveNode.Function.DIODEZ)
			{
				if (fun == PrimitiveNode.Function.RESIST)
				{
					Variable resistVar = ni.getVar(Schematics.SCHEM_RESISTANCE);
					String extra = "";
					if (resistVar != null)
					{
						extra = resistVar.describe(context, ni);
						if (TextUtils.isANumber(extra))
						{
							double pureValue = TextUtils.atof(extra);
							extra = TextUtils.displayedUnits(pureValue, TextDescriptor.Unit.RESISTANCE, TextUtils.UnitScale.NONE);
						}
					}
					writeTwoPort(ni, "R", extra, cni, netList, context);
				} else if (fun == PrimitiveNode.Function.CAPAC || fun == PrimitiveNode.Function.ECAPAC)
				{
					Variable capacVar = ni.getVar(Schematics.SCHEM_CAPACITANCE);
					String extra = "";
					if (capacVar != null)
					{
						extra = capacVar.describe(context, ni);
						if (TextUtils.isANumber(extra))
						{
							double pureValue = TextUtils.atof(extra);
							extra = TextUtils.displayedUnits(pureValue, TextDescriptor.Unit.CAPACITANCE, TextUtils.UnitScale.NONE);
						}
					}
					writeTwoPort(ni, "C", extra, cni, netList, context);
				} else if (fun == PrimitiveNode.Function.INDUCT)
				{
					Variable inductVar = ni.getVar(Schematics.SCHEM_INDUCTANCE);
					String extra = "";
					if (inductVar != null)
					{
						extra = inductVar.describe(context, ni);
						if (TextUtils.isANumber(extra))
						{
							double pureValue = TextUtils.atof(extra);
							extra = TextUtils.displayedUnits(pureValue, TextDescriptor.Unit.INDUCTANCE, TextUtils.UnitScale.NONE);
						}
					}
					writeTwoPort(ni, "L", extra, cni, netList, context);
				} else if (fun == PrimitiveNode.Function.DIODE || fun == PrimitiveNode.Function.DIODEZ)
				{
					Variable diodeVar = ni.getVar(Schematics.SCHEM_DIODE);
					String extra = "";
					if (diodeVar != null)
						extra = diodeVar.describe(context, ni);
					if (extra.length() == 0) extra = "DIODE";
					writeTwoPort(ni, "D", extra, cni, netList, context);
				}
				continue;
			}

			// the default is to handle everything else as a transistor
			if (((PrimitiveNode)niProto).getGroupFunction() != PrimitiveNode.Function.TRANS)
				continue;

			Network gateNet = netList.getNetwork(ni.getTransistorGatePort());
			CellSignal gateCs = cni.getCellSignal(gateNet);
			Network sourceNet = netList.getNetwork(ni.getTransistorSourcePort());
			CellSignal sourceCs = cni.getCellSignal(sourceNet);
			Network drainNet = netList.getNetwork(ni.getTransistorDrainPort());
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
			if (fun == PrimitiveNode.Function.TRANSREF)					// self-referential transistor
			{
				modelChar = "X";
				biasCs = cni.getCellSignal(groundNet);
				modelName = niProto.getName();
			} else if (fun == PrimitiveNode.Function.TRANMOS)			// NMOS (Enhancement) transistor
			{
				modelChar = "M";
				biasCs = cni.getCellSignal(groundNet);
				if (modelName == null) modelName = "N";
			} else if (fun == PrimitiveNode.Function.TRA4NMOS)			// NMOS (Complementary) 4-port transistor
			{
				modelChar = "M";
				if (modelName == null) modelName = "N";
			} else if (fun == PrimitiveNode.Function.TRADMOS)			// DMOS (Depletion) transistor
			{
				modelChar = "M";
				biasCs = cni.getCellSignal(groundNet);
				if (modelName == null) modelName = "D";
			} else if (fun == PrimitiveNode.Function.TRA4DMOS)			// DMOS (Depletion) 4-port transistor
			{
				modelChar = "M";
				if (modelName == null) modelName = "D";
			} else if (fun == PrimitiveNode.Function.TRAPMOS)			// PMOS (Complementary) transistor
			{
				modelChar = "M";
				biasCs = cni.getCellSignal(powerNet);
				if (modelName == null) modelName = "P";
			} else if (fun == PrimitiveNode.Function.TRA4PMOS)			// PMOS (Complementary) 4-port transistor
			{
				modelChar = "M";
				if (modelName == null) modelName = "P";
			} else if (fun == PrimitiveNode.Function.TRANPN)			// NPN (Junction) transistor
			{
				modelChar = "Q";
//				biasn = subnet != NOSPNET ? subnet : 0;
				if (modelName == null) modelName = "NBJT";
			} else if (fun == PrimitiveNode.Function.TRA4NPN)			// NPN (Junction) 4-port transistor
			{
				modelChar = "Q";
				if (modelName == null) modelName = "NBJT";
			} else if (fun == PrimitiveNode.Function.TRAPNP)			// PNP (Junction) transistor
			{
				modelChar = "Q";
//				biasn = subnet != NOSPNET ? subnet : 0;
				if (modelName == null) modelName = "PBJT";
			} else if (fun == PrimitiveNode.Function.TRA4PNP)			// PNP (Junction) 4-port transistor
			{
				modelChar = "Q";
				if (modelName == null) modelName = "PBJT";
			} else if (fun == PrimitiveNode.Function.TRANJFET)			// NJFET (N Channel) transistor
			{
				modelChar = "J";
				biasCs = null;
				if (modelName == null) modelName = "NJFET";
			} else if (fun == PrimitiveNode.Function.TRA4NJFET)			// NJFET (N Channel) 4-port transistor
			{
				modelChar = "J";
				if (modelName == null) modelName = "NJFET";
			} else if (fun == PrimitiveNode.Function.TRAPJFET)			// PJFET (P Channel) transistor
			{
				modelChar = "J";
				biasCs = null;
				if (modelName == null) modelName = "PJFET";
			} else if (fun == PrimitiveNode.Function.TRA4PJFET)			// PJFET (P Channel) 4-port transistor
			{
				modelChar = "J";
				if (modelName == null) modelName = "PJFET";
			} else if (fun == PrimitiveNode.Function.TRADMES ||			// DMES (Depletion) transistor
				fun == PrimitiveNode.Function.TRA4DMES)					// DMES (Depletion) 4-port transistor
			{
				modelChar = "Z";
				biasCs = null;
				modelName = "DMES";
			} else if (fun == PrimitiveNode.Function.TRAEMES ||			// EMES (Enhancement) transistor
				fun == PrimitiveNode.Function.TRA4EMES)					// EMES (Enhancement) 4-port transistor
			{
				modelChar = "Z";
				biasCs = null;
				modelName = "EMES";
			} else if (fun == PrimitiveNode.Function.TRANS)				// special transistor
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
			TransistorSize size = ni.getTransistorSize(context);
			if (size.getDoubleWidth() > 0 || size.getDoubleLength() > 0)
			{
				double w = maskScale * size.getDoubleWidth();
				double l = maskScale * size.getDoubleLength();
				if (!Simulation.isSpiceWriteTransSizeInLambda())
				{
					// make into microns (convert to nanometers then divide by 1000)
					l *= layoutTechnology.getScale() / 1000.0;
					w *= layoutTechnology.getScale() / 1000.0;
				}

				if (fun == PrimitiveNode.Function.TRANMOS  || fun == PrimitiveNode.Function.TRA4NMOS ||
					fun == PrimitiveNode.Function.TRAPMOS || fun == PrimitiveNode.Function.TRA4PMOS ||
					fun == PrimitiveNode.Function.TRADMOS || fun == PrimitiveNode.Function.TRA4DMOS ||
					((fun == PrimitiveNode.Function.TRANJFET || fun == PrimitiveNode.Function.TRAPJFET ||
					  fun == PrimitiveNode.Function.TRADMES || fun == PrimitiveNode.Function.TRAEMES) &&
					  spiceEngine == Simulation.SPICE_ENGINE_H))
				{
                    // schematic transistors may be text
                    if ((size.getDoubleLength() == 0) && (size.getLength() instanceof String)) {
                        infstr.append(" L="+formatParam(trimSingleQuotes((String)size.getLength())));
                    } else {
                        infstr.append(" L=" + TextUtils.formatDouble(l, 2));
                        if (!Simulation.isSpiceWriteTransSizeInLambda()) infstr.append("U");
                    }
                    if ((size.getDoubleWidth() == 0) && (size.getWidth() instanceof String)) {
                        infstr.append(" W="+formatParam(trimSingleQuotes((String)size.getWidth())));
                    } else {
                        infstr.append(" W=" + TextUtils.formatDouble(w, 2));
                        if (!Simulation.isSpiceWriteTransSizeInLambda()) infstr.append("U");
                    }
				}
				if (fun != PrimitiveNode.Function.TRANMOS && fun != PrimitiveNode.Function.TRA4NMOS &&
					fun != PrimitiveNode.Function.TRAPMOS && fun != PrimitiveNode.Function.TRA4PMOS &&
					fun != PrimitiveNode.Function.TRADMOS && fun != PrimitiveNode.Function.TRA4DMOS)
				{
                    infstr.append(" AREA=" + TextUtils.formatDouble(l*w, 2));
                    if (!Simulation.isSpiceWriteTransSizeInLambda()) infstr.append("P");
				}
			} else {
                if ((size.getDoubleLength() == 0) && (size.getLength() instanceof String))
                    infstr.append(" L="+formatParam(trimSingleQuotes((String)size.getLength())));
                if ((size.getDoubleWidth() == 0) && (size.getWidth() instanceof String))
                    infstr.append(" W="+formatParam(trimSingleQuotes((String)size.getWidth())));
            }

			// make sure transistor is connected to nets
			SpiceNet spNetGate = (SpiceNet)spiceNetMap.get(gateNet);
			SpiceNet spNetSource = (SpiceNet)spiceNetMap.get(sourceNet);
			SpiceNet spNetDrain = (SpiceNet)spiceNetMap.get(drainNet);
			if (spNetGate == null || spNetSource == null || spNetDrain == null) continue;

			// compute area of source and drain
			if (!useCDL)
			{
				if (fun == PrimitiveNode.Function.TRANMOS || fun == PrimitiveNode.Function.TRA4NMOS ||
					fun == PrimitiveNode.Function.TRAPMOS || fun == PrimitiveNode.Function.TRA4PMOS ||
					fun == PrimitiveNode.Function.TRADMOS || fun == PrimitiveNode.Function.TRA4DMOS)
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
					if (as > 0.0)
					{
						infstr.append(" AS=" + TextUtils.formatDouble(as, 2));
						if (!Simulation.isSpiceWriteTransSizeInLambda()) infstr.append("P");
					}
					if (ad > 0.0)
					{
						infstr.append(" AD=" + TextUtils.formatDouble(ad, 2));
						if (!Simulation.isSpiceWriteTransSizeInLambda()) infstr.append("P");
					}
					if (ps > 0.0)
					{
						infstr.append(" PS=" + TextUtils.formatDouble(ps, 2));
						if (!Simulation.isSpiceWriteTransSizeInLambda()) infstr.append("U");
					}
					if (pd > 0.0)
					{
						infstr.append(" PD=" + TextUtils.formatDouble(pd, 2));
						if (!Simulation.isSpiceWriteTransSizeInLambda()) infstr.append("U");
					}
				}
			}
            // Writing MFactor if available.
            writeMFactor(context, ni, infstr);

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
					Network net = cs.getNetwork();
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

    /**
     * Check if the specified cell is parameterized. Note that this
     * recursively checks all cells below this cell as well, and marks
     * all cells that contain LE gates, or whose subcells contain LE gates,
     * as parameterized.
     * @return true if cell has been marked as parameterized
     */
    private boolean checkIfParameterized(Cell cell) {
        //System.out.println("Checking Cell "+cell.describe());
        boolean mark = false;
        for (Iterator it = cell.getNodes(); it.hasNext(); ) {
            NodeInst ni = (NodeInst)it.next();
            if (!(ni.getProto() instanceof Cell)) continue;
            if (ni.isIconOfParent()) continue;
            if (ni.getVar("ATTR_LEGATE") != null) { mark = true; continue; }
            if (ni.getVar("ATTR_LEKEEPER") != null) { mark = true; continue; }
            Cell proto = ((Cell)ni.getProto()).contentsView();
            if (proto == null) proto = (Cell)ni.getProto();
            if (checkIfParameterized(proto)) { mark = true; }
        }
        if (mark)
            uniquifyCells.put(cell, cell);
        //System.out.println("---> "+cell.describe()+" is marked "+mark);
        return mark;
    }

    /*
     * Method to create a parameterized name for node instance "ni".
     * If the node is not parameterized, returns zero.
     * If it returns a name, that name must be deallocated when done.
     */
    protected String parameterizedName(Nodable no, VarContext context)
    {
        Cell cell = (Cell)no.getProto();
        StringBuffer uniqueCellName = new StringBuffer(getUniqueCellName(cell));

        if (uniquifyCells.get(cell) != null) {
            // if this cell is marked to be make unique, make a unique name out of the var context
            VarContext vc = context.push(no);
            uniqueCellName.append("_"+vc.getInstPath("."));
        } else {
            if (canParameterizeNames() &&
                no.getProto() instanceof Cell)
            {
                // if there are parameters, append them to this name
                HashMap paramValues = new HashMap();
                for(Iterator it = no.getVariables(); it.hasNext(); )
                {
                    Variable var = (Variable)it.next();
                    if (!var.getTextDescriptor().isParam()) continue;
                    paramValues.put(var.getKey(), var);
                }
                for(Iterator it = paramValues.keySet().iterator(); it.hasNext(); )
                {
                    Variable.Key key = (Variable.Key)it.next();
                    Variable var = no.getVar(key.getName());
                    String eval = var.describe(context, no.getNodeInst());
                    //Object eval = context.evalVar(var, no);
                    if (eval == null) continue;
                    //uniqueCellName += "-" + var.getTrueName() + "-" + eval.toString();
                    uniqueCellName.append("-" + eval.toString());
                }
            }
        }

        // if it is over the length limit, truncate it
        int limit = maxNameLength();
        if (limit > 0 && uniqueCellName.length() > limit)
        {
            Integer i = (Integer)uniqueNames.get(uniqueCellName.toString());
            if (i == null) {
                i = new Integer(uniqueID);
                uniqueID++;
                uniqueNames.put(uniqueCellName.toString(), i);
            }
            uniqueCellName = uniqueCellName.delete(limit-10, uniqueCellName.length());
            uniqueCellName.append("-ID"+i);
        }

        // make it safe
        return getSafeCellName(uniqueCellName.toString());
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

	/** Method to return the proper name of Power */
	protected String getPowerName() { return "vdd"; }

	/** Method to return the proper name of Ground */
	protected String getGroundName() { return "gnd"; }

	/** Method to return the proper name of a Global signal */
	protected String getGlobalName(Global glob) { return glob.getName(); }

    /** Method to report that export names do NOT take precedence over
     * arc names when determining the name of the network. */
    protected boolean isNetworksUseExportedNames() { return true; }

	/** Method to report that library names are NOT always prepended to cell names. */
	protected boolean isLibraryNameAlwaysAddedToCellName() { return false; }

	/** Method to report that aggregate names (busses) are not used. */
	protected boolean isAggregateNamesSupported() { return false; }

    /** If the netlister has requirments not to netlist certain cells and their
     * subcells, override this method.
     * If this cell has a spice template, skip it
     */
    protected boolean skipCellAndSubcells(Cell cell) {
        Variable varTemplate = cell.getVar(preferedEgnineTemplateKey);
        if (varTemplate == null)
            varTemplate = cell.getVar(SPICE_TEMPLATE_KEY);
        if (varTemplate != null) return true;
        return false;
    }

	/**
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

	/**
	 * write a header for "cell" to spice deck "sim_spice_file"
	 * The model cards come from a file specified by tech:~.SIM_spice_model_file
	 * or else tech:~.SIM_spice_header_level%ld
	 * The spice model file can be located in el_libdir
	 */
	private void writeHeader(Cell cell)
	{
		// Print the header line for SPICE 
		multiLinePrint(true, "*** SPICE deck for cell " + cell.noLibDescribe() +
			" from library " + cell.getLibrary().getName() + "\n");
		emitCopyright("*** ", "");
		if (User.isIncludeDateAndVersionInOutput())
		{
			multiLinePrint(true, "*** Created on " + TextUtils.formatDate(topCell.getCreationDate()) + "\n");
			multiLinePrint(true, "*** Last revised on " + TextUtils.formatDate(topCell.getRevisionDate()) + "\n");
			multiLinePrint(true, "*** Written on " + TextUtils.formatDate(new Date()) +
				" by Electric VLSI Design System, version " + Version.getVersion() + "\n");
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
           if (headerFile.startsWith(SPICE_EXTENSION_PREFIX))
			{
				// extension specified: look for a file with the cell name and that extension
				String headerPath = TextUtils.getFilePath(cell.getLibrary().getLibFile());
                String ext = headerFile.substring(SPICE_EXTENSION_PREFIX.length());
                if (ext.startsWith(".")) ext = ext.substring(1);
				String filePart = cell.getName() + "." + ext;
				String fileName = headerPath + filePart;
				File test = new File(fileName);
				if (test.exists())
				{
					multiLinePrint(true, "* Model cards are described in this file:\n");
					addIncludeFile(filePart);
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

	/**
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
			if (trailerFile.startsWith(SPICE_EXTENSION_PREFIX))
			{
				// extension specified: look for a file with the cell name and that extension
				String trailerpath = TextUtils.getFilePath(cell.getLibrary().getLibFile());
				String filePart = cell.getName() + "." + trailerFile.substring(SPICE_EXTENSION_PREFIX.length());
				String fileName = trailerpath + filePart;
				File test = new File(fileName);
				if (test.exists())
				{
					multiLinePrint(true, "* Trailer cards are described in this file:\n");
					addIncludeFile(filePart);
				}
			} else
			{
				// normal trailer file specified
				multiLinePrint(true, "* Trailer cards are described in this file:\n");
				addIncludeFile(trailerFile);
			}
		}
	}

	/**
	 * Function to write a two port device to the file. Complain about any missing connections.
	 * Determine the port connections from the portprotos in the instance
	 * prototype. Get the part number from the 'part' number value;
	 * increment it. The type of device is declared in type; extra is the string
	 * data acquired before calling here.
	 * If the device is connected to the same net at both ends, do not
	 * write it. Is this OK?
	 */
	private void writeTwoPort(NodeInst ni, String partName, String extra, CellNetInfo cni, Netlist netList, VarContext context)
	{
		PortInst port0 = ni.getPortInst(0);
		PortInst port1 = ni.getPortInst(1);
		Network net0 = netList.getNetwork(port0);
		Network net1 = netList.getNetwork(port1);
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

        // add Mfactor if there
        StringBuffer sbExtra = new StringBuffer(extra);
        writeMFactor(context, ni, sbExtra);

		multiLinePrint(false, partName + " " + cs1.getName() + " " + cs0.getName() + " " + sbExtra.toString() + "\n");
	}

    /**
     * This adds formatting to a Spice parameter value.  It adds single quotes
     * around the param string if they do not already exist.
     * @param param the string param value (without the name= part).
     * @return a param string with single quotes around it
     */
    private static String formatParam(String param) {
        if (param.endsWith("'") || param.startsWith("'")) return param;
        return ("'"+param+"'");
    }

    private static String trimSingleQuotes(String param) {
        if (param.startsWith("'") && param.endsWith("'")) {
            return param.substring(1, param.length()-1);
        }
        return param;
    }

	/******************** PARASITIC CALCULATIONS ********************/

	/**
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

		PrimitiveNode.Function function = ni.getFunction();

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
			Network net = netList.getNetwork(ni, pp, 0);

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

	/**
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

	/**
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

	/**
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

	/**
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

	/**
	 * Method to report an error that is built in the infinite string.
	 * The error is sent to the messages window and also to the SPICE deck "f".
	 */
	private void dumpErrorMessage(String message)
	{
		multiLinePrint(true, "*** " + message + "\n");
		System.out.println(message);
	}

	/**
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
