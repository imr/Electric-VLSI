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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.Cell.CellGroup;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
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
	private boolean isPowerOrGround(PortProto pp) {
		PortCharacteristic pc = pp.getCharacteristic();
		return pc==PortCharacteristic.PWR || pc==PortCharacteristic.GND;
	}
	private boolean isPowerOrGround(PortInst pi) {
		return isPowerOrGround(pi.getPortProto());
	}

	private void ensurePwrGndExportsOnBoundingBox(Collection<Cell> stages) {
		for (Cell c : stages) {
			Rectangle2D bnds = c.findEssentialBounds();
			if (bnds==null) {
				prln("Stage: "+c.getName()+" is missing essential bounds");
				continue;
			}
			for (Iterator it=c.getExports(); it.hasNext();) {
				Export e = (Export) it.next();
				if (isPowerOrGround(e)) {
					PortInst pi = e.getOriginalPort();
					if (!onBounds(pi, bnds, 0)) {
						prln("Cell: "+c.getName()+", Export: "+e.getName()+
							 " Export not on Cell Bounding Box");
						prln("  Bounding box: "+bnds.toString());
						prln("  Port Center: "+pi.getCenter().toString());
						onBounds(pi, bnds, 0);
					}
				} else {
					PortCharacteristic pc = e.getCharacteristic();
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
	
	private Cell findLayout(Cell schCell) {
		CellGroup group = schCell.getCellGroup();
		for (Iterator<Cell> cIt=group.getCells(); cIt.hasNext();) {
			Cell c = cIt.next();
			if (c.getView()==View.LAYOUT) return c;
		}
		return null;
	}
	
	private List<NodeInst> addInstances(Cell parentCell, Cell autoSch) {
		List<NodeInst> layInsts = new ArrayList<NodeInst>();
		List<Nodable> schInsts = getStageInstances(autoSch);
		for (Nodable no : schInsts) {
			Cell schCell = (Cell) no.getProto();
			//prln("Name of schematic instance is: "+schCell.getName());
			Cell layCell = findLayout(schCell);
			LayoutLib.error(layCell==null, 
					        "Can't find layout for cell: "+schCell.getName());
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
		Map<Network,ToConnect> netToConn = new HashMap<Network,ToConnect>();
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
					ToConnect conn = netToConn.get(schNet);
					if (conn==null) {
						conn = new ToConnect();
						netToConn.put(schNet, conn);
					}
					conn.addPortInst(layPortInst);
				}
			}
		}
		
		// Mark ToConnects that are exported
		for (Iterator<Export> eIt=autoSch.getExports(); eIt.hasNext();) {
			Export e = eIt.next();
			Name eNmKey = e.getNameKey();
			int busWid = eNmKey.busWidth();
			for (int i=0; i<busWid; i++) {
				Network n = schNets.getNetwork(e, i);
				ToConnect toConn = netToConn.get(n);
				toConn.setExported();
			}
		}
		
		for (Network n : netToConn.keySet()) {
			ToConnect cl = netToConn.get(n);
			if (cl.size()>1)  toConnects.add(cl);
		}
		
		// debug
		//for (ToConnect cl : toConnects)  prln("  "+cl.toString());

		return toConnects;
	}
	
	private boolean nextToBoundary(double coord, double boundCoord, double fudge) {
		return Math.abs(coord-boundCoord) <= fudge;
	}
	
	private boolean isPwr(PortInst pi) {
		PortCharacteristic pc = pi.getPortProto().getCharacteristic();
		return pc==PortCharacteristic.PWR;
	}
	private boolean isGnd(PortInst pi) {
		PortCharacteristic pc = pi.getPortProto().getCharacteristic();
		return pc==PortCharacteristic.GND;
	}
	private boolean isPwrGnd(PortInst pi) {
		return isPwr(pi) || isGnd(pi);
	}
	
	private Rectangle2D findColBounds(Collection<NodeInst> stages) {
		double minX, minY, maxX, maxY;
		minX = minY = Double.MAX_VALUE;
		maxX = maxY = Double.MIN_VALUE;
		for (NodeInst ni : stages) {
			Rectangle2D bounds = ni.findEssentialBounds();
			minX = Math.min(minX, bounds.getMinX());
			maxX = Math.max(maxX, bounds.getMaxX());
			minY = Math.min(minY, bounds.getMinY());
			maxY = Math.max(maxY, bounds.getMaxY());
		}
		return new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
	}
	
	private boolean onBounds(PortInst pi, Rectangle2D bounds, double fudge) {
		double x = pi.getCenter().getX();
		double y = pi.getCenter().getY();
		return nextToBoundary(x, bounds.getMinX(), fudge) ||
		       nextToBoundary(x, bounds.getMaxX(), fudge) ||
		       nextToBoundary(y, bounds.getMinY(), fudge) ||
		       nextToBoundary(y, bounds.getMaxY(), fudge);
	}
	
	private void findChannels(LayerChannels m2Chnls, LayerChannels m3Chnls,
			                  Collection<NodeInst> stages) {
		Blockage1D m2block = new Blockage1D();
		Blockage1D m3block = new Blockage1D();
		Rectangle2D colBounds = findColBounds(stages);
		for (NodeInst ni : stages) {
			Rectangle2D instBounds = ni.findEssentialBounds();
			for (Iterator piIt=ni.getPortInsts(); piIt.hasNext();) {
				PortInst pi = (PortInst) piIt.next();
				double x = pi.getCenter().getX();
				double y = pi.getCenter().getY();
				// this doesn't work because conventions weren't followed
				//double w = LayoutLib.widestWireWidth(pi);
				double w = isPwrGnd(pi) ? 9 : 2.8;
				if (onBounds(pi, instBounds, ABUT_ROUTE_PORT_TO_BOUND_SPACING)) {
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
		Interval prv = null;
		for (Interval i : m2block.getBlockages()) {
			if (prv!=null) {
				m2Chnls.add(new Channel(true, colBounds.getMinX(),
						                colBounds.getMaxX(),
					                    prv.getMax(), i.getMin()));
			}
			prv = i;
		}

		prv = null;
		for (Interval i : m3block.getBlockages()) {
			if (prv!=null) {
				m3Chnls.add(new Channel(false, colBounds.getMinY(),    
						                colBounds.getMaxY(),
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
		PortInst p2 = pis.get(1);
		prln("To connect ports: "+p1+" "+p2);

		double x1 = p1.getCenter().getX();
		double y1 = p1.getCenter().getY();
		double maxY1 = y1 + PIN_HEIGHT;
		double minY1 = y1 - PIN_HEIGHT;
		Channel c1 = m2Chan.findChanOverVertInterval(x1, minY1, maxY1);

		double x2 = p2.getCenter().getX();
		double y2 = p2.getCenter().getY();
		double maxY2 = y2 + PIN_HEIGHT;
		double minY2 = y2 - PIN_HEIGHT;
		Channel c2 = m2Chan.findChanOverVertInterval(x2, minY2, maxY2);
		

		if (c1==c2) {
			// ports share same m2 channel. Route without m3 if possible.
			prln("shared m2 channel not yet implemented");
		}
		
		if (c1==null) prln("no m2 channel for left PortInst");
		if (c2==null) prln("no m2 channel for right PortInst");
		if (c1==null || c2==null) return;
		
		
		// need m3 channel to connect two m2 channels
		Channel c3 = m3Chan.findVertBridge(c1, c2, x1, x2);
		
		// debug
//		prln("    "+c1);
//		prln("    "+c2);
//		prln("    "+c3);
		
		if (c3==null) {prln("no m3 channel"); return;}

		Segment s1 = null;
		if (p1.getPortProto().connectsTo(tech.m1())) {
			s1 = c1.allocate(x1-TRACK_PITCH, c3.getMaxX(), y1, y2);
		} else if (p1.getPortProto().connectsTo(tech.m2())) {
			s1 = c1.allocate(x1-TRACK_PITCH, c3.getMaxX(), y1, y1);
			if (s1.getTrackCenter()!=y1) {
				prln("failed to get required m2 track: center="+y1+
					 "["+(x1-TRACK_PITCH)+", "+c3.getMaxX()+"]");				
				prln(c2.toString());
			}
//			LayoutLib.error(s1.getTrackCenter()!=y1,
//	                        "failed to get required m2 track");
		} else {
			LayoutLib.error(true, "pin doesn't connect to m1 or m2");
		}
		
		
		Segment s2 = null;
		if (p2.getPortProto().connectsTo(tech.m1())) {
			s2 = c2.allocate(c3.getMinX(), x2+TRACK_PITCH, y1, y2);
		} else if (p2.getPortProto().connectsTo(tech.m2())) {
			s2 = c2.allocate(c3.getMinX(), x2+TRACK_PITCH, y2, y2);
//			LayoutLib.error(s2.getTrackCenter()!=y2,
//            				"failed to get required m2 track");
			if (s2.getTrackCenter()!=y2) {
				prln("failed to get required m2 track: center="+y2+
					 "["+c3.getMinX()+", "+(x2+TRACK_PITCH)+"]");				
				prln(c2.toString());
			}
		} else {
			LayoutLib.error(true, "pin doesn't connect to m1 or m2");
		}
			
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
		
		//prln("  Route m3: "+m3.toString());
		
		PortInst m2PortA = null;
		
		Cell parent = pL.getNodeInst().getParent();
		if (pL.getPortProto().connectsTo(tech.m1())) {
			// left port connects to m1
			NodeInst m1m2a = 
				LayoutLib.newNodeInst(tech.m1m2(), 
					              	  pL.getCenter().getX(),
					                  m2L.getTrackCenter(),
					                  DEF_SIZE, DEF_SIZE, 0, parent);
			// left vertical m1
			LayoutLib.newArcInst(tech.m1(), SIGNAL_WID, 
					             pL, m1m2a.getOnlyPortInst());
			m2PortA = m1m2a.getOnlyPortInst();
		} else {
			// left port connects to m2
			m2PortA = pL;
		}
		
		NodeInst m2m3a =
			LayoutLib.newNodeInst(tech.m2m3(),
					              m3.getTrackCenter(),
					              m2L.getTrackCenter(),
					              DEF_SIZE, DEF_SIZE, 0, parent);
		// left horizontal m2
		LayoutLib.newArcInst(tech.m2(), SIGNAL_WID, 
				             m2PortA,
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
		
		PortInst m2PortB = null;
		if (pR.getPortProto().connectsTo(tech.m1())) {
			// right port connects to m1
			NodeInst m1m2b = 
				LayoutLib.newNodeInst(tech.m1m2(), 
					              	  pR.getCenter().getX(),
					                  m2R.getTrackCenter(),
					                  DEF_SIZE, DEF_SIZE, 0, parent);
			
			LayoutLib.newArcInst(tech.m1(), SIGNAL_WID, 
        			             m1m2b.getOnlyPortInst(),
        			             pR);

			m2PortB = m1m2b.getOnlyPortInst();
		} else {
			m2PortB = pR;
		}
			
		LayoutLib.newArcInst(tech.m2(), SIGNAL_WID, 
				             m2m3b.getOnlyPortInst(),
				             m2PortB);

	}
	
	private boolean hasM2Pin(ToConnect toConn) {
		for (PortInst pi : toConn.getPortInsts()) {
			if (pi.getPortProto().connectsTo(tech.m2())) return true;
		}
		return false;
	}
	
	private boolean hasM3Pin(ToConnect toConn) {
		for (PortInst pi : toConn.getPortInsts()) {
			if (connectsToM3(pi)) return true;
		}
		return false;
	}
	
	private boolean connectsToM3(PortInst pi) {
		return pi.getPortProto().connectsTo(tech.m3());
	}
	
	private void connect2PinM3(ToConnect toConn) {
		PortInst pi1 = toConn.getPortInsts().get(0);
		PortInst pi2 = toConn.getPortInsts().get(1);
		LayoutLib.error(!connectsToM3(pi1) || ! connectsToM3(pi2),
				        "only 1 of 2 pins connects to m3");
		double x1 = pi1.getCenter().getX();
		double x2 = pi2.getCenter().getX();
		LayoutLib.error(x1!=x2, "m3 net not vertical");
		
		LayoutLib.newArcInst(tech.m3(), SIGNAL_WID, pi1, pi2);
		
	}
	
	private void route(List<ToConnect> toConns, LayerChannels m2Chan,
			           LayerChannels m3Chan) {
		// Connect m3 pins using vertical m3
		for (ToConnect toConn : toConns) {
			if (hasM3Pin(toConn))  connect2PinM3(toConn);
		}
		
		// We must route nets with m2 pins first because m2 pins allow us no
		// choice of m2 track.
		for (ToConnect toConn : toConns) {
			LayoutLib.error(toConn.size()!=2, "not two pin");
			if (hasM2Pin(toConn))  route2PinNet(toConn, m2Chan, m3Chan);
		}
		for (ToConnect toConn : toConns) {
			if (!hasM2Pin(toConn) && !hasM3Pin(toConn))  
				route2PinNet(toConn, m2Chan, m3Chan);
		}
	}
	
	private void dumpChannels(LayerChannels m2chan, LayerChannels m3chan) {
        prln("m2 channels");
        prln(m2chan.toString());
        
        prln("m3 channels");
        prln(m3chan.toString());
	}
	/** Find which PortInsts are already connected. Stick connected PortInsts 
	 * into a list. Return a list of such lists. */
	private List<List<PortInst>> groupConnectedPorts(ToConnect tc) {
		PortInst firstPi = tc.getPortInsts().get(0);
		Cell parent = firstPi.getNodeInst().getParent();
		Netlist nl = parent.getNetlist(true);
		Map<Network, List<PortInst>> netToPorts = 
			new HashMap<Network, List<PortInst>>();
		for (PortInst pi : tc.getPortInsts()) {
			Network n = nl.getNetwork(pi);
			List<PortInst> ports = netToPorts.get(n);
			if (ports==null) {
				ports = new ArrayList<PortInst>();
				netToPorts.put(n, ports);
			}
			ports.add(pi);
		}
		List<List<PortInst>> groupedPorts = new ArrayList<List<PortInst>>();
		for (Network n : netToPorts.keySet()) {
			groupedPorts.add(netToPorts.get(n));
		}
		return groupedPorts;
	}
	
	private double manhDist(PortInst pi1, PortInst pi2) {
		return Math.abs(pi1.getCenter().getX()-pi2.getCenter().getX()) +
		       Math.abs(pi1.getCenter().getY()-pi2.getCenter().getY());
	}
	private static class PortPair {
		public PortInst p1, p2;
		public double dist;
	}
	private PortPair findClosest(List<PortInst> pl1, List<PortInst> pl2) {
		PortPair closest = new PortPair();
		closest.dist = Double.MAX_VALUE;
		for (PortInst p1 : pl1) {
			for (PortInst p2 : pl2) {
				double d = manhDist(p1, p2);
				if (d<closest.dist) {
					closest.dist = d;
					closest.p1 = p1;
					closest.p2 = p2;
				}
			}
		}
		LayoutLib.error(closest.dist==Double.MAX_VALUE,
				        "empty port lists?");
		return closest;
	}
	private static class ClosestClusters {
		public int ndx1, ndx2;
		public PortPair pair = new PortPair();
	}
	private ClosestClusters findClosest(List<List<PortInst>> portLists) {
		ClosestClusters closest = new ClosestClusters();
		closest.pair.dist = Double.MAX_VALUE;
		for (int i=0; i<portLists.size(); i++) {
			for (int j=i+1; j<portLists.size(); j++) {
				PortPair pair = findClosest(portLists.get(i), portLists.get(j));
				if (pair.dist<closest.pair.dist) {
					closest.pair = pair;
					closest.ndx1 = i;
					closest.ndx2 = j;
				}
			}
		}
		return closest;
	}
	
	private List<ToConnect> reduceToTwoPin1(List<List<PortInst>> portLists) {
		List<ToConnect> twoPins = new ArrayList<ToConnect>();
		while (portLists.size()>1) {
			ClosestClusters cc = findClosest(portLists);
			ToConnect tc = new ToConnect();
			tc.addPortInst(cc.pair.p1);
			tc.addPortInst(cc.pair.p2);
			twoPins.add(tc);
			List<PortInst> pl1 = portLists.get(cc.ndx1);
			List<PortInst> pl2 = portLists.get(cc.ndx2);
			pl1.addAll(pl2);
			portLists.remove(cc.ndx2);
		}
		return twoPins;
	}

	/** Convert each ToConnect with more than two pins to multiple two 
	 * pin TwoConnects */ 
	private List<ToConnect> reduceToTwoPin(List<ToConnect> toConns) {
		List<ToConnect> twoPins = new ArrayList<ToConnect>();
		for (ToConnect tc : toConns) {
			// Some PortInsts on a ToConnect may already be connected in 
			// schematic by abut router
			List<List<PortInst>> connPorts = groupConnectedPorts(tc);

			// Generate a list of two pin ToConnects that connects  
			// disconnected pin lists.
			twoPins.addAll(reduceToTwoPin1(connPorts));
		}
		return twoPins;
	}
	
	private String addIntSuffix(String nm, int count) {
		if (count==0) return nm;
		else return nm + "_" + count;
	}
	
	private void exportPwrGnd(List<NodeInst> stages) {
		int vddCnt = 0;
		int gndCnt = 0;
		 Rectangle2D colBounds = findColBounds(stages);
		 for (NodeInst ni : stages) {
			 for (Iterator piIt=ni.getPortInsts(); piIt.hasNext();) {
				 PortInst pi = (PortInst) piIt.next();
				 if (isPwrGnd(pi)) {
					 if (onBounds(pi, colBounds, 0)) {
						 Cell parent = pi.getNodeInst().getParent();
						 String exptNm;
						 if (isPwr(pi)) {
							 exptNm = addIntSuffix("vdd", vddCnt++);
						 } else {
							 exptNm = addIntSuffix("gnd", gndCnt++);
						 }
						 Export.newInstance(parent, pi, exptNm);
					 }
				 }
			 }
		 }
	}
	
	private void reExport(Cell schCell) {
		Netlist nl = schCell.getNetlist(true);
		
	}
	
	public Infinity(Cell schCell) {
		prln("Generating layout for Infinity");
		Stages stages = findStageCells();
		if (stages.someStageIsMissing()) return;
		ensurePwrGndExportsOnBoundingBox(stages.getStages());
		
        Library autoLib = schCell.getLibrary();
        String groupName = schCell.getCellName().getName();

        Cell autoLay = Cell.newInstance(autoLib, groupName+"{lay}");
        
        List<NodeInst> stageInsts = addInstances(autoLay, schCell);
        connectPwrGnd(stageInsts);
        exportPwrGnd(stageInsts);
        
        List<ToConnect> toConns = getLayToConnFromSch(schCell, autoLay);
        List<ToConnect> twoPins = reduceToTwoPin(toConns);
        
        LayerChannels m2chan = new LayerChannels();
        LayerChannels m3chan = new LayerChannels();
        
        findChannels(m2chan, m3chan, stageInsts);
        
        route(twoPins, m2chan, m3chan);
        
        // Debug
        //dumpChannels(m2chan, m3chan);

        System.out.println("done.");
	}
}
