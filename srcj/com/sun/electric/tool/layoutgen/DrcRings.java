/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DrcRings.java
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
package com.sun.electric.tool.layoutgen;

import java.awt.geom.Rectangle2D;
import java.util.*;

import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.network.*;
import com.sun.electric.technology.*;

import com.sun.electric.tool.layoutgen.gategen.*;

public class DrcRings {
  public static class Filter {
    public boolean skip(NodeInst ni) {return false;}
  }

  public static void addDrcRings(Cell gallery, Filter filter) {
    if (filter==null) filter = new Filter();

    Library lib = gallery.getLibrary();
    StdCellParams stdCell = new StdCellParams(lib);

    // record original gates to avoid putting DrcRings around DrcRings
    ArrayList gates = new ArrayList();
    for (Iterator it=gallery.getNodes(); it.hasNext();)  gates.add(it.next());

    // place a DrcRing around each instance
    for (int i=0; i<gates.size(); i++) {
      NodeInst ni = (NodeInst) gates.get(i);

      // skip things user doesn't want ring around
      if (filter.skip(ni)) continue;

      // only do Cells
      if (ni.getProto() instanceof PrimitiveNode)  continue;
      
      Rectangle2D r = ni.getBounds();

      double loX = r.getX();
      double loY = r.getY();
      double w = r.getWidth() + 3;
      double h = r.getHeight() + 3;
      Cell f = DrcRing.makePart(w, h, stdCell);
      
      // DrcRing reference point is the bottom left inside corner
      double defSz = LayoutLib.DEF_SIZE;
      LayoutLib.newNodeInst(f, defSz, defSz, loX-1.5, loY-1.5, 0, gallery);
      /*
      double ctrX = r.getCenterX();
      double ctrY = r.getCenterY();
      double w = r.getWidth() + 3;
      double h = r.getHeight() + 3;
      Cell f = DrcRing.makePart(w, h, stdCell);
      
      f.newInst(1, 1, ctrX, ctrY, 0, gallery);
      */
    }
  }
}
