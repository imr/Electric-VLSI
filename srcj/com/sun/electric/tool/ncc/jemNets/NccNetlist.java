/*
 */
package com.sun.electric.tool.ncc.jemNets;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Properties;

import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.database.text.Name;

import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.Wire;

/**
 */
public class NccNetlist {
	private NccGlobals globals;
	private ArrayList wires, parts, ports;
	
	// ---------------------------- public methods ---------------------------
	public NccNetlist(Cell root, VarContext context, Netlist netlist, 
					  NccGlobals globals) {
		this.globals = globals;

		Visitor v = new Visitor(globals);
		HierarchyEnumerator.enumerateCell(root, context, netlist, v);
		wires = v.getWireList();
		parts = v.getPartList();
		ports = v.getPortList();
	}
	public ArrayList getWireArray() {return wires;}
	public ArrayList getPartArray() {return parts;}
	public ArrayList getPortArray() {return ports;}
}

/** map from netID to NCC Wire */
class Wires {
	private ArrayList wires = new ArrayList();
	private void growIfNeeded(int ndx) {
		while(ndx>wires.size()-1) wires.add(null);
	}
	public Wire get(int netID, HierarchyEnumerator.CellInfo info) {
		growIfNeeded(netID);
		Wire wire = (Wire) wires.get(netID);
		if (wire==null) {
			String wireNm = info.getUniqueNetName(netID, "/");
			wire = new Wire(wireNm, info.isGlobalNet(netID));
			wires.set(netID, wire);
		}
		return wire;
	}
	// return non-null entries of wires array
	public ArrayList getWireArray() {
		ArrayList nonNullWires = new ArrayList();
		for (Iterator it=wires.iterator(); it.hasNext();) {
			Wire w = (Wire) it.next();
			if (w!=null) nonNullWires.add(w);
		}
		return nonNullWires;
	}
}

class NccCellInfo extends HierarchyEnumerator.CellInfo {
	private HashSet nodablesToDiscard;
	private HashMap nodableSizeMultipliers;

	// Compute a Nodable's hash code based upon its connectivity and Cell type
	private int computeNodableHashCode(Nodable no, Netlist netlist) {
		Cell c = (Cell) no.getProto();
		int hash = c.hashCode();
		for (Iterator it=c.getPorts(); it.hasNext();) {
			Export e = (Export) it.next();
			for (int i=0; i<e.getProtoNameKey().busWidth(); i++) {
				int netNdx = netlist.getNetIndex(no, e, i);
				hash = (hash<<1) ^ netNdx;
			}
		}
		return hash;
	}
	
	private HashMap hashNodablesBasedOnConnectivity(Netlist netlist) {
		HashMap codeToNodable = new HashMap();
		for (Iterator it=netlist.getNodables(); it.hasNext();) {
			Nodable no = (Nodable) it.next();
			NodeProto np = no.getProto();
			if (np instanceof Cell) {
				Integer c = new Integer(computeNodableHashCode(no, netlist));
				LinkedList ll = (LinkedList) codeToNodable.get(c);
				if (ll==null) {
					ll = new LinkedList();
					codeToNodable.put(c, ll);
				}
				ll.add(no);
			}
		}
		return codeToNodable;
	}
	
	private boolean areParalleled(Nodable n1, Nodable n2, Netlist netlist) {
		Cell c = (Cell) n1.getProto();
		Cell c2 = (Cell) n1.getProto();
		if (c2!=c) return false;
		for (Iterator it=c.getPorts(); it.hasNext();) {
			Export e = (Export) it.next();
			if (!netlist.sameNetwork(n1, e, n2, e)) return false;
		}
		return true;
	}
	// Find all parallel Nodables. For each group of paralleled Nodables 
	// discard all Nodables but the first. Record the number of Nodables in 
	// parallel.
	private void findParallelNodables(HashMap nodableSizeMultipliers, 
									  HashSet nodablesToDiscard, LinkedList ll,
									  Netlist netlist) {
		for (Iterator it=ll.iterator(); it.hasNext();) {
			Nodable first = (Nodable) it.next();
			it.remove();
			int count = 1;
			for (; it.hasNext();) {
				Nodable no = (Nodable) it.next();
				if (areParalleled(first, no, netlist)) {
					it.remove();
					nodablesToDiscard.add(no);
					count++;
				}
			}
			if (count>=2) nodableSizeMultipliers.put(first, new Integer(count));
		}
	}
	// Get size multiplier for nodable contained by current Cell	
	private int getSizeMultiplier(Nodable no) {
		Integer i = (Integer) nodableSizeMultipliers.get(no);
		return (i==null) ? 1 : i.intValue();
	}

	// Record the information we need to merge parallel instances of the same 
	// Cell
	public void recordMergeParallelInfo() {
		Netlist netlist = getNetlist();
		HashMap codeToNodables = hashNodablesBasedOnConnectivity(netlist);
		nodablesToDiscard = new HashSet();
		nodableSizeMultipliers = new HashMap();
		for (Iterator it=codeToNodables.keySet().iterator(); it.hasNext();) {
			LinkedList ll = (LinkedList) codeToNodables.get(it.next());
			findParallelNodables(nodableSizeMultipliers, nodablesToDiscard, ll, 
								 netlist);
		}
	}
	public boolean isDiscardable(Nodable no) {
		return nodablesToDiscard.contains(no);
	}
	/** Get size multipler for everything instantiated by the current Cell */
	public int getSizeMultiplier() {
		if (isRootCell()) return 1;
		NccCellInfo parentInfo = (NccCellInfo) getParentInfo();
		int parentMult = parentInfo.getSizeMultiplier();
		int nodableMult = parentInfo.getSizeMultiplier(getParentInst());
		return parentMult * nodableMult;
	}
}

