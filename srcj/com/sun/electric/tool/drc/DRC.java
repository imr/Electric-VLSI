/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DRC.java
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
package com.sun.electric.tool.drc;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.constraint.Layout;
import com.sun.electric.database.geometry.*;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.*;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * This is the Design Rule Checker tool.
 */
public class DRC extends Listener
{
	/** the DRC tool. */								     protected static DRC tool = new DRC();
	/** overrides of rules for each technology. */		     private static Map<Technology,Pref> prefDRCOverride = new HashMap<Technology,Pref>();
	/** map of cells and their objects to DRC */		     private static Map<Cell,Set<Geometric>> cellsToCheck = new HashMap<Cell,Set<Geometric>>();
    /** to temporary store DRC dates for spacing checking */ private static Map<Cell,StoreDRCInfo> storedSpacingDRCDate = new HashMap<Cell,StoreDRCInfo>();
    /** to temporary store DRC dates for area checking */    private static Map<Cell,StoreDRCInfo> storedAreaDRCDate = new HashMap<Cell,StoreDRCInfo>();
    /** for logging incremental errors */                    private static ErrorLogger errorLoggerIncremental = ErrorLogger.newInstance("DRC (incremental)", true);
    static final double TINYDELTA = DBMath.getEpsilon()*1.1;
    /** key of Variable holding DRC Cell annotations. */	static final Variable.Key DRC_ANNOTATION_KEY = Variable.newKey("ATTR_DRC");

    static Layer.Function.Set getMultiLayersSet(Layer layer)
    {
        Layer.Function.Set thisLayerFunction = (layer.getFunction().isPoly()) ?
        new Layer.Function.Set(Layer.Function.POLY1, Layer.Function.GATE) :
        new Layer.Function.Set(layer.getFunction(), layer.getFunctionExtras());
        return thisLayerFunction;
    }

    /**
	 * Method to see if polygons in "pList" (describing arc "ai") should be cropped against a
     * connecting transistor.  Crops the polygon if so.
     */
    static void cropActiveArc(ArcInst ai, boolean ignoreCenterCuts, Poly [] pList)
    {
        // look for an active layer in this arc
        int tot = pList.length;
        int diffPoly = -1;
        for(int j=0; j<tot; j++)
        {
            Poly poly = pList[j];
            Layer layer = poly.getLayer();
            if (layer == null) continue;
            Layer.Function fun = layer.getFunction();
            if (fun.isDiff()) { diffPoly = j;   break; }
        }
        if (diffPoly < 0) return;
        Poly poly = pList[diffPoly];

        // must be manhattan
        Rectangle2D polyBounds = poly.getBox();
        if (polyBounds == null) return;
        polyBounds = new Rectangle2D.Double(polyBounds.getMinX(), polyBounds.getMinY(), polyBounds.getWidth(), polyBounds.getHeight());

        // search for adjoining transistor in the cell
        boolean cropped = false;
        boolean halved = false;
        for(int i=0; i<2; i++)
        {
            PortInst pi = ai.getPortInst(i);
            NodeInst ni = pi.getNodeInst();
            if (!ni.getFunction().isFET()) continue;

            // crop the arc against this transistor
            AffineTransform trans = ni.rotateOut();
            Technology tech = ni.getProto().getTechnology();
            Poly [] activeCropPolyList = tech.getShapeOfNode(ni, false, ignoreCenterCuts, null);
            int nTot = activeCropPolyList.length;
            for(int k=0; k<nTot; k++)
            {
                Poly nPoly = activeCropPolyList[k];
                if (nPoly.getLayer() != poly.getLayer()) continue;
                nPoly.transform(trans);
                Rectangle2D nPolyBounds = nPoly.getBox();
                if (nPolyBounds == null) continue;
                // @TODO Why only one half is half crop?
                // Should I change cropBox by cropBoxComplete?
                int result = (halved) ?
                        Poly.cropBox(polyBounds, nPolyBounds) :
                        Poly.halfCropBox(polyBounds, nPolyBounds);

                if (result == 1)
                {
                    // remove this polygon from consideration
                    poly.setLayer(null);
                    return;
                }
                cropped = true;
                halved = true;
            }
        }
        if (cropped)
        {
            Poly.Type style = poly.getStyle();
            Layer layer = poly.getLayer();
            poly = new Poly(polyBounds);
            poly.setStyle(style);
            poly.setLayer(layer);
            pList[diffPoly] = poly;
        }
    }

    /**
         * Method to examine cell "cell" in the area (lx<=X<=hx, ly<=Y<=hy) for objects
     * on layer "layer".  Apply transformation "moreTrans" to the objects.  If polygons are
     * found at (xf1,yf1) or (xf2,yf2) or (xf3,yf3) then sets "p1found/p2found/p3found" to 1.
     *  If poly1 or poly2 is not null, ignore geometries that are identical to them.
     * If all locations are found, returns true.
     */
    static boolean lookForLayerCoverage(Geometric geo1, Poly poly1, Geometric geo2, Poly poly2, Cell cell,
                                   Layer layer, AffineTransform moreTrans, Rectangle2D bounds,
                                   Point2D pt1, Point2D pt2, Point2D pt3, boolean[] pointsFound,
                                   boolean overlap, Layer.Function.Set layerFunction, boolean ignoreSameGeometry,
                                   boolean ignoreCenterCuts)
    {
        int j;
        Rectangle2D newBounds = new Rectangle2D.Double();  // Sept 30

        for (Iterator<RTBounds> it = cell.searchIterator(bounds); it.hasNext();)
        {
            RTBounds g = it.next();

            // You can't skip the same geometry otherwise layers in the same Geometric won't
            // be tested.
            // But it is necessary while testing flat geometries... in minWidthInternal
            if (ignoreSameGeometry && (g == geo1 || g == geo2))
                continue;

            // I can't skip geometries to exclude from the search
            if (g instanceof NodeInst)
            {
                NodeInst ni = (NodeInst) g;
                if (NodeInst.isSpecialNode(ni))
                    continue; // Nov 16, no need for checking pins or other special nodes;
                if (ni.isCellInstance())
                {
                    // compute bounding area inside of sub-cell
                    AffineTransform rotI = ni.rotateIn();
                    AffineTransform transI = ni.translateIn();
                    rotI.preConcatenate(transI);
                    newBounds.setRect(bounds);
                    DBMath.transformRect(newBounds, rotI);

                    // compute new matrix for sub-cell examination
                    AffineTransform trans = ni.translateOut(ni.rotateOut());
                    trans.preConcatenate(moreTrans);
                    if (lookForLayerCoverage(geo1, poly1, geo2, poly2, (Cell) ni.getProto(), layer, trans, newBounds,
                        pt1, pt2, pt3, pointsFound, overlap, layerFunction, false, ignoreCenterCuts))
                        return true;
                    continue;
                }
                AffineTransform bound = ni.rotateOut();
                bound.preConcatenate(moreTrans);
                Technology tech = ni.getProto().getTechnology();
                // I have to ask for electrical layers otherwise it will retrieve one polygon for polysilicon
                // and poly.polySame(poly1) will never be true. CONTRADICTION!
                Poly[] layerLookPolyList = tech.getShapeOfNode(ni, false, ignoreCenterCuts, layerFunction); // consistent change!);
                int tot = layerLookPolyList.length;
                for (int i = 0; i < tot; i++)
                {
                    Poly poly = layerLookPolyList[i];
                    // sameLayer test required to check if Active layer is not identical to thick active layer
                    if (!tech.sameLayer(poly.getLayer(), layer))
                    {
                        continue;
                    }

                    // Should be the transform before?
                    poly.transform(bound);

                    if (poly1 != null && !overlap && poly.polySame(poly1))
                        continue;
                    if (poly2 != null && !overlap && poly.polySame(poly2))
                        continue;

                    if (!pointsFound[0] && poly.isInside(pt1))
                        pointsFound[0] = true; // @TODO Should still evaluate isInside if pointsFound[i] is already valid?
                    if (!pointsFound[1] && poly.isInside(pt2)) pointsFound[1] = true;
                    if (pt3 != null && !pointsFound[2] && poly.isInside(pt3)) pointsFound[2] = true;
                    for (j = 0; j < pointsFound.length && pointsFound[j]; j++) ;
                    if (j == pointsFound.length) return true;
                    // No need of checking rest of the layers
                    break; // assuming only 1 polygon per layer (non-electrical)
                }
            } else
            {
                ArcInst ai = (ArcInst) g;
                Technology tech = ai.getProto().getTechnology();
                Poly[] layerLookPolyList = tech.getShapeOfArc(ai, layerFunction); // consistent change!);
                int tot = layerLookPolyList.length;
                for (int i = 0; i < tot; i++)
                {
                    Poly poly = layerLookPolyList[i];
                    // sameLayer test required to check if Active layer is not identical to thich actice layer
                    if (!tech.sameLayer(poly.getLayer(), layer))
                    {
                        continue;
                    }
                    poly.transform(moreTrans);  // @TODO Should still evaluate isInside if pointsFound[i] is already valid?
                    if (!pointsFound[0] && poly.isInside(pt1)) pointsFound[0] = true;
                    if (!pointsFound[1] && poly.isInside(pt2)) pointsFound[1] = true;
                    if (pt3 != null && !pointsFound[2] && poly.isInside(pt3)) pointsFound[2] = true;
                    for (j = 0; j < pointsFound.length && pointsFound[j]; j++) ;
                    if (j == pointsFound.length) return true;
                    // No need of checking rest of the layers
                    break;
                }
            }

            for (j = 0; j < pointsFound.length && pointsFound[j]; j++) ;
            if (j == pointsFound.length)
            {
                assert (false); // test when otherwise the calculation is useless! System.out.println("When?");
                return true;
            }
        }
        return false;
    }

