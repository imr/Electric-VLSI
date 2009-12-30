/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JelibParser.java
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
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.id.ArcProtoId;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.ExportId;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.id.NodeProtoId;
import com.sun.electric.database.id.PortProtoId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.id.PrimitivePortId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.CodeExpression;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.ncc.basic.TransitiveRelation;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * This class reads files in new library file (.jelib) format.
 */
public class JelibParser
{
	class CellContents
	{
        final Version version;
		boolean filledIn;
		String fileName;
		int lineNumber;

        CellId cellId;
        CellName groupName;
        long creationDate;
        long revisionDate;
        TechId techId;
        boolean expanded;
        boolean allLocked;
        boolean instLocked;
        boolean cellLib;
        boolean techLib;
        Variable[] vars;

        List<NodeContents> nodes = new ArrayList<NodeContents>();
        List<ExportContents> exports = new ArrayList<ExportContents>();
        List<ArcContents> arcs = new ArrayList<ArcContents>();
		// map disk node names (duplicate node names written "sig"1 and "sig"2)
		HashMap<String,NodeContents> diskName = new HashMap<String,NodeContents>();

		CellContents(Version version)
		{
            this.version = version;
			filledIn = false;
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

        ImmutableNodeInst n;
        NodeInst ni;
    }

    static class ExportContents {
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

    static class ArcContents {
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

    // The parsing result
    Version version;
    Variable[] libVars;

    final LinkedHashMap<LibId,String> externalLibIds = new LinkedHashMap<LibId,String>();
	final LinkedHashMap<CellId,Rectangle2D> externalCells = new LinkedHashMap<CellId,Rectangle2D>();
	final LinkedHashMap<ExportId,EPoint> externalExports = new LinkedHashMap<ExportId,EPoint>();
    final LinkedHashMap<TechId,Variable[]> techIds = new LinkedHashMap<TechId,Variable[]>();
    final LinkedHashMap<PrimitiveNodeId,Variable[]> primitiveNodeIds = new LinkedHashMap<PrimitiveNodeId,Variable[]>();
    final LinkedHashMap<PrimitivePortId,Variable[]> primitivePortIds = new LinkedHashMap<PrimitivePortId,Variable[]>();
    final LinkedHashMap<ArcProtoId,Variable[]> arcProtoIds = new LinkedHashMap<ArcProtoId,Variable[]>();
    final LinkedHashMap<String,Variable[]> tools = new LinkedHashMap<String,Variable[]>();

 	final LinkedHashMap<CellId,CellContents> allCells = new LinkedHashMap<CellId,CellContents>();
//    final LinkedHashSet<String> delibCellFiles = new LinkedHashSet<String>();

    /*---------------------------------------------------------------------*/

	private static String[] revisions =
	{
		// Revision 1
		"8.01aw",
		// Revision 2
		"8.04l",
	};

    private static final Version newDelibHeaderVersion = Version.parseVersion("8.04n");

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

    // Were in LibraryFiles
    final URL fileURL;
    private final FileType fileType;
    private final IdManager idManager;
    private final LibId libId;
    private final String filePath;
    private LineNumberReader lineReader;
    private final LineNumberReader delibHeaderReader;
    private int revision = revisions.length;
    private final ErrorLogger errorLogger;
    private final MutableTextDescriptor mtd = new MutableTextDescriptor();
    /** buffer for reading Variables. */                                    private final ArrayList<Variable> variablesBuf = new ArrayList<Variable>();

    // collect the cells by common protoName and by "groupLines" relation
    private final HashMap<String,ArrayList<CellContents>> cellsWithProtoName = new HashMap<String,ArrayList<CellContents>>();
    private final TransitiveRelation<String> transitiveProtoNames = new TransitiveRelation<String>();

    private HashMap<String,TextDescriptorAndCode> parsedDescriptorsF = new HashMap<String,TextDescriptorAndCode>();
    private HashMap<String,TextDescriptorAndCode> parsedDescriptorsT = new HashMap<String,TextDescriptorAndCode>();
//	private Version version;
	private char escapeChar = '\\';
	private String curLibName;
    private String curReadFile;

    private static class TextDescriptorAndCode {
        private final TextDescriptor td;
        private final CodeExpression.Code code;
        TextDescriptorAndCode(TextDescriptor td, CodeExpression.Code code) {
            this.td = td;
            this.code = code;
        }
    }

