/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableArcInst.java
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
package com.sun.electric.technology;

import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.database.id.TechId;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A customized Map from TechId to Technolgy.
 * All TechIds must belong to same IdManager.
 */
public class TechPool extends AbstractMap<TechId, Technology> {
//    public static final TechPool EMPTY = new TechPool(Collections.<Technology>emptySet());
    public final IdManager idManager;
    private final Technology[] techs;
    private final ArcProto[] univList;
    private final EntrySet entrySet;
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
        univList = new ArcProto[0];
    }

    /**
     * Constructs TechPool from Collection of technologies
     * All technologies must belong to the same IdManager
     * @param technologies Collection of technologies in the Pool
     */
    public TechPool(Collection<Technology> technologies) {
        Iterator<Technology> it = technologies.iterator();
        TechId techId = it.next().getId();
        idManager = techId.idManager;
        int maxTechIndex = techId.techIndex;
        while (it.hasNext()) {
            techId = it.next().getId();
            if (techId.idManager != idManager) {
                throw new IllegalArgumentException();
            }
            maxTechIndex = Math.max(maxTechIndex, techId.techIndex);
        }
        techs = new Technology[maxTechIndex + 1];
        for (Technology tech : technologies) {
            techId = tech.getId();
            int techIndex = techId.techIndex;
            if (techs[techIndex] != null) {
                throw new IllegalArgumentException();
            }
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
        HashMap<TechId, Technology> newMap = new HashMap<TechId, Technology>(this);
        newMap.put(techId, tech);
        return new TechPool(newMap.values());
    }

    /**
     * Get Technology by TechId
     * TechId must belong to same IdManager as TechPool
     * @param techId TechId to find
     * @return Technology b giben TechId or null
     * @throws IllegalArgumentException of TechId is not from this IdManager
     */
    public Technology getTech(TechId techId) {
        if (techId.idManager != idManager) {
            throw new IllegalArgumentException();
        }
        int techIndex = techId.techIndex;
        return techIndex < techs.length ? techs[techIndex] : null;
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
    public void write(IdWriter writer) throws IOException {
        for (Technology tech : values()) {
            writer.writeInt(tech.getId().techIndex);
        }
        writer.writeInt(-1);
    }

    /**
     * Reads TechPool from IdReader
     * @param reader IdReader
     * @return TechPool read
     * @throws java.io.IOException
     */
    public static TechPool read(IdReader reader) throws IOException {
        ArrayList<Technology> technologiesList = new ArrayList<Technology>();
        for (;;) {
            int techIndex = reader.readInt();
            if (techIndex == -1) {
                break;
            }
            TechId techId = reader.idManager.getTechId(techIndex);
            assert techId != null;
            Technology tech = Technology.findTechnology(techId);
            assert tech != null;
            technologiesList.add(tech);
        }
        return new TechPool(technologiesList);
    }

    /**
     * the connecitivity list for universal and invisible pins
     */
    ArcProto[] getUnivList() {
        return univList;
    }

    private class EntrySet extends AbstractSet<Entry<TechId, Technology>> {

        private final ArrayList<AbstractMap.SimpleImmutableEntry<TechId, Technology>> entries =
                new ArrayList<AbstractMap.SimpleImmutableEntry<TechId, Technology>>();

        EntrySet() {
            for (Technology tech : techs) {
                if (tech == null) {
                    continue;
                }
                TechId techId = tech.getId();
                entries.add(new AbstractMap.SimpleImmutableEntry<TechId, Technology>(techId, tech));
            }
        }

        @Override
        public int size() {
            return entries.size();
        }

        @Override
        public boolean contains(Object o) {
            Entry e = (Entry) o;
            Technology tech = (Technology) e.getValue();
            return containsValue(tech) && e.getKey().equals(tech.getId());
        }

        @Override
        public Iterator<Map.Entry<TechId, Technology>> iterator() {
            return new Iterator<Entry<TechId, Technology>>() {

                private int cursor = 0;

                @Override
                public boolean hasNext() {
                    return cursor < entries.size();
                }

                @Override
                public Entry<TechId, Technology> next() {
                    if (cursor >= entries.size()) {
                        throw new NoSuchElementException();
                    }
                    return entries.get(cursor++);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
