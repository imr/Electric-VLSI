package com.sun.electric.database;

import com.sun.electric.technologies.*;


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
		System.out.println("Created cell " + myCell.getProtoName());
		
		// create two pins in the Cell
		NodeProto m1PinProto = TecMoCMOS.tech.findNodeProto("Metal-1-Pin");
//		NodeProto m1PinProto = Technology.findNodeProto("mocmos", "Metal-1-Pin");
//		NodeProto m1PinProto = Technology.findNodeProto("mocmos:Metal-1-Pin");
		NodeInst leftPin = NodeInst.newInstance(m1PinProto, 1000.0, 1000.0, 1.0, 1.0, 0.0, myCell);
		NodeInst rightPin = NodeInst.newInstance(m1PinProto, 5000.0, 1000.0, 1.0, 1.0, 0.0, myCell);
		System.out.println("Created two instances of " + m1PinProto.getProtoName() +
			" in '" + leftPin.toString() + "'" + " and '" + rightPin.toString() + "'");
		
		// connect the pins with an arc
		ArcProto m1Proto = TecMoCMOS.tech.findArcProto("Metal-1");
		PortInst leftPort = leftPin.getPort();
		PortInst rightPort = rightPin.getPort();
		double m1Width = m1Proto.getWidth();
		ArcInst arc = ArcInst.newInstance(m1Proto, m1Width, leftPort, rightPort);
		System.out.println("Created an arc to join them");
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
		if (pred)
			error(msg);
	}

}
