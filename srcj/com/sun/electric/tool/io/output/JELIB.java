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
	private List externalLibs;

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
		printWriter.print("# header information:\n");

		// pick up all full names that might become abbreviations
		externalLibs = new ArrayList();
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

		// write header information (library, version, main cell)
		Cell curCell = lib.getCurCell();
		printWriter.print("H" + lib.getName() + "|" + Version.getVersion() + "|");
		if (curCell != null) printWriter.print(curCell.noLibDescribe());
		writeVars(lib, null);
		printWriter.print("\n");

		// write external library information
		if (externalLibs.size() > 0)
		{
			printWriter.print("\n# External Libraries:\n");
			Collections.sort(externalLibs, new LibrariesByName());
			for(Iterator it = externalLibs.iterator(); it.hasNext(); )
			{
				Library eLib = (Library)it.next();
				String libFile = eLib.getLibFile().toString();
				if (libFile.endsWith(".elib")) libFile = libFile.substring(0, libFile.length()-5) + ".jelib"; else
				if (libFile.endsWith(".txt")) libFile = libFile.substring(0, libFile.length()-4) + ".jelib";
				printWriter.print("L" + eLib.getName() + "|" + libFile + "\n");
			}
		}

		// write tool information
		List toolList = new ArrayList();
		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			if (tool.numPersistentVariables() != 0) toolList.add(tool);
		}
		if (toolList.size() > 0)
		{
			printWriter.print("\n# Tools:\n");
			Collections.sort(toolList, new ToolsByName());
			for(Iterator it = toolList.iterator(); it.hasNext(); )
			{
				Tool tool = (Tool)it.next();
				printWriter.print("O" + tool.getName());
				writeVars(tool, null);
				printWriter.print("\n");
			}
		}

		// write technology information
		for(Iterator it = Technology.getTechnologiesSortedByName().iterator(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();

			// see if the technology has persistent variables
			boolean hasPersistent = (tech.numPersistentVariables() != 0);
			if (!hasPersistent)
			{
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
		boolean hasPersistent = false;
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
			printWriter.print("|" + cell.getTechnology().getTechName());
			printWriter.print("|" + cell.getCreationDate().getTime());
			printWriter.print("|" + cell.getRevisionDate().getTime());
			StringBuffer cellBits = new StringBuffer();
			if (cell.isWantExpanded()) cellBits.append("E");
			if (cell.isAllLocked()) cellBits.append("L");
			if (cell.isInstancesLocked()) cellBits.append("I");
			if (cell.isInCellLibrary()) cellBits.append("C");
			if (cell.isInTechnologyLibrary()) cellBits.append("T");
			printWriter.print("|" + cellBits.toString());
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
				printWriter.print("|" + TextUtils.formatDouble(ni.getAnchorCenterX(), 0));
				printWriter.print("|" + TextUtils.formatDouble(ni.getAnchorCenterY(), 0));
				printWriter.print("|" + TextUtils.formatDouble(ni.getXSizeWithMirror(), 0));
				printWriter.print("|" + TextUtils.formatDouble(ni.getYSizeWithMirror(), 0));
				printWriter.print("|" + ni.getAngle());
				StringBuffer nodeBits = new StringBuffer();
				if (ni.isExpanded()) nodeBits.append("E");
				if (ni.isLocked()) nodeBits.append("L");
				if (ni.isShortened()) nodeBits.append("S");
				if (ni.isVisInside()) nodeBits.append("V");
				if (ni.isWiped()) nodeBits.append("W");
				if (ni.isHardSelect()) nodeBits.append("A");
				int ts = ni.getTechSpecific();
				if (ts != 0) nodeBits.append(ts);
				printWriter.print("|" + nodeBits.toString() + "|");
				if (np instanceof Cell)
				{
					String tdString = describeDescriptor(null, ni.getProtoTextDescriptor());
					printWriter.print(tdString);
				}
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
				printWriter.print("|" + TextUtils.formatDouble(ai.getWidth(), 0));
				StringBuffer arcBits = new StringBuffer();

				if (ai.isRigid()) arcBits.append("R");
				if (!ai.isFixedAngle()) arcBits.append("F");
				if (ai.isSlidable()) arcBits.append("S");
				if (!ai.isExtended()) arcBits.append("E");
				if (ai.isDirectional()) arcBits.append("D");
				if (ai.isReverseEnds()) arcBits.append("V");
				if (ai.isHardSelect()) arcBits.append("A");
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
					printWriter.print("|" + TextUtils.formatDouble(con.getLocation().getX(), 0));
					printWriter.print("|" + TextUtils.formatDouble(con.getLocation().getY(), 0));
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

				String tdString = describeDescriptor(null, pp.getTextDescriptor());
				printWriter.print("|" + tdString);

				// port information
				printWriter.print("|" + pp.getCharacteristic().getShortName());
				if (pp.isAlwaysDrawn()) printWriter.print("/A");
				if (pp.isBodyOnly()) printWriter.print("/B");

				writeVars(pp, cell);
				printWriter.print("\n");
			}

			// write the end-of-cell marker
			printWriter.print("X\n");
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
			return TextUtils.nameSameNumeric(s1, s2);
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
			return TextUtils.nameSameNumeric(s1, s2);
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
			return TextUtils.nameSameNumeric(s1, s2);
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
			return TextUtils.nameSameNumeric(s1, s2);
		}
	}

	private static class LibrariesByName implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Library l1 = (Library)o1;
			Library l2 = (Library)o2;
			String s1 = l1.getName();
			String s2 = l2.getName();
			return s1.compareToIgnoreCase(s2);
		}
	}

	private static class ToolsByName implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Tool t1 = (Tool)o1;
			Tool t2 = (Tool)o2;
			String s1 = t1.getName();
			String s2 = t2.getName();
			return s1.compareToIgnoreCase(s2);
		}
	}

	private static class VariablesByName implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Variable v1 = (Variable)o1;
			Variable v2 = (Variable)o2;
			String s1 = v1.getKey().getName();
			String s2 = v2.getKey().getName();
			return s1.compareToIgnoreCase(s2);
		}
	}
	
	/**
	 * Method to help order the library for proper nonforward references
	 * in the outout
	 */
	private void textRecurse(Cell cell, Library lib)
	{
		Library cellLib = cell.getLibrary();
		if (cellLib == lib)
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
		} else
		{
			if (!externalLibs.contains(cellLib)) externalLibs.add(cellLib);
		}

		// add this cell to the list
		StringBuffer sb = new StringBuffer();
		sb.append(cell.getLibrary().getName());
		sb.append(":");
		sb.append(cell.getName());
		sb.append(";");
		sb.append(cell.getVersion());
		sb.append("{");
		sb.append(cell.getView().getAbbreviation());
		sb.append("}");
		abbreviationMap.put(cell, sb);
	}

	/**
	 * Method to convert a variable to a string that describes its TextDescriptor
	 * @param var the Variable being described (may be null).
	 * @param td the TextDescriptor being described.
	 * @return a String describing the variable/textdescriptor.
	 * The string has these fields:
	 *    Asize; for absolute size
	 *    B if bold
	 *    Cindex; if color index
	 *    Dx for display position (2=bottom 8=top 4=left 6=right 7=upleft 9=upright 1=downleft 3=downright 5=centered 0=boxed)
	 *    FfontName; if a nonstandard font
	 *    Gsize; for relative (grid unit) size
	 *    H if inherit
	 *    I if italic
	 *    L if underline
	 *    N if name=value;
	 *    Ol for language (J=Java L=Lisp T=TCL)
	 *    P if parameter
	 *    R/RR/RRR if rotated (90, 180, 270)
	 *    T if interior
	 *    Ux for units (R=resistance C=capacitance I=inductance A=current V=voltage D=distance T=time)
	 *    Xoffset; for X offset
	 *    Yoffset; for Y offset
	 */
	private String describeDescriptor(Variable var, TextDescriptor td)
	{
		StringBuffer ret = new StringBuffer();
		if (var == null || var.isDisplay())
		{
			// displayable: write display position
			ret.append("D");
			TextDescriptor.Position pos = td.getPos();
			if (pos == TextDescriptor.Position.UP) ret.append("8"); else
			if (pos == TextDescriptor.Position.DOWN) ret.append("2"); else
			if (pos == TextDescriptor.Position.LEFT) ret.append("4"); else
			if (pos == TextDescriptor.Position.RIGHT) ret.append("6"); else
			if (pos == TextDescriptor.Position.UPLEFT) ret.append("7"); else
			if (pos == TextDescriptor.Position.UPRIGHT) ret.append("9"); else
			if (pos == TextDescriptor.Position.DOWNLEFT) ret.append("1"); else
			if (pos == TextDescriptor.Position.DOWNRIGHT) ret.append("3"); else
			if (pos == TextDescriptor.Position.BOXED) ret.append("0"); else
				ret.append("5");

			// write display type
			TextDescriptor.DispPos dispPos = td.getDispPart();
			if (dispPos == TextDescriptor.DispPos.NAMEVALUE) ret.append("N");

			// write size
			TextDescriptor.Size size = td.getSize();
			if (size.isAbsolute()) ret.append("A" + (int)size.getSize() + ";"); else
				ret.append("G" + TextUtils.formatDouble(size.getSize()) + ";");

			// write offset
			double offX = td.getXOff();
			if (offX != 0) ret.append("X" + TextUtils.formatDouble(offX, 0) + ";");
			double offY = td.getYOff();
			if (offY != 0) ret.append("Y" + TextUtils.formatDouble(offY, 0) + ";");

			// write bold/italic/underline
			if (td.isBold()) ret.append("B");
			if (td.isItalic()) ret.append("I");
			if (td.isUnderline()) ret.append("L");

			// write font
			int font = td.getFace();
			if (font != 0)
			{
				TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(font);
				ret.append("F" + af.toString() + ";");
			}

			// write color
			int color = td.getColorIndex();
			if (color != 0)
				ret.append("C" + color + ";");

			// write rotation
			TextDescriptor.Rotation rot = td.getRotation();
			if (rot == TextDescriptor.Rotation.ROT90) ret.append("R"); else
			if (rot == TextDescriptor.Rotation.ROT180) ret.append("RR"); else
			if (rot == TextDescriptor.Rotation.ROT270) ret.append("RRR");
		}

		// write inherit/interior/parameter
		if (td.isInherit()) ret.append("H");
		if (td.isInterior()) ret.append("T");
		if (td.isParam()) ret.append("P");

		// write language
		if (var != null && var.isCode())
		{
			Variable.Code codeType = var.getCode();
			if (codeType == Variable.Code.JAVA) ret.append("OJ"); else
			if (codeType == Variable.Code.LISP) ret.append("OL"); else
			if (codeType == Variable.Code.TCL) ret.append("OT");
		}

		// write units
		TextDescriptor.Unit unit = td.getUnit();
		if (unit == TextDescriptor.Unit.RESISTANCE) ret.append("UR"); else
		if (unit == TextDescriptor.Unit.CAPACITANCE) ret.append("UC"); else
		if (unit == TextDescriptor.Unit.INDUCTANCE) ret.append("UI"); else
		if (unit == TextDescriptor.Unit.CURRENT) ret.append("UA"); else
		if (unit == TextDescriptor.Unit.VOLTAGE) ret.append("UV"); else
		if (unit == TextDescriptor.Unit.DISTANCE) ret.append("UD"); else
		if (unit == TextDescriptor.Unit.TIME) ret.append("UT");

		return ret.toString();
	}

	/**
	 * Method to write the variables on an object.  The current cell is
	 * "curCell" such that any references to objects in a cell must be in
	 * this cell.
	 */
	private void writeVars(ElectricObject eObj, Cell curCell)
	{
		// count the number of variables
		int numVars = 0;
		for(Iterator it = eObj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var.isDontSave()) continue;
			numVars++;
		}
		if (numVars == 0) return;

		// sort the variables if there are more than 1
		Iterator varIterator = eObj.getVariables();
		if (numVars > 1)
		{
			// must sort the names
			List sortedVars = new ArrayList();
			for(Iterator it = eObj.getVariables(); it.hasNext(); )
			{
				Variable var = (Variable)it.next();
				if (var.isDontSave()) continue;
				sortedVars.add(var);
			}
			Collections.sort(sortedVars, new VariablesByName());
			varIterator = sortedVars.iterator();
		}

		// write the variables
		for(Iterator it = varIterator; it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var.isDontSave()) continue;
			String tdString = describeDescriptor(var, var.getTextDescriptor());
			printWriter.print("|" + var.getKey().getName() + "(" + tdString + ")");

			Object varObj = var.getObject();
			String pt = makeString(varObj, curCell);
			if (pt == null) pt = "";
			printWriter.print(pt);
		}
	}

	/**
	 * Method to convert variable "var" to a string for printing in the text file.
	 * returns zero on error
	 */
	private String makeString(Object obj, Cell curCell)
	{
		StringBuffer infstr = new StringBuffer();
		if (obj instanceof Object[])
		{
			Object [] objArray = (Object [])obj;
			int len = objArray.length;
			for(int i=0; i<len; i++)
			{
				Object oneObj = objArray[i];
				if (i != 0) infstr.append(","); else
				{
					infstr.append(getVarType(oneObj));
					infstr.append("[");
				}					
				makeStringVar(infstr, oneObj, curCell);
			}
			infstr.append("]");
		} else
		{
			infstr.append(getVarType(obj));
			makeStringVar(infstr, obj, curCell);
		}
		return infstr.toString();
	}

	/**
	 * Method to make a string from the value in "addr" which has a type in
	 * "type".
	 */
	private void makeStringVar(StringBuffer infstr, Object obj, Cell curCell)
	{
		if (obj instanceof Integer)
		{
			infstr.append(((Integer)obj).intValue());
			return;
		}
		if (obj instanceof Short)
		{
			infstr.append(((Short)obj).shortValue());
			return;
		}
		if (obj instanceof Byte)
		{
			infstr.append(((Byte)obj).byteValue());
			return;
		}
		if (obj instanceof String)
		{
			infstr.append("\"");
			infstr.append(convertString((String)obj));
			infstr.append("\"");
			return;
		}
		if (obj instanceof Float)
		{
			infstr.append(((Float)obj).floatValue());
			return;
		}
		if (obj instanceof Double)
		{
			infstr.append(((Double)obj).doubleValue());
			return;
		}
		if (obj instanceof Boolean)
		{
			infstr.append(((Boolean)obj).booleanValue() ? "T" : "F");
			return;
		}
		if (obj instanceof Long)
		{
			infstr.append(((Long)obj).longValue());
			return;
		}
		if (obj instanceof Point2D)
		{
			Point2D pt2 = (Point2D)obj;
			infstr.append(TextUtils.formatDouble(pt2.getX(), 0) + "/" + TextUtils.formatDouble(pt2.getY(), 0));
			return;
		}
		if (obj instanceof Technology)
		{
			Technology tech = (Technology)obj;
			infstr.append(tech.getTechName());
			return;
		}
		if (obj instanceof Library)
		{
			Library lib = (Library)obj;
			infstr.append("\"" + lib.getName() + "\"");
			return;
		}
		if (obj instanceof Tool)
		{
			Tool tool = (Tool)obj;
			infstr.append(tool.getName());
			return;
		}
		if (obj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)obj;
			infstr.append(ni.getParent().libDescribe() + ":" + ni.getName());
			return;
		}
		if (obj instanceof ArcInst)
		{
			ArcInst ai = (ArcInst)obj;
			String arcName = ai.getName();
			if (arcName == null)
			{
				System.out.println("Cannot save pointer to unnamed ArcInst: " + ai.getParent().describe() + ":" + ai.describe());
			}
			infstr.append(ai.getParent().libDescribe() + ":" + arcName);
			return;
		}
		if (obj instanceof Cell)
		{
			Cell cell = (Cell)obj;
			infstr.append(cell.libDescribe());
			return;
		}
		if (obj instanceof PrimitiveNode)
		{
			PrimitiveNode np = (PrimitiveNode)obj;
			infstr.append(np.getTechnology().getTechName() + ":" + np.getName());
			return;
		}
		if (obj instanceof ArcProto)
		{
			ArcProto ap = (ArcProto)obj;
			infstr.append(ap.getTechnology().getTechName() + ":" + ap.getName());
			return;
		}
		if (obj instanceof Export)
		{
			Export pp = (Export)obj;
			infstr.append(((Cell)pp.getParent()).libDescribe() + ":" + pp.getName());
			return;
		}
	}

	/**
	 * Method to make a string from the value in "addr" which has a type in
	 * "type".
	 */
	private String getVarType(Object obj)
	{
		if (obj instanceof ArcInst) return "A";
		if (obj instanceof Boolean) return "B";
		if (obj instanceof Cell) return "C";
		if (obj instanceof Double) return "D";
		if (obj instanceof Export) return "E";
		if (obj instanceof Float) return "F";
		if (obj instanceof Long) return "G";
		if (obj instanceof Short) return "H";
		if (obj instanceof Integer) return "I";
		if (obj instanceof Library) return "L";
		if (obj instanceof NodeInst) return "N";
		if (obj instanceof Tool) return "O";
		if (obj instanceof PrimitiveNode) return "P";
		if (obj instanceof ArcProto) return "R";
		if (obj instanceof String) return "S";
		if (obj instanceof Technology) return "T";
		if (obj instanceof Point2D) return "V";
		if (obj instanceof Byte) return "Y";
		return null;
	}

	/**
	 * Method to add the string "str" to the infinite string and to quote the
	 * special characters '[', ']', '"', and '^'.
	 */
	private String convertString(String str)
	{
		StringBuffer infstr = new StringBuffer();
		int len = str.length();
		for(int i=0; i<len; i++)
		{
			char ch = str.charAt(i);
			if (ch == '"' || ch == '^')
				infstr.append('^');
			infstr.append(ch);
		}
		return infstr.toString();
	}
}
