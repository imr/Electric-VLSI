/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TechPalette.java
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.change.Undo;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.CellBrowser;
import com.sun.electric.tool.user.dialogs.AnnularRing;
import com.sun.electric.tool.user.dialogs.LayoutText;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.menus.CellMenu;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.lib.LibFile;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: root
 * Date: Aug 14, 2004
 * Time: 11:58:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class TechPalette extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener,
        KeyListener, PaletteFrame.PlaceNodeEventListener {

    /** the number of palette entries. */				private int menuX = -1, menuY = -1;
    /** the size of a palette entry. */					private int entrySize;
    /** the list of objects in the palette. */			private List inPalette;
    /** the currently selected Node object. */			private Object highlightedNode;

    TechPalette()
    {
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    /**
     * Loads a new technology into the palette. Returns the
     * new desired size of the panel
     * @param tech the technology to load
     * @return the preferred size of the new panel
     */
    public Dimension loadForTechnology(Technology tech)
    {
        inPalette = new ArrayList();
        if (tech == Schematics.tech)
        {
            menuX = 2;
            menuY = 14;
            inPalette.add(Schematics.tech.wire_arc);
            inPalette.add(Schematics.tech.wirePinNode);
            inPalette.add("Spice");
            inPalette.add(Schematics.tech.offpageNode);
            inPalette.add(Schematics.tech.globalNode);
            inPalette.add(Schematics.tech.powerNode);
            inPalette.add(Schematics.tech.resistorNode);
            inPalette.add(Schematics.tech.capacitorNode);
            inPalette.add(makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4PNP, 900));
            inPalette.add(makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRAPNP, 900));
            inPalette.add(Schematics.tech.switchNode);
            inPalette.add(Schematics.tech.muxNode);
            inPalette.add(Schematics.tech.xorNode);
            inPalette.add(Schematics.tech.bboxNode);

            inPalette.add(Schematics.tech.bus_arc);
            inPalette.add(Schematics.tech.busPinNode);
            inPalette.add("Cell");
            inPalette.add(Schematics.tech.wireConNode);
            inPalette.add("Misc.");
            inPalette.add(Schematics.tech.groundNode);
            inPalette.add(Schematics.tech.inductorNode);
            inPalette.add(Schematics.tech.diodeNode);
            inPalette.add(makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRAPMOS, 900));
            inPalette.add(makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRANMOS, 900));
            inPalette.add(Schematics.tech.flipflopNode);
            inPalette.add(Schematics.tech.bufferNode);
            inPalette.add(Schematics.tech.orNode);
            inPalette.add(Schematics.tech.andNode);
        } else if (tech == Artwork.tech)
        {
            menuX = 2;
            menuY = 12;
            inPalette.add(Artwork.tech.solidArc);
            inPalette.add(Artwork.tech.thickerArc);
            inPalette.add("Cell");
            inPalette.add(Artwork.tech.openedPolygonNode);
            inPalette.add(Artwork.tech.openedThickerPolygonNode);
            inPalette.add(Artwork.tech.filledTriangleNode);
            inPalette.add(Artwork.tech.filledBoxNode);
            inPalette.add(makeNodeInst(Artwork.tech.filledPolygonNode, NodeProto.Function.ART, 0));
            inPalette.add(Artwork.tech.filledCircleNode);
            inPalette.add(Artwork.tech.pinNode);
            inPalette.add(Artwork.tech.crossedBoxNode);
            inPalette.add(Artwork.tech.thickCircleNode);

            inPalette.add(Artwork.tech.dottedArc);
            inPalette.add(Artwork.tech.dashedArc);
            inPalette.add("Text");
            inPalette.add(Artwork.tech.openedDottedPolygonNode);
            inPalette.add(Artwork.tech.openedDashedPolygonNode);
            inPalette.add(Artwork.tech.triangleNode);
            inPalette.add(Artwork.tech.boxNode);
            inPalette.add(makeNodeInst(Artwork.tech.closedPolygonNode, NodeProto.Function.ART, 0));
            inPalette.add(Artwork.tech.circleNode);
            inPalette.add("Export");
            inPalette.add(Artwork.tech.arrowNode);
            inPalette.add(makeNodeInst(Artwork.tech.splineNode, NodeProto.Function.ART, 0));
        } else
        {
            int pinTotal = 0, pureTotal = 0, compTotal = 0, arcTotal = 0;
            ArcProto firstHighlightedArc = null;
            highlightedNode = null;
            for(Iterator it = tech.getArcs(); it.hasNext(); )
            {
                PrimitiveArc ap = (PrimitiveArc)it.next();
                if (ap.isNotUsed()) continue;
                PrimitiveNode np = ap.findPinProto();
                if (np != null && np.isNotUsed()) continue;
                if (firstHighlightedArc == null) firstHighlightedArc = ap;
                arcTotal++;
                inPalette.add(ap);
            }
            User.tool.setCurrentArcProto(firstHighlightedArc);
            inPalette.add("Cell");
            inPalette.add("Misc.");
            inPalette.add("Pure");
            for(Iterator it = tech.getNodes(); it.hasNext(); )
            {
                PrimitiveNode np = (PrimitiveNode)it.next();
                if (np.isNotUsed()) continue;
                NodeProto.Function fun = np.getFunction();
                if (fun == NodeProto.Function.PIN)
                {
                    pinTotal++;
                    inPalette.add(np);
                } else if (fun == NodeProto.Function.NODE) pureTotal++; else
                {
                    compTotal++;
                    inPalette.add(np);
                }
            }
            if (pinTotal + compTotal == 0) pinTotal = pureTotal;
            menuY = arcTotal + pinTotal + compTotal + 3;
            menuX = 1;
            if (menuY > 40)
            {
                menuY = (menuY+2) / 3;
                menuX = 3;
            } else if (menuY > 20)
            {
                menuY = (menuY+1) / 2;
                menuX = 2;
            }
        }
        Dimension size = TopLevel.getScreenSize();
        entrySize = (int)size.getWidth() / menuX;
        int ysize = (int)(size.getHeight()*0.9) / menuY;
        if (ysize < entrySize) entrySize = ysize;
        size.setSize(entrySize*menuX+1, entrySize*menuY+1);
        User.tool.setCurrentArcProto((ArcProto)tech.getArcs().next());
        //repaint();
        return size;
    }

    private static NodeInst makeNodeInst(NodeProto np, NodeProto.Function func, int angle)
    {
        NodeInst ni = NodeInst.lowLevelAllocate();
        SizeOffset so = np.getProtoSizeOffset();
        Point2D pt = new Point2D.Double((so.getHighXOffset() - so.getLowXOffset()) / 2,
            (so.getHighYOffset() - so.getLowYOffset()) / 2);
        AffineTransform trans = NodeInst.pureRotate(angle, false, false);
        trans.transform(pt, pt);
        ni.lowLevelPopulate(np, pt, np.getDefWidth(), np.getDefHeight(), angle, null);
        np.getTechnology().setPrimitiveFunction(ni, func);
        np.getTechnology().setDefaultOutline(ni);
        return ni;
    }

    /**
     * Method called when the user clicks over an entry in the component menu.
     */
    public void mousePressed(MouseEvent e)
    {
        TechPalette panel = (TechPalette)e.getSource();
        panel.requestFocus();
        Object obj = getObjectUnderCursor(e);
        if (obj instanceof NodeProto || obj instanceof NodeInst)
        {
            JMenuItem menuItem;
            if (obj == Schematics.tech.diodeNode)
            {
                JPopupMenu menu = new JPopupMenu("Diode");
                menu.add(menuItem = new JMenuItem("Normal Diode"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, Schematics.tech.diodeNode));
                menu.add(menuItem = new JMenuItem("Zener Diode"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.diodeNode, NodeProto.Function.DIODEZ, 0)));
                menu.show(panel, e.getX(), e.getY());
                return;
            }
            if (obj == Schematics.tech.capacitorNode)
            {
                JPopupMenu menu = new JPopupMenu("Capacitor");
                menu.add(menuItem = new JMenuItem("Normal Capacitor"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, Schematics.tech.capacitorNode));
                menu.add(menuItem = new JMenuItem("Electrolytic Capacitor"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.capacitorNode, NodeProto.Function.ECAPAC, 0)));
                menu.show(panel, e.getX(), e.getY());
                return;
            }
            if (obj == Schematics.tech.flipflopNode)
            {
                JPopupMenu menu = new JPopupMenu("Flip-flop");
                menu.add(menuItem = new JMenuItem("R-S master/slave"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.flipflopNode, NodeProto.Function.FLIPFLOPRSMS, 0)));
                menu.add(menuItem = new JMenuItem("R-S positive"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.flipflopNode, NodeProto.Function.FLIPFLOPRSP, 0)));
                menu.add(menuItem = new JMenuItem("R-S negative"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.flipflopNode, NodeProto.Function.FLIPFLOPRSN, 0)));
                menu.addSeparator();
                menu.add(menuItem = new JMenuItem("J-K master/slave"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.flipflopNode, NodeProto.Function.FLIPFLOPJKMS, 0)));
                menu.add(menuItem = new JMenuItem("J-K positive"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.flipflopNode, NodeProto.Function.FLIPFLOPJKP, 0)));
                menu.add(menuItem = new JMenuItem("J-K negative"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.flipflopNode, NodeProto.Function.FLIPFLOPJKN, 0)));
                menu.addSeparator();
                menu.add(menuItem = new JMenuItem("D master/slave"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.flipflopNode, NodeProto.Function.FLIPFLOPDMS, 0)));
                menu.add(menuItem = new JMenuItem("D positive"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.flipflopNode, NodeProto.Function.FLIPFLOPDP, 0)));
                menu.add(menuItem = new JMenuItem("D negative"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.flipflopNode, NodeProto.Function.FLIPFLOPDN, 0)));
                menu.addSeparator();
                menu.add(menuItem = new JMenuItem("T master/slave"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.flipflopNode, NodeProto.Function.FLIPFLOPTMS, 0)));
                menu.add(menuItem = new JMenuItem("T positive"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.flipflopNode, NodeProto.Function.FLIPFLOPTP, 0)));
                menu.add(menuItem = new JMenuItem("T negative"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.flipflopNode, NodeProto.Function.FLIPFLOPTN, 0)));
                menu.show(panel, e.getX(), e.getY());
                return;
            }
            if (obj instanceof NodeInst && ((NodeInst)obj).getProto() == Schematics.tech.transistor4Node)
            {
                JPopupMenu menu = new JPopupMenu("4-Port Transistors");
                menu.add(menuItem = new JMenuItem("nMOS 4-port"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4NMOS, 900)));
                menu.add(menuItem = new JMenuItem("PMOS 4-port"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4PMOS, 900)));
                menu.add(menuItem = new JMenuItem("DMOS 4-port"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4DMOS, 900)));
                menu.add(menuItem = new JMenuItem("NPN 4-port"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4NPN, 900)));
                menu.add(menuItem = new JMenuItem("PNP 4-port"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4PNP, 900)));
                menu.add(menuItem = new JMenuItem("DMES 4-port"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4DMES, 900)));
                menu.add(menuItem = new JMenuItem("EMES 4-port"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4EMES, 900)));
                menu.add(menuItem = new JMenuItem("PJFET 4-port"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4PJFET, 900)));
                menu.add(menuItem = new JMenuItem("NJFET 4-port"));
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4NJFET, 900)));
                menu.show(panel, e.getX(), e.getY());
                return;
            }
            if (obj instanceof NodeInst && ((NodeInst)obj).getProto() == Schematics.tech.transistorNode)
            {
                NodeInst ni = (NodeInst)obj;
                if (ni.getFunction() == NodeProto.Function.TRAPNP)
                {
                    JPopupMenu menu = new JPopupMenu("3-Port Transistors");
                    menu.add(menuItem = new JMenuItem("nMOS"));
                    menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRANMOS, 900)));
                    menu.add(menuItem = new JMenuItem("PMOS"));
                    menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRAPMOS, 900)));
                    menu.add(menuItem = new JMenuItem("DMOS"));
                    menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRADMOS, 900)));
                    menu.add(menuItem = new JMenuItem("NPN"));
                    menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRANPN, 900)));
                    menu.add(menuItem = new JMenuItem("PNP"));
                    menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRAPNP, 900)));
                    menu.add(menuItem = new JMenuItem("DMES"));
                    menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRADMES, 900)));
                    menu.add(menuItem = new JMenuItem("EMES"));
                    menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRAEMES, 900)));
                    menu.add(menuItem = new JMenuItem("PJFET"));
                    menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRAPJFET, 900)));
                    menu.add(menuItem = new JMenuItem("NJFET"));
                    menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRANJFET, 900)));
                    menu.show(panel, e.getX(), e.getY());
                    return;
                }
            }
            PaletteFrame.placeInstance(obj, panel, false);
        } else if (obj instanceof PrimitiveArc)
        {
            PrimitiveArc ap = (PrimitiveArc)obj;
            User.tool.setCurrentArcProto(ap);
        } else if (obj instanceof String)
        {
            String msg = (String)obj;
            if (msg.equals("Cell"))
            {
                JPopupMenu cellMenu = new JPopupMenu("Cells");
                List sortedCells = Library.getCurrent().getCellsSortedByName();
                for(Iterator it = sortedCells.iterator(); it.hasNext(); )
                {
                    Cell cell = (Cell)it.next();
                    JMenuItem menuItem = new JMenuItem(cell.describe());
                    menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, cell));
                    cellMenu.add(menuItem);
                }
