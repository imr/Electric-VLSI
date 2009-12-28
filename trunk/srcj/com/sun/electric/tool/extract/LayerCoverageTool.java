/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayerCoverageTool.java
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
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.id.LayerId;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.PrefPackage;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.TransistorSize;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.routing.SeaOfGates;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * Class to describe coverage percentage for a layer.
 */
public class LayerCoverageTool extends Tool
{
    /** the LayerCoverageTool tool. */		protected static LayerCoverageTool tool = new LayerCoverageTool();

    /**
	 * The constructor sets up the DRC tool.
	 */
	private LayerCoverageTool()
	{
		super("coverage");
	}

    /**
     * Method to retrieve the singleton associated with the LayerCoverageTool tool.
     * @return the LayerCoverageTool tool.
     */
    public static LayerCoverageTool getLayerCoverageTool() { return tool; }

    /****************************** OPTIONS ******************************/

    public static class LayerCoveragePreferences extends PrefPackage {
        public static final double DEFAULT_AREA_COVERAGE = 10; // 10%
        // Default value is in um to be technology independent
        private static final double defaultSize = 50000;
        private static final String EXTRACT_NODE = "tool/extract";
        private static final String KEY_COVERAGE = "AreaCoverageJobOf";
        private final transient TechPool techPool;

        public Map<LayerId,Double> areaCoverage = new HashMap<LayerId,Double>();

        /** User preference for deltaX. The default is 50 mm. */
        @DoublePref(node = EXTRACT_NODE, key = "DeltaX", factory = defaultSize)
        public double deltaXInMicrons;

        /** User preference for deltaY. The default is 50 mm. */
        @DoublePref(node = EXTRACT_NODE, key = "DeltaY", factory = defaultSize)
        public double deltaYInMicrons;

        /** User preference for width of the bounding box. The default is 50 mm. */
        @DoublePref(node = EXTRACT_NODE, key = "Width", factory = defaultSize)
        public double widthInMicrons;

        /** User preference for height of the bounding box. The default is 50 mm. */
        @DoublePref(node = EXTRACT_NODE, key = "Height", factory = defaultSize)
        public double heightInMicrons;

        /** Preference for SeaOfGate if used */
        public SeaOfGates.SeaOfGatesOptions seaIfGatesPrefs;

        public LayerCoveragePreferences(boolean factory) {
            this(factory, TechPool.getThreadTechPool());
            seaIfGatesPrefs = new SeaOfGates.SeaOfGatesOptions();
            if (!factory) seaIfGatesPrefs.getOptionsFromPreferences();
        }

        private LayerCoveragePreferences(boolean factory, TechPool techPool)
        {
            super(factory);
            this.techPool = techPool;
            if (factory) return;

            Preferences techPrefs = getPrefRoot().node(TECH_NODE);
            for (Technology tech: techPool.values()) {
                for (Iterator<Layer> it = tech.getLayers(); it.hasNext(); ) {
                    Layer layer = it.next();
                    LayerId layerId = layer.getId();
                    double factoryValue = DEFAULT_AREA_COVERAGE;
                    double value = techPrefs.getDouble(getKey(KEY_COVERAGE, layerId), factoryValue);
                    if (value == factoryValue) continue;
                    areaCoverage.put(layerId, Double.valueOf(value));
                }
            }
        }

        /**
         * Store annotated option fields of the subclass into the speciefied Preferences subtree.
         * @param prefRoot the root of the Preferences subtree.
         * @param removeDefaults remove from the Preferences subtree options which have factory default value.
         */
        @Override
        public void putPrefs(Preferences prefRoot, boolean removeDefaults) {
            super.putPrefs(prefRoot, removeDefaults);
            Preferences techPrefs = prefRoot.node(TECH_NODE);
            for (Technology tech: techPool.values()) {
                for (Iterator<Layer> it = tech.getLayers(); it.hasNext(); ) {
                    Layer layer = it.next();
                    LayerId layerId = layer.getId();
                    String key = getKey(KEY_COVERAGE, layerId);
                    double factoryValue = DEFAULT_AREA_COVERAGE;
                    Double valueObj = areaCoverage.get(layerId);
                    double value = valueObj != null ? valueObj.doubleValue() : factoryValue;
                    if (removeDefaults && value == factoryValue)
                        techPrefs.remove(key);
                    else
                        techPrefs.putDouble(key, value);
                }
            }
        }

