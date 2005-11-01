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
package com.sun.electric.tool.user.tecEdit;

import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.technologies.Generic;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;

/**
 * This class defines graphical node and arc examples during conversion of libraries to technologies.
 */
public class Example
{
	List<Sample> samples;				/* head of list of samples in example */
	Sample       studySample;			/* sample under analysis */
	double       lx, hx, ly, hy;		/* bounding box of example */
	Example      nextExample;			/* next example in list */

	/**
	 * Method to parse the node examples in cell "np" and return a list of
	 * EXAMPLEs (one per example).  "isNode" is true if this is a node
	 * being examined.  Returns NOEXAMPLE on error.
	 */
	static Example getExamples(Cell np, boolean isNode)
	{
		HashMap<NodeInst,Object> nodeExamples = new HashMap<NodeInst,Object>();
		for(Iterator<NodeInst> it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();

			// ignore special nodes with function information
			int funct = Manipulate.getOptionOnNode(ni);
			if (funct != Info.LAYERPATCH && funct != Info.PORTOBJ && funct != Info.HIGHLIGHTOBJ)
			{
				nodeExamples.put(ni, new Integer(0));
			}
		}

		Example neList = null;
		for(Iterator<NodeInst> it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (nodeExamples.get(ni) != null) continue;

			// get a new cluster of nodes
			Example ne = new Example();
			ne.samples = new ArrayList<Sample>();
			ne.nextExample = neList;
			neList = ne;

			SizeOffset so = ni.getSizeOffset();
			Poly poly = new Poly(ni.getAnchorCenterX(), ni.getAnchorCenterY(),
				ni.getXSize() - so.getLowXOffset() - so.getHighXOffset(),
				ni.getYSize() - so.getLowYOffset() - so.getHighYOffset());
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
				for(Iterator<Geometric> oIt = np.searchIterator(soFar); oIt.hasNext(); )
				{
					Geometric geom = (Geometric)oIt.next();
					if (geom == null) break;
					if (!(geom instanceof NodeInst)) continue;
					NodeInst otherNi = (NodeInst)geom;
					SizeOffset oSo = otherNi.getSizeOffset();
					Poly oPoly = new Poly(otherNi.getAnchorCenterX(), otherNi.getAnchorCenterY(),
						otherNi.getXSize() - oSo.getLowXOffset() - oSo.getHighXOffset(),
						otherNi.getYSize() - oSo.getLowYOffset() - oSo.getHighYOffset());
					oPoly.transform(otherNi.rotateOut());
					Rectangle2D otherRect = oPoly.getBounds2D();
					if (!GenMath.rectsIntersect(otherRect, soFar)) continue;
					// make sure the node is valid
					Object otherAssn = nodeExamples.get(otherNi);
					if (otherAssn != null)
					{
						if (otherAssn instanceof Integer) continue;
						if ((Example)otherAssn == ne) continue;
						LibToTech.pointOutError(otherNi, np);
						System.out.println("Examples are too close in " + np);
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
								LibToTech.pointOutError(otherNi, np);
								System.out.println(np + " cannot have ports.  Delete this");
								return null;
							}
							ns.layer = Generic.tech.portNode;
							break;
						case Info.CENTEROBJ:
							if (!isNode)
							{
								LibToTech.pointOutError(otherNi, np);
								System.out.println(np + " cannot have a grab point.  Delete this");
								return null;
							}
							ns.layer = Generic.tech.cellCenterNode;
							break;
						case Info.HIGHLIGHTOBJ:
							hCount++;
							break;
						default:
							ns.layer = Manipulate.getLayerCell(otherNi);
							if (ns.layer == null)
							{
								LibToTech.pointOutError(otherNi, np);
								System.out.println("No layer information on " + otherNi + " in " + np);
								return null;
							}
							break;
					}

					// accumulate state if this is not a "grab point" mark
					if (otherNi.getProto() != Generic.tech.cellCenterNode)
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
				LibToTech.pointOutError(null, np);
				System.out.println("No highlight layer in " + np + " example");
				return null;
			}
			if (hCount != 1)
			{
				LibToTech.pointOutError(null, np);
				System.out.println("Too many highlight layers in " + np + " example.  Delete some");
				return null;
			}
		}
		if (neList == null)
		{
			LibToTech.pointOutError(null, np);
			System.out.println("No examples found in " + np);
			return neList;
		}

		/*
		 * now search the list for the smallest, most upper-right example
		 * (the "main" example)
		 */
		double sizeX = neList.hx - neList.lx;
		double sizeY = neList.hy - neList.ly;
		double locX = (neList.lx + neList.hx) / 2;
		double locY = (neList.ly + neList.hy) / 2;
		Example bestNe = neList;
		for(Example ne = neList; ne != null; ne = ne.nextExample)
		{
			double newSize = ne.hx-ne.lx;
			newSize *= ne.hy-ne.ly;
			if (newSize > sizeX*sizeY) continue;
			if (newSize == sizeX*sizeY && (ne.lx+ne.hx)/2 >= locX && (ne.ly+ne.hy)/2 <= locY)
				continue;
			sizeX = ne.hx - ne.lx;
			sizeY = ne.hy - ne.ly;
			locX = (ne.lx + ne.hx) / 2;
			locY = (ne.ly + ne.hy) / 2;
			bestNe = ne;
		}

		// place the main example at the top of the list
		if (bestNe != neList)
		{
			for(Example ne = neList; ne != null; ne = ne.nextExample)
				if (ne.nextExample == bestNe)
			{
				ne.nextExample = bestNe.nextExample;
				break;
			}
			bestNe.nextExample = neList;
			neList = bestNe;
		}

		// done
		return neList;
	}
};
