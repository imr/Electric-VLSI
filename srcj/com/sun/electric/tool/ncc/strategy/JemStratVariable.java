/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratVariable.java
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

/**
 * JemManager keeps lists of active JemEquivRecords.  It knows what
 * the strategies do and puts newly-created JemEquivRecord on the
 * right lists.
 */
package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.trees.JemRecord;
import com.sun.electric.tool.ncc.trees.JemHistoryRecord;
import com.sun.electric.tool.ncc.trees.JemEquivRecord;
import com.sun.electric.tool.ncc.trees.JemCircuit;
import com.sun.electric.tool.ncc.trees.NetObject;
import com.sun.electric.tool.ncc.lists.JemRecordList;
import com.sun.electric.tool.ncc.lists.JemEquivList;
import com.sun.electric.tool.ncc.basicA.Messenger;

public class JemStratVariable {
	private static Messenger getMessenger(){return JemStrat.getMessenger();}

	JemSets myJemSets;

	private int shouldDo= 1; // the number of promissing splits sought
	private int doneCount;
	private int splitCount;
	private int reprocessCount;
	private static JemAdjacent myAdjacent= JemAdjacent.please();
	private static JemHashPartAll myPartAll= JemHashPartAll.please();
	private static JemHashWireAll myWireAll= JemHashWireAll.please();
	private static JemWireName myNamer= JemWireName.please();
	private static JemStratFrontier myFront= JemStratFrontier.please();

    public JemStratVariable(JemSets js){myJemSets= js;}

	public void doAllProcess(){
        getMessenger().line("----- starting doAllProcess");
		JemEquivList thePartSeed= new JemEquivList();
		JemEquivList theWireSeed= new JemEquivList();
		//do one hash process on Parts
		theWireSeed= onePartsPass(myJemSets);
		//do the full hash process on that seed
		
        doActiveLists(theWireSeed, thePartSeed);
        getMessenger().line("----- done doActiveLists");
		
		thePartSeed= new JemEquivList();
		while(thePartSeed.size() == 0){
			thePartSeed= oneNamePass(myJemSets);
			JemStratCount.doYourJob(myJemSets.starter, myJemSets.ports);
		} //end of loop
		
        doActiveLists(theWireSeed, thePartSeed);
        getMessenger().line("----- done doActiveLists");
		
		thePartSeed= new JemEquivList();
		while(thePartSeed.size() == 0){
			thePartSeed= oneNamePass(myJemSets);
			JemStratCount.doYourJob(myJemSets.starter, myJemSets.ports);
		} //end of loop
		
        doActiveLists(theWireSeed, thePartSeed);
		
		thePartSeed= new JemEquivList();
		while(thePartSeed.size() == 0){
			thePartSeed= oneNamePass(myJemSets);
			JemStratCount.doYourJob(myJemSets.starter, myJemSets.ports);
		} //end of loop
		
        doActiveLists(theWireSeed, thePartSeed);
		
		thePartSeed= new JemEquivList();
		while(thePartSeed.size() == 0){
			thePartSeed= oneNamePass(myJemSets);
			JemStratCount.doYourJob(myJemSets.starter, myJemSets.ports);
		} //end of loop
		
        doActiveLists(theWireSeed, thePartSeed);
        getMessenger().line("----- done doActiveLists");
		return;
	} //end of doAllProcess

	private JemEquivList onePartsPass(JemSets in){
		getMessenger().line("----- starting onePartsPass");
		JemEquivList partOffspring= myPartAll.doYourJob(in);
		JemStratCount.doYourJob(partOffspring, myJemSets.ports);
		JemEquivList freshParts= JemEquivRecord.findNewlyRetired(partOffspring);
		JemEquivList theWireSeed= myAdjacent.doFor(freshParts);
		//JemStratCheck.doYourJob(theWireSeed);
		JemStratCount.doYourJob(theWireSeed, myJemSets.ports);
		//		JemStratPrint.doYourJob(theWireSeed);
		getMessenger().line("----- done onePartsPass");
		return theWireSeed;
	} // end of onePartsPass
	
	private JemEquivList oneNamePass(JemSets in){
		getMessenger().line("----- starting oneNamePass");
		JemEquivList frontier= myFront.doFor(in.wires);
		JemStratCount.doYourJob(frontier, myJemSets.ports);
		JemEquivList offspring= myNamer.doFor(frontier);
		JemEquivList fresh= JemEquivRecord.findNewlyRetired(offspring);
		JemEquivList thePartSeed= myAdjacent.doFor(fresh);
		JemStratCount.doYourJob(thePartSeed, myJemSets.ports);
		getMessenger().line("----- done oneNamePass");
		return thePartSeed;
	} // end of oneNamePass
	
