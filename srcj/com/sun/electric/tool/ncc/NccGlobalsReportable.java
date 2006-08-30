package com.sun.electric.tool.ncc;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NetNameProxy;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NodableNameProxy;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.user.ncc.NccGuiInfo;

public interface NccGlobalsReportable {
	NetNameProxy[][] getEquivalentNets();
	NodableNameProxy[][] getEquivalentNodes();
	Cell[] getRootCells();
	String[] getRootCellNames();
	VarContext[] getRootContexts();
	NccOptions getOptions();
	int[] getPartCounts();
	int[] getPortCounts();
	int[] getWireCounts();
	boolean[] cantBuildNetlistBits();
	NccGuiInfo getNccGuiInfo();

}
