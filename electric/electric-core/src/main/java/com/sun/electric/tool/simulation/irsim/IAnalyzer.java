/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IAnalyzer.java
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.simulation.irsim;

import com.sun.electric.tool.simulation.DigitalSample;
import com.sun.electric.tool.simulation.MutableSignal;
import com.sun.electric.tool.simulation.Signal;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;
import java.util.List;

/**
 * IRSIM simulator interface that is implemented by plugin
 */
public interface IAnalyzer {
    
	/**
	 * Create IRSIM Simulation Engine to simulate a cell.
     * @param gui interface to GUI
     * @param steppingModel stepping model either "RC" or "Linear"
     * @param parameterURL URL of IRSIM parameter file
     * @param irDebug debug flags
     * @param showCommands tru to print issued IRSIM commands
     * @param isDelayedX true if using the delayed X model, false if using the old fast-propagating X model.
	 */
    public EngineIRSIM createEngine(GUI gui, String steppingModel, URL parameterURL, int irDebug, boolean showCommands, boolean isDelayedX);
    
    public interface EngineIRSIM {
        /**
         * Put triansitor into the circuit
         * @param gateName name of transistor gate network
         * @param sourceName name of transistor gate network
         * @param drainName drain name of transistor gate network
         * @param gateLength gate length (lambda)
         * @param gateWidth gate width (lambda)
         * @param activeArea active area (lambda^2)
         * @param activePerim active perim (lambda^2)
         * @param centerX x-coordinate of center (lambda)
         * @param centerY y coordinate of cneter (lambda)
         * @param isNTypeTransistor true if this is N-type transistor
         */
        public void putTransistor(String gateName, String sourceName, String drainName,
                double gateLength, double gateWidth,
                double activeArea, double activePerim,
                double centerX, double centerY,
                boolean isNTypeTransistor);
        /**
         * Put resistor into the circuit
         * @param net1 name of first terminal network
         * @param net2 name of second terminal network
         * @param resistance resistance (ohm)
         */
        public void putResistor(String net1, String net2, double resistance);
        /**
         * Put capacitor into the circuit
         * @param net1 name of first terminal network
         * @param net2 name of second terminal network
         * @param capacitance capacitance (pf)
         */
        public void putCapacitor(String net1, String net2, double capacitance);
        
