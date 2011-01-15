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

import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.geometry.bool.DeltaMerge;
import com.sun.electric.database.geometry.bool.UnloadPolys;
import java.awt.geom.Point2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Properties;

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
     * @param topCell top cell of the layout
     * @param minArea minimal area of valid polygon
     * @param parameters algorithm parameters
     * @param errorLogger an API to report violations
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
            for (PolyBase.PolyBaseTree tree: trees) {
                traversePolyTree(tree, 0, minArea, errorLogger);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void collect(DeltaMerge dm, LayoutCell cell, long x, long y, ManhattanOrientation orient) {
        long[] coords = new long[4];
        for (int i = 0; i < cell.getNumRectangles(); i++) {
            coords[0] = cell.getRectangleMinX(i);
            coords[1] = cell.getRectangleMinY(i);
            coords[2] = cell.getRectangleMinY(i);
            coords[3] = cell.getRectangleMinY(i);
            orient.transformRects(coords, 0, 1);
            int lx = (int)(coords[0] + x);
            int ly = (int)(coords[1] + y);
            int hx = (int)(coords[2] + x);
            int hy = (int)(coords[3] + y);
            if (lx != (int)(coords[0] + x) || ly != (int)(coords[1] + y) ||
                    hx != (int)(coords[2] + x) || hy != (int)(coords[3] + y)) {
                throw new IllegalArgumentException("Too large coordinates");
            }
            dm.put(lx, ly, hx, hy);
        }
        for (int i = 0; i < cell.getNumSubcells(); i++) {
            LayoutCell subCell = cell.getSubcellCell(i);
            coords[0] = cell.getSubcellX(i);
            coords[1] = cell.getSubcellY(i);
            orient.transformPoints(coords, 0, 1);
            ManhattanOrientation subOrient = cell.getSubcellOrientation(i);
            collect(dm, subCell, coords[0] + x,  coords[1] + y, orient.concatenate(subOrient));
        }
    }

    private void traversePolyTree(PolyBase.PolyBaseTree obj, int level, long minArea, ErrorLogger errorLogger) {
        if (level%2 == 0) {
            PolyBase poly = obj.getPoly();
            double area = poly.getArea();
            for (PolyBase.PolyBaseTree son : obj.getSons()) {
                traversePolyTree(son, level+1, minArea, errorLogger);
                area -= son.getPoly().getArea();
            }
            if (area < minArea) {
                Point2D p = poly.getPoints()[0];
                errorLogger.reportMinAreaViolation((long)area, (long)p.getX(), (long)p.getY());
            }
        } else {
            for (PolyBase.PolyBaseTree son : obj.getSons()) {
                traversePolyTree(son, level+1, minArea, errorLogger);
            }
        }
    }
}
