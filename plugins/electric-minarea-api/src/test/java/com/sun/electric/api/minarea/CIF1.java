/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CIF1.java
 * Input/output tool: CIF input
 * Original C CIF Parser (front end) by Robert W. Hon, Schlumberger Palo Alto Research
 * and its interface to C Electric (back end) by Robert Winstanley, University of Calgary.
 * Translated into Java by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class reads files in CIF files.
 */
public class CIF1 implements CIF.CIFActions {

    /** max depth of min/max stack */
    private static final int MAXMMSTACK = 50;

    // types for FrontTransformLists
    static class FrontTransformType {
    }
    FrontTransformType MIRROR = new FrontTransformType();
    FrontTransformType TRANSLATE = new FrontTransformType();
    FrontTransformType ROTATE = new FrontTransformType();
    // error codes for reporting errors
    private static final int FATALINTERNAL = 0;
    private static final int FATALSYNTAX = 1;
    private static final int FATALSEMANTIC = 2;
    private static final int FATALOUTPUT = 3;
    private static final int ADVISORY = 4;
//	private static final int OTHER         = 5;			/* OTHER must be last */
    private static final int TIDENT = 0;
    private static final int TROTATE = 1;
    private static final int TTRANSLATE = 2;
    private static final int TMIRROR = 4;

    static class FrontTransformEntry {

        FrontTransformType kind;
        boolean xCoord;
        int xt, yt;
        int xRot, yRot;
    };

    /** data types for transformation package */
    static class FrontMatrix {

        double a11, a12, a21, a22, a31, a32, a33;
        FrontMatrix prev, next;
        int type;
        boolean multiplied;
    };

    /** bounding box */
    static class FrontBBox {

        int l, r, b, t;
    };

    static class FrontSymbol {

        /** symbol number for this entry */
        int symNumber;
        boolean expanded;
        boolean defined;
        boolean dumped;
        /** bb as if this symbol were called by itself */
        FrontBBox bounds;
        /** flag for rebuilding bounding box */
        boolean boundsValid;
        /** name of this symbol */
        String name;
        /** number of calls made by this symbol */
        int numCalls;
        /** pointer to linked list of objects */
        FrontObjBase guts;

        FrontSymbol(int num) {
            bounds = new FrontBBox();
            symNumber = num;
            expanded = false;
            defined = false;
            dumped = false;
            numCalls = 0;
            boundsValid = false;
            name = null;
            guts = null;
        }
    };

    static class FrontLinkedPoint {

        Point pValue;
        FrontLinkedPoint pNext;
    };

    static class FrontPath {

        FrontLinkedPoint pFirst, pLast;
        int pLength;

        FrontPath() {
            pFirst = null;
            pLast = null;
            pLength = 0;
        }
    };

    static class FrontLinkedTransform {

        FrontTransformEntry tValue;
        FrontLinkedTransform tNext;
    };

    static class FrontTransformList {

        FrontLinkedTransform tFirst, tLast;
        int tLength;

        FrontTransformList() {
            tFirst = null;
            tLast = null;
            tLength = 0;
        }
    };

    /** items in item tree */
    static class FrontItem {

        /** pointer into symbol structure */
        FrontObjBase what;
    };

    /** hack structure for referencing first fields of any object */
    static class FrontObjBase {

        /** bounding box */
        FrontBBox bb;
        /** for ll */
        FrontObjBase next;
        /** layer for this object */
        String layer;

        FrontObjBase() {
            bb = new FrontBBox();
        }
    };

    /** symbol call object */
    static class FrontCall extends FrontObjBase {

        /** rest is non-critical */
        int symNumber;
        int lineNumber;
        FrontSymbol unID;
        FrontMatrix matrix;
        /** transformation list for this call */
        FrontTransformList transList;
    };

    static class FrontGeomName extends FrontObjBase {

        String name;
    };

    static class FrontLabel extends FrontObjBase {

        String name;
        Point pos;
    };

    static class FrontBox extends FrontObjBase {

        int length, width;
        Point center;
        int xRot, yRot;
    };

    static class FrontManBox extends FrontObjBase {
    };

    static class FrontFlash extends FrontObjBase {

        Point center;
        int diameter;
    };

    static class FrontPoly extends FrontObjBase {

        /** array of points in path */
        Point[] points;
    };

    static class FrontWire extends FrontObjBase {

        /** width of wire */
        int width;
        /** array of points in wire */
        Point[] points;
    };
    /** item list */
    private List<FrontItem> currentItemList;
    /** A/B from DS */
    private double cellScaleFactor;
    /** current symbol being defined */
    private FrontSymbol currentFrontSymbol;
    /** place to save layer during def */
    private String backupLayer;
    /** symbol has been named */
    private boolean symbolNamed;
    /** definition in progress flag */
    private boolean isInCellDefinition;
    /** null layer errors encountered */
    private boolean numNullLayerErrors;
    /** ignore statements until DF */
    private boolean ignoreStatements;
    /** 91 pending */
    private boolean namePending;
    /** end command flag */
    private boolean endCommandFound;
    /** current layer */
    private String currentLayer;
    /** symbol table */
    private Map<Integer, FrontSymbol> symbolTable;
    /** the top of stack */
    private FrontMatrix matrixStackTop;
    /** # statements since 91 com */
    private boolean statementsSince91;
    /** min/max stack pointer */
    private int minMaxStackPtr;
    /** min/max stack: left edge */
    private int[] minMaxStackLeft;
    /** min/max stack: right edge */
    private int[] minMaxStackRight;
    /** min/max stack: bottom edge */
    private int[] minMaxStackBottom;
    /** min/max stack: top edge */
    private int[] minMaxStackTop;

