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
import com.sun.electric.plugins.j3d.utils.*;

import java.awt.*;
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
import java.util.List;
import java.io.*;

import com.sun.j3d.utils.behaviors.interpolators.KBKeyFrame;
import com.sun.j3d.utils.behaviors.interpolators.RotPosScaleTCBSplinePathInterpolator;
import com.sun.j3d.utils.behaviors.interpolators.TCBKeyFrame;
import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.picking.PickIntersection;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewingPlatform;
import com.sun.j3d.utils.universe.PlatformGeometry;

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
        HighlightListener, J3DCollisionDetector, Observer
{

	/** # of nodes to consider scene graph big */       private static final int MAX3DVIEWNODES = 5000;

	private SimpleUniverse u;
	private J3DCanvas3D canvas;
	protected TransformGroup objTrans;
    private BranchGroup scene;
    private OrbitBehavior orbit;
////    //private BranchGroup axes;
//	private JMouseRotate rotateB;
//	private JMouseZoom zoomB;
//	private JMouseTranslate translateB;
	//private J3DUtils.OffScreenCanvas3D offScreenCanvas3D;

    // For demo cases
    //KBRotPosScaleSplinePathInterpolator kbSplineInter;
    //RotPosScaleTCBSplinePathInterpolator tcbSplineInter;
    private Map interpolatorMap = new HashMap();
    private J3DKeyCollision keyBehavior;

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
	/** reference to 2D view of the cell */                 private EditWindow view2D;
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
		this.view2D = (EditWindow)view2D;
        // Adding observer
        this.view2D.getWindowFrame().addObserver(this);
        this.oneTransformPerNode = transPerNode;

		highlighter = new Highlighter(Highlighter.SELECT_HIGHLIGHTER, wf);
        highlighter.addHighlightListener(this);

		setLayout(new BorderLayout());
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();

        //
        GraphicsConfigTemplate3D gc3D = new GraphicsConfigTemplate3D( );
		gc3D.setSceneAntialiasing( GraphicsConfigTemplate.PREFERRED );
		//GraphicsDevice gd[] = GraphicsEnvironment.getLocalGraphicsEnvironment( ).getScreenDevices( );
        //config = gd[0].getBestConfiguration( gc3D );

		canvas = new J3DCanvas3D(config); //Canvas3D(config);
		add("Center", canvas);
		canvas.addMouseListener(this);

        // Set global appearances before create the elements
        J3DAppearance.setCellAppearanceValues(this);
        J3DAppearance.setHighlightedAppearanceValues(this);
        J3DAppearance.setAxisAppearanceValues(this);

		// Create a simple scene and attach it to the virtual universe
		scene = createSceneGraph(cell);

		// Have Java 3D perform optimizations on this scene graph.
	    scene.compile();

		//ViewingPlatform viewingPlatform = new ViewingPlatform(4);
		//viewingPlatform.setCapability(ViewingPlatform.ALLOW_CHILDREN_READ);
		//Viewer viewer = new Viewer(canvas);
		u = new SimpleUniverse(canvas); // viewingPlatform, viewer);

        // lights on ViewPlatform geometry group
        PlatformGeometry pg = new PlatformGeometry();
        J3DUtils.createLights(pg);

        ViewingPlatform viewingPlatform = u.getViewingPlatform();

//        JMouseTranslate translate = new JMouseTranslate(canvas, MouseTranslate.INVERT_INPUT);
//        //translate.setTransformGroup(objTrans); //viewingPlatform.getMultiTransformGroup().getTransformGroup(2));
//        translate.setSchedulingBounds(J3DUtils.infiniteBounds);
//		double scale = (cell.getDefWidth() < cell.getDefHeight()) ? cell.getDefWidth() : cell.getDefHeight();
//        translate.setFactor(0.01 * scale); // default 0.02
//        BranchGroup translateBG = new BranchGroup();
//        translateBG.addChild(translate);
//        //viewingPlatform.addChild(translateBG);
//		translateB = translate;

//        JMouseZoom zoom = new JMouseZoom(canvas, MouseZoom.INVERT_INPUT);
//        //zoom.setTransformGroup(viewingPlatform.getMultiTransformGroup().getTransformGroup(1));
//        zoom.setSchedulingBounds(J3DUtils.infiniteBounds);
//        zoom.setFactor(0.7);    // default 0.4
//        BranchGroup zoomBG = new BranchGroup();
//        zoomBG.addChild(zoom);
//        //viewingPlatform.addChild(zoomBG);
//		zoomB = zoom;

//        JMouseRotate rotate = new JMouseRotate(MouseRotate.INVERT_INPUT);
//        //rotate.setTransformGroup(viewingPlatform.getMultiTransformGroup().getTransformGroup(0));
//        BranchGroup rotateBG = new BranchGroup();
//        rotateBG.addChild(rotate);
//        //viewingPlatform.addChild(rotateBG);
//        rotate.setSchedulingBounds(J3DUtils.infiniteBounds);
//		rotateB = rotate;

		// This will move the ViewPlatform back a bit so the
        // objects in the scene can be viewed.
        viewingPlatform.setNominalViewingTransform();

		orbit = new OrbitBehavior(canvas, OrbitBehavior.REVERSE_ALL);
		orbit.setSchedulingBounds(J3DUtils.infiniteBounds);
        orbit.setCapability(OrbitBehavior.ALLOW_LOCAL_TO_VWORLD_READ);

		/** Setting rotation center */
		Point3d center = new Point3d(0, 0, 0);
		BoundingSphere sceneBnd = (BoundingSphere)scene.getBounds();
        sceneBnd.getCenter(center);
		orbit.setRotationCenter(center);
		orbit.setMinRadius(0);
		//orbit.setZoomFactor(10);
		orbit.setTransFactors(10, 10);
        orbit.setProportionalZoom(true);

        //viewingPlatform.setNominalViewingTransform();
    	viewingPlatform.setViewPlatformBehavior(orbit);

		double radius = sceneBnd.getRadius();
		View view = u.getViewer().getView();

		// Too expensive at this point
        if (canvas.getSceneAntialiasingAvailable() && User.is3DAntialiasing())
		    view.setSceneAntialiasingEnable(true);

		// Setting the projection policy
		view.setProjectionPolicy(User.is3DPerspective()? View.PERSPECTIVE_PROJECTION : View.PARALLEL_PROJECTION);
		if (!User.is3DPerspective()) view.setCompatibilityModeEnable(true);

        // Setting transparency sorting
        view.setTransparencySortingPolicy(View.TRANSPARENCY_SORT_GEOMETRY);
        view.setDepthBufferFreezeTransparent(false); // set to true only for transparent layers

        // Setting a good viewpoint for the camera
		Point3d c1 = new Point3d();
		sceneBnd.getCenter(c1);
		Vector3d vCenter = new Vector3d(c1);
		double vDist = 1.4 * radius / Math.tan(view.getFieldOfView()/2.0);
        Point3d c2 = new Point3d();

        sceneBnd.getCenter(c2);
		c2.z += vDist;
		vCenter.z += vDist;
		Transform3D vTrans = new Transform3D();

        //translateB.setView(cellBnd.getWidth(), 0);
        //double[] rotVals = User.transformIntoValues(User.get3DRotation());
        //rotateB.setRotation(rotVals[0], rotVals[1], rotVals[2]);
        //zoomB.setZoom(User.get3DOrigZoom());

		vTrans.set(vCenter);

		view.setBackClipDistance((vDist+radius)*200.0);
		view.setFrontClipDistance((vDist+radius)/200.0);
		view.setBackClipPolicy(View.VIRTUAL_EYE);
		view.setFrontClipPolicy(View.VIRTUAL_EYE);
		if (User.is3DPerspective())
		{
            viewingPlatform.getViewPlatformBehavior().setHomeTransform(vTrans);
            viewingPlatform.getViewPlatformBehavior().goHome();
			//viewingPlatform.getViewPlatformTransform().setTransform(vTrans);
		}
		else
		{
            Transform3D proj = new Transform3D();
            Rectangle2D cellBnd = cell.getBounds();
            proj.ortho(cellBnd.getMinX(), cellBnd.getMinX(), cellBnd.getMinY(), cellBnd.getMaxY(), (vDist+radius)/200.0, (vDist+radius)*2.0);
			view.setVpcToEc(proj);
			//viewingPlatform.getViewPlatformTransform().setTransform(lookAt);
		}

        // Create axis with associated behavior
        BranchGroup axisRoot = new BranchGroup();

        // Position the axis
        Transform3D t = new Transform3D();
        //t.set(new Vector3d(-0.5, -0.5, -1.5));
        t.set(new Vector3d(-0.7, -0.5, -2.0)); // good for Mac?
        t.set(new Vector3d(-0.9, -0.5, -2.5)); // set on Linux
        TransformGroup axisTranslation = new TransformGroup(t);
        axisRoot.addChild(axisTranslation);

        // Create transform group to orient the axis and make it
        // readable & writable (this will be the target of the axis
        // behavior)
        TransformGroup axisTG = new TransformGroup();
        axisTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        axisTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        axisTranslation.addChild(axisTG);

        // Create the axis geometry
        J3DAxis axis = new J3DAxis();
        axisTG.addChild(axis);

        // Add axis into BG
        pg.addChild(axisRoot);

        // Create the axis behavior
        TransformGroup viewPlatformTG =
            viewingPlatform.getViewPlatformTransform();
        J3DAxisBehavior axisBehavior = new J3DAxisBehavior(axisTG, viewPlatformTG);
        axisBehavior.setSchedulingBounds(J3DUtils.infiniteBounds);
        pg.addChild(axisBehavior);

        viewingPlatform.setPlatformGeometry(pg) ;


		u.addBranchGraph(scene);
		setWindowTitle();
	}

    /** Font for 3D axis labels */ private static Font3D font3D;

    /**
     * Method to create axes for reference
     * @return
     */
