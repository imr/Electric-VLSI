package com.sun.electric.tool.generator.flag.router;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist.ShortResistors;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.tool.generator.flag.FlagConfig;
import com.sun.electric.tool.generator.flag.FlagDesign;
import com.sun.electric.tool.generator.flag.LayoutNetlist;
import com.sun.electric.tool.generator.flag.Utils;
import com.sun.electric.tool.generator.flag.scan.Scan;
import com.sun.electric.tool.generator.layout.AbutRouter;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.TechType;

public class Router {
	private class PortInfo {
		public final PortInst portInst;
		public final double x, y, maxY, minY;
		public final Channel m2Chan;
		public Segment m2Seg;
		public PortInfo(PortInst pi, LayerChannels m2Chans) {
			portInst = pi;
			x = pi.getCenter().getX();
			y = pi.getCenter().getY();
			maxY = y + config.pinHeight;
			minY = y - config.pinHeight;
			m2Chan = m2Chans.findChanOverVertInterval(x, minY, maxY);
			if (m2Chan==null) {
				prln("no m2 channel for PortInst: "+pi.toString());
				prln(m2Chans.toString());
			}
		}
		public void getM2OnlySeg(double xL, double xR) {
			if (connectsToM2(portInst)) {
				m2Seg = m2Chan.allocateBiggestFromTrack(xL-config.trackPitch, 
						                                x, xR+config.trackPitch, y);
				if (m2Seg==null) {
					prln("failed to get segment for m2-only PortInst: center="+y+
						 "["+(xL-config.trackPitch)+", "+(xR+config.trackPitch)+"]");				
					prln(m2Chan.toString());
				}
			}
		}
	}
    public static final double DEF_SIZE = LayoutLib.DEF_SIZE;

	private final FlagConfig config;
	private final Scan scan;

	private TechType tech() {return config.techTypeEnum.getTechType();}
	
	private void prln(String s) {Utils.prln(s);}
	private void pr(String s) {Utils.pr(s);}
	private void error(boolean cond, String msg) {Utils.error(cond, msg);}
	private void saveTaskDescription(String s) {Utils.saveTaskDescription(s);}
	private void clearTaskDescription() {Utils.clearTaskDescription();}

	
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

	private void blockInvisibleM3(Blockage1D m3block, double xLeft) {
		double pitch = config.m3PwrGndPitch;
		for (int i=0; i<36; i++) {
			double x = xLeft + pitch/2 + pitch*i;
			m3block.block(x-config.m3PwrGndWid/2, x+config.m3PwrGndWid/2);
		}
	}
	
	private void blockInvisibleM2(LayerChannels m2Chnls, Rectangle2D bounds) {
		double xCenter = bounds.getCenterX();
		double[] yBlocks = new double[] {-44, 228, 476, 716};
		for (double y : yBlocks) {
			Channel m2ch = m2Chnls.findChanOverVertInterval(xCenter, y, y);
			Segment s =  m2ch.allocate(m2ch.getMinTrackEnd()+6, m2ch.getMaxTrackEnd()-6, y, y);
			
			// debug
			prln("Blocking m2: "+s.toString());
		}
	}
	
