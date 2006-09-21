/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Pie.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.ncc;

import com.sun.electric.tool.generator.layout.LayoutLib;

import java.lang.reflect.Constructor;

/** Reflective interface to Port Interchange Experiment. This allows us to compile Electric
 * without the plugin: com.sun.electric.plugins.pie */
public class Pie {
	private Class pieNccJobClass;
	private Constructor pieNccJobConstructor;

	// singleton
	private static final Pie pie = new Pie();
	
	private Pie() {
		try {
			pieNccJobClass = Class.forName("com.sun.electric.plugins.pie.NccJob");
			pieNccJobConstructor = pieNccJobClass.getConstructor(new Class[] {Integer.TYPE});
		} catch (Throwable e) {
			pieNccJobClass = null;
			pieNccJobConstructor = null;
		}
	}
	
	private static void prln(String msg) {System.out.println(msg);}
	
	public static boolean hasPie() {
		return pie.pieNccJobClass!=null;
	}
	public static void invokePieNcc(int numWind) {
		LayoutLib.error(!hasPie(), "trying to invoke non-existant PIE");
		try {
			pie.pieNccJobConstructor.newInstance(new Integer(numWind));
		} catch (Throwable e) {
			prln("Invocation of pie NccJob threw Throwable: "+e);
			e.printStackTrace();
		}
	}
}
