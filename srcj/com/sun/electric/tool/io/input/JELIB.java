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

import com.sun.electric.database.CellId;
import com.sun.electric.database.ExportId;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.LibId;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.ncc.basic.TransitiveRelation;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Set;

/**
 * This class reads files in new library file (.jelib) format.
 */
public class JELIB extends LibraryFiles
{
	private static class CellContents
	{
		boolean filledIn;
		int lineNumber;
        String groupName;
		List<String> cellStrings;
		String fileName;

		CellContents()
		{
			filledIn = false;
			cellStrings = new ArrayList<String>();
		}
	}

	private static String[] revisions =
	{
		// Revision 1
		"8.01aw",
		// Revision 2
		"8.04l"
	};

    private static int defaultArcFlags;
    static {
        defaultArcFlags = 0;
        defaultArcFlags = ImmutableArcInst.HARD_SELECT.set(defaultArcFlags, false); // A
		defaultArcFlags	= ImmutableArcInst.BODY_ARROWED.set(defaultArcFlags, false); // B
        defaultArcFlags = ImmutableArcInst.FIXED_ANGLE.set(defaultArcFlags, true); // F
        defaultArcFlags = ImmutableArcInst.HEAD_NEGATED.set(defaultArcFlags, false); // G
        defaultArcFlags = ImmutableArcInst.HEAD_EXTENDED.set(defaultArcFlags, true); // I
        defaultArcFlags = ImmutableArcInst.TAIL_EXTENDED.set(defaultArcFlags, true); // J
        defaultArcFlags = ImmutableArcInst.TAIL_NEGATED.set(defaultArcFlags, false); // N
        defaultArcFlags = ImmutableArcInst.RIGID.set(defaultArcFlags, false); // R
        defaultArcFlags = ImmutableArcInst.SLIDABLE.set(defaultArcFlags, false); // S
        defaultArcFlags = ImmutableArcInst.HEAD_ARROWED.set(defaultArcFlags, false); // X
        defaultArcFlags = ImmutableArcInst.TAIL_ARROWED.set(defaultArcFlags, false); // Y
    }
 	private LinkedHashMap<Cell,CellContents> allCells = new LinkedHashMap<Cell,CellContents>();
	private HashMap<String,Rectangle2D> externalCells;
	private HashMap<String,Point2D.Double> externalExports;
    private HashMap<String,TextDescriptor> parsedDescriptorsF = new HashMap<String,TextDescriptor>();
    private HashMap<String,TextDescriptor> parsedDescriptorsT = new HashMap<String,TextDescriptor>();
//	private Version version;
	int revision;
	char escapeChar = '\\';
	String curLibName;
    String curReadFile;

    private String curExternalLibName = "";
    private String curExternalCellName = "";
    private Technology curTech = null;
    private PrimitiveNode curPrim = null;
    private ArrayList<Cell[]> groupLines;

//	/** The number of lines that have been "processed" so far. */	private int numProcessed;
//	/** The number of lines that must be "processed". */			private int numToProcess;

	JELIB()
	{
	}

	/**
	 * Ugly hack.
	 * Overrides LibraryFiles so that recursive library reads get to the proper place.
	 */
// 	public boolean readInputLibrary()
// 	{
// 		if (topLevelLibrary) allCells = new HashMap();
// 		boolean error = readLib();
// 		if (topLevelLibrary)
// 		{
// 			if (!error) instantiateCellContents();
// 			allCells = null;
// 		}
// 		return error;
// 	}

	/**
	 * Method to read a Library in new library file (.jelib) format.
	 * @return true on error.
	 */
	protected boolean readLib()
	{
		try
		{
			if (readTheLibrary()) return true;
			nodeProtoCount = allCells.size();
			nodeProtoList = new Cell[nodeProtoCount];
			cellLambda = new double[nodeProtoCount];
			int i = 0;
			for (Cell cell : allCells.keySet())
				nodeProtoList[i++] = cell;
			return false;
		} catch (IOException e)
		{
			Input.errorLogger.logError("End of file reached while reading " + filePath, -1);
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
		revision = revisions.length;
		curExternalLibName = "";
		curExternalCellName = "";
		curTech = null;
		curPrim = null;
		externalCells = new HashMap<String,Rectangle2D>();
		externalExports = new HashMap<String,Point2D.Double>();
		groupLines = new ArrayList<Cell[]>();
        curReadFile = filePath;

        // do all the reading
        readFromFile(false);

        // collect the cells by common protoName and by "groupLines" relation
        TransitiveRelation<Object> transitive = new TransitiveRelation<Object>();
        HashMap<String,String> protoNames = new HashMap<String,String>();
        for (Iterator<Cell> cit = lib.getCells(); cit.hasNext();)
        {
            Cell cell = cit.next();
            String protoName = protoNames.get(cell.getName());
            if (protoName == null)
            {
                protoName = cell.getName();
                protoNames.put(protoName, protoName);
            }
            transitive.theseAreRelated(cell, protoName);
            
            // consider groupName fields
            String groupName = allCells.get(cell).groupName;
            if (groupName == null) continue;
            protoName = protoNames.get(groupName);
            if (protoName == null)
            {
                protoName = cell.getName();
                protoNames.put(protoName, protoName);
            }
            transitive.theseAreRelated(cell, protoName);
        }
        for (Cell[] groupLine : groupLines)
        {
            Cell firstCell = null;
            for (int i = 0; i < groupLine.length; i++)
            {
                if (groupLine[i] == null) continue;
                if (firstCell == null)
                    firstCell = groupLine[i];
                else
                    transitive.theseAreRelated(firstCell, groupLine[i]);
            }
        }

        // create the cell groups
        for (Iterator<Set<Object>> git = transitive.getSetsOfRelatives(); git.hasNext();)
        {
            Set<Object> group = git.next();
            Cell firstCell = null;
            for (Object o : group)
            {
                if (!(o instanceof Cell)) continue;
                Cell cell = (Cell)o;
                if (firstCell == null)
                    firstCell = cell;
                else
                    cell.joinGroup(firstCell);
            }
        }

//        // set main schematic cells
//        for (Cell[] groupLine : groupLines)
//        {
//            Cell firstCell = groupLine[0];
//            if (firstCell == null) continue;
//            if (firstCell.isSchematic() && firstCell.getNewestVersion() == firstCell)
//                firstCell.getCellGroup().setMainSchematics(firstCell);
//        }

        // sensibility check: shouldn't all cells with the same root name be in the same group?
        HashMap<String,Cell.CellGroup> cellGroups = new HashMap<String,Cell.CellGroup>();
        for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
        {
            Cell cell = it.next();
            String canonicName = TextUtils.canonicString(cell.getName());
            Cell.CellGroup group = cell.getCellGroup();
            Cell.CellGroup groupOfName = cellGroups.get(canonicName);
            if (groupOfName == null)
            {
                cellGroups.put(canonicName, group);
            } else
            {
                if (groupOfName != group)
                {
                    Input.errorLogger.logError(filePath + ", Library has multiple cells named '" +
                        canonicName + "' that are not in the same group", -1);

                    // Join groups with like-names cells
// 					for (Iterator cit = group.getCells(); cit.hasNext(); )
// 					{
// 						Cell cellInGroup = cit.next();
// 						cellInGroup.setCellGroup(groupOfName);
// 					}
                }
            }
        }

        lib.clearChanged();
        lib.setFromDisk();
        return false;
    }

    protected void readFromFile(boolean fromDelib) throws IOException {
        boolean ignoreCvsMergedContent = false;
		for(;;)
		{
			// get keyword from file
			String line = lineReader.readLine();
			if (line == null) break;

			// ignore blanks and comments
			if (line.length() == 0) continue;
			char first = line.charAt(0);
			if (first == '#') continue;

            if (line.startsWith("<<<<<<<")) {
                // This marks start of stuff merged from CVS, ignore this stuff
                ignoreCvsMergedContent = true;
                Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
                    ", CVS conflicts found: " + line, -1);
                continue;
            }
            if (ignoreCvsMergedContent && line.startsWith("=======")) {
                // This marks start of local stuff merged, use this stuff
                ignoreCvsMergedContent = false;
                continue;
            }
            if (line.startsWith(">>>>>>>")) {
                // This marks end of cvs merging
                continue;
            }
            if (ignoreCvsMergedContent) continue;

			if (first == 'C')
			{
                readCell(line);
                continue;
			}

			if (first == 'L')
			{
				// cross-library reference
				List<String> pieces = parseLine(line);
				if (pieces.size() != 2)
				{
					Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
						", External library declaration needs 2 fields: " + line, -1);
					continue;
				}
				curExternalLibName = unQuote(pieces.get(0));
				if (Library.findLibrary(curExternalLibName) != null) continue;

				// recurse
				readExternalLibraryFromFilename(unQuote(pieces.get(1)), getPreferredFileType());
				continue;
			}

			if (first == 'R')
			{
				// cross-library cell information
				List<String> pieces = parseLine(line);
//				if (pieces.size() != 5 && pieces.size( ) != 7)
 				int numPieces = revision == 1 ? 7 : 5;
 				if (pieces.size() != numPieces)
				{
					Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
//						", External cell declaration needs 5 or 7 fields: " + line, -1);
						", External cell declaration needs " + numPieces + " fields: " + line, -1);
					continue;
				}
				double lowX = TextUtils.atof(pieces.get(1));
				double highX = TextUtils.atof(pieces.get(2));
				double lowY = TextUtils.atof(pieces.get(3));
				double highY = TextUtils.atof(pieces.get(4));
//				if (pieces.size() > 5)
				if (revision == 1)
				{
					long cDate = Long.parseLong(pieces.get(5));
					long rDate = Long.parseLong(pieces.get(6));
				}
				Rectangle2D bounds = new Rectangle2D.Double(lowX, lowY, highX-lowX, highY-lowY);
				curExternalCellName = curExternalLibName + ":" + unQuote(pieces.get(0));
				externalCells.put(curExternalCellName, bounds);
				continue;
			}

