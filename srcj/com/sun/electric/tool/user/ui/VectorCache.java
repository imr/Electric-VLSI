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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellId;
import com.sun.electric.database.CellUsage;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class VectorCache {
    
    public static final VectorCache theCache = new VectorCache(EDatabase.clientDatabase());
    
    /** database to work. */                                private final EDatabase database; 
	/** list of cell expansions. */							private final ArrayList<VectorCellGroup> cachedCells = new ArrayList<VectorCellGroup>();
	/** list of polygons to include in cells */				private final HashMap<CellId,List<VectorBase>> addPolyToCell = new HashMap<CellId,List<VectorBase>>();
	/** list of instances to include in cells */			private final HashMap<CellId,List<VectorLine>> addInstToCell = new HashMap<CellId,List<VectorLine>>();
    /** List of VectorManhattanBuilders */                  private final ArrayList<VectorManhattanBuilder>boxBuilders = new ArrayList<VectorManhattanBuilder>();
    /** List of VectorManhattanBiilders for pure ndoes. */  private final ArrayList<VectorManhattanBuilder>pureBoxBuilders = new ArrayList<VectorManhattanBuilder>();
    /** Current VarContext. */                              private VarContext varContext;
    /** Current scale. */                                   private double curScale;
    /** True to clear fade images. */                       private boolean clearFadeImages;
    /** True to clear cache. */                             private boolean clearCache;
    
	/** temporary objects (saves allocation) */				private final Point tempPt1 = new Point();
	/** zero rectangle */									private final Rectangle2D CENTERRECT = new Rectangle2D.Double(0, 0, 0, 0);
	private EGraphics instanceGraphics = new EGraphics(false, false, null, 0, 0,0,0, 1.0,true,
			new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});
    
    private final EditWindow0 dummyWnd = new EditWindow0() {
        public VarContext getVarContext() { return varContext; }
        
        public double getScale() { return curScale; }
    };

	/**
	 * Class which defines the common information for all cached displayable objects
	 */
    static abstract class VectorBase
	{
		Layer layer;
		EGraphics graphics;

		VectorBase(Layer layer, EGraphics graphics)
		{
			this.layer = layer;
			this.graphics = graphics;
		}
        
        /**
         * Return true if this is a filled primitive.
         */
        boolean isFilled() { return false; }
	}

	/**
	 * Class which defines a cached Manhattan rectangle.
	 */
	static class VectorManhattan extends VectorBase
	{
        /** coordinates of boxes: 1X, 1Y, hX, hY */
        int[] coords;
        boolean pureLayer;

		VectorManhattan(int[] coords, Layer layer, EGraphics graphics, boolean pureLayer)
		{
			super(layer, graphics);
			this.coords = coords;
            this.pureLayer = pureLayer;
		}
        
		VectorManhattan(double c1X, double c1Y, double c2X, double c2Y, Layer layer, EGraphics graphics, boolean pureLayer)
		{
			this(new int[] { databaseToGrid(c1X), databaseToGrid(c1Y), databaseToGrid(c2X), databaseToGrid(c2Y) },
                    layer, graphics, pureLayer);
        }
        
        @Override boolean isFilled() { return true; }
    }

    /**
     * Class which collects boxes for VectorManhattan.
     */
    static class VectorManhattanBuilder {
        /** Number of boxes. */         int size; // number of boxes
        /** Coordiantes of boxes. */    int[] coords = new int[4];
        
        private void add(double lX, double lY, double hX, double hY) {
            if (size*4 >= coords.length) {
                int[] newCoords = new int[coords.length*2];
                System.arraycopy(coords, 0, newCoords, 0, coords.length);
                coords = newCoords;
            }
            int i = size*4;
            coords[i] = databaseToGrid(lX);
            coords[i + 1] = databaseToGrid(lY);
            coords[i + 2] = databaseToGrid(hX);
            coords[i + 3] = databaseToGrid(hY);
            size++;
        }
        
        int[] toArray() {
            int[] a = new int[size*4];
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
	static class VectorPolygon extends VectorBase
	{
        Point[] points;

		VectorPolygon(Point2D [] points, Layer layer, EGraphics graphics)
		{
			super(layer, graphics);
            this.points = new Point[points.length];
            for (int i = 0; i < points.length; i++) {
                Point2D p = points[i];
                this.points[i] = new Point(databaseToGrid(p.getX()), databaseToGrid(p.getY()));
            }
        }
        
        @Override boolean isFilled() { return true; }
	}

	/**
	 * Class which defines a cached line.
	 */
	static class VectorLine extends VectorBase
	{
		int fX, fY, tX, tY;
		int texture;

		VectorLine(double fX, double fY, double tX, double tY, int texture, Layer layer, EGraphics graphics)
		{
			super(layer, graphics);
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
	static class VectorCircle extends VectorBase
	{
		int cX, cY, eX, eY;
		int nature;

		VectorCircle(double cX, double cY, double eX, double eY, int nature, Layer layer, EGraphics graphics)
		{
			super(layer, graphics);
			this.cX = databaseToGrid(cX);
			this.cY = databaseToGrid(cY);
			this.eX = databaseToGrid(eX);
			this.eY = databaseToGrid(eY);
			this.nature = nature;
		}
        
        @Override boolean isFilled() {
            // true for disc nature
            return nature == 2;
        }
	}

	/**
	 * Class which defines a cached arc of a circle (normal or thick).
	 */
	static class VectorCircleArc extends VectorBase
	{
		int cX, cY, eX1, eY1, eX2, eY2;
		boolean thick;

		VectorCircleArc(double cX, double cY, double eX1, double eY1, double eX2, double eY2, boolean thick,
			Layer layer, EGraphics graphics)
		{
			super(layer, graphics);
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
	static class VectorText extends VectorBase
	{
		/** text is on a Cell */			static final int TEXTTYPECELL       = 1;
		/** text is on an Export */			static final int TEXTTYPEEXPORT     = 2;
		/** text is on a Node */			static final int TEXTTYPENODE       = 3;
		/** text is on an Arc */			static final int TEXTTYPEARC        = 4;
		/** text is on an Annotations */	static final int TEXTTYPEANNOTATION = 5;
		/** text is on an Instances */		static final int TEXTTYPEINSTANCE   = 6;
		/** text is on an Ports */			static final int TEXTTYPEPORT       = 7;

		/** the text location */						Rectangle bounds;
		/** the text style */							Poly.Type style;
		/** the descriptor of the text */				TextDescriptor descript;
		/** the text to draw */							String str;
		/** the text height (in display units) */		float height;
		/** the type of text (CELL, EXPORT, etc.) */	int textType;
		/** valid for port text or export text */		Export e;

		VectorText(Rectangle2D bounds, Poly.Type style, TextDescriptor descript, String str, int textType, Export e,
			Layer layer, EGraphics graphics)
		{
			super(layer, graphics);
			this.bounds = new Rectangle(databaseToGrid(bounds.getX()), databaseToGrid(bounds.getY()),
                    databaseToGrid(bounds.getWidth()), databaseToGrid(bounds.getHeight()));
			this.style = style;
			this.descript = descript;
			this.str = str;
			this.textType = textType;
			this.e = e;

			height = 1;
			if (descript != null)
			{
				TextDescriptor.Size tds = descript.getSize();
				if (!tds.isAbsolute()) height = (float)tds.getSize();
			}
		}
	}

	/**
	 * Class which defines a cached cross (a dot, large or small).
	 */
	static class VectorCross extends VectorBase
	{
        int x, y;
		boolean small;

		VectorCross(double x, double y, boolean small, Layer layer, EGraphics graphics)
		{
			super(layer, graphics);
			this.x = databaseToGrid(x);
			this.y = databaseToGrid(y);
			this.small = small;
		}
	}

	/**
	 * Class which defines a cached subcell reference.
	 */
	static class VectorSubCell
	{
		NodeInst ni;
		Orientation pureRotate;
		Cell subCell;
		int offsetX, offsetY;
        BitSet shownPorts = new BitSet();

		VectorSubCell(NodeInst ni, Point2D offset)
		{
			this.ni = ni;
            pureRotate = ni.getOrient();
            subCell = (Cell)ni.getProto();
            offsetX = databaseToGrid(offset.getX());
            offsetY = databaseToGrid(offset.getY());
		}
	}

	/**
	 * Class which holds the cell caches for a given cell.
	 * Since each cell is cached many times, once for every orientation on the screen,
	 * this object can hold many cell caches.
	 */
	class VectorCellGroup
	{
        CellId cellId;
        ERectangle bounds;
		float cellArea;
        float cellMinSize;
        CellBackup cellBackup;
		boolean isParameterized;
		Cell cell;
		HashMap<Orientation,VectorCell> orientations = new HashMap<Orientation,VectorCell>();
		List<VectorCellExport> exports;

		VectorCellGroup(CellId cellId)
		{
			this.cellId = cellId;
            init();
            updateExports();
		}

        private void init() {
            updateBounds(database.backup());
            CellBackup cellBackup = database.backup().getCell(cellId);
            if (this.cellBackup == cellBackup) return;
            this.cellBackup = cellBackup;
            clear();
            cell = database.getCell(cellId);
            isParameterized = isCellParameterized(cellBackup);
       }
        
        private void updateBounds(Snapshot snapshot) {
            ERectangle newBounds = snapshot.getCellBounds(cellId);
            if (newBounds == bounds) return;
            bounds = newBounds;
            if (bounds != null) {
                cellArea = (float)(bounds.getWidth()*bounds.getHeight());
                cellMinSize = (float)Math.min(bounds.getWidth(), bounds.getHeight());
            } else {
                cellArea = 0;
                cellMinSize = 0;
            }
            for (VectorCell vc: orientations.values())
                vc.updateBounds();
        }
        
        private boolean changedExports() {
            Cell cell = database.getCell(cellId);
            if (cell == null)
                return exports != null;
            if (exports == null) return true;
            Rectangle2D cellBounds = cell.getBounds();
            Iterator<VectorCellExport> cIt = exports.iterator();
            for(Iterator<Export> it = cell.getExports(); it.hasNext(); ) {
                Export e = it.next();
                if (!cIt.hasNext()) return true;
                VectorCellExport vce = cIt.next();
                if (!vce.exportName.equals(e.getName())) return true;
                Poly poly = e.getOriginalPort().getPoly();
                if (vce.exportCtr.getX() != poly.getCenterX() ||
                        vce.exportCtr.getY() != poly.getCenterY()) return true;
            }
            return cIt.hasNext();
        }
        
        private void updateExports() {
            Cell cell = database.getCell(cellId);
            if (cell == null) {
                exports = null;
                return;
            }
            // save export centers to detect hierarchical changes later
            exports = new ArrayList<VectorCellExport>();
            for(Iterator<Export> it = cell.getExports(); it.hasNext(); ) {
                Export e = it.next();
                VectorCellExport vce = new VectorCellExport();
                vce.exportName = e.getName();
                Poly poly = e.getOriginalPort().getPoly();
                vce.exportCtr = new Point2D.Double(poly.getCenterX(), poly.getCenterY());
                exports.add(vce);
            }
            for (VectorCell vc: orientations.values())
                vc.clearPortShapes();
        }
        
		void clear()
		{
            for (VectorCell vc: orientations.values())
                vc.clear();
            if (exports != null)
			    exports.clear();
		}

		VectorCell getAnyCell()
		{
            for (VectorCell vc: orientations.values()) {
                if (vc.valid) return vc;
            }
			return null;
		}
	}

    VectorCellGroup findCellGroup(CellId cellId) {
        int cellIndex = cellId.cellIndex;
        while (cellIndex >= cachedCells.size()) cachedCells.add(null);
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
	static class VectorCellExport
	{
		String exportName;
		Point2D exportCtr;
	}

	/**
	 * Class which defines a cached cell in a single orientation.
	 */
	class VectorCell
	{
        final VectorCellGroup vcg;
        final Orientation orient;
		int[] outlinePoints = new int[8];
        int lX, lY, hX, hY;
		ArrayList<VectorText> portShapes;
        boolean valid;
		ArrayList<VectorBase> filledShapes = new ArrayList<VectorBase>();
        ArrayList<VectorBase> shapes = new ArrayList<VectorBase>();
        ArrayList<VectorBase> topOnlyShapes = new ArrayList<VectorBase>();
		ArrayList<VectorSubCell> subCells = new ArrayList<VectorSubCell>();
		boolean hasFadeColor;
		int fadeColor;
		float maxFeatureSize;
		boolean fadeImage;
		int fadeOffsetX, fadeOffsetY;
		int [] fadeImageColors;
		int fadeImageWid, fadeImageHei;

		VectorCell(VectorCellGroup vcg, Orientation orient)
		{
            this.vcg = vcg;
            this.orient = orient;
            updateBounds();
		}
        
        private void init(Cell cell) {
            vcg.init();
            updateBounds();
            clear();
            maxFeatureSize = 0;
            AffineTransform trans = orient.pureRotate();
            
            vcg.updateExports();
            
//System.out.println("CACHING CELL "+cell +" WITH ORIENTATION "+orientationName);
            for (VectorManhattanBuilder b: boxBuilders)
                b.clear();
            for (VectorManhattanBuilder b: pureBoxBuilders)
                b.clear();
            // draw all arcs
            for(Iterator<ArcInst> arcs = cell.getArcs(); arcs.hasNext(); ) {
                ArcInst ai = arcs.next();
                drawArc(ai, trans, this);
            }
            
            // draw all nodes
            for(Iterator<NodeInst> nodes = cell.getNodes(); nodes.hasNext(); ) {
                NodeInst ni = nodes.next();
                drawNode(ni, trans, this);
            }
            
            // show cell variables
            int numPolys = cell.numDisplayableVariables(true);
            Poly [] polys = new Poly[numPolys];
            cell.addDisplayableVariables(CENTERRECT, polys, 0, dummyWnd, true);
            drawPolys(polys, DBMath.MATID, this, true, VectorText.TEXTTYPECELL, false);
            
            // add in anything "snuck" onto the cell
            List<VectorBase> addThesePolys = addPolyToCell.get(cell);
            if (addThesePolys != null) {
                for(VectorBase vb : addThesePolys)
                    filledShapes.add(vb);
            }
            List<VectorLine> addTheseInsts = addInstToCell.get(cell);
            if (addTheseInsts != null) {
                for(VectorLine vl : addTheseInsts)
                    shapes.add(vl);
            }
            addBoxesFromBuilder(this, cell.getTechnology(), boxBuilders, false);
            addBoxesFromBuilder(this, cell.getTechnology(), pureBoxBuilders, true);
            Collections.sort(filledShapes, shapeByLayer);
            Collections.sort(shapes, shapeByLayer);
            Collections.sort(topOnlyShapes, shapeByLayer);
            
            // icon cells should not get greeked because of their contents
            if (cell.getView() == View.ICON) maxFeatureSize = 0;
            
            valid = true;
        }
        
        private void updateBounds() {
            lX = lY = Integer.MAX_VALUE;
            hX = hY = Integer.MIN_VALUE;
            ERectangle bounds = vcg.bounds;
            if (bounds == null) return;
            double[] points = new double[8];
            points[0] = points[6] = bounds.getMinX();
            points[1] = points[3] = bounds.getMinY();
            points[2] = points[4] = bounds.getMaxX();
            points[5] = points[7] = bounds.getMaxY();
            orient.pureRotate().transform(points, 0, points, 0, 4);
            for (int i = 0; i < 4; i++) {
                int x = databaseToGrid(points[i*2]);
                int y = databaseToGrid(points[i*2 + 1]);
                lX = Math.min(lX, x);
                lY = Math.min(lY, y);
                hX = Math.max(hX, x);
                hY = Math.max(hY, y);
                outlinePoints[i*2] = x;
                outlinePoints[i*2+1] = y;
            }
        }
        
        private void clearPortShapes() {
            portShapes = null;
        }
        
        ArrayList<VectorText> getPortShapes() {
            if (portShapes == null)
                initPortShapes();
            return portShapes;
        }
        
        private void initPortShapes() {
            portShapes = new ArrayList<VectorText>();
            Cell cell = database.getCell(vcg.cellId);
            AffineTransform trans = orient.pureRotate();
            for(int i = 0, numPorts = cell.getNumPorts(); i < numPorts; i++) {
                Export pp = cell.getPort(i);
                Poly portPoly = pp.getOriginalPort().getPoly();
                portPoly.transform(trans);
                
                TextDescriptor descript = portPoly.getTextDescriptor();
                TextDescriptor portDescript = pp.getTextDescriptor(Export.EXPORT_NAME);
                Poly.Type style = Poly.Type.FILLED;
                if (descript != null) {
                    portDescript = portDescript.withColorIndex(descript.getColorIndex());
                    style = descript.getPos().getPolyType();
                }
                VectorText vt = new VectorText(portPoly.getBounds2D(), style, /*portDescript*/descript, null, VectorText.TEXTTYPEPORT, pp,
                        null, null);
                portShapes.add(vt);
            }
        }
        
        private void clear() {
            valid = hasFadeColor = fadeImage = false;
            filledShapes.clear();
            shapes.clear();
            topOnlyShapes.clear();
            subCells.clear();
            fadeImageColors = null;
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
    
	VectorCell drawCell(Cell cell, Orientation prevTrans, VarContext context, double scale)
	{
        VectorCell vc = findVectorCell(cell.getId(), prevTrans);
		if (vc.vcg.isParameterized || !vc.valid) {
            varContext = vc.vcg.isParameterized ? context : null;
            curScale = scale; // Fix it later. Multiple Strings positioning shouldn't use scale.
            vc.init(cell);
        }
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
	public void addBoxToCell(double lX, double lY, double hX, double hY, Layer layer, CellId cellId)
	{
		List<VectorBase> addToThisCell = addPolyToCell.get(cellId);
		if (addToThisCell == null)
		{
			addToThisCell = new ArrayList<VectorBase>();
			addPolyToCell.put(cellId, addToThisCell);
		}
		EGraphics graphics = null;
		if (layer != null)
			graphics = layer.getGraphics();
		VectorManhattan vm = new VectorManhattan(lX, lY, hX, hY, layer, graphics, false);
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
	public void addInstanceToCell(double lX, double lY, double hX, double hY, CellId cellId)
	{
		List<VectorLine> addToThisCell = addInstToCell.get(cellId);
		if (addToThisCell == null)
		{
			addToThisCell = new ArrayList<VectorLine>();
			addInstToCell.put(cellId, addToThisCell);
		}

		// store the subcell
        addToThisCell.add(new VectorLine(lX, lY, hX, lY, 0, null, instanceGraphics));
        addToThisCell.add(new VectorLine(hX, lY, hX, hY, 0, null, instanceGraphics));
        addToThisCell.add(new VectorLine(hX, hY, lX, hY, 0, null, instanceGraphics));
        addToThisCell.add(new VectorLine(lX, hY, lX, lY, 0, null, instanceGraphics));
	}
	
	private static final Variable.Key NCCKEY = Variable.newKey("ATTR_NCC");
    private static final PrimitivePort busPinPort = Schematics.tech.busPinNode.getPort(0);

	/**
	 * Method to tell whether a Cell is parameterized.
	 * Code is taken from tool.drc.Quick.checkEnumerateProtos
	 * Could also use the code in tool.io.output.Spice.checkIfParameterized
	 * @param cell the Cell to examine
	 * @return true if the cell has parameters
	 */
    private static boolean isCellParameterized(CellBackup cellBackup) {
        for(Iterator<Variable> vIt = cellBackup.d.getVariables(); vIt.hasNext(); ) {
            Variable var = vIt.next();
            if (var.getTextDescriptor().isParam()) {
                // this attribute is not a parameter
                if (var.getKey() == NCCKEY) continue;
                return true;
            }
        }
        
        // look for any Java coded stuff (Logical Effort calls)
        for (ImmutableNodeInst n: cellBackup.nodes) {
            for(Iterator<Variable> vIt = n.getVariables(); vIt.hasNext(); ) {
                Variable var = vIt.next();
                if (var.getCode() != TextDescriptor.Code.NONE) return true;
            }
        }
        for (ImmutableArcInst a: cellBackup.arcs) {
            for(Iterator<Variable> vIt = a.getVariables(); vIt.hasNext(); ) {
                Variable var = vIt.next();
                if (var.getCode() != TextDescriptor.Code.NONE) return true;
            }
        }
        
        // bus pin appearance depends on parent Cell
        for (ImmutableExport e: cellBackup.exports) {
            if (e.originalPortId == busPinPort)
                return true;
        }
        return false;
    }
    
    void forceRedraw(Set<CellId> changedCells) {
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
            if (vcg == null) continue;
            if (clearCache)
                vcg.clear();
            if (clearFadeImages) {
                for(VectorCell vc : vcg.orientations.values()) {
                    vc.fadeImageColors = null;
                    vc.fadeImage = false;
                }
            }
            vcg.updateBounds(snapshot);
            if (!changedCells.contains(vcg.cellId) && vcg.cellBackup == snapshot.getCell(cellIndex)) continue;
            cellChanged(vcg.cellId);
        }
    }
    
	/**
	 * Method called when a cell changes: removes any cached displays of that cell
	 * @param cell the cell that changed
	 */
	private void cellChanged(CellId cellId)
	{
		VectorCellGroup vcg = null;
        if (cellId.cellIndex < cachedCells.size())
            vcg = cachedCells.get(cellId.cellIndex);
        Cell cell = database.getCell(cellId);
		if (cell != null)
		{
			// cell still valid: see if it changed from last cache
			if (vcg != null)
			{
				// queue parent cells for recaching if the bounds or exports changed
				if (vcg.changedExports())
				{
                    vcg.updateExports();
					for(Iterator<CellUsage> it = cell.getUsagesOf(); it.hasNext(); )
					{
	                    CellUsage u = it.next();
						cellChanged(u.parentId);
					}
				}
			}
		}
//System.out.println("REMOVING CACHE FOR CELL "+cell);
        if (vcg != null)
            vcg.clear();
	}

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
    
    private static int databaseToGrid(double x) {
        double xg = x*DBMath.GRID;
		return (int)(xg >= 0 ? xg + 0.5 : xg - 0.5);
    }

    private void addBoxesFromBuilder(VectorCell vc, Technology tech, ArrayList<VectorManhattanBuilder> boxBuilders, boolean pureArray) {
        for (int layerIndex = 0; layerIndex < boxBuilders.size(); layerIndex++) {
            VectorManhattanBuilder b = boxBuilders.get(layerIndex);
            if (b.size == 0) continue;
            Layer layer = tech.getLayer(layerIndex);
            VectorManhattan vm = new VectorManhattan(b.toArray(), layer, layer.getGraphics(), pureArray);
            vc.filledShapes.add(vm);
        }
    }

	/**
	 * Comparator class for sorting VectorBase objects by their layer depth.
	 */
    public static Comparator<VectorBase> shapeByLayer = new Comparator<VectorBase>()
    {
		/**
		 * Method to sort Objects by their string name.
		 */
    	public int compare(VectorBase vb1, VectorBase vb2)
        {
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
            if (isContact1 != isContact2)
                return isContact1 ? -1 : 1;
            return level1 - level2;
        }
    };

	/**
	 * Method to cache a NodeInst.
	 * @param ni the NodeInst to cache.
     * @param trans the transformation of the NodeInst to the parent Cell.
	 * @param vc the cached cell in which to place the NodeInst.
     */
	private void drawNode(NodeInst ni, AffineTransform trans, VectorCell vc)
	{
		NodeProto np = ni.getProto();
		AffineTransform localTrans = ni.rotateOut(trans);

		// draw the node
		if (ni.isCellInstance())
		{
			// cell instance
			Cell subCell = (Cell)np;

			// record a call to the instance
			Point2D ctrShift = new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY());
			localTrans.transform(ctrShift, ctrShift);
			VectorSubCell vsc = new VectorSubCell(ni, ctrShift);
			vc.subCells.add(vsc);

            // show the ports that are not further exported or connected
            for(Iterator<Connection> it = ni.getConnections(); it.hasNext();) {
                Connection con = it.next();
                PortInst pi = con.getPortInst();
                vsc.shownPorts.set(pi.getPortProto().getId().getChronIndex());
            }
            for(Iterator<Export> it = ni.getExports(); it.hasNext();) {
                Export exp = it.next();
                PortInst pi = exp.getOriginalPort();
                vsc.shownPorts.set(pi.getPortProto().getId().getChronIndex());
            }

			// draw any displayable variables on the instance
			int numPolys = ni.numDisplayableVariables(true);
			Poly [] polys = new Poly[numPolys];
			Rectangle2D rect = ni.getUntransformedBounds();
			ni.addDisplayableVariables(rect, polys, 0, dummyWnd, true);
			drawPolys(polys, localTrans, vc, false, VectorText.TEXTTYPENODE, false);
		} else
		{
			// primitive: save it
			PrimitiveNode prim = (PrimitiveNode)np;
			int textType = VectorText.TEXTTYPENODE;
			if (prim == Generic.tech.invisiblePinNode) textType = VectorText.TEXTTYPEANNOTATION;
			Technology tech = prim.getTechnology();
			Poly [] polys = tech.getShapeOfNode(ni, dummyWnd, varContext, false, false, null);
            boolean hideOnLowLevel = ni.isVisInside() || np == Generic.tech.cellCenterNode;
			boolean pureLayer = (ni.getFunction() == PrimitiveNode.Function.NODE);
			drawPolys(polys, localTrans, vc, hideOnLowLevel, textType, pureLayer);
		}

		// draw any exports from the node
		Iterator<Export> it = ni.getExports();
		while (it.hasNext())
		{
			Export e = it.next();
			Poly poly = e.getNamePoly();
			Rectangle2D rect = (Rectangle2D)poly.getBounds2D().clone();
			TextDescriptor descript = poly.getTextDescriptor();
			Poly.Type style = descript.getPos().getPolyType();
			style = Poly.rotateType(style, ni);
			VectorText vt = new VectorText(poly.getBounds2D(), style, descript, null, VectorText.TEXTTYPEEXPORT, e,
				null, null);
			vc.topOnlyShapes.add(vt);

			// draw variables on the export
			int numPolys = e.numDisplayableVariables(true);
			if (numPolys > 0)
			{
				Poly [] polys = new Poly[numPolys];
				e.addDisplayableVariables(rect, polys, 0, dummyWnd, true);
				drawPolys(polys, trans, vc, true, VectorText.TEXTTYPEEXPORT, false);
//				drawPolys(polys, localTrans, vc, true, VectorText.TEXTTYPEEXPORT, false);
			}
		}
	}

	/**
	 * Method to cache an ArcInst.
	 * @param ai the ArcInst to cache.
     * @param trans the transformation of the ArcInst to the parent cell.
	 * @param vc the cached cell in which to place the ArcInst.
     */
	private void drawArc(ArcInst ai, AffineTransform trans, VectorCell vc)
	{
		// if the arc is tiny, just approximate it with a single dot
		Rectangle2D arcBounds = ai.getBounds();

        // see if the arc is completely clipped from the screen
        Rectangle2D dbBounds = new Rectangle2D.Double(arcBounds.getX(), arcBounds.getY(), arcBounds.getWidth(), arcBounds.getHeight());
        Poly p = new Poly(dbBounds);
        p.transform(trans);
        dbBounds = p.getBounds2D();

		// draw the arc
		ArcProto ap = ai.getProto();
		Technology tech = ap.getTechnology();
		Poly [] polys = tech.getShapeOfArc(ai, dummyWnd);
		drawPolys(polys, trans, vc, false, VectorText.TEXTTYPEARC, false);
	}

	/**
	 * Method to cache an array of polygons.
	 * @param polys the array of polygons to cache.
	 * @param trans the transformation to apply to each polygon.
	 * @param vc the cached cell in which to place the polygons.
	 * @param hideOnLowLevel true if the polygons should be marked such that they are not visible on lower levels of hierarchy.
	 * @param pureLayer true if these polygons come from a pure layer node.
	 */
	private void drawPolys(Poly[] polys, AffineTransform trans, VectorCell vc, boolean hideOnLowLevel, int textType, boolean pureLayer)
	{
		if (polys == null) return;
		for(int i = 0; i < polys.length; i++)
		{
			// get the polygon and transform it
			Poly poly = polys[i];
			if (poly == null) continue;

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
	private void renderPoly(Poly poly, VectorCell vc, boolean hideOnLowLevel, int textType, boolean pureLayer)
	{
		// now draw it
		Point2D [] points = poly.getPoints();
		Layer layer = poly.getLayer();
		EGraphics graphics = null;
		if (layer != null)
			graphics = layer.getGraphics();
		Poly.Type style = poly.getStyle();
        ArrayList<VectorBase> filledShapes = hideOnLowLevel ? vc.topOnlyShapes : vc.filledShapes;
        ArrayList<VectorBase> shapes = hideOnLowLevel ? vc.topOnlyShapes : vc.shapes;
		if (style == Poly.Type.FILLED)
		{
			Rectangle2D bounds = poly.getBox();
			if (bounds != null)
			{
				// convert coordinates
				double lX = bounds.getMinX();
				double hX = bounds.getMaxX();
				double lY = bounds.getMinY();
				double hY = bounds.getMaxY();
                float minSize = (float)Math.min(hX - lX, hY - lY);
                int layerIndex = -1;
				if (layer != null)
				{
                    if (layer.getTechnology() == vc.vcg.cellBackup.d.tech)
                        layerIndex = layer.getIndex();
					Layer.Function fun = layer.getFunction();
					if (!pureLayer && (fun.isImplant() || fun.isSubstrate()))
					{
						// well and substrate layers are made smaller so that they "greek" sooner
                        minSize /= 10;
					}
				}
                if (layerIndex >= 0) {
                    putBox(layerIndex, pureLayer ? pureBoxBuilders : boxBuilders, lX, lY, hX, hY);
                } else {
                    VectorManhattan vm = new VectorManhattan(lX, lY, hX, hY, layer, graphics, pureLayer);
                    filledShapes.add(vm);
                }
				vc.maxFeatureSize = Math.max(vc.maxFeatureSize, minSize);
				return;
			}
			VectorPolygon vp = new VectorPolygon(points, layer, graphics);
			filledShapes.add(vp);
			return;
		}
		if (style == Poly.Type.CROSSED)
		{
			VectorLine vl1 = new VectorLine(points[0].getX(), points[0].getY(),
				points[1].getX(), points[1].getY(), 0, layer, graphics);
			VectorLine vl2 = new VectorLine(points[1].getX(), points[1].getY(),
				points[2].getX(), points[2].getY(), 0, layer, graphics);
			VectorLine vl3 = new VectorLine(points[2].getX(), points[2].getY(),
				points[3].getX(), points[3].getY(), 0, layer, graphics);
			VectorLine vl4 = new VectorLine(points[3].getX(), points[3].getY(),
				points[0].getX(), points[0].getY(), 0, layer, graphics);
			VectorLine vl5 = new VectorLine(points[0].getX(), points[0].getY(),
				points[2].getX(), points[2].getY(), 0, layer, graphics);
			VectorLine vl6 = new VectorLine(points[1].getX(), points[1].getY(),
				points[3].getX(), points[3].getY(), 0, layer, graphics);
			shapes.add(vl1);
			shapes.add(vl2);
			shapes.add(vl3);
			shapes.add(vl4);
			shapes.add(vl5);
			shapes.add(vl6);
			return;
		}
		if (style.isText())
		{
			Rectangle2D bounds = poly.getBounds2D();
			TextDescriptor descript = poly.getTextDescriptor();
			String str = poly.getString();
			VectorText vt = new VectorText(bounds, style, descript, str, textType, null,
				layer, graphics);
			shapes.add(vt);
			vc.maxFeatureSize = Math.max(vc.maxFeatureSize, vt.height);
			return;
		}
		if (style == Poly.Type.CLOSED || style == Poly.Type.OPENED || style == Poly.Type.OPENEDT1 ||
			style == Poly.Type.OPENEDT2 || style == Poly.Type.OPENEDT3)
		{
			int lineType = 0;
			if (style == Poly.Type.OPENEDT1) lineType = 1; else
			if (style == Poly.Type.OPENEDT2) lineType = 2; else
			if (style == Poly.Type.OPENEDT3) lineType = 3;

			for(int j=1; j<points.length; j++)
			{
				Point2D oldPt = points[j-1];
				Point2D newPt = points[j];
				VectorLine vl = new VectorLine(oldPt.getX(), oldPt.getY(),
					newPt.getX(), newPt.getY(), lineType, layer, graphics);
				shapes.add(vl);
			}
			if (style == Poly.Type.CLOSED)
			{
				Point2D oldPt = points[points.length-1];
				Point2D newPt = points[0];
				VectorLine vl = new VectorLine(oldPt.getX(), oldPt.getY(),
					newPt.getX(), newPt.getY(), lineType, layer, graphics);
				shapes.add(vl);
			}
			return;
		}
		if (style == Poly.Type.VECTORS)
		{
			for(int j=0; j<points.length; j+=2)
			{
				Point2D oldPt = points[j];
				Point2D newPt = points[j+1];
				VectorLine vl = new VectorLine(oldPt.getX(), oldPt.getY(),
					newPt.getX(), newPt.getY(), 0, layer, graphics);
				shapes.add(vl);
			}
			return;
		}
		if (style == Poly.Type.CIRCLE)
		{
			VectorCircle vci = new VectorCircle(points[0].getX(), points[0].getY(),
				points[1].getX(), points[1].getY(), 0, layer, graphics);
			shapes.add(vci);
			return;
		}
		if (style == Poly.Type.THICKCIRCLE)
		{
			VectorCircle vci = new VectorCircle(points[0].getX(), points[0].getY(),
				points[1].getX(), points[1].getY(), 1, layer, graphics);
			shapes.add(vci);
			return;
		}
		if (style == Poly.Type.DISC)
		{
			VectorCircle vci = new VectorCircle(points[0].getX(), points[0].getY(), points[1].getX(),
				points[1].getY(), 2, layer, graphics);
			filledShapes.add(vci);
			return;
		}
		if (style == Poly.Type.CIRCLEARC || style == Poly.Type.THICKCIRCLEARC)
		{
			VectorCircleArc vca = new VectorCircleArc(points[0].getX(), points[0].getY(),
				points[1].getX(), points[1].getY(), points[2].getX(), points[2].getY(),
				style == Poly.Type.THICKCIRCLEARC, layer, graphics);
			shapes.add(vca);
			return;
		}
		if (style == Poly.Type.CROSS)
		{
			// draw the cross
			VectorCross vcr = new VectorCross(points[0].getX(), points[0].getY(), true, layer, graphics);
			shapes.add(vcr);
			return;
		}
		if (style == Poly.Type.BIGCROSS)
		{
			// draw the big cross
			VectorCross vcr = new VectorCross(points[0].getX(), points[0].getY(), false, layer, graphics);
			shapes.add(vcr);
			return;
		}
	}

    private static void putBox(int layerIndex, ArrayList<VectorManhattanBuilder> boxBuilders, double lX, double lY, double hX, double hY) {
        while (layerIndex >= boxBuilders.size()) boxBuilders.add(new VectorManhattanBuilder());
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
