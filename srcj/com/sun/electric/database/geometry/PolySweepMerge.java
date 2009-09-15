/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PolySweepMerge.java
 * Written by Gilda Garreton, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.geometry;

import com.sun.electric.technology.Layer;

import java.awt.geom.*;
import java.awt.Shape;
import java.util.*;

/**
 * Class to implement geometric sweep algorithm in 2D for areas.
 */
public class PolySweepMerge extends GeometryHandler
{
//    public static final int ONE_FRONTIER_MODE = 1;
//    public static final int TWO_FRONTIER_MODE = 2;

    //private int mode = ONE_FRONTIER_MODE;

    /**
	 * Method to create a new "merge" object.
	 */
	public PolySweepMerge()
	{
	}


	/**
	 * Method to create a new "merge" object.
	 */
	public PolySweepMerge(int initialSize)
	{
        super(initialSize);
	}

    /**
     * Method to switch between a sweep algorithm with one
     * or two frontiers
     * @param mode
     */
    public void setMode(int mode)
    {
        //just to  get rest of
        //this.mode = mode;
    }

    private static class PolySweepContainer
    {
        private List<Area> areas = null; // Needs to be a list to apply sort

        public PolySweepContainer(boolean createPolyList)
        {
            areas = (createPolyList) ? new ArrayList<Area>() : null;
        }

        public void add(Object value)
        {
            Area a = null;
            if (value instanceof Shape)
                a = new Area((Shape)value);
            else
                System.out.println("Error: invalid class for addition in PolySweepMerge");
            areas.add(a);
        }

        public void subtract(Object element)
        {
            Area elem = null;
            if (element instanceof Shape)
                elem = new Area((Shape)element);
            else
                System.out.println("Error: invalid class for subtraction in PolySweepMerge");
            for (Area a : areas)
            {
                a.subtract(elem);
            }
        }
    }

    // To insert new element into handler
	public void add(Layer key, Object element)
    {
        PolySweepContainer container = (PolySweepContainer)layers.get(key);

        if (container == null)
        {
            container = new PolySweepContainer(true);
            layers.put(key, container);
        }
        container.add(element);
    }

    /**
	 * Method to subtract a geometrical object from the merged collection.
	 * @param key the key that this Object sits on.
	 * @param element the Object to merge.
	 */
    public void subtract(Object key, Object element)
    {
        PolySweepContainer container = (PolySweepContainer)layers.get(key);

        if (container == null) return;
        container.subtract(element);
    }

    /**
     * Method to subtract all geometries stored in hash map from corresponding layers
     * @param map
     */
    public void subtractAll(Map<Layer,List<PolyBase>> map)
    {
       // Need to add exclusion before final calculation
        for (Map.Entry<Layer,List<PolyBase>> e : map.entrySet())
       {
           PolySweepContainer container = (PolySweepContainer)layers.get(e.getKey());
           if (container == null) continue;
            for (PolyBase p : e.getValue())
              container.subtract(p);
       }
    }

	// To add an entire GeometryHandler like collections
	public void addAll(GeometryHandler subMerge, AffineTransform tTrans)
    {
        PolySweepMerge other = (PolySweepMerge)subMerge;
        List<Area> list = new ArrayList<Area>();

        for (Map.Entry<Layer,Object> e : other.layers.entrySet())
		{
			Layer layer = e.getKey();
            PolySweepContainer container = (PolySweepContainer)layers.get(layer);
            PolySweepContainer otherContainer = (PolySweepContainer)e.getValue();

            // Nothing valid in top cell
            if (container == null)
            {
                // No need of creating polyListArray
                container = new PolySweepContainer(false);
                layers.put(layer, container);
                container.areas = new ArrayList<Area>(otherContainer.areas.size());
            }

            // Since I have to apply local transformation, I can't use the same array
            List<Area> otherAreas = new ArrayList<Area>(otherContainer.areas.size());
            for (Area area : otherContainer.areas)
            {
                otherAreas.add((Area)area.clone());
            }
            Collections.sort(otherAreas, areaSort);
            Collections.sort(container.areas, areaSort);

            for (Area area : otherAreas)
            {
                if (tTrans != null) area.transform(tTrans);
                Rectangle2D rect = area.getBounds2D();
                double areaMinX = rect.getMinX();
                double areaMaxX = rect.getMaxX();
//                boolean done = false;
                list.clear();
                
                // Search for all elements that might overlap
                for (Area thisArea : container.areas)
                {
                    Rectangle2D thisRect = thisArea.getBounds2D();
                    if (areaMaxX < thisRect.getMinX())
                    {
//                        done = true;
                        break;
                    }
                    // They could collide
                    if (areaMinX <= thisRect.getMaxX())
                    {
                        list.add(thisArea);
                        area.add(thisArea);
                    }
                }
                // Remove elements with collisions
                container.areas.removeAll(list);
                container.areas.add(area);
            }
            otherAreas = null;
        }
    }