    /**
         * Method to determine if neighbor would help to cover the minimum conditions
 *
 * @return true if error was found (not warning)
 */
static boolean checkExtensionWithNeighbors(Cell cell, Geometric geom, Poly poly, Layer layer, Rectangle2D bounds,
                                            DRCTemplate minWidthRule, int dir, boolean onlyOne, boolean reportError,
                                            Layer.Function.Set layerFunction, ReportInfo reportInfo)
{
    double actual = 0;
    Point2D left1, left2, left3, right1, right2, right3;
    //if (bounds.getWidth() < minWidthRule.value)
    String msg = "";

    // potential problem along X
    if (dir == 0)
    {
        actual = bounds.getWidth();
        msg = "(X axis)";
        double leftW = bounds.getMinX() - TINYDELTA;
        left1 = new Point2D.Double(leftW, bounds.getMinY());
        left2 = new Point2D.Double(leftW, bounds.getMaxY());
        left3 = new Point2D.Double(leftW, bounds.getCenterY());
        double rightW = bounds.getMaxX() + TINYDELTA;
        right1 = new Point2D.Double(rightW, bounds.getMinY());
        right2 = new Point2D.Double(rightW, bounds.getMaxY());
        right3 = new Point2D.Double(rightW, bounds.getCenterY());
    } else
    {
        actual = bounds.getHeight();
        msg = "(Y axis)";
        double leftH = bounds.getMinY() - TINYDELTA;
        left1 = new Point2D.Double(bounds.getMinX(), leftH);
        left2 = new Point2D.Double(bounds.getMaxX(), leftH);
        left3 = new Point2D.Double(bounds.getCenterX(), leftH);
        double rightH = bounds.getMaxY() + TINYDELTA;
        right1 = new Point2D.Double(bounds.getMinX(), rightH);
        right2 = new Point2D.Double(bounds.getMaxX(), rightH);
        right3 = new Point2D.Double(bounds.getCenterX(), rightH);
    }
    // see if there is more of this layer adjoining on either side
    boolean[] pointsFound = new boolean[3];
    pointsFound[0] = pointsFound[1] = pointsFound[2] = false;
    Rectangle2D newBounds = new Rectangle2D.Double(bounds.getMinX() - TINYDELTA, bounds.getMinY() - TINYDELTA,
        bounds.getWidth() + TINYDELTA * 2, bounds.getHeight() + TINYDELTA * 2);
    boolean zeroWide = (bounds.getWidth() == 0 || bounds.getHeight() == 0);

    boolean overlapLayer = lookForLayer(poly, cell, layer, DBMath.MATID, newBounds,
        left1, left2, left3, pointsFound, layerFunction, reportInfo.ignoreCenterCuts); //) return false;
//        if (overlapLayer && !zeroWide) return false;
    if (overlapLayer) return false;

    // Try the other corner
    pointsFound[0] = pointsFound[1] = pointsFound[2] = false;
    overlapLayer = lookForLayer(poly, cell, layer, DBMath.MATID, newBounds,
        right1, right2, right3, pointsFound, layerFunction, reportInfo.ignoreCenterCuts); //) return false;
//        if (overlapLayer && !zeroWide) return false;
    if (overlapLayer) return false;

    DRCErrorType errorType = DRCErrorType.MINWIDTHERROR;
    String extraMsg = msg;
    String rule = minWidthRule.ruleName;

    // Only when the flat element is fully covered send the warning
    // otherwise it is considered an error.
    if (zeroWide && overlapLayer)
    {
        extraMsg = " but covered by other layer";
        errorType = DRCErrorType.ZEROLENGTHARCWARN;
        rule = null;
    }

    if (reportError)
        createDRCErrorLogger(reportInfo, errorType, extraMsg, cell, minWidthRule.getValue(0), actual, rule,
            (onlyOne) ? null : poly, geom, layer, null, null, null);
    return !overlapLayer;
}

    /**
         * Method to examine cell "cell" in the area (lx<=X<=hx, ly<=Y<=hy) for objects
     * on layer "layer".  Apply transformation "moreTrans" to the objects.  If polygons are
     * found at (xf1,yf1) or (xf2,yf2) or (xf3,yf3) then sets "p1found/p2found/p3found" to 1.
     * If all locations are found, returns true.
     */
    static boolean lookForLayer(Poly thisPoly, Cell cell, Layer layer, AffineTransform moreTrans,
                                Rectangle2D bounds, Point2D pt1, Point2D pt2, Point2D pt3, boolean[] pointsFound,
                                Layer.Function.Set layerFunction, boolean ignoreCenterCuts)
    {
        int j;
        boolean skip = false;
        Rectangle2D newBounds = new Rectangle2D.Double();  // sept 30

        for (Iterator<RTBounds> it = cell.searchIterator(bounds); it.hasNext();)
        {
            RTBounds g = it.next();
            if (g instanceof NodeInst)
            {
                NodeInst ni = (NodeInst) g;
                if (NodeInst.isSpecialNode(ni))
                    continue; // Nov 16, no need for checking pins or other special nodes;
                if (ni.isCellInstance())
                {
                    // compute bounding area inside of sub-cell
                    AffineTransform rotI = ni.rotateIn();
                    AffineTransform transI = ni.translateIn();
                    rotI.preConcatenate(transI);
                    newBounds.setRect(bounds);
                    DBMath.transformRect(newBounds, rotI);

                    // compute new matrix for sub-cell examination
                    AffineTransform trans = ni.translateOut(ni.rotateOut());
                    trans.preConcatenate(moreTrans);
                    if (lookForLayer(thisPoly, (Cell) ni.getProto(), layer, trans, newBounds,
                        pt1, pt2, pt3, pointsFound, layerFunction, ignoreCenterCuts))
                        return true;
                    continue;
                }
                AffineTransform bound = ni.rotateOut();
                bound.preConcatenate(moreTrans);
                Technology tech = ni.getProto().getTechnology();
                Poly[] layerLookPolyList = tech.getShapeOfNode(ni, false, ignoreCenterCuts, layerFunction); // consistent change!
//                layerLookPolyList = tech.getShapeOfNode(ni, false, ignoreCenterCuts, null);
                int tot = layerLookPolyList.length;
                for (int i = 0; i < tot; i++)
                {
                    Poly poly = layerLookPolyList[i];
                    // sameLayer test required to check if Active layer is not identical to thich actice layer
                    if (!tech.sameLayer(poly.getLayer(), layer))
                    {
                        continue;
                    }

                    if (thisPoly != null && poly.polySame(thisPoly)) continue;
                    poly.transform(bound);
                    if (poly.isInside(pt1)) pointsFound[0] = true;
                    if (poly.isInside(pt2)) pointsFound[1] = true;
                    if (pt3 != null && poly.isInside(pt3)) pointsFound[2] = true;
                    for (j = 0; j < pointsFound.length && pointsFound[j]; j++) ;
                    boolean newR = (j == pointsFound.length);
                    if (newR)
                    {
                        return true;
                    }
                    // No need of checking rest of the layers?
                    //break;
                }
            } else
            {
                ArcInst ai = (ArcInst) g;
                Technology tech = ai.getProto().getTechnology();
                Poly[] layerLookPolyList = tech.getShapeOfArc(ai, layerFunction); // consistent change!);
                int tot = layerLookPolyList.length;
                for (int i = 0; i < tot; i++)
                {
                    Poly poly = layerLookPolyList[i];
                    // sameLayer test required to check if Active layer is not identical to thich actice layer
                    if (!tech.sameLayer(poly.getLayer(), layer))
                    {
                        continue;
                    }

                    poly.transform(moreTrans);
                    if (poly.isInside(pt1)) pointsFound[0] = true;
                    if (poly.isInside(pt2)) pointsFound[1] = true;
                    if (pt3 != null && poly.isInside(pt3)) pointsFound[2] = true;
                    for (j = 0; j < pointsFound.length && pointsFound[j]; j++) ;
                    boolean newR = (j == pointsFound.length);
                    if (newR)
                        return true;
                    // No need of checking rest of the layers
                    //break;
                }
            }

            for (j = 0; j < pointsFound.length && pointsFound[j]; j++) ;
            if (j == pointsFound.length)
            {
                System.out.println("When?");
                return true;
            }
        }
        if (skip) System.out.println("This case in lookForLayerNew antes");

        return false;
    }

