package com.sun.electric.tool.generator.flag.hornFunnel2;

import java.util.ArrayList;
import java.util.List;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.Job;

public class SkewTree {
	private static final String OUT_LIB_NM = "treePictures";
	
	static void pr(String s) {System.out.print(s);}
	static void prln(String s) {System.out.println(s);}
	private int pass = 1;

	private void drawTree(Library outLib, BinaryTree tree, int pass) {
		Cell c = Cell.newInstance(outLib, "skewTree"+tree.getHeight()+
				                  "pass"+pass+"{sch}");
		prln("");
		prln("Skewed tree height "+tree.getHeight()+" pass "+pass);
		tree.draw(c);
		
	}
	private void printNodesToMove(List<Node> nodes, int h) {
		prln("Nodes to move at height "+h);
		for (Node n : nodes) {
			prln("  "+n.toString());
		}
	}
	/** Make space in slot dstSlot for a node of level moveableHeight */
	private void makeSpaceLeft(BinaryTree tree, int dstSlot, int moveableHeight) {
		Node n = tree.getNodeInSlot(dstSlot);
		Job.error(n.getHeight()>moveableHeight,
				        "can't make space because destintation is locked: "+
				        n.toString());
		// Put Node with height < moveableHeight into dstSlot
		for (int i=dstSlot; i>=0; i--) {
			Node hole = tree.getNodeInSlot(i);
			if (hole.getHeight()<moveableHeight) {
				tree.moveTo(hole, dstSlot, moveableHeight);
				return;
			}
		}
		Job.error(true, "can't find a node with height < moveableHeight");
	}
	/** Make space in slot dstSlot for a node of level moveableHeight */
	private void makeSpaceRight(BinaryTree tree, int dstSlot, int moveableHeight) {
		Node n = tree.getNodeInSlot(dstSlot);
		Job.error(n.getHeight()>moveableHeight,
				        "can't make space because destintation is locked: "+
				        n.toString());
		// Put Node with height < moveableHeight into dstSlot
		for (int i=dstSlot; i<tree.getNumSlots(); i++) {
			Node hole = tree.getNodeInSlot(i);
			if (hole.getHeight()<moveableHeight) {
				tree.moveTo(hole, dstSlot, moveableHeight);
				return;
			}
		}
		Job.error(true, "can't find a node with height < moveableHeight");
	}
	private void moveNodesLeft(BinaryTree tree, List<Node> nodes, 
		   	                   int dstSlot, int moveableHeight) {
		for (Node n : nodes) {
			makeSpaceLeft(tree, dstSlot, moveableHeight);
			tree.moveTo(n, dstSlot, moveableHeight);
		}
	}
	private void moveNodesRight(BinaryTree tree, List<Node> nodes, 
                                int dstSlot, int moveableHeight) {
		for (Node n : nodes) {
			makeSpaceRight(tree, dstSlot, moveableHeight);
			tree.moveTo(n, dstSlot, moveableHeight);
		}
	}
	private boolean isChildOf(Node child, Node subTree) {
		for (Node n=child; n!=null; n=n.getParent()) {
			if (n==subTree) return true;
		}
		return false;
	}
	private List<Node> findNodesToMoveLeft(BinaryTree tree, int nodeHeight, 
			                               int dstSlot, Node subTree) {
		List<Node> toMove = new ArrayList<Node>();
		for (int i=0; i<tree.getNumSlots(); i++) {
			Node n = tree.getNodeInSlot(i);
			if (n.getHeight()==nodeHeight &&
				n.getSlot()>dstSlot &&
				isChildOf(n, subTree)) {
				toMove.add(n);
			}
		}
		return toMove;
	}
	private List<Node> findNodesToMoveRight(BinaryTree tree, int nodeHeight, 
            	                            int dstSlot, Node subTree) {
		List<Node> toMove = new ArrayList<Node>();
		for (int i=0; i<tree.getNumSlots(); i++) {
			Node n = tree.getNodeInSlot(i);
			if (n.getHeight()==nodeHeight &&
				n.getSlot()<dstSlot &&
				isChildOf(n, subTree)) {
				toMove.add(n);
			}
		}
		return toMove;
	}

