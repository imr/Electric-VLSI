/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SimpleBus.java
 *
 * Copyright (c) 2007 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation.test;

import java.util.List;
import java.util.ArrayList;

/**
 * Create an object for a bus of signals.  The signal name must be the
 * flattened signal name, except for the bus name itself, which can be
 * of bus format: [#,#:#,alpha].
 * <P>
 * Example: top.main[1].bank[0].bus[a,1:4,6,b].
 * <P>
 * Incorrect: top.main[0:1].bank[0].bus[1:2].
 */
public class SimpleBus implements BussedIO {

    private List indexNames;
    private String prefix;

    /**
     * Create a new Bus.  The hierarchical portion of the name must be flat.
     * The bus name must of the format bus[#,#:#,alpha]
     * @param busName the full name of the bus
     */
    public SimpleBus(String busName) {
        indexNames = new ArrayList();
        prefix = "busNameError";

        int openb = busName.lastIndexOf('[');
        if (openb == -1) {
            System.out.println("SimpleBus Error: Not a bussed signal: "+busName);
            return;
        }
        int closeb = busName.lastIndexOf(']');
        if (closeb == -1 || closeb < openb) {
            System.out.println("SimpleBus Error: Bad []'s in: "+busName);
            return;
        }
        prefix = busName.substring(0, openb);
        String indices = busName.substring(openb+1, closeb);
        String [] parts = indices.split(",");
        for (int i=0; i<parts.length; i++) {
            parseArray(parts[i]);
        }
    }

    void parseArray(String arr) {
        if (arr.indexOf(':') > 0) {
            String [] indices = arr.split(":");
            if (indices.length != 2) {
                System.out.println("SimpleBus Error: Invalid bus spec: "+arr);
                return;
            }
            int start, end;
            try {
                start = Integer.parseInt(indices[0]);
                end = Integer.parseInt(indices[1]);
            } catch (NumberFormatException e) {
                System.out.println("SimpleBus Error: Indices in range must be numeric: "+arr);
                return;
            }
            if (start > end) {
                for (int i=start; i>=end; i--) {
                    indexNames.add(String.valueOf(i));
                }
            } else {
                for (int i=start; i<=end; i++) {
                    indexNames.add(String.valueOf(i));
                }
            }
        } else {
            indexNames.add(arr);
        }
    }

    public int getWidth() {
        return indexNames.size();
    }

    public String getName() {
        return prefix;
    }

    public String getSignal(int index) {
        if (index < 0 || index >= indexNames.size()) {
            System.out.println("SimpleBus getSignal error: index out of range: "+index);
            return "";
        }
        return prefix + '[' + indexNames.get(index) + ']';
    }

    public String getSignal(String bitname) {
        for (int i=0; i<indexNames.size(); i++) {
            if (indexNames.get(i).equals(bitname))
                return getSignal(i);
        }
        System.out.println("SimpleBus getSignal error: bitname '"+bitname+"' not found in bus "+prefix);
        return "";
    }

    // ==========================================================

    public static void main(String [] args) {
        SimpleBus bus;
        bus = new SimpleBus("top.foo.bus[1]");
        testBus(bus);
        bus = new SimpleBus("top.foo.bus[1:2]");
        testBus(bus);
        bus = new SimpleBus("top.foo.bus[1,3:4]");
        testBus(bus);
        bus = new SimpleBus("top.foo.bus[a,b,1:4]");
        testBus(bus);
        bus = new SimpleBus("top.foo.bus[5:3,b]");
        testBus(bus);

        /// bad
        bus = new SimpleBus("top.foo.bus");
        testBus(bus);
    }

    private static void testBus(SimpleBus bus) {
        System.out.println("------------------------------------------------------");
        System.out.println("Bus "+bus.getName()+" indices: (width="+bus.getWidth()+")");
        for (int i=0; i<bus.getWidth(); i++) {
            System.out.println("  "+bus.getSignal(i));
        }
    }
}
