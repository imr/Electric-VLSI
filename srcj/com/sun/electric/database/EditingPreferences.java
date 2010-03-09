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

    @BooleanPref(node = USER_NODE, key = "PlaceCellCenter", factory = true)
    private boolean placeCellCenter;
    @IntegerPref(node = USER_NODE, key = "SmartVerticalPlacementExport", factory = 0)
    private int smartVerticalPlacementExport;
    @IntegerPref(node = USER_NODE, key = "SmartHorizontalPlacementExport", factory = 0)
    private int smartHorizontalPlacementExport;
    @IntegerPref(node = USER_NODE, key = "SmartVerticalPlacementArc", factory = 0)
    private int smartVerticalPlacementArc;
    @IntegerPref(node = USER_NODE, key = "SmartHorizontalPlacementArc", factory = 0)
    private int smartHorizontalPlacementArc;
    @BooleanPref(node = USER_NODE, key = "FatWires", factory = true)
    private boolean fatWires;
    @DoublePref(node = USER_NODE, key = "IconGenLeadLength", factory = 2.0)
    private double iconGenLeadLength;
    @DoublePref(node = USER_NODE, key = "IconGenLeadSpacing", factory = 2.0)
    private double iconGenLeadSpacing;
    @BooleanPref(node = USER_NODE, key = "IconGenDrawLeads", factory = true)
    private boolean iconGenDrawLeads;
    @BooleanPref(node = USER_NODE, key = "IconsAlwaysDrawn", factory = false)
    private boolean iconsAlwaysDrawn;
    @BooleanPref(node = USER_NODE, key = "IconGenDrawBody", factory = true)
    private boolean iconGenDrawBody;
    @IntegerPref(node = USER_NODE, key = "IconGenExportPlacement", factory = 0)
    private int iconGenExportPlacement;
    @BooleanPref(node = USER_NODE, key = "IconGenExportPlacementExact", factory = true)
    private boolean iconGenExportPlacementExact;
    @BooleanPref(node = USER_NODE, key = "IconGenReverseExportOrder", factory = false)
    private boolean iconGenReverseExportOrder;
    @DoublePref(node = USER_NODE, key = "IconGenBodyTextSize", factory = 2.0)
    private double iconGenBodyTextSize;
    @IntegerPref(node = USER_NODE, key = "IconGenExportLocation", factory = 1)
    private int iconGenExportLocation;
    @IntegerPref(node = USER_NODE, key = "IconGenExportStyle", factory = 0)
    private int iconGenExportStyle;
    @IntegerPref(node = USER_NODE, key = "IconGenExportTech", factory = 1)
    private int iconGenExportTech;
    @IntegerPref(node = USER_NODE, key = "IconGenInstanceLocation", factory = 0)
    private int iconGenInstanceLocation;
    @IntegerPref(node = USER_NODE, key = "IconGenInputRot", factory = 0)
    private int iconGenInputRot;
    @IntegerPref(node = USER_NODE, key = "IconGenOutputRot", factory = 0)
    private int iconGenOutputRot;
    @IntegerPref(node = USER_NODE, key = "IconGenBidirRot", factory = 0)
    private int iconGenBidirRot;
    @IntegerPref(node = USER_NODE, key = "IconGenPowerRot", factory = 0)
    private int iconGenPowerRot;
    @IntegerPref(node = USER_NODE, key = "IconGenGroundRot", factory = 0)
    private int iconGenGroundRot;
    @IntegerPref(node = USER_NODE, key = "IconGenClockRot", factory = 0)
    private int iconGenClockRot;

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
    protected void putPrefs(Preferences prefRoot, boolean removeDefaults) {
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

    public EditingPreferences withPlaceCellCenter(boolean placeCellCenter) {
        if (placeCellCenter == this.isPlaceCellCenter()) {
            return this;
        }
        return withField("placeCellCenter", placeCellCenter);
    }

    public EditingPreferences withDefaultNodesReset() {
        if (defaultNodes.isEmpty()) {
            return this;
        }
        return withField("defaultNodes", new HashMap<PrimitiveNodeId, ImmutableNodeInst>());
    }

    public EditingPreferences withNodesReset() {
        return withDefaultNodesReset()
                .withPlaceCellCenter(true);
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
                    && this.isPlaceCellCenter() == that.isPlaceCellCenter()
                    && this.defaultArcs.equals(that.defaultArcs)
                    && this.defaultArcAngleIncrements.equals(that.defaultArcAngleIncrements)
                    && this.defaultArcPins.equals(that.defaultArcPins)
                    && Arrays.equals(this.alignments, that.alignments)
                    && this.alignmentIndex == that.alignmentIndex
                    && Arrays.equals(this.textDescriptors, that.textDescriptors)
                    && this.isFatWires() == that.isFatWires()
                    && this.getIconGenLeadLength() == that.getIconGenLeadLength()
                    && this.getIconGenLeadSpacing() == that.getIconGenLeadSpacing()
                    && this.isIconGenDrawLeads() == that.isIconGenDrawLeads()
                    && this.isIconsAlwaysDrawn() == that.isIconsAlwaysDrawn()
                    && this.isIconGenDrawBody() == that.isIconGenDrawBody()
                    && this.getIconGenExportPlacement() == that.getIconGenExportPlacement()
                    && this.isIconGenExportPlacementExact() == that.isIconGenExportPlacementExact()
                    && this.isIconGenReverseExportOrder() == that.isIconGenReverseExportOrder()
                    && this.getIconGenBodyTextSize() == that.getIconGenBodyTextSize()
                    && this.getIconGenExportLocation() == that.getIconGenExportLocation()
                    && this.getIconGenExportStyle() == that.getIconGenExportStyle()
                    && this.getIconGenExportTech() == that.getIconGenExportTech()
                    && this.getIconGenInstanceLocation() == that.getIconGenInstanceLocation()
                    && this.getIconGenInputRot() == that.getIconGenInputRot()
                    && this.getIconGenOutputRot() == that.getIconGenOutputRot()
                    && this.getIconGenBidirRot() == that.getIconGenBidirRot()
                    && this.getIconGenPowerRot() == that.getIconGenPowerRot()
                    && this.getIconGenClockRot() == that.getIconGenClockRot();
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
        if (smartVerticalPlacementExport == this.getSmartVerticalPlacementExport()) {
            return this;
        }
        return withField("smartVerticalPlacementExport", Integer.valueOf(smartVerticalPlacementExport));
    }

    public EditingPreferences withSmartHorizontalPlacementExport(int smartHorizontalPlacementExport) {
        if (smartHorizontalPlacementExport == this.getSmartHorizontalPlacementExport()) {
            return this;
        }
        return withField("smartHorizontalPlacementExport", Integer.valueOf(smartHorizontalPlacementExport));
    }

    public EditingPreferences withSmartVerticalPlacementArc(int smartVerticalPlacementArc) {
        if (smartVerticalPlacementArc == this.getSmartVerticalPlacementArc()) {
            return this;
        }
        return withField("smartVerticalPlacementArc", Integer.valueOf(smartVerticalPlacementArc));
    }

    public EditingPreferences withSmartHorizontalPlacementArc(int smartHorizontalPlacementArc) {
        if (smartHorizontalPlacementArc == this.getSmartHorizontalPlacementArc()) {
            return this;
        }
        return withField("smartHorizontalPlacementArc", Integer.valueOf(smartHorizontalPlacementArc));
    }

    public EditingPreferences withFatWires(boolean fatWires) {
        if (fatWires == this.isFatWires()) {
            return this;
        }
        return withField("fatWires", fatWires);
    }

    public EditingPreferences withPlacementReset() {
        return withSmartHorizontalPlacementArc(0).withSmartVerticalPlacementArc(0).withSmartHorizontalPlacementExport(0).withSmartVerticalPlacementExport(0);
    }

    public EditingPreferences withFatWiresReset() {
        return withFatWires(true);
    }

    public EditingPreferences withIconGenLeadLength(double iconGenLeadLength) {
        if (iconGenLeadLength == this.getIconGenLeadLength()) {
            return this;
        }
        return withField("iconGenLeadLength", iconGenLeadLength);
    }

    public EditingPreferences withIconGenLeadSpacing(double iconGenLeadSpacing) {
        if (iconGenLeadSpacing == this.getIconGenLeadSpacing()) {
            return this;
        }
        return withField("iconGenLeadSpacing", iconGenLeadSpacing);
    }

    public EditingPreferences withIconGenDrawLeads(boolean iconGenDrawLeads) {
        if (iconGenDrawLeads == this.isIconGenDrawLeads()) {
            return this;
        }
        return withField("iconGenDrawLeads", iconGenDrawLeads);
    }

    public EditingPreferences withIconsAlwaysDrawn(boolean iconsAlwaysDrawn) {
        if (iconsAlwaysDrawn == this.isIconsAlwaysDrawn()) {
            return this;
        }
        return withField("iconsAlwaysDrawn", iconsAlwaysDrawn);
    }

    public EditingPreferences withIconGenDrawBody(boolean iconGenDrawBody) {
        if (iconGenDrawBody == this.isIconGenDrawBody()) {
            return this;
        }
        return withField("iconGenDrawBody", iconGenDrawBody);
    }

    public EditingPreferences withIconGenExportPlacement(int iconGenExportPlacement) {
        if (iconGenExportPlacement == this.getIconGenExportPlacement()) {
            return this;
        }
        return withField("iconGenExportPlacement", iconGenExportPlacement);
    }

    public EditingPreferences withIconGenExportPlacementExact(boolean iconGenExportPlacementExact) {
        if (iconGenExportPlacementExact == this.isIconGenExportPlacementExact()) {
            return this;
        }
        return withField("iconGenExportPlacementExact", iconGenExportPlacementExact);
    }

    public EditingPreferences withIconGenReverseExportOrder(boolean iconGenReverseExportOrder) {
        if (iconGenReverseExportOrder == this.isIconGenReverseExportOrder()) {
            return this;
        }
        return withField("iconGenReverseExportOrder", iconGenReverseExportOrder);
    }

    public EditingPreferences withIconGenBodyTextSize(double iconGenBodyTextSize) {
        if (iconGenBodyTextSize == this.getIconGenBodyTextSize()) {
            return this;
        }
        return withField("iconGenBodyTextSize", iconGenBodyTextSize);
    }

    public EditingPreferences withIconGenExportLocation(int iconGenExportLocation) {
        if (iconGenExportLocation == this.getIconGenExportLocation()) {
            return this;
        }
        return withField("iconGenExportLocation", iconGenExportLocation);
    }

    public EditingPreferences withIconGenExportStyle(int iconGenExportStyle) {
        if (iconGenExportStyle == this.getIconGenExportStyle()) {
            return this;
        }
        return withField("iconGenExportStyle", iconGenExportStyle);
    }

    public EditingPreferences withIconGenExportTech(int iconGenExportTech) {
        if (iconGenExportTech == this.getIconGenExportTech()) {
            return this;
        }
        return withField("iconGenExportTech", iconGenExportTech);
    }

    public EditingPreferences withIconGenInstanceLocation(int iconGenInstanceLocation) {
        if (iconGenInstanceLocation == this.getIconGenInstanceLocation()) {
            return this;
        }
        return withField("iconGenInstanceLocation", iconGenInstanceLocation);
    }

    public EditingPreferences withIconGenInputRot(int iconGenInputRot) {
        if (iconGenInputRot == this.getIconGenInputRot()) {
            return this;
        }
        return withField("iconGenInputRot", iconGenInputRot);
    }

    public EditingPreferences withIconGenOutputRot(int iconGenOutputRot) {
        if (iconGenOutputRot == this.getIconGenOutputRot()) {
            return this;
        }
        return withField("iconGenOutputRot", iconGenOutputRot);
    }

    public EditingPreferences withIconGenBidirRot(int iconGenBidirRot) {
        if (iconGenBidirRot == this.getIconGenBidirRot()) {
            return this;
        }
        return withField("iconGenBidirRot", iconGenBidirRot);
    }

    public EditingPreferences withIconGenPowerRot(int iconGenPowerRot) {
        if (iconGenPowerRot == this.getIconGenPowerRot()) {
            return this;
        }
        return withField("iconGenPowerRot", iconGenPowerRot);
    }

    public EditingPreferences withIconGenGroundRot(int iconGenGroundRot) {
        if (iconGenGroundRot == this.getIconGenGroundRot()) {
            return this;
        }
        return withField("iconGenGroundRot", iconGenGroundRot);
    }

    public EditingPreferences withIconGenClockRot(int iconGenClockRot) {
        if (iconGenClockRot == this.getIconGenClockRot()) {
            return this;
        }
        return withField("iconGenClockRot", iconGenClockRot);
    }

    public EditingPreferences withIconGenReset() {
        return withIconGenLeadLength(2)
              .withIconGenLeadSpacing(2)
              .withIconGenDrawLeads(true)
              .withIconsAlwaysDrawn(false)
              .withIconGenDrawBody(true)
              .withIconGenExportPlacement(0)
              .withIconGenExportPlacementExact(true)
              .withIconGenReverseExportOrder(false)
              .withIconGenBodyTextSize(2.0)
              .withIconGenExportLocation(1)
              .withIconGenExportStyle(0)
              .withIconGenExportTech(1)
              .withIconGenInstanceLocation(0)
              .withIconGenInputRot(0)
              .withIconGenOutputRot(0)
              .withIconGenBidirRot(0)
              .withIconGenPowerRot(0)
              .withIconGenGroundRot(0)
              .withIconGenClockRot(0);
    }

    @Override
    protected EditingPreferences withField(String fieldName, Object value) {
        return (EditingPreferences) super.withField(fieldName, value);
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

    /**
     * What type of "smart" vertical text placement should be done for Exports.
     * The values can be 0: no smart placement; 1: place text "inside"; 2: place text "outside".
     * The default is 0.
     * @return the placeCellCenter
     */
    public boolean isPlaceCellCenter() {
        return placeCellCenter;
    }

    /**
     * @return the smartVerticalPlacementExport
     */
    public int getSmartVerticalPlacementExport() {
        return smartVerticalPlacementExport;
    }

    /**
     * What type of "smart" horizontal text placement should be done for Exports.
     * The values can be 0: no smart placement; 1: place text "inside"; 2: place text "outside".
     * The default is 0.
     * @return the smartHorizontalPlacementExport
     */
    public int getSmartHorizontalPlacementExport() {
        return smartHorizontalPlacementExport;
    }

    /**
     * What type of "smart" text placement should be done for vertical Arcs.
     * The values can be 0: place text inside; 1: place text to left; 2: place text to right.
     * The default is 0.
     * @return the smartVerticalPlacementArc
     */
    public int getSmartVerticalPlacementArc() {
        return smartVerticalPlacementArc;
    }

    /**
     * What type of "smart" text placement should be done for horizontal Arcs.
     * The values can be 0: place text inside; 1: place text above; 2: place text below.
     * The default is 0.
     * @return the smartHorizontalPlacementArc
     */
    public int getSmartHorizontalPlacementArc() {
        return smartHorizontalPlacementArc;
    }

    /**
     * What type of arcs are drawn: true to make them as wide as connecting nodes,
     * false to make them normal size.
     * The default is true.
     * @return the fatWires
     */
    public boolean isFatWires() {
        return fatWires;
    }

    /**
     * How long to make leads in generated icons.
     * The default is 2.
     * @return the iconGenLeadLength
     */
    public double getIconGenLeadLength() {
        return iconGenLeadLength;
    }

    /**
     * How far apart to space leads in generated icons.
     * The default is 2.
     * @return the iconGenLeadSpacing
     */
    public double getIconGenLeadSpacing() {
        return iconGenLeadSpacing;
    }

    /**
     * Whether generated icons should have leads drawn.
     * The default is "true".
     * @return the iconGenDrawLeads
     */
    public boolean isIconGenDrawLeads() {
        return iconGenDrawLeads;
    }

    /**
     * Whether generated icon exports should be "always drawn".
     * Exports that are "always drawn" have their text shown on instances, even
     * when those exports are connected or further exported.
     * The default is "false".
     * @return the iconsAlwaysDrawn
     */
    public boolean isIconsAlwaysDrawn() {
        return iconsAlwaysDrawn;
    }

    /**
     * Whether generated icons should have a body drawn.
     * The body is just a rectangle.
     * The default is "true".
     * @return the iconGenDrawBody
     */
    public boolean isIconGenDrawBody() {
        return iconGenDrawBody;
    }

    /**
     * @return the iconGenExportPlacement
     */
    public int getIconGenExportPlacement() {
        return iconGenExportPlacement;
    }

    /**
     * Whether exports are placed exactly according to schematics.
     * Only valid if icon ports are being placed by location.
     * true to place exports exactly according to schematics.
     * false: to place exports relative to their location in the original cell.
     * @return the iconGenExportPlacementExact
     */
    public boolean isIconGenExportPlacementExact() {
        return iconGenExportPlacementExact;
    }

    /**
     * Whether generated icons should reverse the order of exports.
     * Normally, exports are drawn top-to-bottom alphabetically.
     * The default is "false".
     * @return the iconGenReverseExportOrder
     */
    public boolean isIconGenReverseExportOrder() {
        return iconGenReverseExportOrder;
    }

    /**
     * The size of body text on generated icons.
     * The default is 2 unit.
     * @return the iconGenBodyTextSize
     */
    public double getIconGenBodyTextSize() {
        return iconGenBodyTextSize;
    }

    /**
     * Where exports should appear along the leads in generated icons.
     * 0: on the body   1: (the default) at the end of the lead   2: in the middle of the lead
     * @return the iconGenExportLocation
     */
    public int getIconGenExportLocation() {
        return iconGenExportLocation;
    }

    /**
     * How the text should appear in generated icons.
     * 0: (the default) centered at the export location
     * 1: pointing inward from the export location
     * 2: pointing outward from the export location
     * @return the iconGenExportStyle
     */
    public int getIconGenExportStyle() {
        return iconGenExportStyle;
    }

    /**
     * How exports should be constructed in generated icons.
     * 0: use Generic:Universal-Pins for non-bus exports (can connect to ANYTHING).
     * 1: (the default) use Schematic:Bus-Pins for exports (can connect to schematic busses or wires).
     * @return the iconGenExportTech
     */
    public int getIconGenExportTech() {
        return iconGenExportTech;
    }

    /**
     * Where to place an instance of the generated icons in the original schematic.
     * 0: (the default) in the upper-right corner.
     * 1: in the upper-left corner.
     * 2: in the lower-right corner.
     * 3: in the lower-left corner.
     * 4: no instance in the original schematic
     * @return the iconGenInstanceLocation
     */
    public int getIconGenInstanceLocation() {
        return iconGenInstanceLocation;
    }

    /**
     * What angle Input ports should go on generated icons.
     * This applies only when ports are placed by "characteristic", not "location".
     * 0: normal   1: rotate 90 degrees   2: rotate 180 degrees   3: rotate 270 degrees
     * @return the iconGenInputRot
     */
    public int getIconGenInputRot() {
        return iconGenInputRot;
    }

    /**
     * What angle Output ports should go on generated icons.
     * This applies only when ports are placed by "characteristic", not "location".
     * 0: normal   1: rotate 90 degrees   2: rotate 180 degrees   3: rotate 270 degrees
     * @return the iconGenOutputRot
     */
    public int getIconGenOutputRot() {
        return iconGenOutputRot;
    }

    /**
     * What angle Bidirectional ports should go on generated icons.
     * This applies only when ports are placed by "characteristic", not "location".
     * 0: normal   1: rotate 90 degrees   2: rotate 180 degrees   3: rotate 270 degrees
     * @return the iconGenBidirRot
     */
    public int getIconGenBidirRot() {
        return iconGenBidirRot;
    }

    /**
     * Method to tell what angle Power ports should go on generated icons.
     * This applies only when ports are placed by "characteristic", not "location".
     * 0: normal   1: rotate 90 degrees   2: rotate 180 degrees   3: rotate 270 degrees
     * @return the iconGenPowerRot
     */
    public int getIconGenPowerRot() {
        return iconGenPowerRot;
    }

    /**
     * Method to tell what angle Ground ports should go on generated icons.
     * This applies only when ports are placed by "characteristic", not "location".
     * 0: normal   1: rotate 90 degrees   2: rotate 180 degrees   3: rotate 270 degrees
     * @return the iconGenGroundRot
     */
    public int getIconGenGroundRot() {
        return iconGenGroundRot;
    }

    /**
     * Method to tell what angle Clock ports should go on generated icons.
     * This applies only when ports are placed by "characteristic", not "location".
     * 0: normal   1: rotate 90 degrees   2: rotate 180 degrees   3: rotate 270 degrees
     * @return the iconGenClockRot
     */
    public int getIconGenClockRot() {
        return iconGenClockRot;
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
