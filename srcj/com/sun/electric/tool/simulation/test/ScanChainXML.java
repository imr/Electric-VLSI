/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ScanChainXML.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation.test;

import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.tool.io.output.CellModelPrefs;

import java.util.*;
import java.io.*;

/**
 * This class implements
 * User: gainsley
 * Date: Nov 15, 2005
 */
public class ScanChainXML extends HierarchyEnumerator.Visitor {

    private HashMap<Cell,JtagController> jtagControllers;
    private JtagController currentJtagController;
    private HashMap<Cell,Cell> cellsToFlatten;
    private HashMap<Cell,Set<TraceElement>> elements;
    private HashMap<String,String> dataNetMap;      // key: full hierarchical name, value: flat name
    private HashMap<Starter,Starter> starters;
    private HashMap<Starter,String> duplicateStarters; // key: chain, value: chain to duplicate
    private HashMap<String,String> startFromExport; // key: export name, value: chain name
    private List<String> chipNames;
    private int currentChipName = 0;
    private String outputFile;
    private PrintWriter out;
    private File outFile;
    private boolean debugTracing;
    private boolean optimize;
    private String chipTDI;
    private String chipTDO;
    private List<String> scanElementInstanceNames;
    private boolean generateScanDataNets;
    private HashMap<Cell,Integer> elementsFlatCount;          // flat usage count

    public ScanChainXML() {
        cellsToFlatten = new HashMap<Cell,Cell>();
        elements = new HashMap<Cell,Set<TraceElement>>();
        dataNetMap = new HashMap<String,String>();
        starters = new HashMap<Starter,Starter>();
        duplicateStarters = new HashMap<Starter,String>();
        startFromExport = new HashMap<String,String>();
        jtagControllers = new HashMap<Cell,JtagController>();
        chipNames = new ArrayList<String>();
        outputFile = null;
        out = new PrintWriter(System.out);
        outFile = null;
        debugTracing = false;
        optimize = true;
        chipTDI = "TDI";
        chipTDO = "TDO";
        scanElementInstanceNames = new ArrayList<String>();
        generateScanDataNets = true;
        elementsFlatCount = new HashMap<Cell,Integer>();

    }

    /**
     * Start tracing all the scan chains from the any instances of specified
     * jtag controller
     */
    public void start(String libName, String cellName) {
        Library lib = Library.findLibrary(libName);
        if (lib == null) {
            System.out.println("Did not find library "+libName+" for starting chain analysis in cell "+cellName);
            return;
        }
        Cell cell = lib.findNodeProto(cellName);
        if (cell == null) {
            System.out.println("Did not find cell "+cellName+" for starting chain analysis, in library "+libName);
            return;
        }

        // extract chains from hierarchy
        HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, this, Netlist.ShortResistors.ALL);

        // issue warnings for hier instance names specified that were not found
        for (String name : scanElementInstanceNames) {
            System.out.println("Warning: instance not found in hierarchy: "+name);
        }

        List<Starter> chains = new ArrayList<Starter>();

        // the jtag controllers start from the TDI export
        Set<TraceElement> ents = elements.get(cell);
        Entity jtagControllerChain = null;
        for (TraceElement e : ents) {
            if (!(e instanceof Entity)) continue;
            if (e.getInport() != null && e.getInport().equals(chipTDI)) {
                jtagControllerChain = (Entity)e;
                break;
            }
        }
        if (jtagControllerChain == null && startFromExport.keySet().size() == 0) {
            System.out.println("Unable to find JtagController chain that starts with export "+chipTDI+" and ends with export "+chipTDO+". Aborting");
            return;
        }

        // get starters at this cell
        for (Starter s : starters.keySet()) {
            if (s.getCell() == cell) {
                chains.add(s);
            }
        }

        // create list of jtag instances to represent chain of jtag controllers
        // Each jtag controller is set in a "chip", because the test code can
        // handle multiple chips, but not multiple jtag controllers per chip.
        List<JtagInstance> jtags = new ArrayList<JtagInstance>();
        Set<Entity> entitiesUsed = new TreeSet<Entity>();

        if (jtagControllerChain != null) {
            List<VarContext> contexts = jtagControllerChain.getContextsOfJtagInstances();
            for (VarContext context : contexts) {
                // create a new jtag controller for each instance
                Nodable no = context.getNodable();
                Cell jtagCell = (Cell)no.getProto();
                JtagController controller = jtagControllers.get(jtagCell);
                if (controller == null) {
                    controller = jtagControllers.get(jtagCell.contentsView());
                }
                if (controller == null) {
                    System.out.println("Unable to find jtag controller for "+context.getInstPath(".")+", cell "+jtagCell.describe(false));
                    continue;
                }
                JtagInstance inst = new JtagInstance(context, controller, getNextChipName());
                jtags.add(inst);
            }

            // Find out which starter chains belong to which jtag instances
            for (Starter s : chains) {
                VarContext context = s.getContextOfFirstElement(VarContext.globalContext, jtagControllers);
                if (context == null) {
                    System.out.println("No context found for starter "+s.getKey());
                    continue;
                }
                String hier = context.getInstPath(".");
                //System.out.println("Starter "+s.getKey()+" has context "+hier);
                boolean used = false;
                for (JtagInstance inst : jtags) {
                    if (context.getInstPath(".").equals(inst.getContext().getInstPath("."))) {
                        // if there are duplicate chains from this one, add copy
                        for (Map.Entry<Starter,String> dup : duplicateStarters.entrySet()) {
                            Starter copy = dup.getKey();
                            String origChain = dup.getValue();
                            if (copy.getCell() == inst.controller.getCell() && origChain.equals(s.getChain())) {
                                copy.copyContentsOf(s);
                                inst.addStarter(copy);
                            }
                        }
                        inst.addStarter(s);
                        // Determine which Entity definitions should be written to the file
                        entitiesUsed.addAll(s.getEntitiesUsed());
                        used = true;
                        break;
                    }
                }
                if (!used)
                    System.out.println("Warning: starter "+s.getKey()+" not used by any jtag controllers");
            }
        }
        Collections.sort(chains);

        //
        // Get list of chains to write when starting from Exports
        //
        Set<TraceElement> topCellEntities = elements.get(cell);
        List<Starter> chainsFromExports = new ArrayList<Starter>();
        for (String export : startFromExport.keySet()) {
            if (jtagControllerChain == null) {
                // create dummy controller chain
                jtagControllerChain = new Entity(cell, "TDI", "TDO", false, new ArrayList<Instance>(), null);
            }
            String chainName = startFromExport.get(export);
            TraceElement chain = null;
            for (TraceElement e : topCellEntities) {
                if (e.getInport() != null && e.getInport().equals(export)) {
                    chain = e;
                    break;
                }
            }
            if (chain != null && (chain instanceof Entity)) {
                Entity ent = (Entity)chain;
                Starter starter = new Starter(ent.getCell(), ent.getOutport(),
                        ent.getInstances(), chainName, 0, null, null);
                chainsFromExports.add(starter);
                chains.add(starter);
                entitiesUsed.addAll(starter.getEntitiesUsed());
            }
        }
        if (chainsFromExports.size() != 0) {
            // create dummy jtag instance
            JtagInstance inst = new JtagInstance(VarContext.globalContext, currentJtagController, getNextChipName());
            for (Starter s : chainsFromExports) {
                inst.addStarter(s);
            }
            jtags.add(inst);
        }

        // write header
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("\n<!--");
        out.println("    Document      : "+outputFile);
        out.println("    Author        : automatically generated by Electric");
        out.println("    Description   : none");
        out.println("-->\n");
        out.println();
        out.println("<!DOCTYPE ChainG SYSTEM \"file:ChainG.dtd\" [");

        // print out flat data net specs as entities
        if (generateScanDataNets) {
            for (Starter chain : chains) {
                if (chain.getLength() == 0) continue;
                out.println("<!ENTITY "+chain.getChain()+"_dataNets '");
                chain.writeDataNets(out, new StringBuffer("  "), VarContext.globalContext, dataNetMap);
                out.println("'>");
            }
        }

        //
        // Perform any optimizations
        // This needs to occur after writing the dataNets, but before
        // writing the entities and chains for the hierarchical scan chain
        //
        if (optimize) {
            optimize(chains);

        }

        // print entities
        for (Entity ent : entitiesUsed) {
            ent.writeDefinition(out);
        }
        out.println("]>");
        out.println();

        // write chains
        StringBuffer indent = new StringBuffer();
        out.println("<ChainG>");
        indent.append("\t");
        out.println(indent+"<system>");
        indent.append("\t");

        int totalFlatCount = 0;
        for (JtagInstance jtag : jtags) {
            List<Starter> starters = jtag.getStarters();
            if (starters == null || starters.size() == 0) {
                System.out.println("No chains defined as starting from jtag instance "+jtag.getHierName());
                continue;
            }
            System.out.println("Writing chains for jtag instance "+jtag.getHierName());
            Collections.sort(starters, new StarterByOpcodeCompare());
            String chipName;
            int lengthIR;
            chipName = jtag.getChipName();
            lengthIR = jtag.controller.lengthIR;

            // write out all chains
            out.println(indent+"<chip name=\""+chipName+"\" lengthIR=\""+lengthIR+"\">");
            indent.append("\t");
            for (Starter chain : starters) {
                int length = chain.getLength();
                totalFlatCount += length;
                System.out.println("Chain "+chain.chain+" has length "+length);
                if (length == 0) continue;
                chain.write(out, indent);
            }
            indent.setLength(indent.length() - 1);
            out.println(indent+"</chip>");
        }
        if (generateScanDataNets) {
            // write out all data nets
            out.println(indent+"<scandatanets>");
            indent.append("\t");
            for (Starter chain : chains) {
                if (chain.getLength() == 0) continue;
                out.println(indent+"<datachain name=\""+chain.getChain()+"\"> &"+chain.getChain()+"_dataNets; </datachain>");
            }
            indent.setLength(indent.length() - 1);
            out.println(indent+"</scandatanets>");
        }


        indent.setLength(indent.length() - 1);
        out.println(indent+"</system>");
        indent.setLength(indent.length() - 1);
        out.println("</ChainG>");

