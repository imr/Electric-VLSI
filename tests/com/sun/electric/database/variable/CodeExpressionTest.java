/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CodeExpressionTest.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.database.variable;

import com.sun.electric.database.variable.Variable.Key;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test of CodeExpression
 */
public class CodeExpressionTest {

    public CodeExpressionTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        CodeExpression.printStatistics(true);
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getCFlags method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Code.
     */
    @Test
    public void testCodeGetCFlags() {
        System.out.println("getCFlags");

        CodeExpression.Code instance = CodeExpression.Code.NONE;

        int expResult = 0;
        int result = instance.getCFlags();
        assertEquals(expResult, result);
    }

    /**
     * Test of toString method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Code.
     */
    @Test
    public void testCodeToString() {
        System.out.println("toString");

        CodeExpression.Code instance = CodeExpression.Code.NONE;

        String expResult = "Not Code";
        String result = instance.toString();
        assertEquals(expResult, result);
    }

    /**
     * Test of getCodes method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Code.
     */
    @Test
    public void testCodeGetCodes() {
        System.out.println("getCodes");

        List<CodeExpression.Code> expResult = Arrays.asList(
                CodeExpression.Code.JAVA,
                CodeExpression.Code.SPICE,
                CodeExpression.Code.TCL,
                CodeExpression.Code.NONE);
        Iterator<CodeExpression.Code> result = CodeExpression.Code.getCodes();
        int i = 0;
        while (result.hasNext()) {
            assertEquals(expResult.get(i++), result.next());
        }
        assertEquals(expResult.size(), i);
    }

    /**
     * Test of getByCBits method, of class com.sun.electric.database.variable.AbstractTextDescriptor.Code.
     */
    @Test
    public void testCodeGetByCBits() {
        System.out.println("getByCBits");

        int cBits = 0;

        CodeExpression.Code expResult = CodeExpression.Code.NONE;
        CodeExpression.Code result = CodeExpression.Code.getByCBits(cBits);
        assertEquals(expResult, result);
    }

    /**
     * Test of valueOf method, of class CodeExpression.
     */
    @Test
    public void valueOf() {
        System.out.println("valueOf");

        CodeExpression ja = CodeExpression.valueOf("a", CodeExpression.Code.JAVA);
        assertSame(CodeExpression.Code.JAVA, ja.getCode());
        assertTrue(ja.isJava());
        assertEquals("a", ja.getExpr());

        CodeExpression sa = CodeExpression.valueOf("a", CodeExpression.Code.SPICE);
        assertSame(CodeExpression.Code.SPICE, sa.getCode());
        assertFalse(sa.isJava());
        assertEquals("a", sa.getExpr());

        CodeExpression ta = CodeExpression.valueOf("a", CodeExpression.Code.TCL);
        assertSame(CodeExpression.Code.TCL, ta.getCode());
        assertFalse(sa.isJava());
        assertEquals("a", sa.getExpr());

        CodeExpression ja1 = CodeExpression.valueOf("a", CodeExpression.Code.JAVA);
        assertSame(ja, ja1);
        CodeExpression sa1 = CodeExpression.valueOf("a", CodeExpression.Code.SPICE);
        assertSame(sa, sa1);
        CodeExpression ta1 = CodeExpression.valueOf("a", CodeExpression.Code.TCL);
        assertSame(ta, ta1);
    }

    /**
     * Test of valueOf method, of class CodeExpression.
     */
    @Test(expected = NullPointerException.class)
    public void valueOfNullCode() {
        System.out.println("valueOfNullCode");
        CodeExpression.valueOf("a", null);
    }

    /**
     * Test of valueOf method, of class CodeExpression.
     */
    @Test(expected = IllegalArgumentException.class)
    public void valueOfBadCode() {
        System.out.println("valueOfBadCode");
        CodeExpression.valueOf("a", CodeExpression.Code.NONE);
    }

