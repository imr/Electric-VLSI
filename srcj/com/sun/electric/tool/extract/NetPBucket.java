package com.sun.electric.tool.extract;

import com.sun.electric.database.geometry.*;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;

import java.util.Iterator;
import java.util.Collection;
import java.util.HashMap;
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
    protected GeometryHandler merge;
    protected HashMap mediaAxis; // to get the resistance value
    private String net;

    public NetPBucket(Cell cell, String net)
    {
        this.net = net;
        // Only 1 layer should be available per network
        merge = GeometryHandler.createGeometryHandler(GeometryHandler.ALGO_SWEEP, 1, cell.getBounds());
        mediaAxis = new HashMap(1);
    }

    private static class ResistanceBucket
    {
        private double length;
        private String net2;

        ResistanceBucket(double len, String net2)
        {
            this.length = len;
            this.net2 = net2;
        }
    }

    public void addResistance(Object layer, Object poly, String net2)
    {
        if (!(poly instanceof PolyBase))
        {
            System.out.println("Case not implemented in NetPBucket.addResistance");
        }

        HashMap map = (HashMap)mediaAxis.get(layer);
        ResistanceBucket bucket = null;
        double len = 0;
        if (map != null)
        {
            bucket = (ResistanceBucket)map.get(net2);
            if (bucket != null)
                len = bucket.length;
        }
        Rectangle2D rect = ((PolyBase)poly).getBounds2D();
        double w = rect.getWidth();
        double h = rect.getHeight();
        if (DBMath.areEquals(w, h))
        {
            // rectangle
            len += w; // for rectangle, the length of the media axis is identical to the side
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
            len += l;
        }
        if (map == null)
        {
            map = new HashMap(1);
            mediaAxis.put(layer, map);
        }
        if (bucket == null)
        {
            bucket = new ResistanceBucket(0, net2);
            map.put(net2, bucket);
        }
        bucket.length = len;
    }

    /**
     * Method to merge given Poly with rest of geometries in that layer for
     * that particular net
     * @param layer
     * @param poly
     */
    public void addCapacitance(Object layer, Object poly)
    {
        merge.add(layer, poly, false);
    }

    /**
     * Method to be used to retrieve information while printing the deck
     * @return
     */
    public String getInfo(double scale)
    {
        StringBuffer parasitic = new StringBuffer();
        boolean first = true;
        // Resistance values
        for (Iterator it = mediaAxis.keySet().iterator(); it.hasNext();)
        {
            Layer layer = (Layer)it.next();
            HashMap map = (HashMap)mediaAxis.get(layer);
            for (Iterator mapIt = map.keySet().iterator(); mapIt.hasNext();)
            {
                Object nameKey = mapIt.next();
                ResistanceBucket bucket = (ResistanceBucket)map.get(nameKey);
                if (!first)
                    parasitic.append("\n");
                first = false;
                parasitic.append("R " + net + " " + bucket.net2 + " " + bucket.length);
            }
        }

        // Capacitance values
        double areaV = 0, perimV = 0;

        for (Iterator it = merge.getKeyIterator(); it.hasNext();)
        {
            Layer layer = (Layer)it.next();
            if (layer.isDiffusionLayer()) continue;
            Collection c = merge.getObjects(layer, false, true);
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
        areaV *= scale * scale / 1000000; // area in square microns
        perimV *= scale / 1000;           // perim in microns
        double value = areaV + perimV;
        if (!GenMath.doublesEqual(value, 0))
        {
            if (!first) parasitic.append("\n");
            parasitic.append("C " + net + " gnd " + TextUtils.formatDouble(value, 2));
        }
        return parasitic.toString();
    }
}
