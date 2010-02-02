/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ChainModel.java
 * Written by Eric Kim and Tom O'Neill, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation.test;

import java.util.ArrayList;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * Tree data model of a scan chain, with APIs for inspection and I/O. This class
 * should only be used by ChainG, which requires a TreeModel implementation to
 * fire up the JTree. All other programs should probably be using the more
 * streamlined ChainControl instead.
 */

class ChainModel extends ChainControl {

    /** Cruft necessary for TreeModel implementation, should try to remove */
    private ArrayList treeModelListeners;

    private boolean isWorker = false; // true if worker is working

    /**
     * Creates a new instance of ChainModel, with the scan chain hierarchy
     * specified in provided the XML file
     * 
     * @param fileName
     *            Name of XML file containing scan chain description
     */
    ChainModel(String fileName) {
        super(fileName);
        treeModelListeners = new ArrayList();
    }

    /**
     * Messaged when the user has altered the value for the item identified by
     * path to newValue.
     */
    public void valueForPathChanged(TreePath path, Object newValue) {
        System.out.println("*** valueForPathChanged : " + path + " --> "
                + newValue);
    }

    /** Adds a listener for the TreeModelEvent posted after the tree changes. */
    public void addTreeModelListener(TreeModelListener l) {
        treeModelListeners.add(l);
    }

    /** Removes a listener previously added with addTreeModelListener. */
    public void removeTreeModelListener(TreeModelListener l) {
        treeModelListeners.remove(l);
    }

    /** Unit test */
    public static void main(String[] args) {
        ChainModel cm = new ChainModel("heater.xml");
        MyTreeNode node = cm.findNode("heater.pScan");
        System.out.println(node);
        MyTreeNode node2 = cm.findNode("p0.scan430", node);
        System.out.println(node2);
    }
}
