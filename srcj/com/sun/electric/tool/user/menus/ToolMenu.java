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

import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.dialogs.FastHenryArc;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.input.Simulate;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.io.output.Verilog;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.erc.ERCWellCheck;
import com.sun.electric.tool.compaction.Compaction;
import com.sun.electric.tool.erc.ERCAntenna;
import com.sun.electric.tool.routing.Routing;
import com.sun.electric.tool.routing.River;
import com.sun.electric.tool.routing.Maze;
import com.sun.electric.tool.routing.AutoStitch;
import com.sun.electric.tool.routing.MimicStitch;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.NccResult;
import com.sun.electric.tool.ncc.NetEquivalence;
import com.sun.electric.tool.parasitic.ParasiticTool;
import com.sun.electric.tool.generator.PadGenerator;
import com.sun.electric.tool.generator.ROMGenerator;
import com.sun.electric.tool.generator.layout.Tech;
import com.sun.electric.tool.logicaleffort.LETool;
import com.sun.electric.database.variable.*;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.PolyQTree;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.plugins.tsmc90.TSMC90;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.util.*;

/**
 * Class to handle the commands in the "Tool" pulldown menu.
 */
public class ToolMenu {

	protected static void addToolMenu(MenuBar menuBar) {
		MenuBar.MenuItem m;
		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		/****************************** THE TOOL MENU ******************************/

		MenuBar.Menu toolMenu = new MenuBar.Menu("Tool", 'T');
		menuBar.add(toolMenu);

		//------------------- DRC

		MenuBar.Menu drcSubMenu = new MenuBar.Menu("DRC", 'D');
		toolMenu.add(drcSubMenu);
		drcSubMenu.addMenuItem("Check Hierarchically", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { DRC.checkHierarchically(false); }});
		drcSubMenu.addMenuItem("Check Selection Area Hierarchically", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { DRC.checkHierarchically(true); }});

		//------------------- Simulation (SPICE)

		MenuBar.Menu spiceSimulationSubMenu = new MenuBar.Menu("Simulation (Spice)", 'S');
		toolMenu.add(spiceSimulationSubMenu);
		spiceSimulationSubMenu.addMenuItem("Write Spice Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCellCommand(OpenFile.Type.SPICE, true); }});
		spiceSimulationSubMenu.addMenuItem("Write CDL Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCellCommand(OpenFile.Type.CDL, true); }});
		spiceSimulationSubMenu.addMenuItem("Plot Spice Listing...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulate.plotSpiceResults(); }});
		spiceSimulationSubMenu.addMenuItem("Plot Spice for This Cell", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulate.plotSpiceResultsThisCell(); }});
		spiceSimulationSubMenu.addMenuItem("Set Spice Model...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.setSpiceModel(); }});
		spiceSimulationSubMenu.addMenuItem("Add Multiplier", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { addMultiplierCommand(); }});

		spiceSimulationSubMenu.addSeparator();

		spiceSimulationSubMenu.addMenuItem("Set Generic Spice Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_TEMPLATE_KEY); }});
		spiceSimulationSubMenu.addMenuItem("Set Spice 2 Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_2_TEMPLATE_KEY); }});
		spiceSimulationSubMenu.addMenuItem("Set Spice 3 Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_3_TEMPLATE_KEY); }});
		spiceSimulationSubMenu.addMenuItem("Set HSpice Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_H_TEMPLATE_KEY); }});
		spiceSimulationSubMenu.addMenuItem("Set PSpice Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_P_TEMPLATE_KEY); }});
		spiceSimulationSubMenu.addMenuItem("Set GnuCap Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_GC_TEMPLATE_KEY); }});
		spiceSimulationSubMenu.addMenuItem("Set SmartSpice Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_SM_TEMPLATE_KEY); }});

		//------------------- Simulation (Verilog)

		MenuBar.Menu verilogSimulationSubMenu = new MenuBar.Menu("Simulation (Verilog)", 'V');
		toolMenu.add(verilogSimulationSubMenu);
		verilogSimulationSubMenu.addMenuItem("Write Verilog Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCellCommand(OpenFile.Type.VERILOG, true); } });
		verilogSimulationSubMenu.addMenuItem("Plot Verilog VCD Dump...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulate.plotVerilogResults(); }});
		verilogSimulationSubMenu.addMenuItem("Plot Verilog for This Cell", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulate.plotVerilogResultsThisCell(); }});
		verilogSimulationSubMenu.addSeparator();
		verilogSimulationSubMenu.addMenuItem("Set Verilog Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Verilog.VERILOG_TEMPLATE_KEY); }});

		verilogSimulationSubMenu.addSeparator();

		MenuBar.Menu verilogWireTypeSubMenu = new MenuBar.Menu("Set Verilog Wire", 'W');
		verilogWireTypeSubMenu.addMenuItem("Wire", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.setVerilogWireCommand(0); }});
		verilogWireTypeSubMenu.addMenuItem("Trireg", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.setVerilogWireCommand(1); }});
		verilogWireTypeSubMenu.addMenuItem("Default", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.setVerilogWireCommand(2); }});
		verilogSimulationSubMenu.add(verilogWireTypeSubMenu);

		MenuBar.Menu transistorStrengthSubMenu = new MenuBar.Menu("Transistor Strength", 'T');
		transistorStrengthSubMenu.addMenuItem("Weak", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.setTransistorStrengthCommand(true); }});
		transistorStrengthSubMenu.addMenuItem("Normal", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.setTransistorStrengthCommand(false); }});
		verilogSimulationSubMenu.add(transistorStrengthSubMenu);

		//------------------- Simulation (others)

		MenuBar.Menu netlisters = new MenuBar.Menu("Simulation (Others)");
		toolMenu.add(netlisters);
		netlisters.addMenuItem("Write Maxwell Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCellCommand(OpenFile.Type.MAXWELL, true); } });
		netlisters.addMenuItem("Write Tegas Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCellCommand(OpenFile.Type.TEGAS, true); } });
		netlisters.addMenuItem("Write SILOS Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCellCommand(OpenFile.Type.SILOS, true); }});
		netlisters.addMenuItem("Write PAL Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCellCommand(OpenFile.Type.PAL, true); } });
		netlisters.addSeparator();
		netlisters.addMenuItem("Write IRSIM Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCellCommand(OpenFile.Type.IRSIM, true); }});
		netlisters.addMenuItem("Write ESIM/RNL Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCellCommand(OpenFile.Type.ESIM, true); }});
		netlisters.addMenuItem("Write RSIM Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCellCommand(OpenFile.Type.RSIM, true); }});
		netlisters.addMenuItem("Write COSMOS Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCellCommand(OpenFile.Type.COSMOS, true); }});
		netlisters.addMenuItem("Write MOSSIM Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCellCommand(OpenFile.Type.MOSSIM, true); }});
		netlisters.addSeparator();
		netlisters.addMenuItem("Write FastHenry Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.exportCellCommand(OpenFile.Type.FASTHENRY, true); }});
		netlisters.addMenuItem("FastHenry Arc Properties...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FastHenryArc.showFastHenryArcDialog(); }});

		//------------------- ERC

		MenuBar.Menu ercSubMenu = new MenuBar.Menu("ERC", 'E');
		toolMenu.add(ercSubMenu);
		ercSubMenu.addMenuItem("Check Wells", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ERCWellCheck.analyzeCurCell(false); } });
		ercSubMenu.addMenuItem("Antenna Check", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { new ERCAntenna(); } });

		// ------------------- NCC
		MenuBar.Menu nccSubMenu = new MenuBar.Menu("NCC", 'N');
		toolMenu.add(nccSubMenu);
		nccSubMenu.addMenuItem("schematic and layout views of cell in current window", null, 38, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new com.sun.electric.tool.ncc.NccJob(1);
			}
		});
		nccSubMenu.addMenuItem("cells from two windows", null, 11, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new com.sun.electric.tool.ncc.NccJob(2);
			}
		});
		nccSubMenu.addMenuItem("test probing", null, 0, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new com.sun.electric.tool.ncc.NccJob(-1);
			}
		});

		//------------------- Network

		MenuBar.Menu networkSubMenu = new MenuBar.Menu("Network", 'e');
		toolMenu.add(networkSubMenu);
		networkSubMenu.addMenuItem("Show Network", KeyStroke.getKeyStroke('K', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { showNetworkCommand(); } });
		networkSubMenu.addMenuItem("List Networks", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { listNetworksCommand(); } });
		networkSubMenu.addMenuItem("List Connections on Network", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { listConnectionsOnNetworkCommand(); } });
		networkSubMenu.addMenuItem("List Exports on Network", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { listExportsOnNetworkCommand(); } });
		networkSubMenu.addMenuItem("List Exports below Network", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { listExportsBelowNetworkCommand(); } });
		networkSubMenu.addMenuItem("List Geometry on Network", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { listGeometryOnNetworkCommand(); } });
        networkSubMenu.addMenuItem("List Total Wire Lengths on All Networks", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { listGeomsAllNetworksCommand(); }});
		networkSubMenu.addSeparator();
		networkSubMenu.addMenuItem("Show Power and Ground", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { showPowerAndGround(); } });
		networkSubMenu.addMenuItem("Validate Power and Ground", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { validatePowerAndGround(); } });
		networkSubMenu.addMenuItem("Redo Network Numbering", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { new NetworkTool.RenumberJob(); } });

		//------------------- Logical Effort

		MenuBar.Menu logEffortSubMenu = new MenuBar.Menu("Logical Effort", 'L');
		logEffortSubMenu.addMenuItem("Optimize for Equal Gate Delays", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { optimizeEqualGateDelaysCommand(true); }});
		logEffortSubMenu.addMenuItem("Optimize for Equal Gate Delays (no caching)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { optimizeEqualGateDelaysCommand(false); }});
		logEffortSubMenu.addMenuItem("Print Info for Selected Node", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { printLEInfoCommand(); }});
        logEffortSubMenu.addMenuItem("Back Annotate Wire Lengths for Current Cell", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { backAnnotateCommand(); }});
		logEffortSubMenu.addMenuItem("Clear Sizes on Selected Node(s)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { clearSizesNodableCommand(); }});
		logEffortSubMenu.addMenuItem("Clear Sizes in all Libraries", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { clearSizesCommand(); }});
		toolMenu.add(logEffortSubMenu);

		//------------------- Routing

		MenuBar.Menu routingSubMenu = new MenuBar.Menu("Routing", 'R');
		toolMenu.add(routingSubMenu);
		routingSubMenu.addCheckBox("Enable Auto-Stitching", Routing.isAutoStitchOn(), null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Routing.toggleEnableAutoStitching(e); } });
		routingSubMenu.addMenuItem("Auto-Stitch Now", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { AutoStitch.autoStitch(false, true); }});
		routingSubMenu.addMenuItem("Auto-Stitch Highlighted Now", KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { AutoStitch.autoStitch(true, true); }});

		routingSubMenu.addSeparator();

		routingSubMenu.addCheckBox("Enable Mimic-Stitching", Routing.isMimicStitchOn(), null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Routing.toggleEnableMimicStitching(e); }});
		routingSubMenu.addMenuItem("Mimic-Stitch Now", KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { MimicStitch.mimicStitch(true); }});
		routingSubMenu.addMenuItem("Mimic Selected", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Routing.tool.mimicSelected(); }});

		routingSubMenu.addSeparator();

		routingSubMenu.addMenuItem("Maze Route", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Maze.mazeRoute(); }});

		routingSubMenu.addSeparator();

		routingSubMenu.addMenuItem("River-Route", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { River.riverRoute(); }});

		routingSubMenu.addSeparator();

		routingSubMenu.addMenuItem("Unroute", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Routing.unrouteCurrent(); }});
		routingSubMenu.addMenuItem("Get Unrouted Wire", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { getUnroutedArcCommand(); }});
		routingSubMenu.addMenuItem("Copy Routing Topology", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Routing.copyRoutingTopology(); }});
		routingSubMenu.addMenuItem("Paste Routing Topology", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Routing.pasteRoutingTopology(); }});

		//------------------- Generation

		MenuBar.Menu generationSubMenu = new MenuBar.Menu("Generation", 'G');
		toolMenu.add(generationSubMenu);
		generationSubMenu.addMenuItem("Coverage Implants Generator", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) {layerCoverageCommand(Job.Type.CHANGE, LayerCoverageJob.IMPLANT, false);}});
		generationSubMenu.addMenuItem("Pad Frame Generator", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { padFrameGeneratorCommand(); }});
		generationSubMenu.addMenuItem("ROM Generator...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ROMGenerator.generateROM(); }});
		generationSubMenu.addMenuItem("Generate gate layouts (MoCMOS)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { new com.sun.electric.tool.generator.layout.GateLayoutGenerator(MoCMOS.tech); }});
        generationSubMenu.addMenuItem("Generate gate layouts (TSMC90)", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { new com.sun.electric.tool.generator.layout.GateLayoutGenerator(TSMC90.tech); }});

		//------------------- Compaction

		MenuBar.Menu compactionSubMenu = new MenuBar.Menu("Compaction", 'C');
		toolMenu.add(compactionSubMenu);
		compactionSubMenu.addMenuItem("Do Compaction", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Compaction.compactNow();}});

		toolMenu.addSeparator();

		toolMenu.addMenuItem("List Tools",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { listToolsCommand(); } });
		MenuBar.Menu languagesSubMenu = new MenuBar.Menu("Languages");
		languagesSubMenu.addMenuItem("Run Java Bean Shell Script", null,
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

    public static class BackAnnotateJob extends Job {
        private Cell cell;
        public BackAnnotateJob(Cell cell) {
            super("BackAnnotate", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cell = cell;
        }

        public boolean doIt() {
            Cell[] schLayCells = NccUtils.findSchematicAndLayout(cell);
            if (schLayCells == null) {
                System.out.println("Could not find schematic and layout cells for "+cell.describe());
                return false;
            }
            if (cell.getView() == View.LAYOUT) {
                // check if layout cells match, if not, replace
                schLayCells[1] = cell;
            }

            // run NCC, get results
            NccOptions options = new NccOptions();
            NccResult result = Ncc.compare(schLayCells[0], null, schLayCells[1], null, options);
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
                    System.out.println("No attribute L on wire model "+ni.describe()+", ignoring it.");
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
                Netlist netlist = schLayCells[0].getNetlist(true);
                // find equivalent network in layouy
                NetEquivalence equiv = result.getNetEquivalence();
                HierarchyEnumerator.NetNameProxy proxy = equiv.findEquivalent(VarContext.globalContext, schNet, 0);
                if (proxy == null) {
                    System.out.println("No matching network in layout for "+proxy.toString()+", ignoring");
                    continue;
                }
                Network layNet = proxy.getNet();
                // get wire length
                HashSet nets = new HashSet();
                nets.add(layNet);
                GeometryOnNetwork geoms = listGeometryOnNetworks(schLayCells[1], nets);
                double length = geoms.getTotalWireLength();

                // update wire length
                NodeInst ni = (NodeInst)map.get(schNet);
                ni.updateVar("ATTR_L", new Double(length));
                wiresUpdated++;
                System.out.println("Updated wire model "+ni.getName()+" on network "+proxy.toString()+" to: "+length+" lambda");
            }
            System.out.println("Updated "+wiresUpdated+" wire models in "+schLayCells[0].describe()+" from layout "+schLayCells[1].describe());
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
		for (Iterator it = Library.getVisibleLibraries(); it.hasNext(); ) {
			Library lib = (Library)it.next();
			LETool.clearStoredSizesJob(lib);
		}
		System.out.println("Sizes cleared");
	}

	/**
	 * Method to handle the "Show Network" command.
	 */
	public static void showNetworkCommand()
	{
		EditWindow wnd = EditWindow.needCurrent();
		Cell cell = wnd.getCell();
		if (wnd == null) return;
		Highlighter highlighter = wnd.getHighlighter();

		Set nets = highlighter.getHighlightedNetworks();
		//highlighter.clear();
		highlighter.showNetworks(nets, cell.getUserNetlist(), cell);
		highlighter.finished();
	}

	/**
	 * Method to handle the "List Networks" command.
	 */
	public static void listNetworksCommand()
	{
		Cell cell = WindowFrame.getCurrentCell();
		if (cell == null) return;
		Netlist netlist = cell.getUserNetlist();
		int total = 0;
		for(Iterator it = netlist.getNetworks(); it.hasNext(); )
		{
			Network net = (Network)it.next();
			String netName = net.describe();
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
				infstr.append(" " + ai.describe());
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
        Netlist netlist = cell.getUserNetlist();
        for(Iterator it = nets.iterator(); it.hasNext(); )
        {
            Network net = (Network)it.next();
            System.out.println("Network '" + net.describe() + "':");

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
                            portNets.put(oNet, pp);
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
                            portNets.put(oNet, pp);
                        }
                    }
                }

                // if there is only 1 net connected, the node is unimportant
                if (portNets.size() <= 1) continue;
                PortProto pp = (PortProto)portNets.get(net);
                if (pp == null) continue;

                if (total == 0) System.out.println("  Connects to:");
                String name = null;
                if (no instanceof NodeInst) name = ((NodeInst)no).describe(); else
                {
                    name = no.getName();
                }
                System.out.println("    Node " + name + ", port " + pp.getName());
                total++;
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
        Netlist netlist = cell.getUserNetlist();
        for(Iterator it = nets.iterator(); it.hasNext(); )
        {
            Network net = (Network)it.next();
            System.out.println("Network '" + net.describe() + "':");

            // find all exports on network "net"
            HashSet listedExports = new HashSet();
            System.out.println("  Going up the hierarchy from cell " + cell.describe() + ":");
            findPortsUp(netlist, net, cell, listedExports);
            System.out.println("  Going down the hierarchy from cell " + cell.describe() + ":");
            findPortsDown(netlist, net, cell, listedExports);
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
        Netlist netlist = cell.getUserNetlist();
        for(Iterator it = nets.iterator(); it.hasNext(); )
        {
            Network net = (Network)it.next();
            System.out.println("Network '" + net.describe() + "':");

            // find all exports on network "net"
            findPortsDown(netlist, net, cell, new HashSet());
        }
    }

    /**
     * helper method for "telltool network list-hierarchical-ports" to print all
     * ports connected to net "net" in cell "cell", and recurse up the hierarchy
     */
    private static void findPortsUp(Netlist netlist, Network net, Cell cell, HashSet listedExports)
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
                listedExports.add(listedExports);
                System.out.println("    Export " + pp.getName() + " in cell " + cell.describe());

                // code to find the proper instance
                Cell instanceCell = cell.iconView();
                if (instanceCell == null) instanceCell = cell;

                // ascend to higher cell and continue
                for(Iterator uIt = instanceCell.getUsagesOf(); uIt.hasNext(); )
                {
                    NodeUsage nu = (NodeUsage)uIt.next();
                    Cell superCell = nu.getParent();
                    Netlist superNetlist = superCell.getUserNetlist();
                    for(Iterator nIt = superNetlist.getNodables(); nIt.hasNext(); )
                    {
                        Nodable no = (Nodable)nIt.next();
                        if (no.getProto() != cell) continue;
                        Network superNet = superNetlist.getNetwork(no, pp, i);
                        findPortsUp(superNetlist, superNet, superCell, listedExports);
                    }
                }
            }
        }
    }

    /**
     * helper method for "telltool network list-hierarchical-ports" to print all
     * ports connected to net "net" in cell "cell", and recurse down the hierarchy
     */
    private static void findPortsDown(Netlist netlist, Network net, Cell cell, HashSet listedExports)
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
                    System.out.println("    Export " + pp.getName() + " in cell " + subCell.describe());
                    Netlist subNetlist = subCell.getUserNetlist();
                    Network subNet = subNetlist.getNetwork(pp, i);
                    findPortsDown(subNetlist, subNet, subCell, listedExports);
                }
            }
        }
    }

    /**
     * Method to handle the "List Geometry On Network" command.
     */
    public static void listGeometryOnNetworkCommand()
    {
	    Cell cell = WindowFrame.needCurCell();
        if (cell == null) return;
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

        HashSet nets = (HashSet)highlighter.getHighlightedNetworks();
        if (nets.isEmpty())
        {
            System.out.println("No network in cell '" + cell.describe() + "' selected");
            return;
        }
        GeometryOnNetwork geoms = listGeometryOnNetworks(cell, nets);
        geoms.print();
    }

    public static void listGeomsAllNetworksCommand() {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Cell cell = wnd.getCell();
        if (cell == null) return;
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
            ToolMenu.GeometryOnNetwork geoms = ToolMenu.listGeometryOnNetworks(cell, nets);
            if (geoms.getTotalWireLength() == 0) continue;
            System.out.println("Network "+net+" has wire length "+geoms.getTotalWireLength());
        }
    }

    public static GeometryOnNetwork listGeometryOnNetworks(Cell cell, HashSet nets) {
        if (cell == null || nets == null || nets.isEmpty()) return null;
	    Netlist netlist = ((Network)nets.iterator().next()).getNetlist();

	    long startTime = System.currentTimeMillis();
        PolyQTree tree = new PolyQTree(cell.getBounds());
	    LayerCoverageJob.LayerVisitor visitor = new LayerCoverageJob.LayerVisitor(true, tree, null, LayerCoverageJob.NETWORK, null, nets);
	    HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, netlist, visitor);

        double lambda = 1; // lambdaofcell(np);
        GeometryOnNetwork geoms = new GeometryOnNetwork(cell, nets, lambda);
        geoms.setStartTime(startTime);

		// Traversing tree with merged geometry
		for (Iterator it = tree.getKeyIterator(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			Collection set = tree.getObjects(layer, false, true);
			double layerArea = 0;
			double perimeter = 0;

			// Get all objects and sum the area
			for (Iterator i = set.iterator(); i.hasNext(); )
			{
				PolyQTree.PolyNode area = (PolyQTree.PolyNode)i.next();
				layerArea += area.getArea();
				perimeter += area.getPerimeter();
			}
			layerArea /= lambda;
			perimeter /= 2;

            geoms.addLayer(layer, layerArea, perimeter);
		}
	    long endTime = System.currentTimeMillis();
        geoms.setEndTime(endTime);
        return geoms;

		// @TODO GVG lambda and ratio ==0 (when is the case?)
    }

    public static class GeometryOnNetwork {
        public final Cell cell;
        private Set nets;
        private double lambda;
        private long startTime;
        private long endTime;

        // these lists tie together a layer, its area, and its half-perimeter
        private ArrayList layers;
        private ArrayList areas;
        private ArrayList halfPerimeters;
        private double totalWire;

        private GeometryOnNetwork(Cell cell, Set nets, double lambda) {
            this.cell = cell;
            this.nets = nets;
            this.lambda = lambda;
            layers = new ArrayList();
            areas = new ArrayList();
            halfPerimeters = new ArrayList();
            totalWire = 0;
            startTime = endTime = 0;
        }
        private void setStartTime(long startTime) { this.startTime = startTime; }
        private void setEndTime(long endTime) { this.endTime = endTime; }
        public double getTotalWireLength() { return totalWire; }

        private void addLayer(Layer layer, double area, double halfperimeter) {
            layers.add(layer);
            areas.add(new Double(area));
            halfPerimeters.add(new Double(halfperimeter));

            Layer.Function func = layer.getFunction();
            /* accumulate total wire length on all metal/poly layers */
            if (func.isPoly() && !func.isGatePoly() || func.isMetal()) {
                System.out.println(func.toString()+": "+halfperimeter);
                totalWire += halfperimeter;
            }
        }

        public void print() {
            for(Iterator it = nets.iterator(); it.hasNext(); )
            {
                Network net = (Network)it.next();
                System.out.println("For network '" + net.describe() + "' in cell '" + cell.describe() + "':");
            }
            for (int i=0; i<layers.size(); i++) {
                Layer layer = (Layer)layers.get(i);
                Double area = (Double)areas.get(i);
                Double halfperim = (Double)halfPerimeters.get(i);

                System.out.println("Layer " + layer.getName()
                        + ":\t area " + TextUtils.formatDouble(area.doubleValue())
                        + "\t half-perimeter " + TextUtils.formatDouble(halfperim.doubleValue())
                        + "\t ratio " + TextUtils.formatDouble(area.doubleValue()/halfperim.doubleValue()));
            }
            if (totalWire > 0)
                System.out.println("Total wire length = " + TextUtils.formatDouble(totalWire/lambda) +
                        " (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
        }
    }

    public static void showPowerAndGround()
    {
        Cell cell = WindowFrame.needCurCell();
        if (cell == null) return;
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

        Netlist netlist = cell.getUserNetlist();
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
                        System.out.println("Cell " + cell.describe() + ", export " + pp.getName() +
                            ": does not have 'GROUND' characteristic");
                        total++;
                    }
                    if (pp.isNamedPower() && pp.getCharacteristic() != PortCharacteristic.PWR)
                    {
                        System.out.println("Cell " + cell.describe() + ", export " + pp.getName() +
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
            super("Add Spice Multiplier", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.ni = ni;
            startJob();
        }

        public boolean doIt()
        {
            Variable var = ni.newVar("ATTR_M", new Double(1.0));
            if (var != null)
            {
                var.setDisplay(true);
                TextDescriptor td = var.getTextDescriptor();
                td.setOff(-1.5, -1);
                td.setDispPart(TextDescriptor.DispPos.NAMEVALUE);
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
            super("Make template", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
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
            templateVar = cell.newVar(templateKey, "*Undefined");
            if (templateVar != null)
            {
                templateVar.setDisplay(true);
                TextDescriptor td = templateVar.getTextDescriptor();
                td.setInterior(true);
                td.setDispPart(TextDescriptor.DispPos.NAMEVALUE);
                System.out.println("Set "+templateKey.getName().replaceFirst("ATTR_", "")+" for cell "+cell.describe());
            }
            return true;
        }
    }

    public static void getUnroutedArcCommand()
    {
        User.tool.setCurrentArcProto(Generic.tech.unrouted_arc);
    }

    public static void padFrameGeneratorCommand()
    {
        String fileName = OpenFile.chooseInputFile(OpenFile.Type.PADARR, null);
        if (fileName != null)
        {
            PadGenerator.generate(fileName);
        }
    }

    /**
     * Method to handle the "List Layer Coverage" command.
     */
    public static void layerCoverageCommand(Job.Type jobType, int func, boolean test)
    {
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return;
        Job job = new LayerCoverageJob(jobType, curCell, func, test);
    }

    protected static class LayerCoverageJob extends Job
    {
        private Cell curCell;
        private boolean testCase;
        private PolyQTree tree; // = new PolyQTree(curCell.getBounds());
        public final static int AREA = 0;   // function Layer Coverage
        public final static int MERGE = 1;  // Generic merge polygons function
        public final static int IMPLANT = 2; // Coverage implants
	    public final static int NETWORK = 3; // List Geometry on Network function
        private final int function;
        private java.util.List deleteList; // Only used for coverage Implants. New coverage implants are pure primitive nodes
	    private HashMap originalPolygons = new HashMap(); // Storing initial nodes

        public static class LayerVisitor extends HierarchyEnumerator.Visitor
        {
            private boolean testCase;
            private PolyQTree tree;
            private java.util.List deleteList; // Only used for coverage Implants. New coverage implants are pure primitive nodes
            private final int function;
            private HashMap originalPolygons;
	        private Set netSet; // For network type, rest is null

	        /**
	         * Determines if function of given layer is applicable for the corresponding operation
	         * @param func
	         * @param function
	         * @param testCase
	         * @return
	         */
	        private static boolean IsValidFunction(Layer.Function func, int function, boolean testCase)
	        {
		        if (testCase) return (true);
				switch (function)
				{
					case IMPLANT:
						return (func.isSubstrate());
					case AREA:
						return (func.isPoly() || func.isMetal());
					default:
						return (false);
				}
	        }

            public LayerVisitor(boolean test, PolyQTree t, java.util.List delList, int func, HashMap original, Set netSet)
            {
                this.testCase = test;
                this.tree = t;
                this.deleteList = delList;
                this.function = func;
	            this.originalPolygons = original;
	            this.netSet = netSet;
            }
            public void exitCell(HierarchyEnumerator.CellInfo info)
            {
            }

            public boolean enterCell(HierarchyEnumerator.CellInfo info)
            {
                Cell curCell = info.getCell();
	            Netlist netlist = info.getNetlist();
	            // @TODO GVG put node checking into visitorNode

	            // Checking if any network is found
				boolean found = (netSet == null);
				for (Iterator it = netlist.getNetworks(); !found && it.hasNext(); )
				{
					Network aNet = (Network)it.next();
					Network parentNet = aNet;
					HierarchyEnumerator.CellInfo cinfo = info;
					boolean netFound = false;
					while ((netFound = netSet.contains(parentNet)) == false && cinfo.getParentInst() != null) {
						parentNet = cinfo.getNetworkInParent(parentNet);
//						parentNet = HierarchyEnumerator.getNetworkInParent(parentNet, cinfo.getParentInst());
						cinfo = cinfo.getParentInfo();
					}
					found = netFound;
				}
				if (!found) return (false);

                // Traversing arcs
                for (Iterator it = curCell.getArcs(); it.hasNext(); )
                {
                    ArcInst arc = (ArcInst)it.next();
	                int width = netlist.getBusWidth(arc);
					found = (netSet == null);

					for (int i=0; !found && i<width; i++)
					{
						Network parentNet = netlist.getNetwork(arc, i);
						HierarchyEnumerator.CellInfo cinfo = info;
						boolean netFound = false;
						while ((netFound = netSet.contains(parentNet)) == false && cinfo.getParentInst() != null) {
							parentNet = cinfo.getNetworkInParent(parentNet);
//							parentNet = HierarchyEnumerator.getNetworkInParent(parentNet, cinfo.getParentInst());
							cinfo = cinfo.getParentInfo();
						}
		                found = netFound;
					}
					if (!found) continue; // skipping this arc
                    ArcProto arcType = arc.getProto();
                    Technology tech = arcType.getTechnology();
                    Poly[] polyList = tech.getShapeOfArc(arc);

                    // Treating the arcs associated to each node
                    // Arcs don't need to be rotated
                    for (int i = 0; i < polyList.length; i++)
                    {
                        Poly poly = polyList[i];
                        Layer layer = poly.getLayer();
                        Layer.Function func = layer.getFunction();

	                    boolean value = IsValidFunction(func, function, testCase);
                        if (!value) continue;

	                    poly.transform(info.getTransformToRoot());
						PolyQTree.PolyNode pnode = new PolyQTree.PolyNode(poly);
						if (function == IMPLANT)
						{
							// For coverage implants
							Map map = (Map)originalPolygons.get(layer);
							if (map == null)
							{
								map = new HashMap();
								originalPolygons.put(layer, map);
							}
							map.put(pnode, pnode.clone());
						}
	                    //if (layer.getName().equals("Metal-1"))
                        tree.add(layer, pnode, false/*function==NETWORK*/);  // tmp fix
                    }
                }

	            // Skipping actual nodes
	            //if (function == NETWORK) return (true);

                // Traversing nodes
                // This function should be moved to visitNodeInst
                for (Iterator it = curCell.getNodes(); it.hasNext(); )
                {
                    NodeInst node = (NodeInst)it.next();

	                found = (netSet == null);

	                for(Iterator pIt = node.getPortInsts(); !found && pIt.hasNext(); )
					{
						PortInst pi = (PortInst)pIt.next();
						PortProto subPP = pi.getPortProto();
						Network oNet = info.getNetlist().getNetwork(node, subPP, 0);
						Network parentNet = oNet;
						HierarchyEnumerator.CellInfo cinfo = info;
						boolean netFound = false;
						while ((netFound = netSet.contains(parentNet)) == false && cinfo.getParentInst() != null) {
							parentNet = cinfo.getNetworkInParent(parentNet);
//							parentNet = HierarchyEnumerator.getNetworkInParent(parentNet, cinfo.getParentInst());
							cinfo = cinfo.getParentInfo();
						}
		                found = netFound;
					}
					if (!found) continue; // skipping this node

                    // Coverage implants are pure primitive nodes
                    // and they are ignored.
                    if (!testCase && node.isPrimtiveSubstrateNode()) //node.getFunction() == PrimitiveNode.Function.NODE)
                    {
                        deleteList.add(node);
                        continue;
                    }

                    NodeProto protoType = node.getProto();

	                // Skip Facet-Center
	                if (protoType == Tech.facetCenter)
	                    continue;

                    //  Analyzing only leaves
                    if (!(protoType instanceof Cell))
                    {
                        Technology tech = protoType.getTechnology();
                        Poly[] polyList = tech.getShapeOfNode(node);
                        AffineTransform transform = node.rotateOut();

                        for (int i = 0; i < polyList.length; i++)
                        {
                            Poly poly = polyList[i];
                            Layer layer = poly.getLayer();
                            Layer.Function func = layer.getFunction();

                            // Only checking poly or metal for AREA case
                            boolean value = IsValidFunction(func, function, testCase);
                            if (!value) continue;

	                        if (poly.getPoints().length < 3)
	                        {
		                        // When is this happening?
		                        continue;
	                        }

                            poly.transform(transform);
                            // Not sure if I need this for general merge polygons function
                            poly.transform(info.getTransformToRoot());

	                        PolyQTree.PolyNode pnode = new PolyQTree.PolyNode(poly);
	                        if (function == IMPLANT)
	                        {
								// For coverage implants
								Map map = (Map)originalPolygons.get(layer);
								if (map == null)
								{
									map = new HashMap();
									originalPolygons.put(layer, map);
								}
		                        map.put(pnode, pnode.clone());
	                        }
	                        //if (layer.getName().equals("Metal-1"))
                            tree.add(layer, pnode, false/*function==NETWORK*/);
                        }
                    }
                }
                return (true);
            }
            public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
            {
	            // @todo GVG Migrate functionality here from enterCell
                return (true);
            }
        }

        protected LayerCoverageJob(Type jobType, Cell cell, int func, boolean test)
        {
            super("Layer Coverage", User.tool, jobType, null, null, Job.Priority.USER);
            this.curCell = cell;
            this.testCase = test;
            this.tree = new PolyQTree(curCell.getBounds());
            this.function = func;
            this.deleteList = new ArrayList(); // should only be used by IMPLANT

            setReportExecutionFlag(true);
            startJob();
        }

        public boolean doIt()
        {
            // enumerate the hierarchy below here
            LayerVisitor visitor = new LayerVisitor(testCase, tree, deleteList, function, originalPolygons, null);
            HierarchyEnumerator.enumerateCell(curCell, VarContext.globalContext, null, visitor);

            switch (function)
            {
                case AREA:
                    {
                        double lambdaSqr = 1;
                        // @todo GVG Calculates lambda!
                        Rectangle2D bbox = curCell.getBounds();
                        double totalArea =  (bbox.getHeight()*bbox.getWidth())/lambdaSqr;

                        // Traversing tree with merged geometry
                        for (Iterator it = tree.getKeyIterator(); it.hasNext(); )
                        {
                            Layer layer = (Layer)it.next();
                            Collection set = tree.getObjects(layer, false, true);
                            double layerArea = 0;

                            // Get all objects and sum the area
                            for (Iterator i = set.iterator(); i.hasNext(); )
                            {
                                PolyQTree.PolyNode area = (PolyQTree.PolyNode)i.next();
                                layerArea += area.getArea();
                            }
                            System.out.println("Layer " + layer.getName() + " covers " + TextUtils.formatDouble(layerArea) + " square lambda (" + TextUtils.formatDouble((layerArea/totalArea)*100, 0) + "%)");
                        }

                        System.out.println("Cell is " + TextUtils.formatDouble(totalArea, 2) + " square lambda (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
                    }
                    break;
                case MERGE:
                case IMPLANT:
                    {
                        EditWindow wnd = EditWindow.needCurrent();
                        Highlighter highlighter = null;
                        if ((wnd != null) && (wnd.getCell() == curCell))
                            highlighter = wnd.getHighlighter();

                        // With polygons collected, new geometries are calculated
                        if (highlighter != null) highlighter.clear();
                        boolean noNewNodes = true;
                        boolean isMerge = (function == MERGE);

                        // Need to detect if geometry was really modified
                        for(Iterator it = tree.getKeyIterator(); it.hasNext(); )
                        {
                            Layer layer = (Layer)it.next();
                            Collection set = tree.getObjects(layer, !isMerge, true);

                            // Ready to create new implants.
                            for (Iterator i = set.iterator(); i.hasNext(); )
                            {
                                PolyQTree.PolyNode qNode = (PolyQTree.PolyNode)i.next();

	                            if (function == IMPLANT)
	                            {
		                            Map map = (Map)originalPolygons.get(layer);
		                            // One of the original elements
		                            if (map.containsValue(qNode))
			                            continue;
	                            }
                                Rectangle2D rect = qNode.getBounds2D();
                                Point2D center = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
                                PrimitiveNode priNode = layer.getPureLayerNode();
                                // Adding the new implant. New implant not assigned to any local variable                                .
                                NodeInst node = NodeInst.makeInstance(priNode, center, rect.getWidth(), rect.getHeight(), curCell);
                                if (highlighter != null)
                                    highlighter.addElectricObject(node, curCell);

                                if (isMerge)
                                {
                                    Point2D [] points = qNode.getPoints();
                                    node.newVar(NodeInst.TRACE, points);
                                }
                                else
                                {
                                    // New implant can't be selected again
                                    node.setHardSelect();
                                }
                                noNewNodes = false;
                            }
                        }
                        if (highlighter != null) highlighter.finished();
                        for (Iterator it = deleteList.iterator(); it.hasNext(); )
                        {
                            NodeInst node = (NodeInst)it .next();
                            node.kill();
                        }
                        if (noNewNodes)
                            System.out.println("No new areas added");

                    }
                    break;
                default:
                    System.out.println("Error in LayerCoverageJob: function not implemented");
            }
            return true;
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
        String fileName = OpenFile.chooseInputFile(OpenFile.Type.JAVA, null);
        if (fileName != null)
        {
            // start a job to run the script
            EvalJavaBsh.runScript(fileName);
        }
    }

    /****************** Parasitic Tool ********************/
    public static void parasiticCommand()
    {
        EditWindow wnd = EditWindow.needCurrent();
        Cell cell = wnd.getCell();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

        Set nets = highlighter.getHighlightedNetworks();
        for (Iterator it = nets.iterator(); it.hasNext();)
        {
            Network net = (Network)it.next();
            ParasiticTool.getParasiticTool().netwokParasitic(net, cell);
        }
    }
}
