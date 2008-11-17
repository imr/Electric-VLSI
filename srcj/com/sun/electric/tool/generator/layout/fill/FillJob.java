/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FillJob.java
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.generator.layout.fill;

import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.CellChangeJobs;
import com.sun.electric.tool.user.ExportChanges;
import com.sun.electric.tool.routing.InteractiveRouter;
import com.sun.electric.tool.routing.SimpleWirer;
import com.sun.electric.tool.routing.Route;
import com.sun.electric.tool.routing.Router;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;

import java.util.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;
import java.awt.geom.Point2D;

/**
 * Fill Generator working on unconnected arcs
 * User: gg151869
 * Date: Nov 4, 2008
 * Time: 5:10:52 PM
 */
public class FillJob extends Job
{
    private Cell topCell;

    public FillJob(Cell cell, boolean doItNow)
    {
        super("Fill generator job", null, Type.CHANGE, null, null, Priority.USER);
        this.topCell = cell;

        if (doItNow) // call from regressions
        {
            try
            {
                if (doIt())
                    terminateOK();

            } catch (JobException e)
            {
                e.printStackTrace();
            }
        }
        else
            startJob(); // queue the job
    }

    public boolean doIt() throws JobException
    {
        if (topCell == null)
            return false;

        String[] instructions = topCell.getTextViewContents();

        if (instructions == null)
        {
            System.out.println("No fill instructions found.");
            return false;
        }
        for (String line : instructions)
        {
            if (line.startsWith("//"))
                continue; // commented line
            
            StringTokenizer parse = new StringTokenizer(line, ":", false);
            int count = 0;
            String fillCellName = null, rest = null;

            while (parse.hasMoreTokens())
            {
                assert(count < 2);
                String value = parse.nextToken();
                switch (count)
                {
                    case 0: fillCellName = value;
                        break;
                    default:
                        rest = value;
                        break;
                }
                count++;
            }
            if (fillCellName == null) continue; // no parsing matching

            // getting possible options like "w" or "2x4"
            int index = fillCellName.indexOf("(");
            boolean wideOption = false;

            if (index != -1)
            {
                // Getting options like (W)
                String options = fillCellName.substring(index);
                fillCellName = fillCellName.substring(0, index);
                parse = new StringTokenizer(options, "()", false);
                while (parse.hasMoreTokens())
                {
                    String option = parse.nextToken();
                    if (option.toLowerCase().equals("w"))
                    {
                        wideOption = true;
                        fillCellName += "W";
                    }
                }
            }
            List<NodeInst> fillCells = new ArrayList<NodeInst>();
            List<Geometric> fillGeoms = new ArrayList<Geometric>();

            // extracting fill subcells
            parse = new StringTokenizer(rest, " ,", false);
            String newName = fillCellName+"{lay}";
            Cell oldCell = topCell.getLibrary().findNodeProto(newName);
            // Delete previous version
            if (oldCell != null)
            {
                oldCell.kill();
            }
            Cell newCell = Cell.makeInstance(topCell.getLibrary(), newName);
            Point2D center = new Point2D.Double(0, 0);
            Technology tech = null;

            while (parse.hasMoreTokens())
            {
                String fillCell = parse.nextToken();
                index = fillCell.indexOf("("); // options like (I)
                boolean instanceFlag = false;
                if (index != -1)
                {
                    instanceFlag = fillCell.toLowerCase().indexOf("(i") != -1; // only (I) option for now
                    fillCell = fillCell.substring(0, index);
                }
                Cell c = topCell.getLibrary().findNodeProto(fillCell);
                Technology t = c.getTechnology();

                if (tech == null) tech = t;
                else if (tech != t)
                    System.out.println("Mixing technologies in fill generator: " + tech.getTechName() + " " + t.getTechName());
                NodeInst ni = NodeInst.makeInstance(c, center, c.getDefWidth(), c.getDefHeight(), newCell);
                if (!instanceFlag)
                    fillCells.add(ni);
                fillGeoms.add(ni);
            }
            newCell.setTechnology(tech);
            ExportChanges.reExportNodes(newCell, fillGeoms, false, true, false, true);
            new CellChangeJobs.ExtractCellInstances(newCell, fillCells, Integer.MAX_VALUE, true, true);
            generateFill(newCell, wideOption);
        }
        return true;
    }

    /**
     * Class
     */
    public static class FillCellsJob extends Job
    {
        private Cell topCell;

