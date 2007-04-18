/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Gates.java
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
package com.sun.electric.tool.generator.layout;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.CellInfo;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.generator.layout.gates.MoCMOSGenerator;
import com.sun.electric.tool.logicaleffort.LEInst;
import com.sun.electric.tool.user.User;

/*
 * Regression test for gate generators
 */
public class GateLayoutGenerator {

    // specify which gates shouldn't be surrounded by DRC rings
	private static final DrcRings.Filter FILTER = new DrcRings.Filter() {
		public boolean skip(NodeInst ni) {
			// well tie cells don't pass DRC with DrcRings
	        return ni.getProto().getName().indexOf("mosWellTie_") != -1;
		}
	};

//	private static void error(boolean pred, String msg) {
//		LayoutLib.error(pred, msg);
//	}

//	private Cell findCell(Library lib, String cellName) {
//		Cell c = lib.findNodeProto(cellName);
//		LayoutLib.error(c==null, "can't find: "+lib+":"+cellName);
//		return c;
//	}
    public static Cell generateCell(Library outputLib, StdCellParams stdCell,
                                    String type, double Xstrength) {
        if (outputLib == null) return null;
        stdCell.setOutputLibrary(outputLib);
        if (Tech.getTechnology() != stdCell.getTechnology())
            Tech.setTechnology(stdCell.getTechnology());

        if (Xstrength<0) return null;

        int pwr = type.indexOf("_pwr");
        if (pwr!=-1) type = type.substring(0, pwr);

        Cell c = null;
        if (Tech.is90nm())
        {
            try
            {
                Class<?> cmos90GeneratorClass = Class.forName("com.sun.electric.plugins.tsmc.gates90nm.CMOS90Generator");
                Class [] parameterTypes = new Class[] {String.class, Double.class, StdCellParams.class};
                Method makeGateMethod = cmos90GeneratorClass.getDeclaredMethod("makeGate", parameterTypes);
                c = (Cell)makeGateMethod.invoke(null, new Object[] {type, new Double(Xstrength), stdCell});
             } catch (Exception e)
            {
                 System.out.println("ERROR invoking the CMOS90 gate generator");
            }
//			c = CMOS90Generator.makeGate(pNm, x, stdCell);
        } else
        {
            c = MoCMOSGenerator.makeGate(type, Xstrength, stdCell);
        }
        return c;
    }


    /**
     * Generate layout cells from a heirarchical traversal of the
     * schematic cell
     * @param outLib the output library
     * @param cell the schematic cell
     * @param context the context of the schematic cell
     * @param stdCell the standard cell parameters
     * @param topLevelOnly true to generate gates in the top level only,
     * so it does not generate standard cells in sub cells.
     * @return a map of cells generated
     */
    public static Map<Nodable,Cell> generateLayoutFromSchematics(Library outLib,
                                   Cell cell, VarContext context,
                                   StdCellParams stdCell, boolean topLevelOnly) {
        stdCell.setOutputLibrary(outLib);
        Tech.setTechnology(stdCell.getTechnology());
        GenerateLayoutForGatesInSchematic visitor =
			new GenerateLayoutForGatesInSchematic(stdCell, topLevelOnly);
		HierarchyEnumerator.enumerateCell(cell, context, visitor);
//		HierarchyEnumerator.enumerateCell(cell, context, null, visitor);

        Cell gallery = Gallery.makeGallery(outLib);
        DrcRings.addDrcRings(gallery, FILTER, stdCell);

        return visitor.getGeneratedCells();
	}

    public static void generateFromSchematicsJob(TechType type) {
        GenerateFromSchematicsJob job = new GenerateFromSchematicsJob(type);
        job.startJob();
    }

    public static StdCellParams locoParams() {
		StdCellParams stdCell = new StdCellParams(TechType.MOCMOS);
		stdCell.enableNCC("purpleFour");
		stdCell.setSizeQuantizationError(0);
		stdCell.setMaxMosWidth(1000);
		stdCell.setVddY(21);
		stdCell.setGndY(-21);
		stdCell.setNmosWellHeight(42);
		stdCell.setPmosWellHeight(42);
		stdCell.setSimpleName(true);
		return stdCell;
	}

