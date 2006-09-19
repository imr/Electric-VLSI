/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Spice.java
 * Original C Code written by Steven M. Rubin and Sid Penstone
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

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.geometry.PolyMerge;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.*;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.Simulate;
import com.sun.electric.tool.io.input.SpiceNetlistReader;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.Exec;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.ExecDialog;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.waveform.WaveformWindow;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations.NamePattern;
import com.sun.electric.tool.logicaleffort.LENetlister;

import java.awt.geom.AffineTransform;
import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * This is the Simulation Interface tool.
 */
public class Spice extends Topology
{
	/** key of Variable holding generic Spice templates. */		public static final Variable.Key SPICE_TEMPLATE_KEY = Variable.newKey("ATTR_SPICE_template");
	/** key of Variable holding Spice 2 templates. */			public static final Variable.Key SPICE_2_TEMPLATE_KEY = Variable.newKey("ATTR_SPICE_template_spice2");
	/** key of Variable holding Spice 3 templates. */			public static final Variable.Key SPICE_3_TEMPLATE_KEY = Variable.newKey("ATTR_SPICE_template_spice3");
	/** key of Variable holding HSpice templates. */			public static final Variable.Key SPICE_H_TEMPLATE_KEY = Variable.newKey("ATTR_SPICE_template_hspice");
	/** key of Variable holding PSpice templates. */			public static final Variable.Key SPICE_P_TEMPLATE_KEY = Variable.newKey("ATTR_SPICE_template_pspice");
	/** key of Variable holding GnuCap templates. */			public static final Variable.Key SPICE_GC_TEMPLATE_KEY = Variable.newKey("ATTR_SPICE_template_gnucap");
	/** key of Variable holding Smart Spice templates. */		public static final Variable.Key SPICE_SM_TEMPLATE_KEY = Variable.newKey("ATTR_SPICE_template_smartspice");
	/** key of Variable holding Smart Spice templates. */		public static final Variable.Key SPICE_A_TEMPLATE_KEY = Variable.newKey("ATTR_SPICE_template_assura");
	/** key of Variable holding Smart Spice templates. */		public static final Variable.Key SPICE_C_TEMPLATE_KEY = Variable.newKey("ATTR_SPICE_template_calibre");
	/** key of Variable holding SPICE code. */					public static final Variable.Key SPICE_CARD_KEY = Variable.newKey("SIM_spice_card");
	/** key of Variable holding SPICE declaration. */			public static final Variable.Key SPICE_DECLARATION_KEY = Variable.newKey("SIM_spice_declaration");
	/** key of Variable holding SPICE model. */					public static final Variable.Key SPICE_MODEL_KEY = Variable.newKey("SIM_spice_model");
	/** key of Variable holding SPICE flat code. */				public static final Variable.Key SPICE_CODE_FLAT_KEY = Variable.newKey("SIM_spice_code_flat");
    /** key of wire capacitance. */                             public static final Variable.Key ATTR_C = Variable.newKey("ATTR_C");
    /** key of wire resistance. */                              public static final Variable.Key ATTR_R = Variable.newKey("ATTR_R");
	/** Prefix for spice extension. */                          public static final String SPICE_EXTENSION_PREFIX = "Extension ";

    /** key of Variable holding generic CDL templates. */		public static final Variable.Key CDL_TEMPLATE_KEY = Variable.newKey("ATTR_CDL_template");

	/** maximum subcircuit name length */						private static final int SPICEMAXLENSUBCKTNAME     = 70;
    /** maximum subcircuit name length */						private static final int CDLMAXLENSUBCKTNAME     = 40;
    /** maximum subcircuit name length */						private static final int SPICEMAXLENLINE     = 78;
	/** legal characters in a spice deck */						private static final String SPICELEGALCHARS        = "!#$%*+-/<>[]_@";
	/** legal characters in a spice deck */						private static final String PSPICELEGALCHARS        = "!#$%*+-/<>[]_";
	/** legal characters in a CDL deck */						private static final String CDLNOBRACKETLEGALCHARS = "!#$%*+-/<>_";
    /** if CDL writes out empty subckt definitions */           private static final boolean CDLWRITESEMPTYSUBCKTS = false;
    /** if use spice globals */                                 private static final boolean USE_GLOBALS = true;

	/** default Technology to use. */				private Technology layoutTechnology;
	/** Mask shrink factor (default =1) */			private double  maskScale;
	/** True to write CDL format */					private boolean useCDL;
	/** Legal characters */							private String legalSpiceChars;
	/** Template Key for current spice engine */	private Variable.Key preferedEngineTemplateKey;
    /** Special case for HSpice for Assura */       private boolean assuraHSpice = false;
	/** Spice type: 2, 3, H, P, etc */				private Simulation.SpiceEngine spiceEngine;
	/** those cells that have overridden models */	private HashMap<Cell,String> modelOverrides = new HashMap<Cell,String>();
    /** List of segmented nets and parasitics */    private List<SegmentedNets> segmentedParasiticInfo = new ArrayList<SegmentedNets>();
    /** Networks exempted during parasitic ext */   private ExemptedNets exemptedNets;
    /** Whether or not to write empty subckts  */   private boolean writeEmptySubckts = true;
    /** max length per line */                      private int spiceMaxLenLine = SPICEMAXLENLINE;

    /** Flat measurements file */                   private FlatSpiceCodeVisitor spiceCodeFlat = null;

    /** map of "parameterized" cells that are not covered by Topology */    private Map<Cell,Cell> uniquifyCells;
    /** uniqueID */                                                         private int uniqueID;
    /** map of shortened instance names */                                  private Map<String,Integer> uniqueNames;

