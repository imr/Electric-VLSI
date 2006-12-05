/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BoundsBuilder.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Poly;
import java.awt.geom.Rectangle2D;

/**
 * A support class to build shapes of arcs and nodes.
 */
public class BoundsBuilder extends AbstractShapeBuilder {
    private int intMinX, intMinY, intMaxX, intMaxY;
    private double doubleMinX, doubleMinY, doubleMaxX, doubleMaxY;
    private boolean hasIntBounds, hasDoubleBounds;
    
    public BoundsBuilder(Shrinkage shrinkage) {
        setShrinkage(shrinkage);
        clear();
    }
    
    public void clear() {
        hasIntBounds = hasDoubleBounds = false;
    }
    
    public ERectangle makeBounds() {
        if (!hasDoubleBounds) {
            if (!hasIntBounds) return null;
            int iw = intMaxX - intMinX;
            int ih = intMaxY - intMinY;
            return ERectangle.fromGrid(intMinX, intMinY,
                    iw >= 0 ? iw : (long)intMaxX - (long)intMinX,
                    ih >= 0 ? ih : (long)intMaxY - (long)intMinY);
        }
        if (hasIntBounds) {
            if (intMinX < doubleMinX) doubleMinX = intMinX;
            if (intMinY < doubleMinY) doubleMinY = intMinY;
            if (intMaxX > doubleMaxX) doubleMaxX = intMaxX;
            if (intMaxY > doubleMaxY) doubleMaxY = intMaxY;
            hasIntBounds = false;
        }
        long longMinX = GenMath.floorLong(doubleMinX);
        long longMaxX = GenMath.ceilLong(doubleMaxX);
        long longMinY = GenMath.floorLong(doubleMinY);
        long longMaxY = GenMath.ceilLong(doubleMaxY);
        return ERectangle.fromGrid(longMinX, longMinY, longMaxX - longMinX, longMaxY - longMinY);
    }
    
    public boolean makeBounds(Rectangle2D.Double visBounds) {
        double x, y, w, h;
        if (!hasDoubleBounds) {
            assert hasIntBounds;
            x = intMinX;
            y = intMinY;
            int iw = intMaxX - intMinX;
            w = iw >= 0 ? iw : (long)intMaxX - (long)intMinX;
            int ih = intMaxY - intMinY;
            h = ih >= 0 ? ih : (long)intMaxY - (long)intMinY;
        } else {
            if (hasIntBounds) {
                if (intMinX < doubleMinX) doubleMinX = intMinX;
                if (intMinY < doubleMinY) doubleMinY = intMinY;
                if (intMaxX > doubleMaxX) doubleMaxX = intMaxX;
                if (intMaxY > doubleMaxY) doubleMaxY = intMaxY;
                hasIntBounds = false;
            }
            x = GenMath.floorLong(doubleMinX);
            y = GenMath.floorLong(doubleMinY);
            w = GenMath.ceilLong(doubleMaxX) - x;
            h = GenMath.ceilLong(doubleMaxY) - y;
        }
        x = DBMath.gridToLambda(x);
        y = DBMath.gridToLambda(y);
        w = DBMath.gridToLambda(w);
        h = DBMath.gridToLambda(h);
        if (x == visBounds.getX() && y == visBounds.getY() && w == visBounds.getWidth() && h == visBounds.getHeight())
            return false;
        visBounds.setRect(x, y, w, h);
        return true;
    }
    
    @Override
    public void addDoublePoly(int numPoints, Poly.Type style, Layer layer) {
        if (!hasDoubleBounds) {
            assert numPoints > 0;
            doubleMinX = doubleMaxX = doubleCoords[0];
            doubleMinY = doubleMaxY = doubleCoords[1];
            hasDoubleBounds = true;
        }
        for (int i = 0; i < numPoints; i++) {
            double x = doubleCoords[i*2];
            double y = doubleCoords[i*2 + 1];
            if (x < doubleMinX) doubleMinX = x;
            if (x > doubleMaxX) doubleMaxX = x;
            if (y < doubleMinY) doubleMinY = y;
            if (y > doubleMaxY) doubleMaxY = y;
        }
    }
    
    @Override
    public void addIntLine(int[] coords, Poly.Type style, Layer layer) {
        int x1 = coords[0];
        int x2 = coords[2];
        if (x1 > x2) {
            coords[0] = x2;
            coords[2] = x1;
        }
        int y1 = coords[1];
        int y2 = coords[3];
        if (y1 > y2) {
            coords[1] = y2;
            coords[3] = y1;
        }
        addIntBox(coords, layer);
    }
    
    @Override
    public void addIntBox(int[] coords, Layer layer) {
        if (!hasIntBounds) {
            intMinX = coords[0];
            intMinY = coords[1];
            intMaxX = coords[2];
            intMaxY = coords[3];
            hasIntBounds = true;
        } else {
            if (coords[0] < intMinX) intMinX = coords[0];
            if (coords[2] > intMaxX) intMaxX = coords[2];
            if (coords[1] < intMinY) intMinY = coords[1];
            if (coords[3] > intMaxY) intMaxY = coords[3];
        }
    }
}