        out.flush();

        if (outFile != null) {
            System.out.println("Wrote XML file to "+outFile.getAbsolutePath());
            out.flush();
            out.close();
        } else {
            System.out.println("Wrote XML file to console");
        }

        System.out.println("Total Flat Count by Summation of Chains: "+totalFlatCount);
        System.out.println("Total Flat Count by Hierarchy Enumeration: "+elementsFlatCount.get(cell));
        if (elementsFlatCount.get(cell) != totalFlatCount) {
            System.out.println("Mismatch, printing Hierarchy Enumerated count by cell:");
            printFlatCount(cell, 0, new HashMap<Cell,Cell>());
            System.out.println("Please Note that the count may mismatch if the boundary scan chain is counted twice by the XML");
        }
    }

    /**
     * Specify a scan chain element.  When an instance of this is found, it is will
     * be parsed as one bit in the scan chain.
     * @param libName name of the library containing the cell
     * @param cellName name of the cell to be defined as a scan chain element.
     * @param access what can be done with the data. A combination of "R" for read,
     * "W" for write, and "S" for shadow. For example: "RW".
     * @param clears the state set after master clear, "H" for high, "L" for low, "-" for unused.
     * @param inport the name of input data port, typically "sin".
     * May contain index info, such as "s[1]"
     * @param outport the name of the output data port, typically "sout".
     * May contain index info, such as "ss[1]"
     */
    public void addScanChainElement(String libName, String cellName, String access, String clears,
                                    String inport, String outport) {
        addScanChainElementAllViews(getCell(libName, cellName), access, clears, inport, outport, false, "", "", null);
    }

    /**
     * Specify a scan chain element.  When an instance of this is found, it is will
     * be parsed as one bit in the scan chain.
     * @param libName name of the library containing the cell
     * @param cellName name of the cell to be defined as a scan chain element.
     * @param access what can be done with the data. A combination of "R" for read,
     * "W" for write, and "S" for shadow. For example: "RW".
     * @param clears the state set after master clear, "H" for high, "L" for low, "-" for unused.
     * @param inport the name of input data port, typically "sin".
     * May contain index info, such as "s[1]"
     * @param outport the name of the output data port, typically "sout".
     * May contain index info, such as "ss[1]"
     * @param dataport the name of the port the scan data is read from and written to. May include options
     * R, W, or I for (Readable,Writable,Inverted) in parenthesis at the end. Ex: dout(RW)
     * @param dataport2 another port for data like dataport, with the same format.
     */
    public void addScanChainElement(String libName, String cellName, String access, String clears,
                                    String inport, String outport, String dataport, String dataport2) {
        addScanChainElementAllViews(getCell(libName, cellName), access, clears, inport, outport, false, dataport, dataport2, null);
    }

    /**
     * Specify a pass through element.  Pass through elements are found in series in
     * the scan chain, but are not scan chain elements themselves.  Examples of this are
     * inverters and buffers that buffer the scan chain data.
     * @param libName name of the library containing the cell
     * @param cellName name of the cell to be defined as a pass through element
     * @param inport the name of the input port that passes data through
     * May contain index info, such as "s[1]"
     * @param outport the name of the output port that passes data through
     * May contain index info, such as "ss[1]"
     */
    public void addPassThroughCell(String libName, String cellName, String inport, String outport) {
        addScanChainElementAllViews(getCell(libName, cellName), null, null, inport, outport, true, "", "", null);
    }

    /**
     * Specify a pass through element.  Pass through elements are found in series in
     * the scan chain, but are not scan chain elements themselves.  Examples of this are
     * inverters and buffers that buffer the scan chain data.
     * This version limits the pass through to a particular instance specified
     * by the instance name field.
     * @param libName name of the library containing the cell
     * @param cellName name of the cell to be defined as a pass through element
     * @param inport the name of the input port that passes data through
     * May contain index info, such as "s[1]"
     * @param outport the name of the output port that passes data through
     * May contain index info, such as "ss[1]"
     * @param instanceName limit to the particular instance name.
     */
    public void addPassThroughCell(String libName, String cellName, String inport, String outport, String instanceName) {
        addScanChainElementAllViews(getCell(libName, cellName), null, null, inport, outport, true, "", "", instanceName);
        if (instanceName != null)
            scanElementInstanceNames.add(instanceName);
    }

    /**
     * Specify a cell to flatten.  The XML is hierarchical, but sometimes you don't need
     * or want all that hierarchy.  This specifies a cell that will be flattened
     * @param libName the library that contains the cell
     * @param cellName the name of the cell
     */
    public void addCellToFlatten(String libName, String cellName) {
        Cell cell = getCell(libName, cellName);
        if (cell != null) {
            cellsToFlatten.put(cell, cell);
        }
        cell = getCellOtherView(cell);
        if (cell != null) {
            cellsToFlatten.put(cell, cell);
        }
    }

    /**
     * Specify the JTAG Controller.  All scan chains are assumed to start, and end, at the
     * JTAG Controller.  This specifies the jtag controller.
     * This method is ignored if "startFromExport" is used.
     * @param jtagLib the name of the library that holds the jtag controller cell
     * @param jtagCellName the name of the cell that is the jtag controller
     * @param lengthIR the number of instruction register bits in the jtag controller.
     */
    public void setJtagController(String jtagLib, String jtagCellName, int lengthIR) {
        setJtagController(jtagLib, jtagCellName, lengthIR, "TDI", "TDOb");
    }

    /**
     * Specify the JTAG Controller.  All scan chains are assumed to start, and end, at the
     * JTAG Controller.  This specifies the jtag controller.
     * This method is ignored if "startFromExport" is used.
     * @param jtagLib the name of the library that holds the jtag controller cell
     * @param jtagCellName the name of the cell that is the jtag controller
     * @param lengthIR the number of instruction register bits in the jtag controller.
     */
    public void setJtagController(String jtagLib, String jtagCellName, int lengthIR, String inport, String outport) {
        Cell cell = getCell(jtagLib, jtagCellName);
        currentJtagController = new JtagController(cell, inport, outport, false, lengthIR);
        addJtagControllerAllViews(cell, inport, outport, lengthIR);
    }

    /**
     * Set the port name for the chip TDI signal. This is the input data to the
     * first jtag controller on chip.
     * @param TDIport
     */
    public void setChipTDI(String TDIport) {
        chipTDI = TDIport;
    }

    /**
     * Set the port name for the chip TDO signal. This is the output data from the
     * last jtag controller on the chip.
     * @param TDOport
     */
    public void setChipTDO(String TDOport) {
        chipTDO = TDOport;
    }

    /**
     * Add a port to the JTAG Controller that serves as a starting point for a scan chain.
     * A JTAG Controller may have several ports that each have a scan chain attached.
     * The JTAG Controller must have already been specified using setJtagController.
     * This method is ignored if "startFromExport" is used.
     * @param opcode the opcode for this scan chain
     * @param soutPortName the port name that outputs data for the scan chain.
     * May contain index info, such as "leaf1[1]"
     * @param sinPortName the port name that scan data returns to.
     * May contain index info, such as "leaf1[8]"
     * @param chainName the name given to this scan chain
     */
    public void addJtagPort(int opcode, String soutPortName, String sinPortName, String chainName) {
        if (currentJtagController == null) {
            System.out.println("Can't add port "+soutPortName+" because the jtag controller has not been defined yet");
            return;
        }
        //currentJtagController.addPort(soutPortName, chainName, opcode);
        addJtagPortStarterAllViews(currentJtagController.getCell(), sinPortName, soutPortName, chainName, opcode);
/*        Starter s = new Starter(currentJtagController.getCell(), soutPortName, null, chainName, opcode, null);
        // special case: for boundary scan chains we specify two chains
        // from the same port. The algorithm assumes unique chains, so
        // we have to duplicate the chain when printing it out, not during enumeration
        for (Starter olds : starters.keySet()) {
            if (olds.getCell() == currentJtagController.getCell() &&
                    olds.getOutport().equals(soutPortName)) {
                // already specified
                duplicateStarters.put(s, olds.getChain());
                return;
            }
        }
        starters.put(s, chainName);
        add(s);
        // also add as an end element
        ScanChainElement e = new ScanChainElement(currentJtagController.getCell(), "", "", sinPortName, null, true, "", "");
        add(e);*/
    }

    /**
     * Start tracing a chain from the specified export in the start cell.  This
     * is used to trace a section of the scan chain. This
     * traces only one chain.
     * @param exportName the export that starts the chain
     * @param chainName the name for the chain
     */
    public void startFromExport(String exportName, String chainName) {
        startFromExport.put(exportName, chainName);
    }

    /**
     * Specify the name of the chip. Used when writing the chip name to the file.
     * @param name the chip name
     */
    public void setChipName(String name) {
        chipNames.add(name);
    }

    /**
     * Set true to generate scan data nets in the output file, false not to.
     * Then is set to true by default.
     * @param generate whether or not to generate the scan data nets in the output file.
     */
    public void generateScanDataNets(boolean generate) {
        generateScanDataNets = generate;
    }

    private String getNextChipName() {
        String name;
        if (currentChipName >= chipNames.size()) {
            name = "dummy"+currentChipName;
            System.out.println("Warning: creating dummy chip name because not enough calls to setChipName() vs number of jtag controllers");
        } else {
            name = chipNames.get(currentChipName);
        }
        currentChipName++;
        return name;
    }

    /**
     * Set the output file.  This may be an absolute or relative path.  If this
     * option is not specified, the output goes to the Electric console.
     * @param file the name of the file.
     */
    public void setOutput(String file) {
        // try to open outputFile
        outputFile = file;
        try {
            outFile = new File(outputFile);
            out = new PrintWriter(new BufferedWriter(new FileWriter(outFile)));
        } catch (IOException e) {
            System.out.println(e.getMessage() + "\nWriting XML to console");
            outFile = null;
            outputFile = null;
        }
    }

    /**
     * Call this method to print out tracing debug information
     */
    public void debugTracing() {
        debugTracing = true;
    }

    private void addScanChainElementAllViews(Cell cell, String access, String clears,
                                    String inport, String outport, boolean passThrough,
                                    String dataport, String dataport2,
                                    String instanceName) {
        if (cell == null) return;
        for (Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); ) {
            Cell otherCell = it.next();
            if (otherCell.isSchematic() || otherCell.getView() == View.LAYOUT) {
                ScanChainElement e = new ScanChainElement(otherCell, access, clears, inport, outport,
                        passThrough, dataport, dataport2, instanceName);
                add(e);
            }
        }
    }
    
    private void addJtagPortStarterAllViews(Cell cell, String sinPortName, String soutPortName, String chainName, int opcode) {
        if (cell == null) return;
        for (Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); ) {
            Cell otherCell = it.next();
            if (otherCell.isSchematic() || otherCell.getView() == View.LAYOUT) {
                Starter s = new Starter(otherCell, soutPortName, null, chainName, opcode, null, null);
                // special case: for boundary scan chains we specify two chains
                // from the same port. The algorithm assumes unique chains, so
                // we have to duplicate the chain when printing it out, not during enumeration
                boolean add = true;
                for (Starter olds : starters.keySet()) {
                    if (olds.getCell() == otherCell &&
                            olds.getOutport().equals(soutPortName)) {
                        // already specified
                        duplicateStarters.put(s, olds.getChain());
                        add = false;
                    }
                }
                if (add) {
                    starters.put(s, s);
                    add(s);
                    // ender
                    ScanChainElement e = new ScanChainElement(otherCell, "", "", sinPortName, null, true, "", "", null);
                    add(e);
                }
            }
        }
    }

    private void addJtagControllerAllViews(Cell cell, String inport, String outport, int lengthIR) {
        if (cell == null) return;
        for (Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); ) {
            Cell otherCell = it.next();
            if (otherCell.isSchematic() || otherCell.getView() == View.LAYOUT) {
                JtagController jtag = new JtagController(otherCell, inport, outport, false, lengthIR);
                jtagControllers.put(otherCell, jtag);
                add(jtag);
            }
        }
    }

    private Cell getCellOtherView(Cell cell) {
        View otherView = null;
        if (cell.getView() == View.LAYOUT) otherView = View.SCHEMATIC;
        if (cell.isSchematic()) otherView = View.LAYOUT;
        if (cell.isIcon()) otherView = View.LAYOUT;
        Cell otherCell = getCell(cell, otherView);
        return otherCell;
    }

    private Cell getCell(String libName, String cellName) {
        Library lib = Library.findLibrary(libName);
        if (lib == null) {
            System.out.println("Warning: Did not find library "+libName+" for cell "+cellName);
            return null;
        }
        Cell cell = lib.findNodeProto(cellName);
        if (cell == null) {
            System.out.println("Warning: Did not find cell "+cellName+", in library "+libName);
            return null;
        }
        if (cell.isIcon()) {
            cell = getCell(cell, View.SCHEMATIC);
            if (cell == null) {
                System.out.println("Warning: Did not find cell schematic cell for "+cellName+"{ic}, in library "+libName);
                return null;
            }
        }
        return cell;
    }

    private Cell getCell(Cell cell, View view) {
        if (view == null) return null;
        for (Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); ) {
            Cell acell = it.next();
            if (acell.getView() == view) {
                return acell;
            }
        }
        return null;
    }

    private void add(TraceElement e) {
        Set<TraceElement> traceElements = elements.get(e.cell);
        if (traceElements == null) {
            traceElements = new TreeSet<TraceElement>();
            elements.put(e.getCell(), traceElements);
        }
        for (TraceElement ele : traceElements) {
            // same definition
            if (ele.getClass() != e.getClass()) continue;
            if (ele.getCell() != e.getCell()) continue;
            boolean namesSame = false;
            if (ele.getInstanceName() != null && e.getInstanceName() != null) {
                if (ele.getInstanceName().equals(e.getInstanceName()))
                    namesSame = true;
            }
            if (ele.getInstanceName() == null && e.getInstanceName() == null)
                namesSame = true;
            if (!namesSame) continue;
            boolean inportsSame = false;
            if (ele.inport == e.inport) inportsSame = true;
            if (ele.inport != null && e.inport != null && ele.inport.equals(e.inport)) inportsSame = true;
            if (!inportsSame) continue;
            boolean outportsSame = false;
            if (ele.outport == e.outport) inportsSame = true;
            if (ele.outport != null && e.outport != null && ele.outport.equals(e.outport)) outportsSame = true;
            if (inportsSame && outportsSame) {
                System.out.println("Warning, already cell "+e.cell.describe(false)+", inport "+e.inport+" already added");
                return;
            }
        }
        traceElements.add(e);
    }

    // ------------------------------------------------------------------


    public boolean enterCell(HierarchyEnumerator.CellInfo info) {
        Cell cell = info.getCell();
        Set<TraceElement> traceElements = elements.get(cell);
        if (traceElements == null) return true;

        boolean scanChainElement = false;
        for (TraceElement e : traceElements) {
            if (e instanceof ScanChainElement) {
                // We are inside a ScanChainElement
                // get the data net, and do not enumerate
                ScanChainElement sc = (ScanChainElement)e;
                mapDataNet(info, sc.getDataNet());
                mapDataNet(info, sc.getDataNet2());
                scanChainElement = true;
            }
        }
        if (scanChainElement) return false;
        return true;
    }

    private void mapDataNet(HierarchyEnumerator.CellInfo info, DataNet dataNet) {
        if (dataNet == null) return;
        Network net = getNetwork(info.getNetlist(), dataNet.getName());
        String flatContext = "";
        String netName = dataNet.getName();
        if (net != null) {
            // may go up hier
            HierarchyEnumerator.NetDescription desc = info.netIdToNetDescription(info.getNetID(net));
            netName = desc.getNet().getName();
            flatContext = desc.getCellInfo().getContext().getInstPath(".x");            
        }
        if (net == null) {
            // see if name is hierarchical (go down hier)
            VarContext netContext = info.getContext();
            String name = dataNet.getName();
            Cell cell = info.getCell();
            if (name.contains(".")) {
                String hier[] = name.split("\\.");
                // go down hierarchy
                for (int i=0; i<hier.length-1; i++) {
                    String instName = hier[i];
                    for (Iterator<Nodable> nit = cell.getNodables(); nit.hasNext(); ) {
                        Nodable no = nit.next();
                        if (no.getName().equals(instName) && no.isCellInstance()) {
                            Cell proto = (Cell)no.getProto();
                            cell = proto.getEquivalent();
                            if (cell == null) cell = proto;
                            netContext = netContext.push(no);
                        }
                    }
                }
                // last field is net name, check for it in cell
                net = getNetwork(cell.getNetlist(), hier[hier.length-1]);
                flatContext = netContext.getInstPath(".x");
                netName = hier[hier.length-1];
            }
        }
        if (net == null) {
            System.out.println("Error: Cannot find data net "+dataNet.getName()+" in cell "+info.getCell()+", please check dataNet spec for this cell");
            return;
        }
        String contextualName = ScanChainElement.getDataNetPath(dataNet, info.getContext());
        if (flatContext != null && !flatContext.equals("")) {
            flatContext = "x"+flatContext+".";
        } else {
            flatContext = "";
        }
        netName = netName.replace('.', '_');
        String flatName = flatContext + netName;
        dataNetMap.put(contextualName, flatName);
        //if (debugTracing) System.out.println("  mapping dataNet "+contextualName+" --> "+flatName);
    }

    public void exitCell(HierarchyEnumerator.CellInfo info) {

        Set<TraceElement> processedElements = elements.get(info.getCell());
        if (processedElements != null) {
            boolean foundinst = false;
            for (TraceElement e : processedElements) {
                if (e.getInstanceName() != null) {
                    foundinst = true; break;
                }
            }
            if (!foundinst) return;     // already cached, no instances named
        }

        // see if we have already built an entity for this cell
        //if (!doNotSkip && elements.get(info.getCell()) != null) return;           // already cached

/*
        if (info.getContext() == VarContext.globalContext) {
            System.out.println("global context marker for breakpoint");
        }
*/

        String currentHier = info.getContext().getInstPath(".");
        if (debugTracing) System.out.println("---------------------------");
        if (debugTracing) System.out.println("Tracing cell "+info.getCell().describe(false)+" "+currentHier);

        // check for scan chain elements
        HashMap<String,Instance> instancesByInput = new HashMap<String,Instance>();
        HashMap<String,Instance> instancesByOutput = new HashMap<String,Instance>();
        List<Instance> instancesWithNoInput = new ArrayList<Instance>();

        boolean instanceElementPresent = false;
        int flatCount = 0;
        for (Iterator<NodeInst> it = info.getCell().getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();
            if (!ni.isCellInstance()) continue;            // not a cell, continue
            if (ni.isIconOfParent()) continue;
            Cell cell = (Cell)ni.getProto();
            Cell contents = cell.contentsView();
            if (contents != null) cell = contents;          // get contents view

            if (info.getCell().isSchematic() && enumerateLayoutView(cell)) {
                Cell laycell = getLayoutView(cell);
                if (laycell != null) cell = laycell;
            }

            Set<TraceElement> traceElements = elements.get(cell);
            if (traceElements == null) continue;

            // Each cell could have several elements defined for it,
            // since an element is defined by a Cell + input port pair
            for (TraceElement e : traceElements) {

                // Arrays here are handled for cells and pass throughs, but
                // special cased for scan chain element so that we have one entry with
                // a length > 1
                boolean arrayHere = true;
                if (e instanceof ScanChainElement) {
                    ScanChainElement sc = (ScanChainElement)e;
                    if (!sc.isPassThrough()) {
                        arrayHere = false;
                        flatCount += ni.getNameKey().busWidth();
                    }
                }
                if (ni.getNameKey().busWidth() > 1 && arrayHere) {
                    // expand arrayed nodeinsts that are not scan chain elements
                    for (int i=0; i<ni.getNameKey().busWidth(); i++) {
                        Nodable no = Netlist.getNodableFor(ni, i);
                        createInstance(no, info, instancesByInput, instancesByOutput,
                                instancesWithNoInput, e);
                    }
                } else {
                    // just create instance for the nodeinst
                    // but check if instanceName is set. If so, must match, otherwise, ignore
                    String instanceName = info.getContext().push(ni).getInstPath(".");
                    if (e.getInstanceName() != null) {
                        if (e.getInstanceName().equals(instanceName)) {
                            createInstance(ni, info, instancesByInput, instancesByOutput,
                                    instancesWithNoInput, e);
                            scanElementInstanceNames.remove(instanceName);
                        }
                        instanceElementPresent = true;
                    } else {
                        // no instance name means all instances should be recognized
                        createInstance(ni, info, instancesByInput, instancesByOutput,
                                instancesWithNoInput, e);
                    }
                }
            }
            Integer ii = elementsFlatCount.get(cell);
            if (ii != null) {
                flatCount += (ii * ni.getNameKey().busWidth());
            }
        }
        elementsFlatCount.put(info.getCell(), new Integer(flatCount));

        // instances with no input are starters.
        // also, instances with no prev are local starters (their input is likely exported)
        for (Instance inst : instancesByInput.values()) {
            if (inst.prev == null) {
                instancesWithNoInput.add(inst);
            }
        }

        // get traceElements so we can put newly created Entities in them
        Set<TraceElement> traceElements = elements.get(info.getCell());
        if (traceElements == null) {
            traceElements = new TreeSet<TraceElement>();
            elements.put(info.getCell(), traceElements);
        }

        // create Entity.  It may be a starter if start of chain is in this cell
        String entName = null;
        if (instanceElementPresent) {
            entName = info.getContext().getInstPath(".");
        }
        int startercount = 0;

        // find the start instances
        for (Instance inst : instancesWithNoInput) {
            // this is a start instance, follow it to the end, and make an Entity out of it
            Instance last = inst;
            List<Instance> instances = new ArrayList<Instance>();
            instances.add(last);
            while (last.next != null) {
                last = last.next;
                instances.add(last);
            }
            // check if input of start is exported, last check is to make sure it's a real export
            // and not a power/ground export
            String inex = null;
            if (inst.innet != null && inst.innet.isExported() && inst.innet.getExports().hasNext()) {
                inex = inst.innet.getName();
            }
            // check if output of end is exported
            String outex = null;
            if (last.outnet != null && last.outnet.isExported() && last.outnet.getExports().hasNext()) {
                outex = last.outnet.getName();
            }

            Entity ent;
            String entity = "Entity";
            // check if first instance is the start of a chain
            if (starters.containsKey(inst.e) && (inst.e instanceof Starter)) {
                // this entity will be the new starter
                // starters are always flat
                entName = info.getContext().getInstPath(".");
                Starter oldStart = (Starter)inst.e;
                ent = new Starter(info.getCell(), outex, instances, oldStart.chain, oldStart.opcode, oldStart, entName);

                //System.out.println("Creating Starter for cell "+info.getCell().describe(false)+
                //        ", in="+inex+", out="+outex+", chain="+oldStart.chain);

                starters.put((Starter)ent, (Starter)ent);
                startercount++;

                //starters.remove(inst.e);
                entity = "Starter";
                if (outex == null && last.e.outport != null) {
                    // not an ender (ends have null outport definitions), and not exported, must be error
                    String outnet = last.outnet == null ? null : last.outnet.getName();
                    if (inst.e.isPassThrough()) {
                        System.out.println("Warning: pass-through instance "+last.no.getName()+" in cell "+info.getCell().describe(false)+
                                " has nothing connected to its output port "+last.e.outport+"("+outnet+")");
                    } else {
                        System.out.println("Error: instance "+last.no.getName()+" in cell "+info.getCell().describe(false)+
                                " has nothing connected to its output port "+last.e.outport+"("+outnet+")");
                    }
                    continue;
                }
            } else {
                // create Entity

                // check if this chain is just a passthrough chain, and has no real elements
                boolean passThrough = true;
                for (Instance entinst : instances) {
                    if (!entinst.getTraceElement().isPassThrough()) {
                        passThrough = false;
                        break;
                    }
                }

                // if totally unconnected and this chain contains only passthrough elements, don't issue an error
                if (inex == null && inst.getTraceElement().getInport() != null &&
                    outex == null && last.getTraceElement().getOutport() != null) {
                    if (passThrough) {
                        String innet = inst.innet == null ? null : inst.innet.getName();
                        String outnet = last.outnet == null ? null : last.outnet.getName();
                        System.out.println("Warning: Pass through chain of "+info.getCell().describe(false)+" from input net \""+
                                innet+"\" to output net \""+outnet+"\" has both networks not exported, so I am ignoring it");
                        continue;
                    }
                }

                if (inex == null && inst.e.inport != null) {
                    // not a starter, and start network is not exported.  Must be error
                    String innet = inst.innet == null ? null : inst.innet.getName();
                    if (passThrough) {
                        System.out.println("Warning: pass-through instance "+inst.no.getName()+" in cell "+info.getCell().describe(false)+
                                " has nothing connected to its input port "+inst.e.inport+"("+innet+")");
                    } else {
                        System.out.println("Error: instance "+inst.no.getName()+" in cell "+info.getCell().describe(false)+
                                " has nothing connected to its input port "+inst.e.inport+"("+innet+")");
                    }
                    continue;
                }
                if (outex == null && last.e.outport != null) {
                    // not an ender (ends have null outport definitions), and not exported, must be error
                    String outnet = last.outnet == null ? null : last.outnet.getName();
                    if (passThrough) {
                        System.out.println("Warning: pass-through instance "+last.no.getName()+" in cell "+info.getCell().describe(false)+
                                " has nothing connected to its output port "+last.e.outport+"("+outnet+")");
                    } else {
                        System.out.println("Error: instance "+last.no.getName()+" in cell "+info.getCell().describe(false)+
                                " has nothing connected to its output port "+last.e.outport+"("+outnet+")");
                    }
                    continue;
                }
                ent = new Entity(info.getCell(), inex, outex, passThrough, instances, entName);
            }
            if (debugTracing) System.out.println("Creating "+entity+" for cell "+info.getCell().describe(false)+
                    ", in="+inex+", out="+outex+", key="+ent.getKey());
            traceElements.add(ent);
        }
/*
        if (startercount > 0) {
            if (entName != null)
                System.out.println("Created "+startercount+" starters in "+entName);
            else
                System.out.println("Created "+startercount+" starters in "+info.getCell().describe(false));
        }
*/
    }

    private void createInstance(Nodable ni, HierarchyEnumerator.CellInfo info,
                                HashMap<String,Instance> instancesByInput,
                                HashMap<String,Instance> instancesByOutput,
                                List<Instance> instancesWithNoInput, TraceElement e) {
        Instance inst = new Instance(ni, info.getContext(), ni.getNameKey().busWidth(), e);
        // Hook up Instances in linked list, which denotes order of chain.
        Nodable inno = ni;
        Nodable outno = ni;
        if ((ni instanceof NodeInst) && ni.getNameKey().busWidth() > 1) {
            NodeInst nin = (NodeInst)ni;
            inno = Netlist.getNodableFor(nin, 0);
            outno = Netlist.getNodableFor(nin, nin.getNameKey().busWidth()-1);
        }
        if (e.inport != null) {
            // hook up to predecessor instance
            Network innet = getNetwork(inno, info.getNetlist(), e.inport);
            Instance prev = null;
            if (innet != null) {
                prev = instancesByOutput.get(innet.getName());
                if (prev != null) {
                    // make sure one inst not driving two insts
                    //checkBadFanout(prev, prev.next, inst);
                    inst.prev = prev;
                    prev.next = inst;
                    if (debugTracing) System.out.println("  Chaining "+prev.describeOutnet()+" -> "+inst.describeInnet());
                }
                // check for merging of chains
                Instance branch = instancesByInput.get(innet.getName());
                if (branch != null) {
                    if (prev != null) {
                        System.out.println("Error! Chain branches from: "+prev.describeOutput());
                    } else {
                        System.out.println("Error! Chain branches from net "+innet.getName()+" in cell "+ni.getParent().describe(false));
                    }
                    System.out.println("       into: "+inst.describeInput()+" (key="+inst.getTraceElement().getKey()+")");
                    System.out.println("       into: "+branch.describeInput()+" (key="+branch.getTraceElement().getKey()+")");
                }
                inst.innet = innet;
                instancesByInput.put(innet.getName(), inst);
            } else {
                System.out.println("Error: Can't find input network for port "+e.getInport()+" on element "+e.getCell().describe(false));
            }
        } else {
            instancesWithNoInput.add(inst);
        }

        if (e.outport != null) {
            // hook up to successor instance
            Network outnet = getNetwork(outno, info.getNetlist(), e.outport);
            Instance next = null;
            if (outnet != null) {
                next = instancesByInput.get(outnet.getName());
                if (next != null) {
                    //checkBadFanin(inst,  next.prev, next);
                    inst.next = next;
                    next.prev = inst;
                    if (debugTracing) System.out.println("  Chaining "+inst.describeOutnet()+" -> "+next.describeInnet());
                }
                Instance merge = instancesByOutput.get(outnet.getName());
                if (merge != null) {
                    if (next != null) {
                        System.out.println("Error! Chain merges into "+next.describeInput());
                    } else {
                        System.out.println("Error! Chain merges into net "+outnet.getName()+" in cell "+ni.getParent().describe(false));
                    }
                    System.out.println("       from: "+inst.describeOutput()+" (key="+inst.getTraceElement().getKey()+")");
                    System.out.println("       from: "+merge.describeOutput()+" (key="+merge.getTraceElement().getKey()+")");
                }
                inst.outnet = outnet;
                instancesByOutput.put(outnet.getName(), inst);
            } else {
                System.out.println("Error: Can't find output network for port "+e.getOutport()+" on element "+e.getCell().describe(false));
            }
        }
        if (debugTracing) {
            String innet = inst.innet == null ? null : inst.innet.getName();
            String outnet = inst.outnet == null ? null : inst.outnet.getName();
            System.out.println("  Created instance "+inst.no.getName()+
                " in="+e.inport+"("+innet+")"+
                " out="+e.outport+"("+outnet+") for element "+e.getKey());
        }
    }


    public boolean visitNodeInst(Nodable ni, HierarchyEnumerator.CellInfo info) {
        // We need to push into ScanChainElements one level to
        // get at the data net.  We also need to push into everything else,
        // except primitives which cannot be pushed into

        if (ni.isCellInstance()) {
            Cell cell = (Cell)ni.getProto();
            Cell schcell = cell.contentsView();
            if (schcell == null) schcell = cell;
            if (cell.isSchematic() && enumerateLayoutView(schcell)) {
                Cell layCell = getLayoutView(schcell);
                if (layCell != null) {
                    HierarchyEnumerator.enumerateCell(layCell, info.getContext().push(ni), this);
                    return false;
                }
            }
        }

        return true;
    }

    private boolean enumerateLayoutView(Cell cell) {
        return CellModelPrefs.spiceModelPrefs.isUseLayoutView(cell);
    }

    private Cell getLayoutView(Cell cell) {
        Cell layCell = null;
        for (Iterator<Cell> it = cell.getCellGroup().getCells(); it.hasNext(); ) {
            Cell c = it.next();
            if (c.getView() == View.LAYOUT) {
                return c;
            }
        }
        return null;
    }

    private void checkBadFanout(Instance prev, Instance next1, Instance next2) {
        if (next1 == null || next2 == null) return;
        if (prev == null) return;
        // all non-null, issue error
        System.out.println("Error! Chain branches from: "+prev.describeOutput());
        System.out.println("       into: "+next1.describeInput());
        System.out.println("       into: "+next2.describeInput());
    }

    private void checkBadFanin(Instance prev1, Instance prev2, Instance next) {
        if (prev1 == null || prev2 == null) return;
        if (next == null) return;
        // all non-null, issue error
        System.out.println("Error! Chain merges into "+next.describeInput());
        System.out.println("       from: "+prev1.describeOutput());
        System.out.println("       from: "+prev2.describeOutput());
    }

    private HashMap<Nodable,String> jtagControllerNames = new HashMap<Nodable,String>();
    private void setJtagControllerName(Nodable no, String name) {
        jtagControllerNames.put(no, name);
    }
    private String getJtagControllerName(Nodable no) {
        return jtagControllerNames.get(no);
    }

    private void printFlatCount(Cell cell, int indent, Map<Cell,Cell> alreadyPrinted) {
        if (elementsFlatCount.get(cell) == null) return;
        if (elementsFlatCount.get(cell) == 0) return;
        if (alreadyPrinted.containsKey(cell)) return;
        alreadyPrinted.put(cell, cell);
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<indent; i++) {
            buf.append(" ");
        }
        System.out.println(buf+cell.describe(false)+": "+elementsFlatCount.get(cell));
        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();
            if (!ni.isCellInstance()) continue;            // not a cell, continue
            if (ni.isIconOfParent()) continue;
            Cell c = (Cell)ni.getProto();
            Cell contents = c.contentsView();
            if (contents != null) c = contents;          // get contents view

            if (cell.isSchematic() && enumerateLayoutView(c)) {
                Cell laycell = getLayoutView(c);
                if (laycell != null) c = laycell;
            }
            printFlatCount(c, indent+2, alreadyPrinted);
        }
        
    }

    // ------------------------------------------------------------------

    private static class TraceElement implements Comparable {
        private final Cell cell;
        private final String inport;
        private final String outport;
        private final boolean passThrough;
        private String hierInstanceName;          // if limited to a particular instance

        private TraceElement(Cell cell, String inport, String outport,
                             boolean passThrough, String hierInstanceName) {
            this.cell = cell;
            this.inport = inport;
            this.outport = outport;
            this.passThrough = passThrough;
            this.hierInstanceName = hierInstanceName;
        }
        public static String getKey(Cell cell, String inport, String hierInstanceName) {
            String name = (hierInstanceName == null) ? "" : "_"+hierInstanceName; 
            String key = cell.getLibrary().getName()+"_"+cell.getName()+"_"+inport+name;
            key = key.replaceAll("[\\[\\]@]", "_");
            return key;
        }
        public String getKey() {
            return getKey(cell, inport, hierInstanceName);
        }
        public String getInstanceName() { return hierInstanceName; }
        public void setInstanceName(String name) { hierInstanceName = name; }
        public Cell getCell() { return cell; }
        public int compareTo(Object o) {
            TraceElement e = (TraceElement)o;
            return getKey().compareTo(e.getKey());
        }
        public boolean equals(Object o) {
            TraceElement e = (TraceElement)o;
            return getKey().equals(e.getKey());
        }
        public String getOutport() { return outport; }
        public String getInport() { return inport; }
        public boolean isPassThrough() { return passThrough; }
    }

    /** Defines a Scan Chain Element, which contains one bit of scan storage */
    private static class ScanChainElement extends TraceElement {
        private final String access;
        private final String clears;
        private final DataNet dataport;
        private final DataNet dataport2;

        private ScanChainElement(Cell cell, String access, String clears, String inport, String outport,
                                 boolean passThrough, String dataport, String dataport2, String hierInstanceName) {
            super(cell, inport, outport, passThrough, hierInstanceName);
            this.access = access;
            this.clears = clears;
            if (dataport == null || dataport.equals(""))
                this.dataport = null;
            else
                this.dataport = new DataNet(dataport);
            if (dataport2 == null || dataport2.equals(""))
                this.dataport2 = null;
            else
                this.dataport2 = new DataNet(dataport2);
        }
        public DataNet getDataNet() { return dataport; }
        public DataNet getDataNet2() { return dataport2; }
        public static String getDataNetPath(DataNet dataNet, VarContext context) {
            if (dataNet == null) return null;
            return context.getInstPath(".")+"."+dataNet.getName();
        }
        public String getAccess() { return access; }
        public String getClears() { return clears; }
        public boolean combinable(ScanChainElement other) {
            if (access == null && other.getAccess() != null) return false;
            if (access != null && other.getAccess() == null) return false;
            if (access != null && other.getAccess() != null && !access.equals(other.getAccess())) return false;
            if (clears == null && other.getClears() != null) return false;
            if (clears != null && other.getClears() == null) return false;
            if (clears != null && other.getClears() != null && !clears.equals(other.getClears())) return false;
            return true;
        }
    }

    /**
     * The networks that the scan chain elements reads from or
     * writes to.
     */
    private static class DataNet {
        public final String net;
        public final String options;        // options including parenthesis

        /**
         * Creates a data net object that describes a net that is written or read
         * to by a scan chain element.
         * @param netName the name of the data net, including any options prepended
         * in parenthesis. R for readable, W for writeable, I for inverted.
         * Ex: net26 or net26(R) or net26(RWI).
         */
        public DataNet(String netName) {
            int i = netName.indexOf('(');
            if (i != -1) {
                net = netName.substring(0, i);
                options = netName.substring(i, netName.length());
            } else {
                net = netName;
                options = "";
            }
        }

        /**
         * Creates a data net object that describes a net that is written or read
         * to by a scan chain element.
         * @param net the net name
         * @param options options describing the network: R for readable, W for
         * writable, I for inverted. Should be encased in parenthesis, such as
         * (RW).
         */
        public DataNet(String net, String options) {
            this.net = net;
            this.options = options;
        }

        public String toString() { return net+options; }
        public String getName() { return net; }
        public String getOptions() { return options; }
    }

    /**
     * Defines the scan chain elements for a Cell + Input Port pair.
     */
    private static class Entity extends TraceElement {
        private List<Instance> instances;
        private Entity(Cell cell, String inport, String outport, boolean passThrough,
                       List<Instance> instances, String hierInstanceName) {
            super(cell, inport, outport, passThrough, hierInstanceName);
            this.instances = new ArrayList<Instance>();
            if (instances != null) this.instances.addAll(instances);
        }
        public void writeDefinition(PrintWriter out) {
            if (getLength() == 0) return;

            out.println("<!ENTITY "+getKey()+" '");
            List<String> usedNames = new ArrayList<String>();
            for (Instance inst : instances) {
                inst.write(out, new StringBuffer("\t"), usedNames);
            }
            out.println("'>");
        }
        public void writeContents(PrintWriter out, StringBuffer indent) {
            List<String> usedNames = new ArrayList<String>();
            for (Instance inst : instances) {
                inst.write(out, indent, usedNames);
            }
        }
        public void writeDataNets(PrintWriter out, StringBuffer indent, VarContext context, HashMap<String,String> dataNetMap) {
            for (Instance inst : instances) {
                inst.writeDataNets(out, indent, context, dataNetMap);
            }
        }
        public int getLength() {
            int len = 0;
            for (Instance inst : instances) {
                len += inst.getLength();
            }
            return len;
        }
        public int flattenInstances(HashMap<Cell,Cell> cellsToFlatten, HashMap<Entity,Cell> alreadyFlattened) {
            int flattened = 0;
            if (alreadyFlattened.containsKey(this)) return 0;
            if (this instanceof Starter) return 0;              // can't flatten starters?

            for (int i=0; i<instances.size(); i++) {
                Instance inst = instances.get(i);
                // flatten instance if it's entity contains only 1 thing
                TraceElement ele = inst.getTraceElement();
                if (ele instanceof Entity) {
                    Entity ent = (Entity)ele;
                    // make sure the sub element is complete optimized in terms of flattening
                    flattened += ent.flattenInstances(cellsToFlatten, alreadyFlattened);

                    if (ent.getRealInstances().size() == 1 ||
                            cellsToFlatten.containsKey(ent.getCell())) {
                        // flatten, bring contents of instance into this entity
                        instances.remove(i);
                        List<Instance> newInstances = new ArrayList<Instance>();
                        for (Instance subinst : ent.getRealInstances()) {
                            Instance newinst = new Instance(subinst.getNodable(),
                                    subinst.getContext(), subinst.getLength(), subinst.getTraceElement());
                            if (ent.getRealInstances().size() == 1) {
                                // keep upper level name
                                newinst.setNameOverride(inst.getName());
                            } else {
                                // use combination of instances to prevent name conflicts
                                newinst.setNameOverride(inst.getName()+"."+subinst.getName());
                            }
                            newInstances.add(newinst);
                        }
                        instances.addAll(i, newInstances);
                        flattened++;
                        break;          // have to break and start over because we modified list
                    }
                }
            }
            alreadyFlattened.put(this, getCell());

            return flattened;
        }
        // returns the scan chain element if it is combinable, otherwise returns null
        private int combineInstances() {
            List<ArrayedNameList> arrayedNames = new ArrayList<ArrayedNameList>();
            int combosDone = 0;
            int maxDimension = 0;
            // build list of arrayed names.  Cannot combine unless it is a scanChainElement
            for (Instance inst : instances) {
                if (inst.getTraceElement() instanceof ScanChainElement) {
                    ArrayedNameList name = new ArrayedNameList(inst.getName());
                    arrayedNames.add(name);
                    int m = name.numDimensions();
                    if (m > maxDimension) maxDimension = m;
                } else {
                    arrayedNames.add(null);
                }
            }
            // start from lowest dimension
            for (int m=0; m<maxDimension; m++) {
                // check if any names are combinable, and if their instances are combinable
                ArrayedNameList last = null;
                for (int i=0; i<arrayedNames.size(); i++) {
                    ArrayedNameList next = arrayedNames.get(i);
                    if (last == null || next == null) {
                        last = next;
                        continue;
                    }
                    // see if combinable both by name, and by scan chain element type
                    Instance lastinst = instances.get(i-1);
                    Instance nextinst = instances.get(i);
                    ScanChainElement lastE = (ScanChainElement)lastinst.getTraceElement();
                    ScanChainElement nextE = (ScanChainElement)nextinst.getTraceElement();
                    ArrayedNameList combined = last.combineSequentialNumeric(next, m);
                    if (lastE.combinable(nextE) && combined != null) {
                        // can combine them
                        Instance newInst = new Instance(lastinst.getNodable(), lastinst.getContext(),
                                lastinst.getLength()+nextinst.getLength(), lastinst.getTraceElement());
                        newInst.setNameOverride(combined.toString());
                        instances.remove(i-1);      // remove last
                        instances.remove(i-1);      // remove next
                        arrayedNames.remove(i-1);   // keep name indices aligned with instances
                        arrayedNames.remove(i-1);
                        instances.add(i-1, newInst);   // replace both by newInst
                        arrayedNames.add(i-1, combined);
                        next = combined;
                        i--;                        // adjust index
                        combosDone++;
                    }
                    last = next;
                }
            }
            return combosDone;
        }
        protected List<Instance> getInstances() { return instances; }
        protected List<Instance> getRealInstances() {
            List<Instance> realinstances = new ArrayList<Instance>();
            for (Instance inst : instances) {
                if (inst.getTraceElement() instanceof ScanChainElement) {
                    ScanChainElement ele = (ScanChainElement)inst.getTraceElement();
                    if (ele.isPassThrough()) continue;
                }
                realinstances.add(inst);
            }
            return realinstances;
        }
        protected List<Instance> getFlatInstances() {
            List<Instance> flatinstances = new ArrayList<Instance>();
            for (Instance inst : instances) {
                TraceElement e = inst.e;
                if (e.isPassThrough()) continue;
                if (e instanceof Entity) {
                    Entity ent = (Entity)e;
                    flatinstances.addAll(ent.getFlatInstances());
                }
                else {
                    flatinstances.add(inst);
                }
            }
            return flatinstances;
        }
        public List<VarContext> getContextsOfJtagInstances() {
            List<VarContext> contexts = new ArrayList<VarContext>();
            getContextsOfJtagInstances(contexts, VarContext.globalContext);
            return contexts;
        }
        private void getContextsOfJtagInstances(List<VarContext> contexts, VarContext parentContext) {
            for (Instance inst : instances) {
                if (inst.e instanceof Entity) {
                    Entity ent = (Entity)inst.e;
                    ent.getContextsOfJtagInstances(contexts, parentContext.push(inst.getNodable()));
                    continue;
                }
                if (inst.e instanceof JtagController) {
                    contexts.add(parentContext.push(inst.getNodable()));
                }
            }
        }
        // may return null if no valid elements in instances (this ignores passthroughs
        public VarContext getContextOfFirstElement(VarContext parentContext, HashMap<Cell,JtagController> jtagControllers) {
            for (Instance inst : instances) {
                if (inst.e.isPassThrough()) continue;
                if (inst.e instanceof Entity) {
                    Entity ent = (Entity)inst.e;
                    if (jtagControllers.get(ent.getCell()) != null) {
                        // this is a jtag controller instance
                        return parentContext.push(inst.getNodable());
                    }
                    VarContext context = ent.getContextOfFirstElement(parentContext.push(inst.getNodable()), jtagControllers);
                    if (context == null) continue;
                    return context;
                }
                if (inst.e instanceof ScanChainElement || inst.e instanceof JtagController) {
                    VarContext context = parentContext.push(inst.getNodable());
                    return context;
                }
            }
            return null;
        }

        public void copyContentsOf(Entity ent) {
            instances.clear();
            instances.addAll(ent.getInstances());
        }
        public Set<Entity> getEntitiesUsed() {
            Set<Entity> entities = new TreeSet<Entity>();
            for (Instance inst : instances) {
                TraceElement ele = inst.getTraceElement();
                if (ele instanceof Entity) {
                    Entity ent = (Entity)ele;
                    entities.add(ent);
                    entities.addAll(ent.getEntitiesUsed());
                }
            }
            return entities;
        }
        public String toString() { return getKey(); }
    }

    private static class Starter extends Entity {
        private final String chain;
        private final int opcode;
        private String chipName;
        private Starter child;
        private Starter(Cell cell, String outport, List<Instance> instances, String chain, int opcode,
                        Starter child, String hierInstanceName) {
            super(cell, null, outport, false, instances, hierInstanceName);
            this.chain = chain;
            this.opcode = opcode;
            this.chipName = "Temp_";
            this.child = child;
        }
        public void write(PrintWriter out, StringBuffer indent) {
            out.println(indent+"<chain name=\""+chain+"\" opcode=\""+Integer.toBinaryString(opcode)+"\">");
            indent.append("\t");
            writeContents(out, indent);
            indent.setLength(indent.length() - 1);
            out.println(indent+"</chain>");
        }
        public void writeData(PrintWriter out, StringBuffer indent, VarContext context, HashMap<String,String> dataNetMap) {
            out.println(indent+"<chain name=\""+chain+"\" opcode=\""+Integer.toBinaryString(opcode)+"\">");
            indent.append("\t");
            writeDataNets(out, indent, context, dataNetMap);
            indent.setLength(indent.length() - 1);
            out.println(indent+"</chain>");
        }
        public String getKey() {
            return getKey(getCell(), getOrigStarterInstanceName()+"_"+chain, getInstanceName());
        }
        public void setChipName(String chipName) {
            this.chipName = chipName;
            if (child != null) child.setChipName(chipName);
        }
        public String getChipName() {
            return chipName;
        }
        public String getChain() {
            String chipName = getChipName();
            return chipName+"_"+chain;
            //return getOrigStarterInstanceName()+"_"+chain;
        }
        public String getOrigStarterInstanceName() {
            if (child == null) return getInstanceName();
            String name = child.getOrigStarterInstanceName();
            if (name == null) name = getInstanceName();
            return name;
        }
    }

    public static class StarterByOpcodeCompare implements Comparator<Starter> {

        public int compare(Starter s1, Starter s2) {
            int compareChipName = s1.getChipName().compareTo(s2.getChipName());
            if (compareChipName == 0) {
                if (s1.opcode == s2.opcode) return 0;
                if (s1.opcode < s2.opcode) return -1;
                return 1;
            }
            return compareChipName;
        }
    }

    /**
     * An instance of a Trace Element: either a real scan chain element,
     * or an Entity, which is just a cell contain scan chain elements.
     */
    private static class Instance {
        private final Nodable no;
        private final VarContext context;
        private final int length;
        private final TraceElement e;
        private Instance prev;              // for building linked list of instances in order
        private Instance next;
        private Network innet;
        private Network outnet;
        private String nameOverride;
        private Instance(Nodable no, VarContext context, int length, TraceElement e) {
            this.no = no;
            this.context = context;
            this.length = length;
            this.e = e;
            this.prev = null;
            this.next = null;
            this.innet = null;
            this.outnet = null;
            this.nameOverride = null;
        }
        public String describeOutput() {
            return no.getParent().getName()+"."+no.getName()+"."+e.outport;
        }
        public String describeInput() {
            return no.getParent().getName()+"."+no.getName()+"."+e.inport;
        }
        public String describeOutnet() {
            return no.getName()+"["+e.outport+"("+outnet+")]";
        }
        public String describeInnet() {
            return no.getName()+"["+e.inport+"("+innet+")]";
        }
        public void write(PrintWriter out, StringBuffer indent, List<String> usedNames) {
            String name = getName();
            if (usedNames.contains(name) && e.inport != null)
                name = name +"_" + e.inport;
            if (e instanceof Entity) {
                // just reference entity
                Entity ent = (Entity)e;
                if (ent.getLength() == 0) return;
                out.println(indent+"<subchain name=\""+name+"\"> &"+e.getKey()+"; </subchain>");
                usedNames.add(name);
            } else {
                // scan chain element
                ScanChainElement ele = (ScanChainElement)e;
                if (ele.isPassThrough()) return;
                out.print(indent+"<subchain name=\""+name+"\"");
                out.print(" length=\""+length+"\"");
                if (ele.access != null) out.print(" access=\""+ele.access+"\"");
                if (ele.clears != null) out.print(" clears=\""+ele.clears+"\"");
                out.println(" />");
            }
        }
        public void writeDataNets(PrintWriter out, StringBuffer indent, VarContext context, HashMap<String,String> dataNetMap) {
            if (e instanceof Entity) {
                // just reference entity
                Entity ent = (Entity)e;
                ent.writeDataNets(out, indent, context.push(no), dataNetMap);
            } else {
                // scan chain element
                ScanChainElement ele = (ScanChainElement)e;
                if (ele.isPassThrough()) return;
                for (int i=0; i<no.getNameKey().busWidth(); i++) {
                    Nodable thisno = no;
                    if (no instanceof NodeInst) {
                        thisno = Netlist.getNodableFor((NodeInst)no, i);
                    }

                    VarContext subContext = context.push(thisno);
                    out.print(indent+"<datanet name=\""+subContext.getInstPath(".")+"\"");


                    String flatName = getFlatDataNet(ele.getDataNet(), subContext, dataNetMap);
                    String flatName2 = getFlatDataNet(ele.getDataNet2(), subContext, dataNetMap);

                    if (flatName != null) out.print(" net=\""+flatName+"\"");
                    if (flatName2 != null) out.print(" net2=\""+flatName2+"\"");
                    out.println(" />");
                }
            }
        }
        private String getFlatDataNet(DataNet dataNet, VarContext context, HashMap<String,String> dataNetMap) {
            if (dataNet == null) return null;           // null data net, no error
            String contextName = ScanChainElement.getDataNetPath(dataNet, context);
            if (contextName == null) return null;
            String flatName = dataNetMap.get(contextName);
            if (flatName == null) {
                //System.out.println("Can't find mapping for dataNet "+contextName);
                //flatName = "*"+contextName;
                return null;
            }
            return flatName+dataNet.getOptions();
        }

        public int getLength() {
            if (e instanceof ScanChainElement) {
                ScanChainElement ele = (ScanChainElement)e;
                if (ele.isPassThrough()) return 0;
                return length;
            }
            else {
                // just reference entity
                Entity ent = (Entity)e;
                return ent.getLength();
            }
        }

        public void setNameOverride(String name) { this.nameOverride = name; }
        public String getName() { return ( (nameOverride == null) ? no.getName() : nameOverride); }
        protected TraceElement getTraceElement() { return e; }
        protected Nodable getNodable() { return no; }
        protected VarContext getContext() { return context; }
    }

    /** Defines the Jtag controller from which the scan chains start and end */
    private static class JtagController extends TraceElement {
        public final int lengthIR;
        private JtagController(Cell cell, String inport, String outport, boolean passThrough, int lengthIR) {
            super(cell, inport, outport, passThrough, null);
            this.lengthIR = lengthIR;
        }
        public int getLengthIR() { return lengthIR; }
    }

    private static class JtagInstance {
        private final JtagController controller;
        private String chipName;
        private VarContext context;
        private List<Starter> starters;
        private JtagInstance(VarContext context, JtagController controller, String chipName) {
            this.context = context;
            this.controller = controller;
            this.chipName = chipName;
            this.starters = new ArrayList<Starter>();
        }
        public String getHierName() { return context.getInstPath("."); }
        public String getChipName() { return chipName; }
        public VarContext getContext() { return context; }
        public void addStarter(Starter s) {
            starters.add(s);
            s.setChipName(chipName);
        }
        public List<Starter> getStarters() { return starters; }
    }

    private static class Range {
        private int start;
        private int end;
        public Range(int start, int end) {
            this.start = start;
            this.end = end;
        }
        public int getStart() { return start; }
        public int getEnd() { return end; }
        public int getRange() { return Math.abs(end-start); }
        /**
         * Return true if the order is ascending (start < end),
         * or false otherwise.
         * @param includeSingular if range is 0 (start == end),
         * then this method will return the state of this argument
         */
        public boolean isAscending(boolean includeSingular) {
            if (start < end) return true;
            if (start == end) return includeSingular;
            return false;
        }
        /**
         * Return true if the order is descending (end < start),
         * or false otherwise.
         * @param includeSingular if range is 0 (start == end),
         * then this method will return the state of this argument
         */
        public boolean isDescending(boolean includeSingular) {
            if (start > end) return true;
            if (start == end) return includeSingular;
            return false;
        }
        public boolean equals(Object o) {
            Range r = (Range)o;
            if (getStart() == r.getStart() && getEnd() == r.getEnd()) return true;
            return false;
        }
        public String toString() {
            if (start == end) return String.valueOf(start);
            return (start + ":" + end);
        }
        /**
         * Combines the two ranges if possible, otherwise returns null.
         * Ranges must be non-overlapping to be combinable.
         * @param next the next range
         * @return the combined range, or null if not possible
         */
        public Range combine(Range next) {
            // start and end must differ by 1
            int diff = Math.abs(getEnd()-next.getStart());
            if (diff != 1) return null;
            // order (ascending/descending) must be the same
            if (isAscending(true) && next.isAscending(true)) {
                return new Range(getStart(), next.getEnd());
            }
            if (isDescending(true) && next.isDescending(true)) {
                return new Range(getStart(), next.getEnd());
            }
            return null;
        }
    }

    private static class ArrayIndex {
        private List<Object> indices;
        boolean numericOnly;
        /**
         * indices is a string representing some set of indices, of the format:
         * <ul>
         * <li>indices := block{,block}*
         * <li>block := letter | range
         * <li>range := digit | digit : digit
         * </ul>
         * @param indices
         */
        public ArrayIndex(String indices) {
            this.indices = new ArrayList<Object>();
            this.numericOnly = true;

            // split by commas
            String [] parts = indices.split(",");
            for (int i=0; i<parts.length; i++)
                addBlock(parts[i]);
        }
        public ArrayIndex(List<Object> indices) {
            numericOnly = true;
            this.indices = indices;
            for (int i=0; i<indices.size(); i++) {
                if (!(indices.get(i) instanceof Range))
                    numericOnly = false;
            }
        }
        private void addBlock(String block) {
            block = block.trim();
            if (block.matches("\\d+")) {
                // a number
                int start = Integer.parseInt(block);
                indices.add(new Range(start, start));
            } else if (block.matches("\\d+:\\d+")) {
                // a range
                String [] range = block.split(":");
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);
                indices.add(new Range(start, end));
            } else if (block.matches("\\w+")) {
                indices.add(block);
            } else {
                System.out.println("Invalid array index "+block);
            }
        }
        public boolean isNumericOnly() { return numericOnly; }
        public boolean equals(Object o) {
            ArrayIndex a = (ArrayIndex)o;
            if (getNumIndices() != a.getNumIndices()) return false;
            for (int i=0; i<indices.size(); i++) {
                Object index = a.indices.get(i);
                if (!indices.get(i).equals(index))      // either string, or a Range object
                    return false;
            }
            return true;
        }
        public int getNumIndices() { return indices.size(); }
        public Object getIndex(int index) {
            if (index <0 || index > (indices.size()-1)) return null;
            return indices.get(index);
        }
        public String toString() {
            StringBuffer str = new StringBuffer();
            for (Iterator it = indices.iterator(); it.hasNext(); ) {
                Object next = it.next();
                if (next instanceof String)
                    str.append((String)next);
                if (next instanceof Range)
                    str.append(((Range)next).toString());
                if (it.hasNext())
                    str.append(",");
            }
            return str.toString();
        }
        /**
         * Tries to combine the two array indices if they are sequential
         * numeric indices
         * @param next
         * @return
         */
        public ArrayIndex combineSequentialNumeric(ArrayIndex next) {
            if (!isNumericOnly()) return null;
            if (!next.isNumericOnly()) return null;
            Object thisLastObj = getIndex(getNumIndices()-1);
            Object otherFirstObj = next.getIndex(0);
            if (!(thisLastObj instanceof Range) || !(otherFirstObj instanceof Range)) return null;
            Range thisLast = (Range)thisLastObj;
            Range otherFirst = (Range)otherFirstObj;
            // both should be ranges
            if (thisLast == null || otherFirst == null) return null;
            Range newRange = thisLast.combine(otherFirst);
            if (newRange == null) return null;
            List<Object> indices = new ArrayList<Object>(this.indices);
            indices.remove(getNumIndices()-1);
            indices.add(newRange);
            return new ArrayIndex(indices);
        }
    }

    private static class ArrayedName {
        private String name;
        private List<ArrayIndex> indices;  // order of most sig to least sig, unrolling traverses least sig first
        public ArrayedName(String arrayedName) {
            // break name by brackets
            indices = new ArrayList<ArrayIndex>();
            String [] parts = arrayedName.trim().split("[\\[\\]]");
            for (int i=0; i<parts.length; i++) {
                if (i == 0) {
                    // this is the name
                    if (parts[0].equals("")) System.out.println("Invalid name for "+arrayedName);
                    name = parts[0];
                    continue;
                }

                String str = parts[i].trim();
                if (str.equals("")) continue;
                ArrayIndex index = new ArrayIndex(parts[i]);
                indices.add(index);
            }
            // reverse order so that unrolling starts from first indices
            Collections.reverse(indices);
        }
        public ArrayedName(String name, List<ArrayIndex> indices) {
            this.name = name;
            this.indices = indices;
        }
        public String getName() { return name; }
        public int numDimensions() { return indices.size(); }
        public ArrayIndex getIndex(int i) {
            if (i < 0 || i > (indices.size()-1)) {
                return null;
            }
            return indices.get(i);
        }
        /**
         * Tries to combine this name with another name if they are in numeric sequence.
         * This means the ArrayIndex at the specified degree must be completely numeric.
         * If successful, returns the new combined name.  Otherwise, returns null.
         * @param dimension which dimension of a multi-dimensional array to use, starting from 0
         * at the least signficant: name[1:4][1:3], 1:3 is dimension 0, 1:4 is dimension 1
         * @return the new combined name, or null if none possible
         */
        public ArrayedName combineSequentialNumeric(ArrayedName next, int dimension) {
            // check dimension in question, make sure it exists
            ArrayIndex index = getIndex(dimension);
            if (index == null) return null;
            if (!index.isNumericOnly()) return null;

            // make sure names are the same
            if (!name.equals(next.getName())) return null;
            // make sure same dimensions
            if (numDimensions() != next.numDimensions()) return null;

            // make sure all dimensions besides the one in question are the same
            for (int i=0; i<numDimensions(); i++) {
                if (i == dimension) continue;
                ArrayIndex curi = getIndex(i);
                ArrayIndex nexti = next.getIndex(i);
                if (!curi.equals(nexti)) {
                    return null;
                }
            }

            // see if dimension in question is sequential between the two
            ArrayIndex nextIndex = next.getIndex(dimension);
            ArrayIndex combined = index.combineSequentialNumeric(nextIndex);
            if (combined == null) return null;

            // sequential, create a new ArrayedName
            ArrayList<ArrayIndex> newIndices = new ArrayList<ArrayIndex>();
            for (int i=0; i<numDimensions(); i++) {
                if (i == dimension) {
                    newIndices.add(combined);
                    continue;
                }
                newIndices.add(getIndex(i));
            }
            return new ArrayedName(getName(), newIndices);
        }
        public String toString() {
            StringBuffer str = new StringBuffer(name);
            // have to go backwards
            for (int i=indices.size()-1; i>=0; i--) {
                ArrayIndex ind = getIndex(i);
                str.append("[");
                str.append(ind.toString());
                str.append("]");
            }
            return str.toString();
        }

        /** Unit Test */
        public static void main(String [] args) {
            ArrayedName n1 = new ArrayedName("foo[1]");
            ArrayedName n2 = new ArrayedName("foo[2]");
            printTest(n1, n2, 0);
            printTest(n1, n2, 1);

            n1 = new ArrayedName("foo[1:2][5:4][1:2]");
            n2 = new ArrayedName("foo[1:2][3:1][1:2]");
            printTest(n1, n2, 1);
            printTest(n1, n2, 2);
        }
        public static void printTest(ArrayedName n1, ArrayedName n2, int dim) {
            System.out.println("Combining dimension "+dim+" of: "+n1+" and "+ n2+" --> "+n1.combineSequentialNumeric(n2, dim));
        }
    }

    // Parses a string of the form: ArrayedName,ArrayedName,....ArrayedName.
    private static class ArrayedNameList {
        private List<ArrayedName> arrayedNames;
        public ArrayedNameList(String name) {
            arrayedNames = new ArrayList<ArrayedName>();
            List<String> names = new ArrayList<String>();
            int openBrackets = 0;
            int last = 0;
            for (int i=0; i<name.length(); i++) {
                char c = name.charAt(i);
                if (c == '[') openBrackets++;
                if (c == ']') openBrackets--;
                if (c == ',' && openBrackets == 0) {
                    int start = last;
                    if (start >= 0 && i > start) {
                        names.add(name.substring(start, i));
                    }
                    last = i+1;     // skip ,
                }
            }
            if (openBrackets == 0 && last < name.length()) {
                names.add(name.substring(last, name.length()));
            }

            for (String s : names) {
                arrayedNames.add(new ArrayedName(s));
            }
        }
        private ArrayedNameList(List<ArrayedName> arrayedNames) {
            this.arrayedNames = arrayedNames;
        }
        public int getNumNames() { return arrayedNames.size(); }
        public ArrayedName get(int index) {
            if (index < 0 || index > (getNumNames()-1)) return null;
            return arrayedNames.get(index);
        }
        public int numDimensions() {
            int maxDimension = 0;
            for (int i=0; i<getNumNames(); i++) {
                int m = get(i).numDimensions();
                if (m > maxDimension) maxDimension = m;
            }
            return maxDimension;
        }

        public ArrayedNameList combineSequentialNumeric(ArrayedNameList next, int dimension) {
            ArrayedName thisLast = get(getNumNames()-1);
            ArrayedName nextFirst = next.get(0);
            if (thisLast != null && nextFirst != null) {
                // try to combine them
                ArrayedName combined = thisLast.combineSequentialNumeric(nextFirst, dimension);
                if (combined != null) {
                    // build new list with combined name
                    List<ArrayedName> newList = new ArrayList<ArrayedName>();
                    for (int i=0; i<getNumNames()-1; i++)
                        newList.add(get(i));        // skip last
                    newList.add(combined);          // add combined
                    for (int i=1; i<next.getNumNames(); i++) {
                        newList.add(next.get(i));   // skip first
                    }
                    return new ArrayedNameList(newList);
                }
            }
            return null;
        }
        public String toString() {
            StringBuffer buf = new StringBuffer();
            for (int i=0; i<getNumNames(); i++) {
                ArrayedName name = get(i);
                buf.append(name.toString());
                if (i < (getNumNames()-1)) // more to come
                    buf.append(',');
            }
            return buf.toString();
        }

        /** Unit Test */
        public static void main(String [] args) {
            ArrayedNameList n1 = new ArrayedNameList("foo[1],foo[2]");
            ArrayedNameList n2 = new ArrayedNameList("foo[3],foo[4]");
            printTest(n1, n2, 0);
            printTest(n1, n2, 1);

            n1 = new ArrayedNameList("foo[1,a],bar[2:4]");
            n2 = new ArrayedNameList("bar[5],xxx[2]");
            printTest(n1, n2, 0);
            printTest(n1, n2, 1);

            n1 = new ArrayedNameList("foo[1,a],bar[3:5][2:4]");
            n2 = new ArrayedNameList("bar[6][2:4],xxx[2]");
            printTest(n1, n2, 0);
            printTest(n1, n2, 1);
        }
        public static void printTest(ArrayedNameList n1, ArrayedNameList n2, int dim) {
            System.out.println("Combining dimension "+dim+" of: "+n1+" and "+ n2+" --> "+n1.combineSequentialNumeric(n2, dim));
        }

    }

    // ==========================================================================

    /**
     * Optimize the specification of the entities and scan chain
     */
    private void optimize(List<Starter> chains) {
        int done = 1;
        //while (done != 0) {
            done = 0;
            done += flatten(chains);
            done += combineInstances();
        //}
    }

    /**
     * Flatten out specified entities, and flatten out entities that
     * only contain one subchain.  This does one pass through the entities,
     * several passes are required to optimize everything.
     */
    private int flatten(List<Starter> chains) {
        int done = 0;
        HashMap<Entity,Cell> alreadyFlattened = new HashMap<Entity,Cell>();
        for (Starter chain : chains) {
            done += chain.flattenInstances(cellsToFlatten, alreadyFlattened);
        }
        return done;
    }
    private int combineInstances() {
        int done = 0;
        for (Set<TraceElement> list : elements.values()) {
            for (TraceElement e : list) {
                if (e instanceof Entity) {
                    Entity ent = (Entity)e;
                    // check if we can flatten any instances
                    done += ent.combineInstances();
                }
            }
        }
        return done;
    }

    // ==========================================================================

    /**
     * A Port holds information about a single port, that may be part of a bussed portinst
     */
    private static class Port {
        private PortProto pp;
        private int index;
        private Name name;
        private Nodable no;

        private Port(Name name, Nodable no, PortProto pp, int index) {
            this.name = name;
            this.no = no;
            this.pp = pp;
            this.index = index;
        }
        public String toString() { if (name == null) return null; return no.getName() +":"+name.toString(); }
        public void print() {
            System.out.println("  Name: "+name);
            System.out.println("  No: "+no);
            System.out.println("  int: "+index);
            System.out.println("  pp: "+pp);
        }
    }

    private static class ExPort {
        private Export ex;
        private int index;
        private Name name;

        private ExPort(Name name, Export ex, int index) {
            this.name = name;
            this.ex = ex;
            this.index = index;
        }
        public String toString() { if (name == null) return null; return name.toString(); }
        public void print() {
            System.out.println("  Name: "+name);
            System.out.println("  Ex: "+ex);
            System.out.println("  int: "+index);
        }
    }

