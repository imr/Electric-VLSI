package com.sun.electric.tool.generator.flag;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.routing.SeaOfGates;

/** I bundle all the data to be passed by the physical design contructors
 * into a single object because I'd like to be able to add things in the
 * future without changing existing code that may be stored with the design. */
public class FlagConstructorData {
	private final Cell layCell, schCell;
	private final Job job;
	private SeaOfGates.SeaOfGatesOptions prefs;
	FlagConstructorData(Cell layCell, Cell schCell, Job job, SeaOfGates.SeaOfGatesOptions prefs) {
		this.layCell = layCell;
		this.schCell = schCell;
		this.job = job;
		this.prefs = prefs;
	}
	
	// --------------------- public methods ------------------------
	public Cell getLayoutCell() {return layCell;}
	public Cell getSchematicCell() {return schCell;}
	public Job getJob() {return job;}
	public SeaOfGates.SeaOfGatesOptions getSOGPrefs() {return prefs;}
}
