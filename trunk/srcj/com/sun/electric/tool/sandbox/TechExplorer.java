/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TechExplorer.java
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
package com.sun.electric.tool.sandbox;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Xml;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Main class of stand-alone process which loads "electric.jar (possibly with old Electric version),
 * initializes technologies and it and executes different test commands.
 */
public class TechExplorer extends ESandBox {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            String fileName = args[0];
            File electricJar = new File(fileName);
            if (!electricJar.exists())
                throw new FileNotFoundException(fileName);
            TechExplorer m = new TechExplorer(electricJar);

            InputStream commandStream = System.in;
            if (args.length >= 2) {
                commandStream = new BufferedInputStream(new FileInputStream(args[1]));
            }
            m.loop(commandStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TechExplorer(File electricJar) throws IOException, ClassNotFoundException, MalformedURLException, IllegalAccessException {
        super(electricJar.toURI().toURL());
    }

    public String[] initTechnologies(String args) throws IllegalAccessException, InvocationTargetException {
        if (Undo_changesQuiet != null)
            Undo_changesQuiet.invoke(null, Boolean.TRUE);
        if (Tool_initProjectSettings != null)
            Tool_initProjectSettings.invoke(User_getUserTool.invoke(null));
//            Tool_initAllTools.invoke(null);
        Technology_initAllTechnologies.invoke(null);
        List<String> technologies = new ArrayList<String>();
        for (Iterator<?> tit = (Iterator)Technology_getTechnologies.invoke(null); tit.hasNext(); ) {
            Object tech = tit.next();
            String techName = (String)Technology_getTechName.invoke(tech);
            technologies.add(techName);
        }
        return technologies.toArray(new String[technologies.size()]);
    }

    public void dumpAll(String fileName) throws IllegalAccessException, InvocationTargetException {
        for (Iterator<?> tit = (Iterator)Technology_getTechnologies.invoke(null); tit.hasNext(); ) {
            Object tech = tit.next();
            String techName = (String)Technology_getTechName.invoke(tech);
            System.out.println("Technology " + techName);
            Xml.Technology t = makeXml(techName);
            t.writeXml(fileName.replaceAll("lst", techName + "\\.xml"));
        }
//        if (Setting_getSettings != null) {
//            Collection<?> allSettings = (Collection)Setting_getSettings.invoke(null);
//            for (Object setting: allSettings) {
//                String xmlPath = (String)Setting_getXmlPath.invoke(setting);
//                Preferences prefs = (Preferences)Setting_prefs.get(setting);
//                String prefName = (String)Setting_getPrefName.invoke(setting);
//                String prefPath = prefs.absolutePath() + "/" + prefName;
//                Object factoryValue = Setting_getFactoryValue.invoke(setting);
//                assert xmlPath.length() > 0;
//            }
//        }
//        List<?> allPrefs = (List)Pref_allPrefs.get(null);
//        for (Object pref: allPrefs) {
//            Preferences prefs = (Preferences)Pref_prefs.get(pref);
//            String prefName = (String)Pref_getPrefName.invoke(pref);
//            String prefPath = prefs.absolutePath() + "/" + prefName;
//            boolean isMeaning = Pref_getMeaning != null && Pref_getMeaning.invoke(pref) != null;
//            Object factoryValue = Pref_getFactoryValue.invoke(pref);
//        }
    }

    public void dumpPrefs(String fileName) throws IOException {
        PrintWriter out = new PrintWriter(fileName);
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName.replaceAll("\\.lst", "\\.bin"))));
        dumpPrefs(out, dout);
        out.close();
        dout.close();
    }

    private void dumpPrefs(PrintWriter out, DataOutputStream dout) throws IOException {
        try {
            String version = (String)Version_getVersion.invoke(null).toString();
            out.println("Version " + version); dout.writeUTF(version);
            for (Iterator<?> tit = (Iterator)Technology_getTechnologies.invoke(null); tit.hasNext(); ) {
                Object tech = tit.next();
                String techName = (String)Technology_getTechName.invoke(tech);
                assert techName.length() > 0;
                out.println("Technology " + techName); dout.writeUTF(techName);

                for (Iterator<?> it = (Iterator)Technology_getLayers.invoke(tech); it.hasNext(); ) {
                    Object layer = it.next();
                    String layerName = (String)Layer_getName.invoke(layer);
                    assert layerName.length() > 0;
                    Object pseudoLayer = null;
                    if (Layer_getPseudoLayer != null)
                        pseudoLayer = Layer_getPseudoLayer.invoke(layer);
                    out.print("Layer " + layerName); dout.writeUTF(layerName);
                    String pseudoLayerName = "";
                    if (pseudoLayer != null) {
                        pseudoLayerName = (String)Layer_getName.invoke(pseudoLayer);
                        assert pseudoLayerName.length() > 0;
                        out.print(" " + pseudoLayerName);
                    }
                    dout.writeUTF(pseudoLayerName);
                    out.println();
                }
                dout.writeUTF("");
                for (Iterator<?> it = (Iterator)Technology_getArcs.invoke(tech); it.hasNext(); ) {
                    Object ap = it.next();
                    String arcName = (String)ArcProto_getName.invoke(ap);
                    out.println("Arc " + arcName); dout.writeUTF(arcName);
                }
                dout.writeUTF("");
                for (Iterator<?> it = (Iterator)Technology_getNodes.invoke(tech); it.hasNext(); ) {
                    Object pn = it.next();
                    String nodeName = (String)PrimitiveNode_getName.invoke(pn);
                    out.println("Node " + nodeName); dout.writeUTF(nodeName);
                }
                dout.writeUTF("");
            }
            dout.writeUTF("");
            if (Setting_getSettings != null) {
                Collection<?> allSettings = (Collection)Setting_getSettings.invoke(null);
                for (Object setting: allSettings) {
                    String xmlPath = (String)Setting_getXmlPath.invoke(setting);
                    Preferences prefs = (Preferences)Setting_prefs.get(setting);
                    String prefName = (String)Setting_getPrefName.invoke(setting);
                    String prefPath = prefs.absolutePath() + "/" + prefName;
                    Object factoryValue = Setting_getFactoryValue.invoke(setting);
                    out.println("Setting " + xmlPath + " " + prefPath + " <" + factoryValue + ">");
                    assert xmlPath.length() > 0;
                    dout.writeUTF(xmlPath);
                    dout.writeUTF(prefPath);
                }
            }
            dout.writeUTF("");
            List<?> allPrefs = (List)Pref_allPrefs.get(null);
            for (Object pref: allPrefs) {
                Preferences prefs = (Preferences)Pref_prefs.get(pref);
                String prefName = (String)Pref_getPrefName.invoke(pref);
                String prefPath = prefs.absolutePath() + "/" + prefName;
                boolean isMeaning = Pref_getMeaning != null && Pref_getMeaning.invoke(pref) != null;
                Object factoryValue = Pref_getFactoryValue.invoke(pref);
                out.println((isMeaning ? "Mean " : "Pref ") +  prefPath + " <" + factoryValue + ">");
                dout.writeUTF(prefPath); dout.writeBoolean(isMeaning);
            }
            dout.writeUTF("");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

 //    private void dumpShape(Object tech, Object pn) throws IllegalAccessException, InvocationTargetException, InstantiationException {
//        Object lib = Library_newInstance.invoke(null, "l", null);
//        String cellName = "a;1{lay}";
//        Object cell = null;
//        if (classCellVersionGroup != null) {
//            cell = Cell_lowLevelAllocate.invoke(null, lib);
//            Object versionGroup = CellVersionGroup_constructor.newInstance();
//            CellVersionGroup_add.invoke(versionGroup, cell);
//            Cell_lowLevelPopulate.invoke(cell, cellName);
//            Cell_lowLevelLink.invoke(cell);
//        } else {
//            cell = Cell_newInstance.invoke(null, lib, cellName);
//        }
//        Object ni = null;
//        if (NodeInst_newInstance1 != null)
//            ni = NodeInst_newInstance1.invoke(null, pn, new Point2D.Double(), Double.valueOf(10), Double.valueOf(10), cell);
//        else if (NodeInst_newInstance2 != null)
//            ni = NodeInst_newInstance2.invoke(null, pn, new Point2D.Double(), Double.valueOf(10), Double.valueOf(10), Integer.valueOf(0), cell, null);
//        Object[] polys = null;
//        if (Technology_getShapeOfNode1 != null)
//            polys = (Object[])Technology_getShapeOfNode1.invoke(tech, ni, Boolean.FALSE, Boolean.FALSE, null);
//        else if (Technology_getShapeOfNode2 != null)
//            polys = (Object[])Technology_getShapeOfNode2.invoke(tech, ni, null, null, Boolean.FALSE, Boolean.FALSE, null);
//        else if (Technology_getShapeOfNode3 != null)
//            polys = (Object[])Technology_getShapeOfNode3.invoke(tech, ni, null, null, Boolean.FALSE, Boolean.FALSE, null);
//        else if (Technology_getShapeOfNode4 != null)
//            polys = (Object[])Technology_getShapeOfNode4.invoke(tech, ni, null, null, Boolean.FALSE, Boolean.FALSE, null);
//        else if (Technology_getShapeOfNode5 != null)
//            polys = (Object[])Technology_getShapeOfNode5.invoke(tech, ni, null, Boolean.FALSE, Boolean.FALSE, null);
//        else if (Technology_getShapeOfNode6 != null)
//            polys = (Object[])Technology_getShapeOfNode6.invoke(tech, ni, null, Boolean.FALSE, Boolean.FALSE);
//        for (Object poly: polys) {
//            System.out.print("Poly " + Poly_getStyle.invoke(poly).toString());
//            Point2D[] points = (Point2D[])Poly_getPoints.invoke(poly);
//            for (Point2D p: points)
//                System.out.print(" " + p.getX() + "," + p.getY());
//            System.out.println();
//        }
//
//    }

    public Xml.Technology makeXml(String techName) throws IllegalAccessException, InvocationTargetException {
        Object tech = Technology_findTechnology.invoke(null, techName);

        Xml.Technology t = new Xml.Technology();
        t.techName = techName;
        t.className = tech.getClass().getName();
        if (t.className.equals("com.sun.electric.technology.Technology"))
            t.className = null;

        Xml.Version version;
        version = new Xml.Version();
        version.techVersion = 1;
        version.electricVersion = Technology.DISK_VERSION_1;
        t.versions.add(version);
        version = new Xml.Version();
        version.techVersion = 2;
        version.electricVersion = Technology.DISK_VERSION_2;
        t.versions.add(version);

        t.shortTechName = (String)Technology_getTechShortName.invoke(tech);
        t.description = (String)Technology_getTechDesc.invoke(tech);
        t.scaleValue = (Double)Technology_getScale.invoke(tech);
        t.scaleRelevant = (Boolean)Technology_isScaleRelevant.invoke(tech);
        t.resolutionValue = (Double)Technology_getResolution.invoke(tech);
        t.defaultFoundry = "NONE";
        if (Technology_getPrefFoundry != null)
            t.defaultFoundry = Technology_getPrefFoundry.invoke(tech).toString();
        t.minResistance = (Double)Technology_getMinResistance.invoke(tech);
        t.minCapacitance = (Double)Technology_getMinCapacitance.invoke(tech);
        int numTransparentLayers = (Integer)Technology_getNumTransparentLayers.invoke(tech);
        if (numTransparentLayers > 0) {
            Color[] colorMap = (Color[])Technology_getColorMap.invoke(tech);
            for (int i = 0; i < numTransparentLayers; i++) {
                Color transparentColor = colorMap[1 << i];
                t.transparentLayers.add(transparentColor);
            }
        }
//        makeFoundries(t, tech);

        int maxMetal = 0;
        for (Iterator<?> it = (Iterator)Technology_getLayers.invoke(tech); it.hasNext(); ) {
            Object layer = it.next();
            if (isPseudoLayer(layer)) continue;
            String layerName = (String)Layer_getName.invoke(layer);

            Xml.Layer l = new Xml.Layer();
            l.name = layerName;
            Object fun = Layer_getFunction.invoke(layer);
            l.function = fun != null ? LayerFunctions.get(fun) : Layer.Function.UNKNOWN;
            if (l.function.isMetal())
                maxMetal = Math.max(maxMetal, l.function.getLevel());
            l.extraFunction = (Integer)Layer_getFunctionExtras.invoke(layer);
            Object desc = Layer_getGraphics.invoke(layer);
            boolean displayPatterned = (Boolean)EGraphics_isPatternedOnDisplay.invoke(desc);
            boolean printPatterned = (Boolean)EGraphics_isPatternedOnPrinter.invoke(desc);
            EGraphics.Outline outlineWhenPatterned = EGraphics.Outline.NOPAT;
            if (EGraphics_getOutlined != null) {
                Object outline = EGraphics_getOutlined.invoke(desc);
                if (outline != null)
                    outlineWhenPatterned = EGraphicsOutlines.get(outline);
            } else {
                if (EGraphics_isOutlinedOnDisplay != null && (Boolean)EGraphics_isOutlinedOnDisplay.invoke(desc))
                    outlineWhenPatterned = EGraphics.Outline.PAT_S;
                if (EGraphics_isOutlinedOnPrinter != null && (Boolean)EGraphics_isOutlinedOnPrinter.invoke(desc))
                    outlineWhenPatterned = EGraphics.Outline.PAT_S;
            }
            int transparentLayer = (Integer)EGraphics_getTransparentLayer.invoke(desc);
            Color color = (Color)EGraphics_getColor.invoke(desc);
            double opacity = (Double)EGraphics_getOpacity.invoke(desc);
            boolean foreground = (Boolean)EGraphics_getForeground.invoke(desc);
            int[] pattern = (int[])EGraphics_getPattern.invoke(desc);
            EGraphics.J3DTransparencyOption mode = EGraphics.DEFAULT_MODE;
            if (Layer_getTransparencyMode != null)
                mode = EGraphics.J3DTransparencyOption.valueOf((String)Layer_getTransparencyMode.invoke(layer));
            double factor = EGraphics.DEFAULT_FACTOR;
            if (Layer_getTransparencyFactor != null)
                factor = (Double)Layer_getTransparencyFactor.invoke(layer);
            l.desc = new EGraphics(displayPatterned, printPatterned, outlineWhenPatterned,
                    transparentLayer, color.getRed(), color.getGreen(), color.getBlue(), opacity, foreground, pattern, mode, factor);
            l.thick3D = (Double)Layer_getThickness.invoke(layer);
            if (Layer_getDistance != null)
                l.height3D = (Double)Layer_getDistance.invoke(layer);
            else if (Layer_getHeight != null)
                l.height3D = (Double)Layer_getHeight.invoke(layer);
            l.cif = (String)Layer_getCIFLayer.invoke(layer);
            l.skill = (String)Layer_getSkillLayer.invoke(layer);
            l.resistance = (Double)Layer_getResistance.invoke(layer);
            l.capacitance = (Double)Layer_getCapacitance.invoke(layer);
            l.edgeCapacitance = (Double)Layer_getEdgeCapacitance.invoke(layer);
            t.layers.add(l);
        }
        if (Technology_getNumMetals != null)
            maxMetal = (Integer)Technology_getNumMetals.invoke(tech);
        t.minNumMetals = t.maxNumMetals = t.defaultNumMetals = maxMetal;

        Map<String,?> oldArcNames = Technology_getOldArcNames != null ? (Map)Technology_getOldArcNames.invoke(tech) : Collections.emptyMap();
        for (Iterator<?> it = (Iterator)Technology_getArcs.invoke(tech); it.hasNext(); ) {
            Object ap = it.next();
            String arcName = (String)ArcProto_getName.invoke(ap);

            Xml.ArcProto a = new Xml.ArcProto();
            a.name = arcName;
            for (Map.Entry<String,?> e: oldArcNames.entrySet()) {
                if (e.getValue() == ap)
                    a.oldName = e.getKey();
            }
            a.function = ArcProtoFunctions.get(ArcProto_getFunction.invoke(ap));
            a.wipable = (Boolean)ArcProto_isWipable.invoke(ap);
            a.curvable = (Boolean)ArcProto_isCurvable.invoke(ap);
            a.special = ArcProto_isSpecialArc != null && (Boolean)ArcProto_isSpecialArc.invoke(ap);
            a.skipSizeInPalette = ArcProto_isSkipSizeInPalette != null && (Boolean)ArcProto_isSkipSizeInPalette.invoke(ap);
            a.notUsed = (Boolean)ArcProto_isNotUsed.invoke(ap);
            a.extended = (Boolean)ArcProto_isExtended.invoke(ap);
            a.fixedAngle = (Boolean)ArcProto_isFixedAngle.invoke(ap);
            a.angleIncrement = (Integer)ArcProto_getAngleIncrement.invoke(ap);
            if (ERC_getAntennaRatio != null)
                a.antennaRatio = (Double)ERC_getAntennaRatio.invoke(ERC_tool.get(null), ap);
            else if (ArcProto_getAntennaRatio != null)
                a.antennaRatio = (Double)ArcProto_getAntennaRatio.invoke(ap);

            double defaultFullWidth = 0;
            if (ArcProto_getLambdaElibWidthOffset != null && ArcProto_getDefaultLambdaBaseWidth != null)
                defaultFullWidth = (Double)ArcProto_getLambdaElibWidthOffset.invoke(ap) + (Double)ArcProto_getDefaultLambdaBaseWidth.invoke(ap);
            else if (ArcProto_getDefaultLambdaFullWidth != null)
                defaultFullWidth = (Double)ArcProto_getDefaultLambdaFullWidth.invoke(ap);
            else if (ArcProto_getDefaultWidth != null)
                defaultFullWidth = (Double)ArcProto_getDefaultWidth.invoke(ap);
            double widthOffset = 0;
            if (ArcProto_getLambdaElibWidthOffset != null)
                widthOffset = (Double)ArcProto_getLambdaElibWidthOffset.invoke(ap);
            else if (ArcProto_getLambdaWidthOffset != null)
                widthOffset = (Double)ArcProto_getLambdaWidthOffset.invoke(ap);
            else if (ArcProto_getWidthOffset != null)
                widthOffset = (Double)ArcProto_getWidthOffset.invoke(ap);
            if (widthOffset != 0) {
                a.diskOffset.put(Integer.valueOf(1), round(0.5*defaultFullWidth));
                a.diskOffset.put(Integer.valueOf(2), round(0.5*(defaultFullWidth - widthOffset)));
            } else {
                a.diskOffset.put(Integer.valueOf(2), round(0.5*defaultFullWidth));
            }
            Object[] arcLayers = (Object[])ArcProto_layers.get(ap);
            for (Object arcLayer: arcLayers) {
                Xml.ArcLayer al = new Xml.ArcLayer();
                al.layer = (String)Layer_getName.invoke(TechnologyArcLayer_getLayer.invoke(arcLayer));
                al.style = PolyTypes.get(TechnologyArcLayer_getStyle.invoke(arcLayer));
                double extend = 0;
                if (TechnologyArcLayer_getGridExtend != null) {
                    extend = DBMath.gridToLambda((Integer)TechnologyArcLayer_getGridExtend.invoke(arcLayer));
                } else {
                    double offset = 0;
                    if (TechnologyArcLayer_getLambdaOffset != null)
                        offset = (Double)TechnologyArcLayer_getLambdaOffset.invoke(arcLayer);
                    else if (TechnologyArcLayer_getOffset != null)
                        offset = (Double)TechnologyArcLayer_getOffset.invoke(arcLayer);
                    extend = 0.5*(defaultFullWidth - offset);
                }
                al.extend.addLambda(round(extend));
                a.arcLayers.add(al);
            }
            t.arcs.add(a);
        }

        Map<String,?> oldNodeNames = Technology_getOldNodeNames != null ? (Map)Technology_getOldNodeNames.invoke(tech) : Collections.emptyMap();
        for (Iterator<?> it = (Iterator)Technology_getNodes.invoke(tech); it.hasNext(); ) {
            Object pn = it.next();
            String nodeName = (String)PrimitiveNode_getName.invoke(pn);
            PrimitiveNode.Function fun = PrimitiveNodeFunctions.get(PrimitiveNode_getFunction.invoke(pn));
            Object[] nodeLayersArray = (Object[])PrimitiveNode_getLayers.invoke(pn);
            double defWidth = (Double)PrimitiveNode_getDefWidth.invoke(pn);
            double defHeight = (Double)PrimitiveNode_getDefHeight.invoke(pn);
            Iterator<?> ports = (Iterator)PrimitiveNode_getPorts.invoke(pn);
            if (fun == PrimitiveNode.Function.NODE && nodeLayersArray.length == 1) {
                Xml.PureLayerNode pln = new Xml.PureLayerNode();
                pln.name = nodeName;
                for (Map.Entry<String,?> e: oldNodeNames.entrySet()) {
                    if (e.getValue() == pn)
                        pln.oldName = e.getKey();
                }
                Object port = ports.next();
                pln.port = (String)PrimitivePort_getName.invoke(port);
                pln.style = PolyTypes.get(TechnologyNodeLayer_getStyle.invoke(nodeLayersArray[0]));
                pln.size.addLambda(round(defWidth));
                makePortArcs(pln.portArcs, tech, port, null);
                Xml.Layer layer = t.findLayer((String)Layer_getName.invoke(TechnologyNodeLayer_getLayer.invoke(nodeLayersArray[0])));
                layer.pureLayerNode = pln;
                continue;
            }

            Xml.PrimitiveNodeGroup ng = new Xml.PrimitiveNodeGroup();
            ng.isSingleton = true;
            Xml.PrimitiveNode n = new Xml.PrimitiveNode();
            ng.nodes.add(n);
            n.name = nodeName;
            for (Map.Entry<String,?> e: oldNodeNames.entrySet()) {
                if (e.getValue() == pn)
                    n.oldName = e.getKey();
            }
            n.function = fun;
            ng.shrinkArcs = (Boolean)PrimitiveNode_isArcsShrink.invoke(pn);
            ng.square = (Boolean)PrimitiveNode_isSquare.invoke(pn);
            ng.canBeZeroSize = (Boolean)PrimitiveNode_isCanBeZeroSize.invoke(pn);
            ng.wipes = (Boolean)PrimitiveNode_isWipeOn1or2.invoke(pn);
            ng.lockable = (Boolean)PrimitiveNode_isLockedPrim.invoke(pn);
            ng.edgeSelect = (Boolean)PrimitiveNode_isEdgeSelect.invoke(pn);
            if (PrimitiveNode_isSkipSizeInPalette != null)
                ng.skipSizeInPalette = (Boolean)PrimitiveNode_isSkipSizeInPalette.invoke(pn);
            ng.notUsed = (Boolean)PrimitiveNode_isNotUsed.invoke(pn);
            if (PrimitiveNode_LOWVTBIT != null)
                n.lowVt = (Boolean)PrimitiveNode_isNodeBitOn.invoke(pn, PrimitiveNode_LOWVTBIT.get(null));
            if (PrimitiveNode_HIGHVTBIT != null)
                n.highVt = (Boolean)PrimitiveNode_isNodeBitOn.invoke(pn, PrimitiveNode_HIGHVTBIT.get(null));
            if (PrimitiveNode_NATIVEBIT != null)
                n.nativeBit = (Boolean)PrimitiveNode_isNodeBitOn.invoke(pn, PrimitiveNode_NATIVEBIT.get(null));
            if (PrimitiveNode_OD18BIT != null)
                n.od18 = (Boolean)PrimitiveNode_isNodeBitOn.invoke(pn, PrimitiveNode_OD18BIT.get(null));
            if (PrimitiveNode_OD25BIT != null)
                n.od25 = (Boolean)PrimitiveNode_isNodeBitOn.invoke(pn, PrimitiveNode_OD25BIT.get(null));
            if (PrimitiveNode_OD33BIT != null)
                n.od33 = (Boolean)PrimitiveNode_isNodeBitOn.invoke(pn, PrimitiveNode_OD33BIT.get(null));

            EPoint sizeCorrector1 = null;
            EPoint sizeCorrector2 = null;
            if (PrimitiveNode_getSizeCorrector != null) {
                sizeCorrector1 = EPoint.snap((Point2D)PrimitiveNode_getSizeCorrector.invoke(pn, 0));
                sizeCorrector2 = EPoint.snap((Point2D)PrimitiveNode_getSizeCorrector.invoke(pn, 1));
            }
            double minWidth = 0, minHeight = 0;
            String minSizeRule = null;
            if (classPrimitiveNodeNodeSizeRule != null) {
                Object rule = PrimitiveNode_getMinSizeRule.invoke(pn);
                if (rule != null) {
                    minWidth = (Double)PrimitiveNodeNodeSizeRule_getWidth.invoke(rule);
                    minHeight = (Double)PrimitiveNodeNodeSizeRule_getHeight.invoke(rule);
                    minSizeRule = (String)PrimitiveNodeNodeSizeRule_getRuleName.invoke(rule);
                }
            } else {
                minWidth = (Double)PrimitiveNode_getMinWidth.invoke(pn);
                minHeight = (Double)PrimitiveNode_getMinHeight.invoke(pn);
                minSizeRule = (String)PrimitiveNode_getMinSizeRule.invoke(pn);
                if (minWidth == -1 && minHeight == -1 && minSizeRule.equals(""))
                    minSizeRule = null;
            }
            EPoint fullSize = null;
            if (minSizeRule != null) {
                ng.nodeSizeRule = new Xml.NodeSizeRule();
                ng.nodeSizeRule.width = minWidth;
                ng.nodeSizeRule.height = minHeight;
                ng.nodeSizeRule.rule = minSizeRule;
                fullSize = EPoint.fromLambda(0.5*minWidth, 0.5*minHeight);
            } else if (PrimitiveNode_getFullRectangle != null) {
                Rectangle2D r = (Rectangle2D)PrimitiveNode_getFullRectangle.invoke(pn);
                fullSize = EPoint.fromLambda(0.5*r.getWidth(), 0.5*r.getHeight());
            } else {
                fullSize = sizeCorrector1;
            }
            if (fullSize == null)
                fullSize = EPoint.fromLambda(0.5*defWidth, 0.5*defHeight);
            ERectangle fullRectangle = ERectangle.fromLambda(-fullSize.getX(), -fullSize.getY(),
                    2*fullSize.getX(), 2*fullSize.getY());
            ERectangle baseRectangle;
            if (PrimitiveNode_getBaseRectangle != null) {
                baseRectangle = ERectangle.fromLambda((Rectangle2D)PrimitiveNode_getBaseRectangle.invoke(pn));
            } else {
                double lx = fullRectangle.getLambdaMinX();
                double hx = fullRectangle.getLambdaMaxX();
                double ly = fullRectangle.getLambdaMinY();
                double hy = fullRectangle.getLambdaMaxY();
                Object sizeOffset = PrimitiveNode_getProtoSizeOffset.invoke(pn);
                if (sizeOffset != null) {
                    lx += (Double)SizeOffset_getLowXOffset.invoke(sizeOffset);
                    hx -= (Double)SizeOffset_getHighXOffset.invoke(sizeOffset);
                    ly += (Double)SizeOffset_getLowYOffset.invoke(sizeOffset);
                    hy -= (Double)SizeOffset_getHighYOffset.invoke(sizeOffset);
                }
                baseRectangle = ERectangle.fromLambda(lx, ly, hx - lx, hy - ly);
            }
            ng.baseLX.value = baseRectangle.getLambdaMinX();
            ng.baseHX.value = baseRectangle.getLambdaMaxX();
            ng.baseLY.value = baseRectangle.getLambdaMinY();
            ng.baseHY.value = baseRectangle.getLambdaMaxY();
//            if (!baseRectangle.equals(fullRectangle)) {
//                n.sizeOffset = new SizeOffset(
//                    baseRectangle.getLambdaMinX() - fullRectangle.getLambdaMinX(),
//                    fullRectangle.getLambdaMaxX() - baseRectangle.getLambdaMaxX(),
//                    baseRectangle.getLambdaMinY() - fullRectangle.getLambdaMinY(),
//                    fullRectangle.getLambdaMaxY() - baseRectangle.getLambdaMaxY());
//            }
            if (sizeCorrector1 == null)
                sizeCorrector1 = fullSize;
            if (sizeCorrector2 == null)
                sizeCorrector2 = EPoint.fromGrid(baseRectangle.getGridWidth() >> 1, baseRectangle.getGridHeight() >> 1);
            if (!sizeCorrector2.equals(sizeCorrector1))
                ng.diskOffset.put(Integer.valueOf(1), sizeCorrector1);
            if (!sizeCorrector2.equals(EPoint.ORIGIN))
                ng.diskOffset.put(Integer.valueOf(2), sizeCorrector2);
            ng.defaultWidth.addLambda(round(defWidth - fullRectangle.getLambdaWidth()));
            ng.defaultHeight.addLambda(round(defHeight - fullRectangle.getLambdaHeight()));

            List<?> nodeLayers = Arrays.asList(nodeLayersArray);
            Object[] electricalNodeLayersArray = (Object[])PrimitiveNode_getElectricalLayers.invoke(pn);
            List<?> electricalNodeLayers = nodeLayers;
            if (electricalNodeLayersArray != null)
                electricalNodeLayers = Arrays.asList(electricalNodeLayersArray);
            boolean isSerp = (Integer)PrimitiveNode_getSpecialType.invoke(pn) == PrimitiveNode.SERPTRANS;
            int m = 0;
            for (Object nld: electricalNodeLayers) {
                int j = nodeLayers.indexOf(nld);
                if (j < 0) {
                    ng.nodeLayers.add(makeNodeLayerDetails(t, nld, isSerp, fullSize, false, true));
                    continue;
                }
                while (m < j)
                    ng.nodeLayers.add(makeNodeLayerDetails(t, nodeLayers.get(m++), isSerp, fullSize, true, false));
                ng.nodeLayers.add(makeNodeLayerDetails(t, nodeLayers.get(m++), isSerp, fullSize, true, true));
            }
            while (m < nodeLayers.size())
                ng.nodeLayers.add(makeNodeLayerDetails(t, nodeLayers.get(m++), isSerp, fullSize, true, false));

            for (Iterator<?> pit = (Iterator)PrimitiveNode_getPorts.invoke(pn); pit.hasNext(); ) {
                Object pp = pit.next();
                Xml.PrimitivePort ppd = new Xml.PrimitivePort();
                ppd.name = (String)PrimitivePort_getName.invoke(pp);
                ppd.portAngle = (Integer)PrimitivePort_getAngle.invoke(pp);
                if (PrimitivePort_getAngleRange != null) {
                    ppd.portRange = (Integer)PrimitivePort_getAngleRange.invoke(pp);
                } else {
                    /** range of valid angles about port angle */		final int PORTARANGE =           0377000;
                    /** right shift of PORTARANGE field */				final int PORTARANGESH =               9;
                    ppd.portRange = ((Integer)PrimitivePort_lowLevelGetUserbits.invoke(pp) & PORTARANGE) >> PORTARANGESH;
                }
                ppd.portTopology = (Integer)PrimitivePort_getTopology.invoke(pp);

                Object lx = PrimitivePort_getLeft.invoke(pp);
                Object hx = PrimitivePort_getRight.invoke(pp);
                Object ly = PrimitivePort_getBottom.invoke(pp);
                Object hy = PrimitivePort_getTop.invoke(pp);
                ppd.lx.k = (Double)EdgeH_getMultiplier.invoke(lx)*2;
                ppd.lx.addLambda(round((Double)EdgeH_getAdder.invoke(lx) + fullSize.getLambdaX()*ppd.lx.k));
                ppd.hx.k = (Double)EdgeH_getMultiplier.invoke(hx)*2;
                ppd.hx.addLambda(round((Double)EdgeH_getAdder.invoke(hx) + fullSize.getLambdaX()*ppd.hx.k));
                ppd.ly.k = (Double)EdgeV_getMultiplier.invoke(ly)*2;
                ppd.ly.addLambda(round((Double)EdgeV_getAdder.invoke(ly) + fullSize.getLambdaY()*ppd.ly.k));
                ppd.hy.k = (Double)EdgeV_getMultiplier.invoke(hy)*2;
                ppd.hy.addLambda(round((Double)EdgeV_getAdder.invoke(hy) + fullSize.getLambdaY()*ppd.hy.k));

                makePortArcs(ppd.portArcs, tech, pp, null);
                ng.ports.add(ppd);
            }
            ng.specialType = (Integer)PrimitiveNode_getSpecialType.invoke(pn);
            double[] specialValues = (double[])PrimitiveNode_getSpecialValues.invoke(pn);
            if (specialValues != null)
                ng.specialValues = specialValues.clone();
            if (PrimitiveNode_getSpiceTemplate != null)
                ng.spiceTemplate = (String)PrimitiveNode_getSpiceTemplate.invoke(pn);
            t.nodeGroups.add(ng);
        }

        addSpiceHeader(t, 1, (String[])Technology_getSpiceHeaderLevel1.invoke(tech));
        addSpiceHeader(t, 2, (String[])Technology_getSpiceHeaderLevel2.invoke(tech));
        addSpiceHeader(t, 3, (String[])Technology_getSpiceHeaderLevel3.invoke(tech));

        if (Technology_getNodesGrouped1 != null || Technology_getNodesGrouped2 != null) {
            Object[][] origPalette = null;
            if (Technology_getNodesGrouped1 != null)
                origPalette = (Object[][])Technology_getNodesGrouped1.invoke(tech);
            else if (Technology_getNodesGrouped2 != null)
                origPalette = (Object[][])Technology_getNodesGrouped2.invoke(tech, (Object)null);
            if (origPalette != null) {
                int numRows = origPalette.length;
                int numCols = origPalette[0].length;
                for (Object[] row: origPalette) {
                    assert row.length == numCols;
                }
                t.menuPalette = new Xml.MenuPalette();
                t.menuPalette.numColumns = numCols;
                for (int row = 0; row < numRows; row++) {
                    for (int col = 0; col < numCols; col++) {
                        Object origEntry = origPalette[row][col];
                        Object newEntry = null;
                        ArrayList<Object> newBox = new ArrayList<Object>();
                        if (origEntry instanceof List) {
                            List<?> list = (List<?>)origEntry;
                            for (Object o: list) {
                                if (o instanceof List) {
                                    List<?> list2 = (List<?>)o;
                                    for (Object o2: list2)
                                        newBox.add(makeMenuEntry(t, o2));
                                } else {
                                    newBox.add(makeMenuEntry(t, o));
                                }
                            }
                        } else if (origEntry != null) {
                            newBox.add(makeMenuEntry(t, origEntry));
                        }
                        t.menuPalette.menuBoxes.add(newBox);
                    }
                }
            }
        }
        makeFoundries(t, tech);

        return t;
    }

    private void makeFoundries(Xml.Technology t, Object tech) throws IllegalAccessException, InvocationTargetException {
        if (Technology_getFoundries == null) return;

        Object foundries = Technology_getFoundries.invoke(tech);
        Iterator<?> fit = foundries instanceof List ? ((List)foundries).iterator() : (Iterator)foundries;
        for (; fit.hasNext(); ) {
            Object foundry = fit.next();
            Xml.Foundry f = new Xml.Foundry();
            f.name = foundry.toString();
            if (Foundry_getGDSLayers != null) {
                Map<?,String> gdsMap = (Map)Foundry_getGDSLayers.invoke(foundry);
                for (Map.Entry<?,String> e: gdsMap.entrySet()) {
                    String gds = e.getValue();
                    if (gds.length() == 0) continue;
                    Object layer = e.getKey();
                    f.layerGds.put((String)Layer_getName.invoke(layer), gds);
                }
            }

            List<?> rules = (List)Foundry_getRules.invoke(foundry);
            if (rules != null) {
                for (Object rule: rules) {
                    String ruleName = (String)DRCTemplate_ruleName.get(rule);
                    int when = (Integer)DRCTemplate_when.get(rule);
                    final int TSMC = 010000;
                    final int ST = 020000;
                    final int MOSIS = 040000;
                    when = when & ~(TSMC|ST|MOSIS);
                    if (classDRCTemplateDRCMode != null) {
                        int newWhen = 0;
                        for (Map.Entry<Object,DRCTemplate.DRCMode> e: DRCTemplateDRCModes.entrySet()) {
                            int oldMode = (Integer)DRCTemplateDrcMode_mode.invoke(e.getKey());
                            if ((when & oldMode) == oldMode)
                                newWhen |= e.getValue().mode();
                        }
                        when = newWhen;
                    }
                    DRCTemplate.DRCRuleType type = DRCTemplateDRCRuleTypes.get(DRCTemplate_ruleType.get(rule));
                    if (type == null)
                        continue;
                    double maxWidth = (Double)DRCTemplate_maxWidth.get(rule);
                    double minLength = (Double)DRCTemplate_minLength.get(rule);
                    String name1 = (String)DRCTemplate_name1.get(rule);
                    String name2 = (String)DRCTemplate_name2.get(rule);
                    double[] values = null;
                    if (DRCTemplate_values != null) {
                        values = (double[])DRCTemplate_values.get(rule);
                    } else if (DRCTemplate_value1 != null & DRCTemplate_value2 != null) {
                        values = new double[2];
                        values[0] = (Double)DRCTemplate_value1.get(rule);
                        values[1] = (Double)DRCTemplate_value2.get(rule);
                    }
                    values = values.clone();
                    String nodeName = (String)DRCTemplate_nodeName.get(rule);
                    int multiCuts = (Integer)DRCTemplate_multiCuts.get(rule);
                    DRCTemplate r = null;
                    if (nodeName != null)
                        r = new DRCTemplate(ruleName, when, type, name1, name2, values, nodeName, null);
                    else
                        r = new DRCTemplate(ruleName, when, type, maxWidth, minLength, name1, name2, values, multiCuts);
                    f.rules.add(r);
                }
                t.foundries.add(f);
            }
        }

    }

    private Xml.NodeLayer makeNodeLayerDetails(Xml.Technology t, Object nodeLayer, boolean isSerp, EPoint correction, boolean inLayers, boolean inElectricalLayers)
    throws IllegalAccessException, InvocationTargetException {
        Xml.NodeLayer nld = new Xml.NodeLayer();
        Object layer = TechnologyNodeLayer_getLayer.invoke(nodeLayer);
        layer = Layer_getNonPseudoLayer.invoke(layer);
        nld.layer = (String)Layer_getName.invoke(layer);
        nld.style = PolyTypes.get(TechnologyNodeLayer_getStyle.invoke(nodeLayer));
        nld.portNum = (Integer)TechnologyNodeLayer_getPortNum.invoke(nodeLayer);
        nld.inLayers = inLayers;
        nld.inElectricalLayers = inElectricalLayers;
        nld.representation = (Integer)TechnologyNodeLayer_getRepresentation.invoke(nodeLayer);
        Object[] points = (Object[])TechnologyNodeLayer_getPoints.invoke(nodeLayer);
        if (points != null) {
            if ((nld.representation == Technology.NodeLayer.BOX || nld.representation == Technology.NodeLayer.MULTICUTBOX)) {
                Object lx = TechnologyTechPoint_getX.invoke(points[0]);
                Object hx = TechnologyTechPoint_getX.invoke(points[1]);
                Object ly = TechnologyTechPoint_getY.invoke(points[0]);
                Object hy = TechnologyTechPoint_getY.invoke(points[1]);
                nld.lx.k = (Double)EdgeH_getMultiplier.invoke(lx)*2;
                nld.lx.addLambda(round((Double)EdgeH_getAdder.invoke(lx) + correction.getLambdaX()*nld.lx.k));
                nld.hx.k = (Double)EdgeH_getMultiplier.invoke(hx)*2;
                nld.hx.addLambda(round((Double)EdgeH_getAdder.invoke(hx) + correction.getLambdaX()*nld.hx.k));
                nld.ly.k = (Double)EdgeV_getMultiplier.invoke(ly)*2;
                nld.ly.addLambda(round((Double)EdgeV_getAdder.invoke(ly) + correction.getLambdaY()*nld.ly.k));
                nld.hy.k = (Double)EdgeV_getMultiplier.invoke(hy)*2;
                nld.hy.addLambda(round((Double)EdgeV_getAdder.invoke(hy) + correction.getLambdaY()*nld.hy.k));
            } else {
                for (Object p: points)
                    nld.techPoints.add(correction(p, correction));
            }
        }
        if (TechnologyNodeLayer_getMulticutSizeX != null) {
            nld.sizex = round((Double)TechnologyNodeLayer_getMulticutSizeX.invoke(nodeLayer));
            nld.sizey = round((Double)TechnologyNodeLayer_getMulticutSizeY.invoke(nodeLayer));
            nld.sep1d = round((Double)TechnologyNodeLayer_getMulticutSep1D.invoke(nodeLayer));
            nld.sep2d = round((Double)TechnologyNodeLayer_getMulticutSep2D.invoke(nodeLayer));
        }
        if (isSerp) {
            nld.lWidth = round((Double)TechnologyNodeLayer_getSerpentineLWidth.invoke(nodeLayer));
            nld.rWidth = round((Double)TechnologyNodeLayer_getSerpentineRWidth.invoke(nodeLayer));
            nld.tExtent = round((Double)TechnologyNodeLayer_getSerpentineExtentT.invoke(nodeLayer));
            nld.bExtent = round((Double)TechnologyNodeLayer_getSerpentineExtentB.invoke(nodeLayer));
        }
        return nld;
    }

    private Technology.TechPoint correction(Object p, EPoint correction) throws IllegalAccessException, InvocationTargetException {
        Object oh = TechnologyTechPoint_getX.invoke(p);
        double mx = (Double)EdgeH_getMultiplier.invoke(oh);
        EdgeH h = new EdgeH(mx, (Double)EdgeH_getAdder.invoke(oh) + correction.getLambdaX()*mx*2);
        Object ov = TechnologyTechPoint_getY.invoke(p);
        double my = (Double)EdgeV_getMultiplier.invoke(ov);
        EdgeV v = new EdgeV(my, (Double)EdgeV_getAdder.invoke(ov) + correction.getLambdaY()*my*2);
        return new Technology.TechPoint(h, v);
    }

    private static void addSpiceHeader(Xml.Technology t, int level, String[] spiceLines) {
        if (spiceLines == null) return;
        Xml.SpiceHeader spiceHeader = new Xml.SpiceHeader();
        spiceHeader.level = level;
        for (String spiceLine: spiceLines)
            spiceHeader.spiceLines.add(spiceLine);
        t.spiceHeaders.add(spiceHeader);
    }

    private Object makeMenuEntry(Xml.Technology t, Object entry) throws IllegalAccessException, InvocationTargetException {
        if (classArcProto.isInstance(entry))
            return t.findArc((String)ArcProto_getName.invoke(entry));
        if (classPrimitiveNode.isInstance(entry)) {
            PrimitiveNode.Function fun = PrimitiveNodeFunctions.get(PrimitiveNode_getFunction.invoke(entry));
            String name = (String)PrimitiveNode_getName.invoke(entry);
//            if (fun == PrimitiveNode.Function.PIN) {
//                Xml.MenuNodeInst n = new Xml.MenuNodeInst();
//                n.protoName = name;
//                n.function = PrimitiveNode.Function.PIN;
//                return n;
//            }
            return t.findNode(name);
        }
        if (classNodeInst.isInstance(entry)) {
            Xml.MenuNodeInst n = new Xml.MenuNodeInst();
            n.protoName = (String)PrimitiveNode_getName.invoke(NodeInst_getProto.invoke(entry));
            n.function = PrimitiveNodeFunctions.get(NodeInst_getFunction.invoke(entry));
            n.rotation = (Integer)NodeInst_getAngle.invoke(entry);
            for (Iterator<?> it = (Iterator)ElectricObject_getVariables.invoke(entry); it.hasNext(); ) {
                Object var = it.next();
                Object value = Variable_getObject.invoke(var);
                if (!(value instanceof String)) continue;
                n.text = (String)Variable_getObject.invoke(var);
            }
            return n;
        }
        if (entry.getClass().getName().equals("javax.swing.JPopupMenu$Separator"))
            return Technology.SPECIALMENUSEPARATOR;
        assert entry instanceof String;
        return entry;
    }

    private void makePortArcs(List<String> portArcs, Object tech, Object pp, Object excludeAp) throws IllegalAccessException, InvocationTargetException  {
        Object[] connections = (Object[])PrimitivePort_getConnections.invoke(pp);
        for (Object ap: connections) {
            if (ap == null || ap == excludeAp) continue;
            String arcName = (String)ArcProto_getName.invoke(ap);
            if (Technology_findArcProto.invoke(tech, arcName) != ap) continue;
            portArcs.add(arcName);
        }
    }

    private boolean isPseudoLayer(Object layer) throws IllegalAccessException, InvocationTargetException {
        int extraFun = (Integer)Layer_getFunctionExtras.invoke(layer);
        final int PSEUDO =       010000;
        return (extraFun & PSEUDO) != 0 || Layer_isPseudoLayer != null && (Boolean)Layer_isPseudoLayer.invoke(layer);
    }

    private static double round(double v) {
        v = DBMath.round(v);
        return v;
    }

//    public Xml.Technology makeXml(String techName) throws IllegalAccessException, InvocationTargetException {
//        Object tech = Technology_findTechnology.invoke(null, techName);
//
//        Xml.Technology t = new Xml.Technology();
//        t.techName = techName;
//        t.className = tech.getClass().getName();
//        if (t.className.equals("com.sun.electric.technology.Technology"))
//            t.className = null;
//        t.shortTechName = (String)Technology_getTechShortName.invoke(tech);
//        t.description = (String)Technology_getTechDesc.invoke(tech);
//        t.scaleValue = (Double)Technology_getScale.invoke(tech);
//        t.scaleRelevant = (Boolean)Technology_isScaleRelevant.invoke(tech);
//        t.defaultFoundry = "NONE";
//        if (Technology_getPrefFoundry != null)
//            t.defaultFoundry = Technology_getPrefFoundry.invoke(tech).toString();
//        t.minResistance = (Double)Technology_getMinResistance.invoke(tech);
//        t.minCapacitance = (Double)Technology_getMinCapacitance.invoke(tech);
//        int numTransparentLayers = (Integer)Technology_getNumTransparentLayers.invoke(tech);
//        if (numTransparentLayers > 0) {
//            Color[] colorMap = (Color[])Technology_getColorMap.invoke(tech);
//            for (int i = 0; i < numTransparentLayers; i++) {
//                Color transparentColor = colorMap[1 << i];
//                t.transparentLayers.add(transparentColor);
//            }
//        }
//        makeFoundries(t, tech);
//
//        int maxMetal = 0;
//        for (Iterator<?> it = (Iterator)Technology_getLayers.invoke(tech); it.hasNext(); ) {
//            Object layer = it.next();
//            if (isPseudoLayer(layer)) continue;
//            String layerName = (String)Layer_getName.invoke(layer);
//
//            Xml.Layer l = new Xml.Layer();
//            l.name = layerName;
//            Object fun = Layer_getFunction.invoke(layer);
//            l.function = fun != null ? LayerFunctions.get(fun) : Layer.Function.UNKNOWN;
//            if (l.function.isMetal())
//                maxMetal = Math.max(maxMetal, l.function.getLevel());
//            l.extraFunction = (Integer)Layer_getFunctionExtras.invoke(layer);
//            Object desc = Layer_getGraphics.invoke(layer);
//            boolean displayPatterned = (Boolean)EGraphics_isPatternedOnDisplay.invoke(desc);
//            boolean printPatterned = (Boolean)EGraphics_isPatternedOnPrinter.invoke(desc);
//            EGraphics.Outline outlineWhenPatterned = EGraphics.Outline.NOPAT;
//            if (EGraphics_getOutlined != null) {
//                Object outline = EGraphics_getOutlined.invoke(desc);
//                if (outline != null)
//                    outlineWhenPatterned = EGraphicsOutlines.get(outline);
//            } else {
//                if (EGraphics_isOutlinedOnDisplay != null && (Boolean)EGraphics_isOutlinedOnDisplay.invoke(desc))
//                    outlineWhenPatterned = EGraphics.Outline.PAT_S;
//                if (EGraphics_isOutlinedOnPrinter != null && (Boolean)EGraphics_isOutlinedOnPrinter.invoke(desc))
//                    outlineWhenPatterned = EGraphics.Outline.PAT_S;
//            }
//            int transparentLayer = (Integer)EGraphics_getTransparentLayer.invoke(desc);
//            Color color = (Color)EGraphics_getColor.invoke(desc);
//            double opacity = (Double)EGraphics_getOpacity.invoke(desc);
//            boolean foreground = (Boolean)EGraphics_getForeground.invoke(desc);
//            int[] pattern = (int[])EGraphics_getPattern.invoke(desc);
//            l.desc = new EGraphics(displayPatterned, printPatterned, outlineWhenPatterned,
//                    transparentLayer, color.getRed(), color.getGreen(), color.getBlue(), opacity, foreground, pattern);
//            l.thick3D = (Double)Layer_getThickness.invoke(layer);
//            if (Layer_getDistance != null)
//                l.height3D = (Double)Layer_getDistance.invoke(layer);
//            else if (Layer_getHeight != null)
//                l.height3D = (Double)Layer_getHeight.invoke(layer);
//            if (Layer_getTransparencyMode != null)
//                l.mode3D = (String)Layer_getTransparencyMode.invoke(layer);
//            if (Layer_getTransparencyFactor != null)
//                l.factor3D = (Double)Layer_getTransparencyFactor.invoke(layer);
//            l.cif = (String)Layer_getCIFLayer.invoke(layer);
//            l.skill = (String)Layer_getSkillLayer.invoke(layer);
//            l.resistance = (Double)Layer_getResistance.invoke(layer);
//            l.capacitance = (Double)Layer_getCapacitance.invoke(layer);
//            l.edgeCapacitance = (Double)Layer_getEdgeCapacitance.invoke(layer);
//            t.layers.add(l);
//        }
//        if (Technology_getNumMetals != null)
//            maxMetal = (Integer)Technology_getNumMetals.invoke(tech);
//        t.minNumMetals = t.maxNumMetals = t.defaultNumMetals = maxMetal;
//
//        HashSet<Object> arcPins = new HashSet<Object>();
//        Map<String,?> oldArcNames = Technology_getOldArcNames != null ? (Map)Technology_getOldArcNames.invoke(tech) : Collections.emptyMap();
//        for (Iterator<?> it = (Iterator)Technology_getArcs.invoke(tech); it.hasNext(); ) {
//            Object ap = it.next();
//            String arcName = (String)ArcProto_getName.invoke(ap);
//
//            Xml.ArcProto a = new Xml.ArcProto();
//            a.name = arcName;
//            for (Map.Entry<String,?> e: oldArcNames.entrySet()) {
//                if (e.getValue() == ap)
//                    a.oldName = e.getKey();
//            }
//            a.function = ArcProtoFunctions.get(ArcProto_getFunction.invoke(ap));
//            a.wipable = (Boolean)ArcProto_isWipable.invoke(ap);
//            a.curvable = (Boolean)ArcProto_isCurvable.invoke(ap);
//            a.special = ArcProto_isSpecialArc != null && (Boolean)ArcProto_isSpecialArc.invoke(ap);
//            a.skipSizeInPalette = ArcProto_isSkipSizeInPalette != null && (Boolean)ArcProto_isSkipSizeInPalette.invoke(ap);
//            a.notUsed = (Boolean)ArcProto_isNotUsed.invoke(ap);
//            a.extended = (Boolean)ArcProto_isExtended.invoke(ap);
//            a.fixedAngle = (Boolean)ArcProto_isFixedAngle.invoke(ap);
//            a.angleIncrement = (Integer)ArcProto_getAngleIncrement.invoke(ap);
//            if (ERC_getAntennaRatio != null)
//                a.antennaRatio = (Double)ERC_getAntennaRatio.invoke(ERC_tool.get(null), ap);
//            else if (ArcProto_getAntennaRatio != null)
//                a.antennaRatio = (Double)ArcProto_getAntennaRatio.invoke(ap);
//            double defaultFullWidth = 0;
//            if (ArcProto_getDefaultLambdaFullWidth != null)
//                defaultFullWidth = (Double)ArcProto_getDefaultLambdaFullWidth.invoke(ap);
//            else if (ArcProto_getDefaultWidth != null)
//                defaultFullWidth = (Double)ArcProto_getDefaultWidth.invoke(ap);
//            double widthOffset = 0;
//            if (ArcProto_getLambdaElibWidthOffset != null)
//                widthOffset = (Double)ArcProto_getLambdaElibWidthOffset.invoke(ap);
//            else if (ArcProto_getLambdaWidthOffset != null)
//                widthOffset = (Double)ArcProto_getLambdaWidthOffset.invoke(ap);
//            else if (ArcProto_getWidthOffset != null)
//                widthOffset = (Double)ArcProto_getWidthOffset.invoke(ap);
//            a.elibWidthOffset = round(widthOffset);
//            Object[] arcLayers = (Object[])ArcProto_layers.get(ap);
//            for (Object arcLayer: arcLayers) {
//                Xml.ArcLayer al = new Xml.ArcLayer();
//                al.layer = (String)Layer_getName.invoke(TechnologyArcLayer_getLayer.invoke(arcLayer));
//                al.style = PolyTypes.get(TechnologyArcLayer_getStyle.invoke(arcLayer));
//                double extend = 0;
//                if (TechnologyArcLayer_getGridExtend != null) {
//                    extend = DBMath.gridToLambda((Integer)TechnologyArcLayer_getGridExtend.invoke(arcLayer));
//                } else {
//                    double offset = 0;
//                    if (TechnologyArcLayer_getLambdaOffset != null)
//                        offset = (Double)TechnologyArcLayer_getLambdaOffset.invoke(arcLayer);
//                    else if (TechnologyArcLayer_getOffset != null)
//                        offset = (Double)TechnologyArcLayer_getOffset.invoke(arcLayer);
//                    extend = 0.5*(defaultFullWidth - offset);
//                }
//                al.extend.addLambda(round(extend));
//                a.arcLayers.add(al);
//            }
//            t.arcs.add(a);
//            a.arcPin = makeWipablePin(tech, ap, arcPins);
//        }
//
//        Map<String,?> oldNodeNames = Technology_getOldNodeNames != null ? (Map)Technology_getOldNodeNames.invoke(tech) : Collections.emptyMap();
//        for (Iterator<?> it = (Iterator)Technology_getNodes.invoke(tech); it.hasNext(); ) {
//            Object pn = it.next();
//            if (arcPins.contains(pn)) continue;
//            String nodeName = (String)PrimitiveNode_getName.invoke(pn);
//            PrimitiveNode.Function fun = PrimitiveNodeFunctions.get(PrimitiveNode_getFunction.invoke(pn));
//            Object[] nodeLayersArray = (Object[])PrimitiveNode_getLayers.invoke(pn);
//            double defWidth = (Double)PrimitiveNode_getDefWidth.invoke(pn);
//            double defHeight = (Double)PrimitiveNode_getDefHeight.invoke(pn);
//            Iterator<?> ports = (Iterator)PrimitiveNode_getPorts.invoke(pn);
//            if (fun == PrimitiveNode.Function.NODE && nodeLayersArray.length == 1) {
//                Xml.PureLayerNode pln = new Xml.PureLayerNode();
//                pln.name = nodeName;
//                for (Map.Entry<String,?> e: oldNodeNames.entrySet()) {
//                    if (e.getValue() == pn)
//                        pln.oldName = e.getKey();
//                }
//                Object port = ports.next();
//                pln.port = (String)PrimitivePort_getName.invoke(port);
//                pln.style = PolyTypes.get(TechnologyNodeLayer_getStyle.invoke(nodeLayersArray[0]));
//                pln.size.addLambda(round(defWidth));
//                makePortArcs(pln.portArcs, tech, port, null);
//                Xml.Layer layer = t.findLayer((String)Layer_getName.invoke(TechnologyNodeLayer_getLayer.invoke(nodeLayersArray[0])));
//                layer.pureLayerNode = pln;
//                continue;
//            }
//
//            Xml.PrimitiveNode n = new Xml.PrimitiveNode();
//            n.name = nodeName;
//            for (Map.Entry<String,?> e: oldNodeNames.entrySet()) {
//                if (e.getValue() == pn)
//                    n.oldName = e.getKey();
//            }
//            n.function = fun;
//            n.shrinkArcs = (Boolean)PrimitiveNode_isArcsShrink.invoke(pn);
//            n.square = (Boolean)PrimitiveNode_isSquare.invoke(pn);
//            n.canBeZeroSize = (Boolean)PrimitiveNode_isCanBeZeroSize.invoke(pn);
//            n.wipes = (Boolean)PrimitiveNode_isWipeOn1or2.invoke(pn);
//            n.lockable = (Boolean)PrimitiveNode_isLockedPrim.invoke(pn);
//            n.edgeSelect = (Boolean)PrimitiveNode_isEdgeSelect.invoke(pn);
//            if (PrimitiveNode_isSkipSizeInPalette != null)
//                n.skipSizeInPalette = (Boolean)PrimitiveNode_isSkipSizeInPalette.invoke(pn);
//            n.notUsed = (Boolean)PrimitiveNode_isNotUsed.invoke(pn);
//            if (PrimitiveNode_LOWVTBIT != null)
//                n.lowVt = (Boolean)PrimitiveNode_isNodeBitOn.invoke(pn, PrimitiveNode_LOWVTBIT.get(null));
//            if (PrimitiveNode_HIGHVTBIT != null)
//                n.highVt = (Boolean)PrimitiveNode_isNodeBitOn.invoke(pn, PrimitiveNode_HIGHVTBIT.get(null));
//            if (PrimitiveNode_NATIVEBIT != null)
//                n.nativeBit = (Boolean)PrimitiveNode_isNodeBitOn.invoke(pn, PrimitiveNode_NATIVEBIT.get(null));
//            if (PrimitiveNode_OD18BIT != null)
//                n.od18 = (Boolean)PrimitiveNode_isNodeBitOn.invoke(pn, PrimitiveNode_OD18BIT.get(null));
//            if (PrimitiveNode_OD25BIT != null)
//                n.od25 = (Boolean)PrimitiveNode_isNodeBitOn.invoke(pn, PrimitiveNode_OD25BIT.get(null));
//            if (PrimitiveNode_OD33BIT != null)
//                n.od33 = (Boolean)PrimitiveNode_isNodeBitOn.invoke(pn, PrimitiveNode_OD33BIT.get(null));
//
//            double minWidth = 0, minHeight = 0;
//            String minSizeRule = null;
//            if (classPrimitiveNodeNodeSizeRule != null) {
//                Object rule = PrimitiveNode_getMinSizeRule.invoke(pn);
//                if (rule != null) {
//                    minWidth = (Double)PrimitiveNodeNodeSizeRule_getWidth.invoke(rule);
//                    minHeight = (Double)PrimitiveNodeNodeSizeRule_getHeight.invoke(rule);
//                    minSizeRule = (String)PrimitiveNodeNodeSizeRule_getRuleName.invoke(rule);
//                }
//            } else {
//                minWidth = (Double)PrimitiveNode_getMinWidth.invoke(pn);
//                minHeight = (Double)PrimitiveNode_getMinHeight.invoke(pn);
//                minSizeRule = (String)PrimitiveNode_getMinSizeRule.invoke(pn);
//                if (minWidth == -1 && minHeight == -1 && minSizeRule.equals(""))
//                    minSizeRule = null;
//            }
//            EPoint minFullSize;
//            if (minSizeRule != null) {
//                n.nodeSizeRule = new Xml.NodeSizeRule();
//                n.nodeSizeRule.width = minWidth;
//                n.nodeSizeRule.height = minHeight;
//                n.nodeSizeRule.rule = minSizeRule;
//                minFullSize = EPoint.fromLambda(0.5*minWidth, 0.5*minHeight);
//            } else {
//                minFullSize = EPoint.fromLambda(0.5*defWidth, 0.5*defHeight);
//            }
//            n.spiceTemplate = null; // ??????????
//
//            ERectangle nodeBase;
//            if (PrimitiveNode_getBaseRectangle != null) {
//                Rectangle2D baseRectangle = (Rectangle2D)PrimitiveNode_getBaseRectangle.invoke(pn);
//                nodeBase = ERectangle.fromLambda(baseRectangle);
//            } else {
//                double lx = -minFullSize.getLambdaX();
//                double hx = minFullSize.getLambdaX();
//                double ly = -minFullSize.getLambdaY();
//                double hy = minFullSize.getLambdaY();
//                Object sizeOffset = PrimitiveNode_getProtoSizeOffset.invoke(pn);
//                if (sizeOffset != null) {
//                    lx += (Double)SizeOffset_getLowXOffset.invoke(sizeOffset);
//                    hx -= (Double)SizeOffset_getHighXOffset.invoke(sizeOffset);
//                    ly += (Double)SizeOffset_getLowYOffset.invoke(sizeOffset);
//                    hy -= (Double)SizeOffset_getHighYOffset.invoke(sizeOffset);
//                }
//                nodeBase = ERectangle.fromLambda(lx, ly, hx - lx, hy - ly);
//            }
//            n.nodeBase = nodeBase;
//            if (!minFullSize.equals(EPoint.ORIGIN))
//                n.diskOffset = minFullSize;
////            EPoint p2 = EPoint.fromGrid(nodeBase.getGridWidth() >> 1, nodeBase.getGridHeight() >> 1);
////            if (!p2.equals(minFullSize))
////                n.diskOffset.put(Integer.valueOf(1), minFullSize);
////            if (!p2.equals(EPoint.ORIGIN))
////                n.diskOffset.put(Integer.valueOf(2), p2);
//            n.defaultWidth.addLambda(round(defWidth - 2*minFullSize.getLambdaX()));
//            n.defaultHeight.addLambda(round(defHeight - 2*minFullSize.getLambdaY()));
//
//            List<?> nodeLayers = Arrays.asList(nodeLayersArray);
//            Object[] electricalNodeLayersArray = (Object[])PrimitiveNode_getElectricalLayers.invoke(pn);
//            List<?> electricalNodeLayers = nodeLayers;
//            if (electricalNodeLayersArray != null)
//                electricalNodeLayers = Arrays.asList(electricalNodeLayersArray);
//            boolean isSerp = (Integer)PrimitiveNode_getSpecialType.invoke(pn) == PrimitiveNode.SERPTRANS;
//            int m = 0;
//            for (Object nld: electricalNodeLayers) {
//                int j = nodeLayers.indexOf(nld);
//                if (j < 0) {
//                    n.nodeLayers.add(makeNodeLayerDetails(t, nld, isSerp, minFullSize, false, true));
//                    continue;
//                }
//                while (m < j)
//                    n.nodeLayers.add(makeNodeLayerDetails(t, nodeLayers.get(m++), isSerp, minFullSize, true, false));
//                n.nodeLayers.add(makeNodeLayerDetails(t, nodeLayers.get(m++), isSerp, minFullSize, true, true));
//            }
//            while (m < nodeLayers.size())
//                n.nodeLayers.add(makeNodeLayerDetails(t, nodeLayers.get(m++), isSerp, minFullSize, true, false));
//
//            for (Iterator<?> pit = (Iterator)PrimitiveNode_getPorts.invoke(pn); pit.hasNext(); ) {
//                Object pp = pit.next();
//                Xml.PrimitivePort ppd = new Xml.PrimitivePort();
//                ppd.name = (String)PrimitivePort_getName.invoke(pp);
//                ppd.portAngle = (Integer)PrimitivePort_getAngle.invoke(pp);
//                if (PrimitivePort_getAngleRange != null) {
//                    ppd.portRange = (Integer)PrimitivePort_getAngleRange.invoke(pp);
//                } else {
//                    /** range of valid angles about port angle */		final int PORTARANGE =           0377000;
//                    /** right shift of PORTARANGE field */				final int PORTARANGESH =               9;
//                    ppd.portRange = ((Integer)PrimitivePort_lowLevelGetUserbits.invoke(pp) & PORTARANGE) >> PORTARANGESH;
//                }
//                ppd.portTopology = (Integer)PrimitivePort_getTopology.invoke(pp);
//
//                Object lx = PrimitivePort_getLeft.invoke(pp);
//                Object hx = PrimitivePort_getRight.invoke(pp);
//                Object ly = PrimitivePort_getBottom.invoke(pp);
//                Object hy = PrimitivePort_getTop.invoke(pp);
//                ppd.lx.k = (Double)EdgeH_getMultiplier.invoke(lx)*2;
//                ppd.lx.addLambda(round((Double)EdgeH_getAdder.invoke(lx) + minFullSize.getLambdaX()*ppd.lx.k));
//                ppd.hx.k = (Double)EdgeH_getMultiplier.invoke(hx)*2;
//                ppd.hx.addLambda(round((Double)EdgeH_getAdder.invoke(hx) + minFullSize.getLambdaX()*ppd.hx.k));
//                ppd.ly.k = (Double)EdgeV_getMultiplier.invoke(ly)*2;
//                ppd.ly.addLambda(round((Double)EdgeV_getAdder.invoke(ly) + minFullSize.getLambdaY()*ppd.ly.k));
//                ppd.hy.k = (Double)EdgeV_getMultiplier.invoke(hy)*2;
//                ppd.hy.addLambda(round((Double)EdgeV_getAdder.invoke(hy) + minFullSize.getLambdaY()*ppd.hy.k));
//
//                makePortArcs(ppd.portArcs, tech, pp, null);
//                n.ports.add(ppd);
//            }
//            n.specialType = (Integer)PrimitiveNode_getSpecialType.invoke(pn);
//            double[] specialValues = (double[])PrimitiveNode_getSpecialValues.invoke(pn);
//            if (specialValues != null)
//                n.specialValues = specialValues.clone();
//            t.nodes.add(n);
//        }
//
//        addSpiceHeader(t, 1, (String[])Technology_getSpiceHeaderLevel1.invoke(tech));
//        addSpiceHeader(t, 2, (String[])Technology_getSpiceHeaderLevel2.invoke(tech));
//        addSpiceHeader(t, 3, (String[])Technology_getSpiceHeaderLevel3.invoke(tech));
//
//        if (Technology_getNodesGrouped1 != null || Technology_getNodesGrouped2 != null) {
//            Object[][] origPalette = null;
//            if (Technology_getNodesGrouped1 != null)
//                origPalette = (Object[][])Technology_getNodesGrouped1.invoke(tech);
//            else if (Technology_getNodesGrouped2 != null)
//                origPalette = (Object[][])Technology_getNodesGrouped2.invoke(tech, (Object)null);
//            if (origPalette != null) {
//                int numRows = origPalette.length;
//                int numCols = origPalette[0].length;
//                for (Object[] row: origPalette) {
//                    assert row.length == numCols;
//                }
//                t.menuPalette = new Xml.MenuPalette();
//                t.menuPalette.numColumns = numCols;
//                for (int row = 0; row < numRows; row++) {
//                    for (int col = 0; col < numCols; col++) {
//                        Object origEntry = origPalette[row][col];
//                        Object newEntry = null;
//                        ArrayList<Object> newBox = new ArrayList<Object>();
//                        if (origEntry instanceof List) {
//                            List<?> list = (List<?>)origEntry;
//                            for (Object o: list) {
//                                if (o instanceof List) {
//                                    List<?> list2 = (List<?>)o;
//                                    for (Object o2: list2)
//                                        newBox.add(makeMenuEntry(t, o2));
//                                } else {
//                                    newBox.add(makeMenuEntry(t, o));
//                                }
//                            }
//                        } else if (origEntry != null) {
//                            newBox.add(makeMenuEntry(t, origEntry));
//                        }
//                        t.menuPalette.menuBoxes.add(newBox);
//                    }
//                }
//            }
//        }
//
//        return t;
//    }
//
//    private void makeFoundries(Xml.Technology t, Object tech) throws IllegalAccessException, InvocationTargetException {
//        if (Technology_getFoundries == null) return;
//
//        Object foundries = Technology_getFoundries.invoke(tech);
//        Iterator<?> fit = foundries instanceof List ? ((List)foundries).iterator() : (Iterator)foundries;
//        for (; fit.hasNext(); ) {
//            Object foundry = fit.next();
//            Xml.Foundry f = new Xml.Foundry();
//            f.name = foundry.toString();
//            if (Foundry_getGDSLayers != null) {
//                Map<?,String> gdsMap = (Map)Foundry_getGDSLayers.invoke(foundry);
//                for (Map.Entry<?,String> e: gdsMap.entrySet()) {
//                    String gds = e.getValue();
//                    if (gds.length() == 0) continue;
//                    Object layer = e.getKey();
//                    f.layerGds.put((String)Layer_getName.invoke(layer), gds);
//                }
//            }
//
//            List<?> rules = (List)Foundry_getRules.invoke(foundry);
//            if (rules != null) {
//                for (Object rule: rules) {
//                    String ruleName = (String)DRCTemplate_ruleName.get(rule);
//                    int when = (Integer)DRCTemplate_when.get(rule);
//                    final int TSMC = 010000;
//                    final int ST = 020000;
//                    final int MOSIS = 040000;
//                    when = when & ~(TSMC|ST|MOSIS);
//                    if (classDRCTemplateDRCMode != null) {
//                        int newWhen = 0;
//                        for (Map.Entry<Object,DRCTemplate.DRCMode> e: DRCTemplateDRCModes.entrySet()) {
//                            int oldMode = (Integer)DRCTemplateDrcMode_mode.invoke(e.getKey());
//                            if ((when & oldMode) == oldMode)
//                                newWhen |= e.getValue().mode();
//                        }
//                        when = newWhen;
//                    }
//                    DRCTemplate.DRCRuleType type = DRCTemplateDRCRuleTypes.get(DRCTemplate_ruleType.get(rule));
//                    if (type == null)
//                        continue;
//                    double maxWidth = (Double)DRCTemplate_maxWidth.get(rule);
//                    double minLength = (Double)DRCTemplate_minLength.get(rule);
//                    String name1 = (String)DRCTemplate_name1.get(rule);
//                    String name2 = (String)DRCTemplate_name2.get(rule);
//                    double[] values = null;
//                    if (DRCTemplate_values != null) {
//                        values = (double[])DRCTemplate_values.get(rule);
//                    } else if (DRCTemplate_value1 != null & DRCTemplate_value2 != null) {
//                        values = new double[2];
//                        values[0] = (Double)DRCTemplate_value1.get(rule);
//                        values[1] = (Double)DRCTemplate_value2.get(rule);
//                    }
//                    values = values.clone();
//                    String nodeName = (String)DRCTemplate_nodeName.get(rule);
//                    int multiCuts = (Integer)DRCTemplate_multiCuts.get(rule);
//                    DRCTemplate r = null;
//                    if (nodeName != null)
//                        r = new DRCTemplate(ruleName, when, type, name1, name2, values, nodeName, null);
//                    else
//                        r = new DRCTemplate(ruleName, when, type, maxWidth, minLength, name1, name2, values, multiCuts);
//                    f.rules.add(r);
//                }
//                t.foundries.add(f);
//            }
//        }
//
//    }
//
//    private Xml.ArcPin makeWipablePin(Object tech, Object ap, HashSet<Object> arcPins) throws IllegalAccessException, InvocationTargetException {
//        for (Iterator<?> it = (Iterator)Technology_getNodes.invoke(tech); it.hasNext(); ) {
//            Object pn = it.next();
//            PrimitiveNode.Function fun = PrimitiveNodeFunctions.get(PrimitiveNode_getFunction.invoke(pn));
//            if (fun != PrimitiveNode.Function.PIN) continue;
//
//            // Single port
//            Iterator<?> ports = (Iterator)PrimitiveNode_getPorts.invoke(pn);
//            if (!ports.hasNext()) continue;
//            Object pp = ports.next();
//            if (ports.hasNext()) continue;
//            Object[] connections = (Object[])PrimitivePort_getConnections.invoke(pp);
//            if (connections.length == 0 || connections[0] != ap) continue;
//
//            // All layers are pseudo layers
//            Object[] nodeLayersArray = (Object[])PrimitiveNode_getLayers.invoke(pn);
//            boolean allPseudo = true;
//            for (Object nld: nodeLayersArray) {
//                boolean isPseudo;
//                if (TechnologyNodeLayer_isPseudoLayer != null)
//                    isPseudo = (Boolean)TechnologyNodeLayer_isPseudoLayer.invoke(nld);
//                else
//                    isPseudo = isPseudoLayer(TechnologyNodeLayer_getLayer.invoke(nld));
//                allPseudo = allPseudo && isPseudo;
//            }
//            if (!allPseudo) continue;
//
//            // Square geometry
//            Object lx = PrimitivePort_getLeft.invoke(pp);
//            Object hx = PrimitivePort_getRight.invoke(pp);
//            Object ly = PrimitivePort_getBottom.invoke(pp);
//            Object hy = PrimitivePort_getTop.invoke(pp);
//            if ((Double)EdgeH_getMultiplier.invoke(lx) != -0.5 || (Double)EdgeH_getMultiplier.invoke(hx) != 0.5) continue;
//            if ((Double)EdgeV_getMultiplier.invoke(ly) != -0.5 || (Double)EdgeV_getMultiplier.invoke(hy) != 0.5) continue;
//            double portOffset = round((Double)EdgeH_getAdder.invoke(lx));
//            if (round((Double)EdgeH_getAdder.invoke(hx)) != -portOffset) continue;
//            if (round((Double)EdgeV_getAdder.invoke(ly)) != portOffset) continue;
//            if (round((Double)EdgeV_getAdder.invoke(hy)) != -portOffset) continue;
//
//            Xml.ArcPin arcPin = new Xml.ArcPin();
//            arcPin.name = (String)PrimitiveNode_getName.invoke(pn);
//            arcPin.portName = (String)PrimitivePort_getName.invoke(pp);
//            double arcPinElibSize = 2*portOffset;
//            if (PrimitiveNode_getSizeCorrector != null && PrimitiveNode_getFullRectangle != null) {
//                Point2D sizeCorrector = (Point2D)PrimitiveNode_getSizeCorrector.invoke(pn, 0);
//                Rectangle2D fullRectangle = (Rectangle2D)PrimitiveNode_getFullRectangle.invoke(pn);
//                arcPinElibSize += 2*sizeCorrector.getX() - fullRectangle.getWidth();
//            }
//            arcPin.elibSize = DBMath.round(arcPinElibSize);
//            makePortArcs(arcPin.portArcs, tech, pp, ap);
//            arcPins.add(pn);
//            return arcPin;
//        }
//        return null;
//    }
//
//    private Xml.NodeLayer makeNodeLayerDetails(Xml.Technology t, Object nodeLayer, boolean isSerp, EPoint correction, boolean inLayers, boolean inElectricalLayers)
//    throws IllegalAccessException, InvocationTargetException {
//        Xml.NodeLayer nld = new Xml.NodeLayer();
//        Object layer = TechnologyNodeLayer_getLayer.invoke(nodeLayer);
//        layer = Layer_getNonPseudoLayer.invoke(layer);
//        nld.layer = (String)Layer_getName.invoke(layer);
//        nld.style = PolyTypes.get(TechnologyNodeLayer_getStyle.invoke(nodeLayer));
//        nld.portNum = (Integer)TechnologyNodeLayer_getPortNum.invoke(nodeLayer);
//        nld.inLayers = inLayers;
//        nld.inElectricalLayers = inElectricalLayers;
//        nld.representation = (Integer)TechnologyNodeLayer_getRepresentation.invoke(nodeLayer);
//        Object[] points = (Object[])TechnologyNodeLayer_getPoints.invoke(nodeLayer);
//        if (points != null) {
//            if ((nld.representation == Technology.NodeLayer.BOX || nld.representation == Technology.NodeLayer.MULTICUTBOX)) {
//                Object lx = TechnologyTechPoint_getX.invoke(points[0]);
//                Object hx = TechnologyTechPoint_getX.invoke(points[1]);
//                Object ly = TechnologyTechPoint_getY.invoke(points[0]);
//                Object hy = TechnologyTechPoint_getY.invoke(points[1]);
//                nld.lx.k = (Double)EdgeH_getMultiplier.invoke(lx)*2;
//                nld.lx.addLambda(round((Double)EdgeH_getAdder.invoke(lx) + correction.getLambdaX()*nld.lx.k));
//                nld.hx.k = (Double)EdgeH_getMultiplier.invoke(hx)*2;
//                nld.hx.addLambda(round((Double)EdgeH_getAdder.invoke(hx) + correction.getLambdaX()*nld.hx.k));
//                nld.ly.k = (Double)EdgeV_getMultiplier.invoke(ly)*2;
//                nld.ly.addLambda(round((Double)EdgeV_getAdder.invoke(ly) + correction.getLambdaY()*nld.ly.k));
//                nld.hy.k = (Double)EdgeV_getMultiplier.invoke(hy)*2;
//                nld.hy.addLambda(round((Double)EdgeV_getAdder.invoke(hy) + correction.getLambdaY()*nld.hy.k));
//                if (nld.representation == Technology.NodeLayer.MULTICUTBOX) {
//                    DRCTemplate sizeRule = findLayerRule(t, nld.layer, DRCTemplate.DRCRuleType.MINWID);
//                    if (sizeRule == null) {
//                        double value = round((Double)TechnologyNodeLayer_getMulticutSizeX.invoke(nodeLayer));
//                        sizeRule = makeLayerRule(t, "W_" + nld.layer, nld.layer, DRCTemplate.DRCRuleType.MINWID, value);
//                    }
//                    nld.sizeRule = makeRuleName(sizeRule);
//
//                    DRCTemplate sepRule = findLayersRule(t, nld.layer, nld.layer, DRCTemplate.DRCRuleType.CONSPA);
//                    if (sepRule == null)
//                        sepRule = findLayersRule(t, nld.layer, nld.layer, DRCTemplate.DRCRuleType.SPACING);
//                    if (sepRule == null)
//                        sepRule = findLayersRule(t, nld.layer, nld.layer, DRCTemplate.DRCRuleType.UCONSPA);
//                    if (sepRule == null) {
//                        double value = round((Double)TechnologyNodeLayer_getMulticutSep2D.invoke(nodeLayer));
//                        sepRule = makeLayersRule(t, "C_" + nld.layer + "_" + nld.layer, nld.layer, nld.layer, DRCTemplate.DRCRuleType.CONSPA, value);
//                    }
//                    nld.sepRule = makeRuleName(sepRule);
//
//                    DRCTemplate sepRule2D = findLayersRule(t, nld.layer, nld.layer, DRCTemplate.DRCRuleType.UCONSPA2D);
//                    if (sepRule2D != null)
//                        nld.sepRule2D = makeRuleName(sepRule2D);
//                }
//            } else {
//                for (Object p: points)
//                    nld.techPoints.add(correction(p, correction));
//            }
//        }
//        if (isSerp) {
//            nld.lWidth = round((Double)TechnologyNodeLayer_getSerpentineLWidth.invoke(nodeLayer));
//            nld.rWidth = round((Double)TechnologyNodeLayer_getSerpentineRWidth.invoke(nodeLayer));
//            nld.tExtent = round((Double)TechnologyNodeLayer_getSerpentineExtentT.invoke(nodeLayer));
//            nld.bExtent = round((Double)TechnologyNodeLayer_getSerpentineExtentB.invoke(nodeLayer));
//        }
//        return nld;
//    }
//
//    private DRCTemplate findLayersRule(Xml.Technology t, String layerName1, String layerName2, DRCTemplate.DRCRuleType ruleType) {
//        Xml.Foundry foundry = t.foundries.get(0);
//        for (DRCTemplate rule: foundry.rules) {
//            if (rule.ruleType == ruleType && rule.name1.equals(layerName1) && rule.name2.equals(layerName2))
//                return rule;
//        }
//        return null;
//    }
//
//    private DRCTemplate findLayerRule(Xml.Technology t, String layerName, DRCTemplate.DRCRuleType ruleType) {
//        Xml.Foundry foundry = t.foundries.get(0);
//        for (DRCTemplate rule: foundry.rules) {
//            if (rule.ruleType == ruleType && rule.name1.equals(layerName))
//                return rule;
//        }
//        return null;
//    }
//
//    private DRCTemplate makeLayersRule(Xml.Technology t, String ruleName, String layerName1, String layerName2, DRCTemplate.DRCRuleType ruleType, double value) {
//        DRCTemplate rule = null;
//        for (Xml.Foundry foundry: t.foundries) {
//            rule = new DRCTemplate(ruleName, DRCTemplate.DRCMode.ALL.mode(), ruleType,
//                    layerName1, layerName2, new double[] {value}, null, null);
//            foundry.rules.add(rule);
//        }
//        return rule;
//    }
//
//    private DRCTemplate makeLayerRule(Xml.Technology t, String ruleName, String layerName, DRCTemplate.DRCRuleType ruleType, double value) {
//        DRCTemplate rule = null;
//        for (Xml.Foundry foundry: t.foundries) {
//            rule = new DRCTemplate(ruleName, DRCTemplate.DRCMode.ALL.mode(), ruleType,
//                    layerName, null, new double[] {value}, null, null);
//            foundry.rules.add(rule);
//        }
//        return rule;
//    }
//
//    private String makeRuleName(DRCTemplate rule) {
//        String ruleName = rule.ruleName;
//        int spaceIndex = ruleName.indexOf(' ');
//        if (spaceIndex >= 0)
//            ruleName = ruleName.substring(0, spaceIndex);
//        return ruleName;
//    }
//
//    private Technology.TechPoint correction(Object p, EPoint correction) throws IllegalAccessException, InvocationTargetException {
//        Object oh = TechnologyTechPoint_getX.invoke(p);
//        double mx = (Double)EdgeH_getMultiplier.invoke(oh);
//        EdgeH h = new EdgeH(mx, (Double)EdgeH_getAdder.invoke(oh) + correction.getLambdaX()*mx*2);
//        Object ov = TechnologyTechPoint_getY.invoke(p);
//        double my = (Double)EdgeV_getMultiplier.invoke(ov);
//        EdgeV v = new EdgeV(my, (Double)EdgeV_getAdder.invoke(ov) + correction.getLambdaY()*my*2);
//        return new Technology.TechPoint(h, v);
//    }
//
//    private static void addSpiceHeader(Xml.Technology t, int level, String[] spiceLines) {
//        if (spiceLines == null) return;
//        Xml.SpiceHeader spiceHeader = new Xml.SpiceHeader();
//        spiceHeader.level = level;
//        for (String spiceLine: spiceLines)
//            spiceHeader.spiceLines.add(spiceLine);
//        t.spiceHeaders.add(spiceHeader);
//    }
//
//    private Object makeMenuEntry(Xml.Technology t, Object entry) throws IllegalAccessException, InvocationTargetException {
//        if (classArcProto.isInstance(entry))
//            return t.findArc((String)ArcProto_getName.invoke(entry));
//        if (classPrimitiveNode.isInstance(entry)) {
//            PrimitiveNode.Function fun = PrimitiveNodeFunctions.get(PrimitiveNode_getFunction.invoke(entry));
//            String name = (String)PrimitiveNode_getName.invoke(entry);
//            if (fun == PrimitiveNode.Function.PIN) {
//                Xml.MenuNodeInst n = new Xml.MenuNodeInst();
//                n.protoName = name;
//                n.function = PrimitiveNode.Function.PIN;
//                return n;
//            }
//            return t.findNode(name);
//        }
//        if (classNodeInst.isInstance(entry)) {
//            Xml.MenuNodeInst n = new Xml.MenuNodeInst();
//            n.protoName = (String)PrimitiveNode_getName.invoke(NodeInst_getProto.invoke(entry));
//            n.function = PrimitiveNodeFunctions.get(NodeInst_getFunction.invoke(entry));
//            n.rotation = (Integer)NodeInst_getAngle.invoke(entry);
//            for (Iterator<?> it = (Iterator)ElectricObject_getVariables.invoke(entry); it.hasNext(); ) {
//                Object var = it.next();
//                Object value = Variable_getObject.invoke(var);
//                if (!(value instanceof String)) continue;
//                n.text = (String)Variable_getObject.invoke(var);
//                Object td = Variable_getTextDescriptor.invoke(var);
//                n.fontSize = (Double)TextDescriptorSize_getSize.invoke(TextDescriptor_getSize.invoke(td));
//            }
//            return n;
//        }
//        if (entry.getClass().getName().equals("javax.swing.JPopupMenu$Separator"))
//            return "SEPARATOR";
//        assert entry instanceof String;
//        return entry;
//    }
//
//    private void makePortArcs(List<String> portArcs, Object tech, Object pp, Object excludeAp) throws IllegalAccessException, InvocationTargetException  {
//        Object[] connections = (Object[])PrimitivePort_getConnections.invoke(pp);
//        for (Object ap: connections) {
//            if (ap == null || ap == excludeAp) continue;
//            String arcName = (String)ArcProto_getName.invoke(ap);
//            if (Technology_findArcProto.invoke(tech, arcName) != ap) continue;
//            portArcs.add(arcName);
//        }
//    }
//
//    private boolean isPseudoLayer(Object layer) throws IllegalAccessException, InvocationTargetException {
//        int extraFun = (Integer)Layer_getFunctionExtras.invoke(layer);
//        final int PSEUDO =       010000;
//        return (extraFun & PSEUDO) != 0 || Layer_isPseudoLayer != null && (Boolean)Layer_isPseudoLayer.invoke(layer);
//    }
//
//    private static double round(double v) {
//        v = DBMath.round(v);
//        return v;
//    }
}
