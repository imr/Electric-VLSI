/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: J3DMenu.java
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

package com.sun.electric.plugins.j3d.ui;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.plugins.j3d.View3DWindow;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.menus.EMenu;
import com.sun.electric.tool.user.menus.EMenuItem;
import com.sun.electric.tool.Job;
import static com.sun.electric.tool.user.menus.EMenuItem.SEPARATOR;

import java.lang.reflect.Method;


/**
 * Class to handle the commands in the "3D" pulldown menu.
 * @author  Gilda Garreton
 * @version 0.1
 */
public class J3DMenu {

    // It can't be protected static -> reflection doesn't like it
    public static EMenu makeMenu() {
        /****************************** THE 3D MENU ******************************/

        return new EMenu("_3D Window",

        // mnemonic keys available: AB  EFGHIJKLMNOPQ S U WXYZ
        /** 3D view */
            new EMenuItem("_3D View") { public void run() {
                create3DViewCommand(new Boolean(false)); }},
            new EMenuItem("_Capture Frame/Animate") { public void run() {
                J3DDemoDialog.create3DDemoDialog(TopLevel.getCurrentJFrame(), null); }},
//		j3DMenu.addMenuItem("Open 3D Capacitance Window", null,
//			new ActionListener() { public void actionPerformed(ActionEvent e) { WindowMenu.create3DViewCommand(true); } });

//        MenuBar.Menu demoSubMenu = MenuBar.makeMenu("Capacitance _Demo");
//		j3DMenu.add(demoSubMenu);
//        demoSubMenu.addMenuItem("3D _View for Demo", null,
//            new ActionListener() { public void actionPerformed(ActionEvent e) { create3DViewCommand(true); } });
//        demoSubMenu.addMenuItem("Read Data From File", null,
//			new ActionListener() { public void actionPerformed(ActionEvent e) { readDemoDataFromFile(); } });
//        demoSubMenu.addMenuItem("_Read Data", null,
//			new ActionListener() { public void actionPerformed(ActionEvent e) { J3DViewDialog.create3DViewDialog(TopLevel.getCurrentJFrame()); } });

            SEPARATOR,
            new EMenuItem("_Test Hardware") { public void run() {
                runHardwareTest(); }});
    }

    // ---------------------- THE 3D MENU FUNCTIONS -----------------

//    private static void readDemoDataFromFile()
//    {
//        WindowContent content = WindowFrame.getCurrentWindowFrame().getContent();
//        if (!(content instanceof View3DWindow))
//        {
//            System.out.println("Current Window Frame is not a 3D View for Read Demo Data");
//            return;
//        }
//        View3DWindow view3D = (View3DWindow)content;
//        view3D.addInterpolator(null); //J3DUtils.readDemoDataFromFile(view3D));
//        J3DDemoDialog.create3DDemoDialog(TopLevel.getCurrentJFrame());
//    }

    /**
	 * This method creates 3D view of current cell
     * @param transPerNode
     */
	public static void create3DViewCommand(Boolean transPerNode)
    {
	    Cell curCell = WindowFrame.needCurCell();
	    if (curCell == null) return;

        WindowContent view2D = WindowFrame.getCurrentWindowFrame(false).getContent();

        // 3D view can only be triggered by EditWindow instances
        if (!(view2D instanceof EditWindow)) return;
        WindowFrame frame = new WindowFrame();

        View3DWindow.create3DWindow(curCell, frame, view2D, transPerNode.booleanValue());
    }

    /**
     * Calling code available in Java3D plugin using reflection
     */
    private static void runHardwareTest()
    {
        Class app3DClass = Resources.getJMFJ3DClass("J3DQueryProperties");
        try
        {
            Method queryClass = app3DClass.getDeclaredMethod("queryHardwareAcceleration", new Class[] {});
            queryClass.invoke(queryClass, new Object[]{});
        } catch (Exception e) {
            if (Job.getDebug()) e.printStackTrace();
            System.out.println("Cannot call 3D plugin method queryHardwareAcceleration: ");
        }
    }
}
