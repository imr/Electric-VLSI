/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StitchFillJob.java
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

import com.sun.electric.database.EditingPreferences;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.CellChangeJobs;
import com.sun.electric.tool.user.ExportChanges;
import com.sun.electric.tool.user.IconParameters;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.routing.*;
import com.sun.electric.tool.routing.AutoStitch.AutoOptions;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.geometry.*;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.IdMapper;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;

import java.util.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;

/**
 * Fill Generator working on unconnected arcs
 * User: Gilda Garreton
 * Date: Nov 4, 2008
 */
public class StitchFillJob extends Job
{
    private Cell topCell;
    private Library outLibrary;
    private List<Cell> generatedCells = new ArrayList<Cell>();
    private boolean evenHorizontal = true; // even metal layers are horizontal. Basic assumption
    private List<String> layersWithExports = new ArrayList<String>();
    private IconParameters iconParameters = IconParameters.makeInstance(true);

//    private static final boolean doFill = false;

    public StitchFillJob(Cell cell, Library lib, boolean doItNow)
    {
        super("Fill generator job", null, Type.CHANGE, null, null, Priority.USER);
        this.topCell = cell;
        this.outLibrary = lib;

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

    /**
     * Method to replace old cell by new cell if it was instantied in other cells. It deletes the old cell after.
     * @param oldC
     * @param newC
     */
    private static Cell cleanReplacement(Cell oldC, Library lib, String fillCellName)
    {
        if (oldC != null)
        {
            String fakeName = oldC.getName()+"TMPStitch";
            assert(lib.findNodeProto(fakeName) == null); // we don't want to use an existing name
            IdMapper idMapper = oldC.rename(fakeName, null);
            oldC = idMapper.get(oldC.getId()).inDatabase(EDatabase.serverDatabase());
        }
        Cell newCell = Cell.makeInstance(lib, fillCellName);
        if (oldC != null)
        {
            List<NodeInst> nodesToReplace = new ArrayList<NodeInst>();
            for(Iterator<NodeInst> nIt = oldC.getInstancesOf(); nIt.hasNext(); )
                nodesToReplace.add(nIt.next());
            for (NodeInst ni: nodesToReplace) {
                NodeInst replaced = ni.replace(newCell, false, false);
                if (replaced == null)
                    System.out.println("Warning: conflicts while replacing existing cell in StitchFill");
//                assert replaced != null;
            }
            oldC.kill();
        }
        return newCell;
    }

    public boolean doIt() throws JobException
    {
        Point2D center = new Point2D.Double(0, 0);

        // Get cells from EditWindows
        if (topCell == null)
        {
            Technology tech = null;
            List<NodeInst> fillCells = new ArrayList<NodeInst>();
            List<Geometric> fillGeoms = new ArrayList<Geometric>();
            String fillCellName = "NewFill{lay}";

            Library lib = Library.getCurrent();
            if (lib == null)
            {
                System.out.println("No current library available.");
                return false;
            }


            Cell oldCell = lib.findNodeProto(fillCellName);
            // Delete previous version
            // Does a clean replacement and removal of the old cell
            Cell newCell = cleanReplacement(oldCell, lib, fillCellName);

            for (Iterator<WindowFrame> itW = WindowFrame.getWindows(); itW.hasNext();)
            {
                WindowFrame w = itW.next();
                Cell c = w.getContent().getCell();

                if (c == null)
                {
                    System.out.println("No cell in window '" + w.getTitle() + "'");
                    continue;
                }
                if (c.getView() != View.LAYOUT)
                {
                    System.out.println("Cell '" + c + "' is not a layout cell");
                    continue;
                }

                Technology t = c.getTechnology();
                if (tech == null) tech = t;
                else if (tech != t)
                    System.out.println("Mixing technologies in fill generator: " +
                        tech.getTechName() + " " + t.getTechName());

                NodeInst ni = NodeInst.makeInstance(c, center, c.getDefWidth(), c.getDefHeight(), newCell);
                fillCells.add(ni);
                fillGeoms.add(ni);
            }
            // actual fill gen
            doTheJob(newCell, fillCellName, fillCells, fillGeoms, null, false, this);
            return true; // done at this point
        }

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

            // metal direction
            if (line.startsWith("$"))
            {
                StringTokenizer parse = new StringTokenizer(line, "= $", false);
                int count = 0;
                while (parse.hasMoreTokens())
                {
                    assert(count < 2);
                    String value = parse.nextToken();
                    switch (count)
                    {
                        case 0:
                            assert(value.toLowerCase().equals("horizontal")); // only parameter supported for now
                            break;
                        case 1:
                            if (value.toLowerCase().equals("odd"))
                                evenHorizontal = false;
                            else if (value.toLowerCase().equals("even"))
                                evenHorizontal = true;
                            else
                            {
                                System.out.println("Invalid instruction '" + value + "' in stitch instructions doc.");
                            }
                            break;
                        default:
                            assert(false); // no case
                    }
                    count++;
                }
                continue;
            }
            else if (line.startsWith("@exports"))
            {
                int index = line.indexOf("=");
                assert(index != -1);
                // exports is the only command for now
                StringTokenizer parse = new StringTokenizer(line.substring(index+1), " {,}", false);
                while (parse.hasMoreTokens())
                {
                    String value = parse.nextToken();
                    layersWithExports.add(value);
                }
                continue;
            }
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
            List<TileInfo> tileList = new ArrayList<TileInfo>();

            if (index != -1)
            {
                // Getting options like (W)
                String options = fillCellName.substring(index);
                fillCellName = fillCellName.substring(0, index);
                parse = new StringTokenizer(options, "(), ", false);
                while (parse.hasMoreTokens())
                {
                    String option = parse.nextToken();
                    String lowerCase = option.toLowerCase();
                    // wide option where exports are added in lowest layer
                    if (lowerCase.equals("w"))
                    {
                        wideOption = true;
                        fillCellName += "W";
                    }
                    else if (lowerCase.contains("x"))
                    {
                        index = lowerCase.indexOf("x");
                        String str = lowerCase.substring(0, index);
                        int x = Integer.valueOf(str).intValue();
                        str = lowerCase.substring(index+1, lowerCase.length());
                        int y = Integer.valueOf(str).intValue();
                        // tile information
                        TileInfo tile = new TileInfo(x, y);
                        tileList.add(tile);
                    }
                }
            }

            if (rest == null)
            {
                System.out.println("Error parsing the fill instructions");
                return false;
            }
            // extracting fill subcells
            parse = new StringTokenizer(rest, " ,", false);
            String newName = fillCellName+"{lay}";
            Cell oldCell = outLibrary.findNodeProto(newName);
            // Delete previous version
            // Does a clean replacement and removal of the old cell
            Cell newCell = cleanReplacement(oldCell, outLibrary, newName);

            Technology tech = null;
            List<NodeInst> fillCells = new ArrayList<NodeInst>();
            List<Geometric> fillGeoms = new ArrayList<Geometric>();

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
                Cell c = topCell.getLibrary().findNodeProto(fillCell+"{lay}");

                if (c == null)
                {
                    System.out.println("Cell '" + fillCell + "' does not exist");
                    continue;
                }
                if (c.getView() != View.LAYOUT)
                {
                    System.out.println("Cell '" + c + "' is not a layout cell");
                    continue;
                }
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
            generatedCells.add(newCell);

            // actual fill gen
            doTheJob(newCell, fillCellName, fillCells, fillGeoms, tileList, wideOption, this);
        }
        return true;
    }

