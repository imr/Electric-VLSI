/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableCell.java
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
 * Immutable class ImmutableCell represents a cell.
 * It contains the name and node instances index by nodeId.
 * Node instances must have unique names.
 */
public class ImmutableCell
{
	/** Node name. */
	public final String name;
	/** Array which maps nodeIds to ImmutableNodeInstances. It may contain may nulls. */
	final  ImmutableNodeInst[] nodes;

	ImmutableCell(String name, ImmutableNodeInst[] nodes) {
		this.name = name;
		this.nodes = nodes;
		check();
	}

	/**
	 * Returns new ImmutableCell object.
	 * @param name cell name.
	 * @param nodes array of nodes, may contain nulls.
	 * @return new Cell object.
	 * @throws NullPointerException if name is null.
	 * @throws IllegalArgumentException if node names have duplicates.
	 * @throws ConcurrentModificationException if nodes array was modified during construction.
	 */
	public static ImmutableCell newInstance(String name, ImmutableNodeInst[] nodes) {
		if (name == null) throw new NullPointerException("name");
		ImmutableNodeInst[] newNodes = {};
		if (nodes != null) {
			int length = nodes.length;
			while (length > 0 && nodes[length - 1] == null)
				length--;
			if (length > 0) {
				newNodes = new ImmutableNodeInst[length];
				System.arraycopy(nodes, 0, newNodes, 0, length);
				if (newNodes[length - 1] == null) throw new ConcurrentModificationException();
			}
		}
		checkNames(newNodes);
		return new ImmutableCell(name, newNodes);
	}

	/**
	 * Returns maximal nodeId of node instance in this ImmutableCell, or -1.
	 * @return maximal nodeId of node instance in this ImmutableCell, or -1.
	 */
	public int maxNodeId() {
		return nodes.length - 1;
	}

	/**
	 * Returns node with specified nodeId, or null.
	 * @return node with specified nodeId, or null.
	 * @throws ArrayIndexOutOfBoundsError, if nodeId is negative.
	 */
	public ImmutableNodeInst getNodeById(int nodeId) {
		return nodeId < nodes.length ? nodes[nodeId] : null;
	}

	/**
	 * Returns nodeId of node with with specified name, or -1.
	 * @param name specified node name, or null.
	 * @return nodeId of node with with specified name, or -1.
	 */
	public int findNodeId(String name) {
		if (name != null) {
			for (int i = 0; i < nodes.length; i++) {
				ImmutableNodeInst node = nodes[i];
				if (node != null && node.name.equals(name)) return i;
			}
		}
		return -1;
	}

	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by name.
	 * @param name cell name.
	 * @return ImmutableCell which differs from this ImmutableCell by name.
	 * @throws NullPointerException if name is null.
	 */
	public ImmutableCell withName(String name) {
		if (name == null) throw new NullPointerException("name");
		if (this.name == name) return this;
		return new ImmutableCell(name, this.nodes);
	}

	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by node with specified nodeId.
	 * @param nodeId node id.
	 * @param node new node for specified nodeId, or null to delete node.
	 * @return ImmutableCell which differs from this ImmutableCell by cell.
	 * @throws ArrayIndexOutOfBoundsException if nodeId is negative
	 * @throws IllegalArgumentException if node with such name exists in a cell.
	 */
	public ImmutableCell withNode(int nodeId, ImmutableNodeInst node) {
		if (nodeId < 0) throw new ArrayIndexOutOfBoundsException(nodeId);
		int length = nodes.length;
		if (node != null) {
			if (nodeId < length && nodes[nodeId] != null && nodes[nodeId].name.equals(node.name)) {
				if (nodes[nodeId] == node) return this;
			} else if (findNodeId(node.name) >= 0) {
				throw new IllegalArgumentException("node " + node.name + " exists");
			}
			if (nodeId >= length) length = nodeId + 1;
		} else {
			if (nodeId >= length || nodes[nodeId] == null) return this;
			if (nodeId == length - 1 && node == null) length--;
			while (length > 0 && nodes[length - 1] == null)
				length--;
		}
		return withNode(nodeId, node, length);
	}

	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by name
	 * of node with specified nodeId.
	 * @param nodeId node id.
	 * @param name new node name.
	 * @return ImmutableCell which differs from this ImmutableCell by name of node.
	 * @throws ArrayIndexOutOfBoundsException if there is no node with this nodeId.
	 * @throws NullPointerException if name is null.
	 * @throws IllegalArgumentException if node with such name exists in a cell.
	 */
	public ImmutableCell withNodeName(int nodeId, String name) {
		ImmutableNodeInst node = nodes[nodeId];
		if (node == null) throw new ArrayIndexOutOfBoundsException(nodeId);
		if (node.name.equals(name)) {
			if (node.name == name) return this;
		} else if (findNodeId(name) >= 0) {
			throw new IllegalArgumentException("node " + node.name + " exists");
		}
		return withNode(nodeId, node.withName(name), nodes.length);
	}

	/**
	 * Returns ImmutableCell which differs from this ImmutableCell by anchor point
	 * of node with specified nodeId.
	 * @param nodeId node id.
	 * @param anchor new anchor point
	 * @return ImmutableCell which differs from this ImmutableCell by anchor of node.
	 * @throws ArrayIndexOutOfBoundsException if there is no node with this nodeId.
	 * @throws NullPointerException if anchr is null.
	 */
	public ImmutableCell withNodeAnchor(int nodeId, EPoint anchor) {
		ImmutableNodeInst oldNode = nodes[nodeId];
		if (oldNode == null) throw new ArrayIndexOutOfBoundsException(nodeId);
		ImmutableNodeInst newNode = oldNode.withAnchor(anchor);
		if (newNode == oldNode) return this;
		return withNode(nodeId, newNode, nodes.length);
	}

	private ImmutableCell withNode(int nodeId, ImmutableNodeInst node, int nodesLength) {
		ImmutableNodeInst[] newNodes = new ImmutableNodeInst[nodesLength];
		System.arraycopy(nodes, 0, newNodes, 0, Math.min(nodes.length, nodesLength));
		if (nodeId < nodesLength) newNodes[nodeId] = node;
		return new ImmutableCell(this.name, newNodes);
	}

	/**
	 * Checks invariant of this ImmutableCell.
	 * @throws Throwable if invariant is broken.
	 */
	public void check() {
		assert name != null && nodes != null;
		assert nodes.length == 0 || nodes[nodes.length - 1] != null;
		for (int i = 0; i < nodes.length; i++) {
			ImmutableNodeInst node = nodes[i];
			if (node != null) node.check();
		}
		checkNames(nodes);
	}

	/**
	 * Checks that nodes have not duplicate names.
	 * @throws IllegalArgumentException if nodes have duplicate names.
	 */
	private static void checkNames(ImmutableNodeInst[] nodes) {
		HashSet/*<String>*/ names = new HashSet/*<String>*/();
		for (int i = 0; i < nodes.length; i++) {
			ImmutableNodeInst node = nodes[i];
			if (node == null) continue;
			if (names.contains(node.name))
				throw new IllegalArgumentException("Duplicate node " + node.name);
			names.add(node.name);
		}
	}

	/**
	 * Checks that protoIds of nodes of this ImmutableCell are contained in cells.
	 * @param cells array with cells, may contain nulls.
	 * @throws ArrayOutOfBoundsException if protoId of some node is not contained.
	 */
	void checkProto(ImmutableCell[] cells) {
		for (int i = 0; i < nodes.length; i++) {
			ImmutableNodeInst node = nodes[i];
			if (node != null) node.checkProto(cells);
		}
	}
}
