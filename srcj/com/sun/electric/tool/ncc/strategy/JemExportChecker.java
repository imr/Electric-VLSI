/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemStratCheckExports.java
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

package com.sun.electric.tool.ncc.strategy;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;

import com.sun.electric.tool.ncc.basicA.Messenger; 
import com.sun.electric.tool.ncc.NccGlobals; 
import com.sun.electric.tool.ncc.NccOptions; 
import com.sun.electric.tool.ncc.trees.JemEquivRecord;
import com.sun.electric.tool.ncc.trees.JemCircuit;
import com.sun.electric.tool.ncc.jemNets.Port;
import com.sun.electric.tool.ncc.jemNets.Wire;

/**
 * This isn't really a JemStrategy.
 */
public class JemExportChecker {
	private NccGlobals globals;
	private boolean match = true;
	/** The 0th element is null. The ith element maps every port of the 0th
	 * design to the equivalent port of the ith design. */ 
	private HashMap[] equivPortMaps;
	 	
	private JemCircuit[] getJemCircuitsHoldingPorts() {
		JemEquivRecord ports = globals.getPorts();
		globals.error(!ports.isLeaf(), "globals ports must hold circuits");

		int numCkts = globals.getNumNetlistsBeingCompared();
		JemCircuit[] portCkts = new JemCircuit[numCkts];
		int i = 0;
		for (Iterator it=ports.getCircuits(); it.hasNext();) 
			portCkts[i++] = (JemCircuit) it.next();
		return portCkts;		
	}
	private HashMap mapFromExportNameToPort(JemCircuit ckt) {
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
	private HashMap matchPortsByName(JemCircuit ports1, String designName1,
									 JemCircuit ports2, String designName2) {
		HashMap exportName2ToPort2 = mapFromExportNameToPort(ports2);
		HashMap p1ToP2=new HashMap();
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
		return p1ToP2;
	}
	private void matchExportsByName() {
		int numCkts = globals.getNumNetlistsBeingCompared();
		String[] rootCellNames = globals.getRootCellNames();
		JemCircuit[] portCkts = getJemCircuitsHoldingPorts();
		equivPortMaps = new HashMap[numCkts];
		for (int i=1; i<numCkts; i++) {
			HashMap m;
			m = matchPortsByName(portCkts[i], rootCellNames[i], 
							     portCkts[0], rootCellNames[0]);
			m = matchPortsByName(portCkts[0], rootCellNames[0], 
								 portCkts[i], rootCellNames[i]);
			equivPortMaps[i] = m;
		}
	}
	
	/** Constructor matches Exports by name. Run this before Gemini
	 * algorithm in order to give user early warning about 
	 * Export name inconsistencies. */
	public JemExportChecker(NccGlobals globals) {
		this.globals=globals;
		NccOptions options = globals.getOptions();
		boolean verboseSave = options.verbose;
		options.verbose = true;
		matchExportsByName();
		options.verbose = verboseSave;
	}

	/** Check that Exports with matching names are on equivalent nets.
	 * @return true if equivalent. */ 
    public boolean ensureExportsWithMatchingNamesAreOnEquivalentNets() {
    	NccOptions options = globals.getOptions();
    	boolean verboseSave = options.verbose;
    	options.verbose = true;
    	
		String[] rootCellNames = globals.getRootCellNames();
    	for (int i=1; i<equivPortMaps.length; i++) {
    		HashMap portToPort = equivPortMaps[i];
    		for (Iterator it=portToPort.keySet().iterator(); it.hasNext();) {
    			Port p0 = (Port) it.next();
    			Port pn = (Port) portToPort.get(p0);
				JemEquivRecord er0 = p0.getParent().getParent();
				JemEquivRecord ern = pn.getParent().getParent();
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
