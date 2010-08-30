/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VectorCache.java
 * Written by Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.geometry.bool;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellRevision;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.ImmutableIconInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.id.PrimitivePortId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.AbstractShapeBuilder;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 */
public class VectorCache {
    private static boolean DEBUG = false;
    private static final boolean USE_ELECTRICAL = false;
    private static final boolean WIPE_PINS = true;

    private Set<Layer> layers = new TreeSet<Layer>();
    private Set<Layer> badLayers = new HashSet<Layer>();
    private final Snapshot snapshot;
    private final TechPool techPool;
    private HashMap<CellId, MyVectorCell> cells = new HashMap<CellId,MyVectorCell>();
    private final PrimitivePortId busPinPortId;
    private final PrimitiveNodeId cellCenterId;
    private final PrimitiveNodeId essentialBoundsId;
    /** local shape builder */
    private final ShapeBuilder shapeBuilder = new ShapeBuilder();
    /** List of VectorManhattanBuilders */
    private final HashMap<Layer,VectorManhattanBuilder> boxBuilders = new HashMap<Layer,VectorManhattanBuilder>();

    private static final int[] NULL_INT_ARRAY = {};

    private static class CellLayer {
        final Layer layer;
        int[] boxCoords = NULL_INT_ARRAY;
        ERectangle localBounds;
        ArrayList<MyVectorPolygon> polys = new ArrayList<MyVectorPolygon>();
        
        private CellLayer(Layer layer) {
            this.layer = layer;
        }

        private void setBoxCoords(int[] boxCoords) {
            this.boxCoords = boxCoords;
            int lX = Integer.MAX_VALUE, lY = Integer.MAX_VALUE, hX = Integer.MIN_VALUE, hY = Integer.MIN_VALUE;
            for (int i = 0; i < boxCoords.length; i += 4) {
                lX = Math.min(lX, boxCoords[i + 0]);
                lY = Math.min(lY, boxCoords[i + 1]);
                hX = Math.max(hX, boxCoords[i + 2]);
                hY = Math.max(hY, boxCoords[i + 3]);
            }
            localBounds = ERectangle.fromGrid(lX, hY, hX - (long)lX, hY - (long)lY);
        }
    }

    private class MyVectorCell {
        private final TechId techId;
        private final TreeMap<Layer,CellLayer> layers = new TreeMap<Layer,CellLayer>();
        private final ArrayList<ImmutableNodeInst> subCells = new ArrayList<ImmutableNodeInst>();
        private final List<ImmutableNodeInst> unmodifiebleSubCells = Collections.unmodifiableList(subCells);

        MyVectorCell(CellId cellId) {
            CellBackup cellBackup = snapshot.getCell(cellId);
            techId = cellBackup.cellRevision.d.techId;
            Technology tech = techPool.getTech(techId);
            if (isCellParameterized(cellBackup.cellRevision)) {
                throw new IllegalArgumentException();
            }

            long startTime = DEBUG ? System.currentTimeMillis() : 0;

            for (VectorManhattanBuilder b : boxBuilders.values()) {
                b.clear();
            }
            // draw all arcs
            shapeBuilder.setup(cellBackup, Orientation.IDENT, USE_ELECTRICAL, WIPE_PINS, false, null);
            shapeBuilder.polyLayer = null;
            for (Layer layer: tech.getLayersSortedByHeight()) {
                if (layer.getFunction() == Layer.Function.POLY1) {
                    shapeBuilder.polyLayer = layer;
                }
            }
            for (ImmutableArcInst a: cellBackup.cellRevision.arcs) {
                shapeBuilder.genShapeOfArc(a);
            }

            // draw all primitive nodes
            for (ImmutableNodeInst n: cellBackup.cellRevision.nodes) {
                if (n.protoId instanceof CellId) {
                    if (!n.orient.isManhattan()) {
                        throw new IllegalArgumentException();
                    }
                    subCells.add(n);
                } else {
                    boolean hideOnLowLevel = n.is(ImmutableNodeInst.VIS_INSIDE) || n.protoId  == cellCenterId ||
                            n.protoId == essentialBoundsId;
                    if (!hideOnLowLevel) {
                        PrimitiveNode pn = techPool.getPrimitiveNode((PrimitiveNodeId)n.protoId);
                        shapeBuilder.genShapeOfNode(n);
                    }
                }
            }

            addBoxesFromBuilder(this, boxBuilders);

            if (DEBUG) {
                long stopTime = System.currentTimeMillis();
                System.out.println((stopTime - startTime) + " init " + cellBackup.cellRevision.d.cellId);
            }
        }

        private CellLayer getCellLayer(Layer layer) {
            CellLayer cellLayer = layers.get(layer);
            if (cellLayer == null) {
                cellLayer = new CellLayer(layer);
                layers.put(layer, cellLayer);
            }
            return cellLayer;
        }

    }

    public Collection<Layer> getLayers() {
        return new ArrayList<Layer>(layers);
    }

    public Collection<Layer> getBadLayers() {
        return new ArrayList<Layer>(badLayers);
    }

