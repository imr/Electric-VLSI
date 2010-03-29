/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.electric.tool.generator.layout.gates;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.generator.layout.FoldedMos;
import com.sun.electric.tool.generator.layout.FoldedNmos;
import com.sun.electric.tool.generator.layout.FoldedPmos;
import com.sun.electric.tool.generator.layout.FoldsAndWidth;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.StdCellParams;
import com.sun.electric.tool.generator.layout.StdCellParams350;
import com.sun.electric.tool.generator.layout.TechType;
import com.sun.electric.tool.generator.layout.TrackRouter;
import com.sun.electric.tool.generator.layout.TrackRouterH;
import java.util.Iterator;

/**
 *
 * @author dn146861
 */
public class Inv350 {

    public static void makePart(Technology technology, Library outLib) {
        StdCellParams stdCell = new StdCellParams350(technology);
        stdCell.setSizeQuantizationError(0);
        stdCell.setMaxMosWidth(1000);
        stdCell.setVddY(5.5);
        stdCell.setGndY(-4.1);
        stdCell.setNmosWellHeight(6.4);
        stdCell.setPmosWellHeight(6.4);
        stdCell.setSimpleName(true);

        final double inY = 0.0;
        final double outHiY = 1.9;//11.0;
        final double outLoY = -1.9;//-11.0;

        final double p1_nd_sp = 0;
        double p1m1_met_wid = 0.8;

        double totWidN = 1.1;
        double totWidP = 1.7;
        String nm = "inv{lay}";

        TechType tech = stdCell.getTechType();

        double wellOverhangDiff = 1.2;
        double p1m1_wid = tech.getP1Width();
        double p1_p1_sp = tech.getP1ToP1Space();
        double m1_m1_sp = 0.45;
        double m1_wid = 0.5;
        double pd_p1_sp = 0.4;

        // Space needed at the top of the PMOS well and bottom of MOS well.
        // We need more space if we're double strapping poly.
        double outsideSpace = stdCell.getDoubleStrapGate() ? (
          p1_nd_sp + p1m1_wid + p1_p1_sp/2
        ) : (
          wellOverhangDiff
        );

        // find number of folds and width of PMOS
        double spaceAvail =
            stdCell.getCellTop() - outsideSpace - wellOverhangDiff;
        FoldsAndWidth fwP = stdCell.calcFoldsAndWidth(spaceAvail, totWidP, 1);

        // find number of folds and width of NMOS
        spaceAvail = -wellOverhangDiff - (stdCell.getCellBot() + outsideSpace);
        FoldsAndWidth fwN = stdCell.calcFoldsAndWidth(spaceAvail, totWidN, 1);

        // create Inverter Part
        Cell inv = Cell.newInstance(outLib, nm);
        inv.setTechnology(tech.getTechnology());

        // leave vertical m1 track for in
        double exp_wid = 0.9;//4
        double inX = m1_m1_sp/2 + m1_wid/2;
        LayoutLib.newExport(inv, "in", PortCharacteristic.IN, tech.m1(),
                            exp_wid, inX, inY);

        double mosX = inX + m1_wid/2 + m1_m1_sp + m1_wid/2;
        double nmosY = -wellOverhangDiff - fwN.physWid / 2;
        FoldedMos nmos = new FoldedNmos(mosX, nmosY, fwN.nbFolds, 1,
                                        fwN.gateWid, inv, tech);
        double pmosY = wellOverhangDiff + fwP.physWid / 2;
        FoldedMos pmos = new FoldedPmos(mosX, pmosY, fwP.nbFolds, 1,
                                        fwP.gateWid, inv, tech);

        // inverter output:  m1_wid/2 + m1_m1_sp + m1_wid/2
        double rightestCutX = Math.max(
                nmos.getGate(nmos.nbGates() - 1, 'T').getCenter().getX(),
                pmos.getGate(nmos.nbGates() - 1, 'B').getCenter().getX());
        double outX = rightestCutX + p1m1_met_wid/2 + m1_m1_sp + exp_wid/2;
//        double outX = StdCellParams.getRightDiffX(nmos, pmos) + m1_wid/2 + m1_m1_sp + m1_wid/2;
        LayoutLib.newExport(inv, "out", PortCharacteristic.OUT,
                            tech.m1(), exp_wid, outX, 0);

        // create vdd and gnd exports and connect to MOS source/drains
        stdCell.wireVddGnd(nmos, StdCellParams.EVEN, inv);
        stdCell.wireVddGnd(pmos, StdCellParams.EVEN, inv);

        // Connect up input. Do PMOS gates first because PMOS gate spacing
        // is a valid spacing for p1m1 vias even for small strengths.
        double in_wid = 0.8;//3
        TrackRouter in = new TrackRouterH(tech.m1(), in_wid, inY, tech, inv);
        in.connect(inv.findExport("in"));
        for (int i=0; i<pmos.nbGates(); i++)  in.connect(pmos.getGate(i, 'B'));
        for (int i=0; i<nmos.nbGates(); i++)  in.connect(nmos.getGate(i, 'T'));

        if (stdCell.getDoubleStrapGate()) {
            // Connect gates using metal1 along bottom of cell
            double gndBot = stdCell.getGndY() - stdCell.getGndWidth() / 2;
            double inLoFromGnd = gndBot - 3 - 2; // -m1_m1_sp -m1_wid/2
            double nmosBot = nmosY - fwN.physWid / 2;
            double inLoFromMos = nmosBot - 2 - 2.5; // -nd_p1_sp - p1m1_wid/2
            double inLoY = Math.min(inLoFromGnd, inLoFromMos);

            TrackRouter inLo = new TrackRouterH(tech.m1(), 3, inLoY, tech, inv);
            inLo.connect(inv.findExport("in"));
            for (int i = 0; i < nmos.nbGates(); i++) {
                inLo.connect(nmos.getGate(i, 'B'));
            }

            // Connect gates using metal1 along top of cell
            double vddTop = stdCell.getVddY() + stdCell.getVddWidth() / 2;
            double inHiFromVdd = vddTop +m1_m1_sp + m1_wid/2;
            double pmosTop = pmosY + fwP.physWid / 2;
            double inHiFromMos = pmosTop +pd_p1_sp + p1m1_wid/2;
            double inHiY = Math.max(inHiFromVdd, inHiFromMos);

            TrackRouter inHi = new TrackRouterH(tech.m1(), 3, inHiY, tech, inv);
            inHi.connect(inv.findExport("in"));
            for (int i=0; i<pmos.nbGates(); i++) {
                inHi.connect(pmos.getGate(i, 'T'));
            }
        }

        // connect up output
        TrackRouter outHi = new TrackRouterH(tech.m2(), exp_wid, outHiY, tech, inv);
//        outHi.setShareableViaDist(0);
        outHi.connect(inv.findExport("out"));
        for (int i=1; i<pmos.nbSrcDrns(); i += 2) {
            outHi.connect(pmos.getSrcDrn(i));
        }

        TrackRouter outLo = new TrackRouterH(tech.m2(), exp_wid, outLoY, tech, inv);
        outLo.connect(inv.findExport("out"));
        for (int i = 1; i < nmos.nbSrcDrns(); i += 2) {
            outLo.connect(nmos.getSrcDrn(i));
        }

        // add wells
        double wellMinX = 0;
        double wellMaxX = outX + m1_wid/2 + m1_m1_sp/2;
        stdCell.addNmosWell(wellMinX, wellMaxX, inv);
        stdCell.addPmosWell(wellMinX, wellMaxX, inv);

        // add essential bounds
        stdCell.addEssentialBounds(wellMinX, wellMaxX, inv);

        for (Iterator<Export> it = inv.getExports(); it.hasNext(); ) {
            Export e = it.next();
            TextDescriptor td = e.getTextDescriptor(Export.EXPORT_NAME).withRelSize(0.5);
            e.setTextDescriptor(Export.EXPORT_NAME, td);
        }
    }
}
