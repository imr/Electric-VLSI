/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Messenger.java
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

/**
 * Messenger prints to System.out. It also prints to a log file if one
 * has been set.
 *
 * TODO: This is a greatly simplified version of Ivan's messenger. I
 * left it an object because that's the way he had it. Right now it
 * doesn't need to be an object. However I need to figure out what to
 * do to make this thread safe.
 */
package com.sun.electric.tool.ncc.basicA;

import java.io.*;
import java.util.Date;

public class Messenger{
	private static PrintStream logStrm;
	
	public static Messenger toTestPlease(String s){return new Messenger();}

	/** Specify a log file. */
	public static void setLogFile(String logFileName){
		File f = new File(logFileName);
		System.out.println("Log file: "+f.getAbsolutePath());
		FileOutputStream fileStrm=null;
		try {
			fileStrm = new FileOutputStream(logFileName);
		} catch (Exception e) {
			String msg = "can't write to log file: "+logFileName;
			throw new RuntimeException(msg);
		}
		BufferedOutputStream bufStrm = new BufferedOutputStream(fileStrm);
		logStrm = new PrintStream(bufStrm);
		Date d= new Date();
		logStrm.println(logFileName+" file started "+d);
	}
	/** print without trailing newline */
	public void say(String s){
		System.out.print(s);
		if (logStrm!=null) logStrm.print(s);
	}
	
	/** print with trailing newline */
	public void line(String s){
		System.out.println(s);
		if (logStrm!=null) logStrm.println(s);
	}
	
	/** print newline */
	public void freshLine(){
		System.out.println();
		logStrm.println("");
	}

	/** print, dump stack, and halt */
    public void error(String s){
    	String msg = "Error: "+s;
    	RuntimeException e = new RuntimeException(msg);
    	
    	// send stack dump to Electric console
		e.printStackTrace(System.out);
		
		// send stack dump to debugger console
		e.printStackTrace();
		
		// send stack dump to log file
		if (logStrm!=null) e.printStackTrace(logStrm);
		
		// flush all printout before we stop
		System.out.flush();
		if (logStrm!=null) logStrm.flush();
		throw e;
    }
	
}