    static boolean checkMinWidthInternal(Geometric geom, Layer layer, Poly poly, boolean onlyOne,
                                          DRCTemplate minWidthRule, boolean reportError,
                                          Layer.Function.Set layerFunction, ReportInfo reportInfo)
    {
        Cell cell = geom.getParent();
        if (minWidthRule == null) return false;

        double minWidthValue = minWidthRule.getValue(0);
        // simpler analysis if manhattan
        Rectangle2D bounds = poly.getBox();

        // only in case of flat elements represented by a line
        // most likely an flat arc, vertical or horizontal.
        // It doesn't consider arbitrary angled lines.
        // If bounds is null, it might have area if it is non-manhattan
        boolean flatPoly = ((bounds == null && GenMath.doublesEqual(poly.getArea(), 0)));
        if (flatPoly)
        {
            Point2D [] points = poly.getPoints();
            Point2D from = points[0];
            Point2D to = points[1];

            // Assuming it is a single segment the flat region
            // looking for two distinct points
            if (DBMath.areEquals(from, to))
            {
                boolean found = false;
                for (int i = 2; i < points.length; i++)
                {
                    if (!DBMath.areEquals(from, points[i]))
                    {
                        to = points[i];
                        found = true;
                        break;
                    }
                }
                if (!found) // single segment where to == from
                {
                    return false; // skipping this case.
                }
            }

            Point2D center = new Point2D.Double((from.getX() + to.getX()) / 2, (from.getY() + to.getY()) / 2);

            // looking if points around the overlapping area are inside another region
            // to avoid the error
            boolean [] pointsFound = new boolean[3];
            pointsFound[0] = pointsFound[1] = pointsFound[2] = false;
            boolean found = lookForLayerCoverage(geom, poly, null, null, cell, layer, DBMath.MATID,  poly.getBounds2D(),
                from, to, center, pointsFound, true, null, true, reportInfo.ignoreCenterCuts);
            if (found) return false; // no error, flat element covered by othe elements.

            if (reportError)
                createDRCErrorLogger(reportInfo, DRCErrorType.MINWIDTHERROR, null, cell, minWidthValue, 0, minWidthRule.ruleName,
                    (onlyOne) ? null : poly, geom, layer, null, null, null);
            return true;
        }

        if (bounds != null)
        {
            boolean tooSmallWidth = DBMath.isGreaterThan(minWidthValue, bounds.getWidth());
            boolean tooSmallHeight = DBMath.isGreaterThan(minWidthValue, bounds.getHeight());
            if (!tooSmallWidth && !tooSmallHeight) return false;

            boolean foundError = false;
            if (tooSmallWidth && checkExtensionWithNeighbors(cell, geom, poly, layer, bounds, minWidthRule,
                0, onlyOne, reportError, layerFunction, reportInfo))
                foundError = true;
            if (tooSmallHeight && checkExtensionWithNeighbors(cell, geom, poly, layer, bounds, minWidthRule,
                1, onlyOne, reportError, layerFunction, reportInfo))
                foundError = true;
            return foundError;
        }

        // nonmanhattan polygon: stop now if it has no size
        Poly.Type style = poly.getStyle();
        if (style != Poly.Type.FILLED && style != Poly.Type.CLOSED && style != Poly.Type.CROSSED &&
            style != Poly.Type.OPENED && style != Poly.Type.OPENEDT1 && style != Poly.Type.OPENEDT2 &&
            style != Poly.Type.OPENEDT3 && style != Poly.Type.VECTORS) return false;

        // simple check of nonmanhattan polygon for minimum width
        bounds = poly.getBounds2D();
        double actual = Math.min(bounds.getWidth(), bounds.getHeight());
        if (actual < minWidthValue)
        {
            if (reportError)
                createDRCErrorLogger(reportInfo, DRCErrorType.MINWIDTHERROR, null, cell, minWidthValue, actual, minWidthRule.ruleName,
                    (onlyOne) ? null : poly, geom, layer, null, null, null);
            return true;
        }

        // check distance of each line's midpoint to perpendicular opposite point
        Point2D[] points = poly.getPoints();
        int count = points.length;
        for (int i = 0; i < count; i++)
        {
            Point2D from;
            if (i == 0) from = points[count - 1];
            else
                from = points[i - 1];
            Point2D to = points[i];
            if (from.equals(to)) continue;

            double ang = DBMath.figureAngleRadians(from, to);
            Point2D center = new Point2D.Double((from.getX() + to.getX()) / 2, (from.getY() + to.getY()) / 2);
            double perpang = ang + Math.PI / 2;
            for (int j = 0; j < count; j++)
            {
                if (j == i) continue;
                Point2D oFrom;
                if (j == 0) oFrom = points[count - 1];
                else
                    oFrom = points[j - 1];
                Point2D oTo = points[j];
                if (oFrom.equals(oTo)) continue;
                double oAng = DBMath.figureAngleRadians(oFrom, oTo);
                double rAng = ang;
                while (rAng > Math.PI) rAng -= Math.PI;
                double rOAng = oAng;
                while (rOAng > Math.PI) rOAng -= Math.PI;
                if (DBMath.doublesEqual(rAng, rOAng))
                {
                    // lines are parallel: see if they are colinear
                    if (DBMath.isOnLine(from, to, oFrom)) continue;
                    if (DBMath.isOnLine(from, to, oTo)) continue;
                    if (DBMath.isOnLine(oFrom, oTo, from)) continue;
                    if (DBMath.isOnLine(oFrom, oTo, to)) continue;
                }
                Point2D inter = DBMath.intersectRadians(center, perpang, oFrom, oAng);
                if (inter == null) continue;
                if (inter.getX() < Math.min(oFrom.getX(), oTo.getX()) || inter.getX() > Math.max(oFrom.getX(), oTo.getX()))
                    continue;
                if (inter.getY() < Math.min(oFrom.getY(), oTo.getY()) || inter.getY() > Math.max(oFrom.getY(), oTo.getY()))
                    continue;
                double fdx = center.getX() - inter.getX();
                double fdy = center.getY() - inter.getY();
                actual = DBMath.round(Math.sqrt(fdx * fdx + fdy * fdy));

                if (actual < minWidthValue)
                {
                    if (reportError)
                    {
                        // look between the points to see if it is minimum width or notch
                        if (poly.isInside(new Point2D.Double((center.getX() + inter.getX()) / 2, (center.getY() + inter.getY()) / 2)))
                        {
                            createDRCErrorLogger(reportInfo, DRCErrorType.MINWIDTHERROR, null, cell, minWidthValue,
                                actual, minWidthRule.ruleName, (onlyOne) ? null : poly, geom, layer, null, null, null);
                        } else
                        {
                            createDRCErrorLogger(reportInfo, DRCErrorType.NOTCHERROR, null, cell, minWidthValue,
                                actual, minWidthRule.ruleName, (onlyOne) ? null : poly, geom, layer, poly, geom, layer);
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /*********************************** QUICK DRC ERROR REPORTING ***********************************/
    public static enum DRCErrorType
    {
	    // the different types of errors
        SPACINGERROR, MINWIDTHERROR, NOTCHERROR, MINSIZEERROR, BADLAYERERROR, LAYERSURROUNDERROR,
        MINAREAERROR, ENCLOSEDAREAERROR, SURROUNDERROR, FORBIDDEN, RESOLUTION, CUTERROR, SLOTSIZEERROR,
        CROOKEDERROR,
        // Different types of warnings
        ZEROLENGTHARCWARN, TECHMIXWARN
    }

    public static class ReportInfo
    {

        /** error type search */				                    DRC.DRCCheckMode errorTypeSearch;
        /** minimum output grid resolution */				        double minAllowedResolution;
        /** true to ignore center cuts in large contacts. */		boolean ignoreCenterCuts = DRC.isIgnoreCenterCuts();;
        /** maximum area to examine (the worst spacing rule). */	double worstInteractionDistance;
        /** time stamp for numbering networks. */					int checkTimeStamp;
        /** for numbering networks. */								int checkNetNumber;
        /** total errors found in all threads. */					int totalSpacingMsgFound;
        /** for logging errors */                                   ErrorLogger errorLogger;
        /** for interactive error logging */                        boolean interactiveLogger = DRC.isInteractiveLoggingOn();
        /** to cache current extra bits */                          int activeSpacingBits = 0;   
        Map<Cell, Area> exclusionMap = new HashMap<Cell,Area>(); // The DRCExclusion object lists areas where Generic:DRC-Nodes exist to ignore errors.
        boolean inMemory = DRC.isDatesStoredInMemory();

        public ReportInfo(ErrorLogger eL, Technology tech, boolean specificGeoms)
        {
            errorLogger = eL;
            activeSpacingBits = DRC.getActiveBits(tech);
            worstInteractionDistance = DRC.getWorstSpacingDistance(tech, -1);
            // minimim resolution different from zero if flag is on otherwise stays at zero (default)
            minAllowedResolution = tech.getResolution();

            errorTypeSearch = DRC.getErrorType();
            if (specificGeoms)
            {
                errorTypeSearch = DRC.DRCCheckMode.ERROR_CHECK_CELL;
            }
        }
    }

    public static void createDRCErrorLogger(ReportInfo reportInfo,
                                            DRCErrorType errorType, String msg,
                                            Cell cell, double limit, double actual, String rule,
                                            PolyBase poly1, Geometric geom1, Layer layer1,
                                            PolyBase poly2, Geometric geom2, Layer layer2)
    {
        ErrorLogger errorLogger = reportInfo.errorLogger;

        if (errorLogger == null) return;

		// if this error is in an ignored area, don't record it
		StringBuffer DRCexclusionMsg = new StringBuffer();
        if (reportInfo.exclusionMap != null && reportInfo.exclusionMap.get(cell) != null)
		{
			// determine the bounding box of the error
			List<PolyBase> polyList = new ArrayList<PolyBase>(2);
			List<Geometric> geomList = new ArrayList<Geometric>(2);
			polyList.add(poly1); geomList.add(geom1);
			if (poly2 != null)
			{
				polyList.add(poly2);
				geomList.add(geom2);
			}
            boolean found = checkExclusionMap(reportInfo.exclusionMap, cell, polyList, geomList, DRCexclusionMsg);

            // At least one DRC exclusion that contains both
            if (found) return;
		}

		// describe the error
		Cell np1 = (geom1 != null) ? geom1.getParent() : null;
		Cell np2 = (geom2 != null) ? geom2.getParent() : null;

		// Message already logged
        boolean onlyWarning = (errorType == DRCErrorType.ZEROLENGTHARCWARN || errorType == DRCErrorType.TECHMIXWARN);
        // Until a decent algorithm is in place for detecting repeated errors, ERROR_CHECK_EXHAUSTIVE might report duplicate errros
		if ( geom2 != null && reportInfo.errorTypeSearch != DRCCheckMode.ERROR_CHECK_EXHAUSTIVE &&
            errorLogger.findMessage(cell, geom1, geom2.getParent(), geom2, !onlyWarning))
            return;

		StringBuffer errorMessage = new StringBuffer();
        DRCCheckLogging loggingType = getErrorLoggingType();

        int sortKey = cell.hashCode(); // 0;
		if (errorType == DRCErrorType.SPACINGERROR || errorType == DRCErrorType.NOTCHERROR || errorType == DRCErrorType.SURROUNDERROR)
		{
			// describe spacing width error
			if (errorType == DRCErrorType.SPACINGERROR)
				errorMessage.append("Spacing");
			else if (errorType == DRCErrorType.SURROUNDERROR)
				errorMessage.append("Surround");
			else
				errorMessage.append("Notch");
			if (layer1 == layer2)
				errorMessage.append(" (layer '" + layer1.getName() + "')");
			errorMessage.append(": ");

			if (np1 != np2)
			{
				errorMessage.append(np1 + ", ");
			} else if (np1 != cell && np1 != null)
			{
				errorMessage.append("[in " + np1 + "] ");
			}

            if (geom1 != null)
                errorMessage.append(geom1);
			if (layer1 != layer2)
				errorMessage.append(", layer '" + layer1.getName() + "'");

			if (actual < 0) errorMessage.append(" OVERLAPS (BY " + TextUtils.formatDouble(limit-actual) + ") ");
			else if (actual == 0) errorMessage.append(" TOUCHES ");
			else errorMessage.append(" LESS (BY " + TextUtils.formatDouble(limit-actual) + ") THAN " + TextUtils.formatDouble(limit) +
                    ((geom2!=null)?" TO ":""));

			if (np1 != np2 && np2 != null)
				errorMessage.append(np2 + ", ");

            if (geom2 != null)
                errorMessage.append(geom2);
			if (layer1 != layer2)
				errorMessage.append(", layer '" + layer2.getName() + "'");
			if (msg != null)
				errorMessage.append("; " + msg);
		} else
		{
			// describe minimum width/size or layer error
			StringBuffer errorMessagePart2 = null;
			switch (errorType)
			{
                case CROOKEDERROR:
                    errorMessage.append("Crooked error:");
					errorMessagePart2 = new StringBuffer(" is not horizontal nor vertical");
                    break;
                case RESOLUTION:
                    errorMessage.append("Resolution error:");
					errorMessagePart2 = new StringBuffer(msg);
                    break;
                case FORBIDDEN:
                    errorMessage.append("Forbidden error:");
					errorMessagePart2 = new StringBuffer(msg);
                    break;
                case SLOTSIZEERROR:
                    errorMessage.append("Slot size error:");
					errorMessagePart2 = new StringBuffer(", layer '" + layer1.getName() + "'");
					errorMessagePart2.append(" BIGGER THAN " + TextUtils.formatDouble(limit) + " IN LENGTH (IS " + TextUtils.formatDouble(actual) + ")");
                    break;
				case MINAREAERROR:
					errorMessage.append("Minimum area error:");
					errorMessagePart2 = new StringBuffer(", layer '" + layer1.getName() + "'");
					errorMessagePart2.append(" LESS THAN " + TextUtils.formatDouble(limit) + " IN AREA (IS " + TextUtils.formatDouble(actual) + ")");
					break;
				case ENCLOSEDAREAERROR:
					errorMessage.append("Enclosed area error:");
					errorMessagePart2 = new StringBuffer(", layer '" + layer1.getName() + "'");
					errorMessagePart2.append(" LESS THAN " + TextUtils.formatDouble(limit) + " IN AREA (IS " + TextUtils.formatDouble(actual) + ")");
					break;
				case TECHMIXWARN:
					errorMessage.append("Technology mixture warning:");
					errorMessagePart2 = new StringBuffer(msg);
					break;
				case ZEROLENGTHARCWARN:
					errorMessage.append("Zero width warning:");
					errorMessagePart2 = new StringBuffer(msg); break;
				case CUTERROR:
                    errorMessage.append("Maximum cut error" + ((msg != null) ? ("(" + msg + "):") : ""));
                    errorMessagePart2 = new StringBuffer(", layer '" + layer1.getName() + "'");
                    errorMessagePart2.append(" BIGGER THAN " + TextUtils.formatDouble(limit) + " WIDE (IS " + TextUtils.formatDouble(actual) + ")");
					break;
				case MINWIDTHERROR:
                    errorMessage.append("Minimum width/height error" + ((msg != null) ? ("(" + msg + "):") : ""));
					errorMessagePart2 = new StringBuffer(", layer '" + layer1.getName() + "'");
					errorMessagePart2.append(" LESS THAN " + TextUtils.formatDouble(limit) + " WIDE (IS " + TextUtils.formatDouble(actual) + ")");
                    break;
				case MINSIZEERROR:
					errorMessage.append("Minimum size error on " + msg + ":");
					errorMessagePart2 = new StringBuffer(" LESS THAN " + TextUtils.formatDouble(limit) + " IN SIZE (IS " + TextUtils.formatDouble(actual) + ")");
					break;
				case BADLAYERERROR:
					errorMessage.append("Invalid layer ('" + layer1.getName() + "'):");
					break;
				case LAYERSURROUNDERROR:
					errorMessage.append("Layer surround error: " + msg);
					errorMessagePart2 = new StringBuffer(", layer '" + layer1.getName() + "'");
                    String layerName = (layer2 != null) ? layer2.getName() : "Select";
					errorMessagePart2.append(" NEEDS SURROUND OF LAYER '" + layerName + "' BY " + limit);
                    break;
                default:
                    assert(false); // it should not happen
            }

			errorMessage.append(" " + cell + " ");
			if (geom1 != null)
			{
				errorMessage.append(geom1);
			}
            // only when is flat -> use layer index for sorting
            if (layer1 != null && loggingType == DRCCheckLogging.DRC_LOG_FLAT) sortKey = layer1.getIndex();
			errorMessage.append(errorMessagePart2);
		}
		if (rule != null && rule.length() > 0) errorMessage.append(" [rule '" + rule + "']");
		errorMessage.append(DRCexclusionMsg);

		List<Geometric> geomList = new ArrayList<Geometric>();
		List<PolyBase> polyList = new ArrayList<PolyBase>();
		if (poly1 != null) polyList.add(poly1); else
			if (geom1 != null) geomList.add(geom1);
		if (poly2 != null) polyList.add(poly2); else
			if (geom2 != null) geomList.add(geom2);

        switch (loggingType)
        {
            case DRC_LOG_PER_CELL:
                errorLogger.setGroupName(sortKey, cell.getName());
                break;
            case DRC_LOG_PER_RULE:
                String theRuleName = rule;
                if (theRuleName == null)
                    theRuleName = errorType.name();
                sortKey = theRuleName.hashCode();
                if (errorLogger.getGroupName(sortKey) == null) // only if nothing was found
                    errorLogger.setGroupName(sortKey, theRuleName);
                break;
        }

        if (onlyWarning)
            errorLogger.logWarning(errorMessage.toString(), geomList, null, null, null, polyList, cell, sortKey);
        else
		    errorLogger.logError(errorMessage.toString(), geomList, null, null, null, polyList, cell, sortKey);
        // Temporary display of errors.
        if (reportInfo.interactiveLogger)
            Job.getUserInterface().termLogging(errorLogger, false, false);
	}

    private static boolean checkExclusionMap(Map<Cell, Area> exclusionMap, Cell cell, List<PolyBase> polyList,
                                             List<Geometric> geomList, StringBuffer DRCexclusionMsg) {
        Area area = exclusionMap.get(cell);
        if (area == null) return false;

        int count = 0, i = -1;

        for (PolyBase thisPoly : polyList) {
            i++;
            if (thisPoly == null)
                continue; // MinNode case
            boolean found = area.contains(thisPoly.getBounds2D());

            if (found) count++;
            else {
                Rectangle2D rect = (geomList.get(i) != null) ? geomList.get(i).getBounds() : thisPoly.getBounds2D();
                DRCexclusionMsg.append("\n\t(DRC Exclusion in '" + cell.getName() + "' does not completely contain element (" +
                        rect.getMinX() + "," + rect.getMinY() + ") (" + rect.getMaxX() + "," + rect.getMaxY() + "))");
            }
        }
// At least one DRC exclusion that contains both
//        if (count == polyList.size())
        if (count >= 1) // at one element is inside the DRC exclusion
            return true;
        return false;
    }

    /*********************************** END DRC ERROR REPORTING ***********************************/

    private static class StoreDRCInfo
    {
        long date;
        int bits;
        StoreDRCInfo(long d, int b)
        {
            date = d;
            bits = b;
        }
    }

    private static boolean incrementalRunning = false;
    /** key of Variable for last valid DRC date on a Cell. Only area rules */
//    private static final int DRC_BIT_AREA = 01; /* Min area condition */
    private static final int DRC_BIT_EXTENSION = 02;   /* Coverage DRC condition */
    private static final int DRC_BIT_ST_FOUNDRY = 04; /* For ST foundry selection */
    private static final int DRC_BIT_TSMC_FOUNDRY = 010; /* For TSMC foundry selection */
    private static final int DRC_BIT_MOSIS_FOUNDRY = 020; /* For Mosis foundry selection */

    public enum DRCCheckMinArea
    {
        AREA_BASIC("Simple") /*brute force algorithm*/, AREA_LOCAL("Local");
        private final String name;
        DRCCheckMinArea(String s)
        {
            name = s;
        }
        public String toString() {return name;}
    }

    public enum DRCCheckLogging
    {
        DRC_LOG_FLAT("Flat")/*original strategy*/, DRC_LOG_PER_CELL("By Cell"), DRC_LOG_PER_RULE("By Rule");
        private final String name;
        DRCCheckLogging(String s)
        {
            name = s;
        }
        public String toString() {return name;}
    }

    /** Control different level of error checking */
    public enum DRCCheckMode
    {
	    ERROR_CHECK_DEFAULT (0),    /** DRC stops after first error between 2 nodes is found (default) */
        ERROR_CHECK_CELL (1),       /** DRC stops after first error per cell is found */
        ERROR_CHECK_EXHAUSTIVE (2);  /** DRC checks all combinations */
        private final int mode;   // mode
        DRCCheckMode(int m) {this.mode = m;}
        public int mode() { return this.mode; }
        public String toString() {return name();}
    }

    /****************************** TOOL CONTROL ******************************/

	/**
	 * The constructor sets up the DRC tool.
	 */
	private DRC()
	{
		super("drc");
	}

    /**
	 * Method to initialize the DRC tool.
	 */
	public void init()
	{
		setOn();
	}

    /**
     * Method to retrieve the singleton associated with the DRC tool.
     * @return the DRC tool.
     */
    public static DRC getDRCTool() { return tool; }

	private static void includeGeometric(Geometric geom)
	{
		if (!isIncrementalDRCOn()) return;
        Cell cell = geom.getParent();

        synchronized (cellsToCheck)
		{
			Set<Geometric> cellSet = cellsToCheck.get(cell);
			if (cellSet == null)
			{
				cellSet = new HashSet<Geometric>();
				cellsToCheck.put(cell, cellSet);
			}
			cellSet.add(geom);
		}
    }

//	private static void removeGeometric(Geometric geom)
//	{
//		if (!isIncrementalDRCOn()) return;
//		Cell cell = geom.getParent();
//		synchronized (cellsToCheck)
//		{
//			HashSet<Geometric> cellSet = cellsToCheck.get(cell);
//			if (cellSet != null) cellSet.remove(geom);
//		}
//	}

	private static void doIncrementalDRCTask()
	{
		if (!isIncrementalDRCOn()) return;
		if (incrementalRunning) return;

		Library curLib = Library.getCurrent();
		if (curLib == null) return;
		Cell cellToCheck = Job.getUserInterface().getCurrentCell(curLib);
		Set<Geometric> cellSet = null;

		// get a cell to check
		synchronized (cellsToCheck)
		{
			if (cellToCheck != null)
				cellSet = cellsToCheck.get(cellToCheck);
			if (cellSet == null && cellsToCheck.size() > 0)
			{
				cellToCheck = cellsToCheck.keySet().iterator().next();
				cellSet = cellsToCheck.get(cellToCheck);
			}
			if (cellSet != null)
				cellsToCheck.remove(cellToCheck);
		}

		if (cellToCheck == null) return; // nothing to do

		// don't check if cell not in database anymore
		if (!cellToCheck.isLinked()) return;
		// Handling clipboard case (one type of hidden libraries)
		if (cellToCheck.getLibrary().isHidden()) return;

		// if there is a cell to check, do it
		if (cellSet != null)
		{
			Geometric [] objectsToCheck = new Geometric[cellSet.size()];
			int i = 0;
            for(Geometric geom : cellSet)
				objectsToCheck[i++] = geom;

            // cleaning previous errors on the cells to check now.
            for (Geometric geo : objectsToCheck)
            {
                Cell c = geo.getParent();
                ArrayList<ErrorLogger.MessageLog> getAllLogs = errorLoggerIncremental.getAllLogs(c);
                Job.updateIncrementalDRCErrors(c, null, getAllLogs);
            }
            new CheckDRCIncrementally(cellToCheck, objectsToCheck, cellToCheck.getTechnology().isScaleRelevant());
		}
	}

   /**
     * Handles database changes of a Job.
     * @param oldSnapshot database snapshot before Job.
     * @param newSnapshot database snapshot after Job and constraint propagation.
     * @param undoRedo true if Job was Undo/Redo job.
     */
    public void endBatch(Snapshot oldSnapshot, Snapshot newSnapshot, boolean undoRedo)
	{
        for (CellId cellId: newSnapshot.getChangedCells(oldSnapshot)) {
            Cell cell = Cell.inCurrentThread(cellId);
            if (cell == null) continue;
            CellBackup oldBackup = oldSnapshot.getCell(cellId);
            for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
                NodeInst ni = it.next();
                ImmutableNodeInst d = ni.getD();
                if (oldBackup == null || oldBackup.cellRevision.getNode(d.nodeId) != d)
                    includeGeometric(ni);
            }
            for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); ) {
                ArcInst ai = it.next();
                ImmutableArcInst d = ai.getD();
                if (oldBackup == null || oldBackup.cellRevision.getArc(d.arcId) != d)
                    includeGeometric(ai);
            }
        }
		doIncrementalDRCTask();
	}

//	/**
//	 * Method to announce a change to a NodeInst.
//	 * @param ni the NodeInst that was changed.
//	 * @param oD the old contents of the NodeInst.
//	 */
//	public void modifyNodeInst(NodeInst ni, ImmutableNodeInst oD)
//	{
//		includeGeometric(ni);
//	}
//
//	/**
//	 * Method to announce a change to an ArcInst.
//	 * @param ai the ArcInst that changed.
//     * @param oD the old contents of the ArcInst.
//	 */
//	public void modifyArcInst(ArcInst ai, ImmutableArcInst oD)
//	{
//		includeGeometric(ai);
//	}
//
//	/**
//	 * Method to announce the creation of a new ElectricObject.
//	 * @param obj the ElectricObject that was just created.
//	 */
//	public void newObject(ElectricObject obj)
//	{
//		if (obj instanceof Geometric)
//		{
//			includeGeometric((Geometric)obj);
//		}
//	}
//
//	/**
//	 * Method to announce the deletion of an ElectricObject.
//	 * @param obj the ElectricObject that was just deleted.
//	 */
//	public void killObject(ElectricObject obj)
//	{
//		if (obj instanceof Geometric)
//		{
//			removeGeometric((Geometric)obj);
//		}
//	}

	/****************************** DRC INTERFACE ******************************/
    public static ErrorLogger getDRCErrorLogger(boolean layout, boolean incremental, String extraMsg)
    {
        ErrorLogger errorLogger = null;
        String title = (layout) ? "Layout " : "Schematic ";

        if (incremental)
        {
//            if (errorLoggerIncremental == null) errorLoggerIncremental = ErrorLogger.newInstance("DRC (incremental)"/*, true*/);
            errorLogger = errorLoggerIncremental;
        }
        else
        {
            errorLogger = ErrorLogger.newInstance(title + "DRC (full)" + ((extraMsg != null) ? extraMsg:""));
        }
        return errorLogger;
    }

    public static ErrorLogger getDRCIncrementalLogger() {return errorLoggerIncremental;}

    /**
     * This method generates a DRC job from the GUI or for a bash script.
     */
    public static void checkDRCHierarchically(Cell cell, List<Geometric> objs, Rectangle2D bounds,
                                              GeometryHandler.GHMode mode, boolean onlyArea)
    {
        if (cell == null) return;
        boolean isLayout = true; // hierarchical check of layout by default
		if (cell.isSchematic() || cell.getTechnology() == Schematics.tech() ||
			cell.isIcon() || cell.getTechnology() == Artwork.tech())
			// hierarchical check of schematics
			isLayout = false;

        if (mode == null) mode = GeometryHandler.GHMode.ALGO_SWEEP;
        new CheckDRCHierarchically(cell, isLayout, objs, bounds, mode, onlyArea);
    }

	/**
	 * Base class for checking design rules.
	 *
	 */
	public static class CheckDRCJob extends Job
	{
		Cell cell;
        boolean isLayout; // to check layout

        private static String getJobName(Cell cell) { return "Design-Rule Check " + cell; }
		protected CheckDRCJob(Cell cell, Listener tool, Priority priority, boolean layout)
		{
			super(getJobName(cell), tool, Job.Type.EXAMINE, null, null, priority);
			this.cell = cell;
            this.isLayout = layout;

		}
		// never used
		public boolean doIt() { return (false);}
	}

    /**
     * Class for hierarchical DRC for layout and schematics
     */
	private static class CheckDRCHierarchically extends CheckDRCJob
	{
		Rectangle2D bounds;
        private GeometryHandler.GHMode mergeMode; // to select the merge algorithm
        private boolean onlyArea;
        private Geometric[] geoms;

        /**
         * Check bounds within cell. If bounds is null, check entire cell.
         * @param cell
         * @param layout
         * @param bounds
         */
		protected CheckDRCHierarchically(Cell cell, boolean layout, List<Geometric> objs,
                                         Rectangle2D bounds, GeometryHandler.GHMode mode,
                                         boolean onlyA)
		{
			super(cell, tool, Job.Priority.USER, layout);
			this.bounds = bounds;
            this.mergeMode = mode;
            this.onlyArea = onlyA;
            if (objs != null)
            {
                this.geoms = new Geometric[objs.size()];
                objs.toArray(this.geoms);
            }
            startJob();
		}

		public boolean doIt()
		{
			long startTime = System.currentTimeMillis();
            ErrorLogger errorLog = getDRCErrorLogger(isLayout, false, null);
            checkNetworks(errorLog, cell, isLayout);
            if (isLayout)
                Quick.checkDesignRules(errorLog, cell, geoms, null, bounds, this, mergeMode, onlyArea);
            else
                Schematic.doCheck(errorLog, cell, geoms);
            long endTime = System.currentTimeMillis();
            int errorCount = errorLog.getNumErrors();
            int warnCount = errorLog.getNumWarnings();
            System.out.println(errorCount + " errors and " + warnCount + " warnings found (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
            if (onlyArea)
                Job.getUserInterface().termLogging(errorLog, false, false); // otherwise the errors don't appear
            return true;
		}
	}

	private static class CheckDRCIncrementally extends CheckDRCJob
	{
		Geometric [] objectsToCheck;

		protected CheckDRCIncrementally(Cell cell, Geometric[] objectsToCheck, boolean layout)
		{
			super(cell, tool, Job.Priority.ANALYSIS, layout);
			this.objectsToCheck = objectsToCheck;
			startJob();
		}

		public boolean doIt()
		{
			incrementalRunning = true;
            ErrorLogger errorLog = getDRCErrorLogger(isLayout, true, null);
            if (isLayout)
                errorLog = Quick.checkDesignRules(errorLog, cell, objectsToCheck, null, null);
            else
                errorLog = Schematic.doCheck(errorLog, cell, objectsToCheck);
            int errorsFound = errorLog.getNumErrors();
			if (errorsFound > 0)
				System.out.println("Incremental DRC found " + errorsFound + " errors/warnings in "+ cell);
			incrementalRunning = false;
			doIncrementalDRCTask();
			return true;
		}
	}

	/****************************** DESIGN RULE CONTROL ******************************/

	/** The Technology whose rules are cached. */		private static Technology currentTechnology = null;

	/**
	 * Method to build a Rules object that contains the current design rules for a Technology.
	 * The DRC dialogs use this to hold the values while editing them.
	 * It also provides a cache for the design rule checker.
	 * @param tech the Technology to examine.
	 * @return a new Rules object with the design rules for the given Technology.
	 */
	public static DRCRules getRules(Technology tech)
	{
        DRCRules currentRules = tech.getCachedRules();
		if (currentRules != null && tech == currentTechnology) return currentRules;

		// constructing design rules: start with factory rules
		currentRules = tech.getFactoryDesignRules();
		if (currentRules != null)
		{
			// add overrides
			StringBuffer override = getDRCOverrides(tech);
			currentRules.applyDRCOverrides(override.toString(), tech);
		}

		// remember technology whose rules are cached
		currentTechnology = tech;
        tech.setCachedRules(currentRules);
		return currentRules;
	}

	/**
	 * Method to load a full set of design rules for a Technology.
	 * @param tech the Technology to load.
	 * @param newRules a complete design rules object.
	 */
	public static void setRules(Technology tech, DRCRules newRules)
	{
		// get factory design rules
		DRCRules factoryRules = tech.getFactoryDesignRules();

		// determine override differences from the factory rules
		StringBuffer changes = Technology.getRuleDifferences(factoryRules, newRules);

        if (Job.LOCALDEBUGFLAG)
            System.out.println("This function needs attention");

		// get current overrides of factory rules
		StringBuffer override = getDRCOverrides(tech);

		// if the differences are the same as before, stop
		if (changes.toString().equals(override.toString())) return;

		// update the preference for the rule overrides
		setDRCOverrides(changes, tech);

		// update variables on the technology
		tech.setRuleVariables(newRules);

		// flush the cache of rules
		if (currentTechnology == tech) currentTechnology = null;
	}

	/****************************** INDIVIDUAL DESIGN RULES ******************************/

	/**
	 * Method to find the worst spacing distance in the design rules.
	 * Finds the largest spacing rule in the Technology.
	 * @param tech the Technology to examine.
     * @param lastMetal
     * @return the largest spacing distance in the Technology. Zero if nothing found
	 */
	public static double getWorstSpacingDistance(Technology tech, int lastMetal)
	{
		DRCRules rules = getRules(tech);
		if (rules == null)
            return 0;
		return (rules.getWorstSpacingDistance(lastMetal));
	}

    /**
	 * Method to find the maximum design-rule distance around a layer.
	 * @param layer the Layer to examine.
	 * @return the maximum design-rule distance around the layer. -1 if nothing found.
	 */
	public static double getMaxSurround(Layer layer, double maxSize)
	{
		Technology tech = layer.getTechnology();
        if (tech == null) return -1; // case when layer is a Graphics
		DRCRules rules = getRules(tech);
		if (rules == null) return -1;

        return (rules.getMaxSurround(layer, maxSize));
	}

	/**
	 * Method to find the edge spacing rule between two layer.
	 * @param layer1 the first layer.
	 * @param layer2 the second layer.
	 * @return the edge rule distance between the layers.
	 * Returns null if there is no edge spacing rule.
	 */
	public static DRCTemplate getEdgeRule(Layer layer1, Layer layer2)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return null;

		return (rules.getEdgeRule(layer1, layer2));
	}

	/**
	 * Method to find the spacing rule between two layer.
	 * @param layer1 the first layer.
     * @param geo1
	 * @param layer2 the second layer.
     * @param geo2
	 * @param connected true to find the distance when the layers are connected.
	 * @param multiCut true to find the distance when this is part of a multicut contact.
     * @param wideS widest polygon
     * @param length length of the intersection
	 * @return the spacing rule between the layers.
	 * Returns null if there is no spacing rule.
	 */
	public static DRCTemplate getSpacingRule(Layer layer1, Geometric geo1, Layer layer2, Geometric geo2,
                                             boolean connected, int multiCut, double wideS, double length)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return null;
        return (rules.getSpacingRule(layer1, geo1, layer2, geo2, connected, multiCut, wideS, length));
	}

    /**
     * Method to find all possible rules of DRCRuleType type associated a layer.
     * @param layer1 the layer whose rules are desired.
     * @return a list of DRCTemplate objects associated with the layer.
     */
    public static List<DRCTemplate> getRules(Layer layer1, DRCTemplate.DRCRuleType type)
    {
        Technology tech = layer1.getTechnology();
        DRCRules rules = getRules(tech);
		if (rules == null)
            return null;
		return (rules.getRules(layer1, type));
    }

    /**
	 * Method to find the extension rule between two layer.
	 * @param layer1 the first layer.
	 * @param layer2 the second layer.
     * @param isGateExtension to decide between the rule EXTENSIONGATE or EXTENSION
	 * @return the extension rule between the layers.
	 * Returns null if there is no extension rule.
	 */
	public static DRCTemplate getExtensionRule(Layer layer1, Layer layer2, boolean isGateExtension)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return null;
        return (rules.getExtensionRule(layer1, layer2, isGateExtension));
	}

	/**
	 * Method to tell whether there are any design rules between two layers.
	 * @param layer1 the first Layer to check.
	 * @param layer2 the second Layer to check.
	 * @return true if there are design rules between the layers.
	 */
	public static boolean isAnySpacingRule(Layer layer1, Layer layer2)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return false;
        return (rules.isAnySpacingRule(layer1, layer2));
	}

