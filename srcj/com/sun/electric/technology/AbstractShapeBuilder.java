/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AbstractShapeBuilder.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
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
 * GNU General Public License for more details.q
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.technology;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellRevision;
import com.sun.electric.database.CellTree;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.id.NodeProtoId;
import com.sun.electric.database.id.PrimitiveNodeId;
import com.sun.electric.database.id.PrimitivePortId;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Iterator;

/**
 * A support class to build shapes of arcs and nodes.
 */
public abstract class AbstractShapeBuilder {
    private Layer.Function.Set onlyTheseLayers;
    private boolean reasonable;
    private boolean wipePins;
    private boolean electrical;
    private final boolean rotateNodes;
    private Orientation orient;
    private AffineTransform pureRotate;

    protected double[] doubleCoords = new double[8];
    protected int pointCount;
    protected int[] intCoords = new int[4];
    private CellBackup.Memoization m;
    private Shrinkage shrinkage;
    private TechPool techPool;
    private ImmutableNodeInst curNode;

    /** Creates a new instance of AbstractShapeBuilder */
    public AbstractShapeBuilder() {
        this(true);
    }

    public AbstractShapeBuilder(boolean rotateNodes) {
        this.rotateNodes = rotateNodes;
    }

    public void setup(Cell cell) {
        setup(cell.backupUnsafe(), null, false, true, false, null);
    }

    public void setup(CellTree cellTree, Orientation orient, boolean electrical, boolean wipePins, boolean reasonable, Layer.Function.Set onlyTheseLayers) {
        setup(cellTree.top, orient, electrical, wipePins, reasonable, onlyTheseLayers);
        techPool = cellTree.techPool;
    }

    public void setup(CellBackup cellBackup, Orientation orient, boolean electrical, boolean wipePins, boolean reasonable, Layer.Function.Set onlyTheseLayers) {
        this.m = cellBackup.getMemoization();
        this.shrinkage = cellBackup.getShrinkage();
        this.techPool = cellBackup.techPool;
        if (orient == null || orient.canonic() == Orientation.IDENT) {
            this.orient = null;
            pureRotate = null;
        } else {
            this.orient = orient.canonic();
            pureRotate = this.orient.pureRotate();
        }
        this.electrical = electrical;
        this.wipePins = wipePins;
        this.reasonable = reasonable;
        this.onlyTheseLayers = onlyTheseLayers;
        pointCount = 0;
        curNode = null;
    }

    public boolean isElectrical() {
        return electrical;
    }
    public boolean isReasonable() {
        return reasonable;
    }
    public boolean skipLayer(Layer layer) {
        return onlyTheseLayers != null && !onlyTheseLayers.contains(layer.getFunction(), layer.getFunctionExtras());
    }

    public CellBackup.Memoization getMemoization() {
        return m;
    }
    public CellBackup getCellBackup() {
        return m.getCellBackup();
    }
    public Shrinkage getShrinkage() {
        return shrinkage;
    }
    public TechPool getTechPool() {
        return techPool;
    }

    public void genShapeOfArc(ImmutableArcInst a) {
        if (genShapeEasy(a))
            return;
        pointCount = 0;
        curNode = null;
        techPool.getTech(a.protoId.techId).getShapeOfArc(this, a);
    }

    public void genShapeOfNode(ImmutableNodeInst n) {
        PrimitiveNode pn = techPool.getPrimitiveNode((PrimitiveNodeId)n.protoId);
        // if node is erased, remove layers
        if (Technology.ALWAYS_SKIP_WIPED_PINS || wipePins) {
            if (m.isWiped(n)) return;
            if (pn.isWipeOn1or2() && m.pinUseCount(n)) return;
        }

        Technology.NodeLayer[] primLayers = pn.getNodeLayers();
        if (electrical) {
            Technology.NodeLayer[] eLayers = pn.getElectricalLayers();
            if (eLayers != null) primLayers = eLayers;
        }
        pointCount = 0;
        curNode = n;
        pn.getTechnology().genShapeOfNode(this, n, pn, primLayers);
    }

	/**
	 * Returns the polygons that describe node "n", given a set of
	 * NodeLayer objects to use.
	 * This method is called by the specific Technology overrides of getShapeOfNode().
	 * @param n the ImmutableNodeInst that is being described.
     * @param np PrimitiveNode proto of give ImmutableNodeInst in TechPool of Memoization
	 * @param primLayers an array of NodeLayer objects to convert to Poly objects.
	 * @param graphicsOverride the graphics override to use for all generated polygons (if not null).
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 */
	public void genShapeOfNode(ImmutableNodeInst n, PrimitiveNode np, Technology.NodeLayer [] primLayers, EGraphics graphicsOverride)
	{
        pointCount = 0;

        // Pure-layer nodes and serpentine trans may have outline trace
        if (np.isHoldsOutline()) {
            int specialType = np.getSpecialType();
            if (specialType == PrimitiveNode.SERPTRANS) {
                SerpentineTrans std = new SerpentineTrans(n, np, primLayers);
                if (std.layersTotal > 0) {
                    std.initTransPolyFilling();
                    for (int i = 0; i < std.layersTotal; i++)
                        std.fillTransPoly();
                    return;
                }
            } else {
                EPoint[] outline = n.getTrace();
                if (outline != null) {
                    assert primLayers.length == 1;
                    Technology.NodeLayer primLayer = primLayers[0];
                    Layer layer = primLayer.getLayer();
                    if (skipLayer(layer)) return;
                    Poly.Type style = primLayer.getStyle();
                    PrimitivePort pp = primLayer.getPort(np);
                    int startPoint = 0;
                    for (int i = 1; i < outline.length; i++) {
                        boolean breakPoint = (i == outline.length - 1) || (outline[i] == null);
                        if (breakPoint) {
                            if (i == outline.length - 1) i++;
                            for (int j = startPoint; j < i; j++)
                                pushPoint(outline[j]);
                            pushPoly(style, layer, graphicsOverride, pp);
                            startPoint = i + 1;
                        }
                    }
                    return;
                }
            }
        }

		// construct the polygon array
        double xSize = n.size.getGridX();
        double ySize = n.size.getGridY();

		// add in the basic polygons
		for(int i = 0; i < primLayers.length; i++) {
			Technology.NodeLayer primLayer = primLayers[i];
            Layer layer = primLayer.getLayerOrPseudoLayer();
            if (skipLayer(layer)) continue;
	        Poly.Type style = primLayer.getStyle();
	        PrimitivePort pp = primLayer.getPort(np);
            if (layer.isCarbonNanotubeLayer() &&
            	(np.getFunction() == PrimitiveNode.Function.TRANMOSCN || np.getFunction() == PrimitiveNode.Function.TRAPMOSCN)) {
                CarbonNanotube cnd = new CarbonNanotube(n, primLayer);
	            for(int j = 0; j < cnd.numTubes; j++)
	                cnd.fillCutPoly(j, style, layer, pp);
                assert graphicsOverride == null;
 	            continue;
 	        }

            int representation = primLayer.getRepresentation();
            if (representation == Technology.NodeLayer.BOX) {
                EdgeH leftEdge = primLayer.getLeftEdge();
                EdgeH rightEdge = primLayer.getRightEdge();
                EdgeV topEdge = primLayer.getTopEdge();
                EdgeV bottomEdge = primLayer.getBottomEdge();
                double portLowX = leftEdge.getMultiplier() * xSize + leftEdge.getGridAdder();
                double portHighX = rightEdge.getMultiplier() * xSize + rightEdge.getGridAdder();
                double portLowY = bottomEdge.getMultiplier() * ySize + bottomEdge.getGridAdder();
                double portHighY = topEdge.getMultiplier() * ySize + topEdge.getGridAdder();
                pushPoint(portLowX, portLowY);
                pushPoint(portHighX, portLowY);
                pushPoint(portHighX, portHighY);
                pushPoint(portLowX, portHighY);
            } else if (representation == Technology.NodeLayer.POINTS) {
                Technology.TechPoint[] points = primLayer.getPoints();
                for (int j = 0; j < points.length; j++) {
                    EdgeH xFactor = points[j].getX();
                    EdgeV yFactor = points[j].getY();
                    double x = 0, y = 0;
                    if (xFactor != null && yFactor != null) {
                        x = xFactor.getMultiplier() * xSize + xFactor.getGridAdder();
                        y = yFactor.getMultiplier() * ySize + yFactor.getGridAdder();
                    }
                    pushPoint(x, y);
                }
            } else if (representation == Technology.NodeLayer.MULTICUTBOX) {
                MultiCutData mcd = new MultiCutData(n, primLayer);
                int numExtraLayers = reasonable ? mcd.cutsReasonable : mcd.cutsTotal;
                for (int j = 0; j < numExtraLayers; j++)
                    mcd.fillCutPoly(j, style, layer, pp);
                assert graphicsOverride == null;
                continue;
            }

            if (style.isText()) {
                assert graphicsOverride == null;
                pushTextPoly(style, layer, pp, primLayer.getMessage(), primLayer.getDescriptor());
            } else {
                pushPoly(style, layer, graphicsOverride, pp);
            }
        }
	}

