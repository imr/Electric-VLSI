/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemCollection.java
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
package com.sun.electric.tool.ncc.trees;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.strategy.JemStrat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;

/** 
 * JemCollection is a JemTree class that holds a Collection
 * JemCollection will accept any kind of JemTree object
 * Other JemTree classes are fussy about what they will hold
 * JemCollection is used to collect bad things in JemStratCheck
 */
//public class JemCollection{
//    private static final Collection empty= new ArrayList(0);
//	private Collection content;
//
//    public JemCollection(){content= new ArrayList(5);}
//	    
//    //to meet the interface requirements
//    public String nameString(){return "JemCollection";}
//
//    public int getCode(){return 0;}
//    
//    public void clear(){content= null;}
//
//	public int size(){return content.size();}
//	public void add(Object oo){content.add(oo);}
//	public Iterator iterator(){return content.iterator();}
//	    
//} //end of JemCollection
