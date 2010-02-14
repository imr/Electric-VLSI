/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VectorCache.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.redisplay;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellRevision;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.ImmutableIconInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.CellUsage;
import com.sun.electric.database.id.PrimitivePortId;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.AbstractShapeBuilder;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.Job;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to hold scalable representation of circuit displays.
 */
public class VectorCache {

    private static final boolean USE_ELECTRICAL = false;
    private static final boolean WIPE_PINS = true;
    public static boolean DEBUG = false;
    public static final VectorCache theCache = new VectorCache(EDatabase.clientDatabase());
    /** database to work. */
    public final EDatabase database;
    /** list of cell expansions. */
    private final ArrayList<VectorCellGroup> cachedCells = new ArrayList<VectorCellGroup>();
    /** list of polygons to include in cells */
    private final Map<CellId, List<VectorBase>> addPolyToCell = new HashMap<CellId, List<VectorBase>>();
    /** list of instances to include in cells */
    private final Map<CellId, List<VectorLine>> addInstToCell = new HashMap<CellId, List<VectorLine>>();
    /** local shape builder */
    private final ShapeBuilder shapeBuilder = new ShapeBuilder();
    /** List of VectorManhattanBuilders */
    private final ArrayList<VectorManhattanBuilder> boxBuilders = new ArrayList<VectorManhattanBuilder>();
    /** List of VectorManhattanBiilders for pure ndoes. */
    private final ArrayList<VectorManhattanBuilder> pureBoxBuilders = new ArrayList<VectorManhattanBuilder>();
    /** Current VarContext. */
    private VarContext varContext;
    /** Current scale. */
    private double curScale;
    /** True to clear fade images. */
    private boolean clearFadeImages;
    /** True to clear cache. */
    private boolean clearCache;
    /** zero rectangle */
    private final Rectangle2D CENTERRECT = new Rectangle2D.Double(0, 0, 0, 0);
    private EGraphics instanceGraphics = new EGraphics(false, false, null, 0, 0, 0, 0, 1.0, true,
            new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
    private final EditWindow0 dummyWnd = new EditWindow0()
    {
        double globalScale = User.getGlobalTextScale();
        String defaultFont = User.getDefaultFont();

        public VarContext getVarContext() { return varContext; }

        public double getScale() { return curScale; }

        public double getGlobalTextScale() { return globalScale; }

        public String getDefaultFont() { return defaultFont; }
    };

    /**
     * Class which defines the common information for all cached displayable objects
     */
    public static abstract class VectorBase {

        Layer layer;
        EGraphics graphicsOverride;

        VectorBase(Layer layer, EGraphics graphicsOverride) {
            this.layer = layer;
            this.graphicsOverride = graphicsOverride;
        }

        /**
         * Return true if this is a filled primitive.
         */
        boolean isFilled() {
            return false;
        }
    }

    /**
     * Class which defines a cached Manhattan rectangle.
     */
    static class VectorManhattan extends VectorBase {

        /** coordinates of boxes: 1X, 1Y, hX, hY */
        int[] coords;
        boolean pureLayer;

        private VectorManhattan(int[] coords, Layer layer, EGraphics graphicsOverride, boolean pureLayer) {
            super(layer, graphicsOverride);
            this.coords = coords;
            this.pureLayer = pureLayer;
        }

        VectorManhattan(double c1X, double c1Y, double c2X, double c2Y, Layer layer, EGraphics graphicsOverride, boolean pureLayer) {
            this(new int[]{databaseToGrid(c1X), databaseToGrid(c1Y), databaseToGrid(c2X), databaseToGrid(c2Y)},
                    layer, graphicsOverride, pureLayer);
        }

        @Override
        boolean isFilled() {
            return true;
        }
    }

    /**
     * Class which collects boxes for VectorManhattan.
     */
    static class VectorManhattanBuilder {

        /** Number of boxes. */
        int size; // number of boxes
        /** Coordiantes of boxes. */
        int[] coords = new int[4];

        private void add(int lX, int lY, int hX, int hY) {
            if (size * 4 >= coords.length) {
                int[] newCoords = new int[coords.length * 2];
                System.arraycopy(coords, 0, newCoords, 0, coords.length);
                coords = newCoords;
            }
            int i = size * 4;
            coords[i] = lX;
            coords[i + 1] = lY;
            coords[i + 2] = hX;
            coords[i + 3] = hY;
            size++;
        }

        int[] toArray() {
            int[] a = new int[size * 4];
            System.arraycopy(coords, 0, a, 0, a.length);
            return a;
        }

        private void clear() {
            size = 0;
        }
    }

    /**
     * Class which defines a cached polygon (nonmanhattan).
     */
    static class VectorPolygon extends VectorBase {

        Point[] points;

        VectorPolygon(Point2D[] points, Layer layer, EGraphics graphicsOverride) {
            super(layer, graphicsOverride);
            this.points = new Point[points.length];
            for (int i = 0; i < points.length; i++) {
                Point2D p = points[i];
                this.points[i] = new Point(databaseToGrid(p.getX()), databaseToGrid(p.getY()));
            }
        }

        @Override
        boolean isFilled() {
            return true;
        }
    }

    /**
     * Class which defines a cached line.
     */
    static class VectorLine extends VectorBase {

        int fX, fY, tX, tY;
        int texture;

        VectorLine(int fX, int fY, int tX, int tY, int texture, Layer layer, EGraphics graphicsOverride) {
            super(layer, graphicsOverride);
            this.fX = fX;
            this.fY = fY;
            this.tX = tX;
            this.tY = tY;
            this.texture = texture;
        }

        VectorLine(double fX, double fY, double tX, double tY, int texture, Layer layer, EGraphics graphicsOverride) {
            super(layer, graphicsOverride);
            this.fX = databaseToGrid(fX);
            this.fY = databaseToGrid(fY);
            this.tX = databaseToGrid(tX);
            this.tY = databaseToGrid(tY);
            this.texture = texture;
        }
    }

    /**
     * Class which defines a cached circle (filled, opened, or thick).
     */
    static class VectorCircle extends VectorBase {

        int cX, cY, eX, eY;
        int nature;

        VectorCircle(double cX, double cY, double eX, double eY, int nature, Layer layer, EGraphics graphicsOverride) {
            super(layer, graphicsOverride);
            this.cX = databaseToGrid(cX);
            this.cY = databaseToGrid(cY);
            this.eX = databaseToGrid(eX);
            this.eY = databaseToGrid(eY);
            this.nature = nature;
        }

        @Override
        boolean isFilled() {
            // true for disc nature
            return nature == 2;
        }
    }

    /**
     * Class which defines a cached arc of a circle (normal or thick).
     */
    static class VectorCircleArc extends VectorBase {

        int cX, cY, eX1, eY1, eX2, eY2;
        boolean thick;

        VectorCircleArc(double cX, double cY, double eX1, double eY1, double eX2, double eY2, boolean thick,
                Layer layer, EGraphics graphicsOverride) {
            super(layer, graphicsOverride);
            this.cX = databaseToGrid(cX);
            this.cY = databaseToGrid(cY);
            this.eX1 = databaseToGrid(eX1);
            this.eY1 = databaseToGrid(eY1);
            this.eX2 = databaseToGrid(eX2);
            this.eY2 = databaseToGrid(eY2);
            this.thick = thick;
        }
    }

    /**
     * Class which defines cached text.
     */
    static class VectorText extends VectorBase {

        /** text is on a Cell */
        static final int TEXTTYPECELL = 1;
        /** text is on an Export */
        static final int TEXTTYPEEXPORT = 2;
        /** text is on a Node */
        static final int TEXTTYPENODE = 3;
        /** text is on an Arc */
        static final int TEXTTYPEARC = 4;
        /** text is on an Annotations */
        static final int TEXTTYPEANNOTATION = 5;
        /** text is on an Instances */
        static final int TEXTTYPEINSTANCE = 6;
        /** the text location */
        Rectangle bounds;
        /** the text style */
        Poly.Type style;
        /** the descriptor of the text */
        TextDescriptor descript;
        /** the text to draw */
        String str;
        /** the text height (in display units) */
        float height;
        /** the type of text (CELL, EXPORT, etc.) */
        int textType;
        /** valid for export text */
        PrimitivePort basePort;

        VectorText(Rectangle2D bounds, Poly.Type style, TextDescriptor descript, String str, int textType, Export e,
                Layer layer) {
            super(layer, null);
            this.bounds = new Rectangle(databaseToGrid(bounds.getX()), databaseToGrid(bounds.getY()),
                    databaseToGrid(bounds.getWidth()), databaseToGrid(bounds.getHeight()));
            this.style = style;
            this.descript = descript;
            this.str = str;
            this.textType = textType;
            if (e != null) {
                basePort = e.getBasePort();
            }

            height = 1;
            if (descript != null) {
                TextDescriptor.Size tds = descript.getSize();
                if (!tds.isAbsolute()) {
                    height = (float) tds.getSize();
                }
            }
        }
    }

    /**
     * Class which defines a cached cross (a dot, large or small).
     */
    static class VectorCross extends VectorBase {

        int x, y;
        boolean small;

        VectorCross(double x, double y, boolean small, Layer layer, EGraphics graphicsOverride) {
            super(layer, graphicsOverride);
            this.x = databaseToGrid(x);
            this.y = databaseToGrid(y);
            this.small = small;
        }
    }

    /**
     * Class which defines a cached subcell reference.
     */
    static class VectorSubCell {

        ImmutableNodeInst n;
        CellId subCellId;
        int offsetX, offsetY;
        BitSet shownPorts = new BitSet();

        VectorSubCell(NodeInst ni, Point2D offset) {
            n = ni.getD();
            Cell subCell = (Cell) ni.getProto();
            subCellId = subCell.getId();
            offsetX = databaseToGrid(offset.getX());
            offsetY = databaseToGrid(offset.getY());
        }
    }

    /**
     * Class which holds the cell caches for a given cell.
     * Since each cell is cached many times, once for every orientation on the screen,
     * this object can hold many cell caches.
     */
    class VectorCellGroup {

        CellId cellId;
        ERectangle bounds;
        float cellArea;
        float cellMinSize;
        CellBackup cellBackup;
        boolean isParameterized;
        Map<Orientation, VectorCell> orientations = new HashMap<Orientation, VectorCell>();
        List<VectorCellExport> exports;

        VectorCellGroup(CellId cellId) {
            this.cellId = cellId;
            init();
            updateExports();
        }

        private void init() {
            updateBounds(database.backup());
            CellBackup cellBackup = database.backup().getCell(cellId);
            if (this.cellBackup == cellBackup) {
                return;
            }
            this.cellBackup = cellBackup;
            clear();
            isParameterized = isCellParameterized(cellBackup.cellRevision);
        }

        private boolean updateBounds(Snapshot snapshot) {
            ERectangle newBounds = snapshot.getCellBounds(cellId);
            if (newBounds == bounds) {
                return false;
            }
            bounds = newBounds;
            if (bounds != null) {
                cellArea = (float) (bounds.getWidth() * bounds.getHeight());
                cellMinSize = (float) Math.min(bounds.getWidth(), bounds.getHeight());
            } else {
                cellArea = 0;
                cellMinSize = 0;
            }
            for (VectorCell vc : orientations.values()) {
                vc.updateBounds();
            }
            return true;
        }

        private boolean changedExports() {
            Cell cell = database.getCell(cellId);
            if (cell == null) {
                return exports != null;
            }
            if (exports == null) {
                return true;
            }
            Iterator<VectorCellExport> cIt = exports.iterator();
            for (Iterator<Export> it = cell.getExports(); it.hasNext();) {
                Export e = it.next();
                if (!cIt.hasNext()) {
                    return true;
                }
                VectorCellExport vce = cIt.next();
                if (!vce.exportName.equals(e.getName())) {
                    return true;
                }
                Poly poly = e.getPoly();
                if (vce.exportCtr.getX() != poly.getCenterX()
                        || vce.exportCtr.getY() != poly.getCenterY()) {
                    return true;
                }
            }
            return cIt.hasNext();
        }

        private void updateExports() {
            for (VectorCell vc : orientations.values()) {
                vc.clearExports();
            }
            Cell cell = database.getCell(cellId);
            if (DEBUG) {
                System.out.println("updateExports " + cellId);
            }
            if (cell == null) {
                exports = null;
                return;
            }
            // save export centers to detect hierarchical changes later
            exports = new ArrayList<VectorCellExport>();
            for (Iterator<Export> it = cell.getExports(); it.hasNext();) {
                Export e = it.next();
                VectorCellExport vce = new VectorCellExport(e);
                exports.add(vce);
            }
        }

        List<VectorCellExport> getPortShapes() {
            if (exports == null) {
                updateExports();
            }
            return exports;
        }

        void clear() {
            for (VectorCell vc : orientations.values()) {
                vc.clear();
            }
            if (exports != null) {
                exports = null;
            }
            if (DEBUG) {
                System.out.println("clear " + cellId);
            }
        }

        VectorCell getAnyCell() {
            for (VectorCell vc : orientations.values()) {
                if (vc.valid) {
                    return vc;
                }
            }
            return null;
        }
    }

    VectorCellGroup findCellGroup(CellId cellId) {
        int cellIndex = cellId.cellIndex;
        while (cellIndex >= cachedCells.size()) {
            cachedCells.add(null);
        }
        VectorCellGroup vcg = cachedCells.get(cellIndex);
        if (vcg == null) {
            vcg = new VectorCellGroup(cellId);
            cachedCells.set(cellIndex, vcg);
        }
        return vcg;
    }

    /**
     * Class which defines the exports on a cell (used to tell if they changed)
     */
    static class VectorCellExport {

        String exportName;
        Point2D exportCtr;
        private ImmutableExport e;
        /** the text style */
        Poly.Type style;
        /** the descriptor of the text */
        TextDescriptor descript;
        /** the text height (in display units) */
        float height;
        private PrimitivePort basePort;

        VectorCellExport(Export e) {
            this.e = e.getD();
            exportName = e.getName();
            this.descript = this.e.nameDescriptor;
            Poly portPoly = e.getNamePoly();
            assert portPoly.getPoints().length == 1;
            exportCtr = portPoly.getPoints()[0];

            style = Poly.Type.TEXTCENT;
            height = 1;
            if (descript != null) {
//                    portDescript = portDescript.withColorIndex(descript.getColorIndex());
                style = descript.getPos().getPolyType();
                TextDescriptor.Size tds = descript.getSize();
                if (!tds.isAbsolute()) {
                    height = (float) tds.getSize();
                }
            }
            this.e = e.getD();
            basePort = e.getBasePort();
        }

        int getChronIndex() {
            return e.exportId.chronIndex;
        }

        String getName(boolean shortName) {
            String name = e.name.toString();
            if (shortName) {
                int len = name.length();
                for (int i = 0; i < len; i++) {
                    char ch = name.charAt(i);
                    if (TextUtils.isLetterOrDigit(ch)) {
                        continue;
                    }
                    return name.substring(0, i);
                }
            }
            return name;
        }

        PrimitivePort getBasePort() {
            return basePort;
        }
    }

    /**
     * Class which defines a cached cell in a single orientation.
     */
    class VectorCell {

        final VectorCellGroup vcg;
        final Orientation orient;
        int[] outlinePoints = new int[8];
        int lX, lY, hX, hY;
        int[] portCenters;
        boolean valid;
        ArrayList<VectorBase> shapes = new ArrayList<VectorBase>();
        private ArrayList<VectorBase> topOnlyShapes;
        ArrayList<VectorSubCell> subCells = new ArrayList<VectorSubCell>();
        boolean hasFadeColor;
        int fadeColor;
        float maxFeatureSize;
        boolean fadeImage;
        int fadeOffsetX, fadeOffsetY;
        int[] fadeImageColors;
        int fadeImageWid, fadeImageHei;

        VectorCell(VectorCellGroup vcg, Orientation orient) {
            this.vcg = vcg;
            this.orient = orient;
            updateBounds();
        }

        // Constructor for TechPalette
        private VectorCell() {
            vcg = null;
            orient = null;
        }

        private void init(Cell cell) {
            long startTime = DEBUG ? System.currentTimeMillis() : 0;
            vcg.init();
            updateBounds();
            clear();
            maxFeatureSize = 0;
            AffineTransform trans = orient.pureRotate();

            vcg.updateExports();

            for (VectorManhattanBuilder b : boxBuilders) {
                b.clear();
            }
            for (VectorManhattanBuilder b : pureBoxBuilders) {
                b.clear();
            }
            // draw all arcs
            shapeBuilder.setup(cell.backupUnsafe(), orient, USE_ELECTRICAL, WIPE_PINS, false, null);
            shapeBuilder.vc = this;
            shapeBuilder.hideOnLowLevel = false;
            shapeBuilder.textType = VectorText.TEXTTYPEARC;
            for (Iterator<ArcInst> arcs = cell.getArcs(); arcs.hasNext();) {
                ArcInst ai = arcs.next();
                drawArc(ai, trans, this);
            }

            // draw all primitive nodes
            for (Iterator<NodeInst> nodes = cell.getNodes(); nodes.hasNext();) {
                NodeInst ni = nodes.next();
                if (ni.isCellInstance()) {
                    continue;
                }
                boolean hideOnLowLevel = ni.isVisInside() || ni.getProto() == Generic.tech().cellCenterNode;
                if (!hideOnLowLevel) {
                    drawPrimitiveNode(ni, trans, this);
                }
            }

            // draw all subcells
            for (Iterator<NodeInst> nodes = cell.getNodes(); nodes.hasNext();) {
                NodeInst ni = nodes.next();
                if (!ni.isCellInstance()) {
                    continue;
                }
                drawSubcell(ni, trans, this);
            }

            // add in anything "snuck" onto the cell
            CellId cellId = cell.getId();
            List<VectorBase> addThesePolys = addPolyToCell.get(cellId);
            if (addThesePolys != null) {
                for (VectorBase vb : addThesePolys) {
                    shapes.add(vb);
                }
            }
            List<VectorLine> addTheseInsts = addInstToCell.get(cellId);
            if (addTheseInsts != null) {
                for (VectorLine vl : addTheseInsts) {
                    shapes.add(vl);
                }
            }
            addBoxesFromBuilder(this, cell.getTechnology(), boxBuilders, false);
            addBoxesFromBuilder(this, cell.getTechnology(), pureBoxBuilders, true);
            Collections.sort(shapes, shapeByLayer);

            // icon cells should not get greeked because of their contents
            if (cell.isIcon()) {
                maxFeatureSize = 0;
            }

            valid = true;
            if (DEBUG) {
                long stopTime = System.currentTimeMillis();
                System.out.println((stopTime - startTime) + " init " + vcg.cellId + " " + orient);
            }
        }

        private void updateBounds() {
            lX = lY = Integer.MAX_VALUE;
            hX = hY = Integer.MIN_VALUE;
            ERectangle bounds = vcg.bounds;
            if (bounds == null) {
                return;
            }
            double[] points = new double[8];
            points[0] = points[6] = bounds.getMinX();
            points[1] = points[3] = bounds.getMinY();
            points[2] = points[4] = bounds.getMaxX();
            points[5] = points[7] = bounds.getMaxY();
            orient.pureRotate().transform(points, 0, points, 0, 4);
            for (int i = 0; i < 4; i++) {
                int x = databaseToGrid(points[i * 2]);
                int y = databaseToGrid(points[i * 2 + 1]);
                lX = Math.min(lX, x);
                lY = Math.min(lY, y);
                hX = Math.max(hX, x);
                hY = Math.max(hY, y);
                outlinePoints[i * 2] = x;
                outlinePoints[i * 2 + 1] = y;
            }
        }

        private void clear() {
            clearExports();
            valid = hasFadeColor = fadeImage = false;
            shapes.clear();
            subCells.clear();
            fadeImageColors = null;
        }

        private void clearExports() {
            portCenters = null;
            topOnlyShapes = null;
        }

        int[] getPortCenters() {
            if (portCenters == null) {
                initPortCenters();
            }
            return portCenters;
        }

        private void initPortCenters() {
            List<VectorCellExport> portShapes = vcg.getPortShapes();
            portCenters = new int[portShapes.size() * 2];
            AffineTransform trans = orient.pureRotate();
            Point2D.Double tmpPt = new Point2D.Double();
            for (int i = 0, numPorts = portShapes.size(); i < numPorts; i++) {
                VectorCellExport vce = portShapes.get(i);
                trans.transform(vce.exportCtr, tmpPt);
                portCenters[i * 2] = databaseToGrid(tmpPt.getX());
                portCenters[i * 2 + 1] = databaseToGrid(tmpPt.getY());
            }
        }

        ArrayList<VectorBase> getTopOnlyShapes() {
            if (topOnlyShapes == null) {
                initTopOnlyShapes();
            }
            return topOnlyShapes;
        }

        private void initTopOnlyShapes() {
            Cell cell = database.getCell(vcg.cellId);
            if (cell == null) return;
            topOnlyShapes = new ArrayList<VectorBase>();
            // show cell variables
            Poly[] polys = cell.getDisplayableVariables(CENTERRECT, dummyWnd, true);
            drawPolys(polys, DBMath.MATID, this, true, VectorText.TEXTTYPECELL, false);

            // draw nodes visible only inside
            AffineTransform trans = orient.pureRotate();
            shapeBuilder.setup(cell.backupUnsafe(), orient, USE_ELECTRICAL, WIPE_PINS, false, null);
            shapeBuilder.vc = this;
            shapeBuilder.hideOnLowLevel = false;
            shapeBuilder.textType = VectorText.TEXTTYPEARC;
            for (Iterator<NodeInst> nodes = cell.getNodes(); nodes.hasNext();) {
                NodeInst ni = nodes.next();
                if (ni.isCellInstance()) {
                    continue;
                }
                boolean hideOnLowLevel = ni.isVisInside() || ni.getProto() == Generic.tech().cellCenterNode;
                if (hideOnLowLevel) {
                    drawPrimitiveNode(ni, trans, this);
                }
            }

            // draw exports and their variables
            for (Iterator<Export> it = cell.getExports(); it.hasNext();) {
                Export e = it.next();
                Poly poly = e.getNamePoly();
                Rectangle2D rect = (Rectangle2D) poly.getBounds2D().clone();
                TextDescriptor descript = poly.getTextDescriptor();
                Poly.Type style = descript.getPos().getPolyType();
//                style = Poly.rotateType(style, e.getOriginalPort().getNodeInst());
                style = Poly.rotateType(style, e);
                VectorText vt = new VectorText(poly.getBounds2D(), style, descript, e.getName(), VectorText.TEXTTYPEEXPORT, e, null);
                topOnlyShapes.add(vt);

                // draw variables on the export
                polys = e.getDisplayableVariables(rect, dummyWnd, true);
                drawPolys(polys, trans, this, true, VectorText.TEXTTYPEEXPORT, false);
            }
            Collections.sort(topOnlyShapes, shapeByLayer);
        }
//        private void clearTopOnlyShapes() {
//            topOnlyShapes = null;
//        }
    }

    private class ShapeBuilder extends AbstractShapeBuilder {

        private VectorCell vc;
        private boolean hideOnLowLevel;
        private int textType;
        private boolean pureLayer;

        @Override
        public void addDoublePoly(int numPoints, Poly.Type style, Layer layer, EGraphics graphicsOverride, PrimitivePort pp) {
            Point2D.Double[] points = new Point2D.Double[numPoints];
            for (int i = 0; i < numPoints; i++) {
                points[i] = new Point2D.Double(doubleCoords[i * 2], doubleCoords[i * 2 + 1]);
            }
            Poly poly = new Poly(points);
            poly.setStyle(style);
            poly.setLayer(layer);
            poly.setGraphicsOverride(graphicsOverride);
            poly.gridToLambda();
            renderPoly(poly, vc, hideOnLowLevel, textType, pureLayer);
        }

        @Override
        public void addDoubleTextPoly(int numPoints, Poly.Type style, Layer layer, PrimitivePort pp, String message, TextDescriptor descriptor) {
            Point2D.Double[] points = new Point2D.Double[numPoints];
            for (int i = 0; i < numPoints; i++) {
                points[i] = new Point2D.Double(doubleCoords[i * 2], doubleCoords[i * 2 + 1]);
            }
            Poly poly = new Poly(points);
            poly.setStyle(style);
            poly.setLayer(layer);
            poly.gridToLambda();
            poly.setString(message);
            poly.setTextDescriptor(descriptor);
            renderPoly(poly, vc, hideOnLowLevel, textType, pureLayer);
        }

        @Override
        public void addIntPoly(int numPoints, Poly.Type style, Layer layer, EGraphics graphicsOverride, PrimitivePort pp) {
            switch (style) {
                case OPENED:
                    addIntLine(0, layer, graphicsOverride);
                    break;
                case OPENEDT1:
                    addIntLine(1, layer, graphicsOverride);
                    break;
                case OPENEDT2:
                    addIntLine(2, layer, graphicsOverride);
                    break;
                case OPENEDT3:
                    addIntLine(3, layer, graphicsOverride);
                    break;
                default:
                    Point2D.Double[] points = new Point2D.Double[numPoints];
                    for (int i = 0; i < numPoints; i++) {
                        points[i] = new Point2D.Double(intCoords[i * 2], intCoords[i * 2 + 1]);
                    }
                    Poly poly = new Poly(points);
                    poly.setStyle(style);
                    poly.setLayer(layer);
                    poly.setGraphicsOverride(graphicsOverride);
                    poly.gridToLambda();
                    renderPoly(poly, vc, hideOnLowLevel, textType, pureLayer);
                    break;
            }
        }

        private void addIntLine(int lineType, Layer layer, EGraphics graphicsOverride) {
            int x1 = intCoords[0];
            int y1 = intCoords[1];
            int x2 = intCoords[2];
            int y2 = intCoords[3];
            VectorLine vl = new VectorLine(x1, y1, x2, y2, lineType, layer, graphicsOverride);
            ArrayList<VectorBase> shapes = hideOnLowLevel ? vc.topOnlyShapes : vc.shapes;
            shapes.add(vl);
        }

        @Override
        public void addIntBox(int[] coords, Layer layer) {
            ArrayList<VectorBase> shapes = hideOnLowLevel ? vc.topOnlyShapes : vc.shapes;
            // convert coordinates
            int lX = coords[0];
            int lY = coords[1];
            int hX = coords[2];
            int hY = coords[3];
            int layerIndex = -1;
            if (vc.vcg != null && layer.getId().techId == vc.vcg.cellBackup.cellRevision.d.techId) {
                layerIndex = layer.getIndex();
            }
            if (layerIndex >= 0) {
                putBox(layerIndex, pureLayer ? pureBoxBuilders : boxBuilders, lX, lY, hX, hY);
            } else {
                VectorManhattan vm = new VectorManhattan(new int[]{lX, lY, hX, hY}, layer, null, pureLayer);
                shapes.add(vm);
            }

            // ignore implant layers when computing largest feature size
            float minSize = (float) DBMath.gridToLambda(Math.min(hX - lX, hY - lY));
            if (layer != null) {
                Layer.Function fun = layer.getFunction();
                if (fun.isSubstrate()) {
                    minSize = 0;
                }
            }
            vc.maxFeatureSize = Math.max(vc.maxFeatureSize, minSize);
        }
    }

    /** Creates a new instance of VectorCache */
    public VectorCache(EDatabase database) {
        this.database = database;
    }

    VectorCell findVectorCell(CellId cellId, Orientation orient) {
        VectorCellGroup vcg = findCellGroup(cellId);
        orient = orient.canonic();
        VectorCell vc = vcg.orientations.get(orient);
        if (vc == null) {
            vc = new VectorCell(vcg, orient);
            vcg.orientations.put(orient, vc);
        }
        return vc;
    }

    VectorCell drawCell(CellId cellId, Orientation prevTrans, VarContext context, double scale) {
        VectorCell vc = findVectorCell(cellId, prevTrans);
        if (vc.vcg.isParameterized || !vc.valid) {
            varContext = vc.vcg.isParameterized ? context : null;
            curScale = scale; // Fix it later. Multiple Strings positioning shouldn't use scale.
            Cell cell = database.getCell(cellId);
            if (Job.getDebug() && cell == null)
                System.out.println("Cell is null in VectorCell.drawCell"); // extra testing
            if (cell != null && cell.isLinked())
                vc.init(database.getCell(cellId));
        }
        return vc;
    }

    public static VectorBase[] drawNode(NodeInst ni) {
        VectorCache cache = new VectorCache(EDatabase.clientDatabase());
        VectorCell vc = cache.newDummyVectorCell();
        cache.shapeBuilder.setup(ni.getCellBackupUnsafe(), null, USE_ELECTRICAL, WIPE_PINS, false, null);
        cache.shapeBuilder.vc = vc;
        cache.drawPrimitiveNode(ni, GenMath.MATID, vc);
        vc.shapes.addAll(vc.topOnlyShapes);
        Collections.sort(vc.shapes, shapeByLayer);
        return vc.shapes.toArray(new VectorBase[vc.shapes.size()]);
    }

    public static VectorBase[] drawPolys(ImmutableArcInst a, Poly[] polys) {
        VectorCache cache = new VectorCache(EDatabase.clientDatabase());
        VectorCell vc = cache.newDummyVectorCell();
        cache.drawPolys(polys, GenMath.MATID, vc, false, VectorText.TEXTTYPEARC, false);
        assert vc.topOnlyShapes.isEmpty();
        Collections.sort(vc.shapes, shapeByLayer);
        return vc.shapes.toArray(new VectorBase[vc.shapes.size()]);
    }

    private VectorCell newDummyVectorCell() {
        VectorCell vc = new VectorCell();
        vc.topOnlyShapes = new ArrayList<VectorBase>();
        return vc;
    }

    /**
     * Method to insert a manhattan rectangle into the vector cache for a Cell.
     * @param lX the low X of the manhattan rectangle.
     * @param lY the low Y of the manhattan rectangle.
     * @param hX the high X of the manhattan rectangle.
     * @param hY the high Y of the manhattan rectangle.
     * @param layer the layer on which to draw the rectangle.
     * @param cellId the Cell in which to insert the rectangle.
     */
    public void addBoxToCell(double lX, double lY, double hX, double hY, Layer layer, CellId cellId) {
        List<VectorBase> addToThisCell = addPolyToCell.get(cellId);
        if (addToThisCell == null) {
            addToThisCell = new ArrayList<VectorBase>();
            addPolyToCell.put(cellId, addToThisCell);
        }
        VectorManhattan vm = new VectorManhattan(lX, lY, hX, hY, layer, null, false);
        addToThisCell.add(vm);
    }

    /**
     * Method to insert a manhattan rectangle into the vector cache for a Cell.
     * @param lX the low X of the manhattan rectangle.
     * @param lY the low Y of the manhattan rectangle.
     * @param hX the high X of the manhattan rectangle.
     * @param hY the high Y of the manhattan rectangle.
     * @param cellId the Cell in which to insert the rectangle.
     */
    public void addInstanceToCell(double lX, double lY, double hX, double hY, CellId cellId) {
        List<VectorLine> addToThisCell = addInstToCell.get(cellId);
        if (addToThisCell == null) {
            addToThisCell = new ArrayList<VectorLine>();
            addInstToCell.put(cellId, addToThisCell);
        }

        // store the subcell
        addToThisCell.add(new VectorLine(lX, lY, hX, lY, 0, null, instanceGraphics));
        addToThisCell.add(new VectorLine(hX, lY, hX, hY, 0, null, instanceGraphics));
        addToThisCell.add(new VectorLine(hX, hY, lX, hY, 0, null, instanceGraphics));
        addToThisCell.add(new VectorLine(lX, hY, lX, lY, 0, null, instanceGraphics));
    }
    private static final PrimitivePortId busPinPortId = Schematics.tech().busPinNode.getPort(0).getId();

    /**
     * Method to tell whether a Cell is parameterized.
     * Code is taken from tool.drc.Quick.checkEnumerateProtos
     * Could also use the code in tool.io.output.Spice.checkIfParameterized
     * @param cellRevision the Cell to examine
     * @return true if the cell has parameters
     */
    private static boolean isCellParameterized(CellRevision cellRevision) {
        if (cellRevision.d.getNumParameters() > 0) {
            return true;
        }

        // look for any Java coded stuff (Logical Effort calls)
        for (ImmutableNodeInst n : cellRevision.nodes) {
            if (n instanceof ImmutableIconInst) {
                for (Iterator<Variable> vIt = ((ImmutableIconInst) n).getDefinedParameters(); vIt.hasNext();) {
                    Variable var = vIt.next();
                    if (var.isCode()) {
                        return true;
                    }
                }
            }
            for (Iterator<Variable> vIt = n.getVariables(); vIt.hasNext();) {
                Variable var = vIt.next();
                if (var.isCode()) {
                    return true;
                }
            }
        }
        for (ImmutableArcInst a : cellRevision.arcs) {
            for (Iterator<Variable> vIt = a.getVariables(); vIt.hasNext();) {
                Variable var = vIt.next();
                if (var.isCode()) {
                    return true;
                }
            }
        }

        // bus pin appearance depends on parent Cell
        for (ImmutableExport e : cellRevision.exports) {
            if (e.originalPortId == busPinPortId) {
                return true;
            }
        }
        return false;
    }

    public Set<CellId> forceRedrawAfterChange(Set<CellId> topCells) {
        BitSet visibleCells = new BitSet();
        for (CellId cellId : topCells) {
            if (database.getCell(cellId) == null) {
                continue;
            }
            markDown(cellId, visibleCells);
        }

        // deleted cells
        for (int cellIndex = 0; cellIndex < cachedCells.size(); cellIndex++) {
            if (cachedCells.get(cellIndex) != null && !visibleCells.get(cellIndex)) {
                cachedCells.set(cellIndex, null);
            }
        }

        // changed cells
        Snapshot snapshot = database.backup();
//        BitSet changedCells = new BitSet();
        BitSet changedExports = new BitSet();
        BitSet changedBounds = new BitSet();
        BitSet changedParams = new BitSet();
        Set<CellId> changedVisibility = new HashSet<CellId>();
        for (CellId cellId : snapshot.getCellsDownTop()) {
            int cellIndex = cellId.cellIndex;
            if (!visibleCells.get(cellIndex)) {
                continue;
            }
            while (cellIndex >= cachedCells.size()) {
                cachedCells.add(null);
            }
            VectorCellGroup vcg = cachedCells.get(cellIndex);
            boolean changedVis = false;
            if (vcg == null) {
                vcg = new VectorCellGroup(cellId);
//                changedCells.set(cellIndex);
                changedExports.set(cellIndex);
                changedBounds.set(cellIndex);
                if (cellId.isIcon()) {
                    changedParams.set(cellIndex);
                }
                changedVis = true;
            } else if (vcg.cellBackup != snapshot.getCell(cellId)) {
                if (vcg.changedExports()) {
                    changedExports.set(cellIndex);
                }
                if (vcg.updateBounds(snapshot)) {
                    changedBounds.set(cellIndex);
                }
                if (cellId.isIcon()) {
                    changedParams.set(cellIndex);
                }
                vcg.init();
//                changedCells.set(cellIndex);
                changedVis = true;
            } else {
                CellRevision cellRevision = snapshot.getCell(cellId).cellRevision;
                int[] instCounts = cellRevision.getInstCounts();
                boolean subExportsChanged = false;
                boolean subParamsChanged = false;
                for (int i = 0; i < instCounts.length; i++) {
                    if (instCounts[i] == 0) {
                        continue;
                    }
                    int subCellIndex = cellId.getUsageIn(i).protoId.cellIndex;
                    if (changedExports.get(subCellIndex)) {
                        subExportsChanged = true;
                    }
                    if (changedParams.get(subCellIndex)) {
                        subParamsChanged = true;
                    }
                }
                if (vcg.updateBounds(snapshot)) {
                    changedBounds.set(cellIndex);
                    changedVis = true;
                }
                if (subExportsChanged && vcg.changedExports()) {
                    changedExports.set(cellIndex);
                    vcg.updateExports();
                    changedVis = true;
                }
                if (subParamsChanged) {
                    vcg.clear();
                }
                if (!changedVis) {
                    Cell cell = database.getCell(cellId);
                    for (ImmutableNodeInst n : cellRevision.nodes) {
                        if (!(n.protoId instanceof CellId)) {
                            continue;
                        }
                        CellId subCellId = (CellId) n.protoId;
                        int subCellIndex = subCellId.cellIndex;
                        if (cell.isExpanded(n.nodeId)) {
                            if (changedVisibility.contains(subCellId)) {
                                changedVis = true;
                                break;
                            }
                        } else if (changedBounds.get(subCellIndex) || changedExports.get(subCellIndex)) {
                            changedVis = true;
                            break;
                        }
                    }
                }
            }
            if (changedVis) {
                changedVisibility.add(cellId);
            }
        }
        return changedVisibility;
    }

    private void markDown(CellId cellId, BitSet visibleCells) {
        if (visibleCells.get(cellId.cellIndex)) {
            return;
        }
        visibleCells.set(cellId.cellIndex);
        Cell cell = database.getCell(cellId);
        for (Iterator<CellUsage> it = cell.getUsagesIn(); it.hasNext();) {
            CellUsage cu = it.next();
            markDown(cu.protoId, visibleCells);
        }
    }

    void forceRedraw() {
        boolean clearCache, clearFadeImages;
        synchronized (this) {
            clearCache = this.clearCache;
            this.clearCache = false;
            clearFadeImages = this.clearFadeImages;
            this.clearFadeImages = false;
        }
        Snapshot snapshot = database.backup();
        for (int cellIndex = 0, size = cachedCells.size(); cellIndex < size; cellIndex++) {
            VectorCellGroup vcg = cachedCells.get(cellIndex);
            if (vcg == null) {
                continue;
            }
            if (clearCache) {
                vcg.clear();
            }
            if (clearFadeImages) {
                for (VectorCell vc : vcg.orientations.values()) {
                    vc.fadeImageColors = null;
                    vc.fadeImage = false;
                }
            }
            assert vcg.bounds == snapshot.getCellBounds(cellIndex);
//            vcg.updateBounds(snapshot);
//            if (!changedCells.contains(vcg.cellId) && vcg.cellBackup == snapshot.getCell(cellIndex)) continue;
//            cellChanged(vcg.cellId);
        }
    }

//	/**
//	 * Method called when a cell changes: removes any cached displays of that cell
//	 * @param cell the cell that changed
//	 */
//	private void cellChanged(CellId cellId)
//	{
//		VectorCellGroup vcg = null;
//        if (cellId.cellIndex < cachedCells.size())
//            vcg = cachedCells.get(cellId.cellIndex);
//        Cell cell = database.getCell(cellId);
//		if (cell != null)
//		{
//			// cell still valid: see if it changed from last cache
//			if (vcg != null)
//			{
//				// queue parent cells for recaching if the bounds or exports changed
//				if (vcg.changedExports())
//				{
//                    vcg.updateExports();
//					for(Iterator<CellUsage> it = cell.getUsagesOf(); it.hasNext(); )
//					{
//	                    CellUsage u = it.next();
//						cellChanged(u.parentId);
//					}
//				}
//			}
//		}
////System.out.println("REMOVING CACHE FOR CELL "+cell);
//        if (vcg != null)
//            vcg.clear();
//	}
    /**
     * Method called when it is necessary to clear cache.
     */
    public synchronized void clearCache() {
        clearCache = true;
    }

    /**
     * Method called when visible layers have changed.
     * Removes all "greeked images" from cached cells.
     */
    public synchronized void clearFadeImages() {
        clearFadeImages = true;
    }

    private static int databaseToGrid(double lambdaValue) {
        return (int) DBMath.lambdaToGrid(lambdaValue);
    }

    private void addBoxesFromBuilder(VectorCell vc, Technology tech, ArrayList<VectorManhattanBuilder> boxBuilders, boolean pureArray) {
        for (int layerIndex = 0; layerIndex < boxBuilders.size(); layerIndex++) {
            VectorManhattanBuilder b = boxBuilders.get(layerIndex);
            if (b.size == 0) {
                continue;
            }
            Layer layer = tech.getLayer(layerIndex);
            VectorManhattan vm = new VectorManhattan(b.toArray(), layer, null, pureArray);
            vc.shapes.add(vm);
        }
    }
    /**
     * Comparator class for sorting VectorBase objects by their layer depth.
     */
    public static Comparator<VectorBase> shapeByLayer = new Comparator<VectorBase>() {

        /**
         * Method to sort Objects by their string name.
         */
        public int compare(VectorBase vb1, VectorBase vb2) {
            if (vb1.isFilled() != vb2.isFilled()) {
                return vb1.isFilled() ? 1 : -1;
            }
            int level1 = 1000, level2 = 1000;
            boolean isContact1 = false;
            boolean isContact2 = false;
            if (vb1.layer != null) {
                Layer.Function fun = vb1.layer.getFunction();
                level1 = fun.getLevel();
                isContact1 = fun.isContact();
            }
            if (vb2.layer != null) {
                Layer.Function fun = vb2.layer.getFunction();
                level2 = fun.getLevel();
                isContact2 = fun.isContact();
            }
            if (isContact1 != isContact2) {
                return isContact1 ? -1 : 1;
            }
            return level1 - level2;
        }
    };

    /**
     * Method to cache a NodeInst.
     * @param ni the NodeInst to cache.
     * @param trans the transformation of the NodeInst to the parent Cell.
     * @param vc the cached cell in which to place the NodeInst.
     */
    private void drawSubcell(NodeInst ni, AffineTransform trans, VectorCell vc) {
        AffineTransform localTrans = ni.rotateOut(trans);

        // draw the node
        assert ni.isCellInstance();
        // cell instance: record a call to the instance
        Point2D ctrShift = new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY());
        localTrans.transform(ctrShift, ctrShift);
        VectorSubCell vsc = new VectorSubCell(ni, ctrShift);
        vc.subCells.add(vsc);

        // show the ports that are not further exported or connected
        for (Iterator<Connection> it = ni.getConnections(); it.hasNext();) {
            Connection con = it.next();
            PortInst pi = con.getPortInst();
            Export e = (Export) pi.getPortProto();
            if (!e.isAlwaysDrawn()) {
                vsc.shownPorts.set(e.getId().getChronIndex());
            }
        }
        for (Iterator<Export> it = ni.getExports(); it.hasNext();) {
            Export exp = it.next();
            PortInst pi = exp.getOriginalPort();
            Export e = (Export) pi.getPortProto();
            if (!e.isAlwaysDrawn()) {
                vsc.shownPorts.set(e.getId().getChronIndex());
            }
        }

        // draw any displayable variables on the instance
        Poly[] polys = ni.getDisplayableVariables(dummyWnd);
        drawPolys(polys, localTrans, vc, false, VectorText.TEXTTYPENODE, false);
    }

