/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EThread.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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
package com.sun.electric.tool;

import com.sun.electric.database.variable.UserInterface;

import java.util.logging.Level;

/**
 * Thread for execution Jobs in Electric.
 */
abstract class EThread extends Thread {
    EJob ejob;
    
    private volatile boolean canChanging;
    private volatile boolean canComputeBounds;
    private volatile boolean canComputeNetlist;
    private volatile UserInterface userInterface;
    
    /** Creates a new instance of EThread */
    EThread(String threadName) {
        super(threadName);
    }
    
	/**
	 * Method to check whether changing of whole database is allowed.
	 * @throws IllegalStateException if changes are not allowed.
	 */
	void checkChanging() {
        if (canChanging) return;
        IllegalStateException e = new IllegalStateException("Database changes are forbidden");
        Job.logger.logp(Level.WARNING, getClass().getName(), "checkChanging", e.getMessage(), e);
        throw e;
    }
    
    /**
     * Checks if bounds can be computed in this thread.
     * @return true if bounds or netlist can be computed.
     */
    boolean canComputeBounds() { return canComputeBounds; }

    /**
     * Checks if bounds or netlist can be computed in this thread.
     * @return true if bounds or netlist can be computed.
     */
    boolean canComputeNetlist() { return canComputeNetlist; }

    UserInterface getUserInterface() { return userInterface; }

    void print(String str) {
        if (ejob != null && ejob.connection != null)
            ejob.connection.addMessage(str);
    }
    
    void setCanChanging(boolean value) { canChanging = value; }
    void setCanComputeBounds(boolean value) { canComputeBounds = value; }
    void setCanComputeNetlist(boolean value) { canComputeNetlist = value; }
    void setUserInterface(UserInterface userInterface) { this.userInterface = userInterface; }
    void setJob(EJob ejob) { this.ejob = ejob; }
}
