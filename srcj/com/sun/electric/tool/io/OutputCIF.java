/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OutputCIF.java
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

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.technology.Layer;

import java.util.Date;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


/** 
 * Class to write CIF output to disk
 */
public class OutputCIF extends OutputGeometry {
    
    // preferences
    /** minimum output grid resolution (lambda) */      private double minAllowedResolution = 0.5;
    /** instantiate top level */                        private boolean instTopLevel = true;
    /** scale factor from internal units to centi-microns */    private double scale = 10;
    
    // crc checksum stuff
    /** checksum */                                     private int crcChecksum;
    /** crc tab */                                      private int [] crcTab = new int[256];
    /** keep track of chars */                          private boolean crcPrevIsCharSep;
    /** num chars written */                            private int crcNumChars;
    /** MOSIS initial key */                            private static final int[] crcRow =
    {0x04C11DB7, 0x09823B6E, 0x130476DC, 0x2608EDB8, 0x4C11DB70, 0x9823B6E0, 0x34867077, 0x690CE0EE};
    /** ASCII (and UNICODE) space value */              private byte space = 0x20;
    
    // cif output data
    /** cell number */                                  private int cellNumber = 100;
    /** cell to cell number map */                      private HashMap cellNumbers;
    
    /** Creates a new instance of OutputCIF */
    OutputCIF() {
        cellNumbers = new HashMap();
    }
        
    protected void start() {
        Date now = new Date(System.currentTimeMillis());
        //writeLine("( Electric VLSI Design System, version "+")");
        //writeLine("( "+now+" )");
        
        // initialize crc checksum info
        for (int i=0; i<crcTab.length; i++) {
            crcTab[i] = 0;
            for (int j=0; j<8; j++) {
                if (((1 << j) & i) != 0)
                    crcTab[i] = crcTab[i] ^ crcRow[j];
            }
            crcTab[i] &= 0xFFFFFFFF; // unneccessary in java, int is always 32 bits
        }
        crcNumChars = 1;
        crcPrevIsCharSep = true;
        crcChecksum = crcTab[' '];
    }
    
    protected void done() {
        if (instTopLevel)
            writeLine("C "+cellNumber+";");
        writeLine("E");
        
        // finish up crc stuff
        if (!crcPrevIsCharSep) {
            crcChecksum = (crcChecksum << 8) ^ crcTab[((crcChecksum >> 24) ^ ' ') & 0xFF];
            crcNumChars++;
        }
        int bytesread = crcNumChars;
        while (bytesread > 0) {
            crcChecksum = (crcChecksum << 8) ^ crcTab[((crcChecksum >> 24) ^ bytesread) & 0xFF];
            bytesread >>= 8;
        }
        crcChecksum = ~crcChecksum & 0xFFFFFFFF;
        System.out.println("MOSIS CRC: "+EMath.unsignedIntValue(crcChecksum)+" "+crcNumChars+" "+filePath);
    }    

    /** Routine to write cellGeom */
    protected void writeCellGeom(CellGeom cellGeom)
    {
        cellNumber++;
        writeLine("DS "+cellNumber+" 1 1;");        
        writeLine("9 "+cellGeom.cell.describe()+";");
        cellNumbers.put(cellGeom.cell, new Integer(cellNumber));
        
        // write all polys by Layer
        Set layers = cellGeom.polyMap.keySet();
        for (Iterator it = layers.iterator(); it.hasNext();) {
            Layer layer = (Layer)it.next();
            writeLayer(layer);
            List polyList = (List)cellGeom.polyMap.get(layer);
            for (Iterator polyIt = polyList.iterator(); polyIt.hasNext(); ) {
                Poly poly = (Poly)polyIt.next();
                writePoly(poly);
            }
        }
        // write all instances
        for (Iterator noIt = cellGeom.nodables.iterator(); noIt.hasNext(); ) {
            Nodable no = (Nodable)noIt.next();
            writeNodable(no);
        }
        writeLine("DF;");
    }
    
