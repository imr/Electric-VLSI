/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WindowFrame.java
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
package com.sun.electric.plugins.j3d;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.user.ui.*;
import com.sun.electric.tool.user.*;
import com.sun.electric.tool.Job;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;

// Java3D packages
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewingPlatform;
import com.sun.j3d.utils.universe.Viewer;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.behaviors.mouse.*;
import com.sun.j3d.utils.picking.PickTool;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.picking.PickCanvas;

import javax.media.j3d.*;
import javax.vecmath.*;

import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.awt.print.PrinterException;
import java.awt.print.Printable;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda
 * Date: Jun 14, 2004
 * Time: 5:20:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class View3DWindow extends JPanel
        implements WindowContent, MouseMotionListener, MouseListener, MouseWheelListener, KeyListener, ActionListener,
        HighlightListener
{
	private SimpleUniverse u;
	private Canvas3D canvas;
	private TransformGroup objTrans;
	private MouseBehavior rotateB, translateB;
	private JMouseZoom zoomB;

    //private OrbitBehavior orbit;
	/** the window frame containing this editwindow */      private WindowFrame wf;
	/** reference to 2D view of the cell */                 private WindowContent view2D;
	/** the cell that is in the window */					private Cell cell;
    /** scale factor in Z axis */                           private double scale = User.get3DFactor();
	/** Highlighter for this window */                      private Highlighter highlighter;
	private PickCanvas pickCanvas;
	/** Lis with all Shape3D drawn per ElectricObject */    private HashMap electricObjectMap = new HashMap();

	// Done only once.
	/** cell has a unique appearance **/                    private static JAppearance cellApp = new JAppearance(null);
	/** highligh appearance **/                             private static JAppearance highligtAp = new JAppearance(null);
    /** standard colors to be used by materials **/         private static Color3f black = new Color3f(0.0f, 0.0f, 0.0f);
	/** standard colors to be used by materials **/         private static Color3f white = new Color3f(1.0f, 1.0f, 1.0f);

    static {

	    //** Data for cells
		Color3f objColor = new Color3f(Color.GRAY);
		ColoringAttributes ca = new ColoringAttributes();
		ca.setColor(objColor);
		cellApp.setColoringAttributes(ca);

		TransparencyAttributes ta = new TransparencyAttributes();
		ta.setTransparencyMode(TransparencyAttributes.SCREEN_DOOR);
		ta.setTransparency(0.5f);
		cellApp.setTransparencyAttributes(ta);

			// Set up the polygon attributes
		PolygonAttributes pa = new PolygonAttributes();
		pa.setCullFace(PolygonAttributes.CULL_NONE);
		pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
		cellApp.setPolygonAttributes(pa);

		TextureAttributes texAttr = new TextureAttributes();
		texAttr.setTextureMode(TextureAttributes.MODULATE);
		//texAttr.setTextureColorTable(pattern);
		cellApp.setTextureAttributes(texAttr);

		LineAttributes lineAttr = new LineAttributes();
		lineAttr.setLineAntialiasingEnable(true);
		cellApp.setLineAttributes(lineAttr);

	    // For highlighted objects
	    highligtAp.setColoringAttributes(new ColoringAttributes(black, ColoringAttributes.SHADE_GOURAUD));
	    TransparencyAttributes hTa = new TransparencyAttributes(TransparencyAttributes.BLENDED, 0.5f);
	    //PolygonAttributes hPa = new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, 0);
	    //highligtAp.setPolygonAttributes(hPa);
	    highligtAp.setTransparencyAttributes(hTa);
	}

	private static class JAppearance extends Appearance
	{
		private EGraphics graphics; // reference to layer for fast access to appearance
		public JAppearance(EGraphics graphics)
		{
			super();
			this.graphics = graphics;
		}
		public void seGraphics(EGraphics graphics) {this.graphics = graphics;}
		public EGraphics getGraphics() { return graphics;}
	}
	// constructor
	public View3DWindow(Cell cell, WindowFrame wf, WindowContent view2D)
	{
		this.cell = cell;
        this.wf = wf;
		this.view2D = view2D;

		highlighter = new Highlighter(Highlighter.SELECT_HIGHLIGHTER, wf);
        highlighter.addHighlightListener(this);

		/*
		overall = new JPanel();
		//overall.setLayout(new GridBagLayout());
        overall.setLayout(new BorderLayout()); */


		setLayout(new BorderLayout());
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        BoundingSphere infiniteBounds = new BoundingSphere(new Point3d(), Double.MAX_VALUE);

		canvas = new Canvas3D(config);
		//overall.add("Center", c);
        //overall.add(this);
		add("Center", canvas);
		canvas.addMouseListener(this);

		// Create a simple scene and attach it to the virtual universe
		BranchGroup scene = createSceneGraph(cell, infiniteBounds);

		ViewingPlatform viewP = new ViewingPlatform(4);
		viewP.setCapability(ViewingPlatform.ALLOW_CHILDREN_READ);
		Viewer viewer = new Viewer(canvas);
		//u = new SimpleUniverse(canvas, 4);
		u = new SimpleUniverse(viewP, viewer);
		u.addBranchGraph(scene);

        // This will move the ViewPlatform back a bit so the
        // objects in the scene can be viewed.

		ViewingPlatform viewingPlatform = u.getViewingPlatform();
		//viewingPlatform.setCapability(ViewingPlatform.ALLOW_CHILDREN_READ);

        MouseTranslate translate = new MouseTranslate(canvas, MouseTranslate.INVERT_INPUT);
        translate.setTransformGroup(viewingPlatform.getMultiTransformGroup().getTransformGroup(2));
        translate.setSchedulingBounds(infiniteBounds);
        translate.setFactor(0.1); // default 0.02
        BranchGroup translateBG = new BranchGroup();
        translateBG.addChild(translate);
        viewingPlatform.addChild(translateBG);
		translateB = translate;

        JMouseZoom zoom = new JMouseZoom(canvas, MouseZoom.INVERT_INPUT);
        zoom.setTransformGroup(viewingPlatform.getMultiTransformGroup().getTransformGroup(1));
        zoom.setSchedulingBounds(infiniteBounds);
        zoom.setFactor(0.7);    // default 0.4
        BranchGroup zoomBG = new BranchGroup();
        zoomBG.addChild(zoom);
        viewingPlatform.addChild(zoomBG);
		zoomB = zoom;

        MouseRotate rotate = new MouseRotate(MouseRotate.INVERT_INPUT);
        rotate.setTransformGroup(viewingPlatform.getMultiTransformGroup().getTransformGroup(0));
        BranchGroup rotateBG = new BranchGroup();
        rotateBG.addChild(rotate);
        viewingPlatform.addChild(rotateBG);
        rotate.setSchedulingBounds(infiniteBounds);
		rotateB = rotate;

//		OrbitBehavior orbit = new OrbitBehavior(canvas, OrbitBehavior.REVERSE_ALL);
//		orbit.setSchedulingBounds(infiniteBounds);
//
//		/** step A **/
//		Point3d center = new Point3d(0, 0, 0);
//
//		//if (!User.is3DPerspective()) center = new Point3d(cell.getBounds().getCenterX(),cell.getBounds().getCenterY(), -10);
//
//		orbit.setRotationCenter(center);
//		orbit.setMinRadius(0);
//		orbit.setZoomFactor(10);
//		orbit.setTransFactors(10, 10);
//        orbit.setProportionalZoom(true);

//    	viewingPlatform.setViewPlatformBehavior(orbit);

		// This will move the ViewPlatform back a bit so the
        // objects in the scene can be viewed.
        //viewingPlatform.setNominalViewingTransform();


		BoundingSphere sceneBnd = (BoundingSphere)scene.getBounds();
		double radius = sceneBnd.getRadius();
		View view = u.getViewer().getView();

		// Too expensive at this point
        if (canvas.getSceneAntialiasingAvailable() && User.is3DAntialiasing())
		    view.setSceneAntialiasingEnable(true);

		// Setting the projection policy
		view.setProjectionPolicy(User.is3DPerspective()? View.PERSPECTIVE_PROJECTION : View.PARALLEL_PROJECTION);
		if (!User.is3DPerspective()) view.setCompatibilityModeEnable(true);

		Point3d c1 = new Point3d();
		sceneBnd.getCenter(c1);
		Vector3d vCenter = new Vector3d(c1);
		double vDist = 1.4 * radius / Math.tan(view.getFieldOfView()/2.0);
        Point3d c2 = new Point3d();

        sceneBnd.getCenter(c2);
		c2.z += vDist;

		//if (User.is3DPerspective())
		vCenter.z += vDist;
		Transform3D vTrans = new Transform3D();
		Transform3D proj = new Transform3D();
        Transform3D lookAt = new Transform3D();

		//lookAt.lookAt(c2, c1, new Vector3d(1, 1, 1));
		//lookAt.invert();

		proj.ortho(cell.getBounds().getMinX(), cell.getBounds().getMaxX(),
		        cell.getBounds().getMinY(), cell.getBounds().getMaxY(), (vDist+radius)/200.0, (vDist+radius)*2.0);

		vTrans.set(vCenter);

		view.setBackClipDistance((vDist+radius)*2.0);
		view.setFrontClipDistance((vDist+radius)/200.0);
		view.setBackClipPolicy(View.VIRTUAL_EYE);
		view.setFrontClipPolicy(View.VIRTUAL_EYE);
		if (User.is3DPerspective())
		{
			viewingPlatform.getViewPlatformTransform().setTransform(vTrans);
		}
		else
		{
			view.setVpcToEc(proj);
			//viewingPlatform.getViewPlatformTransform().setTransform(lookAt);
		}
		setWindowTitle();
	}

	private BranchGroup createSceneGraph(Cell cell, BoundingSphere infiniteBounds)
	{
		// Create the root of the branch graph
		BranchGroup objRoot = new BranchGroup();
		objRoot.setCapability(BranchGroup.ALLOW_BOUNDS_READ);
        objRoot.setCapability(BranchGroup.ENABLE_PICK_REPORTING);
        objRoot.setCapability(BranchGroup.ALLOW_BOUNDS_WRITE);

		// Create the TransformGroup node and initialize it to the
		// identity. Enable the TRANSFORM_WRITE capability so that
		// our behavior code can modify it at run time. Add it to
		// the root of the subgraph.
		objTrans = new TransformGroup();
		objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		objTrans.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		objRoot.addChild(objTrans);

		// Create a simple Shape3D node; add it to the scene graph.
		Background bg = new Background(new Color3f(new Color(User.getColorBackground())));
		bg.setApplicationBounds(infiniteBounds);
		objRoot.addChild(bg);

		//addCell(cell);
		View3DEnumerator view3D = new View3DEnumerator();
		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, null, view3D);
//		// Drawing nodes
//		for(Iterator nodes = cell.getNodes(); nodes.hasNext(); )
//		{
//			addNode((NodeInst)nodes.next(), objTrans);
//		}
//
//		// Drawing arcs
//		for(Iterator arcs = cell.getArcs(); arcs.hasNext(); )
//		{
//			addArc((ArcInst)arcs.next(), objTrans);
//		}

		// Lights
        Color3f alColor = new Color3f(0.6f, 0.6f, 0.6f);
        AmbientLight aLgt = new AmbientLight(alColor);
		Vector3f lightDir1 = new Vector3f(-1.0f, -1.0f, -1.0f);
		DirectionalLight light1 = new DirectionalLight(white, lightDir1);

		// Setting the influencing bounds
		light1.setInfluencingBounds(infiniteBounds);
	    aLgt.setInfluencingBounds(infiniteBounds);
		// Allow to turn off light while the scene graph is live
		light1.setCapability(Light.ALLOW_STATE_WRITE);
		// Add light to the env.
        objRoot.addChild(aLgt);
		objTrans.addChild(light1);

		// Picking tools
        //PickZoomBehavior behavior2 = new PickZoomBehavior(objRoot, canvas, infiniteBounds);
        //objRoot.addChild(behavior2);

		// Have Java 3D perform optimizations on this scene graph.
	    objRoot.compile();

		pickCanvas = new PickCanvas(canvas, objRoot);
		//pickCanvas.setMode(PickCanvas.BOUNDS);
        pickCanvas.setMode(PickCanvas.GEOMETRY_INTERSECT_INFO);
        pickCanvas.setTolerance(4.0f);
		return objRoot;
	}

	/**
	 * Method to set the window title.
	 */
	public void setWindowTitle()
	{
		if (wf == null) return;
		wf.setTitle(wf.composeTitle(cell, "3D View: ", 0));
	}

	/**
	 * Method to return the cell that is shown in this window.
	 * @return the cell that is shown in this window.
	 */
	public Cell getCell() { return cell; }

	/**
	 * Method to get rid of this EditWindow.  Called by WindowFrame when
	 * that windowFrame gets closed.
	 */
	public void finished()
	{
		// remove myself from listener list
		removeKeyListener(this);
		removeMouseListener(this);
		removeMouseMotionListener(this);
		removeMouseWheelListener(this);
	}

	public void bottomScrollChanged(int e) {}
	public void rightScrollChanged(int e) {}

	/** Dummy functios due to text-oriented functions */
	public void fullRepaint() { System.out.println("View3DWindow::fullRepaint"); }
	public boolean findNextText(boolean reverse) { return false; }
	public void replaceText(String replace) {}
	public JPanel getPanel() { return this; }
	public void initTextSearch(String search, boolean caseSensitive, boolean regExp, Set whatToSearch) {}


	public void zoomOutContents() {zoomB.zoomInOut(false);}
	public void zoomInContents() {zoomB.zoomInOut(true);}

	/**
	 * Method to reset zoom/rotation/translation to original place (Fill Window operation)
	 */
	public void fillScreen()
	{
		Transform3D trans = new Transform3D();

	    Transform3D x = new Transform3D();
		zoomB.getTransformGroup().getTransform(x);
		zoomB.getTransformGroup().setTransform(trans);
		rotateB.getTransformGroup().getTransform(x);
		rotateB.getTransformGroup().setTransform(trans);
		translateB.getTransformGroup().getTransform(x);
		translateB.getTransformGroup().setTransform(trans);
	}

	public void setCell(Cell cell, VarContext context) {}
	public void focusOnHighlighted() {}
	public void cellHistoryGoBack() {}
	public void cellHistoryGoForward() {}
	public boolean cellHistoryCanGoBack() { return false; }
	public boolean cellHistoryCanGoForward() { return false; }
	public void fireCellHistoryStatus() {}
	public void replaceAllText(String replace) {}
    public Highlighter getHighlighter() { return highlighter; }

	/**
	 *
	 * @param rootNode
	 */
	public void loadExplorerTree(DefaultMutableTreeNode rootNode)
	{
		wf.libraryExplorerNode = ExplorerTree.makeLibraryTree();
		wf.jobExplorerNode = Job.getExplorerTree();
		wf.errorExplorerNode = ErrorLogger.getExplorerTree();
		wf.signalExplorerNode = null;
		rootNode.add(wf.libraryExplorerNode);
		rootNode.add(wf.jobExplorerNode);
		rootNode.add(wf.errorExplorerNode);
	}

	/**
	 * Adds given Arc to scene graph
	 * @param ai
	 * @param objTrans
	 */
	public void addArc(ArcInst ai, AffineTransform transform, TransformGroup objTrans)
	{
		// add the arc
		ArcProto ap = ai.getProto();
		Technology tech = ap.getTechnology();

		List list = addPolys(tech.getShapeOfArc(ai), transform, objTrans);
		electricObjectMap.put(ai, list);
	}

	/**
	 * Adds given Node to scene graph
	 * @param no
	 * @param objTrans
	 */
	public void addNode(NodeInst no, AffineTransform transform, TransformGroup objTrans)
	{
		// add the node
		NodeProto nProto = no.getProto();
		Technology tech = nProto.getTechnology();

		// Skipping Special nodes
        if (NodeInst.isSpecialNode(no)) return;

		List list = null;
		if (!(nProto instanceof PrimitiveNode))
		{
			// Cell
			Cell cell = (Cell)nProto;
			Rectangle2D rect = no.getBounds();
			double [] values = new double[2];
			values[0] = Double.MAX_VALUE;
			values[1] = Double.MIN_VALUE;
			cell.getZValues(values);
			values[0] *= scale;
			values[1] *= scale;
			Poly pol = new Poly(rect);			list = new ArrayList(1);

			pol.transform(transform);
			rect = pol.getBounds2D();
			list.add(addPolyhedron(rect, values[0], values[1] - values[0], cellApp, objTrans));
		}
		else
        {
            Poly[] polys = tech.getShapeOfNode(no, null, true, true);
            List boxList = null;

            // Special case for transistors
            if (nProto.getFunction().isTransistor())
            {
                int[] active = new int[2];
                int gate = -1;
                int count = 0;
                int poly = -1;
                boxList = new ArrayList(4);

                // Merge active regions
                for (int i = 0; i < polys.length; i++)
                {
                    Layer.Function fun = polys[i].getLayer().getFunction();
                    if (fun.isDiff())
                    {
                        active[count++] = i;
                    }
                    else if (fun.isGatePoly())
                    {
                        gate = i;
                    }
                    else if (fun.isPoly())
                        poly = i;
                }
                if (count == 2)
                {
                    Rectangle2D rect1 = polys[active[0]].getBounds2D();
                    Rectangle2D rect2 = polys[active[1]].getBounds2D();
                    double minX = Math.min(rect1.getMinX(), rect2.getMinX());
                    double minY = Math.min(rect1.getMinY(), rect2.getMinY());
                    double maxX = Math.max(rect1.getMaxX(), rect2.getMaxX());
                    double maxY = Math.max(rect1.getMaxY(), rect2.getMaxY());
                    Rectangle2D newRect = new Rectangle2D.Double(minX, minY, (maxX-minX), (maxY-minY));
                    Poly tmp = new Poly(newRect);
                    tmp.setLayer(polys[active[0]].getLayer());
                    polys[active[0]] = tmp; // new active with whole area beneath poly gate
                    int last = polys.length - 1;
                    if (active[1] != last)
                        polys[active[1]] = polys[last];
                    polys[last] = null;
                }
                if (gate != -1 && poly != -1)
                {
                    Rectangle2D rect1 = polys[gate].getBounds2D();
	                boolean alongX = !(rect1.getX() == polys[poly].getBounds2D().getX());

	                Poly gateP = new Poly(rect1);
	                gateP.transform(transform);
	                rect1 = gateP.getBounds2D();
                    Point3d [] pts = new Point3d[8];
	                double max, delta;

	                if (alongX)
	                {
						max = rect1.getMaxX();
						delta = rect1.getWidth()/10;
	                }
	                else
	                {
		                max = rect1.getMaxY();
						delta = rect1.getHeight()/10;
	                }
                    double cutGate = (max - delta);
                    double cutPoly = (max + delta);
                    Layer layer = polys[gate].getLayer();
                    double dist = (layer.getDistance() + layer.getThickness()) * scale;
                    //double distPoly = (polys[poly].getLayer().getDistance() + (polys[poly].getLayer().getThickness()/10)) * scale;
                    double distPoly = (polys[poly].getLayer().getDistance()) * scale;
	                if (alongX)
	                {
						pts[0] = new Point3d(cutGate, rect1.getMinY(), dist);
						pts[1] = new Point3d(max, rect1.getMinY(), dist);
						pts[2] = new Point3d(cutPoly, rect1.getMinY(), distPoly);
						pts[3] = new Point3d(max, rect1.getMinY(), distPoly);
						pts[4] = new Point3d(cutGate, rect1.getMaxY(), dist);
						pts[5] = new Point3d(max, rect1.getMaxY(), dist);
						pts[6] = new Point3d(cutPoly, rect1.getMaxY(), distPoly);
						pts[7] = new Point3d(max, rect1.getMaxY(), distPoly);
	                }
	                else
	                {
						pts[0] = new Point3d(rect1.getMaxX(), cutGate, dist);
		                pts[1] = new Point3d(rect1.getMinX(), cutGate, dist);
						pts[2] = new Point3d(rect1.getMinX(), max, dist);
						pts[3] = new Point3d(rect1.getMaxX(), max, dist);
						pts[4] = new Point3d(rect1.getMaxX(), max, distPoly);
						pts[5] = new Point3d(rect1.getMinX(), max, distPoly);
						pts[6] = new Point3d(rect1.getMinX(), cutPoly, distPoly);
						pts[7] = new Point3d(rect1.getMaxX(), cutPoly, distPoly);
	                }
                    // First connection
                    boxList.add(addShape3D(pts, 4, getAppearance(layer)));
                    max = (alongX) ? rect1.getMinX() : rect1.getMinY();
                    cutGate = (max + delta);
                    cutPoly = (max - delta);
	                if (alongX)
	                {
						pts[0] = new Point3d(max, rect1.getMinY(), dist);
						pts[1] = new Point3d(cutGate, rect1.getMinY(), dist);
						pts[2] = new Point3d(max, rect1.getMinY(), distPoly);
						pts[3] = new Point3d(cutPoly, rect1.getMinY(), distPoly);
						pts[4] = new Point3d(max, rect1.getMaxY(), dist);
						pts[5] = new Point3d(cutGate, rect1.getMaxY(), dist);
						pts[6] = new Point3d(max, rect1.getMaxY(), distPoly);
						pts[7] = new Point3d(cutPoly, rect1.getMaxY(), distPoly);
	                }
	                else
	                {
						pts[0] = new Point3d(rect1.getMaxX(), max, dist);
						pts[1] = new Point3d(rect1.getMinX(), max, dist);
						pts[2] = new Point3d(rect1.getMinX(), cutGate, dist);
						pts[3] = new Point3d(rect1.getMaxX(), cutGate, dist);
						pts[4] = new Point3d(rect1.getMaxX(), cutPoly, distPoly);
						pts[5] = new Point3d(rect1.getMinX(), cutPoly, distPoly);
						pts[6] = new Point3d(rect1.getMinX(), max, distPoly);
						pts[7] = new Point3d(rect1.getMaxX(), max, distPoly);
	                }
	                // Second connection
                    boxList.add(addShape3D(pts, 4, getAppearance(layer)));
                }
            }
			list = addPolys(polys, transform /*no.rotateOut()*/, objTrans);
            if (boxList != null) list.addAll(boxList);
        }
		electricObjectMap.put(no, list);
	}

    private Shape3D addShape3D(Point3d[] pts, int listLen, Appearance ap)
    {

        int numFaces = listLen + 2; // contour + top + bottom
        int[] indices = new int[listLen*6];
        int[] stripCounts = new int[numFaces];
        int[] contourCount = new int[numFaces];
        Arrays.fill(contourCount, 1);
        Arrays.fill(stripCounts, 4);
        stripCounts[0] = listLen; // top
        stripCounts[numFaces-1] = listLen; // bottom

        int count = 0;
        // Top face
        for (int i = 0; i < listLen; i++)
            indices[count++] = i;
        // Contour
        for (int i = 0; i < listLen; i++)
        {
            indices[count++] = i;
            indices[count++] = i + listLen;
            indices[count++] = (i+1)%listLen + listLen;
            indices[count++] = (i+1)%listLen;
        }
        // Bottom face
        for (int i = 0; i < listLen; i++)
            indices[count++] = (listLen-i)%listLen + listLen;

        GeometryInfo gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
        gi.setCoordinates(pts);
        gi.setCoordinateIndices(indices);
        gi.setStripCounts(stripCounts);
        gi.setContourCounts(contourCount);
        NormalGenerator ng = new NormalGenerator();
        ng.setCreaseAngle ((float) Math.toRadians(30));
        ng.generateNormals(gi);
        GeometryArray c = gi.getGeometryArray();
        c.setCapability(GeometryArray.ALLOW_INTERSECT);

        Shape3D box = new Shape3D(c, ap);
        box.setCapability(Shape3D.ENABLE_PICK_REPORTING);
        box.setCapability(Node.ALLOW_LOCAL_TO_VWORLD_READ);
        box.setCapability(Shape3D.ALLOW_PICKABLE_READ);
        box.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        box.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
        box.setCapability(Shape3D.ALLOW_BOUNDS_READ);
        PickTool.setCapabilities(box, PickTool.INTERSECT_FULL);
        objTrans.addChild(box);
        return (box);
    }

	/**
	 * Method to add a polyhedron to the transformation group
	 * @param objTrans
	 */
	private Shape3D addPolyhedron(Rectangle2D bounds, double distance, double thickness,
	                          Appearance ap, TransformGroup objTrans)
	{
        GeometryInfo gi = new GeometryInfo(GeometryInfo.QUAD_ARRAY);
        double height = thickness + distance;
        Point3d[] pts = new Point3d[8];
        pts[0] = new Point3d(bounds.getMinX(), bounds.getMinY(), distance);
        pts[1] = new Point3d(bounds.getMinX(), bounds.getMaxY(), distance);
        pts[2] = new Point3d(bounds.getMaxX(), bounds.getMaxY(), distance);
        pts[3] = new Point3d(bounds.getMaxX(), bounds.getMinY(), distance);
        pts[4] = new Point3d(bounds.getMinX(), bounds.getMinY(), height);
        pts[5] = new Point3d(bounds.getMinX(), bounds.getMaxY(), height);
        pts[6] = new Point3d(bounds.getMaxX(), bounds.getMaxY(), height);
        pts[7] = new Point3d(bounds.getMaxX(), bounds.getMinY(), height);
        int[] indices = {0, 1, 2, 3, /* bottom z */
                         0, 4, 5, 1, /* back y */
                         0, 3, 7, 4, /* back x */
                         1, 5, 6, 2, /* front x */
                         2, 6, 7, 3, /* front y */
                         4, 7, 6, 5}; /* top z */
        gi.setCoordinates(pts);
        gi.setCoordinateIndices(indices);
        NormalGenerator ng = new NormalGenerator();
        ng.generateNormals(gi);
        GeometryArray c = gi.getGeometryArray();
        c.setCapability(GeometryArray.ALLOW_INTERSECT);

        Shape3D box = new Shape3D(c, ap);
        box.setCapability(Shape3D.ENABLE_PICK_REPORTING);
        box.setCapability(Node.ALLOW_LOCAL_TO_VWORLD_READ);
        box.setCapability(Shape3D.ALLOW_PICKABLE_READ);
		box.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
		box.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
		box.setCapability(Shape3D.ALLOW_BOUNDS_READ);
        PickTool.setCapabilities(box, PickTool.INTERSECT_FULL);
        objTrans.addChild(box);

		return(box);
	}

    private JAppearance getAppearance(Layer layer)
    {
        // Setting appearance
        EGraphics graphics = layer.getGraphics();
        JAppearance ap = (JAppearance)graphics.get3DAppearance();

        if (ap == null)
        {
            ap = new JAppearance(graphics);
            Color color = layer.getGraphics().getColor();
            Color3f objColor = new Color3f(color);
            /*
            ColoringAttributes ca = new ColoringAttributes(objColor, ColoringAttributes.SHADE_GOURAUD);
            ca.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
            ap.setColoringAttributes(ca);
            */

            /*
            TransparencyAttributes ta = new TransparencyAttributes();
            ta.setTransparencyMode(TransparencyAttributes.BLENDED);
            ta.setTransparency(0.5f);
            //ap.setTransparencyAttributes(ta);
            */

            // Adding Rendering attributes to access visibility flag
            RenderingAttributes ra = new RenderingAttributes();
            ra.setCapability(RenderingAttributes.ALLOW_VISIBLE_READ);
            ra.setCapability(RenderingAttributes.ALLOW_VISIBLE_WRITE);
            ra.setVisible(layer.isVisible());
            ap.setRenderingAttributes(ra);

            // Set up the polygon attributes
            //PolygonAttributes pa = new PolygonAttributes();
            //pa.setCullFace(PolygonAttributes.CULL_NONE);
            //pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
            //ap.setPolygonAttributes(pa);

            //TextureAttributes texAttr = new TextureAttributes();
            //texAttr.setTextureMode(TextureAttributes.MODULATE);
            //texAttr.setTextureColorTable(pattern);
            //ap.setTextureAttributes(texAttr);

            //LineAttributes lineAttr = new LineAttributes();
            //lineAttr.setLineAntialiasingEnable(true);
            //ap.setLineAttributes(lineAttr);

            // Adding to internal material
//				Material mat = new Material(objColor, black, objColor, white, 70.0f);
            Material mat = new Material();
            mat.setDiffuseColor(objColor);
            mat.setSpecularColor(objColor);
            mat.setAmbientColor(objColor);
            mat.setLightingEnable(true);
            mat.setCapability(Material.ALLOW_COMPONENT_READ);
            mat.setCapability(Material.ALLOW_COMPONENT_WRITE);
            ap.setMaterial(mat);

            // For changing color
            //ap.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
            //ap.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
            // For highlight
            ap.setCapability(Appearance.ALLOW_MATERIAL_READ);
            ap.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
            // For visibility
            ap.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_READ);
            ap.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_WRITE);

            graphics.set3DAppearance(ap);
        }
        return (ap);
    }

    /**
     *
     * @param pIt
     * @param distance
     * @param thickness
     * @param ap
     * @param objTrans
     * @return
     */
	private Shape3D addPolyhedron(PathIterator pIt, double distance, double thickness,
	                          Appearance ap, TransformGroup objTrans)
	{
        double height = thickness + distance;
        double [] coords = new double[6];
		List topList = new ArrayList();
		List bottomList = new ArrayList();
        List shapes = new ArrayList();

		while (!pIt.isDone())
		{
			int type = pIt.currentSegment(coords);
			if (type == PathIterator.SEG_CLOSE)
			{
				int listLen = topList.size();
				Point3d [] pts = new Point3d[listLen*2];
				System.arraycopy(topList.toArray(), 0, pts, 0, listLen);
				System.arraycopy(bottomList.toArray(), 0, pts, listLen, listLen);
				int numFaces = listLen + 2; // contour + top + bottom
				int[] indices = new int[listLen*6];
				int[] stripCounts = new int[numFaces];
                int[] contourCount = new int[numFaces];
				Arrays.fill(contourCount, 1);
				Arrays.fill(stripCounts, 4);
				stripCounts[0] = listLen; // top
				stripCounts[numFaces-1] = listLen; // bottom

				int count = 0;
				// Top face
				for (int i = 0; i < listLen; i++)
					indices[count++] = i;
				// Contour
				for (int i = 0; i < listLen; i++)
				{
					indices[count++] = i;
					indices[count++] = i + listLen;
					indices[count++] = (i+1)%listLen + listLen;
					indices[count++] = (i+1)%listLen;
				}
				// Bottom face
				for (int i = 0; i < listLen; i++)
					indices[count++] = (listLen-i)%listLen + listLen;

				GeometryInfo gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
				gi.setCoordinates(pts);
				gi.setCoordinateIndices(indices);
				gi.setStripCounts(stripCounts);
				gi.setContourCounts(contourCount);
				NormalGenerator ng = new NormalGenerator();
				ng.setCreaseAngle ((float) Math.toRadians(30));
				ng.generateNormals(gi);
				GeometryArray c = gi.getGeometryArray();
				c.setCapability(GeometryArray.ALLOW_INTERSECT);

				Shape3D box = new Shape3D(c, ap);
				box.setCapability(Shape3D.ENABLE_PICK_REPORTING);
				box.setCapability(Node.ALLOW_LOCAL_TO_VWORLD_READ);
				box.setCapability(Shape3D.ALLOW_PICKABLE_READ);
				box.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
				box.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
				box.setCapability(Shape3D.ALLOW_BOUNDS_READ);
				PickTool.setCapabilities(box, PickTool.INTERSECT_FULL);
				objTrans.addChild(box);
				shapes.add(box);

				topList.clear();
				bottomList.clear();
			} else if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO)
			{
				Point3d pt = new Point3d(coords[0], coords[1], distance);
				topList.add(pt);
				pt = new Point3d(coords[0], coords[1], height);
				bottomList.add(pt);
			}
			pIt.next();
		}

		if (shapes.size()>1) System.out.println("Error: case not handled");
		return((Shape3D)shapes.get(0));
	}

	/**
	 * Adds given list of Polys representing a PrimitiveNode to the transformation group
	 * @param polys
	 * @param transform
	 * @param objTrans
	 */
	private List addPolys(Poly [] polys, AffineTransform transform, TransformGroup objTrans)
	{
		if (polys == null) return (null);

		List list = new ArrayList();
		for(int i = 0; i < polys.length; i++)
		{
			Poly poly = polys[i];
            if (poly == null) continue; // Case for transistors and active regions.
			Layer layer = poly.getLayer();

			if (!layer.isVisible()) continue; // Doesn't generate the graph

			double thickness = layer.getThickness() * scale;
			double distance = layer.getDistance() * scale;

			if (thickness == 0) continue; // Skip zero-thickness layers

			if (transform != null)
				poly.transform(transform);

			// Setting appearance
            JAppearance ap = getAppearance(layer);

			if (poly.getBox() == null) // non-manhattan shape
			{
				list.add(addPolyhedron(poly.getPathIterator(null), distance, thickness, ap, objTrans));
			}
			else
			{
				Rectangle2D bounds = poly.getBounds2D();
				list.add(addPolyhedron(bounds, distance, thickness, ap, objTrans));
			}
		}
		return (list);
	}

	/**
	 * Method to set visibility in Appearance objects from external tools
	 * @param obj Appearance object
	 * @param visible true if visibility is on
	 */
	public static void set3DVisibility(Object obj, Boolean visible)
	{
		JAppearance app = (JAppearance)obj;
		app.getRenderingAttributes().setVisible(visible.booleanValue());
	}

	/**
	 * Method to set color in Appearance objects from external tools
	 * @param obj Appearance object
	 * @param color new color to setup
	 */
	public static void set3DColor(Object obj, java.awt.Color color)
	{
		JAppearance app = (JAppearance)obj;
		Color3f color3D = new Color3f(color);
		Material mat = app.getMaterial();
		mat.setAmbientColor(color3D);
		mat.setDiffuseColor(color3D);
	}

	/**
	 * Method to connect 2D and 3D highlights.
	 * @param view2D
	 */
	public static void show3DHighlight(WindowContent view2D)
	{
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof View3DWindow)) continue;
			View3DWindow wnd = (View3DWindow)content;

			// Undo previous highlight
			wnd.selectObject(false, false);

			if (wnd.view2D == view2D)
			{
				Highlighter highlighter2D = view2D.getHighlighter();
                List geomList = highlighter2D.getHighlightedEObjs(true, true);

				for (Iterator hIt = geomList.iterator(); hIt.hasNext(); )
				{
					ElectricObject eobj = (ElectricObject)hIt.next();

					List list = (List)wnd.electricObjectMap.get(eobj);

					if (list == null || list.size() == 0) continue;

					for (Iterator lIt = list.iterator(); lIt.hasNext();)
					{
						Shape3D shape = (Shape3D)lIt.next();
						wnd.highlighter.addObject(shape, Highlight.Type.SHAPE3D, wnd.cell);
					}
				}
				wnd.selectObject(true, false);
				return; // done
			}
		}
	}

    /**
     * Method to change Z values in elements
     * @param value
     */
    public static void setScaleFactor(Double value)
    {
	    Transform3D vTrans = new Transform3D();
	    Vector3d vCenter = new Vector3d(1, 1, value.doubleValue());

       	for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof View3DWindow)) continue;
			View3DWindow wnd = (View3DWindow)content;
            wnd.objTrans.getTransform(vTrans);
            vTrans.setScale(vCenter);
            wnd.objTrans.setTransform(vTrans);
		}
    }

	/**
	 * Method to turn on/off antialiasing
	 * @param value true if antialiasing is set to true
	 */
	public static void setAntialiasing(Boolean value)
	{
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof View3DWindow)) continue;
			View3DWindow wnd = (View3DWindow)content;
			View view = wnd.u.getViewer().getView();
			view.setSceneAntialiasingEnable(value.booleanValue());
			//wnd.print();
		}
	}

	private void print()
    {
        // Create the off-screen Canvas3D object
	    OffScreenCanvas3D offScreenCanvas3D = new OffScreenCanvas3D(SimpleUniverse.getPreferredConfiguration(), true);
        // Set the off-screen size based on a scale factor times the
	    // on-screen size
	    Screen3D sOn = canvas.getScreen3D();
	    Screen3D sOff = offScreenCanvas3D.getScreen3D();
	    Dimension dim = sOn.getSize();
	    dim.width *= ImagePrinter.OFF_SCREEN_SCALE;
	    dim.height *= ImagePrinter.OFF_SCREEN_SCALE;
	    sOff.setSize(dim);
	    sOff.setPhysicalScreenWidth(sOn.getPhysicalScreenWidth() * ImagePrinter.OFF_SCREEN_SCALE);
	    sOff.setPhysicalScreenHeight(sOn.getPhysicalScreenHeight() * ImagePrinter.OFF_SCREEN_SCALE);

	    // attach the offscreen canvas to the view
	    u.getViewer().getView().addCanvas3D(offScreenCanvas3D);
        BufferedImage bImage = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
        ImageComponent2D buffer = new ImageComponent2D(ImageComponent.FORMAT_RGBA, bImage);

	    offScreenCanvas3D.setOffScreenBuffer(buffer);
	    offScreenCanvas3D.renderOffScreenBuffer();
	    offScreenCanvas3D.waitForOffScreenRendering();
	    bImage = offScreenCanvas3D.getOffScreenBuffer().getImage();
        new ImagePrinter(bImage).print();
    }

	/**
	 * Method to print window using offscreen canvas
	 * @param ep Image observer plus printable object
	 * @return Printable.NO_SUCH_PAGE or Printable.PAGE_EXISTS
	 */
	public int getOffScreenImage(ElectricPrinter ep)
    {
		BufferedImage bImage = (BufferedImage)ep.getImage();

		// might have problems if visibility of some layers is switched off
		if (bImage == null)
		{
			// Create the off-screen Canvas3D object
			OffScreenCanvas3D offScreenCanvas3D = new OffScreenCanvas3D(SimpleUniverse.getPreferredConfiguration(), true);
			// Set the off-screen size based on a scale factor times the
			// on-screen size
			Screen3D sOn = canvas.getScreen3D();
			Screen3D sOff = offScreenCanvas3D.getScreen3D();
			Dimension dim = sOn.getSize();
			dim.width *= ImagePrinter.OFF_SCREEN_SCALE;
			dim.height *= ImagePrinter.OFF_SCREEN_SCALE;
			sOff.setSize(dim);
			sOff.setPhysicalScreenWidth(sOn.getPhysicalScreenWidth() * ImagePrinter.OFF_SCREEN_SCALE);
			sOff.setPhysicalScreenHeight(sOn.getPhysicalScreenHeight() * ImagePrinter.OFF_SCREEN_SCALE);

			// attach the offscreen canvas to the view
			u.getViewer().getView().addCanvas3D(offScreenCanvas3D);
			bImage = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
			ImageComponent2D buffer = new ImageComponent2D(ImageComponent.FORMAT_RGBA, bImage);

			offScreenCanvas3D.setOffScreenBuffer(buffer);
			offScreenCanvas3D.renderOffScreenBuffer();
			offScreenCanvas3D.waitForOffScreenRendering();
			bImage = offScreenCanvas3D.getOffScreenBuffer().getImage();
			ep.setImage(bImage);
		}
		Graphics2D g2d = (Graphics2D)ep.getGraphics();
		AffineTransform t2d = new AffineTransform();
		t2d.translate(ep.getPageFormat().getImageableX(), ep.getPageFormat().getImageableY());
		double xscale  = ep.getPageFormat().getImageableWidth() / (double)bImage.getWidth();
		double yscale  = ep.getPageFormat().getImageableHeight() / (double)bImage.getHeight();
		double scale = Math.min(xscale, yscale);
		t2d.scale(scale, scale);

		try {
			ImageObserver obj = (ImageObserver)ep;
			g2d.drawImage(bImage, t2d, obj);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return Printable.NO_SUCH_PAGE;
		}
        return Printable.PAGE_EXISTS;
    }

	// ************************************* EVENT LISTENERS *************************************

	//private int lastXPosition, lastYPosition;

	/**
	 * Respond to an action performed, in this case change the current cell
	 * when the user clicks on an entry in the upHierarchy popup menu.
	 */
	public void actionPerformed(ActionEvent e)
	{
		JMenuItem source = (JMenuItem)e.getSource();
		// extract library and cell from string
		Cell cell = (Cell)Cell.findNodeProto(source.getText());
		if (cell == null) return;
		setCell(cell, VarContext.globalContext);
	}

	// the MouseListener events
	public void mousePressed(MouseEvent evt)
	{
		//lastXPosition = evt.getX();   lastYPosition = evt.getY();

		/*
		View3DWindow wnd = (View3DWindow)evt.getSource();
		WindowFrame.setCurrentWindowFrame(wnd.wf);

		WindowFrame.curMouseListener.mousePressed(evt);
		*/
	}

	public void mouseReleased(MouseEvent evt)
	{
		//lastXPosition = evt.getX();   lastYPosition = evt.getY();
		//WindowFrame.curMouseListener.mouseReleased(evt);
	}

	/**
	 * Internal method to hightlight objects
	 * @param toSelect true if element must be highlighted
	 * @param do2D true if 2D highlighter should be called
	 */
	private void selectObject(boolean toSelect, boolean do2D)
	{
		Highlighter highlighter2D = null;
		// Clean previous selection
		if (view2D != null && do2D)
		{
			highlighter2D = view2D.getHighlighter();
			highlighter2D.clear();
		}
		for (Iterator it = highlighter.getHighlights().iterator(); it.hasNext();)
		{
			Highlight h = (Highlight)it.next();
			Shape3D obj = (Shape3D)h.getObject();
			if (toSelect) // highlight cell, set transparency
			{
				JAppearance app = (JAppearance)obj.getAppearance();
				obj.setAppearance(highligtAp);
				//app.getRenderingAttributes().setVisible(false);
				highligtAp.seGraphics(app.getGraphics());
				if (view2D != null && do2D)
				{
					//Geometry geo = obj.getGeometry();
					BoundingBox bb = (BoundingBox)obj.getBounds();
					Point3d lowerP = new Point3d(), upperP = new Point3d();
					bb.getUpper(upperP);
					bb.getLower(lowerP);
					double[] lowerValues = new double[3];
					double[] upperValues = new double[3];
					lowerP.get(lowerValues);
					upperP.get(upperValues);
					Rectangle2D area = new Rectangle2D.Double(lowerValues[0], lowerValues[1],
							(upperValues[0]-lowerValues[0]), (upperValues[1]-lowerValues[1]));
					highlighter2D.addArea(area, cell);
				}
			}
			else // back to normal
			{
				EGraphics graphics = highligtAp.getGraphics();
				if (graphics != null)
				{
					JAppearance origAp = (JAppearance)graphics.get3DAppearance();
					obj.setAppearance(origAp);
				}
				else // its a cell
					obj.setAppearance(cellApp);
			}
		}
		if (!toSelect) highlighter.clear();
		if (do2D) view2D.fullRepaint();
	}

	public void mouseClicked(MouseEvent evt)
	{
		//lastXPosition = evt.getX();   lastYPosition = evt.getY();
		//WindowFrame.curMouseListener.mouseClicked(evt);
		pickCanvas.setShapeLocation(evt);
        Point3d eyePos = pickCanvas.getStartPosition();
		Transform3D t = new Transform3D();
		Transform3D t1 = new Transform3D();
		canvas.getImagePlateToVworld(t);
		canvas.getVworldToImagePlate(t1);

		PickResult result = pickCanvas.pickClosest();
        PickResult[] results = pickCanvas.pickAllSorted();

		// Clean previous selection
		selectObject(false, true);

		if (result != null)
		{
		   Primitive p = (Primitive)result.getNode(PickResult.PRIMITIVE);
		   Shape3D s = (Shape3D)result.getNode(PickResult.SHAPE3D);
			
			if (s != null)
			{
				//selectedObject = s;
				highlighter.addObject(s, Highlight.Type.SHAPE3D, cell);
				selectObject(true, true);
			}
           //PickIntersection pi = result.getClosestIntersection(eyePos);

			/*
            PickIntersection pi =
		    results[0].getClosestIntersection(eyePos);

		   if (pi != null) {
			   Point3d point = pi.getPointCoordinates();
			   Point3d point1 = pi.getPointCoordinatesVW();
			   //point1.sub(eyePos);

			   Point3d p11 = new Point3d();
			   Point3d pp = new Point3d();
			   point = point1;
			   t1.transform(point1, p11);
			   t.transform(point, pp);
               Transform3D vTrans = new Transform3D();
			   //point1.negate();
			   Vector3d vCenter = new Vector3d(point1);
			   vTrans.set(vCenter);
			   //u.getViewingPlatform().getViewPlatformTransform().setTransform(vTrans);
			   Point3d position = new Point3d();

			   canvas.getCenterEyeInImagePlate(position);

			   Transform3D trans = new Transform3D();
			   //objTrans.getTransform(trans);
			   //trans.transform(vCenter);
               //trans.setTranslation (vCenter);
			   //objTrans.setTransform(trans);
               //u.getViewingPlatform().getViewPlatformTransform().setTransform(vTrans);
               orbit.setRotationCenter(point1);
		   } else{
			  System.out.println("null");
		   }
		   */
		}
        WindowFrame.curMouseListener.mouseClicked(evt);
	}

	public void mouseEntered(MouseEvent evt)
	{
		//lastXPosition = evt.getX();   lastYPosition = evt.getY();
		//showCoordinates(evt);
		//WindowFrame.curMouseListener.mouseEntered(evt);
	}

	public void mouseExited(MouseEvent evt)
	{
		//lastXPosition = evt.getX();   lastYPosition = evt.getY();
		//WindowFrame.curMouseListener.mouseExited(evt);
	}

	// the MouseMotionListener events
	public void mouseMoved(MouseEvent evt)
	{
		//lastXPosition = evt.getX();   lastYPosition = evt.getY();
		//showCoordinates(evt);
		//WindowFrame.curMouseMotionListener.mouseMoved(evt);
	}

	public void mouseDragged(MouseEvent evt)
	{
		//lastXPosition = evt.getX();   lastYPosition = evt.getY();
		//showCoordinates(evt);
		//WindowFrame.curMouseMotionListener.mouseDragged(evt);
	}

	private void showCoordinates(MouseEvent evt)
	{
		View3DWindow wnd = (View3DWindow)evt.getSource();

		if (wnd.getCell() == null) StatusBar.setCoordinates(null, wnd.wf); else
		{
			Point2D pt = wnd.screenToDatabase(evt.getX(), evt.getY());
			EditWindow.gridAlign(pt);

			StatusBar.setCoordinates("(" + TextUtils.formatDouble(pt.getX(), 2) + ", " + TextUtils.formatDouble(pt.getY(), 2) + ")", wnd.wf);
		}
	}

	// the MouseWheelListener events
	public void mouseWheelMoved(MouseWheelEvent evt) { WindowFrame.curMouseWheelListener.mouseWheelMoved(evt); }

	// the KeyListener events
	public void keyPressed(KeyEvent evt) {
		System.out.println("Here keyPressed");WindowFrame.curKeyListener.keyPressed(evt); }

	public void keyReleased(KeyEvent evt) {
		System.out.println("Here keyReleased");WindowFrame.curKeyListener.keyReleased(evt); }

	public void keyTyped(KeyEvent evt) {
		System.out.println("Here keyTyped");WindowFrame.curKeyListener.keyTyped(evt); }

    public void highlightChanged(Highlighter which) {
        repaint();
    }

    public void highlighterLostFocus(Highlighter highlighterGainedFocus) {}

	public Point getLastMousePosition()
	{
		//return new Point(lastXPosition, lastYPosition);
		return new Point(0,0);
	}

	// ************************************* COORDINATES *************************************

	/**
	 * Method to convert a screen coordinate to database coordinates.
	 * @param screenX the X coordinate (on the screen in this EditWindow).
	 * @param screenY the Y coordinate (on the screen in this EditWindow).
	 * @return the coordinate of that point in database units.
	 */
	public Point2D screenToDatabase(int screenX, int screenY)
	{
		double dbX = 0, dbY = 0;
		/*
		= (screenX - sz.width/2) / scale + offx;
		double dbY = (sz.height/2 - screenY) / scale + offy;

		*/
		return new Point2D.Double(dbX, dbY);
	}

	//************************ SPECIAL BEHAVIORS *********************************************/
	/**
	 * Extending original zoom class to allow zoom with respect to scene center
	 */
	public class JMouseZoom extends MouseZoom
	{
		Vector3d translation = new Vector3d();

		public JMouseZoom(Component c, int flags) {super(c, flags);}

		void zoomInOut(boolean out)
		{
			// Remember old matrix
			transformGroup.getTransform(currXform);

			Matrix4d mat = new Matrix4d();
			currXform.get(mat);
			double factor = (out) ? 0.5 : 2;
			double dy = currXform.getScale() * factor;

			// Translate to origin
			currXform.setTranslation(new Vector3d(0.0,0.0,0.0));

			translation.z = dy*getFactor();

			//transformX.set(translation);
			transformX.setScale(dy);

			if (invert) {
				currXform.mul(currXform, transformX);
			} else {
				currXform.mul(transformX, currXform);
			}

			// Set old translation back
			Vector3d translation = new Vector3d(mat.m03, mat.m13, mat.m23);
			currXform.setTranslation(translation);

			transformGroup.setTransform(currXform);

			transformChanged( currXform );

		}

		/**
		 * Similar to original MouseZoom function but zoom with respect to
		 * center
		 * @param evt
		 */
		void doProcess(MouseEvent evt)
		{
			int id;
			int dx, dy;

			processMouseEvent(evt);

			if (((buttonPress)&&((flags & MANUAL_WAKEUP) == 0)) ||
			    ((wakeUp)&&((flags & MANUAL_WAKEUP) != 0)))
			{
			    id = evt.getID();
			    if ((id == MouseEvent.MOUSE_DRAGGED) &&
				evt.isAltDown() && !evt.isMetaDown())
			    {
					x = evt.getX();
					y = evt.getY();

					//dx = x - x_last;
					dy = y - y_last;

					if (!reset){
						transformGroup.getTransform(currXform);

						// Remember old matrix
						Matrix4d mat = new Matrix4d();
						currXform.get(mat);

						// Translate to origin
						currXform.setTranslation(new Vector3d(0.0,0.0,0.0));

						translation.z  = dy*getFactor();

						transformX.set(translation);

						if (invert) {
							currXform.mul(currXform, transformX);
						} else {
							currXform.mul(transformX, currXform);
						}

						// Set old translation back
						Vector3d translation = new Vector3d(mat.m03, mat.m13, mat.m23);
						currXform.setTranslation(translation);

						transformGroup.setTransform(currXform);

						transformChanged( currXform );

						/*
						if (callback!=null)
						callback.transformChanged( MouseBehaviorCallback.ZOOM,
									   currXform );
									   */

					}
					else {
						reset = false;
					}

					x_last = x;
					y_last = y;
			    }
			    else if (id == MouseEvent.MOUSE_PRESSED) {
					x_last = evt.getX();
					y_last = evt.getY();
			    }
			}
		}
	}

	//*** To navigate cells
		// Extra functions to check area
	private class View3DEnumerator extends HierarchyEnumerator.Visitor
    {
		public View3DEnumerator()
		{
		}

		/**
		 *
		 * @param info
		 * @return
		 */
		public boolean enterCell(HierarchyEnumerator.CellInfo info)
		{
            AffineTransform rTrans = info.getTransformToRoot();

			for(Iterator it = info.getCell().getArcs(); it.hasNext(); )
			{
				addArc((ArcInst)it.next(), rTrans, objTrans);
			}
			return true;
		}

		/**
		 *
		 * @param info
		 */
		public void exitCell(HierarchyEnumerator.CellInfo info) {}

		/**
		 *
		 * @param no
		 * @param info
		 * @return
		 */
		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
		{
			Cell cell = info.getCell();
			NodeInst ni = no.getNodeInst();
			AffineTransform trans = ni.rotateOut();
			NodeProto np = ni.getProto();
			AffineTransform root = info.getTransformToRoot();
			if (root.getType() != AffineTransform.TYPE_IDENTITY)
				trans.preConcatenate(root);

			addNode(ni, trans, objTrans);

			// For cells, it should go into the hierarchy
            return ni.isExpanded();
		}
    }

    //*** To print 3D canvas
    private class ImagePrinter implements Printable, ImageObserver {
        BufferedImage bImage;
        private static final int OFF_SCREEN_SCALE = 3;

        public int print(Graphics g, PageFormat pf, int pi) throws PrinterException
        {
            if (pi >= 1) {
                return Printable.NO_SUCH_PAGE;
            }

            Graphics2D g2d = (Graphics2D)g;
            AffineTransform t2d = new AffineTransform();
            t2d.translate(pf.getImageableX(), pf.getImageableY());
            double xscale  = pf.getImageableWidth() / (double)bImage.getWidth();
            double yscale  = pf.getImageableHeight() / (double)bImage.getHeight();
            double scale = Math.min(xscale, yscale);
            t2d.scale(scale, scale);
            try {
                g2d.drawImage(bImage,t2d, this);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                return Printable.NO_SUCH_PAGE;
            }
            return Printable.PAGE_EXISTS;
        }

        void print() {
            PrinterJob printJob = PrinterJob.getPrinterJob();
            PageFormat pageFormat = printJob.defaultPage();
            pageFormat.setOrientation(PageFormat.LANDSCAPE);
            pageFormat = printJob.validatePage(pageFormat);
            printJob.setPrintable(this, pageFormat);
            if (printJob.printDialog()) {
                try {
                    printJob.print();
                }
                catch (PrinterException ex) {
                    ex.printStackTrace();
                }
            }
        }

        public boolean imageUpdate(Image img,
                       int infoflags,
                       int x,
                       int y,
                       int width,
                       int height) {
            return false;
        }

        ImagePrinter(BufferedImage bImage) {
            this.bImage = bImage;
        }
    }

    private class OffScreenCanvas3D extends Canvas3D {
        OffScreenCanvas3D(GraphicsConfiguration graphicsConfiguration,
                  boolean offScreen) {
            super(graphicsConfiguration, offScreen);
        }

        BufferedImage doRender(int width, int height)
        {
			BufferedImage bImage =
				new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

			ImageComponent2D buffer =
				new ImageComponent2D(ImageComponent.FORMAT_RGBA, bImage);

			setOffScreenBuffer(buffer);
			renderOffScreenBuffer();
			waitForOffScreenRendering();
			bImage = getOffScreenBuffer().getImage();

			return bImage;
        }

        public void postSwap() {
        // No-op since we always wait for off-screen rendering to complete
        }
    }
}
