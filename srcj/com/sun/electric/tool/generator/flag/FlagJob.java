package com.sun.electric.tool.generator.flag;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.ncc.basic.CellContext;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.routing.SeaOfGates;

/** This class handles the "Job" aspects of the Infinity layout generator */
public class FlagJob extends Job {
	static final long serialVersionUID = 0;
	static private void prln(String msg) {System.out.println(msg);}
	private SeaOfGates.SeaOfGatesOptions prefs;
	
	public FlagJob() {
		super ("Generate layout for Infinity", NetworkTool.getNetworkTool(), 
			   Job.Type.CHANGE, null, null, Job.Priority.ANALYSIS);
		prefs = new SeaOfGates.SeaOfGatesOptions();
        prefs.getOptionsFromPreferences();
		startJob();
	}

	@Override
	public boolean doIt() throws JobException {
		// figure out what to work on
		CellContext cc = NccUtils.getCurrentCellContext();
		if (cc==null) return true;
		Cell cell = cc.cell;
		
		if (!cell.isSchematic()) {
			prln("Current cell must be a schematic for which you wish to generate Infinity layout");
			return true;
		}

		new Flag(cell, this, prefs);
		return true;
	}

}
