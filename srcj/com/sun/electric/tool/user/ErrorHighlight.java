/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ErrorHighlight.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.ExportId;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;

import java.io.PrintStream;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.List;

/**
 * Class to define Highlighted errors.
 */
public abstract class ErrorHighlight implements Serializable {
    public static final ErrorHighlight[] NULL_ARRAY = {};

    private final CellId cellId;
    private final VarContext  context; // Yet do be immutable

    ErrorHighlight(Cell c, VarContext con)
    {
        this.cellId = c != null ? (CellId)c.getId() : null;
        this.context = con;
    }

    public Cell getCell(EDatabase database) { return cellId != null ? database.getCell(cellId) : null; }

    VarContext getVarContext() { return context; }

    boolean containsObject(Cell cell, Object obj) { return false; }

    Object getObject(EDatabase database) { return null; }

    static String getImplementedXmlHeaders() { return "ERRORTYPEGEOM|ERRORTYPETHICKLINE|ERRORTYPELINE|ERRORTYPEPOLY";}
    static boolean isErrorHighlightBody(String name)
    {
        return name.equals("ERRORTYPEGEOM") || name.equals("ERRORTYPETHICKLINE") || name.equals("ERRORTYPELINE") ||
            name.equals("ERRORTYPEPOLY");
    }
    static List<ErrorHighlight> addErrorHighlight(String qName, Cell curCell, String geomName, EPoint p1, EPoint p2,
                                                  List<ErrorHighlight> list)
    {
        List<ErrorHighlight> l = list;
        boolean geoTypeBody = qName.equals("ERRORTYPEGEOM");
        if (geoTypeBody)
        {
            assert(curCell != null);
            Geometric geom = curCell.findNode(geomName);
            if (geom == null) // try arc instead
                geom = curCell.findArc(geomName);
            if (geom != null)
                list.add(ErrorHighlight.newInstance(null, geom));
            else
            {
                System.out.println("Invalid geometry " + geomName + " in " + curCell);
            }
        }
        else
        {
            boolean thickLineTypeBody = qName.equals("ERRORTYPETHICKLINE");
            boolean thinLineTypeBody = qName.equals("ERRORTYPELINE");
            if (thinLineTypeBody || thickLineTypeBody)
            {
                list.add(new ErrorHighLine(curCell, p1, p2, thickLineTypeBody));
            }
            else if (qName.equals("ERRORTYPEPOLY"))
            {
                ErrorHighPoly poly = new ErrorHighPoly(curCell, null);
                list.add(poly);
                l = poly.linesList;
            }
            else
                assert(false); // it should not happen
        }
        return l;
    }
    public static void writeXmlHeader(String indent, PrintStream ps) {System.out.println("Not implemented in writeXmlHeader");}
    void writeXmlDescription(String tabs, PrintStream msg, EDatabase database) {System.out.println("Not implemented in writeXmlDescription");}

    boolean isValid(EDatabase database) { return cellId == null || getCell(database) != null; } // Still have problems with minAre DRC errors

    public void addToHighlighter(Highlighter h, EDatabase database) {;}

    public static ErrorHighlight newInstance(VarContext cont, Geometric geom) {
        if (geom instanceof NodeInst)
            return new ErrorHighNode(cont, (NodeInst)geom);
        return new ErrorHighArc(cont, (ArcInst)geom);
    }

    public static ErrorHighlight newInstance(Cell cell, Point2D p1, Point2D p2) {
        return new ErrorHighLine(cell, EPoint.snap(p1), EPoint.snap(p2), false);
    }

    public static ErrorHighlight newInstance(Export e) {
        return new ErrorHighExport(null, e);
    }
}

class ErrorHighExport extends ErrorHighlight {

    private final ExportId      pp;

    public ErrorHighExport(VarContext con, Export p)
    {
        super(p.getParent(), con);
        this.pp = p.getId();
    }

    boolean isValid(EDatabase database) {return pp.inDatabase(database) != null;}

    public void addToHighlighter(Highlighter h, EDatabase database)
    {
        Export e = pp.inDatabase(database);
        h.addText(e, e.getParent(), Export.EXPORT_NAME);
    }
}

class ErrorHighPoly extends ErrorHighlight
{
    List<ErrorHighlight> linesList;

    public ErrorHighPoly(Cell c, List<ErrorHighlight> list)
    {
        super(c, null);
        linesList =  list;
    }

    public static void writeXmlHeader(String indent, PrintStream ps)
    {
        ps.println(indent + "<!ELEMENT ERRORTYPEPOLY (ERRORTYPETHICKLINE|ERRORTYPELINE)*>");
        ps.println(indent + "<!ATTLIST ERRORTYPEPOLY");
        ps.println(indent + "   cellName CDATA #REQUIRED");
        ps.println(indent + ">");
    }

    void writeXmlDescription(String tabs, PrintStream msg, EDatabase database)
    {
        msg.append(tabs +"<ERRORTYPEPOLY ");
        msg.append("cellName=\"" + getCell(database).describe(false) + "\" >");
        for (ErrorHighlight line : linesList)
            line.writeXmlDescription(tabs+"\t", msg, database);
        msg.append(tabs+"<ERRORTYPEPOLY/>\n");
    }

    public void addToHighlighter(Highlighter h, EDatabase database)
    {
        for (ErrorHighlight line : linesList)
            line.addToHighlighter(h, database);
    }
}

class ErrorHighLine extends ErrorHighlight {

