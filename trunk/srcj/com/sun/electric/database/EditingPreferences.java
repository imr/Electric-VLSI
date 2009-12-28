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
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.id.ArcProtoId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.text.PrefPackage;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
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
    private static final String KEY_ALIGNMENT = "AlignmentToGridVector";
    private static final Dimension2D[] DEFAULT_ALIGNMENTS = {
        new ImmutableDimension2D(20),
        new ImmutableDimension2D(10),
        new ImmutableDimension2D(5),
        new ImmutableDimension2D(1),
        new ImmutableDimension2D(0.5)
    };
    private static final int DEFAULT_ALIGNMENT_INDEX = 3;
    private static final String TEXT_DESCRIPTOR_NODE = "database/variable";
    // In TEXT_DESCRIPTOR_NODE
    private static final String KEY_TEXT_DESCRIPTOR = "TextDescriptorFor";
    private static final String KEY_TEXT_DESCRIPTOR_COLOR = "TextDescriptorColorFor";
    private static final String KEY_TEXT_DESCRIPTOR_FONT = "TextDescriptorFontFor";
    private static final ThreadLocal<EditingPreferences> threadEditingPreferences = new ThreadLocal<EditingPreferences>();
    private transient final TechPool techPool;
    private final HashMap<PrimitiveNodeId, ImmutableNodeInst> defaultNodes;
    private HashMap<ArcProtoId, ImmutableArcInst> defaultArcs;
    private HashMap<ArcProtoId, Integer> defaultArcAngleIncrements;
    private HashMap<ArcProtoId, PrimitiveNodeId> defaultArcPins;
    private Dimension2D[] alignments;
    private int alignmentIndex;
    private TextDescriptor[] textDescriptors;
    /** What type of "smart" vertical text placement should be done for Exports.
     * The values can be 0: no smart placement; 1: place text "inside"; 2: place text "outside".
     * The default is 0.
     */
    @IntegerPref(node = USER_NODE, key = "SmartVerticalPlacementExport", factory = 0)
    public final int smartVerticalPlacementExport = 0;
    /** What type of "smart" horizontal text placement should be done for Exports.
     * The values can be 0: no smart placement; 1: place text "inside"; 2: place text "outside".
     * The default is 0.
     */
    @IntegerPref(node = USER_NODE, key = "SmartHorizontalPlacementExport", factory = 0)
    public final int smartHorizontalPlacementExport = 0;
    /** What type of "smart" text placement should be done for vertical Arcs.
     * The values can be 0: place text inside; 1: place text to left; 2: place text to right.
     * The default is 0.
     */
    @IntegerPref(node = USER_NODE, key = "SmartVerticalPlacementArc", factory = 0)
    public final int smartVerticalPlacementArc = 0;
    /** What type of "smart" text placement should be done for horizontal Arcs.
     * The values can be 0: place text inside; 1: place text above; 2: place text below.
     * The default is 0.
     */
    @IntegerPref(node = USER_NODE, key = "SmartHorizontalPlacementArc", factory = 0)
    public final int smartHorizontalPlacementArc = 0;
    /** What type of arcs are drawn: true to make them as wide as connecting nodes,
     * false to make them normal size.
     * The default is true.
     */
    @BooleanPref(node = USER_NODE, key = "FatWires", factory = true)
    public boolean fatWires;

    public EditingPreferences(boolean factory, TechPool techPool) {
        super(factory);
        this.techPool = techPool;
        defaultNodes = new HashMap<PrimitiveNodeId, ImmutableNodeInst>();
        defaultArcs = new HashMap<ArcProtoId, ImmutableArcInst>();
        defaultArcAngleIncrements = new HashMap<ArcProtoId, Integer>();
        defaultArcPins = new HashMap<ArcProtoId, PrimitiveNodeId>();

        TextDescriptor.TextType[] textTypes = TextDescriptor.TextType.class.getEnumConstants();
        textDescriptors = new TextDescriptor[textTypes.length * 2];
        if (factory) {
            alignments = DEFAULT_ALIGNMENTS;
            alignmentIndex = DEFAULT_ALIGNMENT_INDEX;
            for (int i = 0; i < textTypes.length; i++) {
                TextDescriptor.TextType t = textTypes[i];
                textDescriptors[i * 2 + 0] = t.getFactoryTextDescriptor().withDisplay(false);
                textDescriptors[i * 2 + 1] = t.getFactoryTextDescriptor();
            }
            return;
        }

        Preferences prefRoot = getPrefRoot();
        Preferences techPrefs = prefRoot.node(TECH_NODE);
        Preferences userPrefs = prefRoot.node(USER_NODE);
        Preferences textDescriptorPrefs = prefRoot.node(TEXT_DESCRIPTOR_NODE);

        for (Technology tech : techPool.values()) {
            for (Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext();) {
                PrimitiveNode pn = it.next();
                PrimitiveNodeId pnId = pn.getId();
                ImmutableNodeInst factoryInst = pn.getFactoryDefaultInst();
                EPoint factorySize = factoryInst.size;

                // TECH_NODE
                String keyExtendX = getKey(KEY_EXTEND_X, pnId);
                double factoryExtendX = factorySize.getLambdaX() * 0.5;
                double extendX = techPrefs.getDouble(keyExtendX, factoryExtendX);

                String keyExtendY = getKey(KEY_EXTEND_Y, pnId);
                double factoryExtendY = factorySize.getLambdaY() * 0.5;
                double extendY = techPrefs.getDouble(keyExtendY, factoryExtendY);

                if (extendX == factoryExtendX && extendY == factoryExtendY) {
                    continue;
                }
                EPoint size = EPoint.fromLambda(extendX * 2, extendY * 2);
                defaultNodes.put(pnId, factoryInst.withSize(size));
            }

            for (Iterator<ArcProto> it = tech.getArcs(); it.hasNext();) {
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

                ImmutableArcInst a = factoryInst.withGridExtendOverMin(DBMath.lambdaToGrid(extend)).
                        withFlags(flags);
                if (a != factoryInst) {
                    defaultArcs.put(apId, a);
                }

                String keyAngle = getKey(KEY_ANGLE, apId);
                int factoryAngleIncrement = ap.getFactoryAngleIncrement();
                int angleIncrement = userPrefs.getInt(keyAngle, factoryAngleIncrement);
                if (angleIncrement != factoryAngleIncrement) {
                    defaultArcAngleIncrements.put(apId, Integer.valueOf(angleIncrement));
                }

                String keyPin = getKey(KEY_PIN, apId);
                String arcPinName = techPrefs.get(keyPin, "");
                if (arcPinName.length() > 0) {
                    PrimitiveNode pin = tech.findNodeProto(arcPinName);
                    if (pin != null) {
                        defaultArcPins.put(apId, pin.getId());
                    }
                }
            }
        }

        String alignmentStr = userPrefs.get(KEY_ALIGNMENT, null);
        if (alignmentStr != null) {
            alignments = correctAlignmentGridVector(transformStringIntoArray(alignmentStr));
            alignmentIndex = Math.max(0, Math.min(alignments.length - 1, getDefaultAlignmentIndex(alignmentStr)));
        } else {
            alignments = DEFAULT_ALIGNMENTS;
            alignmentIndex = DEFAULT_ALIGNMENT_INDEX;
        }

        for (int i = 0; i < textTypes.length; i++) {
            TextDescriptor.TextType t = textTypes[i];
            TextDescriptor factoryTd = t.getFactoryTextDescriptor();

            long factoryBits = swap(factoryTd.lowLevelGet());
            long bits = textDescriptorPrefs.getLong(t.getKey(KEY_TEXT_DESCRIPTOR), factoryBits);
            int color = textDescriptorPrefs.getInt(t.getKey(KEY_TEXT_DESCRIPTOR_COLOR), 0);
            String fontName = textDescriptorPrefs.get(t.getKey(KEY_TEXT_DESCRIPTOR_FONT), "");
            MutableTextDescriptor mtd = new MutableTextDescriptor(swap(bits), color, true);
            int face = 0;
            if (fontName.length() > 0) {
                TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(fontName);
                if (af != null) {
                    face = af.getIndex();
                }
            }
            mtd.setFace(face);
            textDescriptors[i * 2 + 1] = TextDescriptor.newTextDescriptor(mtd);
            mtd.setDisplay(false);
            textDescriptors[i * 2 + 0] = TextDescriptor.newTextDescriptor(mtd);
        }
    }

    @Override
    public void putPrefs(Preferences prefRoot, boolean removeDefaults) {
        putPrefs(prefRoot, removeDefaults, null);
    }

    public void putPrefs(Preferences prefRoot, boolean removeDefaults, EditingPreferences oldEp) {
        super.putPrefs(prefRoot, removeDefaults);

        if (oldEp != null && oldEp.techPool != techPool) {
            oldEp = null;
        }

        Preferences techPrefs = prefRoot.node(TECH_NODE);
        Preferences userPrefs = prefRoot.node(USER_NODE);
        Preferences textDescriptorPrefs = prefRoot.node(TEXT_DESCRIPTOR_NODE);
        for (Technology tech : techPool.values()) {
            for (Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext();) {
                PrimitiveNode pn = it.next();
                PrimitiveNodeId pnId = pn.getId();
                ImmutableNodeInst n = defaultNodes.get(pnId);
                if (oldEp == null || n != oldEp.defaultNodes.get(pnId)) {
                    ImmutableNodeInst factoryInst = pn.getFactoryDefaultInst();
                    if (n == null) {
                        n = factoryInst;
                    }

                    String keyX = getKey(KEY_EXTEND_X, pnId);
                    if (removeDefaults && n.size.getGridX() == factoryInst.size.getGridX()) {
                        techPrefs.remove(keyX);
                    } else {
                        techPrefs.putDouble(keyX, n.size.getLambdaX() * 0.5);
                    }

                    String keyY = getKey(KEY_EXTEND_Y, pnId);
                    if (removeDefaults && n.size.getGridY() == factoryInst.size.getGridY()) {
                        techPrefs.remove(keyY);
                    } else {
                        techPrefs.putDouble(keyY, n.size.getLambdaY() * 0.5);
                    }
                }
            }

            for (Iterator<ArcProto> it = tech.getArcs(); it.hasNext();) {
                ArcProto ap = it.next();
                ArcProtoId apId = ap.getId();
                ImmutableArcInst a = defaultArcs.get(apId);
                if (oldEp == null || a != oldEp.defaultArcs.get(apId)) {
                    ImmutableArcInst factoryInst = ap.getFactoryDefaultInst();
                    if (a == null) {
                        a = factoryInst;
                    }

                    // TECH_NODE
                    String keyExtend = getKey(KEY_EXTEND, apId);
                    if (removeDefaults && a.getGridExtendOverMin() == factoryInst.getGridExtendOverMin()) {
                        techPrefs.remove(keyExtend);
                    } else {
                        techPrefs.putDouble(keyExtend, a.getLambdaExtendOverMin());
                    }

                    // USER_NODE
                    String keyRigid = getKey(KEY_RIGID, apId);
                    if (removeDefaults && a.isRigid() == factoryInst.isRigid()) {
                        userPrefs.remove(keyRigid);
                    } else {
                        userPrefs.putBoolean(keyRigid, a.isRigid());
                    }

                    String keyFixedAngle = getKey(KEY_FIXED_ANGLE, apId);
                    if (removeDefaults && a.isFixedAngle() == factoryInst.isFixedAngle()) {
                        userPrefs.remove(keyFixedAngle);
                    } else {
                        userPrefs.putBoolean(keyFixedAngle, a.isFixedAngle());
                    }

                    String keySlidable = getKey(KEY_SLIDABLE, apId);
                    if (removeDefaults && a.isSlidable() == factoryInst.isSlidable()) {
                        userPrefs.remove(keySlidable);
                    } else {
                        userPrefs.putBoolean(keySlidable, a.isSlidable());
                    }

                    String keyExtended = getKey(KEY_EXTENDED, apId);
                    if (removeDefaults && a.isTailExtended() == factoryInst.isTailExtended()) {
                        userPrefs.remove(keyExtended);
                    } else {
                        userPrefs.putBoolean(keyExtended, a.isTailExtended());
                    }

                    String keyDirectional = getKey(KEY_DIRECTIONAL, apId);
                    if (removeDefaults && a.isHeadArrowed() == factoryInst.isHeadArrowed()) {
                        userPrefs.remove(keyDirectional);
                    } else {
                        userPrefs.putBoolean(keyDirectional, a.isHeadArrowed());
                    }
                }

                Integer angleIncrementObj = defaultArcAngleIncrements.get(apId);
                if (oldEp == null || angleIncrementObj != oldEp.defaultArcAngleIncrements.get(apId)) {
                    int factoryAngleIncrement = ap.getFactoryAngleIncrement();
                    int angleIncrement = angleIncrementObj != null ? angleIncrementObj.intValue() : factoryAngleIncrement;
                    String keyAngle = getKey(KEY_ANGLE, apId);
                    if (removeDefaults && angleIncrement == factoryAngleIncrement) {
                        userPrefs.remove(keyAngle);
                    } else {
                        userPrefs.putInt(keyAngle, angleIncrement);
                    }
                }

                PrimitiveNodeId pinId = defaultArcPins.get(apId);
                if (oldEp == null || pinId != oldEp.defaultArcPins.get(apId)) {
                    String keyPin = getKey(KEY_PIN, apId);
                    String pinName = pinId != null ? pinId.name : "";
                    if (removeDefaults && pinName.length() == 0) {
                        techPrefs.remove(keyPin);
                    } else {
                        techPrefs.put(keyPin, pinName);
                    }
                }
            }
        }
        if (oldEp == null || alignments != oldEp.alignments || alignmentIndex != oldEp.alignmentIndex) {
            if (removeDefaults && Arrays.equals(alignments, DEFAULT_ALIGNMENTS) && alignmentIndex == DEFAULT_ALIGNMENT_INDEX) {
                userPrefs.remove(KEY_ALIGNMENT);
            } else {
                userPrefs.put(KEY_ALIGNMENT, transformArrayIntoString(alignments, alignmentIndex));
            }
        }
        if (oldEp == null || textDescriptors != oldEp.textDescriptors) {
            TextDescriptor.TextType[] textTypes = TextDescriptor.TextType.class.getEnumConstants();
            for (int i = 0; i < textTypes.length; i++) {
                TextDescriptor.TextType t = textTypes[i];
                TextDescriptor td = textDescriptors[i * 2 + 1];

                String keyTextDescriptor = t.getKey(KEY_TEXT_DESCRIPTOR);
                String keyTextDescriptorColor = t.getKey(KEY_TEXT_DESCRIPTOR_COLOR);
                String keyTextDescriptorFont = t.getKey(KEY_TEXT_DESCRIPTOR_FONT);

                long factoryBits = t.getFactoryTextDescriptor().lowLevelGet();
                long bits;
                String fontName;
                if (td.getFace() == 0) {
                    bits = td.lowLevelGet();
                    fontName = "";
                } else {
                    fontName = TextDescriptor.ActiveFont.findActiveFont(td.getFace()).getName();
                    MutableTextDescriptor mtd = new MutableTextDescriptor(td);
                    mtd.setFace(0);
                    bits = mtd.lowLevelGet();
                }
                if (removeDefaults && bits == factoryBits) {
                    textDescriptorPrefs.remove(keyTextDescriptor);
                } else {
                    textDescriptorPrefs.putLong(keyTextDescriptor, swap(bits));
                }
                if (removeDefaults && td.getColorIndex() == 0) {
                    textDescriptorPrefs.remove(keyTextDescriptorColor);
                } else {
                    textDescriptorPrefs.putInt(keyTextDescriptorColor, td.getColorIndex());
                }
                if (removeDefaults && fontName.length() == 0) {
                    textDescriptorPrefs.remove(keyTextDescriptorFont);
                } else {
                    textDescriptorPrefs.put(keyTextDescriptorFont, fontName);
                }
            }
        }
    }

    public EditingPreferences withNodeSize(PrimitiveNodeId pnId, EPoint size) {
        PrimitiveNode pn = techPool.getPrimitiveNode(pnId);
        if (pn == null) {
            return this;
        }
        ImmutableNodeInst n = pn.getDefaultInst(this);
        assert n.protoId == pnId;
        if (n.size.equals(size)) {
            return this;
        }
        HashMap<PrimitiveNodeId, ImmutableNodeInst> newDefaultNodes = new HashMap<PrimitiveNodeId, ImmutableNodeInst>(defaultNodes);
        if (size.equals(pn.getFactoryDefaultInst().size)) {
            newDefaultNodes.remove(pnId);
        } else {
            newDefaultNodes.put(pnId, n.withSize(size));
        }
        return (EditingPreferences) withField("defaultNodes", newDefaultNodes);
    }

    public EditingPreferences withNodesReset() {
        if (defaultNodes.isEmpty()) {
            return this;
        }
        return (EditingPreferences) withField("defaultNodes", new HashMap<PrimitiveNodeId, ImmutableNodeInst>());
    }

    public EditingPreferences withArcFlags(ArcProtoId apId, int flags) {
        ArcProto ap = techPool.getArcProto(apId);
        if (ap == null) {
            return this;
        }
        ImmutableArcInst a = ap.getDefaultInst(this);
        if (flags == a.flags) {
            return this;
        }
        HashMap<ArcProtoId, ImmutableArcInst> newDefaultArcs = new HashMap<ArcProtoId, ImmutableArcInst>(defaultArcs);
        ImmutableArcInst factoryA = ap.getFactoryDefaultInst();
        if (flags == factoryA.flags) {
            newDefaultArcs.remove(apId);
        } else {
            newDefaultArcs.put(apId, a.withFlags(flags));
        }
        return (EditingPreferences) withField("defaultArcs", newDefaultArcs);
    }

    public EditingPreferences withArcGridExtend(ArcProtoId apId, long gridExtend) {
        ArcProto ap = techPool.getArcProto(apId);
        if (ap == null) {
            return this;
        }
        ImmutableArcInst a = ap.getDefaultInst(this);
        if (gridExtend == a.getGridExtendOverMin()) {
            return this;
        }
        HashMap<ArcProtoId, ImmutableArcInst> newDefaultArcs = new HashMap<ArcProtoId, ImmutableArcInst>(defaultArcs);
        ImmutableArcInst factoryA = ap.getFactoryDefaultInst();
        if (gridExtend == factoryA.getGridExtendOverMin()) {
            newDefaultArcs.remove(apId);
        } else {
            newDefaultArcs.put(apId, a.withGridExtendOverMin(gridExtend));
        }
        return (EditingPreferences) withField("defaultArcs", newDefaultArcs);
    }

    public EditingPreferences withArcAngleIncrement(ArcProtoId apId, int angleIncrement) {
        ArcProto ap = techPool.getArcProto(apId);
        if (ap == null) {
            return this;
        }
        if (angleIncrement == ap.getAngleIncrement(this)) {
            return this;
        }
        HashMap<ArcProtoId, Integer> newDefaultArcAngleIncrements = new HashMap<ArcProtoId, Integer>(defaultArcAngleIncrements);
        int factoryAngleIncrement = ap.getFactoryAngleIncrement();
        if (angleIncrement == factoryAngleIncrement) {
            newDefaultArcAngleIncrements.remove(apId);
        } else {
            newDefaultArcAngleIncrements.put(apId, Integer.valueOf(angleIncrement));
        }
        return (EditingPreferences) withField("defaultArcAngleIncrements", newDefaultArcAngleIncrements);
    }

    public EditingPreferences withArcPin(ArcProtoId apId, PrimitiveNodeId arcPinId) {
        ArcProto ap = techPool.getArcProto(apId);
        if (ap == null) {
            return this;
        }
        if (arcPinId == ap.findOverridablePinProto(this).getId()) {
            return this;
        }
        HashMap<ArcProtoId, PrimitiveNodeId> newDefaultArcPins = new HashMap<ArcProtoId, PrimitiveNodeId>(defaultArcPins);
        PrimitiveNodeId factoryArcPinId = ap.findPinProto().getId();
        if (arcPinId == factoryArcPinId) {
            newDefaultArcPins.remove(apId);
        } else {
            newDefaultArcPins.put(apId, arcPinId);
        }
        return (EditingPreferences) withField("defaultArcPins", newDefaultArcPins);
    }

    public EditingPreferences withArcsReset() {
        return withDefaultArcsReset().withDefaultAngleIncrementsReset().withDefaultArcPinsReset();
    }

    private EditingPreferences withDefaultArcsReset() {
        if (defaultArcs.isEmpty()) {
            return this;
        }
        return (EditingPreferences) withField("defaultArcs", new HashMap<ArcProtoId, ImmutableArcInst>());
    }

    private EditingPreferences withDefaultAngleIncrementsReset() {
        if (defaultArcAngleIncrements.isEmpty()) {
            return this;
        }
        return (EditingPreferences) withField("defaultArcAngleIncrements", new HashMap<ArcProtoId, Integer>());
    }

    private EditingPreferences withDefaultArcPinsReset() {
        if (defaultArcPins.isEmpty()) {
            return this;
        }
        return (EditingPreferences) withField("defaultArcPins", new HashMap<ArcProtoId, PrimitiveNodeId>());
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

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof EditingPreferences) {
            EditingPreferences that = (EditingPreferences) o;
            return this.techPool == that.techPool
                    && this.defaultNodes.equals(that.defaultNodes)
                    && this.defaultArcs.equals(that.defaultArcs)
                    && this.defaultArcAngleIncrements.equals(that.defaultArcAngleIncrements)
                    && this.defaultArcPins.equals(that.defaultArcPins)
                    && Arrays.equals(this.alignments, that.alignments)
                    && this.alignmentIndex == that.alignmentIndex
                    && Arrays.equals(this.textDescriptors, that.textDescriptors)
                    && this.fatWires == that.fatWires;
        }
        return false;
    }

    /**
     * Method to return the default alignment of objects to the grid.
     * The default is (1,1), meaning that placement and movement should land on whole grid units.
     * @return the default alignment of objects to the grid.
     */
    public Dimension2D getAlignmentToGrid() {
        return alignments[alignmentIndex];
    }

    /**
     * Method to return index of the current alignment.
     * @return the index of the current alignment.
     */
    public int getAlignmentToGridIndex() {
        return alignmentIndex;
    }

    /**
     * Method to return an array of five grid alignment values.
     * @return an array of five grid alignment values.
     */
    public Dimension2D[] getAlignmentToGridVector() {
        return alignments.clone();
    }

    /**
     * Method to set the default alignment of objects to the grid.
     * @param dist the array of grid alignment values.
     * @param current the index in the array that is the current grid alignment.
     */
    public EditingPreferences withAlignment(Dimension2D[] dist, int current) {
        dist = correctAlignmentGridVector(dist.clone());
        current = Math.max(0, Math.min(dist.length - 1, current));
        return withAlignments(dist).withAlignmentIndex(current);
    }

    public EditingPreferences withAlignmentReset() {
        return withAlignments(DEFAULT_ALIGNMENTS).withAlignmentIndex(DEFAULT_ALIGNMENT_INDEX);
    }

    private EditingPreferences withAlignments(Dimension2D[] alignments) {
        if (Arrays.equals(alignments, this.alignments)) {
            return this;
        }
        return (EditingPreferences) withField("alignments", alignments);
    }

    private EditingPreferences withAlignmentIndex(int alignmentIndex) {
        if (alignmentIndex == this.alignmentIndex) {
            return this;
        }
        return (EditingPreferences) withField("alignmentIndex", alignmentIndex);
    }

    public TextDescriptor getTextDescriptor(TextDescriptor.TextType textType, boolean display) {
        return textDescriptors[textType.ordinal() * 2 + (display ? 1 : 0)];
    }

    public EditingPreferences withTextDescriptor(TextDescriptor.TextType textType, TextDescriptor td) {
        td = td.withDisplay(true);
        if (td == textDescriptors[textType.ordinal() * 2 + 1]) {
            return this;
        }
        TextDescriptor[] newTextDescriptors = textDescriptors.clone();
        newTextDescriptors[textType.ordinal() * 2 + 1] = td;
        newTextDescriptors[textType.ordinal() * 2 + 0] = td.withDisplay(false);
        return (EditingPreferences) withField("textDescriptors", newTextDescriptors);
    }

    public EditingPreferences withTextDescriptorsReset() {
        EditingPreferences ep = this;
        TextDescriptor.TextType[] textTypes = TextDescriptor.TextType.class.getEnumConstants();
        for (int i = 0; i < textTypes.length; i++) {
            TextDescriptor.TextType t = textTypes[i];
            ep = ep.withTextDescriptor(t, t.getFactoryTextDescriptor());
        }
        return ep;
    }

    public EditingPreferences withSmartVerticalPlacementExport(int smartVerticalPlacementExport) {
        if (smartVerticalPlacementExport == this.smartVerticalPlacementExport) {
            return this;
        }
        return (EditingPreferences) withField("smartVerticalPlacementExport", Integer.valueOf(smartVerticalPlacementExport));
    }

    public EditingPreferences withSmartHorizontalPlacementExport(int smartHorizontalPlacementExport) {
        if (smartHorizontalPlacementExport == this.smartHorizontalPlacementExport) {
            return this;
        }
        return (EditingPreferences) withField("smartHorizontalPlacementExport", Integer.valueOf(smartHorizontalPlacementExport));
    }

    public EditingPreferences withSmartVerticalPlacementArc(int smartVerticalPlacementArc) {
        if (smartVerticalPlacementArc == this.smartVerticalPlacementArc) {
            return this;
        }
        return (EditingPreferences) withField("smartVerticalPlacementArc", Integer.valueOf(smartVerticalPlacementArc));
    }

    public EditingPreferences withSmartHorizontalPlacementArc(int smartHorizontalPlacementArc) {
        if (smartHorizontalPlacementArc == this.smartHorizontalPlacementArc) {
            return this;
        }
        return (EditingPreferences) withField("smartHorizontalPlacementArc", Integer.valueOf(smartHorizontalPlacementArc));
    }

    public EditingPreferences withPlacementReset() {
        return withSmartHorizontalPlacementArc(0).withSmartVerticalPlacementArc(0).withSmartHorizontalPlacementExport(0).withSmartVerticalPlacementExport(0);
    }

    public EditingPreferences withFatWiresReset() {
        return (EditingPreferences) withField("fatWires", Boolean.TRUE);
    }

    private static Dimension2D[] correctAlignmentGridVector(Dimension2D[] retVal) {
        if (retVal.length < 5) {
            Dimension2D[] newRetVal = new Dimension2D[5];
            int shift = 5 - retVal.length;
            for (int i = retVal.length - 1; i >= 0; i--) {
                newRetVal[i + shift] = retVal[i];
            }
            while (shift > 0) {
                shift--;
                newRetVal[shift] = new Dimension2D.Double(newRetVal[shift + 1].getWidth() * 2, newRetVal[shift + 1].getHeight() * 2);
            }
            retVal = newRetVal;
        }
        for (int i = 0; i < retVal.length; i++) {
            retVal[i] = new ImmutableDimension2D(retVal[i]);
        }
        return retVal;
    }

    /**
     * Method to extract an array of Dimensions from a string.
     * The format of the string is "(x1/y1 x2/y2 x3/y3 ...)"
     * where the "/y1" part may be omitted.  Also, if the dimension
     * starts with an "*" then it is the current one.
     * @param vector the input string.
     * @return the array of values.
     */
    private static Dimension2D[] transformStringIntoArray(String vector) {
        StringTokenizer parse = new StringTokenizer(vector, "( )", false);
        List<Dimension2D> valuesFound = new ArrayList<Dimension2D>();
        while (parse.hasMoreTokens()) {
            String value = parse.nextToken();
            if (value.startsWith("*")) {
                value = value.substring(1);
            }
            int slashPos = value.indexOf('/');
            String xPart = value, yPart = value;
            if (slashPos >= 0) {
                xPart = value.substring(0, slashPos);
                yPart = value.substring(slashPos + 1);
            }
            try {
                Dimension2D dim = new Dimension2D.Double(Math.abs(Double.parseDouble(xPart)), Math.abs(Double.parseDouble(yPart)));
                valuesFound.add(dim);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Dimension2D[] values = new Dimension2D[valuesFound.size()];
        for (int i = 0; i < valuesFound.size(); i++) {
            values[i] = valuesFound.get(i);
        }
        return values;
    }

    /**
     * Method to extract an array of Dimensions from a string.
     * The format of the string is "(x1/y1 x2/y2 x3/y3 ...)"
     * where the "/y1" part may be omitted.  Also, if the dimension
     * starts with an "*" then it is the current one.
     * @param vector the input string.
     * @return the array of values.
     */
    private static int getDefaultAlignmentIndex(String vector) {
        int curVal = 0;
        StringTokenizer parse = new StringTokenizer(vector, "( )", false);
        while (parse.hasMoreTokens()) {
            String value = parse.nextToken();
            if (value.startsWith("*")) {
                return curVal;
            }
            if (TextUtils.atof(value) < 0) {
                return curVal;
            }
            curVal++;
        }
        return 0;
    }

    /**
     * Method to transform an array of Dimension into a string that can be stored in a preference.
     * The format of the string is "(x1/y1 x2/y2 x3/y3 ...)" where the current entry has
     * an "*" in front of it.
     * @param s the values.
     * @param current the current value index (0-based).
     * @return string representing the array.
     */
    private static String transformArrayIntoString(Dimension2D[] s, int current) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length; i++) {
            if (i == 0) {
                sb.append('(');
            } else {
                sb.append(' ');
            }
            if (i == current) {
                sb.append('*');
            }
            sb.append(s[i].getWidth());
            if (s[i].getWidth() != s[i].getHeight()) {
                sb.append('/');
                sb.append(s[i].getHeight());
            }
        }
        sb.append(')');
        String dir = sb.toString();
        return dir;
    }

    private static class ImmutableDimension2D extends Dimension2D {

        private final double width;
        private final double height;

        private ImmutableDimension2D(Dimension2D d) {
            this(d.getWidth(), d.getHeight());
        }

        private ImmutableDimension2D(double size) {
            this(size, size);
        }

        private ImmutableDimension2D(double width, double height) {
            this.width = DBMath.round(width * 0.5) * 2;
            this.height = DBMath.round(height * 0.5) * 2;
        }

        @Override
        public double getWidth() {
            return width;
        }

        @Override
        public double getHeight() {
            return height;
        }

        @Override
        public void setSize(java.awt.geom.Dimension2D d) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSize(double width, double height) {
            throw new UnsupportedOperationException();
        }
    }

    private static long swap(long value) {
        int v0 = (int) value;
        return (value >>> 32) | ((long) v0 << 32);
    }

    @Override
    public int hashCode() {
        return defaultNodes.size() + defaultArcs.size();
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
