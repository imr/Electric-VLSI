/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetEquivalence.java
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
package com.sun.electric.tool.ncc.result.equivalence;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.CellInfo;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NetNameProxy;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.NodaNets;
import com.sun.electric.tool.generator.layout.NodaNets.NodaPortInst;
import com.sun.electric.tool.ncc.basic.CellContext;

/** Object to map from a net or node in one design to the 
 * "NCC equivalent" net or node in the 
 * other design. */
class NetEquivalence implements Serializable {
    static final long serialVersionUID = 0;
    
    // ----------------------------- private types ----------------------------
    private static class TraceConnectivityUpThroughExports 
                                        extends HierarchyEnumerator.Visitor {
    	private final String netNm;
    	private final ArrayList<VarContext> netCtxtArray;
    	private int curCtxtNdx;
    	
		private String nameOfNodableToDescendInto; 
    	private boolean doneEnumerating;
    	// this is the result. null if not found
    	private NetNameProxy highestEquivNet; 
    	
    	// Build ArrayList of all the VarContext nodes from globalContext to ctxt.
    	// This permits random access.
    	// First element of ArrayList is VarContext.globalContext
    	private static ArrayList<VarContext> buildContextArray(VarContext ctxt) {
    		LinkedList<VarContext> ll = new LinkedList<VarContext>();
    		LayoutLib.error(ctxt==null, "buildContextArray: null ctxt not allowed");
    		while (true) {
    			ll.addFirst(ctxt);
    			if (ctxt==VarContext.globalContext) break;
    			ctxt = ctxt.pop();
    		}
    		ArrayList<VarContext> al = new ArrayList<VarContext>();
    		al.addAll(ll);
    		return al;
    	}
    	
    	private Network findNetworkNamed(Netlist nl, String nm) {
    		for (Iterator<Network> netIt=nl.getNetworks(); netIt.hasNext();) {
    			Network net = netIt.next();
    			if (net.hasName(nm)) return net;
    		}
    		return null;
    	}
    	
    	private TraceConnectivityUpThroughExports(String netNm, 
    			                              ArrayList<VarContext> netCtxtArray, 
    			                              int curCtxtNdx) {
    		this.netNm = netNm;
    		this.netCtxtArray = netCtxtArray;
    		this.curCtxtNdx = curCtxtNdx;
    	}
    	
    	// ----------------------- overridden methods -------------------------
    	@Override
    	public boolean visitNodeInst(Nodable no, CellInfo ci) {
    		if (doneEnumerating) return false;
    		if (no.getName().equals(nameOfNodableToDescendInto)) {
    			curCtxtNdx++;
    			return true;
    		}
    		return false;
    	}
    	@Override
    	public boolean enterCell(CellInfo ci) {
    		if (doneEnumerating) return false;
    		if (curCtxtNdx==netCtxtArray.size()-1) {
    			// We've descended into the Cell containing the sought Network
    			// Now find the Network.
    			doneEnumerating = true;
    			
    			Netlist nl = ci.getNetlist();
    			Network net = findNetworkNamed(nl, netNm);
    			if (net==null) return false; // no net matching name!
    			highestEquivNet = ci.getUniqueNetNameProxy(net, "/");

    			return false;
    		} else {
    			// We need to descend once more
    			nameOfNodableToDescendInto = 
    				netCtxtArray.get(curCtxtNdx+1).getNodable().getName();
    			return true;
    		}
    	}
    	@Override
    	public void exitCell(CellInfo ci) {}

    	// --------------------- intended interface ---------------------------
    	/** Trace the connectivity through Exports to find the Network
    	 * in the highest Cell in the design hierarchy that is connected to
    	 * Network net. <p>
    	 * Subtle: Begin the enumeration from the point at which NCC began the
    	 * enumeration because VarContext above that point isn't guaranteed to make
    	 * sense (e.g. VarContext might point to deleted instance) */
    	public static NetNameProxy findHighestNet(Network net, VarContext netCtxt,
    			                                  Cell nccRootCell,
    			                                  VarContext nccRootCtxt) {
    		ArrayList<VarContext> nccRootCtxtArray = buildContextArray(nccRootCtxt);
    		ArrayList<VarContext> netCtxtArray = buildContextArray(netCtxt);
    		int nccRootDepth = nccRootCtxtArray.size();
    		
    		// make sure the initial portion of the netCtxt matches the
    		// nccRootCtxt
    		if (netCtxtArray.size()<nccRootDepth ||
    			!netCtxtArray.get(nccRootDepth-1).equals(nccRootCtxt)) {
    			return null;
    		}
    		
    		TraceConnectivityUpThroughExports visitor =
    			new TraceConnectivityUpThroughExports(net.getName(), netCtxtArray, 
    					                              nccRootDepth-1);
    		HierarchyEnumerator.enumerateCell(nccRootCell, nccRootCtxt, visitor);
    		
    		return visitor.highestEquivNet;
    	}
    }
    