    private FrontTransformList curTList;
    private FrontPath curPath;

            /**
     * Creates a new instance of CIF.
     */
    CIF1() {
    }

    /**
     * Method to import a library from disk.
     * @param lib the library to fill
     * @param currentCells this map will be filled with currentCells in Libraries found in library file
     * @return the created library (null on error).
     */
    protected boolean importALibrary() {
        if (initFind()) {
            return false;
        }

        // parse the CIF and create a listing
        if (interpret()) {
            return false;
        }

        // clean up
        doneInterpreter();

        return true;
    }

    private boolean interpret() {
        initParser();
        initInterpreter();
//        inFromFile();
//        parseFile();		// read in the CIF
//        doneParser();

//        if (numFatalErrors > 0) {
//            return true;
//        }

        getInterpreterBounds();

        // construct a list: first step in the conversion
        createList();
        return false;
    }

    private boolean initFind() {
        return false;
    }

    public void initInterpreter() {
        numNullLayerErrors = false;
        isInCellDefinition = false;
        ignoreStatements = false;
        namePending = false;
        endCommandFound = false;
        currentLayer = null;
        currentItemList = new ArrayList<FrontItem>();
        initUtilities();
        initMatrices();
    }

    private void initParser() {
        isInCellDefinition = false;
    }

    private void doneInterpreter() {
        if (numNullLayerErrors) {
            System.out.println("Warning: some CIF objects were not read");
        }
    }

    private Rectangle getInterpreterBounds() {
        boolean first = true;

        pushTransform();
        for (FrontItem h : currentItemList) {
            FrontObjBase obj = h.what;
            Point temp = new Point();
            temp.x = obj.bb.l;
            temp.y = obj.bb.b;
            Point comperror = transformPoint(temp);
            initMinMax(comperror);
            temp.x = obj.bb.r;
            comperror = transformPoint(temp);
            minMax(comperror);
            temp.y = obj.bb.t;
            comperror = transformPoint(temp);
            minMax(comperror);
            temp.x = obj.bb.l;
            comperror = transformPoint(temp);
            minMax(comperror);

            int left = getMinMaxMinX();
            int right = getMinMaxMaxX();
            int bottom = getMinMaxMinY();
            int top = getMinMaxMaxY();
            doneMinMax();
            temp.x = left;
            temp.y = bottom;
            if (first) {
                first = false;
                initMinMax(temp);
            } else {
                minMax(temp);
            }
            temp.x = right;
            temp.y = top;
            minMax(temp);
        }
        Rectangle ret = new Rectangle(getMinMaxMinX(), getMinMaxMinY(),
                getMinMaxMaxX() - getMinMaxMinX(), getMinMaxMaxY() - getMinMaxMinY());
        doneMinMax();
        popTransform();
        return ret;
    }

    private void createList() {
        if (!isEndSeen()) {
            System.out.println("missing End command, assumed");
        }
//        if (numFatalErrors > 0) {
//            return;
//        }
        sendList(currentItemList);		// sendlist deletes nodes
        currentItemList = null;
    }

    private void sendList(List<FrontItem> list) {
        for (FrontItem h : list) {
            outItem(h);
        }
    }

    /**
     * spit out an item
     */
    private void outItem(FrontItem thing) {
    }

    private boolean isEndSeen() {
        return endCommandFound;
    }

    private void initUtilities() {
        minMaxStackLeft = new int[MAXMMSTACK];
        minMaxStackRight = new int[MAXMMSTACK];
        minMaxStackBottom = new int[MAXMMSTACK];
        minMaxStackTop = new int[MAXMMSTACK];
        symbolTable = new HashMap<Integer, FrontSymbol>();
        minMaxStackPtr = -1;			// min/max stack pointer
    }

    private void initMatrices() {
        matrixStackTop = new FrontMatrix();
        clearMatrix(matrixStackTop);
        matrixStackTop.next = null;
        matrixStackTop.prev = null;
        matrixStackTop.multiplied = true;
    }

    private void clearMatrix(FrontMatrix mat) {
        mat.a11 = 1.0;
        mat.a12 = 0.0;
        mat.a21 = 0.0;
        mat.a22 = 1.0;
        mat.a31 = 0.0;
        mat.a32 = 0.0;
        mat.a33 = 1.0;
        mat.type = TIDENT;
        mat.multiplied = false;
    }

    public void makeLabel(String name, Point pt) {
        statementsSince91 = true;
        if (ignoreStatements) {
            return;
        }
        if (name.length() == 0) {
            errorReport("null label ignored", ADVISORY);
            return;
        }
        FrontLabel obj = new FrontLabel();
        if (isInCellDefinition && cellScaleFactor != 1.0) {
            pt.x = (int) Math.round(cellScaleFactor * pt.x);
            pt.y = (int) Math.round(cellScaleFactor * pt.y);
        }
        obj.pos = pt;
        obj.name = name;

        pushTransform();
        Point temp = transformPoint(pt);
        popTransform();
        obj.bb.l = temp.x;
        obj.bb.r = temp.x;
        obj.bb.b = temp.y;
        obj.bb.t = temp.y;

        if (isInCellDefinition) {
            // insert into symbol's guts
            obj.next = currentFrontSymbol.guts;
            currentFrontSymbol.guts = obj;
        } else {
            topLevelItem(obj);		// stick into item list
        }
    }

