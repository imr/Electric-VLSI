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

import com.sun.electric.database.change.Changes;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Tool;

import java.util.Collection;

/**
 * Top-level class to handle constraints.
 * The methods here are overridden by the actual constraint solver.
 * At this time, there is only one solver: Layout.
 */
public class Constraints implements Changes
{
	private static Constraints curConstraint = new Constraints();

	public static void setCurrent(Constraints con) { curConstraint = con; }
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
	 * @param dCX the change in X center of the NodeInst.
	 * @param dCY the change in Y center of the NodeInst.
	 * @param dSX the change in X size of the NodeInst.
	 * @param dSY the change in Y size of the NodeInst.
	 * @param dRot the change in rotation of the NodeInst.
	 */
	public void modifyNodeInst(NodeInst ni, double dCX, double dCY, double dSX, double dSY, int dRot) {}
	/**
	 * Method to handle a change to many NodeInsts at once.
	 * @param nis the NodeInsts that were changed.
	 * @param dCX the change in X centers of the NodeInsts.
	 * @param dCY the change in Y centers of the NodeInsts.
	 * @param dSX the change in X sizes of the NodeInsts.
	 * @param dSY the change in Y sizes of the NodeInsts.
	 * @param dRot the change in rotations of the NodeInsts.
	 */
	public void modifyNodeInsts(NodeInst [] nis, double [] dCX, double [] dCY, double [] dSX, double [] dSY, int [] dRot) {}
	/**
	 * Method to handle a change to an ArcInst.
	 * @param ai the ArcInst that changed.
	 * @param oHX the old X coordinate of the ArcInst head end.
	 * @param oHY the old Y coordinate of the ArcInst head end.
	 * @param oTX the old X coordinate of the ArcInst tail end.
	 * @param oTY the old Y coordinate of the ArcInst tail end.
	 * @param oWid the old width of the ArcInst.
	 */
	public void modifyArcInst(ArcInst ai, double oHX, double oHY, double oTX, double oTY, double oWid) {}
	/**
	 * Method to handle a change to an Export.
	 * @param pp the Export that moved.
	 * @param oldPi the old PortInst on which it resided.
	 */
	public void modifyExport(Export pp, PortInst oldPi) {}
	/**
	 * Method to handle a change to a Cell.
	 * @param cell the cell that was changed.
	 * @param oLX the old low X bound of the Cell.
	 * @param oHX the old high X bound of the Cell.
	 * @param oLY the old low Y bound of the Cell.
	 * @param oHY the old high Y bound of the Cell.
	 */
	public void modifyCell(Cell cell, double oLX, double oHX, double oLY, double oHY) {}
	/**
	 * Method to announce a move of a Cell int CellGroup.
	 * @param cell the cell that was moved.
	 * @param oCellGroup the old CellGroup of the Cell.
	 */
	public void modifyCellGroup(Cell cell, Cell.CellGroup  oCellGroup) {}
	/**
	 * Method to handle a change to a TextDescriptor.
	 * @param obj the ElectricObject on which the TextDescriptor resides.
	 * @param descript the TextDescriptor that changed.
	 * @param oldDescript0 the former word-0 bits in the TextDescriptor.
	 * @param oldDescript1 the former word-1 bits in the TextDescriptor.
	 */
	public void modifyTextDescript(ElectricObject obj, TextDescriptor descript, int oldDescript0, int oldDescript1, int oldColorIndex) {}

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
	 * @param oldVersion the former version of that ElectricObject (if a Cell).
	 */
	public void renameObject(ElectricObject obj, Name oldName, int oldVersion) {}
	/**
	 * Method to request that an object be redrawn.
	 * @param obj the ElectricObject to be redrawn.
	 */
	public void redrawObject(ElectricObject obj) {}
	/**
	 * Method to handle a new Variable.
	 * @param obj the ElectricObject on which the Variable resides.
	 * @param var the newly created Variable.
	 */
	public void newVariable(ElectricObject obj, Variable var) {}
	/**
	 * Method to handle a deleted Variable.
	 * @param obj the ElectricObject on which the Variable resided.
	 * @param var the deleted Variable.
	 */
	public void killVariable(ElectricObject obj, Variable var) {}
	/**
	 * Method to handle a change to the flag bits of a Variable.
	 * @param obj the ElectricObject on which the Variable resides.
	 * @param var the Variable that was changed.
	 * @param oldFlags the former flag bits on the Variable.
	 */
	public void modifyVariableFlags(ElectricObject obj, Variable var, int oldFlags) {}
	/**
	 * Method to handle a change to a single entry of an arrayed Variable.
	 * @param obj the ElectricObject on which the Variable resides.
	 * @param var the Variable that was changed.
	 * @param index the entry in the array that was changed.
	 * @param oldValue the former value at that entry.
	 */
	public void modifyVariable(ElectricObject obj, Variable var, int index, Object oldValue) {}
	/**
	 * Method to handle an insertion of a new entry in an arrayed Variable.
	 * @param obj the ElectricObject on which the Variable resides.
	 * @param var the Variable that was changed.
	 * @param index the entry in the array that was inserted.
	 */
	public void insertVariable(ElectricObject obj, Variable var, int index) {}
	/**
	 * Method to handle the deletion of a single entry in an arrayed Variable.
	 * @param obj the ElectricObject on which the Variable resides.
	 * @param var the Variable that was changed.
	 * @param index the entry in the array that was deleted.
	 * @param oldValue the former value of that entry.
	 */
	public void deleteVariable(ElectricObject obj, Variable var, int index, Object oldValue) {}

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

}