	/*
	public int portSplit(){
		JemRecord g= (JemEquivRecord)myJemSets.ports;
		if(g == null)return 0;
		myJemSets.active= JemHistoryRecord.findTheLeaves(g);
		if(myJemSets.active.size() == 0)return 0;
		JemNameSplit split= (JemNameSplit)JemNameSplit.please();
		JemRecordList cc= split.doFor(myJemSets.active);
		getMessenger().line("portSplit produced " + cc.size() +
					  " new JemEquivRecords");
		return cc.size(); //the number of offspring produced
	} //end of randomSplit

	public int randomSplit(){
		JemRecord g= myJemSets.wires;
		if(g == null)return 0;
		myJemSets.active= JemHistoryRecord.findTheLeaves(g);
		if(myJemSets.active.size() == 0)return 0;

		JemRandomMatch split= (JemRandomMatch)JemRandomMatch.please();
		JemRecordList cc= split.doFor(myJemSets.active);
		getMessenger().line("randomSplit produced " + cc.size() +
					  " new JemEquivRecords");
		return cc.size();
	} //end of randomSplit
*/
	
	// returns true if processing is done, false otherwise
	private int doActiveLists(JemEquivList inWires, JemEquivList inParts){
	//	myJemSets.sizeReport();
		if((inWires.size() == 0)&&(inParts.size() == 0)){
			getMessenger().line("------ both seed sizes are zero");
			return 0;
		} //end of if 0
        int i= 0;
		JemEquivList wireSeed= inWires;
		JemEquivList partSeed= inParts;
		
		getMessenger().line("------ starting doActiveLists");
        while(i < 500 && ((wireSeed.size() > 0) || (partSeed.size() > 0))){
			if(partSeed.size() > 0){
				getMessenger().line("------ Parts pass " + ++i);
				wireSeed= processParts(partSeed);
				partSeed=  new JemEquivList(); //partSeed is used up
		} //end of if partSeed
			if(wireSeed.size() > 0){
				getMessenger().line("------ Wires pass " + ++i);
				partSeed= processWires(wireSeed); //one pass
				wireSeed=  new JemEquivList(); //wireSeed is used up
		} //end of else
		} //end of while
		getMessenger().line("------ done  doActiveLists after " +
							i + " passes");
		//		JemStratCheck.doYourJob(myJemSets.starter);
		JemStratCount.doYourJob(myJemSets.starter, myJemSets.ports);
		getMessenger().freshLine();
		//JemStratPrint.doYourJob(myJemSets.starter);
        return i;
    } //end of doActiveLists

	//takes in a list of wires and produces the adjacent list of parts
    private JemEquivList processWires(JemEquivList in){
		getMessenger().line("Jemini starting processWires");
		JemEquivList out;
		if(in.size() != 0){
			JemEquivList offspring= myWireAll.doFor(in);
			JemEquivList fresh= JemEquivRecord.findNewlyRetired(offspring);
			out= myAdjacent.doFor(fresh);
			getMessenger().line("Jemini processed " +
								in.size() + " Wire groups, producing " +
								offspring.size() + " offspring of which " +
								fresh.size() + " retired with " +
								out.size() + " adjacent Part groups");
        } else {
			getMessenger().line("processWires got an empty input list");
			out=  new JemEquivList();
		} //end of else
		getMessenger().freshLine();
		return out;
	} //end of processWires

    private JemEquivList processParts(JemEquivList in){
		getMessenger().line("Jemini starting processParts");
		JemEquivList out;
		if(in.size() != 0){
            JemEquivList offspring= myPartAll.doFor(in);
			JemEquivList fresh= JemEquivRecord.findNewlyRetired(offspring);
			 out= myAdjacent.doFor(fresh);
			getMessenger().line("Jemini processed " +
								in.size() + " Part groups, producing " +
								offspring.size() + " offspring of which " +
								fresh.size() + " retired with " +
								out.size() + " adjacent Wire groups");
		} else {
			getMessenger().line("processParts got an empty input list");
			out=  new JemEquivList();
		} //end of else
		getMessenger().freshLine();
		return out;
	} //end of processWires
} //end of JemManager
