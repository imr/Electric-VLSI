/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HelpMenu.java
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
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.menus.MenuBar;
import com.sun.electric.plugins.j3d.utils.J3DQueryProperties;
import com.sun.electric.plugins.j3d.View3DWindow;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Class to handle the commands in the "3D" pulldown menu.
 * @author  Gilda Garreton
 * @version 0.1
 */
public class J3DMenu {

    // It can't be protected static -> reflection doesn't like it
    public static MenuBar.Menu add3DMenus(MenuBar.Menu menu) {

        /****************************** THE 3D MENU ******************************/

        MenuBar.Menu j3DMenu = MenuBar.makeMenu("_3D Window");
        menu.add(j3DMenu);

        // mnemonic keys available: AB  EFGHIJKLMNOPQ S U WXYZ
        /** 3D view */
	    j3DMenu.addMenuItem("_3D View", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { create3DViewCommand(false); } });
        j3DMenu.addMenuItem("_Capture Frame/Animate", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { J3DDemoDialog.create3DDemoDialog(TopLevel.getCurrentJFrame());} });
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

        j3DMenu.addSeparator();
        j3DMenu.addMenuItem("_Test Hardware", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { J3DQueryProperties.queryHardwareAcceleration(); ;} });
		return j3DMenu;
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
	public static void create3DViewCommand(boolean transPerNode)
    {
	    Cell curCell = WindowFrame.needCurCell();
	    if (curCell == null) return;
	    //WindowFrame.create3DViewWindow(curCell, WindowFrame.getCurrentWindowFrame(false).getContent(), transPerNode);

        WindowContent view2D = WindowFrame.getCurrentWindowFrame(false).getContent();

        // 3D view can only be triggered by EditWindow instances
        if (!(view2D instanceof EditWindow)) return;
        WindowFrame frame = new WindowFrame();

        View3DWindow.create3DWindow(curCell, frame, view2D, transPerNode);

//            try
//            {
//                if (create3DMethod == null) create3DMethod = view3DClass.getDeclaredMethod("create3DWindow", new Class[] {Cell.class, WindowFrame.class,
//                                                                                        WindowContent.class, Boolean.class}) ;
//                create3DMethod.invoke(view3DClass, new Object[] {cell, frame, view2D, new Boolean(transPerNode)});
//            } catch (Exception e) {
//                System.out.println("Can't open 3D View window: " + e.getMessage());
//                ActivityLogger.logException(e);
//            }
//
//            return frame;
//        }

    }
}
