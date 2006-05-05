/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: ProjSettings.java
*
* Copyright (c) 2003 Sun Microsystems and Static Free Software
*
* Electric(tm) is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* Electric(tm) is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Electric(tm); see the file COPYING.  If not, write to
* the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
* Boston, Mass 02111-1307, USA.
*/
package com.sun.electric.tool.generator.layout;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class ProjSettings {
	//----------------------------- Preference Data ---------------------------
	public int foo = 17;
	public boolean bar = false;
	public String wow = "wowWee a string";
	public Tool1 tool1 = new Tool1();
	public Tool2 tool2 = new Tool2();
	public static class Tool1 {
		public int a = 3;
		public boolean foo = true;
	}
	public static class Tool2 {
		public double x = 31.3;
		// Test breaking of cycles
		public Tool2 cycle;
		public Tool2() {
			cycle = this;
		}
	}

	//-------------------------- Preference Writer ----------------------------
	private static class Writer {
		private static final int INDENT_INCR = 4;
		private int indent = 0;
		private Set<Object> visited = new HashSet<Object>();
		private void indent() {
			for (int i=0; i<indent*INDENT_INCR; i++) {
				System.out.print(" ");
			}
		}
		private void prIndent(String msg) {
			indent();
			prln(msg);
		}
		private void writeClassField(String fieldName, Object pref) {
			Class prefClass = pref.getClass();
			prIndent("<"+fieldName+">");
			indent++;
			writeFields(pref, prefClass);
			indent--;
			prIndent("</"+fieldName+">");
		}
		private void writePrimitiveField(String fieldName, Object value) {
			prIndent("<"+fieldName+"> "+ value + " </"+fieldName+">");
		}
		private void writeFields(Object pref, Class prefClass) {
			Field[] publicFields = prefClass.getFields();
			for (int i = 0; i < publicFields.length; i++) {
		         String fieldName = publicFields[i].getName();
		         Class typeClass = publicFields[i].getType();
		         String fieldType = typeClass.getName();
		         try {
			         if (fieldType.equals("int")) {
			        	 writePrimitiveField(fieldName, publicFields[i].getInt(pref));
			         } else if (fieldType.equals("long")) {
			        	 writePrimitiveField(fieldName, publicFields[i].getLong(pref));
			         } else if (fieldType.equals("double")) {
			        	 writePrimitiveField(fieldName, publicFields[i].getDouble(pref));
			         } else if (fieldType.equals("boolean")) {
			        	 writePrimitiveField(fieldName, publicFields[i].getBoolean(pref));
			         } else if (fieldType.equals("java.lang.String")) {
			        	 writePrimitiveField(fieldName, publicFields[i].get(pref));
			         } else {
			        	 // some class
			        	 Object value = publicFields[i].get(pref);
			        	 if (visited.contains(value)) {
			        		 writePrimitiveField("ERROR", "Break cycle in preference graph");
			        	 } else {
			        		 visited.add(value);
			        		 writeClassField(fieldName, value);
			        	 }
			         }
		         } catch (IllegalAccessException e) {
		        	 LayoutLib.error(true, "Error accessing ProjPref field");
		         }
	         }
		}
	}
	//---------------------------- utility methods -----------------------------
	private static void prln(String msg) {
		System.out.println(msg);
	}
	
	//---------------------------- public methods -----------------------------
	public void write() {
		Writer wr = new Writer();
		wr.writeClassField("ProjPrefs", this);
	}
	public static void test() {
		prln("begin ProjPrefs test");

		ProjSettings pp = new ProjSettings();
		pp.write();
		
		prln("end ProjPrefs test");
	}
}
