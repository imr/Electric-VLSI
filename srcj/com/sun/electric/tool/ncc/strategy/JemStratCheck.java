/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratCheck.java
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

package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

import java.util.Iterator;

public class JemStratCheck extends JemStratNone{

    //these are variables that pass between levels of the tree
    private JemCollection bad;
    private JemRecord recordParent;
    private JemCircuit circuitParent;

    private JemStratCheck(){}

	public static JemEquivList doYourJob(JemRecord j) {
		JemStratCheck jsc = new JemStratCheck();
		jsc.preamble(j);
		JemEquivList el = jsc.doFor(j);    	
		jsc.summary(j);
		return el;
	}

    // ---------- the tree walking code ---------

    //do something before starting
    private void preamble(JemParent j){
		startTime("JemStratCheck", j.nameString());
		recordParent= null;
		circuitParent= null;
		bad= new JemCollection();
    }

    //summarize at the end
    private void summary(JemParent x){
        if(bad.size()>0){
            getMessenger().say("JemStratCheck done - there are ");
            getMessenger().line(bad.size() + " bad elements:");
            printBad();
        }else{
            getMessenger().line("JemStratCheck passed OK");
        }
        elapsedTime();
    }

    // ---------- for JemRecord -------------

    public JemEquivList doFor(JemRecord j){
        checkTheParent(j,"JemHistoryRecord", recordParent);
        JemRecord oldParent= recordParent; //save the old one
        recordParent= j;
		JemEquivList el= super.doFor(j);
        recordParent= oldParent;
        return el;
    }
    
    // ---------- for JemCircuit -------------

    public JemCircuitMap doFor(JemCircuit j){
        checkTheParent(j,"JemCircuit", recordParent);
        circuitParent= j;
        return super.doFor(j);
    }

    // ---------- for NetObject -------------

    public Integer doFor(NetObject n){
        if(n instanceof Wire)return doFor((Wire)n);
        if(n instanceof Part)return doFor((Part)n);
        return null;
    } //end of doFor

    // ---------- private methods -------------

    private Integer doFor(Wire w){
        checkTheParent(w,"Wire", circuitParent);
        //check the wire
        if(w.checkMe() == false)bad.add(w);
        int cleaned= w.cleanMe();
        return null;
    } //end of doFor

    private Integer doFor(Part j){
        checkTheParent(j,"Part",circuitParent);
        //check the Part
        if(j.checkMe() == false)bad.add(j);
        return null;
    } //end of doFor

    private void checkTheParent(JemChild n, String type, JemParent p){
        if(getDepth() == 0) return;	//no check
        if(n.getParent()==null){
            String s= type;
            if(n instanceof NetObject){
                NetObject no= (NetObject)n;
                s= no.nameString();
            } //end of if
            getMessenger().line( s + " at depth= " +
                                 getDepth() + " is orphan");
            bad.add(n);
        }else{
            //has a parent
            JemParent x= n.getParent();
            if(x != p){
                getMessenger().line("suspected bad parent for "
                                    + n.nameString()
                                    + " " + n.getCode());
                getMessenger().line("parent is "
                                    + p.nameString() + " " + p.getCode());
				if(x.contains(n))
					getMessenger().line("but all is well");
                bad.add(n);
            } //end of if
        } //end of else
        return;
    } //end of checkTheParent

    private void printBad(){
        Iterator i= bad.iterator();
        while(i.hasNext()){
            JemChild jt=(JemChild)i.next();
            printIt(jt);
        } //end of while
    } //end of printBad

    private void printIt(JemChild in){
        if(in instanceof NetObject){
            NetObject no= (NetObject)in;
            no.printMe(6);
        } else {
			JemCircuit inn= (JemCircuit)in;
			if(in instanceof JemCircuit)getMessenger().line
				(in.nameString() + " holds " + inn.size() + " NetObjects");
			else getMessenger().line
				(in.nameString() + " has " + inn.size() + " subordinates");
		} //end of else
		return;
	} //end of printIt

} //end of JemStratCheck
