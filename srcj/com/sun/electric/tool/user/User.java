/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: User.java
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
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.dialogs.GetInfoNode;
import com.sun.electric.tool.user.dialogs.GetInfoArc;
import com.sun.electric.tool.user.dialogs.GetInfoExport;
import com.sun.electric.tool.user.dialogs.GetInfoText;
import com.sun.electric.tool.user.dialogs.GetInfoMulti;
import com.sun.electric.tool.user.dialogs.Attributes;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.PaletteFrame;
import com.sun.electric.tool.user.ui.StatusBar;

import java.util.Iterator;

/**
 * This is the User Interface tool.
 */
public class User extends Listener
{
	// ---------------------- private and protected methods -----------------

	/** the User Interface tool. */		public static User tool = new User();
	/** key of Variable holding rotation overrides for primitives. */	public static final Variable.Key PLACEMENT_ANGLE = ElectricObject.newKey("USER_placement_angle");

	private ArcProto currentArcProto = null;
	private NodeProto currentNodeProto = null;

	/**
	 * The constructor sets up the User tool.
	 */
	private User()
	{
		super("user");
	}

	/**
	 * Method to initialize the User Interface tool.
	 */
	public void init()
	{
		// the user interface tool is always on
		setOn();
		setIncremental();

		// initialize the display
		TopLevel.Initialize();
		TopLevel.getPaletteFrame().loadForTechnology();
	}

	/**
	 * Method to return the "current" NodeProto, as maintained by the user interface.
	 * @return the "current" NodeProto, as maintained by the user interface.
	 */
	public NodeProto getCurrentNodeProto() { return currentNodeProto; }

	/**
	 * Method to set the "current" NodeProto, as maintained by the user interface.
	 * @param np the new "current" NodeProto.
	 */
	public void setCurrentNodeProto(NodeProto np) { currentNodeProto = np; }

	/**
	 * Method to return the "current" ArcProto, as maintained by the user interface.
	 * The current ArcProto is highlighted with a bolder red border in the component menu on the left.
	 * @return the "current" ArcProto, as maintained by the user interface.
	 */
	public ArcProto getCurrentArcProto() { return currentArcProto; }

	/**
	 * Method to set the "current" ArcProto, as maintained by the user interface.
	 * The current ArcProto is highlighted with a bolder red border in the component menu on the left.
	 * @param np the new "current" ArcProto.
	 */
	public void setCurrentArcProto(ArcProto ap) { currentArcProto = ap;   TopLevel.getPaletteFrame().arcProtoChanged(); }

	/**
	 * Daemon Method called when an object is to be redrawn.
	 */
	public void redrawObject(ElectricObject obj)
	{
		if (obj instanceof Geometric)
		{
			Geometric geom = (Geometric)obj;
			Cell parent = geom.getParent();
			markCellForRedraw(parent, true);
		}
		if (obj instanceof PortInst)
		{
			PortInst pi = (PortInst)obj;
			Cell parent = pi.getNodeInst().getParent();
			markCellForRedraw(parent, true);
		}
	}

