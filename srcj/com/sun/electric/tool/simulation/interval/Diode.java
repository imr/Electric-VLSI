/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Diode.java
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
package com.sun.electric.tool.simulation.interval;

import com.sun.electric.tool.simulation.interval.ExprEval;
import com.sun.electric.tool.simulation.interval.MutableInterval;
import com.sun.electric.tool.simulation.interval.RawFile;

import java.nio.ByteOrder;

/**
 * This class contains a set of expressions which defines model equations
 * of diode.
 */

public class Diode
{
    final static double CHARGE = 1.6021918e-19;
    final static double CONSTboltz  = 1.3806226e-23;
    final static double REFTEMP = 300.15;

    ExprEval ev = new ExprEval();
    ExprEval.DoubleConstExpr is = ev.newConst(1e-14);
    ExprEval.DoubleConstExpr gmin = ev.newConst(0.0);
    //ExprEval.DoubleConstExpr gmin = ev.newConst(1e-12);
    ExprEval.DoubleConstExpr vt = ev.newConst(CONSTboltz * REFTEMP / CHARGE);
    ExprEval.DoubleConstExpr vd = ev.newConst();
    ExprEval.DoubleExpr cd;
    ExprEval.DoubleExpr gd;
    //ExprEval.DoubleConstExpr rs;
    //ExprEval.DoubleConstExpr n;
    //ExprEval.DoubleConstExpr tt;
    //ExprEval.DoubleConstExpr cjo;
    //ExprEval.DoubleConstExpr wj;
    //ExprEval.DoubleConstExpr m;
    //ExprEval.DoubleConstExpr fc;
    //ExprEval.DoubleConstExpr temp;

    Diode() {
		is.setName("is");
		gmin.setName("gmin");
		vt.setName("vt");
		vd.setName("vd");
		// evd = exp(vd/vt)
		ExprEval.DoubleExpr evd = vd.divide(vt).exp(); evd.setName("evd");
		// cdp = is*(evd-1)
		ExprEval.DoubleExpr cdp = is.multiply(evd.subtract(ev.newConst(1.0))); cdp.setName("cdp");
		// gd[ = (is/vt)*evd
		ExprEval.DoubleExpr gdp = is.divide(vt).multiply(evd); gdp.setName("gdp");
		// arg = vt*(3/e)/vd
		ExprEval.DoubleExpr arg = vt.multiply(ev.newConst(3.0/Math.E)).divide(vd); arg.setName("arg");
		// arg3 = arg*arg*arg
		ExprEval.DoubleExpr arg3 = arg.multiply(arg).multiply(arg); arg3.setName("arg3");
		// cdm = -is*(1 + arg3)
		ExprEval.DoubleExpr cdm = is.negate().multiply(ev.newConst(1.0).add(arg3)); cdm.setName("cdm");
		// gdm = is*3*arg3/vd
		ExprEval.DoubleExpr gdm = is.multiply(ev.newConst(3)).multiply(arg3).divide(vd); gdm.setName("gdm");
		// direct = (vd >= -3*vt)
		ExprEval.BooleanExpr direct = vd.ge(ev.newConst(-3).multiply(vt)); direct.setName("direct");
		// cd0 = (direct ? cdp : cdm)
		ExprEval.DoubleExpr cd0 = direct.ite(cdp,cdm); cd0.setName("cd0");
		// gd0 = (direct ? gdp : gdm)
		ExprEval.DoubleExpr gd0 = direct.ite(gdp,gdm); gd0.setName("gd0");
		// cd = cd0 + gmin*vd
		cd = cd0.add(gmin.multiply(vd)); cd.setName("cd");
		// gd = gd0 +gmin
		gd = gd0.add(gmin); gd.setName("gd");
    }

    /**
     * Test program plots in raw file diode current versus voltage:
     * at interval [v-delta,v+delta],
     * and at 3 point of this interval - ceneter and borders
     */
    public static void plotDiode(String filePath) {
		System.out.println("Plotting Diode graph to file " + filePath);
		Diode d = new Diode();
		//d.ev.printAll(false);

		double delta = 0.001;
		int numPoints = 10000;
		int numVars = 11;
		RawFile raw = new RawFile(numPoints + 1, numVars);
		raw.setVar(0, "vd", "voltage");
		raw.setVar(1, "j",  "current");
		raw.setVar(2, "g",  "conductance");
		raw.setVar(3, "jm", "current");
		raw.setVar(4, "gm", "conductance");
		raw.setVar(5, "jp", "current");
		raw.setVar(6, "gp", "conductance");
		raw.setVar(7, "jl", "current");
		raw.setVar(8, "gl", "conductance");
		raw.setVar(9, "jh", "current");
		raw.setVar(10,"gh", "conductance");

		for (int k = 0; k <= numPoints; k++)
		{
			double vd = -0.2 + 0.3 / numPoints * k;
			raw.set(k, 0, vd);

			d.vd.setV(vd);
			d.ev.calcAll();
			raw.set(k, 1, d.cd.v());
			raw.set(k, 2, d.gd.v());
	    
			d.vd.setV(vd-delta);
			d.ev.calcAll();
			raw.set(k, 3, d.cd.v());
			raw.set(k, 4, d.gd.v());
	    
			d.vd.setV(vd+delta);
			d.ev.calcAll();
			raw.set(k, 5, d.cd.v());
			raw.set(k, 6, d.gd.v());

			d.vd.setInterval(vd-delta,vd+delta);
			d.ev.calcIntervalAll();
			raw.set(k, 7, d.cd.inf());
			raw.set(k, 8, d.gd.inf());
			raw.set(k, 9, d.cd.sup());
			raw.set(k, 10, d.gd.sup());
	    
		}
		raw.write(filePath);
		//raw.writeBinary(filePath, null);
    }

    
    public static void main (String argv[])
    {
		plotDiode("diode.raw");
    }
}
