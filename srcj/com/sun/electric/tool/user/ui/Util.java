/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Util.java
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.user.ui;

import java.awt.*;
import javax.swing.*;
import java.util.*;

/**
 * Utility functions for the user interface
 */
public class Util {

    //
    // AWT doesn't include HSV<->RGB colorspace conversions.  Nuts, huh?
    //
    public static Color colorFromHSV(float h, float s, float v) {
        float r=0, g=0, b=0;
        float var_h = h * 6;
        float var_i = (float)Math.floor( var_h );
        float var_1 = v * ( 1 - s );
        float var_2 = v * ( 1 - s * ( var_h - var_i ) );
        float var_3 = v * ( 1 - s * ( 1 - ( var_h - var_i ) ) );
        if      ( var_i == 0 ) { r = v     ; g = var_3 ; b = var_1; }
        else if ( var_i == 1 ) { r = var_2 ; g = v     ; b = var_1; }
        else if ( var_i == 2 ) { r = var_1 ; g = v     ; b = var_3; }
        else if ( var_i == 3 ) { r = var_1 ; g = var_2 ; b = v;     }
        else if ( var_i == 4 ) { r = var_3 ; g = var_1 ; b = v;     }
        else                   { r = v     ; g = var_1 ; b = var_2; }
        return new Color(r, g, b);
    }

    public static float hueFromColor(Color c) {
        double r = c.getRed() / 255.;
        double g = c.getGreen() / 255.;
        double b = c.getBlue() / 255.;
        double min, max;
        min = Math.min(r, Math.min(g, b));
        max = Math.max(r, Math.max(g, b));
        double v = max;
        double h = 0;
        if (v == 0) return (float)h;
        r /= v;
        g /= v;
        b /= v;
        min = Math.min(r, Math.min(g, b));
        max = Math.max(r, Math.max(g, b));
        double s = max - min;
        if (s == 0) { h = 0; return (float)h; }
        r = (r - min)/s;
        g = (g - min)/s;
        b = (b - min)/s;
        min = Math.min(r, Math.min(g, b));
        max = Math.max(r, Math.max(g, b));
        if (max == r) {
            h = 0.0 + (60./360.)*(g - b);
            if (h < 0.0) h += 1;
        } else if (max == g) {
            h = (120./360.) + (60./360.)*(b - r);
        } else /* max == b */ {
            h = (240./360.) + (60./360.)*(r - g);
        }
        return (float)h;
    }

}
