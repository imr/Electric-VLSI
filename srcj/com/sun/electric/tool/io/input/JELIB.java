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

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.ArcProtoId;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.id.NodeProtoId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.IconParameters;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * This class reads files in new library file (.jelib) format.
 */
public class JELIB extends LibraryFiles
{
    private final FileType fileType;
    private final JelibParser parser;
    private final IconParameters iconParameters;

    JELIB(LibId libId, URL fileURL, FileType type, IconParameters iconParams) throws IOException
	{
        fileType = type;
        parser = JelibParser.parse(libId, fileURL, fileType, false, Input.errorLogger);
        iconParameters = iconParams;
    }

    public static Map<Setting,Object> readProjectSettings(URL fileURL, FileType fileType, TechPool techPool, ErrorLogger errorLogger) {
        JelibParser parser;
        try {
            parser = parse(techPool.idManager, fileURL, fileType, true, errorLogger);
        } catch (IOException e) {
            errorLogger.logError("Error reading " + fileURL + ": " + e.getMessage(), -1);
            return null;
        }
        HashMap<Setting,Object> projectSettings = new HashMap<Setting,Object>();
        for (Map.Entry<TechId,Variable[]> e: parser.techIds.entrySet()) {
            TechId techId = e.getKey();
            Variable[] vars = e.getValue();
            Technology tech = techPool.getTech(techId);
            if (tech == null && techId.techName.equals("tsmc90"))
                tech = techPool.getTech(techPool.idManager.newTechId("cmos90"));
            if (tech == null) {
                Input.errorLogger.logError(fileURL +
                    ", Cannot identify technology " + techId.techName, -1);
                continue;
            }
            realizeMeaningPrefs(projectSettings, tech, vars);
        }
        for (Map.Entry<String,Variable[]> e: parser.tools.entrySet()) {
            String toolName = e.getKey();
            Variable[] vars = e.getValue();
			Tool tool = Tool.findTool(toolName);
			if (tool == null) {
				Input.errorLogger.logError(fileURL +
                    ", Cannot identify tool " + toolName, -1);
				continue;
			}
            realizeMeaningPrefs(projectSettings, tool, vars);
        }
        return projectSettings;
    }

    public static JelibParser parse(IdManager idManager, URL fileURL, FileType fileType, boolean onlyProjectSettings, ErrorLogger errorLogger) throws IOException {
        LibId libId = idManager.newLibId(TextUtils.getFileNameWithoutExtension(fileURL));
        return JelibParser.parse(libId, fileURL, fileType, onlyProjectSettings, errorLogger);
    }

	/**
	 * Method to read a Library in new library file (.jelib) format.
	 * @return true on error.
	 */
    @Override
    boolean readTheLibrary(boolean onlyProjectSettings, LibraryStatistics.FileContents fc) {
        return false;
    }

    @Override
    Map<Cell,Variable[]> createLibraryCells(boolean onlyProjectSettings) {
        lib.erase();
        realizeVariables(lib, parser.libVars);
        lib.setVersion(parser.version);

        // Project preferences
        for (Map.Entry<TechId,Variable[]> e: parser.techIds.entrySet()) {
            TechId techId = e.getKey();
            Variable[] vars = e.getValue();
            Technology tech = findTechnology(techId);
            if (tech == null) {
                Input.errorLogger.logError(filePath +
                    ", Cannot identify technology " + techId.techName, -1);
                continue;
            }
            realizeMeaningPrefs(tech, vars);
        }
        for (Map.Entry<String,Variable[]> e: parser.tools.entrySet()) {
            String toolName = e.getKey();
            Variable[] vars = e.getValue();
			Tool tool = Tool.findTool(toolName);
			if (tool == null) {
				Input.errorLogger.logError(parser.fileURL +
                    ", Cannot identify tool " + toolName, -1);
				continue;
			}
            realizeMeaningPrefs(tool, vars);
        }
        for (Map.Entry<LibId,String> e: parser.externalLibIds.entrySet()) {
            LibId libId = e.getKey();
            String libFileName = e.getValue();
            if (Library.findLibrary(libId.libName) == null)
                readExternalLibraryFromFilename(libFileName, fileType, iconParameters);
        }

        nodeProtoCount = parser.allCells.size();
        nodeProtoList = new Cell[nodeProtoCount];
        cellLambda = new double[nodeProtoCount];
        HashMap<Cell,Variable[]> originalVars = new HashMap<Cell,Variable[]>();
        HashMap<CellName,Cell> cellGroupExamples = new HashMap<CellName,Cell>();
        int cellNum = 0;
        for (JelibParser.CellContents cc: parser.allCells.values()) {
            CellId cellId = cc.cellId;
            Cell cell = Cell.newInstance(lib, cellId.cellName.toString());
            Technology tech = findTechnology(cc.techId);
            cell.setTechnology(tech);
            cell.lowLevelSetCreationDate(new Date(cc.creationDate));
            cell.lowLevelSetRevisionDate(new Date(cc.revisionDate));
            if (cc.expanded) cell.setWantExpanded();
            if (cc.allLocked) cell.setAllLocked();
            if (cc.instLocked) cell.setInstancesLocked();
            if (cc.cellLib) cell.setInCellLibrary();
            if (cc.techLib) cell.setInTechnologyLibrary();
            originalVars.put(cell, cc.vars);
            nodeProtoList[cellNum++] = cell;
            Cell otherCell = cellGroupExamples.get(cc.groupName);
            if (otherCell == null)
                cellGroupExamples.put(cc.groupName, cell);
            else
                cell.joinGroup(otherCell);
        }

        lib.setFromDisk();
        lib.setDelibCells();
//        lib.setDelibCellFiles(parser.delibCellFiles);
        return originalVars;
	}