        public FillCellsJob(Cell cell)
        {
            super("Fill generator job", null, Type.CHANGE, null, null, Priority.USER);
            this.topCell = cell;
            startJob();
        }

        public boolean doIt() throws JobException
        {
            return generateFill(topCell, false);
        }
    }

    private static boolean generateFill(Cell theCell, boolean wideOption)
    {                           
        InteractiveRouter router  = new SimpleWirer();
        List<Layer> listOfLayers = new ArrayList<Layer>(12);

        for (Iterator<Layer> itL = theCell.getTechnology().getLayers(); itL.hasNext(); )
        {
            Layer l = itL.next();
            if (!l.getFunction().isMetal()) continue;  // only metals
            listOfLayers.add(l);
        }

        Collections.sort(listOfLayers, Layer.layerSortByName);
		Map<ArcProto,Integer> arcsCreatedMap = new HashMap<ArcProto,Integer>();
		Map<NodeProto,Integer> nodesCreatedMap = new HashMap<NodeProto,Integer>();
        boolean evenHorizontal = true; // even metal layers are horizontal. Basic assumption
        List<Route> routeList = new ArrayList<Route>();
        Layer[] topLayers = new Layer[2];
        Layer bottomLayer = null;
        List<String> exportsOnBottom = new ArrayList<String>();
        Map<String,Area> totalAreas = new HashMap<String,Area>();

        for (int i = 0; i < listOfLayers.size() - 1; i++)
        {
            Layer bottom = listOfLayers.get(i);
            boolean horizontal = isLayerHorizontal(bottom, evenHorizontal);

            List<ArcInst> arcs = getArcsInGivenLayer(theCell.getArcs(), horizontal, bottom, null, null);
            Layer top = listOfLayers.get(i+1);
            Map<String,Area> remainingGeos = new HashMap<String,Area>();

            for (ArcInst ai : arcs)
            {
                // Picking only 1 export and considering the root name`
                Netlist netlist = theCell.getNetlist(); // getting new version of netlist after every routing.
                String bottomName = getExportRootName(ai, netlist);

                if (bottomName == null) continue; // nothing to export

                List<PinsArcPair> pairs = new ArrayList<PinsArcPair>();
                Rectangle2D bounds = ai.getBounds();
                Area bottomA = new Area(bounds);

                if (bottomLayer == null || bottom == bottomLayer) // only for the bottom layer, the first one
                {
                    Area totalA = totalAreas.get(bottomName);
                    if (totalA == null)
                    {
                        totalA = new Area();
                        totalAreas.put(bottomName, totalA);
                    }
                    totalA.add(bottomA);
                }
                
                for(Iterator<RTBounds> it = theCell.searchIterator(bounds); it.hasNext(); )
                {
                    Geometric nGeom = (Geometric)it.next();

                    if (!(nGeom instanceof ArcInst))
                    {
                        continue; // only arcs
                    }

                    ArcInst nai = (ArcInst)nGeom;

                    // Checking arc orientation with respect to layer
                    // If the orientation is not correct, then ignore it
                    // Must be perpendicular to the reference layer (bottom)
                    if (!isArcAligned(nai, !horizontal))
                        continue;

//                    assert(nai.getProto().getNumArcLayers() == 1); // only 1 for now

                    Layer nl = nai.getProto().getLayer(0);

                    if (nl != top) continue; // ascending order is easy

                    String topName = getExportRootName(nai, netlist);

                    if (topName == null || !topName.equals(bottomName)) continue; // no export matching

                    Rectangle2D nBnds = nai.getBounds();
                    Area topA = new Area(nBnds);
                    topA.intersect(bottomA);
                    Rectangle2D resultBnd = topA.getBounds2D();

                    // checking for full overlap. If horizontal -> width must be 100%
                    boolean fullCoverageX = (horizontal) ? DBMath.areEquals(resultBnd.getWidth(), nBnds.getWidth()) :
                        DBMath.areEquals(resultBnd.getHeight(), nBnds.getHeight());
                    boolean fullCoverageY = (horizontal) ? DBMath.areEquals(resultBnd.getHeight(), bounds.getHeight()) :
                        DBMath.areEquals(resultBnd.getWidth(), bounds.getWidth());

                    if (!fullCoverageX || !fullCoverageY)
                    {
                        // adding to list of pieces of geometries
                        Area a = remainingGeos.get(bottomName);
                        if (a == null)
                        {
                            a = new Area();
                            remainingGeos.put(bottomName, a);
                        }
                        a.add(topA);
                        continue; // skipping this case
                    }

                    EPoint insert = new EPoint(resultBnd.getCenterX(), resultBnd.getCenterY());
                    pairs.add(new PinsArcPair(nai, insert, resultBnd));
                }

                Collections.sort(pairs, pinsArcSort);
                ArcInst mostLeft = ai;
                routeList.clear();

                if (bottomLayer == null)
                    bottomLayer = bottom;
                if (bottom == bottomLayer)
                    exportsOnBottom.add(bottomName);

                // Mark the layer as possible one of the top ones
                if (!pairs.isEmpty())
                {
                    topLayers[0] = bottom;
                    topLayers[1] = top;
                }

                for (PinsArcPair pair : pairs)
                {
                    //SplitContainter bottomSplit = splitArcAtPoint(mostLeft, pair.insert);
                    //SplitContainter topSplit = splitArcAtPoint(pair.topArc, pair.insert);
                    Route r = router.planRoute(theCell,  mostLeft, pair.topArc, pair.insert, null, true, true, pair.cut);
                    //mostLeft = bottomSplit.rightArc;
                    routeList.add(r);
                }
                for (Route r : routeList)
                {
                    Router.createRouteNoJob(r, theCell, false, arcsCreatedMap, nodesCreatedMap);
                }
            }

            // Adding remaining contacts due to non-100% overlap with original arcs
            for (Map.Entry<String,Area> e : remainingGeos.entrySet())
            {
                Area a = e.getValue();
                Netlist netlist = theCell.getNetlist();
                List<PolyBase> list = PolyBase.getPointsInArea(a, bottomLayer, false, false);
                List<ArcInst> at = getArcsInGivenLayer(theCell.getArcs(), !horizontal, top,  e.getKey(), netlist);
                List<PinsArcPair> pairs = new ArrayList<PinsArcPair>(2);

                // Add extra contacts. All points should be aligned in same horizontal or vertical line
                // first the top arcs. They should be less in number (less number of splits)
                for (PolyBase p : list)
                {
                    Rectangle2D resultBnd = p.getBounds2D();
                    // look for the first pair of arcs in bottom/top arcs that overlap with geometry
                    ArcInst topA = null;

                    for (ArcInst ai : at)
                    {
                        Rectangle2D r = ai.getBounds();
                        if (r.intersects(resultBnd))
                        {
                            // found
                            topA = ai;
                            break;
                        }
                    }
                    assert (topA != null);
                    EPoint insert = new EPoint(resultBnd.getCenterX(), resultBnd.getCenterY());
                    pairs.add(new PinsArcPair(topA, insert, resultBnd));
                }
                // Sort the new pairs from left/top to right/bottom
                Collections.sort(pairs, pinsArcSort);

                // Look for bottomLayer arcs for those given positions
                List<ArcInst> ab = getArcsInGivenLayer(theCell.getArcs(), horizontal, bottom, e.getKey(), netlist);
                routeList.clear();

                for (PinsArcPair pair : pairs)
                {
                    ArcInst bottomA = null;
                    for (ArcInst ai : ab)
                    {
                        Rectangle2D r = ai.getBounds();
                        if (r.contains(pair.insert))
                        {
                            // found
                            bottomA = ai;
                            break;
                        }
                    }
                    assert(bottomA != null);
                    //SplitContainter bottomSplit = splitArcAtPoint(bottomA, pair.insert);
                    //SplitContainter topSplit = splitArcAtPoint(pair.topArc, pair.insert);
                    Route r = router.planRoute(theCell,  bottomA, pair.topArc, pair.insert, null, true, true, pair.cut);
                    // remove the old one and add new arc
                    ab.remove(bottomA);
                    //ab.add(bottomSplit.rightArc);
                    topLayers[0] = bottom;
                    topLayers[1] = top;
                    routeList.add(r);
                }
                for (Route r : routeList)
                {
                    Router.createRouteNoJob(r, theCell, false, arcsCreatedMap, nodesCreatedMap);
                }
            }
        }

        Netlist netlist = theCell.getNetlist();
        // Connect cell instances
//        for (Iterator<NodeInst> itNi = theCell.getNodes(); itNi.hasNext();)
//        {
//            NodeInst ni = itNi.next();
//
//            // only cell instances
//            if (!ni.isCellInstance()) continue;
//
//            // Checking if the export overlaps with
//            // Checking if there are exports overlapping with the arc
//            for (Iterator<Export> itE = ni.getExports(); itE.hasNext(); )
//            {
//                Export ex = itE.next();
//                String rootName = extractRootName(ex.getName());
//                Network jNet = getNetworkFromName(netlist, rootName);
//
//                PrimitiveNode n = ex.getBasePort().getParent();
//                Layer layer = n.getLayerIterator().next(); // assuming only 1
//
//                for (Iterator<ArcInst> itA = jNet.getArcs(); itA.hasNext();)
//                {
//                    ArcInst ai = itA.next();
//                    Layer l = ai.getProto().getLayer(0);
//
//                    if (l == layer)
//                        System.out.println("found in same layer");
//                    else if (Math.abs(l.getIndex() - layer.getIndex()) <=1)
//                    {
//                        System.out.println("Found up or down");
//                    }
//                }
//                PortInst pi = ex.getOriginalPort();
//                PortProto epi = pi.getPortProto(); // Trying to get
//                NodeInst npi = pi.getNodeInst();
//                    System.out.println("");
//            }
//        }

        // Remove exports in lower layers
        Set<Export> toDelete = new HashSet<Export>();
        Map<String,Export> inBottomLayer = new HashMap<String,Export>(); // enough with 1 export stored

        // find the highest level metal for a given network
        Map<Network,Layer> maximumLayer = new HashMap<Network,Layer>();
        for (Iterator<Export> itE = theCell.getExports(); itE.hasNext();) {
            Export exp = itE.next();
            PrimitiveNode n = exp.getBasePort().getParent();
            Network net = netlist.getNetwork(exp, 0);
            Layer l = maximumLayer.get(net);
            for (Iterator<Layer> itL = n.getLayerIterator(); itL.hasNext();)
            {
                Layer lnew = itL.next();
                if (l == null) {
                    l = lnew;
                    continue;
                }
                if (lnew.getFunction().getLevel() > l.getFunction().getLevel()) {
                    l = lnew;
                }
            }
            maximumLayer.put(net, l);
        }

        for (Iterator<Export> itE = theCell.getExports(); itE.hasNext();)
        {
            Export exp = itE.next();
            PrimitiveNode n = exp.getBasePort().getParent();
            boolean found = false;
            String rootName = extractRootName(exp.getName());

            Layer topLayer = maximumLayer.get(netlist.getNetwork(exp, 0));
            int levelMax = topLayer.getFunction().getLevel();
            for (Iterator<Layer> itL = n.getLayerIterator(); itL.hasNext();)
            {
                Layer l = itL.next();
                int level = l.getFunction().getLevel();
                if (level == levelMax || level == (levelMax-1)) {
                    found = true;
                } else {
                    inBottomLayer.put(rootName, exp); // put the last one in the network.
                }
/*
                if (topLayers[0] == l || topLayers[1] == l)
                {
                    found = true;
                }
                else if (l == bottomLayer)
                {
                    inBottomLayer.put(rootName, exp); // put the last one in the network.
                }
*/
            }
            if (!found) // delete the export
                toDelete.add(exp);
        }

        // Add extra export in the middle of the botoom layer.
        if (wideOption)
        {
            Map<String,List<PinsArcPair>> newExports = new HashMap<String,List<PinsArcPair>>();
            boolean horizontal = isLayerHorizontal(bottomLayer, evenHorizontal);

            // For each export 1 export in the bottom layer should be insert
            for (Map.Entry<String,Export> e : inBottomLayer.entrySet())
            {
                Export exp = e.getValue();
                String rootName = extractRootName(exp.getName());
                Network net = netlist.getNetwork(exp, 0);
                // Collect first all the arcs in that layer
                List<ArcInst> arcs = getArcsInGivenLayer(net.getArcs(), horizontal, bottomLayer, null, null);
                Area area = totalAreas.get(rootName);
                List<PinsArcPair> l = new ArrayList<PinsArcPair>();

                // It could be multiple areas disconnected.
                List<PolyBase> list = PolyBase.getPointsInArea(area, bottomLayer, false, false);
                if (list == null) continue; // no matching export on bottom level
                for (PolyBase b : list)
                {
                    Rectangle2D resultBnd = b.getBounds2D();
                    EPoint insert = new EPoint(resultBnd.getCenterX(), resultBnd.getCenterY());
                    PinsArcPair split; // looking for the first arc where the center is contained.
                    for (ArcInst ai : arcs)
                    {
                        Rectangle2D bnd = ai.getBounds();
                        if (bnd.contains(insert.getX(), insert.getY()))
                        {
                            split = new PinsArcPair(ai, insert, null);
                            l.add(split);
                            break;
                        }
                    }
                }
                newExports.put(rootName, l);
            }

            for (Map.Entry<String,List<PinsArcPair>> e : newExports.entrySet())
            {
                List<PinsArcPair> pairs = e.getValue();
                for (PinsArcPair pair : pairs)
                {
                    SplitContainter split = splitArcAtPoint(pair.topArc, pair.insert);
                    Export.newInstance(theCell, split.splitPin.getPortInst(0), e.getKey(), PortCharacteristic.UNKNOWN);
                }
            }
        }

        // Delete after dealing with the wide option
        // At least one connection was done otherwise it will keep the original exports.
        // Option is mostly for debugging purposes.
        if (topLayers[0] != null || topLayers[1] != null)
            theCell.killExports(toDelete);
        return true;
    }

