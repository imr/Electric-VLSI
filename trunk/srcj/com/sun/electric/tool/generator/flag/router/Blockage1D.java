package com.sun.electric.tool.generator.flag.router;

import java.util.LinkedList;
import java.util.List;

import com.sun.electric.tool.generator.flag.Utils;

/** Keep track of blockages in one dimension */
public class Blockage1D {
	// sorted by increasing min values
	private List<Interval> blockages = new LinkedList<Interval>();
	
	private void prln(String msg) {Utils.prln(msg);}
	
	public Blockage1D() {}
	
	public void block(double min, double max) {
		//prln("Blocking "+min+" to "+max);
		int i=0;
		for (; i<blockages.size(); i++) {
			Interval in = blockages.get(i);
			if (in.getMax()<min) continue;
			if (in.getMin()>max) break;
			// overlap detected
			in.merge(min, max);
			return;
		}
		// No overlap detected.  Add blockage to list. 
		blockages.add(i, new Interval(min,max));
	}
	public List<Interval> getBlockages() {
		return blockages;
	}

}