    public void genShapeOfPort(ImmutableNodeInst n, PrimitivePortId portId, Point2D selectPt) {
        PrimitiveNode pn = techPool.getPrimitiveNode((PrimitiveNodeId)n.protoId);
        PrimitivePort pp = pn.getPort(portId);
        pointCount = 0;
        curNode = n;
        pn.getTechnology().genShapeOfPort(this, n, pn, pp, selectPt);
    }

	/**
	 * Puts into shape builder s the polygons that describes port "pp" of node "n".
	 * This method is overridden by specific Technologys.
	 * @param n the ImmutableNodeInst that is being described.
     * @param pn proto of the ImmutableNodeInst in this Technology
     * @param pp PrimitivePort
	 */
    public void genShapeOfPort(ImmutableNodeInst n, PrimitiveNode pn, PrimitivePort pp) {
		if (pn.getSpecialType() == PrimitiveNode.SERPTRANS)
		{
			// serpentine transistors use a more complex port determination
			SerpentineTrans std = new SerpentineTrans(n, pn, pn.getNodeLayers());
			if (std.hasValidData()) {
                std.fillTransPort(pp);
                return;
            }
		}

		// standard port determination, see if there is outline information
		if (pn.isHoldsOutline())
		{
			// outline may determine the port
			EPoint [] outline = n.getTrace();
			if (outline != null)
			{
				int endPortPoly = outline.length;
                for(int i=1; i<outline.length; i++)
                {
                    if (outline[i] == null)
                    {
                        endPortPoly = i;
                        break;
                    }
                }
//				double cX = n.anchor.getLambdaX();
//				double cY = n.anchor.getLambdaY();
//				Point2D [] pointList = new Point2D.Double[endPortPoly];
				for(int i=0; i<endPortPoly; i++) {
//					pointList[i] = new Point2D.Double(cX + outline[i].getX(), cY + outline[i].getY());
                    pushPoint(/*n.anchor,*/ outline[i].getGridX(), outline[i].getGridY());
                }
//				Poly portPoly = new Poly(pointList);
                Poly.Type style;
				if (pn.getTechnology().getPrimitiveFunction(pn, n.techBits) == PrimitiveNode.Function.NODE)
				{
					style = Poly.Type.FILLED;
				} else
				{
					style = Poly.Type.OPENED;
				}
                pushPoly(style, null, null, null);
                return;
//				portPoly.setTextDescriptor(TextDescriptor.getExportTextDescriptor());
//				return portPoly;
			}
		}

		// standard port computation
        double sizeX = n.size.getGridX();
        double sizeY = n.size.getGridY();
//        double sizeX = n.size.getLambdaX();
//        double sizeY = n.size.getLambdaY();
		double portLowX = /*n.anchor.getGridX() +*/ pp.getLeft().getMultiplier() * sizeX + pp.getLeft().getGridAdder();
		double portHighX = /*n.anchor.getGridX() +*/ pp.getRight().getMultiplier() * sizeX + pp.getRight().getGridAdder();
		double portLowY = /*n.anchor.getGridY() +*/ pp.getBottom().getMultiplier() * sizeY + pp.getBottom().getGridAdder();
		double portHighY = /*n.anchor.getGridY() +*/ pp.getTop().getMultiplier() * sizeY + pp.getTop().getGridAdder();
        pushPoint(portLowX, portLowY);
        pushPoint(portHighX, portLowY);
        pushPoint(portHighX, portHighY);
        pushPoint(portLowX, portHighY);
//		double portX = (portLowX + portHighX) / 2;
//		double portY = (portLowY + portHighY) / 2;
//		Poly portPoly = new Poly(portX, portY, portHighX-portLowX, portHighY-portLowY);
        pushPoly(Poly.Type.FILLED, null, null, null);
//		portPoly.setStyle(Poly.Type.FILLED);
//		portPoly.setTextDescriptor(TextDescriptor.getExportTextDescriptor());
//		return portPoly;
    }

	/**
	 * Method to fill in an AbstractShapeBuilder a polygon that describes this ImmutableArcInst in grid units.
	 * The polygon is described by its width, and style.
     * @param a the arc information.
	 * @param gridWidth the gridWidth of the Poly.
	 * @param style the style of the Poly.
     * @param layer layer of the Poly
     * @param graphicsOverride graphics override of the Poly
	 */
    public void makeGridPoly(ImmutableArcInst a, long gridWidth, Poly.Type style, Layer layer, EGraphics graphicsOverride) {
//        long[] result;
        if (techPool.getArcProto(a.protoId).isCurvable()) {
            // get the radius information on the arc
            Double radiusDouble = a.getRadius();
            if (radiusDouble != null && curvedArcGridOutline(a, gridWidth, DBMath.lambdaToGrid(radiusDouble))) {
                pushPoly(style, layer, graphicsOverride, null);
                return;
            }
        }

        // zero-width polygons are simply lines
        if (gridWidth <= 0) {
            pushPoint(a.tailLocation);
            pushPoint(a.headLocation);
            if (style == Poly.Type.FILLED) style = Poly.Type.OPENED;
            pushPoly(style, layer, graphicsOverride, null);
            return;
        }

        // make the polygon
		int w2 = ((int)gridWidth) >>> 1;
        short shrinkT = a.isTailExtended() ? shrinkage.get(a.tailNodeId) : Shrinkage.EXTEND_0;
        short shrinkH = a.isHeadExtended() ? shrinkage.get(a.headNodeId) : Shrinkage.EXTEND_0;

        int angle = a.getAngle();
        double w2x = DBMath.roundShapeCoord(w2*GenMath.cos(angle));
        double w2y = DBMath.roundShapeCoord(w2*GenMath.sin(angle));
        double tx = 0;
        double ty = 0;
        if (shrinkT == Shrinkage.EXTEND_90) {
            tx = -w2x;
            ty = -w2y;
        } else if (shrinkT != Shrinkage.EXTEND_0) {
            Point2D e = computeExtension(w2, -w2x, -w2y, a.getOppositeAngle(), shrinkT);
            tx = e.getX();
            ty = e.getY();
        }
        double hx = 0;
        double hy = 0;
        if (shrinkH == Shrinkage.EXTEND_90) {
            hx = w2x;
            hy = w2y;
        } else if (shrinkH != Shrinkage.EXTEND_0) {
            Point2D e = computeExtension(w2, w2x, w2y, angle, shrinkH);
            hx = e.getX();
            hy = e.getY();
        }

        pushPoint(a.tailLocation, tx - w2y, ty + w2x);
        pushPoint(a.tailLocation, tx + w2y, ty - w2x);
        pushPoint(a.headLocation, hx + w2y, hy - w2x);
        pushPoint(a.headLocation, hx - w2y, hy + w2x);

        // somewhat simpler if rectangle is manhattan
        if (gridWidth != 0 && style.isOpened())
            pushPoint(a.tailLocation, tx - w2y, ty + w2x);
        pushPoly(style, layer, graphicsOverride, null);
    }

