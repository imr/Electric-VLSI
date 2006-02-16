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

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Network;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Class to implement parasitic extraction.
 */
public class ParasiticTool extends Tool {

    /** The Parasitic Extraction tool */              private static ParasiticTool tool = new ParasiticTool();

    private static ErrorLogger errorLogger = ErrorLogger.newInstance("Parasitics Extraction");;

    private ParasiticTool() { super("parasitic");}


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

    public static ErrorLogger getParasiticErrorLogger() { return errorLogger; }

    public static double getAreaScale(double scale) { return scale*scale/1000000; } // area in square microns

    public static double getPerimScale(double scale) { return scale/1000; }          // perim in microns

    public void netwokParasitic(Network network, Cell cell)
    {
        new AnalyzeParasitic(network, cell);
    }

    public static List<Object> calculateParasistic(ParasiticGenerator tool, Cell cell, VarContext context)
    {
        errorLogger.clearLogs(cell);
//        Netlist netList = cell.getNetlist(false);
        if (context == null) context = VarContext.globalContext;
        ParasiticVisitor visitor = new ParasiticVisitor(tool, context);
        HierarchyEnumerator.enumerateCell(cell, context, visitor);
//        HierarchyEnumerator.enumerateCell(cell, context, netList, visitor);
        List<Object> list = visitor.getParasitics();
        return list;
    }

    private static class ParasiticVisitor extends HierarchyEnumerator.Visitor
	{
		private HashMap<Network,NetPBucket> netMap;
        //private Netlist netList;
        private List<Object> transAndRCList = new ArrayList<Object>();
        private ParasiticGenerator tool;
        private VarContext context;

        public List<Object> getParasitics()
        {
            List<Object> list = new ArrayList<Object>(transAndRCList);

            for (Network net : netMap.keySet())
            {
                Object value = netMap.get(net);
                if (value != null) list.add(value);
            }
            return list;
        }

        public HierarchyEnumerator.CellInfo newCellInfo() { return new ParasiticCellInfo(); }

		public ParasiticVisitor(ParasiticGenerator tool, VarContext context)
		{
            //this.netList = netList;
            this.tool = tool;
            netMap = new HashMap<Network,NetPBucket>();
//            netMap = new HashMap(netList.getNumNetworks());
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
                for (Network net : netMap.keySet())
                {
                    NetPBucket bucket = netMap.get(net);
                    bucket.postProcess(false);
                }
            }
        }

        private NetPBucket getNetParasiticsBucket(Network net, HierarchyEnumerator.CellInfo info)
        {
            NetPBucket parasiticNet = netMap.get(net);
            int numRemoveParents = context.getNumLevels();
            
            if (parasiticNet == null)
            {
                String name = info.getUniqueNetNameProxy(net, "/").toString(numRemoveParents);
                parasiticNet = new NetPBucket(name);
                netMap.put(net, parasiticNet);
            }
            return parasiticNet;
        }

