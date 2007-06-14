package com.sun.electric.tool.generator.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;

/** For two Cell instances that abut, connect PortInsts that coincide or
 * nearly coincide. */ 
public class AbutRouter {
	private final TechType tech;
	
	private static void prln(String msg) {System.out.println(msg);}
	
	private AbutRouter(TechType tech) {this.tech=tech;}
	
	private void sortByX(List<PortInst> ports) {
		Collections.sort(ports, new Comparator<PortInst>() {
			public int compare(PortInst pi1, PortInst pi2) {
				double delta = pi1.getCenter().getX() -
				               pi2.getCenter().getX();
				if (delta>0) return 1;
				if (delta==0) return 0;
				if (delta<0) return -1;
				LayoutLib.error(true, "some ports have unordered Y coordinates");
				return 0;
			}
		}
		);
	}

	private List<PortInst> findPortsNearBoundary(NodeInst ni, double boundaryY,
				                                 double distFromBoundary) {
		List<PortInst> boundaryPorts = new ArrayList<PortInst>();
		for (Iterator piIt=ni.getPortInsts(); piIt.hasNext();) {
			PortInst pi = (PortInst) piIt.next();
			double y = pi.getCenter().getY();
			if (Math.abs(y-boundaryY)<=distFromBoundary) {
				boundaryPorts.add(pi);
			}
		}
		sortByX(boundaryPorts);
		return boundaryPorts;
	}

	private List<PortInst> getAndRemovePortsAtX(double x, List<PortInst> ports) {
		List<PortInst> portsAtX = new ArrayList<PortInst>();
		while (ports.size()!=0 && ports.get(0).getCenter().getX()==x) {
			portsAtX.add(ports.get(0));
			ports.remove(0);
		}
		return portsAtX;
	}
	
	private boolean powerToGroundShort(PortInst pi1, PortInst pi2) {
		PortCharacteristic ch1 = pi1.getPortProto().getCharacteristic();
		PortCharacteristic ch2 = pi2.getPortProto().getCharacteristic();
		return (ch1==PortCharacteristic.PWR && ch2==PortCharacteristic.GND) ||
		       (ch1==PortCharacteristic.GND && ch2==PortCharacteristic.PWR);
	}

	private void connectAlignedPorts(List<PortInst> botPorts, 
			                         List<PortInst> topPorts) {
		// N^2 search because we might have more than one layer
		for (PortInst piB : botPorts) {
			ArcProto arcB = tech.highestLayer(piB.getPortProto());
			
			for (PortInst piT : topPorts) {
				if (piT.getPortProto().connectsTo(arcB)) {
					if (powerToGroundShort(piB, piT)) {
						prln("Power and Ground ports overlap: "+piB+" "+piT);
						continue;
					}
					// make a connection
					double w = LayoutLib.widestWireWidth(piB);
					prln("Connecting ports: "+piB+" and "+piT+" using "+arcB+" width "+w);
					LayoutLib.newArcInst(arcB, w, piB, piT);
					// only allow each bot port to connect to at most one top port 
					break;
				}
			}
		}
	}

	private void abutRouteBotTop(NodeInst bot, NodeInst top, 
            					 double distFromBoundary) {
		double botMaxY = bot.findEssentialBounds().getMaxY();
		double topMinY = top.findEssentialBounds().getMinY();
		List<PortInst> botPorts = findPortsNearBoundary(bot, botMaxY, 
		                                   			    distFromBoundary);
		List<PortInst> topPorts = findPortsNearBoundary(top, topMinY, 
		                                                distFromBoundary);
		
		while (botPorts.size()!=0 && topPorts.size()!=0) {
			PortInst bFirst = botPorts.get(0);
			double bX = bFirst.getCenter().getX();
			PortInst tFirst = topPorts.get(0);
			double tX = tFirst.getCenter().getX();
			if (bX<tX) {
				botPorts.remove(0);
			} else if (bX>tX) {
				topPorts.remove(0);
			} else {
				// bX == tX
				List<PortInst> alignedBotPorts = getAndRemovePortsAtX(bX, botPorts);
				List<PortInst> alignedTopPorts = getAndRemovePortsAtX(bX, topPorts);
				connectAlignedPorts(alignedBotPorts, alignedTopPorts);
			}
		}
	}

	public static void abutRouteBotTop(NodeInst bot, NodeInst top, 
                                       double distFromBoundary,
                                       TechType tech) {
		AbutRouter ar = new AbutRouter(tech);
		ar.abutRouteBotTop(bot, top, distFromBoundary);
	}

}
