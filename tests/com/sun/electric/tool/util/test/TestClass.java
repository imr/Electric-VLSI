/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TestClass.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.util.test;

import com.sun.electric.tool.util.test.TestByReflection;

/**
 * @author Felix Schmidt
 * 
 */
public class TestClass {

	public static final String testString = "TestString";

	@SuppressWarnings("unused")
	@TestByReflection(testMethodName = "testMethod")
	private void testMethod() {

	}

	@SuppressWarnings("unused")
	@TestByReflection(testMethodName = "testMethod2")
	private String testMethod2() {
		return testString;
	}
	
	@SuppressWarnings("unused")
	@TestByReflection(testMethodName = "testMethod3")
	private String testMethod3(String value) {
		return value;
	}
}
