/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OutputGeometry.java
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
package com.sun.electric.tool.io;

import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.NodeUsage;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.geom.AffineTransform;

/**
 * Base class for writing geometry to a file
 */
public abstract class OutputGeometry extends Output {
    
    /** number of unique cells processed */             protected int numVisited;
    /** number of unique cells to process */            protected int numCells;

    /** HashMap of all CellGeoms */                     protected HashMap cellGeoms;
    
    /** Creates a new instance of OutputGeometry */
    OutputGeometry() 
    {
    }

    /** 
     * Write cell to file
     * @return true on error
     */
    public boolean writeCell(Cell cell) 
    {
        // see how many cells we have to write, for progress indication
        numCells = HierarchyEnumerator.getNumUniqueChildCells(cell) + 1;
        numVisited = 0;
        cellGeoms = new HashMap();
        
        // write out cells
        start();
        HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, null, new Visitor(this, getMaxHierDepth(cell)));
        done();
        return false;
    }
        
        
    /** Abstract method called before hierarchy traversal */
    protected abstract void start();
    
    /** Abstract method called after traversal */
    protected abstract void done();
    
    /** Abstract method to write CellGeom to disk */
    protected abstract void writeCellGeom(CellGeom cellGeom);
    
    /** Overridable method to determine whether or not to merge geometry */
    protected boolean mergeGeom(int hierLevelsFromBottom)
    {
        return false;
    }
    
    
    /**
     * Class to store polygon geometry of a cell
     */
    protected class CellGeom
    {
        /** HashMap of Poly(gons) in this Cell, keyed by Layer, all polys per layer stored as a List */
                                            protected HashMap polyMap;
        /** Nodables in this Cell */        protected ArrayList nodables;
        /** Cell */                         protected Cell cell;
        
        /** Constructor */
        protected CellGeom()
        {
            polyMap = new HashMap();
            nodables = new ArrayList();            
        }
        
        /** add polys to cell geometry */
        protected void addPolys(Poly[] polys)
        {
            for (int i=0; i<polys.length; i++) {
                ArrayList list = (ArrayList)polyMap.get(polys[i].getLayer());
                if (list == null) {
                    list = new ArrayList(); 
                    polyMap.put(polys[i].getLayer(), list);
                }
                list.add(polys[i]);
            }
        }
    }    
    
    //------------------HierarchyEnumerator.Visitor Implementation----------------------
    
    protected class Visitor extends HierarchyEnumerator.Visitor
    {
        /** OutputGeometry object this Visitor is enumerating for */    private OutputGeometry outGeom;
        /** Current cellGeom */                                         private CellGeom cellGeom = null;
        /** hierarchy max depth */                                      private int maxHierDepth;
        /** current hierarchy depth */                                  private int curHierDepth;
        
        public Visitor(OutputGeometry outGeom, int maxHierDepth)
        {
            this.outGeom = outGeom;
            this.maxHierDepth = maxHierDepth;
            curHierDepth = 0;
        }
        
        public boolean enterCell(HierarchyEnumerator.CellInfo info) 
        {
            if (cellGeoms.containsKey(info.getCell())) return false;    // already processed this Cell
            cellGeom = new CellGeom();
            cellGeom.cell = info.getCell();
            cellGeoms.put(info.getCell(), cellGeom);
            curHierDepth++;
            return true;
        }
    
        public void exitCell(HierarchyEnumerator.CellInfo info) 
        {
            // add arcs to cellGeom
    		for (Iterator it = info.getCell().getArcs(); it.hasNext();) {
        		ArcInst ai = (ArcInst) it.next();
        		ArcProto ap = ai.getProto();
                Technology tech = ap.getTechnology();
        		Poly [] polys = tech.getShapeOfArc(ai, null);
                cellGeom.addPolys(polys);
            }
            
            if (outGeom.mergeGeom(maxHierDepth - curHierDepth)) {
                // merging takes place here
            }           
            
            // write cell
            outGeom.writeCellGeom(cellGeom);
            
            curHierDepth--;
            cellGeom = null;
        }
    
        public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) 
        {
            NodeProto np = no.getProto();
            if (np instanceof PrimitiveNode) {
    			NodeInst ni = (NodeInst)no;
    			// don't copy Facet-Centers
    			if (np.getProtoName().equals("Facet-Center")) return false;
                AffineTransform trans = ni.rotateOut();
				PrimitiveNode prim = (PrimitiveNode)np;
				Technology tech = prim.getTechnology();
				Poly [] polys = tech.getShapeOfNode(ni, null);
                for (int i=0; i<polys.length; i++)
                    polys[i].transform(trans);
                cellGeom.addPolys(polys);
                
                return false;
    		}
            // else just a cell
            cellGeom.nodables.add(no);
            return true;
        }
    
    }
    
    //----------------------------Utility Methods--------------------------------------
    
    /** get the max hierarchical depth of the hierarchy */
    public static int getMaxHierDepth(Cell cell)
    {
        return hierCellsRecurse(cell, 0, 0);
    }
    
    /** Recursive method used to traverse down hierarchy */
    private static int hierCellsRecurse(Cell cell, int depth, int maxDepth)
    {
        if (depth > maxDepth) maxDepth = depth;
        for (Iterator uit = cell.getUsagesIn(); uit.hasNext();)
        {
            NodeUsage nu = (NodeUsage) uit.next();
            if (nu.isIcon()) continue;
            NodeProto np = nu.getProto();
            if (!(np instanceof Cell)) continue;
            maxDepth = hierCellsRecurse((Cell)np, depth+1, maxDepth);
        }
        return maxDepth;
    }
    
}
