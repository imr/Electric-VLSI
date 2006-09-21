/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: InstancePathToNccContext.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.electric.database.hierarchy.HierarchyEnumerator.NameProxy;
import com.sun.electric.database.variable.VarContext;

/** InstancePathToNccContext maps from a point in the design hierarchy to the
 * NccContext at that point in the hierarchy.
 * <p>
 * InstancePathToNccContext builds a tree of NccContext's that parallels the
 * VarContext tree. It searches the tree by starting at the root and matching
 * instance names to descend the hierarchy until it reaches the desired point in
 * the hierarchy. */
class InstancePathToNccContext implements Serializable {
    static final long serialVersionUID = 0;

	// Tricky: If there are no equivalent objects then root will be null. This
    // happens when we build equivalence tables for Cells with no Parts.
    private NccContext root;
	private Map<VarContext,NccContext> varToNccContext = new HashMap<VarContext,NccContext>();
	
	private NccContext getNccContext(VarContext vc) {
		NccContext nc = varToNccContext.get(vc);
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
	private List<String> instNames(VarContext vc) {
		if (vc==VarContext.globalContext) return new ArrayList<String>();
		List<String> names = instNames(vc.pop());
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
		// Tricky: root is null if there aren't any equivalent objects.
		// For example some Cells don't have any Parts.
		if (root==null) return null;
		List<String> names = instNames(vc);
		NccContext nc = root;
		for (String instNm : names) {
			nc = nc.findChild(instNm);
			if (nc==null)  return null;
		}
		return nc;
	}
}