    public void postProcess(boolean merge)
    {
        if (merge) mergeProcess();
        else disjointProcess();
    }

    /**
     * Method to generate a set of disjoint polygons from original set
     */
    private void disjointProcess()
    {
        for (Object obj : layers.values())
        {
            PolySweepContainer container = (PolySweepContainer)obj;
            if (container == null) continue;

            Collections.sort(container.areas, areaSort);
            double maxXSweep = -Double.MAX_VALUE;
            Area areaXTmp = null;
            List<Area> twoFrontierAreas = new ArrayList<Area>();
            List<Area> tmp = new ArrayList<Area>();

            for (Area geomArea : container.areas)
            {
                Rectangle2D rectX = geomArea.getBounds2D();
                double minX = rectX.getX();
                double maxX = rectX.getMaxX();

                if (minX > maxXSweep)
                {
                    // Previous area is 100% disconnected
                    if (areaXTmp != null)
                    {
                       sweepYFrontier(twoFrontierAreas, tmp, false);
                       areaXTmp = null;
                    }
                    tmp.clear();
                }
                tmp.add(geomArea);
                if (areaXTmp == null)
                {
                    // First one!
                    areaXTmp = geomArea;
                }
                if (maxX > maxXSweep)
                    maxXSweep = maxX;
            }
            sweepYFrontier(twoFrontierAreas, tmp, false);
            container.areas = twoFrontierAreas;
            //container.polyList = null;
        }
    }

    private void mergeProcess()
    {
        for (Object obj : layers.values())
        {
            PolySweepContainer container = (PolySweepContainer)obj;
            if (container == null) continue;

            Collections.sort(container.areas, areaSort);
            double maxXSweep = -Double.MAX_VALUE;
            Area areaXTmp = null;
            List<Area> areas = new ArrayList<Area>();

            for (Area geom : container.areas)
            {
                Rectangle2D rectX = geom.getBounds2D();
                double minX = rectX.getX();
                double maxX = rectX.getMaxX();
                if (minX > maxXSweep)
                {
                    // Previous area is 100% disconnected
                    if (areaXTmp != null)
                    {
                       // Note: this comparison is not meant to call Area.equals!
                       if (!areas.contains(areaXTmp))
                           areas.add(areaXTmp);
                       //sweepYFrontier(twoFrontierAreas, tmp, true);
                       areaXTmp = null;
                    }
                }
                if (areaXTmp == null)
                {
                    // First one!
                    areaXTmp = geom;
                    areas.add(areaXTmp);
                }
                else
                {
                    areaXTmp.add(geom);
                }
                if (maxX > maxXSweep)
                    maxXSweep = maxX;
            }
            // Can't use HashSet due to Comparator
            if (areaXTmp != null && !areas.contains(areaXTmp))
                areas.add(areaXTmp);
            container.areas = areas;
            //sweepYFrontier(twoFrontierAreas, tmp, true);
        }
    }