class Visitor extends HierarchyEnumerator.Visitor {
	// --------------------------- private data -------------------------------
	private static final boolean debug = false;
	private NccGlobals globals;
	private int depth = 0;

	/** map from netID to Wire */	 private Wires wires = new Wires();
	/** all Parts in the net list */ private ArrayList parts = new ArrayList();
	/** all ports in the net list */ private ArrayList ports = new ArrayList();
	
	// --------------------------- private methods ----------------------------
	private void error(boolean pred, String msg) {
		globals.error(pred, msg);
	}
	private void spaces() {
		for (int i=0; i<depth; i++)	 globals.print(" ");
	}
	
	private void getExports(HierarchyEnumerator.CellInfo rootInfo) {
		Cell rootCell = rootInfo.getCell();
		Netlist rootNetlist = rootInfo.getNetlist();
		for (Iterator it=rootCell.getPorts(); it.hasNext();) {
			Export e = (Export) it.next();
			int[] expNetIDs = rootInfo.getExportNetIDs(e);
			for (int i=0; i<expNetIDs.length; i++) {
				Wire wire = wires.get(expNetIDs[i], rootInfo);
				String expName = e.getProtoNameKey().subname(i).toString(); 
				Port p = new Port(expName, wire);
				ports.add(p);
			}
		}
	}
	
	/** 
	 * Get the Wire that's attached to port pi. The Nodable must be an instance
	 * of a PrimitiveNode. 
	 * @return the attached Wire.
	 */
	private Wire getWireForPortInst(PortInst pi, 
					     		    HierarchyEnumerator.CellInfo info) {
		NodeInst ni = pi.getNodeInst();
		NodeProto np = ni.getProto();
		error(!(np instanceof PrimitiveNode), "not PrimitiveNode");
		PortProto pp = pi.getPortProto();
		int[] netIDs = info.getPortNetIDs(ni, pp);
		error(netIDs.length!=1, "Primitive Port connected to bus?");
		return wires.get(netIDs[0], info);
	}

	private void buildMOS(NodeInst ni, Transistor.Type type, NccCellInfo info){
		String name = ni.getName();
		int mul = info.getSizeMultiplier();
//		if (mul!=1) {
//			globals.println("mul="+mul+" for "+
//						   info.getContext().getInstPath("/")+"/"+name);
//		}
		double width=0, length=0;
		if (globals.getOptions().checkSizes) {
			Dimension dim = ni.getTransistorSize(info.getContext());
			width = mul * dim.getWidth();
			length = dim.getHeight();
		}
		Wire s = getWireForPortInst(ni.getTransistorSourcePort(), info);
		Wire g = getWireForPortInst(ni.getTransistorGatePort(), info);
		Wire d = getWireForPortInst(ni.getTransistorDrainPort(), info);
		Part t = new TransistorOne(type, name, width, length, s, g, d);
		parts.add(t);								 
	}
	
	private void doPrimitiveNode(NodeInst ni, NodeProto np, NccCellInfo info) {
		NodeProto.Function func = ni.getFunction();
		if (func==NodeProto.Function.TRA4NMOS) {
			// schematic NMOS
			buildMOS(ni, Transistor.NTYPE, info);
		} else if (func==NodeProto.Function.TRA4PMOS) {
			// schematic PMOS
			buildMOS(ni, Transistor.PTYPE, info);
		} else if (func==NodeProto.Function.TRANMOS) {
			// layout NMOS
			buildMOS(ni, Transistor.NTYPE, info);
		} else if (func==NodeProto.Function.TRAPMOS) {
			// layout PMOS
			buildMOS(ni, Transistor.PTYPE, info);
		} else if (func==NodeProto.Function.RESIST) {
//		    error(true, "can't handle Resistors yet");
		} else {	
//		    globals.println("NccNetlist not handled: func="+
//		    			   func+" proto="+ni.getProto().toString());
//		    error(true, "unrecognized PrimitiveNode");
		}
	}
	
	
	// --------------------------- public methods -----------------------------
	public HierarchyEnumerator.CellInfo newCellInfo() {
		return new NccCellInfo();
	}
	
	public boolean enterCell(HierarchyEnumerator.CellInfo ci) {
		NccCellInfo info = (NccCellInfo) ci;
		if (debug) {
			spaces();
			globals.println("Enter cell: " + info.getCell().getProtoName());
			depth++;
		}
		if (info.isRootCell()) getExports(info);
		info.recordMergeParallelInfo();
		return true;
	}

	public void exitCell(HierarchyEnumerator.CellInfo info) {
		if (debug) {
			depth--;
			spaces();
			globals.println("Exit cell: " + info.getCell().getProtoName());
		}
	}
	
	public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo ci) {
		NccCellInfo info = (NccCellInfo) ci;
		NodeProto np = no.getProto();
		if (np instanceof PrimitiveNode) {
			doPrimitiveNode((NodeInst)no, np, info);
			return false; 
		} else {
			error(!(np instanceof Cell), "expecting Cell");
			boolean expand = !info.isDiscardable(no);
//			if (!expand) {
//				globals.println("don't expand: "+
//					info.getContext().getInstPath("/")+"/"+no.getName());
//			}
			return expand;
		}
	}
	public ArrayList getWireList() {return wires.getWireArray();}
	public ArrayList getPartList() {return parts;}
	public ArrayList getPortList() {return ports;}
	
	public Visitor(NccGlobals globals) {this.globals=globals;}
}
