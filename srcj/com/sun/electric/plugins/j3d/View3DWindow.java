/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: View3DWindow.java
 * Written by Gilda Garreton, Sun Microsystems.
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

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.ElectricPrinter;
import com.sun.electric.tool.user.ui.ExplorerTree;
import com.sun.electric.tool.user.ui.StatusBar;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.*;

import com.sun.j3d.utils.behaviors.mouse.MouseBehavior;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseZoom;
import com.sun.j3d.utils.behaviors.interpolators.KBKeyFrame;
import com.sun.j3d.utils.behaviors.interpolators.KBRotPosScaleSplinePathInterpolator;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.picking.PickTool;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.Viewer;
import com.sun.j3d.utils.universe.ViewingPlatform;

import javax.media.j3d.Alpha;
import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Light;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.PositionInterpolator;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Screen3D;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.View;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.vecmath.*;

/**
 * This class deals with 3D View using Java3D
 * @author  Gilda Garreton
 * @version 0.1
 */
public class View3DWindow extends JPanel
        implements WindowContent, MouseMotionListener, MouseListener, MouseWheelListener, KeyListener, ActionListener,
        HighlightListener
{
	private SimpleUniverse u;
	private Canvas3D canvas;
	private TransformGroup objTrans;
	private MouseBehavior rotateB;
	private JMouseZoom zoomB;
	private JMouseTranslate translateB;
	private OffScreenCanvas3D offScreenCanvas3D;
    private BranchGroup scene;

    Alpha alpha = new Alpha (1,Alpha.INCREASING_ENABLE,0,0,1000,0,0,0,0,0);
    PositionInterpolator inter;

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

		setLayout(new BorderLayout());
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        BoundingSphere infiniteBounds = new BoundingSphere(new Point3d(), Double.MAX_VALUE);

		canvas = new Canvas3D(config);
		add("Center", canvas);
		canvas.addMouseListener(this);

		// Create a simple scene and attach it to the virtual universe
		scene = createSceneGraph(cell, infiniteBounds);

		ViewingPlatform viewP = new ViewingPlatform(4);
		viewP.setCapability(ViewingPlatform.ALLOW_CHILDREN_READ);
		Viewer viewer = new Viewer(canvas);
		u = new SimpleUniverse(viewP, viewer);
		u.addBranchGraph(scene);

        // This will move the ViewPlatform back a bit so the
        // objects in the scene can be viewed.

		ViewingPlatform viewingPlatform = u.getViewingPlatform();

        JMouseTranslate translate = new JMouseTranslate(canvas, MouseTranslate.INVERT_INPUT);
        translate.setTransformGroup(viewingPlatform.getMultiTransformGroup().getTransformGroup(2));
        translate.setSchedulingBounds(infiniteBounds);
		double scale = (cell.getDefWidth() < cell.getDefHeight()) ? cell.getDefWidth() : cell.getDefHeight();
        translate.setFactor(0.01 * scale); // default 0.02
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
		pickCanvas = new PickCanvas(canvas, objRoot);
		//pickCanvas.setMode(PickCanvas.BOUNDS);
        pickCanvas.setMode(PickCanvas.GEOMETRY_INTERSECT_INFO);
        pickCanvas.setTolerance(4.0f);

        // Interpolation
        inter = new PositionInterpolator(alpha, objTrans, new Transform3D(), 0, 1);
        inter.setEnable(false);
        inter.setSchedulingBounds(infiniteBounds);
        objTrans.addChild(inter);

		// Have Java 3D perform optimizations on this scene graph.
	    objRoot.compile();
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

	/**
	 * Method to pan along X according to fixed amount of ticks
	 * @param direction
	 * @param panningAmounts
	 * @param ticks
	 */
	public void panXOrY(int direction, double[] panningAmounts, int ticks)
	{
		Cell cell = getCell();
		if (cell == null) return;
		Dimension dim = getSize();
		double panningAmount = panningAmounts[User.getPanningDistance()];

		//double value = (direction == 0) ? dim.width : dim.height;
		int mult = (int)((double)10 * panningAmount);
		if (mult == 0) mult = 1;

		if (direction == 0)
			translateB.panning(mult*ticks, 0);
		else
		    translateB.panning(0, mult*ticks);
	}

	/**
	 * Method to zoom out by a factor of 2 plus mouse pre-defined factor
	 */
	public void zoomOutContents() {zoomB.zoomInOut(false);}

	/**
	 * Method to zoom in by a factor of 2 plus mouse pre-defined factor
	 */
	public void zoomInContents() {zoomB.zoomInOut(true);}

	/**
	 * Method to reset zoom/rotation/extraTrans to original place (Fill Window operation)
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
		int gate = -1;
		int count = 0;
		int poly = -1;

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
            Poly[] polys = tech.getShapeOfNode(no, null, true, true, null);
            List boxList = null;

            // Special case for transistors
            if (nProto.getFunction().isTransistor())
            {
                int[] active = new int[2];
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
            }
			list = addPolys(polys, transform, objTrans);

			// Adding extra layers after polygons are rotated.
            if (nProto.getFunction().isTransistor() && gate != -1 && poly != -1)
            {
				Point3d [] pts = new Point3d[8];
	            Point2D[] points = polys[gate].getPoints();
                Point2D p0 = points[0];
                Point2D p1 = points[1];
                Point2D p2 = points[points.length-1];
	            double dist1 = p0.distance(p1);
	            double dist2 = p0.distance(p2);
				Layer layer = polys[gate].getLayer();
				double dist = (layer.getDistance() + layer.getThickness()) * scale;
				double distPoly = (polys[poly].getLayer().getDistance()) * scale;
                Point2D pointDist, pointClose;
                List topList = new ArrayList();
		        List bottomList = new ArrayList();
                int center, right;

	            if (dist1 > dist2)
	            {
	                pointDist = p1;
		            pointClose = p2;
                    center = 1;
                    right = 2;
	            }
	            else
	            {
	                pointDist = p2;
		            pointClose = p1;
                    center = 2;
                    right = points.length-1;
	            }
                Point2d pDelta = new Point2d(pointDist.getX()-points[0].getX(), pointDist.getY()-points[0].getY());
                pDelta.scale(0.1);
                double[] values = new double[2];
                pDelta.get(values);

                // First extra polyhedron
                topList.add(new Point3d(p0.getX()+values[0], p0.getY()+values[1], dist));
                topList.add(new Point3d(p0.getX(), p0.getY(), distPoly));
                topList.add(new Point3d(p0.getX()-values[0], p0.getY()-values[1], distPoly));
                topList.add(new Point3d(p0.getX(), p0.getY(), dist));
                bottomList.add(new Point3d(pointClose.getX()+values[0], pointClose.getY()+values[1], dist));
                bottomList.add(new Point3d(pointClose.getX(), pointClose.getY(), distPoly));
                bottomList.add(new Point3d(pointClose.getX()-values[0], pointClose.getY()-values[1], distPoly));
                bottomList.add(new Point3d(pointClose.getX(), pointClose.getY(), dist));
                correctNormals(topList, bottomList);
                System.arraycopy(topList.toArray(), 0, pts, 0, 4);
                System.arraycopy(bottomList.toArray(), 0, pts, 4, 4);
                boxList.add(addShape3D(pts, 4, getAppearance(layer)));

                // Second polyhedron
                topList.clear();
                bottomList.clear();
                topList.add(new Point3d(points[center].getX()-values[0], points[center].getY()-values[1], dist));
                topList.add(new Point3d(points[center].getX(), points[center].getY(), distPoly));
                topList.add(new Point3d(points[center].getX()+values[0], points[center].getY()+values[1], distPoly));
                topList.add(new Point3d(points[center].getX(), points[center].getY(), dist));
                bottomList.add(new Point3d(points[right].getX()-values[0], points[right].getY()-values[1], dist));
                bottomList.add(new Point3d(points[right].getX(), points[right].getY(), distPoly));
                bottomList.add(new Point3d(points[right].getX()+values[0], points[right].getY()+values[1], distPoly));
                bottomList.add(new Point3d(points[right].getX(), points[right].getY(), dist));
                correctNormals(topList, bottomList);
                System.arraycopy(topList.toArray(), 0, pts, 0, 4);
                System.arraycopy(bottomList.toArray(), 0, pts, 4, 4);
                boxList.add(addShape3D(pts, 4, getAppearance(layer)));
            }
            if (boxList != null) list.addAll(boxList);
        }
		electricObjectMap.put(no, list);
	}

    /**
     * Method to correct points sequence to obtain valid normals
     * @param topList
     * @param bottomList
     */
    private void correctNormals(List topList, List bottomList)
    {
        // Determining normal direction
        Point3d p0 = (Point3d)topList.get(0);
        Point3d p1 = new Point3d((Point3d)topList.get(1));
        p1.sub(p0);
        Point3d pn = new Point3d((Point3d)topList.get(topList.size()-1));
        pn.sub(p0);
        Vector3d aux = new Vector3d();
        aux.cross(new Vector3d(p1), new Vector3d(pn));
        // the other layer
        Point3d b0 = new Point3d((Point3d)bottomList.get(0));
        b0.sub(p0);
        // Now the dot product
        double dot = aux.dot(new Vector3d(b0));
        if (dot > 0)  // Invert sequence of points otherwise the normals will be wrong
        {
            Collections.reverse(topList);
            Collections.reverse(bottomList);
        }
    }

    /**
     * Simple method to generate polyhedra
     * @param pts
     * @param listLen
     * @param ap
     * @return
     */
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
                correctNormals(topList, bottomList);
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

            if (layer.getTechnology() == null) continue; // Non-layout technology. E.g Artwork
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
		}
	}

    /**
     * Method to set view point of the camera and move to this point
     * by interpolator
     * @param content
     * @param x
     * @param y
     * @param z
     */
    public static void set3DCameraOld(WindowContent content, Double x, Double y, Double z)
    {
        if (!(content instanceof View3DWindow)) return;
        View3DWindow wnd = (View3DWindow)content;
        Cell cell = wnd.cell;

        for (Iterator it = cell.getNodes(); it.hasNext();)
        {
            NodeInst ni = (NodeInst)it.next();
            if (ni.getProto() == Artwork.tech.pinNode)
            {
                Poly [] polyList = Artwork.tech.getShapeOfNode(ni);
                System.out.println("Art " + ni.getBounds() + " " +
                        polyList[0].getCenterX() + " " + polyList[0].getCenterY());

            }
        }
        // Just stopping the motion
        if (x == null)
        {
            wnd.inter.setEnable(false);
            return;
        }
//	    angle += Math.toRadians(10.0);
//	    trans.rotY(angle);
	    //objTrans.setTransform(t);
        Transform3D t = new Transform3D();
        //objTrans.getTransform(t);
        Point3d view = new Point3d(x.doubleValue(), y.doubleValue(), z.doubleValue());
        Vector3d up = new Vector3d(0,0,1);
        Point3d center = new Point3d(wnd.cell.getBounds().getCenterX(), wnd.cell.getBounds().getCenterY(), -10);
        t.lookAt(view, center, up);
        t.invert();
        wnd.inter.setEnable(false);
        wnd.inter.setTransformAxis(t);
        wnd.inter.setEnable(true);
        //wnd.inter.initialize();
    }

	/**
	 * Method to print window using offscreen canvas
	 * @param ep Image observer plus printable object
	 * @return Printable.NO_SUCH_PAGE or Printable.PAGE_EXISTS
	 */
	public BufferedImage getOffScreenImage(ElectricPrinter ep)
    {
		BufferedImage bImage = ep.getBufferedImage();
        int OFF_SCREEN_SCALE = 3;

		// might have problems if visibility of some layers is switched off
		if (bImage == null)
		{
			// Create the off-screen Canvas3D object
			if (offScreenCanvas3D == null)
			{
				offScreenCanvas3D = new OffScreenCanvas3D(SimpleUniverse.getPreferredConfiguration(), true);
				// attach the offscreen canvas to the view
				u.getViewer().getView().addCanvas3D(offScreenCanvas3D);
				// Set the off-screen size based on a scale factor times the
				// on-screen size
				Screen3D sOn = canvas.getScreen3D();
				Screen3D sOff = offScreenCanvas3D.getScreen3D();
				Dimension dim = sOn.getSize();
				dim.width *= OFF_SCREEN_SCALE;
				dim.height *= OFF_SCREEN_SCALE;
				sOff.setSize(dim);
				sOff.setPhysicalScreenWidth(sOn.getPhysicalScreenWidth() * OFF_SCREEN_SCALE);
				sOff.setPhysicalScreenHeight(sOn.getPhysicalScreenHeight() * OFF_SCREEN_SCALE);
				bImage = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
				ImageComponent2D buffer = new ImageComponent2D(ImageComponent.FORMAT_RGBA, bImage);

				offScreenCanvas3D.setOffScreenBuffer(buffer);
			}
			offScreenCanvas3D.renderOffScreenBuffer();
			offScreenCanvas3D.waitForOffScreenRendering();
			bImage = offScreenCanvas3D.getOffScreenBuffer().getImage();
			ep.setBufferedImage(bImage);
			//Need to remove offscreen after that
			//u.getViewer().getView().removeCanvas3D(offScreenCanvas3D);
		}
		Graphics2D g2d = (Graphics2D)ep.getGraphics();
		// In case of printing
		if (g2d != null)
		{
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
				return null;
			}
		}
        return bImage;
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
		Transform3D t = new Transform3D();
		Transform3D t1 = new Transform3D();
		canvas.getImagePlateToVworld(t);
		canvas.getVworldToImagePlate(t1);
		PickResult result = pickCanvas.pickClosest();

		// Clean previous selection
		selectObject(false, true);

		if (result != null)
		{
		   Shape3D s = (Shape3D)result.getNode(PickResult.SHAPE3D);
			
			if (s != null)
			{
				highlighter.addObject(s, Highlight.Type.SHAPE3D, cell);
				selectObject(true, true);
			}
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
	 * Extending original translate class to allow panning
	 */
    public class JMouseTranslate extends MouseTranslate
	{
		Vector3d extraTrans = new Vector3d();

		public JMouseTranslate(Component c, int flags) {super(c, flags);}

		void panning(int dx, int dy)
		{
			transformGroup.getTransform(currXform);

		    extraTrans.x = dx*getXFactor();
		    extraTrans.y = -dy*getYFactor();

		    transformX.set(extraTrans);

		    if (invert) {
			currXform.mul(currXform, transformX);
		    } else {
			currXform.mul(transformX, currXform);
		    }

		    transformGroup.setTransform(currXform);

		    transformChanged( currXform );
		}
	}

	/**
	 * Extending original zoom class to allow zoom not from original behavior
	 */
	public class JMouseZoom extends MouseZoom
	{
		//Vector3d extraTrans = new Vector3d();

		public JMouseZoom(Component c, int flags) {super(c, flags);}

		void zoomInOut(boolean out)
		{
			// Remember old matrix
			transformGroup.getTransform(currXform);

			Matrix4d mat = new Matrix4d();
			currXform.get(mat);
			double z_factor = Math.abs(getFactor());
			double factor = (out) ? (0.5/z_factor) : (2*z_factor);
			double factor1 = (out) ? (1/z_factor) : (z_factor);

			double dy = currXform.getScale() * factor1;
			currXform.setScale(dy);

			// Translate to origin
//			currXform.setTranslation(new Vector3d(0.0,0.0,0.0));
//
//			extraTrans.z = dy; //dy*getFactor();
//
//			//transformX.set(extraTrans);
//			transformX.setScale(dy);
//
//			if (invert) {
//				currXform.mul(currXform, transformX);
//			} else {
//				currXform.mul(transformX, currXform);
//			}
//
//			// Set old extraTrans back
//			Vector3d extraTrans = new Vector3d(mat.m03, mat.m13, mat.m23);
//			currXform.setTranslation(extraTrans);

			transformGroup.setTransform(currXform);

			transformChanged( currXform );

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
		 */
		public void exitCell(HierarchyEnumerator.CellInfo info) {}

		/**
		 */
		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
		{
			NodeInst ni = no.getNodeInst();
			AffineTransform trans = ni.rotateOut();
			AffineTransform root = info.getTransformToRoot();
			if (root.getType() != AffineTransform.TYPE_IDENTITY)
				trans.preConcatenate(root);

			addNode(ni, trans, objTrans);

			// For cells, it should go into the hierarchy
            return ni.isExpanded();
		}
    }

    /**
     * Class to create offscreen from canvas 3D
     */
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

    // Setting Camera functions

    public static void set3DCamera(WindowContent content, Double x, Double y, Double z)
    {
        KBKeyFrame[] splineKeyFrames = new KBKeyFrame[6];
       BranchGroup behaviorBranch = new BranchGroup();

        if (!(content instanceof View3DWindow)) return;
        View3DWindow wnd = (View3DWindow)content;
        Cell cell = wnd.cell;

       setupSplineKeyFrames (splineKeyFrames);
       Transform3D yAxis = new Transform3D();
       Interpolator splineInterpolator =
         new KBRotPosScaleSplinePathInterpolator(wnd.alpha, wnd.objTrans,
                                                  yAxis, splineKeyFrames);
       splineInterpolator.setSchedulingBounds((BoundingSphere)wnd.scene.getBounds());
        behaviorBranch.addChild(splineInterpolator);
       //wnd.objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE); 
       wnd.objTrans.addChild(behaviorBranch);
    }

    private static void setupSplineKeyFrames (KBKeyFrame[] splineKeyFrames) {

    Vector3f           pos0 = new Vector3f(-5.0f, -5.0f, 0.0f);
    Vector3f           pos1 = new Vector3f(-5.0f,  5.0f, 0.0f);
    Vector3f           pos2 = new Vector3f( 0.0f,  5.0f, 0.0f);
    Vector3f           pos3 = new Vector3f( 0.0f, -5.0f, 0.0f);
    Vector3f           pos4 = new Vector3f( 5.0f, -5.0f, 0.0f);
    Vector3f           pos5 = new Vector3f( 5.0f,  5.0f, 0.0f);
      // Prepare spline keyframe data
      Point3f p   = new Point3f (pos0);            // position
      float head  = (float)Math.PI/2.0f;           // heading
      float pitch = 0.0f;                          // pitch
      float bank  = 0.0f;                          // bank
      Point3f s   = new Point3f(1.0f, 1.0f, 1.0f); // uniform scale
      splineKeyFrames[0] =
         new KBKeyFrame(0.0f, 0, p, head, pitch, bank, s, 0.0f, 0.0f, 0.0f);

      p = new Point3f (pos1);
      head  = 0.0f;                               // heading
      pitch = 0.0f;                               // pitch
      bank  = (float)-Math.PI/2.0f;               // bank
      s = new Point3f(1.0f, 1.0f, 1.0f);          // uniform scale
      splineKeyFrames[1] =
         new KBKeyFrame(0.2f, 0, p, head, pitch, bank, s, 0.0f, 0.0f, 0.0f);

      p = new Point3f (pos2);
      head  = 0.0f;                               // heading
      pitch = 0.0f;                               // pitch
      bank  = 0.0f;                               // bank
      s = new Point3f(0.7f, 0.7f, 0.7f);          // uniform scale
      splineKeyFrames[2] =
         new KBKeyFrame(0.4f, 0, p, head, pitch, bank, s, 0.0f, 0.0f, 0.0f);

      p = new Point3f (pos3);
      head  = (float)Math.PI/2.0f;                // heading
      pitch = 0.0f;                               // pitch
      bank  = (float)Math.PI/2.0f;                // bank
      s = new Point3f(0.5f, 0.5f, 0.5f);          // uniform scale
      splineKeyFrames[3] =
         new KBKeyFrame(0.6f, 0, p, head, pitch, bank, s, 0.0f, 0.0f, 0.0f);

      p = new Point3f (pos4);
      head  = (float)-Math.PI/2.0f;               // heading
      pitch = (float)-Math.PI/2.0f;               // pitch
      bank  = (float)Math.PI/2.0f;                // bank
      s = new Point3f(0.4f, 0.4f, 0.4f);          // uniform scale
      splineKeyFrames[4] =
         new KBKeyFrame(0.8f, 0, p, head, pitch, bank, s, 0.0f, 0.0f, 0.0f);

      p = new Point3f (pos5);
      head  = 0.0f;                               // heading
      pitch = 0.0f;                               // pitch
      bank  = 0.0f;                               // bank
      s = new Point3f(1.0f, 1.0f, 1.0f);          // uniform scale
      splineKeyFrames[5] =
         new KBKeyFrame(1.0f, 0, p, head, pitch, bank, s, 0.0f, 0.0f, 0.0f);
    }
}
