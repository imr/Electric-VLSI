/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NodeProto.java
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
package com.sun.electric.database.prototype;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.NodeUsage;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.user.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * The NodeProto class defines a type of NodeInst.
 * It is an abstract class that can be implemented as PrimitiveNode (for primitives from Technologies)
 * or as Cell (for cells in Libraries).
 * <P>
 * Every node in the database appears as one <I>prototypical</I> object and many <I>instantiative</I> objects.
 * Thus, for a PrimitiveNode such as the CMOS P-transistor there is one object (called a PrimitiveNode, which is a NodeProto)
 * that describes the transistor prototype and there are many objects (called NodeInsts),
 * one for every instance of a transistor that appears in a circuit.
 * Similarly, for every Cell, there is one object (called a Cell, which is a NodeProto)
 * that describes the Cell with everything in it and there are many objects (also called NodeInsts)
 * for every use of that Cell in some other Cell.
 * PrimitiveNodes are statically created and placed in the Technology objects,
 * but complex Cells are created by the tools and placed in Library objects.
 * <P>
 * The basic NodeProto has a list of ports, the bounds, a list of instances of this NodeProto
 * as a NodeInst in other Cells, and much more.
 */
public abstract class AbstractNodeProto extends ElectricObject implements NodeProto
{
	// ------------------------ private data --------------------------

	/** set if nonmanhattan instances shrink */				private static final int NODESHRINK =           01;
	/** set if instances should be expanded */				private static final int WANTNEXPAND =          02;
	/** node function (from efunction.h) */					private static final int NFUNCTION =          0774;
	/** right shift for NFUNCTION */						private static final int NFUNCTIONSH =           2;
	/** set if instances can be wiped */					private static final int ARCSWIPE =          01000;
	/** set if node is to be kept square in size */			private static final int NSQUARE =           02000;
	/** primitive can hold trace information */				private static final int HOLDSTRACE =        04000;
	/** set if this primitive can be zero-sized */			private static final int CANBEZEROSIZE =    010000;
	/** set to erase if connected to 1 or 2 arcs */			private static final int WIPEON1OR2 =       020000;
	/** set if primitive is lockable (cannot move) */		private static final int LOCKEDPRIM =       040000;
	/** set if primitive is selectable by edge, not area */	private static final int NEDGESELECT =     0100000;
	/** set if nonmanhattan arcs on this shrink */			private static final int ARCSHRINK =       0200000;
//	/** set if this cell is open in the explorer tree */	private static final int OPENINEXPLORER =  0400000;
//	/** set if this cell is open in the explorer tree */	private static final int VOPENINEXPLORER =01000000;
	/** set if not used (don't put in menu) */				private static final int NNOTUSED =       02000000;
	/** set if everything in cell is locked */				private static final int NPLOCKED =       04000000;
	/** set if instances in cell are locked */				private static final int NPILOCKED =     010000000;
	/** set if cell is part of a "cell library" */			private static final int INCELLLIBRARY = 020000000;
	/** set if cell is from a technology-library */			private static final int TECEDITCELL =   040000000;

	/** Internal flag bits. */								protected int userBits;

	// ----------------- protected and private methods -----------------

	/**
	 * This constructor should not be called.
	 * Use the subclass factory methods to create Cell or PrimitiveNode objects.
	 */
	protected AbstractNodeProto()
	{
        // nodeprotos are always assumed to be in the database
        setLinked(true);
	}

	// ----------------------- public methods -----------------------

	/**
	 * Method to allow instances of this NodeProto to shrink.
	 * Shrinkage occurs on MOS transistors when they are connected to wires at angles that are not manhattan
	 * (the angle between the transistor and the wire is not a multiple of 90 degrees).
	 * The actual transistor must be shrunk back appropriately to prevent little tabs from emerging at the connection site.
	 * This state is only set on primitive node prototypes.
	 * If the actual NodeInst is to shrink, it must be marked with "setShortened".
	 * Note that shrinkage does not apply if there is no arc connected.
	 */
	public void setCanShrink() { checkChanging(); userBits |= NODESHRINK; }

