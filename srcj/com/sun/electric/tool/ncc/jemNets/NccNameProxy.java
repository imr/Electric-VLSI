/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NccNameProxy.java
 *
 * Copyright (c) 2003 Sun Microsystems and Free Software
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
package com.sun.electric.tool.ncc.jemNets;

import java.util.Iterator;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NameProxy;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NetNameProxy;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NodableNameProxy;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.generator.layout.LayoutLib;

/** Stores the information necessary to generate an instance name for a Part 
  * It is the same as HierarchyEnumerator.NameProxy except that it removes
  * a common path prefix from the name. */
public abstract class NccNameProxy {
	private String commonPathPrefix;

	public String toString() {
		String fullName = nameProxy().toString();
		if (commonPathPrefix.length()==0) return fullName;
		LayoutLib.error(!fullName.startsWith(commonPathPrefix), 
                        "common path prefix not found");
		// +1 to skip separator
		return fullName.substring(commonPathPrefix.length()+1);		
	}
	abstract NameProxy nameProxy();
	NccNameProxy(String commonPathPrefix) {
		this.commonPathPrefix = commonPathPrefix;
	}
	public boolean nameIsInTopContext() {
		return nameProxy().getContext()==VarContext.globalContext;
	}
	public Cell leafCell() {return nameProxy().leafCell();}
	public String leafName() {return nameProxy().leafName();}
	public String cellInstPath() {
		return nameProxy().getContext().getInstPath("/");
	}

	public static class PartNameProxy extends NccNameProxy {
		private NodableNameProxy nameProxy;
		NameProxy nameProxy() {return nameProxy;}
		public PartNameProxy(NodableNameProxy nameProxy, 
				             String commonPathPrefix) {
			super(commonPathPrefix);
			this.nameProxy = nameProxy;
		}
	}
	public static class WireNameProxy extends NccNameProxy {
		NetNameProxy nameProxy;
		NameProxy nameProxy() {return nameProxy;}
		public WireNameProxy(NetNameProxy nameProxy, String commonPathPrefix) {
			super(commonPathPrefix);
			this.nameProxy = nameProxy;
		}
		public NetNameProxy getNetNameProxy() {return nameProxy;}
		public Iterator getNetNames() {
			return ((NetNameProxy)nameProxy).leafNames();
		}
	}
}
