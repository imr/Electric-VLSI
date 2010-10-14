/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ConfigurationVerification.java
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

import java.util.Map;

import com.sun.electric.util.CollectionFactory;
import com.sun.electric.util.config.model.Injection;
import com.sun.electric.util.config.model.Parameter;

/**
 * @author fschmidt
 * 
 */
public class XmlConfigVerification {

    private static XmlConfigVerification instance = new XmlConfigVerification();

    private XmlConfigVerification() {

    }

    public static XmlConfigVerification getInstance() {
        return instance;
    }

    public static boolean runVerification(Map<String, Injection> injections) throws LoopExistsException {
        return XmlConfigVerification.getInstance().verifyConfiguration(injections);
    }

    public boolean verifyConfiguration(Map<String, Injection> injections) throws LoopExistsException {
        Map<String, InjectionWrapper> wrappers = CollectionFactory.createHashMap();
        for (Map.Entry<String, Injection> entry : injections.entrySet()) {
            wrappers.put(entry.getKey(), new InjectionWrapper(entry.getValue()));
        }

        for (InjectionWrapper wrapper : wrappers.values()) {
            deepFirstSearch(wrapper, wrappers);
        }

        return true;
    }

    private void deepFirstSearch(InjectionWrapper wrapper, Map<String, InjectionWrapper> injections)
            throws LoopExistsException {
        if (wrapper.finished)
            return;

        if (wrapper.visited)
            throw new LoopExistsException();

        wrapper.visited = true;

        if (wrapper.injection.getParameters() != null) {
            for (Parameter param : wrapper.injection.getParameters()) {
                if (param.getRef() != null) {
                    this.deepFirstSearch(injections.get(param.getRef()), injections);
                }
            }
        }

        wrapper.finished = true;
    }

    private class InjectionWrapper {
        private Injection injection;
        private boolean visited = false;
        private boolean finished = false;

        public InjectionWrapper(Injection parameter) {
            this.injection = parameter;
        }
    }

    public static class LoopExistsException extends Exception {

    }

}