			if (first == 'F')
			{
				// cross-library export information
				List<String> pieces = parseLine(line);
				if (pieces.size() != 3)
				{
					Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
						", External export declaration needs 3 fields: " + line, -1);
					continue;
				}
				String exportName = unQuote(pieces.get(0));
				double posX = TextUtils.atof(pieces.get(1));
				double posY = TextUtils.atof(pieces.get(1));
				externalExports.put(curExternalCellName + ":" + exportName, new Point2D.Double(posX, posY));
				continue;
			}

			if (first == 'H')
			{
				// parse header
				List<String> pieces = parseLine(line);
				if (pieces.size() < 2)
				{
					Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
						", Library declaration needs 2 fields: " + line, -1);
					continue;
				}
				version = Version.parseVersion(pieces.get(1));
				if (version == null)
				{
					Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
						", Badly formed version: " + pieces.get(1), -1);
					continue;
				}
				for (revision = 0; revision < revisions.length; revision++)
				{
					if (version.compareTo(Version.parseVersion(revisions[revision])) < 0) break;
				}
				if (revision < 1)
				{
					escapeChar = '^';
					pieces = parseLine(line);
				}
				curLibName = unQuote(pieces.get(0));
				if (version.compareTo(Version.getVersion()) > 0)
				{
					Input.errorLogger.logWarning(curReadFile + ", line " + lineReader.getLineNumber() +
						", Library " + curLibName + " comes from a NEWER version of Electric (" + version + ")", null, -1);
				}
				Variable[] vars = readVariables(pieces, 2, filePath, lineReader.getLineNumber());
                
                if (!fromDelib) {
                    realizeVariables(lib, vars);
                    lib.setVersion(version);
                }
				continue;
			}

			if (first == 'O')
			{
				// parse Tool information
				List<String> pieces = parseLine(line);
				String toolName = unQuote(pieces.get(0));
				Tool tool = Tool.findTool(toolName);
				if (tool == null)
				{
					Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
						", Cannot identify tool " + toolName, -1);
					continue;
				}

				// get additional meaning preferences starting at position 1
                Variable[] vars = readVariables(pieces, 1, filePath, lineReader.getLineNumber());
				if (topLevelLibrary)
					realizeMeaningPrefs(tool, vars);
//				addVariables(tool, pieces, 1, filePath, lineReader.getLineNumber());
				continue;
			}

			if (first == 'V')
			{
				// parse View information
				List<String> pieces = parseLine(line);
				String viewName = unQuote(pieces.get(0));
				View view = View.findView(viewName);
				if (view == null)
				{
					String viewAbbr = unQuote(pieces.get(1));
					view = View.newInstance(viewName, viewAbbr);
					if (view == null)
					{
						Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
							", Cannot create view " + viewName, -1);
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
				List<String> pieces = parseLine(line);
				String techName = unQuote(pieces.get(0));
				curTech = Technology.findTechnology(techName);
				if (curTech == null)
				{
					Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
						", Cannot identify technology " + techName, -1);
					continue;
				}
				curPrim = null;

				// get additional meaning preferences  starting at position 1
                Variable[] vars = readVariables(pieces, 1, filePath, lineReader.getLineNumber());
				if (topLevelLibrary)
					realizeMeaningPrefs(curTech, vars);
//				addVariables(curTech, pieces, 1, filePath, lineReader.getLineNumber());
				continue;
			}

			if (first == 'D')
			{
				// parse PrimitiveNode information
				List<String> pieces = parseLine(line);
				String primName = unQuote(pieces.get(0));
				if (curTech == null)
				{
					Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
						", Primitive node " + primName + " has no technology before it", -1);
					continue;
				}
				curPrim = findPrimitiveNode(curTech, primName);
				if (curPrim == null)
				{
					Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
						", Cannot identify primitive node " + primName, -1);
					continue;
				}

				// get additional variables starting at position 1
//				addVariables(curPrim, pieces, 1, filePath, lineReader.getLineNumber());
				continue;
			}

			if (first == 'P')
			{
				// parse PrimitivePort information
				List<String> pieces = parseLine(line);
				String primPortName = unQuote(pieces.get(0));
				if (curPrim == null)
				{
					Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
						", Primitive port " + primPortName + " has no primitive node before it", -1);
					continue;
				}

				PrimitivePort pp = (PrimitivePort)curPrim.findPortProto(primPortName);
				if (pp == null) pp = curTech.convertOldPortName(primPortName, curPrim);
				if (pp == null)
				{
					Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
						", Cannot identify primitive port " + primPortName, -1);
					continue;
				}

				// get additional variables starting at position 1
//				addVariables(pp, pieces, 1, filePath, lineReader.getLineNumber());
				continue;
			}

			if (first == 'W')
			{
				// parse ArcProto information
				List<String> pieces = parseLine(line);
				String arcName = unQuote(pieces.get(0));
				if (curTech == null)
				{
					Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
						", Primitive arc " + arcName + " has no technology before it", -1);
					continue;
				}
				ArcProto ap = curTech.findArcProto(arcName);
				if (ap == null)
				{
					Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
						", Cannot identify primitive arc " + arcName, -1);
					continue;
				}

				// get additional variables starting at position 1
