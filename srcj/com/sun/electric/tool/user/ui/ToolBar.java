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

import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.user.MenuCommands;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.dialogs.EditOptions;
import com.sun.electric.tool.user.dialogs.IOOptions;
import com.sun.electric.tool.user.dialogs.ToolOptions;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.Iterator;
import javax.swing.AbstractButton;
import javax.swing.JToolBar;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.ButtonGroup;
import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.InternalFrameEvent;


/**
 * This class manages the Electric toolbar.
 */
public class ToolBar extends JToolBar implements PropertyChangeListener, InternalFrameListener
{
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String selectAreaName = "Area";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String selectObjectsName = "Objects";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String moveFullName = "Full";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String moveHalfName = "Half";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String moveQuarterName = "Quarter";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String cursorClickZoomWireName = "Click/Zoom/Wire";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String cursorSelectName = "Select";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String cursorWiringName = "Wiring";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String cursorPanName = "Pan";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String cursorZoomName = "Zoom";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String cursorOutlineName = "Outline Edit";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String specialSelectName = "Special Select";

    /** Go back button */           private JButton goBackButton;
    /** Go forward button */        private JButton goForwardButton;

    public static final boolean secondaryInputModes = false;

    /**
	 * Mode is a typesafe enum class that describes the current editing mode (select, zoom, etc).
	 */
	public static class CursorMode
	{
		private String name;

		private CursorMode(String name) { this.name = name; }

		public String toString() { return "CursorMode="+name; }
        
        /** Describes ClickZoomWire mode (does everything). */  public static final CursorMode CLICKZOOMWIRE = new CursorMode("clickzoomwire");
		/** Describes Selection mode (click and drag). */		public static final CursorMode SELECT = new CursorMode("select");
		/** Describes wiring mode (creating arcs). */			public static final CursorMode WIRE = new CursorMode("wire");
		/** Describes Panning mode (move window contents). */	public static final CursorMode PAN = new CursorMode("pan");
		/** Describes Zoom mode (scale window contents). */		public static final CursorMode ZOOM = new CursorMode("zoom");
		/** Describes Outline edit mode. */						public static final CursorMode OUTLINE = new CursorMode("outline");
	}

	/**
	 * Mode is a typesafe enum class that describes the distance that arrow keys move (full, half, or quarter).
	 */
	public static class ArrowDistance
	{
		private String name;
		private double amount;

		private ArrowDistance(String name, double amount)
		{
			this.name = name;
			this.amount = amount;
		}

		public double getDistance() { return amount; }
		public String toString() { return "ArrowDistance="+name; }

		/** Describes full grid unit motion. */				public static final ArrowDistance FULL = new ArrowDistance("full", 1.0);
		/** Describes half grid unit motion. */				public static final ArrowDistance HALF = new ArrowDistance("half", 0.5);
		/** Describes quarter grid unit motion. */			public static final ArrowDistance QUARTER = new ArrowDistance("quarter", 0.25);
	}

	/**
	 * SelectMode is a typesafe enum class that describes the current selection modes (objects or area).
	 */
	public static class SelectMode
	{
		private String name;

		private SelectMode(String name) { this.name = name; }

		public String toString() { return "SelectMode="+name; }

		/** Describes Selection mode (click and drag). */		public static final SelectMode OBJECTS = new SelectMode("objects");
		/** Describes Selection mode (click and drag). */		public static final SelectMode AREA = new SelectMode("area");
	}

	private static CursorMode curMode = CursorMode.CLICKZOOMWIRE;
	private static ArrowDistance curArrowDistance = ArrowDistance.FULL;
	private static SelectMode curSelectMode = SelectMode.OBJECTS;

	public static Cursor zoomCursor = readCursor("CursorZoom.gif", 6, 6);
    public static Cursor zoomOutCursor = ToolBar.readCursor("CursorZoomOut.gif", 6, 6);
	public static Cursor panCursor = readCursor("CursorPan.gif", 8, 8);
	public static Cursor wiringCursor = readCursor("CursorWiring.gif", 0, 0);
	public static Cursor outlineCursor = readCursor("CursorOutline.gif", 0, 0);

    public static final ImageIcon selectSpecialIconOn = new ImageIcon(ToolBar.class.getResource("ButtonSelectSpecialOn.gif"));
    public static final ImageIcon selectSpecialIconOff = new ImageIcon(ToolBar.class.getResource("ButtonSelectSpecialOff.gif"));
    
