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
import com.sun.j3d.utils.universe.*;
import javax.media.j3d.*;
import javax.vecmath.*;

public class J3DKeyBehavior extends Behavior
{
	protected static final double FAST_SPEED = 20.0;
	protected static final double NORMAL_SPEED = 1.0;
	protected static final double SLOW_SPEED = 0.5;

	protected TransformGroup transformGroup; /* Contains main scene*/
    private BranchGroup axes; /* Contains the 3 axes, they should only be rotated */
	protected Transform3D transform3D;
	protected WakeupCondition keyCriterion;

	private final static double TWO_PI = (2.0 * Math.PI);
	private double rotateXAmount = Math.PI / 16.0;
	private double rotateYAmount = Math.PI / 16.0;
	private double rotateZAmount = Math.PI / 16.0;

	private double moveRate = 5;
	private double speed = NORMAL_SPEED;

	private final double kMoveForwardScale = 1.1;
	private final double kMoveBackwardScale = 0.9;

	private int forwardKey = KeyEvent.VK_UP;
	private int backKey = KeyEvent.VK_DOWN;
	private int leftKey = KeyEvent.VK_LEFT;
	private int rightKey = KeyEvent.VK_RIGHT;

	public J3DKeyBehavior(TransformGroup tg, BranchGroup axes)
	{
		super();

		transformGroup = tg;
        this.axes = axes;
		transform3D = new Transform3D( );
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
			moveAlongZ(1);
		else if(keycode == backKey)
			moveAlongZ(-1);
		else if(keycode == leftKey)
			rotAlongY(1);
		else if(keycode == rightKey)
			rotAlongY(-1);
	}


	//moves left right, rotate up down
	protected void altMove( int keycode )
	{
		if(keycode == forwardKey)
			rotAlongX(1);
		else if(keycode == backKey)
			rotAlongX(-1);
		else if(keycode == leftKey)
			moveAlongX(-1);
		else if(keycode == rightKey)
			moveAlongX(1);
	}

	//move up down, rot left right
	protected void controlMove( int keycode )
	{
		if(keycode == forwardKey)	
			moveAlongY(1);
		else if(keycode == backKey)
			moveAlongY(-1);
		else if(keycode == leftKey)
			rotAlongZ(1);
		else if(keycode == rightKey)
			rotAlongZ(-1);
	}

	private void moveAlongZ(int dir)
	{
		doMove( new Vector3d( 0.0,0.0, dir * kMoveForwardScale * speed ) );
	}

    private void moveAlongX(int dir)
	{
		doMove( new Vector3d( dir * getMovementRate( ) ,0.0,0.0 ) );
	}

    private void moveAlongY(int dir)
	{
		doMove( new Vector3d( 0.0, dir * getMovementRate( ) ,0.0 ) );
	}

	protected void rotAlongX(int dir)
	{
		doRotateX(dir * rotateXAmount * speed);
	}

	protected void rotAlongY(int dir)
	{
		doRotateY(dir * rotateYAmount * speed);
	}

	protected void rotAlongZ(int dir)
	{
		doRotateZ(dir * rotateZAmount * speed);
	}

	protected void updateTransform( )
	{
		transformGroup.setTransform( transform3D );
	}

    /**
     * Method to rotate always the axes
     */
    private void rotateAxes(Transform3D toMove)
    {
        for (Enumeration e = axes.getAllChildren(); e.hasMoreElements(); )
        {
            TransformGroup axisTrans = (TransformGroup)e.nextElement();
            Transform3D tmp = new Transform3D( );
            axisTrans.getTransform(tmp);
            tmp.mul(toMove);
            axisTrans.setTransform(tmp);
        }
    }

	protected void doRotateY( double radians )
	{
		transformGroup.getTransform( transform3D );
		Transform3D toMove = new Transform3D( );
		toMove.rotY( radians );
		transform3D.mul( toMove );
		updateTransform( );
        rotateAxes(toMove);
	}

	protected void doRotateX( double radians )
	{
		transformGroup.getTransform( transform3D );
		Transform3D toMove = new Transform3D( );
		toMove.rotX( radians );
		transform3D.mul( toMove );
		updateTransform( );
        rotateAxes(toMove);
	}

	protected void doRotateZ( double radians )
	{
		transformGroup.getTransform( transform3D );
		Transform3D toMove = new Transform3D( );
		toMove.rotZ( radians );
		transform3D.mul( toMove );
		updateTransform( );
        rotateAxes(toMove);
	}

	protected void doMove( Vector3d theMove )
	{
		transformGroup.getTransform( transform3D );
		Transform3D toMove = new Transform3D( );
		toMove.setTranslation( theMove );
		transform3D.mul( toMove );
		updateTransform( );
	}

	protected double getMovementRate( )
	{
		return moveRate * speed;
	}

	public void setRotateXAmount( double radians )
	{
		rotateXAmount = radians;
	}

	public void setRotateYAmount( double radians )
	{
		rotateYAmount = radians;
	}

	public void setRotateZAmount( double radians )
	{
		rotateZAmount = radians;
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