    /**
     * Test of dependsOn method, of class CodeExpression.
     */
    @Test
    public void dependsOn() {
        System.out.println("dependsOn");
        CodeExpression instance = CodeExpression.valueOf("@a", CodeExpression.Code.JAVA);
        Set<Key> expResult = Collections.singleton(Variable.newKey("ATTR_a"));
        Set<Key> result = instance.dependsOn();
        assertEquals(expResult, result);
    }

    /**
     * Test of toString method, of class CodeExpression.
     */
    @Test
    public void testToString() {
        System.out.println("toString");
        assertEquals("@a", CodeExpression.valueOf("@a", CodeExpression.Code.JAVA).toString());
        assertEquals("@a", CodeExpression.valueOf("@a", CodeExpression.Code.SPICE).toString());
        assertEquals("@a", CodeExpression.valueOf("@a", CodeExpression.Code.TCL).toString());
    }

    @Test
    public void testGoodJava() {
        goodJava("(@layer<4?0.04:0.056) * @L * 1e-15", "(layer<4?40m:56m)*L*1f");
        goodJava("(@layer==0?0.015:@layer<6?0.025:0.030) * @L * 1e-15", "(layer==0?15m:layer<6?25m:30m)*L*1f");
        goodJava("(@layer==0?6.5:@layer<4?0.084:0.0504)*@L/@width", "(layer==0?6.5:layer<4?84m:50.4m)*L/width");
        goodJava("(@layer==0?7.6:@layer<6?0.086:0.036)*@L/@width", "(layer==0?7.6:layer<6?86m:36m)*L/width");
        goodJava("0.8334", "0.833");
        goodJava("1", "1");
        goodJava("10/3.00", "10/3");
        goodJava("100", "100");
        goodJava("2", "2");
        goodJava("2*P(\"S\")", "2*S");
        goodJava("2.*@X", "2*X");
        goodJava("@BF", "BF");
        goodJava("@SN==0?0:@SN<1.0?(1.0*(2-0.4)/@SN + 0.4):2", "SN==0?0:SN<1?1*(2-0.4)/SN+0.4:2");
        goodJava("LE.getdrive()", "LE.getdrive()");
        goodJava("LE.subdrive(\"invHT1\", \"X\")", "LE.subdrive(\"invHT1\",\"X\")");
//"LE.subdrive("nand21", "X")"	 ? LE.subdrive(<Two operands with no operator>"nand21", "X") -> "LE.subdrive(<Two operands with no operator>"nand21", "X")"
//"LE.subdrive("nand2_sy1", "X")"	 ? LE.subdrive(<Two operands with no operator>"nand2_sy1", "X") -> "LE.subdrive(<Two operands with no operator>"nand2_sy1", "X")"
//"LE.subdrive("nor21", "X")"	 ? LE.subdrive(<Two operands with no operator>"nor21", "X") -> "LE.subdrive(<Two operands with no operator>"nor21", "X")"
//"LE.subdrive("nor2_sy1", "S")"	 ? LE.subdrive(<Two operands with no operator>"nor2_sy1", "S") -> "LE.subdrive(<Two operands with no operator>"nor2_sy1", "S")"
        goodJava("Math.max(((Number)@X).doubleValue()/10., 5./3.)", "max(X/10,5/3)");
//"Math.max(((Number)@X).doubleValue()/10., 5./6.)"	 ATTR_X ? Math.max(<Two operands with no operator>((Number)P("ATTR_X")).doubleValue()/10., 5./6.) -> "Math.max(<Two operands with no operator>((Number)P("ATTR_X")).doubleValue()/10., 5./6.)"
//"P("L")"	 ATTR_L ? P("ATTR_L"<Illegal character ">) -> "P("ATTR_L"<Illegal character ">)"
//"P("L")/2.0"	 ATTR_L ? P("ATTR_L"<Illegal character ">)/2.0 -> "P("ATTR_L"<Illegal character ">)/2.0"
//"P("S")"	 ATTR_S ? P("ATTR_S"<Illegal character ">) -> "P("ATTR_S"<Illegal character ">)"
//"P("SN")>0.5?6*P("SN"):3"	 ATTR_SN ? P("ATTR_SN"<Illegal character ">)>0.5?6*P("ATTR_SN"):3 -> "P("ATTR_SN"<Illegal character ">)>0.5?6*P("ATTR_SN"):3"
//"P("SN")>1?3*P("SN"):3"	 ATTR_SN ? P("ATTR_SN"<Illegal character ">)>1?3*P("ATTR_SN"):3 -> "P("ATTR_SN"<Illegal character ">)>1?3*P("ATTR_SN"):3"
//"P("SP")>0.5?6*P("SP"):3"	 ATTR_SP ? P("ATTR_SP"<Illegal character ">)>0.5?6*P("ATTR_SP"):3 -> "P("ATTR_SP"<Illegal character ">)>0.5?6*P("ATTR_SP"):3"
//"P("W")"	 ATTR_W ? P("ATTR_W"<Illegal character ">) -> "P("ATTR_W"<Illegal character ">)"
//"P("W")<1?2.0/P("W"):2"	 ATTR_W ? P("ATTR_W"<Illegal character ">)<1?2.0/P("ATTR_W"):2 -> "P("ATTR_W"<Illegal character ">)<1?2.0/P("ATTR_W"):2"
//"P("W")>1?3*P("W"):3"	 ATTR_W ? P("ATTR_W"<Illegal character ">)>1?3*P("ATTR_W"):3 -> "P("ATTR_W"<Illegal character ">)>1?3*P("ATTR_W"):3"
//"Pfingers=Math.ceil(2.*@X.doubleValue()/15.); (2.*(Pfingers + 1.0) * 13.0 + @X*4.5) / 4."	 ATTR_X ? Pfingers=Math.ceil(<Expected token ==>2.*P("ATTR_X").doubleValue()/15.); (2.*(Pfingers + 1.0) * 13.0 + P("ATTR_X")*4.5) / 4. -> "Pfingers=Math.ceil(<Expected token ==>2.*P("ATTR_X").doubleValue()/15.); (2.*(Pfingers + 1.0) * 13.0 + P("ATTR_X")*4.5) / 4."
//"Pfingers=Math.ceil(2.*@X.doubleValue()/15.); (2.*(Pfingers + 1.0) * 13.0 + @X*6.)/4."	 ATTR_X ? Pfingers=Math.ceil(<Expected token ==>2.*P("ATTR_X").doubleValue()/15.); (2.*(Pfingers + 1.0) * 13.0 + P("ATTR_X")*6.)/4. -> "Pfingers=Math.ceil(<Expected token ==>2.*P("ATTR_X").doubleValue()/15.); (2.*(Pfingers + 1.0) * 13.0 + P("ATTR_X")*6.)/4."
//"Pfingers=Math.ceil(2.*@X.doubleValue()/15.); (Pfingers + 1.0) * 13.0"	 ATTR_X ? Pfingers=Math.ceil(<Expected token ==>2.*P("ATTR_X").doubleValue()/15.); (Pfingers + 1.0) * 13.0 -> "Pfingers=Math.ceil(<Expected token ==>2.*P("ATTR_X").doubleValue()/15.); (Pfingers + 1.0) * 13.0"
//"Pfingers=Math.ceil(2.*@X.doubleValue()/15.); (Pfingers + 1.0) * 8.0 + (Pfingers*1.5) * 5."	 ATTR_X ? Pfingers=Math.ceil(<Expected token ==>2.*P("ATTR_X").doubleValue()/15.); (Pfingers + 1.0) * 8.0 + (Pfingers*1.5) * 5. -> "Pfingers=Math.ceil(<Expected token ==>2.*P("ATTR_X").doubleValue()/15.); (Pfingers + 1.0) * 8.0 + (Pfingers*1.5) * 5."
    }

