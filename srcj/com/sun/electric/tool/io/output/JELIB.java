/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JELIB.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.ELIBConstants;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;

/** 
 * Class to write a library to disk in new Electric-Library format.
 */
public class JELIB extends Output
{
	private HashMap abbreviationMap;

	JELIB()
	{
	}

	/**
	 * Method to write a Library in Electric Library (.jelib) format.
	 * @param lib the Library to be written.
	 */
	protected boolean writeLib(Library lib)
	{
		try
		{
			return writeTheLibrary(lib);
		} catch (IOException e)
		{
			System.out.println("End of file reached while writing " + filePath);
			return true;
		}
	}

	/**
	 * Method to write the .jelib file.
	 * Returns true on error.
	 */
	private boolean writeTheLibrary(Library lib)
		throws IOException
	{
		printWriter.print("# A arc (instance or primitive) information\n");
		printWriter.print("# C cell information\n");
		printWriter.print("# E export information\n");
		printWriter.print("# G group information\n");
		printWriter.print("# H library header\n");
		printWriter.print("# N node (instance or primitive) information\n");
		printWriter.print("# O tool information\n");
		printWriter.print("# P port (on primitive) information\n");
		printWriter.print("# R main cell name\n");
		printWriter.print("# T technology information\n");
		printWriter.print("# V view information\n");
		printWriter.print("\n");

		// pick up all full names that might become abbreviations
		abbreviationMap = new HashMap();
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			if (cell.getNumUsagesIn() == 0) textRecurse(cell, lib);
		}
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			StringBuffer abbr = (StringBuffer)abbreviationMap.get(cell);
			if (abbr == null) textRecurse(cell, lib);
		}

		// write header information
		printWriter.print("H" + Version.getVersion() + "|" + lib.getName());
		writeVars(lib, null);
		printWriter.print("\n");

		// write tool information
		boolean hasPersistent = false;
		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			if (tool.numPersistentVariables() != 0) { hasPersistent = true;  break; }
		}
		if (hasPersistent)
		{
			printWriter.print("\n# Tools:\n");
			for(Iterator it = Tool.getTools(); it.hasNext(); )
			{
				Tool tool = (Tool)it.next();
				if (tool.numPersistentVariables() == 0) continue;
				printWriter.print("O" + tool.getName());
				writeVars(tool, null);
				printWriter.print("\n");
			}
		}

		// write technology information
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();

			// see if the technology has persistent variables
			hasPersistent = false;
			for(Iterator nIt = tech.getNodes(); nIt.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)nIt.next();
				if (np.numPersistentVariables() != 0) { hasPersistent = true;   break; }
				for(Iterator pIt = np.getPorts(); pIt.hasNext(); )
				{
					PrimitivePort pp = (PrimitivePort)pIt.next();
					if (pp.numPersistentVariables() != 0) { hasPersistent = true;   break; }
				}
				if (hasPersistent) break;
			}
			if (!hasPersistent)
			{
				for(Iterator aIt = tech.getArcs(); aIt.hasNext(); )
				{
					ArcProto ap = (ArcProto)aIt.next();
					if (ap.numPersistentVariables() != 0) { hasPersistent = true;   break; }
				}
			}
			if (!hasPersistent) continue;

			printWriter.print("\n# Technology " + tech.getTechName() + "\n");
			printWriter.print("T" + tech.getTechName());
			writeVars(tech, null);
			printWriter.print("\n");
			for(Iterator nIt = tech.getNodes(); nIt.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)nIt.next();
				hasPersistent = false;
				if (np.numPersistentVariables() != 0) hasPersistent = true; else
				{
					for(Iterator pIt = np.getPorts(); pIt.hasNext(); )
					{
						PrimitivePort pp = (PrimitivePort)pIt.next();
						if (pp.numPersistentVariables() != 0) { hasPersistent = true;   break; }
					}
				}
				if (!hasPersistent) continue;

				printWriter.print("N" + np.getName());
				writeVars(np, null);
				printWriter.print("\n");
				for(Iterator pIt = np.getPorts(); pIt.hasNext(); )
				{
					PrimitivePort pp = (PrimitivePort)pIt.next();
					if (pp.numPersistentVariables() == 0) continue;
					printWriter.print("P" + pp.getName());
					writeVars(pp, null);
					printWriter.print("\n");
				}
			}
			for(Iterator aIt = tech.getArcs(); aIt.hasNext(); )
			{
				ArcProto ap = (ArcProto)aIt.next();
				if (ap.numPersistentVariables() == 0) continue;
				printWriter.print("A" + ap.getName());
				writeVars(ap, null);
				printWriter.print("\n");
			}
		}

		// write view information
		hasPersistent = false;
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View v = (View)it.next();
			if (v.numPersistentVariables() != 0) { hasPersistent = true;  break; }
		}
		if (hasPersistent)
		{
			printWriter.print("\n# Views:\n");
			for(Iterator it = View.getViews(); it.hasNext(); )
			{
				View v = (View)it.next();
				if (v.numPersistentVariables() == 0) continue;
				printWriter.print("V" + v.getFullName() + "|" + v.getAbbreviation());
				writeVars(v, null);
				printWriter.print("\n");
			}
		}

		// write the cells of the database
		List cells = lib.getCellsSortedByName();
		List groups = new ArrayList();
		for(Iterator cIt = cells.iterator(); cIt.hasNext(); )
		{
			// write the Cell name
			Cell cell = (Cell)cIt.next();
			if (!groups.contains(cell.getCellGroup()))
				groups.add(cell.getCellGroup());
			printWriter.print("\n# Cell " + cell.describe() + "\n");
			printWriter.print("C" + cell.getName());
			printWriter.print("|" + cell.getView().getAbbreviation());
			printWriter.print("|" + cell.getVersion());
			printWriter.print("|" + ELIBConstants.dateToSeconds(cell.getCreationDate()));
			printWriter.print("|" + ELIBConstants.dateToSeconds(cell.getRevisionDate()));
			writeVars(cell, cell);
			printWriter.print("\n");

			// write the nodes in this cell (sorted by node name)
			List sortedNodes = new ArrayList();
			for(Iterator it = cell.getNodes(); it.hasNext(); )
				sortedNodes.add(it.next());
			Collections.sort(sortedNodes, new NodesByName());
			for(Iterator it = sortedNodes.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				NodeProto np = ni.getProto();
				printWriter.print("N");
				StringBuffer nodeTypeName = (StringBuffer)abbreviationMap.get(np);
				printWriter.print(nodeTypeName.toString());
				printWriter.print("|" + ni.getName());
				printWriter.print("|" + TextUtils.formatDouble(ni.getAnchorCenterX()));
				printWriter.print("|" + TextUtils.formatDouble(ni.getAnchorCenterX()));
				printWriter.print("|" + TextUtils.formatDouble(ni.getXSizeWithMirror()));
				printWriter.print("|" + TextUtils.formatDouble(ni.getXSizeWithMirror()));
				printWriter.print("|" + ni.getAngle());
				StringBuffer nodeBits = new StringBuffer();
				if (ni.isExpanded()) nodeBits.append("E");
				if (ni.isLocked()) nodeBits.append("L");
				if (ni.isShortened()) nodeBits.append("H");
				if (ni.isVisInside()) nodeBits.append("V");
				if (ni.isWiped()) nodeBits.append("W");
				if (ni.isHardSelect()) nodeBits.append("S");
				int ts = ni.getTechSpecific();
				if (ts != 0) nodeBits.append(ts);
				printWriter.print("|" + nodeBits.toString());
//				if (np instanceof Cell)
//				{
//					printWriter.print("descript: " + ni.getProtoTextDescriptor().lowLevelGet0() + "/" +
//						ni.getProtoTextDescriptor().lowLevelGet0() + "\n");
//				}
				writeVars(ni, cell);
				printWriter.print("\n");
			}

			// write the arcs in this cell
			List sortedArcs = new ArrayList();
			for(Iterator it = cell.getArcs(); it.hasNext(); )
				sortedArcs.add(it.next());
			Collections.sort(sortedArcs, new ArcsByName());
			for(Iterator it = sortedArcs.iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ArcProto ap = ai.getProto();
				StringBuffer arcTypeName = (StringBuffer)abbreviationMap.get(ap);
				printWriter.print("A" + arcTypeName.toString());
				printWriter.print("|" + ai.getName());
				printWriter.print("|" + TextUtils.formatDouble(ai.getWidth()));
				StringBuffer arcBits = new StringBuffer();
				if (ai.isRigid()) arcBits.append("R");
				if (!ai.isFixedAngle()) arcBits.append("F");
				if (ai.isSlidable()) arcBits.append("S");
				if (!ai.isExtended()) arcBits.append("E");
				if (ai.isDirectional()) arcBits.append("D");
				if (ai.isReverseEnds()) arcBits.append("V");
				if (ai.isHardSelect()) arcBits.append("S");
				if (ai.isSkipHead()) arcBits.append("H");
				if (ai.isSkipTail()) arcBits.append("T");
				if (ai.getTail().isNegated()) arcBits.append("N");
				if (ai.getHead().isNegated()) arcBits.append("G");
				printWriter.print("|" + arcBits.toString() + ai.getAngle());
				for(int e=0; e<2; e++)
				{
					Connection con = ai.getConnection(e);
					NodeInst ni = con.getPortInst().getNodeInst();
					printWriter.print("|" + ni.getName() + "|");
					PortProto pp = con.getPortInst().getPortProto();
					if (ni.getProto().getNumPorts() > 1)
						printWriter.print(pp.getName());
					printWriter.print("|" + TextUtils.formatDouble(con.getLocation().getX()));
					printWriter.print("|" + TextUtils.formatDouble(con.getLocation().getY()));
				}
				writeVars(ai, cell);
				printWriter.print("\n");
			}

			// write the portprotos in this cell
			List sortedExports = new ArrayList();
			for(Iterator it = cell.getPorts(); it.hasNext(); )
				sortedExports.add(it.next());
			Collections.sort(sortedExports, new ExportsByName());
			for(Iterator it = sortedExports.iterator(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				printWriter.print("E" + pp.getName());
				NodeInst subNI = pp.getOriginalPort().getNodeInst();
				PortProto subPP = pp.getOriginalPort().getPortProto();
				printWriter.print("|" + subNI.getName() + "|");
				if (subNI.getProto().getNumPorts() > 1)
					printWriter.print(subPP.getName());

				// need to write both words
//				TextDescriptor td = pp.getTextDescriptor();
//				printWriter.print("descript: " + td.lowLevelGet0() + "/" + td.lowLevelGet1() + "\n");
//				printWriter.print("userbits: " + pp.lowLevelGetUserbits() + "\n");
				writeVars(pp, cell);
				printWriter.print("\n");
			}
		}

		// write groups in alphabetical order
		printWriter.print("\n# Groups:\n");
		for(Iterator it = groups.iterator(); it.hasNext(); )
		{
			Cell.CellGroup group = (Cell.CellGroup)it.next();
			List sortedList = new ArrayList();
			for(Iterator cIt = group.getCells(); cIt.hasNext(); )
				sortedList.add(cIt.next());
			Collections.sort(sortedList, new CellsByName());
			printWriter.print("G");
			boolean first = true;
			for(Iterator cIt = sortedList.iterator(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				if (first) first = false; else
					printWriter.print("|");
				printWriter.print(cell.describe());
			}
			printWriter.print("\n");
		}

		// write the main cell
		Cell curCell = lib.getCurCell();
		if (curCell != null)
		{
			printWriter.print("\n# Main cell:\n");
			printWriter.print("R" + curCell.noLibDescribe() + "\n");
		}

		// clean up and return
		lib.clearChangedMinor();
		lib.clearChangedMajor();
		System.out.println(filePath + " written");
		return false;
	}

	private static class NodesByName implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			NodeInst n1 = (NodeInst)o1;
			NodeInst n2 = (NodeInst)o2;
			String s1 = n1.getName();
			String s2 = n2.getName();
			if (s1 == null) s1 = "";
			if (s2 == null) s2 = "";
			return s1.compareToIgnoreCase(s2);
		}
	}

	private static class ArcsByName implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			ArcInst a1 = (ArcInst)o1;
			ArcInst a2 = (ArcInst)o2;
			String s1 = a1.getName();
			String s2 = a2.getName();
			if (s1 == null) s1 = "";
			if (s2 == null) s2 = "";
			return s1.compareToIgnoreCase(s2);
		}
	}

	private static class ExportsByName implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Export e1 = (Export)o1;
			Export e2 = (Export)o2;
			String s1 = e1.getName();
			String s2 = e2.getName();
			return s1.compareToIgnoreCase(s2);
		}
	}

	private static class CellsByName implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Cell c1 = (Cell)o1;
			Cell c2 = (Cell)o2;
			String s1 = c1.describe();
			String s2 = c2.describe();
			return s1.compareToIgnoreCase(s2);
		}
	}
	
	/**
	 * Method to help order the library for proper nonforward references
	 * in the outout
	 */
	private void textRecurse(Cell cell, Library lib)
	{
		if (cell.getLibrary() == lib)
		{
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (ni.getProto() instanceof Cell)
				{
					Cell subCell = (Cell)ni.getProto();
					StringBuffer abbr = (StringBuffer)abbreviationMap.get(subCell);
					if (abbr == null) textRecurse(subCell, lib);
				} else
				{
					PrimitiveNode np = (PrimitiveNode)ni.getProto();
					StringBuffer abbr = (StringBuffer)abbreviationMap.get(np);
					if (abbr == null)
					{
						abbreviationMap.put(np, new StringBuffer(np.getTechnology().getTechName() + ":" + np.getName()));
					}
				}
			}
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ArcProto ap = ai.getProto();
				StringBuffer abbr = (StringBuffer)abbreviationMap.get(ap);
				if (abbr == null)
				{
					abbreviationMap.put(ap, new StringBuffer(ap.getTechnology().getTechName() + ":" + ap.getName()));
				}
			}
		}

		// add this cell to the list
		abbreviationMap.put(cell, new StringBuffer(cell.noLibDescribe()));
	}

	/**
	 * Method to write the variables on an object.  The current cell is
	 * "curCell" such that any references to objects in a cell must be in
	 * this cell.
	 */
	private void writeVars(ElectricObject eObj, Cell curCell)
	{
		// count the number of variables
		for(Iterator it = eObj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var.isDontSave()) continue;
			printWriter.print("|" + var.getKey().getName() + "=" + var.getObject());
//			int type = var.lowLevelGetFlags() & ~(ELIBConstants.VTYPE|ELIBConstants.VISARRAY|ELIBConstants.VLENGTH);
//			Object varObj = var.getObject();
//
//			// special case for "trace" information on NodeInsts
//			if (eObj instanceof NodeInst && var.getKey() == NodeInst.TRACE && varObj instanceof Object[])
//			{
//				Object [] objList = (Object [])varObj;
//				Point2D [] points = (Point2D [])objList;
//				int len = points.length * 2;
//				Float [] newPoints = new Float[len];
//				for(int j=0; j<points.length; j++)
//				{
//					newPoints[j*2] = new Float(points[j].getX());
//					newPoints[j*2+1] = new Float(points[j].getY());
//				}
//				varObj = newPoints;
//			}
//
//			String pt = makeString(varObj, curCell);
//			if (pt == null) pt = "";
//			printName(var.getKey().getName());
//			TextDescriptor td = var.getTextDescriptor();
//
//			if (varObj instanceof Object[])
//			{
//				Object [] objList = (Object [])varObj;
//				int objType = ELIBConstants.getVarType(objList[0]);
//				if (objType == ELIBConstants.VDOUBLE) objType = ELIBConstants.VFLOAT;
//				int len = objList.length;
//				type |= objType | ELIBConstants.VISARRAY | (len << ELIBConstants.VLENGTHSH);
//				printWriter.print("(" + len + ")[0" + Integer.toOctalString(type) + ",0" +
//					Integer.toOctalString(td.lowLevelGet0()) + "/0" + Integer.toOctalString(td.lowLevelGet1()) + "]: ");
//			} else
//			{
//				int objType = ELIBConstants.getVarType(varObj);
//				if (objType == ELIBConstants.VDOUBLE) objType = ELIBConstants.VFLOAT;
//				type |= objType;
//				printWriter.print("[0" + Integer.toOctalString(type) + ",0" +
//					Integer.toOctalString(td.lowLevelGet0()) + "/0" + Integer.toOctalString(td.lowLevelGet1()) + "]: ");
//			}
//			printWriter.print(pt + "\n");
		}
	}

	/**
	 * Method to convert variable "var" to a string for printing in the text file.
	 * returns zero on error
	 */
