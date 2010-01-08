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
package com.sun.electric.tool.ncc.netlist;
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
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NetNameProxy;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NodableNameProxy;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.TransistorSize;
import com.sun.electric.technology.PrimitiveNode.Function;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.basic.TransitiveRelation;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations.NamePattern;
import com.sun.electric.tool.ncc.netlist.NccNameProxy.PartNameProxy;
import com.sun.electric.tool.ncc.netlist.NccNameProxy.WireNameProxy;
import com.sun.electric.tool.ncc.processing.HierarchyInfo;
import com.sun.electric.tool.ncc.processing.SubcircuitInfo;
import com.sun.electric.tool.user.ncc.ExportConflict;
import com.sun.electric.tool.user.ncc.NccGuiInfo;
import com.sun.electric.tool.user.ncc.UnrecognizedPart;
import com.sun.electric.tool.Job;

/**
 * NCC's representation of a netlist.
 */
public class NccNetlist {
    public final static Netlist.ShortResistors SHORT_RESISTORS = Netlist.ShortResistors.PARASITIC;
	private final NccGlobals globals;
	private final Cell rootCell;
	private final VarContext rootContext;
	private ArrayList<Wire> wires;
	private ArrayList<Part> parts;
	private ArrayList<Port> ports;
	private boolean exportAssertionFailures;
	private boolean exportGlobalConflicts;
	private boolean badTransistorType;
	private boolean userAbort;

	// ---------------------------- public methods ---------------------------
	/** Build a netlist for Cell root. Mos transistors are represented by
	 * Mos. Instances of lower level Cells that have already been checked
	 * by NCC are represented by SubCircuit. Instances of lower level Cells 
	 * that haven't been checked by NCC are flattened.
	 * @param root the top Cell
	 * @param context the VarContext for the top Cell
	 * @param hierInfo information about what's already been checked by NCC
	 * @param blackBox we're not actually going to check so just build an
	 * empty netlist
	 * @param globals variables shared by all parts of NCC
	 */
	public NccNetlist(Cell root, VarContext context, 
					  HierarchyInfo hierInfo, boolean blackBox, 
					  NccGlobals globals) {
		this.globals = globals;
		rootCell = root; 
		rootContext = context;

		try {
			Visitor v = new Visitor(globals, hierInfo, blackBox, context);
			HierarchyEnumerator.enumerateCell(root.getNetlist(SHORT_RESISTORS), context, v, true);
			wires = v.getWireList();
			parts = v.getPartList();
			ports = v.getPortList();
			exportAssertionFailures = v.exportAssertionFailures();
			badTransistorType = v.badTransistorType();
		} catch (RuntimeException e) {
			if (e instanceof ExportGlobalConflict) {
				exportGlobalConflicts = true;
			} else if (e instanceof UserAbort) {
				userAbort = true;
			} else {
				throw e;
			}
		}
		// if there are net list errors then make net list look empty
		if (cantBuildNetlist())
		{
            wires = new ArrayList<Wire>();
			parts = new ArrayList<Part>();
			ports = new ArrayList<Port>();
		}
	}
	/** @return a list of all the Wires */
	public ArrayList<Wire> getWireArray() {return wires;}
	/** @return a list of all the Parts */
	public ArrayList<Part> getPartArray() {return parts;}
	/** @return a list of all the Ports */
	public ArrayList<Port> getPortArray() {return ports;}
	/** @return true if some error prevents us from building a netlist */
	public boolean cantBuildNetlist() {
		return exportAssertionFailures || exportGlobalConflicts ||
		       badTransistorType;
	}
	/** @return true if user has requested an abort of NCC run */
	public boolean userAbort() {return userAbort;}
	/** @return the root Cell */
	public Cell getRootCell() {return rootCell;}
	/** @return the VarContext of the root Cell */
	public VarContext getRootContext() {return rootContext;}
}