	private void findChannels(LayerChannels m2Chnls, LayerChannels m3Chnls, 
			                  List<NodeInst> stages) {
		Rectangle2D colBounds = Utils.findBounds(stages.get(0).getParent());
		Blockage1D m2block = new Blockage1D();
		Blockage1D m3block = new Blockage1D();
		for (NodeInst ni : stages) {
			for (Iterator piIt=ni.getPortInsts(); piIt.hasNext();) {
				PortInst pi = (PortInst) piIt.next();
				double x = pi.getCenter().getX();
				double y = pi.getCenter().getY();
				if (Utils.isPwrGnd(pi) || scan.isScan(pi)) {
					if (connectsToM2(pi)) {
						// horizontal m2 channel
						m2block.block(y-config.m2PwrGndWid/2, y+config.m2PwrGndWid/2);
					} else if (connectsToM3(pi)) {
						// vertical m3 channel
						m3block.block(x-config.m3PwrGndWid/2, x+config.m3PwrGndWid/2);
					} else {
						error(true, "unexpected metal for port: "+pi.toString());
					}
				}
			}
		}
		blockInvisibleM3(m3block, colBounds.getMinX());
		
		Interval prv = null;
		for (Interval i : m2block.getBlockages()) {
	//		 debug
	//		prln("Interval: "+i.toString());
			if (prv!=null) {
				m2Chnls.add(new Channel(true, colBounds.getMinX(),
						                colBounds.getMaxX(),
					                    prv.getMax(), i.getMin(), "metal-2"));
			}
			prv = i;
		}

		blockInvisibleM2(m2Chnls, colBounds);
		
		prv = null;
		for (Interval i : m3block.getBlockages()) {
			double prvMax = prv==null ? colBounds.getMinX() : prv.getMax();
			m3Chnls.add(new Channel(false, colBounds.getMinY(),    
					                colBounds.getMaxY(),
				                    prvMax, i.getMin(), "metal-3"));
			prv = i;
		}
		// add channel between last blockage and cell boundary
		Interval last = prv;
		m3Chnls.add(new Channel(false, colBounds.getMinY(),    
                colBounds.getMaxY(),
                last.getMax(), colBounds.getMaxX(), "metal-3"));
		
		// print status
		prln("Found: "+m2Chnls.numChannels()+" metal-2 channels");
		prln("Found: "+m3Chnls.numChannels()+" metal-3 channels");
	}
	private void routeTwoOrThreePinNet(ToConnect toConn, LayerChannels m2Chan,
            LayerChannels m3Chan) {
		if (toConn.numPortInsts()==2) routeTwoPinNet(toConn, m2Chan, m3Chan);
		if (toConn.numPortInsts()==3) routeThreePinNet(toConn, m2Chan, m3Chan);
	}
	// Single vertical m3 connects to all PortInsts in m2
	private void routeThreePinNet(ToConnect toConn, LayerChannels m2Chan,
	                              LayerChannels m3Chan) {
		saveTaskDescription("Connecting three pins: "+toConn);

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
		clearTaskDescription();
	}
	
	
	private void routeTwoPinNet(ToConnect toConn, LayerChannels m2Chan,
	                            LayerChannels m3Chan) {
		List<PortInst> pis = toConn.getPortInsts();
		
		saveTaskDescription("Connecting two pins: "+toConn);

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
					double minX = Math.min(c3.getMinTrackCenter(), infoL.x);
					double maxX = Math.max(c3.getMaxTrackCenter(), infoR.x);
					inf.m2Seg = inf.m2Chan.allocate(minX, maxX, infoL.y, infoR.y);
					
					if (inf.m2Seg==null) return;

//					// debug
//					if (inf.m2Seg.getTrackCenter()==1102 && Math.abs(inf.m2Seg.min-3585)<100) {
//						prln("Allocating segment: "+inf.m2Seg);
//						prln(m2Chan.toString());
//					}
					
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
			infoL.m2Seg.trim(infoL.x-config.trackPitch, s3.getTrackCenter()+config.trackPitch);
			infoR.m2Seg.trim(s3.getTrackCenter()-config.trackPitch, infoR.x+config.trackPitch);
		}

		// do the actual routing right now. In a two level
		// scheme we would actually postpone this
		routeUseM3(infoL.portInst, infoR.portInst, infoL.m2Seg, infoR.m2Seg, s3);
		clearTaskDescription();
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
				LayoutLib.newNodeInst(tech().m1m2(), 
					              	  pL.getCenter().getX(),
					                  m2L.getTrackCenter(),
					                  DEF_SIZE, DEF_SIZE, 0, parent);
			// left vertical m1
			LayoutLib.newArcInst(tech().m1(), config.signalWid, 
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
				LayoutLib.newNodeInst(tech().m1m2(), 
					              	  pR.getCenter().getX(),
					                  m2R.getTrackCenter(),
					                  DEF_SIZE, DEF_SIZE, 0, parent);
			
			LayoutLib.newArcInst(tech().m1(), config.signalWid, 
        			             m1m2b.getOnlyPortInst(),
        			             pR);

			m2PortB = m1m2b.getOnlyPortInst();
		} else {
			m2PortB = pR;
		}

