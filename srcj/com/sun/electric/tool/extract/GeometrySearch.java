package com.sun.electric.tool.extract;

import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.ArcProto;

import java.util.Iterator;
import java.util.HashMap;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Created by IntelliJ IDEA.
 * User: gg151869
 * Date: Aug 24, 2006
 * Time: 10:08:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class GeometrySearch extends HierarchyEnumerator.Visitor
{
    private boolean found = false;
    private ERectangle geomBBnd;
    private Geometric foundElement = null;
    private VarContext context = VarContext.globalContext;
    private boolean visibleObjectsOnly;
    private HashMap<PrimitiveNode,Boolean> cacheVisibilityNodes;
    private HashMap<ArcProto,Boolean> cacheVisibilityArcs;
    private int cellsProcessed;         // for debug

    public GeometrySearch() {
        cacheVisibilityNodes = new HashMap<PrimitiveNode,Boolean>();
        cacheVisibilityArcs = new HashMap<ArcProto,Boolean>();
    }

    /**
     * Find a Primitive Node or Arc at a point in a cell.  The geometric found may exist down
     * the hierarchy from the given cell.  Currently, it stops at the first
     * Geometric it finds.
     * @param cell the cell in which the point resides
     * @param point a point to search under
     * @param visibleObjectsOnly true to consider only Geometries that are visible
     * @return true if a Geometric was found, false if none was found
     */
    public boolean searchGeometries(Cell cell, EPoint point, boolean visibleObjectsOnly)
    {
        this.found = false;
        this.geomBBnd = new ERectangle(point.getX(), point.getY(), 0, 0);
        this.foundElement = null;
        this.context = VarContext.globalContext;
        this.visibleObjectsOnly = visibleObjectsOnly;
        this.cacheVisibilityArcs.clear();
        this.cacheVisibilityNodes.clear();
        this.cellsProcessed = 0;
        HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, this);
        return found;
    }

    public boolean foundGeometry() { return found; }

    public Geometric getGeometricFound() { return foundElement; }

    public VarContext getContext() { return context; }

    public int getCellsProcessed() { return cellsProcessed; }

    public String describeFoundGeometry() {
        String contextstr = "current cell";
        if (context != VarContext.globalContext) contextstr = context.getInstPath(".");
        return "Element " + foundElement + " in " + contextstr;
    }

    /**************************************************************************************************************
     *  Enumerator class
     **************************************************************************************************************/

    public boolean enterCell(HierarchyEnumerator.CellInfo info)
    {
        if (found) return false;
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
        cellsProcessed++;

        boolean continueDown = false;
        for(Iterator<Geometric> it = cell.searchIterator(rect, false); it.hasNext(); )
        {
            Geometric geom = it.next();

            // PrimitiveInst or Cell
            if (geom instanceof NodeInst)
            {
                NodeInst oNi = (NodeInst)geom;
                if (oNi.isCellInstance())
                {
                    // keep searching
                    continueDown = true;
                }
                else // primitive found
                {
                    // ignore nodes that and fully invisible
                    PrimitiveNode node = (PrimitiveNode)oNi.getProto();
                    if (visibleObjectsOnly && !isNodeVisible(node)) continue;
                    foundElement = geom;
                    context = info.getContext();
                    found = true;
                }
            }
            else // arc
            {
                // ignore arcs that and fully invisible
                ArcProto ap = ((ArcInst)geom).getProto();
                if (visibleObjectsOnly && !isArcVisible(ap)) continue;
                foundElement = geom;
                context = info.getContext();
                found = true;
            }
        }
        if (found) return false;
        return continueDown;
    }
    public void exitCell(HierarchyEnumerator.CellInfo info) {}
    public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
    {
        if (found) return false;
        if (visibleObjectsOnly && !no.getNodeInst().isExpanded()) return false;
        return true;
    }

    // ---------------------------------- Private ---------------------------

    private boolean isNodeVisible(PrimitiveNode node) {
        Boolean b = cacheVisibilityNodes.get(node);
        if (b == null) {
            boolean visible = false;
            for (Iterator<Layer> it2 = node.getLayerIterator(); it2.hasNext(); ) {
                Layer lay = it2.next();
                if (lay.isVisible()) {
                    visible = true; break;
                }
            }
            b = new Boolean(visible);
            cacheVisibilityNodes.put(node, b);
        }
        return b.booleanValue();
    }

    private boolean isArcVisible(ArcProto arc) {
        Boolean b = cacheVisibilityArcs.get(arc);
        if (b == null) {
            boolean visible = false;
            for (Iterator<Layer> it2 = arc.getLayerIterator(); it2.hasNext(); ) {
                Layer lay = it2.next();
                if (lay.isVisible()) {
                    visible = true; break;
                }
            }
            b = new Boolean(visible);
            cacheVisibilityArcs.put(arc, b);
        }
        return b.booleanValue();
    }
}
