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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.netlist.Mos;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

/** The OutlierTrans for each Circuit in the EquivRecord */
class OutlierRecord {
	private ArrayList outlierTrans = new ArrayList();
	OutlierRecord(List outlierTrans) {
		this.outlierTrans.addAll(outlierTrans);
	}
	/** Heuristic: average of outlier deviations */
	public double deviation() {
		double sum = 0;
		for (Iterator it=outlierTrans.iterator(); it.hasNext();) {
			sum += ((OutlierTrans)it.next()).deviation;
		}
		return sum/outlierTrans.size();
	}
	public EquivRecord getEquivRecord() {
		OutlierTrans first = (OutlierTrans) outlierTrans.get(1);
		Mos t = first.outlier;
		return t.getParent().getParent();
	}
	public boolean isOutlier(Mos t) {
		for (Iterator it=outlierTrans.iterator(); it.hasNext();) {
			OutlierTrans tr = (OutlierTrans) it.next(); 
			if (t==tr.outlier) return true; 
		}
		return false;
	}
}
/** The Transistor in the Circuit that is most different from the others */
class OutlierTrans {
	public final Mos outlier;
	/** absolute value of difference / average */
	public final double deviation;
	OutlierTrans(Mos t, double diff) {outlier=t; deviation=diff;}
}

/** If Hash code partitioning fails to match all equivalence classes then try to 
 * partition equivalence classes containing Transistors based on Transistor
 * widths. This must be done before random matching or else we'll get width
 * mismatches. */
public class StratSizes extends Strategy {
	private static final Integer CODE_OUTLIER = new Integer(1);
	private static final Integer CODE_REST = new Integer(2);
	private OutlierRecord outlierRecord;

	private StratSizes(NccGlobals globals) {super(globals);}

	private boolean widthsMatch(OutlierTrans o1, OutlierTrans o2) {
		return NccUtils.sizesMatch(o1.outlier.getWidth(), o2.outlier.getWidth(), 
								   globals.getOptions());
	}
	/** Find the Transistors with extreme width and length in this Circuit  
	 * @return array of 4 OutlierTrans or null if Parts not Transistors */
	private OutlierTrans[] findThinestWidestShortestLongest(Circuit c) {
		double minWid = Double.MAX_VALUE;
		double maxWid = Double.MIN_VALUE;
		double minLen = Double.MAX_VALUE;
		double maxLen = Double.MIN_VALUE;
		double sumWid = 0;
		double sumLen = 0;
		int numTrans = 0;
		Mos thinnest = null;
		Mos widest = null;
		Mos shortest = null;
		Mos longest = null;
		for (Iterator it=c.getNetObjs(); it.hasNext();) {
			NetObject no = (NetObject) it.next();
			if (!(no instanceof Mos)) return null;
			Mos m = (Mos) no;

			double w = m.getWidth();
			if (w<minWid) {thinnest=m; minWid=w;}
			if (w>maxWid) {widest=m; maxWid=w;}
			sumWid += w;

			double l = m.getLength();
			if (l<minLen) {shortest=m; minLen=l;}
			if (l>maxLen) {longest=m; maxLen=l;}
			sumLen += l;
			
			numTrans++;
		}
		LayoutLib.error(numTrans==0, "Empty circuit?");
		if (sumWid==0) return null; // all zero width, avoid divide by zero
		double avgWid = sumWid / numTrans;
		double thinDev = (avgWid-minWid) / avgWid;
		double wideDev = (maxWid-avgWid) / avgWid;

		double avgLen = sumLen / numTrans;
		double shortDev = (avgLen-minLen) / avgLen;
		double longDev = (maxWid-avgLen) / avgLen;
		
		return new OutlierTrans[] {
			new OutlierTrans(thinnest, thinDev),
			new OutlierTrans(widest, wideDev),
			new OutlierTrans(shortest, shortDev),
			new OutlierTrans(longest, longDev)
		};
	}
	private OutlierRecord maxDeviation(OutlierRecord o1, OutlierRecord o2) {
		return o1.deviation()>=o2.deviation() ? o1 : o2;
	}
	/** Try matching the smallest and largest transistors in each Circuit. 
	 * @return OutlierRecord with greatest deviation.
	 * Return null if no outliers */
	private OutlierRecord buildOutlierRecord(EquivRecord r) {
		OutlierTrans first = null;
		ArrayList thins = new ArrayList();
		ArrayList wides = new ArrayList();
		ArrayList shorts = new ArrayList();
		ArrayList longs = new ArrayList();
		for (Iterator it=r.getCircuits(); it.hasNext();) {
			Circuit c = (Circuit) it.next();
			OutlierTrans[] o = findThinestWidestShortestLongest(c);
			// no Outlier found
			if (o==null) return null;
			thins.add(o[0]);
			wides.add(o[1]);
			shorts.add(o[2]);
			longs.add(o[3]);
		}
		// Thinnest MOS of each circuit
		OutlierRecord thinestMOSs = new OutlierRecord(thins);
		// Widest MOS of each circuit
		OutlierRecord widestMOSs = new OutlierRecord(wides);
		// Shortest MOS of each circuit
		OutlierRecord shortestMOSs = new OutlierRecord(shorts);
		// Longest MOS of each circuit
		OutlierRecord longestMOSs = new OutlierRecord(longs);
		OutlierRecord outlier = maxDeviation(thinestMOSs, widestMOSs);;
		outlier = maxDeviation(outlier, shortestMOSs);
		outlier = maxDeviation(outlier, longestMOSs);
		return outlier;
	}
	/** Find the OutlierRecord with the largest deviation */
	private OutlierRecord findOutlierRecordWithLargestDeviation() {
		OutlierRecord furthestOut = null;
		Iterator frontier = globals.getPartLeafEquivRecs().getUnmatched();
		while (frontier.hasNext()) {
			EquivRecord r = (EquivRecord) frontier.next();
			if (r.isMismatched())  continue;
			OutlierRecord farOut = buildOutlierRecord(r);
			if (farOut==null) continue;
			if (furthestOut==null) {
				furthestOut = farOut;
			} else {
				furthestOut = maxDeviation(furthestOut, farOut);
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
		Mos t = (Mos) n;
		return outlierRecord.isOutlier(t) ? CODE_OUTLIER : CODE_REST;
	}

	//----------------------------- intended interface ------------------------
	/** Find the equivalence class with the largest deviation in Transistor
	 *  widths and partition that class based on Transistor widths. 
	 * @return the offspring resulting from the partition. Return an empty list
	 * if no partitioning was possible.	*/ 
	public static LeafList doYourJob(NccGlobals globals){
		StratSizes ss = new StratSizes(globals);
		return ss.doYourJob();
	}
}
