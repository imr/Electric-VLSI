class Strictfp {

    private static /*strictfp*/ boolean cycle() {
	double dValue = Double.MIN_VALUE;
	for (int i = 0; i < 10; i++) {
	    boolean overvalue = (i&0x10) == 0;
	    dValue += 0.5*(overvalue ? -Double.MIN_VALUE : Double.MIN_VALUE);
	    System.out.println("i=" + i + " dValue="+dValue);
	    if (dValue == 0.0) return true;
	}
	return false;
    }

    public static void main(String[] args) {
	for (int i = 0; i < 1000; i++) {
	    if (cycle()) break;
	}
    }
}

