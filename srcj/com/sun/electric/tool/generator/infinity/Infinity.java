package com.sun.electric.tool.generator.infinity;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.generator.layout.AbutRouter;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.TechType;
import com.sun.electric.tool.generator.layout.LayoutLib.Corner;

public class Infinity {
	private static final String STAGE_LIB_NAME = "stagesF";
	private static final String WIRE_LIB_NAME = "wiresF";
	private static final String FAN_LIB_NAME = "fansF";
    private static final TechType tech = TechType.CMOS90;
    private static final double DEF_SIZE = LayoutLib.DEF_SIZE;
    private static final double M2_PWR_GND_WID = 9;
    private static final double M3_PWR_GND_WID = 21;
    private static final double SIGNAL_WID = 2.8;
    private static final double TRACK_PITCH = 6;
	private static final String[][] SCAN_PORT_NAMES = {
		{"si", "so", "sdc"},
		{"cscanIn", "cscanOut", "sdb"},
		{"rscanIn", "rscanOut", "sda"}
	};
	private static final int IN=0;
	private static final int OUT=1;
	private static final int INOUT = 2;
	private static final Set<String> SCAN_PORT_NAME_SET = new HashSet<String>();
	private static final int BITS_PER_SCAN_CHAIN = 9;
	private static final double PIN_HEIGHT = 0; 
	private static final double ROW_PITCH = 144;
//	private static final int NUM_PLACES_TO_STRETCH = 3;
//	private static final double STRETCH_AMOUNT = 2 * ROW_PITCH;
	private static final double FILL_CELL_WIDTH = 264;
	private static final double MIN_M2_LEN = 10;
	
	private static StringBuffer connMsg = new StringBuffer();
	private static boolean connMsgPrinted = false;
    
	static {
		for (int i=0; i<SCAN_PORT_NAMES.length; i++) {
			for (int j=0; j<2; j++) {
				SCAN_PORT_NAME_SET.add(SCAN_PORT_NAMES[i][j]);
			}
		}
	}
	private Library openPrimLib(String nm) {
		Library lib = Library.findLibrary(nm);
		error(lib==null, "Please open library: "+nm);
		return lib;
	}
	
	private final Library STAGE_LIB = openPrimLib(STAGE_LIB_NAME);
	private final Library WIRE_LIB = openPrimLib(WIRE_LIB_NAME);
	private final Library FAN_LIB = openPrimLib(FAN_LIB_NAME);
	
	private void saveConnectionMessage(ToConnect toConn, String numPinsMsg) {
		connMsg.setLength(0);
		connMsgPrinted = false;
		connMsg.append(numPinsMsg);
		connMsg.append(" pin ToConnect ports: ");

		for (PortInst pi : toConn.getPortInsts()) {
			connMsg.append(pi+" ");
		}
	}
	
	private static void printConnectionMessage() {
		if (connMsgPrinted) return;
		System.out.println(connMsg.toString());
		connMsgPrinted = true;
	}
	
	public static void prln(String s) {
		printConnectionMessage();
		System.out.println(s);
	}
	public static void pr(String s) {
		printConnectionMessage();
		System.out.print(s);
	}
	public static void error(boolean cond, String msg) {
		if (cond) {
			printConnectionMessage();
			LayoutLib.error(true, msg);
		}
	}
	
	private List<Library> findPrimLayLibs() {
		List<Library> libs = new ArrayList<Library>();
		libs.add(STAGE_LIB);
		libs.add(WIRE_LIB);
		libs.add(FAN_LIB);
		return libs;
	}
	
	private Stages findStageCells() {
		Stages stages = new Stages();
		return stages;
	}
	private void checkStageExports(Collection<Cell> stages) {
		for (Cell c : stages) {
			Rectangle2D bnds = c.findEssentialBounds();
			if (bnds==null) {
				prln("Stage: "+c.getName()+" is missing essential bounds");
				continue;
			}
			for (Iterator it=c.getExports(); it.hasNext();) {
				Export e = (Export) it.next();
				if (isPwrGnd(e)) {
					PortInst pi = e.getOriginalPort();
					if (!onBounds(pi, bnds, 0)) {
						prln("Cell: "+c.getName()+", Export: "+e.getName()+
							 " Export not on Cell Bounding Box");
						prln("  Bounding box: "+bnds.toString());
						prln("  Port Center: "+pi.getCenter().toString());
						onBounds(pi, bnds, 0);
					}
					double w = LayoutLib.widestWireWidth(pi);
					if (w!=9) {
						prln("Cell: "+c.getName()+", power or ground Export: "+
							 e.getName()+" has width: "+w);
					}
				} else {
					PortCharacteristic pc = e.getCharacteristic();
					if (pc!=PortCharacteristic.IN && pc!=PortCharacteristic.OUT &&
						pc!=PortCharacteristic.BIDIR) {
						prln("Cell: "+c.getName()+" Export "+e+" has undesired characteristic: "+pc);
					}
				}
			}
		}
	}
	
//	private List<NodeInst> addInstances(Cell parentCell, Stages stages) {
//		List<NodeInst> stageInsts = new ArrayList<NodeInst>();
//		for (Cell c : stages.getStages()) {
//			stageInsts.add(LayoutLib.newNodeInst(c, 0, 0, 0, 0, 0, parentCell));
//		}
//		LayoutLib.abutBottomTop(stageInsts, STAGE_SPACING);
//		return stageInsts;
//	}
	
//	private void sortSchStageInstsByX(List<Nodable> stageInsts) {
//		Collections.sort(stageInsts, new Comparator<Nodable>() {
//			public int compare(Nodable n1, Nodable n2) {
//				double x1 = n1.getNodeInst().getBounds().getMinX();
//				double x2 = n2.getNodeInst().getBounds().getMinX();
//				double delta = x1 - x2;
//				if (delta>0) return 1;
//				if (delta==0) return 0;
//				if (delta<0) return -1;
//				error(true, "stage instances x-coord unordered "+
//						        "in schematic");
//				return 0;
//			}
//		});
//	}
	
//	private List<Nodable> getSchematicIconInstances(Cell autoSch) {
//		//Library stageLib = Library.findLibrary(STAGE_LIB_NAME);
//		List<Nodable> stageInsts = new ArrayList<Nodable>();
//
//		for (Iterator nIt=autoSch.getNodables(); nIt.hasNext();) {
//			Nodable na = (Nodable) nIt.next();
//			NodeProto np = na.getNodeInst().getProto();
//			if (!(np instanceof Cell)) continue;
//			Cell icon = (Cell) np;
//			
//			// for schematic X discard icons X
//			Cell sch = icon.getEquivalent();
//			if (sch==autoSch) continue;
//			
//			//prln("Schematic instantiates: "+icon.getName());
//			//if (c.getLibrary()==stageLib) stageInsts.add(na);
//			stageInsts.add(na);
//		}
//		sortSchStageInstsByX(stageInsts);
//		return stageInsts;
//	}
	
//	private Cell findLayout(Cell schCell) {
//		CellGroup group = schCell.getCellGroup();
//		for (Iterator<Cell> cIt=group.getCells(); cIt.hasNext();) {
//			Cell c = cIt.next();
//			if (c.getView()==View.LAYOUT) return c;
//		}
//		return null;
//	}
	
//	private List<NodeInst> addInstances(Cell parentCell, Cell autoSch) {
//		List<NodeInst> layInsts = new ArrayList<NodeInst>();
//		List<Nodable> schIconInsts = getSchematicIconInstances(autoSch);
//		for (Nodable no : schIconInsts) {
//			Cell schCell = (Cell) no.getProto();
//			//prln("Name of schematic instance is: "+schCell.getName());
//			Cell layCell = findLayout(schCell);
//			error(layCell==null, 
//					        "Can't find layout for cell: "+schCell.getName());
//			NodeInst layInst = LayoutLib.newNodeInst(layCell, 0, 0, 0, 0, 0, parentCell);
//			layInst.setName(no.getName());
//			layInsts.add(layInst);
//		}
//		LayoutLib.abutBottomTop(layInsts, STAGE_SPACING);
//		return layInsts;
//	}
	