	/**
	 * Method to prevent instances of this NodeProto from shrinking.
	 * Shrinkage occurs on MOS transistors when they are connected to wires at angles that are not manhattan
	 * (the angle between the transistor and the wire is not a multiple of 90 degrees).
	 * The actual transistor must be shrunk back appropriately to prevent little tabs from emerging at the connection site.
	 * This state is only set on primitive node prototypes.
	 * If the actual NodeInst is to shrink, it must be marked with "setShortened".
	 * Note that shrinkage does not apply if there is no arc connected.
	 */
	public void clearCanShrink() { checkChanging(); userBits &= ~NODESHRINK; }

	/**
	 * Method to tell if instances of this NodeProto can shrink.
	 * Shrinkage occurs on MOS transistors when they are connected to wires at angles that are not manhattan
	 * (the angle between the transistor and the wire is not a multiple of 90 degrees).
	 * The actual transistor must be shrunk back appropriately to prevent little tabs from emerging at the connection site.
	 * This state is only set on primitive node prototypes.
	 * If the actual NodeInst is to shrink, it must be marked with "setShortened".
	 * Note that shrinkage does not apply if there is no arc connected.
	 * @return true if instances of this NodeProto can shrink.
	 */
	public boolean canShrink() { return (userBits & NODESHRINK) != 0; }

	/**
	 * Method to set this NodeProto so that instances of it are "expanded" by when created.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * The setting has no meaning for PrimitiveNode instances.
	 */
	public void setWantExpanded() { checkChanging(); userBits |= WANTNEXPAND; }

	/**
	 * Method to set this NodeProto so that instances of it are "not expanded" by when created.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * The setting has no meaning for PrimitiveNode instances.
	 */
	public void clearWantExpanded() { checkChanging(); userBits &= ~WANTNEXPAND; }

	/**
	 * Method to tell if instances of it are "not expanded" by when created.
	 * Expanded NodeInsts are instances of Cells that show their contents.
	 * The setting has no meaning for PrimitiveNode instances.
	 * @return true if instances of it are "not expanded" by when created.
	 */
	public boolean isWantExpanded() { return (userBits & WANTNEXPAND) != 0; }

	/**
	 * Method to return the function of this NodeProto.
	 * The Function is a technology-independent description of the behavior of this NodeProto.
	 * @return the function of this NodeProto.
	 */
	public PrimitiveNode.Function getFunction() { return PrimitiveNode.Function.UNKNOWN; }

	/**
	 * Method to set this NodeProto so that instances of it are "arc-wipable".
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Therefore, pin prototypes have this state set, and when instances of the
	 * appropriate arc prototypes connect to instances of these pins, they stop being drawn.
	 * It is necessary for the arc prototype to enable wiping (with setWipable).
	 * A NodeInst that becomes wiped out has "setWiped" called.
	 * @see ArcProto#setWipable
	 * @see NodeInst#setWiped
	 */
	public void setArcsWipe() { checkChanging(); userBits |= ARCSWIPE; }

	/**
	 * Method to set this NodeProto so that instances of it are not "arc-wipable".
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Therefore, pin prototypes have this state set, and when instances of the
	 * appropriate arc prototypes connect to instances of these pins, they stop being drawn.
	 * It is necessary for the arc prototype to enable wiping (with setWipable).
	 * A NodeInst that becomes wiped out has "setWiped" called.
	 * @see ArcProto#setWipable
	 * @see NodeInst#setWiped
	 */
	public void clearArcsWipe() { checkChanging(); userBits &= ~ARCSWIPE; }

	/**
	 * Method to tell if instances of this NodeProto are "arc-wipable" by when created.
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Therefore, pin prototypes have this state set, and when instances of the
	 * appropriate arc prototypes connect to instances of these pins, they stop being drawn.
	 * It is necessary for the arc prototype to enable wiping (with setWipable).
	 * A NodeInst that becomes wiped out has "setWiped" called.
	 * @return true if instances of this NodeProto are "arc-wipable" by when created.
	 * @see ArcProto#setWipable
	 * @see NodeInst#setWiped
	 */
	public boolean isArcsWipe() { return (userBits & ARCSWIPE) != 0; }

