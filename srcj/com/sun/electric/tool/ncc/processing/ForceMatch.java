package com.sun.electric.tool.ncc.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Network;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Wire;
import com.sun.electric.tool.ncc.netlist.NccNameProxy.WireNameProxy;
import com.sun.electric.tool.ncc.strategy.Strategy;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;
import com.sun.electric.tool.ncc.trees.LeafEquivRecords;

public class ForceMatch {
	private static class PartitionEquivRecord extends Strategy {
		private Map<Wire, Integer> wireToIndex;
		
		@Override
		public Integer doFor(NetObject n) {
			Integer index = wireToIndex.get(n);
			// add 1000 to index to avoid collision with 0 which means "no change"
			return index==null ? 0 : (1000+index.intValue());
		}
		
		public PartitionEquivRecord(EquivRecord er, Map<Wire, Integer> wireToIndex, NccGlobals globals) {
			super(globals);
			this.wireToIndex = wireToIndex;
			doFor(er);
		}
	}
	private NccGlobals globals;
	/*
	private Wire findWireInRootCell(String wireName, Iterator<EquivRecord> erIt, int desNdx) {
		Cell rootCell = globals.getRootCells()[desNdx];
		while (erIt.hasNext()) {
			EquivRecord er = erIt.next();
			Iterator<Circuit> ci = er.getCircuits();
			Circuit ckt = ci.next();
			for (int i=0; i<desNdx; i++) ckt = ci.next();
			for (Iterator<NetObject> ni = ckt.getNetObjs(); ni.hasNext();) {
				Wire w = (Wire) ni.next();
				WireNameProxy prox = w.getNameProxy();
				if (prox.leafCell()==rootCell && prox.getNet().hasName(wireName)) {
					return w;
				}
			}
		}
		return null;
	}
	
	private Wire findWireInRootCell(String wireName, int desNdx) {
		LeafEquivRecords ler = globals.getWireLeafEquivRecs();
		Wire w = findWireInRootCell(wireName, ler.getNotMatched(), desNdx);
		if (w!=null) return w;
		w = findWireInRootCell(wireName, ler.getMatched(), desNdx);
		return w;
	}
	
	private EquivRecord getCommonEquivRec(Set<Wire> wires) {
		EquivRecord er = null;
		for (Wire w : wires) {
			if (er==null) {
				er = w.getParent().getParent();
			} else {
				if (w.getParent().getParent()!=er) return null;
			}
		}
		return er;
	}
	
	private void forceWireMatch(String wireName, Cell[] cells) {
		int numDesigns = cells.length;
		globals.pr("  Forcing match of Wires named: \""+wireName+"\" in Cells: ");
		for (int i=0; i<numDesigns; i++) 
			globals.pr(cells[i].describe(true)  +" ");
		globals.prln("");

		Set<Wire> wires = new HashSet<Wire>();
		for (int i=0; i<numDesigns; i++) {
			Wire w = findWireInRootCell(wireName, i);
			if (w==null) {
				globals.prln("    Couldn't find Wire named: \""+wireName+"\" in Cell: "+cells[i].describe(true));
				return;
			}
			wires.add(w);
		}
		EquivRecord er = getCommonEquivRec(wires);
		if (er==null) {
			globals.prln("    Couldn't force match because Wires already mismatched by local partitioning");
			return;
		}
		if (er.isMatched()) {
			globals.prln("    Wires already matched by local partitioning");
			return;
		}
		new PartitionEquivRecord(er, wires, globals);
		
	}
	*/
	private List<String> getNamesOfWiresToForceMatch() {
		// eliminate duplicates in case of user error
		Set<String> forceNames = new HashSet<String>();

		Cell[] cells = globals.getRootCells();
		for (int i=0; i<cells.length; i++) {
			Cell c = cells[i];
			NccCellAnnotations ann = NccCellAnnotations.getAnnotations(c);
			if (ann==null) continue;
			forceNames.addAll(ann.getForceWireMatches());
		}
		List<String> forceNameList = new ArrayList<String>();
		forceNameList.addAll(forceNames);
		return forceNameList;
	}
	
