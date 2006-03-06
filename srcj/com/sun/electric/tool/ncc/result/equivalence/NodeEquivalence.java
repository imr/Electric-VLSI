package com.sun.electric.tool.ncc.result.equivalence;

import java.io.Serializable;
import java.util.Iterator;

import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NodableNameProxy;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.generator.layout.LayoutLib;

/** Object to map from a Nodable in one design to the 
 * "NCC equivalent" Nodable in the 
 * other design. */
class NodeEquivalence implements Serializable {
    static final long serialVersionUID = 0;

	private final NodableNameProxy[][] equivNodes;
	private final int numDesigns, numNodes;
	private InstancePathToNccContext[] instToNccCtxt;
	/** Cache the index of the last design that satisified the last 
	 * findEquivalent() query? */
	private int lastDesignHit;
	
	private void pr(String s) {System.out.print(s);}

	/** Tricky: Because instToNccCtxt takes so much space, build it 
	 * only on demand */
	private void buildNameTree() {
		if (instToNccCtxt!=null) return; 
		instToNccCtxt = new InstancePathToNccContext[numDesigns];
		for (int i=0; i<numDesigns; i++) {
			LayoutLib.error(equivNodes[i].length!=numNodes,
					        "designs don't have same numbers of nodables?");
			instToNccCtxt[i] = new InstancePathToNccContext(equivNodes[i]); 
		}
	}

	/** @param equivNodes is a NodableNameProxy[][]. NodableNameProxy[d][n] 
	 * gives the nth Nodable of the dth design. NodableNameProxy[a][n] is "NCC
	 * equivalent" to NodableNameProxy[b][n] for all a and b. */
	public NodeEquivalence(NodableNameProxy[][] equivNodes) {
		this.equivNodes = equivNodes;
		numDesigns = equivNodes.length;
		numNodes = equivNodes[0].length;
	}

	private NodableNameProxy findEquivalent(VarContext vc, Nodable node, 
			                                int designIndex) {
		LayoutLib.error(designIndex!=0 && designIndex!=1, 
				        "designIndex must be 0 or 1");
		buildNameTree();
		InstancePathToNccContext nameIndex = instToNccCtxt[designIndex];
		NccContext nc = nameIndex.findNccContext(vc);
		
		if (nc==null) return null;
		if (nc.getCell()!=node.getParent())  return null;
		if (!nc.getContext().equals(vc))  return null;
		for (Iterator<Integer> it=nc.getIndices(); it.hasNext();) {
			int index = it.next().intValue();
			NodableNameProxy prox = equivNodes[designIndex][index];
			if (prox.leafName().equals(node.getName())) {
				int equivDesign = designIndex==0 ? 1 : 0;
				return equivNodes[equivDesign][index];
			}
		}
		return null;
	}
	/** Given a Nodable located at point in the design hierarchy specified by a
	 * VarContext, find the "NCC equivalent" Nodable in the other design.
	 * <p>
	 * This code was copied from NetEquivalence. It depends upon 1) the 
	 * .equals() equality of instance names along the VarContext, 2) the .equals()
	 * equality of the VarContext (which depends upon the == equality of Nodables
	 * along the VarContext), 3) the .equals equality of the names of the Nodable. 
	 * 4) the == equality of the parent Cells of the Nodable
	 * @param vc VarContext specifying a point in the hierarchy.
	 * @param node Nodable located at that point in the hierarchy.
	 * @return the "NCC equivalent" NodableNameProxy or null if no equivalent can
	 * be found. */
	public NodableNameProxy findEquivalent(VarContext vc, Nodable node) {
		NodableNameProxy nnp = findEquivalent(vc, node, lastDesignHit);
		if (nnp!=null) return nnp;
		
		int otherDesign = lastDesignHit==0 ? 1 : 0;
		nnp = findEquivalent(vc, node, otherDesign);
		if (nnp!=null) {lastDesignHit=otherDesign; return nnp;}
		
		return null;
	}
	/** Regression test. Map from every nodable in design 0 to "NCC 
	 * equivalent" nodable in design 1. Map from every nodable in design 1 
	 * to "NCC equivalent" nodable in design 0. 
	 * @return the number of errors. */
	public int regressionTest() {
		LayoutLib.error(numDesigns!=2, "we must have exactly two designs");
		
		int numErrors = 0;
		for (int desNdx=0; desNdx<numDesigns; desNdx++) {
			int otherDesign = desNdx==0 ? 1 : 0;
			for (int nodeNdx=0; nodeNdx<numNodes; nodeNdx++) {
				NodableNameProxy from = equivNodes[desNdx][nodeNdx];
				VarContext fromVc = from.getContext();
				Nodable fromNode = from.getNodable();
				NodableNameProxy to = findEquivalent(fromVc, fromNode);
				
				if (to!=equivNodes[otherDesign][nodeNdx])  numErrors++;
			}
		}
		pr("    Node equivalence regression "+
				         (numErrors==0 ? "passed. " : "failed. "));
		pr(numNodes+" matched Nodables. ");
		if (numErrors!=0) System.out.print(numErrors+" errors.");
		pr("\n");

		// Tricky: because instToNccCtxt takes so much space, delete it 
		// after the regression.
		instToNccCtxt = null;
		
		return numErrors;
	}
}