	/**
	 * Method to get the minimum <type> rule for a Layer
	 * where <type> is the rule type. E.g. MinWidth or Area
	 * @param layer the Layer to examine.
	 * @param type rule type
	 * @return the minimum width rule for the layer.
	 * Returns null if there is no minimum width rule.
	 */
	public static DRCTemplate getMinValue(Layer layer, DRCTemplate.DRCRuleType type)
	{
		Technology tech = layer.getTechnology();
		if (tech == null) return null;
		DRCRules rules = getRules(tech);
		if (rules == null) return null;
        return (rules.getMinValue(layer, type));
	}

    /**
     * Determine if node represented by index in DRC mapping table is forbidden under
     * this foundry.
     */
    public static boolean isForbiddenNode(int index1, int index2, DRCTemplate.DRCRuleType type, Technology tech)
    {
        DRCRules rules = getRules(tech);
        if (rules == null) return false;
        return isForbiddenNode(rules, index1, index2, type);
    }

    public static boolean isForbiddenNode(DRCRules rules, int index1, int index2, DRCTemplate.DRCRuleType type)
    {
        int index = index1; // In case of primitive nodes
        if (index2 != -1 )
            index = rules.getRuleIndex(index1, index2);
        else
            index += rules.getTechnology().getNumLayers(); // Node forbidden
        return (rules.isForbiddenNode(index, type));
    }

