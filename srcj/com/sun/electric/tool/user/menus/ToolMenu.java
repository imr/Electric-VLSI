/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ToolMenu.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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

package com.sun.electric.tool.user.menus;

import static com.sun.electric.tool.user.menus.EMenuItem.SEPARATOR;

import com.sun.electric.database.CellUsage;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.Foundry;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.compaction.Compaction;
import com.sun.electric.tool.drc.AssuraDrcErrors;
import com.sun.electric.tool.drc.CalibreDrcErrors;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.erc.ERCAntenna;
import com.sun.electric.tool.erc.ERCWellCheck;
import com.sun.electric.tool.extract.Connectivity;
import com.sun.electric.tool.extract.LayerCoverageTool;
import com.sun.electric.tool.extract.ParasiticTool;
import com.sun.electric.tool.generator.PadGenerator;
import com.sun.electric.tool.generator.ROMGenerator;
import com.sun.electric.tool.generator.cmosPLA.PLA;
import com.sun.electric.tool.generator.layout.GateLayoutGenerator;
import com.sun.electric.tool.generator.layout.Tech;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.input.Simulate;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.io.output.Verilog;
import com.sun.electric.tool.logicaleffort.LENetlister;
import com.sun.electric.tool.logicaleffort.LETool;
import com.sun.electric.tool.ncc.AllSchemNamesToLay;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccCrossProbing;
import com.sun.electric.tool.ncc.NccJob;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.Pie;
import com.sun.electric.tool.ncc.SchemNamesToLay;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.result.NccResult;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.ncc.result.equivalence.Equivalence;
import com.sun.electric.tool.routing.AutoStitch;
import com.sun.electric.tool.routing.Maze;
import com.sun.electric.tool.routing.MimicStitch;
import com.sun.electric.tool.routing.River;
import com.sun.electric.tool.routing.Routing;
import com.sun.electric.tool.sc.GetNetlist;
import com.sun.electric.tool.sc.Maker;
import com.sun.electric.tool.sc.Place;
import com.sun.electric.tool.sc.Route;
import com.sun.electric.tool.sc.SilComp;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.CompileVHDL;
import com.sun.electric.tool.user.GenerateVHDL;
import com.sun.electric.tool.user.Highlight2;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.FastHenryArc;
import com.sun.electric.tool.user.dialogs.FillGenDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ncc.HighlightEquivalent;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TextWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;


/**
 * Class to handle the commands in the "Tool" pulldown menu.
 */
public class ToolMenu {

