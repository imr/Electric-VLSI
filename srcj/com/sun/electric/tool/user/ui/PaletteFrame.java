/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PaletteFrame.java
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

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.MenuCommands;
import com.sun.electric.tool.user.dialogs.LayoutText;
import com.sun.electric.tool.user.dialogs.CellBrowser;
import com.sun.electric.tool.user.dialogs.OpenFile;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.EventListener;
import javax.swing.JInternalFrame;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.ImageIcon;

/**
 * This class defines a palette window for component selection.
 */
public class PaletteFrame
{
	/** the palette window frame. */					private Container container;
	/** the edit window part */							private PalettePanel panel;
	/** the number of palette entries. */				private int menuX = -1, menuY = -1;
	/** the size of a palette entry. */					private int entrySize;
	/** the list of objects in the palette. */			private List inPalette;
	/** the currently selected Node in the palette. */	private NodeProto highlightedNode;
	/** the popup that selects technologies. */			private JComboBox selector;

	// constructor, never called
	private PaletteFrame() {}

	/**
	 * Method to create a new window on the screen that displays the component menu.
	 * @return the PaletteFrame that shows the component menu.
	 */
	public static PaletteFrame newInstance()
	{
		PaletteFrame palette = new PaletteFrame();

		// initialize the frame
		Dimension screenSize = TopLevel.getScreenSize();
		int screenHeight = (int)screenSize.getHeight();
		Dimension frameSize = new Dimension(100, (int)(screenHeight*0.9)); // multiply by 0.9 to make room for taskbar
		if (TopLevel.isMDIMode())
		{
			JInternalFrame jInternalFrame = new JInternalFrame("Components", true, false, false, false);
			palette.container = jInternalFrame;
			jInternalFrame.setSize(frameSize);
			jInternalFrame.setLocation(0, 0);
			jInternalFrame.setAutoscrolls(true);
			jInternalFrame.setFrameIcon(new ImageIcon(palette.getClass().getResource("IconElectric.gif")));
		} else
		{
			JFrame jFrame = new JFrame("Components");
			palette.container = jFrame;
			jFrame.setSize(frameSize);
			jFrame.setLocation(0, 0);
			jFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		}

		// create a paletteWindow and a selector combobox
		palette.panel = new PalettePanel(palette);
        palette.panel.setFocusable(true);
		palette.selector = new JComboBox();
		List techList = Technology.getTechnologiesSortedByName();
		for(Iterator it = techList.iterator(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			if (tech == Generic.tech) continue;
			palette.selector.addItem(tech.getTechName());
		}
		palette.selector.setSelectedItem(Technology.getCurrent().getTechName());
		palette.selector.addActionListener(new TechnologyPopupActionListener(palette));

		if (TopLevel.isMDIMode())
		{
			((JInternalFrame)palette.container).getContentPane().setLayout(new java.awt.BorderLayout());
			((JInternalFrame)palette.container).getContentPane().add(palette.selector, BorderLayout.NORTH);
			((JInternalFrame)palette.container).getContentPane().add(palette.panel, BorderLayout.CENTER);
			((JInternalFrame)palette.container).show();
			TopLevel.addToDesktop((JInternalFrame)palette.container);
			((JInternalFrame)palette.container).moveToFront();
		} else
		{
			((JFrame)palette.container).getContentPane().setLayout(new java.awt.BorderLayout());
			((JFrame)palette.container).getContentPane().add(palette.selector, BorderLayout.NORTH);
			((JFrame)palette.container).getContentPane().add(palette.panel, BorderLayout.CENTER);
			((JFrame)palette.container).show();
		}
		return palette;
	}

	public PalettePanel getPanel() { return panel; }

	public Rectangle getPaletteLocation()
	{
		return container.getBounds();
	}

	public void arcProtoChanged()
	{
		panel.repaint();
	}

	/**
	 * Method to automatically switch to the proper technology for a Cell.
	 * @param cell the cell being displayed.
	 * If technology auto-switching is on, make sure the right technology is displayed
	 * for the Cell.
	 */
	public static void autoTechnologySwitch(Cell cell)
	{
		Technology tech = cell.getTechnology();
		if (tech != null && tech != Technology.getCurrent())
		{
			if (User.isAutoTechnologySwitch())
			{
				tech.setCurrent();
				TopLevel.getPaletteFrame().selector.setSelectedItem(tech.getTechName());
			}
		}
	}

	public void loadForTechnology()
	{
		Technology tech = Technology.getCurrent();
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
			inPalette.add(makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRADMES, 900));
			inPalette.add(makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRANPN, 900));
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
			inPalette.add(makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRAPJFET, 900));
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
		container.setSize(size);
		User.tool.setCurrentArcProto((ArcProto)tech.getArcs().next());
		panel.repaint();
	}

	private static NodeInst makeNodeInst(NodeProto np, NodeProto.Function func, int angle)
	{
		NodeInst ni = NodeInst.lowLevelAllocate();
		SizeOffset so = np.getSizeOffset();
		Point2D pt = new Point2D.Double((so.getHighXOffset() - so.getLowXOffset()) / 2,
			(so.getHighYOffset() - so.getLowYOffset()) / 2);
		AffineTransform trans = NodeInst.pureRotate(angle, false, false);
		trans.transform(pt, pt);
		ni.lowLevelPopulate(np, pt, np.getDefWidth(), np.getDefHeight(), angle, null);
		np.getTechnology().setPrimitiveFunction(ni, func);
		np.getTechnology().setDefaultOutline(ni);
		return ni;
	}

	// The class that watches changes to the technology popup at the bottom of the component menu
	static class TechnologyPopupActionListener implements ActionListener
	{
		PaletteFrame palette;

		TechnologyPopupActionListener(PaletteFrame palette) { this.palette = palette; }

		public void actionPerformed(ActionEvent evt)
		{
			// the popup of libraies changed
			String techName = (String)palette.selector.getSelectedItem();
			Technology  tech = Technology.findTechnology(techName);
			if (tech != null)
			{
				tech.setCurrent();
				palette.loadForTechnology();					
			}
		}
	}

	/**
	 * Class to define the JPanel in the component menu.
	 */
	public static class PalettePanel extends JPanel
		implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
	{
		PaletteFrame frame;

		PalettePanel(PaletteFrame frame)
		{
			this.frame = frame;
			addKeyListener(this);
			addMouseListener(this);
			addMouseMotionListener(this);
			addMouseWheelListener(this);
		}

		PaletteFrame getFrame() { return frame; }

		/**
		 * Method called when the user clicks over an entry in the component menu.
		 */
		public void mousePressed(MouseEvent e)
		{
			PalettePanel panel = (PalettePanel)e.getSource();
            panel.requestFocus();
			Object obj = getObjectUnderCursor(e);
			if (obj instanceof NodeProto || obj instanceof NodeInst)
			{
				JMenuItem menuItem;
				if (obj == Schematics.tech.diodeNode)
				{
					JPopupMenu menu = new JPopupMenu("Diode");
					menu.add(menuItem = new JMenuItem("Normal Diode"));
					menuItem.addActionListener(new PlacePopupListener(panel, Schematics.tech.diodeNode));
					menu.add(menuItem = new JMenuItem("Zener Diode"));
					menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.diodeNode, NodeProto.Function.DIODEZ, 0)));
					menu.show(panel, e.getX(), e.getY());
					return;
				}
				if (obj == Schematics.tech.capacitorNode)
				{
					JPopupMenu menu = new JPopupMenu("Capacitor");
					menu.add(menuItem = new JMenuItem("Normal Capacitor"));
					menuItem.addActionListener(new PlacePopupListener(panel, Schematics.tech.capacitorNode));
					menu.add(menuItem = new JMenuItem("Electrolytic Capacitor"));
					menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.capacitorNode, NodeProto.Function.ECAPAC, 0)));
					menu.show(panel, e.getX(), e.getY());
					return;
				}
				if (obj == Schematics.tech.flipflopNode)
				{
					JPopupMenu menu = new JPopupMenu("Flip-flop");
					menu.add(menuItem = new JMenuItem("R-S"));
					menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.flipflopNode, NodeProto.Function.FLIPFLOP, 0)));
					menu.add(menuItem = new JMenuItem("J-K"));
					menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.flipflopNode, NodeProto.Function.FLIPFLOP, 0)));
					menu.add(menuItem = new JMenuItem("D"));
					menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.flipflopNode, NodeProto.Function.FLIPFLOP, 0)));
					menu.add(menuItem = new JMenuItem("T"));
					menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.flipflopNode, NodeProto.Function.FLIPFLOP, 0)));
					menu.show(panel, e.getX(), e.getY());
					return;
				}
				if (obj instanceof NodeInst && ((NodeInst)obj).getProto() == Schematics.tech.transistorNode)
				{
					NodeInst ni = (NodeInst)obj;
					if (ni.getFunction() == NodeProto.Function.TRANMOS)
					{
						JPopupMenu menu = new JPopupMenu("MOS");
						menu.add(menuItem = new JMenuItem("nMOS"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRANMOS, 900)));
						menu.add(menuItem = new JMenuItem("PMOS"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRAPMOS, 900)));
						menu.add(menuItem = new JMenuItem("DMOS"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRADMOS, 900)));
						menu.add(menuItem = new JMenuItem("nMOS 4-port"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4NMOS, 900)));
						menu.add(menuItem = new JMenuItem("PMOS 4-port"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4PMOS, 900)));
						menu.add(menuItem = new JMenuItem("DMOS 4-port"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4DMOS, 900)));
						menu.show(panel, e.getX(), e.getY());
						return;
					}
					if (ni.getFunction() == NodeProto.Function.TRANPN)
					{
						JPopupMenu menu = new JPopupMenu("Bipolar");
						menu.add(menuItem = new JMenuItem("NPN"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRANPN, 900)));
						menu.add(menuItem = new JMenuItem("PNP"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRAPNP, 900)));
						menu.add(menuItem = new JMenuItem("NPN 4-port"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4NPN, 900)));
						menu.add(menuItem = new JMenuItem("PNP 4-port"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4PNP, 900)));
						menu.show(panel, e.getX(), e.getY());
						return;
					}
					if (ni.getFunction() == NodeProto.Function.TRADMES)
					{
						JPopupMenu menu = new JPopupMenu("DMES/EMES");
						menu.add(menuItem = new JMenuItem("DMES"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRADMES, 900)));
						menu.add(menuItem = new JMenuItem("EMES"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRAEMES, 900)));
						menu.add(menuItem = new JMenuItem("DMES 4-port"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4DMES, 900)));
						menu.add(menuItem = new JMenuItem("EMES 4-port"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4EMES, 900)));
						menu.show(panel, e.getX(), e.getY());
						return;
					}
					if (ni.getFunction() == NodeProto.Function.TRAPJFET)
					{
						JPopupMenu menu = new JPopupMenu("FET");
						menu.add(menuItem = new JMenuItem("PJFET"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRAPJFET, 900)));
						menu.add(menuItem = new JMenuItem("NJFET"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRANJFET, 900)));
						menu.add(menuItem = new JMenuItem("PJFET 4-port"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4PJFET, 900)));
						menu.add(menuItem = new JMenuItem("NJFET 4-port"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4NJFET, 900)));
						menu.show(panel, e.getX(), e.getY());
						return;
					}
				}

				placeInstance(obj, panel);
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
						menuItem.addActionListener(new PlacePopupListener(panel, cell));
						cellMenu.add(menuItem);
					}
