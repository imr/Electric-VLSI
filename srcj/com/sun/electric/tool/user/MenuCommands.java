/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MenuCommands.java
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
package com.sun.electric.tool.user;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.hierarchy.NodeUsage;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.Input;
import com.sun.electric.tool.io.Output;
import com.sun.electric.tool.user.Clipboard;
import com.sun.electric.tool.user.ErrorLog;
import com.sun.electric.tool.user.ui.Menu;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.dialogs.About;
import com.sun.electric.tool.user.dialogs.Array;
import com.sun.electric.tool.user.dialogs.Attributes;
import com.sun.electric.tool.user.dialogs.CellLists;
import com.sun.electric.tool.user.dialogs.CellOptions;
import com.sun.electric.tool.user.dialogs.CellParameters;
import com.sun.electric.tool.user.dialogs.Change;
import com.sun.electric.tool.user.dialogs.CrossLibCopy;
import com.sun.electric.tool.user.dialogs.EditOptions;
import com.sun.electric.tool.user.dialogs.EditKeyBindings;
import com.sun.electric.tool.user.dialogs.GetInfoArc;
import com.sun.electric.tool.user.dialogs.GetInfoExport;
import com.sun.electric.tool.user.dialogs.GetInfoMulti;
import com.sun.electric.tool.user.dialogs.GetInfoNode;
import com.sun.electric.tool.user.dialogs.GetInfoText;
import com.sun.electric.tool.user.dialogs.IOOptions;
import com.sun.electric.tool.user.dialogs.LayerVisibility;
import com.sun.electric.tool.user.dialogs.NewCell;
import com.sun.electric.tool.user.dialogs.NewExport;
import com.sun.electric.tool.user.dialogs.ToolOptions;
import com.sun.electric.tool.user.dialogs.ViewControl;
import com.sun.electric.tool.logicaleffort.LENetlister;
import com.sun.electric.tool.logicaleffort.LETool;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.generator.PadGenerator;
import com.sun.electric.tool.routing.AutoStitch;
import com.sun.electric.tool.simulation.Spice;
import com.sun.electric.tool.simulation.IRSIMTool;
import com.sun.electric.tool.io.Output;
//import com.sun.electric.tool.ncc.factory.NetFactory;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.JOptionPane;

/**
 * This class has all of the pulldown menu commands in Electric.
 */
public final class MenuCommands
{
	public static JMenuItem selectArea, selectObjects;
	public static JMenuItem moveFull, moveHalf, moveQuarter;
	public static JMenuItem cursorSelect, cursorWiring, cursorSelectSpecial, cursorPan, cursorZoom;

	// It is never useful for anyone to create an instance of this class
	private MenuCommands() {}

