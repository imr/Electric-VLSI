/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellContents.java
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
package com.sun.electric.database;

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

class CellContents
{
	final Cell_ cell;
	NodeInst_[] nodes;
	final TreeMap/*<String,NodeInst_>*/ orderedNodes = new TreeMap/*<String,NodeInst_>*/();

	CellContents(Cell_ cell) {
		assert cell.contentsRef == null;
		this.cell = cell;
		cell.contentsRef = new SoftReference(this);
		nodes = new NodeInst_[cell.nodes.length];
 		restore();
	}

	void unlink() {
		assert nodes != null;
		orderedNodes.clear();
		for (int i = 0; i < nodes.length; i++)
			if (nodes[i] != null) nodes[i].unlink();

		assert cell.contentsRef.get() == this;
		nodes = null;
		cell.contentsRef.clear();
		cell.contentsRef = null;
	}
	
	void restore() {
		ImmutableNodeInst[] inodes = cell.nodes;
		if (nodes.length < inodes.length) {
			NodeInst_[] newNodes = new NodeInst_[inodes.length];
			System.arraycopy(nodes, 0, newNodes, 0, nodes.length);
			nodes = newNodes;
		}
		boolean orderChanged = false;
		for (int i = 0; i < nodes.length; i++) {
			ImmutableNodeInst ini = i < inodes.length ? inodes[i] : null;
			NodeInst_ ni = nodes[i];
			if (ini != null && ni == null) {
				orderChanged = true;
				nodes[i] = new NodeInst_(i, this, ini);
			} else if (ini != null && ni != null) {
				if (ini == ni.d) continue;
				if (ini.name != ni.d.name) orderChanged = true;
				ni.restore(ini);
			} else if (ini == null && ni != null) {
				orderChanged = true;
				ni.unlink();
			}
		}
		if (orderChanged) {
			orderedNodes.clear();
			for (int i = 0; i < nodes.length; i++)
				orderedNodes.put(nodes[i].d.name, nodes[i]);
		}
	}

	void check() {
		assert nodes != null;
		for (int i = 0; i < nodes.length; i++) {
			NodeInst_ node = nodes[i];
			if (node == null) continue;
			node.check();
			assert orderedNodes.get(node.d.name) == node;
		}
		for (Iterator it = orderedNodes.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry e = (Map.Entry)it.next();
			String name = (String)e.getKey();
			NodeInst_ ni = (NodeInst_)e.getValue();
			assert nodes[ni.id] == ni;
		}
	}

	void checkLinked() {
		assert cell.contentsRef.get() == this;
		cell.checkLinked();
	}
}
