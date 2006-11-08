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

import com.sun.electric.database.CellBackup.Memoization;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.Poly;
import java.awt.geom.Rectangle2D;

/**
 * A support class to build shapes of arcs and nodes.
 */
public class BoundsBuilder extends AbstractShapeBuilder {
    int intMinX, intMinY, intMaxX, intMaxY;
    long longMinX, longMinY, longMaxX, longMaxY;
    boolean hasIntBounds, hasLongBounds;
    
    public BoundsBuilder(Memoization m) {
        this.m = m;
        clear();
    }
    
    public void clear() {
        hasIntBounds = hasLongBounds = false;
    }
    
    public void genBoundsOfArc(ImmutableArcInst a) {
        if (a.isEasyShape())
            genBoundsOfArcEasy(a);
        else {
            pointCount = 0;
            a.protoType.tech.getShapeOfArc(this, a, null, null);
        }
    }
    
    private void genBoundsOfArcEasy(ImmutableArcInst a) {
        int gridFullWidth = (int)a.getGridFullWidth();
        if (gridFullWidth == 0) {
            int x1 = (int)a.tailLocation.getGridX();
            int y1 = (int)a.tailLocation.getGridY();
            int x2 = (int)a.headLocation.getGridX();
            int y2 = (int)a.headLocation.getGridY();
            if (x1 <= x2) {
                intCoords[0] = x1;
                intCoords[2] = x2;
            } else {
                intCoords[0] = x2;
                intCoords[2] = x1;
            }
            if (y1 <= y2) {
                intCoords[1] = y1;
                intCoords[3] = y2;
            } else {
                intCoords[1] = y2;
                intCoords[3] = y1;
            }
        } else {
            a.makeGridBoxInt(this, gridFullWidth);
        }
        addIntBox();
    }

    private void addIntBox() {
        if (hasIntBounds) {
            int x1 = intCoords[0];
            if (x1 < intMinX) intMinX = x1;
            int y1 = intCoords[1];
            if (y1 < intMinY) intMinY = y1;
            int x2 = intCoords[2];
            if (x2 > intMaxX) intMaxX = x2;
            int y2 = intCoords[3];
            if (y2 > intMaxY) intMaxY = y2;
        } else {
            intMinX = intCoords[0];
            intMinY = intCoords[1];
            intMaxX = intCoords[2];
            intMaxY = intCoords[3];
            hasIntBounds = true;
        }
    }
    
    public ERectangle makeBounds() {
        if (!hasLongBounds) {
            assert hasIntBounds;
            int iw = intMaxX - intMinX;
            int ih = intMaxY - intMinY;
            return ERectangle.fromGrid(intMinX, intMinY,
                    iw >= 0 ? iw : (long)intMaxX - (long)intMinX,
                    ih >= 0 ? ih : (long)intMaxY - (long)intMinY);
        }
        if (hasIntBounds) {
            if (intMinX < longMinX) longMinX = intMinX;
            if (intMinY < longMinY) longMinY = intMinY;
            if (intMaxX > longMaxX) longMaxX = intMaxX;
            if (intMaxY > longMaxY) longMaxY = intMaxY;
            hasIntBounds = false;
        }
        return ERectangle.fromGrid(longMinX, longMinY, longMaxX - longMinX, longMaxY - longMinY);
    }
    
    public boolean makeBounds(Rectangle2D.Double visBounds) {
        double x, y, w, h;
        if (!hasLongBounds) {
            assert hasIntBounds;
            x = intMinX;
            y = intMinY;
            int iw = intMaxX - intMinX;
            w = iw >= 0 ? iw : (long)intMaxX - (long)intMinX;
            int ih = intMaxY - intMinY;
            h = ih >= 0 ? ih : (long)intMaxY - (long)intMinY;
        } else {
            if (hasIntBounds) {
                if (intMinX < longMinX) longMinX = intMinX;
                if (intMinY < longMinY) longMinY = intMinY;
                if (intMaxX > longMaxX) longMaxX = intMaxX;
                if (intMaxY > longMaxY) longMaxY = intMaxY;
                hasIntBounds = false;
            }
            x = longMinX;
            y = longMinY;
            w = longMaxX - longMinX;
            h = longMaxY - longMinY;
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
    public void addLongPoly(int numPoints, Poly.Type style, Layer layer) {
        if (!hasLongBounds) {
            assert numPoints > 0;
            longMinX = longMaxX = longCoords[0];
            longMinY = longMaxY = longCoords[1];
            hasLongBounds = true;
        }
        for (int i = 0; i < numPoints; i++) {
            long x = longCoords[i*2];
            long y = longCoords[i*2 + 1];
            if (x < longMinX) longMinX = x;
            if (x > longMaxX) longMaxX = x;
            if (y < longMinY) longMinY = y;
            if (y > longMaxY) longMaxY = y;
        }
    }
    
    @Override
    public void addIntLine(int[] coords, Poly.Type style, Layer layer) { throw new UnsupportedOperationException(); }
    
    @Override
    public void addIntBox(int[] coords, Layer layer) { throw new UnsupportedOperationException(); }
}
