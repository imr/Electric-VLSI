/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayerId.java
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
package com.sun.electric.database.id;

import com.sun.electric.database.EObjectInputStream;
import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.hierarchy.EDatabase;

import com.sun.electric.technology.Layer;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;

/**
 * The LayerId immutable class identifies layer independently of threads.
 * It differs from Layer objects, which will be owned by threads in transactional database.
 * This class is thread-safe except inCurrentThread method.
 */
public class LayerId implements Serializable {

    /** TechId of this LayerId. */
    public final TechId techId;
    /** Layer name */
    public final String name;
    /** Layer full name */
    public final String fullName;
    /** Unique index of this LayerId in TechId. */
    public final int chronIndex;

    /**
     * LayerId constructor.
     */
    LayerId(TechId techId, String name, int chronIndex) {
        assert techId != null;
        if (name.length() == 0 || !TechId.jelibSafeName(name)) {
            throw new IllegalArgumentException("LayerId.name");
        }
        this.techId = techId;
        this.name = name;
        fullName = techId.techName + ":" + name;
        this.chronIndex = chronIndex;
    }

    private Object writeReplace() {
        return new LayerIdKey(this);
    }

    private static class LayerIdKey extends EObjectInputStream.Key<LayerId> {

        public LayerIdKey() {
        }

        private LayerIdKey(LayerId layerId) {
            super(layerId);
        }

        @Override
        public void writeExternal(EObjectOutputStream out, LayerId layerId) throws IOException {
            TechId techId = layerId.techId;
            if (techId.idManager != out.getIdManager()) {
                throw new NotSerializableException(layerId + " from other IdManager");
            }
            out.writeInt(techId.techIndex);
            out.writeInt(layerId.chronIndex);
        }

        @Override
        public LayerId readExternal(EObjectInputStream in) throws IOException, ClassNotFoundException {
            int techIndex = in.readInt();
            int chronIndex = in.readInt();
            return in.getIdManager().getTechId(techIndex).getLayerId(chronIndex);
        }
    }

    /**
     * Method to return the Layer representing LayerId in the specified EDatabase.
     * @param database EDatabase where to get from.
     * @return the Layer representing LayerId in the specified database.
     * This method is not properly synchronized.
     */
    public Layer inDatabase(EDatabase database) {
        return database.getTechPool().getLayer(this);
    }

    /**
     * Returns a printable version of this LayerId.
     * @return a printable version of this LayerId.
     */
    @Override
    public String toString() {
        return fullName;
    }

    /**
     * Checks invariants in this LayerId.
     * @exception AssertionError if invariants are not valid
     */
    void check() {
        assert this == techId.getLayerId(chronIndex);
        assert name.length() > 0 && TechId.jelibSafeName(name);
    }
}