//    private ExPort getExportedPort(Port port) {
//        if (port == null) return null;
//
//        Cell cell = port.no.getParent();
//
//        // list of all portinsts on net
//        Netlist netlist = cell.getNetlist(true);
//        Network net = netlist.getNetwork(port.no, port.pp, port.index);
//
//        for (Iterator<PortProto> it = cell.getPorts(); it.hasNext(); ) {
//            Export ex = (Export)it.next();
//            Name name = ex.getNameKey();
//            for (int i=0; i<name.busWidth(); i++) {
//                if (netlist.getNetwork(ex, i) == net)
//                    return new ExPort(name.subname(i), ex, i);
//            }
//        }
//        return null;
//    }

    /**
     * Get the network attached to port 'portName' on nodable 'no'.
     * @param no
     * @param portName
     * @return
     */
    private static Network getNetwork(Nodable no, Netlist netlist, String portName) {
        for (Iterator<PortProto> it = no.getProto().getPorts(); it.hasNext(); ) {
            PortProto pp = it.next();
            Name name = pp.getNameKey();
            for (int i=0; i<name.busWidth(); i++) {
                Name subname = name.subname(i);
                if (subname.toString().equals(portName)) {
                    return netlist.getNetwork(no, pp, i);
                }
            }
        }
        System.out.println("Error: Could not find port "+portName+" on "+no.getProto().describe(false)+"("+no.getName()+") in cell "+no.getParent().describe(false));
        return null;
    }

    private static Network getNetwork(Netlist netlist, String name) {
        for (Iterator<Network> it = netlist.getNetworks(); it.hasNext(); ) {
            Network net = it.next();
            if (net.hasName(name)) return net;
        }
        return null;
    }

    /**
     * Get a port from a port name on a Nodable
     * @param no the nodable
     * @param portName the port name including bus index, such as foo[1][2]
     * @return a Port, or null if none found
     */
    private static Port getPort(Nodable no, String portName) {
        if (portName == null) return null;
        for (Iterator<PortProto> it = no.getProto().getPorts(); it.hasNext(); ) {
            PortProto pp = it.next();
            Name name = pp.getNameKey();
            for (int i=0; i<name.busWidth(); i++) {
                Name subname = name.subname(i);
                if (subname.toString().equals(portName)) {
                    return new Port(subname, no, pp, i);
                }
            }
        }
        System.out.println("Could not find "+portName+" on "+no.getName());
        return null;
    }

    /**
     * Get an export in a cell from a exportName. The export
     * name may include a bus index, such as foo[1][2]
     * @param cell the cell
     * @param exportName the export name
     * @return an ExPort
     */
    private static ExPort getExPort(Cell cell, String exportName) {
        for (Iterator<PortProto> it = cell.getPorts(); it.hasNext(); ) {
            Export ex = (Export)it.next();
            Name name = ex.getNameKey();
            for (int i=0; i<name.busWidth(); i++) {
                Name subname = name.subname(i);
                if (subname.toString().equals(exportName)) {
                    return new ExPort(subname, ex, i);
                }
            }
        }
        return null;
    }


}
