/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AllSchemNamesToLay.java
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
import java.util.Set;

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
import com.sun.electric.tool.ncc.result.NccResult;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.ncc.result.equivalence.Equivalence;
import com.sun.electric.tool.user.User;

/** Copy ALL schematic names, both default and user assigned, to layout.
	 Tricky: renaming layout ArcInsts and NodeInsts invalidates the layout 
	 Networks thereby rendering the NCC equivalence tables useless. Therefore 
	 first extract all information from the NCC equivalence tables and then
	 perform all the renaming.
	 
	 Rename nodes:
        * Scan layout NodeInsts to build a Map: NodeInst name -> NodeInst.
        * Scan schematic to find all Nodables that 1) have an equivalent 
          NodeInst in the layout and 2) the layout equivalent doesn't have
          the same name. Make a list of (schematic_nodable_name, layout_nodeinst)
          pairs. Let this be list A.
        * Assign new autoGen names to all layout NodeInsts that conflict 
          with Nodable names in list A.
        * Perform the renames specified by list A.

     Rename arcs:
        * Scan layout ArcInsts to build a Map from name -> ArcInst
        * Scan schematic to find all Networks that 1) have an equivalent in 
          the layout and 2) the layout equivalent doesn't have the same name. 
          Make a list of (schematic_network_name, layout_ArcInsts) pairs. 
          Let this be list B.
        * Assign new autoGen names to all layout ArcInsts that conflict 
          with schematic Network names in list B. Choose autoGen names to 
          be greater than the largest schematic autoGen name in list B.
        * Assign new autoGen names to all layout ArcInsts in list B. Choose 
          autoGen names greater than the largest schematic autoGen name in 
          list B.
        * Perform the renames specified by list B. Only rename one layout 
          ArcInst per pair.
	 */
public class AllSchemNamesToLay {
    static final long serialVersionUID = 0;
    //------------------------------- private types ---------------------------
    private static class SchemNodaNm_LayNodeInst {
    	final String schemNodableName;
    	final NodeInst equivLayoutNodeInst;
    	SchemNodaNm_LayNodeInst(String schemNodableName,
    			                NodeInst equivLayoutNodeInst) {
    		this.schemNodableName = schemNodableName;
    		this.equivLayoutNodeInst = equivLayoutNodeInst;
    	}
    }
    
    // All the information I need to rename layout NodeInsts
    private static class NodeRenameInfo {
    	// layout NodeInst name -> layout NodeInst
    	final Map<String, NodeInst> nameToLayNodeInst;
    	final List<SchemNodaNm_LayNodeInst> toRename;
    	NodeRenameInfo(Map<String, NodeInst> nameToNodeInst,
    			       List<SchemNodaNm_LayNodeInst> toRename) {
    		this.nameToLayNodeInst = nameToNodeInst;
    		this.toRename = toRename;
    	}
    }
    
    private static class SchemNetNm_LayArcInsts {
    	final String schemNetworkName;
    	final List<ArcInst> equivLayoutArcInsts;
    	SchemNetNm_LayArcInsts(String schemNetworkName, 
    			               List<ArcInst> equivLayoutArcInsts) {
    		this.schemNetworkName = schemNetworkName;
    		this.equivLayoutArcInsts = equivLayoutArcInsts;
    	}
    }

    // All the information I need to rename layout ArcInsts
    private static class ArcRenameInfo {
    	// layout ArcInst name -> layout ArcInst
    	final Map<String, ArcInst> nameToLayArcInst;
    	final List<SchemNetNm_LayArcInsts> toRename; 
    	final int maxSchemAutoNameNumb;
    	ArcRenameInfo(Map<String, ArcInst> nameToArcInst,
    			      List<SchemNetNm_LayArcInsts> toRename,
    			      int maxSchemAutoNameNumb) {
    		this.nameToLayArcInst = nameToArcInst;
    		this.toRename = toRename;
    		this.maxSchemAutoNameNumb = maxSchemAutoNameNumb;
    	}
    }
    // Scan usedNames for all the names of the form: <prefix>@<number>.
    // Then generate names of the form <prefix>@<number> that haven't
    // been used. Always allocate numbers that are larger than the
    // largest number encountered in usedNames.
    private static class NameGenerator {
    	private int maxNumb;
    	private String prefix;
    	NameGenerator(String prefix, int startNumb, Set<String> usedNames) {
    		maxNumb = startNumb;
    		this.prefix = prefix;
    		for (String nm : usedNames) {
    			if (nm.startsWith(prefix+"@")) {
    				int numb = getAutoGenNumber(nm);
    				maxNumb = Math.max(maxNumb, numb);
    			}
    		}
    	}
    	String nextName() {return prefix+"@"+(++maxNumb);}
    }
    