    public boolean isBadLayer(Layer layer) {
        return badLayers.contains(layer);
    }

    public VectorCache(Snapshot snapshot) {
        this.snapshot = snapshot;
        techPool = snapshot.getTechPool();
        busPinPortId = techPool.getSchematics().busPinNode.getPort(0).getId();
        cellCenterId = techPool.getGeneric().cellCenterNode.getId();
        essentialBoundsId = techPool.getGeneric().essentialBoundsNode.getId();
    }

    public void scanLayers(CellId topCellId) {
        HashSet<CellId> visited = new HashSet<CellId>();
        scanLayers(topCellId, visited);
    }

    public List<ImmutableNodeInst> getSubcells(CellId cellId) {
        return findVectorCell(cellId).unmodifiebleSubCells;
    }
    
    public int getNumBoxes(CellId cellId, Layer layer) {
        CellLayer cellLayer = findVectorCell(cellId).layers.get(layer);
        return cellLayer != null ? cellLayer.boxCoords.length/4 : 0;
    }

    public int getNumFlatBoxes(CellId cellId, Layer layer) {
        return getNumFlatBoxes(cellId, layer, new HashMap<CellId,Integer>());
    }

    public ERectangle getLocalBounds(CellId cellId, Layer layer) {
        CellLayer cellLayer = findVectorCell(cellId).layers.get(layer);
        return cellLayer != null ? cellLayer.localBounds : null;
    }

    public void getBoxes(CellId cellId, Layer layer, int offset, int size, int[] result) {
        CellLayer cellLayer = findVectorCell(cellId).layers.get(layer);
        if (cellLayer == null || offset < 0 || size < 0 || offset + size > cellLayer.boxCoords.length/4 || size > result.length/4) {
            throw new IndexOutOfBoundsException();
        }
        System.arraycopy(cellLayer.boxCoords, offset*4, result, 0, size*4);
    }

    public static interface PutRectangle {
        public void put(int lx, int ly, int hx, int hy);
    }

    public void collectLayer(Layer layer, CellId cellId, boolean rotate, PutRectangle putRectangle) {
        Orientation orient = (rotate ? Orientation.XR : Orientation.IDENT).canonic();
        collectLayer(layer, findVectorCell(cellId), new Point(0, 0), orient, putRectangle);
    }

    private void scanLayers(CellId cellId, HashSet<CellId> visited) {
        if (!visited.add(cellId))
            return;
        MyVectorCell mvc = findVectorCell(cellId);
        for (ImmutableNodeInst n: mvc.subCells) {
            scanLayers((CellId)n.protoId, visited);
        }
    }

    private MyVectorCell findVectorCell(CellId cellId) {
        MyVectorCell mvc = cells.get(cellId);
        if (mvc == null) {
            mvc = new MyVectorCell(cellId);
            cells.put(cellId, mvc);
        }
        return mvc;
    }

    private int getNumFlatBoxes(CellId cellId, Layer layer, HashMap<CellId,Integer> numFlatBoxes) {
        Integer num = numFlatBoxes.get(cellId);
        if (num == null) {
            int count = getNumBoxes(cellId, layer);
            for (ImmutableNodeInst n: getSubcells(cellId)) {
                count += getNumFlatBoxes((CellId)n.protoId, layer, numFlatBoxes);
            }
            num = Integer.valueOf(count);
            numFlatBoxes.put(cellId, num);
        }
        return num.intValue();
    }

    private void addBoxesFromBuilder(MyVectorCell vc, HashMap<Layer,VectorManhattanBuilder> boxBuilders) {
        for (Map.Entry<Layer,VectorManhattanBuilder> e: boxBuilders.entrySet()) {
            Layer layer = e.getKey();
            VectorManhattanBuilder b = e.getValue();
            if (b.size == 0) {
                continue;
            }
            CellLayer cellLayer = vc.getCellLayer(layer);
            assert cellLayer.boxCoords.length == 0;
            cellLayer.setBoxCoords(b.toArray());
        }
    }

    private void collectLayer(Layer layer, MyVectorCell vc, Point anchor, Orientation orient, PutRectangle putRectangle) {
        int[] coords = new int[4];
        CellLayer cellLayer = vc.layers.get(layer);
        if (cellLayer != null) {
            int[] boxCoords = cellLayer.boxCoords;
            for (int i = 0; i < boxCoords.length; i += 4) {
                coords[0] = boxCoords[i + 0];
                coords[1] = boxCoords[i + 1];
                coords[2] = boxCoords[i + 2];
                coords[3] = boxCoords[i + 3];
                orient.rectangleBounds(coords);

                int lx = anchor.x + coords[0];
                int ly = anchor.y + coords[1];
                int hx = anchor.x + coords[2];
                int hy = anchor.y + coords[3];
                assert lx <= hx && ly <= hy;

                putRectangle.put(lx, ly, hx, hy);
            }
        }
        for (ImmutableNodeInst n: vc.subCells) {
            assert n.orient.isManhattan();
            coords[0] = (int)n.anchor.getGridX();
            coords[1] = (int)n.anchor.getGridY();
            orient.transformPoints(1, coords);
            Orientation subOrient = orient.concatenate(n.orient).canonic();
            MyVectorCell subCell = findVectorCell((CellId)n.protoId);
            collectLayer(layer, subCell, new Point(anchor.x + coords[0], anchor.y + coords[1]), subOrient, putRectangle);
        }
    }

