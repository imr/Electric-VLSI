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
import com.sun.electric.database.variable.Variable;
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
import com.sun.electric.tool.user.ui.*;
import com.sun.electric.plugins.j3d.utils.J3DUtils;
import com.sun.electric.plugins.j3d.utils.J3DAlpha;
import com.sun.electric.plugins.j3d.utils.J3DAppearance;

import java.awt.BorderLayout;
import java.awt.Color;
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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.*;

import com.sun.j3d.utils.behaviors.mouse.*;
import com.sun.j3d.utils.behaviors.interpolators.KBKeyFrame;
import com.sun.j3d.utils.behaviors.interpolators.KBRotPosScaleSplinePathInterpolator;
import com.sun.j3d.utils.behaviors.interpolators.RotPosScaleTCBSplinePathInterpolator;
import com.sun.j3d.utils.behaviors.interpolators.TCBKeyFrame;
import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.picking.PickIntersection;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.Viewer;
import com.sun.j3d.utils.universe.ViewingPlatform;

import javax.media.j3d.*;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.vecmath.*;

/**
 * This class deals with 3D View using Java3D
 * @author  Gilda Garreton
 * @version 0.1
 */
public class View3DWindow extends JPanel
        implements WindowContent, MouseMotionListener, MouseListener, MouseWheelListener, KeyListener, ActionListener,
        HighlightListener, J3DCollisionDetector
{

	/** # of nodes to consider scene graph big */       private static final int MAX3DVIEWNODES = 5000;
    /** bounding of scene graph */                      public static final BoundingSphere infiniteBounds = new BoundingSphere(new Point3d(), Double.MAX_VALUE);

	private SimpleUniverse u;
	private Canvas3D canvas;
	protected TransformGroup objTrans;
	private JMouseRotate rotateB;
	private JMouseZoom zoomB;
	private JMouseTranslate translateB;
	private J3DUtils.OffScreenCanvas3D offScreenCanvas3D;

    // For demo cases
    //KBRotPosScaleSplinePathInterpolator kbSplineInter;
    //RotPosScaleTCBSplinePathInterpolator tcbSplineInter;
    private Map interpolatorMap = new HashMap();

    private J3DAlpha jAlpha = new J3DAlpha(new Alpha (-1,
        Alpha.INCREASING_ENABLE | Alpha.DECREASING_ENABLE,
        0,
        0,
        2500, // 25000
        400,  // 4000
        100,
        2000,  //20000
        5000,
        50 ), true, 0.5f);

	/** the window frame containing this editwindow */      private WindowFrame wf;
	/** reference to 2D view of the cell */                 private WindowContent view2D;
	/** the cell that is in the window */					protected Cell cell;
    /** scale3D factor in Z axis */                         private double scale3D = User.get3DFactor();
	/** Highlighter for this window */                      private Highlighter highlighter;
	private PickCanvas pickCanvas;
	/** Lis with all Shape3D drawn per ElectricObject */    private HashMap electricObjectMap = new HashMap();
    private boolean oneTransformPerNode = false;
    /** Map with object transformation for individual moves */ private HashMap transformGroupMap = new HashMap();

    public static WindowContent create3DWindow(Cell cell, WindowFrame wf, WindowContent view2D, Boolean transPerNode)
    {
        int number = cell.getNumNodes() + cell.getNumArcs();
        if (number < MAX3DVIEWNODES)
        {
            for (int i = 0; i < cell.getNumNodes(); i++)
            {
                NodeInst node = cell.getNode(i);
                if (node.getProto() instanceof Cell && node.isExpanded())
                {
                    Cell np = (Cell)node.getProto();
                    number += (np.getNumArcs() + np.getNumNodes());
                }
            }
        }

        if (number >= MAX3DVIEWNODES)
        {
            int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
                "The graph scene contains " + number + " nodes, are you sure you want to open 3D view of " + cell.describe() + "?",
                    "Warning", JOptionPane.OK_CANCEL_OPTION);
            if (response == JOptionPane.CANCEL_OPTION) return null;
        }

//        View3DWindow window = (transPerNode.booleanValue()) ?
//                new J3DDemoView(cell, wf, view2D, true) : new View3DWindow(cell, wf, view2D, false);

        View3DWindow window = new View3DWindow(cell, wf, view2D, transPerNode.booleanValue());
        return (window);
    }

    // constructor
	View3DWindow(Cell cell, WindowFrame wf, WindowContent view2D, boolean transPerNode)
	{
		this.cell = cell;
        this.wf = wf;
		this.view2D = view2D;
        this.oneTransformPerNode = transPerNode;

		highlighter = new Highlighter(Highlighter.SELECT_HIGHLIGHTER, wf);
        highlighter.addHighlightListener(this);

		setLayout(new BorderLayout());
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();

		canvas = new Canvas3D(config);
		add("Center", canvas);
		canvas.addMouseListener(this);

        // Set global appearances before create the elements
        J3DAppearance.setCellAppearanceValues(this);
        J3DAppearance.setHighlightedAppearanceValues(this);


		// Create a simple scene and attach it to the virtual universe
		BranchGroup scene = createSceneGraph(cell, infiniteBounds);

		// Have Java 3D perform optimizations on this scene graph.
	    scene.compile();

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

        JMouseRotate rotate = new JMouseRotate(MouseRotate.INVERT_INPUT);
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
        Rectangle2D bnd = cell.getBounds();

        //translateB.setView(bnd.getWidth(), 0);
        rotateB.setRotation(User.get3DRotX(), User.get3DRotY());
        zoomB.setZoom(User.get3DOrigZoom());
		proj.ortho(bnd.getMinX(), bnd.getMinX(), bnd.getMinY(), bnd.getMaxY(), (vDist+radius)/200.0, (vDist+radius)*2.0);

		vTrans.set(vCenter);

		view.setBackClipDistance((vDist+radius)*200.0);
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

    protected BranchGroup createSceneGraph(Cell cell, BoundingSphere infiniteBounds)
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
        objTrans.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        objTrans.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        objTrans.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
		objRoot.addChild(objTrans);

		// Create a simple Shape3D node; add it to the scene graph.
		Background bg = new Background(new Color3f(new Color(User.getColorBackground())));
		bg.setApplicationBounds(infiniteBounds);
		objRoot.addChild(bg);

		View3DEnumerator view3D = new View3DEnumerator();
		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, null, view3D);

        // lights
        J3DUtils.createLights(objRoot, objTrans);

		// Lights
