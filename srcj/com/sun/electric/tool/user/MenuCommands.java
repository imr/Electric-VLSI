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
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.generator.PadGenerator;
import com.sun.electric.tool.io.Input;
import com.sun.electric.tool.io.Output;
import com.sun.electric.tool.io.OutputPostScript;
import com.sun.electric.tool.io.OutputVerilog;
import com.sun.electric.tool.logicaleffort.LENetlister;
import com.sun.electric.tool.logicaleffort.LETool;
import com.sun.electric.tool.routing.AutoStitch;
import com.sun.electric.tool.routing.MimicStitch;
import com.sun.electric.tool.simulation.Spice;
import com.sun.electric.tool.simulation.IRSIMTool;
import com.sun.electric.tool.user.dialogs.*;
import com.sun.electric.tool.user.ui.MenuManager;
import com.sun.electric.tool.user.ui.MenuManager.MenuItem;
import com.sun.electric.tool.user.ui.MenuManager.Menu;
import com.sun.electric.tool.user.ui.MenuManager.MenuBar;
import com.sun.electric.tool.user.ui.*;

import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PrinterJob;
import java.awt.print.Printable;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import javax.print.PrintServiceLookup;
import javax.print.PrintService;
import javax.swing.ButtonGroup;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.JOptionPane;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;

/**
 * This class has all of the pulldown menu commands in Electric.
 * <p>
 * For SDI mode Swing requires that each window have it's own menu.
 * This means for consistency across windows that a change of state on 
 * a menu item in one window's menu must occur in all other window's
 * menus as well (such as checking a check box).
 */
public final class MenuCommands
{

    // It is never useful for anyone to create an instance of this class
	private MenuCommands() {}

