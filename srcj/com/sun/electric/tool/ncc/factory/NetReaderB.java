/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetReaderB.java
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
// Annotated on 29 January 2004 by Ivan Sutherland

/**
 * The NetReaderB class reads in a circuit from a text file.  Modified
 * 19 Novmeber to accept sizes and Ports.  Called "B" when major
 * changes made it best to pick a new class name.
 */

package com.sun.electric.tool.ncc.factory;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.basicA.Name;
import com.sun.electric.tool.ncc.factory.NameFactory;
import com.sun.electric.tool.ncc.trees.JemCircuit;
import com.sun.electric.tool.ncc.jemNets.Wire;
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.jemNets.Port;
import com.sun.electric.tool.ncc.jemNets.TransistorOne;

import java.util.Date;
import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.StreamTokenizer;

public class NetReaderB {
    /** NameFactory for making Wire names */   private NameFactory myNameFactory;
    /** the number of Parts read */				private int numParts;
    /** the number of Ports read */				private int numPorts;
    /** the number of Wires read */				private int numWires;
	/** holder for the tokenized input line */	private String[] theLine;
	/** holds the length and width values */	private double[] theDoubles;
    /** Messenger to make output */
	protected static Messenger myMessenger= Messenger.toTestPlease("NetReaderB");

	/** 
	 * Here is the constructor for NetReaderB
	 * @return a new NetReaderB
	 */
    private NetReaderB(){
		myNameFactory= new NameFactory();
        theLine= new String[6];
		theDoubles= new double[2];
		return;
    } //end of constructor

	/** 
	 * Here is a factory method for NetReaderB class
	 * @return a fresh NetReaderB
	 */
    public static NetReaderB please(){
        return new NetReaderB();
    } //end of please

	/**
	 * Here is method to set up and initialize a StreamTokenizer
	 * @param fileName String defining the name of the file to read
	 * @return the StreamTokenizer for this NetReader to use
	 */
    public StreamTokenizer getMyTokenizer(String fileName){
        FileReader fr= null;
        try{
            fr= new FileReader(fileName);
        }catch (Exception e){
            myMessenger.error(e.toString());
        }
        BufferedReader br= new BufferedReader(fr);
        StreamTokenizer st= new StreamTokenizer(br);
        st.eolIsSignificant(true);
        st.wordChars('/','/');
        st.wordChars('[','[');
        st.wordChars(']',']');
        st.wordChars('_','_');
		st.wordChars('@','@');
        st.wordChars('0','9');
        return st;
    } //end of getMyTokenizer

	/**
	 * The getAline method reads and parses one line from the input
	 * file.
	 * @param the StreamTokenizer to use for input
	 * @return the number of tokens read in the line (not used)
	 * @return (implicit) puts values into theLine and theDoubles
	 */
    //convert a line into an array of Strings.
    //returns null if last one
    public int getAline(StreamTokenizer st){
        int i= 0;
        int j= 0;
		theLine= new String[6];
		theDoubles= new double[2];
        try{
            int c= 0;
            while (st.nextToken() != StreamTokenizer.TT_EOL){
                st.pushBack(); c=st.nextToken();
                if(c==StreamTokenizer.TT_EOF)return i; //should be empty
                if(c==StreamTokenizer.TT_EOL)return i; //should be empty
                if(c==StreamTokenizer.TT_WORD){
					theLine[i]= st.sval;
					i++;
				}
                if(c==StreamTokenizer.TT_NUMBER){
					theDoubles[j]= st.nval;
					j++;
				}
            } //end of while
        }catch(Exception e){
            myMessenger.line(e.toString());
        } //end of catch
        return i;
    } //end of getLine

	/**
	 * Here is a debug method to print the tokens just read
	 * @param int ii to say how many tokens to print
	 */
    public void printAstringLine(int ii){
        for(int i=0; i<ii; i++){
            if(theLine[i] != null){
                myMessenger.say(" " + theLine[i]);
            } //end of if
        } //end of loop
    } //end of printAline

	/**
	 * Here is a debug method to print the numbers just read
	 */
    public void printAnumericLine(){
        for(int i=0; i<2; i++){
			myMessenger.say(" " + theDoubles[i]);
        } //end of loop
    } //end of printAline

	/**
	 * Here is a method to put the component just read into a JemCircuit
	 * @param JemCircuit to hold the component
	 * @param (implicit) theLine and theDouble hold the data to use
	 * @return (implicit) the JemCircuit ends with another component in it
	 */
    public void useAline(JemCircuit j){
		if(j == null) return;
        TransistorOne t= null;
        if(theLine[0] == null)return;
		Name nn= null;
		if(theLine[0].equals("NMOS")){
			nn= myNameFactory.namePlease();
			t= TransistorOne.nPlease(j, nn);
			finishTransistor(t, j);
		} //end of if
		if(theLine[0].equals("PMOS")){
			nn= myNameFactory.namePlease();
			t= TransistorOne.pPlease(j, nn);
			finishTransistor(t, j);
		} //end of if
		if(theLine[0].equals("EXPORT")){
			Port p= Port.please(j, nameFor(1)); //get the String
			Wire s= null;
			numPorts++;
			if(theLine[2] != null)
				s= Wire.please(j, nameFor(2));
			p.connect(s);
		} //end of Part
		return;
    } //end of useAline

	/**
	 * Here is an auxiliary method to attach the Wires to a transistor
	 * @param TransistorOne to have Wires added
	 * @param JemCircuit to hold the new Wires
	 * @return (implicit) the TransistorOne is attached to new Wires
	 * in the JemCircuit
	 */
	private void finishTransistor(TransistorOne t, JemCircuit j){
		Wire s= null, g=null, d= null;
		numParts++;
		t.setWidthLength(theDoubles[0], theDoubles[1]);
		if(theLine[1]!=null) s= Wire.please(j, nameFor(1));
		if(theLine[2]!=null) g= Wire.please(j, nameFor(2));
		if(theLine[3]!=null) d= Wire.please(j, nameFor(3));
		t.connect(s,g,d);
		return;
	} //end of finishTransistor

	/**
	 * Here is an auxiliary method to get Wires for the names in theLine
	 * @param int indicating the connection for which to make a name
	 * @return the Name for this connection - may be either existing
	 * or newly created
	 */
	private Name nameFor(int i){
		Name nn= null;
		if(theLine[i] != null)nn= myNameFactory.namePlease(theLine[i]);
		return nn;
	} //end of nameFor

	/** 
	 * Here is a method to read a file and make a JemCircuit
	 * @param String fileName - the location of the file to read
	 * @return the newly created JemCircuit
	 */
	public JemCircuit read(String fileName){
		long startTime= new Date().getTime();
		JemCircuit out= JemCircuit.please();
		numPorts= 0;
		numParts= 0;
		numWires= Wire.getWireCount();
		StreamTokenizer st= getMyTokenizer(fileName);
		int iii= 0;
		while((iii= getAline(st)) > 0){
			//printAstringLine(iii);
			//printAnumericLine();
			//myMessenger.freshLine();
			useAline(out);
		} //end of while
		long endTime= new Date().getTime();
		myMessenger.say("To read " + (Wire.getWireCount() - numWires) +
						" wires, ");
		myMessenger.say(numPorts + " Ports and ");
		myMessenger.line(numParts + " Parts");
		myMessenger.line("took " + (endTime - startTime) + " milliseconds");
		myMessenger.freshLine();
		//out.printMe(myMessenger);
		return out;
	} //end of read

} //end of NetReaderB