//        Color3f alColor = new Color3f(0.6f, 0.6f, 0.6f);
//        AmbientLight aLgt = new AmbientLight(alColor); //J3DUtils.white);
//        AmbientLight ambLight = new AmbientLight( true, new Color3f( 1.0f, 1.0f, 1.0f ) );
//		Vector3f lightDir1 = new Vector3f(-1.0f, -1.0f, -1.0f);
//        Vector3f lightDir2 = new Vector3f(1.0f, 1.0f, 1.0f);
//		DirectionalLight light1 = new DirectionalLight(J3DUtils.white, lightDir1);
//        DirectionalLight light2 = new DirectionalLight(J3DUtils.white, lightDir2);
//        DirectionalLight headLight = new DirectionalLight( );
//
//		// Setting the influencing bounds
//		light1.setInfluencingBounds(infiniteBounds);
//        light2.setInfluencingBounds(infiniteBounds);
//        ambLight.setInfluencingBounds(infiniteBounds);
//	    aLgt.setInfluencingBounds(infiniteBounds);
//        headLight.setInfluencingBounds(infiniteBounds);
//		// Allow to turn off light while the scene graph is live
//		light1.setCapability(Light.ALLOW_STATE_WRITE);
//		// Add light to the env.
//        objRoot.addChild(aLgt);
//		objTrans.addChild(light1);
//        //objTrans.addChild(light2);

		// Picking tools
        //PickZoomBehavior behavior2 = new PickZoomBehavior(objRoot, canvas, infiniteBounds);
        //objRoot.addChild(behavior2);
		pickCanvas = new PickCanvas(canvas, objRoot);
		//pickCanvas.setMode(PickCanvas.BOUNDS);
        pickCanvas.setMode(PickCanvas.GEOMETRY_INTERSECT_INFO);
        pickCanvas.setTolerance(4.0f);

        setInterpolator(infiniteBounds);

        // Key
//        TransformGroup keyB = new TransformGroup( );
//		keyB.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
//		keyB.setCapability( TransformGroup.ALLOW_TRANSFORM_READ );

		// attach a navigation behavior to the position of the viewer
