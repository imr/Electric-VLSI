/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Output.java
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;

import javax.swing.*;
import java.awt.geom.Rectangle2D;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
//import javax.print.PrintServiceLookup;

/**
 * This class manages writing files in different formats.
 * The class is subclassed by the different file writers.
 */
public class Output//extends IOTool
{
	/** file path */                            protected String filePath;
	/** for writing text files */               protected PrintWriter printWriter;
	/** for writing binary files */             protected DataOutputStream dataOutputStream;

	Output()
	{
	}

	/**
	 * Method to write a Library.
	 * This method is never called.
	 * Instead, it is always overridden by the appropriate write subclass.
	 * @param lib the Library to be written.
     * @return true on error.
	 */
	protected boolean writeLib(Library lib) { return true; }

    /**
     * Method to write a cell.
     * This method is never called.
     * Instead, it is always overridden by the appropriate write subclass.
     * @param cell the Cell to be written.
     * @param context the VarContext of the cell (for parameter evaluation)
     * @return true on error.
     */
    protected boolean writeCell(Cell cell, VarContext context) { return true; }

	/**
	 * Method to write an entire Library with a particular format.
	 * This is used for output formats that capture the entire library
	 * (only the ELIB and Readable Dump formats).
	 * The alternative to writing the entire library is writing a single
	 * cell and the hierarchy below it (use "writeCell").
	 * @param lib the Library to be written.
	 * @param type the format of the output file.
	 * @param compatibleWith6 true to write a library that is compatible with version 6 Electric.
     * @return true on error.
	 */
	public static boolean writeLibrary(Library lib, OpenFile.Type type, boolean compatibleWith6)
	{
		Output out;

        // scan for Dummy Cells, warn user that they still exist
        List dummyCells = new ArrayList();
        dummyCells.add("WARNING: Library "+lib.getName()+" contains the following Dummy cells:");
        for (Iterator it = lib.getCells(); it.hasNext(); ) {
            Cell c = (Cell)it.next();
            if (c.getVar(Input.IO_DUMMY_OBJECT) != null) {
                dummyCells.add("   "+c.noLibDescribe());
            }
        }
        if (dummyCells.size() > 1) {
            dummyCells.add("Do you really want to write this library?");
            Object [] options = {"Continue Writing", "Cancel" };
            int val = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(), dummyCells.toArray(),
                    "Dummy Cells Found in "+lib.getName(), JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    options, options[1]);
            if (val == 1) return true;
        }

		// make sure that all "meaning" options are attached to the database
		Pref.installMeaningVariables();
		
		// make sure that this library save is announced
		for(Iterator it = Tool.getListeners(); it.hasNext(); )
		{
			Listener listener = (Listener)it.next();
			listener.writeLibrary(lib);
		}