        /**
         * Method to return the minimum area coverage that the layer must reach in the technology.
         * @return the minimum area coverage (in percentage).
         */
        public double getAreaCoverage(Layer layer) {
            Double valueObj = areaCoverage.get(layer.getId());
            return valueObj != null ? valueObj.doubleValue() : DEFAULT_AREA_COVERAGE;
        }
        /**
         * Methot to set minimum area coverage that the layer must reach in the technology.
         * @param layer Layer
         * @param area the minimum area coverage (in percentage).
         */
        public void setAreaCoverageInfo(Layer layer, double area) {
            if (area == DEFAULT_AREA_COVERAGE)
                areaCoverage.remove(layer.getId());
            else
                areaCoverage.put(layer.getId(), Double.valueOf(area));
        }

        public void reset() {
            areaCoverage.clear();
            deltaXInMicrons = deltaYInMicrons = widthInMicrons = heightInMicrons = defaultSize;
        }
    }

    /**
     * Method to handle the "List Layer Coverage", "Coverage Implant Generator",  polygons merge
     * except "List Geometry on Network" commands.
     */
    public static List<Object> layerCoverageCommand(LCMode func, GeometryHandler.GHMode mode, Cell curCell, boolean startJob, LayerCoveragePreferences lcp)
    {
        // Must be change job for merge and implant;
        Job.Type jobType = (func == LayerCoverageTool.LCMode.MERGE || func == LayerCoverageTool.LCMode.IMPLANT) ?
                Job.Type.CHANGE : Job.Type.SERVER_EXAMINE;
        LayerCoverageJob job = new LayerCoverageJob(curCell, jobType, func, mode, null, null, lcp);
        if (startJob)
            job.startJob();
        else
        {
            try
            {
                job.doIt();
            }
            catch (Exception e) {}
            return job.nodesAdded;
        }
        return null;
    }

    /**
     * Method to kick area coverage per layer in a cell. It has to be public due to regressions.
     * @param cell
     * @param mode
     * @param startJob to determine if job has to run in a separate thread
     * @return true if job runs without errors. Only valid if startJob is false (regression purpose)
     */
    public static Map<Layer,Double> layerCoverageCommand(Cell cell, GeometryHandler.GHMode mode, boolean startJob, LayerCoveragePreferences lcp)
    {
        if (cell == null) return null;

        double techScale = cell.getTechnology().getScale();
        double width = lcp.widthInMicrons/techScale;
        double height = lcp.heightInMicrons/techScale;
        double deltaX = lcp.deltaXInMicrons/techScale;
        double deltaY = lcp.deltaYInMicrons/techScale;

        // Reset values to cell bounding box if area is bigger than the actual cell
        Rectangle2D bbox = cell.getBounds();
        if (width > bbox.getWidth()) width = bbox.getWidth();
        if (height > bbox.getHeight()) height = bbox.getHeight();
        Map<Layer,Double> map = null;
        AreaCoverageJob job = new AreaCoverageJob(cell, mode, width, height, deltaX, deltaY, lcp);

        // No regression
        if (startJob)
            job.startJob();
        else
        {
        	try
        	{
        		job.doIt();
                map = job.getDataInfo();
        	} catch (JobException e)
        	{
        	}
        }
        return (map);
    }

    /**
     * Method to extract bounding box for a particular Network/Layer
     * @param exportCell
     * @return Rectangle2D containing the bounding box for a particular Network/Layer
     */
    public static Rectangle2D getGeometryOnNetwork(Cell exportCell, PortInst pi, Layer layer, LayerCoverageTool.LayerCoveragePreferences lcp)
    {
        Netlist netlist = exportCell.getNetlist();
        Network net = netlist.getNetwork(pi);
        Set<Network> nets = new HashSet<Network>();
        nets.add(net);
        GeometryOnNetwork geoms = new GeometryOnNetwork(exportCell, nets, 1.0, false, layer, lcp);
        // This assumes that pi.getBounds() alywas gives you a degenerated rectangle (zero area) so
        // only a point should be searched.
        Rectangle2D bnd = pi.getBounds();
		LayerCoverageJob job = new LayerCoverageJob(exportCell, Job.Type.SERVER_EXAMINE, LCMode.NETWORK,
                GeometryHandler.GHMode.ALGO_SWEEP, geoms,
                new Point2D.Double(bnd.getX(), bnd.getY()), lcp);

        // Must run it now
        try
        {
            job.doIt();  // Former listGeometryOnNetworksNoJob
        } catch (JobException e)
        {
            e.printStackTrace();
        }
        List<Object> list = job.nodesAdded;
        // Don't know what to do if there is more than one
        Rectangle2D rect = null;
        if (list.size() != 1)
            System.out.println("Problem here");
//        assert(list.size() == 1);
        else
        {
            PolyBase poly = (PolyBase)list.toArray()[0];
            rect = poly.getBounds2D();
        }
        return rect;
    }

