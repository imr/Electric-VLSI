/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Dimension2D.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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

package com.sun.electric.database.geometry;

/**
 * Class to define a Double-precision Dimension object.
 */
public abstract class Dimension2D extends java.awt.geom.Dimension2D {

    public abstract double getHeight();
    public abstract double getWidth();
    public abstract void setSize(java.awt.geom.Dimension2D d);
    public abstract void setSize(double width, double height);

    public static class Double extends Dimension2D {
        private double width;
        private double height;

        public Double() { width = 0; height = 0; }

        public Double(Dimension2D d) { setSize(d); }

        public Double(double width, double height) { setSize(width, height); }

        public double getHeight() { return height; }
        public double getWidth() { return width; }
        public void setSize(java.awt.geom.Dimension2D d) {
            width = d.getWidth(); height = d.getHeight();
        }
        public void setSize(double width, double height) {
            this.width = width; this.height = height;
        }

        public String toString() { return "("+width+","+height+")"; }
    }

}
