/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NccNetlist.java
 *
 * Copyright (c) 2003 Sun Microsystems and Free Software
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
package com.sun.electric.tool.ncc.jemNets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.CellInfo;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NameProxy;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.TransistorSize;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.basic.TransitiveRelation;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations.NamePattern;
import com.sun.electric.tool.ncc.processing.HierarchyInfo;
import com.sun.electric.tool.ncc.processing.SubcircuitInfo;

/**
 * NCC's representation of a netlist.
 */
public class NccNetlist {
	private final NccGlobals globals;
	private final Cell rootCell;
	private final VarContext rootContext;
	private ArrayList wires, parts, ports;
	private boolean exportAssertionsOK;

	// ---------------------------- public methods ---------------------------
	public NccNetlist(Cell root, VarContext context, Netlist netlist, 
					  HierarchyInfo hierInfo, boolean blackBox, 
					  NccGlobals globals) {
		this.globals = globals;
		rootCell = root; 
		rootContext = context;

		Visitor v = new Visitor(globals, hierInfo, blackBox, context);
		HierarchyEnumerator.enumerateCell(root, context, netlist, v, true);
		wires = v.getWireList();
		parts = v.getPartList();
		ports = v.getPortList();
		exportAssertionsOK = v.exportAssertionsOK();
	}
	public ArrayList getWireArray() {return wires;}
	public ArrayList getPartArray() {return parts;}
	public ArrayList getPortArray() {return ports;}
	public boolean exportAssertionsOK() {return exportAssertionsOK;}
	public Cell getRootCell() {return rootCell;}
	public VarContext getRootContext() {return rootContext;}
}

/** map from netID to NCC Wire */
class Wires {
	private final ArrayList wires = new ArrayList();
	private final String pathPrefix; 
	private void growIfNeeded(int ndx) {
		while(ndx>wires.size()-1) wires.add(null);
	}
	/** Construct a Wire table. The mergedNetIDs argument specifies
	 * sets of net IDs that should point to the same Wire.
	 * @param mergedNetIDs contains sets of net IDs that must be merged
	 * into the same Wire. */
	public Wires(TransitiveRelation mergedNetIDs, CellInfo info, 
	             String pathPrefix) {
		this.pathPrefix = pathPrefix;
		for (Iterator it=mergedNetIDs.getSetsOfRelatives(); it.hasNext();) {
			Set relatives = (Set) it.next();
			Iterator ni = relatives.iterator();
			if (!ni.hasNext()) continue;
			int netId = ((Integer)ni.next()).intValue();
			Wire w = get(netId, info);
			while (ni.hasNext()) {
				netId = ((Integer)ni.next()).intValue();
				growIfNeeded(netId);
				wires.set(netId, w);
			}
		}
	}
	public Wire get(int netID, CellInfo info) {
		growIfNeeded(netID);
		Wire wire = (Wire) wires.get(netID);
		if (wire==null) {
//			String wireNm = info.getUniqueNetName(netID, "/");
//			wireNm = NccUtils.removePathPrefix(wireNm, pathPrefix);
			NameProxy np = info.getUniqueNetNameProxy(netID, "/");
			NccNameProxy wireNm = new NccNameProxy(np, pathPrefix);
			wire = new Wire(wireNm/*, info.isGlobalNet(netID)*/);
			wires.set(netID, wire);
		}
		return wire;
	}
	// return non-null entries of wires array
	// Eliminate duplicated Wires.
	public ArrayList getWireArray() {
		Set wireSet = new HashSet();
		wireSet.addAll(wires);
		wireSet.remove(null);
		ArrayList nonNullWires = new ArrayList();
		nonNullWires.addAll(wireSet);
		return nonNullWires;
	}
}

class NccCellInfo extends CellInfo {
	//private HashSet nodablesToDiscard = new HashSet();
	//private HashMap nodableSizeMultipliers = new HashMap();
	private NccGlobals globals;
	// I'm caching the annotations because otherwise every Cell
	// instance is going to regenerate annotations for it's parent.
	private boolean gotAnnotations = false;
	private NccCellAnnotations annotations;
	public NccCellAnnotations getAnnotations() {
		if (!gotAnnotations) 
			annotations = NccCellAnnotations.getAnnotations(getCell());
		gotAnnotations = true;
		return annotations;
	}

