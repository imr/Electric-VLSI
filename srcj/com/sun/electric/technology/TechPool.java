/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TechPool.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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
package com.sun.electric.technology;

import com.sun.electric.database.CellRevision;
import com.sun.electric.database.Environment;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.id.ArcProtoId;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.database.id.LayerId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.id.PrimitivePortId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology.SizeCorrector;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.util.TextUtils;

import java.io.IOException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A customized Map from TechId to Technolgy.
 * All TechIds must belong to same IdManager.
 */
public class TechPool extends AbstractMap<TechId, Technology> {
//    public static final TechPool EMPTY = new TechPool(Collections.<Technology>emptySet());
    public final IdManager idManager;
    private final Technology[] techs;
    private final EntrySet entrySet;
    private ArcProto[] univList;
    /** Generic technology */
    private Generic generic;
    /** Artwork technology */
    private Artwork artwork;
    /** Schematic technology */
    private Schematics schematics;

    /**
     * Constructs empty TechPool
     * @param idManager pool's IdManager
     */
    public TechPool(IdManager idManager) {
        this.idManager = idManager;
        techs = new Technology[0];
        entrySet = new EntrySet();
        assert size() == 0;
        univList = new ArcProto[0];
    }

    /**
     * Constructs TechPool from Collection of technologies
     * All technologies must belong to the same IdManager
     * @param technologies Collection of technologies in the Pool
     */
    private TechPool(Collection<Technology> technologies) {
        Iterator<Technology> it = technologies.iterator();
        TechId techId = it.next().getId();
        idManager = techId.idManager;
        int maxTechIndex = techId.techIndex;
        while (it.hasNext()) {
            techId = it.next().getId();
            if (techId.idManager != idManager)
                throw new IllegalArgumentException();
            maxTechIndex = Math.max(maxTechIndex, techId.techIndex);
        }
        techs = new Technology[maxTechIndex + 1];
        for (Technology tech: technologies) {
            techId = tech.getId();
            int techIndex = techId.techIndex;
            if (techs[techIndex] != null)
                throw new IllegalArgumentException();
            techs[techIndex] = tech;
            if (tech instanceof Generic) {
                generic = (Generic) tech;
            } else if (tech instanceof Artwork) {
                artwork = (Artwork) tech;
            } else if (tech instanceof Schematics) {
                schematics = (Schematics) tech;
            }
        }
        entrySet = new EntrySet();
        assert size() == technologies.size();
    }

    public Map<TechFactory.Param,Object> getTechParams() {
        LinkedHashMap<TechFactory.Param,Object> techParams = new LinkedHashMap<TechFactory.Param,Object>();
        for (int techIndex = 0; techIndex < techs.length; techIndex++) {
            Technology tech = techs[techIndex];
            if (tech == null) continue;
            techParams.putAll(tech.getParamValues());
        }
        return techParams;
    }

    public TechPool withTechParams(Map<TechFactory.Param,Object> paramValues) {
        ArrayList<Technology> newTechs = new ArrayList<Technology>();
        boolean changed = false;
        for (int techIndex = 0; techIndex < techs.length; techIndex++) {
            Technology tech = techs[techIndex];
            if (tech == null) continue;
            Technology newTech = tech.withTechParams(paramValues);
            if (newTech != tech)
                changed = true;
            newTechs.add(newTech);
        }
        return changed ? new TechPool(newTechs) : this;
    }

    public TechPool deepClone() {
        ArrayList<Technology> newTechs = new ArrayList<Technology>();
        Generic newGeneric = Generic.newInstance(idManager);
        newTechs.add(newGeneric);
        for (int techIndex = 0; techIndex < techs.length; techIndex++) {
            Technology tech = techs[techIndex];
            if (tech == null || tech == generic) continue;
            Technology newTech = tech.techFactory.newInstance(generic, tech.getParamValues());
            newTechs.add(newTech);
        }
        return new TechPool(newTechs);
    }

    /**
     * Returns restriction of this TechPool to specified subset of TechIds.
     * A candidate TechPool is a valid result, it is returned to save allocation.
     * @param techUsed contains techIndex of those TechIds which are in subset
     * @param candidatePool a candidate TechPool to save allocation
     * @return restriction of this TechPool to specified subset of TechIds
     */
    public TechPool restrict(BitSet techUsed, TechPool candidatePool) {
        if (candidatePool != null && candidatePool.isRestriction(this, techUsed))
            return candidatePool;
        return restrict(techUsed);
    }

