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

import org.junit.Test;

import com.sun.electric.util.memory.MemoryUsage;

public class TestMemoryUsage {
    
    @Test
    public void testHeapSize() {
        System.out.println(MemoryUsage.getInstance().getHeapSize());
    }

}
