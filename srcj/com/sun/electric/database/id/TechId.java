/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TechId.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.id;

import com.sun.electric.database.EObjectInputStream;
import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.technology.Technology;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The TechId immutable class identifies technology independently of threads.
 * It differs from Technology objects, which will be owned by threads in transactional database.
 * This class is thread-safe except inCurrentThread method.
 */
public final class TechId implements Serializable {

    /** Empty TechId array for initialization. */
    public static final TechId[] NULL_ARRAY = {};
    /** IdManager which owns this TechId. */
    public final IdManager idManager;
    /** Technology name */
    public final String techName;
    /** Unique index of this TechId. */
    public final int techIndex;
    /** List of LayerIds created so far. */
    final ArrayList<LayerId> layerIds = new ArrayList<LayerId>();
    /** HashMap of LayerIds by their name. */
    private final HashMap<String, LayerId> layerIdsByName = new HashMap<String, LayerId>();
    /** List of ArcProtoIds created so far. */
    final ArrayList<ArcProtoId> arcProtoIds = new ArrayList<ArcProtoId>();
    /** HashMap of ArcProtoIds by their name. */
    private final HashMap<String, ArcProtoId> arcProtoIdsByName = new HashMap<String, ArcProtoId>();
    /** List of PrimitiveNodeIds created so far. */
    final ArrayList<PrimitiveNodeId> primitiveNodeIds = new ArrayList<PrimitiveNodeId>();
    /** HashMap of PrimitiveNodeIds by their name. */
    private final HashMap<String, PrimitiveNodeId> primitiveNodeIdsByName = new HashMap<String, PrimitiveNodeId>();
    /**
     * Variable which is incremented every time when ArcProtoId, PrimitiveNodeId or PrimitiveNodeId is
     * created below this TechId
     */
    volatile int modCount;

    /**
     * TechId constructor.
     */
    TechId(IdManager idManager, String techName, int techIndex) {
        if (techName == null) {
            throw new NullPointerException();
        }
        if (techName.length() == 0 || !jelibSafeName(techName)) {
            throw new IllegalArgumentException(techName);
        }
        this.idManager = idManager;
        this.techName = techName;
        this.techIndex = techIndex;
    }

    private Object writeReplace() {
        return new TechIdKey(this);
    }

    private static class TechIdKey extends EObjectInputStream.Key<TechId> {

        public TechIdKey() {
        }

        private TechIdKey(TechId techId) {
            super(techId);
        }

        @Override
        public void writeExternal(EObjectOutputStream out, TechId techId) throws IOException {
            if (techId.idManager != out.getIdManager()) {
                throw new NotSerializableException(techId + " from other IdManager");
            }
            out.writeInt(techId.techIndex);
        }

        @Override
        public TechId readExternal(EObjectInputStream in) throws IOException, ClassNotFoundException {
            int techIndex = in.readInt();
            return in.getIdManager().getTechId(techIndex);
        }
    }

    /**
     * Returns a number LayerIds in this TechId.
     * This number may grow in time.
     * @return a number of LayerIds.
     */
    synchronized int numLayerIds() {
        return layerIds.size();
    }

    /**
     * Returns LayerId in this TechId with specified chronological index.
     * @param chronIndex chronological index of LayerId.
     * @return LayerId with specified chronological index.
     * @throws ArrayIndexOutOfBoundsException if no such ArcProtoId.
     */
    synchronized LayerId getLayerId(int chronIndex) {
        return layerIds.get(chronIndex);
    }

    /**
     * Returns LayerId with specified layerName.
     * @param layerName layer name.
     * @return LayerId with specified layerName.
     */
    public synchronized LayerId newLayerId(String layerName) {
        LayerId layerId = layerIdsByName.get(layerName);
        if (layerId != null) {
            return layerId;
        }
        assert !idManager.readOnly;
        return newLayerIdInternal(layerName);
    }

    LayerId newLayerIdInternal(String layerName) {
        int chronIndex = layerIds.size();
        LayerId layerId = new LayerId(this, layerName, layerIds.size());
        layerIds.add(layerId);
        layerIdsByName.put(layerName, layerId);
        assert layerIds.size() == layerIdsByName.size();
        modCount++;
        return layerId;
    }

    /**
     * Returns a number ArcProtoIds in this TechId.
     * This number may grow in time.
     * @return a number of ArcProtoIds.
     */
    synchronized int numArcProtoIds() {
        return arcProtoIds.size();
    }

    /**
     * Returns ArcProtoId in this TechId with specified chronological index.
     * @param chronIndex chronological index of ArcProtoId.
     * @return ArcProtoId with specified chronological index.
     * @throws ArrayIndexOutOfBoundsException if no such ArcProtoId.
     */
    synchronized ArcProtoId getArcProtoId(int chronIndex) {
        return arcProtoIds.get(chronIndex);
    }

    /**
     * Returns ArcProtoId with specified arcProtoName.
     * @param arcProtoName arc proto name.
     * @return ArcProtoId with specified arcProtoName.
     */
    public synchronized ArcProtoId newArcProtoId(String arcProtoName) {
        ArcProtoId arcProtoId = arcProtoIdsByName.get(arcProtoName);
        if (arcProtoId != null) {
            return arcProtoId;
        }
        assert !idManager.readOnly;
        return newArcProtoIdInternal(arcProtoName);
    }