    //------------------------------- public types ----------------------------
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
        	AllSchemNamesToLay.copyNames(results);
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
	
    //---------------------------- private data -------------------------------
	private static final boolean DEBUG = true;
	private String header;

	private int numArcRenames, numNodeRenames, 
    	numArcManRenames, numNodeManRenames, numNameConflicts;
	
    // -------------------------- Private Methods -----------------------------
	/** print header if header hasn't already been printed */
	private void printHeader() {
		if (header!=null) prln(header);
		header = null;
	}
	
	private boolean isAutoGenName(String nm) {
		return nm.indexOf('@')!=-1;
	}
	
	// If name is not auto generated return -1
	// else return number after the '@' character.
	private static int getAutoGenNumber(String nm) {
		int ndx = nm.indexOf('@');
		if (ndx==-1) {return -1;}
		StringBuffer sb = new StringBuffer();
		for (int i=ndx+1; i<nm.length(); i++) {
			char c = nm.charAt(i);
			if (!Character.isDigit(c)) break; 
			sb.append(c);	
		}
		if (sb.length()==0) return -1;
		return Integer.valueOf(sb.toString());
	}
	
	private void prln(String s) {System.out.println(s);}

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
    // build Map from layout Cell's ArcInst's name to ArcInst
    private Map<String, ArcInst> buildNameToLayArcInst(Cell layCell) {
    	Map<String, ArcInst> nmToArcInst = new HashMap<String, ArcInst>();
    	for (Iterator<ArcInst> aiIt=layCell.getArcs(); aiIt.hasNext();) {
    		ArcInst ai = aiIt.next();
    		nmToArcInst.put(ai.getName(), ai);
    	}
    	return nmToArcInst;
    }
    private Map<String, Network> buildNameToLayNetwork(Cell layCell) {
    	Map<String, Network> nmToLayNet = new HashMap<String, Network>();
    	Netlist nets = layCell.getNetlist(false);
    	for (Iterator<Network> netIt=nets.getNetworks(); netIt.hasNext();) {
    		Network net = netIt.next();
    		for (Iterator<String> nmIt=net.getNames(); nmIt.hasNext();) {
    			String nm = nmIt.next();
//    			// debug
//    			if (nm.equals("reset_bitlines")) {
//    				printHeader();
//    				System.out.println("layout cell has reset_bitlines");
//    			}
        		nmToLayNet.put(nm, net);
    		}
    	}
    	return nmToLayNet;
    }
    
    private List<ArcInst> getArcInsts(Network layNet) {
    	List<ArcInst> arcs = new ArrayList<ArcInst>();
    	for (Iterator<ArcInst> aiIt=layNet.getArcs(); aiIt.hasNext();) {
    		arcs.add(aiIt.next());
    	}
    	return arcs;
    }
    
    // Complain if schematic network name is manually assigned and occurs on 
    // layout network that isn't equivalent
    private boolean isSchNameOnNonEquivLayNet(String schNetNm, Network layNet, 
    		                                  Map<String, Network> nmToLayNet) {
    	if (isAutoGenName(schNetNm)) return false;
    	Network layNetWithSameNm = nmToLayNet.get(schNetNm);
    	if (layNetWithSameNm!=null && layNetWithSameNm!=layNet) {
			printHeader();
			prln("    Can't copy schematic network name: "+schNetNm+
				 " to layout because some non-equivalent layout network "+
				 "already uses that name");
			numNameConflicts++;
			return true; 
    	}
    	return false;
    }
    
    // Complain if layout network has manually assigned name that is different 
    // from the schematic network name.
    private boolean isEquivLayNetDesignerNamed(String schNetNm, Network layNet) {
    	for (Iterator<String> nmIt=layNet.getNames(); nmIt.hasNext();) {
    		String nm = nmIt.next();
    		if (!isAutoGenName(nm) && !nm.equals(schNetNm)) {
    			printHeader();
    			prln("    Can't copy schematic network name: "+schNetNm+
    				 " to layout because equivalent layout network already "+
    				 "has a name assigned by the designer: "+nm);
    			numArcManRenames++;
    			return true;
    		}
    	}
    	return false;
    }
    // Names of the form: net@161[0] are occur in schematics but are not
    // permitted in layout.
    private boolean isLegalLayNetName(String schNetNm) {
    	if (schNetNm.indexOf('@')!=-1 && 
    		(schNetNm.indexOf('[')!=-1 || schNetNm.indexOf(']')!=-1)) {
    		printHeader();
    		prln("    Can't copy schematic network name: "+schNetNm+
    			 " to layout because name is not a legal name for layout arcs");
    		return false;
    	}
    	return true;
    }
    
