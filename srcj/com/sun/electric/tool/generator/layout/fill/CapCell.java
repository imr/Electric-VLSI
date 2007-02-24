package com.sun.electric.tool.generator.layout.fill;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.tool.generator.layout.Tech;
import com.sun.electric.tool.generator.layout.StdCellParams;
import com.sun.electric.tool.generator.layout.LayoutLib;

// ------------------------------------ CapCell -------------------------------
/** CapCell is built assuming horizontal metal 1 straps. I deal with the
 *  possible 90 degree rotation by creating a NodeInst of this Cell rotated
 *  by -90 degrees. */
public abstract class CapCell
{
	protected int gndNum, vddNum;
	protected Cell cell;

    abstract public int numVdd();
	abstract public int numGnd();
	abstract public double getVddWidth();
	abstract public double getGndWidth();
	public Cell getCell() {return cell;}
}


// ------------------------------------ CapCellMosis -------------------------------
class CapCellMosis extends CapCell{
	/** All the fields in ProtoPlan assume that metal1 runs horizontally
	 *  since that is how we build CapCell */
	private static class ProtoPlan{
		private final double MAX_MOS_WIDTH = 40;
		private final double SEL_WIDTH_OF_NDM1 =
			Tech.getDiffContWidth() +
		    Tech.selectSurroundDiffInActiveContact()*2;
		private final double SEL_TO_MOS =
			Tech.selectSurroundDiffAlongGateInTrans();

		public final double protoWidth, protoHeight;

		public final double vddWidth = 9;
		public final double gndWidth = 4;
		public final double vddGndSpace = 3; // 4 in CMOS90 -> to change

		public final double gateWidth;
		public final int numMosX;
		public final double mosPitchX;
		public final double leftWellContX;

		public final double gateLength;
		public final int numMosY;
		public final double mosPitchY;
		public final double botWellContY;

		public ProtoPlan(CapFloorplan instPlan) {
			protoWidth =
				instPlan.horizontal ? instPlan.cellWidth : instPlan.cellHeight;
			protoHeight =
				instPlan.horizontal ? instPlan.cellHeight : instPlan.cellWidth;

			// compute number of MOS's bottom to top
			mosPitchY = gndWidth + 2*vddGndSpace + vddWidth;
			gateLength = mosPitchY - gndWidth - 2;
			numMosY = (int) Math.floor((protoHeight-Tech.getWellWidth())/mosPitchY);
			botWellContY = - numMosY * mosPitchY / 2;

			// min distance from left Cell edge to center of leftmost diffusion
			// contact.
			double cellEdgeToDiffContCenter =
				Tech.getWellSurroundDiff() + Tech.getDiffContWidth()/2;
			// min distance from left Cell Edge to center of leftmost poly
			// contact.
			double polyContWidth = Math.floor(gateLength / Tech.getP1M1Width()) *
			                       Tech.getP1M1Width();
			double cellEdgeToPolyContCenter =
				Tech.getP1ToP1Space()/2 + polyContWidth/2;
			// diffusion and poly contact centers line up
			double cellEdgeToContCenter = Math.max(cellEdgeToDiffContCenter,
					                               cellEdgeToPolyContCenter);

			// compute number of MOS's left to right
			//double availForCap = protoWidth - 2*(SEL_TO_CELL_EDGE + SEL_WIDTH_OF_NDM1/2);
			double availForCap = protoWidth - 2*cellEdgeToContCenter;
			double numMosD = availForCap /
							 (MAX_MOS_WIDTH + SEL_WIDTH_OF_NDM1 + 2*SEL_TO_MOS);
			numMosX = (int) Math.ceil(numMosD);
			double mosWidth1 = availForCap/numMosX - SEL_WIDTH_OF_NDM1 - 2*SEL_TO_MOS;
			// round down mos Width to integral number of lambdas
			gateWidth = Math.floor(mosWidth1);
			mosPitchX = gateWidth + SEL_WIDTH_OF_NDM1 + 2*SEL_TO_MOS;
			leftWellContX = - numMosX * mosPitchX / 2;

		}
	}

