/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MyTreeNode.java
 * Written by Eric Kim, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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

import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * Default node class for chip-testing hierarchical data structures. This is the
 * superclass of SubchainNode, ChipNode, etc.
 */
public class MyTreeNode implements TreeNode {

    /** One level up the MyTreeNode hieararchy */
    private MyTreeNode parent;

    /** One level down the MyTreeNode hierarchy */
    private ArrayList children;

    /**
     * Node name. E.g., the node path "expC.receive.calibrate" describes a node
     * named "calibrate", which is a child of node "receive", which is a child
     * of the root scan chain "expC".
     */
    private String name;

    /** Comment attached to this node */
    private String comment;

    /** Number of entries in children ArrayList */
    private int childCount;

    /** Define the blackslash and the period as field separators */
    private static final Pattern splitter = Pattern.compile("\\.");

    /**
     * Default constructor.
     * 
     * @param name
     *            node name.
     * @param comment
     *            comment attached to this node
     */
    public MyTreeNode(String name, String comment) {
        setName(name);
        this.comment = comment;
        this.parent = null;
        children = new ArrayList();
        childCount = 0;
    }

    /**
     * Get long version of node name. Overides default toString method, and is
     * in turn overridden by subclasses.
     */
    public String toString() {
        return name;
    }

    /**
     * Get short version of node name. Provided to make short version accessible
     * after toString() gets overridden.
     */
    final public String getName() {
        return name;
    }

    /** Accessor method, sets node name */
    public void setName(String name) {
        this.name = name;
    }

    String getComment() {
        return comment;
    }

    /** Add a child to the node */
    void addChild(MyTreeNode newNode) {
        if (newNode == null) {
            return;
        }
        children.add(newNode);
        newNode.parent = this;
        childCount++;
    }

    public Enumeration children() {
        return new ChildEnumerator(children);
    }

    public boolean getAllowsChildren() {
        return true;
    }

    /**
     * Return selected child
     *
     * @param index
     *            index of the child
     * @return the selected child
     */
    public MyTreeNode getChildAt(int index) {
        if (index < 0 || index >= children.size()) {
            return null;
        } else {
            return (MyTreeNode) children.get(index);
        }
    }

    /** return the number of children */
    public int getChildCount() {
        return childCount;
    }

    /**
     * Returns MyTreeNode object one level up in hierarchy
     *
     * @return parent tree node
     */
    public MyTreeNode getParent() {
        return parent;
    }

    /**
     * return the index of the child
     * 
     * @param child
     *            node to be found
     * @return index of the child
     */
    public int getIndex(TreeNode child) {
        return children.indexOf(child);
    }

    public boolean isLeaf() {
        if (children.size() > 0) return false;
        return true;
    }

    /**
     * Return the node hierarchy from root down to <tt>this</tt>. E.g., if
     * <tt>this</tt> is node expC.receive.calibrate, function will return the
     * MyTreeNode array {expC, receive, calibrate}.
     * 
     * @return array of MyTreeNode objects in path to "this"
     */
    public MyTreeNode[] getHierarchy() {
        ArrayList paths = new ArrayList();
        for (MyTreeNode node = this; node != null; node = node.getParent()) {
            paths.add(0, node);
        }
        MyTreeNode[] ret = new MyTreeNode[paths.size()];
        for (int i = 0; i < paths.size(); i++) {
            ret[i] = (MyTreeNode) paths.get(i);
        }
        return ret;
    }