    /**
     * Computes extension vector of wire,
     */
    public static Point2D computeExtension(int w2, double ix1, double iy1, int angle, short shrink) {
        if (shrink == Shrinkage.EXTEND_90) return new Point2D.Double(ix1, iy1);
        if (shrink == Shrinkage.EXTEND_0) return new Point2D.Double(0, 0);
        assert shrink >= Shrinkage.EXTEND_ANY;
        int angle2 = (shrink - Shrinkage.EXTEND_ANY) - angle;
        if (angle2 < 0)
            angle2 += 3600;
        double x1 = ix1;
        double y1 = iy1;
        double s1;
        if (y1 == 0) {
            if (x1 > 0) {
                s1 = x1;
                x1 = 1;
            } else if (x1 < 0) {
                s1 = -x1;
                x1 = -1;
            } else {
                return new Point2D.Double(0, 0);
            }
        } else if (x1 == 0) {
            if (y1 > 0) {
                s1 = y1;
                y1 = 1;
            } else {
                s1 = -y1;
                y1 = -1;
            }
        } else {
            s1 = x1*x1 + y1*y1;
        }

        double x2 = DBMath.roundShapeCoord(w2*GenMath.cos(angle2));
        double y2 = DBMath.roundShapeCoord(w2*GenMath.sin(angle2));
        double s2;
        if (y2 == 0) {
            if (x2 > 0) {
                s2 = x2;
                x2 = 1;
            } else if (x2 < 0) {
                s2 = -x2;
                x2 = -1;
            } else {
                return new Point2D.Double(0, 0);
            }
        } else if (x2 == 0) {
            if (y2 > 0) {
                s2 = y2;
                y2 = 1;
            } else {
                s2 = -y2;
                y2 = -1;
            }
        } else {
            s2 = x2*x2 + y2*y2;
        }

        double det = x1*y2 - y1*x2;
        if (det == 0) return new Point2D.Double(0, 0);
        double x = (x2*s1 + x1*s2)/det;
        double y = (y2*s1 + y1*s2)/det;
        x = DBMath.roundShapeCoord(x);
        y = DBMath.roundShapeCoord(y);
        x = x + iy1;
        y = y - ix1;
        if (det < 0) {
            x = -x;
            y = -y;
        }
        return new Point2D.Double(x, y);
    }

	/**
	 * when arcs are curved, the number of line segments will be
	 * between this value, and half of this value.
	 */
	private static final int MAXARCPIECES = 16;

	/**
     * Method to fill polygon "poly" with the outline in grid units of the curved arc in
     * this ImmutableArcInst whose width in grid units is "gridWidth".
     * If there is no curvature information in the arc, the routine returns false,
     * otherwise it returns the curved polygon.
     * @param a the arc information.
     * @param gridWidth width in grid units.
     * @param gridRadius radius in grid units.
     * @return true if point were filled to the buuilder
     */
    public boolean curvedArcGridOutline(ImmutableArcInst a, long gridWidth, long gridRadius) {
        // get information about the curved arc
        long pureGridRadius = Math.abs(gridRadius);
        double gridLength = a.getGridLength();

        // see if the lambdaRadius can work with these arc ends
        if (pureGridRadius*2 < gridLength) return false;

        // determine the center of the circle
        Point2D [] centers = DBMath.findCenters(pureGridRadius, a.headLocation.gridMutable(), a.tailLocation.gridMutable());
        if (centers == null) return false;

        Point2D centerPt = centers[1];
        if (gridRadius < 0) {
            centerPt = centers[0];
        }
        double centerX = centerPt.getX();
        double centerY = centerPt.getY();

        // determine the base and range of angles
        int angleBase = DBMath.figureAngle(a.headLocation.getGridX() - centerX, a.headLocation.getGridY() - centerY);
        int angleRange = DBMath.figureAngle(a.tailLocation.getGridX() - centerX, a.tailLocation.getGridY() - centerY);
        angleRange -= angleBase;
        if (angleRange < 0) angleRange += 3600;

        // force the curvature to be the smaller part of a circle (used to determine this by the reverse-ends bit)
        if (angleRange > 1800) {
            angleBase += angleRange;
            if (angleBase < 0) angleBase += 3600;
            angleRange = 3600 - angleRange;
        }

        // determine the number of intervals to use for the arc
        int pieces = angleRange;
        while (pieces > MAXARCPIECES) pieces /= 2;
        if (pieces == 0) return false;

        // get the inner and outer radii of the arc
        double outerRadius = pureGridRadius + gridWidth / 2;
        double innerRadius = outerRadius - gridWidth;

        // fill the polygon
        for(int i=0; i<=pieces; i++) {
            int angle = (angleBase + i * angleRange / pieces) % 3600;
            pushPoint(DBMath.cos(angle) * innerRadius + centerX, DBMath.sin(angle) * innerRadius + centerY);
        }
        for(int i=pieces; i>=0; i--) {
            int angle = (angleBase + i * angleRange / pieces) % 3600;
            pushPoint(DBMath.cos(angle) * outerRadius + centerX, DBMath.sin(angle) * outerRadius + centerY);
        }
        return true;
    }

    /**
     * Generate shape of this ImmutableArcInst in easy case.
     * @param a the arc information.
     * @return true if shape was generated.
     */
    public boolean genShapeEasy(ImmutableArcInst a) {
        if (m.isHardArc(a.arcId)) return false;
        ArcProto protoType = techPool.getArcProto(a.protoId);
        int gridExtendOverMin = (int)a.getGridExtendOverMin();
        int minLayerExtend = gridExtendOverMin + protoType.getMinLayerGridExtend();
        if (minLayerExtend == 0) {
            assert protoType.getNumArcLayers() == 1;
            Technology.ArcLayer primLayer = protoType.getArcLayer(0);
            Layer layer = primLayer.getLayer();
            if (skipLayer(layer)) return true;
            Poly.Type style = primLayer.getStyle();
            if (style == Poly.Type.FILLED) style = Poly.Type.OPENED;
            intCoords[0] = (int)a.tailLocation.getGridX();
            intCoords[1] = (int)a.tailLocation.getGridY();
            intCoords[2] = (int)a.headLocation.getGridX();
            intCoords[3] = (int)a.headLocation.getGridY();
            pushIntLine(style, primLayer.getLayer());
            return true;
        }
        boolean tailExtended = false;
        if (a.isTailExtended()) {
            short shrinkT = shrinkage.get(a.tailNodeId);
            if (shrinkT == Shrinkage.EXTEND_90)
                tailExtended = true;
            else if (shrinkT != Shrinkage.EXTEND_0)
                return false;
        }
        boolean headExtended = false;
        if (a.isHeadExtended()) {
            short shrinkH = shrinkage.get(a.headNodeId);
            if (shrinkH == Shrinkage.EXTEND_90)
                headExtended = true;
            else if (shrinkH != Shrinkage.EXTEND_0)
                return false;
        }
        for (int i = 0, n = protoType.getNumArcLayers(); i < n; i++) {
            Technology.ArcLayer primLayer = protoType.getArcLayer(i);
            Layer layer = primLayer.getLayer();
            assert primLayer.getStyle() == Poly.Type.FILLED;
            if (skipLayer(layer)) continue;
            a.makeGridBoxInt(intCoords, tailExtended, headExtended, gridExtendOverMin + protoType.getLayerGridExtend(i));
            pushIntBox(layer);
        }
        return true;
    }

    public void pushPoint(EPoint p, double gridX, double gridY) {
        pushPointLow(p.getGridX() + DBMath.roundShapeCoord(gridX), p.getGridY() + DBMath.roundShapeCoord(gridY));
    }

    public void pushPoint(double gridX, double gridY) {
        pushPointLow(DBMath.roundShapeCoord(gridX), DBMath.roundShapeCoord(gridY));
    }

    public void pushPoint(EPoint p) {
        pushPointLow(p.getGridX(), p.getGridY());
    }

    private void pushPointLow(double gridX, double gridY) {
        if (pointCount*2 >= doubleCoords.length)
            resize();
        doubleCoords[pointCount*2] = gridX;
        doubleCoords[pointCount*2 + 1] = gridY;
        pointCount++;
    }

    private void resize() {
        double[] newDoubleCoords = new double[doubleCoords.length*2];
        System.arraycopy(doubleCoords, 0, newDoubleCoords, 0, doubleCoords.length);
        doubleCoords = newDoubleCoords;
    }

    public void pushPoly(Poly.Type style, Layer layer, EGraphics graphicsOverride, PrimitivePort pp) {
        if (!electrical)
            pp = null;
        transformDoubleCoords(style);
        addDoublePoly(pointCount, style, layer, graphicsOverride, pp);
        pointCount = 0;
    }

    public void pushTextPoly(Poly.Type style, Layer layer, PrimitivePort pp, String message, TextDescriptor descriptor) {
        if (!electrical)
            pp = null;
        transformDoubleCoords(style);
        addDoubleTextPoly(pointCount, style, layer, pp, message, descriptor);
        pointCount = 0;
    }

