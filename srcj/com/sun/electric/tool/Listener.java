/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Tool.java
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

import com.sun.electric.database.change.Changes;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
// import com.sun.electric.database.network.Network;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
// import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.database.variable.ElectricObject;
// import com.sun.electric.tool.drc.DRC;
// import com.sun.electric.tool.logicaleffort.LETool;
// import com.sun.electric.tool.routing.Routing;
// import com.sun.electric.tool.simulation.Simulation;
// import com.sun.electric.tool.user.User;

// import java.util.List;
// import java.util.ArrayList;
// import java.util.Iterator;
// import java.util.prefs.Preferences;

/**
 * This class represents a Listener - a Tool which can listen Changes.
 */
public class Listener extends Tool implements Changes
{
	/**
	 * The constructor for Listener is only called by subclasses.
	 * @param toolName the name of this listener.
	 */
	protected Listener(String toolName) { super(toolName); }

	public void request(String cmd) {}
	public void examineCell(Cell cell) {}
	public void slice() {}

	public void startBatch(Tool tool, boolean undoRedo) {}
	public void endBatch() {}

	public void modifyNodeInst(NodeInst ni, double oCX, double oCY, double oSX, double oSY, int oRot) {}
	public void modifyNodeInsts(NodeInst [] nis, double [] oCX, double [] oCY, double [] oSX, double [] oSY, int [] oRot) {}
	public void modifyArcInst(ArcInst ai, double oHX, double oHY, double oTX, double oTY, double oWid) {}
	public void modifyExport(Export pp, PortInst oldPi) {}
	public void modifyCell(Cell cell, double oLX, double oHX, double oLY, double oHY) {}
	public void modifyTextDescript(ElectricObject obj, TextDescriptor descript, int oldDescript0, int oldDescript1) {}

	public void newObject(ElectricObject obj) {}
	public void killObject(ElectricObject obj) {}
	public void redrawObject(ElectricObject obj) {}
	public void newVariable(ElectricObject obj, Variable var) {}
	public void killVariable(ElectricObject obj, Variable var) {}
	public void modifyVariableFlags(ElectricObject obj, Variable var, int oldFlags) {}
	public void modifyVariable(ElectricObject obj, Variable var, int index, Object oldValue) {}
	public void insertVariable(ElectricObject obj, Variable var, int index) {}
	public void deleteVariable(ElectricObject obj, Variable var, int index, Object oldValue) {}

	public void readLibrary(Library lib) {}
	public void eraseLibrary(Library lib) {}
	public void writeLibrary(Library lib, boolean pass2) {}

}
