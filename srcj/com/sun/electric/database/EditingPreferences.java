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

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.id.ArcProtoId;
import com.sun.electric.database.id.PrimitiveNodeId;

import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.PrefPackage;
import com.sun.electric.technology.ArcProto;
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
    // In TECH_NODE
    private static final String KEY_EXTEND_X = "DefaultExtendX";
    private static final String KEY_EXTEND_Y = "DefaultExtendY";
    private static final String KEY_EXTEND = "DefaultExtend";
    private static final String KEY_PIN = "Pin";
    // In USER_NODE
    private static final String KEY_RIGID = "DefaultRigid";
    private static final String KEY_FIXED_ANGLE = "DefaultFixedAngle";
    private static final String KEY_SLIDABLE = "DefaultSlidable";
    private static final String KEY_EXTENDED = "DefaultExtended";
    private static final String KEY_DIRECTIONAL = "DefaultDirectional";
	private static final String KEY_ANGLE = "DefaultAngle";

    private static final ThreadLocal<EditingPreferences> threadEditingPreferences = new ThreadLocal<EditingPreferences>();

    private final TechPool techPool;
    private HashMap<PrimitiveNodeId,ImmutableNodeInst> defaultNodes = new HashMap<PrimitiveNodeId,ImmutableNodeInst>();
    private HashMap<ArcProtoId,ImmutableArcInst> defaultArcs = new HashMap<ArcProtoId,ImmutableArcInst>();
    private HashMap<ArcProtoId,Integer> defaultArcAngleIncrements = new HashMap<ArcProtoId,Integer>();
    private HashMap<ArcProtoId,PrimitiveNodeId> defaultArcPins = new HashMap<ArcProtoId,PrimitiveNodeId>();

    private EditingPreferences(TechPool techPool,
            Map<PrimitiveNodeId,ImmutableNodeInst> defaultNodes,
            Map<ArcProtoId,ImmutableArcInst> defaultArcs,
            Map<ArcProtoId,Integer> defaultArcAngleIncrements,
            Map<ArcProtoId,PrimitiveNodeId> defaultArcPins) {
        super("");
        this.techPool = techPool;
        this.defaultNodes.putAll(defaultNodes);
        this.defaultArcs.putAll(defaultArcs);
        this.defaultArcAngleIncrements.putAll(defaultArcAngleIncrements);
        this.defaultArcPins.putAll(defaultArcPins);
    }

    public EditingPreferences(boolean factory, TechPool techPool) {
        super(factory);
        this.techPool = techPool;
        if (factory) return;

        Preferences prefRoot = Pref.getPrefRoot();
        Preferences techPrefs = prefRoot.node(TECH_NODE);
        Preferences userPrefs = prefRoot.node(USER_NODE);
        for (Technology tech: techPool.values()) {
            for (Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); ) {
                PrimitiveNode pn = it.next();
                PrimitiveNodeId pnId = pn.getId();
                ImmutableNodeInst factoryInst = pn.getFactoryDefaultInst();
                EPoint factorySize = factoryInst.size;

                // TECH_NODE
                String keyExtendX = getKey(KEY_EXTEND_X, pnId);
                double factoryExtendX = factorySize.getLambdaX()*0.5;
                double extendX = techPrefs.getDouble(keyExtendX, factoryExtendX);

                String keyExtendY = getKey(KEY_EXTEND_Y, pnId);
                double factoryExtendY = factorySize.getLambdaY()*0.5;
                double extendY = techPrefs.getDouble(keyExtendY, factoryExtendY);

                if (extendX == factoryExtendX && extendY == factoryExtendY) continue;
                EPoint size = EPoint.fromLambda(extendX*2, extendY*2);
                defaultNodes.put(pnId, factoryInst.withSize(size));
            }

            for (Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); ) {
                ArcProto ap = it.next();
                ArcProtoId apId = ap.getId();
                ImmutableArcInst factoryInst = ap.getFactoryDefaultInst();
                int flags = factoryInst.flags;

                // TECH_NODE
                String keyExtend = getKey(KEY_EXTEND, apId);
                double factoryExtend = factoryInst.getLambdaExtendOverMin();
                double extend = techPrefs.getDouble(keyExtend, factoryExtend);

                // USER_NODE
                String keyRigid = getKey(KEY_RIGID, apId);
                boolean factoryRigid = factoryInst.isRigid();
                boolean rigid = userPrefs.getBoolean(keyRigid, factoryRigid);
                flags = ImmutableArcInst.RIGID.set(flags, rigid);

                String keyFixedAngle = getKey(KEY_FIXED_ANGLE, apId);
                boolean factoryFixedAngle = factoryInst.isFixedAngle();
                boolean fixedAngle = userPrefs.getBoolean(keyFixedAngle, factoryFixedAngle);
                flags = ImmutableArcInst.FIXED_ANGLE.set(flags, fixedAngle);

                String keySlidable = getKey(KEY_SLIDABLE, apId);
                boolean factorySlidable = factoryInst.isSlidable();
                boolean slidable = userPrefs.getBoolean(keySlidable, factorySlidable);
                flags = ImmutableArcInst.SLIDABLE.set(flags, slidable);

                String keyExtended = getKey(KEY_EXTENDED, apId);
                boolean factoryExtended = factoryInst.isTailExtended();
                assert factoryExtended == factoryInst.isHeadExtended();
                boolean extended = userPrefs.getBoolean(keyExtended, factoryExtended);
                flags = ImmutableArcInst.TAIL_EXTENDED.set(flags, extended);
                flags = ImmutableArcInst.HEAD_EXTENDED.set(flags, extended);

                String keyDirectional = getKey(KEY_DIRECTIONAL, apId);
                boolean factoryDirectional = factoryInst.isHeadArrowed();
                assert factoryDirectional == factoryInst.isBodyArrowed();
                boolean directional = userPrefs.getBoolean(keyDirectional, factoryDirectional);
                flags = ImmutableArcInst.HEAD_ARROWED.set(flags, directional);
                flags = ImmutableArcInst.BODY_ARROWED.set(flags, directional);

                ImmutableArcInst a = factoryInst.
                        withGridExtendOverMin(DBMath.lambdaToGrid(extend)).
                        withFlags(flags);
                if (a != factoryInst)
                    defaultArcs.put(apId, a);

                String keyAngle = getKey(KEY_ANGLE, apId);
                int factoryAngleIncrement = ap.getFactoryAngleIncrement();
                int angleIncrement = userPrefs.getInt(keyAngle, factoryAngleIncrement);
                if (angleIncrement != factoryAngleIncrement)
                    defaultArcAngleIncrements.put(apId, Integer.valueOf(angleIncrement));

                String keyPin = getKey(KEY_PIN, apId);
                String arcPinName = techPrefs.get(keyPin, "");
                if (arcPinName.length() > 0) {
                    PrimitiveNode pin = tech.findNodeProto(arcPinName);
                    if (pin != null)
                        defaultArcPins.put(apId, pin.getId());
                }
            }
        }
    }

    @Override
    public void putPrefs(Preferences prefRoot, boolean removeDefaults) {
        putPrefs(prefRoot, removeDefaults, null);
    }

    public void putPrefs(Preferences prefRoot, boolean removeDefaults, EditingPreferences oldEp) {
        super.putPrefs(prefRoot, removeDefaults);

        if (oldEp != null && oldEp.techPool != techPool)
            oldEp = null;

        Preferences techPrefs = prefRoot.node(TECH_NODE);
        Preferences userPrefs = prefRoot.node(USER_NODE);
        for (Technology tech: techPool.values()) {
            for (Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); ) {
                PrimitiveNode pn = it.next();
                PrimitiveNodeId pnId = pn.getId();
                ImmutableNodeInst n = defaultNodes.get(pnId);
                if (oldEp == null || n != oldEp.defaultNodes.get(pnId)) {
                    ImmutableNodeInst factoryInst = pn.getFactoryDefaultInst();
                    if (n == null)
                        n = factoryInst;

                    String keyX = getKey(KEY_EXTEND_X, pnId);
                    if (removeDefaults && n.size.getGridX() == factoryInst.size.getGridX())
                        techPrefs.remove(keyX);
                    else
                        techPrefs.putDouble(keyX, n.size.getLambdaX()*0.5);

                    String keyY = getKey(KEY_EXTEND_Y, pnId);
                    if (removeDefaults && n.size.getGridY() == factoryInst.size.getGridY())
                        techPrefs.remove(keyY);
                    else
                        techPrefs.putDouble(keyY, n.size.getLambdaY()*0.5);
                }
            }

            for (Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); ) {
                ArcProto ap = it.next();
                ArcProtoId apId = ap.getId();
                ImmutableArcInst a = defaultArcs.get(apId);
                if (oldEp == null || a != oldEp.defaultArcs.get(ap)) {
                    ImmutableArcInst factoryInst = ap.getFactoryDefaultInst();
                    if (a == null)
                        a = factoryInst;

                    // TECH_NODE
                    String keyExtend = getKey(KEY_EXTEND, apId);
                    if (removeDefaults && a.getGridExtendOverMin() == factoryInst.getGridExtendOverMin())
                        techPrefs.remove(keyExtend);
                    else
                        techPrefs.putDouble(keyExtend, a.getLambdaExtendOverMin());

                    // USER_NODE
                    String keyRigid = getKey(KEY_RIGID, apId);
                    if (removeDefaults && a.isRigid() == factoryInst.isRigid())
                        userPrefs.remove(keyRigid);
                    else
                        userPrefs.putBoolean(keyRigid, a.isRigid());

                    String keyFixedAngle = getKey(KEY_FIXED_ANGLE, apId);
                    if (removeDefaults && a.isFixedAngle() == factoryInst.isFixedAngle())
                        userPrefs.remove(keyFixedAngle);
                    else
                        userPrefs.putBoolean(keyFixedAngle, a.isFixedAngle());

                    String keySlidable = getKey(KEY_SLIDABLE, apId);
                    if (removeDefaults && a.isSlidable() == factoryInst.isSlidable())
                        userPrefs.remove(keySlidable);
                    else
                        userPrefs.putBoolean(keySlidable, a.isSlidable());

                    String keyExtended = getKey(KEY_EXTENDED, apId);
                    if (removeDefaults && a.isTailExtended() == factoryInst.isTailExtended())
                        userPrefs.remove(keyExtended);
                    else
                        userPrefs.putBoolean(keyExtended, a.isTailExtended());

                    String keyDirectional = getKey(KEY_DIRECTIONAL, apId);
                    if (removeDefaults && a.isHeadArrowed() == factoryInst.isHeadArrowed())
                        userPrefs.remove(keyDirectional);
                    else
                        userPrefs.putBoolean(keyDirectional, a.isHeadArrowed());
                }

                Integer angleIncrementObj = defaultArcAngleIncrements.get(apId);
                if (oldEp == null || angleIncrementObj != oldEp.defaultArcAngleIncrements.get(apId)) {
                    int factoryAngleIncrement = ap.getFactoryAngleIncrement();
                    int angleIncrement = angleIncrementObj != null ? angleIncrementObj.intValue() : factoryAngleIncrement;
                    String keyAngle = getKey(KEY_ANGLE, apId);
                    if (removeDefaults && angleIncrement == factoryAngleIncrement)
                        userPrefs.remove(keyAngle);
                    else
                        userPrefs.putInt(keyAngle, angleIncrement);
                }

                PrimitiveNodeId pinId = defaultArcPins.get(apId);
                if (oldEp == null || pinId != oldEp.defaultArcPins.get(apId)) {
                    String keyPin = getKey(KEY_PIN, apId);
                    String pinName = pinId != null ? pinId.name : "";
                    if (removeDefaults && pinName.length() == 0)
                        techPrefs.remove(keyPin);
                    else
                        techPrefs.put(keyPin, pinName);
                }
            }
        }
    }

    public EditingPreferences withDefaultNodes(Map<PrimitiveNodeId,ImmutableNodeInst> defaultNodes) {
        HashMap<PrimitiveNodeId,ImmutableNodeInst> newDefaultNodes = new HashMap<PrimitiveNodeId,ImmutableNodeInst>();
		for(Technology tech: techPool.values()) {
			for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); ) {
				PrimitiveNode np = it.next();
                ImmutableNodeInst n = defaultNodes.get(np.getId());
                if (n == null) continue;
                if (n.protoId != np.getId())
                    throw new IllegalArgumentException();
                newDefaultNodes.put(np.getId(), n);
			}
		}
        if (this.defaultNodes.equals(newDefaultNodes)) return this;
        return new EditingPreferences(this.techPool,
                newDefaultNodes,
                this.defaultArcs,
                this.defaultArcAngleIncrements,
                this.defaultArcPins);
    }

    public EditingPreferences withArcDefaults(
            Map<ArcProtoId,ImmutableArcInst> defaultArcs,
            Map<ArcProtoId,Integer> defaultArcAngleIncrements,
            Map<ArcProtoId,PrimitiveNodeId> defaultArcPins) {
        HashMap<ArcProtoId,ImmutableArcInst> newDefaultArcs = new HashMap<ArcProtoId,ImmutableArcInst>();
        HashMap<ArcProtoId,Integer> newDefaultArcAngleIncrements = new HashMap<ArcProtoId,Integer>();
        HashMap<ArcProtoId,PrimitiveNodeId> newDefaultArcPins = new HashMap<ArcProtoId,PrimitiveNodeId>();
		for(Technology tech: techPool.values()) {
			for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); ) {
				ArcProto ap = it.next();
                ArcProtoId apId = ap.getId();

                ImmutableArcInst a = defaultArcs.get(apId);
                if (a != null) {
                    if (a.protoId != apId)
                        throw new IllegalArgumentException();
                    newDefaultArcs.put(apId, a);
                }

                Integer angleIncrement = defaultArcAngleIncrements.get(apId);
                if (angleIncrement != null)
                    newDefaultArcAngleIncrements.put(apId, angleIncrement);

                PrimitiveNodeId arcPinId = defaultArcPins.get(apId);
                if (arcPinId != null)
                    newDefaultArcPins.put(apId, arcPinId);
			}
		}
        if (this.defaultArcs.equals(newDefaultArcs) &&
            this.defaultArcAngleIncrements.equals(newDefaultArcAngleIncrements) &&
            this.defaultArcPins.equals(newDefaultArcPins)) return this;
        return new EditingPreferences(this.techPool,
                this.defaultNodes,
                newDefaultArcs,
                newDefaultArcAngleIncrements,
                newDefaultArcPins);
    }

    public ImmutableNodeInst getDefaultNode(PrimitiveNodeId pnId) {
        return defaultNodes.get(pnId);
    }

    public ImmutableArcInst getDefaultArc(ArcProtoId apId) {
        return defaultArcs.get(apId);
    }

    public Integer getDefaultAngleIncrement(ArcProtoId apId) {
        return defaultArcAngleIncrements.get(apId);
    }

    public PrimitiveNodeId getDefaultArcPinId(ArcProtoId apId) {
        return defaultArcPins.get(apId);
    }

    public static EditingPreferences getThreadEditingPreferences() {
        return threadEditingPreferences.get();
    }

    public static EditingPreferences setThreadEditingPreferences(EditingPreferences ep) {
        EditingPreferences oldEp = threadEditingPreferences.get();
        threadEditingPreferences.set(ep);
        return oldEp;
    }
}
