package com.sun.electric.tool.generator.infinity;

import java.util.ArrayList;
import java.util.Collection;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;

public class Stages {
	private Collection<Cell> stages = new ArrayList<Cell>();
	private boolean someCellMissing = false;
	public Cell branch, cross, drain, fill, merge, plain;
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
	
	public Stages(Library lib) {
		if (lib==null) return;
		branch = findStage(lib, "aBranchStage{lay}");
		cross = findStage(lib, "aCrossStage{lay}");
		drain = findStage(lib, "aDrainStage{lay}");
		fill = findStage(lib, "aFillStage{lay}");
		merge = findStage(lib, "aMergeStage{lay}");
		plain = findStage(lib, "aPlainStage{lay}");
	}
	public Collection<Cell> getStages() {
		return new ArrayList<Cell>(stages);
	}
	public boolean someStageIsMissing() {return someCellMissing;}
}
