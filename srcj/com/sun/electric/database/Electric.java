package com.sun.electric.database;

import com.sun.electric.technologies.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/*
 * The instructions needed to compile on Windows:
 * cd C:\DevelE\Electric\src\java\com\sun\dbmirror
 * javac -classpath C:\DevelE\Electric\lib\java\bsh-1.2b3.jar *.java
 * cd C:\DevelE\Electric\src\java
 * jar cf C:\DevelE\Electric\lib\java\dbmirror.jar com\sun\dbmirror\*.class
 */

/**
 * This class holds a number of static methods to exchange information
 * with the c-side database.
 *
 * <p>Neither the Jose implementation nor the Jose client should ever
 * create an instance of this class.
 */
public final class Electric
{
	public static void main(String[] args)
	{
		// initialize the database
		View.buildViews();

		// initialize the technologies
		Technology mocmos = TecMoCMOS.newTechnology();
		Technology.setCurrent(mocmos);
		System.out.println("Initialized technologies.  Current one is " + Technology.getCurrent().getTechName());

		// create the first library
		Library mainLib = Library.newLibrary("noname", null);
		Library.setCurrent(mainLib);
		System.out.println("Created library " + Library.getCurrent().getLibName());
//---------------------
		// create a cell in the library
		Cell myCell = mainLib.newCell("test{lay}");
//		Cell myCell = Cell.newCell(mainLib, "text{lay}");

		System.out.println("Created cell " + myCell.getProtoName());
		
		// create two pins in the Cell
		NodeProto m1PinProto = mocmos.findNodeProto("Metal-1-Pin");
//		NodeProto m1PinProto = Technology.findNodeProto("mocmos", "Metal-1-Pin");
//		NodeProto m1PinProto = Technology.findNodeProto("mocmos:Metal-1-Pin");
		NodeInst leftPin = m1PinProto.newInst(1000.0, 1000.0, 1.0, 1.0, 0.0, myCell);
		NodeInst rightPin = m1PinProto.newInst(5000.0, 1000.0, 1.0, 1.0, 0.0, myCell);
		System.out.println("Created two instances of " + m1PinProto.getProtoName() +
			" in '" + leftPin.toString() + "'" +
			" and '" + rightPin.toString() + "'");
		
		// connect the pins with an arc
		ArcProto m1Proto = mocmos.findArcProto("Metal-1");
//		Arcinst metal1Arc = m1Proto.newInst();
	}

	// In order to identify a C structure we've seen before, we keep a
	// global hashtable of C addresses and their corresponding java
	// structures.  This table gets updated automatically when a new
	// Electric object is created.
	// 
	// For memory efficiency, the hashtable of extra information isn't
	// allocated until we actually need it.  For that reason, the
	// hashtable is <code>private</code>, requiring even subclasses to
	// go through the <code>getElectricVar</code> and
	// <code>setVar</code> methods.
	//
	// <i>TODO: write accessor functions for other types, e.g.
	// <code>getIntVar</code>, etc.</i>

	// -------------------------- private types ---------------------------
	static class DoAskString implements Runnable
	{
		Tool t;
		String cmd;
		String msg;
		public DoAskString(Tool t, String cmd, String msg)
		{
			this.t = t;
			this.cmd = cmd;
			this.msg = msg;
		}
		public void run()
		{
//			asktoolString(t.getAddr(), cmd, msg);
		}
	}

	static class RemoveSlicerCmd implements Runnable
	{
		Slicer s;
		public RemoveSlicerCmd(Slicer s)
		{
			this.s = s;
		}
		public void run()
		{
			slicers.remove(s);
		}
	}

	// ------------------------- private data ----------------------------
	private static final String buildnum = "2003.08.06"; //  version
	private static HashMap cache = new HashMap(); // C address to java object
	private static ArrayList cmdQ = new ArrayList(); // commands to execute
	// when we get another slice
	private static ArrayList tools = new ArrayList();
	private static ArrayList windows = new ArrayList();
	private static ArrayList slicers = new ArrayList();
	private static boolean inSlice = false; // are we currently in our slice?
	private static CmdLine cl;
	private static int basePerLambda = 200; // nb internal units per lambda

	// ---------------------- private and protected methods -----------------
	// It is never useful for anyone to create an instance of this class
	private Electric()
	{
	}

