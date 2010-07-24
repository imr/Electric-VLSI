/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ConnectionPoints.java
 * Written by: Andreas Uebelhoer, Alexander Bieles, Emre Selegin (Team 6)
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.routing.experimentalLeeMoore1;

import java.util.ArrayList;
import java.util.List;

import com.sun.electric.tool.routing.experimentalLeeMoore1.LeeMoore.Tupel;

public class ConnectionPoints {
    private Tupel innerPoint = null;
    private Tupel outerPoint = null;
    private boolean areValid = false;

    /**
     * when the constructor is called without tuples than we have notValid connectionPoints
     */
    public ConnectionPoints() {
    }

    /**
     * when the constructor is called with two tuples than we have Valid connectionPoints
     */
    public ConnectionPoints(Tupel inside, Tupel outside){
        this.innerPoint = inside;
        this.outerPoint = outside;
        this.areValid = true;
    }

    /**
     *
     * @return returs if the connectionPoints are valid or not
     */
    public boolean areValid() {
        return areValid;
    }

    /**
     *
     * @return returns the tupel which was inside the original region
     */
    public Tupel getInnerPoint() {
        return innerPoint;
    }

    /**
     *
     * @return returns the tupel which was outside the original region (the neighbor region)
     */
    public Tupel getOuterPoint() {
        return outerPoint;
    }

    /**
     * 
     * @return return an array made of the two connecting points
     */
	public Tupel[] getTupels() {
		Tupel[] result={innerPoint,outerPoint};
		return result;
	}
	
	public List<Tupel> getTupelsAsList(){
		List<Tupel> ret=new ArrayList<Tupel>();
		ret.add(innerPoint);
		ret.add(outerPoint);
		return ret;
	}
}