    /**
     * Returns restriction of this TechPool to specified subset of TechIds
     * @param techUsed contains techIndex of those TechIds which are in subset
     * @return restriction of this TechPool to specified subset of TechIds
     */
    public TechPool restrict(BitSet techUsed) {
        ArrayList<Technology> newTechs = new ArrayList<Technology>();
        for (int techIndex = 0; techIndex < techs.length; techIndex++) {
            if (techUsed.get(techIndex)) {
                newTechs.add(techs[techIndex]);
            }
        }
        if (newTechs.size() != techUsed.cardinality())
            throw new IllegalArgumentException();
        return newTechs.isEmpty() ? new TechPool(idManager) : new TechPool(newTechs);
    }

    /**
     * Returns true if this pool is the restriction of specified super pool.
     * @param superPool specified super pool
     * @return true if this pool is the restriction of specified super pool.
     */
    public boolean isRestriction(TechPool superPool) {
        return isRestriction(superPool, getTechUsages());
    }

    private boolean isRestriction(TechPool superPool, BitSet techUsed) {
        if (techUsed.length() != techs.length)
            return false;
        if (techs.length > superPool.techs.length)
            return false;
        if (idManager != superPool.idManager)
            return false;
        for (int techIndex = 0; techIndex < techs.length; techIndex++) {
            Technology tech = superPool.techs[techIndex];
            if (techUsed.get(techIndex)) {
                if (tech == null)
                    return false;
            } else {
                tech = null;
            }
            if (techs[techIndex] != tech)
                return false;
        }
        return true;
    }

    /**
     * Returns a BitSet of techIndices of Technologies from this TechPool.
     * @return techIndices of Technologies from this TechPool.
     */
    public BitSet getTechUsages() {
        BitSet bs = new BitSet(techs.length);
        for (int techIndex = 0; techIndex < techs.length; techIndex++) {
            if (techs[techIndex] != null)
                bs.set(techIndex);
        }
        return bs;
    }

    /**
     * Returns new TechPool which differs from this TechPool by adding
     * new technology
     * @param tech Technology to add
     * @return TechPool which differs from this TechPool by adding technology
     */
    public TechPool withTech(Technology tech) {
        TechId techId = tech.getId();
        int techIndex = techId.techIndex;
        if (techIndex < techs.length && techs[techIndex] == tech) {
            return this;
        }
        if (techId.idManager != idManager) {
            throw new IllegalArgumentException();
        }
        ArrayList<Technology> newTechs = new ArrayList<Technology>();
        for (int i = 0; i < techs.length; i++) {
            if (i == techIndex || techs[i] == null) continue;
            newTechs.add(techs[i]);
        }
        newTechs.add(tech);
        return new TechPool(newTechs);
    }

    public TechPool withSoftTechnologies(String softTechnologies) {
        Generic generic = getGeneric();
        assert generic != null;
        Map<TechFactory.Param,Object> emptyParams = Collections.emptyMap();
        TechPool newTechPool = this;
        Set<String> softTechNames = new HashSet<String>();
        for(String softTechFile: softTechnologies.split(";")) {
			if (softTechFile.length() == 0) continue;
        	URL url = TextUtils.makeURLToFile(softTechFile);
        	if (TextUtils.URLExists(url))
        	{
//	        	String softTechName = TextUtils.getFileNameWithoutExtension(url);
                TechFactory techFactory = TechFactory.fromXml(url, null);
                Technology tech = techFactory.newInstance(generic, emptyParams);

                // make sure the name is unique . Tech is null if there is a pre-defined xml tech with
                // the same nanme like mocmos.
                if (tech == null)
                {
                    System.out.println("ERROR: Same name as a pre-defined technology. Ignoring new definition of " + softTechFile);
                	continue;
                }
                else if (softTechNames.contains(tech.getTechName()))
                {
                	System.out.println("ERROR: Multiple added technologies named '" + tech.getTechName() +
                		"'.  Ignoring " + softTechFile);
                	continue;
                }
                softTechNames.add(tech.getTechName());

                if (tech != null)
                    newTechPool = newTechPool.withTech(tech);
        	} else
        	{
        		System.out.println("WARNING: could not find added technology: " + softTechFile);
        		System.out.println("  (fix this error in the 'Added Technologies' Project Preferences)");
        	}
        }
        return newTechPool;
    }

    /**
     * Get Technology by TechId
     * TechId must belong to same IdManager as TechPool
     * @param techId TechId to find
     * @return Technology by given TechId or null
     * @throws IllegalArgumentException if TechId is not from this IdManager
     */
    public Technology getTech(TechId techId) {
        if (techId.idManager != idManager) {
            throw new IllegalArgumentException();
        }
        int techIndex = techId.techIndex;
        return techIndex < techs.length ? techs[techIndex] : null;
    }