    public static StdCellParams sportParams() {
        StdCellParams stdCell = new StdCellParams(TechType.CMOS90);
        stdCell.setSizeQuantizationError(0);
        stdCell.setMaxMosWidth(1000);
        stdCell.setVddY(24.5);
        stdCell.setGndY(-24.5);
        stdCell.setNmosWellHeight(84);
        stdCell.setPmosWellHeight(84);
        stdCell.setSimpleName(true);
        return stdCell;
    }

	public static StdCellParams dividerParams(TechType technology) {
		StdCellParams stdCell = new StdCellParams(technology);
		stdCell.enableNCC("purpleFour");
		stdCell.setSizeQuantizationError(0);
		stdCell.setMaxMosWidth(1000);
		stdCell.setVddY(21);
		stdCell.setGndY(-21);
		stdCell.setNmosWellHeight(84);
		stdCell.setPmosWellHeight(84);
		stdCell.setSimpleName(true);
		return stdCell;
	}

    public static StdCellParams fastProxParams(TechType technology) {
        StdCellParams stdCell = new StdCellParams(technology);
        stdCell.enableNCC("purpleFour");
        stdCell.setSizeQuantizationError(0);
        stdCell.setMaxMosWidth(1000);
        stdCell.setVddY(24);
        stdCell.setGndY(-24);
        stdCell.setVddWidth(9);
        stdCell.setGndWidth(9);
        stdCell.setNmosWellHeight(60);
        stdCell.setPmosWellHeight(60);
        stdCell.setSimpleName(true);
        return stdCell;
    }

	public static StdCellParams justinParams(TechType technology) {
		StdCellParams stdCell = new StdCellParams(technology);
		stdCell.enableNCC("purpleFour");
		stdCell.setSizeQuantizationError(0);
		stdCell.setMaxMosWidth(1000);
		stdCell.setVddY(21);
		stdCell.setGndY(-21);
		stdCell.setNmosWellHeight(42);
		stdCell.setPmosWellHeight(42);
		stdCell.setSimpleName(true);
		return stdCell;
	}



    public static class GenerateFromSchematicsJob extends Job {

        private TechType technology;
        private Cell cell;
        private VarContext context;

        public GenerateFromSchematicsJob(TechType techNm) {
            super("Generate gate layouts", User.getUserTool(), Job.Type.CHANGE,
                  null, null, Job.Priority.ANALYSIS);
            this.technology = techNm;

            UserInterface ui = Job.getUserInterface();
            EditWindow_ wnd = ui.needCurrentEditWindow_();
            if (wnd == null) return;

            cell = wnd.getCell();
            context = wnd.getVarContext();
        }

        public boolean doIt() throws JobException {
            String outLibNm = "autoGenLib"+technology;
            Library outLib = LayoutLib.openLibForWrite(outLibNm);

            StdCellParams stdCell;
            Tech.setTechnology(technology);
            Technology cmos90 = Technology.getCMOS90Technology();
            if (cmos90 != null && technology == TechType.CMOS90) {
                stdCell = sportParams();
            } else {
                //stdCell = locoParams(outLib);
                stdCell = dividerParams(technology);
                //stdCell = justinParams(outLib, technology);
            }

            if (cell==null) {
                System.out.println("Please open the schematic for which you " +
                                   "want to generate gate layouts.");
                return false;
            }
            if (!cell.isSchematic()) {
                System.out.println("The current cell isn't a schematic. This " +
                                   "command only works on schematics.");
                return false;
            }
            System.out.println("Generating layouts for gates in the schematic: "+
                               cell.getName()+" and its descendents");
            System.out.println("Output goes to library: " + outLibNm);
            //Library outLib = cell.getLibrary();

            GateLayoutGenerator.generateLayoutFromSchematics(outLib, cell, context, stdCell, false);

            System.out.println("done.");
            return true;
        }
    }
}