    private void transformDoubleCoords(Poly.Type style) {
        if (curNode != null) {
            if (rotateNodes && curNode.orient.canonic() != Orientation.IDENT) {
                // special case for Poly type CIRCLEARC and THICKCIRCLEARC: if transposing, reverse points
                if ((style == Poly.Type.CIRCLEARC || style == Poly.Type.THICKCIRCLEARC) &&
                        curNode.orient.canonic().isCTranspose()) {
                    double t;
                    t = doubleCoords[2]; doubleCoords[2] = doubleCoords[4]; doubleCoords[4] = t;
                    t = doubleCoords[3]; doubleCoords[3] = doubleCoords[5]; doubleCoords[5] = t;
                }
                curNode.orient.pureRotate().transform(doubleCoords, 0, doubleCoords, 0, pointCount);
                if (!curNode.orient.isManhattan()) {
                    for (int i = 0; i < pointCount*2; i++)
                        doubleCoords[i] = DBMath.roundShapeCoord(doubleCoords[i]);
                }
            }
            double anchorX = curNode.anchor.getGridX();
            double anchorY = curNode.anchor.getGridY();
            for (int i = 0; i < pointCount; i++) {
                doubleCoords[i*2 + 0] += anchorX;
                doubleCoords[i*2 + 1] += anchorY;
            }
        }
        if (pureRotate != null) {
            // special case for Poly type CIRCLEARC and THICKCIRCLEARC: if transposing, reverse points
            if ((style == Poly.Type.CIRCLEARC || style == Poly.Type.THICKCIRCLEARC) &&
                    orient.canonic().isCTranspose()) {
                double t;
                t = doubleCoords[2]; doubleCoords[2] = doubleCoords[4]; doubleCoords[4] = t;
                t = doubleCoords[3]; doubleCoords[3] = doubleCoords[5]; doubleCoords[5] = t;
            }

            pureRotate.transform(doubleCoords, 0, doubleCoords, 0, pointCount);
            if (!orient.isManhattan()) {
                for (int i = 0; i < pointCount*2; i++)
                    doubleCoords[i] = DBMath.roundShapeCoord(doubleCoords[i]);
            }
        }
    }

    public void pushIntBox(Layer layer) {
        if (curNode != null && !curNode.orient.isManhattan() || orient != null && !orient.isManhattan()) {
            pushPointLow(intCoords[0], intCoords[1]);
            pushPointLow(intCoords[2], intCoords[1]);
            pushPointLow(intCoords[2], intCoords[3]);
            pushPointLow(intCoords[0], intCoords[3]);
            pushPoly(Poly.Type.FILLED, layer, null, null);
            return;
        }
        if (curNode != null) {
            if (rotateNodes && curNode.orient.canonic() != Orientation.IDENT)
                curNode.orient.rectangleBounds(intCoords);
            int anchorX = (int)curNode.anchor.getGridX();
            int anchorY = (int)curNode.anchor.getGridY();
            intCoords[0] += anchorX;
            intCoords[1] += anchorY;
            intCoords[2] += anchorX;
            intCoords[3] += anchorY;
        }
        if (orient != null)
            orient.rectangleBounds(intCoords);
        addIntBox(intCoords, layer);
    }

    public void pushIntLine(Poly.Type style, Layer layer) {
        if (orient != null) {
            if (!orient.isManhattan()) {
                pushPointLow(intCoords[0], intCoords[1]);
                pushPointLow(intCoords[2], intCoords[3]);
                pushPoly(style, layer, null, null);
                return;
            }
            orient.transformPoints(2, intCoords);
        }
        addIntPoly(2, style, layer, null, null);
    }

    public abstract void addDoublePoly(int numPoints, Poly.Type style, Layer layer, EGraphics graphicsOverride, PrimitivePort pp);
    public void addDoubleTextPoly(int numPoints, Poly.Type style, Layer layer, PrimitivePort pp, String message, TextDescriptor descriptor) {
        addDoublePoly(numPoints, style, layer, null, pp);
    }

    public abstract void addIntPoly(int numPoints, Poly.Type style, Layer layer, EGraphics graphicsOverride, PrimitivePort pp);
    public abstract void addIntBox(int[] coords, Layer layer);

    public static class Shrinkage {
        public static final short EXTEND_90 = 0;
        public static final short EXTEND_0 = 1;
        private static final short EXTEND_ANY = 2;

        private static final int ANGLE_SHIFT = 12;
        private static final int ANGLE_MASK = (1 << ANGLE_SHIFT) - 1;
        private static final int ANGLE_DIAGONAL_MASK = 1 << (ANGLE_SHIFT*2);
        private static final int ANGLE_COUNT_SHIFT = ANGLE_SHIFT*2 + 1;

        private final short[] shrink;

        public Shrinkage() {
            shrink = new short[0];
        }

        public Shrinkage(CellBackup cellBackup) {
            CellRevision cellRevision = cellBackup.cellRevision;
            TechPool techPool = cellBackup.techPool;
            int maxNodeId = -1;
            for (int nodeIndex = 0; nodeIndex < cellRevision.nodes.size(); nodeIndex++)
                maxNodeId = Math.max(maxNodeId, cellRevision.nodes.get(nodeIndex).nodeId);
            int[] angles = new int[maxNodeId+1];
            for (ImmutableArcInst a: cellRevision.arcs) {
                ArcProto ap = techPool.getArcProto(a.protoId);
                if (a.getGridExtendOverMin() + ap.getMaxLayerGridExtend() == 0) continue;
                if (a.tailNodeId == a.headNodeId && a.tailPortId == a.headPortId) {
                    // Fake register for full shrinkage
                    registerArcEnd(angles, a.tailNodeId, 0, false, false);
                    continue;
                }
                boolean is90 = a.isManhattan();
                registerArcEnd(angles, a.tailNodeId, a.getOppositeAngle(), is90, a.isTailExtended());
                registerArcEnd(angles, a.headNodeId, a.getAngle(), is90, a.isHeadExtended());
            }
            short[] shrink = new short[maxNodeId + 1];
            for (int nodeIndex = 0; nodeIndex < cellRevision.nodes.size(); nodeIndex++) {
                ImmutableNodeInst n = cellRevision.nodes.get(nodeIndex);
                NodeProtoId np = n.protoId;
                if (np instanceof PrimitiveNodeId && techPool.getPrimitiveNode((PrimitiveNodeId)np).isArcsShrink())
                    shrink[n.nodeId] = computeShrink(angles[n.nodeId]);
            }
            this.shrink = shrink;
        }

        /**
         * Method to tell the "end shrink" factors on all arcs on a specified ImmutableNodeInst.
         * EXTEND_90 indicates no shortening (extend the arc by half its width).
         * EXTEND_0 indicates no extend.
         * EXTEND_ANY + [0..3600) is a sum of arc angles modulo 3600
         * if this ImmutableNodeInst is a pin which can "isArcsShrink" and this pin connects
         * exactly two arcs whit extended ends and angle between arcs is accute.
         * @param nodeId nodeId of specified ImmutableNodeInst
         * @return shrink factor of specified ImmutableNodeInst is wiped.
         */
        public short get(int nodeId) {
            return nodeId < shrink.length ? shrink[nodeId] : 0;
        }

        private void registerArcEnd(int[] angles, int nodeId, int angle, boolean is90, boolean extended) {
            assert angle >= 0 && angle < 3600;
            int ang = angles[nodeId];
            if (extended) {
                int count = ang >>> ANGLE_COUNT_SHIFT;
                switch (count) {
                    case 0:
                        ang |= angle;
                        ang += (1 << ANGLE_COUNT_SHIFT);
                        break;
                    case 1:
                        ang |= (angle << ANGLE_SHIFT);
                        ang += (1 << ANGLE_COUNT_SHIFT);
                        break;
                    case 2:
                        ang += (1 << ANGLE_COUNT_SHIFT);
                        break;
                }
                if (!is90)
                    ang |= ANGLE_DIAGONAL_MASK;
            } else {
                ang |= (3 << ANGLE_COUNT_SHIFT);
            }
            angles[nodeId] = ang;
        }

        static short computeShrink(int angs) {
            boolean hasAny = (angs&ANGLE_DIAGONAL_MASK) != 0;
            int count = angs >>> ANGLE_COUNT_SHIFT;

            if (hasAny && count == 2) {
                int ang0 = angs & ANGLE_MASK;
                int ang1 = (angs >> ANGLE_SHIFT) & ANGLE_MASK;
                int da = ang0 > ang1 ? ang0 - ang1 : ang1 - ang0;
                if (da == 900 || da == 2700) return EXTEND_90;
                if (da == 1800) return EXTEND_0;
                if (900 < da && da < 2700) {
                    int a = ang0 + ang1;
                    if (a >= 3600)
                        a -= 3600;
                    return (short)(EXTEND_ANY + a);
                }
            }
            return EXTEND_90;
        }
    }

	/**
	 * Class CarbonNanotube determines the location of carbon nanotube rails in the transistor.
	 */
	private class CarbonNanotube
	{
		private ImmutableNodeInst niD;
		private Technology.NodeLayer tubeLayer;
		private int numTubes;
        private long tubeSpacing;

