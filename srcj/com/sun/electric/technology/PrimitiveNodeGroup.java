/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PrimitiveNode.java
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
package com.sun.electric.technology;


import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.prototype.PortCharacteristic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A PrimitiveNodeGroup is a group of similar PrimitiveNodes.
 * PrimitiveNodes in a group share a list of NodeLayers. Each PrimitiveNode
 * may omit some NodeLayers of the list.
 */
public class PrimitiveNodeGroup {
    private final Technology tech;
    private final String name;
    private final Technology.NodeLayer[] nodeLayers;
    private final List<PrimitiveNode> nodes = new ArrayList<PrimitiveNode>();
    private final List<PrimitiveNode> unmodifiableNodes = Collections.unmodifiableList(nodes);
    private final Xml.PrimitiveNode n;
    private final EPoint sizeCorrector1;
    private final EPoint sizeCorrector2;
    private final String minSizeRule;
    private final double defaultWidth;
    private final double defaultHeight;
    private final EPoint fullSize;
    private final ERectangle fullRectangle;
    private final ERectangle baseRectangle;
    private final EdgeH[] elx;
    private final EdgeH[] ehx;
    private final EdgeV[] ely;
    private final EdgeV[] ehy;
    private final ArcProto[][] fullConnections;

    public List<PrimitiveNode> getNodes() {
        return unmodifiableNodes;
    }

