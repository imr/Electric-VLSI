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
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
        subTree(topCell, Orientation.XR, visited);
        showLayer("Metal-1", false);
        showLayer("Metal-2", true,
                22.0, 9.0,
                44.0, 2.8,  50.0, 2.8,  56.0, 2.8,  62.0, 2.8,
                72.0, 9.0,
                82.0, 2.8,  88.0, 2.8,  94.0, 2.8,  100.0, 2.8,  106.0, 2.8, 
                122.0, 9.0);
        showLayer("Metal-3", false,
                0.0, 25.0,
                /* 18.0, 2.8,  24.0, 2.8, */
                24.0, 9.0,
                /* ? 48.0, 9.0 */
                30.0, 2.8,  36.0, 2.8, 42.0, 2.8,  48.0, 2.8,  54.0, 2.8,  60.0, 2.8,  66.0, 2.8,  72.0, 2.8,  78.0, 2.8,  84.0, 2.8,  90.0, 2.8,  96.0, 2.8,  102.0, 2.8,  108.0, 2.8,  114.0, 2.8,
                /* 57.0, 2.8,  63.0, 2.8,  69.0, 2.8,  71.0, 2.8,  73.0, 2.8, */
                /* ? 96.0, 9.0 */
                120.0, 9.0
                /* 120.0, 2.8,  126, 2.8, */
                );
        showLayer("Metal-4", true,
                /* 0.0, 10.0 */
                0.0, 2.8,  5.8, 0.0,  11.6, 2.8,  17.4, 2.8,  23.2, 2.8,
                /* 4.6, 2.8,  13.8, 2.8,  23.0, 2.8, */
                36.0, 15.0,
                48.8, 2.8,  54.6, 2.8,  60.4, 2.8,  66.2, 2.8,  72.0, 2.8,  77.8, 2.8,  83.6, 2.8,  89.4, 2.8,  95.2, 2.8,
                /* 57.5, 2.8,  63.3, 2.8,  69.1, 2.8,  74.9, 2.8,  80.7, 2.8,  86.5, 2.8, */
                108.0, 15.0,
                120.8, 2.8,  126.6, 2.8,  132.4, 2.8,  138.2, 2.8
                /*121.0, 2.8, 130.2, 2.8, 139.4, 2.8*/);
        showLayer("Metal-5", false, 12.0, 12.0,  60.0, 12.0,  84.0, 12.0,  132.0, 12.0);
        showLayer("Metal-6", true , 16.0, 24.0,  56.0, 24.0,  88.0, 24.0,  128.0, 24.0);
        showLayer("Metal-7", false, 16.0, 24.0,  56.0, 24.0,  88.0, 24.0,  128.0, 24.0);
        showLayer("Metal-8", true , 18.0, 27.0,  54.0, 27.0,  90.0, 27.0,  126.0, 27.0);
        showLayer("Metal-9", false, 18.0, 27.0,  54.0, 27.0,  90.0, 27.0,  126.0, 27.0);
    }

    public void showLayer(String layerName, boolean rotate, double... channels) {
        List<Rectangle> rects = new ArrayList<Rectangle>();
        Layer layer = Technology.getCMOS90Technology().findLayer(layerName);
        collectLayer(layer, findVectorCell(topCell.getId(), rotate ? Orientation.XR : Orientation.IDENT), new Point(0, 0), rects);
        if (true) {
            System.out.println(layerName);
            DeltaMerge dm = new DeltaMerge();
            try {
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(layerName + ".dm")));
                out.writeBoolean(rotate);
                dm.loop(rects, out);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                assert lx <= hx && ly <= hy;
                result.add(new Rectangle(lx, ly, hx - lx, hy - ly));
            }
        }
        for (VectorSubCell vsc: vc.subCells) {
            VectorCell subCell = findVectorCell(vsc.subCellId, vc.orient.concatenate(vsc.n.orient));
            collectLayer(layer, subCell, new Point(anchor.x + vsc.offsetX, anchor.y + vsc.offsetY), result);
        }
    }
}