	// Compute a Nodable's hash code based upon its connectivity and Cell type
//	private int computeNodableHashCode(Nodable no, Netlist netlist) {
//		Cell c = (Cell) no.getProto();
//		int hash = c.hashCode();
//		for (Iterator it=c.getPorts(); it.hasNext();) {
//			Export e = (Export) it.next();
//			int[] netIDs = getPortNetIDs(no, e);
//			for (int i=0; i<netIDs.length; i++) {
//				int netID = netIDs[i];
//				hash = (hash<<1) ^ netID;
//			}
//		}
//		return hash;
//	}
	
//	private HashMap hashNodablesBasedOnConnectivity(Netlist netlist) {
//		HashMap codeToNodable = new HashMap();
//		for (Iterator it=netlist.getNodables(); it.hasNext();) {
//			Nodable no = (Nodable) it.next();
//			NodeProto np = no.getProto();
//			if (np instanceof Cell) {
//				Integer c = new Integer(computeNodableHashCode(no, netlist));
//				LinkedList ll = (LinkedList) codeToNodable.get(c);
//				if (ll==null) {
//					ll = new LinkedList();
//					codeToNodable.put(c, ll);
//				}
//				ll.add(no);
//			}
//		}
//		return codeToNodable;
//	}
	
//	private boolean areParalleled(Nodable n1, Nodable n2, Netlist netlist) {
//		Cell c = (Cell) n1.getProto();
//		Cell c2 = (Cell) n2.getProto();
//		if (c2!=c) return false;
//		for (Iterator it=c.getPorts(); it.hasNext();) {
//			Export e = (Export) it.next();
//			int[] netIDs1 = getPortNetIDs(n1, e);
//			int[] netIDs2 = getPortNetIDs(n2, e);
//			for (int i=0; i<netIDs1.length; i++) {
//				if (netIDs1[i]!=netIDs2[i]) return false;
//			}
//		}
//		return true;
//	}
	// Find all parallel Nodables. For each group of paralleled Nodables 
	// discard all Nodables but the first. Record the number of Nodables in 
	// parallel.
//	private void findParallelNodables(LinkedList ll, Netlist netlist) {
//		for (Iterator it=ll.iterator(); it.hasNext();) {
//			Nodable first = (Nodable) it.next();
//			it.remove();
//			int count = 1;
//			for (; it.hasNext();) {
//				Nodable no = (Nodable) it.next();
//				if (areParalleled(first, no, netlist)) {
//					it.remove();
//					nodablesToDiscard.add(no);
//					count++;
//				}
//			}
//			if (count>=2) nodableSizeMultipliers.put(first, new Integer(count));
//		}
//	}
	// Get size multiplier for nodable contained by current Cell	
//	private int getSizeMultiplier(Nodable no) {
//		Integer i = (Integer) nodableSizeMultipliers.get(no);
//		return (i==null) ? 1 : i.intValue();
//	}
	
	// ----------------------------- public methods ---------------------------
	public NccCellInfo(NccGlobals globals) {this.globals=globals;}

	// Record the information we need to merge parallel instances of the same 
	// Cell
//	public void recordMergeParallelInfo() {
//		Netlist netlist = getNetlist();
//		HashMap codeToNodables = hashNodablesBasedOnConnectivity(netlist);
//		for (Iterator it=codeToNodables.keySet().iterator(); it.hasNext();) {
//			LinkedList ll = (LinkedList) codeToNodables.get(it.next());
//			findParallelNodables(ll, netlist);
//		}
//	}
//	public boolean isDiscardable(Nodable no) {
//		return nodablesToDiscard.contains(no);
//	}
	/** Get size multipler for everything instantiated by the current Cell */
//	public int getSizeMultiplier() {
//		if (isRootCell()) return 1;
//		NccCellInfo parentInfo = (NccCellInfo) getParentInfo();
//		int parentMult = parentInfo.getSizeMultiplier();
//		int nodableMult = parentInfo.getSizeMultiplier(getParentInst());
//		return parentMult * nodableMult;
//	}
}

