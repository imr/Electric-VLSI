/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ElecRouteElementArc.java
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

package com.sun.electric.tool.routing;

import com.sun.electric.database.prototype.ArcProto;

/**
 * Specifies an electric connection (an arc) between two ElecRouteElementPorts
 */
public class ElecRouteElementArc {

    /** the arc to use */                   private ArcProto arcProto;
    /** head of arc */                      private ElecRouteElementPort headRE;
    /** tail of arc */                      private ElecRouteElementPort tailRE;

    public ElecRouteElementArc(ArcProto arcProto, ElecRouteElementPort headRE,
                               ElecRouteElementPort tailRE) {
        this.arcProto = arcProto;
        this.headRE = headRE;
        this.tailRE = tailRE;
    }

    public ArcProto getArcProto() { return arcProto; }

    public ElecRouteElementPort getHead() { return headRE; }

    public ElecRouteElementPort getTail() { return tailRE; }

}
