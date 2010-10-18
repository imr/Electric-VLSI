/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FactoryMethodInject.java
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

import java.lang.reflect.Method;

import com.sun.electric.util.config.annotations.Inject;
import com.sun.electric.util.config.model.ParameterEntry;

/**
 * @author Felix Schmidt
 */
public class FactoryMethodInject<T> extends InjectStrategy<T> {

    private String factoryMethod;

    public FactoryMethodInject(String factoryMethod) {
        this.factoryMethod = factoryMethod;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.electric.util.config.InjectStrategy#inject(java.lang.Object,
     * java.lang.Class, com.sun.electric.util.config.model.ParameterEntry[])
     */
    @Override
    public T inject(Class<T> clazz, ParameterEntry... entries) throws Exception {
        T instance = null;
        if (entries == null || entries.length == 0) {
            Method factory = clazz.getMethod(factoryMethod);
            instance = (T) factory.invoke(null);
        } else {
            Object[] paramObjs = new Object[entries.length];
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (method.getName().equals(factoryMethod)) {
                    Inject inject = method.getAnnotation(Inject.class);
                    if (inject != null) {
                        convertParameterEntires(inject, paramObjs, entries);
                        try {
                            Method tmpMethod = clazz.getMethod(factoryMethod,
                                    convertParametersToTypes(method.getParameterTypes(), paramObjs));
                            instance = (T) tmpMethod.invoke(null, paramObjs);
                            break;
                        } catch (IllegalAccessException ex) {
                            instance = null;
                        } catch (IllegalArgumentException ex) {
                            instance = null;
                        } catch(ParameterDoesntFit ex) {
                            instance = null;
                        }
                    }
                }
            }
        }
        return instance;
    }

}
