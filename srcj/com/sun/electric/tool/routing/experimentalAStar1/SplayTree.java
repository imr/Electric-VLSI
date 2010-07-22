/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SplayTree.java
 * Written by: Christian Julg, Jonas Thedering (Team 1)
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.routing.experimentalAStar1;

/**
 * Implements a top-down splay tree.
 * Available at http://www.link.cs.cmu.edu/splay/
 * ftp://ftp.cs.cmu.edu/usr/ftp/usr/sleator/splaying/SplayTree.java
 * Original author: Danny Sleator <sleator@cs.cmu.edu>
 * Original notice: This code is in the public domain.
 * 
 * Adapted specially for our nodes to avoid allocations
 * 
 *  Node.children[] is reused:
 *  2: left, 3: right
 * 
 * @author Jonas Thedering
 * @author Christian Jülg
 */
public class SplayTree
{
    private Node root;
    private Node header = new Node(); // For splay

    public SplayTree() {
    	clear();
    }
    
    public void clear() {
        root = null;
        header.children[2] = null;
        header.children[3] = null;
    }

    /**
     * Insert into the tree.
     * @param x the item to insert.
     * @throws DuplicateItemException if x is already present.
     */
    public void insert(Node key) {
		Node n;
		int c;
	    key.children[2] = null;
	    key.children[3] = null;
		if (root == null) {
		    root = key;
		    return;
		}
		splay(key);
		if ((c = key.f - root.f) == 0) {
		    assert false : "Duplicate item to be inserted into splay tree";
			return;
		}
		n = key;
		if (c < 0) {
		    n.children[2] = root.children[2];
		    n.children[3] = root;
		    root.children[2] = null;
		} else {
		    n.children[3] = root.children[3];
		    n.children[2] = root;
		    root.children[3] = null;
		}
		root = n;
    }

    /**
     * Remove from the tree.
     * @param x the item to remove.
     * @throws ItemNotFoundException if x is not found.
     */
    public void remove(Node key) {
		splay(key);
		if (key.f != root.f) {
		    assert false : "Item not found in splay tree";
			return;
		}
		// Now delete the root
		if (root.children[2] == null) {
		    root = root.children[3];
		} else {
		    Node x = root.children[3];
		    root = root.children[2];
		    splay(key);
		    root.children[3] = x;
		}
    }

    /**
     * Find the smallest item in the tree.
     */
    public Node findMin() {
        Node x = root;
        if(root == null) return null;
        while(x.children[2] != null) x = x.children[2];
        splay(x);
        return x;
    }

    /**
     * Find an item in the tree.
     */
    public Node find(Node key) {
		if (root == null) return null;
		splay(key);
        if(root.f != key.f) return null;
        return root;
    }

    /**
     * Test if the tree is logically empty.
     * @return true if empty, false otherwise.
     */
    public boolean isEmpty() {
        return root == null;
    }
    
    /**
     * Internal method to perform a top-down splay.
     * 
     *   splay(key) does the splay operation on the given key.
     *   If key is in the tree, then the BinaryNode containing
     *   that key becomes the root.  If key is not in the tree,
     *   then after the splay, key.root is either the greatest key
     *   < key in the tree, or the lest key > key in the tree.
     *
     *   This means, among other things, that if you splay with
     *   a key that's larger than any in the tree, the rightmost
     *   node of the tree becomes the root.  This property is used
     *   in the delete() method.
     */

    private void splay(Node key) {
		Node l, r, t, y;
		l = r = header;
		t = root;
		header.children[2] = header.children[3] = null;
		for (;;) {
		    if (key.f < t.f) {
				if (t.children[2] == null) break;
				if (key.f < t.children[2].f) {
				    y = t.children[2];                      /* rotate right */
				    t.children[2] = y.children[3];
				    y.children[3] = t;
				    t = y;
				    if (t.children[2] == null) break;
				}
				r.children[2] = t;                          /* link right */
				r = t;
				t = t.children[2];
		    } else if (key.f > t.f) {
				if (t.children[3] == null) break;
				if (key.f > t.children[3].f) {
				    y = t.children[3];                      /* rotate left */
				    t.children[3] = y.children[2];
				    y.children[2] = t;
				    t = y;
				    if (t.children[3] == null) break;
				}
				l.children[3] = t;                          /* link left */
				l = t;
				t = t.children[3];
		    } else {
		    	break;
		    }
		}
		l.children[3] = t.children[2];                      /* assemble */
		r.children[2] = t.children[3];
		t.children[2] = header.children[3];
		t.children[3] = header.children[2];
		root = t;
    }
}