/** Information from either an Export or a Global signal */
class ExportGlobal {
	public final String name;
	public final int netID;
	public final PortProto.Characteristic type;
	public ExportGlobal(String nm, int id, PortProto.Characteristic ty) {
		name=nm;  netID=id;  type=ty;
	}
}

/** Iterate over a Cell's Exports and global signals. This is useful because
 * NCC creates a Port for each of them. 
 * <p>TODO: RK This was a stupid idea. The next time this has a bug delete it and 
 * replace it with a straightforward loop that returns a list!!! */
class ExportGlobalIter {
	private final int CHECK_EXPORT = 0;
	private final int INIT_GLOBAL = 1;
	private final int CHECK_BIT_IN_BUS = 2;
	private final int CHECK_GLOBAL = 3;
	private final int DONE = 4;
	private CellInfo info;
	private Iterator expIt;
	private Export export;
	private int[] expNetIDs;
	private int bitInBus;
	private Global.Set globals;
	private int globNdx;
	private int state = CHECK_EXPORT;
	private ExportGlobal current;
	
	private void advance() {
		switch (state) {
		  case CHECK_EXPORT:
			if (!expIt.hasNext()) {
				state=INIT_GLOBAL;  advance();  break;
			}
			export = (Export) expIt.next();
			expNetIDs = info.getExportNetIDs(export);
			bitInBus = 0;
		  case CHECK_BIT_IN_BUS:
			if (bitInBus>=expNetIDs.length) {
				state=CHECK_EXPORT;  advance();  break;
			}
			Name nameKey = export.getNameKey();
			current = new ExportGlobal(nameKey.subname(bitInBus).toString(),
									   expNetIDs[bitInBus],
									   export.getCharacteristic());
			bitInBus++;
			state=CHECK_BIT_IN_BUS;  break;
		  case INIT_GLOBAL:
			globals = info.getNetlist().getGlobals();
			globNdx = 0;
		  case CHECK_GLOBAL:
			if (globNdx>=globals.size()) {
				state=DONE;  break;
			}
			Global global = globals.get(globNdx);
			int netIndex = info.getNetlist().getNetIndex(global);
			current = new ExportGlobal(global.getName(),
									   info.getNetID(netIndex),
									   globals.getCharacteristic(global));
			globNdx++;
			state=CHECK_GLOBAL;  break;
		  case DONE:
		  	state=DONE;  break;
		  default:
		  	LayoutLib.error(true, "no such state!");
		}
	}
	public ExportGlobalIter(CellInfo info) {
		this.info = info;
		expIt = info.getCell().getPorts();
		advance();
	}
	public boolean hasNext() {return state!=DONE;}
	public ExportGlobal next() {
		LayoutLib.error(state==DONE, "next() called when DONE");
		ExportGlobal e = current;
		advance();
		return e;
	}
}

class Visitor extends HierarchyEnumerator.Visitor {
	// --------------------------- private data -------------------------------
	private static final boolean debug = false;
	private final NccGlobals globals;
	/** If I'm building a netlist from a node that isn't at the top of the 
	 * design hierarchy then Part and Wire names all share a common prefix.
	 * This is VERY annoying to the user. Save the path prefix here so I can
	 * remove it whenever I build Parts or Wires. */
	private final String pathPrefix;
	private boolean exportAssertionsOK = true;
	private int depth = 0;

	/** map from netID to Wire */	 
	private Wires wires;
	/** all Parts in the net list */ 
	private final ArrayList parts = new ArrayList();
	/** all ports in the net list */ 
	private final ArrayList ports = new ArrayList();
	/** treat these Cells as primitives */
	private final HierarchyInfo hierarchicalCompareInfo;
	/** generate only hierarchical comparison information */
	private final boolean blackBox;
	