    public List<Cell> getGeneratedCells() {return generatedCells;}

    /**
     * Method that executes the different functions to get the tool working
     * @param newCell
     * @param fillCellName
     * @param fillCells
     * @param fillGeoms
     * @param tileList
     * @param wideOption
     * @param job
     */
    private static void doTheJob(Cell newCell, String fillCellName, List<NodeInst> fillCells,
                                 List<Geometric> fillGeoms, List<TileInfo> tileList,
                                 boolean wideOption, StitchFillJob job)
    {
        // Re-exporting
        ExportChanges.reExportNodes(newCell, fillGeoms, false, true, false, true, true, job.iconParameters);
//        if (!doFill) return;

        // Flatting subcells
        new CellChangeJobs.ExtractCellInstances(newCell, fillCells, Integer.MAX_VALUE, true, true, true);
        
        // generation of master fill
        generateFill(newCell, wideOption, job.evenHorizontal, job.layersWithExports, job.iconParameters);

        // tiles generation. Flat generation for now
        generateTiles(newCell, fillCellName, tileList, job);
    }

    /**
     * Class to store tile information
     */
    private class TileInfo
    {
        int x, y; // size in x and y axes

        TileInfo(int a, int b)
        {
            x = a;
            y = b;
        }
    }

    /**
     * Method to generate tiles of a given cell according to the description provided.
     * The function will use the auto stitch tool so no hierarchy is expected.
     * @param template Cell to use as template
     * @param fillCellName Root name of the template cell
     * @param tileList List of dimensions to create
     * @param job Job running the task
     */
    private static void generateTiles(Cell template, String fillCellName, List<TileInfo> tileList, Job job)
    {
        if (tileList == null) return; // nothing to tile

        // tiles generation. Flat generation for now
        for (TileInfo info : tileList)
        {
            String newTileName = fillCellName + info.x + "x" + info.y + "{lay}";
            Cell newTile = Cell.makeInstance(template.getLibrary(), newTileName);
            // generate the array
            List<NodeInst> niList = new ArrayList<NodeInst>();
            Rectangle2D rect = template.getBounds();
            double width = rect.getWidth();
            double height = rect.getHeight();
            double xPos = 0, yPos;

            for (int i = 0; i < info.x; i++)
            {
                yPos = 0;
                for (int j = 0; j < info.y; j++)
                {
                    NodeInst newNi = NodeInst.makeInstance(template,
                    new Point2D.Double(xPos, yPos), width, height, newTile, Orientation.IDENT, null);
                    niList.add(newNi);
                    yPos += height;
                }
                xPos += width;
            }
            AutoOptions prefs = new AutoOptions();
            prefs.createExports = true;
            AutoStitch.runAutoStitch(newTile, niList, null, job, null, null, true, false, prefs, true, null);
        }
    }

