/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: InjectStrategy.java
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

import java.util.List;

import com.sun.electric.util.CollectionFactory;
import com.sun.electric.util.config.annotations.Inject;
import com.sun.electric.util.config.cache.TypeCache;
import com.sun.electric.util.config.model.ParameterEntry;

/**
 * @author fschmidt
 * 
 */
public abstract class InjectStrategy<T> {

    public abstract T inject(Class<T> clazz, ParameterEntry... entries) throws Exception;

    public TypeCache typeCache = TypeCache.getInstance();

    protected Class<?>[] convertParametersToTypes(Class<?>[] signature, Object... parameters) throws ParameterDoesntFit {
        if (parameters == null)
            return null;

        if (parameters.length != signature.length) {
            return null;
        }

        List<Class<?>> parameterTypes = CollectionFactory.createLinkedList();

        int i = 0;
        for (Object obj : parameters) {
            Class<?> tmpClass = obj.getClass();

            if (!tmpClass.equals(signature[i])) {
                typeCache.createTypeHierarchy(tmpClass);
                if (typeCache.contains(tmpClass, signature[i])) {
                    tmpClass = signature[i];
                } else {
                    throw new ParameterDoesntFit();
                }
            }

            parameterTypes.add(tmpClass);
            i++;
        }
        return parameterTypes.toArray(new Class<?>[parameterTypes.size()]);
    }

    protected void convertParameterEntires(Inject inject, Object[] result, ParameterEntry... parameters)
            throws Exception {
        String[] order = inject.parameterOrder();

        if (order.length == 0) {
            for (int i = 0; i < parameters.length; i++) {
                result[i] = parameters[i].getValue().getInstance();
            }
        } else {
            if (order.length != result.length)
                throw new ParameterDoesntFit();
            
            for (int i = 0; i < order.length; i++) {
                result[i] = null;
                for (int j = 0; j < parameters.length; j++) {
                    if (parameters[j].getName().equals(order[i])) {
                        result[i] = parameters[j].getValue().getInstance();
                    }
                }
                if (result[i] == null) {
                    throw new Exception("Could not find parameter: " + order[i]);
                }
            }
        }
    }

    public static <T> InjectStrategy<T> getForConstructor(Class<T> clazz) {
        return new ConstructorInject<T>();
    }

    public static <T> InjectStrategy<T> getForFactoryMethod(Class<T> clazz, String factoryMethod) {
        return new FactoryMethodInject<T>(factoryMethod);
    }

    public static <T> InjectStrategy<T> getForSetter(Class<T> clazz, CreateBy createBy, String factoryMethod) {
        return new SetterInject<T>(createBy, factoryMethod);
    }

    public static <T> InjectStrategy<T> getDefault(Class<T> clazz, CreateBy createBy, String factoryMethod) {
        return InjectStrategy.getForSetter(clazz, createBy, factoryMethod);
    }

    public static class ParameterDoesntFit extends Exception {

    }

}
