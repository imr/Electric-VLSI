/*
 * Options for the NCC Engine.
 */
package com.sun.electric.tool.ncc;

import java.io.OutputStream;

import com.sun.electric.tool.ncc.basicA.Messenger;

public class NccOptions {
	/** enable size checking */
	public boolean checkSizes;

	/** merge parallel Cells into one */
	public boolean mergeParallelCells;

	/** print lots of progress messages */
	public boolean verbose = true;
}
