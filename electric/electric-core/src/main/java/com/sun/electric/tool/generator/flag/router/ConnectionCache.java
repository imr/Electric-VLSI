/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ConnectionCache.java
 *
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.generator.flag.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;

/** Object to allow efficient queries of the form: "Does PortInst
 * have any ArcInst connected?" 
 * 
 * Note that this doesn't work right yet because PortInsts can't
 * be compared using ==.  Maybe PortInst should overload equals()
 * and hashcode().
 * 
 * */
public class ConnectionCache {
	private Map<PortInst, List<Connection>> portToConns = 
		new HashMap<PortInst, List<Connection>>();  
	public ConnectionCache(NodeInst ni) {
		for (Iterator<Connection> cIt=ni.getConnections(); cIt.hasNext();) {
			Connection c = cIt.next();
			PortInst p = c.getPortInst();
			List<Connection> conns = portToConns.get(p);
			if (conns==null) {
				conns = new ArrayList<Connection>();
			}
			conns.add(c);
		}
	}
	
	public boolean hasConnections(PortInst pi) {
		List<Connection> conns = portToConns.get(pi);
		return conns!=null;
	}

}
