package com.sun.electric.database;

import com.sun.electric.technologies.*;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public final class Electric
{
	public static void main(String[] args)
	{
		// Make MOSIS CMOS the current technology
		Technology.setCurrent(TecMoCMOS.tech);

		// create the first library
		Library mainLib = Library.newInstance("noname", null);
		Library.setCurrent(mainLib);
		System.out.println("Created library " + Library.getCurrent().getLibName());

		// create a cell in the library
		Cell myCell = Cell.newInstance(mainLib, "text{lay}");
		System.out.println("Created cell " + myCell.describe());

		// get information about the metal-1-pin, poly-1-pin, and metal-1-poly-1-contact
		NodeProto m1PinProto = NodeProto.findNodeProto("mocmos:Metal-1-Pin");
		double m1PinWidth = m1PinProto.getDefWidth();
		double m1PinHeight = m1PinProto.getDefHeight();
		NodeProto p1PinProto = NodeProto.findNodeProto("mocmos:Polysilicon-1-Pin");
		double p1PinWidth = m1PinProto.getDefWidth();
		double p1PinHeight = m1PinProto.getDefHeight();
		NodeProto m1PolyConProto = NodeProto.findNodeProto("mocmos:Metal-1-Polysilicon-1-Con");
		double m1PolyConWidth = m1PolyConProto.getDefWidth();
		double m1PolyConHeight = m1PolyConProto.getDefHeight();
		
		// put the contact at the top-right, a metal-1 arc going left, a poly-1 arc going down
		NodeInst contactNode = NodeInst.newInstance(m1PolyConProto, new Point2D.Double(50.0, 50.0), m1PolyConWidth, m1PolyConHeight, 0.0, myCell);
		NodeInst metal1Pin = NodeInst.newInstance(m1PinProto, new Point2D.Double(10.0, 50.0), m1PinWidth, m1PinHeight, 0.0, myCell);
		NodeInst poly1Pin = NodeInst.newInstance(p1PinProto, new Point2D.Double(50.0, 10.0), p1PinWidth, p1PinHeight, 0.0, myCell);
		System.out.println("Created 3 nodes: " + contactNode.describe() + ", " + metal1Pin.describe() + ", and " + poly1Pin.describe());

		// get information about the metal-1 and poly-1 arcs
		ArcProto m1Proto = TecMoCMOS.tech.findArcProto("Metal-1");
		double m1Width = m1Proto.getWidth();
		ArcProto p1Proto = TecMoCMOS.tech.findArcProto("Polysilicon-1");
		double p1Width = p1Proto.getWidth();

		// make two arcs to connect them
		PortInst contactPort = contactNode.getPort();
		PortInst m1Port = metal1Pin.getPort();
		PortInst p1Port = poly1Pin.getPort();
		ArcInst metalArc = ArcInst.newInstance(m1Proto, m1Width, contactPort, m1Port);
		ArcInst polyArc = ArcInst.newInstance(p1Proto, p1Width, contactPort, p1Port);
		System.out.println("Created two arcs to join them");

		contactNode.getInfo();
		metal1Pin.getInfo();
		poly1Pin.getInfo();
		metalArc.getInfo();
		polyArc.getInfo();
		System.out.println("*********************** TERMINATED SUCCESSFULLY ***********************");
	}

	// ------------------------- private data ----------------------------

	private static CmdLine cl;

	// ---------------------- private and protected methods -----------------

	// It is never useful for anyone to create an instance of this class
	private Electric()
	{
	}

	static {
		cl = new CmdLine();
		System.setOut(new java.io.PrintStream(cl));
	}

	// ----------------------- public methods -------------------------------

	/** Execute a command.
	 */
	public static void interpret(String cmds[])
	{
		if (cmds[0].equals("mem"))
		{
			Runtime rt = Runtime.getRuntime();
			System.out.println("Total memory: " + rt.totalMemory());
			System.out.println("Free memory: " + rt.freeMemory());
		} else
		{
			// try to execute it
			cl.interpret(cmds);
		}
	}

	/** Equivalent to error(true, msg). */
	public static void error(String msg)
	{
		RuntimeException e = new RuntimeException(msg);
		// Oddly enough, the following prints a stack trace in the
		// Electric message window only
		e.printStackTrace();

		// while the following prints a stack trace in the Jose window
		// only.
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
