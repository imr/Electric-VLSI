/*
 * Created on May 10, 2005
 *
 */
package com.sun.electric.tool.simulation.eventsim.core.classRegistry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.sun.electric.tool.simulation.eventsim.core.classRegistryLoader.ClassRegistryLoader;
import com.sun.electric.tool.simulation.eventsim.core.engine.Director;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;


/**
 *
 * Copyright © 2005 Sun Microsystems Inc. All rights reserved. Use is subject to license terms.
 * 
 * @author ib27688
 *
 * Registry of available component classes and their names/id's.
 * The name is a shorthand used in XML files that define structures that
 * need to be simulated. 
 * Note: full class names could be used instead of shorthands - for now
 * I opted not to use them for the sake of having more readable input.
 * This decision may need to be revisited. 
 */
public class ClassRegistry {
	
	private static int initialCapacity= 64;
	private static float loadFactor= 0.75f;

	// the only copy of the registry
	protected static ClassRegistry instance = new ClassRegistry();

	// registered components
	protected LinkedHashMap<String, ComponentInfo> components;
	protected LinkedHashMap<Class, ComponentInfo> componentsByClass;
	
	protected ClassRegistry() {
		components= new LinkedHashMap<String, ComponentInfo>(initialCapacity, loadFactor);
		componentsByClass= new LinkedHashMap<Class, ComponentInfo>(initialCapacity, loadFactor);
	} // constructor
	
	public static ClassRegistry getInstance() {
		return instance;
	} // getInstance
	
	public void clear() {
		components.clear();
		componentsByClass.clear();
	} // reset
	

	/** load the component registry form a file */
	public void load(String fileName) throws EventSimErrorException {
		ClassRegistryLoader loader= new ClassRegistryLoader();
		try {
			loader.load(fileName);
		}
		catch (Exception e) {
			Director.getInstance().fatalError(e.getMessage());
		}
	} // load

		
	/** Register a new class. If the class name 
	 * already has been registered, then return false and make
	 * no registration.
	 * 
	 * @param name class name
	 * @param c the class
	 * @return true if the class was successfully registered
	 */
	public boolean register(String name, ComponentInfo cInfo) {
		boolean contains= components.containsKey(name);		
		if (!contains) {
			components.put(name, cInfo);
			if (cInfo.componentType != null) {
				componentsByClass.put(cInfo.componentType, cInfo);
			}
		}
		return !contains;
	} // Register
	
	/** Return a component factory associated with the name.
	 * 
	 * @param name name for the component factory
	 * @return the component factory with the given name, null if not found
	 */	
	public Class getComponentType(String name) {
		if (components.get(name)==null) {
			return null;
		} else {
			return components.get(name).componentType;
		}
	} // get
	
	
	public int size() {
		return components.size();
	}

	/**
	 * When a component type was not accessible at the time of
	 * loading a registry, the registry can be updated with the
	 * component type when the type becomes known and accessible
	 * @param name component name
	 * @param type the class
	 * @param typeName full class name 
	 * @return true if successful, false if not
	 */
	public boolean SetComponentType(String name, Class type, String typeName) {
		boolean success= false;
		ComponentInfo ci= components.get(name);
		if (ci != null) {
			ci.componentType= type;
			ci.componentTypeName= typeName;
			componentsByClass.put(type, ci);
		}		
		return success;
	}
	
	/**
	 * Get a collection of component information structures
	 * @return colelction of component information structures
	 */
	public Collection<ComponentInfo> getInfoCollection() {
		return components.values();
	}
	
	/**
	 * Get a collection of component names
	 * @return collection of component names
	 */
	public  Collection<String> componentNames() {
		return components.keySet();
	}
	
	/**
	 * Get ComponentInfo for a comoponent with a given name.
	 * @param name component name
	 * @return ComponentInfo object for the component with the given name. null if
	 *         no such component is in the registry.
	 */
	public ComponentInfo getComponentInfo(String name) {
		return components.get(name);
	}
	
	public ComponentInfo getComponentInfo(Class c) {
		return componentsByClass.get(c);
	}

	public String toString() {
		String result= "Component registry: ";
		
		for (Entry e : components.entrySet()) {
			result+= "\nComponent " + e.getKey() + "\n";
			result+= ((ComponentInfo)e.getValue()).toString();
		}
		
		return result;
	}
	
} // ClassRegistry