    static EMenu makeMenu() {
		/****************************** THE TOOL MENU ******************************/

		// mnemonic keys available:  B   F H JK    PQ      XYZ
		return new EMenu("_Tool",

		//------------------- DRC

		// mnemonic keys available:  B  EFG IJK MNOPQR TUVWXYZ
            new EMenu("_DRC",
		        new EMenuItem("Check _Hierarchically", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)) { public void run() {
                    DRC.checkDRCHierarchically(Job.getUserInterface().needCurrentCell(), null, GeometryHandler.GHMode.ALGO_SWEEP); }},
		        new EMenuItem("Check _Selection Area Hierarchically") { public void run() {
                    EditWindow_ wnd = Job.getUserInterface().getCurrentEditWindow_();
                    if (wnd == null) return;
                    DRC.checkDRCHierarchically(wnd.getCell(), wnd.getHighlightedArea(), GeometryHandler.GHMode.ALGO_SWEEP); }},
                new EMenuItem("Check Area _Coverage") { public void run() {
                    LayerCoverageTool.layerCoverageCommand(WindowFrame.needCurCell(), GeometryHandler.GHMode.ALGO_SWEEP, true); }},
                new EMenuItem("_List Layer Coverage on Cell") { public void run() {
                    layerCoverageCommand(LayerCoverageTool.LCMode.AREA, GeometryHandler.GHMode.ALGO_SWEEP); }},

                SEPARATOR,
		        new EMenuItem("Import _Assura DRC Errors for Current Cell...") { public void run() {
                    importAssuraDrcErrors(); }},
                new EMenuItem("Import Calibre _DRC Errors for Current Cell...") { public void run() {
                    importCalibreDrcErrors(); }},

                SEPARATOR,
		        new EMenuItem("Export DRC Deck...") { public void run() {
                    exportDRCDeck(); }},
                new EMenuItem("Import DRC Deck...") { public void run() {
                    importDRCDeck(); }}),

		//------------------- Simulation (Built-in)

		// mnemonic keys available:  B   F   JK  N PQ      XYZ
            new EMenu("Simulation (Built-in)",
                Simulation.hasIRSIM() ? new EMenuItem("IRSI_M: Simulate Current Cell") { public void run() {
				    Simulation.startSimulation(Simulation.IRSIM_ENGINE, false, null, null); }} : null,
			    Simulation.hasIRSIM() ? new EMenuItem("IRSIM: _Write Deck...") { public void run() {
				    FileMenu.exportCommand(FileType.IRSIM, true); }} : null,
			    Simulation.hasIRSIM() ? new EMenuItem("_IRSIM: Simulate Deck...") { public void run() {
				    Simulation.startSimulation(Simulation.IRSIM_ENGINE, true, null, null); }} : null,

                Simulation.hasIRSIM() ? SEPARATOR : null,

		        new EMenuItem("_ALS: Simulate Current Cell") { public void run() {
                    Simulation.startSimulation(Simulation.ALS_ENGINE, false, null, null); }},

                SEPARATOR,

		        new EMenuItem("Set Signal _High at Main Time", KeyStroke.getKeyStroke('V', 0)) { public void run() {
                    Simulation.setSignalHigh(); }},
		        new EMenuItem("Set Signal _Low at Main Time", KeyStroke.getKeyStroke('G', 0)) { public void run() {
                    Simulation.setSignalLow(); }},
		        new EMenuItem("Set Signal Un_defined at Main Time", KeyStroke.getKeyStroke('X', 0)) { public void run() {
                    Simulation.setSignalX(); }},
		        new EMenuItem("Set Clock on Selected Signal...") { public void run() {
                    Simulation.setClock(); }},

                SEPARATOR,

		        new EMenuItem("_Update Simulation Window") { public void run() {
				    Simulation.update(); }},
		        new EMenuItem("_Get Information about Selected Signals") { public void run() {
                    Simulation.showSignalInfo(); }},

                SEPARATOR,

		        new EMenuItem("_Clear Selected Stimuli") { public void run() {
                    Simulation.removeSelectedStimuli(); }},
		        new EMenuItem("Clear All Stimuli _on Selected Signals") { public void run() {
			    Simulation.removeStimuliFromSignal(); }},
		        new EMenuItem("Clear All S_timuli") { public void run() {
			    Simulation.removeAllStimuli(); }},

                SEPARATOR,

		        new EMenuItem("_Save Stimuli to Disk...") { public void run() {
                    Simulation.saveStimuli(); }},
		        new EMenuItem("_Restore Stimuli from Disk...") { public void run() {
                    Simulation.restoreStimuli(); }}),

		//------------------- Simulation (SPICE)

		// mnemonic keys available: AB  E   IJK  NO QR   VWXYZ
            new EMenu("Simulation (_Spice)",
		        new EMenuItem("Write Spice _Deck...") { public void run() {
                    FileMenu.exportCommand(FileType.SPICE, true); }},
		        new EMenuItem("Write _CDL Deck...") { public void run() {
                    FileMenu.exportCommand(FileType.CDL, true); }},
		        new EMenuItem("Plot Spice _Listing...") { public void run() {
                    Simulate.plotSpiceResults(); }},
		        new EMenuItem("Plot Spice _for This Cell") { public void run() {
                    Simulate.plotSpiceResultsThisCell(); }},
		        new EMenuItem("Set Spice _Model...") { public void run() {
                    Simulation.setSpiceModel(); }},
		        new EMenuItem("Add M_ultiplier") { public void run() {
                    addMultiplierCommand(); }},
                Job.getDebug() ? new EMenuItem("Add flat code") { public void run() {
                    makeTemplate(Spice.SPICE_CODE_FLAT_KEY); }} : null,

                SEPARATOR,

		        new EMenuItem("Set Generic Spice _Template") { public void run() {
                    makeTemplate(Spice.SPICE_TEMPLATE_KEY); }},
		        new EMenuItem("Set Spice _2 Template") { public void run() {
                    makeTemplate(Spice.SPICE_2_TEMPLATE_KEY); }},
		        new EMenuItem("Set Spice _3 Template") { public void run() {
                    makeTemplate(Spice.SPICE_3_TEMPLATE_KEY); }},
		        new EMenuItem("Set _HSpice Template") { public void run() {
                    makeTemplate(Spice.SPICE_H_TEMPLATE_KEY); }},
		        new EMenuItem("Set _PSpice Template") { public void run() {
                    makeTemplate(Spice.SPICE_P_TEMPLATE_KEY); }},
		        new EMenuItem("Set _GnuCap Template") { public void run() {
                    makeTemplate(Spice.SPICE_GC_TEMPLATE_KEY); }},
		        new EMenuItem("Set _SmartSpice Template") { public void run() {
                    makeTemplate(Spice.SPICE_SM_TEMPLATE_KEY); }}),

		//------------------- Simulation (Verilog)

		// mnemonic keys available: AB  EFGHIJKLMNOPQRS U WXYZ
            new EMenu("Simulation (_Verilog)",
		        new EMenuItem("Write _Verilog Deck...") { public void run() {
                    FileMenu.exportCommand(FileType.VERILOG, true); }},
		        new EMenuItem("Plot Verilog VCD _Dump...") { public void run() {
                    Simulate.plotVerilogResults(); }},
		        new EMenuItem("Plot Verilog for This _Cell") { public void run() {
                    Simulate.plotVerilogResultsThisCell(); }},
                SEPARATOR,
		        new EMenuItem("Set Verilog _Template") { public void run() {
                    makeTemplate(Verilog.VERILOG_TEMPLATE_KEY); }},

                SEPARATOR,

		// mnemonic keys available: ABC EFGHIJKLMNOPQRS UV XYZ
                new EMenu("Set Verilog _Wire",
		        	new EMenuItem("_Wire") { public void run() {
                        Simulation.setVerilogWireCommand(0); }},
		        	new EMenuItem("_Trireg") { public void run() {
                        Simulation.setVerilogWireCommand(1); }},
		        	new EMenuItem("_Default") { public void run() {
                        Simulation.setVerilogWireCommand(2); }}),

		// mnemonic keys available: ABCDEFGHIJKLM OPQRSTUV XYZ
                new EMenu("_Transistor Strength",
		        	new EMenuItem("_Weak") { public void run() {
                        Simulation.setTransistorStrengthCommand(true); }},
		        	new EMenuItem("_Normal") { public void run() {
                        Simulation.setTransistorStrengthCommand(false); }})),

		//------------------- Simulation (others)

		// mnemonic keys available:  B D  G   KL N  Q   UVWXYZ
            new EMenu("Simulation (_Others)",
		        new EMenuItem("Write _Maxwell Deck...") { public void run() {
                    FileMenu.exportCommand(FileType.MAXWELL, true); }},
		        new EMenuItem("Write _Tegas Deck...") { public void run() {
                    FileMenu.exportCommand(FileType.TEGAS, true); }},
		        new EMenuItem("Write _SILOS Deck...") { public void run() {
                    FileMenu.exportCommand(FileType.SILOS, true); }},
		        new EMenuItem("Write _PAL Deck...") { public void run() {
                    FileMenu.exportCommand(FileType.PAL, true); }},
                SEPARATOR,
                !Simulation.hasIRSIM() ? new EMenuItem("Write _IRSIM Deck...") { public void run() {
				    FileMenu.exportCommand(FileType.IRSIM, true); }} : null,
		        new EMenuItem("Write _ESIM/RNL Deck...") { public void run() {
                    FileMenu.exportCommand(FileType.ESIM, true); }},
		        new EMenuItem("Write _RSIM Deck...") { public void run() {
                    FileMenu.exportCommand(FileType.RSIM, true); }},
		        new EMenuItem("Write _COSMOS Deck...") { public void run() {
                    FileMenu.exportCommand(FileType.COSMOS, true); }},
		        new EMenuItem("Write M_OSSIM Deck...") { public void run() {
                    FileMenu.exportCommand(FileType.MOSSIM, true); }},
                SEPARATOR,
		        new EMenuItem("Write _FastHenry Deck...") { public void run() {
                    FileMenu.exportCommand(FileType.FASTHENRY, true); }},
		        new EMenuItem("Fast_Henry Arc Properties...") { public void run() {
                    FastHenryArc.showFastHenryArcDialog(); }},
                SEPARATOR,
		        new EMenuItem("Write _ArchSim Deck...") { public void run() {
                    FileMenu.exportCommand(FileType.ARCHSIM, true); }},
		        new EMenuItem("Display ArchSim _Journal...") { public void run() {
                    Simulate.plotArchSimResults(); }}),

		//------------------- ERC

		// mnemonic keys available:  BCDEFGHIJKLMNOPQRSTUV XYZ
            new EMenu("_ERC",
		        new EMenuItem("Check _Wells") { public void run() {
                    ERCWellCheck.analyzeCurCell(GeometryHandler.GHMode.ALGO_SWEEP); }},
		        new EMenuItem("_Antenna Check") { public void run() {
                    ERCAntenna.doAntennaCheck(); }}),

		// ------------------- NCC
		// mnemonic keys available: AB DEFGHIJKLMNOPQRS UVWXYZ
            new EMenu("_NCC",
		        new EMenuItem("Schematic and Layout Views of Cell in _Current Window") { public void run() {
                    new NccJob(1); }},
		        new EMenuItem("Cells from _Two Windows") { public void run() {
                    new NccJob(2); }},
                SEPARATOR,
   		        new EMenuItem("Copy Schematic _User Names to Layout") { public void run() {
                    new SchemNamesToLay.RenameJob(); }},
       		    new EMenuItem("Copy All Schematic _Names to Layout") { public void run() {
                    new AllSchemNamesToLay.RenameJob(); }},
   		        new EMenuItem("Highlight _Equivalent") { public void run() {
   			        HighlightEquivalent.highlight(); }},
                new EMenuItem("Run NCC for Schematic Cross-Probing") { public void run() {
                    runNccSchematicCrossProbing(); }},

                new EMenu("Add NCC _Annotation to Cell",
		        	new EMenuItem("Exports Connected by Parent _vdd") { public void run() {
                        NccCellAnnotations.makeNCCAnnotation("exportsConnectedByParent vdd /vdd_[0-9]+/"); }},
		        	new EMenuItem("Exports Connected By Parent _gnd") { public void run() {
                        NccCellAnnotations.makeNCCAnnotation("exportsConnectedByParent gnd /gnd_[0-9]+/"); }},
		        	new EMenuItem("_Skip NCC") { public void run() {
                        NccCellAnnotations.makeNCCAnnotation("skipNCC <comment explaining why>"); }},
		        	new EMenuItem("_Not a Subcircuit") { public void run() {
                        NccCellAnnotations.makeNCCAnnotation("notSubcircuit <comment explaining why>"); }},
		        	new EMenuItem("_Flatten Instances") { public void run() {
                        NccCellAnnotations.makeNCCAnnotation("flattenInstances <list of instance names>"); }},
		        	new EMenuItem("_Join Group") { public void run() {
                        NccCellAnnotations.makeNCCAnnotation("joinGroup <cell name>"); }},
		        	new EMenuItem("_Transistor Type") { public void run() {
                        NccCellAnnotations.makeNCCAnnotation("transistorType <typeName>"); }},
		        	new EMenuItem("_Resistor Type") { public void run() {
                        NccCellAnnotations.makeNCCAnnotation("resistorType <typeName>"); }},
       		        new EMenuItem("Force _Part Match") { public void run() {
                            NccCellAnnotations.makeNCCAnnotation("forcePartMatch <Part name shared by schematic and layout>"); }},
    		        new EMenuItem("Force _Wire Match") { public void run() {
                            NccCellAnnotations.makeNCCAnnotation("forceWireMatch <Wire name shared by schematic and layout>"); }},
		        	new EMenuItem("_Black Box") { public void run() {
                        NccCellAnnotations.makeNCCAnnotation("blackBox <comment explaining why>"); }})),

        // ------------------- PIE
            // If Pie package is installed then add menu entries to call it
            Pie.hasPie() ? new EMenu("_PIE",
               new EMenuItem("PIE Schematic and Layout Views of Cell in Current Window") { public void run() {
                    Pie.invokePieNcc(1); }},
               new EMenuItem("Cells from _Two Windows") { public void run() {
                    Pie.invokePieNcc(2); }}) : null,

		//------------------- Network

		// mnemonic keys available:    D F  IJK M O Q S   W YZ
            new EMenu("Net_work",
		        new EMenuItem("Show _Network", 'K') { public void run() {
                    showNetworkCommand(); }},
		        new EMenuItem("_List Networks") { public void run() {
                    listNetworksCommand(); }},
		        new EMenuItem("List _Connections on Network") { public void run() {
                    listConnectionsOnNetworkCommand(); }},
		        new EMenuItem("List _Exports on Network") { public void run() {
                    listExportsOnNetworkCommand(); }},
		        new EMenuItem("List Exports _below Network") { public void run() {
                    listExportsBelowNetworkCommand(); }},
		        new EMenuItem("List _Geometry on Network") { public void run() {
                    listGeometryOnNetworkCommand(GeometryHandler.GHMode.ALGO_SWEEP); }},
		        new EMenuItem("Show _All Networks") { public void run() {
                    showAllNetworksCommand(); }},
                new EMenuItem("List _Total Wire Lengths on All Networks") { public void run() {
                    listGeomsAllNetworksCommand(); }},

                SEPARATOR,

                new EMenuItem("E_xtract Current Cell") { public void run() {
                    Connectivity.extractCurCell(false); }},
                new EMenuItem("Extract Current _Hierarchy") { public void run() {
                    Connectivity.extractCurCell(true); }},

                SEPARATOR,

                new EMenuItem("Show _Power and Ground") { public void run() {
                    showPowerAndGround(); }},
		        new EMenuItem("_Validate Power and Ground") { public void run() {
                    validatePowerAndGround(false); }},
		        new EMenuItem("_Repair Power and Ground") { public void run() {
                    new RepairPowerAndGround(); }},
		        new EMenuItem("Redo Network N_umbering") { public void run() {
                    NetworkTool.renumberNetlists(); }}),

		//------------------- Logical Effort

		// mnemonic keys available:    DEFGHIJK M   QRSTUVWXYZ
            new EMenu("_Logical Effort",
		        new EMenuItem("_Optimize for Equal Gate Delays") { public void run() {
                    optimizeEqualGateDelaysCommand(true); }},
		        new EMenuItem("Optimize for Equal Gate Delays (no _caching)") { public void run() {
                    optimizeEqualGateDelaysCommand(false); }},
		        new EMenuItem("_Print Info for Selected Node") { public void run() {
                    printLEInfoCommand(); }},
                new EMenuItem("_Back Annotate Wire Lengths for Current Cell") { public void run() {
                    backAnnotateCommand(); }},
		        new EMenuItem("Clear Sizes on Selected _Node(s)") { public void run() {
                    clearSizesNodableCommand(); }},
		        new EMenuItem("Clear Sizes in _all Libraries") { public void run() {
                    clearSizesCommand(); }},
                SEPARATOR,
		        new EMenuItem("_Load Logical Effort Libraries (Purple, Red, and Orange)") { public void run() {
                    loadLogicalEffortLibraries(); }}),

		//------------------- Routing

		// mnemonic keys available:  B D FG IJK   OPQ    V XY
            new EMenu("_Routing",
                new EMenuItem.CheckBox("Enable _Auto-Stitching") {
                    public boolean isSelected() { return Routing.isAutoStitchOn(); }
                    public void setSelected(boolean b) {
                        Routing.setAutoStitchOn(b);
                        System.out.println("Auto-stitching " + (b ? "enabled" : "disabled"));
                    }
                },
		        new EMenuItem("Auto-_Stitch Now") { public void run() {
                    AutoStitch.autoStitch(false, true); }},
		        new EMenuItem("Auto-Stitch _Highlighted Now", KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0)) { public void run() {
                    AutoStitch.autoStitch(true, true); }},

                SEPARATOR,

                new EMenuItem.CheckBox("Enable _Mimic-Stitching") {
                    public boolean isSelected() { return Routing.isMimicStitchOn(); }
                    public void setSelected(boolean b) {
                        Routing.setMimicStitchOn(b);
                        System.out.println("Mimic-stitching " + (b ? "enabled" : "disabled"));
                    }
                },
		        new EMenuItem("Mimic-Stitch _Now", KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)) { public void run() {
                    MimicStitch.mimicStitch(true); }},
		        new EMenuItem("Mimic S_elected") { public void run() {
                    Routing.getRoutingTool().mimicSelected(); }},

