/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Test.java
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
package com.sun.electric.tool.generator.layout;

import java.util.Iterator;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;

/**
 * Class Test just lets me run small test programs.
 */
public class Test extends Job {
	private static final double DEF_SIZE = LayoutLib.DEF_SIZE;

	public boolean doIt() {
		System.out.println("Begin Test");
		String libDir = "forDima/";
		String libName = "power2_gates"; 

		Library scratch = LayoutLib.openLibForRead(libDir+libName+".elib");

		Cell c = scratch.findNodeProto("jtagBuf_pwr{sch}");

		Netlist nl = c.getNetlist(true);
		int numNetIndices = nl.getNumNetworks();
		System.out.println("num net indices: "+numNetIndices);
		for (int i=0; i<numNetIndices; i++) {
			Network jn = nl.getNetwork(i);
			Iterator<String> ni = jn.getNames();
			if (ni.hasNext()) {
				System.out.println("    net: "+((String)ni.next()));
			} else {
				System.out.println("    net: noName");
			}
		}

		System.out.println("Done Test");
		return true;
	}
	
	public Test() {
		super("Generate Fill Cell Library", User.getUserTool(), Job.Type.CHANGE, 
			  null, null, Job.Priority.ANALYSIS);
		startJob();
	}
}