    /**
     * Find Technology by its name
     * @param techName name of technology
     * @return Technology by given name or null
     */
    public Technology findTechnology(String techName) {
        for (Technology tech: values()) {
            if (tech.getTechName().equals(techName))
                return tech;
        }
        return null;
    }

    /**
     * Get Layer by LayerId
     * LayerId must belong to same IdManager as TechPool
     * @param layerId LayerId to find
     * @return Layer by given LayerId or null
     * @throws IllegalArgumentException if TechId is not from this IdManager
     */
    public Layer getLayer(LayerId layerId) {
        Technology tech = getTech(layerId.techId);
        if (tech == null) return null;
        return tech.getLayerByChronIndex(layerId.chronIndex);
    }

    /**
     * Get ArcProto by ArcProtoId
     * ArcProtoId must belong to same IdManager as TechPool
     * @param arcProtoId ArcProtoId to find
     * @return ArcProto by given ArcProtoId or null
     * @throws IllegalArgumentException if TechId is not from this IdManager
     */
    public ArcProto getArcProto(ArcProtoId arcProtoId) {
        Technology tech = getTech(arcProtoId.techId);
        if (tech == null) return null;
        return tech.getArcProtoByChronIndex(arcProtoId.chronIndex);
    }

    /**
     * Get PrimitiveNode by PrimitiveNodeId
     * PrimitiveNodeId must belong to same IdManager as TechPool
     * @param primitiveNodeId PrimitiveNodeId to find
     * @return PrimitiveNode by given PrimitiveNodeId or null
     * @throws IllegalArgumentException if TechId is not from this IdManager
     */
    public PrimitiveNode getPrimitiveNode(PrimitiveNodeId primitiveNodeId) {
        Technology tech = getTech(primitiveNodeId.techId);
        if (tech == null) return null;
        return tech.getPrimitiveNodeByChronIndex(primitiveNodeId.chronIndex);
    }

    /**
     * Get PrimitivePort by PrimitivePortId
     * PrimitivePortId must belong to same IdManager as TechPool
     * @param primitivePortId PrimitivePortId to find
     * @return PrimitivePort by given PrimitivePortId or null
     * @throws IllegalArgumentException if TechId is not from this IdManager
     */
    public PrimitivePort getPrimitivePort(PrimitivePortId primitivePortId) {
        PrimitiveNode pn = getPrimitiveNode(primitivePortId.getParentId());
        if (pn == null) return null;
        return pn.getPrimitivePortByChronIndex(primitivePortId.chronIndex);
    }

    public void correctSizesToDisk(List<CellRevision> cells, Version version, Map<Setting,Object> projectSettings, boolean isJelib, boolean keepExtendOverMin) {
        HashMap<TechId,SizeCorrector> sizeCorrectors = new HashMap<TechId,SizeCorrector>();
        for (int i = 0; i < cells.size(); i++) {
            CellRevision cellRevision = cells.get(i);

            boolean needCorrection = false;
            for (TechId techId: cellRevision.getTechUsages()) {
                SizeCorrector sizeCorrector = sizeCorrectors.get(techId);
                if (sizeCorrector == null) {
                    if (!sizeCorrectors.containsKey(techId)) {
                        Technology tech = getTech(techId);
                        sizeCorrector = tech.getSizeCorrector(version, projectSettings, isJelib, keepExtendOverMin);
                        if (sizeCorrector.isIdentity())
                            sizeCorrector = null;
                        sizeCorrectors.put(techId, sizeCorrector);
                    }
                }
                if (sizeCorrector != null)
                    needCorrection = true;
            }
            if (!needCorrection) continue;

            ImmutableNodeInst[] correctedNodes = new ImmutableNodeInst[cellRevision.nodes.size()];
            for (int nodeIndex = 0; nodeIndex < correctedNodes.length; nodeIndex++) {
                ImmutableNodeInst n = cellRevision.nodes.get(nodeIndex);
                if (n.protoId instanceof PrimitiveNodeId) {
                    PrimitiveNodeId pnId = (PrimitiveNodeId)n.protoId;
                    SizeCorrector sizeCorrector = sizeCorrectors.get(pnId.techId);
                    if (sizeCorrector != null)
                        n = n.withSize(sizeCorrector.getSizeToDisk(n));
                }
                correctedNodes[nodeIndex] = n;
            }
            ImmutableArcInst[] correctedArcs = new ImmutableArcInst[cellRevision.arcs.size()];
            for (int arcIndex = 0; arcIndex < correctedArcs.length; arcIndex++) {
                ImmutableArcInst a = cellRevision.arcs.get(arcIndex);
                if (a == null) continue;
                ArcProtoId apId = a.protoId;
                SizeCorrector sizeCorrector = sizeCorrectors.get(apId.techId);
                if (sizeCorrector != null)
                    a = a.withGridExtendOverMin(sizeCorrector.getExtendToDisk(a));
                correctedArcs[arcIndex] = a;
            }

            cellRevision = cellRevision.with(cellRevision.d, correctedNodes, correctedArcs, null);
            cells.set(i, cellRevision);
        }
    }