	private final double POLY_CONT_WIDTH = 10;
	private final String TOP_DIFF = "n-trans-diff-top";
	private final String BOT_DIFF = "n-trans-diff-bottom";
	private final String LEFT_POLY = "n-trans-poly-left";
	private final String RIGHT_POLY = "n-trans-poly-right";
	private final ProtoPlan plan;

	/** Interleave well contacts with diffusion contacts left to right. Begin
	 *  and end with well contacts */
	private PortInst[] diffCont(double y, ProtoPlan plan, Cell cell,
	                            StdCellParams stdCell) {
		PortInst[] conts = new PortInst[plan.numMosX];
		double x = - plan.numMosX * plan.mosPitchX / 2;
		PortInst wellCont = LayoutLib.newNodeInst(Tech.pwm1(), x, y, G.DEF_SIZE,
										 		  G.DEF_SIZE, 0, cell
										 		  ).getOnlyPortInst();
		Export e = Export.newInstance(cell, wellCont,
		                              stdCell.getGndExportName()+"_"+gndNum++);
		e.setCharacteristic(stdCell.getGndExportRole());

		for (int i=0; i<plan.numMosX; i++) {
			x += plan.mosPitchX/2;
			conts[i] = LayoutLib.newNodeInst(Tech.ndm1(), x, y, plan.gateWidth, 5,
											 0, cell).getOnlyPortInst();
			LayoutLib.newArcInst(Tech.m1(), plan.gndWidth, wellCont, conts[i]);
			x += plan.mosPitchX/2;
			wellCont = LayoutLib.newNodeInst(Tech.pwm1(), x, y, G.DEF_SIZE,
											 G.DEF_SIZE, 0, cell
											 ).getOnlyPortInst();
			LayoutLib.newArcInst(Tech.m1(), plan.gndWidth, conts[i], wellCont);
		}

		// bring metal to cell left and right edges to prevent notches
		x = -plan.protoWidth/2 + plan.gndWidth/2;
		PortInst pi;
		pi = LayoutLib.newNodeInst(Tech.m1pin(), x, y, G.DEF_SIZE, G.DEF_SIZE, 0,
		                           cell).getOnlyPortInst();
		LayoutLib.newArcInst(Tech.m1(), plan.gndWidth, pi, conts[0]);

		x = plan.protoWidth/2 - plan.gndWidth/2;
		pi = LayoutLib.newNodeInst(Tech.m1pin(), x, y, G.DEF_SIZE, G.DEF_SIZE, 0,
		                           cell).getOnlyPortInst();
		LayoutLib.newArcInst(Tech.m1(), plan.gndWidth, pi, conts[conts.length-1]);

		return conts;
	}

