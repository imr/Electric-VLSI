/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Debug.java
 * Written by Team 5: Andreas Wagner, Thomas Hauck, Philippe Bartscherer
 * 
 * This code has been developed at the Karlsruhe Institute of Technology (KIT), Germany, 
 * as part of the course "Multicore Programming in Practice: Tools, Models, and Languages".
 * Contact instructor: Dr. Victor Pankratius (pankratius@ipd.uka.de)
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.placement.forceDirected1;

import com.sun.electric.tool.placement.forceDirected1.metric.AbstractMetric;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Stack;


/**
 * Placement algorithm to do Force-Directed placement.
 */
public class Debug {
	// data structures
	private ArrayList<String> out = new ArrayList<String>();
	private Stack<Long> tick = new Stack<Long>();

	// output format
	private DecimalFormat df = new DecimalFormat("###,###.###");

	/**
	 * clear all outputs and reset debug object.
	 */
	public void clear() {
		out.clear();
		tick.clear();
	}

	/**
	 * measure time. Nested tick(), tack() allowed !
	 */
	public void tick() {
		long now = System.currentTimeMillis();
		tick.push(new Long(now));
	}
	public void tack() {
		tack("Time");
	}
	public void tack(String label) {
		double t = System.currentTimeMillis() - tick.pop().longValue();
		out.add(align(label) + df.format(t/1000) + " s");
	}
	public void tack(String label, boolean implicitTick) {
		tack(label);
		if(implicitTick) tick();
	}

	public String last() {
		return out.remove(out.size()-1);
	}
	
	/**
	 * primitives
	 */
	public void println(String str) {
		out.add(str);
	}
	public void println(String label, int value) {
		out.add(align(label) + value);
	}
	public void println(String label, long value) {
		out.add(align(label) + value);
	}
	public void println(String label, double value) {
		out.add(align(label) + df.format(value));
	}
	public void println(String label, String str) {
		out.add(align(label) + str);
	}
	public void println(AbstractMetric metric) {
		println(metric.getMetricName(), metric.compute());
	}

	/**
	 * show all stored messages
	 */
	public void flush() {
		flush("");
	}
	public void flush(String str) {
		if(out.size()==0) return;
		if(str.length()>0)
			System.out.println(str);
		for (String s : out)
			System.out.println(s);
		clear();
	}

	//--- private -------------------------------------------------------------

	private String align(String label) {
		label+=":";
		if (label.length() < 8)
			label += "\t ";
		return label+"\t ";
	}
}
