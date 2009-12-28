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
package com.sun.electric.technology;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Schematics;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * A support class to build shapes of arcs and nodes.
 */
public class BoundsBuilder extends AbstractShapeBuilder {
    private int intMinX, intMinY, intMaxX, intMaxY;
    private double doubleMinX, doubleMinY, doubleMaxX, doubleMaxY;
    private boolean hasIntBounds, hasDoubleBounds;

    public BoundsBuilder(CellBackup cellBackup) {
        setup(cellBackup, null, false, true, false, null);
        clear();
    }

    public void clear() {
        hasIntBounds = hasDoubleBounds = false;
    }

    /**
     * Generate bounds of this ImmutableArcInst in easy case.
     * @param a ImmutableArcInst to examine.
     * @param intCoords integer coords to fill.
     * @return true if bounds were generated.
     */
    public boolean genBoundsEasy(ImmutableArcInst a, int[] intCoords) {
        if (getMemoization().isHardArc(a.arcId)) return false;
        int gridExtendOverMin = (int)a.getGridExtendOverMin();
        ArcProto protoType = getTechPool().getArcProto(a.protoId);
        int minLayerExtend = gridExtendOverMin + protoType.getMinLayerGridExtend();
        if (minLayerExtend == 0) {
            assert protoType.getNumArcLayers() == 1;
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
            boolean tailExtended = false;
            if (a.isTailExtended()) {
                short shrinkT = getShrinkage().get(a.tailNodeId);
                if (shrinkT == AbstractShapeBuilder.Shrinkage.EXTEND_90)
                    tailExtended = true;
                else if (shrinkT != AbstractShapeBuilder.Shrinkage.EXTEND_0)
                    return false;
            }
            boolean headExtended = false;
            if (a.isHeadExtended()) {
                short shrinkH = getShrinkage().get(a.headNodeId);
                if (shrinkH == AbstractShapeBuilder.Shrinkage.EXTEND_90)
                    headExtended = true;
                else if (shrinkH != AbstractShapeBuilder.Shrinkage.EXTEND_0)
                    return false;
            }
            a.makeGridBoxInt(intCoords, tailExtended, headExtended, gridExtendOverMin + protoType.getMaxLayerGridExtend());
        }
        return true;
    }

