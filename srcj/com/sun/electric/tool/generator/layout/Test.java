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

import com.sun.electric.database.topology.NodeInst;

import com.sun.electric.database.topology.PortInst;

import com.sun.electric.database.prototype.NodeProto;

import com.sun.electric.database.hierarchy.Cell;

import com.sun.electric.database.hierarchy.Export;

import com.sun.electric.database.hierarchy.HierarchyEnumerator;

import com.sun.electric.database.hierarchy.Nodable;

import com.sun.electric.database.hierarchy.Library;

import com.sun.electric.technology.PrimitiveNode;



public class Test extends Job {



	private String getHomeDir() {

		String homeDir = null;

		if (LayoutLib.userName().equals("rkao")) {

			boolean nfsWedged = false;

			if (LayoutLib.osIsWindows()) {

				// windows

				if (nfsWedged) {

					homeDir = "c:/a1/kao/Sun/";

				} else {

					homeDir = "x:/";

				}

			} else {

				// unix

				homeDir = "/home/rkao/";

			}

		} else {

			LayoutLib.error(true, "unrecognized user");

		}

		return homeDir;

	}



	public void doIt() {

		System.out.println("Begin Test");

		

		Library scratch = LayoutLib.openLibForModify(

			"scratch", 

			getHomeDir()+"work/async/scratch.elib");



		Cell emptyCell = Cell.newInstance(scratch, "emptyCell{lay}");



		Cell lowCell = Cell.newInstance(scratch, "lowCell{lay}");

		NodeInst.newInstance(Tech.m1m2, new Point2D.Double(1,2),

							 5, 9, 0, lowCell, null);

		Cell hiCell = Cell.newInstance(scratch, "hiCell{lay}");

		Cell hiCell2 = Cell.newInstance(scratch, "hiCell2{lay}");

		

		Rectangle2D b = lowCell.getBounds();

		NodeInst lowInst = 

			NodeInst.newInstance(lowCell, new Point2D.Double(0,0), b.getWidth(),

								 b.getHeight(), 0, hiCell, null);

		System.out.println("Instance coordinates: "+lowInst.getGrabCenter());

		NodeInst lowInst2 = 

			NodeInst.newInstance(lowCell, new Point2D.Double(0,0), b.getWidth(),

								 b.getHeight(), 0, hiCell2, null);

		

		NodeInst.newInstance(Tech.p1m1, new Point2D.Double(0,0), 5,

							 5, 0, hiCell, null);

		

		AffineTransform xform = lowInst.rotateOut();

		xform.concatenate(lowInst.translateOut());

		Point2D p = new Point2D.Double(0,0);

		System.out.println("lowInst origin in hiCell"+xform.transform(p, null));

		System.out.println("lowInst bounds:"+lowInst.getBounds());

		

		System.out.println("Done Test");

	}

	

	public Test() {

		super("Run Gate regression", User.tool, Job.Type.CHANGE, 

			  null, null, Job.Priority.ANALYSIS);

		startJob();

	}

}

