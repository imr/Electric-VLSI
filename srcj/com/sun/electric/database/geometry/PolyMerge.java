/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PolyMerge.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.geometry;

import com.sun.electric.technology.Layer;

import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.*;
import java.util.Collection;
import java.util.List;

/**
 * This is the Polygon Merging facility.
 * <P>
 * Initially, call:<BR>
 *    PolyMerge merge = new PolyMerge();<BR>
 * The returned value is used in subsequent calls.
 * <P>
 * For every polygon, call:<BR>
 *    merge.addPolygon(layer, poly);<BR>
 * where "layer" is a layer and "poly" is a polygon to be added.
 * <P>
 * You can also subtract a polygon by calling:<BR>
 *    merge.subtract(layer, poly);<BR>
 * <P>
 * To combine two different merges, use:<BR>
 *    merge.addMerge(addmerge, trans)<BR>
 * to add the merge information in "addmerge" (transformed by "trans")
 * <P>
 * At end of merging, call:<BR>
 *    merge.getMergedPoints(layer)<BR>
 * for each layer, and it returns an array of PolyBases on that layer.
 */
public class PolyMerge
        extends GeometryHandler
{
	/**
	 * Method to create a new "merge" object.
	 */
	public PolyMerge()
	{
	}

	/**
	 * Method to add a PolyBase to the merged collection.
	 * @param key the layer that this PolyBase sits on.
	 * @param value the PolyBase to merge. If value is only Shape type
     * then it would take the bounding box. This might not be precise enough!.
	 */
	public void add(Layer key, Object value)
	{
		Layer layer = key;
        PolyBase poly = null;
        if (value instanceof PolyBase)
		    poly = (PolyBase)value;
        else if (value instanceof Shape)
            poly = new PolyBase(((Shape)value).getBounds2D());
        else
            return;
		addPolygon(layer, poly);
	}
	/**
	 * Method to add a PolyBase to the merged collection.
	 * @param layer the layer that this Poly sits on.
	 * @param poly the PolyBase to merge.
	 */
	public void addPolygon(Layer layer, PolyBase poly)
	{
		Area area = (Area)layers.get(layer);
		if (area == null)
		{
			area = new Area();
			layers.put(layer, area);
		}

		// add "poly" to "area"
		// It can't add only rectangles otherwise it doesn't cover
		// serpentine transistors.
		Area additionalArea = new Area(poly);
		area.add(additionalArea);
	}

	/**
	 * Method to subtract a PolyBase from the merged collection.
	 * @param layer the layer that this PolyBase sits on.
	 * @param poly the PolyBase to merge.
	 */
	public void subtract(Object layer, Object poly)
	{
		Area area = (Area)layers.get(layer);
		if (area == null) return;
		Area subtractArea = new Area((PolyBase)poly);
		area.subtract(subtractArea);
	}

	/**
	 * Method to add another Merge to this one.
	 * @param subMerge the other Merge to add in.
	 * @param trans a transformation on the other Merge.
	 */
	public void addAll(GeometryHandler subMerge, AffineTransform trans)
	{
		PolyMerge other = (PolyMerge)subMerge;
		addMerge(other, trans);
	}

	/**
	 * Method to add another Merge to this one.
	 * @param other the other Merge to add in.
	 * @param trans a transformation on the other Merge.
	 */
	public void addMerge(PolyMerge other, AffineTransform trans)
	{
		for(Layer subLayer : other.layers.keySet())
		{
			Area subArea = (Area)other.layers.get(subLayer);

			Area area = (Area)layers.get(subLayer);
			if (area == null)
			{
				area = new Area();
				layers.put(subLayer, area);
			}
			Area newArea = subArea.createTransformedArea(trans);
			area.add(newArea);
		}
	}

	/**
	 * Method to determine whether a polygon intersects a layer in the merge.
	 * @param layer the layer to test.
	 * @param poly the polygon to examine.
	 * @return true if any part of the polygon exists in that layer.
	 */
	public boolean intersects(Layer layer, PolyBase poly)
	{
		Area layerArea = (Area)layers.get(layer);
		if (layerArea == null) return false;

		// simple calculation for manhattan polygon
		Rectangle2D box = poly.getBox();
		if (box != null)
		{
			return layerArea.intersects(box);
		}

		// more complex calculation (not done yet)
		Area intersectArea = new Area(poly);
		intersectArea.intersect(layerArea);
		return !intersectArea.isEmpty();
	}

	/**
	 * Method to intersect two layers in this merge and produce a third.
	 * @param sourceA the first Layer to intersect.
	 * @param sourceB the second Layer to intersect.
	 * @param dest the destination layer to place the intersection of the first two.
	 * If there is no intersection, all geometry on this layer is cleared.
	 */
	public void intersectLayers(Layer sourceA, Layer sourceB, Layer dest)
	{
		Area destArea = null;
		Area sourceAreaA = (Area)layers.get(sourceA);
		if (sourceAreaA != null)
		{
			Area sourceAreaB = (Area)layers.get(sourceB);
			if (sourceAreaB != null)
			{
				destArea = new Area(sourceAreaA);
				destArea.intersect(sourceAreaB);
				if (destArea.isEmpty()) destArea = null;
			}
		}
		if (destArea == null) layers.remove(dest); else
			layers.put(dest, destArea);
	}

	/**
	 * Method to subtract one layer from another and produce a third.
	 * @param sourceA the first Layer.
	 * @param sourceB the second Layer, which gets subtracted from the first.
	 * @param dest the destination layer to place the sourceA - sourceB.
	 * If there is nothing left, all geometry on the layer is cleared.
	 */
	public void subtractLayers(Layer sourceA, Layer sourceB, Layer dest)
	{
		Area destArea = null;
		Area sourceAreaA = (Area)layers.get(sourceA);
		if (sourceAreaA != null)
		{
			Area sourceAreaB = (Area)layers.get(sourceB);
			if (sourceAreaB != null)
			{
				destArea = new Area(sourceAreaA);
				destArea.subtract(sourceAreaB);
				if (destArea.isEmpty()) destArea = null;
			}
		}
		if (destArea == null) layers.remove(dest); else
			layers.put(dest, destArea);
	}

	/**
	 * Method to inset one layer by a given amount and create a second layer.
	 * @param source the Layer to inset.
	 * @param dest the destination layer to place the inset geometry.
	 * @param amount the distance to inset the layer.
	 */
	public void insetLayer(Layer source, Layer dest, double amount)
	{
		Area sourceArea = (Area)layers.get(source);
		if (sourceArea == null) layers.remove(dest); else
		{
			layers.put(dest, (Area)sourceArea.clone());
			if (amount == 0) return;
			List<PolyBase> orig = getAreaPoints(sourceArea, source, true);
			Point2D [] subtractPoints = new Point2D[4];
			for(PolyBase poly : orig)
			{
				Point2D [] points = poly.getPoints();
				for(int i=0; i<points.length; i++)
				{
					int last = i-1;
					if (last < 0) last = points.length-1;
					Point2D lastPt = points[last];
					Point2D thisPt = points[i];
					if (DBMath.areEquals(lastPt, thisPt)) continue;
					int angle = DBMath.figureAngle(lastPt, thisPt);
					int perpAngle = (angle + 2700) % 3600;
					double offsetX = DBMath.cos(perpAngle) * amount;
					double offsetY = DBMath.sin(perpAngle) * amount;
					Point2D insetLastPt = new Point2D.Double(lastPt.getX() + offsetX, lastPt.getY() + offsetY);
					Point2D insetThisPt = new Point2D.Double(thisPt.getX() + offsetX, thisPt.getY() + offsetY);
					subtractPoints[0] = lastPt;
					subtractPoints[1] = thisPt;
					subtractPoints[2] = insetThisPt;
					subtractPoints[3] = insetLastPt;
					PolyBase subtractPoly = new PolyBase(subtractPoints);
					subtract(dest, subtractPoly);
				}
			}
		}
	}

	/**
	 * Method to delete all geometry on a given layer.
	 * @param layer the Layer to clear in this merge.
	 */
	public void deleteLayer(Layer layer)
	{
		layers.remove(layer);
	}

	/**
	 * Method to tell whether there is any valid geometry on a given layer of this merge.
	 * @param layer the layer to test.
	 * @return true if there is no valid geometry on the given layer in this merge.
	 */
	public boolean isEmpty(Layer layer)
	{
		Area area = (Area)layers.get(layer);
		if (area == null) return true;
		return area.isEmpty();
	}

	/**
	 * Method to determine whether a rectangle exists in the merge.
	 * @param layer the layer being tested.
	 * @param rect the rectangle being tested.
	 * @return true if all of the rectangle is inside of the merge on the given layer.
	 */
	public boolean contains(Layer layer, Rectangle2D rect)
	{
		Area area = (Area)layers.get(layer);
		if (area == null) return false;
		if (area.contains(rect)) return true;

		Area rectArea = new Area(rect);
		rectArea.subtract(area);
		
		// if the new area is empty, then the poly is completely contained in the merge
		if (rectArea.isEmpty()) return true;
		double remainingArea = getAreaOfArea(rectArea);
		if (DBMath.areEquals(remainingArea, 0)) return true;

		return false;
	}

	/**
	 * Method to determine whether a polygon exists in the merge.
	 * @param layer the layer being tested.
	 * @param poly the polygon being tested.
	 * @return true if all of the polygon is inside of the merge on the given layer.
	 */
	public boolean contains(Layer layer, PolyBase poly)
	{
		// find the area for the given layer
		Area area = (Area)layers.get(layer);
		if (area == null) return false;

		// create an area that is the new polygon minus the original area
		Area polyArea = new Area(poly);
		polyArea.subtract(area);
		
		// if the new area is empty, then the poly is completely contained in the merge
		if (polyArea.isEmpty()) return true;
		double remainingArea = getAreaOfArea(polyArea);
		if (DBMath.areEquals(remainingArea, 0)) return true;
		return false;
	}

	private double getAreaOfArea(Area area)
	{
		List<PolyBase> pointList = getAreaPoints(area, null, true);
		double totalArea = 0;
		for(PolyBase p : pointList)
		{
			totalArea += p.getArea();
		}
		return totalArea;
	}

	/**
	 * Method to determine whether a point exists in the merge.
	 * @param layer the layer being tested.
	 * @param pt the point being tested.
	 * @return true if the point is inside of the merge on the given layer.
	 */
	public boolean contains(Layer layer, Point2D pt)
	{
		Area area = (Area)layers.get(layer);
		if (area == null) return false;
		return area.contains(pt);
	}

	public Collection<PolyBase> getObjects(Object layer, boolean modified, boolean simple)
	{
		// Since simple is used, correct detection of loops must be guaranteed
		// outside.
		return getMergedPoints((Layer)layer, simple);
	}

    /**
	 * Method to return list of Polys on a given Layer in this Merge.
	 * @param layer the layer in question.
	 * @param simple
	 * @return the list of Polys that describes this Merge.
	 */
    public List<PolyBase> getMergedPoints(Layer layer, boolean simple)
	{
		Area area = (Area)layers.get(layer);
		if (area == null) return null;
		return getAreaPoints(area, layer, simple);
	}
   
	/**
	 * Method to return a list of polygons in this merge for a given layer.
	 * @param area the Area object that describes the merge.
	 * @param layer the desired Layer.
	 * @param simple true for simple polygons, false to allow complex ones.
	 * @return a List of PolyBase objects that describes the layer in the merge.
	 */
    public static List<PolyBase> getAreaPoints(Area area, Layer layer, boolean simple)
    {
        return PolyBase.getPointsInArea(area, layer, simple, true, null);
//		List<PolyBase> polyList = new ArrayList<PolyBase>();
//		double [] coords = new double[6];
//		List<Point2D> pointList = new ArrayList<Point2D>();
//		Point2D lastMoveTo = null;
//		boolean isSingular = area.isSingular();
//		List<PolyBase> toDelete = new ArrayList<PolyBase>();
//
//		// Gilda: best practice note: System.arraycopy
//		for(PathIterator pIt = area.getPathIterator(null); !pIt.isDone(); )
//		{
//			int type = pIt.currentSegment(coords);
//			if (type == PathIterator.SEG_CLOSE)
//			{
//				if (lastMoveTo != null) pointList.add(lastMoveTo);
//				Point2D [] points = new Point2D[pointList.size()];
//				int i = 0;
//				for(Point2D pt : pointList)
//					points[i++] = pt;
//				PolyBase poly = new PolyBase(points);
//				poly.setLayer(layer);
//				poly.setStyle(Poly.Type.FILLED);
//				lastMoveTo = null;
//				toDelete.clear();
//				if (!simple && !isSingular)
//				{
//					Iterator<PolyBase> it = polyList.iterator();
//					while (it.hasNext())
//					{
//						PolyBase pn = it.next();
//						if (pn.contains((Point2D)pointList.get(0)) ||
//						    poly.contains(pn.getPoints()[0]))
//						/*
//						if (pn.contains(poly.getBounds2D()) ||
//						    poly.contains(pn.getBounds2D()))
//						    */
//						{
//							points = pn.getPoints();
//							for (i = 0; i < points.length; i++)
//								pointList.add(points[i]);
//							Point2D[] newPoints = new Point2D[pointList.size()];
//							System.arraycopy(pointList.toArray(), 0, newPoints, 0, pointList.size());
//							poly = new PolyBase(newPoints);
//							toDelete.add(pn);
//							//break;
//						}
//					}
//				}
//				if (poly != null)
//					polyList.add(poly);
//				polyList.removeAll(toDelete);
//				pointList.clear();
//			} else if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO)
//			{
//				Point2D pt = new Point2D.Double(coords[0], coords[1]);
//				pointList.add(pt);
//				if (type == PathIterator.SEG_MOVETO) lastMoveTo = pt;
//			}
//			pIt.next();
//		}


//        if (Job.LOCALDEBUGFLAG)
//        {
//            List newList = PolyBase.getPointsInArea(area, layer, simple, true, null);
//
//            if (newList.size() != polyList.size())
//                System.out.println("Error in getPointsInArea");
//            else
//            {
//                boolean foundError = false;
//                for (PolyBase poly : polyList)
//                {
//                    boolean found = false;
//                    for (PolyBase poly1 : polyList)
//                    {
//                       if (poly1.polySame(poly)) { found = true; break;};
//                    }
//                    if (!found) {foundError=true; break;}
//                }
//                if (foundError)
//                    System.out.println("Error in getPointsInArea");
//            }
//        }
//		return polyList;
	}
}
