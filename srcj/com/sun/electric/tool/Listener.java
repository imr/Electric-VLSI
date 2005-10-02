/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Listener.java
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
package com.sun.electric.tool;

import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableElectricObject;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.change.Changes;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ImmutableTextDescriptor;
import com.sun.electric.database.variable.ElectricObject;

import java.util.Collection;

/**
 * This class represents a Listener - a Tool which can listen to Changes.
 */
public class Listener extends Tool implements Changes
{
	/**
	 * The constructor for Listener is only called by subclasses.
	 * @param toolName the name of this listener.
	 */
	protected Listener(String toolName) { super(toolName); }

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
	 * @param tool the tool that generated the changes.
	 * @param undoRedo true if these changes are from an undo or redo command.
	 */
	public void startBatch(Tool tool, boolean undoRedo) {}
	/**
	 * Method to handle the end of a batch of changes.
	 */
	public void endBatch() {}

	/**
	 * Method to handle a change to a NodeInst.
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
	 * @param oldPi the old PortInst on which it resided.
	 */
	public void modifyExport(Export pp, PortInst oldPi) {}
	/**
	 * Method to announce a move of a Cell int CellGroup.
	 * @param cell the cell that was moved.
	 * @param oCellGroup the old CellGroup of the Cell.
	 */
	public void modifyCellGroup(Cell cell, Cell.CellGroup oCellGroup) {}
	/**
	 * Method to handle a change to a TextDescriptor.
	 * @param obj the ElectricObject on which the TextDescriptor resides.
     * @param varName name of variable or special name.
     * @param oldDescriptor old text descriptor.
	 */
	public void modifyTextDescript(ElectricObject obj, String varName, ImmutableTextDescriptor oldDescriptor) {}

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
	 * Method to handle the deletion of an Export.
	 * @param pp the Export that was just deleted.
	 * @param oldPortInsts the PortInsts that were on that Export (?).
	 */
	public void killExport(Export pp, Collection oldPortInsts) {}
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
	 * Method to handle a change of object Variables.
	 * @param obj the ElectricObject on which Variables changed.
	 * @param oldImmutable the old Variables.
	 */
	public void modifyVariables(ElectricObject obj, ImmutableElectricObject oldImmutable) {}

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
	 * The method should always be called inside of a Job so that the
	 * implementation can make changes to the database.
	 * @param lib the Library that will be saved.
	 */
	public void writeLibrary(Library lib) {}

}