	/**
	 * Method to recurse flag all windows showing a cell to redraw.
	 * @param cell the Cell that changed.
	 * @param recurseUp true to recurse up the hierarchy, redrawing cells that show this one.
	 */
	private void markCellForRedraw(Cell cell, boolean recurseUp)
	{
		for(Iterator wit = WindowFrame.getWindows(); wit.hasNext(); )
		{
			WindowFrame window = (WindowFrame)wit.next();
			EditWindow win = window.getEditWindow();
			Cell winCell = win.getCell();
			if (winCell == cell) win.redraw();
		}

		if (recurseUp)
		{
			for(Iterator it = cell.getInstancesOf(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (ni.isExpanded()) markCellForRedraw(ni.getParent(), recurseUp);
			}
		}
	}

	/**
	 * Daemon Method called when a batch of changes ends.
	 */
	public void endBatch()
	{
		// redraw all windows with Cells that changed
		for(Iterator it = Undo.ChangeCell.getIterator(); it.hasNext(); )
		{
			Undo.ChangeCell cc = (Undo.ChangeCell)it.next();
			Cell cell = cc.getCell();
			markCellForRedraw(cell, false);
		}

		// update live dialogs and status bar
		updateInformationAreas();
	}

	public void updateInformationAreas()
	{
		GetInfoNode.load();
		GetInfoArc.load();
		GetInfoExport.load();
		GetInfoText.load();
		GetInfoMulti.load();
		Attributes.load();
		StatusBar.updateStatusBar();
	}

	/****************************** ICON GENERATION PREFERENCES ******************************/

	private static Tool.Pref cacheIconGenDrawLeads = User.tool.makeBooleanPref("IconGenDrawLeads", true);
	/**
	 * Method to tell whether generated icons should have leads drawn.
	 * The default is "true".
	 * @return true if generated icons should have leads drawn.
	 */
	public static boolean isIconGenDrawLeads() { return cacheIconGenDrawLeads.getBoolean(); }
	/**
	 * Method to set whether generated icons should have leads drawn.
	 * @param on true if generated icons should have leads drawn.
	 */
	public static void setIconGenDrawLeads(boolean on) { cacheIconGenDrawLeads.setBoolean(on); }

	private static Tool.Pref cacheIconGenDrawBody = User.tool.makeBooleanPref("IconGenDrawBody", true);
	/**
	 * Method to tell whether generated icons should have a body drawn.
	 * The body is just a rectangle.
	 * The default is "true".
	 * @return true if generated icons should have a body drawn.
	 */
	public static boolean isIconGenDrawBody() { return cacheIconGenDrawBody.getBoolean(); }
	/**
	 * Method to set whether generated icons should have a body drawn.
	 * The body is just a rectangle.
	 * @param on true if generated icons should have a body drawn.
	 */
	public static void setIconGenDrawBody(boolean on) { cacheIconGenDrawBody.setBoolean(on); }

	private static Tool.Pref cacheIconGenReverseExportOrder = User.tool.makeBooleanPref("IconGenReverseExportOrder", false);
	/**
	 * Method to tell whether generated icons should reverse the order of exports.
	 * Normally, exports are drawn top-to-bottom alphabetically.
	 * The default is "false".
	 * @return true if generated icons should reverse the order of exports.
	 */
	public static boolean isIconGenReverseExportOrder() { return cacheIconGenReverseExportOrder.getBoolean(); }
	/**
	 * Method to set whether generated icons should reverse the order of exports.
	 * Normally, exports are drawn top-to-bottom alphabetically.
	 * @param on true if generated icons should reverse the order of exports.
	 */
	public static void setIconGenReverseExportOrder(boolean on) { cacheIconGenReverseExportOrder.setBoolean(on); }

	private static Tool.Pref cacheIconGenInputSide = User.tool.makeIntPref("IconGenInputSide", 0);
	/**
	 * Method to tell where Input ports should go on generated icons.
	 * @return information about where Input ports should go on generated icons.
	 * 0: left (the default)   1: right   2: top   3: bottom
	 */
	public static int getIconGenInputSide() { return cacheIconGenInputSide.getInt(); }
	/**
	 * Method to set where Input ports should go on generated icons.
	 * @param side information about where Input ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static void setIconGenInputSide(int side) { cacheIconGenInputSide.setInt(side); }

	private static Tool.Pref cacheIconGenOutputSide = User.tool.makeIntPref("IconGenOutputSide", 1);
	/**
	 * Method to tell where Output ports should go on generated icons.
	 * @return information about where Output ports should go on generated icons.
	 * 0: left   1: right (the default)   2: top   3: bottom
	 */
	public static int getIconGenOutputSide() { return cacheIconGenOutputSide.getInt(); }
	/**
	 * Method to set where Output ports should go on generated icons.
	 * @param side information about where Output ports should go on generated icons.
	 * 0: left   1: right (the default)   2: top   3: bottom
	 */
	public static void setIconGenOutputSide(int side) { cacheIconGenOutputSide.setInt(side); }

	private static Tool.Pref cacheIconGenBidirSide = User.tool.makeIntPref("IconGenBidirSide", 2);
	/**
	 * Method to tell where Bidirectional ports should go on generated icons.
	 * @return information about where Bidirectional ports should go on generated icons.
	 * 0: left   1: right   2: top (the default)   3: bottom
	 */
	public static int getIconGenBidirSide() { return cacheIconGenBidirSide.getInt(); }
	/**
	 * Method to set where Bidirectional ports should go on generated icons.
	 * @param side information about where Bidirectional ports should go on generated icons.
	 * 0: left   1: right   2: top (the default)   3: bottom
	 */
	public static void setIconGenBidirSide(int side) { cacheIconGenBidirSide.setInt(side); }

	private static Tool.Pref cacheIconGenPowerSide = User.tool.makeIntPref("IconGenPowerSide", 3);
	/**
	 * Method to tell where Power ports should go on generated icons.
	 * @return information about where Power ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom (the default)
	 */
	public static int getIconGenPowerSide() { return cacheIconGenPowerSide.getInt(); }
	/**
	 * Method to set where Power ports should go on generated icons.
	 * @param side information about where Power ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom (the default)
	 */
	public static void setIconGenPowerSide(int side) { cacheIconGenPowerSide.setInt(side); }

	private static Tool.Pref cacheIconGenGroundSide = User.tool.makeIntPref("IconGenGroundSide", 3);
	/**
	 * Method to tell where Ground ports should go on generated icons.
	 * @return information about where Ground ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom (the default)
	 */
	public static int getIconGenGroundSide() { return cacheIconGenGroundSide.getInt(); }
	/**
	 * Method to set where Ground ports should go on generated icons.
	 * @param side information about where Ground ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom (the default)
	 */
	public static void setIconGenGroundSide(int side) { cacheIconGenGroundSide.setInt(side); }

	private static Tool.Pref cacheIconGenClockSide = User.tool.makeIntPref("IconGenClockSide", 0);
	/**
	 * Method to tell where Clock ports should go on generated icons.
	 * @return information about where Clock ports should go on generated icons.
	 * 0: left (the default)   1: right   2: top   3: bottom
	 */
	public static int getIconGenClockSide() { return cacheIconGenClockSide.getInt(); }
	/**
	 * Method to set where Clock ports should go on generated icons.
	 * @param side information about where Clock ports should go on generated icons.
	 * 0: left (the default)   1: right   2: top   3: bottom
	 */
	public static void setIconGenClockSide(int side) { cacheIconGenClockSide.setInt(side); }

	private static Tool.Pref cacheIconGenExportLocation = User.tool.makeIntPref("IconGenExportLocation", 1);
	/**
	 * Method to tell where exports should appear along the leads in generated icons.
	 * @return information about where exports should appear along the leads in generated icons.
	 * 0: on the body   1: (the default) at the end of the lead   2: in the middle of the lead
	 */
	public static int getIconGenExportLocation() { return cacheIconGenExportLocation.getInt(); }
	/**
	 * Method to set how exports should appear along the leads in generated icons.
	 * @param loc information about where exports should appear along the leads in generated icons.
	 * 0: on the body   1: at the end of the lead (the default)   2: in the middle of the lead
	 */
	public static void setIconGenExportLocation(int loc) { cacheIconGenExportLocation.setInt(loc); }

	private static Tool.Pref cacheIconGenExportStyle = User.tool.makeIntPref("IconGenExportStyle", 0);
	/**
	 * Method to tell how the text should appear in generated icons.
	 * @return information about how the text should appear in generated icons.
	 * 0: (the default) centered at the export location
	 * 1: pointing inward from the export location
	 * 2: pointing outward from the export location
	 */
	public static int getIconGenExportStyle() { return cacheIconGenExportStyle.getInt(); }
	/**
	 * Method to set how the text should appear in generated icons.
	 * @param style information about how the text should appear in generated icons.
	 * 0: centered at the export location
	 * 1: pointing inward from the export location
	 * 2: pointing outward from the export location
	 */
	public static void setIconGenExportStyle(int style) { cacheIconGenExportStyle.setInt(style); }

	private static Tool.Pref cacheIconGenExportTech = User.tool.makeIntPref("IconGenExportTech", 0);
	/**
	 * Method to tell how exports should be constructed in generated icons.
	 * @return information about how exports should be constructed in generated icons.
	 * 0: (the default) use Generic:Universal-Pins for exports (can connect to ANYTHING).
	 * 1: use Schematic:Bus-Pins for exports (can connect to schematic busses or wires).
	 */
	public static int getIconGenExportTech() { return cacheIconGenExportTech.getInt(); }
	/**
	 * Method to set how exports should be constructed in generated icons.
	 * @param t information about how exports should be constructed in generated icons.
	 * 0: use Generic:Universal-Pins for exports (can connect to ANYTHING).
	 * 1: use Schematic:Bus-Pins for exports (can connect to schematic busses or wires).
	 */
	public static void setIconGenExportTech(int t) { cacheIconGenExportTech.setInt(t); }

	private static Tool.Pref cacheIconGenInstanceLocation = User.tool.makeIntPref("IconGenInstanceLocation", 0);
	/**
	 * Method to tell where to place an instance of the generated icons in the original schematic.
	 * @return information about where to place an instance of the generated icons in the original schematic
	 * 0: (the default) in the upper-right corner.
	 * 1: in the upper-left corner.
	 * 2: in the lower-right corner.
	 * 3: in the lower-left corner.
	 */
	public static int getIconGenInstanceLocation() { return cacheIconGenInstanceLocation.getInt(); }
	/**
	 * Method to set where to place an instance of the generated icons in the original schematic.
	 * @param loc information about where to place an instance of the generated icons in the original schematic.
	 * 0: in the upper-right corner.
	 * 1: in the upper-left corner.
	 * 2: in the lower-right corner.
	 * 3: in the lower-left corner.
	 */
	public static void setIconGenInstanceLocation(int loc) { cacheIconGenInstanceLocation.setInt(loc); }

	private static Tool.Pref cacheIconGenLeadLength = User.tool.makeDoublePref("IconGenLeadLength", 2.0f);
	/**
	 * Method to tell how long to make leads in generated icons.
	 * @return information about how long to make leads in generated icons (the default is 2).
	 */
	public static double getIconGenLeadLength() { return cacheIconGenLeadLength.getDouble(); }
	/**
	 * Method to set how long to make leads in generated icons.
	 * @param len how long to make leads in generated icons.
	 */
	public static void setIconGenLeadLength(double len) { cacheIconGenLeadLength.setDouble(len); }

	private static Tool.Pref cacheIconGenLeadSpacing = User.tool.makeDoublePref("IconGenLeadSpacing", 2.0f);
	/**
	 * Method to tell how far apart to space leads in generated icons.
	 * @return information about how far apart to space leads in generated icons (the default is 2).
	 */
	public static double getIconGenLeadSpacing() { return cacheIconGenLeadSpacing.getDouble(); }
	/**
	 * Method to set how far apart to space leads in generated icons.
	 * @param dist how far apart to space leads in generated icons.
	 */
	public static void setIconGenLeadSpacing(double dist) { cacheIconGenLeadSpacing.setDouble(dist); }

	/****************************** PORT AND EXPORT PREFERENCES ******************************/

	private static Tool.Pref cachePortDisplayLevel = User.tool.makeIntPref("PortDisplayLevel", 0);
	/**
	 * Method to tell how to display ports.
	 * @return how to display ports.
	 * 0: full port names (the default).
	 * 1: short port names (stopping at the first nonalphabetic character).
	 * 2: ports drawn as crosses.
	 */
	public static int getPortDisplayLevel() { return cachePortDisplayLevel.getInt(); }
	/**
	 * Method to set how to display ports.
	 * @param level how to display ports.
	 * 0: full port names (the default).
	 * 1: short port names (stopping at the first nonalphabetic character).
	 * 2: ports drawn as crosses.
	 */
	public static void setPortDisplayLevels(int level) { cachePortDisplayLevel.setInt(level); }

	private static Tool.Pref cacheExportDisplayLevel = User.tool.makeIntPref("ExportDisplayLevel", 0);
	/**
	 * Method to tell how to display exports.
	 * @return how to display exports.
	 * 0: full export names (the default).
	 * 1: short export names (stopping at the first nonalphabetic character).
	 * 2: exports drawn as crosses.
	 */
	public static int getExportDisplayLevel() { return cacheExportDisplayLevel.getInt(); }
	/**
	 * Method to set how to display exports.
	 * @param level how to display exports.
	 * 0: full export names (the default).
	 * 1: short export names (stopping at the first nonalphabetic character).
	 * 2: exports drawn as crosses.
	 */
	public static void setExportDisplayLevels(int level) { cacheExportDisplayLevel.setInt(level); }

	private static Tool.Pref cacheMoveNodeWithExport = User.tool.makeBooleanPref("MoveNodeWithExport", false);
	/**
	 * Method to tell whether to move a node when its export name moves.
	 * The default is "false", which means that the export text can move independently.
	 * @return true to move a node when its export name moves.
	 */
	public static boolean isMoveNodeWithExport() { return cacheMoveNodeWithExport.getBoolean(); }
	/**
	 * Method to set whether to move a node when its export name moves.
	 * @param on true to move a node when its export name moves.
	 */
	public static void setMoveNodeWithExport(boolean on) { cacheMoveNodeWithExport.setBoolean(on); }

	/****************************** SELECTION PREFERENCES ******************************/

	private static Tool.Pref cacheEasySelectionOfCellInstances = User.tool.makeBooleanPref("EasySelectionOfCellInstances", true);
	/**
	 * Method to tell whether cell instances are all be easy-to-select.
	 * The default is "true".
	 * @return true if cell instances are all be easy-to-select.
	 */
	public static boolean isEasySelectionOfCellInstances() { return cacheEasySelectionOfCellInstances.getBoolean(); }
	/**
	 * Method to set whether cell instances are all be easy-to-select.
	 * @param on true if cell instances are all to be easy-to-select.
	 */
	public static void setEasySelectionOfCellInstances(boolean on) { cacheEasySelectionOfCellInstances.setBoolean(on); }

	private static Tool.Pref cacheEasySelectionOfAnnotationText = User.tool.makeBooleanPref("EasySelectionOfAnnotationText", true);
	/**
	 * Method to tell whether annotation text is easy-to-select.
	 * The default is "true".
	 * @return true if annotation text is easy-to-select.
	 */
	public static boolean isEasySelectionOfAnnotationText() { return cacheEasySelectionOfAnnotationText.getBoolean(); }
	/**
	 * Method to set whether annotation text is easy-to-select.
	 * @param on true if annotation text is easy-to-select.
	 */
	public static void setEasySelectionOfAnnotationText(boolean on) { cacheEasySelectionOfAnnotationText.setBoolean(on); }

	private static Tool.Pref cacheDraggingMustEncloseObjects = User.tool.makeBooleanPref("DraggingMustEncloseObjects", false);
	/**
	 * Method to tell whether dragging a selection rectangle must completely encose objects in order to select them.
	 * The default is "false", which means that the selection rectangle need only touch an object in order to select it.
	 * @return true if dragging a selection rectangle must completely encose objects in order to select them.
	 */
	public static boolean isDraggingMustEncloseObjects() { return cacheDraggingMustEncloseObjects.getBoolean(); }
	/**
	 * Method to set whether dragging a selection rectangle must completely encose objects in order to select them.
	 * @param on true if dragging a selection rectangle must completely encose objects in order to select them.
	 */
	public static void setDraggingMustEncloseObjects(boolean on) { cacheDraggingMustEncloseObjects.setBoolean(on); }

	/****************************** GRID AND ALIGNMENT PREFERENCES ******************************/

	private static Tool.Pref cacheDefGridXSpacing = User.tool.makeDoublePref("DefGridXSpacing", 1);
	/**
	 * Method to return the default spacing of grid dots in the X direction.
	 * The default is 1.
	 * @return true the default spacing of grid dots in the X direction.
	 */
	public static double getDefGridXSpacing() { return cacheDefGridXSpacing.getDouble(); }
	/**
	 * Method to set the default spacing of grid dots in the X direction.
	 * @param dist the default spacing of grid dots in the X direction.
	 */
	public static void setDefGridXSpacing(double dist) { cacheDefGridXSpacing.setDouble(dist); }

	private static Tool.Pref cacheDefGridYSpacing = User.tool.makeDoublePref("DefGridYSpacing", 1);
	/**
	 * Method to return the default spacing of grid dots in the Y direction.
	 * The default is 1.
	 * @return true the default spacing of grid dots in the Y direction.
	 */
	public static double getDefGridYSpacing() { return cacheDefGridYSpacing.getDouble(); }
	/**
	 * Method to set the default spacing of grid dots in the Y direction.
	 * @param dist the default spacing of grid dots in the Y direction.
	 */
	public static void setDefGridYSpacing(double dist) { cacheDefGridYSpacing.setDouble(dist); }

	private static Tool.Pref cacheDefGridXBoldFrequency = User.tool.makeIntPref("DefGridXBoldFrequency", 10);
	/**
	 * Method to return the default frequency of bold grid dots in the X direction.
	 * The default is 10.
	 * @return true the default frequency of bold grid dots in the X direction.
	 */
	public static int getDefGridXBoldFrequency() { return cacheDefGridXBoldFrequency.getInt(); }
	/**
	 * Method to set the default frequency of bold grid dots in the X direction.
	 * @param dist the default frequency of bold grid dots in the X direction.
	 */
	public static void setDefGridXBoldFrequency(int dist) { cacheDefGridXBoldFrequency.setInt(dist); }

	private static Tool.Pref cacheDefGridYBoldFrequency = User.tool.makeIntPref("DefGridYBoldFrequency", 10);
	/**
	 * Method to return the default frequency of bold grid dots in the Y direction.
	 * The default is 10.
	 * @return true the default frequency of bold grid dots in the Y direction.
	 */
	public static int getDefGridYBoldFrequency() { return cacheDefGridYBoldFrequency.getInt(); }
	/**
	 * Method to set the default frequency of bold grid dots in the Y direction.
	 * @param dist the default frequency of bold grid dots in the Y direction.
	 */
	public static void setDefGridYBoldFrequency(int dist) { cacheDefGridYBoldFrequency.setInt(dist); }

	private static Tool.Pref cacheAlignGridWithCircuitry = User.tool.makeBooleanPref("AlignGridWithCircuitry", false);
	/**
	 * Method to tell whether to align the grid dots with the circuitry.
	 * The default is "false", which implies that the grid dots are placed independently of object locations.
	 * @return true to align the grid dots with the circuitry.
	 */
	public static boolean isAlignGridWithCircuitry() { return cacheAlignGridWithCircuitry.getBoolean(); }
	/**
	 * Method to set whether to align the grid dots with the circuitry.
	 * @param on true to align the grid dots with the circuitry.
	 */
	public static void setAlignGridWithCircuitry(boolean on) { cacheAlignGridWithCircuitry.setBoolean(on); }

	private static Tool.Pref cacheShowCursorCoordinates = User.tool.makeBooleanPref("ShowCursorCoordinates", false);
	/**
	 * Method to tell whether to show the cursor coordinates as they move in the edit window.
	 * The default is "false".
	 * @return true to show the cursor coordinates as they move in the edit window
	 */
	public static boolean isShowCursorCoordinates() { return cacheShowCursorCoordinates.getBoolean(); }
	/**
	 * Method to set whether to show the cursor coordinates as they move in the edit window
	 * @param on true to show the cursor coordinates as they move in the edit window
	 */
	public static void setShowCursorCoordinates(boolean on) { cacheShowCursorCoordinates.setBoolean(on); }

	private static Tool.Pref cacheAlignmentToGrid = User.tool.makeDoublePref("AlignmentToGrid", 1);
	/**
	 * Method to return the default alignment of objects to the grid.
	 * The default is 1, meaning that placement and movement should land on whole grid units.
	 * @return true the default alignment of objects to the grid.
	 */
	public static double getAlignmentToGrid() { return cacheAlignmentToGrid.getDouble(); }
	/**
	 * Method to set the default alignment of objects to the grid.
	 * @param dist the default alignment of objects to the grid.
	 */
	public static void setAlignmentToGrid(double dist) { cacheAlignmentToGrid.setDouble(dist); }

	private static Tool.Pref cacheEdgeAlignmentToGrid = User.tool.makeDoublePref("EdgeAlignmentToGrid", 0);
	/**
	 * Method to return the default alignment of object edges to the grid.
	 * The default is 0, meaning that no alignment is to be done.
	 * @return true the default alignment of object edges to the grid.
	 */
	public static double getEdgeAlignmentToGrid() { return cacheEdgeAlignmentToGrid.getDouble(); }
	/**
	 * Method to set the default alignment of object edges to the grid.
	 * @param dist the default alignment of object edges to the grid.
	 */
	public static void setEdgeAlignmentToGrid(double dist) { cacheEdgeAlignmentToGrid.setDouble(dist); }

	/****************************** TEXT PREFERENCES ******************************/

	private static Tool.Pref cacheTextVisibilityNode = User.tool.makeBooleanPref("TextVisibilityNode", true);
	/**
	 * Method to tell whether to draw text that resides on nodes.
	 * This text includes the node name and any parameters or attributes on it.
	 * The default is "true".
	 * @return true if the system should draw text that resides on nodes.
	 */
	public static boolean isTextVisibilityOnNode() { return cacheTextVisibilityNode.getBoolean(); }
	/**
	 * Method to set whether to draw text that resides on nodes.
	 * This text includes the node name and any parameters or attributes on it.
	 * @param on true if the system should draw text that resides on nodes.
	 */
	public static void setTextVisibilityOnNode(boolean on) { cacheTextVisibilityNode.setBoolean(on); }

	private static Tool.Pref cacheTextVisibilityArc = User.tool.makeBooleanPref("TextVisibilityArc", true);
	/**
	 * Method to tell whether to draw text that resides on arcs.
	 * This text includes the arc name and any parameters or attributes on it.
	 * The default is "true".
	 * @return true if the system should draw text that resides on arcs.
	 */
	public static boolean isTextVisibilityOnArc() { return cacheTextVisibilityArc.getBoolean(); }
	/**
	 * Method to set whether to draw text that resides on arcs.
	 * This text includes the arc name and any parameters or attributes on it.
	 * @param on true if the system should draw text that resides on arcs.
	 */
	public static void setTextVisibilityOnArc(boolean on) { cacheTextVisibilityArc.setBoolean(on); }

	private static Tool.Pref cacheTextVisibilityPort = User.tool.makeBooleanPref("TextVisibilityPort", true);
	/**
	 * Method to tell whether to draw text that resides on ports.
	 * This text includes the port name and any parameters or attributes on it.
	 * The default is "true".
	 * @return true if the system should draw text that resides on ports.
	 */
	public static boolean isTextVisibilityOnPort() { return cacheTextVisibilityPort.getBoolean(); }
	/**
	 * Method to set whether to draw text that resides on ports.
	 * This text includes the port name and any parameters or attributes on it.
	 * @param on true if the system should draw text that resides on ports.
	 */
	public static void setTextVisibilityOnPort(boolean on) { cacheTextVisibilityPort.setBoolean(on); }

	private static Tool.Pref cacheTextVisibilityExport = User.tool.makeBooleanPref("TextVisibilityExport", true);
	/**
	 * Method to tell whether to draw text that resides on exports.
	 * This text includes the export name and any parameters or attributes on it.
	 * The default is "true".
	 * @return true if the system should draw text that resides on exports.
	 */
	public static boolean isTextVisibilityOnExport() { return cacheTextVisibilityExport.getBoolean(); }
	/**
	 * Method to set whether to draw text that resides on exports.
	 * This text includes the export name and any parameters or attributes on it.
	 * @param on true if the system should draw text that resides on exports.
	 */
	public static void setTextVisibilityOnExport(boolean on) { cacheTextVisibilityExport.setBoolean(on); }

	private static Tool.Pref cacheTextVisibilityAnnotation = User.tool.makeBooleanPref("TextVisibilityAnnotation", true);
	/**
	 * Method to tell whether to draw text annotation text.
	 * Annotation text is not attached to any node or arc, but appears to move freely about the cell.
	 * In implementation, they are displayable Variables on Generic:invisible-pin nodes.
	 * The default is "true".
	 * @return true if the system should draw annotation text.
	 */
	public static boolean isTextVisibilityOnAnnotation() { return cacheTextVisibilityAnnotation.getBoolean(); }
	/**
	 * Method to set whether to draw annotation text.
	 * Annotation text is not attached to any node or arc, but appears to move freely about the cell.
	 * In implementation, they are displayable Variables on Generic:invisible-pin nodes.
	 * @param on true if the system should draw annotation text.
	 */
	public static void setTextVisibilityOnAnnotation(boolean on) { cacheTextVisibilityAnnotation.setBoolean(on); }

	private static Tool.Pref cacheTextVisibilityInstance = User.tool.makeBooleanPref("TextVisibilityInstance", true);
	/**
	 * Method to tell whether to draw the name of on cell instances.
	 * The default is "true".
	 * @return true if the system should draw the name of on cell instances.
	 */
	public static boolean isTextVisibilityOnInstance() { return cacheTextVisibilityInstance.getBoolean(); }
	/**
	 * Method to set whether to draw the name of on cell instances.
	 * @param on true if the system should draw the name of on cell instances.
	 */
	public static void setTextVisibilityOnInstance(boolean on) { cacheTextVisibilityInstance.setBoolean(on); }

	private static Tool.Pref cacheTextVisibilityCell = User.tool.makeBooleanPref("TextVisibilityCell", true);
	/**
	 * Method to tell whether to draw text that resides on the cell.
	 * This includes the current cell's parameters or attributes (for example, spice templates).
	 * The default is "true".
	 * @return true if the system should draw text that resides on the cell.
	 */
	public static boolean isTextVisibilityOnCell() { return cacheTextVisibilityCell.getBoolean(); }
	/**
	 * Method to set whether to draw text that resides on the cell.
	 * This includes the current cell's parameters or attributes (for example, spice templates).
	 * @param on true if the system should draw text that resides on cthe cell.
	 */
	public static void setTextVisibilityOnCell(boolean on) { cacheTextVisibilityCell.setBoolean(on); }

	/****************************** MISCELLANEOUS PREFERENCES ******************************/

	private static Tool.Pref cacheBeepAfterLongJobs = User.tool.makeBooleanPref("BeepAfterLongJobs", false);
	/**
	 * Method to tell whether to beep after long jobs.
	 * Any task longer than 1 minute is considered a "long job".
	 * The default is "false".
	 * @return true if the system should beep after long jobs.
	 */
	public static boolean isBeepAfterLongJobs() { return cacheBeepAfterLongJobs.getBoolean(); }
	/**
	 * Method to set whether to beep after long jobs.
	 * Any task longer than 1 minute is considered a "long job".
	 * @param on true if the system should beep after long jobs.
	 */
	public static void setBeepAfterLongJobs(boolean on) { cacheBeepAfterLongJobs.setBoolean(on); }

	private static Tool.Pref cachePlayClickSoundsWhenCreatingArcs = User.tool.makeBooleanPref("PlayClickSoundsWhenCreatingArcs", true);
	/**
	 * Method to tell whether to play a "click" sound when an arc is created.
	 * The default is "true".
	 * @return true if the system should play a "click" sound when an arc is created
	 */
	public static boolean isPlayClickSoundsWhenCreatingArcs() { return cachePlayClickSoundsWhenCreatingArcs.getBoolean(); }
	/**
	 * Method to set whether to play a "click" sound when an arc is created
	 * @param on true if the system should play a "click" sound when an arc is created
	 */
	public static void setPlayClickSoundsWhenCreatingArcs(boolean on) { cachePlayClickSoundsWhenCreatingArcs.setBoolean(on); }

	private static Tool.Pref cacheIncludeDateAndVersionInOutput = User.tool.makeBooleanPref("IncludeDateAndVersionInOutput", true);
	/**
	 * Method to tell whether to include the date and Electric version in output files.
	 * The default is "true".
	 * @return true if the system should include the date and Electric version in output files.
	 */
	public static boolean isIncludeDateAndVersionInOutput() { return cacheIncludeDateAndVersionInOutput.getBoolean(); }
	/**
	 * Method to set whether to include the date and Electric version in output files.
	 * @param on true if the system should include the date and Electric version in output files.
	 */
	public static void setIncludeDateAndVersionInOutput(boolean on) { cacheIncludeDateAndVersionInOutput.setBoolean(on); }

	private static Tool.Pref cacheErrorLimit = User.tool.makeIntPref("ErrorLimit", 0);
	/**
	 * Method to tell the maximum number of errors to log.
	 * The default is 0, which means that there is no limit.
	 * @return the maximum number of errors to log.
	 */
	public static int getErrorLimit() { return cacheErrorLimit.getInt(); }
	/**
	 * Method to set the maximum number of errors to log.
	 * @param limit the maximum number of errors to log.
	 * A value of zero indicates that there is no limit.
	 */
	public static void setErrorLimit(int limit) { cacheErrorLimit.setInt(limit); }

	private static Tool.Pref cacheAutoTechnologySwitch = User.tool.makeBooleanPref("AutoTechnologySwitch", true);
	/**
	 * Method to tell whether to switch technologies automatically when changing the current Cell.
	 * Switching technologies means that the component menu updates to the new primitive set.
	 * The default is "true".
	 * @return true if the system should switch technologies automatically when changing the current Cell.
	 */
	public static boolean isAutoTechnologySwitch() { return cacheAutoTechnologySwitch.getBoolean(); }
	/**
	 * Method to set whether to switch technologies automatically when changing the current Cell.
	 * Switching technologies means that the component menu updates to the new primitive set.
	 * @param on true if the system should switch technologies automatically when changing the current Cell.
	 */
	public static void setAutoTechnologySwitch(boolean on) { cacheAutoTechnologySwitch.setBoolean(on); }

	private static Tool.Pref cachePlaceCellCenter = User.tool.makeBooleanPref("PlaceCellCenter", true);
	/**
	 * Method to tell whether to place a Cell-Center primitive in every newly created Cell.
	 * The default is "true".
	 * @return true if the system should place a Cell-Center primitive in every newly created Cell.
	 */
	public static boolean isPlaceCellCenter() { return cachePlaceCellCenter.getBoolean(); }
	/**
	 * Method to set whether to place a Cell-Center primitive in every newly created Cell.
	 * @param on true if the system should place a Cell-Center primitive in every newly created Cell.
	 */
	public static void setPlaceCellCenter(boolean on) { cachePlaceCellCenter.setBoolean(on); }

	private static Tool.Pref cacheCheckCellDates = User.tool.makeBooleanPref("CheckCellDates", false);
	/**
	 * Method to tell whether to check Cell dates when placing instances.
	 * This is not currently implemented.
	 * The default is "false".
	 * @return true if the system should check Cell dates when placing instances.
	 */
	public static boolean isCheckCellDates() { return cacheCheckCellDates.getBoolean(); }
	/**
	 * Method to set whether to check Cell dates when placing instances.
	 * This is not currently implemented.
	 * @param on true if the system should check Cell dates when placing instances.
	 */
	public static void setCheckCellDates(boolean on) { cacheCheckCellDates.setBoolean(on); }

	private static Tool.Pref cacheDisallowModificationLockedPrims = User.tool.makeBooleanPref("DisallowModificationLockedPrims", false);
	/**
	 * Method to tell whether locked primitives can be modified.
	 * Locked primitives occur in array-technologies such as FPGA.
	 * The default is "false".
	 * @return true if the locked primitives cannot be modified.
	 */
	public static boolean isDisallowModificationLockedPrims() { return cacheDisallowModificationLockedPrims.getBoolean(); }
	/**
	 * Method to set whether locked primitives can be modified.
	 * @param on true if locked primitives cannot be modified.
	 */
	public static void setDisallowModificationLockedPrims(boolean on) { cacheDisallowModificationLockedPrims.setBoolean(on); }

	private static Tool.Pref cacheMoveAfterDuplicate = User.tool.makeBooleanPref("MoveAfterDuplicate", true);
	/**
	 * Method to tell whether to move objects after duplicating them.
	 * The default is "true".
	 * @return true if the system should move objects after duplicating them.
	 */
	public static boolean isMoveAfterDuplicate() { return cacheMoveAfterDuplicate.getBoolean(); }
	/**
	 * Method to set whether to move objects after duplicating them.
	 * @param on true if the system should move objects after duplicating them.
	 */
	public static void setMoveAfterDuplicate(boolean on) { cacheMoveAfterDuplicate.setBoolean(on); }

	private static Tool.Pref cacheDupCopiesExports = User.tool.makeBooleanPref("DupCopiesExports", false);
	/**
	 * Method to tell whether Duplicate/Paste/Array of NodeInst copies exports.
	 * The default is "false".
	 * @return true if the system copies exports when doing a Duplicate/Paste/Array of NodeInst.
	 */
	public static boolean isDupCopiesExports() { return cacheDupCopiesExports.getBoolean(); }
	/**
	 * Method to set whether Duplicate/Paste/Array of NodeInst copies exports.
	 * @param on true if the system copies exports when doing a Duplicate/Paste/Array of NodeInst.
	 */
	public static void setDupCopiesExports(boolean on) { cacheDupCopiesExports.setBoolean(on); }

	private static Tool.Pref cacheNewNodeRotation = User.tool.makeIntPref("NewNodeRotation", 0);
	/**
	 * Method to return the default rotation of all new nodes.
	 * The default is 0.
	 * @return the default rotation of all new nodes.
	 */
	public static int getNewNodeRotation() { return cacheNewNodeRotation.getInt(); }
	/**
	 * Method to set the default rotation of all new nodes.
	 * @param rot the default rotation of all new nodes.
	 */
	public static void setNewNodeRotation(int rot) { cacheNewNodeRotation.setInt(rot); }

	private static Tool.Pref cacheNewNodeMirrorX = User.tool.makeBooleanPref("NewNodeMirrorX", false);
	/**
	 * Method to tell whether new nodes are mirrored in X.
	 * The default is "false".
	 * @return true if new nodes are mirrored in X.
	 */
	public static boolean isNewNodeMirrorX() { return cacheNewNodeMirrorX.getBoolean(); }
	/**
	 * Method to set whether new nodes are mirrored in X.
	 * @param on true if new nodes are mirrored in X.
	 */
	public static void setNewNodeMirrorX(boolean on) { cacheNewNodeMirrorX.setBoolean(on); }

}
