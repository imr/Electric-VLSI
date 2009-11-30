/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellRevision.java
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

import static com.sun.electric.database.UsageCollector.EMPTY_BITSET;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.CellUsage;
import com.sun.electric.database.id.ExportId;
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.database.id.PortProtoId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * This class represents Cell data (with all arcs/nodes/exports) as it is saved to disk.
 * This representation should be technology-independent
 */
public class CellRevision {

    public static final CellRevision[] NULL_ARRAY = {};
    public static final ImmutableArrayList<CellRevision> EMPTY_LIST = new ImmutableArrayList<CellRevision>(NULL_ARRAY);
    private static final int[] NULL_INT_ARRAY = {};
    static final CellUsageInfo[] NULL_CELL_USAGE_INFO_ARRAY = {};
    static int cellRevisionsCreated = 0;
    /** Cell persistent data. */
    public final ImmutableCell d;
    /** An array of Exports on the Cell by chronological index. */
    public final ImmutableArrayList<ImmutableExport> exports;
    /** A list of NodeInsts in this Cell. */
    public final ImmutableArrayList<ImmutableNodeInst> nodes;
    /** A list of ArcInsts in this Cell. */
    public final ImmutableArrayList<ImmutableArcInst> arcs;
    /** TechId usage counts. */
    final BitSet techUsages;
    /** CellUsageInfos indexed by CellUsage.indefInParent */
    final CellUsageInfo[] cellUsages;
    /** definedExport == [0..definedExportLength) - deletedExports . */
    /** Map from chronIndex of Exports to sortIndex. */
    final int exportIndex[];
    /** Bitmap of defined exports. */
    final BitSet definedExports;
    /** Length of defined exports. */
    final int definedExportsLength;
    /** Bitmap of deleted exports. */
    final BitSet deletedExports;

    /** Creates a new instance of CellRevision */
    private CellRevision(ImmutableCell d,
            ImmutableArrayList<ImmutableNodeInst> nodes,
            ImmutableArrayList<ImmutableArcInst> arcs,
            ImmutableArrayList<ImmutableExport> exports,
            BitSet techUsages,
            CellUsageInfo[] cellUsages, int[] exportIndex, BitSet definedExports, int definedExportsLength, BitSet deletedExports) {
        this.d = d;
        this.nodes = nodes;
        this.arcs = arcs;
        this.exports = exports;
        this.techUsages = techUsages;
        this.cellUsages = cellUsages;
        this.exportIndex = exportIndex;
        this.definedExports = definedExports;
        this.definedExportsLength = definedExportsLength;
        this.deletedExports = deletedExports;
        cellRevisionsCreated++;
    }

    /** Creates a new instance of CellRevision */
    public CellRevision(ImmutableCell d) {
        this(d, ImmutableNodeInst.EMPTY_LIST, ImmutableArcInst.EMPTY_LIST, ImmutableExport.EMPTY_LIST,
                makeTechUsages(d.techId), NULL_CELL_USAGE_INFO_ARRAY, NULL_INT_ARRAY, EMPTY_BITSET, 0, EMPTY_BITSET);
        if (d.techId == null) {
            throw new NullPointerException("techId");
        }
    }

    private static BitSet makeTechUsages(TechId techId) {
        BitSet techUsages = new BitSet();
        techUsages.set(techId.techIndex);
        return techUsages;
    }

    /**
     * Creates a new instance of CellRevision which differs from this CellRevision by revision date.
     * @param revisionDate new revision date.
     * @return new CellRevision which differs from this CellRevision by revision date.
     */
    public CellRevision withRevisionDate(long revisionDate) {
        if (d.revisionDate == revisionDate) {
            return this;
        }
        return new CellRevision(this.d.withRevisionDate(revisionDate), this.nodes, this.arcs, this.exports,
                this.techUsages, this.cellUsages, this.exportIndex,
                this.definedExports, this.definedExportsLength, this.deletedExports);
    }

