/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SchemToLay.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.generator.layout;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.generator.layout.gates.Inv;
import com.sun.electric.tool.generator.layout.gates.Inv2i;
import com.sun.electric.tool.generator.layout.gates.Inv2iKn;
import com.sun.electric.tool.generator.layout.gates.Inv2iKp;
import com.sun.electric.tool.generator.layout.gates.InvCTLn;
import com.sun.electric.tool.generator.layout.gates.InvHT;
import com.sun.electric.tool.generator.layout.gates.InvLT;
import com.sun.electric.tool.generator.layout.gates.Inv_passgate;
import com.sun.electric.tool.generator.layout.gates.Nand2;
import com.sun.electric.tool.generator.layout.gates.Nand2HLT;
import com.sun.electric.tool.generator.layout.gates.Nand2HLT_sy;
import com.sun.electric.tool.generator.layout.gates.Nand2LT;
import com.sun.electric.tool.generator.layout.gates.Nand2LT_sy;
import com.sun.electric.tool.generator.layout.gates.Nand2PH;
import com.sun.electric.tool.generator.layout.gates.Nand2PHfk;
import com.sun.electric.tool.generator.layout.gates.Nand2_sy;
import com.sun.electric.tool.generator.layout.gates.Nand2en;
import com.sun.electric.tool.generator.layout.gates.Nand2en_sy;
import com.sun.electric.tool.generator.layout.gates.Nand3;
import com.sun.electric.tool.generator.layout.gates.Nand3LT;
import com.sun.electric.tool.generator.layout.gates.Nand3LT_sy3;
import com.sun.electric.tool.generator.layout.gates.Nand3MLT;
import com.sun.electric.tool.generator.layout.gates.Nand3_sy3;
import com.sun.electric.tool.generator.layout.gates.Nand3en_sy;
import com.sun.electric.tool.generator.layout.gates.Nand3en_sy3;
import com.sun.electric.tool.generator.layout.gates.Nms1;
import com.sun.electric.tool.generator.layout.gates.Nms2;
import com.sun.electric.tool.generator.layout.gates.Nms2_sy;
import com.sun.electric.tool.generator.layout.gates.Nms3_sy3;
import com.sun.electric.tool.generator.layout.gates.Nor2;
import com.sun.electric.tool.generator.layout.gates.Nor2LT;
import com.sun.electric.tool.generator.layout.gates.Nor2kresetV;
import com.sun.electric.tool.generator.layout.gates.Pms1;
import com.sun.electric.tool.generator.layout.gates.Pms2;
import com.sun.electric.tool.generator.layout.gates.Pms2_sy;
import com.sun.electric.tool.generator.layout.gates.TieHi;
import com.sun.electric.tool.generator.layout.gates.VertTrack;

public class SchemToLay {
    // ------------------------ private types ---------------------------------
	
	// A RouteSeg is a list of layout ports that should be connected
	// and a list of exports on that net.  Networks that connect to
	// both NMOS stack and PMOS stack devices need to be divided into
	// two RouteSeg's, one in the lower half of the cell and one in
	// the upper half of the cell.  Otherwise there is a one to one
	// relationship between a Network and a RouteSeg
	private static class RouteSeg {
		private static final Iterator<Export> NO_EXPORTS = new ArrayList<Export>().iterator();
		
		private ArrayList<PortInst> pStkPorts = new ArrayList<PortInst>();
		private ArrayList<PortInst> nStkPorts = new ArrayList<PortInst>();
		private ArrayList<PortInst> nonStkPorts = new ArrayList<PortInst>();
		private Iterator<Export> exports = NO_EXPORTS;
		private Integer exportTrack = null;
		
		// Has this RouteSeg been assigned a track for its exports?
		// WARNING!! Export tracks are PHYSICAL!!!!! They include blocked
		// tracks.  Everywhere else in this program we talk about
		// available tracks which exclude blocked tracks.
		private boolean hasExpTrk() {return exportTrack!=null;}
		private int getExpTrk() {return exportTrack.intValue();}
		private boolean hasPmosExpTrk() {return hasExpTrk() && getExpTrk()>0;}
		private boolean hasNmosExpTrk() {return hasExpTrk() && getExpTrk()<0;}
		