		public boolean enterCell(HierarchyEnumerator.CellInfo info)
		{
            ParasiticCellInfo iinfo = (ParasiticCellInfo)info;
            iinfo.extInit();
            int numRemoveParents = context.getNumLevels();

            for(Iterator<ArcInst> aIt = info.getCell().getArcs(); aIt.hasNext(); )
            {
                ArcInst ai = aIt.next();

                // don't count non-electrical arcs
                if (ai.getProto().getFunction() == ArcProto.Function.NONELEC) continue;

                Network net = info.getNetlist().getNetwork(ai, 0); // netList.getNetwork(ai, 0);
                if (net == null)
                    continue;

                PortInst tailP = ai.getTailPortInst();
                PortInst headP = ai.getHeadPortInst();
//                Network net1 = netList.getNetwork(tailP);
//                Network net2 = netList.getNetwork(headP);
//                if (net1 != net) net2 = net1;

                NetPBucket parasiticNet = getNetParasiticsBucket(net, info);
                if (parasiticNet == null)
                    continue;
                String netName = info.getUniqueNetNameProxy(net, "/").toString(numRemoveParents);
                String net1Name = netName+"_"+tailP.getNodeInst().getName();
                String net2Name = netName+"_"+headP.getNodeInst().getName();
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

                    if (isDiffArc || layer.getCapacitance() > 0.0)
                    //if (layer.isDiffusionLayer() || (!isDiffArc && layer.getCapacitance() > 0.0))
                        parasiticNet.addCapacitance(layer, poly);
                    if (layer.getResistance() > 0.0)
                        parasiticNet.modifyResistance(layer, poly, new String[] {net1Name, net2Name}, true);
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
            if (ni.isCellInstance()) return true;  // hierarchical

            // Its like pins, facet-center
			if (NodeInst.isSpecialNode(ni)) return (false);

            // initialize to examine the polygons on this node
            Technology tech = ni.getProto().getTechnology();
            AffineTransform trans = ni.rotateOut();
            ExtractedPBucket parasitic = tool.createBucket(ni, (ParasiticCellInfo)info);
            if (parasitic != null) transAndRCList.add(parasitic);

            int numRemoveParents = context.getNumLevels();
            PrimitiveNode.Function function = ni.getFunction();
            // In case of contacts, the area is substracted.
            boolean add = function != PrimitiveNode.Function.CONTACT;

            Poly [] polyList = tech.getShapeOfNode(ni, null, null, true, true, null);
            int tot = polyList.length;
            for(int i=0; i<tot; i++)
            {
                Poly poly = polyList[i];

                // make sure this layer connects electrically to the desired port
                PortProto pp = poly.getPort();
                if (pp == null) continue;
                Network net = info.getNetlist().getNetwork(ni, pp, 0); // netList.getNetwork(ni, pp, 0);
                // don't bother with layers without capacity
                Layer layer = poly.getLayer();
                if (layer.getTechnology() != Technology.getCurrent()) continue;
                if (!layer.isDiffusionLayer() && net == null) continue;  // only this case skipping for now, schematic case
//                if (!layer.isDiffusionLayer() &&
//                        layer.getCapacitance() == 0.0 && layer.getResistance() == 0.0) continue;

                NetPBucket parasiticNet = getNetParasiticsBucket(net, info);
                if (parasiticNet == null)
                    continue;

                boolean isDiffLayer = layer.isDiffusionLayer();
                if (function.isTransistor() && isDiffLayer) parasiticNet.addTransistor(parasitic);

                // get the area of this polygon
                poly.transform(trans);

                // leave out the gate capacitance of transistors
                boolean gateLayer = (layer.getFunction() == Layer.Function.GATE);
                if (isDiffLayer || !gateLayer && layer.getCapacitance() > 0.0) // diffusion might have zero capacitances )
                    parasiticNet.addCapacitance(layer, poly);

                // Doesn't include gates if user set that
                if (gateLayer && !tech.isGateIncluded()) continue;
                String netName = info.getUniqueNetNameProxy(net, "/").toString(numRemoveParents) + "_" + ni.getName();
                if (layer.getResistance() > 0.0)
                    parasiticNet.modifyResistance(layer, poly, new String[] {netName, netName}, add);
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

    private static class ParasiticValue
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
        public static List<ParasiticValue> initParasiticValues(Poly p1, Poly p2)
        {
            Rectangle2D thisBounds = p1.getBox();
            Rectangle2D otherBounds = p2.getBox();
            List<ParasiticValue> values = new ArrayList<ParasiticValue>();

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
    private static class AnalyzeParasitic extends Job
    {
        Cell cell;
        Network net;

        protected AnalyzeParasitic(Network network, Cell cell)
        {
            super ("Analyze "+ network, tool, Job.Type.EXAMINE, null, cell, Job.Priority.USER);
            this.net = network;
            this.cell = cell;
            this.startJob();
        }

        private List<ParasiticValue> getClosestPolys(Cell cell, Geometric geom, Technology tech, Poly poly, Rectangle2D bounds)
        {
            List<ParasiticValue> polyList = new ArrayList<ParasiticValue>();

            for(Iterator<Geometric> it = cell.searchIterator(bounds); it.hasNext(); )
            {
                Geometric nGeom = it.next();
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
                    if (ni.isCellInstance())
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
                        if (nPoly != null)  // @TODO check this null condition
                            polyList.addAll(ParasiticValue.initParasiticValues(poly, nPoly));
                    }
                }
            }
            return polyList;
        }

        /**
         * Implementation of Job.doIt() abstract function
         */
        public boolean doIt() throws JobException
        {
            long startTime = System.currentTimeMillis();
            System.out.println("Extracting Parasitic for " + cell + " " + net);
            Rectangle2D bounds = new Rectangle2D.Double();
            double maxDistance = getMaxDistance();
            List<ParasiticValue> polyToCheckList = new ArrayList<ParasiticValue>();

            // Selecting arcs attached to this network
            for (Iterator<ArcInst> it = net.getArcs(); it.hasNext(); )
            {
                ArcInst ai = it.next();
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
	        HashMap<NodeInst,NodeInst> nodeMap = new HashMap<NodeInst,NodeInst>();
            for (Iterator<PortInst> it = net.getPorts(); it.hasNext(); )
            {
                PortInst pi = it.next();
                NodeInst ni = pi.getNodeInst();
	            if (nodeMap.get(ni) != null) continue; // Already analyzed
                AffineTransform trans = ni.rotateOut();
                NodeProto np = ni.getProto();
	            nodeMap.put(ni, ni);
				if (!ni.isCellInstance())
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
            for (ParasiticValue val : polyToCheckList)
            {
                System.out.println("Value " + val + " Layer " + val.elements[1].poly.getLayer());
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Done (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
			// sort the errors by layer
			errorLogger.sortLogs();
            return true;
        }
    }

    //----------------------------IRSIM Cell Info for HierarchyEnumerator--------------------

    /**
     * Class to define cell information for Parasitic extraction
     */
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
