package com.sun.electric.tool.parasitic;

import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.geometry.*;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Generic;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda Garreton
 * Date: Sep 30, 2004
 * Time: 1:31:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class ParasiticTool extends Tool{

    /** The Parasitic Extraction tool */              private static ParasiticTool tool = new ParasiticTool();

    private ParasiticTool() { super("Parasitic");}


    /**
	 * Method to initialize the ERC tool.
	 */
	public void init()
	{
	}

    /**
     * Method to retrieve singlenton associated to parasitic tool
     * @return
     */
    public static ParasiticTool getParasiticTool() { return tool; }

    public void netwokParasitic(JNetwork network, Cell cell)
    {
        AnalyzeParasitic job = new AnalyzeParasitic(network, cell);
    }

    /****** inner class to store information ****/
    private static class ParasiticBucket
    {
        public Poly poly;
        /** Face area perpendicular to euclidean distance */ public double area;
        /** Width of cross section */ public double length;

        protected ParasiticBucket(Poly p, double a, double l)
        {
            poly = p;
            area = a;
            length = l;
        }
    }

    public static class ParasiticValue
    {
        private ParasiticBucket[] elements = new ParasiticBucket[2];
        /** True if faces are perpendicular */ private boolean angle;
        private double distance = -1;

        /**
         * Overwrite Object.toString()
         */
        public String toString()
        {
            String message = "";
            for (int i = 0; i < 2; i++)
                message += "Poly " + i + ":" + elements[i].poly.getBounds2D() +
                        "(area=" + elements[i].area + ", length=" +  elements[i].length + ") ";
            return (message);
        }

        /**
         * imilar to PolyBase.separationBox
         * @param p1
         * @param p2
         * @return
         */
        public static List initParasiticValues(Poly p1, Poly p2)
        {
            Rectangle2D thisBounds = p1.getBox();
            Rectangle2D otherBounds = p2.getBox();
            List values = new ArrayList();

            // Both polygons must be manhattan-shaped type
            if (thisBounds == null || otherBounds == null) return (null);

            int dir = -1;
            double [][] points1 = {{thisBounds.getMinX(), thisBounds.getMinY()},
                                   {thisBounds.getMaxX(), thisBounds.getMaxY()}};
            double [][] points2 = {{otherBounds.getMinX(), otherBounds.getMinY()},
                                   {otherBounds.getMaxX(), otherBounds.getMaxY()}};
            double pdx = Math.max(points2[0][PolyBase.X]-points1[1][PolyBase.X], points1[0][PolyBase.X]-points2[1][PolyBase.X]);
            double pdy = Math.max(points2[0][PolyBase.Y]-points1[1][PolyBase.Y], points1[0][PolyBase.Y]-points2[1][PolyBase.Y]);
            double area1 = 0, area2 = 0;
            double over1 = 0, over2 = 0;
            double length1 = 0, length2 = 0;
            if (pdx > 0 && pdy > 0) // Diagonal
            {
                // Two elements
                ParasiticValue newValue1 = new ParasiticValue();
                ParasiticValue newValue2 = new ParasiticValue();
                newValue1.distance = newValue2.distance = Math.sqrt(pdx*pdx + pdy*pdy);
                newValue1.angle = newValue2.angle = true;

                // Poly1:X v/s Poly2:Y
                length1 = p1.getBox().getWidth();
                length2 = p2.getBox().getHeight();
                area1 = p1.getLayer().getThickness() * p1.getBox().getHeight();
                area2 = p2.getLayer().getThickness() * p2.getBox().getWidth();
                newValue1.elements[0] = new ParasiticBucket(p1, area1, length1);
                newValue1.elements[1] = new ParasiticBucket(p2, area2, length2);

                // Poly1:X v/s Poly2:Y
                length1 = p1.getBox().getHeight();
                length2 = p2.getBox().getWidth();
                area1 = p1.getLayer().getThickness() * p1.getBox().getWidth();
                area2 = p2.getLayer().getThickness() * p2.getBox().getHeight();
                newValue2.elements[0] = new ParasiticBucket(p1, area1, length1);
                newValue2.elements[1] = new ParasiticBucket(p2, area2, length2);

                values.add(newValue1);
                values.add(newValue2);
            }
            else
            {
                if (pdx == 0 || pdy == 0)
                    System.out.println("How do I treat this?");
                ParasiticValue newValue = new ParasiticValue();
                newValue.angle = false;
                if (pdx > pdy)
                {
                    dir = PolyBase.X;
                    newValue.distance = pdx;
                    length1 = p1.getBox().getWidth();
                    length2 = p2.getBox().getWidth();
                }
                else
                {
                    dir = PolyBase.Y;
                    newValue.distance = pdy;
                    length1 = p1.getBox().getHeight();
                    length2 = p2.getBox().getHeight();
                }
                int oppDir = (dir+1)%2;
                over1 = over2 = Math.min(points1[1][oppDir], points2[1][oppDir]) -
                       Math.max(points1[0][oppDir], points2[0][oppDir]);
                area1 = p1.getLayer().getThickness() * over1;
                area2 = p2.getLayer().getThickness() * over2;
                newValue.elements[0] = new ParasiticBucket(p1, area1, length1);
                newValue.elements[1] = new ParasiticBucket(p2, area2, length2);

                values.add(newValue);
            }
            return values;
        }
    }

    /****************************** OPTIONS ******************************/

    /****************************** Job ******************************/
    protected class AnalyzeParasitic extends Job
    {
        Cell cell;
        JNetwork net;

        protected AnalyzeParasitic(JNetwork network, Cell cell)
        {
            super ("Analyze Network "+ network.describe(), tool, Job.Type.EXAMINE, null, cell, Job.Priority.USER);
            this.net = network;
            this.cell = cell;
            this.startJob();
        }

        private List getClosestPolys(Cell cell, Geometric geom, Technology tech, Poly poly, Rectangle2D bounds)
        {
            List polyList = new ArrayList();

            for(Iterator it = cell.searchIterator(bounds); it.hasNext(); )
            {
                Geometric nGeom = (Geometric)it.next();
			    if ((nGeom == geom)) continue;

                // Touching elements out
                if (Geometric.objectsTouch(nGeom, geom)) continue;

                if (nGeom instanceof NodeInst)
                {
                    NodeInst ni = (NodeInst)nGeom;
                    NodeProto np = ni.getProto();

                    if (np == Generic.tech.cellCenterNode) continue;

                    if (np.getFunction() == NodeProto.Function.PIN) continue;

                    // don't check between technologies
					if (np.getTechnology() != tech) continue;

                    // ignore nodes that are not primitive
                    if (np instanceof Cell)
                        System.out.println("Skipping case for now");
                    else
                    {
                        //if (layer.isNonElectrical()) continue; // Valid for Pins
                        System.out.println("Found node " + nGeom + " for " + geom);
                        AffineTransform trans = ni.rotateOut();
                        Poly [] nodeInstPolyList = tech.getShapeOfNode(ni);
                        for(int i = 0; i < nodeInstPolyList.length; i++)
                        {
                            Poly nPoly = nodeInstPolyList[i];
                            Layer layer = nPoly.getLayer();
                            if (!layer.getFunction().isMetal()) continue;
                            nPoly.transform(trans);
                            polyList.addAll(ParasiticValue.initParasiticValues(poly, nPoly));
                        }
                    }
                } else
                { // Arc then

                    ArcInst ai = (ArcInst)nGeom;
				    ArcProto ap = ai.getProto();

                    // don't check between technologies
					if (ap.getTechnology() != tech) continue;

                    System.out.println("Found arc " + nGeom + " for " + geom);
                    Poly [] arcInstPolyList = tech.getShapeOfArc(ai);
                    for(int i = 0; i < arcInstPolyList.length; i++)
                    {
                        Poly nPoly = arcInstPolyList[i];
                        Layer layer = nPoly.getLayer();
                        if (!layer.getFunction().isMetal()) continue;
                        polyList.addAll(ParasiticValue.initParasiticValues(poly, nPoly));
                    }
                }
            }
            return polyList;
        }

        /**
         * Implementation of Job.doIt() abstract function
         * @return
         */
        public boolean doIt()
        {
            long startTime = System.currentTimeMillis();
            System.out.println("Extracting Parasitic for '" + cell.libDescribe() +
                    "' network '" + net.describe() + "'");
            Rectangle2D bounds = new Rectangle2D.Double();
            double maxDistance = 20;
            List polyToCheckList = new ArrayList();

            // Selecting arcs attached to this network
            for (Iterator it = net.getArcs(); it.hasNext(); )
            {
                ArcInst ai = (ArcInst)it.next();
                Technology tech = ai.getProto().getTechnology();
				Poly [] arcInstPolyList = tech.getShapeOfArc(ai);
                for(int i = 0; i < arcInstPolyList.length; i++)
				{
					Poly poly = arcInstPolyList[i];
					Layer layer = poly.getLayer();
                    if (!layer.getFunction().isMetal()) continue;
                    //polyToCheckList.add(poly);
                    Rectangle2D bnd = poly.getBounds2D();
                    bounds.setRect(bnd.getMinX()-maxDistance, bnd.getMinY()-maxDistance,
                            bnd.getWidth()+maxDistance*2, bnd.getHeight()+maxDistance*2);
                    polyToCheckList.addAll(getClosestPolys(cell, ai, tech, poly, bounds));
                    //merge.add(layer, new PolyQTree.PolyNode(poly), false);
                }
            }

            // Adding related nodes, searching per ports
	        HashMap nodeMap = new HashMap();
            for (Iterator it = net.getPorts(); it.hasNext(); )
            {
                PortInst pi = (PortInst)it.next();
                NodeInst ni = pi.getNodeInst();
	            if (nodeMap.get(ni) != null) continue; // Already analyzed
                AffineTransform trans = ni.rotateOut();
                NodeProto np = ni.getProto();
	            nodeMap.put(ni, ni);
				if (np instanceof PrimitiveNode)
				{
					PrimitiveNode pNp = (PrimitiveNode)np;
					Technology tech = pNp.getTechnology();
					Poly [] nodeInstPolyList = tech.getShapeOfNode(ni);
					for(int i = 0; i < nodeInstPolyList.length; i++)
					{
						Poly poly = nodeInstPolyList[i];
						Layer layer = poly.getLayer();
                        if (!layer.getFunction().isMetal()) continue;
						poly.transform(trans);
                        Rectangle2D bnd = poly.getBounds2D();
                        bounds.setRect(bnd.getMinX()-maxDistance, bnd.getMinY()-maxDistance,
                                bnd.getWidth()+maxDistance*2, bnd.getHeight()+maxDistance*2);
                         polyToCheckList.addAll(getClosestPolys(cell, ni, tech, poly, bounds));
						//merge.add(layer, new PolyQTree.PolyNode(poly), false);
					}
                }
                else
                    System.out.println("Not implemented");
            }
            for (Iterator it = polyToCheckList.iterator(); it.hasNext(); )
            {
                ParasiticValue val = (ParasiticValue)it.next();
                System.out.println("Value " + val + " Layer " + val.elements[1].poly.getLayer());
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Done (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
            return true;
        }
    }
}
