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
package com.sun.electric.database;

import static com.sun.electric.database.UsageCollector.EMPTY_BITSET;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.prototype.NodeProtoId;
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.technology.AbstractShapeBuilder;
import com.sun.electric.technology.BoundsBuilder;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.Job;

import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Iterator;

/**
 *
 */
public class CellBackup {
    public static final CellBackup[] NULL_ARRAY = {};
    public static final ImmutableArrayList<CellBackup> EMPTY_LIST = new ImmutableArrayList<CellBackup>(NULL_ARRAY);

    static int cellBackupsCreated = 0;
    static int cellBackupsMemoized = 0;
    /** Cell data. */                                               public final CellRevision cellRevision;           
    /** "Modified" flag of the Cell. */                             public final boolean modified;

    /** Memoized data for size computation (connectivity etc). */   private volatile Memoization m;
    /** Arc shrinkage data */                                       private AbstractShapeBuilder.Shrinkage shrinkage;
    /** Bounds of primitive arcs in this Cell. */                   private ERectangle primitiveBounds;
    
    /** Creates a new instance of CellBackup */
    private CellBackup(CellRevision cellRevision, boolean modified) {
        this.cellRevision = cellRevision;
        this.modified = modified;
        cellBackupsCreated++;
//        if (Job.getDebug())
//            check();
    }
    
    /** Creates a new instance of CellBackup */
    public CellBackup(ImmutableCell d) {
        this(new CellRevision(d), false);
    }
    
    /**
     * Creates a new instance of CellBackup which differs from this CellBackup.
     * Four array parameters are supplied. Each parameter may be null if its contents is the same as in this Snapshot.
     * @param d new persistent data of a cell.
     * @param revisionDate new revision date.
     * @param modified new modified flag
     * @param nodesArray new array of nodes
     * @param arcsArray new array of arcs
     * @param exportsArray new array of exports
     * @return new snapshot which differs froms this Snapshot or this Snapshot.
     * @throws IllegalArgumentException on invariant violation.
     * @throws ArrayOutOfBoundsException on some invariant violations.
     */
    public CellBackup with(ImmutableCell d, long revisionDate, boolean modified,
            ImmutableNodeInst[] nodesArray, ImmutableArcInst[] arcsArray, ImmutableExport[] exportsArray) {
        CellRevision newRevision = cellRevision.with(d, revisionDate, nodesArray, arcsArray, exportsArray);
        if (newRevision == cellRevision) return this;
        return new CellBackup(newRevision, modified);
    }
    
	/**
	 * Returns CellBackup which differs from this CellBackup by renamed Ids.
	 * @param idMapper a map from old Ids to new Ids.
     * @return CellBackup with renamed Ids.
	 */
    CellBackup withRenamedIds(IdMapper idMapper, CellName newGroupName) {
        CellRevision newRevision = cellRevision.withRenamedIds(idMapper, newGroupName);
        if (newRevision == cellRevision) return this;
        return new CellBackup(newRevision, true);
    }
    
    /**
     * Writes this CellBackup to SnapshotWriter.
     * @param writer where to write.
     */
    void write(SnapshotWriter writer) throws IOException {
        cellRevision.write(writer);
        writer.writeBoolean(modified);
    }
    
    /**
     * Reads CellBackup from SnapshotReader.
     * @param reader where to read.
     */
    static CellBackup read(SnapshotReader reader) throws IOException {
        CellRevision newRevision = CellRevision.read(reader);
        boolean modified = reader.readBoolean();
        return new CellBackup(newRevision, modified);
    }

    /**
	 * Checks invariant of this CellBackup.
	 * @throws AssertionError if invariant is broken.
	 */
    public void check() {
        cellRevision.check();
        
        if (m != null)
            m.check();
    }
    
    @Override
    public String toString() { return cellRevision.toString(); }

    /**
     * Returns data for size computation (connectivity etc).
     * @return data for size computation.
     */
    public Memoization getMemoization() {
        Memoization m = this.m;
        if (m != null) return m;
        return this.m = new Memoization();
    }
    
    /**
     * Returns data for arc shrinkage computation.
     * @return data for arc shrinkage computation.
     */
    public AbstractShapeBuilder.Shrinkage getShrinkage() {
        if (shrinkage == null)
            shrinkage = new AbstractShapeBuilder.Shrinkage(this);
        return shrinkage;
    }
    
    /**
     * Returns bounds of all primitive arcs in this Cell or null if there are not primitives.
     * @return bounds of all primitive arcs or null.
     */
    public ERectangle getPrimitiveBounds() {
        ERectangle primitiveBounds = this.primitiveBounds;
        if (primitiveBounds != null) return primitiveBounds;
        return this.primitiveBounds = computePrimitiveBounds();
    }
    
