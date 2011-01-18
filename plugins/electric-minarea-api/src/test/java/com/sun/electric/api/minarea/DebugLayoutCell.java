/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DebugLayoutCell.java
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

import java.util.ArrayList;
import java.util.List;

import com.sun.electric.api.minarea.geometry.Point;
import com.sun.electric.api.minarea.geometry.Polygon.Rectangle;

/**
 *
 */
public class DebugLayoutCell implements LayoutCell {

    private String name;
    // private int[] rectCoords = new int[4];
    private List<Rectangle> rectangles = new ArrayList<Rectangle>();
    private int numRectangles = 0;

    private static class CellInst {

        private final LayoutCell subCell;
        private final Point anchor;
        private final ManhattanOrientation orient;

        private CellInst(LayoutCell subCell, Point anchor, ManhattanOrientation orient) {
            this.subCell = subCell;
            this.anchor = anchor;
            this.orient = orient;
        }
    }
    private final List<CellInst> subCells = new ArrayList<CellInst>();
    private Rectangle boundingBox;
    private boolean finished;

    DebugLayoutCell(String name) {
        this.name = name;
    }

    // cell name
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // rectangles
    public int getNumRectangles() {
        return rectangles.size();
    }

    @Deprecated
    public int getRectangleMinX(int rectangleIndex) {
        // return rectCoords[rectangleIndex * 4 + 0];
        return this.getRectangle(rectangleIndex).getMin().getX();
    }

    @Deprecated
    public int getRectangleMinY(int rectangleIndex) {
        // return rectCoords[rectangleIndex * 4 + 1];
        return this.getRectangle(rectangleIndex).getMin().getY();
    }

    @Deprecated
    public int getRectangleMaxX(int rectangleIndex) {
        // return rectCoords[rectangleIndex * 4 + 2];
        return this.getRectangle(rectangleIndex).getMax().getX();
    }

    @Deprecated
    public int getRectangleMaxY(int rectangleIndex) {
        // return rectCoords[rectangleIndex * 4 + 3];
        return this.getRectangle(rectangleIndex).getMax().getY();
    }

    public void traverseRectangles(LayoutCell.RectangleHandler h) {
        for (int i = 0; i < numRectangles; i++) {
            h.apply(this.rectangles.get(i));
        }
    }

    // subcells
    public int getNumSubcells() {
        return subCells.size();
    }

    public LayoutCell getSubcellCell(int subCellIndex) {
        return subCells.get(subCellIndex).subCell;
    }

    public int getSubcellAnchorX(int subCellIndex) {
        // return subCells.get(subCellIndex).anchorX;
        return this.getSubcellAnchor(subCellIndex).getX();
    }

    public int getSubcellAnchorY(int subCellIndex) {
        // return subCells.get(subCellIndex).anchorY;
        return this.getSubcellAnchor(subCellIndex).getY();
    }

    public ManhattanOrientation getSubcellOrientation(int subCellIndex) {
        return subCells.get(subCellIndex).orient;
    }

    public void traverseSubcellInstances(LayoutCell.SubcellHandler h) {
        for (CellInst ci : subCells) {
            h.apply(ci.subCell, ci.anchor, ci.orient);
        }
    }

    // bounding box
    public int getBoundingMinX() {
        // if (!finished)
        // computeBoundingBox();
        // return boundingMinX;
        return this.getBoundingBox().getMin().getX();
    }

    public int getBoundingMinY() {
        // if (!finished)
        // computeBoundingBox();
        // return boundingMinY;
        return this.getBoundingBox().getMin().getY();
    }

    public int getBoundingMaxX() {
        // if (!finished)
        // computeBoundingBox();
        // return boundingMaxX;
        return this.getBoundingBox().getMax().getX();
    }

    public int getBoundingMaxY() {
        // if (!finished)
        // computeBoundingBox();
        // return boundingMaxY;
        return this.getBoundingBox().getMax().getY();
    }

