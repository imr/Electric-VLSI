/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ExportChecker.java
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

package com.sun.electric.tool.ncc.processing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.jemNets.Port;
import com.sun.electric.tool.ncc.jemNets.Wire;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;


public class ExportChecker {
	/** a Port that doesn't match by name */
	private static class NoPortNameMatch {
		public final Port port;
		public final int circuit;
		public final int circuitNoMatchingPort;
		public NoPortNameMatch(Port port, int circuit, 
		                       int circuitNoMatchingPort) {
			this.port = port;
			this.circuit = circuit;
			this.circuitNoMatchingPort = circuitNoMatchingPort;
		}
	}
	private static class NoPortNameMatches {
		private List unmatched = new LinkedList();
		public void add(Port port, int circuit, int circuitNoMatchingPort) {
			unmatched.add(new NoPortNameMatch(port, circuit, 
			                                  circuitNoMatchingPort));
		}
		public NoPortNameMatch removeFirst() {
			Iterator it = unmatched.iterator();
			if (!it.hasNext()) return null;
			NoPortNameMatch u = (NoPortNameMatch) it.next();
			it.remove(); 
			return u;
		}
		/** prune reflexively related mismatches so we don't report
		 * "port1 matches port2" and "port2 matches port1" */
		public void removeMismatches(Port port1, int circuit1, 
		                             Port port2, int circuit2) {
			for (Iterator it=unmatched.iterator(); it.hasNext();) {
				NoPortNameMatch u = (NoPortNameMatch) it.next();
				if ((u.port==port1 && u.circuitNoMatchingPort==circuit2) ||
				    (u.port==port2 && u.circuitNoMatchingPort==circuit1)) { 
					it.remove();
				}
			}
		}
	}
	
	private NccGlobals globals;
	/** The 0th element is null. The ith element maps every port of the 0th
	 * design to the equivalent port of the ith design. */ 
	private HashMap[] equivPortMaps;
	/** List of ports that can't be matched by name. This list is used to 
	 * suggest possible repairs to the design. */ 
	private NoPortNameMatches noPortNameMatches = new NoPortNameMatches();

	private Port[][] getPortsForEachCell() {
		int numCells = globals.getNumNetlistsBeingCompared();
		Port[][] portsPerCell = new Port[numCells][];

		EquivRecord portRec = globals.getPorts();
		if (portRec==null) {
			// special case: every Cell being compared has zero Ports
			for (int i=0; i<numCells; i++)  portsPerCell[i]=new Port[0];
		} else {
			globals.error(!portRec.isLeaf(),"globals ports must hold circuits");
			int cellNdx = 0;
			for (Iterator it=portRec.getCircuits(); it.hasNext(); cellNdx++) { 
				Circuit ckt = (Circuit) it.next();
				portsPerCell[cellNdx] = new Port[ckt.numNetObjs()];
				int portNdx = 0;
				for (Iterator pit=ckt.getNetObjs(); pit.hasNext(); portNdx++) {
					portsPerCell[cellNdx][portNdx] = (Port) pit.next();
				}
			}
		}
		return portsPerCell;		
	}
	
	
	private HashMap mapFromExportNameToPort(Port[] ports) {
		HashMap map = new HashMap();
		for (int portNdx=0; portNdx<ports.length; portNdx++) {
			Port p = ports[portNdx];
			for (Iterator itN=p.getExportNames(); itN.hasNext();) {
				map.put(itN.next(), p);
			}
		}
		return map;
	}
	private void prln(String s) {System.out.println(s);}
	private void pr(String s) {System.out.print(s);}

