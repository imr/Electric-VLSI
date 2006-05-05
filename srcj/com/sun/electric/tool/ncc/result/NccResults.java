package com.sun.electric.tool.ncc.result;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.user.ncc.NccGuiInfo;

/** Summary results from the comparison of multiple pairs of Cells */
public class NccResults implements Iterable<NccResult>, Serializable {
    static final long serialVersionUID = 0;

	private List<NccResult> results = new ArrayList<NccResult>();
	
	/** Summary of all the results from all Cell pair comparisons */
	private boolean exportMatch = true;
	private boolean topologyMatch = true;
	private boolean sizeMatch = true;
	private boolean fatalError;
	
	public NccResults() {}

	public void add(NccResult r) {
		// If the user aborted then we have nothing to show
		if (r.userAbort()) return;
		
		LayoutLib.error(r==null, "Null NccResult shouldn't be possible?");
		
		results.add(r); 
		exportMatch &= r.exportMatch();
		topologyMatch &= r.topologyMatch();
		sizeMatch &= r.sizeMatch();
		
		NccGuiInfo cr = r.getNccGuiInfo();
		cr.setNccResult(r);
	} 
	
	/** Normally we save the results from all comparisons. However we don't 
	 * want to do this when checking every Cell flat (FLAT_EACH_CELL) */
	public void abandonPriorResults() {/*results.clear();*/}
	
	/** @return data for the NCC GUI */
    public List<NccGuiInfo> getAllComparisonMismatches() {
    	List<NccGuiInfo> mismatches = 
    		new ArrayList<NccGuiInfo>();
    	for (NccResult r : results) {
    		if (r.guiNeedsToReport())  
    			mismatches.add(r.getNccGuiInfo());
    	}
        return mismatches;
    }
    
	/** No problem was found with Exports */ 
	public boolean exportMatch() {return exportMatch;}
	
	/** No problem was found with the network topology */
	public boolean topologyMatch() {return topologyMatch;}
    
	/** @return true if no problem was found */
    public boolean match() {return exportMatch && topologyMatch && sizeMatch && !fatalError;}
    
    /** @return all the NccResult from all Cell pair comparisons. Begin with
	 * the leaf Cells and move toward the root Cells */ 
    public Iterator<NccResult> iterator() {return results.iterator();}
    
    /** @return the NccResult from comparing the top level Cells in the hierarchy */
    public NccResult getResultFromRootCells() {
    	int sz = results.size();
    	LayoutLib.error(sz==0, "No results in NccResults");
    	return results.get(sz-1);
    }

    public String summary(boolean checkSizes) {
		String s;
		if (exportMatch) {
			s = "exports match, ";
		} else {
			s = "exports mismatch, ";
		}
		if (topologyMatch) {
			s += "topologies match, ";
		} else {
			s += "topologies mismatch, ";
		}
		if (!checkSizes) {
			s += "sizes not checked";
		} else if (sizeMatch) {
			s += "sizes match";
		} else {
			s += "sizes mismatch";
		}
		return s;
	}
}
