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
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.PolyMerge;
import com.sun.electric.database.geometry.PolyQTree;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.erc.ERCWellCheck;
import com.sun.electric.tool.erc.ERCAntenna;
import com.sun.electric.tool.generator.PadGenerator;
import com.sun.electric.tool.generator.layout.Loco;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.io.input.Simulate;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.io.output.PostScript;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.io.output.Verilog;
import com.sun.electric.tool.logicaleffort.LENetlister;
import com.sun.electric.tool.logicaleffort.LETool;
import com.sun.electric.tool.routing.AutoStitch;
import com.sun.electric.tool.routing.MimicStitch;
import com.sun.electric.tool.simulation.IRSIMTool;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.dialogs.*;
import com.sun.electric.tool.user.help.HelpViewer;
import com.sun.electric.tool.user.ui.MenuBar;
import com.sun.electric.tool.user.ui.MenuBar.Menu;
import com.sun.electric.tool.user.ui.MenuBar.MenuItem;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.PixelDrawing;
import com.sun.electric.tool.user.ui.MessagesWindow;
import com.sun.electric.tool.user.ui.PaletteFrame;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;
import com.sun.electric.tool.user.ui.SizeListener;
import com.sun.electric.tool.user.ui.TextWindow;
import com.sun.electric.tool.user.ui.WaveformWindow;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.ZoomAndPanListener;
import com.sun.electric.Main;

import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.Image;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;
import java.awt.print.PrinterJob;
import java.awt.print.Printable;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.print.PrintServiceLookup;
import javax.print.PrintService;
import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.JOptionPane;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.text.DecimalFormat;

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
	public static MenuBar createMenuBar()
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
		fileMenu.addMenuItem("I/O Options...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { IOOptions.ioOptionsCommand(); } });

		fileMenu.addSeparator();

		fileMenu.addMenuItem("Close Library", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { closeLibraryCommand(Library.getCurrent()); } });
		fileMenu.addMenuItem("Save Library", KeyStroke.getKeyStroke('S', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveLibraryCommand(Library.getCurrent(), OpenFile.Type.ELIB); } });
		fileMenu.addMenuItem("Save Library as...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveAsLibraryCommand(); } });
		fileMenu.addMenuItem("Save All Libraries",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveAllLibrariesCommand(); } });
		Menu exportSubMenu = new Menu("Export");
		fileMenu.add(exportSubMenu);
		exportSubMenu.addMenuItem("CIF (Caltech Intermediate Format)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.CIF, false); } });
		exportSubMenu.addMenuItem("GDS II (Stream)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.GDS, false); } });
		exportSubMenu.addMenuItem("PostScript", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.POSTSCRIPT, false); } });
		exportSubMenu.addMenuItem("Readable Dump", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveLibraryCommand(Library.getCurrent(), OpenFile.Type.READABLEDUMP); } });

		fileMenu.addSeparator();

		fileMenu.addMenuItem("Change Current Library...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.changeCurrentLibraryCommand(); } });
		fileMenu.addMenuItem("List Libraries", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.listLibrariesCommand(); } });
		fileMenu.addMenuItem("Rename Library...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.renameLibraryCommand(); } });
		fileMenu.addMenuItem("Mark All Libraries for Saving", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.markAllLibrariesForSavingCommand(); } });
		fileMenu.addMenuItem("Repair Libraries", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.checkAndRepairCommand(); } });

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
			new ActionListener() { public void actionPerformed(ActionEvent e) { Clipboard.cut(); } });
		editMenu.addMenuItem("Copy", KeyStroke.getKeyStroke('C', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Clipboard.copy(); } });
		editMenu.addMenuItem("Paste", KeyStroke.getKeyStroke('V', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Clipboard.paste(); } });
        editMenu.addMenuItem("Duplicate", KeyStroke.getKeyStroke('M', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { Clipboard.duplicate(); } });

		editMenu.addSeparator();

		Menu arcSubMenu = new Menu("Arc", 'A');
		editMenu.add(arcSubMenu);
		arcSubMenu.addMenuItem("Rigid", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcRigidCommand(); }});
		arcSubMenu.addMenuItem("Not Rigid", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcNotRigidCommand(); }});
		arcSubMenu.addMenuItem("Fixed Angle", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcFixedAngleCommand(); }});
		arcSubMenu.addMenuItem("Not Fixed Angle", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcNotFixedAngleCommand(); }});
		arcSubMenu.addSeparator();
		arcSubMenu.addMenuItem("Toggle Directionality", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcDirectionalCommand(); }});
		arcSubMenu.addMenuItem("Toggle Ends Extension", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcEndsExtendCommand(); }});
		arcSubMenu.addMenuItem("Reverse", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcReverseCommand(); }});
		arcSubMenu.addMenuItem("Toggle Head-Skip", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcSkipHeadCommand(); }});
		arcSubMenu.addMenuItem("Toggle Tail-Skip", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcSkipTailCommand(); }});
		arcSubMenu.addSeparator();
		arcSubMenu.addMenuItem("Rip Bus", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.ripBus(); }});

		editMenu.addSeparator();

		editMenu.addMenuItem("Undo", KeyStroke.getKeyStroke('Z', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { undoCommand(); } });
		editMenu.addMenuItem("Redo", KeyStroke.getKeyStroke('Y', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { redoCommand(); } });

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

		Menu sizeSubMenu = new Menu("Size", 'S');
		editMenu.add(sizeSubMenu);
		sizeSubMenu.addMenuItem("Interactively", KeyStroke.getKeyStroke('B', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { SizeListener.sizeObjects(); } });
		sizeSubMenu.addMenuItem("All Selected Nodes...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { SizeListener.sizeAllNodes(); }});
		sizeSubMenu.addMenuItem("All Selected Arcs...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { SizeListener.sizeAllArcs(); }});


		Menu moveSubMenu = new Menu("Move", 'V');
		editMenu.add(moveSubMenu);
		moveSubMenu.addMenuItem("Spread...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Spread.showSpreadDialog(); }});
		moveSubMenu.addMenuItem("Move Objects By...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { MoveBy.showMoveByDialog(); }});
		moveSubMenu.addMenuItem("Align to Grid", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.alignToGrid(); }});
		moveSubMenu.addSeparator();
		moveSubMenu.addMenuItem("Align Horizontally to Left", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.alignNodes(true, 0); }});
		moveSubMenu.addMenuItem("Align Horizontally to Right", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.alignNodes(true, 1); }});
		moveSubMenu.addMenuItem("Align Horizontally to Center", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.alignNodes(true, 2); }});
		moveSubMenu.addSeparator();
		moveSubMenu.addMenuItem("Align Vertically to Top", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.alignNodes(false, 0); }});
		moveSubMenu.addMenuItem("Align Vertically to Bottom", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.alignNodes(false, 1); }});
		moveSubMenu.addMenuItem("Align Vertically to Center", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.alignNodes(false, 2); }});

		editMenu.addMenuItem("Toggle Port Negation", KeyStroke.getKeyStroke('T', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.toggleNegatedCommand(); }});

		editMenu.addSeparator();

		m=editMenu.addMenuItem("Erase", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.deleteSelected(); } });
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), null);
		editMenu.addMenuItem("Erase Geometry", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.deleteSelectedGeometry(); } });

		editMenu.addSeparator();

		editMenu.addMenuItem("Edit Options...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { EditOptions.editOptionsCommand(); } });
		editMenu.addMenuItem("Key Bindings...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { keyBindingsCommand(); } });

		editMenu.addSeparator();

		editMenu.addMenuItem("Get Info...", KeyStroke.getKeyStroke('I', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { getInfoCommand(); } });
		Menu editInfoSubMenu = new Menu("Info", 'V');
		editMenu.add(editInfoSubMenu);
		editInfoSubMenu.addMenuItem("Attributes...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Attributes.showDialog(); } });
		editInfoSubMenu.addMenuItem("See All Parameters on Node", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { seeAllParametersCommand(); } });
		editInfoSubMenu.addMenuItem("Hide All Parameters on Node", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { hideAllParametersCommand(); } });
		editInfoSubMenu.addMenuItem("Default Parameter Visibility", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { defaultParamVisibilityCommand(); } });
		editInfoSubMenu.addSeparator();
		editInfoSubMenu.addMenuItem("List Layer Coverage", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { layerCoverageCommand(); } });

		editMenu.addSeparator();

		Menu modeSubMenu = new Menu("Modes");
		editMenu.add(modeSubMenu);

		Menu modeSubMenuEdit = new Menu("Edit");
		modeSubMenu.add(modeSubMenuEdit);
		ButtonGroup editGroup = new ButtonGroup();
        JMenuItem cursorClickZoomWire, cursorSelect, cursorWiring, cursorPan, cursorZoom, cursorOutline, cursorMeasure;
		cursorClickZoomWire = modeSubMenuEdit.addRadioButton(ToolBar.cursorClickZoomWireName, true, editGroup, KeyStroke.getKeyStroke('S', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.clickZoomWireCommand(); } });
        ToolBar.CursorMode cm = ToolBar.getCursorMode();
        if (cm == ToolBar.CursorMode.CLICKZOOMWIRE) cursorClickZoomWire.setSelected(true);
        if (ToolBar.secondaryInputModes) {
			cursorSelect = modeSubMenuEdit.addRadioButton(ToolBar.cursorSelectName, false, editGroup, KeyStroke.getKeyStroke('M', 0),
				new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.selectCommand(); } });
			cursorWiring = modeSubMenuEdit.addRadioButton(ToolBar.cursorWiringName, false, editGroup, KeyStroke.getKeyStroke('W', 0),
				new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.wiringCommand(); } });
            if (cm == ToolBar.CursorMode.SELECT) cursorSelect.setSelected(true);
            if (cm == ToolBar.CursorMode.WIRE) cursorWiring.setSelected(true);
        }
		cursorPan = modeSubMenuEdit.addRadioButton(ToolBar.cursorPanName, false, editGroup, KeyStroke.getKeyStroke('P', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.panCommand(); } });
		cursorZoom = modeSubMenuEdit.addRadioButton(ToolBar.cursorZoomName, false, editGroup, KeyStroke.getKeyStroke('Z', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.zoomCommand(); } });
		cursorOutline = modeSubMenuEdit.addRadioButton(ToolBar.cursorOutlineName, false, editGroup, KeyStroke.getKeyStroke('Y', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.outlineEditCommand(); } });
		cursorMeasure = modeSubMenuEdit.addRadioButton(ToolBar.cursorMeasureName, false, editGroup, KeyStroke.getKeyStroke('M', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.measureCommand(); } });
		if (cm == ToolBar.CursorMode.PAN) cursorPan.setSelected(true);
		if (cm == ToolBar.CursorMode.ZOOM) cursorZoom.setSelected(true);
		if (cm == ToolBar.CursorMode.OUTLINE) cursorOutline.setSelected(true);
		if (cm == ToolBar.CursorMode.MEASURE) cursorMeasure.setSelected(true);

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

		editMenu.addMenuItem("Array...", KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Array.showArrayDialog(); } });
		editMenu.addMenuItem("Insert Jog In Arc", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { insertJogInArcCommand(); } });
		editMenu.addMenuItem("Change...", KeyStroke.getKeyStroke('C', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Change.showChangeDialog(); } });

		Menu textSubMenu = new Menu("Text");
		editMenu.add(textSubMenu);
		textSubMenu.addMenuItem("Find Text...", KeyStroke.getKeyStroke('L', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { FindText.findTextDialog(); }});
		textSubMenu.addMenuItem("Change Text Size...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ChangeText.changeTextDialog(); }});
		textSubMenu.addMenuItem("Read Text Cell...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { TextWindow.readTextCell(); }});
		textSubMenu.addMenuItem("Save Text Cell...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { TextWindow.writeTextCell(); }});

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

		Menu specialSubMenu = new Menu("Special Function");
		editMenu.add(specialSubMenu);
		specialSubMenu.addMenuItem("Show Undo List", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { showUndoListCommand(); } });

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
		selListSubMenu.addMenuItem("Select Object...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { SelectObject.selectObjectDialog(); }});
		selListSubMenu.addMenuItem("Deselect All Arcs", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { deselectAllArcsCommand(); }});
		selListSubMenu.addSeparator();
		selListSubMenu.addMenuItem("Make Selected Easy", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectMakeEasyCommand(); }});
		selListSubMenu.addMenuItem("Make Selected Hard", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectMakeHardCommand(); }});
		selListSubMenu.addSeparator();
		selListSubMenu.addMenuItem("Enclosed Objects", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectEnclosedObjectsCommand(); }});
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

		cellMenu.addSeparator();
		cellMenu.addMenuItem("Package Into Cell...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.packageIntoCell(); } });
		cellMenu.addMenuItem("Extract Cell Instance", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.extractCells(); } });

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

		exportMenu.addMenuItem("Delete Export", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.deleteExport(); } });
		exportMenu.addMenuItem("Delete All Exports on Highlighted", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.deleteExportsOnHighlighted(); } });
		exportMenu.addMenuItem("Delete Exports in Area", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.deleteExportsInArea(); } });
		exportMenu.addMenuItem("Move Export", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.moveExport(); } });
		exportMenu.addMenuItem("Rename Export", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.renameExport(); } });

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
		viewMenu.addMenuItem("Change Cell's View...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { changeViewCommand(); } });

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

        m = windowMenu.addMenuItem("Fill Display", KeyStroke.getKeyStroke('9', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { fullDisplay(); } });
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD9, buckyBit), null);
        m = windowMenu.addMenuItem("Redisplay Window", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.redrawDisplay(); } });
        m = windowMenu.addMenuItem("Zoom Out", KeyStroke.getKeyStroke('0', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { zoomOutDisplay(); } });
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, buckyBit), null);
        m = windowMenu.addMenuItem("Zoom In", KeyStroke.getKeyStroke('7', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { zoomInDisplay(); } });
        m = windowMenu.addMenuItem("Zoom Box", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { zoomBoxCommand(); } });
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD7, buckyBit), null);
        m = windowMenu.addMenuItem("Focus on Highlighted", KeyStroke.getKeyStroke('F', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { focusOnHighlighted(); } });
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD5, buckyBit), null);

        windowMenu.addSeparator();

        m = windowMenu.addMenuItem("Pan Left", KeyStroke.getKeyStroke('4', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panX(WindowFrame.getCurrentWindowFrame(), 1); }});
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD4, buckyBit), null);
        m = windowMenu.addMenuItem("Pan Right", KeyStroke.getKeyStroke('6', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panX(WindowFrame.getCurrentWindowFrame(), -1); }});
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD6, buckyBit), null);
        m = windowMenu.addMenuItem("Pan Up", KeyStroke.getKeyStroke('8', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panY(WindowFrame.getCurrentWindowFrame(), -1); }});
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD8, buckyBit), null);
        m = windowMenu.addMenuItem("Pan Down", KeyStroke.getKeyStroke('2', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panY(WindowFrame.getCurrentWindowFrame(), 1); }});
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD2, buckyBit), null);
		Menu panningDistanceSubMenu = new Menu("Panning Distance");
		windowMenu.add(panningDistanceSubMenu);

		ButtonGroup windowPanGroup = new ButtonGroup();
		JMenuItem panSmall, panMedium, panLarge;
		panSmall = panningDistanceSubMenu.addRadioButton("Small", true, windowPanGroup, null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panningDistanceCommand(0.15); } });
		panMedium = panningDistanceSubMenu.addRadioButton("Medium", true, windowPanGroup, null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panningDistanceCommand(0.3); } });
		panLarge = panningDistanceSubMenu.addRadioButton("Large", true, windowPanGroup, null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panningDistanceCommand(0.6); } });
		panLarge.setSelected(true);

        windowMenu.addSeparator();

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
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.SPICE, true); }});
		spiceSimulationSubMenu.addMenuItem("Write CDL Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.CDL, true); }});
		spiceSimulationSubMenu.addMenuItem("Plot Spice Listing...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulate.plotSpiceResults(); }});

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
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.VERILOG, true); } });
		verilogSimulationSubMenu.addMenuItem("Plot Verilog VCD Dump...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulate.plotVerilogResults(); }});
		verilogSimulationSubMenu.addSeparator();
		verilogSimulationSubMenu.addMenuItem("Set Verilog Template", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeTemplate(Verilog.VERILOG_TEMPLATE_KEY); }});
		verilogSimulationSubMenu.addSeparator();
		Menu transistorStrengthSubMenu = new Menu("Transistor Strength", 'T');
		transistorStrengthSubMenu.addMenuItem("Weak", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.setTransistorStrengthCommand(true); }});
		transistorStrengthSubMenu.addMenuItem("Normal", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Simulation.setTransistorStrengthCommand(false); }});
		verilogSimulationSubMenu.add(transistorStrengthSubMenu);

		Menu netlisters = new Menu("Simulation (others)");
		toolMenu.add(netlisters);
		netlisters.addMenuItem("Write IRSIM Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { irsimNetlistCommand(); }});
		netlisters.addMenuItem("Write Maxwell Deck...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { exportCellCommand(OpenFile.Type.MAXWELL, true); } });

		Menu ercSubMenu = new Menu("ERC", 'E');
		toolMenu.add(ercSubMenu);
		ercSubMenu.addMenuItem("Check Wells", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ERCWellCheck.analyzeCurCell(true); } });
		ercSubMenu.addMenuItem("Antenna Check", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { new ERCAntenna(); } });

		Menu networkSubMenu = new Menu("Network", 'N');
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
		networkSubMenu.addSeparator();
		networkSubMenu.addMenuItem("Show Power and Ground", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { showPowerAndGround(); } });
		networkSubMenu.addMenuItem("Validate Power and Ground", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { validatePowerAndGround(); } });
		networkSubMenu.addMenuItem("Redo Network Numbering", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { redoNetworkNumberingCommand(); } });

		Menu logEffortSubMenu = new Menu("Logical Effort", 'L');
		logEffortSubMenu.addMenuItem("Optimize for Equal Gate Delays", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { optimizeEqualGateDelaysCommand(); }});
        logEffortSubMenu.addMenuItem("Print Info for Selected Node", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { printLEInfoCommand(); }});
		toolMenu.add(logEffortSubMenu);


		Menu routingSubMenu = new Menu("Routing", 'R');
		routingSubMenu.addMenuItem("Mimic-Stitch Now", KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { MimicStitch.mimicStitch(true); }});
	    routingSubMenu.addMenuItem("Auto-Stitch Now", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { AutoStitch.autoStitch(false, true); }});
		routingSubMenu.addMenuItem("Auto-Stitch Highlighted Now", KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { AutoStitch.autoStitch(true, true); }});
		toolMenu.add(routingSubMenu);
		routingSubMenu.addSeparator();
		routingSubMenu.addMenuItem("Get Unrouted Wire", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { getUnroutedArcCommand(); }});

		Menu generationSubMenu = new Menu("Generation", 'G');
		toolMenu.add(generationSubMenu);
		generationSubMenu.addMenuItem("Coverage Implants Generator", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { implantGeneratorCommand(true, false); }});
		generationSubMenu.addMenuItem("Pad Frame Generator", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { padFrameGeneratorCommand(); }});

		Menu compactionSubMenu = new Menu("Compaction", 'C');
		toolMenu.add(compactionSubMenu);

		toolMenu.addSeparator();

		toolMenu.addMenuItem("Tool Options...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolOptions.toolOptionsCommand(); } });
		toolMenu.addMenuItem("List Tools",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { listToolsCommand(); } });
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
		helpMenu.addMenuItem("Help Index", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { toolTipsCommand(); } });
		helpMenu.addSeparator();
		helpMenu.addMenuItem("Describe this Technology", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { describeTechnologyCommand(); } });
		helpMenu.addSeparator();
		helpMenu.addMenuItem("Make fake circuitry", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeFakeCircuitryCommand(); } });
		helpMenu.addMenuItem("Make fake simulation window", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { WaveformWindow.makeFakeWaveformCommand(); }});
