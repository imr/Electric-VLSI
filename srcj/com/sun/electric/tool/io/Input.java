package com.sun.electric.tool.io;

import com.sun.electric.database.hierarchy.Library;

import java.io.*;

public class Input
{
	String filePath;
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

	// ------------------------- private data ----------------------------

	// ---------------------- private and protected methods -----------------

	protected Input()
	{
	}

	// ----------------------- public methods -------------------------------

	public Library ReadLib() { return null; }

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
		try
		{
			in.fileInputStream = new FileInputStream(fileName);
		} catch (FileNotFoundException e)
		{
			System.out.println("Could not find file " + fileName);
			return null;
		}
		return in.ReadLib();
	}
}
