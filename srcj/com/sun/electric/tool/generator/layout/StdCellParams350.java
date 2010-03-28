/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StdCellParams350.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.generator.layout;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.technology.Technology;

/**
 *
 */
public class StdCellParams350 extends StdCellParams {

    private static final double minGateWid = 0.6;
    private static final double maxMosWidth = Double.POSITIVE_INFINITY;

    private TechType tech;

    public StdCellParams350(Technology tech) {
        super(tech);
        this.tech = getTechType();
        setGndWidth(1.4);
        setVddWidth(1.4);
    }

	/** Calculate the number of folds and the width of a MOS
	 * transistor. Given that there is a limited physical height into
	 * which a MOS transistor must fit, divide the total required width:
	 * totWid into fingers.  Each finger must have width less than or
	 * equal to spaceAvailWid.
	 *
	 * <p> If it is possible, allocate an even number of fingers so that
	 * the left most and right most diffusion contacts may be connected
	 * to power or ground to reducing the capacitance of the inner
	 * switching diffusion contacts.
	 * @param spaceAvailWid the height in the standard cell that is
	 * available for the diffusion of the MOS transistor.
	 * @param totWid the total electrical width required.
	 * @param groupSz This method creates fingers in multiples of
	 * groupSz. For example, if groupSz is 2, then only even numbers of
	 * fingers are created. This is needed when one FoldedMos is
	 * actually going to be wired up as 2 identical, independent
	 * transistors, for example the 2 PMOS pullups for a 2-input NAND
	 * gate. */
	public FoldsAndWidth calcFoldsAndWidth(
		double spaceAvailWid,
		double totWid,
		int groupSz) {
		if (totWid == 0)
			return null;
		double maxAvailWid = Math.min(spaceAvailWid, maxMosWidth);
		int nbGroups = calcNbGroups(maxAvailWid, totWid, groupSz);

		double gateWid = roundGateWidth(totWid / groupSz / nbGroups);

		// If we're unfortunate, rounding up gate width causes gate's width
		// to exceed space available.
		if (gateWid > maxAvailWid) {
			nbGroups = calcNbGroups(maxAvailWid - .5, totWid, groupSz);
			gateWid = roundGateWidth(totWid / groupSz / nbGroups);
		}

		double physWid = Math.max(tech.getDiffContWidth(), gateWid);
		if (gateWid < minGateWid)
			return null;
		return new FoldsAndWidth(nbGroups * groupSz, gateWid, physWid);
	}

	private int calcNbGroups(double maxAvailWid, double totWid, int groupSz) {
		int nbGroups = (int) Math.ceil(totWid / maxAvailWid / groupSz);

		// If groupSz is even then we always create an even number of
		// fingers and we don't have to add more fingers to reduce
		// diffusion capacitance.
		if (groupSz % 2 == 0)
			return nbGroups;

		// If nbGroups is even then we always create an even number of
		// fingers and we don't have to add more fingers to reduce
		// diffusion capacitance
		if (nbGroups % 2 == 0)
			return nbGroups;

        // if totWid is less than maxAvailWid and groupSz is 1, just
        // use 1 gate (no fingers)
        if ((totWid < maxAvailWid) && (groupSz == 1))
            return 1;

		// try adding one more group to get an even number of fingers
		int roundupGroups = nbGroups + 1;
		double wid = totWid / groupSz / roundupGroups;

		// Don't fold if gate width is less than width of diffusion
		// contact.
		if (wid >= tech.getDiffContWidth())
			nbGroups = roundupGroups;

		return nbGroups;
	}

    @Override
	public double roundGateWidth(double w) {
        return DBMath.gridToLambda(DBMath.lambdaToSizeGrid(w));
	}
}
