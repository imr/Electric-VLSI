package com.sun.electric.tool.generator.flag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
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
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.generator.flag.router.ToConnect;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.Job;

public class SchematicVisitor extends Visitor {
	private static final Variable.Key LAYOUT_SPACE_KEY = Variable.newKey("ATTR_LAYOUT_SPACE");
	private final Cell layCell;
	private SchematicPosition schPos = new SchematicPosition();
	private List<NodeInst> layInsts = new ArrayList<NodeInst>();
	private Map<NodeInst, SchematicPosition> layInstToSchPos = 
		new HashMap<NodeInst, SchematicPosition>();
	private Map<NodeInst, Double> layInstToSpacing =
		new HashMap<NodeInst, Double>();
	private Map<Integer, ToConnect> netIdToToConn =
		new HashMap<Integer, ToConnect>();

	private static final void prln(String msg) {Utils.prln(msg);}
	
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
				if (layPortInst==null) {
					prln("layout Cell: "+layInst.getProto().getName()+
						 " missing port: "+subNm);
					continue;
				}
						 
				Network net = nl.getNetwork(iconInst, e, i);
				int netID = info.getNetID(net);
				ToConnect conn = netIdToToConn.get(netID);
				if (conn==null) {
					conn = new ToConnect();
					netIdToToConn.put(netID, conn);
				}
				conn.addPortInst(layPortInst);
			}
		}
	}
	
	// The name "foo[3]__bar is illegal in Electric because the
	// array reference is inside the name. Change inner array
	// refs to use _ instead: "foo_3___bar".
	private String removeInnerArrayRefs(String instNm) {
		final int LEN = instNm.length();
		char[] chars = new char[LEN];
		boolean findMatchOpen = false;
		for (int i=LEN-1; i>=0; i--) {
			chars[i] = instNm.charAt(i);
			if (i==LEN-1 && chars[i]==']') {
				findMatchOpen = true;
				continue;
			}
			if (findMatchOpen && chars[i]=='[') {
				findMatchOpen = false;
				continue;
			}
			if (chars[i]==']' || chars[i]=='[') {
				chars[i] = '_';
			}
		}
		if (findMatchOpen) {
			// Never found a matching open.  Delete the close
			chars[LEN-1] = '_';
		}
		return new String(chars);
	}
	
	private double getLayoutSpacing(Nodable no) {
		NodeInst iconInst = no.getNodeInst();
		Variable v = iconInst.getVar(LAYOUT_SPACE_KEY);
		if (v==null) return 0;
		Object o = v.getObject();
		if (o instanceof Integer) return ((Integer)o).doubleValue();
		if (o instanceof Float) return ((Float)o).doubleValue();
		if (o instanceof Double) return ((Double)o).doubleValue();
		return 0;
	}
	
	private boolean useCellLayout(Cell iconSch) {
		FlagAnnotations ann = new FlagAnnotations(iconSch);
		return ann.isAtomic() || ann.isAutoGen();
	}

	public boolean visitNodeInst(Nodable no, CellInfo info) {
		// ignore primitives
		if (!no.isCellInstance()) return false;
		
		Cell schematic = no.getParent();

		Cell icon = (Cell) no.getProto();
		
		// for schematic X discard icons X
		Cell iconSch = icon.getEquivalent();
		if (iconSch==schematic) return false;

		// expand Cells that are not layout primitives
		if (!useCellLayout(iconSch)) return true;
		
		// Icon is a layout primitive, get layout
		Cell lay = findLayout(icon);
		
		Job.error(lay==null,
		        "Can't find layout for icon: "+icon.getName());

		NodeInst layInst = LayoutLib.newNodeInst(lay, 0, 0, 0, 0, 0, layCell);
		String instNm = info.getUniqueNodableName(no, "__");
		layInst.setName(removeInnerArrayRefs(instNm));
		layInsts.add(layInst);
		SchematicPosition copy = schPos.copy();
		copy.push(no.getNodeInst().getBounds().getMinX());
		layInstToSchPos.put(layInst, copy);
		layInstToSpacing.put(layInst, getLayoutSpacing(no));
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
					List<String> expNms = new ArrayList<String>();
					while (expIt.hasNext()) expNms.add(expIt.next()); 
					netIdToToConn.put(info.getNetID(net), 
                                      new ToConnect(expNms));
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
	
	public SchematicVisitor(Cell layCell) {
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
	/** when stacking layout instances, user may request extra space between
	 * a particular instance and its predecessor */
	public Map<NodeInst, Double> getLayInstSpacing() {
		return layInstToSpacing;
	}

}
