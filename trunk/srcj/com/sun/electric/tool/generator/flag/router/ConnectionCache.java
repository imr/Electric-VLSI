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
