/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratNone.java
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
 * JemStratNone is the parent for strategies with no offspring
 * mainly it circulates through the structure properly.
 * its doFor routines actually return nothing
 * Note: nameString and doFor(NetObject) remain abstract
 */
package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.trees.*;

//import java.util.Iterator;

/** 
 *JemStratNone is the parent for strategies with no offspring.
 * Printing, Counting, and Checking are examples.
 * JemStratNone traverses the tree depth first.
 * Its tree-traversal methods are avaialble to its subclasses.
 * JemStratNone remains abstract because nameString() and
 * 	doFor(NetObject) methods cannot yet be concrete.
 */
public abstract class JemStratNone extends JemStrat {


    protected JemStratNone(){}

    /** 
     * doFor(JemHistoryRecord) walks the tree starting at a JemHistoryRecord.
     * @return null.
     */
    public JemEquivList doFor(JemRecord g){
	JemEquivList el= super.doFor(g);
        return el;
    } //end of doFor(JemRecord)

    /** 
	 * doFor(JemCircuit) walks the tree starting at a JemCircuit.
	 * @return null.
	 */
    public JemCircuitMap doFor(JemCircuit g){
		JemCircuitMap mm= super.doFor(g);
        return mm;
    } //end of doFor

	/**
	 *  doFor(NetObject) tests the NetObject to decide its catagory.
	 *@return an Integer for the choice, or null to drop this NetObject.
	 */
    public abstract Integer doFor(NetObject n);
		
} //end of JemStratNone
