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

import com.sun.electric.tool.user.ui.*;
import com.sun.electric.tool.user.*;
import com.sun.electric.tool.user.dialogs.*;
import com.sun.electric.tool.Job;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Class to handle the commands in the "Edit" pulldown menu.
 */
public class EditMenu {

    protected static void addEditMenu(MenuBar menuBar) {
        MenuBar.MenuItem m;
		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		/****************************** THE EDIT MENU ******************************/

		MenuBar.Menu editMenu = new MenuBar.Menu("Edit", 'E');
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

		MenuBar.MenuItem undo = editMenu.addMenuItem("Undo", KeyStroke.getKeyStroke('Z', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { undoCommand(); } });
        Undo.addPropertyChangeListener(new MenuCommands.MenuEnabler(undo, Undo.propUndoEnabled));
        undo.setEnabled(Undo.getUndoEnabled());
        // TODO: figure out how to remove this property change listener for correct garbage collection
		MenuBar.MenuItem redo = editMenu.addMenuItem("Redo", KeyStroke.getKeyStroke('Y', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { redoCommand(); } });
        Undo.addPropertyChangeListener(new MenuCommands.MenuEnabler(redo, Undo.propRedoEnabled));
        redo.setEnabled(Undo.getRedoEnabled());
        // TODO: figure out how to remove this property change listener for correct garbage collection

		editMenu.addSeparator();

		MenuBar.Menu rotateSubMenu = new MenuBar.Menu("Rotate", 'R');
		editMenu.add(rotateSubMenu);
		rotateSubMenu.addMenuItem("90 Degrees Clockwise", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.rotateObjects(2700); }});
		rotateSubMenu.addMenuItem("90 Degrees Counterclockwise", KeyStroke.getKeyStroke('J', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.rotateObjects(900); }});
		rotateSubMenu.addMenuItem("180 Degrees", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.rotateObjects(1800); }});
		rotateSubMenu.addMenuItem("Other...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.rotateObjects(0); }});

		MenuBar.Menu mirrorSubMenu = new MenuBar.Menu("Mirror", 'M');
		editMenu.add(mirrorSubMenu);
		mirrorSubMenu.addMenuItem("Horizontally (flip over X-axis)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.mirrorObjects(true); }});
		mirrorSubMenu.addMenuItem("Vertically (flip over Y-axis)", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.mirrorObjects(false); }});

		MenuBar.Menu sizeSubMenu = new MenuBar.Menu("Size", 'S');
		editMenu.add(sizeSubMenu);
		sizeSubMenu.addMenuItem("Interactively", KeyStroke.getKeyStroke('B', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { SizeListener.sizeObjects(); } });
		sizeSubMenu.addMenuItem("All Selected Nodes...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { SizeListener.sizeAllNodes(); }});
		sizeSubMenu.addMenuItem("All Selected Arcs...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { SizeListener.sizeAllArcs(); }});


		MenuBar.Menu moveSubMenu = new MenuBar.Menu("Move", 'V');
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

		editMenu.addSeparator();

		m=editMenu.addMenuItem("Erase", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.deleteSelected(); } });
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), null);
		editMenu.addMenuItem("Array...", KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Array.showArrayDialog(); } });
		editMenu.addMenuItem("Change...", KeyStroke.getKeyStroke('C', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Change.showChangeDialog(); } });