    /**
     * return part of the string representation of the path. E.g.,
     * "expC.receive.calibrate" (startLevel=0) or "receive.calibrate"
     * (startLevel=1).
     * 
     * @param startLevel
     *            start point of the path. 0=root, 1=first level, ..
     * @return string representation of the path, starting at startLevel
     */
    public String getPathString(int startLevel) {
        StringBuffer sb = new StringBuffer();
        MyTreeNode[] nodes = getHierarchy();
        for (int i = startLevel; i < nodes.length; i++) {
            sb.append(nodes[i].getName() + '.');
        }

        // Delete the trailing "." (lame)
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * find a node given root node and partial path string. WARNING: Cannot be
     * used to find the root node.
     * 
     * @param root
     *            starting node of the path
     * @param path
     *            path string, starting at level 1 (excludes root node)
     * @return node under root that is described by the path string
     */
    public static MyTreeNode getNode(MyTreeNode root, String path) {
        MyTreeNode node = root, nextNode = null;

        // Generate array of node names (excluding root)
        String[] pathNames = splitter.split(path);
        if (path.equals("")) {
            System.out.println("MyTreeNode.getNode() WARNING: cannot use this "
                    + "method to find root node");
        }

        for (int depth = 0; depth < pathNames.length; depth++) {
            boolean found = false;
            String name = pathNames[depth];

            // Loop over children until find one whose name matches the next
            // node name in the path
            for (int ind = 0; ind < node.getChildCount(); ind++) {
                MyTreeNode child = node.getChildAt(ind);
                if (child.getName().equals(name)) {
                    if (found) {
                        Infrastructure.fatal("Two nodes with name " + name
                                + " in path " + path);
                    }
                    nextNode = child;
                    found = true;
                }
            }

            // If none of the children match at this depth, game over
            if (found == false) {
                return null;
            }
            node = nextNode;
        }

        return node;
    }

    /**
     * Recursively add to a list all of the nodes in the hierarchy under the
     * requested node.
     * 
     * @param node
     *            node whose descendents to add to the list
     * @param list
     *            list to add the descendents to
     */
    private void addDescendentsToList(MyTreeNode node, java.util.List list) {
        int numKids = node.getChildCount();
        for (int ind = 0; ind < numKids; ind++) {
            MyTreeNode kid = node.getChildAt(ind);
            list.add(kid);
            addDescendentsToList(kid, list);
        }
    }

    /**
     * Returns all nodes below the current node in the hierarchy.
     * 
     * @return array of nodes beneath <code>this</code> node
     */
    public MyTreeNode[] getDescendents() {

        // Create list of path names for hierarchy starting at the this node,
        // then remove the entry for the this node itself
        java.util.List kidList = new java.util.ArrayList();
        addDescendentsToList(this, kidList);

        // Convert the path list to a string array
        MyTreeNode[] descendents = new MyTreeNode[kidList.size()];
        for (int ind = 0; ind < kidList.size(); ind++) {
            descendents[ind] = (MyTreeNode) kidList.get(ind);
        }
        return descendents;
    }

    /** Helper for CompareXML */
    void compare(MyTreeNode that, String thisFile, String thatFile) {
        if (getName().equals(that.getName()) == false) {
            System.out.println("**** Node names differ: '" + getPathString(1)
                    + "' in " + thisFile + ", but '" + that.getPathString(1)
                    + "' in " + thatFile);
        }
    }

    public static class ChildEnumerator implements Enumeration {
        private ArrayList list;
        private int index = 0;
        public ChildEnumerator(ArrayList list) {
            this.list = list;
        }
        public boolean hasMoreElements() {
            return index < list.size();
        }
        public Object nextElement() {
            if (index < list.size()) {
                Object obj = list.get(index);
                index++;
                return obj;
            }
            return null;
        }
    }

    /**
     * Unit test. Creates a hierarchy level0 -> {level1a, level1b}; level1b ->
     * level2. Uses it to test the getPathString() and getNode() methods.
     */
    public static void main(String[] args) throws Exception {
        String path;
        MyTreeNode tryFind;

        MyTreeNode level0 = new MyTreeNode("level0", "frog");
        MyTreeNode level1a = new MyTreeNode("level1a", "frog");
        MyTreeNode level1b = new MyTreeNode("level1b", "frog");
        MyTreeNode level2 = new MyTreeNode("level2", "frog");

        level0.addChild(level1a);
        level0.addChild(level1b);
        level1b.addChild(level2);

        path = level0.getPathString(0);
        System.out.println("path string, starting at level 0: " + path);
        path = level0.getPathString(1);
        System.out.println("path string, starting at level 1 (should fail): "
                + path);
        tryFind = MyTreeNode.getNode(level0, "");
        System.out.println("tryFind = " + tryFind);
        tryFind = MyTreeNode.getNode(level0, "level0");
        System.out.println("tryFind = " + tryFind + "\n");

        path = level1b.getPathString(0);
        System.out.println("path string, starting at level 0: " + path);
        path = level1b.getPathString(1);
        System.out.println("path string, starting at level 1: " + path);
        tryFind = MyTreeNode.getNode(level0, path);
        System.out.println("tryFind = " + tryFind + "\n");

        path = level2.getPathString(0);
        System.out.println("path string, starting at level 0: " + path);
        path = level2.getPathString(1);
        System.out.println("path string, starting at level 1: " + path);
        tryFind = MyTreeNode.getNode(level0, path);
        System.out.println("tryFind = " + tryFind);

    }
}
