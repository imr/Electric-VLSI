package com.sun.electric.tool.simulation.test;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Jun 30, 2005
 * Time: 10:12:16 AM
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
 */
public class NanosimLogicSettable implements LogicSettable {

    private final NanosimModel nm;
    private final String port;          // port name

    private static final boolean DEBUG = true;

    NanosimLogicSettable(NanosimModel nm, String port) {
        this.nm = nm;
        this.port = port;
    }

    public boolean isLogicStateHigh() {
        int state = nm.getNodeState(port);
        if (state == 1) return true;
        return false;
    }

    public void setLogicState(boolean logicState) {
        int i = (logicState ? 1 : 0);
        nm.setNodeState(port, i);
    }

    private boolean initState = false;

    public void setInitState(boolean initState) {
        this.initState = initState;
    }

    /**
     * Nodes in Nanosim are referenced by their index number, which can
     * only be found by querying the interactive process after it has started.
     * We'll do it once, then cache it.
     * @return true if successful, false if failed.
     */
    boolean init() {
        setLogicState(initState);
        return true;
    }

    /** Unit Test
     * This test requires the file sim.spi in your working dir
     * */
    public static void main(String [] args) {
        NanosimModel nm = new NanosimModel();
        List list = new ArrayList();
        list.add("a");
        list.add("xinv1.aa");
        list.add("g");
        LogicSettable ls = nm.createLogicSettable(list);
        nm.start("nanosim", "sim.spi", 0);
        ls.isLogicStateHigh();
        nm.finish();
    }
}
