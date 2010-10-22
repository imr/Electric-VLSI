/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 *  
 */

/*
 * Created on Jun 9, 2005
 * 
 */
package com.sun.electric.tool.simulation.eventsim.core.globals;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.globals.loader.GlobalsLoader;

/**
 * @author ib27688
 *
 * Values of global parameters, loaded by a GlobalsLoaderXML from an 
 * XML file.
 */
public class Globals {
	
	/** maps values to keywords/names */
	private static Map<String,String> values;
	
	/** the defaulet name of the file where global values are set */
	public static final String DEF_GLOBALS_FILE= "Settings.fglob";

	/** This class is intended to hold global flags - it is a singleton.
	 */
	private Globals() {
		values= new HashMap<String,String>();
	} // private constructor
	
	/** a private instance - singleton pattern */
	private static Globals instance=null;
	
	/** get instance - singleton pattern */
	public static Globals getInstance() {
		if (Globals.instance == null) Globals.instance= new Globals();
		return Globals.instance;
	} // getInstance
	
	public void load() throws EventSimErrorException {
		load(DEF_GLOBALS_FILE);
	} // load

	
	/** load is additive, old values are not thrown out 
	 * @throws EventSimErrorException */
	public void load(String file) throws EventSimErrorException {
		GlobalsLoader ldr= new GlobalsLoader();
		ldr.load(file);
	} // load
 	
	/** Clear parameters and their values */
	public void clear() {
		if (values != null) values.clear();
	} // clear
	
	public Object value(String param) {
		return values.get(param);
	} // value

	
	/** 
	 * Return the value as an integer.
	 * Return null if the parameter is not defined or
	 * if the value is of a different data type. 
	 * @param param parameter name
	 * @return parameter value or null
	 */
	public Integer intValue(String param) {
		// we know that all values are stored as strings
		String value= (String)values.get(param);
		Integer intValue= null;
		try {
			// convert string to integer
			intValue= Integer.valueOf(value);
		}
		catch (Exception e) {
			// not an int, return null
			intValue= null;
		}
		return intValue;
	} // intValue
	
	/** 
	 * Return the value as a long.
	 * Return null if the parameter is not defined or
	 * if the value is of a different data type. 
	 * @param param parameter name
	 * @return parameter value or null
	 */
	public Long longValue(String param) {
		// we know that all values are stored as strings
		String value= (String)values.get(param);
		Long longValue= null;
		try {
			// convert string to integer
			longValue= Long.valueOf(value);
		}
		catch (Exception e) {
			// not an int, return null
			longValue= null;
		}
		return longValue;
	} // longValue
	
	/** 
	 * Return the value as a double.
	 * Return null if the parameter is not defined or
	 * if the value is of a different data type. 
	 * @param param parameter name
	 * @return parameter value or null
	 */
	public Double doubleValue(String param) {
		// we know that all values are stored as strings
		String value= (String)values.get(param);
		Double doubleValue= null;
		try {
			// convert string to integer
			doubleValue= Double.valueOf(value);
		}
		catch (Exception e) {
			// not an int, return null
			doubleValue= null;
		}
		return doubleValue;
	} // doubleValue

	/** 
	 * Return the value as a double.
	 * Return null if the parameter is not defined or
	 * if the value is of a different data type. 
	 * @param param parameter name
	 * @return parameter value or null
	 */
	public Boolean booleanValue(String param) {
		// we know that all values are stored as strings
		String value= (String)values.get(param);
		if (value == null) return null;
		
		Boolean booleanValue= null;
		try {
			// convert string to integer
			booleanValue= Boolean.valueOf(value);
		}
		catch (Exception e) {
			// not an int, return null
			booleanValue= null;
		}
		return booleanValue;
	} // booleanValue

	
	/** 
	 * Return the value as a String.
	 * Return null if the parameter is not defined or
	 * if the value is of a different data type. 
	 * @param param parameter name
	 * @return parameter value or null
	 */	
	public String stringValue(String param) {
		return (String)values.get(param);
	} // stringValue
	
	
	public boolean hasParam(String param) {
		return values.containsKey(param);
	} // hasParam

	/** convert globals map to a string, mostly for testing */
	public String toString() {
 		String valueString= "";
		
		Iterator ki= values.keySet().iterator();
		while (ki.hasNext()) {
			String key= (String)ki.next();
			String value= (String)values.get(key);
			valueString+= key + "= " + value + "\n";
		} // while
		return valueString;
		/* return values.toString(); */
	}
	
	/** Set a parameter programatically. Can also be sed to 
	 * change the value of a parameter.
	 * @param parameter parameter name
	 * @param value parameter value in String format
	 */
	public void setParameter(String parameter, String value) {
		values.put(parameter, value);
	} //setValue
	
	
    /** test 
     * @throws EventSimErrorException */
    public static void main(String[] args) throws EventSimErrorException {
    		Globals glob= Globals.getInstance();
    		glob.load("globalsTest.xml");
    		System.out.println(glob);
    		
    		Integer lLevel= glob.intValue(GlobalDefaults.LOG_LEVEL);
    		System.out.println("lLevel= " + lLevel);
		int logLevel= (lLevel != null)
			? lLevel.intValue()
			: 42;    		
    		System.out.println("logLevel= " + logLevel);
    		
    		Globals glob1= Globals.getInstance();
    		System.out.println(glob1);
    		
    } // main

	
} // Globals
