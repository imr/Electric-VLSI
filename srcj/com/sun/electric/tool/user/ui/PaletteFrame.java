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

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.NewExport;
import com.sun.electric.tool.user.tecEdit.Manipulate;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.util.EventListener;
import java.util.Iterator;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * This class defines a component window in the side bar.
 */
public class PaletteFrame implements MouseListener
{
	/** the palette window panel. */					private JPanel topPanel;
	/** the technology palette */						private TechPalette techPalette;
	/** the popup that selects technologies. */			private JComboBox techSelector;

	// constructor
	private PaletteFrame() {}

	/**
	 * Method to create a new window on the screen that displays the component menu.
	 * @return the PaletteFrame that shows the component menu.
	 */
	public static PaletteFrame newInstance(WindowFrame ww)
	{
		PaletteFrame palette = new PaletteFrame();
		palette.topPanel = new JPanel();

		// initialize all components
        palette.initComponents(ww);

		return palette;
	}

	/**
	 * Method to update the technology popup selector.
	 * Called at initialization or when a new technology has been created.
	 * @param makeCurrent true to keep the current technology selected,
	 * false to set to the current technology.
	 */
	public void loadTechnologies(boolean makeCurrent)
	{
        Technology cur = Technology.getCurrent();
        if (!makeCurrent) cur = Technology.findTechnology((String)techSelector.getSelectedItem());
        techSelector.removeAllItems();
        for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
        {
            Technology tech = it.next();
            if (tech == Generic.tech) continue;
            techSelector.addItem(tech.getTechName());
        }
        setSelectedItem(cur.getTechName());
	}

    /**
     * Public function to set selected item in techSelector
     * @param anObject
     */
    public void setSelectedItem(Object anObject) { techSelector.setSelectedItem(anObject); }

