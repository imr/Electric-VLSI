package com.sun.electric.tool.user;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;

import java.io.PrintStream;
import java.awt.geom.Point2D;

/**
 * Class to define Highlighted errors.
 */
public abstract class ErrorHighlight {
    Cell        cell;
    VarContext  context;

    ErrorHighlight(Cell c, VarContext con)
    {
        this.cell = c;
        this.context = con;
    }

    Cell getCell() { return cell; }

    VarContext getVarContext(){ return context; }

    boolean containsObject(Cell cell, Object obj) { return false; }

    Object getObject() { return null; }

    void xmlDescription(PrintStream msg) {;System.out.println("Not implemented in xmlDescription");}

    boolean isValid() {return (cell.isLinked());} // Still have problems with minAre DRC errors

    void addToHighlighter(Highlighter h) {;}
}

class ErrorHighExport extends ErrorHighlight {

    Export      pp;

    public ErrorHighExport(Cell c, VarContext con, Export p)
    {
        super(c, con);
        this.pp = p;
    }

    boolean isValid() {return pp.isLinked();}

    void addToHighlighter(Highlighter h)
    {
        h.addText(pp, cell, Export.EXPORT_NAME);
    }
}

class ErrorHighLine extends ErrorHighlight {

	EPoint p1, p2;
    boolean thickLine;

    public ErrorHighLine(Cell c, EPoint x1, EPoint x2, boolean thick)
    {
        super(c, null);
        thickLine = thick;
        p1 = x1;
        p2 = x2;
    }

    void xmlDescription(PrintStream msg)
    {
        msg.append("\t\t<"+((thickLine)?"ERRORTYPETHICKLINE ":"ERRORTYPELINE "));
        msg.append("p1=\"(" + p1.getX() + "," + p1.getY() + ")\" ");
        msg.append("p2=\"(" + p2.getX() + "," + p2.getY() + ")\" ");
        msg.append("cellName=\"" + cell.describe(false) + "\"");
        msg.append(" />\n");
    }

    void addToHighlighter(Highlighter h)
    {
        if (thickLine) h.addThickLine(p1, p2, cell);
        else h.addLine(p1, p2, cell);
    }
}

class ErrorHighPoint extends ErrorHighlight {
	EPoint point;

    ErrorHighPoint(Cell c, EPoint p)
    {
        super(c, null);
        this.point = p;
    }

    void addToHighlighter(Highlighter h)
    {
        double consize = 5;
        h.addLine(new Point2D.Double(point.getX()-consize, point.getY()-consize),
                new Point2D.Double(point.getX()+consize, point.getY()+consize), cell);
        h.addLine(new Point2D.Double(point.getX()-consize, point.getY()+consize),
                new Point2D.Double(point.getX()+consize, point.getY()-consize), cell);
    }
}

class ErrorHighGeom extends ErrorHighlight {

    Geometric   geom;
    boolean     showgeom;

    public ErrorHighGeom(Cell c, VarContext con, Geometric g, boolean show)
    {
        super(c, con);
        this.geom = g;
        this.showgeom = show;
    }

    boolean containsObject(Cell c, Object obj)
    {
        return cell == c && geom == obj;
    }

    Object getObject() { return geom; }

    void xmlDescription(PrintStream msg)
    {
        if (!showgeom) return;
        msg.append("\t\t<ERRORTYPEGEOM ");
        if (geom instanceof NodeInst)
            msg.append("geomName=\"" + ((NodeInst)geom).getD().name + "\" ");
        else
            msg.append("geomName=\"" + ((ArcInst)geom).getD().name + "\" ");
        msg.append("cellName=\"" + cell.describe(false) + "\"");
        msg.append(" />\n");
    }

    boolean isValid() {return geom.isLinked();}

    void addToHighlighter(Highlighter h)
    {
        if (showgeom) h.addElectricObject(geom, cell);
    }
}
