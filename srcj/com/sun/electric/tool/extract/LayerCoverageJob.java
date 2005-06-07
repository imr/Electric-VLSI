/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: LayerCoverageJob.java
* Written by: Gilda Garreton, Sun Microsystems.
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

package com.sun.electric.tool.extract;

import com.sun.electric.database.geometry.*;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.Main;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This utility class is designed for geometric coverage operations.
 * @author  Gilda Garreton
 * @version 0.1
 */
public class LayerCoverageJob extends Job
{
	private Cell curCell;
    private int mode;
	private GeometryHandler tree; // = new PolyQTree(curCell.getBounds());
	public final static int AREA = 0;   // function Layer Coverage
	public final static int MERGE = 1;  // Generic merge polygons function
	public final static int IMPLANT = 2; // Coverage implants
	public final static int NETWORK = 3; // List Geometry on Network function
	private final int function;
	private List deleteList; // Only used for coverage Implants. New coverage implants are pure primitive nodes
	private HashMap originalPolygons = new HashMap(); // Storing initial nodes
	private Highlighter highlighter; // To highlight new implants
	private GeometryOnNetwork geoms;  // Valid only for network job
    private Rectangle2D bBox; // To crop geometry under analysis by given bounding box

    /**
     * Method to calculate area, half-perimeter and ratio of each layer by merging geometries
     * @param cell cell to analyze
     * @param nets networks to analyze
     * @param startJob if job has to run on thread
     * @param mode geometric algorithm to use: GeometryHandler.ALGO_QTREE, GeometryHandler.SWEEP or GeometryHandler.ALGO_MERGE
     * @return
     */
    public static GeometryOnNetwork listGeometryOnNetworks(Cell cell, HashSet nets, boolean startJob, int mode)
    {
	    if (cell == null || nets == null || nets.isEmpty()) return null;
	    double lambda = 1; // lambdaofcell(np);
        // startJob is identical to printable
	    GeometryOnNetwork geoms = new GeometryOnNetwork(cell, nets, lambda, startJob);

		Job job = new LayerCoverageJob(Job.Type.EXAMINE, cell, NETWORK, mode, null, geoms, null);
        if (startJob)
            job.startJob();
        else
            job.doIt();  // Former listGeometryOnNetworksNoJob
	    return geoms;
	}

    private static class LayerVisitor extends HierarchyEnumerator.Visitor
	{
		private GeometryHandler tree;
        private int mode;
		private List deleteList; // Only used for coverage Implants. New coverage implants are pure primitive nodes
		private final int function;
		private HashMap originalPolygons;
		private Set netSet; // For network type, rest is null
        private Rectangle2D origBBox;
        private Area origBBoxArea;   // Area is always in coordinates of top cell

		/**
		 * Determines if function of given layer is applicable for the corresponding operation
		 */
		private static boolean isValidFunction(Layer.Function func, int function)
		{
			switch (function)
			{
                case MERGE:
				case NETWORK:
					return (true);
				case IMPLANT:
					return (func.isSubstrate());
				case AREA:
                    if (Main.LOCALDEBUGFLAG) return (true);
					return (func.isPoly() || func.isMetal());
				default:
					return (false);
			}
		}

		public LayerVisitor(GeometryHandler t, List delList, int func, HashMap original, Set netSet, Rectangle2D bBox)
		{
			this.tree = t;
			this.deleteList = delList;
			this.function = func;
			this.originalPolygons = original;
			this.netSet = netSet;
            this.origBBox = bBox;
            origBBoxArea = (bBox != null) ? new Area(origBBox) : null;

            if (t instanceof PolySweepMerge)
                mode = GeometryHandler.ALGO_SWEEP;
            else if (t instanceof PolyMerge)
               mode = GeometryHandler.ALGO_MERGE;
            else
               mode = GeometryHandler.ALGO_QTREE;
		}

        /**
         * In case of non null bounding box, it will undo the
         * transformation
         * @param info
         */
		public void exitCell(HierarchyEnumerator.CellInfo info)
        {
        }

