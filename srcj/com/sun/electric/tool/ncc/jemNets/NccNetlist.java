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

import com.sun.electric.tool.ncc.jemNets.NccNameProxy.WireNameProxy;
import com.sun.electric.tool.ncc.jemNets.NccNameProxy.PartNameProxy;

import com.sun.electric.technology.Technology;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.CellInfo;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NetNameProxy;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NodableNameProxy;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
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
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
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
	private boolean exportAssertionFailures;
	private boolean exportGlobalConflicts;
	private boolean badTransistorType;

	// ---------------------------- public methods ---------------------------
	public NccNetlist(Cell root, VarContext context, Netlist netlist, 
					  HierarchyInfo hierInfo, boolean blackBox, 
					  NccGlobals globals) {
		this.globals = globals;
		rootCell = root; 
		rootContext = context;

		try {
			Visitor v = new Visitor(globals, hierInfo, blackBox, context);
			HierarchyEnumerator.enumerateCell(root, context, netlist, v, true);
			wires = v.getWireList();
			parts = v.getPartList();
			ports = v.getPortList();
			exportAssertionFailures = v.exportAssertionFailures();
		} catch (RuntimeException e) {
			if (e instanceof ExportGlobalConflict) {
				exportGlobalConflicts = true;
			} else if (e instanceof BadTransistorType) {
				badTransistorType = true;
			} else {
				throw e;
			}
		}
	}
	public ArrayList getWireArray() {return wires;}
	public ArrayList getPartArray() {return parts;}
	public ArrayList getPortArray() {return ports;}
	public boolean netlistErrors() {
		return exportAssertionFailures || exportGlobalConflicts ||
		       badTransistorType;
	}
	public Cell getRootCell() {return rootCell;}
	public VarContext getRootContext() {return rootContext;}
}

class ExportGlobalConflict extends RuntimeException {}
class BadTransistorType extends RuntimeException {}

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
			NetNameProxy np = info.getUniqueNetNameProxy(netID, "/");
			WireNameProxy wireNm = new WireNameProxy(np, pathPrefix);
			wire = new Wire(wireNm);
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
	private NccGlobals globals;
	// I'm caching the annotations because otherwise every Cell
	// instance is going to regenerate annotations for it's parent.
	private boolean gotAnnotations = false;
	private NccCellAnnotations annotations;
	
	// ----------------------------- public methods ---------------------------
	public NccCellInfo(NccGlobals globals) {this.globals=globals;}

	public NccCellAnnotations getAnnotations() {
		if (!gotAnnotations) 
			annotations = NccCellAnnotations.getAnnotations(getCell());
		gotAnnotations = true;
		return annotations;
	}
	// Subtle:  Suppose a schematic network has an export on it named "gnd"
	// and suppose it is also attached to a global signal "gnd". Then we may
	// create two "Ports" for the network, one for the global and one for the
	// export. The problem is both these ports have the same name: "gnd". This
	// violates the invariant that no two Ports may have the same name. I 
	// believe it's better to preserve the invariant by discarding the global 
	// "gnd" if there is already an Export named "gnd".
	public Iterator getExportsAndGlobals() {
		HashMap nameToExport = new HashMap();
		// first collect all exports
		for (Iterator it=getCell().getPorts(); it.hasNext();) {
			Export e = (Export) it.next();
			int[] expNetIDs = getExportNetIDs(e);
			for (int i=0; i<expNetIDs.length; i++) {
				String nm = e.getNameKey().subname(i).toString();
				ExportGlobal eg = 
					new ExportGlobal(nm,
						             expNetIDs[i],
						             e.getCharacteristic());
				nameToExport.put(nm, eg);
			}
		}
		List expGlob = new ArrayList();
		expGlob.addAll(nameToExport.values());

		// next collect all the globals
		Global.Set globNets = getNetlist().getGlobals();
		for (int i=0; i<globNets.size(); i++) {
			Global g = globNets.get(i);
			String nm = g.getName();
			int netID = getNetID(getNetlist().getNetwork(g));
			PortCharacteristic type = globNets.getCharacteristic(g); 
			ExportGlobal eg = (ExportGlobal) nameToExport.get(nm);
			if (eg!=null) {
				// Name collision between an export and a global signal. 
				// Discard the global.
				if (eg.netID!=netID || eg.type!=type) {
					System.out.println(
					    "  Error! Cell: "+getCell().libDescribe()+
						" has both an Export and a global signal "+
					    "named: "+nm+" but their networks or "+
					    "Characteristics differ");
					throw new ExportGlobalConflict();
					//LayoutLib.error(true, "schematics need repair");
				}
			} else {
				eg = new ExportGlobal(nm, netID, type);
				expGlob.add(eg);
			}
		}
		return expGlob.iterator();
	}
}

