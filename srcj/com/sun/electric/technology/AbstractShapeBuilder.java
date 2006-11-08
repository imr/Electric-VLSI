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
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Poly;

/**
 * A support class to build shapes of arcs and nodes.
 */
public abstract class AbstractShapeBuilder {
    protected Layer.Function.Set onlyTheseLayers;
    private long onlyTheseLayersMask = -1L;
    protected boolean reasonable;
    protected boolean electrical;
    
    protected long[] longCoords = new long[8];
    int pointCount; 
    public int[] intCoords = new int[4];
    protected CellBackup.Memoization m;
    
    /** Creates a new instance of AbstractShapeBuilder */
    public AbstractShapeBuilder() {
    }
    
    public void setOnlyTheseLayers(Layer.Function.Set onlyTheseLayers) {
        this.onlyTheseLayers = onlyTheseLayers;
        onlyTheseLayersMask = onlyTheseLayers != null ? onlyTheseLayers.bits : 0;
    }
    public void setReasonable(boolean b) { reasonable = b; }
    public void setElectrical(boolean b) { electrical = b; }
    
    public CellBackup.Memoization getMemoization() {
        return m;
    }
    
    public void genShapeOfArc(ImmutableArcInst a) {
        if (a.isEasyShape())
            genShapeOfArcEasy(a);
        else {
            pointCount = 0;
            a.protoType.tech.getShapeOfArc(this, a, null, onlyTheseLayers);
        }
    }
    
    private void genShapeOfArcEasy(ImmutableArcInst a) {
        int gridFullWidth = (int)a.getGridFullWidth();
        if (gridFullWidth == 0) {
            Technology.ArcLayer primLayer = a.protoType.getArcLayer(0);
            Layer layer = primLayer.getLayer();
            if (onlyTheseLayers != null && onlyTheseLayers.contains(layer.getFunction())) return;
//            if (((int)(onlyTheseLayersMask >> layer.getFunction().ordinal()) & 1) == 0) return;
            Poly.Type style = primLayer.getStyle();
            if (style == Poly.Type.FILLED) style = Poly.Type.OPENED;
            intCoords[0] = (int)a.tailLocation.getGridX();
            intCoords[1] = (int)a.tailLocation.getGridY();
            intCoords[2] = (int)a.headLocation.getGridX();
            intCoords[3] = (int)a.headLocation.getGridY();
            addIntLine(intCoords, style, primLayer.getLayer());
            return;
        }
        for (Technology.ArcLayer primLayer: a.protoType.getLayers()) {
            Layer layer = primLayer.getLayer();
            assert primLayer.getStyle() == Poly.Type.FILLED;
            if (onlyTheseLayers != null && onlyTheseLayers.contains(layer.getFunction())) continue;
//            if (((int)(onlyTheseLayersMask >> layer.getFunction().ordinal()) & 1) == 0) continue;
            a.makeGridBoxInt(this, gridFullWidth - (int)primLayer.getGridOffset());
            addIntBox(intCoords, layer);
        }
    }
    
    public void pushPoint(EPoint p, double gridX, double gridY) {
        pushPoint(p.getGridX() + gridX, p.getGridY() + gridY);
    }
    
    public void pushPoint(double gridX, double gridY) {
        pushPoint(DBMath.roundLong(gridX), DBMath.roundLong(gridY));
    }
    
    public void pushPoint(EPoint p) {
        pushPoint(p.getGridX(), p.getGridY());
    }
    
    public void pushPoint(long gridX, long gridY) {
        if (pointCount*2 >= longCoords.length)
            resize();
        longCoords[pointCount*2] = gridX;
        longCoords[pointCount*2 + 1] = gridY;
        pointCount++;
    }
    
    private void resize() {
        long[] newLongPoints = new long[longCoords.length*2];
        System.arraycopy(longCoords, 0, newLongPoints, 0, longCoords.length);
        longCoords = newLongPoints;
    }
    
    public void pushPoly(Poly.Type style, Layer layer) {
        addLongPoly(pointCount, style, layer);
        pointCount = 0;
    }
    
    public void pushBox(int minX, int minY, int maxX, int maxY, Layer layer) {
        intCoords[0] = minX;
        intCoords[1] = minY;
        intCoords[2] = maxX;
        intCoords[3] = maxY;
        addIntBox(intCoords, layer);
    }
    
    public abstract void addLongPoly(int numPoints, Poly.Type style, Layer layer);
    
    public abstract void addIntLine(int[] coords, Poly.Type style, Layer layer);
    public abstract void addIntBox(int[] coords, Layer layer);
}
