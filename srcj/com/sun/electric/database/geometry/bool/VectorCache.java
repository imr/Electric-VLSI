/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VectorCache.java
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
package com.sun.electric.database.geometry.bool;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellRevision;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.ImmutableIconInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
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
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 */
public class VectorCache {
    private static boolean DEBUG = false;
    private static final boolean USE_ELECTRICAL = false;
    private static final boolean WIPE_PINS = true;

    private Set<Layer> layers = new TreeSet<Layer>();
    private final Snapshot snapshot;
    private final TechPool techPool;
    private HashMap<CellId, MyVectorCell> cells = new HashMap<CellId,MyVectorCell>();
    private final PrimitivePortId busPinPortId;
    private final PrimitiveNodeId cellCenterId;
    private final PrimitiveNodeId essentialBoundsId;
    /** local shape builder */
    private final ShapeBuilder shapeBuilder = new ShapeBuilder();
    /** List of VectorManhattanBuilders */
    private final ArrayList<VectorManhattanBuilder> boxBuilders = new ArrayList<VectorManhattanBuilder>();

    private class MyVectorCell {
        private final TechId techId;
        private final ArrayList<MyVectorManhattan> shapes = new ArrayList<MyVectorManhattan>();
        private final ArrayList<ImmutableNodeInst> subCells = new ArrayList<ImmutableNodeInst>();

        MyVectorCell(CellId cellId) {
            CellBackup cellBackup = snapshot.getCell(cellId);
            techId = cellBackup.cellRevision.d.techId;
            Technology tech = techPool.getTech(techId);
            if (isCellParameterized(cellBackup.cellRevision)) {
                throw new IllegalArgumentException();
            }

            long startTime = DEBUG ? System.currentTimeMillis() : 0;

            for (VectorManhattanBuilder b : boxBuilders) {
                b.clear();
            }
            // draw all arcs
            shapeBuilder.setup(cellBackup, Orientation.IDENT, USE_ELECTRICAL, WIPE_PINS, false, null);
            shapeBuilder.mvc = this;
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

            addBoxesFromBuilder(this, techPool.getTech(cellBackup.cellRevision.d.techId), boxBuilders);

            if (DEBUG) {
                long stopTime = System.currentTimeMillis();
                System.out.println((stopTime - startTime) + " init " + cellBackup.cellRevision.d.cellId);
            }
        }

    }

