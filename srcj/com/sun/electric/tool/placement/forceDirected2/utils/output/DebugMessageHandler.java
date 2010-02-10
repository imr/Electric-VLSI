/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DebugMessageHandler.java
 * Written by Team 7: Felix Schmidt, Daniel Lechner
 * 
 * This code has been developed at the Karlsruhe Institute of Technology (KIT), Germany, 
 * as part of the course "Multicore Programming in Practice: Tools, Models, and Languages".
 * Contact instructor: Dr. Victor Pankratius (pankratius@ipd.uka.de)
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.placement.forceDirected2.utils.output;

import java.util.LinkedList;
import java.util.List;

/**
 * Parallel Placement
 * 
 * This class provides a message handler. Use this for generate output in
 * multiple threads. The synchronisation of stdout and electric doesn't work in
 * a multithreaded environment. This is a work arround. You can print all
 * messages in the messages in the main thread.
 */
public class DebugMessageHandler {

	public static List<String> messages = new LinkedList<String>();

	public synchronized static void printMessage(String message) {
		messages.add(message);
	}

	public synchronized static void printOnStdOut() {
		for (String msg : messages) {
			System.out.println(msg);
		}
	}

}
