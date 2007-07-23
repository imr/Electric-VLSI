package com.sun.electric.tool.generator.infinity;

import java.util.ArrayList;
import java.util.Collection;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;

public class Stages {
	private static final String[][] stageNms = new String[][] {
		{"stagesF", "aBranchStage{lay}"},
		{"stagesF", "aCountStage{lay}"},
		{"stagesF", "aCrossStage{lay}"},
		{"stagesF", "aDrainStage{lay}"},
		{"stagesF", "aFillStage{lay}"},
		{"stagesF", "aMergeStage{lay}"},
		{"stagesF", "aPlainStage{lay}"},
		{"stagesF", "aSwitchStage{lay}"},
		{"stagesF", "aWeakStage{lay}"},
		{"fansF", "columnFanLeft{lay}"},
		{"fansF", "columnFanRight{lay}"},
		{"fansF", "scanFanLeft{lay}"},
		{"fansF", "scanFanRight{lay}"}
	};
	private Collection<Cell> stages = new ArrayList<Cell>();
	private boolean someCellMissing = false;
	public Cell branch, cross, drain, fill, merge /*plain*/;
	private void prln(String msg) {System.out.println(msg);}
	
	private Cell findStage(Library lib, String stageNm) {
		Cell stage = lib.findNodeProto(stageNm);
		if (stage==null) {
			prln("Can't find stage: "+stageNm);
			someCellMissing = true;
		} else {
			stages.add(stage);
		}
		return stage;
	}
	
	public Stages() {
		for (int i=0; i<stageNms.length; i++) {
			Library l = Library.findLibrary(stageNms[i][0]);
			if (l==null) continue;
			Cell stage = findStage(l, stageNms[i][1]);
			if (stage!=null) stages.add(stage);
		}
	}
	public Collection<Cell> getStages() {
		return new ArrayList<Cell>(stages);
	}
	public boolean someStageIsMissing() {return someCellMissing;}
}