        private boolean doesIntersectBoundingBox(Rectangle2D rect, HierarchyEnumerator.CellInfo info)
        {
            // Default case when no bouding box is used to crop the geometry
            if (origBBox == null) return true;

            // only because I need to transform the points.
            PolyBase polyRect = new PolyBase(rect);
            // To avoid transformation while traversing the hierarchy
            polyRect.transform(info.getTransformToRoot());
            rect = polyRect.getBounds2D();
            return rect.intersects(origBBox);
        }

		public boolean enterCell(HierarchyEnumerator.CellInfo info)
		{
			Cell curCell = info.getCell();
			Netlist netlist = info.getNetlist();

            // Nothing to visit  CAREFUL WITH TRANSFORMATION IN SUBCELL!!
            if (!doesIntersectBoundingBox(curCell.getBounds(), info))
                return false;

			// Checking if any network is found
            boolean found = (netSet == null);
            for (Iterator it = netlist.getNetworks(); !found && it.hasNext(); )
            {
                Network aNet = (Network)it.next();
                Network parentNet = aNet;
                HierarchyEnumerator.CellInfo cinfo = info;
                boolean netFound = false;
                while ((netFound = netSet.contains(parentNet)) == false && cinfo.getParentInst() != null) {
                    parentNet = cinfo.getNetworkInParent(parentNet);
                    cinfo = cinfo.getParentInfo();
                }
                found = netFound;
            }
            if (!found) return (false);

			// Traversing arcs

			for (Iterator it = curCell.getArcs(); it.hasNext(); )
			{
				ArcInst arc = (ArcInst)it.next();
				int width = netlist.getBusWidth(arc);
                found = (netSet == null);

                for (int i=0; !found && i<width; i++)
                {
                    Network parentNet = netlist.getNetwork(arc, i);
                    HierarchyEnumerator.CellInfo cinfo = info;
                    boolean netFound = false;
                    while ((netFound = netSet.contains(parentNet)) == false && cinfo.getParentInst() != null) {
                        parentNet = cinfo.getNetworkInParent(parentNet);
                        cinfo = cinfo.getParentInfo();
                    }
                found = netFound;
                }
                if (!found) continue; // skipping this arc
				ArcProto arcType = arc.getProto();
				Technology tech = arcType.getTechnology();
				Poly[] polyList = tech.getShapeOfArc(arc);

				// Treating the arcs associated to each node
				// Arcs don't need to be rotated
				for (int i = 0; i < polyList.length; i++)
				{
					Poly poly = polyList[i];
					Layer layer = poly.getLayer();
					Layer.Function func = layer.getFunction();

					boolean value = isValidFunction(func, function);
					if (!value) continue;

					poly.transform(info.getTransformToRoot());

                    // If points are not rounded, in IMPLANT map.containsValue() might not work
                    poly.roundPoints();

                    storeOriginalPolygons(layer, poly);

                    Shape pnode = cropGeometry(poly, origBBoxArea);
                    // empty intersection
                    if (pnode == null) continue;

                    if (mode == GeometryHandler.ALGO_QTREE)
                        pnode = new PolyQTree.PolyNode(poly);

					tree.add(layer, pnode, /*false*/function==NETWORK);  // tmp fix
				}
			}
			return (true);
		}

        /**
         * To store original polygons checked by coverage implant run and then
         * able to determine if new implants should be created.
         * @param layer
         * @param poly
         */
        private void storeOriginalPolygons(Layer layer, PolyBase poly)
        {
            if (function != IMPLANT) return;
            // For coverage implants
            Set polySet = (Set)originalPolygons.get(layer);
            if (polySet == null)
            {
                polySet = new HashSet();
                originalPolygons.put(layer, polySet);
            }
            //map.put(pnode, pnode.clone());
            polySet.add(poly);
        }

