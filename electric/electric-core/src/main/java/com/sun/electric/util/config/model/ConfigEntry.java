/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ConfigEntry.java
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
package com.sun.electric.util.config.model;

import java.lang.reflect.Method;
import java.util.Arrays;

import com.sun.electric.util.config.InjectStrategy;
import com.sun.electric.util.config.CreateBy;
import com.sun.electric.util.config.SimpleEnumConstructor.TestEnum;
import com.sun.electric.util.config.annotations.Inject;
import com.sun.electric.util.config.annotations.InjectionMethod;
import com.sun.electric.util.config.annotations.InjectionMethod.InjectionStrategy;

/**
 * @author Felix Schmidt
 */
public abstract class ConfigEntry<T> {
    protected boolean singleton = false;
    protected T singletonInstance = null;
    protected ParameterEntry[] parameters;
    protected Class<T> clazz;

    public ConfigEntry(Class<T> clazz, boolean singleton, ParameterEntry... parameter) {
        this.singleton = singleton;
        this.parameters = parameter;
        this.clazz = clazz;
    }

    public abstract T getInstance() throws Exception;

    protected void injectSetters(T instance) throws Exception {
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            Inject inject = method.getAnnotation(Inject.class);
            if (inject != null) {
                String name = inject.name();
                String paramName = "";
                if (name.equals("")) {
                    if (method.getName().startsWith("set")) {
                        paramName = method.getName().substring(3).toLowerCase();
                    } else {
                        paramName = name;
                    }
                } else {
                    paramName = name;
                }

                for (ParameterEntry entry : parameters) {
                    if (entry.getName().equals(paramName)) {
                        method.invoke(instance, entry.getValue().getInstance());
                        break;
                    }
                }
            }
        }
    }

    public static class ConfigEntryConstructor<T> extends ConfigEntry<T> {

        public ConfigEntryConstructor(Class<T> clazz, boolean singleton, ParameterEntry... parameters) {
            super(clazz, singleton, parameters);
        }

        @Override
        public T getInstance() throws Exception {
            T instance = null;

            if (!(singleton && singletonInstance != null)) {

                InjectionMethod inWay = clazz.getAnnotation(InjectionMethod.class);
                InjectStrategy<T> strategy = null;
                if (inWay != null) {
                    if (inWay.injectionStrategy().equals(InjectionStrategy.initialization))
                        strategy = InjectStrategy.getForConstructor(clazz);
                    else {
                        strategy = InjectStrategy.getForSetter(clazz, CreateBy.constructor, null);
                    }
                } else {
                    strategy = InjectStrategy.getDefault(clazz, CreateBy.constructor, null);
                }

                instance = strategy.inject(clazz, parameters);

                if (singleton) {
                    singletonInstance = instance;
                }
            } else {
                instance = singletonInstance;
            }

            return instance;
        }

    }

    public static class ConfigEntryFactoryMethod<T> extends ConfigEntry<T> {

        private String factoryMethod;

        public ConfigEntryFactoryMethod(Class<T> clazz, String factoryMethod, boolean singleton,
                ParameterEntry... parameters) {
            super(clazz, singleton, parameters);
            this.factoryMethod = factoryMethod;

        }

        @Override
        public T getInstance() throws Exception {
            T instance = null;
            if (!(singleton && singletonInstance != null)) {

                InjectionMethod inWay = clazz.getAnnotation(InjectionMethod.class);
                InjectStrategy<T> strategy = null;
                if (inWay != null) {
                    if (inWay.injectionStrategy().equals(InjectionStrategy.initialization))
                        strategy = InjectStrategy.getForFactoryMethod(clazz, factoryMethod);
                    else
                        strategy = InjectStrategy.getForSetter(clazz, CreateBy.factoryMethod, factoryMethod);
                } else {
                    strategy = InjectStrategy.getDefault(clazz, CreateBy.factoryMethod, factoryMethod);
                }

                instance = strategy.inject(clazz, parameters);

                if (singleton) {
                    singletonInstance = instance;
                }
            } else {
                instance = singletonInstance;
            }

            return instance;
        }
    }

    public static class ConfigEntryPrimitive<T> extends ConfigEntry<T> {
        private T value;

        public ConfigEntryPrimitive(T value) {
            super(null, false);
            this.value = value;
        }

        @Override
        public T getInstance() throws Exception {
            return value;
        }
    }
    
    public static class ConfigEntryEnum<T extends Enum<T>> extends ConfigEntry<T> {
    	
    	private T value;
    	
    	public ConfigEntryEnum(Class<T> clazz, String value) {
    		super(null, false);

    		for(T tmp: clazz.getEnumConstants()) {
    			if(tmp.name().equals(value)) {
    				this.value = tmp;
    				return;
    			}
    		}
    	}

		@Override
		public T getInstance() throws Exception {
			return value;
		}
    	
    }

    public static <T> ConfigEntry<T> createForConstructor(Class<T> clazz, boolean singleton,
            ParameterEntry... parameters) {
        return new ConfigEntryConstructor<T>(clazz, singleton, parameters);
    }

    public static <T> ConfigEntry<T> createForFactoryMethod(Class<T> clazz, String factoryMethod,
            boolean singleton, ParameterEntry... parameters) {
        return new ConfigEntryFactoryMethod<T>(clazz, factoryMethod, singleton, parameters);
    }
    
    public static <T extends Enum<T>> ConfigEntry<T> createForEnum(Class<T> clazz, String value) {
        return new ConfigEntryEnum<T>(clazz, value);
    }

}
