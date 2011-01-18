/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Vertex.java
 *
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.api.minarea.geometry;

import com.sun.electric.api.minarea.geometry.Point.Vector;

/**
 * @author Felix Schmidt
 * 
 */
public class Edge {

	private final Point head;
	private final Point tail;

	public Edge(Point tail, Point head) {
		this.tail = tail;
		this.head = head;
	}

	public Point getHead() {
		return head;
	}

	public Point getTail() {
		return tail;
	}
	
	public int length() {
		Vector vec = this.getDirection();
		return (int) Math.sqrt(vec.getX() * vec.getX() + vec.getY() * vec.getY());
	}
	
	public Vector getDirection() {
		Point tmp = head.add(tail.scale(-1));
		return new Vector(tmp.getX(), tmp.getY());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((head == null) ? 0 : head.hashCode());
		result = prime * result + ((tail == null) ? 0 : tail.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Edge other = (Edge) obj;
		if (head == null) {
			if (other.head != null)
				return false;
		} else if (!head.equals(other.head)) {
			if(!head.equals(other.tail))
				return false;
		} if (tail == null) {
			if (other.tail != null)
				return false;
		} else if (!tail.equals(other.tail)) {
			if(!tail.equals(other.head))
				return false;
		}
		return true;
	}

}