	/**
	 * Method to create the pulldown menus.
	 */
	public static JMenuBar createMenuBar()
	{
		// create the menu bar
		MenuBar menuBar = new MenuBar();
        MenuItem m;
		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		/****************************** THE FILE MENU ******************************/

		Menu fileMenu = new Menu("File", 'F');
		menuBar.add(fileMenu);

		fileMenu.addMenuItem("New Library", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { newLibraryCommand(); } });
		fileMenu.addMenuItem("Open Library", KeyStroke.getKeyStroke('O', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { openLibraryCommand(); } });
		Menu importSubMenu = new Menu("Import");
		fileMenu.add(importSubMenu);
		importSubMenu.addMenuItem("Readable Dump", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(); } });

		fileMenu.addSeparator();

		fileMenu.addMenuItem("Close Library", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { closeLibraryCommand(Library.getCurrent()); } });
		fileMenu.addMenuItem("Save Library", KeyStroke.getKeyStroke('S', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveLibraryCommand(Library.getCurrent()); } });
		fileMenu.addMenuItem("Save Library as...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveAsLibraryCommand(); } });
		Menu exportSubMenu = new Menu("Export");
		fileMenu.add(exportSubMenu);
		exportSubMenu.addMenuItem("CIF (Caltech Intermediate Format)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(Output.ExportType.CIF, OpenFile.CIF); } });
		exportSubMenu.addMenuItem("GDS II (Stream)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(Output.ExportType.GDS, OpenFile.GDS); } });
		exportSubMenu.addMenuItem("PostScript", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(Output.ExportType.POSTSCRIPT, OpenFile.POSTSCRIPT); } });

		fileMenu.addSeparator();

        fileMenu.addMenuItem("Repair Libraries", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { checkAndRepairCommand(); } });

        fileMenu.addSeparator();

		fileMenu.addMenuItem("Print...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { printCommand(); } });

		if (TopLevel.getOperatingSystem() != TopLevel.OS.MACINTOSH)
		{
			fileMenu.addSeparator();
			fileMenu.addMenuItem("Quit", KeyStroke.getKeyStroke('Q', buckyBit),
				new ActionListener() { public void actionPerformed(ActionEvent e) { quitCommand(); } });
		}

		/****************************** THE EDIT MENU ******************************/

		Menu editMenu = new Menu("Edit", 'E');
		menuBar.add(editMenu);

		editMenu.addMenuItem("Cut", KeyStroke.getKeyStroke('X', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { cutCommand(); } });
		editMenu.addMenuItem("Copy", KeyStroke.getKeyStroke('C', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { copyCommand(); } });
		editMenu.addMenuItem("Paste", KeyStroke.getKeyStroke('V', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { pasteCommand(); } });

		editMenu.addSeparator();

		Menu arcSubMenu = new Menu("Arc", 'A');
		editMenu.add(arcSubMenu);
		arcSubMenu.addMenuItem("Rigid", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { arcRigidCommand(); }});
		arcSubMenu.addMenuItem("Not Rigid", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { arcNotRigidCommand(); }});
		arcSubMenu.addMenuItem("Fixed Angle", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { arcFixedAngleCommand(); }});
		arcSubMenu.addMenuItem("Not Fixed Angle", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { arcNotFixedAngleCommand(); }});
		arcSubMenu.addSeparator();
		arcSubMenu.addMenuItem("Negated", KeyStroke.getKeyStroke('T', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { arcNegatedCommand(); }});
		arcSubMenu.addMenuItem("Directional", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { arcDirectionalCommand(); }});

		editMenu.addSeparator();

		editMenu.addMenuItem("Undo", KeyStroke.getKeyStroke('Z', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { new UndoCommand(); } });
		editMenu.addMenuItem("Redo", KeyStroke.getKeyStroke('Y', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { new RedoCommand(); } });
        editMenu.addMenuItem("Show Undo List", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { showUndoListCommand(); } });

		editMenu.addSeparator();

		Menu rotateSubMenu = new Menu("Rotate", 'R');
		editMenu.add(rotateSubMenu);
		rotateSubMenu.addMenuItem("90 Degrees Clockwise", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.rotateObjects(2700); }});
		rotateSubMenu.addMenuItem("90 Degrees Counterclockwise", KeyStroke.getKeyStroke('J', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.rotateObjects(900); }});
		rotateSubMenu.addMenuItem("180 Degrees", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.rotateObjects(1800); }});
		rotateSubMenu.addMenuItem("Other...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.rotateObjects(0); }});

		Menu mirrorSubMenu = new Menu("Mirror", 'M');
		editMenu.add(mirrorSubMenu);
		mirrorSubMenu.addMenuItem("Horizontally (flip over X-axis)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.mirrorObjects(true); }});
		mirrorSubMenu.addMenuItem("Vertically (flip over Y-axis)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.mirrorObjects(false); }});

		editMenu.addMenuItem("Adjust Size", KeyStroke.getKeyStroke('B', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { SizeListener.sizeObjects(); } });

		editMenu.addSeparator();

		m=editMenu.addMenuItem("Erase", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.deleteSelected(); } });
        m.addDefaultKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), null);

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

		editMenu.addMenuItem("Get Info...", KeyStroke.getKeyStroke('I', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { getInfoCommand(); } });
		editMenu.addMenuItem("Attributes...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { attributesCommand(); } });

		editMenu.addSeparator();

		Menu modeSubMenu = new Menu("Modes");
		editMenu.add(modeSubMenu);

		Menu modeSubMenuEdit = new Menu("Edit");
		modeSubMenu.add(modeSubMenuEdit);
		ButtonGroup editGroup = new ButtonGroup();
        JMenuItem cursorClickZoomWire, cursorSelect, cursorWiring, cursorPan, cursorZoom, cursorOutline;
		cursorClickZoomWire = modeSubMenuEdit.addRadioButton(ToolBar.cursorClickZoomWireName, true, editGroup, KeyStroke.getKeyStroke('S', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.clickZoomWireCommand(); } });
		cursorSelect = modeSubMenuEdit.addRadioButton(ToolBar.cursorSelectName, false, editGroup, KeyStroke.getKeyStroke('M', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.selectCommand(); } });
		cursorWiring = modeSubMenuEdit.addRadioButton(ToolBar.cursorWiringName, false, editGroup, KeyStroke.getKeyStroke('W', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.wiringCommand(); } });
		cursorPan = modeSubMenuEdit.addRadioButton(ToolBar.cursorPanName, false, editGroup, KeyStroke.getKeyStroke('P', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.panCommand(); } });
		cursorZoom = modeSubMenuEdit.addRadioButton(ToolBar.cursorZoomName, false, editGroup, KeyStroke.getKeyStroke('Z', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.zoomCommand(); } });
		cursorOutline = modeSubMenuEdit.addRadioButton(ToolBar.cursorOutlineName, false, editGroup, KeyStroke.getKeyStroke('Y', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.outlineEditCommand(); } });
		ToolBar.CursorMode cm = ToolBar.getCursorMode();
        if (cm == ToolBar.CursorMode.CLICKZOOMWIRE) cursorClickZoomWire.setSelected(true); else
		if (cm == ToolBar.CursorMode.SELECT) cursorSelect.setSelected(true); else
		if (cm == ToolBar.CursorMode.WIRE) cursorWiring.setSelected(true); else
		if (cm == ToolBar.CursorMode.PAN) cursorPan.setSelected(true); else
		if (cm == ToolBar.CursorMode.ZOOM) cursorZoom.setSelected(true); else
			cursorOutline.setSelected(true);

		Menu modeSubMenuMovement = new Menu("Movement");
		modeSubMenu.add(modeSubMenuMovement);
		ButtonGroup movementGroup = new ButtonGroup();
        JMenuItem moveFull, moveHalf, moveQuarter;
		moveFull = modeSubMenuMovement.addRadioButton(ToolBar.moveFullName, true, movementGroup, KeyStroke.getKeyStroke('F', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.fullArrowDistanceCommand(); } });
		moveHalf = modeSubMenuMovement.addRadioButton(ToolBar.moveHalfName, false, movementGroup, KeyStroke.getKeyStroke('H', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.halfArrowDistanceCommand(); } });
		moveQuarter = modeSubMenuMovement.addRadioButton(ToolBar.moveQuarterName, false, movementGroup, null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.quarterArrowDistanceCommand(); } });
		double ad = ToolBar.getArrowDistance();
		if (ad == 1.0) moveFull.setSelected(true); else
		if (ad == 0.5) moveHalf.setSelected(true); else
			moveQuarter.setSelected(true);

		editMenu.addMenuItem("Array...", KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Array.showArrayDialog(); } });
		editMenu.addMenuItem("Change...", KeyStroke.getKeyStroke('C', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Change.showChangeDialog(); } });

		Menu cleanupSubMenu = new Menu("Cleanup Cell");
		editMenu.add(cleanupSubMenu);
		cleanupSubMenu.addMenuItem("Cleanup Pins", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.cleanupPinsCommand(false); }});
		cleanupSubMenu.addMenuItem("Cleanup Pins Everywhere", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.cleanupPinsCommand(true); }});
		cleanupSubMenu.addMenuItem("Show Nonmanhattan", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.showNonmanhattanCommand(); }});
		cleanupSubMenu.addMenuItem("Shorten Selected Arcs", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.shortenArcsCommand(); }});

		Menu modeSubMenuSelect = new Menu("Select");
		modeSubMenu.add(modeSubMenuSelect);
		ButtonGroup selectGroup = new ButtonGroup();
        JMenuItem selectArea, selectObjects;
		selectArea = modeSubMenuSelect.addRadioButton(ToolBar.selectAreaName, true, selectGroup, null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.selectAreaCommand(); } });
		selectObjects = modeSubMenuSelect.addRadioButton(ToolBar.selectObjectsName, false, selectGroup, null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.selectObjectsCommand(); } });
		ToolBar.SelectMode sm = ToolBar.getSelectMode();
		if (sm == ToolBar.SelectMode.AREA) selectArea.setSelected(true); else
			selectObjects.setSelected(true);
		modeSubMenuSelect.addCheckBox(ToolBar.specialSelectName, false, null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.toggleSelectSpecialCommand(e); } });

		Menu selListSubMenu = new Menu("Selection");
		editMenu.add(selListSubMenu);
		selListSubMenu.addMenuItem("Select All", KeyStroke.getKeyStroke('A', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectAllCommand(); }});
		selListSubMenu.addMenuItem("Select All Like This", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectAllLikeThisCommand(); }});
		selListSubMenu.addMenuItem("Select Easy", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectEasyCommand(); }});
		selListSubMenu.addMenuItem("Select Hard", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectHardCommand(); }});
		selListSubMenu.addMenuItem("Select Nothing", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectNothingCommand(); }});
		selListSubMenu.addSeparator();
		selListSubMenu.addMenuItem("Make Selected Easy", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectMakeEasyCommand(); }});
		selListSubMenu.addMenuItem("Make Selected Hard", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectMakeHardCommand(); }});
		selListSubMenu.addSeparator();
		selListSubMenu.addMenuItem("Show Next Error", KeyStroke.getKeyStroke('>'),
			new ActionListener() { public void actionPerformed(ActionEvent e) { showNextErrorCommand(); }});
		selListSubMenu.addMenuItem("Show Previous Error", KeyStroke.getKeyStroke('<'),
			new ActionListener() { public void actionPerformed(ActionEvent e) { showPrevErrorCommand(); }});

		/****************************** THE CELL MENU ******************************/

		Menu cellMenu = new Menu("Cell", 'C');
		menuBar.add(cellMenu);

        cellMenu.addMenuItem("Edit Cell...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { cellBrowserCommand(CellBrowser.DoAction.editCell); }});
        cellMenu.addMenuItem("Place Cell Instance...", KeyStroke.getKeyStroke('N', 0),
            new ActionListener() { public void actionPerformed(ActionEvent e) { cellBrowserCommand(CellBrowser.DoAction.newInstance); }});
        cellMenu.addMenuItem("New Cell...", KeyStroke.getKeyStroke('N', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { newCellCommand(); } });
        cellMenu.addMenuItem("Rename Cell...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { cellBrowserCommand(CellBrowser.DoAction.renameCell); }});
        cellMenu.addMenuItem("Duplicate Cell...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { cellBrowserCommand(CellBrowser.DoAction.duplicateCell); }});
        cellMenu.addMenuItem("Delete Cell...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { cellBrowserCommand(CellBrowser.DoAction.deleteCell); }});

        cellMenu.addSeparator();

		cellMenu.addMenuItem("Delete Current Cell", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { deleteCellCommand(); } });
        cellMenu.addMenuItem("Cell Control...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { cellControlCommand(); }});
		cellMenu.addMenuItem("Cross-Library Copy...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { crossLibraryCopyCommand(); } });

		cellMenu.addSeparator();

		cellMenu.addMenuItem("Down Hierarchy", KeyStroke.getKeyStroke('D', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { downHierCommand(); }});
		cellMenu.addMenuItem("Up Hierarchy", KeyStroke.getKeyStroke('U', buckyBit),
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
		Menu specListSubMenu = new Menu("Special Cell Lists");
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

		Menu expandListSubMenu = new Menu("Expand Cell Instances");
		cellMenu.add(expandListSubMenu);
		expandListSubMenu.addMenuItem("One Level Down", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { expandOneLevelDownCommand(); }});
		expandListSubMenu.addMenuItem("All the Way", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { expandFullCommand(); }});
		expandListSubMenu.addMenuItem("Specified Amount", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { expandSpecificCommand(); }});
		Menu unExpandListSubMenu = new Menu("Unexpand Cell Instances");
		cellMenu.add(unExpandListSubMenu);
		unExpandListSubMenu.addMenuItem("One Level Up", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { unexpandOneLevelUpCommand(); }});
		unExpandListSubMenu.addMenuItem("All the Way", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { unexpandFullCommand(); }});
		unExpandListSubMenu.addMenuItem("Specified Amount", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { unexpandSpecificCommand(); }});

		/****************************** THE EXPORT MENU ******************************/

		Menu exportMenu = new Menu("Export", 'X');
		menuBar.add(exportMenu);

		exportMenu.addMenuItem("Create Export...", KeyStroke.getKeyStroke('E', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.newExportCommand(); } });

		exportMenu.addSeparator();

		exportMenu.addMenuItem("Re-Export Everything", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.reExportAll(); } });
		exportMenu.addMenuItem("Re-Export Highlighted", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.reExportHighlighted(); } });
		exportMenu.addMenuItem("Re-Export Power and Ground", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.reExportPowerAndGround(); } });

		exportMenu.addSeparator();

		exportMenu.addMenuItem("Delete All Exports on Highlighted", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.deleteExportsOnHighlighted(); } });
		exportMenu.addMenuItem("Delete Exports in Area", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.deleteExportsInArea(); } });

		exportMenu.addSeparator();

		exportMenu.addMenuItem("Summarize Exports", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.describeExports(true); } });
		exportMenu.addMenuItem("List Exports", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.describeExports(false); } });
		exportMenu.addMenuItem("Show Exports", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.showExports(); } });

		exportMenu.addSeparator();

		exportMenu.addMenuItem("Show Ports on Node", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.showPorts(); } });

		/****************************** THE VIEW MENU ******************************/

		Menu viewMenu = new Menu("View", 'V');
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

		Menu windowMenu = new Menu("Window", 'W');
		menuBar.add(windowMenu);

		windowMenu.addMenuItem("Toggle Grid", KeyStroke.getKeyStroke('G', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { toggleGridCommand(); } });

		windowMenu.addSeparator();

		Menu windowPartitionSubMenu = new Menu("Adjust Position");
		windowMenu.add(windowPartitionSubMenu);
		windowPartitionSubMenu.addMenuItem("Tile Horizontally", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { tileHorizontallyCommand(); }});
		windowPartitionSubMenu.addMenuItem("Tile Vertically", KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { tileVerticallyCommand(); }});
		windowPartitionSubMenu.addMenuItem("Cascade", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { cascadeWindowsCommand(); }});
        windowMenu.addMenuItem("Close Window", KeyStroke.getKeyStroke(KeyEvent.VK_W, buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { WindowFrame curWF = WindowFrame.getCurrentWindowFrame();
                curWF.finished(); }});

        if (!TopLevel.isMDIMode()) {
            windowMenu.addSeparator();
            windowMenu.addMenuItem("Move to Other Display", null,
                new ActionListener() { public void actionPerformed(ActionEvent e) { moveToOtherDisplayCommand(); } });
        }

		windowMenu.addSeparator();

        windowMenu.addMenuItem("Layer Visibility...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { layerVisibilityCommand(); } });

        windowMenu.addSeparator();

        m = windowMenu.addMenuItem("Fill Display", KeyStroke.getKeyStroke('9', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.fullDisplay(); } });
        m.addDefaultKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD9, buckyBit), null);
        m = windowMenu.addMenuItem("Zoom Out", KeyStroke.getKeyStroke('0', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.zoomOutDisplay(); } });
        m.addDefaultKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, buckyBit), null);
        m = windowMenu.addMenuItem("Zoom In", KeyStroke.getKeyStroke('7', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.zoomInDisplay(); } });
        m.addDefaultKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD7, buckyBit), null);
        m = windowMenu.addMenuItem("Focus on Highlighted", KeyStroke.getKeyStroke('F', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.focusOnHighlighted(); } });
        m.addDefaultKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD5, buckyBit), null);

        m = windowMenu.addMenuItem("Pan Up", KeyStroke.getKeyStroke('8', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panY(EditWindow.getCurrent(), -4); }});
        m.addDefaultKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD8, buckyBit), null);
        m = windowMenu.addMenuItem("Pan Down", KeyStroke.getKeyStroke('2', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panY(EditWindow.getCurrent(), 4); }});
        m.addDefaultKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD2, buckyBit), null);
        m = windowMenu.addMenuItem("Pan Left", KeyStroke.getKeyStroke('4', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panX(EditWindow.getCurrent(), 4); }});
        m.addDefaultKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD4, buckyBit), null);
        m = windowMenu.addMenuItem("Pan Right", KeyStroke.getKeyStroke('6', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panX(EditWindow.getCurrent(), -4); }});
        m.addDefaultKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD6, buckyBit), null);

		/****************************** THE TOOL MENU ******************************/

		Menu toolMenu = new Menu("Tool", 'T');
		menuBar.add(toolMenu);

		Menu drcSubMenu = new Menu("DRC", 'D');
		toolMenu.add(drcSubMenu);
		drcSubMenu.addMenuItem("Check Hierarchically", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { DRC.checkHierarchically(); }});
		drcSubMenu.addMenuItem("Check Selection Area Hierarchically", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { DRC.checkAreaHierarchically(); }});

		Menu spiceSimulationSubMenu = new Menu("Simulation (SPICE)", 'S');
		toolMenu.add(spiceSimulationSubMenu);
		spiceSimulationSubMenu.addMenuItem("Write SPICE Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Spice.writeSpiceDeck(); }});
		spiceSimulationSubMenu.addSeparator();
		spiceSimulationSubMenu.addMenuItem("Set Generic SPICE Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_TEMPLATE_KEY); }});
		spiceSimulationSubMenu.addMenuItem("Set SPICE 2 Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_2_TEMPLATE_KEY); }});
		spiceSimulationSubMenu.addMenuItem("Set SPICE 3 Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_3_TEMPLATE_KEY); }});
		spiceSimulationSubMenu.addMenuItem("Set HSPICE Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_H_TEMPLATE_KEY); }});
		spiceSimulationSubMenu.addMenuItem("Set PSPICE Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_P_TEMPLATE_KEY); }});
		spiceSimulationSubMenu.addMenuItem("Set GnuCap Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_GC_TEMPLATE_KEY); }});
		spiceSimulationSubMenu.addMenuItem("Set SmartSPICE Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Spice.SPICE_SM_TEMPLATE_KEY); }});

		Menu verilogSimulationSubMenu = new Menu("Simulation (Verilog)", 'V');
		toolMenu.add(verilogSimulationSubMenu);
		verilogSimulationSubMenu.addMenuItem("Write Verilog Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(Output.ExportType.VERILOG, OpenFile.VERILOG); } });
		verilogSimulationSubMenu.addSeparator();
		verilogSimulationSubMenu.addMenuItem("Set Verilog Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(OutputVerilog.VERILOG_TEMPLATE_KEY); }});

		Menu netlisters = new Menu("Simulation (others)");
		netlisters.addMenuItem("Write IRSIM Netlist", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { irsimNetlistCommand(); }});
		toolMenu.add(netlisters);

		Menu ercSubMenu = new Menu("ERC", 'E');
		toolMenu.add(ercSubMenu);

		Menu networkSubMenu = new Menu("Network", 'N');
		toolMenu.add(networkSubMenu);
		networkSubMenu.addMenuItem("redo Network Numbering", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { redoNetworkNumberingCommand(); } });

		Menu logEffortSubMenu = new Menu("Logical Effort", 'L');
		logEffortSubMenu.addMenuItem("Analyze Cell", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { analyzeCellCommand(); }});
		toolMenu.add(logEffortSubMenu);

		Menu routingSubMenu = new Menu("Routing", 'R');
		routingSubMenu.addMenuItem("Mimic-Stitch Now", KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { MimicStitch.mimicStitch(true); }});
	   routingSubMenu.addMenuItem("Auto-Stitch Now", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { AutoStitch.autoStitch(false, true); }});
		routingSubMenu.addMenuItem("Auto-Stitch Highlighted Now", KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { AutoStitch.autoStitch(true, true); }});
		toolMenu.add(routingSubMenu);

		Menu generationSubMenu = new Menu("Generation", 'G');
		toolMenu.add(generationSubMenu);
		generationSubMenu.addMenuItem("Pad Frame Generator", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { padFrameGeneratorCommand(); }});

		Menu compactionSubMenu = new Menu("Compaction", 'C');
		toolMenu.add(compactionSubMenu);

		Menu languagesSubMenu = new Menu("Languages");
		languagesSubMenu.addMenuItem("Run Java Bean Shell Script", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { javaBshScriptCommand(); }});
		toolMenu.add(languagesSubMenu);

		/****************************** THE HELP MENU ******************************/

		Menu helpMenu = new Menu("Help", 'H');
		menuBar.add(helpMenu);

		if (TopLevel.getOperatingSystem() != TopLevel.OS.MACINTOSH)
		{
			helpMenu.addMenuItem("About Electric...", null,
				new ActionListener() { public void actionPerformed(ActionEvent e) { aboutCommand(); } });
		}
		helpMenu.addSeparator();
		helpMenu.addMenuItem("Make fake circuitry...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeFakeCircuitryCommand(); } });
//		helpMenu.addMenuItem("Whit Diffie's design...", null,
//			new ActionListener() { public void actionPerformed(ActionEvent e) { whitDiffieCommand(); } });

		/****************************** Russell's TEST MENU ******************************/

		Menu russMenu = new Menu("Russell", 'R');
		menuBar.add(russMenu);
		russMenu.addMenuItem("create flat netlists for Ivan", null, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new com.sun.electric.tool.generator.layout.IvanFlat();
			}
		});
		russMenu.addMenuItem("layout flat", null, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new com.sun.electric.tool.generator.layout.LayFlat();
			}
		});
		russMenu.addMenuItem("gate regression", null, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new com.sun.electric.tool.generator.layout.GateRegression();
			}
		});
		russMenu.addMenuItem("Jemini", null, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new com.sun.electric.tool.ncc.NccJob();
			}
		});
		russMenu.addMenuItem("Random Test", null, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new com.sun.electric.tool.generator.layout.Test();
			}
		});

		/****************************** Jon's TEST MENU ******************************/

		Menu jongMenu = new Menu("JonG", 'J');
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


        /********************************* Hidden Menus *******************************/

        Menu wiringShortcuts = new Menu("Circuit Editing");
        menuBar.addHidden(wiringShortcuts);
        wiringShortcuts.addMenuItem("Wire to Poly", KeyStroke.getKeyStroke(KeyEvent.VK_0, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.wireTo(0); }});
        wiringShortcuts.addMenuItem("Wire to M1", KeyStroke.getKeyStroke(KeyEvent.VK_1, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.wireTo(1); }});
        wiringShortcuts.addMenuItem("Wire to M2", KeyStroke.getKeyStroke(KeyEvent.VK_2, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.wireTo(2); }});
        wiringShortcuts.addMenuItem("Wire to M3", KeyStroke.getKeyStroke(KeyEvent.VK_3, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.wireTo(3); }});
        wiringShortcuts.addMenuItem("Wire to M4", KeyStroke.getKeyStroke(KeyEvent.VK_4, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.wireTo(4); }});
        wiringShortcuts.addMenuItem("Wire to M5", KeyStroke.getKeyStroke(KeyEvent.VK_5, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.wireTo(5); }});
        wiringShortcuts.addMenuItem("Wire to M6", KeyStroke.getKeyStroke(KeyEvent.VK_6, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.wireTo(6); }});
        //wiringShortcuts.addMenuItem("Switch Wiring Target", KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
        //        new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.switchWiringTarget(); }});

		// return the menu bar
		return menuBar;
	}


	// ---------------------- THE FILE MENU -----------------

	public static void newLibraryCommand()
	{
		String newLibName = JOptionPane.showInputDialog("New Library Name", "");
		if (newLibName == null) return;
		Library lib = Library.newInstance(newLibName, null);
		if (lib == null) return;
		lib.setCurrent();
		ExplorerTree.explorerTreeChanged();
		EditWindow.repaintAll();
	}

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
			URL fileURL = TextUtils.makeURLToFile(fileName);
			ReadBinaryLibrary job = new ReadBinaryLibrary(fileURL);
		}
	}

	/**
	 * Class to read a library in a new thread.
	 * For a non-interactive script, use ReadBinaryLibrary job = new ReadBinaryLibrary(filename).
	 */
	public static class ReadBinaryLibrary extends Job
	{
		URL fileURL;

		public ReadBinaryLibrary(URL fileURL)
		{
			super("Read Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.fileURL = fileURL;
			startJob();
		}

		public void doIt()
		{
			Library lib = Input.readLibrary(fileURL, Input.ImportType.BINARY);
			Undo.noUndoAllowed();
			if (lib == null) return;
			lib.setCurrent();
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
			URL fileURL = TextUtils.makeURLToFile(fileName);
			ReadTextLibrary job = new ReadTextLibrary(fileURL);
		}
	}

	/**
	 * Class to read a text library in a new thread.
	 * For a non-interactive script, use ReadTextLibrary job = new ReadTextLibrary(filename).
	 */
	protected static class ReadTextLibrary extends Job
	{
		URL fileURL;
		protected ReadTextLibrary(URL fileURL)
		{
			super("Read Text Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.fileURL = fileURL;
			startJob();
		}

		public void doIt()
		{
			Library lib = Input.readLibrary(fileURL, Input.ImportType.TEXT);
			Undo.noUndoAllowed();
			if (lib == null) return;
			lib.setCurrent();
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

	public static void closeLibraryCommand(Library lib)
	{
		int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(), "Are you sure you want to delete library " + lib.getLibName() + "?");
		if (response != JOptionPane.YES_OPTION) return;
		String libName = lib.getLibName();
		if (lib.kill())
			System.out.println("Library " + libName + " deleted");
		ExplorerTree.explorerTreeChanged();
		EditWindow.repaintAll();
	}

	/**
	 * This method implements the command to save a library.
	 * It is interactive, and pops up a dialog box.
     * @return true if library saved, false otherwise.
	 */
	public static boolean saveLibraryCommand(Library lib)
	{
		String fileName;
		if (lib.isFromDisk())
		{
			fileName = lib.getLibFile();
		} else
		{
			fileName = OpenFile.chooseOutputFile(OpenFile.ELIB, null, lib.getLibName()+".elib");
			if (fileName == null) return false;

			Library.Name n = Library.Name.newInstance(fileName);
			n.setExtension("elib");
			lib.setLibFile(n.makeName());
			lib.setLibName(n.getName());
		}
		SaveLibrary job = new SaveLibrary(lib);
        return true;
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
			startJob();
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
		saveLibraryCommand(lib);
	}

	/**
	 * This method implements the export cell command for different export types.
	 * It is interactive, and pops up a dialog box.
	 */
	public static void exportCellCommand(Output.ExportType type, OpenFile.EFileFilter filter)
	{
		if (type == Output.ExportType.POSTSCRIPT)
		{
			if (OutputPostScript.syncAll()) return;
		}
		Cell cell = Library.needCurCell();
		if (cell == null) return;

		String [] extensions = filter.getExtensions();
		String filePath = OpenFile.chooseOutputFile(filter, null, cell.getProtoName() + "." + extensions[0]);
		if (filePath == null) return;

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
			startJob();
		}
		
		public void doIt() 
		{
			Output.writeCell(cell, filePath, type);
		}
		
	}

	/**
	 * This method implements the command to print the current window.
	 */
	public static void printCommand()
	{
		Cell printCell = Library.needCurCell();
		if (printCell == null) return;

		PrinterJob pj = PrinterJob.getPrinterJob();
		pj.setJobName("Cell "+printCell.describe());
		ElectricPrinter ep = new ElectricPrinter();
		ep.setPrintCell(printCell);
		pj.setPrintable(ep);

		// see if a default printer should be mentioned
		String pName = Output.getPrinterName();
		PrintService [] printers = PrintServiceLookup.lookupPrintServices(null, null);
		PrintService printerToUse = null;
		for(int i=0; i<printers.length; i++)
		{
			if (pName.equals(printers[i].getName()))
			{
				printerToUse = printers[i];
				break;
			}
		}
		if (printerToUse != null)
		{
			try
			{
				pj.setPrintService(printerToUse);
			} catch (PrinterException e)
			{
			}
		}

		if (pj.printDialog())
		{
			printerToUse = pj.getPrintService();
			if (printerToUse != null)
				Output.setPrinterName(printerToUse.getName());
			PrintJob job = new PrintJob(printCell, pj);
		}
	}

	/**
	 * Class to print a cell in a new thread.
	 */
	protected static class PrintJob extends Job
	{
		Cell cell;
		PrinterJob pj;

		public PrintJob(Cell cell, PrinterJob pj)
		{
			super("Print "+cell.describe(), User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.pj = pj;
			startJob();
		}

		public void doIt() 
		{
			try {
				pj.print();
			} catch (PrinterException pe)
			{
				System.out.println("Print aborted.");
			}
		}
	}

	static class ElectricPrinter implements Printable
	{
		private Cell printCell;
		private Image img = null;

		public void setPrintCell(Cell printCell) { this.printCell = printCell; }

		public int print(Graphics g, PageFormat pageFormat, int page)
			throws java.awt.print.PrinterException
		{
			if (page != 0) return Printable.NO_SUCH_PAGE;

			// create an EditWindow for rendering this cell
			if (img == null)
			{
				EditWindow w = EditWindow.CreateElectricDoc(null, null);
				int iw = (int)pageFormat.getImageableWidth();
				int ih = (int)pageFormat.getImageableHeight();
				w.setScreenSize(new Dimension(iw, ih));
				w.setCell(printCell, VarContext.globalContext);
				PixelDrawing offscreen = w.getOffscreen();
				offscreen.setBackgroundColor(Color.WHITE);
				offscreen.drawImage();
				img = offscreen.getImage();
			}

			// copy the image to the page
			int ix = (int)pageFormat.getImageableX();
			int iy = (int)pageFormat.getImageableY();
			g.drawImage(img, ix, iy, null);
			return Printable.PAGE_EXISTS;
		}
	}

	/**
	 * This method implements the command to quit Electric.
	 */
	public static void quitCommand()
	{
		if (preventLoss(null, 0)) return;
		System.exit(0);
	}

	/**
	 * Method to ensure that one or more libraries are saved.
	 * @param desiredLib the library to check for being saved.
	 * If desiredLib is null, all libraries are checked.
	 * @param action the type of action that will occur:
	 * 0: quit;
	 * 1: close a library;
	 * 2: replace a library.
	 * @return true if the operation should be aborted;
	 * false to continue with the quit/close/replace.
	 */
	public static boolean preventLoss(Library desiredLib, int action)
	{
        boolean saveCancelled = false;
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			if (desiredLib != null && desiredLib != lib) continue;
			if (lib.isHidden()) continue;
			if (!lib.isChangedMajor() && !lib.isChangedMinor()) continue;

			// warn about this library
			String how = "significantly";
			if (!lib.isChangedMajor()) how = "insignificantly";

			String theAction = "Save before quitting?";
			if (action == 1) theAction = "Save before closing?"; else
				if (action == 2) theAction = "Save before replacing?";
			String [] options = {"Yes", "No", "Cancel", "No to All"};
			int ret = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(),
				"Library " + lib.getLibName() + " has changed " + how + ".  " + theAction,
				"Save Library?", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
				null, options, options[0]);
			if (ret == 0)
			{
				// save the library
				if (!saveLibraryCommand(lib))
                    saveCancelled = true;
				continue;
			}
			if (ret == 1) continue;
			if (ret == 2) return true;
			if (ret == 3) break;
		}
        if (saveCancelled) return true;
		return false;
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
			startJob();
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
			startJob();
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
				if (!ai.isRigid())
				{
					ai.setRigid();
					numSet++;
				}
			}
		}
		if (numSet == 0) System.out.println("No arcs made Rigid"); else
		{
			System.out.println("Made " + numSet + " arcs Rigid");
			EditWindow.repaintAll();
		}
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
				if (ai.isRigid())
				{
					ai.clearRigid();
					numSet++;
				}
			}
		}
		if (numSet == 0) System.out.println("No arcs made Non-Rigid"); else
		{
			System.out.println("Made " + numSet + " arcs Non-Rigid");
			EditWindow.repaintAll();
		}
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
				if (!ai.isFixedAngle())
				{
					ai.setFixedAngle();
					numSet++;
				}
			}
		}
		if (numSet == 0) System.out.println("No arcs made Fixed-Angle"); else
		{
			System.out.println("Made " + numSet + " arcs Fixed-Angle");
			EditWindow.repaintAll();
		}
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
				if (ai.isFixedAngle())
				{
					ai.clearFixedAngle();
					numSet++;
				}
			}
		}
		if (numSet == 0) System.out.println("No arcs made Not-Fixed-Angle"); else
		{
			System.out.println("Made " + numSet + " arcs Not-Fixed-Angle");
			EditWindow.repaintAll();
		}
	}

	/**
	 * This method sets the highlighted arcs to be negated.
	 */
	public static void arcNegatedCommand()
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
				if (!ai.isNegated())
				{
					ai.setNegated();
					numSet++;
				}
			}
		}
		if (numSet == 0) System.out.println("No arcs negated"); else
		{
			System.out.println("Negated " + numSet + " arcs");
			EditWindow.repaintAllContents();
		}
	}

	/**
	 * This method sets the highlighted arcs to be Directional.
	 */
	public static void arcDirectionalCommand()
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
				if (!ai.isDirectional())
				{
					ai.setDirectional();
					numSet++;
				}
			}
		}
		if (numSet == 0) System.out.println("No arcs made Directional"); else
		{
			System.out.println("Made " + numSet + " arcs Directional");
			EditWindow.repaintAllContents();
		}
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
            if (c != null) c.getInfo();
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

	/**
	 * This method implements the command to highlight all objects in the current Cell.
	 */
	public static void selectAllCommand()
	{
		doSelection(false, false);
	}

	/**
	 * This method implements the command to highlight all objects in the current Cell
	 * that are easy to select.
	 */
	public static void selectEasyCommand()
	{
		doSelection(true, false);
	}

	/**
	 * This method implements the command to highlight all objects in the current Cell
	 * that are hard to select.
	 */
	public static void selectHardCommand()
	{
		doSelection(false, true);
	}

	private static void doSelection(boolean mustBeEasy, boolean mustBeHard)
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;

		boolean cellsAreHard = !User.isEasySelectionOfCellInstances();
		Highlight.clear();
		for(Iterator it = curCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			boolean hard = ni.isHardSelect();
			if ((ni.getProto() instanceof Cell) && cellsAreHard) hard = true;
			if (mustBeEasy && hard) continue;
			if (mustBeHard && !hard) continue;
			if (ni.isInvisiblePinWithText())
			{
				for(Iterator vIt = ni.getVariables(); vIt.hasNext(); )
				{
					Variable var = (Variable)vIt.next();
					if (var.isDisplay())
					{
						Highlight.addText(ni, curCell, var, null);
						break;
					}
				}
			} else
			{
				Highlight.addElectricObject(ni, curCell);
			}
		}
		for(Iterator it = curCell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			boolean hard = ai.isHardSelect();
			if (mustBeEasy && hard) continue;
			if (mustBeHard && !hard) continue;
			Highlight.addElectricObject(ai, curCell);
		}
		Highlight.finished();
	}

	/**
	 * This method implements the command to highlight all objects in the current Cell
	 * that are like the currently selected object.
	 */
	public static void selectAllLikeThisCommand()
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;

		HashMap likeThis = new HashMap();
		List highlighted = Highlight.getHighlighted(true, true);
		for(Iterator it = highlighted.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
				likeThis.put(ni.getProto(), ni);
			} else
			{
				ArcInst ai = (ArcInst)geom;
				likeThis.put(ai.getProto(), ai);
			}
		}

		Highlight.clear();
		for(Iterator it = curCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			Object isLikeThis = likeThis.get(ni.getProto());
			if (isLikeThis == null) continue;
			if (ni.isInvisiblePinWithText())
			{
				for(Iterator vIt = ni.getVariables(); vIt.hasNext(); )
				{
					Variable var = (Variable)vIt.next();
					if (var.isDisplay())
					{
						Highlight.addText(ni, curCell, var, null);
						break;
					}
				}
			} else
			{
				Highlight.addElectricObject(ni, curCell);
			}
		}
		for(Iterator it = curCell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			Object isLikeThis = likeThis.get(ai.getProto());
			if (isLikeThis == null) continue;
			Highlight.addElectricObject(ai, curCell);
		}
		Highlight.finished();
	}

	/**
	 * This method implements the command to highlight nothing in the current Cell.
	 */
	public static void selectNothingCommand()
	{
		Highlight.clear();
		Highlight.finished();
	}

	/**
	 * This method implements the command to make all selected objects be easy-to-select.
	 */
	public static void selectMakeEasyCommand()
	{
		List highlighted = Highlight.getHighlighted(true, true);
		for(Iterator it = highlighted.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
				ni.clearHardSelect();
			} else
			{
				ArcInst ai = (ArcInst)geom;
				ai.clearHardSelect();
			}
		}
	}

	/**
	 * This method implements the command to make all selected objects be hard-to-select.
	 */
	public static void selectMakeHardCommand()
	{
		List highlighted = Highlight.getHighlighted(true, true);
		for(Iterator it = highlighted.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
				ni.setHardSelect();
			} else
			{
				ArcInst ai = (ArcInst)geom;
				ai.setHardSelect();
			}
		}
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

    public static void cellBrowserCommand(CellBrowser.DoAction action)
    {
        CellBrowser dialog = new CellBrowser(TopLevel.getCurrentJFrame(), true, action);
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
		int levels = TextUtils.atoi((String)obj);

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
		int levels = TextUtils.atoi((String)obj);

		List list = Highlight.getHighlighted(true, false);
		CircuitChanges.ExpandUnExpand job = new CircuitChanges.ExpandUnExpand(list, true, levels);
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

	/**
	 * This method implements the command to toggle the display of the grid.
	 */
	public static void toggleGridCommand()
	{
		EditWindow wnd = EditWindow.getCurrent();
		if (wnd != null)
		{
			wnd.setGrid(!wnd.isGrid());
		}
	}

	/**
	 * This method implements the command to tile the windows horizontally.
	 */
	public static void tileHorizontallyCommand()
	{
		// get the overall area in which to work
		Rectangle tileArea = getWindowArea();

		// tile the windows in this area
		int numWindows = WindowFrame.getNumWindows();
		int windowHeight = tileArea.height / numWindows;
		int i=0;
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			Rectangle windowArea = new Rectangle(tileArea.x, tileArea.y + i*windowHeight, tileArea.width, windowHeight);
			i++;
			wf.setWindowSize(windowArea);
		}
	}

	/**
	 * This method implements the command to tile the windows vertically.
	 */
	public static void tileVerticallyCommand()
	{
		// get the overall area in which to work
		Rectangle tileArea = getWindowArea();

		// tile the windows in this area
		int numWindows = WindowFrame.getNumWindows();
		int windowWidth = tileArea.width / numWindows;
		int i=0;
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			Rectangle windowArea = new Rectangle(tileArea.x + i*windowWidth, tileArea.y, windowWidth, tileArea.height);
			i++;
			wf.setWindowSize(windowArea);
		}
	}

	/**
	 * This method implements the command to tile the windows cascaded.
	 */
	public static void cascadeWindowsCommand()
	{
		// cascade the windows in this area
		int numWindows = WindowFrame.getNumWindows();
		if (numWindows <= 1)
		{
			tileVerticallyCommand();
			return;
		}

		// get the overall area in which to work
		Rectangle tileArea = getWindowArea();
		int windowWidth = tileArea.width * 3 / 4;
		int windowHeight = tileArea.height * 3 / 4;
		int windowSpacing = Math.min(tileArea.width - windowWidth, tileArea.height - windowHeight) / (numWindows-1);
		int numRuns = 1;
		if (windowSpacing < 70)
		{
			numRuns = 70 / windowSpacing;
			if (70 % windowSpacing != 0) numRuns++;
			windowSpacing *= numRuns;
		}
		int windowXSpacing = (tileArea.width - windowWidth) / (numWindows-1) * numRuns;
		int windowYSpacing = (tileArea.height - windowHeight) / (numWindows-1) * numRuns;
		int i=0;
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			int index = i / numRuns;
			Rectangle windowArea = new Rectangle(tileArea.x + index*windowXSpacing,
				tileArea.y + index*windowYSpacing, windowWidth, windowHeight);
			i++;
			wf.setWindowSize(windowArea);
		}
	}

	private static Rectangle getWindowArea()
	{
		// get the overall area in which to work
		Dimension sz = TopLevel.getScreenSize();
		Rectangle tileArea = new Rectangle(sz);

		// remove the tool palette
		PaletteFrame pf = TopLevel.getPaletteFrame();
		Rectangle pb = pf.getPaletteLocation();
		removeOccludingRectangle(tileArea, pb);

		// remove the messages window
		MessagesWindow mw = TopLevel.getMessagesWindow();
		Rectangle mb = mw.getMessagesLocation();
		removeOccludingRectangle(tileArea, mb);
		return tileArea;
	}

	private static void removeOccludingRectangle(Rectangle screen, Rectangle occluding)
	{
		int lX = (int)screen.getMinX();
		int hX = (int)screen.getMaxX();
		int lY = (int)screen.getMinY();
		int hY = (int)screen.getMaxY();
		if (occluding.width > occluding.height)
		{
			// horizontally occluding window
			if (occluding.getMaxY() - lY < hY - occluding.getMinY())
			{
				// occluding window on top
				lY = (int)occluding.getMaxY();
			} else
			{
				// occluding window on bottom
				hY = (int)occluding.getMinY();
			}
		} else
		{
			if (occluding.getMaxX() - lX < hX - occluding.getMinX())
			{
				// occluding window on left
				lX = (int)occluding.getMaxX();
			} else
			{
				// occluding window on right
				hX = (int)occluding.getMinX();
			}
		}
		screen.width = hX - lX;   screen.height = hY - lY;
		screen.x = lX;            screen.y = lY;
	}

	/**
	 * This method implements the command to control Layer visibility.
	 */
	public static void layerVisibilityCommand()
	{
 		LayerVisibility dialog = new LayerVisibility(TopLevel.getCurrentJFrame(), false);
		dialog.show();
	}

    public static void moveToOtherDisplayCommand()
    {
        // this only works in SDI mode
        if (TopLevel.isMDIMode()) return;
        
        // find current screen
		EditWindow curEdit = EditWindow.getCurrent();
        WindowFrame curWF = WindowFrame.getCurrentWindowFrame();
        GraphicsConfiguration curConfig = curEdit.getGraphicsConfiguration();
        GraphicsDevice curDevice = curConfig.getDevice();
        
        // get all screens
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        // find screen after current screen
        int i;
        for (i=0; i<gs.length; i++) {
            if (gs[i] == curDevice) break;
        }
        if (i == (gs.length - 1)) i = 0; else i++;      // go to next device

        curWF.moveEditWindow(gs[i].getDefaultConfiguration());
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

	public static void irsimNetlistCommand()
	{
		EditWindow curEdit = EditWindow.getCurrent();
		if (curEdit == null) {
			System.out.println("Please select valid window first");
			return;
		}
		IRSIMTool.tool.netlistCell(curEdit.getCell(), curEdit.getVarContext(), curEdit);
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

	protected static class MakeTemplate extends Job
	{
		private Variable.Key templateKey;

		protected MakeTemplate(Variable.Key templateKey)
		{
			super("Make template", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.templateKey = templateKey;
			startJob();
		}

		public void doIt()
		{
			Cell cell = Library.needCurCell();
			if (cell == null) return;
			Variable templateVar = cell.getVar(templateKey);
			if (templateVar != null)
			{
				System.out.println("This cell already has a template");
				return;
			}
			templateVar = cell.newVar(templateKey, "*Undefined");
			if (templateVar != null)
			{
				templateVar.setDisplay();
				TextDescriptor td = templateVar.getTextDescriptor();
				td.setInterior();
				td.setDispPart(TextDescriptor.DispPos.NAMEVALUE);
			}
		}
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
			startJob();
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
		URL url = TextUtils.makeURLToFile("/export/gainsley/soesrc_java/test/purpleFour.elib");
		ReadBinaryLibrary job = new ReadBinaryLibrary(url);
//		OpenBinLibraryThread oThread = new OpenBinLibraryThread("/export/gainsley/soesrc_java/test/purpleFour.elib");
//		oThread.start();
	}

	public static void whitDiffieCommand()
	{
		MakeWhitDesign job = new MakeWhitDesign();
	}

	protected static class MakeWhitDesign extends Job
	{
		protected MakeWhitDesign()
		{
			super("Make Whit design", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			startJob();
		}

		public void doIt()
		{
			String [] theStrings =
			{
				"a1f0f1k0p0", "a2f1f2k1p1", "a3f2f3k2p2", "a0a4f0f3f4k3p3", "a0a5f0f4f5k4p4", "a6f5f6k5p5", "a0a7f0f6f7k6p6", "a0f0f7k7p7",
				"a0f1k0k1p0", "a1f2k1k2p1", "a2f3k2k3p2", "a3f0f4k0k3k4p3", "a4f0f5k0k4k5p4", "a5f6k5k6p5", "a6f0f7k0k6k7p6", "a7f0k0k7p7",
				"a0f0k1p0p1", "a1f1k2p1p2", "a2f2k3p2p3", "a3f3k0k4p0p3p4", "a4f4k0k5p0p4p5", "a5f5k6p5p6", "a6f6k0k7p0p6p7", "a7f7k0p0p7",
				"a0a1f0k0p1", "a1a2f1k1p2", "a2a3f2k2p3", "a0a3a4f3k3p0p4", "a0a4a5f4k4p0p5", "a5a6f5k5p6", "a0a6a7f6k6p0p7", "a0a7f7k7p0",

				"e1j0j1o0d0", "e2j1j2o1d1", "e3j2j3o2d2", "e0e4j0j3j4o3d3", "e0e5j0j4j5o4d4", "e6j5j6o5d5", "e0e7j0j6j7o6d6", "e0j0j7o7d7",
				"e0j1o0o1d0", "e1j2o1o2d1", "e2j3o2o3d2", "e3j0j4o0o3o4d3", "e4j0j5o0o4o5d4", "e5j6o5o6d5", "e6j0j7o0o6o7d6", "e7j0o0o7d7",
				"e0j0o1d0d1", "e1j1o2d1d2", "e2j2o3d2d3", "e3j3o0o4d0d3d4", "e4j4o0o5d0d4d5", "e5j5o6d5d6", "e6j6o0o7d0d6d7", "e7j7o0d0d7",
				"e0e1j0o0d1", "e1e2j1o1d2", "e2e3j2o2d3", "e0e3e4j3o3d0d4", "e0e4e5j4o4d0d5", "e5e6j5o5d6", "e0e6e7j6o6d0d7", "e0e7j7o7d0",

				"i1n0n1c0h0", "i2n1n2c1h1", "i3n2n3c2h2", "i0i4n0n3n4c3h3", "i0i5n0n4n5c4h4", "i6n5n6c5h5", "i0i7n0n6n7c6h6", "i0n0n7c7h7",
				"i0n1c0c1h0", "i1n2c1c2h1", "i2n3c2c3h2", "i3n0n4c0c3c4h3", "i4n0n5c0c4c5h4", "i5n6c5c6h5", "i6n0n7c0c6c7h6", "i7n0c0c7h7",
				"i0n0c1h0h1", "i1n1c2h1h2", "i2n2c3h2h3", "i3n3c0c4h0h3h4", "i4n4c0c5h0h4h5", "i5n5c6h5h6", "i6n6c0c7h0h6h7", "i7n7c0h0h7",
				"i0i1n0c0h1", "i1i2n1c1h2", "i2i3n2c2h3", "i0i3i4n3c3h0h4", "i0i4i5n4c4h0h5", "i5i6n5c5h6", "i0i6i7n6c6h0h7", "i0i7n7c7h0",

				"m1b0b1g0l0", "m2b1b2g1l1", "m3b2b3g2l2", "m0m4b0b3b4g3l3", "m0m5b0b4b5g4l4", "m6b5b6g5l5", "m0m7b0b6b7g6l6", "m0b0b7g7l7",
				"m0b1g0g1l0", "m1b2g1g2l1", "m2b3g2g3l2", "m3b0b4g0g3g4l3", "m4b0b5g0g4g5l4", "m5b6g5g6l5", "m6b0b7g0g6g7l6", "m7b0g0g7l7",
				"m0b0g1l0l1", "m1b1g2l1l2", "m2b2g3l2l3", "m3b3g0g4l0l3l4", "m4b4g0g5l0l4l5", "m5b5g6l5l6", "m6b6g0g7l0l6l7", "m7b7g0l0l7",
				"m0m1b0g0l1", "m1m2b1g1l2", "m2m3b2g2l3", "m0m3m4b3g3l0l4", "m0m4m5b4g4l0l5", "m5m6b5g5l6", "m0m6m7b6g6l0l7", "m0m7b7g7l0"

//				"a1f0f1k0p0", "a2f1f2k1p1", "a3f2f3k2p2", "a0a4f0f3f4k3p3", "a0a5f0f4f5k4p4", "a6f5f6k5p5", "a0a7f0f6f7k6p6", "a0f0f7k7p7",
//				"a0f1k0k1p0", "a1f2k1k2p1", "a2f3k2k3p2", "a3f0f4k0k3k4p3", "a4f0f5k0k4k5p4", "a5f6k5k6p5", "a6f0f7k0k6k7p6", "a7f0k0k7p7",
//				"a0f0k1p0p1", "a1f1k2p1p2", "a2f2k3p2p3", "a3f3k0k4p0p3p4", "a4f4k0k5p0p4p5", "a5f5k6p5p6", "a6f6k0k7p0p6p7", "a7f7k0p0p7",
//				"a0a1f0k0p1", "a1a2f1k1p2", "a2a3f2k2p3", "a0a3a4f3k3p0p4", "a0a4a5f4k4p0p5", "a5a6f5k5p6", "a0a6a7f6k6p0p7", "a0a7f7k7p0",
//				"b1g0g1l0m0", "b2g1g2l1m1", "b3g2g3l2m2", "b0b4g0g3g4l3m3", "b0b5g0g4g5l4m4", "b6g5g6l5m5", "b0b7g0g6g7l6m6", "b0g0g7l7m7",
//				"b0g1l0l1m0", "b1g2l1l2m1", "b2g3l2l3m2", "b3g0g4l0l3l4m3", "b4g0g5l0l4l5m4", "b5g6l5l6m5", "b6g0g7l0l6l7m6", "b7g0l0l7m7",
//				"b0g0l1m0m1", "b1g1l2m1m2", "b2g2l3m2m3", "b3g3l0l4m0m3m4", "b4g4l0l5m0m4m5", "b5g5l6m5m6", "b6g6l0l7m0m6m7", "b7g7l0m0m7",
//				"b0b1g0l0m1", "b1b2g1l1m2", "b2b3g2l2m3", "b0b3b4g3l3m0m4", "b0b4b5g4l4m0m5", "b5b6g5l5m6", "b0b6b7g6l6m0m7", "b0b7g7l7m0",
//				"c1h0h1i0n0", "c2h1h2i1n1", "c3h2h3i2n2", "c0c4h0h3h4i3n3", "c0c5h0h4h5i4n4", "c6h5h6i5n5", "c0c7h0h6h7i6n6", "c0h0h7i7n7",
//				"c0h1i0i1n0", "c1h2i1i2n1", "c2h3i2i3n2", "c3h0h4i0i3i4n3", "c4h0h5i0i4i5n4", "c5h6i5i6n5", "c6h0h7i0i6i7n6", "c7h0i0i7n7",
//				"c0h0i1n0n1", "c1h1i2n1n2", "c2h2i3n2n3", "c3h3i0i4n0n3n4", "c4h4i0i5n0n4n5", "c5h5i6n5n6", "c6h6i0i7n0n6n7", "c7h7i0n0n7",
//				"c0c1h0i0n1", "c1c2h1i1n2", "c2c3h2i2n3", "c0c3c4h3i3n0n4", "c0c4c5h4i4n0n5", "c5c6h5i5n6", "c0c6c7h6i6n0n7", "c0c7h7i7n0",
//				"d1e0e1j0o0", "d2e1e2j1o1", "d3e2e3j2o2", "d0d4e0e3e4j3o3", "d0d5e0e4e5j4o4", "d6e5e6j5o5", "d0d7e0e6e7j6o6", "d0e0e7j7o7",
//				"d0e1j0j1o0", "d1e2j1j2o1", "d2e3j2j3o2", "d3e0e4j0j3j4o3", "d4e0e5j0j4j5o4", "d5e6j5j6o5", "d6e0e7j0j6j7o6", "d7e0j0j7o7",
//				"d0e0j1o0o1", "d1e1j2o1o2", "d2e2j3o2o3", "d3e3j0j4o0o3o4", "d4e4j0j5o0o4o5", "d5e5j6o5o6", "d6e6j0j7o0o6o7", "d7e7j0o0o7",
//				"d0d1e0j0o1", "d1d2e1j1o2", "d2d3e2j2o3", "d0d3d4e3j3o0o4", "d0d4d5e4j4o0o5", "d5d6e5j5o6", "d0d6d7e6j6o0o7", "d0d7e7j7o0"
			};

			for(int v=0; v<33; v++)
			{
				String title = "whit";
				if (v < 16) title += "Input" + (v+1); else
					if (v < 32) title += "Output" + (v-15);
				Cell myCell = Cell.newInstance(Library.getCurrent(), title+"{sch}");

				// create the input and output pins
				NodeProto pinNp = com.sun.electric.technology.technologies.Generic.tech.universalPinNode;
				NodeInst [] inputs = new NodeInst[128];
				NodeInst [] outputs = new NodeInst[128];
				for(int i=0; i<128; i++)
				{
					inputs[i] = NodeInst.newInstance(pinNp, new Point2D.Double(-200.0, i*5), 0, 0, 0, myCell, null);
					Variable inVar = inputs[i].newVar("label", "Input "+(i+1));
					inVar.setDisplay();
					inVar.getTextDescriptor().setPos(TextDescriptor.Position.LEFT);
					inVar.getTextDescriptor().setRelSize(5);
					outputs[i] = NodeInst.newInstance(pinNp, new Point2D.Double(200.0, i*5), 0, 0, 0, myCell, null);
					Variable outVar = outputs[i].newVar("label", "Output "+(i+1));
					outVar.setDisplay();
					outVar.getTextDescriptor().setPos(TextDescriptor.Position.RIGHT);
					outVar.getTextDescriptor().setRelSize(5);
				}

				// wire them together
				ArcProto wire = com.sun.electric.technology.technologies.Generic.tech.universal_arc;
				for(int i=0; i<theStrings.length; i++)
				{
					PortInst outPort = outputs[i].getOnlyPortInst();
					int len = theStrings[i].length();
					for(int j=0; j<len; j+=2)
					{
						char letter = theStrings[i].charAt(j);
						char number = theStrings[i].charAt(j+1);
						int index = (letter - 'a')*8 + (number - '0');
						if (v < 16)
						{
							// only interested in the proper letter
							if (v + 'a' != letter) continue;
						} else if (v < 32)
						{
							if (i/8 != v-16) continue;
						}
						PortInst inPort = inputs[index].getOnlyPortInst();
						ArcInst.newInstance(wire, 0, inPort, outPort, null);
					}
				}

				// display the full drawing
				if (v == 32) WindowFrame.createEditWindow(myCell);
			}
		}
	}

}