    private static void sweepYFrontier(List<Area> twoFrontierAreas, List<Area> tmp, boolean merge)
    {
        // No fully tested yet
        if (merge) return;

        // Y frontier
        Collections.sort(tmp, areaSort);
        double maxYSweep = -Double.MAX_VALUE;
        Area areaYTmp = null;

        for (Area area : tmp)
        {
           Rectangle2D rectY = area.getBounds2D();
           double minY = rectY.getY();
           double maxY = rectY.getMaxY();

           // Done with this piece of geometry
           if (minY > maxYSweep)
           {
               if (areaYTmp != null)
               {
                   if (!twoFrontierAreas.contains(areaYTmp))
                       twoFrontierAreas.add(areaYTmp);
               }
               areaYTmp = null;
           }
            if (areaYTmp == null)
            {
                // first one on Y
                // In case no merge, areaYTmp will keep the entire region merged
                // for substraction purposes
                if (!merge)
                {
                    areaYTmp = (Area)area.clone();
                    twoFrontierAreas.add(area);
                }
                else
                {
                    areaYTmp = area;
                    twoFrontierAreas.add(areaYTmp);
                }
            }
            else
            {
                if (merge)
                    areaYTmp.add(area);
                else
                {
                    Area clone = (Area)area.clone();
                    clone.intersect(areaYTmp);
                    if (!clone.isEmpty())
                        area.subtract(areaYTmp);
                    if (!area.isEmpty())
                    {
                        twoFrontierAreas.add(area);
                        areaYTmp.add(area);
                    }
                }
            }

            if (maxY > maxYSweep)
                maxYSweep = maxY;
       }
        if (areaYTmp != null && merge && !twoFrontierAreas.contains(areaYTmp))
            twoFrontierAreas.add(areaYTmp);
    }

	/**
	 * To retrieve leave elements from internal structure
	 * @param layer current layer under analysis
	 * @param modified to avoid retrieving original polygons
	 * @param simple to obtain simple polygons
	 */
	public Collection<PolyBase> getObjects(Object layer, boolean modified, boolean simple)
    {
        PolySweepContainer container = (PolySweepContainer)layers.get(layer);

        if (container == null) return null;

        List<PolyBase> list = new ArrayList<PolyBase>();

        for (Area area : container.areas)
        {
            list.addAll(PolyBase.getPointsInArea(area, (Layer)layer, simple, false));
        }

        return list;
    }

    public List<Area> getAreas(Layer layer)
    {
        PolySweepContainer container = (PolySweepContainer)layers.get(layer);
        return (container != null) ? container.areas : null;
    }

    /**
     * To retrieve the roots containing all loops from the internal structure.
     * @param layer current layer under analysis
     * @return list of trees with loop hierarchy
     */
    public Collection<PolyBase.PolyBaseTree> getTreeObjects(Object layer)
    {
        PolySweepContainer container = (PolySweepContainer)layers.get(layer);

        if (container == null) return null;

        List<PolyBase.PolyBaseTree> list = new ArrayList<PolyBase.PolyBaseTree>();

        for (Area area : container.areas)
        {
            list.addAll(PolyBase.getPolyTrees(area, (Layer)layer));
        }

        return list;
    }

    /***************************************************************************************************************/
    /*                                           Classes for polygon partition
    /***************************************************************************************************************/
    static class PolyEdge
    {
        Point2D start;
        Point2D end;
        int dir;

        PolyEdge(Point2D s, Point2D e)
        {
            start = s;
            end = e;
            boolean alignX = DBMath.areEquals(s.getX(), e.getX());
            boolean alignY = DBMath.areEquals(s.getY(), e.getY());
            if (alignX && alignY)
            {
                System.out.println("Degenerated edge in 1 point :" + start);
//                assert (!alignX || !alignY); // can't be a point.
            }
            if (alignX)
                dir = PolyBase.X;
            else if (alignY)
                dir = PolyBase.Y;
            else
            {
                dir = PolyBase.XY;
//                assert(false); // no ready for angled edges
            }
        }
    }

    /**
     * Method to return the unique set of points in the polygon.
     * @param area
     * @return
     */
    private static Set<Point2D> getPoints(Area area)
    {
        double [] coords = new double[6];
        Set<Point2D> pointSet = new HashSet<Point2D>();

        for(PathIterator pIt = area.getPathIterator(null); !pIt.isDone(); )
        {
            int type = pIt.currentSegment(coords);
            if (type == PathIterator.SEG_CLOSE)
            {
                ;
            } else if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO)
            {
                Point2D pt = new Point2D.Double(coords[0], coords[1]);
                pointSet.add(pt);
            }
            pIt.next();
        }

