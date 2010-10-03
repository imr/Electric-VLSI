/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.sun.electric.util.memory;

public class MemoryUsage {
    
    private static MemoryUsage instance = new MemoryUsage();
    
    private MemoryUsage() {
        
    }
    
    public static MemoryUsage getInstance() {
        return instance;
    }
    
    public long getHeapSize() {
        return Runtime.getRuntime().totalMemory();
    }
    
    public long getFreeSpace() {
        return getHeapSize() - Runtime.getRuntime().freeMemory();
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        
        builder.append(Memory.formatMemorySize(getFreeSpace()));
        builder.append("/");
        builder.append(Memory.formatMemorySize(getHeapSize()));
        
        return builder.toString();
    }

}
