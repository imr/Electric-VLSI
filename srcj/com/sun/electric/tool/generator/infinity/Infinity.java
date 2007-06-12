package com.sun.electric.tool.generator.infinity;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.TechType;

public class Infinity {
	private static final String STAGE_LIB_NAME = new String("stagesF");
	private static final String AUTO_GEN_LIB_NAME = new String("autoInfinity");
    private static final TechType tech = TechType.CMOS90;
    private static final double STAGE_SPACING = 10;
	
	private void prln(String s) {System.out.println(s);}
	
	private Stages findStageCells() {
		Library lib = Library.findLibrary(STAGE_LIB_NAME);
		Stages stages = new Stages(lib);
		if (lib==null) {
			prln("Please open the library containing stage cells: "+
				 STAGE_LIB_NAME);
		}
		return stages;
	}
	
	private void ensurePwrGndExportsOnBoundingBox(Collection<Cell> stages) {
		for (Cell c : stages) {
			Rectangle2D bBox = c.findEssentialBounds();
			if (bBox==null) {
				prln("Stage: "+c.getName()+" is missing essential bounds");
				continue;
			}
			double minX = bBox.getMinX();
			double maxX = bBox.getMaxX();
			double minY = bBox.getMinY();
			double maxY = bBox.getMaxY();
			for (Iterator it=c.getExports(); it.hasNext();) {
				Export e = (Export) it.next();
				PortCharacteristic pc = e.getCharacteristic();
				if (pc==PortCharacteristic.PWR || pc==PortCharacteristic.GND) {
					PortInst pi = e.getOriginalPort();
					EPoint center = pi.getCenter();
					double x = center.getX();
					double y = center.getY();
					if (x!=minX && x!=maxX && y!=minY && y!=maxY) {
						prln("Cell: "+c.getName()+", Export: "+e.getName()+
							 " Export not on Cell Bounding Box");
						prln("  Bounding box: ("+minX+", "+minY+") ("+
							 maxX+", "+maxY+")");
						prln("  Export coordinates: ("+x+", "+y+")");
					}
				}
			}
		}
	}
	
	private Collection<NodeInst> addInstances(Cell parentCell, Stages stages) {
		Collection<NodeInst> stageInsts = new ArrayList<NodeInst>();
		for (Cell c : stages.getStages()) {
			stageInsts.add(LayoutLib.newNodeInst(c, 0, 0, 0, 0, 0, parentCell));
		}
		LayoutLib.abutBottomTop(stageInsts, STAGE_SPACING);
		return stageInsts;
	}
	
	private void connectPwrGnd(Collection<NodeInst> stageInsts) {
		
	}
	
	public Infinity() {
		prln("Generating layout for Infinity");
		Stages stages = findStageCells();
		if (stages.someStageIsMissing()) return;
		ensurePwrGndExportsOnBoundingBox(stages.getStages());

        Library outLib = LayoutLib.openLibForWrite(AUTO_GEN_LIB_NAME);
        Cell parentCell = Cell.newInstance(outLib, "infinityAuto");
        Collection<NodeInst> stageInsts = addInstances(parentCell, stages);
        connectPwrGnd(stageInsts);

        System.out.println("done.");
	}
}
