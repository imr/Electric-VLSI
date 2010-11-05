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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.io.output.IRSIM.IRSIMPreferences;
import com.sun.electric.tool.simulation.Engine;
import com.sun.electric.tool.user.waveform.WaveformWindow;
import java.net.URL;

/**
 * IRSIM simulator interface that is implemented by plugin
 */
public interface IAnalyzer {
    
	/**
	 * Create IRSIM Simulation Engine to simulate a cell.
     * @param ip IRSIM preferences
	 */
    public EngineIRSIM createEngine(String steppingModel, URL parameterURL, int irDebug);
    
    public interface EngineIRSIM extends Engine {

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
         * @param simFileURL URL of .sim fole
         * @return
         */
        public boolean inputSim(URL simFileURL);
        
        /**
         * Finish initialization of the circuit and convert Stimuli.
         */
        public void convertStimuli();
        
        /**
         * Finish initialization
         * @param ww WaveformWindow to show
         */
    	public void init(WaveformWindow ww);
    }
}
