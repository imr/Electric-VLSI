/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ToolBar.java
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
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Highlight2;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.dialogs.PreferencesFrame;
import com.sun.electric.tool.user.menus.EMenu;
import com.sun.electric.tool.user.menus.EMenuItem;
import com.sun.electric.tool.user.menus.FileMenu;
import com.sun.electric.tool.user.menus.MenuCommands;
import com.sun.electric.tool.Client;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JToolBar;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;


/**
 * This class manages the Electric toolbar.
 */
public class ToolBar extends JToolBar
{
	private ToolBar() {
		setFloatable(true);
		setRollover(true);

        EToolBarButton[] buttons = new EToolBarButton[] {
            openLibraryCommand,
            saveLibraryCommand,
            null,
            clickZoomWireCommand,
            panCommand,
            zoomCommand,
            outlineCommand,
            measureCommand,
            null,
            fullArrowDistanceCommand,
            halfArrowDistanceCommand,
            quarterArrowDistanceCommand,
    		null,
            selectObjectsCommand,
            selectAreaCommand,
		    null,
            toggleSelectSpecialCommand,
		    null,
            preferencesCommand,
            null,
            undoCommand,
            redoCommand,
            null,
            goBackButton,
            goForwardButton,
            null,
            expandOneLevelCommand,
            unexpandOneLevelCommand
        };

        for (EToolBarButton b: buttons) {
            if (b == null) {
                addSeparator();
                continue;
            }
            AbstractButton j = b.genToolBarButton();
            add(j);
            j.setFocusable(false);
        }
        
        setFocusable(false);
    }

	/**
	 * Method to create the toolbar.
	 */
	public static ToolBar createToolBar() { return new ToolBar(); }
    
    // --------------------------- class EToolBarBuitton ---------------------------------------------------------

    /**
     * Generic tool bar button.
     */
    private abstract static class EToolBarButton extends EMenuItem {
        /**
         * Default icon for tool bar button instance.
         */
        private ImageIcon defaultIcon;
        
        /**
         * @param text the menu item's displayed text.  An "_" in the string
         * indicates the location of the "mnemonic" key for that entry.
         * @param accelerator the shortcut key, or null if none specified.
         * @param iconName filename without extension of default icon.
         */
        EToolBarButton(String text, KeyStroke accelerator, String iconName) {
            super(text, accelerator);
            this.defaultIcon = Resources.getResource(ToolBar.class, iconName + ".gif");
        }
        
        /**
         * @param text the menu item's displayed text.  An "_" in the string
         * indicates the location of the "mnemonic" key for that entry.
         * @param acceleratorChar the shortcut char.
         * @param iconName filename without extension of default icon.
         */
        EToolBarButton(String text, char acceleratorChar, String iconName) {
            super(text, acceleratorChar);
            this.defaultIcon = Resources.getResource(ToolBar.class, iconName + ".gif");
        }
        
        /**
         * Generates tool bar button item by this this generic EToolBarButton
         * @return generated instance.
         */
        AbstractButton genToolBarButton() {
            AbstractButton b = createToolBarButton();
            b.setToolTipText(getToolTipText());
            b.setIcon(defaultIcon);
            b.addActionListener(this);
            updateToolBarButton(b);
            return b;
        }
        
        /**
         * Creates fresh GUI instance of this generic EToolBarButton.
         * Override in subclasses.
         * @return GUI instance
         */
        AbstractButton createToolBarButton() { return new JButton(); }
        
        /**
         * Updates appearance of toll bar button instance after change of state.
         */
        void updateToolBarButton(AbstractButton item) {
            item.setEnabled(isEnabled());
            item.setSelected(isSelected());
            item.setToolTipText(getToolTipText());
        }
        
        @Override
        protected void registerItem() {
            super.registerItem();
            registerUpdatable();
        }
        
        @Override
        protected void updateButtons() {
            updateToolBarButtons();
        }
    }
    
    /**
     * Generic tool bar radio button.
     */
    private abstract static class EToolBarRadioButton extends EToolBarButton {
        EToolBarRadioButton(String text, KeyStroke accelerator, String iconName) { super(text, accelerator, iconName); }
        EToolBarRadioButton(String text, char acceleratorChar, String iconName) { super(text, acceleratorChar, iconName); }
        @Override protected JMenuItem createMenuItem()
        {
            if (Client.isOSMac())
                return new JMenuItem();
            return new JRadioButtonMenuItem();
        }
        @Override JToggleButton createToolBarButton() { return new JToggleButton(); }
    }
    
