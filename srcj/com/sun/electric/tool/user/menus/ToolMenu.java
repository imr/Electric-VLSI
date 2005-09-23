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

import com.sun.electric.database.CellUsage;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.GeometryHandler;
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
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.compaction.Compaction;
import com.sun.electric.tool.drc.AssuraDrcErrors;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.CalibreDrcErrors;
import com.sun.electric.tool.erc.ERCAntenna;
import com.sun.electric.tool.erc.ERCWellCheck;
import com.sun.electric.tool.extract.Connectivity;
import com.sun.electric.tool.extract.LayerCoverage;
import com.sun.electric.tool.extract.LayerCoverageJob;
import com.sun.electric.tool.extract.ParasiticTool;
import com.sun.electric.tool.generator.PadGenerator;
import com.sun.electric.tool.generator.ROMGenerator;
import com.sun.electric.tool.generator.layout.Tech;
import com.sun.electric.tool.generator.cmosPLA.PLA;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.input.Simulate;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.io.output.Verilog;
import com.sun.electric.tool.logicaleffort.LETool;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.NccResult;
import com.sun.electric.tool.ncc.NetEquivalence;
import com.sun.electric.tool.ncc.basic.NccUtils;
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
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.FastHenryArc;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.KeyStroke;


/**
 * Class to handle the commands in the "Tool" pulldown menu.
 */
public class ToolMenu {

