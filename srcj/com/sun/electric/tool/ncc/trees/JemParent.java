/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemParent.java
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
 * The JemParent class and JemChild interface help build the History tree.
 * A JemParent object has an unordered Collection of JemChild class objects,
 * and a JemChild object has a single JemParent object as its parent.
 * Such objects form a tree with parent objects closer to the root and
 * child objects closer to the leaves.
 * The classes that use these interfaces are:
 * JemParent
 *     JemRecord - extends JemParent - implements JemChild
 *         JemHistoryRecord - extends JemRecord - implements JemChild
 *         JemEquivRecord - extends JemRecord - implements JemChild
 *     JemCircuit - extends JemParent - implements JemChild
 * NetObject - implements only JemChild
 * The JemParent class and JemChild interface have nothing to do
 *   with the SafeList classes.
 */
package com.sun.electric.tool.ncc.trees;
import com.sun.electric.tool.ncc.basicA.Messenger;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class JemParent {

	/** holds the next level in the tree */private Collection content;
    /** for text output */private static Messenger myMessenger;

	/** 
	 * @param an int to suggest an initial size of the Collection
	 */
	protected JemParent(){
        myMessenger= Messenger.toTestPlease("JemParent");
		content= new ArrayList();
	}
	
	/**
	 * nameString returns a String of type and name for this JemParent.
	 * @return a String identifying this JemTree object.
	 */
    public abstract String nameString();

	/**
	 * getCode returns the fixed int hash code for this object.
	 * @return the int fixed hash code for this object.
	 */
	public abstract int getCode();

	/** 
	 * checkChild checks that a proposed JemChild is of the proper class.
	 * @param c the JemChild to test
	 * @return true if the JemChild is an OK class, false otherwise
	 */
	protected abstract boolean checkChild(JemChild c);

	/** 
	 * adopt both adds a Child to this collection and makes this be
	 * the Child's parent.
	 * @param c the JemChild to adopt
	 * @return true if all is well, false otherwise
	 */
	public boolean adopt(JemChild c){
		if(checkChild(c) && c.checkParent(this)){
			c.setParent(this);
			content.add(c);
			return true;
		} else {
			getMessenger().error("bad adoption in " + nameString());
			return false;
		} //end of else
	} //end of adopt

	/**
	 * Remove the named object from the content.
	 * @return true if the object was found and removed or false if
	 * the object is not present
	 */
	public boolean remove(JemChild c){return content.remove(c);}

	/** 
	 * The iterator method fetches an iterator for the collection.
	 * @return the Iterator
	 */
	public Iterator iterator(){return content.iterator();}

	/** Clear the Collection */
	public void clear(){content.clear();}

   	/** Fetch the number of elements in the Collection.
	 * @return an integer number of elements
	 */
	public int size(){return content.size();}

	/**
	 * access method for getting a Messenger.
	 * @return the default Messenger to use for text output
	 */
	protected static Messenger getMessenger(){return myMessenger;}
	
	/** get rid of a JemParent by clearing its content. */
	protected void killMe(){content= null;}
	
	/** 
	 * Here is a method to test if this JemParent contains a
	 * particular child.
	 * @param oo an Object to find in this content
	 * @return true if the Ojbect is found, false otherwise
	 */
	public boolean contains(Object oo){return content.contains(oo);}

} //end of JemParent