    // --------------------------- Load/Save Library ---------------------------------------------------------

    public static final EToolBarButton openLibraryCommand = new EToolBarButton("_Open Library...", 'O', "ButtonOpenLibrary") {
        @Override public void run() {
            FileMenu.openLibraryCommand();
        }
    };

    public static final EToolBarButton saveLibraryCommand = new EToolBarButton("Sa_ve Library", null, "ButtonSaveLibrary") {
        @Override public boolean isEnabled() { return Library.getCurrent() != null; }
        @Override public void run() {
            FileMenu.saveLibraryCommand(Library.getCurrent());
        }
    };
    
	public static void setSaveLibraryButton() {
        updateToolBarButtons();
	}
    
    // --------------------------- CursorMode staff ---------------------------------------------------------

	private static CursorMode curMode = CursorMode.CLICKZOOMWIRE;
    
   /**
	 * CursorMode is a typesafe enum class that describes the current editing mode (select, zoom, etc).
	 */
    public static enum CursorMode {
        /** Describes ClickZoomWire mode (does everything). */  CLICKZOOMWIRE,
//		/** Describes Selection mode (click and drag). */		SELECT("Toggle Select"),
//		/** Describes wiring mode (creating arcs). */			WIRE("Toggle Wiring"),
        /** Describes Panning mode (move window contents). */	PAN,
        /** Describes Zoom mode (scale window contents). */		ZOOM,
        /** Describes Outline edit mode. */						OUTLINE,
        /** Describes Measure mode. */							MEASURE;
        
//        public String toString() { return "CursorMode="+super.toString().toLowerCase(); }
    }
    
	static final Cursor zoomCursor = readCursor("CursorZoom.gif", 6, 6);
    static final Cursor zoomOutCursor = readCursor("CursorZoomOut.gif", 6, 6);
	static final Cursor panCursor = readCursor("CursorPan.gif", 8, 8);
	static final Cursor wiringCursor = readCursor("CursorWiring.gif", 0, 0);
	static final Cursor outlineCursor = readCursor("CursorOutline.gif", 0, 8);
	static final Cursor measureCursor = readCursor("CursorMeasure.gif", 0, 0);

