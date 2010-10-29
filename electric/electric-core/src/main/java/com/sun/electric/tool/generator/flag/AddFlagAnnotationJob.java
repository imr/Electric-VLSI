/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AddFlagAnnotationJob.java
 *
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.generator.flag;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;

public class AddFlagAnnotationJob extends Job {
	static final long serialVersionUID = 0;
	
	private transient EditWindow_ wnd;
    private Cell cell;
    private String newAnnotation;

	private AddFlagAnnotationJob(EditWindow_ wnd, Cell cell, String annotation) {
        super("Make Flag Annotation", NetworkTool.getNetworkTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
        this.wnd = wnd;
        this.cell = cell;
        newAnnotation = annotation;
        startJob();
    }

    public boolean doIt() throws JobException {
    	
		Variable plaidVar = cell.getVar(FlagAnnotations.FLAG_ANNOTATION_KEY);
		if (plaidVar == null) {
			String [] initial = new String[1];
			initial[0] = newAnnotation;
			TextDescriptor td = TextDescriptor.getCellTextDescriptor().withInterior(true).withDispPart(TextDescriptor.DispPos.NAMEVALUE);
			plaidVar = cell.newVar(FlagAnnotations.FLAG_ANNOTATION_KEY, initial, td);
			if (plaidVar == null) return true;
		} else {
			Object oldObj = plaidVar.getObject();
			if (oldObj instanceof String) {
				/* Groan! Menu command always creates PLAID attributes as arrays of strings.
				 * However, if user edits a single line PLAID attribute then dialog box
				 * converts it back into a String.  Be prepared to convert it back into an array*/
				oldObj = new String[] {(String)oldObj};
			}
			error(!(oldObj instanceof String[]), "PLAID annotation not String[]");
			String[] oldVal = (String[]) oldObj;
			TextDescriptor td = plaidVar.getTextDescriptor();

			int newLen = oldVal.length+1;
			String[] newVal = new String[newLen];
			for (int i=0; i<newLen-1; i++) newVal[i]=oldVal[i];
			newVal[newLen-1] = newAnnotation;
			plaidVar = cell.newVar(FlagAnnotations.FLAG_ANNOTATION_KEY, newVal, td);
		}
		return true;
    }
    public void terminateOK() {
    	wnd.clearHighlighting();
		wnd.addHighlightText(cell, cell, FlagAnnotations.FLAG_ANNOTATION_KEY);
		wnd.finishedHighlighting();
    }
    
	/** Method to create NCC annotations in the current Cell.
	 * Called from the menu commands. */
	public static void makeCellAnnotation(String newAnnotation) {
		UserInterface ui = Job.getUserInterface();
		EditWindow_ wnd = ui.needCurrentEditWindow_();
		if (wnd == null) return;
		Cell cell = ui.needCurrentCell();
		if (cell == null) return;
		new AddFlagAnnotationJob(wnd, cell, newAnnotation);
	}
}
