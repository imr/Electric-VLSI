/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TestPlayground.java
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.util;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestPlayground {

    public interface ExtEnum {
        public boolean optional();
    }
    
    public enum ExtEnumee implements ExtEnum {
        test1(), test2(true);

        private boolean opt = false;
        
        ExtEnumee() {
            opt = false;
        }

        ExtEnumee(boolean optional) {
            this.opt = optional;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.electric.util.TestPlayground.ExtEnum#optional()
         */
        public boolean optional() {
            return opt;

        }

    }

    @Before
    public void printLine() {
        System.out.println("===================================================");
    }

    private enum TestEnum {
        test1, test2;
    }

    @Ignore
    @Test
    public void testEnum() {
        this.deepSearch(TestEnum.class);
    }

    @Test
    public void testUserHome() {
        System.out.println("User Property: " + System.getProperty("user.home"));
    }

    @Ignore
    @Test
    public void printMethodNames() {
        Method[] methods = TestPlayground.class.getMethods();
        for (Method method : methods) {
            System.out.println(method.getName());
        }
    }

    @Ignore
    @Test
    public void testInstance() {
        Number n = new Double(12.3);
        Assert.assertTrue(n instanceof Number);
        Assert.assertTrue(n instanceof Double);

        Class<?> c1 = n.getClass();
        System.out.println(c1);

        Method[] methods = TestPlayground.class.getMethods();
        for (Method method : methods) {
            System.out.println(method.getName() + ": ");
            for (Class<?> c : method.getParameterTypes())
                System.out.println("  parameter: " + c.getName());
        }

    }

    @Ignore
    @Test
    public void testDeepSearch() {
        deepSearch(Double.class);
    }

    public void testFunc(Number n) {

    }

    public void deepSearch(Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        System.out.println(clazz.getName());
        for (Class<?> c : clazz.getInterfaces()) {
            deepSearch(c);
        }

        deepSearch(clazz.getSuperclass());
    }

    @Test
    public void testURI() throws URISyntaxException {
        String uri = "jar:file:/E:/workspaceElectric2/electric-public/target/electric-9.0-SNAPSHOT-bin.jar!/econfig.xml";
        URI tmpUri = new URI(uri);
        System.out.println(tmpUri);
        File file = new File(tmpUri.toString());
        System.out.println(file.exists());
    }
}
