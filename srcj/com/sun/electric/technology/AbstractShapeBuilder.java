/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AbstractShapeBuilder.java
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
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.prototype.NodeProtoId;
import java.awt.geom.Point2D;

/**
 * A support class to build shapes of arcs and nodes.
 */
public abstract class AbstractShapeBuilder {
    protected Layer.Function.Set onlyTheseLayers;
    private long onlyTheseLayersMask = -1L;
    protected boolean reasonable;
    protected boolean electrical;
    
    protected double[] doubleCoords = new double[8];
    protected int pointCount; 
    public int[] intCoords = new int[4];
    private Shrinkage shrinkage;
    
    /** Creates a new instance of AbstractShapeBuilder */
    public AbstractShapeBuilder() {
    }
   
    public Layer.Function.Set getOnlyTheseLayers() { return onlyTheseLayers; }
    public void setOnlyTheseLayers(Layer.Function.Set onlyTheseLayers) {
        this.onlyTheseLayers = onlyTheseLayers;
        onlyTheseLayersMask = onlyTheseLayers != null ? onlyTheseLayers.bits : 0;
    }
    public void setReasonable(boolean b) { reasonable = b; }
    public void setElectrical(boolean b) { electrical = b; }
    
    public Shrinkage getShrinkage() {
        return shrinkage;
    }
    public void setShrinkage(Shrinkage shrinkage) {
        this.shrinkage = shrinkage;
    }
    
    public void genShapeOfArc(ImmutableArcInst a) {
        if (genShapeEasy(a))
            return;
        pointCount = 0;
        a.protoType.tech.getShapeOfArc(this, a);
    }
    
	/**
	 * Method to fill in an AbstractShapeBuilder a polygon that describes this ImmutableArcInst in grid units.
	 * The polygon is described by its width, and style.
     * @param b shape builder.
	 * @param gridWidth the gridWidth of the Poly.
	 * @param style the style of the Poly.
	 */
    public void makeGridPoly(ImmutableArcInst a, long gridWidth, Poly.Type style, Layer layer) {
        long[] result;
        if (a.protoType.isCurvable()) {
            // get the radius information on the arc
            Double radiusDouble = a.getRadius();
            if (radiusDouble != null && curvedArcGridOutline(a, gridWidth, DBMath.lambdaToGrid(radiusDouble))) {
                pushPoly(style, layer);
                return;
            }
        }
        
        // zero-width polygons are simply lines
        if (gridWidth <= 0) {
            pushPoint(a.tailLocation);
            pushPoint(a.headLocation);
            if (style == Poly.Type.FILLED) style = Poly.Type.OPENED;
            pushPoly(style, layer);
            return;
        }
        
        // make the polygon
		int w2 = ((int)gridWidth) >>> 1;
        short shrinkT = a.isTailExtended() ? shrinkage.get(a.tailNodeId) : Shrinkage.EXTEND_0;
        short shrinkH = a.isHeadExtended() ? shrinkage.get(a.headNodeId) : Shrinkage.EXTEND_0;

        int angle = a.getAngle();
        double w2x = DBMath.roundShapeCoord(w2*GenMath.cos(angle));
        double w2y = DBMath.roundShapeCoord(w2*GenMath.sin(angle));
        double tx = 0;
        double ty = 0;
        if (shrinkT == Shrinkage.EXTEND_90) {
            tx = -w2x;
            ty = -w2y;
        } else if (shrinkT != Shrinkage.EXTEND_0) {
            Point2D e = computeExtension(w2, -w2x, -w2y, a.getOppositeAngle(), shrinkT);
            tx = e.getX();
            ty = e.getY();
        }
        double hx = 0;
        double hy = 0;
        if (shrinkH == Shrinkage.EXTEND_90) {
            hx = w2x;
            hy = w2y;
        } else if (shrinkH != Shrinkage.EXTEND_0) {
            Point2D e = computeExtension(w2, w2x, w2y, angle, shrinkH);
            hx = e.getX();
            hy = e.getY();
        }
        
        pushPoint(a.tailLocation, tx - w2y, ty + w2x);
        pushPoint(a.tailLocation, tx + w2y, ty - w2x);
        pushPoint(a.headLocation, hx + w2y, hy - w2x);
        pushPoint(a.headLocation, hx - w2y, hy + w2x);
        
        // somewhat simpler if rectangle is manhattan
        if (gridWidth != 0 && style.isOpened())
            pushPoint(a.tailLocation, tx - w2y, ty + w2x);
        pushPoly(style, layer);
    }
    
