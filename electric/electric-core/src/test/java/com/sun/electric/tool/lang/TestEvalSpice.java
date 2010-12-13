package com.sun.electric.tool.lang;

import junit.framework.Assert;

import org.junit.Test;

public class TestEvalSpice {
	
	@Test
	public void testSpiceEvaluation() {
		eval("1 + 2", 3);
        eval("1 + 2 * 3", 7);
        eval("1 * 2 + 3", 5);
        eval("(1 + 2) * 3", 9);
        eval("(1 + 2) * x", "3 * x");
        eval("300 / -1.5e2", -2);
        eval("1.5e-2", 0.015);
        eval("20 * 1.5e-2", 0.3);
        eval("20 * 1.5m", 0.03);
        eval("(1 + a) * 3 + b", "(1 + a) * 3 + b");
        eval("1 + 2 * 3 + - 4", 3);
        eval("-1", -1);
        eval("-1 + 2 * 3 + - 4", 1);
        eval("-(1 + 2) * 3 + -4", -13);
        eval("-(1 + 2) * 3 + -4 * -2 - -4 * -3", -13);
        eval("-sin(3)", -Math.sin(3));
        eval("-sin(x)", "-sin(x)");
        eval("1-min(1,-2)", 3);
        eval("1-min(1,x)", null);
        eval("1-min((a+b)*c,x)", null);
        eval("1-min((a+b)*c,(a+b))", null);
        eval("-a + 2 * 3 * -b + - 4", null);
        eval("1 ? -2 : 4", -2);
        eval("0 ? -2 : 4", 4);
        eval("8 == 1 ? -2 : 4", 4);
        eval("8 > 1 ? -2 : 4", -2);
        eval("1 - 7 <= 1 ? -2 : 4", -2);
        eval("layer == 1 ? two + 1 : eight * 4 / 2", "layer == 1 ? two + 1 : eight * 4 / 2");
        eval("0 * 1 ? 3 / 2 : -4 + 10", 6);
        eval("(3==0?0.00441:3<8?0.011:0.016)*1e-15", 1.1e-17);
        eval("(layer==0?0.00441:layer<8?0.011:0.016)*1e-15", null);
        System.out.println("\nThese should flag as errors:\n---------------------------\n");
        eval("1 2 +", null);
        eval("1 + * 2", null);
        eval("1 + 2 * - -3", null);
        eval("300 / -1.5ee2 + 5", null);
        eval("1-min((a+b)*c,(a+b)", null);
        eval("1/0", null);
        eval("M1 - M3 : 10001", null);
	}
	
	private void eval(String eq, String expected) {
        EvalSpice sp = new EvalSpice(eq);
        String evald = sp.evaluate().toString();
        if (expected == null) {
            System.out.println(eq + " = " + evald);
        } else {
            System.out.println(eq + " = " + evald + " -- (" + expected + ")");
            Assert.assertEquals(expected, evald);
        }
    }

    private void eval(String eq, double expected) {
        EvalSpice sp = new EvalSpice(eq);
        Object evald = sp.evaluate();
        System.out.println(eq + " = " + evald + " -- (" + expected + ")");
        Assert.assertTrue(evald instanceof Double);
        double val = ((Double) evald).doubleValue();
        Assert.assertEquals(expected, val, 0.00001);
    }

}
