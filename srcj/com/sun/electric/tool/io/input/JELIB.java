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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.ELIBConstants;
import com.sun.electric.tool.user.dialogs.OpenFile;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class reads files in new library file (.jelib) format.
 *
 * Things to do:
 *   Cell groups
 *   Variables
 *   Cell userbits
 *   Node userbits
 *   Arc userbits
 *   Export userbits
 */
public class JELIB extends LibraryFiles
{
	private static class CellContents
	{
		boolean filledIn;
		int lineNumber;
		List cellStrings;

		CellContents()
		{
			filledIn = false;
			cellStrings = new ArrayList();
		}
	}

	private HashMap allCells;

	JELIB()
	{
	}

	/**
	 * Method to read a Library in new library file (.jelib) format.
	 * @return true on error.
	 */
	protected boolean readLib()
	{
		try
		{
			return readTheLibrary();
		} catch (IOException e)
		{
			System.out.println("End of file reached while reading " + filePath);
			return true;
		}
	}

	/**
	 * Method to read the .elib file.
	 * Returns true on error.
	 */
	private boolean readTheLibrary()
		throws IOException
	{
		lib.erase();
		allCells = new HashMap();

		for(;;)
		{
			// get keyword from file
			String line = lineReader.readLine();
			if (line == null) break;

			// ignore blanks and comments
			if (line.length() == 0) continue;
			char first = line.charAt(0);
			if (first == '#') continue;

			if (first == 'H')
			{
				// parse header
				continue;
			}

			if (first == 'C')
			{
				// grab a cell description
				List pieces = parseLine(line);
				String name = pieces.get(0) + ";" + pieces.get(2) + "{" + pieces.get(1) + "}";
				Cell newCell = Cell.newInstance(lib, name);
				if (newCell == null)
				{
					System.out.println("Unable to create cell " + name);
				}
				Technology tech = Technology.findTechnology((String)pieces.get(3));
				newCell.setTechnology(tech);
				long cDate = Long.parseLong((String)pieces.get(4));
				long rDate = Long.parseLong((String)pieces.get(5));
				newCell.lowLevelSetCreationDate(new Date(cDate));
				newCell.lowLevelSetRevisionDate(new Date(rDate));

				// gather the contents of the cell into a list of Strings
				CellContents cc = new CellContents();
				cc.lineNumber = lineReader.getLineNumber();
				for(;;)
				{
					String nextLine = lineReader.readLine();
					if (nextLine == null) break;
					if (nextLine.length() == 0) continue;
					char nextFirst = nextLine.charAt(0);
					if (nextFirst == '#') continue;
					if (nextFirst == 'X') break;
					cc.cellStrings.add(nextLine);
				}

				// remember the contents of the cell for later
				allCells.put(newCell, cc);
				continue;
			}

			if (first == 'G')
			{
				// group information
				continue;
			}

			if (first == 'R')
			{
				// the main cell
				List pieces = parseLine(line);
				Cell mainCell = lib.findNodeProto((String)pieces.get(0));
				if (mainCell != null)
					lib.setCurCell(mainCell);
				continue;
			}
		}

		instantiateCellContents();

		lib.clearChangedMajor();
		lib.clearChangedMinor();
		return false;
	}