	/**
	 * Method to set this NodeProto so that instances of it are "square".
	 * Square nodes must have the same X and Y size.
	 * This is useful for round components that really have only one dimension.
	 */
	public void setSquare() { checkChanging(); userBits |= NSQUARE; }

	/**
	 * Method to set this NodeProto so that instances of it are not "square".
	 * Square nodes must have the same X and Y size.
	 * This is useful for round components that really have only one dimension.
	 */
	public void clearSquare() { checkChanging(); userBits &= ~NSQUARE; }

	/**
	 * Method to tell if instances of this NodeProto are square.
	 * Square nodes must have the same X and Y size.
	 * This is useful for round components that really have only one dimension.
	 * @return true if instances of this NodeProto are square.
	 */
	public boolean isSquare() { return (userBits & NSQUARE) != 0; }

	/**
	 * Method to set this NodeProto so that instances of it may hold outline information.
	 * Outline information is an array of coordinates that define the node.
	 * It can be as simple as an opened-polygon that connects the points,
	 * or a serpentine transistor that lays down polysilicon to follow the points.
	 */
	public void setHoldsOutline() { checkChanging(); userBits |= HOLDSTRACE; }

	/**
	 * Method to set this NodeProto so that instances of it may not hold outline information.
	 * Outline information is an array of coordinates that define the node.
	 * It can be as simple as an opened-polygon that connects the points,
	 * or a serpentine transistor that lays down polysilicon to follow the points.
	 */
	public void clearHoldsOutline() { checkChanging(); userBits &= ~HOLDSTRACE; }

	/**
	 * Method to tell if instances of this NodeProto can hold an outline.
	 * Outline information is an array of coordinates that define the node.
	 * It can be as simple as an opened-polygon that connects the points,
	 * or a serpentine transistor that lays down polysilicon to follow the points.
	 * @return true if instances of this NodeProto can hold an outline.
	 */
	public boolean isHoldsOutline() { return (userBits & HOLDSTRACE) != 0; }

	
	/**
	 * Method to set this NodeProto so that it can be zero in size.
	 * The display system uses this to eliminate zero-size nodes that cannot be that way.
	 */
	public void setCanBeZeroSize() { checkChanging(); userBits |= CANBEZEROSIZE; }

	/**
	 * Method to set this NodeProto so that it cannot be zero in size.
	 * The display system uses this to eliminate zero-size nodes that cannot be that way.
	 */
	public void clearCanBeZeroSize() { checkChanging(); userBits &= ~CANBEZEROSIZE; }

	/**
	 * Method to tell if instances of this NodeProto can be zero in size.
	 * The display system uses this to eliminate zero-size nodes that cannot be that way.
	 * @return true if instances of this NodeProto can be zero in size.
	 */
	public boolean isCanBeZeroSize() { return (userBits & CANBEZEROSIZE) != 0; }

	/**
	 * Method to set this NodeProto so that instances of it are wiped when 1 or 2 arcs connect.
	 * This is used in Schematics pins, which are not shown if 1 or 2 arcs connect, but are shown
	 * when standing alone, or when 3 or more arcs make a "T" or other connection to it.
	 */
	public void setWipeOn1or2() { checkChanging(); userBits |= WIPEON1OR2; }

	/**
	 * Method to set this NodeProto so that instances of it are not wiped when 1 or 2 arcs connect.
	 * Only Schematics pins enable this state.
	 */
	public void clearWipeOn1or2() { checkChanging(); userBits &= ~WIPEON1OR2; }

