package com.sun.electric.tool.generator.flag.hornFunnel2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.Job;

public class BinaryTree {
	private static final double DEF_SIZE = LayoutLib.DEF_SIZE;
	private final boolean SLANTED_EDGES = true;
	private static final Technology SCHEM_TECH = 
		Technology.findTechnology("schematic");
	private static final PrimitiveNode SCH_ICON_PROTO = 
		SCHEM_TECH.findNodeProto("Buffer");
	private static final PrimitiveNode DOT_ICON_PROTO =
		SCHEM_TECH.findNodeProto("Wire_Pin");
	private static final ArcProto LINE_PROTO = 
		SCHEM_TECH.findArcProto("Wire"); 

	private final int SLOT_WID = 6;
	private final int SLOT_HEI = 24;
	private List<Node> slots;
	private final int height;
	private final int numSlots;
	private final Node root;
	
	private void prln(String s) {System.out.println(s);}
	private void pr(String s) {System.out.print(s);}
	
	private List<Node> makeSlots(int nbSlots) {
		List<Node> slots = new ArrayList<Node>();
		for (int i=0; i<nbSlots; i++) slots.add(null);
		return slots;
	}
	
	private void drawExternalArc(Cell c, Map<Node, NodeInst> nodeToInst) {
		int dotX = -SLOT_WID;
		int dotY = (height+1) * (SLOT_HEI);
		NodeInst dot = LayoutLib.newNodeInst(DOT_ICON_PROTO, dotX, dotY, 
                                             DEF_SIZE, DEF_SIZE, 
                                             0, c);
		PortInst dotPi = dot.getOnlyPortInst();
		PortInst inPi = nodeToInst.get(root).findPortInst("a");
		drawArc(dotPi, inPi);
	}
	private void drawArc(PortInst p1, PortInst p2) {
		if (SLANTED_EDGES) {
			ArcInst aL = ArcInst.makeInstance(LINE_PROTO, p1, p2);
			aL.setFixedAngle(false);
		} else {
			LayoutLib.newArcInst(LINE_PROTO, DEF_SIZE, p1, p2);
		}
	}
	private void drawArcs(Node n, Map<Node, NodeInst> nodeToInst) {
		if (n.isLeaf()) return;
		PortInst cur = nodeToInst.get(n).findPortInst("y");
		PortInst left = nodeToInst.get(n.getLeftChild()).findPortInst("a");
		PortInst right = nodeToInst.get(n.getRightChild()).findPortInst("a");
		
		drawArc(cur, left);
		drawArc(cur, right);
		
		drawArcs(n.getLeftChild(), nodeToInst);
		drawArcs(n.getRightChild(), nodeToInst);
	}
	
	private void setSlot(int slot, Node n) {
		slots.set(slot, n);
		n.setSlot(slot);
	}
	
	public BinaryTree(int height) {
		this.height = height;
		numSlots = (int) Math.pow(2, height)-1;
		slots = makeSlots(numSlots);
		int halfNbSlots = (numSlots+1)/2;
		Node.resetIds();
		root = new Node(null, height, -1 + halfNbSlots, slots); 
	}
	public int getHeight() {return height;}
	public Node getRoot() {return root;}
	public int getNumSlots() {return numSlots;}	
	
	public void checkSlots() {
		Map<Node, Integer> nodeToNdx = new HashMap<Node, Integer>();
		for (int i=0; i<slots.size(); i++) {
			Node n = slots.get(i);
			Job.error(n.getSlot()!=i, "wrong slot. actual="+i+" incorrect="+n.getSlot());
			Integer index = nodeToNdx.get(n);
			Job.error(index!=null, "already at: "+index);
			nodeToNdx.put(n, i);
		}
	}
	
	private boolean isLocked(Node n, int movableHeight) {
		int h = n.getHeight();
		return h>movableHeight;
	}