//    private BranchGroup createAxis(BranchGroup main, Vector3d dir, Vector3d center, String label, double length)
//    {
//        // Create Axes;
//        TransformGroup branch = new TransformGroup(); // This transform will control all movements
//        branch.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
//        branch.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
//        main.addChild(branch);
//        Transform3D axisTrans = new Transform3D();
//
//        if (dir == J3DUtils.axisX)
//            axisTrans.rotZ(-Math.PI/2);
//        else if (dir == J3DUtils.axisZ)
//           axisTrans.rotX(Math.PI/2);
//        //center.y += 10;
//        axisTrans.setTranslation(center);
//        TransformGroup axes = new TransformGroup(axisTrans);
//        axes.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
//        axes.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
//
//        Transform3D cylinderTrans = new Transform3D();
//        Vector3d cylinderLocation = new Vector3d(0, length/2, 0); // Cylinder and cone are along Y
//        cylinderTrans.setTranslation(cylinderLocation);
//        TransformGroup cylinderG = new TransformGroup(cylinderTrans);
//        float diameter = (float)length*.03f;
//        Primitive axis = new Cylinder(diameter, (float)length);
//        axis.setAppearance(J3DAppearance.axisApps[0]);
//        cylinderG.addChild(axis);
//        axes.addChild(cylinderG);
//
//        // Text
//        if (font3D == null)
//            font3D = new Font3D(new Font(User.getDefaultFont(), Font.PLAIN, 8),
//                     new FontExtrusion());
//        Text3D txt = new Text3D(font3D, label,
//             new Point3f( -label.length()/2.0f, (float)length, 0));
//        Shape3D sh = new Shape3D();
//        sh.setGeometry(txt);
//        sh.setAppearance(J3DAppearance.axisApps[0]);
//        axes.addChild(sh);
//
//        // Arrow
//        Transform3D arrowTrans = new Transform3D();
//        Vector3d arrowLocation = new Vector3d(0, length, 0); // Cylinder and cone are along Y
//        arrowTrans.set(arrowLocation);
//        TransformGroup arrowG = new TransformGroup(arrowTrans);
//        Primitive arrow = new Cone(1.5f * diameter, (float)length/3);
//        arrow.setAppearance(J3DAppearance.axisApps[0]);
//        arrowG.addChild(arrow);
//
//        axes.addChild(arrowG);
//        branch.addChild(axes);
//        return main;
//    }

    /**
     * Method to create main transformation group
     * @param cell
     * @return
     */
    protected BranchGroup createSceneGraph(Cell cell)
	{
		// Create the root of the branch graph
		BranchGroup objRoot = new BranchGroup();
		objRoot.setCapability(BranchGroup.ALLOW_BOUNDS_READ);
        objRoot.setCapability(BranchGroup.ENABLE_PICK_REPORTING);
        objRoot.setCapability(BranchGroup.ALLOW_BOUNDS_WRITE);

        // Create a Transformgroup to scale all objects so they
        // appear in the scene.
        TransformGroup objScale = new TransformGroup();
        Transform3D t3d = new Transform3D();
        t3d.setScale(0.7);
        objScale.setTransform(t3d);
        objRoot.addChild(objScale);

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
        objTrans.setCapability(TransformGroup.ALLOW_BOUNDS_READ);
		//objRoot.addChild(objTrans);
        objScale.addChild(objTrans);

		// Background
        J3DUtils.createBackground(objRoot);

		View3DEnumerator view3D = new View3DEnumerator();
		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, null, view3D);

        // Create Axes
