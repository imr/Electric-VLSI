/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TestXmlConfigVerification.java
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

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.sun.electric.util.CollectionFactory;
import com.sun.electric.util.config.XmlConfigVerification.LoopExistsException;
import com.sun.electric.util.config.model.Injection;
import com.sun.electric.util.config.model.Parameter;

/**
 * @author fschmidt
 *
 */
public class TestXmlConfigVerification {
    
    @Test
    public void testWithoutLoopAndChain() throws LoopExistsException {
        Injection injection1 = new Injection("t1", "t2", null, false);
        Injection injection2 = new Injection("t3", "t4", null, false);
        Map<String, Injection> injections = CollectionFactory.createHashMap();
        injections.put(injection1.getName(), injection1);
        injections.put(injection2.getName(), injection2);
        
        Assert.assertTrue(XmlConfigVerification.runVerification(injections));
    }
    
    @Test
    public void testWithoutLoopAndWithChain() throws LoopExistsException {
        Injection injection1 = new Injection("t1", "t2", null, false);
        Injection injection2 = new Injection("t3", "t4", null, false);
        Injection injection3 = new Injection("t5", "t6", null, false);
        
        Parameter parameter = new Parameter("param1", "t3", null, null);
        List<Parameter> parameters = CollectionFactory.createArrayList();
        parameters.add(parameter);
        
        injection3.setParameters(parameters);
        
        Map<String, Injection> injections = CollectionFactory.createHashMap();
        injections.put(injection1.getName(), injection1);
        injections.put(injection2.getName(), injection2);
        injections.put(injection3.getName(), injection3);
        
        Assert.assertTrue(XmlConfigVerification.runVerification(injections));
    }
    
    @Test(expected = LoopExistsException.class)
    public void testWithLoop() throws LoopExistsException {
        Injection injection1 = new Injection("t1", "t2", null, false);
        Injection injection2 = new Injection("t3", "t4", null, false);
        Injection injection3 = new Injection("t5", "t6", null, false);
        
        Parameter parameter1 = new Parameter("param1", "t3", null, null);
        List<Parameter> parameters1 = CollectionFactory.createArrayList();
        parameters1.add(parameter1);
        injection3.setParameters(parameters1);
        
        Parameter parameter2 = new Parameter("param2", "t5", null, null);
        List<Parameter> parameters2 = CollectionFactory.createArrayList();
        parameters1.add(parameter2);
        injection2.setParameters(parameters2);
        
        Map<String, Injection> injections = CollectionFactory.createHashMap();
        injections.put(injection1.getName(), injection1);
        injections.put(injection2.getName(), injection2);
        injections.put(injection3.getName(), injection3);
        
        Assert.assertTrue(XmlConfigVerification.runVerification(injections));
    }
    
    @Test
    public void testYChain() throws LoopExistsException {
        Injection injection1 = new Injection("t1", "t2", null, false);
        Injection injection2 = new Injection("t3", "t4", null, false);
        Injection injection3 = new Injection("t5", "t6", null, false);
        Injection injection4 = new Injection("t7", "t8", null, false);
        
        Parameter p1 = new Parameter("param1", "t3", null, null);
        List<Parameter> parameters = CollectionFactory.createArrayList();
        parameters.add(p1);
        injection3.setParameters(parameters);
        
        Parameter p2 = new Parameter("param1", "t3", null, null);
        parameters = CollectionFactory.createArrayList();
        parameters.add(p2);
        injection1.setParameters(parameters);
        
        Parameter p3 = new Parameter("param1", "t7", null, null);
        parameters = CollectionFactory.createArrayList();
        parameters.add(p3);
        injection2.setParameters(parameters);
        
        Map<String, Injection> injections = CollectionFactory.createHashMap();
        injections.put(injection1.getName(), injection1);
        injections.put(injection2.getName(), injection2);
        injections.put(injection3.getName(), injection3);
        injections.put(injection4.getName(), injection4);
        
        Assert.assertTrue(XmlConfigVerification.runVerification(injections));
    }
    
    @Test(expected = LoopExistsException.class)
    public void testSelfReference() throws LoopExistsException {
        Injection injection1 = new Injection("t1", "t2", null, false);
        
        Parameter parameter = new Parameter("param1", "t1", null, null);
        List<Parameter> parameters = CollectionFactory.createArrayList();
        parameters.add(parameter);
        
        injection1.setParameters(parameters);
      
        Map<String, Injection> injections = CollectionFactory.createHashMap();
        injections.put(injection1.getName(), injection1);
        
        Assert.assertTrue(XmlConfigVerification.runVerification(injections));
    }

}
