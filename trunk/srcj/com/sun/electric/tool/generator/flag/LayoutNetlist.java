package com.sun.electric.tool.generator.flag;

import java.util.List;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.generator.flag.router.ToConnect;

/** Contains instance and connectivity information for the layout Cell */
public class LayoutNetlist {
	private final Cell layCell;
    private final List<NodeInst> layInsts;
    private final List<ToConnect> toConns;
    LayoutNetlist(Cell layCell, List<NodeInst> layInsts, List<ToConnect> toConns) {
    	this.layCell = layCell;
    	this.layInsts = layInsts;
    	this.toConns = toConns;
    }
    
    // ---------------------------- public methods ----------------------------
    public Cell getLayoutCell() {return layCell;}
    public List<NodeInst> getLayoutInstancesSortedBySchematicPosition() {
    	return layInsts;
    }
    public List<ToConnect> getToConnects() {return toConns;}
}
