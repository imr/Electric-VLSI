package com.sun.electric.tool.io;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;

import java.io.*;

public class Input
{
	String filePath;
	Library lib;
	FileInputStream fileInputStream;
	DataInputStream dataInputStream;

	/**
	 * Function is a typesafe enum class that describes the types of files that can be read.
	 */
	public static class ImportType
	{
		private final String name;

		private ImportType(String name)
		{
			this.name = name;
		}

		public String toString() { return name; }

		/** binary input */			public static final ImportType BINARY=   new ImportType("binary");
		/** text input */			public static final ImportType TEXT=     new ImportType("text");
		/** CIF input */			public static final ImportType CIF=      new ImportType("CIF");
		/** GDS input */			public static final ImportType GDS=      new ImportType("GDS");
	}

	public static class FakeCell
	{
		String cellName;
		NodeProto firstInCell;
	}

	// ------------------------- private data ----------------------------

	// ---------------------- private and protected methods -----------------

	protected Input()
	{
	}

	static String mainLibDirectory = null;

	// ----------------------- public methods -------------------------------

	public boolean ReadLib() { return true; }

	public static Library ReadLibrary(String fileName, Library lib, ImportType type)
	{
		Input in;

		if (type == ImportType.BINARY)
		{
			in = (Input)new InputBinary();
		} else if (type == ImportType.TEXT)
		{
//			in = (Input)new InputText();
			
			// no text reader yet, see if an elib can be found
			if (fileName.endsWith(".txt"))
			{
				fileName = fileName.substring(0, fileName.length()-4) + ".elib";
			}
			in = (Input)new InputBinary();
		} else
		{
			System.out.println("Unknown import type");
			return null;
		}
		in.filePath = fileName;
		boolean topLevel = false;
		if (lib == null)
		{
			File f = new File(fileName);
			String libName = f.getName();
			if (libName.endsWith(".elib"))
			{
				libName = libName.substring(0, libName.length()-5);
			} else if (libName.endsWith(".txt"))
			{
				libName = libName.substring(0, libName.length()-4);
			}
			lib = Library.newInstance(libName, fileName);
			mainLibDirectory = f.getParent();
			topLevel = true;
		}
		in.lib = lib;
		try
		{
			in.fileInputStream = new FileInputStream(fileName);
		} catch (FileNotFoundException e)
		{
			System.out.println("Could not find file " + fileName);
			if (topLevel) mainLibDirectory = null;
			return null;
		}
		in.dataInputStream = new DataInputStream(in.fileInputStream);
		if (in.ReadLib())
		{
			System.out.println("Error reading library");
			if (topLevel) mainLibDirectory = null;
			return null;
		}
		if (topLevel) mainLibDirectory = null;
		return in.lib;
	}
}
