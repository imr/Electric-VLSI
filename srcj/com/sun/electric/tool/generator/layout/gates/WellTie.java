/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WellTie.java
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
package com.sun.electric.tool.generator.layout.gates;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.StdCellParams;
import com.sun.electric.tool.generator.layout.Tech;
import com.sun.electric.tool.generator.layout.TrackRouterH;

// Well Ties now serve two roles. 
//
// 1) First, they are inserted between abutted cells. In this case the
// cells must be moved apart to make room for the tie. The ties are
// chosen to be as narrow as possible because they increase the GasP
// cell width.
//
// 2) Second, they are inserted in half height gaps. In this case the
// ties are also serving as well patches. These well ties are made as
// wide as the gap. A contact is included on the left and right of the
// cell because additional contacts can decrease the width of the GasP
// cell by reducing the need to insert
//
// Well contacts are allocated on a pitch: WELL_CONT_PITCH that
// guarantees that there are no select errors from adjacent
// contacts. There are two reasons well contacts can be adjacent.
// First, a single WellTie can have multiple contacts.  Second, when
// cells are concatenated, a well tie from one cell might be adjacent
// to a well tie from another cell.
public class WellTie {
	private static final double DEF_SIZE = LayoutLib.DEF_SIZE;
	
	// diff_cont_width + select_surround + select_space
	private static final double WELL_CONT_PITCH = 5 + 4 + 2;
	
	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}
	
	/** The maximum distance from an internal well contact to the right
		edge of the WellTie. */
	public static double edgeToContDist() {return WELL_CONT_PITCH*1.5;}
	
	/** Create a well tie
	 * @param nmos should this well tie include nmos well contacts
	 * @param pmos should this well tie include pmos well contacts
	 * @param wid desired width of well tie. 0 means produce minimum
	 * width contact */
	public static Cell makePart(boolean nmos, boolean
								pmos, double wid, StdCellParams stdCell) {
		if (wid==0) wid=WELL_CONT_PITCH;
		
		String nm = (nmos?"Nmos":"") + (pmos?"Pmos":"") + "WellTie";
		nm = stdCell.parameterizedName(nm)+"_W"+wid+"{lay}";
		Cell tie = stdCell.findPart(nm);
		if (tie!=null) return tie;
		tie = stdCell.newPart(nm);
		
		// Leave half a pitch distance from any contact to the right and
		// left edges of WellTie.
		double leftWellContX = WELL_CONT_PITCH/2;
		double rightWellContX = wid - WELL_CONT_PITCH/2;
		
		// Well width must be at least 12 to avoid DRC errors This cell is
		// one of the rare cases where the cell's essential bounds are
		// narrower than the well
		//
		// On second thought, I just noticed that Electric's well contact
		// is asymmetric! It sticks out an extra .5 lambda on top and
		// right. Jon Lexau asked me to completely cover it.
		double wellMinX = leftWellContX - 6.5;
		double wellMaxX = Math.max(leftWellContX, rightWellContX) + 6.5;
		
		// add well contacts
		if (wid<WELL_CONT_PITCH) {
			// This well tie has no room for a contact. It can only being
			// used as a well patch.
		} else {
			if (nmos) {
				// export
				String portNm = stdCell.getNmosWellTieName();
				Export e =
					LayoutLib.newExport(tie, portNm,
										stdCell.getNmosWellTieRole(), Tech.m2,
										4, leftWellContX,
										stdCell.getNmosWellTieY());
				// left well contact
				PortInst left =
					LayoutLib.newNodeInst(Tech.pwm1, leftWellContX, stdCell.getNmosWellTieY(), 
					                      DEF_SIZE, 
					                      DEF_SIZE, 
					                      0, tie).getOnlyPortInst();
				// connect them
				TrackRouterH tr = 
					new TrackRouterH(Tech.m2,
									 stdCell.getNmosWellTieWidth(),
									 stdCell.getNmosWellTieY(), tie);
				tr.connect(e);
				tr.connect(left);
				
				// Insert right contact if there's room. 
				if (wid>=WELL_CONT_PITCH*2) {
					PortInst right =
						LayoutLib.newNodeInst(Tech.pwm1, rightWellContX, stdCell.getNmosWellTieY(), 
						                      DEF_SIZE, 
						                      DEF_SIZE, 0, tie
											  ).getOnlyPortInst();
					tr.connect(right);
				}
			}
			if (pmos) {
				// export
				String portNm = stdCell.getPmosWellTieName();
				Export e =
					LayoutLib.newExport(tie, portNm,
										stdCell.getPmosWellTieRole(), Tech.m2,
										4, leftWellContX,
										stdCell.getPmosWellTieY());
				TrackRouterH tr =
					new TrackRouterH(Tech.m2,
									 stdCell.getPmosWellTieWidth(),
									 stdCell.getPmosWellTieY(), tie);
				// left well contact
				PortInst left =
					LayoutLib.newNodeInst(Tech.nwm1, leftWellContX, stdCell.getPmosWellTieY(), 
					                      DEF_SIZE,
										  DEF_SIZE, 0, tie
										  ).getOnlyPortInst();
				// connect them
				tr.connect(e);
				tr.connect(left);
				
				// Insert right contact if there's room.
				if (wid>=WELL_CONT_PITCH*2) {
					PortInst right =
						LayoutLib.newNodeInst(Tech.nwm1, rightWellContX, stdCell.getPmosWellTieY(), 
						                      DEF_SIZE,
											  DEF_SIZE, 0, tie
											  ).getOnlyPortInst();
					tr.connect(right);
				}
			}
		}
		
		// add wells
		if (nmos)  stdCell.addNmosWell(wellMinX, wellMaxX, tie);
		if (pmos)  stdCell.addPmosWell(wellMinX, wellMaxX, tie);
		
		// add essential bounds
		if (nmos) {
			if (pmos) {
				stdCell.addEssentialBounds(0, wid, tie);
			} else {
				stdCell.addNstackEssentialBounds(0, wid, tie);
			}
		} else {
			error(!pmos, "WellTie must be for at least one well");
			stdCell.addPstackEssentialBounds(0, wid, tie);
		}
		
		return tie;
	}
}
