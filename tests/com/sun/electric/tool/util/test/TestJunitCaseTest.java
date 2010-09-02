/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TestJunitCaseTest.java
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

import junit.framework.Assert;

import org.junit.Test;

import com.sun.electric.util.test.TestHelpers;

/**
 * @author Felix Schmidt
 * 
 */
public class TestJunitCaseTest {

	@Test
	public void testTestByReflection() throws Exception {

		Object testClass = new TestClass();

		TestHelpers.invokePrivateMethod("testMethod", testClass);
	}

	@Test
	public void testTestByReflection2() throws Exception {
		Object testClass = new TestClass();
		String result = (String) TestHelpers.invokePrivateMethod("testMethod2", testClass);

		Assert.assertEquals(TestClass.testString, result);
	}

	@Test
	public void testTestByReflection3() throws Exception {
		Object testClass = new TestClass();
		String result = (String) TestHelpers.invokePrivateMethod("testMethod3", testClass, TestClass.testString);

		Assert.assertEquals(TestClass.testString, result);
	}

}