    /**
	 * Method to get the minimum size rule for a NodeProto.
	 * @param np the NodeProto to examine.
	 * @return the minimum size rule for the NodeProto.
	 * Returns null if there is no minimum size rule.
	 */
	public static PrimitiveNode.NodeSizeRule getMinSize(NodeProto np)
	{
		if (np instanceof Cell) return null;
		PrimitiveNode pnp = (PrimitiveNode)np;
        return pnp.getMinSizeRule();
	}

	/****************************** SUPPORT FOR DESIGN RULES ******************************/

	/**
	 * Method to get the DRC overrides from the preferences for a given technology.
	 * @param tech the Technology on which to get overrides.
	 * @return a Pref describing DRC overrides for the Technology.
	 */
	private static StringBuffer getDRCOverrides(Technology tech)
	{
		Pref pref = prefDRCOverride.get(tech);
		if (pref == null)
		{
			pref = Pref.makeStringPref("DRCOverridesFor" + tech.getTechName(), tool.prefs, "");
			prefDRCOverride.put(tech, pref);
		}
		StringBuffer sb = new StringBuffer();
		sb.append(pref.getString());
		return sb;
	}

	/**
	 * Method to set the DRC overrides for a given technology.
	 * @param sb the overrides (a StringBuffer).
	 * @param tech the Technology on which to get overrides.
	 */
	private static void setDRCOverrides(StringBuffer sb, Technology tech)
	{
		if (sb.length() >= Preferences.MAX_VALUE_LENGTH)
		{
			System.out.println("Warning: Design rule overrides are too complex to be saved (are " +
				sb.length() + " long which is more than the limit of " + Preferences.MAX_VALUE_LENGTH + ")");
		}
		Pref pref = prefDRCOverride.get(tech);
		if (pref == null)
		{
			pref = Pref.makeStringPref("DRCOverridesFor" + tech.getTechName(), tool.prefs, "");
			prefDRCOverride.put(tech, pref);
		}
		pref.setString(sb.toString());
	}