//					cellMenu.addMouseListener(new MyPopupListener());
//					cellMenu.addPopupMenuListener(new MyPopupListener());
                cellMenu.show(panel, e.getX(), e.getY());
            } else if (msg.equals("Misc."))
            {
                JPopupMenu specialMenu = new JPopupMenu("Miscellaneous");
                JMenuItem menuItem = new JMenuItem("Cell Instance...");
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { CellMenu.cellBrowserCommand(CellBrowser.DoAction.newInstance); } });
                specialMenu.add(menuItem);

                specialMenu.addSeparator();

                menuItem = new JMenuItem("Annotation Text");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, "ART_message"));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Layout Text...");
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeLayoutTextCommand(); } });
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Annular Ring...");
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { AnnularRing.showAnnularRingDialog(); } });
                specialMenu.add(menuItem);

                specialMenu.addSeparator();

                menuItem = new JMenuItem("Cell Center");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, Generic.tech.cellCenterNode));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Essential Bounds");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, Generic.tech.essentialBoundsNode));
                specialMenu.add(menuItem);

                specialMenu.addSeparator();

                menuItem = new JMenuItem("Spice Code");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, "SIM_spice_card"));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Verilog Code");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, "VERILOG_code"));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Verilog Declaration");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, "VERILOG_declaration"));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Simulation Probe");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, Generic.tech.simProbeNode));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("DRC Exclusion");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, Generic.tech.drcNode));
                specialMenu.add(menuItem);

                specialMenu.addSeparator();

                menuItem = new JMenuItem("Invisible Pin");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, Generic.tech.invisiblePinNode));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Universal Pin");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, Generic.tech.universalPinNode));
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Unrouted Pin");
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, Generic.tech.unroutedPinNode));
                specialMenu.add(menuItem);
                specialMenu.show(panel, e.getX(), e.getY());
            } else if (msg.equals("Pure"))
            {
                JPopupMenu pureMenu = new JPopupMenu("Pure");
                for(Iterator it = Technology.getCurrent().getNodes(); it.hasNext(); )
                {
                    PrimitiveNode np = (PrimitiveNode)it.next();
                    if (np.isNotUsed()) continue;
                    if (np.getFunction() != NodeProto.Function.NODE) continue;
                    JMenuItem menuItem = new JMenuItem(np.describe());
                    menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, np));
                    pureMenu.add(menuItem);
                }
                pureMenu.show(panel, e.getX(), e.getY());
            } if (msg.equals("Spice"))
            {
                JPopupMenu cellMenu = new JPopupMenu("Spice");

                String currentSpiceLib = Simulation.getSpicePartsLibrary();
                Library spiceLib = Library.findLibrary(currentSpiceLib);
                if (spiceLib == null)
                {
                    // must read the Spice library from disk
                    URL fileURL = LibFile.getLibFile(currentSpiceLib + ".txt");
                    TechPalette.ReadSpiceLibrary job = new TechPalette.ReadSpiceLibrary(fileURL, cellMenu, panel, e.getX(), e.getY());
                } else
                {
                    for(Iterator it = spiceLib.getCells(); it.hasNext(); )
                    {
                        Cell cell = (Cell)it.next();
                        JMenuItem menuItem = new JMenuItem(cell.getName());
                        menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, cell));
                        cellMenu.add(menuItem);
                    }
                    cellMenu.show(panel, e.getX(), e.getY());
                }
            } if (msg.equals("Export"))
            {
                JPopupMenu specialMenu = new JPopupMenu("Export");
                JMenuItem menuItem = new JMenuItem("Wire");
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeExport("wire"); } });
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Bus");
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeExport("bus"); } });
                specialMenu.add(menuItem);
                menuItem = new JMenuItem("Universal");
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeExport("universal"); } });
                specialMenu.add(menuItem);
                specialMenu.show(panel, e.getX(), e.getY());
            } if (msg.equals("Text"))
            {
                PaletteFrame.placeInstance("ART_message", panel, false);
            }
        }
        repaint();
    }

    private void makeExport(String type)
    {
        if (type.equals("wire")) PaletteFrame.placeInstance(Schematics.tech.wirePinNode, this, true); else
        if (type.equals("bus")) PaletteFrame.placeInstance(Schematics.tech.busPinNode, this, true); else
        if (type.equals("universal")) PaletteFrame.placeInstance(Generic.tech.invisiblePinNode, this, true);
    }

    public void makeLayoutTextCommand()
    {
        LayoutText dialog = new LayoutText(TopLevel.getCurrentJFrame(), true);
        dialog.show();
    }

    public void placeNodeStarted(Object nodeToBePlaced) {
        highlightedNode = nodeToBePlaced;
    }

    public void placeNodeFinished(boolean cancelled) {
        highlightedNode = null;
        repaint();
    }

    /**
     * Class to read a Spice library in a new thread.
     */
    private static class ReadSpiceLibrary extends Job
    {
        URL fileURL;
        JPopupMenu cellMenu;
        TechPalette panel;
        int x, y;
        protected ReadSpiceLibrary(URL fileURL, JPopupMenu cellMenu, TechPalette panel, int x, int y)
        {
            super("Read Spice Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.fileURL = fileURL;
            this.cellMenu = cellMenu;
            this.panel = panel;
            this.x = x;
            this.y = y;
            startJob();
        }

        public boolean doIt()
        {
            Library lib = Input.readLibrary(fileURL, OpenFile.Type.READABLEDUMP);
            Undo.noUndoAllowed();
            if (lib == null) return false;
            for(Iterator it = lib.getCells(); it.hasNext(); )
            {
                Cell cell = (Cell)it.next();
                JMenuItem menuItem = new JMenuItem(cell.getName());
                menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, cell));
                cellMenu.add(menuItem);
            }
            cellMenu.show(panel, x, y);
            return true;
        }
    }

    static class PlacePopupListener implements ActionListener
    {
        TechPalette panel;
        Object obj;

        PlacePopupListener(TechPalette panel, Object obj) { super();  this.panel = panel;   this.obj = obj; }

        public void actionPerformed(ActionEvent evt)
        {
            JMenuItem mi = (JMenuItem)evt.getSource();
            String msg = mi.getText();
            PaletteFrame.placeInstance(obj, panel, false);
        }
    };

    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void keyPressed(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}
    public void mouseDragged(MouseEvent e) {}

    /**
     * Method to figure out which palette entry the cursor is over.
     * @return an Object that is in the selected palette entry.
     */
    private Object getObjectUnderCursor(MouseEvent e)
    {
        TechPalette panel = (TechPalette)e.getSource();
        int x = e.getX() / (entrySize+1);
        int y = menuY - (e.getY() / (entrySize+1)) - 1;
        if (y < 0) y = 0;
        int index = x * menuY + y;
        if (index < 0 || index >= inPalette.size()) return null;
        Object obj = inPalette.get(index);
        return obj;
    }

    /**
     * Method called when the mouse hovers over a palette entry.
     * Updates the status area to indicate what the palette will do.
     */
    public void mouseMoved(MouseEvent e)
    {
        Object obj = getObjectUnderCursor(e);
        if (obj instanceof NodeProto || obj instanceof NodeInst)
        {
            NodeProto np = null;
            if (obj instanceof NodeProto) np = (NodeProto)obj; else
                np = ((NodeInst)obj).getProto();
            StatusBar.setSelectionOverride("CREATE NODE: " + np.describe());
        } else if (obj instanceof PrimitiveArc)
        {
            PrimitiveArc ap = (PrimitiveArc)obj;
            StatusBar.setSelectionOverride("USE ARC: " + ap.describe());
        } else if (obj instanceof String)
        {
            StatusBar.setSelectionOverride(null);
        }
    }

    public void mouseExited(MouseEvent e)
    {
        StatusBar.setSelectionOverride(null);
    }

    public void mouseWheelMoved(MouseWheelEvent e) {}

    public void paint(Graphics g)
    {
        // stop now if not initialized
        if (menuX < 0 || menuY < 0) return;

        // recompute size of an entry from current window size
        Dimension size = getSize();
        int wid = (int)size.getWidth();
        int hei = (int)size.getHeight();
        g.clearRect(0, 0, wid, hei);
        entrySize = Math.min(wid / menuX - 1, hei / menuY - 1);

        // create an EditWindow for rendering nodes and arcs
        EditWindow w = EditWindow.CreateElectricDoc(null, null);
        Undo.removeDatabaseChangeListener(w.getHighlighter());
        w.setScreenSize(new Dimension(entrySize, entrySize));

        // draw the menu entries
        for(int x=0; x<menuX; x++)
        {
            for(int y=0; y<menuY; y++)
            {
                // render the entry into an Image
                int index = x * menuY + y;
                if (index >= inPalette.size()) continue;
                Object toDraw = inPalette.get(index);
                Image img = drawMenuEntry(w, toDraw);

                // put the Image in the proper place
                int imgX = x * (entrySize+1)+1;
                int imgY = (menuY-y-1) * (entrySize+1)+1;
                if (img != null)
                {
                    g.drawImage(img, imgX, imgY, this);
                }

                // highlight if an arc or node
                if (toDraw instanceof PrimitiveArc)
                {
                    g.setColor(Color.RED);
                    g.drawRect(imgX, imgY, entrySize-1, entrySize-1);
                    if (toDraw == User.tool.getCurrentArcProto())
                    {
                        g.drawRect(imgX+1, imgY+1, entrySize-3, entrySize-3);
                        g.drawRect(imgX+2, imgY+2, entrySize-5, entrySize-5);
                    }
                }
                if (toDraw instanceof NodeProto || toDraw instanceof NodeInst)
                {
                    g.setColor(Color.BLUE);
                    g.drawRect(imgX, imgY, entrySize-1, entrySize-1);
                    NodeProto np = null;
                    if (toDraw instanceof NodeProto) np = (NodeProto)toDraw; else
                        np = ((NodeInst)toDraw).getProto();
                    if (toDraw == highlightedNode)
                    {
                        g.drawRect(imgX+1, imgY+1, entrySize-3, entrySize-3);
                        g.drawRect(imgX+2, imgY+2, entrySize-5, entrySize-5);
                    }
                    boolean drawArrow = false;
                    if (toDraw == Schematics.tech.diodeNode || toDraw == Schematics.tech.capacitorNode ||
                        toDraw == Schematics.tech.flipflopNode) drawArrow = true;
                    if (toDraw instanceof NodeInst)
                    {
                        NodeInst ni = (NodeInst)toDraw;
                        if (ni.getFunction() == NodeProto.Function.TRAPNP ||
                            ni.getFunction() == NodeProto.Function.TRA4PNP) drawArrow = true;
                    }
                    if (drawArrow) drawArrow(g, x, y);
                }
                if (toDraw instanceof String)
                {
                    String str = (String)toDraw;
                    g.setColor(new Color(User.getColorBackground()));
                    g.fillRect(imgX, imgY, entrySize, entrySize);
                    g.setColor(new Color(User.getColorText()));
                    g.setFont(new Font("Helvetica", Font.PLAIN, 20));
                    FontMetrics fm = g.getFontMetrics();
                    int strWid = fm.stringWidth(str);
                    int strHeight = fm.getMaxAscent() + fm.getMaxDescent();
                    int xpos = imgX+entrySize/2 - strWid/2;
                    int ypos = imgY+entrySize/2 + strHeight/2 - fm.getMaxDescent();
                    g.drawString(str, xpos, ypos);
                    if (str.equals("Cell") || str.equals("Spice") || str.equals("Misc.") || str.equals("Pure"))
                        drawArrow(g, x, y);
                }
            }
        }

        // show dividing lines
        g.setColor(Color.BLACK);
        for(int i=0; i<=menuX; i++)
        {
            int xPos = (entrySize+1) * i;
            g.drawLine(xPos, 0, xPos, menuY*(entrySize+1));
        }
        for(int i=0; i<=menuY; i++)
        {
            int yPos = (entrySize+1) * i;
            g.drawLine(0, yPos, menuX*(entrySize+1), yPos);
        }
    }

    private void drawArrow(Graphics g, int x, int y)
    {
        int imgX = x * (entrySize+1)+1;
        int imgY = (menuY-y-1) * (entrySize+1)+1;
        int [] arrowX = new int[3];
        int [] arrowY = new int[3];
        arrowX[0] = imgX-2 + entrySize*7/8;
        arrowY[0] = imgY-2 + entrySize;
        arrowX[1] = imgX-2 + entrySize;
        arrowY[1] = imgY-2 + entrySize*7/8;
        arrowX[2] = imgX-2 + entrySize*7/8;
        arrowY[2] = imgY-2 + entrySize*3/4;
        g.setColor(Color.BLACK);
        g.fillPolygon(arrowX, arrowY, 3);
    }

    Image drawNodeInMenu(EditWindow wnd, NodeInst ni)
    {
        // determine scale for rendering
        NodeProto np = ni.getProto();
        double largest = 0;
        NodeProto.Function groupFunction = np.getGroupFunction();
        for(Iterator it = np.getTechnology().getNodes(); it.hasNext(); )
        {
            PrimitiveNode otherNp = (PrimitiveNode)it.next();
            if (otherNp.getGroupFunction() != groupFunction) continue;
            if (otherNp.getDefHeight() > largest) largest = otherNp.getDefHeight();
            if (otherNp.getDefWidth() > largest) largest = otherNp.getDefWidth();
        }

        // for pins, make them the same scale as the arcs
        if (groupFunction == NodeProto.Function.PIN)
        {
            largest = 0;
            for(Iterator it = np.getTechnology().getArcs(); it.hasNext(); )
            {
                PrimitiveArc otherAp = (PrimitiveArc)it.next();
                double wid = otherAp.getDefaultWidth();
                if (wid+8 > largest) largest = wid+8;
            }
        }

        // render it
        double scalex = entrySize/largest * 0.8;
        double scaley = entrySize/largest * 0.8;
        double scale = Math.min(scalex, scaley);
        return wnd.renderNode(ni, scale);
    }

    private final static double menuArcLength = 8;

    Image drawMenuEntry(EditWindow wnd, Object entry)
    {
        // setup graphics for rendering (start at bottom and work up)
        if (entry instanceof NodeInst)
        {
            NodeInst ni = (NodeInst)entry;
            return drawNodeInMenu(wnd, ni);
        }
        if (entry instanceof NodeProto)
        {
            // rendering a node: create the temporary node
            NodeProto np = (NodeProto)entry;
            NodeInst ni = NodeInst.makeDummyInstance(np);
            return drawNodeInMenu(wnd, ni);
        }
        if (entry instanceof PrimitiveArc)
        {
            // rendering an arc: create the temporary arc
            PrimitiveArc ap = (PrimitiveArc)entry;
            ArcInst ai = ArcInst.makeDummyInstance(ap, menuArcLength);

            // determine scale for rendering
            double largest = 0;
            for(Iterator it = ap.getTechnology().getArcs(); it.hasNext(); )
            {
                PrimitiveArc otherAp = (PrimitiveArc)it.next();
                double wid = otherAp.getDefaultWidth();
                if (wid+menuArcLength > largest) largest = wid+menuArcLength;
            }

            // render the arc
            double scalex = entrySize/largest * 0.8;
            double scaley = entrySize/largest * 0.8;
            double scale = Math.min(scalex, scaley);
            return wnd.renderArc(ai, scale);
        }
        return null;
    }

}
