/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DatabaseThread.java
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

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class DatabaseThread extends Thread
{
	Cell_[] cells = {};
	TreeMap/*<String,Cell_>*/ orderedCells = new TreeMap/*<String,Cell_>*/();
	boolean valid = true;
	
	public Snapshot backup(Snapshot oldS) {
		if (!valid)
			throw new IllegalStateException("database invalid");
		if (oldS == null) oldS = Snapshot.EMPTY;
		int length = cells.length;
		while (length > 0 && cells[length - 1] == null) length--;
		ImmutableCell[] icells = new ImmutableCell[length];
		for (int i = 0; i < length; i++)
			icells[i] = (cells[i] != null ? cells[i].backup(oldS.getCellById(i)) : null);
		return oldS.withCells(icells);
	}

	void restore(Snapshot s) {
		valid = false;
		if (cells.length <= s.maxCellId()) {
			Cell_[] newCells = new Cell_[s.maxCellId() + 1];
			System.arraycopy(cells, 0, newCells, 0, cells.length);
			cells = newCells;
		}
		boolean changeNames = true;
		for (int i = 0; i < cells.length; i++) {
			Cell_ oldCell = cells[i];
			ImmutableCell icell = s.getCellById(i);
			if (oldCell == null) {
				// created
				changeNames = true;
				new Cell_(i, icell);
			} else if (icell == null) {
				// deleted
				changeNames = true;
				oldCell.unlink();
			} else {
				// updated
				if (oldCell.name != icell.name) changeNames = true;
				oldCell.restore(icell);
			}
		}
		if (changeNames) {
			orderedCells.clear();
			for (int i = 0; i < cells.length; i++) {
				Cell_ cell = cells[i];
				if (cell != null) orderedCells.put(cell.name, cell);
			}
		}
		for (int i = 0; i < cells.length; i++) {
			Cell_ cell = cells[i];
			if (cell == null || cell.contentsRef == null) continue;
			CellContents contents = (CellContents)cell.contentsRef.get();
			if (contents != null) contents.restore();
		}
		valid = true;
	}
	
	void clear() {
		orderedCells.clear();
		for (int i = 0; i < cells.length; i++)
			if (cells[i] != null) cells[i].unlink();
	}

	public void check() {
		// check proper tree
		for (int i = 0; i < cells.length; i++) {
			Cell_ cell = cells[i];
			if (cell == null) continue;
			assert cell.thread == this;
			assert cell.id == i;
			if (cell.contentsRef == null) continue;
			CellContents contents = (CellContents)cell.contentsRef.get();
			if (contents == null) continue;
			assert contents.cell == cell;
			for (int j = 0; j < contents.nodes.length; j++) {
				NodeInst_ node = contents.nodes[j];
				if (node == null) continue;
				assert node.parent == contents;
				assert node.id == j;
			}
		}
		
		if (!valid) return;
		
		// check cell names
		for (int i = 0; i < cells.length; i++) {
			Cell_ cell = cells[i];
			if (cell == null) continue;
			cell.check();
			assert orderedCells.get(cell.name) == cell;
		}
		for (Iterator it = orderedCells.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry e = (Map.Entry)it.next();
			String name = (String)e.getKey();
			Cell_ cell = (Cell_)e.getValue();
			assert name == cell.name;
			assert cell == cells[cell.id];
		}
	}
	
	void checkLinked() {
		assert Thread.currentThread() == this;
	}
	
	DatabaseChangeThread checkChanging() {
		if (this != Thread.currentThread())
			throw new IllegalStateException("Other thread");
		throw new IllegalStateException("Readonly thread");
	}
	
	void checkRunning() {
		if (this != Thread.currentThread())
			throw new IllegalStateException("Other thread");
		if (!valid)
			throw new IllegalStateException("database invalid");
	}
}