    /**
     * Method to calculate area, half-perimeter and ratio of each layer by merging geometries
     * @param cell cell to analyze
     * @param nets networks to analyze
     * @param startJob if job has to run on thread
     * @param mode geometric algorithm to use: GeometryHandler.ALGO_QTREE, GeometryHandler.SWEEP or GeometryHandler.ALGO_MERGE
     * @param lcp LayerCoveragePreferences
     */
    public static GeometryOnNetwork listGeometryOnNetworks(Cell cell, Set<Network> nets, boolean startJob,
                                                           GeometryHandler.GHMode mode, LayerCoveragePreferences lcp)
    {
	    if (cell == null || nets == null || nets.isEmpty()) return null;
	    double lambda = 1; // lambdaofcell(np);
        // startJob is identical to printable
	    GeometryOnNetwork geoms = new GeometryOnNetwork(cell, nets, lambda, startJob, null, lcp);
		Job job = new LayerCoverageJob(cell, Job.Type.SERVER_EXAMINE, LCMode.NETWORK, mode, geoms, null, lcp);

        if (startJob)
            job.startJob();
        else
        {
        	try
        	{
        		job.doIt();  // Former listGeometryOnNetworksNoJob
        	} catch (JobException e)
        	{
        	}
        }
	    return geoms;
	}

    /************************************************************************
     * LayerCoverageData Class
     ************************************************************************/
    private static class LayerCoverageData
    {
        private Cell curCell;
        private Job parentJob; // to stop if parent job is killed
        private LCMode function;
        private GeometryHandler.GHMode mode;
        private GeometryOnNetwork geoms;  // Valid only for network job
        private Rectangle2D bBox; // To crop geometry under analysis by given bounding box
        private List<Object> nodesToExamine = new ArrayList<Object>(); // for implant
        private Point2D overlapPoint; // to detect if obtained geometry is connected to original request,
        // Network in fill generator

        LayerCoverageData(Job parentJob, Cell cell, LCMode func, GeometryHandler.GHMode mode,
                          GeometryOnNetwork geoms, Rectangle2D bBox, Point2D overlapPoint, LayerCoverageTool.LayerCoveragePreferences lcp)
        {
            this.parentJob = parentJob;
            this.curCell = cell;
            this.mode = mode;
            this.function = func;
            this.geoms = geoms; // Valid only for network
            this.bBox = bBox;
            this.overlapPoint = overlapPoint;

            if (func == LCMode.AREA && this.geoms == null)
                this.geoms = new GeometryOnNetwork(curCell, null, 1, true, null, lcp);
        }

        List<Object> getNodesToHighlight() { return nodesToExamine; }