//				addVariables(ap, pieces, 1, filePath, lineReader.getLineNumber());
				continue;
			}

			if (first == 'G')
			{
				// group information
				List<String> pieces = parseLine(line);
				Cell[] groupLine = new Cell[pieces.size()];
				Cell firstCell = null;
				for(int i=0; i<pieces.size(); i++)
				{
					String cellName = unQuote(pieces.get(i));
					if (cellName.length() == 0) continue;
					int colonPos = cellName.indexOf(':');
					if (colonPos >= 0) cellName = cellName.substring(colonPos+1);
					Cell cell = lib.findNodeProto(cellName);
					if (cell == null)
					{
						Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
							", Cannot find cell " + cellName, -1);
						break;
					}
					groupLine[i] = cell;
				}
				groupLines.add(groupLine);
				continue;
			}

			Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
				", Unrecognized line: " + line, -1);
		}
	}

    protected void readCell(String line) throws IOException {
        // grab a cell description
        List<String> pieces = parseLine(line);
        int numPieces = revision >= 2 ? 6 : revision == 1 ? 5 : 7;
        if (pieces.size() < numPieces)
        {
            Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
                ", Cell declaration needs " + numPieces + " fields: " + line, -1);
            return;
        }
        int fieldIndex = 0;
        String name;
        String groupName = null;
        if (revision >= 1) {
            name = unQuote(pieces.get(fieldIndex++));
            if (revision >= 2) {
                String s = pieces.get(fieldIndex++);
                if (s.length() > 0)
                    groupName = unQuote(s);
            }
        } else {
            name = unQuote(pieces.get(fieldIndex++));
            String viewAbbrev = pieces.get(fieldIndex++);
            String versionString = pieces.get(fieldIndex++);
            name = name + ";" + versionString + "{" + viewAbbrev + "}";
        }
        Cell newCell = Cell.newInstance(lib, name);
        if (newCell == null)
        {
            Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
                ", Unable to create cell " + name, -1);
            return;
        }
        Technology tech = Technology.findTechnology(unQuote(pieces.get(fieldIndex++)));
        newCell.setTechnology(tech);
        long cDate = Long.parseLong(pieces.get(fieldIndex++));
        long rDate = Long.parseLong(pieces.get(fieldIndex++));
        newCell.lowLevelSetCreationDate(new Date(cDate));
        newCell.lowLevelSetRevisionDate(new Date(rDate));

        // parse state information
        String stateInfo = pieces.get(fieldIndex++);
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

        // add variables
        assert fieldIndex == numPieces;
        Variable[] vars = readVariables(pieces, numPieces, filePath, lineReader.getLineNumber());
        realizeVariables(newCell, vars);

        // gather the contents of the cell into a list of Strings
        CellContents cc = new CellContents();
        cc.fileName = filePath;
        cc.lineNumber = lineReader.getLineNumber() + 1;
        cc.groupName = groupName;
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
        return;
    }

	/**
	 * Method called after all libraries have been read.
	 * Instantiates all of the Cell contents that were saved in "allCells".
	 */
// 	private void instantiateCellContents()
// 	{
// 		System.out.println("Creating the circuitry...");
// 		progress.setNote("Creating the circuitry");

// 		// count the number of lines that need to be processed
// 		numToProcess = 0;
// 		for(CellContents cc : allCells.values())
// 		{
// 			numToProcess += cc.cellStrings.size();
// 		}

// 		// instantiate all cells recursively
// 		numProcessed = 0;
// 		for(Cell cell : allCells.keySet())
// 		{
// 			CellContents cc = allCells.get(cell);
// 			if (cc.filledIn) continue;

