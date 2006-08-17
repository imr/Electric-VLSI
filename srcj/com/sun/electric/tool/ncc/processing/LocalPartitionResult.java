package com.sun.electric.tool.ncc.processing;

import java.util.ArrayList;
import java.util.List;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.result.EquivRecReport;
import com.sun.electric.tool.ncc.result.NetObjReport;
import com.sun.electric.tool.ncc.trees.EquivRecord;

public class LocalPartitionResult {
    private NccGlobals globals;
    private List<EquivRecord> saveBadPartRecs = new ArrayList<EquivRecord>();
    private List<EquivRecord> saveBadWireRecs = new ArrayList<EquivRecord>();
    private List<EquivRecReport> badPartReps, badWireReps;

    private void prln(String s) {System.out.println(s);}
	 
//    private List<EquivRecord> getNotBalanced(Iterator<EquivRecord> it) {
//    	List<EquivRecord> notBalanced = new ArrayList<EquivRecord>();
//    	while (it.hasNext()) {
//    		EquivRecord er = it.next();
//        	if (!er.isBalanced())  notBalanced.add(er);
//    	}
//    	return notBalanced;
//    }

    private void printCircuitContents(List<NetObjReport> notMatched, 
    		                          List<NetObjReport> matched, 
    		                          String cktName, String t) {
		int numNetObjs = notMatched.size() + matched.size();
		prln("      "+cktName+" has "+numNetObjs+" of these "+t+":");
		int maxPrint = globals.getOptions().maxEquivRecMembersToPrint;
		if (numNetObjs>maxPrint) {
			prln("        Too many "+t+"! I'll only print the first "+maxPrint);
		}
		int numPrint = 0;
		for (NetObjReport o : notMatched) {
			if (numPrint>maxPrint)  break;
			prln("      * "+o.fullDescription());
			numPrint++;
		}
		for (NetObjReport o : matched) {
			if (numPrint>maxPrint)  break;
			prln("        "+o.fullDescription());
			numPrint++;
		}
	}

    private void printBadRecord(EquivRecReport r) {
    	String t = r.hasParts() ? "Parts" : "Wires";
		prln("    The "+t+" in this equivalence class share the following characteristics:");
		List<String> reasons = r.getReasons();
		for (String s : reasons)  prln("      "+s);

		List<List<NetObjReport>> matched = r.getMatchedNetObjs();
		List<List<NetObjReport>> notMatched = r.getNotMatchedNetObjs();
		for (int cktNdx=0; cktNdx<matched.size(); cktNdx++) {
			String cktName = globals.getRootCellNames()[cktNdx];
			printCircuitContents(notMatched.get(cktNdx), matched.get(cktNdx), cktName, t);
		}
	}

    private void printBadRecords(List<EquivRecReport> badRecs) {
        for (EquivRecReport r : badRecs)  printBadRecord(r); 
    }
    
    private void createReports() {
    	if (badPartReps!=null) return;

    	badPartReps = new ArrayList<EquivRecReport>();
    	for (EquivRecord er: saveBadPartRecs)
    		badPartReps.add(new EquivRecReport(er, false));

    	badWireReps = new ArrayList<EquivRecReport>();
    	for (EquivRecord er: saveBadWireRecs)
    		badWireReps.add(new EquivRecReport(er, false));
    }

    // --------------------------- public methods -----------------------------
//	public LocalPartitionResult(NccGlobals globals) {
//    	this.globals = globals;
//    	saveBadPartRecs = 
//		    getNotBalanced(globals.getPartLeafEquivRecs().getNotMatched());
//		saveBadWireRecs = 
//		    getNotBalanced(globals.getWireLeafEquivRecs().getNotMatched());
//	}
	public LocalPartitionResult(List<EquivRecord> notMatchedParts,
			                    List<EquivRecord> notMatchedWires,
			                    NccGlobals globals) {
		this.globals = globals;
		for (EquivRecord er : notMatchedParts) {
			if (!er.isBalanced()) saveBadPartRecs.add(er);
		}
		for (EquivRecord er : notMatchedWires) {
			if (!er.isBalanced()) saveBadWireRecs.add(er);
		}
	}

	/** @return true if no mismatches detected by Local Partitioning */
    public boolean matches() {
    	return saveBadPartRecs.size()==0 && saveBadWireRecs.size()==0;
    }

    /** @return List of all bad Part EquivRecReports detected by 
     * the Local Partition pass. An EquivRecord is bad if it's 
     * Circuits don't have equal numbers of NetObjects. */   
    public List<EquivRecReport> getPartRecReports() {
    	createReports();
    	return badPartReps;
    }
    
    /** @return List of all bad Wire EquivRecReports detected by 
     * the Local Partition pass. An EquivRecord is bad if it's 
     * Circuits don't have equal numbers of NetObjects. */   
    public List<EquivRecReport> getWireRecReports() {
    	createReports();
    	return badWireReps;
    }
    
    /** Print text diagnostics for bad Part and Wire EquivRecords */
    public void printErrorReport() {
    	if (!matches())
    		prln("\n  Mismatches found during local partitioning:\n");
    	createReports();
    	printBadRecords(badPartReps);
    	printBadRecords(badWireReps);
    }
}