    /**
     * Method to cache a NodeInst.
     * @param ni the NodeInst to cache.
     * @param trans the transformation of the NodeInst to the parent Cell.
     * @param vc the cached cell in which to place the NodeInst.
     */
    private void drawPrimitiveNode(NodeInst ni, AffineTransform trans, VectorCell vc) {
        NodeProto np = ni.getProto();
        AffineTransform localTrans = ni.rotateOut(trans);

        // draw the node
        assert !ni.isCellInstance();
        // primitive: save it
        PrimitiveNode prim = (PrimitiveNode) np;
        shapeBuilder.textType = prim == Generic.tech().invisiblePinNode ? VectorText.TEXTTYPEANNOTATION : VectorText.TEXTTYPENODE;
        shapeBuilder.pureLayer = (ni.getFunction() == PrimitiveNode.Function.NODE);
        shapeBuilder.hideOnLowLevel = ni.isVisInside() || np == Generic.tech().cellCenterNode;
        shapeBuilder.genShapeOfNode(ni.getD());
        drawPolys(ni.getDisplayableVariables(dummyWnd), localTrans, vc, shapeBuilder.hideOnLowLevel, shapeBuilder.textType, shapeBuilder.pureLayer);
    }

    /**
     * Method to cache an ArcInst.
     * @param ai the ArcInst to cache.
     * @param trans the transformation of the ArcInst to the parent cell.
     * @param vc the cached cell in which to place the ArcInst.
     */
    private void drawArc(ArcInst ai, AffineTransform trans, VectorCell vc) {
        // draw the arc
        ArcProto ap = ai.getProto();
        shapeBuilder.pureLayer = (ap.getNumArcLayers() == 1);
        shapeBuilder.genShapeOfArc(ai.getD());
        drawPolys(ai.getDisplayableVariables(dummyWnd), trans, vc, false, VectorText.TEXTTYPEARC, false);
    }

