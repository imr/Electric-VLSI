/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UserInitial.java
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
package com.sun.electric.tool.user;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;



/**
 * This class initializes the User Interface.
 * It is the main class of Electric.
 */
public final class UserInitial
{
	public static void main(String[] args)
	{
		// initialize the display
		TopLevel.Initialize();

		// initialize all of the technologies
		Technology.initAllTechnologies();

		// initialize all of the tools
		Tool.initAllTools();

		// create the first library
		Library mainLib = Library.newInstance("noname", null);
		Library.setCurrent(mainLib);

		// test code to make and show something
		makeFakeCircuitry();
	}

	// ---------------------- THE TOOLBAR -----------------

	private static void makeFakeCircuitry()
	{
		// get information about the nodes
		NodeProto m1m2Proto = NodeProto.findNodeProto("mocmos:Metal-1-Metal-2-Con");
		NodeProto m2PinProto = NodeProto.findNodeProto("mocmos:Metal-2-Pin");
		NodeProto p1PinProto = NodeProto.findNodeProto("mocmos:Polysilicon-1-Pin");
		NodeProto m1PolyConProto = NodeProto.findNodeProto("mocmos:Metal-1-Polysilicon-1-Con");
		NodeProto pTransProto = NodeProto.findNodeProto("mocmos:P-Transistor");
		NodeProto nTransProto = NodeProto.findNodeProto("mocmos:N-Transistor");
		NodeProto cellCenterProto = NodeProto.findNodeProto("generic:Facet-Center");
		NodeProto invisiblePinProto = NodeProto.findNodeProto("generic:Invisible-Pin");

		// get information about the arcs
		ArcProto m1Proto = ArcProto.findArcProto("mocmos:Metal-1");
		ArcProto m2Proto = ArcProto.findArcProto("mocmos:Metal-2");
		ArcProto p1Proto = ArcProto.findArcProto("mocmos:Polysilicon-1");

		// get the current library
		Library mainLib = Library.getCurrent();

		// create a layout cell in the library
		Undo.startChanges(User.tool, "Build test{lay}");
		Cell myCell = Cell.newInstance(mainLib, "test{lay}");
		NodeInst cellCenter = NodeInst.newInstance(cellCenterProto, new Point2D.Double(30.0, 30.0), cellCenterProto.getDefWidth(), cellCenterProto.getDefHeight(), 0, myCell);
		cellCenter.setVisInside();
		cellCenter.setHardSelect();
		NodeInst metal12Via = NodeInst.newInstance(m1m2Proto, new Point2D.Double(-20.0, 20.0), m1m2Proto.getDefWidth(), m1m2Proto.getDefHeight(), 0, myCell);
		NodeInst contactNode = NodeInst.newInstance(m1PolyConProto, new Point2D.Double(20.0, 20.0), m1PolyConProto.getDefWidth(), m1PolyConProto.getDefHeight(), 0, myCell);
		NodeInst metal2Pin = NodeInst.newInstance(m2PinProto, new Point2D.Double(-20.0, 10.0), m2PinProto.getDefWidth(), m2PinProto.getDefHeight(), 0, myCell);
		NodeInst poly1Pin = NodeInst.newInstance(p1PinProto, new Point2D.Double(20.0, -20.0), p1PinProto.getDefWidth(), p1PinProto.getDefHeight(), 0, myCell);
		NodeInst transistor = NodeInst.newInstance(pTransProto, new Point2D.Double(0.0, -20.0), pTransProto.getDefWidth(), pTransProto.getDefHeight(), 0, myCell);
		NodeInst rotTrans = NodeInst.newInstance(nTransProto, new Point2D.Double(0.0, 0.0), nTransProto.getDefWidth(), nTransProto.getDefHeight(), 450, myCell);
		if (metal12Via == null || contactNode == null || metal2Pin == null || poly1Pin == null || transistor == null || rotTrans == null) return;
		rotTrans.setName("rotated");

		// make arcs to connect them
		PortInst m1m2Port = metal12Via.getOnlyPortInst();
		PortInst contactPort = contactNode.getOnlyPortInst();
		PortInst m2Port = metal2Pin.getOnlyPortInst();
		PortInst p1Port = poly1Pin.getOnlyPortInst();
		PortInst transRPort = transistor.findPortInst("p-trans-poly-right");
		ArcInst metal2Arc = ArcInst.newInstance(m2Proto, m2Proto.getWidth(), m2Port, m1m2Port);
		if (metal2Arc == null) return;
		metal2Arc.setFixedAngle();
		ArcInst metal1Arc = ArcInst.newInstance(m1Proto, m1Proto.getWidth(), contactPort, m1m2Port);
		if (metal1Arc == null) return;
		metal1Arc.setFixedAngle();
		ArcInst polyArc = ArcInst.newInstance(p1Proto, p1Proto.getWidth(), contactPort, p1Port);
		if (polyArc == null) return;
		polyArc.setFixedAngle();
		ArcInst polyArc2 = ArcInst.newInstance(p1Proto, p1Proto.getWidth(), transRPort, p1Port);
		if (polyArc2 == null) return;
		polyArc2.setFixedAngle();

		// export the two pins
		Export m1Export = Export.newInstance(myCell, m1m2Port, "in");
		m1Export.setCharacteristic(PortProto.Characteristic.IN);
		Export p1Export = Export.newInstance(myCell, p1Port, "out");
		p1Export.setCharacteristic(PortProto.Characteristic.OUT);
		System.out.println("Created cell " + myCell.describe());
		Undo.endChanges();


		// now up the hierarchy
		Undo.startChanges(User.tool, "Build higher{lay}");
		Cell higherCell = Cell.newInstance(mainLib, "higher{lay}");
		NodeInst higherCellCenter = NodeInst.newInstance(cellCenterProto, new Point2D.Double(0.0, 0.0), cellCenterProto.getDefWidth(), cellCenterProto.getDefHeight(), 0, higherCell);
		higherCellCenter.setVisInside();
		higherCellCenter.setHardSelect();
		Rectangle2D bounds = myCell.getBounds();
		double myWidth = myCell.getDefWidth();
		double myHeight = myCell.getDefHeight();
		NodeInst instance1Node = NodeInst.newInstance(myCell, new Point2D.Double(0, 0), myWidth, myHeight, 0, higherCell);
		instance1Node.setExpanded();
		NodeInst instance1UNode = NodeInst.newInstance(myCell, new Point2D.Double(0, 100), myWidth, myHeight, 0, higherCell);

		NodeInst instance2Node = NodeInst.newInstance(myCell, new Point2D.Double(100, 0), myWidth, myHeight, 900, higherCell);
		instance2Node.setExpanded();
		NodeInst instance2UNode = NodeInst.newInstance(myCell, new Point2D.Double(100, 100), myWidth, myHeight, 900, higherCell);

		NodeInst instance3Node = NodeInst.newInstance(myCell, new Point2D.Double(200, 0), myWidth, myHeight, 1800, higherCell);
		instance3Node.setExpanded();
		NodeInst instance3UNode = NodeInst.newInstance(myCell, new Point2D.Double(200, 100), myWidth, myHeight, 1800, higherCell);

		NodeInst instance4Node = NodeInst.newInstance(myCell, new Point2D.Double(300, 0), myWidth, myHeight, 2700, higherCell);
		instance4Node.setExpanded();
		NodeInst instance4UNode = NodeInst.newInstance(myCell, new Point2D.Double(300, 100), myWidth, myHeight, 2700, higherCell);

		// transposed
		NodeInst instance5Node = NodeInst.newInstance(myCell, new Point2D.Double(0, 200), -myWidth, myHeight, 0, higherCell);
		instance5Node.setExpanded();
		NodeInst instance5UNode = NodeInst.newInstance(myCell, new Point2D.Double(0, 300), -myWidth, myHeight, 0, higherCell);

		NodeInst instance6Node = NodeInst.newInstance(myCell, new Point2D.Double(100, 200), -myWidth, myHeight, 900, higherCell);
		instance6Node.setExpanded();
		NodeInst instance6UNode = NodeInst.newInstance(myCell, new Point2D.Double(100, 300),  -myWidth, myHeight, 900, higherCell);

		NodeInst instance7Node = NodeInst.newInstance(myCell, new Point2D.Double(200, 200), -myWidth, myHeight, 1800, higherCell);
		instance7Node.setExpanded();
		NodeInst instance7UNode = NodeInst.newInstance(myCell, new Point2D.Double(200, 300), -myWidth, myHeight, 1800, higherCell);

		NodeInst instance8Node = NodeInst.newInstance(myCell, new Point2D.Double(300, 200), -myWidth, myHeight, 2700, higherCell);
		instance8Node.setExpanded();
		NodeInst instance8UNode = NodeInst.newInstance(myCell, new Point2D.Double(300, 300), -myWidth, myHeight, 2700, higherCell);

		PortInst instance1Port = instance1Node.findPortInst("in");
		PortInst instance2Port = instance1UNode.findPortInst("in");
		ArcInst instanceArc = ArcInst.newInstance(m1Proto, m1Proto.getWidth(), instance1Port, instance2Port);
		instanceArc.setFixedAngle();
		System.out.println("Created cell " + higherCell.describe());
		Undo.endChanges();


		// now a rotation test
		Undo.startChanges(User.tool, "Build rotationTest{lay}");
		Cell rotTestCell = Cell.newInstance(mainLib, "rotationTest{lay}");
		NodeInst rotTestCellCenter = NodeInst.newInstance(cellCenterProto, new Point2D.Double(0.0, 0.0), cellCenterProto.getDefWidth(), cellCenterProto.getDefHeight(), 0, rotTestCell);
		rotTestCellCenter.setVisInside();
		rotTestCellCenter.setHardSelect();
		NodeInst r0Node = NodeInst.newInstance(myCell, new Point2D.Double(0, 0), myWidth, myHeight, 0, rotTestCell);
		r0Node.setExpanded();
		NodeInst nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(0, -35), 0, 0, 0, rotTestCell);
		Variable var = nodeLabel.setVal("ART_message", "Rotated 0");
		var.setDisplay();   var.getTextDescriptor().setRelSize(10);

