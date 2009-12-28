package com.sun.electric.tool.generator.sclibrary;

import java.awt.Color;
import java.util.*;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.generator.layout.GateLayoutGenerator;
import com.sun.electric.tool.generator.layout.StdCellParams;
import com.sun.electric.tool.generator.layout.TechType;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.output.Verilog;
import com.sun.electric.tool.user.User;

/**
 * Generate a standard cell library from purple and red libraries
 * User: gainsley
 * Date: Nov 15, 2006
 */
public class SCLibraryGen {

    private String purpleLibraryName = "purpleFour";
    private String redLibraryName = "redFour";
    private String scLibraryName = "sclib";
    private Library purpleLibrary;
    private Library redLibrary;
    private Library scLibrary;
    private List<StdCellSpec> scellSpecs = new ArrayList<StdCellSpec>();
    private PrimitiveNode pin = Generic.tech().invisiblePinNode;
    private Variable.Key sizeKey = Variable.findKey("ATTR_X");

    public static final Variable.Key STANDARDCELL = Variable.newKey("ATTR_StandardCell");
    public static final Variable.Key DONOTEXTRACTFORDSPF = Variable.newKey("ATTR_ExcludeFromDSPFExtraction");

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
     * @param sc standard cell parameters
     * @return false on error, true otherwise
     */
    public boolean generate(StdCellParams sc) {
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

        if (sc.getTechType() == TechType.TechTypeEnum.TSMC180.getTechType())
            scLibraryName = "sclibTSMC180";
        else if (sc.getTechType() == TechType.TechTypeEnum.CMOS90.getTechType())
            scLibraryName = "sclibCMOS90";
        scLibrary = Library.findLibrary(scLibraryName);
        if (scLibrary == null) {
            scLibrary = Library.newInstance(scLibraryName, null);
            prMsg("Created standard cell library "+scLibraryName);
        }
        prMsg("Using standard cell library "+scLibraryName);

        // dunno how to set standard cell params
        sc.enableNCC(purpleLibraryName);

        for (StdCellSpec stdcell : scellSpecs) {
            for (double d : stdcell.sizes) {

                String cellname = sc.sizedName(stdcell.type, d);
                cellname = cellname.substring(0, cellname.indexOf('{'));

                // generate layout first
                Cell laycell = scLibrary.findNodeProto(cellname+"{lay}");
                if (laycell == null) {
                    laycell = GateLayoutGenerator.generateCell(scLibrary, sc, stdcell.type, d);
                    if (laycell == null) {
                        prErr("Error creating layout cell "+stdcell.type+" of size "+d);
                        continue;
                    }
                }
                // generate icon next
                Cell iconcell = scLibrary.findNodeProto(cellname+"{ic}");
                if (iconcell == null) {
                    copyIconCell(stdcell.type, purpleLibrary, cellname, scLibrary, d);
                }

                // generate sch last
                Cell schcell = scLibrary.findNodeProto(cellname+"{sch}");
                if (schcell == null) {
                    copySchCell(stdcell.type, purpleLibrary, cellname, scLibrary, d);
                }

                schcell = scLibrary.findNodeProto(cellname+"{sch}");
                // mark schematic as standard cell
                List<Cell> cells = new ArrayList<Cell>();
                cells.add(schcell);
                markStandardCell(cells, null);
            }
        }
        return true;
    }

    private boolean copyIconCell(String name, Library lib, String toName, Library toLib, double size) {
        // check if icon already exists
        Cell iconcell = toLib.findNodeProto(toName+"{ic}");
        Cell fromIconCell = lib.findNodeProto(name+"{ic}");
        if (iconcell == null && fromIconCell != null) {
            iconcell = Cell.copyNodeProto(fromIconCell, toLib, toName, false);
            if (iconcell == null) {
                prErr("Unable to copy purple cell "+fromIconCell.describe(false)+" to library "+toLib);
                return false;
            }
            // add size text
            NodeInst sizeni = NodeInst.makeInstance(pin, new EPoint(0,0),
                    0, 0, iconcell);
            sizeni.newVar(Artwork.ART_MESSAGE, new Double(size),
                    TextDescriptor.getAnnotationTextDescriptor().withColorIndex(blueColorIndex));

            // change all arcs to blue
            for (Iterator<ArcInst> it = iconcell.getArcs(); it.hasNext(); ) {
                ArcInst ai = it.next();
                ai.newVar(Artwork.ART_COLOR, new Integer(blueColorIndex));
            }
            for (Iterator<NodeInst> it = iconcell.getNodes(); it.hasNext(); ) {
                NodeInst ni = it.next();
                ni.newVar(Artwork.ART_COLOR, new Integer(blueColorIndex));
            }
            // remove 'X' parameter
            if (iconcell.isParam(sizeKey)) {
                iconcell.getCellGroup().delParam((Variable.AttrKey)sizeKey);
            }
        }
        return true;
    }