//        Rectangle2D cellBnd = cell.getBounds();
//        double length = cellBnd.getHeight() > cellBnd.getWidth() ? cellBnd.getHeight() : cellBnd.getWidth();
//        length *= 0.1;
//        Vector3d center = new Vector3d(cellBnd.getMinX() - length, cellBnd.getMinY() - length, 0);
//        axes = new BranchGroup();
//        axes.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
//        createAxis(axes, J3DUtils.axisX, center, "X", length);
//        createAxis(axes, J3DUtils.axisY, center, "Y", length);
//        createAxis(axes, J3DUtils.axisZ, center, "Z", length);
//        objRoot.addChild(axes);

        // new arrow
//        BranchGroup axesq = new BranchGroup();
//        TransformGroup hola = J3DArrow.createArrow(1, new Vector3f(0, 10, 10), new Vector3f(0, 0, 10));
//        axesq.addChild(hola);
//        objRoot.addChild(axesq);

		// Picking tools
		pickCanvas = new PickCanvas(canvas, objRoot);
        pickCanvas.setMode(PickCanvas.GEOMETRY_INTERSECT_INFO);
        pickCanvas.setTolerance(4.0f);

        setInterpolator();

        // create the KeyBehavior and attach main transformation group
		keyBehavior = new J3DKeyCollision(objTrans, null, this);
		keyBehavior.setSchedulingBounds(J3DUtils.infiniteBounds);
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

