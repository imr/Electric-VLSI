import com.sun.electric.tool.simulation.interval.MutableInterval;
import net.sourceforge.interval.ia_math.RMath;
import net.sourceforge.interval.ia_math.RealInterval;
import net.sourceforge.interval.ia_math.IAMath;
import su.nsk.nbsp.Functions;
import java.math.BigDecimal;

class TestIntervals {

    private MutableInterval mix, miy, miz;
    private su.nsk.nbsp.Interval nix, niy, niz;
    private RealInterval tix, tiy, tiz;
    private boolean mb, nb;
    private double md, nd;

    TestIntervals() {
	mix = new MutableInterval();
	miy = new MutableInterval();
	miz = new MutableInterval();
	nix = new su.nsk.nbsp.Interval();
	niy = new su.nsk.nbsp.Interval();
	niz = new su.nsk.nbsp.Interval();
    }

    private static final double MIN_NORMAL = 0x1.0p-1022;

    private static double[] relnums = {
	Double.NEGATIVE_INFINITY,
	-Long.MAX_VALUE,
	-Math.PI,
	-Math.E,
	-0x1.0000000000001p0,
	-1,
	-0x0.fffffffffffffp0,
	-1.0/5,
	-MIN_NORMAL*3+4*Double.MIN_VALUE,
	-MIN_NORMAL-Double.MIN_VALUE,
	-MIN_NORMAL,
	-MIN_NORMAL+Double.MIN_VALUE,
	-Double.MIN_VALUE*2,
	-Double.MIN_VALUE,
	-0.0,
	0.0,
	Double.MIN_VALUE,
	Double.MIN_VALUE*2,
	MIN_NORMAL-Double.MIN_VALUE,
	MIN_NORMAL,
	MIN_NORMAL+Double.MIN_VALUE,
	MIN_NORMAL*3-4*Double.MIN_VALUE,
	1.0/5,
	0x0.fffffffffffffp0,
	1,
	0x1.0000000000001p0,
	Math.E,
	Math.PI,
	Long.MAX_VALUE,
	Double.POSITIVE_INFINITY
    };

    private static MutableInterval[] mrel;
    private static su.nsk.nbsp.Interval[] nrel;

    static {
	int l = relnums.length;
	int n = 1 + l*(l+1)/2;
	mrel = new MutableInterval[n];
	nrel = new su.nsk.nbsp.Interval[n];
	mrel[0] = new MutableInterval().assignEmpty();
	nrel[0] = (new su.nsk.nbsp.Interval(-1)).log();
	int k = 1;
	for (int i = 0; i < relnums.length; i++) {
	    for (int j = i; j < relnums.length; j++) {
		double inf = relnums[i];
		double sup = relnums[j];
		mrel[k] = new MutableInterval(inf, sup);
		nrel[k] = new su.nsk.nbsp.Interval(inf, sup);
		k++;
	    }
	}
    }

    private void unop(int i) {
	switch (i) {
	    case 0: mb = mix.isPoint(); nb = nix.isDegenerate(); break;
	    case 1: mb = mix.isEmpty(); nb = nix.isEmpty(); break;
	    case 2: mb = mix.isEntire(); nb = nix.isEntire(); break;

	    case 3: md = mix.inf(); nd = nix.inf(); break;
	    case 4: md = mix.sup(); nd = nix.sup(); break;
	    case 5: md = mix.mid(); nd = nix.mid(); break;
	    case 6: md = mix.wid(); nd = nix.wid(); break;
	    case 7: md = mix.rad();
		nd = mix.isEmpty() ? Double.NaN : mix.isInfinite() ? Double.POSITIVE_INFINITY : miz.assign(-mix.mid()).add(mix).mag();
		break;
	    case 8: md = mix.mig(); nd = nix.mig(); break;
	    case 9: md = mix.mag(); nd = nix.mag(); break;

	    case 10: miz.assign(mix).abs(); niz.assign(nix).abs(); break;
	    case 11: miz.assign(mix).negate(); niz.assign(nix).negate(); break;
	    case 12: miz.assign(mix).exp(); niz.assign(nix).exp(); break;
	    case 13: miz.assign(mix).log(); niz.assign(nix).log(); break;
	}
    }

