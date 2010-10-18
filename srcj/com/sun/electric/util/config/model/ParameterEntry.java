/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ParameterEntry.java
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
package com.sun.electric.util.config.model;

import java.util.List;

import com.sun.electric.util.CollectionFactory;

/**
 * @author Felix Schmidt
 */
public class ParameterEntry {

    private String name;
    private ConfigEntry<?> value;

    /**
     * @param name
     * @param value
     */
    public ParameterEntry(String name, ConfigEntry<?> value) {
        super();
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConfigEntry<?> getValue() {
        return value;
    }

    public void setValue(ConfigEntry<?> value) {
        this.value = value;
    }
    
    public static List<Object> convertToObjectList(List<ParameterEntry> entries) throws Exception {
        List<Object> result = CollectionFactory.createArrayList();
        
        for(ParameterEntry entry: entries) {
            result.add(entry.getValue().getInstance());
        }
        
        return result;
    }

}
