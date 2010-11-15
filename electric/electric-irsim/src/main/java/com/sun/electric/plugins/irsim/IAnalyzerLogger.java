/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.electric.plugins.irsim;

import com.sun.electric.api.irsim.IAnalyzer;
import java.io.File;
import java.io.IOException;
import java.io.Reader;

/**
 *
 * @author dn146861
 */
public class IAnalyzerLogger implements IAnalyzer.EngineIRSIM {
    private final IAnalyzer.EngineIRSIM impl;

    IAnalyzerLogger(IAnalyzer.EngineIRSIM impl) {
        this.impl = impl;
    }
    
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
            boolean isNTypeTransistor) {
        System.out.println("x.putTransistor(\"" + gateName + "\", \"" + sourceName + "\", \"" + drainName + "\", " +
                gateLength + ", " + gateWidth + ", " + activeArea + ", " + activePerim + ", " +
                centerX + ",  " + centerY + ", " + isNTypeTransistor + ");");
        impl.putTransistor(gateName, sourceName, drainName, gateLength, gateWidth, activeArea, activePerim, centerX, centerY, isNTypeTransistor);
    }

    /**
     * Put resistor into the circuit
     * @param net1 name of first terminal network
     * @param net2 name of second terminal network
     * @param resistance resistance (ohm)
     */
    public void putResistor(String net1, String net2, double resistance) {
        System.out.println("x.putResistor(\"" + net1 + "\", \"" + net2 + "\", " + resistance + ");");
        impl.putResistor(net1, net2, resistance);
    }

    /**
     * Put capacitor into the circuit
     * @param net1 name of first terminal network
     * @param net2 name of second terminal network
     * @param capacitance capacitance (pf)
     */
    public void putCapacitor(String net1, String net2, double capacitance) {
        System.out.println("x.putCapacitor(\"" + net1 + "\", \"" + net2 + "\", " + capacitance + ");");
        impl.putCapacitor(net1, net2, capacitance);
    }

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
    public int inputSim(Reader simReader, String fileName) throws IOException {
        System.out.println("x.inputSim(simReader, \"" + fileName + "\");");
        return impl.inputSim(simReader, fileName);
    }

    /**
     * Finish initialization of the circuit.
     */
    public void finishNetwork() {
        System.out.println("x.finishNetwork();");
        impl.finishNetwork();
    }

    /**
     * Get lambda value in nanometers
     * @return lambda in nanometers
     */
    public double getLambda() {
        System.out.println("double lambda = x.getLambda();");
        return impl.getLambda();
    }

    /**
     * Finish initialization of the circuit and convert Stimuli.
     */
    public void convertStimuli() {
        System.out.println("x.convertStimuli();");
        impl.convertStimuli();
    }

    /**
     * Finish initialization
     */
    public void init() {
        System.out.println("x.init();");
        impl.init();
    }

    /**
     * Method to play the simulation vectors into the simulator.
     */
    public void playVectors() {
        System.out.println("x.playVectors();");
        impl.playVectors();
    }

    public void newContolPoint(String signalName, double insertTime, IAnalyzer.LogicState value) {
        System.out.println("x.newControlPoint(\"" + signalName + "\", " + insertTime + ", Digita;Sample.Value." + value.name() + ");");
        impl.newContolPoint(signalName, insertTime, value);
    }

    /**
     * Method to show information about the currently-selected signal.
     */
    public void showSignalInfo(IAnalyzer.GuiSignal sig) {
        System.out.println("x.showSignalInfo(findSignal(\"" + sig.getFullName() + "\");");
        impl.showSignalInfo(sig);
    }

    /**
     * Method to clear all simulation vectors.
     */
    public void clearAllVectors() {
        System.out.println("x.clearAllVectors();");
        impl.clearAllVectors();
    }

    /**
     * Method to remove all stimuli from the currently-selected signal.
     * @param sig currently selected signal.
     */
    public void clearControlPoints(IAnalyzer.GuiSignal sig) {
        System.out.println("x.clearControlPoints(findSignal(\"" + sig.getFullName() + "\");");
        impl.clearControlPoints(sig);
    }

    /**
     * Method to remove the selected stimuli.
     * @return true if stimuli were deleted.
     */
    public boolean clearControlPoint(IAnalyzer.GuiSignal sig, double insertTime) {
        System.out.println("x.clearControlPoint(findSignal(\"" + sig.getFullName() + "\", " + insertTime + ");");
        return impl.clearControlPoint(sig, insertTime);
    }

    /**
     * Method to save the current stimuli information to disk.
     * @param stimuliFile file to save stimuli information
     */
    public void saveStimuli(File stimuliFile) throws IOException {
        System.out.println("x.saveStimuli(new File(\"" + stimuliFile.getPath() + "\");");
        impl.saveStimuli(stimuliFile);
    }

    /**
     * Method to restore the current stimuli information from URL.
     * @param reader Reader with stimuli information
     */
    public void restoreStimuli(Reader reader) throws IOException {
        System.out.println("x.restoreStimuli(reader);");
        impl.restoreStimuli(reader);
    }
}
