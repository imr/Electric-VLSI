/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Messenger.java
 *
 * Copyright (c) 2003 Sun Microsystems and Free Software
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
 */
package com.sun.electric.tool.ncc.basicA;

import com.sun.electric.tool.generator.layout.LayoutLib;

import java.io.*;
import java.util.Date;

public class Messenger {
	private PrintStream logStrm;
	
	public Messenger(String logFileName){
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
	public void print(String s){
		System.out.print(s);
		if (logStrm!=null) logStrm.print(s);
	}
	/** print with trailing newline */
	public void println(String s){
		System.out.println(s);
		if (logStrm!=null) logStrm.println(s);
	}
	/** print newline */
	public void println(){
		System.out.println();
		if (logStrm!=null) logStrm.println("");
	}
	public void flush() {
		System.out.flush();
		if (logStrm!=null) logStrm.flush();
	}
	/** print, dump stack, and halt */
    public void error(boolean pred, String msg){
		flush();
    	LayoutLib.error(pred, msg);
    }
	
}