    /**
     * Creates a new instance of CellRevision which differs from this CellRevision.
     * Four array parameters are supplied. Each parameter may be null if its contents is the same as in this Snapshot.
     * @param d new persistent data of a cell.
     * @param nodesArray new array of nodes
     * @param arcsArray new array of arcs
     * @param exportsArray new array of exports
     * @return new snapshot which differs froms this Snapshot or this Snapshot.
     * @throws IllegalArgumentException on invariant violation.
     * @throws ArrayOutOfBoundsException on some invariant violations.
     */
    public CellRevision with(ImmutableCell d,
            ImmutableNodeInst[] nodesArray, ImmutableArcInst[] arcsArray, ImmutableExport[] exportsArray) {
        ImmutableArrayList<ImmutableNodeInst> nodes = copyArray(nodesArray, this.nodes);
        ImmutableArrayList<ImmutableArcInst> arcs = copyArray(arcsArray, this.arcs);
        ImmutableArrayList<ImmutableExport> exports = copyArray(exportsArray, this.exports);
        if (this.d == d && this.nodes == nodes && this.arcs == arcs && this.exports == exports) {
            return this;
        }

        CellId cellId = d.cellId;
        boolean busNamesAllowed = d.busNamesAllowed();
        if (this.d != d) {
            if (d.techId == null) {
                throw new NullPointerException("tech");
            }
//            if (cellId != this.d.cellId)
//                throw new IllegalArgumentException("cellId");
        }

        BitSet techUsages = this.techUsages;
        CellUsageInfo[] cellUsages = this.cellUsages;
        if (this.d.cellId != d.cellId || this.d.techId != d.techId || this.d.getVars() != d.getVars() || nodes != this.nodes || arcs != this.arcs || exports != this.exports) {
            UsageCollector uc = new UsageCollector(d, nodes, arcs, exports);
            techUsages = uc.getTechUsages(this.techUsages);
            cellUsages = uc.getCellUsages(this.cellUsages);
        }
        if (cellId.isIcon() && cellUsages.length != 0) {
            throw new IllegalArgumentException("Icon contains subcells");
        }

        if (nodes != this.nodes && !nodes.isEmpty()) {
            boolean hasCellCenter = false;
            ImmutableNodeInst prevN = null;
            for (int i = 0; i < nodes.size(); i++) {
                ImmutableNodeInst n = nodes.get(i);
                if (ImmutableNodeInst.isCellCenter(n.protoId)) {
                    if (hasCellCenter) {
                        throw new IllegalArgumentException("Duplicate cell center");
                    }
                    hasCellCenter = true;
                }
                if (!busNamesAllowed && n.name.isBus()) {
                    throw new IllegalArgumentException("arrayedName " + n.name);
                }
                if (prevN != null && TextUtils.STRING_NUMBER_ORDER.compare(prevN.name.toString(), n.name.toString()) >= 0) {
                    throw new IllegalArgumentException("nodes order");
                }
                prevN = n;
            }
        }

        if (arcs != this.arcs && !arcs.isEmpty()) {
            ImmutableArcInst prevA = null;
            for (int i = 0; i < arcs.size(); i++) {
                ImmutableArcInst a = arcs.get(i);
                if (!busNamesAllowed && a.name.isBus()) {
                    throw new IllegalArgumentException("arrayedName " + a.name);
                }
                if (prevA != null) {
                    int cmp = TextUtils.STRING_NUMBER_ORDER.compare(prevA.name.toString(), a.name.toString());
                    if (cmp > 0 || cmp == 0 && (a.name.isTempname() || prevA.arcId >= a.arcId))
                        throw new IllegalArgumentException("arcs order");
                }
                prevA = a;
            }
        }

        int[] exportIndex = this.exportIndex;
        BitSet definedExports = this.definedExports;
        int definedExportsLength = this.definedExportsLength;
        BitSet deletedExports = this.deletedExports;
        if (exports != this.exports) {
            int exportIndexLength = 0;
            String prevExportName = null;
            for (ImmutableExport e : exports) {
                if (e.exportId.parentId != cellId) {
                    throw new IllegalArgumentException("exportId");
                }
                String exportName = e.name.toString();
                if (!busNamesAllowed && e.name.isBus()) {
                    throw new IllegalArgumentException("arrayedName " + e.name);
                }
                if (prevExportName != null && TextUtils.STRING_NUMBER_ORDER.compare(prevExportName, exportName) >= 0) {
                    throw new IllegalArgumentException("exportName");
                }
                prevExportName = exportName;
                int chronIndex = e.exportId.chronIndex;
                exportIndexLength = Math.max(exportIndexLength, chronIndex + 1);
            }
            exportIndex = new int[exportIndexLength];
            Arrays.fill(exportIndex, -1);
            for (int portIndex = 0; portIndex < exports.size(); portIndex++) {
                ImmutableExport e = exports.get(portIndex);
                int chronIndex = e.exportId.chronIndex;
                if (exportIndex[chronIndex] >= 0) {
                    throw new IllegalArgumentException("exportChronIndex");
                }
                exportIndex[chronIndex] = portIndex;
                //checkPortInst(nodesById.get(e.originalNodeId), e.originalPortId);
            }
            if (Arrays.equals(this.exportIndex, exportIndex)) {
                exportIndex = this.exportIndex;
            } else {
                definedExports = new BitSet();
                for (int chronIndex = 0; chronIndex < exportIndex.length; chronIndex++) {
                    if (exportIndex[chronIndex] < 0) {
                        continue;
                    }
                    definedExports.set(chronIndex);
                }
                definedExports = UsageCollector.bitSetWith(this.definedExports, definedExports);
                if (definedExports != this.definedExports) {
                    definedExportsLength = definedExports.length();
                    deletedExports = new BitSet();
                    deletedExports.set(0, definedExportsLength);
                    deletedExports.andNot(definedExports);
                    deletedExports = UsageCollector.bitSetWith(this.deletedExports, deletedExports);
                }
            }
        }

        CellRevision revision = new CellRevision(d, nodes, arcs, exports,
                techUsages, cellUsages, exportIndex, definedExports, definedExportsLength, deletedExports);
        return revision;
    }

