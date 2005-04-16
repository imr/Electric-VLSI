/**********************************************************
  Copyright (C) 2001 	Daniel Selman

  First distributed with the book "Java 3D Programming"
  by Daniel Selman and published by Manning Publications.
  http://manning.com/selman

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation, version 2.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  The license can be found on the WWW at:
  http://www.fsf.org/copyleft/gpl.html

  Or by writing to:
  Free Software Foundation, Inc.,
  59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

  Authors can be contacted at:
   Daniel Selman: daniel@selman.org
   Andrew AJ Cain acain@it.swin.edu.au
   Gary S. Moss moss@arl.mil

  If you make changes you think others would like, please 
  contact one of the authors or someone at the 
  www.j3d.org web site.
**************************************************************/

package com.sun.electric.plugins.j3d;

import java.awt.AWTEvent;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Arrays;

import javax.media.j3d.*;
import javax.vecmath.*;

public class J3DKeyBehavior extends Behavior
{
	protected static final double FAST_SPEED = 20.0;
	protected static final double NORMAL_SPEED = 1.0;
	protected static final double SLOW_SPEED = 0.5;

	protected TransformGroup transformGroup; /* Contains main scene*/
	protected Transform3D transform3D;
	protected WakeupCondition keyCriterion;

	private double rotateAmount = Math.PI / 16.0;
	private double moveRate = 5;
	private double speed = NORMAL_SPEED;

	private int forwardKey = KeyEvent.VK_UP;
	private int backKey = KeyEvent.VK_DOWN;
	private int leftKey = KeyEvent.VK_LEFT;
	private int rightKey = KeyEvent.VK_RIGHT;

	public J3DKeyBehavior(TransformGroup tg)
	{
		super();

		transformGroup = tg;
		transform3D = new Transform3D();
	}

	public void initialize( )
	{
		WakeupCriterion[] keyEvents = new WakeupCriterion[2];
		keyEvents[0] = new WakeupOnAWTEvent( KeyEvent.KEY_PRESSED );
		keyEvents[1] = new WakeupOnAWTEvent( KeyEvent.KEY_RELEASED );
		keyCriterion = new WakeupOr( keyEvents );

		wakeupOn( keyCriterion );
	}

	public void processStimulus( Enumeration criteria )
	{
		WakeupCriterion wakeup;
		AWTEvent[] event;

		while( criteria.hasMoreElements( ) )
		{
			wakeup = (WakeupCriterion) criteria.nextElement( );

			if( !(wakeup instanceof WakeupOnAWTEvent) )	
				continue;

			event = ((WakeupOnAWTEvent)wakeup).getAWTEvent( );

			for( int i = 0; i < event.length; i++ )
			{
				if( event[i].getID( ) == KeyEvent.KEY_PRESSED )
				{
					processKeyEvent( (KeyEvent)event[i] );
				}
			}
		}

		wakeupOn( keyCriterion );
	}

	protected void processKeyEvent( KeyEvent event )
	{
		int keycode = event.getKeyCode( );

		if(event.isShiftDown( )) 
			speed = FAST_SPEED;
		else 
			speed = NORMAL_SPEED;

		if( event.isAltDown( ) )
			altMove( keycode );
		else if( event.isControlDown( ))	
			controlMove( keycode );
		else
			standardMove( keycode );
	}

	//moves forward backward or rotates left right
	private void standardMove( int keycode )
	{
		if(keycode == forwardKey)
			moveAlong(Z, 1);
		else if(keycode == backKey)
			moveAlong(Z, -1);
		else if(keycode == leftKey)
			rotAlong(Y, 1);
		else if(keycode == rightKey)
			rotAlong(Y, -1);
	}

	//moves left right, rotate up down
	protected void altMove( int keycode )
	{
		if(keycode == forwardKey)
			rotAlong(X, 1);
		else if(keycode == backKey)
			rotAlong(X, -1);
		else if(keycode == leftKey)
			moveAlong(X, -1);
		else if(keycode == rightKey)
			moveAlong(X, 1);
	}

	//move up down, rot left right
	protected void controlMove( int keycode )
	{
		if(keycode == forwardKey)	
			moveAlong(Y, 1);
		else if(keycode == backKey)
			moveAlong(Y, -1);
		else if(keycode == leftKey)
			rotAlong(Z, 1);
		else if(keycode == rightKey)
			rotAlong(Z, -1);
	}

    private static double[] values = new double[3];
    private static Vector3d move = new Vector3d();

	public void moveAlong(int axis, int dir)
	{
        Arrays.fill(values, 0);
        values[axis] = dir * getMovementRate();
        move.set(values);
        // If move gets a collision, then move back
		if (!doMove(move, false))
        {
            values[axis] = -dir * getMovementRate();
            move.set(values);
            doMove(move, true);
        }
	}

	protected void rotAlong(int axis, int dir)
	{
        double radian = rotateAmount * speed;
        // in case of collision, move the opposite direction
		if (!doRotate(axis, dir * radian, false))
           doRotate(axis, -dir * radian, true);
	}

	protected boolean updateTransform(boolean force)
	{
		transformGroup.setTransform(transform3D);
        return true;
	}

    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;

    /**
     * Method that rotates along given axis
     * @param axis
     * @param radians
     * @return True if there was no collision
     */
	protected boolean doRotate(int axis, double radians, boolean force)
	{
		transformGroup.getTransform(transform3D);
		Transform3D toMove = new Transform3D();
        switch(axis)
        {
            case X:
                toMove.rotX(radians);
                break;
            case Y:
                toMove.rotY(radians);
                break;
            case Z:
                toMove.rotZ(radians);
                break;
        }
		transform3D.mul(toMove);
        // Need to move in opposite direction to avoid collision
        boolean noCollision = updateTransform(force);
        return (noCollision);
	}

    void zoomInOut(boolean out)
    {
        double z_factor = 0.7;//Math.abs(0.7);
        //double factor = (out) ? (0.5/z_factor) : (2*z_factor);
        double factor = (out) ? (1/z_factor) : (z_factor);

        // Remember old matrix
        transformGroup.getTransform(transform3D);
        Matrix4d mat = new Matrix4d();
        transform3D.get(mat);
        double dy = transform3D.getScale() * factor;
        transform3D.setScale(dy);
        transformGroup.setTransform(transform3D);
    }

	private static Transform3D toMove = new Transform3D();
    private boolean doMove(Vector3d theMove, boolean force)
	{
		transformGroup.getTransform(transform3D);
        toMove.setIdentity();
		toMove.setTranslation(theMove);
        //positionVector.add (theMove);
		transform3D.mul( toMove );
		return updateTransform(force);
	}

	protected double getMovementRate( )
	{
		return moveRate * speed;
	}

	public void setMovementRate( double meters )
	{
		moveRate = meters; // Travel rate in meters/frame
	}

	public void setForwardKey( int key )
	{
		forwardKey = key;
	}

	public void setBackKey( int key )
	{
		backKey = key;
	}

	public void setLeftKey( int key )
	{
		leftKey = key;
	}
}
