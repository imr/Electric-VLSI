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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.menus.MenuBar;
import com.sun.electric.tool.user.menus.WindowMenu;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.plugins.j3d.ui.J3DViewDialog;
import com.sun.electric.plugins.j3d.View3DWindow;
import com.sun.electric.plugins.j3d.utils.J3DClientApp;
import com.sun.electric.plugins.j3d.utils.J3DClientApp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.StringTokenizer;
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

        MenuBar.Menu j3DMenu = new MenuBar.Menu("3D", '3');
        menuBar.add(j3DMenu);

        j3DMenu.addMenuItem("Capture Frame", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { J3DDemoDialog.create3DDemoDialog(TopLevel.getCurrentJFrame());} });
		j3DMenu.addMenuItem("Open 3D Capacitance Window", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { WindowMenu.create3DViewCommand(true); } });
        j3DMenu.addMenuItem("Read Capacitance Data From Socket", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { J3DViewDialog.create3DViewDialog(TopLevel.getCurrentJFrame(), "localhost"); } });
        j3DMenu.addMenuItem("Read Capacitance Data From File", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { readDemoDataFromFile(); } });
		return j3DMenu;
    }

    // ---------------------- THE 3D MENU FUNCTIONS -----------------

    public static void readDemoDataFromFile()
    {
        String fileName = OpenFile.chooseInputFile(FileType.TEXT, null);
        Object[] possibleValues = { "OK", "Skip", "Cancel" };

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

            for(;;)
            {
                // get keyword from file
                String line = lineReader.readLine();
                if (line == null) break;
                int response = JOptionPane.showOptionDialog(null,
                "Applying following data " + line, "Action", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, possibleValues,
                        possibleValues[0]);
                if (response == 1) continue; // skip
                else if (response == 2) break; // cancel option

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
}