	/** move Node n to slot dst */
	public void moveTo(Node n, int dst, int moveableHeight) {
		checkSlots();
		int src = n.getSlot();
		Job.error(isLocked(slots.get(dst), moveableHeight),
		                "moveTo fails because destination is locked. \n"+
		                "    src: "+slots.get(src).toString()+"\n"+
		                "    dest: "+slots.get(dst).toString());
		int incr = dst>=src ? 1 : -1;
		Node movee = slots.get(src);
		int wr = src;
		while (wr!=dst) {
			int rd = wr+incr;
			while (isLocked(slots.get(rd), moveableHeight))  rd+=incr;
			setSlot(wr, slots.get(rd));
			wr = rd;
		}
		setSlot(dst, movee);
		checkSlots();
	}
	/** Label each node with it's identifier */
	public void addId(NodeInst ni, int id) {
		ni.setName(""+id);
	}
	
	/** Draw a schematic Cell for this tree */
	public void draw(Cell c) {
		Map<Node, NodeInst> nodeToInst = new HashMap<Node, NodeInst>();
		for (Node n : slots) {
			int h = n.getHeight();
			int s = n.getSlot();
			double x = s * SLOT_WID;
			double y = h * SLOT_HEI;
			NodeInst ni = LayoutLib.newNodeInst(SCH_ICON_PROTO, x, y, 
					                            DEF_SIZE, DEF_SIZE, 
					                            270, c);
			addId(ni, n.getId());

			Job.error(nodeToInst.containsKey(n), "duplicate entry");
			nodeToInst.put(n, ni);
		}
		drawExternalArc(c, nodeToInst);
		drawArcs(root, nodeToInst);
		printStats();
	}

	public int calcWireLength() {
		// there's an implicit edge to the root node
		int wireLen = getRoot().getSlot()+1;
		for (Node n : slots) wireLen += n.getChildWireLength();
		return wireLen;
	}
	public int maxWireLength() {
		// there's an implicit edge to the root node
		int maxLen = getRoot().getSlot()+1;
		for (Node n : slots) 
			maxLen = Math.max(maxLen, n.getChildWireLength());
		return maxLen;
	}
	public int[] countTracks() {
		int[] counts = new int[numSlots];
		
		for (Node n : slots) {
			if (n.isLeaf()) continue;
			int minSlot = n.getMinChildWireSlot();
			int maxSlot = n.getMaxChildWireSlot();
			for (int i=minSlot; i<=maxSlot; i++)  counts[i]++;
		}
		// there's an implicit edge to the root node
		for (int i=0; i<=getRoot().getSlot(); i++)  counts[i]++;
			
		return counts;
	}
	public List<Node> getNodesSortedByChildWireLength() {
		List<Node> sorted = new ArrayList<Node>();
		sorted.addAll(slots);
		Collections.sort(sorted, new Comparator<Node> () {
			public int compare(Node n1, Node n2) {
				return n1.getChildWireLength() -
				       n2.getChildWireLength();  
			}
		});
		return sorted;
	}
	public Node getNodeWithLongestChildWire() {
		List<Node> sorted = getNodesSortedByChildWireLength();
		return sorted.get(sorted.size()-1);
	}
	public int getLowBoundWireLen() {
		return (int) Math.ceil(((double)getNumSlots()) / height);
	}
	public void printStats() {
		pr("Track counts: ");
		int[] counts = countTracks();
		int maxNbTracks = -1;
		for (int count : counts) {
			pr(count+" ");
			maxNbTracks = Math.max(maxNbTracks, count);
		}
		prln("");
		prln("Max numb tracks: "+maxNbTracks);
		
		prln("Total wire length: "+calcWireLength());
		
		prln("Maximum wire length: "+maxWireLength());
		
		int lowBoundWireLen = getLowBoundWireLen();
		
		prln("Lower bound on wire length: "+lowBoundWireLen);
		
		prln("Nodes with child wire length exceeding lower bound: ");
		for (Node n : getNodesSortedByChildWireLength()) {
			if (!n.isLeaf() && n.getChildWireLength()>lowBoundWireLen)
				prln(n.toString());
		}
	}
	public List<Node> getNodesAtHeight(int h) {
		List<Node> nodes = new ArrayList<Node>();
		for (Node n : slots)  if (n.getHeight()==h) nodes.add(n);
		return nodes;
	}
	public Node getNodeInSlot(int i) {return slots.get(i);}
	
}
