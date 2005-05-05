/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EditMenu.java
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

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.FPGA;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.extract.LayerCoverageJob;
import com.sun.electric.tool.extract.LayerCoverageJob;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Clipboard;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.Array;
import com.sun.electric.tool.user.dialogs.ArtworkLook;
import com.sun.electric.tool.user.dialogs.Attributes;
import com.sun.electric.tool.user.dialogs.Change;
import com.sun.electric.tool.user.dialogs.ChangeText;
import com.sun.electric.tool.user.dialogs.EditKeyBindings;
import com.sun.electric.tool.user.dialogs.FindText;
import com.sun.electric.tool.user.dialogs.GetInfoArc;
import com.sun.electric.tool.user.dialogs.GetInfoExport;
import com.sun.electric.tool.user.dialogs.GetInfoMulti;
import com.sun.electric.tool.user.dialogs.GetInfoNode;
import com.sun.electric.tool.user.dialogs.GetInfoOutline;
import com.sun.electric.tool.user.dialogs.GetInfoText;
import com.sun.electric.tool.user.dialogs.MoveBy;
import com.sun.electric.tool.user.dialogs.SelectObject;
import com.sun.electric.tool.user.dialogs.Spread;
import com.sun.electric.tool.user.tecEdit.Calibre;
import com.sun.electric.tool.user.tecEdit.Manipulate;
import com.sun.electric.tool.user.tecEdit.TechToLib;
import com.sun.electric.tool.user.tecEdit.LibToTech;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.OutlineListener;
import com.sun.electric.tool.user.ui.SizeListener;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WaveformWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

/**
 * Class to handle the commands in the "Edit" pulldown menu.
 */
public class EditMenu {

	protected static void addEditMenu(MenuBar menuBar) {
        MenuBar.MenuItem m;
		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		/****************************** THE EDIT MENU ******************************/

		// mnemonic keys available:  B       JK     Q     W  
		// still don't have mnemonic for "Repeat Last Action"
		MenuBar.Menu editMenu = MenuBar.makeMenu("_Edit");
        menuBar.add(editMenu);

		editMenu.addMenuItem("Cu_t", KeyStroke.getKeyStroke('X', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Clipboard.cut(); } });
		editMenu.addMenuItem("Cop_y", KeyStroke.getKeyStroke('C', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Clipboard.copy(); } });
		editMenu.addMenuItem("_Paste", KeyStroke.getKeyStroke('V', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Clipboard.paste(); } });
        editMenu.addMenuItem("Dup_licate", KeyStroke.getKeyStroke('M', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { Clipboard.duplicate(); } });

		editMenu.addSeparator();

		MenuBar.MenuItem undo = editMenu.addMenuItem("_Undo", KeyStroke.getKeyStroke('Z', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { undoCommand(); } });
        Undo.addPropertyChangeListener(new MenuCommands.MenuEnabler(undo, Undo.propUndoEnabled));
        undo.setEnabled(Undo.getUndoEnabled());
        // TODO: figure out how to remove this property change listener for correct garbage collection
		MenuBar.MenuItem redo = editMenu.addMenuItem("Re_do", KeyStroke.getKeyStroke('Y', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { redoCommand(); } });
        Undo.addPropertyChangeListener(new MenuCommands.MenuEnabler(redo, Undo.propRedoEnabled));
        redo.setEnabled(Undo.getRedoEnabled());
        // TODO: figure out how to remove this property change listener for correct garbage collection
        MenuBar.MenuItem repeatAction = editMenu.addMenuItem("Repeat Last Action", KeyStroke.getKeyStroke(KeyEvent.VK_AMPERSAND, 0),
            new ActionListener() { public void actionPerformed(ActionEvent e) { repeatLastCommand(); } });
        MenuBar.repeatLastCommandListener.setRepeatActionMenu(repeatAction);

		editMenu.addSeparator();

		// mnemonic keys available: AB  EFGHIJKLMN PQRSTUV XYZ
		MenuBar.Menu rotateSubMenu = MenuBar.makeMenu("_Rotate");
		editMenu.add(rotateSubMenu);
		rotateSubMenu.addMenuItem("90 Degrees Clock_wise", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.rotateObjects(2700); }});
		rotateSubMenu.addMenuItem("90 Degrees _Counterclockwise", KeyStroke.getKeyStroke('J', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.rotateObjects(900); }});
		rotateSubMenu.addMenuItem("180 _Degrees", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.rotateObjects(1800); }});
		rotateSubMenu.addMenuItem("_Other...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.rotateObjects(0); }});

		// mnemonic keys available: ABCDEFG IJKLMNOPQRSTU WXYZ
		MenuBar.Menu mirrorSubMenu = MenuBar.makeMenu("_Mirror");
		editMenu.add(mirrorSubMenu);
		mirrorSubMenu.addMenuItem("_Horizontally (flip over X-axis)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.mirrorObjects(true); }});
		mirrorSubMenu.addMenuItem("_Vertically (flip over Y-axis)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.mirrorObjects(false); }});

		// mnemonic keys available:  BCDEFGH JKLM OPQRSTUVWXYZ
		MenuBar.Menu sizeSubMenu = MenuBar.makeMenu("Si_ze");
		editMenu.add(sizeSubMenu);
		sizeSubMenu.addMenuItem("_Interactively", KeyStroke.getKeyStroke('B', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { SizeListener.sizeObjects(); } });
		sizeSubMenu.addMenuItem("All Selected _Nodes...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { SizeListener.sizeAllNodes(); }});
		sizeSubMenu.addMenuItem("All Selected _Arcs...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { SizeListener.sizeAllArcs(); }});

		// mnemonic keys available:    DEFGHIJK  NOPQ   U WXYZ
		MenuBar.Menu moveSubMenu = MenuBar.makeMenu("Mo_ve");
		editMenu.add(moveSubMenu);
		moveSubMenu.addMenuItem("_Spread...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Spread.showSpreadDialog(); }});
		moveSubMenu.addMenuItem("_Move Objects By...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { MoveBy.showMoveByDialog(); }});
		moveSubMenu.addMenuItem("_Align to Grid", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.alignToGrid(); }});
		moveSubMenu.addSeparator();
		moveSubMenu.addMenuItem("Align Horizontally to _Left", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.alignNodes(true, 0); }});
		moveSubMenu.addMenuItem("Align Horizontally to _Right", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.alignNodes(true, 1); }});
		moveSubMenu.addMenuItem("Align Horizontally to _Center", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.alignNodes(true, 2); }});
		moveSubMenu.addSeparator();
		moveSubMenu.addMenuItem("Align Vertically to _Top", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.alignNodes(false, 0); }});
		moveSubMenu.addMenuItem("Align Vertically to _Bottom", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.alignNodes(false, 1); }});
		moveSubMenu.addMenuItem("Align _Vertically to Center", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.alignNodes(false, 2); }});

		editMenu.addSeparator();

		m=editMenu.addMenuItem("_Erase", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.deleteSelected(); } });
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), null);
		editMenu.addMenuItem("_Array...", KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Array.showArrayDialog(); } });
		editMenu.addMenuItem("C_hange...", KeyStroke.getKeyStroke('C', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Change.showChangeDialog(); } });

		editMenu.addSeparator();

		// mnemonic keys available: ABCDEFGHIJKLMNOPQRS  VWXYZ
		MenuBar.Menu editInfoSubMenu = MenuBar.makeMenu("In_fo");
		editMenu.add(editInfoSubMenu);
