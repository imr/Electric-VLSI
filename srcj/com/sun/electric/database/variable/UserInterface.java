/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UserInterface.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.variable;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Font;
import java.awt.Point;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * This interface provides information from the user interface.
 */
public interface UserInterface {

	public EditWindow_ getCurrentEditWindow_();
	public EditWindow_ needCurrentEditWindow_();
	public Cell getCurrentCell();
	public Cell needCurrentCell();
	public void repaintAllEditWindows();
	public void alignToGrid(Point2D pt);
	public int getDefaultTextSize();
	public EditWindow_ displayCell(Cell cell);
}
