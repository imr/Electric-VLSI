package com.sun.electric.tool.generator.layout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.tool.ncc.basic.NccUtils;

/** A "netlist-like" object that allows you to find the Nodables
 * attached to a Network, and the Networks attached to a 
 * Nodable. This is the sort of functionality one needs when
 * tracing connectivity through netlists.
 * <p>
 * This is not very object-oriented. However, if we integrated
 * this functionality into all Nodables and Networks then
 * every user would have to pay for its overhead. */
public class NodaNets {
	/** A 1-bit "port-like" object for Nodables */
	public static class NodaPortInst {
		private final Nodable noda;
		private final PortProto port;
		private final int index;
		private final Network net;
		NodaPortInst(Nodable no, PortProto pp, int ndx, Network ne) {
			noda=no; port=pp; index=ndx; net=ne;
		}
		
		public Nodable getNodable() {return noda;}
		public PortProto getPortProto() {return port;}
		public int getIndex() {return index;}
		public Network getNet() {return net;}
	}
	private Map<String, Nodable> nameToNoda = 
		new HashMap<String, Nodable>();
	private Map<String, Network> nameToNet = 
		new HashMap<String, Network>();
	private Map<Nodable, ArrayList<NodaPortInst>> nodeToPorts = 
		new HashMap<Nodable, ArrayList<NodaPortInst>>();
	private Map<Network, ArrayList<NodaPortInst>> netToPorts = 
		new HashMap<Network, ArrayList<NodaPortInst>>();
	
	public NodaNets(Cell c, boolean shortResistors) {
		Date start = new Date();
		Netlist nets = c.getNetlist(shortResistors);
		for (Iterator<Nodable> it=c.getNodables(); it.hasNext();) {
			Nodable no = it.next();
			LayoutLib.error(nameToNoda.containsKey(no.getName()),
					        "Nodable name not unique: ");
			nameToNoda.put(no.getName(), no);
			NodeProto np = no.getProto();
			for (Iterator<PortProto> it2=np.getPorts(); it2.hasNext();) {
				PortProto pp = it2.next();
				Name key = pp.getNameKey();
				int numBits = key.busWidth();
				for (int busNdx=0; busNdx<numBits; busNdx++) {
					Network net = nets.getNetwork(no, pp, busNdx);
					// Apparently Networks don't get allocated for some Nodes such as 
					// the center. In that case just pretend the port doesn't exist.
					if (net==null) continue;
					for (Iterator<String> it3=net.getNames(); it3.hasNext();) {
						String netNm = it3.next();
						Network exists = nameToNet.get(netNm);
						if (exists!=null) {
							LayoutLib.error(exists!=net, "Net name not unique");
						} else {
							nameToNet.put(netNm, net);
						}
					}
					NodaPortInst npi = new NodaPortInst(no, pp, busNdx, net);
					ArrayList<NodaPortInst> ports = nodeToPorts.get(no);
					if (ports==null) {
						ports = new ArrayList<NodaPortInst>();
						nodeToPorts.put(no, ports);
					}
					ports.add(npi);
					ports = netToPorts.get(net);
					if (ports==null) {
						ports = new ArrayList<NodaPortInst>();
						netToPorts.put(net, ports);
					}
					ports.add(npi);
				}
			}
		}
		Date end = new Date();
		System.out.println("    RK Debug: Time to build NodaNets: "+NccUtils.hourMinSec(start, end));
	}
	public Collection<Network> getNets() {
		return Collections.unmodifiableCollection(nameToNet.values());
	}
	public Network getNet(String netNm) {return nameToNet.get(netNm);}
	
	public Collection<Nodable> getNodes() {
		return Collections.unmodifiableCollection(nameToNoda.values());
	}
	public Nodable getNoda(String nodaNm) {return nameToNoda.get(nodaNm);}
	/** Find ports attached to a Nodable */
	public Collection<NodaPortInst> getPorts(Nodable noda) {
		Collection<NodaPortInst> ports = nodeToPorts.get(noda);
		return Collections.unmodifiableCollection(ports);
	}
	/** Find ports attached to a Network */
	public Collection<NodaPortInst> getPorts(Network net) {
		Collection<NodaPortInst> ports = netToPorts.get(net);
		return Collections.unmodifiableCollection(ports);
	}
}