    // --------------------------- private data -------------------------------
	private final NetNameProxy[][] equivNets;
	private final Cell[] nccRootCells;
	private final VarContext[] nccRootCtxts;
	
	private final int numDesigns, numNets;
	private InstancePathToNccContext[] instToNetNccCtxt;
	/** Cache the index of the last design that satisified the last 
	 * findEquivalent() query? */
	private int lastDesignHit;
	
	private boolean nameMatch(NetNameProxy prox, Network net) {
		for (Iterator<String> it=prox.leafNames(); it.hasNext();) {
			String proxNm = it.next();
			if (net.hasName(proxNm)) return true;
		}
		return false;
	}
	private void pr(String s) {System.out.print(s);}
	private void prln(String s) {System.out.println(s);}
	
	/** Tricky: Because instToNetNcCtxt takes so much space, build it 
	 * only on demand */
	private void buildNameTree() {
		if (instToNetNccCtxt!=null) return; 
		instToNetNccCtxt = new InstancePathToNccContext[numDesigns];
		for (int i=0; i<numDesigns; i++) {
			LayoutLib.error(equivNets[i].length!=numNets,
					        "designs don't have same numbers of nets?");
			instToNetNccCtxt[i] = new InstancePathToNccContext(equivNets[i]); 
		}
	}
	
	/** @param equivNets is a NetNameProxy[][]. NetNameProxy[d][n] gives the 
	 * nth net of the dth design.  NetNameProxy[a][n] is "NCC equivalent" to 
	 * NetNameProxy[b][n] for all a and b.*/
	public NetEquivalence(NetNameProxy[][] equivNets, Cell[] nccRootCells,
			              VarContext[] nccRootCtxts) {
		this.equivNets = equivNets;
		numDesigns = equivNets.length;
		numNets = equivNets[0].length;
		this.nccRootCells = nccRootCells;
		this.nccRootCtxts = nccRootCtxts;
	}

	private NetNameProxy findEquivNet(VarContext vc, Network net, 
			                            int designIndex) {
		LayoutLib.error(designIndex!=0 && designIndex!=1, 
				        "designIndex must be 0 or 1");
		buildNameTree();
		InstancePathToNccContext nameIndex = instToNetNccCtxt[designIndex];
		NccContext nc = nameIndex.findNccContext(vc);
		
		if (nc==null) return null;
		if (nc.getCell()!=net.getParent())  return null;
		if (!nc.getContext().equals(vc))  return null;
		for (Iterator<Integer> it=nc.getIndices(); it.hasNext();) {
			int index = it.next().intValue();
			NetNameProxy prox = equivNets[designIndex][index];
			if (nameMatch(prox, net)) {
				int equivDesign = designIndex==0 ? 1 : 0;
				return equivNets[equivDesign][index];
			}
		}
		return null;
	}
	
    private int countUnique() {
        Set<Network> networks = new HashSet<Network>();
        for (int i=0; i<2; i++) {
            for (int j=0; j<numNets; j++)  networks.add(equivNets[i][j].getNet());
        }
        return networks.size();
    }
    
	private NodaPortInst getPortFromUnshortedNet(Network n) {
		NodaNets noshortNets = new NodaNets(n.getParent(), false);
		Collection<NodaPortInst> ports = noshortNets.getPorts(n);
		for (NodaPortInst pi : ports) return pi;

		LayoutLib.error(true, "No ports found on Network?");
		return null;
	}

	// GUI netlists don't treat resistors as shorts. Let's find an
	// equivalent shorted-resistor Network. This is tricky. All
	// Networks except globals are guaranteed to be connected to at 
	// least one port. Therefore find a port on the old network,
	// short resistors, and try to find the network connected to the
	// same port.
	private Network netWhenResShorted(Network n) {
		// if resistors already shorted then nothing else can be done
		if (n.getNetlist().getShortResistors()) return null;
		prln("    RK Debug: Try shorting resistors");
		
		NodaPortInst port = getPortFromUnshortedNet(n);
		NodaNets shortedNets = new NodaNets(n.getParent(), true);
		Nodable no = shortedNets.getNoda(port.getNodable().getName());
		for (NodaPortInst pi : shortedNets.getPorts(no)) {
			if (pi.getIndex()==port.getIndex() &&
				pi.getPortProto()==port.getPortProto()) {
				return pi.getNet();
			}
		}
		prln("    RK Debug: Shorting resistors fails");
		
		return null;
	}

