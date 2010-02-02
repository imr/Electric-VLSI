/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HsimModel.java
 * Written by Frankie Liu, Sun Microsystems.
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.*;

public class HsimModel extends NanosimModel {

  protected String simFile;

  public HsimModel() {
    super();
  }

  /**
   * Set the voltage on node
   * @param node name of node
   * @param value voltage to set in volts
   */
  public void setNodeVoltage(String node, double value) {
      if (assuraRCXNetlist) {
	node = node.replaceAll("\\.", "/");
      } else if (starRCXTNetlist) {
          if (node.startsWith("x")) node = node.substring(1);
          node = node.replaceAll("\\.x?", "/");
      }
      node = node.replaceAll("\\[","?");
      node = node.replaceAll("\\]","?");
    issueCommand("fv "+node+" "+value);
  }

  /**
   * Releases (forcing) on voltage on node
   * @param node name of node
   */
  public void releaseNodeVoltage(String node){
      if (assuraRCXNetlist) {
	node = node.replaceAll("\\.", "/");
      } else if (starRCXTNetlist) {
          if (node.startsWith("x")) node = node.substring(1);
          node = node.replaceAll("\\.x?", "/");
      }
      node = node.replaceAll("\\[","?");
      node = node.replaceAll("\\]","?");
    issueCommand("rv "+node+"");
  }


  /**
   * Sets voltage on node to 0
   * @param node name of node
   */
  public void disableNode(String node) {
    setNodeVoltage(node,0);
    // waitNS(timeStep);
  }

  /**
   * Same a releaseNodeVoltage
   * @param node name of node
   */
  public void enableNode(String node) {
    releaseNodeVoltage(node);
    // waitNS(timeStep);
  }


