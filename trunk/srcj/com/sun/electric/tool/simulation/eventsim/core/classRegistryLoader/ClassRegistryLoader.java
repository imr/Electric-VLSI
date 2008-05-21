package com.sun.electric.tool.simulation.eventsim.core.classRegistryLoader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;


public class ClassRegistryLoader {
	public ClassRegistryLoader() {}
	
	public void load(String fileName)
	throws FileNotFoundException, ParseException {
		FileInputStream inStream= null;

		// FileNotFound exception thrown if a file does not exist
		inStream= new FileInputStream(fileName);
		
		// we have a file reader, now make a parser and load the library
		ClassLibraryParser parser= new ClassLibraryParser(inStream);
		// now load
		// ParseException thrown if there is an error
		parser.start();
	} // load
	
} // class ClassRegistryLoader
