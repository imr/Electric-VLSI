/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SimpleConstructorType.java
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

import com.sun.electric.util.config.annotations.Inject;
import com.sun.electric.util.config.annotations.InjectionMethod;
import com.sun.electric.util.config.annotations.InjectionMethod.InjectionStrategy;

/**
 * @author Felix Schmidt
 */
@InjectionMethod(injectionStrategy=InjectionStrategy.initialization)
public class SimpleConstructorType {
    
    private String test;
    private SimpleDataType simpleData;
    
    @Inject(parameterOrder = {"test", "simpleData"})
    public SimpleConstructorType(String test, SimpleDataType simpleData) {
        this.test = test;
        this.simpleData = simpleData;
    }
    
    public String getTest() {
        return test;
    }

    public SimpleDataType getSimpleData() {
        return simpleData;
    }

    public void print() {
        System.out.print("SimpleConstructorType: ");
        simpleData.print();
    }

}