/** Traverse a schematic hierarchy and generate Cells that we recognize. */
class GenerateLayoutForGatesInSchematic extends HierarchyEnumerator.Visitor {
	private final StdCellParams stdCell;
	private final boolean DEBUG = false;
    private final boolean topLevelOnly;
    private Map<Nodable,Cell> generatedCells;
    private void trace(String s) {
		if (DEBUG) System.out.println(s);
	}
	private void traceln(String s) {
		trace(s);
		trace("\n");
	}
	
	/**
	 * Construct a Visitor that will walk a schematic and generate layout for Cells.
	 * param libraryName the name of the library that contains the Cells for 
	 * which we want to generate layout. For example: "redFour" or "purpleFour".
	 * param cellNames the names of the Cells for which we want to generate layout
	 */
	public GenerateLayoutForGatesInSchematic(StdCellParams stdCell, boolean topLevelOnly) {
		this.stdCell = stdCell;
        this.topLevelOnly = topLevelOnly;
        this.generatedCells = new HashMap<Nodable,Cell>();
    }
	/** @return value of strength attribute "ATTR_X" or -1 if no such
	 * attribute or -2 if attribute exists but has no value. */
	private static double getStrength(Nodable no, VarContext context) {
		Variable var = no.getVar(Tech.ATTR_X);
		if (var==null) return -1;
		Object val = context.evalVar(var, no);
		if (val==null) return -2;
		//LayoutLib.error(val==null, "strength is null?");
		LayoutLib.error(!(val instanceof Number),
				        "strength not number?");
		return ((Number)val).doubleValue();
	}

	private Cell generateCell(Nodable iconInst, CellInfo info) {
		VarContext context = info.getContext();
		String pNm = iconInst.getProto().getName();
		double x = getStrength(iconInst, context);
		if (x==-2) {
			System.out.println("no value for strength attribute for Cell: "+
					           pNm+" instance: "+
							   info.getUniqueNodableName(iconInst, "/"));
		}
//		System.out.println("Try : "+pNm+" X="+x+" for instance: "+
//                           info.getUniqueNodableName(iconInst, "/"));
		
		Cell c = GateLayoutGenerator.generateCell(stdCell.getOutputLibrary(), stdCell, pNm, x);
		if (c!=null) {
            // record defining schematic cell if it is sizable
            if (LEInst.getType(iconInst, context) == LEInst.Type.LEGATE) {
                c.newVar(LEInst.ATTR_LEGATE, c.libDescribe());
            }
            if (LEInst.getType(iconInst, context) == LEInst.Type.LEKEEPER) {
                c.newVar(LEInst.ATTR_LEKEEPER, c.libDescribe());
            }

			System.out.println("Use: "+pNm+" X="+x+" for instance: "+
			                   info.getUniqueNodableName(iconInst, "/"));
		}
        return c;
    }

	public boolean enterCell(CellInfo info) {
		VarContext ctxt = info.getContext();
		traceln("Entering Cell instance: "+ctxt.getInstPath("/"));
		return true; 
	}
	public void exitCell(CellInfo info) {
		VarContext ctxt = info.getContext();
		traceln("Leaving Cell instance: "+ctxt.getInstPath("/"));
	}
	public boolean visitNodeInst(Nodable no, CellInfo info) {
		// we never generate layout for PrimitiveNodes
		if (no instanceof NodeInst) return false;
		
		trace("considering instance: "+
			  info.getUniqueNodableName(no, "/")+" ... ");
		
		Cell cell = (Cell) no.getProto();
		Library lib = cell.getLibrary();
		String libNm = lib.getName();
		if (libNm.equals("redFour") || libNm.equals("purpleFour") ||
			libNm.equals("power2_gates")) {
			traceln("generate");
			Cell c = generateCell(no, info);
            if (c != null) generatedCells.put(no, c);
            return false;
		}
		traceln("descend");
        if (topLevelOnly) return false;
        return true;
	}

    public Map<Nodable,Cell> getGeneratedCells() { return generatedCells; }
}