    /**
     * Method to get Network from a given network root name
     * @param netlist Netlist containing the network information
     * @param rootName Root name of the network
     * @return Network
     */
    private static Network getNetworkFromName(Netlist netlist, String rootName)
    {
        for (Iterator<Network> it = netlist.getNetworks(); it.hasNext();)
        {
            Network net = it.next();
            if (net.getName().startsWith(rootName))
                return net;
        }
        return null;
    }

    /**
     * Method to determine if Layer is aligned horizontally
     * @param layer Given Layer
     * @param evenHorizontal True if even layers are horizontal
     * @return True of layer is horizontal
     */
    private static boolean isLayerHorizontal(Layer layer, boolean evenHorizontal)
    {
        int metalNumber = layer.getFunction().getLevel();
        return (evenHorizontal && metalNumber%2==0) || (!evenHorizontal && metalNumber%2==1);
    }

    /**
     * Method to collect arcs in a given layer with a given export name from an iterator
     * @param itAi Arcs iterator
     * @param horizontal
     * @param layer Given layer
     * @param exportName Given export name. If null, any export name is valid
     * @param netlist  Given Netlist to find exports
     * @return List of ArcInsts
     */
    private static List<ArcInst> getArcsInGivenLayer(Iterator<ArcInst> itAi, Boolean horizontal, Layer layer, String exportName, Netlist netlist)
    {
        List<ArcInst> arcs = new ArrayList<ArcInst>();

        for (; itAi.hasNext();)
        {
            ArcInst ai = itAi.next();

            // Checking arc orientation with respect to layer
            // If the orientation is not correct, then ignore it
            if (!isArcAligned(ai, horizontal))
                continue;

//                assert(ai.getProto().getNumArcLayers() == 1); // only 1 for now
            Layer l = ai.getProto().getLayer(0);
            if (l != layer)
                continue;
            if (exportName != null)
            {
                Network jNet = netlist.getNetwork(ai, 0);
                Export exp = jNet.getExports().next(); // first is enough
                String expName = extractRootName(exp.getName());
                if (!expName.equals(exportName))
                    continue; // no match
            }
            arcs.add(ai);
        }
        return arcs;
    }

