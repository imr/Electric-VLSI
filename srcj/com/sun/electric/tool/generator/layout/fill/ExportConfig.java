/**
 * 
 */
package com.sun.electric.tool.generator.layout.fill;

import java.io.Serializable;

public class ExportConfig implements Serializable {
	public static final long serialVersionUID = 0;
	
	public static final int HIGHEST_LAYER = -1000;
	public static final int NEXT_TO_HIGHEST_LAYER = -2000;
	public static final int LOWEST_LAYER = -3000;
	public static final int NEXT_TO_LOWEST_LAYER = -4000;
	public static final ExportConfig PERIMETER = 
		new ExportConfig(new int[] {HIGHEST_LAYER, NEXT_TO_HIGHEST_LAYER},
				         new int[] {});
	public static final ExportConfig PERIMETER_AND_INTERNAL = 
		new ExportConfig(new int[] {HIGHEST_LAYER, NEXT_TO_HIGHEST_LAYER},
				         new int[] {LOWEST_LAYER});

	private int[] perimeterExports;
	private int[] internalExports;
	public ExportConfig(int[] layersToGetPerimeterExports,
			            int[] layersToGetInternalExports) {
		int l = layersToGetPerimeterExports.length;
		perimeterExports = new int[l];
		for (int i=0; i<l; i++) {
			perimeterExports[i] = layersToGetPerimeterExports[i];
		}
		l = layersToGetInternalExports.length;
		internalExports = new int[l];
		for (int i=0; i<l; i++) {
			internalExports[i] = layersToGetInternalExports[i];
		}
	}
	private int translate(int lay, int loLay, int hiLay) {
		if (lay==HIGHEST_LAYER) {
			return hiLay;
		} else if (lay==NEXT_TO_HIGHEST_LAYER) {
			return hiLay-1;
		} else if (lay==LOWEST_LAYER) {
			return loLay;
		} else if (lay==NEXT_TO_LOWEST_LAYER) {
			return loLay+1;
		} else {
			return lay;
		}
	}

	int[] getPerimeterExports(int loLay, int hiLay) {
		int len = perimeterExports.length;
		int[] ans = new int[len];
		for (int i=0; i<len; i++)  {
			ans[i] = translate(perimeterExports[i], loLay, hiLay);
		}
		return ans;
	}
	int[] getInternalExports(int loLay, int hiLay) {
		int len = internalExports.length;
		int[] ans = new int[len];
		for (int i=0; i<len; i++)  {
			ans[i] = translate(internalExports[i], loLay, hiLay);
		}
		return ans;
	}

}
