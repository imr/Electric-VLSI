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

import com.sun.electric.database.AnalyzeHeap;
import com.sun.electric.database.CellUsage;
import com.sun.electric.database.DumpHeap;
import com.sun.electric.database.geometry.*;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.generator.layout.*;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.extract.LayerCoverageTool;
import com.sun.electric.tool.erc.ERCWellCheck;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.logicaleffort.LENetlister1;
import com.sun.electric.tool.simulation.Analysis;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.simulation.AnalogSignal;
import com.sun.electric.tool.simulation.interval.Diode;
import com.sun.electric.tool.user.*;
import com.sun.electric.tool.user.dialogs.ExecDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.dialogs.FillGen;
import com.sun.electric.tool.user.ui.*;
import com.sun.electric.tool.user.waveform.Panel;
import com.sun.electric.tool.user.waveform.WaveSignal;
import com.sun.electric.tool.user.waveform.WaveformWindow;
import com.sun.electric.Main;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * Class to handle the commands in the debugging pulldown menus.
 */
public class DebugMenus
{
	protected static void addDebugMenus(MenuBar menuBar, MenuBar.Menu helpMenu)
	{
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

        MenuBar.Menu russMenu = MenuBar.makeMenu("_Russell");
        menuBar.add(russMenu);
		russMenu.addMenuItem("Gate Generator Regression (MoCMOS)", null,
							 new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new com.sun.electric.tool.generator.layout.GateRegression(MoCMOS.tech, Tech.Type.MOCMOS);
			}
		});
		if (Technology.getTSMC90Technology() != null)
			russMenu.addMenuItem("Gate Generator Regression (TSMC90)", null,
				new ActionListener() { public void actionPerformed(ActionEvent e) {
                    new com.sun.electric.tool.generator.layout.GateRegression(Technology.getTSMC90Technology(), Tech.Type.TSMC90); } });
        russMenu.addMenuItem("Random Test", null, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new com.sun.electric.tool.generator.layout.Test();
            }
        });

        /****************************** Jon's TEST MENU ******************************/

        MenuBar.Menu jongMenu = MenuBar.makeMenu("_JonG");
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

        MenuBar.Menu gildaMenu = MenuBar.makeMenu("_Gilda");
        menuBar.add(gildaMenu);
        gildaMenu.addMenuItem("New fill", null,
                        new ActionListener() { public void actionPerformed(ActionEvent e) {newFill();}});
        gildaMenu.addMenuItem("Dialog fill", null,
                        new ActionListener() { public void actionPerformed(ActionEvent e) {FillGen.openFillGeneratorDialog(MoCMOS.tech);}});
        gildaMenu.addMenuItem("Gate Generator TSMC180", null,
                        new ActionListener() { public void actionPerformed(ActionEvent e) {new GateRegression(MoCMOS.tech, Tech.Type.TSMC180);}});
        gildaMenu.addMenuItem("Gate Generator Mosis", null,
                        new ActionListener() { public void actionPerformed(ActionEvent e) {new GateRegression(MoCMOS.tech, Tech.Type.MOCMOS);}});
        gildaMenu.addMenuItem("Gate Generator TSMC90", null,
                        new ActionListener() { public void actionPerformed(ActionEvent e) {new GateRegression(Technology.getTSMC90Technology(), Tech.Type.TSMC90);}});
        gildaMenu.addMenuItem("Clean libraries", null,
                        new ActionListener() { public void actionPerformed(ActionEvent e) {cleanSetOfLibraries();}});
        gildaMenu.addMenuItem("9 layers -> 7 layers", null,
                        new ActionListener() { public void actionPerformed(ActionEvent e) {convertTo7LayersTech();}});
        gildaMenu.addMenuItem("Test Parameters", null,
                        new ActionListener() { public void actionPerformed(ActionEvent e) {testParameters();}});
        gildaMenu.addMenuItem("DRC Merge", null,
                        new ActionListener() { public void actionPerformed(ActionEvent e) {DRC.checkDRCHierarchically(Job.getUserInterface().needCurrentCell(),
                                null, GeometryHandler.GHMode.ALGO_MERGE);}});
        gildaMenu.addMenuItem("DRC Sweep", null,
                        new ActionListener() { public void actionPerformed(ActionEvent e) {DRC.checkDRCHierarchically(Job.getUserInterface().needCurrentCell(),
                                null, GeometryHandler.GHMode.ALGO_SWEEP);}});
        gildaMenu.addMenuItem("Test Bash", null,
                        new ActionListener() { public void actionPerformed(ActionEvent e) {testBash();}});
        gildaMenu.addMenuItem("3D View", null,
                        new ActionListener() { public void actionPerformed(ActionEvent e) {threeViewCommand();}});
        gildaMenu.addMenuItem("Parasitic", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ToolMenu.parasiticCommand(); } });
	    gildaMenu.addMenuItem("Check Wells Sweep", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ERCWellCheck.analyzeCurCell(GeometryHandler.GHMode.ALGO_SWEEP); } });
	    gildaMenu.addMenuItem("Check Wells Orig", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ERCWellCheck.analyzeCurCell(GeometryHandler.GHMode.ALGO_MERGE); } });
	    gildaMenu.addMenuItem("List Geometry on Network SWEEP", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ToolMenu.listGeometryOnNetworkCommand(GeometryHandler.GHMode.ALGO_SWEEP); } });
        gildaMenu.addMenuItem("Merge Polyons Merge", null,
                new ActionListener() { public void actionPerformed(ActionEvent e) {ToolMenu.layerCoverageCommand(Job.Type.CHANGE, LayerCoverageTool.LCMode.MERGE, GeometryHandler.GHMode.ALGO_MERGE);}});
        gildaMenu.addMenuItem("Merge Polyons Sweep", null,
                        new ActionListener() { public void actionPerformed(ActionEvent e) {ToolMenu.layerCoverageCommand(Job.Type.CHANGE, LayerCoverageTool.LCMode.MERGE, GeometryHandler.GHMode.ALGO_SWEEP);}});
        gildaMenu.addMenuItem("Covering Implants Merge", null,
                new ActionListener() { public void actionPerformed(ActionEvent e) {ToolMenu.layerCoverageCommand(Job.Type.CHANGE, LayerCoverageTool.LCMode.IMPLANT, GeometryHandler.GHMode.ALGO_MERGE);}});
        gildaMenu.addMenuItem("Covering Implants Sweep", null,
                        new ActionListener() { public void actionPerformed(ActionEvent e) {ToolMenu.layerCoverageCommand(Job.Type.CHANGE, LayerCoverageTool.LCMode.IMPLANT, GeometryHandler.GHMode.ALGO_SWEEP);}});
        gildaMenu.addMenuItem("Covering Implants Old", null,
                new ActionListener() { public void actionPerformed(ActionEvent e) {implantGeneratorCommand(false, false);}});
        gildaMenu.addMenuItem("Generate Fake Nodes", null,
                new ActionListener() { public void actionPerformed(ActionEvent e) {genFakeNodes();}});
        gildaMenu.addMenuItem("List Layer Coverage", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ToolMenu.layerCoverageCommand(Job.Type.EXAMINE, LayerCoverageTool.LCMode.AREA, GeometryHandler.GHMode.ALGO_SWEEP); } });

        /****************************** Dima's TEST MENU ******************************/

        MenuBar.Menu dimaMenu = MenuBar.makeMenu("_Dima");
        menuBar.add(dimaMenu);