	static {
		cl = new CmdLine();
		System.setOut(new java.io.PrintStream(cl));
	}

	// find the java object representing the C object at addr
	static ElectricObject objectFromAddress(int addr)
	{
		Integer i = new Integer(addr);
		ElectricObject obj = (ElectricObject) cache.get(i);
		return obj;
	}

	private static void buildPathAddrs(
		ArrayList nodeInstAddrs,
		ArrayList nodeProtoAddrs,
		VarContext context)
	{
		if (context == VarContext.globalContext)
			return;
		buildPathAddrs(nodeInstAddrs, nodeProtoAddrs, context.pop());
		NodeInst ni = context.getNodeInst();
		NodeProto np = ni.getProto().getEquivalent();
//		nodeInstAddrs.add(new Integer(ni.getAddr()));
//		nodeProtoAddrs.add(new Integer(np.getAddr()));
	}

	// Convert Electric internal units to lambda units
//	static double baseToLambda(int base)
//	{
//		return base / (double) basePerLambda;
//	}
	// Convert lambda units to Electric internal units
	static int lambdaToBase(double lam)
	{
		return ElectricObject.round(lam * basePerLambda);
	}

	final static Iterator getWindows()
	{
		return windows.iterator();
	}

	/** it's dbmirror's turn to do things to the database.  Make any
	 * destructive calls here */
	private static void slice()
	{
		inSlice = true;

		// dequeue any commands, and execute them.
		while (cmdQ.size() > 0)
		{
			Object obj = cmdQ.get(0);
			cmdQ.remove(0);
			if (obj instanceof String[])
			{
				String cmds[] = (String[]) obj;
				doExec(cmds);
			} else if (obj instanceof Runnable)
			{
				Runnable ex = (Runnable) obj;
				ex.run();
			}
		}
		inSlice = false;

		// now run them slicers
		Iterator i = slicers.iterator();
		while (i.hasNext())
		{
			Slicer s = (Slicer) i.next();
			s.doSlice();
		}
	}

	/** sends "asktool(tool, cmd, msg)" to the specified tool */
//	protected static native int asktoolString(
//		int taddr,
//		String cmd,
//		String msg);

	/** get the first two numbers of in the version number.  For
	 * example, if java.vm.version is: "1.4.1_01-b01" then return the
	 * double: 1.4 */
	private static double jvmVerNb(String jvmVer)
	{
		// look for the regular expression:  ^[0-9]+.[0-9]+
		int dot = jvmVer.indexOf(".");
		int end = dot + 1;
		while (Character.isDigit(jvmVer.charAt(end)))
			end++;
		String jvmVerPrefix = jvmVer.substring(0, end);
		return Double.parseDouble(jvmVerPrefix);
	}

	/** Print the version of this code.  This is called by
	 * src/misc/dbmirrortool.c to make sure that Java works */
	private static void version()
	{
		final double minJvmVer = 1.4;
		System.out.println("Java Electric Database mirror, build " + buildnum);

		// Now check the JVM version
		String jvmVerStr = System.getProperty("java.vm.version");
		double jvmVerNb = jvmVerNb(jvmVerStr);
		if (jvmVerNb < minJvmVer)
		{
			String msg =
				"Warning: this Java Virtual Machine: "
					+ jvmVerStr
					+ "\n"
					+ "    is too old for Jose. "
					+ " Please change your LD_LIBRARY_PATH\n"
					+ "    to point to a JVM that is at least as recent as: "
					+ minJvmVer;
			System.out.println(msg);
		}
	}

	/** Print out the available commands */
	private static void doHelp()
	{
		System.out.println("dbmirror tool commands:");
		System.out.println(
			"  count: returns the number of known electric objects");
		System.out.println(
			"  stats: gives some statistics about the kinds of known objects");
		System.out.println("  check: runs a sanity check on the database");
		System.out.println(
			"  info <hexaddr>: print info about an object at given address");
		System.out.println(
			"  exec <class> [<args>...]: run the static void main(String args[]) method of <class>");
		System.out.println(
			"  hilite <hexaddr>...: cause the c-side editor to hilight the object at given address");
		System.out.println("  mem: print statistics about Java's memory usage");
		System.out.println("  help: print this message");
		System.out.println("  otherwise, attempt to evaluate the line in Java");
	}

