package com.sun.electric.tool.extract;

import com.sun.electric.database.geometry.*;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;

import java.util.Iterator;
import java.util.Collection;

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
    protected GeometryHandler mediaAxis; // to get the resistance value
    private String net;

    public NetPBucket(Cell cell, String net)
    {
        this.net = net;
        // Only 1 layer should be available per network
        merge = GeometryHandler.createGeometryHandler(GeometryHandler.ALGO_SWEEP, 1, cell.getBounds());
    }

    /**
     * Method to merge given Poly with rest of geometries in that layer for
     * that particular net
     * @param layer
     * @param poly
     */
    public void add(Object layer, Object poly)
    {
        merge.add(layer, poly, false);
    }

    /**
     * Method to be used to retrieve information while printing the deck
     * @return
     */
    public String getInfo(double scale)
    {
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
        if (GenMath.doublesEqual(value, 0)) return null;
        return "C " + net + " gnd " + TextUtils.formatDouble(value, 2);
    }
}
