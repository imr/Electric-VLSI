/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JELIB.java
 * Input/output tool: JELIB Library input
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
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
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.ELIBConstants;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ErrorLogger;

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
		String fileName;

		CellContents()
		{
			filledIn = false;
			cellStrings = new ArrayList();
		}
	}

	private static HashMap allCells;
	private HashMap externalCells;
	private String version;
	private String curLibName;
	/** The number of lines that have been "processed" so far. */	private int numProcessed;
	/** The number of lines that must be "processed". */			private int numToProcess;

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
			Input.errorLogger.logError("End of file reached while reading " + filePath, null, -1);
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
		String curExternalLibName = "";
		Technology curTech = null;
		PrimitiveNode curPrim = null;
		externalCells = new HashMap();
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
				if (pieces.size() < 7)
				{
					Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
						", Cell declaration needs 7 fields: " + line, null, -1);
					continue;
				}
				String name = unQuote((String)pieces.get(0)) + ";" + pieces.get(2) + "{" + pieces.get(1) + "}";
				Cell newCell = Cell.newInstance(lib, name);
				if (newCell == null)
				{
					Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
						", Unable to create cell " + name, null, -1);
					continue;
				}
				Technology tech = Technology.findTechnology(unQuote((String)pieces.get(3)));
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
				cc.fileName = filePath;
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
				if (pieces.size() != 2)
				{
					Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
						", External library declaration needs 2 fields: " + line, null, -1);
					continue;
				}
				curExternalLibName = unQuote((String)pieces.get(0));
				if (Library.findLibrary(curExternalLibName) != null) continue;

				// recurse
				readExternalLibraryFromFilename(unQuote((String)pieces.get(1)), OpenFile.Type.JELIB);
				continue;
			}

			if (first == 'R')
			{
				// cross-library cell information
				List pieces = parseLine(line);
				if (pieces.size() != 5)
				{
					Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
						", External cell declaration needs 5 fields: " + line, null, -1);
					continue;
				}
				double lowX = TextUtils.atof((String)pieces.get(1));
				double highX = TextUtils.atof((String)pieces.get(2));
				double lowY = TextUtils.atof((String)pieces.get(3));
				double highY = TextUtils.atof((String)pieces.get(4));
				Rectangle2D bounds = new Rectangle2D.Double(lowX, lowY, highX-lowX, highY-lowY);
				String cellName = curExternalLibName + ":" + unQuote((String)pieces.get(0));
				externalCells.put(cellName, bounds);
				continue;
			}

			if (first == 'H')
			{
				// parse header
				List pieces = parseLine(line);
				if (pieces.size() < 2)
				{
					Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
						", Library declaration needs 2 fields: " + line, null, -1);
					continue;
				}
				curLibName = unQuote((String)pieces.get(0));
				version = (String)pieces.get(1);
				continue;
			}

			if (first == 'O')
			{
				// parse Tool information
				List pieces = parseLine(line);
				String toolName = unQuote((String)pieces.get(0));
				Tool tool = Tool.findTool(toolName);
				if (tool == null)
				{
					Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
						", Cannot identify tool " + toolName, null, -1);
					continue;
				}

				// get additional variables starting at position 1
				addVariables(tool, pieces, 1, filePath, lineReader.getLineNumber());
				continue;
			}

			if (first == 'V')
			{
				// parse View information
				List pieces = parseLine(line);
				String viewName = unQuote((String)pieces.get(0));
				View view = View.findView(viewName);
				if (view == null)
				{
					String viewAbbr = unQuote((String)pieces.get(1));
					view = View.newInstance(viewName, viewAbbr);
					if (view == null)
					{
						Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
							", Cannot create view " + viewName, null, -1);
						continue;
					}
				}

//				// get additional variables starting at position 2
//				addVariables(view, pieces, 2, filePath, lineReader.getLineNumber());
				continue;
			}

			if (first == 'T')
			{
				// parse Technology information
				List pieces = parseLine(line);
				String techName = unQuote((String)pieces.get(0));
				curTech = Technology.findTechnology(techName);
				if (curTech == null)
				{
					Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
						", Cannot identify technology " + techName, null, -1);
					continue;
				}
				curPrim = null;

				// get additional variables starting at position 1
				addVariables(curTech, pieces, 1, filePath, lineReader.getLineNumber());
				continue;
			}

			if (first == 'D')
			{
				// parse PrimitiveNode information
				List pieces = parseLine(line);
				String primName = unQuote((String)pieces.get(0));
				if (curTech == null)
				{
					Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
						", Primitive node " + primName + " has no technology before it", null, -1);
					continue;
				}
				curPrim = curTech.findNodeProto(primName);
				if (curPrim == null)
				{
					Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
						", Cannot identify primitive node " + primName, null, -1);
					continue;
				}

				// get additional variables starting at position 1
