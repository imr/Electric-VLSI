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
package com.sun.electric.tool.user.redisplay;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.geometry.bool.DeltaMerge;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 */
public class VectorCacheExt extends VectorCache {
    Set<Layer> layers = new TreeSet<Layer>();
    Cell topCell;

    public VectorCacheExt(Cell topCell) {
        super(topCell.getDatabase());
        this.topCell = topCell;
        Set<VectorCell> visited = new HashSet<VectorCell>();
        subTree(topCell, Orientation.IDENT, visited);
        subTree(topCell, Orientation.R, visited);
        showLayer("Metal-9", false);
        showLayer("Metal-8", true);
        showLayer("Metal-7", false);
        showLayer("Metal-6", true);
        showLayer("Metal-5", false);
        showLayer("Metal-4", true);
        showLayer("Metal-3", false);
    }

    public void showLayer(String layerName, boolean rotate) {
        List<Rectangle> rects = new ArrayList<Rectangle>();
        Layer layer = Technology.getCMOS90Technology().findLayer(layerName);
        collectLayer(layer, findVectorCell(topCell.getId(), rotate ? Orientation.R : Orientation.IDENT), new Point(0, 0), rects);
        if (true) {
            System.out.println(layerName);
            DeltaMerge dm = new DeltaMerge();
            dm.loop(rects);
        } else {
            Collections.sort(rects, new Comparator<Rectangle>() {
                public int compare(Rectangle r1, Rectangle r2) {
                    if (r1.x < r2.x) {
                        return -1;
                    } else if (r1.x > r2.x) {
                        return 1;
                    } else if (r1.y < r2.y) {
                        return -1;
                    } else if (r1.y > r2.y) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });
            GeometryHandler merger = GeometryHandler.createGeometryHandler(GeometryHandler.GHMode.ALGO_SWEEP, 0);
            for (Rectangle rect: rects) {
                merger.add(layer, rect);
            }
            merger.postProcess(true);
            Collection<PolyBase> polys = merger.getObjects(layer, true, true);
            Library destLib = Library.newInstance("Metal9", null);
            Cell destCell = Cell.newInstance(destLib, "merged{lay}");
            PrimitiveNode pn = layer.getPureLayerNode();
            for (PolyBase poly: polys) {
                EPoint anchor = EPoint.fromGrid((long)poly.getCenterX(), (long)poly.getCenterY());
                Rectangle2D box = poly.getBox();
                if (box != null) {
                    long w = Math.round(box.getWidth());
                    long h = Math.round(box.getHeight());
                    if ((w&1) == 0 && (h&1) == 0) {
                        NodeInst.newInstance(pn, anchor,
                                DBMath.gridToLambda(w), DBMath.gridToLambda(h),
                                destCell);
                        continue;
                    }
                }
                NodeInst ni = NodeInst.newInstance(pn, anchor, 0, 0, destCell);
                ni.setTrace(poly.getPoints());
            }
        }
    }

    private void subTree(Cell cell, Orientation orient, Set<VectorCell> visited) {
        VectorCell vc = findVectorCell(cell.getId(), orient);
        if (visited.contains(vc)) {
            return;
        }
        visited.add(vc);
        drawCell(cell.getId(), orient, VarContext.globalContext, 1.0);
        for (VectorBase vb: vc.shapes) {
            if (vb instanceof VectorManhattan) {
                layers.add(vb.layer);
            }
        }

        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();
            if (!ni.isCellInstance()) {
                continue;
            }
            subTree((Cell)ni.getProto(), orient.concatenate(ni.getOrient()), visited);
        }
    }

    private void collectLayer(Layer layer, VectorCell vc, Point anchor, List<Rectangle> result) {
        for (VectorBase vb: vc.shapes) {
            if (vb.layer != layer || !(vb instanceof VectorManhattan)) {
                continue;
            }
            VectorManhattan vm = (VectorManhattan)vb;
            for (int i = 0; i < vm.coords.length; i += 4) {
                int lx = anchor.x + vm.coords[i + 0];
                int ly = anchor.y + vm.coords[i + 1];
                int hx = anchor.x + vm.coords[i + 2];
                int hy = anchor.y + vm.coords[i + 3];
                assert lx < hx && ly < hy;
                result.add(new Rectangle(lx, ly, hx - lx, hy - ly));
            }
        }
        for (VectorSubCell vsc: vc.subCells) {
            VectorCell subCell = findVectorCell(vsc.subCellId, vc.orient.concatenate(vsc.n.orient));
            collectLayer(layer, subCell, new Point(anchor.x + vsc.offsetX, anchor.y + vsc.offsetY), result);
        }
    }
}