    /** Returns Artwork technology in this database */
    public Artwork getArtwork() {
        return artwork;
    }

    /** Returns Generic technology in this database */
    public Generic getGeneric() {
        return generic;
    }

    /** Returns Schematic technology in this database */
    public Schematics getSchematics() {
        return schematics;
    }

    /**
     * Tests that two TechPools contains the same set of Tehnologies
     * @param that second TechPool
     * @return true if this and that TechPools are equal
     */
    public boolean equals(TechPool that) {
        if (idManager != that.idManager || techs.length != that.techs.length) {
            return false;
        }
        for (int techIndex = 0; techIndex < techs.length; techIndex++) {
            if (techs[techIndex] != that.techs[techIndex]) {
                return false;
            }
        }
        return true;
    }

    // Implementation of Map<TechId,Technology>
    @Override
    public boolean containsKey(Object key) {
        int techIndex = ((TechId) key).techIndex;
        if (techIndex >= techs.length) {
            return false;
        }
        Technology tech = techs[techIndex];
        return tech != null && tech.getId() == key;
    }

    @Override
    public boolean containsValue(Object value) {
        int techIndex = ((Technology) value).getId().techIndex;
        return techIndex < techs.length && techs[techIndex] == value;
    }

    @Override
    public Technology get(Object key) {
        int techIndex = ((TechId) key).techIndex;
        if (techIndex >= techs.length) {
            return null;
        }
        Technology tech = techs[techIndex];
        return tech != null && tech.getId() == key ? tech : null;
    }

    @Override
    public Set<Map.Entry<TechId, Technology>> entrySet() {
        return entrySet;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TechPool ? equals((TechPool) o) : super.equals(o);
    }

    /**
     * Writes this TechPool to IdWriter
     * @param writer IdWriter
     * @throws java.io.IOException
     */
    public void writeDiff(IdWriter writer, TechPool old) throws IOException {
        boolean isEmpty = isEmpty();
        writer.writeBoolean(isEmpty);
        if (isEmpty) return;
        writer.writeDiffs();
        Generic generic = getGeneric();
        assert generic != null;
        boolean newGeneric = generic != old.getGeneric();
        writer.writeBoolean(newGeneric);
        for (Technology tech : values()) {
            if (tech == generic) continue;
            TechId techId = tech.getId();
            writer.writeInt(techId.techIndex);
            boolean sameTech = tech == old.getTech(techId);
            writer.writeBoolean(sameTech);
            if (sameTech) continue;
            assert tech != old.getTech(techId);
            tech.techFactory.write(writer);
            for (TechFactory.Param techParam: tech.techFactory.getTechParams()) {
                Object value = tech.getParamValues().get(techParam);
                writer.writeString(techParam.xmlPath);
                Variable.writeObject(writer, value);
            }
        }
        writer.writeInt(-1);
    }

    /**
     * Reads TechPool from IdReader
     * @param reader IdReader
     * @return TechPool read
     * @throws java.io.IOException
     */
    public static TechPool read(IdReader reader, TechPool old) throws IOException {
        boolean isEmpty = reader.readBoolean();
        if (isEmpty)
            return new TechPool(reader.idManager);
        reader.readDiffs();
        ArrayList<Technology> technologiesList = new ArrayList<Technology>();
        boolean newGeneric = reader.readBoolean();
        Generic generic = newGeneric ? Generic.newInstance(reader.idManager) : old.getGeneric();
        technologiesList.add(generic);
        for (;;) {
            int techIndex = reader.readInt();
            if (techIndex == -1)
                break;
            TechId techId = reader.idManager.getTechId(techIndex);
            assert techId != null;
            boolean sameTech = reader.readBoolean();
            if (sameTech) {
                technologiesList.add(old.getTech(techId));
                continue;
            }
            TechFactory techFactory = TechFactory.read(reader);
            HashMap<TechFactory.Param,Object> paramValues = new HashMap<TechFactory.Param,Object>();
            for (TechFactory.Param techParam: techFactory.getTechParams()) {
                String xmlPath = reader.readString();
                assert xmlPath.equals(techParam.xmlPath);
                Object value = Variable.readObject(reader);
                paramValues.put(techParam, value);
            }
            Technology newTech = techFactory.newInstance(generic, paramValues);
            technologiesList.add(newTech);
        }
        return new TechPool(technologiesList);
    }