        boolean doIt()
        {
            GeometryHandler tree = GeometryHandler.createGeometryHandler(mode, curCell.getTechnology().getNumLayers());
            Map<Layer,Set<PolyBase>> originalPolygons = new HashMap<Layer,Set<PolyBase>>(); // Storing initial nodes
            Set<NodeInst> nodesToDelete = new HashSet<NodeInst>(); // should only be used by IMPLANT
            Set<Network> netSet = null;
            Layer onlyThisLayer = null;

            Netlist.ShortResistors shortResistors = Netlist.ShortResistors.NO;
            if (geoms != null)
            {
                netSet = geoms.nets;
                if (geoms.nets != null && !geoms.nets.isEmpty()) {
                    Iterator<Network> nIt = geoms.nets.iterator();
                    shortResistors = nIt.next().getNetlist().getShortResistors();
                    while (nIt.hasNext()) {
                        Netlist.ShortResistors sh = nIt.next().getNetlist().getShortResistors();
                        if (sh != shortResistors)
                            throw new IllegalArgumentException("shortResistors");
                    }
                }
                onlyThisLayer = geoms.onlyThisLayer;
            }
            // enumerate the hierarchy below here
            LayerVisitor visitor = new LayerVisitor(parentJob, tree, nodesToDelete, function,
                    originalPolygons, netSet, bBox, onlyThisLayer, geoms);
            HierarchyEnumerator.enumerateCell(curCell, VarContext.globalContext, visitor, shortResistors);
            tree.postProcess(true);

            switch (function)
            {
                case MERGE:
                case IMPLANT:
                    {
                        // With polygons collected, new geometries are calculated
                        boolean noNewNodes = true;
                        boolean isMerge = (function == LCMode.MERGE);
                        Rectangle2D rect;
                        Point2D [] points;

                        // Need to detect if geometry was really modified
                        for (Layer layer : tree.getKeySet())
                        {
                            Collection<PolyBase> set = tree.getObjects(layer, !isMerge, true);
                            Object[] polyArray = null;
                            if (function == LCMode.IMPLANT)
                            {
                            	Set<PolyBase> polySet = originalPolygons.get(layer);
                            	polyArray = polySet.toArray();
                            }
                            List<Rectangle2D> newImplants = new ArrayList<Rectangle2D>();

                            // Ready to create new implants.
                            for (PolyBase polyB : set)
                            {
                                points = polyB.getPoints();
                                rect = polyB.getBounds2D();

                                if (isMerge)
                                {
                                    // ignore if one of the original elements (implant merge only)
                                    if (polyArray != null)
                                    {
                                        boolean foundOrigPoly = false;
                                        for (Object poly : polyArray)
                                        {
                                            foundOrigPoly = polyB.polySame((PolyBase)poly);
                                            if (foundOrigPoly) break;
                                        }
                                        if (foundOrigPoly) continue;
                                    }

                                    // Adding the new implant. New implant not assigned to any local variable
                                    Point2D center = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
                                    PrimitiveNode priNode = layer.getPureLayerNode();
                                    NodeInst node = NodeInst.makeInstance(priNode, center, rect.getWidth(), rect.getHeight(), curCell);
                                    nodesToExamine.add(node);

                                    EPoint [] ePoints = new EPoint[points.length];
                                    for(int j=0; j<points.length; j++)
                                        ePoints[j] = new EPoint(points[j].getX(), points[j].getY());
                                    node.setTrace(ePoints);
                                } else
                                {
                                	newImplants.add(rect);
                                }
                                noNewNodes = false;
                            }
                            if (function == LCMode.IMPLANT)
                            {
                            	// merge when close to avoid notch errors
            					DRCTemplate rule = DRC.getSpacingRule(layer, null, layer, null, false, -1, 10, 100);
            					if (rule != null)
            					{
            						double dist = rule.getValue(0);
            						for(int i=0; i<newImplants.size(); i++)
            						{
            							Rectangle2D r = newImplants.get(i);
            							boolean merged = false;
            							for(int j=i+1; j<newImplants.size(); j++)
            							{
            								Rectangle2D oR = newImplants.get(j);
            								if (r.getMinX()-dist > oR.getMaxX() ||
            									oR.getMinX()-dist > r.getMaxX() ||
            									r.getMinY()-dist > oR.getMaxY() ||
            									oR.getMinY()-dist > r.getMaxY()) continue;

            								// they are too close: merge them
            								double lx = Math.min(r.getMinX(), oR.getMinX());
            								double hx = Math.max(r.getMaxX(), oR.getMaxX());
            								double ly = Math.min(r.getMinY(), oR.getMinY());
            								double hy = Math.max(r.getMaxY(), oR.getMaxY());
            								r.setRect(lx, ly, hx-lx, hy-ly);
            								newImplants.remove(j);
            								merged = true;
            								break;
            							}
            							if (merged) i--;
            						}
            					}
                                PrimitiveNode priNode = layer.getPureLayerNode();
                            	for(Rectangle2D r : newImplants)
                            	{
                                    // ignore if one of the original elements
                                    if (polyArray != null)
                                    {
                                        boolean covered = false;
                                        for (Object poly : polyArray)
                                        {
                                        	PolyBase pb = (PolyBase)poly;
                                        	Rectangle2D pbRect = pb.getBox();
                                        	if (pbRect == null) continue;
                                        	covered = pbRect.contains(r);
                                            if (covered) break;
                                        }
                                        if (covered) continue;
                                    }

                                    Point2D center = new Point2D.Double(r.getCenterX(), r.getCenterY());
                                    NodeInst node = NodeInst.makeInstance(priNode, center, r.getWidth(), r.getHeight(), curCell);
                                    nodesToExamine.add(node);
                                    node.setHardSelect();
                            	}
                            }
                        }
                        curCell.killNodes(nodesToDelete);
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
                        List<Layer> list = new ArrayList<Layer>(tree.getKeySet());
                        Collections.sort(list, Layer.layerSortByName);

                        for (Layer layer : list)
                        {
                            Collection<PolyBase> set = tree.getObjects(layer, false, true);

                            if (geoms != null && geoms.onlyThisLayer != null)
                            {
                                if (layer != geoms.onlyThisLayer) continue; // ignore this layer
                                // Add all elements
                                if (overlapPoint == null)
                                    nodesToExamine.addAll(set);
                                else
                                {
                                    // they must be connected
                                    for (PolyBase p : set)
                                    {
                                        if (p.contains(overlapPoint))
                                            nodesToExamine.add(p);
                                    }
                                }
                            }

                            double layerArea = 0;
                            double perimeter = 0;

                            // Get all objects and sum the area
                            for (PolyBase poly : set)
                            {
                                layerArea += poly.getArea();
                                perimeter += poly.getPerimeter();
                            }
                            layerArea /= lambdaSqr;
                            perimeter /= 2;

                            if (geoms != null)
                                geoms.addLayer(layer, layerArea, perimeter);
                            else
                                System.out.println("Layer " + layer.getName() + " covers " + TextUtils.formatDouble(layerArea)
                                        + " square lambda (" + TextUtils.formatDouble((layerArea/totalArea)*100) + "%)");
                        }
                        geoms.setTotalArea(totalArea);
                        if (geoms != null)
                            geoms.print();
                        else
                            System.out.println("Cell is " + TextUtils.formatDouble(totalArea) + " square lambda");
                    }
                    break;
                default:
                    System.out.println("Error in LayerCoverage: function not implemented");
            }
            return true;
        }
    }

