/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratDebug.java
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



import java.util.HashMap;

import java.util.TreeMap;

import java.util.Iterator;



import com.sun.electric.tool.ncc.NccGlobals;

import com.sun.electric.tool.ncc.NccOptions;

import com.sun.electric.tool.ncc.basicA.Messenger;

import com.sun.electric.tool.ncc.trees.*;

import com.sun.electric.tool.ncc.lists.*;

import com.sun.electric.tool.ncc.jemNets.*;



/**

 * JemStratDebug performs the debugging function of the day.

*/

public class JemStratDebug extends JemStrat {

	// Constructor does everything

	private JemStratDebug(NccGlobals globals) {

		super(globals);

		NccOptions options = globals.getOptions();

		boolean savedVerbose = options.verbose;

		options.verbose = true;

		

		globals.println("begin JemStratDebug");

		globals.println("dumping active and mismatched JemEquivRecords");

		doFor(globals.getRoot());

		globals.println("end JemStratDebug");

		

		options.verbose = savedVerbose;

	}



    // ---------- the tree walking code ---------

	public JemLeafList doFor(JemEquivRecord er) {

		if (er.isLeaf()) {

			if (er.isActive() || er.isMismatched()) {

				globals.println(er.nameString());

				for (JemEquivRecord r=er; r.getParent()!=null; r=r.getParent()) {

					String reason = r.getPartitionReason();

					if (reason!=null) globals.println("   "+reason);

				}

				super.doFor(er);

			}

		} else {

			super.doFor(er);

		}

		return new JemLeafList();

	}

	

	public HashMap doFor(JemCircuit c) {

		globals.println(" "+c.nameString());

		return super.doFor(c);

	}



    /** 

	 * 

	 */

    public Integer doFor(NetObject n){

		globals.println("  "+n.toString());

        return CODE_NO_CHANGE;

    }

    

    // -------------------------- public method -------------------------------

	public static void doYourJob(NccGlobals globals) {

		new JemStratDebug(globals);

	}

}