                SEPARATOR,

		        new EMenuItem("Ma_ze Route") { public void run() {
                    Maze.mazeRoute(); }},

                SEPARATOR,

		        new EMenuItem("_River-Route") { public void run() {
                    River.riverRoute(); }},

                Routing.hasSunRouter() ? SEPARATOR : null,
                Routing.hasSunRouter() ? new EMenuItem("Sun _Lava Router") { public void run() {
				    Routing.sunRouteCurrentCell(); }} : null,

                SEPARATOR,

		        new EMenuItem("_Unroute") { public void run() {
                    Routing.unrouteCurrent(); }},
		        new EMenuItem("Get Unrouted _Wire") { public void run() {
                    getUnroutedArcCommand(); }},
		        new EMenuItem("_Copy Routing Topology") { public void run() {
                    Routing.copyRoutingTopology(); }},
		        new EMenuItem("Pas_te Routing Topology") { public void run() {
                    Routing.pasteRoutingTopology(); }}),

		//------------------- Generation

		// mnemonic keys available: AB DE GHIJK  NO Q   UVWXYZ
            new EMenu("_Generation",
		        new EMenuItem("_Coverage Implants Generator") { public void run() {
                    layerCoverageCommand(LayerCoverageTool.LCMode.IMPLANT, GeometryHandler.GHMode.ALGO_SWEEP); }},
		        new EMenuItem("_Pad Frame Generator...") { public void run() {
                    padFrameGeneratorCommand(); }},
		        new EMenuItem("_ROM Generator...") { public void run() {
                    ROMGenerator.generateROM(); }},
		        new EMenuItem("MOSIS CMOS P_LA Generator...") { public void run() {
                    PLA.generate(); }},
                new EMenuItem("_Fill (MoCMOS)...") { public void run() {
                    FillGenDialog.openFillGeneratorDialog(MoCMOS.tech); }},
		        new EMenuItem("Generate gate layouts (_MoCMOS)") { public void run() {
                    new GateLayoutGenerator(Tech.Type.MOCMOS); }},
                Technology.getTSMC90Technology() != null ? new EMenuItem("Generate gate layouts (T_SMC180)") { public void run() {
                    new GateLayoutGenerator(Tech.Type.TSMC180); }} : null,
                Technology.getTSMC90Technology() != null ? new EMenuItem("Generate gate layouts (_TSMC90)") { public void run() {
	                new GateLayoutGenerator(Tech.Type.TSMC90); }} : null),

		//------------------- Silicon Compiler

		// mnemonic keys available: AB DEFGHIJKLM OPQRSTUVWXYZ
            new EMenu("Silicon Co_mpiler",
		        new EMenuItem("_Convert Current Cell to Layout") { public void run() {
                    doSiliconCompilation(WindowFrame.needCurCell(), false); }},
                SEPARATOR,
		        new EMenuItem("Compile VHDL to _Netlist View") { public void run() {
                    compileVHDL(); }}),

		//------------------- Compaction

		// mnemonic keys available: AB DEFGHIJKLMNOPQRSTUVWXYZ
            new EMenu("_Compaction",
		        new EMenuItem("Do _Compaction") { public void run() {
                    Compaction.compactNow(); }}),

        //------------------- Others

            SEPARATOR,

            new EMenuItem("List _Tools") { public void run() {
			    listToolsCommand(); }},

		// mnemonic keys available: ABCDEFGHIJKLMNOPQ STUVWXYZ
            new EMenu("Lang_uages",
		        new EMenuItem("_Run Java Bean Shell Script") { public void run() {
                    javaBshScriptCommand(); }}));
	}

	// ---------------------------- Tool Menu Commands ----------------------------

	// Logical Effort Tool
	public static void optimizeEqualGateDelaysCommand(boolean newAlg)
	{
		EditWindow curEdit = EditWindow.needCurrent();
		if (curEdit == null) return;
		LETool letool = LETool.getLETool();
		if (letool == null) {
			System.out.println("Logical Effort tool not found");
			return;
		}
		// set current cell to use global context
		curEdit.setCell(curEdit.getCell(), VarContext.globalContext, null);

		// optimize cell for equal gate delays
        if (curEdit.getCell() == null) {
            System.out.println("No current cell");
            return;
        }
		letool.optimizeEqualGateDelays(curEdit.getCell(), curEdit.getVarContext(), newAlg);
	}

	/** Print Logical Effort info for highlighted nodes */
	public static void printLEInfoCommand() {
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
		Highlighter highlighter = wnd.getHighlighter();
		VarContext context = wnd.getVarContext();

		if (highlighter.getNumHighlights() == 0) {
			System.out.println("Nothing highlighted");
			return;
		}
		for (Highlight2 h : highlighter.getHighlights()) {
			if (!h.isHighlightEOBJ()) continue;

			ElectricObject eobj = h.getElectricObject();
			if (eobj instanceof PortInst) {
				PortInst pi = (PortInst)eobj;
				pi.getInfo();
				eobj = pi.getNodeInst();
			}
			if (eobj instanceof NodeInst) {
				NodeInst ni = (NodeInst)eobj;
				LETool.printResults(ni, context);
			}
		}

	}

    public static void backAnnotateCommand() {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Cell cell = wnd.getCell();
        if (cell == null) return;

        BackAnnotateJob job = new BackAnnotateJob(cell);
        job.startJob();
    }

    /**
     * Method to handle the "List Layer Coverage", "Coverage Implant Generator",  polygons merge
     * except "List Geometry on Network" commands.
     */
    public static void layerCoverageCommand(LayerCoverageTool.LCMode func, GeometryHandler.GHMode mode)
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;
        LayerCoverageTool.layerCoverageCommand(func, mode, curCell, true);
    }

    private static class BackAnnotateJob extends Job {
        private Cell cell;

    	public BackAnnotateJob(Cell cell) {
            super("BackAnnotate", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cell = cell;
        }

        public boolean doIt() throws JobException {
            Cell[] schLayCells = NccUtils.findSchematicAndLayout(cell);
            if (schLayCells == null) {
                System.out.println("Could not find schematic and layout cells for "+cell.describe(true));
                return false;
            }
            if (cell.getView() == View.LAYOUT) {
                // check if layout cells match, if not, replace
                schLayCells[1] = cell;
            }

            // run NCC, get results
            NccOptions options = new NccOptions();
            NccResults results = Ncc.compare(schLayCells[0], null, schLayCells[1], null, options, this);
            // get result of comparison of top schematic and layout Cells
            NccResult result = results.getResultFromRootCells();
            if (!result.match()) {
                System.out.println("Ncc failed, can't back-annotate");
                return false;
            }

            // find all wire models in schematic
            int wiresUpdated = 0;
            ArrayList<Network> networks = new ArrayList<Network>();
            HashMap<Network,NodeInst> map = new HashMap<Network,NodeInst>();        // map of networks to associated wire model nodeinst
            for (Iterator<NodeInst> it = schLayCells[0].getNodes(); it.hasNext(); ) {
                NodeInst ni = it.next();
                Variable var = ni.getVar(LENetlister.ATTR_LEWIRE);
                if (var == null) continue;
                var = ni.getVar(LENetlister.ATTR_L);
                if (var == null) {
                    System.out.println("No attribute L on wire model "+ni.describe(true)+", ignoring it.");
                    continue;
                }
                // grab network wire model is on
                PortInst pi = ni.getPortInst(0);
                if (pi == null) continue;
                Netlist netlist = schLayCells[0].getNetlist(true);
                Network schNet = netlist.getNetwork(pi);
                networks.add(schNet);
                map.put(schNet, ni);
            }
            // sort networks by name
            Collections.sort(networks, new TextUtils.NetworksByName());

            // update wire models
            for (Network schNet : networks) {
                // find equivalent network in layouy
                Equivalence equiv = result.getEquivalence();
                HierarchyEnumerator.NetNameProxy proxy = equiv.findEquivalentNet(VarContext.globalContext, schNet);
                if (proxy == null) {
                    System.out.println("No matching network in layout for "+proxy.toString()+", ignoring");
                    continue;
                }
                Network layNet = proxy.getNet();
                Cell netcell = layNet.getParent();
                // get wire length
                HashSet<Network> nets = new HashSet<Network>();
                nets.add(layNet);
                LayerCoverageTool.GeometryOnNetwork geoms = LayerCoverageTool.listGeometryOnNetworks(netcell, nets,
                        false, GeometryHandler.GHMode.ALGO_SWEEP);
                double length = geoms.getTotalWireLength();

                // update wire length
                NodeInst ni = (NodeInst)map.get(schNet);
                ni.updateVar(LENetlister.ATTR_L, new Double(length));
                wiresUpdated++;
                System.out.println("Updated wire model "+ni.getName()+" on layout network "+proxy.toString()+" to: "+length+" lambda");
            }
            System.out.println("Updated "+wiresUpdated+" wire models in "+schLayCells[0]+" from layout "+schLayCells[1]);
            return true;
        }
    }

	public static void clearSizesNodableCommand() {
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
		Highlighter highlighter = wnd.getHighlighter();

		if (highlighter.getNumHighlights() == 0) {
			System.out.println("Nothing highlighted");
			return;
		}
		for (Highlight2 h : highlighter.getHighlights()) {
			if (!h.isHighlightEOBJ()) continue;

			ElectricObject eobj = h.getElectricObject();
			if (eobj instanceof PortInst) {
				PortInst pi = (PortInst)eobj;
				pi.getInfo();
				eobj = pi.getNodeInst();
			}
			if (eobj instanceof NodeInst) {
				NodeInst ni = (NodeInst)eobj;
				LETool.clearStoredSizesJob(ni);
			}
		}
		System.out.println("Sizes cleared");
	}

	public static void clearSizesCommand() {
		for (Library lib : Library.getVisibleLibraries())
		{
			LETool.clearStoredSizesJob(lib);
		}
		System.out.println("Sizes cleared");
	}

	public static void loadLogicalEffortLibraries()
	{
		if (Library.findLibrary("purpleGeneric180") != null) return;
		URL url = LibFile.getLibFile("purpleGeneric180.jelib");
		new FileMenu.ReadLibrary(url, FileType.JELIB, null, null);
	}

	/**
	 * Method to handle the "Show Network" command.
	 */
	public static void showNetworkCommand()
	{
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
		Cell cell = wnd.getCell();
		if (wnd == null) return;
		Highlighter highlighter = wnd.getHighlighter();

		Set<Network> nets = highlighter.getHighlightedNetworks();
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted netlist display (network information unavailable).  Please try again");
			return;
		}
		highlighter.showNetworks(nets, netlist, cell);
        // 3D display if available
        WindowFrame.show3DHighlight();
		highlighter.finished();
	}

	/**
	 * Method to handle the "List Networks" command.
	 */
	public static void listNetworksCommand()
	{
		Cell cell = WindowFrame.getCurrentCell();
		if (cell == null) return;
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted netlist display (network information unavailable).  Please try again");
			return;
		}
		int total = 0;
		for(Iterator<Network> it = netlist.getNetworks(); it.hasNext(); )
		{
			Network net = it.next();
			String netName = net.describe(false);
			if (netName.length() == 0) continue;
			StringBuffer infstr = new StringBuffer();
			infstr.append("'" + netName + "'");
//			if (net->buswidth > 1)
//			{
//				formatinfstr(infstr, _(" (bus with %d signals)"), net->buswidth);
//			}
			boolean connected = false;
			for(Iterator<ArcInst> aIt = net.getArcs(); aIt.hasNext(); )
			{
				ArcInst ai = aIt.next();
				if (!connected)
				{
					connected = true;
					infstr.append(", on arcs:");
				}
				infstr.append(" " + ai.describe(true));
			}

			boolean exported = false;
			for(Iterator<Export> eIt = net.getExports(); eIt.hasNext(); )
			{
				Export pp = eIt.next();
				if (!exported)
				{
					exported = true;
					infstr.append(", with exports:");
				}
				infstr.append(" " + pp.getName());
			}
			System.out.println(infstr.toString());
			total++;
		}
		if (total == 0) System.out.println("There are no networks in this cell");
	}

    /**
     * Method to handle the "List Connections On Network" command.
     */
    public static void listConnectionsOnNetworkCommand()
    {
        Cell cell = WindowFrame.needCurCell();
        if (cell == null) return;
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

        Set<Network> nets = highlighter.getHighlightedNetworks();
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted query (network information unavailable).  Please try again");
			return;
		}
        for(Network net : nets)
        {
            System.out.println("Network " + net.describe(true) + ":");

            int total = 0;
            for(Iterator<Nodable> nIt = netlist.getNodables(); nIt.hasNext(); )
            {
                Nodable no = nIt.next();
                NodeProto np = no.getProto();

                HashMap<Network,HashSet<Object>> portNets = new HashMap<Network,HashSet<Object>>();
                for(Iterator<PortProto> pIt = np.getPorts(); pIt.hasNext(); )
                {
                    PortProto pp = pIt.next();
                    if (pp instanceof PrimitivePort && ((PrimitivePort)pp).isIsolated())
                    {
                        NodeInst ni = (NodeInst)no;
                        for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
                        {
                            Connection con = cIt.next();
                            ArcInst ai = con.getArc();
                            Network oNet = netlist.getNetwork(ai, 0);
                            HashSet<Object> ports = (HashSet<Object>)portNets.get(oNet);
                            if (ports == null) {
                                ports = new HashSet<Object>();
                                portNets.put(oNet, ports);
                            }
                            ports.add(pp);
                        }
                    } else
                    {
                        int width = 1;
                        if (pp instanceof Export)
                        {
                            Export e = (Export)pp;
                            width = netlist.getBusWidth(e);
                        }
                        for(int i=0; i<width; i++)
                        {
                            Network oNet = netlist.getNetwork(no, pp, i);
                            HashSet<Object> ports = (HashSet<Object>)portNets.get(oNet);
                            if (ports == null) {
                                ports = new HashSet<Object>();
                                portNets.put(oNet, ports);
                            }
                            ports.add(pp);
                        }
                    }
                }

                // if there is only 1 net connected, the node is unimportant
                if (portNets.size() <= 1) continue;
                HashSet<Object> ports = (HashSet<Object>)portNets.get(net);
                if (ports == null) continue;

                if (total == 0) System.out.println("  Connects to:");
                String name = null;
                if (no instanceof NodeInst) name = ((NodeInst)no).describe(false); else
                {
                    name = no.getName();
                }
                for (Object obj : ports) {
                    PortProto pp = (PortProto)obj;
                    System.out.println("    Node " + name + ", port " + pp.getName());
                    total++;
                }
            }
            if (total == 0) System.out.println("  Not connected");
        }
    }

    /**
     * Method to handle the "List Exports On Network" command.
     */
    public static void listExportsOnNetworkCommand()
    {
        Cell cell = WindowFrame.needCurCell();
        if (cell == null) return;
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

        Set<Network> nets = highlighter.getHighlightedNetworks();
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted query (network information unavailable).  Please try again");
			return;
		}
        for(Network net : nets)
        {
            System.out.println("Network '" + net.describe(true) + "':");

            // find all exports on network "net"
            HashSet<Export> listedExports = new HashSet<Export>();
            System.out.println("  Going up the hierarchy from " + cell + ":");
            if (findPortsUp(netlist, net, cell, listedExports)) break;
            System.out.println("  Going down the hierarchy from " + cell + ":");
            if (findPortsDown(netlist, net, listedExports)) break;
        }
    }

    /**
     * Method to handle the "List Exports Below Network" command.
     */
    public static void listExportsBelowNetworkCommand()
    {
        Cell cell = WindowFrame.needCurCell();
        if (cell == null) return;
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

        Set<Network> nets = highlighter.getHighlightedNetworks();
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted query (network information unavailable).  Please try again");
			return;
		}
        for(Network net : nets)
        {
            System.out.println("Network " + net.describe(true) + ":");

            // find all exports on network "net"
            if (findPortsDown(netlist, net, new HashSet<Export>())) break;
        }
    }

    /**
     * helper method for "telltool network list-hierarchical-ports" to print all
     * ports connected to net "net" in cell "cell", and recurse up the hierarchy.
     * @return true if an error occurred.
     */
    private static boolean findPortsUp(Netlist netlist, Network net, Cell cell, HashSet<Export> listedExports)
    {
        // look at every node in the cell
        for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
        {
            Export pp = (Export)it.next();
            int width = netlist.getBusWidth(pp);
            for(int i=0; i<width; i++)
            {
                Network ppNet = netlist.getNetwork(pp, i);
                if (ppNet != net) continue;
                if (listedExports.contains(pp)) continue;
                listedExports.add(pp);
                System.out.println("    Export " + pp.getName() + " in " + cell);

                // code to find the proper instance
                Cell instanceCell = cell.iconView();
                if (instanceCell == null) instanceCell = cell;

                // ascend to higher cell and continue
                for(Iterator<CellUsage> uIt = instanceCell.getUsagesOf(); uIt.hasNext(); )
                {
                    CellUsage u = uIt.next();
                    Cell superCell = u.getParent();
            		Netlist superNetlist = cell.acquireUserNetlist();
            		if (superNetlist == null)
            		{
            			System.out.println("Sorry, a deadlock aborted query (network information unavailable).  Please try again");
            			return true;
            		}
                    for(Iterator<Nodable> nIt = superNetlist.getNodables(); nIt.hasNext(); )
                    {
                        Nodable no = nIt.next();
                        if (no.getProto() != cell) continue;
                        Network superNet = superNetlist.getNetwork(no, pp, i);
                        if (findPortsUp(superNetlist, superNet, superCell, listedExports)) return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * helper method for "telltool network list-hierarchical-ports" to print all
     * ports connected to net "net" in cell "cell", and recurse down the hierarchy
     * @return true on error.
     */
    private static boolean findPortsDown(Netlist netlist, Network net, HashSet<Export> listedExports)
    {
        // look at every node in the cell
        for(Iterator<Nodable> it = netlist.getNodables(); it.hasNext(); )
        {
            Nodable no = it.next();

            // only want complex nodes
            if (!no.isCellInstance()) continue;
            Cell subCell = (Cell)no.getProto();

            // look at all wires connected to the node
            for(Iterator<PortProto> pIt = subCell.getPorts(); pIt.hasNext(); )
            {
                Export pp = (Export)pIt.next();
                int width = netlist.getBusWidth(pp);
                for(int i=0; i<width; i++)
                {
                    Network oNet = netlist.getNetwork(no, pp, i);
                    if (oNet != net) continue;

                    // found the net here: report it
                    if (listedExports.contains(pp)) continue;
                    listedExports.add(pp);
                    System.out.println("    Export " + pp.getName() + " in " + subCell);
            		Netlist subNetlist = subCell.acquireUserNetlist();
            		if (subNetlist == null)
            		{
            			System.out.println("Sorry, a deadlock aborted query (network information unavailable).  Please try again");
            			return true;
            		}
                    Network subNet = subNetlist.getNetwork(pp, i);
                    if (findPortsDown(subNetlist, subNet, listedExports)) return true;
                }
            }
        }
        return false;
    }

    /**
     * Method to handle the "List Geometry On Network" command.
     */
    public static void listGeometryOnNetworkCommand(GeometryHandler.GHMode mode)
    {
	    Cell cell = WindowFrame.needCurCell();
        if (cell == null) return;
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;

        HashSet<Network> nets = (HashSet<Network>)wnd.getHighlighter().getHighlightedNetworks();
        if (nets.isEmpty())
        {
            System.out.println("No network in " + cell + " selected");
            return;
        }
	    else
            LayerCoverageTool.listGeometryOnNetworks(cell, nets, true, mode);
    }

    private static final double SQSIZE = 0.4;

    /**
     * Method to highlight every network in the current cell
     * using a different color
     */
    private static void showAllNetworksCommand()
    {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Cell cell = wnd.getCell();
        if (cell == null) return;
        wnd.clearHighlighting();
        Netlist nl = cell.acquireUserNetlist();
        int colors = nl.getNumNetworks();
        Color [] netColors = makeUniqueColors(colors);
        int index = 0;
		Highlighter h = wnd.getHighlighter();
        for(Iterator<Network> it = nl.getNetworks(); it.hasNext(); )
        {
        	Network net = it.next();
        	Iterator<ArcInst> aIt = net.getArcs();
        	if (!aIt.hasNext()) continue;
        	Color col = netColors[index++];
        	for( ; aIt.hasNext(); )
        	{
        		ArcInst ai = aIt.next();
                Point2D [] points = new Point2D[2];
        		points[0] = ai.getHeadLocation();
        		points[1] = ai.getTailLocation();
        		Poly poly = new Poly(points);
        		poly.setStyle(Poly.Type.OPENED);
        		h.addPoly(poly, cell, col);
        		if (ai.getHeadPortInst().getNodeInst().isCellInstance())
        		{
                    points = new Point2D[4];
            		EPoint ctr = ai.getHeadLocation();
            		points[0] = new EPoint(ctr.getX()-SQSIZE, ctr.getY()-SQSIZE);
            		points[1] = new EPoint(ctr.getX()-SQSIZE, ctr.getY()+SQSIZE);
            		points[2] = new EPoint(ctr.getX()+SQSIZE, ctr.getY()+SQSIZE);
            		points[3] = new EPoint(ctr.getX()+SQSIZE, ctr.getY()-SQSIZE);
            		poly = new Poly(points);
            		poly.setStyle(Poly.Type.CLOSED);
            		h.addPoly(poly, cell, col);
        		}
        		if (ai.getTailPortInst().getNodeInst().isCellInstance())
        		{
                    points = new Point2D[4];
            		EPoint ctr = ai.getTailLocation();
            		points[0] = new EPoint(ctr.getX()-SQSIZE, ctr.getY()-SQSIZE);
            		points[1] = new EPoint(ctr.getX()-SQSIZE, ctr.getY()+SQSIZE);
            		points[2] = new EPoint(ctr.getX()+SQSIZE, ctr.getY()+SQSIZE);
            		points[3] = new EPoint(ctr.getX()+SQSIZE, ctr.getY()-SQSIZE);
            		poly = new Poly(points);
            		poly.setStyle(Poly.Type.CLOSED);
            		h.addPoly(poly, cell, col);
        		}
        	}
        }
        wnd.finishedHighlighting();
    }

    /**
     * Method to generate unique colors.
     * Uses this pattern
	 *	R: 100 110    111 202 202 112    11 23 23    11 24 24
	 *	G: 010 101    202 111 022 121    23 11 32    24 11 42
	 *	B: 001 011    022 022 111 211    32 32 11    42 42 11
	 * Where:
	 *  0=off
	 *	1=the main color
	 *	2=halfway between 1 and 0
	 *	3=halfway between 1 and 2
	 *	4=halfway between 2 and 0
     * @param numColors the number of colors to generate
     * @return an array of colors.
     */
	private static Color [] makeUniqueColors(int numColors)
	{
		int numRuns = (numColors+29) / 30;
		Color [] colors = new Color[numColors];
		int index = 0;
		for(int i=0; i<numRuns; i++)
		{
			int c1 = 255 - 255/numRuns*i;
			int c2 = c1 / 2;
			int c3 = c2 / 2;
			int c4 = (c1 + c2) / 2;

			// combinations of color 1
			if (index < numColors) colors[index++] = new Color(c1,  0,  0);
			if (index < numColors) colors[index++] = new Color( 0, c1,  0);
			if (index < numColors) colors[index++] = new Color( 0,  0, c1);

			if (index < numColors) colors[index++] = new Color(c1, c1,  0);
			if (index < numColors) colors[index++] = new Color( 0, c1, c1);
			if (index < numColors) colors[index++] = new Color(c1,  0, c1);

			// combinations of the colors 1 and 2
			if (index < numColors) colors[index++] = new Color(c1, c2,  0);
			if (index < numColors) colors[index++] = new Color(c1,  0, c2);
			if (index < numColors) colors[index++] = new Color(c1, c2, c2);

			if (index < numColors) colors[index++] = new Color(c2, c1,  0);
			if (index < numColors) colors[index++] = new Color( 0, c1, c2);
			if (index < numColors) colors[index++] = new Color(c2, c1, c2);

			if (index < numColors) colors[index++] = new Color(c2,  0, c1);
			if (index < numColors) colors[index++] = new Color( 0, c2, c1);
			if (index < numColors) colors[index++] = new Color(c2, c2, c1);

			// combinations of colors 1, 2, and 3
			if (index < numColors) colors[index++] = new Color(c1, c2, c3);
			if (index < numColors) colors[index++] = new Color(c1, c3, c2);
			if (index < numColors) colors[index++] = new Color(c2, c1, c3);
			if (index < numColors) colors[index++] = new Color(c3, c1, c2);
			if (index < numColors) colors[index++] = new Color(c2, c3, c1);
			if (index < numColors) colors[index++] = new Color(c3, c2, c1);

			// combinations of colors 1, 2, and 4
			if (index < numColors) colors[index++] = new Color(c1, c2, c4);
			if (index < numColors) colors[index++] = new Color(c1, c4, c2);
			if (index < numColors) colors[index++] = new Color(c2, c1, c4);
			if (index < numColors) colors[index++] = new Color(c4, c1, c2);
			if (index < numColors) colors[index++] = new Color(c2, c4, c1);
			if (index < numColors) colors[index++] = new Color(c4, c2, c1);
		}
		return colors;
	}

    public static void listGeomsAllNetworksCommand() {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Cell cell = wnd.getCell();
        if (cell == null) return;
        new ListGeomsAllNetworksJob(cell);
    }

    private static class ListGeomsAllNetworksJob extends Job {
        private Cell cell;

    	public ListGeomsAllNetworksJob(Cell cell) {
            super("ListGeomsAllNetworks", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.cell = cell;
            startJob();
        }

        public boolean doIt() throws JobException {
            Netlist netlist = cell.getNetlist(true);
            List<Network> networks = new ArrayList<Network>();
            for (Iterator<Network> it = netlist.getNetworks(); it.hasNext(); ) {
                networks.add(it.next());
            }
            // sort list of networks by name
            Collections.sort(networks, new TextUtils.NetworksByName());
            for (Network net : networks) {
                HashSet<Network> nets = new HashSet<Network>();
                nets.add(net);
                LayerCoverageTool.GeometryOnNetwork geoms = LayerCoverageTool.listGeometryOnNetworks(cell, nets,
                        false, GeometryHandler.GHMode.ALGO_SWEEP);
                if (geoms.getTotalWireLength() == 0) continue;
                System.out.println("Network "+net+" has wire length "+geoms.getTotalWireLength());
            }
            return true;
        }
    }

	public static void showPowerAndGround()
    {
        Cell cell = WindowFrame.needCurCell();
        if (cell == null) return;
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

//        Netlist netlist = cell.getUserNetlist();
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted query (network information unavailable).  Please try again");
			return;
		}
        HashSet<Network> pAndG = new HashSet<Network>();
        for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
        {
            Export pp = (Export)it.next();
            if (pp.isPower() || pp.isGround())
            {
                int width = netlist.getBusWidth(pp);
                for(int i=0; i<width; i++)
                {
                    Network net = netlist.getNetwork(pp, i);
                    pAndG.add(net);
                }
            }
        }
        for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
        {
            NodeInst ni = it.next();
            PrimitiveNode.Function fun = ni.getFunction();
            if (fun != PrimitiveNode.Function.CONPOWER && fun != PrimitiveNode.Function.CONGROUND)
                continue;
            for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
            {
                Connection con = cIt.next();
                ArcInst ai = con.getArc();
                int width = netlist.getBusWidth(ai);
                for(int i=0; i<width; i++)
                {
                    Network net = netlist.getNetwork(ai, i);
                    pAndG.add(net);
                }
            }
        }

        highlighter.clear();
        for(Network net : pAndG)
        {
            highlighter.addNetwork(net, cell);
        }
        highlighter.finished();
        if (pAndG.size() == 0)
            System.out.println("This cell has no Power or Ground networks");
    }

	private static class RepairPowerAndGround extends Job
    {
        protected RepairPowerAndGround()
        {
            super("Repair Power and Ground", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            startJob();
        }

        public boolean doIt() throws JobException
        {
			validatePowerAndGround(true);
            return true;
        }
    }

    public static void validatePowerAndGround(boolean repair)
    {
		if (repair) System.out.println("Repairing power and ground exports"); else
			System.out.println("Validating power and ground exports");
        int total = 0;
        for(Iterator<Library> lIt = Library.getLibraries(); lIt.hasNext(); )
        {
            Library lib = lIt.next();
            for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
            {
                Cell cell = cIt.next();
                for(Iterator<PortProto> pIt = cell.getPorts(); pIt.hasNext(); )
                {
                    Export pp = (Export)pIt.next();
                    if (pp.isNamedGround() && pp.getCharacteristic() != PortCharacteristic.GND)
                    {
                        System.out.println("Cell " + cell.describe(true) + ", export " + pp.getName() +
                            ": does not have 'GROUND' characteristic");
						if (repair) pp.setCharacteristic(PortCharacteristic.GND);
                        total++;
                    }
                    if (pp.isNamedPower() && pp.getCharacteristic() != PortCharacteristic.PWR)
                    {
                        System.out.println("Cell " + cell.describe(true) + ", export " + pp.getName() +
                            ": does not have 'POWER' characteristic");
						if (repair) pp.setCharacteristic(PortCharacteristic.PWR);
                        total++;
                    }
                }
            }
        }
        if (total == 0) System.out.println("No problems found"); else
		{
            if (repair) System.out.println("Fixed " + total + " export problems"); else
				System.out.println("Found " + total + " export problems");
		}
    }

    /**
     * Method to add a "M" multiplier factor to the currently selected node.
     */
    public static void addMultiplierCommand()
    {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

        NodeInst ni = (NodeInst)highlighter.getOneElectricObject(NodeInst.class);
        if (ni == null) return;
        new AddMultiplier(ni);
    }

    private static class AddMultiplier extends Job
    {
        private NodeInst ni;

        protected AddMultiplier(NodeInst ni)
        {
            super("Add Spice Multiplier", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.ni = ni;
            startJob();
        }

        public boolean doIt() throws JobException
        {
            TextDescriptor td = TextDescriptor.getNodeTextDescriptor().withDispPart(TextDescriptor.DispPos.NAMEVALUE).withOff(-1.5, -1);
            ni.newVar(Simulation.M_FACTOR_KEY, new Double(1.0), td);
            return true;
        }
    }

	/**
     * Method to create a new template in the current cell.
     * Templates can be for SPICE or Verilog, depending on the Variable name.
     * @param templateKey the name of the variable to create.
     */
    public static void makeTemplate(Variable.Key templateKey)
    {
        new MakeTemplate(templateKey);
    }

    private static class MakeTemplate extends Job
    {
        private Variable.Key templateKey;

        protected MakeTemplate(Variable.Key templateKey)
        {
            super("Make template", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.templateKey = templateKey;
            startJob();
        }

        public boolean doIt() throws JobException
        {
            Cell cell = WindowFrame.needCurCell();
            if (cell == null) return false;
            Variable templateVar = cell.getVar(templateKey);
            if (templateVar != null)
            {
                System.out.println("This cell already has a template");
                return false;
            }
            TextDescriptor td = TextDescriptor.getCellTextDescriptor().withInterior(true).withDispPart(TextDescriptor.DispPos.NAMEVALUE);
            cell.newVar(templateKey, "*Undefined", td);
            return true;
        }
    }

    public static void getUnroutedArcCommand()
    {
		User.getUserTool().setCurrentArcProto(Generic.tech.unrouted_arc);
    }

    public static void padFrameGeneratorCommand()
    {
        String fileName = OpenFile.chooseInputFile(FileType.PADARR, null);
        if (fileName != null)
        {
            PadGenerator.makePadFrame(Library.getCurrent(), fileName);
        }
    }

    public static void listToolsCommand()
    {
        System.out.println("Tools in Electric:");
        for(Iterator<Tool> it = Tool.getTools(); it.hasNext(); )
        {
            Tool tool = it.next();
            StringBuffer infstr = new StringBuffer();
            if (tool.isOn()) infstr.append("On"); else
                infstr.append("Off");
            if (tool.isBackground()) infstr.append(", Background");
            if (tool.isFixErrors()) infstr.append(", Correcting");
            if (tool.isIncremental()) infstr.append(", Incremental");
            if (tool.isAnalysis()) infstr.append(", Analysis");
            if (tool.isSynthesis()) infstr.append(", Synthesis");
            System.out.println(tool.getName() + ": " + infstr.toString());
        }
    }

    public static void javaBshScriptCommand()
    {
        String fileName = OpenFile.chooseInputFile(FileType.JAVA, null);
        if (fileName != null)
        {
            // start a job to run the script
            EvalJavaBsh.runScript(fileName);
        }
    }

	private static final int CONVERT_TO_VHDL     = 1;
	private static final int COMPILE_VHDL_FOR_SC = 2;
	private static final int PLACE_AND_ROUTE     = 4;
	private static final int SHOW_CELL           = 8;

	/**
	 * Method to handle the menu command to convert a cell to layout.
	 * Reads the cell library if necessary;
	 * Converts a schematic to VHDL if necessary;
	 * Compiles a VHDL cell to a netlist if necessary;
	 * Reads the netlist from the cell;
	 * does placement and routing;
	 * Generates Electric layout;
	 * Displays the resulting layout.
	 * @param cell the cell to compile.
     * @param doItNow if the job must executed now
	 */
	public static void doSiliconCompilation(Cell cell, boolean doItNow)
	{
		if (cell == null) return;
		int activities = PLACE_AND_ROUTE | SHOW_CELL;

		// see if the current cell needs to be compiled
		if (cell.getView() != View.NETLISTQUISC)
		{
			if (cell.getView() == View.SCHEMATIC)
			{
				// current cell is Schematic.  See if there is a more recent netlist or VHDL
				Cell vhdlCell = cell.otherView(View.VHDL);
				if (vhdlCell != null && vhdlCell.getRevisionDate().after(cell.getRevisionDate())) cell = vhdlCell; else
					activities |= CONVERT_TO_VHDL | COMPILE_VHDL_FOR_SC;
			}
			if (cell.getView() == View.VHDL)
			{
				// current cell is VHDL.  See if there is a more recent netlist
				Cell netListCell = cell.otherView(View.NETLISTQUISC);
				if (netListCell != null && netListCell.getRevisionDate().after(cell.getRevisionDate())) cell = netListCell; else
					activities |= COMPILE_VHDL_FOR_SC;
			}
		}

		if (Library.findLibrary(SilComp.SCLIBNAME) == null)
        {
            if (doItNow)
                ReadSCLibraryJob.performTaskNoJob();
            else
			    new ReadSCLibraryJob();
        }

		// do the silicon compilation task
        doSilCompActivityNoJob(cell, activities, doItNow);
	}

	/**
	 * Method to handle the menu command to compile a VHDL cell.
	 * Compiles the VHDL in the current cell and displays the netlist.
	 */
	public static void compileVHDL()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		if (cell.getView() != View.VHDL)
		{
			System.out.println("Must be editing a VHDL cell before compiling it");
			return;
		}

		// do the VHDL compilation task
		new DoSilCompActivity(cell, COMPILE_VHDL_FOR_SC | SHOW_CELL);
	}

	/**
	 * Method to handle the menu command to make a VHDL cell.
	 * Converts the schematic in the current cell to VHDL and displays it.
	 */
	public static void makeVHDL()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		if (cell.getView() != View.SCHEMATIC)
		{
			System.out.println("Must be editing a Schematic cell before converting it to VHDL");
			return;
		}
	    new DoSilCompActivity(cell, CONVERT_TO_VHDL | SHOW_CELL);
	}

	/**
	 * Class to read the Silicon Compiler support library in a new job.
	 */
	private static class ReadSCLibraryJob extends Job
	{
		private ReadSCLibraryJob()
		{
			super("Read Silicon Compiler Library", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			startJob();
		}

        public static boolean performTaskNoJob()
        {
			// read standard cell library
			System.out.println("Reading Standard Cell Library '" + SilComp.SCLIBNAME + "'");
			URL fileURL = LibFile.getLibFile(SilComp.SCLIBNAME + ".jelib");
			LibraryFiles.readLibrary(fileURL, null, FileType.JELIB, true);
            return true;
        }

		public boolean doIt() throws JobException
		{
            return performTaskNoJob();
		}
	}

    public static boolean doSilCompActivityNoJob(Cell cell, int activities, boolean doItNow)
    {
        if (doItNow)
        {
            List<Cell> textCellsToRedraw = new ArrayList<Cell>();
            try
            {
                DoSilCompActivity.performTaskNoJob(cell, textCellsToRedraw, activities);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return false;
            }
        }
        else
		    new DoSilCompActivity(cell, activities);
        return true;
    }

	/**
	 * Class to do the next silicon-compilation activity in a Job.
	 */
	private static class DoSilCompActivity extends Job
	{
		private Cell cell;
		private int activities;
		private List<Cell> textCellsToRedraw;

		private DoSilCompActivity(Cell cell, int activities)
		{
			super("Silicon-Compiler activity", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.activities = activities;
			startJob();
		}

        public static Cell performTaskNoJob(Cell cell, List<Cell> textCellsToRedraw, int activities) throws JobException
        {
            Library destLib = cell.getLibrary();
			textCellsToRedraw = new ArrayList<Cell>();

			if ((activities&CONVERT_TO_VHDL) != 0)
			{
				// convert Schematic to VHDL
				System.out.print("Generating VHDL from " + cell + " ...");
				List<String> vhdlStrings = GenerateVHDL.convertCell(cell);
				if (vhdlStrings == null)
					throw new JobException("No VHDL produced");

				String cellName = cell.getName() + "{vhdl}";
				Cell vhdlCell = cell.getLibrary().findNodeProto(cellName);
				if (vhdlCell == null)
				{
					vhdlCell = Cell.makeInstance(cell.getLibrary(), cellName);
					if (vhdlCell == null) return null;
				}
				String [] array = new String[vhdlStrings.size()];
				for(int i=0; i<vhdlStrings.size(); i++) array[i] = (String)vhdlStrings.get(i);
				vhdlCell.setTextViewContents(array);
				textCellsToRedraw.add(vhdlCell);
				System.out.println(" Done, created " + vhdlCell);
				cell = vhdlCell;
			}

			if ((activities&COMPILE_VHDL_FOR_SC) != 0)
			{
				// compile the VHDL to a netlist
				System.out.print("Compiling VHDL in " + cell + " ...");
				CompileVHDL c = new CompileVHDL(cell);
				if (c.hasErrors())
					throw new JobException("ERRORS during compilation, no netlist produced");
				List<String> netlistStrings = c.getQUISCNetlist(destLib);
				if (netlistStrings == null)
					throw new JobException("No netlist produced");

				// store the QUISC netlist
				String cellName = cell.getName() + "{net.quisc}";
				Cell netlistCell = cell.getLibrary().findNodeProto(cellName);
				if (netlistCell == null)
				{
					netlistCell = Cell.makeInstance(cell.getLibrary(), cellName);
					if (netlistCell == null) return null;
				}
				String [] array = new String[netlistStrings.size()];
				for(int i=0; i<netlistStrings.size(); i++) array[i] = (String)netlistStrings.get(i);
				netlistCell.setTextViewContents(array);
				textCellsToRedraw.add(netlistCell);
				System.out.println(" Done, created " + netlistCell);
				cell = netlistCell;
			}

			if ((activities&PLACE_AND_ROUTE) != 0)
			{
				// first grab the information in the netlist
				System.out.println("Reading netlist in " + cell);
				GetNetlist gnl = new GetNetlist();
				if (gnl.readNetCurCell(cell))
					throw new JobException("Error compiling netlist");

				// do the placement
				System.out.println("Placing cells");
				Place place = new Place();
				String err = place.placeCells(gnl);
				if (err != null)
					throw new JobException(err);

				// do the routing
				System.out.println("Routing cells");
				Route route = new Route();
				err = route.routeCells(gnl);
				if (err != null)
					throw new JobException(err);

				// generate the results
				System.out.println("Generating layout");
				Maker maker = new Maker();
				Object result = maker.makeLayout(destLib, gnl);
				if (result instanceof String)
				{
					System.out.println((String)result);
					if (Technology.getCurrent() == Schematics.tech)
						throw new JobException("Should switch to a layout technology first (currently in Schematics)");
				}
				if (!(result instanceof Cell)) return null;
				cell = (Cell)result;
				System.out.println("Created " + cell);
			}
		    return cell;
        }

		public boolean doIt() throws JobException
		{
			fieldVariableChanged("cell");
			textCellsToRedraw = new ArrayList<Cell>();
			fieldVariableChanged("textCellsToRedraw");
			cell = performTaskNoJob(cell, textCellsToRedraw, activities);
            return true;
		}

        public void terminateOK()
        {
            for(Cell cell : textCellsToRedraw) {
                TextWindow.updateText(cell);
            }
            if ((activities&SHOW_CELL) != 0) {
                // show the cell
                WindowFrame.createEditWindow(cell);
            }
        }
	}

    /****************** Parasitic Tool ********************/

	public static void parasiticCommand()
    {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Cell cell = wnd.getCell();
        Highlighter highlighter = wnd.getHighlighter();

        Set<Network> nets = highlighter.getHighlightedNetworks();
        for (Network net : nets)
        {
            ParasiticTool.getParasiticTool().netwokParasitic(net, cell);
        }
    }

    public static void importAssuraDrcErrors() {
        String fileName = OpenFile.chooseInputFile(FileType.ERR, null);
        if (fileName == null) return;
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Cell cell = wnd.getCell();
        if (cell == null) return;
        HashMap<Cell,String> mangledNames = new HashMap<Cell,String>();
        com.sun.electric.tool.io.output.GDS.buildUniqueNames(cell, mangledNames);
        AssuraDrcErrors.importErrors(fileName, mangledNames, "DRC");
    }    

    public static void importCalibreDrcErrors() {
        String fileName = OpenFile.chooseInputFile(FileType.DB, null);
        if (fileName == null) return;
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Cell cell = wnd.getCell();
        HashMap<Cell,String> mangledNames = new HashMap<Cell,String>();
        com.sun.electric.tool.io.output.GDS.buildUniqueNames(cell, mangledNames);
        CalibreDrcErrors.importErrors(fileName, mangledNames, "DRC");
    }

    public static void exportDRCDeck() {
        String fileName = OpenFile.chooseOutputFile(FileType.XML,
                "Save XML DRC deck for foundry '" + Technology.getCurrent().getSelectedFoundry() + "'", null);
        if (fileName == null) return;
        DRCTemplate.exportDRCDecks(fileName, Technology.getCurrent());
    }

    public static void importDRCDeck() {
        String fileName = OpenFile.chooseInputFile(FileType.XML, "Open XML DRC deck", false);
        if (fileName == null) return;
        Technology tech = Technology.getCurrent();
        DRCTemplate.DRCXMLParser parser = DRCTemplate.importDRCDeck(TextUtils.makeURLToFile(fileName), true);
        String message = "Deck file '" + fileName + "' loaded ";

        message += (parser.isParseOK()) ? "without errors." : " with errors. No rules loaded.";

        JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), message,
                    "Import DRC Deck", (parser.isParseOK()) ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE);

        if (!parser.isParseOK())  return; // errors in the file
        
        for (DRCTemplate.DRCXMLBucket bucket : parser.getRules())
        {
            boolean done = false;

            // set the new rules under the foundry imported
            for (Iterator<Foundry> itF = tech.getFoundries(); itF.hasNext();)  
            {
                Foundry f = itF.next();
                if (f.getType().name().equalsIgnoreCase(bucket.foundry))
                {
                    f.setRules(bucket.drcRules);
                    System.out.println("New DRC rules for foundry '" + f.getType().name() + "' were loaded in '" +
                            tech.getTechName() + "'");
                    // Need to clean cells using this foundry because the rules might have changed.
                    DRC.cleanCellsDueToFoundryChanges(tech, f);
                    // Only when the rules belong to the selected foundry, then reload the rules
                    if (f == tech.getSelectedFoundry())
                        tech.setState(true);
                    done = true;
                    break;
                }
            }
            if (!done)
            {
                JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
                    "'" + bucket.foundry + "' is not a valid foundry in '" + tech.getTechName() + "'",
                    "Importing DRC Deck", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void runNccSchematicCrossProbing() {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Cell cell = wnd.getCell();
        if (cell == null) return;
        NccCrossProbing.runNccSchematicCrossProbing(cell, wnd.getVarContext());
    }

//    private static void newAutoFill(boolean hierarchy, boolean binary)
//    {
//        Cell cell = WindowFrame.getCurrentCell();
//        if (cell == null) return;
//
//        FillGeneratorTool.generateAutoFill(cell, hierarchy, binary, false);
//    }

}