    private void checkUnops() {
	for (int k = 0; k <= 12; k++) {
	    for (int i = 0; i < mrel.length; i++) {
		mix.assign(mrel[i]);
		nix.assign(nrel[i]);
		unop(k);
		if (k <= 2) {
		    if (mb != nb)
			System.out.println("checkUnops k=" + k);
		} else if (k <= 9) {
		    if (md != nd && (md == md || nd == nd)) {
			if (k == 5 && (md == Functions.prev(nd) || md == Functions.next(nd))) continue;
			System.out.println("checkUnops k=" + k + " " + nix + " " + md + " " + nd);
			System.out.println(Double.toHexString(mix.inf()) + " " + Double.toHexString(mix.sup()) + " " + Double.toHexString(md) );
			System.out.println(Double.toHexString(miz.inf()) + " " + Double.toHexString(miz.sup()) + " " + Double.toHexString(mix.mid()) );
		    }
		} else {
		    double minf = miz.inf();
		    double msup = miz.sup();
		    double ninf = niz.inf();
		    double nsup = niz.sup();
		    if ((minf != ninf || msup != nsup) && (minf == minf || msup == msup || ninf == ninf || nsup == nsup) ) {
			if ((minf == Functions.prev(ninf) || minf == ninf) && (msup == Functions.next(nsup) || msup == nsup)) continue;
			System.out.println("checkUnops " + nix + " " + k + " m=[" + minf + "," + msup+"] n=[" + ninf + "," + nsup + "]");
		    }
		}
	    }
	}
    }

    private void binop(int i) {
	switch (i) {
	    case 0: miz.assign(mix).min(miy); niz.assign(nix).min(niy); break;
	    case 1: miz.assign(mix).max(miy); niz.assign(nix).max(niy); break;
	    case 2: miz.assign(mix).intersect(miy); niz.assign(nix).ix(niy); break;
	    case 3: miz.assign(mix).interval_hull(miy); niz.assign(nix).ih(niy); break;
	    case 4: miz.assign(mix).add(miy); niz.assign(nix).add(niy); break;
	    case 5: miz.assign(mix).sub(miy); niz.assign(nix).sub(niy); break;
	    case 6: miz.assign(mix).mul(miy); niz.assign(nix).mul(niy); break;
	    case 7: miz.assign(mix).div(miy); niz.assign(nix).div(niy); break;

	    case 8: mb = mix.disjoint(miy); nb = nix.dj(niy); break;
	    case 9: mb = mix.in_interior(miy); nb = nix.interior(niy); break;
	    case 10: mb = mix.seq(miy); nb = nix.seq(niy); break;
	    case 11: mb = mix.sne(miy); nb = nix.sne(niy); break;
	    case 12: mb = mix.sge(miy); nb = nix.sge(niy); break;
	    case 13: mb = mix.sgt(miy); nb = nix.sgt(niy); break;
	    case 14: mb = mix.sle(miy); nb = nix.sle(niy); break;
	    case 15: mb = mix.slt(miy); nb = nix.slt(niy); break;
	    case 16: mb = mix.ceq(miy); nb = nix.ceq(niy); break;
	    case 17: mb = mix.cne(miy); nb = nix.cne(niy); break;
	    case 18: mb = mix.cge(miy); nb = nix.cge(niy); break;
	    case 19: mb = mix.cgt(miy); nb = nix.cgt(niy); break;
	    case 20: mb = mix.cle(miy); nb = nix.cle(niy); break;
	    case 21: mb = mix.clt(miy); nb = nix.clt(niy); break;
	    case 22: mb = mix.peq(miy); nb = nix.peq(niy); break;
	    case 23: mb = mix.pne(miy); nb = nix.pne(niy); break;
	    case 24: mb = mix.pge(miy); nb = nix.pge(niy); break;
	    case 25: mb = mix.pgt(miy); nb = nix.pgt(niy); break;
	    case 26: mb = mix.ple(miy); nb = nix.ple(niy); break;
	    case 27: mb = mix.plt(miy); nb = nix.plt(niy); break;
// 	    case 28: mb = mix.proper_subset(miy); nb = nix.proper_subset(niy); break;
// 	    case 29: mb = mix.subset(miy); nb = nix.subset(niy); break;
// 	    case 30: mb = mix.proper_superset(miy); nb = nix.proper_superset(niy); break;
// 	    case 31: mb = mix.superset(miy); nb = nix.superset(niy); break;
//	    case 32: md = mix.dist(miy); nd = nix.dist(niy); break;
	}
    }
 
