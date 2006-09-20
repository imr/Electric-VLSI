/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TiledCell.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.generator.layout.fill;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.tool.generator.layout.StdCellParams;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.Tech;

import java.awt.geom.Rectangle2D;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: gg151869
 * Date: Sep 19, 2006
 * Time: 12:38:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class TiledCell {
    private enum Orientation{
        VERT_EXTERIOR,  // to check if exports on top or bottom must be exported
        HORI_EXTERIOR,
        INTERIOR
    }

    private int vddNum, gndNum;
    private final String vddNm, gndNm;
    private Cell tileCell;
    protected FillGenConfig config; // required by qTree method

    private String vddName() {
        int n = vddNum++;
        return n==0 ? vddNm : vddNm+"_"+n;
    }
    private String gndName() {
        int n = gndNum++;
        return n==0 ? gndNm : gndNm+"_"+n;
    }

    /**
     * Constructor used by working with qTree fill.
     * @param stdCell
     */
    public TiledCell(StdCellParams stdCell, FillGenConfig conf)
    {
        vddNm = stdCell.getVddExportName();
        gndNm = stdCell.getGndExportName();
        config = conf;
    }

    /**
     * Method to create the master tiledCell. Note that targetHeight and targetHeight do not necessarily match
     * with cellW and cellH if algorithm is used to create a flexible number of tiled cells
     @param numX
     * @param numY
     * @param cell
     * @param plans
     * @param lib
     * @param stdCell
     */
    private TiledCell(int numX, int numY, Cell cell, Floorplan[] plans,
                      Library lib, StdCellParams stdCell)
    {
        vddNm = stdCell.getVddExportName();
        gndNm = stdCell.getGndExportName();

        String tiledName = "t"+cell.getName()+"_"+numX+"x"+numY+"{lay}";
        tileCell = Cell.newInstance(lib, tiledName);

        Rectangle2D bounds = cell.findEssentialBounds();
        ERectangle r = cell.getBounds();
        LayoutLib.error(bounds==null, "missing Essential Bounds");
        double cellW = bounds.getWidth();
        double cellH = bounds.getHeight();

        // put bottom left cell at (0, 0)
        double y = 0;

        NodeInst[][] rows = newRows(numX, numY);
        for (int row=0; row<numY; row++) {
            double x = 0;
            for (int col=0; col<numX; col++) {
                rows[row][col] = LayoutLib.newNodeInst(cell, x, y, G.DEF_SIZE,
                                                       G.DEF_SIZE, 0, tileCell);
                x += cellW;
            }
            y += cellH;
        }
        connectAllPortInsts(tileCell);
        exportUnconnectedPortInsts(rows, plans[plans.length-1].horizontal, tileCell, stdCell);
//		addEssentialBounds(cellW, cellH, numX, numY, tileCell);
        addEssentialBounds1(r.getX(), r.getY(), cellW, cellH, numX, numY, tileCell);
    }

    private static class OrderPortInstsByName implements Comparator<PortInst> {
        private String base(String s) {
            int under = s.indexOf("_");
            if (under==-1) return s;
            return s.substring(0, under);
        }
        private int subscript(String s) {
            int under = s.indexOf("_");
            if (under==-1) return 0;
            String num = s.substring(under+1, s.length());
            return Integer.parseInt(num);
        }
        public int compare(PortInst p1, PortInst p2) {
            String n1 = p1.getPortProto().getName();
            String n2 = p2.getPortProto().getName();
            String base1 = base(n1);
            String base2 = base(n2);
            if (!base1.equals(base2)) {
                return n1.compareTo(n2);
            } else {
                int sub1 = subscript(n1);
                int sub2 = subscript(n2);
                return sub1-sub2;
            }
        }
    }
    public static ArrayList<PortInst> connectAllPortInsts(Cell cell) {
        // get all the ports
        ArrayList<PortInst> ports = new ArrayList<PortInst>();
        for (Iterator<NodeInst> it=cell.getNodes(); it.hasNext();) {
            NodeInst ni = it.next();
            for (Iterator<PortInst> pIt=ni.getPortInsts(); pIt.hasNext();) {
                PortInst pi = pIt.next();
                ports.add(pi);
            }
        }
        FillRouter.connectCoincident(ports);
        return ports;
    }
    private static Orientation orientation(Rectangle2D bounds, PortInst pi) {
        EPoint center = pi.getCenter();
        double portX = center.getX();
        double portY = center.getY();
        double minX = bounds.getMinX();
        double maxX = bounds.getMaxX();
        double minY = bounds.getMinY();
        double maxY = bounds.getMaxY();
        if (DBMath.areEquals(portX,minX) || DBMath.areEquals(portX,maxX)) return Orientation.VERT_EXTERIOR;
        if (DBMath.areEquals(portY,minY) || DBMath.areEquals(portY,maxY)) return Orientation.HORI_EXTERIOR;
        return Orientation.INTERIOR;
    }
    /** return a list of all PortInsts of ni that aren't connected to
     * something. */
    private static ArrayList<PortInst> getUnconnectedPortInsts(List<Orientation> orientationList, NodeInst ni) {
        Rectangle2D bounds = ni.findEssentialBounds();
        if (bounds == null)
            bounds = ni.getBounds();
        ArrayList<PortInst> ports = new ArrayList<PortInst>();
        for (Iterator<PortInst> it=ni.getPortInsts(); it.hasNext();) {
            PortInst pi = it.next();
            if (!pi.hasConnections())
//			Iterator conns = pi.getConnections();
//			if (!conns.hasNext())
            {
                Orientation or = orientation(bounds,pi);
                if (orientationList.contains(or))
                    ports.add(pi);
            }
        }
        return ports;
    }
    private void exportPortInsts(List<PortInst> ports, Cell tiled,
                                        StdCellParams stdCell) {
        Collections.sort(ports, new OrderPortInstsByName());
        for (PortInst pi : ports) {
            PortProto pp = pi.getPortProto();
            PortCharacteristic role = pp.getCharacteristic();
            if (role==stdCell.getVddExportRole()) {
                //System.out.println(pp.getName());
                Export e = Export.newInstance(tiled, pi, vddName());
                e.setCharacteristic(role);
            } else if (role==stdCell.getGndExportRole()) {
                //System.out.println(pp.getName());
                Export e = Export.newInstance(tiled, pi, gndName());
                e.setCharacteristic(role);
            } else {
                LayoutLib.error(true, "unrecognized Characteristic");
            }
        }
    }
    /** export all PortInsts of all NodeInsts in insts that aren't connected
     * to something */
    private static final Orientation[] horizontalPlan = {Orientation.VERT_EXTERIOR, Orientation.HORI_EXTERIOR, Orientation.INTERIOR};
    private static final Orientation[] verticalPlan = {Orientation.HORI_EXTERIOR, Orientation.VERT_EXTERIOR, Orientation.INTERIOR};
    public void exportUnconnectedPortInsts(NodeInst[][] rows, boolean isPlanHorizontal,
                                                   Cell tiled, StdCellParams stdCell) {
        // Subtle!  If top layer is horizontal then begin numbering exports on
        // vertical edges of boundary first. This ensures that fill6_2x2 and
        // fill56_2x2 have matching port names on the vertical edges.
        // Always number interior exports last so they never interfere with
        // perimeter exports.
        Orientation[] orientations = (isPlanHorizontal) ? horizontalPlan : verticalPlan;
        List<Orientation> list = new ArrayList<Orientation>();

        for (int o=0; o<3; o++)
        {
            int numRows = rows.length;
            list.clear();
            list.add(orientations[o]);
            Orientation orientation = orientations[o];
            for (int row=0; row<numRows; row++)
            {
                int numCols = rows[row].length;
                for (int col=0; col<numCols; col++)
                {
//                    boolean forceHExport = (numRows > 1 && rows[(row+1)%2][col].getName().startsWith("empty"));
//                    boolean forceVExport = (numCols > 1 && rows[row][(col+1)%2].getName().startsWith("empty"));
//                    boolean forceExport = (orientation == Orientation.HORI_EXTERIOR) ? forceHExport : forceVExport;

//                    if (forceExport)
//                    {
//                        list.add(Orientation.INTERIOR);
//                    }

                    if (orientation!=Orientation.INTERIOR || row==col) {
                        List<PortInst> ports = getUnconnectedPortInsts(list, rows[row][col]);
                        exportPortInsts(ports, tiled, stdCell);
                    }
                }
            }
        }
    }

    private NodeInst[][] newRows(int numX, int numY) {
        NodeInst[][] rows = new NodeInst[numY][];
        for (int row=0; row<numY; row++) {
            rows[row] = new NodeInst[numX];
        }
        return rows;
    }
    /** Geometric center of bottom left cell is at (0, 0). */
//    private void addEssentialBounds(double cellW, double cellH,
//                                    int numX, int numY, Cell tiled) {
//        double blX = -cellW/2;
//        double blY = -cellH/2;
//        double tlX = cellW/2 + (numX-1)*cellW;
//        double tlY = cellH/2 + (numY-1)*cellH;
//        LayoutLib.newNodeInst(Tech.essentialBounds, blX, blY,
//                              G.DEF_SIZE, G.DEF_SIZE, 180, tiled);
//        LayoutLib.newNodeInst(Tech.essentialBounds, tlX, tlY,
//                              G.DEF_SIZE, G.DEF_SIZE, 0, tiled);
//    }

    private void addEssentialBounds1(double x, double y, double cellW, double cellH, int numX, int numY, Cell tiled)
    {
        double x2 = x + numX*cellW;
        double y2 = y + numY*cellH;
        LayoutLib.newNodeInst(Tech.essentialBounds, x, y,
                              G.DEF_SIZE, G.DEF_SIZE, 180, tiled);
        LayoutLib.newNodeInst(Tech.essentialBounds, x2, y2,
                              G.DEF_SIZE, G.DEF_SIZE, 0, tiled);
    }





    public static Cell makeTiledCell(int numX, int numY, Cell cell,
                                     Floorplan[] plans, Library lib,
                                     StdCellParams stdCell) {
        TiledCell tile = new TiledCell(numX, numY, cell, plans, lib, stdCell);
        return tile.tileCell;
    }
}
