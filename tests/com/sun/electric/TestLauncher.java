/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TestLauncher.java
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
package com.sun.electric;

import java.util.List;

import org.junit.Test;

import com.sun.electric.util.CollectionFactory;

/**
 * @author fschmidt
 *
 */
public class TestLauncher {
    
    private List<String> args;
    
    public TestLauncher() {
        args = CollectionFactory.createArrayList();
        args.add("-anything=test");
        args.add("-anything2=test");
        args.add("-springconfig=testconfig");
        args.add("-additionalfolder=testfolder");
    }
    
    @Test
    public void testGetSpringConfig() throws Exception {
        Class<?> clazz = Class.forName("com.sun.electric.Launcher");
        Launcher launcher = (Launcher) clazz.newInstance();
    } 
}
