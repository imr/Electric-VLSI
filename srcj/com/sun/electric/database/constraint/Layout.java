/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Layout.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.constraint;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellId;
import com.sun.electric.database.ExportId;
import com.sun.electric.database.IdMapper;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableCell;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.ImmutableLibrary;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.LibId;
import com.sun.electric.database.LibraryBackup;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.drc.DRC;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Class to implement the layout-constraint system.
 * Handles the fixed-angle and rigid constraints.
 * Also propagates these constraints up the hierarchy.
 */
public class Layout extends Constraints
{
	private static final Layout layoutConstraint = new Layout();

	static final boolean DEBUG = false;

    static boolean doChangesQuietly;
    static Snapshot oldSnapshot;
    static long revisionDate;
    static String userName;
    static Set<Cell> goodDRCCells;
    static Variable goodDRCDate, goodDRCBit;

    /** Shadow Cell info */
    private static final ArrayList<LayoutCell> cellInfos = new ArrayList<LayoutCell>();
    /** Map which contains temporary rigidity of ArcInsts. */ 
    private static final HashMap<ArcInst,Boolean> tempRigid = new HashMap<ArcInst,Boolean>();
    /** Saved libraries */
    static final HashSet<LibId> librariesWritten = new HashSet<LibId>();
    
	Layout() {}

//	/**
//	 * Method to return the current constraint solver.
//	 * @return the current constraint solver.
//	 */
//	public static Layout getConstraint() { return layoutConstraint; }

	/**
	 * Method to set the subsequent changes to be "quiet".
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
     * This method is used to suppress endBatch.
	 */
	public static void changesQuiet(boolean quiet) {
        doChangesQuietly = true;
    }
    
	/**
	 * Method to start a batch of changes.
     * @param initialSnapshot snapshot before job changes.
	 */
	public void startBatch(Snapshot initialSnapshot)
	{
		// force every cell to remember its current bounds
        doChangesQuietly = false;
        oldSnapshot = initialSnapshot;
        tempRigid.clear();
        librariesWritten.clear();
        goodDRCCells = null;
        makeLayoutCells();
	}