    public void makeGeomName(String name, Point pt, String lay) {
        statementsSince91 = true;
        if (ignoreStatements) {
            return;
        }
        if (name.length() == 0) {
            errorReport("null geometry name ignored", ADVISORY);
            return;
        }
        FrontGeomName obj = new FrontGeomName();
        obj.layer = lay;
        obj.name = name;
        if (isInCellDefinition && cellScaleFactor != 1.0) {
            pt.x = (int) Math.round(cellScaleFactor * pt.x);
            pt.y = (int) Math.round(cellScaleFactor * pt.y);
        }

        pushTransform();
        Point temp = transformPoint(pt);
        popTransform();
        obj.bb.l = temp.x;
        obj.bb.r = temp.x;
        obj.bb.b = temp.y;
        obj.bb.t = temp.y;

        if (isInCellDefinition) {
            // insert into symbol's guts
            obj.next = currentFrontSymbol.guts;
            currentFrontSymbol.guts = obj;
        } else {
            topLevelItem(obj);		// stick into item list
        }
    }

    public void makeSymbolName(String name) {
        statementsSince91 = true;
        if (ignoreStatements) {
            return;
        }
        if (!isInCellDefinition) {
            errorReport("no symbol to name", FATALSEMANTIC);
            return;
        }
        if (name.length() == 0) {
            errorReport("null symbol name ignored", ADVISORY);
            return;
        }
        if (symbolNamed) {
            errorReport("symbol is already named, new name ignored", FATALSEMANTIC);
            return;
        }
        symbolNamed = true;
        currentFrontSymbol.name = name;
    }

    public void makeUserComment(int command, String text) {
        statementsSince91 = true;
        if (ignoreStatements) {
            return;
        }
    }

    public void makeBox(int length, int width, Point center, int xr, int yr) {
        statementsSince91 = true;
        if (ignoreStatements) {
            return;
        }
        if (currentLayer == null) {
            numNullLayerErrors = true;
            return;
        }
        if (length == 0 || width == 0) {
            errorReport("box with null length or width specified, ignored", ADVISORY);
            return;
        }

        if (isInCellDefinition && cellScaleFactor != 1.0) {
            length = (int) Math.round(cellScaleFactor * length);
            width = (int) Math.round(cellScaleFactor * width);
            center.x = (int) Math.round(cellScaleFactor * center.x);
            center.y = (int) Math.round(cellScaleFactor * center.y);
        }

        Rectangle box = boundsBox(length, width, center, xr, yr);
        int tl = box.x;
        int tr = box.x + box.width;
        int tb = box.y;
        int tt = box.y + box.height;

        // check for Manhattan box
        int halfW = width / 2;
        int halfL = length / 2;
        if ((yr == 0 && (length % 2) == 0 && (width % 2) == 0
                && (center.x - halfL) == tl && (center.x + halfL) == tr
                && (center.y - halfW) == tb && (center.y + halfW) == tt)
                || (xr == 0 && (length % 2) == 0 && (width % 2) == 0
                && (center.x - halfW) == tl && (center.x + halfW) == tr
                && (center.y - halfL) == tb && (center.y + halfL) == tt)) {
            // a Manhattan box
            FrontManBox obj = new FrontManBox();
            obj.layer = currentLayer;
            if (yr == 0) {
                obj.bb.l = tl;
                obj.bb.r = tr;
                obj.bb.b = tb;
                obj.bb.t = tt;
            } else {
                // this assumes that bounding box is unaffected by rotation
                obj.bb.l = center.x - halfW;
                obj.bb.r = center.x + halfW;
                obj.bb.b = center.y - halfL;
                obj.bb.t = center.y + halfL;
            }
            if (isInCellDefinition) {
                // insert into symbol's guts
                obj.next = currentFrontSymbol.guts;
                currentFrontSymbol.guts = obj;
            } else {
                topLevelItem(obj);		// stick into item list
            }
        } else {
            FrontBox obj = new FrontBox();
            obj.layer = currentLayer;
            obj.length = length;
            obj.width = width;
            obj.center = center;
            obj.xRot = xr;
            obj.yRot = yr;

            obj.bb.l = tl;
            obj.bb.r = tr;
            obj.bb.b = tb;
            obj.bb.t = tt;
            if (isInCellDefinition) {
                // insert into symbol's guts
                obj.next = currentFrontSymbol.guts;
                currentFrontSymbol.guts = obj;
            } else {
                topLevelItem(obj);		// stick into item list
            }
        }
    }

    public void makeFlash(int diameter, Point center) {
        statementsSince91 = true;
        if (ignoreStatements) {
            return;
        }
        if (currentLayer == null) {
            numNullLayerErrors = true;
            return;
        }
        if (diameter == 0) {
            errorReport("flash with null diamter, ignored", ADVISORY);
            return;
        }

        FrontFlash obj = new FrontFlash();
        obj.layer = currentLayer;
        if (isInCellDefinition && cellScaleFactor != 1.0) {
            diameter = (int) Math.round(cellScaleFactor * diameter);
            center.x = (int) Math.round(cellScaleFactor * center.x);
            center.y = (int) Math.round(cellScaleFactor * center.y);
        }
        obj.diameter = diameter;
        obj.center = center;

        Rectangle box = boundsFlash(diameter, center);
        obj.bb.l = box.x;
        obj.bb.r = box.x + box.width;
        obj.bb.b = box.y;
        obj.bb.t = box.y + box.height;

        if (isInCellDefinition) {
            // insert into symbol's guts
            obj.next = currentFrontSymbol.guts;
            currentFrontSymbol.guts = obj;
        } else {
            topLevelItem(obj);		// stick into item list
        }
    }

