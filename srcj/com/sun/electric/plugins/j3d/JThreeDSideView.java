package com.sun.electric.plugins.j3d;

import com.sun.electric.tool.user.User;
import com.sun.electric.technology.Layer;
import com.sun.electric.database.geometry.GenMath;

import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewingPlatform;
import com.sun.j3d.utils.universe.Viewer;
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

    public JThreeDSideView(JThreeDTab dialog)
    {
        parentDialog = dialog;
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        BoundingSphere infiniteBounds = new BoundingSphere(new Point3d(), Double.MAX_VALUE);
        setLayout(new BorderLayout());
        Canvas3D canvas = new Canvas3D(config);
        add("Center", canvas);
        canvas.addMouseListener(this);

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
        TransformGroup objTrans = new TransformGroup();
        objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        objTrans.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
        objRoot.addChild(objTrans);

        layerPolyhedra = new HashMap(dialog.getTech().getNumLayers());
        for(Iterator it = dialog.getTech().getLayers(); it.hasNext(); )
        {
            Layer layer = (Layer)it.next();
            if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;
            if (!layer.isVisible()) continue;
            JAppearance ap = JAppearance.getAppearance(layer);
            GenMath.MutableDouble thickness = (GenMath.MutableDouble)dialog.threeDThicknessMap.get(layer);
            GenMath.MutableDouble distance = (GenMath.MutableDouble)dialog.threeDDistanceMap.get(layer);
            double dis = distance.doubleValue();
            double thick = thickness.doubleValue();
            Rectangle2D bounds = new Rectangle2D.Double(0, 0, 10, 20);
            layerPolyhedra.put(layer, J3DUtils.addPolyhedron(bounds, dis, thick, ap, objTrans));
            if (dis < lowHeight)
                lowHeight = dis;
            double max = dis + thick;
            if (max > highHeight)
                highHeight = max;
        }

        // lights
        J3DUtils.createLights(infiniteBounds, objRoot, objTrans);

        // picking tool
        pickCanvas = new PickCanvas(canvas, objRoot);
		//pickCanvas.setMode(PickCanvas.BOUNDS);
        pickCanvas.setMode(PickCanvas.GEOMETRY_INTERSECT_INFO);
        pickCanvas.setTolerance(4.0f);

        //objRoot.compile();

        ViewingPlatform viewP = new ViewingPlatform(4);
        viewP.setCapability(ViewingPlatform.ALLOW_CHILDREN_READ);
        Viewer viewer = new Viewer(canvas);
        SimpleUniverse u = new SimpleUniverse(viewP, viewer);
        u.addBranchGraph(objRoot);

        JMouseRotate rotate = new JMouseRotate(MouseRotate.INVERT_INPUT);
        rotate.setTransformGroup(u.getViewingPlatform().getMultiTransformGroup().getTransformGroup(0));
        BranchGroup rotateBG = new BranchGroup();
        rotateBG.addChild(rotate);
        u.getViewingPlatform().addChild(rotateBG);
        rotate.setSchedulingBounds(infiniteBounds);

        JMouseZoom zoom = new JMouseZoom(canvas, MouseZoom.INVERT_INPUT);
        zoom.setTransformGroup(u.getViewingPlatform().getMultiTransformGroup().getTransformGroup(1));
        zoom.setSchedulingBounds(infiniteBounds);
        zoom.setFactor(0.7);    // default 0.4
        BranchGroup zoomBG = new BranchGroup();
        zoomBG.addChild(zoom);
        u.getViewingPlatform().addChild(zoomBG);

        JMouseTranslate translate = new JMouseTranslate(canvas, MouseTranslate.INVERT_INPUT);
        translate.setTransformGroup(u.getViewingPlatform().getMultiTransformGroup().getTransformGroup(2));
        translate.setSchedulingBounds(infiniteBounds);
        translate.setFactor(0.01 * (highHeight-lowHeight)); // default 0.02
        BranchGroup translateBG = new BranchGroup();
        translateBG.addChild(translate);
        u.getViewingPlatform().addChild(translateBG);
//
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
        zoom.setZoom(0.5);
        rotate.setRotation(1.57, 1);

        translate.setView(-10, -50);
        J3DUtils.setViewPoint(u, canvas, objRoot, new Rectangle2D.Double(0, 0, 100, 100));
    }

    /**
     * To highlight layer according to list on the left
     * @param layer
     */
    public void showLayer(Layer layer)
    {
        Shape3D shape;

        if (currentLayerSelected != null)
        {
            // For this shape, its appareance has to be set back to normal
            shape = (Shape3D)layerPolyhedra.get(currentLayerSelected);
            shape.setAppearance((JAppearance)currentLayerSelected.getGraphics().get3DAppearance());
        }
        shape = (Shape3D)layerPolyhedra.get(layer);
        shape.setAppearance(JAppearance.highligtAp);
        currentLayerSelected = layer;
    }

    public void updateZValues(Layer layer, double thickness, double distance)
    {
        Shape3D shape = (Shape3D)layerPolyhedra.get(layer);

        J3DUtils.updateZValues(shape, distance, distance+thickness);
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
                JAppearance app = (JAppearance)s.getAppearance();
                if (app != JAppearance.highligtAp)
                {
                    Layer layer = app.getGraphics().getLayer();
                    parentDialog.threeDLayerList.setSelectedValue(layer.getName(), false);
                    showLayer(layer);
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