//		editInfoSubMenu.addMenuItem("List Layer Coverage", null,
//			new ActionListener() { public void actionPerformed(ActionEvent e) { LayerCoverageJob.layerCoverageCommand(Job.Type.EXAMINE, LayerCoverageJob.AREA, GeometryHandler.ALGO_SWEEP); } });
		editInfoSubMenu.addMenuItem("Show _Undo List", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { showUndoListCommand(); } });
		editInfoSubMenu.addMenuItem("Describe this _Technology", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { describeTechnologyCommand(); } });

		// mnemonic keys available:  BC EFG IJK M  PQR TUVWXYZ
		MenuBar.Menu editPropertiesSubMenu = MenuBar.makeMenu("Propert_ies");
		editMenu.add(editPropertiesSubMenu);
		editPropertiesSubMenu.addMenuItem("_Object Properties...", KeyStroke.getKeyStroke('I', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { getInfoCommand(false); } });
		editPropertiesSubMenu.addMenuItem("_Attribute Properties...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Attributes.showDialog(); } });
		editPropertiesSubMenu.addSeparator();
		editPropertiesSubMenu.addMenuItem("_See All Attributes on Node", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { seeAllParametersCommand(); } });
		editPropertiesSubMenu.addMenuItem("_Hide All Attributes on Node", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { hideAllParametersCommand(); } });
		editPropertiesSubMenu.addMenuItem("_Default Attribute Visibility", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { defaultParamVisibilityCommand(); } });
        editPropertiesSubMenu.addMenuItem("Update Attributes Inheritance on _Node", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { updateInheritance(false); } });
        editPropertiesSubMenu.addMenuItem("Update Attributes Inheritance all _Libraries", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { updateInheritance(true); } });

		// mnemonic keys available:   C E G I KLM OPQ S UVWXYZ
		MenuBar.Menu arcSubMenu = MenuBar.makeMenu("Ar_c");
		editMenu.add(arcSubMenu);
		arcSubMenu.addMenuItem("_Rigid", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcRigidCommand(); }});
		arcSubMenu.addMenuItem("_Not Rigid", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcNotRigidCommand(); }});
		arcSubMenu.addMenuItem("_Fixed Angle", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcFixedAngleCommand(); }});
		arcSubMenu.addMenuItem("Not Fixed _Angle", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcNotFixedAngleCommand(); }});
		arcSubMenu.addSeparator();
		arcSubMenu.addMenuItem("Toggle _Directionality", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcDirectionalCommand(); }});
		arcSubMenu.addMenuItem("Toggle End Extension of _Head", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcHeadExtendCommand(); }});
		arcSubMenu.addMenuItem("Toggle End Extension of _Tail", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcTailExtendCommand(); }});
		arcSubMenu.addSeparator();
		arcSubMenu.addMenuItem("Insert _Jog In Arc", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { insertJogInArcCommand(); } });
		arcSubMenu.addMenuItem("Rip _Bus", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.ripBus(); }});

		// mnemonic keys available: ABCD FGHIJKL NOPQR TUVWXYZ
		MenuBar.Menu modeSubMenu = MenuBar.makeMenu("M_odes");
		editMenu.add(modeSubMenu); 

		// mnemonic keys available: ABCDEFGHIJKLMNOPQRSTUVWXYZ
		MenuBar.Menu modeSubMenuEdit = MenuBar.makeMenu("_Edit");
		modeSubMenu.add(modeSubMenuEdit);
		ButtonGroup editGroup = new ButtonGroup();
        JMenuItem cursorClickZoomWire, cursorSelect, cursorWiring, cursorPan, cursorZoom, cursorOutline, cursorMeasure;
		cursorClickZoomWire = modeSubMenuEdit.addRadioButton(ToolBar.cursorClickZoomWireName, true, editGroup, KeyStroke.getKeyStroke('S', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.clickZoomWireCommand(); } });
        ToolBar.CursorMode cm = ToolBar.getCursorMode();
        if (cm == ToolBar.CursorMode.CLICKZOOMWIRE) cursorClickZoomWire.setSelected(true);

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

		// mnemonic keys available: ABCDEFGHIJKLMNOPQRSTUVWXYZ
		MenuBar.Menu modeSubMenuMovement = MenuBar.makeMenu("_Movement");
		modeSubMenu.add(modeSubMenuMovement);
		ButtonGroup movementGroup = new ButtonGroup();
        JMenuItem moveFull, moveHalf, moveQuarter;
		moveFull = modeSubMenuMovement.addRadioButton(ToolBar.moveFullName, true, movementGroup, KeyStroke.getKeyStroke('F', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.fullArrowDistanceCommand(); } });
		moveHalf = modeSubMenuMovement.addRadioButton(ToolBar.moveHalfName, false, movementGroup, KeyStroke.getKeyStroke('H', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.halfArrowDistanceCommand(); } });
		moveQuarter = modeSubMenuMovement.addRadioButton(ToolBar.moveQuarterName, false, movementGroup, null, // do not put shortcut in here! Too dangerous!!
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.quarterArrowDistanceCommand(); } });
		double ad = User.getAlignmentToGrid();
		if (ad == 1.0) moveFull.setSelected(true); else
		if (ad == 0.5) moveHalf.setSelected(true); else
			moveQuarter.setSelected(true);

		// mnemonic keys available: ABCDEFGHIJKLMNOPQRSTUVWXYZ
		MenuBar.Menu modeSubMenuSelect = MenuBar.makeMenu("_Select");
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

		// mnemonic keys available: AB  E GH JKLMNOPQRSTUVWXYZ
		MenuBar.Menu textSubMenu = MenuBar.makeMenu("Te_xt");
		editMenu.add(textSubMenu);
		textSubMenu.addMenuItem("_Find Text...", KeyStroke.getKeyStroke('L', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { FindText.findTextDialog(); }});
		textSubMenu.addMenuItem("_Change Text Size...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ChangeText.changeTextDialog(); }});
		textSubMenu.addMenuItem("_Increase All Text Size", KeyStroke.getKeyStroke('=', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { changeGlobalTextSize(1.25); }});
		textSubMenu.addMenuItem("_Decrease All Text Size", KeyStroke.getKeyStroke('-', buckyBit),
				new ActionListener() { public void actionPerformed(ActionEvent e) { changeGlobalTextSize(0.8); }});

		// mnemonic keys available: ABCD FGHIJK M O QR TUVWXYZ
		MenuBar.Menu cleanupSubMenu = MenuBar.makeMenu("Clea_nup Cell");
		editMenu.add(cleanupSubMenu);
		cleanupSubMenu.addMenuItem("Cleanup _Pins", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.cleanupPinsCommand(false); }});
		cleanupSubMenu.addMenuItem("Cleanup Pins _Everywhere", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.cleanupPinsCommand(true); }});
		cleanupSubMenu.addMenuItem("Show _Nonmanhattan", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.showNonmanhattanCommand(); }});
		cleanupSubMenu.addMenuItem("Show Pure _Layer Nodes", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.showPureLayerCommand(); }});
		cleanupSubMenu.addMenuItem("_Shorten Selected Arcs", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.shortenArcsCommand(); }});

		// mnemonic keys available:     E GH JK M O QRS UVWXYZ
		MenuBar.Menu specialSubMenu = MenuBar.makeMenu("Technolo_gy Specific");
		editMenu.add(specialSubMenu);
		specialSubMenu.addMenuItem("Toggle Port _Negation", KeyStroke.getKeyStroke('T', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.toggleNegatedCommand(); }});
		specialSubMenu.addMenuItem("_Artwork Appearance...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ArtworkLook.showArtworkLookDialog(); }});

		specialSubMenu.addSeparator();

		// mnemonic keys available:  BCDEFGHIJKLMNO QRSTUVWXYZ
		MenuBar.Menu fpgaSubMenu = MenuBar.makeMenu("_FPGA");
		specialSubMenu.add(fpgaSubMenu);
		fpgaSubMenu.addMenuItem("Read _Architecture And Primitives...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FPGA.readArchitectureFile(true); }});
		fpgaSubMenu.addMenuItem("Read _Primitives...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FPGA.readArchitectureFile(false); }});
		fpgaSubMenu.addSeparator();
		fpgaSubMenu.addMenuItem("Edit Pips...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FPGA.programPips(); }});
		fpgaSubMenu.addSeparator();
		fpgaSubMenu.addMenuItem("Show No Wires", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FPGA.setWireDisplay(0); }});
		fpgaSubMenu.addMenuItem("Show Active Wires", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FPGA.setWireDisplay(1); }});
		fpgaSubMenu.addMenuItem("Show All Wires", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FPGA.setWireDisplay(2); }});
		fpgaSubMenu.addSeparator();
		fpgaSubMenu.addMenuItem("Show Text", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FPGA.setTextDisplay(true); }});
		fpgaSubMenu.addMenuItem("Hide Text", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { FPGA.setTextDisplay(false); }});

		specialSubMenu.addSeparator();
		specialSubMenu.addMenuItem("Convert Technology to _Library for Editing...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { TechToLib.makeLibFromTech(); }});
		specialSubMenu.addMenuItem("Import Cali_bre File...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Calibre.readCalibre(); }});
		specialSubMenu.addMenuItem("Convert Library to _Technology...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { LibToTech.makeTechFromLib(); }});
		specialSubMenu.addSeparator();
		specialSubMenu.addMenuItem("_Identify Primitive Layers", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.identifyLayers(false); }});
		specialSubMenu.addMenuItem("Identify _Ports", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.identifyLayers(true); }});
		specialSubMenu.addSeparator();
		specialSubMenu.addMenuItem("Edit Library _Dependencies...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.editLibraryDependencies(); }});
		specialSubMenu.addMenuItem("Do_cument Current Technology", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.describeTechnology(Technology.getCurrent()); }});

		// mnemonic keys available:  B   F  I KLM  PQR       Z
		MenuBar.Menu selListSubMenu = MenuBar.makeMenu("_Selection");
		editMenu.add(selListSubMenu);
		selListSubMenu.addMenuItem("Sele_ct All", KeyStroke.getKeyStroke('A', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectAllCommand(); }});
		selListSubMenu.addMenuItem("Select All Like _This", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectAllLikeThisCommand(); }});
		selListSubMenu.addMenuItem("Select All _Easy", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectEasyCommand(); }});
		selListSubMenu.addMenuItem("Select All _Hard", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectHardCommand(); }});
		selListSubMenu.addMenuItem("Select Nothin_g", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectNothingCommand(); }});
		selListSubMenu.addSeparator();
		selListSubMenu.addMenuItem("_Select Object...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { SelectObject.selectObjectDialog(); }});
		selListSubMenu.addMenuItem("Deselect All _Arcs", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { deselectAllArcsCommand(); }});
		selListSubMenu.addSeparator();
		selListSubMenu.addMenuItem("Make Selected Eas_y", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectMakeEasyCommand(); }});
		selListSubMenu.addMenuItem("Make Selected Har_d", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectMakeHardCommand(); }});
		selListSubMenu.addSeparator();
		selListSubMenu.addMenuItem("P_ush Selection", KeyStroke.getKeyStroke('1', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) {
                EditWindow wnd = EditWindow.getCurrent(); if (wnd == null) return;
                wnd.getHighlighter().pushHighlight(); }});
		selListSubMenu.addMenuItem("P_op Selection", KeyStroke.getKeyStroke('3', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) {
                EditWindow wnd = EditWindow.getCurrent(); if (wnd ==null) return;
                wnd.getHighlighter().popHighlight(); }});
		selListSubMenu.addSeparator();
		selListSubMenu.addMenuItem("Enclosed Ob_jects", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectEnclosedObjectsCommand(); }});
		selListSubMenu.addSeparator();
		selListSubMenu.addMenuItem("Show Ne_xt Error", KeyStroke.getKeyStroke('>'),
			new ActionListener() { public void actionPerformed(ActionEvent e) { showNextErrorCommand(); }});
		selListSubMenu.addMenuItem("Show Pre_vious Error", KeyStroke.getKeyStroke('<'),
			new ActionListener() { public void actionPerformed(ActionEvent e) { showPrevErrorCommand(); }});
		selListSubMenu.addSeparator();
		selListSubMenu.addMenuItem("Add to Waveform in _New Panel", KeyStroke.getKeyStroke('A', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { addToWaveformNewCommand(); }});
		selListSubMenu.addMenuItem("Add to _Waveform in Current Panel", KeyStroke.getKeyStroke('O', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { addToWaveformCurrentCommand(); }});
    }

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

		public boolean doIt()
		{
			if (!Undo.undoABatch())
				System.out.println("Undo failed!");
			return true;
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

		public boolean doIt()
		{
			if (!Undo.redoABatch())
				System.out.println("Redo failed!");
			return true;
		}
	}

    /**
     * Repeat the last Command
     */
    public static void repeatLastCommand() {
        AbstractButton lastActivated = MenuBar.repeatLastCommandListener.getLastActivated();
        if (lastActivated != null)
        {
        	lastActivated.doClick();
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
		dialog.setVisible(true);
	}

	/**
	 * This method shows the GetInfo dialog for the highlighted nodes, arcs, and/or text.
	 */
	public static void getInfoCommand(boolean doubleClick)
	{
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
		if (wnd.getHighlighter().getNumHighlights() == 0)
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
			NodeInst theNode = null;
			for(Iterator it = wnd.getHighlighter().getHighlights().iterator(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				ElectricObject eobj = h.getElectricObject();
				if (h.getType() == Highlight.Type.EOBJ)
				{
					if (eobj instanceof NodeInst || eobj instanceof PortInst)
					{
						nodeCount++;
						if (eobj instanceof NodeInst) theNode = (NodeInst)eobj; else
							theNode = ((PortInst)eobj).getNodeInst();
					} else if (eobj instanceof ArcInst)
					{
						arcCount++;
					}
				} else if (h.getType() == Highlight.Type.TEXT)
				{
					if (eobj instanceof Export)
					{
                        if (h.getVar() != null)
                            textCount++;
                        else
                            exportCount++;
                    } else
                    {
                    	if (eobj instanceof NodeInst) theNode = (NodeInst)eobj;
						textCount++;
                    }
				} else if (h.getType() == Highlight.Type.BBOX)
				{
					graphicsCount++;
				} else if (h.getType() == Highlight.Type.LINE)
				{
					graphicsCount++;
				}
			}

			// in technology editing mode, handle double-click on special node or text
			if (doubleClick)
			{
				if (arcCount == 0 && exportCount == 0 && graphicsCount == 0 &&
					nodeCount + textCount == 1 && theNode != null)
				{
		    		// if double-clicked on a technology editing object, modify it
	    			int opt = Manipulate.getOptionOnNode(theNode);
	    			if (opt >= 0)
	    			{
	    				Manipulate.modifyObject(wnd, theNode, opt);
	    				return;
		    		}
				}
			}

			if (arcCount <= 1 && nodeCount <= 1 && exportCount <= 1 && textCount <= 1 && graphicsCount == 0)
			{
				if (arcCount == 1) GetInfoArc.showDialog();
				if (nodeCount == 1)
				{
					// if in outline-edit mode, show that dialog
			        if (WindowFrame.getListener() == OutlineListener.theOne)
			        {
			        	GetInfoOutline.showOutlinePropertiesDialog();
			        } else
			        {
			        	GetInfoNode.showDialog();
			        }
				}
				if (exportCount == 1)
				{
					if (doubleClick)
					{
						GetInfoText.editTextInPlace();
					} else
					{
						GetInfoExport.showDialog();
					}
				}
				if (textCount == 1)
				{
					if (doubleClick)
					{
						GetInfoText.editTextInPlace();
					} else
					{
						GetInfoText.showDialog();
					}
				}
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
		ParameterVisibility job = new ParameterVisibility(0, MenuCommands.getSelectedObjects(true, false));
	}

	/**
	 * Method to handle the "Hide All Parameters on Node" command.
	 */
	public static void hideAllParametersCommand()
	{
		ParameterVisibility job = new ParameterVisibility(1, MenuCommands.getSelectedObjects(true, false));
	}

	/**
	 * Method to handle the "Default Parameter Visibility" command.
	 */
	public static void defaultParamVisibilityCommand()
	{
		ParameterVisibility job = new ParameterVisibility(2, MenuCommands.getSelectedObjects(true, false));
	}

	/**
	 * Class to do antenna checking in a new thread.
	 */
	private static class ParameterVisibility extends Job
	{
		private int how;
        private List selected;

		protected ParameterVisibility(int how, List selected)
		{
			super("Change Parameter Visibility", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.how = how;
            this.selected = selected;
			startJob();
		}

		public boolean doIt()
		{
			// change visibility of parameters on the current node(s)
			int changeCount = 0;
			java.util.List list = selected;
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
							var.setDisplay(true);
							changed = true;
							break;
						case 1:			// make all parameters invisible
							if (!var.isDisplay()) continue;
							var.setDisplay(false);
							changed = true;
							break;
						case 2:			// make all parameters have default visiblity
							if (nVar.getTextDescriptor().isInterior())
							{
								// prototype wants parameter to be invisible
								if (!var.isDisplay()) continue;
								var.setDisplay(false);
								changed = true;
							} else
							{
								// prototype wants parameter to be visible
								if (var.isDisplay()) continue;
								var.setDisplay(true);
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
			return true;
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

    public static void updateInheritance(boolean allLibraries)
    {
        // get currently selected node(s)
        List highlighted = MenuCommands.getSelectedObjects(true, false);
        UpdateAttributes job = new UpdateAttributes(highlighted, allLibraries, 0);
    }

    private static class UpdateAttributes extends Job {
        private List highlighted;
        private boolean allLibraries;
        private int whatToUpdate;

        /**
         * Update Attributes.
         * @param highlighted currently highlighted objects
         * @param allLibraries if true, update all nodeinsts in all libraries, otherwise update
         * highlighted
         * @param whatToUpdate if 0, update inheritance. If 1, update attributes locations.
         */
        UpdateAttributes(List highlighted, boolean allLibraries, int whatToUpdate) {
            super("Update Inheritance", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.highlighted = highlighted;
            this.allLibraries = allLibraries;
            this.whatToUpdate = whatToUpdate;
            startJob();
        }

        public boolean doIt() {
            int count = 0;
            if (allLibraries) {
                for (Iterator it = Library.getLibraries(); it.hasNext(); ) {
                    Library lib = (Library)it.next();
                    for (Iterator it2 = lib.getCells(); it2.hasNext(); ) {
                        Cell c = (Cell)it2.next();
                        for (Iterator it3 = c.getNodes(); it3.hasNext(); ) {
                            NodeInst ni = (NodeInst)it3.next();
                            if (ni.getProto() instanceof Cell) {
                                if (whatToUpdate == 0) {
                                    updateInheritance(ni, (Cell)ni.getProto());
                                    count++;
                                }
                                if (whatToUpdate == 1) {
                                    updateLocations(ni, (Cell)ni.getProto());
                                    count++;
                                }
                            }
                        }
                    }
                }
            } else {
                for (Iterator it = highlighted.iterator(); it.hasNext(); ) {
                    ElectricObject eobj = (ElectricObject)it.next();
                    if (eobj instanceof NodeInst) {
                        NodeInst ni = (NodeInst)eobj;
                        if (ni.getProto() instanceof Cell) {
                            if (whatToUpdate == 0) {
                                updateInheritance(ni, (Cell)ni.getProto());
                                count++;
                            }
                            if (whatToUpdate == 1) {
                                updateLocations(ni, (Cell)ni.getProto());
                                count++;
                            }
                        }
                    }
                }
            }
            if (whatToUpdate == 0)
                System.out.println("Updated Attribute Inheritance on "+count+" nodes");
            if (whatToUpdate == 1)
                System.out.println("Updated Attribute Locations on "+count+" nodes");
            return true;
        }

        private void updateInheritance(NodeInst ni, Cell proto) {
            CircuitChanges.inheritAttributes(ni, true);
        }

        private void updateLocations(NodeInst ni, Cell proto) {

        }
    }

    /**
     * Method to change the global text scale by a given amount.
     * @param scale the amount to scale the global text size.
     */
    public static void changeGlobalTextSize(double scale)
    {
    	double curScale = User.getGlobalTextScale();
    	curScale *= scale;
    	if (curScale != 0)
    	{
    		User.setGlobalTextScale(curScale);
    		EditWindow.repaintAllContents();
    	}
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
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

		boolean cellsAreHard = !User.isEasySelectionOfCellInstances();
		highlighter.clear();
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
						highlighter.addText(ni, curCell, var, null);
						break;
					}
				}
			} else
			{
				highlighter.addElectricObject(ni, curCell);
			}
		}
		for(Iterator it = curCell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			boolean hard = ai.isHardSelect();
			if (mustBeEasy && hard) continue;
			if (mustBeHard && !hard) continue;
			highlighter.addElectricObject(ai, curCell);
		}
		// Selecting annotations
		for(Iterator it = curCell.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var.isAttribute())
				highlighter.addText(curCell, curCell, var, null);
		}
		highlighter.finished();
	}

	/**
	 * This method implements the command to highlight all objects in the current Cell
	 * that are like the currently selected object.
	 */
	public static void selectAllLikeThisCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

		HashMap likeThis = new HashMap();
		java.util.List highlighted = highlighter.getHighlightedEObjs(true, true);
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

		highlighter.clear();
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
						highlighter.addText(ni, curCell, var, null);
						break;
					}
				}
			} else
			{
				highlighter.addElectricObject(ni, curCell);
			}
		}
		for(Iterator it = curCell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			Object isLikeThis = likeThis.get(ai.getProto());
			if (isLikeThis == null) continue;
			highlighter.addElectricObject(ai, curCell);
		}
		highlighter.finished();
        System.out.println("Selected "+highlighter.getNumHighlights()+ " objects");
	}

	/**
	 * This method implements the command to highlight nothing in the current Cell.
	 */
	public static void selectNothingCommand()
	{
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
		wnd.getHighlighter().clear();
		wnd.getHighlighter().finished();
	}

	/**
	 * This method implements the command to deselect all selected arcs.
	 */
	public static void deselectAllArcsCommand()
	{
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

		java.util.List newHighList = new ArrayList();
		for(Iterator it = highlighter.getHighlights().iterator(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			if (h.getType() == Highlight.Type.EOBJ || h.getType() == Highlight.Type.TEXT)
			{
				if (h.getElectricObject() instanceof ArcInst) continue;
			}
			newHighList.add(h);
		}
		highlighter.clear();
		highlighter.setHighlightList(newHighList);
		highlighter.finished();
	}

	/**
	 * This method implements the command to make all selected objects be easy-to-select.
	 */
	public static void selectMakeEasyCommand()
	{
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
		java.util.List highlighted = wnd.getHighlighter().getHighlightedEObjs(true, true);
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
				ai.setHardSelect(false);
			}
		}
	}

	/**
	 * This method implements the command to make all selected objects be hard-to-select.
	 */
	public static void selectMakeHardCommand()
	{
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
		java.util.List highlighted = wnd.getHighlighter().getHighlightedEObjs(true, true);
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
				ai.setHardSelect(true);
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
        Highlighter highlighter = wnd.getHighlighter();
		Rectangle2D selection = highlighter.getHighlightedArea(wnd);
		highlighter.clear();
		if (selection != null )
			highlighter.selectArea(wnd, selection.getMinX(), selection.getMaxX(), selection.getMinY(), selection.getMaxY(), false,
			ToolBar.getSelectSpecial());
		highlighter.finished();
	}

	/**
	 * This method implements the command to show the next logged error.
	 * The error log lists the results of the latest command (DRC, NCC, etc.)
	 */
	public static void showNextErrorCommand()
	{
		String msg = ErrorLogger.reportNextMessage();
		System.out.println(msg);
	}

	/**
	 * This method implements the command to show the last logged error.
	 * The error log lists the results of the latest command (DRC, NCC, etc.)
	 */
	public static void showPrevErrorCommand()
	{
		String msg = ErrorLogger.reportPrevMessage();
		System.out.println(msg);
	}

	/**
	 * This method implements the command to add the currently selected network
	 * to the waveform window, in a new panel.
	 */
	public static void addToWaveformNewCommand()
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (!(wf.getContent() instanceof EditWindow)) return;
        EditWindow wnd = (EditWindow)wf.getContent();

		WaveformWindow.Locator wwLoc = new WaveformWindow.Locator(wnd);
		WaveformWindow ww = wwLoc.getWaveformWindow();
		if (ww == null)
		{
			System.out.println("Cannot add selected signals to the waveform window: no waveform window is associated with this cell");
			return;
		}
		Set nets = wnd.getHighlighter().getHighlightedNetworks();
		ww.showSignals(nets, wwLoc.getContext(), true);
	}

	/**
	 * This method implements the command to add the currently selected network
	 * to the waveform window, overlaid on top of the current panel.
	 */
	public static void addToWaveformCurrentCommand()
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (!(wf.getContent() instanceof EditWindow)) return;
        EditWindow wnd = (EditWindow)wf.getContent();
		WaveformWindow.Locator wwLoc = new WaveformWindow.Locator(wnd);
		WaveformWindow ww = wwLoc.getWaveformWindow();
		if (ww == null)
		{
			System.out.println("Cannot overlay selected signals to the waveform window: no waveform window is associated with this cell");
			return;
		}
		Set nets = wnd.getHighlighter().getHighlightedNetworks();
		ww.showSignals(nets, wwLoc.getContext(), false);
	}

    /**
     * This method implements the command to insert a jog in an arc
     */
    public static void insertJogInArcCommand()
    {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        ArcInst ai = (ArcInst)wnd.getHighlighter().getOneElectricObject(ArcInst.class);
        if (ai == null) return;

        System.out.println("Select the position in the arc to place the jog");
        EventListener currentListener = WindowFrame.getListener();
        WindowFrame.setListener(new InsertJogInArcListener(wnd, ai, currentListener));
    }

    /**
     * Class to handle the interactive selection of a jog point in an arc.
     */
    private static class InsertJogInArcListener
        implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
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
            InsertJogPoint job = new InsertJogPoint(ai, insert, wnd.getHighlighter());
            WindowFrame.setListener(currentListener);
        }

        public void mouseMoved(MouseEvent evt)
        {
            Point2D insert = getInsertPoint(evt);
            double x = insert.getX();
            double y = insert.getY();

            double width = (ai.getWidth() - ai.getProto().getWidthOffset()) / 2;
            Highlighter highlighter = wnd.getHighlighter();
            highlighter.clear();
            highlighter.addLine(new Point2D.Double(x-width, y-width), new Point2D.Double(x-width, y+width), ai.getParent());
            highlighter.addLine(new Point2D.Double(x-width, y+width), new Point2D.Double(x+width, y+width), ai.getParent());
            highlighter.addLine(new Point2D.Double(x+width, y+width), new Point2D.Double(x+width, y-width), ai.getParent());
            highlighter.addLine(new Point2D.Double(x+width, y-width), new Point2D.Double(x-width, y-width), ai.getParent());
            highlighter.finished();
            wnd.repaint();
        }

        private Point2D getInsertPoint(MouseEvent evt)
        {
            Point2D mouseDB = wnd.screenToDatabase((int)evt.getX(), (int)evt.getY());
			EditWindow.gridAlign(mouseDB);
            Point2D insert = DBMath.closestPointToSegment(ai.getHead().getLocation(), ai.getTail().getLocation(), mouseDB);
            return insert;
        }

        public void mouseWheelMoved(MouseWheelEvent e) {}

        public void keyPressed(KeyEvent e) {}

        public void keyReleased(KeyEvent e) {}

        public void keyTyped(KeyEvent e) {}

        private static class InsertJogPoint extends Job
        {
            private ArcInst ai;
            private Point2D insert;
            private Highlighter highlighter;

            protected InsertJogPoint(ArcInst ai, Point2D insert, Highlighter highlighter)
            {
                super("Insert Jog in Arc", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
                this.ai = ai;
                this.insert = insert;
                this.highlighter = highlighter;
                startJob();
            }

            public boolean doIt()
            {
                if (CircuitChanges.cantEdit(ai.getParent(), null, true) != 0) return false;

                // create the break pins
                ArcProto ap = ai.getProto();
                NodeProto np = ((PrimitiveArc)ap).findPinProto();
                if (np == null) return false;
                NodeInst ni = NodeInst.makeInstance(np, insert, np.getDefWidth(), np.getDefHeight(), ai.getParent());
                if (ni == null)
                {
                    System.out.println("Cannot create pin " + np.describe());
                    return false;
                }
                NodeInst ni2 = NodeInst.makeInstance(np, insert, np.getDefWidth(), np.getDefHeight(), ai.getParent());
                if (ni2 == null)
                {
                    System.out.println("Cannot create pin " + np.describe());
                    return false;
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
                boolean headNegated = ai.isHeadNegated();
                boolean tailNegated = ai.isTailNegated();
                String arcName = ai.getName();
                int angle = (ai.getAngle() + 900) % 3600;

                // create the new arcs
                ArcInst newAi1 = ArcInst.makeInstance(ap, width, headPort, pi, headPt, insert, null);
                if (headNegated) newAi1.setHeadNegated(true);
                ArcInst newAi2 = ArcInst.makeInstance(ap, width, pi, pi2, insert, insert, null);
                ArcInst newAi3 = ArcInst.makeInstance(ap, width, pi2, tailPort, insert, tailPt, null);
                if (tailNegated) newAi3.setTailNegated(true);
				newAi1.copyTextDescriptorFrom(ai, ArcInst.ARC_NAME_TD);
				newAi3.copyTextDescriptorFrom(ai, ArcInst.ARC_NAME_TD);
				newAi1.setHeadNegated(ai.isHeadNegated());
				newAi3.setTailNegated(ai.isTailNegated());
				ai.kill();
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
                highlighter.clear();
                highlighter.addElectricObject(ni, ai.getParent());
                highlighter.finished();
                return true;
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
            sb.append(" " + ap.getName());
        }
        System.out.println(sb.toString());

        int pinCount = 0, totalCount = 0, pureCount = 0, contactCount = 0;
        for(Iterator it = tech.getNodes(); it.hasNext(); )
        {
            PrimitiveNode np = (PrimitiveNode)it.next();
            if (np.isNotUsed()) continue;
            PrimitiveNode.Function fun = np.getFunction();
            totalCount++;
            if (fun == PrimitiveNode.Function.PIN) pinCount++; else
            if (fun == PrimitiveNode.Function.CONTACT || fun == PrimitiveNode.Function.CONNECT) contactCount++; else
            if (fun == PrimitiveNode.Function.NODE) pureCount++;
        }
        if (pinCount > 0)
        {
            sb = new StringBuffer();
            sb.append("    Has " + pinCount + " pin nodes for making bends in arcs:");
            for(Iterator it = tech.getNodes(); it.hasNext(); )
            {
                PrimitiveNode np = (PrimitiveNode)it.next();
                if (np.isNotUsed()) continue;
                PrimitiveNode.Function fun = np.getFunction();
                if (fun == PrimitiveNode.Function.PIN) sb.append(" " + np.getName());
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
                PrimitiveNode.Function fun = np.getFunction();
                if (fun == PrimitiveNode.Function.CONTACT || fun == PrimitiveNode.Function.CONNECT)
                    sb.append(" " + np.getName());
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
                PrimitiveNode.Function fun = np.getFunction();
                if (fun != PrimitiveNode.Function.PIN && fun != PrimitiveNode.Function.CONTACT &&
                    fun != PrimitiveNode.Function.CONNECT && fun != PrimitiveNode.Function.NODE)
                        sb.append(" " + np.getName());
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
                PrimitiveNode.Function fun = np.getFunction();
                if (fun == PrimitiveNode.Function.NODE) sb.append(" " + np.getName());
            }
            System.out.println(sb.toString());
        }
    }

}
