package com.sun.electric.database.geometry;

import java.awt.geom.Rectangle2D;

/**
 * Created by IntelliJ IDEA.
 * User: gg151869
 * Date: Mar 17, 2005
 * Time: 2:01:44 PM
 * To change this template use File | Settings | File Templates.
 */
/**
 * The intension of this interface is to make transparent transistion between
 * merge structures and the rest of the database classes
 */
public interface PolyNodeMerge 
{
    public PolyBase getPolygon();
    public Rectangle2D getBounds2D();
}
