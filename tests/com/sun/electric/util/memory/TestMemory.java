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


import junit.framework.Assert;

import org.junit.Test;

import com.sun.electric.util.memory.Memory.MemSize;
import java.util.Locale;

public class TestMemory {

    @Test
    public void testEnsmallAndEnlargeMemSize() {
        MemSize[][] mems = { { MemSize.none, MemSize.kilo }, { MemSize.kilo, MemSize.mega },
                { MemSize.mega, MemSize.giga } };

        for (MemSize[] tmp : mems) {
            System.out.print("Enlarge " + tmp[0] + " to " + tmp[1] + "...");
            Assert.assertEquals(tmp[1], tmp[0].enlarge());
            System.out.println(" done");
        }

        System.out.println("----");

        for (MemSize[] tmp : mems) {
            System.out.print("Ensmall " + tmp[1] + " to " + tmp[0] + "...");
            Assert.assertEquals(tmp[0], tmp[1].ensmall());
            System.out.println(" done");
        }

        System.out.println("----");
    }

    @Test
    public void testGetMemSize() {
        Object[][] mems = { { MemSize.none, (long) (1) }, { MemSize.kilo, (long) (1024) },
                { MemSize.mega, (long) (1024 * 1024) }, { MemSize.giga, (long) (1024 * 1024 * 1024) },
                { MemSize.mega, (long) (1024 * 1024 * 73) } };

        for (Object[] tmp : mems) {
            System.out.print("Find best size: " + tmp[1] + "B... ");
            MemSize best = MemSize.getBestMemSize((Long) tmp[1]);
            Assert.assertEquals(tmp[0], best);
            System.out.println(best.toString());
        }

        System.out.println("----");
    }

    @Test
    public void testGetBestMemSizeValue() {
        Object[][] mems = { { (long) (1024), 1.0 }, { (long) (1024 * 1024), 1.0 },
                { (long) (1024 * 3), 3.0 }, { (long) (255 * 255 * 255), 15.8132 } };

        for (Object[] tmp : mems) {
            MemSize best = MemSize.getBestMemSize((Long) tmp[0]);
            double value = MemSize.getBestMemSizeValue((Long) tmp[0], best);

            Assert.assertEquals((Double) tmp[1], value, 0.0001);

            System.out.println(value + " " + best + " => " + tmp[0] + "B");
        }

        System.out.println("----");
    }

    @Test
    public void testFormatMemorySize() {
        Object[][] mems = { { (long) 1024, "1kB" }, { (long) (255 * 255 * 255), "15.8MB" } };

        Locale savedLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ROOT);
            for (Object[] tmp : mems) {
                String formattedString = Memory.formatMemorySize((Long) tmp[0]);
                System.out.println(formattedString);
                Assert.assertEquals(tmp[1], formattedString);
            }
        } finally {
            Locale.setDefault(savedLocale);
        }
    }

}
