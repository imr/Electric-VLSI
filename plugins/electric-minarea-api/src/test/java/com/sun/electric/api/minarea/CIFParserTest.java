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

import java.awt.Point;
import java.io.PrintStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

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
        MyCIFActions c1 = new MyCIFActions();
        c1.out = System.out;
        CIF cif = new CIF(c1);
        cif.openTextInput(CIFParserTest.class.getResource("SimpleHierarchy.cif"));
        cif.importALibrary();
        cif.closeInput();
    }

    public class MyCIFActions implements CIF.CIFActions {
        private PrintStream out;
        
        public void initInterpreter() {
            out.println("initInterpretator();");
        }
        
        public void makeWire(int width/*, path*/) {
            out.println("makeWire(" + width + ");");
        }
        
        public void makeStartDefinition(int symbol, int mtl, int div) {
            out.println("makeStartDefinition(" + symbol + "," + mtl + "," + div + ");");
        }
        
        public void makeEndDefinition() {
            out.println("makeEndDefinition();");
        }
        
        public void makeDeleteDefinition(int n) {
            out.print("makeDeleteDefinition(" + n + ");");
        }
        
        public void initTransform() {
            out.println("initTransform();");
        }
        
        public void appendTranslate(int xt, int yt) {
            out.println("appendTranslate(" + xt + "," + yt + ");");
        }
        
        public void appendMirrorX() {
            out.println("appendMirrorX();");
        }
        
        public void appendMirrorY() {
            out.println("appendMirrorY();");
        }
        
        public void appendRotate(int xRot, int yRot) {
            out.println("appendRotate(" + xRot + "," + yRot + ");");
        }
        
        public void initPath() {
            out.println("initPath();");
        }
        
        public void appendPoint(Point p) {
            out.println("appendPoint(" + p.x + "," + p.y + ");");
        }
        
        public void makeCall(int symbol, int lineNumber/*, transform*/) {
            out.println("makeCall(" + symbol + "," + lineNumber + ");");
        }
        
        public void makeLayer(String lName) {
            out.println("makeLayer(\"" + lName + "\");");
        }
        
        public void makeFlash(int diameter, Point center) {
            out.println("makeFlash(" + diameter + "," + center.x + "," + center.y + ");");
        }
        
        public void makePolygon(/*path*/) {
            out.println("makePolygon();");
        }
        
        public void makeBox(int length, int width, Point center, int xr, int yr) {
            out.println("makeBox(" + length + "," + width + "," + center.x + "," + center.y + "," + xr + "," + yr + ");");
        }
        
        public void makeUserComment(int command, String text) {
            out.println("makeUserComment(" + command + ",\"" + text + "\");");
        }
        
        public void makeSymbolName(String name) {
            out.println("makeSymbolName(\"" + name + "\");");
        }
        
        public void makeInstanceName(String name) {
            out.println("makeInstanceName(\"" + name + "\");");
        }
        
        public void makeGeomName(String name, Point pt, String lay) {
            out.println("makeGeomName(\"" + name + "\"," + pt.x + "," + pt.y + "," + (lay != null ? "\"" + lay + "\"" : "null") + ");");
        }
        
        public void makeLabel(String name, Point pt) {
            out.println("makeLableName(\"" + name + "\"," + pt.x + "," + pt.y + ");");
        }
        
        public void processEnd() {
            out.println("processEnd();");
        }
        
        public void doneInterpreter() {
            out.println("doneInterpreter();");
        }
    }
}