	private ToolBar() {}

	/**
	 * Method to create the toolbar.
	 */
	public static ToolBar createToolBar()
	{
		// create the toolbar
		ToolBar toolbar = new ToolBar();
		toolbar.setFloatable(true);
		toolbar.setRollover(true);

        ToolBarButton clickZoomWireButton, selectButton, wireButton, panButton, zoomButton, outlineButton;
        ToolBarButton fullButton, halfButton, quarterButton;
        ToolBarButton objectsButton, areaButton;
        ToolBarButton selectSpecialButton;
        ButtonGroup modeGroup, arrowGroup, selectGroup;

		// the "Select mode" button
		modeGroup = new ButtonGroup();

        clickZoomWireButton = ToolBarButton.newInstance(cursorClickZoomWireName,
            new ImageIcon(toolbar.getClass().getResource("ButtonClickZoomWire.gif")));
        clickZoomWireButton.addActionListener(
            new ActionListener() { public void actionPerformed(ActionEvent e) { clickZoomWireCommand(); } });
        clickZoomWireButton.setToolTipText("ClickZoomWire");
        clickZoomWireButton.setSelected(true);
        toolbar.add(clickZoomWireButton);
        modeGroup.add(clickZoomWireButton);

        if (secondaryInputModes) {

		selectButton = ToolBarButton.newInstance(cursorSelectName,
            new ImageIcon(toolbar.getClass().getResource("ButtonSelect.gif")));
		selectButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectCommand(); } });
		selectButton.setToolTipText("Select");
		toolbar.add(selectButton);
		modeGroup.add(selectButton);

		// the "Wiring" button
		wireButton = ToolBarButton.newInstance(cursorWiringName,
            new ImageIcon(toolbar.getClass().getResource("ButtonWiring.gif")));
		wireButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { wiringCommand(); } });
		wireButton.setToolTipText("Wiring");
		toolbar.add(wireButton);
		modeGroup.add(wireButton);

        }

		// the "Pan mode" button
		panButton = ToolBarButton.newInstance(cursorPanName,
            new ImageIcon(toolbar.getClass().getResource("ButtonPan.gif")));
		panButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { panCommand(); } });
		panButton.setToolTipText("Pan");
		toolbar.add(panButton);
		modeGroup.add(panButton);

		// the "Zoom mode" button
		zoomButton = ToolBarButton.newInstance(cursorZoomName,
            new ImageIcon(toolbar.getClass().getResource("ButtonZoom.gif")));
		zoomButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { zoomCommand(); } });
		zoomButton.setToolTipText("Zoom");
		toolbar.add(zoomButton);
		modeGroup.add(zoomButton);

		// the "Outline edit mode" button
		outlineButton = ToolBarButton.newInstance(cursorOutlineName,
            new ImageIcon(toolbar.getClass().getResource("ButtonOutline.gif")));
		outlineButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { outlineEditCommand(); } });
		outlineButton.setToolTipText("Outline Edit");
		toolbar.add(outlineButton);
		modeGroup.add(outlineButton);

		// a separator
		toolbar.addSeparator();

		// the "Full arrow distance" button
		arrowGroup = new ButtonGroup();
		fullButton = ToolBarButton.newInstance(moveFullName,
            new ImageIcon(toolbar.getClass().getResource("ButtonFull.gif")));
		fullButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { fullArrowDistanceCommand(); } });
		fullButton.setToolTipText("Full motion");
		fullButton.setSelected(true);
		toolbar.add(fullButton);
		arrowGroup.add(fullButton);

		// the "Half arrow distance" button
        halfButton = ToolBarButton.newInstance(moveHalfName,
            new ImageIcon(toolbar.getClass().getResource("ButtonHalf.gif")));
		halfButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { halfArrowDistanceCommand(); } });
		halfButton.setToolTipText("Half motion");
		toolbar.add(halfButton);
		arrowGroup.add(halfButton);

		// the "Quarter arrow distance" button
		quarterButton = ToolBarButton.newInstance(moveQuarterName,
            new ImageIcon(toolbar.getClass().getResource("ButtonQuarter.gif")));
		quarterButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { quarterArrowDistanceCommand(); } });
		quarterButton.setToolTipText("Quarter motion");
		toolbar.add(quarterButton);
		arrowGroup.add(quarterButton);

		// a separator
		toolbar.addSeparator();

        // the "Select Objects" button
		selectGroup = new ButtonGroup();
		objectsButton = ToolBarButton.newInstance(selectObjectsName,
            new ImageIcon(toolbar.getClass().getResource("ButtonObjects.gif")));
		objectsButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectObjectsCommand(); } });
		objectsButton.setToolTipText("Select Objects");
		objectsButton.setSelected(true);
		toolbar.add(objectsButton);
		selectGroup.add(objectsButton);

        // the "Select Area" button
		areaButton = ToolBarButton.newInstance(selectAreaName,
            new ImageIcon(toolbar.getClass().getResource("ButtonArea.gif")));
		areaButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectAreaCommand(); } });
		areaButton.setToolTipText("Select Area");
		toolbar.add(areaButton);
		selectGroup.add(areaButton);

		// the "Special select mode" button
        // this is a true toggle button, default is no special select
		selectSpecialButton = ToolBarButton.newInstance(specialSelectName, selectSpecialIconOff);
		selectSpecialButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { toggleSelectSpecialCommand(e); } });
		selectSpecialButton.setToolTipText("Toggle Special Select");
		toolbar.add(selectSpecialButton);

		// a separator
		toolbar.addSeparator();

		// the "Options" buttons
		ToolBarButton editOptionButton = ToolBarButton.newInstance("Edit Options",
            new ImageIcon(toolbar.getClass().getResource("ButtonOptionEdit.gif")));
		editOptionButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { EditOptions.editOptionsCommand(); } });
		editOptionButton.setToolTipText("Edit Options");
        editOptionButton.setModel(new javax.swing.DefaultButtonModel());  // this de-highlights the button after it is released
        toolbar.add(editOptionButton);


		ToolBarButton toolOptionButton = ToolBarButton.newInstance("Tool Options",
            new ImageIcon(toolbar.getClass().getResource("ButtonOptionTool.gif")));
		toolOptionButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { ToolOptions.toolOptionsCommand(); } });
		toolOptionButton.setToolTipText("Tool Options");
        toolOptionButton.setModel(new javax.swing.DefaultButtonModel());  // this de-highlights the button after it is released
		toolbar.add(toolOptionButton);

		ToolBarButton ioOptionButton = ToolBarButton.newInstance("I/O Options",
            new ImageIcon(toolbar.getClass().getResource("ButtonOptionIO.gif")));
		ioOptionButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { IOOptions.ioOptionsCommand(); } });
		ioOptionButton.setToolTipText("I/O Options");
        ioOptionButton.setModel(new javax.swing.DefaultButtonModel());  // this de-highlights the button after it is released
		toolbar.add(ioOptionButton);

        toolbar.addSeparator();

        toolbar.goBackButton = new JButton(new ImageIcon(toolbar.getClass().getResource("ButtonGoBack.gif")));
        toolbar.goBackButton.addActionListener(
            new ActionListener() { public void actionPerformed(ActionEvent e) { goBackButtonCommand(); } });
        toolbar.goBackButton.setToolTipText("Go Back a Cell");
        toolbar.goBackButton.setEnabled(false);
        toolbar.add(toolbar.goBackButton);

        toolbar.goForwardButton = new JButton(new ImageIcon(toolbar.getClass().getResource("ButtonGoForward.gif")));
        toolbar.goForwardButton.addActionListener(
            new ActionListener() { public void actionPerformed(ActionEvent e) { goForwardButtonCommand(); } });
        toolbar.goForwardButton.setToolTipText("Go Forward a Cell");
        toolbar.goForwardButton.setEnabled(false);
        toolbar.add(toolbar.goForwardButton);


		// a separator
		toolbar.addSeparator();

		// the "Expanded" button
		ToolBarButton expandButton = ToolBarButton.newInstance("Expand Cell Instances",
            new ImageIcon(toolbar.getClass().getResource("ButtonExpand.gif")));
		expandButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { MenuCommands.expandOneLevelDownCommand(); } });
		expandButton.setToolTipText("Expand Cell Instances");
        expandButton.setModel(new javax.swing.DefaultButtonModel());  // this de-highlights the button after it is released
		toolbar.add(expandButton);

		// the "Unexpanded" button
		ToolBarButton unExpandButton = ToolBarButton.newInstance("Unexpand Cell Instances",
            new ImageIcon(toolbar.getClass().getResource("ButtonUnexpand.gif")));
		unExpandButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { MenuCommands.unexpandOneLevelUpCommand(); } });
		unExpandButton.setToolTipText("Unexpand Cell Instances");
        unExpandButton.setModel(new javax.swing.DefaultButtonModel());  // this de-highlights the button after it is released
		toolbar.add(unExpandButton);

		// return the toolbar
		return toolbar;
	}

    // ------------------------------------------------------------------------------------

	public static Cursor readCursor(String cursorName, int hotX, int hotY)
	{
		ImageIcon imageIcon = new ImageIcon(ToolBar.class.getResource(cursorName));
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

    public static void clickZoomWireCommand()
    {
        WindowFrame.setListener(ClickZoomWireListener.theOne);
        TopLevel.setCurrentCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        curMode = CursorMode.CLICKZOOMWIRE;
    }

	/**
	 * Method called when the "select" button is pressed.
	 */
	public static void selectCommand()
	{
		WindowFrame.setListener(ClickAndDragListener.theOne);
		TopLevel.setCurrentCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		curMode = CursorMode.SELECT;
	}

	/**
	 * Method called when the "wiring" button is pressed.
	 */
	public static void wiringCommand()
	{
		WindowFrame.setListener(WiringListener.theOne);
		//makeCursors();
		TopLevel.setCurrentCursor(wiringCursor);
		curMode = CursorMode.WIRE;
	}

    /**
     * Method called to toggle the state of the "select special"
     * button.
     */
    public static void toggleSelectSpecialCommand(ActionEvent e)
    {
        // get new state of special select to set icon
        AbstractButton b = (AbstractButton)e.getSource();
        if (b.isSelected()) {
            ToolBarButton.setIconForButton(specialSelectName, selectSpecialIconOn);
        } else {
            ToolBarButton.setIconForButton(specialSelectName, selectSpecialIconOff);
        }
    }
    
	/**
	 * Method called when the "pan" button is pressed.
	 */
	public static void panCommand()
	{
		WindowFrame.setListener(ZoomAndPanListener.theOne);
		//makeCursors();
		TopLevel.setCurrentCursor(panCursor);
		curMode = CursorMode.PAN;
	}

	/**
	 * Method called when the "zoom" button is pressed.
	 */
	public static void zoomCommand()
	{
		WindowFrame.setListener(ZoomAndPanListener.theOne);
		TopLevel.setCurrentCursor(zoomCursor);
		curMode = CursorMode.ZOOM;
	}

	/**
	 * Method called when the "outline edit" button is pressed.
	 */
	public static void outlineEditCommand()
	{
        CursorMode oldMode = curMode;
		NodeInst ni = (NodeInst)Highlight.getOneElectricObject(NodeInst.class);
		if (ni == null)
		{
			System.out.println("Must first select a node with outline capabilities");
			if (oldMode == CursorMode.OUTLINE) selectCommand(); else
                setCursorMode(oldMode);
			return;
		}
		if (!ni.getProto().isHoldsOutline())
		{
			System.out.println("Cannot edit outline information on " + ni.getProto().describe() + " nodes");
            if (oldMode == CursorMode.OUTLINE) selectCommand(); else
                setCursorMode(oldMode);
			return;
		}

		if (WindowFrame.getListener() != OutlineListener.theOne)
			OutlineListener.theOne.setNode(ni);
		WindowFrame.setListener(OutlineListener.theOne);
		TopLevel.setCurrentCursor(outlineCursor);
		curMode = CursorMode.OUTLINE;
	}

    /**
     * Set cursor mode
     * @param mode the CursorMode corresponding to the desired mode
     */
    public static void setCursorMode(CursorMode mode)
    {
        if (mode == CursorMode.CLICKZOOMWIRE) ToolBarButton.doClick(cursorClickZoomWireName);
        else if (mode == CursorMode.SELECT) ToolBarButton.doClick(cursorSelectName);
        else if (mode == CursorMode.WIRE) ToolBarButton.doClick(cursorWiringName);
        else if (mode == CursorMode.PAN) ToolBarButton.doClick(cursorPanName);
        else if (mode == CursorMode.ZOOM) ToolBarButton.doClick(cursorZoomName);
        else if (mode == CursorMode.OUTLINE) ToolBarButton.doClick(cursorOutlineName);
    }

	/**
	 * Method to tell which cursor mode is in effect.
	 * @return the current mode (select, special-select, pan, zoom, or outline).
	 */
	public static CursorMode getCursorMode() { return curMode; }

	/**
	 * Method called when the "full arrow distance" button is pressed.
	 */
	public static void fullArrowDistanceCommand()
	{
		curArrowDistance = ArrowDistance.FULL;
	}

	/**
	 * Method called when the "half arrow distance" button is pressed.
	 */
	public static void halfArrowDistanceCommand()
	{
		curArrowDistance = ArrowDistance.HALF;
	}

	/**
	 * Method called when the "quarter arrow distance" button is pressed.
	 */
	public static void quarterArrowDistanceCommand()
	{
		curArrowDistance = ArrowDistance.QUARTER;
	}

	/**
	 * Method to tell what arrow distance is in effect.
	 * @return the current arrow distance (1.0, 0.5, or 0.25).
	 */
	public static double getArrowDistance() { return curArrowDistance.getDistance(); }

	/**
	 * Method called when the "select objects" button is pressed.
	 */
	public static void selectObjectsCommand()
	{
		curSelectMode = SelectMode.OBJECTS;
	}

	/**
	 * Method called when the "select area" button is pressed.
	 */
	public static void selectAreaCommand()
	{
		curSelectMode = SelectMode.AREA;
	}

	/**
	 * Method to tell what selection mode is in effect.
	 * @return the current selection mode (objects or area).
	 */
	public static SelectMode getSelectMode() { return curSelectMode; }

    /**
     * Returns state of "select special" button
     * @return true if select special button selected, false otherwise
     */
    public static boolean getSelectSpecial() { return ToolBarButton.getButtonState(specialSelectName); }

    /**
     * Go back in the cell history
     */
    public static void goBackButtonCommand() {
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf != null) wf.getContent().cellHistoryGoBack();
    }

    /**
     * Go forward in the cell history
     */
    public static void goForwardButtonCommand() {
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf != null) wf.getContent().cellHistoryGoForward();
    }

    // ----------------------------------------------------------------------------

    /**
     * Call when done with this toolBar to release its resources
     */
    public void finished()
    {
        // find ToolBarButtons
        Component [] components = getComponents();
        for (int i=0; i<components.length; i++) {
            Component component = components[i];
            if (component instanceof ToolBarButton) {
                ToolBarButton b = (ToolBarButton)component;
                b.finished();
            }
        }
    }

    /**
     * Listen for property change events.  Currently this supports
     * events generated by an EditWindow when the ability to traverse
     * backwards or forwards through it's cell history changes.
     * @param evt The property change event
     */
    public void propertyChange(PropertyChangeEvent evt) {
        String name = evt.getPropertyName();
        if (name.equals(EditWindow.propGoBackEnabled)) {
            boolean enabled = ((Boolean)evt.getNewValue()).booleanValue();
            goBackButton.setEnabled(enabled);
        }
        if (name.equals(EditWindow.propGoForwardEnabled)) {
            boolean enabled = ((Boolean)evt.getNewValue()).booleanValue();
            goForwardButton.setEnabled(enabled);
        }
    }

    /**
     * Listen for InternalFrame activation events.  Currently this supports
     * when a JInternalFrame gets raised to front, we associate the forward
     * and back history buttons with the EditWindow in that frame
     * @param e the event.
     */
    public void internalFrameActivated(InternalFrameEvent e) {
        JInternalFrame source = e.getInternalFrame();
        if (source instanceof JInternalFrame) {
            // associate go back and forward states with that frame's window
            WindowFrame useFrame = null;
            for (Iterator it = WindowFrame.getWindows(); it.hasNext(); ) {
                WindowFrame frame = (WindowFrame)it.next();
                if (frame.getInternalFrame() == source) {
                    useFrame = frame;
                    break;
                }
            }
            if (useFrame == null) return;
			WindowContent content = useFrame.getContent();
            if (content == null) return;
            goBackButton.setEnabled(content.cellHistoryCanGoBack());
            goForwardButton.setEnabled(content.cellHistoryCanGoForward());
        }
    }

    public void internalFrameClosed(InternalFrameEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void internalFrameClosing(InternalFrameEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void internalFrameDeactivated(InternalFrameEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void internalFrameDeiconified(InternalFrameEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void internalFrameIconified(InternalFrameEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void internalFrameOpened(InternalFrameEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


}
