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

//import com.sun.electric.database.change.DatabaseChangeListener;
//import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ExportChanges;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.tecEdit.Manipulate;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
 * This class defines a palette window for component selection.
 */
public class PaletteFrame implements /*DatabaseChangeListener,*/ MouseListener
{
//	/** the palette window frame. */					private Container container;
	/** the palette window panel. */					private JPanel topPanel;
//	/** panel in which to place palettes */             private JPanel paletteSwitcher;
	/** the technology palette */						private TechPalette techPalette;
//    /** the library palette */                          private LibraryPalette libraryPalette;
	/** the popup that selects technologies. */			private JComboBox techSelector;
//    /** the popup that selects libraries */             private JComboBox librarySelector;
//    /** the radio button for the tech selector */       private JRadioButton techRadioButton;
//    /** the radio button for the library selector */    private JRadioButton libraryRadioButton;
//    /** button group for the radio buttons */           private ButtonGroup radioButtonGroup;
//    /** library Palette size */                         private Dimension libraryPaletteSize;
//    /** library popup menu */                           private JPopupMenu libraryPopup;

    /** minimum size */
//    private static Dimension minSize;
//    private static final String TECHPALETTE = "Tech Palette";
//    private static final String LIBRARYPALETTE = "Library Palette";

	// constructor, never called
	private PaletteFrame() {}

	/**
	 * Method to create a new window on the screen that displays the component menu.
	 * @return the PaletteFrame that shows the component menu.
	 */
	public static PaletteFrame newInstance(WindowFrame ww)
	{
		PaletteFrame palette = new PaletteFrame();

//		// initialize the frame
//        if (TopLevel.isMDIMode())
//		{
//			JInternalFrame jInternalFrame = new JInternalFrame("Components", true, false, false, false);
//			palette.container = jInternalFrame;
//			jInternalFrame.setAutoscrolls(true);
//			jInternalFrame.setFrameIcon(TopLevel.getFrameIcon());
//		} else
//		{
//			JFrame jFrame = new JFrame("Components");
//			palette.container = jFrame;
//			jFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//		}
//
//        // Set dimensions
//        if (minSize == null) {
//            Dimension screenSize = TopLevel.getScreenSize();
//            int screenHeight = (int)screenSize.getHeight();
//            Dimension frameSize = new Dimension(100, (int)(screenHeight*0.9)); // multiply by 0.9 to make room for taskbar
//            minSize = frameSize;
//        }
//        palette.libraryPaletteSize = minSize;
//        palette.container.setSize(minSize);
//        palette.container.setLocation(0,0);

		palette.topPanel = new JPanel();

		// initialize all components
        palette.initComponents(ww);

//		if (TopLevel.isMDIMode())
//		{
//			if (!Main.BATCHMODE) ((JInternalFrame)palette.container).show();
//			TopLevel.addToDesktop((JInternalFrame)palette.container);
//			//((JInternalFrame)palette.container).moveToFront();
//		} else
//		{
//			if (!Main.BATCHMODE) ((JFrame)palette.container).setVisible(true);
//		}

//        Undo.addDatabaseChangeListener(palette);
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
        for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
        {
            Technology tech = (Technology)it.next();
            if (tech == Generic.tech) continue;
            techSelector.addItem(tech.getTechName());
        }
        techSelector.setSelectedItem(cur.getTechName());
	}