		private static ArrayList<PortInst> schemNetToLayPorts(Network net,
													HashMap<NodeInst,NodeInst> iconToLay) {
			ArrayList<PortInst> layPorts = new ArrayList<PortInst>();
			
			Iterator<PortInst> iPorts = SKIP_WIRE_PORTINSTS.filter(net.getPorts());
			while (iPorts.hasNext()) {
				PortInst iconPort = iPorts.next();
				NodeInst layInst =
					iconToLay.get(iconPort.getNodeInst());
				error(layInst==null,
					  "SchemToLay: no layout instance for Icon? "+
					  iconPort.getNodeInst());
				String portNm = iconPort.getPortProto().getName();
				PortInst layPort = layInst.findPortInst(portNm);
				error(layPort==null, "Port: "+portNm+" missing from Part: "+
					  layInst.getProto().getName());
				layPorts.add(layPort);
			}
			return layPorts;
		}
		private static Integer findExportTrack(Network net,
											   HashMap<String,Object> expTrkAsgn) {
			Integer expTrk = null;
			for (Iterator<Export> it=net.getExports(); it.hasNext();) {
				String expNm = it.next().getName();
				if (expTrkAsgn.containsKey(expNm)) {
					error(expTrk!=null,
						  "more than one track assigned to segment!");
					expTrk = (Integer)expTrkAsgn.get(expNm);
				}
			}
			return expTrk;
		}
		private boolean hasNstk() {
			return nStkPorts.size()!=0 || hasNmosExpTrk();
		}
		private boolean hasPstk() {
			return pStkPorts.size()!=0 || hasPmosExpTrk();
		}
		private Iterator<Export> removeExports() {
			exportTrack = null;
			Iterator<Export> oldExports = exports;
			exports = NO_EXPORTS;
			return oldExports;
		}
		private RouteSeg(ArrayList<PortInst> layPorts, Iterator<Export> exports, Integer expTrk) {
			this.exports = exports;
			this.exportTrack = expTrk;
			for (int i=0; i<layPorts.size(); i++) {
				PortInst p = layPorts.get(i);
				if (isNstk(p.getNodeInst())) nStkPorts.add(p);
				else if (isPstk(p.getNodeInst())) pStkPorts.add(p);
				else nonStkPorts.add(p);
			}
		}
		public RouteSeg(Network net, HashMap<NodeInst,NodeInst> iconToLay, HashMap<String,Object> expTrkAsgn) {
			this(schemNetToLayPorts(net, iconToLay), net.getExports(),
				 findExportTrack(net, expTrkAsgn));
		}
		public boolean hasExports() {return exports.hasNext();}
		public Iterator<Export> findExports() {return exports;}
		public boolean hasNonStk() {return nonStkPorts.size()!=0;}
		
		// A RouteSeg containing both NMOS and PMOS stacks must be divided into
		// two RouteSegs
		public RouteSeg splitOffPstkRouteSeg(ArrayList<NodeInst> vertTracks,
											 StdCellParams stdCell,
											 Cell gasp) {
			error(!hasPstk() || !hasNstk(), "can't split off anything");
			if (!hasNonStk()) {
				// There is no non-stack part on this segment.  We need to add
				// a part containing a vertical routing track so we can
				// connect the NMOS and PMOS stacks.
				NodeProto vtProt = VertTrack.makePart(stdCell);
				NodeInst vtInst = 
				    LayoutLib.newNodeInst(vtProt, 0, 0, 0, 0, 0, gasp);
				vertTracks.add(vtInst);
				nonStkPorts.add(vtInst.findPortInst("in"));
			}
			// For now, old segment contains nStk and nonStk, new segment
			// contains pStk and one nonStk to bind the two segments
			// together.  Optimize this later.
			ArrayList<PortInst> pPorts = pStkPorts;
			pStkPorts = new ArrayList<PortInst>();
			pPorts.add(nonStkPorts.get(0));
			
			Integer pExpTrk = hasPmosExpTrk() ? new Integer(getExpTrk()) : null;
			Iterator<Export> pExports = hasPmosExpTrk() ? removeExports() : NO_EXPORTS;
			
			return new RouteSeg(pPorts, pExports, pExpTrk);
		}
		public ArrayList<PortInst> getAllPorts() {
			ArrayList<PortInst> ports = new ArrayList<PortInst>(pStkPorts);
			ports.addAll(nStkPorts);
			ports.addAll(nonStkPorts);
			return ports;
		}
		public double estimateLength() {
			double minX = Double.POSITIVE_INFINITY;
			double maxX = Double.NEGATIVE_INFINITY;
			ArrayList<PortInst> ports = getAllPorts();
			for (int i=0; i<ports.size(); i++) {
				double x = LayoutLib.roundCenterX(ports.get(i));
				minX = Math.min(minX, x);
				maxX = Math.max(maxX, x);
			}
			double weight = hasExports() ? 1/*.5*/ : 1;
			return weight*(maxX-minX);
		}
		public double getLoX() {
			double minX = Double.POSITIVE_INFINITY;
			ArrayList<PortInst> ports = getAllPorts();
			for (int i=0; i<ports.size(); i++) {
				double x = LayoutLib.roundCenterX(ports.get(i));
				minX = Math.min(minX, x);
			}
			return minX;
		}
		public double getHiX() {
			double maxX = Double.NEGATIVE_INFINITY;
			ArrayList<PortInst> ports = getAllPorts();
			for (int i=0; i<ports.size(); i++) {
				double x = LayoutLib.roundCenterX(ports.get(i));
				maxX = Math.max(maxX, x);
			}
			return maxX;
		}
	}
	
	private static class TrackAllocator {
		private static final double FULL_TRACK_XLO = -Integer.MAX_VALUE;
		private static final double FULL_TRACK_WID = 2.0 * Integer.MAX_VALUE;
		private static final boolean PMOS = true;
		private static final boolean NMOS = false;
		private final int nbNmosTracks, nbPmosTracks;
		private StdCellParams stdCell;
		private ArrayList<Rectangle2D> blockages = new ArrayList<Rectangle2D>();
		public TrackAllocator(StdCellParams stdCell) {
			this.stdCell = stdCell;
			nbNmosTracks = stdCell.nbNmosTracks();
			nbPmosTracks = stdCell.nbPmosTracks();
		}
		private boolean isBlocked(double x, double y, double w, double h) {
			for (int i=0; i<blockages.size(); i++) {
				Rectangle2D block = blockages.get(i);
				if (block.intersects(x, y, w, h)) return true;
			}
			return false;
		}
		
