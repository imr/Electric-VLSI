/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CantEditException.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;

public class CantEditException extends JobException
{
	/** the node that is locked and needs to change */					NodeInst lockedNode;
	/** the locked primitive (must allow locked prims to change) */		NodeInst lockedPrim;
	/** the locked complex node (must allow complex nodes to change) */	NodeInst lockedComplex;
	/** the cell with locked instances */								Cell lockedInstances;
	/** the cell that is locked */										Cell lockedAll;
	/** the locked node (when cells are locked) */						NodeInst lockedExample;

    /**
	 * Method to describe this CantEditException.
	 * WARNING: method may change the database if the user disables a cell lock,
	 * so method must be called inside of a Change job.
	 * @return zero if the operation should be done.
	 * Return positive if the user chooses to NOT DO the change.
	 * Return negative if the user chooses to NOT DO the change and the overall operation should be cancelled.
	 */
	public int presentProblem()
	{
		String [] options = {"Yes", "No", "Always", "Cancel"};
		UserInterface ui = Job.getUserInterface();

		// if an instance is specified, check it
		if (lockedNode != null)
		{
			int ret = ui.askForChoice("Changes to locked " + lockedNode + " are disallowed.  Change anyway?",
				"Allow changes", options, options[1]);
			if (ret == 3 || ret == -1) return -1;  // -1 represents ESC or cancel
			if (ret == 1) return 1;
			if (ret == 2) lockedNode.clearLocked();
			return 0;
		}
		if (lockedPrim != null)
		{
			int ret = ui.askForChoice("Changes to locked primitives (such as " + lockedPrim + ") are disallowed.  Change anyway?",
				"Allow changes", options, options[1]);
			if (ret == 3 || ret == -1) return -1;  // -1 represents ESC or cancel
			if (ret == 1) return 1;
			if (ret == 2) User.setDisallowModificationLockedPrims(false);
			return 0;
		}
		if (lockedInstances != null)
		{
			int ret = ui.askForChoice("Modification of instances in " + lockedInstances + " is disallowed.  You cannot move " +
				lockedExample + ".  Change anyway?", "Allow changes", options, options[1]);
			if (ret == 3 || ret == -1) return -1;  // -1 represents ESC or cancel
			if (ret == 1) return 1;
			if (ret == 2) lockedInstances.clearInstancesLocked();
			return 0;
		}
		if (lockedComplex != null)
		{
			int ret = ui.askForChoice("Changes to complex nodes (such as " + lockedComplex + ") are disallowed.  Change anyway?",
				"Allow changes", options, options[1]);
			if (ret == 3 || ret == -1) return -1;  // -1 represents ESC or cancel
			if (ret == 1) return 1;
			if (ret == 2) User.setDisallowModificationComplexNodes(false);
			return 0;
		}

		// check for general changes to the cell
		if (lockedAll != null)
		{
			int ret = ui.askForChoice("Modification of " + lockedAll + " is disallowed.  Change " +
				((lockedExample == null)? "" : lockedExample.toString())+" anyway?",
				"Allow changes", options, options[1]);
			if (ret == 3 || ret == -1) return -1;  // -1 represents ESC or cancel
			if (ret == 1) return 1;
			if (ret == 2) lockedAll.clearAllLocked();
			return 0;
		}
		return 0;
	}

}
