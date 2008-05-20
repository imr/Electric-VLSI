package com.sun.electric.tool.generator.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;

/** For two Cell instances that abut, connect PortInsts that coincide or
 * nearly coincide. */ 
public class AbutRouter {
	// List of layers, from lowest to highest, that we may
	// use for routing.
	private final List<ArcProto> layers;
	private boolean horizBoundary;
	
	private static void prln(String msg) {System.out.println(msg);}
	
	private AbutRouter(List<ArcProto> layers) {this.layers=layers;}
	
	private double coordParallelToBoundary(PortInst pi) {
		return horizBoundary ? pi.getCenter().getX() :
			                   pi.getCenter().getY();
	}
	private double coordPerpedicularToBoundary(PortInst pi) {
		return horizBoundary ? pi.getCenter().getY() :
			                   pi.getCenter().getX();
	}
	
	private void sortByCoordParallelToBoundary(List<PortInst> ports) {
		Collections.sort(ports, new Comparator<PortInst>() {
			public int compare(PortInst pi1, PortInst pi2) {
				double delta = coordParallelToBoundary(pi1) -
				               coordParallelToBoundary(pi2);
				return (int) Math.signum(delta);
			}
		}
		);
	}

	private List<PortInst> findPortsNearBoundary(NodeInst ni, double boundaryXY,
				                                 double distFromBoundary) {
		List<PortInst> boundaryPorts = new ArrayList<PortInst>();
		for (Iterator<PortInst> piIt=ni.getPortInsts(); piIt.hasNext();) {
			PortInst pi = piIt.next();
			double xy = coordPerpedicularToBoundary(pi);
			if (Math.abs(xy-boundaryXY)<=distFromBoundary) {
				boundaryPorts.add(pi);
			}
		}
		sortByCoordParallelToBoundary(boundaryPorts);
		return boundaryPorts;
	}

	private List<PortInst> getAndRemovePortsAtCoord(double xy, List<PortInst> ports) {
		List<PortInst> portsAtXY = new ArrayList<PortInst>();
		while (ports.size()!=0 && coordParallelToBoundary(ports.get(0))==xy) {
			portsAtXY.add(ports.get(0));
			ports.remove(0);
		}
		return portsAtXY;
	}
	
	private boolean powerToGroundShort(PortInst pi1, PortInst pi2) {
		PortCharacteristic ch1 = pi1.getPortProto().getCharacteristic();
		PortCharacteristic ch2 = pi2.getPortProto().getCharacteristic();
		return (ch1==PortCharacteristic.PWR && ch2==PortCharacteristic.GND) ||
		       (ch1==PortCharacteristic.GND && ch2==PortCharacteristic.PWR);
	}
	// Return the highest layers that pi can connect to.
	// Return null if pi can't connect to any of the permitted layers.
	private ArcProto getHighestLayer(PortInst pi) {
		PortProto pp = pi.getPortProto();
		for (int i=layers.size()-1; i>=0; i--) {
			ArcProto ap = layers.get(i);
			if (pp.connectsTo(ap)) return ap; 
		}
		return null;
	}

	private void connectAlignedPorts(List<PortInst> loPorts, 
			                         List<PortInst> hiPorts) {
		// N^2 search because we might have more than one layer
		for (PortInst piB : loPorts) {
			ArcProto arcB = getHighestLayer(piB);
			if (arcB==null) continue;
			
			for (PortInst piT : hiPorts) {
				if (piT.getPortProto().connectsTo(arcB)) {
					if (powerToGroundShort(piB, piT)) {
						prln("Power and Ground ports overlap: "+piB+" "+piT);
						continue;
					}
					// make a connection
					double w = LayoutLib.widestWireWidth(piB);
					// debug
					//prln("Connecting ports: "+piB+" and "+piT+" using "+arcB+" width "+w);
					LayoutLib.newArcInst(arcB, w, piB, piT);
					// only allow each bot port to connect to at most one top port 
					break;
				}
			}
		}
	}

	private void abutRouteBotTop(NodeInst bot, NodeInst top, 
            					 double distFromBoundary) {
		horizBoundary = true;
		double botMaxY = bot.findEssentialBounds().getMaxY();
		double topMinY = top.findEssentialBounds().getMinY();
		abutRoute(bot, top, botMaxY, topMinY, distFromBoundary);
	}
	
	private void abutRoute(NodeInst niLo, NodeInst niHi, 
			               double xyLo, double xyHi, double distFromBoundary) {
		List<PortInst> portsLo = findPortsNearBoundary(niLo, xyLo, 
		                                   			   distFromBoundary);
		List<PortInst> portsHi = findPortsNearBoundary(niHi, xyHi, 
		                                               distFromBoundary);
		
		while (portsLo.size()!=0 && portsHi.size()!=0) {
			PortInst firstLo = portsLo.get(0);
			double loXY = coordParallelToBoundary(firstLo);
			PortInst firstHi = portsHi.get(0);
			double hiXY = coordParallelToBoundary(firstHi);
			if (loXY<hiXY) {
				portsLo.remove(0);
			} else if (loXY>hiXY) {
				portsHi.remove(0);
			} else {
				// loXY == hiXY
				List<PortInst> alignedBotPorts = getAndRemovePortsAtCoord(loXY, portsLo);
				List<PortInst> alignedTopPorts = getAndRemovePortsAtCoord(loXY, portsHi);
				connectAlignedPorts(alignedBotPorts, alignedTopPorts);
			}
		}
	}
	private void abutRouteLeftRight(NodeInst left, NodeInst right,
			                        double distFromBoundary) {
		horizBoundary = false;
		double leftMaxX = left.findEssentialBounds().getMaxX();
		double rightMinX = right.findEssentialBounds().getMinX();
		abutRoute(left, right, leftMaxX, rightMinX, distFromBoundary);
	}
	/** Connect ports on the top edge of bot that line up exactly
	 * with corresponding ports on the bottom edge of top. Only connect
	 * those ports that attach to metals in layers. 
	 * layers must be sorted from lowest to highest */
	public static void abutRouteBotTop(NodeInst bot, NodeInst top, 
                                       double distFromBoundary,
                                       List<ArcProto> layers) {
		AbutRouter ar = new AbutRouter(layers);
		ar.abutRouteBotTop(bot, top, distFromBoundary);
	}
	/** Connect ports on the right edge of left that line up exactly
	 * with corresponding ports on the left edge of right. Only connect
	 * those ports that attach to metals in layers. 
	 * layers must be sorted from lowest to highest */
	public static void abutRouteLeftRight(NodeInst left, NodeInst right, 
                                          double distFromBoundary,
                                          List<ArcProto> layers) {
		AbutRouter ar = new AbutRouter(layers);
		ar.abutRouteLeftRight(left, right, distFromBoundary);
	}

}