    private static <T> ImmutableArrayList<T> copyArray(T[] newArray, ImmutableArrayList<T> oldList) {
        return newArray != null ? new ImmutableArrayList<T>(newArray) : oldList;
    }

    /**
     * Returns CellRevision which differs from this CellRevision by renamed Ids.
     * @param idMapper a map from old Ids to new Ids.
     * @return CellRevision with renamed Ids.
     */
    CellRevision withRenamedIds(IdMapper idMapper, CellName newGroupName) {
        ImmutableCell d = this.d.withRenamedIds(idMapper).withGroupName(newGroupName);

        ImmutableNodeInst[] nodesArray = null;
        for (int i = 0; i < nodes.size(); i++) {
            ImmutableNodeInst oldNode = nodes.get(i);
            ImmutableNodeInst newNode = oldNode.withRenamedIds(idMapper);
            if (newNode != oldNode && nodesArray == null) {
                nodesArray = new ImmutableNodeInst[nodes.size()];
                for (int j = 0; j < i; j++) {
                    nodesArray[j] = nodes.get(j);
                }
            }
            if (nodesArray != null) {
                nodesArray[i] = newNode;
            }
        }

        ImmutableArcInst[] arcsArray = null;
        for (int i = 0; i < arcs.size(); i++) {
            ImmutableArcInst oldArc = arcs.get(i);
            ImmutableArcInst newArc = oldArc.withRenamedIds(idMapper);
            if (newArc != oldArc && arcsArray == null) {
                arcsArray = new ImmutableArcInst[arcs.size()];
                for (int j = 0; j < i; j++) {
                    arcsArray[j] = arcs.get(j);
                }
            }
            if (arcsArray != null) {
                arcsArray[i] = newArc;
            }
        }

        ImmutableExport[] exportsArray = null;
        for (int i = 0; i < exports.size(); i++) {
            ImmutableExport oldExport = exports.get(i);
            ImmutableExport newExport = oldExport.withRenamedIds(idMapper);
            if (newExport != oldExport && exportsArray == null) {
                exportsArray = new ImmutableExport[exports.size()];
                for (int j = 0; j < i; j++) {
                    exportsArray[j] = exports.get(j);
                }
            }
            if (exportsArray != null) {
                exportsArray[i] = newExport;
            }
        }

        if (this.d == d && nodesArray == null && arcsArray == null && exportsArray == null) {
            return this;
        }
        CellRevision newRevision = with(d, nodesArray, arcsArray, exportsArray);
        newRevision.check();
        return newRevision;
    }

