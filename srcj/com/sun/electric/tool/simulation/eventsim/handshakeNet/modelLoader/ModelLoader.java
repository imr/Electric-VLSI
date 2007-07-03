package com.sun.electric.tool.simulation.eventsim.handshakeNet.modelLoader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ModelLoader {

	public ModelLoader(){}
	
	public void loadFile(String fileName)
	throws FileNotFoundException, ParseException {
		FileInputStream inStream= null;

		// FileNotFound exception thrown if a file does not exist
		inStream= new FileInputStream(fileName);
		
		// we have a file reader, now make a parser and load the library
		HandshakeNetParser parser= new HandshakeNetParser(inStream);
		// now load
		// ParseException thrown if there is an error
		parser.start();
	} // load	
} // class ModelLoader
