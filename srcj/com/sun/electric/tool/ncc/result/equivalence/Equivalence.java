package com.sun.electric.tool.ncc.result.equivalence;

import java.io.Serializable;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NetNameProxy;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NodableNameProxy;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.variable.VarContext;
import java.util.HashSet;
import java.util.Set;

public class Equivalence implements Serializable {
    static final long serialVersionUID = 0;

	private final NetEquivalence netEquiv;
	private final NodeEquivalence nodeEquiv;
	
	private void prln(String s) {System.out.println(s);}
    
	public Equivalence(NetNameProxy[][] equivNets,
			           NodableNameProxy[][] equivNodes) {
		netEquiv = new NetEquivalence(equivNets);
		nodeEquiv = new NodeEquivalence(equivNodes);
	}
	public NetNameProxy findEquivalentNet(VarContext vc, Network net) {
		return netEquiv.findEquivalentNet(vc, net);
	}
	public NodableNameProxy findEquivalentNode(VarContext vc, Nodable node) {
		return nodeEquiv.findEquivalent(vc, node);
	}
	public int regressionTest(Cell cell0, Cell cell1) {
		prln("  Equivalence regression for: "+cell0.describe(false)+
			 " and "+cell1.describe(false));
		return netEquiv.regressionTest() +
			   nodeEquiv.regressionTest();
	}
}
