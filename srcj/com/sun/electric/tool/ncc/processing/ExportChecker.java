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

import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.ncc.basic.Messenger; 
import com.sun.electric.tool.ncc.NccGlobals; 
import com.sun.electric.tool.ncc.NccOptions; 
import com.sun.electric.tool.ncc.trees.EquivRecord;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.jemNets.Port;
import com.sun.electric.tool.ncc.jemNets.Wire;

public class ExportChecker {
	private NccGlobals globals;
	/** The 0th element is null. The ith element maps every port of the 0th
	 * design to the equivalent port of the ith design. */ 
	private HashMap[] equivPortMaps;
	 	
	private Circuit[] getCircuitsHoldingPorts() {
		EquivRecord ports = globals.getPorts();
		globals.error(!ports.isLeaf(), "globals ports must hold circuits");

		int numCkts = globals.getNumNetlistsBeingCompared();
		Circuit[] portCkts = new Circuit[numCkts];
		int i = 0;
		for (Iterator it=ports.getCircuits(); it.hasNext(); i++) 
			portCkts[i] = (Circuit) it.next();
		return portCkts;		
	}
	private HashMap mapFromExportNameToPort(Circuit ckt) {
		HashMap map = new HashMap();
		for (Iterator it=ckt.getNetObjs(); it.hasNext();) {
			Port p = (Port) it.next();
			for (Iterator itN=p.getExportNames(); itN.hasNext();) {
				map.put(itN.next(), p);
			}
		}
		return map;
	}
	private void prln(String s) {globals.println(s);}
	private void pr(String s) {globals.print(s);}
	private void printOneToManyError(String design1, String exports1,
								     String design2, String exports2a,
								     String exports2b) {
		prln("A single network in: "+design1+
		   " has Exports with names that ");
		pr("match multiple Exports in: "+design2);
		prln("However, the "+design2+
		   " Exports are attached to more than one network.");
		prln("    The "+design1+" Exports are: "+exports1);
		prln("    The 1st set of "+design2+" Exports are: "+exports2a);
		prln("    The 2nd set of "+design2+" Exports are: "+exports2b);
	}
	/** For each port, p1, in ports1 make sure there is exactly one port in ports2 
	 * that shares any of p1's export names. Return a map from ports1 ports
	 * to matching ports2 ports */  
	private boolean matchPortsByName(HashMap p1ToP2,
									 Circuit ports1, String designName1,
									 Circuit ports2, String designName2) {
		boolean match = true;
		HashMap exportName2ToPort2 = mapFromExportNameToPort(ports2);
		if (p1ToP2==null)  p1ToP2=new HashMap();
		for (Iterator it=ports1.getNetObjs(); it.hasNext();) {
			Port p1 = (Port) it.next();
			Port p2 = null;
			for (Iterator itN=p1.getExportNames(); itN.hasNext();) {
				String exportNm1 = (String) itN.next();
				Port p = (Port) exportName2ToPort2.get(exportNm1);
				if (p==null) {
					// do nothing
				} else if (p2==null) {
					p2 = p;
				} else if (p!=p2) {
					printOneToManyError(designName1, p1.exportNamesString(),
					                    designName2, p.exportNamesString(),
					                    p2.exportNamesString());
					match = false;
				}
			}
			if (p2==null) {
				pr("In "+designName1+
				   " the network with Exports: "+p1.exportNamesString());
				prln("    matches no Export with the same name in: "+designName2);
				match = false;
			} else {
				p1ToP2.put(p1, p2);
			}
		}
		return match;
	}
	private boolean matchExportsByName() {
		int numCkts = globals.getNumNetlistsBeingCompared();
		String[] rootCellNames = globals.getRootCellNames();
		Circuit[] portCkts = getCircuitsHoldingPorts();
		equivPortMaps = new HashMap[numCkts];
		boolean match=true;
		for (int i=1; i<numCkts; i++) {
			match &= matchPortsByName(null, portCkts[i], rootCellNames[i], 
							                portCkts[0], rootCellNames[0]);
			HashMap p0ToPi = new HashMap();
			match &= matchPortsByName(p0ToPi, portCkts[0], rootCellNames[0], 
								              portCkts[i], rootCellNames[i]);
			equivPortMaps[i] = p0ToPi;
		}
		return match;
	}

	private SubcircuitInfo getInfoForReferenceCell(HierarchyInfo hierInfo) {	
		Cell refCell = globals.getRootCells()[0];
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
		Circuit[] portCkts = getCircuitsHoldingPorts();
		Circuit firstCkt = portCkts[0];
		Port[] ports = new Port[firstCkt.numNetObjs()];
		int i=0;
		for (Iterator it=firstCkt.getNetObjs(); it.hasNext(); i++) {
			ports[i] = (Port) it.next();
		}
		return ports;
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

	public ExportChecker(NccGlobals globals) {this.globals=globals;}
	
	/** match Exports by name. Run this before Gemini
	 * algorithm in order to give user early warning about 
	 * Export name inconsistencies. */
	public boolean matchByName() {
		NccOptions options = globals.getOptions();
		boolean verboseSave = options.verbose;
		options.verbose = true;
		boolean match = matchExportsByName();
		options.verbose = verboseSave;
		return match;
	}

	/** Gather information that will allow hierarchical netlist comparison 
	 * at higher level to treat me as a subcircuit. */	
	public void saveInfoNeededToMakeMeASubcircuit(HierarchyInfo hierInfo) {
		if (hierInfo==null) return;

		Cell[] rootCells = globals.getRootCells();

		SubcircuitInfo refCellInfo = getInfoForReferenceCell(hierInfo);		
		
		for (int i=1; i<equivPortMaps.length; i++) {
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
    	NccOptions options = globals.getOptions();
    	boolean verboseSave = options.verbose;
    	options.verbose = true;
    	boolean match=true;
    	
		String[] rootCellNames = globals.getRootCellNames();
    	for (int i=1; i<equivPortMaps.length; i++) {
    		HashMap portToPort = equivPortMaps[i];
    		for (Iterator it=portToPort.keySet().iterator(); it.hasNext();) {
    			Port p0 = (Port) it.next();
    			Port pn = (Port) portToPort.get(p0);
				EquivRecord er0 = p0.getParent().getParent();
				EquivRecord ern = pn.getParent().getParent();
				if (er0!=ern) {
					prln("Exports that match by name aren't on equivalent"+
					   " networks\n"+
					   "  Cell1: "+rootCellNames[0]+"\n"+
					   "  Exports1: "+p0.exportNamesString()+"\n"+
					   "  Cell2: "+rootCellNames[i]+"\n"+
					   "  Exports2: "+pn.exportNamesString()+"\n");
					match = false;
				}
    		}
    	}
    	
    	options.verbose = verboseSave;
    	return match;
    }
}