		/**
		 * Constructor to initialize for carbon nanotube rails.
		 */
		private CarbonNanotube(ImmutableNodeInst niD, Technology.NodeLayer tubeLayer)
		{
			this.niD = niD;
			this.tubeLayer = tubeLayer;
			numTubes = 10;
            Variable var = niD.getVar(Technology.NodeLayer.CARBON_NANOTUBE_COUNT);
            if (var != null) numTubes = ((Integer)var.getObject()).intValue();
            tubeSpacing = -1;
            var = niD.getVar(Technology.NodeLayer.CARBON_NANOTUBE_PITCH);
            if (var != null) tubeSpacing = DBMath.lambdaToGrid(((Double)var.getObject()).doubleValue());
		}

		/**
		 * Method to fill in the rails of the carbon nanotube transistor.
		 * Node is in "ni" and the nanotube number (0 based) is in "r".
		 */
		private void fillCutPoly(int r, Poly.Type style, Layer layer, PrimitivePort pp)
		{
        	EPoint size = niD.size;
            long gridWidth = size.getGridX();
            long gridHeight = size.getGridY();
            Technology.TechPoint[] techPoints = tubeLayer.getPoints();
            long lx = techPoints[0].getX().getGridAdder() + (long)(gridWidth*techPoints[0].getX().getMultiplier());
            long hx = techPoints[1].getX().getGridAdder() + (long)(gridWidth*techPoints[1].getX().getMultiplier());
            long ly = techPoints[0].getY().getGridAdder() + (long)(gridHeight*techPoints[0].getY().getMultiplier());
            long hy = techPoints[1].getY().getGridAdder() + (long)(gridHeight*techPoints[1].getY().getMultiplier());
            if (tubeSpacing < 0) tubeSpacing = (hx-lx) / (numTubes*2-1);
            long tubeDia = (hx-lx - (numTubes-1)*tubeSpacing) / numTubes;
            long tubeHalfHeight = (hy-ly) / 2;
//System.out.println("LAYER FROM "+lx+"<=X<="+hx+" AND "+ly+"<=Y<="+hy+" TUBE SPACING="+tubeSpacing+" TUBE DIAMETER="+tubeDia);
            long cX = lx + (tubeDia>>1) + (tubeDia+tubeSpacing)*r;
            long cY = 0; // + (ly + hy)>>1;
            double lX = cX - (tubeDia >> 1);
            double hX = cX + (tubeDia >> 1);
            double lY = cY - tubeHalfHeight;
            double hY = cY + tubeHalfHeight;
//System.out.println("   SO TUBE "+r+", CENTERED AT ("+cX+","+cY+") IS FROM "+lX+"<=X<="+hX+" AND "+lY+"<=Y<="+hY);
            pushPoint(lX, lY);
            pushPoint(hX, lY);
            pushPoint(hX, hY);
            pushPoint(lX, hY);
            pushPoly(style, layer, null, pp);
		}
	}

	/**
	 * Class MultiCutData determines the locations of cuts in a multi-cut contact node.
	 */
	private class MultiCutData
	{
		/** the size of each cut */													private long cutSizeX, cutSizeY;
		/** the separation between cuts */											private long cutSep;
		/** the separation between cuts */											private long cutSep1D;
		/** the separation between cuts in 3-neighboring or more cases */			private long cutSep2D;
		/** the number of cuts in X and Y */										private int cutsX, cutsY;
		/** the total number of cuts */												private int cutsTotal;
		/** the "reasonable" number of cuts (around the outside only) */			private int cutsReasonable;
		/** the X coordinate of the leftmost cut's center */						private long cutBaseX;
		/** the Y coordinate of the topmost cut's center */							private long cutBaseY;
		/** the lowest X cut that will be shifted to the left */					private long cutShiftLeftXPos;
		/** the lowest X cut that will be shifted to the right */					private long cutShiftRightXPos;
		/** the X cut that will not be shifted (because it is the center cut) */	private long cutShiftNoneXPos;
		/** the lowest Y cut that will be shifted down */							private long cutShiftDownYPos;
		/** the lowest Y cut that will be shifted up */								private long cutShiftUpYPos;
		/** the Y cut that will not be shifted (because it is the center cut) */	private long cutShiftNoneYPos;
		/** the amount X cuts will be shifted to the left */						private long cutShiftLeftXAmt;
		/** the amount X cuts will be shifted to the right */						private long cutShiftRightXAmt;
		/** the amount Y cuts will be shifted down */								private long cutShiftDownYAmt;
		/** the amount Y cuts will be shifted up */									private long cutShiftUpYAmt;

		/** cut position of last top-edge cut (for interior-cut elimination) */		private double cutTopEdge;
		/** cut position of last left-edge cut  (for interior-cut elimination) */	private double cutLeftEdge;
		/** cut position of last right-edge cut  (for interior-cut elimination) */	private double cutRightEdge;

		/**
		 * Constructor to initialize for multiple cuts.
		 */
		private MultiCutData(ImmutableNodeInst niD, Technology.NodeLayer cutLayer)
		{
            calculateInternalData(niD, cutLayer);
		}

		/**
		 * Constructor to initialize for multiple cuts.
		 * @param niD the NodeInst with multiple cuts.
		 */
		private MultiCutData(ImmutableNodeInst niD, TechPool techPool)
		{
            calculateInternalData(niD, techPool.getPrimitiveNode((PrimitiveNodeId)niD.protoId).findMulticut());
		}

        private void calculateInternalData(ImmutableNodeInst niD, Technology.NodeLayer cutLayer)
        {
        	EPoint size = niD.size;
            assert cutLayer.getRepresentation() == Technology.NodeLayer.MULTICUTBOX;
            long gridWidth = size.getGridX();
            long gridHeight = size.getGridY();
            Technology.TechPoint[] techPoints = cutLayer.getPoints();
            long lx = techPoints[0].getX().getGridAdder() + (long)(gridWidth*techPoints[0].getX().getMultiplier());
            long hx = techPoints[1].getX().getGridAdder() + (long)(gridWidth*techPoints[1].getX().getMultiplier());
            long ly = techPoints[0].getY().getGridAdder() + (long)(gridHeight*techPoints[0].getY().getMultiplier());
            long hy = techPoints[1].getY().getGridAdder() + (long)(gridHeight*techPoints[1].getY().getMultiplier());
            cutSizeX = cutLayer.getGridMulticutSizeX();
            cutSizeY = cutLayer.getGridMulticutSizeX();
            cutSep1D = cutLayer.getGridMulticutSep1D();
            cutSep2D = cutLayer.getGridMulticutSep2D();
            if (!niD.isEasyShape())
            {
                // get the value of the cut spacing
                Variable var = niD.getVar(Technology.NodeLayer.CUT_SPACING);
                if (var != null)
                {
                    double spacingD = VarContext.objectToDouble(var.getObject(), -1);
                    if (spacingD != -1)
                        cutSep1D = cutSep2D = DBMath.lambdaToGrid(spacingD);
                }
            }

			// determine the actual node size
            cutBaseX = (lx + hx)>>1;
            cutBaseY = (ly + hy)>>1;
			long cutAreaWidth = hx - lx;
			long cutAreaHeight = hy - ly;

			// number of cuts depends on the size of cut area
            int oneDcutsX = 1 + (int)(cutAreaWidth / (cutSizeX+cutSep1D));
			int oneDcutsY = 1 + (int)(cutAreaHeight / (cutSizeY+cutSep1D));

			// check if configuration gives 2D cuts
			cutSep = cutSep1D;
			cutsX = oneDcutsX;
			cutsY = oneDcutsY;
			if (cutsX > 1 && cutsY > 1)
			{
				// recompute number of cuts for 2D spacing
	            int twoDcutsX = 1 + (int)(cutAreaWidth / (cutSizeX+cutSep2D));
				int twoDcutsY = 1 + (int)(cutAreaHeight / (cutSizeY+cutSep2D));
				cutSep = cutSep2D;
				cutsX = twoDcutsX;
				cutsY = twoDcutsY;
				if (cutsX == 1 || cutsY == 1)
				{
					// 1D separation sees a 2D grid, but 2D separation sees a linear array: use 1D linear settings
					cutSep = cutSep1D;
					if (cutAreaWidth > cutAreaHeight)
					{
						cutsX = oneDcutsX;
					} else
					{
						cutsY = oneDcutsY;
					}
				}
			}
			if (cutsX <= 0) cutsX = 1;
			if (cutsY <= 0) cutsY = 1;

			// compute spacing rules
			cutShiftLeftXPos = cutsX;
			cutShiftRightXPos = cutsX;
			cutShiftDownYPos = cutsY;
			cutShiftUpYPos = cutsY;
			cutShiftNoneXPos = -1;
			cutShiftNoneYPos = -1;
            if (!niD.isEasyShape()) {
                Integer cutAlignment = niD.getVarValue(Technology.NodeLayer.CUT_ALIGNMENT, Integer.class);
                if (cutAlignment != null) {
                    if (cutAlignment.intValue() == Technology.NodeLayer.MULTICUT_SPREAD)
                    {
                        // spread cuts to edge, leaving gap in center
                        cutShiftLeftXPos = 0;
                        cutShiftDownYPos = 0;
                        cutShiftLeftXAmt = (1-cutsX)*(cutSizeX + cutSep)/2 - lx;
                        cutShiftDownYAmt = (1-cutsY)*(cutSizeY + cutSep)/2 - ly;

                        cutShiftRightXPos = cutsX/2;
                        cutShiftUpYPos = cutsY/2;
                        cutShiftRightXAmt = hx - (cutsX-1)*(cutSizeX + cutSep)/2;
                        cutShiftUpYAmt = hy - (cutsY-1)*(cutSizeY + cutSep)/2;
                        if ((cutsX&1) != 0) cutShiftNoneXPos = cutsX/2;
                        if ((cutsY&1) != 0) cutShiftNoneYPos = cutsY/2;
                    } else if (cutAlignment.intValue() == Technology.NodeLayer.MULTICUT_CORNER)
                    {
                        // shift cuts to lower edge
                        cutShiftLeftXPos = 0;
                        cutShiftDownYPos = 0;
                        cutShiftLeftXAmt = (1-cutsX)*(cutSizeX + cutSep)/2 - lx;
                        cutShiftDownYAmt = (1-cutsY)*(cutSizeY + cutSep)/2 - ly;
                    }
                }
            }

			cutsReasonable = cutsTotal = cutsX * cutsY;
			if (cutsTotal != 1)
			{
				// prepare for the multiple contact cut locations
				if (cutsX > 2 && cutsY > 2)
				{
					cutsReasonable = cutsX * 2 + (cutsY-2) * 2;
					cutTopEdge = cutsX*2;
					cutLeftEdge = cutsX*2 + cutsY-2;
					cutRightEdge = cutsX*2 + (cutsY-2)*2;
				}
			}
        }