    private LibId curExternalLibId = null;
    private CellId curExternalCellId = null;
    private TechId curTechId = null;
    private PrimitiveNodeId curPrimId = null;

	private JelibParser(LibId libId, URL fileURL, FileType fileType, boolean onlyProjectSettings, ErrorLogger errorLogger) throws IOException
	{
        idManager = libId.idManager;
        this.libId = libId;
        this.fileURL = fileURL;
        this.fileType = fileType;
        filePath = fileURL.getFile();
        this.errorLogger = errorLogger;

        InputStream inputStream;
        if (fileType == FileType.JELIB) {
            URLConnection urlCon = fileURL.openConnection();
            urlCon.setConnectTimeout(10000);
            urlCon.setReadTimeout(1000);
            curReadFile = filePath;
            inputStream = urlCon.getInputStream();
        } else if (fileType == FileType.DELIB) {
            curReadFile = filePath + File.separator + "header";
            try{
                inputStream = new FileInputStream(curReadFile);
            } catch (IOException e)
            {
                String message = "Header file " + curReadFile + " not found";
                System.out.println(message);
                throw new FileNotFoundException(message);
            }
        } else {
            throw new IllegalArgumentException("fileType");
        }
        InputStreamReader is = new InputStreamReader(inputStream, "UTF-8");
        this.lineReader = new LineNumberReader(is);
        delibHeaderReader = fileType == FileType.DELIB ? lineReader : null;
        try {
            readFromFile(onlyProjectSettings);
            collectCellGroups();
        } catch (Exception e) {
            logError("Exception " + e.getMessage());
        } finally {
            lineReader.close();
        }
	}

    private void collectCellGroups() {
        for (Iterator<Set<String>> git = transitiveProtoNames.getSetsOfRelatives(); git.hasNext(); ) {
            Set<String> protoNames = git.next();

            // Collect cells in this group
            ArrayList<CellContents> cellsInGroup = new ArrayList<CellContents>();
            for (String protoName: protoNames) {
                ArrayList<CellContents> list = cellsWithProtoName.get(protoName);
                if (list == null) {
                    logError("No cells for group name " + protoName);
                    continue;
                }
                cellsInGroup.addAll(list);
            }

            // Make cell group name
            ArrayList<CellName> cellNamesInGroup = new ArrayList<CellName>();
            for (CellContents cc: cellsInGroup)
                cellNamesInGroup.add(cc.cellId.cellName);
            CellName groupName = Snapshot.makeCellGroupName(cellNamesInGroup);

            // Set cell group name for each cell
            for (CellContents cc: cellsInGroup)
                cc.groupName = groupName;
        }
    }

    public static JelibParser parse(LibId libId, URL fileURL, FileType fileType, boolean onlyProjectSettings, ErrorLogger errorLogger) throws IOException {
        return new JelibParser(libId, fileURL, fileType, onlyProjectSettings, errorLogger);
    }

    private void readFromFile(boolean onlyProjectSettings) throws IOException {
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
                logError("CVS conflicts found: " + line);
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
                if (lineReader == delibHeaderReader) {
                    readDelibCell(line);
                } else {
                    readCell(line);
                }
                continue;
			}

			if (first == 'L')
			{
				// cross-library reference
				List<String> pieces = parseLine(line);
				if (pieces.size() != 2)
				{
					logError("External library declaration needs 2 fields: " + line);
					continue;
				}
                String libName = unQuote(pieces.get(0));
                curExternalLibId = idManager.newLibId(libName);
                String libFileName = unQuote(pieces.get(1));

				// recurse
                if (!externalLibIds.containsKey(curExternalLibId))
                    externalLibIds.put(curExternalLibId, libFileName);
				continue;
			}

			if (first == 'R') {
				// cross-library cell information
				List<String> pieces = parseLine(line);
 				int numPieces = revision == 1 ? 7 : 5;
 				if (pieces.size() != numPieces) {
					logError("External cell declaration needs " + numPieces + " fields: " + line);
					continue;
				}
				double lowX = readDouble(pieces.get(1));
				double highX = readDouble(pieces.get(2));
				double lowY = readDouble(pieces.get(3));
				double highY = readDouble(pieces.get(4));
				if (revision == 1)
				{
					Long.parseLong(pieces.get(5));		// ignore cdate
					Long.parseLong(pieces.get(6));		// ignore rdate
				}
				Rectangle2D bounds = new Rectangle2D.Double(lowX, lowY, highX-lowX, highY-lowY);
                String cellName = unQuote(pieces.get(0));
                curExternalCellId = curExternalLibId.newCellId(CellName.parseName(cellName));
                if (!externalCells.containsKey(curExternalCellId))
                    externalCells.put(curExternalCellId, bounds);
				continue;
			}

