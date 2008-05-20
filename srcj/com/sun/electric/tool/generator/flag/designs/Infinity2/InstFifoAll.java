package com.sun.electric.tool.generator.flag.designs.Infinity2;

import java.util.ArrayList;
import java.util.List;

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.tool.generator.flag.FlagConstructorData;
import com.sun.electric.tool.generator.flag.FlagDesign;
import com.sun.electric.tool.generator.flag.router.ToConnect;
import com.sun.electric.tool.generator.layout.AbutRouter;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.LayoutLib.Corner;

public class InstFifoAll extends FlagDesign {
	private final double DEF_SIZE = LayoutLib.DEF_SIZE;
	private final double LEFT_COL_X = -1224;
	private final double RIGHT_COL_X = 1224;
	private final double START_COL_Y = -504;
	private final double Y_PITCH = 144;
	
	private List<NodeInst> lCol = new ArrayList<NodeInst>(), 
	                       rCol = new ArrayList<NodeInst>();
	private NodeInst ctl;
	
	private Library findLibrary(String nm) {
		Library l = Library.findLibrary(nm);
		if (l==null) prln("Can't find Library: "+nm);
		return l;
	}
	
	private Cell findCell(Library lib, String cellNm) {
		Cell c = lib.findNodeProto(cellNm);
		if (c==null) prln("Can't find cell: "+cellNm+" in library: "+lib.getName());
		return c;
	}

	private void stackInsts(List<NodeInst> layInsts) {
        NodeInst prev = null;
        for (NodeInst me : layInsts) {
        	if (prev!=null) {
        		LayoutLib.alignCorners(prev, Corner.TL, me, Corner.BL, 0, 0);
        	}
        	prev = me;
        }
	}
	
	private boolean createLayInsts(Cell layCell) {
		Library registersK = findLibrary("registersK");
		if (registersK==null) return false;
		Cell instReg14 = findCell(registersK, "instReg14{lay}");
		Cell instReg12 = findCell(registersK, "instReg12{lay}");
		Cell instReg14x2 = findCell(registersK, "instReg14x2{lay}");
		Cell instReg12x2 = findCell(registersK, "instReg12x2{lay}");
		Cell instMerge14 = findCell(registersK, "instMerge14{lay}");
		Cell instMerge12 = findCell(registersK, "instMerge12{lay}");
		if (instReg14==null || instReg12==null || instReg14x2==null || 
			instReg12x2==null || instMerge14==null || instMerge12==null)
			return false;
		
		Library fifosK = findLibrary("fifosK");
		if (fifosK==null) return false;
		Cell instFifoCont = fifosK.findNodeProto("instFifoCont{lay}");
		if (instFifoCont==null) return false;
		
		// registers are ordered in the arrays from bottom to top 
		lCol.add(LayoutLib.newNodeInst(instReg14, 0, 0, DEF_SIZE, DEF_SIZE, 0, layCell));
		rCol.add(LayoutLib.newNodeInst(instReg12, 0, 0, DEF_SIZE, DEF_SIZE, 0, layCell));
		for (int i=0; i<6; i++) {
			lCol.add(LayoutLib.newNodeInst(instReg14x2, 0, 0, DEF_SIZE, DEF_SIZE, 0, layCell));
			rCol.add(LayoutLib.newNodeInst(instReg12x2, 0, 0, DEF_SIZE, DEF_SIZE, 0, layCell));
		}
		NodeInst m = LayoutLib.newNodeInst(instMerge14, 0, 0, DEF_SIZE, DEF_SIZE, 0, layCell);
		m.modifyInstance(0, 0, 0, 0, Orientation.Y);	// mirror top <-> bottom
		lCol.add(m);
		m=LayoutLib.newNodeInst(instMerge12, 0, 0, DEF_SIZE, DEF_SIZE, 0, layCell);
		m.modifyInstance(0, 0, 0, 0, Orientation.Y);	// mirror top <-> bottom
		rCol.add(m);
		lCol.add(LayoutLib.newNodeInst(instReg14, 0, 0, DEF_SIZE, DEF_SIZE, 0, layCell));
		rCol.add(LayoutLib.newNodeInst(instReg12, 0, 0, DEF_SIZE, DEF_SIZE, 0, layCell));
		
		ctl = LayoutLib.newNodeInst(instFifoCont, 0, 0, DEF_SIZE, DEF_SIZE, 0, layCell);
		
		return true;
	}
	// position everything relative to the control block
	private void placeLayInsts() {
		LayoutLib.alignCorners(ctl, Corner.BL, lCol.get(0), Corner.BR, 0, 0);
		LayoutLib.alignCorners(ctl, Corner.BR, rCol.get(0), Corner.BL, 0, 0);
		stackInsts(lCol);
		stackInsts(rCol);
	}
	private void abutRoute() {
		List<ArcProto> layers = new ArrayList<ArcProto>();
		layers.add(tech().m2());
		for (NodeInst ni : lCol) {
			AbutRouter.abutRouteLeftRight(ni, ctl, 10, layers);
		}
		for (NodeInst ni: rCol) {
			AbutRouter.abutRouteLeftRight(ctl, ni, 10, layers);
		}
	}
	private void connectBus(List<ToConnect> toConns, int row1, String bus1, int row2, String bus2) {
		for (int i=1; i<=14; i++) {
			ToConnect net = new ToConnect();
			net.addPortInst(lCol.get(row1).findPortInst(bus1+"["+i+"]"));
			net.addPortInst(lCol.get(row2).findPortInst(bus2+"["+i+"]"));
			toConns.add(net);
		}
		for (int i=1; i<=12; i++) {
			ToConnect net = new ToConnect();
			net.addPortInst(rCol.get(row1).findPortInst(bus1+"["+i+"]"));
			net.addPortInst(rCol.get(row2).findPortInst(bus2+"["+i+"]"));
			toConns.add(net);
		}
	}
	private List<ToConnect> createNetlist() {
		List<ToConnect> toConns = new ArrayList<ToConnect>();
		connectBus(toConns, 0, "out", 3, "inX");
		
		
		return toConns;
	}
	
	public InstFifoAll(FlagConstructorData data) {
		super(Infinity2Config.CONFIG, data);
		boolean ok;
		ok = createLayInsts(data.getLayoutCell());
		if (!ok) return;
		placeLayInsts();
        addEssentialBounds(data.getLayoutCell());
		abutRoute();
		List<ToConnect> toConns = createNetlist();
		routeSignalsSog(toConns);
		
        reexportPowerGround(data.getLayoutCell());
        addNccVddGndExportsConnectedByParent(data.getLayoutCell());
	}
}
