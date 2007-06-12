package com.sun.electric.tool.generator.infinity;

import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;

/** This class handles the "Job" aspects of the Infinity layout generator */
public class InfinityJob extends Job {
	static final long serialVersionUID = 0;
	
	public InfinityJob() {
		super ("Generate layout for Infinity", NetworkTool.getNetworkTool(), 
			   Job.Type.CHANGE, null, null, Job.Priority.ANALYSIS);
		startJob();
	}

	@Override
	public boolean doIt() throws JobException {
		new Infinity();
		return true;
	}

}
