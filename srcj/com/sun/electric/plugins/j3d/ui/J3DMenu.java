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
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.menus.MenuBar;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.plugins.j3d.View3DWindow;
import com.sun.electric.plugins.j3d.utils.J3DClientApp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.List;
import java.util.ArrayList;

import javax.swing.*;

/**
 * Class to handle the commands in the "3D" pulldown menu.
 * @author  Gilda Garreton
 * @version 0.1
 */
public class J3DMenu {

    // It can't be protected static -> reflection doesn't like it
    public static MenuBar.Menu add3DMenus(MenuBar menuBar) {

        /****************************** THE 3D MENU ******************************/

        MenuBar.Menu j3DMenu = MenuBar.makeMenu("_3D");
        menuBar.add(j3DMenu);

        /** 3D view */
	    j3DMenu.addMenuItem("_3D View", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { create3DViewCommand(false); } });
        j3DMenu.addMenuItem("Capture Frame", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { J3DDemoDialog.create3DDemoDialog(TopLevel.getCurrentJFrame());} });
//		j3DMenu.addMenuItem("Open 3D Capacitance Window", null,
//			new ActionListener() { public void actionPerformed(ActionEvent e) { WindowMenu.create3DViewCommand(true); } });

        MenuBar.Menu demoSubMenu = MenuBar.makeMenu("Demo");
		j3DMenu.add(demoSubMenu);
        demoSubMenu.addMenuItem("Open 3D View Demo", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { create3DViewCommand(true); } });
        demoSubMenu.addMenuItem("Read Capacitance Data From File", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { readDemoDataFromFile(); } });
        demoSubMenu.addMenuItem("Read Capacitance Data From Socket", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { createSocketDialog(); } });

		return j3DMenu;
    }

    // ---------------------- THE 3D MENU FUNCTIONS -----------------

    public static void createSocketDialog()
    {
        Object value = JOptionPane.showInputDialog(null, "Hostname Dialog", "Enter hostname for socket connection", JOptionPane.PLAIN_MESSAGE,
                null, null, "localhost");
        if (value != null)
            J3DViewDialog.create3DViewDialog(TopLevel.getCurrentJFrame(), value.toString());
    }

    public static void readDemoDataFromFile()
    {
        String fileName = OpenFile.chooseInputFile(FileType.TEXT, null);

        if (fileName == null) return; // Cancel

        Object[] possibleValues = { "Accept All", "OK", "Skip", "Cancel" };
        View3DWindow view3D = null;
        WindowContent content = WindowFrame.getCurrentWindowFrame().getContent();
        if (content instanceof View3DWindow)
            view3D = (View3DWindow)content;
        else
        {
            System.out.println("Current Window Frame is not a 3D View for Read Demo Data");
            return;
        }

        try {
            LineNumberReader lineReader = new LineNumberReader(new FileReader(fileName));
            List knotList = new ArrayList();
            int response = -1;
            for(;;)
            {
                // get keyword from file
                String line = lineReader.readLine();
                if (line == null) break;
                // responce 0 -> Accept All
                if (response != 0)
                {
                    response = JOptionPane.showOptionDialog(null,
                    "Applying following data " + line, "Action", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, possibleValues,
                            possibleValues[0]);
                    if (response == 2) continue; // skip
                    else if (response == 3) break; // cancel option
                }
                String[] stringValues = J3DClientApp.parseValues(line, 0);
                double[] values = J3DClientApp.convertValues(stringValues);
                knotList.add(view3D.moveAndRotate(values));
            }
            view3D.addInterpolator(knotList);
        } catch (Exception e)
        {
            e.printStackTrace();
        }



    }

    /**
	 * This method creates 3D view of current cell
     * @param transPerNode
     */
	public static void create3DViewCommand(boolean transPerNode)
    {
	    Cell curCell = WindowFrame.needCurCell();
	    if (curCell == null) return;
	    WindowFrame.create3DViewtWindow(curCell, WindowFrame.getCurrentWindowFrame(false).getContent(), transPerNode);
    }
}
