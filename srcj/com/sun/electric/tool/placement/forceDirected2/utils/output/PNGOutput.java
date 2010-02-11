/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PNGOutput.java
 * Written by Team 7: Felix Schmidt, Daniel Lechner
 * 
 * This code has been developed at the Karlsruhe Institute of Technology (KIT), Germany, 
 * as part of the course "Multicore Programming in Practice: Tools, Models, and Languages".
 * Contact instructor: Dr. Victor Pankratius (pankratius@ipd.uka.de)
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.placement.forceDirected2.utils.output;

import com.sun.electric.tool.io.output.PNG;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Parallel Placement
 * 
 *         Draw a PNG image of the current placement
 */
public class PNGOutput {

	List<PlacementNode> nodes;
	List<PlacementNetwork> nets;

	public PNGOutput(List<PlacementNode> nodes, List<PlacementNetwork> nets) {

		this.nodes = nodes;
		this.nets = nets;
	}

	public void draw(String fileName) {
		BufferedImage image = new BufferedImage(3000, 3000, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphic = image.createGraphics();

		this.drawNodes(graphic, 0.7);

		PNG.writeImage(image, fileName);

	}

	private void drawNodes(Graphics2D graphic, double scale) {
		for (PlacementNode node : this.nodes) {
			double x = (node.getPlacementX() - node.getWidth() / 2) * scale + 300;
			double y = (node.getPlacementY() - node.getHeight() / 2) * scale + 300;
			graphic.drawRect((int) x, (int) y, (int) (node.getWidth() * scale), (int) (node.getHeight() * scale));
		}
	}

}
