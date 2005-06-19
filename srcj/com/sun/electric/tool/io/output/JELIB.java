/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JELIB.java
 * Input/output tool: JELIB Library output
 * Written by Steven M. Rubin, Sun Microsystems.
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

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Tool;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;


/**
 * Class to write a library to disk in new Electric-Library format.
 */
public class JELIB extends Output
{
	JELIB()
	{
	}

	/**
	 * Method to write a Library in Electric Library (.jelib) format.
	 * @param lib the Library to be written.
	 * @return true on error.
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
	 * @param lib the Library to write.
	 * @return true on error.
	 */
	private boolean writeTheLibrary(Library lib)
		throws IOException
	{
		// gather all referenced objects
		gatherReferencedObjects(lib, false);

		// write header information (library, version, main cell)
		printWriter.println("# header information:");
		printWriter.print("H" + convertString(lib.getName()) + "|" + Version.getVersion());
		printlnVars(lib, null);

		// write view information
		boolean viewHeaderPrinted = false;
		for(Iterator/*<View>*/ it = View.getViews(); it.hasNext(); )
		{
			View view = (View)it.next();
			if (!objInfo.containsKey(view)) continue;
			if (!viewHeaderPrinted)
			{
				printWriter.println();
				printWriter.println("# Views:");
				viewHeaderPrinted = true;
			}
			printWriter.println("V" + convertString(view.getFullName()) + "|" + convertString(view.getAbbreviation()));
		}

		// write external library information
		boolean libraryHeaderPrinted = false;
		for (Iterator/*<Library>*/ it = Library.getLibraries(); it.hasNext(); )
		{
			Library eLib = (Library)it.next();
			if (eLib == lib || !objInfo.containsKey(eLib)) continue;
			if (!libraryHeaderPrinted)
			{
				printWriter.println();
				printWriter.println("# External Libraries and cells:");
				libraryHeaderPrinted = true;
			}
			URL libUrl = eLib.getLibFile();
			String libFile = eLib.getName();
			if (libUrl != null)
			{
				String mainLibPath = TextUtils.getFilePath(lib.getLibFile());
				String thisLibPath = TextUtils.getFilePath(libUrl);
				if (!mainLibPath.equals(thisLibPath)) libFile = libUrl.toString();
			}
			printWriter.println();
			printWriter.println("L" + convertString(eLib.getName()) + "|" + convertString(libFile));
			for(Iterator cIt = eLib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				if (!objInfo.containsKey(cell)) continue;
				Rectangle2D bounds = cell.getBounds();
				printWriter.println("R" + convertString(cell.getCellName().toString()) +
					"|" + TextUtils.formatDouble(DBMath.round(bounds.getMinX()),0) +
					"|" + TextUtils.formatDouble(DBMath.round(bounds.getMaxX()),0) +
					"|" + TextUtils.formatDouble(DBMath.round(bounds.getMinY()),0) +
					"|" + TextUtils.formatDouble(DBMath.round(bounds.getMaxY()),0) +
					"|" + cell.getCreationDate().getTime() +
					"|" + cell.getRevisionDate().getTime());
				objInfo.put(cell, getFullCellName(cell));
				for (Iterator eIt = cell.getPorts(); eIt.hasNext(); )
				{
					Export export = (Export)eIt.next();
					//if (!externalObjs.contains(export)) continue;

					Poly poly = export.getOriginalPort().getPoly();
					printWriter.println("F" + convertString(export.getName()) +
						"|" + TextUtils.formatDouble(DBMath.round(poly.getCenterX()), 0) +
						"|" + TextUtils.formatDouble(DBMath.round(poly.getCenterY()), 0));
				}
			}
		}

		// write tool information
		boolean toolHeaderPrinted = false;
		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			if (Pref.getMeaningVariables(tool).size() == 0) continue;
			if (!toolHeaderPrinted)
			{
				printWriter.println();
				printWriter.println("# Tools:");
				toolHeaderPrinted = true;
			}
			printWriter.print("O" + convertString(tool.getName()));
			printlnMeaningPrefs(tool);
		}