    private void initComponents(WindowFrame wf) {
        Container content = topPanel;

        // layout the Buttons and Combo boxes that control the palette
        content.setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints;

        techSelector = new JComboBox();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        content.add(techSelector, gridBagConstraints);

        // this panel will switch between the different palettes
        techPalette = new TechPalette();
        techPalette.setFocusable(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        content.add(techPalette, gridBagConstraints);

        techSelector.setLightWeightPopupEnabled(false);

        // Getting default tech stored
        loadTechnologies(true);

        techSelector.addActionListener(new WindowFrame.CurTechControlListener(wf));
    }

	/**
	 * Method to set the cursor that is displayed in the PaletteFrame.
	 * @param cursor the cursor to display here.
	 */
	public void setCursor(Cursor cursor)
	{
		techPalette.setCursor(cursor);
	}

	public JPanel getTechPalette()
	{
		return topPanel;
	}

	public void arcProtoChanged()
	{
		techPalette.repaint();
	}

    /**
     * Set the Technology Palette to the current technology.
     */
	public void loadForTechnology(Technology tech, WindowFrame ww)
	{
		techSelector.setSelectedItem(tech.getTechName());
        Dimension size = techPalette.loadForTechnology(tech, ww.getContent().getCell());
        if (techPalette.isVisible()) {
            setSize(size);
        }
	}

    private void setSize(Dimension size) {
        topPanel.setSize(size);
    }

    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {
        if (e.isShiftDown() || e.isControlDown() || e.isAltDown()) return;
    }

    /**
     * Interface for a Palette object that can be added to the Palette frame
     */
    public static interface PlaceNodeEventListener {
        /**
         * Called when the placeNodeListener is started, and the requested Object nodeToBePlaced
         * is in the process of being placed
         * @param nodeToBePlaced the node that will be placed
         */
        public void placeNodeStarted(Object nodeToBePlaced);
        /**
         * Called when the placeNodeListener has finished.
         * @param cancelled true if process aborted and nothing place, false otherwise.
         */
        public void placeNodeFinished(boolean cancelled);
    }

	/**
	 * Method to interactively place an instance of a node.
	 * @param obj the node to create.
	 * If this is a NodeProto, one of these types is created.
	 * If this is a NodeInst, one of these is created, and the specifics of this instance are copied.
	 * @param palette if not null, is notified of certain events during the placing of the node
	 * If this is null, then the request did not come from the palette.
	 */
	public static PlaceNodeListener placeInstance(Object obj, PlaceNodeEventListener palette, boolean export)
	{
		NodeProto np = null;
		NodeInst ni = null;
		String placeText = null;
		String whatToCreate = null;

        // make sure current cell is not null
        Cell curCell = WindowFrame.needCurCell();
        if (curCell == null) return null;

		if (obj instanceof String)
		{
			placeText = (String)obj;
			whatToCreate = Variable.betterVariableName(placeText);
			obj = Generic.tech.invisiblePinNode;
		}
		if (obj instanceof NodeProto)
		{
			np = (NodeProto)obj;
			if (np instanceof Cell)
			{
				// see if a contents is requested when it should be an icon
				Cell cell = (Cell)np;
				Cell iconCell = cell.iconView();
				if (iconCell != null && iconCell != cell)
				{
					int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(),
						"Don't you really want to place the icon " + iconCell.describe(true) + "?");
					if (response == JOptionPane.CANCEL_OPTION) return null;
					if (response == JOptionPane.YES_OPTION) obj = np = iconCell;
				}
			}
		} else if (obj instanceof NodeInst)
		{
			ni = (NodeInst)obj;
			np = ni.getProto();
			whatToCreate = ni.getFunction() + " node";
		}
		if (np != null)
		{
			// remember the listener that was there before
			EventListener oldListener = WindowFrame.getListener();
			Cursor oldCursor = TopLevel.getCurrentCursor();

			if (whatToCreate != null)
                System.out.println("Click to create " + whatToCreate);
            else
			{
				if (np instanceof Cell)
					System.out.println("Click to create an instance of " + np); else
						System.out.println("Click to create " + np);
			}
			EventListener newListener = oldListener;
			if (newListener != null && newListener instanceof PlaceNodeListener)
			{
                // It has to be obj so it would remember if element from list was selected.
				((PlaceNodeListener)newListener).setParameter(obj);
				((PlaceNodeListener)newListener).makePortWhenCreated(export);
			} else
			{
				newListener = new PlaceNodeListener(obj, oldListener, oldCursor, palette);
				((PlaceNodeListener)newListener).makePortWhenCreated(export);
				WindowFrame.setListener(newListener);
			}
			if (placeText != null)
				((PlaceNodeListener)newListener).setTextNode(placeText);
			if (palette != null)
				palette.placeNodeStarted(obj);

			// change the cursor
			TopLevel.setCurrentCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

			// zoom the window to fit the placed node (if appropriate)
			EditWindow wnd = EditWindow.getCurrent();
			if (wnd != null)
            {
			    wnd.requestFocus();
				wnd.zoomWindowToFitCellInstance(np);
            }

			return (PlaceNodeListener)newListener;
		}
        return null;
	}

	/**
	 * Class to choose a location for new node placement.
	 */
	public static class PlaceNodeListener
		implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
	{
		private int oldx, oldy;
//		private Point2D drawnLoc;
//		private boolean doingMotionDrag;
		private Object toDraw;
		private EventListener oldListener;
		private Cursor oldCursor;
		private String textNode;
		private boolean makePort;
        private PlaceNodeEventListener palette;

        /**
         * Places a new Node.  You should be using the static method
         * PaletteFrame.placeInstance() instead of this.
         * @param toDraw
         * @param oldListener
         * @param oldCursor
         * @param palette
         */
		private PlaceNodeListener(Object toDraw, EventListener oldListener, Cursor oldCursor,
                                 PlaceNodeEventListener palette)
		{
			this.toDraw = toDraw;
			this.oldListener = oldListener;
			this.oldCursor = oldCursor;
			this.textNode = null;
			this.makePort = false;
            this.palette = palette;
		}

		public void makePortWhenCreated(boolean m) { makePort = m; }

		public void setParameter(Object toDraw) { this.toDraw = toDraw; }

		public void setTextNode(String varName) { textNode = varName; }

