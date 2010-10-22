/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Flag.java
 *
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.generator.flag;

import java.lang.reflect.Constructor;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.routing.SeaOfGates;

public class Flag {
	private static void prln(String s) {Utils.prln(s);}
	
	private void doEverything(Cell schCell, Job flagJob, SeaOfGates.SeaOfGatesOptions prefs) {
		FlagAnnotations ann = new FlagAnnotations(schCell);
		if (!ann.isAutoGen()) {
			prln("Cell: "+schCell.libDescribe()+" has no autoGen annotation");
			return;
		}
		String className = ann.getAutoGenClassName();
		Class layGenClass = null; 
		try {
			layGenClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			prln("Can't find layout generator class: "+className);
			return;
		}
		Constructor<FlagDesign> layGenConstructor = null; 
		try {
			layGenConstructor = layGenClass.getConstructor(FlagConstructorData.class);
		} catch (NoSuchMethodException e) {
			prln("Layout generator class: "+className+
				 " has no contructor that takes arguments: (FlagConstructorData)");
			return;
		}
		
        Library autoLib = schCell.getLibrary();
        String groupName = schCell.getCellName().getName();
		prln("Generate layout for Cell: "+groupName);
		prln("Using layout generator: "+className);

        Cell layCell = Cell.newInstance(autoLib, groupName+"{lay}");
        layCell.setTechnology(Technology.getCMOS90Technology());
        
        try {
        	layGenConstructor.newInstance(
        			new FlagConstructorData(layCell, schCell, flagJob, prefs));	
        } catch (Throwable th) {
        	prln("Layout generator: "+className+" threw Exception: "+th.getMessage());
        	prln("Printing stack trace:");
        	Utils.printStackTrace(th);
        	th.printStackTrace();
        }
	}
	
	public Flag(Cell schCell, Job flagJob, SeaOfGates.SeaOfGatesOptions prefs) {
		try {
			doEverything(schCell, flagJob, prefs);
		} catch (Throwable th) {
			prln("Oh my! Something went wrong.");
        	Utils.printStackTrace(th);
			th.printStackTrace();
		}
	}
}