    private void goodJava(String expr, String expected) {
        CodeExpression ce = CodeExpression.valueOf(expr, CodeExpression.Code.JAVA);
        assertNull(ce.getParseException());
        assertEquals(expected, ce.getSpiceText());
    }

    @Test
    public void testGoodSpice() {
        System.out.println("goodSpice");
        goodSpice("1 + 2", "1+2");
        goodSpice("1 + 2 * 3", "1+2*3");
        goodSpice("1 * 2 + 3", "1*2+3");
        goodSpice("(1 + 2) * 3", "(1+2)*3");
        goodSpice("(1 + 2) * X", "(1+2)*X");
        goodSpice("300 / -1.5e2", "300/-150");
        goodSpice("1.5e-2", "15m");
        goodSpice("20 * 1.5e-2", "20*15m");
        goodSpice("20 * 1.5m", "20*1.5m");
        goodSpice("(1 + a) * 3 + b", "(1+a)*3+b");
        goodSpice("1 + 2 * 3 + - 4", "1+2*3+-4");
        goodSpice("-1", "-1");
        goodSpice("-1 + 2 * 3 + - 4", "-1+2*3+-4");
        goodSpice("-(1 + 2) * 3 + -4", "-(1+2)*3+-4");
        goodSpice("-(1 + 2) * 3 + -4 * -2 - -4 * -3", "-(1+2)*3+-4*-2--4*-3");
        goodSpice("-sin(3)", "-sin(3)");
        goodSpice("-sin(X)", "-sin(X)");
        goodSpice("1-min(1,-2)", "1-min(1,-2)");
        goodSpice("1-min(1,X)", "1-min(1,X)");
        goodSpice("1-min((a+b)*c,X)", "1-min((a+b)*c,X)");
        goodSpice("1-min((a+b)*c,(a+b))", "1-min((a+b)*c,a+b)");
        goodSpice("-a + 2 * 3 * -b + - 4", "-a+2*3*-b+-4");
        goodSpice("1 ? -2 : 4", "1?-2:4");
        goodSpice("0 ? -2 : 4", "0?-2:4");
        goodSpice("8 == 1 ? -2 : 4", "8==1?-2:4");
        goodSpice("8 > 1 ? -2 : 4", "8>1?-2:4");
        goodSpice("1 - 7 <= 1 ? -2 : 4", "1-7<=1?-2:4");
        goodSpice("layer == 1 ? two + 1 : eight * 4 / 2", "layer==1?two+1:eight*4/2");
        goodSpice("0 * 1 ? 3 / 2 : -4 + 10", "0*1?3/2:-4+10");
        goodSpice("(3==0?0.00441:3<8?0.011:0.016)*1e-15", "(3==0?4.41m:3<8?11m:16m)*1f");
        goodSpice("(layer==0?0.00441:layer<8?0.011:0.016)*1e-15", "(layer==0?4.41m:layer<8?11m:16m)*1f");
        goodSpice("1/0", "1/0");
    }