	private void connectPwrGnd(List<NodeInst> nodeInsts) {
		List<ArcProto> vertLayers = new ArrayList<ArcProto>();
		vertLayers.add(tech.m3());
		NodeInst prev = null;
		for (NodeInst ni : nodeInsts) {
			if (prev!=null) {
				AbutRouter.abutRouteBotTop(prev, ni, 0, vertLayers);
			}
			prev = ni;
		}
	}
	
//	/** From the schematic, get a list of connections that need to be made
//	 * in the layout */
//	private List<ToConnect> getLayToConnFromSch(Cell autoSch,
//			                                    Cell autoLay) {
//		List<ToConnect> toConnects = new ArrayList<ToConnect>();
//		List<Nodable> stageInsts = getSchematicIconInstances(autoSch);
//		Map<Network,ToConnect> netToConn = new HashMap<Network,ToConnect>();
//		Netlist schNets = autoSch.getNetlist(true);
//		
//		for (Nodable schInst : stageInsts) {
//			String schInstNm = schInst.getName();
//			NodeInst layInst = autoLay.findNode(schInstNm);
//			error(layInst==null, "layout instance missing");
//			
//			Cell schCell = (Cell) schInst.getProto();
//			for (Iterator eIt=schCell.getExports(); eIt.hasNext();) {
//				Export e = (Export) eIt.next();
//				Name eNmKey = e.getNameKey();
//				int busWid = eNmKey.busWidth();
//				for (int i=0; i<busWid; i++) {
//					String subNm = eNmKey.subname(i).toString();
//					PortInst layPortInst = layInst.findPortInst(subNm);
//					error(layPortInst==null,
//							        "layout instance port missing");
//					Network schNet = schNets.getNetwork(schInst, e, i);
//					ToConnect conn = netToConn.get(schNet);
//					if (conn==null) {
//						conn = new ToConnect(schNet.getExportedNames());
//						netToConn.put(schNet, conn);
//					}
//					conn.addPortInst(layPortInst);
//				}
//			}
//		}
//		
//		for (Network n : netToConn.keySet())  toConnects.add(netToConn.get(n));
//		
//		// debug
//		//for (ToConnect cl : toConnects)  prln("  "+cl.toString());
//
//		return toConnects;
//	}
	
	private boolean nextToBoundary(double coord, double boundCoord, double fudge) {
		return Math.abs(coord-boundCoord) <= fudge;
	}
	
	private boolean isPowerOrGround(ToConnect tc) {
		for (PortInst pi : tc.getPortInsts()) {
			if (isPwrGnd(pi)) return true;
		}
		return false;
	}

	private boolean isPwr(PortProto pp) {
		return pp.getCharacteristic()==PortCharacteristic.PWR;
	}
	private boolean isPwr(PortInst pi) {
		return isPwr(pi.getPortProto());
	}
	private boolean isGnd(PortProto pp) {
		return pp.getCharacteristic()==PortCharacteristic.GND;
	}
	private boolean isGnd(PortInst pi) {
		return isGnd(pi.getPortProto());
	}
	private boolean isPwrGnd(PortProto pp) {
		return isPwr(pp) || isGnd(pp);
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
			error(bounds==null, 
					        "Layout Cell is missing essential bounds: "+
					        ni.getProto().describe(false));
			minX = Math.min(minX, bounds.getMinX());
			maxX = Math.max(maxX, bounds.getMaxX());
			minY = Math.min(minY, bounds.getMinY());
			maxY = Math.max(maxY, bounds.getMaxY());
		}
		return new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
	}
	private boolean onTop(PortInst pi, Rectangle2D bounds, double fudge) {
		double y = pi.getCenter().getY();
		return nextToBoundary(y, bounds.getMaxY(), fudge);
 	}
	private boolean onBottom(PortInst pi, Rectangle2D bounds, double fudge) {
		double y = pi.getCenter().getY();
		return nextToBoundary(y, bounds.getMinY(), fudge);
 	}
	private boolean onTopOrBottom(PortInst pi, Rectangle2D bounds, double fudge) {
		return onTop(pi, bounds, fudge) || onBottom(pi, bounds, fudge);
	}
	private boolean onLeftOrRight(PortInst pi, Rectangle2D bounds, double fudge) {
		double x = pi.getCenter().getX();
		return nextToBoundary(x, bounds.getMinX(), fudge) ||
	           nextToBoundary(x, bounds.getMaxX(), fudge);
	}
	private boolean onBounds(PortInst pi, Rectangle2D bounds, double fudge) {
		return onTopOrBottom(pi, bounds, fudge) ||
		       onLeftOrRight(pi, bounds, fudge);
	}
	
	private boolean isScan(PortInst pi) {
		String nm = pi.getPortProto().getName();
		
		// strip the array index
		int openBracket = nm.indexOf('[');
		if (openBracket==-1) return false;
		nm = nm.substring(0, openBracket);
		return SCAN_PORT_NAME_SET.contains(nm);
	}
	
	private void findChannels(LayerChannels m2Chnls, LayerChannels m3Chnls,
			                  Collection<NodeInst> stages,
			                  Rectangle2D colBounds) {
		Blockage1D m2block = new Blockage1D();
		Blockage1D m3block = new Blockage1D();
		for (NodeInst ni : stages) {
			for (Iterator piIt=ni.getPortInsts(); piIt.hasNext();) {
				PortInst pi = (PortInst) piIt.next();
				double x = pi.getCenter().getX();
				double y = pi.getCenter().getY();
				if (isPwrGnd(pi) || isScan(pi)) {
					if (connectsToM2(pi)) {
						// horizontal m2 channel
						m2block.block(y-M2_PWR_GND_WID/2, y+M2_PWR_GND_WID/2);
					} else if (connectsToM3(pi)) {
						// vertical m3 channel
						m3block.block(x-M3_PWR_GND_WID/2, x+M3_PWR_GND_WID/2);
					} else {
						error(true, "unexpected metal for port: "+pi.toString());
					}
				}
			}
		}
		Interval prv = null;
		for (Interval i : m2block.getBlockages()) {
			// debug
			//prln("Interval: "+i.toString());
			if (prv!=null) {
				m2Chnls.add(new Channel(true, colBounds.getMinX(),
						                colBounds.getMaxX(),
					                    prv.getMax(), i.getMin(), "metal-2"));
			}
			prv = i;
		}

		prv = null;
		for (Interval i : m3block.getBlockages()) {
			if (prv!=null) {
				m3Chnls.add(new Channel(false, colBounds.getMinY(),    
						                colBounds.getMaxY(),
					                    prv.getMax(), i.getMin(), "metal-3"));
			}
			prv = i;
		}
	}
	
	private void sortLeftToRight(List<PortInfo> pis) {
		Collections.sort(pis, new Comparator<PortInfo>() {
			public int compare(PortInfo p1, PortInfo p2) {
				double diff = p1.portInst.getCenter().getX() -
				              p2.portInst.getCenter().getX();
				return (int) Math.signum(diff);
			}
		});
	}
	private void sortBotToTop(List<PortInfo> pis) {
		Collections.sort(pis, new Comparator<PortInfo>() {
			public int compare(PortInfo p1, PortInfo p2) {
				double diff = p1.portInst.getCenter().getY() -
				              p2.portInst.getCenter().getY();
				return (int) Math.signum(diff);
			}
		});
	}
//	private void sortPortInstsBotToTop(List<PortInst> pis) {
//		Collections.sort(pis, new Comparator<PortInst>() {
//			public int compare(PortInst p1, PortInst p2) {
//				double diff = p1.getCenter().getY() -
//				              p2.getCenter().getY();
//				return (int) Math.signum(diff);
//			}
//		});
//	}
	private void routeTwoOrThreePinNet(ToConnect toConn, LayerChannels m2Chan,
            LayerChannels m3Chan) {
		if (toConn.size()==2) routeTwoPinNet(toConn, m2Chan, m3Chan);
		if (toConn.size()==3) routeThreePinNet(toConn, m2Chan, m3Chan);
	}
	
