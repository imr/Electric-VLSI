/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratResult.java
 *
 * Copyright (c) 2003 Sun Microsystems and Free Software
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

import com.sun.electric.tool.ncc.trees.NetObject;

import com.sun.electric.tool.ncc.trees.JemEquivRecord;

import com.sun.electric.tool.ncc.lists.JemLeafList;



public class JemStratResult extends JemStrat {

	// ------------------------ private data and methods ----------------------

	private JemLeafList mismatched, active;



	// constructor does all the work

	private JemStratResult(JemLeafList mismatched, JemLeafList active,

						   NccGlobals globals) {

		super(globals);

		this.mismatched = mismatched;

		this.active = active;

		mismatched.clear();

		active.clear();

		doFor(globals.getRoot());

	}

	

	// -------------------------- walk tree -----------------------------------

	public JemLeafList doFor(JemEquivRecord r) {

		if (r==globals.getPorts()) {

			return new JemLeafList();

		} else if (r.isLeaf()) {

			if (r.isActive()) {

				active.add(r);	

			} else if (r.isMismatched()) {

				mismatched.add(r);

			}

			return new JemLeafList();

		} else {

			return super.doFor(r);

		}

	}



	public Integer doFor(NetObject n) {return CODE_NO_CHANGE;}



	// --------------------------- real public method -------------------------	

	/**

	 * Walk JemEquivRecord tree to find active and mismatched leaf records

	 * @param mismatched returned ist of mismatched leaf records 

	 * @param active returned list of active leaf records 

	 * @param globals NccGlobals

	 * @return true if matched (all leaf records retired)

	 */

	public static boolean doYourJob(JemLeafList mismatched, JemLeafList active,

						  			NccGlobals globals) {

		new JemStratResult(mismatched, active, globals);

		return mismatched.size()==0 && active.size()==0;		

	}

}