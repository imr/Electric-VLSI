/*
 * Options for the NCC Engine.
 */
package com.sun.electric.tool.ncc;

import com.sun.electric.tool.ncc.basicA.Messenger;

public class NccOptions {
	/** file to which status messages are printed in addition to console. 
	 * null means console only */
	public String logFile;
	/** probably not of interest to anyone but me. Messenger to which status 
	 * are printed in addition to console */
	public Messenger messenger;
	/** enable size checking */
	public boolean checkSizes;
	/** merge parallel Cells into one */
	public boolean mergeParallelCells;
}
