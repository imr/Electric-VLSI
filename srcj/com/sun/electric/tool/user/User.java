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
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

/**
 * This is the User Interface tool.
 */
public class User extends Tool
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

	/**
	 * Method to force all User Preferences to be saved.
	 */
	private static void flushOptions()
	{
		try
		{
	        tool.prefs.flush();
		} catch (BackingStoreException e)
		{
			System.out.println("Failed to save user interface options");
		}
	}

	/****************************** ICON GENERATION PREFERENCES ******************************/

	/**
	 * Method to tell whether generated icons should have leads drawn.
	 * The default is "true".
	 * @return true if generated icons should have leads drawn.
	 */
	private static boolean cacheIconGenDrawLeads = tool.prefs.getBoolean("IconGenDrawLeads", true);
	public static boolean isIconGenDrawLeads() { return cacheIconGenDrawLeads; }
	/**
	 * Method to set whether generated icons should have leads drawn.
	 * @param on true if generated icons should have leads drawn.
	 */
	public static void setIconGenDrawLeads(boolean on)
	{
		tool.prefs.putBoolean("IconGenDrawLeads", cacheIconGenDrawLeads = on);
		flushOptions();
	}

	/**
	 * Method to tell whether generated icons should have a body drawn.
	 * The body is just a rectangle.
	 * The default is "true".
	 * @return true if generated icons should have a body drawn.
	 */
	private static boolean cacheIconGenDrawBody = tool.prefs.getBoolean("IconGenDrawBody", true);
	public static boolean isIconGenDrawBody() { return cacheIconGenDrawBody; }
	/**
	 * Method to set whether generated icons should have a body drawn.
	 * The body is just a rectangle.
	 * @param on true if generated icons should have a body drawn.
	 */
	public static void setIconGenDrawBody(boolean on)
	{
		tool.prefs.putBoolean("IconGenDrawBody", cacheIconGenDrawBody = on);
		flushOptions();
	}

	/**
	 * Method to tell whether generated icons should reverse the order of exports.
	 * Normally, exports are drawn top-to-bottom alphabetically.
	 * The default is "false".
	 * @return true if generated icons should reverse the order of exports.
	 */
	private static boolean cacheIconGenReverseExportOrder = tool.prefs.getBoolean("IconGenReverseExportOrder", false);
	public static boolean isIconGenReverseExportOrder() { return cacheIconGenReverseExportOrder; }
	/**
	 * Method to set whether generated icons should reverse the order of exports.
	 * Normally, exports are drawn top-to-bottom alphabetically.
	 * @param on true if generated icons should reverse the order of exports.
	 */
	public static void setIconGenReverseExportOrder(boolean on)
	{
		tool.prefs.putBoolean("IconGenReverseExportOrder", cacheIconGenReverseExportOrder = on);
		flushOptions();
	}

	/**
	 * Method to tell where Input ports should go on generated icons.
	 * @return information about where Input ports should go on generated icons.
	 * 0: left (the default)   1: right   2: top   3: bottom
	 */
	private static int cacheIconGenInputSide = tool.prefs.getInt("IconGenInputSide", 0);
	public static int getIconGenInputSide() { return cacheIconGenInputSide; }
	/**
	 * Method to set where Input ports should go on generated icons.
	 * @param side information about where Input ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static void setIconGenInputSide(int side)
	{
		tool.prefs.putInt("IconGenInputSide", cacheIconGenInputSide = side);
		flushOptions();
	}

	/**
	 * Method to tell where Output ports should go on generated icons.
	 * @return information about where Output ports should go on generated icons.
	 * 0: left   1: right (the default)   2: top   3: bottom
	 */
	private static int cacheIconGenOutputSide = tool.prefs.getInt("IconGenOutputSide", 1);
	public static int getIconGenOutputSide() { return cacheIconGenOutputSide; }
	/**
	 * Method to set where Output ports should go on generated icons.
	 * @param side information about where Output ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static void setIconGenOutputSide(int side)
	{
		tool.prefs.putInt("IconGenOutputSide", cacheIconGenOutputSide = side);
		flushOptions();
	}

	/**
	 * Method to tell where Bidirectional ports should go on generated icons.
	 * @return information about where Bidirectional ports should go on generated icons.
	 * 0: left   1: right   2: top (the default)   3: bottom
	 */
	private static int cacheIconGenBidirSide = tool.prefs.getInt("IconGenBidirSide", 2);
	public static int getIconGenBidirSide() { return cacheIconGenBidirSide; }
	/**
	 * Method to set where Bidirectional ports should go on generated icons.
	 * @param side information about where Bidirectional ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static void setIconGenBidirSide(int side)
	{
		tool.prefs.putInt("IconGenBidirSide", cacheIconGenBidirSide = side);
		flushOptions();
	}

	/**
	 * Method to tell where Power ports should go on generated icons.
	 * @return information about where Power ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom (the default)
	 */
	private static int cacheIconGenPowerSide = tool.prefs.getInt("IconGenPowerSide", 3);
	public static int getIconGenPowerSide() { return cacheIconGenPowerSide; }
	/**
	 * Method to set where Power ports should go on generated icons.
	 * @param side information about where Power ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static void setIconGenPowerSide(int side)
	{
		tool.prefs.putInt("IconGenPowerSide", cacheIconGenPowerSide = side);
		flushOptions();
	}

	/**
	 * Method to tell where Ground ports should go on generated icons.
	 * @return information about where Ground ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom (the default)
	 */
	private static int cacheIconGenGroundSide = tool.prefs.getInt("IconGenGroundSide", 3);
	public static int getIconGenGroundSide() { return cacheIconGenGroundSide; }
	/**
	 * Method to set where Ground ports should go on generated icons.
	 * @param side information about where Ground ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static void setIconGenGroundSide(int side)
	{
		tool.prefs.putInt("IconGenGroundSide", cacheIconGenGroundSide = side);
		flushOptions();
	}

	/**
	 * Method to tell where Clock ports should go on generated icons.
	 * @return information about where Clock ports should go on generated icons.
	 * 0: left (the default)   1: right   2: top   3: bottom
	 */
	private static int cacheIconGenClockSide = tool.prefs.getInt("IconGenClockSide", 0);
	public static int getIconGenClockSide() { return cacheIconGenClockSide; }
	/**
	 * Method to set where Clock ports should go on generated icons.
	 * @param side information about where Clock ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static void setIconGenClockSide(int side)
	{
		tool.prefs.putInt("IconGenClockSide", cacheIconGenClockSide = side);
		flushOptions();
	}

	/**
	 * Method to tell where exports should appear along the leads in generated icons.
	 * @return information about where exports should appear along the leads in generated icons.
	 * 0: on the body   1: (the default) at the end of the lead   2: in the middle of the lead
	 */
	private static int cacheIconGenExportLocation = tool.prefs.getInt("IconGenExportLocation", 1);
	public static int getIconGenExportLocation() { return cacheIconGenExportLocation; }
	/**
	 * Method to set how exports should appear along the leads in generated icons.
	 * @param loc information about where exports should appear along the leads in generated icons.
	 * 0: on the body   1: at the end of the lead   2: in the middle of the lead
	 */
	public static void setIconGenExportLocation(int loc)
	{
		tool.prefs.putInt("IconGenExportLocation", cacheIconGenExportLocation = loc);
		flushOptions();
	}

	/**
	 * Method to tell how the text should appear in generated icons.
	 * @return information about how the text should appear in generated icons.
	 * 0: (the default) centered at the export location
	 * 1: pointing inward from the export location
	 * 2: pointing outward from the export location
	 */
	private static int cacheIconGenExportStyle = tool.prefs.getInt("IconGenExportStyle", 0);
	public static int getIconGenExportStyle() { return cacheIconGenExportStyle; }
	/**
	 * Method to set how the text should appear in generated icons.
	 * @param style information about how the text should appear in generated icons.
	 * 0: centered at the export location
	 * 1: pointing inward from the export location
	 * 2: pointing outward from the export location
	 */
	public static void setIconGenExportStyle(int style)
	{
		tool.prefs.putInt("IconGenExportStyle", cacheIconGenExportStyle = style);
		flushOptions();
	}

	/**
	 * Method to tell how exports should be constructed in generated icons.
	 * @return information about how exports should be constructed in generated icons.
	 * 0: (the default) use Generic:Universal-Pins for exports (can connect to ANYTHING).
	 * 1: use Schematic:Bus-Pins for exports (can connect to schematic busses or wires).
	 */
	private static int cacheIconGenExportTech = tool.prefs.getInt("IconGenExportTech", 0);
	public static int getIconGenExportTech() { return cacheIconGenExportTech; }
	/**
	 * Method to set how exports should be constructed in generated icons.
	 * @param t information about how exports should be constructed in generated icons.
	 * 0: use Generic:Universal-Pins for exports (can connect to ANYTHING).
	 * 1: use Schematic:Bus-Pins for exports (can connect to schematic busses or wires).
	 */
	public static void setIconGenExportTech(int t)
	{
		tool.prefs.putInt("IconGenExportTech", cacheIconGenExportTech = t);
		flushOptions();
	}

	/**
	 * Method to tell where to place an instance of the generated icons in the original schematic.
	 * @return information about where to place an instance of the generated icons in the original schematic
	 * 0: (the default) in the upper-right corner.
	 * 1: in the upper-left corner.
	 * 2: in the lower-right corner.
	 * 3: in the lower-left corner.
	 */
	private static int cacheIconGenInstanceLocation = tool.prefs.getInt("IconGenInstanceLocation", 0);
	public static int getIconGenInstanceLocation() { return cacheIconGenInstanceLocation; }
	/**
	 * Method to set where to place an instance of the generated icons in the original schematic.
	 * @param loc information about where to place an instance of the generated icons in the original schematic.
	 * 0: in the upper-right corner.
	 * 1: in the upper-left corner.
	 * 2: in the lower-right corner.
	 * 3: in the lower-left corner.
	 */
	public static void setIconGenInstanceLocation(int loc)
	{
		tool.prefs.putInt("IconGenInstanceLocation", cacheIconGenInstanceLocation = loc);
		flushOptions();
	}

	/**
	 * Method to tell how long to make leads in generated icons.
	 * @return information about how long to make leads in generated icons (the default is 2).
	 */
	private static float cacheIconGenLeadLength =  tool.prefs.getFloat("IconGenLeadLength", 2.0f);
	public static float getIconGenLeadLength() { return cacheIconGenLeadLength; }
	/**
	 * Method to set how long to make leads in generated icons.
	 * @param len how long to make leads in generated icons.
	 */
	public static void setIconGenLeadLength(float len)
	{
		tool.prefs.putFloat("IconGenLeadLength", cacheIconGenLeadLength = len);
		flushOptions();
	}

	/**
	 * Method to tell how far apart to space leads in generated icons.
	 * @return information about how far apart to space leads in generated icons (the default is 2).
	 */
	private static float cacheIconGenLeadSpacing =  tool.prefs.getFloat("IconGenLeadSpacing", 2.0f);
	public static float getIconGenLeadSpacing() { return cacheIconGenLeadSpacing; }
	/**
	 * Method to set how far apart to space leads in generated icons.
	 * @param dist how far apart to space leads in generated icons.
	 */
	public static void setIconGenLeadSpacing(float dist)
	{
		tool.prefs.putFloat("IconGenLeadSpacing", cacheIconGenLeadSpacing = dist);
		flushOptions();
	}

	/****************************** PORT AND EXPORT PREFERENCES ******************************/

	/**
	 * Method to tell how to display ports.
	 * @return how to display ports.
	 * 0: full port names (the default).
	 * 1: short port names (stopping at the first nonalphabetic character).
	 * 2: ports drawn as crosses.
	 */
	private static int cachePortDisplayLevel = tool.prefs.getInt("PortDisplayLevel", 0);
	public static int getPortDisplayLevel() { return cachePortDisplayLevel; }
	/**
	 * Method to set how to display ports.
	 * @param level how to display ports.
	 * 0: full port names (the default).
	 * 1: short port names (stopping at the first nonalphabetic character).
	 * 2: ports drawn as crosses.
	 */
	public static void setPortDisplayLevels(int level)
	{
		tool.prefs.putInt("PortDisplayLevel", cachePortDisplayLevel = level);
		flushOptions();
	}

	/**
	 * Method to tell how to display exports.
	 * @return how to display exports.
	 * 0: full export names (the default).
	 * 1: short export names (stopping at the first nonalphabetic character).
	 * 2: exports drawn as crosses.
	 */
	private static int cacheExportDisplayLevel = tool.prefs.getInt("ExportDisplayLevel", 0);
	public static int getExportDisplayLevel() { return cacheExportDisplayLevel; }
	/**
	 * Method to set how to display exports.
	 * @param level how to display exports.
	 * 0: full export names (the default).
	 * 1: short export names (stopping at the first nonalphabetic character).
	 * 2: exports drawn as crosses.
	 */
	public static void setExportDisplayLevels(int level)
	{
		tool.prefs.putInt("ExportDisplayLevel", cacheExportDisplayLevel = level);
		flushOptions();
	}

	/**
	 * Method to tell whether to move a node when its export name moves.
	 * The default is "false", which means that the export text can move independently.
	 * @return true to move a node when its export name moves.
	 */
	private static boolean cacheMoveNodeWithExport = tool.prefs.getBoolean("MoveNodeWithExport", false);
	public static boolean isMoveNodeWithExport() { return cacheMoveNodeWithExport; }
	/**
	 * Method to set whether to move a node when its export name moves.
	 * @param on true to move a node when its export name moves.
	 */
	public static void setMoveNodeWithExport(boolean on)
	{
		tool.prefs.putBoolean("MoveNodeWithExport", cacheMoveNodeWithExport = on);
		flushOptions();
	}

	/****************************** SELECTION PREFERENCES ******************************/

	/**
	 * Method to tell whether cell instances are all be easy-to-select.
	 * The default is "true".
	 * @return true if cell instances are all be easy-to-select.
	 */
	private static boolean cacheEasySelectionOfCellInstances = tool.prefs.getBoolean("EasySelectionOfCellInstances", true);
	public static boolean isEasySelectionOfCellInstances() { return cacheEasySelectionOfCellInstances; }
	/**
	 * Method to set whether cell instances are all be easy-to-select.
	 * @param on true if cell instances are all to be easy-to-select.
	 */
	public static void setEasySelectionOfCellInstances(boolean on)
	{
		tool.prefs.putBoolean("EasySelectionOfCellInstances", cacheEasySelectionOfCellInstances = on);
		flushOptions();
	}

	/**
	 * Method to tell whether annotation text is easy-to-select.
	 * The default is "true".
	 * @return true if annotation text is easy-to-select.
	 */
	private static boolean cacheEasySelectionOfAnnotationText = tool.prefs.getBoolean("EasySelectionOfAnnotationText", true);
	public static boolean isEasySelectionOfAnnotationText() { return cacheEasySelectionOfAnnotationText; }
	/**
	 * Method to set whether annotation text is easy-to-select.
	 * @param on true if annotation text is easy-to-select.
	 */
	public static void setEasySelectionOfAnnotationText(boolean on)
	{
		tool.prefs.putBoolean("EasySelectionOfAnnotationText", cacheEasySelectionOfAnnotationText = on);
		flushOptions();
	}

	/**
	 * Method to tell whether dragging a selection rectangle must completely encose objects in order to select them.
	 * The default is "false", which means that the selection rectangle need only touch an object in order to select it.
	 * @return true if dragging a selection rectangle must completely encose objects in order to select them.
	 */
	private static boolean cacheDraggingMustEncloseObjects = tool.prefs.getBoolean("DraggingMustEncloseObjects", false);
	public static boolean isDraggingMustEncloseObjects() { return cacheDraggingMustEncloseObjects; }
	/**
	 * Method to set whether dragging a selection rectangle must completely encose objects in order to select them.
	 * @param on true if dragging a selection rectangle must completely encose objects in order to select them.
	 */
	public static void setDraggingMustEncloseObjects(boolean on)
	{
		tool.prefs.putBoolean("DraggingMustEncloseObjects", cacheDraggingMustEncloseObjects = on);
		flushOptions();
	}

	/****************************** GRID AND ALIGNMENT PREFERENCES ******************************/

	/**
	 * Method to return the default spacing of grid dots in the X direction.
	 * The default is 1.
	 * @return true the default spacing of grid dots in the X direction.
	 */
	private static float cacheDefGridXSpacing = tool.prefs.getFloat("DefGridXSpacing", 1);
	public static float getDefGridXSpacing() { return cacheDefGridXSpacing; }
	/**
	 * Method to set the default spacing of grid dots in the X direction.
	 * @param dist the default spacing of grid dots in the X direction.
	 */
	public static void setDefGridXSpacing(float dist)
	{
		tool.prefs.putFloat("DefGridXSpacing", cacheDefGridXSpacing = dist);
		flushOptions();
	}

	/**
	 * Method to return the default spacing of grid dots in the Y direction.
	 * The default is 1.
	 * @return true the default spacing of grid dots in the Y direction.
	 */
	private static float cacheDefGridYSpacing = tool.prefs.getFloat("DefGridYSpacing", 1);
	public static float getDefGridYSpacing() { return cacheDefGridYSpacing; }
	/**
	 * Method to set the default spacing of grid dots in the Y direction.
	 * @param dist the default spacing of grid dots in the Y direction.
	 */
	public static void setDefGridYSpacing(float dist)
	{
		tool.prefs.putFloat("DefGridYSpacing", cacheDefGridYSpacing = dist);
		flushOptions();
	}

	/**
	 * Method to return the default frequency of bold grid dots in the X direction.
	 * The default is 10.
	 * @return true the default frequency of bold grid dots in the X direction.
	 */
	private static int cacheDefGridXBoldFrequency = tool.prefs.getInt("DefGridXBoldFrequency", 10);
	public static int getDefGridXBoldFrequency() { return cacheDefGridXBoldFrequency; }
	/**
	 * Method to set the default frequency of bold grid dots in the X direction.
	 * @param dist the default frequency of bold grid dots in the X direction.
	 */
	public static void setDefGridXBoldFrequency(int dist)
	{
		tool.prefs.putInt("DefGridXBoldFrequency", cacheDefGridXBoldFrequency = dist);
		flushOptions();
	}

	/**
	 * Method to return the default frequency of bold grid dots in the Y direction.
	 * The default is 10.
	 * @return true the default frequency of bold grid dots in the Y direction.
	 */
	private static int cacheDefGridYBoldFrequency = tool.prefs.getInt("DefGridYBoldFrequency", 10);
	public static int getDefGridYBoldFrequency() { return cacheDefGridYBoldFrequency; }
	/**
	 * Method to set the default frequency of bold grid dots in the Y direction.
	 * @param dist the default frequency of bold grid dots in the Y direction.
	 */
	public static void setDefGridYBoldFrequency(int dist)
	{
		tool.prefs.putInt("DefGridYBoldFrequency", cacheDefGridYBoldFrequency = dist);
		flushOptions();
	}

	/**
	 * Method to tell whether to align the grid dots with the circuitry.
	 * The default is "false", which implies that the grid dots are placed independently of object locations.
	 * @return true to align the grid dots with the circuitry.
	 */
	private static boolean cacheAlignGridWithCircuitry = tool.prefs.getBoolean("AlignGridWithCircuitry", true);
	public static boolean isAlignGridWithCircuitry() { return cacheAlignGridWithCircuitry; }
	/**
	 * Method to set whether to align the grid dots with the circuitry.
	 * @param on true to align the grid dots with the circuitry.
	 */
	public static void setAlignGridWithCircuitry(boolean on)
	{
		tool.prefs.putBoolean("AlignGridWithCircuitry", cacheAlignGridWithCircuitry = on);
		flushOptions();
	}

	/**
	 * Method to tell whether to show the cursor coordinates as they move in the edit window.
	 * The default is "false".
	 * @return true to show the cursor coordinates as they move in the edit window
	 */
	private static boolean cacheShowCursorCoordinates = tool.prefs.getBoolean("ShowCursorCoordinates", false);
	public static boolean isShowCursorCoordinates() { return cacheShowCursorCoordinates; }
	/**
	 * Method to set whether to show the cursor coordinates as they move in the edit window
	 * @param on true to show the cursor coordinates as they move in the edit window
	 */
	public static void setShowCursorCoordinates(boolean on)
	{
		tool.prefs.putBoolean("ShowCursorCoordinates", cacheShowCursorCoordinates = on);
		flushOptions();
	}

	/**
	 * Method to return the default alignment of objects to the grid.
	 * The default is 1, meaning that placement and movement should land on whole grid units.
	 * @return true the default alignment of objects to the grid.
	 */
	private static float cacheAlignmentToGrid = tool.prefs.getFloat("AlignmentToGrid", 1);
	public static float getAlignmentToGrid() { return cacheAlignmentToGrid; }
	/**
	 * Method to set the default alignment of objects to the grid.
	 * @param dist the default alignment of objects to the grid.
	 */
	public static void setAlignmentToGrid(float dist)
	{
		tool.prefs.putFloat("AlignmentToGrid", cacheAlignmentToGrid = dist);
		flushOptions();
	}

	/**
	 * Method to return the default alignment of object edges to the grid.
	 * The default is 0, meaning that no alignment is to be done.
	 * @return true the default alignment of object edges to the grid.
	 */
	private static float cacheEdgeAlignmentToGrid = tool.prefs.getFloat("EdgeAlignmentToGrid", 0);
	public static float getEdgeAlignmentToGrid() { return cacheEdgeAlignmentToGrid; }
	/**
	 * Method to set the default alignment of object edges to the grid.
	 * @param dist the default alignment of object edges to the grid.
	 */
	public static void setEdgeAlignmentToGrid(float dist)
	{
		tool.prefs.putFloat("EdgeAlignmentToGrid", cacheEdgeAlignmentToGrid = dist);
		flushOptions();
	}

	/****************************** TEXT PREFERENCES ******************************/

	/**
	 * Method to tell whether to draw text that resides on nodes.
	 * This text includes the node name and any parameters or attributes on it.
	 * The default is "true".
	 * @return true if the system should draw text that resides on nodes.
	 */
	private static boolean cacheTextVisibilityNode = tool.prefs.getBoolean("TextVisibilityNode", true);
	public static boolean isTextVisibilityOnNode() { return cacheTextVisibilityNode; }
	/**
	 * Method to set whether to draw text that resides on nodes.
	 * This text includes the node name and any parameters or attributes on it.
	 * @param on true if the system should draw text that resides on nodes.
	 */
	public static void setTextVisibilityOnNode(boolean on)
	{
		tool.prefs.putBoolean("TextVisibilityNode", cacheTextVisibilityNode = on);
		flushOptions();
	}

	/**
	 * Method to tell whether to draw text that resides on arcs.
	 * This text includes the arc name and any parameters or attributes on it.
	 * The default is "true".
	 * @return true if the system should draw text that resides on arcs.
	 */
	private static boolean cacheTextVisibilityArc = tool.prefs.getBoolean("TextVisibilityArc", true);
	public static boolean isTextVisibilityOnArc() { return cacheTextVisibilityArc; }
	/**
	 * Method to set whether to draw text that resides on arcs.
	 * This text includes the arc name and any parameters or attributes on it.
	 * @param on true if the system should draw text that resides on arcs.
	 */
	public static void setTextVisibilityOnArc(boolean on)
	{
		tool.prefs.putBoolean("TextVisibilityArc", cacheTextVisibilityArc = on);
		flushOptions();
	}

	/**
	 * Method to tell whether to draw text that resides on ports.
	 * This text includes the port name and any parameters or attributes on it.
	 * The default is "true".
	 * @return true if the system should draw text that resides on ports.
	 */
	private static boolean cacheTextVisibilityPort = tool.prefs.getBoolean("TextVisibilityPort", true);
	public static boolean isTextVisibilityOnPort() { return cacheTextVisibilityPort; }
	/**
	 * Method to set whether to draw text that resides on ports.
	 * This text includes the port name and any parameters or attributes on it.
	 * @param on true if the system should draw text that resides on ports.
	 */
	public static void setTextVisibilityOnPort(boolean on)
	{
		tool.prefs.putBoolean("TextVisibilityPort", cacheTextVisibilityPort = on);
		flushOptions();
	}

	/**
	 * Method to tell whether to draw text that resides on exports.
	 * This text includes the export name and any parameters or attributes on it.
	 * The default is "true".
	 * @return true if the system should draw text that resides on exports.
	 */
	private static boolean cacheTextVisibilityExport = tool.prefs.getBoolean("TextVisibilityExport", true);
	public static boolean isTextVisibilityOnExport() { return cacheTextVisibilityExport; }
	/**
	 * Method to set whether to draw text that resides on exports.
	 * This text includes the export name and any parameters or attributes on it.
	 * @param on true if the system should draw text that resides on exports.
	 */
	public static void setTextVisibilityOnExport(boolean on)
	{
		tool.prefs.putBoolean("TextVisibilityExport", cacheTextVisibilityExport = on);
		flushOptions();
	}

	/**
	 * Method to tell whether to draw text annotation text.
	 * Annotation text is not attached to any node or arc, but appears to move freely about the cell.
	 * In implementation, they are displayable Variables on Generic:invisible-pin nodes.
	 * The default is "true".
	 * @return true if the system should draw annotation text.
	 */
	private static boolean cacheTextVisibilityAnnotation = tool.prefs.getBoolean("TextVisibilityAnnotation", true);
	public static boolean isTextVisibilityOnAnnotation() { return cacheTextVisibilityAnnotation; }
	/**
	 * Method to set whether to draw annotation text.
	 * Annotation text is not attached to any node or arc, but appears to move freely about the cell.
	 * In implementation, they are displayable Variables on Generic:invisible-pin nodes.
	 * @param on true if the system should draw annotation text.
	 */
	public static void setTextVisibilityOnAnnotation(boolean on)
	{
		tool.prefs.putBoolean("TextVisibilityAnnotation", cacheTextVisibilityAnnotation = on);
		flushOptions();
	}

	/**
	 * Method to tell whether to draw the name of on cell instances.
	 * The default is "true".
	 * @return true if the system should draw the name of on cell instances.
	 */
	private static boolean cacheTextVisibilityInstance = tool.prefs.getBoolean("TextVisibilityInstance", true);
	public static boolean isTextVisibilityOnInstance() { return cacheTextVisibilityInstance; }
	/**
	 * Method to set whether to draw the name of on cell instances.
	 * @param on true if the system should draw the name of on cell instances.
	 */
	public static void setTextVisibilityOnInstance(boolean on)
	{
		tool.prefs.putBoolean("TextVisibilityInstance", cacheTextVisibilityInstance = on);
		flushOptions();
	}

	/**
	 * Method to tell whether to draw text that resides on the cell.
	 * This includes the current cell's parameters or attributes (for example, spice templates).
	 * The default is "true".
	 * @return true if the system should draw text that resides on the cell.
	 */
	private static boolean cacheTextVisibilityCell = tool.prefs.getBoolean("TextVisibilityCell", true);
	public static boolean isTextVisibilityOnCell() { return cacheTextVisibilityCell; }
	/**
	 * Method to set whether to draw text that resides on the cell.
	 * This includes the current cell's parameters or attributes (for example, spice templates).
	 * @param on true if the system should draw text that resides on cthe cell.
	 */
	public static void setTextVisibilityOnCell(boolean on)
	{
		tool.prefs.putBoolean("TextVisibilityCell", cacheTextVisibilityCell = on);
		flushOptions();
	}

	/****************************** MISCELLANEOUS PREFERENCES ******************************/

	/**
	 * Method to tell whether to beep after long jobs.
	 * Any task longer than 1 minute is considered a "long job".
	 * The default is "false".
	 * @return true if the system should beep after long jobs.
	 */
	private static boolean cacheBeepAfterLongJobs = tool.prefs.getBoolean("BeepAfterLongJobs", false);
	public static boolean isBeepAfterLongJobs() { return cacheBeepAfterLongJobs; }
	/**
	 * Method to set whether to beep after long jobs.
	 * Any task longer than 1 minute is considered a "long job".
	 * @param on true if the system should beep after long jobs.
	 */
	public static void setBeepAfterLongJobs(boolean on)
	{
		tool.prefs.putBoolean("BeepAfterLongJobs", cacheBeepAfterLongJobs = on);
		flushOptions();
	}

	/**
	 * Method to tell whether to play a "click" sound when an arc is created.
	 * The default is "true".
	 * @return true if the system should play a "click" sound when an arc is created
	 */
	private static boolean cachePlayClickSoundsWhenCreatingArcs = tool.prefs.getBoolean("PlayClickSoundsWhenCreatingArcs", false);
	public static boolean isPlayClickSoundsWhenCreatingArcs() { return cachePlayClickSoundsWhenCreatingArcs; }
	/**
	 * Method to set whether to play a "click" sound when an arc is created
	 * @param on true if the system should play a "click" sound when an arc is created
	 */
	public static void setPlayClickSoundsWhenCreatingArcs(boolean on)
	{
		tool.prefs.putBoolean("PlayClickSoundsWhenCreatingArcs", cachePlayClickSoundsWhenCreatingArcs = on);
		flushOptions();
	}

	/**
	 * Method to tell whether to include the date and Electric version in output files.
	 * The default is "true".
	 * @return true if the system should include the date and Electric version in output files.
	 */
	private static boolean cacheIncludeDateAndVersionInOutput = tool.prefs.getBoolean("IncludeDateAndVersionInOutput", false);
	public static boolean isIncludeDateAndVersionInOutput() { return cacheIncludeDateAndVersionInOutput; }
	/**
	 * Method to set whether to include the date and Electric version in output files.
	 * @param on true if the system should include the date and Electric version in output files.
	 */
	public static void setIncludeDateAndVersionInOutput(boolean on)
	{
		tool.prefs.putBoolean("IncludeDateAndVersionInOutput", cacheIncludeDateAndVersionInOutput = on);
		flushOptions();
	}

	/**
	 * Method to tell the maximum number of errors to log.
	 * The default is 0, which means that there is no limit.
	 * @return the maximum number of errors to log.
	 */
	private static int cacheErrorLimit = tool.prefs.getInt("ErrorLimit", 0);
	public static int getErrorLimit() { return cacheErrorLimit; }
	/**
	 * Method to set the maximum number of errors to log.
	 * @param limit the maximum number of errors to log.
	 * A value of zero indicates that there is no limit.
	 */
	public static void setErrorLimit(int limit)
	{
		tool.prefs.putInt("ErrorLimit", cacheErrorLimit = limit);
		flushOptions();
	}

	/**
	 * Method to tell whether to switch technologies automatically when changing the current Cell.
	 * Switching technologies means that the component menu updates to the new primitive set.
	 * The default is "true".
	 * @return true if the system should switch technologies automatically when changing the current Cell.
	 */
	private static boolean cacheAutoTechnologySwitch = tool.prefs.getBoolean("AutoTechnologySwitch", false);
	public static boolean isAutoTechnologySwitch() { return cacheAutoTechnologySwitch; }
	/**
	 * Method to set whether to switch technologies automatically when changing the current Cell.
	 * Switching technologies means that the component menu updates to the new primitive set.
	 * @param on true if the system should switch technologies automatically when changing the current Cell.
	 */
	public static void setAutoTechnologySwitch(boolean on)
	{
		tool.prefs.putBoolean("AutoTechnologySwitch", cacheAutoTechnologySwitch = on);
		flushOptions();
	}

	/**
	 * Method to tell whether to place a Cell-Center primitive in every newly created Cell.
	 * The default is "true".
	 * @return true if the system should place a Cell-Center primitive in every newly created Cell.
	 */
	private static boolean cachePlaceCellCenter = tool.prefs.getBoolean("PlaceCellCenter", false);
	public static boolean isPlaceCellCenter() { return cachePlaceCellCenter; }
	/**
	 * Method to set whether to place a Cell-Center primitive in every newly created Cell.
	 * @param on true if the system should place a Cell-Center primitive in every newly created Cell.
	 */
	public static void setPlaceCellCenter(boolean on)
	{
		tool.prefs.putBoolean("PlaceCellCenter", cachePlaceCellCenter = on);
		flushOptions();
	}

	/**
	 * Method to tell whether to check Cell dates when placing instances.
	 * This is not currently implemented.
	 * The default is "false".
	 * @return true if the system should check Cell dates when placing instances.
	 */
	private static boolean cacheCheckCellDates = tool.prefs.getBoolean("CheckCellDates", false);
	public static boolean isCheckCellDates() { return cacheCheckCellDates; }
	/**
	 * Method to set whether to check Cell dates when placing instances.
	 * This is not currently implemented.
	 * @param on true if the system should check Cell dates when placing instances.
	 */
	public static void setCheckCellDates(boolean on)
	{
		tool.prefs.putBoolean("CheckCellDates", cacheCheckCellDates = on);
		flushOptions();
	}

	/**
	 * Method to tell whether locked primitives can be modified.
	 * Locked primitives occur in array-technologies such as FPGA.
	 * The default is "false".
	 * @return true if the locked primitives cannot be modified.
	 */
	private static boolean cacheDisallowModificationLockedPrims = tool.prefs.getBoolean("DisallowModificationLockedPrims", false);
	public static boolean isDisallowModificationLockedPrims() { return cacheDisallowModificationLockedPrims; }
	/**
	 * Method to set whether locked primitives can be modified.
	 * @param on true if locked primitives cannot be modified.
	 */
	public static void setDisallowModificationLockedPrims(boolean on)
	{
		tool.prefs.putBoolean("DisallowModificationLockedPrims", cacheDisallowModificationLockedPrims = on);
		flushOptions();
	}

	/**
	 * Method to tell whether to move objects after duplicating them.
	 * The default is "true".
	 * @return true if the system should move objects after duplicating them.
	 */
	private static boolean cacheMoveAfterDuplicate = tool.prefs.getBoolean("MoveAfterDuplicate", false);
	public static boolean isMoveAfterDuplicate() { return cacheMoveAfterDuplicate; }
	/**
	 * Method to set whether to move objects after duplicating them.
	 * @param on true if the system should move objects after duplicating them.
	 */
	public static void setMoveAfterDuplicate(boolean on)
	{
		tool.prefs.putBoolean("MoveAfterDuplicate", cacheMoveAfterDuplicate = on);
		flushOptions();
	}

	/**
	 * Method to tell whether Duplicate/Paste/Array of NodeInst copies exports.
	 * The default is "false".
	 * @return true if the system copies exports when doing a Duplicate/Paste/Array of NodeInst.
	 */
	private static boolean cacheDupCopiesExports = tool.prefs.getBoolean("DupCopiesExports", false);
	public static boolean isDupCopiesExports() { return cacheDupCopiesExports; }
	/**
	 * Method to set whether Duplicate/Paste/Array of NodeInst copies exports.
	 * @param on true if the system copies exports when doing a Duplicate/Paste/Array of NodeInst.
	 */
	public static void setDupCopiesExports(boolean on)
	{
		tool.prefs.putBoolean("DupCopiesExports", cacheDupCopiesExports = on);
		flushOptions();
	}

	/**
	 * Method to return the default rotation of all new nodes.
	 * The default is 0.
	 * @return the default rotation of all new nodes.
	 */
	private static int cacheNewNodeRotation = tool.prefs.getInt("NewNodeRotation", 0);
	public static int getNewNodeRotation() { return cacheNewNodeRotation; }
	/**
	 * Method to set the default rotation of all new nodes.
	 * @param rot the default rotation of all new nodes.
	 */
	public static void setNewNodeRotation(int rot)
	{
		tool.prefs.putInt("NewNodeRotation", cacheNewNodeRotation = rot);
		flushOptions();
	}

	/**
	 * Method to tell whether new nodes are mirrored in X.
	 * The default is "false".
	 * @return true if new nodes are mirrored in X.
	 */
	private static boolean cacheNewNodeMirrorX = tool.prefs.getBoolean("NewNodeMirrorX", false);
	public static boolean isNewNodeMirrorX() { return cacheNewNodeMirrorX; }
	/**
	 * Method to set whether new nodes are mirrored in X.
	 * @param on true if new nodes are mirrored in X.
	 */
	public static void setNewNodeMirrorX(boolean on)
	{
		tool.prefs.putBoolean("NewNodeMirrorX", cacheNewNodeMirrorX = on);
		flushOptions();
	}

}
