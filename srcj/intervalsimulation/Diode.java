import com.sun.electric.tool.simulation.interval.RawFile;
import net.sourceforge.interval.ia_math.*;
import Jama.*;
//import Jama.util.Maths;
import java.io.*;
//import java.util.*;
//import java.text.DecimalFormat;

/**
 * This class contains a set of expressioans which defines model equations
 * of diode.
 */

class Diode
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
    //RealInterval rs;
    //RealInterval n;
    //RealInterval tt;
    //RealInterval cjo;
    //RealInterval wj;
    //RealInterval m;
    //RealInterval fc;
    //RealInterval temp;

    Diode() {
	is.setName("is");
	gmin.setName("gmin");
	vt.setName("vt");
	vd.setName("vd");
	// evd = exp(vd/vt)
	ExprEval.DoubleExpr evd = ev.newExp(ev.newSlash(vd,vt)); evd.setName("evd");
	// cdp = is*(evd-1)
	ExprEval.DoubleExpr cdp = ev.newTimes(is, ev.newMinus(evd, ev.newConst(1.0))); cdp.setName("cdp");
	// gd[ = (is/vt)*evd
	ExprEval.DoubleExpr gdp = ev.newTimes(ev.newSlash(is,vt),evd); gdp.setName("gdp");
	// arg = vt*(3/e)/vd
	ExprEval.DoubleExpr arg = ev.newSlash(ev.newTimes(vt,ev.newConst(3.0/Math.E)),vd); arg.setName("arg");
	// arg3 = arg*arg*arg
	ExprEval.DoubleExpr arg3 = ev.newTimes(ev.newTimes(arg,arg),arg); arg3.setName("arg3");
	// cdm = -is*(1 + arg3)
	ExprEval.DoubleExpr cdm = ev.newTimes(ev.newNegate(is),ev.newPlus(ev.newConst(1.0),arg3)); cdm.setName("cdm");
	// gdm = is*3*arg3/vd
	ExprEval.DoubleExpr gdm = ev.newTimes(ev.newTimes(is,ev.newConst(3)),ev.newSlash(arg3,vd)); gdm.setName("gdm");
	// direct = (vd >= -3*vt)
	ExprEval.BooleanExpr direct = ev.newGe(vd,ev.newTimes(ev.newConst(-3),vt)); direct.setName("direct");
	// cd0 = (direct ? cdp : cdm)
	ExprEval.DoubleExpr cd0 = ev.newIte(direct,cdp,cdm); cd0.setName("cd0");
	// gd0 = (direct ? gdp : gdm)
	ExprEval.DoubleExpr gd0 = ev.newIte(direct,gdp,gdm); gd0.setName("gd0");
	// cd = cd0 + gmin*vd
	cd = ev.newPlus(cd0,ev.newTimes(gmin,vd)); cd.setName("cd");
	// gd = gd0 +gmin
	gd = ev.newPlus(gd0,gmin); gd.setName("gd");
    }

    /**
     * Test program plots in raw file diode current versus voltage:
     * at interval [v-delta,v+delta],
     * and at 3 point of this interval - ceneter and borders
     */
    
    public static void main (String argv[])
       throws FileNotFoundException
    {
	Diode d = new Diode();
	d.ev.printAll(false);

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
	    raw.set(k, 7, d.cd.iv().lo());
	    raw.set(k, 8, d.gd.iv().lo());
	    raw.set(k, 9, d.cd.iv().hi());
	    raw.set(k, 10, d.gd.iv().hi());
	    
	}
	raw.write("diode.raw");
    }
}
