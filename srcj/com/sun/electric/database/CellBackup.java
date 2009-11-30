/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellBackup.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
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

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.database.id.NodeProtoId;
import com.sun.electric.database.id.PortProtoId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.AbstractShapeBuilder;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.BoundsBuilder;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * CellBackup is a pair of CellRevision and TechPool.
 * It caches data that can be calculated when Technology is already
 * known, but subcells are unknown.
 */
public class CellBackup {

    private static final int BIN_SORT_THRESHOLD = 32;

    public static final CellBackup[] NULL_ARRAY = {};
    public static final ImmutableArrayList<CellBackup> EMPTY_LIST = new ImmutableArrayList<CellBackup>(NULL_ARRAY);
    static int cellBackupsCreated = 0;
    static int cellBackupsMemoized = 0;
    /** Cell data. */
    public final CellRevision cellRevision;
    /** Technologies mapping */
    public final TechPool techPool;
    /** "Modified" flag of the Cell. */
    public final boolean modified;
    /** Memoized data for size computation (connectivity etc). */
    private volatile Memoization m;
    /** Arc shrinkage data */
    private AbstractShapeBuilder.Shrinkage shrinkage;
    /** Bounds of primitive arcs in this Cell. */
    private ERectangle primitiveBounds;

    /** Creates a new instance of CellBackup */
    private CellBackup(CellRevision cellRevision, TechPool techPool, boolean modified) {
        this.cellRevision = cellRevision;
        this.techPool = techPool;
        this.modified = modified;
        cellBackupsCreated++;
        if (Job.getDebug()) {
            check();
        }
    }

    /** Creates a new instance of CellBackup */
    public static CellBackup newInstance(ImmutableCell d, TechPool techPool) {
        if (d.cellId.idManager != techPool.idManager) {
            throw new IllegalArgumentException();
        }
        if (techPool.getTech(d.techId) == null) {
            throw new IllegalArgumentException();
        }
        CellRevision cellRevision = new CellRevision(d);
        TechPool restrictedPool = techPool.restrict(cellRevision.techUsages, techPool);
        return new CellBackup(cellRevision, restrictedPool, true);
    }

    /**
     * Creates a new instance of CellBackup which differs from this CellBackup.
     * Four array parameters are supplied. Each parameter may be null if its contents is the same as in this Snapshot.
     * @param d new persistent data of a cell.
     * @param nodesArray new array of nodes
     * @param arcsArray new array of arcs
     * @param exportsArray new array of exports
     * @param superPool TechPool which defines all used technologies
     * @return new snapshot which differs froms this Snapshot or this Snapshot.
     * @throws IllegalArgumentException on invariant violation.
     * @throws ArrayOutOfBoundsException on some invariant violations.
     */
    public CellBackup with(ImmutableCell d,
            ImmutableNodeInst[] nodesArray, ImmutableArcInst[] arcsArray, ImmutableExport[] exportsArray,
            TechPool superPool) {
        CellRevision newRevision = cellRevision.with(d, nodesArray, arcsArray, exportsArray);
        TechPool restrictedPool = superPool.restrict(newRevision.techUsages, techPool);
        if (newRevision == cellRevision && restrictedPool == techPool) {
            return this;
        }
        if (arcsArray != null) {
            for (ImmutableArcInst a : arcsArray) {
                if (a != null && !a.check(restrictedPool)) {
                    throw new IllegalArgumentException("arc " + a.name + " is not compatible with TechPool");
                }
            }
        }
        return new CellBackup(newRevision, restrictedPool, modified || newRevision != cellRevision);
    }

    /**
     * Creates a new instance of CellBackup which differs from this CellBackup by revision date.
     * @param revisionDate new revision date.
     * @return new CellBackup which differs from this CellBackup by revision date.
     */
    public CellBackup withRevisionDate(long revisionDate) {
        CellRevision newRevision = cellRevision.withRevisionDate(revisionDate);
        if (newRevision == cellRevision) {
            return this;
        }
        return new CellBackup(newRevision, this.techPool, true);
    }

    /**
     * Creates a new instance of CellBackup with modified flag off.
     * @return new snapshot which differs froms this Snapshot or this Snapshot.
     */
    public CellBackup withoutModified() {
        if (!this.modified) {
            return this;
        }
        return new CellBackup(this.cellRevision, this.techPool, false);
    }

    /**
     * Returns CellBackup which differs from this CellBackup by TechPool.
     * @param techPool technology map.
     * @return CellBackup with new TechPool.
     */
    public CellBackup withTechPool(TechPool techPool) {
        TechPool restrictedPool = techPool.restrict(cellRevision.techUsages, this.techPool);
        if (this.techPool == restrictedPool) {
            return this;
        }
        if (techPool.idManager != this.techPool.idManager) {
            throw new IllegalArgumentException();
        }
//        for (Technology tech: this.techPool.values()) {
//            if (techPool.get(tech.getId()) != tech)
//                throw new IllegalArgumentException();
//        }
        return new CellBackup(this.cellRevision, restrictedPool, this.modified);
    }

