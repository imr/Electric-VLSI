package com.sun.electric.tool.user;

import com.sun.electric.database.CellId;
import com.sun.electric.database.ExportId;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;

import java.io.PrintStream;
import java.awt.geom.Point2D;
import java.io.Serializable;

/**
 * Class to define Highlighted errors.
 */
public abstract class ErrorHighlight implements Serializable {
    static final ErrorHighlight[] NULL_ARRAY = {};

    private final CellId cellId;
    private final VarContext  context; // Yet do be immutable

    ErrorHighlight(Cell c, VarContext con)
    {
        this.cellId = c != null ? (CellId)c.getId() : null;
        this.context = con;
    }

    Cell getCell() { return cellId != null ? (Cell)cellId.inCurrentThread() : null; }

    VarContext getVarContext() { return context; }

    boolean containsObject(Cell cell, Object obj) { return false; }

    Object getObject() { return null; }

    void xmlDescription(PrintStream msg) {;System.out.println("Not implemented in xmlDescription");}

    boolean isValid() { return cellId == null || getCell() != null; } // Still have problems with minAre DRC errors

    void addToHighlighter(Highlighter h) {;}
}

class ErrorHighExport extends ErrorHighlight {

    private final ExportId      pp;

    public ErrorHighExport(VarContext con, Export p)
    {
        super((Cell)p.getParent(), con);
        this.pp = (ExportId)p.getId();
    }

    boolean isValid() {return pp.inCurrentThread() != null;}

    void addToHighlighter(Highlighter h)
    {
        Export e = (Export)pp.inCurrentThread();
        h.addText(e, (Cell)e.getParent(), Export.EXPORT_NAME);
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

    void xmlDescription(PrintStream msg)
    {
        msg.append("\t\t<"+((thickLine)?"ERRORTYPETHICKLINE ":"ERRORTYPELINE "));
        msg.append("p1=\"(" + p1.getX() + "," + p1.getY() + ")\" ");
        msg.append("p2=\"(" + p2.getX() + "," + p2.getY() + ")\" ");
        msg.append("cellName=\"" + getCell().describe(false) + "\"");
        msg.append(" />\n");
    }

    void addToHighlighter(Highlighter h)
    {
        Cell cell = getCell();
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

    void addToHighlighter(Highlighter h)
    {
        double consize = 5;
        Cell cell = getCell();
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
        return getCell() == c && getObject() == obj;
    }

    Object getObject() {
        Cell cell = getCell();
        if (cell == null) return null;
        return cell.getNodeById(nodeId);
    }

    void xmlDescription(PrintStream msg)
    {
        NodeInst ni = (NodeInst)getObject();
        msg.append("\t\t<ERRORTYPEGEOM ");
        msg.append("geomName=\"" + ni.getName() + "\" ");
        msg.append("cellName=\"" + ni.getParent().describe(false) + "\"");
        msg.append(" />\n");
    }

    boolean isValid() { return getObject() != null; }

    void addToHighlighter(Highlighter h)
    {
        NodeInst ni = (NodeInst)getObject();
        if (ni != null)
            h.addElectricObject(ni, ni.getParent());
    }
}

class ErrorHighArc extends ErrorHighlight {

    private final int arcId;

    public ErrorHighArc(VarContext con, ArcInst ai)
    {
        super(ai.getParent(), con);
        arcId = ai.getD().arcId;
    }

    boolean containsObject(Cell c, Object obj)
    {
        return getCell() == c && getObject() == obj;
    }

    Object getObject() {
        Cell cell = getCell();
        if (cell == null) return null;
        return cell.getArcById(arcId);
    }

    void xmlDescription(PrintStream msg)
    {
        ArcInst ai = (ArcInst)getObject();
        msg.append("\t\t<ERRORTYPEGEOM ");
        msg.append("geomName=\"" + ai.getD().name + "\" ");
        msg.append("cellName=\"" + ai.getParent().describe(false) + "\"");
        msg.append(" />\n");
    }

    boolean isValid() {return getObject() != null; }

    void addToHighlighter(Highlighter h)
    {
        ArcInst ai = (ArcInst)getObject();
        if (ai != null)
            h.addElectricObject(ai, ai.getParent());
    }
}