    /************************************************************************
     * LayerCoverageJob Class
     ************************************************************************/
    private static class LayerCoverageJob extends Job
    {
        private Cell cell;
        private LCMode func;
        private GeometryHandler.GHMode mode;
        private GeometryOnNetwork geoms;
        private List<Object> nodesAdded;
        private Point2D overlapPoint; // to get to crop the search if a given bbox is not null
        private LayerCoverageTool.LayerCoveragePreferences lcp;

        public LayerCoverageJob(Cell cell, Job.Type jobType, LCMode func, GeometryHandler.GHMode mode,
                                GeometryOnNetwork geoms, Point2D overlapPoint, LayerCoverageTool.LayerCoveragePreferences lcp)
        {
            super("Layer Coverage on " + cell, User.getUserTool(), jobType, null, null, Priority.USER);
            this.cell = cell;
            this.func = func;
            this.mode = mode;
            this.geoms = geoms;
            this.overlapPoint = overlapPoint;
            this.lcp = lcp;
            setReportExecutionFlag(true);
        }

        public boolean doIt() throws JobException
        {
            LayerCoverageData data = new LayerCoverageData(this, cell, func, mode, geoms, null, overlapPoint, lcp);
            boolean done = data.doIt();

            if (func == LCMode.IMPLANT || (func == LCMode.NETWORK && geoms != null))
            {
                nodesAdded = new ArrayList<Object>();
                nodesAdded.addAll(data.getNodesToHighlight());
                if (func == LCMode.IMPLANT)
                	fieldVariableChanged("nodesAdded");
            }
            return done;
        }

        public void terminateOK()
        {
            // For implant highlight the new nodes
            if (func == LCMode.IMPLANT)
            {
                EditWindow_ wnd = Job.getUserInterface().getCurrentEditWindow_();
                if (wnd == null) return; // no GUI present
                if (nodesAdded == null) return; // nothing to show
                for (Object node : nodesAdded)
                {
                    wnd.addElectricObject((NodeInst)node, cell);
                }
            }
        }
    }

    /************************************************************************
     * AreaCoverageJob Class
     ************************************************************************/
    private static class AreaCoverageJob extends Job
    {
        private Cell curCell;
        private double deltaX, deltaY;
        private double width, height;
        private GeometryHandler.GHMode mode;
        private Map<Layer,Double> internalMap;
        private LayerCoverageTool.LayerCoveragePreferences lcp;

        public AreaCoverageJob(Cell cell, GeometryHandler.GHMode mode,
                               double width, double height, double deltaX, double deltaY, LayerCoveragePreferences lcp)
        {
            super("Layer Coverage", User.getUserTool(), Type.SERVER_EXAMINE, null, null, Priority.USER);
            this.curCell = cell;
            this.mode = mode;
            this.width = width;
            this.height = height;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
            this.lcp = lcp;
            setReportExecutionFlag(true); // Want to report statistics
        }