    /**
     * Method to clean those cells that were marked with a valid date due to
     * changes in the DRC rules.
     * @param f
     */
    public static void cleanCellsDueToFoundryChanges(Technology tech, Foundry f)
    {
        // Need to clean cells using this foundry because the rules might have changed.
        System.out.println("Cleaning good DRC dates in cells using '" + f.getType().getName() +
                "' in '" + tech.getTechName() + "'");
        Set<Cell> cleanSpacingDRCDate = new HashSet<Cell>();
        Set<Cell> cleanAreaDRCDate = new HashSet<Cell>();

        int bit = f.getType().getBit();
//        switch(f.getType())
//        {
//            case MOSIS:
//                bit = DRC_BIT_MOSIS_FOUNDRY;
//                break;
//            case TSMC:
//                bit = DRC_BIT_TSMC_FOUNDRY;
//                break;
//            case ST:
//                bit = DRC_BIT_ST_FOUNDRY;
//                break;
//        }

        boolean inMemory = isDatesStoredInMemory();

        for (Iterator<Library> it = Library.getLibraries(); it.hasNext();)
        {
            Library lib = it.next();
            for (Iterator<Cell> itC = lib.getCells(); itC.hasNext();)
            {
                Cell cell = itC.next();
                if (cell.getTechnology() != tech) continue;

                StoreDRCInfo data = getCellGoodDRCDateAndBits(cell, true, !inMemory);

                if (data != null) // there is data
                {
                    // It was marked as valid with previous set of rules
                    if ((data.bits & bit) != 0)
                        cleanSpacingDRCDate.add(cell);
                }

                // Checking area bit
                data = getCellGoodDRCDateAndBits(cell, false, !inMemory);

                if (data != null) // there is data
                {
                    // It was marked as valid with previous set of rules
                    if ((data.bits & bit) != 0)
                        cleanAreaDRCDate.add(cell);
                }
            }
        }
        addDRCUpdate(0, null, cleanSpacingDRCDate, null, cleanAreaDRCDate, null);
    }

    /**
     * Method to retrieve Date in miliseconds if a valid date for the given Key is found
     * @param cell
     * @param key
     * @return true if a valid date is found
     */
    private static boolean getDateStored(Cell cell, Variable.Key key, GenMath.MutableLong date)
    {
        long lastDRCDateInMilliseconds;
        // disk version
        Long lastDRCDateAsLong = cell.getVarValue(key, Long.class); // new strategy
        if (lastDRCDateAsLong != null) {
            lastDRCDateInMilliseconds = lastDRCDateAsLong.longValue();
        } else {
            Integer[] lastDRCDateAsInts = cell.getVarValue(key, Integer[].class);
            if (lastDRCDateAsInts == null) return false;
            long lastDRCDateInSecondsHigh = lastDRCDateAsInts[0].intValue();
            long lastDRCDateInSecondsLow = lastDRCDateAsInts[1].intValue();
            lastDRCDateInMilliseconds = (lastDRCDateInSecondsHigh << 32) | (lastDRCDateInSecondsLow & 0xFFFFFFFFL);
        }
        date.setValue(lastDRCDateInMilliseconds);
        return true;
    }

    /**
     * Method to extract the corresponding DRC bits stored in disk(database) or
     * in memory for a given cell for further analysis
     * @param cell
     * @param fromDisk
     * @return temporary class containing date and bits if available. Null if nothing is found
     */
    private static StoreDRCInfo getCellGoodDRCDateAndBits(Cell cell, boolean spacingCheck, boolean fromDisk)
    {
        Map<Cell,StoreDRCInfo> storedDRCDate = storedSpacingDRCDate;
        Variable.Key dateKey = Layout.DRC_LAST_GOOD_DATE_SPACING;
        Variable.Key bitKey = Layout.DRC_LAST_GOOD_BIT_SPACING;

        if (!spacingCheck)
        {
            storedDRCDate = storedAreaDRCDate;
            dateKey = Layout.DRC_LAST_GOOD_DATE_AREA;
            bitKey = null;
        }

        StoreDRCInfo data = storedDRCDate.get(cell);
        boolean firstTime = false;

        if (data == null)
        {
            boolean validVersion = true;
            Version version = cell.getLibrary().getVersion();
            if (version != null) validVersion = version.compareTo(Version.getVersion()) >=0;
            data = new StoreDRCInfo(-1, Layout.DRC_LAST_GOOD_BIT_DEFAULT);
            storedDRCDate.put(cell, data);
            firstTime = true; // to load Variable date from disk in case of inMemory case.
            if (!validVersion)
                return null; // only the first the data is access the version is considered
        }
        if (fromDisk || (!fromDisk && firstTime))
        {
            GenMath.MutableLong lastDRCDateInMilliseconds = new GenMath.MutableLong(0);
            // Nothing found
            if (!getDateStored(cell, dateKey, lastDRCDateInMilliseconds))
                return null;

            int thisByte = Layout.DRC_LAST_GOOD_BIT_DEFAULT;
            if (bitKey != null)
            {
                Integer varBitsAsInt = cell.getVarValue(bitKey, Integer.class);
                if (varBitsAsInt != null) {
                    thisByte = varBitsAsInt.intValue();
                } else {
                    Byte varBitsAsByte = cell.getVarValue(bitKey, Byte.class);
                    if (varBitsAsByte != null)
                        thisByte = varBitsAsByte.byteValue();
                    else
                        System.out.println("No valid bit associated to DRC data was found as cell variable");
                }
//                Variable varBits = cell.getVar(bitKey, Integer.class);
//                if (varBits == null) // old Byte class
//                {
//                    varBits = cell.getVar(bitKey, Byte.class);
//                    if (varBits != null)
//                        thisByte =((Byte)varBits.getObject()).byteValue();
//                    else
//                        System.out.println("No valid bit associated to DRC data was found as cell variable");
//                }
//                else
//                    thisByte = ((Integer)varBits.getObject()).intValue();
            }
            data.bits = thisByte;
            data.date = lastDRCDateInMilliseconds.longValue();
        }
        else
        {
            data = storedDRCDate.get(cell);
        }
        return data;
    }

    /**
     * Method to check if current date is later than cell revision
     * @param cell
     * @param date
     * @return true if DRC date in cell is valid
     */
    public static boolean isCellDRCDateGood(Cell cell, Date date)
    {
        if (date != null)
        {
            Date lastChangeDate = cell.getRevisionDate();
            if (date.after(lastChangeDate)) return true;
        }
        return false;
    }

    /**
     * Method to tell the date of the last successful DRC of a given Cell.
     * @param cell the cell to query.
     * @param fromDisk
     * @return the date of the last successful DRC of that Cell.
     */
    public static Date getLastDRCDateBasedOnBits(Cell cell, boolean spacingCheck,
                                                 int activeBits, boolean fromDisk)
    {
        StoreDRCInfo data = getCellGoodDRCDateAndBits(cell, spacingCheck, fromDisk);

        // if data is null -> nothing found
        if (data == null)
            return null;

        int thisByte = data.bits;
        if (fromDisk && spacingCheck)
            assert(thisByte!=0);
        if (activeBits != Layout.DRC_LAST_GOOD_BIT_DEFAULT)
        {
//            boolean area = (thisByte & DRC_BIT_AREA) == (activeBits & DRC_BIT_AREA);
            boolean extension = (thisByte & DRC_BIT_EXTENSION) == (activeBits & DRC_BIT_EXTENSION);
            // DRC date is invalid if conditions were checked for another foundry
            boolean sameManufacturer = (thisByte & DRC_BIT_TSMC_FOUNDRY) == (activeBits & DRC_BIT_TSMC_FOUNDRY) &&
                    (thisByte & DRC_BIT_ST_FOUNDRY) == (activeBits & DRC_BIT_ST_FOUNDRY) &&
                    (thisByte & DRC_BIT_MOSIS_FOUNDRY) == (activeBits & DRC_BIT_MOSIS_FOUNDRY);
            assert(activeBits != 0);
            if (activeBits != 0 && (/*!area* || */ !extension || !sameManufacturer))
                return null;
        }

        // If in memory, date doesn't matter
        Date revisionDate = cell.getRevisionDate();
        Date lastDRCDate = new Date(data.date);
        return (lastDRCDate.after(revisionDate)) ? lastDRCDate : null;
    }

    /**
     * Method to clean any DRC date stored previously
     * @param cell the cell to clean
     */
    private static void cleanDRCDateAndBits(Cell cell, Variable.Key key)
    {
        if (key == Layout.DRC_LAST_GOOD_DATE_SPACING)
        {
            cell.delVar(Layout.DRC_LAST_GOOD_DATE_SPACING);
            cell.delVar(Layout.DRC_LAST_GOOD_BIT_SPACING);
        }
        else
            cell.delVar(Layout.DRC_LAST_GOOD_DATE_AREA);
    }

    public static String explainBits(int bits)
    {
        boolean on = !isIgnoreAreaChecking(); // (bits & DRC_BIT_AREA) != 0;
        String msg = "area bit ";
        msg += on ? "on" : "off";

        on = (bits & DRC_BIT_EXTENSION) != 0;
        msg += ", extension bit ";
        msg += on ? "on" : "off";

        if ((bits & DRC_BIT_TSMC_FOUNDRY) != 0)
            msg += ", TSMC bit";
        else if ((bits & DRC_BIT_ST_FOUNDRY) != 0)
            msg += ", ST bit";
        else if ((bits & DRC_BIT_MOSIS_FOUNDRY) != 0)
            msg += ", Mosis bit";
        return msg;
    }

    public static int getActiveBits(Technology tech)
    {
        int bits = 0;
//        if (!isIgnoreAreaChecking()) bits |= DRC_BIT_AREA;
        if (!isIgnoreExtensionRuleChecking()) bits |= DRC_BIT_EXTENSION;
        // Adding foundry to bits set
        Foundry foundry = tech.getSelectedFoundry();
        if (foundry != null)
        {
            bits |= foundry.getType().getBit();
//            switch(foundry.getType())
//	        {
//	            case MOSIS:
//	                bits |= DRC_BIT_MOSIS_FOUNDRY;
//	                break;
//	            case TSMC:
//	                bits |= DRC_BIT_TSMC_FOUNDRY;
//	                break;
//	            case ST:
//	                bits |= DRC_BIT_ST_FOUNDRY;
//	                break;
//	        }
        }
        return bits;
    }