    /**
     * Main class to auto-stitch metal layers found in given cell
     * @param theCell Cell with the metal layer information
     * @param wideOption True if exports in the lowest layer will be added
     * @param evenHor
     * @return True if auto-stitch ran without errors
     */
    private static boolean generateFill(Cell theCell, boolean wideOption, boolean evenHor, List<String> exportNames,
                                        IconParameters iconParameters)
    {
        EditingPreferences ep = theCell.getEditingPreferences();
        InteractiveRouter router  = new SimpleWirer(ep.fatWires);
        List<Layer> listOfLayers = new ArrayList<Layer>(12);

        for (Iterator<Layer> itL = theCell.getTechnology().getLayers(); itL.hasNext(); )
        {
            Layer l = itL.next();
            if (!l.getFunction().isMetal()) continue;  // only metals
            l.getFunction().getLevel();
            listOfLayers.add(l);
        }

        Collections.sort(listOfLayers, Layer.layerSortByFunctionLevel);
		Map<ArcProto,Integer> arcsCreatedMap = new HashMap<ArcProto,Integer>();
		Map<NodeProto,Integer> nodesCreatedMap = new HashMap<NodeProto,Integer>();
        List<Route> routeList = new ArrayList<Route>();
        Layer[] topLayers = new Layer[2];
        Layer bottomLayer = null;
        Map<String,Area> totalAreas = new HashMap<String,Area>();

        for (int i = 0; i < listOfLayers.size() - 1; i++)
        {
            Layer bottom = listOfLayers.get(i);
            boolean horizontal = isLayerHorizontal(bottom, evenHor);

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
//                boolean horizontal = DBMath.isGreaterThan(bounds.getWidth(), bounds.getHeight());
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
                    boolean insertedAlready = false;
                    for (PinsArcPair pp : pairs)
                    {
                        if (pp.insert.equals(insert) && pp.cut.equals(resultBnd))
                        {
                            insertedAlready = true;
                            break;
                        }
                    }
                    if (!insertedAlready)
                        pairs.add(new PinsArcPair(nai, insert, resultBnd));
                }

                Collections.sort(pairs, pinsArcSort);
                ArcInst mostLeft = ai;
                routeList.clear();

                if (bottomLayer == null)
                    bottomLayer = bottom;

                // Mark the layer as possible one of the top ones
                if (!pairs.isEmpty())
                {
                    topLayers[0] = bottom;
                    topLayers[1] = top;
                }

                Area a = remainingGeos.get(bottomName);
                for (PinsArcPair pair : pairs)
                {
                    Route r = router.planRoute(theCell, mostLeft, pair.topArc, pair.insert, null, true, true, pair.cut, null);
                    //mostLeft = bottomSplit.rightArc;
                    routeList.add(r);

                    // Extract area from possible remaining geometry. This will happen
                    // when the wires are in zig-zag
                    if (a != null) // there is a remainig area
                    {
                        Area remove = new Area(pair.cut);
                        a.subtract(remove);
                    }
                }
                for (Route r : routeList)
                {
                    Router.createRouteNoJob(r, theCell, arcsCreatedMap, nodesCreatedMap);
                }
            }

