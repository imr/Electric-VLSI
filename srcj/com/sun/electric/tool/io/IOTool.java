/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IOTool.java
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
package com.sun.electric.tool.io;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.dialogs.Progress;

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

/**
 * This class manages reading files in different formats.
 * The class is subclassed by the different file readers.
 */
public class IOTool extends Tool
{
	/** the IO tool. */										public static IOTool tool = new IOTool();

	/** Varible key for true library of fake cell. */		public static final Variable.Key IO_TRUE_LIBRARY = ElectricObject.newKey("IO_true_library");
	
//	/** Name of the file being input. */					protected String filePath;
//	/** The Library being input. */							protected Library lib;
//	/** The raw input stream. */							protected FileInputStream fileInputStream;
//	/** The binary input stream. */							protected DataInputStream dataInputStream;
//	/** The length of the file. */							protected long fileLength;
//	/** The progress during input. */						protected static Progress progress = null;
//	/** the path to the library being read. */				protected static String mainLibDirectory = null;
//	/** static list of all libraries in Electric */			private static List newLibraries = new ArrayList();

	// ---------------------- private and protected methods -----------------

	/**
	 * The constructor sets up the I/O tool.
	 */
	protected IOTool()
	{
		super("io");
	}

	/**
	 * Method to set name of Geometric object.
	 * @param geom the Geometric object.
	 * @param value name of object
	 * @param td text descriptor.
	 * @param type type mask.
	 */
	protected Name makeGeomName(Geometric geom, Object value, int type)
	{
		if (value == null || !(value instanceof String)) return null;
		String str = (String)value;
		Name name = Name.findName(str);
		if ((type & BinaryConstants.VDISPLAY) != 0)
		{
			if (name.isTempname())
			{
				String newS = "";
				for (int i = 0; i < str.length(); i++)
				{
					char c = str.charAt(i);
					if (c == '@') c = '_';
					newS += c;
				}
				name = Name.findName(newS);
			}
		} else if (!name.isTempname()) return null;
		return name;
	}

	private static String [] fontNames = null;

	/**
	 * Method to grab font associations that were stored on a Library.
	 * The font associations are used later to convert indices to true font names and numbers.
	 * @param lib the Library to examine.
	 */
	public static void getFontAssociationVariable(Library lib)
	{
		fontNames = null;
		Variable var = lib.getVar(Library.FONT_ASSOCIATIONS, String[].class);
		if (var == null) return;

		String [] associationArray = (String [])var.getObject();
		int maxAssociation = 0;
		for(int i=0; i<associationArray.length; i++)
		{
			int fontNumber = TextUtils.atoi(associationArray[i]);
			if (fontNumber > maxAssociation) maxAssociation = fontNumber;
		}
		if (maxAssociation <= 0) return;

		fontNames = new String[maxAssociation];
		for(int i=0; i<maxAssociation; i++) fontNames[i] = null;
		for(int i=0; i<associationArray.length; i++)
		{
			int fontNumber = TextUtils.atoi(associationArray[i]);
			if (fontNumber <= 0) continue;
			int slashPos = associationArray[i].indexOf('/');
			if (slashPos < 0) continue;
			fontNames[fontNumber-1] = associationArray[i].substring(slashPos+1);
		}

		// data cached: delete the association variable
		lib.delVar(Library.FONT_ASSOCIATIONS);
	}

	public static void fixVariableFont(ElectricObject eobj)
	{
		for(Iterator it = eobj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			fixTextDescriptorFont(var.getTextDescriptor());
		}
	}

	/**
	 * Method to convert the font number in a TextDescriptor to the proper value as
	 * cached in the Library.  The caching is examined by "getFontAssociationVariable()".
	 * @param td the TextDescriptor to convert.
	 */
	public static void fixTextDescriptorFont(TextDescriptor td)
	{
		int fontNumber = td.getFace();
		if (fontNumber == 0) return;

		if (fontNames == null) fontNumber = 0; else
		{
			if (fontNumber <= fontNames.length)
			{
				String fontName = fontNames[fontNumber-1];
				TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(fontName);
				if (af == null) fontNumber = 0; else
					fontNumber = af.getIndex();
			}
		}
		td.setFace(fontNumber);
	}

	/****************************** FOR PREFERENCES ******************************/

	/**
	 * Method to force all User Preferences to be saved.
	 */
	protected static void flushOptions()
	{
		try
		{
	        tool.prefs.flush();
		} catch (BackingStoreException e)
		{
			System.out.println("Failed to save user interface options");
		}
	}
}
