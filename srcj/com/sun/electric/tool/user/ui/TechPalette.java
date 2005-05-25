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

import com.sun.electric.Main;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.SwingExamineTask;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.AnnularRing;
import com.sun.electric.tool.user.dialogs.CellBrowser;
import com.sun.electric.tool.user.dialogs.LayoutText;
import com.sun.electric.tool.user.menus.CellMenu;
import com.sun.electric.tool.user.tecEdit.Info;

import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

public class TechPalette extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener,
        KeyListener, PaletteFrame.PlaceNodeEventListener, ComponentListener, DragGestureListener, DragSourceListener {

    /** the number of palette entries. */				private int menuX = -1, menuY = -1;
    /** the size of a palette entry. */					private int entrySize;
    /** the list of objects in the palette. */			private List inPalette = new ArrayList();
    /** the currently selected Node object. */			private Object highlightedNode;
    /** to collect contacts that must be groups */      private HashMap elementsMap = new HashMap();
    /** cached palette image */                         private Image paletteImage;
    /** if the palette image needs to be redrawn */     private boolean paletteImageStale;
    /** Variables needed for drag-and-drop */			private DragSource dragSource = null;

    TechPalette()
    {
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addComponentListener(this);
        paletteImage = null;
        paletteImageStale = true;

        // initialize drag-and-drop from this palette
        dragSource = DragSource.getDefaultDragSource();
		DragGestureRecognizer dgr = dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, this);
    }

    /**
     * Loads a new technology into the palette. Returns the
     * new desired size of the panel
     * @param tech the technology to load
     * @param curCell the cell in the window associated with this palette
     * @return the preferred size of the new panel
     */
    public Dimension loadForTechnology(Technology tech, Cell curCell)
    {
        inPalette.clear();
        elementsMap.clear();

        if (tech == Schematics.tech)
        {
	        List list = null;

            menuX = 2;
            menuY = 14;
            inPalette.add(Schematics.tech.wire_arc);
            inPalette.add(Schematics.tech.wirePinNode);
            inPalette.add("Spice");
            inPalette.add(Schematics.tech.offpageNode);

//          inPalette.add(Schematics.tech.globalNode);
            list = new ArrayList();
	        list.add(Technology.makeNodeInst(Schematics.tech.globalNode, PrimitiveNode.Function.CONNECT, 0, false, "Global Signal", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.globalPartitionNode, PrimitiveNode.Function.CONNECT, 0, false, "Global Partition", 4.5));
            inPalette.add(list);

            inPalette.add(Schematics.tech.powerNode);
            inPalette.add(Schematics.tech.resistorNode);

	        // Capacitor nodes
	        list = new ArrayList();
	        list.add(Technology.makeNodeInst(Schematics.tech.capacitorNode, PrimitiveNode.Function.CAPAC, 0, false, "Normal Capacitor", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.capacitorNode, PrimitiveNode.Function.ECAPAC, 0, false, "Electrolytic Capacitor", 4.5));
            inPalette.add(list);

	        // 4-port transistors
	        list = new ArrayList();
	        list.add(Technology.makeNodeInst(Schematics.tech.transistor4Node, PrimitiveNode.Function.TRA4NPN, 900, false, "NPN 4-port", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.transistor4Node, PrimitiveNode.Function.TRA4PNP, 900, false, "PNP 4-port", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.transistor4Node, PrimitiveNode.Function.TRA4NMOS, 900, false, "nMOS 4-port", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.transistor4Node, PrimitiveNode.Function.TRA4PMOS, 900, false, "PMOS 4-port", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.transistor4Node, PrimitiveNode.Function.TRA4DMOS, 900, false, "DMOS 4-port", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.transistor4Node, PrimitiveNode.Function.TRA4DMES, 900, false, "DMES 4-port", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.transistor4Node, PrimitiveNode.Function.TRA4EMES, 900, false, "EMES 4-port", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.transistor4Node, PrimitiveNode.Function.TRA4PJFET, 900, false, "PJFET 4-port", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.transistor4Node, PrimitiveNode.Function.TRA4PJFET, 900, false, "NJFET 4-port", 4.5));
	        inPalette.add(list);

	        // 3-port transistors
	        list = new ArrayList();
	        list.add(Technology.makeNodeInst(Schematics.tech.transistorNode, PrimitiveNode.Function.TRANPN, 900, false, "NPN", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.transistorNode, PrimitiveNode.Function.TRAPNP, 900, false, "PNP", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.transistorNode, PrimitiveNode.Function.TRANMOS, 900, false, "nMOS", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.transistorNode, PrimitiveNode.Function.TRAPMOS, 900, false, "PMOS", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.transistorNode, PrimitiveNode.Function.TRADMOS, 900, false, "DMOS", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.transistorNode, PrimitiveNode.Function.TRADMES, 900, false, "DMES", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.transistorNode, PrimitiveNode.Function.TRAEMES, 900, false, "EMES", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.transistorNode, PrimitiveNode.Function.TRAPJFET, 900, false, "PJFET", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.transistorNode, PrimitiveNode.Function.TRANJFET, 900, false, "NJFET", 4.5));
	        inPalette.add(list);

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

	        // Diode nodes
	        list = new ArrayList();
	        list.add(Technology.makeNodeInst(Schematics.tech.diodeNode, PrimitiveNode.Function.DIODE, 0, false, "Normal Diode", 4.5));
	        list.add(Technology.makeNodeInst(Schematics.tech.diodeNode, PrimitiveNode.Function.DIODEZ, 0, false, "Zener Diode", 4.5));
            inPalette.add(list);

	        inPalette.add(Technology.makeNodeInst(Schematics.tech.transistorNode, PrimitiveNode.Function.TRAPMOS, 900, false, null, 4.5));
            inPalette.add(Technology.makeNodeInst(Schematics.tech.transistorNode, PrimitiveNode.Function.TRANMOS, 900, false, null, 4.5));

	        // Flip Flop nodes
            list = new ArrayList();
            inPalette.add(list);
	        List subList = new ArrayList();
	        list.add(subList);
            subList.add(Technology.makeNodeInst(Schematics.tech.flipflopNode, PrimitiveNode.Function.FLIPFLOPRSMS, 0, false, "R-S master/slave", 4.5));
            subList.add(Technology.makeNodeInst(Schematics.tech.flipflopNode, PrimitiveNode.Function.FLIPFLOPRSP, 0, false, "R-S positive", 4.5));
            subList.add(Technology.makeNodeInst(Schematics.tech.flipflopNode, PrimitiveNode.Function.FLIPFLOPRSN, 0, false, "R-S negative", 4.5));
	        list.add(new JPopupMenu.Separator());
	        subList = new ArrayList();
	        list.add(subList);
	        subList.add(Technology.makeNodeInst(Schematics.tech.flipflopNode, PrimitiveNode.Function.FLIPFLOPJKMS, 0, false, "J-K master/slave", 4.5));
            subList.add(Technology.makeNodeInst(Schematics.tech.flipflopNode, PrimitiveNode.Function.FLIPFLOPJKP, 0, false, "J-K positive", 4.5));
	        subList.add(Technology.makeNodeInst(Schematics.tech.flipflopNode, PrimitiveNode.Function.FLIPFLOPJKN, 0, false, "J-K negative", 4.5));
	        list.add(new JPopupMenu.Separator());
	        subList = new ArrayList();
	        list.add(subList);
	        subList.add(Technology.makeNodeInst(Schematics.tech.flipflopNode, PrimitiveNode.Function.FLIPFLOPDMS, 0, false, "D master/slave", 4.5));
            subList.add(Technology.makeNodeInst(Schematics.tech.flipflopNode, PrimitiveNode.Function.FLIPFLOPDP, 0, false, "D positive", 4.5));
	        subList.add(Technology.makeNodeInst(Schematics.tech.flipflopNode, PrimitiveNode.Function.FLIPFLOPDN, 0, false, "D negative", 4.5));
	        list.add(new JPopupMenu.Separator());
	        subList = new ArrayList();
	        list.add(subList);
	        subList.add(Technology.makeNodeInst(Schematics.tech.flipflopNode, PrimitiveNode.Function.FLIPFLOPTMS, 0, false, "T master/slave", 4.5));
            subList.add(Technology.makeNodeInst(Schematics.tech.flipflopNode, PrimitiveNode.Function.FLIPFLOPTP, 0, false, "T positive", 4.5));
	        subList.add(Technology.makeNodeInst(Schematics.tech.flipflopNode, PrimitiveNode.Function.FLIPFLOPTN, 0, false, "T negative", 4.5));

            inPalette.add(Schematics.tech.bufferNode);
            inPalette.add(Schematics.tech.orNode);
            inPalette.add(Schematics.tech.andNode);
        } else if (tech == Artwork.tech)
        {
        	if (curCell != null && curCell.isInTechnologyLibrary())
        	{
        		// special variation of Artwork for technology editing
	            menuX = 1;
	            menuY = 16;
	            inPalette.add("Text");
                NodeInst arc = NodeInst.makeDummyInstance(Artwork.tech.circleNode);
                arc.setArcDegrees(0, Math.PI/4);
	            inPalette.add(arc);
                NodeInst half = NodeInst.makeDummyInstance(Artwork.tech.circleNode);
                half.setArcDegrees(0, Math.PI);
	            inPalette.add(half);
	            inPalette.add(Artwork.tech.filledCircleNode);
	            inPalette.add(Artwork.tech.circleNode);
	            inPalette.add(Artwork.tech.openedThickerPolygonNode);
	            inPalette.add(Artwork.tech.openedDashedPolygonNode);
	            inPalette.add(Artwork.tech.openedDottedPolygonNode);
	            inPalette.add(Artwork.tech.openedPolygonNode);
	            inPalette.add(Technology.makeNodeInst(Artwork.tech.closedPolygonNode, PrimitiveNode.Function.ART, 0, false, null, 4.5));
	            inPalette.add(Technology.makeNodeInst(Artwork.tech.filledPolygonNode, PrimitiveNode.Function.ART, 0, false, null, 4.5));
	            inPalette.add(Artwork.tech.boxNode);
	            inPalette.add(Artwork.tech.crossedBoxNode);
	            inPalette.add(Artwork.tech.filledBoxNode);
	            inPalette.add("High");
	            inPalette.add("Port");
        	} else
        	{
        		// regular artwork menu
	            menuX = 2;
	            menuY = 12;
	            inPalette.add(Artwork.tech.solidArc);
	            inPalette.add(Artwork.tech.thickerArc);
	            inPalette.add("Cell");
	            inPalette.add(Artwork.tech.openedPolygonNode);
	            inPalette.add(Artwork.tech.openedThickerPolygonNode);
	            inPalette.add(Artwork.tech.filledTriangleNode);
	            inPalette.add(Artwork.tech.filledBoxNode);
	            inPalette.add(Technology.makeNodeInst(Artwork.tech.filledPolygonNode, PrimitiveNode.Function.ART, 0, false, null, 4.5));
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
	            inPalette.add(Technology.makeNodeInst(Artwork.tech.closedPolygonNode, PrimitiveNode.Function.ART, 0, false, null, 4.5));
	            inPalette.add(Artwork.tech.circleNode);
	            inPalette.add("Export");
	            inPalette.add(Artwork.tech.arrowNode);
	            inPalette.add(Technology.makeNodeInst(Artwork.tech.splineNode, PrimitiveNode.Function.ART, 0, false, null, 4.5));
        	}
        } else
        {
            int pinTotal = 0, pureTotal = 0, compTotal = 0, arcTotal = 0;
            ArcProto firstHighlightedArc = null;
            highlightedNode = null;
            List arcList = new ArrayList();
            List contactList = new ArrayList();
            List groupContactList = new ArrayList();
            List pinList = new ArrayList();
            List transList = new ArrayList();
            List wellList = new ArrayList();

            for(Iterator it = tech.getArcs(); it.hasNext(); )
            {
                PrimitiveArc ap = (PrimitiveArc)it.next();
                if (ap.isNotUsed()) continue;
                PrimitiveNode np = ap.findPinProto();
                if (np != null && np.isNotUsed()) continue;
                if (firstHighlightedArc == null) firstHighlightedArc = ap;
                arcTotal++;
                arcList.add(ap);
                inPalette.add(ap);
            }

            inPalette.add("Cell");
            inPalette.add("Misc.");
            inPalette.add("Pure");

            for(Iterator it = tech.getNodes(); it.hasNext(); )
            {
                PrimitiveNode np = (PrimitiveNode)it.next();
                if (np.isNotUsed()) continue;
                PrimitiveNode.Function fun = np.getFunction();

                if (fun == PrimitiveNode.Function.PIN)
                {
                    pinTotal++;
                    inPalette.add(np);
                    pinList.add(np);
                } else if (fun == PrimitiveNode.Function.NODE)
	                pureTotal++;
                else
                {
                    boolean found = false;
	                Object toAdd = np;
                    List list = null;
                    Object map = null;
	                if (fun == PrimitiveNode.Function.TRANMOS || fun == PrimitiveNode.Function.TRAPMOS)
                    {
                        map = fun;
                        transList.add(map);
                    } else if (fun == PrimitiveNode.Function.CONTACT)
                    {
                        if (np.isGroupNode())
                        {
                            map = np.getLayers()[2].getLayer(); // vias as mapping
                            if (!np.isSpecialNode()) groupContactList.add(map);
                        } else
                            contactList.add(np);
	                // Trick to get "well" in well contacts
                    } else if (fun == PrimitiveNode.Function.SUBSTRATE || fun == PrimitiveNode.Function.WELL)
                    {
	                    toAdd = Technology.makeNodeInst(np, fun, 0, true, "Well", 4.5);
                        if (np.isGroupNode())
                        {
                            map = fun;
                            if (!np.isSpecialNode())
                                wellList.add(map);
                        } else
                           wellList.add(toAdd);
                    }
                    if (map != null)
                    {
                        list = (List)elementsMap.get(map);
                        if (list == null)
                        {
                            list = new ArrayList();
                            elementsMap.put(map, list);
                            inPalette.add(list);
                        }
                        list.add(toAdd);
                        found = true;
                    }
	                // Leaving standard transistors or contact
	                if (!np.isSpecialNode())
	                {
                        compTotal++;
                        if (!found) inPalette.add(toAdd);
	                }
                }
            }
            // Sorting list elements and leaving !isSpecialNode() as default
            for (Iterator it = elementsMap.keySet().iterator(); it.hasNext(); )
            {
                Object map = it.next();
                List list = (List)elementsMap.get(map);
                // Only for more than 1
                if (list.size() > 1)
                {
                    Object obj = list.get(0);
                    PrimitiveNode np = null;

                    // Contact and transistor cases
                    if (obj instanceof PrimitiveNode)
                        np = (PrimitiveNode)obj;
                    else if (obj instanceof NodeInst)
                        np = (PrimitiveNode)((NodeInst)obj).getProto();
                   // Not default -> swap
                   if (np != null && np.isSpecialNode()) Collections.swap(list, 0, 1);
                }
            }
            if (pinTotal + compTotal == 0) pinTotal = pureTotal;
            menuY = arcTotal + pinTotal + compTotal + 3;
            menuX = 1;
            if (menuY > 36)
            {
                menuY = (menuY+3) / 4;
                menuX = 4;
            } else if (menuY > 24)
            {
                menuY = (menuY+2) / 3;
                menuX = 3;
            } else if (menuY > 12)
            {
                menuY = (menuY+1) / 2;
                menuX = 2;
            }

            Object[][] paletteMatrix = tech.getNodesGrouped();

            if (paletteMatrix != null)
            {
            menuX = paletteMatrix[0].length;
            menuY = paletteMatrix.length;
            inPalette.clear();
            for (int i = 0; i < menuX; i++)
            {
                for (int j = 0; j < menuY; j++)
                {
                    Object item = (paletteMatrix[j] == null) ? null : paletteMatrix[j][i];
                    inPalette.add(item);
                }
            }
            }
        }
        Dimension size = TopLevel.getScreenSize();
        entrySize = (int)size.getWidth() / menuX;
        int ysize = (int)(size.getHeight()*0.9) / menuY;
        if (ysize < entrySize) entrySize = ysize;
        size.setSize(entrySize*menuX+1, entrySize*menuY+1);
		User.getUserTool().setCurrentArcProto((ArcProto)tech.getArcs().next());
        //repaint();
        synchronized(this) { paletteImageStale = true; }

        return size;
    }

    /**
     * Method to compose item name depending on object class
     */
    private static String getItemName(Object item, boolean getVarName)
    {
        if (item instanceof PrimitiveNode)
        {
            PrimitiveNode np = (PrimitiveNode)item;
            return (np.getName());
        }
        else if (item instanceof NodeInst)
        {
            NodeInst ni = (NodeInst)item;
            Variable var = ni.getVar(Technology.TECH_TMPVAR);
            if (getVarName && var != null && !var.isDisplay())
            {
                return (var.getObject().toString());
            }
            else // At least case for well contacts
            {
               return (ni.getProto().getName());
            }
        }
        else if (item instanceof PrimitiveArc)
        {
            PrimitiveArc ap = (PrimitiveArc)item;
            return (ap.getName());
        }
        return ("");
    }

    public void mousePressed(MouseEvent e) {}

    /**
     * Method called when the user clicks over an entry in the component menu.
     */
    public void mouseReleased(MouseEvent e)
    {
        TechPalette panel = (TechPalette)e.getSource();
        panel.requestFocus();
        Object obj = getObjectUnderCursor(e.getX(), e.getY());
        JMenuItem menuItem;

        if (obj instanceof NodeProto || obj instanceof NodeInst || obj instanceof PrimitiveArc || obj instanceof List)
        {
            if (obj instanceof List)
            {
                List list = (List)obj;
	            // Getting first element
                obj = list.get(0);
	            if (obj instanceof List) obj = ((List)obj).get(0);
                if (list != null && list.size() > 1 && isCursorOnCorner(e))
				{
                    // Careful with this name
					JPopupMenu menu = new JPopupMenu(getItemName(obj, true));

					for (Iterator it = list.iterator(); it.hasNext();)
					{
                        Object item = it.next();
						if (item instanceof JSeparator)
							menu.add((JSeparator)item);
						else if (item instanceof List)
						{
							List subList = (List)item;
							for (Iterator listIter = subList.iterator(); listIter.hasNext();)
							{
								Object subItem = listIter.next();
								menu.add(menuItem = new JMenuItem(getItemName(subItem, true)));
                                menuItem.addActionListener(new TechPalette.PlacePopupListListener(panel, subItem, list, subList));
							}
						}
                        else
						{
							menu.add(menuItem = new JMenuItem(getItemName(item, true)));
                            menuItem.addActionListener(new TechPalette.PlacePopupListListener(panel, item, list, null));
						}
					}
					menu.show(panel, e.getX(), e.getY());
					return;
				}
            }
            if (obj instanceof PrimitiveArc)
				User.getUserTool().setCurrentArcProto((PrimitiveArc)obj);
            else
                PaletteFrame.placeInstance(obj, panel, false);
        } else if (obj instanceof String)
        {
            String msg = (String)obj;
            if (msg.equals("Cell"))
            {
                JPopupMenu cellMenu = new JPopupMenu("Cells");
                for(Iterator it = Library.getCurrent().getCells(); it.hasNext(); )
                {
                    Cell cell = (Cell)it.next();
                    menuItem = new JMenuItem(cell.describe());
                    menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, cell));
                    cellMenu.add(menuItem);
                }
//					cellMenu.addMouseListener(new MyPopupListener());
//					cellMenu.addPopupMenuListener(new MyPopupListener());
                cellMenu.show(panel, e.getX(), e.getY());
            } else if (msg.equals("Misc."))
            {
                JPopupMenu specialMenu = new JPopupMenu("Miscellaneous");
                menuItem = new JMenuItem("Cell Instance...");
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
                for(Iterator it = Technology.getCurrent().getNodesSortedByName().iterator(); it.hasNext(); )
                {
                    PrimitiveNode np = (PrimitiveNode)it.next();
                    if (np.isNotUsed()) continue;
                    if (np.getFunction() != PrimitiveNode.Function.NODE) continue;
                    Technology.NodeLayer layer = np.getLayers()[0];
                    if (layer.getLayer().getFunction().isContact()) continue;
                    menuItem = new JMenuItem(np.describe());
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
                        menuItem = new JMenuItem(cell.getName());
                        menuItem.addActionListener(new TechPalette.PlacePopupListener(panel, cell));
                        cellMenu.add(menuItem);
                    }
                    cellMenu.show(panel, e.getX(), e.getY());
                }
            } if (msg.equals("Export"))
            {
                JPopupMenu specialMenu = new JPopupMenu("Export");
                menuItem = new JMenuItem("Wire");
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
            	// place a piece of text
                PaletteFrame.placeInstance("ART_message", panel, false);
            } if (msg.equals("High"))
            {
            	// place a technology-edit highlight box
            	NodeInst ni = NodeInst.makeDummyInstance(Artwork.tech.boxNode);
            	ni.newVar(Info.LAYER_KEY, null);
                PaletteFrame.placeInstance(ni, panel, false);
            } if (msg.equals("Port"))
            {
            	// place a technology-edit port
                PaletteFrame.placeInstance(Generic.tech.portNode, panel, false);
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
        if (!Main.BATCHMODE) dialog.setVisible(true);
    }

    public void placeNodeStarted(Object nodeToBePlaced) {
        highlightedNode = nodeToBePlaced;
    }

    public void placeNodeFinished(boolean cancelled) {
        highlightedNode = null;
        repaint();
    }
    
    // ************************************************ DRAG-AND-DROP CODE ************************************************

    public void dragGestureRecognized(DragGestureEvent e)
	{
	    Object obj = getObjectUnderCursor(e.getDragOrigin().x, e.getDragOrigin().y);

		// make a Transferable Object
		EditWindow.NodeProtoTransferable transferable = new EditWindow.NodeProtoTransferable(obj);
	
		// begin the drag
		dragSource.startDrag(e, DragSource.DefaultLinkDrop, transferable, this);
	}

	public void dragEnter(DragSourceDragEvent e) {}

	public void dragOver(DragSourceDragEvent e) {}

	public void dragExit(DragSourceEvent e) {}

	public void dragDropEnd(DragSourceDropEvent e) {}

	public void dropActionChanged (DragSourceDragEvent e) {}

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
            super("Read Spice Library", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.fileURL = fileURL;
            this.cellMenu = cellMenu;
            this.panel = panel;
            this.x = x;
            this.y = y;
            startJob();
        }

        public boolean doIt()
        {
            Library lib = Input.readLibrary(fileURL, null, FileType.READABLEDUMP, false);
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
            PaletteFrame.placeInstance(obj, panel, false);
        }
    };

    static class PlacePopupListListener extends PlacePopupListener
            implements ActionListener
    {
        List list;
	    List subList;

        PlacePopupListListener(TechPalette panel, Object obj, List list, List subList)
        {
	        super(panel, obj);
	        this.list = list;
	        this.subList = subList;
        }

        public void actionPerformed(ActionEvent evt)
        {
            if (obj instanceof PrimitiveArc)
				User.getUserTool().setCurrentArcProto((PrimitiveArc)obj);
            else
                PaletteFrame.placeInstance(obj, panel, false);
            // No first element -> make it default
	        if (subList == null)
		        Collections.swap(list, 0, list.indexOf(obj));
	        else
	        {
		        Collections.swap(list, 0, list.indexOf(subList));
		        Collections.swap(subList, 0, subList.indexOf(obj));
	        }
            synchronized(this) { panel.paletteImageStale = true; }
        }
    };

    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void keyPressed(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}
    public void mouseDragged(MouseEvent e) {}

    /**
     * Method to figure out which palette entry the cursor is over.
     * @return an Object that is in the selected palette entry.
     */
    private Object getObjectUnderCursor(int xp, int yp)
    {
        int x = xp / (entrySize+1);
        int y = menuY - (yp / (entrySize+1)) - 1;
        if (y < 0) y = 0;
        int index = x * menuY + y;
        if (index < 0 || index >= inPalette.size()) return null;
        Object obj = inPalette.get(index);
        return obj;
    }

	/**
     * Method to figure out which palette entry the cursor is over.
     * @return true if mouse is over bottom right corner
     */
    private boolean isCursorOnCorner(MouseEvent e)
    {
		int entryS = (entrySize+1);
        int x = e.getX() / (entryS);
        int y = menuY - (e.getY() / (entryS)) - 1;
        if (y < 0) y = 0;
		double deltaX = (e.getX() - x*entryS)/(double)entryS;
		double deltaY = (e.getY() - (menuY-y-1)*entryS)/(double)entryS;
		return (deltaX > 0.75 && deltaY > 0.75);
    }

    /**
     * Method called when the mouse hovers over a palette entry.
     * Updates the status area to indicate what the palette will do.
     */
    public void mouseMoved(MouseEvent e)
    {
        Object obj = getObjectUnderCursor(e.getX(), e.getY());
        if (obj instanceof List)
        {
            obj = ((List)obj).get(0);
	        if (obj instanceof List) obj = ((List)obj).get(0);
        }
        if (obj instanceof PrimitiveNode)
        {
           StatusBar.setSelectionOverride("CREATE NODE: " + ((PrimitiveNode)obj).describe());
        }
        else if (obj instanceof NodeInst)
        {
           StatusBar.setSelectionOverride("CREATE NODE: " + ((NodeInst)obj).describe());
        }
        else if (obj instanceof NodeProto)
        {
            StatusBar.setSelectionOverride("CREATE NODE: " + ((NodeProto)obj).describe());
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

    public void componentHidden(ComponentEvent e) {}
    public void componentMoved(ComponentEvent e) {}
    public void componentShown(ComponentEvent e) {}
    public void componentResized(ComponentEvent e) {
        synchronized(this) { paletteImageStale = true; }
    }

    public void paint(Graphics g)
    {
        // stop now if not initialized
        if (menuX < 0 || menuY < 0) return;

        // recompute size of an entry from current window size
        Dimension size = getSize();
        int wid = (int)size.getWidth();
        int hei = (int)size.getHeight();
        entrySize = Math.min(wid / menuX - 1, hei / menuY - 1);

        synchronized(this) {
            if (paletteImageStale || paletteImage == null) {
                PaintPalette task = new PaintPalette(menuX, menuY, entrySize, inPalette, this, size);
                if (!task.runImmediately()) {
                    Job.invokeExamineLater(task, PaintPalette.class);
                    // draw old palette image for the time being
                    if (paletteImage != null) g.drawImage(paletteImage, 0, 0, this);
                    return;
                }
            }
            // draw current palette image
            g.drawImage(paletteImage, 0, 0, this);
        }

        // highlight node that user selected
        if (highlightedNode != null) {
            // draw highlights around cell with highlighted node
            int index = inPalette.indexOf(highlightedNode);
            if (index >= 0) {
                // put the Image in the proper place
                int x = (int)(index/menuY);
                int y = index % menuY;
                int imgX = x * (entrySize+1)+1;
                int imgY = (menuY-y-1) * (entrySize+1)+1;
                g.setColor(Color.BLUE);
                g.drawRect(imgX+1, imgY+1, entrySize-3, entrySize-3);
                g.drawRect(imgX+2, imgY+2, entrySize-5, entrySize-5);
            }
        }

        // highlight current arc
        Object arcObj = User.getUserTool().getCurrentArcProto();
        int index = -1;
        for (int i = 0; i < inPalette.size(); i++)
        {
            Object obj = inPalette.get(i);
            if (obj == null) continue;
            if (obj instanceof List) obj = ((List)obj).get(0);
            if (obj == arcObj)
            {
                index = i;
                break;
            }
        }
        //int index = inPalette.indexOf(User.getUserTool().getCurrentArcProto());

        if (index >= 0) {
            int x = (int)(index/menuY);
            int y = index % menuY;
            int imgX = x * (entrySize+1)+1;
            int imgY = (menuY-y-1) * (entrySize+1)+1;
            g.setColor(Color.RED);
            g.drawRect(imgX+1, imgY+1, entrySize-3, entrySize-3);
            g.drawRect(imgX+2, imgY+2, entrySize-5, entrySize-5);
        }
    }

    private static class PaintPalette extends SwingExamineTask {
        private int menuX;
        private int menuY;
        private int entrySize;
        private List inPalette;
        private TechPalette palette;
        private Dimension size;
        private Image image;
        private Rectangle entryRect;

        private PaintPalette(int menuX, int menuY, int entrySize, List inPalette, TechPalette palette, Dimension size) {
            this.menuX = menuX;
            this.menuY = menuY;
            this.entrySize = entrySize;
            this.inPalette = inPalette;
            this.palette = palette;
            this.size = size;
            image = new BufferedImage((int)size.getWidth(), (int)size.getHeight(), BufferedImage.TYPE_INT_RGB);
            entryRect = new Rectangle(new Dimension(entrySize-2, entrySize-2));
        }

        protected boolean doIt(boolean immediate) {
            // draw the menu entries
            // create an EditWindow for rendering nodes and arcs
            // ?if (wnd != null) wnd.finished();
            EditWindow wnd = EditWindow.CreateElectricDoc(null, null);
            Undo.removeDatabaseChangeListener(wnd.getHighlighter());
            wnd.setScreenSize(new Dimension(entrySize, entrySize));

            Graphics2D g = (Graphics2D)image.getGraphics();
            g.setBackground(new Color(User.getColorBackground()));
            int wid = (int)size.getWidth();
            int hei = (int)size.getHeight();
            g.clearRect(0, 0, wid, hei);

            for(int x=0; x<menuX; x++)
            {
                for(int y=0; y<menuY; y++)
                {
                    // render the entry into an Image
                    int index = x * menuY + y;
                    if (index >= inPalette.size()) continue;
                    Object toDraw = inPalette.get(index);
                    boolean drawArrow = false;

                    if (toDraw instanceof List)
                    {
                        List list = ((List)toDraw);
                        toDraw = list.get(0);
	                    if (toDraw instanceof List) toDraw = ((List)toDraw).get(0);
                        drawArrow = list.size() > 1;
                    }

                    Image img = drawMenuEntry(wnd, toDraw);

                    // put the Image in the proper place
                    int imgX = x * (entrySize+1)+1;
                    int imgY = (menuY-y-1) * (entrySize+1)+1;

                    // Draw at the end
                    if (img != null)
                    {
                        g.drawImage(img, imgX, imgY, null);
                    }

                    // highlight if an arc or node
                    if (toDraw instanceof PrimitiveArc)
                    {
                        g.setColor(Color.RED);
                        g.drawRect(imgX, imgY, entrySize-1, entrySize-1);
                    }
                    if (toDraw instanceof NodeProto || toDraw instanceof NodeInst)
                    {
                        if (toDraw == Schematics.tech.diodeNode || toDraw == Schematics.tech.capacitorNode ||
                            toDraw == Schematics.tech.flipflopNode) drawArrow = true;
                        if (toDraw instanceof NodeInst)
                        {
                            NodeInst ni = (NodeInst)toDraw;
                            if (ni.getFunction() == PrimitiveNode.Function.TRAPNP ||
                                ni.getFunction() == PrimitiveNode.Function.TRA4PNP)
                                drawArrow = true;
                        }

                        g.setColor(Color.BLUE);
                        g.drawRect(imgX, imgY, entrySize-1, entrySize-1);

                        if (drawArrow) drawArrow(g, x, y);
                    }
                    if (toDraw instanceof String)
                    {
                        String str = (String)toDraw;
                        if (str.equals("Cell") || str.equals("Spice") || str.equals("Misc.") || str.equals("Pure"))
                            drawArrow = true;
                    }
                    if (drawArrow) drawArrow(g, x, y);
                }
            }

            // show dividing lines
            g.setColor(new Color(User.getColorGrid()));
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
            wnd.finished();
            synchronized(palette) {
                palette.paletteImage = image;
                palette.paletteImageStale = false;
            }
            if (!immediate) palette.repaint();
            return true;
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
            g.setColor(new Color(User.getColorGrid()));
            g.fillPolygon(arrowX, arrowY, 3);
        }

        Image drawNodeInMenu(EditWindow wnd, NodeInst ni)
        {
            // determine scale for rendering
            PrimitiveNode np = (PrimitiveNode)ni.getProto();
            double largest = 0;
            PrimitiveNode.Function groupFunction = np.getGroupFunction();
            for(Iterator it = np.getTechnology().getNodes(); it.hasNext(); )
            {
                PrimitiveNode otherNp = (PrimitiveNode)it.next();
                if (otherNp.getGroupFunction() != groupFunction) continue;
                if (otherNp.isSpecialNode()) continue;
                if (otherNp.getDefHeight() > largest) largest = otherNp.getDefHeight();
                if (otherNp.getDefWidth() > largest) largest = otherNp.getDefWidth();
            }

            // for pins, make them the same scale as the arcs
            if (groupFunction == PrimitiveNode.Function.PIN)
            {
                largest = 0;
                for(Iterator it = np.getTechnology().getArcs(); it.hasNext(); )
                {
                    PrimitiveArc otherAp = (PrimitiveArc)it.next();
                    if (otherAp.isSpecialArc()) continue; // ignore arc for sizing
                    double wid = otherAp.getDefaultWidth();
                    if (wid+8 > largest) largest = wid+8;
                }
            }

            // render it
            double scalex = entrySize/largest * 0.8;
            double scaley = entrySize/largest * 0.8;
            double scale = Math.min(scalex, scaley);
            return wnd.renderNode(ni, scale, true);
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
                    if (otherAp.isSpecialArc()) continue;  // these are not drawn in palette
                    double wid = otherAp.getDefaultWidth();
                    if (wid+menuArcLength > largest) largest = wid+menuArcLength;
                }

                // render the arc
                double scalex = entrySize/largest * 0.8;
                double scaley = entrySize/largest * 0.8;
                double scale = Math.min(scalex, scaley);
                return wnd.renderArc(ai, scale, true);
            }
            if (entry instanceof String)
            {
                return wnd.renderText((String)entry, 0.8, entryRect);
            }
            return null;
        }
    }

}