    /**
     * Returns ImmutableExport by its export id.
     * @param exportId id of export.
     * @return ImmutableExport with this id or null if node doesn't exist.
     */
    public ImmutableExport getExport(ExportId exportId) {
        if (exportId.parentId != d.cellId) {
            throw new IllegalArgumentException();
        }
        int chronIndex = exportId.chronIndex;
        int portIndex = chronIndex < exportIndex.length ? exportIndex[chronIndex] : -1;
        return portIndex >= 0 ? exports.get(portIndex) : null;
    }

    public int getExportIndexByExportId(ExportId exportId) {
        if (exportId.parentId != d.cellId) {
            throw new IllegalArgumentException();
        }
        int chronIndex = exportId.chronIndex;
        return chronIndex < exportIndex.length ? exportIndex[chronIndex] : -1;
    }

    /**
     * Returns subcell instance counts, indexed by CellUsage.indexInParent.
     * @return subcell instance counts, indexed by CellUsage.indexInParent.
     */
    public int[] getInstCounts() {
        int l = cellUsages.length;
        while (l > 0 && (cellUsages[l - 1] == null || cellUsages[l - 1].instCount == 0)) {
            l--;
        }
        if (l == 0) {
            return NULL_INT_ARRAY;
        }
        int[] instCounts = new int[l];
        for (int indexInParent = 0; indexInParent < l; indexInParent++) {
            if (cellUsages[indexInParent] != null) {
                instCounts[indexInParent] = cellUsages[indexInParent].instCount;
            }
        }
        return instCounts;
    }

    /**
     * For given CellUsage in this cell returns count of subcell instances.
     * @param u CellUsage.
     * @return count of subcell instances.
     * @throws IllegalArgumentException if CellUsage's parent is not this cell.
     */
    public int getInstCount(CellUsage u) {
        if (u.parentId != d.cellId) {
            throw new IllegalArgumentException();
        }
        if (u.indexInParent >= cellUsages.length) {
            return 0;
        }
        CellUsageInfo cui = cellUsages[u.indexInParent];
        if (cui == null) {
            return 0;
        }
        return cui.instCount;
    }

    /**
     * Returns Set of Technologies used in this CellRevision
     */
    public Set<TechId> getTechUsages() {
        LinkedHashSet<TechId> techUsagesSet = new LinkedHashSet<TechId>();
        for (int techIndex = 0; techIndex < techUsages.length(); techIndex++) {
            if (techUsages.get(techIndex)) {
                techUsagesSet.add(d.cellId.idManager.getTechId(techIndex));
            }
        }
        return techUsagesSet;
    }

    /**
     * Writes this CellRevision to IdWriter.
     * @param writer where to write.
     */
    void write(IdWriter writer) throws IOException {
        d.write(writer);
        writer.writeInt(nodes.size());
        for (ImmutableNodeInst n : nodes) {
            n.write(writer);
        }
        writer.writeInt(arcs.size());
        for (ImmutableArcInst a : arcs) {
            a.write(writer);
        }
        writer.writeInt(exports.size());
        for (ImmutableExport e : exports) {
            e.write(writer);
        }
    }

    /**
     * Reads CellRevision from SnapshotReader.
     * @param reader where to read.
     */
    static CellRevision read(IdReader reader) throws IOException {
        ImmutableCell d = ImmutableCell.read(reader);
        CellRevision revision = new CellRevision(d.withoutVariables());

        int nodesLength = reader.readInt();
        ImmutableNodeInst[] nodes = new ImmutableNodeInst[nodesLength];
        for (int i = 0; i < nodesLength; i++) {
            nodes[i] = ImmutableNodeInst.read(reader);
        }

        int arcsLength = reader.readInt();
        ImmutableArcInst[] arcs = new ImmutableArcInst[arcsLength];
        for (int i = 0; i < arcsLength; i++) {
            arcs[i] = ImmutableArcInst.read(reader);
        }

        int exportsLength = reader.readInt();
        ImmutableExport[] exports = new ImmutableExport[exportsLength];
        for (int i = 0; i < exportsLength; i++) {
            exports[i] = ImmutableExport.read(reader);
        }

        revision = revision.with(d, nodes, arcs, exports);
        return revision;
    }