        /**
         *
         * @param no
         * @param info
         * @return
         */
		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
		{
			//if (checkAbort()) return false;
			NodeInst node = no.getNodeInst();
			boolean found = (netSet == null);
			NodeProto np = node.getProto();

			// Its like pins, facet-center
			if (NodeInst.isSpecialNode(node)) return (false);

            boolean inside = doesIntersectBoundingBox(node.getBounds(), info);

			// Its a cell
            if (np instanceof Cell) return (inside);

            // Geometry outside contour
            if (!inside) return false;

			for(Iterator pIt = node.getPortInsts(); !found && pIt.hasNext(); )
			{
				PortInst pi = (PortInst)pIt.next();
				PortProto subPP = pi.getPortProto();
				Network oNet = info.getNetlist().getNetwork(node, subPP, 0);
				Network parentNet = oNet;
				HierarchyEnumerator.CellInfo cinfo = info;
				boolean netFound = false;
				while ((netFound = netSet.contains(parentNet)) == false && cinfo.getParentInst() != null) {
					parentNet = cinfo.getNetworkInParent(parentNet);
					cinfo = cinfo.getParentInfo();
				}
				found = netFound;
			}
			if (!found) return (false); // skipping this node

			// Coverage implants are pure primitive nodes
			// and they are ignored.
			if (node.isPrimtiveSubstrateNode()) //node.getFunction() == PrimitiveNode.Function.NODE)
			{
				deleteList.add(node);
				return (false);
			}

			Technology tech = np.getTechnology();
			Poly[] polyList = tech.getShapeOfNode(node);
			AffineTransform transform = node.rotateOut();

			for (int i = 0; i < polyList.length; i++)
			{
				Poly poly = polyList[i];
				Layer layer = poly.getLayer();
				Layer.Function func = layer.getFunction();

				// Only checking poly or metal for AREA case
				boolean value = isValidFunction(func, function);
				if (!value) continue;

				if (poly.getPoints().length < 3)
				{
					// When is this happening?
					continue;
				}

				poly.transform(transform);
				// Not sure if I need this for general merge polygons function
				poly.transform(info.getTransformToRoot());

                // If points are not rounded, in IMPLANT map.containsValue() might not work
                poly.roundPoints();

                storeOriginalPolygons(layer, poly);

                Shape pnode = cropGeometry(poly, origBBoxArea);
                // empty intersection
                if (pnode == null)
                    continue;

                if (mode == GeometryHandler.ALGO_QTREE)
                    pnode = new PolyQTree.PolyNode(pnode);

				tree.add(layer, pnode, /*false*/function==NETWORK);
			}
			return (true);
		}

        /**
         * Method to crop original polygon by given bounding box. If they
         * don't intersect, returns original shape
         * @param origGeom polygon to crop
         * @param bBoxArea area that defines bounding box
         * @return cropped shape
         */
        private static Shape cropGeometry(Shape origGeom, Area bBoxArea)
        {
            Shape pnode = origGeom;

            // exclude area outside bounding box
            if (bBoxArea != null)
            {
                Area tmpA = new Area(pnode);
                tmpA.intersect(bBoxArea);
                // Empty intersection
                if (tmpA.isEmpty()) return null;
                pnode = tmpA;
            }
            return pnode;
        }
	}

	public LayerCoverageJob(Type jobType, Cell cell, int func, int mode, Highlighter highlighter,
                            GeometryOnNetwork geoms, Rectangle2D bBox)
	{
		super("Layer Coverage", User.getUserTool(), jobType, null, null, Priority.USER);
		this.curCell = cell;
        this.mode = mode;
        this.tree = GeometryHandler.createGeometryHandler(mode, curCell.getTechnology().getNumLayers(),
                curCell.getBounds());
//        switch (mode)
//        {
//            case GeometryHandler.ALGO_MERGE:
//                this.tree = new PolyMerge();
//                break;
//            case GeometryHandler.ALGO_QTREE:
//                this.tree = new PolyQTree(curCell.getBounds());
//                break;
//            case GeometryHandler.ALGO_SWEEP:
//                this.tree = new PolySweepMerge();
//                break;
//        }
//        if (mode == GeometryHandler.ALGO_MERGE)
//		this.tree = new PolyQTree(curCell.getBounds());
		this.function = func;
		this.deleteList = new ArrayList(); // should only be used by IMPLANT
		this.highlighter = highlighter;
		this.geoms = geoms; // Valid only for network
        this.bBox = bBox;

		setReportExecutionFlag(true);
	}

