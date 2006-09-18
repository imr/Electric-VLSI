/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EditWindow_.java
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
package com.sun.electric.database.variable;

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Network;
import com.sun.electric.tool.user.Highlight2;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Set;

/**
 * This interface gives a limited access to EditWindow_ necessary
 * for calculating a shape of some primitives.
 */
public interface EditWindow_ extends EditWindow0 {
    
	/**
	 * Method to return the cell that is shown in this window.
	 * @return the cell that is shown in this window.
	 */
	public Cell getCell();
    
    /**
     * Get the window's VarContext
     * @return the current VarContext
     */
    public VarContext getVarContext();
    
	/**
	 * Method to return the scale factor for this window.
	 * @return the scale factor for this window.
	 */
	public double getScale();

	// *************************************************** NEW METHODS FROM STEVE ***************************************************

	public Point getScreenLocationOfCorner();
	public Rectangle2D getDisplayedBounds();
	public Point2D getOffset();
	public void setOffset(Point2D off);
	public void setScale(double scale);
	public void fillScreen();
	public Rectangle2D getBoundsInWindow();
	public Point databaseToScreen(double dbX, double dbY);
	public void repaintContents(Rectangle2D bounds, boolean fullInstantiate);

	public boolean isGrid();
	public double getGridXSpacing();
	public double getGridYSpacing();

	// highlighting methods
//	public Highlighter getHighlighter();
	public void addElectricObject(ElectricObject ni, Cell cell);
	public Rectangle2D getHighlightedArea();
	public void addHighlightArea(Rectangle2D pointRect, Cell cell);
	public void addHighlightLine(Point2D pt1, Point2D pt2, Cell cell, boolean thick);
	public void addHighlightText(ElectricObject eobj, Cell cell, Variable.Key varKey);
	public ElectricObject getOneElectricObject(Class clz);
	public void clearHighlighting();
	public void finishedHighlighting();
	public void setHighlightOffset(int dX, int dY);
	public List<Geometric> getHighlightedEObjs(boolean wantNodes, boolean wantArcs);
	public Set<Network> getHighlightedNetworks();
	public Point2D getHighlightOffset();
	public List<Highlight2> saveHighlightList();
	public void restoreHighlightList(List<Highlight2> list);
}
