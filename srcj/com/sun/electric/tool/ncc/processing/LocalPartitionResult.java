package com.sun.electric.tool.ncc.processing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.strategy.Strategy;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

public class LocalPartitionResult {
	private static class GetNetObjs extends Strategy {
		private ArrayList[] matches;
		private ArrayList[] notMatches;
		private void appendNetObjsFromCircuit(ArrayList[] lists, EquivRecord er) {
			int i=0;
			for (Iterator itC=er.getCircuits(); itC.hasNext(); i++) {
				Circuit ckt = (Circuit) itC.next();
				for (Iterator itN=ckt.getNetObjs(); itN.hasNext();) {
					lists[i].add(itN.next());
				}
			}
		}
		GetNetObjs(NccGlobals globals) {
			super(globals);
			int numDesigns = globals.getNumNetlistsBeingCompared();
			matches = new ArrayList[numDesigns];
			notMatches = new ArrayList[numDesigns];
			for (int i=0; i<numDesigns; i++) {
				matches[i] = new ArrayList();
				notMatches[i] = new ArrayList();
			}
		}
		public LeafList doFor(EquivRecord er) {
			if (er.isLeaf()) {
				appendNetObjsFromCircuit(er.isMatched() ? matches : notMatches, er);
				return new LeafList();
			} else {
				return super.doFor(er);
			}
		}
		ArrayList[] getMatchedNetObjs() {return matches;}
		ArrayList[] getNotMatchedNetObjs() {return notMatches;}
	}
	
	private NccGlobals globals;
    private List badPartRecs, badWireRecs;

    private void prln(String s) {System.out.println(s);}
	 
    private List getNotMatchedEquivRecs(Iterator it) {
    	List mismatched = new ArrayList();
    	while (it.hasNext()) {
    		EquivRecord er = (EquivRecord) it.next();
        	if (er.isMismatched())  mismatched.add(er);
    	}
    	return mismatched;
    }

    private void printCircuitContents(List notMatched, List matched, 
    		                          String cktName, String t) {
		int numNetObjs = notMatched.size() + matched.size();
		prln("      "+cktName+" has "+numNetObjs+" of these "+t+":");
		int maxPrint = globals.getOptions().maxEquivRecMembersToPrint;
		if (numNetObjs>maxPrint) {
			prln("        Too many "+t+"! I'll only print the first "+maxPrint);
		}
		int numPrint = 0;
		for (Iterator it=notMatched.iterator(); it.hasNext(); numPrint++) {
			if (numPrint>maxPrint)  break;
			NetObject o = (NetObject) it.next();
			prln("      * "+o.fullDescription());
		}
		for (Iterator it=matched.iterator(); it.hasNext(); numPrint++) {
			if (numPrint>maxPrint)  break;
			NetObject o = (NetObject) it.next();
			prln("        "+o.fullDescription());
		}
	}

    private void printBadRecord(EquivRecord r, String t) {
		prln("    The "+t+" in this equivalence class share the following characteristics:");
		List reasons = r.getPartitionReasonsFromRootToMe();
		for (Iterator it=reasons.iterator(); it.hasNext();) {
			prln("      "+it.next());
		}
		List matched[] = getMatchedNetObjs(r);
		List notMatched[] = getNotMatchedNetObjs(r);
		for (int cktNdx=0; cktNdx<matched.length; cktNdx++) {
			String cktName = globals.getRootCellNames()[cktNdx];
			printCircuitContents(notMatched[cktNdx], matched[cktNdx], cktName, t);
		}
	}

    private void printBadRecords(List badRecs, String t) {
        for (Iterator it=badRecs.iterator(); it.hasNext();) {
    		printBadRecord((EquivRecord) it.next(), t);			
    	}
    }

	public LocalPartitionResult(NccGlobals globals) {
    	this.globals = globals;
    	badPartRecs = 
		    getNotMatchedEquivRecs(globals.getPartLeafEquivRecs().getNotMatched());
		badWireRecs = 
		    getNotMatchedEquivRecs(globals.getWireLeafEquivRecs().getNotMatched());
	}

	/** @return true if no mismatches detected by Local Partitioning */
    public boolean matches() {
    	return badPartRecs.size()==0 && badWireRecs.size()==0;
    }
    /** @return the total number of mismatches */
    public int size() {
        return badPartRecs.size() + badWireRecs.size();
    }
    /** @return Iterator over all bad Part EquivRecords detected by 
     * the Local Partition pass. An EquivRecord is bad if it's 
     * Circuits don't have equal numbers of NetObjects. */   
    public Iterator getBadPartEquivRecs() {return badPartRecs.iterator();}
    /** @return number of all bad Part EquivRecords detected by 
     * the Local Partition pass. An EquivRecord is bad if it's 
     * Circuits don't have equal numbers of NetObjects. */       
    public int badPartEquivRecCount() {return badPartRecs.size();}
    
    /** @return Iterator over all bad Wire EquivRecords detected by 
     * the Local Partition pass. An EquivRecord is bad if it's 
     * Circuits don't have equal numbers of NetObjects. */   
    public Iterator getBadWireEquivRecs() {return badWireRecs.iterator();}
    /** @return number of all bad Wire EquivRecords detected by 
     * the Local Partition pass. An EquivRecord is bad if it's 
     * Circuits don't have equal numbers of NetObjects. */       
    public int badWireEquivRecCount() {return badWireRecs.size();}
    
    /** Get all matched NetObjects. Visit all EquivRecords that are descendents
     * of er. Accumulate all NetObjects inside those EquivRecords that are 
     * matched(). 
     * @return ArrayLists, one per Circuit, of matched NetObjects. */
    public ArrayList[] getMatchedNetObjs(EquivRecord er) {
    	GetNetObjs gno = new GetNetObjs(globals);
    	gno.doFor(er);
    	return gno.getMatchedNetObjs();
    }
    
    /** Get all not matched NetObjects. Visit all EquivRecords that are 
     * descendents of er. Accumulate all NetObjects inside those 
     * EquivRecords that are not matched().
     * @return ArrayLists, one per Circuit, of notMatched NetObjects. */ 
    public ArrayList[] getNotMatchedNetObjs(EquivRecord er) {
    	GetNetObjs gno = new GetNetObjs(globals);
    	gno.doFor(er);
    	return gno.getNotMatchedNetObjs();
    }
    
    /** Print text diagnostics for bad Part and Wire EquivRecords */
    public void printErrorReport() {
    	if (!matches())
    		prln("\n  Mismatches found during local partitioning:\n");
    	printBadRecords(badPartRecs, "Parts");
    	printBadRecords(badWireRecs, "Wires");
    }
}