		public void mouseReleased(MouseEvent evt)
		{
			if (!(evt.getSource() instanceof EditWindow)) return;
			EditWindow wnd = (EditWindow)evt.getSource();

			oldx = evt.getX();
			oldy = evt.getY();
			Cell cell = wnd.getCell();
			if (cell == null)
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

			// if in a technology editor, validate the creation
			if (cell.isInTechnologyLibrary())
			{
				if (Manipulate.invalidCreation(np, cell))
				{
					// invalid placement: restore the former listener to the edit windows
		            finished(wnd, false);
					return;
				}
			}

			int defAngle = 0;
			String descript = "Create ";
			if (np instanceof Cell) descript += ((Cell)np).noLibDescribe(); else
			{
				descript += np.getName() + " Primitive";
				defAngle = ((PrimitiveNode)np).getDefPlacementAngle();
			}
            wnd.getHighlighter().clear();
            new PlaceNewNode(descript, np, ni, defAngle, where, cell, textNode, makePort);

			// restore the former listener to the edit windows
            finished(wnd, false);
		}

        public void finished(EditWindow wnd, boolean cancelled)
        {
            if (wnd != null) {
                Highlighter highlighter = wnd.getHighlighter();
                highlighter.clear();
                highlighter.finished();
            }
            WindowFrame.setListener(oldListener);
            TopLevel.setCurrentCursor(oldCursor);
            if (palette != null)
                palette.placeNodeFinished(cancelled);
        }

		public void mousePressed(MouseEvent evt) {}
		public void mouseClicked(MouseEvent evt) {}
		public void mouseEntered(MouseEvent evt) {}
		public void mouseExited(MouseEvent evt) {}
		public void mouseMoved(MouseEvent evt)
		{
			if (evt.getSource() instanceof EditWindow)
			{
				EditWindow wnd = (EditWindow)evt.getSource();
				wnd.showDraggedBox(toDraw, evt.getX(), evt.getY());
			}
		}

		public void mouseDragged(MouseEvent evt)
		{
			if (evt.getSource() instanceof EditWindow)
			{
				EditWindow wnd = (EditWindow)evt.getSource();
				wnd.showDraggedBox(toDraw, evt.getX(), evt.getY());
			}
		}

		public void mouseWheelMoved(MouseWheelEvent evt) {}

		public void keyPressed(KeyEvent evt)
		{
			int chr = evt.getKeyCode();
			if (chr == KeyEvent.VK_A || chr == KeyEvent.VK_ESCAPE)
			{
                // abort
				finished(EditWindow.getCurrent(), true);
			}
		}

		public void keyReleased(KeyEvent evt) {}
		public void keyTyped(KeyEvent evt) {}
	}

	/**
	 * Class that creates the node selected from the component menu.
	 */
	public static class PlaceNewNode extends Job
	{
		private NodeProto np;
		private int techBits = 0;
        private Orientation defOrient = Orientation.IDENT;
        private double [] angles = null;
		private EPoint where;
		private Cell cell;
		private String varName;
		private boolean export;
		private Variable.Key varKeyToHighlight;
		private ElectricObject objToHighlight;