//				addVariables(curPrim, pieces, 1, filePath, lineReader.getLineNumber());
				continue;
			}

			if (first == 'P')
			{
				// parse PrimitivePort information
				List pieces = parseLine(line);
				String primPortName = unQuote((String)pieces.get(0));
				if (curPrim == null)
				{
					Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
						", Primitive port " + primPortName + " has no primitive node before it", null, -1);
					continue;
				}

				PrimitivePort pp = (PrimitivePort)curPrim.findPortProto(primPortName);
				if (pp == null)
				{
					Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
						", Cannot identify primitive port " + primPortName, null, -1);
					continue;
				}

				// get additional variables starting at position 1
//				addVariables(pp, pieces, 1, filePath, lineReader.getLineNumber());
				continue;
			}

			if (first == 'W')
			{
				// parse ArcProto information
				List pieces = parseLine(line);
				String arcName = unQuote((String)pieces.get(0));
				if (curTech == null)
				{
					Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
						", Primitive arc " + arcName + " has no technology before it", null, -1);
					continue;
				}
				PrimitiveArc ap = curTech.findArcProto(arcName);
				if (ap == null)
				{
					Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
						", Cannot identify primitive arc " + arcName, null, -1);
					continue;
				}

				// get additional variables starting at position 1
//				addVariables(ap, pieces, 1, filePath, lineReader.getLineNumber());
				continue;
			}

			if (first == 'G')
			{
				// group information
				List pieces = parseLine(line);
				Cell firstCell = null;
				for(int i=0; i<pieces.size(); i++)
				{
					String cellName = unQuote((String)pieces.get(i));
					if (cellName.length() == 0) continue;
					int colonPos = cellName.indexOf(':');
					if (colonPos >= 0) cellName = cellName.substring(colonPos+1);
					Cell cell = lib.findNodeProto(cellName);
					if (cell == null)
					{
						Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
							", Cannot find cell " + cellName, null, -1);
						break;
					}

					// the first entry is the "main schematic cell"
					if (firstCell == null)
					{
						firstCell = cell;
						cell.putInOwnCellGroup();
					} else
					{
						cell.joinGroup(firstCell);
					}
					if (i == 0)
					{
						if (cell.getView() == View.SCHEMATIC) cell.getCellGroup().setMainSchematics(cell);
					}
				}
				continue;
			}

			Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
				", Unrecognized line: " + line, null, -1);
		}

		// sensibility check: shouldn't all cells with the same root name be in the same group?
		HashMap cellGroups = new HashMap();
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			String lowerCaseName = cell.getName().toLowerCase();
			Cell.CellGroup group = cell.getCellGroup();
			Cell.CellGroup groupOfName = (Cell.CellGroup)cellGroups.get(lowerCaseName);
			if (groupOfName == null)
			{
				cellGroups.put(lowerCaseName, group);
			} else
			{
				if (groupOfName != group)
				{
					Input.errorLogger.logError(filePath + ", Library has multiple cells named '" +
						lowerCaseName + "' that are not in the same group", null, -1);
				}
			}
		}

		lib.clearChangedMajor();
		lib.clearChangedMinor();
		lib.setFromDisk();
		return false;
	}

	/**
	 * Method called after all libraries have been read.
	 * Instantiates all of the Cell contents that were saved in "allCells".
	 */
	private void instantiateCellContents()
	{
		System.out.println("Creating the circuitry...");
		progress.setNote("Creating the circuitry");

		// count the number of lines that need to be processed
		numToProcess = 0;
		for(Iterator it = allCells.values().iterator(); it.hasNext(); )
		{
			CellContents cc = (CellContents)it.next();
			numToProcess += cc.cellStrings.size();
		}

		// instantiate all cells recursively
		numProcessed = 0;
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

		// map disk node names (duplicate node names written "sig"1 and "sig"2)
		HashMap diskName = new HashMap();

		// place all nodes
		for(int line=0; line<numStrings; line++)
		{
			String cellString = (String)cc.cellStrings.get(line);
			if (cellString.charAt(0) != 'N') continue;
			numProcessed++;
			if ((numProcessed%100) == 0) progress.setProgress(numProcessed * 100 / numToProcess);

			// parse the node line
			List pieces = parseLine(cellString);
			if (pieces.size() < 10)
			{
				Input.errorLogger.logError(cc.fileName + ", line " + lineReader.getLineNumber() +
					", Node instance needs 10 fields: " + cellString, cell, -1);
				continue;
			}
			String protoName = unQuote((String)pieces.get(0));
			double x = TextUtils.atof((String)pieces.get(3));
			double y = TextUtils.atof((String)pieces.get(4));
			double wid = TextUtils.atof((String)pieces.get(5));
			double hei = TextUtils.atof((String)pieces.get(6));
			int angle = TextUtils.atoi((String)pieces.get(7));

			String prefixName = lib.getName();
			NodeProto np = null;
			Library cellLib = lib;
			int colonPos = protoName.indexOf(':');
			if (colonPos < 0) np = lib.findNodeProto(protoName); else
			{
				prefixName = protoName.substring(0, colonPos);
				protoName = protoName.substring(colonPos+1);
				if (prefixName.equals(curLibName)) np = lib.findNodeProto(protoName); else
				{
					Technology tech = Technology.findTechnology(prefixName);
					if (tech != null) np = tech.findNodeProto(protoName);
					if (np == null)
					{
						cellLib = Library.findLibrary(prefixName);
						if (cellLib != null)
							np = cellLib.findNodeProto(protoName);
					}
				}
			}
			if (np == null)
			{
				if (cellLib == null)
				{
					Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
						", Creating dummy library " + prefixName, cell, -1);
					cellLib = Library.newInstance(prefixName, null);
				}
				Cell dummyCell = Cell.makeInstance(cellLib, protoName);
				if (dummyCell == null)
				{
					Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
						", Unable to create dummy cell " + protoName + " in library " + cellLib.getName(), cell, -1);
					continue;
				}
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					", Creating dummy cell " + protoName + " in library " + cellLib.getName(), cell, -1);
				Rectangle2D bounds = (Rectangle2D)externalCells.get(pieces.get(0));
				if (bounds == null)
				{
					Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
						", Warning: cannot find information about external cell " + pieces.get(0), cell, -1);
					NodeInst.newInstance(Generic.tech.invisiblePinNode, new Point2D.Double(0,0), wid, hei, dummyCell);
				} else
				{
					NodeInst.newInstance(Generic.tech.invisiblePinNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
						bounds.getWidth(), bounds.getHeight(), dummyCell);
				}

				// mark this as a dummy cell
				dummyCell.newVar(IO_TRUE_LIBRARY, prefixName);
				dummyCell.newVar(IO_DUMMY_OBJECT, protoName);
				np = dummyCell;
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

			// figure out the name for this node.  Handle the form: "Sig"12
			String diskNodeName = unQuote((String)pieces.get(1));
			String nodeName = diskNodeName;
			if (nodeName.charAt(0) == '"')
			{
				int lastQuote = nodeName.lastIndexOf('"');
				if (lastQuote > 1)
				{
					nodeName = nodeName.substring(1, lastQuote);
				}
			}

			// parse state information in field 8
			String stateInfo = (String)pieces.get(8);
			boolean expanded = false, locked = false, shortened = false,
				visInside = false, wiped = false, hardSelect = false;
			int techSpecific = 0;
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
					techSpecific = TextUtils.atoi(stateInfo.substring(i));
					break;
				}
			}

			// create the node
			NodeInst ni = NodeInst.newInstance(np, new Point2D.Double(x, y), wid, hei, cell, angle, nodeName, techSpecific);
			if (ni == null)
			{
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					" (cell " + cell.describe() + ") cannot create node " + protoName, cell, -1);
				continue;
			}

			// get the node name text descriptor
			String nameTextDescriptorInfo = (String)pieces.get(2);
			loadTextDescriptor(ni.getNameTextDescriptor(), null, nameTextDescriptorInfo, cc.fileName, cc.lineNumber + line);

			// insert into map of disk names
			diskName.put(diskNodeName, ni);

			// add state bits
			if (expanded) ni.setExpanded(); else ni.clearExpanded();
			if (locked) ni.setLocked(); else ni.clearLocked();
			if (shortened) ni.setShortened(); else ni.clearShortened();
			if (visInside) ni.setVisInside(); else ni.clearVisInside();
			if (wiped) ni.setWiped(); else ni.clearWiped();
			if (hardSelect) ni.setHardSelect(); else ni.clearHardSelect();

			// get text descriptor for cell instance names
			String textDescriptorInfo = (String)pieces.get(9);
			loadTextDescriptor(ni.getProtoTextDescriptor(), null, textDescriptorInfo, cc.fileName, cc.lineNumber + line);

			// add variables in fields 10 and up
			addVariables(ni, pieces, 10, cc.fileName, cc.lineNumber + line);
		}

		// place all exports
		for(int line=0; line<numStrings; line++)
		{
			String cellString = (String)cc.cellStrings.get(line);
			if (cellString.charAt(0) != 'E') continue;
			numProcessed++;
			if ((numProcessed%100) == 0) progress.setProgress(numProcessed * 100 / numToProcess);

			// parse the export line
			List pieces = parseLine(cellString);
			if (pieces.size() < 7)
			{
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					", Export needs 7 fields, has " + pieces.size() + ": " + cellString, cell, -1);
				continue;
			}
			String exportName = unQuote((String)pieces.get(0));
			String nodeName = unQuote((String)pieces.get(2));
			String portName = unQuote((String)pieces.get(3));
			double x = TextUtils.atof((String)pieces.get(4));
			double y = TextUtils.atof((String)pieces.get(5));
			PortInst pi = figureOutPortInst(cell, portName, nodeName, x, y, diskName, cc.fileName, cc.lineNumber + line);
			if (pi == null) continue;

			// create the export
			Export pp = Export.newInstance(cell, pi, exportName, false);
			if (pp == null)
			{
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					" (cell " + cell.describe() + ") cannot create export " + exportName, cell, -1);
				continue;
			}

			// get text descriptor in field 1
			String textDescriptorInfo = (String)pieces.get(1);
			loadTextDescriptor(pp.getTextDescriptor(), null, textDescriptorInfo, cc.fileName, cc.lineNumber + line);

			// parse state information in field 6
			String stateInfo = (String)pieces.get(6);
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
			PortCharacteristic ch = PortCharacteristic.findCharacteristicShort(stateInfo);
			pp.setCharacteristic(ch);

			// add variables in fields 7 and up
			addVariables(pp, pieces, 7, cc.fileName, cc.lineNumber + line);
		}

		// next place all arcs
		for(int line=0; line<numStrings; line++)
		{
			String cellString = (String)cc.cellStrings.get(line);
			if (cellString.charAt(0) != 'A') continue;
			numProcessed++;
			if ((numProcessed%100) == 0) progress.setProgress(numProcessed * 100 / numToProcess);

			// parse the arc line
			List pieces = parseLine(cellString);
			if (pieces.size() < 13)
			{
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					", Arc instance needs 13 fields: " + cellString, cell, -1);
				continue;
			}
			String protoName = unQuote((String)pieces.get(0));
			ArcProto ap = ArcProto.findArcProto(protoName);
			if (ap == null)
			{
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					" (cell " + cell.describe() + ") cannot find arc " + protoName, cell, -1);
				continue;
			}
			String arcName = unQuote((String)pieces.get(1));
			double wid = TextUtils.atof((String)pieces.get(3));

			String headNodeName = unQuote((String)pieces.get(5));
			String headPortName = unQuote((String)pieces.get(6));
			double headX = TextUtils.atof((String)pieces.get(7));
			double headY = TextUtils.atof((String)pieces.get(8));
			PortInst headPI = figureOutPortInst(cell, headPortName, headNodeName, headX, headY, diskName, cc.fileName, cc.lineNumber + line);
			if (headPI == null) continue;

			String tailNodeName = unQuote((String)pieces.get(9));
			String tailPortName = unQuote((String)pieces.get(10));
			double tailX = TextUtils.atof((String)pieces.get(11));
			double tailY = TextUtils.atof((String)pieces.get(12));
			PortInst tailPI = figureOutPortInst(cell, tailPortName, tailNodeName, tailX, tailY, diskName, cc.fileName, cc.lineNumber + line);
			if (tailPI == null) continue;

			// parse state information in field 4
			String stateInfo = (String)pieces.get(4);
			boolean rigid = false, fixedAngle = true, slidable = false,
				extended = true, directional = false, reverseEnds = false,
				hardSelect = false, skipHead = false, skipTail = false,
				tailNegated = false, headNegated = false;
			int angle = 0;
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
					angle = TextUtils.atoi(stateInfo.substring(i));
					break;
				}
			}

			ArcInst ai = ArcInst.newInstance(ap, wid, headPI, tailPI, new Point2D.Double(headX, headY),
				new Point2D.Double(tailX, tailY), arcName, angle);
			if (ai == null)
			{
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					" (cell " + cell.describe() + ") cannot create arc " + protoName, cell, -1);
				continue;
			}

			// get the ard name text descriptor
			String nameTextDescriptorInfo = (String)pieces.get(2);
			loadTextDescriptor(ai.getNameTextDescriptor(), null, nameTextDescriptorInfo, cc.fileName, cc.lineNumber + line);

			// add state bits
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

			// add variables in fields 13 and up
			addVariables(ai, pieces, 13, cc.fileName, cc.lineNumber + line);
		}
		cc.filledIn = true;
	}

	/**
	 * Method to find the proper PortInst for a specified port on a node, at a given position.
	 * @param cell the cell in which this all resides.
	 * @param portName the name of the port (may be an empty string if there is only 1 port).
	 * @param nodeName the name of the node.
	 * @param xPos the X coordinate of the port on the node.
	 * @param yPos the Y coordinate of the port on the node.
	 * @param diskName a HashMap that maps node names to actual nodes.
	 * @param lineNumber the line number in the file being read (for error reporting).
	 * @return the PortInst specified (null if none can be found).
	 */
	private PortInst figureOutPortInst(Cell cell, String portName, String nodeName, double xPos, double yPos, HashMap diskName, String fileName, int lineNumber)
	{
		NodeInst ni = (NodeInst)diskName.get(nodeName);
		if (ni == null)
		{
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				" (cell " + cell.describe() + ") cannot find node " + nodeName, cell, -1);
			return null;
		}

		PortInst pi = null;
		if (portName.length() == 0)
		{
			if (ni.getNumPortInsts() > 0)
				pi = ni.getPortInst(0);
		} else
		{
			pi = ni.findPortInst(portName);
		}

		// primitives use the name match
		NodeProto np = ni.getProto();
		if (np instanceof PrimitiveNode) return pi;

		// make sure the port can handle the position
		Point2D headPt = new Point2D.Double(xPos, yPos);
		if (pi != null)
		{
			Poly poly = pi.getPoly();
			if (!poly.isInside(headPt)) pi = null;
		}
		if (pi != null) return pi;

		// see if this is a dummy cell
		Cell subCell = (Cell)ni.getProto();
		Variable var = subCell.getVar(IO_TRUE_LIBRARY);
		if (var == null)
		{
			// not a dummy cell: create a pin at the top level
			NodeInst portNI = NodeInst.newInstance(Generic.tech.universalPinNode, headPt, 0, 0, cell);
			if (portNI == null)
			{
				Input.errorLogger.logError(fileName + ", line " + lineNumber +
					", Unable to create dummy node in cell " + cell.describe() + " (cannot create source node)", cell, -1);
				return null;
			}
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				", Creating dummy node in cell " + cell.describe() + " to connect to node " + nodeName + ", port " + portName, cell, -1);
			return portNI.getOnlyPortInst();
		}

		// a dummy cell: create a dummy export on it to fit this
		String name = portName;
		if (name.length() == 0) name = "X";
		AffineTransform unRot = ni.rotateIn();
		unRot.transform(headPt, headPt);
		AffineTransform unTrans = ni.translateIn();
		unTrans.transform(headPt, headPt);
		NodeInst portNI = NodeInst.newInstance(Generic.tech.universalPinNode, headPt, 0, 0, subCell);
		if (portNI == null)
		{
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				", Unable to create export " + name + " on dummy cell " + subCell.describe() + " (cannot create source node)", cell, -1);
			return null;
		}
		PortInst portPI = portNI.getOnlyPortInst();
		Export pp = Export.newInstance(subCell, portPI, name, false);
		if (pp == null)
		{
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				", Unable to create export " + name + " on dummy cell " + subCell.describe(), cell, -1);
			return null;
		}
		pi = ni.findPortInstFromProto(pp);
		Input.errorLogger.logError(fileName + ", line " + lineNumber +
			", Creating export " + name + " on dummy cell " + subCell.describe(), cell, -1);

		return pi;
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
		StringBuffer sb = new StringBuffer();
		boolean inQuote = false;
		while (pos < len)
		{
			char chr = line.charAt(pos++);
			if (chr == '^')
			{
				sb.append(chr);
				sb.append(line.charAt(pos++));
				continue;
			}
			if (chr == '"') inQuote = !inQuote;
			if (chr == '|' && !inQuote)
			{
				String piece = sb.toString();
				stringPieces.add(piece);
				sb = new StringBuffer();
			} else
			{
				sb.append(chr);
			}
		}
		String piece = sb.toString();
		stringPieces.add(piece);
		return stringPieces;
	}

	private String unQuote(String line)
	{
		if (line.indexOf('^') < 0) return line;
		StringBuffer sb = new StringBuffer();
		int len = line.length();
		for(int i=0; i<len; i++)
		{
			char chr = line.charAt(i);
			if (chr == '^')
			{
				i++;
				if (i >= len) break;
				chr = line.charAt(i);
			}
			sb.append(chr);
		}
		return sb.toString();
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
			int openPos = 0;
			for(; openPos < piece.length(); openPos++)
			{
				char chr = piece.charAt(openPos);
				if (chr == '^') { openPos++;   continue; }
				if (chr == '(') break;
			}
			if (openPos >= piece.length())
			{
				Input.errorLogger.logError(fileName + ", line " + lineNumber +
					", Badly formed variable (no open parenthesis): " + piece, null, -1);
				continue;
			}
			String varName = piece.substring(0, openPos);
			Variable.Key varKey = ElectricObject.newKey(varName);
			if (eObj.isDeprecatedVariable(varKey)) continue;
			int closePos = piece.indexOf(')', openPos);
			if (closePos < 0)
			{
				Input.errorLogger.logError(fileName + ", line " + lineNumber +
					", Badly formed variable (no close parenthesis): " + piece, null, -1);
				continue;
			}
			String varBits = piece.substring(openPos+1, closePos);
			int objectPos = closePos + 1;
			if (objectPos >= piece.length())
			{
				Input.errorLogger.logError(fileName + ", line " + lineNumber +
					", Variable type missing: " + piece, null, -1);
				continue;
			}
			char varType = piece.charAt(objectPos++);
			switch (varType)
			{
				case 'B':
				case 'C':
				case 'D':
				case 'E':
				case 'F':
				case 'G':
				case 'H':
				case 'I':
				case 'L':
				case 'O':
				case 'P':
				case 'R':
				case 'S':
				case 'T':
				case 'V':
				case 'Y':
					break; // break from switch
				default:
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Variable type invalid: " + piece, null, -1);
					continue; // continue loop
			}
			if (objectPos >= piece.length())
			{
				Input.errorLogger.logError(fileName + ", line " + lineNumber +
					", Variable value missing: " + piece, null, -1);
				continue;
			}
			Object obj = null;
			if (piece.charAt(objectPos) == '[')
			{
				List objList = new ArrayList();
				objectPos++;
				while (objectPos < piece.length())
				{
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
						if (piece.charAt(objectPos) == ',' || piece.charAt(objectPos) == ']') break;
						if (piece.charAt(objectPos) == '"')
						{
							inQuote = true;
						}
						objectPos++;
					}
					Object oneObj = getVariableValue(piece.substring(start, objectPos), 0, varType, fileName, lineNumber);
					objList.add(oneObj);
					if (piece.charAt(objectPos) == ']') break;
					objectPos++;
				}
				if (objectPos >= piece.length())
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed array (no closed bracket): " + piece, null, -1);
					continue;
				}
				else if (objectPos < piece.length() - 1)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed array (extra characters after closed bracket): " + piece, null, -1);
					continue;
				}
				int limit = objList.size();
				Object [] objArray = null;
				switch (varType)
				{
// 					case 'A': objArray = new ArcInst[limit];        break;
					case 'B': objArray = new Boolean[limit];        break;
					case 'C': objArray = new Cell[limit];           break;
					case 'D': objArray = new Double[limit];         break;
					case 'E': objArray = new Export[limit];         break;
					case 'F': objArray = new Float[limit];          break;
					case 'G': objArray = new Long[limit];           break;
					case 'H': objArray = new Short[limit];          break;
					case 'I': objArray = new Integer[limit];        break;
					case 'L': objArray = new Library[limit];        break;
// 					case 'N': objArray = new NodeInst[limit];       break;
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
				Input.errorLogger.logError(fileName + ", line " + lineNumber +
					", Cannot create variable: " + piece, null, -1);
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
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Incorrect display specification: " + varBits, null, -1);
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
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Bad absolute size (semicolon missing): " + varBits, null, -1);
						break;
					}
					td.setAbsSize(TextUtils.atoi(varBits.substring(j+1, semiPos)));
					j = semiPos;
					break;
				case 'G':		// relative text size
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Bad relative size (semicolon missing): " + varBits, null, -1);
						break;
					}
					td.setRelSize(TextUtils.atof(varBits.substring(j+1, semiPos)));
					j = semiPos;
					break;
				case 'X':		// X offset
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Bad X offset (semicolon missing): " + varBits, null, -1);
						break;
					}
					td.setOff(TextUtils.atof(varBits.substring(j+1, semiPos)), td.getYOff());
					j = semiPos;
					break;
				case 'Y':		// Y offset
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Bad Y offset (semicolon missing): " + varBits, null, -1);
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
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Bad font (semicolon missing): " + varBits, null, -1);
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
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Bad color (semicolon missing): " + varBits, null, -1);
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
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Bad language specification: " + varBits, null, -1);
						break;
					}
					char codeLetter = varBits.charAt(j);
					if (var == null)
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Illegal use of language specification: " + varBits, null, -1);
						break;
					}
					if (codeLetter == 'J') var.setCode(Variable.Code.JAVA); else
					if (codeLetter == 'L') var.setCode(Variable.Code.LISP); else
					if (codeLetter == 'T') var.setCode(Variable.Code.TCL); else
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Unknown language specification: " + varBits, null, -1);
					}
					break;
				case 'U':		// units
					j++;
					if (j >= varBits.length())
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Bad units specification: " + varBits, null, -1);
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
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Unknown units specification: " + varBits, null, -1);
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
		int colonPos;
		String libName;
		Library lib;
		int secondColonPos;
		String cellName;
		Cell cell;
		int commaPos;

		switch (varType)
		{
// 			case 'A':		// ArcInst (should delay analysis until database is built!!!)
// 				int colonPos = piece.indexOf(':', objectPos);
// 				if (colonPos < 0)
// 				{
// 					Input.errorLogger.logError(fileName + ", line " + lineNumber +
// 						", Badly formed Export (missing library colon): " + piece, null, -1);
// 					break;
// 				}
// 				String libName = piece.substring(objectPos, colonPos);
// 				Library lib = Library.findLibrary(libName);
// 				if (lib == null)
// 				{
// 					Input.errorLogger.logError(fileName + ", line " + lineNumber +
// 						", Unknown library: " + libName, null, -1);
// 					break;
// 				}
// 				int secondColonPos = piece.indexOf(':', colonPos+1);
// 				if (secondColonPos < 0)
// 				{
// 					Input.errorLogger.logError(fileName + ", line " + lineNumber +
// 						", Badly formed Export (missing cell colon): " + piece, null, -1);
// 					break;
// 				}
// 				String cellName = piece.substring(colonPos+1, secondColonPos);
// 				Cell cell = lib.findNodeProto(cellName);
// 				if (cell == null)
// 				{
// 					Input.errorLogger.logError(fileName + ", line " + lineNumber +
// 						", Unknown Cell: " + piece, null, -1);
// 					break;
// 				}
// 				String arcName = piece.substring(secondColonPos+1);
// 				int commaPos = arcName.indexOf(',');
// 				if (commaPos >= 0) arcName = arcName.substring(0, commaPos);
// 				ArcInst ai = cell.findArc(arcName);
// 				if (ai == null)
// 					Input.errorLogger.logError(fileName + ", line " + lineNumber +
// 						", Unknown ArcInst: " + piece, null, -1);
// 				return ai;
			case 'B':		// Boolean
				return new Boolean(piece.charAt(objectPos)=='T' ? true : false);
			case 'C':		// Cell (should delay analysis until database is built!!!)
				colonPos = piece.indexOf(':', objectPos);
				if (colonPos < 0)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed ArcProto (missing colon): " + piece, null, -1);
					break;
				}
				libName = piece.substring(objectPos, colonPos);
				lib = Library.findLibrary(libName);
				if (lib == null)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown library: " + libName, null, -1);
					break;
				}
				cellName = piece.substring(colonPos+1);
				commaPos = cellName.indexOf(',');
				if (commaPos >= 0) cellName = cellName.substring(0, commaPos);
				cell = lib.findNodeProto(cellName);
				if (cell == null)
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown Cell: " + piece, null, -1);
				return cell;
			case 'D':		// Double
				return new Double(TextUtils.atof(piece.substring(objectPos)));
			case 'E':		// Export (should delay analysis until database is built!!!)
				colonPos = piece.indexOf(':', objectPos);
				if (colonPos < 0)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed Export (missing library colon): " + piece, null, -1);
					break;
				}
				libName = piece.substring(objectPos, colonPos);
				lib = Library.findLibrary(libName);
				if (lib == null)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown library: " + libName, null, -1);
					break;
				}
				secondColonPos = piece.indexOf(':', colonPos+1);
				if (secondColonPos < 0)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed Export (missing cell colon): " + piece, null, -1);
					break;
				}
				cellName = piece.substring(colonPos+1, secondColonPos);
				cell = lib.findNodeProto(cellName);
				if (cell == null)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown Cell: " + piece, null, -1);
					break;
				}
				String exportName = piece.substring(secondColonPos+1);
				commaPos = exportName.indexOf(',');
				if (commaPos >= 0) exportName = exportName.substring(0, commaPos);
				Export pp = cell.findExport(exportName);
				if (pp == null)
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown Export: " + piece, null, -1);
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
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown Library: " + piece, null, -1);
				return lib;
