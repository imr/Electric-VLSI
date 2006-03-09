package com.sun.electric.tool.user.ncc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NetNameProxy;
import com.sun.electric.database.hierarchy.HierarchyEnumerator.NodableNameProxy;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.generator.layout.LayoutLib;
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
	
	private void highlightEquivalentNet(Network n, Equivalence e, 
			                            CellContext cc) {
		prln("Finding equivalent of Network: "+n.describe(false));
		NetNameProxy eqProx = e.findEquivalentNet(cc.context, n);
		if (eqProx==null) {prln("Can't find an equivalent network.");  return;}
		VarContext eqCtxt = eqProx.getContext();
		Cell eqCell = eqProx.leafCell();
		Highlighter h = HighlightTools.getHighlighter(eqCell, eqCtxt);
		HighlightTools.highlightNetNamed(h, eqCell, eqProx.leafName());
	}
	// Sort Names treating embedded numbers as numbers
	private static class NameCompare implements Comparator<String> {
		public int compare(String nm1, String nm2) {
			return TextUtils.STRING_NUMBER_ORDER.compare(nm1, nm2);
		}
	}
	
	private Network selectNet(Set<Network> netSet) {
		List<Network> nets = new ArrayList<Network>(netSet);
		if (nets.size()==1) return nets.get(0);

		Map<String,Network> nameToNet = new HashMap<String,Network>();
		String[] names = new String[nets.size()];
		int i=0;
		for (Network n : nets) {
			String netNm = n.getName();
			nameToNet.put(netNm, n);
			names[i++] = netNm;
		}
		Arrays.sort(names, new NameCompare());	

		String name = (String) 
		  JOptionPane.showInputDialog(
				null, "Select the Network you want to find the match of",
				"Select Network:", JOptionPane.PLAIN_MESSAGE, null, names, 
				names[0]);
		if (name==null) {
			// user clicked "Cancel"
			return null;
		}
		return nameToNet.get(name);
	}
	
	private void highlightEquivOfNet(Set<Network> nets, Equivalence e, CellContext cc) {
		Network n = selectNet(nets);
		if (n==null) {
			// user clicked "Cancel"
			return;
		}
		highlightEquivalentNet(n, e, cc);
	}
	
	private List<NodeInst> getNodes(List<Geometric> geoms) {
		List<NodeInst> nodes = new ArrayList<NodeInst>();
		for (Geometric g : geoms) {
			if (g instanceof NodeInst) nodes.add((NodeInst)g);
		}
		return nodes;
	}
	
	private List<Nodable> findNodablesFrom(NodeInst ni) {
		List<Nodable> nodables = new ArrayList<Nodable>();
		Cell c = ni.getParent();
		// Search for all Nodables associated with a bussed NodeInst
		for (Iterator<Nodable> it=c.getNetlist(true).getNodables(); it.hasNext();) {
			Nodable no = (Nodable) it.next();
			if (no.getNodeInst()==ni) nodables.add(no);
		}
		return nodables;
	}
	
	private Nodable selectNodable(List<Nodable> nodes) {
		if (nodes.size()==1) return nodes.get(0);

		Map<String,Nodable> nameToNode = new HashMap<String,Nodable>();
		String[] names = new String[nodes.size()];
		int i=0;
		for (Nodable n : nodes) {
			String nodeNm = n.getName();
			nameToNode.put(nodeNm, n);
			names[i++] = nodeNm;
		}
		Arrays.sort(names, new NameCompare());	

		String name = (String) 
		  JOptionPane.showInputDialog(
				null, "Select the Instance you want to find the match of",
				"Select Instance:", JOptionPane.PLAIN_MESSAGE, null, names, 
				names[0]);
		if (name==null) {
			// user clicked "Cancel"
			return null;
		}
		return nameToNode.get(name);
	}
	
	private void highlightEquivOfNodeInst(NodeInst ni, Equivalence e,
			                              CellContext cc) {
		prln("Find the equivalent of NodeInst: "+ni.describe(false));
		List<Nodable> nodables = findNodablesFrom(ni);
		LayoutLib.error(nodables.size()==0, "No Nodable for NodeInst?");
		Nodable node = selectNodable(nodables);
		if (node==null) {
			// user clicked "Cancel"
			return;
		}

		NodableNameProxy eqProx = e.findEquivalentNode(cc.context,node);
		if (eqProx==null) {
			prln(" No equivalent found.");
			return;
		}
		Nodable eqNo = eqProx.getNodable();
		NodeInst eqNi;
		if (eqNo instanceof NodeInst)  eqNi = (NodeInst) eqNo;
		  else  eqNi = eqNo.getNodeInst();
		Cell eqCell = eqProx.leafCell();
		VarContext eqCtxt = eqProx.getContext();
		Highlighter h = HighlightTools.getHighlighter(eqCell, eqCtxt);
		h.addElectricObject(eqNi, eqCell);
	}
	
	private void highlightEquivOfGeometric(List<Geometric> geoms, 
			                        	   Equivalence e, CellContext cc) {
		if (geoms.size()==0) {
			prln("I couldn't find anything to highlight");
			return;
		}
		
		Geometric g = geoms.iterator().next();
		if (!(g instanceof NodeInst)) {
			prln("Highlighted object is not a nodeInst: ");
			g.getInfo();
			return;
		}
		highlightEquivOfNodeInst((NodeInst)g, e, cc);
	}
		
	
	private void highlight1() {
		NccResults results = NccJob.getLastNccResults();
		if (results==null) {
			prln("No saved NCC results.  Please run NCC first.");
			return;
		}
		UserInterface ui = Job.getUserInterface();
		EditWindow_ wnd = ui.needCurrentEditWindow_();
		if (wnd==null) {
			prln("No current Window");
			return;
		}
		CellContext cc = NccUtils.getCellContext(wnd);
		NccResult r = results.getResultFromRootCells();
		Equivalence e = r.getEquivalence();
		List<Geometric> geoms = wnd.getHighlightedEObjs(true, false);
		if (geoms.size()>0) {
			highlightEquivOfGeometric(geoms, e, cc);
		} else {
			Set<Network> nets = wnd.getHighlightedNetworks();
			highlightEquivOfNet(nets, e, cc);
		}
	}
	
	private HighlightEquivalent() {highlight1();}
	
	// -------------------------- public method -------------------------------
	public static void highlight() {new HighlightEquivalent();}
}