    /**
     * Returns CellBackup which differs from this CellBackup by renamed Ids.
     * @param idMapper a map from old Ids to new Ids.
     * @return CellBackup with renamed Ids.
     */
    CellBackup withRenamedIds(IdMapper idMapper, CellName newGroupName) {
        CellRevision newRevision = cellRevision.withRenamedIds(idMapper, newGroupName);
        if (newRevision == cellRevision) {
            return this;
        }
        return new CellBackup(newRevision, this.techPool, true);
    }

    /**
     * Writes this CellBackup to IdWriter.
     * @param writer where to write.
     */
    void write(IdWriter writer) throws IOException {
        cellRevision.write(writer);
        writer.writeBoolean(modified);
    }

    /**
     * Reads CellBackup from SnapshotReader.
     * @param reader where to read.
     */
    static CellBackup read(IdReader reader, TechPool techPool) throws IOException {
        CellRevision newRevision = CellRevision.read(reader);
        boolean modified = reader.readBoolean();
        TechPool restrictedPool = techPool.restrict(newRevision.techUsages, techPool);
        return new CellBackup(newRevision, restrictedPool, modified);
    }

    /**
     * Checks invariant of this CellBackup.
     * @throws AssertionError if invariant is broken.
     */
    public void check() {
        cellRevision.check();
        IdManager idManager = cellRevision.d.cellId.idManager;
        assert techPool.idManager == idManager;
        BitSet techUsages = new BitSet();
        for (Technology tech : techPool.values()) {
            int techIndex = tech.getId().techIndex;
            assert !techUsages.get(techIndex);
            techUsages.set(techIndex);
        }
        assert techUsages.equals(cellRevision.techUsages);
//        for (int techIndex = 0; techIndex < cellRevision.techUsages.length(); techIndex++) {
//            if (cellRevision.techUsages.get(techIndex))
//                assert techPool.getTech(idManager.getTechId(techIndex)) != null;
//        }
        for (ImmutableArcInst a : cellRevision.arcs) {
            if (a != null) {
                a.check(techPool);
            }
        }

        if (m != null) {
            m.check();
        }
    }

    @Override
    public String toString() {
        return cellRevision.toString();
    }

    /**
     * Returns data for size computation (connectivity etc).
     * @return data for size computation.
     */
    public Memoization getMemoization() {
        Memoization m = this.m;
        if (m != null) {
            return m;
        }
        return this.m = new Memoization();
    }

    public Memoization computeMemoization() {
        return new Memoization();
    }

    /**
     * Returns data for arc shrinkage computation.
     * @return data for arc shrinkage computation.
     */
    public AbstractShapeBuilder.Shrinkage getShrinkage() {
        if (shrinkage == null) {
            shrinkage = new AbstractShapeBuilder.Shrinkage(this);
        }
        return shrinkage;
    }

    /**
     * Returns bounds of all primitive arcs in this Cell or null if there are not primitives.
     * @return bounds of all primitive arcs or null.
     */
    public ERectangle getPrimitiveBounds() {
        ERectangle primitiveBounds = this.primitiveBounds;
        if (primitiveBounds != null) {
            return primitiveBounds;
        }
        return this.primitiveBounds = computePrimitiveBounds();
    }

    public ERectangle computePrimitiveBounds() {
        double cellLowX = 0, cellHighX = 0, cellLowY = 0, cellHighY = 0;
        boolean boundsEmpty = true;
        BoundsBuilder b = new BoundsBuilder(this);
        Rectangle2D.Double bounds = new Rectangle2D.Double();
        for (ImmutableNodeInst n : cellRevision.nodes) {
            if (!(n.protoId instanceof PrimitiveNodeId)) {
                continue;
            }
            NodeProto np = techPool.getPrimitiveNode((PrimitiveNodeId) n.protoId);
            n.computeBounds(b, bounds);

            // special case: do not include "cell center" primitives from Generic
            if (np == Generic.tech().cellCenterNode) {
                continue;
            }

            // special case for invisible pins: do not include if inheritable or interior-only
            if (np == Generic.tech().invisiblePinNode) {
                boolean found = false;
                for (Iterator<Variable> it = n.getVariables(); it.hasNext();) {
                    Variable var = it.next();
                    if (var.isDisplay()) {
                        TextDescriptor td = var.getTextDescriptor();
                        if (td.isInterior() || td.isInherit()) {
                            found = true;
                            break;
                        }
                    }
                }
                if (found) {
                    continue;
                }
            }

            double lowx = bounds.getMinX();
            double highx = bounds.getMaxX();
            double lowy = bounds.getMinY();
            double highy = bounds.getMaxY();
            if (boundsEmpty) {
                boundsEmpty = false;
                cellLowX = lowx;
                cellHighX = highx;
                cellLowY = lowy;
                cellHighY = highy;
            } else {
                if (lowx < cellLowX) {
                    cellLowX = lowx;
                }
                if (highx > cellHighX) {
                    cellHighX = highx;
                }
                if (lowy < cellLowY) {
                    cellLowY = lowy;
                }
                if (highy > cellHighY) {
                    cellHighY = highy;
                }
            }
        }
        long gridMinX = DBMath.lambdaToGrid(cellLowX);
        long gridMinY = DBMath.lambdaToGrid(cellLowY);
        long gridMaxX = DBMath.lambdaToGrid(cellHighX);
        long gridMaxY = DBMath.lambdaToGrid(cellHighY);

        ERectangle primitiveArcBounds = computePrimitiveBoundsOfArcs();
        if (boundsEmpty) {
            return primitiveArcBounds;
        }
        if (primitiveArcBounds != null) {
            assert !boundsEmpty;
            gridMinX = Math.min(gridMinX, primitiveArcBounds.getGridMinX());
            gridMaxX = Math.max(gridMaxX, primitiveArcBounds.getGridMaxX());
            gridMinY = Math.min(gridMinY, primitiveArcBounds.getGridMinY());
            gridMaxY = Math.max(gridMaxY, primitiveArcBounds.getGridMaxY());
        }
        return ERectangle.fromGrid(gridMinX, gridMinY, gridMaxX - gridMinX, gridMaxY - gridMinY);
    }