		// write technology information
		boolean technologyHeaderPrinted = false;
		for (Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			if (!objInfo.containsKey(tech))	continue;
			if (!technologyHeaderPrinted)
			{
				printWriter.println();
				printWriter.println("# Technologies:");
				technologyHeaderPrinted = true;
			}
			printWriter.print("T" + convertString(tech.getTechName()));
			printlnMeaningPrefs(tech);

// 			for(Iterator nIt = tech.getNodes(); nIt.hasNext(); )
// 			{
// 				PrimitiveNode pn = (PrimitiveNode)nIt.next();
// 				if (!externalObjs.contains(pn)) continue;

// 				printWriter.println("D" + convertString(pn.getName()));
// 				for(Iterator pIt = pn.getPorts(); pIt.hasNext(); )
// 				{
// 					PrimitivePort pp = (PrimitivePort)pIt.next();
// 					if (!externalObjs.contains(pp)) continue;
// 					printWriter.println("P" + convertString(pp.getName()));
// 				}
// 			}
// 			for(Iterator aIt = tech.getArcs(); aIt.hasNext(); )
// 			{
// 				ArcProto ap = (ArcProto)aIt.next();
// 				if (!externalObjs.contains(ap)) continue;
// 				printWriter.println("W" + convertString(ap.getName()));
// 			}
		}

		// gather groups and put cell names into objInfo
		LinkedHashSet groups = new LinkedHashSet();
		for (Iterator cIt = lib.getCells(); cIt.hasNext(); )
		{
			Cell cell = (Cell)cIt.next();
			if (!groups.contains(cell.getCellGroup()))
				groups.add(cell.getCellGroup());
			objInfo.put(cell, convertString(cell.getCellName().toString()));
		}

		// write the cells of the database
		for (Iterator cIt = lib.getCells(); cIt.hasNext(); )
		{
			Cell cell = (Cell)cIt.next();

			// write the Cell name
			printWriter.println();
			printWriter.println("# Cell " + cell.noLibDescribe());
			printWriter.print("C" + convertString(cell.getCellName().toString()));
			printWriter.print("|" + convertString(cell.getTechnology().getTechName()));
			printWriter.print("|" + cell.getCreationDate().getTime());
			printWriter.print("|" + cell.getRevisionDate().getTime());
			StringBuffer cellBits = new StringBuffer();
			if (cell.isInCellLibrary()) cellBits.append("C");
			if (cell.isWantExpanded()) cellBits.append("E");
			if (cell.isInstancesLocked()) cellBits.append("I");
			if (cell.isAllLocked()) cellBits.append("L");
			if (cell.isInTechnologyLibrary()) cellBits.append("T");
			printWriter.print("|" + cellBits.toString());
			printlnVars(cell, cell);

			// write the nodes in this cell (sorted by node name)
			// write the nodes in this cell
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				NodeProto np = ni.getProto();
				if (np instanceof Cell)
				{
					printWriter.print("I" + objInfo.get(np));
				} else {
					PrimitiveNode prim = (PrimitiveNode)np;
					if (cell.getTechnology() == prim.getTechnology())
						printWriter.print("N" + convertString(prim.getName()));
					else
						printWriter.print("N" + convertString(prim.getFullName()));
				}
				String diskNodeName = getGeomName(ni);
				objInfo.put(ni, diskNodeName);
				printWriter.print("|" + diskNodeName + "|");
				if (!ni.getNameKey().isTempname())
					printWriter.print(describeDescriptor(null, ni.getTextDescriptor(NodeInst.NODE_NAME_TD)));
				printWriter.print("|" + TextUtils.formatDouble(ni.getAnchorCenterX(), 0));
				printWriter.print("|" + TextUtils.formatDouble(ni.getAnchorCenterY(), 0));
				if (np instanceof PrimitiveNode)
				{
					printWriter.print("|" + TextUtils.formatDouble(ni.getXSize(), 0));
					printWriter.print("|" + TextUtils.formatDouble(ni.getYSize(), 0));
				}
				printWriter.print('|');
				if (ni.isXMirrored()) printWriter.print('X');
				if (ni.isYMirrored()) printWriter.print('Y');
				int angle = ni.getAngle() % 3600;
				if (angle == 900 || angle == -2700) printWriter.print("R");
				else if (angle == 1800 || angle == -1800) printWriter.print("RR");
				else if (angle == 2700 || angle == -900) printWriter.print("RRR");
				else if (angle != 0) printWriter.print(angle);
				StringBuffer nodeBits = new StringBuffer();
				if (ni.isHardSelect()) nodeBits.append("A");
				if (ni.isExpanded()) nodeBits.append("E");
				if (ni.isLocked()) nodeBits.append("L");
//				if (ni.isShortened()) nodeBits.append("S");
				if (ni.isVisInside()) nodeBits.append("V");
				if (ni.isWiped()) nodeBits.append("W");
				int ts = ni.getTechSpecific();
				if (ts != 0) nodeBits.append(ts);
				printWriter.print("|" + nodeBits.toString());
				if (np instanceof Cell)
				{
					String tdString = describeDescriptor(null, ni.getTextDescriptor(NodeInst.NODE_PROTO_TD));
					printWriter.print("|" + tdString);
				}
				printlnVars(ni, cell);
			}

