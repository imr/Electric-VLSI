/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 *  
 */

/*
 * Created on Feb 2, 2005
 *
 */
package com.sun.electric.tool.simulation.eventsim.core.globals;

/**
 * @author ib27688
 */

public class GlobalDefaults {


	public static final String GLOBAL_LOG= "globalLog";
	public static final boolean GLOBAL_LOG_DEF= false;
	
	//public static final String JOURNAL_STATE= "journalState";
	public static final boolean JOURNAL_STATE_DEF= true;
	
	//public static final String JOURNAL_ACTION= "journalAction";
	public static final boolean JOURNAL_ACTION_DEF= false;

	public static final String JOURNAL_TERMINAL= "journalTerminal";
	public static final boolean JOURNAL_TERMINAL_DEF= false;

	
	public static final String JOURNAL_ARBITER= "journalArbiter";
	public static final boolean JOURNAL_ARBITER_DEF= false;
	
	public static final int logDetails= 0;
	public static final int logForPlayback= 1;
	
	public static final String LOG_LEVEL= "logLevel";
	public static final int LOG_LEVEL_DEF= logForPlayback;	

} // class GlobalDefaults