	public boolean doIt()
	{
		// enumerate the hierarchy below here
		LayerVisitor visitor = new LayerVisitor(tree, deleteList, function,
                originalPolygons, (geoms != null) ? (geoms.nets) : null, bBox);
		HierarchyEnumerator.enumerateCell(curCell, VarContext.globalContext, NetworkTool.getUserNetlist(curCell), visitor);  
        tree.postProcess(true);

		switch (function)
		{
			case MERGE:
			case IMPLANT:
				{
					// With polygons collected, new geometries are calculated
					if (highlighter != null) highlighter.clear();
					boolean noNewNodes = true;
					boolean isMerge = (function == MERGE);
                    Rectangle2D rect;
                    PolyBase polyB = null;
                    Point2D [] points;

					// Need to detect if geometry was really modified
					for(Iterator it = tree.getKeyIterator(); it.hasNext(); )
					{
						Layer layer = (Layer)it.next();
						Collection set = tree.getObjects(layer, !isMerge, true);
                        Set polySet = (function == IMPLANT) ? (Set)originalPolygons.get(layer) : null;

						// Ready to create new implants.
						for (Iterator i = set.iterator(); i.hasNext(); )
						{
                            if (mode == GeometryHandler.ALGO_QTREE)
                            {
                                PolyQTree.PolyNode qNode = (PolyQTree.PolyNode)i.next();
                                points = qNode.getPoints(false);

                                // One of the original elements
                                if (polySet != null)
                                {
                                    polyB = new PolyBase(points);
                                    polyB.roundPoints();
                                }
                                rect = qNode.getBounds2D();
                            }
                            else
                            {
                                polyB = (PolyBase)i.next();
                                points = polyB.getPoints();
                                rect = polyB.getBounds2D();
                            }

                            // One of the original elements
                            if (polySet != null)
                            {
                                Object[] array = polySet.toArray();
                                boolean foundOrigPoly = false;
                                for (int j = 0; j < array.length; j++)
                                {
                                    foundOrigPoly = polyB.polySame((PolyBase)array[j]);
                                    if (foundOrigPoly)
                                        break;
                                }
                                if (foundOrigPoly)
                                    continue;
                            }

							Point2D center = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
							PrimitiveNode priNode = layer.getPureLayerNode();
							// Adding the new implant. New implant not assigned to any local variable                                .
							NodeInst node = NodeInst.makeInstance(priNode, center, rect.getWidth(), rect.getHeight(), curCell);
							if (highlighter != null)
								highlighter.addElectricObject(node, curCell);

							if (isMerge)
							{
								node.newVar(NodeInst.TRACE, points);
							}
							else
							{
								// New implant can't be selected again
								node.setHardSelect();
							}
							noNewNodes = false;
						}
					}
					if (highlighter != null) highlighter.finished();
					for (Iterator it = deleteList.iterator(); it.hasNext(); )
					{
						NodeInst node = (NodeInst)it .next();
						node.kill();
					}
					if (noNewNodes)
						System.out.println("No new areas added");
				}
				break;
            case AREA:
			case NETWORK:
				{
			        double lambdaSqr = 1; // lambdaofcell(np);
                    Rectangle2D bbox = curCell.getBounds();
					double totalArea =  (bbox.getHeight()*bbox.getWidth())/lambdaSqr;
					// Traversing tree with merged geometry and sorting layers per name first
				    List list = new ArrayList(tree.getKeySet());
				    Collections.sort(list, Layer.layerSort);

					for (Iterator it = list.iterator(); it.hasNext(); )
					{
						Layer layer = (Layer)it.next();
						Collection set = tree.getObjects(layer, false, true);
						double layerArea = 0;
						double perimeter = 0;

						// Get all objects and sum the area
						for (Iterator i = set.iterator(); i.hasNext(); )
						{
                            if (mode == GeometryHandler.ALGO_QTREE)
                            {
                                PolyQTree.PolyNode area = (PolyQTree.PolyNode)i.next();
                                layerArea += area.getArea();
							    perimeter += area.getPerimeter();
                            }
                            else
                            {
                                PolyBase poly = (PolyBase)i.next();
                                layerArea += poly.getArea();
							    perimeter += poly.getPerimeter();
                            }
						}
						layerArea /= lambdaSqr;
						perimeter /= 2;

                        if (geoms != null)
						    geoms.addLayer(layer, layerArea, perimeter);
                        else
                            System.out.println("Layer " + layer.getName() + " covers " + TextUtils.formatDouble(layerArea) + " square lambda (" + TextUtils.formatDouble((layerArea/totalArea)*100, 0) + "%)");
					}
                    if (geoms != null)
					    geoms.print();
                    else
                        System.out.println("Cell is " + TextUtils.formatDouble(totalArea, 2) + " square lambda");
                }
				break;
			default:
				System.out.println("Error in LayerCoverageJob: function not implemented");
		}
		return true;
	}