// 			instantiateCellContent(cell, cc);
// 		}
// 	}

	/**
	 * Method to recursively create the contents of each cell in the library.
	 */
	protected void realizeCellsRecursively(Cell cell, HashSet<Cell> recursiveSetupFlag, String scaledCellName, double scale)
	{
		if (scaledCellName != null) return;
		CellContents cc = allCells.get(cell);
		if (cc == null || cc.filledIn) return;
		instantiateCellContent(cell, cc, recursiveSetupFlag);
		cellsConstructed++;
        setProgressValue(cellsConstructed * 100 / totalCells);
//		if (progress != null) progress.setProgress(cellsConstructed * 100 / totalCells);
		recursiveSetupFlag.add(cell);
        cell.loadExpandStatus();
	}

	/**
	 * Method called after all libraries have been read to instantiate a single Cell.
	 * @param cell the Cell to instantiate.
	 * @param cc the contents of that cell (the strings from the file).
	 */
	private void instantiateCellContent(Cell cell, CellContents cc, HashSet<Cell> recursiveSetupFlag)
	{
		int numStrings = cc.cellStrings.size();

		// map disk node names (duplicate node names written "sig"1 and "sig"2)
		HashMap<String,NodeInst> diskName = new HashMap<String,NodeInst>();

		// place all nodes
		for(int line=0; line<numStrings; line++)
		{
			String cellString = cc.cellStrings.get(line);
			char firstChar = cellString.charAt(0);
			if (firstChar != 'N' && firstChar != 'I') continue;
//			numProcessed++;
//			if ((numProcessed%100) == 0) progress.setProgress(numProcessed * 100 / numToProcess);

			// parse the node line
			List<String> pieces = parseLine(cellString);
			int numPieces = revision < 1 ? 10 : firstChar == 'N' ? 9 : 8;
			if (pieces.size() < numPieces)
			{
				String lineNumber = "";
				if (lineReader != null) lineNumber = ", line " + lineReader.getLineNumber();
				Input.errorLogger.logError(cc.fileName + lineNumber +
					", Node instance needs " + numPieces + " fields: " + cellString, cell, -1);
				continue;
			}
			String protoName = unQuote(pieces.get(0));
			// figure out the name for this node.  Handle the form: "Sig"12
			String diskNodeName = revision >= 1 ? pieces.get(1) : unQuote(pieces.get(1));
			String nodeName = diskNodeName;
			if (nodeName.charAt(0) == '"')
			{
				int lastQuote = nodeName.lastIndexOf('"');
				if (lastQuote > 1)
				{
					nodeName = nodeName.substring(1, lastQuote);
					if (revision >= 1) nodeName = unQuote(nodeName);
				}
			}
			String nameTextDescriptorInfo = pieces.get(2);
			double x = TextUtils.atof(pieces.get(3));
			double y = TextUtils.atof(pieces.get(4));

			String prefixName = lib.getName();
			NodeProto np = null;
			Library cellLib = lib;
			int colonPos = protoName.indexOf(':');
			if (colonPos < 0)
			{
				if (firstChar == 'I' || revision < 1)
					np = lib.findNodeProto(protoName);
				else if (cell.getTechnology() != null)
					np = findPrimitiveNode(cell.getTechnology(), protoName);
			} else
			{
				prefixName = protoName.substring(0, colonPos);
				protoName = protoName.substring(colonPos+1);
				if (firstChar == 'N')
				{
					Technology tech = Technology.findTechnology(prefixName);
					if (tech != null) np = findPrimitiveNode(tech, protoName);
				}
				if (firstChar == 'I' || revision < 1 && np == null)
				{
					if (prefixName.equalsIgnoreCase(curLibName)) np = lib.findNodeProto(protoName); else
					{
						cellLib = Library.findLibrary(prefixName);
						if (cellLib != null)
							np = cellLib.findNodeProto(protoName);
					}
				}
			}

			// make sure the subcell has been instantiated
			if (np != null && np instanceof Cell)
			{
				Cell subCell = (Cell)np;
				// subcell: make sure that cell is setup
				if (!recursiveSetupFlag.contains(subCell))
				{
					LibraryFiles reader = this;
					if (subCell.getLibrary() != cell.getLibrary())
						reader = getReaderForLib(subCell.getLibrary());

					// subcell: make sure that cell is setup
					if (reader != null)
						reader.realizeCellsRecursively(subCell, recursiveSetupFlag, null, 0);
				}
			}

			double wid = 0, hei = 0;
            boolean flipX = false, flipY = false;
			String orientString;
			String stateInfo;
			String textDescriptorInfo = "";
			if (firstChar == 'N' || revision < 1)
			{
				wid = TextUtils.atof(pieces.get(5));
				if (wid < 0 || wid == 0 && 1/wid < 0) {
                    if (revision >= 1)
    					Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
        					", Negative width " + pieces.get(5) + " of cell instance", cell, -1);
                    flipX = true;
                    wid = -wid;
                }
				hei = TextUtils.atof(pieces.get(6));
				if (hei < 0 || hei == 0 && 1/hei < 0) {
                    if (revision >= 1)
    					Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
        					", Negative height " + pieces.get(5) + " of cell instance", cell, -1);
                    flipY = true;
                    hei = -hei;
                }
				orientString = pieces.get(7);
				stateInfo = pieces.get(8);
				if (revision < 1)
					textDescriptorInfo = pieces.get(9);
			} else
			{
				if (np != null)
				{
					Rectangle2D bounds = ((Cell)np).getBounds();
					wid = bounds.getWidth();
					hei = bounds.getHeight();
				}
				orientString = pieces.get(5);
				stateInfo = pieces.get(6);
				textDescriptorInfo = pieces.get(7);
			}
			int angle = 0;
			for (int i = 0; i < orientString.length(); i++)
			{
				char ch = orientString.charAt(i);
				if (ch == 'X') flipX = !flipX;
				else if (ch == 'Y')	flipY = !flipY;
				else if (ch == 'R') angle += 900;
				else
				{
					angle += TextUtils.atoi(orientString.substring(i));
					break;
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
						", Unable to create dummy cell " + protoName + " in " + cellLib, cell, -1);
					continue;
				}
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					", Creating dummy cell " + protoName + " in " + cellLib, cell, -1);
				Rectangle2D bounds = externalCells.get(pieces.get(0));
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
// 			if (np instanceof Cell)
// 			{
// 				Cell subCell = (Cell)np;
// 				CellContents subCC = allCells.get(subCell);
// 				if (subCC != null)
// 				{
// 					if (!subCC.filledIn)
// 						instantiateCellContent(subCell, subCC);
// 				}
// 			}

			// parse state information in stateInfo field
            TextDescriptor nameTextDescriptor = loadTextDescriptor(nameTextDescriptorInfo, false, cc.fileName, cc.lineNumber + line);
            int flags = 0, techBits = 0;
            boolean expanded = false;
            // parse state information in jelibUserBits
			parseStateInfo:
            for(int i=0; i<stateInfo.length(); i++) {
                char chr = stateInfo.charAt(i);
                switch (chr) {
                    case 'E': /*flags = ImmutableNodeInst.EXPAND.set(flags, true);*/ break;
                    case 'L': flags = ImmutableNodeInst.LOCKED.set(flags, true); break;
                    case 'S': /*userBits |= NSHORT;*/ break; // deprecated
                    case 'V': flags = ImmutableNodeInst.VIS_INSIDE.set(flags, true); break;
                    case 'W': /*flags = ImmutableNodeInst.WIPED.set(flags, true);*/ break; // deprecated
                    case 'A': flags = ImmutableNodeInst.HARD_SELECT.set(flags, true); break;
                    default:
                        if (Character.isDigit(chr)) {
                            stateInfo = stateInfo.substring(i);
                            try {
                                techBits = Integer.parseInt(stateInfo);
                            } catch (NumberFormatException e) {
                                Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
                                        " (" + cell + ") bad node bits" + stateInfo, cell, -1);
                            }
                            break parseStateInfo;
                        }
                }
            }
           TextDescriptor protoTextDescriptor = loadTextDescriptor(textDescriptorInfo, false, cc.fileName, cc.lineNumber + line); 

			// create the node
            Orientation orient = Orientation.fromJava(angle, flipX, flipY);
			NodeInst ni = NodeInst.newInstance(cell, np, nodeName, nameTextDescriptor,
                    new EPoint(x, y), wid, hei, orient, flags, techBits, protoTextDescriptor, Input.errorLogger);
			if (ni == null)
			{
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					" (" + cell + ") cannot create node " + protoName, cell, -1);
				continue;
			}

			// insert into map of disk names
			diskName.put(diskNodeName, ni);

			// add variables in fields 10 and up
			Variable[] vars = readVariables(pieces, numPieces, cc.fileName, cc.lineNumber + line);
            realizeVariables(ni, vars);
		}

		// place all exports
		for(int line=0; line<numStrings; line++)
		{
			String cellString = cc.cellStrings.get(line);
			if (cellString.charAt(0) != 'E') continue;
//			numProcessed++;
//			if ((numProcessed%100) == 0) progress.setProgress(numProcessed * 100 / numToProcess);

			// parse the export line
			List<String> pieces = parseLine(cellString);
            if (revision >= 2 && pieces.size() == 1) {
                // Unused ExportId
                String exportName = unQuote(pieces.get(0));
                cell.getId().newExportId(exportName);
                continue;
            }
			int numPieces = revision >= 2 ? 6 : revision == 1 ? 5 : 7;
			if (pieces.size() < numPieces)
			{
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					", Export needs " + numPieces + " fields, has " + pieces.size() + ": " + cellString, cell, -1);
				continue;
			}
            int fieldIndex = 0;
			String exportName = unQuote(pieces.get(fieldIndex++));
            String exportUserName = null;
            if (revision >= 2) {
                String s = pieces.get(fieldIndex++);
                if (s.length() != 0)
                    exportUserName = unQuote(s);
            }
			// get text descriptor in field 1
			String textDescriptorInfo = pieces.get(fieldIndex++);
			String nodeName = revision >= 1 ? pieces.get(fieldIndex++) : unQuote(pieces.get(fieldIndex++));
			String portName = unQuote(pieces.get(fieldIndex++));
			Point2D pos = null;
			if (revision < 1)
			{
				double x = TextUtils.atof(pieces.get(fieldIndex++));
				double y = TextUtils.atof(pieces.get(fieldIndex++));
				pos = new Point2D.Double(x, y);
			}
    		// parse state information in field 6
            String userBits = pieces.get(fieldIndex++);
            assert fieldIndex == numPieces;
            
			PortInst pi = figureOutPortInst(cell, portName, nodeName, pos, diskName, cc.fileName, cc.lineNumber + line);
			if (pi == null) continue;

            TextDescriptor nameTextDescriptor = loadTextDescriptor(textDescriptorInfo, false, cc.fileName, cc.lineNumber + line);
			// parse state information
            boolean alwaysDrawn = false;
            boolean bodyOnly = false;
            int slashPos = userBits.indexOf('/');
            if (slashPos >= 0) {
                String extras = userBits.substring(slashPos);
                userBits = userBits.substring(0, slashPos);
                while (extras.length() > 0) {
                    switch (extras.charAt(1)) {
                        case 'A': alwaysDrawn = true; break;
                        case 'B': bodyOnly = true; break;
                    }
                    extras = extras.substring(2);
                }
            }
            PortCharacteristic ch = PortCharacteristic.findCharacteristicShort(userBits);
            if (ch == null) ch = PortCharacteristic.UNKNOWN;
            
			// create the export
			Export pp = Export.newInstance(cell, exportName, exportUserName, nameTextDescriptor, pi, alwaysDrawn, bodyOnly, ch, errorLogger);
			if (pp == null)
			{
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					" (" + cell + ") cannot create export " + exportName, pi.getNodeInst(), cell, null, -1);
				continue;
			}

            // add variables in tail fields
			Variable[] vars = readVariables(pieces, numPieces, cc.fileName, cc.lineNumber + line);
            realizeVariables(pp, vars);
		}

		// next place all arcs
		for(int line=0; line<numStrings; line++)
		{
			String cellString = cc.cellStrings.get(line);
			if (cellString.charAt(0) != 'A') continue;
//			numProcessed++;
//			if ((numProcessed%100) == 0) progress.setProgress(numProcessed * 100 / numToProcess);

			// parse the arc line
			List<String> pieces = parseLine(cellString);
			if (pieces.size() < 13)
			{
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					", Arc instance needs 13 fields: " + cellString, cell, -1);
				continue;
			}
			String protoName = unQuote(pieces.get(0));
			ArcProto ap = null;
			if (protoName.indexOf(':') >= 0)
				ap = ArcProto.findArcProto(protoName);
			else if (cell.getTechnology() != null)
				ap = cell.getTechnology().findArcProto(protoName);
			if (ap == null)
			{
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					" (" + cell + ") cannot find arc " + protoName, cell, -1);
				continue;
			}
			String diskArcName = revision >= 1 ? pieces.get(1) : unQuote(pieces.get(1));
			String arcName = diskArcName;
			if (arcName.charAt(0) == '"')
			{
				int lastQuote = arcName.lastIndexOf('"');
				if (lastQuote > 1)
				{
					arcName = arcName.substring(1, lastQuote);
					if (revision >= 1) arcName = unQuote(arcName);
				}
			}
			double wid = TextUtils.atof(pieces.get(3));

			String headNodeName = revision >= 1 ? pieces.get(5) : unQuote(pieces.get(5));
			String headPortName = unQuote(pieces.get(6));
			double headX = TextUtils.atof(pieces.get(7));
			double headY = TextUtils.atof(pieces.get(8));
			PortInst headPI = figureOutPortInst(cell, headPortName, headNodeName, new Point2D.Double(headX, headY), diskName, cc.fileName, cc.lineNumber + line);
			if (headPI == null) continue;

			String tailNodeName = revision >= 1 ? pieces.get(9) : unQuote(pieces.get(9));
			String tailPortName = unQuote(pieces.get(10));
			double tailX = TextUtils.atof(pieces.get(11));
			double tailY = TextUtils.atof(pieces.get(12));
			PortInst tailPI = figureOutPortInst(cell, tailPortName, tailNodeName, new Point2D.Double(tailX, tailY), diskName, cc.fileName, cc.lineNumber + line);
			if (tailPI == null) continue;

			// parse state information in field 4
			String stateInfo = pieces.get(4);
			boolean extended = true, directional = false, reverseEnds = false,
				skipHead = false, skipTail = false,
				tailNotExtended = false, headNotExtended = false,
				tailArrowed = false, headArrowed = false, bodyArrowed = false;
            // Default flags are false except FIXED_ANGLE
            // SLIDABLE 
			int flags = defaultArcFlags;
            int angle = 0;
            parseStateInfo:
			for(int i=0; i<stateInfo.length(); i++)
			{
				char chr = stateInfo.charAt(i);
                switch (chr) {
                    case 'R': flags = ImmutableArcInst.RIGID.set(flags, true); break;
                    case 'F': flags = ImmutableArcInst.FIXED_ANGLE.set(flags, false); break;
                    case 'S': flags = ImmutableArcInst.SLIDABLE.set(flags, true); break;
                    case 'A': flags = ImmutableArcInst.HARD_SELECT.set(flags, true); break;
                    case 'N': flags = ImmutableArcInst.TAIL_NEGATED.set(flags, true); break;
                    case 'G': flags = ImmutableArcInst.HEAD_NEGATED.set(flags, true); break;
                    case 'X': headArrowed = true; break;
                    case 'Y': tailArrowed = true; break;
                    case 'B': bodyArrowed = true; break;
                    case 'I': headNotExtended = true; break;
                    case 'J': tailNotExtended = true; break;

    				// deprecated
                    case 'E': extended = false; break;
                    case 'D': directional = true; break;
                    case 'V': reverseEnds = true; break;
                    case 'H': skipHead = true; break;
                    case 'T': skipTail = true; break;
                    default:
                        if (TextUtils.isDigit(chr))
                        {
                            angle = TextUtils.atoi(stateInfo.substring(i));
                            break parseStateInfo;
                        }
                }
			}

			// if old bits were used, convert them
			if (!extended || directional)
			{
				if (!extended) headNotExtended = tailNotExtended = true;
				if (directional)
				{
					if (reverseEnds) tailArrowed = true; else
						headArrowed = true;
					bodyArrowed = true;
				}
				if (skipHead) headArrowed = headNotExtended = false;
				if (skipTail) tailArrowed = tailNotExtended = false;
			}

			// set the bits
			flags = ImmutableArcInst.HEAD_EXTENDED.set(flags, !headNotExtended);
			flags = ImmutableArcInst.TAIL_EXTENDED.set(flags, !tailNotExtended);
			flags = ImmutableArcInst.HEAD_ARROWED.set(flags, headArrowed);
			flags = ImmutableArcInst.TAIL_ARROWED.set(flags, tailArrowed);
			flags = ImmutableArcInst.BODY_ARROWED.set(flags, bodyArrowed);

			// get the ard name text descriptor
			String nameTextDescriptorInfo = pieces.get(2);
			TextDescriptor nameTextDescriptor = loadTextDescriptor(nameTextDescriptorInfo, false, cc.fileName, cc.lineNumber + line);

            ArcInst ai = ArcInst.newInstance(cell, ap, arcName, nameTextDescriptor,
                    headPI, tailPI, new EPoint(headX, headY), new EPoint(tailX, tailY), wid, angle, flags);
			if (ai == null)
			{
				List<Geometric> geomList = new ArrayList<Geometric>();
				geomList.add(headPI.getNodeInst());
				geomList.add(tailPI.getNodeInst());
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					" (" + cell + ") cannot create arc " + protoName, geomList, null, cell, -1);
				continue;
			}

			// add variables in fields 13 and up
			Variable[] vars = readVariables(pieces, 13, cc.fileName, cc.lineNumber + line);
            realizeVariables(ai, vars);
		}
		cc.filledIn = true;
		cc.cellStrings = null;
	}

	/**
	 * Method to find the proper PortInst for a specified port on a node, at a given position.
	 * @param cell the cell in which this all resides.
	 * @param portName the name of the port (may be an empty string if there is only 1 port).
	 * @param nodeName the name of the node.
	 * @param pos the position of the port on the node.
	 * @param diskName a HashMap that maps node names to actual nodes.
	 * @param lineNumber the line number in the file being read (for error reporting).
	 * @return the PortInst specified (null if none can be found).
	 */
	private PortInst figureOutPortInst(Cell cell, String portName, String nodeName, Point2D pos, HashMap<String,NodeInst> diskName, String fileName, int lineNumber)
	{
		NodeInst ni = diskName.get(nodeName);
		if (ni == null)
		{
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				" (" + cell + ") cannot find node " + nodeName, cell, -1);
			return null;
		}

		PortInst pi = null;
		if (portName.length() == 0)
		{
			if (ni.getNumPortInsts() > 0)
				pi = ni.getPortInst(0);
		} else {
            PortProto pp = findPortProto(ni.getProto(), portName);
            if (pp != null)
                pi = ni.findPortInstFromProto(pp);
        }

		// primitives use the name match
