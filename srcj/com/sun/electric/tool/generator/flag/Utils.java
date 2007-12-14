package com.sun.electric.tool.generator.flag;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.tool.generator.flag.router.ToConnect;

public class Utils {
	/** Warning: this is global data and these are global methods. There
	 * is no other way of allowing static methods to use TaskPrinter */
	private static TaskPrinter taskPrinter = new TaskPrinter();
	public static void prln(String s) {taskPrinter.prln(s);}
	public static void pr(String s) {taskPrinter.pr(s);}
	public static void error(boolean cond, String msg) {taskPrinter.error(cond, msg);}
	public static void saveTaskDescription(String msg) {taskPrinter.saveTaskDescription(msg);}
	public static void clearTaskDescription() {taskPrinter.clearTaskDescription();}
	
	public static boolean isPwr(PortProto pp) {
		return pp.getCharacteristic()==PortCharacteristic.PWR;
	}
	public static boolean isPwr(PortInst pi) {
		return isPwr(pi.getPortProto());
	}
	public static boolean isGnd(PortProto pp) {
		return pp.getCharacteristic()==PortCharacteristic.GND;
	}
	public static boolean isGnd(PortInst pi) {
		return isGnd(pi.getPortProto());
	}
	public static boolean isPwrGnd(PortProto pp) {
		return isPwr(pp) || isGnd(pp);
	}
	public static boolean isPwrGnd(PortInst pi) {
		return isPwr(pi) || isGnd(pi);
	}
	public static boolean isPwrGnd(ToConnect tc) {
		for (PortInst pi : tc.getPortInsts()) {
			if (isPwrGnd(pi)) return true;
		}
		return false;
	}
	public static boolean onTop(PortInst pi, Rectangle2D bounds, double fudge) {
		double y = pi.getCenter().getY();
		return Utils.nextToBoundary(y, bounds.getMaxY(), fudge);
	}
	public static boolean onBottom(PortInst pi, Rectangle2D bounds, double fudge) {
		double y = pi.getCenter().getY();
		return Utils.nextToBoundary(y, bounds.getMinY(), fudge);
	}
	public static boolean onTopOrBottom(PortInst pi, Rectangle2D bounds, double fudge) {
		return onTop(pi, bounds, fudge) || onBottom(pi, bounds, fudge);
	}
	public static boolean onLeftOrRight(PortInst pi, Rectangle2D bounds, double fudge) {
		double x = pi.getCenter().getX();
		return Utils.nextToBoundary(x, bounds.getMinX(), fudge) ||
	           Utils.nextToBoundary(x, bounds.getMaxX(), fudge);
	}
	public static boolean onBounds(PortInst pi, Rectangle2D bounds, double fudge) {
		return onTopOrBottom(pi, bounds, fudge) ||
		       onLeftOrRight(pi, bounds, fudge);
	}
	public static boolean nextToBoundary(double coord, double boundCoord, double fudge) {
		return Math.abs(coord-boundCoord) <= fudge;
	}
	public static Rectangle2D findBounds(Cell c) {
		double minX, minY, maxX, maxY;
		minX = minY = Double.MAX_VALUE;
		maxX = maxY = Double.MIN_VALUE;
		for (Iterator<NodeInst> niIt=c.getNodes(); niIt.hasNext();) {
			NodeInst ni = niIt.next();
			if (!ni.isCellInstance()) continue;

			Rectangle2D bounds = ni.findEssentialBounds();
			error(bounds==null, "Layout Cell is missing essential bounds: "+
				  ni.getProto().describe(false));
			minX = Math.min(minX, bounds.getMinX());
			maxX = Math.max(maxX, bounds.getMaxX());
			minY = Math.min(minY, bounds.getMinY());
			maxY = Math.max(maxY, bounds.getMaxY());
		}
		if (minX==Double.MAX_VALUE) {
			// empty Cell. Return a zero sized rectangle around origin
			return new Rectangle2D.Double(0,0,0,0);
		} else {
			return new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
		}
	}
	public static void printStackTrace(Throwable th) {
		boolean first = true;
		for (; th!=null; th=th.getCause()) {
			if (!first) pr("Caused by: ");
			first = false;
			prln(th.toString());
			for (StackTraceElement elem : th.getStackTrace()) {
				prln("    "+elem.toString());
			}
		}
	}


}