    /**
     * Method to return a map from arcId of arcs in the Cell to their arcIndex in alphanumerical order.
     * @return a map from arcId to arcIndex.
     */
    public int[] getArcIndexByArcIdMap() {
        return getMemoization().arcIndexByArcId.clone();
    }

    private ERectangle computePrimitiveBoundsOfArcs() {
        ImmutableArrayList<ImmutableArcInst> arcs = cellRevision.arcs;
        if (arcs.isEmpty()) {
            return null;
        }
        int intMinX = Integer.MAX_VALUE, intMinY = Integer.MAX_VALUE, intMaxX = Integer.MIN_VALUE, intMaxY = Integer.MIN_VALUE;
        int[] intCoords = new int[4];
        BoundsBuilder boundsBuilder = new BoundsBuilder(this);
        for (ImmutableArcInst a : arcs) {
            if (boundsBuilder.genBoundsEasy(a, intCoords)) {
                int x1 = intCoords[0];
                if (x1 < intMinX) {
                    intMinX = x1;
                }
                int y1 = intCoords[1];
                if (y1 < intMinY) {
                    intMinY = y1;
                }
                int x2 = intCoords[2];
                if (x2 > intMaxX) {
                    intMaxX = x2;
                }
                int y2 = intCoords[3];
                if (y2 > intMaxY) {
                    intMaxY = y2;
                }
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
                    iw >= 0 ? iw : (long) intMaxX - (long) intMinX,
                    ih >= 0 ? ih : (long) intMaxY - (long) intMinY);
        }
        if (intMinX > intMaxX) {
            return bounds;
        }
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
        /**
         * nodeIndex by nodeId.
         */
        private final int[] nodeIndexByNodeId;
        /**
         * arcIndex by arcId.
         */
        private final int[] arcIndexByArcId;
//        /**
//         * Base of a segment in portCounts array for valid nodeIds
//         */
//        private int[] portBasesForNodes;
//        /**
//         * Array which has an entry for every port instance.
//         * The index for port instance (nodeInd, portProtoId) is
//         * portBasesForNodes[nodeId] + portProtoId.chronIndex
//         */
//        private int[] portCounts;

        private final int[] connections;
        /** ImmutableExports sorted by original PortInst. */
        private final ImmutableExport[] exportIndexByOriginalPort;
        private final BitSet wiped = new BitSet();
        private final BitSet hardArcs = new BitSet();

        Memoization() {
//            long startTime = System.currentTimeMillis();
            cellBackupsMemoized++;

            nodeIndexByNodeId = makeNodeIndexByNodeId();
            arcIndexByArcId = makeArcIndexByArcId();

            connections = makeConnections1();
            exportIndexByOriginalPort = makeExportIndexByOriginalPort1();
//            if (USE_COUNTS_FOR_CONNECTIONS || USE_COUNTS_FOR_EXPORTS) {
//                initPortCounts();
//            }
//            connections = USE_COUNTS_FOR_CONNECTIONS ? makeConnections2() : makeConnections1();
//            exportIndexByOriginalPort = USE_COUNTS_FOR_EXPORTS ? makeExportIndexByOriginalPort2() : makeExportIndexByOriginalPort1();
//            portBasesForNodes = null;
//            portCounts = null;

            for (ImmutableArcInst a : cellRevision.arcs) {
                ArcProto ap = techPool.getArcProto(a.protoId);
                // wipe status
                if (ap.isWipable()) {
                    wiped.set(a.tailNodeId);
                    wiped.set(a.headNodeId);
                }
                // hard arcs
                if (!ap.getTechnology().isEasyShape(a, false)) {
                    hardArcs.set(a.arcId);
                }
            }

            ImmutableArrayList<ImmutableNodeInst> nodes = cellRevision.nodes;
            for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
                ImmutableNodeInst n = nodes.get(nodeIndex);
                NodeProtoId np = n.protoId;
                if (!(np instanceof PrimitiveNodeId && techPool.getPrimitiveNode((PrimitiveNodeId) np).isArcsWipe())) {
                    wiped.clear(n.nodeId);
                }
            }
//            long stopTime = System.currentTimeMillis();
//            System.out.println("Memoization " + cellRevision.d.cellId + " took " + (stopTime - startTime) + " msec");
            check();
        }