//        if (Job.CLIENT)
//            dimaMenu.addMenuItem("Replay snapshot", null,
//                new ActionListener() { public void actionPerformed(ActionEvent e) { Snapshot.updateSnapshot(); } });
	    dimaMenu.addMenuItem("Show memory usage", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { System.out.println("Memory usage " + Main.getMemoryUsage() + " bytes"); } });
	    dimaMenu.addMenuItem("Plot diode", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { Diode.plotDiode(User.getWorkingDirectory() + File.separator + "diode.raw"); } });
	    dimaMenu.addMenuItem("Var stat", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { varStatistics(); } });
	    dimaMenu.addMenuItem("Dump heap", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { DumpHeap.dump("heapdump.dat"); } });
	    dimaMenu.addMenuItem("Read dump", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { AnalyzeHeap.analyze("heapdump.dat"); } });
    }

	// ---------------------- For Regression Testing -----------------

    /**
     * Class to set a cell to be the current cell, done in a Job.
     * By encapsulating this simple operation in a Job, it gets done
     * in the proper order when scheduled by a regression test.
     */
	private static class SetCellJob extends Job
    {
    	private String cellName;

        public SetCellJob(String cellName)
        {
            super("Set current cell", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cellName = cellName;
            startJob();
        }

        public boolean doIt() throws JobException
        {
    		Library lib = Library.getCurrent();
    		lib.setCurCell(lib.findNodeProto(cellName));
            return true;
        }
    }

	private static class SaveLibraryJob extends Job
    {
    	private String fileName;

    	public SaveLibraryJob(String fileName)
        {
            super("Save Library", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.fileName = fileName;
            startJob();
        }

        public boolean doIt() throws JobException
        {
    		Library lib = Library.getCurrent();
    		Cell cell = lib.getCurCell();
    		cell.lowLevelSetRevisionDate(new Date(0));	// reset modification date for consistent output
    		URL outURL = TextUtils.makeURLToFile(fileName);
    		lib.setLibFile(outURL);
    		lib.setName(TextUtils.getFileNameWithoutExtension(outURL));
    		Output.writeLibrary(lib, FileType.JELIB, false, false);
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
        } else
        {
            Cell myCell = MakeFakeCircuitry.doItInternal(tech);
            if (!Job.BATCHMODE)
			    WindowFrame.createEditWindow(myCell);
        }
	}

//	/**
//	 * Class to test compaction regressions in a new thread.
//	 */
//	private static class TestCompactionRegression extends Job
//	{
//		protected TestCompactionRegression()
//		{
//			super("Test Regression", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
//			startJob();
//		}
//
//		public boolean doIt() throws JobException
//		{
//			URL fileURL = TextUtils.makeURLToFile("compactionInput.jelib");
//			Library lib = LibraryFiles.readLibrary(fileURL, "compactionTests", FileType.JELIB, true);
//			Cell lay = lib.findNodeProto("CompactionTest1{lay}");
//			Compaction.compactNow(lay);
//			new Output.WriteJELIB(lib, "compactionOutput.jelib");			
//            return true;
//        }
//	}

	/**
	 * Class to read a library in a new thread.
	 */
	private static class MakeFakeCircuitry extends Job
	{
		private String theTechnology;
		private Cell myCell;

		protected MakeFakeCircuitry(String tech)
		{
			super("Make fake circuitry", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			theTechnology = tech;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			myCell = doItInternal(theTechnology);
			fieldVariableChanged("myCell");
            return true;
        }

        public void terminateOK()
        {
			WindowFrame wf = WindowFrame.getCurrentWindowFrame(false);
			if (wf != null) wf.loadComponentMenuForTechnology();

			// display a cell
            if (!BATCHMODE)
			    WindowFrame.createEditWindow(myCell);
        }

        /**
         * External static call for regressions
         * @param technology
         * @return
         */
		private static Cell doItInternal(String technology)
		{
			// get information about the nodes
			Technology tech = Technology.findTechnology(technology);
			if (tech == null)
			{
				System.out.println("Technology not found in MakeFakeCircuitry");
				return null;
			}
			tech.setCurrent();

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
			NodeInst rotTrans = NodeInst.newInstance(nTransProto, new Point2D.Double(0.0, 10.0), nTransProto.getDefWidth(), nTransProto.getDefHeight(), myCell, Orientation.fromAngle(3150), "rotated", 0);
			if (metal12Via == null || contactNode == null || metal2Pin == null || poly1PinA == null ||
				poly1PinB == null || transistor == null || rotTrans == null) return myCell;

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
			if (metal2Arc == null) return myCell;
			metal2Arc.setRigid(true);
			ArcInst metal1Arc = ArcInst.makeInstance(m1Proto, m1Proto.getWidth(), contactPort, m1m2Port);
			if (metal1Arc == null) return myCell;
			ArcInst polyArc1 = ArcInst.makeInstance(p1Proto, p1Proto.getWidth(), contactPort, p1PortB);
			if (polyArc1 == null) return myCell;
			ArcInst polyArc3 = ArcInst.makeInstance(p1Proto, p1Proto.getWidth(), p1PortB, p1PortA);
			if (polyArc3 == null) return myCell;
			ArcInst polyArc2 = ArcInst.makeInstance(p1Proto, p1Proto.getWidth(), transPortR, p1PortA);
			if (polyArc2 == null) return myCell;
			ArcInst polyArc4 = ArcInst.makeInstance(p1Proto, p1Proto.getWidth(), transRPortR, p1PortB);
			if (polyArc4 == null) return myCell;
			// export the two pins
			Export m1Export = Export.newInstance(myCell, m1m2Port, "in");
			m1Export.setCharacteristic(PortCharacteristic.IN);
			Export p1Export = Export.newInstance(myCell, p1PortA, "out");
			p1Export.setCharacteristic(PortCharacteristic.OUT);
			System.out.println("Created " + myCell);


			// now up the hierarchy
			Cell higherCell = Cell.makeInstance(mainLib, "higher{lay}");
			Rectangle2D bounds = myCell.getBounds();
			double myWidth = myCell.getDefWidth();
			double myHeight = myCell.getDefHeight();
            for (int iX = 0; iX < 2; iX++) {
                boolean flipX = iX != 0;
                for (int i = 0; i < 4; i++) {
                    Orientation orient = Orientation.fromJava(i*900, flipX, false);
                    NodeInst instanceNode = NodeInst.newInstance(myCell, new Point2D.Double(i*100, iX*200), myWidth, myHeight, higherCell, orient, null, 0);
                    instanceNode.setExpanded();
                    NodeInst instanceUNode = NodeInst.newInstance(myCell, new Point2D.Double(i*100, iX*200 + 100), myWidth, myHeight, higherCell, orient, null, 0);
                    if (iX == 0 && i == 0) {
                        PortInst instance1Port = instanceNode.findPortInst("in");
                        PortInst instance2Port = instanceUNode.findPortInst("in");
                        ArcInst instanceArc = ArcInst.makeInstance(m1Proto, m1Proto.getWidth(), instance1Port, instance2Port);
                    }
                }
            }
			System.out.println("Created " + higherCell);


			// now a rotation test
			Cell rotTestCell = Cell.makeInstance(mainLib, "rotationTest{lay}");
            TextDescriptor td = TextDescriptor.getNodeTextDescriptor().withRelSize(10);
            for (int iY = 0; iY < 2; iY++) {
                boolean flipY = iY != 0;
                for (int iX = 0; iX < 2; iX++) {
                    boolean flipX = iX != 0;
                    for (int i = 0; i < 4; i++) {
                        int angle = i*900;
                        Orientation orient = Orientation.fromJava(angle, flipX, flipY);
                        int x = i*100;
                        int y = iX*100 + iY*200;
                        NodeInst ni = NodeInst.newInstance(myCell, new Point2D.Double(x, y), myWidth, myHeight, rotTestCell, orient, null, 0);
                        ni.setExpanded();
                        NodeInst nodeLabel = NodeInst.newInstance(invisiblePinProto, new Point2D.Double(x, y - 35), 0, 0, rotTestCell);
                        String message = "Rotated " + (orient == Orientation.IDENT ? "0" : orient.toString());
                        Variable var = nodeLabel.newVar(Artwork.ART_MESSAGE, message,td);
//                        var.setRelSize(10);
                    }
                }
            }
			System.out.println("Created " + rotTestCell);


			// now up the hierarchy even farther
			Cell bigCell = Cell.makeInstance(mainLib, "big{lay}");
			int arraySize = 20;
			for(int y=0; y<arraySize; y++)
			{
				for(int x=0; x<arraySize; x++)
				{
					String theName = "arr["+ x + "][" + y + "]";
					NodeInst instanceNode = NodeInst.newInstance(myCell, new Point2D.Double(x*(myWidth+2), y*(myHeight+2)),
						myWidth, myHeight, bigCell, Orientation.IDENT, theName, 0);
					instanceNode.setOff(NodeInst.NODE_NAME, 0, 8);
					if ((x%2) == (y%2)) instanceNode.setExpanded();
				}
			}
			System.out.println("Created " + bigCell);
			return myCell;
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
        {
            Cell myCell = FakeCoverageCircuitry.doItInternal(tech);
            if (!Job.BATCHMODE)
			    WindowFrame.createEditWindow(myCell);
        }
	}

    private static class FakeCoverageCircuitry extends Job
    {
        private String theTechnology;
        private Cell myCell;

        protected FakeCoverageCircuitry(String tech)
        {
            super("Make fake circuitry for coverage tests", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            theTechnology = tech;
            startJob();
        }

        public boolean doIt() throws JobException
        {
            myCell = doItInternal(theTechnology);
			fieldVariableChanged("myCell");
            return true;
        }

        public void terminateOK()
        {
			WindowFrame wf = WindowFrame.getCurrentWindowFrame(false);
			if (wf != null) wf.loadComponentMenuForTechnology();
			// display a cell
            if (!BATCHMODE)
			    WindowFrame.createEditWindow(myCell);
        }

        private static Cell doItInternal(String technology)
		{
			// get information about the nodes
			Technology tech = Technology.findTechnology(technology);
			if (tech == null)
			{
				System.out.println("Technology not found in createCoverageTestCells");
				return null;
			}
			tech.setCurrent();

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
            for (int iX = 0; iX < 2; iX++) {
                boolean flipX = iX != 0;
                for (int i = 0; i < 4; i++) {
                    Orientation orient = Orientation.fromJava(i*900, flipX, false);
                    NodeInst instanceNode = NodeInst.newInstance(myCell, new Point2D.Double(i*myWidth, iX*myHeight), myWidth, myHeight, higherCell, orient, null, 0);
                    instanceNode.setExpanded();
                }
            }
			System.out.println("Created " + higherCell);

            return myCell;
		}
    }

	/**
	 * Test method to build an analog waveform with fake data.
	 */
	public static void makeFakeWaveformCommand()
	{
		// make the waveform data
		int numEvents = 100;
		Stimuli sd = new Stimuli();
		Analysis an = new Analysis(sd, Analysis.ANALYSIS_SIGNALS);
		double timeStep = 0.0000000001;
		an.buildCommonTime(numEvents);
		for(int i=0; i<numEvents; i++)
			an.setCommonTime(i, i * timeStep);
		for(int i=0; i<18; i++)
		{
			AnalogSignal as = new AnalogSignal(an);
			as.setSignalName("Signal"+(i+1));
			as.buildValues(numEvents);
			for(int k=0; k<numEvents; k++)
			{
				as.setValue(k, Math.sin((k+i*10) / (2.0+i*2)) * 4);
			}
		}
		sd.setCell(null);

		// make the waveform window
		WindowFrame wf = WindowFrame.createWaveformWindow(sd);
		WaveformWindow ww = (WaveformWindow)wf.getContent();

		// make some waveform panels and put signals in them
		for(int i=0; i<6; i++)
		{
			Panel wp = new Panel(ww, Analysis.ANALYSIS_SIGNALS);
			wp.setYAxisRange(-5, 5);
			for(int j=0; j<(i+1)*3; j++)
			{
				AnalogSignal as = (AnalogSignal)an.getSignals().get(j);
				WaveSignal wsig = new WaveSignal(wp, as);
			}
		}
	}

    /**
     * Class to define an interval signal in the simulation waveform window.
     */
    private static class IntervalAnalogSignal extends AnalogSignal {
        private final int signalIndex;
        private final double timeStep;
        
        private IntervalAnalogSignal(Analysis an, double timeStep, int signalIndex) {
            super(an);
            this.signalIndex = signalIndex;
            this.timeStep = timeStep;
            setSignalName("Signal"+(signalIndex+1));
        }
        
        /**
         * Method to return the low end of the interval range for this signal at a given event index.
         * @param sweep sweep index.
         * @param index the event index (0-based).
         * @param result double array of length 3 to return (time, lowValue, highValue)
         */
        public void getEvent(int sweep, int index, double[] result) {
            result[0] = index * timeStep;
            double lowValue = Math.sin((index+signalIndex*10) / (2.0+signalIndex*2)) * 4;
            double increment = Math.sin((index+signalIndex*5) / (2.0+signalIndex));
            result[1] = Math.min(lowValue, lowValue + increment);
            result[2] = Math.max(lowValue, lowValue + increment);
        }
        
        /**
         * Method to return the number of events in this signal.
         * This is the number of events along the horizontal axis, usually "time".
         * @param sweep sweep index.
         * @return the number of events in this signal.
         */
        public int getNumEvents(int sweep) { return 100; }
    }
    
	private static void makeFakeIntervalWaveformCommand()
	{
		// make the interval waveform data
		Stimuli sd = new Stimuli();
		Analysis an = new Analysis(sd, Analysis.ANALYSIS_SIGNALS);
        final double timeStep = 0.0000000001;
		for(int i=0; i<6; i++)
		{
			AnalogSignal as = new IntervalAnalogSignal(an, timeStep, i);
		}
		sd.setCell(null);

		// make the waveform window
		WindowFrame wf = WindowFrame.createWaveformWindow(sd);
		WaveformWindow ww = (WaveformWindow)wf.getContent();
//		ww.setMainXPositionCursor(timeStep*22);
//		ww.setExtensionXPositionCursor(timeStep*77);
//		ww.setDefaultHorizontalRange(0, timeStep*100);

		// make some waveform panels and put signals in them
		int k = 0;
		for(int i=0; i<3; i++)
		{
			Panel wp = new Panel(ww, Analysis.ANALYSIS_SIGNALS);
			wp.setYAxisRange(-5, 5);
			for(int j=0; j<=i; j++)
			{
				AnalogSignal as = (AnalogSignal)an.getSignals().get(k++);
				WaveSignal wsig = new WaveSignal(wp, as);
			}
		}
	}

	// ---------------------- Gilda's Stuff MENU -----------------
    private static void newFill()
    {
        Cell cell = WindowFrame.getCurrentCell();
        if (cell == null) return;
        List<PortInst> list = new ArrayList<PortInst>();

        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
        {
            NodeInst ni = it.next();

            for (Iterator<PortInst> itP = ni.getPortInsts(); itP.hasNext(); )
            {
                PortInst p = itP.next();

                if (!p.getPortProto().isGround() && !p.getPortProto().isPower()) continue;
                list.add(p);
            }
        }

        // Creating fill template
        FillGenerator fg = new FillGenerator(cell.getTechnology());
        fg.setFillLibrary("fillLibGIlda2");
        Rectangle2D bnd = cell.getBounds();

        double drcSpacingRule = 6;
        double vddReserve = drcSpacingRule*2;
        double gndReserve = drcSpacingRule*3;
        fg.setFillCellWidth(bnd.getWidth());
        fg.setFillCellHeight(bnd.getHeight());
        fg.makeEvenLayersHorizontal(true);
        fg.reserveSpaceOnLayer(3, vddReserve, FillGenerator.LAMBDA, gndReserve, FillGenerator.LAMBDA);
        fg.reserveSpaceOnLayer(4, vddReserve, FillGenerator.LAMBDA, gndReserve, FillGenerator.LAMBDA);
        new FillGenerator.FillGenJob(cell, fg, FillGenerator.PERIMETER, 3, 4, null);
    }

    private static void cleanSetOfLibraries()
    {
        boolean noMoreFound = false;
        String keyword = "qLiteTop";

//        while (!noMoreFound)
        {
            noMoreFound = true;
            for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
            {
                Library lib = it.next();

                // skip top cell
                if (lib.getName().indexOf(keyword) != -1)
                    continue;

                for (Iterator<Cell> itCell = lib.getCells(); itCell.hasNext(); )
                {
                    Cell cell = itCell.next();
                    if (!cell.isInUse("delete", true))
                    {
                        System.out.println(cell + " can be deleted");
                        noMoreFound = false;
                    }
                }
            }
        }
    }

    private static void convertTo7LayersTech()
    {
        // Select file
        String fileName = OpenFile.chooseDirectory("Choose Source Directory");
        if (fileName != null)
            new convertTo7LayersTechJob(fileName);
    }

    private static class convertTo7LayersTechJob extends Job
    {
        private String fileName;

        convertTo7LayersTechJob(String name)
        {
            super("Converting into 7 layers Tech", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
            fileName = name;
            this.startJob();
        }

        public boolean doIt() throws JobException
        {
            File workDir = new File(fileName);
            String topPath = "";
            String[] filesList = new String[]{fileName};

            if (workDir.isDirectory())
            {
                topPath = fileName + "/";
                filesList = workDir.list();
            }
            String newDir = OpenFile.chooseDirectory("Choose Destination Directory");
            String currentDir = User.getWorkingDirectory();
            if (newDir.equals(currentDir))
            {
                JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), new String [] {"Destination directory '" + newDir
                        + "' is identical to current directory. Possible file overwrite."}, "Error creating " + newDir + "' directory", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            File dir = new File(newDir);
            if (!dir.exists() && !dir.mkdir())
            {
                JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), new String [] {"Could not create '" + newDir
                        + "' directory",
                     dir.getAbsolutePath()}, "Error creating " + newDir + "' directory", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            System.out.println("Saving libraries in 7Layers directory under " + newDir);

            for (int i = 0; i < filesList.length; i++)
            {
                try {
                    String thisName =topPath+filesList[i];
                    LineNumberReader reader = new LineNumberReader(new FileReader(thisName));
                    URL url = TextUtils.makeURLToFile(filesList[i]);
                    String name = TextUtils.getFileNameWithoutExtension(url);
                    String ext = TextUtils.getExtension(url);
                    if (!ext.equals("jelib")) continue; // only jelib
                    System.out.println("Reading '" + thisName + "'");
                    String line = null;
                    PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(newDir+"/"+name+"."+
                            FileType.JELIB.getExtensions()[0])));
                    while ((line = reader.readLine()) != null)
                    {
                        // Set the correct number of layers
                        if (line.startsWith("Ttsmc90|"))
                        {
                            int index = line.indexOf("TSMC90NumberOfMetalLayers()I"); //28
                            if (index != -1)
                            {
                                String l = line.substring(0, index);
                                String s = line.substring(index+29, line.length());
                                line = l + "TSMC90NumberOfMetalLayers()I7" + s;
                            }
                            else // Have to add it
                              line += "|TSMC90NumberOfMetalLayers()I7"; 
                        }
                        line = line.replaceAll("Metal-5-Metal-8-Con", "Metal-5-Metal-6-Con");
                        line = line.replaceAll("Metal-7-Metal-8-Con", "Metal-5-Metal-6-Con");
                        line = line.replaceAll("Metal-8-Metal-9-Con", "Metal-6-Metal-7-Con");
                        line = line.replaceAll("Metal-8-Pin", "Metal-6-Pin");
                        line = line.replaceAll("Metal-9-Pin", "Metal-7-Pin");
                        line = line.replaceAll("Metal-9-Node", "Metal-7-Node");
                        line = line.replaceAll("Metal-8-Node", "Metal-6-Node");
                        line = line.replaceAll("metal-8", "metal-6"); // arc metal 8
                        line = line.replaceAll("metal-9", "metal-7"); // arc metal 9
                        printWriter.println(line);
                    }
                    printWriter.close();
                }
                catch (Exception e)
                {
                    System.out.println(e.getMessage());
                }
            }
            return true;
        }
    }

    private static void testParameters()
    {
        for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = it.next();

            for (Iterator<Cell> itCell = lib.getCells(); itCell.hasNext(); )
            {
                Cell cell = itCell.next();

                // Checking NodeInst/Cell master relation
                for(Iterator<NodeInst> itNodes = cell.getNodes(); itNodes.hasNext(); )
                {
                    NodeInst node = itNodes.next();
                    if (node.isIconOfParent()) continue;
                    NodeProto np = (NodeProto)node.getProto();
                    if (np instanceof Cell)
                    {
                        Cell master = (Cell)np;
                        if (!master.isIcon()) continue;
                        NodeInst ni = null;
                        // Searching for instance of that icon in master cell
                        for (Iterator<NodeInst> itU = master.getNodes(); itU.hasNext(); )
                        {
                            NodeInst ni1 = itU.next();
                            if (ni1.isIconOfParent())
                            {
                                ni = ni1;
                                break;
                            }
                        }
                        if (ni == null)
                        {
//                            System.out.println("Something is wrong!");
                            continue;
                        }

                        for (Iterator<Variable> itVar = node.getVariables(); itVar.hasNext();)
                        {
                            Variable var = itVar.next();
                            if (var.isAttribute())
                            {
                                // Not found in cell master
                                if (ni.getVar(var.getKey())==null)
                                {
                                    System.out.println("Cell " + cell.describe(true) + " " + node + " adding " + var);
                                }
                            }
                        }
                    }
                }

                // Checking schematic/icon relation
                for (Iterator<NodeInst> itInstOf = cell.getInstancesOf(); itInstOf.hasNext(); )
                {
                    NodeInst instOf = itInstOf.next();

                    if (instOf.isIconOfParent())
                    {
                        NodeInst icon = instOf;
                        Cell parent = icon.getParent();

                        for (Iterator<Variable> itVar = icon.getVariables(); itVar.hasNext();)
                        {
                            Variable var = itVar.next();
                            if (var.isAttribute())
                            {
                                if (parent.getVar(var.getKey())==null)
                                {
                                    System.out.println("Cell " + parent.describe(true) + " " + icon.getProto() + " ignoring " + var);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Easy way to test bash scripts
     */
    private static void testBash()
    {
        System.out.println("Num Log" + Input.errorLogger.getNumLogs() + NetworkTool.errorLogger.getNumLogs());
//        String regressionname = "sportTop";
//String testname = "sportTop";
//String testpath = "sport/correctedData/";
//String testlib = "sport";
//String logname = "output/"+regressionname+"IO-"+Version.getVersion()+".log";
//
//try {
//  TopLevel.getMessagesWindow().save(logname);
//
//
//            // Running diff
//            File dir = new File("./");
//            FileOutputStream outputStream = new FileOutputStream("tmpSport.log");
//            FileOutputStream errStream = new FileOutputStream("errSport.log");
//
//        for(Iterator it = Library.getLibraries(); it.hasNext(); )
//        {
//            Library lib = it.next();
//            String libName = lib.getName();
//            if (lib.getLibFile() == null) continue; // Clipboard
//            String oldName = "../../data/"+testpath+"/"+libName+".jelib";
//            String newName = "tmp/sport/"+libName+".jelib";
//            FileMenu.SaveLibrary job = new FileMenu.SaveLibrary(lib, "tmp/sport/"+libName, FileType.JELIB, false, true);
//    job.performTask();
//
//            LineNumberReader oldReader = new LineNumberReader(new FileReader(oldName));
//            LineNumberReader newReader = new LineNumberReader(new FileReader(newName));
//            int oldLineNum = 0;
//            int newLineNum = -1;
//            boolean diff = false;
//            String oldLine = null, newLine = null;
//
//            for(;;)
//            {
//                oldLine = oldReader.readLine();
//                oldLineNum = oldReader.getLineNumber();
//                if (oldLine == null) break;
//                newLine = newReader.readLine();
//                newLineNum = newReader.getLineNumber();
//                if (newLine == null) break;
//                // skipping the headers
//                if (oldLine.startsWith("H") &&
//                        newLine.startsWith("H")) continue;
//                // skipping
//                if (oldLine.startsWith("L") &&
//                        newLine.startsWith("L"))
//                {
//                    int index = oldLine.indexOf("|");
//                    oldLine = oldLine.substring(1, index);
//                    index = newLine.indexOf("|");
//                    newLine = newLine.substring(1, index);
//                }
//                diff = !oldLine.equals(newLine);
//                if (diff) break;
//            }
//            System.out.println("Library " + oldName + " and " + newName + " at line " + oldLineNum);
//            System.out.println(oldLine);
//             System.out.println(newLine);
//
////            Exec e = new Exec("/usr/bin/diff " + oldName + " " + newName, null, dir, outputStream, errStream);
////            e.start();
////    outputStream.flush();
////            errStream.flush();
//            //Runtime.getRuntime().exec("cmd /c /usr/bin/diff " + oldName + " " + newName + " >> gilda.log" );
//        }
//            outputStream.close();
//    errStream.close();
//
//} catch (Exception e) {
//  System.out.println("exception: "+e);
//  e.printStackTrace();
//  System.exit(1);
//}
    }

    public static void threeViewCommand()
	{
        Class three3DViewDialog = Resources.get3DClass("J3DViewDialog");

        if (three3DViewDialog == null) return; // error in class initialization or not available

        try
        {
            Method createDialog = three3DViewDialog.getDeclaredMethod("create3DViewDialog",
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
			super("Coverage Implant Old", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.curCell = cell;
            this.highlighter = highlighter;
			setReportExecutionFlag(true);
			startJob();
		}

		public boolean doIt() throws JobException
		{
			PolyMerge merge = new PolyMerge();
			List<NodeInst> deleteList = new ArrayList<NodeInst>(); // New coverage implants are pure primitive nodes
			HashMap<Layer,List<Poly>> allLayers = new HashMap<Layer,List<Poly>>();

			// Traversing arcs
			for(Iterator<ArcInst> it = curCell.getArcs(); it.hasNext(); )
			{
				ArcInst arc = it.next();
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
						List<Poly> rectList = (List<Poly>)allLayers.get(layer);

						if ( rectList == null )
						{
							rectList = new ArrayList<Poly>();
							allLayers.put(layer, rectList);
						}
						rectList.add(poly);
					}
				}
			}
			// Traversing nodes
			for(Iterator<NodeInst> it = curCell.getNodes(); it.hasNext(); )
			{
				NodeInst node = it.next();

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
						List<Poly> rectList = (List<Poly>)allLayers.get(layer);

						if (rectList == null)
						{
							rectList = new ArrayList<Poly>();
							allLayers.put(layer, rectList);
						}
						rectList.add(poly);
					}
				}
			}

			// With polygons collected, new geometries are calculated
			highlighter.clear();
			List<NodeInst> nodesList = new ArrayList<NodeInst>();

			// Need to detect if geometry was really modified
			for(Iterator<Layer> it = merge.getKeyIterator(); it.hasNext(); )
			{
				Layer layer = it.next();
				List<PolyBase> list = merge.getMergedPoints(layer, true) ;

				// Temp solution until qtree implementation is ready
				// delete uncessary polygons. Doesn't insert poly if identical
				// to original. Very ineficient!!
				List<Poly> rectList = (List<Poly>)allLayers.get(layer);
				List<PolyBase> delList = new ArrayList<PolyBase>();

				for (Poly p : rectList)
				{
					Rectangle2D rect = p.getBounds2D();

					for (PolyBase poly : list)
					{
						Rectangle2D r = poly.getBounds2D();

						if (r.equals(rect))
						{
							delList.add(poly);
						}
					}
				}
				for (PolyBase pb : delList)
				{
					list.remove(pb);
				}

				// Ready to create new implants.
				for(PolyBase poly : list)
				{
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
			for (NodeInst node : deleteList)
			{
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
		for (Highlight2 h : wnd.getHighlighter().getHighlights()) {
			if (!h.isHighlightEOBJ()) continue;
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
					if (ni.getProto() instanceof Cell)
						((Cell)ni.getProto()).getInfo();
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
		for (Highlight2 h : curEdit.getHighlighter().getHighlights()) {
            if (!h.isHighlightEOBJ()) continue;
			ElectricObject eobj = h.getElectricObject();
			Iterator<Variable> itVar = eobj.getVariables();
			while(itVar.hasNext()) {
				Variable var = itVar.next();
				Object obj = curEdit.getVarContext().evalVar(var);
				System.out.print(var.getKey().getName() + ": ");
				System.out.println(obj);
			}
		}
	}

	public static void listLibVars() {
		Library lib = Library.getCurrent();
		Iterator<Variable> itVar = lib.getVariables();
		System.out.println("----------"+lib+" Vars-----------");
		while(itVar.hasNext()) {
			Variable var = itVar.next();
			Object obj = VarContext.globalContext.evalVar(var);
			System.out.println(var.getKey().getName() + ": " +obj);
		}
	}

    public static void addStringVar() {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;

        if (wnd.getHighlighter().getNumHighlights() == 0) return;
        for (Highlight2 h : wnd.getHighlighter().getHighlights()) {
            if (h.isHighlightEOBJ()) {
                ElectricObject eobj = h.getElectricObject();
                AddStringVar job = new AddStringVar(eobj);
                break;
            }
        }
    }

    private static class AddStringVar extends Job {
        private ElectricObject eobj;

        private AddStringVar(ElectricObject eobj) {
            super("AddStringVar", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.eobj = eobj;
            startJob();
        }

        public boolean doIt() throws JobException {
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

	private static class StackOverflowJob extends Job {
        private StackOverflowJob() {
            super("overflow", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
            startJob();
        }
        public boolean doIt() throws JobException {
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
            super("RedrawTest", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
            startJob();
        }

        public boolean doIt() throws JobException {
            long startTime = System.currentTimeMillis();
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
            super("RedisplayTest", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.delayTimeMS = delayTimeMS;
            startJob();
        }

        public boolean doIt() throws JobException {
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

    private static ArrayList<Object> sharedList = new ArrayList<Object>();

    private static void changeSharedList() {
        //if (sharedList.size() < 100) sharedList.add(new Integer(sharedList.size()));
        //else sharedList.remove(sharedList.size()-1);
        Object o = sharedList.get(0);
    }

    private static class DefunctJob extends Job {

        public DefunctJob() {
            super("Defunct Job", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            startJob();
        }

        public boolean doIt() throws JobException {
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
                super("CountJob", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
                this.mutex = mutex;
            }

            public boolean doIt() throws JobException {
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
        for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
            Cell cell = it.next();
            if (cell.getView() != view) continue;
            if (CircuitChanges.deleteCell(cell, false, false))
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
	private static HashSet<Point2D> points;
    private static HashSet<TextDescriptor> descriptors;

	private static void varStatistics()
	{
		int subCells = 0;
		int cellUsages = 0;
		int namedArcs = 0;
		int namedNodes = 0;
		int sameLocations = 0;

		objs = new int[96];
		vobjs = new int[96];
		vobjs1 = new int[96];
		vcnt = new int[96];
		points = new HashSet<Point2D>();
        descriptors = new HashSet<TextDescriptor>();
		numPoints = 0;
		
		TreeSet<String> nodeNames = new TreeSet<String>();
		TreeSet<String> arcNames = new TreeSet<String>();

		for (Iterator<Library> lIt = Library.getLibraries(); lIt.hasNext(); )
		{
			Library lib = lIt.next();
			countVars('H', lib);

			for (Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = cIt.next();
				countVars('C', cell);
				TreeSet<String> cellNodes = new TreeSet<String>();
				TreeSet<String> cellArcs = new TreeSet<String>();

				for (Iterator<CellUsage> uIt = cell.getUsagesIn(); uIt.hasNext(); )
				{
					CellUsage nu = uIt.next();
					cellUsages++;
				}

				for (Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
				{
					NodeInst ni = nIt.next();
					countVars('N', ni);
					if (ni.getProto() instanceof Cell) subCells++;
					if (ni.isUsernamed()) namedNodes++;
// 					if (cellNodes.contains(ni.getName()))
// 						System.out.println(cell + " has duplicate node " + ni.getName());
					cellNodes.add(ni.getName());
                    if (ni.isUsernamed()) countDescriptor(ni.getTextDescriptor(NodeInst.NODE_NAME), true, null);
                    if (ni.getProto() instanceof Cell) countDescriptor(ni.getTextDescriptor(NodeInst.NODE_PROTO), true, null);
					countPoint(ni.getAnchorCenter());
					
					for (Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
					{
						PortInst pi = pIt.next();
						countVars('P', pi);
					}
				}

				for (Iterator<ArcInst> aIt = cell.getArcs(); aIt.hasNext(); )
				{
					ArcInst ai = aIt.next();
					countVars('A', ai);
					if (ai.isUsernamed()) namedArcs++;
// 					if (cellArcs.contains(ai.getName()))
// 						System.out.println(cell + " has duplicate arc " + ai.getName());
					cellArcs.add(ai.getName());
                    if (ai.isUsernamed()) countDescriptor(ai.getTextDescriptor(ArcInst.ARC_NAME), true, null);
					for (int i = 0; i < 2; i++) {
						Point2D p = ai.getLocation(i);
						if (ai.getPortInst(i).getNodeInst().getAnchorCenter().equals(p))
							sameLocations++;
						countPoint(p);
					}
				}

				for (Iterator<PortProto> eIt = cell.getPorts(); eIt.hasNext(); )
				{
					Export e = (Export)eIt.next();
                    countDescriptor(e.getTextDescriptor(Export.EXPORT_NAME), true, null);
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
				((double)subCells)/cellUsages /*+ " " + Math.sqrt(((double)cellSqr)/cellUsages)*/);
		int prims = objs['N'] - subCells;
        System.out.println(prims + " prims");
		System.out.println(namedNodes + " named nodes " + nodeNames.size());
		System.out.println(namedArcs + " named arcs " + arcNames.size());
		System.out.println(sameLocations + " same locations");
		System.out.println(numPoints + " points " + points.size());
		HashSet<Double> doubles = new HashSet<Double>();
		for (Point2D point : points)
		{
			doubles.add(new Double(point.getX()));
			doubles.add(new Double(point.getY()));
		}
		int whole = 0;
		int quarter = 0;
		for (Double dO : doubles)
		{
			double d = dO.doubleValue();
			double rd = Math.rint(d);
			if (d == Math.rint(d))
				whole++;
			else if (d*4 == Math.rint(d*4))
				quarter++;
		}
		System.out.println(doubles.size() + " doubles " + whole + " whole " + quarter + " quarter");
        System.out.println(descriptors.size() + " descriptors. cacheSize=" + TextDescriptor.cacheSize());
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
1256 descriptors

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
1515 descriptors
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
		for (Iterator<Variable> it = eObj.getVariables(); it.hasNext(); )
		{
			Variable var = it.next();
            countDescriptor(var.getTextDescriptor(), var.isDisplay(), var.getCode());
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

    private static void countDescriptor(TextDescriptor td, boolean display, TextDescriptor.Code code)
    {
        if (code == null) code = TextDescriptor.Code.NONE;
        descriptors.add(td);
    }
}
