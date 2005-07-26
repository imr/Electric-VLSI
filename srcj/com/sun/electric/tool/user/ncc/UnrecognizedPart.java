package com.sun.electric.tool.user.ncc;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;

public class UnrecognizedPart {
    private Cell cell;
    private VarContext context;
    private String name;
    private NodeInst nodeInst;
    
    public UnrecognizedPart(Cell cel, VarContext con, String nm, NodeInst inst) {
        cell = cel;
        context = con;
        name = nm;
        nodeInst = inst;
    }
    
    public Cell       getCell()     { return cell; }
    public VarContext getContext()  { return context; }
    public String     getName()     { return name; }
    public NodeInst   getNodeInst() { return nodeInst; }
}
