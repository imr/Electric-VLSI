/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DebugMenus.java
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

package com.sun.electric.tool.user.menus;

import com.sun.electric.database.Cell_;
import com.sun.electric.database.DatabaseChangeThread;
import com.sun.electric.database.ImmutableCell;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.NodeInst_;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.*;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.NodeUsage;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.extract.LayerCoverageJob;
import com.sun.electric.tool.erc.ERCWellCheck;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.logicaleffort.LENetlister1;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.interval.Diode;
import com.sun.electric.tool.user.*;
import com.sun.electric.tool.user.dialogs.ExecDialog;
import com.sun.electric.tool.user.ui.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.*;

/**
 * Class to handle the commands in the debugging pulldown menus.
 */
public class DebugMenus {

    protected static void addDebugMenus(MenuBar menuBar, MenuBar.Menu helpMenu) {
        MenuBar.MenuItem m;

		/****************************** ADDITIONS TO THE HELP MENU ******************************/

		helpMenu.addSeparator();

		helpMenu.addMenuItem("Make fake circuitry MoCMOS", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { makeFakeCircuitryCommand("mocmos", true); } });
		if (Technology.getTSMC90Technology() != null)
			helpMenu.addMenuItem("Make fake circuitry TSMC90", null,
					new ActionListener() { public void actionPerformed(ActionEvent e) { makeFakeCircuitryCommand("tsmc90", true); } });
		helpMenu.addMenuItem("Make fake analog simulation window", null,
				new ActionListener() { public void actionPerformed(ActionEvent e) { makeFakeWaveformCommand(); }});
		helpMenu.addMenuItem("Make fake interval simulation window", null,
				new ActionListener() { public void actionPerformed(ActionEvent e) { makeFakeIntervalWaveformCommand(); }});

        /****************************** Russell's TEST MENU ******************************/