			if (first == 'F') {
				// cross-library export information
				List<String> pieces = parseLine(line);
				if (pieces.size() != 3) {
					logError("External export declaration needs 3 fields: " + line);
					continue;
				}
				String exportName = unQuote(pieces.get(0));
				double posX = readDouble(pieces.get(1));
				double posY = readDouble(pieces.get(1));
                ExportId exportId = curExternalCellId.newPortId(exportName);
				externalExports.put(exportId, EPoint.fromLambda(posX, posY));
				continue;
			}

			if (first == 'H') {
				// parse header
				List<String> pieces = parseLine(line);
				if (pieces.size() < 2) {
					logError("Library declaration needs 2 fields: " + line);
					continue;
				}
				version = Version.parseVersion(pieces.get(1));
				if (version == null) {
					logError("Badly formed version: " + pieces.get(1));
					continue;
				}
				for (revision = 0; revision < revisions.length; revision++) {
					if (version.compareTo(Version.parseVersion(revisions[revision])) < 0) break;
				}
				escapeChar = revision < 1 ? '^' : '\\';
				pieces = parseLine(line);
				curLibName = unQuote(pieces.get(0));
				if (version.compareTo(Version.getVersion()) > 0) {
					logWarning("Library " + curLibName + " comes from a NEWER version of Electric (" + version + ")");
				}
				libVars = readVariables(pieces, 2);
				continue;
			}

			if (first == 'O')
			{
				// parse Tool information
				List<String> pieces = parseLine(line);
				String toolName = unQuote(pieces.get(0));

				// get additional meaning preferences starting at position 1
                Variable[] vars = readVariables(pieces, 1);
                if (!tools.containsKey(toolName))
                    tools.put(toolName, vars);
				continue;
			}

			if (first == 'V') {
				// parse View information
				List<String> pieces = parseLine(line);
				String viewName = unQuote(pieces.get(0));
				View view = View.findView(viewName);
				if (view == null) {
					String viewAbbr = unQuote(pieces.get(1));
					view = View.newInstance(viewName, viewAbbr);
					if (view == null) {
						logError("Cannot create view " + viewName);
						continue;
					}
				}

				// get additional variables starting at position 2
                Variable[] vars = readVariables(pieces, 2);
				continue;
			}

			if (first == 'T') {
				// parse Technology information
				List<String> pieces = parseLine(line);
				String techName = unQuote(pieces.get(0));
                curTechId = idManager.newTechId(techName);
                curPrimId = null;

				// get additional meaning preferences  starting at position 1
                Variable[] vars = readVariables(pieces, 1);
                if (!techIds.containsKey(curTechId))
                    techIds.put(curTechId, vars);
				continue;
			}

			if (first == 'D') {
				// parse PrimitiveNode information
				List<String> pieces = parseLine(line);
				String primName = unQuote(pieces.get(0));
				if (curTechId == null) {
					logError("Primitive node " + primName + " has no technology before it");
					continue;
				}
                curPrimId = curTechId.newPrimitiveNodeId(primName);

				// get additional variables starting at position 1
                Variable[] vars = readVariables(pieces, 1);
                if (!primitiveNodeIds.containsKey(curPrimId))
                    primitiveNodeIds.put(curPrimId, vars);
				continue;
			}

			if (first == 'P') {
				// parse PrimitivePort information
				List<String> pieces = parseLine(line);
				String primPortName = unQuote(pieces.get(0));
				if (curPrimId == null) {
					logError("Primitive port " + primPortName + " has no primitive node before it");
					continue;
				}
                PrimitivePortId primitivePortId = curPrimId.newPortId(primPortName);

				// get additional variables starting at position 1
                Variable[] vars = readVariables(pieces, 1);
                if (!primitivePortIds.containsKey(primitivePortId))
                    primitivePortIds.put(primitivePortId, vars);
				continue;
			}