	private void printOneToManyError(String design1, String exports1,
								     String design2, Set exports2) {
		prln("  A single network in: "+design1+" has Exports with names that "+
			 "match multiple Exports in: "+design2);
		prln("  However, the "+design2+
		     " Exports are attached to more than one network.");
		prln("    The "+design1+" Exports: "+exports1);
		for (Iterator it=exports2.iterator(); it.hasNext();) {
			Port p2 = (Port) it.next();
			prln("      matches the "+design2+" Exports: "+p2.exportNamesString());
		}
	}
	/** For each port, p1, in Circuit ckt1 make sure there is exactly one port 
	 * circuit ckt2 that shares any of p1's export names. Return a map from 
	 * ckt1 ports to matching ckt2 ports. If p1 has no matching port in ckt2
	 * then record the mismatch in noPortNameMatches. */  
	private boolean matchPortsByName(HashMap p1ToP2, Port[][] portsPerCell, 
								     String[] designNames, int ckt1, int ckt2) {
		boolean match = true;
		HashMap exportName2ToPort2 = mapFromExportNameToPort(portsPerCell[ckt2]);
		if (p1ToP2==null)  p1ToP2=new HashMap();
		Port[] ckt1Ports = portsPerCell[ckt1];
		for (int portNdx=0; portNdx<ckt1Ports.length; portNdx++) {
			Port p1 = ckt1Ports[portNdx];
			Set p2ports = new HashSet();
			for (Iterator itN=p1.getExportNames(); itN.hasNext();) {
				String exportNm1 = (String) itN.next();
				Port p = (Port) exportName2ToPort2.get(exportNm1);
				if (p!=null)  p2ports.add(p);
		    }

			if (p2ports.size()==0) {
				prln("  In "+designNames[ckt1]+
				     " the network with Exports: "+p1.exportNamesString()+
					 "    matches no Export with the same name in: "+
				     designNames[ckt2]);
				noPortNameMatches.add(p1, ckt1, ckt2);
				match = false;
			} else if (p2ports.size()==1){
				p1ToP2.put(p1, (Port)p2ports.iterator().next());
			} else {
				printOneToManyError(designNames[ckt1], p1.exportNamesString(),
						            designNames[ckt2], p2ports);
				match = false;
			}
		}
		return match;
	}

	private SubcircuitInfo getInfoForReferenceCell(Cell refCell, HierarchyInfo hierInfo) {	
		SubcircuitInfo refInfo = hierInfo.getSubcircuitInfo(refCell);
		
		// If the reference Cell has already been compared to another Cell
		// then return existing SubcircuitInfo
		if (refInfo!=null) return refInfo;
		
		// This is the first time the first Cell is used as a reference. Create
		// a SubcircuitInfo for it. This SubcircuitInfo will be a reference for
		// all the other Cells this one is compared with.
		Port[] refCktPorts = getFirstCktPorts();
		refInfo = new SubcircuitInfo(hierInfo.currentSubcircuitName(),
									 hierInfo.currentSubcircuitID(), 
									 refCktPorts);
		hierInfo.addSubcircuitInfo(refCell, refInfo);

		return refInfo;
	}

	private Port[] getFirstCktPorts() {
		Port[][] portsPerCell = getPortsForEachCell();
		return portsPerCell[0];
	}

	private Map mapExportNameToPortIndex(SubcircuitInfo refCellInfo, 
										 Map equivPortMap) {
		Map exportNameToPortIndex = new HashMap();
		Port[] firstCktPorts = getFirstCktPorts();
		for (int i=0; i<firstCktPorts.length; i++) {
			Port firstCktPort = firstCktPorts[i];
			String firstCktPortName = 
				(String) firstCktPort.getExportNames().next();
			int portNdx = refCellInfo.getPortIndex(firstCktPortName);
			Port equivPort = (Port) equivPortMap.get(firstCktPort);
			// if export names don't match then there may be no equivPort
			if (equivPort==null) continue; 
			for (Iterator it=equivPort.getExportNames(); it.hasNext();) {
				exportNameToPortIndex.put(it.next(), new Integer(portNdx));
			}
		}
		return exportNameToPortIndex;
	}

	private Circuit getNthCircuit(EquivRecord er, int nth) {
		LayoutLib.error(!er.isLeaf(), "only leaf EquivRecords have Circuits!");
		Iterator it = er.getCircuits();
		for (int i=0; i<nth; i++) {
			LayoutLib.error(!it.hasNext(), "EquivRec has no Circuit at index: "+nth);
			it.next(); 
		}
		LayoutLib.error(!it.hasNext(), "EquivRec has no Circuit at index: "+nth);
		return (Circuit) it.next();
	}