	protected static void addToolMenu(MenuBar menuBar) {
		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		/****************************** THE TOOL MENU ******************************/

		// mnemonic keys available:  B   F H JK    PQ      XYZ
		MenuBar.Menu toolMenu = MenuBar.makeMenu("_Tool");
		menuBar.add(toolMenu);

		//------------------- DRC

		// mnemonic keys available:  B  EFG IJK MNOPQR TUVWXYZ
		MenuBar.Menu drcSubMenu = MenuBar.makeMenu("_DRC");
		toolMenu.add(drcSubMenu);
		drcSubMenu.addMenuItem("Check _Hierarchically", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { DRC.checkHierarchically(false, GeometryHandler.ALGO_SWEEP); }});
		drcSubMenu.addMenuItem("Check _Selection Area Hierarchically", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { DRC.checkHierarchically(true, GeometryHandler.ALGO_SWEEP); }});
        drcSubMenu.addMenuItem("Check Area _Coverage", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { layerCoverageCommand(null, GeometryHandler.ALGO_SWEEP, true);} });

        drcSubMenu.addMenuItem("_List Layer Coverage on Cell", null,
                new ActionListener() { public void actionPerformed(ActionEvent e) { layerCoverageCommand(Job.Type.EXAMINE,
                        LayerCoverageJob.AREA, GeometryHandler.ALGO_SWEEP); } });

		drcSubMenu.addSeparator();
		drcSubMenu.addMenuItem("Import _Assura DRC Errors...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { importAssuraDrcErrors();}});
        drcSubMenu.addMenuItem("Import Calibre _DRC Errors...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { importCalibreDrcErrors();}});

		//------------------- Simulation (Built-in)

		// mnemonic keys available:  B  EF   JK  N PQ    V XYZ
		MenuBar.Menu builtInSimulationSubMenu = MenuBar.makeMenu("Simulation (Built-in)");
		toolMenu.add(builtInSimulationSubMenu);
		if (Simulation.hasIRSIM())
		{
			builtInSimulationSubMenu.addMenuItem("IRSI_M: Simulate Current Cell", null,
				new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.startSimulation(Simulation.IRSIM_ENGINE, false, null, null); } });
			builtInSimulationSubMenu.addMenuItem("IRSIM: _Write Deck...", null,
				new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCommand(FileType.IRSIM, true); }});
			builtInSimulationSubMenu.addMenuItem("_IRSIM: Simulate Deck...", null,
				new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.startSimulation(Simulation.IRSIM_ENGINE, true, null, null); }});

			builtInSimulationSubMenu.addSeparator();
		}
		builtInSimulationSubMenu.addMenuItem("_ALS: Simulate Current Cell", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.startSimulation(Simulation.ALS_ENGINE, false, null, null); } });

		builtInSimulationSubMenu.addSeparator();

		builtInSimulationSubMenu.addMenuItem("Set Signal _High at Main Time", KeyStroke.getKeyStroke('V', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.setSignalHigh(); } });
		builtInSimulationSubMenu.addMenuItem("Set Signal _Low at Main Time", KeyStroke.getKeyStroke('G', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.setSignalLow(); } });
		builtInSimulationSubMenu.addMenuItem("Set Signal Un_defined at Main Time", KeyStroke.getKeyStroke('X', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.setSignalX(); } });
		builtInSimulationSubMenu.addMenuItem("Set Clock on Selected Signal...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.setClock(); } });

		builtInSimulationSubMenu.addSeparator();

		builtInSimulationSubMenu.addMenuItem("_Update Simulation Window", null,
				new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.update(); } });
		builtInSimulationSubMenu.addMenuItem("_Get Information about Selected Signals", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.showSignalInfo(); } });

		builtInSimulationSubMenu.addSeparator();

		builtInSimulationSubMenu.addMenuItem("_Clear Selected Stimuli", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.removeSelectedStimuli(); } });
		builtInSimulationSubMenu.addMenuItem("Clear All Stimuli _on Selected Signals", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.removeStimuliFromSignal(); } });
		builtInSimulationSubMenu.addMenuItem("Clear All S_timuli", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.removeAllStimuli(); } });

		builtInSimulationSubMenu.addSeparator();

		builtInSimulationSubMenu.addMenuItem("_Save Stimuli to Disk...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.saveStimuli(); } });
		builtInSimulationSubMenu.addMenuItem("_Restore Stimuli from Disk...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.restoreStimuli(); } });

		//------------------- Simulation (SPICE)

		// mnemonic keys available: AB  E   IJK  NO QR   VWXYZ
		MenuBar.Menu spiceSimulationSubMenu = MenuBar.makeMenu("Simulation (_Spice)");
		toolMenu.add(spiceSimulationSubMenu);
		spiceSimulationSubMenu.addMenuItem("Write Spice _Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCommand(FileType.SPICE, true); }});
		spiceSimulationSubMenu.addMenuItem("Write _CDL Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCommand(FileType.CDL, true); }});
		spiceSimulationSubMenu.addMenuItem("Plot Spice _Listing...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulate.plotSpiceResults(); }});
		spiceSimulationSubMenu.addMenuItem("Plot Spice _for This Cell", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulate.plotSpiceResultsThisCell(); }});
		spiceSimulationSubMenu.addMenuItem("Set Spice _Model...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.setSpiceModel(); }});
		spiceSimulationSubMenu.addMenuItem("Add M_ultiplier", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { addMultiplierCommand(); }});

		spiceSimulationSubMenu.addSeparator();

		spiceSimulationSubMenu.addMenuItem("Set Generic Spice _Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_TEMPLATE_KEY); }});
		spiceSimulationSubMenu.addMenuItem("Set Spice _2 Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_2_TEMPLATE_KEY); }});
		spiceSimulationSubMenu.addMenuItem("Set Spice _3 Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_3_TEMPLATE_KEY); }});
		spiceSimulationSubMenu.addMenuItem("Set _HSpice Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_H_TEMPLATE_KEY); }});
		spiceSimulationSubMenu.addMenuItem("Set _PSpice Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_P_TEMPLATE_KEY); }});
		spiceSimulationSubMenu.addMenuItem("Set _GnuCap Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_GC_TEMPLATE_KEY); }});
		spiceSimulationSubMenu.addMenuItem("Set _SmartSpice Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_SM_TEMPLATE_KEY); }});

		//------------------- Simulation (Verilog)

		// mnemonic keys available: AB  EFGHIJKLMNOPQRS U WXYZ
		MenuBar.Menu verilogSimulationSubMenu = MenuBar.makeMenu("Simulation (_Verilog)");
		toolMenu.add(verilogSimulationSubMenu);
		verilogSimulationSubMenu.addMenuItem("Write _Verilog Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCommand(FileType.VERILOG, true); } });
		verilogSimulationSubMenu.addMenuItem("Plot Verilog VCD _Dump...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulate.plotVerilogResults(); }});
		verilogSimulationSubMenu.addMenuItem("Plot Verilog for This _Cell", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulate.plotVerilogResultsThisCell(); }});
		verilogSimulationSubMenu.addSeparator();
		verilogSimulationSubMenu.addMenuItem("Set Verilog _Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Verilog.VERILOG_TEMPLATE_KEY); }});

		verilogSimulationSubMenu.addSeparator();

		// mnemonic keys available: ABC EFGHIJKLMNOPQRS UV XYZ
		MenuBar.Menu verilogWireTypeSubMenu = MenuBar.makeMenu("Set Verilog _Wire");
		verilogWireTypeSubMenu.addMenuItem("_Wire", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.setVerilogWireCommand(0); }});
		verilogWireTypeSubMenu.addMenuItem("_Trireg", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.setVerilogWireCommand(1); }});
		verilogWireTypeSubMenu.addMenuItem("_Default", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.setVerilogWireCommand(2); }});
		verilogSimulationSubMenu.add(verilogWireTypeSubMenu);

		// mnemonic keys available: ABCDEFGHIJKLM OPQRSTUV XYZ
		MenuBar.Menu transistorStrengthSubMenu = MenuBar.makeMenu("_Transistor Strength");
		transistorStrengthSubMenu.addMenuItem("_Weak", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.setTransistorStrengthCommand(true); }});
		transistorStrengthSubMenu.addMenuItem("_Normal", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.setTransistorStrengthCommand(false); }});
		verilogSimulationSubMenu.add(transistorStrengthSubMenu);

		//------------------- Simulation (others)

		// mnemonic keys available:  B D  G   KL N  Q   UVWXYZ
		MenuBar.Menu netlisters = MenuBar.makeMenu("Simulation (_Others)");
		toolMenu.add(netlisters);
		netlisters.addMenuItem("Write _Maxwell Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCommand(FileType.MAXWELL, true); } });
		netlisters.addMenuItem("Write _Tegas Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCommand(FileType.TEGAS, true); } });
		netlisters.addMenuItem("Write _SILOS Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCommand(FileType.SILOS, true); }});
		netlisters.addMenuItem("Write _PAL Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCommand(FileType.PAL, true); } });
		netlisters.addSeparator();
		if (!Simulation.hasIRSIM())
		{
			netlisters.addMenuItem("Write _IRSIM Deck...", null,
				new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCommand(FileType.IRSIM, true); }});
		}
		netlisters.addMenuItem("Write _ESIM/RNL Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCommand(FileType.ESIM, true); }});
		netlisters.addMenuItem("Write _RSIM Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCommand(FileType.RSIM, true); }});
		netlisters.addMenuItem("Write _COSMOS Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCommand(FileType.COSMOS, true); }});
		netlisters.addMenuItem("Write M_OSSIM Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCommand(FileType.MOSSIM, true); }});
		netlisters.addSeparator();
		netlisters.addMenuItem("Write _FastHenry Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCommand(FileType.FASTHENRY, true); }});
		netlisters.addMenuItem("Fast_Henry Arc Properties...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FastHenryArc.showFastHenryArcDialog(); }});
		netlisters.addSeparator();
		netlisters.addMenuItem("Write _ArchSim Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCommand(FileType.ARCHSIM, true); } });
		netlisters.addMenuItem("Display ArchSim _Journal...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulate.plotArchSimResults(); } });

		//------------------- ERC

		// mnemonic keys available:  BCDEFGHIJKLMNOPQRSTUV XYZ
		MenuBar.Menu ercSubMenu = MenuBar.makeMenu("_ERC");
		toolMenu.add(ercSubMenu);
		ercSubMenu.addMenuItem("Check _Wells", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ERCWellCheck.analyzeCurCell(GeometryHandler.ALGO_SWEEP); } });
		ercSubMenu.addMenuItem("_Antenna Check", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { new ERCAntenna(); } });

		// ------------------- NCC
		// mnemonic keys available: AB DEFGHIJKLMNOPQRS UVWXYZ
		MenuBar.Menu nccSubMenu = MenuBar.makeMenu("_NCC");
		toolMenu.add(nccSubMenu);
		nccSubMenu.addMenuItem("Schematic and Layout Views of Cell in _Current Window", null, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new com.sun.electric.tool.ncc.NccJob(1);
			}
		});
		nccSubMenu.addMenuItem("Cells from _Two Windows", null, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new com.sun.electric.tool.ncc.NccJob(2);
			}
		});

		//------------------- Network

		// mnemonic keys available: A  D F  IJK M O QRS   W YZ
		MenuBar.Menu networkSubMenu = MenuBar.makeMenu("Net_work");
		toolMenu.add(networkSubMenu);
		networkSubMenu.addMenuItem("Show _Network", KeyStroke.getKeyStroke('K', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { showNetworkCommand(); } });
		networkSubMenu.addMenuItem("_List Networks", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { listNetworksCommand(); } });
		networkSubMenu.addMenuItem("List _Connections on Network", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { listConnectionsOnNetworkCommand(); } });
		networkSubMenu.addMenuItem("List _Exports on Network", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { listExportsOnNetworkCommand(); } });
		networkSubMenu.addMenuItem("List Exports _below Network", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { listExportsBelowNetworkCommand(); } });
		networkSubMenu.addMenuItem("List _Geometry on Network", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { listGeometryOnNetworkCommand(GeometryHandler.ALGO_SWEEP); } });
        networkSubMenu.addMenuItem("List _Total Wire Lengths on All Networks", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { listGeomsAllNetworksCommand(); }});

        networkSubMenu.addSeparator();
		
        networkSubMenu.addMenuItem("E_xtract Current Cell", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { Connectivity.extractCurCell(false); }});
        networkSubMenu.addMenuItem("Extract Current _Hierarchy", null,
                new ActionListener() { public void actionPerformed(ActionEvent e) { Connectivity.extractCurCell(true); }});

        networkSubMenu.addSeparator();

        networkSubMenu.addMenuItem("Show _Power and Ground", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { showPowerAndGround(); } });
		networkSubMenu.addMenuItem("_Validate Power and Ground", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { validatePowerAndGround(); } });
		networkSubMenu.addMenuItem("Redo Network N_umbering", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { NetworkTool.renumberNetlists(); } });

		//------------------- Logical Effort

		// mnemonic keys available:    DEFGHIJK M   QRSTUVWXYZ
		MenuBar.Menu logEffortSubMenu = MenuBar.makeMenu("_Logical Effort");
        toolMenu.add(logEffortSubMenu);
		logEffortSubMenu.addMenuItem("_Optimize for Equal Gate Delays", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { optimizeEqualGateDelaysCommand(true); }});
		logEffortSubMenu.addMenuItem("Optimize for Equal Gate Delays (no _caching)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { optimizeEqualGateDelaysCommand(false); }});
		logEffortSubMenu.addMenuItem("_Print Info for Selected Node", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { printLEInfoCommand(); }});
        logEffortSubMenu.addMenuItem("_Back Annotate Wire Lengths for Current Cell", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { backAnnotateCommand(); }});
		logEffortSubMenu.addMenuItem("Clear Sizes on Selected _Node(s)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { clearSizesNodableCommand(); }});
		logEffortSubMenu.addMenuItem("Clear Sizes in _all Libraries", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { clearSizesCommand(); }});
		logEffortSubMenu.addSeparator();
		logEffortSubMenu.addMenuItem("_Load Logical Effort Libraries (Purple, Red, and Orange)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { loadLogicalEffortLibraries(); }});

		//------------------- Routing

		// mnemonic keys available:  B D FG IJKL  OPQ    V XY 
		MenuBar.Menu routingSubMenu = MenuBar.makeMenu("_Routing");
		toolMenu.add(routingSubMenu);
		routingSubMenu.addCheckBox("Enable _Auto-Stitching", Routing.isAutoStitchOn(), null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Routing.toggleEnableAutoStitching(e); } });
		routingSubMenu.addMenuItem("Auto-_Stitch Now", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { AutoStitch.autoStitch(false, true); }});
		routingSubMenu.addMenuItem("Auto-Stitch _Highlighted Now", KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { AutoStitch.autoStitch(true, true); }});

		routingSubMenu.addSeparator();

		routingSubMenu.addCheckBox("Enable _Mimic-Stitching", Routing.isMimicStitchOn(), null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Routing.toggleEnableMimicStitching(e); }});
		routingSubMenu.addMenuItem("Mimic-Stitch _Now", KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { MimicStitch.mimicStitch(true); }});
		routingSubMenu.addMenuItem("Mimic S_elected", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Routing.getRoutingTool().mimicSelected(); }});

		routingSubMenu.addSeparator();

		routingSubMenu.addMenuItem("Ma_ze Route", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Maze.mazeRoute(); }});

		routingSubMenu.addSeparator();

		routingSubMenu.addMenuItem("_River-Route", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { River.riverRoute(); }});

		routingSubMenu.addSeparator();

		routingSubMenu.addMenuItem("_Unroute", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Routing.unrouteCurrent(); }});
		routingSubMenu.addMenuItem("Get Unrouted _Wire", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { getUnroutedArcCommand(); }});
		routingSubMenu.addMenuItem("_Copy Routing Topology", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Routing.copyRoutingTopology(); }});
		routingSubMenu.addMenuItem("Pas_te Routing Topology", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Routing.pasteRoutingTopology(); }});

		//------------------- Generation

		// mnemonic keys available: AB DEFGHIJK  NO Q   UVWXYZ
		MenuBar.Menu generationSubMenu = MenuBar.makeMenu("_Generation");
		toolMenu.add(generationSubMenu);
		generationSubMenu.addMenuItem("_Coverage Implants Generator", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) {layerCoverageCommand(Job.Type.CHANGE,
                    LayerCoverageJob.IMPLANT, GeometryHandler.ALGO_SWEEP);}});
		generationSubMenu.addMenuItem("_Pad Frame Generator...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { padFrameGeneratorCommand(); }});
		generationSubMenu.addMenuItem("_ROM Generator...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ROMGenerator.generateROM(); }});
		generationSubMenu.addMenuItem("MOSIS CMOS P_LA Generator...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { PLA.generate(); }});
		generationSubMenu.addMenuItem("Generate gate layouts (_MoCMOS)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { new com.sun.electric.tool.generator.layout.GateLayoutGenerator(MoCMOS.tech, Tech.MOCMOS); }});
        generationSubMenu.addMenuItem("Generate gate layouts (T_SMC180)", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { new com.sun.electric.tool.generator.layout.GateLayoutGenerator(MoCMOS.tech, Tech.TSMC180); }});
        if (Technology.getTSMC90Technology() != null)
	        generationSubMenu.addMenuItem("Generate gate layouts (_TSMC90)", null,
	            new ActionListener() { public void actionPerformed(ActionEvent e) { new com.sun.electric.tool.generator.layout.GateLayoutGenerator(Technology.getTSMC90Technology(), Tech.TSMC90); }});

		//------------------- Silicon Compiler

		// mnemonic keys available: AB DEFGHIJKLM OPQRSTUVWXYZ
		MenuBar.Menu silCompSubMenu = MenuBar.makeMenu("Silicon Co_mpiler");
		toolMenu.add(silCompSubMenu);
		silCompSubMenu.addMenuItem("_Convert Current Cell to Layout", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { doSiliconCompilation(null); }});
		silCompSubMenu.addSeparator();
		silCompSubMenu.addMenuItem("Compile VHDL to _Netlist View", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { compileVHDL();}});

		//------------------- Compaction

		// mnemonic keys available: AB DEFGHIJKLMNOPQRSTUVWXYZ
		MenuBar.Menu compactionSubMenu = MenuBar.makeMenu("_Compaction");
		toolMenu.add(compactionSubMenu);
		compactionSubMenu.addMenuItem("Do _Compaction", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Compaction.compactNow(null);}});

        //------------------- Others

		toolMenu.addSeparator();

		toolMenu.addMenuItem("List _Tools",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { listToolsCommand(); } });

		// mnemonic keys available: ABCDEFGHIJKLMNOPQ STUVWXYZ
		MenuBar.Menu languagesSubMenu = MenuBar.makeMenu("Lang_uages");
		languagesSubMenu.addMenuItem("_Run Java Bean Shell Script", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { javaBshScriptCommand(); }});
		toolMenu.add(languagesSubMenu);

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
		curEdit.setCell(curEdit.getCell(), VarContext.globalContext);

		// optimize cell for equal gate delays
        if (curEdit.getCell() == null) {
            System.out.println("No current cell");
            return;
        }
		letool.optimizeEqualGateDelays(curEdit.getCell(), curEdit.getVarContext(), curEdit, newAlg);
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
		for (Iterator it = highlighter.getHighlights().iterator(); it.hasNext();) {
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.EOBJ) continue;

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
     * Method to kick area coverage per layer in a cell. It has to be public due to regressions.
     * @param cell
     * @param mode
     * @param startJob to determine if job has to run in a separate thread
     * @return true if job runs without errors. Only valid if startJob is false (regression purpose)
     */
    public static boolean layerCoverageCommand(Cell cell, int mode, boolean startJob)
    {
        Cell curCell = cell;

        if (curCell == null ) curCell = WindowFrame.needCurCell();
        if (curCell == null) return false;
	    EditWindow wnd = EditWindow.needCurrent();
	    Highlighter highlighter = null;
	    if ((wnd != null) && (wnd.getCell() == curCell))
		    highlighter = wnd.getHighlighter();

        double width = LayerCoverage.getWidth(curCell.getTechnology());
        double height = LayerCoverage.getHeight(curCell.getTechnology());
        double deltaX = LayerCoverage.getDeltaX(curCell.getTechnology());
        double deltaY = LayerCoverage.getDeltaY(curCell.getTechnology());

        // Reset values to cell bounding box if area is bigger than the actual cell
        Rectangle2D bbox = curCell.getBounds();
        if (width > bbox.getWidth()) width = bbox.getWidth();
        if (height > bbox.getHeight()) height = bbox.getHeight();
        LayerCoverage.AreaCoverage job = new LayerCoverage.AreaCoverage(curCell, highlighter, mode, width, height,
                deltaX, deltaY);

        // No regression
        if (startJob)
            job.startJob();
        else
            job.doIt();
        return (job.isOK());
    }

    /**
     * Method to handle the "List Layer Coverage", "Coverage Implant Generator",  polygons merge
     * except "List Geometry on Network" commands.
     */
    public static void layerCoverageCommand(Job.Type jobType, int func, int mode)
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;
	    EditWindow wnd = EditWindow.needCurrent();
	    Highlighter highlighter = null;
	    if ((wnd != null) && (wnd.getCell() == curCell))
		    highlighter = wnd.getHighlighter();
                                     ;
        Job job = new LayerCoverageJob(null, jobType, curCell, func, mode, highlighter, null, null);
        job.startJob();
    }

    private static class BackAnnotateJob extends Job {
        private Cell cell;
        public BackAnnotateJob(Cell cell) {
            super("BackAnnotate", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cell = cell;
        }

        public boolean doIt() {
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
            NccResult result = Ncc.compare(schLayCells[0], null, schLayCells[1], null, options, this);
            if (result == null || !result.topologyMatch()) {
                System.out.println("Ncc failed, can't back-annotate");
                return false;
            }

            // find all wire models in schematic
            int wiresUpdated = 0;
            ArrayList networks = new ArrayList();
            HashMap map = new HashMap();        // map of networks to associated wire model nodeinst
            for (Iterator it = schLayCells[0].getNodes(); it.hasNext(); ) {
                NodeInst ni = (NodeInst)it.next();
                Variable var = ni.getVar("ATTR_LEWIRE");
                if (var == null) continue;
                var = ni.getVar("ATTR_L");
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
            for (Iterator it = networks.iterator(); it.hasNext(); ) {
                Network schNet = (Network)it.next();
//                Netlist netlist = schLayCells[0].getNetlist(true);
                // find equivalent network in layouy
                NetEquivalence equiv = result.getNetEquivalence();
                HierarchyEnumerator.NetNameProxy proxy = equiv.findEquivalent(VarContext.globalContext, schNet);
                if (proxy == null) {
                    System.out.println("No matching network in layout for "+proxy.toString()+", ignoring");
                    continue;
                }
                Network layNet = proxy.getNet();
                Cell netcell = layNet.getParent();
                // get wire length
                HashSet nets = new HashSet();
                nets.add(layNet);
                //LayerCoverageJob.GeometryOnNetwork geoms = LayerCoverageJob.listGeometryOnNetworks(schLayCells[1], nets,
                LayerCoverageJob.GeometryOnNetwork geoms = LayerCoverageJob.listGeometryOnNetworks(netcell, nets,
                        false, GeometryHandler.ALGO_QTREE);
                double length = geoms.getTotalWireLength();

                // update wire length
                NodeInst ni = (NodeInst)map.get(schNet);
                ni.updateVar("ATTR_L", new Double(length));
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
		for (Iterator it = highlighter.getHighlights().iterator(); it.hasNext();) {
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.EOBJ) continue;

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
		/*for (Library lib: Library.getVisibleLibraries()) LETool.clearStoredSizesJob(lib);*/
		for (Iterator it = Library.getVisibleLibraries().iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			LETool.clearStoredSizesJob(lib);
		}
		System.out.println("Sizes cleared");
	}

	public static void loadLogicalEffortLibraries()
	{
		if (Library.findLibrary("purpleGeneric180") != null) return;
		URL url = LibFile.getLibFile("purpleGeneric180.jelib");
		FileMenu.ReadLibrary job = new FileMenu.ReadLibrary(url, FileType.JELIB, null);
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

		Set nets = highlighter.getHighlightedNetworks();
		//highlighter.clear();
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
//		Netlist netlist = cell.getUserNetlist();
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted netlist display (network information unavailable).  Please try again");
			return;
		}
		int total = 0;
		for(Iterator it = netlist.getNetworks(); it.hasNext(); )
		{
			Network net = (Network)it.next();
			String netName = net.describe(false);
			if (netName.length() == 0) continue;
			StringBuffer infstr = new StringBuffer();
			infstr.append("'" + netName + "'");
//			if (net->buswidth > 1)
//			{
//				formatinfstr(infstr, _(" (bus with %d signals)"), net->buswidth);
//			}
			boolean connected = false;
			for(Iterator aIt = net.getArcs(); aIt.hasNext(); )
			{
				ArcInst ai = (ArcInst)aIt.next();
				if (!connected)
				{
					connected = true;
					infstr.append(", on arcs:");
				}
				infstr.append(" " + ai.describe(true));
			}

			boolean exported = false;
			for(Iterator eIt = net.getExports(); eIt.hasNext(); )
			{
				Export pp = (Export)eIt.next();
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

        Set nets = highlighter.getHighlightedNetworks();
//        Netlist netlist = cell.getUserNetlist();
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted query (network information unavailable).  Please try again");
			return;
		}
        for(Iterator it = nets.iterator(); it.hasNext(); )
        {
            Network net = (Network)it.next();
            System.out.println("Network " + net.describe(true) + ":");

            int total = 0;
            for(Iterator nIt = netlist.getNodables(); nIt.hasNext(); )
            {
                Nodable no = (Nodable)nIt.next();
                NodeProto np = no.getProto();

                HashMap portNets = new HashMap();
                for(Iterator pIt = np.getPorts(); pIt.hasNext(); )
                {
                    PortProto pp = (PortProto)pIt.next();
                    if (pp instanceof PrimitivePort && ((PrimitivePort)pp).isIsolated())
                    {
                        NodeInst ni = (NodeInst)no;
                        for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
                        {
                            Connection con = (Connection)cIt.next();
                            ArcInst ai = con.getArc();
                            Network oNet = netlist.getNetwork(ai, 0);
                            HashSet ports = (HashSet)portNets.get(oNet);
                            if (ports == null) {
                                ports = new HashSet();
                                portNets.put(oNet, ports);
                            }
                            ports.add(pp);
//                           portNets.put(oNet, pp);
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
                            HashSet ports = (HashSet)portNets.get(oNet);
                            if (ports == null) {
                                ports = new HashSet();
                                portNets.put(oNet, ports);
                            }
                            ports.add(pp);
//                            portNets.put(oNet, pp);
                        }
                    }
                }

                // if there is only 1 net connected, the node is unimportant
                if (portNets.size() <= 1) continue;
                HashSet ports = (HashSet)portNets.get(net);
                if (ports == null) continue;
//                PortProto pp = (PortProto)portNets.get(net);
//                if (pp == null) continue;

                if (total == 0) System.out.println("  Connects to:");
                String name = null;
                if (no instanceof NodeInst) name = ((NodeInst)no).describe(false); else
                {
                    name = no.getName();
                }
                for (Iterator pIt = ports.iterator(); pIt.hasNext(); ) {
                    PortProto pp = (PortProto)pIt.next();
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

        Set nets = highlighter.getHighlightedNetworks();
//        Netlist netlist = cell.getUserNetlist();
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted query (network information unavailable).  Please try again");
			return;
		}
        for(Iterator it = nets.iterator(); it.hasNext(); )
        {
            Network net = (Network)it.next();
            System.out.println("Network '" + net.describe(true) + "':");

            // find all exports on network "net"
            HashSet/*<Export>*/ listedExports = new HashSet/*<Export>*/();
            System.out.println("  Going up the hierarchy from " + cell + ":");
            if (findPortsUp(netlist, net, cell, listedExports)) break;
            System.out.println("  Going down the hierarchy from " + cell + ":");
            if (findPortsDown(netlist, net, cell, listedExports)) break;
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

        Set nets = highlighter.getHighlightedNetworks();
//        Netlist netlist = cell.getUserNetlist();
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted query (network information unavailable).  Please try again");
			return;
		}
        for(Iterator it = nets.iterator(); it.hasNext(); )
        {
            Network net = (Network)it.next();
            System.out.println("Network " + net.describe(true) + ":");

            // find all exports on network "net"
            if (findPortsDown(netlist, net, cell, new HashSet())) break;
        }
    }

    /**
     * helper method for "telltool network list-hierarchical-ports" to print all
     * ports connected to net "net" in cell "cell", and recurse up the hierarchy.
     * @return true if an error occurred.
     */
    private static boolean findPortsUp(Netlist netlist, Network net, Cell cell, HashSet/*<Export>*/ listedExports)
    {
        // look at every node in the cell
        for(Iterator it = cell.getPorts(); it.hasNext(); )
        {
            Export pp = (Export)it.next();
            int width = netlist.getBusWidth(pp);
            for(int i=0; i<width; i++)
            {
                Network ppNet = netlist.getNetwork(pp, i);
                if (ppNet != net) continue;
                if (listedExports.contains(pp)) continue;
                listedExports.add(pp);
//                listedExports.add(listedExports);
                System.out.println("    Export " + pp.getName() + " in " + cell);

                // code to find the proper instance
                Cell instanceCell = cell.iconView();
                if (instanceCell == null) instanceCell = cell;

                // ascend to higher cell and continue
                for(Iterator uIt = instanceCell.getUsagesOf(); uIt.hasNext(); )
                {
                    CellUsage u = (CellUsage)uIt.next();
                    Cell superCell = u.getParent();
//                    Netlist superNetlist = superCell.getUserNetlist();
            		Netlist superNetlist = cell.acquireUserNetlist();
            		if (superNetlist == null)
            		{
            			System.out.println("Sorry, a deadlock aborted query (network information unavailable).  Please try again");
            			return true;
            		}
                    for(Iterator nIt = superNetlist.getNodables(); nIt.hasNext(); )
                    {
                        Nodable no = (Nodable)nIt.next();
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
    private static boolean findPortsDown(Netlist netlist, Network net, Cell cell, HashSet/*<Export>*/ listedExports)
    {
        // look at every node in the cell
        for(Iterator it = netlist.getNodables(); it.hasNext(); )
        {
            Nodable no = (Nodable)it.next();

            // only want complex nodes
            NodeProto subnp = no.getProto();
            if (!(subnp instanceof Cell)) continue;
            Cell subCell = (Cell)subnp;

            // look at all wires connected to the node
            for(Iterator pIt = subCell.getPorts(); pIt.hasNext(); )
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
//                    Netlist subNetlist = subCell.getUserNetlist();
            		Netlist subNetlist = subCell.acquireUserNetlist();
            		if (subNetlist == null)
            		{
            			System.out.println("Sorry, a deadlock aborted query (network information unavailable).  Please try again");
            			return true;
            		}
                    Network subNet = subNetlist.getNetwork(pp, i);
                    if (findPortsDown(subNetlist, subNet, subCell, listedExports)) return true;
                }
            }
        }
        return false;
    }

    /**
     * Method to handle the "List Geometry On Network" command.
     */
    public static void listGeometryOnNetworkCommand(int mode)
    {
	    Cell cell = WindowFrame.needCurCell();
        if (cell == null) return;
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;

        HashSet nets = (HashSet)wnd.getHighlighter().getHighlightedNetworks();
        if (nets.isEmpty())
        {
            System.out.println("No network in " + cell + " selected");
            return;
        }
	    else
            LayerCoverageJob.listGeometryOnNetworks(cell, nets, true, mode);
    }

    public static void listGeomsAllNetworksCommand() {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Cell cell = wnd.getCell();
        if (cell == null) return;
        ListGeomsAllNetworksJob job = new ListGeomsAllNetworksJob(cell);
    }

    private static class ListGeomsAllNetworksJob extends Job {
        private Cell cell;
        public ListGeomsAllNetworksJob(Cell cell) {
            super("ListGeomsAllNetworks", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.cell = cell;
            startJob();
        }

        public boolean doIt() {
            Netlist netlist = cell.getNetlist(true);
            ArrayList networks = new ArrayList();
            for (Iterator it = netlist.getNetworks(); it.hasNext(); ) {
                networks.add(it.next());
            }
            // sort list of networks by name
            Collections.sort(networks, new TextUtils.NetworksByName());
            for (Iterator it = networks.iterator(); it.hasNext(); ) {
                Network net = (Network)it.next();
                HashSet nets = new HashSet();
                nets.add(net);
                LayerCoverageJob.GeometryOnNetwork geoms = LayerCoverageJob.listGeometryOnNetworks(cell, nets,
                        false, GeometryHandler.ALGO_QTREE);
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
        HashSet pAndG = new HashSet();
        for(Iterator it = cell.getPorts(); it.hasNext(); )
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
        for(Iterator it = cell.getNodes(); it.hasNext(); )
        {
            NodeInst ni = (NodeInst)it.next();
            PrimitiveNode.Function fun = ni.getFunction();
            if (fun != PrimitiveNode.Function.CONPOWER && fun != PrimitiveNode.Function.CONGROUND)
                continue;
            for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
            {
                Connection con = (Connection)cIt.next();
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
        for(Iterator it = pAndG.iterator(); it.hasNext(); )
        {
            Network net = (Network)it.next();
            highlighter.addNetwork(net, cell);
        }
        highlighter.finished();
        if (pAndG.size() == 0)
            System.out.println("This cell has no Power or Ground networks");
    }

    public static void validatePowerAndGround()
    {
        System.out.println("Validating power and ground networks");
        int total = 0;
        for(Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
        {
            Library lib = (Library)lIt.next();
            for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
            {
                Cell cell = (Cell)cIt.next();
                for(Iterator pIt = cell.getPorts(); pIt.hasNext(); )
                {
                    Export pp = (Export)pIt.next();
                    if (pp.isNamedGround() && pp.getCharacteristic() != PortCharacteristic.GND)
                    {
                        System.out.println("Cell " + cell.describe(true) + ", export " + pp.getName() +
                            ": does not have 'GROUND' characteristic");
                        total++;
                    }
                    if (pp.isNamedPower() && pp.getCharacteristic() != PortCharacteristic.PWR)
                    {
                        System.out.println("Cell " + cell.describe(true) + ", export " + pp.getName() +
                            ": does not have 'POWER' characteristic");
                        total++;
                    }
                }
            }
        }
        if (total == 0) System.out.println("No problems found"); else
            System.out.println("Found " + total + " export problems");
    }

    /**
     * Method to add a "M" multiplier factor to the currently selected node.
     */
    public static void addMultiplierCommand()
    {
        Cell cell = WindowFrame.needCurCell();
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

        NodeInst ni = (NodeInst)highlighter.getOneElectricObject(NodeInst.class);
        if (ni == null) return;
        AddMultiplier job = new AddMultiplier(ni);
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

        public boolean doIt()
        {
            Variable var = ni.newDisplayVar(Simulation.M_FACTOR_KEY, new Double(1.0));
            if (var != null)
            {
//                var.setDisplay(true);
                var.setOff(-1.5, -1);
                var.setDispPart(TextDescriptor.DispPos.NAMEVALUE);
            }
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
        MakeTemplate job = new MakeTemplate(templateKey);
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

        public boolean doIt()
        {
            Cell cell = WindowFrame.needCurCell();
            if (cell == null) return false;
            Variable templateVar = cell.getVar(templateKey);
            if (templateVar != null)
            {
                System.out.println("This cell already has a template");
                return false;
            }
            templateVar = cell.newDisplayVar(templateKey, "*Undefined");
            if (templateVar != null)
            {
//                templateVar.setDisplay(true);
                templateVar.setInterior(true);
                templateVar.setDispPart(TextDescriptor.DispPos.NAMEVALUE);
                System.out.println("Set "+templateKey.getName().replaceFirst("ATTR_", "")+" for "+cell);
            }
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
            PadGenerator.makePadFrame(fileName);
        }
    }

    public static void listToolsCommand()
    {
        System.out.println("Tools in Electric:");
        for(Iterator it = Tool.getTools(); it.hasNext(); )
        {
            Tool tool = (Tool)it.next();
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

	private static final String SCLIBNAME = "sclib";
	private static final int READ_LIBRARY         =  1;
	private static final int CONVERT_TO_VHDL      =  2;
	private static final int COMPILE_VHDL_FOR_SC  =  4;
	private static final int PLACE_AND_ROUTE      =  8;
	private static final int SHOW_CELL            = 16;

	/**
	 * Method to handle the menu command to convert the current cell to layout.
	 * Reads the cell library if necessary;
	 * Converts a schematic to VHDL if necessary;
	 * Compiles a VHDL cell to a netlist if necessary;
	 * Reads the netlist from the cell;
	 * does placement and routing;
	 * Generates Electric layout;
	 * Displays the resulting layout.
	 * @param completion runnable to invoke when the compilation has finished.
	 */
	public static void doSiliconCompilation(Job completion)
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		int activities = PLACE_AND_ROUTE | SHOW_CELL;

		// see if the current cell needs to be compiled
		Cell originalCell = cell;
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
		if (SilComp.getCellLib() == null)
		{
			Library lib = Library.findLibrary(SCLIBNAME);
			if (lib != null) SilComp.setCellLib(lib); else
				activities |= READ_LIBRARY;
		}
	    DoNextActivity sJob = new DoNextActivity(cell, activities, originalCell, null, completion);
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
	    DoNextActivity sJob = new DoNextActivity(cell, COMPILE_VHDL_FOR_SC | SHOW_CELL, cell, null, null);
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
	    DoNextActivity sJob = new DoNextActivity(cell, CONVERT_TO_VHDL | SHOW_CELL, cell, null, null);
	}

	/**
	 * Class to do the next silicon-compilation activity in a new thread.
	 */
	private static class DoNextActivity extends Job
	{
		private Cell cell, originalCell;
		private VarContext originalContext;
		private int activities;
		private Job completion;

		private DoNextActivity(Cell cell, int activities, Cell originalCell, VarContext originalContext, Job completion)
		{
			super("Silicon-Compiler activity", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.activities = activities;
			this.originalCell = originalCell;
			this.originalContext = originalContext;
			this.completion = completion;
			startJob();
		}

		public boolean doIt()
		{
			if ((activities&READ_LIBRARY) != 0)
			{
				// read standard cell library
				System.out.println("Reading Standard Cell Library '" + SCLIBNAME + "' ...");
				URL fileURL = LibFile.getLibFile(SCLIBNAME + ".jelib");
				Library lib = LibraryFiles.readLibrary(fileURL, null, FileType.JELIB, false);
		        Undo.noUndoAllowed();
				if (lib != null) SilComp.setCellLib(lib);
				System.out.println(" Done");
			    DoNextActivity sJob = new DoNextActivity(cell, activities & ~READ_LIBRARY, originalCell, originalContext, completion);
			    return true;
			}

			if ((activities&CONVERT_TO_VHDL) != 0)
			{
				// convert Schematic to VHDL
				System.out.print("Generating VHDL from " + cell + " ...");
				List vhdlStrings = GenerateVHDL.convertCell(cell);
				if (vhdlStrings == null)
				{
					System.out.println("No VHDL produced");
					return false;
				}

				String cellName = cell.getName() + "{vhdl}";
				Cell vhdlCell = cell.getLibrary().findNodeProto(cellName);
				if (vhdlCell == null)
				{
					vhdlCell = Cell.makeInstance(cell.getLibrary(), cellName);
					if (vhdlCell == null) return false;
				}
				String [] array = new String[vhdlStrings.size()];
				for(int i=0; i<vhdlStrings.size(); i++) array[i] = (String)vhdlStrings.get(i);
				vhdlCell.setTextViewContents(array);
				System.out.println(" Done, created " + vhdlCell);
			    DoNextActivity sJob = new DoNextActivity(vhdlCell, activities & ~CONVERT_TO_VHDL, originalCell, originalContext, completion);
			    return true;
			}

			if ((activities&COMPILE_VHDL_FOR_SC) != 0)
			{
				// compile the VHDL to a netlist
				System.out.print("Compiling VHDL in " + cell + " ...");
				CompileVHDL c = new CompileVHDL(cell);
				if (c.hasErrors())
				{
					System.out.println("ERRORS during compilation, no netlist produced");
					return false;
				}
				List netlistStrings = c.getQUISCNetlist();
				if (netlistStrings == null)
				{
					System.out.println("No netlist produced");
					return false;
				}

				// store the QUISC netlist
				String cellName = cell.getName() + "{net.quisc}";
				Cell netlistCell = cell.getLibrary().findNodeProto(cellName);
				if (netlistCell == null)
				{
					netlistCell = Cell.makeInstance(cell.getLibrary(), cellName);
					if (netlistCell == null) return false;
				}
				String [] array = new String[netlistStrings.size()];
				for(int i=0; i<netlistStrings.size(); i++) array[i] = (String)netlistStrings.get(i);
				netlistCell.setTextViewContents(array);
				System.out.println(" Done, created " + netlistCell);
			    DoNextActivity sJob = new DoNextActivity(netlistCell, activities & ~COMPILE_VHDL_FOR_SC, originalCell, originalContext, completion);
			    return true;
			}

			if ((activities&PLACE_AND_ROUTE) != 0)
			{
				// first grab the information in the netlist
				System.out.print("Reading netlist in " + cell + " ...");
				GetNetlist gnl = new GetNetlist();
				if (gnl.readNetCurCell(cell)) { System.out.println();   return false; }
				System.out.println(" Done");

				// do the placement
				System.out.print("Placing cells ...");
				Place place = new Place();
				String err = place.placeCells(gnl);
				if (err != null)
				{
					System.out.println("\n" + err);
					return false;
				}
				System.out.println(" Done");

				// do the routing
				System.out.print("Routing cells ...");
				Route route = new Route();
				err = route.routeCells(gnl);
				if (err != null)
				{
					System.out.println("\n" + err);
					return false;
				}
				System.out.println(" Done");

				// generate the results
				System.out.print("Generating layout ...");
				Maker maker = new Maker();
				Object result = maker.makeLayout(gnl);
				if (result instanceof String)
				{
					System.out.println("\n" + (String)result);
					if (Technology.getCurrent() == Schematics.tech)
						System.out.println("Should switch to a layout technology first (currently in Schematics)");
					return false;
				}
				if (result instanceof Cell)
				{
					Cell newCell = (Cell)result;
					System.out.println(" Done, created " + newCell);
				    DoNextActivity sJob = new DoNextActivity(newCell, activities & ~PLACE_AND_ROUTE, originalCell, originalContext, completion);
				}
				System.out.println();
			    return true;
			}

			if ((activities&SHOW_CELL) != 0)
			{
				// show the cell
				WindowFrame.createEditWindow(cell);
			    DoNextActivity sJob = new DoNextActivity(cell, activities & ~SHOW_CELL, originalCell, originalContext, completion);
				return true;
			}

			if (completion != null)
			{
				completion.startJob();
			}
			return false;
		}
	}

    /****************** Parasitic Tool ********************/
    public static void parasiticCommand()
    {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Cell cell = wnd.getCell();
        Highlighter highlighter = wnd.getHighlighter();

        Set nets = highlighter.getHighlightedNetworks();
        for (Iterator it = nets.iterator(); it.hasNext();)
        {
            Network net = (Network)it.next();
            ParasiticTool.getParasiticTool().netwokParasitic(net, cell);
        }
    }

    public static void importAssuraDrcErrors() {
        String fileName = OpenFile.chooseInputFile(FileType.ERR, null);
        if (fileName == null) return;
        AssuraDrcErrors.importErrors(fileName);
    }    

    public static void importCalibreDrcErrors() {
        String fileName = OpenFile.chooseInputFile(FileType.DB, null);
        if (fileName == null) return;
        CalibreDrcErrors.importErrors(fileName);
    }
}
