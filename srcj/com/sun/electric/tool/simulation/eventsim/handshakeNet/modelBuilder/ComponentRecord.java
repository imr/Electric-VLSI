/*
 * Created on May 16, 2005
 */
package com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder;

import com.sun.electric.tool.simulation.eventsim.core.common.Parameters;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.component.Component;



/**
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All rights reserved. Use is subject to license terms.
 * 
 * @author ib27688
 *
 * A record of a component read from the XML input.
 *
 */
public class ComponentRecord {

	/** component name */
	public String name;
	/** optional parameter */
	// public String parameter= null;
	
	public Parameters attributes= new Parameters();
	
	/** component class - point to the same item as the class registry */
	public Class type=null;

	public String typeName=null;
	
	/** the line where the component is encoutered first */
	public int line;
	
	/** the actual component */
	public Component component;
	
	public String toString() {
		String result= "Component record:\n"
			+ "name = " + name + "\n"
			+ "line= " + line + "\n";
		
		if (type != null) {
			result+= "class= " + type.getName() + "\n";
		}
		else {
			result+= "class = null\n";
		}
		
		if (typeName != null) {
			result+= "className= " + typeName + "\n";
		}
		else {
			result+= "class = null\n";
		}
		
		
		if (component!= null) {
			result+= "component= " + component + "\n";
		}
		else {
			result+= "component= null\n";
		}

		if (attributes != null) {
			result+= "attributes= " + attributes.toString() + "\n";
		}
		else {
			result+= "attributes= null\n";
		}
		
		return result;
	}
	
	
} // class ComponentRecord
