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

import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.geometry.PolyMerge;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.CodeExpression;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Foundry;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveNodeSize;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.TransistorSize;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.generator.sclibrary.SCLibraryGen;
import com.sun.electric.tool.io.input.SimulationData;
import com.sun.electric.tool.io.input.spicenetlist.SpiceNetlistReader;
import com.sun.electric.tool.io.input.spicenetlist.SpiceSubckt;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations.NamePattern;
import com.sun.electric.tool.simulation.SimulationTool;
import com.sun.electric.tool.user.Exec;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.ExecDialog;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.util.TextUtils;

import java.awt.geom.AffineTransform;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import javax.swing.SwingUtilities;

/**
 * This is the Simulation Interface tool.
 */
public class Spice extends Topology
{
    private static final boolean DETECT_SPICE_PARAMS = true;
    private static final boolean USE_JAVA_CODE = true;

	/** key of Variable holding generic Spice templates. */		public static final Variable.Key SPICE_TEMPLATE_KEY = Variable.newKey("ATTR_SPICE_template");
	/** key of Variable holding Spice 2 templates. */			public static final Variable.Key SPICE_2_TEMPLATE_KEY = Variable.newKey("ATTR_SPICE_template_spice2");
	/** key of Variable holding Spice 3 templates. */			public static final Variable.Key SPICE_3_TEMPLATE_KEY = Variable.newKey("ATTR_SPICE_template_spice3");
	/** key of Variable holding HSpice templates. */			public static final Variable.Key SPICE_H_TEMPLATE_KEY = Variable.newKey("ATTR_SPICE_template_hspice");
	/** key of Variable holding PSpice templates. */			public static final Variable.Key SPICE_P_TEMPLATE_KEY = Variable.newKey("ATTR_SPICE_template_pspice");
	/** key of Variable holding GnuCap templates. */			public static final Variable.Key SPICE_GC_TEMPLATE_KEY = Variable.newKey("ATTR_SPICE_template_gnucap");
	/** key of Variable holding Smart Spice templates. */		public static final Variable.Key SPICE_SM_TEMPLATE_KEY = Variable.newKey("ATTR_SPICE_template_smartspice");
	/** key of Variable holding Smart Spice templates. */		public static final Variable.Key SPICE_A_TEMPLATE_KEY = Variable.newKey("ATTR_SPICE_template_assura");
	/** key of Variable holding Smart Spice templates. */		public static final Variable.Key SPICE_C_TEMPLATE_KEY = Variable.newKey("ATTR_SPICE_template_calibre");
	/** key of Variable holding Spice model file. */		    public static final Variable.Key SPICE_NETLIST_FILE_KEY = Variable.newKey("ATTR_SPICE_netlist_file");
	/** key of Variable holding SPICE code. */					public static final Variable.Key SPICE_CARD_KEY = Variable.newKey("SIM_spice_card");
	/** key of Variable holding SPICE declaration. */			public static final Variable.Key SPICE_DECLARATION_KEY = Variable.newKey("SIM_spice_declaration");
	/** key of Variable holding SPICE model. */					public static final Variable.Key SPICE_MODEL_KEY = Variable.newKey("SIM_spice_model");
	/** key of Variable holding SPICE flat code. */				public static final Variable.Key SPICE_CODE_FLAT_KEY = Variable.newKey("SIM_spice_code_flat");
	/** key of deprecated Variable holding model file */        public static final Variable.Key SPICE_MODEL_FILE_KEY = Variable.newKey("SIM_spice_behave_file");
    /** key of Variable holding generic CDL templates. */		public static final Variable.Key CDL_TEMPLATE_KEY = Variable.newKey("ATTR_CDL_template");
	/** Prefix for spice extension. */                          public static final String SPICE_EXTENSION_PREFIX = "Extension ";
	/** Prefix for spice null extension. */                     public static final String SPICE_NOEXTENSION_PREFIX = "N O N E ";

	/** maximum subcircuit name length */						private static final int SPICEMAXLENSUBCKTNAME     = 70;
    /** maximum subcircuit name length */						private static final int CDLMAXLENSUBCKTNAME       = 40;
    /** maximum subcircuit name length */						private static final int SPICEMAXLENLINE           = 78;
	/** legal characters in a spice deck */						private static final String SPICELEGALCHARS        = "!#$%*+-/<>[]_@";
	/** legal characters in a spice deck */						private static final String PSPICELEGALCHARS       = "!#$%*+-/<>[]_";
	/** legal characters in a CDL deck */						private static final String CDLNOBRACKETLEGALCHARS = "!#$%*+-/<>_";
    /** if CDL writes out empty subckt definitions */			private static final boolean CDLWRITESEMPTYSUBCKTS = false;
    /** A mark for uniquify */                                  private static final Set<Variable.Key> UNIQUIFY_MARK = Collections.emptySet();

	/** default Technology to use. */							private Technology layoutTechnology;
	/** Mask shrink factor (default =1) */						private double maskScale;
	/** True to write CDL format */								private boolean useCDL;
	/** Legal characters */										private String legalSpiceChars;
	/** Template Key for current spice engine */				private Variable.Key preferedEngineTemplateKey;
    /** Special case for HSpice for Assura */					private boolean assuraHSpice = false;
	/** Spice type: 2, 3, H, P, etc */							private SimulationTool.SpiceEngine spiceEngine;
	/** those cells that have overridden models */				private Map<Cell,String> modelOverrides = new HashMap<Cell,String>();
    /** Parameters used for Spice */                            private Map<NodeProto,Set<Variable.Key>> allSpiceParams = new HashMap<NodeProto,Set<Variable.Key>>();
    /** for RC parasitics */                                    private SpiceParasiticsGeneral parasiticInfo;
    /** Networks exempted during parasitic ext */				private SpiceExemptedNets exemptedNets;
    /** Whether or not to write empty subckts  */				private boolean writeEmptySubckts = true;
    /** max length per line */									private int spiceMaxLenLine = SPICEMAXLENLINE;

    /** Flat measurements file */								private FlatSpiceCodeVisitor spiceCodeFlat = null;

    /** map of "parameterized" cells not covered by Topology */	private Map<Cell,Cell> uniquifyCells;
    /** uniqueID */                                             private int uniqueID;
    /** map of shortened instance names */                      private Map<String,Integer> uniqueNames;
    /** local copy of preferences */                            private SpicePreferences localPrefs;

	public static class SpicePreferences extends OutputPreferences
    {
		public boolean                      cdl;
        public SimulationTool.SpiceEngine   engine = SimulationTool.getFactorySpiceEngine();
        public String                       level = SimulationTool.getFactorySpiceLevel();
        public int                          shortResistors = SimulationTool.getFactorySpiceShortResistors();
        public String                       runChoice = SimulationTool.getFactorySpiceRunChoice();
        public String                       runDir = SimulationTool.getFactorySpiceRunDir();
        public boolean                      useRunDir = SimulationTool.getFactorySpiceUseRunDir();
        public boolean                      outputOverwrite = SimulationTool.getFactorySpiceOutputOverwrite();
        public boolean                      runProbe = SimulationTool.getFactorySpiceRunProbe();
        public String                       runProgram = SimulationTool.getFactorySpiceRunProgram();
        public String                       runProgramArgs = SimulationTool.getFactorySpiceRunProgramArgs();
        public String                       partsLibrary = SimulationTool.getFactorySpicePartsLibrary();
        public String                       headerCardInfo = SimulationTool.getFactorySpiceHeaderCardInfo();
        public String                       trailerCardInfo = SimulationTool.getFactorySpiceTrailerCardInfo();

        public SimulationTool.SpiceParasitics parasiticsLevel = SimulationTool.getFactorySpiceParasiticsLevel();
    	public boolean                      parasiticsUseVerboseNaming = SimulationTool.isFactoryParasiticsUseVerboseNaming();
        public boolean                      parasiticsBackAnnotateLayout = SimulationTool.isFactoryParasiticsBackAnnotateLayout();
        public boolean                      parasiticsExtractPowerGround = SimulationTool.isFactoryParasiticsExtractPowerGround();
        public boolean                      parasiticsUseExemptedNetsFile = SimulationTool.isFactoryParasiticsUseExemptedNetsFile();
        public boolean                      parasiticsIgnoreExemptedNets = SimulationTool.isFactoryParasiticsIgnoreExemptedNets();
        public boolean                      parasiticsExtractsR = SimulationTool.isFactoryParasiticsExtractsR();
        public boolean                      parasiticsExtractsC = SimulationTool.isFactoryParasiticsExtractsC();

        public SimulationTool.SpiceGlobal   globalTreatment = SimulationTool.getFactorySpiceGlobalTreatment();
        public boolean                      writePwrGndInTopCell = SimulationTool.isFactorySpiceWritePwrGndInTopCell();
        public boolean                      useCellParameters = SimulationTool.isFactorySpiceUseCellParameters();
        public boolean                      writeTransSizeInLambda = SimulationTool.isFactorySpiceWriteTransSizeInLambda();
        public boolean                      writeSubcktTopCell = SimulationTool.isFactorySpiceWriteSubcktTopCell();
        public boolean                      writeTopCellInstance = true;
        public boolean                      writeEmptySubckts = true;
        public boolean                      writeFinalDotEnd = true;
        public boolean                      ignoreParasiticResistors = false;
        public String                       extractedNetDelimiter = SimulationTool.getFactorySpiceExtractedNetDelimiter();
        public boolean                      ignoreModelFiles = SimulationTool.isFactorySpiceIgnoreModelFiles();

        public String                       cdlLibName = SimulationTool.getFactoryCDLLibName();
        public String                       cdlLibPath = SimulationTool.getFactoryCDLLibPath();
        public boolean                      cdlConvertBrackets = SimulationTool.isFactoryCDLConvertBrackets();
        public String                       cdlIncludeFile = SimulationTool.getFactoryCDLIncludeFile();
        public boolean                      cdlIgnoreResistors = false;

        public Map<Cell,String>             modelFiles = Collections.emptyMap();
        public String                       workdir = "";

		public SpicePreferences() { this(false, false); }
		public SpicePreferences(boolean factory, boolean cdl)
		{
            super(factory);
			this.cdl = cdl;
            if (!factory)
                fillPrefs();
		}

        private void fillPrefs() {
            engine                          = SimulationTool.getSpiceEngine();
            level                           = SimulationTool.getSpiceLevel();
            shortResistors                  = SimulationTool.getSpiceShortResistors();
            runChoice                       = SimulationTool.getSpiceRunChoice();
            runDir                          = SimulationTool.getSpiceRunDir();
            useRunDir                       = SimulationTool.getSpiceUseRunDir();
            outputOverwrite                 = SimulationTool.getSpiceOutputOverwrite();
            runProbe                        = SimulationTool.getSpiceRunProbe();
            runProgram                      = SimulationTool.getSpiceRunProgram();
            runProgramArgs                  = SimulationTool.getSpiceRunProgramArgs();
            partsLibrary                    = SimulationTool.getSpicePartsLibrary();
            headerCardInfo                  = SimulationTool.getSpiceHeaderCardInfo();
            trailerCardInfo                 = SimulationTool.getSpiceTrailerCardInfo();

            parasiticsLevel                 = SimulationTool.getSpiceParasiticsLevel();
            parasiticsUseVerboseNaming      = SimulationTool.isParasiticsUseVerboseNaming();
            parasiticsBackAnnotateLayout    = SimulationTool.isParasiticsBackAnnotateLayout();
            parasiticsExtractPowerGround    = SimulationTool.isParasiticsExtractPowerGround();
            parasiticsUseExemptedNetsFile   = SimulationTool.isParasiticsUseExemptedNetsFile();
            parasiticsIgnoreExemptedNets    = SimulationTool.isParasiticsIgnoreExemptedNets();
            parasiticsExtractsR             = SimulationTool.isParasiticsExtractsR();
            parasiticsExtractsC             = SimulationTool.isParasiticsExtractsC();

            globalTreatment                 = SimulationTool.getSpiceGlobalTreatment();
            writePwrGndInTopCell            = SimulationTool.isSpiceWritePwrGndInTopCell();
            useCellParameters               = SimulationTool.isSpiceUseCellParameters();
            writeTransSizeInLambda          = SimulationTool.isSpiceWriteTransSizeInLambda();
            writeSubcktTopCell              = SimulationTool.isSpiceWriteSubcktTopCell();
            writeTopCellInstance            = SimulationTool.isSpiceWriteTopCellInstance();
            writeEmptySubckts               = SimulationTool.isSpiceWriteEmtpySubckts();
            writeFinalDotEnd                = SimulationTool.isSpiceWriteFinalDotEnd();
            ignoreParasiticResistors        = SimulationTool.isSpiceIgnoreParasiticResistors();
            extractedNetDelimiter           = SimulationTool.getSpiceExtractedNetDelimiter();
            ignoreModelFiles                = SimulationTool.isSpiceIgnoreModelFiles();

            cdlLibName                      = SimulationTool.getCDLLibName();
            cdlLibPath                      = SimulationTool.getCDLLibPath();
            cdlConvertBrackets              = SimulationTool.isCDLConvertBrackets();
            cdlIncludeFile                  = SimulationTool.getCDLIncludeFile();
            cdlIgnoreResistors              = SimulationTool.isCDLConvertBrackets();

            modelFiles                      = CellModelPrefs.spiceModelPrefs.getUnfilteredFileNames(EDatabase.clientDatabase());
            workdir                         = User.getWorkingDirectory();
        }