	// --------------------------- private methods ----------------------------
	private void error(boolean pred, String msg) {globals.error(pred, msg);}
	private String spaces() {
		StringBuffer sp = new StringBuffer();
		for (int i=0; i<depth; i++)	 sp.append(" ");
		return sp.toString();
	}
	private void addMatchingNetIDs(List netIDs, NamePattern pattern, 
	                               CellInfo rootInfo) {
		for (ExportGlobalIter it=new ExportGlobalIter(rootInfo); it.hasNext();) {
			ExportGlobal eg = it.next();
			if (pattern.matches(eg.name)) netIDs.add(new Integer(eg.netID));			 								
		}
	}
	private void doExportsConnAnnot(TransitiveRelation mergedNetIDs,
	                                List connected, NccCellInfo rootInfo) {
		List netIDs = new ArrayList();
		for (Iterator it=connected.iterator(); it.hasNext();) {
			addMatchingNetIDs(netIDs, (NamePattern) it.next(), rootInfo);
		}
		for (int i=1; i<netIDs.size(); i++) {
			mergedNetIDs.theseAreRelated(netIDs.get(0), netIDs.get(i));
		}
	}
	private void doExportsConnAnnots(TransitiveRelation mergedNetIDs,
	                                 NccCellInfo rootInfo) {
		NccCellAnnotations ann = rootInfo.getAnnotations();
		if (ann==null) return;
		for (Iterator it=ann.getExportsConnected(); it.hasNext();) {
			doExportsConnAnnot(mergedNetIDs, (List)it.next(), rootInfo);					                                     
		}
	}
	private void initWires(NccCellInfo rootInfo) {
		TransitiveRelation mergedNetIDs = new TransitiveRelation();
		doExportsConnAnnots(mergedNetIDs, rootInfo);
		wires = new Wires(mergedNetIDs, rootInfo, pathPrefix);
	}
	
	private void createPortsFromExports(CellInfo rootInfo){
		HashSet portSet = new HashSet();
		for (ExportGlobalIter it=new ExportGlobalIter(rootInfo); it.hasNext();) {
			ExportGlobal eg = it.next();
			Wire wire = wires.get(eg.netID, rootInfo);
			portSet.add(wire.addExport(eg.name, eg.type));
		}
//		Cell rootCell = rootInfo.getCell();
//		for (Iterator it=rootCell.getPorts(); it.hasNext();) {
//			Export e = (Export) it.next();
//			PortProto.Characteristic type = e.getCharacteristic();
//			int[] expNetIDs = rootInfo.getExportNetIDs(e);
//			for (int i=0; i<expNetIDs.length; i++) {
//				Wire wire = wires.get(expNetIDs[i], rootInfo);
//				String expName = e.getNameKey().subname(i).toString();
//				portSet.add(wire.addExport(expName, type));
//			}
//		}
//		// create exports for global schematic signals
//		Netlist rootNetlist = rootInfo.getNetlist();
//		Global.Set globals = rootNetlist.getGlobals();
//		for (int i=0; i<globals.size(); i++) {
//			Global global = globals.get(i);
//			PortProto.Characteristic type = globals.getCharacteristic(global);
//			String globName = global.getName();
//			int netIndex = rootNetlist.getNetIndex(global);
//			int netID = rootInfo.getNetID(netIndex);
//			Wire wire = wires.get(netID, rootInfo);
//			portSet.add(wire.addExport(globName, type));
//		}
		
		for (Iterator it=portSet.iterator(); it.hasNext();) 
			ports.add(it.next());
	}
	
	/** Get the Wire that's attached to port pi. The Nodable must be an instance
	 * of a PrimitiveNode. 
	 * @return the attached Wire. */
	private Wire getWireForPortInst(PortInst pi, CellInfo info) {
		NodeInst ni = pi.getNodeInst();
		NodeProto np = ni.getProto();
		error(!(np instanceof PrimitiveNode), "not PrimitiveNode");
		PortProto pp = pi.getPortProto();
		int[] netIDs = info.getPortNetIDs(ni, pp);
		error(netIDs.length!=1, "Primitive Port connected to bus?");
		return wires.get(netIDs[0], info);
	}

