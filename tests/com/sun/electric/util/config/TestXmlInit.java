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

import org.junit.Test;

/**
 * @author fschmidt
 *
 */
public class TestXmlInit {
    
    @Test
    public void testXmlInit() {
        EConfig econfig = new EConfig(new XmlInitSax(ClassLoader.getSystemResource("com/sun/electric/util/config/testConfig.xml")));
    }
    
    @Test
    public void testXmlInitConstructor() {
        EConfig econfig = new EConfig(new XmlInitSax(ClassLoader.getSystemResource("com/sun/electric/util/config/testConfigConstructor.xml")));
        SimpleInterface si = econfig.lookupImpl(SimpleInterface.class);
    }
    
    @Test
    public void testXmlInitFactoryMethod() {
        EConfig econfig = new EConfig(new XmlInitSax(ClassLoader.getSystemResource("com/sun/electric/util/config/testConfigFactoryMethod.xml")));
        SimpleInterface si = econfig.lookupImpl(SimpleInterface.class);
    }

}
