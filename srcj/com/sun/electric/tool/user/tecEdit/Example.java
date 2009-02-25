/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Example.java
 * Technology Editor, helper class during conversion of libraries to technologies
 * Written by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.user.tecEdit;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.RTBounds;
import com.sun.electric.technology.technologies.Generic;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class defines graphical node and arc examples during conversion of libraries to technologies.
 */
public class Example implements Serializable
{
	/** head of list of samples in example */	List<Sample> samples;
	/** sample under analysis */				Sample       studySample;
	/** bounding box of example */				double       lx, hx, ly, hy;

	public Example()
	{
		samples = new ArrayList<Sample>();
	}

	/**
	 * Method to parse the node examples in cell "np" and return a list of
	 * EXAMPLEs (one per example).  "isNode" is true if this is a node
	 * being examined.  Returns NOEXAMPLE on error.
	 */
	public static List<Example> getExamples(Cell np, boolean isNode, TechConversionResult tcr, List<Example> variations)
	{
		Map<NodeInst,Object> nodeExamples = new HashMap<NodeInst,Object>();
		for(Iterator<NodeInst> it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();

			// ignore special nodes with function information
			int funct = Manipulate.getOptionOnNode(ni);
			if (funct != Info.LAYERPATCH && funct != Info.PORTOBJ && funct != Info.HIGHLIGHTOBJ)
			{
				nodeExamples.put(ni, new Integer(0));
			}
		}

		List<Example> neList = new ArrayList<Example>();
		for(Iterator<NodeInst> it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (nodeExamples.get(ni) != null) continue;

			// get a new cluster of nodes
			Example ne = new Example();
			neList.add(ne);

			Poly poly = new Poly(ni.getAnchorCenterX(), ni.getAnchorCenterY(),
				ni.getLambdaBaseXSize(), ni.getLambdaBaseYSize());
			poly.transform(ni.rotateOut());
			Rectangle2D soFar = poly.getBounds2D();

			// now find all others that touch this area
			boolean gotBBox = false;
			boolean foundOne = true;
			int hCount = 0;
			while (foundOne)
			{
				foundOne = false;

				// begin to search the area so far
                List<NodeInst> sortedNodes = new ArrayList<NodeInst>();
				for(Iterator<RTBounds> oIt = np.searchIterator(soFar); oIt.hasNext(); )
				{
					RTBounds geom = oIt.next();
					if (geom == null) break;
					if (geom instanceof NodeInst)
                    	sortedNodes.add((NodeInst)geom);
                }
				for(NodeInst otherNi: sortedNodes)
				{
					Poly oPoly = new Poly(otherNi.getAnchorCenterX(), otherNi.getAnchorCenterY(),
						otherNi.getLambdaBaseXSize(), otherNi.getLambdaBaseYSize());
					oPoly.transform(otherNi.rotateOut());
					Rectangle2D otherRect = oPoly.getBounds2D();
					if (!DBMath.rectsIntersect(otherRect, soFar)) continue;

					// make sure the node is valid
					Object otherAssn = nodeExamples.get(otherNi);
					if (otherAssn != null)
					{
						if (otherAssn instanceof Integer) continue;
						if ((Example)otherAssn == ne) continue;
						String error = "Examples are too close.  Found " + neList.size() + " examples at:";
						for(Example nee : neList)
						{
							error += " [" + TextUtils.formatDistance(nee.lx) + "<=X<=" + TextUtils.formatDistance(nee.hx) +
								" and " + TextUtils.formatDistance(nee.ly) + "<=Y<=" + TextUtils.formatDistance(nee.hy) + "]";
						}
						tcr.markError(otherNi, np, error);
						return null;
					}
					nodeExamples.put(otherNi, ne);
					// add it to the cluster
					Sample ns = new Sample();
					ns.node = otherNi;
					ns.values = null;
					ns.msg = null;
					ns.parent = ne;
					ne.samples.add(ns);
					ns.assoc = null;
					ns.xPos = otherRect.getCenterX();
					ns.yPos = otherRect.getCenterY();
					int funct = Manipulate.getOptionOnNode(otherNi);
					switch (funct)
					{
						case Info.PORTOBJ:
							if (!isNode)
							{
								tcr.markError(otherNi, np, "Ports can only exist in nodes");
								return null;
							}
							ns.layer = Generic.tech().portNode;
							break;
						case Info.CENTEROBJ:
							if (!isNode)
							{
								tcr.markError(otherNi, np, "Grab points can only exist in nodes");
								return null;
							}
							ns.layer = Generic.tech().cellCenterNode;
							break;
						case Info.HIGHLIGHTOBJ:
							hCount++;
							break;
						default:
							ns.layer = Manipulate.getLayerCell(otherNi);
							if (ns.layer == null)
							{
                                Manipulate.getLayerCell(otherNi);
                                tcr.markError(otherNi, np, "Node has no layer information");
								return null;
							}
							break;
					}

					// accumulate state if this is not a "grab point" mark
					if (otherNi.getProto() != Generic.tech().cellCenterNode)
					{
						if (!gotBBox)
						{
							ne.lx = otherRect.getMinX();   ne.hx = otherRect.getMaxX();
							ne.ly = otherRect.getMinY();   ne.hy = otherRect.getMaxY();
							gotBBox = true;
						} else
						{
							if (otherRect.getMinX() < ne.lx) ne.lx = otherRect.getMinX();
							if (otherRect.getMaxX() > ne.hx) ne.hx = otherRect.getMaxX();
							if (otherRect.getMinY() < ne.ly) ne.ly = otherRect.getMinY();
							if (otherRect.getMaxY() > ne.hy) ne.hy = otherRect.getMaxY();
						}
						soFar.setRect(ne.lx, ne.ly, ne.hx-ne.lx, ne.hy-ne.ly);
					}
					foundOne = true;
				}
			}
			if (hCount == 0)
			{
				tcr.markError(null, np, "No highlight layer found");
				return null;
			}
			if (hCount != 1)
			{
				tcr.markError(null, np, "Too many highlight layers found");
				return null;
			}
		}
		if (neList == null)
		{
			tcr.markError(null, np, "No examples found");
			return neList;
		}

		// put variations in a separate list
		if (variations != null)
		{
			for(int i=0; i<neList.size(); i++)
			{
				Example e = neList.get(i);
				for(Sample s : e.samples)
				{
					if (!s.node.getNameKey().isTempname())
					{
						// named a layer: this is a variation
						variations.add(e);
						neList.remove(i);
						i--;
						break;
					}
				}
			}
			if (neList.size() == 0 && variations.size() > 0)
			{
				tcr.markError(null, np, "All examples have text on them...text should be used only in variations");
				return neList;
			}
		}

		// now search the list for the smallest, most upper-left example (the "main" example)
		double sizeX = 0;
		double sizeY = 0;
		double locX = 0;
		double locY = 0;
		Example bestNe = null;
		for(Example ne : neList)
		{
			double newSize = ne.hx-ne.lx;
			newSize *= ne.hy-ne.ly;
			if (bestNe != null)
			{
				if (newSize > sizeX*sizeY) continue;
				if (newSize == sizeX*sizeY && (ne.lx+ne.hx)/2 >= locX && (ne.ly+ne.hy)/2 <= locY)
					continue;
			}
			sizeX = ne.hx - ne.lx;
			sizeY = ne.hy - ne.ly;
			locX = (ne.lx + ne.hx) / 2;
			locY = (ne.ly + ne.hy) / 2;
			bestNe = ne;
		}

		// place the main example at the top of the list
		if (bestNe != null && bestNe != neList)
		{
			neList.remove(bestNe);
			neList.add(0, bestNe);
		}

		// done
		return neList;
	}
}
