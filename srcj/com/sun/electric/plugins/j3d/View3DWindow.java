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

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.plugins.j3d.utils.J3DAppearance;
import com.sun.electric.plugins.j3d.utils.J3DAxis;
import com.sun.electric.plugins.j3d.utils.J3DCanvas3D;
import com.sun.electric.plugins.j3d.utils.J3DUtils;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.Highlight2;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.ElectricPrinter;
import com.sun.electric.tool.user.ui.StatusBar;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.j3d.utils.behaviors.interpolators.KBKeyFrame;
import com.sun.j3d.utils.behaviors.interpolators.RotPosScaleTCBSplinePathInterpolator;
import com.sun.j3d.utils.behaviors.interpolators.TCBKeyFrame;
import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickIntersection;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.universe.PlatformGeometry;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewingPlatform;

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
import java.awt.print.PageFormat;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import javax.media.j3d.*;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.tree.MutableTreeNode;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

/**
 * This class deals with 3D View using Java3D
 * @author  Gilda Garreton
 * @version 0.1
 */
public class View3DWindow extends JPanel
        implements WindowContent, MouseMotionListener, MouseListener, MouseWheelListener, KeyListener, ActionListener,
        Observer
{
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
    private Map<TransformGroup,Interpolator> interpolatorMap = new HashMap<TransformGroup,Interpolator>();
    private J3DKeyCollision keyBehavior;

    /** the window frame containing this editwindow */      private WindowFrame wf;
	/** reference to 2D view of the cell */                 private EditWindow view2D;
	/** the cell that is in the window */					protected Cell cell;
    /** scale3D factor in Z axis */                         private double scale3D = J3DUtils.get3DFactor();
	/** Highlighter for this window */                      private Highlighter highlighter;
	private PickCanvas pickCanvas;
	/** Lis with all Shape3D drawn per ElectricObject */    private HashMap<ElectricObject,List<Shape3D>> electricObjectMap = new HashMap<ElectricObject,List<Shape3D>>();
    private boolean oneTransformPerNode = false;
    /** Map with object transformation for individual moves */ private HashMap<Shape3D,TransformGroup> transformGroupMap = new HashMap<Shape3D,TransformGroup>();
    /** To detect max number of nodes */                    private boolean reachLimit = false;
    /** To ask question only once */                        private boolean alreadyChecked = false;
    /** Job reference */                                    private Job job;
    /** Reference to limit to consider scene graph big */   private int maxNumNodes;

    /** Inner class to create 3D view in a job. This should be safer in terms of the number of nodes
     * and be able to stop it
     */
    private static class View3DWindowJob extends Job
    {
        private Cell cell;
        private transient WindowFrame windowFrame;
        private transient WindowContent view2D;
        private boolean transPerNode;

        public View3DWindowJob(Cell cell, WindowFrame wf, WindowContent view2D, boolean transPerNode)
        {
            super("3D View Job", null, Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.cell = cell;
            this.windowFrame = wf;
            this.view2D = view2D;
            this.transPerNode = transPerNode;
			startJob();
        }

        public boolean doIt() throws JobException
        {
            return true;
        }

        public void terminateOK()
        {
            View3DWindow window = new View3DWindow(cell, windowFrame, view2D, transPerNode, this);
            windowFrame.finishWindowFrameInformation(window, cell);
        }
    }

    public static void create3DWindow(Cell cell, WindowFrame wf, WindowContent view2D, boolean transPerNode)
    {
        new View3DWindowJob(cell, wf, view2D, transPerNode);
//        View3DWindow window = new View3DWindow(cell, wf, view2D, transPerNode, this);
//        wf.finishWindowFrameInformation(window, cell);
    }

    public void getObjTransform(Transform3D trans)
    {
        objTrans.getTransform(trans);
    }

    public void setObjTransform(Transform3D trans)
    {
        objTrans.setTransform(trans);
    }

    /**
     * Method to return if size limit has been reached
     * @param number
     * @return true if number of nodes is still under maximum value
     */
    private boolean isSizeLimitOK(int number)
    {
        if (reachLimit || number > maxNumNodes)
        {
            // Only ask once
            if (!alreadyChecked)
            {
                String[] possibleValues = { "Full", "Limit", "Cancel" };
                int response = Job.getUserInterface().askForChoice("Number of nodes in graph scene reached limit of " + maxNumNodes +
                    " (loaded " + number + " nodes so far).\nClick 'Full' to include all nodes in " +cell +
                    ", 'Limit' to show " + number + " nodes or 'Cancel' to abort process.\nUnexpand cells to reduce the number).",
                    "Warning", possibleValues, possibleValues[2]);
                alreadyChecked = true;
                if (response > 0) // Cancel or limit
                {
                    if (response == 2)
                        job.abort();
                    reachLimit = true;
                }
            }
            if (reachLimit)
                return false;
        }
        return true;
    }

    // constructor
	View3DWindow(Cell cell, WindowFrame wf, WindowContent view2D, boolean transPerNode, Job job)
	{
		this.cell = cell;
        this.wf = wf;
		this.view2D = (EditWindow)view2D;
        // Adding observer
        this.view2D.getWindowFrame().addObserver(this);
        this.oneTransformPerNode = transPerNode;
        this.job = job;
        this.maxNumNodes = J3DUtils.get3DMaxNumNodes();

		highlighter = new Highlighter(Highlighter.SELECT_HIGHLIGHTER, wf);

		setLayout(new BorderLayout());
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        if (config == null)
        {
        GraphicsConfigTemplate3D gc3D = new GraphicsConfigTemplate3D( );
		gc3D.setSceneAntialiasing( GraphicsConfigTemplate.PREFERRED );
		GraphicsDevice gd[] = GraphicsEnvironment.getLocalGraphicsEnvironment( ).getScreenDevices( );
        config = gd[0].getBestConfiguration( gc3D );
        }

		canvas = new J3DCanvas3D(config);
		add("Center", canvas);
		canvas.addMouseListener(this);

        // Set global appearances before create the elements
        J3DAppearance.setCellAppearanceValues(this);
        J3DAppearance.setHighlightedAppearanceValues(this);
        J3DAppearance.setAxisAppearanceValues(this);

        // Set global alpha value
        J3DUtils.setAlpha(J3DUtils.get3DAlpha());

		// Create a simple scene and attach it to the virtual universe
		scene = createSceneGraph(cell);
        if (scene == null) return;

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
//		orbit.setZoomFactor(10);
//		orbit.setTransFactors(10, 10);
        orbit.setProportionalZoom(true);

        viewingPlatform.setNominalViewingTransform();
    	viewingPlatform.setViewPlatformBehavior(orbit);

		double radius = sceneBnd.getRadius();
		View view = u.getViewer().getView();

		// Too expensive at this point
        if (canvas.getSceneAntialiasingAvailable() && J3DUtils.is3DAntialiasing())
		    view.setSceneAntialiasingEnable(true);

		// Setting the projection policy
		view.setProjectionPolicy(J3DUtils.is3DPerspective()? View.PERSPECTIVE_PROJECTION : View.PARALLEL_PROJECTION);
		if (!J3DUtils.is3DPerspective()) view.setCompatibilityModeEnable(true);

        // Setting transparency sorting
        view.setTransparencySortingPolicy(View.TRANSPARENCY_SORT_GEOMETRY);
        view.setDepthBufferFreezeTransparent(false); // set to true only for transparent layers

        // Setting a good viewpoint for the camera
		Vector3d vCenter = new Vector3d(center);
		double vDist = 1.4 * radius / Math.tan(view.getFieldOfView()/2.0);
		vCenter.z += vDist;
		Transform3D vTrans = new Transform3D();

        //translateB.setView(cellBnd.getWidth(), 0);
        //double[] rotVals = User.transformIntoValues(User.get3DRotation());
        //rotateB.setRotation(rotVals[0], rotVals[1], rotVals[2]);
        //zoomB.setZoom(User.get3DOrigZoom());

		vTrans.setTranslation(vCenter);

		view.setBackClipDistance((vDist+radius)*200.0);
		view.setFrontClipDistance((vDist+radius)/200.0);
//		view.setBackClipPolicy(View.VIRTUAL_EYE);
//		view.setFrontClipPolicy(View.VIRTUAL_EYE);
		if (J3DUtils.is3DPerspective())
		{
            //keyBehavior.setHomeRotation(User.transformIntoValues(J3DUtils.get3DRotation()));

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

		u.addBranchGraph(scene);

        // Create axis with associated behavior
        BranchGroup axisRoot = new BranchGroup();

        // Position the axis
        Transform3D t = new Transform3D();
//        t.set(new Vector3d(-0.9, -0.5, -2.5)); // set on Linux
        t.set(new Vector3d(-radius/10, -radius/16, -radius/3.5));
        TransformGroup axisTranslation = new TransformGroup(t);
        axisRoot.addChild(axisTranslation);

        // Create the axis behavior
        // Using reflection to create this behavior because Java3D plugin
        // might not be available. Reflection would have to  be here until Java3D team
        // releases this new behavior
        Class plugin = Resources.getJMFJ3DClass("J3DAxisBehavior");
        if (plugin == null)
        {
            if (Job.getDebug())
                System.out.println("Java3D plugin not available. 3D axes not created.");
        }
        else
        {
            // Create transform group to orient the axis and make it
            // readable & writable (this will be the target of the axis
            // behavior)
            TransformGroup axisTG = new TransformGroup();
            axisTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
            axisTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
            axisTranslation.addChild(axisTG);

            // Create the axis geometry
            J3DAxis axis = new J3DAxis(radius/10, J3DAppearance.axisApps[0], J3DAppearance.axisApps[1],
                    J3DAppearance.axisApps[2], User.getDefaultFont());
            axisTG.addChild(axis);

            // Add axis into BG
            pg.addChild(axisRoot);

            TransformGroup viewPlatformTG = viewingPlatform.getViewPlatformTransform();
            try {
                Constructor instance = plugin.getDeclaredConstructor(new Class[]{TransformGroup.class, TransformGroup.class});
                Object obj = instance.newInstance(new Object[] {axisTG, viewPlatformTG});
                if (obj != null)
                {
                    Behavior axisBehavior = (Behavior)obj; //new J3DAxisBehavior(axisTG, viewPlatformTG);
                    axisBehavior.setSchedulingBounds(J3DUtils.infiniteBounds);
                    pg.addChild(axisBehavior);
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        viewingPlatform.setPlatformGeometry(pg) ;

		setWindowTitle();
	}

    /**
     * Method to create main transformation group
     * @param cell
     * @return BrachGroup representing the scene graph
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
		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, view3D);
//		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, null, view3D);

        if (job.checkAbort()) return null; // Job cancel

		// Picking tools
		pickCanvas = new PickCanvas(canvas, objRoot);
        pickCanvas.setMode(PickCanvas.GEOMETRY_INTERSECT_INFO);
        pickCanvas.setTolerance(4.0f);

        setInterpolator();

        // create the KeyBehavior and attach main transformation group
		keyBehavior = new J3DKeyCollision(objTrans, this);
		keyBehavior.setSchedulingBounds(J3DUtils.infiniteBounds);
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
	public void initTextSearch(String search, boolean caseSensitive, boolean regExp, Set<TextUtils.WhatToSearch> whatToSearch) {}

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

		int mult = (int)((double)10 * panningAmount);
		if (mult == 0) mult = 1;

        keyBehavior.moveAlongAxis(direction, mult*ticks);
	}

//    public void setViewAndZoom(double x, double y, double zoom)
//    {
////        translateB.setView(x, y);
//    }

	/**
	 * Method to zoom out by a factor of 2 plus mouse pre-defined factor
	 */
	public void zoomOutContents()
    {
        keyBehavior.zoomInOut(false);
//        zoomB.zoomInOut(false);
    }

	/**
	 * Method to zoom in by a factor of 2 plus mouse pre-defined factor
	 */
	public void zoomInContents()
    {
        keyBehavior.zoomInOut(true);
        //zoomB.zoomInOut(true);
    }

	/**
	 * Method to reset zoom/rotation/extraTrans to original place (Fill Window operation)
	 */
	public void fillScreen()
	{
        objTrans.setTransform(new Transform3D());
        u.getViewingPlatform().getViewPlatformBehavior().goHome();
	}

	public void setCell(Cell cell, VarContext context, WindowFrame.DisplayAttributes da) {}
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
	 */
	public List<MutableTreeNode> loadExplorerTrees()
	{
        return wf.loadDefaultExplorerTree();
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

		List<Shape3D> list = addPolys(tech.getShapeOfArc(ai), transform, objTrans);
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

		List<Shape3D> list = null;
		if (no.isCellInstance())
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
            list = new ArrayList<Shape3D>(1);

            if (transform.getType() != AffineTransform.TYPE_IDENTITY)
			    pol.transform(transform);
			rect = pol.getBounds2D();
			list.add(J3DUtils.addPolyhedron(rect, values[0], values[1] - values[0], J3DAppearance.cellApp, objTrans));
		}
		else
        {
            Poly[] polys = tech.getShapeOfNode(no, null, null, true, true, null);
            List<Shape3D> boxList = null;

            // Special case for transistors
            if (nProto.getFunction().isTransistor())
            {
                int[] active = new int[2];
                boxList = new ArrayList<Shape3D>(4);

                // Merge active regions
                for (int i = 0; i < polys.length; i++)
                {
                    Layer.Function fun = polys[i].getLayer().getFunction();
                    if (fun.isDiff())
                    {
                        // Only 2 active regions are allowed
                        if (count > 1)
                            System.out.println("More than 2 active regions detected in Transistor '" + no.getName() + "'. Ignoring this layer");
                        else
                            active[count++] = i;
                    }
                    else if (fun.isGatePoly())
                        gate = i;
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
                List<Point3d> topList = new ArrayList<Point3d>();
		        List<Point3d> bottomList = new ArrayList<Point3d>();
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
	private List<Shape3D> addPolys(Poly [] polys, AffineTransform transform, TransformGroup objTrans)
	{
		if (polys == null) return (null);

		List<Shape3D> list = new ArrayList<Shape3D>();
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
		for (Highlight2 h : highlighter.getHighlights())
		{
//			Shape3D obj = (Shape3D)h.getObject();
            HighlightShape3D hObj = (HighlightShape3D)(h.getObject());
            Shape3D obj = hObj.shape;
			if (toSelect) // highlight cell, set transparency
			{
				//J3DAppearance app = (J3DAppearance)obj.getAppearance();
				obj.setAppearance(J3DAppearance.highligtApp);
				//app.getRenderingAttributes().setVisible(false);
				//J3DAppearance.highligtApp.setGraphics(app.getGraphics());
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
				//EGraphics graphics = J3DAppearance.highligtApp.getGraphics();
                if (hObj.origApp != null)
				//if (graphics != null)
				{
					//J3DAppearance origAp = (J3DAppearance)graphics.get3DAppearance();
					obj.setAppearance(hObj.origApp);
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
            List<Geometric> geomList = highlighter2D.getHighlightedEObjs(true, true);

            for (Geometric geom : geomList)
            {
                ElectricObject eobj = (ElectricObject)geom;

                List<Shape3D> list = electricObjectMap.get(eobj);

                if (list == null || list.size() == 0) continue;

                for (Shape3D shape : list)
                {
                    highlighter.addObject(new HighlightShape3D(shape), cell);
                }
            }
            selectObject(true, false);
            return; // done
        }
    }

    /** This class will help to remember original appearance of the node
     *
     */
    private static class HighlightShape3D
    {
        Shape3D shape;
        Appearance origApp;

        HighlightShape3D(Shape3D shape)
        {
            this.shape = shape;
            this.origApp = shape.getAppearance();;
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

       	for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
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
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof View3DWindow)) continue;
			View3DWindow wnd = (View3DWindow)content;
			View view = wnd.u.getViewer().getView();
			view.setSceneAntialiasingEnable(value);
		}
	}

    /**
     * Method to change geometry of all nodes using this particular layer
     * This could be an expensive function!.
     * @param layer
     * @param distance
     * @param thickness
     */
    public static void setZValues(Layer layer, Double origDist, Double origThick, Double distance, Double thickness)
    {
        for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
        {
            WindowFrame wf = it.next();
            WindowContent content = wf.getContent();
            if (!(content instanceof View3DWindow)) continue;
            View3DWindow wnd = (View3DWindow)content;
            for (int i = 0; i < wnd.objTrans.numChildren(); i++)
            {
                Node node = wnd.objTrans.getChild(i);
                if (node instanceof Shape3D)
                {
                    Shape3D shape = (Shape3D)node;
                    J3DAppearance app = (J3DAppearance)shape.getAppearance();
                    if (app.getGraphics().getLayer() == layer)
                        J3DUtils.updateZValues(shape, origDist.floatValue(), (float)(origDist.floatValue()+origThick.floatValue()),
                                distance.floatValue(), (float)(distance.floatValue()+thickness.floatValue()));

                }
            }
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
	 * Method to intialize for printing.
	 * @param ep the ElectricPrinter object.
	 * @param pageFormat information about the print job.
     * @return false for now.
	 */
    public boolean initializePrinting(ElectricPrinter ep, PageFormat pageFormat) { return false;}

	/**
	 * Method to print window using offscreen canvas.
	 * @param ep printable object.
	 * @return the image to print (null on error).
	 */
	public BufferedImage getPrintImage(ElectricPrinter ep)
    {
		BufferedImage bImage = ep.getBufferedImage();
        //int OFF_SCREEN_SCALE = 3;

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
		setCell(cell, VarContext.globalContext, null);
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

        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext();)
        {
            NodeInst ni = it.next();
            Variable var = (Variable)ni.getVar("3D_NODE_DEMO");
            if (var == null) continue;
            List<Shape3D> list = electricObjectMap.get(ni);
            for (int i = 0; i < list.size(); i++)
            {
                Shape3D obj = list.get(i);
                TransformGroup grp = transformGroupMap.get(obj);

                grp.getTransform(currXform);
                currXform.setTranslation(newPos);
//                tmpVec.set(newPos);
//                boolean invert = true;
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
        return(new J3DUtils.ThreeDDemoKnot(1, newPos, quaf, null));
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
				highlighter.addObject(new HighlightShape3D(s), cell);
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
			UserInterface ui = Job.getUserInterface();
			ui.alignToGrid(pt);

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

//    public void highlightChanged(Highlighter which) {
//        repaint();
//    }

//    public void highlighterLostFocus(Highlighter highlighterGainedFocus) {}

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
            if (job != null && job.checkAbort()) return false;
            if (!isSizeLimitOK(info.getCell().getNumArcs() + objTrans.numChildren())) return false;  // limit reached

            AffineTransform rTrans = info.getTransformToRoot();

			for(Iterator<ArcInst> it = info.getCell().getArcs(); it.hasNext(); )
			{
				addArc(it.next(), rTrans, objTrans);
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
            if (reachLimit) return false;
            if (job != null && job.checkAbort()) return false;

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

            if (!isSizeLimitOK(grp.numChildren())) return false;  // limit reached

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
//    public void set3DCamera(int mode)
//    {
////        if (mode == 0)
////        {
////            boolean state = kbSplineInter.getEnable();
////            kbSplineInter.setEnable(!state);
////        }
////        else
////        {
////            boolean state = tcbSplineInter.getEnable();
////            tcbSplineInter.setEnable(!state);
////        }
//    }

    /**
     * Method to create spline interpolator for demo mode
     */
    private void setInterpolator()
    {
        Transform3D yAxis = new Transform3D();
        List<J3DUtils.ThreeDDemoKnot> polys = new ArrayList<J3DUtils.ThreeDDemoKnot>();

        double [] zValues = new double[2];
        cell.getZValues(zValues);
        double zCenter = (zValues[0] + zValues[1])/2;
        Rectangle2D bounding = cell.getBounds();
        Vector3d translation = new Vector3d (bounding.getCenterX(), bounding.getCenterY(), zCenter);
        yAxis.setTranslation(translation);

        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext();)
        {
            NodeInst ni = it.next();
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
            J3DUtils.ThreeDDemoKnot knot = polys.get(i);
            splineKeyFrames[i] = J3DUtils.getNextKBKeyFrame((float)((float)i/(polys.size()-1)), knot);
            keyFrames[i] = J3DUtils.getNextTCBKeyFrame((float)((float)i/(polys.size()-1)), knot);
        }

//        kbSplineInter = new KBRotPosScaleSplinePathInterpolator(jAlpha, objTrans,
//                                                  yAxis, splineKeyFrames);
//        kbSplineInter.setSchedulingBounds(infiniteBounds);
//        //behaviorBranch.addChild(kbSplineInter);
//        kbSplineInter.setEnable(false);

        Interpolator tcbSplineInter = new RotPosScaleTCBSplinePathInterpolator(J3DUtils.jAlpha, objTrans,
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
     * @param knotList list with knot data. If null, search for data attached to nodes
     */
    public Map<TransformGroup,BranchGroup> addInterpolator(List<J3DUtils.ThreeDDemoKnot> knotList)
    {
        if (knotList != null && knotList.size() < 2)
        {
            System.out.println("Needs at least 2 frams for the interpolator");
            return null;
        }

        Map<TransformGroup,BranchGroup> interMap = new HashMap<TransformGroup,BranchGroup>(1);
        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext();)
        {
            NodeInst ni = it.next();
            Variable var = (Variable)ni.getVar("3D_NODE_DEMO");
            if (var == null) continue;
            List<J3DUtils.ThreeDDemoKnot> tmpList = knotList;
            if (tmpList == null)
            {
                tmpList = J3DUtils.readDemoDataFromFile(this);
                if (tmpList == null) continue; // nothing load
            }
            List<Shape3D> list = electricObjectMap.get(ni);
            for (int j = 0; j < list.size(); j++)
            {
                Shape3D obj = list.get(j);
                TransformGroup grp = transformGroupMap.get(obj);
                interMap = addInterpolatorPerGroup(tmpList, grp, interMap, false);
//                BranchGroup behaviorBranch = new BranchGroup();
//                behaviorBranch.setCapability(BranchGroup.ALLOW_DETACH); // to detach this branch from parent group
//                TCBKeyFrame[] keyFrames = new TCBKeyFrame[tmpList.size()];
//                for (int i = 0; i < tmpList.size(); i++)
//                {
//                    J3DUtils.ThreeDDemoKnot knot = tmpList.get(i);
//                    keyFrames[i] = J3DUtils.getNextTCBKeyFrame((float)((float)i/(tmpList.size()-1)), knot);
//                }
//                Transform3D yAxis = new Transform3D();
//                Interpolator tcbSplineInter = new RotPosScaleTCBSplinePathInterpolator(J3DUtils.jAlpha, grp,
//                                                          yAxis, keyFrames);
//                tcbSplineInter.setSchedulingBounds(new BoundingSphere(new Point3d(), Double.MAX_VALUE));
//                behaviorBranch.addChild(tcbSplineInter);
//                interMap.put(grp, behaviorBranch);
//                grp.addChild(behaviorBranch);
//                interpolatorMap.put(grp, tcbSplineInter);
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
    public Map<TransformGroup,BranchGroup> addInterpolatorPerGroup(List<J3DUtils.ThreeDDemoKnot> knotList, TransformGroup grp, Map<TransformGroup,BranchGroup> interMap, boolean useView)
    {
        if (knotList == null || knotList.size() < 2)
        {
            System.out.println("Needs at least 2 frams for the interpolator");
            return null;
        }

        if (interMap == null)
            interMap = new HashMap<TransformGroup,BranchGroup>(1);
        if (grp == null)
        {
            if (!useView) grp = objTrans;
            else grp = u.getViewingPlatform().getViewPlatformTransform();
        }
        BranchGroup behaviorBranch = new BranchGroup();
        behaviorBranch.setCapability(BranchGroup.ALLOW_DETACH); // to detach this branch from parent group
        TCBKeyFrame[] keyFrames = new TCBKeyFrame[knotList.size()];
        for (int i = 0; i < knotList.size(); i++)
        {
            J3DUtils.ThreeDDemoKnot knot = knotList.get(i);
            keyFrames[i] = J3DUtils.getNextTCBKeyFrame((float)((float)i/(knotList.size()-1)), knot);
        }
        Transform3D yAxis = new Transform3D();
        Interpolator tcbSplineInter = new J3DRotPosScaleTCBSplinePathInterpolator(J3DUtils.jAlpha, grp,
                                                  yAxis, keyFrames, knotList);
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
    public void removeInterpolator(Map<TransformGroup,BranchGroup> interMap)
    {
        canvas.resetMoveFrames();
        for (TransformGroup grp : interMap.keySet())
        {
            Node node = interMap.get(grp);
            grp.removeChild(node);
        }
    }

    ///////////////////// KEY BEHAVIOR FUNCTION ///////////////////////////////

    private static Vector3d tmpVec = new Vector3d();
    private static Vector3d mapSize = null;

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

    public boolean isCollision(Transform3D t3d)
	{
		// get the translation
		t3d.get(tmpVec);

		// we need to scale up by the scale that was
		// applied to the root TG on the view side of the scenegraph
			tmpVec.scale( 1.0 / getScale( ) );

//        Vector3d mapSquareSize = getMapSize( );

		// first check that we are still inside the "world"
//		if (tmpVec.x < -getPanel().getWidth() + mapSquareSize.x ||
//			tmpVec.x > getPanel().getWidth() - mapSquareSize.x ||
//			tmpVec.y < -getPanel().getHeight() + mapSquareSize.y ||
//			tmpVec.y > getPanel().getHeight() - mapSquareSize.y  )
//			return true;

        // then do a pixel based look up using the map
        return isCollision(tmpVec);
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
//             Shape3D shape = (Shape3D)result.getNode(PickResult.SHAPE3D);
             //shape.setAppearance(J3DAppearance.highligtApp);
            for (int i = 0; i < result.numIntersections(); i++)
            {
                PickIntersection inter = result.getIntersection(i);
//            System.out.println("Collision " + inter.getDistance() + " " + inter.getPointCoordinates() + " normal " + inter.getPointNormal());
//                 System.out.println("Point  " + point + " world " + worldCoord);
//                GeometryArray geo = inter.getGeometryArray();

                if (inter.getDistance() < 6) 
                    return (true); // collision
            }
        }

        return (false);
	}

    public J3DUtils.ThreeDDemoKnot addFrame(boolean useView)
    {
        Transform3D tmpTrans = new Transform3D();
        if (!useView) objTrans.getTransform(tmpTrans);
        else u.getViewingPlatform().getViewPlatformTransform().getTransform(tmpTrans);
        tmpTrans.get(tmpVec);
        Quat4f rot = new Quat4f();
        tmpTrans.get(rot);
        Shape3D shape = null;


//        for (Highlight h : highlighter.getHighlights())
//		{
//			shape = (Shape3D)h.getObject();
//            break;
//        }
//        repaint();
        return(new J3DUtils.ThreeDDemoKnot(1, new Vector3f(tmpVec), rot, shape));
    }

    public void saveMovie(String filename)
    {
        if (filename != null)
            canvas.saveMovie(filename);
    }

    private static class J3DRotPosScaleTCBSplinePathInterpolator extends com.sun.j3d.utils.behaviors.interpolators.RotPosScaleTCBSplinePathInterpolator
    {
        List knotList;
        int previousUpper = -1, previousLower = -1;

        public J3DRotPosScaleTCBSplinePathInterpolator(Alpha alpha, TransformGroup target, Transform3D axisOfTransform, TCBKeyFrame[] keys, List list)
        {
            super(alpha, target, axisOfTransform, keys);
            knotList = list;
        }

        public void processStimulus(java.util.Enumeration criteria)
        {
            super.processStimulus(criteria);
//
//            if (upperKnot == previousUpper && lowerKnot == previousLower) return;
//            previousUpper = upperKnot;
//            previousLower = lowerKnot;
//            J3DUtils.ThreeDDemoKnot knot = knotList.get(upperKnot-1);
//            if (knot != null && knot.shape != null)
//                target.addChild(knot.shape);
////            knot.shape.getAppearance().getRenderingAttributes().setVisible(true);
//            knot = knotList.get(lowerKnot-1);
//            if (knot != null && knot.shape != null)
////                target.removeChild(knot.shape);
//            knot.shape.getAppearance().getRenderingAttributes().setVisible(false);
////            System.out.println("Criteria " + upperKnot + " " + lowerKnot);
        }
    }
}
;