    /**
     * Method to tell whether a Cell is parameterized.
     * Code is taken from tool.drc.Quick.checkEnumerateProtos
     * Could also use the code in tool.io.output.Spice.checkIfParameterized
     * @param cellRevision the Cell to examine
     * @return true if the cell has parameters
     */
    private boolean isCellParameterized(CellRevision cellRevision) {
        if (cellRevision.d.getNumParameters() > 0) {
            return true;
        }

        // look for any Java coded stuff (Logical Effort calls)
        for (ImmutableNodeInst n : cellRevision.nodes) {
            if (n instanceof ImmutableIconInst) {
                for (Iterator<Variable> vIt = ((ImmutableIconInst) n).getDefinedParameters(); vIt.hasNext();) {
                    Variable var = vIt.next();
                    if (var.isCode()) {
                        return true;
                    }
                }
            }
            for (Iterator<Variable> vIt = n.getVariables(); vIt.hasNext();) {
                Variable var = vIt.next();
                if (var.isCode()) {
                    return true;
                }
            }
        }
        for (ImmutableArcInst a : cellRevision.arcs) {
            for (Iterator<Variable> vIt = a.getVariables(); vIt.hasNext();) {
                Variable var = vIt.next();
                if (var.isCode()) {
                    return true;
                }
            }
        }

        // bus pin appearance depends on parent Cell
        for (ImmutableExport e : cellRevision.exports) {
            if (e.originalPortId == busPinPortId) {
                return true;
            }
        }
        return false;
    }

    private class ShapeBuilder extends AbstractShapeBuilder {
        private Layer polyLayer;

        @Override
        public void pushPoly(Poly.Type style, Layer layer, EGraphics graphicsOverride, PrimitivePort pp) {
            if (layer.getFunction() == Layer.Function.GATE && polyLayer != null) {
                layer = polyLayer;
            }
            super.pushPoly(style, layer, null, null);
        }

        @Override
        public void addDoublePoly(int numPoints, Poly.Type style, Layer layer, EGraphics graphicsOverride, PrimitivePort pp) {
            if (numPoints == 2)
                return;
            if (layer.isPseudoLayer()) {
                return;
            }
            badLayer(layer);
        }

        @Override
        public void addDoubleTextPoly(int numPoints, Poly.Type style, Layer layer, PrimitivePort pp, String message, TextDescriptor descriptor) {
            badLayer(layer);
        }

        @Override
        public void addIntPoly(int numPoints, Poly.Type style, Layer layer, EGraphics graphicsOverride, PrimitivePort pp) {
            if (numPoints == 2)
                return;
            badLayer(layer);
        }

        @Override
        public void addIntBox(int[] coords, Layer layer) {
            layers.add(layer);
            // convert coordinates
            int lX = coords[0];
            int lY = coords[1];
            int hX = coords[2];
            int hY = coords[3];
            putBox(layer, boxBuilders, lX, lY, hX, hY);
        }
    }

    private void badLayer(Layer layer) {
        if (badLayers.add(layer))
            layer = layer;
    }

    private static void putBox(Layer layer, HashMap<Layer,VectorManhattanBuilder> boxBuilders, int lX, int lY, int hX, int hY) {
        VectorManhattanBuilder b = boxBuilders.get(layer);
        if (b == null) {
            b = new VectorManhattanBuilder();
            boxBuilders.put(layer, b);
        }
        assert lX <= hX && lY <= hY;
        if (lX < hX && lY < hY)
            b.add(lX, lY, hX, hY);
    }
    
    static class MyVectorPolygon {
        final Layer layer;
        final Poly.Type style;
        final Point2D[] points;

        private MyVectorPolygon(Poly.Type style, Layer layer, Point2D[] points) {
            this.layer = layer;
            this.style = style;
            this.points = points;
        }
    }

    /**
     * Class which collects boxes for VectorManhattan.
     */
    static class VectorManhattanBuilder {

        /** Number of boxes. */
        int size; // number of boxes
        /** Coordiantes of boxes. */
        int[] coords = new int[4];

        private void add(int lX, int lY, int hX, int hY) {
            if (size * 4 >= coords.length) {
                int[] newCoords = new int[coords.length * 2];
                System.arraycopy(coords, 0, newCoords, 0, coords.length);
                coords = newCoords;
            }
            int i = size * 4;
            coords[i] = lX;
            coords[i + 1] = lY;
            coords[i + 2] = hX;
            coords[i + 3] = hY;
            size++;
        }

        int[] toArray() {
            int[] a = new int[size * 4];
            System.arraycopy(coords, 0, a, 0, a.length);
            return a;
        }

        private void clear() {
            size = 0;
        }
    }
}
