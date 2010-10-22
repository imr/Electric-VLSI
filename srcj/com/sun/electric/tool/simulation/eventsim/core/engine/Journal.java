
package com.sun.electric.tool.simulation.eventsim.core.engine;

import com.sun.electric.tool.simulation.eventsim.core.globals.GlobalDefaults;
import com.sun.electric.tool.simulation.eventsim.core.globals.Globals;


/**
 *
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 * 
 * Common denominator for journalinig simulation results
 * @author ib27688
 */
abstract class Journal {
	
	/** Global settings */
	protected Globals globals= Globals.getInstance();
	
	/** record the data captured by an Object */
	void record(Object data) {
		Boolean globalLog= globals.booleanValue(GlobalDefaults.GLOBAL_LOG);
		boolean log= (globalLog != null)?
					globalLog.booleanValue()
				:	GlobalDefaults.GLOBAL_LOG_DEF;
		if (log) {		
			System.out.println(data);
		}
	}
	
	/** record an error */
	void error(Object data) {
		System.err.println(data);
	}
	
	/** record informational output */
	void info(Object data) {
		Boolean globalLog= globals.booleanValue(GlobalDefaults.GLOBAL_LOG);
		boolean log= (globalLog != null)?
					globalLog.booleanValue()
				:	GlobalDefaults.GLOBAL_LOG_DEF;
		if (log) {		
			System.out.println(data);
		}
	}
	
	/** set the output for all journal, errors, and info */
	abstract void setOutput(String fileName);
	
	
	/** set the output for the journal */
	abstract void setJournalOutput(String fileName);
	
	/** set the destination for errors */
	abstract void setErrorOutput(String fileName);
	
	/** set the destination for the info output */
	abstract void setInfoOutput(String fileName);
	
	/** close the output */
	abstract void done();
	
} // class Journal
