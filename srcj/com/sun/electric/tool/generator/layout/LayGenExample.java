package com.sun.electric.tool.generator.layout;

import java.awt.geom.Rectangle2D;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.generator.layout.gates.Inv;
import com.sun.electric.tool.user.User;
/**
 * Example of how to instantiate cells and connect them.
 * For Ivan.
 * @author rkao
 *
 */

public class LayGenExample extends Job {
	public static final long serialVersionUID = 0;
	
	private void makeInv() {
		StdCellParams stdCell = new StdCellParams(TechType.TSMC180);
		stdCell.setSimpleName(true);
        Tech.setTechnology(TechType.TSMC180);
        String outLibNm = "exampleLib";
        Library outLib = LayoutLib.openLibForWrite(outLibNm);
        stdCell.setOutputLibrary(outLib);
        Inv.makePart(12, stdCell);
	}
	
	private Cell makeParentCell() {
		Library lib = Library.findLibrary("exampleLib");
		return Cell.newInstance(lib, "exampleCell");
	}
	
	private NodeInst[] instantiateInverters(Cell parent) {
		NodeInst[] invs = new NodeInst[10];
		Library lib = Library.findLibrary("exampleLib");
		Cell inv = lib.findNodeProto("inv_X012.2");
		Rectangle2D bounds = inv.findEssentialBounds();
		double w = bounds.getWidth();
		for (int i=0; i<10; i++) {
			invs[i] = LayoutLib.newNodeInst(inv,w*i,0,0,0,0,parent);
		}
		return invs;
	}
	
	private void connectInverters(NodeInst[] invs, TechType tech, Cell parent) {
		TrackRouter inBus = new TrackRouterH(tech.m2(), 4, 12, parent);
		for (int i=0; i<10; i++) {
			PortInst pi = invs[i].findPortInst("in");
			inBus.connect(pi);
		}
		TrackRouter outBus = new TrackRouterH(tech.m3(), 5, -12, parent);
		for (int i=0; i<10; i++) {
			PortInst pi = invs[i].findPortInst("out");
			outBus.connect(pi);
		}
	}
	
	private void doYourJob() {
		System.out.println("Hello world");
		TechType tech = TechType.TSMC180;
		makeInv();
		Cell parent = makeParentCell();
		
		NodeInst[] invs = instantiateInverters(parent);
		connectInverters(invs, tech, parent);
	}
	
	
	
	//--------------------------- public methods ------------------------------
	public LayGenExample() {
        super("Layout generation example", User.getUserTool(), Job.Type.CHANGE,
                null, null, Job.Priority.ANALYSIS);
        startJob();
	}
	
	@Override
	public boolean doIt() {
		doYourJob();
		return true;
	}

}
