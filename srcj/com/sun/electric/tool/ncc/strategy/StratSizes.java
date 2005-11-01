/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratSizes.java
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
import java.util.Iterator;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.netlist.Mos;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Part;
import com.sun.electric.tool.ncc.netlist.Resistor;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

/** The OutlierTrans for each Circuit in the EquivRecord */
class OutlierRecord {
	/** The Part in the Circuit that is most different from the others */
	private static class Outlier {
		public final Part part;
		public final double deviation;
		Outlier(Part p, double diff) {part=p; deviation=diff;}
	}

	private final Part[] parts;
	private final double deviation;
	
	private boolean hasSize(Part p) {
		return p instanceof Resistor || p instanceof Mos;
	}
	private double getWidth(Part p) {
		if (p instanceof Resistor) {
			return ((Resistor)p).getWidth();
		} else {
			return ((Mos)p).getWidth();
		}
	}
	private double getLength(Part p) {
		if (p instanceof Resistor) {
			return ((Resistor)p).getLength();
		} else {
			return ((Mos)p).getLength();
		}
	}
	private double[] computeAverageWidthLength(Circuit c) {
		double totLen=0, totWid=0;
		for (Iterator<NetObject> it=c.getNetObjs(); it.hasNext();) {
			Part p = (Part) it.next();
			totLen += getLength(p);
			totWid += getWidth(p);
		}
		int numParts = c.numNetObjs();
		return new double[] {totWid/numParts, totLen/numParts};
	}
	private double computeWidthLengthDeviation(double avgWid, double avgLen, Part p) {
		double deltaW = (getWidth(p)-avgWid) / avgWid;
		double deltaL = (getLength(p)-avgLen) / avgLen;
		return Math.abs(deltaW) + Math.abs(deltaL);
	}
	private double computeWidthLengthMismatch(Part p1, Part p2) {
		double w1 = getWidth(p1);
		double l1 = getLength(p1);
		double w2 = getWidth(p2);
		double l2 = getLength(p2);
		double avgW = (w1+w2)/2;
		double avgL = (l1+l2)/2;
		return Math.abs((w1-w2)/avgW) + Math.abs((l1-l2)/avgL);
	}
	private Outlier findOutlier(Circuit c) {
		if (!hasSize((Part)c.getNetObjs().next())) 
			return new Outlier(null, 0);

		double[] avgWidLen = computeAverageWidthLength(c);
		double avgWid = avgWidLen[0];
		double avgLen = avgWidLen[1];
		
		Part worstPart = null;
		double worstDeviation = 0; 
		for (Iterator<NetObject> it=c.getNetObjs(); it.hasNext();) {
			Part p = (Part) it.next();
			double deviation = computeWidthLengthDeviation(avgWid, avgLen, p);
			if (deviation>worstDeviation) {
				worstPart = p;
				worstDeviation = deviation;
			}
		}
		return new Outlier(worstPart, worstDeviation);
	}
	private Part findClosestSizedPart(Circuit c, Part outlier) {
		Part bestPart = null;
		double bestMismatch = Double.MAX_VALUE; 
		for (Iterator<NetObject> it=c.getNetObjs(); it.hasNext();) {
			Part p = (Part) it.next();
			double mismatch = computeWidthLengthMismatch(outlier, p);
			if (mismatch<bestMismatch) {
				bestPart = p;
				bestMismatch = mismatch;
			}
		}
		return bestPart;
	}
	public OutlierRecord(EquivRecord r) {
		parts = new Part[r.numCircuits()];
		Iterator<Circuit> it = r.getCircuits();

		// find outlier of 0th Circuit
		Outlier outlier = findOutlier((Circuit) it.next());
		deviation = outlier.deviation;
		parts[0] = outlier.part;
		if (deviation==0) return;
		
		// find closest sized Parts from all other Circuits 
		// parts[0] already initialized
		for (int cktNdx=1; it.hasNext(); cktNdx++) {
			Circuit c = (Circuit) it.next();
			parts[cktNdx] = findClosestSizedPart(c, outlier.part);
		}
	}

	public double deviation() {return deviation;}
	public EquivRecord getEquivRecord() {
		return parts[0].getParent().getParent();
	}
	public boolean isOutlier(Part p) {
		for (int i=0; i<parts.length; i++) {
			if (p==parts[i]) return true; 
		}
		return false;
	}
}

/** If Hash code partitioning fails to match all equivalence classes then try to 
 * partition equivalence classes containing Transistors and Resistors based on 
 * widths and lengths. This must be done before random matching or else we'll 
 * get width and length mismatches. */
public class StratSizes extends Strategy {
	private static final Integer CODE_OUTLIER = new Integer(1);
	private static final Integer CODE_REST = new Integer(2);
	private OutlierRecord outlierRecord;

	private StratSizes(NccGlobals globals) {super(globals);}

	private OutlierRecord findOutlierRecordWithLargestDeviation() {
		OutlierRecord furthestOut = null;
		Iterator<EquivRecord> frontier = globals.getPartLeafEquivRecs().getNotMatched();
		while (frontier.hasNext()) {
			EquivRecord r = (EquivRecord) frontier.next();
			if (!r.isBalanced())  continue;
			OutlierRecord farOut = new OutlierRecord(r);
			if (farOut.deviation()==0) continue;
			if (furthestOut==null) {
				furthestOut = farOut;
			} else if (farOut.deviation()>furthestOut.deviation()) {
				furthestOut = farOut;
			}
		}
		return furthestOut;
	}
	
	private LeafList doYourJob() {
		OutlierRecord r = findOutlierRecordWithLargestDeviation();
		if (r==null) return new LeafList();

		outlierRecord = r;
		return doFor(r.getEquivRecord());
	}

	public Integer doFor(NetObject n){
		Part p = (Part) n;
		return outlierRecord.isOutlier(p) ? CODE_OUTLIER : CODE_REST;
	}

	//----------------------------- intended interface ------------------------
	/** Find the equivalence class with the largest deviation in Part
	 *  sizes and partition that class based on sizes. 
	 * @return the offspring resulting from the partition. Return an empty list
	 * if no partitioning was possible.	*/ 
	public static LeafList doYourJob(NccGlobals globals){
		StratSizes ss = new StratSizes(globals);
		return ss.doYourJob();
	}
}
