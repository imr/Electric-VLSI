/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TestEConfig.java
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
package com.sun.electric.util.config;

import org.junit.Assert;
import org.junit.Test;

import com.sun.electric.tool.util.concurrent.runtime.taskParallel.IThreadPool;
import com.sun.electric.util.config.EConfig.ConfigEntry;
import com.sun.electric.util.test.TestHelpers;

/**
 * @author fschmidt
 * 
 */
public class TestEConfig {

    @Test
    public void testEConfigString() {
        Configuration config = new EConfig();
        Assert.assertNotNull(config.lookupImpl(IThreadPool.class.getName()));
    }

    @Test
    public void testEConfigClass() {
        Configuration config = new EConfig();
        Assert.assertNotNull(config.lookupImpl(IThreadPool.class));
    }

    @Test
    public void testParametersConstructor() throws Exception {
        EConfig config = new EConfig();
        config.addConfigEntry(TestInterface.class,
                ConfigEntry.createForConstructor(ConcreteTestInterfaceImpl.class, "testStringConstructor"));
        Assert.assertNotNull(config.lookupImpl(TestInterface.class));
    }

    @Test
    public void testParametersFactoryMethod() throws Exception {
        EConfig config = new EConfig();
        config.addConfigEntry(TestInterface.class, ConfigEntry.createForFactoryMethod(
                ConcreteTestInterfaceImpl.class, "createInstance", "testStringFactoryMethod"));
        Assert.assertNotNull(config.lookupImpl(TestInterface.class));
    }

    @Test
    public void testRemove() throws Exception {
        EConfig config = new EConfig();
        config.addConfigEntry(TestInterface.class, ConfigEntry.createForFactoryMethod(
                ConcreteTestInterfaceImpl.class, "createInstance", "testStringFactoryMethod")); 
        Assert.assertNotNull(config.lookupImpl(TestInterface.class));
        config.removeConfigEntry(TestInterface.class);
        Assert.assertNull(config.lookupImpl(TestInterface.class));
    }

    public static interface TestInterface {

    }

    public static class ConcreteTestInterfaceImpl {
        public ConcreteTestInterfaceImpl(String test) {
            System.out.println("- in constructor");
            System.out.println(test);
        }

        public static ConcreteTestInterfaceImpl createInstance(String test) {
            System.out.println("- in factory method");
            return new ConcreteTestInterfaceImpl(test);
        }
    }
}