    private PrimitiveNodeGroup(Technology tech, Xml.PrimitiveNode n, Map<String,Layer> layers, Map<String,ArcProto> arcs, Technology.DistanceContext context) {
        this.tech  = tech;
        this.n = n;
        name = n.name;
        EPoint sizeCorrector1 = n.diskOffset.get(Integer.valueOf(1));
        EPoint sizeCorrector2 = n.diskOffset.get(Integer.valueOf(2));
        if (sizeCorrector2 == null)
            sizeCorrector2 = EPoint.ORIGIN;
        if (sizeCorrector1 == null)
            sizeCorrector1 = sizeCorrector2;
        this.sizeCorrector1 = sizeCorrector1;
        this.sizeCorrector2 = sizeCorrector2;
        long lx, hx, ly, hy;
        if (n.nodeSizeRule != null) {
            hx = DBMath.lambdaToGrid(0.5*n.nodeSizeRule.width);
            lx = -hx;
            hy = DBMath.lambdaToGrid(0.5*n.nodeSizeRule.height);
            ly = -hy;
            minSizeRule = n.nodeSizeRule.rule;
        } else {
            lx = Long.MAX_VALUE;
            hx = Long.MIN_VALUE;
            ly = Long.MAX_VALUE;
            hy = Long.MIN_VALUE;
            for (int i = 0; i < n.nodeLayers.size(); i++) {
                Xml.NodeLayer nl = n.nodeLayers.get(i);
                long x, y;
                if (nl.representation == Technology.NodeLayer.BOX || nl.representation == Technology.NodeLayer.MULTICUTBOX) {
                    x = DBMath.lambdaToGrid(nl.lx.value);
                    lx = Math.min(lx, x);
                    hx = Math.max(hx, x);
                    x = DBMath.lambdaToGrid(nl.hx.value);
                    lx = Math.min(lx, x);
                    hx = Math.max(hx, x);
                    y = DBMath.lambdaToGrid(nl.ly.value);
                    ly = Math.min(ly, y);
                    hy = Math.max(hy, y);
                    y = DBMath.lambdaToGrid(nl.hy.value);
                    ly = Math.min(ly, y);
                    hy = Math.max(hy, y);
                } else {
                    for (Technology.TechPoint p: nl.techPoints) {
                        x = p.getX().getGridAdder();
                        lx = Math.min(lx, x);
                        hx = Math.max(hx, x);
                        y = p.getY().getGridAdder();
                        ly = Math.min(ly, y);
                        hy = Math.max(hy, y);
                    }
                }
            }
            minSizeRule = null;
        }
        fullRectangle = ERectangle.fromGrid(lx, ly, hx - lx, hy - ly);
        fullSize = EPoint.fromGrid((hx - lx + 1)/2, (hy - ly + 1)/2);
        nodeLayers = new Technology.NodeLayer[n.nodeLayers.size()];
        for (int i = 0; i < n.nodeLayers.size(); i++) {
            Xml.NodeLayer nl = n.nodeLayers.get(i);
            Layer layer = layers.get(nl.layer);
            Technology.TechPoint[] techPoints;
            if (nl.representation == Technology.NodeLayer.BOX || nl.representation == Technology.NodeLayer.MULTICUTBOX) {
                techPoints = new Technology.TechPoint[2];
                if (nl.lx.value > nl.hx.value || nl.lx.k > nl.hx.k ||
                    nl.ly.value > nl.hy.value || nl.ly.k > nl.hy.k)
                {
                    System.out.println("Negative-size polygon in primitive node " + tech.getTechName() + ":" + n.name +
                        ", layer " + layer.getName());
                }
                techPoints[0] = Technology.makeTechPoint(nl.lx, nl.ly, context, fullSize);
                techPoints[1] = Technology.makeTechPoint(nl.hx, nl.hy, context, fullSize);
            } else {
                techPoints = nl.techPoints.toArray(new Technology.TechPoint[nl.techPoints.size()]);
                for (int j = 0; j < techPoints.length; j++)
                    techPoints[j] = Technology.makeTechPoint(techPoints[j], fullSize);
            }
            Technology.NodeLayer nodeLayer;
            if (n.shrinkArcs) {
                if (layer.getPseudoLayer() == null)
                    layer.makePseudo();
                layer = layer.getPseudoLayer();
            }
            if (nl.representation == Technology.NodeLayer.MULTICUTBOX) {
                nodeLayer = Technology.NodeLayer.makeMulticut(layer, nl.portNum, nl.style, techPoints, nl.sizex, nl.sizey, nl.sep1d, nl.sep2d);
            }
            else if (n.specialType == PrimitiveNode.SERPTRANS)
                nodeLayer = new Technology.NodeLayer(layer, nl.portNum, nl.style, nl.representation, techPoints, nl.lWidth, nl.rWidth, nl.tExtent, nl.bExtent);
            else {
                nodeLayer = new Technology.NodeLayer(layer, nl.portNum, nl.style, nl.representation, techPoints);
            }
            nodeLayers[i] = nodeLayer;
        }
        if (n.sizeOffset != null) {
            lx += n.sizeOffset.getLowXGridOffset();
            hx -= n.sizeOffset.getHighXGridOffset();
            ly += n.sizeOffset.getLowYGridOffset();
            hy -= n.sizeOffset.getHighYGridOffset();
        }
        baseRectangle = ERectangle.fromGrid(lx, ly, hx - lx, hy - ly);
        defaultWidth = DBMath.round(n.defaultWidth.value + 2*fullSize.getLambdaX());
        defaultHeight = DBMath.round(n.defaultHeight.value + 2*fullSize.getLambdaY());
        elx = new EdgeH[n.ports.size()];
        ehx = new EdgeH[n.ports.size()];
        ely = new EdgeV[n.ports.size()];
        ehy = new EdgeV[n.ports.size()];
        fullConnections = new ArcProto[n.ports.size()][];
        for (int i = 0; i < n.ports.size(); i++) {
            Xml.PrimitivePort p = n.ports.get(i);
            if (p.lx.value > p.hx.value || p.lx.k > p.hx.k || p.ly.value > p.hy.value || p.ly.k > p.hy.k)
            {
                double lX = p.lx.value - fullSize.getLambdaX()*p.lx.k;
                double hX = p.hx.value - fullSize.getLambdaX()*p.hx.k;
                double lY = p.ly.value - fullSize.getLambdaY()*p.ly.k;
                double hY = p.hy.value - fullSize.getLambdaY()*p.hy.k;
                String explain = " (LX=" + TextUtils.formatDouble(p.lx.k/2) + "W";
                if (lX >= 0) explain += "+";
                explain += TextUtils.formatDouble(lX) + ", HX=" + TextUtils.formatDouble(p.hx.k/2) + "W";
                if (hX >= 0) explain += "+";
                explain += TextUtils.formatDouble(hX) + ", LY=" + TextUtils.formatDouble(p.ly.k/2) + "H";
                if (lY >= 0) explain += "+";
                explain += TextUtils.formatDouble(lY) + ", HY=" + TextUtils.formatDouble(p.hy.k/2) + "H";
                if (hY >= 0) explain += "+";
                explain += TextUtils.formatDouble(hY);
                explain += " but size is " + fullSize.getLambdaX()*2 + "x" + fullSize.getLambdaY()*2 + ")";
                System.out.println("Warning: port " + p.name + " in primitive " + tech.getTechName() + ":" + n.name + " has negative size" + explain);
            }
            elx[i] = Technology.makeEdgeH(p.lx, context, fullSize);
            ehx[i] = Technology.makeEdgeH(p.hx, context, fullSize);
            ely[i] = Technology.makeEdgeV(p.ly, context, fullSize);
            ehy[i] = Technology.makeEdgeV(p.hy, context, fullSize);
            fullConnections[i] = Technology.makeConnections(n.name, p.name, p.portArcs, arcs);
        }
    }

