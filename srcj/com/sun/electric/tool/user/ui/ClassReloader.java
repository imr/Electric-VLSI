package com.sun.electric.tool.user.ui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * The ClassReloader class provides a method for finding a class by name
 * and <i>reloading</i> it, if the .class file has changed since the last
 * time we referenced it.  For a .class file to be reloadable, it <b>must</b>
 * be placed in a directory called "reloadable" which is itself within
 * the <tt>CLASSPATH</tt> search path for Java.
 * <p>
 * Usage: call ClassReloader.classForName("classname") whenever you need
 * a new instance of the class.
 * <p>
 * Special considerations: each of the reloadable classes referenced by
 * the initially requested classname lives in a special namespace belonging
 * to that class.  If the top-level class changes, new versions of each of
 * the subordinate classes will be loaded.  Old versions can't talk to 
 * new versions.  Classes not in the reloadable directory can be shared
 * by everybody.  A change in one of the subordinate classes will NOT be
 * noticed.
 */
public class ClassReloader extends ClassLoader
{
	private File mainsrc;
	private long srcmodified;

	/**
	 * every top-level-referenced class lives in its own ClassLoader world.
	 * This allows us to reload the class by dropping its class loader
	 * and creating a new one.  Class loaders (of type ClassReloader)
	 * are indexed here by the name of the requested class.
	 */
	private static HashMap loaders = new HashMap();

	/**
	 * remove class information about a particular class name.  There
	 * should be no reason whatsoever to use this method.
	 */
	public static void disposeClass(String name)
	{
		loaders.remove(name);
	}

	/**
	 * find a class with a specific name whose .class file resides in 
	 * a directory called "<code>reloadable</code>" that is on the normal
	 * Java search path.  The class and its dependent classes will be
	 * reloaded into Java if the .class file has changed since it was
	 * last found.
	 */
	public static Class classForName(String name) throws ClassNotFoundException
	{
		ClassReloader cl = (ClassReloader) loaders.get(name);
		// System.out.println("ClassReloader is looking for "+name);
		if (cl != null && cl.isOld())
		{
			//  System.out.println("New version of "+name+" detected.");
			cl = null;
		}
		if (cl == null)
		{
			cl = new ClassReloader();
			loaders.put(name, cl);
		}
		return cl.loadClass(name);
	}

	protected boolean isOld()
	{
		/*
		if (mainsrc==null) {
		    System.out.println("don't have a source file");
		} else if (!mainsrc.exists()) {
		    System.out.println("source file doesn't exist");
		} else {
		    System.out.println("New version: "+mainsrc.lastModified()+
				       ", old version: "+srcmodified);
		}
		*/
		return mainsrc != null
			&& mainsrc.exists()
			&& mainsrc.lastModified() != srcmodified;
	}

	protected Class findClass(String name) throws ClassNotFoundException
	{
		byte[] b = loadClassData(name);
		return defineClass(name, b, 0, b.length);
	}

	private byte[] loadClassData(String name) throws ClassNotFoundException
	{
		String searchPath = System.getProperty("java.class.path", ".");
		//System.out.println("File.pathSeparator="+File.pathSeparator+"   File.separator="+File.separator);
		//System.out.println("searchPath= '"+searchPath+"'");
		String filename = name.replace('.', File.separatorChar);
		// split search path on ":"; add sep+"reloadable"+sep+name
		int loc = 0;
		int end = 1;
		String path;
		while (end >= 0)
		{
			end = searchPath.indexOf(File.pathSeparator, loc);
			if (end < 0)
			{
				path = searchPath.substring(loc);
			} else
			{
				path = searchPath.substring(loc, end);
			}
			//System.out.println("ClassReloader checking "+path);
			if (!path.endsWith(".jar"))
			{
				path =
					path
						+ File.separator
						+ "reloadable"
						+ File.separator
						+ filename
						+ ".class";
				//System.out.println("   ...checking for '"+path+"'");
				File f = new File(path);
				if (f.exists())
				{
					//System.out.println("...found file in "+path);
					try
					{
						byte[] data = new byte[(int) f.length()];
						BufferedInputStream bis =
							new BufferedInputStream(new FileInputStream(f));
						bis.read(data);
						bis.close();
						if (mainsrc == null)
						{
							mainsrc = f;
							srcmodified = f.lastModified();
						}
						return data;
					} catch (IOException fnfe)
					{
						System.out.println(
							"error deep in ClassReloader.loadClassData...");
						System.out.println(fnfe);
					}
				}
			}
			loc = end + 1;
		}
		throw new ClassNotFoundException(
			"Problem loading class data for " + name);
	}

}