		private int findClearTrack(RouteSeg r, boolean isPmos, double wireWid) {
			// external nets block the entire track
			boolean ext = r.hasExports();
			double xLo = ext ? FULL_TRACK_XLO : (r.getLoX() - wireWid/2);
			double wid = ext ? FULL_TRACK_WID : (r.getHiX() - r.getLoX() + wireWid);
			
			int maxTrack = isPmos ? nbPmosTracks : nbNmosTracks;
			int dir = isPmos ? 1 : -1;
			for (int track=1; track<=maxTrack; track++) {
				double yLo = stdCell.getTrackY(dir*track) - wireWid/2;
				if (!isBlocked(xLo, yLo, wid, wireWid)) return track*dir;
			}
			return 0;
		}
		
		public void occupyTrack(RouteSeg r, double y, double wireWid) {
			// external nets block the entire track
			boolean ext = r.hasExports();
			double xLo = ext ? FULL_TRACK_XLO : (r.getLoX() - wireWid/2);
			double wid = ext ? FULL_TRACK_WID : (r.getHiX() - r.getLoX() + wireWid);
			double yLo = y - wireWid/2;
			Rectangle2D block = new Rectangle2D.Double(xLo, yLo, wid, wireWid);
			blockages.add(block);
		}
		
		public void occupyTrack(RouteSeg r, int trkNdx, double wireWid) {
			occupyTrack(r, stdCell.getTrackY(trkNdx), wireWid);
		}
		
		public double getTrackY(RouteSeg r, double wireWid) {
			int track;
			if (r.hasPstk()) {
				// segments with PMOS stacks must use tracks in P region
				error(r.hasNstk(), "RouteSeg requires 2 tracks");
				track = findClearTrack(r, PMOS, wireWid);
				error(track==-1, "ran out of PMOS routing tracks");
			} else if (r.hasNstk()) {
				// segments with NMOS stacks must use tracks in N region
				track = findClearTrack(r, NMOS, wireWid);
				error(track==0, "ran out of NMOS routing tracks");
			} else {
				// Segments without stacks can be routed in either region.
				int pTrack = findClearTrack(r, PMOS, wireWid);
				int nTrack = findClearTrack(r, NMOS, wireWid);
				error(pTrack==0 && nTrack==0, "ran out of routing tracks");
				if (pTrack!=0 && nTrack!=0) {
					// use the inner most track
					track = (pTrack < -nTrack) ? pTrack : nTrack;
				} else {
					// use the only track we've got
					track = (nTrack!=0) ? nTrack : pTrack;
				}
			}
			occupyTrack(r, track, wireWid);
			return stdCell.getTrackY(track);
		}
	}
	
	// In addition to the ordinary uninteresting schematic pieces, also
	// skip Cell wire{ic}
	private static class SkipWirePortInsts extends PortFilter.SchemPortFilter {
		public boolean skipPort(PortInst pi) {
			// if it's a wire pin, bus pin, off page then pitch it
			if (super.skipPort(pi))  return true;
			NodeProto np = pi.getNodeInst().getProto();
			
			// if it's an instance of the Cell wire{ic} then pitch it
			if (np instanceof Cell && np.getName().equals("wire{ic}")) {
				return true;
			} 
			if (np instanceof PrimitiveNode && 
			    np.getName().equals("Global-Signal")) {
				return true;
			}
			return false;
		}
	}
	
