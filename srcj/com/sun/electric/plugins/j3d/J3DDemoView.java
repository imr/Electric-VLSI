package com.sun.electric.plugins.j3d;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.plugins.j3d.utils.J3DUtils;

import javax.media.j3d.Transform3D;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.BoundingSphere;
import javax.vecmath.Vector3d;
import javax.vecmath.Point2d;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda
 * Date: Mar 1, 2005
 * Time: 1:25:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class J3DDemoView extends View3DWindow
        implements J3DCollisionDetector
{
    private Vector3d tmpTrans = new Vector3d();
    private Vector3d mapSize = null;

    public J3DDemoView(Cell cell, WindowFrame wf, WindowContent view2D, boolean transPerNode)
    {
        super(cell, wf, view2D, transPerNode);
    }

    protected BranchGroup createSceneGraph(Cell cell)
    {
        BranchGroup scene = super.createSceneGraph(cell);

        // create the KeyBehavior and attach
		J3DKeyCollision keyBehavior = new J3DKeyCollision(objTrans, null, this);
		keyBehavior.setSchedulingBounds(J3DUtils.infiniteBounds);
		//keyBehavior.setMovementRate( 0.7 );
//        BranchGroup behaviorBranch = new BranchGroup();
//        behaviorBranch.addChild(keyBehavior);
//        objTrans.addChild(behaviorBranch);
		objTrans.addChild(keyBehavior);

        return scene;
    }

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

        return (false);
//		int color = m_MapImage.getRGB( (int) point.x, (int) point.y );
//
//		// we can't walk through walls or bookcases
//		return( color == m_ColorWall || color == m_ColorBookcase );
	}
}