        /**
		 * Method to return the number of cuts in the contact node.
		 * @return the number of cuts in the contact node.
		 */
		private int numCuts() { return cutsTotal; }

		/**
		 * Method to return the number of cuts along X axis in the contact node.
		 * @return the number of cuts in the contact node along X axis.
		 */
		private int numCutsX() { return cutsX; }

		/**
		 * Method to return the number of cuts along Y axis in the contact node.
		 * @return the number of cuts in the contact node along Y axis.
		 */
		private int numCutsY() { return cutsY; }

        /**
         * Method to return the size of the cut along X.
         */
        private double getCutSizeX() { return cutSizeX; }

        /**
         * Method to return the size of the cut along Y.
         */
        private double getCutSizeY() { return cutSizeY; }

        /**
         * Method to fill in the contact cuts based on anchor information.
        */
        private void fillCutPoly(int cut, Poly.Type style, Layer layer, PrimitivePort pp)
		{
            long cX = cutBaseX;
            long cY = cutBaseY;
            if (cutsX > 1 || cutsY > 1)
            {
                if (cutsX > 2 && cutsY > 2)
                {
                    // rearrange cuts so that the initial ones go around the outside
                    if (cut < cutsX) {
                        // bottom edge: it's ok as is
                    } else if (cut < cutTopEdge) {
                        // top edge: shift up
                        cut += cutsX * (cutsY-2);
                    } else if (cut < cutLeftEdge) {
                        // left edge: rearrange
                        cut = (int)((cut - cutTopEdge) * cutsX + cutsX);
                    } else if (cut < cutRightEdge) {
                        // right edge: rearrange
                        cut = (int)((cut - cutLeftEdge) * cutsX + cutsX*2-1);
                    } else {
                        // center: rearrange and scale down
                        cut = cut - (int)cutRightEdge;
                        int cutx = cut % (cutsX-2);
                        int cuty = cut / (cutsX-2);
                        cut = cuty * cutsX + cutx+cutsX+1;
                    }
                }

                // locate the X center of the cut
                if (cutsX != 1)
                {
                	int cutNum = cut % cutsX;
                    cX += (cutNum*2 - (cutsX - 1))*(cutSizeX + cutSep)*0.5;
                    if (cutNum != cutShiftNoneXPos)
                    {
	                    if (cutNum >= cutShiftRightXPos) cX += cutShiftRightXAmt; else
		                    if (cutNum >= cutShiftLeftXPos) cX -= cutShiftLeftXAmt;
                    }
                }

                // locate the Y center of the cut
                if (cutsY != 1)
                {
                	int cutNum = cut / cutsX;
                    cY += (cutNum*2 - (cutsY - 1))*(cutSizeY + cutSep)*0.5;
                    if (cutNum != cutShiftNoneYPos)
                    {
	                    if (cutNum >= cutShiftUpYPos) cY += cutShiftUpYAmt; else
		                    if (cutNum >= cutShiftDownYPos) cY -= cutShiftDownYAmt;
                    }
                }
            }
            double lX = cX - (cutSizeX >> 1);
            double hX = cX + (cutSizeX >> 1);
            double lY = cY - (cutSizeY >> 1);
            double hY = cY + (cutSizeY >> 1);
            pushPoint(lX, lY);
            pushPoint(hX, lY);
            pushPoint(hX, hY);
            pushPoint(lX, hY);
            pushPoly(style, layer, null, pp);
		}
	}

	/**
	 * Class SerpentineTrans here.
	 */
	private class SerpentineTrans
	{
		private static final int LEFTANGLE =  900;
		private static final int RIGHTANGLE =  2700;

		/** the ImmutableNodeInst that is this serpentine transistor */			private ImmutableNodeInst theNode;
		/** the prototype of this serpentine transistor */						private PrimitiveNode theProto;
		/** the number of polygons that make up this serpentine transistor */	private int layersTotal;
		/** the number of segments in this serpentine transistor */				private int numSegments;
		/** the extra gate width of this serpentine transistor */				private double extraScale;
		/** the node layers that make up this serpentine transistor */			private Technology.NodeLayer [] primLayers;
		/** the gate coordinates for this serpentine transistor */				private Point2D [] points;
		/** the defining values for this serpentine transistor */				private double [] specialValues;
		/** true if there are separate field and gate polys */					private boolean fieldPolyOnEndsOnly;
		/** counter for filling the polygons of the serpentine transistor */	private int fillBox;

		/**
		 * Constructor throws initialize for a serpentine transistor.
		 * @param niD the NodeInst with a serpentine transistor.
		 */
		private SerpentineTrans(ImmutableNodeInst niD, PrimitiveNode protoType, Technology.NodeLayer [] pLayers)
		{
			theNode = niD;
			layersTotal = 0;
			points = niD.getTrace();
			if (points != null)
			{
				if (points.length < 2) points = null;
			}
			if (points != null)
			{
				theProto = protoType;
				specialValues = theProto.getSpecialValues();
				primLayers = pLayers;
				int count = primLayers.length;
				numSegments = points.length - 1;
				layersTotal = count;
//				layersTotal = count * numSegments;

				extraScale = 0;
				double length = niD.getSerpentineTransistorLength();
				if (length > 0) extraScale = (length - specialValues[3]) / 2;

				// see if there are separate field and gate poly layers
				fieldPolyOnEndsOnly = false;
				int numFieldPoly = 0, numGatePoly = 0;
				for(int i=0; i<count; i++)
				{
					if (primLayers[i].getLayer().getFunction().isPoly())
					{
						if (primLayers[i].getLayer().getFunction() == Layer.Function.GATE) numGatePoly++; else
							numFieldPoly++;
					}
				}
				if (numFieldPoly > 0 && numGatePoly > 0)
				{
					// when there are both field and gate poly elements, use field poly only on the ends
					fieldPolyOnEndsOnly = true;
//					layersTotal = (count-numFieldPoly) * numSegments + numFieldPoly;
				}
			}
		}

		/**
		 * Method to tell whether this SerpentineTrans object has valid outline information.
		 * @return true if the data exists.
		 */
		private boolean hasValidData() { return points != null; }

