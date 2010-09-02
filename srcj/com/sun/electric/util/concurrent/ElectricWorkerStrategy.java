/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ElectricWorkerStrategy.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.util.concurrent;

import com.sun.electric.database.Environment;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.util.concurrent.datastructures.IStructure;
import com.sun.electric.tool.util.concurrent.patterns.PTask;
import com.sun.electric.tool.util.concurrent.runtime.taskParallel.SimpleWorker;

/**
 * @author Felix Schmidt
 * 
 */
public class ElectricWorkerStrategy extends SimpleWorker {

	private UserInterface ui;

	/**
	 * @param taskPool
	 */
	public ElectricWorkerStrategy(IStructure<PTask> taskPool) {
		super(taskPool);
		// TODO Auto-generated constructor stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.util.concurrent.runtime.WorkerStrategy#execute()
	 */
	@Override
	public void execute() {

		try {
			Job.setUserInterface(this.getUi());
			Environment.setThreadEnvironment(Job.getUserInterface().getDatabase().getEnvironment());
		} catch (Exception ex) {

		}

		super.execute();

	}

	/**
	 * @param ui
	 *            the ui to set
	 */
	public void setUi(UserInterface ui) {
		this.ui = ui;
	}

	/**
	 * @return the ui
	 */
	public UserInterface getUi() {
		return ui;
	}

}
