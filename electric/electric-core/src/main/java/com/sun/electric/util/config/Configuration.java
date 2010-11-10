/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Configuration.java
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
package com.sun.electric.util.config;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Felix Schmidt
 * 
 */
public abstract class Configuration {

    private static Configuration instance = null;
    protected static String configName = "";
    protected static Logger logger = Logger.getLogger(Configuration.class.getName());

    protected Configuration() {
    }

    public static Configuration getInstance() {
        if (instance == null) {
            instance = extractConfigurationImplementation();
        }
        return instance;
    }

    public static void setConfigName(String configName) {
        Configuration.configName = configName;
    }

    public static Object lookup(String name) {
        return getInstance().lookupImpl(name);
    }

    public static <T> T lookup(Class<T> clazz) {
        return getInstance().lookupImpl(clazz);
    }

    private static Configuration extractConfigurationImplementation() {
        Configuration result = null;

        result = new EConfigContainer(Configuration.configName);

        return result;
    }

    protected abstract Object lookupImpl(String name);

    protected abstract <T> T lookupImpl(Class<T> clazz);

}