	private void instantiateCellContents()
	{
		// look through all cells
		for(Iterator it = allCells.keySet().iterator(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			CellContents cc = (CellContents)allCells.get(cell);
			if (cc.filledIn) continue;

			instantiateCellContent(cell, cc);
		}
	}
	
	private void instantiateCellContent(Cell cell, CellContents cc)
	{
		// place all nodes
		for(Iterator sIt = cc.cellStrings.iterator(); sIt.hasNext(); )
		{
			String cellString = (String)sIt.next();
			if (cellString.charAt(0) != 'N') continue;

			// parse the node line
			List pieces = parseLine(cellString);
			String protoName = (String)pieces.get(0);
			NodeProto np = null;
			if (protoName.indexOf(':') < 0) np = lib.findNodeProto(protoName); else
				np = NodeProto.findNodeProto(protoName);
			if (np == null)
			{
				System.out.println("Cell " + cell.describe() + ": cannot find node " + protoName);
				continue;
			}

			// make sure the subcell has been instantiated
			if (np instanceof Cell)
			{
				Cell subCell = (Cell)np;
				CellContents subCC = (CellContents)allCells.get(subCell);
				if (subCC != null)
				{
					if (!subCC.filledIn)
						instantiateCellContent(subCell, subCC);
				}
			}
			String nodeName = (String)pieces.get(1);
			double x = TextUtils.atof((String)pieces.get(2));
			double y = TextUtils.atof((String)pieces.get(3));
			double wid = TextUtils.atof((String)pieces.get(4));
			double hei = TextUtils.atof((String)pieces.get(5));
			int angle = TextUtils.atoi((String)pieces.get(6));
			NodeInst ni = NodeInst.newInstance(np, new Point2D.Double(x, y), wid, hei, angle, cell, nodeName);
			if (ni == null)
			{
				System.out.println("Cell " + cell.describe() + ": cannot create node " + protoName);
				continue;
			}

			// parse state information in field 7
			// add variables in fields 8 and up
		}

		// place all exports
		for(Iterator sIt = cc.cellStrings.iterator(); sIt.hasNext(); )
		{
			String cellString = (String)sIt.next();
			if (cellString.charAt(0) != 'E') continue;

			// parse the export line
			List pieces = parseLine(cellString);
			String exportName = (String)pieces.get(0);
			String nodeName = (String)pieces.get(1);
			NodeInst ni = cell.findNode(nodeName);
			if (ni == null)
			{
				System.out.println("Cell " + cell.describe() + ": cannot find export node instance " + nodeName);
				continue;
			}
			String portName = (String)pieces.get(2);
			PortInst pi = ni.getPortInst(0);
			if (portName.length() != 0)
				pi = ni.findPortInst(portName);

			Export pp = Export.newInstance(cell, pi, exportName);
			if (pp == null)
			{
				System.out.println("Cell " + cell.describe() + ": cannot create export " + exportName);
				continue;
			}

			// parse state information in field 7
			// add variables in fields 8 and up
		}

		// next place all arcs
		for(Iterator sIt = cc.cellStrings.iterator(); sIt.hasNext(); )
		{
			String cellString = (String)sIt.next();
			if (cellString.charAt(0) != 'A') continue;

			// parse the arc line
			List pieces = parseLine(cellString);
			String protoName = (String)pieces.get(0);
			ArcProto ap = ArcProto.findArcProto(protoName);
			if (ap == null)
			{
				System.out.println("Cell " + cell.describe() + ": cannot find arc " + protoName);
				continue;
			}
			String arcName = (String)pieces.get(1);
			double wid = TextUtils.atof((String)pieces.get(2));

			String headNodeName = (String)pieces.get(4);
			NodeInst headNI = cell.findNode(headNodeName);
			if (headNI == null)
			{
				System.out.println("Cell " + cell.describe() + ": cannot find head node instance " + headNodeName);
				continue;
			}
			String headPortName = (String)pieces.get(5);
			PortInst headPI = headNI.getPortInst(0);
			if (headPortName.length() != 0)
				headPI = headNI.findPortInst(headPortName);
			double headX = TextUtils.atof((String)pieces.get(6));
			double headY = TextUtils.atof((String)pieces.get(7));

			String tailNodeName = (String)pieces.get(8);
			NodeInst tailNI = cell.findNode(tailNodeName);
			if (tailNI == null)
			{
				System.out.println("Cell " + cell.describe() + ": cannot find tail node instance " + tailNodeName);
				continue;
			}
			String tailPortName = (String)pieces.get(9);
			PortInst tailPI = tailNI.getPortInst(0);
			if (tailPortName.length() != 0)
				tailPI = tailNI.findPortInst(tailPortName);
			double tailX = TextUtils.atof((String)pieces.get(10));
			double tailY = TextUtils.atof((String)pieces.get(11));

			ArcInst ai = ArcInst.newInstance(ap, wid, headPI, new Point2D.Double(headX, headY),
				tailPI, new Point2D.Double(tailX, tailY), arcName);
			if (ai == null)
			{
				System.out.println("Cell " + cell.describe() + ": cannot create arc " + protoName);
				continue;
			}

			// parse state information in field 7
			// add variables in fields 8 and up
		}
		cc.filledIn = true;
	}

	private List parseLine(String line)
	{
		List stringPieces = new ArrayList();
		int len = line.length();
		int pos = 1;
		int start = 1;
		while (pos < len)
		{
			char chr = line.charAt(pos);
			if (chr == '"')
			{
				for(;;)
				{
					pos++;
					chr = line.charAt(pos);
					if (chr == '"') break;
					if (chr == '^') pos++;
				}
			} else if (chr == '|')
			{
				String piece = line.substring(start, pos);
				stringPieces.add(piece);
				start = pos+1;
			}
			pos++;
		}
		String piece = line.substring(start, pos);
		stringPieces.add(piece);
		return stringPieces;
	}

	// --------------------------------- VARIABLE PARSING METHODS ---------------------------------

	/**
	 * get variables on current object (keyword "variables")
	 */
//	private void keywordGetVar()
//		throws IOException
//	{
//		ElectricObject naddr = null;
//		switch (varPos)
//		{
//			case INVTOOL:				// keyword applies to tools
//				naddr = curTool;
//				break;
//			case INVTECHNOLOGY:			// keyword applies to technologies
//				naddr = curTech;
//				break;
//			case INVLIBRARY:			// keyword applies to library
//				naddr = lib;
//				break;
//			case INVNODEPROTO:			// keyword applies to nodeproto
//				naddr = curNodeProto;
//				break;
//			case INVNODEINST:			// keyword applies to nodeinst
//				naddr = nodeInstList[curCellNumber].theNode[curNodeInstIndex];
//				break;
//			case INVPORTPROTO:			// keyword applies to portproto
//				naddr = exportList[curCellNumber].exportList[curExportIndex];
//				break;
//			case INVARCINST:			// keyword applies to arcinst
//				naddr = arcInstList[curCellNumber].arcList[curArcInstIndex];
//				break;
//		}
//
//		// find out how many variables to read
//		int count = Integer.parseInt(keyWord);
//		for(int i=0; i<count; i++)
//		{
//			// read the first keyword with the name, type, and descriptor
//			if (getKeyword())
//			{
//				System.out.println("EOF too soon");
//				return;
//			}
//
//			// get the variable name
//			String varName = "";
//			int len = keyWord.length();
//			if (keyWord.charAt(len-1) != ':')
//			{
//				System.out.println("Error on line "+lineReader.getLineNumber()+": missing colon in variable specification: "+keyWord);
//				return;
//			}
//			for(int j=0; j<len; j++)
//			{
//				char cat = keyWord.charAt(j);
//				if (cat == '^' && j < len-1)
//				{
//					j++;
//					varName += keyWord.charAt(j);
//					continue;
//				}
//				if (cat == '(' || cat == '[' || cat == ':') break;
//				varName += cat;
//			}
//			Variable.Key varKey = ElectricObject.newKey(varName);
//
//			// see if the variable is valid
//			boolean invalid = false;
//			if (naddr == null) invalid = true; else
//				invalid = naddr.isDeprecatedVariable(varKey);
//
//			// get type
//			int openSquarePos = keyWord.indexOf('[');
//			if (openSquarePos < 0)
//			{
//				System.out.println("Error on line "+lineReader.getLineNumber()+": missing type information in variable: " + keyWord);
//				return;
//			}
//			int type = TextUtils.atoi(keyWord, openSquarePos+1);
//
//			// get the descriptor
//			int commaPos = keyWord.indexOf(',');
//			int td0 = 0;
//			int td1 = 0;
//			if (commaPos >= 0)
//			{
//				td0 = TextUtils.atoi(keyWord, commaPos+1);
//				td1 = 0;
//				int slashPos = keyWord.indexOf('/');
//				if (slashPos >= 0)
//					td1 = TextUtils.atoi(keyWord, slashPos+1);
//			}
//			TextDescriptor td = new TextDescriptor(null, td0, td1, 0);
//
//			// get value
//			if (getKeyword())
//			{
//				System.out.println("EOF too soon");
//				return;
//			}
//			Object value = null;
//			if ((type&ELIBConstants.VISARRAY) == 0)
//			{
//				value = variableDecode(keyWord, type);
//			} else
//			{
//				if (keyWord.charAt(0) != '[')
//				{
//					System.out.println("Error on line "+lineReader.getLineNumber()+": missing '[' in list of variable values: " + keyWord);
//					return;
//				}
//				ArrayList al = new ArrayList();
//				int pos = 1;
//				len = keyWord.length();
//				for(;;)
//				{
//					// string arrays must be handled specially
//					int start = pos;
//					if ((type&ELIBConstants.VTYPE) == ELIBConstants.VSTRING)
//					{
//						while (keyWord.charAt(pos) != '"' && pos < len-1) pos++;
//						start = pos;
//						if (pos < len)
//						{
//							pos++;
//							for(;;)
//							{
//								if (keyWord.charAt(pos) == '^' && pos < len-1)
//								{
//									pos += 2;
//									continue;
//								}
//								if (keyWord.charAt(pos) == '"' || pos == len-1) break;
//								pos++;
//							}
//							if (pos < len) pos++;
//						}
//					} else
//					{
//						while (keyWord.charAt(pos) != ',' && keyWord.charAt(pos) != ']' && pos < len) pos++;
//					}
//					if (pos >= len)
//					{
//						System.out.println("Error on line "+lineReader.getLineNumber()+": array too short in variable values: " + keyWord);
//						return;
//					}
//					String entry = keyWord.substring(start, pos);
//					al.add(variableDecode(entry, type));
//					if (keyWord.charAt(pos) == ']') break;
//					if (keyWord.charAt(pos) != ',')
//					{
//						System.out.println("Error on line "+lineReader.getLineNumber()+": missing comma between array entries: " + keyWord);
//						return;
//					}
//					pos++;
//				}
//				int arrayLen = al.size();
//				switch (type&ELIBConstants.VTYPE)
//				{
//					case ELIBConstants.VADDRESS:
//					case ELIBConstants.VINTEGER:    value = new Integer[arrayLen];     break;
//					case ELIBConstants.VFRACT:      
//					case ELIBConstants.VFLOAT:      value = new Float[arrayLen];       break;
//					case ELIBConstants.VDOUBLE:     value = new Double[arrayLen];      break;
//					case ELIBConstants.VSHORT:      value = new Short[arrayLen];       break;
//					case ELIBConstants.VBOOLEAN:
//					case ELIBConstants.VCHAR:       value = new Byte[arrayLen];        break;
//					case ELIBConstants.VSTRING:     value = new String[arrayLen];      break;
//					case ELIBConstants.VNODEINST:   value = new NodeInst[arrayLen];    break;
//					case ELIBConstants.VNODEPROTO:  value = new NodeProto[arrayLen];   break;
//					case ELIBConstants.VARCPROTO:   value = new ArcProto[arrayLen];    break;
//					case ELIBConstants.VPORTPROTO:  value = new PortProto[arrayLen];   break;
//					case ELIBConstants.VARCINST:    value = new ArcInst[arrayLen];     break;
//					case ELIBConstants.VTECHNOLOGY: value = new Technology[arrayLen];  break;
//					case ELIBConstants.VLIBRARY:    value = new Library[arrayLen];     break;
//					case ELIBConstants.VTOOL:       value = new Tool[arrayLen];        break;
//				}
//				if (value != null)
//				{
//					for(int j=0; j<arrayLen; j++)
//					{
//						((Object [])value)[j] = al.get(j);
//					}
//				}
//			}
//
//			// Geometric names are saved as variables.
//			if (value instanceof String)
//			{
//				if ((naddr instanceof NodeInst && varKey == NodeInst.NODE_NAME) ||
//					(naddr instanceof ArcInst && varKey == ArcInst.ARC_NAME))
//				{
//					Geometric geom = (Geometric)naddr;
//					Input.fixTextDescriptorFont(td);
//					geom.setNameTextDescriptor(td);
//					Name name = makeGeomName(geom, value, type);
//					if (naddr instanceof NodeInst)
//						nodeInstList[curCellNumber].name[curNodeInstIndex] = name;
//					else
//						arcInstList[curCellNumber].arcInstName[curArcInstIndex] = name;
//					continue;
//				}
//			}
//			if (!invalid)
//			{
//				Variable var = naddr.newVar(varKey, value);
//				if (var == null)
//				{
//					System.out.println("Error on line "+lineReader.getLineNumber()+": cannot store array variable: " + keyWord);
//					return;
//				}
//				var.setTextDescriptor(td);
//				var.lowLevelSetFlags(type);
//
//				// handle updating of technology caches
////				if (naddr instanceof Technology)
////					changedtechnologyvariable(key);
//			}
//		}
//		if (varPos == INVLIBRARY)
//		{
//			// cache the font associations
//			Input.getFontAssociationVariable(lib);
//		}
//		Input.fixVariableFont(naddr);
//	}

//	private Object variableDecode(String name, int type)
//	{
//		int thistype = type;
//		if ((thistype&(ELIBConstants.VCODE1|ELIBConstants.VCODE2)) != 0) thistype = ELIBConstants.VSTRING;
//
//		switch (thistype&ELIBConstants.VTYPE)
//		{
//			case ELIBConstants.VINTEGER:
//			case ELIBConstants.VSHORT:
//			case ELIBConstants.VBOOLEAN:
//			case ELIBConstants.VADDRESS:
//				return new Integer(TextUtils.atoi(name));
//			case ELIBConstants.VFRACT:
//				return new Float(TextUtils.atoi(name) / 120.0f);
//			case ELIBConstants.VCHAR:
//				return new Character(name.charAt(0));
//			case ELIBConstants.VSTRING:
//				char [] letters = new char[name.length()];
//				int outpos = 0;
//				int inpos = 0;
//				if (name.charAt(inpos) == '"') inpos++;
//				for( ; inpos < name.length(); inpos++)
//				{
//					if (name.charAt(inpos) == '^' && inpos < name.length()-1)
//					{
//						inpos++;
//						letters[outpos++] = name.charAt(inpos);
//						continue;
//					}
//					if (name.charAt(inpos) == '"') break;
//					letters[outpos++] = name.charAt(inpos);
//				}
//				return new String(letters, 0, outpos);
//			case ELIBConstants.VFLOAT:
//				return new Float(Float.parseFloat(name));
//			case ELIBConstants.VDOUBLE:
//				return new Double(Double.parseDouble(name));
//			case ELIBConstants.VNODEINST:
//				int niIndex = TextUtils.atoi(name);
//				NodeInst ni = nodeInstList[curCellNumber].theNode[niIndex];
//				return ni;
//			case ELIBConstants.VNODEPROTO:
//				int colonPos = name.indexOf(':');
//				if (colonPos < 0)
//				{
//					// just an integer specification
//					int cindex = Integer.parseInt(name);
//					return allCellsArray[cindex];
//				} else
//				{
//					// parse primitive nodeproto name
//					NodeProto np = NodeProto.findNodeProto(name);
//					if (np == null)
//					{
//						System.out.println("Error on line "+lineReader.getLineNumber()+": cannot find node " + name);
//						return null;
//					}
//					return np;
//				}
//			case ELIBConstants.VPORTPROTO:
//				int ppIndex = TextUtils.atoi(name);
//				PortProto pp = exportList[curCellNumber].exportList[ppIndex];
//				return pp;
//			case ELIBConstants.VARCINST:
//				int aiIndex = TextUtils.atoi(name);
//				ArcInst ai = arcInstList[curCellNumber].arcList[aiIndex];
//				return ai;
//			case ELIBConstants.VARCPROTO:
//				return ArcProto.findArcProto(name);
//			case ELIBConstants.VTECHNOLOGY:
//				return Technology.findTechnology(name);
//			case ELIBConstants.VTOOL:
//				return Tool.findTool(name);
//		}
//		return null;
//	}
}
