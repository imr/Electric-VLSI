/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Cell_.java
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

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Iterator;

public class Cell_
{
	final int id;
	final DatabaseThread thread;
	String name;
	ImmutableNodeInst[] nodes;
	SoftReference contentsRef;
	
	Cell_(int id, ImmutableCell d) {
		assert id >= 0 && d != null;
		DatabaseThread thread = (DatabaseThread)Thread.currentThread();
		this.id = id;
		this.thread = thread;

		// link
		assert thread.cells[id] == null;
		thread.cells[id] = this;
		this.name = d.name;
		this.nodes = (ImmutableNodeInst[])d.nodes.clone();
	}

	void unlink() {
		nodes = null;
		if (contentsRef != null) {
			CellContents contents = (CellContents)contentsRef.get();
			if (contents != null) contents.unlink();
			contentsRef.clear();
			contentsRef = null;
		}
		name = null;
		thread.cells[id] = null;
	}

	public String getName() { checkAlive(); return name; }

	public int getNumNodes() {
		checkAlive();
		CellContents contents = getContents();
		return contents.orderedNodes.size();
	}
	
	public Iterator getNodes() {
		checkAlive();
		CellContents contents = getContents();
		return Collections.unmodifiableCollection(contents.orderedNodes.values()).iterator();
	}
		
	public static Cell_ newInstance(String name) {
		if (!(Thread.currentThread() instanceof DatabaseChangeThread))
			throw new IllegalStateException("Not in DatabaseChangeThread");
		if (name == null)
			throw new IllegalArgumentException("Cell name is null");
		DatabaseChangeThread thread = (DatabaseChangeThread)Thread.currentThread();
		if (thread.orderedCells.get(name) != null)
			throw new IllegalArgumentException("Cell " + name + " exists");

		thread.checkChanging();
		int cellId = thread.allocCellId();
		ImmutableCell icell = ImmutableCell.newInstance(name, null);
		if (thread.cells.length <= cellId) {
			Cell_[] newCells = new Cell_[cellId + 1];
			System.arraycopy(thread.cells, 0, newCells, 0, thread.cells.length);
			thread.cells = newCells;
		}
		Cell_ cell = new Cell_(cellId, icell);
		thread.orderedCells.put(name, cell);
		thread.endChanging();
		return cell;
	}

	public static Cell_ findCell(String name) {
		if (!(Thread.currentThread() instanceof DatabaseThread))
			throw new IllegalStateException("Not in DatabaseThread");
		DatabaseThread thread = (DatabaseThread)Thread.currentThread();
		if (!thread.valid)
			throw new IllegalStateException("database invalid");
		if (name == null) return null;
		return (Cell_)thread.orderedCells.get(name);
	}
	
	public void kill() {
		DatabaseChangeThread thread = this.thread.checkChanging();
		unlink();
		thread.endChanging();
	}
	
	public void setName(String name) {
		DatabaseChangeThread thread = this.thread.checkChanging();
		if (name == null)
			throw new IllegalArgumentException("Cell name is null");
		if (!this.name.equals(name) && thread.orderedCells.get(name) != null)
			throw new IllegalArgumentException("Cell " + name + " exists");
		thread.orderedCells.remove(this.name);
		thread.orderedCells.put(name, this);
		this.name = name;
		thread.endChanging();
	}
	
	public NodeInst_ newNode(Cell_ proto, String name, EPoint anchor) {
		DatabaseChangeThread thread = this.thread.checkChanging();
		if (name == null)
			throw new NullPointerException("node name");
		if (proto == null)
			throw new NullPointerException("proto");
		if (anchor == null)
			throw new NullPointerException("anchor");
		if (proto.thread != thread || proto.nodes == null)
			throw new IllegalArgumentException("proto not alive");
		ImmutableNodeInst inode = new ImmutableNodeInst(proto.id, name, anchor);
		int nodeId = thread.allocNodeId(id);
		CellContents contents = getContents();
		assert nodes.length == contents.nodes.length;
		if (nodeId >= nodes.length) {
			ImmutableNodeInst[] newINodes = new ImmutableNodeInst[nodeId + 1];
			NodeInst_[] newNodes = new NodeInst_[nodeId + 1];
			System.arraycopy(nodes, 0, newINodes, 0, nodes.length);
			System.arraycopy(contents.nodes, 0, newNodes, 0, contents.nodes.length);
			nodes = newINodes;
			contents.nodes = newNodes;
		}
		NodeInst_ node = new NodeInst_(nodeId, contents, inode);
		nodes[nodeId] = inode;
		thread.endChanging();
		return null;
	}
	
	ImmutableCell backup(ImmutableCell oldS) {
		if (oldS == null) return ImmutableCell.newInstance(name, nodes);
		return oldS.withName(name).withNodes(nodes);
	}
	
	void restore(ImmutableCell icell) {
		
	}
	
	void recover(ImmutableCell icell) {
		// clear all ???
		name = icell.name;
		nodes = (ImmutableNodeInst[])icell.nodes.clone();
	}

	private CellContents getContents() {
		if (contentsRef != null) {
			CellContents contents = (CellContents)contentsRef.get();
			if (contents != null) return contents;
		};
		return new CellContents(this);
	}
	
	public boolean isAlive() {
		return nodes != null && thread == Thread.currentThread() && thread.valid;
	}
	
	private void checkAlive() {
		thread.checkRunning();
		if (nodes == null)
			throw new IllegalStateException("Killed cell");
	}
	
	private DatabaseChangeThread checkChanging() {
		DatabaseChangeThread thread = this.thread.checkChanging();
		if (nodes == null)
			throw new IllegalStateException("Killed cell");
		return thread;
	}
		
	void check() {
		assert nodes != null;
		assert name != null;
		if (contentsRef != null) {
			CellContents contents = (CellContents)contentsRef.get();
			if (contents != null) {
				contents.check();
				assert contents.nodes.length == nodes.length;
				for (int i = 0; i < nodes.length; i++) {
					NodeInst_ node = contents.nodes[i];
					assert nodes[i] == (node != null ? node.d : null);
				}
			}
		}
	}

	void checkLinked() {
		assert thread.cells[id] == this;
		thread.checkLinked();
	}
}
