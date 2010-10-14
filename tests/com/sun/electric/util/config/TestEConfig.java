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
import org.junit.Ignore;
import org.junit.Test;

import com.sun.electric.tool.util.concurrent.runtime.taskParallel.IThreadPool;
import com.sun.electric.util.config.annotations.Inject;
import com.sun.electric.util.config.annotations.InjectionMethod;
import com.sun.electric.util.config.annotations.InjectionMethod.InjectionStrategy;
import com.sun.electric.util.config.model.ConfigEntry;

/**
 * @author fschmidt
 * 
 */
public class TestEConfig {

    @Test
    public void testEConfigString() {
        Configuration config = new EConfigContainer();
        Assert.assertNotNull(config.lookupImpl(IThreadPool.class.getName()));
    }

    @Test
    public void testEConfigClass() {
        Configuration config = new EConfigContainer();
        Assert.assertNotNull(config.lookupImpl(IThreadPool.class));
    }

    @Test
    public void testParametersConstructor() throws Exception {
        EConfigContainer config = new EConfigContainer();
        config.addConfigEntry(
                TestInterface.class.getName(),
                ConfigEntry.createForConstructor(ConcreteTestInterfaceImpl.class, false,
                        TstHelper.convertToParameterEntry("test", "testStringConstructor")));
        Assert.assertNotNull(config.lookupImpl(TestInterface.class));
    }

    @Test
    public void testParametersFactoryMethod() throws Exception {
        EConfigContainer config = new EConfigContainer();
        config.addConfigEntry(TestInterface.class.getName(), ConfigEntry.createForFactoryMethod(
                ConcreteTestInterfaceImpl.class, "createInstance", false,
                TstHelper.convertToParameterEntry("test", "testStringFactoryMethod")));
        Assert.assertNotNull(config.lookupImpl(TestInterface.class));
    }

    @Test
    public void testRemove() throws Exception {
        EConfigContainer config = new EConfigContainer();
        config.addConfigEntry(TestInterface.class.getName(), ConfigEntry.createForFactoryMethod(
                ConcreteTestInterfaceImpl.class, "createInstance", false,
                TstHelper.convertToParameterEntry("test", "testStringFactoryMethod")));
        Assert.assertNotNull(config.lookupImpl(TestInterface.class));
        config.removeConfigEntry(TestInterface.class.getName());
        Assert.assertNull(config.lookupImpl(TestInterface.class));
    }

    @Test
    public void testParametersViaSetters() throws Exception {
        String testString = "testStringConstructor";
        EConfigContainer config = new EConfigContainer();
        config.addConfigEntry(
                TestInterface.class.getName(),
                ConfigEntry.createForConstructor(ConcreteTestInterfaceImpl2.class, false,
                        TstHelper.convertToParameterEntry("test", testString)));
        ConcreteTestInterfaceImpl2 impl = (ConcreteTestInterfaceImpl2) config.lookupImpl(TestInterface.class);
        Assert.assertNotNull(impl);
        Assert.assertEquals(testString, impl.getTest());

    }

    public static interface TestInterface {

    }

    @InjectionMethod(injectionStrategy=InjectionStrategy.initialization)
    public static class ConcreteTestInterfaceImpl {
        @Inject(parameterOrder = { "test" })
        public ConcreteTestInterfaceImpl(String test) {
            System.out.println("- in constructor");
            System.out.println(test);
        }

        @Inject(parameterOrder = { "test" })
        public static ConcreteTestInterfaceImpl createInstance(String test) {
            System.out.println("- in factory method");
            return new ConcreteTestInterfaceImpl(test);
        }
    }

    public static class ConcreteTestInterfaceImpl2 {
        private String test;

        @Inject(name = "test")
        public void setTest(String test) {
            this.test = test;
        }

        public String getTest() {
            return test;
        }

    }
}
