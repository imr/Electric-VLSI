/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetPBucket.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;

import java.util.*;
import java.awt.geom.Rectangle2D;

/**
 * Class to describe extracted circuit information.
 */
public class NetPBucket implements ExtractedPBucket
{
    private GeometryHandler capMerge;
    private GeometryHandler resGeom; // to get the resistance value
    private HashMap<Layer,List<String>> resNameMap; // to collect subnetwork names
    private HashMap<Layer,List<PolyBase>> resSubGeom; // these are the contact geometries that should be substracted from merged area
    // For diffusion areas: source and drain
    private List<ExtractedPBucket> transistorsList;

    private String net;

    public NetPBucket(String net)
    {
        this.net = net;
        // Only 1 layer should be available per network
        capMerge = GeometryHandler.createGeometryHandler(GeometryHandler.GHMode.ALGO_SWEEP, 1);
        resGeom = GeometryHandler.createGeometryHandler(GeometryHandler.GHMode.ALGO_SWEEP, 1);
        resNameMap = new HashMap<Layer,List<String>>(1);
        resSubGeom = new HashMap<Layer,List<PolyBase>>(1); // contact geometries that will be substracted
    }

    public void addTransistor(ExtractedPBucket transBucket)
    {
        if (!(transBucket instanceof TransistorPBucket)) return; // nothing to do about it
        if (transistorsList == null) transistorsList = new ArrayList<ExtractedPBucket>();
        transistorsList.add(transBucket);
    }

    public void modifyResistance(Layer layer, PolyBase poly, String[] subNets, boolean add)
    {
        if (add)
            resGeom.add(layer, poly);
        else
        {
            List<PolyBase> list = resSubGeom.get(layer);
            if (list == null)
            {
                list = new ArrayList<PolyBase>(1);
                resSubGeom.put(layer, list);
            }
            list.add(poly);
        }

        List<String> list = resNameMap.get(layer);
        if (list == null)
        {
            list = new ArrayList<String>(2);
            resNameMap.put(layer, list);
        }

        // Among all names in the subNetwork, two distintic are required
        for (int i = 0; i < subNets.length; i++)
        {
            if (list.contains(subNets[i])) list.remove(subNets[i]);
            else list.add(subNets[i]);
        }
    }

    /**
     * Method to merge given Poly with rest of geometries in that layer for
     * that particular net
     * @param layer
     * @param poly
     */
    public void addCapacitance(Layer layer, Poly poly)
    {
        capMerge.add(layer, poly);
    }

    /**
     * Method to be used to retrieve information while printing the deck.
     */
    public String getInfo(Technology tech)
    {
        if (net.equalsIgnoreCase("gnd") && !tech.isGroundNetIncluded())
            return null;
        StringBuffer parasitic = new StringBuffer();
        boolean first = true;
        double scale = tech.getScale();

        // Resistance values
        for (Layer layer : resGeom.getKeySet())
        {
            Collection<PolyBase> c = resGeom.getObjects(layer, false, true);
            List<String> nameList = resNameMap.get(layer);
            if (nameList == null || nameList.size() != 2)
                continue;
            double value = 0;
            for (PolyBase poly : c)
            {
                Rectangle2D rect = ((PolyBase)poly).getBounds2D();
                double w = rect.getWidth();
                double h = rect.getHeight();
                if (DBMath.areEquals(w, h))
                {
                    // rectangle
                    value += 1; // l/w = 1 for rectangle, the length of the media axis is identical to the side
                }
                else
                {
                    double min, max;
                    if (w < h)
                    {
                        min = w;
                        max = h;
                    }
                    else
                    {
                        min = h;
                        max = w;
                    }
                    //
                    double l = max - min;
                    value += l/min; // values should be per square
                }
            }
            value *= layer.getResistance();
            if (!first)
                parasitic.append("\n");
            first = false;
            if (value > tech.getMinResistance())
                parasitic.append("r " + nameList.get(0) + " " + nameList.get(1) + " " + value);
        }

        // Capacitance values
        double areaV = 0, perimV = 0;

        for (Layer layer : capMerge.getKeySet())
        {
            if (layer.isDiffusionLayer()) continue;      // diffusion layers included in transistors
            Collection<PolyBase> c = capMerge.getObjects(layer, false, true);
            double area = 0, perim = 0;

            for (PolyBase poly : c)
            {
                area += poly.getArea();
                perim += poly.getPerimeter();
            }
            areaV += area * layer.getCapacitance();
            perimV += perim * layer.getEdgeCapacitance();
        }
        areaV *= ParasiticTool.getAreaScale(scale); // area in square microns
        perimV *= ParasiticTool.getPerimScale(scale);           // perim in microns
        double value = areaV + perimV;
        if (value > tech.getMinCapacitance())
        {
            if (!first) parasitic.append("\n");
            // IRSIM C values must be in fF
            parasitic.append("C " + net + " gnd " + TextUtils.formatDouble(value, 2));
        }
        return parasitic.toString();
    }

   /**
     * Method to perform operations after no more elemenets will
     * be added. Valid for PolySweepMerge
     * @param merge true if polygons must be merged otherwise non-overlapping polygons will be generated.
     */
    public void postProcess(boolean merge)
    {
       if (capMerge != null)
       {
           capMerge.postProcess(true);

           if (transistorsList != null && transistorsList.size() > 0)
           {
               double area = 0, perim = 0;

               for (Layer layer : capMerge.getKeySet())
               {
                    if (!layer.isDiffusionLayer()) continue;
                    Collection<PolyBase> c = capMerge.getObjects(layer, false, true);

                    for (PolyBase poly : c)
                    {
                        area += poly.getArea();
                        perim += poly.getPerimeter();
                    }
               }
               // Resistribute source/drain areas among transistors.
               area /= transistorsList.size();
               perim /= transistorsList.size();
               for (int i = 0; i < transistorsList.size(); i++)
               {
                   TransistorPBucket bucket = (TransistorPBucket)transistorsList.get(i);
                   bucket.addDifussionInformation(net, area, perim);
               }
           }
       }

       // Need to add exclusion before final calculation
       if (resGeom != null)
       {
           resGeom.subtractAll(resSubGeom);
           resGeom.postProcess(false);
       }
    }
}