	/** Given a Network located at point in the design hierarchy specified by a
	 * VarContext, find the "NCC equivalent" net in the other design.
	 * <p>
	 * Subtle: Because the user interface may rebuild the Networks, we cannot 
	 * depend upon Network "==" for equality. Instead I depend upon 1) the
	 * .equals() equality of instance names along the VarContext, 2) the .equals()
	 * equality of the VarContext (which depends upon the == equality of Nodables
	 * along the VarContext), 3) the .equals equality of the names of the Network. 
	 * 4) the == equality of the parent Cells of the Network
	 * @param vc VarContext specifying a point in the hierarchy.
	 * @param net Network located at that point in the hierarchy.
	 * @return the "NCC equivalent" NetNameProxy or null if no equivalent can
	 * be found. */
	public NetNameProxy findEquivalentNet(VarContext vc, Network net) {
		NetNameProxy nnp = findEquivNet(vc, net, lastDesignHit);
		if (nnp!=null) return nnp;
		
		int otherDesign = lastDesignHit==0 ? 1 : 0;
		nnp = findEquivNet(vc, net, otherDesign);
		if (nnp!=null) {lastDesignHit=otherDesign; return nnp;}

		// net may be connected to an Export. If so, find the Network highest
		// in the design hierarchy connected to net
		if (!net.isExported()) return null;
		
		NetNameProxy higherNet = 
			TraceConnectivityUpThroughExports.findHighestNet(net, vc,
                nccRootCells[lastDesignHit], nccRootCtxts[lastDesignHit]);
		if (higherNet!=null) {
			nnp = findEquivNet(higherNet.getContext(), higherNet.getNet(), 
					           lastDesignHit);
			if (nnp!=null) {return nnp;}
		}
		
		higherNet = 
			TraceConnectivityUpThroughExports.findHighestNet(net, vc,
                nccRootCells[otherDesign], nccRootCtxts[otherDesign]);
		if (higherNet!=null) {
			nnp = findEquivNet(higherNet.getContext(), higherNet.getNet(), 
					           lastDesignHit);
			if (nnp!=null) {lastDesignHit=otherDesign;  return nnp;}
		}
		
		return null;
	}
	
	public NetNameProxy findEquivalentNetShortingResistors(VarContext vc, Network net) {
		NetNameProxy eqProx = findEquivalentNet(vc, net);
		if (eqProx==null) {
			// try extending this net by shorting resistors
			net = netWhenResShorted(net);
			if (net==null) return null;
			eqProx = findEquivalentNet(vc, net);
			if (eqProx==null) return null;
		}
		return eqProx;
	}
	
	/** Release cached information when you no longer need the Equivalence
	 * information.	 */
	void clearCache() { instToNetNccCtxt = null; }
	
	/** Regression test. Map from every net in design 0 to "NCC equivalent"
	 * net in design 1. Map from every net in design 1 to "NCC equivalent"
	 * net in design 0. 
	 * @return the number of errors. */
	public int regressionTest() {
		LayoutLib.error(numDesigns!=2, "we must have exactly two designs");
		
		int numErrors = 0;
		for (int desNdx=0; desNdx<numDesigns; desNdx++) {
			int otherDesign = desNdx==0 ? 1 : 0;
			for (int netNdx=0; netNdx<numNets; netNdx++) {
				NetNameProxy from = equivNets[desNdx][netNdx];
				VarContext fromVc = from.getContext();
				Network fromNet = from.getNet();
				NetNameProxy to = findEquivalentNet(fromVc, fromNet);
				
				if (to!=equivNets[otherDesign][netNdx]) {
					numErrors++;
					// Print Diagnostics
					prln("      From: "+from.toString());
					prln("      To: "+(to==null?"null":to.toString()));
					prln("      Equiv: "+equivNets[otherDesign][netNdx]);
				}
			}
		}
		pr("    Net equivalence regression "+
				         (numErrors==0 ? "passed. " : "failed. "));
		pr(" Equiv table size="+numNets+". ");
		pr(" Num unique Networks="+countUnique()+". ");
		if (numErrors!=0) System.out.print(numErrors+" errors.");
		pr("\n");

		return numErrors;
	}
}


