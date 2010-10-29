/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Node.java
 *
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.generator.flag.hornFunnel2;

import java.util.List;

public class Node {
	private static int nodeCounter = 1;
	private final int id;
	private final int height; // Height in the tree.
	                          // height of leaf Node is 1
	private final Node leftChild, rightChild, parent;
	private int slot;
	public Node(Node parent, int height, int slot, List<Node> slots) {
		this.id = nodeCounter++;
		this.parent = parent;
		this.height = height;
		this.slot = slot;
		slots.set(slot, this);
		
		if (height==1) {
			leftChild = rightChild = null;
		} else {
			int childHeight = height-1;
			int childNbSlots = (int) (Math.pow(2, childHeight)-1);
			int halfChildSlots = (childNbSlots+1) /2;
			leftChild = new Node(this, childHeight, 
					             slot-halfChildSlots, slots);
			rightChild = new Node(this, childHeight, 
					              slot+halfChildSlots, slots);
		}
	}
	public Node getLeftChild() {return leftChild;}
	public Node getRightChild() {return rightChild;}
	public Node getParent() {return parent;}
	public int getSlot(){return slot;}
	public void setSlot(int s) {slot=s;}
	public int getHeight() {return height;}
	public boolean isLeaf() {return leftChild==null;}
	public boolean isRoot() {return parent==null;}
	public int getMinChildWireSlot() {
		int s = getSlot();
		if (isLeaf()) return s;
		s = Math.min(s, getLeftChild().getSlot());
		s = Math.min(s, getRightChild().getSlot());
		return s;
	}
	public int getMaxChildWireSlot() {
		int s = getSlot();
		if (isLeaf()) return s;
		s = Math.max(s, getLeftChild().getSlot());
		s = Math.max(s, getRightChild().getSlot());
		return s;
	}

	public int getChildWireLength() {
		return getMaxChildWireSlot() - getMinChildWireSlot();
	}
	public int getId() {return id;}
	@Override
	public String toString() {
		return "Node id="+id+" height="+height+" slot="+slot+
		       " childWireLen="+getChildWireLength();
	}
	public static void resetIds() {nodeCounter=1;}
}

