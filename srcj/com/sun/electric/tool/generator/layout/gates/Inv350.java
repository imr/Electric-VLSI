/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.electric.tool.generator.layout.gates;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.extract.LayerCoverageTool;
import com.sun.electric.tool.extract.LayerCoverageTool.LCMode;
import com.sun.electric.tool.generator.layout.FoldedMos;
import com.sun.electric.tool.generator.layout.FoldedNmos;
import com.sun.electric.tool.generator.layout.FoldedPmos;
import com.sun.electric.tool.generator.layout.FoldsAndWidth;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.StdCellParams;
import com.sun.electric.tool.generator.layout.TechType;
import com.sun.electric.tool.generator.layout.TrackRouter;
import com.sun.electric.tool.generator.layout.TrackRouterH;
import java.util.Iterator;

/**
 *
 * @author dn146861
 */
public class Inv350 {

    private static final double inY = 0.0;

    private static final double p1_nd_sp = 0;
    private static final double p1m1_met_wid = 0.8;
    private static final double m1m2_m1_wid = 0.9;

    private static final double totWidN = 1.1;
    private static final double totWidP = 1.7;
    private static final String nm = "inv{lay}";

    private static final double wellOverhangDiff = 1.2;
    private static final double m1_m1_sp = 0.45;
    private static final double m1_wid = 0.5;
    private static final double pd_p1_sp = 0.4;
    private static final double nd_p1_sp = 0.4;

	private static void error(boolean pred, String msg) {
		Job.error(pred, msg);
	}