			if (first == 'W') {
				// parse ArcProto information
				List<String> pieces = parseLine(line);
				String arcName = unQuote(pieces.get(0));
				if (curTechId == null) {
					logError("Primitive arc " + arcName + " has no technology before it");
					continue;
				}
                ArcProtoId arcProtoId = curTechId.newArcProtoId(arcName);

				// get additional variables starting at position 1
                Variable[] vars = readVariables(pieces, 1);
                if (!arcProtoIds.containsKey(arcProtoId))
                    arcProtoIds.put(arcProtoId, vars);
				continue;
			}

			if (first == 'G')
			{
				// group information
				List<String> pieces = parseLine(line);
                String firstProtoName = null;
				for(int i=0; i<pieces.size(); i++)
				{
					String cellNameString = unQuote(pieces.get(i));
					if (cellNameString.length() == 0) continue;
					int colonPos = cellNameString.indexOf(':');
					if (colonPos >= 0) cellNameString = cellNameString.substring(colonPos+1);
                    CellName cellName = CellName.parseName(cellNameString);
					if (cellName == null) {
						logError("Bad cell name " + cellNameString);
						continue;
					}
                    if (cellsWithProtoName.get(cellName.getName()) == null) {
 						logError("Unknown cell " + cellName);
						continue;
                    }
                    String protoName = cellName.getName();
                    if (firstProtoName == null)
                        firstProtoName = protoName;
                    else
                        transitiveProtoNames.theseAreRelated(firstProtoName, protoName);
				}
				continue;
			}

			logError("Unrecognized line: " + line);
		}
	}

    private void readDelibCell(String line) throws IOException {
        // get the file location; remove 'C' at start
        String cellFile = line.substring(1, line.length());

        // New header file as of version 8.04n, no cell refs, searches delib dir for cell files
        if (version.compareTo(newDelibHeaderVersion) >= 0) {
            if (cellFile.equals(com.sun.electric.tool.io.output.DELIB.SEARCH_FOR_CELL_FILES)) {
                File delibDir = new File(filePath);
                if (delibDir.exists() && delibDir.isDirectory()) {
                    for (File file : delibDir.listFiles()) {
                        if (file.isDirectory()) continue;
                        String name = file.getName();
                        int dot = name.lastIndexOf('.');
                        if (dot < 0) continue;
                        View view = View.findView(name.substring(dot+1));
                        if (view == null) continue;
                        try {
                            readDelibFile(file);
                        } catch (Exception e) {
                            if (e instanceof IOException) throw (IOException)e;
                            // some other exception, probably invalid cell file
                            Input.errorLogger.logError("Exception reading file "+file, -1);
                        }
                    }
                }
            }
            return;
        }

        cellFile = cellFile.replace(com.sun.electric.tool.io.output.DELIB.PLATFORM_INDEPENDENT_FILE_SEPARATOR, File.separatorChar);
        cellFile = cellFile.replace(File.separatorChar, ':');
        File cellFD = new File(filePath, cellFile);
        readDelibFile(cellFD);
    }

    private void readDelibFile(File cellFD) throws IOException {

        LineNumberReader cellReader;
        try {
            FileInputStream fin = new FileInputStream(cellFD);
            InputStreamReader is = new InputStreamReader(fin);
            cellReader = new LineNumberReader(is);
        } catch (IOException e) {
            System.out.println("Error opening file "+cellFD+": "+e.getMessage());
            return;
        }
        Version savedVersion = version;
        int savedRevision = revision;
        char savedEscapeChar = escapeChar;
        String savedCurLibName = curLibName;
        lineReader = cellReader;
        curReadFile = cellFD.getAbsolutePath();
        try {
            readFromFile(false);
//            delibCellFiles.add(curReadFile);
        } finally {
            version = savedVersion;
            revision = savedRevision;
            escapeChar = savedEscapeChar;
            curLibName = savedCurLibName;
            lineReader.close();
            lineReader = delibHeaderReader;
            curReadFile = filePath;
        }
    }