        public boolean doIt() throws JobException
        {
            ErrorLogger errorLogger = ErrorLogger.newInstance("Area Coverage");
            Rectangle2D bBoxOrig = curCell.getBounds();
            double maxY = bBoxOrig.getMaxY();
            double maxX = bBoxOrig.getMaxX();

            // if negative or zero values -> only once
            if (deltaX <= 0) deltaX = bBoxOrig.getWidth();
            if (deltaY <= 0) deltaY = bBoxOrig.getHeight();
            if (width <= 0) width = bBoxOrig.getWidth();
            if (height <= 0) height = bBoxOrig.getHeight();

            internalMap = new HashMap<Layer,Double>();
//            fieldVariableChanged("internalMap");

            for (double posY = bBoxOrig.getMinY(); posY < maxY; posY += deltaY)
            {
                for (double posX = bBoxOrig.getMinX(); posX < maxX; posX += deltaX)
                {
                    Rectangle2D box = new Rectangle2D.Double(posX, posY, width, height);
                    GeometryOnNetwork geoms = new GeometryOnNetwork(curCell, null, 1, true, null, lcp);
                    System.out.println("Calculating Coverage on cell '" + curCell.getName() + "' for area (" +
                            DBMath.round(posX) + "," + DBMath.round(posY) + ") (" +
                            DBMath.round(box.getMaxX()) + "," + DBMath.round(box.getMaxY()) + ")");
                    LayerCoverageData data = new LayerCoverageData(this, curCell, LCMode.AREA, mode, geoms, box, null, lcp);
                    if (!data.doIt())  // aborted by user
                    {
                        return false; // didn't finish
                    }
                    geoms.analyzeCoverage(box, errorLogger);

                    for (int i = 0; i < geoms.layers.size(); i++)
                    {
                        Layer layer = geoms.layers.get(i);
                        Double area = geoms.areas.get(i);

                        Double oldV = internalMap.get(layer);
                        double newV = area.doubleValue();
                        if (oldV != null)
                            newV += oldV.doubleValue();
                        internalMap.put(layer, new Double(newV));
                    }
                }
            }
            errorLogger.termLogging(true);
            return true;
        }

        public Map<Layer,Double> getDataInfo() { return internalMap; }
    }

    public enum LCMode // LC = LayerCoverageTool mode
    {
	    AREA,   // function Layer Coverage
	    MERGE,  // Generic merge polygons function
	    IMPLANT, // Coverage implants
	    NETWORK // List Geometry on Network function
    }


    /************************************************************************
     * LayerVisitor Class
     ************************************************************************/
    public static class LayerVisitor extends HierarchyEnumerator.Visitor
	{
        private Job parentJob;
		private GeometryHandler tree;
		private Set<NodeInst> deleteList; // Only used for coverage Implants. New coverage implants are pure primitive nodes
		private final LCMode function;
		private Map<Layer,Set<PolyBase>> originalPolygons;
		private Set<Network> netSet; // For network type, rest is null
        private Rectangle2D origBBox;
        private Area origBBoxArea;   // Area is always in coordinates of top cell
        private Layer onlyThisLayer;
        private GeometryOnNetwork geoms;

		public LayerVisitor(Job job, GeometryHandler t, Set<NodeInst> delList, LCMode func,
			Map<Layer, Set<PolyBase>> original, Set<Network> netSet, Rectangle2D bBox, Layer onlyThisLayer, GeometryOnNetwork geoms)
		{
            this.parentJob = job;
			this.tree = t;
			this.deleteList = delList;
			this.function = func;
			this.originalPolygons = original;
			this.netSet = netSet;
            this.origBBox = bBox;
            origBBoxArea = (bBox != null) ? new Area(origBBox) : null;
            this.onlyThisLayer = onlyThisLayer;
            this.geoms = geoms;
		}

		/**
		 * Determines if function of given layer is applicable for the corresponding operation
		 */
		private boolean isValidFunction(Layer layer, LCMode function)
		{
            if (onlyThisLayer != null && layer != onlyThisLayer)
                return false;

            Layer.Function func = layer.getFunction();

			switch (function)
			{
                case MERGE:
				case NETWORK:
					return (true);
				case IMPLANT:
					return (func.isSubstrate());
				case AREA:
					return (func.isPoly() || func.isMetal());
				default:
					return (false);
			}
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
            // Default case when no bounding box is used to crop the geometry
            if (origBBox == null) return true;

            // only because I need to transform the points.
            PolyBase polyRect = new PolyBase(rect);
            // To avoid transformation while traversing the hierarchy
            polyRect.transform(info.getTransformToRoot());
            rect = polyRect.getBounds2D();
            return rect.intersects(origBBox);
        }

        private boolean isJobAborted()
        {
            return (parentJob != null && parentJob.checkAbort());
        }