	/**
	 * Method to create the pulldown menus.
	 */
	public static JMenuBar createMenuBar()
	{
		// create the menu bar
		JMenuBar menuBar = new JMenuBar();

		/****************************** THE FILE MENU ******************************/

		Menu fileMenu = Menu.createMenu("File", 'F');
		menuBar.add(fileMenu);

		fileMenu.addMenuItem("Open", KeyStroke.getKeyStroke('O', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { openLibraryCommand(); } });
		Menu importSubMenu = Menu.createMenu("Import");
		fileMenu.add(importSubMenu);
		importSubMenu.addMenuItem("Readable Dump", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(); } });
        fileMenu.addMenuItem("Save", KeyStroke.getKeyStroke('S', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveLibraryCommand(); } });
		fileMenu.addMenuItem("Save as...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveAsLibraryCommand(); } });
        Menu exportSubMenu = Menu.createMenu("Export");
        fileMenu.add(exportSubMenu);
        exportSubMenu.addMenuItem("CIF (Caltech Intermediate Format)", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(Output.ExportType.CIF); } });
		if (TopLevel.getOperatingSystem() == TopLevel.OS.MACINTOSH)
		{
//			MRJApplicationUtils.registerQuitHandler(new MRJQuitHandler()
//			{
//				public void handleQuit() { quitCommand(); }
//			});
		} else
		{
			fileMenu.addSeparator();
			fileMenu.addMenuItem("Quit", KeyStroke.getKeyStroke('Q', InputEvent.CTRL_MASK),
				new ActionListener() { public void actionPerformed(ActionEvent e) { quitCommand(); } });
		}

		/****************************** THE EDIT MENU ******************************/

		Menu editMenu = Menu.createMenu("Edit", 'E');
		menuBar.add(editMenu);

		editMenu.addMenuItem("Undo", KeyStroke.getKeyStroke('Z', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { new UndoCommand(); } });
		editMenu.addMenuItem("Redo", KeyStroke.getKeyStroke('Y', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { new RedoCommand(); } });

		editMenu.addSeparator();

		editMenu.addMenuItem("Cut", KeyStroke.getKeyStroke('X', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { cutCommand(); } });
		editMenu.addMenuItem("Copy", KeyStroke.getKeyStroke('C', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { copyCommand(); } });
		editMenu.addMenuItem("Paste", KeyStroke.getKeyStroke('V', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { pasteCommand(); } });

		editMenu.addSeparator();

		Menu arcSubMenu = Menu.createMenu("Arc", 'A');
		editMenu.add(arcSubMenu);
        arcSubMenu.addMenuItem("Rigid", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { arcRigidCommand(); }});
        arcSubMenu.addMenuItem("Not Rigid", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { arcNotRigidCommand(); }});
        arcSubMenu.addMenuItem("Fixed Angle", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { arcFixedAngleCommand(); }});
        arcSubMenu.addMenuItem("Not Fixed Angle", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { arcNotFixedAngleCommand(); }});

		editMenu.addSeparator();

		editMenu.addMenuItem("I/O Options...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ioOptionsCommand(); } });
		editMenu.addMenuItem("Edit Options...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { editOptionsCommand(); } });
		editMenu.addMenuItem("Tool Options...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { toolOptionsCommand(); } });
        editMenu.addMenuItem("Key Bindings...",null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { keyBindingsCommand(); } });
		editMenu.addSeparator();

		editMenu.addMenuItem("Get Info...", KeyStroke.getKeyStroke('I', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { getInfoCommand(); } });
		editMenu.addMenuItem("Attributes...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { attributesCommand(); } });

		editMenu.addSeparator();
        
        Menu modeSubMenu = Menu.createMenu("Modes");
        editMenu.add(modeSubMenu);

		Menu modeSubMenuEdit = Menu.createMenu("Edit");
        modeSubMenu.add(modeSubMenuEdit);
        ButtonGroup editGroup = new ButtonGroup();
        cursorSelect = modeSubMenuEdit.addRadioButton("Select", true, editGroup, KeyStroke.getKeyStroke('M', 0),
            new ActionListener() { public void actionPerformed(ActionEvent e) { setEditModeCommand("Select"); } });
        cursorWiring = modeSubMenuEdit.addRadioButton("Wiring", false, editGroup, KeyStroke.getKeyStroke('W', 0),
            new ActionListener() { public void actionPerformed(ActionEvent e) { setEditModeCommand("Wiring"); } });
        cursorSelectSpecial = modeSubMenuEdit.addRadioButton("Special Select", false, editGroup, null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { setEditModeCommand("Special Select"); } });
        cursorPan = modeSubMenuEdit.addRadioButton("Pan", false, editGroup, KeyStroke.getKeyStroke('P', 0),
            new ActionListener() { public void actionPerformed(ActionEvent e) { setEditModeCommand("Pan"); } });
        cursorZoom = modeSubMenuEdit.addRadioButton("Zoom", false, editGroup, KeyStroke.getKeyStroke('Z', 0),
            new ActionListener() { public void actionPerformed(ActionEvent e) { setEditModeCommand("Zoom"); } });
		ToolBar.CursorMode cm = ToolBar.getCursorMode();
		if (cm == ToolBar.CursorMode.SELECT) cursorSelect.setSelected(true); else
		if (cm == ToolBar.CursorMode.WIRE) cursorWiring.setSelected(true); else
		if (cm == ToolBar.CursorMode.SELECTSPECIAL) cursorSelectSpecial.setSelected(true); else
		if (cm == ToolBar.CursorMode.PAN) cursorPan.setSelected(true); else
			cursorZoom.setSelected(true);

		Menu modeSubMenuMovement = Menu.createMenu("Movement");
        modeSubMenu.add(modeSubMenuMovement);
        ButtonGroup movementGroup = new ButtonGroup();
        moveFull = modeSubMenuMovement.addRadioButton("Full", true, movementGroup, KeyStroke.getKeyStroke('F', 0),
            new ActionListener() { public void actionPerformed(ActionEvent e) { setMovementModeCommand("Full"); } });
        moveHalf = modeSubMenuMovement.addRadioButton("Half", false, movementGroup, KeyStroke.getKeyStroke('H', 0),
            new ActionListener() { public void actionPerformed(ActionEvent e) { setMovementModeCommand("Half"); } });
        moveQuarter = modeSubMenuMovement.addRadioButton("Quarter", false, movementGroup, KeyStroke.getKeyStroke('Q', 0),
            new ActionListener() { public void actionPerformed(ActionEvent e) { setMovementModeCommand("Quarter"); } });
		double ad = ToolBar.getArrowDistance();
		if (ad == 1.0) moveFull.setSelected(true); else
		if (ad == 0.5) moveHalf.setSelected(true); else
			moveQuarter.setSelected(true);

		editMenu.addMenuItem("Array...", KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Array.showArrayDialog(); } });
		editMenu.addMenuItem("Change...", KeyStroke.getKeyStroke('C', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Change.showChangeDialog(); } });

		Menu modeSubMenuSelect = Menu.createMenu("Select");
        modeSubMenu.add(modeSubMenuSelect);
        ButtonGroup selectGroup = new ButtonGroup();
        selectArea = modeSubMenuSelect.addRadioButton("Area", true, selectGroup, null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { setSelectModeCommand("Area"); } });
        selectObjects = modeSubMenuSelect.addRadioButton("Objects", false, selectGroup, null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { setSelectModeCommand("Objects"); } });
		ToolBar.SelectMode sm = ToolBar.getSelectMode();
		if (sm == ToolBar.SelectMode.AREA) selectArea.setSelected(true); else
			selectObjects.setSelected(true);

		Menu selListSubMenu = Menu.createMenu("Selection");
		editMenu.add(selListSubMenu);
        selListSubMenu.addMenuItem("Select All", KeyStroke.getKeyStroke('A', InputEvent.CTRL_MASK), 
            new ActionListener() { public void actionPerformed(ActionEvent e) { selectAllCommand(); }});
        selListSubMenu.addMenuItem("Show Next Error", KeyStroke.getKeyStroke('>'), 
            new ActionListener() { public void actionPerformed(ActionEvent e) { showNextErrorCommand(); }});
        selListSubMenu.addMenuItem("Show Previous Error", KeyStroke.getKeyStroke('<'), 
            new ActionListener() { public void actionPerformed(ActionEvent e) { showPrevErrorCommand(); }});

		/****************************** THE CELL MENU ******************************/

		Menu cellMenu = Menu.createMenu("Cell", 'C');
		menuBar.add(cellMenu);

		cellMenu.addMenuItem("Cell Control...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { cellControlCommand(); }});
		cellMenu.addMenuItem("New Cell...", KeyStroke.getKeyStroke('N', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { newCellCommand(); } });
		cellMenu.addMenuItem("Delete Current Cell", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { deleteCellCommand(); } });
		cellMenu.addMenuItem("Cross-Library Copy...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { crossLibraryCopyCommand(); } });

		cellMenu.addSeparator();
 
		cellMenu.addMenuItem("Down Hierarchy", KeyStroke.getKeyStroke('D', InputEvent.CTRL_MASK),
            new ActionListener() { public void actionPerformed(ActionEvent e) { downHierCommand(); }});
        cellMenu.addMenuItem("Up Hierarchy", KeyStroke.getKeyStroke('U', InputEvent.CTRL_MASK),
            new ActionListener() { public void actionPerformed(ActionEvent e) { upHierCommand(); }});

		cellMenu.addSeparator();

		cellMenu.addMenuItem("New Version of Current Cell", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { newCellVersionCommand(); } });
		cellMenu.addMenuItem("Duplicate Current Cell", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { duplicateCellCommand(); } });
		cellMenu.addMenuItem("Delete Unused Old Versions", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { deleteOldCellVersionsCommand(); } });

		cellMenu.addSeparator();

		cellMenu.addMenuItem("Describe this Cell", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CellLists.describeThisCellCommand(); } });
		cellMenu.addMenuItem("General Cell Lists...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CellLists.generalCellListsCommand(); } });
		Menu specListSubMenu = Menu.createMenu("Special Cell Lists");
		cellMenu.add(specListSubMenu);
        specListSubMenu.addMenuItem("List Nodes in this Cell", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { CellLists.listNodesInCellCommand(); }});
        specListSubMenu.addMenuItem("List Cell Instances", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { CellLists.listCellInstancesCommand(); }});
        specListSubMenu.addMenuItem("List Cell Usage", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { CellLists.listCellUsageCommand(); }});
		cellMenu.addMenuItem("Cell Parameters...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { cellParametersCommand(); } });

		cellMenu.addSeparator();

		Menu expandListSubMenu = Menu.createMenu("Expand Cell Instances");
		cellMenu.add(expandListSubMenu);
        expandListSubMenu.addMenuItem("One Level Down", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { expandOneLevelDownCommand(); }});
        expandListSubMenu.addMenuItem("All the Way", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { expandFullCommand(); }});
        expandListSubMenu.addMenuItem("Specified Amount", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { expandSpecificCommand(); }});
		Menu unExpandListSubMenu = Menu.createMenu("Unexpand Cell Instances");
		cellMenu.add(unExpandListSubMenu);
        unExpandListSubMenu.addMenuItem("One Level Up", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { unexpandOneLevelUpCommand(); }});
        unExpandListSubMenu.addMenuItem("All the Way", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { unexpandFullCommand(); }});
        unExpandListSubMenu.addMenuItem("Specified Amount", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { unexpandSpecificCommand(); }});

		/****************************** THE EXPORT MENU ******************************/

		Menu exportMenu = Menu.createMenu("Export", 'X');
		menuBar.add(exportMenu);

		exportMenu.addMenuItem("Create Export...", KeyStroke.getKeyStroke('E', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { newExportCommand(); } });

		/****************************** THE VIEW MENU ******************************/

		Menu viewMenu = Menu.createMenu("View", 'V');
		menuBar.add(viewMenu);

		viewMenu.addMenuItem("View Control...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { viewControlCommand(); } });

		viewMenu.addSeparator();

		viewMenu.addMenuItem("Edit Layout View", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { editLayoutViewCommand(); } });
		viewMenu.addMenuItem("Edit Schematic View", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { editSchematicViewCommand(); } });
		viewMenu.addMenuItem("Edit Icon View", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { editIconViewCommand(); } });
		viewMenu.addMenuItem("Edit VHDL View", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { editVHDLViewCommand(); } });
		viewMenu.addMenuItem("Edit Documentation View", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { editDocViewCommand(); } });
		viewMenu.addMenuItem("Edit Skeleton View", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { editSkeletonViewCommand(); } });
		viewMenu.addMenuItem("Edit Other View", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { editOtherViewCommand(); } });

		viewMenu.addSeparator();

		viewMenu.addMenuItem("Make Icon View", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.makeIconViewCommand(); } });

		/****************************** THE WINDOW MENU ******************************/

		Menu windowMenu = Menu.createMenu("Window", 'W');
		menuBar.add(windowMenu);

		windowMenu.addMenuItem("Fill Display", KeyStroke.getKeyStroke('9', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { fullDisplayCommand(); } });
		windowMenu.addMenuItem("Zoom Out", KeyStroke.getKeyStroke('0', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { zoomOutDisplayCommand(); } });
		windowMenu.addMenuItem("Zoom In", KeyStroke.getKeyStroke('7', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { zoomInDisplayCommand(); } });
		windowMenu.addMenuItem("Focus on Highlighted", KeyStroke.getKeyStroke('F', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { focusOnHighlightedCommand(); } });

		windowMenu.addSeparator();

		windowMenu.addMenuItem("Toggle Grid", KeyStroke.getKeyStroke('G', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { toggleGridCommand(); } });

		windowMenu.addSeparator();

		windowMenu.addMenuItem("Layer Visibility...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { layerVisibilityCommand(); } });

		/****************************** THE TOOL MENU ******************************/

		Menu toolMenu = Menu.createMenu("Tool", 'T');
		menuBar.add(toolMenu);

		Menu drcSubMenu = Menu.createMenu("DRC", 'D');
		toolMenu.add(drcSubMenu);
		drcSubMenu.addMenuItem("Check Hierarchically", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), 
            new ActionListener() { public void actionPerformed(ActionEvent e) { DRC.checkHierarchically(); }});
		drcSubMenu.addMenuItem("Check Selection Area Hierarchically", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { DRC.checkAreaHierarchically(); }});

		Menu simulationSubMenu = Menu.createMenu("Simulation (SPICE)", 'S');
		toolMenu.add(simulationSubMenu);
		simulationSubMenu.addMenuItem("Write SPICE Deck...", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { Spice.writeSpiceDeck(); }});

		Menu netlisters = Menu.createMenu("Simulation (others)");
        netlisters.addMenuItem("Write IRSIM Netlist", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { irsimNetlistCommand(); }});
        toolMenu.add(netlisters);

		Menu ercSubMenu = Menu.createMenu("ERC", 'E');
		toolMenu.add(ercSubMenu);

		Menu networkSubMenu = Menu.createMenu("Network", 'N');
		toolMenu.add(networkSubMenu);
		networkSubMenu.addMenuItem("redo Network Numbering", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { redoNetworkNumberingCommand(); } });
		networkSubMenu.addMenuItem("NCC test 1", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { nccTest1Command(); }});
		networkSubMenu.addMenuItem("NCC test 2", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { nccTest2Command(); }});
		networkSubMenu.addMenuItem("NCC test 3", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { nccTest3Command(); }});
		networkSubMenu.addMenuItem("NCC test 4", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { nccTest4Command(); }});

		Menu logEffortSubMenu = Menu.createMenu("Logical Effort", 'L');
        logEffortSubMenu.addMenuItem("Analyze Cell", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { analyzeCellCommand(); }});
		toolMenu.add(logEffortSubMenu);

		Menu routingSubMenu = Menu.createMenu("Routing", 'R');
        routingSubMenu.addMenuItem("Auto-Stitch Now", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { AutoStitch.autoStitch(false); }});
        routingSubMenu.addMenuItem("Auto-Stitch Highlighted Now", KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), 
            new ActionListener() { public void actionPerformed(ActionEvent e) { AutoStitch.autoStitch(true); }});
		toolMenu.add(routingSubMenu);

		Menu generationSubMenu = Menu.createMenu("Generation", 'G');
		toolMenu.add(generationSubMenu);
        generationSubMenu.addMenuItem("Pad Frame Generator", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { padFrameGeneratorCommand(); }});

		Menu compactionSubMenu = Menu.createMenu("Compaction", 'C');
		toolMenu.add(compactionSubMenu);
 
		Menu languagesSubMenu = Menu.createMenu("Languages");
        languagesSubMenu.addMenuItem("Run Java Bean Shell Script", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { javaBshScriptCommand(); }});
        toolMenu.add(languagesSubMenu);

		/****************************** THE HELP MENU ******************************/

		Menu helpMenu = Menu.createMenu("Help", 'H');
		menuBar.add(helpMenu);

		helpMenu.addMenuItem("About Electric...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { aboutCommand(); } });
		helpMenu.addMenuItem("Check and Repair Libraries...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { checkAndRepairCommand(); } });
		helpMenu.addMenuItem("Show Undo List", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { showUndoListCommand(); } });
		helpMenu.addMenuItem("Make fake circuitry...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeFakeCircuitryCommand(); } });

		/****************************** Russell's TEST MENU ******************************/

		Menu russMenu = Menu.createMenu("Russell", 'R');
		menuBar.add(russMenu);
		russMenu.addMenuItem("create flat netlists for Ivan", null, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new com.sun.electric.tool.generator.layout.IvanFlat();
			}	
		});
		russMenu.addMenuItem("layout flat", null, new com.sun.electric.tool.generator.layout.LayFlat());
		russMenu.addMenuItem("gate regression", null, new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			    new com.sun.electric.tool.generator.layout.GateRegression();
		    }
		});
		russMenu.addMenuItem("Jemini", null, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new com.sun.electric.tool.ncc.jemini_2();
			}
		});
		
		/****************************** Jon's TEST MENU ******************************/

		Menu jongMenu = Menu.createMenu("JonG", 'J');
		menuBar.add(jongMenu);
        jongMenu.addMenuItem("Describe Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { listVarsOnObject(false); }});
        jongMenu.addMenuItem("Describe Proto Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { listVarsOnObject(true); }});
        jongMenu.addMenuItem("Describe Current Library Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { listLibVars(); }});
        jongMenu.addMenuItem("Eval Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { evalVarsOnObject(); }});
        jongMenu.addMenuItem("LE test1", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { LENetlister.test1(); }});
        jongMenu.addMenuItem("Open Purple Lib", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { openP4libCommand(); }});
            
		// return the menu bar
		return menuBar;
	}

	// ---------------------- THE FILE MENU -----------------

	/**
	 * This method implements the command to read a library.
     * It is interactive, and pops up a dialog box.
	 */
	public static void openLibraryCommand()
	{
		String fileName = OpenFile.chooseInputFile(OpenFile.ELIB, null);
		if (fileName != null)
		{
			// start a job to do the input
			ReadBinaryLibrary job = new ReadBinaryLibrary(fileName);
		}
	}

	/**
	 * Class to read a library in a new thread.
     * For a non-interactive script, use ReadBinaryLibrary job = new ReadBinaryLibrary(filename).
	 */
	public static class ReadBinaryLibrary extends Job
	{
		String fileName;

		public ReadBinaryLibrary(String fileName)
		{
			super("Read Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.fileName = fileName;
			this.startJob();
		}

		public void doIt()
		{
			Library lib = Input.readLibrary(fileName, Input.ImportType.BINARY);
			Undo.noUndoAllowed();
			if (lib == null) return;
			Library.setCurrent(lib);
			Cell cell = lib.getCurCell();
			if (cell == null) System.out.println("No current cell in this library"); else
			{
                // check if edit window open with null cell, use that one if exists
                for (Iterator it = WindowFrame.getWindows(); it.hasNext(); ) {
                    WindowFrame wf = (WindowFrame)it.next();
                    EditWindow wnd = wf.getEditWindow();
                    if (wnd.getCell() == null) {
                        wnd.setCell(cell, VarContext.globalContext);
                        return;
                    }
                }
				WindowFrame.createEditWindow(cell);
			}
		}
	}

	/**
	 * This method implements the command to import a library (Readable Dump format).
     * It is interactive, and pops up a dialog box.
	 */
	public static void importLibraryCommand()
	{
		String fileName = OpenFile.chooseInputFile(OpenFile.TEXT, null);
		if (fileName != null)
		{
			// start a job to do the input
			ReadTextLibrary job = new ReadTextLibrary(fileName);
		}
	}

	/**
	 * Class to read a text library in a new thread.
     * For a non-interactive script, use ReadTextLibrary job = new ReadTextLibrary(filename).
	 */
	protected static class ReadTextLibrary extends Job
	{
		String fileName;
		protected ReadTextLibrary(String fileName)
		{
			super("Read Text Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.fileName = fileName;
			this.startJob();
		}

		public void doIt()
		{
			Library lib = Input.readLibrary(fileName, Input.ImportType.TEXT);
			Undo.noUndoAllowed();
			if (lib == null) return;
			Library.setCurrent(lib);
			Cell cell = lib.getCurCell();
			if (cell == null) System.out.println("No current cell in this library"); else
			{
                // check if edit window open with null cell, use that one if exists
                for (Iterator it = WindowFrame.getWindows(); it.hasNext(); ) {
                    WindowFrame wf = (WindowFrame)it.next();
                    EditWindow wnd = wf.getEditWindow();
                    if (wnd.getCell() == null) {
                        wnd.setCell(cell, VarContext.globalContext);
                        return;
                    }
                }
				WindowFrame.createEditWindow(cell);
			}
		}
	}

	/**
	 * This method implements the command to save a library.
     * It is interactive, and pops up a dialog box.
	 */
	public static void saveLibraryCommand()
	{
		Library lib = Library.getCurrent();
		String fileName;
		if (lib.isFromDisk())
		{
			fileName = lib.getLibFile();
		} else
		{
			fileName = OpenFile.chooseOutputFile(OpenFile.ELIB, null, lib.getLibName()+".elib");
			if (fileName == null) return;

			Library.Name n = Library.Name.newInstance(fileName);
			n.setExtension("elib");
			lib.setLibFile(n.makeName());
			lib.setLibName(n.getName());
		}
		SaveLibrary job = new SaveLibrary(lib);
	}

	/**
	 * Class to save a library in a new thread.
     * For a non-interactive script, use SaveLibrary job = new SaveLibrary(filename).
     * Saves as an elib.
	 */
	protected static class SaveLibrary extends Job
	{
		Library lib;

		protected SaveLibrary(Library lib)
		{
			super("Write Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.lib = lib;
			this.startJob();
		}

		public void doIt()
		{
			boolean error = Output.writeLibrary(lib, Output.ExportType.BINARY);
			if (error)
			{
				System.out.println("Error writing the library file");
			}
		}
	}

	/**
	 * This method implements the command to save a library to a different file.
     * It is interactive, and pops up a dialog box.
	 */
	public static void saveAsLibraryCommand()
	{
		Library lib = Library.getCurrent();
		lib.clearFromDisk();
		saveLibraryCommand();
	}

    /**
     * This method implements the export cell command for different export types.
     * It is interactive, and pops up a dialog box.
     */
    public static void exportCellCommand(Output.ExportType type)
    {
        EditWindow curEdit = EditWindow.getCurrent();
        Cell cell = curEdit.getCell();
        if (cell == null) {
            System.out.println("No Cell in current window");
            return;
        }
        String filePath = OpenFile.chooseOutputFile(OpenFile.CIF, null, cell.getProtoName()+".cif");
        exportCellCommand(cell, filePath, type);
    }
    /**
     * This is the non-interactive version of exportCellCommand
     */
    public static void exportCellCommand(Cell cell, String filePath, Output.ExportType type)
    {
        ExportCell job = new ExportCell(cell, filePath, type);
    }
    
	/**
	 * Class to export a cell in a new thread.
     * For a non-interactive script, use 
     * ExportCell job = new ExportCell(Cell cell, String filename, Output.ExportType type).
     * Saves as an elib.
	 */
	protected static class ExportCell extends Job
	{
        Cell cell;
        String filePath;
        Output.ExportType type;
        
        public ExportCell(Cell cell, String filePath, Output.ExportType type)
        {
			super("Export "+cell.describe()+" ("+type+")", User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.cell = cell;
            this.filePath = filePath;
            this.type = type;
            this.startJob();
        }
        
        public void doIt() 
        {
            Output.writeCell(cell, filePath, type);
        }
        
    }
        
    /**
	 * This method implements the command to quit Electric.
	 */
	public static void quitCommand()
	{
		System.exit(0);
	}

	// ---------------------- THE EDIT MENU -----------------

	/**
	 * This class implement the command to undo the last change.
	 */
	protected static class UndoCommand extends Job
	{
		protected UndoCommand()
		{
			super("Undo", User.tool, Job.Type.UNDO, Undo.upCell(false), null, Job.Priority.USER);
			this.startJob();
		}

		public void doIt()
		{
			Highlight.clear();
			Highlight.finished();
			if (!Undo.undoABatch())
				System.out.println("Undo failed!");
		}
	}

	/**
	 * This class implement the command to undo the last change (Redo).
	 */
	protected static class RedoCommand extends Job
	{
		protected RedoCommand()
		{
			super("Redo", User.tool, Job.Type.UNDO, Undo.upCell(true), null, Job.Priority.USER);
			this.startJob();
		}

		public void doIt()
		{
			Highlight.clear();
			Highlight.finished();
			if (!Undo.redoABatch())
				System.out.println("Redo failed!");
		}
	}

	/**
	 * This method implements the command to cut the highlighted circuitry or text.
	 */
	public static void cutCommand()
	{
		Clipboard.cut();
	}

	/**
	 * This method implements the command to copy the highlighted circuitry or text.
	 */
	public static void copyCommand()
	{
		Clipboard.copy();
	}

	/**
	 * This method implements the command to paste circuitry or text.
	 */
	public static void pasteCommand()
	{
		Clipboard.paste();
	}

    /**
     * This method sets the highlighted arcs to Rigid
     */
	public static void arcRigidCommand()
	{
		int numSet = 0;
		for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.EOBJ) continue;
			ElectricObject eobj = h.getElectricObject();
			if (eobj instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)eobj;
				ai.setRigid();
				numSet++;
			}
		}
		if (numSet == 0) System.out.println("No arcs made Rigid"); else
			System.out.println("Made " + numSet + " arcs Rigid");
		EditWindow.redrawAll();
	}

    /**
     * This method sets the highlighted arcs to Non-Rigid
     */
	public static void arcNotRigidCommand()
	{
		int numSet = 0;
		for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.EOBJ) continue;
			ElectricObject eobj = h.getElectricObject();
			if (eobj instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)eobj;
				ai.clearRigid();
				numSet++;
			}
		}
		if (numSet == 0) System.out.println("No arcs made Non-Rigid"); else
			System.out.println("Made " + numSet + " arcs Non-Rigid");
		EditWindow.redrawAll();
	}

    /**
     * This method sets the highlighted arcs to Fixed-Angle
     */
	public static void arcFixedAngleCommand()
	{
		int numSet = 0;
		for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.EOBJ) continue;
			ElectricObject eobj = h.getElectricObject();
			if (eobj instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)eobj;
				ai.setFixedAngle();
				numSet++;
			}
		}
		if (numSet == 0) System.out.println("No arcs made Fixed-Angle"); else
			System.out.println("Made " + numSet + " arcs Fixed-Angle");
		EditWindow.redrawAll();
	}

    /**
     * This method sets the highlighted arcs to Not-Fixed-Angle
     */
	public static void arcNotFixedAngleCommand()
	{
		int numSet = 0;
		for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.EOBJ) continue;
			ElectricObject eobj = h.getElectricObject();
			if (eobj instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)eobj;
				ai.clearFixedAngle();
				numSet++;
			}
		}
		if (numSet == 0) System.out.println("No arcs made Not-Fixed-Angle"); else
			System.out.println("Made " + numSet + " arcs Not-Fixed-Angle");
		EditWindow.redrawAll();
	}

	/**
	 * This method implements the command to show the I/O Options dialog.
	 */
	public static void ioOptionsCommand()
	{
 		IOOptions dialog = new IOOptions(TopLevel.getCurrentJFrame(), true);
		dialog.show();
	}

	/**
	 * This method implements the command to show the Edit Options dialog.
	 */
	public static void editOptionsCommand()
	{
 		EditOptions dialog = new EditOptions(TopLevel.getCurrentJFrame(), true);
		dialog.show();
	}

	/**
	 * This method implements the command to show the Tool Options dialog.
	 */
	public static void toolOptionsCommand()
	{
 		ToolOptions dialog = new ToolOptions(TopLevel.getCurrentJFrame(), true);
		dialog.show();
	}
    
    /** 
     * This method implements the command to show the Key Bindings Options dialog.
     */
    public static void keyBindingsCommand()
    {
        EditKeyBindings dialog = new EditKeyBindings(TopLevel.getCurrentJFrame(), true);
        dialog.show();
    }

    /** 
     * This method shows the GetInfo dialog for the highlighted nodes, arcs, and/or text.
     */
	public static void getInfoCommand()
	{
		if (Highlight.getNumHighlights() == 0)
		{
			// information about the cell
			Cell c = Library.getCurrent().getCurCell();
			c.getInfo();
		} else
		{
			// information about the selected items
			int arcCount = 0;
			int nodeCount = 0;
			int exportCount = 0;
			int textCount = 0;
			int graphicsCount = 0;
			for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				ElectricObject eobj = h.getElectricObject();
				if (h.getType() == Highlight.Type.EOBJ)
				{
					if (eobj instanceof NodeInst || eobj instanceof PortInst)
					{
						nodeCount++;
					} else if (eobj instanceof ArcInst)
					{
						arcCount++;
					}
				} else if (h.getType() == Highlight.Type.TEXT)
				{
					if (eobj instanceof Export) exportCount++; else
						textCount++;
				} else if (h.getType() == Highlight.Type.BBOX)
				{
					graphicsCount++;
				} else if (h.getType() == Highlight.Type.LINE)
				{
					graphicsCount++;
				}
			}
			if (arcCount <= 1 && nodeCount <= 1 && exportCount <= 1 && textCount <= 1 && graphicsCount == 0)
			{
				if (arcCount == 1) GetInfoArc.showDialog();
				if (nodeCount == 1) GetInfoNode.showDialog();
				if (exportCount == 1) GetInfoExport.showDialog();
				if (textCount == 1) GetInfoText.showDialog();
			} else
			{
				GetInfoMulti.showDialog();
			}
		}
	}

    public static void attributesCommand()
	{
		Attributes.showDialog();
	}

    public static void setEditModeCommand(String mode)
    {
        if (mode.equals("Select")) ToolBar.selectCommand();
        if (mode.equals("Special Select")) ToolBar.selectSpecialCommand();
        if (mode.equals("Wiring")) ToolBar.wiringCommand();
        if (mode.equals("Pan")) ToolBar.panCommand();
        if (mode.equals("Zoom")) ToolBar.zoomCommand();
    }

    public static void setMovementModeCommand(String mode)
    {
        if (mode.equals("Full")) ToolBar.fullArrowDistanceCommand();
        if (mode.equals("Half")) ToolBar.halfArrowDistanceCommand();
        if (mode.equals("Quarter")) ToolBar.quarterArrowDistanceCommand();
    }

    public static void setSelectModeCommand(String mode)
    {
        if (mode.equals("Area")) ToolBar.selectAreaCommand();
        if (mode.equals("Objects")) ToolBar.selectObjectsCommand();
    }

	/**
	 * This method implements the command to highlight all objects in the current Cell.
	 */
	public static void selectAllCommand()
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;

		boolean ignoreCells = !User.isEasySelectionOfCellInstances();
		Highlight.clear();
		for(Iterator it = curCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if ((ni.getProto() instanceof Cell) && ignoreCells) continue;
			Highlight.addElectricObject(ni, curCell);
		}
		for(Iterator it = curCell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			Highlight.addElectricObject(ai, curCell);
		}
		Highlight.finished();
    }

	/**
	 * This method implements the command to show the next logged error.
	 * The error log lists the results of the latest command (DRC, NCC, etc.)
	 */
	public static void showNextErrorCommand()
	{
		String msg = ErrorLog.reportNextError();
		System.out.println(msg);
	}

	/**
	 * This method implements the command to show the last logged error.
	 * The error log lists the results of the latest command (DRC, NCC, etc.)
	 */
	public static void showPrevErrorCommand()
	{
		String msg = ErrorLog.reportPrevError();
		System.out.println(msg);
	}

	// ---------------------- THE CELL MENU -----------------

	/**
	 * This method implements the command to do cell options.
	 */
	public static void cellControlCommand()
	{
 		CellOptions dialog = new CellOptions(TopLevel.getCurrentJFrame(), true);
		dialog.show();
	}

    /** 
     * This command opens a dialog box to edit a Cell.
     */
    public static void newCellCommand()
	{
 		NewCell dialog = new NewCell(TopLevel.getCurrentJFrame(), true);
		dialog.show();
    }

	/**
	 * This method implements the command to delete the current Cell.
	 */
    public static void deleteCellCommand()
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;
		CircuitChanges.deleteCell(curCell, true);
    }

	/**
	 * This method implements the command to do cross-library copies.
	 */
	public static void crossLibraryCopyCommand()
	{
 		CrossLibCopy dialog = new CrossLibCopy(TopLevel.getCurrentJFrame(), true);
		dialog.show();
	}

    /**
     * This command pushes down the hierarchy
     */
	public static void downHierCommand() {
        EditWindow curEdit = EditWindow.getCurrent();
        curEdit.downHierarchy();
    }

    /**
     * This command goes up the hierarchy
     */
    public static void upHierCommand() {
        EditWindow curEdit = EditWindow.getCurrent();
        curEdit.upHierarchy();
    }

	/**
	 * This method implements the command to make a new version of the current Cell.
	 */
	public static void newCellVersionCommand()
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;
		CircuitChanges.newVersionOfCell(curCell);
	}

	/**
	 * This method implements the command to make a copy of the current Cell.
	 */
	public static void duplicateCellCommand()
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;

		String newName = JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "Name of duplicated cell",
			curCell.getProtoName() + "NEW");
		if (newName == null) return;
		CircuitChanges.duplicateCell(curCell, newName);
	}

	/**
	 * This method implements the command to delete old, unused versions of cells.
	 */
	public static void deleteOldCellVersionsCommand()
	{
		CircuitChanges.deleteUnusedOldVersions();
	}

	/**
	 * This method implements the command to do cell parameters.
	 */
	public static void cellParametersCommand()
	{
 		CellParameters dialog = new CellParameters(TopLevel.getCurrentJFrame(), true);
		dialog.show();
	}

	/**
	 * This method implements the command to expand the selected cells by 1 level down.
	 */
	public static void expandOneLevelDownCommand()
	{
		List list = Highlight.getHighlighted(true, false);
		CircuitChanges.ExpandUnExpand job = new CircuitChanges.ExpandUnExpand(list, false, 1);
	}

	/**
	 * This method implements the command to expand the selected cells all the way to the bottom of the hierarchy.
	 */
	public static void expandFullCommand()
	{
		List list = Highlight.getHighlighted(true, false);
		CircuitChanges.ExpandUnExpand job = new CircuitChanges.ExpandUnExpand(list, false, Integer.MAX_VALUE);
	}

	/**
	 * This method implements the command to expand the selected cells by a given number of levels from the top.
	 */
	public static void expandSpecificCommand()
	{
		Object obj = JOptionPane.showInputDialog("Number of levels to expand", "1");
		int levels = EMath.atoi((String)obj);

		List list = Highlight.getHighlighted(true, false);
		CircuitChanges.ExpandUnExpand job = new CircuitChanges.ExpandUnExpand(list, false, levels);
	}

	/**
	 * This method implements the command to unexpand the selected cells by 1 level up.
	 */
	public static void unexpandOneLevelUpCommand()
	{
		List list = Highlight.getHighlighted(true, false);
		CircuitChanges.ExpandUnExpand job = new CircuitChanges.ExpandUnExpand(list, true, 1);
	}

	/**
	 * This method implements the command to unexpand the selected cells all the way from the bottom of the hierarchy.
	 */
	public static void unexpandFullCommand()
	{
		List list = Highlight.getHighlighted(true, false);
		CircuitChanges.ExpandUnExpand job = new CircuitChanges.ExpandUnExpand(list, true, Integer.MAX_VALUE);
	}

	/**
	 * This method implements the command to unexpand the selected cells by a given number of levels from the bottom.
	 */
	public static void unexpandSpecificCommand()
	{
		Object obj = JOptionPane.showInputDialog("Number of levels to unexpand", "1");
		int levels = EMath.atoi((String)obj);

		List list = Highlight.getHighlighted(true, false);
		CircuitChanges.ExpandUnExpand job = new CircuitChanges.ExpandUnExpand(list, true, levels);
	}

	// ---------------------- THE EXPORT MENU -----------------

    /**
	 * This method implements the command to create a new Export.
	 */
	public static void newExportCommand()
	{
 		NewExport dialog = new NewExport(TopLevel.getCurrentJFrame(), true);
		dialog.show();
	}

	// ---------------------- THE VIEW MENU -----------------

	/**
	 * This method implements the command to control Views.
	 */
	public static void viewControlCommand()
	{
 		ViewControl dialog = new ViewControl(TopLevel.getCurrentJFrame(), true);
		dialog.show();
	}

	public static void editLayoutViewCommand()
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;
		Cell layoutView = curCell.otherView(View.LAYOUT);
		if (layoutView != null)
			WindowFrame.createEditWindow(layoutView);
	}

	public static void editSchematicViewCommand()
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;
		Cell schematicView = curCell.otherView(View.SCHEMATIC);
		if (schematicView != null)
			WindowFrame.createEditWindow(schematicView);
	}

	public static void editIconViewCommand()
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;
		Cell iconView = curCell.otherView(View.ICON);
		if (iconView != null)
			WindowFrame.createEditWindow(iconView);
	}

	public static void editVHDLViewCommand()
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;
		Cell vhdlView = curCell.otherView(View.VHDL);
		if (vhdlView != null)
			WindowFrame.createEditWindow(vhdlView);
	}

	public static void editDocViewCommand()
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;
		Cell docView = curCell.otherView(View.DOC);
		if (docView != null)
			WindowFrame.createEditWindow(docView);
	}

	public static void editSkeletonViewCommand()
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;
		Cell skelView = curCell.otherView(View.LAYOUTSKEL);
		if (skelView != null)
			WindowFrame.createEditWindow(skelView);
	}

	public static void editOtherViewCommand()
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;

		List views = View.getOrderedViews();
		String [] viewNames = new String[views.size()];
		for(int i=0; i<views.size(); i++)
			viewNames[i] = ((View)views.get(i)).getFullName();
		Object newName = JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "Which associated view do you want to see?",
			"Choose alternate view", JOptionPane.QUESTION_MESSAGE, null, viewNames, curCell.getView().getFullName());
		if (newName == null) return;
		String newViewName = (String)newName;
		View newView = View.findView(newViewName);
		Cell otherView = curCell.otherView(newView);
		if (otherView != null)
			WindowFrame.createEditWindow(otherView);
	}

	// ---------------------- THE WINDOW MENU -----------------

	public static void fullDisplayCommand()
	{
		// get the current window
        EditWindow wnd = EditWindow.getCurrent();
		if (wnd == null) return;

		// make the circuit fill the window
		wnd.fillScreen();
		wnd.redraw();
	}

	public static void zoomOutDisplayCommand()
	{
		// get the current window
        EditWindow wnd = EditWindow.getCurrent();
		if (wnd == null) return;

		// zoom out by a factor of two
		double scale = wnd.getScale();
		wnd.setScale(scale / 2);
		wnd.redraw();
	}

	public static void zoomInDisplayCommand()
	{
		// get the current window
        EditWindow wnd = EditWindow.getCurrent();
		if (wnd == null) return;

		// zoom in by a factor of two
		double scale = wnd.getScale();
		wnd.setScale(scale * 2);
		wnd.redraw();
	}

	public static void focusOnHighlightedCommand()
	{
		// get the current window
        EditWindow wnd = EditWindow.getCurrent();
		if (wnd == null) return;

		// focus on highlighting
		Rectangle2D bounds = Highlight.getHighlightedArea(wnd);
		wnd.focusScreen(bounds);
		wnd.redraw();
	}

	/**
	 * This method implements the command to toggle the display of the grid.
	 */
	public static void toggleGridCommand()
	{
		EditWindow wnd = EditWindow.getCurrent();
		if (wnd != null)
		{
			wnd.setGrid(!wnd.getGrid());
		}
	}

    /**
	 * This method implements the command to control Layer visibility.
	 */
	public static void layerVisibilityCommand()
	{
 		LayerVisibility dialog = new LayerVisibility(TopLevel.getCurrentJFrame(), true);
		dialog.show();
	}

	// ---------------------- THE TOOLS MENU -----------------

    // Logical Effort Tool
    public static void analyzeCellCommand()
    {
        EditWindow curEdit = EditWindow.getCurrent();
        if (curEdit == null) {
            System.out.println("Please select valid window first");
            return;
        }
        LETool letool = LETool.getLETool();
        if (letool == null) {
            System.out.println("Logical Effort tool not found");
            return;
        }
        letool.analyzeCell(curEdit.getCell(), curEdit.getVarContext(), curEdit);
    }

	public static void redoNetworkNumberingCommand()
	{
		long startTime = System.currentTimeMillis();
		int ncell = 0;
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			for(Iterator cit = lib.getCells(); cit.hasNext(); )
			{
				Cell cell = (Cell)cit.next();
				ncell++;
				cell.getNetlist(false);
			}
		}
		long endTime = System.currentTimeMillis();
		float finalTime = (endTime - startTime) / 1000F;
		System.out.println("**** Renumber networks of "+ncell+" cells took " + finalTime + " seconds");
	}

	// NCC Tool
	public static void nccTest1Command()
	{
		System.out.println("Not yet");
//		NetFactory nf = new NetFactory();
//		nf.testOne();
	}

	public static void nccTest2Command()
	{
		System.out.println("Not yet");
//		NetFactory nf = new NetFactory();
//		nf.testTwo();
	}

	public static void nccTest3Command()
	{
		System.out.println("Not yet");
//		NetFactory nf = new NetFactory();
//		nf.testThree();
	}

	public static void nccTest4Command()
	{
		System.out.println("Not yet");
//		NetFactory nf = new NetFactory();
//		nf.testFour();
	}

    public static void irsimNetlistCommand()
    {
        EditWindow curEdit = EditWindow.getCurrent();
        if (curEdit == null) {
            System.out.println("Please select valid window first");
            return;
        }
        IRSIMTool.tool.netlistCell(curEdit.getCell(), curEdit.getVarContext(), curEdit);
    }        

	public static void padFrameGeneratorCommand()
	{
		PadGenerator gen = new PadGenerator();
		gen.ArrayFromFile();
	}
    
    public static void javaBshScriptCommand()
    {
		String fileName = OpenFile.chooseInputFile(OpenFile.JAVA, null);
		if (fileName != null)
		{
			// start a job to run the script
            EvalJavaBsh.tool.runScript(fileName);
        }
    }

    // ---------------------- THE HELP MENU -----------------

	public static void aboutCommand()
	{
		About dialog = new About(TopLevel.getCurrentJFrame(), true);
		dialog.show();
	}

	public static void checkAndRepairCommand()
	{
		int errorCount = 0;
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			errorCount += lib.checkAndRepair();
		}
		if (errorCount > 0) System.out.println("Found " + errorCount + " errors"); else
			System.out.println("No errors found");
	}

	/**
	 * This method implements the command to show the undo history.
	 */
	public static void showUndoListCommand()
	{
		Undo.showHistoryList();
	}

