/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SchemNamesToLay.java
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
package com.sun.electric.tool.ncc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NetNameProxy;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NodableNameProxy;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.result.NccResult;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.ncc.result.equivalence.Equivalence;
import com.sun.electric.tool.user.User;

/** Copy schematic names to layout */
public class SchemNamesToLay {
    static final long serialVersionUID = 0;
    private String header;

	private int numArcRenames, numNodeRenames, 
    	numArcManRenames, numNodeManRenames, numNameConflicts;
	
	public static class RenameResult {
		public final int numArcRenames, numNodeRenames, 
		    numArcManRenames, numNodeManRenames, numNameConflicts;
		RenameResult(int numArcRenames, int numNodeRenames,
				 	 int numArcManRenames, int numNodeManRenames,
				 	 int numNameConflicts) {
			this.numArcRenames = numArcRenames;
			this.numNodeRenames = numNodeRenames;
			this.numArcManRenames = numArcManRenames;
			this.numNodeManRenames = numNodeManRenames;
			this.numNameConflicts = numNameConflicts;
		}
	}
    public static class RenameJob extends Job {
        static final long serialVersionUID = 0;

        // these fields are passed to server 
        private final NccResults results;

        public boolean doIt() throws JobException {
        	SchemNamesToLay.copyNames(results);
        	return true;
        }

        public void terminateOK() {
        	// We've changed the netlist so the old NetEquivalence table is invalid
        	NccJob.invalidateLastNccResult();
        }
        
        public RenameJob(NccResults r) {
        	super("SchemNamesToLayJob", User.getUserTool(), Job.Type.CHANGE, 
          		  null, null, Job.Priority.USER);
          	results = r;
          	startJob();
        }

        public RenameJob() {
        	this(NccJob.getLastNccResults());
        }
    }
    
    /** "For" class is an experiment. It allows me to use the compact "for" 
	 * syntax when I have an iterators rather than an Iterable. */
	private static class For<T> implements Iterable<T> {
    	Iterator<T> it;
    	For(Iterator<T> it) {this.it=it;}
    	public Iterator<T> iterator() {return it;}
    }
	
    // ---------------------- Private Methods -----------------------------
	/** print header if header hasn't already been printed */
	private void printHeader() {
		if (header!=null) prln(header);
		header = null;
	}
	
	private boolean isAutoGenName(String nm) {
		return nm.indexOf('@')!=-1;
	}
	
	private void prln(String s) {System.out.println(s);}


    private void copySchematicNamesToLayout(NccResult result) {
        Equivalence equivs = result.getEquivalence();
        Cell [] rootCells = result.getRootCells();

        // get layout cell
        if (rootCells.length != 2) return;
        int schNdx;
        
        if (rootCells[0].getView()==View.SCHEMATIC && 
        	rootCells[1].getView()==View.LAYOUT) {
        	schNdx = 0;
        } else if (rootCells[0].getView()==View.LAYOUT && 
        	       rootCells[1].getView()==View.SCHEMATIC) {
        	schNdx = 1;
        } else {
        	return;
        }
        int layNdx = schNdx==0 ? 1 : 0;
        Cell schCell = rootCells[schNdx];
        Cell layCell = rootCells[layNdx];
        
        VarContext schContext = result.getRootContexts()[schNdx];
        copySchematicNamesToLayout(schCell, layCell, schContext, equivs);
    }
    
    private Map<String, Network> namesToNetworks(Cell c) {
    	Map<String, Network> nmsToNets = new HashMap<String, Network>();
    	Netlist nets = c.getNetlist(false);
    	for (Network net : new For<Network>(nets.getNetworks())) {
    		for (String nm : new For<String>(net.getNames())) {
    			nmsToNets.put(nm, net);
    		}
    	}
    	return nmsToNets;
    }

    /** @return true if schematic net's preferred name conflicts with some
     * non-equivalent layout net */
    private boolean reportNetNameConflicts(Network schNet, Network layNet,
    		                               Map<String,Network> layNmsToNets) {
    	String prefSchNm = schNet.getName();
    	LayoutLib.error(isAutoGenName(prefSchNm), "missing preferred name");
    	boolean prefNameConflict = false;
    	for (String schNm : new For<String>(schNet.getNames())) {
    		if (isAutoGenName(schNm))  continue;
        	Network layNetWithSameName = layNmsToNets.get(schNm);
    		if (layNetWithSameName==null)  continue;
    		if (layNetWithSameName!=layNet) {
    			prefNameConflict |= schNm.equals(prefSchNm);
    			printHeader();
    			prln("    Schematic and layout each have a Network named: "+schNm+
				     " but those networks don't match topologically.");
    			numNameConflicts++;
    		}
    	}
    	return prefNameConflict;
    }
    
