/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Maxwell.java
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.User;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class to generate Maxwell netlists.
 */
public class Maxwell extends Output
{	
	HashMap maxNetMap;
	int boxNumber;

	/**
	 * Main entry point for Maxwell output.
	 * @param cell the top-level cell to write.
	 * @param filePath the name of the file to create.
	 */
	public static void writeMaxwellFile(Cell cell, VarContext context, String filePath)
	{
		Maxwell out = new Maxwell();
		if (out.openTextOutputStream(filePath)) return;
		out.initialize(cell);

		// enumerate the hierarchy below here
		Visitor wcVisitor = new Visitor(out);
		HierarchyEnumerator.enumerateCell(cell, context, null, wcVisitor);

		out.terminate();
		if (out.closeTextOutputStream()) return;
		System.out.println(filePath + " written");
	}

	/**
	 * Creates a new instance of Maxwell
	 */
	Maxwell()
	{
	}

	private void initialize(Cell cell)
	{
		maxNetMap = new HashMap();
		boxNumber = 1;

		printWriter.print("# Maxwell netlist for cell " + cell.noLibDescribe() + " from library " + cell.getLibrary().getLibName() + "\n");
		if (User.isIncludeDateAndVersionInOutput())
		{
			SimpleDateFormat sdf = new SimpleDateFormat("EEE MMMM dd, yyyy HH:mm:ss");
			printWriter.print("# CELL CREATED ON " + sdf.format(cell.getCreationDate()) + "\n");
			printWriter.print("# LAST REVISED ON " + sdf.format(cell.getRevisionDate()) + "\n");
			printWriter.print("# Generated automatically by the Electric VLSI Design System, version " + Version.CURRENT + "\n");
			printWriter.print("# WRITTEN ON " + sdf.format(new Date()) + "\n");
		} else
		{
			printWriter.print("# Generated automatically by the Electric VLSI Design System\n");
		}
		printWriter.print("\n");
		emitCopyright("# ", "");
	}

	private void terminate()
	{
		for(Iterator it = maxNetMap.keySet().iterator(); it.hasNext(); )
		{
			Integer index = (Integer)it.next();
			List layerList = (List)maxNetMap.get(index);
			if (layerList.size() <= 1) continue;
			printWriter.print("Unite {");
			boolean first = true;
			for(Iterator lIt = layerList.iterator(); lIt.hasNext(); )
			{
				Integer boxNum = (Integer)lIt.next();
				if (first) first = false; else
					printWriter.print(" ");
				printWriter.print("\"Box-" + boxNum + "\"");
			}
			printWriter.print("}\n");
		}
	}

	private void writePolygon(Poly poly, int globalNetNum)
	{
		Layer layer = poly.getLayer();
		if (layer.getTechnology() != Technology.getCurrent()) return;
		if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) return;
		Rectangle2D box = poly.getBox();
		if (box == null) return;
		Color color = layer.getGraphics().getColor();
		int red = color.getRed();
		int green = color.getGreen();
		int blue = color.getBlue();
		printWriter.print("NewObjColor " + red + " " + green + " " + blue + "\n");

		// find this network object
		Integer index = new Integer(globalNetNum);
		List layerList = (List)maxNetMap.get(index);
		if (layerList == null)
		{
			layerList = new ArrayList();
			maxNetMap.put(index, layerList);
		}
		printWriter.print("Box pos3 " + box.getMinX() + " " + box.getMinY() + " " + layer.getName() + "-Bot   " +
			box.getWidth() + " " + box.getHeight() + " " + layer.getName() + "-Hei \"Box-" + boxNumber + "\"\n");
		layerList.add(new Integer(boxNumber));
		boxNumber++;
	}

    public static class Visitor extends HierarchyEnumerator.Visitor
    {
		Maxwell generator;

        public Visitor(Maxwell generator)
        {
			this.generator = generator;
        }

        public boolean enterCell(HierarchyEnumerator.CellInfo info) 
        {
			// emit all node polygons
			Netlist netList = info.getNetlist();
			for(Iterator it = info.getCell().getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				NodeProto np = ni.getProto();
				if (!(np instanceof PrimitiveNode)) continue;
				AffineTransform transRot = ni.rotateOut();
				Technology tech = np.getTechnology();
				Poly [] polyList = tech.getShapeOfNode(ni, null, true, false);
				int tot = polyList.length;
				for(int i=0; i<tot; i++)
				{
					Poly poly = polyList[i];
					if (poly.getPort() == null) continue;
					poly.transform(transRot);
					poly.transform(info.getTransformToRoot());

					PortInst pi = ni.findPortInstFromProto(poly.getPort());
					JNetwork net = netList.getNetwork(pi);
					if (net == null) continue;
					int globalNetNum = info.getNetID(net);
					generator.writePolygon(poly, globalNetNum);
				}
			}

			// emit all arc polygons
			for(Iterator it = info.getCell().getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				Technology tech = ai.getProto().getTechnology();
				Poly [] polyList = tech.getShapeOfArc(ai);
				int tot = polyList.length;
				for(int i=0; i<tot; i++)
				{
					Poly poly = polyList[i];
					poly.transform(info.getTransformToRoot());
					JNetwork net = netList.getNetwork(ai, 0);
					int globalNetNum = info.getNetID(net);
					generator.writePolygon(poly, globalNetNum);
				}
			}
            return true;
        }

        public void exitCell(HierarchyEnumerator.CellInfo info) 
        {
		}

        public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) 
        {
            return true;
        }
    }

}