    private void initComponents(WindowFrame ww) {
//        Container content = ((RootPaneContainer)container).getContentPane();
        Container content = topPanel;

        // layout the Buttons and Combo boxes that control the palette
        content.setLayout(new java.awt.GridBagLayout());
        GridBagConstraints gridBagConstraints;

//        techRadioButton = new JRadioButton();
//        gridBagConstraints = new java.awt.GridBagConstraints();
//        gridBagConstraints.gridx = 0;
//        gridBagConstraints.gridy = 0;
//        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
//        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
//        content.add(techRadioButton, gridBagConstraints);
//
//        libraryRadioButton = new JRadioButton();
//        gridBagConstraints = new java.awt.GridBagConstraints();
//        gridBagConstraints.gridx = 0;
//        gridBagConstraints.gridy = 1;
//        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
//        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
//        content.add(libraryRadioButton, gridBagConstraints);
//
//        radioButtonGroup = new ButtonGroup();
//        radioButtonGroup.add(techRadioButton);
//        radioButtonGroup.add(libraryRadioButton);
//        techRadioButton.setSelected(true);

        techSelector = new JComboBox();
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
        content.add(techSelector, gridBagConstraints);

//        librarySelector = new JComboBox();
//        gridBagConstraints = new java.awt.GridBagConstraints();
//        gridBagConstraints.gridx = 1;
//        gridBagConstraints.gridy = 1;
//        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
//        gridBagConstraints.weightx = 1.0;
//        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
//        content.add(librarySelector, gridBagConstraints);

        // this panel will switch between the different palettes
//        paletteSwitcher = new JPanel(new CardLayout());
        techPalette = new TechPalette();
        techPalette.setFocusable(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
        content.add(techPalette, gridBagConstraints);

        techSelector.setLightWeightPopupEnabled(false);
//        librarySelector.setLightWeightPopupEnabled(false);

        // create the palettes and add them
//        libraryPalette = new LibraryPalette(minSize);
//        paletteSwitcher.add(techPalette, TECHPALETTE);
//        paletteSwitcher.add(libraryPalette, LIBRARYPALETTE);

        // Getting default tech stored
        //techSelector.setSelectedItem(User.getDefaultTechnology());
        loadTechnologies(true);

        PaletteControlListener l = new PaletteControlListener(this, ww);
//        techRadioButton.addActionListener(l);
        techSelector.addActionListener(l);
//        librarySelector.addActionListener(l);
//        libraryRadioButton.addActionListener(l);
//        librarySelector.addMouseListener(this);

//        // Populate the library combo box
//        updateLibrarySelector();
    }

//    /**
//     * Update the list of libraries in the library selector
//     */
//    private void updateLibrarySelector() {
//        librarySelector.removeAllItems();
//        /*for (Library lib: Library.getVisibleLibraries()) librarySelector.addItem(lib.getName());*/
//        for (Iterator it = Library.getVisibleLibraries().iterator(); it.hasNext(); )
//		{
//			Library lib = (Library)it.next();
//			librarySelector.addItem(lib.getName());
//		}
//        Library current = Library.getCurrent();
//        if (current != null)
//            librarySelector.setSelectedItem(current.getName());
//        libraryPalette.setLibrary(current);
//    }

	/**
	 * Method to set the cursor that is displayed in the PaletteFrame.
	 * @param cursor the cursor to display here.
	 */
	public void setCursor(Cursor cursor)
	{
		techPalette.setCursor(cursor);
//        libraryPalette.setCursor(cursor);
	}

//	public Rectangle getPaletteLocation()
//	{
//		return container.getBounds();
//	}

	public JPanel getTechPalette()
	{
		return topPanel;
	}

	public void arcProtoChanged()
	{
		techPalette.repaint();
	}

	/**
	 * Method to automatically switch to the proper technology for a Cell.
	 * @param cell the cell being displayed.
	 * If technology auto-switching is on, make sure the right technology is displayed
	 * for the Cell.
	 */
	public static void autoTechnologySwitch(Cell cell, WindowFrame wf)
	{
		if (cell.getView().isTextView()) return;
		Technology tech = cell.getTechnology();
		if (tech != null && tech != Technology.getCurrent())
		{
			if (User.isAutoTechnologySwitch())
			{
				tech.setCurrent();
				wf.getPaletteTab().techSelector.setSelectedItem(tech.getTechName());
			}
		}
	}

//    /**
//     * Switch the currently displayed Palette
//     * @param paletteName
//     */
//    public void switchToPalette(String paletteName) {
//        if (libraryPalette.isVisible()) {
//            // save old size
//            libraryPaletteSize = container.getSize();
//        }
//
//        CardLayout layout = (CardLayout)paletteSwitcher.getLayout();
//        if (paletteName == TECHPALETTE) {
//            layout.show(paletteSwitcher, TECHPALETTE);
//        }
//        if (paletteName == LIBRARYPALETTE) {
//            layout.show(paletteSwitcher, LIBRARYPALETTE);
//            setSize(libraryPaletteSize);
//        }
//    }

    /**
     * Set the Technology Palette (if shown) to the current technology.
     */
	public void loadForTechnology(Technology tech, WindowFrame ww)
	{
//		Technology tech = Technology.getCurrent();
        //Technology tech = Technology.findTechnology(User.getDefaultTechnology());
        Dimension size = techPalette.loadForTechnology(tech, ww.getContent().getCell());
        if (techPalette.isVisible()) {
            setSize(size);
        }
	}

//    /**
//     * Set the Library Palette (if shown) to the specified library.
//     * @param lib
//     */
//    public void loadForLibrary(Library lib)
//    {
//        libraryPalette.setLibrary(lib);
//    }

    private void setSize(Dimension size) {
//        if (size.getWidth() < minSize.getWidth()) size.setSize(minSize.getWidth(), size.getHeight());
//        if (size.getHeight() < minSize.getHeight()) size.setSize(size.getWidth(), minSize.getHeight());
        topPanel.setSize(size);
    }

    // responds to changes in the Palette control
    private static class PaletteControlListener implements ActionListener
    {
        private PaletteFrame palette;
        private WindowFrame ww;

        PaletteControlListener(PaletteFrame palette, WindowFrame ww)
        {
        	this.palette = palette;
        	this.ww = ww;
        }

        public void actionPerformed(ActionEvent evt) {
            Object source = evt.getSource();
            if (source == palette.techSelector) {
                // change the technology
                String techName = (String)palette.techSelector.getSelectedItem();
                Technology  tech = Technology.findTechnology(techName);
                if (tech != null) {
                    tech.setCurrent();
                    palette.loadForTechnology(tech, ww);
                }
            }
//            else if (source == palette.librarySelector) {
//                // change the library
//                String libName = (String)palette.librarySelector.getSelectedItem();
//                Library lib = Library.findLibrary(libName);
//                if (lib != null) {
//                    palette.loadForLibrary(lib);
//                }
//            }
//            else if (source == palette.techRadioButton) {
//                // switch to technology palette
//                palette.switchToPalette(TECHPALETTE);
//                palette.loadForTechnology();
//            }
//            else if (source == palette.libraryRadioButton) {
//                // switch to the library palette
//                palette.switchToPalette(LIBRARYPALETTE);
//                String libName = (String)palette.librarySelector.getSelectedItem();
//                Library lib = Library.findLibrary(libName);
//                if (lib != null) {
//                    palette.loadForLibrary(lib);
//                }
//            }
        }
    }

//     public void databaseChanged(Undo.Change evt) {}
//     public boolean isGUIListener() { return true; }
//     public void databaseEndChangeBatch(Undo.ChangeBatch batch) {
//         boolean libChanged = false;
//         for (Iterator it = batch.getChanges(); it.hasNext(); ) {
//             Undo.Change change = (Undo.Change)it.next();
//             if (change.getType() == Undo.Type.LIBRARYKILL ||
//                 change.getType() == Undo.Type.LIBRARYNEW) {
//                 libChanged = true;
//                 break;
//             }
//             if ((change.getType() == Undo.Type.OBJECTRENAME) &&
//                 (change.getObject() instanceof Library)) {
//                 libChanged = true;
//                 break;
//             }
//         }
// //        if (libChanged)
// //            updateLibrarySelector();
//     }

    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {
        if (e.isShiftDown() || e.isControlDown() || e.isAltDown()) return;

//        if (e.isMetaDown()) {
//            Object source = e.getSource();
//            if (source == librarySelector) {
//                initLibraryPopup();
//                libraryPopup.show(librarySelector, e.getX(), e.getY());
//            }
//        }
    }

//    private void initLibraryPopup() {
//        if (libraryPopup != null) return;
//
//        libraryPopup = new JPopupMenu();
//        JMenuItem m;
//        m = new JMenuItem("Set current");
//        m.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent e) { selectedLibSetCurrent(); }
//        });
//        libraryPopup.add(m);
//        m = new JMenuItem("Save");
//        m.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent e) { selectedLibSave(); }
//        });
//        libraryPopup.add(m);
//        m = new JMenuItem("Save as...");
//        m.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent e) { selectedLibSaveAs(); }
//        });
//        libraryPopup.add(m);
//        m = new JMenuItem("Rename");
//        m.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent e) { selectedLibRename(); }
//        });
//        libraryPopup.add(m);
//        m = new JMenuItem("Close");
//        m.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent e) { selectedLibClose(); }
//        });
//        libraryPopup.add(m);
//    }

//    private Library getSelectedLib() {
//        String libName = (String)librarySelector.getSelectedItem();
//        Library lib = Library.findLibrary(libName);
//        if (lib == null) System.out.println("No such library \""+libName+"\"");
//        return lib;
//    }
//    private void selectedLibSetCurrent() {
//        Library lib = getSelectedLib();
//        if (lib == null) return;
//        lib.setCurrent();
//    }
//    private void selectedLibSave() {
//        Library lib = getSelectedLib();
//        if (lib == null) return;
//        FileMenu.saveLibraryCommand(lib, FileType.DEFAULTLIB, false, true);
//    }
//    private void selectedLibSaveAs() {
//        Library lib = getSelectedLib();
//        if (lib == null) return;
//        FileMenu.saveAsLibraryCommand(lib);
//    }
//    private void selectedLibRename() {
//        Library lib = getSelectedLib();
//        if (lib == null) return;
//        CircuitChanges.renameLibrary(lib);
//    }
//    private void selectedLibClose() {
//        Library lib = getSelectedLib();
//        if (lib == null) return;
//        FileMenu.closeLibraryCommand(lib);
//    }

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
						"Don't you really want to place the icon " + iconCell.describe() + "?");
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
				wnd.zoomWindowToFitCellInstance(np);

