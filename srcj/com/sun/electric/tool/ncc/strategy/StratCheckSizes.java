/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StratCheckSizes.java
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

/** StratFrontier finds all non-matched EquivRecords */

package com.sun.electric.tool.ncc.strategy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.lists.LeafList;
import com.sun.electric.tool.ncc.netlist.Mos;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Part;
import com.sun.electric.tool.ncc.netlist.Subcircuit;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

public class StratCheckSizes extends Strategy {
	// ----------------------- private types ----------------------------------0
	public static abstract class Mismatch {
		private StringBuffer sb = new StringBuffer();
		private void aln(String s) {sb.append(s); sb.append("\n");}
		public final double min, max;
		public final Mos minMos, maxMos;
        public final int minNdx, maxNdx;
		Mismatch(double min, Mos minMos, int minNdx, 
                 double max, Mos maxMos, int maxNdx) {
			this.min=min; this.max=max;
			this.minMos=minMos; this.maxMos=maxMos;
            this.minNdx = minNdx; this.maxNdx = maxNdx;
		}
		public double relErr() {return (max-min)/min;}
		public double absErr() {return max-min;}
		public boolean isCap() {return minMos.isCapacitor();}
		public abstract String widLen();
		public abstract String wl();
		public String toString() {
			// don't round if error will round to zero
			double relErr, absErr, minSz, maxSz;
			if (relErr()*100<.1 || absErr()<.1) {
				relErr = relErr()*100;
				absErr = absErr();
				minSz = min;
				maxSz = max;
			} else {
				relErr = NccUtils.round(relErr()*100,1);
				absErr = NccUtils.round(absErr(),2);
				minSz = NccUtils.round(min,2);
				maxSz = NccUtils.round(max,2);
			}
			aln("    MOS"+
				(isCap()?" capacitor":"")+
				" "+widLen()+"s don't match. "+
				" relativeError="+relErr+"%"+
				" absoluteError="+absErr);
			aln("      "+wl()+"="+minSz+" for "+minMos.fullDescription());
			aln("      "+wl()+"="+maxSz+" for "+maxMos.fullDescription());
			return sb.toString();
		}
	}
    public static class LengthMismatch extends Mismatch {
		public String widLen() {return "length";}
		public String wl() {return "L";}
		public LengthMismatch(double min, Mos minMos, int minNdx, 
                              double max, Mos maxMos, int maxNdx) {
			super(min, minMos, minNdx, max, maxMos, maxNdx);
		}
	}
    public static class WidthMismatch extends Mismatch {
		public String widLen() {return "width";}
		public String wl() {return "W";}
		public WidthMismatch(double min, Mos minMos, int minNdx, 
                             double max, Mos maxMos, int maxNdx) {
			super(min, minMos, minNdx, max, maxMos, maxNdx);
		}
	}
	// ------------------------------ private data -----------------------------
	private NccOptions options;
	private double minWidth, maxWidth, minLength, maxLength;
	private Mos minWidMos, maxWidMos, minLenMos, maxLenMos;
	private List mismatches = new ArrayList();
    private int cktNdx, minWidNdx, maxWidNdx, minLenNdx, maxLenNdx;
	
    private StratCheckSizes(NccGlobals globals) {
    	super(globals);
    	options = globals.getOptions();
    }
    
    private void checkWidthMismatch() {
		if (!NccUtils.sizesMatch(maxWidth, minWidth, options)) {
			mismatches.add(new WidthMismatch(minWidth, minWidMos, minWidNdx,  
					                         maxWidth, maxWidMos, maxWidNdx));
		}
    }
    private void checkLengthMismatch() {
    	if (!NccUtils.sizesMatch(maxLength, minLength, options)) {
    		mismatches.add(new LengthMismatch(minLength, minLenMos, minLenNdx,
    				                          maxLength, maxLenMos, maxLenNdx));
    	}
    }
    private static class MismatchComparator implements Comparator {
    	public int compare(Object o1, Object o2) {
    		Mismatch m1 = (Mismatch) o1;
    		Mismatch m2 = (Mismatch) o2;
    		double diff = m1.relErr() - m2.relErr();
    		if (diff==0) return 0;
    		return diff>0 ? 1 : -1;
    	}
    }
    private void summary() {
    	if (mismatches.size()==0) return; // no news is good news
    	Collections.sort(mismatches, new MismatchComparator());
    	System.out.println("  There are "+mismatches.size()+" size mismatches.");
    	for (Iterator it=mismatches.iterator(); it.hasNext();) {
    		Mismatch m = (Mismatch) it.next();
    		System.out.print(m.toString());
    	}
        globals.getComparisonResult().setSizeMismatches(mismatches);
    }

	private boolean matches() {return mismatches.size()==0;}

	public LeafList doFor(EquivRecord j) {
		if(j.isLeaf()) {
			if (j.isMatched()) {
				minWidth = minLength = Double.MAX_VALUE;
				maxWidth = maxLength = Double.MIN_VALUE;
                cktNdx = 0;
				super.doFor(j);
				checkWidthMismatch();
				checkLengthMismatch();
			}
		} else {
			super.doFor(j);
		}
		return new LeafList();
	}
	
    public HashMap doFor(Circuit c){
        HashMap result = super.doFor(c);
        cktNdx++;
        return result;
    }    
    
	public Integer doFor(NetObject n) {
		Part p = (Part) n;
		if (p instanceof Subcircuit) {
			minWidth = maxWidth = minLength = maxLength = 1;
		} else {
			globals.error(!(p instanceof Mos), "unimplemented part type");
			Mos t = (Mos) p;
			double w = t.getWidth();
			if (w<minWidth) {minWidth=w;  minWidMos=t; minWidNdx=cktNdx;}
			if (w>maxWidth) {maxWidth=w;  maxWidMos=t; maxWidNdx=cktNdx;}
			double l = t.getLength();
			if (l<minLength) {minLength=l;  minLenMos=t; minLenNdx=cktNdx;}
			if (l>maxLength) {maxLength=l;  maxLenMos=t; maxLenNdx=cktNdx;}
		}
		return CODE_NO_CHANGE;
	}

	// ------------------- intended interface ------------------------
    public static boolean doYourJob(NccGlobals globals) {
    	NccOptions options = globals.getOptions();
    	if (!options.checkSizes) return true;

    	EquivRecord parts = globals.getParts();
    	if (parts==null)  return true;
    	
    	StratCheckSizes jsf = new StratCheckSizes(globals);
    	jsf.doFor(parts);
    	jsf.summary();
    	return jsf.matches(); 
    }
}