//		if (!ni.isCellInstance()) return pi;

//		// make sure the port can handle the position
//		if (pi != null && pos != null)
//		{
//			Poly poly = pi.getPoly();
//			if (!(poly.isInside(pos) || poly.polyDistance(pos.getX(), pos.getY()) < TINYDISTANCE))
//			{
//				NodeProto np = ni.getProto();
//				Input.errorLogger.logError(fileName + ", line " + lineNumber +
//					" (" + cell + ") point (" + pos.getX() + "," + pos.getY() + ") does not fit in " +
//					pi + " which is centered at (" + poly.getCenterX() + "," + poly.getCenterY() + ")", new EPoint(pos.getX(), pos.getY()), cell, -1);
//				if (np instanceof Cell)
//					pi = null;
//			}
//		}
		if (pi != null) return pi;

		// see if this is a dummy cell
        Variable var = null;
        Cell subCell = null;
        if (ni.isCellInstance()) {
            subCell = (Cell)ni.getProto();
            var = subCell.getVar(IO_TRUE_LIBRARY);
            if (pos == null)
                pos = externalExports.get(subCell.getCellName().toString() + ":" + portName);
		}
        if (pos == null)
            pos = EPoint.ORIGIN;
		if (var == null)
		{
			// not a dummy cell: create a pin at the top level
			NodeInst portNI = NodeInst.newInstance(Generic.tech.universalPinNode, pos, 0, 0, cell);
			if (portNI == null)
			{
				Input.errorLogger.logError(fileName + ", line " + lineNumber +
					", Unable to create dummy node in " + cell + " (cannot create source node)", cell, -1);
				return null;
			}
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				", Port "+portName+" on "+ni.getProto() + " renamed or deleted, still used on node "+nodeName+" in " + cell, portNI, cell, null, -1);
			return portNI.getOnlyPortInst();
		}

		// a dummy cell: create a dummy export on it to fit this
		String name = portName;
		if (name.length() == 0) name = "X";
