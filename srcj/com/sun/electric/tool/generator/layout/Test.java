/*
 * Class Test just lets me run small test programs
 */
package com.sun.electric.tool.generator.layout;

import java.util.Iterator;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;

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
			Iterator ni = jn.getNames();
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
		super("Generate Fill Cell Library", User.tool, Job.Type.CHANGE, 
			  null, null, Job.Priority.ANALYSIS);
		startJob();
	}
}
