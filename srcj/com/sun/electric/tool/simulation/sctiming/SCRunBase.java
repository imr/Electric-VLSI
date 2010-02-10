/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SCRunBase.java
 * Written by Jonathan Gainsley, Sun Microsystems.
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation.sctiming;

import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class SCRunBase {

    /**
     * Gates with pre-configured timing arcs
     */
    public enum GateType {
        /** Inverter */                         INV,
        /** High threshold inverter */          INVHT,
        /** Low threshold inverter */           INVLT,
        /** Clock threshold inverter */         INVCLK,
        /** 2 input NAND */                     NAND2EN,
        /** 2 input NAND */                     NAND2CLKEN,
        /** 2 input NAND */                     NAND2,
        /** 3 input NAND */                     NAND3,
        /** 2 input NOR */                      NOR2,
        /** 2 input XOR */                      XOR2
        /** DDR flop */                         //DDR, FLOPDDR,
        /** SDR flop */                         //SDR, FLOPSDR
    }

    /**
     * Delay type, Max or Min path timing
     */
    public enum DelayType {
        /** Max delay */                        MAX,
        /** Min delay */                        MIN,
    }

    /**
     * Characterize all pre-configured cells in the current Electric library.
     * @param settings the settings to use
     * @return true on success, false on failure.
     */
    public boolean characterizeCells(SCSettings settings, DelayType delayType) {
        Library lib = Library.getCurrent();
        if (lib == null) {
            System.out.println("No current library to characterize");
            return false;
        }

        // set up directories
        File libraryDir = null;
        if (lib.getLibFile() != null)
            libraryDir = TextUtils.getFile(lib.getLibFile()).getParentFile();
        else
            libraryDir = new File(User.getWorkingDirectory());
        File outputDir = new File(libraryDir, lib.getName()+"_chardata"+delayType.toString());
        if (!outputDir.exists()) {
            if (!outputDir.mkdir()) {
                System.out.println("Cannot make char data directory "+outputDir.getPath());
                return false;
            }
        }
        System.out.println("Set output directory to "+outputDir.getPath());

        settings.setLibrary(lib.getName());
        LibData.Group library = settings.getLibrary();

        // see if lib file already exists
        File libertyFile = new File(libraryDir, lib.getName()+"."+delayType.toString()+".lib");
        if (libertyFile.exists()) {
            // read it
            LibData libdata = parseExisting(libertyFile);
            if (libdata != null) {
                System.out.println("Found existing liberty file "+libertyFile.getPath());
                System.out.println("  Using settings in existing liberty file");
                System.out.println("  Characterizing new cells only");
                settings.getSettingsFromLibData(libdata);
                library = libdata.getLibrary();
            } else {
                System.out.println("Error reading existing liberty file");
                return false;
            }
        }

        // See if extracted netlist exists
        Map<String,String> extractedCells = new HashMap<String,String>();
        File extractedNetlist = new File(User.getWorkingDirectory(), lib.getName()+".sp");
        if (extractedNetlist.exists()) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(extractedNetlist));
                String line = null;
                while ( (line = in.readLine()) != null) {
                    if (line.startsWith(".SUBCKT")) {
                        String [] parts = line.split("\\s+");
                        if (parts.length >= 2) {
                            extractedCells.put(parts[1], parts[1]);
                        }
                    }
                }
                in.close();
            } catch (IOException ee) {
                System.out.println(ee.getMessage());
            }
        }


        boolean warned = false;
        // Characterize all cells in library for which we have characterization settings
        List<String> messages = new ArrayList<String>();
        for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
            Cell c = it.next();
            if (c.getView() == View.LAYOUT) {
                if (c != c.getNewestVersion()) continue;

                String verilogCellName = Spice.getSafeNetName(c.getName(), Simulation.SpiceEngine.SPICE_ENGINE_H);

                // if data already in liberty file, ignore
                List<LibData.Group> existing = library.getGroups("cell", verilogCellName);
                if (existing.size() > 0) {
                    messages.add("Info: Skipped cell "+c.getName()+", already exists in liberty file data");
                    continue;
                }

                SCTiming timing = getSCTimingSetup(c, settings);
                String cname = c.getName();
                if (timing == null) {
                    messages.add("Warning: Skipping "+cname+": Unable to find timing arcs");
                    continue;
                }

                File cellDir = new File(outputDir, c.getName());
                if (!cellDir.exists()) {
                    if (!cellDir.mkdir()) {
                        messages.add("Error: Cannot make cell char data directory "+cellDir.getPath()+"; skipping cell "+cname);
                        continue;
                    }
                }

                // Use either star rcxt extracted netlist or write netlist from Electric layout
                File outputFile = new File(cellDir, c.getName()+".sp");

                String cellName = Spice.getSafeNetName(c.getName(), Simulation.SpiceEngine.SPICE_ENGINE_H);
                if (extractedCells.containsKey(cellName)) {
                    try {
                        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
                        writer.println("* Characterization file for Cell "+cellName);
                        writer.println("*");
                        writer.println(".include '"+extractedNetlist.getAbsolutePath()+"'");
                        writer.println(".include '"+settings.commonHeaderFile+"'");
                        writer.println();
                        writer.close();
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                    timing.setTopCellName(cellName);
                } else {
                    // set correct settings for spice netlisting
                    Spice.SpicePreferences sp = new Spice.SpicePreferences(true, false);
                    sp.writeSubcktTopCell = true;
                    sp.writeTopCellInstance = false;
                    sp.writeTransSizeInLambda = true;
                    sp.writeEmptySubckts = false;
                    sp.cdlIgnoreResistors = false;
                    sp.useCellParameters = true;
                    sp.parasiticsLevel = Simulation.SpiceParasitics.RC_CONSERVATIVE;
                    sp.writeFinalDotEnd = false;
                    sp.parasiticsUseExemptedNetsFile = false;
                    sp.parasiticsExtractsR = true;
                    sp.parasiticsExtractsC = true;
                    if (settings.commonHeaderFile.length() > 0)
                        sp.headerCardInfo = settings.commonHeaderFile;

                    // write spice netlist
                    sp.doOutput(c, VarContext.globalContext, outputFile.getPath());

                    timing.setTopCellName(c.getName());
                }

                timing.setOutputDir(cellDir.getPath());
                timing.setTopCell(c);
                timing.setTopCellNameLiberty(verilogCellName);
                timing.setInputFile(outputFile.getPath());

                try {
                    timing.characterize_(delayType);
                } catch (SCTimingException e) {
                    messages.add("Error: Failed to characterize cell "+c.describe(false)+": "+e.getMessage());
                    continue;
                }

                timing.getCellLibData(library);
                // set area of cell
                List<LibData.Group> list = library.getGroups("cell", verilogCellName);
                ERectangle rect = c.getBounds();
                double scale = c.getTechnology().getScale() / 1000; // lambda to microns
                double area = rect.getWidth() * rect.getHeight() * scale * scale;
                for (LibData.Group g : list) {
                    g.putAttribute("area", area);
                }
            }
        }

        System.out.println("\nCharacterization Complete\n");
        for (String message : messages) {
            System.out.println(message);
        }

        if (libertyFile.exists()) {
            File temp = new File(libertyFile.getPath());
            File backup = new File(libertyFile.getParent(), libertyFile.getName()+".old");
            System.out.println("Renaming existing liberty file to "+backup.getName());
            if (!temp.renameTo(backup)) {
                System.out.println("Warning, unable to rename "+libertyFile.getName()+" to "+backup.getName());
            }
        }

        LibData libData = new LibData();
        libData.setLibrary(library);
        libData.write(libertyFile.getPath());
        System.out.println("Wrote liberty file to "+libertyFile.getPath());
        return true;
    }

    protected LibData parseExisting(File libertyFile) {
        return null;
    }
    
    /**
     * Get an SCTiming object with Timing Arcs for the given cell
     * @param cell the cell
     * @param settings global settings
     * @return SCTiming object for characterization
     */
    public static SCTiming getSCTimingSetup(Cell cell, SCSettings settings) {
        SCTiming timing = null;
        for (Iterator<Cell> itc = cell.getCellGroup().getCells(); itc.hasNext(); ) {
            Cell acell = itc.next();
            if (acell.getView() == View.DOC && acell == acell.getNewestVersion()) {
                timing = getSetupFromScript(acell.getTextViewContents(), settings);
                break;
            }
        }
        if (timing == null) {
            String cname = cell.getName().toLowerCase();
            for (GateType type : GateType.values()) {
                if (cname.matches(type.toString().toLowerCase()+"_x[0-9.]+")) {
                    String size = cname.substring(cname.lastIndexOf("x")+1, cname.length());
                    double xsize = Double.parseDouble(size);
                    timing = getSCTimingSetup(type, settings, xsize);
                    break;
                }
            }
        }
        return timing;
    }

    public static SCTiming getSetupFromScript(String [] script, SCSettings settings) {
        EvalJavaBsh bsh = new EvalJavaBsh();
        bsh.doEvalLine("import com.sun.electric.plugins.sctiming.*;");
        SCTiming timing = new SCTiming();
        timing.setSettings(settings);

        bsh.setVariable("timing", timing);
        bsh.setVariable("settings", settings);
        boolean run = false;
        boolean scriptFound = false;
        for (String line : script) {
            if (line.toLowerCase().startsWith("--start characterization script")) {
                run = true;
                scriptFound = true;
                continue;
            }
            if (line.toLowerCase().startsWith("--end characterization script")) {
                run = false;
                continue;
            }
            if (run)
                bsh.doEvalLine(line);
        }
        Object t = bsh.getVariable("timing");
        if (scriptFound && t instanceof SCTiming)
            return (SCTiming)t;
        return null;
    }

    /**
     * Get an SCTiming object with Timing Arcs for the given gate type
     * @param type recognized gate type
     * @param settings global settings
     * @return SCTiming object for characterization
     */
    public static SCTiming getSCTimingSetup(GateType type, SCSettings settings, double xsize) {
        switch (type) {
            case INV: return getSetupInv(settings, xsize);
            case INVHT: return getSetupInv(settings, xsize);
            case INVLT: return getSetupInv(settings, xsize);
            case INVCLK: return getSetupInv(settings, xsize);
            case NAND2: return getSetupNand2(settings, xsize);
            case NAND2EN: return getSetupNand2en(settings, xsize);
            case NAND2CLKEN: return getSetupNand2en(settings, xsize);
            case NAND3: return getSetupNand3(settings, xsize);
            case NOR2: return getSetupNor2(settings, xsize);
            //case XOR2: return getSetupXor2(settings, xsize);
            //case DDR: return getSetupFlopDDR(settings);
            //case FLOPDDR: return getSetupFlopDDR(settings);
            //case SDR: return getSetupFlopSDR(settings);
            //case FLOPSDR: return getSetupFlopSDR(settings);
        }
        return null;
    }

    /**
     * Get an SCTiming object with Timing Arcs for an Inverter type gate
     * @param settings global settings
     * @return SCTiming object for characterization
     */
    public static SCTiming getSetupInv(SCSettings settings, double xsize) {
        SCTiming timing = new SCTiming();
        timing.setSettings(settings);
        timing.setFunctionCombinational("out", "!in");

        String outputLoads = getLoadSweep(xsize);
        String buffers = getInputBufferSweep(xsize);

        Arc arc = new Arc();
        arc.setInputTransition("in", PinEdge.Transition.RISE);
        arc.setOutputTransition("out", PinEdge.Transition.FALL);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("in", PinEdge.Transition.FALL);
        arc.setOutputTransition("out", PinEdge.Transition.RISE);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        return timing;
    }

    /**
     * Get an SCTiming object with Timing Arcs for a NAND2 type gate
     * @param settings global settings
     * @return SCTiming object for characterization
     */
    public static SCTiming getSetupNand2(SCSettings settings, double xsize) {
        SCTiming timing = new SCTiming();
        timing.setSettings(settings);
        timing.setFunctionCombinational("out", "!(ina * inb)");

        String outputLoads = getLoadSweep(xsize);
        String buffers = getInputBufferSweep(xsize);

        Arc arc = new Arc();
        arc.setInputTransition("ina", PinEdge.Transition.RISE);
        arc.setOutputTransition("out", PinEdge.Transition.FALL);
        arc.addStableInput("inb", PinEdge.Transition.STABLE1);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("ina", PinEdge.Transition.FALL);
        arc.setOutputTransition("out", PinEdge.Transition.RISE);
        arc.addStableInput("inb", PinEdge.Transition.STABLE1);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("inb", PinEdge.Transition.RISE);
        arc.setOutputTransition("out", PinEdge.Transition.FALL);
        arc.addStableInput("ina", PinEdge.Transition.STABLE1);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("inb", PinEdge.Transition.FALL);
        arc.setOutputTransition("out", PinEdge.Transition.RISE);
        arc.addStableInput("ina", PinEdge.Transition.STABLE1);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        return timing;
    }

    /**
     * Get an SCTiming object with Timing Arcs for a NAND2EN type gate
     * NOTE: this assumes inb is the enable input
     * @param settings global settings
     * @return SCTiming object for characterization
     */
    public static SCTiming getSetupNand2en(SCSettings settings, double xsize) {
        SCTiming timing = new SCTiming();
        timing.setSettings(settings);
        timing.setFunctionCombinational("out", "!(ina * inb)");

        String outputLoads = getLoadSweep(xsize);
        String buffers = getInputBufferSweep(xsize);

        Arc arc = new Arc();
        arc.setInputTransition("ina", PinEdge.Transition.RISE);
        arc.setOutputTransition("out", PinEdge.Transition.FALL);
        arc.addStableInput("inb", PinEdge.Transition.STABLE1);
        //arc.setOutputLoadSweep("2 4 8 12 24 26 28 30");
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("ina", PinEdge.Transition.FALL);
        arc.setOutputTransition("out", PinEdge.Transition.RISE);
        arc.addStableInput("inb", PinEdge.Transition.STABLE1);
        //arc.setOutputLoadSweep("2 4 8 12 24 26 28 30");
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("inb", PinEdge.Transition.RISE);
        arc.setOutputTransition("out", PinEdge.Transition.FALL);
        arc.addStableInput("ina", PinEdge.Transition.STABLE1);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("inb", PinEdge.Transition.FALL);
        arc.setOutputTransition("out", PinEdge.Transition.RISE);
        arc.addStableInput("ina", PinEdge.Transition.STABLE1);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        return timing;
    }

    /**
     * Get an SCTiming object with Timing Arcs for a NAND3 type gate
     * @param settings global settings
     * @return SCTiming object for characterization
     */
    public static SCTiming getSetupNand3(SCSettings settings, double xsize) {
        SCTiming timing = new SCTiming();
        timing.setSettings(settings);
        timing.setFunctionCombinational("out", "!(ina * inb * inc)");

        String outputLoads = getLoadSweep(xsize);
        String buffers = getInputBufferSweep(xsize);

        Arc arc = new Arc();
        arc.setInputTransition("ina", PinEdge.Transition.RISE);
        arc.setOutputTransition("out", PinEdge.Transition.FALL);
        arc.addStableInput("inb", PinEdge.Transition.STABLE1);
        arc.addStableInput("inc", PinEdge.Transition.STABLE1);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("ina", PinEdge.Transition.FALL);
        arc.setOutputTransition("out", PinEdge.Transition.RISE);
        arc.addStableInput("inb", PinEdge.Transition.STABLE1);
        arc.addStableInput("inc", PinEdge.Transition.STABLE1);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("inb", PinEdge.Transition.RISE);
        arc.setOutputTransition("out", PinEdge.Transition.FALL);
        arc.addStableInput("ina", PinEdge.Transition.STABLE1);
        arc.addStableInput("inc", PinEdge.Transition.STABLE1);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("inb", PinEdge.Transition.FALL);
        arc.setOutputTransition("out", PinEdge.Transition.RISE);
        arc.addStableInput("ina", PinEdge.Transition.STABLE1);
        arc.addStableInput("inc", PinEdge.Transition.STABLE1);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("inc", PinEdge.Transition.RISE);
        arc.setOutputTransition("out", PinEdge.Transition.FALL);
        arc.addStableInput("ina", PinEdge.Transition.STABLE1);
        arc.addStableInput("inb", PinEdge.Transition.STABLE1);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("inc", PinEdge.Transition.FALL);
        arc.setOutputTransition("out", PinEdge.Transition.RISE);
        arc.addStableInput("ina", PinEdge.Transition.STABLE1);
        arc.addStableInput("inb", PinEdge.Transition.STABLE1);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        return timing;
    }

    /**
     * Get an SCTiming object with Timing Arcs for a NOR2 type gate
     * @param settings global settings
     * @return SCTiming object for characterization
     */
    public static SCTiming getSetupNor2(SCSettings settings, double xsize) {
        SCTiming timing = new SCTiming();
        timing.setSettings(settings);
        timing.setFunctionCombinational("out", "!(ina + inb)");

        String outputLoads = getLoadSweep(xsize);
        String buffers = getInputBufferSweep(xsize);

        Arc arc = new Arc();
        arc.setInputTransition("ina", PinEdge.Transition.RISE);
        arc.setOutputTransition("out", PinEdge.Transition.FALL);
        arc.addStableInput("inb", PinEdge.Transition.STABLE0);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("ina", PinEdge.Transition.FALL);
        arc.setOutputTransition("out", PinEdge.Transition.RISE);
        arc.addStableInput("inb", PinEdge.Transition.STABLE0);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("inb", PinEdge.Transition.RISE);
        arc.setOutputTransition("out", PinEdge.Transition.FALL);
        arc.addStableInput("ina", PinEdge.Transition.STABLE0);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("inb", PinEdge.Transition.FALL);
        arc.setOutputTransition("out", PinEdge.Transition.RISE);
        arc.addStableInput("ina", PinEdge.Transition.STABLE0);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        return timing;
    }

    /**
     * Get an SCTiming object with Timing Arcs for a XOR2 type gate
     * @param settings global settings
     * @return SCTiming object for characterization
     */
    public static SCTiming getSetupXor2(SCSettings settings, double xsize) {
        SCTiming timing = new SCTiming();
        timing.setSettings(settings);
        timing.setFunctionCombinational("out", "ina ^ inb");

        String outputLoads = getLoadSweep(xsize);
        String buffers = getInputBufferSweep(xsize);

        Arc arc = new Arc();
        arc.setInputTransition("ina", PinEdge.Transition.RISE);
        arc.setOutputTransition("out", PinEdge.Transition.FALL);
        arc.addDependentStableInput("inb", PinEdge.Transition.STABLE1);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("ina", PinEdge.Transition.FALL);
        arc.setOutputTransition("out", PinEdge.Transition.RISE);
        arc.addDependentStableInput("inb", PinEdge.Transition.STABLE1);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("ina", PinEdge.Transition.RISE);
        arc.setOutputTransition("out", PinEdge.Transition.RISE);
        arc.addDependentStableInput("inb", PinEdge.Transition.STABLE0);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("ina", PinEdge.Transition.FALL);
        arc.setOutputTransition("out", PinEdge.Transition.FALL);
        arc.addDependentStableInput("inb", PinEdge.Transition.STABLE0);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("inb", PinEdge.Transition.RISE);
        arc.setOutputTransition("out", PinEdge.Transition.FALL);
        arc.addDependentStableInput("ina", PinEdge.Transition.STABLE1);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("inb", PinEdge.Transition.FALL);
        arc.setOutputTransition("out", PinEdge.Transition.RISE);
        arc.addDependentStableInput("ina", PinEdge.Transition.STABLE1);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("inb", PinEdge.Transition.RISE);
        arc.setOutputTransition("out", PinEdge.Transition.RISE);
        arc.addDependentStableInput("ina", PinEdge.Transition.STABLE0);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("inb", PinEdge.Transition.FALL);
        arc.setOutputTransition("out", PinEdge.Transition.FALL);
        arc.addDependentStableInput("ina", PinEdge.Transition.STABLE0);
        arc.setOutputLoadSweep(outputLoads);
        arc.setInputBufferSweep(buffers);
        timing.addTimingArc(arc);

        return timing;
    }

    // params for load and input buffer sweeps
    private static double lowerBound = 0.00001;
    private static int upperBound = 310;
    private static int numSizes = 8;

    /**
     * Get sweep of loads based on dut xsize
     * @param xsize size of gate to characterize
     * @return list of sizes to use for loads
     */
    public static String getLoadSweep(double xsize) {
        // config, hard coded for now.
        double minStepup = 0.5;
        double maxStepup = 12;

        double minSize = xsize*minStepup;
        double maxSize = xsize*maxStepup;
        double stepX = (maxSize - minSize) / numSizes;
        double su = Math.pow(maxStepup/minStepup, 1.0/(numSizes-1));

        StringBuffer loads = new StringBuffer();
        for (int i=0; i<numSizes; i++) {
            //double size = minSize+(i)*stepX;
            double size = xsize * minStepup * Math.pow(su, i);
            if (size < lowerBound || size > upperBound) continue;
            loads.append(TextUtils.formatDouble(size,2)+" ");
        }
        return loads.toString().trim();
    }

    public static String getInputBufferSweep(double xsize) {
        double minStepup = 1.0/12;
        double maxStepup = 2.0;
        double su = Math.pow(maxStepup/minStepup, 1.0/(numSizes-1));

        StringBuffer loads = new StringBuffer();
        for (int i=numSizes-1; i>=0; i--) {
            double size = xsize * minStepup * Math.pow(su, i);
            size = size/2; // account for second inverter in buffer that is double size
            if (size < lowerBound || size > upperBound) continue;
            loads.append(TextUtils.formatDouble(size,2)+" ");
        }
        return loads.toString().trim();
    }

    /**
     * Get an SCTiming object with Timing Arcs for a DDR Flop type gate
     * @param settings global settings
     * @return SCTiming object for characterization
     */
    public static SCTiming getSetupFlopDDR(SCSettings settings) {
        SCTiming timing = new SCTiming();
        timing.setSettings(settings);
        timing.setFunctionFlipFlop("Q", "QB", "D", "clkt");

        Arc arc = new Arc();
        arc.setInputTransition("D", PinEdge.Transition.RISE);
        arc.setOutputTransition("Q", PinEdge.Transition.RISE);
        arc.setClkTransition("clkt", PinEdge.Transition.RISE);
        arc.setClkFalseTransition("clkf", PinEdge.Transition.FALL);
        arc.addDUTInitialCondition("topnode2", settings.vdd);
        arc.addDUTInitialCondition("topnode1", 0);
        arc.addDUTInitialCondition("botnode2", settings.vdd);
        arc.addDUTInitialCondition("botnode1", 0);
        arc.setInputTransition("clr", PinEdge.Transition.STABLE0);
        arc.setInputTransition("init", PinEdge.Transition.STABLE0);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("D", PinEdge.Transition.FALL);
        arc.setOutputTransition("Q", PinEdge.Transition.FALL);
        arc.setClkTransition("clkt", PinEdge.Transition.RISE);
        arc.setClkFalseTransition("clkf", PinEdge.Transition.FALL);
        arc.addDUTInitialCondition("topnode2", 0);
        arc.addDUTInitialCondition("topnode1", settings.vdd);
        arc.addDUTInitialCondition("botnode2", 0);
        arc.addDUTInitialCondition("botnode1", settings.vdd);
        arc.setInputTransition("clr", PinEdge.Transition.STABLE0);
        arc.setInputTransition("init", PinEdge.Transition.STABLE0);
        timing.addTimingArc(arc);

        return timing;
    }

    /**
     * Get an SCTiming object with Timing Arcs for a SDR Flop type gate
     * @param settings global settings
     * @return SCTiming object for characterization
     */
    public static SCTiming getSetupFlopSDR(SCSettings settings) {
        SCTiming timing = new SCTiming();
        timing.setSettings(settings);
        timing.setFunctionFlipFlop("Q", "QB", "D", "clkt");

        Arc arc = new Arc();
        arc.setInputTransition("D", PinEdge.Transition.RISE);
        arc.setOutputTransition("Q", PinEdge.Transition.RISE);
        arc.setClkTransition("clkt", PinEdge.Transition.RISE);
        arc.setClkFalseTransition("clkf", PinEdge.Transition.FALL);
        timing.addTimingArc(arc);

        arc = new Arc();
        arc.setInputTransition("D", PinEdge.Transition.FALL);
        arc.setOutputTransition("Q", PinEdge.Transition.FALL);
        arc.setClkTransition("clkt", PinEdge.Transition.RISE);
        arc.setClkFalseTransition("clkf", PinEdge.Transition.FALL);
        timing.addTimingArc(arc);

        return timing;
    }
}
