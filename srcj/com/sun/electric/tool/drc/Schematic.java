/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Schematic.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.drc;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.RTBounds;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to do schematic design-rule checking.
 * Examines artwork of a schematic for sensibility.
 */
public class Schematic
{
    // Cells, nodes and arcs
    private Set<ElectricObject> nodesChecked = new HashSet<ElectricObject>();
    private ErrorLogger errorLogger;
    private Map<Geometric,List<Variable>> newVariables = new HashMap<Geometric,List<Variable>>();

    public static void doCheck(ErrorLogger errorLog, Cell cell, Geometric[] geomsToCheck, DRC.DRCPreferences dp)
    {
        Schematic s = new Schematic();
        s.errorLogger = errorLog;
        s.checkSchematicCellRecursively(cell, geomsToCheck);
        DRC.addDRCUpdate(0, null, null, null, null, s.newVariables, dp);
    }

    private Cell isACellToCheck(Geometric geo)
    {
        if (geo instanceof NodeInst)
        {
            NodeInst ni = (NodeInst)geo;

            // ignore documentation icon
            if (ni.isIconOfParent()) return null;

            if (!ni.isCellInstance()) return null;
            Cell subCell = (Cell)ni.getProto();

            Cell contentsCell = subCell.contentsView();
            if (contentsCell == null) contentsCell = subCell;
            if (nodesChecked.contains(contentsCell)) return null;
            return contentsCell;
        }
        return null;
    }

    private void checkSchematicCellRecursively(Cell cell, Geometric[] geomsToCheck)
    {
        nodesChecked.add(cell);

        // ignore if not a schematic
        if (!cell.isSchematic() && cell.getTechnology() != Schematics.tech())
            return;

        // recursively check contents in case of hierchically checking
        if (geomsToCheck == null)
        {
            for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
            {
                NodeInst ni = it.next();
                Cell contentsCell = isACellToCheck(ni);
                if (contentsCell != null)
                    checkSchematicCellRecursively(contentsCell, geomsToCheck);
            }
        }
        else
        {
            for (Geometric geo : geomsToCheck)
            {
                Cell contentsCell = isACellToCheck(geo);

                if (contentsCell != null)
                    checkSchematicCellRecursively(contentsCell, geomsToCheck);
            }
        }

        // now check this cell
        System.out.println("Checking schematic " + cell);
        ErrorGrouper eg = new ErrorGrouper(cell);
        checkSchematicCell(cell, false, geomsToCheck, eg);
    }

    private int cellIndexCounter;

    private class ErrorGrouper
    {
        private boolean inited;
        private int cellIndex;
        private Cell cell;

        ErrorGrouper(Cell cell)
        {
            inited = false;
            cellIndex = cellIndexCounter++;
            this.cell = cell;
        }

        public int getSortKey()
        {
            if (!inited)
            {
                inited = true;
                errorLogger.setGroupName(cellIndex, cell.getName());
            }
            return cellIndex;
        }
    }

    private void checkSchematicCell(Cell cell, boolean justThis, Geometric[] geomsToCheck, ErrorGrouper eg)
    {
        int initialErrorCount = errorLogger.getNumErrors();
        Netlist netlist = cell.getNetlist();

        // Normal hierarchically geometry
        if (geomsToCheck == null)
        {
            for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
            {
                NodeInst ni = it.next();
                if (!ni.isCellInstance() &&
                    ni.getProto().getTechnology() == Generic.tech()) continue;
                schematicDoCheck(netlist, ni, eg);
            }
            for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
            {
                ArcInst ai = it.next();
                schematicDoCheck(netlist, ai, eg);
            }
        } else
        {
            for (Geometric geo : geomsToCheck)
                schematicDoCheck(netlist, geo, eg);
        }

        checkCaseInsensitiveNetworks(netlist, eg);

        int errorCount = errorLogger.getNumErrors();
        int thisErrors = errorCount - initialErrorCount;
        String indent = "   ";
        if (justThis) indent = "";
        if (thisErrors == 0) System.out.println(indent + "No errors found"); else
            System.out.println(indent + thisErrors + " errors found");
        if (justThis) errorLogger.termLogging(true);
    }

    /**
     * Method to add all variables of a given NodeInst that must be added after Schematics DRC job is done.
     */
    private void addVariable(NodeInst ni, Variable var)
    {
        List<Variable> list = newVariables.get(ni);

        if (list == null) // first time
        {
            list = new ArrayList<Variable>();
            newVariables.put(ni, list);
        }
        list.add(var);
    }