        private int[] makeNodeIndexByNodeId() {
            ImmutableArrayList<ImmutableNodeInst> nodes = cellRevision.nodes;
            int maxNodeId = -1;
            for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
                maxNodeId = Math.max(maxNodeId, nodes.get(nodeIndex).nodeId);
            }
            int[] nodesById = new int[maxNodeId + 1];
            Arrays.fill(nodesById, -1);
            for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
                ImmutableNodeInst n = nodes.get(nodeIndex);
                nodesById[n.nodeId] = nodeIndex;
            }
            return nodesById;
        }

        private int[] makeArcIndexByArcId() {
            ImmutableArrayList<ImmutableArcInst> arcs = cellRevision.arcs;
            int maxArcId = -1;
            for (int arcIndex = 0; arcIndex < arcs.size(); arcIndex++) {
                maxArcId = Math.max(maxArcId, arcs.get(arcIndex).arcId);
            }
            int[] arcsById = new int[maxArcId + 1];
            Arrays.fill(arcsById, -1);
            for (int arcIndex = 0; arcIndex < arcs.size(); arcIndex++) {
                ImmutableArcInst a = arcs.get(arcIndex);
                arcsById[a.arcId] = arcIndex;
            }
            return arcsById;
        }

        /**
         * Compute connections using sortConnections
         * @return connections
         */
        private int[] makeConnections1() {
            ImmutableArrayList<ImmutableArcInst> arcs = cellRevision.arcs;

            // Put connections into buckets by nodeId.
            int[] connections = new int[arcs.size() * 2];
            int[] connectionsByNodeId = new int[nodeIndexByNodeId.length];
            for (ImmutableArcInst a : arcs) {
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
                connections[connectionsByNodeId[a.tailNodeId]++] = i * 2;
                connections[connectionsByNodeId[a.headNodeId]++] = i * 2 + 1;
            }

            // Sort each bucket by portId.
            sum = 0;
            for (int nodeId = 0; nodeId < connectionsByNodeId.length; nodeId++) {
                int start = sum;
                sum = connectionsByNodeId[nodeId];
                if (sum - 1 > start) {
                    sortConnections(connections, start, sum - 1);
                }
            }
            return connections;
        }

        /**
         * Compute exportIndexByOriginalPort using Arrays.sort
         * @return exportIndexByOriginalPort
         */
        private ImmutableExport[] makeExportIndexByOriginalPort1() {
            ImmutableExport[] exportIndexByOriginalPort = cellRevision.exports.toArray(new ImmutableExport[cellRevision.exports.size()]);
            Arrays.sort(exportIndexByOriginalPort, BY_ORIGINAL_PORT);
            return exportIndexByOriginalPort;
        }