    /**
     * Method to check whether a given arc is properly oriented with respect to the expected input
     * @param ai
     * @param horizontal
     * @return
     */
    private static boolean isArcAligned(ArcInst ai, boolean horizontal)
    {
        EPoint head = ai.getHeadLocation();
        EPoint tail = ai.getTailLocation();
        if (horizontal)
            return (DBMath.areEquals(head.getY(), tail.getY()));
        return (DBMath.areEquals(head.getX(), tail.getX()));
    }

    /**
     * Method to extract the export root name from a given name
     * @param name String containing the export name
     * @return String containing the root name of the export
     */
    private static String extractRootName(String name)
    {
        int index = name.indexOf("_");
        if (index != -1) // remove any character after _
            name = name.substring(0, index);
        return name;
    }

    /**
     * Methot to extrac root name of the export in a given arc
     * @param ai arc with the export
     * @return Non-null string with the root name. Null if no export was found.
     */
    private static String getExportRootName(ArcInst ai, Netlist netlist)
    {
        Network jNet = netlist.getNetwork(ai, 0);
        // Picking only 1 export and considering the root name`
        // Assuming at 1 export per arc
        assert(jNet != null);
        Iterator<String> exportNames = jNet.getExportedNames();
        if (!exportNames.hasNext())
            return null; // no export
        String rootName = extractRootName(exportNames.next());
        return rootName;
    }

