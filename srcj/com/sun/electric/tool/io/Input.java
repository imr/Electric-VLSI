package com.sun.electric.tool.io;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;

import java.io.*;

public class Input
{
	String filePath;
	Library lib;
	FileInputStream fileInputStream;

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

	// ----------------------- public methods -------------------------------

	public boolean ReadLib() { return true; }

	public static Library ReadLibrary(String fileName, ImportType type)
	{
		Input in;

		if (type == ImportType.BINARY)
			in = (Input)new BinaryIn(); else
		{
			System.out.println("Unknown import type");
			return null;
		}
		in.filePath = fileName;
		in.lib = Library.newInstance(fileName, fileName);
		try
		{
			in.fileInputStream = new FileInputStream(fileName);
		} catch (FileNotFoundException e)
		{
			System.out.println("Could not find file " + fileName);
			return null;
		}
		if (in.ReadLib())
		{
			System.out.println("Error reading library");
			return null;
		}
		return in.lib;
	}
}