    /**
     * Check networks rules of this Cell.
     * @param errorLog error logger
     * @param cell cell to check
     * @param true if this is layout cell.
     */
    private static void checkNetworks(ErrorLogger errorLog, Cell cell, boolean isLayout) {
        final int errorSortNetworks = 0;
        final int errorSortNodes = 1;
        Map<NodeProto,ArrayList<NodeInst>> strangeNodes = null;
        Map<NodeProto,ArrayList<NodeInst>> unconnectedPins = null;
        for (int i = 0, numNodes = cell.getNumNodes(); i < numNodes; i++) {
            NodeInst ni = cell.getNode(i);
            NodeProto np = ni.getProto();
            if (!cell.isIcon()) {
                if (ni.isIconOfParent() ||
                        np.getFunction() == PrimitiveNode.Function.ART && np != Generic.tech().simProbeNode ||
//                        np == Artwork.tech.pinNode ||
                        np == Generic.tech().invisiblePinNode) {
                    if (ni.hasConnections()) {
                        String msg = "Network: " + cell + " has connections on " + ni;
                        System.out.println(msg);
                        errorLog.logError(msg, ni, cell, null, errorSortNodes);
                    }
                } else if (np.getFunction() == PrimitiveNode.Function.PIN &&
                        cell.getTechnology().isLayout() && !ni.hasConnections()) {
                    if (unconnectedPins == null)
                        unconnectedPins = new HashMap<NodeProto,ArrayList<NodeInst>>();
                    ArrayList<NodeInst> pinsOfType = unconnectedPins.get(np);
                    if (pinsOfType == null) {
                        pinsOfType = new ArrayList<NodeInst>();
                        unconnectedPins.put(np, pinsOfType);
                    }
                    pinsOfType.add(ni);
                }
            }
            if (isLayout) {
                if (ni.getNameKey().isBus()) {
                    String msg = "Network: Layout " + cell + " has arrayed " + ni;
                    System.out.println(msg);
                    errorLog.logError(msg, ni, cell, null, errorSortNetworks);
                }
                boolean isSchematicNode;
                if (ni.isCellInstance()) {
                    Cell subCell = (Cell)np;
                    isSchematicNode = subCell.isIcon() || subCell.isSchematic();
                } else {
                    isSchematicNode = np == Generic.tech().universalPinNode ||np.getTechnology() == Schematics.tech();
                }
                if (isSchematicNode) {
                    if (strangeNodes == null)
                        strangeNodes = new HashMap<NodeProto,ArrayList<NodeInst>>();
                    ArrayList<NodeInst> nodesOfType = strangeNodes.get(np);
                    if (nodesOfType == null) {
                        nodesOfType = new ArrayList<NodeInst>();
                        strangeNodes.put(np, nodesOfType);
                    }
                    nodesOfType.add(ni);
                }
            }
        }
        if (unconnectedPins != null) {
            for (NodeProto np : unconnectedPins.keySet()) {
                ArrayList<NodeInst> pinsOfType = unconnectedPins.get(np);
                String msg = "Network: " + cell + " has " + pinsOfType.size() + " unconnected pins " + np;
                System.out.println(msg);
                errorLog.logWarning(msg, Collections.<Geometric>unmodifiableList(pinsOfType),
                        null, null, null, null, cell, errorSortNodes);
            }
        }
        if (strangeNodes != null) {
            for (NodeProto np : strangeNodes.keySet()) {
                ArrayList<NodeInst> nodesOfType = strangeNodes.get(np);
                String msg = "Network: Layout " + cell + " has " + nodesOfType.size() +
                        " " + np.describe(true) + " nodes";
                System.out.println(msg);
                errorLog.logError(msg, Collections.<Geometric>unmodifiableList(nodesOfType), null, cell, errorSortNetworks);
            }
        }
    }

    static boolean checkNodeSize(NodeInst ni, Cell cell, ReportInfo reportInfo)
    {
        boolean errorsFound = false;
        // check node for minimum size
        NodeProto np = ni.getProto();
        PrimitiveNode.NodeSizeRule sizeRule = getMinSize(np);
		if (sizeRule != null)
		{
            PrimitiveNodeSize npSize = ni.getNodeInstSize(null);
            List<PrimitiveNode.NodeSizeRule.NodeSizeRuleError> errorsList = sizeRule.checkSize(npSize);
//            EPoint niSize = new EPoint(ni.getXSize(), ni.getYSize());
//            EPoint niBase = new EPoint(ni.getLambdaBaseXSize(), ni.getLambdaBaseYSize());
//            List<PrimitiveNode.NodeSizeRule.NodeSizeRuleError> errorsLis = sizeRule.checkSize(niSize, niBase);

            if (errorsList != null)
            {
                for (PrimitiveNode.NodeSizeRule.NodeSizeRuleError e : errorsList)
                {
                    createDRCErrorLogger(reportInfo, DRC.DRCErrorType.MINSIZEERROR, e.message, cell,
                        e.minSize, e.actual, sizeRule.getRuleName(), null, ni, null, null, null, null);
                    errorsFound = true;
                }
            }
        }
        return errorsFound;
    }

    /****************************** OPTIONS ******************************/

	private static Pref cacheIncrementalDRCOn = Pref.makeBooleanPref("IncrementalDRCOn", tool.prefs, false);
	/**
	 * Method to tell whether DRC should be done incrementally.
	 * The default is "false".
	 * @return true if DRC should be done incrementally.
	 */
	public static boolean isIncrementalDRCOn() { return cacheIncrementalDRCOn.getBoolean(); }
	/**
	 * Method to set whether DRC should be done incrementally.
	 * @param on true if DRC should be done incrementally.
	 */
	public static void setIncrementalDRCOn(boolean on) { cacheIncrementalDRCOn.setBoolean(on); }
	/**
	 * Method to tell whether DRC should be done incrementally, by default.
	 * @return true if DRC should be done incrementally, by default.
	 */
	public static boolean isFactoryIncrementalDRCOn() { return cacheIncrementalDRCOn.getBooleanFactoryValue(); }

	private static Pref cacheInteractiveDRCDragOn = Pref.makeBooleanPref("InteractiveDRCDrag", tool.prefs, true);
	/**
	 * Method to tell whether DRC violations should be shown while nodes and arcs are dragged.
	 * The default is "true".
	 * @return true if DRC violations should be shown while nodes and arcs are dragged.
	 */
	public static boolean isInteractiveDRCDragOn() { return cacheInteractiveDRCDragOn.getBoolean(); }
	/**
	 * Method to set whether DRC violations should be shown while nodes and arcs are dragged.
	 * @param on true if DRC violations should be shown while nodes and arcs are dragged.
	 */
	public static void setInteractiveDRCDragOn(boolean on) { cacheInteractiveDRCDragOn.setBoolean(on); }
	/**
	 * Method to tell whether DRC violations should be shown while nodes and arcs are dragged, by default.
	 * @return true if DRC violations should be shown while nodes and arcs are dragged, by default.
	 */
	public static boolean isFactoryInteractiveDRCDragOn() { return cacheInteractiveDRCDragOn.getBooleanFactoryValue(); }

    /** Logging Type **/
    private static Pref cacheErrorLoggingType = Pref.makeStringPref("ErrorLoggingType", tool.prefs,
            DRCCheckLogging.DRC_LOG_PER_CELL.name());
	/**
	 * Method to retrieve logging type in DRC
	 * The default is "DRC_LOG_PER_CELL".
	 * @return integer representing error type
	 */
	public static DRCCheckLogging getErrorLoggingType() {return DRCCheckLogging.valueOf(cacheErrorLoggingType.getString());}
	/**
	 * Method to set DRC logging type.
	 * @param type representing error logging mode.
	 */
	public static void setErrorLoggingType(DRCCheckLogging type) { cacheErrorLoggingType.setString(type.name()); }
	/**
	 * Method to retrieve logging type in DRC, by default.
	 * @return integer representing error type, by default.
	 */
	public static DRCCheckLogging getFactoryErrorLoggingType() {return DRCCheckLogging.valueOf(cacheErrorLoggingType.getStringFactoryValue());}

    /** ErrorLevel **/
    private static Pref cacheErrorCheckLevel = Pref.makeIntPref("ErrorCheckLevel", tool.prefs,
            DRCCheckMode.ERROR_CHECK_DEFAULT.mode());
	/**
	 * Method to retrieve checking level in DRC.
	 * The default is "ERROR_CHECK_DEFAULT".
	 * @return integer representing error type.
	 */
	public static DRCCheckMode getErrorType()
    {
        int val = cacheErrorCheckLevel.getInt();
        for (DRCCheckMode p : DRCCheckMode.values())
        {
            if (p.mode() == val) return p;
        }
        return null;
    }
	/**
	 * Method to set DRC error type.
	 * @param type representing error level.
	 */
	public static void setErrorType(DRCCheckMode type) { cacheErrorCheckLevel.setInt(type.mode()); }
	/**
	 * Method to retrieve checking level in DRC, by default.
	 * @return integer representing error type, by default.
	 */
	public static DRCCheckMode getFactoryErrorType()
    {
        int val = cacheErrorCheckLevel.getIntFactoryValue();
        for (DRCCheckMode p : DRCCheckMode.values())
        {
            if (p.mode() == val) return p;
        }
        return null;
    }

	private static Pref cacheIgnoreCenterCuts = Pref.makeBooleanPref("IgnoreCenterCuts", tool.prefs, false);
//    static { cacheIgnoreCenterCuts.attachToObject(tool, "Tools/DRC tab", "DRC ignores center cuts in large contacts"); }
	/**
	 * Method to tell whether DRC should ignore center cuts in large contacts.
	 * Only the perimeter of cuts will be checked.
	 * The default is "false".
	 * @return true if DRC should ignore center cuts in large contacts.
	 */
	public static boolean isIgnoreCenterCuts() { return cacheIgnoreCenterCuts.getBoolean(); }
	/**
	 * Method to set whether DRC should ignore center cuts in large contacts.
	 * Only the perimeter of cuts will be checked.
	 * @param on true if DRC should ignore center cuts in large contacts.
	 */
	public static void setIgnoreCenterCuts(boolean on) { cacheIgnoreCenterCuts.setBoolean(on); }
	/**
	 * Method to tell whether DRC should ignore center cuts in large contacts, by default.
	 * Only the perimeter of cuts will be checked.
	 * @return true if DRC should ignore center cuts in large contacts, by default.
	 */
	public static boolean isFactoryIgnoreCenterCuts() { return cacheIgnoreCenterCuts.getBooleanFactoryValue(); }

    private static Pref cacheIgnoreAreaChecking = Pref.makeBooleanPref("IgnoreAreaCheck", tool.prefs, false);
//    static { cacheIgnoreAreaChecking.attachToObject(tool, "Tools/DRC tab", "DRC ignores area checking"); }
	/**
	 * Method to tell whether DRC should ignore minimum/enclosed area checking.
	 * The default is "false".
	 * @return true if DRC should ignore minimum/enclosed area checking.
	 */
	public static boolean isIgnoreAreaChecking() { return cacheIgnoreAreaChecking.getBoolean(); }
	/**
	 * Method to set whether DRC should ignore minimum/enclosed area checking.
	 * @param on true if DRC should ignore minimum/enclosed area checking.
	 */
	public static void setIgnoreAreaChecking(boolean on) { cacheIgnoreAreaChecking.setBoolean(on); }
	/**
	 * Method to tell whether DRC should ignore minimum/enclosed area checking, by default.
	 * @return true if DRC should ignore minimum/enclosed area checking, by default.
	 */
	public static boolean isFactoryIgnoreAreaChecking() { return cacheIgnoreAreaChecking.getBooleanFactoryValue(); }

