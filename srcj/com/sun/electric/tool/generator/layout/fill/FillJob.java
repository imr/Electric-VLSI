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
import com.sun.electric.tool.routing.InteractiveRouter;
import com.sun.electric.tool.routing.SimpleWirer;
import com.sun.electric.tool.routing.Route;
import com.sun.electric.tool.routing.Router;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.ArcProto;

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

            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
            startJob(); // queue the job
    }

    public boolean doIt()
    {                           
        InteractiveRouter router  = new SimpleWirer();
        Map<Layer, List<ArcInst>> map = new HashMap<Layer, List<ArcInst>>();

        for (Iterator<ArcInst> itAi = topCell.getArcs(); itAi.hasNext();)
        {
            ArcInst ai = itAi.next();
            assert(ai.getProto().getNumArcLayers() == 1); // only 1 for now
            Layer l = ai.getProto().getLayer(0);
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

        for (int i = 0; i < listOfLayers.size() - 1; i++)
        {
            Layer bottom = listOfLayers.get(i);
            List<ArcInst> arcs = map.get(bottom);

            if (arcs == null) continue; // nothing to stitch

            Layer top = listOfLayers.get(i+1);
            for (ArcInst ai : arcs)
            {
                Rectangle2D bounds = ai.getBounds();
                Area bottomA = new Area(bounds);
                // Picking only 1 export and considering the root name`
                String bottomName = getExportRootName(ai);

                if (bottomName == null) continue; // nothing to export

                for(Iterator<RTBounds> it = topCell.searchIterator(bounds); it.hasNext(); )
                {
                    Geometric nGeom = (Geometric)it.next();

                    if (!(nGeom instanceof ArcInst)) continue; // only arcs

                    ArcInst nai = (ArcInst)nGeom;

                    assert(nai.getProto().getNumArcLayers() == 1); // only 1 for now

                    Layer nl = nai.getProto().getLayer(0);

                    if (nl != top) continue; // ascending order is easy

                    String topName = getExportRootName(nai);

                    if (topName == null || !topName.equals(bottomName)) continue; // no export matching

                    Rectangle2D nBnds = nai.getBounds();
                    Area topA = new Area(nBnds);
                    topA.intersect(bottomA);
                    Rectangle2D resultBnd = topA.getBounds2D();
                    EPoint insert = new EPoint(resultBnd.getCenterX(), resultBnd.getCenterY());
                    NodeInst bottomPin = insertJogInArc(ai, insert);
                    NodeInst topPin = insertJogInArc(nai, insert);

                    Route r = router.planRoute(topCell,  bottomPin, topPin, insert, null, true, true);
                    Router.createRouteNoJob(r, topCell, false, arcsCreatedMap, nodesCreatedMap);
                }
            }
        }
        return true;
    }

    private String getExportRootName(ArcInst ai)
    {
        // Picking only 1 export and considering the root name`
        // Assuming at 1 export per arc
        Export exp;
        if (ai.getTailPortInst().getNodeInst().getNumExports() > 0)
            exp = ai.getTailPortInst().getNodeInst().getExports().next();
        else if (ai.getHeadPortInst().getNodeInst().getNumExports() > 0)
            exp = ai.getHeadPortInst().getNodeInst().getExports().next();
        else
        {
            System.out.println("Should this happen?");
            return null;
        }
        String rootName = exp.getName();
        int index = rootName.indexOf("_");
        if (index != -1) // remove any character after _
            rootName = rootName.substring(0, index);
        return rootName;
    }

    private NodeInst insertJogInArc(ArcInst ai, EPoint insert)
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


        // get location of connection to these pins
        PortInst pi = ni.getOnlyPortInst();

        // now save the arc information and delete it
        PortInst headPort = ai.getHeadPortInst();
        PortInst tailPort = ai.getTailPortInst();
        Point2D headPt = ai.getHeadLocation();
        Point2D tailPt = ai.getTailLocation();
        double width = ai.getLambdaBaseWidth();
        String arcName = ai.getName();

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
        ai.kill();
        if (arcName != null)
        {
            if (headPt.distance(insert) > tailPt.distance(insert))
            {
                newAi1.setName(arcName);
                newAi1.copyTextDescriptorFrom(ai, ArcInst.ARC_NAME);
            } else
            {
                newAi2.setName(arcName);
                newAi2.copyTextDescriptorFrom(ai, ArcInst.ARC_NAME);
            }
        }
        return ni;
    }
}