		public PlaceNewNode(String description, NodeProto np, NodeInst ni, int defAngle, Point2D where, Cell cell, String varName, boolean export)
		{
			super(description, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.np = np;
			// get default creation angle
            if (ni != null)
            {
                defOrient = ni.getOrient();
				techBits = ni.getTechSpecific();
                angles = ni.getArcDegrees();
            } else if (np instanceof PrimitiveNode)
			{
                defOrient = Orientation.fromJava(defAngle, defAngle >= 3600, false);
			}
			this.where = EPoint.snap(where);
			this.cell = cell;
			this.varName = varName;
			this.export = export;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			double width = np.getDefWidth();
			double height = np.getDefHeight();
			if (varName != null) width = height = 0;

			NodeInst newNi = NodeInst.makeInstance(np, where, width, height, cell, defOrient, null, techBits);
			if (newNi == null) return false;
			if (np == Generic.tech.cellCenterNode || np == Generic.tech.essentialBoundsNode ||
                (np instanceof PrimitiveNode && ((PrimitiveNode)np).isPureWellNode()))
					newNi.setHardSelect();
			if (varName != null)
			{
				// text object: add initial text
				varKeyToHighlight = Variable.newKey(varName);
				newNi.newVar(varKeyToHighlight, "text", TextDescriptor.getAnnotationTextDescriptor());
				objToHighlight = newNi;
				fieldVariableChanged("objToHighlight");
				fieldVariableChanged("varKeyToHighlight");
			} else
			{
				if (np == Schematics.tech.resistorNode)
				{
					Variable var = newNi.newDisplayVar(Schematics.SCHEM_RESISTANCE, "100");
                    // Adding two extra variables: length and width
                    if (newNi.getFunction() == PrimitiveNode.Function.PRESIST)
                    {
                        // They will be visible
                        TextDescriptor td = TextDescriptor.getNodeTextDescriptor();
						if (td.getSize().isAbsolute())
							td = td.withAbsSize((int)(td.getSize().getSize() - 1));
						else
							td = td.withRelSize(td.getSize().getSize() - 0.5);
						td = td.withOff(1.5, 0);
						var = newNi.newVar(Schematics.ATTR_WIDTH, "2", td);

						td = TextDescriptor.getNodeTextDescriptor();
						if (td.getSize().isAbsolute())
							td = td.withAbsSize((int)(td.getSize().getSize() - 2));
						else
							td = td.withRelSize(td.getSize().getSize() - 0.7);
						td = td.withOff(-1.5, 0);
						var = newNi.newVar(Schematics.ATTR_LENGTH, "2", td);
                    }
				} else if (np == Schematics.tech.capacitorNode)
				{
					Variable var = newNi.newDisplayVar(Schematics.SCHEM_CAPACITANCE, "100M");
				} else if (np == Schematics.tech.inductorNode)
				{
					Variable var = newNi.newDisplayVar(Schematics.SCHEM_INDUCTANCE, "100");
				} else if (np == Schematics.tech.diodeNode)
				{
					Variable var = newNi.newDisplayVar(Schematics.SCHEM_DIODE, "10");
				} else if (np == Schematics.tech.transistorNode || np == Schematics.tech.transistor4Node)
				{
					if (newNi.isFET())
					{
						TextDescriptor td = TextDescriptor.getNodeTextDescriptor().withOff(0.5, -1);
						Variable var = newNi.newVar(Schematics.ATTR_WIDTH, "2", td);

						td = TextDescriptor.getNodeTextDescriptor();
						if (td.getSize().isAbsolute())
							td = td.withAbsSize((int)(td.getSize().getSize() - 2));
						else
							td = td.withRelSize(td.getSize().getSize() - 0.5);
						td = td.withOff(-0.5, -1);
						var = newNi.newVar(Schematics.ATTR_LENGTH, "2", td);
					} else
					{
						Variable var = newNi.newDisplayVar(Schematics.ATTR_AREA, "10");
					}
				} else if (np == Artwork.tech.circleNode)
				{
					if (angles != null)
					{
						newNi.setArcDegrees(angles[0], angles[1]);
					}
				}
				objToHighlight = newNi;
				fieldVariableChanged("objToHighlight");
			}
			return true;
		}

        public void terminateOK()
        {
            EditWindow wnd = EditWindow.getCurrent();
            Highlighter highlighter = wnd.getHighlighter();
            if (varKeyToHighlight != null)
        	{
        		highlighter.addText(objToHighlight, cell, varKeyToHighlight);
        	} else
        	{
				highlighter.addElectricObject(objToHighlight, cell);
        	}
			highlighter.finished();

            // regaining focus in editing space
            wnd.requestFocus();
            
			// for technology edit cells, mark the new geometry specially
			if (cell.isInTechnologyLibrary())
			{
				Manipulate.completeNodeCreation((NodeInst)objToHighlight, null); // ni);
			}
			if (export)
			{
				new NewExport(TopLevel.getCurrentJFrame(), true);
			}
        }
	}
}
