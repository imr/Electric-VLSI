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
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.jemNets.Wire;

import java.util.Iterator;

public class JemStratVariable {
	JemSets myJemSets;

	private static void error(boolean pred, String msg) {
		if (pred) Messenger.error(msg);
	}

    private JemStratVariable(JemSets js){myJemSets= js;}

	public static void doYourJob(JemSets js) {
		JemStratVariable jsv = new JemStratVariable(js);
		jsv.doAllProcess();
	}

	private void doAllProcess(){
        Messenger.line("----- starting doAllProcess");

		while (true) {
			JemEquivList partOffspring = hashFrontierParts(myJemSets);
			chaseRetired(partOffspring);
		
			JemEquivList wireOffspring = hashFrontierWires(myJemSets);
			chaseRetired(wireOffspring);
			if (partOffspring.size()==0 && wireOffspring.size()==0) break;
		}
		
		while (true) {
			JemStratCount.doYourJob(myJemSets.starter);
			JemEquivList offspring = JemWireName.doYourJob(myJemSets);
			if (offspring.size()==0) break;
			chaseRetired(offspring);
		}

        Messenger.line("----- done doAllProcess");
	}

	private JemEquivList hashFrontierParts(JemSets in){
		Messenger.line("----- hash all Parts on frontier");
		JemEquivList frontier = JemStratFrontier.doYourJob(in.parts);
		JemEquivList offspring = JemHashPartAll.doYourJob(frontier);
		return offspring;
	}
	
	private JemEquivList hashFrontierWires(JemSets in){
		Messenger.line("----- hash all Wires on frontier");
		JemEquivList frontier = JemStratFrontier.doYourJob(in.wires);
		JemEquivList offspring = JemHashWireAll.doYourJob(frontier);
		return offspring;
	}
	
	private boolean isPartsList(JemEquivList el) {
		JemEquivRecord er = (JemEquivRecord) el.get(0);
		return er.getNetObjType()==NetObject.Type.PART;
	}

	/**
	 * Takes a list of newly divided Part/Wire JemEquivRecords. If any
	 * of them retire then find the retirees' neighbors and perform a
	 * hash step on them to get newly divided Wire/Part
	 * JemEquivRecords. Repeat until the hash step yields no newly
	 * divided JemEquivRecords.
	 * @param newDivided newly divided JemEquivRecords
	 */
	private void chaseRetired(JemEquivList newDivided) {
		Messenger.line("------ starting chaseRetired");
		int i=0;
		while (true) {
			Messenger.line("------ chaseRetired pass: " + i++);
			if (newDivided.size()==0) break;
			JemEquivList newRetired = newDivided.selectRetired();
			if (newRetired.size()==0) break;
			JemEquivList adjacent = JemAdjacent.doYourJob(newRetired);
			if (adjacent.size()==0)  break;
			boolean doParts = isPartsList(adjacent);
			newDivided = doParts ? JemHashPartAll.doYourJob(adjacent) :
								   JemHashWireAll.doYourJob(adjacent);
		}
		Messenger.line("------ done  chaseRetired after "+i+" passes");
		//		JemStratCheck.doYourJob(myJemSets.starter);
		Messenger.freshLine();
	}
	
	/**
	 * Takes a list of newly divided Part/Wire JemEquivRecords. Finds
	 * their neighbors and performs a hash step on them to get newly
	 * divided Wire/Part JemEquivRecords. Repeat until the hash step
	 * yields no newly divided JemEquivRecords.
	 * @param newDivided newly divided JemEquivRecords
	 */
	private void chaseDivided(JemEquivList newDivided) {
		Messenger.line("------ starting chaseDivided");
		int i=0;
		while (true) {
			Messenger.line("------ chaseDivided pass: " + i++);
			if (newDivided.size()==0) break;
			JemEquivList adjacent = JemAdjacent.doYourJob(newDivided);
			if (adjacent.size()==0)  break;
			boolean doParts = isPartsList(adjacent);
			newDivided = doParts ? JemHashPartAll.doYourJob(adjacent) :
								   JemHashWireAll.doYourJob(adjacent);
		}
		Messenger.line("------ done  chaseDivided after "+i+" passes");
		//		JemStratCheck.doYourJob(myJemSets.starter);
		Messenger.freshLine();
	}
	

}
