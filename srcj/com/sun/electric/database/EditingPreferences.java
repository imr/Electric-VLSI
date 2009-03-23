/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EditingPreferences.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.id.ArcProtoId;
import com.sun.electric.database.id.PrimitiveNodeId;

import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.PrefPackage;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Class to mirror on a server a portion of client environment
 */
public class EditingPreferences extends PrefPackage {
    private static final ThreadLocal<EditingPreferences> threadEditingPreferences = new ThreadLocal<EditingPreferences>();

    private final TechPool techPool;
    private HashMap<PrimitiveNodeId,ImmutableNodeInst> defaultNodes = new HashMap<PrimitiveNodeId,ImmutableNodeInst>();
    private HashMap<ArcProtoId,ImmutableArcInst> defaultArcs = new HashMap<ArcProtoId,ImmutableArcInst>();

    private EditingPreferences(TechPool techPool, Map<PrimitiveNodeId,ImmutableNodeInst> defaultNodes) {
        super("");
        this.techPool = techPool;
        this.defaultNodes.putAll(defaultNodes);
    }

    public EditingPreferences(boolean factory, TechPool techPool) {
        super(factory);
        this.techPool = techPool;
        if (factory) return;

        Preferences techPrefs = Pref.getTechnologyPreferences();
        for (Technology tech: techPool.values()) {
            for (Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); ) {
                PrimitiveNode pn = it.next();
                PrimitiveNodeId pnId = pn.getId();
                String keyX = getKey("DefaultExtendX", pnId);
                String keyY = getKey("DefaultExtendY", pnId);
                ImmutableNodeInst factoryInst = pn.getFactoryDefaultInst();
                EPoint factorySize = factoryInst.size;
                double factoryExtendX = factorySize.getLambdaX()*0.5;
                double factoryExtendY = factorySize.getLambdaY()*0.5;
                double extendX = techPrefs.getDouble(keyX, factoryExtendX);
                double extendY = techPrefs.getDouble(keyY, factoryExtendY);
                if (extendX == factoryExtendX && extendY == factoryExtendY) continue;
                EPoint size = EPoint.fromLambda(extendX*2, extendY*2);
                defaultNodes.put(pnId, factoryInst.withAnchor(size));
            }
        }
    }

    @Override
    public void putPrefs(Preferences prefRoot, boolean removeDefaults) {
        super.putPrefs(prefRoot, removeDefaults);

        Preferences techPrefs = Pref.getTechnologyPreferences(prefRoot);
        for (Technology tech: techPool.values()) {
            for (Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); ) {
                PrimitiveNode pn = it.next();
                PrimitiveNodeId pnId = pn.getId();
                String keyX = getKey("DefaultExtendX", pnId);
                String keyY = getKey("DefaultExtendY", pnId);
                EPoint factorySize = pn.getFactoryDefaultInst().size;
                ImmutableNodeInst n = defaultNodes.get(pnId);
                EPoint size = n != null ? n.size : factorySize;
                if (removeDefaults && size.getGridX() == factorySize.getGridX())
                    techPrefs.remove(keyX);
                else
                    techPrefs.putDouble(keyX, size.getLambdaX()*0.5);
                if (removeDefaults && size.getGridY() == factorySize.getGridY())
                    techPrefs.remove(keyY);
                else
                    techPrefs.putDouble(keyY, size.getLambdaY()*0.5);
            }
        }
    }

    public EditingPreferences withDefaultNodes(Map<PrimitiveNodeId,ImmutableNodeInst> defaultNodes) {
        if (this.defaultNodes.equals(defaultNodes)) return this;
        return new EditingPreferences(techPool, defaultNodes);
    }

    public ImmutableNodeInst getDefaultNode(PrimitiveNodeId pnId) {
        return defaultNodes.get(pnId);
    }

    public static EditingPreferences getThreadEditingPreferences() {
        return threadEditingPreferences.get();
    }

    public static EditingPreferences setThreadEditingPreferences(EditingPreferences ep) {
        EditingPreferences oldEp = threadEditingPreferences.get();
        threadEditingPreferences.set(ep);
        return oldEp;
    }

    private String getKey(String what, PrimitiveNodeId pnId) {
        int len = what.length() + pnId.fullName.length() + 4;
        StringBuilder sb = new StringBuilder(len);
        sb.append(what);
        sb.append("For");
        sb.append(pnId.name);
        sb.append("IN");
        sb.append(pnId.techId.techName);
        assert sb.length() == len;
        return sb.toString();
    }
}
