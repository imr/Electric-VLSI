package com.sun.electric.tool.generator.infinity;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.tool.generator.layout.AbutRouter;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.TechType;

public class Infinity {
	private static final String STAGE_LIB_NAME = new String("stagesF");
	private static final String AUTO_GEN_LIB_NAME = new String("autoInfinity");
	private static final String AUTO_GEN_CELL_NAME = new String("autoInfCell{lay}");
    private static final TechType tech = TechType.CMOS90;
    private static final double STAGE_SPACING = 144;
    private static final double ABUT_ROUTE_PORT_TO_BOUND_SPACING = 0;
	
	private void prln(String s) {System.out.println(s);}
	
	private Stages findStageCells() {
		Library lib = Library.findLibrary(STAGE_LIB_NAME);
		Stages stages = new Stages(lib);
		if (lib==null) {
			prln("Please open the library containing stage cells: "+
				 STAGE_LIB_NAME);
		}
		return stages;
	}
	
	private void ensurePwrGndExportsOnBoundingBox(Collection<Cell> stages) {
		for (Cell c : stages) {
			Rectangle2D bBox = c.findEssentialBounds();
			if (bBox==null) {
				prln("Stage: "+c.getName()+" is missing essential bounds");
				continue;
			}
			double minX = bBox.getMinX();
			double maxX = bBox.getMaxX();
			double minY = bBox.getMinY();
			double maxY = bBox.getMaxY();
			for (Iterator it=c.getExports(); it.hasNext();) {
				Export e = (Export) it.next();
				PortCharacteristic pc = e.getCharacteristic();
				if (pc==PortCharacteristic.PWR || pc==PortCharacteristic.GND) {
					PortInst pi = e.getOriginalPort();
					EPoint center = pi.getCenter();
					double x = center.getX();
					double y = center.getY();
					if (x!=minX && x!=maxX && y!=minY && y!=maxY) {
						prln("Cell: "+c.getName()+", Export: "+e.getName()+
							 " Export not on Cell Bounding Box");
						prln("  Bounding box: ("+minX+", "+minY+") ("+
							 maxX+", "+maxY+")");
						prln("  Export coordinates: ("+x+", "+y+")");
					}
				}
			}
		}
	}
	
	private List<NodeInst> addInstances(Cell parentCell, Stages stages) {
		List<NodeInst> stageInsts = new ArrayList<NodeInst>();
		for (Cell c : stages.getStages()) {
			stageInsts.add(LayoutLib.newNodeInst(c, 0, 0, 0, 0, 0, parentCell));
		}
		LayoutLib.abutBottomTop(stageInsts, STAGE_SPACING);
		return stageInsts;
	}
	
	private void sortSchStageInstsByX(List<Nodable> stageInsts) {
		Collections.sort(stageInsts, new Comparator<Nodable>() {
			public int compare(Nodable n1, Nodable n2) {
				double x1 = n1.getNodeInst().getBounds().getMinX();
				double x2 = n2.getNodeInst().getBounds().getMinX();
				double delta = x1 - x2;
				if (delta>0) return 1;
				if (delta==0) return 0;
				if (delta<0) return -1;
				LayoutLib.error(true, "stage instances x-coord unordered "+
						        "in schematic");
				return 0;
			}
		});
	}
	
	private List<Nodable> getStageInstances(Cell autoSch) {
		Library stageLib = Library.findLibrary(STAGE_LIB_NAME);
		List<Nodable> stageInsts = new ArrayList<Nodable>();

		for (Iterator nIt=autoSch.getNodables(); nIt.hasNext();) {
			Nodable na = (Nodable) nIt.next();
			NodeProto np = na.getNodeInst().getProto();
			if (!(np instanceof Cell)) continue;
			Cell c = (Cell) np;
			if (c.getLibrary()==stageLib) stageInsts.add(na);
		}
		sortSchStageInstsByX(stageInsts);
		return stageInsts;
	}
	
	private List<NodeInst> addInstances(Cell parentCell, Cell autoSch) {
		Library stageLib = Library.findLibrary(STAGE_LIB_NAME);
		List<NodeInst> layInsts = new ArrayList<NodeInst>();
		List<Nodable> schInsts = getStageInstances(autoSch);
		for (Nodable no : schInsts) {
			Cell schCell = (Cell) no.getProto();
			prln("Name of schematic instance is: "+schCell.getName());
			Cell layCell = stageLib.findNodeProto(schCell.getName()+"{lay}");
			NodeInst layInst = LayoutLib.newNodeInst(layCell, 0, 0, 0, 0, 0, parentCell);
			layInst.setName(no.getName());
			layInsts.add(layInst);
		}
		LayoutLib.abutBottomTop(layInsts, STAGE_SPACING);
		return layInsts;
	}
	
	private void connectPwrGnd(List<NodeInst> nodeInsts) {
		NodeInst prev = null;
		for (NodeInst ni : nodeInsts) {
			if (prev!=null) {
				AbutRouter.abutRouteBotTop(prev, ni, 
						                   ABUT_ROUTE_PORT_TO_BOUND_SPACING, 
						                   tech);
			}
			prev = ni;
		}
	}
	
	private List<ConnList> getLayConnFromSch(Cell autoSch,
			                                  Cell autoLay) {
		List<ConnList> toConnects = new ArrayList<ConnList>();
		List<Nodable> stageInsts = getStageInstances(autoSch);
		Map<Network,ConnList> intToConn = new HashMap<Network,ConnList>();
		Netlist schNets = autoSch.getNetlist(true);
		
		for (Nodable schInst : stageInsts) {
			String schInstNm = schInst.getName();
			NodeInst layInst = autoLay.findNode(schInstNm);
			LayoutLib.error(layInst==null, "layout instance missing");
			
			Cell schCell = (Cell) schInst.getProto();
			for (Iterator eIt=schCell.getExports(); eIt.hasNext();) {
				Export e = (Export) eIt.next();
				Name eNmKey = e.getNameKey();
				int busWid = eNmKey.busWidth();
				for (int i=0; i<busWid; i++) {
					String subNm = eNmKey.subname(i).toString();
					PortInst layPortInst = layInst.findPortInst(subNm);
					LayoutLib.error(layPortInst==null,
							        "layout instance port missing");
					Network schNet = schNets.getNetwork(schInst, e, i);
					ConnList conn = intToConn.get(schNet);
					if (conn==null) {
						conn = new ConnList();
						intToConn.put(schNet, conn);
					}
					conn.addPortInst(layPortInst);
				}
			}
		}
		// debug
		prln("Dump nets");
		for (Network n : intToConn.keySet()) {
			ConnList cl = intToConn.get(n);
			prln("  "+cl.toString());
		}
		return toConnects;
	}
	
	public Infinity() {
		prln("Generating layout for Infinity");
		Stages stages = findStageCells();
		if (stages.someStageIsMissing()) return;
		ensurePwrGndExportsOnBoundingBox(stages.getStages());

        Library autoLib = LayoutLib.openLibForWrite(AUTO_GEN_LIB_NAME);
        Cell parentCell = Cell.newInstance(autoLib, AUTO_GEN_CELL_NAME);
        Cell autoSch = autoLib.findNodeProto("autoInfCell{sch}");
        if (autoSch==null) {
        	prln("Can't find autoInfCell{sch}");
        	return;
        }
        
        //List<NodeInst> stageInsts = addInstances(parentCell, stages);
        List<NodeInst> stageInsts = addInstances(parentCell, autoSch);
        connectPwrGnd(stageInsts);
        
        getLayConnFromSch(autoSch, parentCell);
        
        System.out.println("done.");
	}
}
