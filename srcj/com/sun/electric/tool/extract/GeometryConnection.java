package com.sun.electric.tool.extract;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;

import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda
 * Date: Feb 7, 2005
 * Time: 11:12:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class GeometryConnection
{

    // Private construction for now
    private GeometryConnection() {;}

    public static boolean checkCellConnectivity(NodeInst cellA, NodeInst cellB)
    {
        Rectangle2D cellBounds = (Rectangle2D)cellA.getBounds().clone();
        AffineTransform rTransI = cellB.rotateIn(cellB.translateIn());
        AffineTransform cellUp = cellB.transformOut();
        DBMath.transformRect(cellBounds, rTransI);
        Netlist topNetlist = NetworkTool.acquireUserNetlist(cellA.getParent());

        if (cellB.getProto() instanceof Cell && cellA.getProto() instanceof Cell)
        {
            Cell cellBProto = (Cell)cellB.getProto();
            AffineTransform subTrans = cellA.rotateIn(cellA.translateIn());

            // Search in other cell possible neighbors
            for(Iterator it = cellBProto.searchIterator(cellBounds); it.hasNext(); )
            {
                Geometric nGeom = (Geometric)it.next();

                if (nGeom instanceof NodeInst)
                {
                Rectangle2D rect = (Rectangle2D)nGeom.getBounds().clone();
                DBMath.transformRect(rect, cellUp);
                DBMath.transformRect(rect, subTrans);
                ConnectionEnumerator check = new ConnectionEnumerator(cellA, nGeom, rect,
                        cellBProto.getNetlist(false), topNetlist);
                HierarchyEnumerator.enumerateCell(cellA.getParent(), VarContext.globalContext,
                        NetworkTool.acquireUserNetlist(cellA.getParent()),
                        check);
                if (check.found) return true;
                }
            }
        }
       return false;
    }

    public static boolean searchInExportNetwork(Netlist netlist1, Network net1,
                                                Netlist netlist2, Network net2)
    {
        for (Iterator it = net1.getExports(); it.hasNext();)
        {
            Export exp1 = (Export)it.next();
            Network tmpNet1 = netlist1.getNetwork(exp1.getOriginalPort());
            for (Iterator otherIt = net2.getExports(); otherIt.hasNext();)
            {
                Export exp2 = (Export)otherIt.next();
                Network tmpNet2 = netlist2.getNetwork(exp2.getOriginalPort());
                if (tmpNet2 == tmpNet1) return true;
            }
        }
        return false;
    }

    /**************************************************************************************************************
	 *  QuickAreaEnumerator class
	 **************************************************************************************************************/
	// Extra functions to check area
	private static class ConnectionEnumerator extends HierarchyEnumerator.Visitor
    {
        private Geometric geomA;
        private Geometric geomB;
        private Rectangle2D geomBBnd;
        public boolean found; // To determine if connection without port was found
        private Set netsB;
        private Netlist topNetlist;
        private Cell topCell;

        public ConnectionEnumerator(Geometric geomA, Geometric geomB, Rectangle2D cellABnds,
                                    Netlist netlistB, Netlist topNetlist)
        {
            this.geomA = geomA;
            this.geomB = geomB;
            this.geomBBnd = cellABnds;
            this.found = false;
            this.netsB = getNetworks(geomB, netlistB, null);
            this.topNetlist = topNetlist;
            this.topCell = geomA.getParent();
        }

        private Set getNetworks(Geometric geom, Netlist netlist, Set nets)
        {
            if (nets == null) nets = new HashSet();
            else nets.clear();

            if (geom instanceof ArcInst)
                nets.add(netlist.getNetwork((ArcInst)geom, 0));
            else
            {
                NodeInst ni = (NodeInst)geom;
                for (Iterator pIt = ni.getPortInsts(); pIt.hasNext(); )
                {
                    PortInst pi = (PortInst)pIt.next();
                    nets = NetworkTool.getNetworksOnPort(pi, netlist, nets);
                    //nets.add(netlist.getNetwork(ni, pi.getPortProto(), 0));
                    //nets.add(netlist.getNetwork(pi));
                }
            }
            return nets;
        }

        /**
		 */
		public boolean enterCell(HierarchyEnumerator.CellInfo info)
        {
            Cell cell = info.getCell();

            if (cell == geomA.getParent()) return true; // skip first one.

            Set nets = null;

            for(Iterator it = cell.searchIterator(geomBBnd); it.hasNext(); )
            {
                Geometric nGeom = (Geometric)it.next();
                System.out.println("Node " + nGeom.getName());

                // Only valid for arc-nodeinst pair
                if (Geometric.objectsTouch(nGeom, geomB))
                {
                    found = true;
                    return false;
                }
                // Checking if they already belong to same net
                nets = getNetworks(nGeom, info.getNetlist(), nets);
                if (nets.containsAll(netsB))
                         System.out.println("Found net");
                else if (nets.size() == 1 && netsB.size() == 1)
                {
                    for (Iterator pIt = topCell.getPorts(); pIt.hasNext();)
                    {
                        PortProto port = (PortProto)pIt.next();
                        System.out.println("Port ");
                    }
                    if (searchInExportNetwork(topNetlist,(Network)netsB.toArray()[0],
                              topNetlist, (Network)nets.toArray()[0]))
//                    if (NetworkTool.searchNetworkInParent((Network)netsB.toArray()[0], info,
//                            (Network)nets.toArray()[0]))
                         System.out.println("Found net paretn");
                }
//                for (int i = 0; i < nets.size(); i++)
//                {
//                    // binarySearch doesn't work with only 1 element?
//                    //if (Collections.binarySearch(netsB, nets.get(i)) > -1)
//                    for (int j = 0; j < netsB.size(); j++)
//                    {
//                        nets.
//                        if (nets.get(i) == netsB.get(j))
//                         System.out.println("Found net");
//                    }
//                }
            }

            return false;
        }

        /**
		 */
		public void exitCell(HierarchyEnumerator.CellInfo info) {}

        /**
		 */
		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
        {
            return true;
        }
    }
}
