/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Changes.java
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
package com.sun.electric.database.change;

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
import com.sun.electric.tool.Tool;


/**
 * This interface defines changes that are made to the database.
 */
public interface Changes
{
	/**
	 * Method to initialize a tool.
	 */
	public void init();

	/**
	 * Method to make a request of a tool (not used).
	 * @param cmd the command request.
	 */
	public void request(String cmd);

	/**
	 * Method to examine a cell because it has changed.
	 * @param cell the Cell to examine.
	 */
	public void examineCell(Cell cell);

	/**
	 * Method to give a tool a chance to run.
	 */
	public void slice();

	/**
	 * Method to announce the start of a batch of changes.
	 * @param tool the tool that generated the changes.
	 * @param undoRedo true if these changes are from an undo or redo command.
	 */
	public void startBatch(Tool tool, boolean undoRedo);

   /**
     * Method to annonunce database changes of a Job.
     * @param oldSnapshot database snapshot before Job.
     * @param newSnapshot database snapshot after Job and constraint propagation.
     * @param undoRedo true if Job was Undo/Redo job.
     */
    public void endBatch(Snapshot oldSnapshot, Snapshot newSnapshot, boolean undoRedo);

	/**
	 * Method to announce a change to a NodeInst.
	 * @param ni the NodeInst that was changed.
	 * @param oD the old contents of the NodeInst.
	 */
	public void modifyNodeInst(NodeInst ni, ImmutableNodeInst oD);

	/**
	 * Method to announce a change to an ArcInst.
	 * @param ai the ArcInst that changed.
     * @param oD the old contents of the ArcInst.
	 */
	public void modifyArcInst(ArcInst ai, ImmutableArcInst oD);

	/**
	 * Method to announce a change to an Export.
	 * @param pp the Export that moved.
	 * @param oldD the old contents of the Export.
	 */
	public void modifyExport(Export pp, ImmutableExport oldD);

	/**
	 * Method to announce a change to a Cell.
	 * @param cell the Cell that was changed.
	 * @param oD the old contents of the Cell.
	 */
	public void modifyCell(Cell cell, ImmutableCell oD);

	/**
	 * Method to announce a move of a Cell int CellGroup.
	 * @param cell the cell that was moved.
	 * @param oCellGroup the old CellGroup of the Cell.
	 */
	public void modifyCellGroup(Cell cell, Cell.CellGroup oCellGroup);

	/**
	 * Method to announce a change to a Library.
	 * @param lib the Library that was changed.
	 * @param oldD the old contents of the Library.
	 */
	public void modifyLibrary(Library lib, ImmutableLibrary oldD);

	/**
	 * Method to announce the creation of a new ElectricObject.
	 * @param obj the ElectricObject that was just created.
	 */
	public void newObject(ElectricObject obj);

	/**
	 * Method to announce the deletion of an ElectricObject.
	 * @param obj the ElectricObject that was just deleted.
	 */
	public void killObject(ElectricObject obj);

	/**
	 * Method to announce the renaming of an ElectricObject.
	 * @param obj the ElectricObject that was renamed.
	 * @param oldName the former name of that ElectricObject.
	 */
	public void renameObject(ElectricObject obj, Object oldName);

	/**
	 * Method to announce that a Library has been read.
	 * @param lib the Library that was read.
	 */
	public void readLibrary(Library lib);

	/**
	 * Method to announce that a Library is about to be erased.
	 * @param lib the Library that will be erased.
	 */
	public void eraseLibrary(Library lib);

	/**
	 * Method to announce that a Library is about to be written to disk.
	 * @param lib the Library that will be saved.
	 */
	public void writeLibrary(Library lib);
}