    /**
     * Method to cache an array of polygons.
     * @param polys the array of polygons to cache.
     * @param trans the transformation to apply to each polygon.
     * @param vc the cached cell in which to place the polygons.
     * @param hideOnLowLevel true if the polygons should be marked such that they are not visible on lower levels of hierarchy.
     * @param pureLayer true if these polygons come from a pure layer node.
     */
    private void drawPolys(Poly[] polys, AffineTransform trans, VectorCell vc, boolean hideOnLowLevel, int textType, boolean pureLayer) {
        if (polys == null) {
            return;
        }
        for (int i = 0; i < polys.length; i++) {
            // get the polygon and transform it
            Poly poly = polys[i];
            if (poly == null) {
                continue;
            }

            // transform the bounds
            poly.transform(trans);

            // render the polygon
            renderPoly(poly, vc, hideOnLowLevel, textType, pureLayer);
        }
    }

    /**
     * Method to cache a Poly.
     * @param poly the polygon to cache.
     * @param vc the cached cell in which to place the polygon.
     * @param hideOnLowLevel true if the polygon should be marked such that it is not visible on lower levels of hierarchy.
     * @param pureLayer true if the polygon comes from a pure layer node.
     */
    private void renderPoly(Poly poly, VectorCell vc, boolean hideOnLowLevel, int textType, boolean pureLayer) {
        // now draw it
        Point2D[] points = poly.getPoints();
        Layer layer = poly.getLayer();
        EGraphics graphicsOverride = poly.getGraphicsOverride();
        Poly.Type style = poly.getStyle();
        ArrayList<VectorBase> shapes = hideOnLowLevel ? vc.topOnlyShapes : vc.shapes;
        if (style == Poly.Type.FILLED) {
            Rectangle2D bounds = poly.getBox();
            if (bounds != null) {
                // convert coordinates
                double lX = bounds.getMinX();
                double hX = bounds.getMaxX();
                double lY = bounds.getMinY();
                double hY = bounds.getMaxY();
                float minSize = (float) Math.min(hX - lX, hY - lY);
                int layerIndex = -1;
                if (layer != null && graphicsOverride == null) {
                    if (vc.vcg != null && layer.getId().techId == vc.vcg.cellBackup.cellRevision.d.techId) {
                        layerIndex = layer.getIndex();
                    }
                }
                if (layerIndex >= 0) {
                    putBox(layerIndex, pureLayer ? pureBoxBuilders : boxBuilders,
                            databaseToGrid(lX), databaseToGrid(lY), databaseToGrid(hX), databaseToGrid(hY));
                } else {
                    VectorManhattan vm = new VectorManhattan(lX, lY, hX, hY, layer, graphicsOverride, pureLayer);
                    shapes.add(vm);
                }

                // ignore implant layers when computing largest feature size
                if (layer != null) {
                    Layer.Function fun = layer.getFunction();
                    if (fun.isSubstrate()) {
                        minSize = 0;
                    }
                }
                vc.maxFeatureSize = Math.max(vc.maxFeatureSize, minSize);
                return;
            }
            VectorPolygon vp = new VectorPolygon(points, layer, graphicsOverride);
            shapes.add(vp);
            return;
        }
        if (style == Poly.Type.CROSSED) {
            VectorLine vl1 = new VectorLine(points[0].getX(), points[0].getY(),
                    points[1].getX(), points[1].getY(), 0, layer, graphicsOverride);
            VectorLine vl2 = new VectorLine(points[1].getX(), points[1].getY(),
                    points[2].getX(), points[2].getY(), 0, layer, graphicsOverride);
            VectorLine vl3 = new VectorLine(points[2].getX(), points[2].getY(),
                    points[3].getX(), points[3].getY(), 0, layer, graphicsOverride);
            VectorLine vl4 = new VectorLine(points[3].getX(), points[3].getY(),
                    points[0].getX(), points[0].getY(), 0, layer, graphicsOverride);
            VectorLine vl5 = new VectorLine(points[0].getX(), points[0].getY(),
                    points[2].getX(), points[2].getY(), 0, layer, graphicsOverride);
            VectorLine vl6 = new VectorLine(points[1].getX(), points[1].getY(),
                    points[3].getX(), points[3].getY(), 0, layer, graphicsOverride);
            shapes.add(vl1);
            shapes.add(vl2);
            shapes.add(vl3);
            shapes.add(vl4);
            shapes.add(vl5);
            shapes.add(vl6);
            return;
        }
        if (style.isText()) {
            Rectangle2D bounds = poly.getBounds2D();
            TextDescriptor descript = poly.getTextDescriptor();
            String str = poly.getString();
            assert graphicsOverride == null;
            VectorText vt = new VectorText(bounds, style, descript, str, textType, null, layer);
            shapes.add(vt);
            vc.maxFeatureSize = Math.max(vc.maxFeatureSize, vt.height);
            return;
        }
        if (style == Poly.Type.CLOSED || style == Poly.Type.OPENED || style == Poly.Type.OPENEDT1
                || style == Poly.Type.OPENEDT2 || style == Poly.Type.OPENEDT3) {
            int lineType = 0;
            if (style == Poly.Type.OPENEDT1) {
                lineType = 1;
            } else if (style == Poly.Type.OPENEDT2) {
                lineType = 2;
            } else if (style == Poly.Type.OPENEDT3) {
                lineType = 3;
            }

            for (int j = 1; j < points.length; j++) {
                Point2D oldPt = points[j - 1];
                Point2D newPt = points[j];
                VectorLine vl = new VectorLine(oldPt.getX(), oldPt.getY(),
                        newPt.getX(), newPt.getY(), lineType, layer, graphicsOverride);
                shapes.add(vl);
            }
            if (style == Poly.Type.CLOSED) {
                Point2D oldPt = points[points.length - 1];
                Point2D newPt = points[0];
                VectorLine vl = new VectorLine(oldPt.getX(), oldPt.getY(),
                        newPt.getX(), newPt.getY(), lineType, layer, graphicsOverride);
                shapes.add(vl);
            }
            return;
        }
        if (style == Poly.Type.VECTORS) {
            for (int j = 0; j < points.length; j += 2) {
                Point2D oldPt = points[j];
                Point2D newPt = points[j + 1];
                VectorLine vl = new VectorLine(oldPt.getX(), oldPt.getY(),
                        newPt.getX(), newPt.getY(), 0, layer, graphicsOverride);
                shapes.add(vl);
            }
            return;
        }
        if (style == Poly.Type.CIRCLE) {
            VectorCircle vci = new VectorCircle(points[0].getX(), points[0].getY(),
                    points[1].getX(), points[1].getY(), 0, layer, graphicsOverride);
            shapes.add(vci);
            return;
        }
        if (style == Poly.Type.THICKCIRCLE) {
            VectorCircle vci = new VectorCircle(points[0].getX(), points[0].getY(),
                    points[1].getX(), points[1].getY(), 1, layer, graphicsOverride);
            shapes.add(vci);
            return;
        }
        if (style == Poly.Type.DISC) {
            VectorCircle vci = new VectorCircle(points[0].getX(), points[0].getY(), points[1].getX(),
                    points[1].getY(), 2, layer, graphicsOverride);
            shapes.add(vci);
            return;
        }
        if (style == Poly.Type.CIRCLEARC || style == Poly.Type.THICKCIRCLEARC) {
            VectorCircleArc vca = new VectorCircleArc(points[0].getX(), points[0].getY(),
                    points[1].getX(), points[1].getY(), points[2].getX(), points[2].getY(),
                    style == Poly.Type.THICKCIRCLEARC, layer, graphicsOverride);
            shapes.add(vca);
            return;
        }
        if (style == Poly.Type.CROSS) {
            // draw the cross
            VectorCross vcr = new VectorCross(points[0].getX(), points[0].getY(), true, layer, graphicsOverride);
            shapes.add(vcr);
            return;
        }
        if (style == Poly.Type.BIGCROSS) {
            // draw the big cross
            VectorCross vcr = new VectorCross(points[0].getX(), points[0].getY(), false, layer, graphicsOverride);
            shapes.add(vcr);
            return;
        }
    }

