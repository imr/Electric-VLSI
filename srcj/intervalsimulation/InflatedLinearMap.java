import com.sun.electric.tool.simulation.interval.Interval;

class InflatedLinearMap {

    private static final double ULP = 0x1.0p-52;
    private static final double MIN_NORMAL = Double.MIN_VALUE*0x1.0p52;

    private int n;
    private double[] center;
    private double[] radius;
    private double[] incr;
    private double[][] shrink;
    private double[] err;

    InflatedLinearMap(int n) {
	this.n = n;
	center = new double[n];
	radius = new double[n];
	incr = new double[n];
	shrink = new double[n][n];
	err = new double[n];
    }

    double getCenter(int i) {
	return center[i];
    }

    void setCenter(int i, double v) {
	center[i] = v;
    }

    double getRadius(int i) {
	return radius[i];
    }

    void setRadius(int i, double v) {
	radius[i] = v;
    }

    double getIncr(int i) {
	return incr[i];
    }

    void setIncr(int i, double v) {
	incr[i] = v;
    }

    double getShrink(int i, int j) {
	return shrink[i][j];
    }

    void setShrink(int i, int j, double v) {
	shrink[i][j] = v;
    }

    double getErr(int i) {
	return err[i];
    }

    void setErr(int i, double v) {
	err[i] = v;
    }

    private static double addErr(double z, double x, double y) {
	return ((x >= 0 ? x : -x) >= (y >= 0 ? y : -y) ? (x - z) + y : (y - z) + x);
    }

    static abstract class LocalSolver {
	private int n;

	LocalSolver(int n) {
	    this.n = n;
	}

	int size() {
	    return n;
	}

	abstract void solve(InflatedLinearMap map);
    }

    static abstract class GlobalSolver {

	int n;
	LocalSolver lSolver;
	InflatedLinearMap map;

	GlobalSolver(LocalSolver lSolver) {
	    n = lSolver.size();
	    this.lSolver = lSolver;
	    map = new InflatedLinearMap(n);
	}

	abstract void step();
    }

    static class SimpleGlobalSolver extends GlobalSolver {

	private double[] center;
	private double[] radius;
	private double radScaleUp;
	private Interval itmp = new Interval();

	SimpleGlobalSolver (LocalSolver lSolver) {
	    super(lSolver);
	    center = new double[n];
	    radius = new double[n];
	    radScaleUp = 1.0 + (n+1)/2*ULP; // Compensate round error of n additions.
	}

	void setState(double[] center) {
	    if (center.length != n)
		throw new IllegalArgumentException(center.length + " != " + n);
	    for (int i = 0; i < n; i++) {
		this.center[i] = center[i];
		this.radius[i] = 0;
	    }
	}

	void printState() {
	    for (int i = 0; i < n; i++) {
		System.out.println(i + " " + center[i] + " " + Double.toHexString(center[i]) +
		    " +- " + radius[i] + " " + Double.toHexString(radius[i]) + " " + (center[i] + radius[i]));
	    }
	}

	void step() {
	    for (int i = 0; i < n; i++) {
		map.setCenter(i, center[i]);
		map.setRadius(i, radius[i]);
	    }
	    lSolver.solve(map);
	    for (int i = 0; i < n; i++) {
		double centI = center[i];
		double incrI = map.getIncr(i);
		double s = centI + incrI;
		center[i] = s;
		double rad = Math.abs(addErr(s, centI, incrI)) + map.getErr(i);
		for (int j = 0; j < n; j++) {
		    if (j == i) continue;
		    double sh = map.getShrink(i,j);
		    rad = (sh >= 0 ? rad + sh : rad - sh);
		}
		rad *= radScaleUp;
		// radius[i] = rad + abs(shrink[i][i] + radius[i])
		double diagSh = map.getShrink(i,i);
		radius[i] = (diagSh >= -radius[i]
		    ? Interval.addPosUp(radius[i], Interval.addUp(diagSh, rad))
		    : Interval.addPosUp(rad, Interval.addUp(-diagSh, -radius[i])));
	    }
	}
    }

