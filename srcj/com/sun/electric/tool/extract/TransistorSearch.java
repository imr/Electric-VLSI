package com.sun.electric.tool.extract;

import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.prototype.PortCharacteristic;

import java.util.Iterator;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda
 * Date: Aug 24, 2006
 * Time: 3:25:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class TransistorSearch
{
    public static void countNumberOfTransistors(Cell cell)
    {
        if (cell.getView() != View.SCHEMATIC)
        {
            System.out.println("Counting number of transistors only valid for Schematics cells");
            return;
        }
        TransistorSearchEnumerator visitor = new TransistorSearchEnumerator();
        HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, visitor);
        System.out.println("Number of transistors found from cell " + cell.getName() + ": " + visitor.transistorNumber);
    }

    /**************************************************************************************************************
     *  ConnectionEnumerator class
     **************************************************************************************************************/
    private static class TransistorSearchEnumerator extends HierarchyEnumerator.Visitor
    {
        private int transistorNumber;

        public TransistorSearchEnumerator() {}

        public boolean enterCell(HierarchyEnumerator.CellInfo info)
        {
            return true;
        }
        public void exitCell(HierarchyEnumerator.CellInfo info) {}
        public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
        {
            NodeInst ni = no.getNodeInst();
            if (ni.isCellInstance()) return true;
            if (ni.getProto().getFunction().isTransistor())
            {
                Netlist netlist = info.getNetlist();
                Global.Set globals = netlist.getGlobals();
                // checking the ports
                boolean found = false; // no cap transistor (2 gnd or 2 vdd)
                HashMap<Integer,Integer> map = new HashMap<Integer,Integer>();
                int netID = -1;
                for (Iterator<PortInst> itPi = ni.getPortInsts(); itPi.hasNext();)
                {
                    PortInst pi = itPi.next();
                    // Only checking active ports: source and drain
                    if (pi.getPortProto().getCharacteristic() != PortCharacteristic.BIDIR)
                        continue;
                    Network net = netlist.getNetwork(pi);
                    int key = info.getNetID(net);
                    if (netID == -1)
                        netID = key;
                    else if (key == netID)  // same network
                    {
                        found = true;
                        for (int j = 0; j < globals.size(); j++)
                        {
                            Global g = globals.get(j);
                            if (netlist.getNetwork(g) == net)
                                System.out.println();
                        }
                        break;
                    }
                }
                // Only counting when it is not a cap transistor.
                if (!found)
                    transistorNumber++;
            }
            return true;
        }
    }
}
