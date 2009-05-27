package com.sun.electric.tool.generator.flag;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.Cell.CellGroup;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;


public class FlagLibraryCheckerJob extends Job {
	private static final long serialVersionUID = 0;
	
	private void prln(String msg) {System.out.println(msg);}
	private Set<CellGroup> getCellGroupsToCheck() {
		Set<CellGroup> groupsToCheck = new HashSet<CellGroup>();
		for (Iterator<Library> liIt=Library.getLibraries(); liIt.hasNext();) {
			Library lib = liIt.next();
			for (Iterator<Cell> cellIt=lib.getCells(); cellIt.hasNext();) {
				Cell cell = cellIt.next();
				if (cell.isSchematic()) {
					FlagAnnotations ann = new FlagAnnotations(cell);
					if (ann!=null && ann.isAtomic()) {
						groupsToCheck.add(cell.getCellGroup());
					}
				}
			}
		}
		return groupsToCheck;
	}
	private Cell findLayoutCell(CellGroup cg) {
		Cell layCell = null;
		for (Iterator<Cell> cIt=cg.getCells(); cIt.hasNext();) {
			Cell c = cIt.next();
			if (c.getView()==View.LAYOUT) {
				if (layCell!=null) {
					prln("    Cell group has more than one layout Cell");
					break;
				}
				layCell = c;
			}
		}
		return layCell;
	}
	private void checkCell(Cell c) {
		Rectangle2D bnds = c.findEssentialBounds();
		if (bnds==null) {
			prln("Stage: "+c.getName()+" is missing essential bounds");
			return;
		}
		for (Iterator it=c.getExports(); it.hasNext();) {
			Export e = (Export) it.next();
//			if (Utils.isPwrGnd(e)) {
//				PortInst pi = e.getOriginalPort();
//				if (!Utils.onBounds(pi, bnds, 0)) {
//					prln("Cell: "+c.getName()+", Export: "+e.getName()+
//						 " Export not on Cell Bounding Box");
//					prln("  Bounding box: "+bnds.toString());
//					prln("  Port Center: "+pi.getCenter().toString());
//					Utils.onBounds(pi, bnds, 0);
//				}
//				double w = LayoutLib.widestWireWidth(pi);
//				if (w!=9) {
//					prln("Cell: "+c.getName()+", power or ground Export: "+
//						 e.getName()+" has width: "+w);
//				}
//			} else {
				PortCharacteristic pc = e.getCharacteristic();
				if (pc!=PortCharacteristic.PWR && pc!=PortCharacteristic.GND &&
					pc!=PortCharacteristic.IN && pc!=PortCharacteristic.OUT &&
					pc!=PortCharacteristic.BIDIR) {
					prln("Cell: "+c.getName()+" Export "+e+" has undesired characteristic: "+pc);
				}
//			}
		}

	}
	private void checkCellGroup(CellGroup cg) {
		prln("  Checking layout: "+cg.getName());
		Cell layCell = findLayoutCell(cg);
		if (layCell==null) {
			prln("    Cell group has no layout Cell");
			return;
		}
		checkCell(layCell);
	}
	private void checkCellGroups(Collection<CellGroup> cellGroups) {
		for (CellGroup cg : cellGroups) {
			checkCellGroup(cg);
		}
	}
	
	@Override 
	public boolean doIt() throws JobException {
		Set<CellGroup> cellsToCheck = getCellGroupsToCheck(); 
		prln("FLAG Found "+cellsToCheck.size()+" atomic Cell groups");
		checkCellGroups(cellsToCheck);
		return true;
	}
	public FlagLibraryCheckerJob() {
        super("FLAG library checker", NetworkTool.getNetworkTool(), Job.Type.CLIENT_EXAMINE, null, null, Job.Priority.USER);
        startJob();
	}
}
