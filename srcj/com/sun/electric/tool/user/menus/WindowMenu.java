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

import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.MessagesStream;
import com.sun.electric.tool.user.dialogs.SetFocus;
import static com.sun.electric.tool.user.menus.EMenuItem.SEPARATOR;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.EditWindowFocusBrowser;
import com.sun.electric.tool.user.ui.MessagesWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.ZoomAndPanListener;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.EventListener;
import java.util.Iterator;
import javax.swing.KeyStroke;

/**
 * Class to handle the commands in the "Window" pulldown menu.
 */
public class WindowMenu {
    public static KeyStroke getCloseWindowAccelerator() { return EMenuItem.shortcut(KeyEvent.VK_W); }

    static EMenu makeMenu() {
        /****************************** THE WINDOW MENU ******************************/

		// mnemonic keys available: A         K     Q  T    Y 
        return new EMenu("_Window",

            new EMenuItem("_Fill Window", '9', KeyEvent.VK_NUMPAD9) { public void run() {
                fullDisplay(); }},
            new EMenuItem("Redisplay _Window") { public void run() {
                ZoomAndPanListener.redrawDisplay(); }},
            new EMenuItem("Zoom _Out", '0', KeyEvent.VK_NUMPAD0) { public void run() {
                zoomOutDisplay(); }},
            new EMenuItem("Zoom _In", '7', KeyEvent.VK_NUMPAD7) { public void run() {
                zoomInDisplay(); }},

		// mnemonic keys available: ABCDEF  IJKLMNOPQRSTUV XY 
            new EMenu("Special _Zoom",
                new EMenuItem("Focus on _Highlighted", 'F', KeyEvent.VK_NUMPAD5) { public void run() {
                    focusOnHighlighted(); }},
                new EMenuItem("_Zoom Box") { public void run() {
                    zoomBoxCommand(); }},
                new EMenuItem("Make _Grid Just Visible") { public void run() {
                    makeGridJustVisibleCommand(); }},
                new EMenuItem("Match Other _Window") { public void run() {
                    matchOtherWindowCommand(0); }}),

            SEPARATOR,

            new EMenuItem("Pan _Left", '4', KeyEvent.VK_NUMPAD4) { public void run() {
                ZoomAndPanListener.panXOrY(0, WindowFrame.getCurrentWindowFrame(), 1); }},
            new EMenuItem("Pan _Right", '6', KeyEvent.VK_NUMPAD6) { public void run() {
                ZoomAndPanListener.panXOrY(0, WindowFrame.getCurrentWindowFrame(), -1); }},
            new EMenuItem("Pan _Up",  '8', KeyEvent.VK_NUMPAD8) { public void run() {
                ZoomAndPanListener.panXOrY(1, WindowFrame.getCurrentWindowFrame(), -1); }},
            new EMenuItem("Pan _Down", '2', KeyEvent.VK_NUMPAD2) { public void run() {
                ZoomAndPanListener.panXOrY(1, WindowFrame.getCurrentWindowFrame(), 1); }},

		// mnemonic keys available: AB DEFGHIJKLMNOPQR TUVW  Z
            new EMenu("Special _Pan",
                new EMenuItem("Center _Selection") { public void run() {
                    ZoomAndPanListener.centerSelection(); }},
                new EMenuItem("Center _Cursor", '5') { public void run() {
                    ZoomAndPanListener.centerCursor(); }},
                new EMenuItem("Match Other Window in _X") { public void run() {
                    matchOtherWindowCommand(1); }},
                new EMenuItem("Match Other Window in _Y") { public void run() {
                    matchOtherWindowCommand(2); }}),

        //    new EMenuItem("Saved Views...") { public void run() {
        //        SavedViews.showSavedViewsDialog(); }},
            new EMenuItem("Go To Pre_vious Focus") { public void run() {
                goToPreviousSavedFocus(); }},
            new EMenuItem("Go To Ne_xt Focus") { public void run() {
                goToNextSavedFocus(); }},
            new EMenuItem("_Set Focus...") { public void run() {
			    SetFocus.showSetFocusDialog(); }},

            SEPARATOR,

            new EMenuItem("Toggle _Grid", 'G') { public void run() {
                toggleGridCommand(); }},

            SEPARATOR,

		// mnemonic keys available: AB DEFG IJKLMNOPQRSTU WXYZ
            new EMenu("Ad_just Position",
                new EMenuItem("Tile _Horizontally") { public void run() {
                    tileHorizontallyCommand(); }},
                new EMenuItem("Tile _Vertically", KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)) { public void run() {
                    tileVerticallyCommand(); }},
                new EMenuItem("_Cascade") { public void run() {
                    cascadeWindowsCommand(); }}),

            new EMenuItem("Clos_e Window", getCloseWindowAccelerator()) { public void run() {
                closeWindowCommand(); }},

            !TopLevel.isMDIMode() ? SEPARATOR : null,
            !TopLevel.isMDIMode() && getAllGraphicsDevices().length >= 2 ? new EMenuItem("Move to Ot_her Display") { public void run() {
                moveToOtherDisplayCommand(); }} : null,
			!TopLevel.isMDIMode() ? new EMenuItem("Remember Locatio_n of Display") { public void run() {
				    rememberDisplayLocation(); }} : null,
            SEPARATOR,

        // mnemonic keys available: A CDEFGHIJKLMNOPQ STUV XYZ
            new EMenu("_Color Schemes",
                new EMenuItem("_Restore Default Colors") { public void run() {
                    defaultBackgroundCommand(); }},
                new EMenuItem("_Black Background Colors") { public void run() {
                    blackBackgroundCommand(); }},
                new EMenuItem("_White Background Colors") { public void run() {
                    whiteBackgroundCommand(); }}),

		// mnemonic keys available: AB DE GHIJKLMNOPQR TUVWXYZ
            new EMenu("_Messages Window",
                new EMenuItem("_Save Messages...") { public void run() {
                    MessagesStream.getMessagesStream().save(); }},
                new EMenuItem("_Clear") { public void run() {
                    TopLevel.getMessagesWindow().clear(); }},
                new EMenuItem("Set F_ont...") { public void run() {
                    TopLevel.getMessagesWindow().selectFont(); }}),

            MenuCommands.makeExtraMenu("j3d.ui.J3DMenu"),
        
		// mnemonic keys available: ABCDEFGHIJK MNOPQ STUVWXYZ
            new EMenu("Side _Bar",
		        new EMenuItem("On _Left") { public void run() {
                    WindowFrame.setSideBarLocation(true); }},
		        new EMenuItem("On _Right") { public void run() {
                    WindowFrame.setSideBarLocation(false); }}));
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
        wnd.repaintContents(null, false);
    }

    /**
     * Method to adjust the current window so that it matches that of the "other" window.
     * For this to work, there must be exactly one other window shown.
     * @param how 0 to match scale; 1 to match in X; 2 to match in Y.
     */
    public static void matchOtherWindowCommand(int how)
    {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        int numOthers = 0;
        EditWindow other = null;
        for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
        {
            WindowFrame wf = it.next();
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
        switch (how)
        {
        	case 0:
        		wnd.setScale(other.getScale());
        		break;
        	case 1:
        		wnd.setOffset(new Point2D.Double(other.getOffset().getX(), wnd.getOffset().getY()));
        		break;
        	case 2:
        		wnd.setOffset(new Point2D.Double(wnd.getOffset().getX(), other.getOffset().getY()));
        		break;
        }
        wnd.repaintContents(null, false);
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
        // get the current frame
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;
        if (wf.getContent() instanceof EditWindow)
        {
        	EditWindow wnd = (EditWindow)wf.getContent();
	        if (wnd == null) return;
	        wnd.setGrid(!wnd.isGrid());
        } else if (wf.getContent() instanceof WaveformWindow)
        {
        	WaveformWindow ww = (WaveformWindow)wf.getContent();
        	ww.toggleGridPoints();
        } else
        {
        	System.out.println("Cannot draw a grid in this type of window");
        }
    }

    /**
     * This method implements the command to tile the windows horizontally.
     */
    public static void tileHorizontallyCommand()
    {
        // get the overall area in which to work
        Rectangle [] areas = getWindowAreas();

        // tile the windows in each area
        for(int j=0; j<areas.length; j++)
        {
        	Rectangle area = areas[j];

        	// see how many windows are on this screen
        	int count = 0;
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				Rectangle wfBounds = wf.getFrame().getBounds();
				int locX = (int)wfBounds.getCenterX();
				int locY = (int)wfBounds.getCenterY();
				if (locX >= area.x && locX < area.x+area.width &&
					locY >= area.y && locY < area.y+area.height) count++;
			}
			if (count == 0) continue;

			int windowHeight = area.height / count;
			count = 0;
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				Rectangle wfBounds = wf.getFrame().getBounds();
				int locX = (int)wfBounds.getCenterX();
				int locY = (int)wfBounds.getCenterY();
				if (locX >= area.x && locX < area.x+area.width &&
					locY >= area.y && locY < area.y+area.height)
				{
					Rectangle windowArea = new Rectangle(area.x, area.y + count*windowHeight, area.width, windowHeight);
					count++;
					wf.setWindowSize(windowArea);
				}
			}
        }
    }

    /**
     * This method implements the command to tile the windows vertically.
     */
    public static void tileVerticallyCommand()
    {
		// get the overall area in which to work
		Rectangle [] areas = getWindowAreas();

		// tile the windows in each area
		for(int j=0; j<areas.length; j++)
		{
			Rectangle area = areas[j];

			// see how many windows are on this screen
			int count = 0;
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				Rectangle wfBounds = wf.getFrame().getBounds();
				int locX = (int)wfBounds.getCenterX();
				int locY = (int)wfBounds.getCenterY();
				if (locX >= area.x && locX < area.x+area.width &&
					locY >= area.y && locY < area.y+area.height) count++;
			}
			if (count == 0) continue;

			int windowWidth = area.width / count;
			count = 0;
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				Rectangle wfBounds = wf.getFrame().getBounds();
				int locX = (int)wfBounds.getCenterX();
				int locY = (int)wfBounds.getCenterY();
				if (locX >= area.x && locX < area.x+area.width &&
					locY >= area.y && locY < area.y+area.height)
				{
					Rectangle windowArea = new Rectangle(area.x + count*windowWidth, area.y, windowWidth, area.height);
					count++;
					wf.setWindowSize(windowArea);
				}
			}
		}
    }

    /**
     * This method implements the command to tile the windows cascaded.
     */
    public static void cascadeWindowsCommand()
    {
		// get the overall area in which to work
		Rectangle [] areas = getWindowAreas();

		// tile the windows in each area
		for(int j=0; j<areas.length; j++)
		{
			Rectangle area = areas[j];

			// see how many windows are on this screen
			int count = 0;
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				Rectangle wfBounds = wf.getFrame().getBounds();
				int locX = (int)wfBounds.getCenterX();
				int locY = (int)wfBounds.getCenterY();
				if (locX >= area.x && locX < area.x+area.width &&
					locY >= area.y && locY < area.y+area.height) count++;
			}
			if (count == 0) continue;
			int numRuns = 1;
			int windowXSpacing = 0, windowYSpacing = 0;
			int windowWidth = area.width;
			int windowHeight = area.height;
			if (count > 1)
			{
				windowWidth = area.width * 3 / 4;
				windowHeight = area.height * 3 / 4;
				int windowSpacing = Math.min(area.width - windowWidth, area.height - windowHeight) / (count-1);
				if (windowSpacing < 70)
				{
					numRuns = 70 / windowSpacing;
					if (70 % windowSpacing != 0) numRuns++;
					windowSpacing *= numRuns;
				}
				windowXSpacing = (area.width - windowWidth) / (count-1) * numRuns;
				windowYSpacing = (area.height - windowHeight) / (count-1) * numRuns;
			}

			count = 0;
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				Rectangle wfBounds = wf.getFrame().getBounds();
				int locX = (int)wfBounds.getCenterX();
				int locY = (int)wfBounds.getCenterY();
				if (locX >= area.x && locX < area.x+area.width &&
					locY >= area.y && locY < area.y+area.height)
				{
					int index = count / numRuns;
					Rectangle windowArea = new Rectangle(area.x + index*windowXSpacing,
						area.y + index*windowYSpacing, windowWidth, windowHeight);
					count++;
					wf.setWindowSize(windowArea);
				}
			}
		}
    }

	private static void closeWindowCommand()
	{
		WindowFrame curWF = WindowFrame.getCurrentWindowFrame();
		curWF.finished();
	}

	private static Rectangle [] getWindowAreas()
    {
		Rectangle [] areas = null;
		if (TopLevel.isMDIMode())
		{
			TopLevel tl = TopLevel.getCurrentJFrame();
			Dimension sz = tl.getContentPane().getSize();
			areas = new Rectangle[1];
			areas[0] = new Rectangle(0, 0, sz.width, sz.height);
		} else
		{
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice [] gs = ge.getScreenDevices();
			areas = new Rectangle[gs.length];
			for (int j = 0; j < gs.length; j++)
			{
				GraphicsDevice gd = gs[j];
				GraphicsConfiguration gc = gd.getDefaultConfiguration();
				areas[j] = gc.getBounds();
			}
		}

        // remove the messages window
        MessagesWindow mw = TopLevel.getMessagesWindow();
        Rectangle mb = mw.getMessagesLocation();
        removeOccludingRectangle(areas, mb);
        return areas;
    }

    private static void removeOccludingRectangle(Rectangle [] areas, Rectangle occluding)
    {
		int cX = occluding.x + occluding.width/2;
		int cY = occluding.y + occluding.height/2;
    	for(int i=0; i<areas.length; i++)
    	{
			int lX = (int)areas[i].getMinX();
			int hX = (int)areas[i].getMaxX();
			int lY = (int)areas[i].getMinY();
			int hY = (int)areas[i].getMaxY();
			if (cX > lX && cX < hX && cY > lY && cY < hY)
			{
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
				areas[i].x = lX;   areas[i].width = hX - lX;
				areas[i].y = lY;   areas[i].height = hY - lY;
			}
    	}
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
		User.setColorWaveformBackground(Color.BLACK.getRGB());
		User.setColorWaveformForeground(Color.WHITE.getRGB());
		User.setColorWaveformStimuli(Color.RED.getRGB());

		// change the colors in the "Generic" technology
        Generic.setBackgroudColor(Color.BLACK);

        EditWindow.repaintAllContents();
        for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
        {
        	WindowFrame wf = it.next();
        	wf.loadComponentMenuForTechnology();
        }
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
		User.setColorWaveformBackground(Color.BLACK.getRGB());
		User.setColorWaveformForeground(Color.WHITE.getRGB());
		User.setColorWaveformStimuli(Color.RED.getRGB());

		// change the colors in the "Generic" technology
        Generic.setBackgroudColor(Color.WHITE);

		EditWindow.repaintAllContents();
        for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
        {
        	WindowFrame wf = it.next();
        	wf.loadComponentMenuForTechnology();
        }
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
		User.setColorWaveformBackground(Color.WHITE.getRGB());
		User.setColorWaveformForeground(Color.BLACK.getRGB());
		User.setColorWaveformStimuli(Color.RED.getRGB());

		// change the colors in the "Generic" technology
        Generic.setBackgroudColor(Color.BLACK);

        EditWindow.repaintAllContents();
        for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
        {
        	WindowFrame wf = it.next();
        	wf.loadComponentMenuForTechnology();
        }
    }

    private static GraphicsDevice [] getAllGraphicsDevices() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        return gs;
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
        GraphicsDevice[] gs = getAllGraphicsDevices();
        for (int j=0; j<gs.length; j++) {
            //System.out.println("Found GraphicsDevice: "+gs[j]+", type: "+gs[j].getType());
        }
        // find screen after current screen
        int i;
        for (i=0; i<gs.length; i++) {
            if (gs[i] == curDevice) break;
        }
        if (i == (gs.length - 1)) i = 0; else i++;      // go to next device

        curWF.moveEditWindow(gs[i].getDefaultConfiguration());
    }

	public static void rememberDisplayLocation()
    {
        // this only works in SDI mode
        if (TopLevel.isMDIMode()) return;

        // remember main window information
        WindowFrame curWF = WindowFrame.getCurrentWindowFrame();
		TopLevel tl = curWF.getFrame();
		Point pt = tl.getLocation();
		User.setDefaultWindowPos(pt);
		Dimension sz = tl.getSize();
		User.setDefaultWindowSize(sz);

		// remember messages information
        MessagesWindow mw = TopLevel.getMessagesWindow();
		Rectangle rect = mw.getMessagesLocation();
		User.setDefaultMessagesPos(new Point(rect.x, rect.y));
		User.setDefaultMessagesSize(new Dimension(rect.width, rect.height));
    }

    /**
     * Go to the previous saved view for the current Edit Window
     */
    public static void goToPreviousSavedFocus() {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        EditWindowFocusBrowser browser = wnd.getSavedFocusBrowser();
        browser.goBack();
    }

    /**
     * Go to the previous saved view for the current Edit Window
     */
    public static void goToNextSavedFocus() {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        EditWindowFocusBrowser browser = wnd.getSavedFocusBrowser();
        browser.goForward();
    }
}