		NodeInst r90Node = NodeInst.newInstance(myCell, new Point2D.Double(100, 0), myWidth, myHeight, 900, rotTestCell);
		r90Node.setExpanded();
		nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(100, -35), 0, 0, 0, rotTestCell);
		var = nodeLabel.setVal("ART_message", "Rotated 90");
		var.setDisplay();   var.getTextDescriptor().setRelSize(10);

		NodeInst r180Node = NodeInst.newInstance(myCell, new Point2D.Double(200, 0), myWidth, myHeight, 1800, rotTestCell);
		r180Node.setExpanded();
		nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(200, -35), 0, 0, 0, rotTestCell);
		var = nodeLabel.setVal("ART_message", "Rotated 180");
		var.setDisplay();   var.getTextDescriptor().setRelSize(10);

		NodeInst r270Node = NodeInst.newInstance(myCell, new Point2D.Double(300, 0), myWidth, myHeight, 2700, rotTestCell);
		r270Node.setExpanded();
		nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(300, -35), 0, 0, 0, rotTestCell);
		var = nodeLabel.setVal("ART_message", "Rotated 270");
		var.setDisplay();   var.getTextDescriptor().setRelSize(10);

		// Mirrored in X
		NodeInst r0MXNode = NodeInst.newInstance(myCell, new Point2D.Double(0, 100), -myWidth, myHeight, 0, rotTestCell);
		r0MXNode.setExpanded();
		nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(0, 100-35), 0, 0, 0, rotTestCell);
		var = nodeLabel.setVal("ART_message", "Rotated 0 MX");
		var.setDisplay();   var.getTextDescriptor().setRelSize(10);

		NodeInst r90MXNode = NodeInst.newInstance(myCell, new Point2D.Double(100, 100), -myWidth, myHeight, 900, rotTestCell);
		r90MXNode.setExpanded();
		nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(100, 100-35), 0, 0, 0, rotTestCell);
		var = nodeLabel.setVal("ART_message", "Rotated 90 MX");
		var.setDisplay();   var.getTextDescriptor().setRelSize(10);

		NodeInst r180MXNode = NodeInst.newInstance(myCell, new Point2D.Double(200, 100), -myWidth, myHeight, 1800, rotTestCell);
		r180MXNode.setExpanded();
		nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(200, 100-35), 0, 0, 0, rotTestCell);
		var = nodeLabel.setVal("ART_message", "Rotated 180 MX");
		var.setDisplay();   var.getTextDescriptor().setRelSize(10);

		NodeInst r270MXNode = NodeInst.newInstance(myCell, new Point2D.Double(300, 100), -myWidth, myHeight, 2700, rotTestCell);
		r270MXNode.setExpanded();
		nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(300, 100-35), 0, 0, 0, rotTestCell);
		var = nodeLabel.setVal("ART_message", "Rotated 270 MX");
		var.setDisplay();   var.getTextDescriptor().setRelSize(10);

		// Mirrored in Y
		NodeInst r0MYNode = NodeInst.newInstance(myCell, new Point2D.Double(0, 200), myWidth, -myHeight, 0, rotTestCell);
		r0MYNode.setExpanded();
		nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(0, 200-35), 0, 0, 0, rotTestCell);
		var = nodeLabel.setVal("ART_message", "Rotated 0 MY");
		var.setDisplay();   var.getTextDescriptor().setRelSize(10);

		NodeInst r90MYNode = NodeInst.newInstance(myCell, new Point2D.Double(100, 200), myWidth, -myHeight, 900, rotTestCell);
		r90MYNode.setExpanded();
		nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(100, 200-35), 0, 0, 0, rotTestCell);
		var = nodeLabel.setVal("ART_message", "Rotated 90 MY");
		var.setDisplay();   var.getTextDescriptor().setRelSize(10);

		NodeInst r180MYNode = NodeInst.newInstance(myCell, new Point2D.Double(200, 200), myWidth, -myHeight, 1800, rotTestCell);
		r180MYNode.setExpanded();
		nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(200, 200-35), 0, 0, 0, rotTestCell);
		var = nodeLabel.setVal("ART_message", "Rotated 180 MY");
		var.setDisplay();   var.getTextDescriptor().setRelSize(10);

		NodeInst r270MYNode = NodeInst.newInstance(myCell, new Point2D.Double(300, 200), myWidth, -myHeight, 2700, rotTestCell);
		r270MYNode.setExpanded();
		nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(300, 200-35), 0, 0, 0, rotTestCell);
		var = nodeLabel.setVal("ART_message", "Rotated 270 MY");
		var.setDisplay();   var.getTextDescriptor().setRelSize(10);

		// Mirrored in X and Y
		NodeInst r0MXYNode = NodeInst.newInstance(myCell, new Point2D.Double(0, 300), -myWidth, -myHeight, 0, rotTestCell);
		r0MXYNode.setExpanded();
		nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(0, 300-35), 0, 0, 0, rotTestCell);
		var = nodeLabel.setVal("ART_message", "Rotated 0 MXY");
		var.setDisplay();   var.getTextDescriptor().setRelSize(10);

		NodeInst r90MXYNode = NodeInst.newInstance(myCell, new Point2D.Double(100, 300), -myWidth, -myHeight, 900, rotTestCell);
		r90MXYNode.setExpanded();
		nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(100, 300-35), 0, 0, 0, rotTestCell);
		var = nodeLabel.setVal("ART_message", "Rotated 90 MXY");
		var.setDisplay();   var.getTextDescriptor().setRelSize(10);

		NodeInst r180MXYNode = NodeInst.newInstance(myCell, new Point2D.Double(200, 300), -myWidth, -myHeight, 1800, rotTestCell);
		r180MXYNode.setExpanded();
		nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(200, 300-35), 0, 0, 0, rotTestCell);
		var = nodeLabel.setVal("ART_message", "Rotated 180 MXY");
		var.setDisplay();   var.getTextDescriptor().setRelSize(10);

		NodeInst r270MXYNode = NodeInst.newInstance(myCell, new Point2D.Double(300, 300), -myWidth, -myHeight, 2700, rotTestCell);
		r270MXYNode.setExpanded();
		nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(300, 300-35), 0, 0, 0, rotTestCell);
		var = nodeLabel.setVal("ART_message", "Rotated 270 MXY");
		var.setDisplay();   var.getTextDescriptor().setRelSize(10);

		System.out.println("Created cell " + rotTestCell.describe());
		Undo.endChanges();

		
		// now up the hierarchy even farther
		Undo.startChanges(User.tool, "Build big{lay}");
		Cell bigCell = Cell.newInstance(mainLib, "big{lay}");
		NodeInst bigCellCenter = NodeInst.newInstance(cellCenterProto, new Point2D.Double(0.0, 0.0), cellCenterProto.getDefWidth(), cellCenterProto.getDefHeight(), 0, bigCell);
		bigCellCenter.setVisInside();
		bigCellCenter.setHardSelect();
		int arraySize = 20;
		for(int y=0; y<arraySize; y++)
		{
			for(int x=0; x<arraySize; x++)
			{
				NodeInst instanceNode = NodeInst.newInstance(myCell, new Point2D.Double(x*(myWidth+2), y*(myHeight+2)),
					myWidth, myHeight, 0, bigCell);
				if ((x%2) == (y%2)) instanceNode.setExpanded();
				String theName = x + "," + y;
				var = instanceNode.setName(theName);
				if (var != null)
				{
					TextDescriptor td = var.getTextDescriptor();
					td.setOff(0, 32);
				}
			}
		}
		System.out.println("Created cell " + bigCell.describe());
		Undo.endChanges();

		// show some stuff
//		instance1Node.getInfo();
//		instance2Node.getInfo();
//		instanceArc.getInfo();

		// display a cell
		WindowFrame window1 = WindowFrame.createEditWindow(myCell);
	}

}