	private final EPoint p1, p2;
    private final boolean thickLine;

    public ErrorHighLine(Cell c, EPoint x1, EPoint x2, boolean thick)
    {
        super(c, null);
        thickLine = thick;
        p1 = x1;
        p2 = x2;
    }

    public static void writeXmlHeader(String indent, PrintStream ps)
    {
        ps.println(indent + "<!ELEMENT ERRORTYPELINE ANY>");
        ps.println(indent + "<!ATTLIST ERRORTYPELINE");
        ps.println(indent + "   p1 CDATA #REQUIRED");
        ps.println(indent + "   p2 CDATA #REQUIRED");
        ps.println(indent + "   cellName CDATA #REQUIRED");
        ps.println(indent + ">");

        ps.println(indent + "<!ELEMENT ERRORTYPETHICKLINE ANY>");
        ps.println(indent + "<!ATTLIST ERRORTYPETHICKLINE");
        ps.println(indent + "   p1 CDATA #REQUIRED");
        ps.println(indent + "   p2 CDATA #REQUIRED");
        ps.println(indent + "   cellName CDATA #REQUIRED");
        ps.println(indent + ">");
    }

    void writeXmlDescription(String tabs, PrintStream msg, EDatabase database)
    {
        msg.append(tabs +"<"+((thickLine)?"ERRORTYPETHICKLINE ":"ERRORTYPELINE "));
        msg.append("p1=\"(" + p1.getX() + "," + p1.getY() + ")\" ");
        msg.append("p2=\"(" + p2.getX() + "," + p2.getY() + ")\" ");
        msg.append("cellName=\"" + getCell(database).describe(false) + "\"");
        msg.append(" />\n");
    }

    public void addToHighlighter(Highlighter h, EDatabase database)
    {
        Cell cell = getCell(database);
        if (thickLine) h.addThickLine(p1, p2, cell);
        else h.addLine(p1, p2, cell);
    }
}

class ErrorHighPoint extends ErrorHighlight {
	private final EPoint point;

    ErrorHighPoint(Cell c, EPoint p)
    {
        super(c, null);
        this.point = p;
    }

    public void addToHighlighter(Highlighter h, EDatabase database)
    {
        double consize = 5;
        Cell cell = getCell(database);
        h.addLine(new Point2D.Double(point.getX()-consize, point.getY()-consize),
                new Point2D.Double(point.getX()+consize, point.getY()+consize), cell);
        h.addLine(new Point2D.Double(point.getX()-consize, point.getY()+consize),
                new Point2D.Double(point.getX()+consize, point.getY()-consize), cell);
    }
}

class ErrorHighNode extends ErrorHighlight {

    private final int nodeId;

    public ErrorHighNode(VarContext con, NodeInst ni)
    {
        super(ni.getParent(), con);
        nodeId = ni.getD().nodeId;
    }

    boolean containsObject(Cell c, Object obj)
    {
        EDatabase database = c.getDatabase();
        return getCell(database) == c && getObject(database) == obj;
    }

    Object getObject(EDatabase database) {
        Cell cell = getCell(database);
        if (cell == null) return null;
        return cell.getNodeById(nodeId);
    }

    public static void writeXmlHeader(String indent, PrintStream ps)
    {
        ps.println(indent + "<!ELEMENT ERRORTYPEGEOM ANY>");
        ps.println(indent + "<!ATTLIST ERRORTYPEGEOM");
        ps.println(indent + "   geomName CDATA #REQUIRED");
        ps.println(indent + "   cellName CDATA #REQUIRED");
        ps.println(indent + ">");
    }

    void writeXmlDescription(String tabs, PrintStream msg, EDatabase database)
    {
        NodeInst ni = (NodeInst)getObject(database);
        msg.append(tabs+"<ERRORTYPEGEOM ");
        msg.append("geomName=\"" + ni.getName() + "\" ");
        msg.append("cellName=\"" + ni.getParent().describe(false) + "\"");
        msg.append(" />\n");
    }

    boolean isValid(EDatabase database) { return getObject(database) != null; }

    public void addToHighlighter(Highlighter h, EDatabase database)
    {
        NodeInst ni = (NodeInst)getObject(database);
        if (ni != null)
            h.addElectricObject(ni, ni.getParent());
    }
}

class ErrorHighArc extends ErrorHighlight {

    private final int arcId;

    public ErrorHighArc(VarContext con, ArcInst ai)
    {
        super(ai.getParent(), con);
        arcId = ai.getArcId();
    }

    boolean containsObject(Cell c, Object obj)
    {
        EDatabase database = c.getDatabase();
        return getCell(database) == c && getObject(database) == obj;
    }

    Object getObject(EDatabase database) {
        Cell cell = getCell(database);
        if (cell == null) return null;
        return cell.getArcById(arcId);
    }

    void writeXmlDescription(String tabs, PrintStream msg, EDatabase database)
    {
        ArcInst ai = (ArcInst)getObject(database);
        msg.append(tabs+"<ERRORTYPEGEOM ");
        msg.append("geomName=\"" + ai.getD().name + "\" ");
        msg.append("cellName=\"" + ai.getParent().describe(false) + "\"");
        msg.append(" />\n");
    }

    boolean isValid(EDatabase database) {return getObject(database) != null; }

    public void addToHighlighter(Highlighter h, EDatabase database)
    {
        ArcInst ai = (ArcInst)getObject(database);
        if (ai != null)
            h.addElectricObject(ai, ai.getParent());
    }
}
