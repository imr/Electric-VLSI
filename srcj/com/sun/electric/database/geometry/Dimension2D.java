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

	/**
	 * Method to return the Y size of this Dimension2D.
	 * @return the Y size of this Dimension2D.
	 */
    public abstract double getHeight();

	/**
	 * Method to return the X size of this Dimension2D.
	 * @return the X size of this Dimension2D.
	 */
    public abstract double getWidth();

	/**
	 * Method to set the size of this Dimension2D.
	 * param d the new size of this Dimension2D.
	 */
    public abstract void setSize(java.awt.geom.Dimension2D d);

	/**
	 * Method to set the size of this Dimension2D.
	 * param width the new width of this Dimension2D.
	 * param height the new height of this Dimension2D.
	 */
    public abstract void setSize(double width, double height);

    public static class Double extends Dimension2D {
        private double width;
        private double height;

		/**
		 * Constructor to build a Dimension2D.Double with size 0x0.
		 */
        public Double() { width = 0; height = 0; }

		/**
		 * Constructor to build a Dimension2D.Double.
		 * @param d the size of this Dimension2D.Double.
		 */
        public Double(Dimension2D d) { setSize(d); }

		/**
		 * Constructor to build a Dimension2D.Double.
		 * @param width the width of this Dimension2D.Double.
		 * @param height the height of this Dimension2D.Double.
		 */
        public Double(double width, double height) { setSize(width, height); }

		/**
		 * Method to return the Y size of this Dimension2D.Double.
		 * @return the Y size of this Dimension2D.Double.
		 */
        public double getHeight() { return height; }

		/**
		 * Method to return the X size of this Dimension2D.Double.
		 * @return the X size of this Dimension2D.Double.
		 */
		public double getWidth() { return width; }

		/**
		 * Method to set the size of this Dimension2D.Double.
		 * param d the new size of this Dimension2D.Double.
		 */
        public void setSize(java.awt.geom.Dimension2D d) {
            width = d.getWidth(); height = d.getHeight();
        }

		/**
		 * Method to set the size of this Dimension2D.Double.
		 * param width the new width of this Dimension2D.Double.
		 * param height the new height of this Dimension2D.Double.
		 */
        public void setSize(double width, double height) {
            this.width = width; this.height = height;
        }

		/**
		 * Returns a printable version of this Dimension2D.Double.
		 * @return a printable version of this Dimension2D.Double.
		 */
        public String toString() { return "("+width+","+height+")"; }
    }

}