    protected void writeLayer(Layer layer) {
        writeLine("L "+layer.getCIFLayer()+";");
    }
    
    protected void writePoly(Poly poly) {
        Point2D [] points = poly.getPoints();
        
        checkResolution(poly);
        
        if (poly.getStyle() == Poly.Type.DISC) {
            double r = points[0].distance(points[1]);
            if (r <= 0) return;                     // ignore zero size geometry
            r = scale(r);
            double x = scale(points[0].getX());
            double y = scale(points[0].getY());            
            String line = " R "+(long)r+" "+(int)x+" "+(int)y+";";
            writeLine(line);
        } else {
            Rectangle2D bounds = poly.getBounds2D();
            // ignore zero size geometry
            if (bounds.getHeight() <= 0 || bounds.getWidth() <= 0) return;
            Rectangle2D box = poly.getBox();
            // simple case if poly is a box
            if (box != null) {
                double width = scale(box.getWidth());
                double height = scale(box.getHeight());
                double x = scale(box.getCenterX());
                double y = scale(box.getCenterY());
                String line = " B "+(int)width+" "+(int)height+" "+
                                    (int)x+" "+(int)y+";";
                writeLine(line);
                return;
            }
            // not a box
            StringBuffer line = new StringBuffer(" P");
            for (int i=0; i<points.length; i++) {
                double x = scale(points[i].getX());
                double y = scale(points[i].getY());
                line.append(" "+(int)x+" "+(int)y);
            }
            line.append(";");
            writeLine(line.toString());
        }
    }

    protected void writeNodable(Nodable no) {
        NodeProto np = no.getProto();
        Cell cell = (Cell)np;
        int cellNum = ((Integer)cellNumbers.get(cell)).intValue();
        for (Iterator it = no.getNodeUsage().getInsts(); it.hasNext(); ) {
            NodeInst ni = (NodeInst)it.next();
            int rotx = 0;//(int)scale((EMath.cos(ni.getAngle())>>14) * 100 >> 16);
            int roty = 0;//(int)scale((EMath.sin(ni.getAngle())>>14) * 100 >> 16);
            String line = "C "+cellNum+" R "+rotx+" "+roty+" "+
                          "T "+ni.getCenterX()+" "+ni.getCenterY()+";";
            writeLine(line);
        }
    }
    
    /** Write a line to the CIF file, and accumlate 
     * checksum information.
     */
    protected void writeLine(String line) {
        line = line + '\n';
        printWriter.print(line);
        // crc checksum stuff
        for (int i=0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c > ' ') {
                crcChecksum = (crcChecksum << 8) ^ crcTab[((crcChecksum >> 24) ^ c) & 0xFF];
                crcPrevIsCharSep = false;
                crcNumChars++;
            } else if (!crcPrevIsCharSep) {
                crcChecksum = (crcChecksum << 8) ^ crcTab[((crcChecksum >> 24) ^ ' ') & 0xFF];
                crcPrevIsCharSep = true;
                crcNumChars++;
            }
        }
    }
    
    /** Method to scale Electric units to CIF units
     */
    protected double scale(double n)
    {
        return scale*n;
    }
        
    /** Check Poly for CIF Resolution Errors */
    protected void checkResolution(Poly poly) {
        ArrayList badpoints = new ArrayList();
        Point2D [] points = poly.getPoints();
        for (int i=0; i<points.length; i++) {
            if ( (points[i].getX() % minAllowedResolution) != 0 ||
                 (points[i].getY() % minAllowedResolution) != 0)
                badpoints.add(points[i]);
        }
        if (badpoints.size() > 0) {
            // there was an error, for now print error
            System.out.println("Error on poly "+poly);
            Point2D [] badpolypoints = new Point2D[badpoints.size()];
            for (int i=0; i<badpoints.size(); i++) {
                badpolypoints[i] = (Point2D)badpoints.get(i);
            }
            Poly badpoly = new Poly(badpolypoints);
        }
    }
    
}
