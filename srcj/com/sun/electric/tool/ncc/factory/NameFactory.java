/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NameFactory.java
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
// Annotated on 28 January 2004 by Ivan Sutherland

package com.sun.electric.tool.ncc.factory;

import java.util.Hashtable;
import com.sun.electric.tool.ncc.basicA.Name;
import com.sun.electric.tool.ncc.basicA.Messenger;

/**
 * The NameFactory holds methods for creating and sorting instances of
 * the Names class.  NameFactory ensures that each Name instance has a
 * String unique to this factory.  Factory methods are called "please"
 * and return an instance of Name, creating a new instance only if
 * necessary.
 */
public class NameFactory {

	/** myHash holds the Strings for Names */	private Hashtable myHash;
    /** provides a way to deliver text output */	private Messenger myMessenger;
    /** a counter for generated names */	private int si;
	/** a table of characters */ private static String[] st= {
		"a" , "b", "c", "d", "e", "f", "g", "h", "i",
		"j" , "k", "l", "m", "n", "o", "p", "q", "r",
		"s" , "t", "u", "v", "w", "x", "y", "z" };

	/** Here is the constructor for NameFactory.
		* @return a new NameFactory
		*/
	public NameFactory(){
		myHash= new Hashtable();
		myMessenger= Messenger.toTestPlease("NameFactory");
		si= 0;
		return;
	} //end of NameFactory

	/**
	 * Here is a no-argument factory method for making a Name.
	 * @return a Name with the next sequential String
	 */
	public Name namePlease(){
		Name n= null;
		String s= nextString();
		return namePlease(s);
	} //end of namePlease

    /** 
	 * Here is a factory method for finding or making a Name with
	 * the given String.
	 * @param the String to use for this name
	 * @return a Name with the input String, newly created only if necessary
	 */
    public Name namePlease(String s){
		if(s==null)return namePlease();
		Name n= null;
		if(myHash.containsKey(s)){
			n= (Name)myHash.get(s);
		}else{
			n= Name.please(s);
			myHash.put(s, n);
		} //end of else
		return n;
	} //end of namePlease

	/**
	 * Here is a factory method for making a bus name with a bracketed index
	 * @param the String to use as the base name
	 * @param the integer bus index
	 * @return a Name with the bus string
	 */
	public Name namePlease(String s, int i){
		String ss= (s + "[" + i + "]");
		Name n= Name.please(ss);
		return n;
	} //end of namePlease

	// ---------- private methods ----------

	/** Here is a method to make a new unique string.
		* Successive Strings are a through z, aa through zz followed by xyz[<integer>].
		* @param the integer to convert into a String of the particular form
		* @return a String of the the calculated form
		*/
    private static String makeAstring(int i){
		if(i < 26)return st[i];
		if(i < 26*27){
			int ms= i/26;
			int ls= i-ms*26;
			String s= st[ms-1] + st[ls];
			return s;
		} //end of if
		else return ("xyz[" + (i-26*27) + "]");
    } //end of makeAstring

	/**
	 * Here is a method to make the next successive String.
	 * @return the next String in the String sequence.
	 */
    private String nextString(){
		si++;
		return makeAstring(si-1);
    } //end of nextString

    // ---------- public methods ----------

	/**
	 * Here is a method to test whether a given string is known to this factory.
	 * @param the String to test
	 * @return true if the String is known, false otherwise
	 */
    public boolean isKnown(String s){return myHash.containsKey(s);}

	//this is the test code
	/** Here ia method that tests various features of the NetFactory class.
		* @param none
		* @return none
		*/
    public void testMe(){
		myMessenger.line("testing NameFactory");
		for (int i=0; i<30; i++){
			myMessenger.say(nextString() + " ");
		} // end of loop
		myMessenger.freshLine();
		Name n, nn;
		n= namePlease("tom");
		myMessenger.say(n.toString());
		myMessenger.say(" " + n.isIndexed());
		myMessenger.say(" " + n.isExtended());
		myMessenger.line(" " + Name.getNumberNames());
		n= namePlease("mary_5");
		myMessenger.say(n.toString());
		myMessenger.say(" " + n.isIndexed());
		myMessenger.say(" " + n.isExtended());
		myMessenger.say(" " + n.baseString());
		myMessenger.line(" " + Name.getNumberNames());
		n= namePlease("pete", 4);
		myMessenger.say(n.toString());
		myMessenger.say(" " + n.isIndexed());
		myMessenger.say(" " + n.isExtended());
		myMessenger.line(" " + Name.getNumberNames());
		n= namePlease("tom");
		myMessenger.say(n.toString());
		myMessenger.say(" " + n.isIndexed());
		myMessenger.say(" " + n.isExtended());
		myMessenger.line(" " + Name.getNumberNames());
		myMessenger.freshLine();
		for (int i=0; i<30; i++){
			myMessenger.line(i + " gives " + makeAstring(i));
		} //end of loop
		myMessenger.freshLine();
		for (int i=0; i<10; i++){
			myMessenger.line(i+50 + " gives " + makeAstring(i+50));
		} //end of loop
		myMessenger.freshLine();
		for (int i=0; i<10; i++){
			myMessenger.line(i+700 + " gives " + makeAstring(i+700));
		} //end of loop
    } //end of testMe

} //end of NameFactory class
