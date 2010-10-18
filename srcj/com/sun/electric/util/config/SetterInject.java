/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SetterInject.java
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.sun.electric.util.config.annotations.Inject;
import com.sun.electric.util.config.model.ParameterEntry;

/**
 * @author Felix Schmidt
 * 
 */
public class SetterInject<T> extends InjectStrategy<T> {
   

    private CreateBy createBy;
    private String factoryName;
    
    public SetterInject(CreateBy createBy, String factoryName) {
        this.createBy = createBy;
        this.factoryName = factoryName;
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
        if(createBy.equals(CreateBy.constructor)) {
            instance = clazz.newInstance();
        } else if(createBy.equals(CreateBy.factoryMethod)){
            Method method = clazz.getMethod(factoryName);
            instance = (T) method.invoke(null);
        }
        
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

                for (ParameterEntry entry : entries) {
                    if (entry.getName().toLowerCase().equals(paramName)) {
                        method.invoke(instance, entry.getValue().getInstance());
                        break;
                    }
                }
            }
        }
        return instance;
    }

}
