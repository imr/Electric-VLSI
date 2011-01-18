/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ManhattanOrientationTest.java
 *
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.api.minarea;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.electric.api.minarea.geometry.Point;
import com.sun.electric.api.minarea.geometry.Polygon.Rectangle;

/**
 *
 */
public class CIFParserTest {

    public CIFParserTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of values method, of class ManhattanOrientation.
     */
    @Test
    public void testParser() {
        System.out.println("parser");
        GenCIFActions gcif = new GenCIFActions();
        gcif.layerSelector = "CPG";
        DebugCIFActions dcif = new DebugCIFActions();
        dcif.out = System.out;
        dcif.impl = gcif;
        CIF cif = new CIF(dcif);
        cif.openTextInput(CIFParserTest.class.getResource("SimpleHierarchy.cif"));
        cif.importALibrary();
        cif.closeInput();
    }

    public class GenCIFActions implements CIF.CIFActions {

        private String layerSelector;
        private Map<Integer, DebugLayoutCell> cells = new HashMap<Integer, DebugLayoutCell>();
        private DebugLayoutCell curCell;
        private ManhattanOrientation curOrient;
        // private long[] curTranslate = new long[2];
        private Point curTranslate;
        private boolean isSelectedLayer;

        public void initInterpreter() {
            cells.clear();
            curCell = null;
        }

        public void makeWire(int width/* , path */) {
        }

        public void makeStartDefinition(int symbol, int mtl, int div) {
            Integer symbolObj = Integer.valueOf(symbol);
            assert curCell == null;
            if (cells.containsKey(symbolObj)) {
                throw new IllegalStateException("attempt to redefine symbol " + symbol);
            }
            curCell = new DebugLayoutCell(symbolObj.toString());
            cells.put(symbolObj, curCell);
            isSelectedLayer = false;
        }

        public void makeEndDefinition() {
            assert curCell != null;
            curCell = null;
        }

        public void makeDeleteDefinition(int n) {
            System.out.println("makeDeleteDefinition not supported");
        }

        public void initTransform() {
            curOrient = ManhattanOrientation.R0;
            curTranslate = new Point(0, 0);
        }

        public void appendTranslate(int xt, int yt) {
            curTranslate.add(new Point(xt, yt));
        }

        public void appendMirrorX() {
            /*
             * MirrorX in CIF means change sign of x-coordinate, this correspond
             * to MY in EDIF notation.
             */
            appendOrient(ManhattanOrientation.MY);
        }

        public void appendMirrorY() {
            /*
             * MirrorY in CIF means change sign of y-coordinate, this correspond
             * to MX in EDIF notation.
             */
            appendOrient(ManhattanOrientation.MX);
        }

        public void appendRotate(int xRot, int yRot) {
            ManhattanOrientation orient;
            if (yRot == 0) {
                if (xRot == 0) {
                    throw new IllegalArgumentException("Zero rotate vector");
                }
                orient = xRot > 0 ? ManhattanOrientation.R0 : ManhattanOrientation.R180;
            } else {
                if (xRot != 0) {
                    throw new UnsupportedOperationException("Unly Manhattan rotations are supported");
                }
                orient = yRot > 0 ? ManhattanOrientation.R90 : ManhattanOrientation.R270;
            }
            appendOrient(orient);
        }

        private void appendOrient(ManhattanOrientation orient) {
            curTranslate = orient.transformPoint(curTranslate);
            curOrient = orient.concatenate(curOrient);
        }

        public void initPath() {
        }

        public void appendPoint(Point p) {
        }

        public void makeCall(int symbol, int lineNumber/* , transform */) {
            DebugLayoutCell subCell = cells.get(Integer.valueOf(symbol));
            if (subCell == null) {
                throw new IllegalArgumentException("Subcell " + symbol + " not found");
            }
            if (subCell == curCell) {
                throw new IllegalArgumentException("Recursive cell call");
            }
            if (curCell != null) {
                curCell.addSubCell(subCell, curTranslate, curOrient);
            }
        }

        public void makeLayer(String lName) {
            isSelectedLayer = lName.equals(layerSelector);
        }

        public void makeFlash(int diameter, Point center) {
        }

        public void makePolygon(/* path */) {
        }

        public void makeBox(int length, int width, Point center, int xr, int yr) {
            int xl = center.getX() - length / 2;
            int yl = center.getY() - width / 2;
            int xh = center.getX() + length / 2;
            int yh = center.getY() + width / 2;
            if (yr != 0 || xr <= 0) {
                throw new UnsupportedOperationException("Rotated boxes are not supported");
            }
            if (curCell != null && isSelectedLayer) {
                curCell.addRectangle(new Rectangle(new Point(xl, yl), new Point(xh, yh)));
            }
        }

        public void makeUserComment(int command, String text) {
        }

        public void makeSymbolName(String name) {
            if (curCell != null) {
                curCell.setName(name);
            }
        }

        public void makeInstanceName(String name) {
        }

        public void makeGeomName(String name, Point pt, String lay) {
        }

        public void makeLabel(String name, Point pt) {
        }

        public void processEnd() {
        }

        public void doneInterpreter() {
        }
    }

