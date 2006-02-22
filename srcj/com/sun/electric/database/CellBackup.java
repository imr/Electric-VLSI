/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellBackup.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.database;
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.database.text.TextUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
/**
 *
 */
public class CellBackup {
    public static final CellBackup[] NULL_ARRAY = {};
    
    /** Cell persistent data. */                                    public final ImmutableCell d;
	/** The date this ImmutableCell was last modified. */           public final long revisionDate;
    /** "Modified" flag of the Cell. */                             public final byte modified;
	/** This Cell is mainSchematics in its group. */				public final boolean isMainSchematics;
    /** An array of Exports on the Cell by chronological index. */  public final ImmutableExport[] exports;
	/** A list of NodeInsts in this Cell. */						public final ImmutableNodeInst[] nodes;
    /** Counts of NodeInsts for each CellUsage. */                  public final int[] cellUsages;
    /** Bitmap of used exports for each CellUsage. */               public final BitSet[] exportUsages;
    /** A list of ArcInsts in this Cell. */							public final ImmutableArcInst[] arcs;

    /** Creates a new instance of CellBackup */
    public CellBackup(ImmutableCell d, long revisionDate, byte modified, boolean isMainSchematics,
            ImmutableNodeInst[] nodes, ImmutableArcInst[] arcs, ImmutableExport[] exports, int[] cellUsages, BitSet[] exportUsages) {
        this.d = d;
        this.revisionDate = revisionDate;
        this.modified = modified;
        this.isMainSchematics = isMainSchematics;
        this.nodes = nodes;
        this.arcs = arcs;
        this.exports = exports;
        this.cellUsages = cellUsages;
        this.exportUsages = exportUsages;
        check();
    }
    
    /**
     * Creates a new instance of CellBackup which differs from given by persistent data.
     * @param cellBackup given CellBackup.
     * @param d new persistent data.
     */
    public CellBackup(CellBackup cellBackup, ImmutableCell d, long revisionDate) {
        this.d = d;
        this.revisionDate = revisionDate;
        this.modified = 1;
        isMainSchematics = cellBackup.isMainSchematics;
        nodes = cellBackup.nodes;
        arcs = cellBackup.arcs;
        exports = cellBackup.exports;
        cellUsages = cellBackup.cellUsages;
        exportUsages = cellBackup.exportUsages;
    }
    
    /**
     * Returns ImmutableNodeInst by its node id.
     * @param nodeId id of node.
     * @return ImmutableNodeInst with this id or null if node doesn't exist.
     */
    public ImmutableNodeInst getNode(int nodeId) { return nodeId < nodes.length ? nodes[nodeId] : null; }
    
    /**
     * Returns ImmutableArcInst by its arc id.
     * @param arcId id of node.
     * @return ImmutableArcInst with this id or null if node doesn't exist.
     */
    public ImmutableArcInst getArc(int arcId) { return arcId < arcs.length ? arcs[arcId] : null; }
    
    /**
     * Writes this CellBackup to SnapshotWriter.
     * @param writer where to write.
     */
    void write(SnapshotWriter writer) throws IOException {
        d.write(writer);
        writer.out.writeLong(revisionDate);
        writer.out.writeByte(modified);
        writer.out.writeBoolean(isMainSchematics);
        writer.out.writeInt(cellUsages.length);
        writer.out.writeInt(exportUsages.length);
        writer.out.writeInt(nodes.length);
        int maxNodeId = -1;
        for (int i = 0; i < nodes.length; i++)
            maxNodeId = Math.max(maxNodeId, nodes[i].nodeId);
        writer.out.writeInt(maxNodeId);
        for (int i = 0; i < nodes.length; i++)
            nodes[i].write(writer);
        writer.out.writeInt(arcs.length);
        for (int i = 0; i < arcs.length; i++)
            arcs[i].write(writer);
        writer.out.writeInt(exports.length);
        for (int i = 0; i < exports.length; i++)
            exports[i].write(writer);
    }
    
    /**
     * Reads CellBackup from SnapshotReader.
     * @param reader where to read.
     */
    static CellBackup read(SnapshotReader reader) throws IOException {
        ImmutableCell d = ImmutableCell.read(reader);
        long revisionDate = reader.in.readLong();
        byte modified = reader.in.readByte();
        boolean isMainSchematics = reader.in.readBoolean();
        int cellUsagesLen = reader.in.readInt();
        int[] cellUsages = new int[cellUsagesLen];
        int exportUsagesLen = reader.in.readInt();
        BitSet[] exportUsages = new BitSet[exportUsagesLen];
        int nodesLength = reader.in.readInt();
        ImmutableNodeInst[] nodes = new ImmutableNodeInst[nodesLength];
        int maxNodeId = reader.in.readInt();
        ImmutableNodeInst[] nodesById = new ImmutableNodeInst[maxNodeId + 1];
        for (int i = 0; i < nodesLength; i++) {
            ImmutableNodeInst nid = ImmutableNodeInst.read(reader);
            assert nodesById[nid.nodeId] == null;
            nodesById[nid.nodeId] = nid;
            if (nid.protoId instanceof CellId) {
                CellId subCellId = (CellId)nid.protoId;
                CellUsage u = d.cellId.getUsageIn(subCellId);
                cellUsages[u.indexInParent]++;
                if (nid.ports != null) {
                    for (int j = 0; j < nid.ports.length; j++) {
                        ImmutablePortInst pid = nid.ports[j];
                        if (pid == ImmutablePortInst.EMPTY) continue;
                        registerPortInstUsage(d.cellId, nid, u.protoId.getExportId(j), exportUsages);
                    }
                }
            }
            nodes[i] = nid;
        }
        int arcsLength = reader.in.readInt();
        ImmutableArcInst[] arcs = new ImmutableArcInst[arcsLength];
        for (int i = 0; i < arcsLength; i++) {
            ImmutableArcInst aid = ImmutableArcInst.read(reader);
            registerPortInstUsage(d.cellId, nodesById[aid.tailNodeId], aid.tailPortId, exportUsages);
            registerPortInstUsage(d.cellId, nodesById[aid.headNodeId], aid.headPortId, exportUsages);
            arcs[i] = aid;
        }
        int exportsLength = reader.in.readInt();
        ImmutableExport[] exports = new ImmutableExport[exportsLength];
        for (int i = 0; i < exportsLength; i++) {
            ImmutableExport eid = ImmutableExport.read(reader);
            registerPortInstUsage(d.cellId, nodesById[eid.originalNodeId], eid.originalPortId, exportUsages);
            exports[i] = eid;
        }
        return new CellBackup(d, revisionDate, modified, isMainSchematics, nodes, arcs, exports, cellUsages, exportUsages);
    }
    