    /**
     * Method to check schematic object "geom".
     */
    private void schematicDoCheck(Netlist netlist, Geometric geom, ErrorGrouper eg)
    {
        // Checked already
        if (nodesChecked.contains(geom))
            return;
        nodesChecked.add(geom);

        Cell cell = geom.getParent();
        if (geom instanceof NodeInst)
        {
            NodeInst ni = (NodeInst)geom;
            NodeProto np = ni.getProto();

            // check for bus pins that don't connect to any bus arcs
            if (np == Schematics.tech().busPinNode)
            {
                // proceed only if it has no exports on it
                if (!ni.hasExports())
                {
                    // must not connect to any bus arcs
                    boolean found = false;
                    for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
                    {
                        Connection con = it.next();
                        if (con.getArc().getProto() == Schematics.tech().bus_arc) { found = true;   break; }
                    }
                    if (!found)
                    {
                        errorLogger.logError("Bus pin does not connect to any bus arcs", geom, cell, null, eg.getSortKey());
                        return;
                    }
                }

                // flag bus pin if more than 1 wire is connected
                int i = 0;
                for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
                {
                    Connection con = it.next();
                    if (con.getArc().getProto() == Schematics.tech().wire_arc) i++;
                }
                if (i > 1)
                {
                    List<Geometric> geomList = new ArrayList<Geometric>();
                    geomList.add(geom);
                    for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
                    {
                        Connection con = it.next();
                        if (con.getArc().getProto() == Schematics.tech().wire_arc) i++;
                        geomList.add(con.getArc());
                    }
                    errorLogger.logMessage("Wire arcs cannot connect through a bus pin", geomList, cell, eg.getSortKey(), true);
                    return;
                }
            }

            // check all pins
            if (np.getFunction().isPin())
            {
                // may be stranded if there are no exports or arcs
                if (!ni.hasExports() && !ni.hasConnections())
                {
                    // see if the pin has displayable variables on it
                    boolean found = false;
                    for(Iterator<Variable> it = ni.getVariables(); it.hasNext(); )
                    {
                        Variable var = it.next();
                        if (var.isDisplay()) { found = true;   break; }
                    }
                    if (!found)
                    {
                        errorLogger.logError("Stranded pin (not connected or exported)", geom, cell, null, eg.getSortKey());
                        return;
                    }
                }

                if (ni.isInlinePin())
                {
                    errorLogger.logError("Unnecessary pin (between 2 arcs)", geom, cell, null, eg.getSortKey());
                    return;
                }

                Point2D pinLoc = ni.invisiblePinWithOffsetText(false);
                if (pinLoc != null)
                {
                    List<Geometric> geomList = new ArrayList<Geometric>();
                    List<EPoint> ptList = new ArrayList<EPoint>();
                    geomList.add(geom);
                    ptList.add(new EPoint(ni.getAnchorCenterX(), ni.getAnchorCenterY()));
                    ptList.add(new EPoint(pinLoc.getX(), pinLoc.getY()));
                    errorLogger.logMessageWithLines("Invisible pin has text in different location",
                    geomList, ptList, cell, eg.getSortKey(), true);
                    return;
                }
            }

            // check parameters
            if (np instanceof Cell)
            {
                Cell instCell = (Cell)np;
                Cell contentsCell = instCell.contentsView();
                if (contentsCell == null) contentsCell = instCell;

                // ensure that this node matches the parameter list
                for(Iterator<Variable> it = ni.getDefinedParameters(); it.hasNext(); )
                {
                    Variable var = it.next();
                    assert ni.isParam(var.getKey());

                    Variable foundVar = contentsCell.getParameter(var.getKey());
                    if (foundVar == null)
                    {
                        // this node's parameter is no longer on the cell: delete from instance
                        String trueVarName = var.getTrueName();
                        errorLogger.logError("Parameter '" + trueVarName + "' on " + ni +
                            " is invalid", geom, cell, null, eg.getSortKey());
                    } else
                    {
                        // this node's parameter is still on the cell: make sure units are OK
                        if (var.getUnit() != foundVar.getUnit())
                        {
                            String trueVarName = var.getTrueName();
                            errorLogger.logError("Parameter '" + trueVarName + "' on " + ni +
                                " had incorrect units (now fixed)", geom, cell, null, eg.getSortKey());
                            addVariable(ni, var.withUnit(foundVar.getUnit()));
                        }

                        // make sure visibility is OK
                        if (foundVar.isInterior())
                        {
                            if (var.isDisplay())
                            {
                                String trueVarName = var.getTrueName();
                                errorLogger.logError("Parameter '" + trueVarName + "' on " + ni +
                                    " should not be visible (now fixed)", geom, cell, null, eg.getSortKey());
                                addVariable(ni, var.withDisplay(false));
                            }
                        } else
                        {
                            if (!var.isDisplay())
                            {
                                String trueVarName = var.getTrueName();
                                errorLogger.logError("Parameter '" + trueVarName + "' on " + ni +
                                    " should be visible (now fixed)", geom, cell, null, eg.getSortKey());
                                addVariable(ni, var.withDisplay(true));
                            }
                        }
                    }
                }

                // make sure instance name isn't the same as a network in the cell
                String nodeName = ni.getName();
                for(Iterator<Network> it = netlist.getNetworks(); it.hasNext(); )
                {
                    Network net = it.next();
                    if (net.hasName(nodeName))
                    {
                        errorLogger.logError("Node " + ni + " is named '" + nodeName +
                            "' which conflicts with a network name in this cell", geom, cell, null, eg.getSortKey());
                        break;
                    }
                }
            }

            // check all exports for proper icon/schematics characteristics match
            Cell parentCell = ni.getParent();
            for(Iterator<Cell> cIt = parentCell.getCellGroup().getCells(); cIt.hasNext(); )
            {
                Cell iconCell = cIt.next();
                if (iconCell.getView() != View.ICON) continue;
                for(Iterator<Export> it = ni.getExports(); it.hasNext(); )
                {
                    Export e = it.next();
                    Export iconExport = e.getEquivalentPort(iconCell);
                    if (iconExport == null) continue;
                    if (e.getCharacteristic() != iconExport.getCharacteristic())
                    {
                        errorLogger.logError("Export '" + e.getName() + "' on " + ni +
                            " is " + e.getCharacteristic().getFullName() +
                            " but export in icon cell " + iconCell.describe(false) + " is " +
                        iconExport.getCharacteristic().getFullName(), geom, cell, null, eg.getSortKey());
                    }
                }
            }

            // check for port overlap
            checkPortOverlap(netlist, ni, eg);
        } else
        {
            ArcInst ai = (ArcInst)geom;

            // check for being floating if it does not have a visible name on it
            boolean checkDangle = false;

            if (Artwork.isArtworkArc(ai.getProto()))
                return; // ignore artwork arcs

            Name arcName = ai.getNameKey();
            if (arcName == null || arcName.isTempname()) checkDangle = true;
            if (checkDangle)
            {
                // do not check for dangle when busses are on named networks
                if (ai.getProto() == Schematics.tech().bus_arc)
                {
                    Name name = netlist.getBusName(ai);
                    if (name != null && !name.isTempname()) checkDangle = false;
                }
            }
            if (checkDangle)
            {
                // check to see if this arc is floating
                for(int i=0; i<2; i++)
                {
                    NodeInst ni = ai.getPortInst(i).getNodeInst();

                    // OK if not a pin
                    if (!ni.getProto().getFunction().isPin()) continue;

                    // OK if it has exports on it
                    if (ni.hasExports()) continue;

                    // OK if it connects to more than 1 arc
                    if (ni.getNumConnections() != 1) continue;

                    // the arc dangles
                    errorLogger.logError("Arc dangles", geom, cell, null, eg.getSortKey());
                    return;
                }
            }

            // check to see if its width is sensible
            int signals = netlist.getBusWidth(ai);
            if (signals < 1) signals = 1;
            for(int i=0; i<2; i++)
            {
                PortInst pi = ai.getPortInst(i);
                NodeInst ni = pi.getNodeInst();
                if (!ni.isCellInstance()) continue;
                Cell subNp = (Cell)ni.getProto();
                PortProto pp = pi.getPortProto();

                Cell np = subNp.contentsView();
                if (np != null)
                {
                    pp = ((Export)pi.getPortProto()).getEquivalent();
                    if (pp == null || pp == pi.getPortProto())
                    {
                        List<Geometric> geomList = new ArrayList<Geometric>();
                        geomList.add(geom);
                        geomList.add(ni);
                        errorLogger.logMessage("Arc " + ai.describe(true) + " connects to " +
                            pi.getPortProto() + " of " + ni + ", but there is no equivalent port in " + np,
                            geomList, cell, eg.getSortKey(), true);
                        continue;
                    }
                }

                int portWidth = netlist.getBusWidth((Export)pp);
                if (portWidth < 1) portWidth = 1;
                int nodeSize = ni.getNameKey().busWidth();
                if (nodeSize <= 0) nodeSize = 1;
                if (signals != portWidth && signals != portWidth*nodeSize)
                {
                    List<Geometric> geomList = new ArrayList<Geometric>();
                    geomList.add(geom);
                    geomList.add(ni);
                    errorLogger.logMessage("Arc " + ai.describe(true) + " (" + signals + " wide) connects to " +
                        pp + " of " + ni + " (" + portWidth + " wide)", geomList, cell, eg.getSortKey(), true);
                }
            }
        }
    }