	private static void doMem()
	{
		Runtime rt = Runtime.getRuntime();
		System.out.println("Total memory: " + rt.totalMemory());
		System.out.println("Free memory: " + rt.freeMemory());
		//	System.out.println("Max memory: "+rt.maxMemory());

	}

//	private static void doHilite(String args[])
//	{
//		if (args.length < 2)
//		{
//			System.out.println("Usage: hilite <hexaddr> ...");
//			return;
//		}
//		ArrayList list = new ArrayList();
//		for (int i = 1; i < args.length; i++)
//		{
//			ElectricObject e = objectFromAddress(Integer.parseInt(args[i], 16));
//			if (e == null)
//			{
//				System.out.println("No object at address " + args[i]);
//			} else if (!(e instanceof Geometric))
//			{
//				System.out.println(e + " isn't a NodeInst or ArcInst");
//			} else
//			{
//				list.add(e);
//			}
//		}
//		if (list.size() > 0)
//		{
//			hilite(list);
//		}
//	}

	/** Execute the public static void main(String args[]) method of
	 * the class identified as args[0]. */
	private static void doExec(String args[])
	{
		if (args.length < 2)
		{
			System.out.println("exec requires the name of a class with");
			System.out.println(
				"a public static void main(String args[]) method");
			return;
		}
		// use our own classloader to load the classes so we can nuke
		// (reload) them later.  Each class gets its own classloader,
		// and all of the classloaders are remembered in a hashtable.
		// To unload a class, just remove its loader from the table.
		try
		{
			Class c = ClassReloader.classForName(args[1]);
			Class[] targs = new Class[1];
			targs[0] = args.getClass();
			Method m = c.getMethod("main", targs);
			String margs[] = new String[args.length - 2];
			System.arraycopy(args, 2, margs, 0, margs.length);
			Object margset[] = new Object[1];
			margset[0] = margs;
			m.invoke(null, margset);
		} catch (ClassNotFoundException cnfe)
		{
			System.out.println(
				"Can't find the class named "
					+ args[1]
					+ ".  Check the classpath or package");
		} catch (NoSuchMethodException mnfe)
		{
			System.out.println("Class " + args[1] + " doesn't contain");
			System.out.println(
				"a public static void main(String args[]) method");
		} catch (IllegalAccessException iae)
		{
			System.out.println(
				"Can't access the main method of "
					+ args[1]
					+ ": IllegalAccessException");
		} catch (InvocationTargetException ite)
		{
			System.out.println("There is a bug in the exec'd code:");
			ite.getTargetException().printStackTrace(System.out);
		}
	}

	/** Return the number of Java Electric objects held in the
	 * reference table. */
	private static void doCount()
	{
		System.out.println(cache.size() + " objects known");
	}

	/** Print out statistics of the objects in the reference table. */
	private static void doStats()
	{
		doCount();
		HashMap hm = new HashMap();
		Iterator i = cache.values().iterator();
		while (i.hasNext())
		{
			ElectricObject e = (ElectricObject) i.next();
			Class type = e.getClass();
			if (hm.containsKey(type))
			{
				Integer v = (Integer) hm.get(type);
				hm.put(type, new Integer(v.intValue() + 1));
			} else
			{
				hm.put(type, new Integer(1));
			}
		}
		i = hm.keySet().iterator();
		while (i.hasNext())
		{
			Class c = (Class) i.next();
			Integer v = (Integer) hm.get(c);
			System.out.println(c.getName() + ": " + v.intValue());
			if (v.intValue() <= 10 || c.getName().endsWith("Tool"))
			{
				Iterator j = cache.values().iterator();
				while (j.hasNext())
				{
					ElectricObject e = (ElectricObject) j.next();
					Class cc = e.getClass();
					if (cc == c)
					{
						System.out.println("  * " + e);
					}
				}
			}
		}
	}

