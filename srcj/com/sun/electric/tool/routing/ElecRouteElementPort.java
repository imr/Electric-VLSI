/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ElecRouteElementPort.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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

package com.sun.electric.tool.routing;

import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Specifies a port on a node in an Electrically specified route.
 */
public class ElecRouteElementPort {

    /** The port to use */                      private PortProto portProto;
    /** The node to use */                      private NodeProto nodeProto;
    /** A list of all ElecRouteElementArcs connected */ private List allArcs;

    public ElecRouteElementPort(PortProto portProto, NodeProto nodeProto) {
        this.portProto = portProto;
        this.nodeProto = nodeProto;
        allArcs = new ArrayList();
    }

    public PortProto getPortProto() { return portProto; }

    public NodeProto getNodeProto() { return nodeProto; }

    public void addArc(ElecRouteElementArc arc) { allArcs.add(arc); }

    public void removeArc(ElecRouteElementArc arc) { allArcs.remove(arc); }

    public Iterator getArcs() { return allArcs.iterator(); }

}
