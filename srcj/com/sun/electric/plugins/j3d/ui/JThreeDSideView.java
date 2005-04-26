package com.sun.electric.plugins.j3d.ui;

import com.sun.electric.tool.user.User;
import com.sun.electric.technology.Layer;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.plugins.j3d.ui.JThreeDTab;
import com.sun.electric.plugins.j3d.*;
import com.sun.electric.plugins.j3d.utils.J3DUtils;
import com.sun.electric.plugins.j3d.utils.J3DAppearance;

import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewingPlatform;
import com.sun.j3d.utils.universe.Viewer;
import com.sun.j3d.utils.universe.PlatformGeometry;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseZoom;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;

import javax.media.j3d.*;
import javax.vecmath.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda
 * Date: Feb 17, 2005
 * Time: 11:14:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class JThreeDSideView extends JPanel
    implements MouseMotionListener, MouseListener
{
    private static Layer currentLayerSelected = null;
    private HashMap layerPolyhedra = null;
    double lowHeight = Double.MAX_VALUE, highHeight = Double.MIN_VALUE;
    private PickCanvas pickCanvas;
    private JThreeDTab parentDialog;

    // This class will store previous Z values assigned to layer
    private static class Shape3DTab
    {
        Shape3D shape;
        double origDist;
        double origThick;

        Shape3DTab(Shape3D shape, double dist, double thick)
        {
            this.shape = shape;
            this.origDist = dist;
            this.origThick = thick;
        }
    }

    public JThreeDSideView(JThreeDTab dialog)
    {
        parentDialog = dialog;
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        setLayout(new BorderLayout());
        Canvas3D canvas = new Canvas3D(config);
        add("Center", canvas);
        canvas.addMouseListener(this);

        // Set global highlight appearance before create the elements
        J3DAppearance.setHighlightedAppearanceValues(this);

        // Creating scene
        BranchGroup scene = createSceneGraph(canvas, J3DUtils.infiniteBounds);

//        SimpleUniverse u = new SimpleUniverse(canvas);
//        u.getViewingPlatform().setNominalViewingTransform();
//        u.addBranchGraph(scene);

        ViewingPlatform viewP = new ViewingPlatform(4);
        viewP.setCapability(ViewingPlatform.ALLOW_CHILDREN_READ);
        Viewer viewer = new Viewer(canvas);
        SimpleUniverse u = new SimpleUniverse(viewP, viewer);
        u.addBranchGraph(scene);


        // lights on ViewPlatform geometry group
        PlatformGeometry pg = new PlatformGeometry();
        J3DUtils.createLights(pg);
        viewP.setPlatformGeometry(pg) ;

//        BranchGroup rotateBG = new BranchGroup();
//        Billboard billboard = new Billboard( objTrans, Billboard.ROTATE_ABOUT_AXIS, new Point3f( 1.0f, 1.0f, 0.0f ) );
//		billboard.setSchedulingBounds( infiniteBounds );
//		rotateBG.addChild( billboard );
//        u.getViewingPlatform().addChild(rotateBG);

        JMouseRotate rotate = new JMouseRotate(MouseRotate.INVERT_INPUT);
        rotate.setTransformGroup(u.getViewingPlatform().getMultiTransformGroup().getTransformGroup(0));
        BranchGroup rotateBG = new BranchGroup();
        rotateBG.addChild(rotate);
        u.getViewingPlatform().addChild(rotateBG);
        rotate.setSchedulingBounds(J3DUtils.infiniteBounds);

        JMouseZoom zoom = new JMouseZoom(canvas, MouseZoom.INVERT_INPUT);
        zoom.setTransformGroup(u.getViewingPlatform().getMultiTransformGroup().getTransformGroup(1));
        zoom.setSchedulingBounds(J3DUtils.infiniteBounds);
        zoom.setFactor(0.7);    // default 0.4
        BranchGroup zoomBG = new BranchGroup();
        zoomBG.addChild(zoom);
        u.getViewingPlatform().addChild(zoomBG);

        JMouseTranslate translate = new JMouseTranslate(canvas, MouseTranslate.INVERT_INPUT);
        translate.setTransformGroup(u.getViewingPlatform().getMultiTransformGroup().getTransformGroup(2));
        translate.setSchedulingBounds(J3DUtils.infiniteBounds);
        translate.setFactor(0.01 * (highHeight-lowHeight)); // default 0.02
        BranchGroup translateBG = new BranchGroup();
        translateBG.addChild(translate);
        u.getViewingPlatform().addChild(translateBG);

//        OrbitBehavior orbit = new OrbitBehavior(canvas, OrbitBehavior.REVERSE_ALL);
//        orbit.setSchedulingBounds(infiniteBounds);
//                Point3d center = new Point3d(-10, -10, (lowHeight+highHeight)/2);
//
//    //orbit.setRotationCenter(center);
//    orbit.setMinRadius(0);
//    orbit.setZoomFactor(1);
//    //orbit.setTransFactors(10, 10);
//    orbit.setProportionalZoom(true);
//        Transform3D home = new Transform3D();
//        home.setTranslation(new Vector3d(center));
//        //orbit.setHomeTransform(home);
//        u.getViewingPlatform().setViewPlatformBehavior(orbit);

        u.getViewingPlatform().setNominalViewingTransform();

        //translate.setView(-10, -50);
        J3DUtils.setViewPoint(u, canvas, scene, new Rectangle2D.Double(0, 0, 10, 20));
        rotate.setRotation(-1.57, 0.5, 0);
        zoom.setZoom(0.5);
    }

    private BranchGroup createSceneGraph(Canvas3D canvas, BoundingSphere infiniteBounds)
    {
        BranchGroup objRoot = new BranchGroup();
        objRoot.setCapability(BranchGroup.ALLOW_BOUNDS_READ);
        objRoot.setCapability(BranchGroup.ENABLE_PICK_REPORTING);
        objRoot.setCapability(BranchGroup.ALLOW_BOUNDS_WRITE);

        // Create a simple Shape3D node; add it to the scene graph.
        Background bg = new Background(new Color3f(new Color(User.getColorBackground())));
        bg.setApplicationBounds(infiniteBounds);
        objRoot.addChild(bg);

        // Create the TransformGroup node and initialize it to the
        // identity. Enable the TRANSFORM_WRITE capability so that
        // our behavior code can modify it at run time. Add it to
        // the root of the subgraph.
        	// Create a transform group node to scale and position the object.
        Transform3D t = new Transform3D();
        t.set(1, new Vector3d(0, -10, -40));
        TransformGroup objTrans = new TransformGroup(t);
        objRoot.addChild(objTrans);

        TransformGroup nodesGroup = new TransformGroup();
        nodesGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        nodesGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        nodesGroup.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
        objTrans.addChild(nodesGroup);

        layerPolyhedra = new HashMap(parentDialog.getTech().getNumLayers());
        for(Iterator it = parentDialog.getTech().getLayers(); it.hasNext(); )
        {
            Layer layer = (Layer)it.next();
            if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;
            //if (!layer.isVisible()) continue;
            double xyFactor = (layer.getFunctionExtras() == Layer.Function.CONMETAL) ? 0.8 : 1;
            J3DAppearance ap = (J3DAppearance)parentDialog.transparencyMap.get(layer);
            GenMath.MutableDouble thickness = (GenMath.MutableDouble)parentDialog.threeDThicknessMap.get(layer);
            GenMath.MutableDouble distance = (GenMath.MutableDouble)parentDialog.threeDDistanceMap.get(layer);
            double dis = distance.doubleValue();
            double thick = thickness.doubleValue();
            Rectangle2D bounds = new Rectangle2D.Double(0, 0, 10*xyFactor, 20*xyFactor);
            Shape3DTab shape = new Shape3DTab(J3DUtils.addPolyhedron(bounds, dis, thick, ap, nodesGroup), dis, thick);
            layerPolyhedra.put(layer, shape);
            if (dis < lowHeight)
                lowHeight = dis;
            double max = dis + thick;
            if (max > highHeight)
                highHeight = max;
        }

        // picking tool
        pickCanvas = new PickCanvas(canvas, objRoot);
		//pickCanvas.setMode(PickCanvas.BOUNDS);
        pickCanvas.setMode(PickCanvas.GEOMETRY_INTERSECT_INFO);
        pickCanvas.setTolerance(4.0f);

        objRoot.compile();

        return objRoot;
    }

    /**
     * To highlight layer according to list on the left
     * @param layer
     */
    public void showLayer(Layer layer)
    {
        Shape3DTab shape;

        if (currentLayerSelected != null)
        {
            // For this shape, its appareance has to be set back to normal
            shape = (Shape3DTab)layerPolyhedra.get(currentLayerSelected);
            if (shape != null) // is null if previous shape belongs to another dialog (another tech)
                shape.shape.setAppearance((J3DAppearance)parentDialog.transparencyMap.get(currentLayerSelected));
        }
        shape = (Shape3DTab)layerPolyhedra.get(layer);
        if (shape != null)
            shape.shape.setAppearance(J3DAppearance.highligtApp);
        else
            System.out.println("Shape is null in JThreeDSideView.showLayer");
        currentLayerSelected = layer;
    }

    public void updateZValues(Layer layer, double thickness, double distance)
    {
        Shape3DTab shape = (Shape3DTab)layerPolyhedra.get(layer);

        if (J3DUtils.updateZValues(shape.shape, (float)shape.origDist,  (float)(shape.origDist+shape.origThick),
                (float)distance, (float)(distance+thickness)))
        {
            // It has to remember temporary new values until they are committed into database
            shape.origDist = distance;
            shape.origThick = thickness;
        }
    }

    // the MouseEvent events
    public void mousePressed(MouseEvent evt)
    {
    }
    public void mouseReleased(MouseEvent evt) {}

    /**
     * Method to handle picking tool in 3D
     * @param evt
     */
    public void mouseClicked(MouseEvent evt)
    {
        pickCanvas.setShapeLocation(evt);
		PickResult result = pickCanvas.pickClosest();

		if (result != null)
		{
            Shape3D s = (Shape3D)result.getNode(PickResult.SHAPE3D);
            if (s != null)
            {
                J3DAppearance app = (J3DAppearance)s.getAppearance();
                if (app != J3DAppearance.highligtApp)
                {
                    Layer layer = app.getGraphics().getLayer();
                    parentDialog.threeDLayerList.setSelectedValue(layer.getName(), false);
                    parentDialog.processDataInFields(layer, false);
                }
            }
		}
    }

    public void mouseEntered(MouseEvent evt) {}
    public void mouseExited(MouseEvent evt) {}

    // the MouseMotionListener events
    public void mouseMoved(MouseEvent evt) {}
    public void mouseDragged(MouseEvent evt) {}
}