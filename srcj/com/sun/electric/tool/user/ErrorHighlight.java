package com.sun.electric.tool.user;

import com.sun.electric.database.CellId;
import com.sun.electric.database.ExportId;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;

import java.io.PrintStream;
import java.awt.geom.Point2D;
import java.io.Serializable;

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

    void xmlDescription(PrintStream msg, EDatabase database) {;System.out.println("Not implemented in xmlDescription");}

    boolean isValid(EDatabase database) { return cellId == null || getCell(database) != null; } // Still have problems with minAre DRC errors

    void addToHighlighter(Highlighter h, EDatabase database) {;}
    
    public static ErrorHighlight newInstance(VarContext context, Geometric geom) {
        if (geom instanceof NodeInst)
            return new ErrorHighNode(context, (NodeInst)geom);
        else
            return new ErrorHighArc(context, (ArcInst)geom);
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
        super((Cell)p.getParent(), con);
        this.pp = (ExportId)p.getId();
    }

    boolean isValid(EDatabase database) {return pp.inDatabase(database) != null;}

    void addToHighlighter(Highlighter h, EDatabase database)
    {
        Export e = pp.inDatabase(database);
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

    void xmlDescription(PrintStream msg, EDatabase database)
    {
        msg.append("\t\t<"+((thickLine)?"ERRORTYPETHICKLINE ":"ERRORTYPELINE "));
        msg.append("p1=\"(" + p1.getX() + "," + p1.getY() + ")\" ");
        msg.append("p2=\"(" + p2.getX() + "," + p2.getY() + ")\" ");
        msg.append("cellName=\"" + getCell(database).describe(false) + "\"");
        msg.append(" />\n");
    }

    void addToHighlighter(Highlighter h, EDatabase database)
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

    void addToHighlighter(Highlighter h, EDatabase database)
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

    void xmlDescription(PrintStream msg, EDatabase database)
    {
        NodeInst ni = (NodeInst)getObject(database);
        msg.append("\t\t<ERRORTYPEGEOM ");
        msg.append("geomName=\"" + ni.getName() + "\" ");
        msg.append("cellName=\"" + ni.getParent().describe(false) + "\"");
        msg.append(" />\n");
    }

    boolean isValid(EDatabase database) { return getObject(database) != null; }

    void addToHighlighter(Highlighter h, EDatabase database)
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
        arcId = ai.getD().arcId;
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

    void xmlDescription(PrintStream msg, EDatabase database)
    {
        ArcInst ai = (ArcInst)getObject(database);
        msg.append("\t\t<ERRORTYPEGEOM ");
        msg.append("geomName=\"" + ai.getD().name + "\" ");
        msg.append("cellName=\"" + ai.getParent().describe(false) + "\"");
        msg.append(" />\n");
    }

    boolean isValid(EDatabase database) {return getObject(database) != null; }

    void addToHighlighter(Highlighter h, EDatabase database)
    {
        ArcInst ai = (ArcInst)getObject(database);
        if (ai != null)
            h.addElectricObject(ai, ai.getParent());
    }
}
