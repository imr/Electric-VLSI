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
import com.sun.electric.tool.user.ui.Button;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.ui.UIEditFrame;
import com.sun.electric.tool.user.ui.UIMenu;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.UITopLevel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.JMenuBar;

/**
 * This class initializes the User Interface.
 * It is the main class of Electric.
 */
public final class UserInitial
{
	// ------------------------- private data ----------------------------

	public static void main(String[] args)
	{
		// initialize the display
		UITopLevel.Initialize();
       
		// setup the File menu
		UIMenu fileMenu = UIMenu.CreateUIMenu("File", 'F');
		fileMenu.addMenuItem("Open", KeyStroke.getKeyStroke('O', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.openLibraryCommand(); } });
		fileMenu.addMenuItem("Import Readable Dump", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.importLibraryCommand(); } });
		fileMenu.addMenuItem("Save", KeyStroke.getKeyStroke('S', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.saveLibraryCommand(); } });
		fileMenu.addMenuItem("Save as...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.saveAsLibraryCommand(); } });
		fileMenu.addSeparator();
		fileMenu.addMenuItem("Quit", KeyStroke.getKeyStroke('Q', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.quitCommand(); } });

		// setup the Edit menu
		UIMenu editMenu = UIMenu.CreateUIMenu("Edit", 'E');
		editMenu.addMenuItem("Undo", KeyStroke.getKeyStroke('Z', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.undoCommand(); } });
		editMenu.addMenuItem("Redo", KeyStroke.getKeyStroke('Y', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.redoCommand(); } });
		editMenu.addMenuItem("Show Undo List", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.showUndoListCommand(); } });

		// setup the Cell menu
		UIMenu cellMenu = UIMenu.CreateUIMenu("Cell", 'C');
        cellMenu.addMenuItem("Down Hierarchy", KeyStroke.getKeyStroke('D', InputEvent.CTRL_MASK),
            new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.downHierCommand(); }});
        cellMenu.addMenuItem("Up Hierarchy", KeyStroke.getKeyStroke('U', InputEvent.CTRL_MASK),
            new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.upHierCommand(); }});
		cellMenu.addMenuItem("Show Cell Groups", KeyStroke.getKeyStroke('T', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.showCellGroupsCommand(); } });

		// setup the Export menu
		UIMenu exportMenu = UIMenu.CreateUIMenu("Export", 'X');

		// setup the View menu
		UIMenu viewMenu = UIMenu.CreateUIMenu("View", 'V');

		// setup the Window menu
		UIMenu windowMenu = UIMenu.CreateUIMenu("Window", 'W');
		windowMenu.addMenuItem("Full Display", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.fullDisplayCommand(); } });
		windowMenu.addMenuItem("Toggle Grid", KeyStroke.getKeyStroke('G', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.toggleGridCommand(); } });

		// setup the Technology menu
		UIMenu technologyMenu = UIMenu.CreateUIMenu("Technology", 'H');

		// setup the Tool menu
		UIMenu toolMenu = UIMenu.CreateUIMenu("Tool", 'T');

		// setup the Help menu
		UIMenu helpMenu = UIMenu.CreateUIMenu("Help", 'H');

		// setup Steve's test menu
		UIMenu steveMenu = UIMenu.CreateUIMenu("Steve", 'S');
		steveMenu.addMenuItem("Get Info", KeyStroke.getKeyStroke('I', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.getInfoCommand(); } });
		steveMenu.addMenuItem("Show R-Tree", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.showRTreeCommand(); } });

		// setup Russell's test menu
		UIMenu russMenu = UIMenu.CreateUIMenu("Russell", 'R');
		russMenu.addMenuItem("ivanFlat", new com.sun.electric.rkao.IvanFlat());
		russMenu.addMenuItem("layout flat", new com.sun.electric.rkao.LayFlat());

		// setup Dima's test menu
		UIMenu dimaMenu = UIMenu.CreateUIMenu("Dima", 'D');
		dimaMenu.addMenuItem("redo Network Numbering", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.redoNetworkNumberingCommand(); } });
		dimaMenu.addMenuItem("test NodeInstsOterator", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.testNodeInstsIteratorCommand(); } });

        // setup JonGainsley's test menu
        UIMenu jongMenu = UIMenu.CreateUIMenu("JonG", 'J');
        jongMenu.addMenuItem("Describe Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.listVarsOnObject(); }});
        jongMenu.addMenuItem("Eval Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.evalVarsOnObject(); }});

            
		// create the menu bar
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(cellMenu);
		menuBar.add(exportMenu);
		menuBar.add(viewMenu);
		menuBar.add(windowMenu);
		menuBar.add(technologyMenu);
		menuBar.add(toolMenu);
		menuBar.add(helpMenu);
		menuBar.add(steveMenu);
		menuBar.add(russMenu);
		menuBar.add(dimaMenu);
        menuBar.add(jongMenu);
		UITopLevel.setMenuBar(menuBar);

		//create button
		Button openButton = Button.CreateButton(new ImageIcon("com/sun/electric/tool/user/icon11.gif"));
		//set button border style
		openButton.setButtonStyle(Button.STYLE_HOVER);
		//set mouse roll feature on
		openButton.setMouseHover(true);
		openButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.openLibraryCommand(); } });
		openButton.setToolTipText("Open");
	
		Button aButton = Button.CreateButton(new ImageIcon("com/sun/electric/tool/user/icon10.gif"));
		aButton.setButtonStyle(Button.STYLE_HOVER);
		aButton.setMouseHover(true);
		aButton.setToolTipText("test");
		
		//set an area for popup menu to be triggered within a button
		Insets insets = new Insets(22,22,32,32);
		JPopupMenu popup = new JPopupMenu();
		JMenuItem testItem = new JMenuItem("test 1");
		popup.add(testItem);
		testItem = new JMenuItem("test 2");
		popup.add(testItem);
		
		aButton.addPopupMenu(popup, insets);
		
		//add buttons to toolbar
		ToolBar toolbar = ToolBar.CreateToolBar();
		toolbar.add(openButton);
		toolbar.add(aButton);

		
		(UITopLevel.getTopLevel()).getContentPane().add(toolbar, BorderLayout.NORTH);
		
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
		Rectangle2D bounds = myCell.getBounds();
		NodeInst instance1Node = NodeInst.newInstance(myCell, new Point2D.Double(0, 0),
			myCell.getDefWidth(), myCell.getDefHeight(), 0, higherCell);
		instance1Node.setExpanded();
		NodeInst instance1UNode = NodeInst.newInstance(myCell, new Point2D.Double(0, 100),
			myCell.getDefWidth(), myCell.getDefHeight(), 0, higherCell);

		NodeInst instance2Node = NodeInst.newInstance(myCell, new Point2D.Double(100, 0),
			myCell.getDefWidth(), myCell.getDefHeight(), 900, higherCell);
		instance2Node.setExpanded();
		NodeInst instance2UNode = NodeInst.newInstance(myCell, new Point2D.Double(100, 100),
			myCell.getDefWidth(), myCell.getDefHeight(), 900, higherCell);

		NodeInst instance3Node = NodeInst.newInstance(myCell, new Point2D.Double(200, 0),
			myCell.getDefWidth(), myCell.getDefHeight(), 1800, higherCell);
		instance3Node.setExpanded();
		NodeInst instance3UNode = NodeInst.newInstance(myCell, new Point2D.Double(200, 100),
			myCell.getDefWidth(), myCell.getDefHeight(), 1800, higherCell);

		NodeInst instance4Node = NodeInst.newInstance(myCell, new Point2D.Double(300, 0),
			myCell.getDefWidth(), myCell.getDefHeight(), 2700, higherCell);
		instance4Node.setExpanded();
		NodeInst instance4UNode = NodeInst.newInstance(myCell, new Point2D.Double(300, 100),
			myCell.getDefWidth(), myCell.getDefHeight(), 2700, higherCell);

		// transposed
		NodeInst instance5Node = NodeInst.newInstance(myCell, new Point2D.Double(0, 200),
			-myCell.getDefWidth(), myCell.getDefHeight(), 0, higherCell);
		instance5Node.setExpanded();
		NodeInst instance5UNode = NodeInst.newInstance(myCell, new Point2D.Double(0, 300),
			-myCell.getDefWidth(), myCell.getDefHeight(), 0, higherCell);

		NodeInst instance6Node = NodeInst.newInstance(myCell, new Point2D.Double(100, 200),
			-myCell.getDefWidth(), myCell.getDefHeight(), 900, higherCell);
		instance6Node.setExpanded();
		NodeInst instance6UNode = NodeInst.newInstance(myCell, new Point2D.Double(100, 300),
			-myCell.getDefWidth(), myCell.getDefHeight(), 900, higherCell);

		NodeInst instance7Node = NodeInst.newInstance(myCell, new Point2D.Double(200, 200),
			-myCell.getDefWidth(), myCell.getDefHeight(), 1800, higherCell);
		instance7Node.setExpanded();
		NodeInst instance7UNode = NodeInst.newInstance(myCell, new Point2D.Double(200, 300),
			-myCell.getDefWidth(), myCell.getDefHeight(), 1800, higherCell);

		NodeInst instance8Node = NodeInst.newInstance(myCell, new Point2D.Double(300, 200),
			-myCell.getDefWidth(), myCell.getDefHeight(), 2700, higherCell);
		instance8Node.setExpanded();
		NodeInst instance8UNode = NodeInst.newInstance(myCell, new Point2D.Double(300, 300),
			-myCell.getDefWidth(), myCell.getDefHeight(), 2700, higherCell);

		PortInst instance1Port = instance1Node.findPortInst("in");
		PortInst instance2Port = instance1UNode.findPortInst("in");
		ArcInst instanceArc = ArcInst.newInstance(m1Proto, m1Proto.getWidth(), instance1Port, instance2Port);
		instanceArc.setFixedAngle();
		System.out.println("Created cell " + higherCell.describe());
		Undo.endChanges();

		
		// now up the hierarchy even farther
		Undo.startChanges(User.tool, "Build big{lay}");
		Cell bigCell = Cell.newInstance(mainLib, "big{lay}");
		int arraySize = 20;
		double cellWidth = myCell.getDefWidth();
		double cellHeight = myCell.getDefHeight();
		for(int y=0; y<arraySize; y++)
		{
			for(int x=0; x<arraySize; x++)
			{
				NodeInst instanceNode = NodeInst.newInstance(myCell, new Point2D.Double(x*(cellWidth+2), y*(cellHeight+2)),
					cellWidth, cellHeight, 0, bigCell);
				if ((x%2) == (y%2)) instanceNode.setExpanded();
				String theName = x + "," + y;
				Variable var = instanceNode.setName(theName);
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

		// display some cells
		UIEditFrame window1 = UIEditFrame.CreateEditWindow(myCell);
		System.out.println("************* Use SHIFT-CLICK to Pan *************");
		System.out.println("************* Use CONTROL-CLICK to Zoom *************");
	}

}
