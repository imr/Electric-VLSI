/**
 * Copyright 2005, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.electric.tool.simulation.eventsim.core.common;

import java.util.Vector;

/**
 * @author ib27688
 *
 */
public class Parameters {
	protected Vector<NameValuePair> myAttributes;
	
	public Parameters() {
		myAttributes= new Vector<NameValuePair>();
	}
	
	public String getValue(int n) {
		String result= null;
		if (0 <= n && n <myAttributes.size()) {
			result= myAttributes.elementAt(n).value;
		}
		return result;
	}
	
	public String getName(int n) {
		String result= null;
		if (0 <= n && n <myAttributes.size()) {
			result= myAttributes.elementAt(n).name;
		}
		return result;
	}
	
	public void add(String name, String value) {
		myAttributes.add(new NameValuePair(name, value));
	}
	
	public int size() {
		return myAttributes.size();
	}
	
	public void clear() {
		myAttributes.clear();
	}
	
	
	public String toString() {
		String result="Attributes: ";
		
		for (NameValuePair vp: myAttributes) {
			result+= "\n" + vp.toString();
		}
		
		return result;
	}
	
	protected class NameValuePair {
		public String name;
		public String value;
		
		public NameValuePair(String name, String value) {
			this.name= name;
			this.value= value;
		}
		 
		
		public String toString() {
			return "[ " + name + "= " + value + " ]";
		}
	} // NameValuePair
	
}