    private void readCell(String line) throws IOException {
        // grab a cell description
        List<String> pieces = parseLine(line);
        int numPieces = revision >= 2 ? 6 : revision == 1 ? 5 : 7;
        if (pieces.size() < numPieces)
        {
            logError("Cell declaration needs " + numPieces + " fields: " + line);
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
        CellName cellName = CellName.parseName(name);
        CellContents cc = new CellContents(version);
        cc.fileName = curReadFile;
        cc.lineNumber = lineReader.getLineNumber() + 1;
        cc.cellId = libId.newCellId(cellName);
        String techName = unQuote(pieces.get(fieldIndex++));
        cc.techId = idManager.newTechId(techName);
        cc.creationDate = Long.parseLong(pieces.get(fieldIndex++));
        cc.revisionDate = Long.parseLong(pieces.get(fieldIndex++));

        // parse state information
        String stateInfo = pieces.get(fieldIndex++);
        for(int i=0; i<stateInfo.length(); i++)
        {
            switch (stateInfo.charAt(i)) {
                case 'E': cc.expanded = true; break;
                case 'L': cc.allLocked = true; break;
                case 'I': cc.instLocked = true; break;
                case 'C': cc.cellLib = true; break;
                case 'T': cc.techLib = true; break;
            }
        }

        // add variables
        assert fieldIndex == numPieces;
        cc.vars = readVariables(pieces, numPieces);

        // gather the contents of the cell
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
                    parseNode(nextLine, cc);
                    break;
                case 'E':
                    parseExport(nextLine, cc);
                    break;
                case 'A':
                    parseArc(nextLine, cc);
                    break;
                default:
            }
        }

        // check if the version is not null
        if (cc.version == null)
        {
            logError("Version for Cell '" + cc.cellId.cellName + "' is null");
            return;
        }

        // remember the contents of the cell for later
        if (allCells.containsKey(cc.cellId)) {
            logError("Duplicate cell " + cc.cellId);
            return;
        }
        String protoName = cellName.getName();
        if (groupName == null)
            groupName = protoName;
        transitiveProtoNames.theseAreRelated(protoName, groupName);
        allCells.put(cc.cellId, cc);
        ArrayList<CellContents> list = cellsWithProtoName.get(protoName);
        if (list == null) {
            list = new ArrayList<CellContents>();
            cellsWithProtoName.put(protoName, list);
        }
        list.add(cc);
        return;
    }