	private int indexOfMatchingName(List<String> forceNames, Network net) {
		for (int i=0; i<forceNames.size(); i++) {
			if (net.hasName(forceNames.get(i))) return i;
		}
		return -1;
	}
	/** Make sure that for each name we have one Wire for each root Cell.
	 * Print messages saying we're forcing matches.
	 * Print messages if we can't find names. 
	 * Clear Wires if name match is incomplete.
	 * @param forceNames
	 * @param forceWires */
	private Map<Wire, Integer> buildForceWireMap(List<String> forceNames, Wire[][] forceWires) {
		Map<Wire, Integer> wireToNdx = new HashMap<Wire, Integer>();
		
		for (int nameNdx=0; nameNdx<forceNames.size(); nameNdx++) {
			String forceName = forceNames.get(nameNdx);
			boolean haveAllWires = true;
			Cell[] rootCells = globals.getRootCells();
			for (int desNdx=0; desNdx<forceWires.length; desNdx++) {
				if (forceWires[desNdx][nameNdx]==null) {
					globals.prln("  forceMatch: Can't find Wire named: "+forceName+
							     " in Cell: "+rootCells[desNdx].describe(false));
					haveAllWires = false;
				}
			}
			if (haveAllWires) {
				globals.pr("  Forcing match of Wires named: "+forceName+" in Cells: ");
				for (int desNdx=0; desNdx<rootCells.length; desNdx++) {
					globals.pr(rootCells[desNdx].describe(false)+" ");
				}
				globals.prln("");
				for (int desNdx=0; desNdx<forceWires.length; desNdx++) {
					wireToNdx.put(forceWires[desNdx][nameNdx], nameNdx);
				}
			}
		}
		
		return wireToNdx;
	}
	
	/** @return Wire[circuitIndex][nameIndex] */
	private Wire[][] findWiresToForceMatch(List<String> forceNames) {
		Wire[][] forceWires = new Wire[globals.getRootCells().length][];
		                               
		Cell[] rootCells = globals.getRootCells();
		EquivRecord wires = globals.getWires();
		int cktNdx = 0;
		for (Iterator<Circuit> cit=wires.getCircuits(); cit.hasNext(); cktNdx++) {
			Circuit ckt = cit.next();
			Cell rootCell = rootCells[cktNdx];
			forceWires[cktNdx] = new Wire[forceNames.size()];
			for (Iterator<NetObject> nit=ckt.getNetObjs(); nit.hasNext();) {
				Wire wire = (Wire) nit.next();
				Network net = wire.getNameProxy().getNet();
				if (net.getParent()==rootCell) {
					int ndx = indexOfMatchingName(forceNames, net);
					if (ndx!=-1) {
						forceWires[cktNdx][ndx] = wire;
					}
				}
			}
		}
		return forceWires;
	}
	
	private Set<Wire> forceMatches() {
		// Cells with no Parts and no Exports have no Wires
		if (globals.getWires()==null) return new HashSet<Wire>();;
		
		List<String> forceNames = getNamesOfWiresToForceMatch();
		Wire[][] forceWires = findWiresToForceMatch(forceNames);
		Map<Wire, Integer> wireToIndex = buildForceWireMap(forceNames, forceWires);
		new PartitionEquivRecord(globals.getWires(), wireToIndex, globals);
		return wireToIndex.keySet();
	}
	
	private ForceMatch(NccGlobals g) {globals = g;}
	
	/** Must be called before any other partitioning occurs. It assumes that
	 * all the Wires are in a single, root partition.
	 * @param g Ncc Globals
	 * @return set of Wires that were forced to match */
	public static Set<Wire> doYourJob(NccGlobals g) {
		ForceMatch fm = new ForceMatch(g);
		return fm.forceMatches();
	}
	
}