    /**
     * Method to check whether any port on a node overlaps others without connecting.
     */
    private void checkPortOverlap(Netlist netlist, NodeInst ni, ErrorGrouper eg)
    {
        if (ni.getProto().getTechnology() == Generic.tech() ||
            ni.getProto().getTechnology() == Artwork.tech()) return;
        Cell cell = ni.getParent();
        for(Iterator<PortInst> it = ni.getPortInsts(); it.hasNext(); )
        {
            PortInst pi = it.next();
            Network net = netlist.getNetwork(pi);
            Rectangle2D bounds = pi.getPoly().getBounds2D();
            for(Iterator<RTBounds> sIt = cell.searchIterator(bounds); sIt.hasNext(); )
            {
                Geometric oGeom = (Geometric)sIt.next();
                if (!(oGeom instanceof NodeInst)) continue;
                NodeInst oNi = (NodeInst)oGeom;
                if (ni == oNi) continue;
                if (ni.getNodeIndex() > oNi.getNodeIndex()) continue;
                if (oNi.getProto().getTechnology() == Generic.tech() ||
                    oNi.getProto().getTechnology() == Artwork.tech()) continue;

                // see if ports touch
                for(Iterator<PortInst> pIt = oNi.getPortInsts(); pIt.hasNext(); )
                {
                    PortInst oPi = pIt.next();
                    Rectangle2D oBounds = oPi.getPoly().getBounds2D();
                    if (bounds.getMaxX() < oBounds.getMinX()) continue;
                    if (bounds.getMinX() > oBounds.getMaxX()) continue;
                    if (bounds.getMaxY() < oBounds.getMinY()) continue;
                    if (bounds.getMinY() > oBounds.getMaxY()) continue;

                    // see if they are connected
                    if (net == netlist.getNetwork(oPi)) continue;

                    // report the error
                    List<Geometric> geomList = new ArrayList<Geometric>();
                    geomList.add(ni);
                    geomList.add(oNi);
                    errorLogger.logMessage("Nodes '" + ni + "' '" + oNi + "' have touching ports that are not connected",
                        geomList, cell, eg.getSortKey(), true);
                    return;
                }
            }
        }
    }

