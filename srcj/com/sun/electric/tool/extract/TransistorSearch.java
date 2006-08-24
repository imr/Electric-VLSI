package com.sun.electric.tool.extract;

import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.PrimitiveNode;

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
                // checking the ports
                transistorNumber++;
            }
            return true;
        }
    }
}