    private void checkBinops() {
	for (int k = 0; k <= 27; k++) {
	    for (int i = 0; i < mrel.length; i++) {
		for (int j = 0; j < mrel.length; j++) {
		    mix.assign(mrel[i]);
		    nix.assign(nrel[i]);
		    miy.assign(mrel[j]);
		    niy.assign(nrel[j]);
		    binop(k);
		    if (k <= 7) {
			double minf = miz.inf();
			double msup = miz.sup();
			double ninf = niz.inf();
			double nsup = niz.sup();
			if ((minf != ninf || msup != nsup) && (minf == minf || msup == msup || ninf == ninf || nsup == nsup) ) {
			    if ((k == 0 || k == 1) && (mrel[i].isEmpty() || mrel[j].isEmpty()) && minf != minf && msup != msup) continue;
			    if ((k == 6 || k == 7) && (minf == Functions.prev(ninf) || minf == ninf) && (msup == Functions.next(nsup) || msup == nsup)) continue;
			    System.out.println("checkBinops " + nix + " " + k + " " + niy + " m=[" + minf + "," + msup+"] n=[" + ninf + "," + nsup + "]");
			}
		    } else if (mb != nb) {
			System.out.println("checkBinops k=" + k);
		    }
		}
	    }
	}
    }

    private void checkOutput() {
	for (int i = 0; i < mrel.length; i++) {
	    mix.assign(mrel[i]);
	    nix.assign(nrel[i]);
	    String ms = mix.toString();
	    String ns = nix.toString();
	    miz = new MutableInterval(ms);
	    miy = new MutableInterval(ns);
	    double minf = miz.inf();
	    double msup = miz.sup();
	    double ninf = miy.inf();
	    double nsup = miy.sup();
	    if ((!(minf <= mix.inf()) || !(msup >= mix.sup())) && (minf == minf || msup == msup || !mix.isEmpty()) ) {
		System.out.println("checkOutput " + i + " m=[" + minf + "," + msup+"] o=[" + mix.inf() + "," + mix.sup() + "]");
	    }
	    if ((!(ninf <= mix.inf()) || !(nsup >= mix.sup())) && (ninf == ninf || nsup == nsup || !mix.isEmpty()) ) {
		System.out.println("checkOutput " + i + " n=[" + ninf + "," + nsup+"] o=[" + mix.inf() + "," + mix.sup() + "]");
	    }
// 	    if ((!(ninf <= minf) || !(nsup >= msup)) && (minf == minf || msup == msup || ninf == ninf || nsup == nsup) ) {
// 		System.out.println("checkOutput " + i + " m=[" + minf + "," + msup+"] n=[" + ninf + "," + nsup + "] o=[" + mix.inf() + "," + mix.sup() + "]");
// 	    }
	}
    }

    private static void checkTab() {
	for (int i = 0; i < mrel.length; i++) {
	    double minf = mrel[i].inf();
	    double msup = mrel[i].sup();
	    double ninf = nrel[i].inf();
	    double nsup = nrel[i].sup();
	    if ((minf != ninf || msup != nsup) && (minf == minf || msup == msup || ninf == ninf || nsup == nsup) ) {
		System.out.println("checkTab " + i + " m=[" + minf + "," + msup+"] n=[" + ninf + "," + nsup + "]");
	    }
	}
    }



    public static void main (String argv[])
    {
	checkTab();
	TestIntervals t = new TestIntervals();
	t.checkOutput();
	t.checkUnops();
	t.checkBinops();
    }
}
