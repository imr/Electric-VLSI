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
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.PrimitiveArc;
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

	private static HashMap allCells;
	private String version;
	private String curLibName;

	JELIB()
	{
	}

	/**
	 * Ugly hack.
	 * Overrides LibraryFiles so that recursive library reads get to the proper place.
	 */
	public boolean readInputLibrary()
	{
		if (topLevelLibrary) allCells = new HashMap();
		boolean error = readLib();
		if (topLevelLibrary)
		{
			if (!error) instantiateCellContents();
		}
		return error;
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
		curLibName = null;
		version = null;
		String mainCell = "";
		Technology curTech = null;
		for(;;)
		{
			// get keyword from file
			String line = lineReader.readLine();
			if (line == null) break;

			// ignore blanks and comments
			if (line.length() == 0) continue;
			char first = line.charAt(0);
			if (first == '#') continue;

			if (first == 'C')
			{
				// grab a cell description
				List pieces = parseLine(line);
				String name = pieces.get(0) + ";" + pieces.get(2) + "{" + pieces.get(1) + "}";
				Cell newCell = Cell.newInstance(lib, name);
				if (newCell == null)
				{
					System.out.println(filePath + ", line " + lineReader.getLineNumber() + ", Unable to create cell " + name);
				}
				Technology tech = Technology.findTechnology((String)pieces.get(3));
				newCell.setTechnology(tech);
				long cDate = Long.parseLong((String)pieces.get(4));
				long rDate = Long.parseLong((String)pieces.get(5));
				newCell.lowLevelSetCreationDate(new Date(cDate));
				newCell.lowLevelSetRevisionDate(new Date(rDate));

				// parse state information in field 6
				String stateInfo = (String)pieces.get(6);
				boolean expanded = false, allLocked = false, instLocked = false,
					cellLib = false, techLib = false;
				for(int i=0; i<stateInfo.length(); i++)
				{
					char chr = stateInfo.charAt(i);
					if (chr == 'E') expanded = true; else
					if (chr == 'L') allLocked = true; else
					if (chr == 'I') instLocked = true; else
					if (chr == 'C') cellLib = true; else
					if (chr == 'T') techLib = true;
				}
				if (expanded) newCell.setWantExpanded(); else newCell.clearWantExpanded();
				if (allLocked) newCell.setAllLocked(); else newCell.clearAllLocked();
				if (instLocked) newCell.setInstancesLocked(); else newCell.clearInstancesLocked();
				if (cellLib) newCell.setInCellLibrary(); else newCell.clearInCellLibrary();
				if (techLib) newCell.setInTechnologyLibrary(); else newCell.clearInTechnologyLibrary();

				// add variables in fields 7 and up
				addVariables(newCell, pieces, 7, filePath, lineReader.getLineNumber());

				// gather the contents of the cell into a list of Strings
				CellContents cc = new CellContents();
				cc.lineNumber = lineReader.getLineNumber() + 1;
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

			if (first == 'L')
			{
				// cross-library reference
				List pieces = parseLine(line);
				String libName = (String)pieces.get(0);
				Library elib = Library.findLibrary(libName);
				if (elib != null) continue;

				// recurse
				elib = readExternalLibraryFromFilename((String)pieces.get(1), OpenFile.Type.JELIB);
				continue;
			}

			if (first == 'H')
			{
				// parse header
				List pieces = parseLine(line);
				curLibName = (String)pieces.get(0);
				version = (String)pieces.get(1);
				mainCell = (String)pieces.get(2);
				continue;
			}

			if (first == 'O')
			{
				// parse Tool information
				List pieces = parseLine(line);
				String toolName = (String)pieces.get(0);
				Tool tool = Tool.findTool(toolName);
				if (tool == null)
				{
					System.out.println(filePath + ", line " + lineReader.getLineNumber() + ", Cannot identify tool " + toolName);
					continue;
				}

				// get additional variables starting at position 1
				addVariables(tool, pieces, 1, filePath, lineReader.getLineNumber());
				continue;
			}

			if (first == 'T')
			{
				// parse Technology information
				List pieces = parseLine(line);
				String techName = (String)pieces.get(0);
				curTech = Technology.findTechnology(techName);
				if (curTech == null)
				{
					System.out.println(filePath + ", line " + lineReader.getLineNumber() + ", Cannot identify technology " + techName);
					continue;
				}

				// get additional variables starting at position 1
				addVariables(curTech, pieces, 1, filePath, lineReader.getLineNumber());
				continue;
			}

			if (first == 'N')
			{
				// parse PrimitiveNode information
				List pieces = parseLine(line);
				String primName = (String)pieces.get(0);
				if (curTech == null)
				{
					System.out.println(filePath + ", line " + lineReader.getLineNumber() + ", Primitive node " + primName + " has no technology before it");
					continue;
				}
				PrimitiveNode np = curTech.findNodeProto(primName);
				if (np == null)
				{
					System.out.println(filePath + ", line " + lineReader.getLineNumber() + ", Cannot identify primitive node " + primName);
					continue;
				}

				// get additional variables starting at position 1
				addVariables(np, pieces, 1, filePath, lineReader.getLineNumber());
				continue;
			}

			if (first == 'A')
			{
				// parse ArcProto information
				List pieces = parseLine(line);
				String arcName = (String)pieces.get(0);
				if (curTech == null)
				{
					System.out.println(filePath + ", line " + lineReader.getLineNumber() + ", Primitive arc " + arcName + " has no technology before it");
					continue;
				}
				PrimitiveArc ap = curTech.findArcProto(arcName);
				if (ap == null)
				{
					System.out.println(filePath + ", line " + lineReader.getLineNumber() + ", Cannot identify primitive arc " + arcName);
					continue;
				}

				// get additional variables starting at position 1
				addVariables(ap, pieces, 1, filePath, lineReader.getLineNumber());
				continue;
			}

			if (first == 'G')
			{
				// group information
				List pieces = parseLine(line);
				Cell firstCell = null;
				for(int i=0; i<pieces.size(); i++)
				{
					String cellName = (String)pieces.get(i);
					Cell cell = lib.findNodeProto(cellName);
					if (cell == null)
					{
						System.out.println(filePath + ", line " + lineReader.getLineNumber() + ", Cannot find cell " + cellName);
						break;
					}
					if (i == 0)
					{
						firstCell = cell;
						cell.putInOwnCellGroup();
					} else
					{
						cell.joinGroup(firstCell);
					}
				}
				continue;
			}
		}

		// set the main cell
		if (mainCell.length() > 0)
		{
			Cell main = lib.findNodeProto(mainCell);
			if (main != null)
				lib.setCurCell(main);
		}

		lib.clearChangedMajor();
		lib.clearChangedMinor();
		return false;
	}

	/**
	 * Method called after all libraries have been read.
	 * Instantiates all of the Cell contents that were saved in "allCells".
	 */
	private void instantiateCellContents()
	{
		// look through all cells
		System.out.println("Creating the circuitry...");
		for(Iterator it = allCells.keySet().iterator(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			CellContents cc = (CellContents)allCells.get(cell);
			if (cc.filledIn) continue;

			instantiateCellContent(cell, cc);
		}
	}

	/**
	 * Method called after all libraries have been read to instantiate a single Cell.
	 * @param cell the Cell to instantiate.
	 * @param cc the contents of that cell (the strings from the file).
	 */
	private void instantiateCellContent(Cell cell, CellContents cc)
	{
		int numStrings = cc.cellStrings.size();
		String curFile = cell.getLibrary().getLibFile().toString();

		// place all nodes
		for(int line=0; line<numStrings; line++)
		{
			String cellString = (String)cc.cellStrings.get(line);
			if (cellString.charAt(0) != 'N') continue;

			// parse the node line
			List pieces = parseLine(cellString);
			String protoName = (String)pieces.get(0);
			NodeProto np = null;
			int colonPos = protoName.indexOf(':');
			if (colonPos < 0) np = lib.findNodeProto(protoName); else
			{
				String prefixName = protoName.substring(0, colonPos);
				if (prefixName.equals(curLibName)) np = lib.findNodeProto(protoName.substring(colonPos+1)); else
				{
					np = NodeProto.findNodeProto(protoName);
				}
			}
			if (np == null)
			{
				System.out.println(curFile + ", line " + (cc.lineNumber + line) + " (cell " + cell.describe() + ") cannot find node " + protoName);
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
				System.out.println(curFile + ", line " + (cc.lineNumber + line) + " (cell " + cell.describe() + ") cannot create node " + protoName);
				continue;
			}

			// parse state information in field 7
			String stateInfo = (String)pieces.get(7);
			boolean expanded = false, locked = false, shortened = false,
				visInside = false, wiped = false, hardSelect = false;
			for(int i=0; i<stateInfo.length(); i++)
			{
				char chr = stateInfo.charAt(i);
				if (chr == 'E') expanded = true; else
				if (chr == 'L') locked = true; else
				if (chr == 'S') shortened = true; else
				if (chr == 'V') visInside = true; else
				if (chr == 'W') wiped = true; else
				if (chr == 'A') hardSelect = true; else
				if (Character.isDigit(chr))
				{
					int techSpecific = TextUtils.atoi(stateInfo.substring(i));
					ni.setTechSpecific(techSpecific);
					break;
				}
			}
			if (expanded) ni.setExpanded(); else ni.clearExpanded();
			if (locked) ni.setLocked(); else ni.clearLocked();
			if (shortened) ni.setShortened(); else ni.clearShortened();
			if (visInside) ni.setVisInside(); else ni.clearVisInside();
			if (wiped) ni.setWiped(); else ni.clearWiped();
			if (hardSelect) ni.setHardSelect(); else ni.clearHardSelect();

			// get text descriptor for cell instance names
			String textDescriptorInfo = (String)pieces.get(8);
			loadTextDescriptor(ni.getProtoTextDescriptor(), null, textDescriptorInfo, curFile, cc.lineNumber + line);

			// add variables in fields 9 and up
			addVariables(ni, pieces, 9, curFile, cc.lineNumber + line);
		}

		// place all exports
		for(int line=0; line<numStrings; line++)
		{
			String cellString = (String)cc.cellStrings.get(line);
			if (cellString.charAt(0) != 'E') continue;

			// parse the export line
			List pieces = parseLine(cellString);
			String exportName = (String)pieces.get(0);
			String nodeName = (String)pieces.get(1);
			NodeInst ni = cell.findNode(nodeName);
			if (ni == null)
			{
				System.out.println(curFile + ", line " + (cc.lineNumber + line) + " (cell " + cell.describe() + ") cannot find export node instance " + nodeName);
				continue;
			}
			String portName = (String)pieces.get(2);
			PortInst pi = ni.getPortInst(0);
			if (portName.length() != 0)
				pi = ni.findPortInst(portName);

			// create the export
			Export pp = Export.newInstance(cell, pi, exportName);
			if (pp == null)
			{
				System.out.println(curFile + ", line " + (cc.lineNumber + line) + " (cell " + cell.describe() + ") cannot create export " + exportName);
				continue;
			}

			// get text descriptor in field 3
			String textDescriptorInfo = (String)pieces.get(3);
			loadTextDescriptor(pp.getTextDescriptor(), null, textDescriptorInfo, curFile, cc.lineNumber + line);

			// parse state information in field 4
			String stateInfo = (String)pieces.get(4);
			int slashPos = stateInfo.indexOf('/');
			if (slashPos >= 0)
			{
				String extras = stateInfo.substring(slashPos);
				stateInfo = stateInfo.substring(0, slashPos);
				while (extras.length() > 0)
				{
					if (extras.charAt(1) == 'A') pp.setAlwaysDrawn(); else
					if (extras.charAt(1) == 'B') pp.setBodyOnly();
					extras = extras.substring(2);
				}
			}
			PortProto.Characteristic ch = PortProto.Characteristic.findCharacteristicShort(stateInfo);
			pp.setCharacteristic(ch);

			// add variables in fields 5 and up
			addVariables(pp, pieces, 5, curFile, cc.lineNumber + line);
		}

		// next place all arcs
		for(int line=0; line<numStrings; line++)
		{
			String cellString = (String)cc.cellStrings.get(line);
			if (cellString.charAt(0) != 'A') continue;

			// parse the arc line
			List pieces = parseLine(cellString);
			String protoName = (String)pieces.get(0);
			ArcProto ap = ArcProto.findArcProto(protoName);
			if (ap == null)
			{
				System.out.println(curFile + ", line " + (cc.lineNumber + line) + " (cell " + cell.describe() + ") cannot find arc " + protoName);
				continue;
			}
			String arcName = (String)pieces.get(1);
			double wid = TextUtils.atof((String)pieces.get(2));

			String headNodeName = (String)pieces.get(4);
			NodeInst headNI = cell.findNode(headNodeName);
			if (headNI == null)
			{
				System.out.println(curFile + ", line " + (cc.lineNumber + line) + " (cell " + cell.describe() + ") cannot find head node instance " + headNodeName);
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
				System.out.println(curFile + ", line " + (cc.lineNumber + line) + " (cell " + cell.describe() + ") cannot find tail node instance " + tailNodeName);
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
				System.out.println(curFile + ", line " + (cc.lineNumber + line) + " (cell " + cell.describe() + ") cannot create arc " + protoName);
				continue;
			}

			// parse state information in field 3
			String stateInfo = (String)pieces.get(3);
			boolean rigid = false, fixedAngle = true, slidable = false,
				extended = true, directional = false, reverseEnds = false,
				hardSelect = false, skipHead = false, skipTail = false,
				tailNegated = false, headNegated = false;
			for(int i=0; i<stateInfo.length(); i++)
			{
				char chr = stateInfo.charAt(i);
				if (chr == 'R') rigid = true; else
				if (chr == 'F') fixedAngle = false; else
				if (chr == 'S') slidable = true; else
				if (chr == 'E') extended = false; else
				if (chr == 'D') directional = true; else
				if (chr == 'V') reverseEnds = true; else
				if (chr == 'A') hardSelect = true; else
				if (chr == 'H') skipHead = true; else
				if (chr == 'T') skipTail = true; else
				if (chr == 'N') tailNegated = true; else
				if (chr == 'G') headNegated = true; else
				if (Character.isDigit(chr))
				{
					int angle = TextUtils.atoi(stateInfo.substring(i));
					ai.setAngle(angle);
					break;
				}
			}
			ai.setRigid(rigid);
			ai.setFixedAngle(fixedAngle);
			ai.setSlidable(slidable);
			ai.setExtended(extended);
			ai.setDirectional(directional);
			ai.setReverseEnds(reverseEnds);
			ai.setHardSelect(hardSelect);
			ai.setSkipHead(skipHead);
			ai.setSkipTail(skipTail);
			ai.getHead().setNegated(headNegated);
			ai.getTail().setNegated(tailNegated);

			// add variables in fields 12 and up
			addVariables(ai, pieces, 12, curFile, cc.lineNumber + line);
		}
		cc.filledIn = true;
	}

	/**
	 * Method to parse a line from the file, breaking it into a List of Strings.
	 * Each field in the file is separated by "|".
	 * Quoted strings are handled properly, as are the escape character, "^".
	 * @param line the text from the file.
	 * @return a List of Strings.
	 */
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

	/**
	 * Method to add variables to an ElectricObject from a List of strings.
	 * @param eObj the ElectricObject to augment with Variables.
	 * @param pieces the array of Strings that described the ElectricObject.
	 * @param position the index in the array of strings where Variable descriptions begin.
	 * @param fileName the name of the file that this came from (for error reporting).
	 * @param lineNumber the line number in the file that this came from (for error reporting).
	 */
	private void addVariables(ElectricObject eObj, List pieces, int position, String fileName, int lineNumber)
	{
		int total = pieces.size();
		for(int i=position; i<total; i++)
		{
			String piece = (String)pieces.get(i);
			int openPos = piece.indexOf('(');
			if (openPos < 0)
			{
				System.out.println(fileName + ", line " + lineNumber + ", Badly formed variable (no open parenthesis): " + piece);
				continue;
			}
			String varName = piece.substring(0, openPos);
			Variable.Key varKey = ElectricObject.newKey(varName);
			if (eObj.isDeprecatedVariable(varKey)) continue;
			int closePos = piece.indexOf(')', openPos);
			if (closePos < 0)
			{
				System.out.println(fileName + ", line " + lineNumber + ", Badly formed variable (no close parenthesis): " + piece);
				continue;
			}
			String varBits = piece.substring(openPos+1, closePos);
			int objectPos = closePos + 1;
			if (objectPos >= piece.length())
			{
				System.out.println(fileName + ", line " + lineNumber + ", Variable type missing: " + piece);
				continue;
			}
			char varType = piece.charAt(objectPos++);
			if (objectPos >= piece.length())
			{
				System.out.println(fileName + ", line " + lineNumber + ", Variable value missing: " + piece);
				continue;
			}
			Object obj = null;
			if (piece.charAt(objectPos) == '[')
			{
				List objList = new ArrayList();
				objectPos++;
				while (objectPos < piece.length())
				{
					if (piece.charAt(objectPos) == ']') break;
					int start = objectPos;
					boolean inQuote = false;
					while (objectPos < piece.length())
					{
						if (inQuote)
						{
							if (piece.charAt(objectPos) == '^')
							{
								objectPos++;
							} else if (piece.charAt(objectPos) == '"')
							{
								inQuote = false;
							}
							objectPos++;
							continue;
						}
						if (piece.charAt(objectPos) == ',') break;
						if (piece.charAt(objectPos) == '"')
						{
							inQuote = true;
						}
						objectPos++;
					}
					Object oneObj = getVariableValue(piece.substring(start, objectPos), 0, varType, fileName, lineNumber);
					objList.add(oneObj);
					objectPos++;
				}
				int limit = objList.size();
				Object [] objArray = null;
				switch (varType)
				{
					case 'A': objArray = new ArcInst[limit];        break;
					case 'B': objArray = new Boolean[limit];        break;
					case 'C': objArray = new Cell[limit];           break;
					case 'D': objArray = new Double[limit];         break;
					case 'E': objArray = new Export[limit];         break;
					case 'F': objArray = new Float[limit];          break;
					case 'G': objArray = new Long[limit];           break;
					case 'H': objArray = new Short[limit];          break;
					case 'I': objArray = new Integer[limit];        break;
					case 'L': objArray = new Library[limit];        break;
					case 'N': objArray = new NodeInst[limit];       break;
					case 'O': objArray = new Tool[limit];           break;
					case 'P': objArray = new PrimitiveNode[limit];  break;
					case 'R': objArray = new ArcProto[limit];       break;
					case 'S': objArray = new String[limit];         break;
					case 'T': objArray = new Technology[limit];     break;
					case 'V': objArray = new Point2D[limit];        break;
					case 'Y': objArray = new Byte[limit];           break;
				}
				for(int j=0; j<limit; j++)
					objArray[j] = objList.get(j);
				obj = objArray;
			} else
			{
				// a scalar Variable
				obj = getVariableValue(piece, objectPos, varType, fileName, lineNumber);
			}

			// create the variable
			Variable newVar = eObj.newVar(varKey, obj);
			if (newVar == null)
			{
				System.out.println(fileName + ", line " + lineNumber + ", Cannot create variable: " + piece);
				continue;	
			}			

			// see if the variable is a "meaning option"
			if (topLevelLibrary)
			{
				Pref.Meaning meaning = Pref.getMeaningVariable(eObj, varName);
				if (meaning != null) Pref.changedMeaningVariable(meaning);
			}

			// add in extra information
			TextDescriptor td = newVar.getTextDescriptor();
			loadTextDescriptor(td, newVar, varBits, fileName, lineNumber);
		}
	}

	/**
	 * Cell redFour:nor2en_2p{sch}, node redFour:nor2en_2p{ic}[nor2en_2@0] is 5.0x4.25, but prototype is 10.48 x 12.0 ****REPAIRED****
	 * file:/C:/DevelE/Electric/TESTLIBS/ivan/redFour.jelib, line 4038 (cell redFour:nor2en_2p{sch}) cannot create node redFour:nor2en_2p;1{ic}

	 * Method to load a TextDescriptor from a String description of it.
	 * @param td the TextDescriptor to load.
	 * @param var the Variable that this TextDescriptor resides on.
	 * It may be null if the TextDescriptor is on a NodeInst or Export.
	 * @param varBits the String that describes the TextDescriptor.
	 * @param fileName the name of the file that this came from (for error reporting).
	 * @param lineNumber the line number in the file that this came from (for error reporting).
	 */
	private void loadTextDescriptor(TextDescriptor td, Variable var, String varBits, String fileName, int lineNumber)
	{
		for(int j=0; j<varBits.length(); j++)
		{
			char varBit = varBits.charAt(j);
			switch (varBit)
			{
				case 'D':		// display position
					if (var != null) var.setDisplay(true);
					j++;
					if (j >= varBits.length())
					{
						System.out.println(fileName + ", line " + lineNumber + ", Incorrect display specification: " + varBits);
						break;
					}
					switch (varBits.charAt(j))
					{
						case '5': td.setPos(TextDescriptor.Position.CENT);       break;
						case '8': td.setPos(TextDescriptor.Position.UP);         break;
						case '2': td.setPos(TextDescriptor.Position.DOWN);       break;
						case '4': td.setPos(TextDescriptor.Position.LEFT);       break;
						case '6': td.setPos(TextDescriptor.Position.RIGHT);      break;
						case '7': td.setPos(TextDescriptor.Position.UPLEFT);     break;
						case '9': td.setPos(TextDescriptor.Position.UPRIGHT);    break;
						case '1': td.setPos(TextDescriptor.Position.DOWNLEFT);   break;
						case '3': td.setPos(TextDescriptor.Position.DOWNRIGHT);  break;
						case '0': td.setPos(TextDescriptor.Position.BOXED);      break;
					}
					break;
				case 'N':		// display type
					td.setDispPart(TextDescriptor.DispPos.NAMEVALUE);
					break;
				case 'A':		// absolute text size
					int semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						System.out.println(fileName + ", line " + lineNumber + ", Bad absolute size (semicolon missing): " + varBits);
						break;	
					}
					td.setAbsSize(TextUtils.atoi(varBits.substring(j+1, semiPos)));
					j = semiPos;
					break;
				case 'G':		// relative text size
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						System.out.println(fileName + ", line " + lineNumber + ", Bad relative size (semicolon missing): " + varBits);
						break;	
					}
					td.setRelSize(TextUtils.atof(varBits.substring(j+1, semiPos)));
					j = semiPos;
					break;
				case 'X':		// X offset
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						System.out.println(fileName + ", line " + lineNumber + ", Bad X offset (semicolon missing): " + varBits);
						break;	
					}
					td.setOff(TextUtils.atof(varBits.substring(j+1, semiPos)), td.getYOff());
					j = semiPos;
					break;
				case 'Y':		// Y offset
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						System.out.println(fileName + ", line " + lineNumber + ", Bad Y offset (semicolon missing): " + varBits);
						break;	
					}
					td.setOff(td.getXOff(), TextUtils.atof(varBits.substring(j+1, semiPos)));
					j = semiPos;
					break;
				case 'B':		// bold
					td.setBold(true);
					break;
				case 'I':		// italic
					td.setItalic(true);
					break;
				case 'L':		// underlined
					td.setUnderline(true);
					break;
				case 'F':		// font
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						System.out.println(fileName + ", line " + lineNumber + ", Bad font (semicolon missing): " + varBits);
						break;	
					}
					TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(varBits.substring(j+1, semiPos));
					td.setFace(af.getIndex());
					j = semiPos;
					break;
				case 'C':		// color
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						System.out.println(fileName + ", line " + lineNumber + ", Bad color (semicolon missing): " + varBits);
						break;	
					}
					td.setColorIndex(TextUtils.atoi(varBits.substring(j+1, semiPos)));
					j = semiPos;
					break;
				case 'R':		// rotation
					TextDescriptor.Rotation rot = TextDescriptor.Rotation.ROT90;
					if (j+1 < varBits.length() && varBits.charAt(j+1) == 'R')
					{
						rot = TextDescriptor.Rotation.ROT180;
						j++;
					}
					if (j+1 < varBits.length() && varBits.charAt(j+1) == 'R')
					{
						rot = TextDescriptor.Rotation.ROT270;
						j++;
					}
					break;
				case 'H':		// inheritable
					td.setInherit(true);
					break;
				case 'T':		// interior
					td.setInterior(true);
					break;
				case 'P':		// parameter
					td.setParam(true);
					break;
				case 'O':		// code
					j++;
					if (j >= varBits.length())
					{
						System.out.println(fileName + ", line " + lineNumber + ", Bad language specification: " + varBits);
						break;	
					}
					char codeLetter = varBits.charAt(j);
					if (var == null)
					{
						System.out.println(fileName + ", line " + lineNumber + ", Illegal use of language specification: " + varBits);
						break;
					}
					if (codeLetter == 'J') var.setCode(Variable.Code.JAVA); else
					if (codeLetter == 'L') var.setCode(Variable.Code.LISP); else
					if (codeLetter == 'T') var.setCode(Variable.Code.TCL); else
					{
						System.out.println(fileName + ", line " + lineNumber + ", Unknown language specification: " + varBits);
					}
					break;
				case 'U':		// units
					j++;
					if (j >= varBits.length())
					{
						System.out.println(fileName + ", line " + lineNumber + ", Bad units specification: " + varBits);
						break;	
					}
					char unitsLetter = varBits.charAt(j);
					if (unitsLetter == 'R') td.setUnit(TextDescriptor.Unit.RESISTANCE); else
					if (unitsLetter == 'C') td.setUnit(TextDescriptor.Unit.CAPACITANCE); else
					if (unitsLetter == 'I') td.setUnit(TextDescriptor.Unit.INDUCTANCE); else
					if (unitsLetter == 'A') td.setUnit(TextDescriptor.Unit.CURRENT); else
					if (unitsLetter == 'V') td.setUnit(TextDescriptor.Unit.VOLTAGE); else
					if (unitsLetter == 'D') td.setUnit(TextDescriptor.Unit.DISTANCE); else
					if (unitsLetter == 'T') td.setUnit(TextDescriptor.Unit.TIME); else
					{
						System.out.println(fileName + ", line " + lineNumber + ", Unknown units specification: " + varBits);
					}
					break;
			}
		}
	}

	/**
	 * Method to convert a String to an Object so that it can be stored in a Variable.
	 * @param piece the String to be converted.
	 * @param objectPos the character number in the string to consider.
	 * Note that the string may be larger than the object description, both by having characters
	 * before it, and also by having characters after it.
	 * Therefore, do not assume that the end of the string is the proper termination of the object specification.
	 * @param varType the type of the object to convert (a letter from the file).
	 * @param fileName the name of the file that this came from (for error reporting).
	 * @param lineNumber the line number in the file that this came from (for error reporting).
	 * @return the Object representation of the given String.
	 */
	private Object getVariableValue(String piece, int objectPos, char varType, String fileName, int lineNumber)
	{
		switch (varType)
		{
			case 'A':		// ArcInst (should delay analysis until database is built!!!)
				int colonPos = piece.indexOf(':', objectPos);
				if (colonPos < 0)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Badly formed Export (missing library colon): " + piece);
					break;
				}
				String libName = piece.substring(objectPos, colonPos);
				Library lib = Library.findLibrary(libName);
				if (lib == null)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Unknown library: " + libName);
					break;
				}
				int secondColonPos = piece.indexOf(':', colonPos+1);
				if (secondColonPos < 0)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Badly formed Export (missing cell colon): " + piece);
					break;
				}
				String cellName = piece.substring(colonPos+1, secondColonPos);
				Cell cell = lib.findNodeProto(cellName);
				if (cell == null)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Unknown Cell: " + piece);
					break;
				}
				String arcName = piece.substring(secondColonPos+1);
				int commaPos = arcName.indexOf(',');
				if (commaPos >= 0) arcName = arcName.substring(0, commaPos);
				ArcInst ai = cell.findArc(arcName);
				if (ai == null)
					System.out.println(fileName + ", line " + lineNumber + ", Unknown ArcInst: " + piece);
				return ai;
			case 'B':		// Boolean
				return new Boolean(piece.charAt(objectPos)=='T' ? true : false);
			case 'C':		// Cell (should delay analysis until database is built!!!)
				colonPos = piece.indexOf(':', objectPos);
				if (colonPos < 0)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Badly formed ArcProto (missing colon): " + piece);
					break;
				}
				libName = piece.substring(objectPos, colonPos);
				lib = Library.findLibrary(libName);
				if (lib == null)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Unknown library: " + libName);
					break;
				}
				cellName = piece.substring(colonPos+1);
				commaPos = cellName.indexOf(',');
				if (commaPos >= 0) cellName = cellName.substring(0, commaPos);
				cell = lib.findNodeProto(cellName);
				if (cell == null)
					System.out.println(fileName + ", line " + lineNumber + ", Unknown Cell: " + piece);
				return cell;
			case 'D':		// Double
				return new Double(TextUtils.atof(piece.substring(objectPos)));
			case 'E':		// Export (should delay analysis until database is built!!!)
				colonPos = piece.indexOf(':', objectPos);
				if (colonPos < 0)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Badly formed Export (missing library colon): " + piece);
					break;
				}
				libName = piece.substring(objectPos, colonPos);
				lib = Library.findLibrary(libName);
				if (lib == null)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Unknown library: " + libName);
					break;
				}
				secondColonPos = piece.indexOf(':', colonPos+1);
				if (secondColonPos < 0)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Badly formed Export (missing cell colon): " + piece);
					break;
				}
				cellName = piece.substring(colonPos+1, secondColonPos);
				cell = lib.findNodeProto(cellName);
				if (cell == null)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Unknown Cell: " + piece);
					break;
				}
				String exportName = piece.substring(secondColonPos+1);
				commaPos = exportName.indexOf(',');
				if (commaPos >= 0) exportName = exportName.substring(0, commaPos);
				Export pp = cell.findExport(exportName);
				if (pp == null)
					System.out.println(fileName + ", line " + lineNumber + ", Unknown Export: " + piece);
				return pp;
			case 'F':		// Float
				return new Float((float)TextUtils.atof(piece.substring(objectPos)));
			case 'G':		// Long
				return new Long(TextUtils.atoi(piece.substring(objectPos)));
			case 'H':		// Short
				return new Short((short)TextUtils.atoi(piece.substring(objectPos)));
			case 'I':		// Integer
				return new Integer(TextUtils.atoi(piece.substring(objectPos)));
			case 'L':		// Library (should delay analysis until database is built!!!)
				libName = piece.substring(objectPos);
				commaPos = libName.indexOf(',');
				if (commaPos >= 0) libName = libName.substring(0, commaPos);
				lib = Library.findLibrary(libName);
				if (lib == null)
					System.out.println(fileName + ", line " + lineNumber + ", Unknown Library: " + piece);
				return lib;
			case 'N':		// NodeInst (should delay analysis until database is built!!!)
				colonPos = piece.indexOf(':', objectPos);
				if (colonPos < 0)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Badly formed Export (missing library colon): " + piece);
					break;
				}
				libName = piece.substring(objectPos, colonPos);
				lib = Library.findLibrary(libName);
				if (lib == null)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Unknown library: " + libName);
					break;
				}
				secondColonPos = piece.indexOf(':', colonPos+1);
				if (secondColonPos < 0)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Badly formed Export (missing cell colon): " + piece);
					break;
				}
				cellName = piece.substring(colonPos+1, secondColonPos);
				cell = lib.findNodeProto(cellName);
				if (cell == null)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Unknown Cell: " + piece);
					break;
				}
				String nodeName = piece.substring(secondColonPos+1);
				commaPos = nodeName.indexOf(',');
				if (commaPos >= 0) nodeName = nodeName.substring(0, commaPos);
				NodeInst ni = cell.findNode(nodeName);
				if (ni == null)
					System.out.println(fileName + ", line " + lineNumber + ", Unknown NodeInst: " + piece);
				return ni;
			case 'O':		// Tool
				String toolName = piece.substring(objectPos);
				commaPos = toolName.indexOf(',');
				if (commaPos >= 0) toolName = toolName.substring(0, commaPos);
				Tool tool = Tool.findTool(toolName);
				if (tool == null)
					System.out.println(fileName + ", line " + lineNumber + ", Unknown Tool: " + piece);
				return tool;
			case 'P':		// PrimitiveNode
				colonPos = piece.indexOf(':', objectPos);
				if (colonPos < 0)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Badly formed PrimitiveNode (missing colon): " + piece);
					break;
				}
				String techName = piece.substring(objectPos, colonPos);
				Technology tech = Technology.findTechnology(techName);
				if (tech == null)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Unknown technology: " + techName);
					break;
				}
				nodeName = piece.substring(colonPos+1);
				commaPos = nodeName.indexOf(',');
				if (commaPos >= 0) nodeName = nodeName.substring(0, commaPos);
				PrimitiveNode np = tech.findNodeProto(nodeName);
				if (np == null)
					System.out.println(fileName + ", line " + lineNumber + ", Unknown PrimitiveNode: " + piece);
				return np;
			case 'R':		// ArcProto
				colonPos = piece.indexOf(':', objectPos);
				if (colonPos < 0)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Badly formed ArcProto (missing colon): " + piece);
					break;
				}
				techName = piece.substring(objectPos, colonPos);
				tech = Technology.findTechnology(techName);
				if (tech == null)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Unknown technology: " + techName);
					break;
				}
				arcName = piece.substring(colonPos+1);
				commaPos = arcName.indexOf(',');
				if (commaPos >= 0) arcName = arcName.substring(0, commaPos);
				ArcProto ap = tech.findArcProto(arcName);
				if (ap == null)
					System.out.println(fileName + ", line " + lineNumber + ", Unknown ArcProto: " + piece);
				return ap;
			case 'S':		// String
				if (piece.charAt(objectPos) != '"')
				{
					System.out.println(fileName + ", line " + lineNumber + ", Badly formed string variable (missing open quote): " + piece);
					break;
				}
				StringBuffer sb = new StringBuffer();
				while (objectPos < piece.length())
				{
					objectPos++;
					if (piece.charAt(objectPos) == '"') break;
					if (piece.charAt(objectPos) == '^') objectPos++;
					sb.append(piece.charAt(objectPos));
				}
				return sb.toString();
			case 'T':		// Technology
				techName = piece.substring(objectPos);
				commaPos = techName.indexOf(',');
				if (commaPos >= 0) techName = techName.substring(0, commaPos);
				tech = Technology.findTechnology(techName);
				if (tech == null)
					System.out.println(fileName + ", line " + lineNumber + ", Unknown Technology: " + piece);
				return tech;
			case 'V':		// Point2D
				double x = TextUtils.atof(piece.substring(objectPos));
				int slashPos = piece.indexOf('/', objectPos);
				if (slashPos < 0)
				{
					System.out.println(fileName + ", line " + lineNumber + ", Badly formed Point2D variable (missing slash): " + piece);
					break;
				}
				double y = TextUtils.atof(piece.substring(slashPos+1));
				return new Point2D.Double(x, y);
			case 'Y':		// Byte
				return new Byte((byte)TextUtils.atoi(piece.substring(objectPos)));
		}
		return null;
	}
}