		// make sure all technologies with irrelevant scale information have the same scale value
		double largestScale = 0;
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			if (tech.isScaleRelevant()) continue;
			if (tech == Generic.tech) continue;
			if (tech.getScale() > largestScale) largestScale = tech.getScale();
		}
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			if (tech.isScaleRelevant()) continue;
			if (tech == Generic.tech) continue;
			tech.setScale(largestScale);
		}

		// handle different file types
		URL libFile = lib.getLibFile();
		if (libFile == null)
			libFile = TextUtils.makeURLToFile(lib.getName());
		if (type == OpenFile.Type.ELIB)
		{
			// backup previous files if requested
			int backupScheme = IOTool.getBackupRedundancy();
			if (backupScheme == 1)
			{
				// one-level backup
				String backupFileName = libFile.getPath() + "~";
				File oldFile = new File(backupFileName);
				if (oldFile.exists())
				{
					oldFile.delete();
				}
				File newFile = new File(libFile.getPath());
				if (newFile.exists())
				{
					newFile.renameTo(oldFile);
				}
			} else if (backupScheme == 2)
			{
				// full-history backup
				File newFile = new File(libFile.getPath());
				if (newFile.exists())
				{
					long modified = newFile.lastModified();
					Date modifiedDate = new Date(modified);
					SimpleDateFormat sdf = new SimpleDateFormat("-yyyy-MM-dd");
					for(int i=0; i<1000; i++)
					{
						String backupFileName = TextUtils.getFileNameWithoutExtension(libFile) + sdf.format(modifiedDate);
						if (i != 0)
							backupFileName += "--" + i;
						backupFileName += "." + TextUtils.getExtension(libFile);
						File oldFile = new File(backupFileName);
						if (oldFile.exists()) continue;
						newFile.renameTo(oldFile);
						break;
					}
				}
			}
			ELIB elib = new ELIB();
			if (compatibleWith6) elib.write6Compatible();
			out = (Output)elib;
			String properOutputName = TextUtils.getFilePath(libFile) + TextUtils.getFileNameWithoutExtension(libFile) + ".elib";
            if (out.openBinaryOutputStream(properOutputName)) return true;
            if (out.writeLib(lib)) return true;
            if (out.closeBinaryOutputStream()) return true;
		} else if (type == OpenFile.Type.READABLEDUMP)
		{
			out = (Output)new ReadableDump();
			String properOutputName = TextUtils.getFilePath(libFile) + TextUtils.getFileNameWithoutExtension(libFile) + ".txt";
			if (out.openTextOutputStream(properOutputName)) return true;
			if (out.writeLib(lib)) return true;
			if (out.closeTextOutputStream()) return true;
		} else
		{
			System.out.println("Unknown export type: " + type);
			return true;
		}
		return false;
	}
    
    /**
     * Method to write a Cell to a file with a particular format.
	 * In addition to the specified Cell, these formats typically
	 * also include the hierarchy below it.
	 * The alternative is to write the entire library, regardless of
	 * hierarchical structure (use "WriteLibrary").
     * @param cell the Cell to be written.
     * @param filePath the path to the disk file to be written.
     * @param type the format of the output file.
     */
    public static void writeCell(Cell cell, VarContext context, String filePath, OpenFile.Type type)
    {
		if (type == OpenFile.Type.CDL)
		{
			Spice.writeSpiceFile(cell, context, filePath, true);
		} else if (type == OpenFile.Type.CIF)
		{
			CIF.writeCIFFile(cell, context, filePath);
		} else if (type == OpenFile.Type.GDS)
		{
			GDS.writeGDSFile(cell, context, filePath);
		} else if (type == OpenFile.Type.MAXWELL)
		{
			Maxwell.writeMaxwellFile(cell, context, filePath);
		} else if (type == OpenFile.Type.POSTSCRIPT)
		{
			PostScript.writePostScriptFile(cell, context, filePath);
		} else if (type == OpenFile.Type.SPICE)
		{
			Spice.writeSpiceFile(cell, context, filePath, false);
		} else if (type == OpenFile.Type.VERILOG)
		{
			Verilog.writeVerilogFile(cell, context, filePath);
		} else if (type == OpenFile.Type.IRSIM)
		{
			IRSIM.writeIRSIMFile(cell, context, filePath);
		}
        
//		if (error)
//		{
//			System.out.println("Error writing "+type+" file");
//			return true;
//		}
//		return false;        
    }

	/**
	 * Method to gather all font settings in a Library and attach them to the Library.
	 * @param lib the Library to examine.
	 * The method examines all TextDescriptors that might be saved with the Library
	 * and adds a new Variable to that Library that describes the font associations.
	 * The Variable is called "LIB_font_associations" and it is an array of Strings.
	 * Each String is of the format NUMBER/FONTNAME where NUMBER is the font number
	 * in the TextDescriptor and FONTNAME is the font name.
	 */
	public static void createFontAssociationVariable(Library lib)
	{
		int maxIndices = TextDescriptor.ActiveFont.getMaxIndex();
		if (maxIndices == 0) return;
		boolean [] fontFound = new boolean[maxIndices];
		for(int i=0; i<maxIndices; i++) fontFound[i] = false;

		// now examine all objects to see which fonts are in use
		checkFontUsage(lib, fontFound);
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			checkFontUsage(cell, fontFound);
			for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
			{
				NodeInst ni = (NodeInst)nIt.next();
				checkFontUsage(ni, fontFound);
				updateFontUsage(ni.getNameTextDescriptor(), fontFound);
				if (ni.getProto() instanceof Cell)
					updateFontUsage(ni.getProtoTextDescriptor(), fontFound);
				for(Iterator pIt = ni.getPortInsts(); pIt.hasNext(); )
				{
					PortInst pi = (PortInst)pIt.next();
					checkFontUsage(pi, fontFound);
				}
			}
			for(Iterator aIt = cell.getArcs(); aIt.hasNext(); )
			{
				ArcInst ai = (ArcInst)aIt.next();
				checkFontUsage(ai, fontFound);
				updateFontUsage(ai.getNameTextDescriptor(), fontFound);
			}
			for(Iterator eIt = cell.getPorts(); eIt.hasNext(); )
			{
				Export pp = (Export)eIt.next();
				checkFontUsage(pp, fontFound);
				updateFontUsage(pp.getTextDescriptor(), fontFound);
			}
		}
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			checkFontUsage(tech, fontFound);
			for(Iterator nIt = tech.getNodes(); nIt.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)nIt.next();
				checkFontUsage(np, fontFound);
				for(Iterator pIt = np.getPorts(); pIt.hasNext(); )
				{
					PrimitivePort pp = (PrimitivePort)pIt.next();
					checkFontUsage(pp, fontFound);
				}
			}
			for(Iterator aIt = tech.getArcs(); aIt.hasNext(); )
			{
				PrimitiveArc ap = (PrimitiveArc)aIt.next();
				checkFontUsage(ap, fontFound);
			}
		}
		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			checkFontUsage(tool, fontFound);
		}
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View view = (View)it.next();
			checkFontUsage(view, fontFound);
		}

		// now save the associations
		List associations = new ArrayList();
		for(int i=0; i<maxIndices; i++)
		{
			if (!fontFound[i]) continue;
			TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(i+1);
			if (af == null) continue;
			String association = Integer.toString(i+1) + "/" + af.getName();
			associations.add(association);
		}
		int numAssociations = associations.size();
		if (numAssociations == 0) return;
		String [] assocArray = new String[numAssociations];
		int i = 0;
		for(Iterator it = associations.iterator(); it.hasNext(); )
			assocArray[i++] = (String)it.next();
		lib.newVar(Library.FONT_ASSOCIATIONS, assocArray);
	}

	private static void checkFontUsage(ElectricObject eobj, boolean [] fontFound)
	{
		for(Iterator it = eobj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			updateFontUsage(var.getTextDescriptor(), fontFound);
		}
	}

	private static void updateFontUsage(TextDescriptor td, boolean [] fontFound)
	{
		int fontIndex = td.getFace();
		if (fontIndex == 0) return;
		fontFound[fontIndex-1] = true;
	}

    /**
     * Opens the dataOutputStream for writing of binary files.
     * @return true on error.
     */
    protected boolean openBinaryOutputStream(String filePath)
    {
		this.filePath = filePath;
        FileOutputStream fileOutputStream;
		try
		{
			fileOutputStream = new FileOutputStream(filePath);
		} catch (FileNotFoundException e)
		{
			System.out.println("Could not write file " + filePath);
			System.out.println("Reason: " + e.getMessage());
			return true;
		}
		BufferedOutputStream bufStrm = new BufferedOutputStream(fileOutputStream);
		dataOutputStream = new DataOutputStream(bufStrm);
        return false;
    }
    
    /** 
     * Closes the dataOutputStream.
     * @return true on error.
     */
    protected boolean closeBinaryOutputStream()
    {
		try
		{
			dataOutputStream.close();
		} catch (IOException e)
		{
			System.out.println("Error closing " + filePath);
			return true;
		}
        return false;
    }
    
    /**
     * Open the printWriter for writing text files
     * @return true on error.
     */
    protected boolean openTextOutputStream(String filePath)
    {
		this.filePath = filePath;
        try
		{
            printWriter = new PrintWriter(new BufferedWriter(new FileWriter(filePath)));
        } catch (IOException e)
		{
            System.out.println("Error opening " + filePath);
            return true;
        }
        return false;
    }

	protected void emitCopyright(String prefix, String postfix)
	{
		if (!IOTool.isUseCopyrightMessage()) return;
		String str = IOTool.getCopyrightMessage();
		int start = 0;
		while (start < str.length())
		{
			int endPos = str.indexOf('\n', start);
			if (endPos < 0) endPos = str.length();
			String oneLine = str.substring(start, endPos);
			printWriter.println(prefix + oneLine + postfix);
			start = endPos+1;
		}
	}

	/**
	 * Method to determine the area of a cell that is to be printed.
	 * Returns null if the area cannot be determined.
	 */
	public Rectangle2D getAreaToPrint(Cell cell, boolean reduce)
	{
		Rectangle2D bounds = cell.getBounds();

		// extend it and make it square
		if (reduce)
		{
			double wid = bounds.getWidth() * 0.75;
			double hei = bounds.getHeight() * 0.75;
//			us_squarescreen(el_curwindowpart, NOWINDOWPART, FALSE, lx, hx, ly, hy, 0);
			bounds.setRect(bounds.getCenterX(), bounds.getCenterY(), wid, hei);
		}

		if (IOTool.getPlotArea() != 0)
		{
			EditWindow wnd = EditWindow.getCurrent();
			if (wnd == null)
			{
				System.out.println("No current window: printing entire cell");
			} else
			{
				if (IOTool.getPlotArea() == 2)
				{
					bounds = wnd.getDisplayedBounds();
				} else
				{
					Rectangle2D hBounds = wnd.getHighlighter().getHighlightedArea(wnd);
					if (hBounds == null || hBounds.getWidth() == 0 || hBounds.getHeight() == 0)
					{
						System.out.println("Warning: no highlighted area; printing entire cell");
					} else
					{
						bounds = hBounds;
					}
				}
			}
		}
		return bounds;
	}

	/** 
     * Close the printWriter.
     * @return true on error.
     */
    protected boolean closeTextOutputStream()
    {
        printWriter.close();
        return false;
    }

}

