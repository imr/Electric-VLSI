/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Constraint.java
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

import com.sun.electric.database.change.Change;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.tool.Tool;


public class Constraint implements Change
{
	private static Constraint curConstraint = new Constraint();

	public static void setCurrent(Constraint con) { curConstraint = con; }
	public static Constraint getCurrent() { return curConstraint; }

	public void init() {}
	public void request(String cmd) {}
	public void examineCell(Cell cell) {}
	public void slice() {}

	public void startBatch(Tool tool, boolean undoRedo) {}
	public void endBatch() {}

	public void modifyNodeInst(NodeInst ni, double dCX, double dCY, double dSX, double dSY, int dRot) {}
	public void modifyNodeInsts(NodeInst [] nis, double [] dCX, double [] dCY, double [] dSX, double [] dSY, int [] dRot) {}
	public void modifyArcInst(ArcInst ai, double oHX, double oHY, double oTX, double oTY, double oWid) {}
	public void modifyExport(Export pp, PortInst oldPi) {}
	public void modifyCell(Cell cell, double oLX, double oHX, double oLY, double oHY) {}
	public void modifyTextDescript(ElectricObject obj, int key, Object oldValue) {}

	public void newObject(ElectricObject obj) {}
	public void killObject(ElectricObject obj) {}
	public void newVariable(ElectricObject obj, Variable.Key key, int type) {}
	public void killVariable(ElectricObject obj, Variable.Key key, Object oldValue, TextDescriptor oldDescript) {}
	public void modifyVariable(ElectricObject obj, Variable.Key key, int type, int index, Object oldValue) {}
	public void insertVariable(ElectricObject obj, Variable.Key key, int type, int index) {}
	public void deleteVariable(ElectricObject obj, Variable.Key key, int type, int index, Object oldValue) {}

	public void readLibrary(Library lib) {}
	public void eraseLibrary(Library lib) {}
	public void writeLibrary(Library lib, boolean pass2) {}
	
}
