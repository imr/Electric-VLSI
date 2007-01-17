/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SeaOfGates.java
 * Routing tool: Sea of Gates
 * Written by: Steven M. Rubin
 *
 * Copyright (c) 2007 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.routing;

import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.RTBounds;
import com.sun.electric.database.topology.RTNode;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

/**
 * Class to do sea-of-gates routing.
 */
public class SeaOfGates
{
	private SeaOfGates() {}

	/**
	 * Method to run Sea-of-Gates routing
	 */
	public static void seaOfGatesRoute()
	{
		UserInterface ui = Job.getUserInterface();
		Cell curCell = ui.needCurrentCell();
		if (curCell == null) return;
		new SeaOfGatesJob(curCell);
	}

	private static class SeaOfGatesJob extends Job
	{
		private Cell cell;

		protected SeaOfGatesJob(Cell cell)
		{
			super("Sea-Of-Gates Route", Routing.getRoutingTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			SeaOfGates router = new SeaOfGates();
			router.routeIt(cell);
			return true;
		}

        public void terminateOK()
        {
			UserInterface ui = Job.getUserInterface();
			EditWindow_ wnd = ui.getCurrentEditWindow_();
	        if (wnd != null)
	        {
	        	wnd.clearHighlighting();
	        	wnd.finishedHighlighting();
	        }
        }
	}

	/**
	 * This is the public interface for Sea-of-Gates Routing when done in batch mode.
	 * @param cell the cell to be Sea-of-Gates-routed.
	 */
	public void routeIt(Cell cell)
	{
		System.out.println("Sea-of-Gates router is not working yet");

		Technology curTech = Technology.getCurrent();
		Layer metal1 = curTech.findLayerFromFunction(Layer.Function.METAL1);
		if (metal1 == null)
		{
			System.out.println("Cannot find Metal-1 layer in " + curTech.getTechName() + " technology");
			return;
		}
		RTNode m1Tree = makeRTree(metal1, cell);
	}

	private RTNode makeRTree(Layer layer, Cell cell)
	{
		RTNode root = RTNode.makeTopLevel();
		Visitor visitor = new Visitor(root, layer);
		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, visitor);
		return visitor.getRoot();
	}

	private static class SOGBound implements RTBounds
	{
		private Rectangle2D bound;

		SOGBound(Rectangle2D bound)
		{
			this.bound = bound;
		}
		
		public Rectangle2D getBounds() { return bound; }
	}

	/**
	 * HierarchyEnumerator subclass to examine a cell for a given layer and fill an R-Tree.
	 */
	private class Visitor extends HierarchyEnumerator.Visitor
    {
		private RTNode root;
		private Layer layer;

		public Visitor(RTNode root, Layer layer)
        {
			this.root = root;
			this.layer = layer;
        }

		public RTNode getRoot() { return root; }

		public boolean enterCell(HierarchyEnumerator.CellInfo info) { return true; }

        public void exitCell(HierarchyEnumerator.CellInfo info)
        {
			Cell cell = info.getCell();
			Netlist nl = info.getNetlist();
			AffineTransform trans = info.getTransformToRoot();
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				Technology tech = ai.getProto().getTechnology();
				PolyBase [] polys = tech.getShapeOfArc(ai);
				for(int i=0; i<polys.length; i++)
				{
					PolyBase poly = polys[i];
					if (poly.getLayer() != layer) continue;
					Rectangle2D bounds = poly.getBounds2D();
					GenMath.transformRect(bounds, trans);
					root = RTNode.linkGeom(null, root, new SOGBound(bounds));
				}
			}
        }

        public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
        {
			AffineTransform trans = info.getTransformToRoot();
			NodeInst ni = no.getNodeInst();
			if (!ni.isCellInstance())
			{
				PrimitiveNode pNp = (PrimitiveNode)ni.getProto();
				Technology tech = pNp.getTechnology();
				Poly [] nodeInstPolyList = tech.getShapeOfNode(ni);
				for(int i=0; i<nodeInstPolyList.length; i++)
				{
					Poly poly = nodeInstPolyList[i];
					if (poly.getLayer() != layer) continue;
					Rectangle2D bound = poly.getBounds2D();
					GenMath.transformRect(bound, trans);
					root = RTNode.linkGeom(null, root, new SOGBound(bound));
				}
			}
            return true;
        }
    }


}