    private Rectangle boundsFlash(int diameter, Point center) {
        return boundsBox(diameter, diameter, center, 1, 0);
    }

    private Rectangle boundsBox(int length, int width, Point center, int xr, int yr) {
        int dx = length / 2;
        int dy = width / 2;

        pushTransform();	// new transformation
        rotateMatrix(xr, yr);
        translateMatrix(center.x, center.y);
        Point temp = new Point(dx, dy);
        initMinMax(transformPoint(temp));
        temp.y = -dy;
        minMax(transformPoint(temp));
        temp.x = -dx;
        minMax(transformPoint(temp));
        temp.y = dy;
        minMax(transformPoint(temp));
        popTransform();
        Rectangle ret = new Rectangle(getMinMaxMinX(), getMinMaxMinY(),
                getMinMaxMaxX() - getMinMaxMinX(), getMinMaxMaxY() - getMinMaxMinY());
        doneMinMax();
        return ret;
    }

    public void makeLayer(String lName) {
        statementsSince91 = true;
        if (ignoreStatements) {
            return;
        }
        currentLayer = lName;
    }

    public void initTransform() {
        curTList = new FrontTransformList();
    }
    
    public void appendTranslate(int xt, int yt) {
        FrontTransformEntry trans = new FrontTransformEntry();
        trans.kind = TRANSLATE;
        trans.xt = xt;
        trans.yt = yt;
        appendTransformEntry(curTList, trans);
    }
    
    public void appendMirrorX() {
        FrontTransformEntry trans = new FrontTransformEntry();
        trans.kind = MIRROR;
        trans.xCoord = true;
        appendTransformEntry(curTList, trans);
    }
    
    public void appendMirrorY() {
        FrontTransformEntry trans = new FrontTransformEntry();
        trans.kind = MIRROR;
        trans.xCoord = false;
        appendTransformEntry(curTList, trans);
    }
        
    public void appendRotate(int xRot, int yRot) {
        FrontTransformEntry trans = new FrontTransformEntry();
        trans.kind = ROTATE;
        trans.xRot = xRot;
        trans.yRot = yRot;
        appendTransformEntry(curTList, trans);
    }
    
    public void makeCall(int symbol, int lineNumber) {
        FrontTransformList list = curTList;
        curTList = null;
        
        if (ignoreStatements) {
            return;
        }
        int j = getFrontTransformListLength(list);
        FrontTransformList newtlist = null;
        if (j != 0) {
            newtlist = new FrontTransformList();
        }

        pushTransform();		// get new frame of reference
        for (int i = 1; i <= j; i++) {
            // build up incremental transformations
            FrontTransformEntry temp = removeFrontTransformEntry(list);
            if (temp.kind == MIRROR) {
                mirrorMatrix(temp.xCoord);
            } else if (temp.kind == TRANSLATE) {
                if (isInCellDefinition && cellScaleFactor != 1.0) {
                    temp.xt = (int) Math.round(cellScaleFactor * temp.xt);
                    temp.yt = (int) Math.round(cellScaleFactor * temp.yt);
                }
                translateMatrix(temp.xt, temp.yt);
            } else if (temp.kind == ROTATE) {
                rotateMatrix(temp.xRot, temp.yRot);
            } else {
                errorReport("interpreter: no such transformation", FATALINTERNAL);
            }
            appendTransformEntry(newtlist, temp);	// copy the list
        }

        FrontCall obj = new FrontCall();
        obj.lineNumber = lineNumber;

        // must make a copy of the matrix
        obj.matrix = new FrontMatrix();
        obj.matrix.a11 = matrixStackTop.a11;
        obj.matrix.a12 = matrixStackTop.a12;
        obj.matrix.a21 = matrixStackTop.a21;
        obj.matrix.a22 = matrixStackTop.a22;
        obj.matrix.a31 = matrixStackTop.a31;
        obj.matrix.a32 = matrixStackTop.a32;
        obj.matrix.a33 = matrixStackTop.a33;
        obj.matrix.type = matrixStackTop.type;
        obj.matrix.multiplied = matrixStackTop.multiplied;

        popTransform();		// return to previous state

        obj.symNumber = symbol;
        obj.unID = null;
        obj.transList = newtlist;

        if (namePending) {
            if (statementsSince91) {
                errorReport("statements between name and instance", ADVISORY);
            }
            namePending = false;
        }
        if (isInCellDefinition) {
            // insert into guts of symbol
            obj.next = currentFrontSymbol.guts;
            currentFrontSymbol.guts = obj;
            currentFrontSymbol.numCalls++;
        } else {
            topLevelItem(obj);
        }
    }

