/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NodeInst_.java
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
package com.sun.electric.database;

import com.sun.electric.database.geometry.EPoint;

public class NodeInst_
{
	final int id;
	public final CellContents parent;
	Cell_ proto;
	ImmutableNodeInst d;
	
	NodeInst_(int id, CellContents parent, ImmutableNodeInst d) {
		assert d != null && parent.nodes[id] == null;
		this.id = id;
		this.parent = parent;
		parent.nodes[id] = this;
		this.d = d;

		this.proto = ((DatabaseThread)Thread.currentThread()).cells[d.protoId];
		assert this.proto != null;
	}

	void unlink() {
		assert parent.nodes[id] == this;
		d = null;
		parent.nodes[id] = null;
	}

	public String getName() { checkAlive(); return d.name; }
	public EPoint getAnchor() { checkAlive(); return d.anchor; }

	public void rename(String name) {
		DatabaseChangeThread thread = parent.cell.thread.checkChanging();
		if (name == null)
			throw new IllegalArgumentException("Node name is null");
		if (parent.orderedNodes.containsKey(name) && !name.equals(d.name))
			throw new IllegalArgumentException("Node " + name + " exists");
		parent.orderedNodes.remove(d.name);
		parent.orderedNodes.put(name, this);
		parent.cell.nodes[id] = d = d.withName(name);
		thread.endChanging();
	}

	public void setAnchor(EPoint anchor) {
		DatabaseChangeThread thread = parent.cell.thread.checkChanging();
		parent.cell.nodes[id] = d = d.withAnchor(anchor);
		thread.endChanging();
	}

	public void kill() {
		DatabaseChangeThread thread = parent.cell.thread.checkChanging();
		parent.orderedNodes.remove(d.name);
		unlink();
		parent.cell.nodes[id] = null;
		thread.endChanging();
	}

	public boolean isAlive() {
		return d != null && parent.cell.thread == Thread.currentThread() && parent.cell.thread.valid;
	}

	void checkAlive() {
		parent.cell.thread.checkRunning();
		if (d == null)
			throw new IllegalStateException("Not alive");
	}

	void checkChanging() {
		if (parent.cell.thread != Thread.currentThread())
			throw new IllegalStateException("Other thread");
		if (!(parent.cell.thread instanceof DatabaseChangeThread))
			throw new IllegalStateException("Readonly thread");
		if (d == null)
			throw new IllegalStateException("Not alive");
	}

	void restore(ImmutableNodeInst d) {
		if (d.protoId != proto.id)
			proto = parent.cell.thread.cells[d.protoId];
		assert proto != null;
		this.d = d;
	}

	void check() {
		assert d != null;
		assert proto == parent.cell.thread.cells[proto.id];
		assert proto != null;
		assert proto.id == d.protoId;
	}

	void checkLinked() {
		assert parent.nodes[id] == this;
		parent.checkLinked();
	}
}