    private void goodSpice(String expr, String expected) {
        CodeExpression ce = CodeExpression.valueOf(expr, CodeExpression.Code.SPICE);
        assertNull(ce.getParseException());
        assertEquals(expected, ce.getSpiceText());
    }

    @Test
    public void testBadSpice() {
        System.out.println("badSpice");
        badSpice("", "<Expected identifier>");
        badSpice("%", "%<Illegal character %>");
        badSpice("-", "-<Expected identifier>");
        badSpice("+", "+<Operator + with no left hand operand>");
        badSpice("1+", "1+<Expected identifier>");
        badSpice("+1", "+<Operator + with no left hand operand>1");
        badSpice("1 1", "1 1<Two operands with no operator>");
        badSpice("1 + + 1", "1 + +<Operator + with no left hand operand> 1");
        badSpice("(", "(<Expected identifier>");
        badSpice("(a", "(a<Expected token )>");
        badSpice(")", ")<Expected identifier>");
        badSpice("a a", "a a<Two operands with no operator>");
        badSpice("sin x", "sin x<Expected token (>");
        // From EvalSpice test
        badSpice("1 2 +", "1 2 <Two operands with no operator>+");
        badSpice("1 + * 2", "1 + *<Operator * with no left hand operand> 2");
        badSpice("1 + 2 * - -3", "1 + 2 * - -<Operator - with no left hand operand>3");
        badSpice("300 / -1.5ee2 + 5", "300 / -1.5ee<Invalid token>2 + 5");
        badSpice("1-min((a+b)*c,(a+b)", "1-min((a+b)*c,(a+b)<Expected token )>");
        badSpice("M1 - M3 : 10001", "M1 - M3 :<Unexpected character :> 10001");
    }

