/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TestHelpers.java
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

import java.lang.reflect.Method;


/**
 * @author Felix Schmidt
 * 
 */
public class TestHelpers {

	public static Object invokePrivateMethod(String name, Object obj, Object... parameters) throws Exception {

		Class<?>[] classes = null;

		if (parameters != null) {
			classes = new Class<?>[parameters.length];
			int i = 0;
			for (Object paramObj : parameters) {
				classes[i] = paramObj.getClass();
				i++;
			}
		}

		Method method = obj.getClass().getDeclaredMethod(name, classes);

		if (method.isAnnotationPresent(TestByReflection.class)) {
			TestByReflection testby = method.getAnnotation(TestByReflection.class);
			if (testby.testMethodName().equals(name)) {
				method.setAccessible(true);
				return method.invoke(obj, parameters);
			} else {
				throw new Exception("");
			}
		}
		return null;
	}
}