    /**
     * Method to split an arc at a given point
     * @param ai arc to split
     * @param insert point to split at
     * @return SplitContainter representing the split pin and new arcs
     */
    private static SplitContainter splitArcAtPoint(ArcInst ai, EPoint insert)
    {
        // create the break pins
        ArcProto ap = ai.getProto();
        NodeProto np = ap.findPinProto();
        if (np == null) return null;
        NodeInst ni = NodeInst.makeInstance(np, insert, np.getDefWidth(), np.getDefHeight(), ai.getParent());
        if (ni == null)
        {
            System.out.println("Cannot create pin " + np.describe(true));
            return null;
        }

        SplitContainter container = new SplitContainter();
        container.splitPin = ni;

        // get location of connection to these pins
        PortInst pi = ni.getOnlyPortInst();

        // now save the arc information and delete it
        PortInst headPort = ai.getHeadPortInst();
        PortInst tailPort = ai.getTailPortInst();
        Point2D headPt = ai.getHeadLocation();
        Point2D tailPt = ai.getTailLocation();
        double width = ai.getLambdaBaseWidth();
//        String arcName = ai.getName();

        // create the new arcs
        ArcInst newAi1 = ArcInst.makeInstanceBase(ap, width, headPort, pi, headPt, insert, null);
        ArcInst newAi2 = ArcInst.makeInstanceBase(ap, width, pi, tailPort, insert, tailPt, null);
        newAi1.setHeadNegated(ai.isHeadNegated());
        newAi1.setHeadExtended(ai.isHeadExtended());
        newAi1.setHeadArrowed(ai.isHeadArrowed());
        newAi1.setTailNegated(ai.isTailNegated());
        newAi1.setTailExtended(ai.isTailExtended());
        newAi1.setTailArrowed(ai.isTailArrowed());

        newAi2.setHeadNegated(ai.isHeadNegated());
        newAi2.setHeadExtended(ai.isHeadExtended());
        newAi2.setHeadArrowed(ai.isHeadArrowed());
        newAi2.setTailNegated(ai.isTailNegated());
        newAi2.setTailExtended(ai.isTailExtended());
        newAi2.setTailArrowed(ai.isTailArrowed());

        // Determining which arc is left/top
        if (isLeftTop(headPt, tailPt))
        {
            container.leftArc = newAi1;
            container.rightArc = newAi2;
        }
        else
        {
            container.leftArc = newAi2;
            container.rightArc = newAi1;
        }
        ai.kill();
//        if (arcName != null)
//        {
//            if (headPt.distance(insert) > tailPt.distance(insert))
//            {
//                newAi1.setName(arcName);
//                newAi1.copyTextDescriptorFrom(ai, ArcInst.ARC_NAME);
//            } else
//            {
//                newAi2.setName(arcName);
//                newAi2.copyTextDescriptorFrom(ai, ArcInst.ARC_NAME);
//            }
//        }
        return container;
    }

