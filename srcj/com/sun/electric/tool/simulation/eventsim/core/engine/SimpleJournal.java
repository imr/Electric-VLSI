package com.sun.electric.tool.simulation.eventsim.core.engine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.sun.electric.tool.simulation.eventsim.core.globals.GlobalDefaults;

/**
 *
 * Copyright © 2005 Sun Microsystems Inc. All rights reserved. Use is subject to license terms.
 * 
 * @author ib27688
 */
class SimpleJournal extends Journal {

	/** writer for the simulation log */
	protected PrintWriter outWriter;
	/** writer for error messages */
	protected PrintWriter errWriter;
	/** writer for info messages */
	protected PrintWriter infoWriter;
	
	public SimpleJournal() {
		// set the writers, turn buffering on
		outWriter= new PrintWriter(System.out, true);
		// detect Electric
		try
		{
			Class.forName("com.sun.electric.Main");
			// Electric, use System.out
			errWriter= new PrintWriter(System.out, true);
			
		} catch (ClassNotFoundException e)
		{
			// no Electric, use system.err
			errWriter= new PrintWriter(System.err, true);
		}

		infoWriter= new PrintWriter(System.out, true);
	} // constructor
	
	public SimpleJournal(String fileName) {
		setOutput(fileName);
	} // constructor
	
	
	/** a pre-built version of the journal */
	protected static Journal myInstance;
	
	/** the only way to get an instance of a SimpleJournal */
	public static Journal getInstance() {
		if (myInstance == null) myInstance= new SimpleJournal();
		return myInstance;
	} // get Instance
	
	public void record(Object o) {
		Boolean globalLog= globals.booleanValue(GlobalDefaults.GLOBAL_LOG);
		boolean log= (globalLog != null)?
					globalLog.booleanValue()
				:	GlobalDefaults.GLOBAL_LOG_DEF;
		if (log) {		
			outWriter.println(o);
		}
	} // record
	
	public void error(Object o) {
		errWriter.println(o);
	} // error

	public void info(Object o) {
		Boolean globalLog= globals.booleanValue(GlobalDefaults.GLOBAL_LOG);
		boolean log= (globalLog != null)?
					globalLog.booleanValue()
				:	GlobalDefaults.GLOBAL_LOG_DEF;
		if (log) {		
			infoWriter.println(o);
		}
	} // info
		
	/** redirect standard output */
	public void setJournalOutput(String fileName) {
		try {
			outWriter= new PrintWriter(
					new BufferedWriter (new FileWriter(fileName)));
		}
		catch (IOException e) {
			// do nothing - keep the output as it was
			System.err.println("Not able to set journal output file: " + fileName);
		}
	} // setJournalOutput
	
	public void setErrorOutput(String fileName) {
		try {
			errWriter= new PrintWriter(
					new BufferedWriter (new FileWriter(fileName)));
		}
		catch (IOException e) {
			// do nothing - keep the output as it was
			System.err.println("Not able to set error output file: " + fileName);
		}		
	} // setErrorOutput

	
	public void setInfoOutput(String fileName) {
		try {
			infoWriter= new PrintWriter(
					new BufferedWriter (new FileWriter(fileName)));
		}
		catch (IOException e) {
			// do nothing - keep the output as it was
			System.err.println("Not able to set info output file: " + fileName);

		}		
	} // setInfoOutput

	
	public void setOutput(String fileName) {
		try {
			outWriter= new PrintWriter(
					new BufferedWriter (new FileWriter(fileName)));
			errWriter=outWriter;
			infoWriter= outWriter;
		}
		catch (IOException e) {
			System.err.println("Not able to set common output file: " + fileName);
			// do nothing - keep the output as it was
		}				
	} // setOutputFile
	
	public void done() {
		if (outWriter != null ) outWriter.close();
		if (errWriter != null 
				&& errWriter != outWriter) errWriter.close();
		if (infoWriter != null 
				&& infoWriter != outWriter 
				&& infoWriter != errWriter) infoWriter.close();
	}
	
} // simplejournal