    private boolean copySchCell(String name, Library lib, String toName, Library toLib, double size) {
        // check if sch already exists
        Cell schcell = toLib.findNodeProto(toName+"{sch}");
        Cell fromSchCell = lib.findNodeProto(name+"{sch}");
        if (schcell == null && fromSchCell != null) {
            schcell = Cell.copyNodeProto(fromSchCell, toLib, toName, false);
            if (schcell == null) {
                prErr("Unable to copy purple cell "+fromSchCell.describe(false)+" to library "+toLib);
                return false;
            }
            // replace master icon cell in schematic
            Cell iconcell = toLib.findNodeProto(toName+"{ic}");
            Cell fromIconCell = lib.findNodeProto(name+"{ic}");
            if (iconcell != null && fromIconCell != null) {
                for (Iterator<NodeInst> it = schcell.getNodes(); it.hasNext(); ) {
                    NodeInst ni = it.next();
                    if (ni.isCellInstance()) {
                        Cell np = (Cell)ni.getProto();
                        if (np == fromIconCell) {
                            ni.replace(iconcell, true, true);
                        }
                    }
                }
            }
            // remove 'X' parameter
            if (schcell.isParam(sizeKey)) {
                schcell.getCellGroup().delParam((Variable.AttrKey)sizeKey);
            }
            // remove verilog template attribute
            if (schcell.getVar(Verilog.VERILOG_TEMPLATE_KEY) != null) {
                schcell.delVar(Verilog.VERILOG_TEMPLATE_KEY);
            }
            // change X value on red gate
            for (Iterator<NodeInst> it = schcell.getNodes(); it.hasNext(); ) {
                NodeInst ni = it.next();
                if (ni.isCellInstance()) {
                    Cell np = (Cell)ni.getProto();
                    if (np.getLibrary() == redLibrary) {
                        if (ni.isDefinedParameter(sizeKey)) {
                            ni.updateParam(sizeKey, Double.valueOf(size));
                        }
                    }
                    if (np.isIconOf(schcell)) {
                        // remove size attribute
                        ni.delParameter(sizeKey);
                    }
                }
            }
        }
        return true;
    }

    /* =======================================================
     * Utility
     * ======================================================= */

    /**
     * Mark the cell as a standard cell.
     * This version of the method performs the task in a Job.
     * @param standardCells a list of Cells to mark with the standard cell attribute marker.
     * @param notStandardCells a list of Cells to remove the standard cell attribute marker.
     */
    public static void markStandardCellJob(List<Cell> standardCells, List<Cell> notStandardCells) {
        CreateVar job = new CreateVar(standardCells, notStandardCells, STANDARDCELL);
        job.startJob();
    }

    /**
     * Mark the cell as a standard cell
     * @param standardCells a list of Cells to mark with the standard cell attribute marker.
     * @param notStandardCells a list of Cells to remove the standard cell attribute marker.
     */
    public static void markStandardCell(List<Cell> standardCells, List<Cell> notStandardCells) {
        CreateVar job = new CreateVar(standardCells, notStandardCells, STANDARDCELL);
        job.doIt();
    }

    private static class CreateVar extends Job
    {
        private List<Cell> standardCells;
        private List<Cell> notStandardCells;
        private Variable.Key varName;

        public CreateVar(List<Cell> standardCells, List<Cell> notStandardCells, Variable.Key varName)
        {
            super("Create Var", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.standardCells = standardCells;
            this.notStandardCells = notStandardCells;
            this.varName = varName;
        }

        public boolean doIt()
        {
            TextDescriptor td = TextDescriptor.getCellTextDescriptor().withInterior(true).withDispPart(TextDescriptor.DispPos.NAMEVALUE);
            if (standardCells != null)
            	for(Cell cell : standardCells)
            		cell.newVar(varName, new Integer(1), td);
            if (notStandardCells != null)
	            for(Cell cell : notStandardCells)
	            	cell.delVar(varName);
            return true;
        }
    }

