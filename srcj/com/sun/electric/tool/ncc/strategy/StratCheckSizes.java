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

/** StratFrontier finds all non-retired EquivRecords */

package com.sun.electric.tool.ncc.strategy;

import java.util.HashMap;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.basic.Messenger;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.jemNets.*;
import com.sun.electric.tool.ncc.trees.*;
import com.sun.electric.tool.ncc.lists.*;

public class StratCheckSizes extends Strategy {
	private final double absTol;
	private final double relTol;
	private double minWidth, maxWidth, minLength, maxLength;
	private Transistor minWidMos, maxWidMos, minLenMos, maxLenMos;
	private boolean matches = true;
	
    private StratCheckSizes(NccGlobals globals) {
    	super(globals);
    	NccOptions options = globals.getOptions();
    	absTol = options.absoluteSizeTolerance;
    	relTol = options.relativeSizeTolerance / 100; // convert from percent
    }
    
    private void checkWidthMismatch() {
		double relWidErr = (maxWidth - minWidth) / minWidth;
		double absWidErr = maxWidth - minWidth;
		if (absWidErr>absTol && relWidErr>relTol) {
			System.out.println("MOS widths don't match. "+
							   " relativeError="+relWidErr+" absoluteError="+absWidErr);
			System.out.println("   minimumWidth="+minWidth+" maximumWidth="+maxWidth);
			System.out.println("   narrowMOS="+minWidMos.toString());
			System.out.println("   wideMOS="+maxWidMos.toString());
			matches = false;
		}
    }
    private void checkLengthMismatch() {
		double relLenErr = (maxLength - minLength) / minLength;
    	double absLenErr = maxLength - minLength;
    	if (absLenErr>absTol && relLenErr>relTol) {
			System.out.println("MOS lengths don't match. "+
							   " relativeError="+relLenErr+" absoluteError="+absLenErr);
			System.out.println("   minimumLength="+minLength+" maximumLength="+maxLength);
			System.out.println("   shortMOS="+minLenMos.toString());
			System.out.println("   longMOS="+maxLenMos.toString());
			matches = false;
    	}
    }

	public LeafList doFor(EquivRecord j) {
		if(j.isLeaf()) {
			if (j.isRetired()) {
				minWidth = minLength = Double.MAX_VALUE;
				maxWidth = maxLength = Double.MIN_VALUE;
				super.doFor(j);
				checkWidthMismatch();
				checkLengthMismatch();
			}
		} else {
			super.doFor(j);
		}
		return new LeafList();
	}
	
	public Integer doFor(NetObject n) {
		Part p = (Part) n;
		globals.error(!(p instanceof Transistor), "unimplemented part type");
		Transistor t = (Transistor) p;
		double w = t.getWidth();
		if (w<minWidth) {
			minWidth = w;
			minWidMos = t;
		}
		if (w>maxWidth) {
			maxWidth = w;
			maxWidMos = t;
		}
		double l = t.getLength();
		if (l<minLength) {
			minLength = l;
			minLenMos = t;
		}
		if (l>maxLength) {
			maxLength = l;
			maxLenMos = t;
		}
		return CODE_NO_CHANGE;
	}
	
	private boolean matches() {return matches;}

	// ------------------- intended interface ------------------------
    public static boolean doYourJob(NccGlobals globals) {
    	NccOptions options = globals.getOptions();
    	if (!options.checkSizes) return true;

    	EquivRecord parts = globals.getParts();
    	if (parts==null)  return true;
    	
    	StratCheckSizes jsf = new StratCheckSizes(globals);
    	jsf.doFor(parts);
    	return jsf.matches(); 
    }
}
