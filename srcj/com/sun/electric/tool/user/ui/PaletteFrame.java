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
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.simulation.Spice;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.io.Input;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.EventListener;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import javax.swing.JInternalFrame;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JDesktopPane;
import javax.swing.JComboBox;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

/**
 * This class defines a palette window for component selection.
 */
public class PaletteFrame
{
	/** the edit window part */							private PalettePanel panel;
	/** the internal frame (if MDI). */					private JInternalFrame jif;
	/** the top-level frame (if SDI). */				private JFrame jf;
	/** the number of palette entries. */				private int menuX = -1, menuY = -1;
	/** the size of a palette entry. */					private int entrySize;
	/** the list of objects in the palette. */			private List inPalette;
	/** the currently selected Node in the palette. */	private NodeProto highlightedNode;

	// constructor, never called
	private PaletteFrame() {}

	/**
	 * Routine to create a new window on the screen that displays the component menu.
	 * @return the PaletteFrame that shows the component menu.
	 */
	public static PaletteFrame newInstance()
	{
		PaletteFrame palette = new PaletteFrame();

		// initialize the frame
		Dimension screenSize = TopLevel.getScreenSize();
		int screenHeight = (int)screenSize.getHeight();
		Dimension frameSize = new Dimension(100, screenHeight-100);
		if (TopLevel.isMDIMode())
		{
			palette.jif = new JInternalFrame("Components", true, false, false, false);
			palette.jif.setSize(frameSize);
			palette.jif.setLocation(0, 0);
			palette.jif.setAutoscrolls(true);
		} else
		{
			palette.jf = new JFrame("Components");
			palette.jf.setSize(frameSize);
			palette.jf.setLocation(0, 0);
			palette.jf.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		}

		// create a paletteWindow and a selector combobox
		palette.panel = new PalettePanel(palette);
		JComboBox selector = new JComboBox();
		List techList = Technology.getTechnologiesSortedByName();
		for(Iterator it = techList.iterator(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			if (tech == Generic.tech) continue;
			selector.addItem(tech.getTechName());
		}
		selector.setSelectedItem(Technology.getCurrent().getTechName());
        selector.addActionListener(new TechnologyPopupActionListener(palette));

		if (TopLevel.isMDIMode())
		{
			palette.jif.getContentPane().setLayout(new java.awt.BorderLayout());
			palette.jif.getContentPane().add(palette.panel, BorderLayout.CENTER);
			palette.jif.getContentPane().add(selector, BorderLayout.SOUTH);
			palette.jif.show();
			TopLevel.addToDesktop(palette.jif);
			palette.jif.moveToFront();
		} else
		{
			palette.jf.getContentPane().setLayout(new java.awt.BorderLayout());
			palette.jf.getContentPane().add(palette.panel, BorderLayout.CENTER);
			palette.jf.getContentPane().add(selector, BorderLayout.SOUTH);
			palette.jf.show();
		}
		return palette;
	}

	public PalettePanel getPanel() { return panel; }

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
			inPalette.add(Schematics.tech.flipflopNode);
			inPalette.add(Schematics.tech.muxNode);
			inPalette.add(Schematics.tech.xorNode);
			inPalette.add(null);

			inPalette.add(Schematics.tech.bus_arc);
			inPalette.add(Schematics.tech.busPinNode);
			inPalette.add("Inst.");
			inPalette.add(Schematics.tech.wireConNode);
			inPalette.add(Schematics.tech.switchNode);
			inPalette.add(Schematics.tech.groundNode);
			inPalette.add(Schematics.tech.inductorNode);
			inPalette.add(Schematics.tech.diodeNode);
			inPalette.add(makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRAPJFET, 900));
			inPalette.add(makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRANMOS, 900));
			inPalette.add(Schematics.tech.bufferNode);
			inPalette.add(Schematics.tech.andNode);
			inPalette.add(Schematics.tech.orNode);
			inPalette.add(Schematics.tech.bboxNode);
		} else if (tech == Artwork.tech)
		{
			menuX = 2;
			menuY = 12;
			inPalette.add(Artwork.tech.solidArc);
			inPalette.add(Artwork.tech.thickerArc);
			inPalette.add("Inst.");
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
			inPalette.add("Inst.");
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
			menuY = arcTotal + pinTotal + compTotal + 1;
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
		int ysize = (int)(size.getHeight()-100) / menuY;
		if (ysize < entrySize) entrySize = ysize;
		size.setSize(entrySize*menuX+1, entrySize*menuY+1);
		if (TopLevel.isMDIMode())
		{
			jif.setSize(size);
		} else
		{
			jf.setSize(size);
		}
		panel.repaint();
	}

	private static NodeInst makeNodeInst(NodeProto np, NodeProto.Function func, int angle)
	{
		NodeInst ni = NodeInst.lowLevelAllocate();
		ni.lowLevelPopulate(np, new Point2D.Double(0,0), np.getDefWidth(), np.getDefHeight(), angle, null);
		np.getTechnology().setPrimitiveFunction(ni, func);
//		Undo.setNextChangeQuiet();
		np.getTechnology().setDefaultOutline(ni);
		return ni;
	}

	// The class that watches changes to the technology popup at the bottom of the component menu
	static class TechnologyPopupActionListener implements ActionListener
	{
		PaletteFrame palette;

		TechnologyPopupActionListener(PaletteFrame palette) { this.palette = palette; }

		public void actionPerformed(java.awt.event.ActionEvent evt)
		{
			// the popup of libraies changed
			JComboBox cb = (JComboBox)evt.getSource();
			String techName = (String)cb.getSelectedItem();
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
		 * Routine called when the user clicks over an entry in the component menu.
		 */
		public void mousePressed(java.awt.event.MouseEvent e)
		{
			PalettePanel panel = (PalettePanel)e.getSource();
			int x = e.getX() / (panel.frame.entrySize+1);
			int y = panel.frame.menuY - (e.getY() / (panel.frame.entrySize+1)) - 1;
			int index = x * frame.menuY + y;
			Object obj = panel.frame.inPalette.get(index);
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
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRANMOS, 0)));
						menu.add(menuItem = new JMenuItem("PMOS"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRAPMOS, 0)));
						menu.add(menuItem = new JMenuItem("DMOS"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRADMOS, 0)));
						menu.add(menuItem = new JMenuItem("nMOS 4-port"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4NMOS, 0)));
						menu.add(menuItem = new JMenuItem("PMOS 4-port"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4PMOS, 0)));
						menu.add(menuItem = new JMenuItem("DMOS 4-port"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4DMOS, 0)));
						menu.show(panel, e.getX(), e.getY());
						return;
					}
					if (ni.getFunction() == NodeProto.Function.TRANPN)
					{
						JPopupMenu menu = new JPopupMenu("Bipolar");
						menu.add(menuItem = new JMenuItem("NPN"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRANPN, 0)));
						menu.add(menuItem = new JMenuItem("PNP"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRAPNP, 0)));
						menu.add(menuItem = new JMenuItem("NPN 4-port"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4NPN, 0)));
						menu.add(menuItem = new JMenuItem("PNP 4-port"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4PNP, 0)));
						menu.show(panel, e.getX(), e.getY());
						return;
					}
					if (ni.getFunction() == NodeProto.Function.TRADMES)
					{
						JPopupMenu menu = new JPopupMenu("DMES/EMES");
						menu.add(menuItem = new JMenuItem("DMES"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRADMES, 0)));
						menu.add(menuItem = new JMenuItem("EMES"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRAEMES, 0)));
						menu.add(menuItem = new JMenuItem("DMES 4-port"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4DMES, 0)));
						menu.add(menuItem = new JMenuItem("EMES 4-port"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4EMES, 0)));
						menu.show(panel, e.getX(), e.getY());
						return;
					}
					if (ni.getFunction() == NodeProto.Function.TRAPJFET)
					{
						JPopupMenu menu = new JPopupMenu("FET");
						menu.add(menuItem = new JMenuItem("PJFET"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRAPJFET, 0)));
						menu.add(menuItem = new JMenuItem("NJFET"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistorNode, NodeProto.Function.TRANJFET, 0)));
						menu.add(menuItem = new JMenuItem("PJFET 4-port"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4PJFET, 0)));
						menu.add(menuItem = new JMenuItem("NJFET 4-port"));
						menuItem.addActionListener(new PlacePopupListener(panel, makeNodeInst(Schematics.tech.transistor4Node, NodeProto.Function.TRA4NJFET, 0)));
						menu.show(panel, e.getX(), e.getY());
						return;
					}
				}

				// remember the listener that was there before
				EventListener oldListener = EditWindow.getListener();
				Cursor oldCursor = TopLevel.getCurrentCursor();

				NodeProto np = null;
				NodeInst ni = null;
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
					System.out.println("Click to create node "+np.describe());
					EventListener currentListener = oldListener;
					if (currentListener != null && currentListener instanceof PlaceNodeListener)
					{
						((PlaceNodeListener)currentListener).setParameter(np);
					} else
					{
						currentListener = new PlaceNodeListener(panel, obj, oldListener, oldCursor);
						EditWindow.setListener(currentListener);
					}
					frame.highlightedNode = np;
				}

				// change the cursor
				TopLevel.setCurrentCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			} else if (obj instanceof PrimitiveArc)
			{
				PrimitiveArc ap = (PrimitiveArc)obj;
				System.out.println("Clicked on arc "+ap.describe());
				User.tool.setCurrentArcProto(ap);
			} else if (obj instanceof String)
			{
				String msg = (String)obj;
				if (msg.equals("Inst."))
				{
					JPopupMenu cellMenu = new JPopupMenu("Cells");
					for(Iterator it = Library.getCurrent().getCells(); it.hasNext(); )
					{
						Cell cell = (Cell)it.next();
						JMenuItem menuItem = new JMenuItem(cell.describe());
						menuItem.addActionListener(new PlacePopupListener(panel, cell));
						cellMenu.add(menuItem);
					}
					cellMenu.show(panel, e.getX(), e.getY());
				} if (msg.equals("Spice"))
				{
					JPopupMenu cellMenu = new JPopupMenu("Spice");

					String currentSpiceLib = Spice.getSpicePartsLibrary();
					Library spiceLib = Library.findLibrary(currentSpiceLib);
					if (spiceLib == null)
					{
						// must read the Spice library from disk
						String fileName = LibFile.getLibFile(currentSpiceLib + ".txt");
						ReadSpiceLibrary job = new ReadSpiceLibrary(fileName, cellMenu, panel, e.getX(), e.getY());
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

		/**
		 * Class to read a Spice library in a new thread.
		 */
		protected static class ReadSpiceLibrary extends Job
		{
			String fileName;
			JPopupMenu cellMenu;
			PalettePanel panel;
			int x, y;
			protected ReadSpiceLibrary(String fileName, JPopupMenu cellMenu, PalettePanel panel, int x, int y)
			{
				super("Read Spice Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
				this.fileName = fileName;
				this.cellMenu = cellMenu;
				this.panel = panel;
				this.x = x;
				this.y = y;
				this.startJob();
			}

			public void doIt()
			{
				Library lib = Input.readLibrary(fileName, Input.ImportType.TEXT);
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

			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				// the popup of libraies changed
				JMenuItem mi = (JMenuItem)evt.getSource();
				String msg = mi.getText();

				EventListener oldListener = EditWindow.getListener();
				Cursor oldCursor = TopLevel.getCurrentCursor();

				if (obj instanceof Cell)
					System.out.println("Click to create an instance of cell "+((Cell)obj).describe());
				EventListener currentListener = oldListener;
				if (currentListener != null && currentListener instanceof PlaceNodeListener)
				{
					((PlaceNodeListener)currentListener).setParameter(obj);
				} else
				{
					currentListener = new PlaceNodeListener(panel, obj, oldListener, oldCursor);
					EditWindow.setListener(currentListener);
				}
				PaletteFrame frame = panel.getFrame();
//				frame.highlightedNode = obj;
			}
		};

		public void mouseClicked(java.awt.event.MouseEvent e) {}
		public void mouseEntered(java.awt.event.MouseEvent e) {}
		public void mouseExited(java.awt.event.MouseEvent e) {}
		public void mouseReleased(java.awt.event.MouseEvent e) {}
		public void keyPressed(KeyEvent e) {}
		public void keyReleased(KeyEvent e) {}
		public void keyTyped(KeyEvent e) {}
		public void mouseDragged(MouseEvent e) {}
		public void mouseMoved(MouseEvent e) {}
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

			// make a single-square Image
			Image img = createImage(frame.entrySize, frame.entrySize);
			Graphics2D g2 = (Graphics2D)img.getGraphics();
			for(int x=0; x<frame.menuX; x++)
			{
				for(int y=0; y<frame.menuY; y++)
				{
					// render the entry into the Image
					g2.clearRect(0, 0, frame.entrySize, frame.entrySize);
					int index = x * frame.menuY + y;
					if (index >= frame.inPalette.size()) continue;
					Object toDraw = frame.inPalette.get(index);
					drawMenuEntry(g, img, toDraw);

					// put the Image in the proper place
					int imgX = x * (frame.entrySize+1)+1;
					int imgY = (frame.menuY-y-1) * (frame.entrySize+1)+1;
					g.drawImage(img, imgX, imgY, this);

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
						g.setColor(Color.BLACK);
						g.setFont(new Font("Helvetica", Font.PLAIN, 20));
						FontMetrics fm = g.getFontMetrics();
						int strWid = fm.stringWidth(str);
						int strHeight = fm.getMaxAscent() + fm.getMaxDescent();
						int xpos = imgX+frame.entrySize/2 - strWid/2;
						int ypos = imgY+frame.entrySize/2 + strHeight/2 - fm.getMaxDescent();
						g.drawString(str, xpos, ypos);
						if (str.equals("Inst.") || str.equals("Spice"))
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

		void drawNodeInMenu(Graphics g, Image img, NodeInst ni)
		{
			// determine scale for rendering
			Graphics2D g2 = (Graphics2D)img.getGraphics();
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
			g2.translate(frame.entrySize/2, frame.entrySize/2);
			double scalex = frame.entrySize/largest * 0.8;
			double scaley = frame.entrySize/largest * 0.8;
			double scale = Math.min(scalex, scaley);
			g2.scale(scale, -scale);
			SizeOffset so = np.getSizeOffset();
			double offx = 0;   // (so.getHighXOffset() - so.getLowXOffset()) / 2;
			double offy = 0;   // (so.getHighYOffset() - so.getLowYOffset()) / 2;
			g2.translate(-offx, -offy);
			EditWindow w = EditWindow.CreateElectricDoc(null);
			w.setScale(scale);
			w.drawNode(g2, ni, new AffineTransform(), true);
		}

		private final static double menuArcLength = 8;

		void drawMenuEntry(Graphics g, Image img, Object entry)
		{
			g.setColor(Color.lightGray);
			Graphics2D g2 = (Graphics2D)img.getGraphics();

			// setup graphics for rendering (start at bottom and work up)
			if (entry instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)entry;
				drawNodeInMenu(g, img, ni);
			}
			if (entry instanceof NodeProto)
			{
				// rendering a node: reate the temporary node
				NodeProto np = (NodeProto)entry;
				NodeInst ni = NodeInst.lowLevelAllocate();
				ni.lowLevelPopulate(np, new Point2D.Double(0,0), np.getDefWidth(), np.getDefHeight(), 0, null);
				drawNodeInMenu(g, img, ni);
			}
			if (entry instanceof PrimitiveArc)
			{
				// rendering an arc
				PrimitiveArc ap = (PrimitiveArc)entry;
				PrimitiveNode npEnd = ap.findPinProto();
				if (npEnd == null) return;

				// create the head node
				NodeInst niH = NodeInst.lowLevelAllocate();
				niH.lowLevelPopulate(npEnd, new Point2D.Double(-menuArcLength/2,0), npEnd.getDefWidth(), npEnd.getDefHeight(), 0, null);
				PortInst piH = niH.getOnlyPortInst();
				Rectangle2D boundsH = piH.getBounds();
				double xH = boundsH.getCenterX();
				double yH = boundsH.getCenterY();

				// create the tail node
				NodeInst niT = NodeInst.lowLevelAllocate();
				niT.lowLevelPopulate(npEnd, new Point2D.Double(menuArcLength/2,0), npEnd.getDefWidth(), npEnd.getDefHeight(), 0, null);
				PortInst piT = niT.getOnlyPortInst();
				Rectangle2D boundsT = piT.getBounds();
				double xT = boundsT.getCenterX();
				double yT = boundsT.getCenterY();

				// create the arc that connects them
				ArcInst ai = ArcInst.lowLevelAllocate();
				ai.lowLevelPopulate(ap, ap.getDefaultWidth(), piH, new Point2D.Double(xH, yH), piT, new Point2D.Double(xT, yT));

				// determine scale for rendering
				double largest = 0;
				for(Iterator it = ap.getTechnology().getArcs(); it.hasNext(); )
				{
					PrimitiveArc otherAp = (PrimitiveArc)it.next();
					double wid = otherAp.getDefaultWidth();
					if (wid+menuArcLength > largest) largest = wid+menuArcLength;
				}

				// render the arc
				g2.translate(frame.entrySize/2, frame.entrySize/2);
				double scalex = frame.entrySize/largest * 0.8;
				double scaley = frame.entrySize/largest * 0.8;
				double scale = Math.min(scalex, scaley);
				g2.scale(scale, -scale);
				double offx = 0, offy = 0;
				g2.translate(-offx, -offy);
				EditWindow w = EditWindow.CreateElectricDoc(null);
				w.setScale(scale);
				w.drawArc(g2, ai, new AffineTransform(), true);
			}
		}
	}

	static class PlaceNodeListener
		implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
	{
		private int oldx, oldy;
		private Point2D drawnLoc;
		private boolean doingMotionDrag;
		private Object toDraw;
		private EventListener oldListener;
		private Cursor oldCursor;
		private boolean isDrawn;
		private PalettePanel window;

		private PlaceNodeListener(PalettePanel window, Object toDraw, EventListener oldListener, Cursor oldCursor)
		{
			this.window = window;
			this.toDraw = toDraw;
			this.oldListener = oldListener;
			this.oldCursor = oldCursor;
			this.isDrawn = false;
		}

		public void setParameter(Object toDraw) { this.toDraw = toDraw; }

		private void updateBox(EditWindow wnd, int oldx, int oldy)
		{
			if (isDrawn)
			{
				// undraw it
				Highlight.clear();
			}

			// draw it
			drawnLoc = wnd.screenToDatabase(oldx, oldy);
			wnd.gridAlign(drawnLoc, 1);
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
				SizeOffset so = np.getSizeOffset();
				double trueSizeX = np.getDefWidth() - so.getLowXOffset() - so.getHighXOffset();
				double trueSizeY = np.getDefHeight() - so.getLowYOffset() - so.getHighYOffset();
				double lowX = drawnLoc.getX() - trueSizeX/2;
				double lowY = drawnLoc.getY() - trueSizeY/2;
				Highlight h = Highlight.addArea(new Rectangle2D.Double(lowX, lowY, trueSizeX, trueSizeY), wnd.getCell());
				isDrawn = true;
				wnd.repaint();
			}
		}

		public void mouseReleased(MouseEvent evt)
		{
			oldx = evt.getX();
			oldy = evt.getY();
			EditWindow wnd = (EditWindow)evt.getSource();
			Point2D where = wnd.screenToDatabase(oldx, oldy);
			wnd.gridAlign(where, 1);

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
			PlaceNewNode job = new PlaceNewNode(descript, toDraw, where, wnd.getCell());

			// restore the former listener to the edit windows
			Highlight.clear();
			EditWindow.setListener(oldListener);
			window.frame.highlightedNode = null;
			TopLevel.setCurrentCursor(oldCursor);
			window.repaint();
		}

		public void mousePressed(MouseEvent evt) {}
		public void mouseClicked(MouseEvent evt) {}
		public void mouseEntered(MouseEvent evt) {}
		public void mouseExited(MouseEvent evt) {}
		public void mouseMoved(MouseEvent evt)
		{
			updateBox((EditWindow)evt.getSource(), evt.getX(), evt.getY());
		}

		public void mouseDragged(MouseEvent evt)
		{
			updateBox((EditWindow)evt.getSource(), evt.getX(), evt.getY());
		}

		public void mouseWheelMoved(MouseWheelEvent evt) {}

		public void keyPressed(KeyEvent evt)
		{
			int chr = evt.getKeyCode();
			EditWindow wnd = (EditWindow)evt.getSource();
			if (chr == KeyEvent.VK_A)
			{
				// abort?
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

		protected PlaceNewNode(String description, Object toDraw, Point2D where, Cell cell)
		{
			super(description, User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.toDraw = toDraw;
			this.where = where;
			this.cell = cell;
			this.startJob();
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
			NodeInst newNi = NodeInst.newInstance(np, where, np.getDefWidth(), np.getDefHeight(), 0, cell, null);
			if (newNi == null) return;
			if (ni != null) newNi.setTechSpecific(ni.getTechSpecific());
			np.getTechnology().setDefaultOutline(newNi);
			Highlight.addGeometric(newNi);
		}
	}
}