        @Override
        public Output doOutput(Cell cell, VarContext context, String filePath)
        {
    		Spice out = new Spice(this);
    		out.useCDL = cdl;
            out.spiceEngine = engine;
    		if (out.openTextOutputStream(filePath)) return out.finishWrite();
    		if (out.writeCell(cell, context)) return out.finishWrite();
    		if (out.closeTextOutputStream()) return out.finishWrite();
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
    			if (out.openTextOutputStream(templateFile)) return out.finishWrite();

    			String libName = cdlLibName;
    			String libPath = cdlLibPath;
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
    			if (out.closeTextOutputStream()) return out.finishWrite();
    			System.out.println(templateFile + " written");
    		}

            String runSpice = runChoice;
            if (!runSpice.equals(SimulationTool.spiceRunChoiceDontRun)) {
                String command = runProgram + " " + runProgramArgs;

                // see if user specified custom dir to run process in
                String workdir = User.getWorkingDirectory();
                String rundir = workdir;
                if (useRunDir) {
                    rundir = runDir;
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
                command = command.replaceAll("\\$\\{WORKING_DIR}", Matcher.quoteReplacement(workdir));
                command = command.replaceAll("\\$\\{USE_DIR}", Matcher.quoteReplacement(rundir));
                command = command.replaceAll("\\$\\{FILEPATH}", Matcher.quoteReplacement(filePath));
                command = command.replaceAll("\\$\\{FILENAME}", Matcher.quoteReplacement(filename));
                command = command.replaceAll("\\$\\{FILENAME_NO_EXT}", Matcher.quoteReplacement(filename_noext));

                // set up run probe
                Exec.FinishedListener l = new SpiceFinishedListener();

                if (runSpice.equals(SimulationTool.spiceRunChoiceRunIgnoreOutput))
                {
                    Exec e = new Exec(command, null, dir, null, null);
                    if (runProbe) e.addFinishedListener(l);
                    e.start();
                }
                if (runSpice.equals(SimulationTool.spiceRunChoiceRunReportOutput))
                {
                    ExecDialog dialog = new ExecDialog(TopLevel.getCurrentJFrame(), false);
                    if (runProbe) dialog.addFinishedListener(l);
                    dialog.startProcess(command, null, dir);
                }
                System.out.println("Running spice command: "+command);
            }

            if (parasiticsBackAnnotateLayout && out.parasiticInfo != null)
            {
                out.parasiticInfo.backAnnotate();
            }
            return out.finishWrite();
        }
    }

	/**
	 * Constructor for the Spice netlister.
	 */
	Spice(SpicePreferences sp) { localPrefs = sp; }

	/**
	 * Method called once by the traversal mechanism.
	 * Initializes Spice netlisting and writes headers.
	 */
	protected void start()
	{
		// find the proper technology to use if this is schematics
        if (topCell.getTechnology().isLayout())
            layoutTechnology = topCell.getTechnology();
        else
            layoutTechnology = Schematics.getDefaultSchematicTechnology();

		// make sure key is cached
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
        if (useCDL) {
            preferedEngineTemplateKey = CDL_TEMPLATE_KEY;
        }
        if (assuraHSpice || (useCDL && !CDLWRITESEMPTYSUBCKTS) ||
            (!useCDL && !localPrefs.writeEmptySubckts)) {
            writeEmptySubckts = false;
        }

        // get the mask scale
		maskScale = 1.0;

        // set up the parameterized cells
        uniquifyCells = new HashMap<Cell,Cell>();
        uniqueID = 0;
        uniqueNames = new HashMap<String,Integer>();
        markCellsToUniquify(topCell);

		// setup the legal characters
		legalSpiceChars = SPICELEGALCHARS;
		if (spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_P ||
			spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_G) legalSpiceChars = PSPICELEGALCHARS;

		// start writing the spice deck
		if (useCDL)
		{
			// setup bracket conversion for CDL
			if (localPrefs.cdlConvertBrackets)
				legalSpiceChars = CDLNOBRACKETLEGALCHARS;

			multiLinePrint(true, "* First line is ignored\n");
            // see if include file specified
            String headerPath = TextUtils.getFilePath(topCell.getLibrary().getLibFile());
            String filePart = localPrefs.cdlIncludeFile;
            if (!filePart.equals("")) {
                String fileName = headerPath + filePart;
                File test = new File(fileName);
                if (test.exists())
                {
                    multiLinePrint(true, "* Primitives described in this file:\n");
                    addIncludeFile(filePart);
                } else {
                    reportWarning("Warning: CDL Include file not found: "+fileName);
                }
            }
		} else
		{
			writeHeader(topCell);
            spiceCodeFlat = new FlatSpiceCodeVisitor(filePath+".flatcode", this);
            HierarchyEnumerator.enumerateCell(topCell, VarContext.globalContext, spiceCodeFlat, getShortResistorsFlat());
            spiceCodeFlat.close();
		}

        if (localPrefs.parasiticsUseExemptedNetsFile) {
            String headerPath = TextUtils.getFilePath(topCell.getLibrary().getLibFile());
            exemptedNets = new SpiceExemptedNets(new File(headerPath + File.separator + "exemptedNets.txt"));
        }
	}

	/**
	 * Method called once at the end of netlisting.
	 */
	protected void done()
	{
		if (!useCDL)
		{
			writeTrailer(topCell);
            if (localPrefs.writeFinalDotEnd)
                multiLinePrint(false, ".END\n");
		}
	}

	/**
	 * Method called by traversal mechanism to write one level of hierarchy in the Spice netlist.
	 * This could be the top level or a subcircuit.
	 * The bulk of the Spice netlisting happens here.
	 * @param cell the cell to write.
	 * @param cellName the name of the cell to use in the header.
	 * @param cni information from the hierarchy traverser.
	 * @param context the hierarchical cell context.
	 * @param info more information from the hierarchy traverser.
	 */
	protected void writeCellTopology(Cell cell, String cellName, CellNetInfo cni, VarContext context, Topology.MyCellInfo info)
	{
		// if this is the top level cell, write globals
		if (cell == topCell)
		{
            Netlist netList = cni.getNetList();
            Global.Set globals = netList.getGlobals();
            int globalSize = globals.size();
    		if (localPrefs.globalTreatment == SimulationTool.SpiceGlobal.USEGLOBALBLOCK)
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
            } else
    		{
        		// make sure power and ground appear at the top level
    			if (cni.getPowerNet() == null)
    				System.out.println("WARNING: cannot find power at top level of circuit");
    			if (cni.getGroundNet() == null)
    				System.out.println("WARNING: cannot find ground at top level of circuit");
    		}
        }

		// create electrical nets (SpiceNet) for every Network in the cell
		Netlist netList = cni.getNetList();
		Map<Network,SpiceNet> spiceNetMap = new HashMap<Network,SpiceNet>();
        for(Iterator<Network> it = netList.getNetworks(); it.hasNext(); )
        {
            Network net = it.next();
            SpiceNet spNet = new SpiceNet();
            spiceNetMap.put(net, spNet);
        }

        // for non-simple parasitics, create a SpiceSegmentedNets object to deal with them
        SimulationTool.SpiceParasitics spLevel = localPrefs.parasiticsLevel;
        if (useCDL || cell.getView() != View.LAYOUT) spLevel = SimulationTool.SpiceParasitics.SIMPLE;
        SpiceSegmentedNets segmentedNets = null;
        if (spLevel != SimulationTool.SpiceParasitics.SIMPLE)
        {
            // make the parasitics info object if it does not already exist
            if (parasiticInfo == null)
            {
                if (spLevel == SimulationTool.SpiceParasitics.RC_PROXIMITY)
                {
            		parasiticInfo = new SpiceParasitic(localPrefs);
                } else if (spLevel == SimulationTool.SpiceParasitics.RC_CONSERVATIVE)
                {
            		parasiticInfo = new SpiceRCSimple(localPrefs);
                }
            }
            segmentedNets = parasiticInfo.initializeSegments(cell, cni, layoutTechnology, exemptedNets, info);
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
			if (fun.isNTypeTransistor()) nmosTrans++; else
			if (fun.isPTypeTransistor()) pmosTrans++;
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
			dumpMessage(message, false);
		}
		if (nmosTrans != 0 && groundNet == null)
		{
			String message = "WARNING: no ground connection for N-transistor wells in " + cell;
			dumpMessage(message, false);
		}

		// generate header for subckt or top-level cell
        boolean forceEval = useCDL || !localPrefs.useCellParameters || detectSpiceParams(cell) == UNIQUIFY_MARK;
        String topLevelInstance = "";
		if (cell == topCell && !useCDL && !localPrefs.writeSubcktTopCell)
		{
			multiLinePrint(true, "\n*** TOP LEVEL CELL: " + cell.describe(false) + "\n");
		} else
		{
            if (!writeEmptySubckts) {
                if (cellIsEmpty(cell))
                    return;
            }

			multiLinePrint(true, "\n*** SUBCIRCUIT " + cellName + " FROM CELL " + cell.describe(false) + "\n");
			StringBuffer infstr = new StringBuffer();
			infstr.append(".SUBCKT " + cellName);
			for(Iterator<CellSignal> sIt = cni.getCellSignals(); sIt.hasNext(); )
			{
				CellSignal cs = sIt.next();
                if (ignoreSubcktPort(cs)) continue;
                if (!cs.isGlobal() && cs.getExport() == null) continue;

                // special case for parasitic extraction
                if (parasiticInfo != null && !cs.isGlobal() && cs.getExport() != null)
                {
                	parasiticInfo.writeSubcircuitHeader(cs, infstr);
                } else
                {
                    infstr.append(" " + cs.getName());
                }
			}

			Global.Set globals = netList.getGlobals();
			int globalSize = globals.size();
            if (cell == topCell && localPrefs.writeSubcktTopCell) {
                // create top level instantiation
                if (localPrefs.writeTopCellInstance)
                    topLevelInstance = infstr.toString().replaceFirst("\\.SUBCKT ", "X") + " " + cellName;
            }
			if (!useCDL && localPrefs.useCellParameters)
			{
				// add in parameters to this cell
				boolean firstParam = true;
                Set<Variable.Key> spiceParams = detectSpiceParams(cell);
				for(Iterator<Variable> it = cell.getParameters(); it.hasNext(); )
				{
					Variable paramVar = it.next();
                    if (DETECT_SPICE_PARAMS && !spiceParams.contains(paramVar.getKey())) continue;
                    String value = paramVar.getPureValue(-1);
                    if (USE_JAVA_CODE) {
                        value = evalParam(context, info.getParentInst(), paramVar, forceEval);
                    } else if (!isNetlistableParam(paramVar))
                        continue;
                    if (spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_O && firstParam)
                    {
                        infstr.append(" param:");
                    	firstParam = false;
                    }
                    infstr.append(" " + paramVar.getTrueName() + "=" + value);
				}
			}
			infstr.append("\n");
			multiLinePrint(false, infstr.toString());

			// write global comments
			if (localPrefs.globalTreatment == SimulationTool.SpiceGlobal.USEGLOBALBLOCK)
			{
				for(int i=0; i<globalSize; i++)
				{
					Global global = globals.get(i);
					Network net = netList.getNetwork(global);
					CellSignal cs = cni.getCellSignal(net);
					multiLinePrint(true, "** GLOBAL " + cs.getName() + "\n");
				}
			}
		}