    public Collection<Layer> getLayers() {
        return new ArrayList<Layer>(layers);
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

    public List<Rectangle> collectLayer(Layer layer, CellId cellId, boolean rotate) {
        List<Rectangle> result = new ArrayList<Rectangle>();
        Orientation orient = (rotate ? Orientation.XR : Orientation.IDENT).canonic();
        collectLayer(layer, findVectorCell(cellId), new Point(0, 0), orient, result);
        return result;
    }

    private void addBoxesFromBuilder(MyVectorCell vc, Technology tech, ArrayList<VectorManhattanBuilder> boxBuilders) {
        for (int layerIndex = 0; layerIndex < boxBuilders.size(); layerIndex++) {
            VectorManhattanBuilder b = boxBuilders.get(layerIndex);
            if (b.size == 0) {
                continue;
            }
            Layer layer = tech.getLayer(layerIndex);
            MyVectorManhattan vm = new MyVectorManhattan(b.toArray(), layer);
            vc.shapes.add(vm);
        }
    }

    private void collectLayer(Layer layer, MyVectorCell vc, Point anchor, Orientation orient, List<Rectangle> result) {
        int[] coords = new int[4];
        for (MyVectorManhattan vb:  vc.shapes) {
            if (vb.layer != layer) {
                continue;
            }
            MyVectorManhattan vm = (MyVectorManhattan)vb;
            for (int i = 0; i < vm.coords.length; i += 4) {
                coords[0] = vm.coords[i + 0];
                coords[1] = vm.coords[i + 1];
                coords[2] = vm.coords[i + 2];
                coords[3] = vm.coords[i + 3];
                orient.rectangleBounds(coords);

                int lx = anchor.x + coords[0];
                int ly = anchor.y + coords[1];
                int hx = anchor.x + coords[2];
                int hy = anchor.y + coords[3];
                assert lx <= hx && ly <= hy;

                result.add(new Rectangle(lx, ly, hx - lx, hy - ly));
            }
        }
        for (ImmutableNodeInst n: vc.subCells) {
            if (!n.orient.isManhattan()) {
                throw new IllegalArgumentException();
            }
            coords[0] = (int)n.anchor.getGridX();
            coords[1] = (int)n.anchor.getGridY();
            orient.transformPoints(1, coords);
            Orientation subOrient = orient.concatenate(n.orient).canonic();
            MyVectorCell subCell = findVectorCell((CellId)n.protoId);
            collectLayer(layer, subCell, new Point(anchor.x + coords[0], anchor.y + coords[1]), subOrient, result);
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
        private MyVectorCell mvc;
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
            if (layer.isPseudoLayer()) {
                return;
            }
            throw new UnsupportedOperationException();
//            Point2D.Double[] points = new Point2D.Double[numPoints];
//            for (int i = 0; i < numPoints; i++) {
//                points[i] = new Point2D.Double(doubleCoords[i * 2], doubleCoords[i * 2 + 1]);
//            }
//            Poly poly = new Poly(points);
//            poly.setStyle(style);
//            poly.setLayer(layer);
//            poly.setGraphicsOverride(graphicsOverride);
//            poly.gridToLambda();
//            renderPoly(poly, vc, hideOnLowLevel, textType, false);
        }

        @Override
        public void addDoubleTextPoly(int numPoints, Poly.Type style, Layer layer, PrimitivePort pp, String message, TextDescriptor descriptor) {
            throw new UnsupportedOperationException();
//            Point2D.Double[] points = new Point2D.Double[numPoints];
//            for (int i = 0; i < numPoints; i++) {
//                points[i] = new Point2D.Double(doubleCoords[i * 2], doubleCoords[i * 2 + 1]);
//            }
//            Poly poly = new Poly(points);
//            poly.setStyle(style);
//            poly.setLayer(layer);
//            poly.gridToLambda();
//            poly.setString(message);
//            poly.setTextDescriptor(descriptor);
//            renderPoly(poly, vc, hideOnLowLevel, textType, false);
        }

        @Override
        public void addIntPoly(int numPoints, Poly.Type style, Layer layer, EGraphics graphicsOverride, PrimitivePort pp) {
            throw new UnsupportedOperationException();
//            switch (style) {
//                case OPENED:
//                    addIntLine(0, layer, graphicsOverride);
//                    break;
//                case OPENEDT1:
//                    addIntLine(1, layer, graphicsOverride);
//                    break;
//                case OPENEDT2:
//                    addIntLine(2, layer, graphicsOverride);
//                    break;
//                case OPENEDT3:
//                    addIntLine(3, layer, graphicsOverride);
//                    break;
//                default:
//                    Point2D.Double[] points = new Point2D.Double[numPoints];
//                    for (int i = 0; i < numPoints; i++) {
//                        points[i] = new Point2D.Double(intCoords[i * 2], intCoords[i * 2 + 1]);
//                    }
//                    Poly poly = new Poly(points);
//                    poly.setStyle(style);
//                    poly.setLayer(layer);
//                    poly.setGraphicsOverride(graphicsOverride);
//                    poly.gridToLambda();
//                    renderPoly(poly, vc, hideOnLowLevel, textType, false);
//                    break;
//            }
        }

        private void addIntLine(int lineType, Layer layer, EGraphics graphicsOverride) {
            throw new UnsupportedOperationException();
//            int x1 = intCoords[0];
//            int y1 = intCoords[1];
//            int x2 = intCoords[2];
//            int y2 = intCoords[3];
//            VectorLine vl = new VectorLine(x1, y1, x2, y2, lineType, layer, graphicsOverride);
//            vc.shapes.add(vl);
        }

        @Override
        public void addIntBox(int[] coords, Layer layer) {
            layers.add(layer);
            // convert coordinates
            int lX = coords[0];
            int lY = coords[1];
            int hX = coords[2];
            int hY = coords[3];
            int layerIndex = -1;
            if (layer.getId().techId == mvc.techId) {
                layerIndex = layer.getIndex();
            }
            if (layerIndex >= 0) {
                putBox(layerIndex, boxBuilders, lX, lY, hX, hY);
            } else {
                MyVectorManhattan vm = new MyVectorManhattan(new int[]{lX, lY, hX, hY}, layer);
                mvc.shapes.add(vm);
            }
        }
    }

    private static void putBox(int layerIndex, ArrayList<VectorManhattanBuilder> boxBuilders, int lX, int lY, int hX, int hY) {
        while (layerIndex >= boxBuilders.size()) {
            boxBuilders.add(new VectorManhattanBuilder());
        }
        VectorManhattanBuilder b = boxBuilders.get(layerIndex);
        b.add(lX, lY, hX, hY);
    }
    
    /**
     * Class which defines a cached Manhattan rectangle.
     */
    static class MyVectorManhattan {
        final Layer layer;
        /** coordinates of boxes: 1X, 1Y, hX, hY */
        final int[] coords;

        private MyVectorManhattan(int[] coords, Layer layer) {
            this.layer = layer;
            this.coords = coords;
        }

        MyVectorManhattan(double c1X, double c1Y, double c2X, double c2Y, Layer layer) {
            this(new int[]{databaseToGrid(c1X), databaseToGrid(c1Y), databaseToGrid(c2X), databaseToGrid(c2Y)}, layer);
        }
    }

    private static int databaseToGrid(double lambdaValue) {
        return (int) DBMath.lambdaToGrid(lambdaValue);
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
