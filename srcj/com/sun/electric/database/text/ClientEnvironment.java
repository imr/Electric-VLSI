/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Environment.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.text;

import java.util.HashMap;

/**
 * Class to mirror on a server a portion of client environment
 */
public class ClientEnvironment {
    private static final ThreadLocal<ClientEnvironment> threadEnvironment = new ThreadLocal<ClientEnvironment>();
    
    private HashMap<String,Object> serverPrefValues = new HashMap<String,Object>();

    public Object getValue(Pref pref) {
        Object factoryValue = pref.getFactoryValue();
        Object value = serverPrefValues.get(pref.getPrefPath());
        if (value == null || value.equals(factoryValue) || value.getClass() != factoryValue.getClass())
            value = factoryValue;
        return value;
    }

    public static ClientEnvironment getThreadEnvironment() {
        return threadEnvironment.get();
    }

    public static ClientEnvironment setThreadEnvironment(ClientEnvironment environment) {
        ClientEnvironment oldEnvironment = threadEnvironment.get();
        threadEnvironment.set(environment);
        return oldEnvironment;
    }

}