	/** Get detailed information about a specific object */
	private static void doInfo(String[] cmds)
	{
		// cmds= hex code of address.
		if (cmds.length < 2)
		{
			// snag the USER_highlighted string array
			String[] strs =
				(String[]) findTool("user").getVar("USER_highlighted");
			if (strs == null || strs.length == 0)
			{
				System.out.println(
					"info usage: telltool dbmirror info <hexaddress>");
			} else
			{
				// parse those strings for 
				for (int i = 0; i < strs.length; i++)
				{
					System.out.println("---info: " + strs[i]);
					int addridx = strs[i].indexOf("FROM=");
					if (addridx >= 0)
					{
						int addrend = strs[i].indexOf(";", addridx);
						int addr =
							Integer.parseInt(
								strs[i].substring(addridx + 5, addrend),
								8);
						ElectricObject obj = objectFromAddress(addr);
						if (obj != null)
						{
							obj.showInfo();
						} else
						{
							System.out.println(
								"   no object at 0"
									+ Integer.toOctalString(addr));
						}
					}
				}
			}
			return;
		}
		int addr = Integer.parseInt(cmds[1], 16);
		ElectricObject obj = objectFromAddress(addr);
		if (obj == null)
		{
			System.out.println("No object at " + Integer.toHexString(addr));
		} else
		{
			obj.showInfo();
		}
	}

	// -------------------------- native methods ---------------------------------

	// The newNodeInst argument order: lx, ly, hx, hy was deliberately
	// chosen to be different from Electric's newnodeinst argument order
	// because in Electric, newnodeinst is inconsistent with
	// modifynodeinst, modifynodeinsts, nodesizeoffset and
	// nodeprotosizeoffset. Steve should have fixed newnodeinst's
	// inconsistency in Electric.  However, since he didn't, I'm
	// fixing it in the Jose-Electric interface.  This prevents me from
	// getting too confused.
	protected static native NodeInst newNodeInst(
		int proto,
		int lx,
		int ly,
		int hx,
		int hy,
		int trans,
		int rot,
		int parent);
	protected static native void modifyNodeInst(
		int inst,
		int lx,
		int ly,
		int hx,
		int hy,
		int rot,
		int trans);
	protected static native ArcInst newArcInst(
		int proto,
		int width,
		int nodeA,
		int portA,
		int xa,
		int ya,
		int nodeB,
		int portB,
		int xb,
		int yb,
		int parent);
	protected static native void modifyArcInst(int inst, int wid);
	protected static native Cell newCell(String name, int lib);
	protected static native Export newPortProto(
		int cell,
		int nodeinst,
		int portproto,
		String name);
//	protected static native Library openLib(String name, String path);
	protected static native void setVariable(
		int addr,
		int type,
		String name,
		int value,
		int valtype);
	protected static native void setVariableString(
		int addr,
		int type,
		String name,
		String value);
	protected static native void delVariable(int addr, int type, String name);
	// addr is C address of object to be deleted.  faddr is Cell
	// address and is only used when deleting Exports.  Return true if
	// error.
	protected static native boolean delElectricObject(
		int addr,
		int type,
		int faddr);

	//
	// This is a truly disgusting KLUDGE! RKao
	//
	// When a Jose client makes a change to a Cell, the ancestors of that
	// Cell must potentially have their bounds updated.  For the sake of
	// efficiency, Electric sometimes postpones this computation until
	// after a number of changes have been made. Unfortunately, Electric
	// sometimes delays notifying Jose of an update to a Cell's bounds
	// until it is too late; Jose uses an obsolete bounds to compute the
	// bounds for a NodeInst.  The result is that the NodeInst gets scaled
	// and positioned incorrectly.
	//
	// My attempted solution is to explicitly ask Electric to update the
	// bounds whenever the Jose client does something that will eventually
	// directly or indirectly the bounds.  This is error prone.  I hope
	// that it works.
	protected static native void updatecellBounds(int cellAddr);
	protected static native boolean ncc(
		int cell1Addr,
		int[] nodeInstAddrs,
		int[] nodeProtoAddrs,
		int cell2Addr,
		double absTolerance,
		boolean checkExportNames,
		boolean checkSizes,
		boolean hierarchical,
		boolean ignorePwrGnd,
		boolean interactive,
		boolean mergeParallel,
		boolean mergeSeries,
		double percentTolerance,
		boolean preAnalyze,
		boolean recurse,
		boolean verboseText,
		boolean verboseGraphics);

	// ----------------------- public methods -------------------------------

	/** Ask a c-side tool to perform some action, whose argument is a
	 * string. */
	public static void askTool(Tool t, String cmd, String msg)
	{
		DoAskString das = new DoAskString(t, cmd, msg);
		//addCmd(das);
	}