    ArcProtoId newArcProtoIdInternal(String arcProtoName) {
        int chronIndex = arcProtoIds.size();
        ArcProtoId arcProtoId = new ArcProtoId(this, arcProtoName, arcProtoIds.size());
        arcProtoIds.add(arcProtoId);
        arcProtoIdsByName.put(arcProtoName, arcProtoId);
        assert arcProtoIds.size() == arcProtoIdsByName.size();
        modCount++;
        return arcProtoId;
    }

    /**
     * Returns a number PrimitiveNodeIds in this TechId.
     * This number may grow in time.
     * @return a number of PrimitiveNodeIds.
     */
    synchronized int numPrimitiveNodeIds() {
        return primitiveNodeIds.size();
    }

    /**
     * Returns PrimitiveNodeId in this TechId with specified chronological index.
     * @param chronIndex chronological index of PrimitiveNodeId.
     * @return PrimitiveNodeId with specified chronological index.
     * @throws ArrayIndexOutOfBoundsException if no such PrimitiveNodeId.
     */
    synchronized PrimitiveNodeId getPrimitiveNodeId(int chronIndex) {
        return primitiveNodeIds.get(chronIndex);
    }

    /**
     * Returns PrimitiveNodeId with specified primitiveNodeName.
     * @param primitiveNodeName primitive node name.
     * @return PrimitiveNodeId with specified primitiveNodeName.
     */
    public synchronized PrimitiveNodeId newPrimitiveNodeId(String primitiveNodeName) {
        PrimitiveNodeId primitiveNodeId = primitiveNodeIdsByName.get(primitiveNodeName);
        if (primitiveNodeId != null) {
            return primitiveNodeId;
        }
        assert !idManager.readOnly;
        return newPrimitiveNodeIdInternal(primitiveNodeName);
    }

    PrimitiveNodeId newPrimitiveNodeIdInternal(String primitiveNodeName) {
        int chronIndex = primitiveNodeIds.size();
        PrimitiveNodeId primitiveNodeId = new PrimitiveNodeId(this, primitiveNodeName, primitiveNodeIds.size());
        primitiveNodeIds.add(primitiveNodeId);
        primitiveNodeIdsByName.put(primitiveNodeName, primitiveNodeId);
        assert primitiveNodeIds.size() == primitiveNodeIdsByName.size();
        modCount++;
        return primitiveNodeId;
    }

    /**
     * Method to return the Technology representing TechId in the specified EDatabase.
     * @param database EDatabase where to get from.
     * @return the Technology representing TechId in the specified database.
     * This method is not properly synchronized.
     */
    public Technology inDatabase(EDatabase database) {
        return database.getTech(this);
    }

    /**
     * Returns a printable version of this TechId.
     * @return a printable version of this TechId.
     */
    public String toString() {
        return techName;
    }

    /**
     * Checks invariants in this TechId.
     * @exception AssertionError if invariants are not valid
     */
    void check() {
        assert this == idManager.getTechId(techIndex);
        assert techName.length() > 0 && jelibSafeName(techName);
        for (Map.Entry<String, ArcProtoId> e : arcProtoIdsByName.entrySet()) {
            ArcProtoId arcProtoId = e.getValue();
            assert arcProtoId.techId == this;
            assert arcProtoId.name == e.getKey();
            arcProtoId.check();
        }
        for (int chronIndex = 0; chronIndex < arcProtoIds.size(); chronIndex++) {
            ArcProtoId arcProtoId = arcProtoIds.get(chronIndex);
            arcProtoId.check();
            assert arcProtoIdsByName.get(arcProtoId.name) == arcProtoId;
        }

        for (Map.Entry<String, PrimitiveNodeId> e : primitiveNodeIdsByName.entrySet()) {
            PrimitiveNodeId primitiveNodeId = e.getValue();
            assert primitiveNodeId.techId == this;
            assert primitiveNodeId.name == e.getKey();
            primitiveNodeId.check();
        }
        for (int chronIndex = 0; chronIndex < primitiveNodeIds.size(); chronIndex++) {
            PrimitiveNodeId primitiveNodeId = primitiveNodeIds.get(chronIndex);
            primitiveNodeId.check();
            assert primitiveNodeIdsByName.get(primitiveNodeId.name) == primitiveNodeId;
        }
    }

    /**
     * Method checks that string is safe to write into JELIB file without
     * conversion.
     * @param str the string to check.
     * @return true if string is safe to write into JELIB file.
     */
    public static boolean jelibSafeName(String str) {
        return jelibSafeName(str, false);
    }

    /**
     * Method checks that string is safe to write into JELIB file without
     * conversion.
     * @param str the string to check.
     * @param allowSpace exemption for space char
     * @return true if string is safe to write into JELIB file.
     */
    static boolean jelibSafeName(String str, boolean allowSpace) {
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch == ':' || ch == '|' || ch == '^' || ch == '\\' || ch == '"') {
                return false;
            }
            if (Character.isWhitespace(ch) && !(allowSpace && ch == ' ')) {
                return false;
            }
        }
        return true;
    }
}