//	public static void showRTreeCommand()
//	{
//		Library curLib = Library.getCurrent();
//		Cell curCell = curLib.getCurCell();
//		System.out.println("Current cell is " + curCell.describe());
//		if (curCell == null) return;
//		curCell.getRTree().printRTree(0);
//	}

	public static void makeFakeCircuitryCommand()
	{
        // test code to make and show something
		MakeFakeCircuitry job = new MakeFakeCircuitry();
	}

	/**
	 * Class to read a library in a new thread.
	 */
	protected static class MakeFakeCircuitry extends Job
	{
		protected MakeFakeCircuitry()
		{
			super("Make fake circuitry", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.startJob();
		}

		public void doIt()
		{
			// get information about the nodes
			NodeProto m1m2Proto = NodeProto.findNodeProto("mocmos:Metal-1-Metal-2-Con");
			NodeProto m2PinProto = NodeProto.findNodeProto("mocmos:Metal-2-Pin");
			NodeProto p1PinProto = NodeProto.findNodeProto("mocmos:Polysilicon-1-Pin");
			NodeProto m1PolyConProto = NodeProto.findNodeProto("mocmos:Metal-1-Polysilicon-1-Con");
			NodeProto pTransProto = NodeProto.findNodeProto("mocmos:P-Transistor");
			NodeProto nTransProto = NodeProto.findNodeProto("mocmos:N-Transistor");
			NodeProto cellCenterProto = NodeProto.findNodeProto("generic:Facet-Center");
			NodeProto invisiblePinProto = NodeProto.findNodeProto("generic:Invisible-Pin");

			// get information about the arcs
			ArcProto m1Proto = ArcProto.findArcProto("mocmos:Metal-1");
			ArcProto m2Proto = ArcProto.findArcProto("mocmos:Metal-2");
			ArcProto p1Proto = ArcProto.findArcProto("mocmos:Polysilicon-1");

			// get the current library
			Library mainLib = Library.getCurrent();

			// create a layout cell in the library
			Cell myCell = Cell.newInstance(mainLib, "test{lay}");
			NodeInst cellCenter = NodeInst.newInstance(cellCenterProto, new Point2D.Double(30.0, 30.0), cellCenterProto.getDefWidth(), cellCenterProto.getDefHeight(), 0, myCell, null);
			cellCenter.setVisInside();
			cellCenter.setHardSelect();
			NodeInst metal12Via = NodeInst.newInstance(m1m2Proto, new Point2D.Double(-20.0, 20.0), m1m2Proto.getDefWidth(), m1m2Proto.getDefHeight(), 0, myCell, null);
			NodeInst contactNode = NodeInst.newInstance(m1PolyConProto, new Point2D.Double(20.0, 20.0), m1PolyConProto.getDefWidth(), m1PolyConProto.getDefHeight(), 0, myCell, null);
			NodeInst metal2Pin = NodeInst.newInstance(m2PinProto, new Point2D.Double(-20.0, 10.0), m2PinProto.getDefWidth(), m2PinProto.getDefHeight(), 0, myCell, null);
			NodeInst poly1PinA = NodeInst.newInstance(p1PinProto, new Point2D.Double(20.0, -20.0), p1PinProto.getDefWidth(), p1PinProto.getDefHeight(), 0, myCell, null);
			NodeInst poly1PinB = NodeInst.newInstance(p1PinProto, new Point2D.Double(20.0, -10.0), p1PinProto.getDefWidth(), p1PinProto.getDefHeight(), 0, myCell, null);
			NodeInst transistor = NodeInst.newInstance(pTransProto, new Point2D.Double(0.0, -20.0), pTransProto.getDefWidth(), pTransProto.getDefHeight(), 0, myCell, null);
			NodeInst rotTrans = NodeInst.newInstance(nTransProto, new Point2D.Double(0.0, 10.0), nTransProto.getDefWidth(), nTransProto.getDefHeight(), 3150, myCell, "rotated");
			if (metal12Via == null || contactNode == null || metal2Pin == null || poly1PinA == null ||
				poly1PinB == null || transistor == null || rotTrans == null) return;

			// make arcs to connect them
			PortInst m1m2Port = metal12Via.getOnlyPortInst();
			PortInst contactPort = contactNode.getOnlyPortInst();
			PortInst m2Port = metal2Pin.getOnlyPortInst();
			PortInst p1PortA = poly1PinA.getOnlyPortInst();
			PortInst p1PortB = poly1PinB.getOnlyPortInst();
			PortInst transPortR = transistor.findPortInst("p-trans-poly-right");
			PortInst transRPortR = rotTrans.findPortInst("n-trans-poly-right");
			ArcInst metal2Arc = ArcInst.makeInstance(m2Proto, m2Proto.getWidth(), m2Port, m1m2Port, null);
			if (metal2Arc == null) return;
			metal2Arc.setRigid();
			ArcInst metal1Arc = ArcInst.makeInstance(m1Proto, m1Proto.getWidth(), contactPort, m1m2Port, null);
			if (metal1Arc == null) return;
			ArcInst polyArc1 = ArcInst.makeInstance(p1Proto, p1Proto.getWidth(), contactPort, p1PortB, null);
			if (polyArc1 == null) return;
			ArcInst polyArc3 = ArcInst.makeInstance(p1Proto, p1Proto.getWidth(), p1PortB, p1PortA, null);
			if (polyArc3 == null) return;
			ArcInst polyArc2 = ArcInst.makeInstance(p1Proto, p1Proto.getWidth(), transPortR, p1PortA, null);
			if (polyArc2 == null) return;
			ArcInst polyArc4 = ArcInst.makeInstance(p1Proto, p1Proto.getWidth(), transRPortR, p1PortB, null);
			if (polyArc4 == null) return;

			// export the two pins
			Export m1Export = Export.newInstance(myCell, m1m2Port, "in");
			m1Export.setCharacteristic(PortProto.Characteristic.IN);
			Export p1Export = Export.newInstance(myCell, p1PortA, "out");
			p1Export.setCharacteristic(PortProto.Characteristic.OUT);
			System.out.println("Created cell " + myCell.describe());


			// now up the hierarchy
			Cell higherCell = Cell.newInstance(mainLib, "higher{lay}");
			NodeInst higherCellCenter = NodeInst.newInstance(cellCenterProto, new Point2D.Double(0.0, 0.0), cellCenterProto.getDefWidth(), cellCenterProto.getDefHeight(), 0, higherCell, null);
			higherCellCenter.setVisInside();
			higherCellCenter.setHardSelect();
			Rectangle2D bounds = myCell.getBounds();
			double myWidth = myCell.getDefWidth();
			double myHeight = myCell.getDefHeight();
			NodeInst instance1Node = NodeInst.newInstance(myCell, new Point2D.Double(0, 0), myWidth, myHeight, 0, higherCell, null);
			instance1Node.setExpanded();
			NodeInst instance1UNode = NodeInst.newInstance(myCell, new Point2D.Double(0, 100), myWidth, myHeight, 0, higherCell, null);

			NodeInst instance2Node = NodeInst.newInstance(myCell, new Point2D.Double(100, 0), myWidth, myHeight, 900, higherCell, null);
			instance2Node.setExpanded();
			NodeInst instance2UNode = NodeInst.newInstance(myCell, new Point2D.Double(100, 100), myWidth, myHeight, 900, higherCell, null);

			NodeInst instance3Node = NodeInst.newInstance(myCell, new Point2D.Double(200, 0), myWidth, myHeight, 1800, higherCell, null);
			instance3Node.setExpanded();
			NodeInst instance3UNode = NodeInst.newInstance(myCell, new Point2D.Double(200, 100), myWidth, myHeight, 1800, higherCell, null);

			NodeInst instance4Node = NodeInst.newInstance(myCell, new Point2D.Double(300, 0), myWidth, myHeight, 2700, higherCell, null);
			instance4Node.setExpanded();
			NodeInst instance4UNode = NodeInst.newInstance(myCell, new Point2D.Double(300, 100), myWidth, myHeight, 2700, higherCell, null);

			// transposed
			NodeInst instance5Node = NodeInst.newInstance(myCell, new Point2D.Double(0, 200), -myWidth, myHeight, 0, higherCell, null);
			instance5Node.setExpanded();
			NodeInst instance5UNode = NodeInst.newInstance(myCell, new Point2D.Double(0, 300), -myWidth, myHeight, 0, higherCell, null);

			NodeInst instance6Node = NodeInst.newInstance(myCell, new Point2D.Double(100, 200), -myWidth, myHeight, 900, higherCell, null);
			instance6Node.setExpanded();
			NodeInst instance6UNode = NodeInst.newInstance(myCell, new Point2D.Double(100, 300),  -myWidth, myHeight, 900, higherCell, null);

			NodeInst instance7Node = NodeInst.newInstance(myCell, new Point2D.Double(200, 200), -myWidth, myHeight, 1800, higherCell, null);
			instance7Node.setExpanded();
			NodeInst instance7UNode = NodeInst.newInstance(myCell, new Point2D.Double(200, 300), -myWidth, myHeight, 1800, higherCell, null);

			NodeInst instance8Node = NodeInst.newInstance(myCell, new Point2D.Double(300, 200), -myWidth, myHeight, 2700, higherCell, null);
			instance8Node.setExpanded();
			NodeInst instance8UNode = NodeInst.newInstance(myCell, new Point2D.Double(300, 300), -myWidth, myHeight, 2700, higherCell, null);

			PortInst instance1Port = instance1Node.findPortInst("in");
			PortInst instance2Port = instance1UNode.findPortInst("in");
			ArcInst instanceArc = ArcInst.makeInstance(m1Proto, m1Proto.getWidth(), instance1Port, instance2Port, null);
			System.out.println("Created cell " + higherCell.describe());


			// now a rotation test
			Cell rotTestCell = Cell.newInstance(mainLib, "rotationTest{lay}");
			NodeInst rotTestCellCenter = NodeInst.newInstance(cellCenterProto, new Point2D.Double(0.0, 0.0), cellCenterProto.getDefWidth(), cellCenterProto.getDefHeight(), 0, rotTestCell, null);
			rotTestCellCenter.setVisInside();
			rotTestCellCenter.setHardSelect();
			NodeInst r0Node = NodeInst.newInstance(myCell, new Point2D.Double(0, 0), myWidth, myHeight, 0, rotTestCell, null);
			r0Node.setExpanded();
			NodeInst nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(0, -35), 0, 0, 0, rotTestCell, null);
			Variable var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 0");
			var.setDisplay();   var.getTextDescriptor().setRelSize(10);

			NodeInst r90Node = NodeInst.newInstance(myCell, new Point2D.Double(100, 0), myWidth, myHeight, 900, rotTestCell, null);
			r90Node.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(100, -35), 0, 0, 0, rotTestCell, null);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 90");
			var.setDisplay();   var.getTextDescriptor().setRelSize(10);

			NodeInst r180Node = NodeInst.newInstance(myCell, new Point2D.Double(200, 0), myWidth, myHeight, 1800, rotTestCell, null);
			r180Node.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(200, -35), 0, 0, 0, rotTestCell, null);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 180");
			var.setDisplay();   var.getTextDescriptor().setRelSize(10);

			NodeInst r270Node = NodeInst.newInstance(myCell, new Point2D.Double(300, 0), myWidth, myHeight, 2700, rotTestCell, null);
			r270Node.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(300, -35), 0, 0, 0, rotTestCell, null);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 270");
			var.setDisplay();   var.getTextDescriptor().setRelSize(10);

			// Mirrored in X
			NodeInst r0MXNode = NodeInst.newInstance(myCell, new Point2D.Double(0, 100), -myWidth, myHeight, 0, rotTestCell, null);
			r0MXNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(0, 100-35), 0, 0, 0, rotTestCell, null);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 0 MX");
			var.setDisplay();   var.getTextDescriptor().setRelSize(10);

			NodeInst r90MXNode = NodeInst.newInstance(myCell, new Point2D.Double(100, 100), -myWidth, myHeight, 900, rotTestCell, null);
			r90MXNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(100, 100-35), 0, 0, 0, rotTestCell, null);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 90 MX");
			var.setDisplay();   var.getTextDescriptor().setRelSize(10);

			NodeInst r180MXNode = NodeInst.newInstance(myCell, new Point2D.Double(200, 100), -myWidth, myHeight, 1800, rotTestCell, null);
			r180MXNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(200, 100-35), 0, 0, 0, rotTestCell, null);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 180 MX");
			var.setDisplay();   var.getTextDescriptor().setRelSize(10);

			NodeInst r270MXNode = NodeInst.newInstance(myCell, new Point2D.Double(300, 100), -myWidth, myHeight, 2700, rotTestCell, null);
			r270MXNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(300, 100-35), 0, 0, 0, rotTestCell, null);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 270 MX");
			var.setDisplay();   var.getTextDescriptor().setRelSize(10);

			// Mirrored in Y
			NodeInst r0MYNode = NodeInst.newInstance(myCell, new Point2D.Double(0, 200), myWidth, -myHeight, 0, rotTestCell, null);
			r0MYNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(0, 200-35), 0, 0, 0, rotTestCell, null);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 0 MY");
			var.setDisplay();   var.getTextDescriptor().setRelSize(10);

			NodeInst r90MYNode = NodeInst.newInstance(myCell, new Point2D.Double(100, 200), myWidth, -myHeight, 900, rotTestCell, null);
			r90MYNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(100, 200-35), 0, 0, 0, rotTestCell, null);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 90 MY");
			var.setDisplay();   var.getTextDescriptor().setRelSize(10);

			NodeInst r180MYNode = NodeInst.newInstance(myCell, new Point2D.Double(200, 200), myWidth, -myHeight, 1800, rotTestCell, null);
			r180MYNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(200, 200-35), 0, 0, 0, rotTestCell, null);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 180 MY");
			var.setDisplay();   var.getTextDescriptor().setRelSize(10);

			NodeInst r270MYNode = NodeInst.newInstance(myCell, new Point2D.Double(300, 200), myWidth, -myHeight, 2700, rotTestCell, null);
			r270MYNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(300, 200-35), 0, 0, 0, rotTestCell, null);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 270 MY");
			var.setDisplay();   var.getTextDescriptor().setRelSize(10);

			// Mirrored in X and Y
			NodeInst r0MXYNode = NodeInst.newInstance(myCell, new Point2D.Double(0, 300), -myWidth, -myHeight, 0, rotTestCell, null);
			r0MXYNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(0, 300-35), 0, 0, 0, rotTestCell, null);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 0 MXY");
			var.setDisplay();   var.getTextDescriptor().setRelSize(10);

			NodeInst r90MXYNode = NodeInst.newInstance(myCell, new Point2D.Double(100, 300), -myWidth, -myHeight, 900, rotTestCell, null);
			r90MXYNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(100, 300-35), 0, 0, 0, rotTestCell, null);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 90 MXY");
			var.setDisplay();   var.getTextDescriptor().setRelSize(10);

			NodeInst r180MXYNode = NodeInst.newInstance(myCell, new Point2D.Double(200, 300), -myWidth, -myHeight, 1800, rotTestCell, null);
			r180MXYNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(200, 300-35), 0, 0, 0, rotTestCell, null);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 180 MXY");
			var.setDisplay();   var.getTextDescriptor().setRelSize(10);

			NodeInst r270MXYNode = NodeInst.newInstance(myCell, new Point2D.Double(300, 300), -myWidth, -myHeight, 2700, rotTestCell, null);
			r270MXYNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(300, 300-35), 0, 0, 0, rotTestCell, null);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 270 MXY");
			var.setDisplay();   var.getTextDescriptor().setRelSize(10);

			System.out.println("Created cell " + rotTestCell.describe());


			// now up the hierarchy even farther
			Cell bigCell = Cell.newInstance(mainLib, "big{lay}");
			NodeInst bigCellCenter = NodeInst.newInstance(cellCenterProto, new Point2D.Double(0.0, 0.0), cellCenterProto.getDefWidth(), cellCenterProto.getDefHeight(), 0, bigCell, null);
			bigCellCenter.setVisInside();
			bigCellCenter.setHardSelect();
			int arraySize = 20;
			for(int y=0; y<arraySize; y++)
			{
				for(int x=0; x<arraySize; x++)
				{
					String theName = "arr["+ x + "][" + y + "]";
					NodeInst instanceNode = NodeInst.newInstance(myCell, new Point2D.Double(x*(myWidth+2), y*(myHeight+2)),
						myWidth, myHeight, 0, bigCell, theName);
					TextDescriptor td = instanceNode.getNameTextDescriptor();
					td.setOff(0, 8);
					instanceNode.setNameTextDescriptor(td);
					if ((x%2) == (y%2)) instanceNode.setExpanded();
				}
			}
			System.out.println("Created cell " + bigCell.describe());

			// disallow undo
			Undo.noUndoAllowed();

			// display a cell
			WindowFrame.createEditWindow(myCell);
		}
	}

    // ---------------------- THE JON GAINSLEY MENU -----------------
    
    public static void listVarsOnObject(boolean useproto) {
        if (Highlight.getNumHighlights() == 0) {
            System.out.println("Nothing highlighted");
            return;
        }
        for (Iterator it = Highlight.getHighlights(); it.hasNext();) {
            Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.EOBJ) continue;
            ElectricObject eobj = h.getElectricObject();
            if (eobj instanceof NodeInst) {
                NodeInst ni = (NodeInst)eobj;
                if (useproto) {
                    System.out.println("using prototype");
                    ((ElectricObject)ni.getProto()).getInfo();
                } else {
                    ni.getInfo();
                }
            }
        }
    }

    public static void evalVarsOnObject() {
        EditWindow curEdit = EditWindow.getCurrent();
        if (Highlight.getNumHighlights() == 0) {
            System.out.println("Nothing highlighted");
            return;
        }
        for (Iterator it = Highlight.getHighlights(); it.hasNext();) {
            Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.EOBJ) continue;
            ElectricObject eobj = h.getElectricObject();
            Iterator itVar = eobj.getVariables();
            while(itVar.hasNext()) {
                Variable var = (Variable)itVar.next();
                Object obj = curEdit.getVarContext().evalVar(var);
                System.out.print(var.getKey().getName() + ": ");
                System.out.println(obj);
            }
        }
    }
    
    public static void listLibVars() {
        Library lib = Library.getCurrent();
        Iterator itVar = lib.getVariables();
        System.out.println("----------"+lib+" Vars-----------");
        while(itVar.hasNext()) {
            Variable var = (Variable)itVar.next();
            Object obj = VarContext.globalContext.evalVar(var);
            System.out.println(var.getKey().getName() + ": " +obj);
        }
    }
    
    public static void openP4libCommand() {
		ReadBinaryLibrary job = new ReadBinaryLibrary("/export/gainsley/soesrc_java/test/purpleFour.elib");
//        OpenBinLibraryThread oThread = new OpenBinLibraryThread("/export/gainsley/soesrc_java/test/purpleFour.elib");
//        oThread.start();
    }

}
