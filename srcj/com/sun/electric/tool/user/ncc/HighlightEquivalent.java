package com.sun.electric.tool.user.ncc;

import java.util.Set;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NetNameProxy;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.ncc.NccJob;
import com.sun.electric.tool.ncc.basic.CellContext;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.result.NccResult;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.ncc.result.equivalence.Equivalence;
import com.sun.electric.tool.user.Highlighter;

/** Called from Tool -> NCC -> Highlight Equivalent */
public class HighlightEquivalent {
	private static void prln(String s) {System.out.println(s);}
	
	private void highlightEquivalentNet(Network n, NccResults results, 
			                            CellContext cc) {
		NccResult r = results.getResultFromRootCells();
		Equivalence e = r.getEquivalence();
		NetNameProxy eqProx = e.findEquivalentNet(cc.context, n);
		if (eqProx==null) {prln("Can't find an equivalent network.");  return;}
		VarContext eqCtxt = eqProx.getContext();
		Cell eqCell = eqProx.leafCell();
		Highlighter h = HighlightTools.getHighlighter(eqCell, eqCtxt);
		HighlightTools.highlightNetNamed(h, eqCell, eqProx.leafName());
	}
	
	private void highlight1() {
		NccResults results = NccJob.getLastNccResults();
		if (results==null) {
			prln("No saved NCC results.  Please run NCC first.");
			return;
		}
		UserInterface ui = Job.getUserInterface();
		EditWindow_ wnd = ui.needCurrentEditWindow_();
		Set<Network> nets = wnd.getHighlightedNetworks();
		if (nets.size()==0)  {prln("No network selected");  return;}
		if (nets.size()>1)  prln("Multiple networks selected, equivalating only one");
		CellContext cc = NccUtils.getCellContext(wnd);
		for (Network n : nets) {
			prln("Find equivalent for network: "+n.describe(false));
			highlightEquivalentNet(n, results, cc);
			break;
		}
	}
	
	private HighlightEquivalent() {highlight1();}
	
	// -------------------------- public method -------------------------------
	public static void highlight() {new HighlightEquivalent();}
}