    /**
     * Low-level method to register PortInst in ExportUsage arrays.
     * @param parentId cell parent of PortInst.
     * @param nodeId node id of PortInst.
     * @param portId port id of PortInst.
     * @exportUsages array for registration.
     */
    public static void registerPortInstUsage(CellId parentId, ImmutableNodeInst nodeId, PortProtoId portId, BitSet[] exportUsages) {
        if (!(nodeId.protoId instanceof CellId)) return;
        CellUsage u = parentId.getUsageIn((CellId)nodeId.protoId);
        BitSet exportUsage = exportUsages[u.indexInParent];
        if (exportUsage == null) {
            exportUsage = new BitSet();
            exportUsages[u.indexInParent] = exportUsage;
        }
        exportUsage.set(portId.getChronIndex());
    }
    
    /**
	 * Checks invariant of this CellBackup.
	 * @throws AssertionError if invariant is broken.
	 */
    public void check() {
        d.check();
        assert d.cellName.getVersion() > 0;
        assert d.tech != null;
        assert -1 <= modified && modified <= 1;
        CellId cellId = d.cellId;
        int[] checkCellUsages = checkCellUsages = (int[])cellUsages.clone();
        ArrayList<ImmutableNodeInst> nodesById = new ArrayList<ImmutableNodeInst>();
        ImmutableNodeInst prevN = null;
        for (int i = 0; i < nodes.length; i++) {
            ImmutableNodeInst n = nodes[i];
            n.check();
            while (n.nodeId >= nodesById.size()) nodesById.add(null);
            ImmutableNodeInst oldNode = nodesById.set(n.nodeId, n);
            assert oldNode == null;
			if (prevN != null) {
				int cmp = TextUtils.STRING_NUMBER_ORDER.compare(prevN.name.toString(), n.name.toString());
				assert cmp <= 0;
				if (cmp == 0)
					assert prevN.nodeId < n.nodeId;
			}
            prevN = n;
            if (n.protoId instanceof CellId) {
                CellId subCellId = (CellId)n.protoId;
                CellUsage u = cellId.getUsageIn(subCellId);
                checkCellUsages[u.indexInParent]--;
                for (int j = 0; j < n.ports.length; j++) {
                    ImmutablePortInst pid = n.ports[j];
                    if (pid == ImmutablePortInst.EMPTY) continue;
                    checkPortInst(n, subCellId.getExportId(j));
                }
            }
        }
        for (int i = 0; i < checkCellUsages.length; i++)
            assert checkCellUsages[i] == 0;
        BitSet arcIds = new BitSet();
        ImmutableArcInst prevA = null;
        for (int i = 0; i < arcs.length; i++) {
            ImmutableArcInst a = arcs[i];
            assert !arcIds.get(a.arcId);
            arcIds.set(a.arcId);
			if (prevA != null) {
				int cmp = TextUtils.STRING_NUMBER_ORDER.compare(prevA.name.toString(), a.name.toString());
				assert cmp <= 0;
				if (cmp == 0)
					assert prevA.arcId < a.arcId;
			}
            prevA = a;
            
            a.check();
            checkPortInst(nodesById.get(a.tailNodeId), a.tailPortId);
            checkPortInst(nodesById.get(a.headNodeId), a.headPortId);
        }
        BitSet exportChronIndicies = new BitSet();
        for (int i = 0; i < exports.length; i++) {
            ImmutableExport e = exports[i];
            e.check();
            assert e.exportId.parentId == cellId;
            assert !exportChronIndicies.get(e.exportId.chronIndex);
            exportChronIndicies.set(e.exportId.chronIndex);
            if (i > 0)
                assert(TextUtils.STRING_NUMBER_ORDER.compare(exports[i - 1].name.toString(), e.name.toString()) < 0) : i;
            checkPortInst(nodesById.get(e.originalNodeId), e.originalPortId);
        }
    }
    
    private void checkPortInst(ImmutableNodeInst node, PortProtoId portId) {
        assert node != null;
        assert portId.getParentId() == node.protoId;
        if (portId instanceof ExportId) {
            CellId protoId = (CellId)node.protoId;
            CellUsage u = d.cellId.getUsageIn(protoId);
            assert exportUsages[u.indexInParent].get(portId.getChronIndex());
        }
    }
    
    public boolean sameExports(CellBackup thatBackup) {
        if (exports.length != thatBackup.exports.length)
            return false;
        for (int i = 0; i < exports.length; i++) {
            if (exports[i].exportId != thatBackup.exports[i].exportId)
                return false;
        }
        return true;
    }
}
