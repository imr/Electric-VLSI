/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EConfig.java
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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;

import com.sun.electric.util.CollectionFactory;
import com.sun.electric.util.config.InitStrategy.StaticInit;
import com.sun.electric.util.config.model.ConfigEntry;

/**
 * @author Felix Schmidt
 * 
 */
public class EConfigContainer extends Configuration {

    EConfigContainer() {
        this(new StaticInit());
    }

    EConfigContainer(String configFile) {
        File file = new File(configFile);
        if (file.exists()) {
            new XmlInitSax(configFile).init(this);
        } else {
            URL fileName = EConfigContainer.class.getResource("/" + configFile);
            try {
                System.out.println(fileName.toURI());
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            new XmlInitSax(fileName).init(this);
        }
    }

    EConfigContainer(InitStrategy initMethod) {
        initMethod.init(this);
    }

    private Map<String, ConfigEntry<?>> lookupMapClass = CollectionFactory.createHashMap();

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.electric.util.config.Configuration#lookup(java.lang.String)
     */
    @Override
    protected Object lookupImpl(String name) {
        Object result = null;
        ConfigEntry<?> entry = (ConfigEntry<?>) lookupMapClass.get(name);
        if (entry != null) {
            try {
                result = entry.getInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.electric.util.config.Configuration#lookup(java.lang.Class)
     */
    @Override
    protected <T> T lookupImpl(Class<T> clazz) {
        try {
            return (T) lookupImpl(clazz.getName());
        } catch (Exception ex) {
            return null;
        }
    }

    void addConfigEntry(String name, ConfigEntry<?> entry) {
        lookupMapClass.put(name, entry);
        logger.log(Level.FINE, "Add " + name + " to lookup map");
    }

    void removeConfigEntry(String name) {
        lookupMapClass.remove(name);
        logger.log(Level.FINE, "Remove " + name + " from lookup map");
    }

}
