/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IRSIM.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.ToolSettings;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.output.GenerateVHDL;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.io.output.Verilog;
import com.sun.electric.tool.simulation.als.ALS;
import com.sun.electric.tool.user.CompileVHDL;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.menus.EMenu;
import com.sun.electric.tool.user.ui.TextWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.waveform.Panel;
import com.sun.electric.tool.user.waveform.WaveSignal;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/**
 * IRSIM
 */
public class IRSIM {

    private static boolean irsimChecked = false;
    private static Class<?> irsimClass = null;
    private static Method irsimSimulateMethod;
    
    /**
     * Method to tell whether the IRSIM simulator is available.
     * IRSIM is packaged separately because it is from Stanford University.
     * This method dynamically figures out whether the IRSIM module is present by using reflection.
     * @return true if the IRSIM simulator is available.
     */
    public static boolean hasIRSIM()
    {
        if (!irsimChecked)
            {
                irsimChecked = true;

                // find the IRSIM class
                try
                    {
                        irsimClass = Class.forName("com.sun.electric.plugins.irsim.Analyzer");
                    } catch (ClassNotFoundException e)
                    {
                        irsimClass = null;
                        return false;
                    }

                // find the necessary methods on the IRSIM class
                try
                    {
                        irsimSimulateMethod = irsimClass.getMethod("simulateCell", new Class[] {Cell.class, VarContext.class, String.class});
                    } catch (NoSuchMethodException e)
                    {
                        irsimClass = null;
                        return false;
                    }
            }

        // if already initialized, return
        if (irsimClass == null) return false;
        return true;
    }

    /**
     * Method to run the IRSIM simulator on a given cell, context or file.
     * Uses reflection to find the IRSIM simulator (if it exists).
     * @param cell the Cell to simulate.
     * @param context the context to the cell to simulate.
     * @param fileName the name of the file with the netlist.  If this is null, simulate the cell.
     * If this is not null, ignore the cell and simulate the file.
     */
    public static void runIRSIM(Cell cell, VarContext context, String fileName)
    {
        try
            {
                irsimSimulateMethod.invoke(irsimClass, new Object[] {cell, context, fileName});
                return;
            } catch (Exception e)
            {
                System.out.println("Unable to run the IRSIM simulator");
                e.printStackTrace(System.out);
            }
    }

}
