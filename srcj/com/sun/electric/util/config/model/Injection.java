/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Injetion.java
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
import java.util.Map;

import com.sun.electric.util.CollectionFactory;

/**
 * @author Felix Schmidt
 * 
 */
public class Injection {

    public enum Attributes {
        name, implementation, factoryMethod, singleton
    }

    private String name;
    private String implementation;
    private String factoryMethod;
    private List<Parameter> parameters;
    private boolean singleton = false;

    /**
     * @param name
     * @param implementation
     * @param factoryMethod
     */
    public Injection(String name, String implementation, String factoryMethod, boolean singleton) {
        this.name = name;
        this.implementation = implementation;
        this.factoryMethod = factoryMethod;
        this.singleton = singleton;
        this.setParameters(null);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    public String getFactoryMethod() {
        return factoryMethod;
    }

    public void setFactoryMethod(String factoryMethod) {
        this.factoryMethod = factoryMethod;
    }

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    public boolean isSingleton() {
        return singleton;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public ConfigEntry<?> createConfigEntry(Map<String, Injection> allInjections)
            throws ClassNotFoundException {
        ConfigEntry<?> entry = null;

        List<ParameterEntry> paramEntries = null;
        ParameterEntry[] entries = null;

        if (this.parameters != null) {
            paramEntries = CollectionFactory.createArrayList();
            for (Parameter param : parameters) {
                paramEntries.add(param.createParameter(allInjections));
            }
            entries = paramEntries.toArray(new ParameterEntry[paramEntries.size()]);
        }

        if (factoryMethod == null) {
            entry = ConfigEntry.createForConstructor(Class.forName(this.implementation), singleton, entries);
        } else {
            entry = ConfigEntry.createForFactoryMethod(Class.forName(this.implementation),
                    this.factoryMethod, singleton, entries);
        }

        ConfigEntries.getEntries().put(name, entry);

        return entry;
    }

}