//		if (direction == 0)
//			translateB.panning(mult*ticks, 0);
//		else
//		    translateB.panning(0, mult*ticks);
	}

    public void setViewAndZoom(double x, double y, double zoom)
    {
//        translateB.setView(x, y);
    }

	/**
	 * Method to zoom out by a factor of 2 plus mouse pre-defined factor
	 */
	public void zoomOutContents()
    {
//        zoomB.zoomInOut(false);
    }

	/**
	 * Method to zoom in by a factor of 2 plus mouse pre-defined factor
	 */
	public void zoomInContents()
    {
        Transform3D trans = new Transform3D();
        TransformGroup transformGroup = u.getViewingPlatform().getViewPlatformTransform();
        double z_factor = orbit.getZoomFactor();
        double factor = (false) ? (1/z_factor) : (z_factor);
        Matrix4d mat = new Matrix4d();
        trans.get(mat);
        double dy = trans.getScale() * factor;
        trans.setScale(dy);
        transformGroup.setTransform(trans);
        //zoomB.zoomInOut(true);
    }

	/**
	 * Method to reset zoom/rotation/extraTrans to original place (Fill Window operation)
	 */
	public void fillScreen()
	{
//		Transform3D trans = new Transform3D();
//	    Transform3D x = new Transform3D();
//		zoomB.getTransformGroup().getTransform(x);
//		zoomB.getTransformGroup().setTransform(trans);
//		rotateB.getTransformGroup().getTransform(x);
//		rotateB.getTransformGroup().setTransform(trans);
//		translateB.getTransformGroup().getTransform(x);
//		translateB.getTransformGroup().setTransform(trans);
        u.getViewingPlatform().getViewPlatformBehavior().goHome();
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
			Poly pol = new Poly(rect);
            list = new ArrayList(1);

            if (transform.getType() != AffineTransform.TYPE_IDENTITY)
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

    /********************************************************************************************************
     *                  Model-View paradigm to control refresh from 2D
     ********************************************************************************************************/

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
				obj.setAppearance(J3DAppearance.highligtApp);
				//app.getRenderingAttributes().setVisible(false);
				J3DAppearance.highligtApp.setGraphics(app.getGraphics());
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
				EGraphics graphics = J3DAppearance.highligtApp.getGraphics();
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

    /**
     * Observer method to highlight 3D nodes by clicking 2D objects
     * @param o
     * @param arg
     */
    public void update(Observable o, Object arg)
    {
        // Undo previous highlight
        selectObject(false, false);

        if (o == view2D.getWindowFrame())
        {
            Highlighter highlighter2D = view2D.getHighlighter();
            List geomList = highlighter2D.getHighlightedEObjs(true, true);

            for (Iterator hIt = geomList.iterator(); hIt.hasNext(); )
            {
                ElectricObject eobj = (ElectricObject)hIt.next();

                List list = (List)electricObjectMap.get(eobj);

                if (list == null || list.size() == 0) continue;

                for (Iterator lIt = list.iterator(); lIt.hasNext();)
                {
                    Shape3D shape = (Shape3D)lIt.next();
                    highlighter.addObject(shape, Highlight.Type.SHAPE3D, cell);
                }
            }
            selectObject(true, false);
            return; // done
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
     * Method to export directly PNG file
     * @param ep
     * @param filePath
     */
    public void writeImage(ElectricPrinter ep, String filePath)
    {
        canvas.filePath = filePath;
        saveImage(false);
    }

    public void saveImage(boolean movieMode)
    {
        canvas.movieMode = movieMode;
        canvas.writePNG_ = true;
        canvas.repaint();
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
            //Forcint the repaint
            canvas.writePNG_ = true;
            canvas.repaint();
            bImage = canvas.img;

//			// Create the off-screen Canvas3D object
//			if (offScreenCanvas3D == null)
//			{
//				offScreenCanvas3D = new J3DUtils.OffScreenCanvas3D(SimpleUniverse.getPreferredConfiguration(), true);
//				// attach the offscreen canvas to the view
//				u.getViewer().getView().addCanvas3D(offScreenCanvas3D);
//				// Set the off-screen size based on a scale3D factor times the
//				// on-screen size
//				Screen3D sOn = canvas.getScreen3D();
//				Screen3D sOff = offScreenCanvas3D.getScreen3D();
//				Dimension dim = sOn.getSize();
//				dim.width *= OFF_SCREEN_SCALE;
//				dim.height *= OFF_SCREEN_SCALE;
//				sOff.setSize(dim);
//				sOff.setPhysicalScreenWidth(sOn.getPhysicalScreenWidth() * OFF_SCREEN_SCALE);
//				sOff.setPhysicalScreenHeight(sOn.getPhysicalScreenHeight() * OFF_SCREEN_SCALE);
//				bImage = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
//				ImageComponent2D buffer = new ImageComponent2D(ImageComponent.FORMAT_RGBA, bImage);
//
//				offScreenCanvas3D.setOffScreenBuffer(buffer);
//			}
//			offScreenCanvas3D.renderOffScreenBuffer();
//			offScreenCanvas3D.waitForOffScreenRendering();
//			bImage = offScreenCanvas3D.getOffScreenBuffer().getImage();
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
        Vector3f newPos = new Vector3f((float)values[0], (float)values[1], (float)values[2]);
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
//                tmpVec.set(newPos);
                boolean invert = true;
//                if (invert) {
//                    currXform.mul(currXform, tmpVec);
//                } else {
//                    currXform.mul(tmpVec, currXform);
//                }
                grp.setTransform(currXform);

                grp.getTransform(currXform);

                Matrix4d mat = new Matrix4d();
                // Remember old matrix
                currXform.get(mat);

                //tmpVec.setEuler(rotation);

                // Translate to rotation point
                currXform.setTranslation(new Vector3d(values[6], values[7], values[8]));
                currXform.setRotation(quaf);
//                if (invert) {
//                currXform.mul(currXform, tmpVec);
//                } else {
//                currXform.mul(tmpVec, currXform);
//                }

                // Set old translation back
                Vector3d translation = new
                Vector3d(mat.m03, mat.m13, mat.m23);
                currXform.setTranslation(translation);

                // Update xform
                grp.setTransform(currXform);
            }
        }
//        return(new J3DUtils.ThreeDDemoKnot(values[0], values[1], values[2], 1,
//                        0, 0, 0, values[3], values[4], values[5]));

        return(new J3DUtils.ThreeDDemoKnot(1, newPos, quaf));
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
			AffineTransform trans = ni.rotateOutAboutTrueCenter();
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
     */
    private void setInterpolator()
    {
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
        tcbSplineInter.setSchedulingBounds(J3DUtils.infiniteBounds);
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
                interMap = addInterpolatorPerGroup(knotList, grp, interMap);
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
     * Method to add interpolator per group
     * @param knotList
     * @param grp
     * @param interMap
     * @return
     */
    public Map addInterpolatorPerGroup(List knotList, TransformGroup grp, Map interMap)
    {
        if (knotList.size() < 2)
        {
            System.out.println("Needs at least 2 frams for the interpolator");
            return null;
        }

        if (interMap == null)
            interMap = new HashMap(1);
        if (grp == null)
            grp = objTrans;
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
        return interMap;
    }

    /**
     * Method to remove certain interpolators from scene graph
     * @param interMap
     */
    public void removeInterpolator(Map interMap)
    {
        canvas.resetMoveFrames();
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

    private Vector3d tmpVec = new Vector3d();
    private Vector3d mapSize = null;
    private Transform3D tmpTrans = new Transform3D();

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
		t3d.get( tmpVec );

		// we need to scale up by the scale that was
		// applied to the root TG on the view side of the scenegraph
		if( bViewSide != false )
			tmpVec.scale( 1.0 / getScale( ) );

//        Vector3d mapSquareSize = getMapSize( );

		// first check that we are still inside the "world"
//		if (tmpVec.x < -getPanel().getWidth() + mapSquareSize.x ||
//			tmpVec.x > getPanel().getWidth() - mapSquareSize.x ||
//			tmpVec.y < -getPanel().getHeight() + mapSquareSize.y ||
//			tmpVec.y > getPanel().getHeight() - mapSquareSize.y  )
//			return true;

		if( bViewSide != false )
			// then do a pixel based look up using the map
			return isCollision(tmpVec);

		return false;
	}

    /**
     * Method to detect if give x, y location in the world collides with geometry
     * @param worldCoord
     * @return
     */
	protected boolean isCollision( Vector3d worldCoord )
	{
		Point2d point = convertToMapCoordinate( worldCoord );

//        PickTool pickTool = new PickTool(scene);
//				pickTool.setMode( PickTool.BOUNDS );
//
//				BoundingSphere bounds = (BoundingSphere) objTrans.getBounds( );
//				PickBounds pickBounds = new PickBounds( new BoundingSphere( new Point3d(keyBehavior.positionVector.x,
//                        keyBehavior.positionVector.y, keyBehavior.positionVector.z), bounds.getRadius( ) ) );
//				pickTool.setShape( pickBounds, new Point3d(0, 0, 0));
//				PickResult[] resultArray = pickTool.pickAll( );
//
//        System.out.println( "Wold Point " + worldCoord + " local " + keyBehavior.positionVector);
//
//        if (resultArray != null)
//        {
//        for( int n = 0; n < resultArray.length; n++ )
//		{
//			Object userData = resultArray[n].getObject( ).getUserData( );
//
//			if ( userData != null && userData instanceof String )
//			{
//					System.out.println( "Collision between: " + objTrans.getUserData( ) + " and: " + userData );
//				// check that we are not colliding with ourselves...
//				if ( ((String) userData).equals( (String) objTrans.getUserData( ) ) == false )
//				{
//					System.out.println( "Collision between: " + objTrans.getUserData( ) + " and: " + userData );
//					return true;
//				}
//			}
//		}
//        }

        pickCanvas.setShapeLocation((int)point.x, (int)point.y);
        PickResult result = pickCanvas.pickClosest();

        if (result != null && result.getNode(PickResult.SHAPE3D) != null)
        {
             Shape3D shape = (Shape3D)result.getNode(PickResult.SHAPE3D);
             //shape.setAppearance(J3DAppearance.highligtApp);
            for (int i = 0; i < result.numIntersections(); i++)
            {
                PickIntersection inter = result.getIntersection(i);
//            System.out.println("Collision " + inter.getDistance() + " " + inter.getPointCoordinates() + " normal " + inter.getPointNormal());
//                 System.out.println("Point  " + point + " world " + worldCoord);
                GeometryArray geo = inter.getGeometryArray();

                if (inter.getDistance() < 6) 
                    return (true); // collision
            }
        }

        return (false);
	}

    public J3DUtils.ThreeDDemoKnot addFrame()
    {
        objTrans.getTransform(tmpTrans);
        tmpTrans.get(tmpVec);
        Quat4f rot = new Quat4f();
        tmpTrans.get(rot);

        return(new J3DUtils.ThreeDDemoKnot(1, new Vector3f(tmpVec), rot));
    }


    /**
     * Method to read in demo knots from disk using serialization
     * @param filename
     * @return
     */
    public List readDemo(String filename)
    {
        if (filename == null) return null;

        List list = null;
        try
        {
            FileInputStream inputStream = new FileInputStream(filename);
            ObjectInputStream in = new ObjectInputStream(inputStream);
            J3DSerialization serial = (J3DSerialization)in.readObject();
            list = serial.list;
            objTrans.getTransform(tmpTrans);
            Transform3D trans = new Transform3D(serial.matrix);
            objTrans.setTransform(trans);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return list;
    }

    public void saveMovie(String filename)
    {
        if (filename != null)
            canvas.saveMovie(filename);
    }

    /**
     * Method to store demo knots in disk using serialization
     * @param knotList
     * @param filename
     */
    public void saveDemo(List knotList, String filename)
    {
        if (filename == null || knotList == null) return;

        try
        {
            FileOutputStream outputStream = new FileOutputStream(filename);
            ObjectOutputStream out = new ObjectOutputStream(outputStream);
            objTrans.getTransform(tmpTrans);
            J3DSerialization serial = new J3DSerialization(knotList, tmpTrans);
            out.writeObject(serial);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
