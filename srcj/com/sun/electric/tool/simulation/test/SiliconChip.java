package com.sun.electric.tool.simulation.test;

import java.util.List;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Oct 6, 2005
 * Time: 11:16:48 AM
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
 */
public class SiliconChip implements ChipModel {

    public SiliconChip() {
    }

    public void wait(float seconds) {
        Infrastructure.wait(seconds);
    }

    public void waitNS(double nanoseconds) {
        Infrastructure.wait((float)(nanoseconds/1e9));
    }

    public void waitPS(double picoseconds) {
        Infrastructure.wait((float)(picoseconds/1e12));
    }
}