    /**
     * Internal class to keep potential connections
     */
    private static class PinsArcPair
    {
        private ArcInst topArc;
        private EPoint insert;
        private Rectangle2D cut;

        PinsArcPair(ArcInst topA, EPoint point, Rectangle2D c)
        {
            topArc = topA;
            insert = point;
            cut = c;
        }
    }

    private static boolean isLeftTop(Point2D p1, Point2D p2)
    {
        if (DBMath.areEquals(p1.getX(), p2.getX()))
        {
            return (!DBMath.isGreaterThan(p1.getY(), p2.getY()));
        }
        else if (DBMath.areEquals(p1.getY(), p2.getY()))
        {
            return (!DBMath.isGreaterThan(p1.getX(), p2.getX()));
        }
        else
            assert(false); // not considered yet
        return false;
    }

    /**
     * Class to store temp info
     */
    private static class SplitContainter
    {
        NodeInst splitPin;
        ArcInst leftArc, rightArc; // to keep track of new arcs after the original one was split
    }

    /**
     * To sort PinsArcPair
     */
    private static final PinsArcPairSort pinsArcSort = new PinsArcPairSort();

    /**
     * Comparator class for sorting PinsArcPair by their insertion point
     */
    private static class PinsArcPairSort implements Comparator<PinsArcPair>
    {
        /**
         * Method to compare two PinsArcPair objects by their insertion point.
         * @param l1 one PinsArcPair.
         * @param l2 another PinsArcPair.
         * @return an integer indicating their sorting order.
         */
        public int compare(PinsArcPair l1, PinsArcPair l2)
        {
            EPoint p1 = l1.insert;
            EPoint p2 = l2.insert;
            return (isLeftTop(p2, p1)?1:-1);
        }
    }

}
