/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemChild.java
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
// Annotated by Ivan Sutherland, 30 January 2004

/** 
 * The JemParent class and JemChild interface help build the History
 * tree.  A JemParent object has an unordered Collection of JemChild
 * class objects, and a JemChild object has a single JemParent object
 * as its parent.  Such objects form a tree with parent objects closer
 * to the root and child objects closer to the leaves.  The classes
 * that use these interfaces are:
 * JemParent
 *     JemRecord - extends JemParent - implements JemChild
 *         JemHistoryRecord - extends JemRecord - implements JemChild
 *         JemEquivRecord - extends JemRecord - implements JemChild
 *     JemCircuit - extends JemParent - implements JemChild
 * NetObject - implements only JemChild
 * The JemParent class and JemChild interface have nothing to do with
 * the SafeList classes.
 */
package com.sun.electric.tool.ncc.trees;

public interface JemChild {
    /** 
     * nameString returns a String of type and name for this JemChild.
     * @return a String identifying this JemTree object.
     */
    public abstract String nameString();
    
    /**
     * getCode returns the fixed int hash code for this object.
     * @return the int fixed hash code for this object.
     */
    public abstract int getCode();
    
    /**
     * getParent fetches the parent JemParent towards the tree's root.
     * @return the parent of this JemChild, or null for the root.
     */
    public abstract JemParent getParent();
    
    /**
     * setParent sets the parent, which points towards the root.
     * @param the JemParent to use
     * @return true if parent was accepted, false otherwise
     */
    public abstract boolean setParent(JemParent x);
    
    /**
     * checkParent tests a proposed JemParent for inclusion in this.
     * @param the JemParent to test
     * @return true if the JemParent is acceptable, false otherwise
     */
    public abstract boolean checkParent(JemParent x); // is parent an OK class
    
}
