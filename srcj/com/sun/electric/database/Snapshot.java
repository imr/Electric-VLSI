/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Snapshot.java
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

import com.sun.electric.database.geometry.EPoint;

import java.util.ConcurrentModificationException;
import java.util.HashSet;

/**
 * Immutable class Snapshot represents a snapshot of Electric database.
 * It contains cells indexed by cellId.
 * Cells must have unique names. Node in cells must have valid protoIds.
 */
public class Snapshot
{
	/** Array which maps cellIds to ImmutableCells. It may contain may nulls. */
	private final ImmutableCell[] cells;

	private static final ImmutableCell[] NULL_CELLS = {};

	/** EMPTY Snapshot without any cells. */
	public static final Snapshot EMPTY = new Snapshot(NULL_CELLS);
	
	Snapshot(ImmutableCell[] cells) {
		this.cells = cells;
		check();
	}

	/**
	 * Returns maximal cellId of cell in this Snapshot, or -1.
	 * @return maximal cellId of cell in this Snapshot, or -1.
	 */
	public int maxCellId() {
		return cells.length - 1;
	}

	/**
	 * Returns cell with specified cellId, or null.
	 * @param cellId specified cellId.
	 * @return cell with specified cellId, or null.
	 * @throws ArrayIndexOutOfBoundsException, if cellId is negative.
	 */
	public ImmutableCell getCellById(int cellId) {
		return cellId < cells.length ? cells[cellId] : null;
	}

	/**
	 * Returns cell with specified cellId.
	 * @param cellId specified cellId.
	 * @return cell with specified cellId.
	 * @throws ArrayIndexOutOfBoundsException, if there is no cell with such cellId.
	 */
	private ImmutableCell getCellByIdSurely(int cellId) {
		ImmutableCell cell = cells[cellId];
		if (cell == null) throw new ArrayIndexOutOfBoundsException(cellId);
		return cell;
	}

	/**
	 * Returns cellId of cell with with specified name, or -1.
	 * @param name specified cell name, or null.
	 * @return cellId of cell with with specified name, or -1.
	 */
	public int findCellId(String name) {
		if (name != null) {
			for (int i = 0; i < cells.length; i++) {
				ImmutableCell cell = cells[i];
				if (cell != null && cell.name.equals(name)) return i;
			}
		}
		return -1;
	}

	/**
	 * Returns new Snapshot object.
	 * @param cells array of cells, may contain nulls.
	 * @return new Snapshot object.
	 * @throws IllegalArgumentException if cell names have duplicates.
	 * @throws ConcurrentModificationException if cells array was modified during construction.
	 */
	public static Snapshot newInstance(ImmutableCell[] cells) {
		ImmutableCell[] newCells = clone(cells, NULL_CELLS);
		checkNames(newCells);
		checkProto(newCells);
		return new Snapshot(newCells);
	}

	/**
	 * Returns Snapshot which differs from this Snapshot by cells.
	 * Old snapshot is used for faster invariant check.
	 * @param cells new nodes array or null.
	 * @return Snapshot which differs from this Snapshot by cells.
	 * @throws IllegalArgumentException if cells have duplicate names.
	 * @throws ArrayIndexOutOfBoundsException if some node has bad protoId.
	 */
	public Snapshot withCells(ImmutableCell[] cells) {
		ImmutableCell[] newCells = clone(cells, this.cells);
		if (newCells == this.cells) return this;
		boolean checkNames = false;
		boolean checkProtos = false;
		for (int i = 0; i < newCells.length; i++) {
			ImmutableCell oldCell = getCellById(i);
			ImmutableCell newCell = newCells[i];
			if (oldCell == newCell) continue;
			boolean checkProto = false;
			if (oldCell == null) {
				// created
				checkNames = true;
				checkProto = true;
			} else if (newCell == null) {
				// deleted
				checkProtos = true;
			} else {
				// updated
				if (!newCell.name.equals(oldCell.name))	checkNames = true;
				if (newCell.nodes != oldCell.nodes) checkProto = true;
			}
			if (checkProto && !checkProtos) newCell.checkProto(newCells);
		}
		if (checkNames) checkNames(newCells);
		if (checkProtos) checkProto(newCells);
		return new Snapshot(newCells);
	}