    /**
     * Computes extension vector of wire, 
     */
    public static Point2D computeExtension(int w2, double ix1, double iy1, int angle, short shrink) {
        if (shrink == Shrinkage.EXTEND_90) return new Point2D.Double(ix1, iy1);
        if (shrink == Shrinkage.EXTEND_0) return new Point2D.Double(0, 0);
        assert shrink >= Shrinkage.EXTEND_ANY;
        int angle2 = (shrink - Shrinkage.EXTEND_ANY) - angle;
        if (angle2 < 0)
            angle2 += 3600;
        double x1 = ix1;
        double y1 = iy1;
        double s1;
        if (y1 == 0) {
            s1 = x1;
            if (x1 == 0) return new Point2D.Double(0, 0);
            x1 = x1 > 0 ? 1 : -1;
        } else if (x1 == 0) {
            s1 = y1;
            y1 = y1 > 0 ? 1 : -1;
        } else {
            s1 = x1*x1 + y1*y1;
        }
        
        double x2 = DBMath.roundShapeCoord(w2*GenMath.cos(angle2));
        double y2 = DBMath.roundShapeCoord(w2*GenMath.sin(angle2));
        double s2;
        if (y2 == 0) {
            s2 = x2;
            if (x2 == 0) return new Point2D.Double(0, 0);
            x2 = x2 > 0 ? 1 : -1;
        } else if (x2 == 0) {
            s2 = y2;
            y2 = y2 > 0 ? 1 : -1;
        } else {
            s2 = x2*x2 + y2*y2;
        }
        
        double det = x1*y2 - y1*x2;
        if (det == 0) return new Point2D.Double(0, 0);
        double x = (x2*s1 + x1*s2)/det;
        double y = (y2*s1 + y1*s2)/det;
        x = DBMath.roundShapeCoord(x);
        y = DBMath.roundShapeCoord(y);
        x = x + iy1;
        y = y - ix1;
        if (det < 0) {
            x = -x;
            y = -y;
        }
        return new Point2D.Double(x, y);
    }
    
	/**
	 * when arcs are curved, the number of line segments will be
	 * between this value, and half of this value.
	 */
	private static final int MAXARCPIECES = 16;

	/**
     * Method to fill polygon "poly" with the outline in grid units of the curved arc in
     * this ImmutableArcInst whose width in grid units is "gridWidth".
     * If there is no curvature information in the arc, the routine returns false,
     * otherwise it returns the curved polygon.
     * @param b builder to fill points
     * @param gridWidth width in grid units.
     * @param gridRadius radius in grid units.
     * @return true if point were filled to the buuilder
     */
    public boolean curvedArcGridOutline(ImmutableArcInst a, long gridWidth, long gridRadius) {
        // get information about the curved arc
        long pureGridRadius = Math.abs(gridRadius);
        double gridLength = a.getGridLength();
        
        // see if the lambdaRadius can work with these arc ends
        if (pureGridRadius*2 < gridLength) return false;
        
        // determine the center of the circle
        Point2D [] centers = DBMath.findCenters(pureGridRadius, a.headLocation.gridMutable(), a.tailLocation.gridMutable());
        if (centers == null) return false;
        
        Point2D centerPt = centers[1];
        if (gridRadius < 0) {
            centerPt = centers[0];
        }
        double centerX = centerPt.getX();
        double centerY = centerPt.getY();
        
        // determine the base and range of angles
        int angleBase = DBMath.figureAngle(a.headLocation.getGridX() - centerX, a.headLocation.getGridY() - centerY);
        int angleRange = DBMath.figureAngle(a.tailLocation.getGridX() - centerX, a.tailLocation.getGridY() - centerY);
        angleRange -= angleBase;
        if (angleRange < 0) angleRange += 3600;
        
        // force the curvature to be the smaller part of a circle (used to determine this by the reverse-ends bit)
        if (angleRange > 1800) {
            angleBase += angleRange;
            if (angleBase < 0) angleBase += 3600;
            angleRange = 3600 - angleRange;
        }
        
        // determine the number of intervals to use for the arc
        int pieces = angleRange;
        while (pieces > MAXARCPIECES) pieces /= 2;
        if (pieces == 0) return false;
        
        // get the inner and outer radii of the arc
        double outerRadius = pureGridRadius + gridWidth / 2;
        double innerRadius = outerRadius - gridWidth;
        
        // fill the polygon
        for(int i=0; i<=pieces; i++) {
            int angle = (angleBase + i * angleRange / pieces) % 3600;
            pushPoint(DBMath.cos(angle) * innerRadius + centerX, DBMath.sin(angle) * innerRadius + centerY);
        }
        for(int i=pieces; i>=0; i--) {
            int angle = (angleBase + i * angleRange / pieces) % 3600;
            pushPoint(DBMath.cos(angle) * outerRadius + centerX, DBMath.sin(angle) * outerRadius + centerY);
        }
        return true;
    }
    
