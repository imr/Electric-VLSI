/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WireReport.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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
package com.sun.electric.tool.ncc.result;

public class PortReport extends NetObjReport {
	static final long serialVersionUID = 0;
	
	public interface PortReportable extends NetObjReportable {
		String getWireName();
		String exportNamesString();
		boolean isImplied();
	}
	
	private final String wireName;
	private final String exportNamesString;
	private final boolean isImplied;
	public PortReport(PortReportable p) {
		super(p);
		wireName = p.getWireName();
		exportNamesString = p.exportNamesString();
		isImplied = p.isImplied();
	}

	public String getWireName() {return wireName;}
	public String exportNamesString() {return exportNamesString;}
	public boolean isImplied() {return isImplied;}
}