    private PrimitiveNode makePrimitiveNode(String name, int nodeIndex) {
        boolean needElectricalLayers = false;
        assert nodeLayers.length == n.nodeLayers.size();
        ArrayList<Technology.NodeLayer> visualNodeLayers = new ArrayList<Technology.NodeLayer>();
        ArrayList<Technology.NodeLayer> electricalNodeLayers = new ArrayList<Technology.NodeLayer>();
        for (int i = 0; i < n.nodeLayers.size(); i++) {
            Xml.NodeLayer nl = n.nodeLayers.get(i);
            Technology.NodeLayer nodeLayer = nodeLayers[i];
            if (nl.inNodes != null && !nl.inNodes.get(nodeIndex))
                continue;
            if (!(nl.inLayers && nl.inElectricalLayers))
                needElectricalLayers = true;
            if (nl.inLayers)
                visualNodeLayers.add(nodeLayer);
            if (nl.inElectricalLayers)
                electricalNodeLayers.add(nodeLayer);
        }

        PrimitiveNode pnp = PrimitiveNode.newInstance(name, tech, sizeCorrector1, sizeCorrector2, minSizeRule,
                defaultWidth, defaultHeight,
                fullRectangle, baseRectangle, visualNodeLayers.toArray(new Technology.NodeLayer[visualNodeLayers.size()]));
        pnp.setFunction(n.function);
        if (needElectricalLayers)
            pnp.setElectricalLayers(electricalNodeLayers.toArray(new Technology.NodeLayer[electricalNodeLayers.size()]));
        if (n.shrinkArcs) {
            pnp.setArcsWipe();
            pnp.setArcsShrink();
        }
        if (n.square)
            pnp.setSquare();
        if (n.canBeZeroSize)
            pnp.setCanBeZeroSize();
        if (n.wipes)
            pnp.setWipeOn1or2();
        if (n.lockable)
            pnp.setLockedPrim();
        if (n.edgeSelect)
            pnp.setEdgeSelect();
        if (n.skipSizeInPalette)
            pnp.setSkipSizeInPalette();
        if (n.notUsed)
            pnp.setNotUsed(true);
        if (n.lowVt)
            pnp.setNodeBit(PrimitiveNode.LOWVTBIT);
        if (n.highVt)
            pnp.setNodeBit(PrimitiveNode.HIGHVTBIT);
        if (n.nativeBit)
            pnp.setNodeBit(PrimitiveNode.NATIVEBIT);
        if (n.od18)
            pnp.setNodeBit(PrimitiveNode.OD18BIT);
        if (n.od25)
            pnp.setNodeBit(PrimitiveNode.OD25BIT);
        if (n.od33)
            pnp.setNodeBit(PrimitiveNode.OD33BIT);

        PrimitivePort[] ports = new PrimitivePort[n.ports.size()];
        for (int i = 0; i < ports.length; i++) {
            Xml.PrimitivePort p = n.ports.get(i);
            ports[i] = PrimitivePort.newInstance(tech, pnp, fullConnections[i], p.name,
                    p.portAngle, p.portRange, p.portTopology, PortCharacteristic.UNKNOWN,
                    elx[i], ely[i], ehx[i], ehy[i]);
        }
        pnp.addPrimitivePorts(ports);
        pnp.setSpecialType(n.specialType);
        switch (n.specialType) {
            case com.sun.electric.technology.PrimitiveNode.POLYGONAL:
                pnp.setHoldsOutline();
                break;
            case com.sun.electric.technology.PrimitiveNode.SERPTRANS:
                pnp.setHoldsOutline();
                pnp.setCanShrink();
                pnp.setSpecialValues(n.specialValues);
                break;
            default:
                break;
        }
        assert n.function != PrimitiveNode.Function.NODE;
        if (n.spiceTemplate != null)
            pnp.setSpiceTemplate(n.spiceTemplate);
        return pnp;
    }

    static void makePrimitiveNodes(Technology tech, Xml.PrimitiveNode n, Map<String,Layer> layers, Map<String,ArcProto> arcs, Technology.DistanceContext context) {
        PrimitiveNodeGroup group = new PrimitiveNodeGroup(tech, n, layers, arcs, context);
        if (n instanceof Xml.PrimitiveNodeGroup) {
            Xml.PrimitiveNodeGroup g = (Xml.PrimitiveNodeGroup)n;
            for (int i = 0; i < g.nodes.size(); i++) {
                String nodeName = g.nodes.get(i);
                PrimitiveNode pnp = group.makePrimitiveNode(nodeName, i);
                group.nodes.add(pnp);
                pnp.group = group;
            }
            tech.primitiveNodeGroups.add(group);
        } else {
            PrimitiveNode pnp = group.makePrimitiveNode(n.name, -1);
            if (n.oldName != null)
                tech.oldNodeNames.put(n.oldName, pnp);
        }
    }

    Xml.PrimitiveNode makeXml() {
        return n;
    }

}