    public static Cell makePart(double sz, String threshold, StdCellParams stdCell) {
		TechType tech = stdCell.getTechType();
		sz = stdCell.roundSize(sz);
		error(!threshold.equals("") && !threshold.equals("LT")
			  && !threshold.equals("HT"),
			  "Inv: threshold not \"\", \"LT\", or \"HT\": " + threshold);
		String nm = "inv"+threshold
		            +(stdCell.getDoubleStrapGate() ? "_strap" : "");

		sz = stdCell.checkMinStrength(sz, threshold.equals("LT") ? .5 : 1, nm);

        double p1m1_wid = tech.getP1Width();
        double p1_p1_sp = tech.getP1ToP1Space();

        double exp_wid = 0.9;//4
        double in_wid = 0.8;//3
        double outHiY = inY + (in_wid/2 + m1_m1_sp + exp_wid/2);
        double outLoY = inY - (in_wid/2 + m1_m1_sp + exp_wid/2);

        // Space needed at the top of the PMOS well and bottom of MOS well.
        // We need more space if we're double strapping poly.
        double outsideSpace = stdCell.getDoubleStrapGate() ? (
          p1_nd_sp + p1m1_wid + p1_p1_sp/2
        ) : (
          wellOverhangDiff
        );

        double tranHiInnerY = wellOverhangDiff;
        double tranLoInnerY = -wellOverhangDiff;
//        double tranHiInnerY = Math.max(wellOverhangDiff, outHiY + (exp_wid/2 + m1_m1_sp));
//        double tranLoInnerY = Math.min(-wellOverhangDiff, outLoY - (exp_wid/2 + m1_m1_sp));
        double tranHiOuterY = Math.min(stdCell.getCellTop() - outsideSpace, stdCell.getVddY() - stdCell.getVddWidth()/2 - m1_m1_sp);
        double tranLoOuterY = Math.max(stdCell.getCellBot() + outsideSpace, stdCell.getGndY() + stdCell.getGndWidth()/2 + m1_m1_sp);

        // find number of folds and width of PMOS
        double spaceAvail = tranHiOuterY - tranHiInnerY;
		double lamPerSz = threshold.equals("HT") ? 3.4 : 1.7;
		double totWidP = sz * lamPerSz;
        FoldsAndWidth fwP = stdCell.calcFoldsAndWidth(spaceAvail, totWidP, 1);

        // find number of folds and width of NMOS
        spaceAvail = tranLoInnerY - tranLoOuterY;
		lamPerSz = threshold.equals("LT") ? 2.2 : 1.1;
		double totWidN = sz * lamPerSz;
        FoldsAndWidth fwN = stdCell.calcFoldsAndWidth(spaceAvail, totWidN, 1);

		// create Inverter Part
		Cell inv = stdCell.findPart(nm, sz);
		if (inv!=null)  return inv;
		inv = stdCell.newPart(nm, sz);

        // leave vertical m1 track for in
        double inX = m1_m1_sp/2 + m1m2_m1_wid/2;
        Export eIn = LayoutLib.newExport(inv, "in", PortCharacteristic.IN, tech.m1(),
                            exp_wid, inX, inY);

        double mosX = tech.getDiffContWidth()/2 + tech.getSelectSurroundDiffInTrans() + tech.getSelectSpacingRule()/2;
        double nmosY = tranLoInnerY - fwN.physWid / 2;
        FoldedMos nmos = new FoldedNmos(mosX, nmosY, fwN.nbFolds, 1,
                                        fwN.gateWid, null, 'C', inv, tech);
        double pmosY = tranHiInnerY + fwP.physWid / 2;
        FoldedMos pmos = new FoldedPmos(mosX, pmosY, fwP.nbFolds, 1,
                                        fwP.gateWid, null, 'C', inv, tech);

        // inverter output:  m1_wid/2 + m1_m1_sp + m1_wid/2
        double rightestGateX = Math.max(
                nmos.getGate(nmos.nbGates() - 1, 'T').getCenter().getX(),
                pmos.getGate(pmos.nbGates() - 1, 'B').getCenter().getX());
        double outX = rightestGateX + p1m1_met_wid/2 + m1_m1_sp + exp_wid/2;
//        double outX = StdCellParams.getRightDiffX(nmos, pmos) + m1_wid/2 + m1_m1_sp + m1_wid/2;
        double outY = 0;
        Export eOut = LayoutLib.newExport(inv, "out", PortCharacteristic.OUT,
                            tech.m1(), exp_wid, outX, outY);

        // create vdd and gnd exports and connect to MOS source/drains
        stdCell.wireVddGnd(nmos, StdCellParams.EVEN, inv);
        stdCell.wireVddGnd(pmos, StdCellParams.EVEN, inv);

        // Connect up input. Do PMOS gates first because PMOS gate spacing
        // is a valid spacing for p1m1 vias even for small strengths.
        TrackRouter in = new TrackRouterH(tech.p1(), tech.getP1M1Width(), inY, tech, inv);
        in.connect(eIn);
        for (int i=0; i<pmos.nbGates(); i++)  in.connect(pmos.getGate(i, 'B'));
        for (int i=0; i<nmos.nbGates(); i++)  in.connect(nmos.getGate(i, 'T'));

//        if (stdCell.getDoubleStrapGate()) {
//            // Connect gates using metal1 along bottom of cell
//            double gndBot = stdCell.getGndY() - stdCell.getGndWidth() / 2;
//            double inLoFromGnd = gndBot -m1_m1_sp -m1_wid/2;
//            double nmosBot = nmosY - fwN.physWid / 2;
//            double inLoFromMos = nmosBot -nd_p1_sp - p1m1_wid/2;
//            double inLoY = Math.min(inLoFromGnd, inLoFromMos);
//
//            TrackRouter inLo = new TrackRouterH(tech.m1(), 3, inLoY, tech, inv);
//            inLo.connect(eIn);
//            for (int i = 0; i < nmos.nbGates(); i++) {
//                inLo.connect(nmos.getGate(i, 'B'));
//            }
//
//            // Connect gates using metal1 along top of cell
//            double vddTop = stdCell.getVddY() + stdCell.getVddWidth() / 2;
//            double inHiFromVdd = vddTop +m1_m1_sp + m1_wid/2;
//            double pmosTop = pmosY + fwP.physWid / 2;
//            double inHiFromMos = pmosTop +pd_p1_sp + p1m1_wid/2;
//            double inHiY = Math.max(inHiFromVdd, inHiFromMos);
//
//            TrackRouter inHi = new TrackRouterH(tech.m1(), 3, inHiY, tech, inv);
//            inHi.connect(eIn);
//            for (int i=0; i<pmos.nbGates(); i++) {
//                inHi.connect(pmos.getGate(i, 'T'));
//            }
//        }

        // connect up output
        TrackRouter outH = new TrackRouterH(tech.m1(), exp_wid, outY, tech, inv);
        TrackRouter outHi = outH;
//        TrackRouter outHi = new TrackRouterH(tech.m1(), exp_wid, outHiY, tech, inv);
//        outHi.setShareableViaDist(0);
        outHi.connect(eOut);
        for (int i=1; i<pmos.nbSrcDrns(); i += 2) {
            outHi.connect(pmos.getSrcDrn(i));
        }

        TrackRouter outLo = outH;
//        TrackRouter outLo = new TrackRouterH(tech.m1(), exp_wid, outLoY, tech, inv);
        outLo.connect(eOut);
        for (int i = 1; i < nmos.nbSrcDrns(); i += 2) {
            outLo.connect(nmos.getSrcDrn(i));
        }

        // add wells
        double wellMinX = 0;
        double wellMaxX = outX + m1_wid/2 + m1_m1_sp/2;
        stdCell.addNmosWell(wellMinX, wellMaxX, inv);
        stdCell.addPmosWell(wellMinX, wellMaxX, inv);

        double boundsLeftX = 0;
        double boundsRightX = outX + exp_wid/2 + m1_m1_sp/2;

        EPoint gndLeft = EPoint.fromLambda(boundsLeftX, stdCell.getGndY());
        EPoint gndRight = EPoint.fromLambda(boundsRightX, stdCell.getGndY());
        EPoint vddLeft = EPoint.fromLambda(boundsLeftX, stdCell.getVddY());
        EPoint vddRight = EPoint.fromLambda(boundsRightX, stdCell.getVddY());
        NodeInst gndPinLeft = NodeInst.newInstance(tech.m1pin(), gndLeft, 0, 0, inv);
        NodeInst gndPinRight = NodeInst.newInstance(tech.m1pin(), gndRight, 0, 0, inv);
        NodeInst vddPinLeft = NodeInst.newInstance(tech.m1pin(), vddLeft, 0, 0, inv);
        NodeInst vddPinRight = NodeInst.newInstance(tech.m1pin(), vddRight, 0, 0, inv);
        ArcInst gndArcLeft = ArcInst.makeInstance(tech.m1(), gndPinLeft.getOnlyPortInst(), inv.findExport("gnd").getOriginalPort());
        gndArcLeft.setLambdaBaseWidth(stdCell.getGndWidth());
        gndArcLeft.setTailExtended(false);
        gndArcLeft.setHeadExtended(false);
        ArcInst gndArcRight = ArcInst.makeInstance(tech.m1(), gndPinRight.getOnlyPortInst(), inv.findExport("gnd").getOriginalPort());
        gndArcRight.setLambdaBaseWidth(stdCell.getGndWidth());
        gndArcRight.setTailExtended(false);
        gndArcRight.setHeadExtended(false);
        ArcInst vddArcLeft = ArcInst.makeInstance(tech.m1(), vddPinLeft.getOnlyPortInst(), inv.findExport("vdd").getOriginalPort());
        vddArcLeft.setLambdaBaseWidth(stdCell.getVddWidth());
        vddArcLeft.setTailExtended(false);
        vddArcLeft.setHeadExtended(false);
        ArcInst vddArcRight = ArcInst.makeInstance(tech.m1(), vddPinRight.getOnlyPortInst(), inv.findExport("vdd").getOriginalPort());
        vddArcRight.setLambdaBaseWidth(stdCell.getVddWidth());
        vddArcRight.setTailExtended(false);
        vddArcRight.setHeadExtended(false);

        NodeInst deviceMark = NodeInst.newInstance(tech.getTechnology().findNodeProto("DeviceMark-Node"),
                EPoint.fromLambda((boundsRightX + boundsLeftX)/2, (stdCell.getVddY() + stdCell.getGndY())/2),
                (boundsRightX - boundsLeftX), (stdCell.getVddY() - stdCell.getGndY()), inv);

        // add essential bounds
        stdCell.addEssentialBounds(boundsLeftX, boundsRightX, inv);

        for (Iterator<Export> it = inv.getExports(); it.hasNext(); ) {
            Export e = it.next();
            TextDescriptor td = e.getTextDescriptor(Export.EXPORT_NAME).withRelSize(0.5);
            e.setTextDescriptor(Export.EXPORT_NAME, td);
        }

        LayerCoverageTool.LayerCoveragePreferences lcp = new LayerCoverageTool.LayerCoveragePreferences(true);
        LayerCoverageTool.layerCoverageCommand(LayerCoverageTool.LCMode.IMPLANT, GeometryHandler.GHMode.ALGO_SWEEP, inv, false, lcp);
        
		// perform Network Consistency Check
		stdCell.doNCC(inv, nm+"{sch}");

        return inv;
    }
}