//		KeyNavigatorBehavior key = new KeyNavigatorBehavior(objTrans);
//		key.setSchedulingBounds(infiniteBounds);
//		key.setEnable( true );
//		objRoot.addChild( key );

        // create the KeyBehavior and attach
		J3DKeyCollision keyBehavior = new J3DKeyCollision(objTrans, this);
		keyBehavior.setSchedulingBounds(infiniteBounds);
		//keyBehavior.setMovementRate( 0.7 );
		objTrans.addChild(keyBehavior);

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
		double panningAmount = panningAmounts[User.getPanningDistance()];

		//double value = (direction == 0) ? dim.width : dim.height;
		int mult = (int)((double)10 * panningAmount);
		if (mult == 0) mult = 1;

		if (direction == 0)
			translateB.panning(mult*ticks, 0);
		else
		    translateB.panning(0, mult*ticks);
	}

    public void setViewAndZoom(double x, double y, double zoom)
    {
        translateB.setView(x, y);
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
	private void addNode(NodeInst no, AffineTransform transform, TransformGroup objTrans)
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
			values[0] *= scale3D;
			values[1] *= scale3D;
			Poly pol = new Poly(rect);			list = new ArrayList(1);

			pol.transform(transform);
			rect = pol.getBounds2D();
			list.add(J3DUtils.addPolyhedron(rect, values[0], values[1] - values[0], J3DAppearance.cellApp, objTrans));
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
				double dist = (layer.getDistance() + layer.getThickness()) * scale3D;
				double distPoly = (polys[poly].getLayer().getDistance()) * scale3D;
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
                J3DUtils.correctNormals(topList, bottomList);
                System.arraycopy(topList.toArray(), 0, pts, 0, 4);
                System.arraycopy(bottomList.toArray(), 0, pts, 4, 4);
                boxList.add(J3DUtils.addShape3D(pts, 4, J3DAppearance.getAppearance(layer.getGraphics()), objTrans));

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
                J3DUtils.correctNormals(topList, bottomList);
                System.arraycopy(topList.toArray(), 0, pts, 0, 4);
                System.arraycopy(bottomList.toArray(), 0, pts, 4, 4);
                boxList.add(J3DUtils.addShape3D(pts, 4, J3DAppearance.getAppearance(layer.getGraphics()), objTrans));
            }
            if (boxList != null) list.addAll(boxList);
        }
		electricObjectMap.put(no, list);
        for (int i = 0; i < list.size(); i++)
        {
            transformGroupMap.put(list.get(i), objTrans);
        }
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

            if (layer == null || layer.getTechnology() == null) continue; // Non-layout technology. E.g Artwork
			if (!layer.isVisible()) continue; // Doesn't generate the graph

			double thickness = layer.getThickness() * scale3D;
			double distance = layer.getDistance() * scale3D;

			if (thickness == 0) continue; // Skip zero-thickness layers

			if (transform != null)
				poly.transform(transform);

			// Setting appearance
            J3DAppearance ap = J3DAppearance.getAppearance(layer.getGraphics());

			if (poly.getBox() == null) // non-manhattan shape
			{
				list.add(J3DUtils.addPolyhedron(poly.getPathIterator(null), distance, thickness, ap, objTrans));
			}
			else
			{
				Rectangle2D bounds = poly.getBounds2D();
				list.add(J3DUtils.addPolyhedron(bounds, distance, thickness, ap, objTrans));
			}
		}
		return (list);
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
    public static void setScaleFactor(double value)
    {
	    Transform3D vTrans = new Transform3D();
	    Vector3d vCenter = new Vector3d(1, 1, value);

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
	public static void setAntialiasing(boolean value)
	{
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof View3DWindow)) continue;
			View3DWindow wnd = (View3DWindow)content;
			View view = wnd.u.getViewer().getView();
			view.setSceneAntialiasingEnable(value);
		}
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
				offScreenCanvas3D = new J3DUtils.OffScreenCanvas3D(SimpleUniverse.getPreferredConfiguration(), true);
				// attach the offscreen canvas to the view
				u.getViewer().getView().addCanvas3D(offScreenCanvas3D);
				// Set the off-screen size based on a scale3D factor times the
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
     * Method to rotate individual groups
     * @param values array of values
     */
    public J3DUtils.ThreeDDemoKnot moveAndRotate(double[] values)
    {
        Vector3d newPos = new Vector3d(values[0], values[1], values[2]);
        double factor = 10;
        Quat4f quaf = J3DUtils.createQuaternionFromEuler(factor*values[3], factor*values[4], factor*values[5]);
        Transform3D currXform = new Transform3D();

        for (Iterator it = cell.getNodes(); it.hasNext();)
        {
            NodeInst ni = (NodeInst)it.next();
            Variable var = (Variable)ni.getVar("3D_NODE_DEMO");
            if (var == null) continue;
            List list = (List)electricObjectMap.get(ni);
            for (int i = 0; i < list.size(); i++)
            {
                Shape3D obj = (Shape3D)list.get(i);
                TransformGroup grp = (TransformGroup)transformGroupMap.get(obj);

                grp.getTransform(currXform);
                currXform.setTranslation(newPos);
//                tmpTrans.set(newPos);
                boolean invert = true;
//                if (invert) {
//                    currXform.mul(currXform, tmpTrans);
//                } else {
//                    currXform.mul(tmpTrans, currXform);
//                }
                grp.setTransform(currXform);

                grp.getTransform(currXform);

                Matrix4d mat = new Matrix4d();
                // Remember old matrix
                currXform.get(mat);

                //tmpTrans.setEuler(rotation);

                // Translate to rotation point
                currXform.setTranslation(new Vector3d(values[6], values[7], values[8]));
                currXform.setRotation(quaf);
//                if (invert) {
//                currXform.mul(currXform, tmpTrans);
//                } else {
//                currXform.mul(tmpTrans, currXform);
//                }

                // Set old translation back
                Vector3d translation = new
                Vector3d(mat.m03, mat.m13, mat.m23);
                currXform.setTranslation(translation);

                // Update xform
                grp.setTransform(currXform);
            }
        }
        return(new J3DUtils.ThreeDDemoKnot(values[0], values[1], values[2], 1,
                        0, 0, 0, values[3], values[4], values[5]));
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
				J3DAppearance app = (J3DAppearance)obj.getAppearance();
				obj.setAppearance(J3DAppearance.highligtAp);
				//app.getRenderingAttributes().setVisible(false);
				J3DAppearance.highligtAp.setGraphics(app.getGraphics());
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
				EGraphics graphics = J3DAppearance.highligtAp.getGraphics();
				if (graphics != null)
				{
					J3DAppearance origAp = (J3DAppearance)graphics.get3DAppearance();
					obj.setAppearance(origAp);
				}
				else // its a cell
					obj.setAppearance(J3DAppearance.cellApp);
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
//		Transform3D t = new Transform3D();
//		Transform3D t1 = new Transform3D();
//		canvas.getImagePlateToVworld(t);
//		canvas.getVworldToImagePlate(t1);
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

	public void showCoordinates(MouseEvent evt)
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
		= (screenX - sz.width/2) / scale3D + offx;
		double dbY = (sz.height/2 - screenY) / scale3D + offy;

		*/
		return new Point2D.Double(dbX, dbY);
	}

    /*****************************************************************************
     *          To navigate in tree and create 3D objects                           *
     *****************************************************************************/
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

            TransformGroup grp = objTrans;
            if (oneTransformPerNode)
            {
                grp = new TransformGroup();
                grp.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		        grp.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
                grp.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
                grp.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
                grp.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
                objTrans.addChild(grp);
            }
			addNode(ni, trans, grp);

			// For cells, it should go into the hierarchy
            return ni.isExpanded();
		}
    }

    /*****************************************************************************
     *          Demo Stuff                                                       *
     *****************************************************************************/
    /**
     * Method to set view point of the camera and move to this point
     * by interpolator
     * @param mode 0 if KB spline, 1 if TCB spline
     */
    public void set3DCamera(int mode)
    {
//        if (mode == 0)
//        {
//            boolean state = kbSplineInter.getEnable();
//            kbSplineInter.setEnable(!state);
//        }
//        else
//        {
//            boolean state = tcbSplineInter.getEnable();
//            tcbSplineInter.setEnable(!state);
//        }
    }

    /**
     * Method to create spline interpolator for demo mode
     * @param infiniteBounds
     */
    private void setInterpolator(BoundingSphere infiniteBounds)
    {
        //BranchGroup behaviorBranch = new BranchGroup();
        Transform3D yAxis = new Transform3D();
        List polys = new ArrayList();

        double [] zValues = new double[2];
        cell.getZValues(zValues);
        double zCenter = (zValues[0] + zValues[1])/2;
        Rectangle2D bounding = cell.getBounds();
        Vector3d translation = new Vector3d (bounding.getCenterX(), bounding.getCenterY(), zCenter);
        yAxis.setTranslation(translation);

        for (Iterator it = cell.getNodes(); it.hasNext();)
        {
            NodeInst ni = (NodeInst)it.next();
            if (ni.getProto() == Artwork.tech.pinNode)
            {
                Rectangle2D rect = ni.getBounds();
                Variable var = (Variable)ni.getVar("3D_Z_VALUE");
                double zValue = (var == null) ? zCenter : TextUtils.atof(var.getObject().toString());
                var = (Variable)ni.getVar("3D_SCALE_VALUE");
                double scale = (var == null) ? 1 : TextUtils.atof(var.getObject().toString());
                var = (Variable)ni.getVar("3D_HEADING_VALUE");
                double heading = (var == null) ? 0 : TextUtils.atof(var.getObject().toString());
                var = (Variable)ni.getVar("3D_PITCH_VALUE");
                double pitch = (var == null) ? 0 : TextUtils.atof(var.getObject().toString());
                var = (Variable)ni.getVar("3D_BANK_VALUE");
                double bank = (var == null) ? 0 : TextUtils.atof(var.getObject().toString());
                var = (Variable)ni.getVar("3D_ROTX_VALUE");
                double rotX = (var == null) ? 0 : TextUtils.atof(var.getObject().toString());
                var = (Variable)ni.getVar("3D_ROTY_VALUE");
                double rotY = (var == null) ? 0 : TextUtils.atof(var.getObject().toString());
                var = (Variable)ni.getVar("3D_ROTZ_VALUE");
                double rotZ = (var == null) ? 0 : TextUtils.atof(var.getObject().toString());
                J3DUtils.ThreeDDemoKnot knot = new J3DUtils.ThreeDDemoKnot(rect.getCenterX(), rect.getCenterY(),
                        zValue, scale, heading, pitch, bank, rotX, rotY, rotZ);
                polys.add(knot);
            }
        }

        if (polys.size() == 0) return; // nothing to create

        KBKeyFrame[] splineKeyFrames = new KBKeyFrame[polys.size()];
        TCBKeyFrame[] keyFrames = new TCBKeyFrame[polys.size()];
        for (int i = 0; i < polys.size(); i++)
        {
            J3DUtils.ThreeDDemoKnot knot = (J3DUtils.ThreeDDemoKnot)polys.get(i);
            splineKeyFrames[i] = J3DUtils.getNextKBKeyFrame((float)((float)i/(polys.size()-1)), knot);
            keyFrames[i] = J3DUtils.getNextTCBKeyFrame((float)((float)i/(polys.size()-1)), knot);
        }

//        kbSplineInter = new KBRotPosScaleSplinePathInterpolator(jAlpha, objTrans,
//                                                  yAxis, splineKeyFrames);
//        kbSplineInter.setSchedulingBounds(infiniteBounds);
//        //behaviorBranch.addChild(kbSplineInter);
//        kbSplineInter.setEnable(false);

        Interpolator tcbSplineInter = new RotPosScaleTCBSplinePathInterpolator(jAlpha, objTrans,
                                                  yAxis, keyFrames);
        tcbSplineInter.setSchedulingBounds(infiniteBounds);
        //behaviorBranch.addChild(tcbSplineInter);
        tcbSplineInter.setEnable(false);
        interpolatorMap.put(objTrans, tcbSplineInter);

        //objTrans.addChild(behaviorBranch);
        //objTrans.addChild(kbSplineInter);
        objTrans.addChild(tcbSplineInter);
    }

    /**
     * Method to create a path interpolator using knots
     * defined in input list
     * @param knotList
     */
    public Map addInterpolator(List knotList)
    {
        if (knotList.size() < 2)
        {
            System.out.println("Needs at least 2 frams for the interpolator");
            return null;
        }

        Map interMap = new HashMap(1);
        for (Iterator it = cell.getNodes(); it.hasNext();)
        {
            NodeInst ni = (NodeInst)it.next();
            Variable var = (Variable)ni.getVar("3D_NODE_DEMO");
            if (var == null) continue;
            List list = (List)electricObjectMap.get(ni);
            for (int j = 0; j < list.size(); j++)
            {
                Shape3D obj = (Shape3D)list.get(j);
                TransformGroup grp = (TransformGroup)transformGroupMap.get(obj);
                BranchGroup behaviorBranch = new BranchGroup();
                behaviorBranch.setCapability(BranchGroup.ALLOW_DETACH); // to detach this branch from parent group
                TCBKeyFrame[] keyFrames = new TCBKeyFrame[knotList.size()];
                for (int i = 0; i < knotList.size(); i++)
                {
                    J3DUtils.ThreeDDemoKnot knot = (J3DUtils.ThreeDDemoKnot)knotList.get(i);
                    keyFrames[i] = J3DUtils.getNextTCBKeyFrame((float)((float)i/(knotList.size()-1)), knot);
                }
                Transform3D yAxis = new Transform3D();
                Interpolator tcbSplineInter = new RotPosScaleTCBSplinePathInterpolator(jAlpha, grp,
                                                          yAxis, keyFrames);
                tcbSplineInter.setSchedulingBounds(new BoundingSphere(new Point3d(), Double.MAX_VALUE));
                behaviorBranch.addChild(tcbSplineInter);
                interMap.put(grp, behaviorBranch);
                grp.addChild(behaviorBranch);
                interpolatorMap.put(grp, tcbSplineInter);
            }
        }
        return interMap;
    }

    /**
     * Method to remove certain interpolators from scene graph
     * @param interMap
     */
    public void removeInterpolator(Map interMap)
    {
        for (Iterator it = interMap.keySet().iterator(); it.hasNext();)
        {
            TransformGroup grp = (TransformGroup)it.next();
            Node node = (Node)interMap.get(grp);
            grp.removeChild(node);
        }
    }

    /**
     * Method to retrieve alpha value associated to this view
     * @return
     */
    public J3DAlpha getAlpha() { return jAlpha; }

    ///////////////////// KEY BEHAVIOR FUNCTION ///////////////////////////////

    private Vector3d tmpTrans = new Vector3d();
    private Vector3d mapSize = null;

    protected double getScale( )
	{
		return 0.05;
	}

    Vector3d getMapSize( )
    {
        if (mapSize == null)
            mapSize = new Vector3d(2, 0, 2);
        return mapSize;
    }

    Point2d convertToMapCoordinate( Vector3d worldCoord )
	{
		Point2d point2d = new Point2d( );

		Vector3d squareSize = getMapSize();

		point2d.x = (worldCoord.x + getPanel().getWidth())/ squareSize.x;
		point2d.y = (worldCoord.z + getPanel().getHeight())/ squareSize.z;

		return point2d;
	}

    public boolean isCollision( Transform3D t3d, boolean bViewSide )
	{
		// get the translation
		t3d.get( tmpTrans );

		// we need to scale up by the scale that was
		// applied to the root TG on the view side of the scenegraph
		if( bViewSide != false )
			tmpTrans.scale( 1.0 / getScale( ) );

//        Vector3d mapSquareSize = getMapSize( );

		// first check that we are still inside the "world"
//		if (tmpTrans.x < -getPanel().getWidth() + mapSquareSize.x ||
//			tmpTrans.x > getPanel().getWidth() - mapSquareSize.x ||
//			tmpTrans.y < -getPanel().getHeight() + mapSquareSize.y ||
//			tmpTrans.y > getPanel().getHeight() - mapSquareSize.y  )
//			return true;

		if( bViewSide != false )
			// then do a pixel based look up using the map
			return isCollision(tmpTrans);

		return false;
	}

    // returns true if the given x,z location in the world corresponds to a wall section
	protected boolean isCollision( Vector3d worldCoord )
	{
		Point2d point = convertToMapCoordinate( worldCoord );
//		int nImageWidth = (int)cell.getBounds().getWidth();
//		int nImageHeight = (int)cell.getBounds().getHeight();

        int nImageWidth = getPanel().getWidth();
		int nImageHeight = getPanel().getHeight();

		// outside of image
//		if( point.x < 0 || point.x >= nImageWidth ||
//			point.y < 0 || point.y >= nImageHeight )
//			return true;

        pickCanvas.setShapeLocation((int)point.x, (int)point.y);
        PickResult result = pickCanvas.pickClosest();

        if (result != null && result.getNode(PickResult.SHAPE3D) != null)
        {
             Shape3D shape = (Shape3D)result.getNode(PickResult.SHAPE3D);
             shape.setAppearance(J3DAppearance.highligtAp);
            for (int i = 0; i < result.numIntersections(); i++)
            {
                PickIntersection inter = result.getIntersection(i);
            System.out.println("Collision " + inter.getDistance() + " " + inter.getPointCoordinates() + " normal " + inter.getPointNormal());
                 System.out.println("Point  " + point + " world " + worldCoord);
                GeometryArray geo = inter.getGeometryArray();

                if (inter.getDistance() < 6) return (true); // collision
            }
        }

        return (false);
//		int color = m_MapImage.getRGB( (int) point.x, (int) point.y );
//
//		// we can't walk through walls or bookcases
//		return( color == m_ColorWall || color == m_ColorBookcase );
	}
}