	/** Left shift the Nodes on the boundary of subTree so that no wire exceeds 
	 * segLen. The parent of subTree occupies slot subTreeParentSlot */
	private void shiftBoundaryNodesLeft(BinaryTree tree, Node subTree, 
			                            int subTreeParentSlot,
			                            int firstSegLen, int restSegLen, 
			                            Library outLib) {
		int height = subTree.getHeight();
		
		for (int h=height; h>=2; h--) {
			int dstSlot = subTreeParentSlot + firstSegLen + restSegLen*(height-h);
			List<Node> mustMove = findNodesToMoveLeft(tree, h, dstSlot, subTree);
			printNodesToMove(mustMove, h);
			if (mustMove.size()==0) continue; // Don't print tree if no change from previous
			moveNodesLeft(tree, mustMove, dstSlot, h);
			drawTree(outLib, tree, pass++);
		}
	}
	/** Right shift the Nodes on the boundary of subTree so that no wire exceeds 
	 * segLen. The parent of subTree occupies slot subTreeParentSlot */
	private void shiftBoundaryNodesRight(BinaryTree tree, Node subTree, 
			                             int subTreeParentSlot,
			                             int firstSegLen, int restSegLen, 
			                             Library outLib) {
		int height = subTree.getHeight();
		
		for (int h=height; h>=2; h--) {
			int dstSlot = subTreeParentSlot - firstSegLen - restSegLen*(height-h);
			List<Node> mustMove = findNodesToMoveRight(tree, h, dstSlot, subTree);
			printNodesToMove(mustMove, h);
			if (mustMove.size()==0) continue; // Don't print tree if no change from previous
			moveNodesRight(tree, mustMove, dstSlot, h);
			drawTree(outLib, tree, pass++);
		}
	}

	/** The wire to the children of subTree is too long. Skew subTree's
	 * descendents so that the wire is no longer than segLen */
	private void optimizeSubTree(BinaryTree tree, Node subTree, 
			                     int segLen, Library outLib) {
		prln("Optimize subTree: "+subTree.toString());
		Node leftChild = subTree.getLeftChild();
		Node rightChild = subTree.getRightChild();
		int subTreeSlot = subTree.getSlot();
		int leftChildSlot = leftChild.getSlot();
		int rightChildSlot = rightChild.getSlot();
		if (subTreeSlot<leftChildSlot) {
			// subTree root is left of it's children
			// skew Nodes on the upper right boundary of subTree
			shiftBoundaryNodesLeft(tree, rightChild, subTreeSlot, segLen, segLen, outLib);
		} else if (subTreeSlot>rightChildSlot) {
			// skew Nodes on the upper left boundary of subTree
			shiftBoundaryNodesRight(tree, leftChild, subTreeSlot, segLen, segLen, outLib);
		} else {
			// skew Nodes on the upper right and left boundaries of subTree
			// split segLen between left and right halves of subTree.
			// If segLen is odd, arbitrarily let right subtree have extra length.
			int leftSegLen = segLen / 2;
			int rightSegLen = segLen / 2 + (segLen % 2);
			shiftBoundaryNodesLeft(tree, rightChild, subTreeSlot, rightSegLen, segLen, outLib);
			shiftBoundaryNodesRight(tree, leftChild, subTreeSlot, leftSegLen, segLen, outLib);
		}
	}
	private void optimizationIteration(BinaryTree t, int segLen, Library outLib) {
		// First skew is a special case because t's parent isn't represented
		// by the data structures.
		shiftBoundaryNodesLeft(t, t.getRoot(), -1, segLen, segLen, outLib); 
		
		while (true) {
			Node n = t.getNodeWithLongestChildWire();
			if (n.getChildWireLength()<=segLen) break;
			if (pass>14) break;
			optimizeSubTree(t, n, segLen, outLib);
		}
	}
	private void doIt1() {
		Library outLib = LayoutLib.openLibForWrite(OUT_LIB_NM);
		int height = 8;
		BinaryTree t = new BinaryTree(height);

		prln("Generate skewed trees of height "+height);
		int segLen = t.getLowBoundWireLen();
		prln("Lower bound on wire length: "+segLen);

		Cell c = Cell.newInstance(outLib, "skewTree"+height+"pass0{sch}");
		
		prln("Symmetric tree of height "+height);
		t.draw(c);
		try {
			//skew2(t, outLib);
			optimizationIteration(t, segLen, outLib);
		} catch (Throwable th) {
			prln("Exception caught in SkewTree: "+th);
		}
	}
	public static void doIt() {
		SkewTree sk = new SkewTree();
		sk.doIt1();
	}
}