		/**
		 * Method to start the filling of polygons in the serpentine transistor.
		 * Call this before repeated calls to "fillTransPoly".
		 */
		private void initTransPolyFilling() { fillBox = 0; }

		/**
		 * Method to describe a box of a serpentine transistor.
		 * If the variable "trace" exists on the node, get that
		 * x/y/x/y information as the centerline of the serpentine path.  The outline is
		 * placed in the polygon "poly".
		 * NOTE: For each trace segment, the left hand side of the trace
		 * will contain the polygons that appear ABOVE the gate in the node
		 * definition. That is, the "top" port and diffusion will be above a
		 * gate segment that extends from left to right, and on the left of a
		 * segment that goes from bottom to top.
		 */
		private void fillTransPoly()
		{
			int element = fillBox++;
			Technology.NodeLayer primLayer = primLayers[element];
            Layer layer = primLayer.getLayer();
            if (skipLayer(layer)) return;
			double extendt = primLayer.getSerpentineExtentT();
			double extendb = primLayer.getSerpentineExtentB();

			// if field poly appears only on the ends of the transistor, ignore interior requests
			boolean extendEnds = true;
			if (fieldPolyOnEndsOnly)
			{
				if (layer.getFunction().isPoly())
				{
					if (layer.getFunction() == Layer.Function.GATE)
					{
						// found the gate poly: do not extend it
						extendEnds = false;
					} else
					{
						// found piece of field poly
						if (extendt != 0)
						{
							// first endcap: extend "thissg" 180 degrees back
							int thissg = 0;   int nextsg = 1;
							Point2D thisPt = points[thissg];
							Point2D nextPt = points[nextsg];
							int angle = DBMath.figureAngle(thisPt, nextPt);
							nextPt = thisPt;
							int ang = angle+1800;
							thisPt = DBMath.addPoints(thisPt, DBMath.cos(ang) * extendt, DBMath.sin(ang) * extendt);
							buildSerpentinePoly(element, 0, numSegments, thisPt, nextPt, angle);
                            return;
						} else if (extendb != 0)
						{
							// last endcap: extend "next" 0 degrees forward
							int thissg = numSegments-1;   int nextsg = numSegments;
							Point2D thisPt = points[thissg];
							Point2D nextPt = points[nextsg];
							int angle = DBMath.figureAngle(thisPt, nextPt);
							thisPt = nextPt;
							nextPt = DBMath.addPoints(nextPt, DBMath.cos(angle) * extendb, DBMath.sin(angle) * extendb);
							buildSerpentinePoly(element, 0, numSegments, thisPt, nextPt, angle);
                            return;
						}
					}
				}
			}

			// fill the polygon
			Point2D [] outPoints = new Point2D.Double[(numSegments+1)*2];
			for(int segment=0; segment<numSegments; segment++)
			{
				int thissg = segment;   int nextsg = segment+1;
				Point2D thisPt = points[thissg];
				Point2D nextPt = points[nextsg];
				int angle = DBMath.figureAngle(thisPt, nextPt);
				if (extendEnds)
				{
					if (thissg == 0)
					{
						// extend "thissg" 180 degrees back
						int ang = angle+1800;
						thisPt = DBMath.addPoints(thisPt, DBMath.cos(ang) * extendt, DBMath.sin(ang) * extendt);
					}
					if (nextsg == numSegments)
					{
						// extend "next" 0 degrees forward
						nextPt = DBMath.addPoints(nextPt, DBMath.cos(angle) * extendb, DBMath.sin(angle) * extendb);
					}
				}

				// see if nonstandard width is specified
				double lwid = primLayer.getSerpentineLWidth();
				double rwid = primLayer.getSerpentineRWidth();
				lwid += extraScale;
				rwid += extraScale;

				// compute endpoints of line parallel to and left of center line
				int ang = angle+LEFTANGLE;
				double sin = DBMath.sin(ang) * lwid;
				double cos = DBMath.cos(ang) * lwid;
				Point2D thisL = DBMath.addPoints(thisPt, cos, sin);
				Point2D nextL = DBMath.addPoints(nextPt, cos, sin);

				// compute endpoints of line parallel to and right of center line
				ang = angle+RIGHTANGLE;
				sin = DBMath.sin(ang) * rwid;
				cos = DBMath.cos(ang) * rwid;
				Point2D thisR = DBMath.addPoints(thisPt, cos, sin);
				Point2D nextR = DBMath.addPoints(nextPt, cos, sin);

				// determine proper intersection of this and the previous segment
				if (thissg != 0)
				{
					Point2D otherPt = points[thissg-1];
					int otherang = DBMath.figureAngle(otherPt, thisPt);
					if (otherang != angle)
					{
						ang = otherang + LEFTANGLE;
						thisL = DBMath.intersect(DBMath.addPoints(thisPt, DBMath.cos(ang)*lwid, DBMath.sin(ang)*lwid),
							otherang, thisL,angle);
						ang = otherang + RIGHTANGLE;
						thisR = DBMath.intersect(DBMath.addPoints(thisPt, DBMath.cos(ang)*rwid, DBMath.sin(ang)*rwid),
							otherang, thisR,angle);
					}
				}

				// determine proper intersection of this and the next segment
				if (nextsg != numSegments)
				{
					Point2D otherPt = points[nextsg+1];
					int otherang = DBMath.figureAngle(nextPt, otherPt);
					if (otherang != angle)
					{
						ang = otherang + LEFTANGLE;
						Point2D newPtL = DBMath.addPoints(nextPt, DBMath.cos(ang)*lwid, DBMath.sin(ang)*lwid);
						nextL = DBMath.intersect(newPtL, otherang, nextL,angle);
						ang = otherang + RIGHTANGLE;
						Point2D newPtR = DBMath.addPoints(nextPt, DBMath.cos(ang)*rwid, DBMath.sin(ang)*rwid);
						nextR = DBMath.intersect(newPtR, otherang, nextR,angle);
					}
				}

				// fill the polygon
				if (segment == 0)
				{
					// fill in the first two points
					outPoints[0] = thisL;
					outPoints[1] = nextL;
					outPoints[(numSegments+1)*2-2] = nextR;
					outPoints[(numSegments+1)*2-1] = thisR;
				} else
				{
					outPoints[segment+1] = nextL;
					outPoints[(numSegments+1)*2-2-segment] = nextR;
				}
			}

            for (Point2D point: outPoints)
                pushPoint(point.getX()*DBMath.GRID, point.getY()*DBMath.GRID);
            pushPoly(primLayer.getStyle(), layer, null, primLayer.getPort(theProto));
		}

		private void buildSerpentinePoly(int element, int thissg, int nextsg, Point2D thisPt, Point2D nextPt, int angle)
		{
			// see if nonstandard width is specified
			Technology.NodeLayer primLayer = primLayers[element];
			double lwid = primLayer.getSerpentineLWidth();
			double rwid = primLayer.getSerpentineRWidth();
			lwid += extraScale;
			rwid += extraScale;

			// compute endpoints of line parallel to and left of center line
			int ang = angle+LEFTANGLE;
			double sin = DBMath.sin(ang) * lwid;
			double cos = DBMath.cos(ang) * lwid;
			Point2D thisL = DBMath.addPoints(thisPt, cos, sin);
			Point2D nextL = DBMath.addPoints(nextPt, cos, sin);

			// compute endpoints of line parallel to and right of center line
			ang = angle+RIGHTANGLE;
			sin = DBMath.sin(ang) * rwid;
			cos = DBMath.cos(ang) * rwid;
			Point2D thisR = DBMath.addPoints(thisPt, cos, sin);
			Point2D nextR = DBMath.addPoints(nextPt, cos, sin);

			// determine proper intersection of this and the previous segment
			if (thissg != 0)
			{
				Point2D otherPt = points[thissg-1];
				int otherang = DBMath.figureAngle(otherPt, thisPt);
				if (otherang != angle)
				{
					ang = otherang + LEFTANGLE;
					thisL = DBMath.intersect(DBMath.addPoints(thisPt, DBMath.cos(ang)*lwid, DBMath.sin(ang)*lwid),
						otherang, thisL,angle);
					ang = otherang + RIGHTANGLE;
					thisR = DBMath.intersect(DBMath.addPoints(thisPt, DBMath.cos(ang)*rwid, DBMath.sin(ang)*rwid),
						otherang, thisR,angle);
				}
			}

			// determine proper intersection of this and the next segment
			if (nextsg != numSegments)
			{
				Point2D otherPt = points[nextsg+1];
				int otherang = DBMath.figureAngle(nextPt, otherPt);
				if (otherang != angle)
				{
					ang = otherang + LEFTANGLE;
					Point2D newPtL = DBMath.addPoints(nextPt, DBMath.cos(ang)*lwid, DBMath.sin(ang)*lwid);
					nextL = DBMath.intersect(newPtL, otherang, nextL,angle);
					ang = otherang + RIGHTANGLE;
					Point2D newPtR = DBMath.addPoints(nextPt, DBMath.cos(ang)*rwid, DBMath.sin(ang)*rwid);
					nextR = DBMath.intersect(newPtR, otherang, nextR,angle);
				}
			}

			// fill the polygon
            pushPoint(thisL.getX()*DBMath.GRID, thisL.getY()*DBMath.GRID);
            pushPoint(thisR.getX()*DBMath.GRID, thisR.getY()*DBMath.GRID);
            pushPoint(nextR.getX()*DBMath.GRID, nextR.getY()*DBMath.GRID);
            pushPoint(nextL.getX()*DBMath.GRID, nextL.getY()*DBMath.GRID);
            pushPoly(primLayer.getStyle(), primLayer.getLayer(), null, primLayer.getPort(theProto));
		}