	/**
	 * Method to tell if instances of this NodeProto are wiped when 1 or 2 arcs connect.
	 * This is used in Schematics pins, which are not shown if 1 or 2 arcs connect, but are shown
	 * when standing alone, or when 3 or more arcs make a "T" or other connection to it.
	 * @return true if instances of this NodeProto are wiped when 1 or 2 arcs connect.
	 */
	public boolean isWipeOn1or2() { return (userBits & WIPEON1OR2) != 0; }

	/**
	 * Method to set this NodeProto so that instances of it are locked.
	 * Locked Primitives cannot be created, deleted, or modified.
	 * Typically, array technologies (such as FPGA) have lockable primitives which are used for the fixed part of a design,
	 * and then locked to prevent the customization work from damaging the circuit.
	 */
	public void setLockedPrim() { checkChanging(); userBits |= LOCKEDPRIM; }

	/**
	 * Method to set this NodeProto so that instances of it are not locked.
	 * Locked Primitives cannot be created, deleted, or modified.
	 * Typically, array technologies (such as FPGA) have lockable primitives which are used for the fixed part of a design,
	 * and then locked to prevent the customization work from damaging the circuit.
	 */
	public void clearLockedPrim() { checkChanging(); userBits &= ~LOCKEDPRIM; }

	/**
	 * Method to tell if instances of this NodeProto are loced.
	 * Locked Primitives cannot be created, deleted, or modified.
	 * Typically, array technologies (such as FPGA) have lockable primitives which are used for the fixed part of a design,
	 * and then locked to prevent the customization work from damaging the circuit.
	 * @return true if instances of this NodeProto are loced.
	 */
	public boolean isLockedPrim() { return (userBits & LOCKEDPRIM) != 0; }

	/**
	 * Method to set this NodeProto so that instances of it are selectable only by their edges.
	 * Artwork primitives that are not filled-in or are outlines want edge-selection, instead
	 * of allowing a click anywhere in the bounding box to work.
	 */
	public void setEdgeSelect() { checkChanging(); userBits |= NEDGESELECT; }

	/**
	 * Method to set this NodeProto so that instances of it are not selectable only by their edges.
	 * Artwork primitives that are not filled-in or are outlines want edge-selection, instead
	 * of allowing a click anywhere in the bounding box to work.
	 */
	public void clearEdgeSelect() { checkChanging(); userBits &= ~NEDGESELECT; }

	/**
	 * Method to tell if instances of this NodeProto are selectable on their edges.
	 * Artwork primitives that are not filled-in or are outlines want edge-selection, instead
	 * of allowing a click anywhere in the bounding box to work.
	 * @return true if instances of this NodeProto are selectable on their edges.
	 */
	public boolean isEdgeSelect() { return (userBits & NEDGESELECT) != 0; }

	/**
	 * Method to set this NodeProto so that arcs connected to instances will shrink in nonmanhattan situations.
	 * This happens to pins where any combination of multiple arcs in angles that are not increments of 90 degrees
	 * will cause tabs to emerge at the connection site.
	 */
	public void setArcsShrink() { checkChanging(); userBits |= ARCSHRINK; }

	/**
	 * Method to set this NodeProto so that arcs connected to instances will not shrink in nonmanhattan situations.
	 * This happens to pins where any combination of multiple arcs in angles that are not increments of 90 degrees
	 * will cause tabs to emerge at the connection site.
	 */
	public void clearArcsShrink() { checkChanging(); userBits &= ~ARCSHRINK; }

	/**
	 * Method to tell if instances of this NodeProto cause arcs to shrink in nonmanhattan situations.
	 * This happens to pins where any combination of multiple arcs in angles that are not increments of 90 degrees
	 * will cause tabs to emerge at the connection site.
	 * @return true if instances of this NodeProto cause arcs to shrink in nonmanhattan situations.
	 */
	public boolean isArcsShrink() { return (userBits & ARCSHRINK) != 0; }

	/**
	 * Method to set this NodeProto so that everything inside of it is locked.
	 * Locked instances cannot be moved or deleted.
	 * This only applies to Cells.
	 */
	public void setAllLocked() { checkChanging(); userBits |= NPLOCKED; }

