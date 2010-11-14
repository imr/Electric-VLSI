/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TextUtils.java
 *
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.plugins.irsim;

/**
 * Collects Electric dependencies in IRSIM code.
 */
class Electric {

    /**
     * Class to define an Double-like object that can be modified.
     */
    static class MutableDouble
    {
        private double value;

        /**
         * Constructor creates a MutableDouble object with an initial value.
         * @param value the initial value.
         */
        public MutableDouble(double value) { this.value = value; }

        /**
         * Method to change the value of this MutableDouble.
         * @param value the new value.
         */
        public void setValue(double value) { this.value = value; }

        /**
         * Method to return the value of this MutableDouble.
         * @return the current value of this MutableDouble.
         */
        public double doubleValue() { return value; }

        /**
         * Returns a printable version of this MutableDouble.
         * @return a printable version of this MutableDouble.
         */
        @Override
        public String toString() { return Double.toString(value); }
    }
    
    /**
     * Small epsilon value.
     * set so that 1+DBL_EPSILON != 1
     */	private static double DBL_EPSILON = 2.2204460492503131e-016;

    /**
     * Method to compare two double-precision numbers within an acceptable epsilon.
     * @param a the first number.
     * @param b the second number.
     * @return true if the numbers are equal to 16 decimal places.
     */
    static boolean doublesEqual(double a, double b)
    {
        return doublesEqual(a, b, DBL_EPSILON);
    }

    /**
     * Method to compare two double-precision numbers within a given epsilon
     * @param a the first number.
     * @param b the second number.
     * @param myEpsilon the given epsilon
     * @return true if the values are close.
     */
    static boolean doublesEqual(double a, double b, double myEpsilon)
    {
        return (Math.abs(a-b) <= myEpsilon);
    }

	/**
	 * Method to compare two numbers and see if one is less than the other within an acceptable epsilon.
	 * @param a the first number.
     * @param b the second number.
     * @return true if "a" is less than "b" to 16 decimal places.
	 */
    static boolean doublesLessThan(double a, double b)
	{
		if (a+DBL_EPSILON < b) return true;
		return false;
	}
}