	/** Find a possible matching Wire or Port for lonely in the nth 
	 * circuit by examining lonely's Wire's equivalence record.   
	 * @return the matching Port or Wire. Return null if no unique matching 
	 * Port or Wire can be found */ 
	private Object findMatchingPortOrWire(Port lonely, int nth) {
		Wire lonelyWire = lonely.getWire();
		Circuit lonelyCkt = lonelyWire.getParent();
		if (lonelyCkt.numNetObjs()!=1) return null;
		EquivRecord equivRecord = lonelyCkt.getParent();
		Circuit nthCircuit = getNthCircuit(equivRecord, nth);
		if (nthCircuit.numNetObjs()!=1) return null;
		Wire wire = (Wire) nthCircuit.getNetObjs().next();
		Port port = wire.getPort();
		return port!=null ? (Object)port : (Object)wire;
	}

	private String getDescription(Object portOrWire) {
		if (portOrWire instanceof Wire) {
			return "network: "+((Wire)portOrWire).getName();
		} else {
			LayoutLib.error(!(portOrWire instanceof Port), "not a Wire?");
			return "network with Exports: "+
				   ((Port)portOrWire).exportNamesString();
		}
	}

	private void markPortForRenaming(Port p, NccCellAnnotations ann) {
		for (Iterator it=p.getExportNames(); it.hasNext();) {
			String name = (String) it.next();
			if (ann.renameExport(name)) {p.setToBeRenamed();  return;}
		}
	}
	
	private void markPortsForRenaming(Cell cell, Port[] ports) {
		NccCellAnnotations ann = NccCellAnnotations.getAnnotations(cell);
		if (ann==null) return;
		for (int portNdx=0; portNdx<ports.length; portNdx++) 
			markPortForRenaming(ports[portNdx], ann);
	}
    private void printNewPortNames(Port[] ports, Cell rootCell, int cktIndex) {
    	boolean printedHeader = false;
    	int otherCktIndex = cktIndex==0 ? 1 : 0;
    	for (int portNdx=0; portNdx<ports.length; portNdx++) {
    		Port p = ports[portNdx];
    		if (p.getToBeRenamed()) {
    			if (!printedHeader) {
    				prln("  Attempting to find better names for Exports in Cell: "+
    				      NccUtils.fullName(rootCell)); 
    				printedHeader = true;
    			}
				Object o = findMatchingPortOrWire(p, otherCktIndex);
    			prln("    "+ p.exportNamesString()+" -> "+
				     getDescription(o));
    		}
    	}
    }
    
    private void printNewNamesForPortsThatTheUserWantsUsToRename() {
    	Cell[] rootCells = globals.getRootCells();
    	Port[][] portsPerCell = getPortsForEachCell();
    	for (int cellNdx=0; cellNdx<portsPerCell.length; cellNdx++)  
    		printNewPortNames(portsPerCell[cellNdx], rootCells[cellNdx], 
    				          cellNdx);
    }

	/** For each port not matched by name, suggest a possible match */
    private void suggestMatchForPortsThatDontMatchByName() {
		String[] rootCellNames = globals.getRootCellNames();
		boolean printedHeader = false;
		for (NoPortNameMatch no=noPortNameMatches.removeFirst(); no!=null; 
		     no=noPortNameMatches.removeFirst()) {
			// skip Ports for which the user has already requested a new name
			if (no.port.getToBeRenamed()) continue;
			Object o = findMatchingPortOrWire(no.port, no.circuitNoMatchingPort);
			if (o==null) continue;
			if (!printedHeader) {
				printedHeader = true;
				prln("  The following list suggests possible matches for "+
					 "Exports that failed to match by name.");
			}
			prln("    in Cell "+rootCellNames[no.circuit]+
				 " the network with Exports: "+
				 no.port.exportNamesString()+
				 " might match in Cell "+rootCellNames[no.circuitNoMatchingPort]+
				 " the "+getDescription(o));
			if (o instanceof Port) {
				noPortNameMatches.removeMismatches(no.port, no.circuit, (Port)o, 
				                                   no.circuitNoMatchingPort);
			}
		}
		if (printedHeader) prln("");
    }

	// -------------------------- public methods ------------------------------    