    public ERectangle computePrimitiveBounds() {
        ImmutableArrayList<ImmutableArcInst> arcs = cellRevision.arcs;
        if (arcs.isEmpty()) return null;
        int intMinX = Integer.MAX_VALUE, intMinY = Integer.MAX_VALUE, intMaxX = Integer.MIN_VALUE, intMaxY = Integer.MIN_VALUE;
        int[] intCoords = new int[4];
        CellBackup.Memoization m = getMemoization();
        AbstractShapeBuilder.Shrinkage shrinkage = getShrinkage();
        BoundsBuilder boundsBuilder = new BoundsBuilder(m, shrinkage);
        for (ImmutableArcInst a: arcs) {
            if (a.genBoundsEasy(m, shrinkage, intCoords)) {
                int x1 = intCoords[0];
                if (x1 < intMinX) intMinX = x1;
                int y1 = intCoords[1];
                if (y1 < intMinY) intMinY = y1;
                int x2 = intCoords[2];
                if (x2 > intMaxX) intMaxX = x2;
                int y2 = intCoords[3];
                if (y2 > intMaxY) intMaxY = y2;
                continue;
            }
            boundsBuilder.genShapeOfArc(a);
        }
        ERectangle bounds = boundsBuilder.makeBounds();
        if (bounds == null) {
            assert intMinX <= intMaxX && intMinY <= intMaxY;
            int iw = intMaxX - intMinX;
            int ih = intMaxY - intMinY;
            return ERectangle.fromGrid(intMinX, intMinY,
                    iw >= 0 ? iw : (long)intMaxX - (long)intMinX,
                    ih >= 0 ? ih : (long)intMaxY - (long)intMinY);
        }
        if (intMinX > intMaxX)
            return bounds;
        long longMinX = Math.min(bounds.getGridMinX(), intMinX);
        long longMinY = Math.min(bounds.getGridMinY(), intMinY);
        long longMaxX = Math.max(bounds.getGridMaxX(), intMaxX);
        long longMaxY = Math.max(bounds.getGridMaxY(), intMaxY);
        return ERectangle.fromGrid(longMinX, longMinY, longMaxX - longMinX, longMaxY - longMinY);
    }
    
    /**
     * Class which memoizes data for size computation (connectivity etc).
     */
    public class Memoization {
//        /**
//         * ImmutableNodeInsts accessed by their nodeId.
//         */
//        private final int[] nodesById;
        public final int[] connections;
        /** ImmutableExports sorted by original PortInst. */
        private final ImmutableExport[] exportIndexByOriginalPort;
        private final BitSet wiped = new BitSet();
        private final BitSet hardArcs = new BitSet();
        
        Memoization() {
//            long startTime = System.currentTimeMillis();
            cellBackupsMemoized++;
            ImmutableArrayList<ImmutableNodeInst> nodes = cellRevision.nodes;
            ImmutableArrayList<ImmutableArcInst> arcs = cellRevision.arcs;
            int maxNodeId = -1;
            for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++)
                maxNodeId = Math.max(maxNodeId, nodes.get(nodeIndex).nodeId);
//            int[] nodesById = new int[maxNodeId + 1];
//            for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
//                ImmutableNodeInst n = nodes.get(nodeIndex);
//                nodesById[n.nodeId] = nodeIndex;
//            }
//            this.nodesById = nodesById;

            // Put connections into buckets by nodeId.
            int[] connections = new int[arcs.size()*2];
            int[] connectionsByNodeId = new int[maxNodeId+1];
            for (ImmutableArcInst a: arcs) {
                connectionsByNodeId[a.headNodeId]++;
                connectionsByNodeId[a.tailNodeId]++;
            }
            int sum = 0;
            for (int nodeId = 0; nodeId < connectionsByNodeId.length; nodeId++) {
                int start = sum;
                sum += connectionsByNodeId[nodeId];
                connectionsByNodeId[nodeId] = start;
            }
            for (int i = 0; i < arcs.size(); i++) {
                ImmutableArcInst a = arcs.get(i);
                connections[connectionsByNodeId[a.tailNodeId]++] = i*2;
                connections[connectionsByNodeId[a.headNodeId]++] = i*2 + 1;
            }
            
            // Sort each bucket by portId.
            sum = 0;
            for (int nodeId = 0; nodeId < connectionsByNodeId.length; nodeId++) {
                int start = sum;
                sum = connectionsByNodeId[nodeId];
                if (sum - 1 > start)
                    sortConnections(connections, start, sum - 1);
            }
            this.connections = connections;
            
            ImmutableExport[] exportIndexByOriginalPort = cellRevision.exports.toArray(new ImmutableExport[cellRevision.exports.size()]);
            Arrays.sort(exportIndexByOriginalPort, BY_ORIGINAL_PORT);
            this.exportIndexByOriginalPort = exportIndexByOriginalPort;
            