	/**
	 * Returns Snapshot which differs from this Snapshot by cell with specified cellId.
	 * @param cellId cell id.
	 * @param cell new cell with specified cellId, or null to delete cell.
	 * @return Snapshot which differs from this Snapshot by cell.
	 * @throws ArrayIndexOutOfBoundsException if cellId is negative or if some node has bad protoId.
	 * @throws IllegalArgumentException if cell with such name exists in a cell.
	 */
	public Snapshot withCell(int cellId, ImmutableCell cell) {
		ImmutableCell oldCell = getCellById(cellId);
		if (cell == oldCell) return this;
		int length = cells.length;
		boolean checkName = false;
		boolean checkProtos = false;
		boolean checkProto = false;
		if (oldCell == null) {
			// created
			checkName = true;
			checkProto = true;
			if (cellId >= length) length = cellId + 1;
		} else if (cell == null) {
			// deleted
			checkProtos = true;
			if (cellId == length - 1)
				do { length--; } while (length > 0 && cells[length - 1] == null);
		} else {
			// updated
			if (!cell.name.equals(oldCell.name)) checkName = true;
			if (cell.nodes != oldCell.nodes) checkProto = true;
		}
		if (checkName && findCellId(cell.name) >= 0)
			throw new IllegalArgumentException("cell " + cell.name + " exists");
		ImmutableCell[] newCells = new ImmutableCell[length];
		System.arraycopy(cells, 0, newCells, 0, Math.min(cells.length, length));
		if (cellId < length) newCells[cellId] = cell;
		if (checkProtos)
			checkProto(newCells);
		else if (checkProto)
			cell.checkProto(newCells);
		return new Snapshot(newCells);
	}

	/**
	 * Returns Snapshot which differs from this Snapshot by name
	 * of cell with specified cellId.
	 * @param cellId cell id.
	 * @param name new cell name.
	 * @return Snapshot which differs from this Snapshot by name of cell.
	 * @throws ArrayIndexOutOfBoundsException if there is no cell with this cellId.
	 * @throws NullPointerException if name is null.
	 * @throws IllegalArgumentException if cell with such name exists in database.
	 */
	public Snapshot withCellName(int cellId, String name) {
		return withCell(cellId, getCellByIdSurely(cellId).withName(name));
	}

	/**
	 * Returns Snapshot which differs from this Snapshot by node with
	 * specified cellId and nodeId.
	 * @param cellId cell id.
	 * @param nodeId node id.
	 * @param node new node for these ids, or null to delete node.
	 * @return Snapshot which differs from this Snapshot by node
	 * @throws ArrayIndexOutOfBoundsException if there is no node with this cellId and nodeId or
	 *         new node has bad protoId.
	 * @throws IllegalArgumentException if node with new name exists in the cell.
	 */
	public Snapshot withNode(int cellId, int nodeId, ImmutableNodeInst node) {
		return withCell(cellId, getCellByIdSurely(cellId).withNode(nodeId, node));
	}