	// ----------------------------- private data -----------------------------
	static final PortFilter SKIP_WIRE_PORTINSTS = new SkipWirePortInsts();
	
	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}
	
	private static double getNumericVal(Object val) {
		if (val==null) {
			System.out.println("null value detected, using 40");
			return 40;
		} if (val instanceof Number) {
			return ((Number)val).doubleValue();
		}
		error(true, "not a numeric value: "+val);
		return 0;
	}
	
	private static Cell getPurpleLay(NodeInst iconInst, VarContext context,
									 StdCellParams stdCell) {
		NodeProto prot = iconInst.getProto();
		String pNm = prot.getName();
		
		// we actually generate layout for two primitive nodes: Power and Ground
		if (!iconInst.isCellInstance()) {
			if (pNm.equals("Power")) {
				return TieHi.makePart(stdCell);
			} else if (pNm.equals("Ground")) {
				//return TieLo.makePart(stdCell);
			}
			error(true, "can't generate layout for PrimitiveNode: "+pNm);
		}
		
		// Keepers require special handling because they don't have sizes
		// attached to them.  Instead, the components making up keepers
		// have sizes attached.  We need to pass the schematic
		// implementing the keeper to the keeper generator.
		if (pNm.equals("keeper_high{ic}")) {
			NodeProto npe = ((Cell)iconInst.getProto()).getEquivalent();
			return KeeperHigh.makePart((Cell)npe, context.push(iconInst), stdCell);
		} else if (pNm.equals("keeper_low{ic}")) {
			NodeProto npe = ((Cell)iconInst.getProto()).getEquivalent();
			return KeeperLow.makePart((Cell)npe, context.push(iconInst), stdCell);
		}
		
		Variable var = iconInst.getVar(Tech.ATTR_S);
		if (var==null)  var = iconInst.getVar(Tech.ATTR_SP);
		if (var==null)  var = iconInst.getVar(Tech.ATTR_SN);
		if (var==null)  var = iconInst.getVar(Tech.ATTR_X);
		double x = getNumericVal(context.evalVar(var));
		//System.out.println("Gate Type: " + pNm + ", Gate Size: " + x);  // Daniel
		
		StdCellParams sc = stdCell;
		if      (pNm.equals("nms1{ic}"))          return Nms1.makePart(x, sc);
		else if (pNm.equals("nms1K{ic}"))         return Nms1.makePart(x, sc);
		else if (pNm.equals("nms2{ic}"))          return Nms2.makePart(x, sc);
		else if (pNm.equals("nms2_sy{ic}"))       return Nms2_sy.makePart(x, sc);
		else if (pNm.equals("nms3_sy3{ic}"))      return Nms3_sy3.makePart(x, sc);
		else if (pNm.equals("pms1{ic}"))          return Pms1.makePart(x, sc);
		else if (pNm.equals("pms1K{ic}"))         return Pms1.makePart(x, sc);
		else if (pNm.equals("pms2{ic}"))          return Pms2.makePart(x, sc);
		else if (pNm.equals("pms2_sy{ic}"))       return Pms2_sy.makePart(x, sc);
		else if (pNm.equals("inv{ic}"))           return Inv.makePart(x, sc);
		else if (pNm.equals("invCTLn{ic}"))       return InvCTLn.makePart(x, sc);
		// for Daniel
		else if (pNm.equals("inv_ll{ic}"))        return Inv.makePart(x, sc);
		else if (pNm.equals("inv_passgate{ic}"))  return Inv_passgate.makePart(x, sc);
		else if (pNm.equals("inv2i{ic}"))         return Inv2i.makePart(x, sc);
		else if (pNm.equals("inv2iKp{ic}"))       return Inv2iKp.makePart(x, sc);
		else if (pNm.equals("inv2iKn{ic}"))       return Inv2iKn.makePart(x, sc);
		else if (pNm.equals("invLT{ic}"))         return InvLT.makePart(x, sc);
		else if (pNm.equals("invHT{ic}"))         return InvHT.makePart(x, sc);
		else if (pNm.equals("nand2{ic}"))         return Nand2.makePart(x, sc);
		else if (pNm.equals("nand2k{ic}"))        return Nand2.makePart(x, sc);
		else if (pNm.equals("nand2en{ic}"))       return Nand2en.makePart(x, sc);
		else if (pNm.equals("nand2en_sy{ic}"))    return Nand2en_sy.makePart(x, sc);
		else if (pNm.equals("nand2HLT{ic}"))      return Nand2HLT.makePart(x, sc);
		else if (pNm.equals("nand2LT{ic}"))       return Nand2LT.makePart(x, sc);
		else if (pNm.equals("nand2_sy{ic}"))      return Nand2_sy.makePart(x, sc);
		else if (pNm.equals("nand2HLT_sy{ic}"))   return Nand2HLT_sy.makePart(x, sc);
		else if (pNm.equals("nand2LT_sy{ic}"))    return Nand2LT_sy.makePart(x, sc);
		else if (pNm.equals("nand2PH{ic}"))       return Nand2PH.makePart(x, sc);    
		else if (pNm.equals("nand2PHfk{ic}"))     return Nand2PHfk.makePart(x, sc);    
		else if (pNm.equals("nand3{ic}"))         return Nand3.makePart(x, sc);
		else if (pNm.equals("nand3MLT{ic}"))      return Nand3MLT.makePart(x, sc);
		else if (pNm.equals("nand3LT{ic}"))       return Nand3LT.makePart(x, sc);
		else if (pNm.equals("nand3_sy3{ic}"))     return Nand3_sy3.makePart(x, sc);
		else if (pNm.equals("nand3en_sy{ic}"))    return Nand3en_sy.makePart(x, sc);
		else if (pNm.equals("nand3LT_sy3{ic}"))   return Nand3LT_sy3.makePart(x, sc);
		else if (pNm.equals("nand3en_sy3{ic}"))   return Nand3en_sy3.makePart(x, sc);
		else if (pNm.equals("nor2{ic}"))          return Nor2.makePart(x, sc);
		else if (pNm.equals("nor2LT{ic}"))        return Nor2LT.makePart(x, sc);
		else if (pNm.equals("nor2kresetV{ic}"))   return Nor2kresetV.makePart(x, sc);
		// patch for Robert Drost
		else if (pNm.equals("invK{ic}"))          return Inv.makePart(x, sc);
		else {
			error(true, "Don't know how to generate layout for schematic icon: "+
				  pNm);
			return null;
		}
	}
	
	private static boolean isNstkNm(String nm) {
		return nm.startsWith("nms1") || nm.startsWith("nms2") ||
			nm.startsWith("NmosWellTie");
	}
	private static boolean isPstkNm(String nm) {
		return nm.startsWith("pms1") || nm.startsWith("pms2") ||
			nm.startsWith("PmosWellTie");
	}
	private static boolean isNstk(Cell c) {
		return isNstkNm(c.getName());
	}
	private static boolean isPstk(Cell c) {
		return isPstkNm(c.getName());
	}
	private static boolean isNstk(NodeInst ni) {
		return isNstkNm(ni.getProto().getName());
  }
	private static boolean isPstk(NodeInst ni) {
		return isPstkNm(ni.getProto().getName());
	}
	
	private static boolean isUsefulIconInst(NodeInst ni, Cell schematic) {
		if (ni.isIconOfParent()) return false;
		NodeProto np = ni.getProto();
		if (!ni.isCellInstance()) {
			// Power and Ground symbols mean we need to connect this net to
			// vdd or gnd.
			if (np.getName().equals("Power") ||
				np.getName().equals("Ground")) return true;
			
			// skip all other primitive drawing nodes such as wire pins,
			// off-page.
			return false;
		}
		
		Cell c = (Cell) np;
		
		// skip anything that's not an Icon View
		if (!c.getView().getFullName().equals("icon")) return false;
		if (c.getName().equals("wire{ic}")) return false;
		return true;
	}
	
	
	// Return layout instances. iconToLay maps from the icon instance to
	// the layout instance.
	private static void makeLayoutInsts(ArrayList<NodeInst> layInsts, HashMap<NodeInst,NodeInst> iconToLay,
										Cell schematic, Cell gasp,
										VarContext context,
										StdCellParams stdCell) {
		Iterator<NodeInst> iconInsts = schematic.getNodes();
		while (iconInsts.hasNext()) {
			NodeInst iconInst = iconInsts.next();
			if (!isUsefulIconInst(iconInst, schematic)) continue;
			Cell lay = getPurpleLay(iconInst, context, stdCell);
			NodeInst layInst = LayoutLib.newNodeInst(lay, 0, 0, 1, 1, 0, gasp);
			iconToLay.put(iconInst, layInst);
			layInsts.add(layInst);
		}
	}
	
	// Sort nets to reduce non-determinism. This isn't totally
	// satisfactory because only nets with names will be ordered.
	private static ArrayList<Network> sortNets(Iterator<Network> nets) {
		ArrayList<Network> sortedNets = new ArrayList<Network>();
		while (nets.hasNext()) {sortedNets.add(nets.next());}
		
		Comparator<Network> netNmComp = new Comparator<Network>() {
				public int compare(Network n1, Network n2) {
					Iterator<String> nms1 = n1.getNames();
					Iterator<String> nms2 = n2.getNames();
					
					if (!nms1.hasNext() && !nms2.hasNext()) {
						// neither net has a name
						return 0;
					} else if (!nms1.hasNext()) {
						return 1;
	  } else if (!nms2.hasNext()) {
		  return -1;
	  }
					String nm1 = nms1.next();
					String nm2 = nms2.next();
					return nm1.compareTo(nm2);
				}
      };
		Collections.sort(sortedNets, netNmComp);
		return sortedNets;
	}
	
	// Build route segments and sort them into two groups.
	//
	// 1) stkSegs: segments with N-stacks but no P-stacks and segments
	// with P-stacks but no N-stacks
	//
	// 2) noStkSegs: segments with neither N-stacks nor P-stacks.
	//
	// Nets with both N-stacks and P-stacks are divided into two
	// segments, one N-stack only, and one P-stack only.  If nets of
	// this type don't have at least one non-stack device then we need
	// to generate an additional part that contains a vertical routing
	// channel for connecting the two segments.
	private static void buildRouteSegs(ArrayList<RouteSeg> stkSegs, ArrayList<RouteSeg> noStkSegs,
                                     ArrayList<NodeInst> vertTracks, Iterator<Network> nets,
									   HashMap<NodeInst,NodeInst> iconToLay, HashMap<String,Object> expTrkAsgn,
									   StdCellParams stdCell, Cell gasp) {
		ArrayList<Network> sortedNets = sortNets(nets);
		
		for (int i=0; i<sortedNets.size(); i++) {
			Network net = sortedNets.get(i);
			RouteSeg r = new RouteSeg(net, iconToLay, expTrkAsgn);
			
			if (r.getAllPorts().size()==0) {
				// Schematic net had nothing useful attached to it
			} else if (r.hasNstk() && r.hasPstk()) {
				RouteSeg pr = r.splitOffPstkRouteSeg(vertTracks, stdCell, gasp);
				stkSegs.add(r);
				stkSegs.add(pr);
			} else if (r.hasNstk() || r.hasPstk()) {
				stkSegs.add(r);
			} else {
				noStkSegs.add(r);
			}
		}
	}
	
	private static void sortPortsLeftToRight(ArrayList<PortInst> ports) {
		Comparator<PortInst> compare = new Comparator<PortInst>() {
				public int compare(PortInst p1, PortInst p2) {
					double diff = LayoutLib.roundCenterX(p1) - 
					              LayoutLib.roundCenterX(p2);
					if (diff<0) return -1;
					if (diff==0) return 0;
					return 1;
				}
			};
		Collections.sort(ports, compare);
	}
	
	private static void metal1route(PortInst prev, PortInst port) {
		NodeInst prevInst = prev.getNodeInst();
		NodeInst portInst = port.getNodeInst();
		// If one gate is half height and the other full height then we
		// must route horizontally from the half height gate first because
		// the half height gate doesn't have a full vertical metal-1
		// channel.
		if (isNstk(prevInst) || isPstk(prevInst)) {
			LayoutLib.newArcInst(Tech.m1, 4, prev, port);
		} else {
			// prev is full height so route vertically from prev
			LayoutLib.newArcInst(Tech.m1, 4, port, prev);
		}
	}
	
	// Try to route physically adjacent ports in metal-1.  If we connect
	// a pair of ports in metal-1, then remove the first of the two
	// ports from the port list so the Track FillRouter doesn't connect them
	// also.
	//
	// The list of ports must be sorted in X from lowest to hightest.
	private static void metal1route(ArrayList<PortInst> ports) {
		final double ADJACENT_DIST = 7;
		for (int i=1; i<ports.size(); i++) {
			PortInst prev = ports.get(i-1);
			PortInst port = ports.get(i);
			
			double dx = LayoutLib.roundCenterX(port) - 
			            LayoutLib.roundCenterX(prev);
			error(dx<=0, "metal1route: ports not sorted left to right!");
			if (dx<=ADJACENT_DIST) {
				metal1route(prev, port);
				// Remove prev and back up so we consider the port that moves
				// into index i as a result of the deletion.
				ports.remove(--i);
			}
			prev = port;
		}
	}
	
	private static void connectSegment(RouteSeg r, TrackAllocator trackAlloc,
									   StdCellParams stdCell, Cell gasp) {
		ArrayList<PortInst> ports = r.getAllPorts();
		sortPortsLeftToRight(ports);
		
		// first route physically adjacent ports in metal-1
		metal1route(ports);
		
		// Don't allocate a metal-2 track if the net is internal and there
		// is nothing left to connect in metal-2.
		if (ports.size()<=1 && !r.hasExports()) return;
		
		double trackY = r.hasExpTrk() ? (
            stdCell.getPhysTrackY(r.getExpTrk())
        ) : (
            trackAlloc.getTrackY(r, 4.0)
        );
		
		//if (r.hasExpTrk()) {
		//System.out.println("Using assigned track: "+trackY);
		//}
	  
		TrackRouter route = new TrackRouterH(Tech.m2, 4.0, trackY, gasp);
		
		// connect RouteSeg's exports
		Iterator<Export> expIt = r.findExports();
		while (expIt.hasNext()) {
			Export exp = expIt.next();
			String expNm = exp.getName();
			// Align the export with left most port
			
			// RKao debug
			error(ports.size()==0, "No device ports on this net?: "+expNm);
			
			double x = LayoutLib.roundCenterX(ports.get(0));
			LayoutLib.newExport(gasp, expNm, exp.getCharacteristic(), Tech.m2, 
			                    4, x, trackY);
			route.connect(gasp.findExport(expNm));
		}
		
		// connect RouteSeg's ports
		route.connect(ports);
	}
	
	private static void connectSegments(TrackAllocator trackAlloc,
										ArrayList<RouteSeg> segs, boolean exports,
										StdCellParams stdCell, Cell gasp) {
		for (int i=0; i<segs.size(); i++) {
			RouteSeg r = segs.get(i);
			if (exports==r.hasExports()) connectSegment(r, trackAlloc, stdCell, gasp);
		}
	}
	
	// find the sums of the lengths of all routing segments
	private static double estimateWireLength(ArrayList<RouteSeg> routeSegs) {
		double len = 0;
		for (int i=0; i<routeSegs.size(); i++) {
			len += routeSegs.get(i).estimateLength();
		}
		return len;
	}
	
	private static void buildPlacerNetlist(Placer placer, ArrayList<NodeInst> insts,
										   ArrayList<RouteSeg> routeSegs) {
		// create Placer instances
		HashMap<PortInst,Placer.Port> portToPlacerPort = new HashMap<PortInst,Placer.Port>();
		for (int i=0; i<insts.size(); i++) {
			NodeInst inst = insts.get(i);
			//inst.alterShape(1, 1, 0, 0, 0);
			
			int type;
			if (isNstk(inst))         type=Placer.N;
			else if (isPstk(inst))    type=Placer.P;
			else                      type=Placer.PN;
			double w = inst.getBounds().getWidth();
			
			Placer.Inst plInst = placer.addInst(type, w, inst);
			
			Iterator<PortInst> it = inst.getPortInsts();
			while (it.hasNext()) {
				PortInst port = it.next();
				double x = LayoutLib.roundCenterX(port);
				double y = LayoutLib.roundCenterY(port);
				Placer.Port plPort = plInst.addPort(x, y);
				portToPlacerPort.put(port, plPort);
			}
		}
		// create Placer nets
		for (int i=0; i<routeSegs.size(); i++) {
			RouteSeg seg = routeSegs.get(i);
			Placer.Net plNet = placer.addNet();
			
			ArrayList<PortInst> ports = seg.getAllPorts();
			for (int j=0; j<ports.size(); j++) {
				PortInst port = ports.get(j);
				Placer.Port plPort = portToPlacerPort.get(port);
				error(plPort==null, "can't find placer port");
				plNet.addPort(plPort);
			}
		}
	}
	
	// try again using data structures built just for placement
	private static ArrayList<NodeInst> place(ArrayList<NodeInst> insts, ArrayList<RouteSeg> routeSegs,
								   StdCellParams stdCell, Cell gasp) {
		long sT = System.currentTimeMillis();
		
		Placer placer = new Placer(stdCell, gasp);
		buildPlacerNetlist(placer, insts, routeSegs);
		ArrayList<NodeInst> ans = placer.place1row();
		long eT = System.currentTimeMillis();
		double seconds = (eT-sT)/1000.0;
		//System.out.println("Placement took: "+seconds+" seconds");
		
		return ans;
	}
	
	private static void connectWellTies(ArrayList<NodeInst> layInsts,
										StdCellParams stdCell, Cell gasp) {
		if (stdCell.getSeparateWellTies()) {
			// Rock connects well ties to exports rather than to vdd or gnd
			TrackRouter nTie = new TrackRouterH(Tech.m2,
												stdCell.getNmosWellTieWidth(),
												stdCell.getNmosWellTieY(),
												gasp);
			LayoutLib.newExport(gasp, stdCell.getNmosWellTieName(), 
			                    stdCell.getNmosWellTieRole(),
								Tech.m2, 4,
								// m1_m1_sp/2
								stdCell.getNmosWellTieWidth()/2 + 1.5, 
								stdCell.getNmosWellTieY());
			nTie.connect(gasp.findExport(stdCell.getNmosWellTieName()));
			nTie.connect(layInsts, stdCell.getNmosWellTieName());
			TrackRouter pTie = new TrackRouterH(Tech.m2,
												stdCell.getPmosWellTieWidth(),
												stdCell.getPmosWellTieY(),
												gasp);
			LayoutLib.newExport(gasp, stdCell.getPmosWellTieName(),
								stdCell.getPmosWellTieRole(),
								Tech.m2, 4,
								// m1_m1_sp/2
								stdCell.getPmosWellTieWidth()/2 + 1.5,
								stdCell.getPmosWellTieY());
			pTie.connect(gasp.findExport(stdCell.getPmosWellTieName()));
			pTie.connect(layInsts, stdCell.getPmosWellTieName());
		}
	}
	
	/*
	  private static ArrayList wireEquivPortsList() {
	  ArrayList equivPortsLists = new ArrayList();
	  
	  // search all libraries for the wire icon
	  Iterator it = Electric.getLibraries();
	  while (it.hasNext()) {
      Library lib = it.next();
      Cell wireIcon = lib.findCell("wire{ic}");  if (wireIcon==null) continue;
      PortProto a = wireIcon.findPort("a");        if (a==null) continue;
      PortProto b = wireIcon.findPort("b");        if (b==null) continue;
      
      // Well, it's got the right type, name, and port names. I might
      // as well treat it like a wire icon.
      ArrayList equivPortsList = new ArrayList();
      equivPortsList.add(a);  equivPortsList.add(b);
      equivPortsLists.add(equivPortsList);
	  }
	  return equivPortsLists;
	  }
	*/
	
	// Remove from instance name first open bracket and everything
	// following it.
	private static String stripBusNotation(String instNm) {
		int openBrack = instNm.indexOf("[");
		return (openBrack>=0) ? instNm.substring(0,openBrack) : instNm;
	}
	
	private static void instPath1(StringBuffer path, VarContext context) {
		Nodable ni = context.getNodable();
		if (ni==null) return;
		
		instPath1(path, context.pop());
		
		String me = ni.getName();
		error(me==null, "instance in VarContext with no name!!!");
		
		String noBus = stripBusNotation(me);
		path.append("/"+noBus);
	}
	
	private static String instPath(VarContext context) {
		StringBuffer path = new StringBuffer();
		instPath1(path, context);
		return path.toString();
	}
	
	private static PortInst findFirstPort(ArrayList<NodeInst> layInsts, String nm) {
		for (int i=0; i<layInsts.size(); i++) {
			NodeInst pi = layInsts.get(i);
			PortInst port = pi.findPortInst(nm);
			if (port!=null) return port;
		}
		error(true, "no NodeInst with port found: "+nm);
		return null;
	}
	
	// Create the name of the new layout Cell by appending the instance
	// path to the schematic facet name.
	private static String layoutCellName(Cell schematic, VarContext context) {
		// get the name of the schematic (without the "{sch}" suffix)
		String schemNm = schematic.getName();
		int sfxPos = schemNm.indexOf("{sch}");
		error(sfxPos==-1, "SchemToLay: no {sch} suffix on Cell schematic name?");
		schemNm = schemNm.substring(0, sfxPos);
		
		return schemNm + "__" + instPath(context) + "{lay}";
	}
	
	private static void blockAssignedTracks(TrackAllocator trackAlloc,
											ArrayList<RouteSeg> routeSegs,
											StdCellParams stdCell) {
		for (int i=0; i<routeSegs.size(); i++) {
			RouteSeg r = routeSegs.get(i);
			if (r.hasExpTrk()) {
				trackAlloc.occupyTrack(r, stdCell.getPhysTrackY(r.getExpTrk()), 4.0);
			}
		}
	}
	
	/** A schematic may have track assignments in the form of variables:
	 * "ATTR_track=234" attached to Exports.  Track assignments may also
	 * be specified by the program calling SchemToLay.makePart().  In
	 * the case of conflicts, the programatic assignments will override
	 * the schematic assignments. */
	private static HashMap<String,Object> mergeTrackAssign(Cell schem, HashMap<String,Object> progAsgn,
											StdCellParams stdCell) {
		// get assignment from schematic
		HashMap<String,Object> schAsgn = stdCell.getSchemTrackAssign(schem);
		
		// program assignments override schematic assignments
		HashMap<String,Object> combAsgn = new HashMap<String,Object>(schAsgn);
		combAsgn.putAll(progAsgn);
		
		// report erroneous assignments
		stdCell.validateTrackAssign(progAsgn, schem);
		stdCell.validateTrackAssign(combAsgn, schem);
		
		return combAsgn;
	}
	
	/** Read a Gasp cell schematic and produce the layout for it. <p>
	 * Equivalent to:
	 *
	 *<p> <code> makePart(schematic, context, new HashMap(), stdCell);
	 *</code>*/
	public static Cell makePart(Cell schem, VarContext context,
								StdCellParams stdCell) {
		return makePart(schem, context, new HashMap<String,Object>(), stdCell);
	}
	
	/** Read a Gasp cell schematic and produce the layout for it.
	 * @param schem Schematic view Cell
	 * @param context Hierarchical path from root schematic.
	 * @param exportTrackAssign Map from export name to Integer track
	 * index. Negative indices represent NMOS tracks. Non-negative
	 * indices represent PMOS tracks. Index 0 is PMOS track closest to
	 * the center. Index -1 is NMOS track closest to the center.
	 * @param stdCell Standard cell parameters used to build the layout */
	public static Cell makePart(Cell schem, VarContext context,
								HashMap<String,Object> exportTrackAssign,
								StdCellParams stdCell) {
		error(!schem.getView().getFullName().equals("schematic"),
			  "not a schematic: "+schem.getName());
		
		String nm = layoutCellName(schem, context);
		System.out.println("SchemToLay making: "+nm);
		Cell gasp = stdCell.findPart(nm);
		if (gasp!=null) return gasp;
		gasp = stdCell.newPart(nm);
		
		// create layout for each schematic instance 
		HashMap<NodeInst,NodeInst> iconToLay = new HashMap<NodeInst,NodeInst>();
		ArrayList<NodeInst> layInsts = new ArrayList<NodeInst>();
		makeLayoutInsts(layInsts, iconToLay, schem, gasp, context, stdCell);
		
		HashMap<String,Object> combTrkAsgn = mergeTrackAssign(schem, exportTrackAssign, stdCell);
		
		// create routing segments that will each require a track to route
		Netlist netlist = schem.getNetlist(true);
		ArrayList<RouteSeg> stkSegs = new ArrayList<RouteSeg>();
		ArrayList<RouteSeg> noStkSegs = new ArrayList<RouteSeg>();
		ArrayList<NodeInst> vertTracks = new ArrayList<NodeInst>();
		buildRouteSegs(stkSegs, noStkSegs, vertTracks, netlist.getNetworks(),
					   iconToLay, combTrkAsgn, stdCell, gasp);
		// Append parts containing space for vertical routing tracks
		// needed to connect NMOS stacks and PMOS stacks.
		layInsts.addAll(vertTracks);
		
		ArrayList<RouteSeg> allSegs = new ArrayList<RouteSeg>(stkSegs);
		allSegs.addAll(noStkSegs);
		
		layInsts = place(layInsts, allSegs, stdCell, gasp);
		
		// vdd and gnd wires
		TrackRouter gnd = new TrackRouterH(Tech.m2, stdCell.getGndWidth(),
										   stdCell.getGndY(), gasp);
		TrackRouter vdd = new TrackRouterH(Tech.m2, stdCell.getVddWidth(),
										   stdCell.getVddY(), gasp);
		
		// place vdd and gnd exports on the first full height layout instance
		Export.newInstance(gasp, findFirstPort(layInsts, "gnd"), "gnd")
		    .setCharacteristic(PortCharacteristic.GND);
		gnd.connect(gasp.findExport("gnd"));
		Export.newInstance(gasp, findFirstPort(layInsts, "vdd"), "vdd")
		    .setCharacteristic(PortCharacteristic.PWR);
		vdd.connect(gasp.findExport("vdd"));
		
		// connect up vdd and gnd
		vdd.connect(layInsts, "vdd");
		gnd.connect(layInsts, "gnd");
		
		// Rock connect well ties to exports rather than to vdd or gnd
		connectWellTies(layInsts, stdCell, gasp);
		
		//
		// Route signal wires.
		//
		// The key problem is that a P-stack is only half height and only
		// allocates vertical routing channels in the upper half of a
		// cell.  N-stack devices have a corresponding restriction.  It's
		// difficult to connect a P-stack to an N-stack because there's no
		// single vertical channel that is guaranteed to be clear in both.
		// Thus we route nets connected to P-stacks in the upper half of
		// the cell, and nets connected to N-stacks in the lower half of
		// the cell.  A Net that connects to both N-stacks and P-stacks
		// must be be split into an N-stack segment and a P-stack
		// segment. The two segments must be connected by a non-stack cell
		// or a special vertical routing channel.
		
		TrackAllocator trackAlloc = new TrackAllocator(stdCell);
		
		blockAssignedTracks(trackAlloc, stkSegs, stdCell);
		blockAssignedTracks(trackAlloc, noStkSegs, stdCell);
		
		// First connect segments that have no exports.  These can
		// potentially share tracks.  Then connect segments that are cell
		// exports.  We need to leave track clear for these.
		for (int i=0; i<2; i++) {
			boolean exports = i==0 ? false : true;
			
			// Route RouteSegs that must be in the top of the cell and
			// RouteSegs that must be in the bottom of the cell.
			connectSegments(trackAlloc, stkSegs, exports, stdCell, gasp);
			
			// Route segments that can be either in the top or bottom of the
			// cell.
			connectSegments(trackAlloc, noStkSegs, exports, stdCell, gasp);
		}
		
		// Compare schematic to layout
		/*
		if (stdCell.nccEnabled()) {
			NccOptions options = new NccOptions();
			options.checkExportNames = true;
			options.hierarchical = true;
			options.mergeParallel = true;
			boolean mismatch =
				Electric.networkConsistencyCheck(schem, context, gasp, options);
			error(mismatch, "SchemToLay: gasp cell topological mismatch");
		}
		*/
		return gasp;
	}
}
