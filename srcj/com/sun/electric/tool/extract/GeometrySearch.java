package com.sun.electric.tool.extract;

import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;

import java.util.Iterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Created by IntelliJ IDEA.
 * User: gg151869
 * Date: Aug 24, 2006
 * Time: 10:08:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class GeometrySearch
{
    public static void searchGeometries(Cell cell, EPoint point)
    {
        GeometrySearchEnumerator visitor = new GeometrySearchEnumerator(point);
        HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, visitor);
        System.out.println("Element found " + visitor.foundElement + " under point " + point);
    }

    /**************************************************************************************************************
     *  ConnectionEnumerator class
     **************************************************************************************************************/
    private static class GeometrySearchEnumerator extends HierarchyEnumerator.Visitor
    {
        private boolean found = false;
        private ERectangle geomBBnd;
        private Geometric foundElement;

        public GeometrySearchEnumerator(EPoint point)
        {
            this.geomBBnd = new ERectangle(point.getX(), point.getY(), 0, 0);
        }

        public boolean enterCell(HierarchyEnumerator.CellInfo info)
        {
            Cell cell = info.getCell();

            AffineTransform xformToRoot = null;

            try
            {
                xformToRoot = info.getTransformToRoot().createInverse();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            assert(xformToRoot!=null);
            Rectangle2D rect = new Rectangle2D.Double();
            rect.setRect(geomBBnd);
            DBMath.transformRect(rect, xformToRoot);

            for(Iterator<Geometric> it = cell.searchIterator(rect); it.hasNext(); )
            {
                Geometric geom = it.next();

                // PrimitiveInst or Cell
                if (geom instanceof NodeInst)
                {
                    NodeInst oNi = (NodeInst)geom;
                    if (oNi.isCellInstance())
                    {
                        // keep searching
                    }
                    else // primitive found
                    {
                        foundElement = geom;
                        found = true;
                    }
                }
                else // arc
                {
                    foundElement = geom;
                    found = true;
                }
            }
            return !found;
        }
        public void exitCell(HierarchyEnumerator.CellInfo info) {}
        public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
        {
            return true;
        }
    }
}
