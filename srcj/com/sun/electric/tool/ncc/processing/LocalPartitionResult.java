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
		private ArrayList<NetObject>[] matches;
		private ArrayList<NetObject>[] notMatches;
		private void appendNetObjsFromCircuit(ArrayList<NetObject>[] lists, EquivRecord er) {
			int i=0;
			for (Iterator<Circuit> itC=er.getCircuits(); itC.hasNext(); i++) {
				Circuit ckt = itC.next();
				for (Iterator<NetObject> itN=ckt.getNetObjs(); itN.hasNext();) {
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
				matches[i] = new ArrayList<NetObject>();
				notMatches[i] = new ArrayList<NetObject>();
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
		ArrayList<NetObject>[] getMatchedNetObjs() {return matches;}
		ArrayList<NetObject>[] getNotMatchedNetObjs() {return notMatches;}
	}
	
	private NccGlobals globals;
    private List<EquivRecord> badPartRecs, badWireRecs;

    private void prln(String s) {System.out.println(s);}
	 
    private List<EquivRecord> getNotBalancedEquivRecs(Iterator<EquivRecord> it) {
    	List<EquivRecord> notBalanced = new ArrayList<EquivRecord>();
    	while (it.hasNext()) {
    		EquivRecord er = it.next();
        	if (!er.isBalanced())  notBalanced.add(er);
    	}
    	return notBalanced;
    }

    private void printCircuitContents(List<NetObject> notMatched, List<NetObject> matched, 
    		                          String cktName, String t) {
		int numNetObjs = notMatched.size() + matched.size();
		prln("      "+cktName+" has "+numNetObjs+" of these "+t+":");
		int maxPrint = globals.getOptions().maxEquivRecMembersToPrint;
		if (numNetObjs>maxPrint) {
			prln("        Too many "+t+"! I'll only print the first "+maxPrint);
		}
		int numPrint = 0;
		for (Iterator<NetObject> it=notMatched.iterator(); it.hasNext(); numPrint++) {
			if (numPrint>maxPrint)  break;
			NetObject o = it.next();
			prln("      * "+o.fullDescription());
		}
		for (Iterator<NetObject> it=matched.iterator(); it.hasNext(); numPrint++) {
			if (numPrint>maxPrint)  break;
			NetObject o = it.next();
			prln("        "+o.fullDescription());
		}
	}

    private void printBadRecord(EquivRecord r, String t) {
		prln("    The "+t+" in this equivalence class share the following characteristics:");
		List<String> reasons = r.getPartitionReasonsFromRootToMe();
		for (String str : reasons) {
			prln("      "+str);
		}
		List<NetObject> matched[] = getMatchedNetObjs(r);
		List<NetObject> notMatched[] = getNotMatchedNetObjs(r);
		for (int cktNdx=0; cktNdx<matched.length; cktNdx++) {
			String cktName = globals.getRootCellNames()[cktNdx];
			printCircuitContents(notMatched[cktNdx], matched[cktNdx], cktName, t);
		}
	}

    private void printBadRecords(List<EquivRecord> badRecs, String t) {
        for (EquivRecord er : badRecs) {
    		printBadRecord(er, t);			
    	}
    }

	public LocalPartitionResult(NccGlobals globals) {
    	this.globals = globals;
    	badPartRecs = 
		    getNotBalancedEquivRecs(globals.getPartLeafEquivRecs().getNotMatched());
		badWireRecs = 
		    getNotBalancedEquivRecs(globals.getWireLeafEquivRecs().getNotMatched());
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
    public Iterator<EquivRecord> getBadPartEquivRecs() {return badPartRecs.iterator();}
    /** @return number of all bad Part EquivRecords detected by 
     * the Local Partition pass. An EquivRecord is bad if it's 
     * Circuits don't have equal numbers of NetObjects. */       
    public int badPartEquivRecCount() {return badPartRecs.size();}
    
    /** @return Iterator over all bad Wire EquivRecords detected by 
     * the Local Partition pass. An EquivRecord is bad if it's 
     * Circuits don't have equal numbers of NetObjects. */   
    public Iterator<EquivRecord> getBadWireEquivRecs() {return badWireRecs.iterator();}
    /** @return number of all bad Wire EquivRecords detected by 
     * the Local Partition pass. An EquivRecord is bad if it's 
     * Circuits don't have equal numbers of NetObjects. */       
    public int badWireEquivRecCount() {return badWireRecs.size();}
    
    /** Get all matched NetObjects. Visit all EquivRecords that are descendents
     * of er. Accumulate all NetObjects inside those EquivRecords that are 
     * matched(). 
     * @return ArrayLists, one per Circuit, of matched NetObjects. */
    public ArrayList<NetObject>[] getMatchedNetObjs(EquivRecord er) {
    	GetNetObjs gno = new GetNetObjs(globals);
    	gno.doFor(er);
    	return gno.getMatchedNetObjs();
    }
    
    /** Get all not matched NetObjects. Visit all EquivRecords that are 
     * descendents of er. Accumulate all NetObjects inside those 
     * EquivRecords that are not matched().
     * @return ArrayLists, one per Circuit, of notMatched NetObjects. */ 
    public ArrayList<NetObject>[] getNotMatchedNetObjs(EquivRecord er) {
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