    private void rotateMatrix(int xRot, int yRot) {
        double si = yRot;
        double co = xRot;
        if (yRot == 0 && xRot >= 0) {
            return;
        }

        matrixStackTop.type |= TROTATE;
        if (xRot == 0) {
            double temp = matrixStackTop.a11;
            matrixStackTop.a11 = -matrixStackTop.a12;
            matrixStackTop.a12 = temp;

            temp = matrixStackTop.a21;
            matrixStackTop.a21 = -matrixStackTop.a22;
            matrixStackTop.a22 = temp;

            temp = matrixStackTop.a31;
            matrixStackTop.a31 = -matrixStackTop.a32;
            matrixStackTop.a32 = temp;
            if (yRot < 0) {
                matrixStackTop.a33 = -matrixStackTop.a33;
            }
        } else if (yRot == 0) {
            matrixStackTop.a33 = -matrixStackTop.a33;	// xRot < 0
        } else {
            double temp = matrixStackTop.a11 * co - matrixStackTop.a12 * si;
            matrixStackTop.a12 = matrixStackTop.a11 * si + matrixStackTop.a12 * co;
            matrixStackTop.a11 = temp;
            temp = matrixStackTop.a21 * co - matrixStackTop.a22 * si;
            matrixStackTop.a22 = matrixStackTop.a21 * si + matrixStackTop.a22 * co;
            matrixStackTop.a21 = temp;
            temp = matrixStackTop.a31 * co - matrixStackTop.a32 * si;
            matrixStackTop.a32 = matrixStackTop.a31 * si + matrixStackTop.a32 * co;
            matrixStackTop.a31 = temp;
            matrixStackTop.a33 = new Point2D.Double(0, 0).distance(new Point2D.Double(co, si));
        }
    }

    private void translateMatrix(int xtrans, int ytrans) {
        if (xtrans != 0 || ytrans != 0) {
            matrixStackTop.a31 += matrixStackTop.a33 * xtrans;
            matrixStackTop.a32 += matrixStackTop.a33 * ytrans;
            matrixStackTop.type |= TTRANSLATE;
        }
    }

    private void mirrorMatrix(boolean xCoord) {
        if (xCoord) {
            matrixStackTop.a11 = -matrixStackTop.a11;
            matrixStackTop.a21 = -matrixStackTop.a21;
            matrixStackTop.a31 = -matrixStackTop.a31;
        } else {
            matrixStackTop.a12 = -matrixStackTop.a12;
            matrixStackTop.a22 = -matrixStackTop.a22;
            matrixStackTop.a32 = -matrixStackTop.a32;
        }
        matrixStackTop.type |= TMIRROR;
    }

    private int getFrontTransformListLength(FrontTransformList a) {
        if (a == null) {
            return 0;
        }
        return a.tLength;
    }

    private FrontTransformEntry removeFrontTransformEntry(FrontTransformList a) {
        if (a.tFirst == null) {
            // added extra code to initialize "ans" to a dummy value
            FrontTransformEntry ans = new FrontTransformEntry();
            ans.kind = TRANSLATE;
            ans.xt = ans.yt = 0;
            return ans;
        }
        FrontLinkedTransform temp = a.tFirst.tNext;
        FrontTransformEntry ans = a.tFirst.tValue;
        a.tFirst = temp;
        if (a.tFirst == null) {
            a.tLast = null;
        }
        a.tLength -= 1;
        return ans;
    }

    public void makeDeleteDefinition(int n) {
        statementsSince91 = true;
        errorReport("DD not supported (ignored)", ADVISORY);
    }

    public void makeEndDefinition() {
        statementsSince91 = true;
        if (ignoreStatements) {
            ignoreStatements = false;
            return;
        }
        isInCellDefinition = false;
        currentLayer = backupLayer;		// restore old layer
        if (!symbolNamed) {
            String s = "SYM" + currentFrontSymbol.symNumber;
            currentFrontSymbol.name = s;
        }
        currentFrontSymbol.defined = true;
    }

    public void makeStartDefinition(int symbol, int mtl, int div) {
        statementsSince91 = true;
        currentFrontSymbol = lookupSymbol(symbol);
        if (currentFrontSymbol.defined) {
            // redefining this symbol
            String mess = "attempt to redefine symbol " + symbol + " (ignored)";
            errorReport(mess, ADVISORY);
            ignoreStatements = true;
            return;
        }

        isInCellDefinition = true;
        if (mtl != 0 && div != 0) {
            cellScaleFactor = ((float) mtl) / ((float) div);
        } else {
            errorReport("illegal scale factor, ignored", ADVISORY);
            cellScaleFactor = 1.0;
        }
        backupLayer = currentLayer;	// save current layer
        currentLayer = null;
        symbolNamed = false;					// symbol not named
    }

    public void initPath() {
        curPath = new FrontPath();
    }
    
    public void appendPoint(Point p) {
        appendPoint(curPath, p);
    }
    
    public void makeWire(int width) {
        FrontPath a = curPath;
        curPath = null;
        int length = a.pLength;
        statementsSince91 = true;
        if (ignoreStatements) {
            return;
        }
        if (currentLayer == null) {
            numNullLayerErrors = true;
            return;
        }
        FrontWire obj = new FrontWire();
        FrontPath tPath = a;
        FrontPath sPath = null;			// path in case of scaling
        obj.layer = currentLayer;
        if (isInCellDefinition && cellScaleFactor != 1.0) {
            sPath = new FrontPath();
            scalePath(a, sPath);		// scale all points
            width = (int) Math.round(cellScaleFactor * width);
            tPath = sPath;
        }
        obj.width = width;

        FrontPath bbpath = new FrontPath();		// get a new path for bounding box use
        copyPath(tPath, bbpath);
        boundsWire(width, bbpath);
        obj.points = new Point[length];
        for (int i = 0; i < length; i++) {
            obj.points[i] = removePoint(tPath);
        }

        if (isInCellDefinition) {
            // insert into symbol's guts
            obj.next = currentFrontSymbol.guts;
            currentFrontSymbol.guts = obj;
        } else {
            topLevelItem(obj);		// stick into item list
        }
    }

