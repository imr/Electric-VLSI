/*
 * Created on Nov 11, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.sun.electric.tool.ncc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.electric.database.hierarchy.HierarchyEnumerator.NameProxy;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NetNameProxy;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.generator.layout.LayoutLib;
/**
 * @author rkao
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class NetEquivalence {
	private final NetNameProxy[][] equivNets;
	private final int numDesigns, numNets;
	private NccNameIndex[] nameIndices;
	private boolean nameMatch(NetNameProxy prox, Network net) {
		for (Iterator it=prox.leafNames(); it.hasNext();) {
			String proxNm = (String) it.next();
			if (net.hasName(proxNm)) return true;
		}
		return false;
	}
	public NetEquivalence(NetNameProxy[][] equivNets) {
		this.equivNets = equivNets;
		numDesigns = equivNets.length;
		numNets = equivNets[0].length;
		nameIndices = new NccNameIndex[numDesigns];
		for (int i=0; i<numDesigns; i++) {
			nameIndices[i] = new NccNameIndex(equivNets[i]); 
		}
	}
	public NetNameProxy findEquivalent(VarContext vc, Network net, int designIndex) {
		LayoutLib.error(designIndex!=0 && designIndex!=1, "designIndex must be 0 or 1");
		NccNameIndex nameIndex = nameIndices[designIndex];
		List indices = nameIndex.search(vc);
		for (int i=0; i<indices.size(); i++) {
			NetNameProxy prox = equivNets[designIndex][i];
			if (nameMatch(prox, net)) {
				int equivDesign = designIndex==0 ? 1 : 0;
				return equivNets[equivDesign][i];
			}
		}
		return null;
	}
}

class NccNameIndex {
	private NccContext root;
	private Map varToNccContext = new HashMap();
	private NccContext getNccContext(VarContext vc) {
		NccContext nc = (NccContext) varToNccContext.get(vc);
		if (nc==null) {
			nc = new NccContext(vc);
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
	private void addObjectIndex(NameProxy obj, int i) {
		NccContext nc = getNccContext(obj.getContext());
		nc.addIndex(i);
	}
	/** @return List of instance names from the root to this vc */
	private List instNames(VarContext vc) {
		if (vc==VarContext.globalContext) return new ArrayList();
		List names = instNames(vc.pop());
		names.add(vc.getNodable().getName());
		return names;
	}
	public NccNameIndex(NameProxy[] objects) {
		int numObj = objects.length;
		for (int i=0; i<numObj; i++) {
			addObjectIndex(objects[i], i);
		}
	}
	public List search(VarContext vc) {
		List names = instNames(vc);
		NccContext nc = root;
		for (Iterator it=names.iterator(); it.hasNext();) {
			String instNm = (String) it.next();
			NccContext child = nc.findChild(instNm);
			if (child==null) {
				NodeProto np = nc.getProto();
				System.out.println(
					"Can't find instance named: "+instNm+
					" in NodeProto: "+np.getName()+
					" along path: "+vc.getInstPath("/"));
				return new ArrayList();
			}
		}
		return nc.getIndices();
	}
}

/* a data structure that parallels VarContext. The key difference is that 
 * NccContext has pointers from the root to the leaves while VarContext has
 * pointers from the leaves to the root. */
class NccContext {
	private VarContext context;
	private Map nodableNameToChild = new HashMap();
	private List objectIndices = new ArrayList();
	public NccContext(VarContext vc) {context=vc;}
	public void addChild(NccContext child) {
		String name = child.context.getNodable().getName();
		nodableNameToChild.put(name, child);
	}
	public void addIndex(int i) {
		objectIndices.add(new Integer(i));
	}
	public List getIndices() {return objectIndices;}
	public NccContext findChild(String instNm) {
		return (NccContext) nodableNameToChild.get(instNm);
	}
	public NodeProto getProto() {return context.getNodable().getProto();}
}