            for (ImmutableArcInst a: arcs) {
                // wipe status
                if (a.protoType.isWipable()) {
                    wiped.set(a.tailNodeId);
                    wiped.set(a.headNodeId);
                }
                // hard arcs
                if (!a.isEasyShape(a.protoType))
                    hardArcs.set(a.arcId);
            }
            for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
                ImmutableNodeInst n = nodes.get(nodeIndex);
                NodeProtoId np = n.protoId;
                if (!(np instanceof PrimitiveNode && ((PrimitiveNode)np).isArcsWipe()))
                    wiped.clear(n.nodeId);
            }
//            long stopTime = System.currentTimeMillis();
//            System.out.println("Memoization " + cellRevision.d.cellId + " took " + (stopTime - startTime) + " msec");
        }
        
       /**
         * Returns true of there are Exports on specified NodeInst.
         * @param originalNodeId nodeId of specified NodeInst.
         * @return true if there are Exports on specified NodeInst.
         */
        public boolean hasExports(int originalNodeId) {
            int startIndex = searchExportByOriginalPort(originalNodeId, 0);
            if (startIndex >= exportIndexByOriginalPort.length) return false;
            ImmutableExport e = exportIndexByOriginalPort[startIndex];
            return e.originalNodeId == originalNodeId;
        }
        
        /**
         * Method to return the number of Exports on specified NodeInst.
         * @param originalNodeId nodeId of specified NodeInst.
         * @return the number of Exports on specified NodeInst.
         */
        public int getNumExports(int originalNodeId) {
            int startIndex = searchExportByOriginalPort(originalNodeId, 0);
            int j = startIndex;
            for (; j < exportIndexByOriginalPort.length; j++) {
                ImmutableExport e = exportIndexByOriginalPort[j];
                if (e.originalNodeId != originalNodeId) break;
            }
            return j - startIndex;
        }
        
        /**
         * Method to return an Iterator over all ImmutableExports on specified NodeInst.
         * @param originalNodeId nodeId of specified NodeInst.
         * @return an Iterator over all ImmutableExports on specified NodeInst.
         */
        public Iterator<ImmutableExport> getExports(int originalNodeId) {
            int startIndex = searchExportByOriginalPort(originalNodeId, 0);
            int j = startIndex;
            for (; j < exportIndexByOriginalPort.length; j++) {
                ImmutableExport e = exportIndexByOriginalPort[j];
                if (e.originalNodeId != originalNodeId) break;
            }
            return ArrayIterator.iterator(exportIndexByOriginalPort, startIndex, j);
        }
        
        private int searchExportByOriginalPort(int originalNodeId, int originalChronIndex) {
            int low = 0;
            int high = exportIndexByOriginalPort.length-1;
            while (low <= high) {
                int mid = (low + high) >> 1; // try in a middle
                ImmutableExport e = exportIndexByOriginalPort[mid];
                int cmp = e.originalNodeId - originalNodeId;
                if (cmp == 0)
                    cmp = e.originalPortId.getChronIndex() >= originalChronIndex ? 1 : -1;
                
                if (cmp < 0)
                    low = mid + 1;
                else
                    high = mid - 1;
            }
            return low;
        }
        
        public int searchConnectionByPort(int nodeId, int chronIndex) {
            int low = 0;
            int high = connections.length-1;
            while (low <= high) {
                int mid = (low + high) >> 1; // try in a middle
                int con = connections[mid];
                ImmutableArcInst a = cellRevision.arcs.get(con >>> 1);
                boolean end = (con & 1) != 0;
                int endNodeId = end ? a.headNodeId : a.tailNodeId;
                int cmp = endNodeId - nodeId;
                if (cmp == 0) {
                    PortProtoId portId = end ? a.headPortId : a.tailPortId;
                    cmp = portId.getChronIndex() - chronIndex;
                }
                
                if (cmp < 0)
                    low = mid + 1;
                else
                    high = mid - 1;
            }
            return low;
        }
        
        public ImmutableArrayList<ImmutableArcInst> getArcs() { return cellRevision.arcs; }
        
        /**
         * Method to tell whether the specified ImmutableNodeInst is wiped.
         * Wiped ImmutableNodeInsts are erased.  Typically, pin ImmutableNodeInsts can be wiped.
         * This means that when an arc connects to the pin, it is no longer drawn.
         * In order for a ImmutableNodeInst to be wiped, its prototype must have the "setArcsWipe" state,
         * and the arcs connected to it must have "setWipable" in their prototype.
         * @param nodeId nodeId of specified ImmutableNodeInst
         * @return true if specified ImmutableNodeInst is wiped.
         */
        public boolean isWiped(int nodeId) {
            return wiped.get(nodeId);
        }
        
        public boolean isHardArc(int arcId) {
            return hardArcs.get(arcId);
        }
        
        /**
         * Checks invariant of this CellBackup.
         * @throws AssertionError if invariant is broken.
         */
        private void check() {
            assert exportIndexByOriginalPort.length == cellRevision.exports.size();
            ImmutableExport prevE = null;
            for (ImmutableExport e: exportIndexByOriginalPort) {
                if (prevE != null)
                    assert BY_ORIGINAL_PORT.compare(prevE, e) < 0;
                assert e == cellRevision.getExport(e.exportId);
                prevE = e;
            }
            assert connections.length == cellRevision.arcs.size()*2;
            for (int i = 1; i < connections.length; i++)
                assert compareConnections(connections[i - 1], connections[i]) < 0;
        }
        
        private void sortConnections(int[] connections, int l, int r) {
            ImmutableArrayList<ImmutableArcInst> arcs = cellRevision.arcs;
            while (l + 1 < r) {
                int x = connections[(l + r) >>> 1];
                ImmutableArcInst ax = arcs.get(x >>> 1);
                boolean endx = (x & 1) != 0;
                PortProtoId portIdX = endx ? ax.headPortId : ax.tailPortId;
                int chronIndexX = portIdX.getChronIndex(); 
                int i = l, j = r;
                do {
                    // while (compareConnections(connections[i], x) < 0) i++;
                    for (;;) {
                        int con = connections[i];
                        ImmutableArcInst a = arcs.get(con >>> 1);
                        boolean end = (con & 1) != 0;
                        PortProtoId portId = end ? a.headPortId : a.tailPortId;
                        if (portId.getChronIndex() > chronIndexX) break;
                        if (portId.getChronIndex() == chronIndexX && con >= x) break;
                        i++;
                    }
                    
                    // while (compareConnections(x, connections[j]) < 0) j--;
                    for (;;) {
                        int con = connections[j];
                        ImmutableArcInst a = arcs.get(con >>> 1);
                        boolean end = (con & 1) != 0;
                        PortProtoId portId = end ? a.headPortId : a.tailPortId;
                        if (chronIndexX > portId.getChronIndex()) break;
                        if (chronIndexX == portId.getChronIndex() && x >= con) break;
                        j--;
                    }
                    
                    if (i <= j) {
                        int w = connections[i];
                        connections[i] = connections[j];
                        connections[j] = w;
                        i++; j--;
                    }
                } while (i <= j);
                if (j - l < r - i) {
                    sortConnections(connections, l, j);
                    l = i;
                } else {
                    sortConnections(connections, i, r);
                    r = j;
                }
            }
            if (l + 1 == r) {
                int con1 = connections[l];
                int con2 = connections[r];
                ImmutableArcInst a1 = arcs.get(con1 >>> 1);
                ImmutableArcInst a2 = arcs.get(con2 >>> 1);
                boolean end1 = (con1 & 1) != 0;
                boolean end2 = (con2 & 1) != 0;
                PortProtoId portId1 = end1 ? a1.headPortId : a1.tailPortId;
                PortProtoId portId2 = end2 ? a2.headPortId : a2.tailPortId;
                int cmp = portId1.getChronIndex() - portId2.getChronIndex();
                if (cmp > 0 || cmp == 0 && con1 > con2) {
                    connections[l] = con2;
                    connections[r] = con1;
                }
            }
        }
        
        private int compareConnections(int con1, int con2) {
            ImmutableArcInst a1 = cellRevision.arcs.get(con1 >>> 1);
            ImmutableArcInst a2 = cellRevision.arcs.get(con2 >>> 1);
            boolean end1 = (con1 & 1) != 0;
            boolean end2 = (con2 & 1) != 0;
            int nodeId1 = end1 ? a1.headNodeId : a1.tailNodeId;
            int nodeId2 = end2 ? a2.headNodeId : a2.tailNodeId;
            int cmp = nodeId1 - nodeId2;
            if (cmp != 0) return cmp;
            PortProtoId portId1 = end1 ? a1.headPortId : a1.tailPortId;
            PortProtoId portId2 = end2 ? a2.headPortId : a2.tailPortId;
            cmp = portId1.getChronIndex() - portId2.getChronIndex();
            if (cmp != 0) return cmp;
            return con1 - con2;
        }
    }
    
    private static final Comparator<ImmutableExport> BY_ORIGINAL_PORT = new Comparator<ImmutableExport>() {
        public int compare(ImmutableExport e1, ImmutableExport e2) {
            int result = e1.originalNodeId - e2.originalNodeId;
            if (result != 0) return result;
            result = e1.originalPortId.getChronIndex() - e2.originalPortId.getChronIndex();
            if (result != 0) return result;
            return e1.exportId.chronIndex - e2.exportId.chronIndex;
        }
    };
 }