	public static Cursor readCursor(String cursorName, int hotX, int hotY)
	{
		ImageIcon imageIcon = Resources.getResource(ToolBar.class, cursorName);
		Image image = imageIcon.getImage();
		int width = image.getWidth(null);
		int height = image.getHeight(null);
		Dimension bestSize = Toolkit.getDefaultToolkit().getBestCursorSize(width, height);
		int bestWidth = (int)bestSize.getWidth();
		int bestHeight = (int)bestSize.getHeight();
		if (bestWidth != 0 && bestHeight != 0)
		{
			if (bestWidth != width || bestHeight != height)
			{
				if (bestWidth > width && bestHeight > height)
				{
					// want a larger cursor, so just pad this one
					Image newImage = new BufferedImage(bestWidth, bestHeight, BufferedImage.TYPE_INT_ARGB);
					Graphics g = newImage.getGraphics();
					g.drawImage(image, (bestWidth-width)/2, (bestHeight-height)/2, null);
					image = newImage;
					hotX += (bestWidth-width)/2;
					hotY += (bestHeight-height)/2;
				} else
				{
					// want a smaller cursor, so scale this one
					image = image.getScaledInstance(bestWidth, bestHeight, 0);
					hotX = hotX * bestWidth / width;
					hotY = hotY * bestHeight / height;
				}
			}
		}
		Cursor cursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(hotX, hotY), cursorName);
		return cursor;
	}

	/**
	 * Method to tell which cursor mode is in effect.
	 * @return the current mode (select, pan, zoom, outline, measure).
	 */
	public static CursorMode getCursorMode() { return curMode; }

    private static void setCursorMode(CursorMode cm) {
        switch (cm) {
            case CLICKZOOMWIRE:
                checkLeavingOutlineMode();
                WindowFrame.setListener(ClickZoomWireListener.theOne);
                TopLevel.setCurrentCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                curMode = CursorMode.CLICKZOOMWIRE;
                break;
            case PAN:
                if (WindowFrame.getListener() == ZoomAndPanListener.theOne && curMode == CursorMode.PAN) {
                    // switch back to click zoom wire listener
                    setCursorMode(CursorMode.CLICKZOOMWIRE);
                    return;
                }
                WindowFrame.setListener(ZoomAndPanListener.theOne);
                //makeCursors();
                TopLevel.setCurrentCursor(panCursor);
                curMode = CursorMode.PAN;
                break;
            case ZOOM:
                if (WindowFrame.getListener() == ZoomAndPanListener.theOne && curMode == CursorMode.ZOOM) {
                    // switch back to click zoom wire listener
                    setCursorMode(CursorMode.CLICKZOOMWIRE);
                    return;
                }
                checkLeavingOutlineMode();
                WindowFrame.setListener(ZoomAndPanListener.theOne);
                TopLevel.setCurrentCursor(zoomCursor);
                curMode = CursorMode.ZOOM;
                break;
            case OUTLINE:
                if (WindowFrame.getListener() == OutlineListener.theOne) {
                    // switch back to click zoom wire listener
                    setCursorMode(CursorMode.CLICKZOOMWIRE);
                    return;
                }
                EditWindow wnd = EditWindow.needCurrent();
                if (wnd == null) return;
                Highlighter highlighter = wnd.getHighlighter();
                
                CursorMode oldMode = curMode;
                NodeInst ni = (NodeInst)highlighter.getOneElectricObject(NodeInst.class);
                if (ni == null) {
                    if (oldMode == CursorMode.OUTLINE) setCursorMode(CursorMode.CLICKZOOMWIRE); else
                        setCursorMode(oldMode);
                    return;
                }
                NodeProto np = ni.getProto();
                if (ni.isCellInstance() || !((PrimitiveNode)np).isHoldsOutline()) {
                    System.out.println("Sorry, " + np + " does not hold outline information");
                    if (oldMode == CursorMode.OUTLINE) setCursorMode(CursorMode.CLICKZOOMWIRE); else
                        setCursorMode(oldMode);
                    return;
                }
                
                if (WindowFrame.getListener() != OutlineListener.theOne)
                    OutlineListener.theOne.setNode(ni);
                WindowFrame.setListener(OutlineListener.theOne);
                TopLevel.setCurrentCursor(outlineCursor);
                curMode = CursorMode.OUTLINE;
                break;
            case MEASURE:
                if (WindowFrame.getListener() == MeasureListener.theOne) {
                    // switch back to click zoom wire listener
                    setCursorMode(CursorMode.CLICKZOOMWIRE);
                    return;
                }
                checkLeavingOutlineMode();
                MeasureListener.theOne.reset();
                WindowFrame.setListener(MeasureListener.theOne);
                TopLevel.setCurrentCursor(measureCursor);
                curMode = CursorMode.MEASURE;
                break;
        }
    }
    
    private static void checkLeavingOutlineMode()
    {
    	// if exiting outline-edit mode, turn off special display
        if (WindowFrame.getListener() == OutlineListener.theOne && curMode == CursorMode.OUTLINE)
    	{
            EditWindow wnd = EditWindow.needCurrent();
            if (wnd != null)
            {
            	Highlighter highlighter = wnd.getHighlighter();
            	NodeInst ni = (NodeInst)highlighter.getOneElectricObject(NodeInst.class);
        		if (ni != null)
        		{
        			Highlight2 high = highlighter.getOneHighlight();
        			if (high != null)
        			{
        				high.setPoint(-1);
        				wnd.repaint();
        			}
        		}
            }
    	}
    }

    private static final CursorModeButton clickZoomWireCommand = new CursorModeButton("Click/Zoom/Wire", 'S', "ButtonClickZoomWire", CursorMode.CLICKZOOMWIRE);
    private static final CursorModeButton panCommand = new CursorModeButton("Toggle Pan", 'P', "ButtonPan", CursorMode.PAN);
    private static final CursorModeButton zoomCommand = new CursorModeButton("Toggle Zoom", 'Z', "ButtonZoom", CursorMode.ZOOM);
    private static final CursorModeButton outlineCommand = new CursorModeButton("Toggle Outline Edit", 'Y', "ButtonOutline", CursorMode.OUTLINE);
    private static final CursorModeButton measureCommand = new CursorModeButton("Toggle Measure Distance", 'M', "ButtonMeasure", CursorMode.MEASURE);
    
    private static class CursorModeButton extends EToolBarRadioButton {
        private final CursorMode cm;
        CursorModeButton(String text, char acceleratorChar, String iconName, CursorMode cm) {
            super(text, KeyStroke.getKeyStroke(acceleratorChar, 0), iconName);
            this.cm = cm;
        }
        @Override public boolean isSelected() { return getCursorMode() == cm; }
        @Override public void run() { setCursorMode(cm); }
    }
        
   // --------------------------- ArrowDistance staff ---------------------------------------------------------

    /**
     * ArrowDisatance is a typesafe enum class that describes the distance that arrow keys move (full, half, or quarter).
     */
    public static enum ArrowDistance {
        /** Describes full grid unit motion. */				FULL(1.0, 0),
        /** Describes half grid unit motion. */				HALF(0.5, 1),
        /** Describes quarter grid unit motion. */			QUARTER(0.25, 2);
        
//        private final double amount;
        private final int position;

        private ArrowDistance(double amount, int pos) {
//            this.amount = amount;
            this.position = pos;
        }

        public boolean isSelected()
        {
            double[] vals = User.getAlignmentToGridVector();
            for (int i = 0; i < vals.length; i++)
            {
                if (vals[i] < 0)
                    return (i == position);
            }
            return false;
        }

        public void setAlignmentToGrid()
        {
            double[] vals = User.getAlignmentToGridVector();
            for (int i = 0; i < vals.length; i++)
            {
                if (i != position)
                    vals[i] = Math.abs(vals[i]);
                else
                    vals[i] = Math.abs(vals[i]) * -1;
            }
            User.setAlignmentToGridVector(vals);
        }

//        public double getDistance()
//        {
////            return amount;
//        }
//		public String toString() { return "ArrowDistance="+super.toString().toLowerCase(); }
    }

    /**
     * Method to signal ToolBar that gridAlignment changed
     */
    public static void setGridAligment() {
        updateToolBarButtons();
    }

    private static final ArrowDistanceButton fullArrowDistanceCommand = new ArrowDistanceButton("Full motion", 'F', "ButtonFull", ArrowDistance.FULL);
    private static final ArrowDistanceButton halfArrowDistanceCommand = new ArrowDistanceButton("Half motion", 'H', "ButtonHalf", ArrowDistance.HALF);
    private static final ArrowDistanceButton quarterArrowDistanceCommand = new ArrowDistanceButton("Quarter motion", "ButtonQuarter", ArrowDistance.QUARTER);
            
    private static class ArrowDistanceButton extends EToolBarRadioButton {
        private final ArrowDistance ad;
        
        ArrowDistanceButton(String text, String iconName, ArrowDistance ad) {
            super(text, null, iconName);
            this.ad = ad;
        }

        ArrowDistanceButton(String text, char acceleratorChar, String iconName, ArrowDistance ad) {
            super(text, KeyStroke.getKeyStroke(acceleratorChar, 0), iconName);
            this.ad = ad;
        }

        @Override public boolean isSelected()
        {
//            return User.getAlignmentToGrid() == ad.getDistance();
            return ad.isSelected();
        }
        @Override public void run()
        {
            ad.setAlignmentToGrid();
//            User.setAlignmentToGrid(ad.getDistance());
        }
    }
    
   // --------------------------- SelectMode staff ---------------------------------------------------------

	private static SelectMode curSelectMode = SelectMode.OBJECTS;
    
    /**
     * SelectMode is a typesafe enum class that describes the current selection modes (objects or area).
     */
    public static enum SelectMode {
        /** Describes Selection mode (click and drag). */		OBJECTS,
        /** Describes Selection mode (click and drag). */		AREA;
        
//        public String toString() { return "SelectMode="+super.toString().toLowerCase(); }
    }
    
	/**
	 * Method to tell what selection mode is in effect.
	 * @return the current selection mode (objects or area).
	 */
	public static SelectMode getSelectMode() { return curSelectMode; }

    private static void setSelectMode(SelectMode selectMode) {
        curSelectMode = selectMode;
    }
    
    private static final SelectModeButton selectObjectsCommand = new SelectModeButton("Select Objects", "ButtonObjects", SelectMode.OBJECTS);
    private static final SelectModeButton selectAreaCommand = new SelectModeButton("Select Area", "ButtonArea", SelectMode.AREA);
    
    public static class SelectModeButton extends EToolBarRadioButton {
        private final SelectMode sm;
        
        SelectModeButton(String text, String iconName, SelectMode sm) {
            super(text, null, iconName);
            this.sm = sm;
        }

        @Override public boolean isSelected() { return getSelectMode() == sm; }
        @Override public void run() { setSelectMode(sm); }
    }
    
   // --------------------------- SelectSpecial staff ---------------------------------------------------------

    private static boolean selectSpecial = false;
    
    /**
     * Returns state of "select special" button
     * @return true if select special button selected, false otherwise
     */
    public static boolean isSelectSpecial() { return selectSpecial; }

    /**
     * Method called to toggle the state of the "select special"
     * button.
     */
    private static void setSelectSpecial(boolean b) { selectSpecial = b; }
    
    private static final ImageIcon selectSpecialIconOn = Resources.getResource(ToolBar.class, "ButtonSelectSpecialOn.gif");
    private static final ImageIcon selectSpecialIconOff = Resources.getResource(ToolBar.class, "ButtonSelectSpecialOff.gif");

    private static EToolBarButton toggleSelectSpecialCommand = new EToolBarButton("Toggle Special Select", null, "ButtonSelectSpecialOff") {
        public boolean isSelected() { return isSelectSpecial(); }
        @Override protected JMenuItem createMenuItem() { return new JCheckBoxMenuItem(); }
        @Override AbstractButton createToolBarButton() { return new JToggleButton(); }
        @Override public void run() { setSelectSpecial(!isSelectSpecial()); }
        @Override void updateToolBarButton(AbstractButton item) {
            super.updateToolBarButton(item);
            item.setSelected(isSelected());
            item.setIcon(isSelected() ? selectSpecialIconOn : selectSpecialIconOff);
        }
    };
    
    // --------------------------- Modes submenu of Edit menu ---------------------------------------------------------

		// mnemonic keys available: ABCD FGHIJKL NOPQR TUVWXYZ
    public static final EMenu modesSubMenu = new EMenu("M_odes",
            
		// mnemonic keys available: ABCDEFGHIJKLMNOPQRSTUVWXYZ
        new EMenu("_Edit",
            clickZoomWireCommand,
            panCommand,
            zoomCommand,
            outlineCommand,
            measureCommand),
            
		// mnemonic keys available: ABCDEFGHIJKLMNOPQRSTUVWXYZ
        new EMenu("_Movement",
            fullArrowDistanceCommand,
            halfArrowDistanceCommand,
            quarterArrowDistanceCommand),
            
		// mnemonic keys available: ABCDEFGHIJKLMNOPQRSTUVWXYZ
        new EMenu("_Select",
                    selectObjectsCommand,
                    selectAreaCommand,
                    toggleSelectSpecialCommand));

    // --------------------------- Misc commands ---------------------------------------------------------

    public static final EToolBarButton preferencesCommand = new EToolBarButton("P_references...", null, "ButtonPreferences") {
        public void run() {
            PreferencesFrame.preferencesCommand();
        }
    };
                    
    public static final EToolBarButton expandOneLevelCommand = new EToolBarButton("_One Level Down", null, "ButtonExpand") {
        public void run() {
            CircuitChanges.DoExpandCommands(false, 1);
        }
    };
            
    public static final EToolBarButton unexpandOneLevelCommand = new EToolBarButton("_One Level Up", null, "ButtonUnexpand") {
        public void run() {
            CircuitChanges.DoExpandCommands(true, 1);
        }
    };
    
    // --------------------------- Undo/Redo staff ---------------------------------------------------------

    public static final EToolBarButton undoCommand = new EToolBarButton("_Undo", 'Z', "ButtonUndo") {
        public void run() {
            WindowFrame wf = WindowFrame.getCurrentWindowFrame();
            if (wf != null && wf.getContent() instanceof TextWindow) {
                // handle undo in text windows specially
                TextWindow tw = (TextWindow)wf.getContent();
                tw.undo();
            } else {
                // do database undo
                Undo.undo();
            }
        }
        public boolean isEnabled() { return UserInterfaceMain.getUndoEnabled(); }
    };
    
    public static final EToolBarButton redoCommand = new EToolBarButton("Re_do", 'Y', "ButtonRedo") {
        public void run() {
            WindowFrame wf = WindowFrame.getCurrentWindowFrame();
            if (wf != null && wf.getContent() instanceof TextWindow) {
                // handle undo in text windows specially
                TextWindow tw = (TextWindow)wf.getContent();
                tw.redo();
            } else {
                // do database undo
                Undo.redo();
            }
        }
        public boolean isEnabled() { return UserInterfaceMain.getRedoEnabled(); }
    };
                    
	public static void updateUndoRedoButtons(boolean undo, boolean redo) {
        updateToolBarButtons();
	}
    
   // --------------------------- CellHistory staff ---------------------------------------------------------

    /** Go back button */           private CellHistoryButton goBackButton = new CellHistoryButton("Go Back a Cell", "ButtonGoBack") {
        public void run() {
            WindowFrame wf = WindowFrame.getCurrentWindowFrame();
            if (wf != null) wf.cellHistoryGoBack();
        }
    };
    /** Go forward button */        private CellHistoryButton goForwardButton = new CellHistoryButton("Go Forward a Cell", "ButtonGoForward") {
        public void run() {
            WindowFrame wf = WindowFrame.getCurrentWindowFrame();
            if (wf != null) wf.cellHistoryGoForward();
        }
    };
        
    private static abstract class CellHistoryButton extends EToolBarButton implements MouseListener {
        private boolean enabled;
        CellHistoryButton(String text, String iconName) { super(text, null, iconName); }
        
        @Override AbstractButton genToolBarButton() {
            AbstractButton b = super.genToolBarButton();
            b.addMouseListener(this);
            return b;
        }
        
        public boolean isEnabled() { return enabled; }
        void setEnabled(boolean b) { enabled = b; }
        
        public void mouseClicked(MouseEvent e) {}
        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}
        public void mousePressed(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {
            AbstractButton b = (AbstractButton) e.getSource();
            if(ClickZoomWireListener.isRightMouse(e) && b.contains(e.getX(), e.getY()))
                showHistoryPopup(e);
        }
    }

    /**
     * Update CellHistory buttons on this ToolBar
     * @param backEnabled true to enable goBackButton.
     * @param forwardEnabled true toenable goForwardButton.
     */
    public void updateCellHistoryStatus(boolean backEnabled, boolean forwardEnabled) {
        goBackButton.setEnabled(backEnabled);
        goForwardButton.setEnabled(forwardEnabled);
        updateToolBarButtons();
    }
    
    private static void showHistoryPopup(MouseEvent e) {
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;
        List<WindowFrame.CellHistory> historyList = wf.getCellHistoryList();
        int location = wf.getCellHistoryLocation();

        JPopupMenu popup = new JPopupMenu();
        HashMap<Cell,Cell> listed = new HashMap<Cell,Cell>();
        for (int i=historyList.size()-1; i > -1; i--) {
        	WindowFrame.CellHistory entry = (WindowFrame.CellHistory)historyList.get(i);
            Cell cell = entry.getCell();
            // skip if already shown such a cell
			if (cell == null) continue;
            if (listed.get(cell) != null) continue;
            listed.put(cell, cell);

            boolean shown = (i == location);
            JMenuItem m = new JMenuItem(cell.noLibDescribe() + (shown? "  (shown)" : ""));
            m.addActionListener(new HistoryPopupAction(wf, i));
            popup.add(m);
        }
        Component invoker = e.getComponent();
        if (invoker != null) {
            popup.setInvoker(invoker);
            Point2D loc = invoker.getLocationOnScreen();
            popup.setLocation((int)loc.getX() + invoker.getWidth()/2, (int)loc.getY() + invoker.getHeight()/2);
        }
        popup.setVisible(true);

    }

    private static class HistoryPopupAction implements ActionListener {
        private final WindowFrame wf;
        private final int historyLocation;
        private HistoryPopupAction(WindowFrame wf, int loc) {
            this.wf = wf;
            this.historyLocation = loc;
        }
        public void actionPerformed(ActionEvent e) {
        	wf.setCellByHistory(historyLocation);
        }
    }

    // ----------------------------------------------------------------------------

    /**
     * Update associated ToolBarButtons on all toolbars und updatable menu items on all menubars
     */
    public static void updateToolBarButtons() {
        for (ToolBar toolBar: TopLevel.getToolBars()) {
            for (Component c: toolBar.getComponents()) {
                if (!(c instanceof AbstractButton)) continue;
                AbstractButton b = (AbstractButton)c;
                for (ActionListener a: b.getActionListeners()) {
                    if (a instanceof EToolBarButton)
                        ((EToolBarButton)a).updateToolBarButton(b);
                }
            }
        }
        MenuCommands.menuBar().updateAllButtons();
    }
    
    /**
     * Call when done with this toolBar to release its resources
     */
    public void finished() {}
}
