/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Name.java
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
// Revised 22 November 2003 to extend String
// Annotated on 28 January 2004 by Ivan Sutherland

/** The Names class represents the names of NetObjects.
* Each Name has a text string.
* Methods for generating names are in the "NameFactory class.
* I made some attempt, largely unused, to represent bus names.
*/

package com.sun.electric.tool.ncc.basicA;

public class Name implements Comparable {

	/** the String for this Name */		private String myString= null;
	/** how many Names have been made */	private static int numNames= 0;

	/** Here is the access method for the number of Names created.
		* @return the number of Names created
		*/
	public static int getNumberNames(){return numNames;}

	/** Here is the constructor for Names.
		* @param a string to associate with this name
		*/
	private Name(String s){
		numNames++;
		myString= s;
	} //end of constructor

    // ---------- public methods ----------

	/** please is a factory method for making names.
		* @param a string to associate with this name
		* @return the newly created Name
		*/
	public static Name please(String s){
		Name n= new Name(s);
		return n;
	} //end of please

	/** The compareTo method compares this name with another.
		* needed for the Comparable interface.
		* @param the Name to compare with this one
		* @return an integer indicating their relative order
		*/
	public int compareTo(Object x){
		if(x instanceof Name){
			Name n= (Name)x;
			return myString.compareTo(n.myString);
		} //end of if
		return 0;
	} //end of compareTo

	/** The parallelMerge method combines names by concatinating their Strings.
		* I believe that the parallelMerge method has fallen into disuse.
		* I intended parallelMerge ultimately to combine bus names.
		* @param the Name to merge with this one
		* @return a Name with the combined strings
		*/
	public Name parallelMerge(Name x){
		if(x == null)return this;
		if(x == this)return this;
		if(myString.compareTo(x.myString) < 0)
			return please(myString + " " + x.myString);
		else return please(x.myString + " " + myString);
	} //end of parallelMerge


   	/** The toString method accesses the String for this Name.
		* @return the String for this Name
		*/
	public String toString(){return myString;}

	/** The isIndexed method tests whether this Name includes the character "[".
		* @return true if the "[" character is present, false otherwise
		*/
	public boolean isIndexed(){
		int i= myString.indexOf("[");
		if(i > 0)return true;
		return false;
	} //end of indexed

	/** The isExtended method tests whether this Name includes the underbar character.
		* @return true if "_" character is present, false otherwise
		*/
	public boolean isExtended(){
		int i= myString.lastIndexOf("_");
		if(i > 0)return true;
		return false;
	} //end of extended

	/** The baseString method extracts the String representing the base of this Name..
		* @return the base String: all characters up to but not including [ or _
			*/
	public String baseString(){
		int i= myString.indexOf("_");
		int j= myString.indexOf("[");
		int k;
		if((i < 0) && (j < 0))return null;
		if(i < 0)k= j;
		else if(j < 0)k= i;
		else k= (i < j) ? i : j  ;
		String s= myString.substring(0,k);
		return s;
	} //end of baseString

	/** The testMe method exercises some features of the Name class.
		*/
	public static void testMe(){
		Name a= please("tom");
		Name b= please("pete");
		Name c= please("tom[3]");
		Name d= please("tom_1");
		Name e= a.parallelMerge(b);
		System.out.println(e.toString());
		Name f= a.parallelMerge(c);
		System.out.println(f.toString());
		Name g= a.parallelMerge(d);
		System.out.println(g.toString());
		Name h= a.parallelMerge(a);
		System.out.println(h.toString());
		Name i= f.parallelMerge(e);
		Name j= e.parallelMerge(f);
		System.out.println(i.toString());
		System.out.println(j.toString());
		return;
	} //end of testMe
	
} //end of Name