    /**
     * Mark the cell to not be extracted for DSPF
     * @param cells the cells
     */
    public static void markDoNotExtractForDSPFJob(List<Cell> cells) {
        CreateVar job = new CreateVar(cells, null, DONOTEXTRACTFORDSPF);
        job.startJob();
    }

    /**
     * Return the standard cells in a hierarchy starting from
     * the specified top cell.
     * @param topCell the top cell in the hierarchy (sch or lay view)
     * @return a set of standard cells in the hierarchy
     */
    public static Set<Cell> getStandardCellsInHierarchy(Cell topCell) {
        StandardCellHierarchy cells = new StandardCellHierarchy();
        HierarchyEnumerator.enumerateCell(topCell, VarContext.globalContext, cells);
        return cells.getStandardCellsInHier();
    }

    /**
     * Returns true if the cell is marked as a standard cell for Static
     * Timing Analysis
     * @param cell the cell to check
     * @return true if standard cell, false otherwise
     */
    public static boolean isStandardCell(Cell cell) {
        Cell schcell = cell.getCellGroup().getMainSchematics();
        if (schcell != null) cell = schcell;
        return cell.getVar(STANDARDCELL) != null;
    }

    /**
     * Returns true if the cell is marked to not be extracted for DSPF,
     * for Static Timing Analysis
     * @param cell the cell to check
     * @return true if marked to not extract for DSPF
     */
    public static boolean isMarkedDoNotExtractForDSPF(Cell cell) {
        Cell schcell = cell.getCellGroup().getMainSchematics();
        if (schcell != null) cell = schcell;
        return cell.getVar(DONOTEXTRACTFORDSPF) != null;
    }

    private void prErr(String msg) {
        System.out.println("Standard Cell Library Generator Error: "+msg);
    }
//    private void prWarn(String msg) {
//        System.out.println("Standard Cell Library Generator Warning: "+msg);
//    }
    private void prMsg(String msg) {
        System.out.println("Standard Cell Library Generator: "+msg);
    }

    /****************************** Standard Cell Enumerator *************************/

    public static class StandardCellHierarchy extends HierarchyEnumerator.Visitor {

        private static final Integer standardCell = new Integer(0);
        private static final Integer containsStandardCell = new Integer(1);
        private static final Integer doesNotContainStandardCell = new Integer(2);

        private Map<Cell,Integer> standardCellMap = new HashMap<Cell,Integer>();
        private Map<String,Cell> standardCellsByName = new HashMap<String,Cell>();
        private List<VarContext> standardCellContexts = new ArrayList<VarContext>();
        private Map<VarContext,VarContext> emptyCellContexts = new HashMap<VarContext,VarContext>();
        private Map<VarContext,VarContext> containsStandardCellContexts = new HashMap<VarContext,VarContext>();
        private Set<Cell> doNotExtractForDSPF = new HashSet<Cell>();
        private boolean nameConflict = false;

        public boolean enterCell(HierarchyEnumerator.CellInfo info) {
            Cell cell = info.getCell();

            if (isMarkedDoNotExtractForDSPF(cell)) doNotExtractForDSPF.add(cell);

            // skip cached and does not contain standard cell
            // (we want to traverse all hierarchy that contains standard cells
            // to produce the standard cell contexts list)
            if (standardCellMap.get(cell) == doesNotContainStandardCell) {
                emptyCellContexts.put(info.getContext(), info.getContext());
                return false;
            }

            if (SCLibraryGen.isStandardCell(cell)) {
                standardCellContexts.add(info.getContext());

                if (!standardCellMap.containsKey(cell)) {
                    standardCellMap.put(cell, standardCell);
                    // check for name conflict
                    Cell otherCell = standardCellsByName.get(cell.getName());
                    if (otherCell != null && otherCell != cell) {
                        System.out.println("Error: multiple standard cells with same name not allowed: "+
                                cell.libDescribe()+" and "+ otherCell.libDescribe());
                        nameConflict = true;
                    } else {
                        standardCellsByName.put(cell.getName(), cell);
                    }
                }
                return false;
            }
            return true;
        }

