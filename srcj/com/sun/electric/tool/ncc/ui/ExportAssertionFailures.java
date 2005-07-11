package com.sun.electric.tool.ncc.ui;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;

public class ExportAssertionFailures {
    private Cell cell;
    private VarContext context;
    private Object[][] items;  // an array of Export and/or Network objects
    
    public ExportAssertionFailures(Cell cell, VarContext context,Object[][] items) {
        this.cell = cell;
        this.context = context;
        this.items = items;
        System.out.println(" ### ExportAssertionFailures: NEW obj: cell = "+cell.getName()
                + "; context = " + context.getInstPath("/")
                + "; array of "+items.length+" items");
    }
    
    public Cell       getCell()           { return cell; }
    public VarContext getContext()        { return context; }
    public Object[][] getExportsGlobals() { return items; }
}
