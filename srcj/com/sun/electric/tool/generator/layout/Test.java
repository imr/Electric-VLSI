/*
 * Class Test just lets me run small test programs
 */
package com.sun.electric.tool.generator.layout;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;


import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;

public class Test extends Job {
	private static final double DEF_SIZE = LayoutLib.DEF_SIZE;

	public boolean doIt() {
		System.out.println("Begin Test");
		String libDir = "forDima/";
		String libName = "power2_gates"; 

		Library scratch = LayoutLib.openLibForRead(libName, 
												   libDir+libName+".elib");

		Cell c = scratch.findNodeProto("jtagBuf_pwr{sch}");

		Netlist nl = c.getNetlist(true);
		int numNetIndices = nl.getNumNetworks();
		System.out.println("num net indices: "+numNetIndices);
		for (int i=0; i<numNetIndices; i++) {
			JNetwork jn = nl.getNetwork(i);
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
