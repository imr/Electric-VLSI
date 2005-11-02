/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Placer.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.generator.layout;

import java.util.ArrayList;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.generator.layout.gates.WellTie;

class Placer {
	private static final boolean VERBOSE = false;
	
	public static final int P = 0;        // PMOS only gate
	public static final int N = 1;        // NMOS only gate
	public static final int PN = 2;       // PMOS and NMOS gate
	
	private StdCellParams stdCell;
	private Cell part;
	private double rowHeight;
	
	private ArrayList<Inst> buildInsts = new ArrayList<Inst>();
	private ArrayList<Net> buildNets = new ArrayList<Net>();
	
	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}
	
	// ---------------------- private classes --------------------------
	interface PermutationAction {
		// return true if you want to stop checking other permutations
		boolean usePermutation(int[] permutation);
		boolean prunePermutation(int[] permutation, boolean[] placed,
								 int depth);
	}
	
	private static class PermChecker implements PermutationAction {
		int[] bestPermutation = null;
		double bestCost = Double.MAX_VALUE;
		ArrayList<Inst> insts;
		ArrayList<Net> nets;
		double leftX;
		ArrayList<Inst> permInsts = new ArrayList<Inst>();
		long nbChecked, maxPerms;
		
		private void abutLeftRight(int[] permutation) {
			for (int i=0; i<permutation.length; i++) {
				permInsts.set(i, insts.get(permutation[i]));
			}
			Placer.abutLeftRight(leftX, permInsts);
		}
		
		// returns true to abort checking
		public boolean usePermutation(int[] permutation) {
			error(permutation.length!=insts.size(), "wrong permutation size");
			abutLeftRight(permutation);
			double cost = getCostX(nets);
			if (cost<bestCost) {
				bestCost = cost;
				bestPermutation = (int[]) permutation.clone();
				//System.out.println("Cost= "+cost);
			}
			nbChecked++;
			if (nbChecked>=maxPerms) {
				if (VERBOSE) {
					System.out.println("Abort after checking "+maxPerms+" permutations");
				}
				return true;
			}
			return false;
		}
		// Return the X coordinate of the right boundary of the rightmost
		// placed cell.  Any ports to the left of this are placed. Any
		// ports to the right of this are unplaced.  Return -1 if we
		// shouldn't prune.
		private double abutLeftRight(int[] permutation, boolean[] placed,
									 int depth) {
			double pX=leftX, nX=leftX;
			for (int i=0; i<=depth; i++) {
				Inst inst = (Inst) insts.get(permutation[i]);
				if (inst.isN()) {
					// NMOS only gate
					inst.moveTo(nX, 0);
					nX += inst.getWidth();
				} else if (inst.isP()) {
					// PMOS only gate
					inst.moveTo(pX, 0);
					pX += inst.getWidth();
				} else {
					// full height NMOS and PMOS gate
					double x = Math.max(nX, pX);
					inst.moveTo(x, 0);
					pX = nX = x+inst.getWidth();
				}
			}
			if (pX!=nX) {
				// Abut failed because I don't know how to find a set of
				// positions representing a tight lower bound on the cost of
				// the unplaced instances.  We will simply choose not to prune
				// in this case.
				return -1;
			}
			
			// Stack all unplaced cells on top of each other. Cost can't
			// possibly be less than this.
			for (int i=0; i<placed.length; i++) {
				if (!placed[i]) {
					Inst inst = (Inst) insts.get(i);
					inst.moveTo(nX, 0);
				}
			}
			return nX;
		}
		public boolean prunePermutation(int[] permutation, boolean[] placed,
										int depth) {
			// If we've placed all but one, there's nothing to prune.
			// If we've placed all but two, we only save checking 4 permutations.
			// Only consider pruning if there are three or more unplaced cells.
			int nbUnplaced = placed.length - (depth+1);
			if (nbUnplaced<=2) return false;
			
			double maxX = abutLeftRight(permutation, placed, depth);
			if (maxX==-1) return false;
			
			double cost = getPlacedCostX(nets, maxX);
			boolean prune = cost>=bestCost;
			/*
			  if (prune) {
			  if (nbUnplaced>=5) 
			  System.out.println("Pruning at placed/total: "+(depth+1)+"/"+placed.length);
			  }
			*/
			return prune;
		}
		private double getBestCost() {return bestCost;}
		private ArrayList<Inst> getBestPermutation() {
			ArrayList<Inst> best = new ArrayList<Inst>();
			for (int i=0; i<bestPermutation.length; i++)
				best.add(insts.get(bestPermutation[i]));
			return best;
		}
		PermChecker(ArrayList<Inst> insts, ArrayList<Net> nets, double leftX,
					int maxPerms) {
			this.insts=insts;  this.nets=nets;  this.leftX=leftX;
			this.maxPerms=maxPerms; nbChecked=0;
			
			for (int i=0; i<insts.size(); i++)  permInsts.add(null);
		}
	}
	
	// ------------------------ Private methods ------------------------
	private static void updateElectric(ArrayList<Inst> insts, double rowHeight) {
		for (int i=0; i<insts.size(); i++) {
			((Inst)insts.get(i)).updateElectric(rowHeight);
		}
	}
	
	private static void abutLeftRight(double leftX, ArrayList<Inst> insts) {
		double pX=leftX, nX=leftX;
		for (int i=0; i<insts.size(); i++) {
			Inst inst = (Inst) insts.get(i);
			if (inst.isN()) {
				// NMOS only gate
				inst.moveTo(nX, 0);
				nX += inst.getWidth();
			} else if (inst.isP()) {
				// PMOS only gate
				inst.moveTo(pX, 0);
				pX += inst.getWidth();
			} else {
				// full height NMOS and PMOS gate
				double x = Math.max(nX, pX);
				inst.moveTo(x, 0);
				pX = nX = x+inst.getWidth();
			}
		}
	}
	
	private static double getCostX(ArrayList<Net> nets) {
		double cost = 0;
		for (int i=0; i<nets.size(); i++) {
			cost += ((Net)nets.get(i)).getCostX();
		}
		return cost;
	}
	private static double getCost2row(ArrayList<Net> nets) {
		double cost = 0;
		for (int i=0; i<nets.size(); i++) {
			cost += ((Net)nets.get(i)).getCost2row();
		}
		return cost;
	}
	
	// Any ports to the right of maxX are unplaced. Compute the cost as
	// if they are all at maxX.
	private static double getPlacedCostX(ArrayList<Net> nets, double maxX) {
		double cost = 0;
		for (int i=0; i<nets.size(); i++) {
			cost += ((Net)nets.get(i)).getPlacedCostX(maxX);
		}
		return cost;
	}
	
	// returns true to stop checking
	private static boolean forEachPermutation1(int depth, boolean[] mask,
											   int[] permutation,
											   PermutationAction action) {
		int sz = mask.length;
		if (depth>sz-1) {
			// do something with permutation
			return action.usePermutation(permutation);
		}
		for (int i=0; i<sz; i++) {
			if (mask[i]==false) {
				permutation[depth] = i;
				mask[i] = true;
				if (!action.prunePermutation(permutation, mask, depth)) {
					if (forEachPermutation1(depth+1, mask, permutation,
											action)) {
						return true;
					}
				}
				mask[i] = false;
			}
		}
		return false;
	}
	
	// find all permuatations of nb objects
	private static void forEachPermutation(int nb, PermutationAction action) {
		error(nb==0, "permutations of nothing?");
		int[] permutation = new int[nb];
		boolean[] mask = new boolean[nb];
		for (int i=0; i<nb; i++)  mask[i]=false;
		forEachPermutation1(0, mask, permutation, action);
	}
	
	private static long factorial(int i) {
		if (i==0) return 0;
		if (i==1) return 1;
		return i * factorial(i-1);
	}
	
	// try every permutation: exponential
	private static ArrayList<Inst> exhaustive(ArrayList<Inst> insts, ArrayList<Net> nets,
										double leftX, int maxPerms) {
		PermChecker checker = new PermChecker(insts, nets, leftX, maxPerms);
		int nbGates = insts.size();
		if (nbGates==0) return new ArrayList<Inst>();
		if (VERBOSE) {
			System.out.print("Number of gates: "+nbGates);
			System.out.println(", Number of permutations: "+
							   factorial(nbGates));
		}
		forEachPermutation(nbGates, checker);
		return checker.getBestPermutation();
	}
	
	// The distance between ties should be no more than maxDist.  The
	// distance from the edge to the closest tie should be maxDist/2.
	private static ArrayList<Inst> insertWellTies(ArrayList<Inst> insts,
											StdCellParams stdCell, Cell part) {
		// In order to patch right most well gaps, add a full height dummy
		// instance at the end.  Remove it after we're done.
		insts = (ArrayList) insts.clone(); // don't modify input list
		Inst dummy = new Inst(PN, 0, null);
		insts.add(dummy);
		
		// build smallest well ties containing 1 well contact.
		Cell pTie = WellTie.makePart(false, true, 0, stdCell);
		Cell nTie = WellTie.makePart(true, false, 0, stdCell);
		
		// default width of WellTie containing 1 contact
		final double tieWid = pTie.getBounds().getWidth();
		
		// maximum distance from right edge of well tie to closest contact
		final double tieEdgeToContDist = WellTie.edgeToContDist();
		
		double pX=0, nX=0;
		double maxDist = stdCell.getWellTiePitch();
		double pSp=maxDist/2, nSp=maxDist/2;
		for (int i=0; i<insts.size(); i++) {
			Inst inst = (Inst) insts.get(i);
			double instWid = inst.getWidth();
			if (instWid > maxDist) {
				System.out.println("The gate: "+
								  inst.getNodeInst().getProto().getName()+
								   " is larger than the well tap spacing!!!");
			}
			
			boolean nAbuts = nX>=pX;
			double testDist = inst==dummy ? maxDist/2 : maxDist;
			if (inst.isN() || (inst.isPN() && nAbuts)) {
				// NMOS abuts NMOS
				if ((nSp + instWid > testDist) && nSp!=0) {
					// insert NMOS well tie
					NodeInst ni = LayoutLib.newNodeInst(nTie,0,0,0,0,0,part);
					insts.add(i, new Inst(N, tieWid, ni));
					nX += tieWid;
					nSp = 0;
				} else if (inst.isPN()) {
					// abut full height gate
					double gap = nX-pX;
					if (gap>0) {
						// There's a PMOS well gap. Patch it.
						Cell patch = WellTie.makePart(false, true,gap,stdCell);
						NodeInst ni =
							LayoutLib.newNodeInst(patch,0,0,0,0,0,part);
						insts.add(i, new Inst(P, gap, ni));
						i++;
						pSp = gap>=tieWid ? tieEdgeToContDist : pSp + gap;
					}
					pX = nX = nX + instWid;
					nSp += instWid;
					pSp += instWid;
				} else {
					// abut half height NMOS
					nX += instWid;
					nSp += instWid;
				}
			} else if (inst.isP() || (inst.isPN() && !nAbuts)) {
				// PMOS abuts PMOS
				if ((pSp + instWid > testDist) && pSp!=0) {
					// insert PMOS well tie
					NodeInst ni = LayoutLib.newNodeInst(pTie,0,0,0,0,0,part);
					insts.add(i, new Inst(P, tieWid, ni));
					pX += tieWid;
					pSp = 0;
				} else if (inst.isPN()) {
					// abut full height gate
					double gap = pX-nX;
					if (gap>0) {
						// There's a NMOS well gap. Patch it.
						Cell patch = WellTie.makePart(true, false,gap,stdCell);
						NodeInst ni =
							LayoutLib.newNodeInst(patch,0,0,0,0,0,part);
 						insts.add(i, new Inst(N, gap, ni));
						i++;
						nSp = gap>=tieWid ? tieEdgeToContDist : nSp + gap;
					}
					pX = nX = pX + instWid;
					pSp += instWid;
					nSp += instWid;
				} else {
					// abut half height PMOS
					pX += instWid;
					pSp += instWid;
				}
			}
		}
		insts.remove(dummy);
		return insts;
	}
	
	private static ArrayList<Inst> threeRegionPlace(ArrayList<Inst> insts, ArrayList<Net> nets,
											  int maxPerms) {
		ArrayList<Inst> nInsts=new ArrayList<Inst>(),  pInsts=new ArrayList<Inst>(),
			pnInsts=new ArrayList<Inst>();
		for (int i=0; i<insts.size(); i++) {
			Inst inst = (Inst) insts.get(i);
			if (inst.isN()) {
				nInsts.add(inst);
			} else if (inst.isP()) {
				pInsts.add(inst);
			} else {
				pnInsts.add(inst);
			}
		}
		ArrayList<Inst> allInsts = new ArrayList<Inst>(pnInsts);
		allInsts.addAll(nInsts);
		allInsts.addAll(pInsts);
		abutLeftRight(0, allInsts);
		
		Inst lastFull = (Inst) pnInsts.get(pnInsts.size()-1);
		double rightFullX = lastFull.getX() + lastFull.getWidth();
		
		ArrayList<Inst> ans = new ArrayList<Inst>(exhaustive(pnInsts, nets, 0, maxPerms));
		ans.addAll(exhaustive(nInsts, nets, rightFullX, maxPerms));
		ans.addAll(exhaustive(pInsts, nets, rightFullX, maxPerms));
		
		return ans;
	}
	
	// ------------------------ Public classes -----------------------------
	public static class Inst {
		double x;        // position of Cell reference
		int row;
		double w;
		boolean mirrorX, mirrorY;
		int type;           // P, N, or PN
		NodeInst nodeInst;  // allows us to position the part instance
		
		ArrayList<Port> ports = new ArrayList<Port>();
		
		Inst(int type, double width, NodeInst nodeInst) {
			error(type!=P && type!=N && type!=PN, "Placer.Inst: bad type: "+type);
			this.type=type; w=width;
			this.nodeInst = nodeInst;
		}
		
		int nbPorts() {return ports.size();}
		Port getPort(int i) {return (Port) ports.get(i);}
		public Port addPort(double ofstX, double ofstY) {
			Port p = new Port(this, ofstX, ofstY);
			ports.add(p);
			return p;
		}
		
		void moveTo(double x, int row) {this.x=x; this.row=row;}
		double getX() {return x;}
		double getMaxX() {return x+w;}
		int getRow() {return row;}
		
		void setMirrorX(boolean mirror) {mirrorX=mirror;}
		boolean getMirrorX() {return mirrorX;}
		void setMirrorY(boolean mirror) {mirrorY=mirror;}
		boolean getMirrorY() {return mirrorY;}
		boolean isN() {return type==N;}
		boolean isP() {return type==P;}
		boolean isPN() {return type==PN;}
		double getWidth() {return w;}
		NodeInst getNodeInst() {return nodeInst;}
		
		// Move NodeInsts in Electric.
		void updateElectric(double rowHeight) {
			LayoutLib.modNodeInst(nodeInst, x, row*rowHeight,0,0,false,false,0);
		}
	}
	
	public static class Port {
		Inst inst;
		double ofstX, ofstY;
		Port(Inst inst, double ofstX, double ofstY) {
			this.inst=inst; this.ofstX=ofstX; this.ofstY=ofstY;
		}
		double getX() {
			double offset = inst.getMirrorX() ? (inst.getWidth()-ofstX) : ofstX;
			return inst.getX() + offset;
		}
		int getRow() {return inst.getRow();}
		Inst getInst() {return inst;}
	}
	
	public static class Net {
		ArrayList<Port> ports = new ArrayList<Port>();
		
		int nbPorts() {return ports.size();}
		Port getPort(int i) {return (Port) ports.get(i);}
		public void addPort(Port port) {ports.add(port);}
		
		double getCostX() {
			// handle special case because otherwise we return -infinity
			if (nbPorts()==0)  return 0;
			
			double minX = Double.MAX_VALUE;
			double maxX = Double.MIN_VALUE;
			for (int i=0; i<ports.size(); i++) {
				double x = getPort(i).getX();
				minX = Math.min(minX, x);
				maxX = Math.max(maxX, x);
			}
			return maxX-minX;
		}
		
		// Any port to the right of unplacedX is unplaced. Compute the
		// cost as if all those ports are at unplacedX.
		double getPlacedCostX(double unplacedX) {
			double minX = Double.MAX_VALUE;
			double maxX = Double.MIN_VALUE;
			for (int i=0; i<ports.size(); i++) {
				double x = getPort(i).getX();
				minX = Math.min(minX, x);
				maxX = Math.max(maxX, x);
			}
			maxX = Math.min(maxX, unplacedX);
			minX = Math.min(minX, unplacedX);
			return maxX-minX;
		}
		
		double getCost2row() {
			final int nbRows = 2;
			double[] minX = new double[nbRows],
				maxX = new double[nbRows],
				costX = new double[nbRows];
			for (int i=0; i<nbRows; i++) {
				minX[i] = Double.MAX_VALUE;  maxX[i] = Double.MIN_VALUE;
			}
			for (int i=0; i<ports.size(); i++) {
				Port port = getPort(i);
				double x = port.getX();
				int r = port.getRow();
				minX[r] = Math.min(minX[r], x);
				maxX[r] = Math.max(maxX[r], x);
			}
			double cost = 0;
			for (int i=0; i<nbRows; i++) {
				if (minX[i]!=Double.MAX_VALUE)  cost += maxX[i] - minX[i];
			}
			return cost;
		}
	}
	
	// -------------------------- public methods------------------------------
	public Placer(StdCellParams stdCell, Cell part) {
		this.stdCell=stdCell;  this.part=part;
		rowHeight = stdCell.getNmosWellHeight() + stdCell.getPmosWellHeight();
	}
	
	public Inst addInst(int type, double w, NodeInst nodeInst) {
		Inst inst = new Inst(type, w, nodeInst);
		buildInsts.add(inst);
		return inst;
	}
	
	public Net addNet() {
		Net net = new Net();
		buildNets.add(net);
		return net;
	}
	
	public ArrayList<NodeInst> place1row() {
		int maxPerms = stdCell.getNbPlacerPerms();
		ArrayList<Inst> insts = threeRegionPlace(buildInsts, buildNets, maxPerms);
		abutLeftRight(0, insts);
		double threeRegCost = getCostX(buildNets);
		
		if (stdCell.getExhaustivePlace()) {
			insts = exhaustive(insts, buildNets, 0, stdCell.getNbPlacerPerms());
			abutLeftRight(0, insts);
			double exhCost = getCostX(buildNets);
			double improve = Math.rint(1000 * (threeRegCost-exhCost)/threeRegCost)/10;
			if (VERBOSE) {
				System.out.println("exhaustive search improvement: "+improve+"%");
			}
		}
		
		insts = insertWellTies(insts, stdCell, part);
		abutLeftRight(0, insts);
		updateElectric(insts, rowHeight);
		
		Inst rightInst = (Inst) insts.get(insts.size()-1);
		stdCell.addEssentialBounds(0, rightInst.getMaxX(), part);
		
		ArrayList<NodeInst> nodeInsts = new ArrayList<NodeInst>();
		for (int i=0; i<insts.size(); i++) {
			nodeInsts.add(((Inst)insts.get(i)).getNodeInst());
		}
		return nodeInsts;
	}
	
	public ArrayList place2row() {
		return new ArrayList();
	}
}
