package com.sun.electric.tool.simulation.eventsim.core.globals.loader;

import java.io.FileInputStream;

import com.sun.electric.tool.simulation.eventsim.core.engine.Director;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.globals.Globals;

/** 
 * Load a file with definitions of global parameters.
 * A facade for the generated parser-loader.
 * Format:
 * name = value
 */
public class GlobalsLoader {

	public void load(String fileName) throws EventSimErrorException {
		try {
			// FileNotFound exception thrown if a file does not exist
			FileInputStream inStream= new FileInputStream(fileName);
			// we have a file reader, now make a loader 
			GlobalsParser parser= new GlobalsParser(inStream);
			parser.start();
		}
		catch (Exception e) {
			Director.getInstance().fatalError("Globals loader failed: " + e.getMessage());
		}
	} // load
	
	public static void main(String[] args) {
		
		try {
		if (args.length != 1) {
			System.err.println("Exactly one argument required");
			System.exit(1);
		}
		String fileName= args[0];
		GlobalsLoader gl= new GlobalsLoader();
		gl.load(fileName);
		System.out.println(Globals.getInstance().toString());
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
} // class GlobalsLoader