	/** Cause the c-side display to highlight a set of items
	 * @param items an ArrayList of the Electric objects you want highlighted */
//	public static void hilite(ArrayList items)
//	{
//		StringBuffer sb = new StringBuffer();
//		for (int i = 0; i < items.size(); i++)
//		{
//			if (sb.length() != 0)
//			{
//				sb.append("\n");
//			}
//			Geometric g = (Geometric) items.get(i);
//			sb.append(
//				"CELL="
//					+ g.getParent().getProtoName()
//					+ " FROM=0"
//					+ Integer.toOctalString(g.getGeomPtr())
//					+ ";-1;0");
//		}
//		DoAskString cmd;
//		if (sb.length() == 0)
//		{
//			cmd = new DoAskString(findTool("user"), "clear", "");
//		} else
//		{
//			cmd =
//				new DoAskString(
//					findTool("user"),
//					"show-multiple",
//					sb.toString());
//		}
//		//addCmd(cmd);
//	}

	/** Find the tool with a particular name.  See the Electric internals
	 * manual for examples of Tools.
	 * @param name the name of the desired tool
	 * @return the Tool with the same name, or null if no tool matches. */
	public static Tool findTool(String name)
	{
		for (int i = 0; i < tools.size(); i++)
		{
			Tool t = (Tool) tools.get(i);
			if (t.getName().equals(name))
				return t;
		}
		System.out.println("Couldn't find tool named '" + name + "'.");
		return null;
	}

	/** Get an iterator over all tools. */
	public static Iterator getTools()
	{
		return tools.iterator();
	}

	/** Set number of Elecric internal units per lambda. <br> This
	 * defaults to 200 internal units per lambda.
	 *
	 * <p>In an attempt to simplify the Jose programming interface, I'm
	 * doing something very different from what Electric does.  I'm
	 * having the programmer declare how large lambda is in terms of
	 * Electric's internal database units.
	 *
	 * <p>At first I thought I would simply try to mirror Electric's
	 * model. However I realized that there's no way to get the units
	 * right when a Jose client asks for the size of a PrimitiveNode.
	 * The reason is that the number of internal units per lambda
	 * depends upon the Library into which the instance of the NodeProto
	 * is going. If you have two Libraries open, each with different
	 * units per lambda then there is no correct answer to the question:
	 * "How large is this PrimitiveNode in lambda?".
	 *
	 * <p>I may have to revisit this once I start writing programs to
	 * create schematics. */
	public static void setBasePerLambda(int i)
	{
		basePerLambda = i;
	}

	/** Get number of Elecric internal units per lambda */
	public static int getBasePerLambda()
	{
		return basePerLambda;
	}

	/** Return the WindowPart that is editing a particular Cell.
	 * <p>
	 * TODO: does this really work? */
	public final static WindowPart findWindowPart(Cell f)
	{
		for (int i = 0; i < windows.size(); i++)
		{
			WindowPart wp = (WindowPart) windows.get(i);
			wp.showInfo();
			if (wp.getEditing() == f)
				return wp;
		}
		return null;
	}

	/** Add a Slicer to the list of classes that want time to execute
	 * something on a regular basis. */
	public static void addSlicer(Slicer s)
	{
		slicers.add(s);
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

	/** Execute a command.  Called by dbmirrortool.c when the user
	 * types "-" then "telltool dbmirror <cmds>".
	 * Also called by CmdLine.  You must break the command up into
	 * individual strings before calling <code>interpret</code>.
	 * <p>
	 * Current commands include <code>help</code>, <code>count</code>,
	 * <code>stats</code>, <code>check</code>, <code>info</code>,
	 * <code>hilite</code>, <code>mem</code> and <code>exec</code>. */
	public static void interpret(String cmds[])
	{
		if (cmds.length == 0 || cmds[0].equals("help"))
		{
			doHelp();
		} else if (cmds[0].equals("count"))
		{
			doCount();
		} else if (cmds[0].equals("stats"))
		{
			doStats();
		} else if (cmds[0].equals("info"))
		{
			doInfo(cmds);
		} else if (cmds[0].equals("mem"))
		{
			doMem();
		} else
		{
			// try to execute it
			cl.interpret(cmds);
		}
	}

}