    private Rectangle boundsWire(int width, FrontPath pPath) {
        int half = (width + 1) / 2;
        int limit = pPath.pLength;

        pushTransform();	// new transformation
        initMinMax(transformPoint(removePoint(pPath)));
        for (int i = 1; i < limit; i++) {
            minMax(transformPoint(removePoint(pPath)));
        }
        popTransform();
        Rectangle rect = new Rectangle(getMinMaxMinX() - half, getMinMaxMinY() - half,
                getMinMaxMaxX() - getMinMaxMinX() + half * 2, getMinMaxMaxY() - getMinMaxMinY() + half * 2);
        doneMinMax();
        return rect;
    }

    void appendTransformEntry(FrontTransformList a, FrontTransformEntry p) {
        FrontLinkedTransform newT = new FrontLinkedTransform();
        if (newT == null) {
            return;
        }

        FrontLinkedTransform temp = a.tLast;
        a.tLast = newT;
        if (temp != null) {
            temp.tNext = a.tLast;
        }
        a.tLast.tValue = p;
        a.tLast.tNext = null;
        if (a.tFirst == null) {
            a.tFirst = a.tLast;
        }
        a.tLength += 1;
    }

    public void makePolygon() {
        FrontPath a = curPath;
        curPath = null;
        int length = a.pLength;
        statementsSince91 = true;
        if (ignoreStatements) {
            return;
        }
        if (currentLayer == null) {
            numNullLayerErrors = true;
            return;
        }
        if (length < 3) {
            errorReport("polygon with < 3 pts in path, ignored", ADVISORY);
            return;
        }

        FrontPoly obj = new FrontPoly();
        FrontPath tPath = a;
        obj.layer = currentLayer;
        if (isInCellDefinition && cellScaleFactor != 1.0) {
            FrontPath sPath = new FrontPath();
            scalePath(a, sPath);		// scale all points
            tPath = sPath;
        }

        FrontPath bbpath = new FrontPath();		// get a new path for bounding box use
        copyPath(tPath, bbpath);
        Rectangle box = getPolyBounds(bbpath);
        obj.bb.l = box.x;
        obj.bb.r = box.x + box.width;
        obj.bb.b = box.y;
        obj.bb.t = box.y + box.height;
        obj.points = new Point[length];
        for (int i = 0; i < length; i++) {
            obj.points[i] = removePoint(tPath);
        }

        if (isInCellDefinition) {
            // insert into symbol's guts
            obj.next = currentFrontSymbol.guts;
            currentFrontSymbol.guts = obj;
        } else {
            topLevelItem(obj);		// stick into item list
        }
    }

    /**
     * a bare item has been found
     */
    private void topLevelItem(FrontObjBase object) {
        if (object == null) {
            errorReport("item: null object", FATALINTERNAL);
            return;
        }

        FrontItem newItem = new FrontItem();
        currentItemList.add(newItem);
        newItem.what = object;

        // symbol calls only
        if (object instanceof FrontCall) {
            findCallBounds((FrontCall) object);
        }
    }

    /**
     * find the bounding box for this particular call
     */
    private void findCallBounds(FrontCall object) {
        FrontSymbol thisST = lookupSymbol(object.symNumber);
        if (!thisST.defined) {
            String mess = "call to undefined symbol " + thisST.symNumber;
            errorReport(mess, FATALSEMANTIC);
            return;
        }
        if (thisST.expanded) {
            String mess = "recursive call on symbol " + thisST.symNumber;
            errorReport(mess, FATALSEMANTIC);
            return;
        }
        thisST.expanded = true;		// mark as under expansion

        findBounds(thisST);		// get the bounding box of the symbol in its FrontSymbol
        object.unID = thisST;	// get this symbol's id

        pushTransform();			// set up a new frame of reference
        applyLocal(object.matrix);
        Point temp = new Point();
        temp.x = thisST.bounds.l;
        temp.y = thisST.bounds.b;	// lower-left
        Point comperror = transformPoint(temp);
        initMinMax(comperror);
        temp.x = thisST.bounds.r;
        comperror = transformPoint(temp);
        minMax(comperror);
        temp.y = thisST.bounds.t;	// upper-right
        comperror = transformPoint(temp);
        minMax(comperror);
        temp.x = thisST.bounds.l;
        comperror = transformPoint(temp);
        minMax(comperror);

        object.bb.l = getMinMaxMinX();
        object.bb.r = getMinMaxMaxX();
        object.bb.b = getMinMaxMinY();
        object.bb.t = getMinMaxMaxY();
        doneMinMax();		// object now has transformed bounding box of the symbol
        popTransform();

        thisST.expanded = false;
    }

    /**
     * find bounding box for sym
     */
    private void findBounds(FrontSymbol sym) {
        boolean first = true;
        FrontObjBase ob = sym.guts;
        if (sym.boundsValid) {
            return;			// already done
        }
        if (ob == null) // empty symbol
        {
            String name = sym.name;
            if (name == null) {
                name = "#" + sym.symNumber;
            }
            System.out.println("Warning: cell " + name + " has no geometry in it");
            sym.bounds.l = 0;
            sym.bounds.r = 0;
            sym.bounds.b = 0;
            sym.bounds.t = 0;
            sym.boundsValid = true;
            return;
        }

        while (ob != null) {
            // find bounding box for symbol calls, all primitive are done already
            if (ob instanceof FrontCall) {
                findCallBounds((FrontCall) ob);
            }
            Point temp = new Point();
            temp.x = ob.bb.l;
            temp.y = ob.bb.b;
            if (first) {
                first = false;
                initMinMax(temp);
            } else {
                minMax(temp);
            }
            temp.x = ob.bb.r;
            temp.y = ob.bb.t;
            minMax(temp);
            ob = ob.next;
        }
        sym.bounds.l = getMinMaxMinX();
        sym.bounds.r = getMinMaxMaxX();
        sym.bounds.b = getMinMaxMinY();
        sym.bounds.t = getMinMaxMaxY();
        sym.boundsValid = true;
        doneMinMax();
    }

