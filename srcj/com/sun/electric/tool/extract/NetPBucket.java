package com.sun.electric.tool.extract;

import com.sun.electric.database.geometry.*;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;

import java.util.*;
import java.awt.geom.Rectangle2D;

/**
 * Created by IntelliJ IDEA.
 * User: gg151869
 * Date: Mar 17, 2005
 * Time: 10:14:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class NetPBucket implements ExtractedPBucket
{
    private GeometryHandler capMerge;
    private GeometryHandler resGeom; // to get the resistance value
    private HashMap resNameMap; // to collect subnetwork names
    private HashMap resSubGeom; // these are the contact geometries that should be substracted from merged area
    // For diffusion areas: source and drain
    private List transistorsList;

    private String net;

    public NetPBucket(Cell cell, String net)
    {
        this.net = net;
        // Only 1 layer should be available per network
        capMerge = GeometryHandler.createGeometryHandler(GeometryHandler.ALGO_SWEEP, 1, cell.getBounds());
        resGeom = GeometryHandler.createGeometryHandler(GeometryHandler.ALGO_SWEEP, 1, cell.getBounds());
        resNameMap = new HashMap(1);
        resSubGeom = new HashMap(1); // contact geometries that will be substracted
    }

    public void addTransistor(ExtractedPBucket transBucket)
    {
        if (!(transBucket instanceof TransistorPBucket)) return; // nothing to do about it
        if (transistorsList == null) transistorsList = new ArrayList();
        transistorsList.add(transBucket);
    }

    public void modifyResistance(Object layer, Object poly, String[] subNets, boolean add)
    {
        if (add)
            resGeom.add(layer, poly, false);
        else
        {
            List list = (List)resSubGeom.get(layer);
            if (list == null)
            {
                list = new ArrayList(1);
                resSubGeom.put(layer, list);
            }
            list.add(poly);
        }

        List list = (List)resNameMap.get(layer);
        if (list == null)
        {
            list = new ArrayList(2);
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
    public void addCapacitance(Object layer, Object poly)
    {
        capMerge.add(layer, poly, true);
    }

    /**
     * Method to be used to retrieve information while printing the deck
     * @return
     */
    public String getInfo(Technology tech)
    {
        if (net.equalsIgnoreCase("gnd") && !tech.isGroundNetIncluded())
            return null;
        StringBuffer parasitic = new StringBuffer();
        boolean first = true;
        double scale = tech.getScale();

        // Resistance values
        for (Iterator it = resGeom.getKeyIterator(); it.hasNext();)
        {
            Layer layer = (Layer)it.next();
            Collection c = resGeom.getObjects(layer, false, true);
            List nameList = (List)resNameMap.get(layer);
            if (nameList == null || nameList.size() != 2)
                continue;
            double value = 0;
            for (Iterator i = c.iterator(); i.hasNext(); )
            {
                PolyNodeMerge node = (PolyNodeMerge)i.next();
                PolyBase poly = node.getPolygon();
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

        for (Iterator it = capMerge.getKeyIterator(); it.hasNext();)
        {
            Layer layer = (Layer)it.next();
            if (layer.isDiffusionLayer()) continue;      // diffusion layers included in transistors
            Collection c = capMerge.getObjects(layer, false, true);
            double area = 0, perim = 0;

            for (Iterator i = c.iterator(); i.hasNext(); )
            {
                PolyNodeMerge node = (PolyNodeMerge)i.next();
                PolyBase poly = node.getPolygon();
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
               for (Iterator it = capMerge.getKeyIterator(); it.hasNext();)
               {
                    Layer layer = (Layer)it.next();
                    if (!layer.isDiffusionLayer()) continue;
                    Collection c = capMerge.getObjects(layer, false, true);

                    for (Iterator i = c.iterator(); i.hasNext(); )
                    {
                        PolyNodeMerge node = (PolyNodeMerge)i.next();
                        PolyBase poly = node.getPolygon();
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