            // Adding remaining contacts due to non-100% overlap with original arcs
            for (Map.Entry<String,Area> e : remainingGeos.entrySet())
            {
                Area a = e.getValue();
                Netlist netlist = theCell.getNetlist();
                List<PolyBase> list = PolyBase.getPointsInArea(a, bottom, false, false);
                List<ArcInst> at = getArcsInGivenLayer(theCell.getArcs(), !horizontal, top, e.getKey(), netlist);
                List<PinsArcPair> pairs = new ArrayList<PinsArcPair>(2);

                // Add extra contacts. All points should be aligned in same horizontal or vertical line
                // first the top arcs. They should be less in number (less number of splits)
                for (PolyBase p : list)
                {
                    Rectangle2D resultBnd = p.getBounds2D();

                    // look for the first pair of arcs in bottom/top arcs that overlap with geometry
                    ArcInst topA = getArcInstOverlappingWithArea(resultBnd, at, horizontal, true); // perferct if fully contains the arc

                    if (topA != null)
                    {
                        EPoint insert = new EPoint(resultBnd.getCenterX(), resultBnd.getCenterY());
                        pairs.add(new PinsArcPair(topA, insert, resultBnd));
                    }
                }
                // Sort the new pairs from left/top to right/bottom
                Collections.sort(pairs, pinsArcSort);

                // Look for bottomLayer arcs for those given positions
                List<ArcInst> ab = getArcsInGivenLayer(theCell.getArcs(), horizontal, bottom, e.getKey(), netlist);
                routeList.clear();

                for (PinsArcPair pair : pairs)
                {
                    ArcInst bottomA = getArcInstOverlappingWithArea(pair.cut, ab, horizontal, false);
                    if (bottomA != null)
                    {
                        //SplitContainter bottomSplit = splitArcAtPoint(bottomA, pair.insert);
                        //SplitContainter topSplit = splitArcAtPoint(pair.topArc, pair.insert);
                        Route r = router.planRoute(theCell,  bottomA, pair.topArc, pair.insert, null, true, true, pair.cut, null);
                        // remove the old one and add new arc
                        ab.remove(bottomA);
                        //ab.add(bottomSplit.rightArc);
                        topLayers[0] = bottom;
                        topLayers[1] = top;
                        routeList.add(r);
                    }
                    else if (Job.getDebug())
                    {
                         getArcInstOverlappingWithArea(pair.cut, ab, horizontal, false);
                        System.out.println("AFG: It couldn't find bottom layer for " + pair.cut);
                    }
                }
                for (Route r : routeList)
                {
                    Router.createRouteNoJob(r, theCell, arcsCreatedMap, nodesCreatedMap);
                }

            }
        }

        // Connect cell instances
        for (Iterator<NodeInst> itNi = theCell.getNodes(); itNi.hasNext();)
        {
            NodeInst ni = itNi.next();

            // only cell instances
            if (!ni.isCellInstance()) continue;

            // Checking if the export overlaps with
            // Checking if there are exports overlapping with the arc
            List<String> doneExports = new ArrayList<String>();
            routeList.clear();
            SimpleWirer niRouter = new SimpleWirer(ep.fatWires);

            Netlist netlist = theCell.getNetlist();
            for (Iterator<Export> itE = ni.getExports(); itE.hasNext(); )
            {
                Export ex = itE.next();
                String exportName = ex.getName();
                String rootName = extractRootName(exportName);
                boolean getAllPossibleConnection = true;

                if (!getAllPossibleConnection && doneExports.contains(rootName))   // exportName
                    continue; // export for this given NodeInst was done

                PrimitiveNode n = ex.getBasePort().getParent();
                Layer layer = n.getLayerIterator().next(); // assuming only 1
                PortInst pi = ex.getOriginalPort();
                PortProto epi = pi.getPortProto();
                EPoint exportCenter = pi.getCenter();
                NodeProto np = epi.getParent();
                if (!(np instanceof Cell))
                    continue; // not good for now.
                Cell c = (Cell)np;
                Netlist netl = c.getNetlist();
                Network jExp = netl.getNetwork((Export)epi, 0);

                // Look for all arcs in the cell instances that are on the given layer and network
                Cell jCell = jExp.getParent();
                SearchInHierarchy searchElems = new SearchInHierarchy(jExp, layer);
                HierarchyEnumerator.enumerateCell(jCell, VarContext.globalContext, searchElems);
                Area expA = searchElems.expA;
                
                // Algorithm will look for the closest connection.
                Rectangle2D bestCut = null; // represents the best cut
                ArcInst bestArc = null;
                Point2D bestCenter = null;
                double bestDistance = Double.MAX_VALUE;
                List<Network> netList = getNetworkFromName(netlist, rootName);

                Area[] theInters = new Area[]{new Area(), new Area()}; // for intersections above and below of the given Layer
                Layer[] theLayers = new Layer[2];
                List<ArcInst> ab = new ArrayList<ArcInst>();

                for (Network jNet : netList)
                {
                    for (Iterator<ArcInst> itA = jNet.getArcs(); itA.hasNext();)
                    {
                        ArcInst ai = itA.next();
                        Layer l = ai.getProto().getLayer(0);

                        if (l == layer)
                            System.out.println("found in same layer");
                        else
                        {
                            int level = Layer.LayerSortByFunctionLevel.getNeighborLevel(l, layer);
                            if (Math.abs(level) == 1) // direct neightbor
                            {
                                int pos = (level == 1) ? 1 : 0;
                                // Not selecting the closest element yet
                                Area bnd = new Area(ai.getBounds());
                                bnd.intersect(expA);
                                if (!bnd.isEmpty())
                                {
                                    assert(theLayers[pos] == null || theLayers[pos] ==l);
                                    theInters[pos].add(bnd); // add piece to the big area
                                    theLayers[pos] = l;
                                    ab.add(ai);
                                }
                            }
                        }
                    }
                }

                for (int i = 0; i < theLayers.length; i++)
                {
                    Layer l = theLayers[i];
                    if (l == null) continue; // nothing found
                    Area a = theInters[i];
                    if (a.isEmpty()) continue; // nothing found

                    // get single polygons which represents the cuts
                    List<PolyBase> pols = Poly.getLoopsFromArea(a, l);
                    for (PolyBase p : pols)
                    {
                        Rectangle2D cut = p.getBounds2D();
                        Point2D center = new EPoint(cut.getCenterX(), cut.getCenterY());
                        ArcInst ai = getArcInstOverlappingWithArea(cut, ab);
                        boolean horOrVer = DBMath.areEquals(center.getX(), exportCenter.getX()) ||
                                DBMath.areEquals(center.getY(), exportCenter.getY());
                        // angled arcs are not allowed.
                        if (!horOrVer)
                            continue;

                        if (getAllPossibleConnection)
                        {
                            Route r = niRouter.planRoute(theCell, ai, pi, center, null, true, true, cut, null);
                            routeList.add(r);
                            doneExports.add(rootName);
                        }
                        else
                        {
                            double dist = DBMath.distBetweenPoints(center, exportCenter);
                            if (bestCenter == null || DBMath.isLessThan(dist, bestDistance))
                            {
                                // first time or the new distance is shorter
                                bestCenter = center;
                                bestCut = cut;
                                bestArc = ai;
                                bestDistance = dist;
                            }
                        }
                    }

                }

                if (!getAllPossibleConnection && bestCut != null) // best location found
                {
                    Route r = niRouter.planRoute(theCell,  bestArc, pi, bestCenter, null, true, true, bestCut, null);
                    routeList.add(r);
                    doneExports.add(rootName);
                }
            }

            for (Route r : routeList)
            {
                Router.createRouteNoJob(r, theCell, arcsCreatedMap, nodesCreatedMap);
            }
        }

        // Remove exports in lower layers
        Set<Export> toDelete = new HashSet<Export>();
        Map<String,Export> inBottomLayer = new HashMap<String,Export>(); // enough with 1 export stored

        Netlist netlist = theCell.getNetlist();
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
                // default case, two top layers
                int level = l.getFunction().getLevel();
                boolean exportOnLayer = exportNames.isEmpty() ? (level == levelMax || level == (levelMax-1)) : // old condition
                    exportNames.contains(l.getName());

                if (exportOnLayer) {
                    found = true;
                } else {
                    inBottomLayer.put(rootName, exp); // put the last one in the network.
                }
            }
            if (!found) // delete the export
                toDelete.add(exp);
        }

        // Add extra export in the middle of the botoom layer.
        if (wideOption)
        {
            Map<String,List<PinsArcPair>> newExports = new HashMap<String,List<PinsArcPair>>();
            boolean horizontal = isLayerHorizontal(bottomLayer, evenHor);
            assert netlist == theCell.getNetlist();

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
//                            boolean horizontal = !DBMath.isGreaterThan(bnd.getWidth(), bnd.getHeight());
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
                    Export.newInstance(theCell, split.splitPin.getPortInst(0), e.getKey(),
                        PortCharacteristic.UNKNOWN, iconParameters);
                }
            }
        }

        // Delete after dealing with the wide option
        // At least one connection was done otherwise it will keep the original exports.
        // Option is mostly for debugging purposes.
        if (topLayers[0] != null || topLayers[1] != null)
            theCell.killExports(toDelete);

        // make sure at least one export on a network is the root name
        List<Network> nets = new ArrayList<Network>();
        for (Iterator<Network> itN = theCell.getNetlist().getNetworks(); itN.hasNext(); )
            nets.add(itN.next());

        // Collect the data to modify to avoid concurrency issues
        Map<Export, String> map = new HashMap<Export, String>();
        for (Network net: nets) {
            if (!net.isExported()) continue;
            String name = net.getName();
            String rootName = extractRootName(name);
            if (!name.equals(rootName)) {
                Export exp = net.getExports().next();
                map.put(exp, rootName);
            }
        }
        // Do the actual change now
        for (Map.Entry<Export,String> e : map.entrySet())
        {
            e.getKey().rename(e.getValue());
        }
        return true;
    }

    /**
     * Class to detect other elements in the same network with same layer
     */
    private static class SearchInHierarchy extends HierarchyEnumerator.Visitor
    {
        Layer layer;
        Layer.Function.Set layerSet;
        Network jExp;
        Area expA;

        SearchInHierarchy(Network exp, Layer l)
        {
            jExp = exp;
            layer = l;
            layerSet = new Layer.Function.Set(l);
            expA = new Area();
        }

        public boolean enterCell(HierarchyEnumerator.CellInfo info)
		{
            Cell cell = info.getCell();
            AffineTransform rTrans = info.getTransformToRoot();

            // Checking only arcs
            for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
            {
                ArcInst ai = it.next();
                Network aNet = info.getNetlist().getNetwork(ai, 0);

                boolean found = HierarchyEnumerator.searchInExportNetwork(aNet, info, jExp);
                if (!found) continue; // different networks
                
                Technology tech = ai.getProto().getTechnology();
                Poly[] arcInstPolyList = tech.getShapeOfArc(ai);
                int tot = arcInstPolyList.length;
                for(int i=0; i<tot; i++)
                {
                    Poly poly = arcInstPolyList[i];
                    Layer l = poly.getLayer();

                    if (l != layer)
                        continue; // wrong layer
                    poly.transform(rTrans);
                    expA.add(new Area(poly.getBounds2D()));
                }
            }
            return true;
        }

        public void exitCell(HierarchyEnumerator.CellInfo info)
		{
        }

        public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
		{
            NodeInst ni = no.getNodeInst();

            if (ni.isCellInstance()) return true; // not interested in cells

            if (NodeInst.isSpecialNode(ni)) return true; // like center or pin

            // NOTE: in the past, only arcs were checked.
            // Now regular PrimitiveNodes need to be checked because the node extractor now adds pure layer nodes.

            // checking if any port in the node connects to the export in case
            boolean found = false;
            for (Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext();)
            {
                PortInst pi = pIt.next();
                Network nNet = info.getNetlist().getNetwork(no, pi.getPortProto(), 0);
                found = HierarchyEnumerator.searchInExportNetwork(nNet, info, jExp);
                if (found)
                    break; // found network
            }
            if (found)
            {
                Technology tech = ni.getProto().getTechnology();
                Poly[] nodeInstPolyList = tech.getShapeOfNode(ni, true, true, layerSet);
                assert nodeInstPolyList.length < 2; // either not found or only 1
                if (nodeInstPolyList.length == 1)
                {
                    AffineTransform rTrans = info.getTransformToRoot();
                    Poly poly = nodeInstPolyList[0];
                    poly.transform(rTrans);
                    expA.add(new Area(poly.getBounds2D()));
                }
            }

            return true;
        }
    }

    /**
         * Method to collect arcs found in a given area. It doesn't check if the arc is horizontal or top level
         * @param resultBnd Search area
         * @param at List with arcs to search from
         * @return
         */
        private static ArcInst getArcInstOverlappingWithArea(Rectangle2D resultBnd, List<ArcInst> at)
        {
            Area topArea = new Area(resultBnd);
            for (ArcInst ai : at)
            {
                Rectangle2D r = ai.getBounds();

                // test if the current ai inserts with the given area
                // and it is fully contained along the axis the arc is aligned
                if (r.intersects(resultBnd))
                {
                    return ai;
                }
            }
            return null;
        }

    /**
     * Method to collect arcs found in a given area
     * @param resultBnd Search area
     * @param at List with arcs to search from
     * @param horizontal
     * @param topLayer
     * @return
     */
    private static ArcInst getArcInstOverlappingWithArea(Rectangle2D resultBnd, List<ArcInst> at, boolean horizontal, boolean topLayer)
    {
        Area topArea = new Area(resultBnd);
        for (ArcInst ai : at)
        {
            Rectangle2D r = ai.getBounds();

            // test if the current ai inserts with the given area
            // and it is fully contained along the axis the arc is aligned
            if (r.intersects(resultBnd))
            {
                Area rArea = new Area(r);
                rArea.intersect(topArea);
                Rectangle2D rect = rArea.getBounds2D();
                boolean validArc;
//                boolean horizontal = !DBMath.isGreaterThan(r.getWidth(), r.getHeight());
                
                if (horizontal && topLayer || !horizontal && !topLayer)  // top arc is aligned along Y or bottom arc along Y
                {
                    validArc = DBMath.areEquals(rect.getWidth(), r.getWidth());
                }
                else // top arc is aligned along X
                {
                    validArc = DBMath.areEquals(rect.getHeight(), r.getHeight());
                }

                if (validArc)
                {
                    // found
                    return ai;
                }
            }
        }
        return null;
    }

    /**
     * Method to get Network from a given network root name
     * @param netlist Netlist containing the network information
     * @param rootName Root name of the network
     * @return Network
     */
    private static List<Network> getNetworkFromName(Netlist netlist, String rootName)
    {
        List<Network> list = new ArrayList<Network>();

        for (Iterator<Network> it = netlist.getNetworks(); it.hasNext();)
        {
            Network net = it.next();
            if (net.getName().startsWith(rootName))
                list.add(net);
        }
        return list;
    }

    /**
     * Method to determine if Layer is aligned horizontally
     * @param layer Given Layer
     * @param evenH True if even layers are horizontal
     * @return True of layer is horizontal
     */
    private static boolean isLayerHorizontal(Layer layer, boolean evenH)
    {
        int metalNumber = layer.getFunction().getLevel();
        return (evenH && metalNumber%2==0) || (!evenH && metalNumber%2==1);
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
    private static List<ArcInst> getArcsInGivenLayer(Iterator<ArcInst> itAi, boolean horizontal, Layer layer, String exportName, Netlist netlist)
    {
        List<ArcInst> arcs = new ArrayList<ArcInst>();

        for (; itAi.hasNext();)
        {
            ArcInst ai = itAi.next();

            // Checking arc orientation with respect to layer
            // If the orientation is not correct, then ignore it
            if (!isArcAligned(ai, horizontal))
                continue;

            Layer l = ai.getProto().getLayer(0);
            if (l != layer)
                continue;
            if (exportName != null)
            {
                Network jNet = netlist.getNetwork(ai, 0);
                Iterator<Export> itE = jNet.getExports();
                if (!itE.hasNext())
                {
                    if (Job.getDebug())
                        System.out.println("AFG: No export name associated to ArcInst '" + ai.getName() + "'");
                    continue; // no export
                }
                Export exp = itE.next(); // first is enough
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
     * @param ai the given ArcInst
     * @param horizontal True if the arc must be horizontal
     * @return True if the arc is aligned with the given direction
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
     * @param netlist Given network to search in
     * @return Non-null string with the root name. Null if no export was found.
     */
    private static String getExportRootName(ArcInst ai, Netlist netlist)
    {
        Network jNet = netlist.getNetwork(ai, 0);
        // Picking only 1 export and considering the root name`
        // Assuming at 1 export per arc
        if (jNet == null)
            return null;
        assert(jNet != null);
        Iterator<String> exportNames = jNet.getExportedNames();
        if (!exportNames.hasNext())
            return null; // no export
        return extractRootName(exportNames.next());
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
        {
            // based on X
            return (!DBMath.isGreaterThan(p1.getX(), p2.getX()));
//            if (Job.getDebug())
//                System.out.println("Case not considered in FillJob:isLeftTop");
//            assert(false); // not considered yet
        }
//        return false;
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
     * Class to store temp area according to a given direction
     */
//    private static class ExtraArea
//    {
//        Area[] areas = new Area[2]; // 0 -> bottom is horizontal, 1 -> vertical
//
//        ExtraArea(Area a, boolean hoz)
//        {
//            int pos = (hoz) ? 0 : 1;
//            areas[pos] = a;
//        }
//
//        Area getArea(boolean hoz)
//        {
//            int pos = (hoz) ? 0 : 1;
//            return areas[pos];
//        }
//
//        void setArea(Area a, boolean hoz)
//        {
//            int pos = (hoz) ? 0 : 1;
//            assert(areas[pos] == null);
//            areas[pos] = a;
//        }
//    }

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
