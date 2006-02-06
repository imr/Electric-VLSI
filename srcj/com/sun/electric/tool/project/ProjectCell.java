/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ProjectCell.java
 * Project management tool: cell information
 * Written by: Steven M. Rubin
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.project;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.tool.io.FileType;

import java.io.Serializable;


/**
 * Class to describe cells checked into the Project Management system.
 */
public class ProjectCell implements Serializable
{
	/** the actual cell (if known) */					private Cell     cell;
	/** name of the cell */								private String   cellName;
	/** cell view */									private View     cellView;
	/** cell version */									private int      cellVersion;
	/** the type of the library file with this cell */	private FileType libType;
	/** true if this is the latest version */			private boolean  latestVersion;
	/** date of cell's checkin */						private String   checkInDate;
	/** current owner of this cell (if checked out) */	private String   owner;
	/** previous owner of this cell (if checked in) */	private String   lastOwner;
	/** comments for this cell */						private String   comment;
	/** library that contains this cell */				private ProjectLibrary projLib;

	ProjectCell(Cell cell, ProjectLibrary pl)
	{
		this.cell = cell;
		if (cell != null)
		{
			this.cellName = cell.getName();
			this.cellView = cell.getView();
			this.cellVersion = cell.getVersion();
		} else
		{
			this.cellName = "";
			this.cellView = View.UNKNOWN;
		}
		this.latestVersion = true;
		this.owner = "";
		this.lastOwner = "";
		this.projLib = pl;
		this.libType = FileType.JELIB;
		pl.addProjectCell(this);
	}

	Cell getCell() { return cell; }

	void setCell(Cell cell) { this.cell = cell; }

	String getCellName() { return cellName; }

	void setCellName(String cellName) { this.cellName = cellName; }

	View getView() { return cellView; }

	void setView(View cellView) { this.cellView = cellView; }

	int getVersion() { return cellVersion; }

	void setVersion(int version) { this.cellVersion = version; }

	FileType getLibType() { return libType; }

	String getLibExtension() { return libType.getExtensions()[0]; }

	void setLibType(FileType libType) { this.libType = libType; }

	boolean isLatestVersion() { return this.latestVersion; }

	void setLatestVersion(boolean latestVersion) { this.latestVersion = latestVersion; }

	String getOwner() { return owner; }

	void setOwner(String owner) { this.owner = owner; }

	String getLastOwner() { return lastOwner; }

	void setLastOwner(String lastOwner) { this.lastOwner = lastOwner; }

	String getComment() { return comment; }

	void setComment(String comment) { this.comment = comment; }

	String getCheckInDate() { return checkInDate; }

	void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }

	ProjectLibrary getProjectLibrary() { return projLib; }

	String describe()
	{
		String cn = cellName;
		if (cellView != View.UNKNOWN) cn += "{" + cellView.getAbbreviation() + "}";
		return cn;
	}

	String describeWithVersion()
	{
		String cn = cellName + ";" + cellVersion;
		if (cellView != View.UNKNOWN) cn += "{" + cellView.getAbbreviation() + "}";
		return cn;
	}
}

