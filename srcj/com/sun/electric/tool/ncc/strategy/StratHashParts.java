/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratHashParts.java
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
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.jemNets.NetObject;
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.lists.RecordList;
import com.sun.electric.tool.ncc.trees.EquivRecord;

public class StratHashParts extends Strategy {
	private int numPartsProcessed;
	private int numEquivProcessed;

    private StratHashParts(NccGlobals globals){super(globals);}
	
    private void preamble(int nbParts){
		startTime("StratHashParts", nbParts+" Parts");
    }
	
    private void summary(LeafList offspring){
		globals.status2(" processed " +
					   numPartsProcessed + " Parts from " +
					   numEquivProcessed + " leaf Records");
		globals.status2(offspringStats(offspring));
		globals.status2(offspring.sizeInfoString());
		elapsedTime();
    }
	
    public LeafList doFor(EquivRecord g){
		if (g.isLeaf())  numEquivProcessed++;
		return super.doFor(g);
    }
	
    public Integer doFor(NetObject n){
		error(!(n instanceof Part), "StratHashPartAll expects only Parts");
		numPartsProcessed++;
		Part p= (Part)n;
		return p.computeHashCode();
    }
	
	// ------------------ intended interface -----------------
	public static LeafList doYourJob(RecordList l,
										NccGlobals globals){
		StratHashParts jhpa = new StratHashParts(globals);

		// If no Parts suppress all StratHashParts messages
		if (l.size()==0) return new LeafList();

		jhpa.preamble(l.size());
		LeafList offspring= jhpa.doFor(l);
		jhpa.summary(offspring);
		return offspring;
	}
}
