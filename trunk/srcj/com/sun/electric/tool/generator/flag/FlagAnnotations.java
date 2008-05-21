package com.sun.electric.tool.generator.flag;

import java.util.ArrayList;
import java.util.List;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.Variable;

public class FlagAnnotations {
	/** key of Variable holding FLAG Cell annotations. */	
	public static final Variable.Key FLAG_ANNOTATION_KEY = Variable.newKey("ATTR_FLAG");
	public static final String ATOMIC = "atomic";
	public static final String AUTO_GEN = "autoGen";
	
	private String cellThatOwnsMe;
	private List<String> annotText = new ArrayList<String>();
	private boolean cellHasAnnotations;
	private boolean atomic;
	private String autoGenClassName;
	
	private void prErr(String s) {
		String currAnnot = annotText.get(annotText.size()-1);
		System.out.println("  "+s+"  cell= "+cellThatOwnsMe+" annotation= "+currAnnot);
	}
	private void doAnnotation(String note) {
		annotText.add(note); // for prErr()
		String[] toks = note.split("\\s+");
		String ann = toks[0];
		if (ann.equals(ATOMIC)) {
			atomic = true;
		} else if (ann.equals(AUTO_GEN)) {
			if (toks.length!=2) {
				prErr("Wrong number of arguments to FLAG "+AUTO_GEN+" annotation.");
				return;
			}
			autoGenClassName = toks[1];
		} else {
			prErr("Unrecognized FLAG annotation");
		}
	}
    
    
    // -------------------------- public methods ------------------------------
    
	/** Method to get the FLAG annotations on a Cell. */
	public FlagAnnotations(Cell cell) {
		Variable flagVar = cell.getVar(FLAG_ANNOTATION_KEY);
		if (flagVar==null) return;
		Object annotation = flagVar.getObject();
		
    	cellThatOwnsMe = cell.libDescribe();
		if (annotation instanceof String) {
			doAnnotation((String) annotation);
		} else if (annotation instanceof String[]) {
			String[] ss = (String[]) annotation;
			for (int i=0; i<ss.length; i++)  doAnnotation(ss[i]);
		} else {
			prErr(" ignoring bad Flag annotation: ");
		}
    }
	public boolean isAtomic() {return atomic;}
	public boolean isAutoGen() {return autoGenClassName!=null;}
	public String getAutoGenClassName() {return autoGenClassName;}
	public boolean cellHasAnnotations() {return cellHasAnnotations;}
}
