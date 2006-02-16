/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PortFilter.java
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
package com.sun.electric.tool.generator.layout;

import java.util.ArrayList;
import java.util.Iterator;

import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.PrimitiveNode;

/** A PortFilter is useful for removing certain PortInsts from the
 * list of PortInsts on a Network. It is built into Jose because
 * certain filters are commonly needed by Jose clients. */
public abstract class PortFilter {
    // --------------------------- public types -----------------------------
    
    /** Skip uninteresting elements of a schematic.
     *
     * <p> When most tools inspect a Network they are interested in
     * extracting the connectivity of "real" schematic elements such
     * resistors, transistors, and user defined Facets. These tools are
     * usually uninterested in the schematic constructs: Wire_Pins,
     * Bus_Pins, Off-Page, Facet-Center, and Invisible-Pin. They're also
     * usually uninterested in any icons of the schematic, itself.
     *
     * <p> This class is provided so that a user may extend it to skip
     * additional PortInsts.
     *
     * <p> For convenience, an instance of this class is provided in:
     *  <code>SCHEMATIC</code>. */
    public static class SchemPortFilter extends PortFilter {
	public boolean skipPort(PortInst pi) {
	    NodeInst ni = pi.getNodeInst();
	    if (ni.isIconOfParent()) return true;
	    
	    NodeProto np = ni.getProto();
	    String nm = np.getName();
	    return !ni.isCellInstance() &&
		(nm.equals("Wire_Pin") || nm.equals("Bus_Pin") ||
		 nm.equals("Off-Page") || nm.equals("Facet-Center") ||
		 nm.equals("Invisible-Pin"));
	}
    }
    
    // -------------------------- public constants -------------------------
    /** This constant object can be invoked on the return value of
     * Network.getPorts() omit PortInsts of the schematic elements that
     * are typically uninteresting to Jose clients reading
     * schematics. The following code describes a typical use:
     * <code>
     * Network net = // application initializes this variable
     * Iterator ports = PortFilter.SCHEMATIC.filter(net.getPorts());
     * while (ports.hasNext()) {
     *     // application only looks at "useful" schematics ports
     * }
     * </code> */
    public static final PortFilter SCHEMATIC = new SchemPortFilter();
    
    // ------------------------ public methods ----------------------------
    /** Should we filter out this PortInst? */
    public abstract boolean skipPort(PortInst pi);
    
    /** Remove selected PortInsts from the input list
     * @param ports an iterator over a collection of PortInsts. Note
     * that after filter() is called ports.hasNext()==false;
     * @return all PortInsts in ports except those excluded by
     * skipPort() */
    public final Iterator<PortInst> filter(Iterator<PortInst> ports) {
	ArrayList<PortInst> filtered = new ArrayList<PortInst>();
	while (ports.hasNext()) {
	    PortInst pi = ports.next();
	    if (!skipPort(pi)) filtered.add(pi);
	}
	return filtered.iterator();
    }
}

