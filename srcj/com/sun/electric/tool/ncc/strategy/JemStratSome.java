/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratSome.java
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
 * JemStratSome is the parent for strategies like JemStratTypeSplit
 * produce a known number of offspring.
 */
package com.sun.electric.tool.ncc.strategy;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.trees.NetObject;

import java.util.Iterator;

public abstract class JemStratSome extends JemStrat {

    protected JemStratSome(){}
	
    // ---------- for JemRecord -------------
    public JemEquivList doFor(JemRecord g){
		return super.doFor(g);
    }
	
	// ---------- for JemCircuit -------------

    public JemCircuitMap doFor(JemCircuit g){
		return super.doFor(g);
    }
	
    //------------- for NetObject ------------

    //the doFor(NetOject) call returns an integer saying where it goes
    //of null to drop it entirely
    public abstract Integer doFor(NetObject n);

	/** Get the parent of 0th JemEquivRecord */
	protected JemRecord getOffspringParent(JemEquivList el){
		JemEquivRecord er = (JemEquivRecord) el.get(0);
		return (JemRecord) er.getParent();
	}

    //comments on the "code"th offspring of g
    protected JemEquivRecord pickAnOffspring(Integer code, JemEquivList g,
											 String ss){
		int value= code.intValue();
		for(Iterator it=g.iterator(); it.hasNext();){
			JemEquivRecord ch= (JemEquivRecord)it.next();
			if(ch.getValue() == value){
				getMessenger().line(ch.nameString() + " of "
								 	+ ch.maxSize() + " " + ss);
				return ch;
			}
		}
		//falls out if not found
		getMessenger().line("without " + ss);
        return null;
    }

}
