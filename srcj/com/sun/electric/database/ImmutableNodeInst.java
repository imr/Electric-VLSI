/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableNodeInst.java
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

/**
 * Immutable class ImmutableNodeInst represents a node instance.
 * It contains the id of prototype cell, the name, the anchor point.
 * 
 * @promise "requiresColor DBChanger;" for with*(**) | newInstance(**)
 * @promise "requiresColor (DBChanger | DBExaminer | AWT);" for check()
 */
public class ImmutableNodeInst
{
	/** Prototype cell id. */		public final int protoId;
	/** Node name. */				public final String name;
	/** Node anchor point. */		public final EPoint anchor;

	ImmutableNodeInst(int protoId, String name, EPoint anchor) {
		this.protoId = protoId;
		this.name = name;
		this.anchor = anchor;
		check();
	}

	/**
	 * Returns new ImmutableNodeInst object.
	 * @param protoId Prototype cell id.
	 * @param name node name.
	 * @param anchor node anchor point.
	 * @return new ImmutableNodeInst object.
	 * @throws ArrayIndexOutOfBoundsException if protoId is negative.
	 * @throws NullPointerException if name or anchor is null.
	 */
	public static ImmutableNodeInst newInstance(int protoId, String name, EPoint anchor) {
		if (protoId < 0) throw new ArrayIndexOutOfBoundsException(protoId);
		if (name == null) throw new NullPointerException("name");
		if (anchor == null) throw new NullPointerException("anchor");
		return new ImmutableNodeInst(protoId, name, anchor);
	}

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by protoId.
	 * @param protoId node protoId.
	 * @return ImmutableNodeInst which differs from this ImmutableNodeInst by protoId.
	 * @throws ArrayIndexOutOfBoundsException if protoId is negative.
	 */
	public ImmutableNodeInst withProto(int protoId) {
		if (this.protoId == protoId) return this;
		if (protoId < 0) throw new ArrayIndexOutOfBoundsException(protoId);
		return new ImmutableNodeInst(protoId, this.name, this.anchor);
	}

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by name.
	 * @param name node name.
	 * @return ImmutableNodeInst which differs from this ImmutableNodeInst by name.
	 * @throws NullPointerException if name is null
	 */
	public ImmutableNodeInst withName(String name) {
		if (this.name == name) return this;
		if (name == null) throw new NullPointerException("name");
		return new ImmutableNodeInst(this.protoId, name, this.anchor);
	}

	/**
	 * Returns ImmutableNodeInst which differs from this ImmutableNodeInst by anchor point.
	 * @param anchor node anchor point.
	 * @return ImmutableNodeInst which differs from this ImmutableNodeInst by anchor point.
	 * @throws NullPointerException if anchor is null.
	 */
	public ImmutableNodeInst withAnchor(EPoint anchor) {
		if (this.anchor == anchor) return this;
		if (anchor == null) throw new NullPointerException("anchor");
		return new ImmutableNodeInst(this.protoId, this.name, anchor);
	}

	/**
	 * Checks invariant of this ImmutableNodeInst.
	 * @throws AssertionError if invariant is broken.
	 */
	public void check() {
		assert protoId >= 0;
		assert name != null;
		assert anchor != null;
	}

	/**
	 * Checks that protoId of this ImmutableNodeInst is contained in cells.
	 * @param cells array with cells, may contain nulls.
	 * @throws ArrayIndexOutOfBoundsException if protoId is not contained.
	 */
	void checkProto(ImmutableCell[] cells) {
		if (cells[protoId] == null)
			throw new ArrayIndexOutOfBoundsException(protoId);
	}
}