	/**
	 * Returns Snapshot which differs from this Snapshot by protoId
	 * of node with specified cellId and nodeId.
	 * @param cellId cell id.
	 * @param nodeId node id.
	 * @param protoId new node protoId.
	 * @return Snapshot which differs from this Snapshot by name of node.
	 * @throws ArrayIndexOutOfBoundsException if there is no node with this cellId and nodeId, or protoId is negative.
	 * @throws IllegalArgumentException if cell with such name exists in database.
	 */
	public Snapshot withNodeProto(int cellId, int nodeId, int protoId) {
		return withCell(cellId, getCellByIdSurely(cellId).withNodeProto(nodeId, protoId));
	}
	/**
	 * Returns Snapshot which differs from this Snapshot by name
	 * of node with specified cellId and nodeId.
	 * @param cellId cell id.
	 * @param nodeId node id.
	 * @param name new node name.
	 * @return Snapshot which differs from this Snapshot by name of node.
	 * @throws ArrayIndexOutOfBoundsException if there is no node with this cellId and nodeId.
	 * @throws NullPointerException if name is null.
	 * @throws IllegalArgumentException if cell with such name exists in database.
	 */
	public Snapshot withNodeName(int cellId, int nodeId, String name) {
		return withCell(cellId, getCellByIdSurely(cellId).withNodeName(nodeId, name));
	}

	/**
	 * Returns Snapshot which differs from this Snapshot by anchor point
	 * of node with specified cellId and nodeOd.
	 * @param cellId cell id.
	 * @param nodeId node id.
	 * @param anchor new node anchor point.
	 * @return Snapshot which differs from this Snapshot by anchor of node.
	 * @throws ArrayIndexOutOfBoundsException if there is no node with this cellId and nodeId.
	 * @throws NullPointerException if anchor is null.
	 */
	public Snapshot withNodeAnchor(int cellId, int nodeId, EPoint anchor) {
		return withCell(cellId, getCellByIdSurely(cellId).withNodeAnchor(nodeId, anchor));
	}

	/**
	 * Make a defensive copy of array of cells. Array with trailing nulls is truncated.
	 * If new array has the same entries as old, old is returned.
	 * @param cells array to copy, or null
	 * @param oldCells old array which may be returned if it has the same entries as new.
	 * @throws ConcurrentModificationException if cells array was modified during construction.
	 */
	private static ImmutableCell[] clone(ImmutableCell[] cells, ImmutableCell[] oldCells) {
		ImmutableCell[] newCells = NULL_CELLS;
		if (cells != null) {
			int length = cells.length;
			while (length > 0 && cells[length - 1] == null) length--;
			if (length == oldCells.length) {
				int i;
				for (i = length - 1; i >= 0; i--)
					if (cells[i] != oldCells[i]) break;
				if (i < 0) return oldCells;
			}
			if (length > 0) {
				newCells = new ImmutableCell[length];
				System.arraycopy(cells, 0, newCells, 0, length);
				if (newCells[length - 1] == null) throw new ConcurrentModificationException();
			}
		}
		return newCells;
	}

	/**
	 * Checks invariant of this Snapshot.
	 * @throws Throwable if invariant is broken.
	 */
	public void check() {
		assert cells != null;
		assert cells.length == 0 || cells[cells.length - 1] != null;
		for (int i = 0; i < cells.length; i++) {
			ImmutableCell cell = cells[i];
			if (cell != null) cell.check();
		}
		checkNames(cells);
		checkProto(cells);
	}

	/**
	 * Checks that cells have not duplicate names.
	 * @throws IllegalArgumentException if cells have duplicate names.
	 */
	private static void checkNames(ImmutableCell[] cells) {
		HashSet/*<String>*/ names = new HashSet/*<String>*/();
		for (int i = 0; i < cells.length; i++) {
			ImmutableCell cell = cells[i];
			if (cell == null) continue;
			if (names.contains(cell.name))
				throw new IllegalArgumentException("Duplicate cell " + cell.name);
			names.add(cell.name);
		}
	}

	/**
	 * Checks that protoIds of nodes of cells of this Snapshot are contained in cells.
	 * @param cells array with cells, may contain nulls.
	 * @throws ArrayOutOfBoundsException if protoId of some node is not contained.
	 */
	private static void checkProto(ImmutableCell[] cells) {
		for (int i = 0; i < cells.length; i++) {
			ImmutableCell cell = cells[i];
			if (cell != null) cell.checkProto(cells);
		}
	}

}