    private void badSpice(String expr, String expected) {
        CodeExpression ce = CodeExpression.valueOf(expr, CodeExpression.Code.SPICE);
        assertEquals(expected, ce.getParseException().getMessage());
    }

    /**
     * Test written by Jonathan Gainsley for EvalSpice class
     */
    @Test
    public void testJonG() {
        System.out.println("Jon's test");
        testEval("1 + 2", 3);
        testEval("1 + 2 * 3", 7);
        testEval("1 * 2 + 3", 5);
        testEval("(1 + 2) * 3", 9);
        testEval("(1 + 2) * x", "3 * x");
        testEval("300 / -1.5e2", -2);
        testEval("1.5e-2", 0.015);
        testEval("20 * 1.5e-2", 0.3);
        testEval("20 * 1.5m", 0.03);
        testEval("(1 + a) * 3 + b", "(1 + a) * 3 + b");
        testEval("1 + 2 * 3 + - 4", 3);
        testEval("-1", -1);
        testEval("-1 + 2 * 3 + - 4", 1);
        testEval("-(1 + 2) * 3 + -4", -13);
        testEval("-(1 + 2) * 3 + -4 * -2 - -4 * -3", -13);
        testEval("-sin(3)", -Math.sin(3));
        testEval("-sin(x)", "-sin(x)");
        testEval("1-min(1,-2)", 3);
        testEval("1-min(1,x)", null);
        testEval("1-min((a+b)*c,x)", null);
        testEval("1-min((a+b)*c,(a+b))", null);
        testEval("-a + 2 * 3 * -b + - 4", null);
        testEval("1 ? -2 : 4", -2);
        testEval("0 ? -2 : 4", 4);
        testEval("8 == 1 ? -2 : 4", 4);
        testEval("8 > 1 ? -2 : 4", -2);
        testEval("1 - 7 <= 1 ? -2 : 4", -2);
        testEval("layer == 1 ? two + 1 : eight * 4 / 2", "layer == 1 ? two + 1 : eight * 4 / 2");
        testEval("0 * 1 ? 3 / 2 : -4 + 10", 6);
        testEval("(3==0?0.00441:3<8?0.011:0.016)*1e-15", 1.1e-17);
        testEval("(layer==0?0.00441:layer<8?0.011:0.016)*1e-15", null);
        System.out.println("\nThese should flag as errors:\n---------------------------\n");
        testEval("1 2 +", null);
        testEval("1 + * 2", null);
        testEval("1 + 2 * - -3", null);
        testEval("300 / -1.5ee2 + 5", null);
        testEval("1-min((a+b)*c,(a+b)", null);
        testEval("1/0", null);
        testEval("M1 - M3 : 10001", null);
    }

    private void testEval(String expr, String expected) {
//        CodeExpression ce = CodeExpression.valueOf(expr, false);
//        String evald = ce.eval().toString();
//        if (expected == null) {
//            System.out.println(expr+" = "+evald);
//        } else {
//            System.out.println(expr+" = "+evald+" -- ("+expected+")");
//            assertEquals(expected, evald);
//        }
    }

    private void testEval(String expr, double expected) {
        CodeExpression ce = CodeExpression.valueOf(expr, CodeExpression.Code.SPICE);
        assertNull(ce.getParseException());
        Double result = (Double) ce.eval();
        assertEquals(expected, result, 0);
    }
}