        public void exitCell(HierarchyEnumerator.CellInfo info) {
            Cell cell = info.getCell();
            VarContext context = info.getContext();

            for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
                NodeInst ni = it.next();
                if (!ni.isCellInstance()) continue;
                if (ni.isIconOfParent()) continue;
                // standard cell tag is on schematic view
                Cell proto = ni.getProtoEquivalent();
                if (proto == null) proto = (Cell)ni.getProto();
                if (containsStandardCell(proto) || standardCellMap.get(proto) == standardCell) {
                    standardCellMap.put(cell, containsStandardCell);
                    containsStandardCellContexts.put(context, context);
                    return;
                }
            }
            standardCellMap.put(cell, doesNotContainStandardCell);
            emptyCellContexts.put(context, context);
            for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
                NodeInst ni = it.next();
                if (!ni.isCellInstance()) continue;
                if (ni.isIconOfParent()) continue;
                Cell proto = ni.getProtoEquivalent();
                if (proto == null) proto = (Cell)ni.getProto();
                if (standardCellMap.get(proto) == doesNotContainStandardCell) {
                    VarContext nicontext = context.push(ni);
                    emptyCellContexts.remove(nicontext);
                }
            }
        }

        public boolean visitNodeInst(Nodable ni, HierarchyEnumerator.CellInfo info) {
            return true;
        }

        /**
         * True if the cell contains standard cells (but false if it does not,
         * or if it is a standard cell.
         * @param cell the cell in question
         * @return true if the clel contains a standard cell, false otherwise.
         */
        public boolean containsStandardCell(Cell cell) {
            if (standardCellMap.get(cell) == containsStandardCell)
                return true;
            return false;
        }

        /**
         * Get the standard cells in the hiearchy after the hierarchy has
         * been enumerated
         * @return a set of the standard cells
         */
        public Set<Cell> getStandardCellsInHier() {
            return getCells(standardCell);
        }

        /**
         * Get the cells that contain standard cells
         * in the hiearchy after the hierarchy has
         * been enumerated
         * @return a set of the cells that contain standard cells
         */
        public Set<Cell> getContainsStandardCellsInHier() {
            return getCells(containsStandardCell);
        }

        /**
         * Get the cells that do not contain standard cells
         * in the hiearchy after the hierarchy has
         * been enumerated
         * @return a set of the cells that contain standard cells
         */
        public Set<Cell> getDoesNotContainStandardCellsInHier() {
            return getCells(doesNotContainStandardCell);
        }

        /**
         * Returns true if there was a name conflict, where two standard
         * cells from different libraries have the same name.
         * @return true if name conflict exists
         */
        public boolean getNameConflict() { return nameConflict; }

        public List<VarContext> getStandardCellsContextsInHier() {
            return standardCellContexts;
        }

        public Set<String> getContainsStandardCellContextsInHier() {
            Set<VarContext> contexts = containsStandardCellContexts.keySet();
            Set<String> sorted = new TreeSet<String>();
            for (VarContext context : contexts) {
                String s = context.getInstPath("/");
                sorted.add(s);
            }
            return sorted;
        }

        /**
         * Get the contexts of cells that do not contain standard cells
         * in the hierarchy. This set is minimal.
         * @return a set of var contexts
         */
        public Set<String> getEmptyCellContextsInHier() {
            Set<VarContext> contexts = emptyCellContexts.keySet();
            Set<String> sorted = new TreeSet<String>();
            for (VarContext context : contexts) {
                String s = context.getInstPath("/");
                sorted.add(s);
            }
            return sorted;
        }

        /**
         * Get cells that have been marked to not be extracted for DSPF
         * @return the cells
         */
        public Set<Cell> getCellsMarkedToNotExtractForDSPF() {
            return doNotExtractForDSPF;
        }

        /**
         * Get cells of the given type. The type is one of
         * Type.StandardCell
         * Type.ContainsStandardCell
         * Type.DoesNotContainStandardCell
         * @param type the type of cells to get
         * @return a set of cells
         */
        private Set<Cell> getCells(Integer type) {
            TreeSet<Cell> set = new TreeSet<Cell>();
            for (Map.Entry<Cell,Integer> entry : standardCellMap.entrySet()) {
                if (entry.getValue() == type)
                    set.add(entry.getKey());
            }
            return set;
        }
    }


}
