/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LENetwork.java
 * Written by: Jonathan Gainsley, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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
package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.variable.VarContext;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintStream;

public class LENetwork {

    /** Name */                             private String name;
    /** List of pins on network */          private List<LEPin> pins;
    /** List of networks on network */      private List<LENetwork> networks;

    protected LENetwork(String name) {
        this.name = name;
        pins = new ArrayList<LEPin>();
        networks = new ArrayList<LENetwork>();
    }

    protected void add(LEPin pin) { pins.add(pin); }
    protected void add(LENetwork net) { networks.add(net); }

    protected String getName() { return name; }
    protected Iterator<LENetwork> getSubNets() { return networks.iterator(); }

    protected List<LEPin> getAllPins() {
        List<LEPin> allpins = new ArrayList<LEPin>(pins);
        for (LENetwork net : networks) {
            allpins.addAll(net.getAllPins());
        }
        return allpins;
    }

    protected void print() {
        print("", System.out);
    }

    protected void print(String header, PrintStream out) {
        out.println(header+"Network "+name+", connects to: ");
        for (LEPin pin : pins) {
            LENodable leno = pin.getInstance();
            out.println(header+"  "+leno.printOneLine(""));
        }
        for (LENetwork net : networks) {
            net.print(header+"  ", out);
        }
    }
}
