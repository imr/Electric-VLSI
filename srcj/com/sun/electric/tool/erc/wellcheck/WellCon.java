/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WellCon.java
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
package com.sun.electric.tool.erc.wellcheck;

import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveNode.Function;

/**
 * @author fschmidt
 * 
 */
public class WellCon {

	private Point2D ctr;
	private int netNum;
	private NetValues wellNum = null;
	private boolean onProperRail;
	private boolean onRail;
	private PrimitiveNode.Function fun;
	private NodeInst ni;
	private AtomicBoolean marked = new AtomicBoolean(false);

	/**
	 * @param ctr
	 * @param netNum
	 * @param wellNum
	 * @param onProperRail
	 * @param onRail
	 * @param fun
	 * @param ni
	 */
	public WellCon(Point2D ctr, int netNum, NetValues wellNum, boolean onProperRail, boolean onRail,
			Function fun, NodeInst ni) {
		super();
		this.ctr = ctr;
		this.netNum = netNum;
		this.wellNum = wellNum;
		this.onProperRail = onProperRail;
		this.onRail = onRail;
		this.fun = fun;
		this.ni = ni;
	}

	public Point2D getCtr() {
		return ctr;
	}

	public void setCtr(Point2D ctr) {
		this.ctr = ctr;
	}

	public int getNetNum() {
		return netNum;
	}

	public void setNetNum(int netNum) {
		this.netNum = netNum;
	}

	public NetValues getWellNum() {
		return wellNum;
	}

	public void setWellNum(NetValues wellNum) {
		this.wellNum = wellNum;
	}

	public boolean isOnProperRail() {
		return onProperRail;
	}

	public void setOnProperRail(boolean onProperRail) {
		this.onProperRail = onProperRail;
	}

	public boolean isOnRail() {
		return onRail;
	}

	public void setOnRail(boolean onRail) {
		this.onRail = onRail;
	}

	public PrimitiveNode.Function getFun() {
		return fun;
	}

	public void setFun(PrimitiveNode.Function fun) {
		this.fun = fun;
	}

	public NodeInst getNi() {
		return ni;
	}

	public void setNi(NodeInst ni) {
		this.ni = ni;
	}

	public void setMarked(AtomicBoolean marked) {
		this.marked = marked;
	}

	public AtomicBoolean getMarked() {
		return marked;
	}
	
	public static class WellConComparator implements Comparator<WellCon> {

		private WellCon base;

		public WellConComparator(WellCon base) {
			this.base = base;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object,
		 * java.lang.Object)
		 */
		public int compare(WellCon o1, WellCon o2) {
			Double o1Dist = o1.getCtr().distance(base.getCtr());
			Double o2Dist = o2.getCtr().distance(base.getCtr());

			return Double.compare(o1Dist, o2Dist);
		}

	}

}
