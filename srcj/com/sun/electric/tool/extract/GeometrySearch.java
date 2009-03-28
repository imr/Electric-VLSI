/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GeometrySearch.java
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
package com.sun.electric.tool.extract;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.RTBounds;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.user.Highlighter;

import com.sun.electric.tool.user.ui.LayerVisibility;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class to search hierarchically at a point and return all objects found.
 */
public class GeometrySearch extends HierarchyEnumerator.Visitor
{
    private List<GeometrySearchResult> found;
    private ERectangle geomBBnd;
    private boolean visibleObjectsOnly;
    private LayerVisibility lv;
    private int cellsProcessed;         // for debug

    /**
     * Class that holds results of the search (a packaged Geometric and VarContext).
     */
	public static class GeometrySearchResult
	{
		private Geometric geom;
		private VarContext context;

		GeometrySearchResult(Geometric g, VarContext c)
		{
			geom = g;
			context = c;
		}

		public Geometric getGeometric() { return geom; }

		public VarContext getContext() { return context; }

		public String describe()
	    {
	        String contextstr = "current cell";
	        if (context != VarContext.globalContext) contextstr = getInstPath(context);
	        return geom + " in " + contextstr;
	    }

		/**
		 * Return the concatenation of all instances names left to right
	     * from the root to the leaf. Begin with the string with a separator
	     * and place a separator between adjacent instance names.
	     * @param vc the context of the search.
	     */
	    public String getInstPath(VarContext vc)
	    {
	        if (vc == VarContext.globalContext) return "";
	        String prefix = vc.pop()==VarContext.globalContext ? "" : getInstPath(vc.pop());
	        Nodable no = vc.getNodable();
	        if (no == null)
	        {
	            System.out.println("VarContext.getInstPath: context with null NodeInst?");
	        }
	        String me = no.getName();
	        if (no instanceof NodeInst)
	        {
	            // nodeInst, we want netlisted name, assume zero index of arrayed node
	            Name name = no.getNameKey();
	            me = name.subname(0).toString();
	        }
	        me = no.getProto().getName() + "[" + me + "]";
	        if (prefix.equals("")) return me;
	        return prefix + " / " + me;
	    }
	}

    public GeometrySearch(LayerVisibility lv)
    {
        this.lv = lv;
    }

    /**
     * Find a Primitive Node or Arc at a point in a cell.  The geometric found may exist down
     * the hierarchy from the given cell.
     * @param cell the cell in which the point resides
     * @param point a point to search under
     * @param visibleObjectsOnly true to consider only Geometries that are visible
     * @return a List of all search results
     */
    public List<GeometrySearchResult> searchGeometries(Cell cell, EPoint point, boolean visibleObjectsOnly)
    {
    	found = new ArrayList<GeometrySearchResult>();
        geomBBnd = ERectangle.fromLambda(point.getX(), point.getY(), 0, 0);
        this.visibleObjectsOnly = visibleObjectsOnly;
//    	PrimitiveNode.resetAllVisibility();
        cellsProcessed = 0;
        HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, this);
        return found;
    }

    public int getCellsProcessed() { return cellsProcessed; }

    /**************************************************************************************************************
     *  Enumerator class
     **************************************************************************************************************/

    public boolean enterCell(HierarchyEnumerator.CellInfo info)
    {
        Cell cell = info.getCell();
        AffineTransform xformToRoot = null;
        try
        {
            xformToRoot = info.getTransformToRoot().createInverse();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        assert(xformToRoot!=null);
        Rectangle2D rect = new Rectangle2D.Double();
        rect.setRect(geomBBnd);
        DBMath.transformRect(rect, xformToRoot);
        cellsProcessed++;

        boolean continueDown = false;
        for(Iterator<RTBounds> it = cell.searchIterator(rect, false); it.hasNext(); )
        {
            Geometric geom = (Geometric)it.next();

            // PrimitiveInst or Cell
            if (geom instanceof NodeInst)
            {
                NodeInst oNi = (NodeInst)geom;
                if (oNi.isCellInstance())
                {
                    // keep searching
                    continueDown = true;
                } else
                {
                    // primitive found, ignore nodes that are fully invisible
                	double dist = Highlighter.distToNode(rect, oNi, null);
                	if (dist > 0) continue;
                    PrimitiveNode node = (PrimitiveNode)oNi.getProto();
                    if (visibleObjectsOnly && !lv.isVisible(node)) continue;
                    found.add(new GeometrySearchResult(geom, info.getContext()));
                }
            } else
            {
                // arc, ignore arcs that and fully invisible
            	ArcInst ai = (ArcInst)geom;
            	double dist = Highlighter.distToArc(rect, ai, null);
            	if (dist > 0) continue;
                ArcProto ap = ai.getProto();
                if (visibleObjectsOnly && !lv.isVisible(ap)) continue;
                found.add(new GeometrySearchResult(geom, info.getContext()));
            }
        }
        return continueDown;
    }

    public void exitCell(HierarchyEnumerator.CellInfo info) {}

    public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
    {
        if (visibleObjectsOnly && !no.getNodeInst().isExpanded()) return false;
        return true;
    }
}
