/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ExportNames.java
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