class ExportGlobalConflict extends RuntimeException {
	static final long serialVersionUID = 0;
}
class UserAbort extends RuntimeException {
	static final long serialVersionUID = 0;
}

/** map from netID to NCC Wire */
class Wires {
	private final ArrayList<Wire> wires = new ArrayList<Wire>();
	private final String pathPrefix; 
	private void growIfNeeded(int ndx) {
		while(ndx>wires.size()-1) wires.add(null);
	}
	/** Construct a Wire table. The mergedNetIDs argument specifies
	 * sets of net IDs that should point to the same Wire.
	 * @param mergedNetIDs contains sets of net IDs that must be merged
	 * into the same Wire. */
	public Wires(TransitiveRelation<Integer> mergedNetIDs, CellInfo info, 
	             String pathPrefix) {
		this.pathPrefix = pathPrefix;
		for (Iterator<Set<Integer>> it=mergedNetIDs.getSetsOfRelatives(); it.hasNext();) {
			Set<Integer> relatives = it.next();
			Iterator<Integer> ni = relatives.iterator();
			if (!ni.hasNext()) continue;
			int netId = ni.next().intValue();
			Wire w = get(netId, info);
			while (ni.hasNext()) {
				netId = ni.next().intValue();
				growIfNeeded(netId);
				wires.set(netId, w);
			}
		}
	}
	public Wire get(int netID, CellInfo info) {
		growIfNeeded(netID);
		Wire wire = wires.get(netID);
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
	public ArrayList<Wire> getWireArray() {
		Set<Wire> wireSet = new HashSet<Wire>();
		wireSet.addAll(wires);
		wireSet.remove(null);
		ArrayList<Wire> nonNullWires = new ArrayList<Wire>();
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
	public Iterator<ExportGlobal> getExportsAndGlobals() {
		HashMap<String,ExportGlobal> nameToExport = new HashMap<String,ExportGlobal>();
		// first collect all exports
		for (Iterator<PortProto> it=getCell().getPorts(); it.hasNext();) {
			Export e = (Export) it.next();
            if (ignoreExport(e.getName(), getAnnotations())) continue;
            int[] expNetIDs = getExportNetIDs(e);
			for (int i=0; i<expNetIDs.length; i++) {
				String nm = e.getNameKey().subname(i).toString();
				ExportGlobal eg = 
					new ExportGlobal(nm, expNetIDs[i],
						             e.getCharacteristic(), 
									 getNetlist().getNetwork(e,i), e);
				nameToExport.put(nm, eg);
			}
		}
		List<ExportGlobal> expGlob = new ArrayList<ExportGlobal>();
		expGlob.addAll(nameToExport.values());

		// next collect all the globals
		Global.Set globNets = getNetlist().getGlobals();
		for (int i=0; i<globNets.size(); i++) {
			Global g = globNets.get(i);
			String nm = g.getName();
			Network net = getNetlist().getNetwork(g);
			int netID = getNetID(net);
			PortCharacteristic type = globNets.getCharacteristic(g); 
			ExportGlobal eg = nameToExport.get(nm);
			if (eg!=null) {
				// Name collision between an export and a global signal. 
				// Discard the global.
				if (eg.netID!=netID || eg.type!=type) {
					if (eg.netID!=netID) {
						globals.prln(
							"  Error! Cell: "+getCell().libDescribe()+
							" has both an Export and a global signal "+
							"named: "+nm+" but their networks differ");
						
						// GUI
                        ExportConflict.NetworkConflict conf = 
                            new ExportConflict.NetworkConflict(getCell(), getContext(),
                                    nm, eg.network, net);
                        globals.getNccGuiInfo().addNetworkExportConflict(conf);                        
					}
					if (eg.type!=type) {
						globals.prln(
							"  Error! Cell: "+getCell().libDescribe()+
							" has both an Export and a global signal "+
							"named: "+nm+" but their Characteristics differ");
						// GUI
                        ExportConflict.CharactConflict conf = 
                            new ExportConflict.CharactConflict(getCell(), getContext(),
                                    nm, type.getFullName(), eg.type.getFullName(), eg.getExport());
                        globals.getNccGuiInfo().addCharactExportConflict(conf);
					}
					
					throw new ExportGlobalConflict();
				}
			} else {
				eg = new ExportGlobal(nm, netID, type, net);
				expGlob.add(eg);
			}
		}
		return expGlob.iterator();
	}

    private boolean ignoreExport(String name, NccCellAnnotations ann) {
        if (ann==null) return false;
        for (Iterator<List<NamePattern>> patit=ann.getExportsToIgnore(); patit.hasNext();) {
            List<NamePattern> patterns = patit.next();
            for (NamePattern pat : patterns) {
                if (pat.matches(name)) {
                    return true;
                }
            }
        }
        return false;
    }
}

/** Information from either an Export or a Global signal */
class ExportGlobal {
	private final Export export;
	public final String name;
	public final int netID;
	public final PortCharacteristic type;
	public final Network network;
	/** For Export 
	 * @param export is needed because this ExportGlobal 
	 * might be a piece of bussed Export; for example if this ExportGlobal
	 * is foo[1] but is only a part of the Export foo[1:3]. */
	public ExportGlobal(String nm, int id, PortCharacteristic ty, Network net,
			            Export export) {
		this.export=export;  name=nm;  netID=id;  type=ty;  network=net;
	}
	/** For Global */
	public ExportGlobal(String nm, int id, PortCharacteristic ty, Network net) {
		export=null; name=nm;  netID=id;  type=ty; network=net;
	}
	public boolean isExport() {return export!=null;}
	public Export getExport() {
		Job.error(export==null, "this is a Global, not an Export!");
		return export;
	}
}

class Visitor extends HierarchyEnumerator.Visitor {
	// --------------------------- private data -------------------------------
	private static final boolean debug = false;
	private static final Technology SCHEMATIC = Technology.findTechnology("schematic");
	private static final Function4PortTo3Port fourToThree = new Function4PortTo3Port();

	private final NccGlobals globals;
	/** If I'm building a netlist from a node that isn't at the top of the 
	 * design hierarchy then Part and Wire names all share a common prefix.
	 * This is VERY annoying to the user. Save the path prefix here so I can
	 * remove it whenever I build Parts or Wires. */
	private final String pathPrefix;
	private boolean exportAssertionFailures = false;
	private boolean badPartType = false;
	private int depth = 0;

	/** map from netID to Wire */	 
	private Wires wires;
	/** all Parts in the net list */ 
	private final ArrayList<Part> parts = new ArrayList<Part>();
	/** all ports in the net list */ 
	private final ArrayList<Port> ports = new ArrayList<Port>();
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
	private void addMatchingNetIDs(List<Integer> netIDs, NamePattern pattern, 
	                               NccCellInfo rootInfo) {
		for (Iterator<ExportGlobal> it=rootInfo.getExportsAndGlobals(); it.hasNext();) {
			ExportGlobal eg = it.next();
			if (pattern.matches(eg.name)) netIDs.add(new Integer(eg.netID));			 								
		}
	}
	private void doExportsConnAnnot(TransitiveRelation<Integer> mergedNetIDs,
	                                List<NamePattern> connected, NccCellInfo rootInfo) {
		List<Integer> netIDs = new ArrayList<Integer>();
		for (NamePattern np : connected) {
			addMatchingNetIDs(netIDs, np, rootInfo);
		}
		for (int i=1; i<netIDs.size(); i++) {
			mergedNetIDs.theseAreRelated(netIDs.get(0), netIDs.get(i));
		}
	}
	private void doExportsConnAnnots(TransitiveRelation<Integer> mergedNetIDs,
	                                 NccCellInfo rootInfo) {
		NccCellAnnotations ann = rootInfo.getAnnotations();
		if (ann==null) return;
		for (Iterator<List<NamePattern>> it=ann.getExportsConnected(); it.hasNext();) {
			doExportsConnAnnot(mergedNetIDs, it.next(), rootInfo);					                                     
		}
	}
	private void initWires(NccCellInfo rootInfo) {
		TransitiveRelation<Integer> mergedNetIDs = new TransitiveRelation<Integer>();
		doExportsConnAnnots(mergedNetIDs, rootInfo);
		wires = new Wires(mergedNetIDs, rootInfo, pathPrefix);
	}
	
	private void createPortsFromExports(NccCellInfo rootInfo) {
		boolean oneNamePerPort = globals.getOptions().oneNamePerPort;
		HashSet<Port> portSet = new HashSet<Port>();
		for (Iterator<ExportGlobal> it=rootInfo.getExportsAndGlobals(); it.hasNext();) {
			ExportGlobal eg = it.next();
			Wire wire = wires.get(eg.netID, rootInfo);
			portSet.add(wire.addExport(eg.name, eg.type, oneNamePerPort));
		}
		
		for (Port p : portSet) 
			ports.add(p);
	}

	/** Get the Wire that's attached to port pi. The Nodable must be an
	 * instance of a PrimitiveNode. 
	 * @return the attached Wire. */
	private Wire getWireForPortInst(PortInst pi, CellInfo info) {
		NodeInst ni = pi.getNodeInst();
		error(ni.isCellInstance(), "not PrimitiveNode");
		PortProto pp = pi.getPortProto();
		int[] netIDs = info.getPortNetIDs(ni, pp);
		error(netIDs.length!=1, "Primitive Port connected to bus?");
		return wires.get(netIDs[0], info);
	}
	
	private boolean isSchematicPrimitive(NodeInst ni) {
		return ni.getProto().getTechnology()==SCHEMATIC;
	}
	
	private Function getMosType(NodeInst ni, NccCellInfo info) {
		Function func = ni.getFunction();

		if (isSchematicPrimitive(ni)) {
			// This is a workaround to allow compatibility with the regression
			// suite. The old regressions use "transistorType" declarations. 
			
			// Designer convention:
			// If the Cell containing a schematic transistor primitive has an  
			// NCC declaration "transistorType" then get the type information 
			// from that annotation. Otherwise use the type of the transistor
			// primitive.
			Cell parent = ni.getParent();
			
			NccCellAnnotations ann = NccCellAnnotations.getAnnotations(parent);
			String typeNm = ann==null ? null : ann.getTransistorType();
			if (typeNm!=null) {
				func = PrimitiveNameToFunction.nameToFunction(typeNm);
				if (func==null) {
					badPartType = true;
					prln("  Unrecognized transistor type: "+typeNm);
					
					// GUI
					globals.getNccGuiInfo().addUnrecognizedPart(
		                    new UnrecognizedPart(ni.getParent(), info.getContext(), typeNm, ni));
				}
			}
		} /* else {
			// This is a workaround until we update the Technologies to utilize 
			// NodeProto.Function.
			if (func==Function.TRANMOS || func==Function.TRAPMOS) {
				String protoNm = ni.getProto().getName();
				Function funcOverride = PrimitiveNameToFunction.nameToFunction(protoNm);
				if (funcOverride!=null)  func=funcOverride;
			}
		}*/
		
		func = fourToThree.translate(func);
		
		return func;
	}

	private void buildMos(NodeInst ni, NccCellInfo info) {
		NodableNameProxy np = info.getUniqueNodableNameProxy(ni, "/");
		PartNameProxy name = new PartNameProxy(np, pathPrefix); 
		double width=0, length=0, mfactor=1;
		if (globals.getOptions().checkSizes) {
			TransistorSize dim = ni.getTransistorSize(info.getContext());
			width = dim.getDoubleWidth() * dim.getMFactor();
			length = dim.getDoubleLength();
		}
		Wire s = getWireForPortInst(ni.getTransistorSourcePort(), info);
		Wire g = getWireForPortInst(ni.getTransistorGatePort(), info);
		Wire d = getWireForPortInst(ni.getTransistorDrainPort(), info);
		PortInst biasPi = ni.getTransistorBiasPort();
		Wire w = globals.getOptions().checkBody && biasPi!=null ? ( 
			getWireForPortInst(biasPi, info)
		) : (
			null
		);
		Function type = getMosType(ni, info);
		// if unrecognized transistor type then ignore MOS
		if (type==null) return;
		
		Part t = w!=null ? (
			new Mos(type, name, width, length, s, g, d, w)
		) : (
			new Mos(type, name, width, length, s, g, d)
		);
		parts.add(t);
	}
	private void buildBipolar(NodeInst ni, NccCellInfo info) {
		NodableNameProxy np = info.getUniqueNodableNameProxy(ni, "/");
		PartNameProxy name = new PartNameProxy(np, pathPrefix); 
		double area=0;
		if (globals.getOptions().checkSizes) {
			TransistorSize dim = ni.getTransistorSize(info.getContext());
			area = dim!=null ? dim.getDoubleArea() : 0;
		}
		Wire e = getWireForPortInst(ni.getTransistorEmitterPort(), info);
		Wire b = getWireForPortInst(ni.getTransistorBasePort(), info);
		Wire c = getWireForPortInst(ni.getTransistorCollectorPort(), info);
		Function f = ni.getFunction();
		Part t = new Bipolar(f, name, area, e, b, c);
		parts.add(t);								 
	}
	private void buildTransistor(NodeInst ni, NccCellInfo info) {
		Function f = ni.getFunction();
		if (f.isFET()) {
			buildMos(ni, info);
		} else if (f.isBipolar()) {
			buildBipolar(ni, info);
		} else {
			globals.error(true, "Unrecognized primitive transistor: "+f.getShortName());
		}
	}
	private Function getResistorType(NodeInst ni, NccCellInfo info) {
		Function f = ni.getFunction();
		if (isSchematicPrimitive(ni)) {
			// This is a work-around to allow compatibility with older
			// regressions.
			// Designer convention:
			// Cell containing a schematic transistor primitive may have an  
			// NCC declaration "resistorType" with Primitive Node name.
			Cell parent = ni.getParent();
			NccCellAnnotations ann = NccCellAnnotations.getAnnotations(parent);
			String typeNm = ann==null ? null : ann.getResistorType();
			if (typeNm!=null) {
				Function funcOverride = PrimitiveNameToFunction.nameToFunction(typeNm);
				if (funcOverride!=null) {
					f = funcOverride;
				} else {
					badPartType = true;
					prln("  Unrecognized resistor type: "+typeNm);
					
					// GUI
					globals.getNccGuiInfo().addUnrecognizedPart(
		                    new UnrecognizedPart(ni.getParent(), info.getContext(), typeNm, ni));
				}
			}
		} else {
			// This is a work-around until we update Technologies to specify
			// the correct resistor Function.
			if (f==Function.PRESIST || f==Function.WRESIST) {
				String typeNm = ni.getProto().getName();
				if (typeNm!=null) {
					Function funcOverride = PrimitiveNameToFunction.nameToFunction(typeNm);
					if (funcOverride!=null) f = funcOverride;
				}
			}
		}
		return f;
	}
	private Wire[] getResistorWires(NodeInst ni, NccCellInfo info) {
		Wire a = getWireForPortInst(ni.getPortInst(0), info);
		Wire b = getWireForPortInst(ni.getPortInst(1), info);
		return new Wire[] {a, b};
	}
	private double getDoubleVariableValue(String varName, NodeInst ni, VarContext context) {
		Variable var = ni.getParameterOrVariable(varName);
		if (var==null) return 0;
		Object obj = context==null ? var.getObject() : context.evalVar(var, ni);
		return VarContext.objectToDouble(obj, 0);
	}
	private double[] getResistorSize(NodeInst ni, VarContext context) {
		double w=0, l=0;
		if (isSchematicPrimitive(ni)) {
			w = getDoubleVariableValue("ATTR_width", ni, context);
			l = getDoubleVariableValue("ATTR_length", ni, context);
		} else {
			w = ni.getLambdaBaseYSize();
			l = ni.getLambdaBaseXSize();
//			SizeOffset so = ni.getSizeOffset();
//			w = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();
//			l = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
		}
		return new double[] {w, l};
	}
	private void buildResistor(NodeInst ni, NccCellInfo info) {
		NodableNameProxy np = info.getUniqueNodableNameProxy(ni, "/");
		PartNameProxy name = new PartNameProxy(np, pathPrefix); 
		double width=0, length=0;
		if (globals.getOptions().checkSizes) {
			double[] dim = getResistorSize(ni, info.getContext());
			width = dim[0];
			length = dim[1];
		}
		Wire[] wires = getResistorWires(ni, info);
		Function type = getResistorType(ni, info);
		// if unrecognized resistor type then ignore 
		if (type!=null) {
			Part t = new Resistor(type, name, width, length, wires[0], wires[1]);
			parts.add(t);
		}
	}
	private void doPrimitiveNode(NodeInst ni, NccCellInfo info) {
		Function f = ni.getFunction();
		if (f.isTransistor()) {
			buildTransistor(ni, info);
		} else if (f.isResistor() && f!=Function.RESIST) {
			// We use normal resistors to model parasitic wire resistance.
			// NCC considers them to be "short circuits" and discards them. 
			buildResistor(ni, info);
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
	private void printExports(HashSet<ExportGlobal> exportNames) {
		pr("{ ");
		for (ExportGlobal eg : exportNames)
			pr(eg.name+" ");
		pr("}");
	}
	private void printExportAssertionFailure(HashMap<Wire,HashSet<ExportGlobal>> wireToExportGlobals,
	                                         NccCellInfo info) {
		String instPath = 
			NccNameProxy.removePrefix(pathPrefix, 
				                      info.getContext().getInstPath("/"));
		String cellName = NccUtils.fullName(info.getCell());
		prln("  Assertion: exportsConnectedByParent in cell: "+
			 cellName+" fails. Instance path is: "+instPath);
		prln("    The exports are connected to "+
		     wireToExportGlobals.size()+" different networks");
		for (Wire w : wireToExportGlobals.keySet()) {
			pr("    On network: "+w.getName()+" are exports: ");
			printExports(wireToExportGlobals.get(w));
			prln("");
		}
		// The GUI should put the following into one box
        VarContext context = info.getContext();
        Cell cell = info.getCell();

        NccGuiInfo cm = globals.getNccGuiInfo();
        Object[][] items = new Object[wireToExportGlobals.keySet().size()][];
        String[][] names = new String[wireToExportGlobals.keySet().size()][];
        int j = 0;
		for (Iterator<Wire> it=wireToExportGlobals.keySet().iterator(); it.hasNext(); j++) {
			HashSet<ExportGlobal> exportGlobals = wireToExportGlobals.get(it.next());
           
            items[j] = new Object[exportGlobals.size()];
            names[j] = new String[exportGlobals.size()];
            int i = 0;
			// The GUI should put the following on one line
			for (Iterator<ExportGlobal> it2=exportGlobals.iterator(); it2.hasNext(); i++) {
				ExportGlobal eg = it2.next();
                names[j][i] = eg.name;
				if (eg.isExport())
					items[j][i] = eg.getExport();
				else
                    items[j][i] = eg.network;
			}
		}
        cm.addExportAssertionFailure(cell,context,items, names);
	}
	private void matchExports(HashMap<Wire,HashSet<ExportGlobal>> wireToExportGlobals, NamePattern pattern,
							  NccCellInfo info) {
		for (Iterator<ExportGlobal> it=info.getExportsAndGlobals(); it.hasNext();) {
			ExportGlobal eg = it.next();
			if (!pattern.matches(eg.name)) continue;
			Wire wire = wires.get(eg.netID, info);
			HashSet<ExportGlobal> exportGlobals = wireToExportGlobals.get(wire);
			if (exportGlobals==null) {
				exportGlobals = new HashSet<ExportGlobal>();
				wireToExportGlobals.put(wire, exportGlobals);
			}
			exportGlobals.add(eg);
		}
	}
	private boolean exportAssertionFailure(List<NamePattern> patterns, NccCellInfo info) {
		// map from Wire to Set of Export Names
		HashMap<Wire,HashSet<ExportGlobal>> wireToExportGlobals = new HashMap<Wire,HashSet<ExportGlobal>>();
		for (NamePattern np : patterns) {
			matchExports(wireToExportGlobals, np, info);
		}
		if (wireToExportGlobals.size()<=1) return false;
		printExportAssertionFailure(wireToExportGlobals, info);
		return true;
	}
	private boolean exportAssertionFailures(NccCellInfo info) {
		NccCellAnnotations ann = info.getAnnotations();
		if (ann==null) return false;
		boolean gotError = false;
		for (Iterator<List<NamePattern>> it=ann.getExportsConnected(); it.hasNext();) 
			gotError |= exportAssertionFailure(it.next(), info);
		return gotError;	                                      	
	}
	
	private void doSubcircuit(SubcircuitInfo subcktInfo, NccCellInfo info) {
		Wire[] pins = new Wire[subcktInfo.numPorts()];

		for (Iterator<ExportGlobal> it=info.getExportsAndGlobals(); it.hasNext();) {
			ExportGlobal eg = it.next();
			Wire wire = wires.get(eg.netID, info);
			int pinNdx = subcktInfo.getPortIndex(eg.name);
			if (pinNdx!=-1)  addToPins(pins, pinNdx, wire);
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
	@Override
	public CellInfo newCellInfo() {
		return new NccCellInfo(globals);
	}
	
	@Override
	public boolean enterCell(CellInfo ci) {
		if (globals.userWantsToAbort()) throw new UserAbort();
		
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
			// the same compareList. Then we will first compare 
			// mux21{sch} with mux21{lay}, and then with mux21_r{lay}. For the 
			// second comparison hierarchicalCompareInfo will tell us to treat 
			// mux21{sch} as a primitive. Ignore it. We NEVER want to treat
			// the root Cell as a primitive.
			Cell cell = info.getCell();
			if (!parentSaysFlattenMe(info) &&
//			    hierarchicalCompareInfo!=null && 
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
	@Override
	public void exitCell(CellInfo info) {
		if (debug) {
			depth--;
			globals.status2(spaces()+"Exit cell: " + info.getCell().getName());
		}
	}
	@Override
	public boolean visitNodeInst(Nodable no, CellInfo ci) {
		NccCellInfo info = (NccCellInfo) ci;
		if (!no.isCellInstance()) {
			doPrimitiveNode((NodeInst)no, info);
			return false; 
		} else {
			error(!no.isCellInstance(), "expecting Cell");
//			boolean paralleled = info.isDiscardable(no);
//			return !paralleled;
			return true;
		}
	}
	// ---------------------- intended public interface -----------------------
	/** @return a list of Wires */
	public ArrayList<Wire> getWireList() {return wires.getWireArray();}
	/** @return a list of Parts */
	public ArrayList<Part> getPartList() {return parts;}
	/** @return a list of Ports */
	public ArrayList<Port> getPortList() {return ports;}
	/** Ensure that all subcircuits we instantiate have valid exportsConnectedByParent assertions.
	 * If not then this netlist isn't valid. */
	public boolean exportAssertionFailures() {return exportAssertionFailures;}
	/** @return true if we found a transistor type we don't recognize */
	public boolean badTransistorType() {return badPartType;}
	
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

