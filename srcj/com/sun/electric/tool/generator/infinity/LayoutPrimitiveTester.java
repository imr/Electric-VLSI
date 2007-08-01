package com.sun.electric.tool.generator.infinity;

import java.util.ArrayList;
import java.util.List;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;

public class LayoutPrimitiveTester {
	private List<Library> primLayLibs = new ArrayList<Library>();
	public LayoutPrimitiveTester(List<Library> primLibs) {
		primLayLibs.addAll(primLibs);
	}
	public boolean isLayoutPrimitive(Cell c) {
		if (c.getCellName().getName().equals("fillCellA")) return false;
		Library lib = c.getLibrary();
		for (Library l : primLayLibs) {
			if (lib==l) return true;
		}
		return false;
	}
}
