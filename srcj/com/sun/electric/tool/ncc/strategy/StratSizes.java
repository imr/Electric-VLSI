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
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.NccEngine;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.trees.EquivRecord;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.jemNets.NetObject;
import com.sun.electric.tool.ncc.lists.RecordList;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.basic.Messenger;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.jemNets.Wire;
import com.sun.electric.tool.ncc.jemNets.Transistor;
import com.sun.electric.tool.ncc.NccGlobals;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

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
		Transistor t = first.outlier;
		return t.getParent().getParent();
	}
	public boolean isOutlier(Transistor t) {
		for (Iterator it=outlierTrans.iterator(); it.hasNext();) {
			OutlierTrans tr = (OutlierTrans) it.next(); 
			if (t==tr.outlier) return true; 
		}
		return false;
	}
}
/** The Transistor in the Circuit that is most different from the others */
class OutlierTrans {
	public final Transistor outlier;
	/** absolute value of difference / average */
	public final double deviation;
	OutlierTrans(Transistor t, double diff) {outlier=t; deviation=diff;}
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
	/** Find the smallest and largest Transistors in this Circuit  
	 * @return array of 2 OutlierTrans or null if Parts not Transistors */
	private OutlierTrans[] findSmallLargeOutliers(Circuit c) {
		double minSz = Double.MAX_VALUE;
		double maxSz = Double.MIN_VALUE;
		double sumSz = 0;
		int numTrans = 0;
		Transistor minT = null;
		Transistor maxT = null;
		for (Iterator it=c.getNetObjs(); it.hasNext();) {
			NetObject no = (NetObject) it.next();
			if (!(no instanceof Transistor)) return null;
			Transistor t = (Transistor) no;
			double sz = t.getWidth();
			if (sz<minSz) {minT=t; minSz=sz;}
			if (sz>maxSz) {maxT=t; maxSz=sz;}
			sumSz += sz;
			numTrans++;
		}
		LayoutLib.error(numTrans==0, "Empty circuit?");
		if (sumSz==0) return null; // all zero width, avoid divide by zero
		double avgSz = sumSz / numTrans;
		double smallDev = (avgSz-minSz) / avgSz;
		double largeDev = (maxSz-avgSz) / avgSz;
		return new OutlierTrans[] {
			new OutlierTrans(minT, smallDev),
			new OutlierTrans(maxT, largeDev)
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
		ArrayList small = new ArrayList();
		ArrayList big = new ArrayList();
		for (Iterator it=r.getCircuits(); it.hasNext();) {
			Circuit c = (Circuit) it.next();
			OutlierTrans[] o = findSmallLargeOutliers(c);
			// no Outlier found
			if (o==null) return null;
			small.add(o[0]);
			big.add(o[1]);
		}
		return maxDeviation(new OutlierRecord(small), new OutlierRecord(big));
	}
	/** Find the OutlierRecord with the largest deviation */
	private OutlierRecord findOutlierRecordWithLargestDeviation() {
		OutlierRecord furthestOut = null;
		LeafList frontier = StratFrontier.doYourJob(globals.getParts(), globals);
		for (Iterator it=frontier.iterator(); it.hasNext();) {
			EquivRecord r = (EquivRecord) it.next();
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
		Transistor t = (Transistor) n;
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