		// write out any directly-typed SPICE declarations for the cell
		if (!useCDL)
		{
			boolean firstDecl = true;
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				if (ni.getProto() != Generic.tech().invisiblePinNode) continue;
				Variable cardVar = ni.getVar(SPICE_DECLARATION_KEY);
				if (cardVar == null) continue;
				if (firstDecl)
				{
					firstDecl = false;
					multiLinePrint(true, "\n* Spice Declaration nodes in cell " + cell + "\n");
				}
				emitEmbeddedSpice(cardVar, context, segmentedNets, info, false, forceEval);
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

				// get the SPICE template on the prototype (if any)
				Variable varTemplate = getEngineTemplate(subCell);
				if (varTemplate != null)
				{
					if (varTemplate.getObject() instanceof Object[])
					{
						Object [] manyLines = (Object [])varTemplate.getObject();
						for(int i=0; i<manyLines.length; i++)
						{
							String line = manyLines[i].toString();
							StringBuffer infstr = replacePortsAndVars(line, no, context, cni, segmentedNets, info, false, forceEval);
		                    // Writing MFactor if available. Not sure here
							if (i == 0) writeMFactor(context, no, infstr);
							infstr.append('\n');
							multiLinePrint(false, infstr.toString());
						}
					} else
					{
						String line = varTemplate.getObject().toString();
						StringBuffer infstr = replacePortsAndVars(line, no, context, cni, segmentedNets, info, false, forceEval);
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

                if (!writeEmptySubckts)
                {
                    // do not instantiate if empty
                    if (cellIsEmpty((Cell)niProto)) continue;
                }

				String modelChar = "X";
				if (no.getName() != null) modelChar += getSafeNetName(no.getName(), false);
				StringBuffer infstr = new StringBuffer();
				infstr.append(modelChar);
				for(Iterator<CellSignal> sIt = subCni.getCellSignals(); sIt.hasNext(); )
				{
					CellSignal subCS = sIt.next();
	                if (ignoreSubcktPort(subCS)) continue;
					PortProto pp = subCS.getExport();
	                if (!subCS.isGlobal() && pp == null) continue;

	                // If global pwr/vdd will be included in the subcircuit
                    // Preparing code for bug #1828
//					if (!localPrefs.writePwrGndInTopCell && pp!= null && subCS.isGlobal() && (subCS.isGround() || subCS.isPower()))
//						continue;

                    Network net;
                    int exportIndex = subCS.getExportIndex();

                    // This checks if we are netlisting a schematic top level with swapped-in layout subcells
                    if (pp != null && cell.isSchematic() && (subCni.getCell().getView() == View.LAYOUT))
                    {
                        // find equivalent pp from layout to schematic
                        Network subNet = subCS.getNetwork();  // layout network name
                        boolean found = false;
                        for (Iterator<Export> eIt = subCell.getExports(); eIt.hasNext(); )
                        {
                            Export ex = eIt.next();
                            for (int i=0; i<ex.getNameKey().busWidth(); i++)
                            {
                                String exName = ex.getNameKey().subname(i).toString();
                                if (exName.equals(subNet.getName()))
                                {
                                    pp = ex;
                                    exportIndex = i;
                                    found = true;
                                    break;
                                }
                            }
                            if (found) break;
                        }
                        if (!found)
                        {
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

                    if (subCS.isGlobal())
                    {
                        net = netList.getNetwork(no, subCS.getGlobal());
                    }
                    else
                        net = netList.getNetwork(no, pp, exportIndex);
					if (net == null)
					{
						reportWarning("Warning: cannot find network for signal " + subCS.getName() + " in cell " +
							subCni.getCell().describe(false));
						continue;
					}
					CellSignal cs = cni.getCellSignal(net);

                    // special case for parasitic extraction
					SpiceSegmentedNets subSN = null;
                    if (parasiticInfo != null && !cs.isGlobal())
                    	subSN = parasiticInfo.getSegmentedNets((Cell)no.getProto());
                    if (subSN != null)
                    {
                    	parasiticInfo.getParasiticName(no, subCS.getNetwork(), subSN, infstr);
                    } else
                    {
                        String name = cs.getName();
                        if (parasiticInfo != null)
                        {
                            name = segmentedNets.getNetName(no.getNodeInst().findPortInstFromProto(pp));
                        }
                        infstr.append(" " + name);
                    }
				}

                if (useCDL) infstr.append(" /"); else infstr.append(" ");
                String subCellName = subCni.getParameterizedName();

                // make sure to use the correct icon cell name if there are more than one
                NodeInst ni = no.getNodeInst();
                if (ni != null)
                {
                	String alternateSubCellName = getIconCellName((Cell)ni.getProto());
                	if (alternateSubCellName != null) subCellName = alternateSubCellName;
                }
                infstr.append(subCellName);

				if (!useCDL && localPrefs.useCellParameters)
				{
					// add in parameters to this instance
                    Set<Variable.Key> spiceParams = detectSpiceParams(subCell);
					for(Iterator<Variable> it = subCell.getParameters(); it.hasNext(); )
					{
						Variable paramVar = it.next();
                        if (DETECT_SPICE_PARAMS && !spiceParams.contains(paramVar.getKey())) continue;
                        if (!USE_JAVA_CODE && !isNetlistableParam(paramVar)) continue;
                        Variable instVar = no.getParameter(paramVar.getKey());
                        String paramStr = "??";
						if (instVar != null)
						{
                            if (USE_JAVA_CODE)
                            {
                                paramStr = evalParam(context, no, instVar, forceEval);
                            } else
                            {
                                Object obj = null;
                                if (isNetlistableParam(instVar))
                                {
                                    obj = context.evalSpice(instVar, false);
                                } else
                                {
                                    obj = context.evalVar(instVar, no);
                                }
                                if (obj != null)
                                    paramStr = formatParam(String.valueOf(obj), instVar.getUnit(), false);
                            }
                        }
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

			// look for a SPICE template on the primitive
			String line = ((PrimitiveNode)ni.getProto()).getSpiceTemplate();
			if (line != null)
			{
				StringBuffer infstr = replacePortsAndVars(line, no, context, cni, segmentedNets, info, false, forceEval);
                // Writing MFactor if available. Not sure here
				writeMFactor(context, no, infstr);

				infstr.append('\n');
				multiLinePrint(false, infstr.toString());
				continue;
			}

			// handle resistors, inductors, capacitors, and diodes
			PrimitiveNode.Function fun = ni.getFunction();
			if (fun.isResistor() || fun.isCapacitor() ||
                fun == PrimitiveNode.Function.INDUCT ||
				fun == PrimitiveNode.Function.DIODE || fun == PrimitiveNode.Function.DIODEZ)
			{
				if (fun.isResistor())
				{
                    if ((fun.isComplexResistor() && isShortExplicitResistors()) ||
                        (fun == PrimitiveNode.Function.RESIST && isShortResistors()))
                        	continue;
					Variable resistVar = ni.getVar(Schematics.SCHEM_RESISTANCE);
					String extra = "";
                    String partName = "R";
					if (resistVar != null)
					{
                        if (USE_JAVA_CODE) {
                            extra = evalParam(context, ni, resistVar, forceEval);
                        } else {
                            if (resistVar.getCode() == CodeExpression.Code.SPICE) {
                                if (!useCDL && localPrefs.useCellParameters) {
                                    Object obj = context.evalSpice(resistVar, false);
                                    extra = String.valueOf(obj);
                                } else {
                                    extra = resistVar.describe(context, ni);
                                }
                            }
                            if (extra == "")
                                extra = resistVar.describe(context, ni);
                            if (TextUtils.isANumber(extra))
                            {
                                double pureValue = TextUtils.atof(extra);
                                extra = TextUtils.formatDoublePostFix(pureValue); //displayedUnits(pureValue, TextDescriptor.Unit.RESISTANCE, TextUtils.UnitScale.NONE);
                            } else
                                extra = formatParam(extra, resistVar.getUnit(), false);
                        }
					} else {
                        if (fun.isComplexResistor())
                        {
                            partName = "XR";
                            double width = ni.getLambdaBaseYSize();
                            double length = ni.getLambdaBaseXSize();
                            if (localPrefs.writeTransSizeInLambda)
                            {
                                extra = " L="+length+" W="+width;
                            } else
                            {
                                extra = " L="+formatParam(length+"*LAMBDA", TextDescriptor.Unit.NONE, false)+
                                	" W="+formatParam(width+"*LAMBDA", TextDescriptor.Unit.NONE, false);
                            }
                            String prepend = "";
                            if (fun == PrimitiveNode.Function.RESPPOLY) prepend = "rppo1rpo"; else
                            	if (fun == PrimitiveNode.Function.RESNPOLY) prepend = "rnpo1rpo"; else
                                    if (fun == PrimitiveNode.Function.RESPNSPOLY) prepend = "rpponsrpo"; else   // made up value
                            	        if (fun == PrimitiveNode.Function.RESNNSPOLY) prepend = "rnponsrpo"; else  // made up value
                                            if (fun == PrimitiveNode.Function.RESPWELL) prepend = "rpwod "; else
                                                if (fun == PrimitiveNode.Function.RESNWELL) prepend = "rnwod "; else
                                                   if (fun == PrimitiveNode.Function.RESPACTIVE) prepend = "rpaod "; else
                                                       if (fun == PrimitiveNode.Function.RESNACTIVE) prepend = "rnaod ";
                            if (layoutTechnology == Technology.getCMOS90Technology() ||
                                (cell.getView() == View.LAYOUT && cell.getTechnology() == Technology.getCMOS90Technology()))
                            {
                                if (fun == PrimitiveNode.Function.RESPPOLY) prepend = "GND rpporpo"; else
                                	if (fun == PrimitiveNode.Function.RESNPOLY) prepend = "GND rnporpo";
                            }
                            extra = prepend + extra;
                        }
                    }
					writeTwoPort(ni, partName, extra, cni, netList, context, segmentedNets);
				} else if (fun.isCapacitor())
				{
					Variable capacVar = ni.getVar(Schematics.SCHEM_CAPACITANCE);
					String extra = "";
					if (capacVar != null)
					{
                        if (USE_JAVA_CODE) {
                            extra = evalParam(context, no, capacVar, forceEval);
                        } else {
                            if (capacVar.getCode() == CodeExpression.Code.SPICE) {
                                if (!useCDL && localPrefs.useCellParameters) {
                                    Object obj = context.evalSpice(capacVar, false);
                                    extra = String.valueOf(obj);
                                } else {
                                    extra = capacVar.describe(context, ni);
                                }
                            }
                            if (extra == "")
                                extra = capacVar.describe(context, ni);
                            if (TextUtils.isANumber(extra))
                            {
                                double pureValue = TextUtils.atof(extra);
                                extra = TextUtils.formatDoublePostFix(pureValue); // displayedUnits(pureValue, TextDescriptor.Unit.CAPACITANCE, TextUtils.UnitScale.NONE);
                            } else
                                extra = formatParam(extra, capacVar.getUnit(), false);
                        }
					}
					writeTwoPort(ni, "C", extra, cni, netList, context, segmentedNets);
				} else if (fun == PrimitiveNode.Function.INDUCT)
				{
					Variable inductVar = ni.getVar(Schematics.SCHEM_INDUCTANCE);
					String extra = "";
					if (inductVar != null)
					{
                        if (USE_JAVA_CODE) {
                            extra = evalParam(context, no, inductVar, forceEval);
                        } else {
                            if (inductVar.getCode() == CodeExpression.Code.SPICE) {
                                if (!useCDL && localPrefs.useCellParameters) {
                                    Object obj = context.evalSpice(inductVar, false);
                                    extra = String.valueOf(obj);
                                } else {
                                    extra = inductVar.describe(context, ni);
                                }
                            }
                            if (extra == "")
                                extra = inductVar.describe(context, ni);
                            if (TextUtils.isANumber(extra))
                            {
                                double pureValue = TextUtils.atof(extra);
                                extra = TextUtils.formatDoublePostFix(pureValue); // displayedUnits(pureValue, TextDescriptor.Unit.INDUCTANCE, TextUtils.UnitScale.NONE);
                            } else
                                extra = formatParam(extra, inductVar.getUnit(), false);
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
				dumpMessage(message, false);
			}

			// get model information
			String modelName = null;
            String defaultBulkName = null;
			Variable modelVar = ni.getVar(SPICE_MODEL_KEY);
			if (modelVar != null) modelName = modelVar.getObject().toString();

            // special case for ST090 technology which has stupid non-standard transistor
            // models which are subcircuits
            boolean st090laytrans = false;
            boolean tsmc090laytrans = false;
            if (cell.getView() == View.LAYOUT && layoutTechnology == Technology.getCMOS90Technology()) {
                if (layoutTechnology.getSelectedFoundry().getType() == Foundry.Type.TSMC)
                    tsmc090laytrans = true;
                else if (layoutTechnology.getSelectedFoundry().getType() == Foundry.Type.ST)
                    st090laytrans = true;
            }

			String modelChar = "";
			if (fun == PrimitiveNode.Function.TRANSREF)					// self-referential transistor
			{
				modelChar = "X";
				if (biasCs == null) biasCs = cni.getCellSignal(groundNet);
				modelName = niProto.getName();
			} else if (fun == PrimitiveNode.Function.TRANMOS)			// NMOS (Enhancement) transistor
			{
				modelChar = "M";
				if (biasCs == null) biasCs = cni.getCellSignal(groundNet);
                defaultBulkName = "gnd";
				if (modelName == null) modelName = "N";
                if (st090laytrans) {
                    modelChar = "XM";
                    modelName = "nsvt";
                }
                if (tsmc090laytrans) {
                    modelName = "nch";
                }
            } else if (fun == PrimitiveNode.Function.TRA4NMOS)			// NMOS (Complementary) 4-port transistor
			{
				modelChar = "M";
				if (modelName == null) modelName = "N";
                if (st090laytrans) {
                    modelChar = "XM";
                    modelName = "nsvt";
                }
                if (tsmc090laytrans) {
                    modelName = "nch";
                }
			} else if (fun == PrimitiveNode.Function.TRADMOS)			// DMOS (Depletion) transistor
			{
				modelChar = "M";
				if (biasCs == null) biasCs = cni.getCellSignal(groundNet);
				if (modelName == null) modelName = "D";
			} else if (fun == PrimitiveNode.Function.TRA4DMOS)			// DMOS (Depletion) 4-port transistor
			{
				modelChar = "M";
				if (modelName == null) modelName = "D";
			} else if (fun == PrimitiveNode.Function.TRAPMOS)			// PMOS (Complementary) transistor
			{
				modelChar = "M";
				if (biasCs == null) biasCs = cni.getCellSignal(powerNet);
                defaultBulkName = "vdd";
				if (modelName == null) modelName = "P";
                if (st090laytrans) {
                    modelChar = "XM";
                    modelName = "psvt";
                }
                if (tsmc090laytrans) {
                    modelName = "pch";
                }
            } else if (fun == PrimitiveNode.Function.TRA4PMOS)			// PMOS (Complementary) 4-port transistor
			{
				modelChar = "M";
				if (modelName == null) modelName = "P";
                if (st090laytrans) {
                    modelChar = "XM";
                    modelName = "psvt";
                }
                if (tsmc090laytrans) {
                    modelName = "pch";
                }
            } else if (fun == PrimitiveNode.Function.TRANPN)			// NPN (Junction) transistor
			{
				modelChar = "Q";
				if (modelName == null) modelName = "NBJT";
			} else if (fun == PrimitiveNode.Function.TRA4NPN)			// NPN (Junction) 4-port transistor
			{
				modelChar = "Q";
				if (modelName == null) modelName = "NBJT";
			} else if (fun == PrimitiveNode.Function.TRAPNP)			// PNP (Junction) transistor
			{
				modelChar = "Q";
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
 			} else if (fun == PrimitiveNode.Function.TRANMOSCN)			// Pallav: NMOS (carbon-nanotube) transistor
			{
				modelChar = "X";
				biasCs = cni.getCellSignal(groundNet);
				defaultBulkName = "gnd";
				modelName = "NCNFET";
			} else if (fun == PrimitiveNode.Function.TRA4NMOSCN)		// Pallav: NMOS (carbon-nanotube) 4-port transistor
			{
				modelChar = "X";
				modelName = "NCNFET";
			} else if (fun == PrimitiveNode.Function.TRAPMOSCN)			// Pallav: PMOS (carbon-nanotube) transistor
			{
				modelChar = "X";
				biasCs = cni.getCellSignal(powerNet);
				defaultBulkName = "vdd";
				modelName = "PCNFET";
			} else if (fun == PrimitiveNode.Function.TRA4PMOSCN)		// Pallav: PMOS (carbon-nanotube) 4-port transistor
			{
				modelChar = "X";
				modelName = "PCNFET";
			} else if (fun == PrimitiveNode.Function.TRANS)				// special transistor
			{
				modelChar = "Q";
			}
			if (ni.getName() != null) modelChar += getSafeNetName(ni.getName(), false);
			StringBuffer infstr = new StringBuffer();
			String drainName = drainCs.getName();
			String gateName = gateCs.getName();
			String sourceName = sourceCs.getName();
            if (segmentedNets != null) {
                drainName = segmentedNets.getNetName(ni.getTransistorDrainPort());
                gateName = segmentedNets.getNetName(ni.getTransistorGatePort());
                sourceName = segmentedNets.getNetName(ni.getTransistorSourcePort());
            }
			infstr.append(modelChar + " " + drainName + " " + gateName + " " + sourceName);
			if (biasCs != null) {
                String biasName = biasCs.getName();
                if (segmentedNets != null && ni.getTransistorBiasPort() != null)
                {
                    String bn = segmentedNets.getNetName(ni.getTransistorBiasPort());
                    if (bn != null) biasName = bn;
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
                reportWarning("Warning: transistor has null size " + ni.describe(false));
            else
            {
            	// write the length
            	Double foundLen = null;
                Variable varLen = ni.getVar(Schematics.ATTR_LENGTH);
                if (varLen != null && varLen.getCode() == CodeExpression.Code.SPICE &&
                	!useCDL && localPrefs.useCellParameters)
                {
                	// write as a parameter
                    infstr.append(" L=" + evalParam(context, no, varLen, forceEval));
                } else
                {
                	// write the value, start by getting gate length subtraction in lambda
                    double lengthSubtraction = layoutTechnology.getGateLengthSubtraction() /
                    	layoutTechnology.getScale() * 1000;
	                if (size.getDoubleLength() > 0)
	                {
	                    double l = maskScale * size.getDoubleLength();
	                    l -= lengthSubtraction;

	                    // make into microns (convert to nanometers then divide by 1000)
	                    if (!localPrefs.writeTransSizeInLambda)
	                        l *= layoutTechnology.getScale() / 1000.0;

	                    if (fun == PrimitiveNode.Function.TRANMOS  || fun == PrimitiveNode.Function.TRA4NMOS ||
	                        fun == PrimitiveNode.Function.TRAPMOS || fun == PrimitiveNode.Function.TRA4PMOS ||
	                        fun == PrimitiveNode.Function.TRADMOS || fun == PrimitiveNode.Function.TRA4DMOS ||
	                        ((fun == PrimitiveNode.Function.TRANJFET || fun == PrimitiveNode.Function.TRAPJFET ||
	                          fun == PrimitiveNode.Function.TRADMES || fun == PrimitiveNode.Function.TRAEMES) &&
	                          (spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_H || spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_H_ASSURA)))
	                    {
	                        // schematic transistors may be text
	                        if (size.getDoubleLength() == 0 && size.getLength() instanceof String)
	                        {
                                infstr.append(" L=" + formatParam((String)size.getLength(), TextDescriptor.Unit.DISTANCE, false));
	                            if (lengthSubtraction != 0) infstr.append(" - " + lengthSubtraction);
	                        } else
	                        {
	                            infstr.append(" L=" + TextUtils.formatDouble(l));
	                            if (!localPrefs.writeTransSizeInLambda && !st090laytrans) infstr.append("U");
	                        }
	                    }
	                	foundLen = new Double(l);
	                } else
	                {
	                    // get gate length subtraction in lambda
	                    if (size.getDoubleLength() == 0 && size.getLength() instanceof String)
	                    {
                            infstr.append(" L=" + formatParam((String)size.getLength(), TextDescriptor.Unit.DISTANCE, false));
	                        if (lengthSubtraction != 0) infstr.append(" - " + lengthSubtraction);
	                    }
	                }
                }

                // write the width
            	Double foundWid = null;
                Variable varWid = ni.getVar(Schematics.ATTR_WIDTH);
                if (varWid != null && varWid.getCode() == CodeExpression.Code.SPICE &&
                	!useCDL && localPrefs.useCellParameters)
                {
                	// write as a parameter
                    infstr.append(" W=" + evalParam(context, no, varWid, forceEval));
                } else
                {
                	// write the value
	                if (size.getDoubleWidth() > 0)
	                {
	                    double w = maskScale * size.getDoubleWidth();

	                    // make into microns (convert to nanometers then divide by 1000)
	                    if (!localPrefs.writeTransSizeInLambda)
	                        w *= layoutTechnology.getScale() / 1000.0;

	                    if (fun == PrimitiveNode.Function.TRANMOS  || fun == PrimitiveNode.Function.TRA4NMOS ||
	                        fun == PrimitiveNode.Function.TRAPMOS || fun == PrimitiveNode.Function.TRA4PMOS ||
	                        fun == PrimitiveNode.Function.TRADMOS || fun == PrimitiveNode.Function.TRA4DMOS ||
	                        ((fun == PrimitiveNode.Function.TRANJFET || fun == PrimitiveNode.Function.TRAPJFET ||
	                          fun == PrimitiveNode.Function.TRADMES || fun == PrimitiveNode.Function.TRAEMES) &&
	                          (spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_H || spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_H_ASSURA)))
	                    {
	                        if ((size.getDoubleWidth() == 0) && (size.getWidth() instanceof String)) {
	                            infstr.append(" W="+formatParam((String)size.getWidth(), TextDescriptor.Unit.DISTANCE, false));
	                        } else {
	                            infstr.append(" W=" + TextUtils.formatDouble(w));
	                            if (!localPrefs.writeTransSizeInLambda && !st090laytrans) infstr.append("U");
	                        }
	                    }
	                	foundWid = new Double(w);
	                } else
	                {
	                    // get gate length subtraction in lambda
	                    if (size.getDoubleWidth() == 0 && size.getWidth() instanceof String)
	                        infstr.append(" W="+formatParam((String)size.getWidth(), TextDescriptor.Unit.DISTANCE, false));
	                }
                }

				// write area if appropriate
				if (fun != PrimitiveNode.Function.TRANMOS && fun != PrimitiveNode.Function.TRA4NMOS &&
					fun != PrimitiveNode.Function.TRAPMOS && fun != PrimitiveNode.Function.TRA4PMOS &&
					fun != PrimitiveNode.Function.TRADMOS && fun != PrimitiveNode.Function.TRA4DMOS &&
					fun != PrimitiveNode.Function.TRANMOSCN && fun != PrimitiveNode.Function.TRA4NMOSCN &&
					fun != PrimitiveNode.Function.TRAPMOSCN && fun != PrimitiveNode.Function.TRA4PMOSCN)
				{
					if (foundLen != null && foundWid != null)
					{
						infstr.append(" AREA=" + TextUtils.formatDouble(foundLen.doubleValue()*foundWid.doubleValue()));
						if (!localPrefs.writeTransSizeInLambda) infstr.append("P");
					}
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
						if (!localPrefs.writeTransSizeInLambda)
						{
							as *= layoutTechnology.getScale() * layoutTechnology.getScale() / 1000000.0;
							ps *= layoutTechnology.getScale() / 1000.0;
						}
					}
					if (spNetDrain.transistorCount != 0)
					{
						ad = spNetDrain.diffArea / spNetDrain.transistorCount;
						pd = spNetDrain.diffPerim / spNetDrain.transistorCount;
						if (!localPrefs.writeTransSizeInLambda)
						{
							ad *= layoutTechnology.getScale() * layoutTechnology.getScale() / 1000000.0;
							pd *= layoutTechnology.getScale() / 1000.0;
						}
					}
					if (as > 0.0)
					{
						infstr.append(" AS=" + TextUtils.formatDouble(as));
						if (!localPrefs.writeTransSizeInLambda && !st090laytrans) infstr.append("P");
					}
					if (ad > 0.0)
					{
						infstr.append(" AD=" + TextUtils.formatDouble(ad));
						if (!localPrefs.writeTransSizeInLambda && !st090laytrans) infstr.append("P");
					}
					if (ps > 0.0)
					{
						infstr.append(" PS=" + TextUtils.formatDouble(ps));
						if (!localPrefs.writeTransSizeInLambda && !st090laytrans) infstr.append("U");
					}
					if (pd > 0.0)
					{
						infstr.append(" PD=" + TextUtils.formatDouble(pd));
						if (!localPrefs.writeTransSizeInLambda && !st090laytrans) infstr.append("U");
					}
				}
			}

			// Writing MFactor if available.
			writeMFactor(context, ni, infstr);

			infstr.append("\n");
			multiLinePrint(false, infstr.toString());
		}

		// print resistances and capacitances
		if (segmentedNets != null)
		{
			if (spLevel == SimulationTool.SpiceParasitics.RC_PROXIMITY)
			{
				parasiticInfo.writeNewSpiceCode(cell, cni, layoutTechnology, this);
			} else {
				// write caps
				int capCount = 0;
				multiLinePrint(true, "** Extracted Parasitic Capacitors ***\n");
		        for (SpiceSegmentedNets.NetInfo netInfo : segmentedNets.getUniqueSegments()) {
	                if (netInfo.getCap() > cell.getTechnology().getMinCapacitance()) {
	                    if (netInfo.getName().equals("gnd")) continue;           // don't write out caps from gnd to gnd
	                    multiLinePrint(false, "C" + capCount + " " + netInfo.getName() + " 0 " + TextUtils.formatDouble(netInfo.getCap()) + "fF\n");
	                    capCount++;
	                }
	            }

	            // write resistors
	            int resCount = 0;
	            multiLinePrint(true, "** Extracted Parasitic Resistors ***\n");
	            for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); ) {
	                ArcInst ai = it.next();
	                Double res = segmentedNets.getRes(ai);
	                if (res == null) continue;
	                String n0 = segmentedNets.getNetName(ai.getHeadPortInst());
	                String n1 = segmentedNets.getNetName(ai.getTailPortInst());
	                int arcPImodels = SpiceSegmentedNets.getNumPISegments(res.doubleValue(), layoutTechnology.getMaxSeriesResistance());
	                if (arcPImodels > 1) {
	                    // have to break it up into smaller pieces
	                    double segCap = segmentedNets.getArcCap(ai)/(arcPImodels+1);
	                    double segRes = res.doubleValue()/arcPImodels;
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
	                            if (!segn1.equals("gnd") && segCap > layoutTechnology.getMinCapacitance()) {
	                                String capVal = TextUtils.formatDouble(segCap);
	                                if (!capVal.equals("0.00")) {
	                                    multiLinePrint(false, "C"+capCount+" "+segn1+" 0 "+capVal+"fF\n");
	                                    capCount++;
	                                }
	                            }
	                        }
	                        segn0 = segn1;
	                    }
	                } else {
	                    multiLinePrint(false, "R" + resCount + " " + n0 + " " + n1 + " " + TextUtils.formatDouble(res.doubleValue()) + "\n");
	                    resCount++;
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
				if (ni.getProto() != Generic.tech().invisiblePinNode) continue;
				Variable cardVar = ni.getVar(SPICE_CARD_KEY);
				if (cardVar == null) continue;
				if (firstDecl)
				{
					firstDecl = false;
					multiLinePrint(true, "\n* Spice Code nodes in cell " + cell + "\n");
				}
				emitEmbeddedSpice(cardVar, context, segmentedNets, info, false, forceEval);
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
                for (Network net : netsToConnect) {
                    if (name != null) {
                        multiLinePrint(false, "R"+name+" "+name+" "+net.getName()+" 0.001\n");
                    }
                    name = net.getName();
                }
            }
        }

		// now we're finished writing the subcircuit.
		if (cell != topCell || useCDL || localPrefs.writeSubcktTopCell)
		{
			multiLinePrint(false, ".ENDS " + cni.getParameterizedName() + "\n");
		}
        if (cell == topCell && localPrefs.writeSubcktTopCell) {
            multiLinePrint(false, "\n\n"+topLevelInstance+"\n\n");
        }
	}

    /****************************** PARAMETERS ******************************/

    /**
     * Method to create a parameterized name for node instance "ni".
     * If the node is not parameterized, returns zero.
     * If it returns a name, that name must be deallocated when done.
     */
    protected String parameterizedName(Nodable no, VarContext context)
    {
        Cell cell = (Cell)no.getProto();
        String uniqueName = getUniqueCellName(cell);
        if (uniqueName == null)
        {
            uniqueName = cell.getName();
            String msg = "Cell " + cell.describe(true) + " is missing information. Corresponding schematic might be missing";
            msg += " Taking '" + uniqueName + "' now.";
            dumpMessage(msg, true);
        }
        StringBuffer uniqueCellName = new StringBuffer(uniqueName);

        if (uniquifyCells.get(cell) != null && modelOverrides.get(cell) == null) {
            // if this cell is marked to be make unique, make a unique name out of the var context
            VarContext vc = context.push(no);
            uniqueCellName.append("_"+vc.getInstPath("."));
        } else {
            boolean useCellParams = !useCDL && localPrefs.useCellParameters;
            if (canParameterizeNames() && no.isCellInstance() && !SCLibraryGen.isStandardCell(cell))
            {
                // if there are parameters, append them to this name
                Set<Variable.Key> spiceParams = detectSpiceParams(cell);
                List<Variable> paramValues = new ArrayList<Variable>();
                for(Iterator<Variable> it = no.getDefinedParameters(); it.hasNext(); )
                {
                    Variable var = it.next();
                    if (DETECT_SPICE_PARAMS && !spiceParams.contains(var.getKey())) continue;
                    if (USE_JAVA_CODE) {
                        if (useCellParams && !spiceParams.contains(null)) continue;
                    } else {
                        if (useCellParams && !var.isJava()) continue;
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
     * Returns true if variable can be netlisted as a spice parameter (unevaluated expression).
     * If not, it should be evaluated before netlisting.
     * @param var the var to check
     * @return true if the variable should be netlisted as a spice parameter
     */
    private boolean isNetlistableParam(Variable var)
    {
        if (USE_JAVA_CODE)
        {
            CodeExpression ce = var.getCodeExpression();
            if (ce == null) return true;
            if (ce.getHSpiceText(false) != null) return true;
            return false;
        }
        if (var.isJava()) return false;
        return true;
    }

    /**
     * Returns text representation of instance parameter
     * on specified Nodable in specified VarContext
     * The text representation can be either spice parameter expression or
     * a value evaluated in specified VarContext
     * @param context specified VarContext
     * @param no specified Nodable
     * @param instParam instance parameter
     * @param forceEval true to always evaluate param
     * @return true if the variable should be netlisted as a spice parameter
     */
    private String evalParam(VarContext context, Nodable no, Variable instParam, boolean forceEval)
    {
        return evalParam(context, no, instParam, forceEval, false, false);
    }

    /**
     * Returns text representation of instance parameter
     * on specified Nodable in specified VarContext
     * The text representation can be either spice parameter expression or
     * a value evaluated in specified VarContext
     * @param context specified VarContext
     * @param no specified Nodable
     * @param instParam instance parameter
     * @param forceEval true to always evaluate param
     * @param wrapped true if the string is already "wrapped" (with parenthesis) and does not need to have quotes around it.
     * @return true if the variable should be netlisted as a spice parameter
     */
    private String evalParam(VarContext context, Nodable no, Variable instParam, boolean forceEval, boolean inPars, boolean wrapped) {
        assert USE_JAVA_CODE;
        Object obj = null;
        if (!forceEval) {
            CodeExpression ce = instParam.getCodeExpression();
            if (ce != null)
                obj = ce.getHSpiceText(inPars);
        }
        if (obj == null)
            obj = context.evalVar(instParam, no);

        if (obj instanceof Number)
            obj = TextUtils.formatDoublePostFix(((Number)obj).doubleValue());
        return formatParam(String.valueOf(obj), instParam.getUnit(), wrapped);
    }

    private Set<Variable.Key> detectSpiceParams(NodeProto np)
    {
        Set<Variable.Key> params = allSpiceParams.get(np);
        if (params != null) return params;
        if (np instanceof PrimitiveNode) {
            params = getImportantVars((PrimitiveNode)np);
//            if (!params.isEmpty())
//                printlnParams(pn + " depends on:", params);
        } else {
            Cell cell = (Cell)np;
            params = getTemplateVars(cell);
            if (params == null) {
                if (cell.isIcon() && cell.contentsView() != null) {
                    Cell schCell = cell.contentsView();
                    params = detectSpiceParams(schCell);
//                    printlnParams(cell + " inherits from " + schCell, params);
                } else {
                    params = new HashSet<Variable.Key>();
                    boolean uniquify = false;
                    for (Iterator<NodeInst> nit = cell.getNodes(); nit.hasNext();) {
                        NodeInst ni = nit.next();
                        if (ni.isIconOfParent()) continue;
                        Set<Variable.Key> protoParams = detectSpiceParams(ni.getProto());
                        if (protoParams == UNIQUIFY_MARK)
                            uniquify = true;
                        for (Variable.Key protoParam: protoParams) {
                            Variable var = ni.getParameterOrVariable(protoParam);
                            if (var == null) continue;
                            CodeExpression ce = var.getCodeExpression();
                            if (ce == null) continue;
                            if (!isNetlistableParam(var)) uniquify = true;
                            Set<Variable.Key> depends = ce.dependsOn();
                            params.addAll(depends);
//                            if (!depends.isEmpty())
//                                printlnParams("\t" + ni + " added", depends);
                        }
                        if (ni.getProto() == Generic.tech().invisiblePinNode) {
                            findVarsInTemplate(ni.getVar(Spice.SPICE_DECLARATION_KEY), cell, false, params);
                            findVarsInTemplate(ni.getVar(Spice.SPICE_CARD_KEY), cell, false, params);
                        }
                    }
                    if (uniquify/* && USE_UNIQUIFY_MARK*/)
                        params = UNIQUIFY_MARK;
//                    printlnParams(cell + " collected params", params);
                }
            }
        }
        allSpiceParams.put(np, params);
        return params;
    }

//  private static void printlnParams(String message, Set<Variable.Key> varKeys) {
//      System.out.print(message);
//      for (Variable.Key varKey: varKeys)
//          System.out.print(" " + varKey);
//      System.out.println();
//  }

    /**
     * Method to tell which Variables are important for primitive node in this netlister
     * @param pn primitive node to tell
     * @return a set of important variables or null if all variables may be important
     */
    protected Set<Variable.Key> getImportantVars(PrimitiveNode pn)
    {
        Set<Variable.Key> importantVars = new HashSet<Variable.Key>();

        // look for a SPICE template on the primitive
        String line = pn.getSpiceTemplate();
        if (line != null) {
            findVarsInLine(line, pn, true, importantVars);
            // Writing MFactor if available. Not sure here
            // No, this causes problems because "ATTR_M" does not actually exist on proto
            //importantVars.add(SimulationTool.M_FACTOR_KEY);
            return importantVars;
        }

        // handle resistors, inductors, capacitors, and diodes
        PrimitiveNode.Function fun = pn.getFunction();
        if (fun.isResistor()) {
            importantVars.add(Schematics.SCHEM_RESISTANCE);
        } else if (fun.isCapacitor()) {
            importantVars.add(Schematics.SCHEM_CAPACITANCE);
        } else if (fun == PrimitiveNode.Function.INDUCT) {
            importantVars.add(Schematics.SCHEM_INDUCTANCE);
        } else if (fun == PrimitiveNode.Function.DIODE || fun == PrimitiveNode.Function.DIODEZ) {
            importantVars.add(Schematics.SCHEM_DIODE);
        } else if (pn.getGroupFunction() == PrimitiveNode.Function.TRANS) {
            // model information
            importantVars.add(SPICE_MODEL_KEY);

            // compute length and width (or area for nonMOS transistors)
            if (pn.getTechnology() instanceof Schematics) {
                importantVars.add(Schematics.ATTR_LENGTH);
                importantVars.add(Schematics.ATTR_WIDTH);
                importantVars.add(Schematics.ATTR_AREA);
            } else {
                importantVars.add(NodeInst.TRACE);
            }
            importantVars.add(SimulationTool.M_FACTOR_KEY);
        } else if (pn == Generic.tech().invisiblePinNode) {
            importantVars.add(Spice.SPICE_DECLARATION_KEY);
            importantVars.add(Spice.SPICE_CARD_KEY);
        }
        return importantVars;
    }

    /****************************** TEMPLATE WRITING ******************************/

	private void emitEmbeddedSpice(Variable cardVar, VarContext context, SpiceSegmentedNets segNets, HierarchyEnumerator.CellInfo info, boolean flatNetNames, boolean forceEval)
	{
		Object obj = cardVar.getObject();
		if (!(obj instanceof String) && !(obj instanceof String[])) return;
		if (!cardVar.isDisplay()) return;
		if (obj instanceof String)
		{
	        StringBuffer buf = replacePortsAndVars((String)obj, context.getNodable(), context.pop(), null, segNets, info, flatNetNames, forceEval);
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
	            StringBuffer buf = replacePortsAndVars(strings[i], context.getNodable(), context.pop(), null, segNets, info, flatNetNames, forceEval);
				buf.append('\n');
				String msg = buf.toString();
				boolean isComment = false;
				if (msg.startsWith("*")) isComment = true;
				multiLinePrint(isComment, msg);
	        }
		}
	}

    /**
     * Method to determine which Spice engine is being targeted and to get the proper
     * template variable.
     * Looks in associated icon/schematics cells to find it.
     * @param cell the Cell being netlisted.
     * @return a Variable with the proper Spice template (null if none).
     */
    private Variable getEngineTemplate(Cell cell)
    {
        Variable varTemplate = getEngineTemplateJustThis(cell);

        // try associated icon/schematic if no template found
        if (varTemplate == null)
        {
        	if (cell.isIcon())
        	{
        		Cell schCell = cell.contentsView();
        		if (schCell != null)
        	        varTemplate = getEngineTemplateJustThis(schCell);
        	} else
        	{
        		Cell iconCell = cell.iconView();
        		if (iconCell != null)
        	        varTemplate = getEngineTemplateJustThis(iconCell);
        	}
        }
        return varTemplate;
    }

    /**
     * Method to determine which Spice engine is being targeted and to get the proper
     * template variable.
     * @param cell the Cell being netlisted.
     * @return a Variable with the proper Spice template (null if none).
     */
    private Variable getEngineTemplateJustThis(Cell cell)
    {
        Variable varTemplate = cell.getVar(preferedEngineTemplateKey);

        // If not looking for the default spice templates, and special one is not found, try default
        if (varTemplate == null && preferedEngineTemplateKey != SPICE_TEMPLATE_KEY)
            varTemplate = cell.getVar(SPICE_TEMPLATE_KEY);
        return varTemplate;
    }

    private Set<Variable.Key> getTemplateVars(Cell cell)
    {
        Variable varTemplate = getEngineTemplate(cell);
        if (varTemplate == null) return null;
        Set<Variable.Key> depends = new HashSet<Variable.Key>();
        findVarsInTemplate(varTemplate, cell, true, depends);
        return depends;
    }

    private void findVarsInTemplate(Variable varTemplate, NodeProto cell, boolean isPortReplacement, Set<Variable.Key> vars)
    {
        if (varTemplate == null) return;
        Object value = varTemplate.getObject();
        if (value instanceof Object[])
        {
            for (Object o : ((Object[]) value))
            {
                findVarsInLine(o.toString(), cell, isPortReplacement, vars);
            }
        } else
        {
            findVarsInLine(value.toString(), cell, isPortReplacement, vars);
        }
    }

    /**
     * Replace ports and vars in 'line'.  Ports and Vars should be
     * referenced via $(name)
     * @param line the string to search and replace within
     * @param no the nodable up the hierarchy that has the parameters on it
     * @param context the context of the nodable
     * @param cni the cell net info of cell in which the nodable exists (if cni is
     * null, no port name replacement will be done)
     * @param segNets
     * @param info
     * @param flatNetNames
     * @param forceEval always evaluate parameters to numbers
     * @return the modified line
     */
    private StringBuffer replacePortsAndVars(String line, Nodable no, VarContext context,
                                       CellNetInfo cni, SpiceSegmentedNets segNets,
                                       HierarchyEnumerator.CellInfo info, boolean flatNetNames, boolean forceEval) {
    	StringBufferQuoteParity infstr = new StringBufferQuoteParity();
        NodeProto prototype = null;
    	PrimitiveNode prim = null;
    	if (no != null)
    	{
    		prototype = no.getProto();
    		if (prototype instanceof PrimitiveNode) prim = (PrimitiveNode)prototype;
    	}

        for(int pt = 0; pt < line.length(); pt++)
        {
        	// see if the character is part of a substitution expression
            char chr = line.charAt(pt);
            if (chr != '$' || pt+1 >= line.length() || line.charAt(pt+1) != '(')
            {
            	// not part of substitution: just emit it
                infstr.append(chr);
                continue;
            }

            // may be part of substitution expression: look for closing parenthesis
            int start = pt + 2;
            for(pt = start; pt < line.length(); pt++)
                if (line.charAt(pt) == ')') break;

            // do the parameter substitution
            String paramName = line.substring(start, pt);

            PortProto pp = null;
            int ppIndex = -1;
            if (prototype != null)
            {
            	if (prototype instanceof Cell)
            	{
            		Cell subCell = (Cell)prototype;
            		Netlist nl = subCell.getNetlist();
            		for(Iterator<Export> it = subCell.getExports(); it.hasNext(); )
            		{
            			Export e = it.next();
            			int width = nl.getBusWidth(e);
            			for(int i=0; i<width; i++)
            			{
            				Network net = nl.getNetwork(e, i);
                			for(Iterator<String> nIt = net.getNames(); nIt.hasNext(); )
                			{
                				String netName = nIt.next();
                				if (netName.equals(paramName))
                				{
                					pp = e;
                					ppIndex = i;
                					break;
                				}
                			}
                			if (ppIndex >= 0) break;
            			}
            			if (ppIndex >= 0) break;
            		}
            	} else
            	{
            		pp = prototype.findPortProto(paramName);
            	}
            }
            Variable.Key varKey;

            if (paramName.equalsIgnoreCase("node_name") && no != null)
            {
            	String nodeName = getSafeNetName(no.getName(), false);
//            	nodeName = nodeName.replaceAll("[\\[\\]]", "_");
                infstr.append(nodeName);
            } else if (paramName.equalsIgnoreCase("width") && prim != null)
            {
            	NodeInst ni = (NodeInst)no;
                PrimitiveNodeSize npSize = ni.getPrimitiveDependentNodeSize(context);
            	if (npSize != null) infstr.append(npSize.getWidth().toString());
            } else if (paramName.equalsIgnoreCase("length") && prim != null)
            {
            	NodeInst ni = (NodeInst)no;
                PrimitiveNodeSize npSize = ni.getPrimitiveDependentNodeSize(context);
                if (npSize != null) infstr.append(npSize.getLength().toString());
            } else if (cni != null && pp != null)
            {
                // port name found: use its spice node
            	if (ppIndex < 0) ppIndex = 0;
                Network net = cni.getNetList().getNetwork(no, pp, ppIndex);
                CellSignal cs = cni.getCellSignal(net);
                String portName = cs.getName();
                if (segNets != null) {
                    PortInst pi = no.getNodeInst().findPortInstFromProto(pp);
                    portName = segNets.getNetName(pi);
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
                    attrVar = no.getParameterOrVariable(varKey);
                }
                if (attrVar == null) infstr.append("??"); else
                {
                    String pVal = "?";
                    Variable parentVar = attrVar;
                    if (prototype != null && prototype instanceof Cell)
                        parentVar = ((Cell)prototype).getParameterOrVariable(attrVar.getKey());
                    if (USE_JAVA_CODE) {
                        pVal = evalParam(context, no, attrVar, forceEval, true, infstr.inParens());
                        if (infstr.inQuotes()) pVal = trimSingleQuotes(pVal);
                    } else {
                        if (!useCDL && localPrefs.useCellParameters &&
                                parentVar.getCode() == CodeExpression.Code.SPICE) {
                            Object obj = context.evalSpice(attrVar, false);
                            if (obj != null)
                                pVal = obj.toString();
                        } else {
                            pVal = String.valueOf(context.evalVar(attrVar, no));
                        }
//                    if (attrVar.getCode() != TextDescriptor.Code.NONE)
                        if (infstr.inQuotes()) pVal = trimSingleQuotes(pVal); else
                            pVal = formatParam(pVal, attrVar.getUnit(), infstr.inParens());
                    }
                    infstr.append(pVal);
                }
            } else {
                // look for the network name
                boolean found = false;
                String hierName = null;
                String [] names = paramName.split("\\.");
                if (names.length > 1 && flatNetNames) {
                    // hierarchical name, down hierarchy
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
                    System.out.println("Cannot find parameter $("+paramName+") in cell "+
                    	(prototype != null ? prototype.describe(false) : context.getInstPath(".")));
                }
            }
        }
        return infstr.getStringBuffer();
    }

    private static class StringBufferQuoteParity
    {
    	private StringBuffer sb = new StringBuffer();
    	private int quoteCount;
    	private int parenDepth;

    	void append(String str)
    	{
    		sb.append(str);
    		for(int i=0; i<str.length(); i++)
    		{
    			char ch = str.charAt(i);
    			if (ch == '\'') quoteCount++; else
    			if (ch == '(') parenDepth++; else
    			if (ch == ')') parenDepth--;
    		}
    	}

    	void append(char c)
    	{
    		sb.append(c);
    		if (c == '\'') quoteCount++; else
			if (c == '(') parenDepth++; else
			if (c == ')') parenDepth--;
    	}

    	boolean inQuotes() { return (quoteCount&1) != 0; }

    	boolean inParens() { return parenDepth > 0; }

    	StringBuffer getStringBuffer() { return sb; }
    }

    /**
     * Find vars in 'line'.  Ports and Vars should be referenced via $(name)
     * @param line the string to search and replace within
     * @param prototype of instance that has the parameters on it
     * @param isPortReplacement true if port name replacement will be done)
     */
    private void findVarsInLine(String line, NodeProto prototype, boolean isPortReplacement, Set<Variable.Key> vars) {
    	PrimitiveNode prim = null;
    	if (prototype != null) {
    		if (prototype instanceof PrimitiveNode) prim = (PrimitiveNode)prototype;
    	}

        for(int pt = 0; pt < line.length(); pt++) {
        	// see if the character is part of a substitution expression
            char chr = line.charAt(pt);
            if (chr != '$' || pt+1 >= line.length() || line.charAt(pt+1) != '(') {
            	// not part of substitution: just emit it
                continue;
            }

            // may be part of substitution expression: look for closing parenthesis
            int start = pt + 2;
            for(pt = start; pt < line.length(); pt++)
                if (line.charAt(pt) == ')') break;

            // do the parameter substitution
            String paramName = line.substring(start, pt);

            PortProto pp = null;
            if (prototype != null) {
                pp = prototype.findPortProto(paramName);
            }
            Variable.Key varKey;

            if (paramName.equalsIgnoreCase("node_name") && prototype != null) continue;
            if (paramName.equalsIgnoreCase("width") && prim != null) continue;
            if (paramName.equalsIgnoreCase("length") && prim != null) continue;
            if (isPortReplacement && pp != null) continue;
            if (prototype != null && (varKey = Variable.findKey("ATTR_" + paramName)) != null) {
                if (prototype instanceof PrimitiveNode || ((Cell) prototype).getParameterOrVariable(varKey) != null)
                    vars.add(varKey);
            }
        }
    }

    private Network findNet(Netlist netlist, String netName)
    {
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
    private Global getGlobal(Network net)
    {
        Netlist netlist = net.getNetlist();
        for (int i=0; i<netlist.getGlobals().size(); i++) {
            Global g = netlist.getGlobals().get(i);
            if (netlist.getNetwork(g) == net)
                return g;
        }
        return null;
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
		if (spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_2 ||
			spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_P ||
			spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_G) return "0";

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

	/** Abstract method to decide whether aggregate names (busses) can have gaps in their ranges. */
	protected boolean isAggregateNameGapsSupported() { return false; }

	/** Method to report whether input and output names are separated. */
	protected boolean isSeparateInputAndOutput() { return false; }

	/** Abstract method to decide whether netlister is case-sensitive (Verilog) or not (Spice). */
	protected boolean isCaseSensitive() { return false; }

    /**
	 * Method to adjust a network name to be safe for Spice output.
	 * Spice has a list of legal punctuation characters that it allows.
	 */
	protected String getSafeNetName(String name, boolean bus)
	{
        return getSafeNetName(name, bus, legalSpiceChars, spiceEngine);
    }

    /**
     * Method called during hierarchy traversal.
     * Called at the end of the enter cell phase.
     */
    protected void enterCell(HierarchyEnumerator.CellInfo info)
    {
        if (exemptedNets != null)
            exemptedNets.setExemptedNets(info);
    }

    /** Method to report that not to choose best export name among exports connected to signal. */
    protected boolean isChooseBestExportName() { return false; }

    /** If the netlister has requirments not to netlist certain cells and their
     * subcells, override this method.
     * If this cell has a spice template, skip it
     */
    protected boolean skipCellAndSubcells(Cell cell)
	{
		// skip if there is a template
        Variable varTemplate = null;
        varTemplate = getEngineTemplate(cell);
        if (varTemplate != null) return true;

		// look for a model file for the current cell, can come from pref or on cell
        String fileName = null;
        String unfilteredFileName = localPrefs.modelFiles.get(cell);
        if (CellModelPrefs.isUseModelFromFile(unfilteredFileName)) {
            fileName = CellModelPrefs.getModelFile(unfilteredFileName);
        }
        varTemplate = cell.getVar(SPICE_NETLIST_FILE_KEY);
        if (varTemplate != null) {
            Object obj = varTemplate.getObject();
            if (obj instanceof String) {
                String str = (String)obj;
                if (!str.equals("") && !str.equals("*Undefined"))
                    fileName = str;
            }
        }
        if (fileName != null && !localPrefs.ignoreModelFiles) {
            if (!modelOverrides.containsKey(cell))
            {
                String absFileName = fileName;
                if (!fileName.startsWith("/") && !fileName.startsWith("\\")) {
                    File spiceFile = new File(filePath);
                    absFileName = (new File(spiceFile.getParent(), fileName)).getPath();
                }
                boolean alreadyIncluded = false;
                for (String includeFile : modelOverrides.values()) {
                    if (absFileName.equals(includeFile))
                        alreadyIncluded = true;
                }
                if (alreadyIncluded) {
                    multiLinePrint(true, "\n* " + cell + " is described in this file:\n");
                    multiLinePrint(true, "* "+fileName+" (already included) \n");
                } else {
                    multiLinePrint(true, "\n* " + cell + " is described in this file:\n");
                    addIncludeFile(fileName);
                }
                modelOverrides.put(cell, absFileName);
            }
            return true;
        }

        return false;
    }

	/**
	 * Since the Spice netlister should write a separate copy of schematic cells for each icon,
	 * this override returns true.
	 */
	protected boolean isWriteCopyForEachIcon() { return false; }

    /**
     * Method called when a cell is skipped.
     * Performs any checking to validate that no error occurs.
     */
    protected void validateSkippedCell(HierarchyEnumerator.CellInfo info) {
        String fileName = modelOverrides.get(info.getCell());
        if (fileName != null) {
            // validate included file
            SpiceNetlistReader reader = new SpiceNetlistReader();
            try {
                reader.readFile(fileName, false);
                HierarchyEnumerator.CellInfo parentInfo = info.getParentInfo();
                Nodable no = info.getParentInst();
                String parameterizedName = info.getCell().getName();
                if (no != null && parentInfo != null) {
                    parameterizedName = parameterizedName(no, parentInfo.getContext());
                }
                CellNetInfo cni = getCellNetInfo(parameterizedName);
                SpiceSubckt subckt = reader.getSubckt(parameterizedName);
                if (cni != null && subckt != null) {
                    if (subckt == null) {
                        reportError("Error: No subckt for "+parameterizedName+" found in included file: "+fileName);
                    } else {
                        List<String> signals = new ArrayList<String>();
                        for (Iterator<CellSignal> sIt = cni.getCellSignals(); sIt.hasNext(); ) {
                            CellSignal cs = sIt.next();
                            if (ignoreSubcktPort(cs)) continue;
                            signals.add(cs.getName());
                        }
                        List<String> subcktSignals = subckt.getPorts();
                        if (signals.size() != subcktSignals.size()) {
                            reportWarning("Warning: wrong number of ports for subckt "+
                                    parameterizedName+": expected "+signals.size()+", but found "+
                                    subcktSignals.size()+", in included file "+fileName);
                        }
                        int len = Math.min(signals.size(), subcktSignals.size());
                        for (int i=0; i<len; i++) {
                            String s1 = signals.get(i);
                            String s2 = subcktSignals.get(i);
                            if (!s1.equalsIgnoreCase(s2)) {
                                reportWarning("Warning: port "+i+" of subckt "+parameterizedName+
                                        " is named "+s1+" in Electric, but "+s2+" in included file "+fileName);
                            }
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                reportError("Error validating included file for cell "+info.getCell().describe(true)+": "+e.getMessage());
            }
        }
    }

    /** Tell the Hierarchy enumerator how to short resistors */
    @Override
    protected Netlist.ShortResistors getShortResistors() {
    	// should use the value of localPrefs.shortResistors
//    	switch (localPrefs.shortResistors)
//    	{
//    		case 0: return Netlist.ShortResistors.NO;
//    		case 1: return Netlist.ShortResistors.PARASITIC;
//    		case 2: return Netlist.ShortResistors.ALL;
//    	}

    	if (useCDL && localPrefs.cdlIgnoreResistors)
            return Netlist.ShortResistors.PARASITIC;
        // this option is used for writing spice netlists for LVS and RCX (it always returns FALSE)
        if (localPrefs.ignoreParasiticResistors)
            return Netlist.ShortResistors.PARASITIC;
        return Netlist.ShortResistors.NO;
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
        return CellModelPrefs.isUseLayoutView(localPrefs.modelFiles.get(cell));
    }

    private Netlist.ShortResistors getShortResistorsFlat() {
        return Netlist.ShortResistors.ALL;
    }

	/******************** DECK GENERATION SUPPORT ********************/

    /**
     * Method to write M factor information into a given string buffer
     * @param context the context of the nodable for finding M-factors farther up the hierarchy.
     * @param no Nodable representing the node
     * @param infstr Buffer where to write to
     */
    private void writeMFactor(VarContext context, Nodable no, StringBuffer infstr)
    {
        Variable mVar = no.getVar(SimulationTool.M_FACTOR_KEY);
        if (mVar == null) return;
        Object value = context.evalVar(mVar);

        // check for M=@M, and warn user that this is a bad idea, and we will not write it out
        if (mVar.getObject().toString().equals("@M") || (mVar.getObject().toString().equals("P(\"M\")")))
        {
            reportWarning("Warning: M=@M [eval=" + value + "] on " + no.getName() +
            	" is a bad idea, not writing it out: " + context.push(no).getInstPath("."));
            return;
        }

        infstr.append(" M=" + formatParam(value.toString(), TextDescriptor.Unit.NONE, false));
    }

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
		if (localPrefs.includeDateAndVersionInOutput)
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
        boolean useParasitics = !useCDL &&
        	localPrefs.parasiticsLevel != SimulationTool.SpiceParasitics.SIMPLE &&
            cell.getView() == View.LAYOUT;
        if (useParasitics) {
            for (Layer layer : layoutTechnology.getLayersSortedByHeight()) {
                if (layer.isPseudoLayer()) continue;
                double edgecap = layer.getEdgeCapacitance();
                double areacap = layer.getCapacitance();
                double res = layer.getResistance();
                if (edgecap != 0 || areacap != 0 || res != 0) {
                    multiLinePrint(true, "***    "+layer.getName()+":\tareacap="+areacap+"FF/um^2,\tedgecap="+edgecap+"FF/um,\tres="+res+"ohms/sq\n");
                }
            }
        }
        multiLinePrint(false, ".OPTIONS NOMOD NOPAGE\n");

        if (localPrefs.useCellParameters) {
            multiLinePrint(false, ".options parhier=local\n");
        }
        // if sizes to be written in lambda, tell spice conversion factor
		if (localPrefs.writeTransSizeInLambda)
		{
			double scale = layoutTechnology.getScale();
			multiLinePrint(true, "*** Lambda Conversion ***\n");
			multiLinePrint(false, ".opt scale=" + TextUtils.formatDouble(scale / 1000.0) + "U\n\n");
		}

		// see if spice model/option cards from file if specified
		String headerFile = localPrefs.headerCardInfo;
		if (headerFile.length() > 0 && !headerFile.startsWith(SPICE_NOEXTENSION_PREFIX))
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
                reportWarning("Spice Header Card '" + fileName + "' cannot be loaded");
            } else
			{
				// normal header file specified
				File test = new File(headerFile);
				if (!test.exists())
					reportWarning("Warning: cannot find model file '" + headerFile + "'");
				multiLinePrint(true, "* Model cards are described in this file:\n");
				addIncludeFile(headerFile);
				return;
			}
		}

		// no header files: write predefined header for this level and technology
		int level = TextUtils.atoi(localPrefs.level);
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
	}

	/**
	 * Write a trailer from an external file, defined as a variable on
	 * the current technology in this library: tech:~.SIM_spice_trailer_file
	 * if it is available.
	 */
	private void writeTrailer(Cell cell)
	{
		// get spice trailer cards from file if specified
		String trailerFile = localPrefs.trailerCardInfo;
		if (trailerFile.length() > 0 && !trailerFile.startsWith(SPICE_NOEXTENSION_PREFIX))
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
                {
                    reportWarning("Spice Trailer Card '" + fileName + "' cannot be loaded");
                }
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
	private void writeTwoPort(NodeInst ni, String partName, String extra, CellNetInfo cni, Netlist netList,
		VarContext context, SpiceSegmentedNets segmentedNets)
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
			dumpMessage(message, false);
		}
		if (cs0 != null && cs1 != null && cs0 == cs1)
		{
			String message = "WARNING: " + ni + " component appears to be shorted on net " + net0.toString() +
				" in " + ni.getParent();
			dumpMessage(message, false);
			return;
		}

		if (ni.getName() != null) partName += getSafeNetName(ni.getName(), false);

        // add Mfactor if there
        StringBuffer sbExtra = new StringBuffer(extra);
        writeMFactor(context, ni, sbExtra);

        String name0 = cs0.getName();
        String name1 = cs1.getName();
        if (segmentedNets != null) {
            name0 = segmentedNets.getNetName(port0);
            name1 = segmentedNets.getNetName(port1);
        }
		multiLinePrint(false, partName + " " + name1 + " " + name0 + " " + sbExtra.toString() + "\n");
	}

	/******************** SIMPLE PARASITIC CALCULATIONS (AREA/PERIM) ********************/

	/**
	 * Method to recursively determine the area of diffusion and capacitance
	 * associated with port "pp" of nodeinst "ni".  If the node is mult_layer, then
	 * determine the dominant capacitance layer, and add its area; all other
	 * layers will be added as well to the extra_area total.
	 * Continue out of the ports on a complex cell
	 */
	private void addNodeInformation(Netlist netList, Map<Network,SpiceNet> spiceNets, NodeInst ni)
	{
		// cells have no area or capacitance (for now)
		if (ni.isCellInstance()) return;  // No area for complex nodes

		PrimitiveNode.Function function = ni.getFunction();

		// initialize to examine the polygons on this node
		Technology tech = ni.getProto().getTechnology();
		AffineTransform trans = ni.rotateOut();

		// make linked list of polygons
		Poly [] polyList = tech.getShapeOfNode(ni, true, true, null);
		int tot = polyList.length;
		for(int i=0; i<tot; i++)
		{
			Poly poly = polyList[i];

			// make sure this layer connects electrically to the desired port
			PortProto pp = poly.getPort();
			if (pp == null) continue;
			Network net = netList.getNetwork(ni, pp, 0);

			// don't bother with layers without capacity
            if (poly.isPseudoLayer()) continue;
			Layer layer = poly.getLayer();
			if (layer.getTechnology() != Technology.getCurrent()) continue;
			if (!layer.isDiffusionLayer() && layer.getCapacitance() == 0.0) continue;

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
        if (!isDiffArc) {
            // all cap besides diffusion is handled by segmented nets
            return;
        }

        Technology tech = ai.getProto().getTechnology();
		Poly [] arcInstPolyList = tech.getShapeOfArc(ai);
		int tot = arcInstPolyList.length;
		for(int j=0; j<tot; j++)
		{
			Poly poly = arcInstPolyList[j];
			if (poly.getStyle().isText()) continue;

            if (poly.isPseudoLayer()) continue;
			Layer layer = poly.getLayer();
			if (layer.getTechnology() != Technology.getCurrent()) continue;

			if (layer.isDiffusionLayer() ||
				(!isDiffArc && layer.getCapacitance() > 0.0))
					merge.addPolygon(layer, poly);
		}
	}

    /******************** SUPPORT ********************/

	/**
	 * Method to determine whether a signal should be a subcircuit parameter.
	 * @param cs the signal to examine.
	 * @return true to ignore this signal in the subcircuit parameter list.
	 */
    private boolean ignoreSubcktPort(CellSignal cs)
    {
		if (localPrefs.globalTreatment != SimulationTool.SpiceGlobal.USESUBCKTPORTS)
		{
	        // ignore networks that aren't exported
	        PortProto pp = cs.getExport();
            if (pp == null) return true;
		}
        return false;
    }

	/**
     * Finds cells that must be uniquified during netlisting. These are
     * cells that have parameters which are Java Code. Because the Java Code
     * expression can return values that are different for different instances
     * in the hierarchy, the hierarchy must be flattened above that instance.
     * @param cell the cell hierarchy to check
     * @return true if the cell has been marked to unquify (which is true if
     * any cell below it has been marked).
     */
    private boolean markCellsToUniquify(Cell cell)
    {
        if (uniquifyCells.containsKey(cell)) return uniquifyCells.get(cell) != null;
        boolean mark = false;

        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();
            if (ni.isIconOfParent()) continue;

            Set<Variable.Key> varkeys = detectSpiceParams(ni.getProto());
            // check vars on instance; only uniquify cells that contain instances with Java params
            for (Variable.Key key : varkeys) {
                Variable var = ni.getVar(key);
                if (var != null && !isNetlistableParam(var)) mark = true;
            }
            if (!ni.isCellInstance()) continue;
            Cell proto = ((Cell)ni.getProto()).contentsView();
            if (proto == null) proto = (Cell)ni.getProto();
            if (getEngineTemplate(proto) != null) continue;
            if (markCellsToUniquify(proto)) { mark = true; }
        }
        boolean isUnique = detectSpiceParams(cell) == UNIQUIFY_MARK;
        assert mark == isUnique;
        if (mark)
            uniquifyCells.put(cell, cell);
        else
            uniquifyCells.put(cell, null);
        return mark;
    }

    private static final boolean CELLISEMPTYDEBUG = false;
    private Map<Cell,Boolean> checkedCells = new HashMap<Cell,Boolean>();

    public boolean cellIsEmpty(Cell cell)
    {
        Boolean b = checkedCells.get(cell);
        if (b != null) return b.booleanValue();

        boolean empty = true;

        boolean useParasitics = !useCDL &&
            localPrefs.parasiticsLevel != SimulationTool.SpiceParasitics.SIMPLE &&
            cell.getView() == View.LAYOUT;
        if (useParasitics) return false;

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
                }
                empty = false;
                break;
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

            // check for spice code on pins
            if (ni.getVar(SPICE_CARD_KEY) != null) {
                empty = false;
                break;
            }
        }

        // look for a model file on the current cell
        if (modelOverrides.get(cell) != null) {
            empty = false;
        }

        // check for spice template
        if (getEngineTemplate(cell) != null) {
            empty = false;
        }

        // empty
        if (CELLISEMPTYDEBUG && empty) {
            System.out.println(cell+" is empty and contains the following empty cells:");
            for (Cell c : emptyCells)
                System.out.println("   "+c.describe(true));
        }
        checkedCells.put(cell, Boolean.valueOf(empty));
        return empty;
    }

	/******************** TEXT METHODS ********************/

    /**
     * Method to adjust a network name to be safe for Spice output.
     * Spice has a list of legal punctuation characters that it allows.
     */
    public static String getSafeNetName(String name, SimulationTool.SpiceEngine engine)
    {
        String legalSpiceChars = SPICELEGALCHARS;
        if (engine == SimulationTool.SpiceEngine.SPICE_ENGINE_P)
            legalSpiceChars = PSPICELEGALCHARS;
        return getSafeNetName(name, false, legalSpiceChars, engine);
    }

    /**
     * Method to adjust a network name to be safe for Spice output.
     * Spice has a list of legal punctuation characters that it allows.
     */
    private static String getSafeNetName(String name, boolean bus, String legalSpiceChars, SimulationTool.SpiceEngine spiceEngine)
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
			spiceEngine != SimulationTool.SpiceEngine.SPICE_ENGINE_G &&
			spiceEngine != SimulationTool.SpiceEngine.SPICE_ENGINE_P &&
			spiceEngine != SimulationTool.SpiceEngine.SPICE_ENGINE_2) sb.append('_');
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

    /**
     * This adds formatting to a Spice parameter value.  It adds single quotes
     * around the param string if they do not already exist.
     * @param param the string param value (without the name= part).
     * @param wrapped true if the string is already "wrapped" (with parenthesis) and does not need to have quotes around it.
     * @return a param string with single quotes around it
     */
    private String formatParam(String param, TextDescriptor.Unit u, boolean wrapped)
    {
    	// first remove quotes
        String value = trimSingleQuotes(param);

        // see if the result evaluates to a number
		if (TextUtils.isANumberPostFix(value)) return value;

		// if purely numeric, try converting to proper units
        try {
            Double v = Double.valueOf(value);
            if (u == TextDescriptor.Unit.DISTANCE)
            	return TextUtils.formatDoublePostFix(v.doubleValue());
            return value;
        } catch (NumberFormatException e) {}


        // not a number and some Spices like a wrapper, so enclose it
		if (!wrapped)
		{
	        // Spice2 and Spice3 don't need a wrapper
			if (spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_2 ||
				spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_3) return value;
	        if (spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_O)
	        	value = "{" + value + "}"; else
	        		value = "'" + value + "'";
		}
        return value;
    }

    private String trimSingleQuotes(String param)
    {
        if (param.startsWith("'") && param.endsWith("'")) {
            return param.substring(1, param.length()-1);
        }
        return param;
    }

	/**
	 * Method to insert an "include" of file "filename" into the stream "io".
	 */
	private void addIncludeFile(String fileName)
	{
        if (useCDL) {
            multiLinePrint(false, ".include "+ fileName + "\n");
            return;
        }

		if (spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_2 || spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_3 ||
			spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_G || spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_S)
		{
			multiLinePrint(false, ".include " + fileName + "\n");
		} else if (spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_H || spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_H_ASSURA ||
                spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_H_CALIBRE)
		{
			multiLinePrint(false, ".include '" + fileName + "'\n");
		} else if (spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_P)
		{
			multiLinePrint(false, ".INC " + fileName + "\n");
		}
	}

	/**
	 * Method to report an error or warning messagethat is built in the infinite string.
	 * The message is sent to the messages window, errorLogger and also to the SPICE deck "f".
	 */
	private void dumpMessage(String message, boolean isErrorMsg)
	{
		multiLinePrint(true, "*** " + message + "\n");
        if (isErrorMsg)
            reportError(message);
        else
            reportWarning(message);
	}

	/**
	 * Formatted output to file "stream".  All spice output is in upper case.
	 * The buffer can contain no more than 1024 chars including the newlinelastMoveTo
	 * and null characters.
	 * Doesn't return anything.
	 */
	public void multiLinePrint(boolean isComment, String str)
	{
		// convert "@" characters to "_" for Opus
        if (spiceEngine == SimulationTool.SpiceEngine.SPICE_ENGINE_O)
        	str = str.replaceAll("@", "_");

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

	/******************** SUPPORT CLASSES ********************/

	private static class SpiceNet
	{
		/** merged geometry for this network */		PolyMerge merge;
		/** area of diffusion */					double    diffArea;
		/** perimeter of diffusion */				double    diffPerim;
		/** amount of capacitance in non-diff */	float     nonDiffCapacitance;
		/** number of transistors on the net */		int       transistorCount;

		SpiceNet()
		{
            transistorCount = 0;
            diffArea = 0;
            diffPerim = 0;
            nonDiffCapacitance = 0;
            merge = new PolyMerge();
		}
	}

    private static class SpiceFinishedListener implements Exec.FinishedListener
    {
        private SpiceFinishedListener() {}

        public void processFinished(Exec.FinishedEvent e)
        {
            SwingUtilities.invokeLater(new Runnable() { public void run() {
                UserInterface ui = Job.getUserInterface();
                Cell cell = ui.needCurrentCell();
                if (cell == null) return;
            	SimulationData.plotGuessed(cell, null);
            }});
        }
    }

    public static class FlatSpiceCodeVisitor extends HierarchyEnumerator.Visitor {

        private PrintWriter printWriter;
        private PrintWriter spicePrintWriter;
        private String filePath;
        Spice spice; // just used for file writing and formatting
        SpiceSegmentedNets segNets;

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
				if (ni.getProto() != Generic.tech().invisiblePinNode) continue;
                Variable cardVar = ni.getVar(SPICE_CODE_FLAT_KEY);
                if (cardVar != null) {
                    if (printWriter == null) {
                        try {
                            printWriter = new PrintWriter(new BufferedWriter(new FileWriter(filePath)));
                        } catch (IOException e) {
                            spice.reportWarning("Unable to open "+filePath+" for write.");
                            return;
                        }
                        spice.printWriter = printWriter;
                        segNets = new SpiceSegmentedNets(null, false, null, spice.localPrefs);
                    }
                    spice.emitEmbeddedSpice(cardVar, info.getContext(), segNets, info, true, true);
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