/** Information from either an Export or a Global signal */
class ExportGlobal {
	public final String name;
	public final int netID;
	public final PortCharacteristic type;
	public ExportGlobal(String nm, int id, PortCharacteristic ty) {
		name=nm;  netID=id;  type=ty;
	}
}



/** Iterate over a Cell's Exports and global signals. This is useful because
 * NCC creates a Port for each of them. 
 * <p>TODO: RK This was a stupid idea. The next time this has a bug delete it and 
 * replace it with a straightforward loop that returns a list!!! */
//class ExportGlobalIter {
//	private final int CHECK_EXPORT = 0;
//	private final int INIT_GLOBAL = 1;
//	private final int CHECK_BIT_IN_BUS = 2;
//	private final int CHECK_GLOBAL = 3;
//	private final int DONE = 4;
//	private CellInfo info;
//	private Iterator expIt;
//	private Export export;
//	private int[] expNetIDs;
//	private int bitInBus;
//	private Global.Set globals;
//	private int globNdx;
//	private int state = CHECK_EXPORT;
//	private ExportGlobal current;
//	
//	private void advance() {
//		switch (state) {
//		  case CHECK_EXPORT:
//			if (!expIt.hasNext()) {
//				state=INIT_GLOBAL;  advance();  break;
//			}
//			export = (Export) expIt.next();
//			expNetIDs = info.getExportNetIDs(export);
//			bitInBus = 0;
//		  case CHECK_BIT_IN_BUS:
//			if (bitInBus>=expNetIDs.length) {
//				state=CHECK_EXPORT;  advance();  break;
//			}
//			Name nameKey = export.getNameKey();
//			current = new ExportGlobal(nameKey.subname(bitInBus).toString(),
//									   expNetIDs[bitInBus],
//									   export.getCharacteristic());
//			bitInBus++;
//			state=CHECK_BIT_IN_BUS;  break;
//		  case INIT_GLOBAL:
//			globals = info.getNetlist().getGlobals();
//			globNdx = 0;
//		  case CHECK_GLOBAL:
//			if (globNdx>=globals.size()) {
//				state=DONE;  break;
//			}
//			Global global = globals.get(globNdx);
//			Network net = info.getNetlist().getNetwork(global);
//			current = new ExportGlobal(global.getName(),
//									   info.getNetID(net),
//									   globals.getCharacteristic(global));
//			globNdx++;
//			state=CHECK_GLOBAL;  break;
//		  case DONE:
//		  	state=DONE;  break;
//		  default:
//		  	LayoutLib.error(true, "no such state!");
//		}
//	}
//	public ExportGlobalIter(CellInfo info) {
//		this.info = info;
//		expIt = info.getCell().getPorts();
//		advance();
//	}
//	public boolean hasNext() {return state!=DONE;}
//	public ExportGlobal next() {
//		LayoutLib.error(state==DONE, "next() called when DONE");
//		ExportGlobal e = current;
//		advance();
//		return e;
//	}
//}

