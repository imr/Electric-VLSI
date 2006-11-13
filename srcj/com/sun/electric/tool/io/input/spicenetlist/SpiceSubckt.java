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

import java.util.*;
import java.io.PrintStream;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Aug 3, 2006
 * Time: 4:18:45 PM
 * To change this template use File | Settings | File Templates.
 */

public class SpiceSubckt {
    public enum PortType { IN, OUT, BIDIR }

    private String name;
    private List<String> ports;
    private HashMap<String,String> params;
    private List<SpiceInstance> instances;
    private HashMap<String,PortType> porttypes;
    public SpiceSubckt(String name) {
        this.name = name;
        this.ports = new ArrayList<String>();
        this.params = new LinkedHashMap<String,String>();
        this.instances = new ArrayList<SpiceInstance>();
        this.porttypes = new HashMap<String,PortType>();
    }
    public String getName() { return name; }
    public void addPort(String port) { ports.add(port); }
    public boolean hasPort(String portname) { return ports.contains(portname.toLowerCase()); }
    public List<String> getPorts() { return ports; }
    public String getParamValue(String name) { return params.get(name.toLowerCase()); }
    public HashMap<String,String> getParams() { return params; }
    void addInstance(SpiceInstance inst) { instances.add(inst); }
    public List<SpiceInstance> getInstances() { return instances; }
    public void setPortType(String port, PortType type) {
        if (ports.contains(port) && type != null)
            porttypes.put(port, type);
    }
    public PortType getPortType(String port) { return porttypes.get(port.toLowerCase()); }
    public void write(PrintStream out) {
        StringBuffer buf = new StringBuffer(".subckt ");
        buf.append(name);
        buf.append(" ");
        for (String port : ports) {
            buf.append(port);
            buf.append(" ");
        }
        for (String key : params.keySet()) {
            buf.append(key);
            buf.append("=");
            buf.append(params.get(key));
            buf.append(" ");
        }
        buf.append("\n");
        SpiceNetlistReader.multiLinePrint(out, false, buf.toString());
        for (SpiceInstance inst : instances) {
            inst.write(out);
        }
        out.println(".ends "+name);
    }
}
