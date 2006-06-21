package com.sun.electric.tool.ncc.result.equivalence;

import java.io.Serializable;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NetNameProxy;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NodableNameProxy;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.variable.VarContext;

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
	public NetNameProxy findEquivalentNet(VarContext vc, Network net) {
		return netEquiv.findEquivalentNet(vc, net);
	}
	public NetNameProxy findEquivalentNetShortingResistors(VarContext vc, Network net) {
		return netEquiv.findEquivalentNetShortingResistors(vc, net);
	}
	public NodableNameProxy findEquivalentNode(VarContext vc, Nodable node) {
		return nodeEquiv.findEquivalent(vc, node);
	}
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
