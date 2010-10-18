/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TestXmlInit.java
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

import junit.framework.Assert;

import org.junit.Test;

/**
 * @author Felix Schmidt
 * 
 */
public class TestXmlInit {

    @Test
    public void testXmlInit() {
        EConfigContainer econfig = new EConfigContainer(new XmlInitSax(
                ClassLoader.getSystemResource("com/sun/electric/util/config/testConfig.xml")));

        Assert.assertNotNull(econfig);
    }

    @Test
    public void testXmlInitConstructor() {
        EConfigContainer econfig = new EConfigContainer(new XmlInitSax(
                ClassLoader.getSystemResource("com/sun/electric/util/config/testConfigConstructor.xml")));
        SimpleInterface si = econfig.lookupImpl(SimpleInterface.class);

        Assert.assertNotNull(si);
    }

    @Test
    public void testXmlInitFactoryMethod() {
        EConfigContainer econfig = new EConfigContainer(new XmlInitSax(
                ClassLoader.getSystemResource("com/sun/electric/util/config/testConfigFactoryMethod.xml")));
        SimpleInterface si = econfig.lookupImpl(SimpleInterface.class);

        Assert.assertNotNull(si);
    }

    @Test
    public void testXmlInitParameterViaSetters() {
        EConfigContainer econfig = new EConfigContainer(new XmlInitSax(
                ClassLoader.getSystemResource("com/sun/electric/util/config/testConfigParameters.xml")));
        SimpleParameterType pt = (SimpleParameterType) econfig.lookupImpl("simpleParameterType");

        Assert.assertNotNull(pt);

        System.out.println(pt.getTest());
        pt.print();
    }

    @Test
    public void testXmlInitParameterViaConstructor() {
        EConfigContainer econfig = new EConfigContainer(new XmlInitSax(
                ClassLoader.getSystemResource("com/sun/electric/util/config/testConfigParameters.xml")));
        SimpleConstructorType pt = (SimpleConstructorType) econfig.lookupImpl("simpleConstructorType");

        Assert.assertNotNull(pt);

        System.out.println(pt.getTest());
        pt.print();
    }

    @Test
    public void testXmlInitParameterViaFactory() {
        EConfigContainer econfig = new EConfigContainer(new XmlInitSax(
                ClassLoader.getSystemResource("com/sun/electric/util/config/testConfigParameters.xml")));
        SimpleFactoryType pt = (SimpleFactoryType) econfig.lookupImpl("simpleFactoryType");

        Assert.assertNotNull(pt);

        System.out.println(pt.getTest());
        pt.print();
    }

    @Test
    public void testXmlInitSingleton() {
        EConfigContainer econfig = new EConfigContainer(new XmlInitSax(
                ClassLoader.getSystemResource("com/sun/electric/util/config/testConfig.xml")));
        Singleton singleton1 = (Singleton) econfig.lookupImpl("singleton");
        Singleton singleton2 = (Singleton) econfig.lookupImpl("singleton");

        Assert.assertNotNull(singleton1);
        Assert.assertNotNull(singleton2);

        Assert.assertEquals(singleton1, singleton2);

        System.out.println("s1: " + singleton1);
        System.out.println("s2: " + singleton2);
    }
    
    @Test
    public void testXmlInitInclude() {
        EConfigContainer econfig = new EConfigContainer(new XmlInitSax(
                ClassLoader.getSystemResource("com/sun/electric/util/config/testConfigIncluder.xml")));
        SimpleInterface si = econfig.lookupImpl(SimpleInterface.class);

        Assert.assertNotNull(si);
    }
    
    @Test
    public void testXmlInitIncludeSelfReference() {
        EConfigContainer econfig = new EConfigContainer(new XmlInitSax(
                ClassLoader.getSystemResource("com/sun/electric/util/config/testConfigIncluderSelfReference.xml")));
    }
    
    @Test
    public void testXmlInitConstructorMultiple() {
        EConfigContainer econfig = new EConfigContainer(new XmlInitSax(
                ClassLoader.getSystemResource("com/sun/electric/util/config/testConfigConstructor.xml")));
        SimpleConstructorType2 si1 = (SimpleConstructorType2) econfig.lookupImpl("multipleConstructorOneParameter");
        SimpleConstructorType2 si2 = (SimpleConstructorType2) econfig.lookupImpl("multipleConstructorTwoParameters");
        SimpleConstructorType2 si3 = (SimpleConstructorType2) econfig.lookupImpl("multipleConstructorOneParameterInteger");
        SimpleConstructorType2 si4 = (SimpleConstructorType2) econfig.lookupImpl("multipleConstructorOneParameterDouble");

        Assert.assertNotNull(si1);
        Assert.assertNotNull(si2);
        Assert.assertNotNull(si3);
        Assert.assertNotNull(si4);
    }
    
    @Test
    public void testXmlInitParameterViaFactoryInheritance() {
        EConfigContainer econfig = new EConfigContainer(new XmlInitSax(
                ClassLoader.getSystemResource("com/sun/electric/util/config/testConfigParameters.xml")));
        SimpleFactoryType pt = (SimpleFactoryType) econfig.lookupImpl("simpleFactoryTypeNumber");

        Assert.assertNotNull(pt);

        System.out.println(pt.getTest());
        pt.print();
    }

}
