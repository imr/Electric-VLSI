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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sun.electric.database.hierarchy.HierarchyEnumerator.NetNameProxy;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.generator.layout.LayoutLib;

/** Object to map from a net or node in one design to the 
 * "NCC equivalent" net or node in the 
 * other design. */
class NetEquivalence implements Serializable {
    static final long serialVersionUID = 0;

	private final NetNameProxy[][] equivNets;
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
	public NetEquivalence(NetNameProxy[][] equivNets) {
		this.equivNets = equivNets;
		numDesigns = equivNets.length;
		numNets = equivNets[0].length;
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
		
		return null;
	}
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

		// Tricky: because instToNetNccCtxt takes so much space, delete it 
		// after the regression.
		instToNetNccCtxt = null;
		
		return numErrors;
	}
}


