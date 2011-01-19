/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DefaultLayoutCell.java
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
import java.io.IOException;
import java.io.Serializable;

/**
 *
 */
public class DefaultLayoutCell implements LayoutCell, Serializable {

    private String name;
    private transient int[] rectCoords = new int[4];
    private int numRectangles = 0;

    private static class CellInst implements Serializable {

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
    private transient Rectangle boundingBox;
    private transient boolean finished;

    DefaultLayoutCell(String name) {
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
        return numRectangles;
    }

    /**
     * Traverse all rectangles by specified handler 
     * @param h handler
     */
    public void traverseRectangles(RectangleHandler h) {
        traverseRectangles(h, 0, numRectangles);
    }

    /**
     * Traverse part of rectangles by specified handler 
     * @param h handler
     * @param offset the first rectangle 
     * @param count the number of rectangle
     */
    public void traverseRectangles(RectangleHandler h, int offset, int count) {
        if (offset < 0 || count < 0 || offset + count > numRectangles) {
            throw new IndexOutOfBoundsException();
        }
        for (int i = 0; i < numRectangles; i++) {
            int minX = rectCoords[(i + offset) * 4 + 0];
            int minY = rectCoords[(i + offset) * 4 + 1];
            int maxX = rectCoords[(i + offset) * 4 + 2];
            int maxY = rectCoords[(i + offset) * 4 + 3];
            h.apply(minX, minY, maxX, maxY);
        }
    }

    /**
     * Read coordinates of part of rectangles into int array.
     * The length of the result array must be at least 4*count .
     * The coordinates are placed into the result array in such an order:
     * (minX0, minY0, maxX0, maxY0, minX1, minY1, maxX1, maxY1, ...)
     * This is the same layout as in ManhattanOrientation.transoformRects method.
     * @param result
     * @param offset The first rectangle
     * @param count The number of rectangles
     */
    public void readRectangleCoords(int[] result, int offset, int count) {
        if (offset + count > numRectangles) {
            throw new IndexOutOfBoundsException();
        }
        System.arraycopy(rectCoords, offset * 4, result, 0, count * 4);
    }

    // subcells
    public int getNumSubcells() {
        return subCells.size();
    }

    public LayoutCell getSubcellCell(int subCellIndex) {
        return subCells.get(subCellIndex).subCell;
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
            lx = Math.min(lx, rectCoords[i * 4 + 0]);
            ly = Math.min(ly, rectCoords[i * 4 + 1]);
            hx = Math.max(hx, rectCoords[i * 4 + 2]);
            hy = Math.max(hy, rectCoords[i * 4 + 3]);
        }
        int[] bounds = new int[4];
        for (CellInst ci : subCells) {
            long x = ci.anchor.getX();
            long y = ci.anchor.getY();
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
            if (lx < -LayoutCell.MAX_COORD
                    || hx > LayoutCell.MAX_COORD
                    || ly < -LayoutCell.MAX_COORD
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
        if (numRectangles * 4 >= rectCoords.length) {
            int[] newRectCoords = new int[rectCoords.length * 2];
            System.arraycopy(rectCoords, 0, newRectCoords, 0, rectCoords.length);
            rectCoords = newRectCoords;
        }
        rectCoords[numRectangles * 4 + 0] = minX;
        rectCoords[numRectangles * 4 + 1] = minY;
        rectCoords[numRectangles * 4 + 2] = maxX;
        rectCoords[numRectangles * 4 + 3] = maxY;
        numRectangles++;

        System.out.println("\"" + name + "\".addRectangle(" + minX + "," + minY + "," + maxX + "," + maxY
                + ")");
    }

    public void addRectangle(Rectangle rect) {
        if (finished) {
            throw new IllegalStateException();
        }
        addRectangle(rect.getMin().getX(), rect.getMin().getY(), rect.getMax().getX(), rect.getMax().getY());
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
    
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        rectCoords = new int[numRectangles*4];
        for (int i = 0; i < rectCoords.length; i++) {
            rectCoords[i] = in.readInt();
        }
        computeBoundingBox();
    }
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        for (int i = 0; i < numRectangles*4; i++) {
            out.writeInt(rectCoords[i]);
        }
    }
}