// 			case 'N':		// NodeInst (should delay analysis until database is built!!!)
// 				colonPos = piece.indexOf(':', objectPos);
// 				if (colonPos < 0)
// 				{
// 					Input.errorLogger.logError(fileName + ", line " + lineNumber +
// 						", Badly formed Export (missing library colon): " + piece, null, -1);
// 					break;
// 				}
// 				libName = piece.substring(objectPos, colonPos);
// 				lib = Library.findLibrary(libName);
// 				if (lib == null)
// 				{
// 					Input.errorLogger.logError(fileName + ", line " + lineNumber +
// 						", Unknown library: " + libName, null, -1);
// 					break;
// 				}
// 				secondColonPos = piece.indexOf(':', colonPos+1);
// 				if (secondColonPos < 0)
// 				{
// 					Input.errorLogger.logError(fileName + ", line " + lineNumber +
// 						", Badly formed Export (missing cell colon): " + piece, null, -1);
// 					break;
// 				}
// 				cellName = piece.substring(colonPos+1, secondColonPos);
// 				cell = lib.findNodeProto(cellName);
// 				if (cell == null)
// 				{
// 					Input.errorLogger.logError(fileName + ", line " + lineNumber +
// 						", Unknown Cell: " + piece, null, -1);
// 					break;
// 				}
// 				String nodeName = piece.substring(secondColonPos+1);
// 				commaPos = nodeName.indexOf(',');
// 				if (commaPos >= 0) nodeName = nodeName.substring(0, commaPos);
// 				NodeInst ni = cell.findNode(nodeName);
// 				if (ni == null)
// 					Input.errorLogger.logError(fileName + ", line " + lineNumber +
// 						", Unknown NodeInst: " + piece, null, -1);
// 				return ni;
			case 'O':		// Tool
				String toolName = piece.substring(objectPos);
				commaPos = toolName.indexOf(',');
				if (commaPos >= 0) toolName = toolName.substring(0, commaPos);
				Tool tool = Tool.findTool(toolName);
				if (tool == null)
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown Tool: " + piece, null, -1);
				return tool;
			case 'P':		// PrimitiveNode
				colonPos = piece.indexOf(':', objectPos);
				if (colonPos < 0)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed PrimitiveNode (missing colon): " + piece, null, -1);
					break;
				}
				String techName = piece.substring(objectPos, colonPos);
				Technology tech = Technology.findTechnology(techName);
				if (tech == null)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown technology: " + techName, null, -1);
					break;
				}
				String nodeName = piece.substring(colonPos+1);
				commaPos = nodeName.indexOf(',');
				if (commaPos >= 0) nodeName = nodeName.substring(0, commaPos);
				PrimitiveNode np = tech.findNodeProto(nodeName);
				if (np == null)
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown PrimitiveNode: " + piece, null, -1);
				return np;
			case 'R':		// ArcProto
				colonPos = piece.indexOf(':', objectPos);
				if (colonPos < 0)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed ArcProto (missing colon): " + piece, null, -1);
					break;
				}
				techName = piece.substring(objectPos, colonPos);
				tech = Technology.findTechnology(techName);
				if (tech == null)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown technology: " + techName, null, -1);
					break;
				}
				String arcName = piece.substring(colonPos+1);
				commaPos = arcName.indexOf(',');
				if (commaPos >= 0) arcName = arcName.substring(0, commaPos);
				ArcProto ap = tech.findArcProto(arcName);
				if (ap == null)
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown ArcProto: " + piece, null, -1);
				return ap;
			case 'S':		// String
				if (piece.charAt(objectPos) != '"')
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed string variable (missing open quote): " + piece, null, -1);
					break;
				}
				StringBuffer sb = new StringBuffer();
				int len = piece.length();
				while (objectPos < len)
				{
					objectPos++;
					if (piece.charAt(objectPos) == '"') break;
					if (piece.charAt(objectPos) == '^')
					{
						objectPos++;
						if (objectPos <= len - 2 && piece.charAt(objectPos) == '\\' && piece.charAt(objectPos+1) == 'n')
						{
							sb.append('\n');
							objectPos++;
							continue;
						}
					}
					sb.append(piece.charAt(objectPos));
				}
				return sb.toString();
			case 'T':		// Technology
				techName = piece.substring(objectPos);
				commaPos = techName.indexOf(',');
				if (commaPos >= 0) techName = techName.substring(0, commaPos);
				tech = Technology.findTechnology(techName);
				if (tech == null)
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown Technology: " + piece, null, -1);
				return tech;
			case 'V':		// Point2D
				double x = TextUtils.atof(piece.substring(objectPos));
				int slashPos = piece.indexOf('/', objectPos);
				if (slashPos < 0)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed Point2D variable (missing slash): " + piece, null, -1);
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
