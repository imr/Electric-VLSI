/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DebugMenuGilda.java
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

import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.geometry.ObjectQTree;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.geometry.PolyMerge;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.erc.ERCWellCheck;
import com.sun.electric.tool.extract.LayerCoverageTool;
import com.sun.electric.tool.generator.layout.FillGenerator;
import com.sun.electric.tool.generator.layout.GateRegression;
import com.sun.electric.tool.generator.layout.Tech;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.FillGen;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.menus.EMenu;
import com.sun.electric.tool.user.menus.EMenuItem;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;

/**
 * Gilda's TEST MENU
 */
public class DebugMenuGilda {
    
    static EMenu makeMenu() {
        return new EMenu("_Gilda",
            new EMenuItem("Test Layer") { public void run() {
                makeFakeJobCommand(); }},
            new EMenuItem("Hierarchy fill Intersection") { public void run() {
                newFill(true, false); }},
            new EMenuItem("QTREE") { public void run() {
                testQTree(); }},
            new EMenuItem("Count Lib") { public void run() {
                for (Iterator<Library> it = Library.getLibraries(); it.hasNext();) {
                    Library lib = it.next();
                    System.out.println("Library " + lib.getName() + " number = " + lib.getNumCells());
                }
            }},
            new EMenuItem("Flat fill") { public void run() {
                newFill(false, false); }},
            new EMenuItem("Dialog fill") { public void run() {
                FillGen.openFillGeneratorDialog(MoCMOS.tech); }},
            new EMenuItem("Gate Generator TSMC180") { public void run() {
                new GateRegression(Tech.Type.TSMC180); }},
            new EMenuItem("Gate Generator Mosis") { public void run() {
                new GateRegression(Tech.Type.MOCMOS); }},
            new EMenuItem("Gate Generator TSMC90") { public void run() {
                new GateRegression(Tech.Type.TSMC90); }},
            new EMenuItem("Clean libraries") { public void run() {
                cleanSetOfLibraries(); }},
            new EMenuItem("9 layers -> 7 layers") { public void run() {
                convertTo7LayersTech(); }},
            new EMenuItem("Test Parameters") { public void run() {
                testParameters(); }},
            new EMenuItem("DRC Merge") { public void run() {
                DRC.checkDRCHierarchically(Job.getUserInterface().needCurrentCell(),
                        null, GeometryHandler.GHMode.ALGO_MERGE); }},
            new EMenuItem("DRC Sweep") { public void run() {
                DRC.checkDRCHierarchically(Job.getUserInterface().needCurrentCell(),
                        null, GeometryHandler.GHMode.ALGO_SWEEP); }},
            new EMenuItem("Test Bash") { public void run() {
                testBash(); }},
            new EMenuItem("3D View") { public void run() {
                threeViewCommand(); }},
            new EMenuItem("Parasitic") { public void run() {
                ToolMenu.parasiticCommand(); }},
	        new EMenuItem("Check Wells Sweep") { public void run() {
                ERCWellCheck.analyzeCurCell(GeometryHandler.GHMode.ALGO_SWEEP); }},
	        new EMenuItem("Check Wells Orig") { public void run() {
                ERCWellCheck.analyzeCurCell(GeometryHandler.GHMode.ALGO_MERGE); }},
	        new EMenuItem("List Geometry on Network SWEEP") { public void run() {
                ToolMenu.listGeometryOnNetworkCommand(GeometryHandler.GHMode.ALGO_SWEEP); }},
            new EMenuItem("Merge Polyons Merge") { public void run() {
                ToolMenu.layerCoverageCommand(LayerCoverageTool.LCMode.MERGE, GeometryHandler.GHMode.ALGO_MERGE); }},
            new EMenuItem("Merge Polyons Sweep") { public void run() {
                ToolMenu.layerCoverageCommand(LayerCoverageTool.LCMode.MERGE, GeometryHandler.GHMode.ALGO_SWEEP); }},
            new EMenuItem("Covering Implants Merge") { public void run() {
                ToolMenu.layerCoverageCommand(LayerCoverageTool.LCMode.IMPLANT, GeometryHandler.GHMode.ALGO_MERGE); }},
            new EMenuItem("Covering Implants Sweep") { public void run() {
                ToolMenu.layerCoverageCommand(LayerCoverageTool.LCMode.IMPLANT, GeometryHandler.GHMode.ALGO_SWEEP); }},
            new EMenuItem("Covering Implants Old") { public void run() {
                implantGeneratorCommand(false, false); }},
            new EMenuItem("List Layer Coverage") { public void run() {
                ToolMenu.layerCoverageCommand(LayerCoverageTool.LCMode.AREA, GeometryHandler.GHMode.ALGO_SWEEP); } });
    }
    
	// ---------------------- For Regression Testing -----------------

    private static void makeFakeJobCommand()
    {
        // Using reflection to not force the loading of test plugin
        try
        {
            Class fakeJob = Class.forName("com.sun.electric.plugins.tests.FakeTestJob");
            Constructor<Object> instance = fakeJob.getDeclaredConstructor(new Class[]{Integer.class});
            instance.newInstance(new Object[] {new Integer(1)});
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        };
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

	// ---------------------- Gilda's Stuff MENU -----------------

    private static void testQTree()
    {
        Cell cell = WindowFrame.getCurrentCell();
        ObjectQTree tree = new ObjectQTree(cell.getBounds());
        Rectangle2D searchBox = null;

        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
        {
            NodeInst ni = it.next();
            NodeProto np = ni.getProto();
            if (np == Generic.tech.drcNode)
                searchBox = ni.getBounds();
            if (NodeInst.isSpecialNode(ni)) continue;
            tree.add(ni, ni.getBounds());
        }
        tree.print();
        if (searchBox != null)
        {
            Set set = tree.find(searchBox);
            if (set != null)
                for (Object obj : set)
                    System.out.println(" Obj " + obj);
        }
    }

    private static void newFill(boolean hierarchy, boolean binary)
    {
        Cell cell = WindowFrame.getCurrentCell();
        if (cell == null) return;

        FillGenerator.generateAutoFill(cell, hierarchy, binary, false);
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
                    if (node.isCellInstance())
                    {
                        Cell master = (Cell)node.getProto();
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
        System.out.println("Num Log" + Input.errorLogger.getNumLogs()/* + NetworkTool.errorLogger.getNumLogs()*/);
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
						List<Poly> rectList = allLayers.get(layer);

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

				if (node.isCellInstance()) continue;

				Technology tech = node.getProto().getTechnology();
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
						List<Poly> rectList = allLayers.get(layer);

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
            for (Layer layer : merge.getKeySet())
			{
				List<PolyBase> list = merge.getMergedPoints(layer, true) ;

				// Temp solution until qtree implementation is ready
				// delete uncessary polygons. Doesn't insert poly if identical
				// to original. Very ineficient!!
				List<Poly> rectList = allLayers.get(layer);
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
}