        MenuBar.Menu russMenu = new MenuBar.Menu("Russell", 'R');
        menuBar.add(russMenu);
		russMenu.addMenuItem("Gate Generator Regression (MoCMOS)", null,
							 new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new com.sun.electric.tool.generator.layout.GateRegression(MoCMOS.tech);
			}
		});
		if (Technology.getTSMC90Technology() != null)
			russMenu.addMenuItem("Gate Generator Regression (TSMC90)", null,
				new ActionListener() { public void actionPerformed(ActionEvent e) { new com.sun.electric.tool.generator.layout.GateRegression(Technology.getTSMC90Technology()); } });
        russMenu.addMenuItem("create flat netlists for Ivan", null, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new com.sun.electric.tool.generator.layout.IvanFlat();
            }
        });
        russMenu.addMenuItem("layout flat", null, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new com.sun.electric.tool.generator.layout.LayFlat();
            }
        });
        russMenu.addMenuItem("Random Test", null, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new com.sun.electric.tool.generator.layout.Test();
            }
        });

        /****************************** Jon's TEST MENU ******************************/

        MenuBar.Menu jongMenu = new MenuBar.Menu("JonG", 'J');
        menuBar.add(jongMenu);
        jongMenu.addMenuItem("Describe Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { listVarsOnObject(false); }});
        jongMenu.addMenuItem("Describe Proto Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { listVarsOnObject(true); }});
        jongMenu.addMenuItem("Describe Current Library Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { listLibVars(); }});
        jongMenu.addMenuItem("Eval Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { evalVarsOnObject(); }});
        jongMenu.addMenuItem("LE test1", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { LENetlister1.test1(); }});
        jongMenu.addMenuItem("Display shaker", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { shakeDisplay(); }});
        jongMenu.addMenuItem("Run command", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { runCommand(); }});
        jongMenu.addMenuItem("Start defunct Job", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { startDefunctJob(); }});
        jongMenu.addMenuItem("Add String var", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { addStringVar(); }});
        jongMenu.addMenuItem("Edit clipboard", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { Clipboard.editClipboard(); }});
        jongMenu.addMenuItem("Cause stack overflow", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { causeStackOverflow(true, false, "blah", 234, "xvsdf"); }});
        jongMenu.addMenuItem("Cause stack overflow in Job", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { causeStackOverflowJob(); }});
        jongMenu.addMenuItem("Time method calls", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { timeMethodCalls(); }});
        jongMenu.addMenuItem("Delete layout cells in current library", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { deleteCells(View.LAYOUT); }});
		if (Technology.getTSMC90Technology() != null)
			jongMenu.addMenuItem("fill generator 90nm test", null,
				new ActionListener() { public void actionPerformed(ActionEvent e) { invokeTSMC90FillGenerator(); }});

        /****************************** Gilda's TEST MENU ******************************/

        MenuBar.Menu gildaMenu = new MenuBar.Menu("Gilda", 'G');
        menuBar.add(gildaMenu);
        gildaMenu.addMenuItem("Test Bash", null,
                        new ActionListener() { public void actionPerformed(ActionEvent e) {testBash();}});
        gildaMenu.addMenuItem("3D View", null,
                        new ActionListener() { public void actionPerformed(ActionEvent e) {threeViewCommand();}});
        gildaMenu.addMenuItem("Parasitic", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ToolMenu.parasiticCommand(); } });
	    gildaMenu.addMenuItem("Check Wells Sweep", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ERCWellCheck.analyzeCurCell(GeometryHandler.ALGO_SWEEP); } });
	    gildaMenu.addMenuItem("Check Wells Orig", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ERCWellCheck.analyzeCurCell(GeometryHandler.ALGO_MERGE); } });
	    gildaMenu.addMenuItem("Check Wells QTree", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ERCWellCheck.analyzeCurCell(GeometryHandler.ALGO_QTREE); } });
	    gildaMenu.addMenuItem("List Geometry on Network SWEEP", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ToolMenu.listGeometryOnNetworkCommand(GeometryHandler.ALGO_SWEEP); } });
	    gildaMenu.addMenuItem("3D View", null,
                new ActionListener() { public void actionPerformed(ActionEvent e) { WindowMenu.create3DViewCommand(false); } });
        gildaMenu.addMenuItem("Merge Polyons qTree", null,
                new ActionListener() { public void actionPerformed(ActionEvent e) {CellMenu.layerCoverageCommand(Job.Type.CHANGE, LayerCoverageJob.MERGE, GeometryHandler.ALGO_QTREE);}});
        gildaMenu.addMenuItem("Merge Polyons Sweep", null,
                        new ActionListener() { public void actionPerformed(ActionEvent e) {CellMenu.layerCoverageCommand(Job.Type.CHANGE, LayerCoverageJob.MERGE, GeometryHandler.ALGO_SWEEP);}});
        gildaMenu.addMenuItem("Covering Implants qTree", null,
                new ActionListener() { public void actionPerformed(ActionEvent e) {CellMenu.layerCoverageCommand(Job.Type.CHANGE, LayerCoverageJob.IMPLANT, GeometryHandler.ALGO_QTREE);}});
        gildaMenu.addMenuItem("Covering Implants Sweep", null,
                        new ActionListener() { public void actionPerformed(ActionEvent e) {CellMenu.layerCoverageCommand(Job.Type.CHANGE, LayerCoverageJob.IMPLANT, GeometryHandler.ALGO_SWEEP);}});
        gildaMenu.addMenuItem("Covering Implants Old", null,
                new ActionListener() { public void actionPerformed(ActionEvent e) {implantGeneratorCommand(false, false);}});
        gildaMenu.addMenuItem("Generate Fake Nodes", null,
                new ActionListener() { public void actionPerformed(ActionEvent e) {genFakeNodes();}});
        gildaMenu.addMenuItem("List Layer Coverage", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { CellMenu.layerCoverageCommand(Job.Type.EXAMINE, LayerCoverageJob.AREA, GeometryHandler.ALGO_SWEEP); } });

        /****************************** Dima's TEST MENU ******************************/

        MenuBar.Menu dimaMenu = new MenuBar.Menu("Dima", 'D');
        menuBar.add(dimaMenu);
	    dimaMenu.addMenuItem("Plot diode", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { Diode.plotDiode(User.getWorkingDirectory() + File.separator + "diode.raw"); } });
	    dimaMenu.addMenuItem("Var stat", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { varStatistics(); } });
	    dimaMenu.addMenuItem("Transactional", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { transactionalTest(); } });
    }

	// ---------------------- For Regression Testing -----------------

    /**
     * Class to set a cell to be the current cell, done in a Job.
     * By encapsulating this simple operation in a Job, it gets done
     * in the proper order when scheduled by a regression test.
     */
    public static class SetCellJob extends Job
    {
    	private String cellName;

        public SetCellJob(String cellName)
        {
            super("Set current cell", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cellName = cellName;
            startJob();
        }

        public boolean doIt()
        {
    		Library lib = Library.getCurrent();
    		lib.setCurCell(lib.findNodeProto(cellName));
            return true;
        }
    }

    public static class SaveLibraryJob extends Job
    {
    	private String fileName;

    	public SaveLibraryJob(String fileName)
        {
            super("Save Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.fileName = fileName;
            startJob();
        }

        public boolean doIt()
        {
    		Library lib = Library.getCurrent();
    		Cell cell = lib.getCurCell();
    		cell.lowLevelSetRevisionDate(new Date(0));	// reset modification date for consistent output
    		URL outURL = TextUtils.makeURLToFile(fileName);
    		lib.setLibFile(outURL);
    		lib.setName(TextUtils.getFileNameWithoutExtension(outURL));
    		Output.writeLibrary(lib, FileType.JELIB, false);
            return true;
        }
    }

	// ---------------------- Help Menu additions -----------------

	public static void makeFakeCircuitryCommand(String tech, boolean asJob)
	{
		// test code to make and show something
        if (asJob)
        {
            MakeFakeCircuitry job = new MakeFakeCircuitry(tech);
        }
        else
            MakeFakeCircuitry.doItInternal(tech);
	}

	/**
	 * Class to read a library in a new thread.
	 */
	private static class MakeFakeCircuitry extends Job
	{
		private String theTechnology;

		protected MakeFakeCircuitry(String tech)
		{
			super("Make fake circuitry", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			theTechnology = tech;
			startJob();
		}

		public boolean doIt()
		{
            return (doItInternal(theTechnology));
        }

        /**
         * External static call for regressions
         * @param technology
         * @return
         */
		private static boolean doItInternal(String technology)
		{
			// get information about the nodes
			Technology  tech = Technology.findTechnology(technology);

			if (tech == null)
			{
				System.out.println("Technology not found in MakeFakeCircuitry");
				return (false);
			}
			tech.setCurrent();
			WindowFrame wf = WindowFrame.getCurrentWindowFrame(false);
			if (wf != null) wf.loadComponentMenuForTechnology();

			StringBuffer polyName = new StringBuffer("Polysilicon");
			String lateral = "top";

			if (technology.equals("mocmos"))
			{
				polyName.append("-1");
				lateral = "right";
			}

			NodeProto m1m2Proto = Cell.findNodeProto(technology+":Metal-1-Metal-2-Con");
			NodeProto m2PinProto = Cell.findNodeProto(technology+":Metal-2-Pin");
			NodeProto p1PinProto = Cell.findNodeProto(technology+":" + polyName + "-Pin");
			NodeProto m1PolyConProto = Cell.findNodeProto(technology+":Metal-1-" + polyName + "-Con");
			NodeProto pTransProto = Cell.findNodeProto(technology+":P-Transistor");
			NodeProto nTransProto = Cell.findNodeProto(technology+":N-Transistor");
			NodeProto invisiblePinProto = Cell.findNodeProto("generic:Invisible-Pin");

			// get information about the arcs
			ArcProto m1Proto = ArcProto.findArcProto(technology+":Metal-1");
			ArcProto m2Proto = ArcProto.findArcProto(technology+":Metal-2");
			ArcProto p1Proto = ArcProto.findArcProto(technology+":"+polyName);

			// get the current library
			Library mainLib = Library.getCurrent();

			// create a layout cell in the library
			Cell myCell = Cell.makeInstance(mainLib, technology+"test{lay}");
			NodeInst metal12Via = NodeInst.newInstance(m1m2Proto, new Point2D.Double(-20.0, 20.0), m1m2Proto.getDefWidth(), m1m2Proto.getDefHeight(), myCell);
			NodeInst contactNode = NodeInst.newInstance(m1PolyConProto, new Point2D.Double(20.0, 20.0), m1PolyConProto.getDefWidth(), m1PolyConProto.getDefHeight(), myCell);
			NodeInst metal2Pin = NodeInst.newInstance(m2PinProto, new Point2D.Double(-20.0, 10.0), m2PinProto.getDefWidth(), m2PinProto.getDefHeight(), myCell);
			NodeInst poly1PinA = NodeInst.newInstance(p1PinProto, new Point2D.Double(20.0, -20.0), p1PinProto.getDefWidth(), p1PinProto.getDefHeight(), myCell);
			NodeInst poly1PinB = NodeInst.newInstance(p1PinProto, new Point2D.Double(20.0, -10.0), p1PinProto.getDefWidth(), p1PinProto.getDefHeight(), myCell);
			NodeInst transistor = NodeInst.newInstance(pTransProto, new Point2D.Double(0.0, -20.0), pTransProto.getDefWidth(), pTransProto.getDefHeight(), myCell);
			NodeInst rotTrans = NodeInst.newInstance(nTransProto, new Point2D.Double(0.0, 10.0), nTransProto.getDefWidth(), nTransProto.getDefHeight(), myCell, 3150, "rotated", 0);
			if (metal12Via == null || contactNode == null || metal2Pin == null || poly1PinA == null ||
				poly1PinB == null || transistor == null || rotTrans == null) return false;

			// make arcs to connect them
			PortInst m1m2Port = metal12Via.getOnlyPortInst();
			PortInst contactPort = contactNode.getOnlyPortInst();
			PortInst m2Port = metal2Pin.getOnlyPortInst();
			PortInst p1PortA = poly1PinA.getOnlyPortInst();
			PortInst p1PortB = poly1PinB.getOnlyPortInst();
			PortInst transPortR = transistor.findPortInst("poly-" + lateral);
            // Old style
            if (transPortR == null) transPortR = transistor.findPortInst("p-trans-poly-" + lateral);
			PortInst transRPortR = rotTrans.findPortInst("poly-" + lateral);
            // Old style
            if (transRPortR == null) transRPortR = rotTrans.findPortInst("n-trans-poly-" + lateral);
			ArcInst metal2Arc = ArcInst.makeInstance(m2Proto, m2Proto.getWidth(), m2Port, m1m2Port);
			if (metal2Arc == null) return false;
			metal2Arc.setRigid(true);
			ArcInst metal1Arc = ArcInst.makeInstance(m1Proto, m1Proto.getWidth(), contactPort, m1m2Port);
			if (metal1Arc == null) return false;
			ArcInst polyArc1 = ArcInst.makeInstance(p1Proto, p1Proto.getWidth(), contactPort, p1PortB);
			if (polyArc1 == null) return false;
			ArcInst polyArc3 = ArcInst.makeInstance(p1Proto, p1Proto.getWidth(), p1PortB, p1PortA);
			if (polyArc3 == null) return false;
			ArcInst polyArc2 = ArcInst.makeInstance(p1Proto, p1Proto.getWidth(), transPortR, p1PortA);
			if (polyArc2 == null) return false;
			ArcInst polyArc4 = ArcInst.makeInstance(p1Proto, p1Proto.getWidth(), transRPortR, p1PortB);
			if (polyArc4 == null) return false;

			// export the two pins
			Export m1Export = Export.newInstance(myCell, m1m2Port, "in");
			m1Export.setCharacteristic(PortCharacteristic.IN);
			Export p1Export = Export.newInstance(myCell, p1PortA, "out");
			p1Export.setCharacteristic(PortCharacteristic.OUT);
			System.out.println("Created cell " + myCell.describe());


			// now up the hierarchy
			Cell higherCell = Cell.makeInstance(mainLib, "higher{lay}");
			Rectangle2D bounds = myCell.getBounds();
			double myWidth = myCell.getDefWidth();
			double myHeight = myCell.getDefHeight();
			NodeInst instance1Node = NodeInst.newInstance(myCell, new Point2D.Double(0, 0), myWidth, myHeight, higherCell);
			instance1Node.setExpanded();
			NodeInst instance1UNode = NodeInst.newInstance(myCell, new Point2D.Double(0, 100), myWidth, myHeight, higherCell);

			NodeInst instance2Node = NodeInst.newInstance(myCell, new Point2D.Double(100, 0), myWidth, myHeight, higherCell, 900, null, 0);
			instance2Node.setExpanded();
			NodeInst instance2UNode = NodeInst.newInstance(myCell, new Point2D.Double(100, 100), myWidth, myHeight, higherCell, 900, null, 0);

			NodeInst instance3Node = NodeInst.newInstance(myCell, new Point2D.Double(200, 0), myWidth, myHeight, higherCell, 1800, null, 0);
			instance3Node.setExpanded();
			NodeInst instance3UNode = NodeInst.newInstance(myCell, new Point2D.Double(200, 100), myWidth, myHeight, higherCell, 1800, null, 0);

			NodeInst instance4Node = NodeInst.newInstance(myCell, new Point2D.Double(300, 0), myWidth, myHeight, higherCell, 2700, null, 0);
			instance4Node.setExpanded();
			NodeInst instance4UNode = NodeInst.newInstance(myCell, new Point2D.Double(300, 100), myWidth, myHeight, higherCell, 2700, null, 0);

			// transposed
			NodeInst instance5Node = NodeInst.newInstance(myCell, new Point2D.Double(0, 200), -myWidth, myHeight, higherCell);
			instance5Node.setExpanded();
			NodeInst instance5UNode = NodeInst.newInstance(myCell, new Point2D.Double(0, 300), -myWidth, myHeight, higherCell);

			NodeInst instance6Node = NodeInst.newInstance(myCell, new Point2D.Double(100, 200), -myWidth, myHeight, higherCell, 900, null, 0);
			instance6Node.setExpanded();
			NodeInst instance6UNode = NodeInst.newInstance(myCell, new Point2D.Double(100, 300),  -myWidth, myHeight, higherCell, 900, null, 0);

			NodeInst instance7Node = NodeInst.newInstance(myCell, new Point2D.Double(200, 200), -myWidth, myHeight, higherCell, 1800, null, 0);
			instance7Node.setExpanded();
			NodeInst instance7UNode = NodeInst.newInstance(myCell, new Point2D.Double(200, 300), -myWidth, myHeight, higherCell, 1800, null, 0);

			NodeInst instance8Node = NodeInst.newInstance(myCell, new Point2D.Double(300, 200), -myWidth, myHeight, higherCell, 2700, null, 0);
			instance8Node.setExpanded();
			NodeInst instance8UNode = NodeInst.newInstance(myCell, new Point2D.Double(300, 300), -myWidth, myHeight, higherCell, 2700, null, 0);

			PortInst instance1Port = instance1Node.findPortInst("in");
			PortInst instance2Port = instance1UNode.findPortInst("in");
			ArcInst instanceArc = ArcInst.makeInstance(m1Proto, m1Proto.getWidth(), instance1Port, instance2Port);
			System.out.println("Created cell " + higherCell.describe());


			// now a rotation test
			Cell rotTestCell = Cell.makeInstance(mainLib, "rotationTest{lay}");
			NodeInst r0Node = NodeInst.newInstance(myCell, new Point2D.Double(0, 0), myWidth, myHeight, rotTestCell);
			r0Node.setExpanded();
			NodeInst nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(0, -35), 0, 0, rotTestCell);
			Variable var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 0");
			var.setDisplay(true);   var.setRelSize(10);

			NodeInst r90Node = NodeInst.newInstance(myCell, new Point2D.Double(100, 0), myWidth, myHeight, rotTestCell, 900, null, 0);
			r90Node.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(100, -35), 0, 0, rotTestCell);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 90");
			var.setDisplay(true);   var.setRelSize(10);

			NodeInst r180Node = NodeInst.newInstance(myCell, new Point2D.Double(200, 0), myWidth, myHeight, rotTestCell, 1800, null, 0);
			r180Node.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(200, -35), 0, 0, rotTestCell);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 180");
			var.setDisplay(true);   var.setRelSize(10);

			NodeInst r270Node = NodeInst.newInstance(myCell, new Point2D.Double(300, 0), myWidth, myHeight, rotTestCell, 2700, null, 0);
			r270Node.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(300, -35), 0, 0, rotTestCell);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 270");
			var.setDisplay(true);   var.setRelSize(10);

			// Mirrored in X
			NodeInst r0MXNode = NodeInst.newInstance(myCell, new Point2D.Double(0, 100), -myWidth, myHeight, rotTestCell);
			r0MXNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(0, 100-35), 0, 0, rotTestCell);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 0 MX");
			var.setDisplay(true);   var.setRelSize(10);

			NodeInst r90MXNode = NodeInst.newInstance(myCell, new Point2D.Double(100, 100), -myWidth, myHeight, rotTestCell, 900, null, 0);
			r90MXNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(100, 100-35), 0, 0, rotTestCell);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 90 MX");
			var.setDisplay(true);   var.setRelSize(10);

			NodeInst r180MXNode = NodeInst.newInstance(myCell, new Point2D.Double(200, 100), -myWidth, myHeight, rotTestCell, 1800, null, 0);
			r180MXNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(200, 100-35), 0, 0, rotTestCell);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 180 MX");
			var.setDisplay(true);   var.setRelSize(10);

			NodeInst r270MXNode = NodeInst.newInstance(myCell, new Point2D.Double(300, 100), -myWidth, myHeight, rotTestCell, 2700, null, 0);
			r270MXNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(300, 100-35), 0, 0, rotTestCell);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 270 MX");
			var.setDisplay(true);   var.setRelSize(10);

			// Mirrored in Y
			NodeInst r0MYNode = NodeInst.newInstance(myCell, new Point2D.Double(0, 200), myWidth, -myHeight, rotTestCell);
			r0MYNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(0, 200-35), 0, 0, rotTestCell);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 0 MY");
			var.setDisplay(true);   var.setRelSize(10);

			NodeInst r90MYNode = NodeInst.newInstance(myCell, new Point2D.Double(100, 200), myWidth, -myHeight, rotTestCell, 900, null, 0);
			r90MYNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(100, 200-35), 0, 0, rotTestCell);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 90 MY");
			var.setDisplay(true);   var.setRelSize(10);

			NodeInst r180MYNode = NodeInst.newInstance(myCell, new Point2D.Double(200, 200), myWidth, -myHeight, rotTestCell, 1800, null, 0);
			r180MYNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(200, 200-35), 0, 0, rotTestCell);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 180 MY");
			var.setDisplay(true);   var.setRelSize(10);

			NodeInst r270MYNode = NodeInst.newInstance(myCell, new Point2D.Double(300, 200), myWidth, -myHeight, rotTestCell, 2700, null, 0);
			r270MYNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(300, 200-35), 0, 0, rotTestCell);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 270 MY");
			var.setDisplay(true);   var.setRelSize(10);

			// Mirrored in X and Y
			NodeInst r0MXYNode = NodeInst.newInstance(myCell, new Point2D.Double(0, 300), -myWidth, -myHeight, rotTestCell);
			r0MXYNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(0, 300-35), 0, 0, rotTestCell);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 0 MXY");
			var.setDisplay(true);   var.setRelSize(10);

			NodeInst r90MXYNode = NodeInst.newInstance(myCell, new Point2D.Double(100, 300), -myWidth, -myHeight, rotTestCell, 900, null, 0);
			r90MXYNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(100, 300-35), 0, 0, rotTestCell);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 90 MXY");
			var.setDisplay(true);   var.setRelSize(10);

			NodeInst r180MXYNode = NodeInst.newInstance(myCell, new Point2D.Double(200, 300), -myWidth, -myHeight, rotTestCell, 1800, null, 0);
			r180MXYNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(200, 300-35), 0, 0, rotTestCell);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 180 MXY");
			var.setDisplay(true);   var.setRelSize(10);

			NodeInst r270MXYNode = NodeInst.newInstance(myCell, new Point2D.Double(300, 300), -myWidth, -myHeight, rotTestCell, 2700, null, 0);
			r270MXYNode.setExpanded();
			nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(300, 300-35), 0, 0, rotTestCell);
			var = nodeLabel.newVar(Artwork.ART_MESSAGE, "Rotated 270 MXY");
			var.setDisplay(true);   var.setRelSize(10);

			System.out.println("Created cell " + rotTestCell.describe());


			// now up the hierarchy even farther
			Cell bigCell = Cell.makeInstance(mainLib, "big{lay}");
			int arraySize = 20;
			for(int y=0; y<arraySize; y++)
			{
				for(int x=0; x<arraySize; x++)
				{
					String theName = "arr["+ x + "][" + y + "]";
					NodeInst instanceNode = NodeInst.newInstance(myCell, new Point2D.Double(x*(myWidth+2), y*(myHeight+2)),
						myWidth, myHeight, bigCell, 0, theName, 0);
					instanceNode.setOff(NodeInst.NODE_NAME_TD, 0, 8);
					if ((x%2) == (y%2)) instanceNode.setExpanded();
				}
			}
			System.out.println("Created cell " + bigCell.describe());

			// display a cell
			WindowFrame.createEditWindow(myCell);
			return true;
		}
	}

    public static void makeFakeCircuitryForCoverageCommand(String tech, boolean asJob)
	{
		// test code to make and show something
        if (asJob)
        {
            FakeCoverageCircuitry job = new FakeCoverageCircuitry(tech);
        }
        else
            FakeCoverageCircuitry.doItInternal(tech);
	}

    private static class FakeCoverageCircuitry extends Job
    {
        private String theTechnology;

        protected FakeCoverageCircuitry(String tech)
        {
            super("Make fake circuitry for coverage tests", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            theTechnology = tech;
            startJob();
        }

        public boolean doIt()
        {
            return (doItInternal(theTechnology));
        }

        private static boolean doItInternal(String technology)
		{
			// get information about the nodes
			Technology  tech = Technology.findTechnology(technology);

			if (tech == null)
			{
				System.out.println("Technology not found in createCoverageTestCells");
				return (false);
			}
			tech.setCurrent();
			WindowFrame wf = WindowFrame.getCurrentWindowFrame(false);
			if (wf != null) wf.loadComponentMenuForTechnology();

			NodeProto m1NodeProto = Cell.findNodeProto(technology+":Metal-1-Node");
            NodeProto m2NodeProto = Cell.findNodeProto(technology+":Metal-2-Node");
            NodeProto m3NodeProto = Cell.findNodeProto(technology+":Metal-3-Node");
            NodeProto m4NodeProto = Cell.findNodeProto(technology+":Metal-4-Node");

            NodeProto invisiblePinProto = Cell.findNodeProto("generic:Invisible-Pin");

			// get information about the arcs
			ArcProto m1ArcProto = ArcProto.findArcProto(technology+":Metal-1");

			// get the current library
			Library mainLib = Library.getCurrent();

			// create a layout cell in the library
			Cell m1Cell = Cell.makeInstance(mainLib, technology+"Metal1Test{lay}");
            NodeInst metal1Node = NodeInst.newInstance(m1NodeProto, new Point2D.Double(0, 0), m1NodeProto.getDefWidth(), m1NodeProto.getDefHeight(), m1Cell);

            // Two metals
            Cell myCell = Cell.makeInstance(mainLib, technology+"M1M2Test{lay}");
            NodeInst node = NodeInst.newInstance(m1NodeProto, new Point2D.Double(-m1NodeProto.getDefWidth()/2, -m1NodeProto.getDefHeight()/2),
                    m1NodeProto.getDefWidth(), m1NodeProto.getDefHeight(), myCell);
            node = NodeInst.newInstance(m2NodeProto, new Point2D.Double(-m2NodeProto.getDefWidth()/2, m2NodeProto.getDefHeight()/2),
                    m2NodeProto.getDefWidth(), m2NodeProto.getDefHeight(), myCell);
            node = NodeInst.newInstance(m3NodeProto, new Point2D.Double(m3NodeProto.getDefWidth()/2, -m3NodeProto.getDefHeight()/2),
                    m3NodeProto.getDefWidth(), m3NodeProto.getDefHeight(), myCell);
            node = NodeInst.newInstance(m4NodeProto, new Point2D.Double(m4NodeProto.getDefWidth()/2, m4NodeProto.getDefHeight()/2),
                    m4NodeProto.getDefWidth(), m4NodeProto.getDefHeight(), myCell);

			// now up the hierarchy
			Cell higherCell = Cell.makeInstance(mainLib, "higher{lay}");
			Rectangle2D bounds = myCell.getBounds();
			double myWidth = myCell.getDefWidth();
			double myHeight = myCell.getDefHeight();
			NodeInst instance1Node = NodeInst.newInstance(myCell, new Point2D.Double(0, 0), myWidth, myHeight, higherCell);
			instance1Node.setExpanded();

			NodeInst instance2Node = NodeInst.newInstance(myCell, new Point2D.Double(myWidth, 0), myWidth, myHeight, higherCell, 0, null, 0);
            //NodeInst instance2Node = NodeInst.newInstance(myCell, new Point2D.Double(myWidth, 0), myWidth, myHeight, higherCell, 900, null, 0);
			instance2Node.setExpanded();

			NodeInst instance3Node = NodeInst.newInstance(myCell, new Point2D.Double(2*myWidth, 0), myWidth, myHeight, higherCell, 1800, null, 0);
			instance3Node.setExpanded();

			NodeInst instance4Node = NodeInst.newInstance(myCell, new Point2D.Double(3*myWidth, 0), myWidth, myHeight, higherCell, 2700, null, 0);
			instance4Node.setExpanded();

			// transposed
			NodeInst instance5Node = NodeInst.newInstance(myCell, new Point2D.Double(0, myHeight), -myWidth, myHeight, higherCell);
			instance5Node.setExpanded();

			NodeInst instance6Node = NodeInst.newInstance(myCell, new Point2D.Double(myWidth, myHeight), -myWidth, myHeight, higherCell, 900, null, 0);
			instance6Node.setExpanded();

			NodeInst instance7Node = NodeInst.newInstance(myCell, new Point2D.Double(2*myWidth, myHeight), -myWidth, myHeight, higherCell, 1800, null, 0);
			instance7Node.setExpanded();

			NodeInst instance8Node = NodeInst.newInstance(myCell, new Point2D.Double(3*myWidth, myHeight), -myWidth, myHeight, higherCell, 2700, null, 0);
			instance8Node.setExpanded();

			System.out.println("Created cell " + higherCell.describe());

			// display a cell
			WindowFrame.createEditWindow(myCell);
            return (true);
		}
    }

	/**
	 * Test method to build an analog waveform with fake data.
	 */
	public static void makeFakeWaveformCommand()
	{
		// make the waveform data
		Simulation.SimData sd = new Simulation.SimData();
		double timeStep = 0.0000000001;
		sd.buildCommonTime(100);
		for(int i=0; i<100; i++)
			sd.setCommonTime(i, i * timeStep);
		for(int i=0; i<18; i++)
		{
			Simulation.SimAnalogSignal as = new Simulation.SimAnalogSignal(sd);
			as.setSignalName("Signal"+(i+1));
			as.buildValues(100);
			for(int k=0; k<100; k++)
			{
				as.setValue(k, Math.sin((k+i*10) / (2.0+i*2)) * 4);
			}
		}
		sd.setCell(null);

		// make the waveform window
		WindowFrame wf = WindowFrame.createWaveformWindow(sd);
		WaveformWindow ww = (WaveformWindow)wf.getContent();
		ww.setMainTimeCursor(timeStep*22);
		ww.setExtensionTimeCursor(timeStep*77);
		ww.setDefaultTimeRange(0, timeStep*100);

		// make some waveform panels and put signals in them
		for(int i=0; i<6; i++)
		{
			WaveformWindow.Panel wp = new WaveformWindow.Panel(ww, true);
			wp.setValueRange(-5, 5);
			for(int j=0; j<(i+1)*3; j++)
			{
				Simulation.SimAnalogSignal as = (Simulation.SimAnalogSignal)sd.getSignals().get(j);
				WaveformWindow.Signal wsig = new WaveformWindow.Signal(wp, as);
			}
		}
	}

	private static void makeFakeIntervalWaveformCommand()
	{
		// make the interval waveform data
		Simulation.SimData sd = new Simulation.SimData();
		double timeStep = 0.0000000001;
		sd.buildCommonTime(100);
		for(int i=0; i<100; i++)
			sd.setCommonTime(i, i * timeStep);
		for(int i=0; i<6; i++)
		{
			Simulation.SimAnalogSignal as = new Simulation.SimAnalogSignal(sd);
			as.setSignalName("Signal"+(i+1));
			as.buildIntervalValues(100);
			for(int k=0; k<100; k++)
			{
				double lowValue = Math.sin((k+i*10) / (2.0+i*2)) * 4;
				double increment = Math.sin((k+i*5) / (2.0+i));
				as.setIntervalValue(k, lowValue, lowValue+increment);
			}
		}
		sd.setCell(null);

		// make the waveform window
		WindowFrame wf = WindowFrame.createWaveformWindow(sd);
		WaveformWindow ww = (WaveformWindow)wf.getContent();
		ww.setMainTimeCursor(timeStep*22);
		ww.setExtensionTimeCursor(timeStep*77);
		ww.setDefaultTimeRange(0, timeStep*100);

		// make some waveform panels and put signals in them
		int k = 0;
		for(int i=0; i<3; i++)
		{
			WaveformWindow.Panel wp = new WaveformWindow.Panel(ww, true);
			wp.setValueRange(-5, 5);
			for(int j=0; j<=i; j++)
			{
				Simulation.SimAnalogSignal as = (Simulation.SimAnalogSignal)sd.getSignals().get(k++);
				WaveformWindow.Signal wsig = new WaveformWindow.Signal(wp, as);
			}
		}
	}

	// ---------------------- Gilda's Stuff MENU -----------------

    /**
     * Easy way to test bash scripts
     */
    public static void testBash()
    {
        String regressionname = "sportTop";
String testname = "sportTop";
String testpath = "sport/correctedData/";
String testlib = "sport";
String logname = "output/"+regressionname+"IO-"+Version.getVersion()+".log";

try {
  TopLevel.getMessagesWindow().save(logname);


            // Running diff
            File dir = new File("./");
            FileOutputStream outputStream = new FileOutputStream("tmpSport.log");
            FileOutputStream errStream = new FileOutputStream("errSport.log");

        for(Iterator it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = (Library)it.next();
            String libName = lib.getName();
            if (lib.getLibFile() == null) continue; // Clipboard
            String oldName = "../../data/"+testpath+"/"+libName+".jelib";
            String newName = "tmp/sport/"+libName+".jelib";
            FileMenu.SaveLibrary job = new FileMenu.SaveLibrary(lib, "tmp/sport/"+libName, FileType.JELIB, false, true);
    job.performTask();

            LineNumberReader oldReader = new LineNumberReader(new FileReader(oldName));
            LineNumberReader newReader = new LineNumberReader(new FileReader(newName));
            int oldLineNum = 0;
            int newLineNum = -1;
            boolean diff = false;
            String oldLine = null, newLine = null;

            for(;;)
            {
                oldLine = oldReader.readLine();
                oldLineNum = oldReader.getLineNumber();
                if (oldLine == null) break;
                newLine = newReader.readLine();
                newLineNum = newReader.getLineNumber();
                if (newLine == null) break;
                // skipping the headers
                if (oldLine.startsWith("H") &&
                        newLine.startsWith("H")) continue;
                // skipping
                if (oldLine.startsWith("L") &&
                        newLine.startsWith("L"))
                {
                    int index = oldLine.indexOf("|");
                    oldLine = oldLine.substring(1, index);
                    index = newLine.indexOf("|");
                    newLine = newLine.substring(1, index);
                }
                diff = !oldLine.equals(newLine);
                if (diff) break;
            }
            System.out.println("Library " + oldName + " and " + newName + " at line " + oldLineNum);
            System.out.println(oldLine);
             System.out.println(newLine);

//            Exec e = new Exec("/usr/bin/diff " + oldName + " " + newName, null, dir, outputStream, errStream);
//            e.start();
//    outputStream.flush();
//            errStream.flush();
            //Runtime.getRuntime().exec("cmd /c /usr/bin/diff " + oldName + " " + newName + " >> gilda.log" );
        }
            outputStream.close();
    errStream.close();

} catch (Exception e) {
  System.out.println("exception: "+e);
  e.printStackTrace();
  System.exit(1);
}
    }

    public static void threeViewCommand()
	{
        Class three3DViewDialog = Resources.get3DClass("J3DViewDialog");

        if (three3DViewDialog == null) return; // error in class initialization or not available

        try
        {
            Method createDialog = three3DViewDialog.getDeclaredMethod("createThreeViewDialog",
                    new Class[] {java.awt.Frame.class});
            createDialog.invoke(three3DViewDialog, new Object[]{TopLevel.getCurrentJFrame()});
        } catch (Exception e) {
            System.out.println("Can't open 3D Dialog window: " + e.getMessage());
            ActivityLogger.logException(e);
        }
	}

    public static void genFakeNodes()
    {
        makeFakeCircuitryForCoverageCommand("tsmc90", true);
    }

	/**
	 * First attempt for coverage implant
	 * @param newIdea
	 * @param test
	 */
	public static void implantGeneratorCommand(boolean newIdea, boolean test) {
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;

        Job job = new CoverImplantOld(curCell, wnd.getHighlighter());
	}

	private static class CoverImplantOld extends Job
	{
		private Cell curCell;
        private Highlighter highlighter;

		protected CoverImplantOld(Cell cell, Highlighter highlighter)
		{
			super("Coverage Implant Old", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.curCell = cell;
            this.highlighter = highlighter;
			setReportExecutionFlag(true);
			startJob();
		}

		public boolean doIt()
		{
			PolyMerge merge = new PolyMerge();
			java.util.List deleteList = new ArrayList(); // New coverage implants are pure primitive nodes
			HashMap allLayers = new HashMap();

			// Traversing arcs
			for(Iterator it = curCell.getArcs(); it.hasNext(); )
			{
				ArcInst arc = (ArcInst)it.next();
				ArcProto arcType = arc.getProto();
				Technology tech = arcType.getTechnology();
				Poly[] polyList = tech.getShapeOfArc(arc);

				// Treating the arcs associated to each node
				// Arcs don't need to be rotated
				for (int i = 0; i < polyList.length; i++)
				{
					Poly poly = polyList[i];
					Layer layer = poly.getLayer();
					Layer.Function func = layer.getFunction();

					if ( func.isSubstrate() )
					{
						merge.addPolygon(layer, poly);
						java.util.List rectList = (java.util.List)allLayers.get(layer);

						if ( rectList == null )
						{
							rectList = new ArrayList();
							allLayers.put(layer, rectList);
						}
						rectList.add(poly);
					}
				}
			}
			// Traversing nodes
			for(Iterator it = curCell.getNodes(); it.hasNext(); )
			{
				NodeInst node = (NodeInst)it .next();

				// New coverage implants are pure primitive nodes
				// and previous get deleted and ignored.
				//if (function == PrimitiveNode.Function.NODE)
				if (node.isPrimtiveSubstrateNode())
				{
					deleteList.add(node);
					continue;
				}

				NodeProto protoType = node.getProto();
				if (protoType instanceof Cell) continue;

				Technology tech = protoType.getTechnology();
				Poly[] polyList = tech.getShapeOfNode(node);
				AffineTransform transform = node.rotateOut();

				for (int i = 0; i < polyList.length; i++)
				{
					Poly poly = polyList[i];
					Layer layer = poly.getLayer();
					Layer.Function func = layer.getFunction();

                    // Only substrate layers, skipping center information
					if ( func.isSubstrate() )
					{
						poly.transform(transform);
						merge.addPolygon(layer, poly);
						java.util.List rectList = (java.util.List)allLayers.get(layer);

						if ( rectList == null )
						{
							rectList = new ArrayList();
							allLayers.put(layer, rectList);
						}
						rectList.add(poly);
					}
				}
			}

			// With polygons collected, new geometries are calculated
			highlighter.clear();
			java.util.List nodesList = new ArrayList();

			// Need to detect if geometry was really modified
			for(Iterator it = merge.getKeyIterator(); it.hasNext(); )
			{
				Layer layer = (Layer)it.next();
				java.util.List list = merge.getMergedPoints(layer, true) ;

				// Temp solution until qtree implementation is ready
				// delete uncessary polygons. Doesn't insert poly if identical
				// to original. Very ineficient!!
				java.util.List rectList = (java.util.List)allLayers.get(layer);
				java.util.List delList = new ArrayList();

				for (Iterator iter = rectList.iterator(); iter.hasNext();)
				{
					PolyBase p = (PolyBase)iter.next();
					Rectangle2D rect = p.getBounds2D();

					for (Iterator i = list.iterator(); i.hasNext();)
					{
						PolyBase poly = (PolyBase)i.next();
						Rectangle2D r = poly.getBounds2D();

						if (r.equals(rect))
						{
							delList.add(poly);
						}
					}
				}
				for (Iterator iter = delList.iterator(); iter.hasNext();)
				{
					list.remove(iter.next());
				}

				// Ready to create new implants.
				for(Iterator i = list.iterator(); i.hasNext(); )
				{
					PolyBase poly = (PolyBase)i.next();
					Rectangle2D rect = poly.getBounds2D();
					Point2D center = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
					PrimitiveNode priNode = layer.getPureLayerNode();
					// Adding the new implant. New implant not assigned to any local variable                                .
					NodeInst node = NodeInst.makeInstance(priNode, center, rect.getWidth(), rect.getHeight(), curCell);
					highlighter.addElectricObject(node, curCell);
					// New implant can't be selected again
					node.setHardSelect();
					nodesList.add(node);
				}
			}
			highlighter.finished();
			for (Iterator it = deleteList.iterator(); it.hasNext(); )
			{
				NodeInst node = (NodeInst)it .next();
				node.kill();
			}
			if ( nodesList.isEmpty() )
				System.out.println("No implant areas added");
			return true;
		}
	}

	// ---------------------- THE JON GAINSLEY MENU -----------------

	private static void invokeTSMC90FillGenerator()
	{
		try
		{
			Class tsmc90FillGeneratorClass = Class.forName("com.sun.electric.plugins.tsmc90.fill90nm.FillGenerator90");
			Class [] parameterTypes = new Class[] {};
			Method testMethod = tsmc90FillGeneratorClass.getDeclaredMethod("test", parameterTypes);
			testMethod.invoke(null, new Object[] {});
 		} catch (Exception e)
        {
 			System.out.println("ERROR invoking the Fill Generator test");
        }
	}

	public static void listVarsOnObject(boolean useproto) {
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
		if (wnd.getHighlighter().getNumHighlights() == 0) {
			// list vars on cell
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (wf == null) return;
            Cell cell = wf.getContent().getCell();
            cell.getInfo();
			return;
		}
		for (Iterator it = wnd.getHighlighter().getHighlights().iterator(); it.hasNext();) {
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.EOBJ) continue;
			ElectricObject eobj = h.getElectricObject();
            if (eobj instanceof PortInst) {
                PortInst pi = (PortInst)eobj;
                pi.getInfo();
                eobj = pi.getNodeInst();
            }
			if (eobj instanceof NodeInst) {
				NodeInst ni = (NodeInst)eobj;
				if (useproto) {
					System.out.println("using prototype");
					((ElectricObject)ni.getProto()).getInfo();
				} else {
					ni.getInfo();
				}
			}
		}
	}

	public static void evalVarsOnObject() {
		EditWindow curEdit = EditWindow.needCurrent();
        if (curEdit == null) return;

		if (curEdit.getHighlighter().getNumHighlights() == 0) return;
		for (Iterator it = curEdit.getHighlighter().getHighlights().iterator(); it.hasNext();) {
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.EOBJ) continue;
			ElectricObject eobj = h.getElectricObject();
			Iterator itVar = eobj.getVariables();
			while(itVar.hasNext()) {
				Variable var = (Variable)itVar.next();
				Object obj = curEdit.getVarContext().evalVar(var);
				System.out.print(var.getKey().getName() + ": ");
				System.out.println(obj);
			}
		}
	}

	public static void listLibVars() {
		Library lib = Library.getCurrent();
		Iterator itVar = lib.getVariables();
		System.out.println("----------"+lib+" Vars-----------");
		while(itVar.hasNext()) {
			Variable var = (Variable)itVar.next();
			Object obj = VarContext.globalContext.evalVar(var);
			System.out.println(var.getKey().getName() + ": " +obj);
		}
	}

    public static void addStringVar() {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;

        if (wnd.getHighlighter().getNumHighlights() == 0) return;
        for (Iterator it = wnd.getHighlighter().getHighlights().iterator(); it.hasNext();) {
            Highlight h = (Highlight)it.next();
            if (h.getType() == Highlight.Type.EOBJ) {
                ElectricObject eobj = h.getElectricObject();
                AddStringVar job = new AddStringVar(eobj);
                break;
            }
        }
    }

    private static class AddStringVar extends Job {
        private ElectricObject eobj;

        private AddStringVar(ElectricObject eobj) {
            super("AddStringVar", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.eobj = eobj;
            startJob();
        }

        public boolean doIt() {
            eobj.newVar("ATTR_XXX", "1");
            System.out.println("Added var ATTR_XXX as String \"1\"");
            return true;
        }
    }


    public static void causeStackOverflow(boolean x, boolean y, String l, int r, String f) {
        // this will cause a stack overflow
        causeStackOverflow(x, y, l, r, f);
    }

    public static void causeStackOverflowJob() {
        StackOverflowJob job = new StackOverflowJob();
    }

    public static class StackOverflowJob extends Job {
        private StackOverflowJob() {
            super("overflow", User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
            startJob();
        }
        public boolean doIt() {
            dosomething(true, "asfjka;dj");
            return true;
        }
        private void dosomething(boolean b, String str) {
            dosomething(b, str);
        }
    }

    public static void shakeDisplay() {
        //RedisplayTest job = new RedisplayTest(50);
        //RedrawTest test = new RedrawTest();
        long startTime = System.currentTimeMillis();

        EditWindow wnd = EditWindow.getCurrent();
        for (int i=0; i<100; i++) {
            //wnd.redrawTestOnly();
            //doWait();
        }
        long endTime = System.currentTimeMillis();

        StringBuffer buf = new StringBuffer();
        Date start = new Date(startTime);
        buf.append("  start time: "+start+"\n");
        Date end = new Date(endTime);
        buf.append("  end time: "+end+"\n");
        long time = endTime - startTime;
        buf.append("  time taken: "+TextUtils.getElapsedTime(time)+"\n");
        System.out.println(buf.toString());

    }

    private static class RedrawTest extends Job {

        private RedrawTest() {
            super("RedrawTest", User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
            startJob();
        }

        public boolean doIt() {
            long startTime = System.currentTimeMillis();

            EditWindow wnd = EditWindow.getCurrent();
            for (int i=0; i<100; i++) {
                if (getScheduledToAbort()) return false;
                //wnd.redrawTestOnly();
                //doWait();
            }
            long endTime = System.currentTimeMillis();

            StringBuffer buf = new StringBuffer();
            Date start = new Date(startTime);
            buf.append("  start time: "+start+"\n");
            Date end = new Date(endTime);
            buf.append("  end time: "+end+"\n");
            long time = endTime - startTime;
            buf.append("  time taken: "+TextUtils.getElapsedTime(time)+"\n");
            System.out.println(buf.toString());

            return true;
        }

        private void doWait() {
            try {
                boolean donesleeping = false;
                while (!donesleeping) {
                    Thread.sleep(100);
                    donesleeping = true;
                }
            } catch (InterruptedException e) {}
        }
    }

    private static class RedisplayTest extends Job {

        private long delayTimeMS;

        private RedisplayTest(long delayTimeMS) {
            super("RedisplayTest", User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.delayTimeMS = delayTimeMS;
            startJob();
        }

        public boolean doIt() {
            Random rand = new Random(143137493);

            for (int i=0; i<200; i++) {
                if (getScheduledToAbort()) return false;

                WindowFrame wf = WindowFrame.getCurrentWindowFrame();
                //int next = rand.nextInt(4);
                int next = i % 4;
                switch(next) {
                    case 0: { ZoomAndPanListener.panXOrY(0, wf, 1); break; }
                    case 1: { ZoomAndPanListener.panXOrY(1, wf, 1); break; }
                    case 2: { ZoomAndPanListener.panXOrY(0, wf, -1); break; }
                    case 3: { ZoomAndPanListener.panXOrY(1, wf, -1); break; }
                }
                doWait();
            }
            System.out.println(getInfo());
            return true;
        }

        private void doWait() {
            try {
                boolean donesleeping = false;
                while (!donesleeping) {
                    Thread.sleep(delayTimeMS);
                    donesleeping = true;
                }
            } catch (InterruptedException e) {}
        }
    }

    public static void startDefunctJob() {
        DefunctJob j = new DefunctJob();
    }

    private static ArrayList sharedList = new ArrayList();

    private static void changeSharedList() {
        //if (sharedList.size() < 100) sharedList.add(new Integer(sharedList.size()));
        //else sharedList.remove(sharedList.size()-1);
        Object o = sharedList.get(0);
    }

    private static class DefunctJob extends Job {

        public DefunctJob() {
            super("Defunct Job", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            startJob();
        }

        public boolean doIt() {
            while (true) {
                changeSharedList();
            }
        }
    }

    private static class TestObject {
        private int count;
        private final Object mutex;
        private TestObject() {
            mutex = new Object();
            count = 0;
        }
        private final int getCount() { return count; }
        private synchronized int getCountSync() { return count; }
        private int getCountExamineCheck() {
            Job.checkExamine();
            return count;
        }
        private int getCountExamineLock() {
            Job.acquireExamineLock(false);
            try {
                Job.releaseExamineLock();
            } catch (Error e) {
                Job.releaseExamineLock();                
            }
            return count;
        }
        private int getCountJob() {
            CountJob job = new CountJob(mutex);
            synchronized(mutex) {
                job.startJob(false, true);
                try {
                    mutex.wait();
                } catch (InterruptedException e) {}
            }
            return count;
        }

        private static class CountJob extends Job {
            private final Object mutex;
            private CountJob(Object mutex) {
                super("CountJob", User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
                this.mutex = mutex;
            }

            public boolean doIt() {
                synchronized(mutex) { mutex.notify(); }
                return true;
            }
        }
    }

    public static void timeMethodCalls() {
        TestObject obj = new TestObject();
        int limit = 500000;

        long start = System.currentTimeMillis();
        for (int i=0; i<limit; i++) {
            obj.getCount();
        }
        System.out.println("Baseline case: "+TextUtils.getElapsedTime(System.currentTimeMillis()-start));

        start = System.currentTimeMillis();
        for (int i=0; i<limit; i++) {
            obj.getCountSync();
        }
        System.out.println("Synchronized case: "+TextUtils.getElapsedTime(System.currentTimeMillis()-start));

        start = System.currentTimeMillis();
        for (int i=0; i<limit; i++) {
            obj.getCountExamineCheck();
        }
        System.out.println("Checking case (no sync): "+TextUtils.getElapsedTime(System.currentTimeMillis()-start));

        start = System.currentTimeMillis();
        for (int i=0; i<limit; i++) {
            obj.getCountExamineLock();
        }
        System.out.println("Locking case (no sync): "+TextUtils.getElapsedTime(System.currentTimeMillis()-start));

        start = System.currentTimeMillis();
        for (int i=0; i<limit; i++) {
            obj.getCountJob();
        }
        System.out.println("Job case: "+TextUtils.getElapsedTime(System.currentTimeMillis()-start));
    }

    public static void runCommand() {
        ExecDialog d = new ExecDialog(TopLevel.getCurrentJFrame(), false);
        File dir = new File("/home/gainsley");
        d.startProcess("/bin/tcsh", null, dir);
    }

    public static void deleteCells(View view) {
        Library lib = Library.getCurrent();
        int deleted = 0;
        int notDeleted = 0;
        for (Iterator it = lib.getCells(); it.hasNext(); ) {
            Cell cell = (Cell)it.next();
            if (cell.getView() != view) continue;
            if (CircuitChanges.deleteCell(cell, false))
                deleted++;
            else
                notDeleted++;
        }
        System.out.println("Deleted: "+deleted);
        System.out.println("Not deleted: "+ notDeleted);
    }

	// ---------------------- Dima's Stuff MENU -----------------

	private static int[] objs;
	private static int[] vobjs;
	private static int[] vobjs1;
	private static int[] vcnt;
	private static int numPoints;
	private static HashSet points;

	private static void varStatistics()
	{
		int subCells = 0;
		int cellUsages = 0;
		long cellSqr = 0;
		int primUsages = 0;
		long primSqr = 0;
		int namedArcs = 0;
		int namedNodes = 0;
		int sameLocations = 0;

		objs = new int[96];
		vobjs = new int[96];
		vobjs1 = new int[96];
		vcnt = new int[96];
		points = new HashSet();
		numPoints = 0;
		
		TreeSet nodeNames = new TreeSet();
		TreeSet arcNames = new TreeSet();

		for (Iterator lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = (Library)lIt.next();
			countVars('H', lib);

			for (Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				countVars('C', cell);
				TreeSet cellNodes = new TreeSet();
				TreeSet cellArcs = new TreeSet();

				for (Iterator uIt = cell.getUsagesIn(); uIt.hasNext(); )
				{
					NodeUsage nu = (NodeUsage)uIt.next();
					if (nu.getProto() instanceof Cell) {
						cellUsages++;
						cellSqr += nu.getNumInsts()*nu.getNumInsts();
					} else {
						primUsages++;
						primSqr += nu.getNumInsts()*nu.getNumInsts();
					}
				}

				for (Iterator nIt = cell.getNodes(); nIt.hasNext(); )
				{
					NodeInst ni = (NodeInst)nIt.next();
					countVars('N', ni);
					if (ni.getProto() instanceof Cell) subCells++;
					if (ni.isUsernamed()) namedNodes++;
// 					if (cellNodes.contains(ni.getName()))
// 						System.out.println(cell + " has duplicate node " + ni.getName());
					cellNodes.add(ni.getName());
					countPoint(ni.getAnchorCenter());
					
					for (Iterator pIt = ni.getPortInsts(); pIt.hasNext(); )
					{
						PortInst pi = (PortInst)pIt.next();
						countVars('P', pi);
					}
				}

				for (Iterator aIt = cell.getArcs(); aIt.hasNext(); )
				{
					ArcInst ai = (ArcInst)aIt.next();
					countVars('A', ai);
					if (ai.isUsernamed()) namedArcs++;
// 					if (cellArcs.contains(ai.getName()))
// 						System.out.println(cell + " has duplicate arc " + ai.getName());
					cellArcs.add(ai.getName());
					for (int i = 0; i < 2; i++) {
						Point2D p = ai.getLocation(i);
						if (ai.getPortInst(i).getNodeInst().getAnchorCenter().equals(p))
							sameLocations++;
						countPoint(p);
					}
				}

				for (Iterator eIt = cell.getPorts(); eIt.hasNext(); )
				{
					Export e = (Export)eIt.next();
					countVars('E', e);
				}
				nodeNames.addAll(cellNodes);
				arcNames.addAll(cellArcs);
			}
		}

		int o = 0, v = 0, v1 = 0, c = 0;
		for (int i = 0; i < objs.length; i++)
		{
			if (objs[i] == 0) continue;
			System.out.println(((char)i) + " " + objs[i] + " " + vobjs[i] + " " + vobjs1[i] + " " + vcnt[i]);
			o += objs[i];
			v += vobjs[i];
			v1 += vobjs1[i];
			c += vcnt[i];
		}
		System.out.println(o + " " + v + " " + v1 + " " + c);
		if (cellUsages != 0)
			System.out.println(subCells + " subcells " + cellUsages + " cellUsages " +
				((double)subCells)/cellUsages + " " + Math.sqrt(((double)cellSqr)/cellUsages));
		int prims = objs['N'] - subCells;
		if (primUsages != 0)
			System.out.println(prims + " prims " + primUsages + " primUsages " +
				((double)prims)/primUsages + " " + Math.sqrt(((double)primSqr)/primUsages));
		System.out.println(namedNodes + " named nodes " + nodeNames.size());
		System.out.println(namedArcs + " named arcs " + arcNames.size());
		System.out.println(sameLocations + " same locations");
		System.out.println(numPoints + " points " + points.size());
		HashSet doubles = new HashSet();
		for (Iterator it = points.iterator(); it.hasNext(); )
		{
			Point2D point = (Point2D)it.next();
			doubles.add(new Double(point.getX()));
			doubles.add(new Double(point.getY()));
		}
		int whole = 0;
		int quarter = 0;
		for (Iterator it = doubles.iterator(); it.hasNext(); )
		{
			double d = ((Double)it.next()).doubleValue();
			double rd = Math.rint(d);
			if (d == Math.rint(d))
				whole++;
			else if (d*4 == Math.rint(d*4))
				quarter++;
		}
		System.out.println(doubles.size() + " doubles " + whole + " whole " + quarter + " quarter");
/*
loco
A 192665 1657 1657 1657
C 2106 1872 1018 3509
E 37765 189 130 283
H 43 42 37 47
N 113337 4713 2328 22715
P 392542 0 0 0
738458 8473 5170 28211
16916 subcells 3093 cellUsages 5.469123827998707 24.734189873996737
96421 prims 13727 primUsages 7.024185910978364 40.263985564608774
468 named nodes 12604
1496 named arcs 10925
121518 same locations
499519 points 136298
14542 doubles 7299 whole 6728 quarter

qFour
A 336551 2504 2504 2504
C 3370 3161 2155 4898
E 112598 309 248 407
H 49 47 43 51
N 188496 8490 4847 32189
P 704883 0 0 0
1345947 14511 9797 40049
25997 subcells 7383 cellUsages 3.5211973452526073 17.251291844283067
162499 prims 20344 primUsages 7.987563900904443 69.86202450228595
910 named nodes 19655
5527 named arcs 10363
233128 same locations
862879 points 230599
18702 doubles 9531 whole 8486 quarter
*/
	}

	private static void countVars(char type, ElectricObject eObj)
	{
		int c = (int)type;
		objs[c]++;
		int numVars = eObj.getNumVariables();
		if (numVars == 0) return;
		vobjs[c]++;
		if (numVars == 1) vobjs1[c]++;
		vcnt[c] += numVars;
		for (Iterator it = eObj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			Object value = var.getObject();
			if (value instanceof Point2D)
			{
				countPoint((Point2D)value);
			} else if (value instanceof Point2D[])
			{
				Point2D[] points = (Point2D[])value;
				for (int i = 0; i < points.length; i++)
					countPoint(points[i]);
			}
		}
	}

	private static void countPoint(Point2D point)
	{
		double x = DBMath.round(point.getX());
		if (x == 0) x = 0;
		double y = DBMath.round(point.getY());
		if (x == 0) x = 0;
		point = new Point2D.Double(x, y);
		numPoints++;
		points.add(point);
	}

	private static void transactionalTest()
	{
		(new DatabaseTestThread()).start();
	}

	private static class DatabaseTestThread extends DatabaseChangeThread {
		private Snapshot s = null;
		
		private void show() {
			check();
			s = backup(s);
			print(s);
		}
		
		/** (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 * @requiresColor (!AWT & !DBExaminer);
		 */
		public void run() {//@grant DBChanger
			Cell_ cell = Cell_.newInstance("c0");
			show();
			Cell_ cell1 = Cell_.newInstance("c1");
			show();
			cell.setName("c2");
			show();
			cell.newNode(cell1, "n0", EPoint.ORIGIN);
			show();
//			NodeInst_ node = cell.addNode("n0");
//			ImmutableCell[] icells = new ImmutableCell[3];
//
//			ImmutableNodeInst[] inodes0 = new ImmutableNodeInst[10];
//			inodes0[0] = ImmutableNodeInst.newInstance(0, "n0", EPoint.ORIGIN);
//			inodes0[3] = ImmutableNodeInst.newInstance(2, "n3", new EPoint(2, 3));
//			icells[0] = ImmutableCell.newInstance("c0", inodes0);
//
//			inodes0[2] = ImmutableNodeInst.newInstance(2, "n2", new EPoint(2, 2));
//			icells[0] = icells[0].withNodes(inodes0);
//
//			ImmutableNodeInst[] inodes2 = new ImmutableNodeInst[1];
//			inodes2[0] = ImmutableNodeInst.newInstance(2, "qq", EPoint.ORIGIN);
//			icells[2] = ImmutableCell.newInstance("c2", inodes2);
//
//			Snapshot s = Snapshot.newInstance(icells);
//
//			s.check();
//			print(s);
//			try {
//				s = s.withNodeName(0, 3, "qwerty");
//			} catch (Throwable e) {
//				e.printStackTrace();
//			}
//			s.check();
//			print(s);
		}
	}

	private static void print(Snapshot s) {
		PrintWriter out = new PrintWriter(System.out, true);
		for (int cellId = 0, maxCellId = s.maxCellId(); cellId <= maxCellId; cellId++) {
			ImmutableCell cell = s.getCellById(cellId);
			if (cell == null) continue;
			out.println(cellId + " cell " + cell.name);
			for (int nodeId = 0, maxNodeId = cell.maxNodeId(); nodeId <= maxNodeId; nodeId++) {
				ImmutableNodeInst node = cell.getNodeById(nodeId);
				if (node == null) continue;
				out.println(nodeId + "\tnode " + node.name + " " + node.protoId + " " + node.anchor);
			}
		}
		out.println("----");
		out.flush();
	}
}