	/** Interleave gate contacts and MOS transistors left to right. Begin
	 *  and end with gate contacts. */
	private void mos(PortInst[] botDiffs, PortInst[] topDiffs, double y,
					 ProtoPlan plan, Cell cell, StdCellParams stdCell) {
		final double POLY_CONT_HEIGHT = plan.vddWidth + 1;
		double x = plan.leftWellContX;
		PortInst poly = LayoutLib.newNodeInst(Tech.p1m1(), x, y, POLY_CONT_WIDTH,
											  POLY_CONT_HEIGHT, 0, cell
											  ).getOnlyPortInst();
		PortInst leftCont = poly;
		Export e = Export.newInstance(cell, poly,
		                              stdCell.getVddExportName()+"_"+vddNum++);
		e.setCharacteristic(stdCell.getVddExportRole());

		for (int i=0; i<plan.numMosX; i++) {
			x += plan.mosPitchX/2;
			NodeInst mos = LayoutLib.newNodeInst(Tech.nmos(), x, y, plan.gateWidth,
												 plan.gateLength, 0, cell);
			G.noExtendArc(Tech.p1(), POLY_CONT_HEIGHT, poly,
						  mos.findPortInst(LEFT_POLY));
			x += plan.mosPitchX/2;
			PortInst polyR = LayoutLib.newNodeInst(Tech.p1m1(), x, y,
												   POLY_CONT_WIDTH,
										 		   POLY_CONT_HEIGHT, 0, cell
										 		   ).getOnlyPortInst();
			G.noExtendArc(Tech.m1(), plan.vddWidth, poly, polyR);
			poly = polyR;
			G.noExtendArc(Tech.p1(), POLY_CONT_HEIGHT, poly,
						  mos.findPortInst(RIGHT_POLY));
			botDiffs[i] = mos.findPortInst(BOT_DIFF);
			topDiffs[i] = mos.findPortInst(TOP_DIFF);
		}
		PortInst rightCont = poly;

		// bring metal to cell left and right edges to prevent notches
		x = -plan.protoWidth/2 + plan.vddWidth/2;
		PortInst pi;
		pi = LayoutLib.newNodeInst(Tech.m1pin(), x, y, G.DEF_SIZE, G.DEF_SIZE, 0,
								   cell).getOnlyPortInst();
		LayoutLib.newArcInst(Tech.m1(), plan.vddWidth, pi, leftCont);

		x = plan.protoWidth/2 - plan.vddWidth/2;
		pi = LayoutLib.newNodeInst(Tech.m1pin(), x, y, G.DEF_SIZE, G.DEF_SIZE, 0,
								   cell).getOnlyPortInst();
		LayoutLib.newArcInst(Tech.m1(), plan.vddWidth, pi, rightCont);

	}

	double roundToHalfLambda(double x) {
		return Math.rint(x * 2) / 2;
	}

	// The height of a MOS diff contact is 1/2 lambda. Therefore, using the
	// center for diffusion arcs always generates CIF resolution errors
	private void newDiffArc(PortInst p1, PortInst p2) {
        EPoint p1P = p1.getCenter();
		double x = p1P.getX(); // LayoutLib.roundCenterX(p1);
		double y1 = roundToHalfLambda(p1P.getY()); // LayoutLib.roundCenterY(p1));
		double y2 = roundToHalfLambda(LayoutLib.roundCenterY(p2));

		LayoutLib.newArcInst(Tech.ndiff(), LayoutLib.DEF_SIZE, p1, x, y1, p2, x, y2);
	}

	private void connectDiffs(PortInst[] a, PortInst[] b) {
		for (int i=0; i<a.length; i++) {
			//LayoutLib.newArcInst(Tech.ndiff, G.DEF_SIZE, a[i], b[i]);
			newDiffArc(a[i], b[i]);
		}
	}

	public CapCellMosis(Library lib, CapFloorplan instPlan, StdCellParams stdCell) {
		this.plan = new ProtoPlan(instPlan);
		PortInst[] botDiffs = new PortInst[plan.numMosX];
		PortInst[] topDiffs = new PortInst[plan.numMosX];

		String nameExt = stdCell.getVddExportName().equals("vdd") ? "" : "_pwr";
		cell = Cell.newInstance(lib, "fillCap"+nameExt+"{lay}");
		double y = plan.botWellContY;

		PortInst[] lastCont = diffCont(y, plan, cell, stdCell);
		for (int i=0; i<plan.numMosY; i++) {
			y += plan.mosPitchY/2;
			mos(botDiffs, topDiffs, y, plan, cell, stdCell);
			connectDiffs(lastCont, botDiffs);
			y += plan.mosPitchY/2;
			lastCont = diffCont(y, plan, cell, stdCell);
			connectDiffs(topDiffs, lastCont);
		}
		// Cover the sucker with well to eliminate notch errors
		LayoutLib.newNodeInst(Tech.pwell(), 0, 0, plan.protoWidth,
		                      plan.protoHeight, 0, cell);
	}
    public int numVdd() {return plan.numMosY;}
	public int numGnd() {return plan.numMosY+1;}
	public double getVddWidth() {return plan.vddWidth;}
	public double getGndWidth() {return plan.gndWidth;}
}