  /**
   * Checks if simFile is from Assura RCX
   * @return true if no IOException
   */
  protected boolean checkRcx() {
    // check if simfile is from assura RCX
    // if it is, we will have to replace hierarchy delimiter '.' with '/'
    System.out.println(simFile);

    try {
      BufferedReader freader = new BufferedReader(new FileReader(simFile));
      // PROGRAM will be in the first 10 or so lines, so search up till line 20
      for (int i=0; i<20; i++) {
	String line = freader.readLine();
	if (line == null) break;
	if (line.matches("\\*  PROGRAM .*?assura.*")) {
            assuraRCXNetlist = true;
            System.out.println("Info: Running on Assura extracted netlist, will replace all '.' in net names with '/'");
            break;
        } else if (line.matches("\\*|PROGRAM .*?Star-RCXT.*")) {
            starRCXTNetlist = true;
            System.out.println("Info: Running on Star-RCXT extracted netlist, will replace all '.x' in net names with '/'");
            break;
	}
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
      return false;
    }
    return true;
  }

  boolean start_(String command, String simFile, int recordSim) {

    setPrompt("HSIM > ");
    this.simFile=simFile;

    if (!checkRcx()){
      return false;
    }

    // consider the -top option when the top-level goo is wrapped in a .subckt

    // max sim time (-t) is 90071s
    // -wait_lic?
    if (!startProcess(command+" -time 90000 "+simFile+" -o "+simFile, null, null, simFile+".run"))
      return false;
      
    // get value of vdd
    //vdd = getNodeVoltage("vdd");
    vdd = 0.9;
    if (DEBUG)
      System.out.println("Using VDD of "+vdd);
    timeStep = getSimTres();
    if (DEBUG)
      System.out.println("Using time step of "+timeStep+" ns");

    // initialize test devices
    for (Iterator it = logicSettables.iterator(); it.hasNext(); ) {
      NanosimLogicSettable ls = (NanosimLogicSettable)it.next();
      if (!ls.init()) {
	System.out.println("LogicSettable initialization failed, aborting.");
	return false;
      }
    }
    for (Iterator it = jtagTesters.iterator(); it.hasNext(); ) {
      JtagTester tester = (JtagTester)it.next();
      tester.reset();
    }
    return true;
  }

  protected static final Pattern patSimTime = Pattern.compile("tnow=(\\S+)ns");
  public static final double badtime = -1;

  /**
   * Gets simulation time
   * @return simulation time in ns
   */
  public double getSimTime() {
    issueCommand("pt");
    String result = getLastCommandOutput().toString();
    if(!result.startsWith("tnow")){
      System.out.println(">> Error in getSimTime : no tnow");
      return badtime;
    }
    Matcher m = patSimTime.matcher(result);
    if (!m.find()){
      System.out.println(">> Error in getSimTime : no match");
      return badtime;
    }
    double tmp;
    try {
      tmp = Double.parseDouble(m.group(1));
    } catch(NumberFormatException e) {
      System.out.println(">> Error in getSimTime : bad number conversion");
      return badtime;
    }
    return tmp;
  }

  /**
   * Applies voltages and continues simualtion for a time
   * @param nanoseconds time to continue simuation
   * @param applyVoltages true if voltages are to be applied
   */
  public void waitNS(double nanoseconds, boolean applyVoltages) {

    if (applyVoltages)
      applyVoltages();

    double tmp = getSimTime();
    if (tmp!=badtime) {
      simTime = tmp;
    }

    // calculate stop time (time at which to set breakpoint)
    double stopTime = simTime + nanoseconds;

    // set break point
    issueCommand("stop -at "+stopTime); // in ns
    issueCommand("cont");
    simTime = stopTime;

  }


  /**
   * Get the simulation's time step
   * @return the simulation time step in nanoseconds
   */
  public double getSimTres() {
    return 0.01;    // default is 10ps
  }

  // ====================================================================
  // ====================================================================

  //                      Get/Set voltages

  // ====================================================================
  // ====================================================================

  /**
   * This is used to apply all voltages at the same time for the current time step.
   */
  public void applyVoltages() {
    // release all forced nodes
    releaseNodes(new ArrayList(nodesToSet.keySet()));

    // all nodes are released, now apply all the values
    for (Iterator it = nodesToSet.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry entry = (Map.Entry)it.next();
      String node = (String)entry.getKey();
      Double voltage = (Double)entry.getValue();
      if (assuraRCXNetlist) {
	node = node.replaceAll("\\.", "/");
      } else if (starRCXTNetlist) {
          if (node.startsWith("x")) node = node.substring(1);
          node = node.replaceAll("\\.x?", "/");
      }
      setNodeVoltage(node,voltage.doubleValue());
    }
    nodesToSet.clear();
    // waitNS(timeStep, false);
  }
  /**
   * release the list of nodes (Strings)
   */
  public void releaseNodes(List nodes) {
    for (Iterator it = nodes.iterator(); it.hasNext(); ) {
      String node = (String)it.next();
      if (assuraRCXNetlist) {
	node = node.replaceAll("\\.", "/");
      } else if (starRCXTNetlist) {
          if (node.startsWith("x")) node = node.substring(1);
          node = node.replaceAll("\\.x?", "/");
      }
      releaseNodeVoltage(node);
    }
    // waitNS(timeStep, false);
  }

  // sample output:
  //
  // HSIM > nv out
  // Node out (2):
  // V=0.628472, dV/dt=0.00371528 V/ns, time=100.000000ns
  // Capacitance report
  // Constant node capacitance          : 10000 fF
  // Voltage-dependent node capacitance : 0 fF
  // Total node capacitance             : 10000 fF

  protected static final Pattern patNodeInfo[] = {
    Pattern.compile(".*?V=(\\S+), dV/dt=(\\S+) (\\S+), time=(\\S+)"),
    Pattern.compile(".*?Constant node capacitance\\s+: (\\S+) (\\S+)"),
    Pattern.compile(".*?Voltage-dependent node capacitance\\s+: (\\S+) (\\S+)"),
    Pattern.compile(".*?Total node capacitance\\s+: (\\S+) (\\S+)")
  };

  /**
   * Gets the voltages on the nodes
   * @param nodes the nodes to query
   * @param returnState defunct, not used
   * @return a list of voltages, or null on error
   */
  protected List getNodeInfo(List nodes, boolean returnState) {

    // apply any outstanding voltages
    if (nodesToSet.size() > 0)
      applyVoltages();

    if (assuraRCXNetlist || starRCXTNetlist) {
      List nodesfixed = new ArrayList();
      for (Iterator it = nodes.iterator(); it.hasNext(); ) {
	String node = (String)it.next();
        if (assuraRCXNetlist) {
            node = node.replaceAll("\\.", "/");
        } else if (starRCXTNetlist) {
          if (node.startsWith("x")) node = node.substring(1);
          node = node.replaceAll("\\.x?", "/");
        }
	nodesfixed.add(node);
      }
      nodes = nodesfixed;
    }

    List nodeslc = new ArrayList();
    List voltages = new ArrayList();

    StringBuffer cmd = new StringBuffer();
    cmd.append("nv ");

    boolean error=false;
    for (Iterator it = nodes.iterator(); it.hasNext() && !error; ) {
      String node = (String)it.next();
      node = node.toLowerCase();
      nodeslc.add(node);

      String nodex = node;
      nodex = nodex.replaceAll("\\[","?");
      nodex = nodex.replaceAll("\\]","?");

      // HACK
      if (nodex.equals("vss")) {
          voltages.add(0);
      } else {
          cmd.append(nodex);
          issueCommand(cmd.toString());
          String result = getLastCommandOutput().toString();
      
          /*
            
            String [] results = result.toString().trim().split("\n");
            
            int i = results.length;
            for (i=0;i<results.length;i++){
            System.out.println("Results: "+i);
            System.out.println(results[i]);
            }

          */

          if (!result.startsWith("Node "+node)) {
              System.out.println("Error on getInfoNode: for node "+node+"; result:\n  "+result);
              error=true;
              continue;
          }

          Double voltage = new Double(0);
          Double dvdt    = new Double(0);
          Double time    = new Double(0);
          Double cap     = new Double(0);
          Double vcap    = new Double(0);
          Double tcap    = new Double(0);
          
          for (int patternIndex=0; patternIndex < patNodeInfo.length && !error; patternIndex++) {
              
              Matcher m = patNodeInfo[patternIndex].matcher(result);
              if (!m.find()){
                  System.out.println(">> Error on getInfoNode : for node "+node);
                  System.out.println(">> Pattern " + patternIndex +" : "+patNodeInfo[patternIndex]);
                  System.out.println(">> String : "+result);
                  error=true;
                  continue;
              }
              try {
                  switch (patternIndex) {
                      case 0:
                          voltage = Double.parseDouble(m.group(1));
                          dvdt    = Double.parseDouble(m.group(2));
                          // time    = Double.parseDouble(m.group(4));
                          break;
                      case 1:
                          cap     = Double.parseDouble(m.group(1));
                          break;
                      case 2:
                          vcap    = Double.parseDouble(m.group(1));
                          break;
                      case 3:
                          tcap    = Double.parseDouble(m.group(1));
                          break;
                      case 4:
                          break;
                      default:
                  }
              } catch (NumberFormatException e) {
                  System.out.println(">> Error on get_info_node: NumberFormatException for node "+node);
                  System.out.println(">> Pattern: "+patNodeInfo[patternIndex]);
                  System.out.println(">> String: "+result);
                  for(int ngroup=0;ngroup<=m.groupCount();ngroup++){
                      System.out.println(">> Group "+ngroup+" : "+m.group(ngroup));
                  }
                  error=true; continue;
              }
              result = result.substring(m.end());
              if (DEBUG) {
                  System.out.println(">> Pattern: "+patNodeInfo[patternIndex]);
                  System.out.println(">> "+result);
              }
          }

          if (DEBUG) {
              System.out.println("getInfoNode "+node+":");
              System.out.println(voltage);
              System.out.println(dvdt);
              System.out.println(time);
              System.out.println(cap);
              System.out.println(vcap);
              System.out.println(tcap);
          }
          
          voltages.add(returnState ? (voltage >= vdd/2 ? 1.0 : 0.0) : voltage);
      }
    }

    if (error)
      return null;
    else
      return voltages;
  }

  /**
   * Saves all nodes. Xref restartDatabase, see also .STORE/.RESTORE
   * @param filename 
   */
  public void saveDatabase(String filename) {
    issueCommand("savesim filename");
  }

  /**
   * Restore all nodes. Xref saveDatabase, see also .STORE/.RESTORE
   * @param filename 
   * @param timens some earlier time in nanoseconds, basically
   * one can restart many times, each output file will have .rs# suffix
   */
  public void restartDatabase(String filename,double timens) {
    issueCommand("restart "+timens+" "+filename);
  }

  /** Unit Test
   * This test requires the file sim.spi in your working dir
   * */
  public static void main(String[] args) {
    HsimModel nm = new HsimModel();
    // the example file sim.spi needs to be in your working dir
    nm.start("hsim64", "rc.sp", 0);
    nm.issueCommand("pt");
    nm.waitNS(2);
    nm.finish();
    
    /* rc.sp
* Title
.option post
vin vdd gnd 1
rin vdd out 10k
cout out gnd 10p
.ic v(out) 0
.tran 10p 1000n
.param HSIMSTOPAT=100n
*.stop at=100n
.print v(*)
.end
    */

  }
}
