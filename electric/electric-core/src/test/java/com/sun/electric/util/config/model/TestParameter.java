/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TestParameter.java
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
package com.sun.electric.util.config.model;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.sun.electric.util.CollectionFactory;

/**
 * @author Felix Schmidt
 */
public class TestParameter {
    
    @Test
    public void testConvert() throws Exception {
        Injection injection = new Injection("testObject", "java.lang.Integer", null, false);
        Map<String, Injection> allInjections = CollectionFactory.createHashMap();
        allInjections.put("testObject", injection);
        
        Parameter parameter = new Parameter("testParam", "testObject", null, null);
        
        ParameterEntry entry = parameter.createParameter(allInjections);
        
        Assert.assertNotNull(entry);
        
    }

}