/*
		editMenu.addSeparator();

		editMenu.addMenuItem("Key Bindings...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { keyBindingsCommand(); } });
*/
		editMenu.addSeparator();

		MenuBar.Menu editInfoSubMenu = new MenuBar.Menu("Info", 'V');
		editMenu.add(editInfoSubMenu);
		editInfoSubMenu.addMenuItem("List Layer Coverage", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolMenu.layerCoverageCommand(Job.Type.EXAMINE, ToolMenu.LayerCoverageJob.AREA, false); } });
		editInfoSubMenu.addMenuItem("Show Undo List", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { showUndoListCommand(); } });
		editInfoSubMenu.addMenuItem("Describe this Technology", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { describeTechnologyCommand(); } });

		MenuBar.Menu editPropertiesSubMenu = new MenuBar.Menu("Properties", 'V');
		editMenu.add(editPropertiesSubMenu);
		editPropertiesSubMenu.addMenuItem("Object Properties...", KeyStroke.getKeyStroke('I', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { getInfoCommand(); } });
		editPropertiesSubMenu.addMenuItem("Attribute Properties...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { Attributes.showDialog(); } });
		editPropertiesSubMenu.addSeparator();
		editPropertiesSubMenu.addMenuItem("See All Attributes on Node", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { seeAllParametersCommand(); } });
		editPropertiesSubMenu.addMenuItem("Hide All Attributes on Node", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { hideAllParametersCommand(); } });
		editPropertiesSubMenu.addMenuItem("Default Attribute Visibility", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { defaultParamVisibilityCommand(); } });
        editPropertiesSubMenu.addMenuItem("Update Attributes Inheritance on Node", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { updateInheritance(false); } });
        editPropertiesSubMenu.addMenuItem("Update Attributes Inheritance all Libraries", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { updateInheritance(true); } });


		MenuBar.Menu arcSubMenu = new MenuBar.Menu("Arc", 'A');
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
		arcSubMenu.addMenuItem("Toggle End Extension", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcEndsExtendCommand(); }});
		arcSubMenu.addMenuItem("Reverse", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcReverseCommand(); }});
		arcSubMenu.addMenuItem("Toggle Head-Skip", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcSkipHeadCommand(); }});
		arcSubMenu.addMenuItem("Toggle Tail-Skip", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.arcSkipTailCommand(); }});
		arcSubMenu.addSeparator();
		arcSubMenu.addMenuItem("Insert Jog In Arc", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { insertJogInArcCommand(); } });
		arcSubMenu.addMenuItem("Rip Bus", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.ripBus(); }});

		MenuBar.Menu modeSubMenu = new MenuBar.Menu("Modes");
		editMenu.add(modeSubMenu);

		MenuBar.Menu modeSubMenuEdit = new MenuBar.Menu("Edit");
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

		MenuBar.Menu modeSubMenuMovement = new MenuBar.Menu("Movement");
		modeSubMenu.add(modeSubMenuMovement);
		ButtonGroup movementGroup = new ButtonGroup();
        JMenuItem moveFull, moveHalf, moveQuarter;
		moveFull = modeSubMenuMovement.addRadioButton(ToolBar.moveFullName, true, movementGroup, KeyStroke.getKeyStroke('F', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.fullArrowDistanceCommand(); } });
		moveHalf = modeSubMenuMovement.addRadioButton(ToolBar.moveHalfName, false, movementGroup, KeyStroke.getKeyStroke('H', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.halfArrowDistanceCommand(); } });
		moveQuarter = modeSubMenuMovement.addRadioButton(ToolBar.moveQuarterName, false, movementGroup, KeyStroke.getKeyStroke('Q', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolBar.quarterArrowDistanceCommand(); } });
		double ad = ToolBar.getArrowDistance();
		if (ad == 1.0) moveFull.setSelected(true); else
		if (ad == 0.5) moveHalf.setSelected(true); else
			moveQuarter.setSelected(true);

		MenuBar.Menu modeSubMenuSelect = new MenuBar.Menu("Select");
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

		MenuBar.Menu textSubMenu = new MenuBar.Menu("Text");
		editMenu.add(textSubMenu);
		textSubMenu.addMenuItem("Find Text...", KeyStroke.getKeyStroke('L', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { FindText.findTextDialog(); }});
		textSubMenu.addMenuItem("Change Text Size...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ChangeText.changeTextDialog(); }});

		MenuBar.Menu cleanupSubMenu = new MenuBar.Menu("Cleanup Cell");
		editMenu.add(cleanupSubMenu);
		cleanupSubMenu.addMenuItem("Cleanup Pins", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.cleanupPinsCommand(false); }});
		cleanupSubMenu.addMenuItem("Cleanup Pins Everywhere", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.cleanupPinsCommand(true); }});
		cleanupSubMenu.addMenuItem("Show Nonmanhattan", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.showNonmanhattanCommand(); }});
		cleanupSubMenu.addMenuItem("Shorten Selected Arcs", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.shortenArcsCommand(); }});

		MenuBar.Menu specialSubMenu = new MenuBar.Menu("Technology Specific");
		editMenu.add(specialSubMenu);
		specialSubMenu.addMenuItem("Toggle Port Negation", KeyStroke.getKeyStroke('T', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.toggleNegatedCommand(); }});
		specialSubMenu.addMenuItem("Artwork Appearance...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ArtworkLook.showArtworkLookDialog(); }});

		MenuBar.Menu selListSubMenu = new MenuBar.Menu("Selection");
		editMenu.add(selListSubMenu);
		selListSubMenu.addMenuItem("Select All", KeyStroke.getKeyStroke('A', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectAllCommand(); }});
		selListSubMenu.addMenuItem("Select All Like This", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectAllLikeThisCommand(); }});
		selListSubMenu.addMenuItem("Select All Easy", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectEasyCommand(); }});
		selListSubMenu.addMenuItem("Select All Hard", null,
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
		selListSubMenu.addMenuItem("Push Selection", KeyStroke.getKeyStroke('1', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Highlight.pushHighlight(); }});
		selListSubMenu.addMenuItem("Pop Selection", KeyStroke.getKeyStroke('3', buckyBit),
			new ActionListener() { public void actionPerformed(ActionEvent e) { Highlight.popHighlight(); }});
		selListSubMenu.addSeparator();
		selListSubMenu.addMenuItem("Enclosed Objects", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectEnclosedObjectsCommand(); }});
		selListSubMenu.addSeparator();
		selListSubMenu.addMenuItem("Show Next Error", KeyStroke.getKeyStroke('>'),
			new ActionListener() { public void actionPerformed(ActionEvent e) { showNextErrorCommand(); }});
		selListSubMenu.addMenuItem("Show Previous Error", KeyStroke.getKeyStroke('<'),
			new ActionListener() { public void actionPerformed(ActionEvent e) { showPrevErrorCommand(); }});
		selListSubMenu.addSeparator();
		selListSubMenu.addMenuItem("Add to Waveform in New Panel", KeyStroke.getKeyStroke('A', 0),
			new ActionListener() { public void actionPerformed(ActionEvent e) { addToWaveformNewCommand(); }});
		selListSubMenu.addMenuItem("Add to Waveform in Current Panel", KeyStroke.getKeyStroke('O', 0),
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
		  //Highlight.clear();
		  //Highlight.finished();
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

		public boolean doIt()
		{
			// change visibility of parameters on the current node(s)
			int changeCount = 0;
			java.util.List list = Highlight.getHighlighted(true, false);
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
        List highlighted = Highlight.getHighlighted(true, false);
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
		java.util.List highlighted = Highlight.getHighlighted(true, true);
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
		java.util.List newHighList = new ArrayList();
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
		java.util.List highlighted = Highlight.getHighlighted(true, true);
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
		java.util.List highlighted = Highlight.getHighlighted(true, true);
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
		String msg = ErrorLogger.reportNextError();
		System.out.println(msg);
	}

	/**
	 * This method implements the command to show the last logged error.
	 * The error log lists the results of the latest command (DRC, NCC, etc.)
	 */
	public static void showPrevErrorCommand()
	{
		String msg = ErrorLogger.reportPrevError();
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
		WaveformWindow ww = WaveformWindow.findWaveformWindow(wf.getContent().getCell());
		if (ww == null)
		{
			System.out.println("Cannot add selected signals to the waveform window: this cell has no waveform window");
			return;
		}
		Set nets = Highlight.getHighlightedNetworks();
		ww.showSignals(nets, true);
	}

	/**
	 * This method implements the command to add the currently selected network
	 * to the waveform window, overlaid on top of the current panel.
	 */
	public static void addToWaveformCurrentCommand()
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (!(wf.getContent() instanceof EditWindow)) return;
		WaveformWindow ww = WaveformWindow.findWaveformWindow(wf.getContent().getCell());
		if (ww == null)
		{
			System.out.println("Cannot overlay selected signals to the waveform window: this cell has no waveform window");
			return;
		}
		Set nets = Highlight.getHighlightedNetworks();
		ww.showSignals(nets, false);
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
			EditWindow.gridAlign(mouseDB);
            Point2D insert = DBMath.closestPointToSegment(ai.getHead().getLocation(), ai.getTail().getLocation(), mouseDB);
            return insert;
        }

        public void mouseWheelMoved(MouseWheelEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void keyPressed(KeyEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void keyReleased(KeyEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void keyTyped(KeyEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
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

            public boolean doIt()
            {
                // create the break pins
                ArcProto ap = ai.getProto();
                NodeProto np = ((PrimitiveArc)ap).findPinProto();
                if (np == null) return false;
                NodeInst ni = NodeInst.makeInstance(np, insert, np.getDefWidth(), np.getDefHeight(),
                    0, ai.getParent(), null);
                if (ni == null)
                {
                    System.out.println("Cannot create pin " + np.describe());
                    return false;
                }
                NodeInst ni2 = NodeInst.makeInstance(np, insert, np.getDefWidth(), np.getDefHeight(),
                    0, ai.getParent(), null);
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

//	public static void showRTreeCommand()
//	{
//		Library curLib = Library.getCurrent();
//		Cell curCell = curLib.getCurCell();
//		System.out.println("Current cell is " + curCell.describe());
//		if (curCell == null) return;
//		curCell.getRTree().printRTree(0);
//	}

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
                if (fun == NodeProto.Function.PIN) sb.append(" " + np.getName());
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
                NodeProto.Function fun = np.getFunction();
                if (fun != NodeProto.Function.PIN && fun != NodeProto.Function.CONTACT &&
                    fun != NodeProto.Function.CONNECT && fun != NodeProto.Function.NODE)
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
                NodeProto.Function fun = np.getFunction();
                if (fun == NodeProto.Function.NODE) sb.append(" " + np.getName());
            }
            System.out.println(sb.toString());
        }
    }

}
