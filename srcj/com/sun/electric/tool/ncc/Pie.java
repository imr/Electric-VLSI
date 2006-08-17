package com.sun.electric.tool.ncc;

import java.lang.reflect.Constructor;

import com.sun.electric.tool.generator.layout.LayoutLib;

/** Reflective interface to Port Interchange Experiment. This allows us to compile Electric
 * without the plugin: com.sun.electric.plugins.pie */
public class Pie {
	private Class pieNccJobClass;
	private Constructor pieNccJobConstructor;

	// singleton
	private static final Pie pie = new Pie();
	
	private Pie() {
		try {
			pieNccJobClass = Class.forName("com.sun.electric.plugins.pie.NccJob");
			pieNccJobConstructor = pieNccJobClass.getConstructor(new Class[] {Integer.TYPE});
		} catch (Throwable e) {
			pieNccJobClass = null;
			pieNccJobConstructor = null;
		}
	}
	
	private static void prln(String msg) {System.out.println(msg);}
	
	public static boolean hasPie() {
		return pie.pieNccJobClass!=null;
	}
	public static void invokePieNcc(int numWind) {
		LayoutLib.error(!hasPie(), "trying to invoke non-existant PIE");
		try {
			pie.pieNccJobConstructor.newInstance(new Integer(numWind));
		} catch (Throwable e) {
			prln("Invocation of pie NccJob threw Throwable: "+e);
			e.printStackTrace();
		}
	}
}
