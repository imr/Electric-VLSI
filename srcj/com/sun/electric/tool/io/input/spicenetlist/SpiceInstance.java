/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SpiceNetlistReader.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.input.spicenetlist;

import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.io.PrintStream;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Aug 3, 2006
 * Time: 4:18:45 PM
 * To change this template use File | Settings | File Templates.
 */

public class SpiceInstance {
    private char type;                  // spice type
    private String name;
    private List<String> nets;
    private SpiceSubckt subckt;              // may be null if primitive element
    private HashMap<String,String> params;

    public SpiceInstance(String typeAndName) {
        this.type = typeAndName.charAt(0);
        this.name = typeAndName.substring(1);
        this.nets = new ArrayList<String>();
        this.subckt = null;
        this.params = new LinkedHashMap<String,String>();
    }
    public SpiceInstance(SpiceSubckt subckt, String name) {
        this.type = 'x';
        this.name = name;
        this.nets = new ArrayList<String>();
        this.subckt = subckt;
        this.params = new LinkedHashMap<String,String>();
        for (String key : subckt.getParams().keySet()) {
            // set default param values
            this.params.put(key, subckt.getParams().get(key));
        }
    }
    public char getType() { return type; }
    public String getName() { return name; }
    public List<String> getNets() { return nets; }
    public void addNet(String net) { nets.add(net); }
    public HashMap<String,String> getParams() { return params; }
    public SpiceSubckt getSubckt() { return subckt; }
    public void write(PrintStream out) {
        StringBuffer buf = new StringBuffer();
        buf.append(type);
        buf.append(name);
        buf.append(" ");
        for (String net : nets) {
            buf.append(net); buf.append(" ");
        }
        if (subckt != null) {
            buf.append(subckt.getName());
            buf.append(" ");
        }
        for (String key : params.keySet()) {
            buf.append(key);
            String value = params.get(key);
            if (value != null) {
                buf.append("=");
                buf.append(value);
            }
            buf.append(" ");
        }
        buf.append("\n");
        SpiceNetlistReader.multiLinePrint(out, false, buf.toString());
    }
}
