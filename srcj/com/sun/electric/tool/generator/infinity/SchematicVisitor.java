package com.sun.electric.tool.generator.infinity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.Cell.CellGroup;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.CellInfo;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.Visitor;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.tool.generator.layout.LayoutLib;

public class SchematicVisitor extends Visitor {
	private Library stageLib;
	private Cell layCell;
	private SchematicPosition schPos = new SchematicPosition();
	private List<NodeInst> layInsts = new ArrayList<NodeInst>();
	private Map<NodeInst, SchematicPosition> layInstToSchPos = 
		new HashMap<NodeInst, SchematicPosition>();
	private Map<Integer, ToConnect> netIdToToConn =
		new HashMap<Integer, ToConnect>();

	private Cell findLayout(Cell icon) {
		CellGroup group = icon.getCellGroup();
		for (Iterator<Cell> cIt=group.getCells(); cIt.hasNext();) {
			Cell c = cIt.next();
			if (c.getView()==View.LAYOUT) return c;
		}
		return null;
	}
	
	private void getLayConnectionsFromIcon(Cell icon, Nodable iconInst,
			                               NodeInst layInst, CellInfo info) {
		Netlist nl = info.getNetlist();
		for (Iterator eIt=icon.getExports(); eIt.hasNext();) {
			Export e = (Export) eIt.next();
			Name eNmKey = e.getNameKey();
			int busWid = eNmKey.busWidth();
			for (int i=0; i<busWid; i++) {
				String subNm = eNmKey.subname(i).toString();
				PortInst layPortInst = layInst.findPortInst(subNm);
				LayoutLib.error(layPortInst==null,
						        "layout instance port missing");
				Network net = nl.getNetwork(iconInst, e, i);
				int netID = info.getNetID(net);
				ToConnect conn = netIdToToConn.get(netID);
				if (conn==null) {
					conn = new ToConnect(null);
					netIdToToConn.put(netID, conn);
				}
				conn.addPortInst(layPortInst);
			}
		}
	}
	

	public boolean visitNodeInst(Nodable no, CellInfo info) {
		// ignore primitives
		if (!no.isCellInstance()) return false;
		
		Cell schematic = no.getParent();

		Cell icon = (Cell) no.getProto();
		
		// for schematic X discard icons X
		Cell iconSch = icon.getEquivalent();
		if (iconSch==schematic) return false;

		// expand Cells not contained by stageLib
		if (icon.getLibrary()!=stageLib) return true;
		
		// Icon belongs to stageLib, get layout
		Cell lay = findLayout(icon);
		
		LayoutLib.error(lay==null, 
		        "Can't find layout for icon: "+icon.getName());

		NodeInst layInst = LayoutLib.newNodeInst(lay, 0, 0, 0, 0, 0, layCell);
		layInst.setName(info.getUniqueNodableName(no, "__"));
		layInsts.add(layInst);
		SchematicPosition copy = schPos.copy();
		copy.push(no.getNodeInst().getBounds().getMinX());
		layInstToSchPos.put(layInst, copy);

		getLayConnectionsFromIcon(icon, no, layInst, info);
		
		return false;
	}
	public boolean enterCell(CellInfo info) {
		if (info.isRootCell()) {
			// create ToConnect for every exported Network
			Netlist nl = info.getNetlist();
			for (Iterator<Network> netIt=nl.getNetworks(); netIt.hasNext();) {
				Network net = netIt.next();
				Iterator<String> expIt = net.getExportedNames();
				if (expIt.hasNext()) {
					netIdToToConn.put(info.getNetID(net), 
                                      new ToConnect(expIt));
				}
			}
		} else {
			Nodable parentInst = info.getParentInst();
			schPos.push(parentInst.getNodeInst().getBounds().getMinX());
		}
		return true;
	}
	public void exitCell(CellInfo info) {
		if (!info.isRootCell())  schPos.pop();
	}
	
	public SchematicVisitor(Cell layCell, Library stageLib) {
		this.stageLib = stageLib;
		this.layCell = layCell;
	}
	public List<ToConnect> getLayoutToConnects() {
		List<ToConnect> toConnects = new ArrayList<ToConnect>();
		for (Integer i : netIdToToConn.keySet())  toConnects.add(netIdToToConn.get(i));
		return toConnects;
	}
	public List<NodeInst> getLayInsts() {
		return layInsts;
	}
	public Map<NodeInst, SchematicPosition> getLayInstSchematicPositions() {
		return layInstToSchPos;
	}
	

}