			// write the arcs in this cell
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ArcProto ap = ai.getProto();
				if (cell.getTechnology() == ap.getTechnology())
					printWriter.print("A" + convertString(ap.getName()));
				else
					printWriter.print("A" + convertString(ap.getFullName()));
				printWriter.print("|" + getGeomName(ai) + "|");
				//printWriter.print("|" + convertString(ai.getName()) + "|");
				if (!ai.getNameKey().isTempname())
					printWriter.print(describeDescriptor(null, ai.getTextDescriptor(ArcInst.ARC_NAME_TD)));
				printWriter.print("|" + TextUtils.formatDouble(ai.getWidth(), 0));
				StringBuffer arcBits = new StringBuffer();

				if (ai.isHardSelect()) arcBits.append("A");
				if (ai.isBodyArrowed()) arcBits.append("B");
				if (!ai.isFixedAngle()) arcBits.append("F");
				if (ai.isHeadNegated()) arcBits.append("G");
				if (!ai.isHeadExtended()) arcBits.append("I");
				if (!ai.isTailExtended()) arcBits.append("J");
				if (ai.isTailNegated()) arcBits.append("N");
				if (ai.isRigid()) arcBits.append("R");
				if (ai.isSlidable()) arcBits.append("S");
				if (ai.isHeadArrowed()) arcBits.append("X");
				if (ai.isTailArrowed()) arcBits.append("Y");
				printWriter.print("|" + arcBits.toString() + ai.getAngle());
				for(int e=1; e >= 0; e--)
//				for(int e=0; e<2; e++)
				{
					NodeInst ni = ai.getPortInst(e).getNodeInst();
					printWriter.print("|" + objInfo.get(ni) + "|");
					PortProto pp = ai.getPortInst(e).getPortProto();
					if (ni.getProto().getNumPorts() > 1)
						printWriter.print(convertString(pp.getName()));
					printWriter.print("|" + TextUtils.formatDouble(ai.getLocation(e).getX(), 0));
					printWriter.print("|" + TextUtils.formatDouble(ai.getLocation(e).getY(), 0));
				}
				printlnVars(ai, cell);
			}

			// write the exports in this cell
			for(Iterator it = cell.getPorts(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				printWriter.print("E" + convertString(pp.getName()));
				printWriter.print("|" + describeDescriptor(null, pp.getTextDescriptor(Export.EXPORT_NAME_TD)));

				PortInst subPI = pp.getOriginalPort();
				NodeInst subNI = subPI.getNodeInst();
				PortProto subPP = subPI.getPortProto();
				printWriter.print("|" + objInfo.get(subNI) + "|");
				if (subNI.getProto().getNumPorts() > 1)
					printWriter.print(convertString(subPP.getName()));
				printWriter.print("|" + pp.getCharacteristic().getShortName());
				if (pp.isAlwaysDrawn()) printWriter.print("/A");
				if (pp.isBodyOnly()) printWriter.print("/B");

				printlnVars(pp, cell);
			}

			// write the end-of-cell marker
			printWriter.println("X");
		}

		// write groups in alphabetical order
		printWriter.println();
		printWriter.println("# Groups:");
		for(Iterator it = groups.iterator(); it.hasNext(); )
		{
			Cell.CellGroup group = (Cell.CellGroup)it.next();
			printWriter.print("G");

			// if there is a main schematic cell, write that first
			Cell main = group.getMainSchematics();
			if (main != null)
			{
				printWriter.print(convertString(main.getCellName().toString()));
			}

			for(Iterator cIt = group.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				if (cell == main) continue;

				printWriter.print("|");
				printWriter.print((String)objInfo.get(cell));
			}
			printWriter.println();
		}

		// clean up and return
		lib.clearChangedMinor();
		lib.clearChangedMajor();
		lib.setFromDisk();
		System.out.println(filePath + " written");
		return false;
	}

	private String getGeomName(Geometric geom)
	{
		int duplicate = geom.getDuplicate();
		if (duplicate == 0)
			return convertString(geom.getName());
		else
			return "\"" + convertQuotedString(geom.getName()) + "\"" + duplicate;
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
		boolean display = false;
		if (var == null || var.isDisplay()) display = true;

		if (display)
		{
			// write size
			TextDescriptor.Size size = td.getSize();
			if (size.isAbsolute()) ret.append("A" + (int)size.getSize() + ";");

			// write bold
			if (td.isBold()) ret.append("B");

			// write color
			int color = td.getColorIndex();
			if (color != 0)
				ret.append("C" + color + ";");

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

			// write font
			int font = td.getFace();
			if (font != 0)
			{
				TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(font);
				ret.append("F" + convertString(af.toString()) + ";");
			}

			if (!size.isAbsolute()) ret.append("G" + TextUtils.formatDouble(size.getSize()) + ";");
		}

		// write inherit
		if (td.isInherit()) ret.append("H");

		if (display)
		{
			// write italic
			if (td.isItalic()) ret.append("I");

			// write underline
			if (td.isUnderline()) ret.append("L");

			// write display type
			TextDescriptor.DispPos dispPos = td.getDispPart();
			if (dispPos == TextDescriptor.DispPos.NAMEVALUE) ret.append("N");
		}

		// write language
		if (var != null && var.isCode() && (var.getObject() instanceof String || var.getObject() instanceof String[]))
		{
			TextDescriptor.Code codeType = var.getCode();
			if (codeType == TextDescriptor.Code.JAVA) ret.append("OJ"); else
			if (codeType == TextDescriptor.Code.LISP) ret.append("OL"); else
			if (codeType == TextDescriptor.Code.TCL) ret.append("OT");
		}

		// write parameter
		if (var != null && td.isParam()) ret.append("P");

		if (display)
		{
			// write rotation
			TextDescriptor.Rotation rot = td.getRotation();
			if (rot == TextDescriptor.Rotation.ROT90) ret.append("R"); else
			if (rot == TextDescriptor.Rotation.ROT180) ret.append("RR"); else
			if (rot == TextDescriptor.Rotation.ROT270) ret.append("RRR");
		}

		// write interior
		if (td.isInterior()) ret.append("T");

		// write units
		TextDescriptor.Unit unit = td.getUnit();
		if (unit == TextDescriptor.Unit.RESISTANCE) ret.append("UR"); else
		if (unit == TextDescriptor.Unit.CAPACITANCE) ret.append("UC"); else
		if (unit == TextDescriptor.Unit.INDUCTANCE) ret.append("UI"); else
		if (unit == TextDescriptor.Unit.CURRENT) ret.append("UA"); else
		if (unit == TextDescriptor.Unit.VOLTAGE) ret.append("UV"); else
		if (unit == TextDescriptor.Unit.DISTANCE) ret.append("UD"); else
		if (unit == TextDescriptor.Unit.TIME) ret.append("UT");

		if (display)
		{
			// write offset
			double offX = td.getXOff();
			if (offX != 0) ret.append("X" + TextUtils.formatDouble(offX, 0) + ";");
			double offY = td.getYOff();
			if (offY != 0) ret.append("Y" + TextUtils.formatDouble(offY, 0) + ";");
		}

		return ret.toString();
	}

	/**
	 * Method to write the variables on an object.
	 * If object is NodeInst write variables on its PortInsts also.
	 * The current cell is "curCell" such that any references to objects in a cell
	 * must be in this cell.
	 */
	private void printlnVars(ElectricObject eObj, Cell curCell)
	{
		// write the variables
		printVars(eObj, curCell);
		if (eObj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)eObj;
			for(Iterator it = ni.getPortInsts(); it.hasNext(); )
			{
				PortInst pi = (PortInst)it.next();
				if (pi.getNumVariables() != 0)
					printVars(pi, curCell);
			}
		}
		printWriter.println();
	}

	/**
	 * Method to write the variables on an object.  The current cell is
	 * "curCell" such that any references to objects in a cell must be in
	 * this cell.
	 */
	private void printVars(ElectricObject eObj, Cell curCell)
	{
		// write the variables
		for(Iterator it = eObj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
//			if (var.isDontSave()) continue;
			String tdString = describeDescriptor(var, var.getTextDescriptor());
			printWriter.print("|" + convertVariableName(diskName(var)) + "(" + tdString + ")");
			Object varObj = var.getObject();
			String pt = makeString(varObj, curCell);
			if (pt == null) pt = "";
			printWriter.print(pt);
		}
	}

	/**
	 * Method to write the meaning preferences on an object.
	 */
	private void printlnMeaningPrefs(Object obj)
	{
		List prefs = Pref.getMeaningVariables(obj);
		for(Iterator it = prefs.iterator(); it.hasNext(); )
		{
			Pref pref = (Pref)it.next();
			Object value = pref.getValue();
			printWriter.print("|" + convertVariableName(pref.getPrefName()) + "()" + makeString(value, null));
		}
		printWriter.println();
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
					infstr.append(getVarType(obj));
					infstr.append("[");
				}					
				makeStringVar(infstr, oneObj, curCell, true);
			}
			infstr.append("]");
		} else
		{
			infstr.append(getVarType(obj));
			makeStringVar(infstr, obj, curCell, false);
		}
		return infstr.toString();
	}

	/**
	 * Method to make a string from the value in "addr" which has a type in
	 * "type".
	 */
	private void makeStringVar(StringBuffer infstr, Object obj, Cell curCell, boolean inArray)
	{
		if (obj == null) return;
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
			infstr.append(convertString((String)obj, inArray));
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
			infstr.append(convertString(tech.getTechName(), inArray));
			return;
		}
		if (obj instanceof Library)
		{
			Library lib = (Library)obj;
			infstr.append(convertString(lib.getName(), inArray));
			return;
		}
		if (obj instanceof Tool)
		{
			Tool tool = (Tool)obj;
			infstr.append(convertString(tool.getName(), inArray));
			return;
		}
		if (obj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)obj;
			infstr.append(convertString(getFullCellName(ni.getParent()) + ":" + ni.getName(), inArray));
			return;
		}
		if (obj instanceof ArcInst)
		{
			ArcInst ai = (ArcInst)obj;
			String arcName = ai.getName();
			if (arcName == null)
			{
				System.out.println("Cannot save pointer to unnamed ArcInst: " + ai.getParent().describe(true) + ":" + ai.describe(true));
			}
			infstr.append(convertString(getFullCellName(ai.getParent()) + ":" + arcName, inArray));
			return;
		}
		if (obj instanceof Cell)
		{
			Cell cell = (Cell)obj;
			infstr.append(convertString(getFullCellName(cell), inArray));
			return;
		}
		if (obj instanceof PrimitiveNode)
		{
			PrimitiveNode np = (PrimitiveNode)obj;
			infstr.append(convertString(np.getFullName(), inArray));
			return;
		}
		if (obj instanceof ArcProto)
		{
			ArcProto ap = (ArcProto)obj;
			infstr.append(convertString(ap.getFullName(), inArray));
			return;
		}
		if (obj instanceof Export)
		{
			Export pp = (Export)obj;
			infstr.append(convertString(getFullCellName((Cell)pp.getParent()) + ":" + pp.getName(), inArray));
			return;
		}
	}

	private String getFullCellName(Cell cell)
	{
		return convertString(cell.getLibrary().getName() + ":" + cell.getCellName());
	}

	/**
	 * Method to make a string from the value in "addr" which has a type in
	 * "type".
	 */
	private String getVarType(Object obj)
	{
		if (obj == null) return "X";
		if (obj instanceof ArcInst       || obj instanceof ArcInst [])       return "S"; // "A"
		if (obj instanceof Boolean       || obj instanceof Boolean [])       return "B";
		if (obj instanceof Cell          || obj instanceof Cell [])          return "C";
		if (obj instanceof Double        || obj instanceof Double [])        return "D";
		if (obj instanceof Export        || obj instanceof Export [])        return "E";
		if (obj instanceof Float         || obj instanceof Float [])         return "F";
		if (obj instanceof Long          || obj instanceof Long [])          return "G";
		if (obj instanceof Short         || obj instanceof Short [])         return "H";
		if (obj instanceof Integer       || obj instanceof Integer [])       return "I";
		if (obj instanceof Library       || obj instanceof Library [])       return "L";
		if (obj instanceof NodeInst      || obj instanceof NodeInst [])      return "S"; // "N"
		if (obj instanceof Tool          || obj instanceof Tool [])          return "O";
		if (obj instanceof PrimitiveNode || obj instanceof PrimitiveNode []) return "P";
		if (obj instanceof ArcProto      || obj instanceof ArcProto [])      return "R";
		if (obj instanceof String        || obj instanceof String [])        return "S";
		if (obj instanceof Technology    || obj instanceof Technology [])    return "T";
		if (obj instanceof Point2D       || obj instanceof Point2D [])       return "V";
		if (obj instanceof Byte          || obj instanceof Byte [])          return "Y";
		return null;
	}

	/**
	 * Method convert a string that is going to be quoted.
	 * Inserts the quote character (^) before any quotation character (") or quote character (^) in the string.
	 * Converts newlines to "^\n" (makeing the "\" and "n" separate characters).
	 * @param str the string to convert.
	 * @return the string with the appropriate quote characters.
	 * If no conversion is necessary, the input string is returned.
	 */
	private String convertQuotedString(String str)
	{
		StringBuffer infstr = new StringBuffer();
		int len = str.length();
		for(int i=0; i<len; i++)
		{
			char ch = str.charAt(i);
			if (ch == '\n') { infstr.append("\\n");   continue; }
			if (ch == '\r') { infstr.append("\\r");   continue; }
			if (ch == '"' || ch == '\\')
				infstr.append('\\');
			infstr.append(ch);
		}
		return infstr.toString();
	}

	/**
	 * Method convert a string that is not going to be quoted.
	 * Inserts a quote character (^) before any separator (|), quotation character (") or quote character (^) in the string.
	 * Converts newlines to spaces.
	 * @param str the string to convert.
	 * @return the string with the appropriate quote characters.
	 * If no conversion is necessary, the input string is returned.
	 */
	private String convertString(String str)
	{
		return convertString(str, (char)0, (char)0);
	}

	/**
	 * Method convert a string that is not going to be quoted.
	 * Inserts a quote character (^) before any separator (|), quotation character (") or quote character (^) in the string.
	 * Converts newlines to spaces.
	 * @param str the string to convert.
	 * @return the string with the appropriate quote characters.
	 * If no conversion is necessary, the input string is returned.
	 */
	private String convertString(String str, char delim1, char delim2)
	{
		if (str.length() != 0 &&
			str.indexOf('\n') < 0 &&
			str.indexOf('\r') < 0 &&
			str.indexOf('\\') < 0 &&
			str.indexOf('"') < 0 &&
			str.indexOf('|') < 0 &&
			(delim1 == 0 || str.indexOf(delim1) < 0) &&
			(delim2 == 0 || str.indexOf(delim2) < 0))
			return str;
		return '"' + convertQuotedString(str) + '"';
	}

	/**
	 * Method convert a string that is a variable name.
	 * If string contains end-of-lines, backslashes, bars, quotation characters, open parenthesis,
	 * encloses string in quotational characters.
	 * @param str the string to convert.
	 * @return the string with the appropriate quote characters.
	 * If no conversion is necessary, the input string is returned.
	 */
	private String convertVariableName(String str)
	{
		return convertString(str, '(', (char)0);
	}

	/**
	 * Method convert a string that is a variable value.
	 * If string contains end-of-lines, backslashes, bars, quotation characters, comma,
	 * close bracket, returns string enclosed in quotational characters.
	 * @param str the string to convert.
	 * @param inArray true if string is element of array/
	 * @return the string with the appropriate quote characters.
	 * If no conversion is necessary, the input string is returned.
	 */
	private String convertString(String str, boolean inArray)
	{
		return inArray ? convertString(str, ',', ']') : convertString(str, (char)0, (char)0);
	}
}