    /**
     * Generate shape of this ImmutableArcInst in easy case.
     * @param b AbstractShapeBuilder to generate to.
     * @return true if shape was generated.
     */
    public boolean genShapeEasy(ImmutableArcInst a) {
        if (!a.isEasyShape()) return false;
        ArcProto protoType = a.protoType;
        int gridFullWidth = (int)a.getGridFullWidth();
        if (gridFullWidth == 0) {
            Technology.ArcLayer primLayer = protoType.getArcLayer(0);
            Layer layer = primLayer.getLayer();
            if (onlyTheseLayers != null && !onlyTheseLayers.contains(layer.getFunction())) return true;
            Poly.Type style = primLayer.getStyle();
            if (style == Poly.Type.FILLED) style = Poly.Type.OPENED;
            intCoords[0] = (int)a.tailLocation.getGridX();
            intCoords[1] = (int)a.tailLocation.getGridY();
            intCoords[2] = (int)a.headLocation.getGridX();
            intCoords[3] = (int)a.headLocation.getGridY();
            addIntLine(intCoords, style, primLayer.getLayer());
            return true;
        }
        if (gridFullWidth <= protoType.getMaxLayerGridOffset())
            return false;
        boolean tailExtended = false;
        if (a.isTailExtended()) {
            short shrinkT = shrinkage.get(a.tailNodeId);
            if (shrinkT == Shrinkage.EXTEND_90)
                tailExtended = true;
            else if (shrinkT != Shrinkage.EXTEND_0)
                return false;
        }
        boolean headExtended = false;
        if (a.isHeadExtended()) {
            short shrinkH = shrinkage.get(a.headNodeId);
            if (shrinkH == Shrinkage.EXTEND_90)
                headExtended = true;
            else if (shrinkH != Shrinkage.EXTEND_0)
                return false;
        }
        for (int i = 0, n = protoType.getNumArcLayers(); i < n; i++) {
            Technology.ArcLayer primLayer = protoType.getArcLayer(i);
            Layer layer = primLayer.getLayer();
            assert primLayer.getStyle() == Poly.Type.FILLED;
            if (onlyTheseLayers != null && !onlyTheseLayers.contains(layer.getFunction())) continue;
            a.makeGridBoxInt(intCoords, tailExtended, headExtended, gridFullWidth - (int)primLayer.getGridOffset());
            addIntBox(intCoords, layer);
        }
        return true;
    }
    
    public void pushPoint(EPoint p, double gridX, double gridY) {
        pushPointLow(p.getGridX() + DBMath.roundShapeCoord(gridX), p.getGridY() + DBMath.roundShapeCoord(gridY));
    }
    
    public void pushPoint(double gridX, double gridY) {
        pushPointLow(DBMath.roundShapeCoord(gridX), DBMath.roundShapeCoord(gridY));
    }
    
    public void pushPoint(EPoint p) {
        pushPointLow(p.getGridX(), p.getGridY());
    }
    
    private void pushPointLow(double gridX, double gridY) {
        if (pointCount*2 >= doubleCoords.length)
            resize();
        doubleCoords[pointCount*2] = gridX;
        doubleCoords[pointCount*2 + 1] = gridY;
        pointCount++;
    }
    
    private void resize() {
        double[] newDoubleCoords = new double[doubleCoords.length*2];
        System.arraycopy(doubleCoords, 0, newDoubleCoords, 0, doubleCoords.length);
        doubleCoords = newDoubleCoords;
    }
    
    public void pushPoly(Poly.Type style, Layer layer) {
        addDoublePoly(pointCount, style, layer);
        pointCount = 0;
    }
    
    public void pushBox(int minX, int minY, int maxX, int maxY, Layer layer) {
        intCoords[0] = minX;
        intCoords[1] = minY;
        intCoords[2] = maxX;
        intCoords[3] = maxY;
        addIntBox(intCoords, layer);
    }
    
    public abstract void addDoublePoly(int numPoints, Poly.Type style, Layer layer);
    
    public abstract void addIntLine(int[] coords, Poly.Type style, Layer layer);
    public abstract void addIntBox(int[] coords, Layer layer);
    