    private static Pref cacheIgnoreExtensionRuleChecking = Pref.makeBooleanPref("IgnoreExtensionRuleCheck", tool.prefs, false);
//    static { cacheIgnoreExtensionRuleChecking.attachToObject(tool, "Tools/DRC tab", "DRC extension rule checking"); }
	/**
	 * Method to tell whether DRC should check extension rules.
	 * The default is "false".
	 * @return true if DRC should check extension rules.
	 */
	public static boolean isIgnoreExtensionRuleChecking() { return cacheIgnoreExtensionRuleChecking.getBoolean(); }
	/**
	 * Method to set whether DRC should check extension rules.
	 * @param on true if DRC should check extension rules.
	 */
	public static void setIgnoreExtensionRuleChecking(boolean on) { cacheIgnoreExtensionRuleChecking.setBoolean(on); }
	/**
	 * Method to tell whether DRC should check extension rules, by default.
	 * @return true if DRC should check extension rules, by default.
	 */
	public static boolean isFactoryIgnoreExtensionRuleChecking() { return cacheIgnoreExtensionRuleChecking.getBooleanFactoryValue(); }

    private static Pref cacheStoreDatesInMemory = Pref.makeBooleanPref("StoreDatesInMemory", tool.prefs, false);
    /**
     * Method to tell whether DRC dates should be stored in memory or not.
     * The default is "false".
     * @return true if DRC dates should be stored in memory or not.
     */
    public static boolean isDatesStoredInMemory() { return cacheStoreDatesInMemory.getBoolean(); }
    /**
     * Method to set whether DRC dates should be stored in memory or not.
     * @param on true if DRC dates should be stored in memory or not.
     */
    public static void setDatesStoredInMemory(boolean on) { cacheStoreDatesInMemory.setBoolean(on); }
    /**
     * Method to tell whether DRC dates should be stored in memory or not, by default.
     * @return true if DRC dates should be stored in memory or not, by default.
     */
    public static boolean isFactoryDatesStoredInMemory() { return cacheStoreDatesInMemory.getBooleanFactoryValue(); }

    private static Pref cacheInteractiveLog = Pref.makeBooleanPref("InteractiveLog", tool.prefs, false);
    /**
     * Method to tell whether DRC loggers should be displayed in Explorer immediately
     * The default is "false".
     * @return true if DRC loggers should be displayed in Explorer immediately or not.
     */
    public static boolean isInteractiveLoggingOn() { return cacheInteractiveLog.getBoolean(); }
    /**
     * Method to set whether DRC loggers should be displayed in Explorer immediately or not
     * @param on true if DRC loggers should be displayed in Explorer immediately.
     */
    public static void setInteractiveLogging(boolean on) { cacheInteractiveLog.setBoolean(on); }
    /**
     * Method to tell whether DRC loggers should be displayed in Explorer immediately, by default.
     * @return true if DRC loggers should be displayed in Explorer immediately or not, by default.
     */
    public static boolean isFactoryInteractiveLoggingOn() { return cacheInteractiveLog.getBooleanFactoryValue(); }

    private static Pref cacheMinAreaAlgo = Pref.makeStringPref("MinAreaAlgorithm", tool.prefs, DRCCheckMinArea.AREA_LOCAL.name());
    /**
     * Method to tell which min area algorithm to use.
     * The default is AREA_BASIC which is the brute force version
     * @return true if DRC loggers should be displayed in Explorer immediately or not.
     */
    public static DRCCheckMinArea getMinAreaAlgoOption() { return DRCCheckMinArea.valueOf(cacheMinAreaAlgo.getString()); }
    /**
     * Method to set which min area algorithm to use.
     * @param mode DRCCheckMinArea type to set
     */
    public static void setMinAreaAlgoOption(DRCCheckMinArea mode) { cacheMinAreaAlgo.setString(mode.name()); }
    /**
     * Method to tell which min area algorithm to use, by default.
     * @return true if DRC loggers should be displayed in Explorer immediately or not, by default.
     */
    public static DRCCheckMinArea getFactoryMinAreaAlgoOption() { return DRCCheckMinArea.valueOf(cacheMinAreaAlgo.getStringFactoryValue()); }

    private static Pref cacheMultiThread = Pref.makeBooleanPref("MinMultiThread", tool.prefs, false);
    /**
     * Method to tell whether DRC should run in a single thread or multi-threaded.
     * The default is single-threaded.
     * @return true if DRC run in a multi-threaded fashion.
     */
    public static boolean isMultiThreaded() { return cacheMultiThread.getBoolean(); }
    /**
     * Method to set whether DRC should run in a single thread or multi-threaded.
     * @param mode True if it will run a multi-threaded version.
     */
    public static void setMultiThreaded(boolean mode) { cacheMultiThread.setBoolean(mode); }
    /**
     * Method to tell whether DRC should run in a single thread or multi-threaded, by default.
     * @return true if DRC run in a multi-threaded fashion, by default.
     */
    public static boolean isFactoryMultiThreaded() { return cacheMultiThread.getBooleanFactoryValue(); }

    /****************************** END OF OPTIONS ******************************/

    /***********************************
     * Update Functions
     ***********************************/

    static void addDRCUpdate(int spacingBits,
                             Set<Cell> goodSpacingDRCDate, Set<Cell> cleanSpacingDRCDate,
                             Set<Cell> goodAreaDRCDate, Set<Cell> cleanAreaDRCDate,
                             Map<Geometric, List<Variable>> newVariables)
    {
        boolean goodSpace = (goodSpacingDRCDate != null && goodSpacingDRCDate.size() > 0);
        boolean cleanSpace = (cleanSpacingDRCDate != null && cleanSpacingDRCDate.size() > 0);
        boolean goodArea = (goodAreaDRCDate != null && goodAreaDRCDate.size() > 0);
        boolean cleanArea = (cleanAreaDRCDate != null && cleanAreaDRCDate.size() > 0);
        boolean vars = (newVariables != null && newVariables.size() > 0);
        if (!goodSpace && !cleanSpace && !vars && !goodArea && !cleanArea) return; // nothing to do
        new DRCUpdate(spacingBits, goodSpacingDRCDate, cleanSpacingDRCDate,
            goodAreaDRCDate, cleanAreaDRCDate, newVariables);
    }

	/**
	 * Method to delete all cached date information on all cells.
     * @param startJob
     */
	public static void resetDRCDates(boolean startJob)
	{
        new DRCReset(startJob);
	}

    /***********************************
     * DRCReset class
     ***********************************/
    private static class DRCReset extends Job
    {
        DRCReset(boolean startJob)
        {
            super("Resetting DRC Dates", User.getUserTool(), Job.Type.CHANGE, null, null, Priority.USER);
            if (startJob)
                startJob();
            else
                doIt();
        }

        public boolean doIt()
        {
            storedSpacingDRCDate.clear();
            storedAreaDRCDate.clear();
            // Always clean the dates as variables.
//            if (!isDatesStoredInMemory())
            {
                for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
                {
                    Library lib = it.next();
                    for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
                    {
                        Cell cell = cIt.next();
                        cleanDRCDateAndBits(cell, Layout.DRC_LAST_GOOD_DATE_SPACING);
                        cleanDRCDateAndBits(cell, Layout.DRC_LAST_GOOD_DATE_AREA);
                    }
                }
            }
            return true;
        }
    }

    /***********************************
     * DRCUpdate class
     ***********************************/
    /**
	 * Class to save good Layout DRC dates in a new thread or add new variables in Schematic DRC
	 */
	private static class DRCUpdate extends Job
	{
		Set<Cell> goodSpacingDRCDate;
		Set<Cell> cleanSpacingDRCDate;
        Set<Cell> goodAreaDRCDate;
		Set<Cell> cleanAreaDRCDate;
        Map<Geometric,List<Variable>> newVariables;
        int activeBits = Layout.DRC_LAST_GOOD_BIT_DEFAULT;

		public DRCUpdate(int bits,
                         Set<Cell> goodSpacingDRCD, Set<Cell> cleanSpacingDRCD,
                         Set<Cell> goodAreaDRCD, Set<Cell> cleanAreaDRCD,
                         Map<Geometric, List<Variable>> newVars)
		{
			super("Update DRC data", tool, Type.CHANGE, null, null, Priority.USER);
            this.goodSpacingDRCDate = goodSpacingDRCD;
			this.cleanSpacingDRCDate = cleanSpacingDRCD;
            this.goodAreaDRCDate = goodAreaDRCD;
            this.cleanAreaDRCDate = cleanAreaDRCD;
            this.newVariables = newVars;
            this.activeBits = bits;
            // Only works for layout with in memory dates -> no need of adding the job into the queue
            if (isDatesStoredInMemory() && (newVars == null || newVars.isEmpty()))
            {
                try {doIt();} catch (Exception e) {e.printStackTrace();}
            }
            else // put it into the queue
			    startJob();
		}

        /**
         * Template method to set DAte and bits information for a given map.
         * @param inMemory
         */
        private static void setInformation(Map<Cell,StoreDRCInfo> storedDRCDate,
                                           Set<Cell> goodDRCDate, Set<Cell> cleanDRCDate,
                                           Variable.Key key, int bits, boolean inMemory)
        {
            Set<Cell> goodDRCCells = new HashSet<Cell>();
            long time = System.currentTimeMillis();

            if (goodDRCDate != null)
            {
                for (Cell cell : goodDRCDate)
                {
                    if (!cell.isLinked())
                        new JobException("Cell '" + cell + "' is invalid to clean DRC date");
                    else
                    {
                        if (inMemory)
                            storedDRCDate.put(cell, new StoreDRCInfo(time, bits));
                        else
                            goodDRCCells.add(cell);
                    }
                }
            }
            if (!goodDRCCells.isEmpty())
                Layout.setGoodDRCCells(goodDRCCells, key, bits, inMemory);

            if (cleanDRCDate != null)
            {
                for (Cell cell : cleanDRCDate)
                {
                    if (!cell.isLinked())
                        new JobException("Cell '" + cell + "' is invalid to clean DRC date");
                    else
                    {
                        StoreDRCInfo data = storedDRCDate.get(cell);
                        assert(data != null);
                        data.date = -1;
                        data.bits = Layout.DRC_LAST_GOOD_BIT_DEFAULT; // I can't put null because of the version
                        if (!inMemory)
                            cleanDRCDateAndBits(cell, key);
                    }
                }
            }

        }

        public boolean doIt() throws JobException
		{
            boolean inMemory = isDatesStoredInMemory();

            setInformation(storedSpacingDRCDate, goodSpacingDRCDate, cleanSpacingDRCDate,
                Layout.DRC_LAST_GOOD_DATE_SPACING, activeBits, inMemory);

            setInformation(storedAreaDRCDate, goodAreaDRCDate, cleanAreaDRCDate,
                Layout.DRC_LAST_GOOD_DATE_AREA, Layout.DRC_LAST_GOOD_BIT_DEFAULT, inMemory);

            // Update variables in Schematics DRC
            if (newVariables != null)
            {
                assert(!inMemory);
                for (Map.Entry<Geometric,List<Variable>> e : newVariables.entrySet())
                {
                    Geometric ni = e.getKey();
                    for (Variable var : e.getValue()) {
                        if (ni.isParam(var.getKey()))
                            ((NodeInst)ni).addParameter(var);
                        else
                            ni.addVar(var);
                    }
                }
            }
			return true;
		}
	}

    /***********************************
     * JUnit interface
     ***********************************/
    public static boolean testAll()
    {
        return true;
    }
}
