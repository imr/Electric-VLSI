package com.sun.electric.tool.ncc;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sun.electric.database.hierarchy.Cell;

/**
 * Cell pairs that have already passed NCC.
 */
class PassedNcc {
	private static class Pair {
		private Cell c1, c2;
		public Pair(Cell c1, Cell c2) {this.c1=c1; this.c2=c2;}
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Pair)) return false;
			Pair p = (Pair) o;
			return (c1==p.c1 && c2==p.c2) || (c1==p.c2 && c2==p.c1);
		}
		@Override
		public int hashCode() {return c1.hashCode()*c2.hashCode();}
		public boolean changedAfter(Date date) {
			return c1.getRevisionDate().after(date) || 
			       c2.getRevisionDate().after(date);
		}
	}
	private Set<Pair> passed = new HashSet<Pair>();
	private Date lastNccRunDate;
	
	public synchronized void setPassed(Cell c1, Cell c2) {passed.add(new Pair(c1, c2));}
	public synchronized boolean getPassed(Cell c1, Cell c2) {
		return passed.contains(new Pair(c1, c2));
	}
	/** Purge all Cells that were changed since the last NCC run.
	 * Then record the time of this new NCC Run. */
	public synchronized void beginNewNccRun() {
		for (Iterator<Pair> pIt=passed.iterator(); pIt.hasNext();) {
			Pair p = pIt.next();
			if (p.changedAfter(lastNccRunDate)) pIt.remove();
		}
		lastNccRunDate = new Date();
	}
}