    private void computeBoundingBox() {
        long lx = Long.MAX_VALUE;
        long ly = Long.MAX_VALUE;
        long hx = Long.MIN_VALUE;
        long hy = Long.MIN_VALUE;
        for (int i = 0; i < numRectangles; i++) {
            Rectangle rect = this.rectangles.get(i);
            lx = Math.min(lx, rect.getMin().getX());
            ly = Math.min(ly, rect.getMin().getY());
            hx = Math.max(hx, rect.getMax().getX());
            hy = Math.max(hy, rect.getMax().getY());
        }
        long[] bounds = new long[4];
        for (CellInst ci : subCells) {
            int x = ci.anchor.getX();
            int y = ci.anchor.getY();
            bounds[0] = ci.subCell.getBoundingMinX();
            bounds[1] = ci.subCell.getBoundingMinY();
            bounds[2] = ci.subCell.getBoundingMaxX();
            bounds[3] = ci.subCell.getBoundingMaxY();
            ci.orient.transformRects(bounds, 0, 1);
            lx = Math.min(lx, x + bounds[0]);
            ly = Math.min(ly, y + bounds[1]);
            hx = Math.max(hx, x + bounds[2]);
            hy = Math.max(hy, y + bounds[3]);
        }
        if (lx <= hx && ly <= hy) {
            if (lx < -LayoutCell.MAX_COORD || hx > LayoutCell.MAX_COORD || ly < -LayoutCell.MAX_COORD
                    || hy > LayoutCell.MAX_COORD) {
                throw new IllegalArgumentException("Too large bounding box");
            }
            boundingBox = new Rectangle(new Point((int) lx, (int) ly), new Point((int) hx, (int) hy));
        }
        finished = true;
    }

    @Deprecated
    public void addRectangle(int minX, int minY, int maxX, int maxY) {
        if (finished) {
            throw new IllegalStateException();
        }
        if (minX >= maxX || minY >= maxY) {
            throw new IllegalArgumentException();
        }
//		if (numRectangles * 4 >= rectCoords.length) {
//			int[] newRectCoords = new int[rectCoords.length * 2];
//			System.arraycopy(rectCoords, 0, newRectCoords, 0, rectCoords.length);
//			rectCoords = newRectCoords;
//		}
//		rectCoords[numRectangles * 4 + 0] = minX;
//		rectCoords[numRectangles * 4 + 1] = minY;
//		rectCoords[numRectangles * 4 + 2] = maxX;
//		rectCoords[numRectangles * 4 + 3] = maxY;
//		numRectangles++;

        this.rectangles.add(new Rectangle(new Point(minX, minY), new Point(maxX, maxY)));

        System.out.println("\"" + name + "\".addRectangle(" + minX + "," + minY + "," + maxX + "," + maxY
                + ")");
    }

    public void addRectangle(Rectangle rect) {
        if (finished) {
            throw new IllegalStateException();
        }
        this.rectangles.add(rect);
        System.out.println("\"" + name + "\".addRectangle(" + rect.getMin().getX() + ","
                + rect.getMin().getY() + "," + rect.getMax().getX() + "," + rect.getMax().getY() + ")");
    }

    public void addSubCell(LayoutCell subCell, Point anchor, ManhattanOrientation orient) {
        if (finished) {
            throw new IllegalStateException();
        }
        subCells.add(new CellInst(subCell, anchor, orient));
        System.out.println("\"" + name + "\".addSubCell(\"" + subCell.getName() + "\"," + anchor.getX() + ","
                + anchor.getY() + "," + orient + ");");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.electric.api.minarea.LayoutCell#getRectangle(int)
     */
    public Rectangle getRectangle(int rectangleIndex) {
        return this.rectangles.get(rectangleIndex);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.electric.api.minarea.LayoutCell#getSubcellAnchor(int)
     */
    public Point getSubcellAnchor(int subCellIndex) {
        return subCells.get(subCellIndex).anchor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.electric.api.minarea.LayoutCell#getBoundingBox()
     */
    public Rectangle getBoundingBox() {
        if (!finished) {
            computeBoundingBox();
        }

        return this.boundingBox;
    }
}
