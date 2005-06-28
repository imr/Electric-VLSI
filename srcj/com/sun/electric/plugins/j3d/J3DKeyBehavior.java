/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: J3DKeyBehavior.java
 * Written by Gilda Garreton, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

import java.awt.AWTEvent;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Arrays;

import javax.media.j3d.*;
import javax.vecmath.*;

/** Inspired in example found in Daniel Selman's book "Java 3D Programming"
 * For more information about the original example, contact Daniel Selman: daniel@selman.org
 * Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * @author  Gilda Garreton
 * @version 0.1
*/

public class J3DKeyBehavior extends Behavior
{
	protected static final double FAST_SPEED = 10.0;
	protected static final double NORMAL_SPEED = 1.0;

	protected TransformGroup tGroup; /* Contains main scene*/
	protected Transform3D transform;
	protected WakeupCondition keyCriterion;

	private double delta = 5;
	private double rotateAmount = Math.PI / 10.0;
	private double speed = NORMAL_SPEED;

	private int forwardKey = KeyEvent.VK_UP;
	private int backKey = KeyEvent.VK_DOWN;
	private int leftKey = KeyEvent.VK_LEFT;
	private int rightKey = KeyEvent.VK_RIGHT;

	public J3DKeyBehavior(TransformGroup tg)
	{
		super();

		tGroup = tg;
		transform = new Transform3D();
	}

	public void initialize()
	{
		WakeupCriterion[] keyEvents = new WakeupCriterion[2];
		keyEvents[0] = new WakeupOnAWTEvent(KeyEvent.KEY_PRESSED);
		keyEvents[1] = new WakeupOnAWTEvent(KeyEvent.KEY_RELEASED);
		keyCriterion = new WakeupOr(keyEvents);

		wakeupOn(keyCriterion);
	}

	public void processStimulus(Enumeration criteria)
	{
		WakeupCriterion wakeup;
		AWTEvent[] event;

		while(criteria.hasMoreElements( ))
		{
			wakeup = (WakeupCriterion) criteria.nextElement( );

			if( !(wakeup instanceof WakeupOnAWTEvent) )	
				continue;

			event = ((WakeupOnAWTEvent)wakeup).getAWTEvent( );

			for( int i = 0; i < event.length; i++ )
			{
				if( event[i].getID( ) == KeyEvent.KEY_PRESSED )
					processKeyEvent((KeyEvent)event[i]);
			}
		}

		wakeupOn( keyCriterion );
	}

	protected void processKeyEvent(KeyEvent event)
	{
		int keycode = event.getKeyCode();

		if(event.isShiftDown())
			speed = FAST_SPEED;
		else 
			speed = NORMAL_SPEED;

		if( event.isAltDown())
			altMove(keycode);
		else if(event.isControlDown())
			controlMove(keycode );
		else
			standardMove(keycode);
	}

	//moves forward backward or rotates left right
	private void standardMove(int keycode)
	{
		if(keycode == forwardKey)
			moveAlongAxis(Z, 1);
		else if(keycode == backKey)
			moveAlongAxis(Z, -1);
		else if(keycode == leftKey)
			rotateAlongAxis(Y, 1);
		else if(keycode == rightKey)
			rotateAlongAxis(Y, -1);
	}

	//moves left right, rotate up down
	protected void altMove(int keycode)
	{
		if(keycode == forwardKey)
			rotateAlongAxis(X, 1);
		else if(keycode == backKey)
			rotateAlongAxis(X, -1);
		else if(keycode == leftKey)
			moveAlongAxis(X, -1);
		else if(keycode == rightKey)
			moveAlongAxis(X, 1);
	}

	//move up down, rot left right
	protected void controlMove(int keycode)
	{
		if(keycode == forwardKey)	
			moveAlongAxis(Y, 1);
		else if(keycode == backKey)
			moveAlongAxis(Y, -1);
		else if(keycode == leftKey)
			rotateAlongAxis(Z, 1);
		else if(keycode == rightKey)
			rotateAlongAxis(Z, -1);
	}

    private static double[] values = new double[3];
    private static Vector3d move = new Vector3d();

    /**
     * Method to shift along axis in direction provided
     * @param axis
     * @param dir
     */
	public void moveAlongAxis(int axis, int dir)
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

    /**
     * Method to rotate along given axis and direction provided
     * @param axis
     * @param dir
     */
	protected void rotateAlongAxis(int axis, int dir)
	{
        double radian = rotateAmount * speed;
        // in case of collision, move the opposite direction
		if (!rotate(axis, dir * radian, false))
           rotate(axis, -dir * radian, true);
	}

	protected boolean updateTransform(boolean force)
	{
		tGroup.setTransform(transform);
        return true;
	}

    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;

    /**
     * Method to set the original rotation
     * @param rotVals
     */
    public void setHomeRotation(double[] rotVals)
    {
        for (int i = 0; i < rotVals.length; i++)
            rotate(i, rotVals[i], true);
    }

    /**
     * Method that rotates along given axis
     * @param axis
     * @param radians
     * @return True if there was no collision
     */
	protected boolean rotate(int axis, double radians, boolean force)
	{
		tGroup.getTransform(transform);
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
		transform.mul(toMove);
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
        tGroup.getTransform(transform);
        Matrix4d mat = new Matrix4d();
        transform.get(mat);
        double dy = transform.getScale() * factor;
        transform.setScale(dy);
        tGroup.setTransform(transform);
    }

	private static Transform3D toMove = new Transform3D();
    private boolean doMove(Vector3d theMove, boolean force)
	{
		tGroup.getTransform(transform);
        toMove.setIdentity();
		toMove.setTranslation(theMove);
		transform.mul(toMove);
		return updateTransform(force);
	}

	protected double getMovementRate()
	{
		return delta * speed;
	}
}
