/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TestMemory.java
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
            Locale.setDefault(Locale.US);
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
