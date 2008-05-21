package com.sun.electric.tool.generator.flag;

public class ExportNamer {
	private int cnt = 0;
	private final String baseName;
	public ExportNamer(String nm) {baseName=nm;}
	public ExportNamer(String nm, int startCnt) {
		this(nm);
		cnt = startCnt;
	}
	private String addIntSuffix(String nm, int count) {
		if (count==0) return nm;
		else return nm + "_" + count;
	}
	
	public String nextName() {
		return addIntSuffix(baseName, cnt++);
	}
}
