/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SimpleChecker.java
 *
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.api.minarea;

import java.awt.geom.Point2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Properties;

import com.sun.electric.api.minarea.geometry.Point;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.geometry.bool.DeltaMerge;
import com.sun.electric.database.geometry.bool.UnloadPolys;
import com.sun.electric.util.math.DBMath;

/**
 * Simple MinAreaChecker
 */
public class SimpleChecker implements MinAreaChecker {

    /**
     * 
     * @return the algorithm name
     */
    public String getAlgorithmName() {
        return "SimpleChecker";
    }

    /**
     * 
     * @return the names and default values of algorithm parameters
     */
    public Properties getDefaultParameters() {
        return new Properties();
    }

    /**
     * @param topCell
     *            top cell of the layout
     * @param minArea
     *            minimal area of valid polygon
     * @param parameters
     *            algorithm parameters
     * @param errorLogger
     *            an API to report violations
     */
    public void check(LayoutCell topCell, long minArea, Properties parameters, ErrorLogger errorLogger) {
        DeltaMerge dm = new DeltaMerge();
        collect(dm, topCell, 0, 0, ManhattanOrientation.R0);
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bout);
            dm.loop(out);
            out.close();
            byte[] ba = bout.toByteArray();
            bout = null;
            DataInputStream inpS = new DataInputStream(new ByteArrayInputStream(ba));
            UnloadPolys up = new UnloadPolys();
            Iterable<PolyBase.PolyBaseTree> trees = up.loop(inpS, false);
            inpS.close();
            for (PolyBase.PolyBaseTree tree : trees) {
                traversePolyTree(tree, 0, minArea, errorLogger);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void collect(DeltaMerge dm, LayoutCell cell, int x, int y, ManhattanOrientation orient) {
        int bufSize = Math.max(1, Math.min(16, cell.getNumRectangles()));
        int[] coords = new int[4 * bufSize];
        int ir = 0;
        while (ir < cell.getNumRectangles()) {
            int sz = Math.min(bufSize, cell.getNumRectangles() - ir);
            cell.readRectangleCoords(ir, sz, coords);
            orient.transformRects(coords, 0, 1);
            for (int j = 0; j < sz; j++) {
                // The coordinates will fit into ints, because bounding box
                // of LayoutCell is limited within [-MAX_Coord,+MAX_COORD]
                int lx = coords[j * 4 + 0] + x;
                int ly = coords[j * 4 + 1] + y;
                int hx = coords[j * 4 + 2] + x;
                int hy = coords[j * 4 + 3] + y;
                dm.put(lx, ly, hx, hy);
            }
            ir += sz;
        }

        for (int i = 0; i < cell.getNumSubcells(); i++) {
            LayoutCell subCell = cell.getSubcellCell(i);

            Point p = cell.getSubcellAnchor(i).transform(orient);
            ManhattanOrientation subOrient = cell.getSubcellOrientation(i);
            collect(dm, subCell, p.getX() + x, p.getY() + y, orient.concatenate(subOrient));
        }
    }

    private void traversePolyTree(PolyBase.PolyBaseTree obj, int level, long minArea, ErrorLogger errorLogger) {
        if (level % 2 == 0) {
            PolyBase poly = obj.getPoly();
            double area = poly.getArea();
            for (PolyBase.PolyBaseTree son : obj.getSons()) {
                traversePolyTree(son, level + 1, minArea, errorLogger);
                area -= son.getPoly().getArea();
            }
            long larea = DBMath.lambdaToGrid(area * DBMath.GRID);
            if (larea < minArea) {
                Point2D p = poly.getPoints()[0];
                errorLogger.reportMinAreaViolation(larea, DBMath.lambdaToGrid(p.getX()), DBMath.lambdaToGrid(p.getY()));
            }
        } else {
            for (PolyBase.PolyBaseTree son : obj.getSons()) {
                traversePolyTree(son, level + 1, minArea, errorLogger);
            }
        }
    }
}