    /**
     * Checks invariant of this CellRevision.
     * @throws AssertionError if invariant is broken.
     */
    public void check() {
        d.check();
        CellId cellId = d.cellId;
        boolean busNamesAllowed = d.busNamesAllowed();
        BitSet checkTechUsages = new BitSet();
        checkTechUsages.set(d.techId.techIndex);
        int[] checkCellUsages = getInstCounts();
        boolean hasCellCenter = false;
        ArrayList<ImmutableNodeInst> nodesById = new ArrayList<ImmutableNodeInst>();
        ImmutableNodeInst prevN = null;
        for (ImmutableNodeInst n : nodes) {
            n.check();
            if (ImmutableNodeInst.isCellCenter(n.protoId)) {
                assert !hasCellCenter;
                hasCellCenter = true;
            }
            while (n.nodeId >= nodesById.size()) {
                nodesById.add(null);
            }
            ImmutableNodeInst oldNode = nodesById.set(n.nodeId, n);
            assert oldNode == null;
            assert busNamesAllowed || !n.name.isBus();
            if (prevN != null) {
                assert TextUtils.STRING_NUMBER_ORDER.compare(prevN.name.toString(), n.name.toString()) < 0;
            }
            prevN = n;
            if (n.protoId instanceof CellId) {
                CellId subCellId = (CellId) n.protoId;
                CellUsage u = cellId.getUsageIn(subCellId);
                checkCellUsages[u.indexInParent]--;
                CellUsageInfo cui = cellUsages[u.indexInParent];
                assert cui != null;
                for (int j = 0; j < n.ports.length; j++) {
                    ImmutablePortInst pid = n.ports[j];
                    if (pid == ImmutablePortInst.EMPTY) {
                        continue;
                    }
                    checkPortInst(n, subCellId.getPortId(j));
                }
                if (subCellId.isIcon()) {
                    for (Variable param : n.getDefinedParams()) {
                        assert cui.usedAttributes.get((Variable.AttrKey) param.getKey()) == param.getUnit();
                    }
                    for (Iterator<Variable> it = n.getVariables(); it.hasNext();) {
                        Variable.Key varKey = it.next().getKey();
                        if (varKey.isAttribute()) {
                            assert cui.usedAttributes.get(varKey) == null;
                        }
                    }
                }
            } else {
                TechId techId = ((PrimitiveNodeId) n.protoId).techId;
                checkTechUsages.set(techId.techIndex);
            }
        }
        for (int i = 0; i < checkCellUsages.length; i++) {
            assert checkCellUsages[i] == 0;
        }
        BitSet arcIds = new BitSet();
        ImmutableArcInst prevA = null;
        for (ImmutableArcInst a : arcs) {
            assert !arcIds.get(a.arcId);
            arcIds.set(a.arcId);
            assert busNamesAllowed || !a.name.isBus();
            if (prevA != null) {
                int cmp = TextUtils.STRING_NUMBER_ORDER.compare(prevA.name.toString(), a.name.toString());
                assert cmp <= 0;
                if (cmp == 0) {
                    assert !a.name.isTempname();
                    assert prevA.arcId < a.arcId;
                }
            }
            prevA = a;

            a.check();
            checkPortInst(nodesById.get(a.tailNodeId), a.tailPortId);
            checkPortInst(nodesById.get(a.headNodeId), a.headPortId);

            checkTechUsages.set(a.protoId.techId.techIndex);
        }

        if (exportIndex.length > 0) {
            assert exportIndex[exportIndex.length - 1] >= 0;
        }
        assert exportIndex.length == definedExportsLength;
        assert definedExports.length() == definedExportsLength;
        for (int i = 0; i < exports.size(); i++) {
            ImmutableExport e = exports.get(i);
            e.check();
            assert e.exportId.parentId == cellId;
            assert exportIndex[e.exportId.chronIndex] == i;
            assert busNamesAllowed || !e.name.isBus();
            if (i > 0) {
                assert (TextUtils.STRING_NUMBER_ORDER.compare(exports.get(i - 1).name.toString(), e.name.toString()) < 0) : i;
            }
            checkPortInst(nodesById.get(e.originalNodeId), e.originalPortId);
        }
        int exportCount = 0;
        for (int chronIndex = 0; chronIndex < exportIndex.length; chronIndex++) {
            int portIndex = exportIndex[chronIndex];
            if (portIndex == -1) {
                assert !definedExports.get(chronIndex);
                continue;
            }
            assert definedExports.get(chronIndex);
            exportCount++;
            assert exports.get(portIndex).exportId.chronIndex == chronIndex;
        }
        assert exports.size() == exportCount;
        BitSet checkDeleted = new BitSet();
        checkDeleted.set(0, definedExportsLength);
        checkDeleted.andNot(definedExports);
        assert deletedExports.equals(checkDeleted);
        if (definedExports.isEmpty()) {
            assert definedExports == EMPTY_BITSET;
        }
        if (deletedExports.isEmpty()) {
            assert deletedExports == EMPTY_BITSET;
        }
        assert techUsages.equals(checkTechUsages);

        if (d.cellId.isIcon()) {
            assert cellUsages.length == 0;
        }
        for (int i = 0; i < cellUsages.length; i++) {
            CellUsageInfo cui = cellUsages[i];
            if (cui == null) {
                continue;
            }
            cui.check(d.cellId.getUsageIn(i));
        }
    }