//	private Rectangle2D getPortInstBounds(ToConnect toConn) {
//		double minX, minY, maxX, maxY;
//		minX = minY = Double.MAX_VALUE;
//		maxX = maxY = Double.MIN_VALUE;
//		for (PortInst pi : toConn.getPortInsts()) {
//			double x = pi.getCenter().getX();
//			double y = pi.getCenter().getY();
//			if (x<minX) minX=x;
//			if (x>maxX) maxX=x;
//			if (y<minY) minY=y;
//			if (y>maxY) maxY=y;
//		}
//		return new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
//	}
	
	private class PortInfo {
		public final PortInst portInst;
		public final double x, y, maxY, minY;
		public final Channel m2Chan;
		public Segment m2Seg;
		public PortInfo(PortInst pi, LayerChannels m2Chans) {
			portInst = pi;
			x = pi.getCenter().getX();
			y = pi.getCenter().getY();
			maxY = y + PIN_HEIGHT;
			minY = y - PIN_HEIGHT;
			m2Chan = m2Chans.findChanOverVertInterval(x, minY, maxY);
			if (m2Chan==null) {
				prln("no m2 channel for PortInst: "+pi.toString());
				prln(m2Chans.toString());
			}
		}
		public void getM2OnlySeg(double xL, double xR) {
			if (connectsToM2(portInst)) {
				m2Seg = m2Chan.allocateBiggestFromTrack(xL-TRACK_PITCH, 
						                                x, xR+TRACK_PITCH, y);
				if (m2Seg==null) {
					prln("failed to get segment for m2-only PortInst: center="+y+
						 "["+(xL-TRACK_PITCH)+", "+(xR+TRACK_PITCH)+"]");				
					prln(m2Chan.toString());
				}
			}
		}
	}
	
	// Single vertical m3 connects to all PortInsts in m2
	private void routeThreePinNet(ToConnect toConn, LayerChannels m2Chan,
	                              LayerChannels m3Chan) {
		saveConnectionMessage(toConn, "Three");

		List<PortInst> pis = toConn.getPortInsts();

		List<PortInfo> infos = new ArrayList<PortInfo>();
		for (PortInst pi : pis) {
			PortInfo inf = new PortInfo(pi, m2Chan);
			if (inf.m2Chan==null) return;
			infos.add(inf);
		}
		
		sortLeftToRight(infos);
		PortInfo infoL = infos.get(0);
		PortInfo infoLR = infos.get(1);
		PortInfo infoR = infos.get(2);

		sortBotToTop(infos);
		PortInfo infoB = infos.get(0);
		PortInfo infoT = infos.get(2);

		/** For m2-only pins, allocate m2 segment that can connect any
		 *  two PortInsts */
		for (PortInfo inf : infos) inf.getM2OnlySeg(infoL.x, infoR.x);
		
		// share m2 segments if we can connect PortInsts using m2
		for (int i=0; i<infos.size(); i++) {
			PortInfo inf1 = infos.get(i);
			for (int j=i+1; j<infos.size(); j++) {
				PortInfo inf2 = infos.get(j);
				if (inf1.m2Chan==inf2.m2Chan && !(inf1.m2Seg!=null && inf2.m2Seg!=null)) {
					// connect using m2 
					if (inf1.m2Seg==null && inf2.m2Seg!=null) {
						inf2.m2Seg = inf1.m2Seg;
					} else if (inf1.m2Seg!=null && inf2.m2Seg==null) {
						inf2.m2Seg = inf1.m2Seg; 
					} else {
						// both are null
						// allocate m2 segment that can connect any two PortInsts
						inf1.m2Seg = inf2.m2Seg = 
							inf1.m2Chan.allocate(infoL.x, infoR.x, inf1.y, inf2.y);
						if (inf1.m2Seg==null) return;
					}
				}
			}
		}
		
		// In principle we should test to see if all three pins can be connected 
		// in m2 only. Leave this for later.
		
		// need m3 channel to connect top and bottom m2 channels
		Channel c3 = m3Chan.findVertBridge(infoB.m2Chan, infoT.m2Chan,
				                           infoL.x, infoR.x);
		if (c3==null) {prln("no m3 channel"); return;}

		Segment m3Seg = c3.allocate(infoB.m2Chan.getMinTrackCenter(), 
				                    infoT.m2Chan.getMaxTrackCenter(), 
				                    infoL.x, infoR.x);
		if (m3Seg==null) return;

		// allocate m2 segments for PortInsts that don't share m2 and 
		// that aren't m2 only.
		for (PortInfo inf : infos) {
			if (inf.m2Seg==null) {
				inf.m2Seg = inf.m2Chan.allocate(infoL.x, infoR.x, infoB.y, infoT.y);
				if (inf.m2Seg==null)  return;
			}
		}

		routeUseM3(infoL.portInst, infoLR.portInst, infoL.m2Seg, infoLR.m2Seg, 
				   m3Seg);
		routeUseM3(infoLR.portInst, infoR.portInst, infoLR.m2Seg, infoR.m2Seg,
				   m3Seg);
	}
	
	
	private void routeTwoPinNet(ToConnect toConn, LayerChannels m2Chan,
	                            LayerChannels m3Chan) {
		List<PortInst> pis = toConn.getPortInsts();
		
		saveConnectionMessage(toConn, "Two");

		List<PortInfo> infos = new ArrayList<PortInfo>();
		for (PortInst pi : pis) {
			PortInfo inf = new PortInfo(pi, m2Chan);
			if (inf.m2Chan==null) return;
			infos.add(inf);
		}
		
		sortLeftToRight(infos);
		PortInfo infoL = infos.get(0);
		PortInfo infoR = infos.get(1);
		
		/** For m2 only pins, allocate m2 segment that can connect any
		 *  two PortInsts */
		for (PortInfo inf : infos) inf.getM2OnlySeg(infoL.x, infoR.x);

		Segment s3 = null;
		
		if (infoL.m2Chan==infoR.m2Chan && !(infoL.m2Seg!=null && infoR.m2Seg!=null)) {
			// connect using m2 only
			if (infoL.m2Seg==null && infoR.m2Seg!=null) {
				infoR.m2Seg = infoL.m2Seg;
			} else if (infoL.m2Seg!=null && infoR.m2Seg==null) {
				infoR.m2Seg = infoL.m2Seg; 
			} else if (infoL.m2Seg==null && infoR.m2Seg==null){
				infoL.m2Seg = infoR.m2Seg = 
					infoL.m2Chan.allocate(infoL.x, infoR.x, infoL.y, infoR.y);
				if (infoL.m2Seg==null) return;
			}
		} else {
			// need to use m3
			// need m3 channel to connect two m2 channels
			Channel c3 = m3Chan.findVertBridge(infoL.m2Chan, infoR.m2Chan, 
					                           infoL.x, infoR.x);
			if (c3==null) {prln("no m3 channel"); return;}
			
			// allocate segments for PortInsts that don't share m2 and that aren't 
			// m2 only.
			for (PortInfo inf : infos) {
				if (inf.m2Seg==null) {
					inf.m2Seg = inf.m2Chan.allocate(infoL.x, infoR.x, infoL.y, infoR.y);
					if (inf.m2Seg==null) return;
				}
			}
				
			// use more than necessary because using adjacent tracks causes via 
			// spacing violations
			double minY = Math.min(infoL.m2Seg.getTrackCenter(), 
					               infoR.m2Seg.getTrackCenter());
			double maxY = Math.max(infoL.m2Seg.getTrackCenter(), 
					               infoR.m2Seg.getTrackCenter());
			s3 = c3.allocate(minY, maxY, infoL.x, infoR.x);
			
			// hack, return the unused portion of both m2Segs that we don't need
			// I need to do this because of two metal-2-only pins in the same 
			// m3 routing channel.
			infoL.m2Seg.trim(infoL.x-TRACK_PITCH, s3.getTrackCenter()+TRACK_PITCH);
			infoR.m2Seg.trim(s3.getTrackCenter()-TRACK_PITCH, infoR.x+TRACK_PITCH);
		}

		// do the actual routing right now. In a two level
		// scheme we would actually postpone this
		routeUseM3(infoL.portInst, infoR.portInst, infoL.m2Seg, infoR.m2Seg, s3);
	}
	
	private void routeUseM3(PortInst pL, PortInst pR, Segment m2L, Segment m2R, Segment m3) {
		if (m2L==null)  {prln("no m2 track for left PortInst");}
		if (m2R==null)  {prln("no m2 track for right PortInst");}
		if (m3==null)   {prln("no m3 track");}
		if (m2L==null || m2R==null || m3==null) return;
		
		//prln("  Route m3: "+m3.toString());
		
		PortInst m2PortA = null;
		
		Cell parent = pL.getNodeInst().getParent();
		if (connectsToM1(pL)) {
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
		
		PortInst m2PortB = null;
		if (connectsToM1(pR)) {
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

		if (m3!=null) {
			NodeInst m2m3a =
				LayoutLib.newNodeInst(tech.m2m3(),
						              m3.getTrackCenter(),
						              m2L.getTrackCenter(),
						              DEF_SIZE, DEF_SIZE, 0, parent);
			// left horizontal m2
			newM2SignalWire(m2PortA, m2m3a.getOnlyPortInst());
			NodeInst m2m3b =
				LayoutLib.newNodeInst(tech.m2m3(),
						              m3.getTrackCenter(),
						              m2R.getTrackCenter(),
						              DEF_SIZE, DEF_SIZE, 0, parent);
			// vertical m3
			LayoutLib.newArcInst(tech.m3(), SIGNAL_WID, 
					             m2m3a.getOnlyPortInst(),
					             m2m3b.getOnlyPortInst());
			
			// right horizontal m2
			newM2SignalWire(m2m3b.getOnlyPortInst(), m2PortB);
		} else {
			newM2SignalWire(m2PortA, m2PortB);
		}
	}
	
	private void newM2SignalWire(PortInst p1, PortInst p2) {
		PortInst pL, pR;
		if (p1.getCenter().getX()<p2.getCenter().getX()) {
			pL=p1; pR=p2;
		} else {
			pL=p2; pR=p1;
		}
		Cell parent = pL.getNodeInst().getParent();
		double y = p1.getCenter().getY();
		double yR = p2.getCenter().getY();
		error(y!=yR, "M2 must be horizontal");
		double xL = pL.getCenter().getX();
		double xR = pR.getCenter().getX();
		double len = xR-xL;
		double extend = MIN_M2_LEN - (len + SIGNAL_WID);
		if (extend > 0) {
			// round extention to tenths of a lambda
			double halfExt = Math.ceil(10*extend/2)/10;
			//prln("halfExt: "+halfExt);
			// add additional m2 to this wire
			NodeInst pin1 = LayoutLib.newNodeInst(tech.m2pin(), xL-halfExt, y, 
					                              DEF_SIZE, DEF_SIZE, 0, 
					                              parent);
			NodeInst pin2 = LayoutLib.newNodeInst(tech.m2pin(), xR+halfExt, y, 
								                  DEF_SIZE, DEF_SIZE, 0, 
								                  parent);
			LayoutLib.newArcInst(tech.m2(), SIGNAL_WID, 
					             pin1.getOnlyPortInst(), pL);
			LayoutLib.newArcInst(tech.m2(), SIGNAL_WID, 
		                         pR, pin2.getOnlyPortInst());
		}
		LayoutLib.newArcInst(tech.m2(), SIGNAL_WID, pL, pR);
	}
	
	private boolean connectsToM1(PortProto pp) {
		return pp.connectsTo(tech.m1());
	}
	private boolean connectsToM1(PortInst pi) {
		return connectsToM1(pi.getPortProto());
	}
	private boolean connectsToM2(PortProto pp) {
		return pp.connectsTo(tech.m2());
	}
	private boolean connectsToM2(PortInst pi) {
		return connectsToM2(pi.getPortProto());
	}
	private boolean connectsToM3(PortProto pp) {
		return pp.connectsTo(tech.m3());
	}
	private boolean connectsToM3(PortInst pi) {
		return connectsToM3(pi.getPortProto());
	}
	
	private boolean hasM2Pin(ToConnect toConn) {
		for (PortInst pi : toConn.getPortInsts()) {
			if (connectsToM2(pi)) return true;
		}
		return false;
	}
	
	private boolean hasM3Pin(ToConnect toConn) {
		for (PortInst pi : toConn.getPortInsts()) {
			if (connectsToM3(pi)) return true;
		}
		return false;
	}
	
//	private void connect2PinM3(ToConnect toConn) {
//		prln("M3 "+toConn.toString());
//		PortInst pi1 = toConn.getPortInsts().get(0);
//		PortInst pi2 = toConn.getPortInsts().get(1);
//		error(!connectsToM3(pi1) || ! connectsToM3(pi2),
//				        "only 1 of 2 pins connects to m3");
//		double x1 = pi1.getCenter().getX();
//		double x2 = pi2.getCenter().getX();
//		error(x1!=x2, "m3 net not vertical");
//		
//		LayoutLib.newArcInst(tech.m3(), SIGNAL_WID, pi1, pi2);
//		
//	}
	
	private void route(List<ToConnect> toConns, LayerChannels m2Chan,
			           LayerChannels m3Chan) {
		// Connect m3 pins using vertical m3
//		for (ToConnect toConn : toConns) {
//			if (hasM3Pin(toConn))  connect2PinM3(toConn);
//		}

		// We must route nets with m2 pins first because m2 pins allow us no
		// choice of m2 track.
		for (ToConnect toConn : toConns) {
			if (hasM2Pin(toConn))  routeTwoOrThreePinNet(toConn, m2Chan, m3Chan);
		}

		for (ToConnect toConn : toConns) {
			if (!hasM2Pin(toConn) && !hasM3Pin(toConn))  
				routeTwoOrThreePinNet(toConn, m2Chan, m3Chan);
		}

	}
	
//	private void dumpChannels(LayerChannels m2chan, LayerChannels m3chan) {
//        prln("m2 channels");
//        prln(m2chan.toString());
//        
//        prln("m3 channels");
//        prln(m3chan.toString());
//	}
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
		error(closest.dist==Double.MAX_VALUE,
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
	
//	private List<ToConnect> reduceToTwoPinX(List<List<PortInst>> portLists) {
//		List<ToConnect> twoPins = new ArrayList<ToConnect>();
//		while (portLists.size()>1) {
//			ClosestClusters cc = findClosest(portLists);
//			ToConnect tc = new ToConnect(null);
//			tc.addPortInst(cc.pair.p1);
//			tc.addPortInst(cc.pair.p2);
//			twoPins.add(tc);
//			List<PortInst> pl1 = portLists.get(cc.ndx1);
//			List<PortInst> pl2 = portLists.get(cc.ndx2);
//			pl1.addAll(pl2);
//			portLists.remove(cc.ndx2);
//		}
//		return twoPins;
//	}
	
	/** Special case.  If a net has exactly two clusters, it only needs one connection
	 * between two PortInsts. Return those two PortInsts. */
	private List<List<PortInst>> makeTwoClusterSimple(List<List<PortInst>> portLists) {
		error(portLists.size()!=2, "only handle 2 clusters");
		ClosestClusters cc = findClosest(portLists);
		List<List<PortInst>> pls = new ArrayList<List<PortInst>>();
		List<PortInst> pl = new ArrayList<PortInst>();
		pl.add(cc.pair.p1);
		pls.add(pl);
		pl = new ArrayList<PortInst>();
		pl.add(cc.pair.p2);
		pls.add(pl);
		return pls;
	}

	/** Convert each ToConnect with more than two pins to multiple two 
	 * pin TwoConnects */ 
//	private List<ToConnect> reduceToTwoPin(List<ToConnect> toConns) {
//		List<ToConnect> twoPins = new ArrayList<ToConnect>();
//		for (ToConnect tc : toConns) {
//			// Skip Exported net that touches no stage PortInsts 
//			if (tc.size()==0) continue;
//			
//			// Some PortInsts on a ToConnect may already be connected in 
//			// schematic by abut router
//			List<List<PortInst>> connPorts = groupConnectedPorts(tc);
//
//			// Generate a list of two pin ToConnects that connects  
//			// disconnected pin lists.
//			twoPins.addAll(reduceToTwoPinX(connPorts));
//		}
//		return twoPins;
//	}
	
	private void dumpConnPorts(List<List<PortInst>> connPorts) {
		prln("Clustered port connections:");
		for (List<PortInst> ports : connPorts) {
			pr("    cluster: ");
			for (PortInst port : ports) pr(port.toString()+" ");
			prln("");
		}
	}
	
	
	// return true if ok
	private boolean isSimple(List<List<PortInst>> connPorts) {
		// if no cluster then nothing to connect
		// if one cluster then everything already connected
		if (connPorts.size()==0 || connPorts.size()==1) return true;
		
		if (connPorts.size()==2 || connPorts.size()==3) {
			for (List<PortInst> ports : connPorts) {
				if (ports.size()!=1) {
					prln("Can't handle pre-connected PortInsts");
					dumpConnPorts(connPorts);
					return false;
				}
			}
			return true;
		}
		
		prln("Can't handle Nets that connect more than three PortInsts:");
		dumpConnPorts(connPorts);
		return false;
	}
	
	private List<ToConnect> reduceToTwoOrThreePin(List<ToConnect> toConns) {
		List<ToConnect> twoOrThreePin = new ArrayList<ToConnect>();
		for (ToConnect tc : toConns) {
			// Skip Exported net that touches no stage PortInsts 
			if (tc.size()==0) continue;
			
			// Some PortInsts on a ToConnect may already be connected in 
			// schematic by abut router or by scan chain stitcher
			List<List<PortInst>> connPorts = groupConnectedPorts(tc);
			
			if (connPorts.size()==2)  connPorts = makeTwoClusterSimple(connPorts);
			
			// make sure this routing problem is simple
			if (!isSimple(connPorts)) continue;
			
			if (connPorts.size()==2 || connPorts.size()==3) {
				ToConnect tcX = new ToConnect(null);
				for (List<PortInst> ports : connPorts) {
					error(ports.size()!=1, "We only allow one port per cluster");
					tcX.addPortInst(ports.get(0));
				}
				twoOrThreePin.add(tcX);
			}
		}
		return twoOrThreePin;
	}
	
//	private boolean portOnLayer(PortInst pi, List<ArcProto> layers) {
//		for (ArcProto ap : layers) {
//			if (pi.getPortProto().connectsTo(ap)) return true;
//		}
//		return false;
//	}
	private void getM3PwrGndExports(Map<Double, PortInst> pwr, Map<Double, PortInst> gnd, 
			                        NodeInst ni, double y) {
		for (Iterator piIt=ni.getPortInsts(); piIt.hasNext();) {
			PortInst pi = (PortInst) piIt.next();
			if (pi.getCenter().getY()!=y) continue;
			if (!connectsToM3(pi)) continue;
			double x  = pi.getCenter().getX();
			if (isPwr(pi)) pwr.put(x, pi);
			else if (isGnd(pi)) gnd.put(x, pi);
		}
	}
	
	private void fattenVerticalM3(Map<Double, PortInst> bot,
			                      Map<Double, PortInst> top) {
		for (Double x : bot.keySet()) {
			PortInst piBot = bot.get(x);
			PortInst piTop = top.get(x);
			if (piTop==null) continue;
			LayoutLib.newArcInst(tech.m3(), M3_PWR_GND_WID, 
		                         piBot, piTop);
		}
	}
	
	// Hack until I get a chance to fatten based upon need.
	private void fattenPwrGnd(List<NodeInst> stages, Rectangle2D colBounds) {
		NodeInst topInst = stages.get(stages.size()-1);
		Map<Double, PortInst> topPwr = new TreeMap<Double, PortInst>();
		Map<Double, PortInst> topGnd = new TreeMap<Double, PortInst>();
		getM3PwrGndExports(topPwr, topGnd, topInst, colBounds.getMaxY());
		
		NodeInst botInst = stages.get(0);
		Map<Double, PortInst> botPwr = new TreeMap<Double, PortInst>();
		Map<Double, PortInst> botGnd = new TreeMap<Double, PortInst>();
		getM3PwrGndExports(botPwr, botGnd, botInst, colBounds.getMinY());

		fattenVerticalM3(botGnd, topGnd);
		fattenVerticalM3(botPwr, topPwr);
	}
	
//	private int[] exportPwrGnd(List<NodeInst> stages, Rectangle2D colBounds) {
//		
//		List<ArcProto> vertLayers = new ArrayList<ArcProto>();
//		vertLayers.add(tech.m3());
//		List<ArcProto> horiLayers = new ArrayList<ArcProto>();
//		horiLayers.add(tech.m2());
//		int vddCnt = 0;
//		int gndCnt = 0;
//		for (NodeInst ni : stages) {
//			 for (Iterator piIt=ni.getPortInsts(); piIt.hasNext();) {
//				 PortInst pi = (PortInst) piIt.next();
//				 if (isPwrGnd(pi)) {
//					 if ((onTopOrBottom(pi, colBounds, 0) && 
//					      portOnLayer(pi, vertLayers)) ||
//						 (onLeftOrRight(pi, colBounds, 0) && 
//						  portOnLayer(pi, horiLayers))) {
//						 Cell parent = pi.getNodeInst().getParent();
//						 String exptNm;
//						 if (isPwr(pi)) {
//							 exptNm = addIntSuffix("vdd", vddCnt++);
//						 } else {
//							 exptNm = addIntSuffix("gnd", gndCnt++);
//						 }
//						 Export.newInstance(parent, pi, exptNm);
//					 }
//				 }
//			 }
//		}
//		fattenPwrGnd(stages, colBounds, vddCnt, gndCnt);
//		return new int[] {vddCnt, gndCnt};
//	}
	
	private static class CloseToBound implements Comparator<PortInst> {
		private Rectangle2D bound;
		private double distToBound(PortInst pi) {
			double x = pi.getCenter().getX();
			double l = Math.abs(x - bound.getMinX());
			double r = Math.abs(x - bound.getMaxX());
			double y = pi.getCenter().getY();
			double b = Math.abs(y - bound.getMinY());
			double t = Math.abs(y - bound.getMaxY());
			return Math.min(Math.min(l, r), 
					        Math.min(t, b)); 
		}
		public CloseToBound(Rectangle2D bound) {this.bound=bound;}
		public int compare(PortInst pi1, PortInst pi2) {
			double d = distToBound(pi1) - distToBound(pi2);
			return (int) Math.signum(d);
		}
	}
	
	/** Re-export all ports that the schematic exports. Don't do power
	 * and ground because they are handled by another method. */
	private void reExport(List<ToConnect> toConns, Rectangle2D colBounds) {
		CloseToBound closeToBound = new CloseToBound(colBounds);
		for (ToConnect tc : toConns) {
			if (tc.size()>0 && tc.isExported() && !isPowerOrGround(tc)) {
				// separate out connected and not connected PortInsts
				// Because PortInst.hasConnections() is so slow, make sure
				// we do this test once. Keeping two different lists accomplishes
				// this.
				List<PortInst> unconnPorts = new ArrayList<PortInst>();
				List<PortInst> connPorts = new ArrayList<PortInst>();
				for (PortInst pi : tc.getPortInsts()) {
					if (pi.hasConnections()) connPorts.add(pi);
					else  unconnPorts.add(pi);
				}
				Collections.sort(connPorts, closeToBound);
				Collections.sort(unconnPorts, closeToBound);

				// Export disconnected PortInsts first
				unconnPorts.addAll(connPorts);
				
				int portNdx = 0;
				for (String expNm : tc.getExportName()) {
					PortInst pi = unconnPorts.get(portNdx++);
					//prln("Add Export: "+expNm);
					Export.newInstance(pi.getNodeInst().getParent(), 
							           pi, expNm);
				}
			}
		}
	}
	
	private void checkStageLibrary() {
		Stages stages = findStageCells();
		if (stages.someStageIsMissing()) return;
		checkStageExports(stages.getStages());
	}
	
	private void addEssentialBounds(List<NodeInst> stages, 
			                        Rectangle2D colBounds) {
		 Cell parent = stages.get(0).getParent();
		 LayoutLib.newNodeInst(tech.essentialBounds(), 
				               colBounds.getMinX(),
				               colBounds.getMinY(),
				               DEF_SIZE, DEF_SIZE, 0, parent);
		 LayoutLib.newNodeInst(tech.essentialBounds(), 
				               colBounds.getMaxX(),
				               colBounds.getMaxY(),
				               DEF_SIZE, DEF_SIZE, 0, parent);
	}
	
	private static class CompareLayInstSchPos implements Comparator<NodeInst> {
		Map<NodeInst, SchematicPosition> layInstToSchPos;
		public int compare(NodeInst ni1, NodeInst ni2) {
			SchematicPosition sp1, sp2;
			sp1 = layInstToSchPos.get(ni1);
			sp2 = layInstToSchPos.get(ni2);
			return sp1.compareTo(sp2);
		}
		public CompareLayInstSchPos(Map<NodeInst, SchematicPosition> layInstToSchPos) {
			this.layInstToSchPos = layInstToSchPos;
		}
	}
	// sort layout instances according to X-coordinate in schematic
	private List<NodeInst> getSortedLayInsts(SchematicVisitor visitor) {
        List<NodeInst> layInsts = 
        	new ArrayList<NodeInst>(visitor.getLayInsts());
        Map<NodeInst, SchematicPosition> layInstSchPos = 
        	visitor.getLayInstSchematicPositions();
        
        CompareLayInstSchPos compareLayInstSchPos = 
        	new CompareLayInstSchPos(layInstSchPos);
        
        Collections.sort(layInsts, compareLayInstSchPos);
        return layInsts;
	}
	
	private void stackLayInsts(List<NodeInst>layInsts, 
			                   SchematicVisitor visitor) {
        Map<NodeInst, Double> layInstSpacing = 
        	visitor.getLayInstSpacing();
        NodeInst prev = null;
        for (NodeInst me : layInsts) {
        	Double sP = layInstSpacing.get(me);
        	double sp = sP==null ? 0 : sP;
        	if (prev!=null)  LayoutLib.abutBottomTop(prev, 
        			                                 sp, 
        			                                 me);
        	prev = me;
        }
		//LayoutLib.abutBottomTop(layInsts, STAGE_SPACING);
	}
	
	// If pi can connect to metal-3 then return it. If pi
	// can connect to metal-2 then add and connect a via to 
	// metal-3 and return the via port. 
	private PortInst raiseToM3(PortInst pi) {
		if (connectsToM3(pi)) return pi;
		if (connectsToM2(pi)) {
			double x = pi.getBounds().getCenterX();
			double y = pi.getBounds().getCenterY();
			Cell parent = pi.getNodeInst().getParent();
			NodeInst via = 
				LayoutLib.newNodeInst(tech.m2m3(), x, y, 
					                  DEF_SIZE, DEF_SIZE, 0, parent);
			newM2SignalWire(pi, via.getOnlyPortInst());
			return via.getOnlyPortInst();
		}
		error(true, "scan port on other than m2 or m3?");
		return null;
	}
	
	private void connectScanPorts(NodeInst niOut, NodeInst niIn, 
			                      String outPortNm, String inPortNm) {
		for (int i=1; i<=BITS_PER_SCAN_CHAIN; i++) {
			PortInst piOut = niOut.findPortInst(outPortNm+"["+i+"]");
			PortInst piIn = niIn.findPortInst(inPortNm+"["+i+"]");
			LayoutLib.newArcInst(tech.m3(), SIGNAL_WID, 
					             raiseToM3(piOut), 
					             raiseToM3(piIn));
			//prln("Scan chain connect: "+piOut+" to "+piIn);
		}
	}
	// If an instance has a PortInst with a name that matches
	// an input or bidirectional scan chain then return the index
	// of that name. Otherwise return -1
	private int getScanInput(NodeInst ni, String[] chainNames) {
		String inPortNm = chainNames[IN]+"[1]";
		if (ni.findPortInst(inPortNm)!=null) return IN;
		inPortNm = chainNames[INOUT]+"[1]";
		if (ni.findPortInst(inPortNm)!=null) return INOUT;
		return -1;
	}
	
	// If an instance has a PortInst with a name that matches
	// an output or bidirectional scan chain then return the index
	// of that name. Otherwise return -1
	private int getScanOutput(NodeInst ni, String[] chainNames) {
		String inPortNm = chainNames[OUT]+"[1]";
		if (ni.findPortInst(inPortNm)!=null) return OUT;
		inPortNm = chainNames[INOUT]+"[1]";
		if (ni.findPortInst(inPortNm)!=null) return INOUT;
		return -1;
	}
	
	private void stitchScanChains(List<NodeInst> layInsts) {
		final int nbChains = SCAN_PORT_NAMES.length;
		
		for (int i=0; i<nbChains; i++) {
			String[] chainNames = SCAN_PORT_NAMES[i];
			NodeInst chainPrev = null;
			for (NodeInst ni : layInsts) {
				int inNdx = getScanInput(ni, chainNames);
				if (inNdx!=-1 && chainPrev!=null) {
					int outNdx = getScanOutput(chainPrev, chainNames);
					error(outNdx==-1, "prev has no scan output?");
					connectScanPorts(chainPrev, ni, 
					                 chainNames[outNdx], chainNames[inNdx]);
					chainPrev = null;
				}
				int outNdx = getScanOutput(ni, chainNames);
				if (outNdx!=-1)  chainPrev = ni;
			}
		}
	}
	
//	private boolean isPlain(NodeInst ni) {
//		return ni.getProto().getName().contains("aPlainStage");
//	}
//	
//	// infinityA and infinityB need to be stretched to the height of infinityC
//	private void stretchInfinityAB(List<NodeInst> layInsts) {
//		Cell parent = layInsts.get(0).getParent();
//		Cell dummyStage = STAGE_LIB.findNodeProto("aDummyStage{lay}");
//		if (!layInsts.get(0).getParent().getName().contains("infinityA") &&
//			!layInsts.get(0).getParent().getName().contains("infinityB")) 
//			return;
//		
//		NodeInst prev = null;
//		int stretchCnt = 0;
//		for (ListIterator<NodeInst> niIt=layInsts.listIterator(); niIt.hasNext();) {
//			NodeInst ni =  niIt.next();
//			if (stretchCnt>=NUM_PLACES_TO_STRETCH) return;
//			if (prev!=null && isPlain(prev) && isPlain(ni)) {
//				NodeInst dumInst = LayoutLib.newNodeInst(dummyStage, 0, 0, 
//						                                 DEF_SIZE, DEF_SIZE, 
//						                                 0, parent);
//				niIt.add(dumInst);
//				stretchCnt++;
//			}
//			prev = ni;
//		}
//	}
	// infinityC needs its 2nd and 3rd stages overlapped. Same with the 
	// two stages next to the last stage.
	private void overlapInfinityC(List<NodeInst> layInsts) {
		NodeInst dataFan = layInsts.get(1);
		if (!dataFan.getParent().getName().contains("infinityC"))
			return;

		double h = dataFan.findEssentialBounds().getHeight();
		for (int i=2; i<layInsts.size(); i++) {
			NodeInst ni = layInsts.get(i);
			ni.move(0, -h);
		}
		for (int i=layInsts.size()-2; i<layInsts.size(); i++) {
			NodeInst ni = layInsts.get(i);
			ni.move(0, -h);
		}
	}
	// infinityC needs stages stacked top to bottom. Furthermore,
	// all stages except for the first three and last three need to be
	// mirrored top <=> bottom 
	private void flipInfinityC(List<NodeInst> layInsts) {
		NodeInst dataFan = layInsts.get(1);
		if (!dataFan.getParent().getName().contains("infinityC"))
			return;
		Collections.reverse(layInsts);
		for (int i=3; i<layInsts.size()-3; i++) {
			NodeInst ni = layInsts.get(i);
			ni.modifyInstance(0, 0, 0, 0, Orientation.Y);
		}
	}
	
	// infinityC needs scan stitched bottom to top
	private void reverseScanListInfinityC(List<NodeInst> scanList) {
		NodeInst first = scanList.get(1);
		if (!first.getParent().getName().contains("infinityC"))
			return;
		Collections.reverse(scanList);
	}
	
	private NodeInst findInst(String type, List<NodeInst> insts) {
		for (NodeInst ni : insts) {
			String t = ni.getProto().getName();
			if (t.equals(type)) return ni;
		}
		return null;
	}
	
	private void doInfinity(Cell autoLay, Cell schCell) {
        Library autoLib = autoLay.getLibrary();
        List<Library> primLibs = new ArrayList<Library>();
        primLibs.add(autoLib);
        LayoutPrimitiveTester lpt = new LayoutPrimitiveTester(primLibs);
        SchematicVisitor visitor = new SchematicVisitor(autoLay, lpt);
        HierarchyEnumerator.enumerateCell(schCell, VarContext.globalContext, visitor);

        List<NodeInst> layInsts = visitor.getLayInsts();
        
		NodeInst niA = findInst("infinityA", layInsts);
		NodeInst niB = findInst("infinityB", layInsts);
		NodeInst niC = findInst("infinityC", layInsts);
		
		double colWid = niA.findEssentialBounds().getWidth();
		double numFillCellsAcross = Math.ceil(colWid/FILL_CELL_WIDTH);
		double columnPitch = FILL_CELL_WIDTH * numFillCellsAcross;
		double spaceBetweenCols = FILL_CELL_WIDTH + columnPitch - colWid;
		
		LayoutLib.alignCorners(niC, Corner.TL, niA, Corner.TR, -spaceBetweenCols, 0);
		LayoutLib.alignCorners(niC, Corner.BR, niB, Corner.BL, spaceBetweenCols, 0);
		List<ArcProto> horizLayers = new ArrayList<ArcProto>();
		horizLayers.add(tech.m2());
		horizLayers.add(tech.m4());
		AbutRouter.abutRouteLeftRight(niA, niC, 0, horizLayers);
		AbutRouter.abutRouteLeftRight(niC, niB, 0, horizLayers);

        Rectangle2D bounds = findColBounds(layInsts);
        addEssentialBounds(layInsts, bounds);
        ExportNamer vddNm = new ExportNamer("vdd");
        ExportNamer gndNm = new ExportNamer("gnd");
        exportPwrGnd(autoLay, vddNm, gndNm);

        List<ToConnect> toConns = visitor.getLayoutToConnects();
        reExport(toConns, bounds);
	}
	private void doGuts(Cell autoLay, Cell schCell) {
        Library autoLib = autoLay.getLibrary();
        List<Library> primLibs = new ArrayList<Library>();
        primLibs.add(autoLib);
        LayoutPrimitiveTester lpt = new LayoutPrimitiveTester(primLibs);
        SchematicVisitor visitor = new SchematicVisitor(autoLay, lpt);
        HierarchyEnumerator.enumerateCell(schCell, VarContext.globalContext, visitor);

        List<NodeInst> layInsts = visitor.getLayInsts();
        
		NodeInst niI = findInst("infinity", layInsts);
		NodeInst niC = findInst("crosser", layInsts);
		NodeInst niR = findInst("ring", layInsts);
		
		double colWid = niR.findEssentialBounds().getWidth();
		double numFillCellsAcross = Math.ceil(colWid/FILL_CELL_WIDTH);
		double columnPitch = FILL_CELL_WIDTH * numFillCellsAcross;
		double spaceBetweenCols = columnPitch - colWid;

		LayoutLib.alignCorners(niI, Corner.BR, niC, Corner.BL, spaceBetweenCols, (670-2));
		LayoutLib.alignCorners(niC, Corner.TL, niR, Corner.BL, 0, 0);
		
		List<ArcProto> horizLayers = new ArrayList<ArcProto>();
		horizLayers.add(tech.m2());
		horizLayers.add(tech.m4());

		AbutRouter.abutRouteLeftRight(niI, niC, 0, horizLayers);
		AbutRouter.abutRouteLeftRight(niI, niR, 0, horizLayers);
	}
	
	private void doEverything(Cell schCell) {
		prln("Test stage library");
		List<Library> primLibs = findPrimLayLibs();
		LayoutPrimitiveTester lpt = new LayoutPrimitiveTester(primLibs);
		checkStageLibrary();
		
//		prln("Fix scan fans");
//        fixScanFans();
        
        Library autoLib = schCell.getLibrary();
        String groupName = schCell.getCellName().getName();
		prln("Generate layout for Cell: "+groupName);

        Cell autoLay = Cell.newInstance(autoLib, groupName+"{lay}");
        autoLay.setTechnology(Technology.getCMOS90Technology());
        
		if (groupName.equals("infinity")) {
			doInfinity(autoLay, schCell);
		} else if (groupName.equals("gutsDrc")) {
			doGuts(autoLay, schCell);
		} else {
			doInfinityABC(autoLay, lpt, schCell);
		}
	}
	
//	private void getM3PwrGndX(Set<Double> m3PwrX, Set<Double> m3GndX, Cell plain) {
//		for (Iterator expIt=plain.getExports(); expIt.hasNext();) {
//			Export e = (Export) expIt.next();
//			if (connectsToM3(e)) {
//				double x = e.getOriginalPort().getCenter().getX();
//				if (isPwr(e)) {
//					m3PwrX.add(x);
//				} else if (isGnd(e)) {
//					m3GndX.add(x);
//				}
//			}
//		}
//		prln("Found "+m3PwrX.size()+" power X and "+m3GndX.size()+" ground X in Cell: "+plain.getName());
//	}
	
	
//	private void findPwrGndM2Exports(List<PortInst> pwrGnd,
//			                         Cell scanFan) {
//		for (Iterator eIt=scanFan.getExports(); eIt.hasNext();) {
//			Export e = (Export) eIt.next();
//			if (!connectsToM2(e)) continue;
//			PortInst o = e.getOriginalPort();
//			if (isPwr(e) || isGnd(e)) pwrGnd.add(o);
//		}
//		sortPortInstsBotToTop(pwrGnd);
//		error(pwrGnd.size()!=4, "expect 4 metal-2 pwr/gnd exports");
//	}
	
//	private void fixScanFan(Cell scanFan, 
//			                Set<Double> m3PwrX, Set<Double> m3GndX) {
//		Set<Double> existingM3PwrX = new TreeSet<Double>();
//		Set<Double> existingM3GndX = new TreeSet<Double>();
//		getM3PwrGndX(existingM3PwrX, existingM3GndX, scanFan);
//		
//		// find out what's missing
//		Set<Double> pwrX = new TreeSet<Double>(m3PwrX);
//		pwrX.removeAll(existingM3PwrX);
//		Set<Double> gndX = new TreeSet<Double>(m3GndX);
//		gndX.removeAll(existingM3GndX);
//		
//		List<PortInst> pwrGndExp = new ArrayList<PortInst>();
//		findPwrGndM2Exports(pwrGndExp, scanFan);
//		
//		double topY = scanFan.findEssentialBounds().getMaxY();
//		double botY = scanFan.findEssentialBounds().getMinY();
//		
//		Cell gndStrap = WIRE_LIB.findNodeProto("strapScanGND{lay}");
//		Cell vddStrap = WIRE_LIB.findNodeProto("strapScanVDD{lay}");
//		ExportNamer vddNm = new ExportNamer("vdd",10);
//		addScanFanM3Wire(scanFan, pwrX, pwrGndExp, topY, botY, 
//				         vddStrap, vddNm);
//		ExportNamer gndNm = new ExportNamer("gnd", 10);
//		addScanFanM3Wire(scanFan, gndX, pwrGndExp, topY, botY, 
//				         gndStrap, gndNm);
//	}
//	private List<PortInst> getPwrGndPorts(NodeInst strap) {
//		List<PortInst> ports = new ArrayList<PortInst>();
//		for (Iterator piIt=strap.getPortInsts(); piIt.hasNext();) {
//			ports.add((PortInst) piIt.next());
//		}
//		sortPortInstsBotToTop(ports);
//		return ports;
//	}
//	private void addScanFanM3Wire(Cell scanFan, Set<Double> pwrX, 
//			                      List<PortInst> pwrGndExp,
//			                      double topY, double botY,
//			                      Cell pwrStrap,
//			                      ExportNamer vddNm) {
//		List<PortInst> prev = pwrGndExp;
//		for (Double pX : pwrX) {
//			NodeInst strap = 
//				LayoutLib.newNodeInst(pwrStrap, pX, 0, DEF_SIZE, DEF_SIZE, 0, scanFan);
//			List<PortInst> strapPorts = getPwrGndPorts(strap);
//			
//			PortInst topPin = strapPorts.get(5);
//			PortInst botPin = strapPorts.get(0);
//			List<PortInst> cur = strapPorts.subList(1, 5);
//			
//			for (int i=0; i<4; i++) {
//				LayoutLib.newArcInst(tech.m2(), 9, 
//						             prev.get(i), cur.get(i));				
//			}
//			Export.newInstance(scanFan, topPin, vddNm.nextName());
//			Export.newInstance(scanFan, botPin, vddNm.nextName());
//			prev = cur;
//		}
//	}
	
//	private void fixScanFans() {
//		Cell plain = STAGE_LIB.findNodeProto("aPlainStage{lay}");
//
//		Set<Double> m3PwrX = new TreeSet<Double>();
//		Set<Double> m3GndX = new TreeSet<Double>();
//		getM3PwrGndX(m3PwrX, m3GndX, plain);
//
//		Cell scanFanLeft = FAN_LIB.findNodeProto("scanFanLeft{lay}");
//		fixScanFan(scanFanLeft, m3PwrX, m3GndX);
//
//		Cell scanFanRight = FAN_LIB.findNodeProto("scanFanRight{lay}");
//		fixScanFan(scanFanRight, m3PwrX, m3GndX);
//	}
	
//	private NodeInst findFirstStage(List<NodeInst> insts) {
//		for (NodeInst ni : insts) {
//			if (((Cell)ni.getProto()).getLibrary()==STAGE_LIB) return ni;
//		}
//		return null;
//	}
	
//	private NodeInst findLastStage(List<NodeInst> insts) {
//		for (int i=insts.size()-1; i>=0; i--) {
//			NodeInst ni = insts.get(i);
//			if (((Cell)ni.getProto()).getLibrary()==STAGE_LIB) return ni;
//		}
//		return null;
//	}

//	private void connectCovers(List<NodeInst> covers) {
//		List<ArcProto> vertLayer = new ArrayList<ArcProto>();
//		vertLayer.add(tech.m3());
//		NodeInst prev = null;
//		for (NodeInst ni : covers) {
//			if (prev!=null) {
//				AbutRouter.abutRouteBotTop(prev, ni, 0, vertLayer);
//			}
//			prev = ni;
//		}
//	}
	
//	private List<NodeInst> coverWithContacts(List<NodeInst> insts) {
//		Cell autoLay = insts.get(0).getParent();
//		Cell cover = STAGE_LIB.findNodeProto("stageCoverWeak{lay}");
//		double covHeight = cover.findEssentialBounds().getHeight();
//
//		NodeInst top = insts.get(insts.size()-1);
//		double topY = top.findEssentialBounds().getMaxY();
//		
//		NodeInst first = findFirstStage(insts);
//		double botFirstY = first.findEssentialBounds().getMinY();
//		
//		int numAbove = (int) Math.ceil((topY-botFirstY) / covHeight);
//		
//		double botY = insts.get(0).findEssentialBounds().getMinY();
//		double belowFirst = botFirstY - botY;
//		int numBelow = (int) Math.ceil(belowFirst/covHeight);
//		
//		double startY = botFirstY - numBelow*covHeight;  
//
//		double startCenter = startY - cover.findEssentialBounds().getMinY();
//
//		double oldY = cover.findEssentialBounds().getMinY();
//		double newY = -cover.findEssentialBounds().getMaxY();
//		double adjustY = oldY - newY;
//
//		List<NodeInst> covers = new ArrayList<NodeInst>();
//		
//		int num = numAbove + numBelow;
//		for (int i=0; i<num; i++) {
//			NodeInst c = LayoutLib.newNodeInst(cover, 0, startCenter+ i*covHeight, DEF_SIZE, DEF_SIZE, 0, autoLay);
//			if ((i % 2) == 1) {
//				c.modifyInstance(0, 0, 0, 0, Orientation.Y);
//				c.move(0, adjustY);
//			}
//			covers.add(c);
//		}
//		connectCovers(covers);
//		return covers;
//	}

	// Re-export all PortInsts of all NodeInsts that have the power or ground
	// characteristic that aren't connected to arcs.
	private void exportPwrGnd(Cell c, ExportNamer vdd, ExportNamer gnd) {
		for (Iterator<NodeInst> niIt=c.getNodes(); niIt.hasNext();) {
			NodeInst ni = niIt.next();
			if (!(ni.getProto() instanceof Cell)) continue;
			for (Iterator<PortInst> piIt=ni.getPortInsts(); piIt.hasNext();) {
				PortInst pi = piIt.next();
				if (!isPwrGnd(pi)) continue;
				if (pi.hasConnections()) continue;
				Export e;
				if (isPwr(pi)) {
					e = Export.newInstance(c, pi, vdd.nextName());
					e.setCharacteristic(PortCharacteristic.PWR);
				} else {
					e = Export.newInstance(c, pi, gnd.nextName());
					e.setCharacteristic(PortCharacteristic.GND);
				}
			}
		}
	}
	
	private void doInfinityABC(Cell autoLay, LayoutPrimitiveTester lpt, Cell schCell) {
        SchematicVisitor visitor = new SchematicVisitor(autoLay, lpt);
        HierarchyEnumerator.enumerateCell(schCell, VarContext.globalContext, visitor);
        
        List<NodeInst> layInsts = getSortedLayInsts(visitor);
        flipInfinityC(layInsts);
        //stretchInfinityAB(layInsts);
        stackLayInsts(layInsts, visitor);
        overlapInfinityC(layInsts);
        
        Rectangle2D colBounds = findColBounds(layInsts);
        addEssentialBounds(layInsts, colBounds);
        connectPwrGnd(layInsts);

        List<NodeInst> scanList = new ArrayList<NodeInst>(layInsts);
        reverseScanListInfinityC(scanList);
        stitchScanChains(scanList);
        
        //List<ToConnect> toConns = getLayToConnFromSch(schCell, autoLay);
        List<ToConnect> toConns = visitor.getLayoutToConnects();
        
		// debug
		//for (ToConnect cl : toConns)  prln("  N-Pin "+cl.toString());

        reExport(toConns, colBounds);
        
        //List<ToConnect> twoPins = reduceToTwoPin(toConns);
        List<ToConnect> twoOrThreePins = reduceToTwoOrThreePin(toConns);
        
        LayerChannels m2chan = new LayerChannels();
        LayerChannels m3chan = new LayerChannels();
        
        findChannels(m2chan, m3chan, layInsts, colBounds);
        
        route(twoOrThreePins, m2chan, m3chan);
        
//        List<NodeInst> covers = coverWithContacts(layInsts);
//        //exportVddGndCover(vddCnt, gndCnt, covers);
//        List<ArcProto> m3Layer = new ArrayList<ArcProto>();
//        m3Layer.add(tech.m3());
//		AbutRouter.abutRouteBotTop(covers.get(0), layInsts.get(0), 0, m3Layer);
		
        ExportNamer vddNmr = new ExportNamer("vdd");
        ExportNamer gndNmr = new ExportNamer("gnd");
		exportPwrGnd(autoLay, vddNmr, gndNmr);
		fattenPwrGnd(layInsts, colBounds);
        
        // Debug
        //dumpChannels(m2chan, m3chan);

        System.out.println("done.");
	}
	
	public Infinity(Cell schCell) {
		try {
			doEverything(schCell);
		} catch (Throwable th) {
			prln("Oh my! Something went wrong.");
		}
	}
}