//	private String makeString(Object obj, Cell curCell)
//	{
//		StringBuffer infstr = new StringBuffer();
//		if (obj instanceof Object[])
//		{
//			Object [] objArray = (Object [])obj;
//			int len = objArray.length;
//			for(int i=0; i<len; i++)
//			{
//				Object oneObj = objArray[i];
//				if (i == 0) infstr.append("["); else
//					infstr.append(",");
//				makeStringVar(infstr, oneObj, curCell);
//			}
//			infstr.append("]");
//		} else makeStringVar(infstr, obj, curCell);
//		return infstr.toString();
//	}

	/**
	 * Method to make a string from the value in "addr" which has a type in
	 * "type".
	 */
//	private void makeStringVar(StringBuffer infstr, Object obj, Cell curCell)
//	{
//		if (obj instanceof Integer)
//		{
//			infstr.append(((Integer)obj).intValue());
//			return;
//		}
//		if (obj instanceof Short)
//		{
//			infstr.append(((Short)obj).shortValue());
//			return;
//		}
//		if (obj instanceof Byte)
//		{
//			infstr.append(((Byte)obj).byteValue());
//			return;
//		}
//		if (obj instanceof String)
//		{
//			infstr.append("\"");
//			infstr.append(convertString((String)obj));
//			infstr.append("\"");
//			return;
//		}
//		if (obj instanceof Float)
//		{
//			infstr.append(((Float)obj).floatValue());
//			return;
//		}
//		if (obj instanceof Double)
//		{
//			infstr.append(((Double)obj).doubleValue());
//			return;
//		}
//		if (obj instanceof Boolean)
//		{
//			infstr.append(((Boolean)obj).booleanValue() ? 1 : 0);
//			return;
//		}
//		if (obj instanceof Long)
//		{
//			infstr.append(((Long)obj).longValue());
//			return;
//		}
//		if (obj instanceof Technology)
//		{
//			Technology tech = (Technology)obj;
//			infstr.append(tech.getTechName());
//			return;
//		}
//		if (obj instanceof Library)
//		{
//			Library lib = (Library)obj;
//			infstr.append("\"" + lib.getName() + "\"");
//			return;
//		}
//		if (obj instanceof Tool)
//		{
//			Tool tool = (Tool)obj;
//			infstr.append(tool.getName());
//			return;
//		}
//		if (obj instanceof NodeInst)
//		{
//			NodeInst ni = (NodeInst)obj;
//			Integer nodeIndex = (Integer)nodeMap.get(ni);
//			int cIndex = -1;
//			if (nodeIndex == null) nodeInstError++; else
//				cIndex = nodeIndex.intValue();
//			infstr.append(Integer.toString(cIndex));
//			return;
//		}
//		if (obj instanceof ArcInst)
//		{
//			ArcInst ai = (ArcInst)obj;
//			Integer arcIndex = (Integer)arcMap.get(ai);
//			int cIndex = -1;
//			if (arcIndex == null) arcInstError++; else
//				cIndex = arcIndex.intValue();
//			infstr.append(Integer.toString(cIndex));
//			return;
//		}
//		if (obj instanceof Cell)
//		{
//			Cell cell = (Cell)obj;
//			DBMath.MutableInteger mi = (DBMath.MutableInteger)cellOrderMap.get(cell);
//			int cIndex = -1;
//			if (mi != null) cIndex = mi.intValue();
//			infstr.append(Integer.toString(cIndex));
//			return;
//		}
//		if (obj instanceof PrimitiveNode)
//		{
//			PrimitiveNode np = (PrimitiveNode)obj;
//			infstr.append(np.getTechnology().getTechName() + ":" + np.getName());
//			return;
//		}
//		if (obj instanceof ArcProto)
//		{
//			ArcProto ap = (ArcProto)obj;
//			infstr.append(ap.getTechnology().getTechName() + ":" + ap.getName());
//			return;
//		}
//		if (obj instanceof Export)
//		{
//			Export pp = (Export)obj;
//			Integer portIndex = (Integer)portMap.get(pp);
//			int cIndex = -1;
//			if (portIndex == null) portProtoError++; else
//				cIndex = portIndex.intValue();
//			infstr.append(Integer.toString(cIndex));
//			return;
//		}
//		typeError++;
//	}

	/**
	 * Method to add the string "str" to the infinite string and to quote the
	 * special characters '[', ']', '"', and '^'.
	 */
//	private String convertString(String str)
//	{
//		StringBuffer infstr = new StringBuffer();
//		int len = str.length();
//		for(int i=0; i<len; i++)
//		{
//			char ch = str.charAt(i);
//			if (ch == '[' || ch == ']' || ch == '"' || ch == '^')
//				infstr.append('^');
//			infstr.append(ch);
//		}
//		return infstr.toString();
//	}

	/**
	 * Method to print the variable name in "name" on file "file".  The
	 * conversion performed is to quote with a backslash any of the characters
	 * '(', '[', or '^'.
	 */
//	private void printName(String name)
//	{
//		int len = name.length();
//		for(int i=0; i<len; i++)
//		{
//			char pt = name.charAt(i);
//			if (pt == '^' || pt == '[' || pt == '(') printWriter.print("^");
//			printWriter.print(pt);
//		}
//	}
}
