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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.sun.electric.tool.util.concurrent.runtime.taskParallel.IThreadPool;
import com.sun.electric.tool.util.concurrent.runtime.taskParallel.ThreadPool;
import com.sun.electric.util.CollectionFactory;
import com.sun.electric.util.config.InitStrategy.StaticInit;

/**
 * @author fschmidt
 * 
 */
public class EConfig extends Configuration {

    EConfig() {
        this(new StaticInit());
    }
    
    EConfig(InitStrategy initMethod) {
        initMethod.init(this);
    }

    private Map<Class<?>, ConfigEntry<?>> lookupMapClass = CollectionFactory.createHashMap();

    public static abstract class ConfigEntry<T> {
        public abstract T getNewInstance() throws Exception;

        protected Class<?>[] convertParametersToTypes(Object... parameters) {
            if (parameters == null)
                return null;

            List<Class<?>> parameterTypes = CollectionFactory.createLinkedList();
            for (Object obj : parameters) {
                parameterTypes.add(obj.getClass());
            }
            return parameterTypes.toArray(new Class<?>[parameterTypes.size()]);
        }

        public static class ConfigEntryConstructor<T> extends ConfigEntry<T> {

            private Class<T> clazz;
            private Object[] parameters = null;

            public ConfigEntryConstructor(Class<T> clazz, Object... parameters) {
                this.clazz = clazz;
                this.parameters = parameters;
            }

            @Override
            public T getNewInstance() throws Exception {

                if (parameters == null) {
                    return clazz.newInstance();
                } else {
                    Constructor<T> constructor = clazz.getConstructor(convertParametersToTypes(parameters));
                    return constructor.newInstance(parameters);
                }
            }

        }

        public static class ConfigEntryFactoryMethod<T> extends ConfigEntry<T> {

            private Class<T> clazz;
            private String factoryMethod;
            private Object[] parameters;

            public ConfigEntryFactoryMethod(Class<T> clazz, String factoryMethod, Object... parameters) {
                this.clazz = clazz;
                this.factoryMethod = factoryMethod;
                this.parameters = parameters;
            }

            @Override
            public T getNewInstance() throws Exception {
                Method factory = clazz.getMethod(factoryMethod, convertParametersToTypes(parameters));
                return (T) factory.invoke(null, parameters);
            }
        }

        public static <T> ConfigEntry<T> createForConstructor(Class<T> clazz, Object... parameters) {
            return new ConfigEntryConstructor<T>(clazz, parameters);
        }

        public static <T> ConfigEntry<T> createForFactoryMethod(Class<T> clazz, String factoryMethod,
                Object... parameters) {
            return new ConfigEntryFactoryMethod<T>(clazz, factoryMethod, parameters);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.electric.util.config.Configuration#lookup(java.lang.String)
     */
    @Override
    protected Object lookupImpl(String name) {
        try {
            return lookup(Class.forName(name));
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.electric.util.config.Configuration#lookup(java.lang.Class)
     */
    @Override
    protected <T> T lookupImpl(Class<T> clazz) {
        T result = null;
        ConfigEntry<T> entry = (ConfigEntry<T>) lookupMapClass.get(clazz);
        if (entry != null) {
            try {
                result = entry.getNewInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    void addConfigEntry(Class<?> clazz, ConfigEntry<?> entry) {
        lookupMapClass.put(clazz, entry);
        logger.log(Level.INFO, "Add " + clazz.getName() + " to lookup map");
    }

    void removeConfigEntry(Class<?> clazz) {
        lookupMapClass.remove(clazz);
        logger.log(Level.INFO, "Remove " + clazz.getName() + " from lookup map");
    }

}