    /**
     * the connecitivity list for universal and invisible pins
     */
    ArcProto[] getUnivList() {
        if (univList == null)
            makeUnivList();
        return univList;
    }

    private void makeUnivList() {
        // prepare connectivity list for universal and invisible pins
        int univListCount = 0;
        for (Technology tech : techs) {
            if (tech != null) {
                univListCount += tech.getNumArcs();
            }
        }
        univList = new ArcProto[univListCount];
        univListCount = 0;
        for (Technology tech : techs) {
            if (tech == null) {
                continue;
            }
            for (Iterator<ArcProto> ait = tech.getArcs(); ait.hasNext();) {
                univList[univListCount++] = ait.next();
            }
        }
        assert univListCount == univList.length;
    }

	/**
	 * Checks invariants in this TechPool.
     * @exception AssertionError if invariants are not valid
	 */
    public void check() {
        int size = 0;
        int arcCount = 0;
        for (int techIndex = 0; techIndex < techs.length; techIndex++) {
            Technology tech = techs[techIndex];
            if (tech == null) {
                continue;
            }
            size++;
            TechId techId = tech.getId();
            assert techId.idManager == idManager;
            assert techId.techIndex == techIndex;
            assert idManager.getTechId(techIndex) == techId;
            assert get(techId) == tech;
            assert containsKey(techId);
            assert containsValue(tech);
            if (univList != null) {
                for (Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); ) {
                    ArcProto ap = it.next();
                    assert univList[arcCount++] == ap;
                }
            }
        }
        assert size == size();
        if (size != 0)
            assert getGeneric() != null;
        assert size == 0 || techs[techs.length - 1] != null;
        if (univList != null)
            assert arcCount == univList.length;
        TechId prevTechId = null;
        for (Entry<TechId,Technology> e: entrySet()) {
            assert entrySet.contains(e);
            TechId techId = e.getKey();
            Technology tech = e.getValue();
            assert techId == tech.getId();
            assert techs[techId.techIndex] == tech;
            if (prevTechId != null)
                assert TextUtils.STRING_NUMBER_ORDER.compare(prevTechId.techName, techId.techName) < 0;
            prevTechId = techId;
        }
    }

    private class EntrySet extends AbstractSet<Map.Entry<TechId, Technology>> {
        private final TechEntry[] entries;

        EntrySet() {
            TreeSet<Technology> sortedTechs = new TreeSet<Technology>();
            for (Technology tech : techs) {
                if (tech != null)
                    sortedTechs.add(tech);
            }
            entries = new TechEntry[sortedTechs.size()];
            int i = 0;
            for (Technology tech: sortedTechs) {
                TechId techId = tech.getId();
                entries[i++] = new TechEntry(techId, tech);
            }
        }

        @Override
        public int size() {
            return entries.length;
        }

        @Override
        public boolean contains(Object o) {
            Entry e = (Entry) o;
            Technology tech = (Technology) e.getValue();
            return containsValue(tech) && e.getKey().equals(tech.getId());
        }

        @Override
        public Iterator<Entry<TechId, Technology>> iterator() {
            return ArrayIterator.<Map.Entry<TechId,Technology>>iterator(entries);
        }
    }

    private static class TechEntry implements Map.Entry<TechId,Technology> {
        private final TechId key;
        private final Technology value;

    	public TechEntry(TechId key, Technology value) {
            if (key == null || value == null)
                throw new NullPointerException();
            this.key   = key;
            this.value = value;
        }

        public TechId getKey() { return key; }
    	public Technology getValue() { return value; }
        public Technology setValue(Technology value) { throw new UnsupportedOperationException(); }
        @Override public int hashCode() { return key.hashCode() ^ value.hashCode(); }
        @Override public String toString() { return key + "=" + value; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry)o;
            return key.equals(e.getKey()) && value.equals(e.getValue());
        }
    }

    /**
     * Returns thread-local TechPool
     * @return thread-local TechPool
     */

    public static TechPool getThreadTechPool() {
        return Environment.getThreadEnvironment().techPool;
    }
}