    @Override
    Variable[] findVarsOnExampleIcon(Cell parentCell, Cell iconCell) {
        JelibParser.CellContents cc = parser.allCells.get(parentCell.getId());
        if (cc == null) return null;
        for (JelibParser.NodeContents nc: cc.nodes) {
            if (nc.protoId == iconCell.getId())
                return nc.vars;
        }
        return null;
    }

	/**
	 * Method to recursively create the contents of each cell in the library.
	 */
    @Override
	protected void realizeCellsRecursively(Cell cell, HashSet<Cell> recursiveSetupFlag, String scaledCellName, double scale)
	{
		if (scaledCellName != null) return;
		JelibParser.CellContents cc = parser.allCells.get(cell.getId());
		if (cc == null || cc.filledIn) return;
		instantiateCellContent(cell, cc, recursiveSetupFlag);
		cellsConstructed++;
        setProgressValue(cellsConstructed * 100 / totalCells);
		recursiveSetupFlag.add(cell);
//        cell.loadExpandStatus();
	}

	/**
	 * Method called after all libraries have been read to instantiate a single Cell.
	 * @param cell the Cell to instantiate.
	 * @param cc the contents of that cell (the strings from the file).
	 */
	private void instantiateCellContent(Cell cell, JelibParser.CellContents cc, HashSet<Cell> recursiveSetupFlag)
	{
        HashMap<Technology,Technology.SizeCorrector> sizeCorrectors = new HashMap<Technology,Technology.SizeCorrector>();

//        boolean immutableInstantiateOK = true;
//        for (int nodeId = 0; nodeId < cc.nodes.size(); nodeId++) {
//            JelibParser.NodeContents n = cc.nodes.get(nodeId);
//            try {
//                n.n = ImmutableNodeInst.newInstance(nodeId, n.protoId, Name.findName(n.nodeName), n.nameTextDescriptor,
//                        n.orient, n.anchor, n.size, n.flags, n.techBits, n.protoTextDescriptor);
//            } catch (Exception e) {
//                immutableInstantiateOK = false;
//                System.out.println("Exception in immutable instantiate " + cell);
//                break;
//            }
//        }
        
		// place all nodes
        for (JelibParser.NodeContents n: cc.nodes) {
            int line = n.line;

			String prefixName = lib.getName();
			NodeProto np = null;
            NodeProtoId protoId = n.protoId;
            if (protoId instanceof CellId)
                np = database.getCell((CellId)protoId);
            else {
                PrimitiveNodeId pnId = (PrimitiveNodeId)protoId;
                np = findPrimitiveNode(pnId);
                if (np == null) {
					Input.errorLogger.logError(cc.fileName + ", line " + line +
						", Cannot find primitive node " + pnId, cell, -1);
                    Set<PrimitiveNodeId> primSet = undefinedTechsAndPrimitives.get(pnId.techId);
                    if (primSet == null)
                    {
                        primSet = new HashSet<PrimitiveNodeId>();
                        undefinedTechsAndPrimitives.put(pnId.techId, primSet);
                    }
                    primSet.add(pnId);
                    CellName cellName = CellName.parseName(pnId.name);
                    if (cellName.getVersion() <= 0)
                        cellName = CellName.newName(cellName.getName(), cellName.getView(), 1);
                    protoId = lib.getId().newCellId(cellName);
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

            EPoint size = n.size;
            if (np instanceof PrimitiveNode) {
                PrimitiveNode pn = (PrimitiveNode)np;
                Technology tech = pn.getTechnology();
                Technology.SizeCorrector sizeCorrector = sizeCorrectors.get(tech);
                if (sizeCorrector == null) {
                    sizeCorrector = tech.getSizeCorrector(cc.version, projectSettings, true, false);
                    sizeCorrectors.put(tech, sizeCorrector);
                }
                size = sizeCorrector.getSizeFromDisk(pn, size.getLambdaX(), size.getLambdaY());
            }

			if (np == null)
			{
                CellId dummyCellId = (CellId)protoId;
                String protoName = dummyCellId.cellName.toString();
                Library cellLib = database.getLib(dummyCellId.libId);
				if (cellLib == null)
				{
					Input.errorLogger.logError(cc.fileName + ", line " + line +
						", Creating dummy library " + prefixName, cell, -1);
					cellLib = Library.newInstance(prefixName, null);
				}
                Cell dummyCell = cellLib.findNodeProto(protoName);
                if (dummyCell != null && dummyCell.getVar(IO_DUMMY_OBJECT) == null)
                    dummyCell = null;
                if (dummyCell == null) {
                    dummyCell = Cell.makeInstance(cellLib, protoName);
                    if (dummyCell == null)
                    {
                        Input.errorLogger.logError(cc.fileName + ", line " + line +
                            ", Unable to create dummy cell " + protoName + " in " + cellLib, cell, -1);
                        continue;
                    }
                    Input.errorLogger.logError(cc.fileName + ", line " + line +
                        ", Creating dummy cell " + protoName + " in " + cellLib, cell, -1);
                }
				Rectangle2D bounds = parser.externalCells.get(dummyCellId.toString());
				if (bounds == null)
				{
					Input.errorLogger.logError(cc.fileName + ", line " + line +
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
//            if (immutableInstantiateOK && !ni.getD().equalsExceptVariables(n.n)) {
//                System.out.println("Difference between immutable and mutable nodes in " + cell);
//                immutableInstantiateOK = false;
//            }

			// add variables
            realizeVariables(ni, n.vars);

			// insert into map of disk names
            n.ni = ni;

		}

		// place all exports
        for (JelibParser.ExportContents e: cc.exports) {
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
		for(JelibParser.ArcContents a: cc.arcs) {
            int line = a.line;

            ArcProto ap = findArcProto(a.arcProtoId);
			if (ap == null)
            {
                if (ap == null)
                    ap = cell.getTechnology().convertOldArcName(a.arcProtoId.name);
                if (ap == null)
                {
                    Input.errorLogger.logError(cc.fileName + ", line " + line +
					" (" + cell + ") cannot find arc " + a.arcProtoId, cell, -1);
				    continue;
                }
			}
            Technology tech = ap.getTechnology();
            Technology.SizeCorrector sizeCorrector = sizeCorrectors.get(tech);
            if (sizeCorrector == null) {
                 sizeCorrector = tech.getSizeCorrector(cc.version, projectSettings, true, false);
                sizeCorrectors.put(tech, sizeCorrector);
            }
			long gridExtendOverMin = sizeCorrector.getExtendFromDisk(ap, a.diskWidth);

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
				Input.errorLogger.logMessage(cc.fileName + ", line " + line +
					" (" + cell + ") cannot create arc " + a.arcProtoId, geomList, cell, 2, true);
				continue;
			}
            realizeVariables(ai, a.vars);
		}
		cc.filledIn = true;
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
	private PortInst figureOutPortInst(Cell cell, String portName, JelibParser.NodeContents n, Point2D pos,
                                       String fileName, int lineNumber)
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
                pos = parser.externalExports.get(subCell.getId().newPortId(portName));
		}
        if (pos == null)
            pos = ni.getAnchorCenter();
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
        Point2D.Double tpos = new Point2D.Double();
        ni.transformIn().transform(pos, tpos);
		NodeInst portNI = NodeInst.newInstance(Generic.tech().universalPinNode, tpos, 0, 0, subCell);
		if (portNI == null)
		{
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				", Unable to create export " + name + " on dummy " + subCell + " (cannot create source node)", cell, -1);
			return null;
		}
		PortInst portPI = portNI.getOnlyPortInst();
		Export export = Export.newInstance(subCell, portPI, name, null, false, iconParameters);
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

    Technology findTechnology(TechId techId) {
        TechPool techPool = database.getTechPool();
        Technology tech = techPool.getTech(techId);
        if (tech == null && techId.techName.equals("tsmc90"))
            tech = techPool.getTech(idManager.newTechId("cmos90"));
        return tech;
    }

	PrimitiveNode findPrimitiveNode(PrimitiveNodeId primitiveNodeId) {
        TechPool techPool = database.getTechPool();
        PrimitiveNode pn = techPool.getPrimitiveNode(primitiveNodeId);
        if (pn == null && primitiveNodeId.techId.techName.equals("tsmc90"))
            pn = findPrimitiveNode(idManager.newTechId("cmos90").newPrimitiveNodeId(primitiveNodeId.name));
		if (pn == null) {
            Technology tech = findTechnology(primitiveNodeId.techId);
            if (tech != null)
                pn = tech.convertOldNodeName(primitiveNodeId.name);
        }
        return pn;
	}

    ArcProto findArcProto(ArcProtoId arcProtoId) {
        TechPool techPool = database.getTechPool();
        ArcProto ap = techPool.getArcProto(arcProtoId);
        if (ap == null && arcProtoId.techId.techName.equals("tsmc90"))
            ap = findArcProto(idManager.newTechId("cmos90").newArcProtoId(arcProtoId.name));
        return ap;
    }
}
