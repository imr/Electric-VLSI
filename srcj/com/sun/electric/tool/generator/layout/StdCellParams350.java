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
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;

/**
 *
 */
public class StdCellParams350 extends StdCellParams {

    private static final double minGateWid = 0.6;
    private static final double maxMosWidth = Double.POSITIVE_INFINITY;
	private static final double m1OverhangsDiffCont = 0.15;
	private static final double m1Space = 0.45;


    private TechType tech;

	private static void error(boolean pred, String msg) {
		Job.error(pred, msg);
	}

    public static StdCellParams invParams(Technology technology) {
        StdCellParams stdCell = new StdCellParams350(technology);
        stdCell.setSizeQuantizationError(0);
        stdCell.setMaxMosWidth(1000);
        stdCell.setVddY(5.5);
        stdCell.setGndY(-4.1);
        stdCell.setPmosWellHeight(6.4);
        stdCell.setNmosWellHeight(5.0);
//        stdCell.setVddExportName("VDD");
//        stdCell.setGndExportName("VSS");
        stdCell.setSimpleName(true);
        stdCell.enableNCC("purpleFour");
        return stdCell;
    }

    private StdCellParams350(Technology tech) {
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

	/** Wire pmos or nmos to vdd or gnd, respectively.  Add an export
	 * if there is none. */
	public void wireVddGnd(FoldedMos[] moss, SelectSrcDrn select, Cell p) {
		FoldedMos mos = moss[0];

		PortInst leftDiff = mos.getSrcDrn(0);
		Cell f = leftDiff.getNodeInst().getParent();
		double busWid = mos instanceof FoldedPmos ? getVddWidth() : getGndWidth();
		double busY = mos instanceof FoldedPmos ? getVddY() : getGndY();
		TrackRouter net = new TrackRouterH(tech.m1(), busWid, busY, tech, p);

		String exportNm = mos instanceof FoldedPmos ? getVddExportName() : getGndExportName();
		if (f.findPortProto(exportNm)==null) {
			// The export doesn't yet exist.  Create and export a metal2 pin
			// aligned with the first diffusion.
			double x = leftDiff.getBounds().getCenterX();
			NodeInst pinProt = LayoutLib.newNodeInst(tech.m1pin(), x,
			                                         busY, LayoutLib.DEF_SIZE, LayoutLib.DEF_SIZE, 0, f);
			PortInst pin = pinProt.getOnlyPortInst();
			Export e = Export.newInstance(f, pin, exportNm);
			PortCharacteristic role =
				mos instanceof FoldedPmos ? getVddExportRole() : getGndExportRole();
			e.setCharacteristic(role);

			// Connect the export to itself using a standard width power or
			// ground strap.  The width of this strap serves as a hint to
			// Electric as to the width to use to connect to this export at
			// the next level up.
			LayoutLib.newArcInst(tech.m1(), busWid, pin, pin);

			net.connect(pin);
		}
		double diffY = LayoutLib.roundCenterY(leftDiff);
		double notchLoY = Math.min(busY - busWid / 2, diffY);
		double notchHiY = Math.max(busY + busWid / 2, diffY);
		PortInst lastDiff = null;
		for (int i=0; i<moss.length; i++) {
			for (int j=0; j<moss[i].nbSrcDrns(); j++) {
				if (select.connectThisOne(i, j)) {
					PortInst thisDiff = moss[i].getSrcDrn(j);
					net.connect(thisDiff);

					if (lastDiff!=null) {
						// Check to see if we just created a notch.
						double leftX = LayoutLib.roundCenterX(lastDiff);
						double rightX = LayoutLib.roundCenterX(thisDiff);
						error(leftX>rightX,
							  "wireVddGnd: trans not sorted left to right");
						double deltaX = rightX - leftX;
						if (deltaX>0
							&& deltaX < m1OverhangsDiffCont*2+m1Space) {
							// Fill notches, sigh! (This is starting to lose
							// it's novelty value.)
							//
							// Make height integral number of lambdas so
							// centerY will be on .5 lambda grid and connecting
							// to center won't generate CIF resolution errors.
							double dY = Math.ceil(notchHiY - notchLoY);
							NodeInst patchNode =
							  LayoutLib.newNodeInst(tech.m1Node(), (leftX+rightX)/2, notchLoY+dY/2,
							  deltaX, dY, 0, f);
							PortInst patch = patchNode.getOnlyPortInst();
							LayoutLib.newArcInst(tech.m1(), LayoutLib.DEF_SIZE, patch, thisDiff);
						}
					}
					lastDiff = thisDiff;
				}
			}
		}
	}

	/** essential bounds for cells with both NMOS and PMOS */
	public void addEssentialBounds(double loX, double hiX, Cell cell) {
		LayoutLib.newNodeInst(tech.essentialBounds(), loX, getGndY(), LayoutLib.DEF_SIZE,
		                      LayoutLib.DEF_SIZE, 180, cell);
		LayoutLib.newNodeInst(tech.essentialBounds(), hiX, getVddY(), LayoutLib.DEF_SIZE,
			                  LayoutLib.DEF_SIZE, 0, cell);
	}

    @Override
	public double roundGateWidth(double w) {
        return DBMath.round(Math.ceil(w*10.0)/10.0);
	}

    public double getDRCRingSpacing() {
        return 2.3;
    }


}
