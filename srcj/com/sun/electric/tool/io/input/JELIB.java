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
 * the Free Software Foundation; either version 3 of the License, or
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

import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.id.ArcProtoId;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.ExportId;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.id.NodeProtoId;
import com.sun.electric.database.id.PortProtoId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
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
	private class CellContents
	{
        final int revision;
        private final Version version;
		boolean filledIn;
		int lineNumber;
        String groupName;
		List<String> cellStrings = new ArrayList<String>();
        List<NodeContents> nodes = new ArrayList<NodeContents>();
        List<ExportContents> exports = new ArrayList<ExportContents>();
        List<ArcContents> arcs = new ArrayList<ArcContents>();
		// map disk node names (duplicate node names written "sig"1 and "sig"2)
		HashMap<String,NodeContents> diskName = new HashMap<String,NodeContents>();
		String fileName;
        private HashMap<Technology,Technology.SizeCorrector> sizeCorrectors = new HashMap<Technology,Technology.SizeCorrector>();

		CellContents(int revision, Version version)
		{
            this.revision = revision;
            this.version = version;
			filledIn = false;
		}
        
        Technology.SizeCorrector getSizeCorrector(Technology tech) {
            Technology.SizeCorrector corrector = sizeCorrectors.get(tech);
            if (corrector == null) {
                corrector = tech.getSizeCorrector(version, projectSettings, true, false);
                sizeCorrectors.put(tech, corrector);
            }
            return corrector;
        }
    
	}

    static class NodeContents {
        int line;
        NodeProtoId protoId;
        String nodeName;
        TextDescriptor nameTextDescriptor;
        EPoint anchor;
        Orientation orient;
        EPoint size;
        TextDescriptor protoTextDescriptor;
        int flags;
        int techBits;
        Variable[] vars;
        
        NodeInst ni;
    }
    
    private static class ExportContents {
        int line;
        ExportId exportId;
        String exportUserName;
        NodeContents originalNode;
        PortProtoId originalPort;
        TextDescriptor nameTextDescriptor;
        PortCharacteristic ch;
        boolean alwaysDrawn;
        boolean bodyOnly;
        Variable[] vars;
        Point2D pos;
    }
    
    private static class ArcContents {
        int line;
        ArcProtoId arcProtoId;
        String arcName;
        TextDescriptor nameTextDescriptor;
        double diskWidth;
        NodeContents headNode;
        PortProtoId headPort;
        EPoint headPoint;
        NodeContents tailNode;
        PortProtoId tailPort;
        EPoint tailPoint;
        int angle;
        int flags;
        Variable[] vars;
    }
    
	private static String[] revisions =
	{
		// Revision 1
		"8.01aw",
		// Revision 2
		"8.04l",
	};

    private static int defaultArcFlags;
    static {
        defaultArcFlags = ImmutableArcInst.DEFAULT_FLAGS;
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
    @Override
    protected boolean readProjectSettings() {
        try {
            curLibName = null;
            version = null;
            curExternalLibName = "";
            curExternalCellName = "";
            curTech = null;
            curPrim = null;
            externalCells = new HashMap<String,Rectangle2D>();
            externalExports = new HashMap<String,Point2D.Double>();
            curReadFile = filePath;
            
            // do all the reading
            readFromFile(false, true);
            return false;
        } catch (IOException e) {
            Input.errorLogger.logError("End of file reached while reading " + filePath, -1);
            return true;
        }
    }

	/**
	 * Method to read a Library in new library file (.jelib) format.
	 * @return true on error.
	 */
    @Override
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
		curExternalLibName = "";
		curExternalCellName = "";
		curTech = null;
		curPrim = null;
		externalCells = new HashMap<String,Rectangle2D>();
		externalExports = new HashMap<String,Point2D.Double>();
		groupLines = new ArrayList<Cell[]>();
        curReadFile = filePath;

        // do all the reading
        readFromFile(false, false);

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
                protoName = groupName;
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

        lib.setFromDisk();
        return false;
    }

    protected void readFromFile(boolean fromDelib, boolean onlyProjectSettings) throws IOException {
		int revision = revisions.length;
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
            if (onlyProjectSettings && first != 'H' && first != 'O' && first != 'T') continue;

			if (first == 'C')
			{
                readCell(revision, line);
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
				curExternalLibName = unQuote(revision, pieces.get(0));
				if (Library.findLibrary(curExternalLibName) != null) continue;

				// recurse
				readExternalLibraryFromFilename(unQuote(revision, pieces.get(1)), getPreferredFileType());
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
					Long.parseLong(pieces.get(5));		// ignore cdate
					Long.parseLong(pieces.get(6));		// ignore rdate
				}
				Rectangle2D bounds = new Rectangle2D.Double(lowX, lowY, highX-lowX, highY-lowY);
				curExternalCellName = curExternalLibName + ":" + unQuote(revision, pieces.get(0));
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
				String exportName = unQuote(revision, pieces.get(0));
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
				curLibName = unQuote(revision, pieces.get(0));
				if (version.compareTo(Version.getVersion()) > 0)
				{
					Input.errorLogger.logWarning(curReadFile + ", line " + lineReader.getLineNumber() +
						", Library " + curLibName + " comes from a NEWER version of Electric (" + version + ")", null, -1);
				}
				Variable[] vars = readVariables(revision, lib, pieces, 2, filePath, lineReader.getLineNumber());
                
                if (!fromDelib && !onlyProjectSettings) {
                    realizeVariables(lib, vars);
                    lib.setVersion(version);
                }
				continue;
			}

			if (first == 'O')
			{
				// parse Tool information
				List<String> pieces = parseLine(line);
				String toolName = unQuote(revision, pieces.get(0));
				Tool tool = Tool.findTool(toolName);
				if (tool == null)
				{
					Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
						", Cannot identify tool " + toolName, -1);
					continue;
				}

				// get additional meaning preferences starting at position 1
                Variable[] vars = readVariables(revision, null, pieces, 1, filePath, lineReader.getLineNumber());
                realizeMeaningPrefs(tool, vars);