	public static class GeometryOnNetwork {
	    public final Cell cell;
	    protected Set nets;
	    private double lambda;
		private boolean printable;

	    // these lists tie together a layer, its area, and its half-perimeter
	    private ArrayList layers;
	    private ArrayList areas;
	    private ArrayList halfPerimeters;
	    private double totalWire;

	    protected GeometryOnNetwork(Cell cell, Set nets, double lambda, boolean printable) {
	        this.cell = cell;
	        this.nets = nets;
	        this.lambda = lambda;
	        layers = new ArrayList();
	        areas = new ArrayList();
	        halfPerimeters = new ArrayList();
		    this.printable = printable;
	        totalWire = 0;
	    }
	    public double getTotalWireLength() { return totalWire; }

	    private void addLayer(Layer layer, double area, double halfperimeter) {
	        layers.add(layer);
	        areas.add(new Double(area));
	        halfPerimeters.add(new Double(halfperimeter));

	        Layer.Function func = layer.getFunction();
	        /* accumulate total wire length on all metal/poly layers */
	        if (func.isPoly() && !func.isGatePoly() || func.isMetal()) {
	            totalWire += halfperimeter;
	        }
	    }

        /**
         * Method to analyze amount of area covered by layer and if meets the minimum
         * specified
         * @param bbox
         * @param errorLogger
         * @return true if no errors are found
         */
        public boolean analyzeCoverage(Rectangle2D bbox, ErrorLogger errorLogger)
        {
            double totalArea =  (bbox.getHeight()*bbox.getWidth())/lambda;
            boolean foundError = false;

            for (int i = 0; i < layers.size(); i++)
            {
                Layer layer = (Layer)layers.get(i);
                Double area = (Double)areas.get(i);
                double percentage = area.doubleValue()/totalArea * 100;
                double minV = layer.getAreaCoverage();
                if (percentage < minV)
                {
                    String msg = "Error area coverage " + layer.getName() + " min value = " + minV + " actual value = " + percentage;
                    ErrorLogger.MessageLog err = errorLogger.logError(msg, cell, layer.getIndex());
                    PolyBase poly = new PolyBase(bbox);
                    err.addPoly(poly, true, cell);
                    foundError = true;
                }
            }
            return foundError;
        }

	    public void print() {
		    // Doesn't print information
		    if (!printable) return;

            // nets is null for mode=AREA
            if (nets != null)
            {
                for(Iterator it = nets.iterator(); it.hasNext(); )
                {
                    Network net = (Network)it.next();
                    System.out.println("For network '" + net.describe() + "' in cell '" + cell.describe() + "':");
                }
            }
	        for (int i=0; i<layers.size(); i++) {
	            Layer layer = (Layer)layers.get(i);
	            Double area = (Double)areas.get(i);
	            Double halfperim = (Double)halfPerimeters.get(i);

	            System.out.println("\tLayer " + layer.getName()
	                    + ":\t area " + TextUtils.formatDouble(area.doubleValue())
	                    + "\t half-perimeter " + TextUtils.formatDouble(halfperim.doubleValue())
	                    + "\t ratio " + TextUtils.formatDouble(area.doubleValue()/halfperim.doubleValue()));
	        }
	        if (totalWire > 0)
	            System.out.println("Total wire length = " + TextUtils.formatDouble(totalWire/lambda));
	    }
	}
}
