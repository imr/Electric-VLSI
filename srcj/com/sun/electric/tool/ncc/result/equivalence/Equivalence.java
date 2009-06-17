/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Equivalence.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NetNameProxy;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NodableNameProxy;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.variable.VarContext;

/** Save the equivalence information produced by NCC so 
 * that a tool can map a Part or Wire in the schematic 
 * to the equivalent Part or Wire in the layout.
 */
public class Equivalence implements Serializable {
    static final long serialVersionUID = 0;

	private final NetEquivalence netEquiv;
	private final NodeEquivalence nodeEquiv;
	
	private void prln(String s) {System.out.println(s);}
    
	public Equivalence(NetNameProxy[][] equivNets,
			           NodableNameProxy[][] equivNodes,
			           Cell[] nccRootCells, 
			           VarContext[] nccRootCtxts) {
		netEquiv = new NetEquivalence(equivNets, nccRootCells, nccRootCtxts);
		nodeEquiv = new NodeEquivalence(equivNodes);
	}
	/** Given a Network in one design, return the matching network
	 * in the other design. 
	 * @param vc the VarContext specifying the instance path of the network
	 * @param net the network in one design
	 * @return the matching network in the other design
	 */
	public NetNameProxy findEquivalentNet(VarContext vc, Network net) {
		return netEquiv.findEquivalentNet(vc, net);
	}
	/** Given a Network in one design, return the matching network
	 * in the other design. Treat resistors as short circuits. 
	 * @param vc the VarContext specifying the instance path of the network
	 * @param net the network in one design
	 * @return the matching network in the other design
	 */
	public NetNameProxy findEquivalentNetShortingResistors(VarContext vc, Network net) {
		return netEquiv.findEquivalentNetShortingResistors(vc, net);
	}
	/** Given a Nodable in one design, return the matching Nodable
	 * in the other design. 
	 * @param vc the VarContext specifying the instance path of the Nodable
	 * @param node the Nodable in one design
	 * @return the matching Nodable in the other design
	 */
	public NodableNameProxy findEquivalentNode(VarContext vc, Nodable node) {
		return nodeEquiv.findEquivalent(vc, node);
	}
	/** Perform a sanity check of the equivalence tables. This method
	 * is used by the NCC regressions.
	 * @param cell0 first Cell. this is used for the status message
	 * @param cell1 second Cell. this is used for the status message
	 */
	public int regressionTest(Cell cell0, Cell cell1) {
		prln("  Equivalence regression for: "+cell0.describe(false)+
			 " and "+cell1.describe(false));
		int numErrors = netEquiv.regressionTest() +
			            nodeEquiv.regressionTest();
		clearCache();
		return numErrors;
	}
	/** To reduce storage requirements, release cached information when you
	 * are done using this Equivalence table. */
	public void clearCache() {
		netEquiv.clearCache();
		nodeEquiv.clearCache();
	}
}
