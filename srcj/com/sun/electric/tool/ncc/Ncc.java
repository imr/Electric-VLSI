/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Ncc.java
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
package com.sun.electric.tool.ncc;

import java.util.Date;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.basic.CellContext;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.result.NccResults;

/** Compare potentially multiple cells in a hierarchy. 
 * <p>This is the class that should be used by programs wishing to perform
 * netlist comparison. */
public class Ncc {
	private static void prln(String s) {System.out.println(s);}
	private static void pr(String s) {System.out.print(s);}

	private Ncc() {}
	
	private NccResults compare1(CellContext cc1, CellContext cc2,
			                    NccOptions options, Aborter aborter) {
		if (options.operation==NccOptions.LIST_ANNOTATIONS) {
			ListNccAnnotations.doYourJob(cc1.cell, cc2.cell);
			return new NccResults();
		} else {
	    	Date before = new Date();
			switch (options.operation) {
			  case NccOptions.FLAT_TOP_CELL:
				pr("Flat NCC top cells: "); break;
			  case NccOptions.FLAT_EACH_CELL:
				pr("Flat NCC every cell in the design: "); break;
			  case NccOptions.HIER_EACH_CELL:
				pr("Hierarchical NCC every cell in the design: "); break;
			  default:
				LayoutLib.error(true, "bad operation: "+options.operation);
			}
			prln(cc1.cell+"  "+cc2.cell);
			NccResults results = NccBottomUp.compare(cc1, cc2, options, aborter); 

			if (aborter.userWantsToAbort()) return results;
			

			prln("Summary for all cells: "+results.summary(options.checkSizes));
			Date after = new Date();
			prln("NCC command completed in: "+
			                   NccUtils.hourMinSec(before, after)+".");
			return results;
		}
    }
   
    // ------------------------- public method --------------------------------
	/** Compare two cells. We don't need to be able to abort */
    public static NccResults compare(Cell cell1, VarContext ctxt1, 
                                    Cell cell2, VarContext ctxt2, 
									NccOptions options) {
    	return compare(cell1, ctxt1, cell2, ctxt2, options, null); 
    }
	/** Compare two cells. We need to be able to abort */
    public static NccResults compare(Cell cell1, VarContext ctxt1, 
    		                         Cell cell2, VarContext ctxt2, 
									 NccOptions options, Job job) {
    	if (ctxt1==null) ctxt1 = VarContext.globalContext; 
    	if (ctxt2==null) ctxt2 = VarContext.globalContext; 
    	Ncc ncc = new Ncc();
    	NccResults results = ncc.compare1(new CellContext(cell1, ctxt1), 
    			                          new CellContext(cell2, ctxt2), 
    			                          options, new Aborter(job));
//    	if (options.checkNetEquivalenceMap) {
//    		// Tricky: copyNames only works from change jobs. Thus
//    		// copyNames call is possible from regressions (bean shell)
//    		// because Bean shell runs all jobs as change jobs. 
//    		// I tried to create a SchemNamesToLay.RenameJob() but
//    		// the bean shell exited before RenameJob() finished.
//    		SchemNamesToLay.copyNames(results);
//    	}
    	return results;
    }
}
