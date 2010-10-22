/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ExportConfig.java
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
