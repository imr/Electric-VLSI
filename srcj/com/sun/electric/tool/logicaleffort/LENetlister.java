package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Aug 30, 2004
 * Time: 1:18:03 PM
 * To change this template use File | Settings | File Templates.
 */
public interface LENetlister {

    /** Call to start netlisting */
    public void netlist(Cell cell, VarContext context);

    /** Call to stop or interrupt netlisting */
    public void done();

    /**
     * Call to size netlist with the specified algorithm
     * @return true if successful, false otherwise
     */
    public boolean size(LESizer.Alg algorithm);

    /** Call to update and save sizes */
    public void updateSizes();

    // ---------------------------- statistics ---------------------------------

    /** print the results for the Nodable
     * @return true if successful, false otherwise */
    public boolean printResults(Nodable no, VarContext context);

}
