package com.sun.electric.tool.generator.sclibrary;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.IdMapper;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.generator.layout.StdCellParams;
import com.sun.electric.tool.generator.layout.Tech;
import com.sun.electric.tool.generator.layout.GateLayoutGenerator;
import com.sun.electric.tool.user.CellChangeJobs;
import com.sun.electric.technology.technologies.Artwork;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Nov 15, 2006
 * Time: 3:39:44 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 * Generate a standard cell library from purple and red libraries
 */
public class SCLibraryGen {

    private String purpleLibraryName = "purpleFour";
    private String redLibraryName = "redFour";
    private String scLibraryName = "sclib";
    private Library purpleLibrary;
    private Library redLibrary;
    private Library scLibrary;
    private List<StdCellSpec> scellSpecs = new ArrayList<StdCellSpec>();

    private static final int blueColorIndex = EGraphics.makeIndex(Color.blue);

    public SCLibraryGen() {}

    private static class StdCellSpec {
        private String type;
        private double [] sizes;
        private StdCellSpec(String type, double [] sizes) {
            this.type = type;
            this.sizes = sizes;
        }
    }

    /* =======================================================
     * Settings
     * ======================================================= */

    /**
     * Set the names of the purple and red libraries. These must
     * be loaded when running the generation, and are used
     * as templates for the schematics and icons of standard cells.
     * @param purpleLibraryName
     * @param redLibraryName
     */
    public void setPurpleRedLibs(String purpleLibraryName, String redLibraryName) {
        this.purpleLibraryName = purpleLibraryName;
        this.redLibraryName = redLibraryName;
    }

    /**
     * Set the name of the output standard cell library.
     * Defaults to "sclib".
     * @param name
     */
    public void setOutputLibName(String name) {
        this.scLibraryName = name;
    }

    /**
     * Add command to generate the standard cell type
     * for the given space-separated list of sizes.
     * @param type
     * @param sizes
     */
    public void addStandardCell(String type, String sizes) {
        sizes = sizes.trim();
        if (sizes.equals("")) return;
        String [] ss = sizes.split("\\s+");
        double [] sss = new double [ss.length];
        for (int i=0; i<ss.length; i++) {
            sss[i] = Double.parseDouble(ss[i]);
        }
        scellSpecs.add(new StdCellSpec(type, sss));
    }

    /**
     * Generates the standard cell library
     */
    public boolean generate() {
        // check for red and purple libraries
        purpleLibrary = Library.findLibrary(purpleLibraryName);
        if (purpleLibrary == null) {
            prErr("Purple library \""+purpleLibraryName+"\" is not loaded.");
            return false;
        }
        redLibrary = Library.findLibrary(redLibraryName);
        if (redLibrary == null) {
            prErr("Red library \""+redLibraryName+"\" is not loaded.");
            return false;
        }
        prMsg("Using purple library \""+purpleLibraryName+"\" and red library \""+redLibraryName+"\"");

        scLibrary = Library.findLibrary(scLibraryName);
        if (scLibrary != null) {
            prWarn("Library "+scLibraryName+" already exists, overwriting it");
            scLibrary.kill("Standard Cell Generator regenerating standard cell library");
        }
        scLibrary = Library.newInstance(scLibraryName, null);
        prMsg("Created standard cell library "+scLibraryName);

        // dunno how to set standard cell params
        StdCellParams sc = GateLayoutGenerator.dividerParams(Tech.Type.TSMC180);
        sc.enableNCC(purpleLibraryName);

        for (StdCellSpec stdcell : scellSpecs) {
            for (double d : stdcell.sizes) {

                // generate layout first
                Cell laycell = GateLayoutGenerator.generateCell(scLibrary, sc, stdcell.type, d);
                if (laycell == null) {
                    prErr("Error creating layout cell "+stdcell.type+" of size "+d);
                    continue;
                }

                // copy sch cell (note that this also copies icon cell)
                Cell purplesch = purpleLibrary.findNodeProto(stdcell.type+"{sch}");
                copySchCell(purplesch, scLibrary, laycell.getName());

                Cell iconcell = scLibrary.findNodeProto(laycell.getName()+"{ic}");
                if (iconcell == null) continue;
                // change all arcs to blue
                for (Iterator<ArcInst> it = iconcell.getArcs(); it.hasNext(); ) {
                    ArcInst ai = it.next();
                    ai.newVar(Artwork.ART_COLOR, new Integer(blueColorIndex));
                }

                Cell schcell = scLibrary.findNodeProto(laycell.getName()+"{sch}");
                if (schcell == null) continue;
                // remove 'X' attribute
                if (schcell.getVar("ATTR_X") != null) {
                    schcell.delVar(Variable.findKey("ATTR_X"));
                }
                // change X value on red gate
                for (Iterator<NodeInst> it = schcell.getNodes(); it.hasNext(); ) {
                    NodeInst ni = it.next();
                    if (ni.isCellInstance()) {
                        Cell np = (Cell)ni.getProto();
                        if (np.getLibrary() == redLibrary) {
                            Variable var = ni.getVar("ATTR_X");
                            if (var != null) {
                                ni.newVar("ATTR_X", new Double(d));
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean copySchCell(Cell fromSchCell, Library toLib, String toName) {
        List<Cell> cellsToCopy = new ArrayList<Cell>();
        // check if sch already exists
        Cell schcell = toLib.findNodeProto(toName+"{sch}");
        if (schcell == null) {
            cellsToCopy.add(fromSchCell);
            IdMapper schid = CellChangeJobs.copyRecursively(cellsToCopy, scLibrary,
                    false, false, false, false, true);
            if (schid == null) {
                prErr("Unable to copy purple cell "+fromSchCell.describe(false)+" to library "+toLib);
                return false;
            }
        }
        // rename schematic cell
        schcell = scLibrary.findNodeProto(fromSchCell.getName()+"{sch}");
        if (schcell == null) return false;
        schcell.rename(toName, toName);
        schcell = scLibrary.findNodeProto(toName+"{sch}");
        if (schcell == null) return false;

        // check if icon already exists
        Cell iconcell = toLib.findNodeProto(toName+"{ic}");
        Cell fromIconCell = fromSchCell.getLibrary().findNodeProto(fromSchCell.getName()+"{ic}");
        if (iconcell == null && fromIconCell != null) {
            cellsToCopy.clear();
            cellsToCopy.add(fromIconCell);
            IdMapper id = CellChangeJobs.copyRecursively(cellsToCopy, scLibrary,
                    false, false, false, false, true);
            if (id == null) {
                prErr("Unable to copy purple cell "+fromIconCell.describe(false)+" to library "+toLib);
                return false;
            }
        }
        // rename icon cell if it was also copied
        iconcell = scLibrary.findNodeProto(fromSchCell.getName()+"{ic}");
        if (iconcell == null) return false;
        iconcell.rename(toName, toName);
        iconcell = scLibrary.findNodeProto(toName+"{ic}");
        if (iconcell == null) return false;
        return true;
    }

    /* =======================================================
     * Utility
     * ======================================================= */

    private void prErr(String msg) {
        System.out.println("Standard Cell Library Generator Error: "+msg);
    }
    private void prWarn(String msg) {
        System.out.println("Standard Cell Library Generator Warning: "+msg);
    }
    private void prMsg(String msg) {
        System.out.println("Standard Cell Library Generator: "+msg);
    }
}