    private void parseNode(String cellString, CellContents cc) {
        NodeContents n = new NodeContents();
        n.line = lineReader.getLineNumber();

        // parse the node line
        List<String> pieces = parseLine(cellString);
        char firstChar = cellString.charAt(0);
        int numPieces = revision < 1 ? 10 : firstChar == 'N' ? 9 : 8;
        if (pieces.size() < numPieces)
        {
            logError("Node instance needs " + numPieces + " fields: " + cellString, cc.cellId);
            return;
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
        n.nodeName = nodeName;
        String nameTextDescriptorInfo = pieces.get(2);
        double x = readDouble(pieces.get(3));
        double y = readDouble(pieces.get(4));

        LibId libId = cc.cellId.libId;
        String prefixName = libId.libName;
        int colonPos = protoName.indexOf(':');
        if (colonPos < 0)
        {
            if (firstChar == 'I' || revision < 1)
                n.protoId = libId.newCellId(CellName.parseName(protoName));
            else
                n.protoId = cc.techId.newPrimitiveNodeId(protoName);
        } else
        {
            prefixName = protoName.substring(0, colonPos);
            protoName = protoName.substring(colonPos+1);
            if (firstChar == 'I' || revision < 1 && protoName.indexOf('{') >= 0) {
                if (!prefixName.equals(curLibName))
                    libId = idManager.newLibId(prefixName);
                n.protoId = libId.newCellId(CellName.parseName(protoName));
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
            double wid = readDouble(pieces.get(5));
            if (revision < 1 && (wid < 0 || wid == 0 && 1/wid < 0)) {
                flipX = true;
                wid = -wid;
            }
            double hei = readDouble(pieces.get(6));
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
                angle += Integer.valueOf(orientString.substring(i));
                break;
            }
        }

        // parse state information in stateInfo field
        TextDescriptorAndCode nameTdC = loadTextDescriptor(nameTextDescriptorInfo, false);
        n.nameTextDescriptor = nameTdC.td;
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
                            logError("(" + cc.cellId + ") bad node bits" + stateInfo, cc.cellId);
                        }
                        break parseStateInfo;
                    }
            }
        }
        n.flags = flags;
        n.techBits = techBits;
        TextDescriptorAndCode protoTdC = loadTextDescriptor(textDescriptorInfo, false);
        n.protoTextDescriptor = protoTdC.td;

        // create the node
        n.orient = Orientation.fromJava(angle, flipX, flipY);
        n.anchor = EPoint.fromLambda(x, y);

        // add variables in fields 10 and up
        n.vars = readVariables(pieces, numPieces);
        cc.nodes.add(n);
        // insert into map of disk names
        cc.diskName.put(diskNodeName, n);
    }

    private void parseExport(String cellString, CellContents cc) {
        ExportContents e = new ExportContents();
        e.line = lineReader.getLineNumber();

        // parse the export line
        List<String> pieces = parseLine(cellString);
        if (revision >= 2 && pieces.size() == 1) {
            // Unused ExportId
            String exportName = unQuote(pieces.get(0));
            cc.cellId.newPortId(exportName);
            return;
        }
        int numPieces = revision >= 2 ? 6 : revision == 1 ? 5 : 7;
        if (pieces.size() < numPieces)
        {
            logError("Export needs " + numPieces + " fields, has " + pieces.size() + ": " + cellString, cc.cellId);
            return;
        }
        int fieldIndex = 0;
        String exportName = unQuote(pieces.get(fieldIndex++));
        String exportUserName = null;
        if (revision >= 2) {
            String s = pieces.get(fieldIndex++);
            if (s.length() != 0)
                exportUserName = unQuote(s);
        }
        if (exportUserName == null || exportName.equals(exportUserName))
            exportName = Name.findName(exportName).toString(); // save memory using String from Name
        e.exportId = cc.cellId.newPortId(exportName);
        e.exportUserName = exportUserName;
        // get text descriptor in field 1
        String textDescriptorInfo = pieces.get(fieldIndex++);
        String nodeName = revision >= 1 ? pieces.get(fieldIndex++) : unQuote(pieces.get(fieldIndex++));
        e.originalNode = cc.diskName.get(nodeName);
        String portName = unQuote(pieces.get(fieldIndex++));
        e.originalPort = e.originalNode.protoId.newPortId(portName);
        Point2D pos = null;
        if (revision < 1)
        {
            double x = readDouble(pieces.get(fieldIndex++));
            double y = readDouble(pieces.get(fieldIndex++));
            pos = new Point2D.Double(x, y);
        }
        e.pos = pos;
        // parse state information in field 6
        String userBits = pieces.get(fieldIndex++);
        assert fieldIndex == numPieces;

        TextDescriptorAndCode nameTdC =  loadTextDescriptor(textDescriptorInfo, false);
        e.nameTextDescriptor = nameTdC.td;
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
        e.vars = readVariables(pieces, numPieces);
        cc.exports.add(e);
    }

    private void parseArc(String cellString, CellContents cc) {
        ArcContents a = new ArcContents();
        a.line = lineReader.getLineNumber();

        // parse the arc line
        List<String> pieces = parseLine(cellString);
        if (pieces.size() < 13)
        {
            logError("Arc instance needs 13 fields: " + cellString, cc.cellId);
            return;
        }
        TechId techId = cc.techId;
        String protoName = unQuote(pieces.get(0));
        int indexOfColon = protoName.indexOf(':');
        if (indexOfColon >= 0) {
            techId = idManager.newTechId(protoName.substring(0, indexOfColon));
            protoName = protoName.substring(indexOfColon + 1);
        }
        a.arcProtoId = techId.newArcProtoId(protoName);
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
        a.arcName = arcName;
        a.diskWidth = readDouble(pieces.get(3));

        String headNodeName = revision >= 1 ? pieces.get(5) : unQuote(pieces.get(5));
        String headPortName = unQuote(pieces.get(6));
        double headX = readDouble(pieces.get(7));
        double headY = readDouble(pieces.get(8));
        a.headNode = cc.diskName.get(headNodeName);
        a.headPort = a.headNode.protoId.newPortId(headPortName);
        a.headPoint = EPoint.fromLambda(headX, headY);

        String tailNodeName = revision >= 1 ? pieces.get(9) : unQuote(pieces.get(9));
        String tailPortName = unQuote(pieces.get(10));
        double tailX = readDouble(pieces.get(11));
        double tailY = readDouble(pieces.get(12));
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
                    if ('0' <= chr && chr <= '9')
                    {
                        angle = Integer.valueOf(stateInfo.substring(i));
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
        TextDescriptorAndCode nameTdC = loadTextDescriptor(nameTextDescriptorInfo, false);
        a.nameTextDescriptor = nameTdC.td;

        // add variables in fields 13 and up
        a.vars = readVariables(pieces, 13);
        cc.arcs.add(a);
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
     * @return an array of Variables.
	 */
	private Variable[] readVariables(List<String> pieces, int position)
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
				logError("Badly formed variable (no open parenthesis): " + piece);
				continue;
			}
			String varName = unQuote(piece.substring(0, openPos));
			Variable.Key varKey = Variable.newKey(varName);
			int closePos = piece.indexOf(')', openPos);
			if (closePos < 0)
			{
				logError("Badly formed variable (no close parenthesis): " + piece);
				continue;
			}
			String varBits = piece.substring(openPos+1, closePos);
			int objectPos = closePos + 1;
			if (objectPos >= piece.length())
			{
				logError("Variable type missing: " + piece);
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
					logError("Variable type invalid: " + piece);
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
						Object oneObj = getVariableValue(piece.substring(start, objectPos), varType);
						objList.add(oneObj);
						if (piece.charAt(objectPos) == ']') break;
						objectPos++;
					}
					if (objectPos >= piece.length())
					{
						logError("Badly formed array (no closed bracket): " + piece);
						continue;
					}
					else if (objectPos < piece.length() - 1)
					{
						logError("Badly formed array (extra characters after closed bracket): " + piece);
						continue;
					}
					int limit = objList.size();
					Object [] objArray;
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
                        default: throw new AssertionError();
					}
					for(int j=0; j<limit; j++)
						objArray[j] = objList.get(j);
					obj = objArray;
				} else
				{
					// a scalar Variable
					obj = getVariableValue(piece.substring(objectPos), varType);
                    if (obj == null) {
                        // ????
                        continue;
                    }
				}
			}

			// create the variable
            TextDescriptorAndCode tdc = loadTextDescriptor(varBits, true);
            obj = Variable.withCode(obj, tdc.code);
            Variable d = Variable.newInstance(varKey, obj, tdc.td);
            variablesBuf.add(d);
		}
        return variablesBuf.toArray(Variable.NULL_ARRAY);
	}

	/**
	 * Method to load a TextDescriptor from a String description of it.
	 * @param varBits the String that describes the TextDescriptor.
     * @param onVar true if this TextDescriptor resides on a Variable
	 * It may be false if the TextDescriptor is on a NodeInst or Export.
	 * @return loaded TextDescriptor
	 */
	private TextDescriptorAndCode loadTextDescriptor(String varBits, boolean onVar)
	{
        HashMap<String,TextDescriptorAndCode> parsedDescriptors = onVar ? parsedDescriptorsT : parsedDescriptorsF;
        TextDescriptorAndCode tdc = parsedDescriptors.get(varBits);
        if (tdc != null) return tdc;

        boolean error = false;
        mtd.setCBits(0, 0, 0);
        CodeExpression.Code code = CodeExpression.Code.NONE;
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
						logError("Incorrect display specification: " + varBits);
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
						logError("Bad absolute size (semicolon missing): " + varBits);
                        error = true;
						break;
					}
					mtd.setAbsSize(Integer.valueOf(varBits.substring(j+1, semiPos)));
					j = semiPos;
					break;
				case 'G':		// relative text size
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						logError("Bad relative size (semicolon missing): " + varBits);
                        error = true;
						break;
					}
					mtd.setRelSize(readDouble(varBits.substring(j+1, semiPos)));
					j = semiPos;
					break;
				case 'X':		// X offset
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						logError("Bad X offset (semicolon missing): " + varBits);
                        error = true;
						break;
					}
					xoff = readDouble(varBits.substring(j+1, semiPos));
					j = semiPos;
					break;
				case 'Y':		// Y offset
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						logError("Bad Y offset (semicolon missing): " + varBits);
                        error = true;
						break;
					}
					yoff = readDouble(varBits.substring(j+1, semiPos));
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
						logError("Bad font (semicolon missing): " + varBits);
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
						logError("Bad color (semicolon missing): " + varBits);
                        error = true;
						break;
					}
					mtd.setColorIndex(Integer.valueOf(varBits.substring(j+1, semiPos)));
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
						logError("Bad language specification: " + varBits);
                        error = true;
						break;
					}
					char codeLetter = varBits.charAt(j);
					if (!onVar)
					{
						logError("Illegal use of language specification: " + varBits);
						error = true;
						break;
					}
                    switch (codeLetter) {
                        case 'J': code = CodeExpression.Code.JAVA; break;
                        case 'L': code = CodeExpression.Code.SPICE; break;
                        case 'T': code = CodeExpression.Code.TCL; break;
                        default:
                            logError("Unknown language specification: " + varBits);
                            error = true;
                    }
					break;
				case 'U':		// units
					j++;
					if (j >= varBits.length())
					{
						logError("Bad units specification: " + varBits);
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
						logError("Unknown units specification: " + varBits);
                        error = true;
					}
					break;
			}
		}
		mtd.setOff(xoff, yoff);
		TextDescriptor td = TextDescriptor.newTextDescriptor(mtd);
        tdc = new TextDescriptorAndCode(td, code);
        if (!error) parsedDescriptors.put(varBits, tdc);
        return tdc;
	}

	/**
	 * Method to convert a String to an Object so that it can be stored in a Variable.
	 * @param piece the String to be converted.
	 * @param varType the type of the object to convert (a letter from the file).
	 * @return the Object representation of the given String.
	 */
	private Object getVariableValue(String piece, char varType)
	{
		int colonPos;
		String libName;
		int secondColonPos;
		String cellName;
		int commaPos;

        if (piece.length() == 0)
            return null;
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
					logError("Badly formed Cell (missing colon): " + piece);
					break;
				}
				libName = piece.substring(0, colonPos);
                LibId libId = idManager.newLibId(libName);
				cellName = piece.substring(colonPos+1);
				commaPos = cellName.indexOf(',');
				if (commaPos >= 0) cellName = cellName.substring(0, commaPos);
                return libId.newCellId(CellName.parseName(cellName));
			case 'D':		// Double
				return Double.valueOf(piece);
			case 'E':		// Export (should delay analysis until database is built!!!)
				colonPos = piece.indexOf(':');
				if (colonPos < 0)
				{
					logError("Badly formed Export (missing library colon): " + piece);
					break;
				}
				libName = piece.substring(0, colonPos);
                libId = idManager.newLibId(libName);
				secondColonPos = piece.indexOf(':', colonPos+1);
				if (secondColonPos < 0)
				{
					logError("Badly formed Export (missing cell colon): " + piece);
					break;
				}
				cellName = piece.substring(colonPos+1, secondColonPos);
                CellId cellId = libId.newCellId(CellName.parseName(cellName));
				String exportName = piece.substring(secondColonPos+1);
				commaPos = exportName.indexOf(',');
				if (commaPos >= 0) exportName = exportName.substring(0, commaPos);
                return cellId.newPortId(exportName);
			case 'F':		// Float
				return Float.valueOf(piece);
			case 'G':		// Long
				return Long.valueOf(piece);
			case 'H':		// Short
				return Short.valueOf(piece);
			case 'I':		// Integer
				return Integer.valueOf(piece);
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
					logError("Unknown Tool: " + piece);
				return tool;
			case 'P':		// PrimitiveNodeId
				colonPos = piece.indexOf(':');
				if (colonPos < 0)
				{
					logError("Badly formed PrimitiveNode (missing colon): " + piece);
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
					logError("Badly formed ArcProto (missing colon): " + piece);
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
					logError("Badly formed string variable (missing open quote): " + piece);
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
				int slashPos = piece.indexOf('/');
				if (slashPos < 0)
				{
					logError("Badly formed Point2D variable (missing slash): " + piece);
					break;
				}
				double x = Double.valueOf(piece.substring(0, slashPos));
				double y = Double.valueOf(piece.substring(slashPos+1));
				return new EPoint(x, y);
			case 'Y':		// Byte
				return Byte.valueOf(piece);
		}
		return null;
	}

    private static double readDouble(String s) {
        return s.length() > 0 ? Double.parseDouble(s) : 0;
    }

    private void logError(String message) {
        String s = curReadFile + ", line " + lineReader.getLineNumber() + ", " + message;
        errorLogger.logError(s, -1);
    }

    private void logWarning(String message) {
        String s = curReadFile + ", line " + lineReader.getLineNumber() + ", " + message;
        errorLogger.logWarning(s, null, -1);
    }

    private void logError(String message, CellId cellId) {
        String s = curReadFile + ", line " + lineReader.getLineNumber() + ", " + message;
        errorLogger.logError(s, cellId, -1);
    }
}
