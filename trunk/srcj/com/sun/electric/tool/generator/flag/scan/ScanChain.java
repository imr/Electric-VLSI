package com.sun.electric.tool.generator.flag.scan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.tool.generator.flag.Utils;

public class ScanChain {
	private final List<String> inNames;
	private final List<String> outNames;
	private final List<String> feedNames;

	private static void error(boolean cond, String msg) {Utils.error(cond, msg);}

	private void addNames(List<String> names, Name name) {
		if (name==null) return;
		for (int i=0; i<name.busWidth(); i++) {
			names.add(name.subname(i).toString());
//			names.add(name.subname(i).canonicString());
		}
	}
	private boolean hasScanInput(NodeInst ni) {
		String portNm = inNames.get(0);
		PortInst pi = ni.findPortInst(portNm);
		return pi!=null;
	}
	private boolean hasScanOutput(NodeInst ni) {
		String portNm = outNames.get(0);
		PortInst pi = ni.findPortInst(portNm);
		return pi!=null;
	}
	private boolean hasScanFeedthrough(NodeInst ni) {
		if (feedNames.size()==0) return false;
		String portNm = feedNames.get(0);
		PortInst pi = ni.findPortInst(portNm);
		return pi!=null;
	}

	public ScanChain(String in, String out, String feedthrough) {
		error(in==null, "ScanChain(): argument \"in\" may not be null");
		error(out==null, "ScanChain(): argument \"out\" may not be null");
		error(feedthrough==null, "ScanChain(): Argument \"feedthrough\" may not be null");
		Name inName = Name.findName(in);
		Name outName = Name.findName(out);
		Name feedName = feedthrough.length()!=0 ? Name.findName(feedthrough)
				                                : null;
		error(inName.busWidth() != outName.busWidth() ||
			  (feedName!=null && inName.busWidth()!=feedName.busWidth()),
			  "ScanChain(): bus widths don't match");
		List<String> inNames = new ArrayList<String>();
		List<String> outNames = new ArrayList<String>();
		List<String> feedNames = new ArrayList<String>();
		addNames(inNames, inName);
		addNames(outNames, outName);
		addNames(feedNames, feedName);
		this.inNames = Collections.unmodifiableList(inNames);
		this.outNames = Collections.unmodifiableList(outNames);
		this.feedNames = Collections.unmodifiableList(feedNames);
	}
	/** If ni has scan-in or feedthrough ports, return List of those port
	 * names.  */
	public List<String> getInputOrFeedNames(NodeInst ni) {
		if (hasScanInput(ni)) return inNames;
		if (hasScanFeedthrough(ni)) return feedNames;
		return null;
	}

	/** If ni has scan-out or feedthrough ports, return List of those port
	 * names.  */
	public List<String> getOutputOrFeedNames(NodeInst ni) {
		if (hasScanOutput(ni)) return outNames;
		if (hasScanFeedthrough(ni)) return feedNames;
		return null;
	}
	public List<String> getInputNames() {return inNames;}
	public List<String> getOutputNames() {return outNames;}
	public List<String> getFeedthroughNames() {return feedNames;}

}