    public class DebugCIFActions implements CIF.CIFActions {

        private PrintStream out;
        private CIF.CIFActions impl;

        public void initInterpreter() {
            out.println("initInterpretator();");
            impl.initInterpreter();
        }

        public void makeWire(int width/* , path */) {
            out.println("makeWire(" + width + ");");
            impl.makeWire(width);
        }

        public void makeStartDefinition(int symbol, int mtl, int div) {
            out.println("makeStartDefinition(" + symbol + "," + mtl + "," + div + ");");
            impl.makeStartDefinition(symbol, mtl, div);
        }

        public void makeEndDefinition() {
            out.println("makeEndDefinition();");
            impl.makeEndDefinition();
        }

        public void makeDeleteDefinition(int n) {
            out.print("makeDeleteDefinition(" + n + ");");
            impl.makeDeleteDefinition(n);
        }

        public void initTransform() {
            out.println("initTransform();");
            impl.initTransform();
        }

        public void appendTranslate(int xt, int yt) {
            out.println("appendTranslate(" + xt + "," + yt + ");");
            impl.appendTranslate(xt, yt);
        }

        public void appendMirrorX() {
            out.println("appendMirrorX();");
            impl.appendMirrorX();
        }

        public void appendMirrorY() {
            out.println("appendMirrorY();");
            impl.appendMirrorY();
        }

        public void appendRotate(int xRot, int yRot) {
            out.println("appendRotate(" + xRot + "," + yRot + ");");
            impl.appendRotate(xRot, yRot);
        }

        public void initPath() {
            out.println("initPath();");
            impl.initPath();
        }

        public void appendPoint(Point p) {
            out.println("appendPoint(" + p.getX() + "," + p.getY() + ");");
            impl.appendPoint(p);
        }

        public void makeCall(int symbol, int lineNumber/* , transform */) {
            out.println("makeCall(" + symbol + "," + lineNumber + ");");
            impl.makeCall(symbol, lineNumber);
        }

        public void makeLayer(String lName) {
            out.println("makeLayer(\"" + lName + "\");");
            impl.makeLayer(lName);
        }

        public void makeFlash(int diameter, Point center) {
            out.println("makeFlash(" + diameter + "," + center.getX() + "," + center.getY() + ");");
            impl.makeFlash(diameter, center);
        }

        public void makePolygon(/* path */) {
            out.println("makePolygon();");
            impl.makePolygon();
        }

        public void makeBox(int length, int width, Point center, int xr, int yr) {
            out.println("makeBox(" + length + "," + width + "," + center.getX() + "," + center.getY() + ","
                    + xr + "," + yr + ");");
            impl.makeBox(length, width, center, xr, yr);
        }

        public void makeUserComment(int command, String text) {
            out.println("makeUserComment(" + command + ",\"" + text + "\");");
            impl.makeUserComment(command, text);
        }

        public void makeSymbolName(String name) {
            out.println("makeSymbolName(\"" + name + "\");");
            impl.makeSymbolName(name);
        }

        public void makeInstanceName(String name) {
            out.println("makeInstanceName(\"" + name + "\");");
            impl.makeInstanceName(name);
        }

        public void makeGeomName(String name, Point pt, String lay) {
            out.println("makeGeomName(\"" + name + "\"," + pt.getX() + "," + pt.getY() + ","
                    + (lay != null ? "\"" + lay + "\"" : "null") + ");");
            impl.makeGeomName(name, pt, lay);
        }

        public void makeLabel(String name, Point pt) {
            out.println("makeLableName(\"" + name + "\"," + pt.getX() + "," + pt.getY() + ");");
            impl.makeLabel(name, pt);
        }

        public void processEnd() {
            out.println("processEnd();");
            impl.processEnd();
        }

        public void doneInterpreter() {
            out.println("doneInterpreter();");
            impl.doneInterpreter();
        }
    }

    public class NullCIFActions implements CIF.CIFActions {

        public void initInterpreter() {
        }

        public void makeWire(int width/* , path */) {
        }

        public void makeStartDefinition(int symbol, int mtl, int div) {
        }

        public void makeEndDefinition() {
        }

        public void makeDeleteDefinition(int n) {
        }

        public void initTransform() {
        }

        public void appendTranslate(int xt, int yt) {
        }

        public void appendMirrorX() {
        }

        public void appendMirrorY() {
        }

        public void appendRotate(int xRot, int yRot) {
        }

        public void initPath() {
        }

        public void appendPoint(Point p) {
        }

        public void makeCall(int symbol, int lineNumber/* , transform */) {
        }

        public void makeLayer(String lName) {
        }

        public void makeFlash(int diameter, Point center) {
        }

        public void makePolygon(/* path */) {
        }

        public void makeBox(int length, int width, Point center, int xr, int yr) {
        }

        public void makeUserComment(int command, String text) {
        }

        public void makeSymbolName(String name) {
        }

        public void makeInstanceName(String name) {
        }

        public void makeGeomName(String name, Point pt, String lay) {
        }

        public void makeLabel(String name, Point pt) {
        }

        public void processEnd() {
        }

        public void doneInterpreter() {
        }
    }
}