	/**
	 * Method to set this NodeProto so that everything inside of it is not locked.
	 * Locked instances cannot be moved or deleted.
	 * This only applies to Cells.
	 */
	public void clearAllLocked() { checkChanging(); userBits &= ~NPLOCKED; }

	/**
	 * Method to tell if the contents of this NodeProto are locked.
	 * Locked instances cannot be moved or deleted.
	 * This only applies to Cells.
	 * @return true if the contents of this NodeProto are locked.
	 */
	public boolean isAllLocked() { return (userBits & NPLOCKED) != 0; }

	/**
	 * Method to set this NodeProto so that all instances inside of it are locked.
	 * Locked instances cannot be moved or deleted.
	 * This only applies to Cells.
	 */
	public void setInstancesLocked() { checkChanging(); userBits |= NPILOCKED; }

	/**
	 * Method to set this NodeProto so that all instances inside of it are not locked.
	 * Locked instances cannot be moved or deleted.
	 * This only applies to Cells.
	 */
	public void clearInstancesLocked() { checkChanging(); userBits &= ~NPILOCKED; }

	/**
	 * Method to tell if the sub-instances in this NodeProto are locked.
	 * Locked instances cannot be moved or deleted.
	 * This only applies to Cells.
	 * @return true if the sub-instances in this NodeProto are locked.
	 */
	public boolean isInstancesLocked() { return (userBits & NPILOCKED) != 0; }

	/**
	 * Method to set this NodeProto so that it is not used.
	 * Unused nodes do not appear in the component menus and cannot be created by the user.
	 * The state is useful for hiding primitives that the user should not use.
	 */
	public void setNotUsed() { checkChanging(); userBits |= NNOTUSED; }

	/**
	 * Method to set this NodeProto so that it is used.
	 * Unused nodes do not appear in the component menus and cannot be created by the user.
	 * The state is useful for hiding primitives that the user should not use.
	 */
	public void clearNotUsed() { checkChanging(); userBits &= ~NNOTUSED; }

	/**
	 * Method to tell if this NodeProto is used.
	 * Unused nodes do not appear in the component menus and cannot be created by the user.
	 * The state is useful for hiding primitives that the user should not use.
	 * @return true if this NodeProto is used.
	 */
	public boolean isNotUsed() { return (userBits & NNOTUSED) != 0; }

	/**
	 * Method to set this NodeProto so that it is part of a cell library.
	 * Cell libraries are simply libraries that contain standard cells but no hierarchy
	 * (as opposed to libraries that define a complete circuit).
	 * Certain commands exclude facets from cell libraries, so that the actual circuit hierarchy can be more clearly seen.
	 */
	public void setInCellLibrary() { checkChanging(); userBits |= INCELLLIBRARY; }

	/**
	 * Method to set this NodeProto so that it is not part of a cell library.
	 * Cell libraries are simply libraries that contain standard cells but no hierarchy
	 * (as opposed to libraries that define a complete circuit).
	 * Certain commands exclude facets from cell libraries, so that the actual circuit hierarchy can be more clearly seen.
	 */
	public void clearInCellLibrary() { checkChanging(); userBits &= ~INCELLLIBRARY; }

	/**
	 * Method to tell if this NodeProto is part of a cell library.
	 * Cell libraries are simply libraries that contain standard cells but no hierarchy
	 * (as opposed to libraries that define a complete circuit).
	 * Certain commands exclude facets from cell libraries, so that the actual circuit hierarchy can be more clearly seen.
	 * @return true if this NodeProto is part of a cell library.
	 */
	public boolean isInCellLibrary() { return (userBits & INCELLLIBRARY) != 0; }

	/**
	 * Method to set this NodeProto so that it is part of a technology library.
	 * Technology libraries are those libraries that contain Cells with
	 * graphical descriptions of the nodes, arcs, and layers of a technology.
	 */
	public void setInTechnologyLibrary() { checkChanging(); userBits |= TECEDITCELL; }

