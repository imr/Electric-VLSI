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
	private Preferences prefs = Preferences.userNodeForPackage(getClass());

	/**
	 * The constructor sets up the User tool.
	 */
	private User()
	{
		super("user");
	}

	/**
	 * Routine to initialize the User Interface tool.
	 */
	public void init()
	{
		// the user interface tool is always on
		setOn();
		setIncremental();

		// initialize the display
		TopLevel.Initialize();
		PaletteFrame pf = TopLevel.getPaletteFrame();

		pf.loadForTechnology();
	}

	/**
	 * Routine to return the "current" NodeProto, as maintained by the user interface.
	 * @return the "current" NodeProto, as maintained by the user interface.
	 */
	public NodeProto getCurrentNodeProto() { return currentNodeProto; }

	/**
	 * Routine to set the "current" NodeProto, as maintained by the user interface.
	 * @param np the new "current" NodeProto.
	 */
	public void setCurrentNodeProto(NodeProto np) { currentNodeProto = np; }

	/**
	 * Routine to return the "current" ArcProto, as maintained by the user interface.
	 * The current ArcProto is highlighted with a bolder red border in the component menu on the left.
	 * @return the "current" ArcProto, as maintained by the user interface.
	 */
	public ArcProto getCurrentArcProto() { return currentArcProto; }

	/**
	 * Routine to set the "current" ArcProto, as maintained by the user interface.
	 * The current ArcProto is highlighted with a bolder red border in the component menu on the left.
	 * @param np the new "current" ArcProto.
	 */
	public void setCurrentArcProto(ArcProto ap) { currentArcProto = ap; }

	/**
	 * Daemon routine called when an object is to be redrawn.
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
	 * Routine to recurse flag all windows showing a cell to redraw.
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
	 * Daemon routine called when a batch of changes ends.
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

		// update any Get-Info dialogs
		GetInfoNode.load();
		GetInfoArc.load();
		GetInfoExport.load();
		GetInfoText.load();
		GetInfoMulti.load();
		Attributes.load();
	}

	/**
	 * Routine to force all User Preferences to be saved.
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
	 * Routine to tell whether generated icons should have leads drawn.
	 * The default is "true".
	 * @return true if generated icons should have leads drawn.
	 */
	public static boolean isIconGenDrawLeads() { return tool.prefs.getBoolean("IconGenDrawLeads", true); }
	/**
	 * Routine to set whether generated icons should have leads drawn.
	 * @param on true if generated icons should have leads drawn.
	 */
	public static void setIconGenDrawLeads(boolean on) { tool.prefs.putBoolean("IconGenDrawLeads", on);   flushOptions(); }

	/**
	 * Routine to tell whether generated icons should have a body drawn.
	 * The body is just a rectangle.
	 * The default is "true".
	 * @return true if generated icons should have a body drawn.
	 */
	public static boolean isIconGenDrawBody() { return tool.prefs.getBoolean("IconGenDrawBody", true); }
	/**
	 * Routine to set whether generated icons should have a body drawn.
	 * The body is just a rectangle.
	 * @param on true if generated icons should have a body drawn.
	 */
	public static void setIconGenDrawBody(boolean on) { tool.prefs.putBoolean("IconGenDrawBody", on);   flushOptions(); }

	/**
	 * Routine to tell whether generated icons should reverse the order of exports.
	 * Normally, exports are drawn top-to-bottom alphabetically.
	 * The default is "false".
	 * @return true if generated icons should reverse the order of exports.
	 */
	public static boolean isIconGenReverseExportOrder() { return tool.prefs.getBoolean("IconGenReverseExportOrder", false); }
	/**
	 * Routine to set whether generated icons should reverse the order of exports.
	 * Normally, exports are drawn top-to-bottom alphabetically.
	 * @param on true if generated icons should reverse the order of exports.
	 */
	public static void setIconGenReverseExportOrder(boolean on) { tool.prefs.putBoolean("IconGenReverseExportOrder", on);   flushOptions(); }

	/**
	 * Routine to tell where Input ports should go on generated icons.
	 * @return information about where Input ports should go on generated icons.
	 * 0: left (the default)   1: right   2: top   3: bottom
	 */
	public static int getIconGenInputSide() { return tool.prefs.getInt("IconGenInputSide", 0); }
	/**
	 * Routine to set where Input ports should go on generated icons.
	 * @param side information about where Input ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static void setIconGenInputSide(int side) { tool.prefs.putInt("IconGenInputSide", side);   flushOptions(); }

	/**
	 * Routine to tell where Output ports should go on generated icons.
	 * @return information about where Output ports should go on generated icons.
	 * 0: left   1: right (the default)   2: top   3: bottom
	 */
	public static int getIconGenOutputSide() { return tool.prefs.getInt("IconGenOutputSide", 1); }
	/**
	 * Routine to set where Output ports should go on generated icons.
	 * @param side information about where Output ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static void setIconGenOutputSide(int side) { tool.prefs.putInt("IconGenOutputSide", side);   flushOptions(); }

	/**
	 * Routine to tell where Bidirectional ports should go on generated icons.
	 * @return information about where Bidirectional ports should go on generated icons.
	 * 0: left   1: right   2: top (the default)   3: bottom
	 */
	public static int getIconGenBidirSide() { return tool.prefs.getInt("IconGenBidirSide", 2); }
	/**
	 * Routine to set where Bidirectional ports should go on generated icons.
	 * @param side information about where Bidirectional ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static void setIconGenBidirSide(int side) { tool.prefs.putInt("IconGenBidirSide", side);   flushOptions(); }

	/**
	 * Routine to tell where Power ports should go on generated icons.
	 * @return information about where Power ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom (the default)
	 */
	public static int getIconGenPowerSide() { return tool.prefs.getInt("IconGenPowerSide", 3); }
	/**
	 * Routine to set where Power ports should go on generated icons.
	 * @param side information about where Power ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static void setIconGenPowerSide(int side) { tool.prefs.putInt("IconGenPowerSide", side);   flushOptions(); }

	/**
	 * Routine to tell where Ground ports should go on generated icons.
	 * @return information about where Ground ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom (the default)
	 */
	public static int getIconGenGroundSide() { return tool.prefs.getInt("IconGenGroundSide", 3); }
	/**
	 * Routine to set where Ground ports should go on generated icons.
	 * @param side information about where Ground ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static void setIconGenGroundSide(int side) { tool.prefs.putInt("IconGenGroundSide", side);   flushOptions(); }

	/**
	 * Routine to tell where Clock ports should go on generated icons.
	 * @return information about where Clock ports should go on generated icons.
	 * 0: left (the default)   1: right   2: top   3: bottom
	 */
	public static int getIconGenClockSide() { return tool.prefs.getInt("IconGenClockSide", 0); }
	/**
	 * Routine to set where Clock ports should go on generated icons.
	 * @param side information about where Clock ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static void setIconGenClockSide(int side) { tool.prefs.putInt("IconGenClockSide", side);   flushOptions(); }

	/**
	 * Routine to tell where exports should appear along the leads in generated icons.
	 * @return information about where exports should appear along the leads in generated icons.
	 * 0: on the body   1: (the default) at the end of the lead   2: in the middle of the lead
	 */
	public static int getIconGenExportLocation() { return tool.prefs.getInt("IconGenExportLocation", 1); }
	/**
	 * Routine to set how exports should appear along the leads in generated icons.
	 * @param loc information about where exports should appear along the leads in generated icons.
	 * 0: on the body   1: at the end of the lead   2: in the middle of the lead
	 */
	public static void setIconGenExportLocation(int loc) { tool.prefs.putInt("IconGenExportLocation", loc);   flushOptions(); }

	/**
	 * Routine to tell how the text should appear in generated icons.
	 * @return information about how the text should appear in generated icons.
	 * 0: (the default) centered at the export location
	 * 1: pointing inward from the export location
	 * 2: pointing outward from the export location
	 */
	public static int getIconGenExportStyle() { return tool.prefs.getInt("IconGenExportStyle", 0); }
	/**
	 * Routine to set how the text should appear in generated icons.
	 * @param style information about how the text should appear in generated icons.
	 * 0: centered at the export location
	 * 1: pointing inward from the export location
	 * 2: pointing outward from the export location
	 */
	public static void setIconGenExportStyle(int style) { tool.prefs.putInt("IconGenExportStyle", style);   flushOptions(); }

	/**
	 * Routine to tell how exports should be constructed in generated icons.
	 * @return information about how exports should be constructed in generated icons.
	 * 0: (the default) use Generic:Universal-Pins for exports (can connect to ANYTHING).
	 * 1: use Schematic:Bus-Pins for exports (can connect to schematic busses or wires).
	 */
	public static int getIconGenExportTech() { return tool.prefs.getInt("IconGenExportTech", 0); }
	/**
	 * Routine to set how exports should be constructed in generated icons.
	 * @param t information about how exports should be constructed in generated icons.
	 * 0: use Generic:Universal-Pins for exports (can connect to ANYTHING).
	 * 1: use Schematic:Bus-Pins for exports (can connect to schematic busses or wires).
	 */
	public static void setIconGenExportTech(int t) { tool.prefs.putInt("IconGenExportTech", t);   flushOptions(); }

	/**
	 * Routine to tell where to place an instance of the generated icons in the original schematic.
	 * @return information about where to place an instance of the generated icons in the original schematic
	 * 0: (the default) in the upper-right corner.
	 * 1: in the upper-left corner.
	 * 2: in the lower-right corner.
	 * 3: in the lower-left corner.
	 */
	public static int getIconGenInstanceLocation() { return tool.prefs.getInt("IconGenInstanceLocation", 0); }
	/**
	 * Routine to set where to place an instance of the generated icons in the original schematic.
	 * @param loc information about where to place an instance of the generated icons in the original schematic.
	 * 0: in the upper-right corner.
	 * 1: in the upper-left corner.
	 * 2: in the lower-right corner.
	 * 3: in the lower-left corner.
	 */
	public static void setIconGenInstanceLocation(int loc) { tool.prefs.putInt("IconGenInstanceLocation", loc);   flushOptions(); }

	/**
	 * Routine to tell how long to make leads in generated icons.
	 * @return information about how long to make leads in generated icons (the default is 2).
	 */
	public static float getIconGenLeadLength() { return tool.prefs.getFloat("IconGenLeadLength", 2.0f); }
	/**
	 * Routine to set how long to make leads in generated icons.
	 * @param len how long to make leads in generated icons.
	 */
	public static void setIconGenLeadLength(float len) { tool.prefs.putFloat("IconGenLeadLength", len);   flushOptions(); }

	/**
	 * Routine to tell how far apart to space leads in generated icons.
	 * @return information about how far apart to space leads in generated icons (the default is 2).
	 */
	public static float getIconGenLeadSpacing() { return tool.prefs.getFloat("IconGenLeadSpacing", 2.0f); }
	/**
	 * Routine to set how far apart to space leads in generated icons.
	 * @param dist how far apart to space leads in generated icons.
	 */
	public static void setIconGenLeadSpacing(float dist) { tool.prefs.putFloat("IconGenLeadSpacing", dist);   flushOptions(); }

	/****************************** MISCELLANEOUS PREFERENCES ******************************/

	/**
	 * Routine to tell whether to switch technologies automatically when changing the current Cell.
	 * Switching technologies means that the component menu updates to the new primitive set.
	 * The default is "true".
	 * @return true if the system should switch technologies automatically when changing the current Cell.
	 */
	public static boolean isAutoTechnologySwitch() { return tool.prefs.getBoolean("AutoTechnologySwitch", true); }
	/**
	 * Routine to set whether to switch technologies automatically when changing the current Cell.
	 * Switching technologies means that the component menu updates to the new primitive set.
	 * @param on true if the system should switch technologies automatically when changing the current Cell.
	 */
	public static void setAutoTechnologySwitch(boolean on) { tool.prefs.putBoolean("AutoTechnologySwitch", on);   flushOptions(); }

	/**
	 * Routine to tell whether to place a Cell-Center primitive in every newly created Cell.
	 * The default is "true".
	 * @return true if the system should place a Cell-Center primitive in every newly created Cell.
	 */
	public static boolean isPlaceCellCenter() { return tool.prefs.getBoolean("PlaceCellCenter", true); }
	/**
	 * Routine to set whether to place a Cell-Center primitive in every newly created Cell.
	 * @param on true if the system should place a Cell-Center primitive in every newly created Cell.
	 */
	public static void setPlaceCellCenter(boolean on) { tool.prefs.putBoolean("PlaceCellCenter", on);   flushOptions(); }

	/**
	 * Routine to tell whether to check Cell dates when placing instances.
	 * This is not currently implemented.
	 * The default is "false".
	 * @return true if the system should check Cell dates when placing instances.
	 */
	public static boolean isCheckCellDates() { return tool.prefs.getBoolean("CheckCellDates", false); }
	/**
	 * Routine to set whether to check Cell dates when placing instances.
	 * This is not currently implemented.
	 * @param on true if the system should check Cell dates when placing instances.
	 */
	public static void setCheckCellDates(boolean on) { tool.prefs.putBoolean("CheckCellDates", on);   flushOptions(); }


	/**
	 * Routine to tell whether locked primitives can be modified.
	 * Locked primitives occur in array-technologies such as FPGA.
	 * The default is "false".
	 * @return true if the locked primitives cannot be modified.
	 */
	public static boolean isDisallowModificationLockedPrims() { return tool.prefs.getBoolean("DisallowModificationLockedPrims", false); }
	/**
	 * Routine to set whether locked primitives can be modified.
	 * @param on true if locked primitives cannot be modified.
	 */
	public static void setDisallowModificationLockedPrims(boolean on) { tool.prefs.putBoolean("DisallowModificationLockedPrims", on);   flushOptions(); }

	/**
	 * Routine to tell whether to move objects after duplicating them.
	 * The default is "true".
	 * @return true if the system should move objects after duplicating them.
	 */
	public static boolean isMoveAfterDuplicate() { return tool.prefs.getBoolean("MoveAfterDuplicate", true); }
	/**
	 * Routine to set whether to move objects after duplicating them.
	 * @param on true if the system should move objects after duplicating them.
	 */
	public static void setMoveAfterDuplicate(boolean on) { tool.prefs.putBoolean("MoveAfterDuplicate", on);   flushOptions(); }

	/**
	 * Routine to tell whether Duplicate/Paste/Array of NodeInst copies exports.
	 * The default is "false".
	 * @return true if the system copies exports when doing a Duplicate/Paste/Array of NodeInst.
	 */
	public static boolean isDupCopiesExports() { return tool.prefs.getBoolean("DupCopiesExports", false); }
	/**
	 * Routine to set whether Duplicate/Paste/Array of NodeInst copies exports.
	 * @param on true if the system copies exports when doing a Duplicate/Paste/Array of NodeInst.
	 */
	public static void setDupCopiesExports(boolean on) { tool.prefs.putBoolean("DupCopiesExports", on);   flushOptions(); }

	/**
	 * Routine to return the default rotation of all new nodes.
	 * The default is 0.
	 * @return truethe  default rotation of all new nodes.
	 */
	public static int getNewNodeRotation() { return tool.prefs.getInt("NewNodeRotation", 0); }
	/**
	 * Routine to set the default rotation of all new nodes.
	 * @param rot the default rotation of all new nodes.
	 */
	public static void setNewNodeRotation(int rot) { tool.prefs.putInt("NewNodeRotation", rot);   flushOptions(); }

	/**
	 * Routine to tell whether new nodes are mirrored in X.
	 * The default is "false".
	 * @return true if new nodes are mirrored in X.
	 */
	public static boolean isNewNodeMirrorX() { return tool.prefs.getBoolean("NewNodeMirrorX", false); }
	/**
	 * Routine to set whether new nodes are mirrored in X.
	 * @param on true if new nodes are mirrored in X.
	 */
	public static void setNewNodeMirrorX(boolean on) { tool.prefs.putBoolean("NewNodeMirrorX", on);   flushOptions(); }

	/**
	 * Routine to tell whether cell instances are all be easy-to-select.
	 * The default is "true".
	 * @return true if cell instances are all be easy-to-select.
	 */
	public static boolean isEasySelectionOfCellInstances() { return tool.prefs.getBoolean("EasySelectionOfCellInstances", true); }
	/**
	 * Routine to set whether cell instances are all be easy-to-select.
	 * @param on true if cell instances are all to be easy-to-select.
	 */
	public static void setEasySelectionOfCellInstances(boolean on) { tool.prefs.putBoolean("EasySelectionOfCellInstances", on);   flushOptions(); }

	private static boolean annotationTextInvalid = true;
	private static boolean annotationTextHardCache = false;

	/**
	 * Routine to tell whether annotation text is easy-to-select.
	 * The default is "true".
	 * @return true if annotation text is easy-to-select.
	 */
	public static boolean isEasySelectionOfAnnotationText()
	{
		if (annotationTextInvalid)
		{
			annotationTextHardCache = tool.prefs.getBoolean("EasySelectionOfAnnotationText", true);
			annotationTextInvalid = false;
		}
		return annotationTextHardCache;
	}
	/**
	 * Routine to set whether annotation text is easy-to-select.
	 * @param on true if annotation text is easy-to-select.
	 */
	public static void setEasySelectionOfAnnotationText(boolean on)
	{
		tool.prefs.putBoolean("EasySelectionOfAnnotationText", annotationTextHardCache = on);
		flushOptions();
	}

	/**
	 * Routine to tell whether dragging a selection rectangle must completely encose objects in order to select them.
	 * The default is "false", which means that the selection rectangle need only touch an object in order to select it.
	 * @return true if dragging a selection rectangle must completely encose objects in order to select them.
	 */
	public static boolean isDraggingMustEncloseObjects() { return tool.prefs.getBoolean("DraggingMustEncloseObjects", false); }
	/**
	 * Routine to set whether dragging a selection rectangle must completely encose objects in order to select them.
	 * @param on true if dragging a selection rectangle must completely encose objects in order to select them.
	 */
	public static void setDraggingMustEncloseObjects(boolean on) { tool.prefs.putBoolean("DraggingMustEncloseObjects", on);   flushOptions(); }

}
