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
    private static final double ABUT_ROUTE_PORT_TO_BOUND_SPACING = 5;
    private static final double DEF_SIZE = LayoutLib.DEF_SIZE;
    private static final double SIGNAL_WID = 2.8;
    private static final double M1_WID = 2.4;
    private static final double TRACK_PITCH = 6;
	
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
				} else {
					if (pc!=PortCharacteristic.IN && pc!=PortCharacteristic.OUT) {
						prln(" Export "+e+" has undesired characteristic: "+pc);
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
	
	/** From the schematic, get a list of connections that need to be made
	 * in the layout */
	private List<ToConnect> getLayToConnFromSch(Cell autoSch,
			                                    Cell autoLay) {
		List<ToConnect> toConnects = new ArrayList<ToConnect>();
		List<Nodable> stageInsts = getStageInstances(autoSch);
		Map<Network,ToConnect> intToConn = new HashMap<Network,ToConnect>();
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
					ToConnect conn = intToConn.get(schNet);
					if (conn==null) {
						conn = new ToConnect();
						intToConn.put(schNet, conn);
					}
					conn.addPortInst(layPortInst);
				}
			}
		}
		
		for (Network n : intToConn.keySet()) {
			ToConnect cl = intToConn.get(n);
			if (cl.size()>1)  toConnects.add(cl);
		}
		
		// debug
		//for (ToConnect cl : toConnects)  prln("  "+cl.toString());

		return toConnects;
	}
	
	private boolean nextToBoundary(double coord, double boundCoord) {
		return Math.abs(coord-boundCoord) <= ABUT_ROUTE_PORT_TO_BOUND_SPACING;
	}
	
	private boolean isPwrGnd(PortInst pi) {
		PortCharacteristic pc = pi.getPortProto().getCharacteristic();
		return pc==PortCharacteristic.PWR || pc==PortCharacteristic.GND;
	}
	
	private void findChannels(LayerChannels m2Chnls, LayerChannels m3Chnls,
			                  Cell autoLay) {
		double colMinX, colMaxX, colMinY, colMaxY;
		colMinX = colMinY = Double.MAX_VALUE;
		colMaxX = colMaxY = Double.MIN_VALUE; 
		
		Blockage1D m2block = new Blockage1D();
		Blockage1D m3block = new Blockage1D();
		for (Iterator niIt=autoLay.getNodes(); niIt.hasNext();) {
			NodeInst ni = (NodeInst) niIt.next();
			if (ni.getProto() instanceof Cell) {
				Rectangle2D bounds = ni.findEssentialBounds();
				double instMinX = bounds.getMinX();
				double instMaxX = bounds.getMaxX();
				double instMinY = bounds.getMinY();
				double instMaxY = bounds.getMaxY();
				colMinX = Math.min(colMinX, instMinX);
				colMaxX = Math.max(colMaxX, instMaxX);
				colMinY = Math.min(colMinY, instMinY);
				colMaxY = Math.max(colMaxY, instMaxY);
				for (Iterator piIt=ni.getPortInsts(); piIt.hasNext();) {
					PortInst pi = (PortInst) piIt.next();
					double x = pi.getCenter().getX();
					double y = pi.getCenter().getY();
					// this doesn't work because conventions weren't followed
					//double w = LayoutLib.widestWireWidth(pi);
					double w = isPwrGnd(pi) ? 9 : 2.8;
					if (nextToBoundary(x, instMinX) || 
					    nextToBoundary(x, instMaxX) || 
					    nextToBoundary(y, instMinY) || 
					    nextToBoundary(y, instMaxY)) {
						if (pi.getPortProto().connectsTo(tech.m2())) {
							// horizontal m2 channel
							m2block.block(y-w/2, y+w/2);
						} else if (pi.getPortProto().connectsTo(tech.m3())) {
							// vertical m3 channel
							m3block.block(x-w/2, x+w/2);
						} else {
							LayoutLib.error(true, "unexpected metal for port: "+pi.toString());
						}
					}
				}
			}
		}
		Interval prv = null;
		for (Interval i : m2block.getBlockages()) {
			if (prv!=null) {
				m2Chnls.add(new Channel(true, colMinX, colMaxX,
					                    prv.getMax(), i.getMin()));
			}
			prv = i;
		}

		prv = null;
		for (Interval i : m3block.getBlockages()) {
			if (prv!=null) {
				m3Chnls.add(new Channel(false, colMinY, colMaxY,
					                    prv.getMax(), i.getMin()));
			}
			prv = i;
		}
	}
	
	private void sortLeftToRight(List<PortInst> pis) {
		Collections.sort(pis, new Comparator<PortInst>() {
			public int compare(PortInst p1, PortInst p2) {
				double diff = p1.getCenter().getX() -
				              p2.getCenter().getX();
				return (int) Math.signum(diff);
			}
		});
	}
	
	private void route2PinNet(ToConnect toConn, LayerChannels m2Chan,
	                          LayerChannels m3Chan) {
		final double PIN_HEIGHT = 0; 
		List<PortInst> pis = toConn.getPortInsts();
		sortLeftToRight(pis);
		
		PortInst p1 = pis.get(0);
		double x1 = p1.getCenter().getX();
		double y1 = p1.getCenter().getY();
		double maxY1 = y1 + PIN_HEIGHT;
		double minY1 = y1 - PIN_HEIGHT;
		Channel c1 = m2Chan.findChanOverVertInterval(x1, minY1, maxY1);

		PortInst p2 = pis.get(1);
		double x2 = p2.getCenter().getX();
		double y2 = p2.getCenter().getY();
		double maxY2 = y2 + PIN_HEIGHT;
		double minY2 = y2 - PIN_HEIGHT;
		Channel c2 = m2Chan.findChanOverVertInterval(x2, minY2, maxY2);
		
		if (c1==c2) {
			// ports share same m2 channel. Route without m3 if possible.
			prln("shared m2 channel not yet implemented");
		}
		
		// need m3 channel to connect two m2 channels
		Channel c3 = m3Chan.findVertBridge(c1, c2, x1, x2);
		
		// debug
		prln("To connect ports: "+p1+" "+p2);
//		prln("    "+c1);
//		prln("    "+c2);
//		prln("    "+c3);
		
		if (c1==null) prln("no m2 channel for left PortInst");
		if (c2==null) prln("no m2 channel for right PortInst");
		if (c3==null) prln("no m3 channel");
		if (c1==null || c2==null || c3==null) return;

		Segment s1 = c1.allocate(x1-TRACK_PITCH, c3.getMaxX(), y1, y2);
		Segment s2 = c2.allocate(c3.getMinX(), x2+TRACK_PITCH, y1, y2);
		// use more than necessary because using adjacent tracks causes via 
		// spacing violations
		double minY = Math.min(s1.getTrackCenter(), s2.getTrackCenter()) - TRACK_PITCH;
		double maxY = Math.max(s1.getTrackCenter(), s2.getTrackCenter()) + TRACK_PITCH;
		Segment s3 = c3.allocate(minY, maxY, x1, x2);
		
		// do the actual routing right now. In a two level
		// scheme we would actually postpone this
		routeUseM3(p1, p2, s1, s2, s3);
	}
	
	private void routeUseM3(PortInst pL, PortInst pR, Segment m2L, Segment m2R, Segment m3) {
		if (m2L==null)  prln("no m2 track for left PortInst");
		if (m2R==null)  prln("no m2 track for right PortInst");
		if (m3==null)   prln("no m3 track");
		if (m2L==null || m2R==null || m3==null) return;
		
		prln("  Route m3: "+m3.toString());
		
		Cell parent = pL.getNodeInst().getParent();
		NodeInst m1m2a = 
			LayoutLib.newNodeInst(tech.m1m2(), 
				              	  pL.getCenter().getX(),
				                  m2L.getTrackCenter(),
				                  DEF_SIZE, DEF_SIZE, 0, parent);
		// left vertical m1
		LayoutLib.newArcInst(tech.m1(), M1_WID, 
				             pL, m1m2a.getOnlyPortInst());
		
		NodeInst m2m3a =
			LayoutLib.newNodeInst(tech.m2m3(),
					              m3.getTrackCenter(),
					              m2L.getTrackCenter(),
					              DEF_SIZE, DEF_SIZE, 0, parent);
		// left horizontal m2
		LayoutLib.newArcInst(tech.m2(), SIGNAL_WID, 
				             m1m2a.getOnlyPortInst(),
				             m2m3a.getOnlyPortInst());
		NodeInst m2m3b =
			LayoutLib.newNodeInst(tech.m2m3(),
					              m3.getTrackCenter(),
					              m2R.getTrackCenter(),
					              DEF_SIZE, DEF_SIZE, 0, parent);
		// vertical m3
		LayoutLib.newArcInst(tech.m3(), SIGNAL_WID, 
				             m2m3a.getOnlyPortInst(),
				             m2m3b.getOnlyPortInst());
		
		NodeInst m1m2b = 
			LayoutLib.newNodeInst(tech.m1m2(), 
				              	  pR.getCenter().getX(),
				                  m2R.getTrackCenter(),
				                  DEF_SIZE, DEF_SIZE, 0, parent);
		
		LayoutLib.newArcInst(tech.m2(), SIGNAL_WID, 
				             m2m3b.getOnlyPortInst(),
				             m1m2b.getOnlyPortInst());

		LayoutLib.newArcInst(tech.m1(), M1_WID, 
	             			 m1m2b.getOnlyPortInst(),
	             			 pR);
	}
	
	private void route(List<ToConnect> toConns, LayerChannels m2Chan,
			           LayerChannels m3Chan) {
		for (ToConnect toConn : toConns) {
			if (toConn.size()==2) {
				route2PinNet(toConn, m2Chan, m3Chan);
			} else if (toConn.size()==3) {
				prln("Skip route of 3 pin net: "+toConn);
			} else {
				prln("Skip route of 4+ pin net:"+toConn);
			}
		}
	}
	
	private void dumpChannels(LayerChannels m2chan, LayerChannels m3chan) {
        prln("m2 channels");
        prln(m2chan.toString());
        
        prln("m3 channels");
        prln(m3chan.toString());
	}
	
	public Infinity() {
		prln("Generating layout for Infinity");
		Stages stages = findStageCells();
		if (stages.someStageIsMissing()) return;
		ensurePwrGndExportsOnBoundingBox(stages.getStages());

        Library autoLib = LayoutLib.openLibForWrite(AUTO_GEN_LIB_NAME);
        Cell autoLay = Cell.newInstance(autoLib, AUTO_GEN_CELL_NAME);
        Cell autoSch = autoLib.findNodeProto("autoInfCell{sch}");
        if (autoSch==null) {
        	prln("Can't find autoInfCell{sch}");
        	return;
        }
        
        List<NodeInst> stageInsts = addInstances(autoLay, autoSch);
        connectPwrGnd(stageInsts);
        
        List<ToConnect> toConn = getLayToConnFromSch(autoSch, autoLay);
        
        LayerChannels m2chan = new LayerChannels();
        LayerChannels m3chan = new LayerChannels();
        
        findChannels(m2chan, m3chan, autoLay);
        
        route(toConn, m2chan, m3chan);
        
        // Debug
        dumpChannels(m2chan, m3chan);

        System.out.println("done.");
	}
}