	/**
	 * Method to do hierarchical update on any cells that changed
	 */
	public void endBatch(String userName)
	{
        if (DEBUG) {
            System.out.println("Temporary rigid:");
            for (Map.Entry<ArcInst,Boolean> e : tempRigid.entrySet()) {
                System.out.println("\t" + e.getKey() + " --> " + e.getValue());
            }
        }
        Layout.userName = userName;
        revisionDate = System.currentTimeMillis();
        if (goodDRCCells != null) {
            TextDescriptor td = TextDescriptor.getCellTextDescriptor().withDisplay(false);
            goodDRCDate = Variable.newInstance(DRC.DRC_LAST_GOOD_DATE, new Long(revisionDate + 1), td); // If cell is changed during this 1 millisecond ???
        }
        if (!doChangesQuietly) {
            // Propagate changes and mark changed cells.
            for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
                Library lib = it.next();
                for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); ) {
                    Cell cell = cIt.next();
                    if (!cell.isLinked()) continue;
                    LayoutCell cellInfo = getCellInfo(cell);
                    cellInfo.compute();
                }
            }
            // Mark justSaved libraries as not chanted.
            for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
                Library lib = it.next();
                if (librariesWritten.contains(lib.getId()))
                    lib.clearChanged();
                else {
                    LibraryBackup libBackup = oldSnapshot.getLib(lib.getId());
                    if (lib.backup() != libBackup)
                        lib.setChanged();
                }
            }
            // Mark libraries with killed cells as changed
            for (CellBackup cellBackup: oldSnapshot.cellBackups) {
                if (cellBackup != null && EDatabase.serverDatabase().getCell(cellBackup.d.cellId) == null) {
                    Library lib = EDatabase.serverDatabase().getLib(cellBackup.d.libId);
                    if (lib != null)
                        lib.setChanged();
                }
            }
        }

        cellInfos.clear();
        tempRigid.clear();
        librariesWritten.clear();
        Snapshot newSnapshot = EDatabase.serverDatabase().backup();
        goodDRCCells = null;
        oldSnapshot = null;
	}

	/**
	 * Method to handle a change to a NodeInst.
	 * @param ni the NodeInst that was changed.
	 * @param oD the old contents of the NodeInst.
	 */
	public void modifyNodeInst(NodeInst ni, ImmutableNodeInst oD) {
        if (doChangesQuietly) return;
        getCellInfo(ni.getParent()).modifyNodeInst(ni, oD);
    }

	/**
	 * Method to handle a change to an ArcInst.
	 * @param ai the ArcInst that changed.
     * @param oD the old contents of the ArcInst.
	 */
	public void modifyArcInst(ArcInst ai, ImmutableArcInst oD) {
        if (doChangesQuietly) return;
        getCellInfo(ai.getParent()).modifyArcInst(ai, oD);
    }

	/**
	 * Method to handle a change to an Export.
	 * @param pp the Export that moved.
	 * @param oldD the old contents of the Export.
	 */
	public void modifyExport(Export pp, ImmutableExport oldD) {
        if (doChangesQuietly) return;
        PortInst oldPi = ((Cell)pp.getParent()).getPortInst(oldD.originalNodeId, oldD.originalPortId);
        if (oldPi == pp.getOriginalPort()) return;
        getCellInfo((Cell)pp.getParent()).modifyExport(pp, oldPi);
    }
    
	/**
	 * Method to handle a change to a Cell.
	 * @param cell the Cell that was changed.
	 * @param oD the old contents of the Cell.
	 */
	public void modifyCell(Cell cell, ImmutableCell oD) {}

	/**
	 * Method to handle a change to a Library.
	 * @param lib the Library that was changed.
	 * @param oldD the old contents of the Library.
	 */
	public void modifyLibrary(Library lib, ImmutableLibrary oldD) {}

	/**
	 * Method to handle the creation of a new ElectricObject.
	 * @param obj the ElectricObject that was just created.
	 */
	public void newObject(ElectricObject obj) {
        if (doChangesQuietly) return;
        Cell cell = obj.whichCell();
        if (obj == cell)
            newCellInfo(cell, null);
        else if (cell != null)
            getCellInfo(cell).newObject(obj);
	}

	/**
	 * Method to announce that a Library is about to be written to disk.
	 * @param lib the Library that will be saved.
	 */
	public void writeLibrary(Library lib) { librariesWritten.add(lib.getId()); }
    
    /**
     * Method to announce than Ids were renamed.
     * @param idMapper mapper from old Ids to new Ids.
     */
    public void renameIds(IdMapper idMapper) {
        EDatabase database = EDatabase.serverDatabase();
        for (CellId cellId: idMapper.getNewCellIds())
            newObject(cellId.inDatabase(database));
        for (ExportId exportId: idMapper.getNewExportIds())
            newObject(exportId.inDatabase(database));
    }
    
	/**
	 * Method to set temporary rigidity on an ArcInst.
	 * @param ai the ArcInst to make temporarily rigid/not-rigid.
	 * @param tempRigid true to make the ArcInst temporarily rigid;
	 * false to make it temporarily not-rigid.
	 */
	public static void setTempRigid(ArcInst ai, boolean tempRigid)
	{
        if (DEBUG) System.out.println("setTempRigid " + ai + " " + tempRigid);
        ai.checkChanging();
        Layout.tempRigid.put(ai, Boolean.valueOf(tempRigid));
//		if (tempRigid)
//		{
//			if (ai.getChangeClock() == changeClock + 2) return;
//			ai.setChangeClock(changeClock + 2);
//		} else
//		{
//			if (ai.getChangeClock() == changeClock + 3) return;
//			ai.setChangeClock(changeClock + 3);
//		}
	}

	/**
	 * Method to remove temporary rigidity on an ArcInst.
	 * @param ai the ArcInst to remove temporarily rigidity.
	 */
	public static void removeTempRigid(ArcInst ai)
	{
        ai.checkChanging();
        tempRigid.remove(ai);
//		if (ai.getChangeClock() != changeClock + 3 && ai.getChangeClock() != changeClock + 2) return;
//		ai.setChangeClock(changeClock - 3);
	}

    /*
     * Method to request to set 
     */
    public static void setGoodDRCCells(Set<Cell> goodDRCCells, int activeBits, boolean inMemory)
    {
        assert(!inMemory); // call only if you are storing in disk
        Layout.goodDRCCells = goodDRCCells;
        TextDescriptor td = TextDescriptor.getCellTextDescriptor().withDisplay(false);
        goodDRCBit = Variable.newInstance(DRC.DRC_LAST_GOOD_BIT, new Integer(activeBits), td);
    }
    
    /**
     ** Returns rigidity of an ArcInst considering temporary rigidity.
     * @param ai ArcInst to test rigidity.
     * @return true if the ArcInst is considered rigid in this batch.
     */
    static boolean isRigid(ArcInst ai) {
        Boolean override = tempRigid.get(ai);
        return override != null ? override.booleanValue() : ai.isRigid();
    }
    
    private static void makeLayoutCells() {
        cellInfos.clear();
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = it.next();
			for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = cIt.next();
                newCellInfo(cell, oldSnapshot.getCell((CellId)cell.getId()));
			}
		}
    }
    
	/******************** NODE MODIFICATION CODE *************************/

	/**
	 * Method to compute the position of portinst "pi" and
	 * place the center of the area in the parameters "x" and "y".  The position
	 * is the "old" position, as determined by any changes that may have occured
	 * to the nodeinst (and any sub-nodes).
	 */
	static Poly oldPortPosition(PortInst pi)
	{
        NodeInst ni = pi.getNodeInst();
        PortProto pp = pi.getPortProto();
		// descend to the primitive node
		AffineTransform subrot = makeOldRot(ni);
        if (subrot == null) return null;
		NodeInst bottomNi = ni;
		PortProto bottomPP = pp;
		while (bottomNi.isCellInstance())
		{
			AffineTransform localtran = makeOldTrans(bottomNi);
			subrot.concatenate(localtran);

            PortInst bottomPi = getOldOriginalPort((Export)bottomPP);
			bottomNi = bottomPi.getNodeInst();
			bottomPP = bottomPi.getPortProto();
			localtran = makeOldRot(bottomNi);
			subrot.concatenate(localtran);
		}

		// if the node hasn't changed, use its current values
        ImmutableNodeInst d = Layout.getOldD(bottomNi);
        assert d != null;
        if (d != bottomNi.getD()) {
			// create a fake node with these values
            bottomNi = NodeInst.makeDummyInstance(bottomNi.getProto());
            bottomNi.lowLevelModify(d);
		}
		PrimitiveNode np = (PrimitiveNode)bottomNi.getProto();
		Technology tech = np.getTechnology();
		Poly poly = tech.getShapeOfPort(bottomNi, (PrimitivePort)bottomPP);
		poly.transform(subrot);
		return (poly);
	}

	private static AffineTransform makeOldRot(NodeInst ni)
	{
		// if the node has not been modified, just use the current transformation
        ImmutableNodeInst d = getOldD(ni);
        if (d == null) return null;
        
		// get the old values
        double cX = d.anchor.getX();
        double cY = d.anchor.getY();
        return d.orient.rotateAbout(cX, cY);
	}

	private static AffineTransform makeOldTrans(NodeInst ni)
	{
        ImmutableNodeInst d = getOldD(ni);
        if (d == null) return null;

		// create the former translation matrix
		AffineTransform transform = new AffineTransform();
        double cX = d.anchor.getX();
        double cY = d.anchor.getY();
		transform.translate(cX, cY);
		return transform;
	}

    static PortInst getOldOriginalPort(Export e) {
        return getCellInfo((Cell)e.getParent()).getOldOriginalPort(e);
    }
    
    static ImmutableNodeInst getOldD(NodeInst ni) {
        return getCellInfo(ni.getParent()).getOldD(ni);
    }
    
    private static void newCellInfo(Cell cell, CellBackup oldBackup) {
        int cellIndex = cell.getCellIndex();
        while (cellInfos.size() <= cellIndex) cellInfos.add(null);
        assert cellInfos.get(cellIndex) == null;
        cellInfos.set(cellIndex, new LayoutCell(cell, oldBackup));
    }
    
    static LayoutCell getCellInfo(Cell cell) {
        return cellInfos.get(cell.getCellIndex());
    }
    
}
