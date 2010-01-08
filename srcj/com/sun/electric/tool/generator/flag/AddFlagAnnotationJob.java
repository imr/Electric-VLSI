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