	/**
	 * Method to set this NodeProto so that it is not part of a technology library.
	 * Technology libraries are those libraries that contain Cells with
	 * graphical descriptions of the nodes, arcs, and layers of a technology.
	 */
	public void clearInTechnologyLibrary() { checkChanging(); userBits &= ~TECEDITCELL; }

	/**
	 * Method to tell if this NodeProto is part of a Technology Library.
	 * Technology libraries are those libraries that contain Cells with
	 * graphical descriptions of the nodes, arcs, and layers of a technology.
	 * @return true if this NodeProto is part of a Technology Library.
	 */
	public boolean isInTechnologyLibrary() { return (userBits & TECEDITCELL) != 0; }

	/**
	 * Method to tell if this NodeProto is icon cell which is a part of multi-part icon.
	 * @return true if this NodeProto is part of multi-part icon.
	 */
	public boolean isMultiPartIcon() { return false; }

	/**
	 * Abstract method to return the default rotation for new instances of this NodeProto.
	 * @return the angle, in tenth-degrees to use when creating new NodeInsts of this NodeProto.
	 * If the value is 3600 or greater, it means that X should be mirrored.
	 */
	public int getDefPlacementAngle()
	{
		int defAngle = User.getNewNodeRotation();
		Variable var = getVar(User.PLACEMENT_ANGLE, Integer.class);
		if (var != null)
		{
			Integer rot = (Integer)var.getObject();
			defAngle = rot.intValue();
		}
		return defAngle;
	}

	/**
	 * Abstract method to return the default width of this NodeProto.
	 * Cells return the actual width of the contents.
	 * PrimitiveNodes return the default width of new instances of this NodeProto.
	 * @return the width to use when creating new NodeInsts of this NodeProto.
	 */
	public abstract double getDefWidth();

	/**
	 * Abstract method to return the default height of this NodeProto.
	 * Cells return the actual height of the contents.
	 * PrimitiveNodes return the default height of new instances of this NodeProto.
	 * @return the height to use when creating new NodeInsts of this NodeProto.
	 */
	public abstract double getDefHeight();

	/**
	 * Method to size offset of this Cell.
	 * @return the size offset of this Cell.  It is always zero for cells.
	 */
	public abstract SizeOffset getProtoSizeOffset();

	/**
	 * Abstract method to return the Technology to which this NodeProto belongs.
	 * For Cells, the Technology varies with the View and contents.
	 * For PrimitiveNodes, the Technology is simply the one that owns it.
	 * @return the Technology associated with this NodeProto.
	 */
	public abstract Technology getTechnology();

	/**
	 * Method to determine whether this NodeProto is an icon Cell.
	 * overriden in Cell
	 * @return true if this NodeProto is an icon  Cell.
	 */
	public boolean isIcon() { return false; }

	/**
	 * Method to determine whether this NodeProto is an icon of another Cell.
	 * overriden in Cell
	 * @param cell the other cell which this may be an icon of.
	 * @return true if this NodeProto is an icon of that other Cell.
	 */
	public boolean isIconOf(Cell cell) { return false; }

	/**
	 * Abstract method to describe this NodeProto as a string.
	 * PrimitiveNodes may prepend their Technology name if it is
	 * not the current technology (for example, "mocmos:N-Transistor").
	 * Cells may prepend their Library if it is not the current library,
	 * and they will include view and version information
	 * (for example: "Wires:wire100{ic}").
	 * @return a String describing this NodeProto.
	 */
	public abstract String describe();

	/**
	 * Low-level method to get the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @return the "user bits".
	 */
	public int lowLevelGetUserbits() { return userBits; }

	/**
	 * Low-level method to set the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @param userBits the new "user bits".
	 */
	public void lowLevelSetUserbits(int userBits) { checkChanging(); this.userBits = userBits; }

	/**
	 * Returns a printable version of this NodeProto.
	 * @return a printable version of this NodeProto.
	 */
	public String toString()
	{
		return "NodeProto " + describe();
	}
}