    private static final boolean useNewParasitics = true;

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
        private FileType type;
        private String file;
        private SpiceFinishedListener(Cell cell, FileType type, String file) {
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
     * @param context the hierarchical context to the cell.
	 * @param filePath the disk file to create.
	 * @param cdl true if this is CDL output (false for Spice).
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
			out.printWriter.print("    'cdlFile                \"" + deckPath + File.separator + deckFile + "\"\n");
			out.printWriter.print("    'userSkillFile          \"\"\n");
			out.printWriter.print("    'opusLib                \"" + libName + "\"\n");
			out.printWriter.print("    'primaryCell            \"" + cell.getName() + "\"\n");
			out.printWriter.print("    'caseSensitivity        \"lower\"\n");
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
            FileType type = Simulate.getCurrentSpiceOutputType();
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

        if (Simulation.isParasiticsBackAnnotateLayout() && Simulation.isSpiceUseParasitics()) {
            out.backAnnotateLayout();
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
        if (topCell.getTechnology().isLayout())
            layoutTechnology = topCell.getTechnology();
        else
            layoutTechnology = Schematics.getDefaultSchematicTechnology();

		// make sure key is cached
		spiceEngine = Simulation.getSpiceEngine();
		preferedEngineTemplateKey = SPICE_TEMPLATE_KEY;
        assuraHSpice = false;
		switch (spiceEngine)
		{
			case SPICE_ENGINE_2: preferedEngineTemplateKey = SPICE_2_TEMPLATE_KEY;   break;
			case SPICE_ENGINE_3: preferedEngineTemplateKey = SPICE_3_TEMPLATE_KEY;   break;
			case SPICE_ENGINE_H: preferedEngineTemplateKey = SPICE_H_TEMPLATE_KEY;   break;
			case SPICE_ENGINE_P: preferedEngineTemplateKey = SPICE_P_TEMPLATE_KEY;   break;
			case SPICE_ENGINE_G: preferedEngineTemplateKey = SPICE_GC_TEMPLATE_KEY;   break;
			case SPICE_ENGINE_S: preferedEngineTemplateKey = SPICE_SM_TEMPLATE_KEY;   break;
            case SPICE_ENGINE_H_ASSURA: preferedEngineTemplateKey = SPICE_A_TEMPLATE_KEY;  assuraHSpice = true; break;
            case SPICE_ENGINE_H_CALIBRE: preferedEngineTemplateKey = SPICE_C_TEMPLATE_KEY; assuraHSpice = true; break;
		}
        if (assuraHSpice || (useCDL && !CDLWRITESEMPTYSUBCKTS) ||
            (!useCDL && !Simulation.isSpiceWriteEmtpySubckts())) {
            writeEmptySubckts = false;
        }

		// get the mask scale
		maskScale = 1.0;
//		Variable scaleVar = layoutTechnology.getVar("SIM_spice_mask_scale");
//		if (scaleVar != null) maskScale = TextUtils.atof(scaleVar.getObject().toString());

        // set up the parameterized cells
        uniquifyCells = new HashMap<Cell,Cell>();
        uniqueID = 0;
        uniqueNames = new HashMap<String,Integer>();
        checkIfParameterized(topCell);

		// setup the legal characters
		legalSpiceChars = SPICELEGALCHARS;
		if (spiceEngine == Simulation.SpiceEngine.SPICE_ENGINE_P ||
			spiceEngine == Simulation.SpiceEngine.SPICE_ENGINE_G) legalSpiceChars = PSPICELEGALCHARS;

		// start writing the spice deck
		if (useCDL)
		{
			// setup bracket conversion for CDL
			if (Simulation.isCDLConvertBrackets())
				legalSpiceChars = CDLNOBRACKETLEGALCHARS;

			multiLinePrint(true, "* First line is ignored\n");
            // see if include file specified
            String headerPath = TextUtils.getFilePath(topCell.getLibrary().getLibFile());
            String filePart = Simulation.getCDLIncludeFile();
            if (!filePart.equals("")) {
                String fileName = headerPath + filePart;
                File test = new File(fileName);
                if (test.exists())
                {
                    multiLinePrint(true, "* Primitives described in this file:\n");
                    addIncludeFile(filePart);
                } else {
                    System.out.println("Warning: CDL Include file not found: "+fileName);
                }
            }
		} else
		{
			writeHeader(topCell);
            spiceCodeFlat = new FlatSpiceCodeVisitor(filePath+".flatcode", this);
            HierarchyEnumerator.enumerateCell(topCell, VarContext.globalContext, spiceCodeFlat, true);
            spiceCodeFlat.close();
		}

        if (Simulation.isParasiticsUseExemptedNetsFile()) {
            String headerPath = TextUtils.getFilePath(topCell.getLibrary().getLibFile());
            exemptedNets = new ExemptedNets(new File(headerPath + File.separator + "exemptedNets.txt"));
        }

		// gather all global signal names
/*
		if (USE_GLOBALS)
		{
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
						Global global = globals.get(i);
						String name = global.getName();
						if (global == Global.power) { if (getPowerName(null) != null) name = getPowerName(null); }
						if (global == Global.ground) { if (getGroundName(null) != null) name = getGroundName(null); }
						infstr.append(" " + name);
					}
					infstr.append("\n");
					multiLinePrint(false, infstr.toString());
				}
			}
		}
*/
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
        Variable mVar = no.getVar(Simulation.M_FACTOR_KEY);
        if (mVar == null) return;
        Object value = context.evalVar(mVar);

        // check for M=@M, and warn user that this is a bad idea, and we will not write it out
        if (mVar.getObject().toString().equals("@M") || (mVar.getObject().toString().equals("P(\"M\")"))) {
            System.out.println("Warning: M=@M [eval="+value+"] on "+no.getName()+" is a bad idea, not writing it out: "+context.push(no).getInstPath("."));
            return;
        }

        infstr.append(" M=" + formatParam(value.toString()));
    }

    /** Called at the end of the enter cell phase of hierarchy enumeration */
    protected void enterCell(HierarchyEnumerator.CellInfo info) {
        if (exemptedNets != null)
            exemptedNets.setExemptedNets(info);
    }

    private Variable getEngineTemplate(Cell cell)
    {
        Variable varTemplate = null;

        varTemplate = cell.getVar(preferedEngineTemplateKey);
        if (!assuraHSpice) // null in case of NO_TEMPLATE_KEY
        {
            if (varTemplate == null)
                varTemplate = cell.getVar(SPICE_TEMPLATE_KEY);
        }
        return varTemplate;
    }

	/**
	 * Method to write cellGeom
	 */
	protected void writeCellTopology(Cell cell, CellNetInfo cni, VarContext context, Topology.MyCellInfo info)
	{
		if (cell == topCell && USE_GLOBALS) {
            Netlist netList = cni.getNetList();
            Global.Set globals = netList.getGlobals();
            int globalSize = globals.size();
            if (!Simulation.isSpiceUseNodeNames() || spiceEngine != Simulation.SpiceEngine.SPICE_ENGINE_3)
            {
                if (globalSize > 0)
                {
                    StringBuffer infstr = new StringBuffer();
                    infstr.append("\n.global");
                    for(int i=0; i<globalSize; i++)
                    {
                        Global global = globals.get(i);
                        String name = global.getName();
                        if (global == Global.power) { if (getPowerName(null) != null) name = getPowerName(null); }
                        if (global == Global.ground) { if (getGroundName(null) != null) name = getGroundName(null); }
                        infstr.append(" " + name);
                    }
                    infstr.append("\n");
                    multiLinePrint(false, infstr.toString());
                }
            }
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
		HashMap<Network,SpiceNet> spiceNetMap = new HashMap<Network,SpiceNet>();

        // create SpiceNet objects for all networks in the cell
        for(Iterator<Network> it = netList.getNetworks(); it.hasNext(); )
        {
            Network net = it.next();
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

        // create list of segemented networks for parasitic extraction
        boolean verboseSegmentedNames = Simulation.isParasiticsUseVerboseNaming();
        boolean useParasitics = useNewParasitics && (!useCDL) &&
                Simulation.isSpiceUseParasitics() && (cell.getView() == View.LAYOUT);
        SegmentedNets segmentedNets = new SegmentedNets(cell, verboseSegmentedNames, cni, useParasitics);
        segmentedParasiticInfo.add(segmentedNets);

        if (useParasitics) {

            double scale = layoutTechnology.getScale(); // scale to convert units to nanometers
            //System.out.println("\n     Finding parasitics for cell "+cell.describe(false));
            HashMap<Network,Network> exemptedNetsFound = new HashMap<Network,Network>();
            for (Iterator<ArcInst> ait = cell.getArcs(); ait.hasNext(); ) {
                ArcInst ai = ait.next();
                boolean ignoreArc = false;

                // figure out res and cap, see if we should ignore it
                if (ai.getProto().getFunction() == ArcProto.Function.NONELEC)
                    ignoreArc = true;
                double length = ai.getLength() * scale / 1000;      // length in microns
                double width = ai.getWidth() * scale / 1000;        // width in microns
                double area = length * width;
                double fringe = length*2;
                double cap = 0;
                double res = 0;

                Technology tech = ai.getProto().getTechnology();
                Poly [] arcInstPolyList = tech.getShapeOfArc(ai);
                int tot = arcInstPolyList.length;
                for(int j=0; j<tot; j++)
                {
                    Poly poly = arcInstPolyList[j];
                    if (poly.getStyle().isText()) continue;

                    Layer layer = poly.getLayer();
                    if (layer.getTechnology() != layoutTechnology) continue;
                    if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;

                    if (!layer.isDiffusionLayer() && layer.getCapacitance() > 0.0) {
                        double areacap = area * layer.getCapacitance();
                        double fringecap = fringe * layer.getEdgeCapacitance();
                        cap = areacap + fringecap;
                        res = length/width * layer.getResistance();
                    }
                }
                // add res if big enough
                if (res <= cell.getTechnology().getMinResistance()) {
                    ignoreArc = true;
                }

                if (Simulation.isParasiticsUseExemptedNetsFile()) {
                    Network net = netList.getNetwork(ai, 0);
                    // ignore nets in exempted nets file
                    if (Simulation.isParasiticsIgnoreExemptedNets()) {
                        // check if this net is exempted
                        if (exemptedNets.isExempted(info.getNetID(net))) {
                            ignoreArc = true;
                            cap = 0;
                            if (!exemptedNetsFound.containsKey(net)) {
                                System.out.println("Not extracting net "+cell.describe(false)+" "+net.getName());
                                exemptedNetsFound.put(net, net);
                                cap = exemptedNets.getReplacementCap(cell, net);
                            }
                        }
                    // extract only nets in exempted nets file
                    } else {
                        if (exemptedNets.isExempted(info.getNetID(net)) && ignoreArc == false) {
                            if (!exemptedNetsFound.containsKey(net)) {
                                System.out.println("Extracting net "+cell.describe(false)+" "+net.getName());
                                exemptedNetsFound.put(net, net);
                            }
                        } else {
                            ignoreArc = true;
                        }
                    }
                }

                int arcPImodels = SegmentedNets.getNumPISegments(res, layoutTechnology.getMaxSeriesResistance());

                if (ignoreArc)
                    arcPImodels = 1;                        // split cap to two pins if ignoring arc

                // add caps
                segmentedNets.putSegment(ai.getHeadPortInst(), cap/(arcPImodels+1));
                segmentedNets.putSegment(ai.getTailPortInst(), cap/(arcPImodels+1));

                if (ignoreArc) {
                    // short arc
                    segmentedNets.shortSegments(ai.getHeadPortInst(), ai.getTailPortInst());
                } else {
                    segmentedNets.addArcRes(ai, res);
                    if (arcPImodels > 1)
                        segmentedNets.addArcCap(ai, cap);       // need to store cap later to break it up
                }
            }
            // Don't take into account gate resistance: so we need to short two PortInsts
            // of gate together if this is layout
            for(Iterator<NodeInst> aIt = cell.getNodes(); aIt.hasNext(); )
            {
                NodeInst ni = aIt.next();
                if (!ni.isCellInstance()) {
                    if (((PrimitiveNode)ni.getProto()).getGroupFunction() == PrimitiveNode.Function.TRANS) {
                        PortInst gate0 = ni.getTransistorGatePort();
                        PortInst gate1 = null;
                        for (Iterator<PortInst> pit = ni.getPortInsts(); pit.hasNext();) {
                            PortInst p2 = pit.next();
                            if (p2 != gate0 && netList.getNetwork(gate0) == netList.getNetwork(p2))
                                gate1 = p2;
                        }
                        if (gate1 != null) {
                            segmentedNets.shortSegments(gate0, gate1);
                        }
                    }
                } else {
                    //System.out.println("Shorting shorted exports");
                    // short together pins if shorted by subcell
                    Cell subCell = (Cell)ni.getProto();
                    SegmentedNets subNets = getSegmentedNets(subCell);
                    // list of lists of shorted exports
                    if (subNets != null) {      // subnets may be null if mixing schematics with layout technologies
                        for (Iterator<List<String>> it = subNets.getShortedExports(); it.hasNext(); ) {
                            List<String> exports = it.next();
                            PortInst pi1 = null;
                            // list of exports shorted together
                            for (String exportName : exports) {
                                // get portinst on node
                                PortInst pi = ni.findPortInst(exportName);
                                if (pi1 == null) {
                                    pi1 = pi; continue;
                                }
                                segmentedNets.shortSegments(pi1, pi);
                            }
                        }
                    }
                }
            } // for (cell.getNodes())
        }

		// count the number of different transistor types
		int bipolarTrans = 0, nmosTrans = 0, pmosTrans = 0;
		for(Iterator<NodeInst> aIt = cell.getNodes(); aIt.hasNext(); )
		{
			NodeInst ni = aIt.next();
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
		for(Iterator<ArcInst> aIt = cell.getArcs(); aIt.hasNext(); )
		{
			ArcInst ai = aIt.next();

			// don't count non-electrical arcs
			if (ai.getProto().getFunction() == ArcProto.Function.NONELEC) continue;

			// ignore busses
//			if (ai->network->buswidth > 1) continue;
			Network net = netList.getNetwork(ai, 0);
			SpiceNet spNet = spiceNetMap.get(net);
			if (spNet == null) continue;

			addArcInformation(spNet.merge, ai);
		}

		// get merged polygons so far
		for(Iterator<Network> it = netList.getNetworks(); it.hasNext(); )
		{
			Network net = it.next();
			SpiceNet spNet = spiceNetMap.get(net);

            for (Layer layer : spNet.merge.getKeySet())
			{
				List<PolyBase> polyList = spNet.merge.getMergedPoints(layer, true);
				if (polyList == null) continue;
                if (polyList.size() > 1)
                    Collections.sort(polyList, GeometryHandler.shapeSort);
				for(PolyBase poly : polyList)
				{
					// compute perimeter and area
					double perim = poly.getPerimeter();
					double area = poly.getArea();

					// accumulate this information
                    double scale = layoutTechnology.getScale(); // scale to convert units to nanometers
                    if (layer.isDiffusionLayer()) {
                        spNet.diffArea += area * maskScale * maskScale;
                        spNet.diffPerim += perim * maskScale;
                    } else {
                        area = area * scale * scale / 1000000; // area in square microns
                        perim = perim * scale / 1000;           // perim in microns
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
			String message = "WARNING: no power connection for P-transistor wells in " + cell;
			dumpErrorMessage(message);
		}
		if (nmosTrans != 0 && groundNet == null)
		{
			String message = "WARNING: no ground connection for N-transistor wells in " + cell;
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
        String topLevelInstance = "";
		if (cell == topCell && !useCDL && !Simulation.isSpiceWriteSubcktTopCell())
		{
			multiLinePrint(true, "\n*** TOP LEVEL CELL: " + cell.describe(false) + "\n");
		} else
		{
            if (!writeEmptySubckts) {
                if (cellIsEmpty(cell))
                    return;
            }

			String cellName = cni.getParameterizedName();
			multiLinePrint(true, "\n*** CELL: " + cell.describe(false) + "\n");
			StringBuffer infstr = new StringBuffer();
			infstr.append(".SUBCKT " + cellName);
			for(Iterator<CellSignal> sIt = cni.getCellSignals(); sIt.hasNext(); )
			{
				CellSignal cs = sIt.next();
                if (ignoreSubcktPort(cs)) continue;

                if (cs.isGlobal()) {
                    System.out.println("Warning: Explicit Global signal "+cs.getName()+" exported in "+cell.describe(false));
                }

                // special case for parasitic extraction
                if (useParasitics && !cs.isGlobal() && cs.getExport() != null) {
                    Network net = cs.getNetwork();
                    HashMap<String,List<String>> shortedExportsMap = new HashMap<String,List<String>>();
                    // For a single logical network, we need to:
                    // 1) treat certain exports as separate so as not to short resistors on arcs between the exports
                    // 2) join certain exports that do not have resistors on arcs between them, and record this information
                    //   so that the next level up knows to short networks connection to those exports.
                    for (Iterator<Export> it = net.getExports(); it.hasNext(); ) {
                        Export e = it.next();
                        PortInst pi = e.getOriginalPort();
                        String name = segmentedNets.getNetName(pi);
                        // exports are shorted if their segmented net names are the same (case (2))
                        List<String> shortedExports = shortedExportsMap.get(name);
                        if (shortedExports == null) {
                            shortedExports = new ArrayList<String>();
                            shortedExportsMap.put(name, shortedExports);
                            // this is the first occurance of this segmented network,
                            // use the name as the export (1)
                            infstr.append(" " + name);
                        }
                        shortedExports.add(e.getName());
                    }
                    // record shorted exports
                    for (List<String> shortedExports : shortedExportsMap.values()) {
                        if (shortedExports.size() > 1) {
                            segmentedNets.addShortedExports(shortedExports);
                        }
                    }
                } else {
                    infstr.append(" " + cs.getName());
                }
			}

			Global.Set globals = netList.getGlobals();
			int globalSize = globals.size();
			if (USE_GLOBALS)
			{
				if (!Simulation.isSpiceUseNodeNames() || spiceEngine == Simulation.SpiceEngine.SPICE_ENGINE_3)
				{
					for(int i=0; i<globalSize; i++)
					{
						Global global = globals.get(i);
						Network net = netList.getNetwork(global);
						CellSignal cs = cni.getCellSignal(net);
						infstr.append(" " + cs.getName());
					}
				}
			}
            if (cell == topCell && Simulation.isSpiceWriteSubcktTopCell()) {
                // create top level instantiation
                topLevelInstance = infstr.toString().replaceFirst("\\.SUBCKT ", "X") + " " + cellName;
            }
			if (!useCDL && Simulation.isSpiceUseCellParameters())
			{
				// add in parameters to this cell
				for(Iterator<Variable> it = cell.getParameters(); it.hasNext(); )
				{
					Variable paramVar = it.next();
                    if (paramVar.getCode() != TextDescriptor.Code.SPICE) continue;
                    infstr.append(" " + paramVar.getTrueName() + "=" + paramVar.getPureValue(-1));
				}
//				for(Iterator it = cell.getVariables(); it.hasNext(); )
//				{
//					Variable paramVar = it.next();
//					if (!paramVar.isParam()) continue;
//					infstr.append(" " + paramVar.getTrueName() + "=" + paramVar.getPureValue(-1));
//				}
			}
            // Writing M factor
            //writeMFactor(cell, infstr);
			infstr.append("\n");
			multiLinePrint(false, infstr.toString());

			// generate pin descriptions for reference (when not using node names)
			for(int i=0; i<globalSize; i++)
			{
				Global global = globals.get(i);
				Network net = netList.getNetwork(global);
				CellSignal cs = cni.getCellSignal(net);
				multiLinePrint(true, "** GLOBAL " + cs.getName() + "\n");
			}

			// write exports to this cell
			for(Iterator<CellSignal> sIt = cni.getCellSignals(); sIt.hasNext(); )
			{
				CellSignal cs = sIt.next();

				// ignore networks that aren't exported
				PortProto pp = cs.getExport();
				if (pp == null) continue;

				if (cs.isGlobal()) continue;
				//multiLinePrint(true, "** PORT " + cs.getName() + "\n");
			}
		}

		// write out any directly-typed SPICE declaratons for the cell
		if (!useCDL)
		{
			boolean firstDecl = true;
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				if (ni.getProto() != Generic.tech.invisiblePinNode) continue;
				Variable cardVar = ni.getVar(SPICE_DECLARATION_KEY);
				if (cardVar == null) continue;
				if (firstDecl)
				{
					firstDecl = false;
					multiLinePrint(true, "\n* Spice Declaration nodes in cell " + cell + "\n");
				}
				emitEmbeddedSpice(cardVar, context, segmentedNets, info, false);
			}
		}

		// third pass through the node list, print it this time
		for(Iterator<Nodable> nIt = netList.getNodables(); nIt.hasNext(); )
		{
			Nodable no = nIt.next();
			NodeProto niProto = no.getProto();

			// handle sub-cell calls
			if (no.isCellInstance())
			{
				Cell subCell = (Cell)niProto;

				// look for a SPICE template on the prototype
				Variable varTemplate = null;
				if (useCDL)
				{
					varTemplate = subCell.getVar(CDL_TEMPLATE_KEY);
				} else
				{
                    varTemplate = getEngineTemplate(subCell);
//					varTemplate = subCell.getVar(preferedEngineTemplateKey);
//					if (varTemplate == null)
//						varTemplate = subCell.getVar(SPICE_TEMPLATE_KEY);
				}

				// handle templates
				if (varTemplate != null)
				{
					if (varTemplate.getObject() instanceof Object[])
					{
						Object [] manyLines = (Object [])varTemplate.getObject();
						for(int i=0; i<manyLines.length; i++)
						{
							String line = manyLines[i].toString();
							StringBuffer infstr = replacePortsAndVars(line, no, context, cni, segmentedNets, info, false);
		                    // Writing MFactor if available. Not sure here
							if (i == 0) writeMFactor(context, no, infstr);
							infstr.append('\n');
							multiLinePrint(false, infstr.toString());
						}
					} else
					{
						String line = varTemplate.getObject().toString();
						StringBuffer infstr = replacePortsAndVars(line, no, context, cni, segmentedNets, info, false);
	                    // Writing MFactor if available. Not sure here
						writeMFactor(context, no, infstr);
						
						infstr.append('\n');
						multiLinePrint(false, infstr.toString());
					}
					continue;
				}

				// get the ports on this node (in proper order)
				CellNetInfo subCni = getCellNetInfo(parameterizedName(no, context));
				if (subCni == null) continue;

                if (!writeEmptySubckts) {
                    // do not instantiate if empty
                    if (cellIsEmpty((Cell)niProto))
                        continue;
                }

				String modelChar = "X";
				if (no.getName() != null) modelChar += getSafeNetName(no.getName(), false);
				StringBuffer infstr = new StringBuffer();
				infstr.append(modelChar);
				for(Iterator<CellSignal> sIt = subCni.getCellSignals(); sIt.hasNext(); )
				{
					CellSignal subCS = sIt.next();

					// ignore networks that aren't exported
					PortProto pp = subCS.getExport();
					Network net;
                    int exportIndex = subCS.getExportIndex();

                    // This checks if we are netlisting a schematic top level with
                    // swapped-in layout subcells
                    if (pp != null && (cell.getView() == View.SCHEMATIC) && (subCni.getCell().getView() == View.LAYOUT)) {
                        // find equivalent pp from layout to schematic
                        Network subNet = subCS.getNetwork();  // layout network name
                        boolean found = false;
                        for (Iterator<Export> eIt = subCell.getExports(); eIt.hasNext(); ) {
                            Export ex = eIt.next();
                            for (int i=0; i<ex.getNameKey().busWidth(); i++) {
                                String exName = ex.getNameKey().subname(i).toString();
                                if (exName.equals(subNet.getName())) {
                                    pp = ex;
                                    exportIndex = i;
                                    found = true;
                                    break;
                                }
                            }
                            if (found) break;
                        }
                        if (!found) {
                            if (pp.isGround() && pp.getName().startsWith("gnd")) {
                                infstr.append(" gnd");
                            } else if (pp.isPower() && pp.getName().startsWith("vdd")) {
                                infstr.append(" vdd");
                            } else {
                                System.out.println("No matching export on schematic/icon found for export "+
                                        subNet.getName()+" in cell "+subCni.getCell().describe(false));
                                infstr.append(" unknown");
                            }
                            continue;
                        }
                    }

					if (USE_GLOBALS)
					{
						if (pp == null) continue;

                        if (subCS.isGlobal() && subCS.getNetwork().isExported()) {
                            // only add to port list if exported with global name
                            if (!isGlobalExport(subCS)) continue;
                        }

						net = netList.getNetwork(no, pp, exportIndex);
					} else
					{
						if (pp == null && !subCS.isGlobal()) continue;
						if (subCS.isGlobal())
							net = netList.getNetwork(no, subCS.getGlobal());
						else
							net = netList.getNetwork(no, pp, exportIndex);
					}
					CellSignal cs = cni.getCellSignal(net);
                    // special case for parasitic extraction
                    if (useParasitics && !cs.isGlobal()) {
                        // connect to all exports (except power and ground of subcell net)
                        SegmentedNets subSegmentedNets = getSegmentedNets((Cell)no.getProto());
                        Network subNet = subCS.getNetwork();
                        List<String> exportNames = new ArrayList<String>();
                        for (Iterator<Export> it = subNet.getExports(); it.hasNext(); ) {
                            // get subcell export, unless collapsed due to less than min R
                            Export e = it.next();
                            PortInst pi = e.getOriginalPort();
                            String name = subSegmentedNets.getNetName(pi);
                            if (exportNames.contains(name)) continue;
                            exportNames.add(name);
                            // ok, there is a port on the subckt on this subcell for this export,
                            // now get the appropriate network in this cell
                            pi = no.getNodeInst().findPortInstFromProto(no.getProto().findPortProto(e.getNameKey()));
                            name = segmentedNets.getNetName(pi);
                            infstr.append(" " + name);
                        }
                    } else {
                        String name = cs.getName();
                        if (segmentedNets.getUseParasitics()) {
                            name = segmentedNets.getNetName(no.getNodeInst().findPortInstFromProto(pp));
                        }
                        infstr.append(" " + name);
                    }
				}

				if (USE_GLOBALS)
				{
					if (!Simulation.isSpiceUseNodeNames() || spiceEngine == Simulation.SpiceEngine.SPICE_ENGINE_3)
					{
						Global.Set globals = subCni.getNetList().getGlobals();
						int globalSize = globals.size();
						for(int i=0; i<globalSize; i++)
						{
							Global global = globals.get(i);
							infstr.append(" " + global.getName());
						}
					}
				}
                if (useCDL) {
				    infstr.append(" /" + subCni.getParameterizedName());
                } else {
                    infstr.append(" " + subCni.getParameterizedName());
                }

				if (!useCDL && Simulation.isSpiceUseCellParameters())
				{
					// add in parameters to this instance
					for(Iterator<Variable> it = subCell.getParameters(); it.hasNext(); )
					{
						Variable paramVar = it.next();
						Variable instVar = no.getVar(paramVar.getKey());
                        String paramStr = "??";
						if (instVar != null) {
                            if (paramVar.getCode() != TextDescriptor.Code.SPICE) continue;
                            Object obj = context.evalSpice(instVar, false);
                            if (obj != null)
                                paramStr = formatParam(String.valueOf(obj));
                        }
						infstr.append(" " + paramVar.getTrueName() + "=" + paramStr);
					}
//					for(Iterator it = subCell.getVariables(); it.hasNext(); )
//					{
//						Variable paramVar = it.next();
//						if (!paramVar.isParam()) continue;
//						Variable instVar = no.getVar(paramVar.getKey());
//						String paramStr = "??";
//						if (instVar != null) paramStr = formatParam(trimSingleQuotes(String.valueOf(context.evalVar(instVar))));
//						infstr.append(" " + paramVar.getTrueName() + "=" + paramStr);
//					}
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
			if (fun.isResistor() || // == PrimitiveNode.Function.RESIST ||
                fun == PrimitiveNode.Function.INDUCT ||
				fun.isCapacitor() || // == PrimitiveNode.Function.CAPAC || fun == PrimitiveNode.Function.ECAPAC ||
				fun == PrimitiveNode.Function.DIODE || fun == PrimitiveNode.Function.DIODEZ)
			{
				if (fun.isResistor()) // == PrimitiveNode.Function.RESIST)
				{
                    if ((fun == PrimitiveNode.Function.PRESIST && isShortExplicitResistors()) ||
                        (fun == PrimitiveNode.Function.RESIST && isShortResistors()))
                        continue;
					Variable resistVar = ni.getVar(Schematics.SCHEM_RESISTANCE);
					String extra = "";
                    String partName = "R";
					if (resistVar != null)
					{
						extra = resistVar.describe(context, ni);
						if (TextUtils.isANumber(extra))
						{
							double pureValue = TextUtils.atof(extra);
							extra = TextUtils.formatDoublePostFix(pureValue); //displayedUnits(pureValue, TextDescriptor.Unit.RESISTANCE, TextUtils.UnitScale.NONE);
						}
					} else {
                        if (fun == PrimitiveNode.Function.PRESIST) {
                            partName = "XR";
                            double width = ni.getYSize();
                            double length = ni.getXSize();
                            SizeOffset offset = ni.getSizeOffset();
                            width = width - offset.getHighYOffset() - offset.getLowYOffset();
                            length = length - offset.getHighXOffset() - offset.getLowXOffset();
                            extra = " L='"+length+"*LAMBDA' W='"+width+"*LAMBDA'";
                            if (layoutTechnology == Technology.getTSMC90Technology() ||
                               (cell.getView() == View.LAYOUT && cell.getTechnology() == Technology.getTSMC90Technology())) {
                                if (ni.getProto().getName().equals("P-Poly-RPO-Resistor")) {
                                    extra = "GND rpporpo"+extra;
                                }
                                if (ni.getProto().getName().equals("N-Poly-RPO-Resistor")) {
                                    extra = "GND rnporpo"+extra;
                                }
                            } else {
                                if (ni.getProto().getName().equals("P-Poly-RPO-Resistor")) {
                                    extra = "rppo1rpo"+extra;
                                }
                                if (ni.getProto().getName().equals("N-Poly-RPO-Resistor")) {
                                    extra = "rnpo1rpo"+extra;
                                }
                            }
                        }
                    }
					writeTwoPort(ni, partName, extra, cni, netList, context, segmentedNets);
				} else if (fun.isCapacitor()) // == PrimitiveNode.Function.CAPAC || fun == PrimitiveNode.Function.ECAPAC)
				{
					Variable capacVar = ni.getVar(Schematics.SCHEM_CAPACITANCE);
					String extra = "";
					if (capacVar != null)
					{
						extra = capacVar.describe(context, ni);
						if (TextUtils.isANumber(extra))
						{
							double pureValue = TextUtils.atof(extra);
							extra = TextUtils.formatDoublePostFix(pureValue); // displayedUnits(pureValue, TextDescriptor.Unit.CAPACITANCE, TextUtils.UnitScale.NONE);
						}
					}
					writeTwoPort(ni, "C", extra, cni, netList, context, segmentedNets);
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
							extra = TextUtils.formatDoublePostFix(pureValue); // displayedUnits(pureValue, TextDescriptor.Unit.INDUCTANCE, TextUtils.UnitScale.NONE);
						}
					}
					writeTwoPort(ni, "L", extra, cni, netList, context, segmentedNets);
				} else if (fun == PrimitiveNode.Function.DIODE || fun == PrimitiveNode.Function.DIODEZ)
				{
					Variable diodeVar = ni.getVar(Schematics.SCHEM_DIODE);
					String extra = "";
					if (diodeVar != null)
						extra = diodeVar.describe(context, ni);
					if (extra.length() == 0) extra = "DIODE";
					writeTwoPort(ni, "D", extra, cni, netList, context, segmentedNets);
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
				String message = "WARNING: " + ni + " not fully connected in " + cell;
				dumpErrorMessage(message);
			}

			// get model information
			String modelName = null;
            String defaultBulkName = null;
			Variable modelVar = ni.getVar(SPICE_MODEL_KEY);
			if (modelVar != null) modelName = modelVar.getObject().toString();

            // special case for ST090 technology which has stupid non-standard transistor
            // models which are subcircuits
            boolean st090laytrans = false;
            if (cell.getView() == View.LAYOUT && layoutTechnology == Technology.getTSMC90Technology()) {
                st090laytrans = true;
            }

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
                defaultBulkName = "gnd";
				if (modelName == null) modelName = "N";
                if (st090laytrans) {
                    modelChar = "XM";
                    modelName = "nsvt";
                }
			} else if (fun == PrimitiveNode.Function.TRA4NMOS)			// NMOS (Complementary) 4-port transistor
			{
				modelChar = "M";
				if (modelName == null) modelName = "N";
                if (st090laytrans) {
                    modelChar = "XM";
                    modelName = "nsvt";
                }
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
                defaultBulkName = "vdd";
				if (modelName == null) modelName = "P";
                if (st090laytrans) {
                    modelChar = "XM";
                    modelName = "psvt";
                }
			} else if (fun == PrimitiveNode.Function.TRA4PMOS)			// PMOS (Complementary) 4-port transistor
			{
				modelChar = "M";
				if (modelName == null) modelName = "P";
                if (st090laytrans) {
                    modelChar = "XM";
                    modelName = "psvt";
                }
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
			if (ni.getName() != null) modelChar += getSafeNetName(ni.getName(), false);
			StringBuffer infstr = new StringBuffer();
            String drainName = drainCs.getName();
            String gateName = gateCs.getName();
            String sourceName = sourceCs.getName();
            if (segmentedNets.getUseParasitics()) {
                drainName = segmentedNets.getNetName(ni.getTransistorDrainPort());
                gateName = segmentedNets.getNetName(ni.getTransistorGatePort());
                sourceName = segmentedNets.getNetName(ni.getTransistorSourcePort());
            }
			infstr.append(modelChar + " " + drainName + " " + gateName + " " + sourceName);
			if (biasCs != null) {
                String biasName = biasCs.getName();
                if (segmentedNets.getUseParasitics()) {
                    if (ni.getTransistorBiasPort() != null)
                        biasName = segmentedNets.getNetName(ni.getTransistorBiasPort());
                }
                infstr.append(" " + biasName);
            } else {
                if (cell.getView() == View.LAYOUT && defaultBulkName != null)
                    infstr.append(" " + defaultBulkName);
            }
			if (modelName != null) infstr.append(" " + modelName);

			// compute length and width (or area for nonMOS transistors)
			TransistorSize size = ni.getTransistorSize(context);
            if (size == null)
                System.out.println("Warning: transistor with null size " + ni.getName());
            else
            {
                if (size.getDoubleWidth() > 0 || size.getDoubleLength() > 0)
                {
                    double w = maskScale * size.getDoubleWidth();
                    double l = maskScale * size.getDoubleLength();
                    // get gate length subtraction in lambda
                    double lengthSubtraction = layoutTechnology.getGateLengthSubtraction() / layoutTechnology.getScale() * 1000;
                    l -= lengthSubtraction;
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
                          (spiceEngine == Simulation.SpiceEngine.SPICE_ENGINE_H || spiceEngine == Simulation.SpiceEngine.SPICE_ENGINE_H_ASSURA)))
                    {
                        // schematic transistors may be text
                        if ((size.getDoubleLength() == 0) && (size.getLength() instanceof String)) {
                            if (lengthSubtraction != 0)
                                infstr.append(" L="+formatParam((String)size.getLength()) + " - "+ lengthSubtraction);
                            else
                                infstr.append(" L="+formatParam((String)size.getLength()));
                        } else {
                            infstr.append(" L=" + TextUtils.formatDouble(l, 2));
                            if (!Simulation.isSpiceWriteTransSizeInLambda() && !st090laytrans) infstr.append("U");
                        }
                        if ((size.getDoubleWidth() == 0) && (size.getWidth() instanceof String)) {
                            infstr.append(" W="+formatParam((String)size.getWidth()));
                        } else {
                            infstr.append(" W=" + TextUtils.formatDouble(w, 2));
                            if (!Simulation.isSpiceWriteTransSizeInLambda() && !st090laytrans) infstr.append("U");
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
                    // get gate length subtraction in lambda
                    double lengthSubtraction = layoutTechnology.getGateLengthSubtraction() / layoutTechnology.getScale() * 1000;
                    if ((size.getDoubleLength() == 0) && (size.getLength() instanceof String)) {
                        if (lengthSubtraction != 0)
                            infstr.append(" L="+formatParam((String)size.getLength()) + " - "+lengthSubtraction);
                        else
                            infstr.append(" L="+formatParam((String)size.getLength()));
                    }
                    if ((size.getDoubleWidth() == 0) && (size.getWidth() instanceof String))
                        infstr.append(" W="+formatParam((String)size.getWidth()));
                }
            }

			// make sure transistor is connected to nets
			SpiceNet spNetGate = spiceNetMap.get(gateNet);
			SpiceNet spNetSource = spiceNetMap.get(sourceNet);
			SpiceNet spNetDrain = spiceNetMap.get(drainNet);
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
						infstr.append(" AS=" + TextUtils.formatDouble(as, 3));
						if (!Simulation.isSpiceWriteTransSizeInLambda() && !st090laytrans) infstr.append("P");
					}
					if (ad > 0.0)
					{
						infstr.append(" AD=" + TextUtils.formatDouble(ad, 3));
						if (!Simulation.isSpiceWriteTransSizeInLambda() && !st090laytrans) infstr.append("P");
					}
					if (ps > 0.0)
					{
						infstr.append(" PS=" + TextUtils.formatDouble(ps, 3));
						if (!Simulation.isSpiceWriteTransSizeInLambda() && !st090laytrans) infstr.append("U");
					}
					if (pd > 0.0)
					{
						infstr.append(" PD=" + TextUtils.formatDouble(pd, 3));
						if (!Simulation.isSpiceWriteTransSizeInLambda() && !st090laytrans) infstr.append("U");
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
			if (Simulation.isSpiceUseParasitics() && cell.getView() == View.LAYOUT)
			{
                if (useNewParasitics) {

                    int capCount = 0;
                    int resCount = 0;
                    // write caps
                    multiLinePrint(true, "** Extracted Parasitic Capacitors ***\n");
                    for (SegmentedNets.NetInfo netInfo : segmentedNets.getUniqueSegments()) {
                        if (netInfo.cap > cell.getTechnology().getMinCapacitance()) {
                            if (netInfo.netName.equals("gnd")) continue;           // don't write out caps from gnd to gnd
                            multiLinePrint(false, "C" + capCount + " " + netInfo.netName + " 0 " + TextUtils.formatDouble(netInfo.cap, 2) + "fF\n");
                            capCount++;
                        }
                    }
                    // write resistors
                    multiLinePrint(true, "** Extracted Parasitic Resistors ***\n");
                    for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); ) {
                        ArcInst ai = it.next();
                        Double res = segmentedNets.arcRes.get(ai);
                        if (res == null) continue;
                        String n0 = segmentedNets.getNetName(ai.getHeadPortInst());
                        String n1 = segmentedNets.getNetName(ai.getTailPortInst());
                        int arcPImodels = SegmentedNets.getNumPISegments(res.doubleValue(), layoutTechnology.getMaxSeriesResistance());
                        if (arcPImodels > 1) {
                            // have to break it up into smaller pieces
                            double segCap = segmentedNets.getArcCap(ai)/((double)(arcPImodels+1));
                            double segRes = res.doubleValue()/((double)arcPImodels);
                            String segn0 = n0;
                            String segn1 = n0;
                            for (int i=0; i<arcPImodels; i++) {
                                segn1 = n0 + "##" + i;
                                // print cap on intermediate node
                                if (i == (arcPImodels-1))
                                    segn1 = n1;

                                multiLinePrint(false, "R"+resCount+" "+segn0+" "+segn1+" "+TextUtils.formatDouble(segRes)+"\n");
                                resCount++;
                                if (i < (arcPImodels-1)) {
                                    if (!segn1.equals("gnd")) {
                                        multiLinePrint(false, "C"+capCount+" "+segn1+" 0 "+TextUtils.formatDouble(segCap)+"fF\n");
                                        capCount++;
                                    }
                                }
                                segn0 = segn1;
                            }
                        } else {
                            multiLinePrint(false, "R" + resCount + " " + n0 + " " + n1 + " " + TextUtils.formatDouble(res.doubleValue(), 2) + "\n");
                            resCount++;
                        }
                    }
                } else {
                    // print parasitic capacitances
                    boolean first = true;
                    int capacNum = 1;
                    for(Iterator<CellSignal> sIt = cni.getCellSignals(); sIt.hasNext(); )
                    {
                        CellSignal cs = sIt.next();
                        Network net = cs.getNetwork();
                        if (net == cni.getGroundNet()) continue;

                        SpiceNet spNet = spiceNetMap.get(net);
                        if (spNet.nonDiffCapacitance > layoutTechnology.getMinCapacitance())
                        {
                            if (first)
                            {
                                first = false;
                                multiLinePrint(true, "** Extracted Parasitic Elements:\n");
                            }
                            multiLinePrint(false, "C" + capacNum + " " + cs.getName() + " 0 " + TextUtils.formatDouble(spNet.nonDiffCapacitance, 2) + "fF\n");
                            capacNum++;
                        }
                    }
                }
			}
		}

		// write out any directly-typed SPICE cards
		if (!useCDL)
		{
			boolean firstDecl = true;
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				if (ni.getProto() != Generic.tech.invisiblePinNode) continue;
				Variable cardVar = ni.getVar(SPICE_CARD_KEY);
				if (cardVar == null) continue;
				if (firstDecl)
				{
					firstDecl = false;
					multiLinePrint(true, "\n* Spice Code nodes in cell " + cell + "\n");
				}
				emitEmbeddedSpice(cardVar, context, segmentedNets, info, false);
			}
		}

        // finally, if this is the top level,
        // write out some very small resistors between any networks
        // that are asserted will be connected by the NCC annotation "exportsConnectedByParent"
        // this should really be done internally in the network tool with a switch, but
        // this hack works here for now
        NccCellAnnotations anna = NccCellAnnotations.getAnnotations(cell);
        if (cell == topCell && anna != null) {
            // each list contains all name patterns that are be shorted together
            if (anna.getExportsConnected().hasNext()) {
                multiLinePrint(true, "\n*** Exports shorted due to NCC annotation 'exportsConnectedByParent':\n");
            }

            for (Iterator<List<NamePattern>> it = anna.getExportsConnected(); it.hasNext(); ) {
                List<NamePattern> list = it.next();
                List<Network> netsToConnect = new ArrayList<Network>();
                // each name pattern can match any number of exports in the cell
                for (NccCellAnnotations.NamePattern pat : list) {
                    for (Iterator<PortProto> it3 = cell.getPorts(); it3.hasNext(); ) {
                        Export e = (Export)it3.next();
                        String name = e.getName();
                        // keep track of networks to short together
                        if (pat.matches(name)) {
                            Network net = netList.getNetwork(e, 0);
                            if (!netsToConnect.contains(net))
                                netsToConnect.add(net);
                        }
                    }
                }
                // connect all nets in list of nets to connect
                String name = null;
//                int i=0;
                for (Network net : netsToConnect) {
                    if (name != null) {
                        multiLinePrint(false, "R"+name+" "+name+" "+net.getName()+" 0.001\n");
                    }
                    name = net.getName();
//                    i++;
                }
            }
        }

		// now we're finished writing the subcircuit.
		if (cell != topCell || useCDL || Simulation.isSpiceWriteSubcktTopCell())
		{
			multiLinePrint(false, ".ENDS " + cni.getParameterizedName() + "\n");
		}
        if (cell == topCell && Simulation.isSpiceWriteSubcktTopCell()) {
            multiLinePrint(false, "\n\n"+topLevelInstance+"\n\n");
        }
	}

	private void emitEmbeddedSpice(Variable cardVar, VarContext context, SegmentedNets segmentedNets, HierarchyEnumerator.CellInfo info, boolean flatNetNames)
	{
		Object obj = cardVar.getObject();
		if (!(obj instanceof String) && !(obj instanceof String[])) return;
		if (!cardVar.isDisplay()) return;
		if (obj instanceof String)
		{
	        StringBuffer buf = replacePortsAndVars((String)obj, context.getNodable(), context.pop(), null, segmentedNets, info, flatNetNames);
			buf.append('\n');
			String msg = buf.toString();
			boolean isComment = false;
			if (msg.startsWith("*")) isComment = true;
			multiLinePrint(isComment, msg);
		} else
		{
			String [] strings = (String [])obj;
			for(int i=0; i<strings.length; i++)
			{
	            StringBuffer buf = replacePortsAndVars(strings[i], context.getNodable(), context.pop(), null, segmentedNets, info, flatNetNames);
				buf.append('\n');
				String msg = buf.toString();
				boolean isComment = false;
				if (msg.startsWith("*")) isComment = true;
				multiLinePrint(isComment, msg);
	        }
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
        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();
            if (!ni.isCellInstance()) continue;
            if (ni.isIconOfParent()) continue;
            if (ni.getVar(LENetlister.ATTR_LEGATE) != null) {
                // make sure cell also has that var
                Cell np = (Cell)ni.getProto();
                if (np.contentsView() != null) np = np.contentsView();
                if (np.getVar(LENetlister.ATTR_LEGATE) != null)
                    mark = true; continue;
            }
            if (ni.getVar(LENetlister.ATTR_LEKEEPER) != null) {
                // make sure cell also has that var
                Cell np = (Cell)ni.getProto();
                if (np.contentsView() != null) np = np.contentsView();
                if (np.getVar(LENetlister.ATTR_LEKEEPER) != null)
                    mark = true; continue;
            }
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
            boolean useCellParams = !useCDL && Simulation.isSpiceUseCellParameters();
            if (canParameterizeNames() && no.isCellInstance())
            {
                // if there are parameters, append them to this name
                List<Variable> paramValues = new ArrayList<Variable>();
                for(Iterator<Variable> it = no.getVariables(); it.hasNext(); )
                {
                    Variable var = it.next();
                    if (!no.getNodeInst().isParam(var.getKey())) continue;
//                    if (!var.isParam()) continue;
                    Variable cellvar = cell.getVar(var.getKey());
                    if (useCellParams && (cellvar.getCode() == TextDescriptor.Code.SPICE)) {
                        continue;
                    }
                    paramValues.add(var);
                }
                for(Variable var : paramValues)
                {
                    String eval = var.describe(context, no);
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
            Integer i = uniqueNames.get(uniqueCellName.toString());
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

    /**
     * Replace ports and vars in 'line'.  Ports and Vars should be
     * referenced via $(name)
     * @param line the string to search and replace within
     * @param no the nodable up the hierarchy that has the parameters on it
     * @param context the context of the nodable
     * @param cni the cell net info of cell in which the nodable exists (if cni is
     * null, no port name replacement will be done)
     * @return the modified line
     */
    private StringBuffer replacePortsAndVars(String line, Nodable no, VarContext context,
                                       CellNetInfo cni, SegmentedNets segmentedNets,
                                       HierarchyEnumerator.CellInfo info, boolean flatNetNames) {
        StringBuffer infstr = new StringBuffer();
        Cell subCell = null;
    	if (no != null)
    	{
    		subCell = (Cell)no.getProto();
    	}

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
            // do the parameter substitution
            String paramName = line.substring(start, pt);

            PortProto pp = null;
            if (subCell != null) {
                pp = subCell.findPortProto(paramName);
            }
            Variable.Key varKey;

            if (paramName.equalsIgnoreCase("node_name") && no != null)
            {
            	String nodeName = getSafeNetName(no.getName(), false);
//            	nodeName = nodeName.replaceAll("[\\[\\]]", "_");
                infstr.append(nodeName);
            } else if (cni != null && pp != null)
            {
                // port name found: use its spice node
                Network net = cni.getNetList().getNetwork(no, pp, 0);
                CellSignal cs = cni.getCellSignal(net);
                String portName = cs.getName();
                if (segmentedNets.getUseParasitics()) {
                    PortInst pi = no.getNodeInst().findPortInstFromProto(pp);
                    portName = segmentedNets.getNetName(pi);
                }
                if (flatNetNames) {
                    portName = info.getUniqueNetName(net, ".");
                }
                infstr.append(portName);
            } else if (no != null && (varKey = Variable.findKey("ATTR_" + paramName)) != null)
            {
                // no port name found, look for variable name
                Variable attrVar = null;
                //Variable.Key varKey = Variable.findKey("ATTR_" + paramName);
                if (varKey != null) {
                    attrVar = no.getVar(varKey);
                    if (attrVar == null) attrVar = no.getParameter(varKey);
                }
                if (attrVar == null) infstr.append("??"); else
                {
                    String pVal = "?";
                    Variable parentVar = attrVar;
                    if (subCell != null)
                        parentVar = subCell.getVar(attrVar.getKey());
                    if (!useCDL && Simulation.isSpiceUseCellParameters() &&
                            parentVar.getCode() == TextDescriptor.Code.SPICE) {
                        Object obj = context.evalSpice(attrVar, false);
                        if (obj != null)
                            pVal = obj.toString();
                    } else {
                        pVal = String.valueOf(context.evalVar(attrVar, no));
                    }
                    if (attrVar.getCode() != TextDescriptor.Code.NONE) pVal = trimSingleQuotes(pVal);
                    infstr.append(pVal);
                    //else
                    //    infstr.append(trimSingleQuotes(attrVar.getPureValue(-1, -1)));
                }
            } else {
                // look for the network name
                boolean found = false;
                String hierName = null;
                String [] names = paramName.split("\\.");
                if (names.length > 1 && flatNetNames) {
                    // hierarchical name, down hierarchy
                    Cell thisCell = info.getCell();
                    Netlist thisNetlist = info.getNetlist();
                    VarContext thisContext = context;
                    if (no != null) {
                        // push it back on, it got popped off in "embedSpice..."
                        thisContext = thisContext.push(no);
                    }
                    for (int i=0; i<names.length-1; i++) {
                        boolean foundno = false;
                        for (Iterator<Nodable> it = thisNetlist.getNodables(); it.hasNext(); ) {
                            Nodable subno = it.next();
                            if (subno.getName().equals(names[i])) {
                                if (subno.getProto() instanceof Cell) {
                                    thisCell = (Cell)subno.getProto();
                                    thisNetlist = thisNetlist.getNetlist(subno);
                                    thisContext = thisContext.push(subno);
                                }
                                foundno = true;
                                continue;
                            }
                        }
                        if (!foundno) {
                            System.out.println("Unable to find "+names[i]+" in "+paramName);
                            break;
                        }
                    }
                    Network net = findNet(thisNetlist, names[names.length-1]);
                    if (net != null) {
                        HierarchyEnumerator.NetNameProxy proxy = new HierarchyEnumerator.NetNameProxy(
                                thisContext, ".x", net);
                        Global g = getGlobal(proxy.getNet());
                        if (g != null)
                            hierName = g.getName();
                        else
                            hierName = proxy.toString();
                    }

                } else {
                    // net may be exported and named at higher level, use getUniqueName
                    Network net = findNet(info.getNetlist(), paramName);
                    if (net != null) {
                        if (flatNetNames) {
                            HierarchyEnumerator.NetNameProxy proxy = info.getUniqueNetNameProxy(net, ".x");
                            Global g = getGlobal(proxy.getNet());
                            if (g != null)
                                hierName = g.getName();
                            else
                                hierName = proxy.toString();
                        } else {
                            hierName = cni.getCellSignal(net).getName();
                        }
                    }
                }

                // convert to spice format
                if (hierName != null) {
                    if (flatNetNames) {
                        if (hierName.indexOf(".x") > 0) {
                            hierName = "x"+hierName;
                        }
                        // remove x in front of net name
                        int i = hierName.lastIndexOf(".x");
                        if (i > 0)
                            hierName = hierName.substring(0, i+1) + hierName.substring(i+2);
                        else {
                            i = hierName.lastIndexOf("."+paramName);
                            if (i > 0) {
                                hierName = hierName.substring(0, i) + "_" + hierName.substring(i+1);
                            }
                        }
                    }
                    infstr.append(hierName);
                    found = true;
                }
                if (!found) {
                    System.out.println("Unable to lookup key $("+paramName+") in cell "+context.getInstPath("."));
                }
            }
        }
        return infstr;
    }

    private Network findNet(Netlist netlist, String netName) {
        Network foundnet = null;
        for (Iterator<Network> it = netlist.getNetworks(); it.hasNext(); ) {
            Network net = it.next();
            if (net.hasName(netName)) {
                foundnet = net;
                break;
            }
        }
        return foundnet;
    }

    /**
     * Get the global associated with this net. Returns null if net is
     * not a global.
     * @param net
     * @return global associated with this net, or null if not global
     */
    private Global getGlobal(Network net) {
        Netlist netlist = net.getNetlist();
        for (int i=0; i<netlist.getGlobals().size(); i++) {
            Global g = netlist.getGlobals().get(i);
            if (netlist.getNetwork(g) == net)
                return g;
        }
        return null;
    }

    /**
     * Check if the global cell signal is exported as with a global name,
     * rather than just being a global tied to some other export.
     * I.e., returns true if global "gnd" has been exported using name "gnd".
     * @param cs
     * @return true if global signal exported with global name
     */
    private boolean isGlobalExport(CellSignal cs) {
        if (!cs.isGlobal() || !cs.isExported()) return false;
        for (Iterator<Export> it = cs.getNetwork().getExports(); it.hasNext(); ) {
            Export ex = it.next();
            for (int i=0; i<ex.getNameKey().busWidth(); i++) {
                String name = ex.getNameKey().subname(i).canonicString();
                if (cs.getName().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean ignoreSubcktPort(CellSignal cs) {
        // ignore networks that aren't exported
        PortProto pp = cs.getExport();
        if (USE_GLOBALS)
        {
            if (pp == null) return true;

            if (cs.isGlobal() && !cs.getNetwork().isExported()) return true;

            if (cs.isGlobal() && cs.getNetwork().isExported()) {
                // only add to port list if exported with global name
                if (!isGlobalExport(cs)) return true;
            }
        } else
        {
            if (pp == null && !cs.isGlobal()) return true;
        }
        if (useCDL)
        {
//					// if this is output and the last was input (or visa-versa), insert "/"
//					if (i > 0 && netlist[i-1]->temp2 != net->temp2)
//						infstr.append(" /");
        }
        return false;
    }


    /**
     * Class to take care of added networks in cell due to
     * extracting resistance of arcs.  Takes care of naming,
     * addings caps at portinst locations (due to PI model end caps),
     * and storing resistance of arcs.
     * <P>
     * A network is broken into segments at all PortInsts along the network.
     * Each PortInst is given a new net segment name.  These names are used
     * to write out caps and resistors.  Each Arc writes out a PI model
     * (a cap on each end and a resistor in the middle).  Sometimes, an arc
     * has very little or zero resistance, or we want to ignore it.  Then
     * we must short two portinsts together into the same net segment.
     * However, we do not discard the capacitance, but continue to add it up.
     */
    private static class SegmentedNets {
        private static Comparator<PortInst> PORT_INST_COMPARATOR = new Comparator<PortInst>() {
            public int compare(PortInst p1, PortInst p2) {
                if (p1 == p2) return 0;
                int cmp = p1.getNodeInst().compareTo(p2.getNodeInst());
                if (cmp != 0) return cmp;
                if (p1.getPortIndex() < p2.getPortIndex()) return -1;
                return 1;
            }
        };
        private static class NetInfo implements Comparable {
            private String netName = "unassigned";
            private double cap = 0;
            private TreeSet<PortInst> joinedPorts = new TreeSet<PortInst>(PORT_INST_COMPARATOR);     // list of portInsts on this new net
            /**
             * Compares NetInfos by thier first PortInst.
             * @param obj the other NetInfo.
             * @return a comparison between the NetInfos.
             */
            public int compareTo(Object obj) {
                NetInfo that = (NetInfo)obj;
                if (this.joinedPorts.isEmpty()) return that.joinedPorts.isEmpty() ? 0 : -1;
                if (that.joinedPorts.isEmpty()) return 1;
                return PORT_INST_COMPARATOR.compare(this.joinedPorts.first(), that.joinedPorts.first());
            }
        }

        private HashMap<PortInst,NetInfo> segmentedNets;          // key: portinst, obj: PortInstInfo
        private HashMap<ArcInst,Double> arcRes;                 // key: arcinst, obj: Double (arc resistance)
        boolean verboseNames = false;           // true to give renamed nets verbose names
        private CellNetInfo cni;                // the Cell's net info
        boolean useParasitics = false;          // disable or enable netname remapping
        private HashMap<Network,Integer> netCounters;            // key: net, obj: Integer - for naming segments
        private Cell cell;
        private List<List<String>> shortedExports; // list of lists of export names shorted together
        private HashMap<ArcInst,Double> longArcCaps;            // for arcs to be broken up into multiple PI models, need to record cap

        private SegmentedNets(Cell cell, boolean verboseNames, CellNetInfo cni, boolean useParasitics) {
            segmentedNets = new HashMap<PortInst,NetInfo>();
            arcRes = new HashMap<ArcInst,Double>();
            this.verboseNames = verboseNames;
            this.cni = cni;
            this.useParasitics = useParasitics;
            netCounters = new HashMap<Network,Integer>();
            this.cell = cell;
            shortedExports = new ArrayList<List<String>>();
            longArcCaps = new HashMap<ArcInst,Double>();
        }
        // don't call this method outside of SegmentedNets
        // Add a new PortInst net segment
        private NetInfo putSegment(PortInst pi, double cap) {
            // create new info for PortInst
            NetInfo info = segmentedNets.get(pi);
            if (info == null) {
                info = new NetInfo();
                info.netName = getNewName(pi, info);
                info.cap += cap;
                if (isPowerGround(pi)) info.cap = 0;        // note if you remove this line,
                                                            // you have to explicity short all
                                                            // power portinsts together, or you can get duplicate caps
                info.joinedPorts.add(pi);
                segmentedNets.put(pi, info);
            } else {
                info.cap += cap;
                //assert(info.joinedPorts.contains(pi));  // should already contain pi if info already exists
            }
            return info;
        }
        // don't call this method outside of SegmentedNets
        // Get a new name for the net segment associated with the portinst
        private String getNewName(PortInst pi, NetInfo info) {
            Network net = cni.getNetList().getNetwork(pi);
            CellSignal cs = cni.getCellSignal(net);
            if (!useParasitics || (!Simulation.isParasiticsExtractPowerGround() &&
                    isPowerGround(pi))) return cs.getName();

            Integer i = netCounters.get(net);
            if (i == null) {
                i = new Integer(0);
                netCounters.put(net, i);
            }
            // get new name
            String name = info.netName;
            Export ex = pi.getExports().hasNext() ? pi.getExports().next() : null;
            //if (ex != null && ex.getName().equals(cs.getName())) {
            if (ex != null) {
                name = ex.getName();
            } else {
                if (i.intValue() == 0 && !cs.isExported())      // get rid of #0 if net not exported
                    name = cs.getName();
                else {
                    if (verboseNames)
                        name = cs.getName() + "#" + i.intValue() + pi.getNodeInst().getName() + "_" + pi.getPortProto().getName();
                    else
                        name = cs.getName() + "#" + i.intValue();
                }
                i = new Integer(i.intValue() + 1);
                netCounters.put(net, i);
            }
            return name;
        }
        // short two net segments together by their portinsts
        private void shortSegments(PortInst p1, PortInst p2) {
            if (!segmentedNets.containsKey(p1))
                putSegment(p1, 0);
            if (!segmentedNets.containsKey(p2));
                putSegment(p2, 0);
            NetInfo info1 = segmentedNets.get(p1);
            NetInfo info2 = segmentedNets.get(p2);
            if (info1 == info2) return;                     // already joined
            // short
            //System.out.println("Shorted together "+info1.netName+ " and "+info2.netName);
            info1.joinedPorts.addAll(info2.joinedPorts);
            info1.cap += info2.cap;
            if (TextUtils.STRING_NUMBER_ORDER.compare(info2.netName, info1.netName) < 0) {
//            if (info2.netName.compareTo(info1.netName) < 0) {
                info1.netName = info2.netName;
            }
            //info1.netName += info2.netName;
            // replace info2 with info1, info2 is no longer used
            // need to do for every portinst in merged segment
            for (PortInst pi : info1.joinedPorts)
                segmentedNets.put(pi, info1);
        }
        // get the segment name for the portinst.
        // if no parasitics, this is just the CellSignal name.
        private String getNetName(PortInst pi) {
            if (!useParasitics || (isPowerGround(pi) &&
                    !Simulation.isParasiticsExtractPowerGround())) {
                CellSignal cs = cni.getCellSignal(cni.getNetList().getNetwork(pi));
                //System.out.println("CellSignal name for "+pi.getNodeInst().getName()+"."+pi.getPortProto().getName()+" is "+cs.getName());
//System.out.println("NETWORK NAMED "+cs.getName());
                return cs.getName();
            }
            NetInfo info = segmentedNets.get(pi);
            if (info == null) {
                info = putSegment(pi, 0);
            }
//System.out.println("NETWORK INAMED "+info.netName);
            return info.netName;
        }
        private void addArcRes(ArcInst ai, double res) {
            // short out if both conns are power/ground
            if (isPowerGround(ai.getHeadPortInst()) && isPowerGround(ai.getTailPortInst()) &&
                    !Simulation.isParasiticsExtractPowerGround()) {
                shortSegments(ai.getHeadPortInst(), ai.getTailPortInst());
                return;
            }
            arcRes.put(ai, new Double(res));
        }
        private boolean isPowerGround(PortInst pi) {
            Network net = cni.getNetList().getNetwork(pi);
            CellSignal cs = cni.getCellSignal(net);
            if (cs.isPower() || cs.isGround()) return true;
            if (cs.getName().startsWith("vdd")) return true;
            if (cs.getName().startsWith("gnd")) return true;
            return false;
        }
        /**
         * Return list of NetInfos for unique segments
         * @return a list of al NetInfos
         */
        private TreeSet<NetInfo> getUniqueSegments() {
            return new TreeSet<NetInfo>(segmentedNets.values());
        }
        private boolean getUseParasitics() {
            return useParasitics;
        }
        // list of export names (Strings)
        private void addShortedExports(List<String> exports) {
            shortedExports.add(exports);
        }
        // list of lists of export names (Strings)
        private Iterator<List<String>> getShortedExports() { return shortedExports.iterator(); }
        public static int getNumPISegments(double res, double maxSeriesResistance) {
            int arcPImodels = 1;
            arcPImodels = (int)(res/maxSeriesResistance);            // need preference here
            if ((res % maxSeriesResistance) != 0) arcPImodels++;
            return arcPImodels;
        }
        // for arcs of larger than max series resistance, we need to break it up into
        // multiple PI models.  So, we need to store the cap associated with the arc
        private void addArcCap(ArcInst ai, double cap) {
            longArcCaps.put(ai, new Double(cap));
        }
        private double getArcCap(ArcInst ai) {
            Double d = longArcCaps.get(ai);
            return d.doubleValue();
        }
    }

    private SegmentedNets getSegmentedNets(Cell cell) {
        for (SegmentedNets seg : segmentedParasiticInfo) {
            if (seg.cell == cell) return seg;
        }
        return null;
    }

    private void backAnnotateLayout()
    {
    	Set<Cell>      cellsToClear = new HashSet<Cell>();
    	List<PortInst> capsOnPorts  = new ArrayList<PortInst>();
    	List<String>   valsOnPorts  = new ArrayList<String>();
    	List<ArcInst>  resOnArcs    = new ArrayList<ArcInst>();
    	List<Double>   valsOnArcs   = new ArrayList<Double>();
        for (SegmentedNets segmentedNets : segmentedParasiticInfo)
        {
            Cell cell = segmentedNets.cell;
            if (cell.getView() != View.LAYOUT) continue;

            // gather cells to clear capacitor values
            cellsToClear.add(cell);

            // gather capacitor updates
            for (SegmentedNets.NetInfo info : segmentedNets.getUniqueSegments())
            {
                PortInst pi = info.joinedPorts.iterator().next();
                if (info.cap > cell.getTechnology().getMinCapacitance())
                {
                	capsOnPorts.add(pi);
                	valsOnPorts.add(TextUtils.formatDouble(info.cap, 2) + "fF");
                }
            }

            // gather resistor updates
            for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
            {
                ArcInst ai = it.next();
                Double res = segmentedNets.arcRes.get(ai);
                Variable var = ai.getVar(ATTR_R);
               
                resOnArcs.add(ai);
                valsOnArcs.add(res);
            }
        }
        new BackAnnotateJob(cellsToClear, capsOnPorts, valsOnPorts, resOnArcs, valsOnArcs);
    }

    private static class BackAnnotateJob extends Job
    {
    	private Set<Cell> cellsToClear;
    	private List<PortInst> capsOnPorts;
    	private List<String> valsOnPorts;
    	private List<ArcInst> resOnArcs;
    	private List<Double> valsOnArcs;

        private BackAnnotateJob(Set<Cell> cellsToClear, List<PortInst> capsOnPorts, List<String> valsOnPorts,
        	List<ArcInst> resOnArcs, List<Double> valsOnArcs)
    	{
            super("Spice Layout Back Annotate", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.capsOnPorts = capsOnPorts;
            this.valsOnPorts = valsOnPorts;
            this.resOnArcs = resOnArcs;
            this.valsOnArcs = valsOnArcs;
            this.cellsToClear = cellsToClear;
            startJob();
        }

        public boolean doIt() throws JobException
        {
            TextDescriptor ctd = TextDescriptor.getPortInstTextDescriptor().withDispPart(TextDescriptor.DispPos.NAMEVALUE);
            TextDescriptor rtd = TextDescriptor.getArcTextDescriptor().withDispPart(TextDescriptor.DispPos.NAMEVALUE);
            int capCount = 0;
            int resCount = 0;

            // clear caps on layout
            for(Cell cell : cellsToClear)
            {
                // delete all C's already on layout
                for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
                {
                    NodeInst ni = it.next();
                    for (Iterator<PortInst> pit = ni.getPortInsts(); pit.hasNext(); )
                    {
                        PortInst pi = pit.next();
                        Variable var = pi.getVar(ATTR_C);
                        if (var != null) pi.delVar(var.getKey());
                    }
                }
            }

            // add new C's
            for(int i=0; i<capsOnPorts.size(); i++)
            {
            	PortInst pi = capsOnPorts.get(i);
            	String str = valsOnPorts.get(i);
                pi.newVar(ATTR_C, str, ctd);
                resCount++;
            }

            // add new R's
            for(int i=0; i<resOnArcs.size(); i++)
            {
            	ArcInst ai = resOnArcs.get(i);
            	Double res = valsOnArcs.get(i);

                // delete R if no new one
                Variable var = ai.getVar(ATTR_R);
                if (res == null && var != null)
                    ai.delVar(ATTR_R);

                // change R if new one
                if (res != null)
                {
                    ai.newVar(ATTR_R, res, rtd);
                    resCount++;
                }
            }
            System.out.println("Back-annotated "+resCount+" Resistors and "+capCount+" Capacitors");
            return true;
        }
    }

    /**
     * These are nets that are either extracted when nothing else is extracted,
     * or not extracted during extraction.  They are specified via the top level
     * net cell + name, any traversal of that net down the hierarchy is also not extracted.
     */
    private static class ExemptedNets {
        private HashMap<Cell,List<Net>> netsByCell;         // key: cell, value: List of ExemptedNets.Net objects
        private Set<Integer> exemptedNetIDs;

        private static class Net {
            private String name;
            private double replacementCap;
        }

        private ExemptedNets(File file) {
            netsByCell = new HashMap<Cell,List<Net>>();
            exemptedNetIDs = new TreeSet<Integer>();

            try {
                FileReader reader = new FileReader(file);
                BufferedReader br = new BufferedReader(reader);
                String line;
                int lineno = 1;
                System.out.println("Using exempted nets file "+file.getAbsolutePath());
                while ((line = br.readLine()) != null) {
                    processLine(line, lineno);
                    lineno++;
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
                return;
            }
        }

        private void processLine(String line, int lineno) {
            if (line == null) return;
            if (line.trim().equals("")) return;
            String parts[] = line.trim().split("\\s+");
            if (parts.length < 3) {
                System.out.println("Error on line "+lineno+": Expected 'LibraryName CellName NetName', but was "+line);
                return;
            }
            Cell cell = getCell(parts[0], parts[1]);
            if (cell == null) return;
            double cap = 0;
            if (parts.length > 3) {
                try {
                    cap = Double.parseDouble(parts[3]);
                } catch (NumberFormatException e) {
                    System.out.println("Error on line "+lineno+" "+e.getMessage());
                }
            }

            List<Net> list = netsByCell.get(cell);
            if (list == null) {
                list = new ArrayList<Net>();
                netsByCell.put(cell, list);
            }
            Net n = new Net();
            n.name = parts[2];
            n.replacementCap = cap;
            list.add(n);
        }

        private Cell getCell(String library, String cell) {
            Library lib = Library.findLibrary(library);
            if (lib == null) {
                System.out.println("Could not find library "+library);
                return null;
            }
            Cell c = lib.findNodeProto(cell);
            if (c == null) {
                System.out.println("Could not find cell "+cell+" in library "+library);
                return null;
            }
            return c;
        }

        /**
         * Get the netIDs for all exempted nets in the cell specified by the CellInfo
         * @param info
         */
        private void setExemptedNets(HierarchyEnumerator.CellInfo info) {
            Cell cell = info.getCell();
            List<Net> netNames = netsByCell.get(cell);
            if (netNames == null) return;               // nothing for this cell
            for (Net n : netNames) {
                String netName = n.name;
                Network net = findNetwork(info, netName);
                if (net == null) {
                    System.out.println("Cannot find network "+netName+" in cell "+cell.describe(true));
                    continue;
                }
                // get the global ID
                System.out.println("Specified exemption of net "+cell.describe(false)+"  "+netName);
                int netID = info.getNetID(net);
                exemptedNetIDs.add(new Integer(netID));
            }
        }

        private Network findNetwork(HierarchyEnumerator.CellInfo info, String name) {
            for (Iterator<Network> it = info.getNetlist().getNetworks(); it.hasNext(); ) {
                Network net = it.next();
                if (net.hasName(name)) return net;
            }
            return null;
        }

        private boolean isExempted(int netID) {
            return exemptedNetIDs.contains(new Integer(netID));
        }

        private double getReplacementCap(Cell cell, Network net) {
            List<Net> netNames = netsByCell.get(cell);
            if (netNames == null) return 0;               // nothing for this cell
            for (Net n : netNames) {
                if (net.hasName(n.name)) {
                    return n.replacementCap;
                }
            }
            return 0;
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
		return getSafeNetName(name, false);
	}

	/** Method to return the proper name of Power */
	protected String getPowerName(Network net)
	{
		if (net != null)
		{
			// favor "vdd" if it is present
			for(Iterator<String> it = net.getNames(); it.hasNext(); )
			{
				String netName = it.next();
				if (netName.equalsIgnoreCase("vdd")) return "vdd";
			}
		}
		return null;
	}

	/** Method to return the proper name of Ground */
	protected String getGroundName(Network net)
	{
		if (spiceEngine == Simulation.SpiceEngine.SPICE_ENGINE_2 ||
			spiceEngine == Simulation.SpiceEngine.SPICE_ENGINE_P ||
			spiceEngine == Simulation.SpiceEngine.SPICE_ENGINE_G) return "0";

		if (net != null)
		{
			// favor "gnd" if it is present
			for(Iterator<String> it = net.getNames(); it.hasNext(); )
			{
				String netName = it.next();
				if (netName.equalsIgnoreCase("gnd")) return "gnd";
			}
		}
		return null;
	}

	/** Method to return the proper name of a Global signal */
	protected String getGlobalName(Global glob) { return glob.getName(); }

    /** Method to report that export names do NOT take precedence over
     * arc names when determining the name of the network. */
    protected boolean isNetworksUseExportedNames() { return false; }

	/** Method to report that library names are NOT always prepended to cell names. */
	protected boolean isLibraryNameAlwaysAddedToCellName() { return false; }

	/** Method to report that aggregate names (busses) are not used. */
	protected boolean isAggregateNamesSupported() { return false; }

    /** Method to report that not to choose best export name among exports connected to signal. */
    protected boolean isChooseBestExportName() { return false; } 
    
	/** Method to report whether input and output names are separated. */
	protected boolean isSeparateInputAndOutput() { return false; }

    /** If the netlister has requirments not to netlist certain cells and their
     * subcells, override this method.
     * If this cell has a spice template, skip it
     */
    protected boolean skipCellAndSubcells(Cell cell)
	{
        if (useCDL) {
            // check for CDL template: if exists, skip
            Variable cdlTemplate = cell.getVar(CDL_TEMPLATE_KEY);
            if (cdlTemplate != null) return true;
            // no template, return false
            return false;
        }

		// skip if there is a template
        Variable varTemplate = null;
        varTemplate = cell.getVar(preferedEngineTemplateKey);
        if (varTemplate != null) return true;
        if (!assuraHSpice)
        {
            varTemplate = cell.getVar(SPICE_TEMPLATE_KEY);
        }
        if (varTemplate != null) return true;

		// look for a model file on the current cell
        if (CellModelPrefs.spiceModelPrefs.isUseModelFromFile(cell)) {
            String fileName = CellModelPrefs.spiceModelPrefs.getModelFile(cell);
            if (!modelOverrides.containsKey(cell))
            {
                multiLinePrint(true, "\n* " + cell + " is described in this file:\n");
                addIncludeFile(fileName);
                if (!fileName.startsWith("/") && !fileName.startsWith("\\")) {
                    File spiceFile = new File(filePath);
                    fileName = (new File(spiceFile.getParent(), fileName)).getPath();
                }
                modelOverrides.put(cell, fileName);
            }
            return true;
        }
		return false;
    }

    protected void validateSkippedCell(HierarchyEnumerator.CellInfo info) {
        String fileName = modelOverrides.get(info.getCell());
        if (fileName != null) {
            // validate included file
            SpiceNetlistReader reader = new SpiceNetlistReader();
            try {
                reader.readFile(fileName, false);
                HierarchyEnumerator.CellInfo parentInfo = info.getParentInfo();
                Nodable no = info.getParentInst();
                String parameterizedName = parameterizedName(no, parentInfo.getContext());
                CellNetInfo cni = getCellNetInfo(parameterizedName);
                SpiceNetlistReader.Subckt subckt = reader.getSubckts().get(parameterizedName.toLowerCase());
                if (subckt == null) {
                    System.out.println("Error: No subckt for "+parameterizedName+" found in included file: "+fileName);
                } else {
                    List<String> signals = new ArrayList<String>();
                    for (Iterator<CellSignal> sIt = cni.getCellSignals(); sIt.hasNext(); ) {
                        CellSignal cs = sIt.next();
                        if (ignoreSubcktPort(cs)) continue;
                        signals.add(cs.getName());
                    }
                    List<String> subcktSignals = subckt.getPorts();
                    if (signals.size() != subcktSignals.size()) {
                        System.out.println("Warning: wrong number of ports for subckt "+
                                parameterizedName+": expected "+signals.size()+", but found "+
                                subcktSignals.size()+", in included file "+fileName);
                    }
                    int len = Math.min(signals.size(), subcktSignals.size());
                    for (int i=0; i<len; i++) {
                        String s1 = signals.get(i);
                        String s2 = subcktSignals.get(i);
                        if (!s1.equalsIgnoreCase(s2)) {
                            System.out.println("Warning: port "+i+" of subckt "+parameterizedName+
                                    " is named "+s1+" in Electric, but "+s2+" in included file "+fileName);
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                System.out.println("Error validating included file: "+e.getMessage());
            }
        }
    }

    /**
	 * Method to adjust a network name to be safe for Spice output.
	 * Spice has a list of legal punctuation characters that it allows.
	 */
	protected String getSafeNetName(String name, boolean bus)
	{
        return getSafeNetName(name, bus, legalSpiceChars, spiceEngine);
    }

    /**
     * Method to adjust a network name to be safe for Spice output.
     * Spice has a list of legal punctuation characters that it allows.
     */
    public static String getSafeNetName(String name)
    {
        String legalSpiceChars = SPICELEGALCHARS;
        if (Simulation.getSpiceEngine() == Simulation.SpiceEngine.SPICE_ENGINE_P)
            legalSpiceChars = PSPICELEGALCHARS;
        return getSafeNetName(name, false, legalSpiceChars, Simulation.getSpiceEngine());
    }

    /**
     * Method to adjust a network name to be safe for Spice output.
     * Spice has a list of legal punctuation characters that it allows.
     */
    private static String getSafeNetName(String name, boolean bus, String legalSpiceChars, Simulation.SpiceEngine spiceEngine)
    {
        // simple names are trivially accepted as is
		boolean allAlNum = true;
		int len = name.length();
		if (len <= 0) return name;
		for(int i=0; i<len; i++)
		{
			boolean valid = TextUtils.isLetterOrDigit(name.charAt(i));
			if (i == 0) valid = Character.isLetter(name.charAt(i));
			if (!valid)
			{
				allAlNum = false;
				break;
			}
		}
		if (allAlNum) return name;

		StringBuffer sb = new StringBuffer();
		if (TextUtils.isDigit(name.charAt(0)) &&
			spiceEngine != Simulation.SpiceEngine.SPICE_ENGINE_G &&
			spiceEngine != Simulation.SpiceEngine.SPICE_ENGINE_P &&
			spiceEngine != Simulation.SpiceEngine.SPICE_ENGINE_2) sb.append('_');
		for(int t=0; t<name.length(); t++)
		{
			char chr = name.charAt(t);
			boolean legalChar = TextUtils.isLetterOrDigit(chr);
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

    /** Tell the Hierarchy enumerator whether or not to short parasitic resistors */
    protected boolean isShortResistors() {
        if (useCDL && Simulation.getCDLIgnoreResistors())
            return true;
        return false;
    }

    /** Tell the Hierarchy enumerator whether or not to short explicit (poly) resistors */
    protected boolean isShortExplicitResistors() {
        // until netlister is changed
        if (useCDL && Simulation.getCDLIgnoreResistors())
            return false;
        return false;
    }

	/**
	 * Method to tell whether the topological analysis should mangle cell names that are parameterized.
	 */
	protected boolean canParameterizeNames() { return true; } //return !useCDL; }

	/**
	 * Method to tell set a limit on the number of characters in a name.
	 * @return the limit to name size (SPICE limits to 32 character names?????). 
	 */
	protected int maxNameLength() { if (useCDL) return CDLMAXLENSUBCKTNAME; return SPICEMAXLENSUBCKTNAME; }

    protected boolean enumerateLayoutView(Cell cell) {
        return (CellModelPrefs.spiceModelPrefs.isUseLayoutView(cell));
    }

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

        String foundry = layoutTechnology.getSelectedFoundry() == null ? "" : (", foundry "+layoutTechnology.getSelectedFoundry().toString());
        multiLinePrint(true, "*** Layout tech: "+layoutTechnology.getTechName()+foundry+"\n");
        multiLinePrint(true, "*** UC SPICE *** , MIN_RESIST " + layoutTechnology.getMinResistance() +
			", MIN_CAPAC " + layoutTechnology.getMinCapacitance() + "FF\n");
        boolean useParasitics = useNewParasitics && (!useCDL) &&
                Simulation.isSpiceUseParasitics() && (cell.getView() == View.LAYOUT);
        if (useParasitics) {
            for (Layer layer : layoutTechnology.getLayersSortedByHeight()) {
                double edgecap = layer.getEdgeCapacitance();
                double areacap = layer.getCapacitance();
                double res = layer.getResistance();
                if (edgecap != 0 || areacap != 0 || res != 0) {
                    multiLinePrint(true, "***    "+layer.getName()+":\tareacap="+areacap+"FF/um^2,\tedgecap="+edgecap+"FF/um,\tres="+res+"ohms/sq\n");
                }
            }
        }
        multiLinePrint(false, ".OPTIONS NOMOD NOPAGE\n");

		// if sizes to be written in lambda, tell spice conversion factor
		if (Simulation.isSpiceWriteTransSizeInLambda())
		{
			double scale = layoutTechnology.getScale();
			multiLinePrint(true, "*** Lambda Conversion ***\n");
			multiLinePrint(false, ".opt scale=" + TextUtils.formatDouble(scale / 1000.0, 3) + "U\n\n");
		}

		// see if spice model/option cards from file if specified
		String headerFile = Simulation.getSpiceHeaderCardInfo();
		if (headerFile.length() > 0)
		{
			if (headerFile.startsWith(SPICE_EXTENSION_PREFIX))
			{
				// extension specified: look for a file with the cell name and that extension
				String headerPath = TextUtils.getFilePath(TextUtils.makeURLToFile(filePath));
                String ext = headerFile.substring(SPICE_EXTENSION_PREFIX.length());
                if (ext.startsWith(".")) ext = ext.substring(1);
				String filePart = cell.getName() + "." + ext;
				String fileName = headerPath + filePart;
				File test = new File(fileName);
				if (test.exists())
				{
					multiLinePrint(true, "* Model cards are described in this file:\n");
					addIncludeFile(filePart);
                    System.out.println("Spice Header Card '" + fileName + "' is included");
                    return;
				}
                else
                    System.out.println("Spice Header Card '" + fileName + "' cannot be loaded");
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
				String trailerpath = TextUtils.getFilePath(TextUtils.makeURLToFile(filePath));
                String ext = trailerFile.substring(SPICE_EXTENSION_PREFIX.length());
                if (ext.startsWith(".")) ext = ext.substring(1);
				String filePart = cell.getName() + "." + ext;
				String fileName = trailerpath + filePart;
				File test = new File(fileName);
				if (test.exists())
				{
					multiLinePrint(true, "* Trailer cards are described in this file:\n");
					addIncludeFile(filePart);
                    System.out.println("Spice Trailer Card '" + fileName + "' is included");
				}
                else
                    System.out.println("Spice Trailer Card '" + fileName + "' cannot be loaded");
			} else
			{
				// normal trailer file specified
				multiLinePrint(true, "* Trailer cards are described in this file:\n");
				addIncludeFile(trailerFile);
                System.out.println("Spice Trailer Card '" + trailerFile + "' is included");
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
	private void writeTwoPort(NodeInst ni, String partName, String extra, CellNetInfo cni, Netlist netList, VarContext context, SegmentedNets segmentedNets)
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
			String message = "WARNING: " + ni + " component not fully connected in " + ni.getParent();
			dumpErrorMessage(message);
		}
		if (cs0 != null && cs1 != null && cs0 == cs1)
		{
			String message = "WARNING: " + ni + " component appears to be shorted on net " + net0.toString() +
				" in " + ni.getParent();
			dumpErrorMessage(message);
			return;
		}

		if (ni.getName() != null) partName += getSafeNetName(ni.getName(), false);

        // add Mfactor if there
        StringBuffer sbExtra = new StringBuffer(extra);
        writeMFactor(context, ni, sbExtra);

        String name0 = cs0.getName();
        String name1 = cs1.getName();
        if (segmentedNets.getUseParasitics()) {
            name0 = segmentedNets.getNetName(port0);
            name1 = segmentedNets.getNetName(port1);
        }
		multiLinePrint(false, partName + " " + name1 + " " + name0 + " " + sbExtra.toString() + "\n");
	}

    /**
     * This adds formatting to a Spice parameter value.  It adds single quotes
     * around the param string if they do not already exist.
     * @param param the string param value (without the name= part).
     * @return a param string with single quotes around it
     */
    private static String formatParam(String param) {
        String value = trimSingleQuotes(param);
        try {
            Double.valueOf(value);
            return value;
        } catch (NumberFormatException e) {
            return ("'"+value+"'");
        }
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
	private void addNodeInformation(Netlist netList, HashMap<Network,SpiceNet> spiceNets, NodeInst ni)
	{
		// cells have no area or capacitance (for now)
		if (ni.isCellInstance()) return;  // No area for complex nodes

		PrimitiveNode.Function function = ni.getFunction();

		// initialize to examine the polygons on this node
		Technology tech = ni.getProto().getTechnology();
		AffineTransform trans = ni.rotateOut();

		// make linked list of polygons
		Poly [] polyList = tech.getShapeOfNode(ni, null, null, true, true, null);
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
			if (!layer.isDiffusionLayer() && layer.getCapacitance() == 0.0) continue;
			if (layer.getTechnology() != Technology.getCurrent()) continue;

			// leave out the gate capacitance of transistors
			if (layer.getFunction() == Layer.Function.GATE) continue;

			SpiceNet spNet = spiceNets.get(net);
			if (spNet == null) continue;

			// get the area of this polygon
			poly.transform(trans);
			spNet.merge.addPolygon(layer, poly);

			// count the number of transistors on this net
			if (layer.isDiffusionLayer() && function.isTransistor()) spNet.transistorCount++;
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
		boolean isDiffArc = ai.isDiffusionArc();    // check arc function

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

			if (layer.isDiffusionLayer()||
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
        if (useCDL) {
            multiLinePrint(false, ".include "+ fileName + "\n");
            return;
        }

		if (spiceEngine == Simulation.SpiceEngine.SPICE_ENGINE_2 || spiceEngine == Simulation.SpiceEngine.SPICE_ENGINE_3 ||
			spiceEngine == Simulation.SpiceEngine.SPICE_ENGINE_G || spiceEngine == Simulation.SpiceEngine.SPICE_ENGINE_S)
		{
			multiLinePrint(false, ".include " + fileName + "\n");
		} else if (spiceEngine == Simulation.SpiceEngine.SPICE_ENGINE_H || spiceEngine == Simulation.SpiceEngine.SPICE_ENGINE_H_ASSURA ||
                spiceEngine == Simulation.SpiceEngine.SPICE_ENGINE_H_CALIBRE)
		{
			multiLinePrint(false, ".include '" + fileName + "'\n");
		} else if (spiceEngine == Simulation.SpiceEngine.SPICE_ENGINE_P)
		{
			multiLinePrint(false, ".INC " + fileName + "\n");
		}
	}

    private void validateIncludeFile(String fileName, String subcktName) {
    }

    /******************** SUPPORT ********************/

	/**
	 * Method to return value if arc contains device active diffusion
	 */
//	private boolean arcIsDiff(ArcInst ai)
//	{
//		ArcProto.Function fun = ai.getProto().getFunction();
//        boolean newV = ai.isDiffusionArc();
//        boolean oldV = (fun == ArcProto.Function.DIFFP || fun == ArcProto.Function.DIFFN ||
//                fun == ArcProto.Function.DIFF || fun == ArcProto.Function.DIFFS || fun == ArcProto.Function.DIFFW);
//        if (newV != oldV)
//            System.out.println("Difference in arcIsDiff");
//        return oldV;
////		if (fun == ArcProto.Function.DIFFP || fun == ArcProto.Function.DIFFN || fun == ArcProto.Function.DIFF) return true;
////		if (fun == ArcProto.Function.DIFFS || fun == ArcProto.Function.DIFFW) return true;
////		return false;
//	}

    private static final boolean CELLISEMPTYDEBUG = false;
    private HashMap<Cell,Boolean> checkedCells = new HashMap<Cell,Boolean>();
    private boolean cellIsEmpty(Cell cell)
    {
        Boolean b = checkedCells.get(cell);
        if (b != null) return b.booleanValue();

        boolean empty = true;

        List<Cell> emptyCells = new ArrayList<Cell>();

        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();

            // if node is a cell, check if subcell is empty
            if (ni.isCellInstance()) {
                // ignore own icon
                if (ni.isIconOfParent()) {
                    continue;
                }
                Cell iconCell = (Cell)ni.getProto();
                Cell schCell = iconCell.contentsView();
                if (schCell == null) schCell = iconCell;
                if (cellIsEmpty(schCell)) {
                    if (CELLISEMPTYDEBUG) emptyCells.add(schCell);
                    continue;
                } else {
                    empty = false;
                    break;
                }
            }

            // otherwise, this is a primitive
            PrimitiveNode.Function fun = ni.getFunction();
            // Passive devices used by spice/CDL
            if (fun.isResistor() || // == PrimitiveNode.Function.RESIST ||
                fun == PrimitiveNode.Function.INDUCT ||
                fun.isCapacitor() || // == PrimitiveNode.Function.CAPAC || fun == PrimitiveNode.Function.ECAPAC ||
                fun == PrimitiveNode.Function.DIODE || fun == PrimitiveNode.Function.DIODEZ)
            {
                empty = false;
                break;
            }
            // active devices used by Spice/CDL
            if (((PrimitiveNode)ni.getProto()).getGroupFunction() == PrimitiveNode.Function.TRANS) {
                empty = false;
                break;
            }
        }
        // look for a model file on the current cell
        if (CellModelPrefs.spiceModelPrefs.isUseModelFromFile(cell)) {
            empty = false;
        }

        // empty
        if (CELLISEMPTYDEBUG && empty) {
            System.out.println(cell+" is empty and contains the following empty cells:");
            for (Cell c : emptyCells)
                System.out.println("   "+c.describe(true));
        }
        checkedCells.put(cell, new Boolean(empty));
        return empty;

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
				if (count >= spiceMaxLenLine && !insideQuotes && lastSpace > -1)
				{
					String partial = str.substring(lineStart, lastSpace+1);
					printWriter.print(partial + "\n" + contChar);
					count = count - partial.length();
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

    public static class FlatSpiceCodeVisitor extends HierarchyEnumerator.Visitor {

        private PrintWriter printWriter;
        private PrintWriter spicePrintWriter;
        private String filePath;
        Spice spice; // just used for file writing and formatting
        SegmentedNets segNets;

        public FlatSpiceCodeVisitor(String filePath, Spice spice) {
            this.spice = spice;
            this.spicePrintWriter = spice.printWriter;
            this.filePath = filePath;
            spice.spiceMaxLenLine = 1000;
            segNets = null;
        }

        public boolean enterCell(HierarchyEnumerator.CellInfo info) {
            return true;
        }

        public void exitCell(HierarchyEnumerator.CellInfo info) {
            Cell cell = info.getCell();
            for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				if (ni.getProto() != Generic.tech.invisiblePinNode) continue;
                Variable cardVar = ni.getVar(SPICE_CODE_FLAT_KEY);
                if (cardVar != null) {
                    if (printWriter == null) {
                        try {
                            printWriter = new PrintWriter(new BufferedWriter(new FileWriter(filePath)));
                        } catch (IOException e) {
                            System.out.println("Unable to open "+filePath+" for write.");
                            return;
                        }
                        spice.printWriter = printWriter;
                        segNets = new SegmentedNets(null, false, null, false);
                    }
                    spice.emitEmbeddedSpice(cardVar, info.getContext(), segNets, info, true);
                }
			}
        }

        public boolean visitNodeInst(Nodable ni, HierarchyEnumerator.CellInfo info) {
            return true;
        }

        public void close() {
            if (printWriter != null) {
                System.out.println(filePath+" written");
                spice.printWriter = spicePrintWriter;
                printWriter.close();
            }
            spice.spiceMaxLenLine = SPICEMAXLENLINE;
        }
    }
}
