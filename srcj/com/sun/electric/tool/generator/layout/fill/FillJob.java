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
import com.sun.electric.database.topology.*;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;

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
            
            List<NodeInst> fillCells = new ArrayList<NodeInst>();
            List<Geometric> fillGeoms = new ArrayList<Geometric>();

            // extracting fill subcells
            parse = new StringTokenizer(rest, " ", false);
            Cell newCell = Cell.makeInstance(topCell.getLibrary(), fillCellName+"{lay}");
            Point2D center = new Point2D.Double(0, 0);
            Technology tech = null;

            while (parse.hasMoreTokens())
            {
                String fillCell = parse.nextToken();
                Cell c = topCell.getLibrary().findNodeProto(fillCell);
                Technology t = c.getTechnology();

                if (tech == null) tech = t;
                else if (tech != t)
                    System.out.println("Mixing technologies in fill generator: " + tech.getTechName() + " " + t.getTechName());
                NodeInst ni = NodeInst.makeInstance(c, center, c.getDefWidth(), c.getDefHeight(), newCell);
                fillCells.add(ni);
                fillGeoms.add(ni);
            }
            newCell.setTechnology(tech);
            ExportChanges.reExportNodes(newCell, fillGeoms, false, true, false, true);
            new CellChangeJobs.ExtractCellInstances(newCell, fillCells, Integer.MAX_VALUE, true, true);
            //generateFill(newCell);
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
            return generateFill(topCell);
        }
    }

    private static boolean generateFill(Cell theCell)
    {                           
        InteractiveRouter router  = new SimpleWirer();
        Map<Layer, List<ArcInst>> map = new HashMap<Layer, List<ArcInst>>();

        for (Iterator<ArcInst> itAi = theCell.getArcs(); itAi.hasNext();)
        {
            ArcInst ai = itAi.next();
            assert(ai.getProto().getNumArcLayers() == 1); // only 1 for now
            Layer l = ai.getProto().getLayer(0);

            if (!l.getFunction().isMetal()) continue;  // only metals

            List<ArcInst> arcs = map.get(l);
            if (arcs == null)
            {
                arcs = new ArrayList<ArcInst>();
                map.put(l, arcs);
            }
            arcs.add(ai);
        }

        Set<Layer> setOfLayers = map.keySet();
        List<Layer> listOfLayers = new ArrayList<Layer>(setOfLayers.size());
        listOfLayers.addAll(setOfLayers);
        Collections.sort(listOfLayers, Layer.layerSortByName);
		Map<ArcProto,Integer> arcsCreatedMap = new HashMap<ArcProto,Integer>();
		Map<NodeProto,Integer> nodesCreatedMap = new HashMap<NodeProto,Integer>();
        boolean evenHorizontal = true; // even metal layers are horizontal. Basic assumption
        List<Route> routeList = new ArrayList<Route>();
        List<ArcInst> arcs = new ArrayList<ArcInst>();

        for (int i = 0; i < listOfLayers.size() - 1; i++)
        {
            Layer bottom = listOfLayers.get(i);
            int metalNumber = bottom.getFunction().getLevel();
            boolean horizontal = (evenHorizontal && metalNumber%2==0) || (!evenHorizontal && metalNumber%2==1);
//            List<ArcInst> arcs1 = map.get(bottom);

            arcs.clear();
            for (Iterator<ArcInst> itAi = theCell.getArcs(); itAi.hasNext();)
            {
                ArcInst ai = itAi.next();
                assert(ai.getProto().getNumArcLayers() == 1); // only 1 for now
                Layer l = ai.getProto().getLayer(0);
                if (l == bottom)
                    arcs.add(ai);
            }

            Layer top = listOfLayers.get(i+1);
            for (ArcInst ai : arcs)
            {
                // Checking arc orientation with respect to layer
                // If the orientation is not correct, then ignore it
                if (!isArcAligned(ai, horizontal))
                    continue;

                Rectangle2D bounds = ai.getBounds();
                Area bottomA = new Area(bounds);
                // Picking only 1 export and considering the root name`
                Netlist netlist = theCell.getNetlist(); // getting new version of netlist after every routing.
                String bottomName = getExportRootName(ai, netlist);

                if (bottomName == null) continue; // nothing to export

                List<PinsArcPair> pairs = new ArrayList<PinsArcPair>();

                for(Iterator<RTBounds> it = theCell.searchIterator(bounds); it.hasNext(); )
                {
                    Geometric nGeom = (Geometric)it.next();

                    if (!(nGeom instanceof ArcInst)) continue; // only arcs

                    ArcInst nai = (ArcInst)nGeom;

                    assert(nai.getProto().getNumArcLayers() == 1); // only 1 for now

                    Layer nl = nai.getProto().getLayer(0);

                    if (nl != top) continue; // ascending order is easy

                    // Checking arc orientation with respect to layer
                    // If the orientation is not correct, then ignore it
                    // Must be perpendicular to the reference layer (bottom)
                    if (!isArcAligned(nai, !horizontal))
                        continue;

                    String topName = getExportRootName(nai, netlist);

                    if (topName == null || !topName.equals(bottomName)) continue; // no export matching

                    Rectangle2D nBnds = nai.getBounds();
                    Area topA = new Area(nBnds);
                    topA.intersect(bottomA);
                    Rectangle2D resultBnd = topA.getBounds2D();

                    // checking for full overlap. If horizontal -> width must be 100%
                    boolean fullCoverage = (horizontal) ? DBMath.areEquals(resultBnd.getWidth(), nBnds.getWidth()) :
                        DBMath.areEquals(resultBnd.getHeight(), nBnds.getHeight());

                    if (!fullCoverage)
                        continue; // skipping this case

                    EPoint insert = new EPoint(resultBnd.getCenterX(), resultBnd.getCenterY());
                    pairs.add(new PinsArcPair(nai, insert));
                }

                Collections.sort(pairs, pinsArcSort);
                ArcInst mostLeft = ai;
                routeList.clear();

                for (PinsArcPair pair : pairs)
                {
                    SplitContainter bottomSplit = splitArcAtPoint(mostLeft, pair.insert);
                    SplitContainter topSplit = splitArcAtPoint(pair.topArc, pair.insert);
                    Route r = router.planRoute(theCell,  bottomSplit.splitPin, topSplit.splitPin, pair.insert, null, true, true);
                    mostLeft = bottomSplit.rightArc;
                    routeList.add(r);
                }
                for (Route r : routeList)
                {
                    Router.createRouteNoJob(r, theCell, false, arcsCreatedMap, nodesCreatedMap);
                }
            }
        }
        return true;
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
     * Methot to extrac root name of the export in a given arc
     * @param ai arc with the export
     * @return Non-null string with the root name. Null if no export was found.
     */
    private static String getExportRootName(ArcInst ai, Netlist netlist)
    {
        Network jNet = netlist.getNetwork(ai, 0);
        // Picking only 1 export and considering the root name`
        // Assuming at 1 export per arc
        if (jNet == null)
            System.out.println("Here");
        Iterator<String> exportNames = jNet.getExportedNames();
        assert (exportNames.hasNext()); // must have one?
        String rootName = exportNames.next();

//        Export exp;
//        if (ai.getTailPortInst().getNodeInst().getNumExports() > 0)
//            exp = ai.getTailPortInst().getNodeInst().getExports().next();
//        else if (ai.getHeadPortInst().getNodeInst().getNumExports() > 0)
//            exp = ai.getHeadPortInst().getNodeInst().getExports().next();
//        else
//        {
//            System.out.println("Should this happen?");
//            return null;
//        }
//        String rootName = exp.getName();
        int index = rootName.indexOf("_");
        if (index != -1) // remove any character after _
            rootName = rootName.substring(0, index);
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

        PinsArcPair(ArcInst topA, EPoint point)
        {
            topArc = topA;
            insert = point;
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