//					cellMenu.addMouseListener(new MyPopupListener());
//					cellMenu.addPopupMenuListener(new MyPopupListener());
					cellMenu.show(panel, e.getX(), e.getY());
				} else if (msg.equals("Misc."))
				{
					JPopupMenu specialMenu = new JPopupMenu("Miscellaneous");
					JMenuItem menuItem = new JMenuItem("Cell Instance...");
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { MenuCommands.cellBrowserCommand(CellBrowser.DoAction.newInstance); } });
					specialMenu.add(menuItem);

					specialMenu.addSeparator();

					menuItem = new JMenuItem("Annotation Text");
					menuItem.addActionListener(new PlacePopupListener(panel, "ART_message"));
					specialMenu.add(menuItem);
					menuItem = new JMenuItem("Layout Text...");
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeLayoutTextCommand(); } });
					specialMenu.add(menuItem);

					specialMenu.addSeparator();

					menuItem = new JMenuItem("Cell Center");
					menuItem.addActionListener(new PlacePopupListener(panel, Generic.tech.cellCenterNode));
					specialMenu.add(menuItem);
					menuItem = new JMenuItem("Essential Bounds");
					menuItem.addActionListener(new PlacePopupListener(panel, Generic.tech.essentialBoundsNode));
					specialMenu.add(menuItem);

					specialMenu.addSeparator();

					menuItem = new JMenuItem("Spice Code");
					menuItem.addActionListener(new PlacePopupListener(panel, "SIM_spice_card"));
					specialMenu.add(menuItem);
					menuItem = new JMenuItem("Verilog Code");
					menuItem.addActionListener(new PlacePopupListener(panel, "VERILOG_code"));
					specialMenu.add(menuItem);
					menuItem = new JMenuItem("Verilog Declaration");
					menuItem.addActionListener(new PlacePopupListener(panel, "VERILOG_declaration"));
					specialMenu.add(menuItem);
					menuItem = new JMenuItem("Simulation Probe");
					menuItem.addActionListener(new PlacePopupListener(panel, Generic.tech.simProbeNode));
					specialMenu.add(menuItem);
					menuItem = new JMenuItem("DRC Exclusion");
					menuItem.addActionListener(new PlacePopupListener(panel, Generic.tech.drcNode));
					specialMenu.add(menuItem);

					specialMenu.addSeparator();

					menuItem = new JMenuItem("Invisible Pin");
					menuItem.addActionListener(new PlacePopupListener(panel, Generic.tech.invisiblePinNode));
					specialMenu.add(menuItem);
					menuItem = new JMenuItem("Universal Pin");
					menuItem.addActionListener(new PlacePopupListener(panel, Generic.tech.universalPinNode));
					specialMenu.add(menuItem);
					menuItem = new JMenuItem("Unrouted Pin");
					menuItem.addActionListener(new PlacePopupListener(panel, Generic.tech.unroutedPinNode));
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
						menuItem.addActionListener(new PlacePopupListener(panel, np));
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
						ReadSpiceLibrary job = new ReadSpiceLibrary(fileURL, cellMenu, panel, e.getX(), e.getY());
					} else
					{
						for(Iterator it = spiceLib.getCells(); it.hasNext(); )
						{
							Cell cell = (Cell)it.next();
							JMenuItem menuItem = new JMenuItem(cell.getProtoName());
							menuItem.addActionListener(new PlacePopupListener(panel, cell));
							cellMenu.add(menuItem);
						}
						cellMenu.show(panel, e.getX(), e.getY());
					}
				}
			}
			repaint();
		}

		public void makeLayoutTextCommand()
		{
			LayoutText dialog = new LayoutText(TopLevel.getCurrentJFrame(), true);
			dialog.show();
		}

		/**
		 * Class to read a Spice library in a new thread.
		 */
		protected static class ReadSpiceLibrary extends Job
		{
			URL fileURL;
			JPopupMenu cellMenu;
			PalettePanel panel;
			int x, y;
			protected ReadSpiceLibrary(URL fileURL, JPopupMenu cellMenu, PalettePanel panel, int x, int y)
			{
				super("Read Spice Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
				this.fileURL = fileURL;
				this.cellMenu = cellMenu;
				this.panel = panel;
				this.x = x;
				this.y = y;
				startJob();
			}

			public void doIt()
			{
				Library lib = Input.readLibrary(fileURL, OpenFile.Type.READABLEDUMP);
				Undo.noUndoAllowed();
				if (lib == null) return;
				for(Iterator it = lib.getCells(); it.hasNext(); )
				{
					Cell cell = (Cell)it.next();
					JMenuItem menuItem = new JMenuItem(cell.getProtoName());
					menuItem.addActionListener(new PlacePopupListener(panel, cell));
					cellMenu.add(menuItem);
				}
				cellMenu.show(panel, x, y);
			}
		}

		static class PlacePopupListener implements ActionListener
		{
			PalettePanel panel;
			Object obj;

			PlacePopupListener(PalettePanel panel, Object obj) { super();  this.panel = panel;   this.obj = obj; }

			public void actionPerformed(ActionEvent evt)
			{
				JMenuItem mi = (JMenuItem)evt.getSource();
				String msg = mi.getText();
				placeInstance(obj, panel);
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
			PalettePanel panel = (PalettePanel)e.getSource();
			int x = e.getX() / (panel.frame.entrySize+1);
			int y = panel.frame.menuY - (e.getY() / (panel.frame.entrySize+1)) - 1;
			if (y < 0) y = 0;
			int index = x * frame.menuY + y;
			if (index < 0 || index >= panel.frame.inPalette.size()) return null;
			Object obj = panel.frame.inPalette.get(index);
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
			if (frame.menuX < 0 || frame.menuY < 0) return;

			// recompute size of an entry from current window size
			Dimension size = getSize();
			int wid = (int)size.getWidth();
			int hei = (int)size.getHeight();
			g.clearRect(0, 0, wid, hei);
			frame.entrySize = Math.min(wid / frame.menuX - 1, hei / frame.menuY - 1);

			// create an EditWindow for rendering nodes and arcs
			EditWindow w = EditWindow.CreateElectricDoc(null, null);
			w.setScreenSize(new Dimension(frame.entrySize, frame.entrySize));

			// draw the menu entries
			for(int x=0; x<frame.menuX; x++)
			{
				for(int y=0; y<frame.menuY; y++)
				{
					// render the entry into an Image
					int index = x * frame.menuY + y;
					if (index >= frame.inPalette.size()) continue;
					Object toDraw = frame.inPalette.get(index);
					Image img = drawMenuEntry(w, toDraw);

					// put the Image in the proper place
					int imgX = x * (frame.entrySize+1)+1;
					int imgY = (frame.menuY-y-1) * (frame.entrySize+1)+1;
					if (img != null)
					{
						g.drawImage(img, imgX, imgY, this);
					}

					// highlight if an arc or node
					if (toDraw instanceof PrimitiveArc)
					{
						g.setColor(Color.RED);
						g.drawRect(imgX, imgY, frame.entrySize-1, frame.entrySize-1);
						if (toDraw == User.tool.getCurrentArcProto())
						{
							g.drawRect(imgX+1, imgY+1, frame.entrySize-3, frame.entrySize-3);
							g.drawRect(imgX+2, imgY+2, frame.entrySize-5, frame.entrySize-5);
						}
					}
					if (toDraw instanceof NodeProto || toDraw instanceof NodeInst)
					{
						g.setColor(Color.BLUE);
						g.drawRect(imgX, imgY, frame.entrySize-1, frame.entrySize-1);
						NodeProto np = null;
						if (toDraw instanceof NodeProto) np = (NodeProto)toDraw; else
							np = ((NodeInst)toDraw).getProto();
						if (np == frame.highlightedNode)
						{
							g.drawRect(imgX+1, imgY+1, frame.entrySize-3, frame.entrySize-3);
							g.drawRect(imgX+2, imgY+2, frame.entrySize-5, frame.entrySize-5);
						}
						if (toDraw == Schematics.tech.diodeNode || toDraw == Schematics.tech.capacitorNode ||
							toDraw == Schematics.tech.flipflopNode ||
							(toDraw instanceof NodeInst && ((NodeInst)toDraw).getProto() == Schematics.tech.transistorNode))
						{
							drawArrow(g, x, y);
						}
					}
					if (toDraw instanceof String)
					{
						String str = (String)toDraw;
						g.setColor(Color.LIGHT_GRAY);
						g.fillRect(imgX, imgY, frame.entrySize, frame.entrySize);
						g.setColor(Color.BLACK);
						g.setFont(new Font("Helvetica", Font.PLAIN, 20));
						FontMetrics fm = g.getFontMetrics();
						int strWid = fm.stringWidth(str);
						int strHeight = fm.getMaxAscent() + fm.getMaxDescent();
						int xpos = imgX+frame.entrySize/2 - strWid/2;
						int ypos = imgY+frame.entrySize/2 + strHeight/2 - fm.getMaxDescent();
						g.drawString(str, xpos, ypos);
						if (str.equals("Cell") || str.equals("Spice") || str.equals("Misc.") || str.equals("Pure"))
							drawArrow(g, x, y);
					}
				}
			}

			// show dividing lines
			g.setColor(Color.BLACK);
			for(int i=0; i<=frame.menuX; i++)
			{
				int xPos = (frame.entrySize+1) * i;
				g.drawLine(xPos, 0, xPos, frame.menuY*(frame.entrySize+1));
			}
			for(int i=0; i<=frame.menuY; i++)
			{
				int yPos = (frame.entrySize+1) * i;
				g.drawLine(0, yPos, frame.menuX*(frame.entrySize+1), yPos);
			}
		}

		private void drawArrow(Graphics g, int x, int y)
		{
			int imgX = x * (frame.entrySize+1)+1;
			int imgY = (frame.menuY-y-1) * (frame.entrySize+1)+1;
			int [] arrowX = new int[3];
			int [] arrowY = new int[3];
			arrowX[0] = imgX-2 + frame.entrySize*7/8;
			arrowY[0] = imgY-2 + frame.entrySize;
			arrowX[1] = imgX-2 + frame.entrySize;
			arrowY[1] = imgY-2 + frame.entrySize*7/8;
			arrowX[2] = imgX-2 + frame.entrySize*7/8;
			arrowY[2] = imgY-2 + frame.entrySize*3/4;
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
			double scalex = frame.entrySize/largest * 0.8;
			double scaley = frame.entrySize/largest * 0.8;
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
				double scalex = frame.entrySize/largest * 0.8;
				double scaley = frame.entrySize/largest * 0.8;
				double scale = Math.min(scalex, scaley);
				return wnd.renderArc(ai, scale);
			}
			return null;
		}
	}

	/**
	 * Method to interactively place an instance of a node.
	 * @param obj the node to create.
	 * If this is a NodeProto, one of these types is created.
	 * If this is a NodeInst, one of these is created, and the specifics of this instance are copied.
	 * @param panel the PalettePanel that invoked this request.
	 * If this is null, then the request did not come from the palette.
	 */
	public static void placeInstance(Object obj, PalettePanel panel)
	{
		NodeProto np = null;
		NodeInst ni = null;
		String placeText = null;
		String whatToCreate = null;

		if (obj instanceof String)
		{
			placeText = (String)obj;
			if (placeText.equals("SIM_spice_card")) whatToCreate = "Spice code"; else
			if (placeText.equals("VERILOG_code")) whatToCreate = "Verilog code"; else
			if (placeText.equals("VERILOG_declaration")) whatToCreate = "Verilog declaration"; else
				whatToCreate = "Annotation Text";
			obj = Generic.tech.invisiblePinNode;
		}
		if (obj instanceof NodeProto)
		{
			np = (NodeProto)obj;
		} else if (obj instanceof NodeInst)
		{
			ni = (NodeInst)obj;
			np = ni.getProto();
		}
		if (np != null)
		{
			// remember the listener that was there before
			EventListener oldListener = WindowFrame.getListener();
			Cursor oldCursor = TopLevel.getCurrentCursor();

			if (whatToCreate != null) System.out.println("Click to create " + whatToCreate); else
			{
				if (np instanceof Cell)
					System.out.println("Click to create an instance of cell " + np.describe()); else
						System.out.println("Click to create node " + np.describe());
			}
			EventListener newListener = oldListener;
			if (newListener != null && newListener instanceof PlaceNodeListener)
			{
				((PlaceNodeListener)newListener).setParameter(np);
			} else
			{
				newListener = new PlaceNodeListener(panel, obj, oldListener, oldCursor);
				WindowFrame.setListener(newListener);
			}
			if (placeText != null)
				((PlaceNodeListener)newListener).setTextNode(placeText);
			if (panel != null)
				panel.getFrame().highlightedNode = np;

			// change the cursor
			TopLevel.setCurrentCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
	}

	public static class PlaceNodeListener
		implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
	{
		private int oldx, oldy;
		private Point2D drawnLoc;
		private boolean doingMotionDrag;
		private Object toDraw;
		private EventListener oldListener;
		private Cursor oldCursor;
		private boolean isDrawn;
		private String textNode;
		private PalettePanel window;
		private int defAngle;

		public PlaceNodeListener(PalettePanel window, Object toDraw, EventListener oldListener, Cursor oldCursor)
		{
			this.window = window;
			this.toDraw = toDraw;
			this.oldListener = oldListener;
			this.oldCursor = oldCursor;
			this.isDrawn = false;
			this.textNode = null;

			// get default creation angle
			NodeProto np = null;
			defAngle = 0;
			if (toDraw instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)toDraw;
				np = ni.getProto();
				defAngle = ni.getAngle();
			}
			if (toDraw instanceof NodeProto)
			{
				np = (NodeProto)toDraw;
				defAngle = np.getDefPlacementAngle();
			}

            if (window != null) {
                window.addKeyListener(this);
            }
		}

		public void setParameter(Object toDraw) { this.toDraw = toDraw; }

		public void setTextNode(String varName) { textNode = varName; }

		private void updateBox(Object source, int oldx, int oldy)
		{
			if (!(source instanceof EditWindow.CircuitPart)) return;
			EditWindow.CircuitPart dispPart = (EditWindow.CircuitPart)source;
			EditWindow wnd = dispPart.wnd;
			if (isDrawn)
			{
				// undraw it
				Highlight.clear();
			}

			// draw it
			drawnLoc = wnd.screenToDatabase(oldx, oldy);
			EditWindow.gridAlign(drawnLoc);
			NodeProto np = null;
			if (toDraw instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)toDraw;
				np = ni.getProto();
			}
			if (toDraw instanceof NodeProto)
			{
				np = (NodeProto)toDraw;
			}
			if (np != null)
			{
				Poly poly = null;
				if (np instanceof Cell)
				{
					Cell placeCell = (Cell)np;
					Rectangle2D cellBounds = placeCell.getBounds();
					SizeOffset so = np.getSizeOffset();
					poly = new Poly(cellBounds);
					AffineTransform rotate = NodeInst.pureRotate(defAngle%3600,
						(defAngle >= 3600 ? true : false), false);
					AffineTransform translate = new AffineTransform();
					translate.setToTranslation(drawnLoc.getX(), drawnLoc.getY());
					rotate.concatenate(translate);
					poly.transform(rotate);
				} else
				{
					SizeOffset so = np.getSizeOffset();
					double trueSizeX = np.getDefWidth() - so.getLowXOffset() - so.getHighXOffset();
					double trueSizeY = np.getDefHeight() - so.getLowYOffset() - so.getHighYOffset();
					poly = new Poly(drawnLoc.getX(), drawnLoc.getY(), trueSizeX, trueSizeY);
					AffineTransform trans = NodeInst.rotateAbout(defAngle%3600, drawnLoc.getX(), drawnLoc.getY(),
						(defAngle >= 3600 ? -trueSizeX : trueSizeX), trueSizeY);
					poly.transform(trans);
				}
				Point2D [] points = poly.getPoints();
				for(int i=0; i<points.length; i++)
				{
					int last = i-1;
					if (i == 0) last = points.length - 1;
					Highlight.addLine(points[last], points[i], wnd.getCell());
				}
				isDrawn = true;
				wnd.repaint();
			}
			Highlight.finished();
		}

		public void mouseReleased(MouseEvent evt)
		{
			if (!(evt.getSource() instanceof EditWindow.CircuitPart)) return;
			EditWindow.CircuitPart dispPart = (EditWindow.CircuitPart)evt.getSource();
			EditWindow wnd = dispPart.wnd;

			oldx = evt.getX();
			oldy = evt.getY();
			if (wnd.getCell() == null)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Cannot create node: this window has no cell in it");
				return;
			}
			Point2D where = wnd.screenToDatabase(oldx, oldy);
			EditWindow.gridAlign(where);

			// schedule the node to be created
			NodeInst ni = null;
			NodeProto np = null;
			if (toDraw instanceof NodeProto)
			{
				np = (NodeProto)toDraw;
			} else if (toDraw instanceof NodeInst)
			{
				ni = (NodeInst)toDraw;
				np = ni.getProto();
			}
			String descript = "Create ";
			if (np instanceof Cell) descript += ((Cell)np).noLibDescribe(); else
				descript += np.getProtoName() + " Primitive";
			PlaceNewNode job = new PlaceNewNode(descript, toDraw, where, wnd.getCell(), textNode);

			// restore the former listener to the edit windows
            finished();
		}

        public void finished()
        {
            Highlight.clear();
            Highlight.finished();
            WindowFrame.setListener(oldListener);
            TopLevel.setCurrentCursor(oldCursor);
            if (window != null)
            {
                window.removeKeyListener(this);
                window.frame.highlightedNode = null;
                window.repaint();
            }
        }

		public void mousePressed(MouseEvent evt) {}
		public void mouseClicked(MouseEvent evt) {}
		public void mouseEntered(MouseEvent evt) {}
		public void mouseExited(MouseEvent evt) {}
		public void mouseMoved(MouseEvent evt)
		{
			updateBox(evt.getSource(), evt.getX(), evt.getY());
		}

		public void mouseDragged(MouseEvent evt)
		{
			updateBox(evt.getSource(), evt.getX(), evt.getY());
		}

		public void mouseWheelMoved(MouseWheelEvent evt) {}

		public void keyPressed(KeyEvent evt)
		{
			int chr = evt.getKeyCode();
			if (chr == KeyEvent.VK_A || chr == KeyEvent.VK_ESCAPE)
			{
                // abort
				finished();
			}
		}

		public void keyReleased(KeyEvent evt) {}
		public void keyTyped(KeyEvent evt) {}
	}

	/** class that creates the node selected from the component menu */
	protected static class PlaceNewNode extends Job
	{
		Object toDraw;
		Point2D where;
		Cell cell;
		String varName;

		protected PlaceNewNode(String description, Object toDraw, Point2D where, Cell cell, String varName)
		{
			super(description, User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.toDraw = toDraw;
			this.where = where;
			this.cell = cell;
			this.varName = varName;
			startJob();
		}

		public void doIt()
		{
			NodeProto np = null;
			NodeInst ni = null;
			if (toDraw instanceof NodeProto)
			{
				np = (NodeProto)toDraw;
			} else if (toDraw instanceof NodeInst)
			{
				ni = (NodeInst)toDraw;
				np = ni.getProto();
			}
			double width = np.getDefWidth();
			double height = np.getDefHeight();
			if (varName != null) width = height = 0;

			// get default creation angle
			int defAngle = 0;
			if (ni != null)
			{
				defAngle = ni.getAngle();
			} else
			{
				defAngle = np.getDefPlacementAngle();
				if (defAngle >= 3600)
				{
					defAngle %= 3600;
					width = -width;
				}
			}

			NodeInst newNi = NodeInst.makeInstance(np, where, width, height, defAngle, cell, null);
			if (newNi == null) return;
			if (varName != null)
			{
				// text object: add initial text
				Variable var = newNi.newVar(varName, "text");
				if (var != null)
				{
					var.setDisplay();
					TextDescriptor td = TextDescriptor.newNonLayoutDescriptor(null);
					if (!varName.equals("ART_message")) td.setDispPart(TextDescriptor.DispPos.NAMEVALUE);
					var.setTextDescriptor(td);
					Highlight h = Highlight.addText(newNi, cell, var, null);
				}
			} else
			{
				if (ni != null) newNi.setTechSpecific(ni.getTechSpecific());
				Highlight.addElectricObject(newNi, cell);
			}
			Highlight.finished();
		}
	}
}