	private void buildMOS(NodeInst ni, Transistor.Type type, NccCellInfo info) {
//		String name = info.getUniqueNodableName(ni, "/");
//		name = NccUtils.removePathPrefix(name, pathPrefix);
		NameProxy np = info.getUniqueNodableNameProxy(ni, "/");
		NccNameProxy name = new NccNameProxy(np, pathPrefix); 
//		int mul = info.getSizeMultiplier();
//		if (mul!=1) {
//			globals.println("mul="+mul+" for "+
//						   info.getContext().getInstPath("/")+"/"+name);
//		}
		double width=0, length=0;
		if (globals.getOptions().checkSizes) {
			TransistorSize dim = ni.getTransistorSize(info.getContext());
			width = dim.getDoubleWidth();
			length = dim.getDoubleLength();
		}
		Wire s = getWireForPortInst(ni.getTransistorSourcePort(), info);
		Wire g = getWireForPortInst(ni.getTransistorGatePort(), info);
		Wire d = getWireForPortInst(ni.getTransistorDrainPort(), info);
		Part t = new Transistor(type, name, width, length, s, g, d);
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
	
	private void addToPins(Wire[] pins, int pinNdx, Wire w) {
		if (pins[pinNdx]==null) {
			pins[pinNdx] = w;
		} else {
			globals.error(pins[pinNdx]!=w, 
						  "exports that should be connected aren't");
		}
	}
	private void pr(String s) {System.out.print(s);}
	private void prln(String s) {System.out.println(s);}
	private void printExports(HashSet exportNames) {
		pr("{ ");
		for (Iterator it=exportNames.iterator(); it.hasNext();)
			pr((String) it.next()+" ");
		pr("}");
	}
	private void printExportAssertionFailure(HashMap wireToExports,
	                                         NccCellInfo info) {
		String instPath = info.getContext().getInstPath("/");
		String cellName = NccUtils.fullName(info.getCell());
		prln("Assertion: exportsConnectedByParent in cell: "+
			 cellName+" fails. Instance path is: "+instPath);
		prln("    The exports are connected to "+
		     wireToExports.size()+" different networks");
		for (Iterator it=wireToExports.keySet().iterator(); it.hasNext();) {
			Wire w = (Wire) it.next();
			pr("    On network: "+w.getName()+" are exports: ");
			printExports((HashSet) wireToExports.get(w));
			prln("");
		}
	}
	private void matchExports(HashMap wireToExports, NamePattern pattern,
							  NccCellInfo info) {
		for (ExportGlobalIter it=new ExportGlobalIter(info); it.hasNext();) {
			ExportGlobal eg = it.next();
			if (!pattern.matches(eg.name)) continue;
			Wire wire = wires.get(eg.netID, info);
			HashSet exports = (HashSet) wireToExports.get(wire);
			if (exports==null) {
				exports = new HashSet();
				wireToExports.put(wire, exports);
			}
			exports.add(eg.name);
		}
	}
	private boolean exportAssertionOK(List patterns, NccCellInfo info) {
		// map from Wire to Set of Export Names
		HashMap wireToExports = new HashMap();
		for (Iterator it=patterns.iterator(); it.hasNext();) {
			matchExports(wireToExports, (NamePattern) it.next(), info);
		}
		if (wireToExports.size()<=1) return true;
		printExportAssertionFailure(wireToExports, info);
		return false;
	}
	private boolean exportAssertionsOK(NccCellInfo info) {
		NccCellAnnotations ann = info.getAnnotations();
		if (ann==null) return true;
		boolean ok = true;
		for (Iterator it=ann.getExportsConnected(); it.hasNext();)
			ok &= exportAssertionOK((List)it.next(), info);
		return ok;	                                      	
	}
	
	private void doSubcircuit(SubcircuitInfo subcktInfo, NccCellInfo info) {
		Cell cell = info.getCell();
		Wire[] pins = new Wire[subcktInfo.numPorts()];

		for (ExportGlobalIter it=new ExportGlobalIter(info); it.hasNext();) {
			ExportGlobal eg = it.next();
			Wire wire = wires.get(eg.netID, info);
			int pinNdx = subcktInfo.getPortIndex(eg.name);
			addToPins(pins, pinNdx, wire);
		}
//		for (Iterator it=cell.getPorts(); it.hasNext();) {
//			Export e = (Export) it.next();
//			int[] expNetIDs = info.getExportNetIDs(e);
//			for (int i=0; i<expNetIDs.length; i++) {
//				Wire wire = wires.get(expNetIDs[i], info);
//				String expName = e.getNameKey().subname(i).toString();
//				int pinNdx = subcktInfo.getPortIndex(expName);
//				addToPins(pins, pinNdx, wire);
//			}
//		}
//		// connect pins corresponding to Exports created for global 
//		// schematic signals
//		Netlist netlist = info.getNetlist();
//		Global.Set globalNets = netlist.getGlobals();
//		for (int i=0; i<globalNets.size(); i++) {
//			Global global = globalNets.get(i);
//			String globalName = global.getName();
//			int pinNdx = subcktInfo.getPortIndex(globalName);
//			int netIndex = netlist.getNetIndex(global);
//			int netID = info.getNetID(netIndex);
//			Wire wire = wires.get(netID, info);
//			addToPins(pins, pinNdx, wire);
//		}
		for (int i=0; i<pins.length; i++) 
			globals.error(pins[i]==null, "disconnected subcircuit pins!");

		CellInfo parentInfo = info.getParentInfo();
		Nodable parentInst = info.getParentInst();
		NameProxy np = parentInfo.getUniqueNodableNameProxy(parentInst, "/");
		NccNameProxy name = new NccNameProxy(np, pathPrefix); 

//		String instName = info.getParentInst().getName();
		parts.add(new Subcircuit(name, subcktInfo, pins));
	}
	/** Check to see if the parent of the current Cell instance says to
	 * flatten the current Cell instance */
	private boolean parentSaysFlattenMe(NccCellInfo info) {
		if (info.isRootCell()) return false;
		NccCellInfo parentInfo = (NccCellInfo) info.getParentInfo();
		//if (!parentInfo.isRootCell()) return false;
		NccCellAnnotations parentAnn = parentInfo.getAnnotations();
		if (parentAnn==null) return false;
		Nodable no = info.getParentInst();
		String instName = no.getName();
		return parentAnn.flattenInstance(instName);
	}
	
	// --------------------------- public methods -----------------------------
	public CellInfo newCellInfo() {
		return new NccCellInfo(globals);
	}
	
	public boolean enterCell(CellInfo ci) {
		NccCellInfo info = (NccCellInfo) ci;
		if (debug) {
			globals.status2(spaces()+"Enter cell: " + info.getCell().getName());
			depth++;
		}
		if (info.isRootCell()) {
			initWires(info);
			createPortsFromExports(info);
			// "black box" means only use Export information. Ignore contents.
			if (blackBox) return false;
		} else {
			// We need to test exportsConnectedByParent assertions here
			// because if assertions fail then doSubcircuit will attempt
			// to connect multiple wires to the same port.
			boolean exportAssertOK = exportAssertionsOK(info);
			exportAssertionsOK &= exportAssertOK;
			// Subtle! Suppose mux21{sch}, mux21{lay}, and mux21_r{lay} are in 
			// the same (simulated) CellGroup. Then we will first compare 
			// mux21{sch} with mux21{lay}, and then with mux21_r{lay}. For the 
			// second comparison hierarchicalCompareInfo will tell us to treat 
			// mux21{sch} as a primitive. Ignore it. We NEVER want to treat
			// the root Cell as a primitive.
			Cell cell = info.getCell();
			if (!parentSaysFlattenMe(info) &&
			    hierarchicalCompareInfo!=null && 
				hierarchicalCompareInfo.treatAsPrimitive(cell) &&
				exportAssertOK) {
				SubcircuitInfo subcktInfo = 
					hierarchicalCompareInfo.getSubcircuitInfo(cell);
				doSubcircuit(subcktInfo, info);
				return false;			
			} 
		}
//		if (globals.getOptions().mergeParallelCells) {  
//			info.recordMergeParallelInfo();
//		}
		return true;
	}

	public void exitCell(CellInfo info) {
		if (debug) {
			depth--;
			globals.status2(spaces()+"Exit cell: " + info.getCell().getName());
		}
	}
	
	public boolean visitNodeInst(Nodable no, CellInfo ci) {
		NccCellInfo info = (NccCellInfo) ci;
		NodeProto np = no.getProto();
		if (np instanceof PrimitiveNode) {
			doPrimitiveNode((NodeInst)no, np, info);
			return false; 
		} else {
			error(!(np instanceof Cell), "expecting Cell");
//			boolean paralleled = info.isDiscardable(no);
//			return !paralleled;
			return true;
		}
	}
	// ---------------------- intended public interface -----------------------
	public ArrayList getWireList() {return wires.getWireArray();}
	public ArrayList getPartList() {return parts;}
	public ArrayList getPortList() {return ports;}
	/** Do all subcircuits we instantiate have valid exportsConnectedByParent 
	 * assertions? If not then this netlist isn't valid. */
	public boolean exportAssertionsOK() {return exportAssertionsOK;}
	
	public Visitor(NccGlobals globals, 
	               HierarchyInfo hierarchicalCompareInfo,
	               boolean blackBox,
	               VarContext context) {
		this.globals = globals;
		this.hierarchicalCompareInfo = hierarchicalCompareInfo;
		this.blackBox = blackBox;
		this.pathPrefix = context.getInstPath("/");
	}
}
//abstract class ExportGlobalEnumerator {
//public void enumerate(CellInfo info) {
//	  Cell rootCell = info.getCell();  								 
//	  for (Iterator it=rootCell.getPorts(); it.hasNext();) {
//		  Export e = (Export) it.next();
//		  int[] expNetIDs = info.getExportNetIDs(e);
//		  for (int i=0; i<expNetIDs.length; i++) {
//			  String expName = e.getNameKey().subname(i).toString();
//			  exportGlobal(expName, expNetIDs[i]);
//		  }
//	  }
//	  // global schematic signals
//	  Netlist rootNetlist = info.getNetlist();
//	  Global.Set globals = rootNetlist.getGlobals();
//	  for (int i=0; i<globals.size(); i++) {
//		  Global global = globals.get(i);
//		  String globName = global.getName();
//		  int netIndex = rootNetlist.getNetIndex(global);
//		  int netID = info.getNetID(netIndex);
//		  exportGlobal(globName, netID);
//	  }
//}
//abstract void exportGlobal(String name, int netID);
//}

//class C extends ExportGlobalEnumerator {
//	List netIDs;
//	NamePattern pattern;
//	C(List ids, NamePattern pat) {
//		netIDs=ids;  pattern=pat;
//	}
//	void exportGlobal(String name, int netID) {
//		if (pattern.matches(name)) netIDs.add(new Integer(netID));			 								
//	}
//}
//(new C(netIDs, pattern)).enumerate(rootInfo);
//
//Cell rootCell = rootInfo.getCell();  								 
//for (Iterator it=rootCell.getPorts(); it.hasNext();) {
//	Export e = (Export) it.next();
//	int[] expNetIDs = rootInfo.getExportNetIDs(e);
//	for (int i=0; i<expNetIDs.length; i++) {
//		String expName = e.getNameKey().subname(i).toString();
//		if (pattern.matches(expName)) netIDs.add(new Integer(expNetIDs[i]));
//	}
//}
//// global schematic signals
//Netlist rootNetlist = rootInfo.getNetlist();
//Global.Set globals = rootNetlist.getGlobals();
//for (int i=0; i<globals.size(); i++) {
//	Global global = globals.get(i);
//	String globName = global.getName();
//	int netIndex = rootNetlist.getNetIndex(global);
//	int netID = rootInfo.getNetID(netIndex);
//	if (pattern.matches(globName)) netIDs.add(new Integer(netID));
//}