    private static void putBox(int layerIndex, ArrayList<VectorManhattanBuilder> boxBuilders, int lX, int lY, int hX, int hY) {
        while (layerIndex >= boxBuilders.size()) {
            boxBuilders.add(new VectorManhattanBuilder());
        }
        VectorManhattanBuilder b = boxBuilders.get(layerIndex);
        b.add(lX, lY, hX, hY);
    }
//    /*------------------------------------------------------*/
//
//    public void showStatistics(Layer layer) {
//        Map<Layer,GenMath.MutableInteger> totalLayerBag = new TreeMap<Layer,GenMath.MutableInteger>(Layer.layerSortByName);
//        int numCells = 0, numCellLayer = 0;
//        int totalNoBox = 0, totalNoPoly = 0, totalNoDisc = 0, totalNoShapes = 0, totalNoText = 0, totalNoTopShapes = 0, totalNoTopText = 0;
//        for (VectorCellGroup vcg: cachedCells) {
//            if (vcg == null) continue;
//            VectorCell vc = vcg.getAnyCell();
//            numCells++;
//            Map<Layer,GenMath.MutableInteger> layerBag = new TreeMap<Layer,GenMath.MutableInteger>(Layer.layerSortByName);
//            int noText = 0, noTopText = 0;
//            for (VectorBase vs: vc.filledShapes) {
//                if (vs instanceof VectorManhattan)
//                    totalNoBox++;
//                else if (vs instanceof VectorPolygon)
//                    totalNoPoly++;
//                else if (vs instanceof VectorCircle)
//                    totalNoDisc++;
//                assert vs.layer != null;
//                GenMath.addToBag(layerBag, vs.layer);
//            }
//            numCellLayer += layerBag.size();
//            GenMath.addToBag(totalLayerBag, layerBag);
//            for (VectorBase vs: vc.shapes) {
//                if (vs instanceof VectorText)
//                    noText++;
//            }
//            totalNoShapes += vc.shapes.size();
//            totalNoText += noText;
//            for (VectorBase vs: vc.topOnlyShapes) {
//                if (vs instanceof VectorText)
//                    noTopText++;
//            }
//            totalNoTopShapes += vc.topOnlyShapes.size();
//            totalNoTopText += noTopText;
//            System.out.print(vcg.cellBackup.d.cellName + " " + vcg.orientations.size() + " ors " + vc.subCells.size() + " subs " +
//                    vc.topOnlyShapes.size() + " topOnlyShapes(" + noTopText + " text) " +
//                    vc.shapes.size() + " shapes(" + noText + " text) " +
//                    vc.filledShapes.size() + " filledShapes ");
//            for (Map.Entry<Layer,GenMath.MutableInteger> e: layerBag.entrySet())
//                System.out.print(" " + e.getKey().getName() + ":" + e.getValue());
//            System.out.println();
//        }
//        System.out.println(numCells + " cells " + numCellLayer + " cellLayes");
//        System.out.println("Top shapes " + totalNoTopShapes + " (" + totalNoTopText + " text)");
//        System.out.println("Shapes " + totalNoShapes + " (" + totalNoText + " text)");
//        System.out.print("FilledShapes " + (totalNoBox + totalNoPoly + totalNoDisc) + " (" +
//                totalNoBox + " boxes " + totalNoPoly + " polys " + totalNoDisc + " discs  ");
//        for (Map.Entry<Layer,GenMath.MutableInteger> e: totalLayerBag.entrySet())
//            System.out.print(" " + e.getKey().getName() + ":" + e.getValue());
//        System.out.println();
//
//        EditWindow wnd = (EditWindow)Job.getUserInterface().needCurrentEditWindow_();
//        VectorCellGroup topCell = cachedCells.get(wnd.getCell().getCellIndex());
//        HashMap<VectorCell,LayerDrawing.LayerCell> layerCells = new HashMap<VectorCell,LayerDrawing.LayerCell>();
//        LayerDrawing ld = new LayerDrawing(layer);
//        ld.topCell = gatherLayer(topCell, Orientation.IDENT, layer, ld, layerCells);
//        System.out.println(layerCells.size() + " layerCells");
//        ld.draw(wnd);
//    }
//
//    private LayerDrawing.LayerCell gatherLayer(VectorCellGroup vcg, Orientation or, Layer layer, LayerDrawing ld, HashMap<VectorCell,LayerDrawing.LayerCell> layerCells) {
//        VectorCell vc = vcg.orientations.get(or.canonic());
//        if (vc == null || !vc.valid) return null;
//        LayerDrawing.LayerCell lc = layerCells.get(vc);
//        if (lc == null) {
//            lc = ld.newCell();
//            layerCells.put(vc, lc);
//            for (VectorBase vs: vc.filledShapes) {
//                if (vs.layer == layer && vs instanceof VectorManhattan) {
//                    VectorManhattan vm = (VectorManhattan)vs;
//                    for (int i = 0; i < vm.coords.length; i += 4) {
//                        int c1X = vm.coords[i];
//                        int c1Y = vm.coords[i + 1];
//                        int c2X = vm.coords[i + 2];
//                        int c2Y = vm.coords[i + 3];
//                        lc.rects.add(new Rectangle2D.Float(c1X, c1Y, c2X - c1X, c2Y - c1Y));
//                    }
//                }
//            }
//            for (VectorSubCell vsc: vc.subCells) {
//                VectorCellGroup subVC = cachedCells.get(vsc.subCell.getCellIndex());
//                if (subVC == null) continue;
//				Orientation recurseTrans = or.concatenate(vsc.pureRotate);
//                LayerDrawing.LayerCell proto = gatherLayer(subVC, recurseTrans, layer, ld, layerCells);
//                if (proto == null) continue;
//                lc.addSubCell(proto, vsc.offsetX, vsc.offsetY);
//            }
//        }
//        return lc;
//    }
}