    private ArcInst getLongestArc(Network net) {
    	ArcInst longest = null;
    	for (ArcInst ai : new For<ArcInst>(net.getArcs())) {
    		if (longest==null) longest = ai;
    		else if (ai.getLength()>longest.getLength())  longest = ai;
    	}
    	return longest;
    }
    
    private static class ArcAndName {
    	public final ArcInst arc;
    	public final String name;
    	ArcAndName(ArcInst a, String n) {arc=a; name=n;}
    }
    
//    public static void dumpNetlist(Netlist netlist) {
//    	System.out.println("Begin dumping networks for Cell");
//    	for (Iterator<Network> nIt=netlist.getNetworks(); nIt.hasNext();) {
//    		Network net = nIt.next();
//    		System.out.print("    Network names: ");
//    		for (Iterator<String> nmIt=net.getNames(); nmIt.hasNext();) {
//    			System.out.print(nmIt.next()+" ");
//    		}
//    		System.out.println();
//    		
//    	}
//    	System.out.println("End dumping networks for Cell");
//    }
    
    private List<ArcAndName> buildArcNameList(Cell schCell, Cell layCell,
            						          VarContext schCtxt,
                                              Equivalence equivs) {
    	Map<String, Network> layNmsToNets = namesToNetworks(layCell);

    	List<ArcAndName> arcAndNms = new ArrayList<ArcAndName>();

    	Netlist nets = schCell.getNetlist(false);
    	
    	for (Network schNet : new For<Network>(nets.getNetworks())) {
    		NetNameProxy layProx = equivs.findEquivalentNet(schCtxt, schNet);

    		// skip if layout has no equivalent net (e.g.: net in center of 
    		// NMOS_2STACK)
    		if (layProx==null)  continue;

    		Network layNet = layProx.getNet();

    		// skip if layout net isn't in top level Cell 
    		if (layNet.getParent()!=layCell) continue;  

    		String schName = schNet.getName();

    		// skip if schematic name is automatically generated
    		if (isAutoGenName(schName)) continue;
    		
    		// Skip if layout net gets its name from an Export because
    		// designer has already chosen a useful name.
    		if (layNet.isExported()) continue;
    		
    		// Tricky: I must not check for conflicts in exported names
    		// because some nets connected by exportsConnectedByParent
    		// will have conflicts
    		boolean prefNameConflict = 
    			reportNetNameConflicts(schNet, layNet, layNmsToNets);
    		
    		// If schematic's preferred name is already on some other
    		// layout net then designer needs to fix.
    		if (prefNameConflict) continue;

    		String layName = layNet.getName();
    		
    		// If the schematic and layout nets already have the
    		// same preferred name then we're all set
    		if (layName.equals(schName)) continue;

    		if (!isAutoGenName(layName)) {
    			printHeader();
    			prln("    The layout Network named: "+layName+
    				 " should, instead, be named: "+schName);
    			numArcManRenames++;
    			continue;
    		}
    		
    		ArcInst ai = getLongestArc(layNet);
    		
    		// If no arcs then continue
    		if (ai==null) continue;

    		printHeader();
    		prln("    Renaming arc from: "+ai.getName()+" to: "+schName);
    		arcAndNms.add(new ArcAndName(ai, schName));
    		numArcRenames++;
    	}
		return arcAndNms;
    }
    
    private void renameArcs(List<ArcAndName> arcAndNms) {
    	for (ArcAndName an : arcAndNms)  an.arc.setName(an.name);
    }

    /** @return true if schematic Nodable's name conflicts with some
     * non-equivalent layout Nodable */
    private boolean reportNodeNameConflicts(Nodable schNode, NodeInst layNode,
    		                                Map<String,NodeInst> layNmsToNodes) {
    	String schNm = schNode.getName();
    	NodeInst ni = layNmsToNodes.get(schNm);
    	if (ni==null) return false;

    	if (ni!=layNode) {
    		printHeader();
    		prln("    Schematic and layout each have a Nodable named: "+schNm+
				 " but those Nodables don't match topologically.");
    		numNameConflicts++;
    		return true;
    	}
    	return false;
    }

    private Map<String, NodeInst> namesToNodes(Cell c) {
    	Map<String, NodeInst> nmsToNodes = new HashMap<String, NodeInst>();
    	for (Nodable no : new For<Nodable>(c.getNodables())) {
    		if (no instanceof NodeInst) {
    			nmsToNodes.put(no.getName(), (NodeInst)no);
    		}
    	}
    	return nmsToNodes;
    }