		if (m3!=null) {
			NodeInst m2m3a =
				LayoutLib.newNodeInst(tech().m2m3(),
						              m3.getTrackCenter(),
						              m2L.getTrackCenter(),
						              DEF_SIZE, DEF_SIZE, 0, parent);
			// left horizontal m2
			newM2SignalWire(m2PortA, m2m3a.getOnlyPortInst());
			NodeInst m2m3b =
				LayoutLib.newNodeInst(tech().m2m3(),
						              m3.getTrackCenter(),
						              m2R.getTrackCenter(),
						              DEF_SIZE, DEF_SIZE, 0, parent);
			// vertical m3
			LayoutLib.newArcInst(tech().m3(), config.signalWid, 
					             m2m3a.getOnlyPortInst(),
					             m2m3b.getOnlyPortInst());
			
			// right horizontal m2
			newM2SignalWire(m2m3b.getOnlyPortInst(), m2PortB);
		} else {
			newM2SignalWire(m2PortA, m2PortB);
		}
	}
	
	public void newM2SignalWire(PortInst p1, PortInst p2) {
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
		double extend = config.minM2Len - (len + config.signalWid);
		if (extend > 0) {
			// round extention to tenths of a lambda
			double halfExt = Math.ceil(10*extend/2)/10;
			//prln("halfExt: "+halfExt);
			// add additional m2 to this wire
			NodeInst pin1 = LayoutLib.newNodeInst(tech().m2pin(), xL-halfExt, y, 
					                              DEF_SIZE, DEF_SIZE, 0, 
					                              parent);
			NodeInst pin2 = LayoutLib.newNodeInst(tech().m2pin(), xR+halfExt, y, 
								                  DEF_SIZE, DEF_SIZE, 0, 
								                  parent);
			LayoutLib.newArcInst(tech().m2(), config.signalWid, 
					             pin1.getOnlyPortInst(), pL);
			LayoutLib.newArcInst(tech().m2(), config.signalWid, 
		                         pR, pin2.getOnlyPortInst());
		}
		LayoutLib.newArcInst(tech().m2(), config.signalWid, pL, pR);
	}
	
	private boolean connectsToM1(PortProto pp) {
		return pp.connectsTo(tech().m1());
	}
	private boolean connectsToM1(PortInst pi) {
		return connectsToM1(pi.getPortProto());
	}
	private boolean connectsToM2(PortProto pp) {
		return pp.connectsTo(tech().m2());
	}
	public boolean connectsToM2(PortInst pi) {
		return connectsToM2(pi.getPortProto());
	}
	private boolean connectsToM3(PortProto pp) {
		return pp.connectsTo(tech().m3());
	}
	public boolean connectsToM3(PortInst pi) {
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
	
	public void connectPwrGnd(List<NodeInst> nodeInsts) {
		List<ArcProto> vertLayers = new ArrayList<ArcProto>();
		vertLayers.add(tech().m3());
		NodeInst prev = null;
		for (NodeInst ni : nodeInsts) {
			if (prev!=null) {
				AbutRouter.abutRouteBotTop(prev, ni, 0, vertLayers);
			}
			prev = ni;
		}
	}
	
	public Router(FlagConfig config, Scan scan) {
		this.config = config; this.scan = scan;
	}
	public PortInst raiseToM3(PortInst pi) {
		if (connectsToM3(pi)) return pi;
		if (connectsToM2(pi)) {
			double x = pi.getBounds().getCenterX();
			double y = pi.getBounds().getCenterY();
			Cell parent = pi.getNodeInst().getParent();
			NodeInst via = 
				LayoutLib.newNodeInst(tech().m2m3(), x, y, 
					                  FlagDesign.DEF_SIZE, FlagDesign.DEF_SIZE, 0, parent);
			newM2SignalWire(pi, via.getOnlyPortInst());
			return via.getOnlyPortInst();
		}
		Utils.error(true, "scan port on other than m2 or m3?");
		return null;
	}
	/** Find which PortInsts are already connected. Stick connected PortInsts 
	 * into a list. Return a list of such lists. */
	private List<List<PortInst>> groupConnectedPorts(ToConnect tc) {
		PortInst firstPi = tc.getPortInsts().get(0);
		Cell parent = firstPi.getNodeInst().getParent();
		Netlist nl = parent.getNetlist(ShortResistors.PARASITIC);
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
			if (tc.numPortInsts()==0) continue;
			
			// Some PortInsts on a ToConnect may already be connected in 
			// schematic by abut router or by scan chain stitcher
			List<List<PortInst>> connPorts = groupConnectedPorts(tc);
			
			if (connPorts.size()==2)  connPorts = makeTwoClusterSimple(connPorts);
			
			// make sure this routing problem is simple
			if (!isSimple(connPorts)) continue;
			
			if (connPorts.size()==2 || connPorts.size()==3) {
				ToConnect tcX = new ToConnect();
				for (List<PortInst> ports : connPorts) {
					error(ports.size()!=1, "We only allow one port per cluster");
					tcX.addPortInst(ports.get(0));
				}
				twoOrThreePin.add(tcX);
			}
		}
		return twoOrThreePin;
	}
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

	
	private void getM3PwrGndExports(Map<Double, PortInst> pwr, Map<Double, PortInst> gnd, 
			                        NodeInst ni, double y) {
		for (Iterator piIt=ni.getPortInsts(); piIt.hasNext();) {
			PortInst pi = (PortInst) piIt.next();
			if (pi.getCenter().getY()!=y) continue;
			if (!connectsToM3(pi)) continue;
			double x  = pi.getCenter().getX();
			if (Utils.isPwr(pi)) pwr.put(x, pi);
			else if (Utils.isGnd(pi)) gnd.put(x, pi);
		}
	}
	
	private void route(List<ToConnect> toConns, LayerChannels m2Chan,	 
			           LayerChannels m3Chan) {
//		 Connect m3 pins using vertical m3
//		for (ToConnect toConn : toConns) {
//			if (hasM3Pin(toConn))  connect2PinM3(toConn);
//		}

//		 We must route nets with m2 pins first because m2 pins allow us no
//		 choice of m2 track.
		for (ToConnect toConn : toConns) {
			if (hasM2Pin(toConn))  routeTwoOrThreePinNet(toConn, m2Chan, m3Chan);
		}

		for (ToConnect toConn : toConns) {
			if (!hasM2Pin(toConn) && !hasM3Pin(toConn))  
				routeTwoOrThreePinNet(toConn, m2Chan, m3Chan);
		}

	}

	public void routeSignals(List<ToConnect> toConns, LayoutNetlist layNets) {
		List<NodeInst> layInsts = layNets.getLayoutInstancesSortedBySchematicPosition();
		if (layInsts.size()==0) return;
        List<ToConnect> twoOrThreePins = reduceToTwoOrThreePin(toConns);
        
        LayerChannels m2chan = new LayerChannels();
        LayerChannels m3chan = new LayerChannels();
        
        findChannels(m2chan, m3chan, layInsts);
        
//        //	debug
//        prln("Metal 2 channels");
//        prln(m2chan.toString());
//        
//        prln("Metal 3 channels");
//        prln(m3chan.toString());
        
        route(twoOrThreePins, m2chan, m3chan);
	}
	

	


	
}