        /**
         * Load a .sim file into memory.
         *
         * A .sim file consists of a series of lines, each of which begins with a key letter.
         * The key letter beginning a line determines how the remainder of the line is interpreted.
         * The following are the list of key letters understood.
         *
         *   | units: s tech: tech format: MIT|LBL|SU
         *     If present, this must be the first line in the .sim file.
         *     It identifies the technology of this circuit as tech and gives a scale factor for units of linear dimension as s.
         *     All linear dimensions appearing in the .sim file are multiplied by s to give centimicrons.
         *     The format field signifies the sim variant. Electric only recognizes SU format. 
         *   type g s d l w x y g=gattrs s=sattrs d=dattrs
         *     Defines a transistor of type type. Currently, type may be e or d for NMOS, or p or n for CMOS.
         *     The name of the node to which the gate, source, and drain of the transistor are connected are given by g, s, and d respectively.
         *     The length and width of the transistor are l and w. The next two tokens, x and y, are optional.
         *     If present, they give the location of a point inside the gate region of the transistor.
         *     The last three tokens are the attribute lists for the transistor gate, source, and drain.
         *     If no attributes are present for a particular terminal, the corresponding attribute list may be absent
         *     (i.e, there may be no g= field at all).
         *     The attribute lists gattrs, etc. are comma-separated lists of labels.
         *     The label names should not include any spaces, although some tools can accept label names with
         *     spaces if they are enclosed in double quotes. In version 6.4.5 and later the default format
         *     produced by ext2sim is SU. In this format the attribute of the gate starting with S_ is the substrate node of the fet.
         *     The attributes of the gate, and source and substrate starting with A_, P_ are the area and perimeter
         *     (summed for that node only once) of the source and drain respectively. This addition to the format is backwards compatible. 
         *   C n1 n2 cap
         *     Defines a capacitor between nodes n1 and n2. The value of the capacitor is cap femtofarads.
         *     NOTE: since many analysis tools compute transistor gate capacitance themselves from the
         *     transistor's area and perimeter, the capacitance between a node and substrate (GND!)
         *     normally does not include the capacitance from transistor gates connected to that node.
         *     If the .sim file was produced by ext2sim(1), check the technology file that was used to
         *     produce the original .ext files to see whether transistor gate capacitance is included or excluded;
         *     see "Magic Maintainer's Manual 2 - The Technology File for details. 
         *   R node res
         *     Defines the lumped resistance of node node to be res ohms.
         *   r node1 node2 res
         *     Defines an explicit resistor between nodes node1 and node2 of resistance res ohms.
         *   N node darea dperim parea pperim marea mperim
         *     As an alternative to computed capacitances, some tools expect the total perimeter and area
         *     of the polysilicon, diffusion, and metal in each node to be reported in the .sim file.
         *     The N construct associates diffusion area darea (in square centimicrons) and diffusion
         *     perimeter dperim (in centimicrons) with node node, polysilicon area parea and perimeter pperim,
         *     and metal area marea and perimeter mperim. This construct is technology dependent and obsolete. 
         *   = node1 node2
         *     Each node in a .sim file is named implicitly by having it appear in a transistor definition.
         *     All node names appearing in a .sim file are assumed to be distinct.
         *     Some tools, such as esim(1), recognize aliases for node names.
         *     The = construct allows the name node2 to be defined as an alias for the name node1.
         *     Aliases defined by means of this construct may not appear anywhere else in the .sim file.
         * @param simReader Reader of .sim file
         * @param fileName file name for error messages
         * @return number of errors
         */
        public int inputSim(Reader simReader, String fileName) throws IOException;
        
        /**
         * Finish initialization of the circuit.
         */
        public void finishNetwork();
        
        /**
         * Get lambda value in nanometers
         * @return lambda in nanometers
         */
        public double getLambda();
        
        /**
         * Finish initialization of the circuit and convert Stimuli.
         */
        public void convertStimuli();
        
        /**
         * Finish initialization
         */
    	public void init();
        
        /**
         * Method to play the simulation vectors into the simulator.
         */
        public void playVectors();
        
        public void newContolPoint(String signalName, double insertTime, DigitalSample.Value value);
        /**
         * Method to show information about the currently-selected signal.
         */
        public void showSignalInfo(Signal<?> sig);
        /**
         * Method to clear all simulation vectors.
         */
        public void clearAllVectors();
       /**
         * Method to remove all stimuli from the currently-selected signal.
         * @param sig currently selected signal.
         */
        public void clearContolPoints(Signal<?> sig);
        /**
         * Method to remove the selected stimuli.
         * @return true if stimuli were deleted.
         */
        public boolean clearControlPoint(Signal<?> sig, double insertTime);
        /**
         * Method to save the current stimuli information to disk.
         * @param stimuliFile file to save stimuli information
         */
        public void saveStimuli(File stimuliFile) throws IOException;
        /**
         * Method to restore the current stimuli information from URL.
         * @param reader Reader with stimuli information
         */
        public void restoreStimuli(Reader reader) throws IOException;
    }
    
    public interface GUI {
        public MutableSignal<DigitalSample> makeSignal(String name);
        public void makeBusSignals(List<Signal<?>> sigList);
        public void createBus(String busName, Signal<DigitalSample> ... subsigs);
        public Collection<Signal<?>> getSignals();
        
        public void setMainXPositionCursor(double curTime);
        public void openPanel(Collection<Signal<DigitalSample>> sigs);
        public void closePanels();
        public double getMaxPanelTime();
        public void repaint();
   }
}