    private ArcRenameInfo buildArcRenameInfo(Cell schCell, Cell layCell,
            						         VarContext schCtxt,
                                             Equivalence equivs) {
    	Map<String, ArcInst> nmToLayArcInst = buildNameToLayArcInst(layCell);
    	Map<String, Network> nmToLayNet = buildNameToLayNetwork(layCell);

    	List<SchemNetNm_LayArcInsts> schNmLayArcInsts = new ArrayList<SchemNetNm_LayArcInsts>();

    	Netlist nets = schCell.getNetlist(false);
    	
    	int maxSchemAutoGen = 0;
    	
    	for (Network schNet : new For<Network>(nets.getNetworks())) {
    		NetNameProxy layProx = equivs.findEquivalentNet(schCtxt, schNet);

    		// skip if layout has no equivalent net (e.g.: net in center of 
    		// NMOS_2STACK)
    		if (layProx==null)  continue;

    		Network layNet = layProx.getNet();

    		// skip if layout net isn't in top level Cell 
    		if (layNet.getParent()!=layCell) continue;  

    		// Skip if layout net gets its name from an Export because
    		// designer has already chosen a useful name.
    		if (layNet.isExported()) continue;
    		
    		String layNetNm = layNet.getName();
    		String schNetNm = schNet.getName();

    		// If the schematic and layout nets already have the
    		// same preferred name then we're all set
    		if (layNetNm.equals(schNetNm)) continue;
    		
    		if (isSchNameOnNonEquivLayNet(schNetNm, layNet, nmToLayNet)) continue;
    		
    		if (isEquivLayNetDesignerNamed(schNetNm, layNet)) continue;
    		
    		if (!isLegalLayNetName(schNetNm)) continue;

    		List<ArcInst> layArcs = getArcInsts(layNet);
    		
    		// If no arcs then continue
    		if (layArcs.size()==0) {
    			printHeader();
    			prln("    Can't copy schematic network name: "+schNetNm+
    				 " to layout because equivalent layout network has "+
    				 "no Arcs");
    			continue;
    		}

    		// Whew! It's OK to rename
    		numArcRenames++;
    		
    		int autoGenNumb = getAutoGenNumber(schNetNm);
    		if (autoGenNumb!=-1) {
    			maxSchemAutoGen = Math.max(maxSchemAutoGen, autoGenNumb);
    		}

    		if (DEBUG) {
	    		printHeader();
	    		prln("    Renaming layout net from: "+layNetNm+" to: "+schNetNm);
	    		schNmLayArcInsts.add(new SchemNetNm_LayArcInsts(schNetNm, layArcs));
    		}
    	}
		return new ArcRenameInfo(nmToLayArcInst, schNmLayArcInsts, maxSchemAutoGen);
    }
    
    private Map<String, NodeInst> buildNmToLayNodeInst(Cell layCell) {
    	Map<String, NodeInst> nmsToLayNodeInsts = new HashMap<String, NodeInst>();
    	for (Iterator<NodeInst> niIt=layCell.getNodes(); niIt.hasNext();) {
    		NodeInst ni = niIt.next();
    		nmsToLayNodeInsts.put(ni.getName(), ni);
    	}
    	return nmsToLayNodeInsts;
    }
    
