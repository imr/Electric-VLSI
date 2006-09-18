/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Constraints.java
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
package com.sun.electric.database.constraint;

import com.sun.electric.database.IdMapper;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableCell;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.ImmutableLibrary;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;

/**
 * Top-level class to handle constraints.
 * The methods here are overridden by the actual constraint solver.
 * At this time, there is only one solver: Layout.
 */
public abstract class Constraints
{
	private static Constraints curConstraint = new Layout();

//	/**
//	 * Method to set a new current constraint system.
//	 * @param con the constraint system to become the current one.
//	 */
//	public static void setCurrent(Constraints con) { curConstraint = con; }

	/**
	 * Method to return the current constraint system.
	 * return the current constraint system.
	 */
	public static Constraints getCurrent() { return curConstraint; }

	/**
	 * Method to initialize a constraint system.
	 */
	public void init() {}
	/**
	 * Method to make a request of a constraint system (not used).
	 * @param cmd the command request.
	 */
	public void request(String cmd) {}
	/**
	 * Method to examine a cell because it has changed.
	 * @param cell the Cell to examine.
	 */
	public void examineCell(Cell cell) {}
	/**
	 * Method to give a constraint system a chance to run.
	 */
	public void slice() {}

	/**
	 * Method to handle the start of a batch of changes.
     * @param initialSnapshot snapshot before job changes.
	 */
	public void startBatch(Snapshot initialSnapshot) {}
	/**
	 * Method to handle the end of a batch of changes.
	 */
	public abstract void endBatch(String userName);

	/**
	 * Method to announce a change to a NodeInst.
	 * @param ni the NodeInst that was changed.
	 * @param oD the old contents of the NodeInst.
	 */
	public void modifyNodeInst(NodeInst ni, ImmutableNodeInst oD) {}
	/**
	 * Method to handle a change to an ArcInst.
	 * @param ai the ArcInst that changed.
     * @param oD the old contents of the ArcInst.
	 */
	public void modifyArcInst(ArcInst ai, ImmutableArcInst oD) {}
	/**
	 * Method to handle a change to an Export.
	 * @param pp the Export that moved.
	 * @param oD the old contents of the Export.
	 */
	public void modifyExport(Export pp, ImmutableExport oD) {}
	/**
	 * Method to announce a change to a Cell.
	 * @param cell the Cell that was changed.
	 * @param oD the old contents of the Cell.
	 */
	public void modifyCell(Cell cell, ImmutableCell oD) {}
	/**
	 * Method to announce a move of a Cell int CellGroup.
	 * @param cell the cell that was moved.
	 * @param oCellGroup the old CellGroup of the Cell.
	 */
	public void modifyCellGroup(Cell cell, Cell.CellGroup  oCellGroup) {}
	/**
	 * Method to announce a change to a Library.
	 * @param lib the Library that was changed.
	 * @param oldD the old contents of the Library.
	 */
	public void modifyLibrary(Library lib, ImmutableLibrary oldD) {}

	/**
	 * Method to handle the creation of a new ElectricObject.
	 * @param obj the ElectricObject that was just created.
	 */
	public void newObject(ElectricObject obj) {}
	/**
	 * Method to handle the deletion of an ElectricObject.
	 * @param obj the ElectricObject that was just deleted.
	 */
	public void killObject(ElectricObject obj) {}
	/**
	 * Method to handle the renaming of an ElectricObject.
	 * @param obj the ElectricObject that was renamed.
	 * @param oldName the former name of that ElectricObject.
	 */
	public void renameObject(ElectricObject obj, Object oldName) {}
	/**
	 * Method to request that an object be redrawn.
	 * @param obj the ElectricObject to be redrawn.
	 */
	public void redrawObject(ElectricObject obj) {}

	/**
	 * Method to announce that a Library has been read.
	 * @param lib the Library that was read.
	 */
	public void readLibrary(Library lib) {}
	/**
	 * Method to announce that a Library is about to be erased.
	 * @param lib the Library that will be erased.
	 */
	public void eraseLibrary(Library lib) {}
	/**
	 * Method to announce that a Library is about to be written to disk.
	 * @param lib the Library that will be saved.
	 */
	public void writeLibrary(Library lib) {}
    /**
     * Method to announce than Ids were renamed.
     * @param idMapper mapper from old Ids to new Ids.
     */
    public void renameIds(IdMapper idMapper) {}
    
}