    /**
     * Method to find a given symbol.
     * If none, make a blank entry.
     * @return a pointer to whichever.
     */
    private FrontSymbol lookupSymbol(int sym) {
        FrontSymbol val = symbolTable.get(new Integer(sym));
        if (val == null) {
            // create a new entry
            val = new FrontSymbol(sym);
            symbolTable.put(new Integer(sym), val);
        }
        return val;
    }

    private void applyLocal(FrontMatrix tm) {
        assignMatrix(tm, matrixStackTop);
    }

    private void scalePath(FrontPath src, FrontPath dest) {
        int limit = src.pLength;
        for (int i = 0; i < limit; i++) {
            Point temp = removePoint(src);

            temp.x = (int) Math.round(cellScaleFactor * temp.x);
            temp.y = (int) Math.round(cellScaleFactor * temp.y);
            appendPoint(dest, temp);
        }
    }

    private void copyPath(FrontPath src, FrontPath dest) {
        FrontLinkedPoint temp = src.pFirst;
        if (src == dest) {
            return;
        }
        while (temp != null) {
            appendPoint(dest, temp.pValue);
            temp = temp.pNext;
        }
    }

    private Rectangle getPolyBounds(FrontPath pPath) {
        int limit = pPath.pLength;

        pushTransform();	// new transformation
        initMinMax(transformPoint(removePoint(pPath)));
        for (int i = 1; i < limit; i++) {
            minMax(transformPoint(removePoint(pPath)));
        }
        popTransform();
        Rectangle ret = new Rectangle(getMinMaxMinX(), getMinMaxMinY(), getMinMaxMaxX() - getMinMaxMinX(), getMinMaxMaxY() - getMinMaxMinY());
        doneMinMax();
        return ret;
    }

    private void minMax(Point foo) {
        if (foo.x > minMaxStackRight[minMaxStackPtr]) {
            minMaxStackRight[minMaxStackPtr] = foo.x;
        } else {
            if (foo.x < minMaxStackLeft[minMaxStackPtr]) {
                minMaxStackLeft[minMaxStackPtr] = foo.x;
            }
        }
        if (foo.y > minMaxStackTop[minMaxStackPtr]) {
            minMaxStackTop[minMaxStackPtr] = foo.y;
        } else {
            if (foo.y < minMaxStackBottom[minMaxStackPtr]) {
                minMaxStackBottom[minMaxStackPtr] = foo.y;
            }
        }
    }

    private void initMinMax(Point foo) {
        if (++minMaxStackPtr >= MAXMMSTACK) {
            errorReport("initMinMax: out of stack", FATALINTERNAL);
            return;
        }
        minMaxStackLeft[minMaxStackPtr] = foo.x;
        minMaxStackRight[minMaxStackPtr] = foo.x;
        minMaxStackBottom[minMaxStackPtr] = foo.y;
        minMaxStackTop[minMaxStackPtr] = foo.y;
    }

    private void doneMinMax() {
        if (minMaxStackPtr < 0) {
            errorReport("doneMinMax: pop from empty stack", FATALINTERNAL);
        } else {
            minMaxStackPtr--;
        }
    }

    private int getMinMaxMinX() {
        return minMaxStackLeft[minMaxStackPtr];
    }

    private int getMinMaxMinY() {
        return minMaxStackBottom[minMaxStackPtr];
    }

    private int getMinMaxMaxX() {
        return minMaxStackRight[minMaxStackPtr];
    }

    private int getMinMaxMaxY() {
        return minMaxStackTop[minMaxStackPtr];
    }

    private void pushTransform() {
        if (matrixStackTop.next == null) {
            matrixStackTop.next = new FrontMatrix();
            clearMatrix(matrixStackTop.next);
            matrixStackTop.next.prev = matrixStackTop;
            matrixStackTop = matrixStackTop.next;
            matrixStackTop.next = null;
        } else {
            matrixStackTop = matrixStackTop.next;
            clearMatrix(matrixStackTop);
        }
    }

    private void popTransform() {
        if (matrixStackTop.prev != null) {
            matrixStackTop = matrixStackTop.prev;
        } else {
            errorReport("pop, empty trans stack", FATALINTERNAL);
        }
    }