        public boolean enterCell(HierarchyEnumerator.CellInfo info)
		{
            // Checking if job is scheduled for abort or already aborted
	        if (isJobAborted())
                return (false);

			Cell curCell = info.getCell();
			Netlist netlist = info.getNetlist();

            // Nothing to visit  CAREFUL WITH TRANSFORMATION IN SUBCELL!!
            if (!doesIntersectBoundingBox(curCell.getBounds(), info))
                return false;

			// Checking if any network is found
            boolean found = (netSet == null);
            for (Iterator<Network> it = netlist.getNetworks(); !found && it.hasNext(); )
            {
                // In case there are many networks
                if (isJobAborted())
                    return (false);
                Network parentNet = it.next();
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
			for (Iterator<ArcInst> it = curCell.getArcs(); it.hasNext(); )
			{
				ArcInst arc = it.next();
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
				for (Poly poly : polyList)
				{
					Layer layer = poly.getLayer();

					boolean value = isValidFunction(layer, function);
					if (!value) continue;

					poly.transform(info.getTransformToRoot());

                    // If points are not rounded, in IMPLANT map.containsValue() might not work
                    poly.roundPoints();

                    storeOriginalPolygons(layer, poly);

                    Shape pnode = cropGeometry(poly, origBBoxArea);
                    // empty intersection
                    if (pnode == null) continue;

					tree.add(layer, pnode);  // tmp fix
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
            if (function != LCMode.IMPLANT) return;
            // For coverage implants
            Set<PolyBase> polySet = originalPolygons.get(layer);
            if (polySet == null)
            {
                polySet = new HashSet<PolyBase>();
                originalPolygons.put(layer, polySet);
            }
            //map.put(pnode, pnode.clone());
            polySet.add(poly);
        }

        /**
         *
         * @param no
         * @param info
         * @return true if node was visited
         */
		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
		{
             if (isJobAborted())
                 return false;
            //if (checkAbort()) return false;
			NodeInst node = no.getNodeInst();

			// Its like pins, facet-center
			if (NodeInst.isSpecialNode(node)) return (false);

            boolean inside = doesIntersectBoundingBox(node.getBounds(), info);

			// Its a cell
            if (node.isCellInstance()) return (inside);

            // Geometry outside contour
            if (!inside) return false;

//			boolean found = (netSet == null);
//			for(Iterator<PortInst> pIt = node.getPortInsts(); !found && pIt.hasNext(); )
//			{
//				PortInst pi = pIt.next();
//				PortProto subPP = pi.getPortProto();
//				Network parentNet = info.getNetlist().getNetwork(node, subPP, 0);
//				HierarchyEnumerator.CellInfo cinfo = info;
//				boolean netFound = false;
//				while ((netFound = netSet.contains(parentNet)) == false && cinfo.getParentInst() != null) {
//					parentNet = cinfo.getNetworkInParent(parentNet);
//					cinfo = cinfo.getParentInfo();
//				}
//				found = netFound;
//			}
//			if (!found) return (false); // skipping this node

			// Coverage implants are pure primitive nodes and they are ignored.
			if (node.isPrimtiveSubstrateNode()) //node.getFunction() == PrimitiveNode.Function.NODE)
			{
				if (info.isRootCell())
					deleteList.add(node);
				return (false);
			}
			Technology tech = node.getProto().getTechnology();
			Poly[] polyList = tech.getShapeOfNode(node, true, false, null);
			AffineTransform transform = node.rotateOut();

			boolean includedNode = false;
			for (int i = 0; i < polyList.length; i++)
			{
				Poly poly = polyList[i];
				if (netSet != null)
				{
					PortProto subPP = poly.getPort();
					Network parentNet = info.getNetlist().getNetwork(node, subPP, 0);
					HierarchyEnumerator.CellInfo cinfo = info;
					boolean netFound = false;
					while ((netFound = netSet.contains(parentNet)) == false && cinfo.getParentInst() != null) {
						parentNet = cinfo.getNetworkInParent(parentNet);
						cinfo = cinfo.getParentInfo();
					}
					if (!netFound) continue; // skipping this polygon
				}
				includedNode = true;

				Layer layer = poly.getLayer();

				// Only checking poly or metal for AREA case
				boolean value = isValidFunction(layer, function);
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

				tree.add(layer, pnode);
			}

			// add transistor gate/active if any part of the node matched a network
			if (includedNode && geoms != null)
			{
				PrimitiveNode.Function fun = node.getFunction();
				if (fun.isTransistor())
				{
					if (fun.isNTypeTransistor())
					{
						TransistorSize ts = node.getTransistorSize(info.getContext());
						geoms.n_active.area += ts.getDoubleArea();
						geoms.n_active.width += ts.getDoubleWidth();
						geoms.n_gate.area += ts.getDoubleArea();
						geoms.n_gate.width += ts.getDoubleWidth();
					} else if (fun.isPTypeTransistor())
					{
						TransistorSize ts = node.getTransistorSize(info.getContext());
						geoms.p_active.area += ts.getDoubleArea();
						geoms.p_active.width += ts.getDoubleWidth();
						geoms.p_gate.area += ts.getDoubleArea();
						geoms.p_gate.width += ts.getDoubleWidth();
					}
				}
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

    public static class TransistorInfo implements Serializable
	{
		/** sum of area of transistors */		public double area;
		/** sum of width of transistors */		public double width;
	};

	/**
	 * Class to represent all geometry on a network during layer coverage analysis.
	 */
	public static class GeometryOnNetwork implements Serializable {
	    public final Cell cell;
	    protected Set<Network> nets;
	    private double lambda;
		private boolean printable;
        private Layer onlyThisLayer;

	    // these lists tie together a layer, its area, and its half-perimeter
	    private ArrayList<Layer> layers;
	    private ArrayList<Double> areas;
	    private ArrayList<Double> halfPerimeters;
	    private double totalWire;
        private double totalArea;
        private LayerCoveragePreferences lcp;

        // these are the area and transistor widths for gate and active in N and P
        TransistorInfo p_gate, n_gate, p_active, n_active;

        public GeometryOnNetwork(Cell cell, Set<Network> nets, double lambda, boolean printable,
                                 Layer onlyThisLayer, LayerCoveragePreferences lcp)
        {
            this.lcp = lcp;
	        this.cell = cell;
	        this.nets = nets;
	        this.lambda = lambda;
            this.onlyThisLayer = onlyThisLayer;
	        layers = new ArrayList<Layer>();
	        areas = new ArrayList<Double>();
	        halfPerimeters = new ArrayList<Double>();
		    this.printable = printable;
	        totalWire = 0;
            totalArea = 0;
            p_gate = new TransistorInfo();
            n_gate = new TransistorInfo();
            p_active = new TransistorInfo();
            n_active = new TransistorInfo();
	    }

	    public double getTotalWireLength() { return totalWire; }

	    protected void setTotalArea(double area) {totalArea = area; }

	    public TransistorInfo getPGate() { return p_gate; }

	    public TransistorInfo getNGate() { return n_gate; }

	    public TransistorInfo getPActive() { return p_active; }

	    public TransistorInfo getNActive() { return n_active; }

	    public List<Layer> getLayers() { return layers; }

	    public List<Double> getAreas() { return areas; }

	    public List<Double> getHalfPerimeters() { return halfPerimeters; }

        private void addLayer(Layer layer, double area, double halfperimeter)
        {
            assert(layer != null);
            if (onlyThisLayer != null && layer != onlyThisLayer) return; // skip this one
	        layers.add(layer);
	        areas.add(new Double(area));
	        halfPerimeters.add(new Double(halfperimeter));

	        Layer.Function func = layer.getFunction();
	        // accumulate total wire length on all metal/poly layers
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
            totalArea = (bbox.getHeight()*bbox.getWidth())/(lambda*lambda);
            boolean foundError = false;

            for (int i = 0; i < layers.size(); i++)
            {
                Layer layer = layers.get(i);
                Double area = areas.get(i);
                double percentage = area.doubleValue()/totalArea * 100;
                double minV = lcp.getAreaCoverage(layer);
                if (percentage < minV)
                {
                    String msg = "Error area coverage " + layer.getName() + " min value = " + minV + " actual value = " + percentage;
                    PolyBase poly = new PolyBase(bbox);
                    errorLogger.logError(msg, poly, cell, layer.getIndex());
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
                for(Network net : nets)
                {
                    System.out.println("For " + net + " in " + cell + ":");
                }
            }

	        for (int i=0; i<layers.size(); i++) {
	            Layer layer = layers.get(i); // autoboxing
	            double area = areas.get(i).doubleValue();  // autoboxing
	            double halfperim = halfPerimeters.get(i).doubleValue();

	            System.out.println("\tLayer " + layer.getName()
	                    + ":\t area " + TextUtils.formatDouble(area) + "(" + TextUtils.formatDouble((area/totalArea)*100) + "%)"
	                    + "\t half-perimeter " + TextUtils.formatDouble(halfperim)
	                    + "\t ratio " + TextUtils.formatDouble(area/halfperim));
	        }
	        if (totalWire > 0)
	            System.out.println("Total wire length = " + TextUtils.formatDouble(totalWire/lambda));
            if (totalArea > 0)
                System.out.println("Total cell area = " + TextUtils.formatDouble(totalArea));
	    }
	}

    /***********************************
     * JUnit interface
     ***********************************/
    public static boolean testAll()
    {
        //return (LayerCoverageToolTest.basicAreaCoverageTest("area.log"));
        return true;
    }

}
