package com.sun.electric.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

/** Jose uses JNetworks to represent connectivity.
 *
 * <p> For a Cell, each JNetwork represents a collection of PortInsts
 * that are electrically connected.  The JNetworks for a Cell and all
 * its descendents are created when the user calls
 * Cell.rebuildNetworks().
 *
 * <p> A Cell's JNetworks are <i>not</i> kept up to date when Cells
 * are modified.  If you modify Cells and wish to get an updated view
 * of the connectivity you <i>must</i> call Cell.rebuildNetworks() after all
 * modifications are complete.
 *
 * <p> For PrimitiveNodes, JNetworks are used to indicate which of
 * it's PrimitivePorts are connected.  For example, a MOS transistor
 * has one PrimitivePort at each end of its gate.  You can tell that
 * the two ends are connected because when you call getNetwork() on
 * the two PrimitivePorts you get the same JNetwork.  However,
 * PrimitivePort JNetworks are always empty; they contain no
 * PortInsts.
 *
 * <p> The JNetwork is a pure-java data structure. It does *not*
 * reference a c-side Electric Network object.
 *
 * <p> TODO: I need to generalize this to handle busses. */
public class JNetwork
{
	// ------------------------- private data ------------------------------
	private NodeProto parent; // Cell or Primitve node that owns this JNetwork
	private ArrayList portInsts;
	private TreeSet names = new TreeSet(); // Sorted list of names. The
	// first name is the most
	// appropriate.
	private static HashMap prims = new HashMap(); // Cache of known
	// primitive network
	// names.

	// ----------------------- protected and private methods -----------------
	private static void error(boolean pred, String msg)
	{
		Electric.error(pred, msg);
	}

	// used for PrimitivePorts
	private JNetwork(Collection names, NodeProto np)
	{
		this.parent = np;
		this.portInsts = new ArrayList();
		for (Iterator it = names.iterator(); it.hasNext();)
		{
			addName((String) it.next());
		}
	}

	// used to build Cell networks
	JNetwork(NodeProto np)
	{
		this(new ArrayList(), np);
	}

	/** Remove a part from this JNetwork.  If there are no more parts,
	 * the JNetwork will remove itself. */
	/*
	void removePart(Networkable part) {
	  if (!parts.contains(part)) {
	    // RKao debug  why is this suddenly complaining?
	    //System.out.println(this+" in "+parent+" does not contain "+part);
	    return;
	  }
	  parts.remove(part);
	  if (parts.size()==0) remove();
	}
	*/

	void addPortInst(PortInst port)
	{
		if (portInsts.contains(port))
		{
			System.out.println(
				this +" in " + parent + " already references " + port);
		} else
		{
			portInsts.add(port);
			port.setNetwork(this);
		}
	}

	void addName(String nm)
	{
		if (nm != null)
			names.add(nm);
	}

	private static void mergeSmallIntoBig(JNetwork bigNet, JNetwork smallNet)
	{
		if (bigNet == smallNet)
			return;

		bigNet.portInsts.addAll(smallNet.portInsts);
		bigNet.names.addAll(smallNet.names);

		for (Iterator it = smallNet.getPorts(); it.hasNext();)
		{
			((PortInst) it.next()).setNetwork(bigNet);
		}

		// Invalidate smallNet to force any use of smallNet to crash.
		smallNet.portInsts = null;
		smallNet.names = null;
	}

	/** Merge nets net0 and net1. Invalidates discarded net to catch
	 * anyone trying to reference it */
	static JNetwork merge(JNetwork net0, JNetwork net1)
	{
		error(
			net0.parent != net1.parent,
			"merging nets with different parents?");

		if (net0.portInsts.size() >= net1.portInsts.size())
		{
			mergeSmallIntoBig(net0, net1);
			return net0;
		} else
		{
			mergeSmallIntoBig(net1, net0);
			return net1;
		}
	}

	private static JNetwork findBiggestNet(Iterator nets)
	{
		int maxPorts = 0;
		JNetwork bigNet = null;
		while (nets.hasNext())
		{
			JNetwork net = (JNetwork) nets.next();
			int sz = net.portInsts.size();
			if (sz > maxPorts)
			{
				maxPorts = sz;
				bigNet = net;
			}
		}
		error(bigNet == null, "JNetwork.findBiggestNet: no networks?");
		return bigNet;
	}

	static JNetwork merge(HashSet nets)
	{
		JNetwork bigNet = findBiggestNet(nets.iterator());
		for (Iterator it = nets.iterator(); it.hasNext();)
		{
			mergeSmallIntoBig(bigNet, (JNetwork) it.next());
		}
		return bigNet;
	}

	/** Remove this JNetwork.  Actually, we just let the garbage collector
	 * take care of it. */
	void remove()
	{
	}

	/** Find the primitive network with a specific name */
	static JNetwork findPrimNetwork(String name, PrimitiveNode parent)
	{
		String searchterm =
			parent.getTechnology().getTechName()
				+ ":"
				+ parent.getProtoName()
				+ ":"
				+ name;
		JNetwork net = (JNetwork) prims.get(searchterm);
		if (net == null)
		{
			ArrayList names = new ArrayList();
			names.add(name);
			net = new JNetwork(names, parent);
			prims.put(searchterm, net);
		}
		return net;
	}

	/** Create a JNetwork based on this one, but attached to a new Cell */
	JNetwork copy(Cell f)
	{
		return new JNetwork(names, f);
	}

	// --------------------------- public methods ------------------------------
	public NodeProto getParent()
	{
		return parent;
	}

	/** A net can have multiple names. Return alphabetized list of names. */
	public Iterator getNames()
	{
		return names.iterator();
	}

	/** Returns true if nm is one of JNetwork's names */
	public boolean hasName(String nm)
	{
		return names.contains(nm);
	}

	/** TODO: write getNetwork() in JNetwork */
	public JNetwork getNetwork()
	{
		return this;
	}

	/** Get iterator over all PortInsts on JNetwork.  Note that the
	 * PortFilter class is useful for filtering out frequently excluded
	 * PortInsts.  */
	public Iterator getPorts()
	{
		return portInsts.iterator();
	}

	/** Get iterator over all Exports on JNetwork */
	public Iterator getExports()
	{
		ArrayList exports = new ArrayList();
		for (Iterator it = parent.getPorts(); it.hasNext();)
		{
			Export e = (Export) it.next();
			if (e.getNetwork() == this)
				exports.add(e);
		}
		return exports.iterator();
	}

	/** Get iterator over all ArcInsts on JNetwork */
	public Iterator getArcs()
	{
		ArrayList arcs = new ArrayList();
		for (Iterator it = ((Cell) parent).getArcs(); it.hasNext();)
		{
			ArcInst ai = (ArcInst) it.next();
			if (ai.getConnection(false).getPortInst().getNetwork() == this
				|| ai.getConnection(false).getPortInst().getNetwork() == this)
			{
				arcs.add(ai);
			}
		}
		return arcs.iterator();
	}
}
