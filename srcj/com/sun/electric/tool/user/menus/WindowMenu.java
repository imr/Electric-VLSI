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
 * the Free Software Foundation; either version 3 of the License, or
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

import static com.sun.electric.tool.user.menus.EMenuItem.SEPARATOR;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.MessagesStream;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.dialogs.SetFocus;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.EditWindowFocusBrowser;
import com.sun.electric.tool.user.ui.InvisibleLayerConfiguration;
import com.sun.electric.tool.user.ui.LayerTab;
import com.sun.electric.tool.user.ui.MeasureListener;
import com.sun.electric.tool.user.ui.MessagesWindow;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.ZoomAndPanListener;
import com.sun.electric.tool.user.ui.ToolBar.CursorMode;
import com.sun.electric.tool.user.waveform.*;
import com.sun.electric.tool.simulation.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * Class to handle the commands in the "Window" pulldown menu.
 */
public class WindowMenu {
    public static KeyStroke getCloseWindowAccelerator() { return EMenuItem.shortcut(KeyEvent.VK_W); }
    private static EMenu thisWindowMenu = null;
    private static EMenu visibleLayersMenu = null;
    private static EMenuItem hiddenWindowCycleMenuItem = null;

    static EMenu makeMenu() {
        /****************************** THE WINDOW MENU ******************************/

        // bindings for numpad keys.  Need extra one because over VNC, they get sent as shift+KP_*
        int ctrlshift = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK;
        KeyStroke [] numpad4 = new KeyStroke [] { EMenuItem.shortcut('4'), EMenuItem.shortcut(KeyEvent.VK_NUMPAD4),
                KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, ctrlshift)};
        KeyStroke [] numpad6 = new KeyStroke [] { EMenuItem.shortcut('6'), EMenuItem.shortcut(KeyEvent.VK_NUMPAD6),
                KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, ctrlshift)};
        KeyStroke [] numpad8 = new KeyStroke [] { EMenuItem.shortcut('8'), EMenuItem.shortcut(KeyEvent.VK_NUMPAD8),
                KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, ctrlshift)};
        KeyStroke [] numpad2 = new KeyStroke [] { EMenuItem.shortcut('2'), EMenuItem.shortcut(KeyEvent.VK_NUMPAD2),
                KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, ctrlshift)};
        KeyStroke [] numpad9 = new KeyStroke [] { EMenuItem.shortcut('9'), EMenuItem.shortcut(KeyEvent.VK_NUMPAD9),
                KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, ctrlshift)};
        KeyStroke [] numpad7 = new KeyStroke [] { EMenuItem.shortcut('7'), EMenuItem.shortcut(KeyEvent.VK_NUMPAD7),
                KeyStroke.getKeyStroke(KeyEvent.VK_HOME, ctrlshift)};
        KeyStroke [] numpad0 = new KeyStroke [] { EMenuItem.shortcut('0'), EMenuItem.shortcut(KeyEvent.VK_NUMPAD0),
                KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, ctrlshift)};
        KeyStroke [] numpad5 = new KeyStroke [] { EMenuItem.shortcut('5'), EMenuItem.shortcut(KeyEvent.VK_NUMPAD5),
                KeyStroke.getKeyStroke(KeyEvent.VK_BEGIN, ctrlshift)};

	    visibleLayersMenu = new EMenu("Visible La_yers",
            new EMenuItem(InvisibleLayerConfiguration.getOnly().getMenuName(1), KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.SHIFT_MASK))
	    		{ public void run() { setLayerVisible(1); }},
            new EMenuItem(InvisibleLayerConfiguration.getOnly().getMenuName(2), KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.SHIFT_MASK))
                { public void run() { setLayerVisible(2); }},
            new EMenuItem(InvisibleLayerConfiguration.getOnly().getMenuName(3), KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.SHIFT_MASK))
            	{ public void run() { setLayerVisible(3); }},
            new EMenuItem(InvisibleLayerConfiguration.getOnly().getMenuName(4), KeyStroke.getKeyStroke(KeyEvent.VK_4, InputEvent.SHIFT_MASK))
            	{ public void run() { setLayerVisible(4); }},
            new EMenuItem(InvisibleLayerConfiguration.getOnly().getMenuName(5), KeyStroke.getKeyStroke(KeyEvent.VK_5, InputEvent.SHIFT_MASK))
            	{ public void run() { setLayerVisible(5); }},
            new EMenuItem(InvisibleLayerConfiguration.getOnly().getMenuName(6), KeyStroke.getKeyStroke(KeyEvent.VK_6, InputEvent.SHIFT_MASK))
            	{ public void run() { setLayerVisible(6); }},
            new EMenuItem(InvisibleLayerConfiguration.getOnly().getMenuName(7), KeyStroke.getKeyStroke(KeyEvent.VK_7, InputEvent.SHIFT_MASK))
            	{ public void run() { setLayerVisible(7); }},
            new EMenuItem(InvisibleLayerConfiguration.getOnly().getMenuName(8), KeyStroke.getKeyStroke(KeyEvent.VK_8, InputEvent.SHIFT_MASK))
            	{ public void run() { setLayerVisible(8); }},
            new EMenuItem(InvisibleLayerConfiguration.getOnly().getMenuName(9), KeyStroke.getKeyStroke(KeyEvent.VK_9, InputEvent.SHIFT_MASK))
            	{ public void run() { setLayerVisible(9); }},
            new EMenuItem(InvisibleLayerConfiguration.getOnly().getMenuName(10), KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK))
                { public void run() { setLayerVisible(10); }},
            new EMenuItem(InvisibleLayerConfiguration.getOnly().getMenuName(11), KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK))
                { public void run() { setLayerVisible(11); }},
            new EMenuItem(InvisibleLayerConfiguration.getOnly().getMenuName(12), KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK))
                { public void run() { setLayerVisible(12); }},
            new EMenuItem(InvisibleLayerConfiguration.getOnly().getMenuName(0), KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.SHIFT_MASK))
            	{ public void run() { setLayerVisible(0); }});

        // mnemonic keys available:           K     Q  T
	    EMenu menu = new EMenu("_Window",

            new EMenuItem("_Fill Window", numpad9) { public void run() {
                fullDisplay(); }},
            new EMenuItem("Redisplay _Window") { public void run() {
                ZoomAndPanListener.redrawDisplay(); }},
            new EMenuItem("Zoom _Out", numpad0) { public void run() {
                zoomOutDisplay(); }},
            new EMenuItem("Zoom _In", numpad7) { public void run() {
                zoomInDisplay(); }},

		// mnemonic keys available: ABCDEF  IJKLMNOPQRSTUV XY
            new EMenu("Special _Zoom",
                new EMenuItem("Focus on _Highlighted", 'F') { public void run() {
                    focusOnHighlighted(); }},
                new EMenuItem("_Zoom Box") { public void run() {
                    zoomBoxCommand(); }},
                new EMenuItem("Make _Grid Just Visible") { public void run() {
                    makeGridJustVisibleCommand(); }},
                new EMenuItem("Match Other _Window") { public void run() {
                    matchOtherWindowCommand(0); }}),

            SEPARATOR,

            new EMenuItem("Pan _Left", numpad4) { public void run() {
                ZoomAndPanListener.panXOrY(0, WindowFrame.getCurrentWindowFrame(), 1); }},
            new EMenuItem("Pan _Right", numpad6) { public void run() {
                ZoomAndPanListener.panXOrY(0, WindowFrame.getCurrentWindowFrame(), -1); }},
            new EMenuItem("Pan _Up",  numpad8) { public void run() {
                ZoomAndPanListener.panXOrY(1, WindowFrame.getCurrentWindowFrame(), -1); }},
            new EMenuItem("Pan _Down", numpad2) { public void run() {
                ZoomAndPanListener.panXOrY(1, WindowFrame.getCurrentWindowFrame(), 1); }},

		// mnemonic keys available:  B DEFGHIJKLMNOPQR TUVW  Z
            new EMenu("Special _Pan",
                new EMenuItem("Center _Selection") { public void run() {
                    ZoomAndPanListener.centerSelection(); }},
                new EMenuItem("Center _Cursor", numpad5) { public void run() {
                    ZoomAndPanListener.centerCursor(WindowFrame.getCurrentWindowFrame()); }},
                new EMenuItem("Match Other Window in _X") { public void run() {
                    matchOtherWindowCommand(1); }},
                new EMenuItem("Match Other Window in _Y") { public void run() {
                    matchOtherWindowCommand(2); }},
                new EMenuItem("Match Other Window in X, Y, _and Scale") { public void run() {
                    matchOtherWindowCommand(3); }}),

        //    new EMenuItem("Saved Views...") { public void run() {
        //        SavedViews.showSavedViewsDialog(); }},
            new EMenuItem("Go To Pre_vious Focus") { public void run() {
                goToPreviousSavedFocus(); }},
            new EMenuItem("Go To Ne_xt Focus") { public void run() {
                goToNextSavedFocus(); }},
            new EMenuItem("_Set Focus...") { public void run() {
			    SetFocus.showSetFocusDialog(); }},

            SEPARATOR,

    		// mnemonic keys available: AB DEFGHIJKLMNOPQRS UVWXYZ
            new EMenuItem("Toggle _Grid", 'G') { public void run() {
                toggleGridCommand(); }},
            new EMenu("Me_asurements",
                new EMenuItem("_Toggle Measurement Mode") { public void run() {
                	ToolBar.setCursorMode(CursorMode.MEASURE); }},
                new EMenuItem("_Clear Measurements") { public void run() {
                	MeasureListener.theOne.reset(); }}),

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

//            new EMenuItem("Select Next Window") { public void run() {
//                nextWindowCommand(); }},

            !TopLevel.isMDIMode() ? SEPARATOR : null,
            !TopLevel.isMDIMode() && getAllGraphicsDevices().length >= 2 ? new EMenuItem("Move to Ot_her Display") { public void run() {
                moveToOtherDisplayCommand(); }} : null,
			!TopLevel.isMDIMode() ? new EMenuItem("Remember Locatio_n of Display") { public void run() {
				    rememberDisplayLocation(); }} : null,

		    SEPARATOR,

		    visibleLayersMenu,

        // mnemonic keys available: A  DEFGHIJKLMNOPQ STUV XYZ
            new EMenu("_Color Schemes",
                new EMenuItem("_Restore Default Colors") { public void run() {
                    defaultBackgroundCommand(); }},
                new EMenuItem("_Black Background Colors") { public void run() {
                    blackBackgroundCommand(); }},
                new EMenuItem("_White Background Colors") { public void run() {
                    whiteBackgroundCommand(); }},
                new EMenuItem("_Cadence Colors, Layers and Keystrokes") { public void run() {
                    importCadencePreferences(); }}
                    ),

		// mnemonic keys available: AB   FGHIJKLMNO Q  TUVW  Z
            new EMenu("W_aveform Window",
		        new EMenuItem("_Save Waveform Window Configuration to Disk...") { public void run() {
                    WaveformWindow.saveConfiguration(); }},
		        new EMenuItem("_Restore Waveform Window Configuration from Disk...") { public void run() {
                    WaveformWindow.restoreConfiguration(); }},
                SEPARATOR,
                new EMenuItem("Refresh Simulation _Data",
                              Resources.getResource(WaveformWindow.class, "ButtonSimRefresh.gif")) { public void run() {
                    WaveformWindow.refreshSimulationData(); }},
                SEPARATOR,
                new EMenuItem("_Clear All Signals in Waveform Window") { public void run() {
                    WaveformWindow.clearSimulationData(); }},
                SEPARATOR,
                new EMenuItem("_Export Simulation Data...") { public void run() {
                    WaveformWindow.exportSimulationData(); }},
                new EMenuItem("_Export Simulation Data As CSV...") { public void run() {
                    WaveformWindow.exportSimulationDataAsCSV(OpenFile.chooseOutputFile((FileType)null, "Save Simulation Data as CSV", "simulation.csv")); }},
                new EMenuItem("_Plot Simulation Data On Screen") { public void run() {
                    WaveformWindow.plotSimulationData(null,null); }},
                new EMenuItem("Plot Simulation Data as PS...") { public void run() {
                    WaveformWindow.plotSimulationData(OpenFile.chooseOutputFile((FileType)null, "Save Plot as PS", "plot.ps"),"postscript eps color dashed");
                }},
                SEPARATOR,
                new EMenuItem("Fill Only in _X") { public void run() {
                    WaveformWindow.fillInX(); }},
                new EMenuItem("Fill Only in _Y") { public void run() {
                    WaveformWindow.fillInY(); }},
                SEPARATOR,
                new EMenuItem("Create New Waveform Panel",
                              Resources.getResource(WaveformWindow.class, "ButtonSimAddPanel.gif")) { public void run() {
                                  WaveformWindow.getCurrentWaveformWindow().makeNewPanel();
                    }},
                new EMenuItem("Toggle Horizontal Panel Lock",
                              Resources.getResource(WaveformWindow.class, "ButtonSimLockTime.gif")) { public void run() {
                                  WaveformWindow.getCurrentWaveformWindow().togglePanelXAxisLock();
                    }},
                SEPARATOR,
                new EMenuItem("Show Points and Lines",
                              Resources.getResource(WaveformWindow.class, "ButtonSimLineOnPointOn.gif")) { public void run() {
                                  WaveformWindow.getCurrentWaveformWindow().toggleShowPoints();
                    }},
                new EMenuItem("Show Lines",
                              Resources.getResource(WaveformWindow.class, "ButtonSimLineOnPointOff.gif")) { public void run() {
                                  WaveformWindow.getCurrentWaveformWindow().toggleShowPoints();
                    }},
                new EMenuItem("Show Points",
                              Resources.getResource(WaveformWindow.class, "ButtonSimLineOffPointOn.gif")) { public void run() {
                                  WaveformWindow.getCurrentWaveformWindow().toggleShowPoints();
                    }},
                SEPARATOR,
                new EMenuItem("Toggle Grid Points",
                              Resources.getResource(WaveformWindow.class, "ButtonSimGrid.gif")) { public void run() {
                                  WaveformWindow.getCurrentWaveformWindow().toggleGridPoints();
                    }},
                SEPARATOR,
                new EMenuItem("Increase Minimum Panel Height",
                              Resources.getResource(WaveformWindow.class, "ButtonSimGrow.gif")) { public void run() {
                                  WaveformWindow.getCurrentWaveformWindow().growPanels(1.25);
                    }},
                new EMenuItem("Decrease Minimum Panel Height",
                              Resources.getResource(WaveformWindow.class, "ButtonSimShrink.gif")) { public void run() {
                                  WaveformWindow.getCurrentWaveformWindow().growPanels(0.8);
                    }},
                SEPARATOR,
                new EMenuItem("Rewind Main X Axis Cursor to Start",
                              Resources.getResource(WaveformWindow.class, "ButtonVCRRewind.gif")) { public void run() {
                                  WaveformWindow.getCurrentWaveformWindow().vcrClickRewind();
                    }},
                new EMenuItem("Play Main X Axis Cursor Backwards",
                              Resources.getResource(WaveformWindow.class, "ButtonVCRPlayBackward.gif")) { public void run() {
                                  WaveformWindow.getCurrentWaveformWindow().vcrClickPlayBackwards();
                    }},
                new EMenuItem("Stop Moving Main X Axis Cursor",
                              Resources.getResource(WaveformWindow.class, "ButtonVCRStop.gif")) { public void run() {
                                  WaveformWindow.getCurrentWaveformWindow().vcrClickStop();
                    }},
                new EMenuItem("Play Main X Axis Cursor",
                              Resources.getResource(WaveformWindow.class, "ButtonVCRPlay.gif")) { public void run() {
                                  WaveformWindow.getCurrentWaveformWindow().vcrClickPlay();
                    }},
                new EMenuItem("Move Main X Axis Cursor to End",
                              Resources.getResource(WaveformWindow.class, "ButtonVCRToEnd.gif")) { public void run() {
                                  WaveformWindow.getCurrentWaveformWindow().vcrClickToEnd();
                    }},
                new EMenuItem("Move Main X Axis Cursor Faster",
                              Resources.getResource(WaveformWindow.class, "ButtonVCRFaster.gif")) { public void run() {
                                  WaveformWindow.getCurrentWaveformWindow().vcrClickFaster();
                    }},
                new EMenuItem("Move Main X Axis Cursor Slower",
                              Resources.getResource(WaveformWindow.class, "ButtonVCRSlower.gif")) { public void run() {
                                  WaveformWindow.getCurrentWaveformWindow().vcrClickSlower();
                    }},
                SEPARATOR,
                new EMenuItem("Generate Digital Signal from Analog Signal (0.5v threshold)") { public void run() {
                    WindowFrame current = WindowFrame.getCurrentWindowFrame();
                    WindowContent content = current.getContent();
                    if (!(content instanceof WaveformWindow)) {
                        System.out.println("Must select a Waveform window first");
                        return;
                    }
                    WaveformWindow ww = (WaveformWindow)content;
                    Panel panel = ww.getPanel(0);
                    Stimuli stim = ww.getSimData();
                    Signal<?> s1 = panel.getSignals().get(0).getSignal();
                    SignalCollection sc = Stimuli.newSignalCollection(stim, "DC SIGNALS");
                    Signal<?> derived = new DerivedSignal<DigitalSample,ScalarSample>
                        (sc,
                         stim,
                         s1.getSignalName(),
                         s1.getSignalContext(),
                         true,
                         new Signal[] { s1 }) {
                        public boolean isEmpty() { return false; }
                        public RangeSample<DigitalSample> getDerivedRange(RangeSample<ScalarSample>[] s) {
                            if (s[0]==null) return null;
                            double min = s[0].getMin().getValue();
                            double max = s[0].getMax().getValue();
                            return new RangeSample<DigitalSample>(min <  0.5 ? DigitalSample.LOGIC_0 : DigitalSample.LOGIC_1,
                                                                  max >= 0.5 ? DigitalSample.LOGIC_1 : DigitalSample.LOGIC_0);
                        }
                    };
                    new WaveSignal(panel, derived);
                } }
                ),

		// mnemonic keys available: AB DE GHIJKLMNOPQR  UVWXYZ
            new EMenu("_Messages Window",
                new EMenuItem("_Tile with Edit Window") { public void run() {
                    MessagesWindow.tileWithEdit(); }},
                new EMenuItem("_Save Messages...") { public void run() {
                    MessagesStream.getMessagesStream().save(); }},
                new EMenuItem("_Clear") { public void run() {
                    MessagesWindow.clearAll(); }},
                new EMenuItem("Set F_ont...") { public void run() {
                    MessagesWindow.selectFont(); }}),

            MenuCommands.makeExtraMenu("j3d.ui.J3DMenu"),

		// mnemonic keys available: ABCDEFGHIJK MNOPQ STUVWXYZ
            new EMenu("Side _Bar",
		        new EMenuItem("On _Left") { public void run() {
                    WindowFrame.setSideBarLocation(true); }},
		        new EMenuItem("On _Right") { public void run() {
                    WindowFrame.setSideBarLocation(false); }}),

            SEPARATOR
        );
        thisWindowMenu = menu;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { setDynamicVisibleLayerMenus(); }
        });
        return menu;
    }

    static EMenuItem getHiddenWindowCycleMenuItem() {
        if (hiddenWindowCycleMenuItem == null) {
            hiddenWindowCycleMenuItem = new EMenuItem("Window Cycle", KeyStroke.getKeyStroke('Q', 0)) {
                public void run() {
                WindowFrame.getWindows().next().requestFocus(); }};
        }
        return hiddenWindowCycleMenuItem;
    }

    private static DynamicEMenuItem messageDynamicMenu = null;

    public static void setDynamicMenus()
    {
        List<DynamicEMenuItem> list = new ArrayList<DynamicEMenuItem>();
        KeyStroke accelerator = getHiddenWindowCycleMenuItem().getAccelerator();
        for (Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext();) {
            WindowFrame wf = it.next();
            list.add(new DynamicEMenuItem(wf, accelerator));
            accelerator = null;
        }
        if (messageDynamicMenu == null)
            messageDynamicMenu = new DynamicEMenuItem(accelerator);
        list.add(messageDynamicMenu);
        // Sort list of dynamic menus before adding into the menus
        Collections.sort(list, new TextUtils.ObjectsByToString());
        thisWindowMenu.setDynamicItems(list);
    }

    /**
     * Method to change the Visible Layer pulldown entry to reflect the current configurations.
     */
    public static void setDynamicVisibleLayerMenus()
    {
        List<DynamicLayerVisibilityMenuItem> list = new ArrayList<DynamicLayerVisibilityMenuItem>();
        for(String cName : InvisibleLayerConfiguration.getOnly().getConfigurationNames())
        {
        	if (InvisibleLayerConfiguration.getOnly().getConfigurationHardwiredIndex(cName) >= 0) continue;
            list.add(new DynamicLayerVisibilityMenuItem(cName));
        }

        for (EMenuBar.Instance menuBarInstance: TopLevel.getMenuBars())
        {
            JMenu menu = (JMenu)menuBarInstance.findMenuItem(visibleLayersMenu.getPath());
            while (menu.getMenuComponentCount() > InvisibleLayerConfiguration.NUM_CONFIGS)
            	menu.remove(menu.getMenuComponentCount()-1);
            boolean hasMore = false;
            for (EMenuItem elem : list)
            {
                if (elem == EMenuItem.SEPARATOR)
                {
                    menu.addSeparator();
                    continue;
                }
                if (!hasMore) menu.addSeparator();
                hasMore = true;
                JMenuItem item = elem.genMenu();
                menu.add(item);
            }
        }
    }

    public static void setLayerVisible(int level)
    {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        WindowFrame frame = wnd.getWindowFrame();
        if (frame == null) return;
        LayerTab layerTab = frame.getLayersTab();
        if (layerTab == null) return;

        String cName = InvisibleLayerConfiguration.getOnly().findHardWiredConfiguration(level);
    	if (cName == null)
    	{
    		// nothing saved: use old way
	        Cell cell = wnd.getCell();
	        if (cell == null) return;
	        Technology tech = cell.getTechnology();
	        if (!tech.isLayout()) return;
	        layerTab.setVisibilityLevel(level);
    	} else
    	{
    		layerTab.setInvisibleLayerConfiguration(cName);
    	}
    }

    private static class DynamicLayerVisibilityMenuItem extends EMenuItem
    {
        private String cName;

        public DynamicLayerVisibilityMenuItem(String cName)
        {
            super(cName);
            this.cName = cName;
        }

        public String getDescription() { return "Visible Layer Combination"; }

        protected void updateButtons() {}

        public void run()
        {
            for (Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext();)
            {
                WindowFrame wf = it.next();
                wf.getLayersTab().setInvisibleLayerConfiguration(cName);
            }
        }
    }

    private static class DynamicEMenuItem extends EMenuItem
    {
        private WindowFrame window;

        public DynamicEMenuItem(WindowFrame w, KeyStroke accelerator)
        {
            super(w.getTitle(), accelerator);
            window = w;
        }
        public DynamicEMenuItem(KeyStroke accelerator)
        {
            super("Electric Messages", accelerator);
            window = null;
        }

        public String getDescription() { return "Window Cycle"; }

        protected void updateButtons() {}

        public void run()
        {
            if (window != null)
                window.requestFocus();
            else
                for(MessagesWindow mw : MessagesWindow.getMessagesWindows()) {
                    // message window. This could be done by creating another interface
                    mw.requestFocus();
                    break;
                }
        }
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
        double scaleX = wnd.getGridXSpacing() * sz.width / 5.01 / displayable.getWidth();
        double scaleY = wnd.getGridYSpacing() * sz.height / 5.01 / displayable.getHeight();
        double scale = Math.min(scaleX, scaleY);
        wnd.setScale(wnd.getScale() / scale);
        wnd.setGrid(true);
    }

    /**
     * Method to adjust the current window so that it matches that of the "other" window.
     * For this to work, there must be exactly one other window shown.
     * @param how 0 to match scale; 1 to match in X; 2 to match in Y; 3 to match all.
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
        	case 3:
        		wnd.setScale(other.getScale());
        		wnd.setOffset(new Point2D.Double(other.getOffset().getX(), other.getOffset().getY()));
        		break;
        }
        wnd.fullRepaint();
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
        Rectangle [] areas = getArrangementWindowAreas();

        // find offset for multiscreen systems
        Point loc = new Point(0, 0);
		if (TopLevel.isMDIMode())
		{
			TopLevel tl = TopLevel.getCurrentJFrame();
			loc = tl.getContentPane().getLocationOnScreen();
		}

		// tile the windows in each area
        for (Rectangle area : areas)
        {
        	// see how many windows are on this screen
        	int count = 0;
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				Rectangle wfBounds = wf.getFrame().getBounds();
				int locX = (int)wfBounds.getCenterX() - loc.x;
				int locY = (int)wfBounds.getCenterY() - loc.y;
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
				int locX = (int)wfBounds.getCenterX() - loc.x;
				int locY = (int)wfBounds.getCenterY() - loc.y;
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
		Rectangle [] areas = getArrangementWindowAreas();

        // find offset for multiscreen systems
        Point loc = new Point(0, 0);
		if (TopLevel.isMDIMode())
		{
			TopLevel tl = TopLevel.getCurrentJFrame();
			loc = tl.getContentPane().getLocationOnScreen();
		}

		// tile the windows in each area
        for (Rectangle area : areas)
		{
			// see how many windows are on this screen
			int count = 0;
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				Rectangle wfBounds = wf.getFrame().getBounds();
				int locX = (int)wfBounds.getCenterX() - loc.x;
				int locY = (int)wfBounds.getCenterY() - loc.y;
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
				int locX = (int)wfBounds.getCenterX() - loc.x;
				int locY = (int)wfBounds.getCenterY() - loc.y;
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
		Rectangle [] areas = getArrangementWindowAreas();

        // find offset for multiscreen systems
        Point loc = new Point(0, 0);
		if (TopLevel.isMDIMode())
		{
			TopLevel tl = TopLevel.getCurrentJFrame();
			loc = tl.getContentPane().getLocationOnScreen();
		}

		// tile the windows in each area
		for (Rectangle area : areas)
		{
			// see how many windows are on this screen
			int count = 0;
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				Rectangle wfBounds = wf.getFrame().getBounds();
				int locX = (int)wfBounds.getCenterX() - loc.x;
				int locY = (int)wfBounds.getCenterY() - loc.y;
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
				int locX = (int)wfBounds.getCenterX() - loc.x;
				int locY = (int)wfBounds.getCenterY() - loc.y;
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

//	private static void nextWindowCommand()
//	{
//		Object cur = null;
//		List<Object> frames = new ArrayList<Object>();
//		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
//		{
//			WindowFrame wf = it.next();
//			if (wf.isFocusOwner()) cur = wf;
//			frames.add(wf);
//		}
//		MessagesWindow mw = TopLevel.getMessagesWindow();
//		if (mw.isFocusOwner()) cur = mw;
//		frames.add(mw);
//
//		// find current frame in the list
//		int found = -1;
//		for(int i=0; i<frames.size(); i++)
//		{
//			if (cur == frames.get(i))
//			{
//				found = i;
//				break;
//			}
//		}
//		if (found >= 0)
//		{
//			found++;
//			if (found >= frames.size()) found = 0;
//			Object newCur = frames.get(found);
//			if (newCur instanceof WindowFrame)
//				((WindowFrame)newCur).requestFocus(); else
//					((MessagesWindow)newCur).requestFocus();
//		}
//	}

	private static Rectangle [] getArrangementWindowAreas()
    {
		// get list of possible window areas
		Rectangle [] areas = TopLevel.getWindowAreas();

        // remove the messages window
        for (MessagesWindow mw : MessagesWindow.getMessagesWindows()) {
            Rectangle mb = mw.getMessagesLocation();
            if (mb!=null) removeOccludingRectangle(areas, mb);
        }
        return areas;
    }

    private static void removeOccludingRectangle(Rectangle [] areas, Rectangle occluding)
    {
		int cX = occluding.x + occluding.width/2;
		int cY = occluding.y + occluding.height/2;


        for (Rectangle area : areas)
    	{
			int lX = (int)area.getMinX();
			int hX = (int)area.getMaxX();
			int lY = (int)area.getMinY();
			int hY = (int)area.getMaxY();
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
				area.x = lX;   area.width = hX - lX;
				area.y = lY;   area.height = hY - lY;
			}
    	}
    }

    /**
     * This method implements the command to set default background colors. This function resets colors
     * set by blackBackgroundCommand or whiteBackgroundCommand.
     */
    public static void defaultBackgroundCommand()
    {
        User.resetFactoryColor(User.ColorPrefType.BACKGROUND);
        User.resetFactoryColor(User.ColorPrefType.GRID);
        User.resetFactoryColor(User.ColorPrefType.MEASUREMENT);   
        User.resetFactoryColor(User.ColorPrefType.HIGHLIGHT);
        User.resetFactoryColor(User.ColorPrefType.PORT_HIGHLIGHT);
        User.resetFactoryColor(User.ColorPrefType.TEXT);
        User.resetFactoryColor(User.ColorPrefType.INSTANCE);
		User.resetFactoryColor(User.ColorPrefType.WAVE_BACKGROUND);
		User.resetFactoryColor(User.ColorPrefType.WAVE_FOREGROUND);
		User.resetFactoryColor(User.ColorPrefType.WAVE_STIMULI);

        EDatabase database = EDatabase.clientDatabase();
        // change default Artwork graphics color
        Layer layer = database.getArtwork().defaultLayer;
        layer.setGraphics(layer.getGraphics().withColor(Color.BLACK));
		// change the colors in the "Generic" technology
        database.getGeneric().setBackgroudColor(Color.BLACK);

        // redraw
        WindowFrame.repaintAllWindows();
    }

    /**
     * This method implements the command to set colors so that there is a black background.
     */
    public static void blackBackgroundCommand()
    {
        User.setColor(User.ColorPrefType.BACKGROUND, Color.BLACK.getRGB());
        User.setColor(User.ColorPrefType.GRID, Color.WHITE.getRGB());
        User.setColor(User.ColorPrefType.MEASUREMENT, Color.RED.getRGB());    
        User.setColor(User.ColorPrefType.HIGHLIGHT, Color.RED.getRGB());    
        User.setColor(User.ColorPrefType.PORT_HIGHLIGHT, Color.YELLOW.getRGB());
        User.setColor(User.ColorPrefType.TEXT, Color.WHITE.getRGB());
        User.setColor(User.ColorPrefType.INSTANCE, Color.WHITE.getRGB());
		User.setColor(User.ColorPrefType.WAVE_BACKGROUND, Color.BLACK.getRGB());
		User.setColor(User.ColorPrefType.WAVE_FOREGROUND, Color.WHITE.getRGB());
		User.setColor(User.ColorPrefType.WAVE_STIMULI, Color.RED.getRGB());

        EDatabase database = EDatabase.clientDatabase();
        // change default Artwork graphics color
        Layer layer = database.getArtwork().defaultLayer;
        layer.setGraphics(layer.getGraphics().withColor(Color.WHITE));
		// change the colors in the "Generic" technology
        database.getGeneric().setBackgroudColor(Color.WHITE);

        // redraw
        WindowFrame.repaintAllWindows();
    }

    /**
     * This method implements the command to set colors so that there is a white background.
     */
    public static void whiteBackgroundCommand()
    {
        User.setColor(User.ColorPrefType.BACKGROUND, Color.WHITE.getRGB());
        User.setColor(User.ColorPrefType.GRID, Color.BLACK.getRGB());
        User.setColor(User.ColorPrefType.MEASUREMENT, Color.RED.getRGB());    
        User.setColor(User.ColorPrefType.HIGHLIGHT, Color.RED.getRGB());
        User.setColor(User.ColorPrefType.PORT_HIGHLIGHT, Color.DARK_GRAY.getRGB());
        User.setColor(User.ColorPrefType.TEXT, Color.BLACK.getRGB());
        User.setColor(User.ColorPrefType.INSTANCE, Color.BLACK.getRGB());
		User.setColor(User.ColorPrefType.WAVE_BACKGROUND, Color.WHITE.getRGB());
		User.setColor(User.ColorPrefType.WAVE_FOREGROUND, Color.BLACK.getRGB());
		User.setColor(User.ColorPrefType.WAVE_STIMULI, Color.RED.getRGB());

        EDatabase database = EDatabase.clientDatabase();
        // change default Artwork graphics color
        Layer layer = database.getArtwork().defaultLayer;
        layer.setGraphics(layer.getGraphics().withColor(Color.BLACK));
		// change the colors in the "Generic" technology
        database.getGeneric().setBackgroudColor(Color.BLACK);

        // redraw
        WindowFrame.repaintAllWindows();
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
//        for (int j=0; j<gs.length; j++) {
//            //System.out.println("Found GraphicsDevice: "+gs[j]+", type: "+gs[j].getType());
//        }
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

		// remember messages information; arbitrarily use only the first window if there exist more than one
        for (MessagesWindow mw : MessagesWindow.getMessagesWindows()) {
            Rectangle rect = mw.getMessagesLocation();
            User.setDefaultMessagesPos(new Point(rect.x, rect.y));
            User.setDefaultMessagesSize(new Dimension(rect.width, rect.height));
            break;
        }
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

    /**
     * Method to import color pattern used in Cadence
     */
    private static void importCadencePreferences()
    {
        String [] options = { "Yes", "No", "Cancel Import"};
        int response = Job.getUserInterface().askForChoice("You won't be able to restore previous preferences.\nDo you want to backup your preferences " +
             "before importing Cadence values?", "Import Cadence Preferences", options, options[1]);
        if (response == 0) // Save previous preferences
        {
            String backFileName = OpenFile.chooseOutputFile(FileType.XML, "Backup Preferences", "electricPrefsBack.xml");
            if (backFileName != null)
                Pref.exportPrefs(backFileName);
            else
               System.out.println("Previous Preferences not backup");
        }
        if (response != 2) // cancel
        {
            String cadenceFileName = "CadencePrefs.xml";
            URL fileURL = Resources.getURLResource(TopLevel.class, cadenceFileName);
            if (fileURL != null)
                UserInterfaceMain.importPrefs(fileURL);
            else
                System.out.println("Cannot import '" + cadenceFileName + "'");
        }
    }
}
