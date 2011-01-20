/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MinAreaChecker.java
 *
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.api.minarea;

import java.util.Properties;

/**
 *
 */
public interface MinAreaChecker {

    /**
     * 
     * @return the algorithm name
     */
    public String getAlgorithmName();

    /**
     * 
     * @return the names and default values of algorithm parameters
     */
    public Properties getDefaultParameters();

    /**
     * @param topCell top cell of the layout
     * @param minArea minimal area of valid polygon
     * @param parameters algorithm parameters
     * @param errorLogger an API to report violations
     */
    public void check(LayoutCell topCell, long minArea, Properties parameters, ErrorLogger errorLogger);

    /**
     * A class to report Min Area violations.
     */
    public interface ErrorLogger {

        /**
         * The algorithm uses this method to report about polygon that violates
         * min area rule. The algorithm report actual area of the polygon and
         * vertex with lexigraphically maximal coordinates (x,y).
         * This means that rightmost vertical edges of polygon are choosen,
         * and than the most upper vertex on these edges is choosen.
         * Formally, such point (x,y) is reported that for any other point (x',y') of
         * this polygin:  (x' < x || x' == x && y' < y)
         * @param area the area of violating polygon
         * @param x x-coordinate of lexigraphically largest point of violating polygon
         * @param y y-coordinate of lexigraphically largest point of violating polygon
         */
        public void reportMinAreaViolation(long area, int x, int y);
        
        /**
         * 
         */
        public void printReports();
    }
}
