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
import java.awt.geom.PathIterator;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;

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
 *    merge.subPolygon(layer, poly);<BR>
 * <P>
 * To combine two different merges, use:<BR>
 *    merge.addMerge(addmerge, trans)<BR>
 * to add the merge information in "addmerge" (transformed by "trans")
 * <P>
 * At end of merging, call:<BR>
 *    merge.getMergedPoints(layer)<BR>
 * for each layer, and it returns an array of Polys on that layer.
 */
public class PolyMerge
{
	private HashMap allLayers;

	/*
	 * Method to create a new "merge" object.  After this call, multiple calls to
	 * "mergeaddpolygon()" may be made, followed by a single call to "mergeextract()"
	 * to obtain the merged geometry, and a call to "mergedelete()" to free this object.
	 * Returns zero on error.
	 */
	public PolyMerge()
	{
		allLayers = new HashMap();
	}

	/*
	 * Method to add polygon "poly" to the merged collection.  The polygon is on layer
	 * "layer" of technology "tech".
	 */
	public void addPolygon(Layer layer, Poly poly)
	{
		Area area = (Area)allLayers.get(layer);
		if (area == null)
		{
			area = new Area();
			allLayers.put(layer, area);
		}

		// add "poly" to "area"
		Rectangle2D bounds = poly.getBox();
		if (bounds != null)
		{
			Area additionalArea = new Area(bounds);
			area.add(additionalArea);
		} else
		{
		}
	}

	/*
	 * Method to subtract polygon "poly" from the merged collection.  The polygon is on layer
	 * "layer" of technology "tech".
	 */
	public void subPolygon(Layer layer, Poly poly)
	{
	}

	/*
	 * Method to add merge object "addmerge" to the merged collection "merge", transforming
	 * it by "trans".
	 */
	public void addMerge(PolyMerge other, AffineTransform trans)
	{
		for(Iterator it = other.allLayers.keySet().iterator(); it.hasNext(); )
		{
			Layer subLayer = (Layer)it.next();
			Area subArea = (Area)other.allLayers.get(subLayer);

			Area area = (Area)allLayers.get(subLayer);
			if (area == null)
			{
				area = new Area();
				allLayers.put(subLayer, area);
			}
			Area newArea = subArea.createTransformedArea(trans);
			area.add(newArea);
		}
	}

	public Iterator getLayersUsed()
	{
		return allLayers.keySet().iterator();
	}

	/*
	 * Method to return a merged Poly for a given layer.
	 * THIS CANNOT BE RIGHT, BECAUSE THERE MAY BE MULTIPLE POLYGONS.
	 */
	public List getMergedPoints(Layer layer)
	{
		Area area = (Area)allLayers.get(layer);
		if (area == null) return null;

		List polyList = new ArrayList();
		double [] coords = new double[6];
		List pointList = new ArrayList();
		Point2D lastMoveTo = null;
		for(PathIterator pIt = area.getPathIterator(null); !pIt.isDone(); )
		{
			int type = pIt.currentSegment(coords);
			if (type == PathIterator.SEG_CLOSE)
			{
				if (lastMoveTo != null) pointList.add(lastMoveTo);
				Point2D [] points = new Point2D[pointList.size()];
				int i = 0;
				for(Iterator it = pointList.iterator(); it.hasNext(); )
					points[i++] = (Point2D)it.next();
				Poly poly = new Poly(points);
				poly.setLayer(layer);
				poly.setStyle(Poly.Type.FILLED);
				polyList.add(poly);
				pointList.clear();
				lastMoveTo = null;
			} else if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO)
			{
				Point2D pt = new Point2D.Double(coords[0], coords[1]);
				pointList.add(pt);
				if (type == PathIterator.SEG_MOVETO) lastMoveTo = pt;
			}
			pIt.next();
		}
		return polyList;
	}
}
