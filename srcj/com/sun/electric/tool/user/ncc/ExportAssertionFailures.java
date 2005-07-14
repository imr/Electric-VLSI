package com.sun.electric.tool.user.ncc;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;

public class ExportAssertionFailures {
    private Cell cell;
    private VarContext context;
    private Object[][] items;  // an array of Export and/or Network objects
    private String[][] names;  // names corresponding to objects in items 
    
    public ExportAssertionFailures(Cell cell, VarContext context,
                                   Object[][] items, String[][] names) {
        this.cell = cell;
        this.context = context;
        this.items = items;
        this.names = names;
    }
    
    public Cell       getCell()           { return cell; }
    public VarContext getContext()        { return context; }
    public Object[][] getExportsGlobals() { return items; }
    public String[][] getNames()          { return names; }
}
