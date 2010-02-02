/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NanosimLogicSettable.java
 * Written by Jonathan Gainsley, Sun Microsystems.
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
package com.sun.electric.tool.simulation.test;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

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