    static class AccurateGlobalSolver extends GlobalSolver {

	private Interval center = new Interval();
	private Interval radius = new Interval();
	private Interval err = new Interval();
	private Interval shrink = new Interval();
	private Interval incr = new Interval();
	private Interval t = new Interval();

	AccurateGlobalSolver (LocalSolver lSolver) {
	    super(lSolver);
	}

	void setState(double center) {
	    this.center.assign(center);
	    this.radius.assign(0);
	}

	void printState() {
	    System.out.println(center.inf() + " " + Double.toHexString(center.inf()) +
		" +[" + radius.inf() + "," + radius.sup() + "] " + 
		radius.wid()/2 + " " + (center.sup() - radius.sup()));
	}

	void step() {
// 	    assert center.isPoint();
// 	    map.setCenter(0, center.inf());
// 	    map.setRadius(0, err.mag());
// 	    lSolver.solve(map);
// 	    incr.assign(map.getIncr(0));
// 	    shrink.assign(map.getShrink(0,0));
// 	    err.assign(-map.getErr(0),map.getErr(0));

// 	    double newCenter = center.inf() + (map.getIncr(0) + radius.mid());
// 	    t.assign(-newCenter).add(center).add(incr);
// 	    if (!t.isPoint())
// 		System.out.println("t is not point");
// 	    radius.add(shrink.div(map.getRadius(0)).mul(radius.mid()).add(err).add(t);
// 	    center.assign(newCenter);
	}
    }

    static class LocalSolverExp extends LocalSolver {

	double h;
	double incrFactor;
	double errFactor;

	LocalSolverExp(int k, double h) {
	    super(1);
	    if (h <= 0 || h > 1)
		throw new IllegalArgumentException("h=" + h);
	    this.h = 1;
	    while (this.h > h)
		this.h /= 2;
	    if (this.h != h)
		throw new IllegalArgumentException("h must be power of 2");
	    if (k == 0) {
		incrFactor = 0;
		errFactor = h;
	    } else if (k == 1) {
		incrFactor = -h;
		errFactor = 0.5*h*h*(1 + ULP);
	    } else {
		double s = 1;
		double fact = 1 + k*ULP;
		for (int i = k; i >= 2; i--) {
		    s = 1 - s*h/i;
		    fact = fact*h/(i+1);
		}
		incrFactor = -h*s;
		errFactor = h*(0.5*h*fact + ULP);
	    }
	}

	void solve(InflatedLinearMap map) {
	    double center = map.getCenter(0);
	    double radius = map.getRadius(0);
	    map.setIncr(0, center*incrFactor);
	    map.setShrink(0, 0, radius*incrFactor);
	    double err = (Math.abs(center) + radius) * errFactor + MIN_NORMAL;
	    map.setErr(0, err);
	    //System.out.println("err=" + err + " " + Double.toHexString(err) +
	    //		" shrink=" + radius*incrFactor + " " + Double.toHexString(radius*incrFactor));
	}
    }

    public static void main (String argv[])
    {
	int k0 = 0;
	int order = 3;
	double x = 1.0/(1L << k0);
	double[] initialState = {1.0};
	System.out.println("exp(" + (-x) + ")=" + Math.exp(-x) + " " + Double.toHexString(Math.exp(-x)));
	for (int k = 0; k < 28; k++) {
	    double h = x / (1 << k);
	    LocalSolverExp lSolver = new LocalSolverExp(order, h);
	    System.out.println("k=" + (k+k0) + " h=" + h);

	    SimpleGlobalSolver sSolver = new SimpleGlobalSolver(lSolver);
	    sSolver.setState(initialState);
	    for (int i = 0; i < (1 << k); i++) {
		//sSolver.printState();
		sSolver.step();
	    }
	    sSolver.printState();
// 	    AccurateGlobalSolver aSolver = new AccurateGlobalSolver(lSolver);
// 	    aSolver.setState(1.0);
// 	    for (int i = 0; i < (1 << k); i++)
// 		aSolver.step();
// 	    aSolver.printState();
	}
    }

}
