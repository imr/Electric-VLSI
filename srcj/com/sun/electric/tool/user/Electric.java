package com.sun.electric.tool.user;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.technologies.TecGeneric;
import com.sun.electric.technology.technologies.TecSchematics;
import com.sun.electric.technology.technologies.TecArtwork;
import com.sun.electric.technology.technologies.TecCMOS;
import com.sun.electric.technology.technologies.TecMoCMOS;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.Rectangle;
import javax.swing.*;

public final class Electric
{
	// ------------------------- private data ----------------------------

	public static void main(String[] args)
	{
		// Because of lazy evaluation, technologies aren't initialized unless they're referenced here
		Technology.setCurrent(TecGeneric.tech);
		Technology.setCurrent(TecSchematics.tech);
		Technology.setCurrent(TecArtwork.tech);
		Technology.setCurrent(TecCMOS.tech);
		Technology.setCurrent(TecMoCMOS.tech);

		// get information about the nodes
		NodeProto m1m2Proto = NodeProto.findNodeProto("mocmos:Metal-1-Metal-2-Con");
		NodeProto m2PinProto = NodeProto.findNodeProto("mocmos:Metal-2-Pin");
		NodeProto p1PinProto = NodeProto.findNodeProto("mocmos:Polysilicon-1-Pin");
		NodeProto m1PolyConProto = NodeProto.findNodeProto("mocmos:Metal-1-Polysilicon-1-Con");
		NodeProto pTransProto = NodeProto.findNodeProto("mocmos:P-Transistor");
		NodeProto nTransProto = NodeProto.findNodeProto("mocmos:N-Transistor");
		NodeProto cellCenterProto = NodeProto.findNodeProto("generic:Facet-Center");
		NodeProto wirePinProto = NodeProto.findNodeProto("schematic:Wire_Pin");
		NodeProto busPinProto = NodeProto.findNodeProto("schematic:Bus_Pin");
		NodeProto wireConProto = NodeProto.findNodeProto("schematic:Wire_Con");

		// get information about the arcs
		ArcProto m1Proto = ArcProto.findArcProto("mocmos:Metal-1");
		ArcProto m2Proto = ArcProto.findArcProto("mocmos:Metal-2");
		ArcProto p1Proto = ArcProto.findArcProto("mocmos:Polysilicon-1");
		ArcProto wireProto = ArcProto.findArcProto("schematic:wire");


		// create the first library
		Library mainLib = Library.newInstance("noname", null);
		Library.setCurrent(mainLib);

		// create a layout cell in the library
		Cell myCell = Cell.newInstance(mainLib, "test{lay}");
		NodeInst cellCenter = NodeInst.newInstance(cellCenterProto, new Point2D.Double(30.0, 30.0), cellCenterProto.getDefWidth(), cellCenterProto.getDefHeight(), 0, myCell);
		cellCenter.setVisInside();
		NodeInst metal12Via = NodeInst.newInstance(m1m2Proto, new Point2D.Double(-20.0, 20.0), m1m2Proto.getDefWidth(), m1m2Proto.getDefHeight(), 0.0, myCell);
		NodeInst contactNode = NodeInst.newInstance(m1PolyConProto, new Point2D.Double(20.0, 20.0), m1PolyConProto.getDefWidth(), m1PolyConProto.getDefHeight(), 0.0, myCell);
		NodeInst metal2Pin = NodeInst.newInstance(m2PinProto, new Point2D.Double(-20.0, 10.0), m2PinProto.getDefWidth(), m2PinProto.getDefHeight(), 0.0, myCell);
		NodeInst poly1Pin = NodeInst.newInstance(p1PinProto, new Point2D.Double(20.0, -20.0), p1PinProto.getDefWidth(), p1PinProto.getDefHeight(), 0.0, myCell);
		NodeInst transistor = NodeInst.newInstance(pTransProto, new Point2D.Double(0.0, -20.0), pTransProto.getDefWidth(), pTransProto.getDefHeight(), 0.0, myCell);
		NodeInst rotTrans = NodeInst.newInstance(nTransProto, new Point2D.Double(0.0, 0.0), nTransProto.getDefWidth(), nTransProto.getDefHeight(), Math.PI/4, myCell);

		// make arcs to connect them
		PortInst m1m2Port = metal12Via.getOnlyPortInst();
		PortInst contactPort = contactNode.getOnlyPortInst();
		PortInst m2Port = metal2Pin.getOnlyPortInst();
		PortInst p1Port = poly1Pin.getOnlyPortInst();
		PortInst transRPort = transistor.findPortInst("p-trans-poly-right");
		ArcInst metal2Arc = ArcInst.newInstance(m2Proto, m2Proto.getWidth(), m2Port, m1m2Port);
		ArcInst metal1Arc = ArcInst.newInstance(m1Proto, m1Proto.getWidth(), contactPort, m1m2Port);
		ArcInst polyArc = ArcInst.newInstance(p1Proto, p1Proto.getWidth(), contactPort, p1Port);
		ArcInst polyArc2 = ArcInst.newInstance(p1Proto, p1Proto.getWidth(), transRPort, p1Port);

		// export the two pins
		Export m1Export = Export.newInstance(myCell, metal12Via, m1m2Port, "in");
		Export p1Export = Export.newInstance(myCell, poly1Pin, p1Port, "out");
		System.out.println("Created cell " + myCell.describe());



		// create a schematics cell in the library
		Cell schemCell = Cell.newInstance(mainLib, "test{sch}");
		NodeInst pinLeft = NodeInst.newInstance(wirePinProto, new Point2D.Double(-10.0, 10.0), wirePinProto.getDefWidth(), wirePinProto.getDefHeight(), 0, schemCell);
		NodeInst pinRight = NodeInst.newInstance(wirePinProto, new Point2D.Double(10.0, 10.0), wirePinProto.getDefWidth(), wirePinProto.getDefHeight(), 0, schemCell);
		NodeInst pinCenter = NodeInst.newInstance(wirePinProto, new Point2D.Double(0.0, 10.0), wirePinProto.getDefWidth(), wirePinProto.getDefHeight(), 0, schemCell);
		NodeInst conDown = NodeInst.newInstance(wireConProto, new Point2D.Double(0.0, 0.0), wireConProto.getDefWidth(), wireConProto.getDefHeight(), 0, schemCell);
		PortInst pinLeftPort = pinLeft.getOnlyPortInst();
		PortInst pinRightPort = pinRight.getOnlyPortInst();
		PortInst pinCenterPort = pinCenter.getOnlyPortInst();
		PortInst conPort = conDown.getOnlyPortInst();
		ArcInst wire1Arc = ArcInst.newInstance(wireProto, wireProto.getWidth(), pinLeftPort, pinCenterPort);
		ArcInst wire2Arc = ArcInst.newInstance(wireProto, wireProto.getWidth(), pinRightPort, pinCenterPort);
		ArcInst wire3Arc = ArcInst.newInstance(wireProto, wireProto.getWidth(), conPort, pinCenterPort);


		// now up the hierarchy
		Cell higherCell = Cell.newInstance(mainLib, "higher{lay}");
		Rectangle2D bounds = myCell.getBounds();
		NodeInst instance1Node = NodeInst.newInstance(myCell, new Point2D.Double(0, 0),
			myCell.getDefWidth(), myCell.getDefHeight(), 0, higherCell);
		instance1Node.setExpanded();
		NodeInst instance1UNode = NodeInst.newInstance(myCell, new Point2D.Double(0, 100),
			myCell.getDefWidth(), myCell.getDefHeight(), 0, higherCell);

		NodeInst instance2Node = NodeInst.newInstance(myCell, new Point2D.Double(100, 0),
			myCell.getDefWidth(), myCell.getDefHeight(), Math.PI/2, higherCell);
		instance2Node.setExpanded();
		NodeInst instance2UNode = NodeInst.newInstance(myCell, new Point2D.Double(100, 100),
			myCell.getDefWidth(), myCell.getDefHeight(), Math.PI/2, higherCell);

		NodeInst instance3Node = NodeInst.newInstance(myCell, new Point2D.Double(200, 0),
			myCell.getDefWidth(), myCell.getDefHeight(), Math.PI, higherCell);
		instance3Node.setExpanded();
		NodeInst instance3UNode = NodeInst.newInstance(myCell, new Point2D.Double(200, 100),
			myCell.getDefWidth(), myCell.getDefHeight(), Math.PI, higherCell);

		NodeInst instance4Node = NodeInst.newInstance(myCell, new Point2D.Double(300, 0),
			myCell.getDefWidth(), myCell.getDefHeight(), Math.PI/2*3, higherCell);
		instance4Node.setExpanded();
		NodeInst instance4UNode = NodeInst.newInstance(myCell, new Point2D.Double(300, 100),
			myCell.getDefWidth(), myCell.getDefHeight(), Math.PI/2*3, higherCell);

		PortInst instance1Port = instance1Node.findPortInst("in");
		PortInst instance2Port = instance1UNode.findPortInst("in");
		ArcInst instanceArc = ArcInst.newInstance(m1Proto, m1Proto.getWidth(), instance1Port, instance2Port);
		System.out.println("Created cell " + higherCell.describe());


		// now up the hierarchy even farther
		Cell bigCell = Cell.newInstance(mainLib, "big{lay}");
		int arraySize = 20;
		double cellWidth = myCell.getDefWidth();
		double cellHeight = myCell.getDefHeight();
		for(int y=0; y<arraySize; y++)
		{
			for(int x=0; x<arraySize; x++)
			{
				NodeInst instanceNode = NodeInst.newInstance(myCell, new Point2D.Double(x*(cellWidth+2), y*(cellHeight+2)),
					cellWidth, cellHeight, 0, bigCell);
				if ((x%2) == (y%2)) instanceNode.setExpanded();
			}
		}
		System.out.println("Created cell " + bigCell.describe());

		// show some stuff
		instance1Node.getInfo();
		instance2Node.getInfo();
		instanceArc.getInfo();

		// display some cells
		EditWindow window1 = EditWindow.newInstance(myCell);
		EditWindow window2 = EditWindow.newInstance(schemCell);
		EditWindow window3 = EditWindow.newInstance(higherCell);
		EditWindow window4 = EditWindow.newInstance(bigCell);
		System.out.println("*********************** TERMINATED SUCCESSFULLY ***********************");
		System.out.println("************* Click and drag to Pan");
		System.out.println("************* Use CONTROL-CLICK to Zoom");
	}

	// ---------------------- private and protected methods -----------------

	// It is never useful for anyone to create an instance of this class
	private Electric()
	{
	}

	private static MessagesWindow cl;

	static
	{
		// create the command line
		cl = new MessagesWindow();
	}

	// ----------------------- public methods -------------------------------

	/** Equivalent to error(true, msg). */
	public static void error(String msg)
	{
		RuntimeException e = new RuntimeException(msg);
		e.printStackTrace();
		throw e;
	}

	/** If pred is true, print an error message, dump the stack, and
	 * stop Java execution dead in its tracks.
	 *
	 * Output goes to the Jose window as well as the Electric message
	 * window.  This allows us to see when a Java error occurs with
	 * respect to debugging messages printed on the C side (for example
	 * messages printed by dbmirror.cpp) and debugging messages printed
	 * on the Java side. */
	public static void error(boolean pred, String msg)
	{
		if (pred) error(msg);
	}

}