//        /**
//         * Compute connections using portBasesForNodes and portCounts
//         * @return connections
//         */
//        private int[] makeConnections2() {
//            ImmutableArrayList<ImmutableArcInst> arcs = cellRevision.arcs;
//
//            clearPortCounts();
//            for (int arcIndex = 0; arcIndex < arcs.size(); arcIndex++) {
//                ImmutableArcInst a = arcs.get(arcIndex);
//                incrementPortCount(a.tailNodeId, a.tailPortId);
//                incrementPortCount(a.headNodeId, a.headPortId);
//            }
//            portCountsToPortOffsets();
//            int[] connections = new int[arcs.size() * 2];
//            Arrays.fill(connections, -1);
//            for (int arcIndex = 0; arcIndex < arcs.size(); arcIndex++) {
//                ImmutableArcInst a = arcs.get(arcIndex);
//                connections[incrementPortCount(a.tailNodeId, a.tailPortId)] = arcIndex << 1;
//                connections[incrementPortCount(a.headNodeId, a.headPortId)] = (arcIndex << 1) | 1;
//            }
//            return connections;
//        }
//
//        /**
//         * Compute exportIndexByOriginalPort using portBasesForNodes and portCounts
//         * @return exportIndexByOriginalPort
//         */
//        private ImmutableExport[] makeExportIndexByOriginalPort2() {
//            clearPortCounts();
//            for (ImmutableExport e: cellRevision.exports) {
//                incrementPortCount(e.originalNodeId, e.originalPortId);
//            }
//            portCountsToPortOffsets();
//            ImmutableExport[] exportIndexByOriginalPort = new ImmutableExport[cellRevision.exports.size()];
//            for (int exportId = 0; exportId < cellRevision.exportIndex.length; exportId++) {
//                int exportIndex = cellRevision.exportIndex[exportId];
//                if (exportIndex < 0) continue;
//                ImmutableExport e = cellRevision.exports.get(exportIndex);
//                exportIndexByOriginalPort[incrementPortCount(e.originalNodeId, e.originalPortId)] = e;
//            }
//            return exportIndexByOriginalPort;
//        }
//
//        /**
//         * Initialize portBasesForNodes and portCounts
//         */
//        private void initPortCounts() {
//            // Prepare port map
//            CellId cellId = cellRevision.d.cellId;
//            IdentityHashMap<NodeProtoId,Integer> maxPortIds = new IdentityHashMap<NodeProtoId,Integer>();
//            for (int i = 0; i < cellRevision.cellUsages.length; i++) {
//                CellRevision.CellUsageInfo cui = cellRevision.cellUsages[i];
//                if (cui == null) continue;
//                assert cui.instCount > 0;
//                maxPortIds.put(cellId.getUsageIn(i).protoId, cui.usedExportsLength);
//            }
//            int totalPortCount = 0;
//            portBasesForNodes = new int[nodeIndexByNodeId.length];
//            Arrays.fill(portBasesForNodes, Integer.MIN_VALUE);
//            for (int nodeId = 0; nodeId < nodeIndexByNodeId.length; nodeId++) {
//                int nodeIndex = nodeIndexByNodeId[nodeId];
//                if (nodeIndex < 0) continue;
//                ImmutableNodeInst n = cellRevision.nodes.get(nodeIndex);
//                Integer portCount = maxPortIds.get(n.protoId);
//                if (portCount == null) {
//                    PrimitiveNode pn = techPool.getPrimitiveNode((PrimitiveNodeId)n.protoId);
//                    int maxPortId = -1;
//                    for (Iterator<PortProto> it = pn.getPorts(); it.hasNext(); )
//                        maxPortId = Math.max(maxPortId, it.next().getId().chronIndex);
//                    portCount = maxPortId + 1;
//                    maxPortIds.put(n.protoId, portCount);
//                }
//                portBasesForNodes[n.nodeId] = totalPortCount;
//                totalPortCount += portCount;
//            }
//            portCounts = new int[totalPortCount];
//        }
//
//        /*
//         * Clear portCounts
//         */
//        private void clearPortCounts() {
//            Arrays.fill(portCounts, 0);
//        }
//
//        /**
//         * Increment portCounts entry for specified port intance
//         * @param nodeId nodeId of port instance
//         * @param portId PortProtoId of port instance
//         * @return the value of portCounts entry before increment
//         */
//        private int incrementPortCount(int nodeId, PortProtoId portId) {
//            return portCounts[portBasesForNodes[nodeId] + portId.chronIndex]++;
//        }
//
//        /**
//         * Convert port counts to port offsets
//         */
//        private void portCountsToPortOffsets() {
//            int connOffset = 0;
//            for (int i = 0; i < portCounts.length; i++) {
//                int numConns = portCounts[i];
//                portCounts[i] = connOffset;
//                connOffset += numConns;
//            }
//        }

        /**
         * Returns arcIndex of specified ImmutableArcInst.
         * @param a specified ImmutableArcInst.
         * @return arcIndex of specified ImmutableArcInst.
         */
        public int getArcIndex(ImmutableArcInst a) {
            return arcIndexByArcId[a.arcId];
        }

        /**
         * Returns true of there are Exports on specified NodeInst.
         * @param originalNode specified NodeInst.
         * @return true if there are Exports on specified NodeInst.
         */
        public boolean hasExports(ImmutableNodeInst originalNode) {
            int startIndex = searchExportByOriginalPort(originalNode.nodeId, 0);
            if (startIndex >= exportIndexByOriginalPort.length) {
                return false;
            }
            ImmutableExport e = exportIndexByOriginalPort[startIndex];
            return e.originalNodeId == originalNode.nodeId;
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
                if (e.originalNodeId != originalNodeId) {
                    break;
                }
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
                if (e.originalNodeId != originalNodeId) {
                    break;
                }
            }
            return ArrayIterator.iterator(exportIndexByOriginalPort, startIndex, j);
        }

        /**
         * Method to determine whether the display of specified pin NodeInst should be supressed.
         * In Schematics technologies, pins are not displayed if there are 1 or 2 connections,
         * but are shown for 0 or 3 or more connections (called "Steiner points").
         * @param pin specified pin ImmutableNodeInst
         * @return true if specieifed pin NodeInst should be supressed.
         */
        public boolean pinUseCount(ImmutableNodeInst pin) {
            int numConnections = getNumConnections(pin);
            if (numConnections > 2) {
                return false;
            }
            if (hasExports(pin)) {
                return true;
            }
            if (numConnections == 0) {
                return false;
            }
            return true;
        }

        /**
         * Method to return a list of arcs connected to speciefed or all ports of
         * specified ImmutableNodeInst.
         * @param headEnds true if i-th arc connects by head end
         * @param n specified ImmutableNodeInst
         * @param portId specified port or null
         * @return a List of connected ImmutableArcInsts
         */
        public List<ImmutableArcInst> getConnections(BitSet headEnds, ImmutableNodeInst n, PortProtoId portId) {
            ArrayList<ImmutableArcInst> result = null;
            if (headEnds != null) {
                headEnds.clear();
            }
            int myNodeId = n.nodeId;
            int chronIndex = 0;
            if (portId != null) {
                assert portId.parentId == n.protoId;
                chronIndex = portId.chronIndex;
            }
            int i = searchConnectionByPort(myNodeId, chronIndex);
            int j = i;
            for (; j < connections.length; j++) {
                int con = connections[j];
                ImmutableArcInst a = getArcs().get(con >>> 1);
                boolean end = (con & 1) != 0;
                int nodeId = end ? a.headNodeId : a.tailNodeId;
                if (nodeId != myNodeId) {
                    break;
                }
                if (portId != null) {
                    PortProtoId endProtoId = end ? a.headPortId : a.tailPortId;
                    if (endProtoId.getChronIndex() != chronIndex) {
                        break;
                    }
                }
                if (result == null) {
                    result = new ArrayList<ImmutableArcInst>();
                }
                if (headEnds != null && end) {
                    headEnds.set(result.size());
                }
                result.add(a);
            }
            return result != null ? result : Collections.<ImmutableArcInst>emptyList();
        }

        /**
         * Returns true of there are Connections on specified ImmutableNodeInst
         * connected either to specified port or to all ports
         * @param n specified ImmutableNodeInst
         * @param portId specified port or null
         * @return true if there are Connections on specified ImmutableNodeInst amd specified port.
         */
        public boolean hasConnections(ImmutableNodeInst n, PortProtoId portId) {
            int chronIndex = 0;
            if (portId != null) {
                assert portId.parentId == n.protoId;
                chronIndex = portId.chronIndex;
            }
            int i = searchConnectionByPort(n.nodeId, chronIndex);
            if (i >= connections.length) {
                return false;
            }
            int con = connections[i];
            ImmutableArcInst a = getArcs().get(con >>> 1);
            boolean end = (con & 1) != 0;
            int nodeId = end ? a.headNodeId : a.tailNodeId;
            if (nodeId != n.nodeId) {
                return false;
            }
            return portId == null || portId == (end ? a.headPortId : a.tailPortId);
        }

        /**
         * Method to return the number of Connections on specified ImmutableNodeInst.
         * @param n specified ImmutableNodeInst
         * @return the number of Connections on specified ImmutableNodeInst.
         */
        public int getNumConnections(ImmutableNodeInst n) {
            int myNodeId = n.nodeId;
            int i = searchConnectionByPort(myNodeId, 0);
            int j = i;
            for (; j < connections.length; j++) {
                int con = connections[j];
                ImmutableArcInst a = getArcs().get(con >>> 1);
                boolean end = (con & 1) != 0;
                int nodeId = end ? a.headNodeId : a.tailNodeId;
                if (nodeId != myNodeId) {
                    break;
                }
            }
            return j - i;
        }

        private int searchExportByOriginalPort(int originalNodeId, int originalChronIndex) {
            int low = 0;
            int high = exportIndexByOriginalPort.length - 1;
            while (low <= high) {
                int mid = (low + high) >> 1; // try in a middle
                ImmutableExport e = exportIndexByOriginalPort[mid];
                int cmp = e.originalNodeId - originalNodeId;
                if (cmp == 0) {
                    cmp = e.originalPortId.getChronIndex() >= originalChronIndex ? 1 : -1;
                }

                if (cmp < 0) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
            return low;
        }

        private int searchConnectionByPort(int nodeId, int chronIndex) {
            int low = 0;
            int high = connections.length - 1;
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

                if (cmp < 0) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
            return low;
        }

        public CellBackup getCellBackup() {
            return CellBackup.this;
        }

        public TechPool getTechPool() {
            return techPool;
        }

        /**
         * Returns ImmutableNodeInst by its node id.
         * @param nodeId id of node.
         * @return ImmutableNodeInst with this id or null if node doesn't exist.
         */
        public ImmutableNodeInst getNodeById(int nodeId) {
            if (nodeId >= nodeIndexByNodeId.length)
                return null;
            int nodeIndex = nodeIndexByNodeId[nodeId];
            return nodeIndex >= 0 ? cellRevision.nodes.get(nodeIndex) : null;
        }

        public int getNodeIndexByNodeId(int nodeId) {
            return nodeIndexByNodeId[nodeId];
        }

        /**
         * Returns ImmutableArcInst by its arc id.
         * @param arcId id of node.
         * @return ImmutableArcInst with this id or null if node doesn't exist.
         */
        public ImmutableArcInst getArcById(int arcId) {
            if (arcId >= arcIndexByArcId.length)
                return null;
            int arcIndex = arcIndexByArcId[arcId];
            return arcIndex >= 0 ? cellRevision.arcs.get(arcIndex) : null;
        }

        public ImmutableArrayList<ImmutableArcInst> getArcs() {
            return cellRevision.arcs;
        }

        /**
         * Method to tell whether the specified ImmutableNodeInst is wiped.
         * Wiped ImmutableNodeInsts are erased.  Typically, pin ImmutableNodeInsts can be wiped.
         * This means that when an arc connects to the pin, it is no longer drawn.
         * In order for a ImmutableNodeInst to be wiped, its prototype must have the "setArcsWipe" state,
         * and the arcs connected to it must have "setWipable" in their prototype.
         * @param n specified ImmutableNodeInst
         * @return true if specified ImmutableNodeInst is wiped.
         */
        public boolean isWiped(ImmutableNodeInst n) {
            return wiped.get(n.nodeId);
        }

        public boolean isHardArc(int arcId) {
            return hardArcs.get(arcId);
        }

        /**
         * Checks invariant of this CellBackup.
         * @throws AssertionError if invariant is broken.
         */
        private void check() {
            for (int nodeIndex = 0; nodeIndex < cellRevision.nodes.size(); nodeIndex++) {
                ImmutableNodeInst n = cellRevision.nodes.get(nodeIndex);
                assert nodeIndexByNodeId[n.nodeId] == nodeIndex;
            }
            int numNodes = 0;
            for (int nodeId = 0; nodeId < nodeIndexByNodeId.length; nodeId++) {
                if (nodeIndexByNodeId[nodeId] == -1) continue;
                numNodes++;
            }
            assert numNodes == cellRevision.nodes.size();
            
            for (int arcIndex = 0; arcIndex < cellRevision.arcs.size(); arcIndex++) {
                ImmutableArcInst a = cellRevision.arcs.get(arcIndex);
                assert arcIndexByArcId[a.arcId] == arcIndex;
            }
            int numArcs = 0;
            for (int arcId = 0; arcId < arcIndexByArcId.length; arcId++) {
                if (arcIndexByArcId[arcId] == -1) continue;
                numArcs++;
            }
            assert numArcs == cellRevision.arcs.size();

            assert exportIndexByOriginalPort.length == cellRevision.exports.size();
            ImmutableExport prevE = null;
            for (ImmutableExport e : exportIndexByOriginalPort) {
                if (prevE != null) {
                    assert BY_ORIGINAL_PORT.compare(prevE, e) < 0;
                }
                assert e == cellRevision.getExport(e.exportId);
                prevE = e;
            }
            assert connections.length == cellRevision.arcs.size() * 2;
            for (int i = 1; i < connections.length; i++) {
                assert compareConnections(connections[i - 1], connections[i]) < 0;
            }
        }

        private void sortConnections(int[] connections, int l, int r) {
            ImmutableArrayList<ImmutableArcInst> arcs = cellRevision.arcs;
            while (r - l > BIN_SORT_THRESHOLD) {
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
                        if (portId.getChronIndex() > chronIndexX) {
                            break;
                        }
                        if (portId.getChronIndex() == chronIndexX && con >= x) {
                            break;
                        }
                        i++;
                    }

                    // while (compareConnections(x, connections[j]) < 0) j--;
                    for (;;) {
                        int con = connections[j];
                        ImmutableArcInst a = arcs.get(con >>> 1);
                        boolean end = (con & 1) != 0;
                        PortProtoId portId = end ? a.headPortId : a.tailPortId;
                        if (chronIndexX > portId.getChronIndex()) {
                            break;
                        }
                        if (chronIndexX == portId.getChronIndex() && x >= con) {
                            break;
                        }
                        j--;
                    }

                    if (i <= j) {
                        int w = connections[i];
                        connections[i] = connections[j];
                        connections[j] = w;
                        i++;
                        j--;
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
            binarySort(connections, l, r + 1);
        }

        /**
         * This is a specifalized version of {@link java.utils.TimSort#binarySort}.
         *
         * Sorts the specified portion of the specified array using a binary
         * insertion sort.  This is the best method for sorting small numbers
         * of elements.  It requires O(n log n) compares, but O(n^2) data
         * movement (worst case).
         *
         * If the initial part of the specified range is already sorted,
         * this method can take advantage of it: the method assumes that the
         * elements from index {@code lo}, inclusive, to {@code start},
         * exclusive are already sorted.
         *
         * @param a the array in which a range is to be sorted
         * @param lo the index of the first element in the range to be sorted
         * @param hi the index after the last element in the range to be sorted
         * @param start the index of the first element in the range that is
         *        not already known to be sorted (@code lo <= start <= hi}
         * @param c comparator to used for the sort
         */
        @SuppressWarnings("fallthrough")
        private void binarySort(int[] connections, int lo, int hi) {
            ImmutableArrayList<ImmutableArcInst> arcs = cellRevision.arcs;
            assert lo <= hi;
            int start = lo + 1;
            if (start >= hi) return;
            int conS;
            ImmutableArcInst aS;
            PortProtoId portIdS;
            int conL = connections[lo];
            ImmutableArcInst aL = arcs.get(conL >>> 1);
            PortProtoId portIdL = (conL & 1) != 0 ? aL.headPortId : aL.tailPortId;
            for (;;) {
                conS = connections[start];
                aS = arcs.get(conS >>> 1);
                portIdS = (conS & 1) != 0 ? aS.headPortId : aS.tailPortId;
                int cmp = portIdS.chronIndex - portIdL.chronIndex;
                if (cmp < 0) break;
                if (cmp == 0 && conS < conL) break;
                start++;
                if (start >= hi) return;
                conL = conS;
                aL = aS;
                portIdL = portIdS;
            }
            for (;;) {
                assert start < hi;
                // Set left (and right) to the index where a[start] (pivot) belongs
                int left = lo;
                int right = start;
                assert left <= right;
                /*
                 * Invariants:
                 *   pivot >= all in [lo, left).
                 *   pivot <  all in [right, start).
                 */
                while (left < right) {
                    int mid = (left + right) >>> 1;
                    int conM = connections[mid];
                    ImmutableArcInst aM = arcs.get(conM >>> 1);
                    PortProtoId portIdM = (conM & 1) != 0 ? aM.headPortId : aM.tailPortId;
                    int cmp = portIdS.chronIndex - portIdM.chronIndex;
                    if (cmp == 0) {
                        cmp = conS - conM;
                    }
                    if (cmp < 0)
                        right = mid;
                    else
                        left = mid + 1;
                }
                assert left == right;

                /*
                 * The invariants still hold: pivot >= all in [lo, left) and
                 * pivot < all in [left, start), so pivot belongs at left.  Note
                 * that if there are elements equal to pivot, left points to the
                 * first slot after them -- that's why this sort is stable.
                 * Slide elements over to make room for pivot.
                 */
                int n = start - left;  // The number of elements to move
                // Switch is just an optimization for arraycopy in default case
                switch(n) {
                    case 2:  connections[left + 2] = connections[left + 1];
                    case 1:  connections[left + 1] = connections[left];
                             break;
                    default: System.arraycopy(connections, left, connections, left + 1, n);
                }
                connections[left] = conS;

                start++;
                if (start >= hi) return;
                conS = connections[start];
                aS = arcs.get(conS >>> 1);
                portIdS = (conS & 1) != 0 ? aS.headPortId : aS.tailPortId;
            }
        }

        private int compareConnectionsSameNode(int con1, int con2) {
            ImmutableArcInst a1 = cellRevision.arcs.get(con1 >>> 1);
            ImmutableArcInst a2 = cellRevision.arcs.get(con2 >>> 1);
            PortProtoId portId1 = (con1 & 1) != 0 ? a1.headPortId : a1.tailPortId;
            PortProtoId portId2 = (con2 & 1) != 0 ? a2.headPortId : a2.tailPortId;
            int cmp = portId1.chronIndex - portId2.chronIndex;
            return cmp != 0 ? cmp : con1 - con2;
        }

        private int compareConnections(int con1, int con2) {
            ImmutableArcInst a1 = cellRevision.arcs.get(con1 >>> 1);
            ImmutableArcInst a2 = cellRevision.arcs.get(con2 >>> 1);
            boolean end1 = (con1 & 1) != 0;
            boolean end2 = (con2 & 1) != 0;
            int nodeId1 = end1 ? a1.headNodeId : a1.tailNodeId;
            int nodeId2 = end2 ? a2.headNodeId : a2.tailNodeId;
            int cmp = nodeId1 - nodeId2;
            if (cmp != 0) {
                return cmp;
            }
            PortProtoId portId1 = end1 ? a1.headPortId : a1.tailPortId;
            PortProtoId portId2 = end2 ? a2.headPortId : a2.tailPortId;
            cmp = portId1.getChronIndex() - portId2.getChronIndex();
            if (cmp != 0) {
                return cmp;
            }
            return con1 - con2;
        }
    }
    private static final Comparator<ImmutableExport> BY_ORIGINAL_PORT = new Comparator<ImmutableExport>() {

        public int compare(ImmutableExport e1, ImmutableExport e2) {
            int result = e1.originalNodeId - e2.originalNodeId;
            if (result != 0) {
                return result;
            }
            result = e1.originalPortId.getChronIndex() - e2.originalPortId.getChronIndex();
            if (result != 0) {
                return result;
            }
            return e1.exportId.chronIndex - e2.exportId.chronIndex;
        }
    };
}