    private NodeRenameInfo buildNodeRenameInfo(Cell schCell, Cell layCell,
	          								   VarContext schCtxt,
	          								   Equivalence equivs) {
    	Map<String, NodeInst> nmToLayNodeInst = buildNmToLayNodeInst(layCell);
    	
    	List<SchemNodaNm_LayNodeInst> schNmLayNodeInst = 
    		new ArrayList<SchemNodaNm_LayNodeInst>();
    	
    	for (Nodable schNode : new For<Nodable>(schCell.getNodables())) {
    		NodableNameProxy layProx = equivs.findEquivalentNode(schCtxt, schNode);
    		
    		// skip if layout has no equivalent nodable (e.g.: MOS deleted 
    		// because it is in parallel to another MOS 
    		if (layProx==null)  continue;
    		
    		Nodable layNoda = layProx.getNodable();
    		
    		// skip if layout Nodable isn't in top level Cell 
    		if (layNoda.getParent()!=layCell) continue;
    		
    		// skip if layout Nodable isn't a NodeInst. (This can happen
    		// because Electric allows mixing of schematics and layout elements
    		// in the same Cell.
    		if (!(layNoda instanceof NodeInst)) continue;
    		
    		NodeInst layNodeInst = (NodeInst) layNoda;
    		String layNodeNm = layNodeInst.getName();
    		String schNodeNm = schNode.getName();
    		
    		// If the schematic and layout nodes already have the
    		// same name then we're all set
    		if (layNodeNm.equals(schNodeNm)) continue;
    		
    		if (!isAutoGenName(layNodeNm)) {
    			printHeader();
    			prln("    Can't copy schematic node name: "+schNodeNm+
    				 " to layout because equivalent layout NodeInst already "+
    				 "has a user assigned name: "+layNodeNm);
    			numNodeManRenames++;
    			continue;
    		}
    		
    		if (!isAutoGenName(schNodeNm) && nmToLayNodeInst.containsKey(schNodeNm)) {
    			printHeader();
    			prln("   Can't copy schematic node name: "+schNodeNm+
    				 " to layout because some "+
    				 " non-equivalent layout node already uses that name");
    			numNameConflicts++;
    			continue;
    		}
    		
    		// Whew! It's OK to copy
    		schNmLayNodeInst.add(new SchemNodaNm_LayNodeInst(schNodeNm, layNodeInst));
    		numNodeRenames++;
    		
    		if (DEBUG) {
	    		printHeader();
	    		prln("    Renaming layout NodeInst from: "+layNodeNm+" to: "+
	    			 schNodeNm);
    		}
    	}
    	return new NodeRenameInfo(nmToLayNodeInst, schNmLayNodeInst);
    }
    private void renameLayNodesWithConflictingNames(NodeRenameInfo info, 
    		                                        NameGenerator nameGen) {
    	Map<String, NodeInst> nameToLayNodeInst = info.nameToLayNodeInst;
    	for (SchemNodaNm_LayNodeInst i : info.toRename) {
    		NodeInst layNode = nameToLayNodeInst.get(i.schemNodableName);
    		if (layNode!=null)  layNode.setName(nameGen.nextName());
    	}
    }
    private void renameEquivLayNodes(NodeRenameInfo info, NameGenerator nameGen) {
    	for (SchemNodaNm_LayNodeInst i : info.toRename) {
    		i.equivLayoutNodeInst.setName(i.schemNodableName);
    	}
    }
    
    private void renameNodes(NodeRenameInfo info) {
    	NameGenerator nameGen = 
    		new NameGenerator("ncc", 0, info.nameToLayNodeInst.keySet());
    	renameLayNodesWithConflictingNames(info, nameGen);
    	renameEquivLayNodes(info, nameGen);
    }
    
    // If a layout ArcInst has the same name as a schematic Network then 
    // rename then ArcInst
    private void renameLayArcsWithConflictingNames(ArcRenameInfo info,
    		                                       NameGenerator nameGen) {
    	Map<String, ArcInst> nameToLayArcInst = info.nameToLayArcInst;
    	for (SchemNetNm_LayArcInsts i : info.toRename) {
    		ArcInst layArc = nameToLayArcInst.get(i.schemNetworkName); 
    		if (layArc!=null)  layArc.setName(nameGen.nextName());
    	}
    }

    private ArcInst getLongestArc(List<ArcInst> arcs) {
    	ArcInst longest = null;
    	for (ArcInst ai : arcs) {
    		if (longest==null) longest = ai;
    		else if (ai.getLength()>longest.getLength())  longest = ai;
    	}
    	return longest;
    }

    // If a layout Network is about to get a name from the schematic,
    // rename all the arcs on the layout Network to prevent their
    // names from becoming "preferred". Remember that we may be copying
    // an auto generated name from the schematic to the layout.
    private void renameLayArcsToSchemName(ArcRenameInfo info, 
    		                                 NameGenerator nameGen) {
    	for (SchemNetNm_LayArcInsts i : info.toRename) {
    		// rename all ArcInsts on network to make sure none is
    		// preferred over schematic name
    		for (ArcInst a : i.equivLayoutArcInsts) {
    			a.setName(nameGen.nextName());
    		}
    		// Give longest arc the name from schematic
    		ArcInst longest = getLongestArc(i.equivLayoutArcInsts);
    		longest.setName(i.schemNetworkName);
    	}
    }
    
    private void renameArcs(ArcRenameInfo info) {
    	NameGenerator nameGen = 
    		new NameGenerator("net", info.maxSchemAutoNameNumb, 
    				          info.nameToLayArcInst.keySet());
    	renameLayArcsWithConflictingNames(info, nameGen);
    	renameLayArcsToSchemName(info, nameGen);
    }
    
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
    	NodeRenameInfo nodeInfo = buildNodeRenameInfo(schCell, layCell, schCtxt, equivs);
    	ArcRenameInfo arcInfo = buildArcRenameInfo(schCell, layCell, schCtxt, equivs);
    	renameNodes(nodeInfo);
    	renameArcs(arcInfo);
    }
    
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
    
    // Constructor does all the work
    private AllSchemNamesToLay(NccResults results) {
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
    	AllSchemNamesToLay sntl = new AllSchemNamesToLay(r);
    	return sntl.getResult();
    }
}
