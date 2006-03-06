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
    
    private void count(NetNameProxy[][] equivNets, NodableNameProxy[][] equivNodes) {
        prln("  number of EquivNets: "+ equivNets[0].length);
        prln("  number of EquivNodes: "+ equivNodes[0].length);
        Set<Network> networks = new HashSet<Network>();
        Set<Nodable> nodes = new HashSet<Nodable>();
        for (int i=0; i<2; i++) {
            for (int j=0; j<equivNets[i].length; j++)  networks.add(equivNets[i][j].getNet());
            for (int j=0; j<equivNodes[i].length; j++)  nodes.add(equivNodes[i][j].getNodable());
        }
        prln("  number of Networks: "+networks.size());
        prln("  number of Nodables: "+nodes.size());
    }
	
	public Equivalence(NetNameProxy[][] equivNets,
			           NodableNameProxy[][] equivNodes) {
        //count(equivNets, equivNodes);
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
