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
package com.sun.electric.tool.ncc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NameProxy;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NetNameProxy;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.generator.layout.LayoutLib;

/** Object to map from a net in one design to the "NCC equivalent" net in the 
 * other design. */
public class NetEquivalence {
	protected final NetNameProxy[][] equivNets;
	private final int numDesigns, numNets;
	private InstancePathToNccContext[] instToIndices;
	/** Cache the index of the last design that satisified the last 
	 * findEquivalent() query? */
	private int lastDesignHit;
	
	private boolean nameMatch(NetNameProxy prox, Network net) {
		for (Iterator it=prox.leafNames(); it.hasNext();) {
			String proxNm = (String) it.next();
			if (net.hasName(proxNm)) return true;
		}
		return false;
	}
	private void pr(String s) {System.out.print(s);}
	/** @param equivNets is a NetNameProxy[][]. NetNameProxy[d][n] gives the 
	 * nth net of the dth design.  NetNameProxy[a][n] is "NCC equivalent" to 
	 * NetNameProxy[b][n] for all a and b.*/
	public NetEquivalence(NetNameProxy[][] equivNets) {
		this.equivNets = equivNets;
		numDesigns = equivNets.length;
		numNets = equivNets[0].length;
		instToIndices = new InstancePathToNccContext[numDesigns];
		for (int i=0; i<numDesigns; i++) {
			LayoutLib.error(equivNets[i].length!=numNets,
					        "designs don't have same numbers of nets?");
			instToIndices[i] = new InstancePathToNccContext(equivNets[i]); 
		}
	}

	private NetNameProxy findEquivalent(VarContext vc, Network net, 
			                           int designIndex) {
		LayoutLib.error(designIndex!=0 && designIndex!=1, 
				        "designIndex must be 0 or 1");
		InstancePathToNccContext nameIndex = instToIndices[designIndex];
		NccContext nc = nameIndex.findNccContext(vc);
		
		if (nc==null) return null;
		if (nc.getCell()!=net.getParent())  return null;
		if (!nc.getContext().equals(vc))  return null;
		for (Iterator it=nc.getIndices(); it.hasNext();) {
			int index = ((Integer)it.next()).intValue();
			NetNameProxy prox = equivNets[designIndex][index];
			if (nameMatch(prox, net)) {
				int equivDesign = designIndex==0 ? 1 : 0;
				return equivNets[equivDesign][index];
			}
		}
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
	public NetNameProxy findEquivalent(VarContext vc, Network net) {
		NetNameProxy nnp = findEquivalent(vc, net, lastDesignHit);
		if (nnp!=null) return nnp;
		
		int otherDesign = lastDesignHit==0 ? 1 : 0;
		nnp = findEquivalent(vc, net, otherDesign);
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
				NetNameProxy to = findEquivalent(fromVc, fromNet);
				
				if (to!=equivNets[otherDesign][netNdx])  numErrors++;
			}
		}
		pr("  Net equivalence regression "+
				         (numErrors==0 ? "passed. " : "failed. "));
		pr(numNets+" matched Networks. ");
		if (numErrors!=0) System.out.print(numErrors+" errors.");
		pr("\n");

		return numErrors;
	}
}

/** InstancePathToNccContext maps from a point in the design hierarchy to the
 * NccContext at that point in the hierarchy.
 * <p>
 * InstancePathToNccContext builds a tree of NccContext's that parallels the
 * VarContext tree. It searches the tree by starting at the root and matching
 * instance names to descend the hierarchy until it reaches the desired point in
 * the hierarchy. */
class InstancePathToNccContext {
	private NccContext root;
	private Map varToNccContext = new HashMap();
	private NccContext getNccContext(VarContext vc) {
		NccContext nc = (NccContext) varToNccContext.get(vc);
		if (nc==null) {
			nc = new NccContext(vc);
			varToNccContext.put(vc, nc);
			if (vc==VarContext.globalContext) {
				root = nc;
			} else {
				// snap a link from parent to child
				NccContext parent = getNccContext(vc.pop());
				parent.addChild(nc);
			}
		}
		return nc;
	}
	private void addNameProxyIndex(NameProxy np, int i) {
		NccContext nc = getNccContext(np.getContext());
		nc.addNameProxyIndex(np, i);
	}
	/** @return List of instance names from the root to vc. */
	private List instNames(VarContext vc) {
		if (vc==VarContext.globalContext) return new ArrayList();
		List names = instNames(vc.pop());
		names.add(vc.getNodable().getName());
		return names;
	}
	/** Build a data structure to map from an instance path to the indices of the
	 * NameProxys located at that point in the hierarchy. I map to indicies
	 * rather than the NameProxys because the indices allow the users of this
	 * object to find the "NCC equivalent" NameProxys in all the other designs.
	 * @param objects array of NameProxy's. The search operation returns indices
	 * into this array. */
	public InstancePathToNccContext(NameProxy[] objects) {
		int numObj = objects.length;
		for (int i=0; i<numObj; i++)  addNameProxyIndex(objects[i], i);
	}
	/** @return NccContext associated with VarContext vc. If instance path
	 * specified by vc isn't found then return null. */
	public NccContext findNccContext(VarContext vc) {
		List names = instNames(vc);
		NccContext nc = root;
		for (Iterator it=names.iterator(); it.hasNext();) {
			String instNm = (String) it.next();
			nc = nc.findChild(instNm);
			if (nc==null)  return null;
		}
		return nc;
	}
}

/* A data structure that parallels VarContext. A key difference is that 
 * NccContext has pointers from the root to the leaves while VarContext has
 * pointers from the leaves to the root. Also, the NccContext holds the indices
 * of NameProxy's at that point in the design hierarchy. */
class NccContext {
	private VarContext context;
	/** Parent of all NameProxy's at this point in the design hierarchy */ 
	private Cell cell;
	private Map nodableNameToChild = new HashMap();
	private Set objectIndices = new HashSet();
	
	public NccContext(VarContext vc) {context=vc;}
	public void addChild(NccContext child) {
		String name = child.context.getNodable().getName();
		LayoutLib.error(nodableNameToChild.containsKey(name), 
				        "2 nodables with same name?");
		nodableNameToChild.put(name, child);
	}
	public void addNameProxyIndex(NameProxy np, int i) {
		Integer bi = new Integer(i);
		LayoutLib.error(objectIndices.contains(bi),
				        "duplicate index?");
		objectIndices.add(bi);
		// Check invariant: All NameProxy's in the same NccContext are
		// contained by the same parent Cell
		LayoutLib.error(cell!=null && cell!=np.leafCell(),
				        "NameProxy's in NccContext don't have same parent");
		cell = np.leafCell();
		LayoutLib.error(cell==null, "NameProxy with no parent Cell?");
	}
	public Iterator getIndices() {return objectIndices.iterator();}
	public NccContext findChild(String instNm) {
		return (NccContext) nodableNameToChild.get(instNm);
	}
	/** @return the VarContext */
	public VarContext getContext() {return context;}
	/** @return the parent Cell of all NameProxy's at this point in the 
	 * design hierarchy.  If there aren't any NameProxy's here then return
	 * null. */
	public Cell getCell() {return cell;}
}