		/**
		 * Method to describe a port in a transistor that is part of a serpentine path.
		 * The port path is shrunk by "diffInset" in the length and is pushed "diffExtend" from the centerline.
		 * The default width of the transistor is "defWid".
		 * The assumptions about directions are:
		 * Segments have port 1 to the left, and port 3 to the right of the gate trace.
		 * Port 0, the "left-hand" end of the gate, appears at the starting
		 * end of the first trace segment; port 2, the "right-hand" end of the gate,
		 * appears at the end of the last trace segment.  Port 3 is drawn as a
		 * reflection of port 1 around the trace.
		 * The poly ports are extended "polyExtend" beyond the appropriate end of the trace
		 * and are inset by "polyInset" from the polysilicon edge.
		 * The diffusion ports are extended "diffExtend" from the polysilicon edge
		 * and set in "diffInset" from the ends of the trace segment.
		 */
		private void fillTransPort(PortProto pp)
		{
			double diffInset = specialValues[1];
			double diffExtend = specialValues[2];
			double defWid = specialValues[3] + extraScale;
			double polyInset = specialValues[4];
			double polyExtend = specialValues[5];

			// prepare to fill the serpentine transistor port
			int total = points.length;

			// determine which port is being described
			int which = 0;
			for(Iterator<PortProto> it = theProto.getPorts(); it.hasNext(); )
			{
				PortProto lpp = it.next();
				if (lpp == pp) break;
				which++;
			}
            assert which == pp.getPortIndex();

			// ports 0 and 2 are poly (simple)
			if (which == 0)
			{
				Point2D thisPt = new Point2D.Double(points[0].getX(), points[0].getY());
				Point2D nextPt = new Point2D.Double(points[1].getX(), points[1].getY());
				int angle = DBMath.figureAngle(thisPt, nextPt);
				int ang = (angle+1800) % 3600;
				thisPt.setLocation(thisPt.getX() + DBMath.cos(ang) * polyExtend,
					thisPt.getY() + DBMath.sin(ang) * polyExtend);

				ang = (angle+LEFTANGLE) % 3600;
				Point2D end1 = new Point2D.Double(thisPt.getX() + DBMath.cos(ang) * (defWid/2-polyInset),
					thisPt.getY() + DBMath.sin(ang) * (defWid/2-polyInset));

				ang = (angle+RIGHTANGLE) % 3600;
				Point2D end2 = new Point2D.Double(thisPt.getX() + DBMath.cos(ang) * (defWid/2-polyInset),
					thisPt.getY() + DBMath.sin(ang) * (defWid/2-polyInset));

                pushPoint(end1.getX()*DBMath.GRID, end1.getY()*DBMath.GRID);
                pushPoint(end2.getX()*DBMath.GRID, end2.getY()*DBMath.GRID);
                pushPoly(Poly.Type.OPENED, null, null, null);
                return;
//				Point2D [] portPoints = new Point2D.Double[2];
//				portPoints[0] = end1;
//				portPoints[1] = end2;
//				trans.transform(portPoints, 0, portPoints, 0, 2);
//				Poly retPoly = new Poly(portPoints);
//				retPoly.setStyle(Poly.Type.OPENED);
//				return retPoly;
			}
			if (which == 2)
			{
				Point2D thisPt = new Point2D.Double(points[total-1].getX(), points[total-1].getY());
				Point2D nextPt = new Point2D.Double(points[total-2].getX(), points[total-2].getY());
				int angle = DBMath.figureAngle(thisPt, nextPt);
				int ang = (angle+1800) % 3600;
				thisPt.setLocation(thisPt.getX() + DBMath.cos(ang) * polyExtend,
					thisPt.getY() + DBMath.sin(ang) * polyExtend);

				ang = (angle+LEFTANGLE) % 3600;
				Point2D end1 = new Point2D.Double(thisPt.getX() + DBMath.cos(ang) * (defWid/2-polyInset),
					thisPt.getY() + DBMath.sin(ang) * (defWid/2-polyInset));

				ang = (angle+RIGHTANGLE) % 3600;
				Point2D end2 = new Point2D.Double(thisPt.getX() + DBMath.cos(ang) * (defWid/2-polyInset),
					thisPt.getY() + DBMath.sin(ang) * (defWid/2-polyInset));

                pushPoint(end1.getX()*DBMath.GRID, end1.getY()*DBMath.GRID);
                pushPoint(end2.getX()*DBMath.GRID, end2.getY()*DBMath.GRID);
                pushPoly(Poly.Type.OPENED, null, null, null);
                return;
//				Point2D [] portPoints = new Point2D.Double[2];
//				portPoints[0] = end1;
//				portPoints[1] = end2;
//				trans.transform(portPoints, 0, portPoints, 0, 2);
//				Poly retPoly = new Poly(portPoints);
//				retPoly.setStyle(Poly.Type.OPENED);
//				return retPoly;
			}

			// port 3 is the negated path side of port 1
			if (which == 3)
			{
				diffExtend = -diffExtend;
				defWid = -defWid;
			}

			// extra port on some n-transistors
			if (which == 4) diffExtend = defWid = 0;

			Point2D [] portPoints = new Point2D.Double[total];
			Point2D lastPoint = null;
			int lastAngle = 0;
			for(int nextIndex=1; nextIndex<total; nextIndex++)
			{
				int thisIndex = nextIndex-1;
				Point2D thisPt = new Point2D.Double(points[thisIndex].getX(), points[thisIndex].getY());
				Point2D nextPt = new Point2D.Double(points[nextIndex].getX(), points[nextIndex].getY());
				int angle = DBMath.figureAngle(thisPt, nextPt);

				// determine the points
				if (thisIndex == 0)
				{
					// extend "this" 0 degrees forward
					thisPt.setLocation(thisPt.getX() + DBMath.cos(angle) * diffInset,
						thisPt.getY() + DBMath.sin(angle) * diffInset);
				}
				if (nextIndex == total-1)
				{
					// extend "next" 180 degrees back
					int backAng = (angle+1800) % 3600;
					nextPt.setLocation(nextPt.getX() + DBMath.cos(backAng) * diffInset,
						nextPt.getY() + DBMath.sin(backAng) * diffInset);
				}

				// compute endpoints of line parallel to center line
				int ang = (angle+LEFTANGLE) % 3600;
				double sine = DBMath.sin(ang);
				double cosine = DBMath.cos(ang);
				thisPt.setLocation(thisPt.getX() + cosine * (defWid/2+diffExtend),
					thisPt.getY() + sine * (defWid/2+diffExtend));
				nextPt.setLocation(nextPt.getX() + cosine * (defWid/2+diffExtend),
					nextPt.getY() + sine * (defWid/2+diffExtend));

				if (thisIndex != 0)
				{
					// compute intersection of this and previous line
					thisPt = DBMath.intersect(lastPoint, lastAngle, thisPt, angle);
				}
				portPoints[thisIndex] = thisPt;
				lastPoint = thisPt;
				lastAngle = angle;
				if (nextIndex == total-1)
					portPoints[nextIndex] = nextPt;
			}
            for (Point2D point: portPoints)
                pushPoint(point.getX()*DBMath.GRID, point.getY()*DBMath.GRID);
            pushPoly(Poly.Type.OPENED, null, null, null);
//			if (total > 0)
//				trans.transform(portPoints, 0, portPoints, 0, total);
//			Poly retPoly = new Poly(portPoints);
//			retPoly.setStyle(Poly.Type.OPENED);
//			return retPoly;
		}
	}
}