	public ExportChecker(NccGlobals globals) {this.globals=globals;}
	
	public void markPortsForRenaming() {
		Cell[] rootCells = globals.getRootCells();
		Port[][] portsPerCell = getPortsForEachCell();
		for (int cellNdx=0; cellNdx<portsPerCell.length; cellNdx++) 
			markPortsForRenaming(rootCells[cellNdx], portsPerCell[cellNdx]);
	}
  
	/** match Exports by name. Run this before Gemini algorithm in order to 
	 * give user early warning about Export name inconsistencies. */
	public boolean matchByName() {
		int numCkts = globals.getNumNetlistsBeingCompared();
		String[] rootCellNames = globals.getRootCellNames();
		Port[][] portsPerCell = getPortsForEachCell();
		equivPortMaps = new HashMap[numCkts];
		boolean match=true;
		for (int i=1; i<numCkts; i++) {
			match &= matchPortsByName(null, portsPerCell, rootCellNames, i, 0);
			HashMap p0ToPi = new HashMap();
			match &= matchPortsByName(p0ToPi, portsPerCell, rootCellNames, 0, i);
			equivPortMaps[i] = p0ToPi;
		}
		return match;
	}

	/** Gather information that will allow hierarchical netlist comparison 
	 * at higher level to treat me as a subcircuit. */	
	public void saveInfoNeededToMakeMeASubcircuit(HierarchyInfo hierInfo) {
		if (hierInfo==null) return;

		Cell[] rootCells = globals.getRootCells();
		Cell refCell = globals.getRootCells()[0];

		SubcircuitInfo refCellInfo = getInfoForReferenceCell(refCell, hierInfo);		
		
		// It is reasonable for two different designs to share some
		// cells. It's also reasonable to compare the same schematic Cell 
		// with two different VarContexts. When this happens, don't create 
		// two SubcircuitInfo's for the same cell.
		Set doneCells = new HashSet();
		doneCells.add(refCell);
	
		for (int i=1; i<equivPortMaps.length; i++) {
			if (doneCells.contains(rootCells[i])) continue;
			doneCells.add(rootCells[i]);
			Map exportNameToPortIndex = 
				mapExportNameToPortIndex(refCellInfo, equivPortMaps[i]);
			SubcircuitInfo subInf = 
				new SubcircuitInfo(refCellInfo, exportNameToPortIndex);
			hierInfo.addSubcircuitInfo(rootCells[i], subInf);
		}
	}

	/** Check that Exports with matching names are on equivalent nets.
	 * @return true if equivalent. */ 
    public boolean ensureExportsWithMatchingNamesAreOnEquivalentNets() {
    	boolean match=true;
    	
		String[] rootCellNames = globals.getRootCellNames();
    	for (int i=1; i<equivPortMaps.length; i++) {
    		HashMap portToPort = equivPortMaps[i];
    		for (Iterator it=portToPort.keySet().iterator(); it.hasNext();) {
    			Port p0 = (Port) it.next();
    			Port pn = (Port) portToPort.get(p0);
    			// skip Ports that the user wants us to rename
    			if (p0.getToBeRenamed() || pn.getToBeRenamed()) continue;
    			Wire w0 = p0.getWire();
    			Wire wn = pn.getWire();
				EquivRecord er0 = w0.getParent().getParent();
				EquivRecord ern = wn.getParent().getParent();
				if (er0!=ern) {
					prln("  Exports that match by name aren't on equivalent"+
					     " networks");
					prln("    Cell1: "+rootCellNames[0]);
					prln("    Exports1: "+p0.exportNamesString());
					prln("    Cell2: "+rootCellNames[i]);
					prln("    Exports2: "+pn.exportNamesString());
					Object portOrWire = findMatchingPortOrWire(p0, i);
					if (portOrWire!=null) 
						prln("    However the Cell1 network appears to match Cell2's: "+
						     getDescription(portOrWire));
					prln("");
					match = false;
				}
    		}
    	}
    	return match;
    }
    
    public void suggestPortMatchesBasedOnTopology() {
    	printNewNamesForPortsThatTheUserWantsUsToRename();
    	suggestMatchForPortsThatDontMatchByName();
    }
}