    public boolean genBoundsEasy(ImmutableNodeInst n, int[] intCoords) {
        TechPool techPool = getTechPool();
		PrimitiveNode pn = techPool.getPrimitiveNode((PrimitiveNodeId)n.protoId);
        Technology tech = pn.getTechnology();
        ERectangle full = pn.getFullRectangle();
        long gridWidth = n.size.getGridX() + full.getGridWidth();
        long gridHeight = n.size.getGridY() + full.getGridHeight();
		if (gridWidth == 0 && gridHeight == 0)
		{
            intCoords[0] = intCoords[2] = (int)n.anchor.getGridX();
            intCoords[1] = intCoords[3] = (int)n.anchor.getGridY();
            return true;
		}

		// special case for arcs of circles
        if (tech instanceof Artwork && (pn == techPool.getArtwork().circleNode || pn == techPool.getArtwork().thickCircleNode)) {
			// see if this circle is only a partial one
			double [] angles = n.getArcDegrees();
			if (angles[0] != 0.0 || angles[1] != 0.0)
                return false;
        }

		// schematic bus pins are so complex that only the technology knows their true size
		if (tech instanceof Schematics && pn == techPool.getSchematics().busPinNode)
            return false;

		// special case for pins that become steiner points
		if (pn.isWipeOn1or2() && !getMemoization().hasExports(n) && getMemoization().pinUseCount(n)) {
            intCoords[0] = intCoords[2] = (int)n.anchor.getGridX();
            intCoords[1] = intCoords[3] = (int)n.anchor.getGridY();
            return true;
		}

        // special case for polygonally-defined nodes: compute precise geometry
		if (pn.isHoldsOutline() && n.getTrace() != null)
            return false;

        int w2 = (int)(gridWidth >> 1);
        int h2 = (int)(gridHeight >> 1);
        if (n.orient.isManhattan()) {
            intCoords[0] = -w2;
            intCoords[1] = -h2;
            intCoords[2] = w2;
            intCoords[3] = h2;
            n.orient.rectangleBounds(intCoords);
        } else {
            Rectangle2D.Double mbb = new Rectangle2D.Double();
            n.orient.rectangleBounds(-w2, -h2, w2, h2, 0, 0, mbb);
            intCoords[0] = GenMath.floorInt(mbb.getMinX());
            intCoords[1] = GenMath.floorInt(mbb.getMinY());
            intCoords[2] = -intCoords[0];
            intCoords[3] = -intCoords[1];
        }
        intCoords[0] += (int)n.anchor.getGridX();
        intCoords[1] += (int)n.anchor.getGridY();
        intCoords[2] += (int)n.anchor.getGridX();
        intCoords[3] += (int)n.anchor.getGridY();
        return true;
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

    public boolean makeBounds(EPoint anchor, Rectangle2D.Double visBounds) {
        double x, y, w, h;
        if (!hasDoubleBounds) {
            if (hasIntBounds) {
                x = intMinX;
                y = intMinY;
                int iw = intMaxX - intMinX;
                w = iw >= 0 ? iw : (long)intMaxX - (long)intMinX;
                int ih = intMaxY - intMinY;
                h = ih >= 0 ? ih : (long)intMaxY - (long)intMinY;
            } else {
                x = anchor.getGridX();
                y = anchor.getGridY();
                w = h = 0;
            }
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
    public void addDoublePoly(int numPoints, Poly.Type style, Layer layer, EGraphics graphicsOverride, PrimitivePort pp) {
		if (style == Poly.Type.CIRCLEARC || style == Poly.Type.THICKCIRCLEARC)
		{
            Point2D.Double p0 = new Point2D.Double(doubleCoords[0], doubleCoords[1]);
            Point2D.Double p1 = new Point2D.Double(doubleCoords[2], doubleCoords[3]);
            Point2D.Double p2 = new Point2D.Double(doubleCoords[4], doubleCoords[5]);
			Rectangle2D bounds = GenMath.arcBBox(p1, p2, p0);
            if (!hasDoubleBounds) {
                doubleMinX = doubleMinY = Double.POSITIVE_INFINITY;
                doubleMaxX = doubleMaxY = Double.NEGATIVE_INFINITY;
                hasDoubleBounds = true;
            }
            if (bounds.getMinX() < doubleMinX) doubleMinX  = bounds.getMinX();
            if (bounds.getMaxX() > doubleMaxX) doubleMaxX  = bounds.getMaxX();
            if (bounds.getMinY() < doubleMinY) doubleMinY  = bounds.getMinY();
            if (bounds.getMaxY() > doubleMaxY) doubleMaxY  = bounds.getMaxY();
			return;
		}

        if (!hasDoubleBounds) {
            assert numPoints > 0;
            doubleMinX = doubleMaxX = doubleCoords[0];
            doubleMinY = doubleMaxY = doubleCoords[1];
            hasDoubleBounds = true;
        }
		if (style == Poly.Type.CIRCLE || style == Poly.Type.THICKCIRCLE || style == Poly.Type.DISC)
		{
			double cX = doubleCoords[0];
			double cY = doubleCoords[1];
			double radius = Point2D.distance(cX, cY, doubleCoords[2], doubleCoords[3]);
            if (cX - radius < doubleMinX) doubleMinX = cX - radius;
            if (cX + radius > doubleMaxX) doubleMaxX = cX + radius;
            if (cY - radius < doubleMinY) doubleMinY = cY - radius;
            if (cY + radius > doubleMaxY) doubleMaxY = cY + radius;
			return;
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
    public void addIntPoly(int numPoints, Poly.Type style, Layer layer, EGraphics graphicsOverride, PrimitivePort pp) {
        int i = 0;
        if (!hasIntBounds) {
            int x = intCoords[0];
            int y = intCoords[1];
            intMinX = x;
            intMinY = y;
            intMaxX = x;
            intMaxY = y;
            hasIntBounds = true;
            i = 1;
         }
        while (i < numPoints) {
            int x = intCoords[i*2];
            int y = intCoords[i*2 + 1];
            if (x < intMinX) intMinX = x;
            if (x > intMinY) intMinY = x;
            if (y < intMinY) intMinY = y;
            if (y > intMaxY) intMaxY = y;
            i++;
         }
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
