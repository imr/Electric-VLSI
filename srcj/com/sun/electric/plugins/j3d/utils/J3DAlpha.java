/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: J3DAlpha.java
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
package com.sun.electric.plugins.j3d.utils;

import javax.media.j3d.Alpha;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.*;

/**
 * Alpha class to control motion with JSlider class
 * Idea taken from Selman's book
 * @author  Gilda Garreton
 * @version 0.1
 */
public class J3DAlpha extends Alpha implements ChangeListener
{
    private Alpha alpha;
    private float manualValue = 0.5f; // in the middle
    private boolean autoMode = true;
    private int speed;  // in miliseconds

    public J3DAlpha(int speed, boolean mode, float value)
    {
        this.speed = speed;
        this.autoMode = mode;
        this.manualValue = value;
        setAlphaSpeed(speed);
    }

    public void setAlphaSpeed(int speed)
    {
        // previous value is OK
        if (alpha != null && this.speed == speed) return;
        alpha = new Alpha (-1, Alpha.INCREASING_ENABLE | Alpha.DECREASING_ENABLE,
                    0,
                    0,
                    (long)(2.5*speed), // 2500
                    (long)(0.4*speed),  // 400
                    (long)(0.1*speed),  // 100
                    (long)(2*speed),  //2000
                    (long)(5*speed), // 5000
                    (long)(0.05*speed)); // 50
    }

    /**
     * Set AutoMode flag according to boolean
     * @param mode True if interpolator runs automatically
     */
    public void setAutoMode(boolean mode) {autoMode = mode;}

    /**
     * Method to retrieve auto mode flag
     * @return
     */
    public boolean getAutoMode() {return autoMode;}

    /**
     * Overwrites original Alpha's value function
     * to consider manual mode
     * @param time
     * @return
     */
    public float value(long time)
    {
        if (autoMode) return alpha.value(time);
        return manualValue;
    }

    /**
     *
     * @param e
     */
    public void stateChanged( ChangeEvent e )
	{
		if( e.getSource( ) instanceof JSlider )
		{
			manualValue =  ((JSlider) e.getSource( )).getValue( ) / 100.0f;
		}
	}
}