//				addVariables(tool, pieces, 1, filePath, lineReader.getLineNumber());
				continue;
			}

			if (first == 'V')
			{
				// parse View information
				List<String> pieces = parseLine(line);
				String viewName = unQuote(revision, pieces.get(0));
				View view = View.findView(viewName);
				if (view == null)
				{
					String viewAbbr = unQuote(revision, pieces.get(1));
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
				String techName = unQuote(revision, pieces.get(0));
				curTech = findTechnology(techName);
				if (curTech == null)
				{
					Input.errorLogger.logError(curReadFile + ", line " + lineReader.getLineNumber() +
						", Cannot identify technology " + techName, -1);
					continue;
				}
				curPrim = null;

				// get additional meaning preferences  starting at position 1
                Variable[] vars = readVariables(revision, null, pieces, 1, filePath, lineReader.getLineNumber());
				realizeMeaningPrefs(curTech, vars);
//				addVariables(curTech, pieces, 1, filePath, lineReader.getLineNumber());
				continue;
			}

			if (first == 'D')
			{
				// parse PrimitiveNode information
				List<String> pieces = parseLine(line);
				String primName = unQuote(revision, pieces.get(0));
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
				String primPortName = unQuote(revision, pieces.get(0));
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
				String arcName = unQuote(revision, pieces.get(0));
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
				for(int i=0; i<pieces.size(); i++)
				{
					String cellName = unQuote(revision, pieces.get(i));
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

    void readCell(int revision, String line) throws IOException {
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
            name = unQuote(revision, pieces.get(fieldIndex++));
            if (revision >= 2) {
                String s = pieces.get(fieldIndex++);
                if (s.length() > 0)
                    groupName = unQuote(revision, s);
            }
        } else {
            name = unQuote(revision, pieces.get(fieldIndex++));
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
        String techName = unQuote(revision, pieces.get(fieldIndex++));
        TechId techId = idManager.newTechId(techName);
        Technology tech = findTechnology(techName);
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
        Variable[] vars = readVariables(revision, newCell, pieces, numPieces, filePath, lineReader.getLineNumber());
        realizeVariables(newCell, vars);

        // gather the contents of the cell into a list of Strings
        CellId cellId = newCell.getId();
        LibId libId = lib.getId();
        assert libId == cellId.libId;
        CellContents cc = new CellContents(revision, version);
        cc.fileName = filePath;
        cc.lineNumber = lineReader.getLineNumber() + 1;
        cc.groupName = groupName;
        for(;;)
        {
            String nextLine = lineReader.readLine();
            if (nextLine == null) break;
            if (nextLine.length() == 0) continue;
            char nextFirst = nextLine.charAt(0);
            if (nextFirst == 'X') break;
            switch (nextFirst) {
                case '#':
                    break;
                case 'N':
                case 'I':
                    parseNode(revision, nextLine, newCell, techId, cc);
                    break;
                case 'E':
                    parseExport(revision, nextLine, newCell, cc);
                    break;
                case 'A':
                    parseArc(revision, nextLine, newCell, techId, cc);
                    break;
                default:
                    cc.cellStrings.add(nextLine);
            }
        }

        // remember the contents of the cell for later
        allCells.put(newCell, cc);
        return;
    }

    private void parseNode(int revision, String cellString, Cell cell, TechId techId, CellContents cc) {
        CellId cellId = cell.getId();
        LibId libId = cellId.libId;
        NodeContents n = new NodeContents();
        n.line = lineReader.getLineNumber();

        // parse the node line
        List<String> pieces = parseLine(cellString);
        char firstChar = cellString.charAt(0);
        int numPieces = revision < 1 ? 10 : firstChar == 'N' ? 9 : 8;
        if (pieces.size() < numPieces)
        {
            String lineNumber = "";
            if (lineReader != null) lineNumber = ", line " + lineReader.getLineNumber();
            Input.errorLogger.logError(filePath + lineNumber +
                ", Node instance needs " + numPieces + " fields: " + cellString, cell, -1);
            return;
        }
        String protoName = unQuote(revision, pieces.get(0));
        // figure out the name for this node.  Handle the form: "Sig"12
        String diskNodeName = revision >= 1 ? pieces.get(1) : unQuote(revision, pieces.get(1));
        String nodeName = diskNodeName;
        if (nodeName.charAt(0) == '"')
        {
            int lastQuote = nodeName.lastIndexOf('"');
            if (lastQuote > 1)
            {
                nodeName = nodeName.substring(1, lastQuote);
                if (revision >= 1) nodeName = unQuote(revision, nodeName);
            }
        }
        n.nodeName = nodeName;
        String nameTextDescriptorInfo = pieces.get(2);
        double x = TextUtils.atof(pieces.get(3));
        double y = TextUtils.atof(pieces.get(4));

        String prefixName = lib.getName();
        int colonPos = protoName.indexOf(':');
        if (colonPos < 0)
        {
            if (firstChar == 'I' || revision < 1)
                n.protoId = libId.newCellId(CellName.parseName(protoName));
            else
                n.protoId = techId.newPrimitiveNodeId(protoName);
        } else
        {
            prefixName = protoName.substring(0, colonPos);
            protoName = protoName.substring(colonPos+1);
            if (firstChar == 'I' || revision < 1 && protoName.indexOf('{') >= 0) {
                LibId nodeLibId = prefixName.equals(curLibName) ? libId : idManager.newLibId(prefixName);
                n.protoId = nodeLibId.newCellId(CellName.parseName(protoName));
            } else {
                n.protoId = idManager.newTechId(prefixName).newPrimitiveNodeId(protoName);
            }
        }

        n.size = EPoint.ORIGIN;
        boolean flipX = false, flipY = false;
        String orientString;
        String stateInfo;
        String textDescriptorInfo = "";
        if (firstChar == 'N' || revision < 1)
        {
            double wid = TextUtils.atof(pieces.get(5));
            if (revision < 1 && (wid < 0 || wid == 0 && 1/wid < 0)) {
                flipX = true;
                wid = -wid;
            }
            double hei = TextUtils.atof(pieces.get(6));
            if (revision < 1 && (hei < 0 || hei == 0 && 1/hei < 0)) {
                flipY = true;
                hei = -hei;
            }
            if (n.protoId instanceof PrimitiveNodeId)
                n.size = EPoint.fromLambda(wid, hei);
            orientString = pieces.get(7);
            stateInfo = pieces.get(8);
            if (revision < 1)
                textDescriptorInfo = pieces.get(9);
        } else
        {
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

        // parse state information in stateInfo field
        n.nameTextDescriptor = loadTextDescriptor(nameTextDescriptorInfo, false, filePath, lineReader.getLineNumber());
        int flags = 0, techBits = 0;
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
                            Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
                                    " (" + cell + ") bad node bits" + stateInfo, cell, -1);
                        }
                        break parseStateInfo;
                    }
            }
        }
        n.flags = flags;
        n.techBits = techBits;
        n.protoTextDescriptor = loadTextDescriptor(textDescriptorInfo, false, filePath, lineReader.getLineNumber()); 

        // create the node
        n.orient = Orientation.fromJava(angle, flipX, flipY);
        n.anchor = EPoint.fromLambda(x, y);

        // add variables in fields 10 and up
        n.vars = readVariables(revision, null, pieces, numPieces, filePath, lineReader.getLineNumber());
        cc.nodes.add(n);
        // insert into map of disk names
        cc.diskName.put(diskNodeName, n);
    }
    
    private void parseExport(int revision, String cellString, Cell cell, CellContents cc) {
        CellId cellId = cell.getId();
        ExportContents e = new ExportContents();
        e.line = lineReader.getLineNumber();

        // parse the export line
        List<String> pieces = parseLine(cellString);
        if (revision >= 2 && pieces.size() == 1) {
            // Unused ExportId
            String exportName = unQuote(revision, pieces.get(0));
            cellId.newPortId(exportName);
            return;
        }
        int numPieces = revision >= 2 ? 6 : revision == 1 ? 5 : 7;
        if (pieces.size() < numPieces)
        {
            Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
                ", Export needs " + numPieces + " fields, has " + pieces.size() + ": " + cellString, cell, -1);
            return;
        }
        int fieldIndex = 0;
        String exportName = unQuote(revision, pieces.get(fieldIndex++));
        String exportUserName = null;
        if (revision >= 2) {
            String s = pieces.get(fieldIndex++);
            if (s.length() != 0)
                exportUserName = unQuote(revision, s);
        }
        if (exportUserName == null || exportName.equals(exportUserName))
            exportName = Name.findName(exportName).toString(); // save memory using String from Name
        e.exportId = cellId.newPortId(exportName);
        e.exportUserName = exportUserName;
        // get text descriptor in field 1
        String textDescriptorInfo = pieces.get(fieldIndex++);
        String nodeName = revision >= 1 ? pieces.get(fieldIndex++) : unQuote(revision, pieces.get(fieldIndex++));
        e.originalNode = cc.diskName.get(nodeName);
        String portName = unQuote(revision, pieces.get(fieldIndex++));
        e.originalPort = e.originalNode.protoId.newPortId(portName);
        Point2D pos = null;
        if (revision < 1)
        {
            double x = TextUtils.atof(pieces.get(fieldIndex++));
            double y = TextUtils.atof(pieces.get(fieldIndex++));
            pos = new Point2D.Double(x, y);
        }
        e.pos = pos;
        // parse state information in field 6
        String userBits = pieces.get(fieldIndex++);
        assert fieldIndex == numPieces;

        e.nameTextDescriptor = loadTextDescriptor(textDescriptorInfo, false, filePath, lineReader.getLineNumber());
        // parse state information
        int slashPos = userBits.indexOf('/');
        if (slashPos >= 0) {
            String extras = userBits.substring(slashPos);
            userBits = userBits.substring(0, slashPos);
            while (extras.length() > 0) {
                switch (extras.charAt(1)) {
                    case 'A': e.alwaysDrawn = true; break;
                    case 'B': e.bodyOnly = true; break;
                }
                extras = extras.substring(2);
            }
        }
        PortCharacteristic ch = PortCharacteristic.findCharacteristicShort(userBits);
        e.ch = ch != null ? ch : PortCharacteristic.UNKNOWN;

        // add variables in tail fields
        e.vars = readVariables(revision, null, pieces, numPieces, filePath, lineReader.getLineNumber());
        cc.exports.add(e);
    }
    
    private void parseArc(int revision, String cellString, Cell cell, TechId techId, CellContents cc) {
        ArcContents a = new ArcContents();
        a.line = lineReader.getLineNumber();
        
        // parse the arc line
        List<String> pieces = parseLine(cellString);
        if (pieces.size() < 13)
        {
            Input.errorLogger.logError(filePath + ", line " + lineReader.getLineNumber() +
                ", Arc instance needs 13 fields: " + cellString, cell, -1);
            return;
        }
        String protoName = unQuote(revision, pieces.get(0));
        ArcProto ap = null;
        int indexOfColon = protoName.indexOf(':');
        if (indexOfColon >= 0) {
            techId = idManager.newTechId(protoName.substring(0, indexOfColon));
            protoName = protoName.substring(indexOfColon + 1);
        }
        a.arcProtoId = techId.newArcProtoId(protoName);
        String diskArcName = revision >= 1 ? pieces.get(1) : unQuote(revision, pieces.get(1));
        String arcName = diskArcName;
        if (arcName.charAt(0) == '"')
        {
            int lastQuote = arcName.lastIndexOf('"');
            if (lastQuote > 1)
            {
                arcName = arcName.substring(1, lastQuote);
                if (revision >= 1) arcName = unQuote(revision, arcName);
            }
        }
        a.arcName = arcName;
        a.diskWidth = TextUtils.atof(pieces.get(3));

        String headNodeName = revision >= 1 ? pieces.get(5) : unQuote(revision, pieces.get(5));
        String headPortName = unQuote(revision, pieces.get(6));
        double headX = TextUtils.atof(pieces.get(7));
        double headY = TextUtils.atof(pieces.get(8));
        a.headNode = cc.diskName.get(headNodeName);
        a.headPort = a.headNode.protoId.newPortId(headPortName);
        a.headPoint = EPoint.fromLambda(headX, headY);

        String tailNodeName = revision >= 1 ? pieces.get(9) : unQuote(revision, pieces.get(9));
        String tailPortName = unQuote(revision, pieces.get(10));
        double tailX = TextUtils.atof(pieces.get(11));
        double tailY = TextUtils.atof(pieces.get(12));
        a.tailNode = cc.diskName.get(tailNodeName);
        a.tailPort = a.tailNode.protoId.newPortId(tailPortName);
        a.tailPoint = EPoint.fromLambda(tailX, tailY);

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
        a.angle = angle;
        a.flags = flags;

        // get the ard name text descriptor
        String nameTextDescriptorInfo = pieces.get(2);
        a.nameTextDescriptor = loadTextDescriptor(nameTextDescriptorInfo, false, filePath, lineReader.getLineNumber());

        // add variables in fields 13 and up
        a.vars = readVariables(revision, null, pieces, 13, filePath, lineReader.getLineNumber());
        cc.arcs.add(a);
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

		// place all nodes
        for (NodeContents n: cc.nodes) {
            int line = n.line;

			String prefixName = lib.getName();
			NodeProto np = null;
            NodeProtoId protoId = n.protoId;
            if (protoId instanceof CellId)
                np = database.getCell((CellId)protoId);
            else {
                PrimitiveNodeId pnId = (PrimitiveNodeId)protoId;
                Technology tech = findTechnology(pnId.techId.techName);
                if (tech != null)
                    np = findPrimitiveNode(tech, pnId.name);
                if (np == null)
                    protoId = idManager.newLibId(pnId.techId.techName).newCellId(CellName.parseName(pnId.name));
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

            EPoint size = n.size;
            if (np instanceof PrimitiveNode) {
                PrimitiveNode pn = (PrimitiveNode)np;
                Technology.SizeCorrector sizeCorrector = cc.getSizeCorrector(pn.getTechnology());
                size = sizeCorrector.getSizeFromDisk(pn, size.getLambdaX(), size.getLambdaY());
            }

			if (np == null)
			{
                CellId dummyCellId = (CellId)protoId;
                String protoName = dummyCellId.cellName.toString();
                Library cellLib = database.getLib(dummyCellId.libId);
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
				Rectangle2D bounds = externalCells.get(dummyCellId.toString());
				if (bounds == null)
				{
					Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
						", Warning: cannot find information about external cell " + dummyCellId, cell, -1);
					NodeInst.newInstance(Generic.tech().invisiblePinNode, new Point2D.Double(0,0), 0, 0, dummyCell);
				} else
				{
					NodeInst.newInstance(Generic.tech().invisiblePinNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
						bounds.getWidth(), bounds.getHeight(), dummyCell);
				}

				// mark this as a dummy cell
				dummyCell.newVar(IO_TRUE_LIBRARY, prefixName);
				dummyCell.newVar(IO_DUMMY_OBJECT, protoName);
				np = dummyCell;
			}

			// create the node
			NodeInst ni = NodeInst.newInstance(cell, np, n.nodeName, n.nameTextDescriptor,
                    n.anchor, size, n.orient, n.flags, n.techBits, n.protoTextDescriptor, Input.errorLogger);
			if (ni == null)
			{
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					" (" + cell + ") cannot create node " + n.protoId, cell, -1);
				continue;
			}

			// add variables
            realizeVariables(ni, n.vars);
            
			// insert into map of disk names
            n.ni = ni;

		}

		// place all exports
        CellId cellId = cell.getId();
        for (ExportContents e: cc.exports) {
            int line = e.line;

			PortInst pi = figureOutPortInst(cell, e.originalPort.externalId, e.originalNode, e.pos, cc.fileName, line);
			if (pi == null) continue;
            
			// create the export
			Export pp = Export.newInstance(cell, e.exportId, e.exportUserName, e.nameTextDescriptor, pi,
                    e.alwaysDrawn, e.bodyOnly, e.ch, errorLogger);
			if (pp == null)
			{
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					" (" + cell + ") cannot create export " + e.exportUserName, pi.getNodeInst(), cell, null, -1);
				continue;
			}

            // add variables in tail fields
            realizeVariables(pp, e.vars);
		}

		// next place all arcs
		for(ArcContents a: cc.arcs) {
            int line = a.line;
            
            ArcProto ap = null;
            Technology tech = findTechnology(a.arcProtoId.techId.techName);
            if (tech != null)
                ap = tech.findArcProto(a.arcProtoId.name);
			if (ap == null)
			{
				Input.errorLogger.logError(cc.fileName + ", line " + (cc.lineNumber + line) +
					" (" + cell + ") cannot find arc " + a.arcProtoId, cell, -1);
				continue;
			}
			long gridExtendOverMin = cc.getSizeCorrector(ap.getTechnology()).getExtendFromDisk(ap, a.diskWidth);

			PortInst headPI = figureOutPortInst(cell, a.headPort.externalId, a.headNode, a.headPoint, cc.fileName, line);
			if (headPI == null) continue;

			PortInst tailPI = figureOutPortInst(cell, a.tailPort.externalId, a.tailNode, a.tailPoint, cc.fileName, line);
			if (tailPI == null) continue;

            ArcInst ai = ArcInst.newInstance(cell, ap, a.arcName, a.nameTextDescriptor,
                    headPI, tailPI, a.headPoint, a.tailPoint, gridExtendOverMin, a.angle, a.flags);
			if (ai == null)
			{
				List<Geometric> geomList = new ArrayList<Geometric>();
				geomList.add(headPI.getNodeInst());
				geomList.add(tailPI.getNodeInst());
				Input.errorLogger.logError(cc.fileName + ", line " + line +
					" (" + cell + ") cannot create arc " + a.arcProtoId, geomList, null, cell, 2);
				continue;
			}
            realizeVariables(ai, a.vars);
		}
		cc.filledIn = true;
		cc.cellStrings = null;
	}

	/**
	 * Method to find the proper PortInst for a specified port on a node, at a given position.
	 * @param cell the cell in which this all resides.
	 * @param portName the name of the port (may be an empty string if there is only 1 port).
	 * @param n the node.
	 * @param pos the position of the port on the node.
	 * @param lineNumber the line number in the file being read (for error reporting).
	 * @return the PortInst specified (null if none can be found).
	 */
	private PortInst figureOutPortInst(Cell cell, String portName, NodeContents n, Point2D pos, String fileName, int lineNumber)
	{
		NodeInst ni = n != null ? n.ni : null;
		if (ni == null)
		{
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				" (" + cell + ") cannot find node " + n.nodeName, cell, -1);
			return null;
		}

		PortInst pi = null;
        PortProto pp = findPortProto(ni.getProto(), portName);
        if (pp != null)
            pi = ni.findPortInstFromProto(pp);

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
            pos = ni.getAnchorCenter().lambdaMutable();
		if (var == null)
		{
			// not a dummy cell: create a pin at the top level
			NodeInst portNI = NodeInst.newInstance(Generic.tech().universalPinNode, pos, 0, 0, cell);
			if (portNI == null)
			{
				Input.errorLogger.logError(fileName + ", line " + lineNumber +
					", Unable to create dummy node in " + cell + " (cannot create source node)", cell, -1);
				return null;
			}
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				", Port "+portName+" on "+ni.getProto() + " renamed or deleted, still used on node "+n.nodeName+" in " + cell, portNI, cell, null, -1);
			return portNI.getOnlyPortInst();
		}

		// a dummy cell: create a dummy export on it to fit this
		String name = portName;
		if (name.length() == 0) name = "X";
        ni.transformIn().transform(pos, pos);
		NodeInst portNI = NodeInst.newInstance(Generic.tech().universalPinNode, pos, 0, 0, subCell);
		if (portNI == null)
		{
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				", Unable to create export " + name + " on dummy " + subCell + " (cannot create source node)", cell, -1);
			return null;
		}
		PortInst portPI = portNI.getOnlyPortInst();
		Export export = Export.newInstance(subCell, portPI, name, false);
		if (export == null)
		{
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				", Unable to create export " + name + " on dummy " + subCell, cell, -1);
			return null;
		}
		pi = ni.findPortInstFromProto(export);
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

	private String unQuote(int revision, String line)
	{
		int len = line.length();
		if (revision >= 1)
		{
			if (len < 2 || line.charAt(0) != '"') return line;
			int lastQuote = line.lastIndexOf('"');
			if (lastQuote != len - 1)
				return unQuote(revision, line.substring(0, lastQuote + 1)) + line.substring(lastQuote + 1);
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
     * @param revision the revision of JELIB format
	 * @param pieces the array of Strings that described the ElectricObject.
	 * @param position the index in the array of strings where Variable descriptions begin.
	 * @param fileName the name of the file that this came from (for error reporting).
	 * @param lineNumber the line number in the file that this came from (for error reporting).
     * @return an array of Variables. 
	 */
	private Variable[] readVariables(int revision, ElectricObject parentObj, List<String> pieces, int position, String fileName, int lineNumber)
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
			String varName = unQuote(revision, piece.substring(0, openPos));
			Variable.Key varKey = Variable.newKey(varName, parentObj);
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
				continue;
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
						Object oneObj = getVariableValue(revision, piece.substring(start, objectPos), varType, fileName, lineNumber);
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
						case 'L': objArray = new LibId[limit];          break;
						case 'O': objArray = new Tool[limit];           break;
						case 'P': objArray = new PrimitiveNodeId[limit];break;
						case 'R': objArray = new ArcProtoId[limit];     break;
						case 'S': objArray = new String[limit];         break;
						case 'T': objArray = new TechId[limit];         break;
						case 'V': objArray = new EPoint[limit];         break;
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
					obj = getVariableValue(revision, piece.substring(objectPos), varType, fileName, lineNumber);
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
        return variablesBuf.toArray(Variable.NULL_ARRAY);
	}

	/**
	 * Method to load a TextDescriptor from a String description of it.
	 * @param varBits the String that describes the TextDescriptor.
     * @param onVar true if this TextDescriptor resides on a Variable
	 * It may be false if the TextDescriptor is on a NodeInst or Export.
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
        if (!onVar) mtd.setDisplay(TextDescriptor.Display.SHOWN);
		double xoff = 0, yoff = 0;
		for(int j=0; j<varBits.length(); j++)
		{
			char varBit = varBits.charAt(j);
			switch (varBit)
			{
				case 'D':		// display position
				case 'd':		// display position
					mtd.setDisplay(varBit == 'D' ? TextDescriptor.Display.SHOWN : TextDescriptor.Display.HIDDEN);
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
     * @param revision the revision of JELIB format.
	 * @param piece the String to be converted.
	 * @param varType the type of the object to convert (a letter from the file).
	 * @param fileName the name of the file that this came from (for error reporting).
	 * @param lineNumber the line number in the file that this came from (for error reporting).
	 * @return the Object representation of the given String.
	 */
	private Object getVariableValue(int revision, String piece, char varType, String fileName, int lineNumber)
	{
		int colonPos;
		String libName;
		Library lib;
		int secondColonPos;
		String cellName;
		Cell cell;
		int commaPos;

		if (revision >= 1)
			piece = unQuote(revision, piece);

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
                LibId libId = idManager.newLibId(libName);
				cellName = piece.substring(colonPos+1);
				commaPos = cellName.indexOf(',');
				if (commaPos >= 0) cellName = cellName.substring(0, commaPos);
                return libId.newCellId(CellName.parseName(cellName));
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
                libId = idManager.newLibId(libName);
				secondColonPos = piece.indexOf(':', colonPos+1);
				if (secondColonPos < 0)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed Export (missing cell colon): " + piece, -1);
					break;
				}
				cellName = piece.substring(colonPos+1, secondColonPos);
                CellId cellId = libId.newCellId(CellName.parseName(cellName));
				String exportName = piece.substring(secondColonPos+1);
				commaPos = exportName.indexOf(',');
				if (commaPos >= 0) exportName = exportName.substring(0, commaPos);
                return cellId.newPortId(exportName);
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
				return idManager.newLibId(libName);
			case 'O':		// Tool
				String toolName = piece;
				commaPos = toolName.indexOf(',');
				if (commaPos >= 0) toolName = toolName.substring(0, commaPos);
				Tool tool = Tool.findTool(toolName);
				if (tool == null)
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Unknown Tool: " + piece, -1);
				return tool;
			case 'P':		// PrimitiveNodeId
				colonPos = piece.indexOf(':');
				if (colonPos < 0)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed PrimitiveNode (missing colon): " + piece, -1);
					break;
				}
				String techName = piece.substring(0, colonPos);
                TechId techId = idManager.newTechId(techName);
				String nodeName = piece.substring(colonPos+1);
				commaPos = nodeName.indexOf(',');
				if (commaPos >= 0) nodeName = nodeName.substring(0, commaPos);
				return techId.newPrimitiveNodeId(nodeName);
			case 'R':		// ArcProtoId
				colonPos = piece.indexOf(':');
				if (colonPos < 0)
				{
					Input.errorLogger.logError(fileName + ", line " + lineNumber +
						", Badly formed ArcProto (missing colon): " + piece, -1);
					break;
				}
				techName = piece.substring(0, colonPos);
                techId = idManager.newTechId(techName);
				String arcName = piece.substring(colonPos+1);
				commaPos = arcName.indexOf(',');
				if (commaPos >= 0) arcName = arcName.substring(0, commaPos);
				return techId.newArcProtoId(arcName);
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
			case 'T':		// TechId
				techName = piece;
				commaPos = techName.indexOf(',');
				if (commaPos >= 0) techName = techName.substring(0, commaPos);
                return idManager.newTechId(techName);
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
			case 'Y':		// Byte
				return new Byte((byte)TextUtils.atoi(piece));
		}
		return null;
	}

    Technology findTechnology(String techName) {
        Technology tech = Technology.findTechnology(techName);
        if (tech == null && techName.equals("tsmc90"))
            tech = Technology.findTechnology("cmos90");
        return tech;
            
    }
    
	PrimitiveNode findPrimitiveNode(Technology tech, String name)
	{
		PrimitiveNode pn = tech.findNodeProto(name);
		if (pn != null) return pn;
		return tech.convertOldNodeName(name);
	}

    protected FileType getPreferredFileType() { return FileType.JELIB; }
}
