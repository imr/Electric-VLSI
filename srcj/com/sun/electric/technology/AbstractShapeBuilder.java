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
    
    protected double[] doubleCoords = new double[8];
    int pointCount; 
    public int[] intCoords = new int[4];
    protected CellBackup.Memoization m;
    
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
    
    public CellBackup.Memoization getMemoization() {
        return m;
    }
    
    public void genShapeOfArc(ImmutableArcInst a) {
        if (a.genShapeEasy(this))
            return;
        pointCount = 0;
        a.protoType.tech.getShapeOfArc(this, a, null, onlyTheseLayers);
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
}
