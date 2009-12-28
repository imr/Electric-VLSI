/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NccJob.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.generator.flag.hornFunnel2;

import java.util.List;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.basic.CellContext;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.user.ncc.NccMsgsFrame;

/* Implements the NCC user command */
public class SkewTreeJob extends Job {
    static final long serialVersionUID = 0;

	private void prln(String s) {System.out.println(s);}
	private void pr(String s) {System.out.print(s);}
	
	// Some day we may run this on server
	@Override
    public boolean doIt() {
		SkewTree.doIt();
		return true;
    }
    @Override
    public void terminateOK() {}

	// ------------------------- public methods -------------------------------
    
	public SkewTreeJob() {
		super("Run SkewTree", NetworkTool.getNetworkTool(), Job.Type.CHANGE, null, 
			  null, Job.Priority.ANALYSIS);
		
		startJob();
	}
}