// 		AffineTransform unRot = ni.rotateIn();
// 		unRot.transform(pos, pos);
// 		AffineTransform unTrans = ni.translateIn();
// 		unTrans.transform(pos, pos);
		NodeInst portNI = NodeInst.newInstance(Generic.tech.universalPinNode, pos, 0, 0, subCell);
		if (portNI == null)
		{
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				", Unable to create export " + name + " on dummy " + subCell + " (cannot create source node)", cell, -1);
			return null;
		}
		PortInst portPI = portNI.getOnlyPortInst();
		Export pp = Export.newInstance(subCell, portPI, name, false);
		if (pp == null)
		{
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				", Unable to create export " + name + " on dummy " + subCell, cell, -1);
			return null;
		}
		pi = ni.findPortInstFromProto(pp);
		Input.errorLogger.logError(fileName + ", line " + lineNumber +
			", Creating export " + name + " on dummy " + subCell, cell, -1);

		return pi;
	}

	/**
	 * Method to parse a line from the file, breaking it into a List of Strings.
	 * Each field in the file is separated by "|".
	 * Quoted strings are handled properly, as are the escape character.
	 * @param line the text from the file.
	 * @return a List of Strings.
	 */
	private List<String> parseLine(String line)
	{
		List<String> stringPieces = new ArrayList<String>();
		int len = line.length();
		int pos = 1;
		int startPos = 1;
		boolean inQuote = false;
		while (pos < len)
		{
			char chr = line.charAt(pos++);
			if (chr == escapeChar)
			{
				pos++;
				continue;
			}
			if (chr == '"') inQuote = !inQuote;
			if (chr == '|' && !inQuote)
			{
				stringPieces.add(line.substring(startPos, pos - 1));
				startPos = pos;
			}
		}
		if (pos > len) pos = len;
		stringPieces.add(line.substring(startPos, pos));
		return stringPieces;
	}

	private String unQuote(String line)
	{
		int len = line.length();
		if (revision >= 1)
		{
			if (len < 2 || line.charAt(0) != '"') return line;
			int lastQuote = line.lastIndexOf('"');
			if (lastQuote != len - 1)
				return unQuote(line.substring(0, lastQuote + 1)) + line.substring(lastQuote + 1);
			line = line.substring(1, len - 1);
			len -= 2;
		} else
		{
			if (line.indexOf(escapeChar) < 0) return line;
		}
		StringBuffer sb = new StringBuffer();
		assert len == line.length();
		for(int i=0; i<len; i++)
		{
			char chr = line.charAt(i);
			if (chr == escapeChar)
			{
				i++;
				if (i >= len) break;
				chr = line.charAt(i);
				if (chr == 'n' && revision >= 1) chr = '\n';
				if (chr == 'r' && revision >= 1) chr = '\r';
			}
			sb.append(chr);
		}
		return sb.toString();
	}

	/**
	 * Method to read variables to an ElectricObject from a List of strings.
	 * @param pieces the array of Strings that described the ElectricObject.
	 * @param position the index in the array of strings where Variable descriptions begin.
	 * @param fileName the name of the file that this came from (for error reporting).
	 * @param lineNumber the line number in the file that this came from (for error reporting).
     * @return an array of Variables. 
	 */
	private Variable[] readVariables(List<String> pieces, int position, String fileName, int lineNumber)
	{
        variablesBuf.clear();
		int total = pieces.size();
		for(int i=position; i<total; i++)
		{
			String piece = pieces.get(i);
			int openPos = 0;
			boolean inQuote = false;
			for(; openPos < piece.length(); openPos++)
			{
				char chr = piece.charAt(openPos);
				if (chr == escapeChar) { openPos++;   continue; }
				if (chr == '"') inQuote = !inQuote;
				if (chr == '(' && !inQuote) break;
			}
			if (openPos >= piece.length())
			{
				Input.errorLogger.logError(fileName + ", line " + lineNumber +
					", Badly formed variable (no open parenthesis): " + piece, -1);
				continue;
			}
			String varName = unQuote(piece.substring(0, openPos));
			Variable.Key varKey = Variable.newKey(varName);
			int closePos = piece.indexOf(')', openPos);
			if (closePos < 0)
			{
				Input.errorLogger.logError(fileName + ", line " + lineNumber +
					", Badly formed variable (no close parenthesis): " + piece, -1);
				continue;
			}
			String varBits = piece.substring(openPos+1, closePos);
			int objectPos = closePos + 1;
			if (objectPos >= piece.length())
			{
				Input.errorLogger.logError(fileName + ", line " + lineNumber +
					", Variable type missing: " + piece, -1);
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
//				case 'X':
				case 'Y':
					break; // break from switch
				default:
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Variable type invalid: " + piece, -1);
					continue; // continue loop
			}
			Object obj = null;
			if (objectPos >= piece.length())
			{
				if (varType != 'X')
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Variable value missing: " + piece, -1);
					continue;
				}
			} else
			{
				if (piece.charAt(objectPos) == '[')
				{
					List<Object> objList = new ArrayList<Object>();
					objectPos++;
					while (objectPos < piece.length())
					{
						int start = objectPos;
						inQuote = false;
						while (objectPos < piece.length())
						{
							if (inQuote)
							{
								if (piece.charAt(objectPos) == escapeChar)
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
						Object oneObj = getVariableValue(piece.substring(start, objectPos), varType, fileName, lineNumber);
						objList.add(oneObj);
						if (piece.charAt(objectPos) == ']') break;
						objectPos++;
					}
					if (objectPos >= piece.length())
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Badly formed array (no closed bracket): " + piece, -1);
						continue;
					}
					else if (objectPos < piece.length() - 1)
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Badly formed array (extra characters after closed bracket): " + piece, -1);
						continue;
					}
					int limit = objList.size();
					Object [] objArray = null;
					switch (varType)
					{
						case 'B': objArray = new Boolean[limit];        break;
						case 'C': objArray = new CellId[limit];         break;
						case 'D': objArray = new Double[limit];         break;
						case 'E': objArray = new ExportId[limit];       break;
						case 'F': objArray = new Float[limit];          break;
						case 'G': objArray = new Long[limit];           break;
						case 'H': objArray = new Short[limit];          break;
						case 'I': objArray = new Integer[limit];        break;
						case 'L': objArray = new LibId[limit];         break;
						case 'O': objArray = new Tool[limit];           break;
						case 'P': objArray = new PrimitiveNode[limit];  break;
						case 'R': objArray = new ArcProto[limit];       break;
						case 'S': objArray = new String[limit];         break;
						case 'T': objArray = new Technology[limit];     break;
						case 'V': objArray = new EPoint[limit];        break;
						case 'Y': objArray = new Byte[limit];           break;
					}
					if (objArray == null && limit > 0)
					{
						System.out.println("HHEY, vartype="+varType+" on line "+lineNumber);
					}
					for(int j=0; j<limit; j++)
						objArray[j] = objList.get(j);
					obj = objArray;
				} else
				{
					// a scalar Variable
					obj = getVariableValue(piece.substring(objectPos), varType, fileName, lineNumber);
                    if (obj == null) {
                        // ????
                        continue;
                    }
				}
			}

			// create the variable
            TextDescriptor td = loadTextDescriptor(varBits, true, fileName, lineNumber);
            Variable d = Variable.newInstance(varKey, obj, td);
            variablesBuf.add(d);
		}
        return (Variable[])variablesBuf.toArray(Variable.NULL_ARRAY);
	}

	/**
	 * Method to load a TextDescriptor from a String description of it.
	 * @param var the Variable that this TextDescriptor resides on.
	 * It may be null if the TextDescriptor is on a NodeInst or Export.
	 * @param varBits the String that describes the TextDescriptor.
	 * @param fileName the name of the file that this came from (for error reporting).
	 * @param lineNumber the line number in the file that this came from (for error reporting).
	 * @return loaded TextDescriptor
	 */
	private TextDescriptor loadTextDescriptor(String varBits, boolean onVar, String fileName, int lineNumber)
	{
        HashMap<String,TextDescriptor> parsedDescriptors = onVar ? parsedDescriptorsT : parsedDescriptorsF;
        TextDescriptor td = parsedDescriptors.get(varBits);
        if (td != null) return td;
        
        boolean error = false;
        mtd.setCBits(0, 0, 0);
        if (!onVar) mtd.setDisplay(true);
		double xoff = 0, yoff = 0;
		for(int j=0; j<varBits.length(); j++)
		{
			char varBit = varBits.charAt(j);
			switch (varBit)
			{
				case 'D':		// display position
					mtd.setDisplay(true);
					j++;
					if (j >= varBits.length())
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Incorrect display specification: " + varBits, -1);
                        error = true;
						break;
					}
					switch (varBits.charAt(j))
					{
						case '5': mtd.setPos(TextDescriptor.Position.CENT);       break;
						case '8': mtd.setPos(TextDescriptor.Position.UP);         break;
						case '2': mtd.setPos(TextDescriptor.Position.DOWN);       break;
						case '4': mtd.setPos(TextDescriptor.Position.LEFT);       break;
						case '6': mtd.setPos(TextDescriptor.Position.RIGHT);      break;
						case '7': mtd.setPos(TextDescriptor.Position.UPLEFT);     break;
						case '9': mtd.setPos(TextDescriptor.Position.UPRIGHT);    break;
						case '1': mtd.setPos(TextDescriptor.Position.DOWNLEFT);   break;
						case '3': mtd.setPos(TextDescriptor.Position.DOWNRIGHT);  break;
						case '0': mtd.setPos(TextDescriptor.Position.BOXED);      break;
					}
					break;
				case 'N':		// display type
					mtd.setDispPart(TextDescriptor.DispPos.NAMEVALUE);
					break;
				case 'A':		// absolute text size
					int semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Bad absolute size (semicolon missing): " + varBits, -1);
                        error = true;
						break;
					}
					mtd.setAbsSize(TextUtils.atoi(varBits.substring(j+1, semiPos)));
					j = semiPos;
					break;
				case 'G':		// relative text size
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Bad relative size (semicolon missing): " + varBits, -1);
                        error = true;
						break;
					}
					mtd.setRelSize(TextUtils.atof(varBits.substring(j+1, semiPos)));
					j = semiPos;
					break;
				case 'X':		// X offset
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Bad X offset (semicolon missing): " + varBits, -1);
                        error = true;
						break;
					}
					xoff = TextUtils.atof(varBits.substring(j+1, semiPos));
					j = semiPos;
					break;
				case 'Y':		// Y offset
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Bad Y offset (semicolon missing): " + varBits, -1);
                        error = true;
						break;
					}
					yoff = TextUtils.atof(varBits.substring(j+1, semiPos));
					j = semiPos;
					break;
				case 'B':		// bold
					mtd.setBold(true);
					break;
				case 'I':		// italic
					mtd.setItalic(true);
					break;
				case 'L':		// underlined
					mtd.setUnderline(true);
					break;
				case 'F':		// font
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Bad font (semicolon missing): " + varBits, -1);
                        error = true;
						break;
					}
					TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(varBits.substring(j+1, semiPos));
                    if (af != null)
                        mtd.setFace(af.getIndex());
					j = semiPos;
					break;
				case 'C':		// color
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Bad color (semicolon missing): " + varBits, -1);
                        error = true;
						break;
					}
					mtd.setColorIndex(TextUtils.atoi(varBits.substring(j+1, semiPos)));
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
					mtd.setRotation(rot);
					break;
				case 'H':		// inheritable
					mtd.setInherit(true);
					break;
				case 'T':		// interior
					mtd.setInterior(true);
					break;
				case 'P':		// parameter
					mtd.setParam(true);
					break;
				case 'O':		// code
					j++;
					if (j >= varBits.length())
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Bad language specification: " + varBits, -1);
                        error = true;
						break;
					}
					char codeLetter = varBits.charAt(j);
					if (!onVar)
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Illegal use of language specification: " + varBits, -1);
						error = true;
						break;
					}
					if (codeLetter == 'J') mtd.setCode(TextDescriptor.Code.JAVA); else
					if (codeLetter == 'L') mtd.setCode(TextDescriptor.Code.SPICE); else
					if (codeLetter == 'T') mtd.setCode(TextDescriptor.Code.TCL); else
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Unknown language specification: " + varBits, -1);
                        error = true;
					}
					break;
				case 'U':		// units
					j++;
					if (j >= varBits.length())
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Bad units specification: " + varBits, -1);
                        error = true;
						break;
					}
					char unitsLetter = varBits.charAt(j);
					if (unitsLetter == 'R') mtd.setUnit(TextDescriptor.Unit.RESISTANCE); else
					if (unitsLetter == 'C') mtd.setUnit(TextDescriptor.Unit.CAPACITANCE); else
					if (unitsLetter == 'I') mtd.setUnit(TextDescriptor.Unit.INDUCTANCE); else
					if (unitsLetter == 'A') mtd.setUnit(TextDescriptor.Unit.CURRENT); else
					if (unitsLetter == 'V') mtd.setUnit(TextDescriptor.Unit.VOLTAGE); else
					if (unitsLetter == 'D') mtd.setUnit(TextDescriptor.Unit.DISTANCE); else
					if (unitsLetter == 'T') mtd.setUnit(TextDescriptor.Unit.TIME); else
					{
						Input.errorLogger.logError(fileName + ", line " + lineNumber +
							", Unknown units specification: " + varBits, -1);
                        error = true;
					}
					break;
			}
		}
		mtd.setOff(xoff, yoff);
		td = TextDescriptor.newTextDescriptor(mtd);
        if (!error) parsedDescriptors.put(varBits, td);
        return td;
	}

	/**
	 * Method to convert a String to an Object so that it can be stored in a Variable.
	 * @param piece the String to be converted.
	 * @param varType the type of the object to convert (a letter from the file).
	 * @param fileName the name of the file that this came from (for error reporting).
	 * @param lineNumber the line number in the file that this came from (for error reporting).
	 * @return the Object representation of the given String.
	 */
	private Object getVariableValue(String piece, char varType, String fileName, int lineNumber)
	{
		int colonPos;
		String libName;
		Library lib;
		int secondColonPos;
		String cellName;
		Cell cell;
		int commaPos;

		if (revision >= 1)
			piece = unQuote(piece);

		switch (varType)
		{
			case 'B':		// Boolean
				return new Boolean(piece.charAt(0)=='T' ? true : false);
			case 'C':		// Cell (should delay analysis until database is built!!!)
				if (piece.length() == 0) return null;
				colonPos = piece.indexOf(':');
				if (colonPos < 0)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed Cell (missing colon): " + piece, -1);
					break;
				}
				libName = piece.substring(0, colonPos);
				lib = Library.findLibrary(libName);
				if (lib == null)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown library: " + libName, -1);
					break;
				}
				cellName = piece.substring(colonPos+1);
				commaPos = cellName.indexOf(',');
				if (commaPos >= 0) cellName = cellName.substring(0, commaPos);
				cell = lib.findNodeProto(cellName);
				if (cell == null) {
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown Cell: " + piece, -1);
                    break;
                }
				return cell.getId();
			case 'D':		// Double
				return new Double(TextUtils.atof(piece));
			case 'E':		// Export (should delay analysis until database is built!!!)
				colonPos = piece.indexOf(':');
				if (colonPos < 0)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed Export (missing library colon): " + piece, -1);
					break;
				}
				libName = piece.substring(0, colonPos);
				lib = Library.findLibrary(libName);
				if (lib == null)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown library: " + libName, -1);
					break;
				}
				secondColonPos = piece.indexOf(':', colonPos+1);
				if (secondColonPos < 0)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed Export (missing cell colon): " + piece, -1);
					break;
				}
				cellName = piece.substring(colonPos+1, secondColonPos);
				cell = lib.findNodeProto(cellName);
				if (cell == null)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown Cell: " + piece, -1);
					break;
				}
				String exportName = piece.substring(secondColonPos+1);
				commaPos = exportName.indexOf(',');
				if (commaPos >= 0) exportName = exportName.substring(0, commaPos);
                Export pp = (Export)findPortProto(cell, exportName);
				if (pp == null) {
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown Export: " + piece, -1);
                    break;
                }
				return pp;
			case 'F':		// Float
				return new Float((float)TextUtils.atof(piece));
			case 'G':		// Long
				return Long.valueOf(piece);
			case 'H':		// Short
				return new Short((short)TextUtils.atoi(piece));
			case 'I':		// Integer
				return new Integer(TextUtils.atoi(piece));
			case 'L':		// Library (should delay analysis until database is built!!!)
				libName = piece;
				commaPos = libName.indexOf(',');
				if (commaPos >= 0) libName = libName.substring(0, commaPos);
				lib = Library.findLibrary(libName);
				if (lib == null) {
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown Library: " + piece, -1);
                    break;
                }
				return lib;
			case 'O':		// Tool
				String toolName = piece;
				commaPos = toolName.indexOf(',');
				if (commaPos >= 0) toolName = toolName.substring(0, commaPos);
				Tool tool = Tool.findTool(toolName);
				if (tool == null)
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown Tool: " + piece, -1);
				return tool;
			case 'P':		// PrimitiveNode
				colonPos = piece.indexOf(':');
				if (colonPos < 0)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed PrimitiveNode (missing colon): " + piece, -1);
					break;
				}
				String techName = piece.substring(0, colonPos);
				Technology tech = Technology.findTechnology(techName);
				if (tech == null)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown technology: " + techName, -1);
					break;
				}
				String nodeName = piece.substring(colonPos+1);
				commaPos = nodeName.indexOf(',');
				if (commaPos >= 0) nodeName = nodeName.substring(0, commaPos);
				PrimitiveNode np = findPrimitiveNode(tech, nodeName);
				if (np == null)
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown PrimitiveNode: " + piece, -1);
				return np;
			case 'R':		// ArcProto
				colonPos = piece.indexOf(':');
				if (colonPos < 0)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed ArcProto (missing colon): " + piece, -1);
					break;
				}
				techName = piece.substring(0, colonPos);
				tech = Technology.findTechnology(techName);
				if (tech == null)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown technology: " + techName, -1);
					break;
				}
				String arcName = piece.substring(colonPos+1);
				commaPos = arcName.indexOf(',');
				if (commaPos >= 0) arcName = arcName.substring(0, commaPos);
				ArcProto ap = tech.findArcProto(arcName);
				if (ap == null)
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown ArcProto: " + piece, -1);
				return ap;
			case 'S':		// String
				if (revision >= 1) return piece;
				if (piece.charAt(0) != '"')
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed string variable (missing open quote): " + piece, -1);
					break;
				}
				StringBuffer sb = new StringBuffer();
				int len = piece.length();
				int objectPos = 0;
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
				techName = piece;
				commaPos = techName.indexOf(',');
				if (commaPos >= 0) techName = techName.substring(0, commaPos);
				tech = Technology.findTechnology(techName);
				if (tech == null)
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown Technology: " + piece, -1);
				return tech;
			case 'V':		// Point2D
				double x = TextUtils.atof(piece);
				int slashPos = piece.indexOf('/');
				if (slashPos < 0)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed Point2D variable (missing slash): " + piece, -1);
					break;
				}
				double y = TextUtils.atof(piece.substring(slashPos+1));
				return new EPoint(x, y);
			case 'X':		// null
				return null;
			case 'Y':		// Byte
				return new Byte((byte)TextUtils.atoi(piece));
		}
		return null;
	}

	PrimitiveNode findPrimitiveNode(Technology tech, String name)
	{
		PrimitiveNode pn = (PrimitiveNode)tech.findNodeProto(name);
		if (pn != null) return pn;
		return tech.convertOldNodeName(name);
	}

    protected FileType getPreferredFileType() { return FileType.JELIB; }
}
