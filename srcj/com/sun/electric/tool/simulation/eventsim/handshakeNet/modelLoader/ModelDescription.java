/*
 * Created on May 16, 2005
 *
 */
package com.sun.electric.tool.simulation.eventsim.handshakeNet.modelLoader;

import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.sun.electric.tool.simulation.eventsim.core.classRegistry.ClassRegistry;
import com.sun.electric.tool.simulation.eventsim.core.engine.Director;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder.ChannelRecord;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder.ComponentRecord;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder.ConnectionPoint;

/**
 *
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 * 
 * @author ib27688
 *
 * An aggregate for the list of connections and components
 * This is a singleton.
 */
public class ModelDescription {
	
	protected Director director= Director.getInstance();
	
	/** class registry for component classes */
	public ClassRegistry registry;
	
	/** the list of connections */
	public List<ChannelRecord> connectionRecordList;

	/** model name */
	public String name;
	
	/* A mapping from component names to components.
	 * Each name can appear only once - this is ensured by the code
	 * that reads XML input.
	 */
	public LinkedHashMap<String, ComponentRecord> componentNameMap;
	
	private ModelDescription() {
		name= null; 
		
		registry= ClassRegistry.getInstance();
		connectionRecordList = new LinkedList<ChannelRecord>();
		componentNameMap= new LinkedHashMap<String, ComponentRecord>();
		
	} // ModelDescription constructor

	private static ModelDescription instance= new ModelDescription();
	
	// singleton
	public static ModelDescription getInstance() {
		return instance;
	} // getInstance
	
	public void clear() {
		connectionRecordList.clear();
		componentNameMap.clear();
		name= null;
	} // clear
	

	public void load(String fileName) throws EventSimErrorException {
		ModelLoader ldr= new ModelLoader();
		try {
			ldr.loadFile(fileName);
		}
		catch (Exception e) {
			director.fatalError("Model load failed: " + e.getMessage());
		}
	} // load

	/**
	 * List the components and connectins in the model.
	 * Used in testing.
	 */
	public void print() {
		director.info("MODEL NAME: " + name);

		// go through components
		Iterator ci= componentNameMap.values().iterator();
		while (ci.hasNext()) {
			ComponentRecord cr= (ComponentRecord) ci.next();
			director.info("COMPONENT NAME: " + cr.name 
					+ " COMPONENT TYPE: " + cr.type.getName());
		}
		
		// go through connections
		ci= connectionRecordList.iterator();
		while (ci.hasNext()) {
			ChannelRecord cr= (ChannelRecord) ci.next();
			director.info("CONNECTION " + cr.name);
			
			// sources first
			Iterator si= cr.sources.iterator();
			while(si.hasNext()) {
				ConnectionPoint src= (ConnectionPoint)si.next();
				director.info("\t FROM COMPONENT " 
						+ src.componentName 
						+ ", AT TERMINAL "
						+ src.terminalName);
			}
			
			// now destinations
			Iterator di= cr.destinations.iterator();
			while (di.hasNext()) {
				ConnectionPoint dest= (ConnectionPoint)di.next();
				director.info("\t TO COMPONENT " 
						+ dest.componentName 
						+ ", AT TERMINAL "
						+ dest.terminalName);				
			}
		} // while ci has next
		
	} // print
	
	public String toString() {
		//System.out.println("MAP= " + componentNameMap);
		
		String result= "model " + name + ";";
		// go through components
		for (ComponentRecord cr : componentNameMap.values()) {
						
			if (cr.typeName != null) {
				result += "\n" + "component " + cr.name + " instanceof " 
					+ registry.getComponentInfo(cr.typeName).componentTypeName;
			}
			else if (cr.type != null){
				result += "\n" + "component " + cr.name + " instanceof " 
				+ registry.getComponentInfo(cr.type).componentTypeName;
			}
						
			if (cr.attributes != null && cr.attributes.size() > 0) {
				result+= " { \n";
				for (int i= 0; i< cr.attributes.size(); i++) {
					String name= cr.attributes.getName(i);
					String value= cr.attributes.getValue(i);
					result+= "\tparameter " + name + "= " + value + "\n"; 
				}
				result+= "}";
				
			}
			
		}	
		result+= "\n";
		// go through connections
		for (ChannelRecord cr : connectionRecordList) {
			result+= "\n" + "channel " + cr.name + " { ";
			
			// sources first
			Iterator si= cr.sources.iterator();
			while(si.hasNext()) {
				ConnectionPoint src= (ConnectionPoint)si.next();
				result += "\n" + "\tfrom " 
						+ src.componentName 
						+ "."
						+ src.terminalName;
			}
			
			// now destinations
			Iterator di= cr.destinations.iterator();
			while (di.hasNext()) {
				ConnectionPoint dest= (ConnectionPoint)di.next();
				result+= "\n" +"\tto " 
						+ dest.componentName 
						+ "."
						+ dest.terminalName;
			}
			
			if (cr.attributes != null && cr.attributes.size() > 0) {
				for (int i= 0; i< cr.attributes.size(); i++) {
					String name= cr.attributes.getName(i);
					String value= cr.attributes.getValue(i);
					result+= "\n\tparameter " + name + "= " + value; 
				}
			}
			
			result+= "\n}";
		} // while ci has next
		return result;
	}
	
} // class ModelDescription