			return (PlaceNodeListener)newListener;
		}
        return null;
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
			//this.window = window;
			this.toDraw = toDraw;
			this.oldListener = oldListener;
			this.oldCursor = oldCursor;
			this.textNode = null;
			this.makePort = false;
            this.palette = palette;

            //if (window != null) {
                //window.addKeyListener(this);
            //}
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

			String descript = "Create ";
			if (np instanceof Cell) descript += ((Cell)np).noLibDescribe(); else
				descript += np.getName() + " Primitive";
            wnd.getHighlighter().clear();
			PlaceNewNode job = new PlaceNewNode(descript, toDraw, where, cell, textNode, makePort);

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
            //if (window != null)
            //{
            //    window.removeKeyListener(this);
            //}
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

	/** class that creates the node selected from the component menu */
	public static class PlaceNewNode extends Job
	{
		Object toDraw;
		Point2D where;
		Cell cell;
		String varName;
		boolean export;

		public PlaceNewNode(String description, Object toDraw, Point2D where, Cell cell, String varName, boolean export)
		{
			super(description, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.toDraw = toDraw;
			this.where = where;
			this.cell = cell;
			this.varName = varName;
			this.export = export;
			startJob();
		}

		public boolean doIt()
		{
            EditWindow wnd = EditWindow.getCurrent();
            Highlighter highlighter = wnd.getHighlighter();

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
			if (np == null) return false;
			double width = np.getDefWidth();
			double height = np.getDefHeight();
			if (varName != null) width = height = 0;

			// get default creation angle
			int defAngle = 0;
			int techBits = 0;
			if (ni != null)
			{
				defAngle = ni.getAngle();
				techBits = ni.getTechSpecific();
			} else if (np instanceof PrimitiveNode)
			{
				defAngle = ((PrimitiveNode)np).getDefPlacementAngle();
				if (defAngle >= 3600)
				{
					defAngle %= 3600;
					width = -width;
				}
			}

			NodeInst newNi = NodeInst.makeInstance(np, where, width, height, cell, defAngle, null, techBits);
			if (newNi == null) return false;
			if (np == Generic.tech.cellCenterNode || np == Generic.tech.essentialBoundsNode)
				newNi.setHardSelect();
			if (varName != null)
			{
				// text object: add initial text
				Variable var = newNi.newVar(varName, "text");
				if (var != null)
				{
					var.setDisplay(true);
					MutableTextDescriptor td = MutableTextDescriptor.getAnnotationTextDescriptor();
//					if (!varName.equals("ART_message")) td.setDispPart(TextDescriptor.DispPos.NAMEVALUE);
					var.setTextDescriptor(td);
					Highlight h = highlighter.addText(newNi, cell, var, null);
				}
			} else
			{
				//if (ni != null) newNi.setTechSpecific(ni.getTechSpecific());
				if (np == Schematics.tech.resistorNode)
				{
					Variable var = newNi.newVar(Schematics.SCHEM_RESISTANCE, "100");
					var.setDisplay(true);
					MutableTextDescriptor td = MutableTextDescriptor.getNodeTextDescriptor();
					var.setTextDescriptor(td);
				} else if (np == Schematics.tech.capacitorNode)
				{
					Variable var = newNi.newVar(Schematics.SCHEM_CAPACITANCE, "100M");
					var.setDisplay(true);
					MutableTextDescriptor td = MutableTextDescriptor.getNodeTextDescriptor();
					var.setTextDescriptor(td);
				} else if (np == Schematics.tech.inductorNode)
				{
					Variable var = newNi.newVar(Schematics.SCHEM_INDUCTANCE, "100");
					var.setDisplay(true);
					MutableTextDescriptor td = MutableTextDescriptor.getNodeTextDescriptor();
					var.setTextDescriptor(td);
				} else if (np == Schematics.tech.diodeNode)
				{
					Variable var = newNi.newVar(Schematics.SCHEM_DIODE, "10");
					var.setDisplay(true);
					MutableTextDescriptor td = MutableTextDescriptor.getNodeTextDescriptor();
					var.setTextDescriptor(td);
				} else if (np == Schematics.tech.transistorNode || np == Schematics.tech.transistor4Node)
				{
					if (newNi.isFET())
					{
						Variable var = newNi.newVar(Schematics.ATTR_WIDTH, "2");
						var.setDisplay(true);
						MutableTextDescriptor td = MutableTextDescriptor.getNodeTextDescriptor();
						td.setOff(0.5, -1);
						var.setTextDescriptor(td);

						var = newNi.newVar(Schematics.ATTR_LENGTH, "2");
						var.setDisplay(true);

						td = MutableTextDescriptor.getNodeTextDescriptor();
						td.setOff(-0.5, -1);
						if (td.getSize().isAbsolute())
							td.setAbsSize((int)(td.getSize().getSize() - 2));
						else
							td.setRelSize(td.getSize().getSize() - 0.5);
						var.setTextDescriptor(td);
					} else
					{
						Variable var = newNi.newVar(Schematics.ATTR_AREA, "10");
						var.setDisplay(true);
						MutableTextDescriptor td = MutableTextDescriptor.getNodeTextDescriptor();
						var.setTextDescriptor(td);
					}
				} else if (np == Artwork.tech.circleNode)
				{
					if (ni != null)
					{
						double [] angles = ni.getArcDegrees();
						newNi.setArcDegrees(angles[0], angles[1]);
					}
				}
				ElectricObject eObj = newNi;
				if (newNi.getNumPortInsts() > 0) eObj = (ElectricObject)newNi.getPortInsts().next();
				highlighter.addElectricObject(eObj, cell);
			}

			// for technology edit cells, mark the new geometry specially
			if (cell.isInTechnologyLibrary())
			{
				Manipulate.completeNodeCreation(newNi, toDraw);
			}
			highlighter.finished();
			if (export)
			{
				ExportChanges.newExportCommand();
				System.out.println("SHOULD EXPORT IT NOW");
			}
			return true;
		}
	}
}