    private static class NodeAndName {
    	public final NodeInst node;
    	public final String name;
    	NodeAndName(NodeInst no, String na) {node=no; name=na;}
    }
    
    private List<NodeAndName> buildNodeNameList(Cell schCell, Cell layCell,
	          								    VarContext schCtxt,
	          								    Equivalence equivs) {
    	Map<String, NodeInst> layNmsToNodes = namesToNodes(layCell);
    	
    	List<NodeAndName> nodeAndNms = new ArrayList<NodeAndName>();
    	
    	for (Nodable schNode : new For<Nodable>(schCell.getNodables())) {
    		NodableNameProxy layProx = equivs.findEquivalentNode(schCtxt, schNode);
    		
    		// skip if layout has no equivalent nodable (e.g.: MOS deleted 
    		// because it is in parallel to another MOS 
    		if (layProx==null)  continue;
    		
    		Nodable layNode = layProx.getNodable();
    		
    		// skip if layout Nodable isn't in top level Cell 
    		if (layNode.getParent()!=layCell) continue;
    		
    		// skip if layout Nodable isn't a NodeInst. (This can happen
    		// because Electric allows mixing of schematics and layout elements
    		// in the same Cell.
    		if (!(layNode instanceof NodeInst)) continue;
    		
    		NodeInst layNodeInst = (NodeInst) layNode;
    		
    		String schName = schNode.getName();
    		
    		// skip if schematic name is automatically generated
    		if (isAutoGenName(schName)) continue;
    		
    		//If schematic Nodable's name is already on some other
    		// layout NodeInst then designer needs to fix.
    		boolean nameConflict = 
    			reportNodeNameConflicts(schNode, layNodeInst, layNmsToNodes);
    		if (nameConflict) continue;
    		
    		String layName = layNodeInst.getName();
    		
    		// If the schematic and layout nodes already have the
    		// same name then we're all set
    		if (layName.equals(schName)) continue;
    		
    		if (!isAutoGenName(layName)) {
    			printHeader();
    			prln("    The layout NodeInst named: "+layName+
    				 " should, instead, be named: "+schName);
    			numNodeManRenames++;
    			continue;
    		}
    		
    		printHeader();
    		prln("    Renaming NodeInst from: "+layNodeInst.getName()+" to: "+
    			 schName);
    		nodeAndNms.add(new NodeAndName(layNodeInst, schName));
    		numNodeRenames++;
    	}
    	return nodeAndNms;
    }
    
    private void renameNodes(List<NodeAndName> nodeAndNms) {
    	for (NodeAndName an : nodeAndNms)  an.node.setName(an.name);
    }

    // Tricky: renaming ArcInsts invalidates the Networks. Therefore first
    // find all Arcs before renaming any of them. I don't think NodeInsts
    // have this problem but I do it the same way as Arcs anyway.
    private void copySchematicNamesToLayout(Cell schCell, Cell layCell,
                                            VarContext schCtxt,
                                            Equivalence equivs) {
    	header = "  Copy from: "+schCell.describe(false)+" to "+
		         layCell.describe(false);

    	if (!schCell.isSchematic()) {
    		printHeader();
    		prln("    First Cell isn't schematic: "+schCell.describe(false));
    		return;
    	}
    	if (layCell.getView()!=View.LAYOUT) {
    		printHeader();
    		prln("    Second Cell isn't layout: "+layCell.describe(false));
    		return;
    	}
    		
    	List<ArcAndName> arcAndNms = 
    		buildArcNameList(schCell, layCell, schCtxt, equivs);
    	List<NodeAndName> nodeAndNms =
    		buildNodeNameList(schCell, layCell, schCtxt, equivs);
    	renameArcs(arcAndNms);
    	renameNodes(nodeAndNms);
    }
    
    // Constructor does all the work
    private SchemNamesToLay(NccResults results) {
    	prln("Begin copying Network and Instance names from Schematic to Layout");
    	if (results==null) {
    		prln("  No saved NCC results. Please run NCC first.");
    		return;
    	}
    	
    	for (NccResult r : results) {
    		if (r.match())  copySchematicNamesToLayout(r);
    	}
    	prln("Done");
    }
	RenameResult getResult() {
		return new RenameResult(numArcRenames, numNodeRenames, numArcManRenames, 
				                numNodeManRenames, numNameConflicts);
	}
    
    // --------------------------- public method -----------------------------
    public static RenameResult copyNames(NccResults r) {
    	SchemNamesToLay sntl = new SchemNamesToLay(r);
    	return sntl.getResult();
    }
}
