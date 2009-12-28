/*
 * Created on Jan 21, 2005
 *
 */
package com.sun.electric.tool.simulation.eventsim.core.hierarchy;

/**
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All rights reserved. Use is subject to license terms.
 * 
 * @author ib27688
 *
 * The collection of all entities in the simulation.
 * 
 */

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class Entities {

	// note redundancy below - at the minimum, we need only one
	// map and a list, or two maps
	
	/** the list of entities */
	private static List<Entity> entityList = new LinkedList<Entity>();
	/** An index of entities - by name */
	private static Map<String, Entity> nameToEntityMap= new HashMap<String, Entity>();
	/** An index of entities - by ID */
	private static Map<Long, Entity> idToEntityMap = new HashMap<Long, Entity>();
	
	/** The constructor is empty and private - an instance of this
	 * class cannot be created.
	 *
	 */
	private Entities() {
		// empty
	}
	
	public static void add(Entity e) {
		// extract the name
		String name= e.getName();
		// extract ID, turn it into an object
		Long id= new Long(e.getID());
		// add the entity to the list, and add the mappings
		entityList.add(e);
		nameToEntityMap.put(name, e);
		idToEntityMap.put(id, e);
	} // add
	
	public static Entity find(String name) {
		return (Entity) nameToEntityMap.get(name);
	} // find
	
	public static Entity find(long id) {
		return (Entity) idToEntityMap.get(new Long(id));
	} // find
	
	public static Iterator<Entity> iterator() {
		return entityList.iterator();
	}
	
	/**
	 * Remove all entities.
	 *
	 */
	public static void clear() {
		entityList.clear();
		nameToEntityMap.clear();
		idToEntityMap.clear();
	} // clear 

} // entities

	