    private void checkPortInst(ImmutableNodeInst node, PortProtoId portId) {
        assert node != null;
        assert portId.getParentId() == node.protoId;
        if (portId instanceof ExportId) {
            checkExportId((ExportId) portId);
        }
    }

    private void checkExportId(ExportId exportId) {
        CellUsage u = d.cellId.getUsageIn(exportId.getParentId());
        assert cellUsages[u.indexInParent].usedExports.get(exportId.getChronIndex());
    }

    public boolean sameExports(CellRevision thatRevision) {
        if (thatRevision == this) {
            return true;
        }
        if (exports.size() != thatRevision.exports.size()) {
            return false;
        }
        for (int i = 0; i < exports.size(); i++) {
            if (exports.get(i).exportId != thatRevision.exports.get(i).exportId) {
                return false;
            }
        }
        return true;
    }

    static class CellUsageInfo {

        final int instCount;
        final BitSet usedExports;
        final int usedExportsLength;
        final TreeMap<Variable.AttrKey, TextDescriptor.Unit> usedAttributes;

        CellUsageInfo(int instCount, BitSet usedExports, TreeMap<Variable.AttrKey, TextDescriptor.Unit> usedAttributes) {
            this.instCount = instCount;
            usedExportsLength = usedExports.length();
            this.usedExports = usedExportsLength > 0 ? usedExports : EMPTY_BITSET;
            this.usedAttributes = usedAttributes;
        }

        CellUsageInfo with(int instCount, BitSet usedExports, TreeMap<Variable.AttrKey, TextDescriptor.Unit> usedAttributes) {
            usedExports = UsageCollector.bitSetWith(this.usedExports, usedExports);
            usedAttributes = UsageCollector.usedAttributesWith(this.usedAttributes, usedAttributes);
            if (this.instCount == instCount && this.usedExports == usedExports
                    && this.usedAttributes == usedAttributes) {
                return this;
            }
            return new CellUsageInfo(instCount, usedExports, usedAttributes);
        }

        void checkUsage(CellRevision subCellRevision) {
            if (subCellRevision == null) {
                throw new IllegalArgumentException("subCell deleted");
            }
            if (subCellRevision.definedExportsLength < usedExportsLength || subCellRevision.deletedExports.intersects(usedExports)) {
                throw new IllegalArgumentException("exportUsages");
            }
            if (isIcon()) {
                for (Map.Entry<Variable.AttrKey, TextDescriptor.Unit> e : usedAttributes.entrySet()) {
                    Variable.AttrKey paramKey = e.getKey();
                    Variable param = subCellRevision.d.getParameter(paramKey);
                    TextDescriptor.Unit unit = e.getValue();
                    if (unit != null) {
                        if (param == null || param.getUnit() != unit) {
                            throw new IllegalArgumentException("param " + paramKey);
                        }
                    } else {
                        if (param != null) {
                            throw new IllegalArgumentException("param " + paramKey);
                        }
                    }
                }
            }
        }

        private boolean isIcon() {
            return usedAttributes != null;
        }

        private void check(CellUsage u) {
            assert instCount > 0;
            assert usedExportsLength == usedExports.length();
            if (usedExportsLength == 0) {
                assert usedExports == EMPTY_BITSET;
            }
            assert isIcon() == u.protoId.isIcon();
            assert !u.parentId.isIcon();
        }
    }

    @Override
    public String toString() {
        return d.toString();
    }
}