        return pointSet;
    }

    private static List<PolyEdge> getEdges(Area area)
    {
        double [] coords = new double[6];
        List<Point2D> pointList = new ArrayList<Point2D>();
        List<PolyEdge> edgesList = new ArrayList<PolyEdge>();

        for(PathIterator pIt = area.getPathIterator(null); !pIt.isDone(); )
        {
            int type = pIt.currentSegment(coords);
            if (type == PathIterator.SEG_CLOSE)
            {
                int size = pointList.size();
                for (int i = 0; i < size; i++)
                {
                    PolyEdge edge = new PolyEdge(pointList.get(i), pointList.get((i+1)%size));
                    edgesList.add(edge);
                }
                pointList.clear();
            } else if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO)
            {
                Point2D pt = new Point2D.Double(coords[0], coords[1]);
                pointList.add(pt);
            }
            pIt.next();
        }

        return edgesList;
    }

    /***************************************************************************/
    /*                      GeometryHandlerQTree
    /***************************************************************************/
    static class GeometryHandlerQTree
    {
        Area area;
        List<GeometryHandlerQTree> sons = new ArrayList<GeometryHandlerQTree>();

        GeometryHandlerQTree(Area a)
        {
            area = a;
        }

        private static class CutBucket
        {
            int fullyContainedEdges;
            int totalCuts;
            double best, min, max;
            int dir;
            boolean found;
            List<PolyEdge> edgesList;

            CutBucket(int d, double m, double n, List<PolyEdge> list)
            {
                dir = d;
                found = false;
                min = m;
                max = n;
                edgesList = list;
            }

            void analyzePoint(double p)
            {
                // no cut
                if (!DBMath.isInBetweenExclusive(p, min, max)) return;

                // the last cut found for now
                found = true;
                best = p;
                totalCuts++;
            }
        }

        void refineDir(CutBucket cut)
        {
            Rectangle2D origRec = area.getBounds2D();
            Rectangle2D leftCut = null, rightCut = null;

            if (cut.dir == PolyBase.X) // vertical cut
            {
                leftCut = new Rectangle2D.Double(origRec.getX(), origRec.getY(),
                    (cut.best - origRec.getX()), origRec.getHeight());
                rightCut = new Rectangle2D.Double(cut.best, origRec.getY(),
                    (origRec.getMaxX() - cut.best), origRec.getHeight());
            }
            else if (cut.dir == PolyBase.Y)
            {
                leftCut = new Rectangle2D.Double(origRec.getX(), origRec.getY(),
                    origRec.getWidth(), (cut.best - origRec.getY()));
                rightCut = new Rectangle2D.Double(origRec.getX(), cut.best,
                    origRec.getWidth(), (origRec.getMaxY() - cut.best));
            }
            else
                assert(false); // XY case not implemented

            Area son1 = new Area(area); // copy original shape
            son1.intersect(new Area(leftCut));

            Area son2 = new Area(area);
            son2.intersect(new Area(rightCut));

            sons.add(new GeometryHandlerQTree(son1));
            sons.add(new GeometryHandlerQTree(son2));

//            // look for the cut points
//            Set<Double> newPoints = new HashSet<Double>(); // only unique points are required
//            List<PolyEdge> removeList = new ArrayList<PolyEdge>();
//            List<PolyEdge> addLeftList = new ArrayList<PolyEdge>();
//            List<PolyEdge> addRightList = new ArrayList<PolyEdge>();
//
//            for (PolyEdge e : cut.edgesList)
//            {
//                // No parallel
//                Double newP = null;
//                boolean horizontal = e.dir == PolyBase.Y;
//                if (e.dir != cut.dir)
//                {
//                    double min = horizontal ? e.start.getX() : e.start.getY();
//                    double max = horizontal ? e.end.getX() : e.end.getY();
//                    boolean startIsLeft = (min < max);
//
//                    if (DBMath.areEquals(min, cut.best)) // add the start point
//                    {
//                        newP = horizontal ? e.start.getY() : e.start.getX();
//                        if (startIsLeft) addLeftList.add(e);
//                        else addRightList.add(e);
//                    }
//                    else if (DBMath.areEquals(max, cut.best)) // add the end point
//                    {
//                        newP = horizontal ? e.end.getY() : e.end.getX();
//                        if (startIsLeft) addRightList.add(e);
//                        else addLeftList.add(e);
//                    }
//                    else if (DBMath.isInBetween(cut.best, min, max))
//                    // the swap btw min and max is done inside isInBetween
//                    {
//                        // adding new point
////                        newP = horizontal ? e.start.getY() : e.start.getX();  // same results with e.end
//                        removeList.add(e);
//                        Point2D newPoint;
//                        if (horizontal)
//                        {
//                            newP = e.start.getY();
//                            newPoint = new Point2D.Double(cut.best, newP);
//                        }
//                        else
//                        {
//                            newP = e.start.getX();
//                            newPoint = new Point2D.Double(newP, cut.best);
//                        }
//                        if (startIsLeft) // start is left/top
//                        {
//                            addLeftList.add(new PolyEdge(e.start, newPoint));
//                            addRightList.add(new PolyEdge(e.end, newPoint));
//                        }
//                        else  // start is right
//                        {
//                            addRightList.add(new PolyEdge(e.start, newPoint));
//                            addLeftList.add(new PolyEdge(e.end, newPoint));
//                        }
//                    }
//                }
////                else // parallel -> skip them because the other direction will provide the cutting points
////                {
////                    boolean aligned = (horizontal) ? DBMath.areEquals(e.start.getY(), cut.best) :
////                        DBMath.areEquals(e.start.getX(), cut.best);
////                    if (aligned)
////                    {
////                        // add both points
////                        newP = e.start;
////                        newPoints.add(e.end);
////                    }
////                }
//                if (newP != null)
//                    newPoints.add(newP);
//            }
//
//            // first remove delete old edges
////            cut.edgesList.removeAll(removeList);
//
//            // Sort the points to produce extra edges
//            List<Double> pList = new ArrayList<Double>();
//            pList.addAll(newPoints);
//            Collections.sort(pList);
//            // add the new edges
//            assert(pList.size()%2 == 0); // even number of points since the geometries are manhattan.
//            boolean horizontal = cut.dir == PolyBase.Y;
//            List<PolyEdge> inAllLists = new ArrayList<PolyEdge>();
//
//            for (int i = 0; i < pList.size(); i+=2)
//            {
//                PolyEdge edge;
//
//                if (horizontal)
//                    edge = new PolyEdge(new Point2D.Double(pList.get(i), cut.best),
//                        new Point2D.Double(pList.get(i+1), cut.best));
//                else
//                    edge = new PolyEdge(new Point2D.Double(cut.best, pList.get(i)),
//                        new Point2D.Double(cut.best, pList.get(i+1)));
//                addLeftList.add(edge);
//                addRightList.add(edge);
//            }
//
////            PolyBase poly1 = new PolyBase();
//
//            Area son1 = new Area();

        }

        void refine()
        {
            if (area.isRectangular())
                return; // nothing to refine
            Set<Point2D> pointsList = getPoints(area);
            List<PolyEdge> edgesList = getEdges(area);
            Rectangle2D rect = area.getBounds2D();
            // search for best X or Y cut
            CutBucket cutX = new CutBucket(PolyBase.X, rect.getMinX(), rect.getMaxX(), edgesList);
            CutBucket cutY = new CutBucket(PolyBase.Y, rect.getMinY(), rect.getMaxY(), edgesList);

            for (Point2D p : pointsList)
            {
                // X cut
                cutX.analyzePoint(p.getX());
                // Y cut
                cutY.analyzePoint(p.getY());
            }

            if (!cutX.found && !cutY.found)
                return; // nothing to refine
            if (cutX.found && !cutY.found) // refine along X
                refineDir(cutX);
            else if (!cutX.found && cutY.found)
                refineDir(cutY); // refine along Y
            else
            {
                // Easy solutions for now
                if (cutX.totalCuts > cutY.totalCuts)
                   refineDir(cutX);
                else
                   refineDir(cutY); // refine along Y
                // No heuristic to decide which direction first (only X or Y refinement)
//                assert(false);
            }

            // Continue recursively
            for (GeometryHandlerQTree s : sons)
            {
                s.refine();
            }
        }

        private void getSimplePolygons(List<PolyBase> list)
        {
            if (sons.isEmpty())  // it should be only 1!!
            {
                List<PolyBase> l = PolyBase.getLoopsFromArea(area, null);
                assert(l.size() == 1); // by design
                list.addAll(l);
            }
            else
            {
                for (GeometryHandlerQTree s : sons)
                {
                    s.getSimplePolygons(list);
                }
            }
        }
    }

    public Collection<PolyBase> getPolyPartition(Object layer)
    {
        List<PolyBase> list = new ArrayList<PolyBase>();
        PolySweepContainer container = (PolySweepContainer)layers.get(layer);
        Collections.sort(container.areas, areaSort);

        for (Area area : container.areas)
        {
            GeometryHandlerQTree g = new GeometryHandlerQTree(area);
            g.refine();
            g.getSimplePolygons(list);
        }
        return list;
    }
}