    public static class Shrinkage {
        public static final short EXTEND_90 = 0;
        public static final short EXTEND_0 = 1;
        private static final short EXTEND_ANY = 2;
    
        private static final int ANGLE_SHIFT = 12;
        private static final int ANGLE_MASK = (1 << ANGLE_SHIFT) - 1;
        private static final int ANGLE_DIAGONAL_MASK = 1 << (ANGLE_SHIFT*2);
        private static final int ANGLE_COUNT_SHIFT = ANGLE_SHIFT*2 + 1;
        
        private final short[] shrink;
        
        public Shrinkage() {
            shrink = new short[0];
        }
        
        public Shrinkage(CellBackup cellBackup) {
            int maxNodeId = -1;
            for (int nodeIndex = 0; nodeIndex < cellBackup.nodes.size(); nodeIndex++)
                maxNodeId = Math.max(maxNodeId, cellBackup.nodes.get(nodeIndex).nodeId);
            int[] angles = new int[maxNodeId+1];
            for (ImmutableArcInst a: cellBackup.arcs) {
                if (a.getGridFullWidth() == 0) continue;
                if (a.tailNodeId == a.headNodeId && a.tailPortId == a.headPortId) {
                    // Fake register for full shrinkage
                    registerArcEnd(angles, a.tailNodeId, 0, false, false);
                    continue;
                }
                boolean is90 = a.isManhattan();
                registerArcEnd(angles, a.tailNodeId, a.getOppositeAngle(), is90, a.isTailExtended());
                registerArcEnd(angles, a.headNodeId, a.getAngle(), is90, a.isHeadExtended());
            }
            short[] shrink = new short[maxNodeId + 1];
            for (int nodeIndex = 0; nodeIndex < cellBackup.nodes.size(); nodeIndex++) {
                ImmutableNodeInst n = cellBackup.nodes.get(nodeIndex);
                NodeProtoId np = n.protoId;
                if (np instanceof PrimitiveNode && ((PrimitiveNode)np).isArcsShrink())
                    shrink[n.nodeId] = computeShrink(angles[nodeIndex]);
            }
            this.shrink = shrink;
        }
        
        /**
         * Method to tell the "end shrink" factors on all arcs on a specified ImmutableNodeInst.
         * EXTEND_90 indicates no shortening (extend the arc by half its width).
         * EXTEND_0 indicates no extend.
         * EXTEND_ANY + [0..3600) is a sum of arc angles modulo 3600
         * if this ImmutableNodeInst is a pin which can "isArcsShrink" and this pin connects
         * exactly two arcs whit extended ends and angle between arcs is accute.
         * @param nodeId nodeId of specified ImmutableNodeInst
         * @return shrink factor of specified ImmutableNodeInst is wiped.
         */
        public short get(int nodeId) {
            return nodeId < shrink.length ? shrink[nodeId] : 0;
        }
        
    private void registerArcEnd(int[] angles, int nodeId, int angle, boolean is90, boolean extended) {
        assert angle >= 0 && angle < 3600;
        int ang = angles[nodeId];
        if (extended) {
            int count = ang >>> ANGLE_COUNT_SHIFT;
            switch (count) {
                case 0:
                    ang |= angle;
                    ang += (1 << ANGLE_COUNT_SHIFT);
                    break;
                case 1:
                    ang |= (angle << ANGLE_SHIFT);
                    ang += (1 << ANGLE_COUNT_SHIFT);
                    break;
                case 2:
                    ang += (1 << ANGLE_COUNT_SHIFT);
                    break;
            }
            if (!is90)
                ang |= ANGLE_DIAGONAL_MASK;
        } else {
            ang |= (3 << ANGLE_COUNT_SHIFT);
        }
        angles[nodeId] = ang;
    }
    
    static short computeShrink(int angs) {
        boolean hasAny = (angs&ANGLE_DIAGONAL_MASK) != 0;
        int count = angs >>> ANGLE_COUNT_SHIFT;
        
        if (hasAny && count == 2) {
            int ang0 = angs & ANGLE_MASK;
            int ang1 = (angs >> ANGLE_SHIFT) & ANGLE_MASK;
            int da = ang0 > ang1 ? ang0 - ang1 : ang1 - ang0;
            if (da == 900 || da == 2700) return EXTEND_90;
            if (da == 1800) return EXTEND_0;
            if (900 < da && da < 2700) {
                int a = ang0 + ang1;
                if (a >= 3600)
                    a -= 3600;
                return (short)(EXTEND_ANY + a);
            }
        }
        return EXTEND_90;
    }
        
    }
}
