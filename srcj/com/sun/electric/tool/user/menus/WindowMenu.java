/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WindowMenu.java
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

package com.sun.electric.tool.user.menus;

import com.sun.electric.tool.user.ui.*;
import com.sun.electric.tool.user.dialogs.LayerVisibility;
import com.sun.electric.tool.user.User;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.EventListener;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Jun 23, 2004
 * Time: 11:43:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class WindowMenu {

    protected static void addWindowMenu(MenuBar menuBar) {
        MenuBar.MenuItem m;
		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        /****************************** THE WINDOW MENU ******************************/

        MenuBar.Menu windowMenu = new MenuBar.Menu("Window", 'W');
        menuBar.add(windowMenu);

        m = windowMenu.addMenuItem("Fill Display", KeyStroke.getKeyStroke('9', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { fullDisplay(); } });
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD9, buckyBit), null);
        m = windowMenu.addMenuItem("Redisplay Window", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.redrawDisplay(); } });
        m = windowMenu.addMenuItem("Zoom Out", KeyStroke.getKeyStroke('0', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { zoomOutDisplay(); } });
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, buckyBit), null);
        m = windowMenu.addMenuItem("Zoom In", KeyStroke.getKeyStroke('7', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { zoomInDisplay(); } });
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD7, buckyBit), null);

        MenuBar.Menu specialZoomSubMenu = new MenuBar.Menu("Special Zoom");
        windowMenu.add(specialZoomSubMenu);
        m = specialZoomSubMenu.addMenuItem("Focus on Highlighted", KeyStroke.getKeyStroke('F', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { focusOnHighlighted(); } });
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD5, buckyBit), null);
        m = specialZoomSubMenu.addMenuItem("Zoom Box", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { zoomBoxCommand(); }});
        specialZoomSubMenu.addMenuItem("Make Grid Just Visible", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { makeGridJustVisibleCommand(); }});
        specialZoomSubMenu.addMenuItem("Match Other Window", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { matchOtherWindowCommand(); }});

        windowMenu.addSeparator();

        m = windowMenu.addMenuItem("Pan Left", KeyStroke.getKeyStroke('4', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panX(WindowFrame.getCurrentWindowFrame(), 1); }});
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD4, buckyBit), null);
        m = windowMenu.addMenuItem("Pan Right", KeyStroke.getKeyStroke('6', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panX(WindowFrame.getCurrentWindowFrame(), -1); }});
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD6, buckyBit), null);
        m = windowMenu.addMenuItem("Pan Up", KeyStroke.getKeyStroke('8', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panY(WindowFrame.getCurrentWindowFrame(), -1); }});
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD8, buckyBit), null);
        m = windowMenu.addMenuItem("Pan Down", KeyStroke.getKeyStroke('2', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panY(WindowFrame.getCurrentWindowFrame(), 1); }});
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD2, buckyBit), null);

        MenuBar.Menu panningDistanceSubMenu = new MenuBar.Menu("Panning Distance");
        windowMenu.add(panningDistanceSubMenu);
        ButtonGroup windowPanGroup = new ButtonGroup();
        JMenuItem panSmall, panMedium, panLarge;
        panSmall = panningDistanceSubMenu.addRadioButton("Small", true, windowPanGroup, null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panningDistanceCommand(0.15); } });
        panMedium = panningDistanceSubMenu.addRadioButton("Medium", true, windowPanGroup, null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panningDistanceCommand(0.3); } });
        panLarge = panningDistanceSubMenu.addRadioButton("Large", true, windowPanGroup, null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panningDistanceCommand(0.6); } });
        panMedium.setSelected(true);

        MenuBar.Menu centerSubMenu = new MenuBar.Menu("Center");
        windowMenu.add(centerSubMenu);
        centerSubMenu.addMenuItem("Selection", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.centerSelection(); }});
        centerSubMenu.addMenuItem("Cursor", KeyStroke.getKeyStroke('5', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.centerCursor(e); }});

        windowMenu.addSeparator();

        windowMenu.addMenuItem("Toggle Grid", KeyStroke.getKeyStroke('G', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { toggleGridCommand(); } });

        windowMenu.addSeparator();

        MenuBar.Menu windowPartitionSubMenu = new MenuBar.Menu("Adjust Position");
        windowMenu.add(windowPartitionSubMenu);
        windowPartitionSubMenu.addMenuItem("Tile Horizontally", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { tileHorizontallyCommand(); }});
        windowPartitionSubMenu.addMenuItem("Tile Vertically", KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0),
            new ActionListener() { public void actionPerformed(ActionEvent e) { tileVerticallyCommand(); }});
        windowPartitionSubMenu.addMenuItem("Cascade", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { cascadeWindowsCommand(); }});
        windowMenu.addMenuItem("Close Window", KeyStroke.getKeyStroke(KeyEvent.VK_W, buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { WindowFrame curWF = WindowFrame.getCurrentWindowFrame();
                curWF.finished(); }});

        if (!TopLevel.isMDIMode()) {
            windowMenu.addSeparator();
            windowMenu.addMenuItem("Move to Other Display", null,
                new ActionListener() { public void actionPerformed(ActionEvent e) { moveToOtherDisplayCommand(); } });
        }

        windowMenu.addSeparator();

        windowMenu.addMenuItem("Layer Visibility...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { layerVisibilityCommand(); } });
        MenuBar.Menu colorSubMenu = new MenuBar.Menu("Color Schemes");
        windowMenu.add(colorSubMenu);
        colorSubMenu.addMenuItem("Restore Default Colors", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { defaultBackgroundCommand(); }});
        colorSubMenu.addMenuItem("Black Background Colors", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { blackBackgroundCommand(); }});
        colorSubMenu.addMenuItem("White Background Colors", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { whiteBackgroundCommand(); }});

        MenuBar.Menu messagesSubMenu = new MenuBar.Menu("Messages Window");
        windowMenu.add(messagesSubMenu);
        messagesSubMenu.addMenuItem("Save Messages", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { TopLevel.getMessagesWindow().save(); }});
        messagesSubMenu.addMenuItem("Clear", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { TopLevel.getMessagesWindow().clear(); }});

    }


    public static void fullDisplay()
    {
        // get the current frame
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;

        // make the circuit fill the window
        wf.getContent().fillScreen();
    }

    public static void zoomOutDisplay()
    {
        // get the current frame
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;

        // zoom out
        wf.getContent().zoomOutContents();
    }

    public static void zoomInDisplay()
    {
        // get the current frame
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;

        // zoom in
        wf.getContent().zoomInContents();
    }

    public static void zoomBoxCommand()
    {
        // only works with click zoom wire listener
        EventListener oldListener = WindowFrame.getListener();
        WindowFrame.setListener(ClickZoomWireListener.theOne);
        ClickZoomWireListener.theOne.zoomBoxSingleShot(oldListener);
    }

    /**
     * Method to make the current window's grid be just visible.
     * If it is zoomed-out beyond grid visibility, it is zoomed-in so that the grid is shown.
     * If it is zoomed-in such that the grid is not at minimum pitch,
     * it is zoomed-out so that the grid is barely able to fit.
     */
    public static void makeGridJustVisibleCommand()
    {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Rectangle2D displayable = wnd.displayableBounds();
        Dimension sz = wnd.getSize();
        double scaleX = wnd.getGridXSpacing() * sz.width / 5 / displayable.getWidth();
        double scaleY = wnd.getGridYSpacing() * sz.height / 5 / displayable.getHeight();
        double scale = Math.min(scaleX, scaleY);
        wnd.setScale(wnd.getScale() / scale);
        wnd.repaintContents(null);
    }

    /**
     * Method to zoom the current window so that its scale matches that of the "other" window.
     * For this to work, there must be exactly one other window shown.
     */
    public static void matchOtherWindowCommand()
    {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        int numOthers = 0;
        EditWindow other = null;
        for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
        {
            WindowFrame wf = (WindowFrame)it.next();
            if (wf.getContent() instanceof EditWindow)
            {
                EditWindow wfWnd = (EditWindow)wf.getContent();
                if (wfWnd == wnd) continue;
                numOthers++;
                other = wfWnd;
            }
        }
        if (numOthers != 1)
        {
            System.out.println("There must be exactly two windows in order for one to match the other");
            return;
        }
        wnd.setScale(other.getScale());
        wnd.repaintContents(null);
    }

    public static void focusOnHighlighted()
    {
        // get the current frame
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;

        // focus on highlighted
        wf.getContent().focusOnHighlighted();
    }

    /**
     * This method implements the command to toggle the display of the grid.
     */
    public static void toggleGridCommand()
    {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        wnd.setGrid(!wnd.isGrid());
    }

    /**
     * This method implements the command to tile the windows horizontally.
     */
    public static void tileHorizontallyCommand()
    {
        // get the overall area in which to work
        Rectangle tileArea = getWindowArea();

        // tile the windows in this area
        int numWindows = WindowFrame.getNumWindows();
        int windowHeight = tileArea.height / numWindows;
        int i=0;
        for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
        {
            WindowFrame wf = (WindowFrame)it.next();
            Rectangle windowArea = new Rectangle(tileArea.x, tileArea.y + i*windowHeight, tileArea.width, windowHeight);
            i++;
            wf.setWindowSize(windowArea);
        }
    }

    /**
     * This method implements the command to tile the windows vertically.
     */
    public static void tileVerticallyCommand()
    {
        // get the overall area in which to work
        Rectangle tileArea = getWindowArea();

        // tile the windows in this area
        int numWindows = WindowFrame.getNumWindows();
        int windowWidth = tileArea.width / numWindows;
        int i=0;
        for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
        {
            WindowFrame wf = (WindowFrame)it.next();
            Rectangle windowArea = new Rectangle(tileArea.x + i*windowWidth, tileArea.y, windowWidth, tileArea.height);
            i++;
            wf.setWindowSize(windowArea);
        }
    }

    /**
     * This method implements the command to tile the windows cascaded.
     */
    public static void cascadeWindowsCommand()
    {
        // cascade the windows in this area
        int numWindows = WindowFrame.getNumWindows();
        if (numWindows <= 1)
        {
            tileVerticallyCommand();
            return;
        }

        // get the overall area in which to work
        Rectangle tileArea = getWindowArea();
        int windowWidth = tileArea.width * 3 / 4;
        int windowHeight = tileArea.height * 3 / 4;
        int windowSpacing = Math.min(tileArea.width - windowWidth, tileArea.height - windowHeight) / (numWindows-1);
        int numRuns = 1;
        if (windowSpacing < 70)
        {
            numRuns = 70 / windowSpacing;
            if (70 % windowSpacing != 0) numRuns++;
            windowSpacing *= numRuns;
        }
        int windowXSpacing = (tileArea.width - windowWidth) / (numWindows-1) * numRuns;
        int windowYSpacing = (tileArea.height - windowHeight) / (numWindows-1) * numRuns;
        int i=0;
        for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
        {
            WindowFrame wf = (WindowFrame)it.next();
            int index = i / numRuns;
            Rectangle windowArea = new Rectangle(tileArea.x + index*windowXSpacing,
                tileArea.y + index*windowYSpacing, windowWidth, windowHeight);
            i++;
            wf.setWindowSize(windowArea);
        }
    }

    private static Rectangle getWindowArea()
    {
        // get the overall area in which to work
        Dimension sz = TopLevel.getScreenSize();
        Rectangle tileArea = new Rectangle(sz);

        // remove the tool palette
        PaletteFrame pf = TopLevel.getPaletteFrame();
        Rectangle pb = pf.getPaletteLocation();
        removeOccludingRectangle(tileArea, pb);

        // remove the messages window
        MessagesWindow mw = TopLevel.getMessagesWindow();
        Rectangle mb = mw.getMessagesLocation();
        removeOccludingRectangle(tileArea, mb);
        return tileArea;
    }

    private static void removeOccludingRectangle(Rectangle screen, Rectangle occluding)
    {
        int lX = (int)screen.getMinX();
        int hX = (int)screen.getMaxX();
        int lY = (int)screen.getMinY();
        int hY = (int)screen.getMaxY();
        if (occluding.width > occluding.height)
        {
            // horizontally occluding window
            if (occluding.getMaxY() - lY < hY - occluding.getMinY())
            {
                // occluding window on top
                lY = (int)occluding.getMaxY();
            } else
            {
                // occluding window on bottom
                hY = (int)occluding.getMinY();
            }
        } else
        {
            if (occluding.getMaxX() - lX < hX - occluding.getMinX())
            {
                // occluding window on left
                lX = (int)occluding.getMaxX();
            } else
            {
                // occluding window on right
                hX = (int)occluding.getMinX();
            }
        }
        screen.width = hX - lX;   screen.height = hY - lY;
        screen.x = lX;            screen.y = lY;
    }

    /**
     * This method implements the command to control Layer visibility.
     */
    public static void layerVisibilityCommand()
    {
         LayerVisibility dialog = new LayerVisibility(TopLevel.getCurrentJFrame(), false);
        dialog.setVisible(true);
    }

    /**
     * This method implements the command to set default colors.
     */
    public static void defaultBackgroundCommand()
    {
        User.setColorBackground(Color.LIGHT_GRAY.getRGB());
        User.setColorGrid(Color.BLACK.getRGB());
        User.setColorHighlight(Color.WHITE.getRGB());
        User.setColorPortHighlight(Color.YELLOW.getRGB());
        User.setColorText(Color.BLACK.getRGB());
        User.setColorInstanceOutline(Color.BLACK.getRGB());
        EditWindow.repaintAllContents();
        TopLevel.getPaletteFrame().loadForTechnology();
    }

    /**
     * This method implements the command to set colors so that there is a black background.
     */
    public static void blackBackgroundCommand()
    {
        User.setColorBackground(Color.BLACK.getRGB());
        User.setColorGrid(Color.WHITE.getRGB());
        User.setColorHighlight(Color.RED.getRGB());
        User.setColorPortHighlight(Color.YELLOW.getRGB());
        User.setColorText(Color.WHITE.getRGB());
        User.setColorInstanceOutline(Color.WHITE.getRGB());
        EditWindow.repaintAllContents();
        TopLevel.getPaletteFrame().loadForTechnology();
    }

    /**
     * This method implements the command to set colors so that there is a white background.
     */
    public static void whiteBackgroundCommand()
    {
        User.setColorBackground(Color.WHITE.getRGB());
        User.setColorGrid(Color.BLACK.getRGB());
        User.setColorHighlight(Color.RED.getRGB());
        User.setColorPortHighlight(Color.DARK_GRAY.getRGB());
        User.setColorText(Color.BLACK.getRGB());
        User.setColorInstanceOutline(Color.BLACK.getRGB());
        EditWindow.repaintAllContents();
        TopLevel.getPaletteFrame().loadForTechnology();
    }

    public static void moveToOtherDisplayCommand()
    {
        // this only works in SDI mode
        if (TopLevel.isMDIMode()) return;

        // find current screen
        WindowFrame curWF = WindowFrame.getCurrentWindowFrame();
        WindowContent content = curWF.getContent();
        GraphicsConfiguration curConfig = content.getPanel().getGraphicsConfiguration();
        GraphicsDevice curDevice = curConfig.getDevice();

        // get all screens
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        // find screen after current screen
        int i;
        for (i=0; i<gs.length; i++) {
            if (gs[i] == curDevice) break;
        }
        if (i == (gs.length - 1)) i = 0; else i++;      // go to next device

        curWF.moveEditWindow(gs[i].getDefaultConfiguration());
    }


}
