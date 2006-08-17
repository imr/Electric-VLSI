/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EquivRecReport.java
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
package com.sun.electric.tool.ncc.result;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.result.NetObjReport.NetObjReportable;

public class EquivRecReport implements Serializable {
	static final long serialVersionUID = 0;
	
	public interface EquivRecReportable {
		List<String> getPartitionReasonsFromRootToMe();
		void getNetObjReportablesFromEntireTree(
							List<List<NetObjReportable>> matched,
				            List<List<NetObjReportable>> notMatched);
	}

	// ------------------------------- data -----------------------------------
	// A ResEquivRec is either due to a local partitioning mismatch
	// or a hash code mismatch.
	private final boolean hashMismatch;
	
	// matched exists only for bad local partition records. matched
	// holds those ResNetObjs belonging to bad local partition
	// records that were matched by hash coding.
	private final List<List<NetObjReport>> matched, notMatched; 
	private final List<String> reasons;
	
	private List<NetObjReport> makeReports1(List<NetObjReportable> nor) {
		List<NetObjReport> ans = new ArrayList<NetObjReport>();
		for (NetObjReportable j : nor) {
			ans.add(NetObjReport.newNetObjReport(j));
		}
		return ans;
	}
	private List<List<NetObjReport>> makeReports(List<List<NetObjReportable>> nors) {
		List<List<NetObjReport>> ans = new ArrayList<List<NetObjReport>>();
		for (List<NetObjReportable> i : nors) {
			ans.add(makeReports1(i));
		}
		return ans;
	}

	public EquivRecReport(EquivRecReportable er, boolean hashMismatch) {
		this.hashMismatch = hashMismatch;
		reasons = er.getPartitionReasonsFromRootToMe();
		List<List<NetObjReportable>> m = new ArrayList<List<NetObjReportable>>();
		List<List<NetObjReportable>> nm = new ArrayList<List<NetObjReportable>>();
		er.getNetObjReportablesFromEntireTree(m, nm);

		matched = makeReports(m);
		notMatched = makeReports(nm);
	}

	public EquivRecReport(boolean hashMismatch, boolean hasParts,
						  List<List<NetObjReport>> matched, 
						  List<List<NetObjReport>> notMatched,
						  List<String> reasons) {
		this.hashMismatch = hashMismatch;
		this.matched = matched;
		this.notMatched = notMatched;
		this.reasons = reasons;
	}
	public int maxSize() {
		return Math.max(matched.get(0).size() + notMatched.get(0).size(),
			            matched.get(1).size() + notMatched.get(1).size());
	}
	public boolean hashMismatch() {return hashMismatch;}
	public List<String> getReasons() {return reasons;}
	public boolean hasParts() {
		for (List<NetObjReport> i : matched) {
			for (NetObjReport j : i) {
				return (j instanceof PartReport);
			}
		}
		for (List<NetObjReport> i : notMatched) {
			for (NetObjReport j : i) {
				return (j instanceof PartReport);
			}
		}
		LayoutLib.error(true, "EquivRecord with no NetObjects?");
		return false;
	}
	/** Only bad local partition EquivRecords have matched net objects. */
	public List<List<NetObjReport>> getMatchedNetObjs() {return matched;}
	public List<List<NetObjReport>> getNotMatchedNetObjs() {return notMatched;}
}