    private void checkCaseInsensitiveNetworks(Netlist netlist, ErrorGrouper eg) {
        Cell cell = netlist.getCell();
        HashMap<String, Network> canonicToNetwork = new HashMap<String, Network>();
        for (Iterator<Network> it = netlist.getNetworks(); it.hasNext(); ) {
            Network net = it.next();
            for (Iterator<String> sit = net.getNames(); sit.hasNext(); ) {
                String s = sit.next();
                String cs = TextUtils.canonicString(s);
                Network net1 = canonicToNetwork.get(cs);
                if (net1 == null ) {
                    canonicToNetwork.put(cs, net);
                } else if (net1 != net) {
                    String message = "Network: Schematic " + cell.libDescribe() + " doesn't connect " + net + " and " + net1;
                    boolean sameName = net1.hasName(s);
                    if (sameName)
                        message += " Like-named Global and Export may be connected in future releases";
                    System.out.println(message);
                    List<Geometric> geomList = new ArrayList<Geometric>();
                    push(geomList, net);
                    push(geomList, net1);
                    errorLogger.logMessage(message, geomList, cell, eg.getSortKey(), sameName);
                }
            }
        }
    }

    private void push(List<Geometric> geomList, Network net) {
        Iterator<Export> eit = net.getExports();
        if (eit.hasNext()) {
            geomList.add(eit.next().getOriginalPort().getNodeInst());
            return;
        }
        Iterator<ArcInst> ait = net.getArcs();
        if (ait.hasNext()) {
            geomList.add(ait.next());
            return;
        }
        Iterator<PortInst> pit = net.getPorts();
        if (pit.hasNext()) {
            geomList.add(pit.next().getNodeInst());
            return;
        }
    }
}