    private Point transformPoint(Point foo) {
        Point ans = new Point();

        if (!matrixStackTop.multiplied) {
            matrixMult(matrixStackTop, matrixStackTop.prev, matrixStackTop);
        }
        switch (matrixStackTop.type) {
            case TIDENT:
                return foo;
            case TTRANSLATE:
                ans.x = (int) matrixStackTop.a31;
                ans.y = (int) matrixStackTop.a32;
                ans.x += foo.x;
                ans.y += foo.y;
                return ans;
            case TMIRROR:
                ans.x = (matrixStackTop.a11 < 0) ? -foo.x : foo.x;
                ans.y = (matrixStackTop.a22 < 0) ? -foo.y : foo.y;
                return ans;
            case TROTATE:
                ans.x = (int) (foo.x * matrixStackTop.a11 + foo.y * matrixStackTop.a21);
                ans.y = (int) (foo.x * matrixStackTop.a12 + foo.y * matrixStackTop.a22);
                return ans;
            default:
                ans.x = (int) (matrixStackTop.a31 + foo.x * matrixStackTop.a11
                        + foo.y * matrixStackTop.a21);
                ans.y = (int) (matrixStackTop.a32 + foo.x * matrixStackTop.a12
                        + foo.y * matrixStackTop.a22);
        }
        return ans;
    }

    private void matrixMult(FrontMatrix l, FrontMatrix r, FrontMatrix result) {
        if (l == null || r == null || result == null) {
            errorReport("null arg to matrixMult", FATALINTERNAL);
        }
        if (result.multiplied) {
            errorReport("can't re-mult matrix", FATALINTERNAL);
            return;
        }
        if (!r.multiplied) {
            FrontMatrix temp = new FrontMatrix();
            temp.multiplied = false;
            matrixMult(r, r.prev, temp);
            matrixMultCore(l, temp, result);
        } else {
            matrixMultCore(l, r, result);
        }
    }

    private void matrixMultCore(FrontMatrix l, FrontMatrix r, FrontMatrix result) {
        if (l == null || r == null || result == null) {
            errorReport("null arg to matrixMultCore", FATALINTERNAL);
            return;
        }
        if (l.type == TIDENT) {
            assignMatrix(r, result);
        } else if (r.type == TIDENT) {
            assignMatrix(l, result);
        } else {
            FrontMatrix temp = new FrontMatrix();
            temp.a11 = l.a11 * r.a11 + l.a12 * r.a21;
            temp.a12 = l.a11 * r.a12 + l.a12 * r.a22;
            temp.a21 = l.a21 * r.a11 + l.a22 * r.a21;
            temp.a22 = l.a21 * r.a12 + l.a22 * r.a22;
            temp.a31 = l.a31 * r.a11 + l.a32 * r.a21 + l.a33 * r.a31;
            temp.a32 = l.a31 * r.a12 + l.a32 * r.a22 + l.a33 * r.a32;
            temp.a33 = l.a33 * r.a33;
            temp.type = l.type | r.type;
            assignMatrix(temp, result);
        }
        if (result.a33 != 1.0) {
            // divide by a33
            result.a11 /= result.a33;
            result.a12 /= result.a33;
            result.a21 /= result.a33;
            result.a22 /= result.a33;
            result.a31 /= result.a33;
            result.a32 /= result.a33;
            result.a33 = 1.0;
        }
        result.multiplied = true;
    }

    private void assignMatrix(FrontMatrix src, FrontMatrix dest) {
        dest.a11 = src.a11;
        dest.a12 = src.a12;
        dest.a21 = src.a21;
        dest.a22 = src.a22;
        dest.a31 = src.a31;
        dest.a32 = src.a32;
        dest.a33 = src.a33;
        dest.type = src.type;
        dest.multiplied = src.multiplied;
    }

    private Point removePoint(FrontPath a) {
        if (a.pFirst == null) {
            // added code to initialize return value with dummy numbers
            return new Point(0, 0);
        }
        FrontLinkedPoint temp = a.pFirst.pNext;
        Point ans = a.pFirst.pValue;
        a.pFirst = temp;
        if (a.pFirst == null) {
            a.pLast = null;
        }
        a.pLength -= 1;
        return ans;
    }

    public void makeInstanceName(String name) {
        if (ignoreStatements) {
            return;
        }
        if (name.length() == 0) {
            errorReport("null instance name ignored", ADVISORY);
            return;
        }
        if (namePending) {
            errorReport("there is already a name pending, new name replaces it", ADVISORY);
        }
        namePending = true;
        statementsSince91 = false;
    }

    public void processEnd() {
        statementsSince91 = true;
        endCommandFound = true;
        if (namePending) {
            errorReport("no instance to match name command", ADVISORY);
            namePending = false;
        }
    }

    void appendPoint(FrontPath a, Point p) {
        FrontLinkedPoint temp;

        temp = a.pLast;
        a.pLast = new FrontLinkedPoint();
        if (temp != null) {
            temp.pNext = a.pLast;
        }
        a.pLast.pValue = p;
        a.pLast.pNext = null;
        if (a.pFirst == null) {
            a.pFirst = a.pLast;
        }
        a.pLength += 1;
    }

    private void errorReport(String mess, int kind) {
//        if (charactersRead > 0) {
//            System.out.println("line " + (lineReader.getLineNumber() + (resetInputBuffer ? 0 : 1)) + ": " + inputBuffer.toString());
//        }
        if (kind == FATALINTERNAL || kind == FATALINTERNAL
                || kind == FATALSEMANTIC || kind == FATALOUTPUT) {
//            numFatalErrors++;
        }
        switch (kind) {
            case FATALINTERNAL:
                System.out.println("Fatal internal error: " + mess);
                break;
            case FATALSYNTAX:
                System.out.println("Syntax error: " + mess);
                break;
            case FATALSEMANTIC:
                System.out.println("Error: " + mess);
                break;
            case FATALOUTPUT:
                System.out.println("Output error: " + mess);
                break;
            case ADVISORY:
                System.out.println("Warning: " + mess);
                break;
            default:
                System.out.println(mess);
                break;
        }
    }
}
