/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ParasiticTool.java
 * Written by: Gilda Garreton, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.simulation.Simulation;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ParasiticTool extends Tool{

    /** The Parasitic Extraction tool */              private static ParasiticTool tool = new ParasiticTool();

    private ParasiticTool() { super("Parasitic");}


    /**
	 * Method to initialize the Parasitics tool.
	 */
	public void init()
	{
	}

    /**
     * Method to retrieve singlenton associated to parasitic tool
     */
    public static ParasiticTool getParasiticTool() { return tool; }

    public void netwokParasitic(Network network, Cell cell)
    {
        AnalyzeParasitic job = new AnalyzeParasitic(network, cell);
    }

    public static List calculateParasistic(ParasiticGenerator tool, Cell cell, VarContext context)
    {
        Netlist netList = cell.getNetlist(false);
        if (context == null) context = VarContext.globalContext;
        ParasiticVisitor visitor = new ParasiticVisitor(tool, netList, context);
        HierarchyEnumerator.enumerateCell(cell, context, netList, visitor);
        List list = visitor.getParasitics();
        return list;
    }

    private static class ParasiticVisitor extends HierarchyEnumerator.Visitor
	{
		private HashMap netMap;
        private Netlist netList;
        private List transAndRCList = new ArrayList();
        private ParasiticGenerator tool;
        private VarContext context;

        public List getParasitics()
        {
            List list = new ArrayList(transAndRCList);

            for (Iterator it = netMap.keySet().iterator(); it.hasNext();)
            {
                Object key = it.next();
                Object value = netMap.get(key);
                if (value != null) list.add(value);
            }
            return list;
        }

        public HierarchyEnumerator.CellInfo newCellInfo() { return new ParasiticCellInfo(); }

		public ParasiticVisitor(ParasiticGenerator tool, Netlist netList, VarContext context)
		{
            this.netList = netList;
            this.tool = tool;
            netMap = new HashMap(netList.getNumNetworks());
            this.context = context;
		}

        /**
         * @param info
         */
		public void exitCell(HierarchyEnumerator.CellInfo info)
        {
            // Done with root cell
            if (info.getParentInfo() == null)
            {
                for (Iterator it = netMap.keySet().iterator(); it.hasNext();)
                {
                    Object obj = it.next();
                    NetPBucket bucket = (NetPBucket)netMap.get(obj);
                    GeometryHandler merge = bucket.merge;
                    if (merge != null) merge.postProcess();
                }
            }
        }

        private NetPBucket getNetParasiticsBucket(Network net, HierarchyEnumerator.CellInfo info)
        {
            NetPBucket parasiticNet = (NetPBucket)netMap.get(net);
            int numRemoveParents = context.getNumLevels();
            
            if (parasiticNet == null)
            {
                String name = info.getUniqueNetNameProxy(net, "/").toString(numRemoveParents);
                parasiticNet = new NetPBucket(info.getCell(), name);
                netMap.put(net, parasiticNet);
            }
            return parasiticNet;
        }

		public boolean enterCell(HierarchyEnumerator.CellInfo info)
		{
            ParasiticCellInfo iinfo = (ParasiticCellInfo)info;
            iinfo.extInit();
            int numRemoveParents = context.getNumLevels();

            for(Iterator aIt = info.getCell().getArcs(); aIt.hasNext(); )
            {
                ArcInst ai = (ArcInst)aIt.next();

                // don't count non-electrical arcs
                if (ai.getProto().getFunction() == ArcProto.Function.NONELEC) continue;

                Network net = netList.getNetwork(ai, 0);
                if (net == null)
                    continue;

                PortInst tailP = ai.getTail().getPortInst();
                PortInst headP = ai.getHead().getPortInst();
                Network net1 = netList.getNetwork(tailP);
                Network net2 = netList.getNetwork(headP);
                if (net1 != net) net2 = net1;

                NetPBucket parasiticNet = getNetParasiticsBucket(net, info);
                if (parasiticNet == null)
                    continue;

                String net2Name = info.getUniqueNetNameProxy(net2, "/").toString(numRemoveParents);
                boolean isDiffArc = ai.isDiffusionArc();    // check arc function

                Technology tech = ai.getProto().getTechnology();
                Poly [] arcInstPolyList = tech.getShapeOfArc(ai);
                int tot = arcInstPolyList.length;
                for(int j=0; j<tot; j++)
                {
                    Poly poly = arcInstPolyList[j];
                    if (poly.getStyle().isText()) continue;

                    Layer layer = poly.getLayer();
                    if (layer.getTechnology() != Technology.getCurrent()) continue;
                    if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;

                    if (layer.isDiffusionLayer() || (!isDiffArc && layer.getCapacitance() > 0.0))
                        parasiticNet.addCapacitance(layer, poly);
                    if (layer.getResistance() > 0)
                        parasiticNet.addResistance(layer, poly, net2Name);
                }
            }

            return (true);
		}

        /**
         *
         * @param no
         * @param info
         * @return
         */
		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
		{
            NodeInst ni = no.getNodeInst();
            NodeProto np = ni.getProto();
            if (np instanceof Cell) return true;  // hierarchical

            // Its like pins, facet-center
			if (NodeInst.isSpecialNode(ni)) return (false);

            // initialize to examine the polygons on this node
            Technology tech = np.getTechnology();
            AffineTransform trans = ni.rotateOut();
            ExtractedPBucket parasitic = tool.createBucket(ni, netList, (ParasiticCellInfo)info);
            if (parasitic != null) transAndRCList.add(parasitic);

            Poly [] polyList = tech.getShapeOfNode(ni, null, true, true, null);
            int tot = polyList.length;
            for(int i=0; i<tot; i++)
            {
                Poly poly = polyList[i];

                // make sure this layer connects electrically to the desired port
                PortProto pp = poly.getPort();
                if (pp == null) continue;
                Network net = netList.getNetwork(ni, pp, 0);
                if (net == null)
                    continue;

                // don't bother with layers without capacity
                Layer layer = poly.getLayer();
                if (!layer.isDiffusionLayer() && layer.getCapacitance() == 0.0) continue;
                if (layer.getTechnology() != Technology.getCurrent()) continue;

                // leave out the gate capacitance of transistors
                if (layer.getFunction() == Layer.Function.GATE) continue;

                NetPBucket parasiticNet = getNetParasiticsBucket(net, info);
                if (parasiticNet == null)
                    continue;

                // get the area of this polygon
                poly.transform(trans);
                parasiticNet.addCapacitance(layer, poly);
            }
			return (true);
		}
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
	        StringBuffer buf = new StringBuffer();
            for (int i = 0; i < 2; i++)
                buf.append("Poly " + i + ":" + elements[i].poly.getBounds2D() +
                        "(area=" + elements[i].area + ", length=" +  elements[i].length + ") ");
            return (buf.toString());
        }

        /**
         * similar to PolyBase.separationBox
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
            double thickP1 = p1.getLayer().getThickness();
            double thickP2 = p1.getLayer().getThickness();

            // 1 for now
            thickP1 = thickP2 = 1;
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
                area1 = thickP1 * p1.getBox().getHeight();
                area2 = thickP2 * p2.getBox().getWidth();
                newValue1.elements[0] = new ParasiticBucket(p1, area1, length1);
                newValue1.elements[1] = new ParasiticBucket(p2, area2, length2);

                // Poly1:X v/s Poly2:Y
                length1 = p1.getBox().getHeight();
                length2 = p2.getBox().getWidth();
                area1 = thickP1 * p1.getBox().getWidth();
                area2 = thickP2 * p2.getBox().getHeight();
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
                area1 = thickP1 * over1;
                area2 = thickP2 * over2;
                newValue.elements[0] = new ParasiticBucket(p1, area1, length1);
                newValue.elements[1] = new ParasiticBucket(p2, area2, length2);

                values.add(newValue);
            }
            return values;
        }
    }

    /****************************** OPTIONS ******************************/

    /****************************** Job ******************************/
    protected static class AnalyzeParasitic extends Job
    {
        Cell cell;
        Network net;

        protected AnalyzeParasitic(Network network, Cell cell)
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

                    if (np.getFunction() == PrimitiveNode.Function.PIN) continue;

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
	                    nPoly = ai.cropPerLayer(nPoly);
                        polyList.addAll(ParasiticValue.initParasiticValues(poly, nPoly));
                    }
                }
            }
            return polyList;
        }

        /**
         * Implementation of Job.doIt() abstract function
         */
        public boolean doIt()
        {
            long startTime = System.currentTimeMillis();
            System.out.println("Extracting Parasitic for '" + cell.libDescribe() +
                    "' network '" + net.describe() + "'");
            Rectangle2D bounds = new Rectangle2D.Double();
            double maxDistance = getMaxDistance();
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
	                poly = ai.cropPerLayer(poly);
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

    //----------------------------IRSIM Cell Info for HierarchyEnumerator--------------------

    /** Parasitic Cell Info class */
    public static class ParasiticCellInfo extends HierarchyEnumerator.CellInfo
    {
        /** M-factor to be applied to size */       private float mFactor;

        /** initialize ParasiticCellInfo: called from enterCell */
        protected void extInit()
        {
            HierarchyEnumerator.CellInfo parent = getParentInfo();
            if (parent == null) mFactor = 1f;
            	else mFactor = ((ParasiticCellInfo)parent).getMFactor();
            // get mfactor from instance we pushed into
            Nodable ni = getContext().getNodable();
            if (ni == null) return;
            Variable mvar = ni.getVar(Simulation.M_FACTOR_KEY);
            if (mvar == null) return;
            Object mval = getContext().evalVar(mvar, null);
            if (mval == null) return;
            mFactor = mFactor * VarContext.objectToFloat(mval, 1f);
        }

        /** get mFactor */
        public float getMFactor() { return mFactor; }
    }

    /** PREFERENCES */
    private static Pref cacheMaxDistance = Pref.makeDoublePref("MaximumDistance", ParasiticTool.tool.prefs, 20);
	/**
	 * Method to get maximum dstance for searching window
	 * @return double representing the preference
	 */
	public static double getMaxDistance() { return cacheMaxDistance.getDouble(); }
	/**
	 * Method to set maximum distance to use during searching window
	 * @param value to set
	 */
	public static void setMaxDistance(double value) { cacheMaxDistance.setDouble(value); }
}