//		helpMenu.addMenuItem("Whit Diffie's design...", null,
//			new ActionListener() { public void actionPerformed(ActionEvent e) { whitDiffieCommand(); } });

		/****************************** Russell's TEST MENU ******************************/

		Menu russMenu = new Menu("Russell", 'R');
		menuBar.add(russMenu);
		russMenu.addMenuItem("Generate fill cells", null, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new com.sun.electric.tool.generator.layout.FillLibGen();
			}
		});
		russMenu.addMenuItem("Gate Generator Regression", null,
		                     new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new com.sun.electric.tool.generator.layout.GateRegression();
			}
		});
		russMenu.addMenuItem("Generate gate layouts", null,
							 new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new com.sun.electric.tool.generator.layout.Loco();
			}
		});
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
        //jongMenu.addMenuItem("Check Exports", null,
        //    new ActionListener() { public void actionPerformed(ActionEvent e) { checkExports(); }});

        /****************************** Gilda's TEST MENU ******************************/
		Menu gildaMenu = new Menu("Gilda", 'G');
		menuBar.add(gildaMenu);
		gildaMenu.addMenuItem("Merge Polyons", null,
		        new ActionListener() { public void actionPerformed(ActionEvent e) {implantGeneratorCommand(true, true);}});
		gildaMenu.addMenuItem("Covering Implants Old", null,
		        new ActionListener() { public void actionPerformed(ActionEvent e) {implantGeneratorCommand(false, false);}});
		gildaMenu.addMenuItem("List Layer Coverage", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { layerCoverageCommand(); } });

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
        wiringShortcuts.addMenuItem("Switch Wiring Target", KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
                new ActionListener() { public void actionPerformed(ActionEvent e) { ClickZoomWireListener.theOne.switchWiringTarget(); }});

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
		WindowFrame.wantToRedoLibraryTree();
		EditWindow.repaintAll();
		TopLevel.getCurrentJFrame().getToolBar().setEnabled(ToolBar.SaveLibraryName, Library.getCurrent() != null);
	}

	/**
	 * This method implements the command to read a library.
	 * It is interactive, and pops up a dialog box.
	 */
	public static void openLibraryCommand()
	{
		String fileName = OpenFile.chooseInputFile(OpenFile.Type.ELIB, null);
		if (fileName != null)
		{
			// start a job to do the input
			URL fileURL = TextUtils.makeURLToFile(fileName);
			ReadELIB job = new ReadELIB(fileURL);
		}
	}

	/**
	 * Class to read a library in a new thread.
	 * For a non-interactive script, use ReadELIB job = new ReadELIB(filename).
	 */
	public static class ReadELIB extends Job
	{
		URL fileURL;

		public ReadELIB(URL fileURL)
		{
			super("Read Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.fileURL = fileURL;
			startJob();
		}

		public void doIt()
		{
			Library lib = Input.readLibrary(fileURL, OpenFile.Type.ELIB);
			Undo.noUndoAllowed();
			if (lib == null) return;
			lib.setCurrent();
			Cell cell = lib.getCurCell();
			if (cell == null)
				System.out.println("No current cell in this library");
			else
			{
				// check if edit window open with null cell, use that one if exists
				for (Iterator it = WindowFrame.getWindows(); it.hasNext(); )
				{
					WindowFrame wf = (WindowFrame)it.next();
					WindowContent content = wf.getContent();
					if (content.getCell() == null)
					{
						wf.setCellWindow(cell);
						WindowFrame.setCurrentWindowFrame(wf);
						TopLevel.getCurrentJFrame().getToolBar().setEnabled(ToolBar.SaveLibraryName, Library.getCurrent() != null);
						return;
					}
				}
				WindowFrame.createEditWindow(cell);
				// no clean for now.
				TopLevel.getCurrentJFrame().getToolBar().setEnabled(ToolBar.SaveLibraryName, Library.getCurrent() != null);
			}
		}
	}

	/**
	 * This method implements the command to import a library (Readable Dump format).
	 * It is interactive, and pops up a dialog box.
	 */
	public static void importLibraryCommand()
	{
		String fileName = OpenFile.chooseInputFile(OpenFile.Type.READABLEDUMP, null);
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
	private static class ReadTextLibrary extends Job
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
			Library lib = Input.readLibrary(fileURL, OpenFile.Type.READABLEDUMP);
			Undo.noUndoAllowed();
			if (lib == null) return;
			lib.setCurrent();
			Cell cell = lib.getCurCell();
			if (cell == null) System.out.println("No current cell in this library"); else
			{
				// check if edit window open with null cell, use that one if exists
				for (Iterator it = WindowFrame.getWindows(); it.hasNext(); )
				{
					WindowFrame wf = (WindowFrame)it.next();
					WindowContent content = wf.getContent();
					if (content instanceof EditWindow)
					{
						if (content.getCell() == null)
						{
							content.setCell(cell, VarContext.globalContext);
							return;
						}
					}
				}
				WindowFrame.createEditWindow(cell);
			}
		}
	}

	public static void closeLibraryCommand(Library lib)
	{
		int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(), "Are you sure you want to close library " + lib.getLibName() + "?");
		if (response != JOptionPane.YES_OPTION) return;
		String libName = lib.getLibName();
		WindowFrame.removeLibraryReferences(lib);
		if (lib.kill())
			System.out.println("Library " + libName + " closed");
		WindowFrame.wantToRedoLibraryTree();
		EditWindow.repaintAll();
		// Disable save icon if no more libraries are open
		TopLevel.getCurrentJFrame().getToolBar().setEnabled(ToolBar.SaveLibraryName, Library.getCurrent() != null);
	}

	/**
	 * This method implements the command to save a library.
	 * It is interactive, and pops up a dialog box.
     * @return true if library saved, false otherwise.
	 */
	public static boolean saveLibraryCommand(Library lib, OpenFile.Type type)
	{
		String [] extensions = type.getExtensions();
		String extension = extensions[0];
		String fileName;
		if (lib.isFromDisk() && type == OpenFile.Type.ELIB)
		{
			fileName = lib.getLibFile().getPath();
		} else
		{
			fileName = OpenFile.chooseOutputFile(type, null, lib.getLibName() + "." + extension);
			if (fileName == null) return false;

			int dotPos = fileName.lastIndexOf('.');
			if (dotPos < 0) fileName += "." + extension; else
			{
				if (!fileName.substring(dotPos+1).equals(extension))
				{
					fileName = fileName.substring(0, dotPos) + "." + extension;
				}
			}
		}
		SaveLibrary job = new SaveLibrary(lib, fileName, type);
        return true;
	}

	/**
	 * Class to save a library in a new thread.
	 * For a non-interactive script, use SaveLibrary job = new SaveLibrary(filename).
	 * Saves as an elib.
	 */
	public static class SaveLibrary extends Job
	{
		Library lib;
		String newName;
		OpenFile.Type type;

		public SaveLibrary(Library lib, String newName, OpenFile.Type type)
		{
			super("Write Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.lib = lib;
			this.newName = newName;
			this.type = type;
			startJob();
		}

		public void doIt()
		{
			// rename the library if requested
			if (newName != null)
			{
				URL libURL = TextUtils.makeURLToFile(newName);
				lib.setLibFile(libURL);
				lib.setLibName(TextUtils.getFileNameWithoutExtension(libURL));
			}

			boolean error = Output.writeLibrary(lib, type);
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
		saveLibraryCommand(lib, OpenFile.Type.ELIB);
	}

	/**
	 * This method implements the command to save all libraries.
	 */
	public static void saveAllLibrariesCommand()
	{
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			if (lib.isHidden()) continue;
			if (!lib.isChangedMajor() && !lib.isChangedMinor()) continue;
			if (!saveLibraryCommand(lib, OpenFile.Type.ELIB)) break;
		}
	}

	/**
	 * This method implements the export cell command for different export types.
	 * It is interactive, and pops up a dialog box.
	 */
	public static void exportCellCommand(OpenFile.Type type, boolean isNetlist)
	{
		if (type == OpenFile.Type.POSTSCRIPT)
		{
			if (PostScript.syncAll()) return;
		}
        EditWindow wnd = EditWindow.needCurrent();
        Cell cell = wnd.getCell();
        if (cell == null)
        {
        	System.out.println("No cell in this window");
        	return;
        }
        VarContext context = wnd.getVarContext();

		String [] extensions = type.getExtensions();
		String filePath = cell.getProtoName() + "." + extensions[0];
		if (User.isShowFileSelectionForNetlists() || !isNetlist)
		{
			filePath = OpenFile.chooseOutputFile(type, null, filePath);
			if (filePath == null) return;
		} else
		{
			filePath = User.getWorkingDirectory() + File.separator + filePath;
		}

		exportCellCommand(cell, context, filePath, type);
	}

	/**
	 * This is the non-interactive version of exportCellCommand
	 */
	public static void exportCellCommand(Cell cell, VarContext context, String filePath, OpenFile.Type type)
	{
		ExportCell job = new ExportCell(cell, context, filePath, type);
	}

	/**
	 * Class to export a cell in a new thread.
	 * For a non-interactive script, use
	 * ExportCell job = new ExportCell(Cell cell, String filename, Output.ExportType type).
	 * Saves as an elib.
	 */
	private static class ExportCell extends Job
	{
		Cell cell;
        VarContext context;
		String filePath;
		OpenFile.Type type;

		public ExportCell(Cell cell, VarContext context, String filePath, OpenFile.Type type)
		{
			super("Export "+cell.describe()+" ("+type+")", User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.cell = cell;
            this.context = context;
			this.filePath = filePath;
			this.type = type;
			startJob();
		}

		public void doIt()
		{
			Output.writeCell(cell, context, filePath, type);
		}

	}

	/**
	 * This method implements the command to print the current window.
	 */
	public static void printCommand()
	{
		Cell printCell = WindowFrame.needCurCell();
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
	private static class PrintJob extends Job
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
	public static boolean quitCommand()
	{
		if (preventLoss(null, 0)) return (false);
		QuitJob job = new QuitJob();
        return (true);
	}

	/**
	 * Class to quit Electric in a new thread.
	 */
	private static class QuitJob extends Job
	{
		public QuitJob()
		{
			super("Quitting", User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
			startJob();
		}

		public void doIt()
		{
			System.exit(0);
		}
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
				if (!saveLibraryCommand(lib, OpenFile.Type.ELIB))
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

    public static void undoCommand() { new UndoCommand(); }

	/**
	 * This class implement the command to undo the last change.
	 */
	private static class UndoCommand extends Job
	{
		private UndoCommand()
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

    public static void redoCommand() { new RedoCommand(); }

	/**
	 * This class implement the command to undo the last change (Redo).
	 */
	private static class RedoCommand extends Job
	{
		private RedoCommand()
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
	 * This method implements the command to show the Key Bindings Options dialog.
	 */
	public static void keyBindingsCommand()
	{
        // edit key bindings for current menu
        TopLevel top = (TopLevel)TopLevel.getCurrentJFrame();
		EditKeyBindings dialog = new EditKeyBindings(top.getTheMenuBar(), top, true);
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
			Cell c = WindowFrame.getCurrentCell();
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

	/**
	 * Method to handle the "See All Parameters on Node" command.
	 */
	public static void seeAllParametersCommand()
	{
		ParameterVisibility job = new ParameterVisibility(0);
	}

	/**
	 * Method to handle the "Hide All Parameters on Node" command.
	 */
	public static void hideAllParametersCommand()
	{
		ParameterVisibility job = new ParameterVisibility(1);
	}

	/**
	 * Method to handle the "Default Parameter Visibility" command.
	 */
	public static void defaultParamVisibilityCommand()
	{
		ParameterVisibility job = new ParameterVisibility(2);
	}

	/**
	 * Class to do antenna checking in a new thread.
	 */
	private static class ParameterVisibility extends Job
	{
		private int how;
	
		protected ParameterVisibility(int how)
		{
			super("Change Parameter Visibility", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.how = how;
			startJob();
		}
	
		public void doIt()
		{
			// change visibility of parameters on the current node(s)
			int changeCount = 0;
			List list = Highlight.getHighlighted(true, false);
			for(Iterator it = list.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (!(ni.getProto() instanceof Cell)) continue;
				boolean changed = false;
				for(Iterator vIt = ni.getVariables(); vIt.hasNext(); )
				{
					Variable var = (Variable)vIt.next();
					Variable nVar = findParameterSource(var, ni);
					if (nVar == null) continue;
					switch (how)
					{
						case 0:			// make all parameters visible
							if (var.isDisplay()) continue;
							var.setDisplay();
							changed = true;
							break;
						case 1:			// make all parameters invisible
							if (!var.isDisplay()) continue;
							var.clearDisplay();
							changed = true;
							break;
						case 2:			// make all parameters have default visiblity
							if (nVar.getTextDescriptor().isInterior())
							{
								// prototype wants parameter to be invisible
								if (!var.isDisplay()) continue;
								var.clearDisplay();
								changed = true;
							} else
							{
								// prototype wants parameter to be visible
								if (var.isDisplay()) continue;
								var.setDisplay();
								changed = true;
							}
							break;
					}
				}
				if (changed)
				{
					Undo.redrawObject(ni);
					changeCount++;
				}
			}
			if (changeCount == 0) System.out.println("No Parameter visibility changed"); else
				System.out.println("Changed visibility on " + changeCount + " nodes");
		}
	}

	/**
	 * Method to find the formal parameter that corresponds to the actual parameter
	 * "var" on node "ni".  Returns null if not a parameter or cannot be found.
	 */
	private static Variable findParameterSource(Variable var, NodeInst ni)
	{
		// find this parameter in the cell
		Cell np = (Cell)ni.getProto();
		Cell cnp = np.contentsView();
		if (cnp != null) np = cnp;
		for(Iterator it = np.getVariables(); it.hasNext(); )
		{
			Variable nVar = (Variable)it.next();
			if (var.getKey() == nVar.getKey()) return nVar;
		}
		return null;
	}

	/**
	 * Method to handle the "List Layer Coverage" command.
	 */
	public static void layerCoverageCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
        Job job = new LayerCoverage(curCell);
	}

	private static class LayerCoverage extends Job
	{
		private Cell curCell;

		protected LayerCoverage(Cell cell)
		{
			super("Layer Coverage", User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.curCell = cell;
			setReportExecutionFlag(true);
			startJob();
		}

		public void doIt()
		{
			PolyQTree tree = new PolyQTree();

			// Traversing nodes
			for (Iterator it = curCell.getNodes(); it.hasNext(); )
			{
				NodeInst node = (NodeInst)it .next();

				// Coverage implants are pure primitive nodes
				// and they are ignored.
				if (node.getFunction() == NodeProto.Function.NODE) continue;

				NodeProto protoType = node.getProto();
				if (protoType instanceof Cell)
				{
					System.out.println("recursive");
				}
                else
				{
					Technology tech = protoType.getTechnology();
					Poly[] polyList = tech.getShapeOfNode(node);
					AffineTransform transform = node.rotateOut();

					for (int i = 0; i < polyList.length; i++)
					{
						Poly poly = polyList[i];
						Layer layer = poly.getLayer();
						Layer.Function func = layer.getFunction();

						// Only checking poly or metal
						if (!func.isPoly() && !func.isMetal()) continue;

						poly.transform(transform);
						tree.insert((Object)layer, curCell.getBounds(), new PolyQTree.PolyNode(poly.getBounds2D()));
					}
				}
			}

			double lambdaSqr = 1;
			// @todo GVG Calculates lambda!
			Rectangle2D bbox = curCell.getBounds();
			double totalArea =  (bbox.getHeight()*bbox.getWidth())/lambdaSqr;

			// Traversing tree with merged geometry
			for (Iterator it = tree.getKeyIterator(); it.hasNext(); )
			{
				Layer layer = (Layer)it.next();
				Set set = tree.getObjects(layer, false);
				double layerArea = 0;

				// Get all objects and sum the area
				for (Iterator i = set.iterator(); i.hasNext(); )
				{
					PolyQTree.PolyNode area = (PolyQTree.PolyNode)i.next();
					layerArea += area.getArea();
				}
				System.out.println("Layer " + layer.getName() + " covers " + TextUtils.formatDouble(layerArea) + " square lambda (" + TextUtils.formatDouble((layerArea/totalArea)*100, 0) + "%)");
			}

			System.out.println("Cell is " + TextUtils.formatDouble(totalArea) + " square lambda");
		}
//		// initialize for analysis
//		us_coveragetech = cell->tech;
//
//		// determine which layers are being collected
//		us_coveragelayercount = 0;
//		for(i=0; i<us_coveragetech->layercount; i++)
//		{
//			fun = layerfunction(us_coveragetech, i);
//			if ((fun&LFPSEUDO) != 0) continue;
//			if (!layerismetal(fun) && !layerispoly(fun)) continue;
//			us_coveragelayercount++;
//		}
//		if (us_coveragelayercount == 0)
//		{
//			ttyputerr(_("No metal or polysilicon layers in this technology"));
//			return;
//		}
//		us_coveragelayers = (INTBIG *)emalloc(us_coveragelayercount * SIZEOFINTBIG, us_tool->cluster);
//		if (us_coveragelayers == 0) return;
//		us_coveragelayercount = 0;
//		for(i=0; i<us_coveragetech->layercount; i++)
//		{
//			fun = layerfunction(us_coveragetech, i);
//			if ((fun&LFPSEUDO) != 0) continue;
//			if (!layerismetal(fun) && !layerispoly(fun)) continue;
//			us_coveragelayers[us_coveragelayercount++] = i;
//		}
//
//		// show the progress dialog
//		us_coveragedialog = DiaInitProgress(_("Merging geometry..."), 0);
//		if (us_coveragedialog == 0)
//		{
//			termerrorlogging(TRUE);
//			return;
//		}
//		DiaSetProgress(us_coveragedialog, 0, 1);
//
//		// reset merging information
//		for(lib = el_curlib; lib != NOLIBRARY; lib = lib->nextlibrary)
//		{
//			for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//			{
//				np->temp1 = 0;
//
//				// if this cell has parameters, force its polygons to be examined for every instance
//				for(i=0; i<np->numvar; i++)
//				{
//					var = &np->firstvar[i];
//					if (TDGETISPARAM(var->textdescript) != 0) break;
//				}
//				if (i < np->numvar) np->temp1 = -1;
//			}
//		}
//
//		// run through the work and count the number of polygons
//		us_coveragepolyscrunched = 0;
//		us_gathercoveragegeometry(cell, el_matid, 0);
//		us_coveragejobsize = us_coveragepolyscrunched;
//
//		// now gather all of the geometry into the polygon merging system
//		polymerge = (void **)emalloc(us_coveragelayercount * (sizeof (void *)), us_tool->cluster);
//		if (polymerge == 0) return;
//		for(i=0; i<us_coveragelayercount; i++)
//			polymerge[i] = mergenew(us_tool->cluster);
//		us_coveragepolyscrunched = 0;
//		us_gathercoveragegeometry(cell, el_matid, polymerge);
//
//		// extract the information
//		us_coveragearea = (float *)emalloc(us_coveragelayercount * (sizeof (float)), us_tool->cluster);
//		if (us_coveragearea == 0) return;
//		for(i=0; i<us_coveragelayercount; i++)
//		{
//			us_coveragearea[i] = 0.0;
//			mergeextract(polymerge[i], us_getcoveragegeometry);
//		}
//
//		// show the results
//		totalarea = (float)(cell->highx - cell->lowx);
//		totalarea *= (float)(cell->highy - cell->lowy);
//		ttyputmsg(x_("Cell is %g square lambda"), totalarea/(float)lambda/(float)lambda);
//		for(i=0; i<us_coveragelayercount; i++)
//		{
//			if (us_coveragearea[i] == 0.0) continue;
//			if (totalarea == 0.0) coverageratio = 0.0; else
//				coverageratio = us_coveragearea[i] / totalarea;
//			percentcoverage = (INTBIG)(coverageratio * 100.0 + 0.5);
//
//			ttyputmsg(x_("Layer %s covers %g square lambda (%ld%%)"),
//				layername(us_coveragetech, us_coveragelayers[i]),
//				us_coveragearea[i]/(float)lambda/(float)lambda, percentcoverage);
//		}
//
//		// delete merge information
//		for(lib = el_curlib; lib != NOLIBRARY; lib = lib->nextlibrary)
//		{
//			for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//			{
//				if (np->temp1 == 0 || np->temp1 == -1) continue;
//				submerge = (void **)np->temp1;
//				for(i=0; i<us_coveragelayercount; i++)
//					mergedelete(submerge[i]);
//				efree((CHAR *)submerge);
//			}
//		}
//		for(i=0; i<us_coveragelayercount; i++)
//			mergedelete(polymerge[i]);
//		efree((CHAR *)polymerge);
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
		Cell curCell = WindowFrame.needCurCell();
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
		Cell curCell = WindowFrame.needCurCell();
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
	 * This method implements the command to deselect all selected arcs.
	 */
	public static void deselectAllArcsCommand()
	{
		List newHighList = new ArrayList();
		for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			if (h.getType() == Highlight.Type.EOBJ || h.getType() == Highlight.Type.TEXT)
			{
				if (h.getElectricObject() instanceof ArcInst) continue;
			}
			newHighList.add(h);
		}
		Highlight.clear();
		Highlight.setHighlightList(newHighList);
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
	 * This method implements the command to replace the rectangular highlight
	 * with the selection of objects in that rectangle.
	 */
	public static void selectEnclosedObjectsCommand()
	{
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
		Rectangle2D selection = Highlight.getHighlightedArea(wnd);
		Highlight.clear();
		Highlight.selectArea(wnd, selection.getMinX(), selection.getMaxX(), selection.getMinY(), selection.getMaxY(), false,
			ToolBar.getSelectSpecial());
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
		Cell curCell = WindowFrame.needCurCell();
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
		EditWindow curEdit = EditWindow.needCurrent();
		curEdit.downHierarchy();
	}

	/**
	 * This command goes up the hierarchy
	 */
	public static void upHierCommand() {
		EditWindow curEdit = EditWindow.needCurrent();
		curEdit.upHierarchy();
	}

	/**
	 * This method implements the command to make a new version of the current Cell.
	 */
	public static void newCellVersionCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
		CircuitChanges.newVersionOfCell(curCell);
	}

	/**
	 * This method implements the command to make a copy of the current Cell.
	 */
	public static void duplicateCellCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
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

	public static void changeViewCommand()
	{
		Cell cell = WindowFrame.getCurrentCell();
		if (cell == null) return;

		List views = View.getOrderedViews();
		String [] viewNames = new String[views.size()];
		for(int i=0; i<views.size(); i++)
			viewNames[i] = ((View)views.get(i)).getFullName();
		Object newName = JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "New view for this cell",
			"Choose alternate view", JOptionPane.QUESTION_MESSAGE, null, viewNames, cell.getView().getFullName());
		if (newName == null) return;
		String newViewName = (String)newName;
		View newView = View.findView(newViewName);
		if (newView != null && newView != cell.getView())
		{
			CircuitChanges.changeCellView(cell, newView);
		}
	}

	public static void editLayoutViewCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
		Cell layoutView = curCell.otherView(View.LAYOUT);
		if (layoutView != null)
			WindowFrame.createEditWindow(layoutView);
	}

	public static void editSchematicViewCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
		Cell schematicView = curCell.otherView(View.SCHEMATIC);
		if (schematicView != null)
			WindowFrame.createEditWindow(schematicView);
	}

	public static void editIconViewCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
		Cell iconView = curCell.otherView(View.ICON);
		if (iconView != null)
			WindowFrame.createEditWindow(iconView);
	}

	public static void editVHDLViewCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
		Cell vhdlView = curCell.otherView(View.VHDL);
		if (vhdlView != null)
			WindowFrame.createEditWindow(vhdlView);
	}

	public static void editDocViewCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
		Cell docView = curCell.otherView(View.DOC);
		if (docView != null)
			WindowFrame.createEditWindow(docView);
	}

	public static void editSkeletonViewCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
		Cell skelView = curCell.otherView(View.LAYOUTSKEL);
		if (skelView != null)
			WindowFrame.createEditWindow(skelView);
	}

	public static void editOtherViewCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
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

	public static void fullDisplay()
	{
		// get the current frame
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;

		// make the circuit fill the window
		wf.getContent().fillScreen();
	}

	public static void zoomOutDisplay()
	{
		// get the current frame
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;

		// zoom out
		wf.getContent().zoomOutContents();
	}

	public static void zoomInDisplay()
	{
		// get the current frame
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;

		// zoom in
		wf.getContent().zoomInContents();
	}

    public static void zoomBoxCommand()
    {
        // only works with click zoom wire listener
        EventListener oldListener = WindowFrame.getListener();
        WindowFrame.setListener(ClickZoomWireListener.theOne);
        ClickZoomWireListener.theOne.zoomBoxSingleShot(oldListener);
    }

	public static void focusOnHighlighted()
	{
		// get the current frame
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;

		// focus on highlighted
		wf.getContent().focusOnHighlighted();
	}

	/**
	 * This method implements the command to toggle the display of the grid.
	 */
	public static void toggleGridCommand()
	{
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
		wnd.setGrid(!wnd.isGrid());
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
        WindowFrame curWF = WindowFrame.getCurrentWindowFrame();
		WindowContent content = curWF.getContent();
        GraphicsConfiguration curConfig = content.getPanel().getGraphicsConfiguration();
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
	public static void optimizeEqualGateDelaysCommand()
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
		letool.optimizeEqualGateDelays(curEdit.getCell(), curEdit.getVarContext(), curEdit);
	}

    /** Print Logical Effort info for highlighted nodes */
    public static void printLEInfoCommand() {
        if (Highlight.getNumHighlights() == 0) {
            System.out.println("Nothing highlighted");
            return;
        }
        for (Iterator it = Highlight.getHighlights(); it.hasNext();) {
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
                LETool.printResults(ni);
            }
        }

    }

	/**
	 * Method to handle the "Show Network" command.
	 */
	public static void showNetworkCommand()
	{
		Set nets = Highlight.getHighlightedNetworks();
		Highlight.clear();
		for(Iterator it = nets.iterator(); it.hasNext(); )
		{
			JNetwork net = (JNetwork)it.next();
			Cell cell = net.getParent();
			Highlight.addNetwork(net, cell);
		}
		Highlight.finished();
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
			JNetwork net = (JNetwork)it.next();
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
				infstr.append(" " + pp.getProtoName());
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
		Set nets = Highlight.getHighlightedNetworks();
		Netlist netlist = cell.getUserNetlist();
		for(Iterator it = nets.iterator(); it.hasNext(); )
		{
			JNetwork net = (JNetwork)it.next();
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
					if (pp.isIsolated())
					{
						NodeInst ni = (NodeInst)no;
						for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
						{
							Connection con = (Connection)cIt.next();
							ArcInst ai = con.getArc();
							JNetwork oNet = netlist.getNetwork(ai, 0);
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
							JNetwork oNet = netlist.getNetwork(no, pp, i);
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
				System.out.println("    Node " + name + ", port " + pp.getProtoName());
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
		Set nets = Highlight.getHighlightedNetworks();
		Netlist netlist = cell.getUserNetlist();
		for(Iterator it = nets.iterator(); it.hasNext(); )
		{
			JNetwork net = (JNetwork)it.next();
			System.out.println("Network '" + net.describe() + "':");

			// find all exports on network "net"
			FlagSet fs = Geometric.getFlagSet(1);
			for(Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
			{
				Library lib = (Library)lIt.next();
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell oCell = (Cell)cIt.next();
					for(Iterator pIt = oCell.getPorts(); pIt.hasNext(); )
					{
						Export pp = (Export)pIt.next();
						pp.clearBit(fs);
					}
				}
			}
			System.out.println("  Going up the hierarchy from cell " + cell.describe() + ":");
			findPortsUp(netlist, net, cell, fs);
			System.out.println("  Going down the hierarchy from cell " + cell.describe() + ":");
			findPortsDown(netlist, net, cell, fs);
			fs.freeFlagSet();
		}
	}

	/**
	 * Method to handle the "List Exports Below Network" command.
	 */
	public static void listExportsBelowNetworkCommand()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		Set nets = Highlight.getHighlightedNetworks();
		Netlist netlist = cell.getUserNetlist();
		for(Iterator it = nets.iterator(); it.hasNext(); )
		{
			JNetwork net = (JNetwork)it.next();
			System.out.println("Network '" + net.describe() + "':");

			// find all exports on network "net"
			FlagSet fs = Geometric.getFlagSet(1);
			for(Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
			{
				Library lib = (Library)lIt.next();
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell oCell = (Cell)cIt.next();
					for(Iterator pIt = oCell.getPorts(); pIt.hasNext(); )
					{
						Export pp = (Export)pIt.next();
						pp.clearBit(fs);
					}
				}
			}
			findPortsDown(netlist, net, cell, fs);
			fs.freeFlagSet();
		}
	}

	/**
	 * helper method for "telltool network list-hierarchical-ports" to print all
	 * ports connected to net "net" in cell "cell", and recurse up the hierarchy
	 */
	private static void findPortsUp(Netlist netlist, JNetwork net, Cell cell, FlagSet fs)
	{
		// look at every node in the cell
		for(Iterator it = cell.getPorts(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			int width = netlist.getBusWidth(pp);
			for(int i=0; i<width; i++)
			{
				JNetwork ppNet = netlist.getNetwork(pp, i);
				if (ppNet != net) continue;
				if (pp.isBit(fs)) continue;
				pp.setBit(fs);
				System.out.println("    Export " + pp.getProtoName() + " in cell " + cell.describe());

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
						JNetwork superNet = superNetlist.getNetwork(no, pp, i);
						findPortsUp(superNetlist, superNet, superCell, fs);
					}
				}
			}
		}
	}

	/**
	 * helper method for "telltool network list-hierarchical-ports" to print all
	 * ports connected to net "net" in cell "cell", and recurse down the hierarchy
	 */
	private static void findPortsDown(Netlist netlist, JNetwork net, Cell cell, FlagSet fs)
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
					JNetwork oNet = netlist.getNetwork(no, pp, i);
					if (oNet != net) continue;

					// found the net here: report it
					if (pp.isBit(fs)) continue;
					pp.setBit(fs);
					System.out.println("    Export " + pp.getProtoName() + " in cell " + subCell.describe());
					Netlist subNetlist = subCell.getUserNetlist();
					JNetwork subNet = subNetlist.getNetwork(pp, i);
					findPortsDown(subNetlist, subNet, subCell, fs);
				}
			}
		}
	}

	/**
	 * Method to handle the "List Geometry On Network" command.
	 */
	public static void listGeometryOnNetworkCommand()
	{
		System.out.println("Can't Yet");
//		/* gather geometry on this network */
//		np = net->parent;
//		firstarpe = net_gathergeometry(net, &p_gate, &n_gate, &p_active, &n_active, TRUE);
//
//		/* copy the linked list to an array for sorting */
//		total = 0;
//		for(arpe = firstarpe; arpe != NOAREAPERIM; arpe = arpe->nextareaperim)
//			if (arpe->layer >= 0) total++;
//		if (total == 0)
//		{
//			ttyputmsg(_("No geometry on network '%s' in cell %s"), describenetwork(net),
//				describenodeproto(np));
//			return;
//		}
//		arpelist = (AREAPERIM **)emalloc(total * (sizeof (AREAPERIM *)), net_tool->cluster);
//		if (arpelist == 0) return;
//		i = 0;
//		for(arpe = firstarpe; arpe != NOAREAPERIM; arpe = arpe->nextareaperim)
//			if (arpe->layer >= 0) arpelist[i++] = arpe;
//
//		/* sort the layers */
//		esort(arpelist, total, sizeof (AREAPERIM *), net_areaperimdepthascending);
//
//		ttyputmsg(_("For network '%s' in cell %s:"), describenetwork(net),
//			describenodeproto(np));
//		lambda = lambdaofcell(np);
//		widest = 0;
//		for(i=0; i<total; i++)
//		{
//			arpe = arpelist[i];
//			lname = layername(arpe->tech, arpe->layer);
//			len = estrlen(lname);
//			if (len > widest) widest = len;
//		}
//		totalWire = 0;
//		for(i=0; i<total; i++)
//		{
//			arpe = arpelist[i];
//			lname = layername(arpe->tech, arpe->layer);
//			infstr = initinfstr();
//			for(j=estrlen(lname); j<widest; j++) addtoinfstr(infstr, ' ');
//			pad = returninfstr(infstr);
//			if (arpe->perimeter == 0)
//			{
//				ttyputmsg(_("Layer %s:%s area=%7g  half-perimeter=%s"), lname, pad,
//					arpe->area/(float)lambda/(float)lambda, latoa(arpe->perimeter/2, 0));
//			} else
//			{
//				ratio = (arpe->area / (float)lambda) / (float)(arpe->perimeter/2);
//				ttyputmsg(_("Layer %s:%s area=%7g  half-perimeter=%s ratio=%g"), lname,
//					pad, arpe->area/(float)lambda/(float)lambda,
//						latoa(arpe->perimeter/2, lambda), ratio);
//
//				/* accumulate total wire length on all metal/poly layers */
//				fun = layerfunction(arpe->tech, arpe->layer);
//				if ((layerispoly(fun) && !layerisgatepoly(fun)) || layerismetal(fun))
//					totalWire += arpe->perimeter / 2;
//			}
//			efree((CHAR *)arpelist[i]);
//		}
//		if (totalWire > 0.0) ttyputmsg(_("Total wire length = %s"), latoa(totalWire,lambda));
	}

	public static void showPowerAndGround()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
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
					JNetwork net = netlist.getNetwork(pp, i);
					pAndG.add(net);
				}
			}
		}
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto.Function fun = ni.getFunction();
			if (fun != NodeProto.Function.CONPOWER && fun != NodeProto.Function.CONGROUND)
				continue;
			for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
			{
				Connection con = (Connection)cIt.next();
				ArcInst ai = con.getArc();
				int width = netlist.getBusWidth(ai);
				for(int i=0; i<width; i++)
				{
					JNetwork net = netlist.getNetwork(ai, i);
					pAndG.add(net);
				}
			}
		}

		Highlight.clear();
		for(Iterator it = pAndG.iterator(); it.hasNext(); )
		{
			JNetwork net = (JNetwork)it.next();
			Highlight.addNetwork(net, cell);
		}
		Highlight.finished();
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
					if (pp.isNamedGround() && pp.getCharacteristic() != PortProto.Characteristic.GND)
					{
						System.out.println("Cell " + cell.describe() + ", export " + pp.getProtoName() +
							": does not have 'GROUND' characteristic");
						total++;
					}
					if (pp.isNamedPower() && pp.getCharacteristic() != PortProto.Characteristic.PWR)
					{
						System.out.println("Cell " + cell.describe() + ", export " + pp.getProtoName() +
							": does not have 'POWER' characteristic");
						total++;
					}
				}
			}
		}
		if (total == 0) System.out.println("No problems found"); else
			System.out.println("Found " + total + " export problems");
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
		EditWindow curEdit = EditWindow.needCurrent();
		if (curEdit == null) return;
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

	private static class MakeTemplate extends Job
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
			Cell cell = WindowFrame.needCurCell();
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

	// ---------------------- THE HELP MENU -----------------

	public static void aboutCommand()
	{
		About dialog = new About(TopLevel.getCurrentJFrame(), true);
		dialog.show();
	}

    public static void toolTipsCommand()
    {
        HelpViewer dialog = new HelpViewer(TopLevel.getCurrentJFrame(), false, null);
        dialog.show();
    }

	public static void describeTechnologyCommand()
	{
		Technology tech = Technology.getCurrent();
		System.out.println("Technology " + tech.getTechName());
		System.out.println("    Full name: " + tech.getTechDesc());
		if (tech.isScaleRelevant())
		{
			System.out.println("    Scale: 1 grid unit is " + tech.getScale() + " nanometers (" +
				(tech.getScale()/1000) + " microns)");
		}
		int arcCount = 0;
		for(Iterator it = tech.getArcs(); it.hasNext(); )
		{
			PrimitiveArc ap = (PrimitiveArc)it.next();
			if (!ap.isNotUsed()) arcCount++;
		}
		StringBuffer sb = new StringBuffer();
		sb.append("    Has " + arcCount + " arcs (wires):");
		for(Iterator it = tech.getArcs(); it.hasNext(); )
		{
			PrimitiveArc ap = (PrimitiveArc)it.next();
			if (ap.isNotUsed()) continue;
			sb.append(" " + ap.getProtoName());
		}
		System.out.println(sb.toString());

		int pinCount = 0, totalCount = 0, pureCount = 0, contactCount = 0;
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			if (np.isNotUsed()) continue;
			NodeProto.Function fun = np.getFunction();
			totalCount++;
			if (fun == NodeProto.Function.PIN) pinCount++; else
			if (fun == NodeProto.Function.CONTACT || fun == NodeProto.Function.CONNECT) contactCount++; else
			if (fun == NodeProto.Function.NODE) pureCount++;
		}
		if (pinCount > 0)
		{
			sb = new StringBuffer();
			sb.append("    Has " + pinCount + " pin nodes for making bends in arcs:");
			for(Iterator it = tech.getNodes(); it.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)it.next();
				if (np.isNotUsed()) continue;
				NodeProto.Function fun = np.getFunction();
				if (fun == NodeProto.Function.PIN) sb.append(" " + np.getProtoName());
			}
			System.out.println(sb.toString());
		}
		if (contactCount > 0)
		{
			sb = new StringBuffer();
			sb.append("    Has " + contactCount + " contact nodes for joining different arcs:");
			for(Iterator it = tech.getNodes(); it.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)it.next();
				if (np.isNotUsed()) continue;
				NodeProto.Function fun = np.getFunction();
				if (fun == NodeProto.Function.CONTACT || fun == NodeProto.Function.CONNECT)
					sb.append(" " + np.getProtoName());
			}
			System.out.println(sb.toString());
		}
		if (pinCount+contactCount+pureCount < totalCount)
		{
			sb = new StringBuffer();
			sb.append("    Has " + (totalCount-pinCount-contactCount-pureCount) + " regular nodes:");
			for(Iterator it = tech.getNodes(); it.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)it.next();
				if (np.isNotUsed()) continue;
				NodeProto.Function fun = np.getFunction();
				if (fun != NodeProto.Function.PIN && fun != NodeProto.Function.CONTACT &&
					fun != NodeProto.Function.CONNECT && fun != NodeProto.Function.NODE)
						sb.append(" " + np.getProtoName());
			}
			System.out.println(sb.toString());
		}
		if (pureCount > 0)
		{
			sb = new StringBuffer();
			sb.append("    Has " + pureCount + " pure-layer nodes for creating custom geometry:");
			for(Iterator it = tech.getNodes(); it.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)it.next();
				if (np.isNotUsed()) continue;
				NodeProto.Function fun = np.getFunction();
				if (fun == NodeProto.Function.NODE) sb.append(" " + np.getProtoName());
			}
			System.out.println(sb.toString());
		}
	}

	/**
	 * This method implements the command to insert a jog in an arc
	 */
	public static void insertJogInArcCommand()
	{
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
		ArcInst ai = (ArcInst)Highlight.getOneElectricObject(ArcInst.class);
		if (ai == null) return;
		if (CircuitChanges.cantEdit(ai.getParent(), null, true)) return;

		System.out.println("Select the position in the arc to place the jog");
		EventListener currentListener = WindowFrame.getListener();
		WindowFrame.setListener(new InsertJogInArcListener(wnd, ai, currentListener));
	}

	/**
	 * Class to handle the interactive selection of a jog point in an arc.
	 */
	private static class InsertJogInArcListener
		implements MouseMotionListener, MouseListener
	{
		private EditWindow wnd;
		private ArcInst ai;
		private EventListener currentListener;

		/**
		 * Create a new insert-jog-point listener
		 * @param wnd Controlling window
		 * @param ai the arc that is having a jog inserted.
		 * @param currentListener listener to restore when done
		 */
		public InsertJogInArcListener(EditWindow wnd, ArcInst ai, EventListener currentListener)
		{
			this.wnd = wnd;
			this.ai = ai;
			this.currentListener = currentListener;
		}

		public void mousePressed(MouseEvent evt) {}
		public void mouseClicked(MouseEvent evt) {}
		public void mouseEntered(MouseEvent evt) {}
		public void mouseExited(MouseEvent evt) {}

		public void mouseDragged(MouseEvent evt)
		{
			mouseMoved(evt);
		}

		public void mouseReleased(MouseEvent evt)
		{
			Point2D insert = getInsertPoint(evt);
			InsertJogPoint job = new InsertJogPoint(ai, insert);
			WindowFrame.setListener(currentListener);
		}

		public void mouseMoved(MouseEvent evt)
		{
			Point2D insert = getInsertPoint(evt);
			double x = insert.getX();
			double y = insert.getY();

			double width = (ai.getWidth() - ai.getProto().getWidthOffset()) / 2;
			Highlight.clear();
			Highlight.addLine(new Point2D.Double(x-width, y-width), new Point2D.Double(x-width, y+width), ai.getParent());
			Highlight.addLine(new Point2D.Double(x-width, y+width), new Point2D.Double(x+width, y+width), ai.getParent());
			Highlight.addLine(new Point2D.Double(x+width, y+width), new Point2D.Double(x+width, y-width), ai.getParent());
			Highlight.addLine(new Point2D.Double(x+width, y-width), new Point2D.Double(x-width, y-width), ai.getParent());
			Highlight.finished();
			wnd.repaint();
		}

		private Point2D getInsertPoint(MouseEvent evt)
		{
			Point2D mouseDB = wnd.screenToDatabase((int)evt.getX(), (int)evt.getY());
			Point2D insert = EMath.closestPointToSegment(ai.getHead().getLocation(), ai.getTail().getLocation(), mouseDB);
			EditWindow.gridAlign(insert);
			return insert;
		}

		private static class InsertJogPoint extends Job
		{
			ArcInst ai;
			Point2D insert;

			protected InsertJogPoint(ArcInst ai, Point2D insert)
			{
				super("Insert Jog in Arc", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
				this.ai = ai;
				this.insert = insert;
				startJob();
			}

			public void doIt()
			{
				// create the break pins
				ArcProto ap = ai.getProto();
				NodeProto np = ((PrimitiveArc)ap).findPinProto();
				if (np == null) return;
				NodeInst ni = NodeInst.makeInstance(np, insert, np.getDefWidth(), np.getDefHeight(),
					0, ai.getParent(), null);
				if (ni == null)
				{
					System.out.println("Cannot create pin " + np.describe());
					return;
				}
				NodeInst ni2 = NodeInst.makeInstance(np, insert, np.getDefWidth(), np.getDefHeight(),
					0, ai.getParent(), null);
				if (ni2 == null)
				{
					System.out.println("Cannot create pin " + np.describe());
					return;
				}

				// get location of connection to these pins
				PortInst pi = ni.getOnlyPortInst();
				PortInst pi2 = ni2.getOnlyPortInst();

//				// see if edge alignment is appropriate
//				if (us_edgealignment_ratio != 0 && (ai->end[0].xpos == ai->end[1].xpos ||
//					ai->end[0].ypos == ai->end[1].ypos))
//				{
//					edgealignment = muldiv(us_edgealignment_ratio, WHOLE, el_curlib->lambda[el_curtech->techindex]);
//					px = us_alignvalue(x, edgealignment, &otheralign);
//					py = us_alignvalue(y, edgealignment, &otheralign);
//					if (px != x || py != y)
//					{
//						// shift the nodes and make sure the ports are still valid
//						startobjectchange((INTBIG)ni, VNODEINST);
//						modifynodeinst(ni, px-x, py-y, px-x, py-y, 0, 0);
//						endobjectchange((INTBIG)ni, VNODEINST);
//						startobjectchange((INTBIG)ni2, VNODEINST);
//						modifynodeinst(ni2, px-x, py-y, px-x, py-y, 0, 0);
//						endobjectchange((INTBIG)ni2, VNODEINST);
//						(void)shapeportpoly(ni, ppt, poly, FALSE);
//						if (!isinside(nx, ny, poly)) getcenter(poly, &nx, &ny);
//					}
//				}

				// now save the arc information and delete it
				PortInst headPort = ai.getHead().getPortInst();
				PortInst tailPort = ai.getTail().getPortInst();
				Point2D headPt = ai.getHead().getLocation();
				Point2D tailPt = ai.getTail().getLocation();
				double width = ai.getWidth();
				boolean headNegated = ai.getHead().isNegated();
				boolean tailNegated = ai.getTail().isNegated();
				if (ai.isReverseEnds())
				{
					boolean swap = headNegated;   headNegated = tailNegated;   tailNegated = swap;
				}
				String arcName = ai.getName();
				int angle = (ai.getAngle() + 900) % 3600;
				ai.kill();

				// create the new arcs
				ArcInst newAi1 = ArcInst.makeInstance(ap, width, headPort, headPt, pi, insert, null);
				if (headNegated) newAi1.getHead().setNegated(true);
				ArcInst newAi2 = ArcInst.makeInstance(ap, width, pi, insert, pi2, insert, null);
				ArcInst newAi3 = ArcInst.makeInstance(ap, width, pi2, insert, tailPort, tailPt, null);
				if (tailNegated) newAi3.getTail().setNegated(true);
				if (arcName != null)
				{
					if (headPt.distance(insert) > tailPt.distance(insert))
					{
						newAi1.setName(arcName);
					} else
					{
						newAi3.setName(arcName);
					}
				}
				newAi2.setAngle(angle);

				// highlight one of the jog nodes
				Highlight.clear();
				Highlight.addElectricObject(ni, ai.getParent());
				Highlight.finished();
			}
		}
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
	private static class MakeFakeCircuitry extends Job
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

    // ---------------------- Gilda's Stuff MENU -----------------
	public static void implantGeneratorCommand(boolean newIdea, boolean test) {
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
        Job job = null;

	    if (newIdea)
	    {
			job = new CoverImplant(curCell, test);
	    }
	    else
	    {
		    job = new CoverImplantOld(curCell);
	    }
	}

	private static class CoverImplant extends Job
	{
		private Cell curCell;
        private boolean testMerge = false;

		protected CoverImplant(Cell cell, boolean test)
		{
			super("Coverage Implant", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.curCell = cell;
            this.testMerge = test;
			setReportExecutionFlag(true);
			startJob();
		}

		public void doIt()
		{
			List deleteList = new ArrayList(); // New coverage implants are pure primitive nodes
            PolyQTree tree = new PolyQTree();

			// Traversing arcs
			for (Iterator it = curCell.getArcs(); it.hasNext(); )
			{
				ArcInst arc = (ArcInst)it.next();
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

					if (Main.getDebug() || func.isSubstrate())
					{
						//Area bounds = new PolyQTree.PolyNode(poly.getBounds2D());
						tree.insert((Object)layer, curCell.getBounds(), new PolyQTree.PolyNode(poly.getBounds2D()));
					}
				}
			}
			// Traversing nodes
			for (Iterator it = curCell.getNodes(); it.hasNext(); )
			{
				NodeInst node = (NodeInst)it .next();

				// New coverage implants are pure primitive nodes
				// and previous get deleted and ignored.
				if (!Main.getDebug() && node.getFunction() == NodeProto.Function.NODE)
				{
					deleteList.add(node);
					continue;
				}

				NodeProto protoType = node.getProto();
				if (protoType instanceof Cell) continue;

				Technology tech = protoType.getTechnology();
				Poly[] polyList = tech.getShapeOfNode(node);
				AffineTransform transform = node.rotateOut();

				for (int i = 0; i < polyList.length; i++)
				{
					Poly poly = polyList[i];
					Layer layer = poly.getLayer();
					Layer.Function func = layer.getFunction();

                    // Only substrate layers, skipping center information
					if (Main.getDebug() || func.isSubstrate())
					{
						poly.transform(transform);
						//Area bounds = new PolyQTree.PolyNode(poly.getBounds2D());
						tree.insert((Object)layer, curCell.getBounds(), new PolyQTree.PolyNode(poly.getBounds2D()));
					}
				}
			}

			// tree.print();

			// With polygons collected, new geometries are calculated
			Highlight.clear();
			List nodesList = new ArrayList();

			// Need to detect if geometry was really modified
			for(Iterator it = tree.getKeyIterator(); it.hasNext(); )
			{
				Layer layer = (Layer)it.next();
				Set set = tree.getObjects(layer, true);

				// Ready to create new implants.
				for (Iterator i = set.iterator(); i.hasNext(); )
				{
					Rectangle2D rect = ((PolyQTree.PolyNode)i.next()).getBounds2D();
					Point2D center = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
					PrimitiveNode priNode = layer.getPureLayerNode();
					// Adding the new implant. New implant not assigned to any local variable                                .
					NodeInst node = NodeInst.makeInstance(priNode, center, rect.getWidth(), rect.getHeight(), 0, curCell, null);
					Highlight.addElectricObject(node, curCell);
					// New implant can't be selected again
					node.setHardSelect();
					nodesList.add(node);
				}
			}
			Highlight.finished();
			for (Iterator it = deleteList.iterator(); it.hasNext(); )
			{
				NodeInst node = (NodeInst)it .next();
				node.kill();
			}
			if ( nodesList.isEmpty() )
				System.out.println("No implant areas added");
		}
	}

	private static class CoverImplantOld extends Job
	{
		private Cell curCell;

		protected CoverImplantOld(Cell cell)
		{
			super("Coverage Implant Old", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.curCell = cell;
			setReportExecutionFlag(true);
			startJob();
		}

		public void doIt()
		{
			PolyMerge merge = new PolyMerge();
			List deleteList = new ArrayList(); // New coverage implants are pure primitive nodes
			HashMap allLayers = new HashMap();

			// Traversing arcs
			for(Iterator it = curCell.getArcs(); it.hasNext(); )
			{
				ArcInst arc = (ArcInst)it.next();
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

					if ( func.isSubstrate() )
					{
						merge.addPolygon(layer, poly);
						List rectList = (List)allLayers.get(layer);

						if ( rectList == null )
						{
							rectList = new ArrayList();
							allLayers.put(layer, rectList);
						}
						rectList.add(poly);
					}
				}
			}
			// Traversing nodes
			for(Iterator it = curCell.getNodes(); it.hasNext(); )
			{
				NodeInst node = (NodeInst)it .next();

				// New coverage implants are pure primitive nodes
				// and previous get deleted and ignored.
				if ( node.getFunction() == NodeProto.Function.NODE )
				{
					deleteList.add(node);
					continue;
				}

				NodeProto protoType = node.getProto();
				if (protoType instanceof Cell) continue;

				Technology tech = protoType.getTechnology();
				Poly[] polyList = tech.getShapeOfNode(node);
				AffineTransform transform = node.rotateOut();

				for (int i = 0; i < polyList.length; i++)
				{
					Poly poly = polyList[i];
					Layer layer = poly.getLayer();
					Layer.Function func = layer.getFunction();

                    // Only substrate layers, skipping center information
					if ( func.isSubstrate() )
					{
						poly.transform(transform);
						merge.addPolygon(layer, poly);
						List rectList = (List)allLayers.get(layer);

						if ( rectList == null )
						{
							rectList = new ArrayList();
							allLayers.put(layer, rectList);
						}
						rectList.add(poly);
					}
				}
			}

			// With polygons collected, new geometries are calculated
			Highlight.clear();
			List nodesList = new ArrayList();

			// Need to detect if geometry was really modified
			for(Iterator it = merge.getLayersUsed(); it.hasNext(); )
			{
				Layer layer = (Layer)it.next();
				List list = merge.getMergedPoints(layer) ;

				// Temp solution until qtree implementation is ready
				// delete uncessary polygons. Doesn't insert poly if identical
				// to original. Very ineficient!!
				List rectList = (List)allLayers.get(layer);
				List delList = new ArrayList();

				for (Iterator iter = rectList.iterator(); iter.hasNext();)
				{
					Poly p = (Poly)iter.next();
					Rectangle2D rect = p.getBounds2D();

					for (Iterator i = list.iterator(); i.hasNext();)
					{
						Poly poly = (Poly)i.next();
						Rectangle2D r = poly.getBounds2D();

						if (r.equals(rect))
						{
							delList.add(poly);
						}
					}
				}
				for (Iterator iter = delList.iterator(); iter.hasNext();)
				{
					list.remove(iter.next());
				}

				// Ready to create new implants.
				for(Iterator i = list.iterator(); i.hasNext(); )
				{
					Poly poly = (Poly)i.next();
					Rectangle2D rect = poly.getBounds2D();
					Point2D center = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
					PrimitiveNode priNode = layer.getPureLayerNode();
					// Adding the new implant. New implant not assigned to any local variable                                .
					NodeInst node = NodeInst.makeInstance(priNode, center, rect.getWidth(), rect.getHeight(), 0, curCell, null);
					Highlight.addElectricObject(node, curCell);
					// New implant can't be selected again
					node.setHardSelect();
					nodesList.add(node);
				}
			}
			Highlight.finished();
			for (Iterator it = deleteList.iterator(); it.hasNext(); )
			{
				NodeInst node = (NodeInst)it .next();
				node.kill();
			}
			if ( nodesList.isEmpty() )
				System.out.println("No implant areas added");
		}
	}
	// ---------------------- THE JON GAINSLEY MENU -----------------

	public static void listVarsOnObject(boolean useproto) {
		if (Highlight.getNumHighlights() == 0) {
			// list vars on cell
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (wf == null) return;
            Cell cell = wf.getContent().getCell();
            cell.getInfo();
			return;
		}
		for (Iterator it = Highlight.getHighlights(); it.hasNext();) {
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
		EditWindow curEdit = EditWindow.needCurrent();
		if (Highlight.getNumHighlights() == 0) return;
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
		ReadELIB job = new ReadELIB(url);
//		OpenBinLibraryThread oThread = new OpenBinLibraryThread("/export/gainsley/soesrc_java/test/purpleFour.elib");
//		oThread.start();
	}

	public static void whitDiffieCommand()
	{
		MakeWhitDesign job = new MakeWhitDesign();
	}

	private static class MakeWhitDesign extends Job
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
				// correct data
//				"a1f0f1k0p0", "a2f1f2k1p1", "a3f2f3k2p2", "a0a4f0f3f4k3p3", "a0a5f0f4f5k4p4", "a6f5f6k5p5", "a0a7f0f6f7k6p6", "a0f0f7k7p7",
//				"a0f1k0k1p0", "a1f2k1k2p1", "a2f3k2k3p2", "a3f0f4k0k3k4p3", "a4f0f5k0k4k5p4", "a5f6k5k6p5", "a6f0f7k0k6k7p6", "a7f0k0k7p7",
//				"a0f0k1p0p1", "a1f1k2p1p2", "a2f2k3p2p3", "a3f3k0k4p0p3p4", "a4f4k0k5p0p4p5", "a5f5k6p5p6", "a6f6k0k7p0p6p7", "a7f7k0p0p7",
//				"a0a1f0k0p1", "a1a2f1k1p2", "a2a3f2k2p3", "a0a3a4f3k3p0p4", "a0a4a5f4k4p0p5", "a5a6f5k5p6", "a0a6a7f6k6p0p7", "a0a7f7k7p0",
//				"e1j0j1o0d0", "e2j1j2o1d1", "e3j2j3o2d2", "e0e4j0j3j4o3d3", "e0e5j0j4j5o4d4", "e6j5j6o5d5", "e0e7j0j6j7o6d6", "e0j0j7o7d7",
//				"e0j1o0o1d0", "e1j2o1o2d1", "e2j3o2o3d2", "e3j0j4o0o3o4d3", "e4j0j5o0o4o5d4", "e5j6o5o6d5", "e6j0j7o0o6o7d6", "e7j0o0o7d7",
//				"e0j0o1d0d1", "e1j1o2d1d2", "e2j2o3d2d3", "e3j3o0o4d0d3d4", "e4j4o0o5d0d4d5", "e5j5o6d5d6", "e6j6o0o7d0d6d7", "e7j7o0d0d7",
//				"e0e1j0o0d1", "e1e2j1o1d2", "e2e3j2o2d3", "e0e3e4j3o3d0d4", "e0e4e5j4o4d0d5", "e5e6j5o5d6", "e0e6e7j6o6d0d7", "e0e7j7o7d0",
//				"i1n0n1c0h0", "i2n1n2c1h1", "i3n2n3c2h2", "i0i4n0n3n4c3h3", "i0i5n0n4n5c4h4", "i6n5n6c5h5", "i0i7n0n6n7c6h6", "i0n0n7c7h7",
//				"i0n1c0c1h0", "i1n2c1c2h1", "i2n3c2c3h2", "i3n0n4c0c3c4h3", "i4n0n5c0c4c5h4", "i5n6c5c6h5", "i6n0n7c0c6c7h6", "i7n0c0c7h7",
//				"i0n0c1h0h1", "i1n1c2h1h2", "i2n2c3h2h3", "i3n3c0c4h0h3h4", "i4n4c0c5h0h4h5", "i5n5c6h5h6", "i6n6c0c7h0h6h7", "i7n7c0h0h7",
//				"i0i1n0c0h1", "i1i2n1c1h2", "i2i3n2c2h3", "i0i3i4n3c3h0h4", "i0i4i5n4c4h0h5", "i5i6n5c5h6", "i0i6i7n6c6h0h7", "i0i7n7c7h0",
//				"m1b0b1g0l0", "m2b1b2g1l1", "m3b2b3g2l2", "m0m4b0b3b4g3l3", "m0m5b0b4b5g4l4", "m6b5b6g5l5", "m0m7b0b6b7g6l6", "m0b0b7g7l7",
//				"m0b1g0g1l0", "m1b2g1g2l1", "m2b3g2g3l2", "m3b0b4g0g3g4l3", "m4b0b5g0g4g5l4", "m5b6g5g6l5", "m6b0b7g0g6g7l6", "m7b0g0g7l7",
//				"m0b0g1l0l1", "m1b1g2l1l2", "m2b2g3l2l3", "m3b3g0g4l0l3l4", "m4b4g0g5l0l4l5", "m5b5g6l5l6", "m6b6g0g7l0l6l7", "m7b7g0l0l7",
//				"m0m1b0g0l1", "m1m2b1g1l2", "m2m3b2g2l3", "m0m3m4b3g3l0l4", "m0m4m5b4g4l0l5", "m5m6b5g5l6", "m0m6m7b6g6l0l7", "m0m7b7g7l0"

				// sorted data
//				"a1f0f1k0p0", "a2f1f2k1p1", "a3f2f3k2p2", "a0a4f0f3f4k3p3", "a0a5f0f4f5k4p4", "a6f5f6k5p5", "a0a7f0f6f7k6p6", "a0f0f7k7p7",
//				"a0f1k0k1p0", "a1f2k1k2p1", "a2f3k2k3p2", "a3f0f4k0k3k4p3", "a4f0f5k0k4k5p4", "a5f6k5k6p5", "a6f0f7k0k6k7p6", "a7f0k0k7p7",
//				"a0f0k1p0p1", "a1f1k2p1p2", "a2f2k3p2p3", "a3f3k0k4p0p3p4", "a4f4k0k5p0p4p5", "a5f5k6p5p6", "a6f6k0k7p0p6p7", "a7f7k0p0p7",
//				"a0a1f0k0p1", "a1a2f1k1p2", "a2a3f2k2p3", "a0a3a4f3k3p0p4", "a0a4a5f4k4p0p5", "a5a6f5k5p6", "a0a6a7f6k6p0p7", "a0a7f7k7p0",
//				"d0e1j0j1o0", "d1e2j1j2o1", "d2e3j2j3o2", "d3e0e4j0j3j4o3", "d4e0e5j0j4j5o4", "d5e6j5j6o5", "d6e0e7j0j6j7o6", "d7e0j0j7o7",
//				"d0e0j1o0o1", "d1e1j2o1o2", "d2e2j3o2o3", "d3e3j0j4o0o3o4", "d4e4j0j5o0o4o5", "d5e5j6o5o6", "d6e6j0j7o0o6o7", "d7e7j0o0o7",
//				"d0d1e0j0o1", "d1d2e1j1o2", "d2d3e2j2o3", "d0d3d4e3j3o0o4", "d0d4d5e4j4o0o5", "d5d6e5j5o6", "d0d6d7e6j6o0o7", "d0d7e7j7o0",
//				"d1e0e1j0o0", "d2e1e2j1o1", "d3e2e3j2o2", "d0d4e0e3e4j3o3", "d0d5e0e4e5j4o4", "d6e5e6j5o5", "d0d7e0e6e7j6o6", "d0e0e7j7o7",
//				"c0h0i1n0n1", "c1h1i2n1n2", "c2h2i3n2n3", "c3h3i0i4n0n3n4", "c4h4i0i5n0n4n5", "c5h5i6n5n6", "c6h6i0i7n0n6n7", "c7h7i0n0n7",
//				"c0c1h0i0n1", "c1c2h1i1n2", "c2c3h2i2n3", "c0c3c4h3i3n0n4", "c0c4c5h4i4n0n5", "c5c6h5i5n6", "c0c6c7h6i6n0n7", "c0c7h7i7n0",
//				"c1h0h1i0n0", "c2h1h2i1n1", "c3h2h3i2n2", "c0c4h0h3h4i3n3", "c0c5h0h4h5i4n4", "c6h5h6i5n5", "c0c7h0h6h7i6n6", "c0h0h7i7n7",
//				"c0h1i0i1n0", "c1h2i1i2n1", "c2h3i2i3n2", "c3h0h4i0i3i4n3", "c4h0h5i0i4i5n4", "c5h6i5i6n5", "c6h0h7i0i6i7n6", "c7h0i0i7n7",
//				"b0b1g0l0m1", "b1b2g1l1m2", "b2b3g2l2m3", "b0b3b4g3l3m0m4", "b0b4b5g4l4m0m5", "b5b6g5l5m6", "b0b6b7g6l6m0m7", "b0b7g7l7m0",
//				"b1g0g1l0m0", "b2g1g2l1m1", "b3g2g3l2m2", "b0b4g0g3g4l3m3", "b0b5g0g4g5l4m4", "b6g5g6l5m5", "b0b7g0g6g7l6m6", "b0g0g7l7m7",
//				"b0g1l0l1m0", "b1g2l1l2m1", "b2g3l2l3m2", "b3g0g4l0l3l4m3", "b4g0g5l0l4l5m4", "b5g6l5l6m5", "b6g0g7l0l6l7m6", "b7g0l0l7m7",
//				"b0g0l1m0m1", "b1g1l2m1m2", "b2g2l3m2m3", "b3g3l0l4m0m3m4", "b4g4l0l5m0m4m5", "b5g5l6m5m6", "b6g6l0l7m0m6m7", "b7g7l0m0m7"

				// original data
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

				// bad data
				"a1f0f1k0p0", "a2f1f2k1p1", "a3f2f3k2p2", "a0a4f0f3f4k3p3", "a0a5f0f4f5k4p4", "a6f5f6k5p5", "a0a7f0f6f7k6p6", "a0f0f7k7p7",
				"a0f1k0k1p0", "a1f2k1k2p1", "a2f3k2k3p2", "a3f0f4k0k3k4p3", "a4f0f5k0k4k5p4", "a5f6k5k6p5", "a6f0f7k0k6k7p6", "a7f0k0k7p7",
				"a0f0k1p0p1", "a1f1k2p1p2", "a2f2k3p2p3", "a3f3k0k4p0p3p4", "a4f4k0k5p0p4p5", "a5f5k6p5p6", "a6f6k0k7p0p6p7", "a7f7k0p0p7",
				"a0a1f0k0p1", "a1a2f1k1p2", "a2a3f2k2p3", "a0a3a4f3k3p0p4", "a0a4a5f4k4p0p5", "a5a6f5k5p6", "a0a6a7f6k6p0p7", "a0a7f7k7p0",
				"b1g0g1l0m0", "b2g1g2l1m1", "b3g2g3l2m2", "b0b4g0g3g4l3m3", "b0b5g0g4g5l4m4", "b6g5g6l5m5", "b0b7g0g6g7l6m6", "b0g0g7l7m7",
				"b0g1l0l1m0", "b1g2l1l2m1", "b2g3l2l3m2", "b3g0g4l0l3l4m3", "b4g0g5l0l4l5m4", "b5g6l5l6m5", "b6g0g7l0l6l7m6", "b7g0l0l7m7",
				"b0g0l1m0m1", "b1g1l2m1m2", "b2g2l3m2m3", "b3g3l0l4m0m3m4", "b4g4l0l5m0m4m5", "b5g5l6m5m6", "b6g6l0l7m0m6m7", "b7g7l0m0m7",
				"b0b1g0l0m1", "b1b2g1l1m2", "b2b3g2l2m3", "b0b3b4g3l3m0m4", "b0b4b5g4l4m0m5", "b5b6g5l5m6", "b0b6b7g6l6m0m7", "b0b7g7l7m0",
				"c1h0h1i0n0", "c2h1h2i1n1", "c3h2h3i2n2", "c0c4h0h3h4i3n3", "c0c5h0h4h5i4n4", "c6h5h6i5n5", "c0c7h0h6h7i6n6", "c0h0h7i7n7",
				"c0h1i0i1n0", "c1h2i1i2n1", "c2h3i2i3n2", "c3h0h4i0i3i4n3", "c4h0h5i0i4i5n4", "c5h6i5i6n5", "c6h0h7i0i6i7n6", "c7h0i0i7n7",
				"c0h0i1n0n1", "c1h1i2n1n2", "c2h2i3n2n3", "c3h3i0i4n0n3n4", "c4h4i0i5n0n4n5", "c5h5i6n5n6", "c6h6i0i7n0n6n7", "c7h7i0n0n7",
				"c0c1h0i0n1", "c1c2h1i1n2", "c2c3h2i2n3", "c0c3c4h3i3n0n4", "c0c4c5h4i4n0n5", "c5c6h5i5n6", "c0c6c7h6i6n0n7", "c0c7h7i7n0",
				"d1e0e1j0o0", "d2e1e2j1o1", "d3e2e3j2o2", "d0d4e0e3e4j3o3", "d0d5e0e4e5j4o4", "d6e5e6j5o5", "d0d7e0e6e7j6o6", "d0e0e7j7o7",
				"d0e1j0j1o0", "d1e2j1j2o1", "d2e3j2j3o2", "d3e0e4j0j3j4o3", "d4e0e5j0j4j5o4", "d5e6j5j6o5", "d6e0e7j0j6j7o6", "d7e0j0j7o7",
				"d0e0j1o0o1", "d1e1j2o1o2", "d2e2j3o2o3", "d3e3j0j4o0o3o4", "d4e4j0j5o0o4o5", "d5e5j6o5o6", "d6e6j0j7o0o6o7", "d7e7j0o0o7",
				"d0d1e0j0o1", "d1d2e1j1o2", "d2d3e2j2o3", "d0d3d4e3j3o0o4", "d0d4d5e4j4o0o5", "d5d6e5j5o6", "d0d6d7e6j6o0o7", "d0d7e7j7o0"
			};

			boolean threePage = false;
			for(int v=0; v<33; v++)
			{
				if (v != 32) continue;
				String title = "whit";
				if (v < 16) title += "Input" + (v+1); else
					if (v < 32) title += "Output" + (v-15);
				Cell myCell = Cell.newInstance(Library.getCurrent(), title+"{sch}");

				// create the input and output pins
				NodeProto pinNp = com.sun.electric.technology.technologies.Generic.tech.universalPinNode;
				NodeInst [] inputs = new NodeInst[128];
				NodeInst [] outputs = new NodeInst[128];
				NodeInst [] inputsAbove = new NodeInst[128];
				NodeInst [] outputsAbove = new NodeInst[128];
				NodeInst [] inputsBelow = new NodeInst[128];
				NodeInst [] outputsBelow = new NodeInst[128];
				for(int j=0; j<3; j++)
				{
					if (!threePage && j != 1) continue;
					for(int i=0; i<128; i++)
					{
						// the input side
						int index = i;
						if (j == 0) index += 128; else
							if (j == 2) index -= 128;
						NodeInst in = NodeInst.newInstance(pinNp, new Point2D.Double(-200.0, index*5), 1, 1, 0, myCell, null);
						switch (j)
						{
							case 0: inputsAbove[i] = in;   break;
							case 1: inputs[i] = in;        break;
							case 2: inputsBelow[i] = in;   break;
						}
						if (j == 1)
						{
							NodeInst leftArrow = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.boxNode,
								new Point2D.Double(-267, i*5), 10, 0, 0, myCell, null);
							NodeInst leftArrowHead = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.arrowNode,
								new Point2D.Double(-264, i*5), 4, 4, 0, myCell, null);
						}

						// the output side
						NodeInst out = NodeInst.newInstance(pinNp, new Point2D.Double(200.0, index*5), 0, 0, 0, myCell, null);
						switch (j)
						{
							case 0: outputsAbove[i] = out;   break;
							case 1: outputs[i] = out;        break;
							case 2: outputsBelow[i] = out;   break;
						}
						if (j == 1)
						{
							NodeInst circle = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.circleNode, new Point2D.Double(202.5, i*5), 5, 5, 0, myCell, null);
							NodeInst horiz = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.boxNode,
								new Point2D.Double(202.5, i*5), 4, 0, 0, myCell, null);
							NodeInst vert = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.boxNode,
								new Point2D.Double(202.5, i*5), 0, 4, 0, myCell, null);
							NodeInst rightArrow = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.boxNode,
								new Point2D.Double(210, i*5), 10, 0, 0, myCell, null);
							NodeInst rightArrowHead = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.arrowNode,
								new Point2D.Double(213, i*5), 4, 4, 0, myCell, null);
						}
					}
				}
				for(int i=0; i<16; i++)
				{
					NodeInst inputBox = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.boxNode,
						new Point2D.Double(-222.0, i*5*8+20-2.5), 40, 40, 0, myCell, null);
					Variable inVar = inputBox.newVar("label", "S-box");
					inVar.setDisplay();
					inVar.getTextDescriptor().setRelSize(12);
				}
				NodeInst inputBox1 = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.boxNode,
					new Point2D.Double(-252.0, 320-2.5), 20, 640, 0, myCell, null);
				Variable inVar1 = inputBox1.newVar("label", "Keying");
				inVar1.setDisplay();
				inVar1.getTextDescriptor().setRotation(TextDescriptor.Rotation.ROT90);
				inVar1.getTextDescriptor().setRelSize(15);
				NodeInst inputBox2 = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.boxNode,
					new Point2D.Double(-282.0, 320-2.5), 20, 640, 0, myCell, null);
				Variable inVar2 = inputBox2.newVar("label", "Input");
				inVar2.setDisplay();
				inVar2.getTextDescriptor().setRotation(TextDescriptor.Rotation.ROT90);
				inVar2.getTextDescriptor().setRelSize(15);

				NodeInst outputBox = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.boxNode,
					new Point2D.Double(225.0, 320-2.5), 20, 640, 0, myCell, null);
				Variable inVar3 = outputBox.newVar("label", "Output");
				inVar3.setDisplay();
				inVar3.getTextDescriptor().setRotation(TextDescriptor.Rotation.ROT90);
				inVar3.getTextDescriptor().setRelSize(15);

				NodeInst titleBox = NodeInst.newInstance(com.sun.electric.technology.technologies.Generic.tech.invisiblePinNode,
					new Point2D.Double(0, 670), 0, 0, 0, myCell, null);
				Variable inVar4 = titleBox.newVar("label", "One Round of AES");
				inVar4.setDisplay();
				inVar4.getTextDescriptor().setRelSize(24);

				// wire them together
				ArcProto wire = com.sun.electric.technology.technologies.Generic.tech.universal_arc;
				for(int i=0; i<theStrings.length; i++)
				{
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

						// handle wrapping
						if (threePage && Math.abs(index - i) > 64)
						{
							if (i < 64)
							{
								PortInst inPort = inputsBelow[index].getOnlyPortInst();
								PortInst outPort = outputs[i].getOnlyPortInst();
								ArcInst.newInstance(wire, 0, inPort, outPort, null);
								inPort = inputs[index].getOnlyPortInst();
								outPort = outputsAbove[i].getOnlyPortInst();
								ArcInst.newInstance(wire, 0, inPort, outPort, null);
							} else
							{
								PortInst inPort = inputsAbove[index].getOnlyPortInst();
								PortInst outPort = outputs[i].getOnlyPortInst();
								ArcInst.newInstance(wire, 0, inPort, outPort, null);
								inPort = inputs[index].getOnlyPortInst();
								outPort = outputsBelow[i].getOnlyPortInst();
								ArcInst.newInstance(wire, 0, inPort, outPort, null);
							}
						} else
						{
							PortInst inPort = inputs[index].getOnlyPortInst();
							PortInst outPort = outputs[i].getOnlyPortInst();
							ArcInst.newInstance(wire, 0, inPort, outPort, null);
						}
					}
				}

				// display the full drawing
				if (v == 32) WindowFrame.createEditWindow(myCell);
			}
		}
	}

}