class Visitor extends HierarchyEnumerator.Visitor {
	// --------------------------- private data -------------------------------
	private static final boolean debug = false;
	private static final Technology SCHEMATIC = Technology.findTechnology("schematic");
	private final NccGlobals globals;
	/** If I'm building a netlist from a node that isn't at the top of the 
	 * design hierarchy then Part and Wire names all share a common prefix.
	 * This is VERY annoying to the user. Save the path prefix here so I can
	 * remove it whenever I build Parts or Wires. */
	private final String pathPrefix;
	private boolean exportAssertionFailures = false;
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
	                               NccCellInfo rootInfo) {
		for (Iterator it=rootInfo.getExportsAndGlobals(); it.hasNext();) {
			ExportGlobal eg = (ExportGlobal) it.next();
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
	
	private void createPortsFromExports(NccCellInfo rootInfo) {
		HashSet portSet = new HashSet();
		for (Iterator it=rootInfo.getExportsAndGlobals(); it.hasNext();) {
			ExportGlobal eg = (ExportGlobal) it.next();
			Wire wire = wires.get(eg.netID, rootInfo);
			portSet.add(wire.addExport(eg.name, eg.type));
		}
		
		for (Iterator it=portSet.iterator(); it.hasNext();) 
			ports.add(it.next());
	}
	
	/** Get the Wire that's attached to port pi. The Nodable must be an 
	 * instance of a PrimitiveNode. 
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
	
	private boolean isInstanceOfSchematicPrimitive(NodeInst ni) {
		return ni.getProto().getTechnology()==SCHEMATIC;
	}
	
	private boolean isNmosPrimitive(NodeInst ni) {
		PrimitiveNode.Function func = ni.getFunction();

		if (func==PrimitiveNode.Function.TRA4NMOS ||
	        func==PrimitiveNode.Function.TRANMOS) {
			return true;
		} else {
			LayoutLib.error(func!=PrimitiveNode.Function.TRA4PMOS &&
			                func!=PrimitiveNode.Function.TRAPMOS,
							"Not NMOS or PMOS Primitive Node");
			return false;
		}
	}
	
	private Transistor.Type getTransistorType(NodeInst ni) {
		String typeNm;
		if (isInstanceOfSchematicPrimitive(ni)) {
			Cell parent = ni.getParent();
			NccCellAnnotations ann = NccCellAnnotations.getAnnotations(parent);
			typeNm = ann==null ? null : ann.getTransistorType();
			if (typeNm==null) {
				// No transistorType annotation. Use defaults.
				typeNm = isNmosPrimitive(ni) ? "N-Transistor" : "P-Transistor";
			}
		} else {
			typeNm = ni.getProto().getName();
		}
		Transistor.Type t = Transistor.TYPES.getTypeFromLongName(typeNm);
		if (t==null) {
			System.out.println("  Unrecognized transistor type: "+typeNm);
			throw new BadTransistorType();
		}
		return t;
	}

	private void buildMOS(NodeInst ni, NccCellInfo info) {
		NodableNameProxy np = info.getUniqueNodableNameProxy(ni, "/");
		PartNameProxy name = new PartNameProxy(np, pathPrefix); 
		double width=0, length=0;
		if (globals.getOptions().checkSizes) {
			TransistorSize dim = ni.getTransistorSize(info.getContext());
			width = dim.getDoubleWidth();
			length = dim.getDoubleLength();
		}
		Wire s = getWireForPortInst(ni.getTransistorSourcePort(), info);
		Wire g = getWireForPortInst(ni.getTransistorGatePort(), info);
		Wire d = getWireForPortInst(ni.getTransistorDrainPort(), info);
		Transistor.Type type = getTransistorType(ni);
		Part t = new Transistor(type, name, width, length, s, g, d);
		parts.add(t);								 
	}
	
	private void doPrimitiveNode(NodeInst ni, NodeProto np, NccCellInfo info) {
		PrimitiveNode.Function func = ni.getFunction();
		if (ni.isPrimitiveTransistor()) {
			buildMOS(ni, info);
		} else if (func==PrimitiveNode.Function.RESIST) {
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
		for (Iterator it=info.getExportsAndGlobals(); it.hasNext();) {
			ExportGlobal eg = (ExportGlobal) it.next();
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
	private boolean exportAssertionFailure(List patterns, NccCellInfo info) {
		// map from Wire to Set of Export Names
		HashMap wireToExports = new HashMap();
		for (Iterator it=patterns.iterator(); it.hasNext();) {
			matchExports(wireToExports, (NamePattern) it.next(), info);
		}
		if (wireToExports.size()<=1) return false;
		printExportAssertionFailure(wireToExports, info);
		return true;
	}
	private boolean exportAssertionFailures(NccCellInfo info) {
		NccCellAnnotations ann = info.getAnnotations();
		if (ann==null) return false;
		for (Iterator it=ann.getExportsConnected(); it.hasNext();)
			if (exportAssertionFailure((List)it.next(), info)) return true;
		return false;	                                      	
	}
	
	private void doSubcircuit(SubcircuitInfo subcktInfo, NccCellInfo info) {
		Cell cell = info.getCell();
		Wire[] pins = new Wire[subcktInfo.numPorts()];

		for (Iterator it=info.getExportsAndGlobals(); it.hasNext();) {
			ExportGlobal eg = (ExportGlobal) it.next();
			Wire wire = wires.get(eg.netID, info);
			int pinNdx = subcktInfo.getPortIndex(eg.name);
			addToPins(pins, pinNdx, wire);
		}
		for (int i=0; i<pins.length; i++) 
			globals.error(pins[i]==null, "disconnected subcircuit pins!");

		CellInfo parentInfo = info.getParentInfo();
		Nodable parentInst = info.getParentInst();
		NodableNameProxy np = parentInfo.getUniqueNodableNameProxy(parentInst, "/");
		PartNameProxy name = new PartNameProxy(np, pathPrefix); 

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
			boolean exportAssertFail = exportAssertionFailures(info);
			exportAssertionFailures |= exportAssertFail;
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
				!exportAssertFail) {
				SubcircuitInfo subcktInfo = 
					hierarchicalCompareInfo.getSubcircuitInfo(cell);
				doSubcircuit(subcktInfo, info);
				return false;			
			} 
		}
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
	/**
	 * Ensure that all subcircuits we instantiate have valid exportsConnectedByParent assertions.
	 * If not then this netlist isn't valid.
	 */
	public boolean exportAssertionFailures() {return exportAssertionFailures;}
	
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

