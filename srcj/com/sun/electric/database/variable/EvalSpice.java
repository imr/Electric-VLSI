package com.sun.electric.database.variable;

import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Aug 17, 2006
 * Time: 1:58:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class EvalSpice {

    private String expr;
    private StreamTokenizer tokenizer;
    int openParens;

    public EvalSpice(String expr) {
        this.expr = expr;
        openParens = 0;
    }

    public Object evaluate() {
        if (expr == null) return null;
        StringReader reader = new StringReader(expr);
        tokenizer = new StreamTokenizer(reader);
        tokenizer.parseNumbers();
        tokenizer.ordinaryChar('(');
        tokenizer.ordinaryChar(')');
        tokenizer.ordinaryChar('*');
        tokenizer.ordinaryChar('/');
        tokenizer.ordinaryChar('+');
        tokenizer.ordinaryChar('-');
        try {
            return evalEq().eval();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (ParseException e) {
            String parsed = "";
            try {
                long left = reader.skip(Long.MAX_VALUE);
                parsed = expr.substring(0, expr.length()-(int)left);
            } catch (IOException e2) {}
            if (!parsed.equals(""))
                parsed = ": "+parsed+" <--";
            System.out.println("Parse Error: "+e.getMessage()+parsed +" "+tokenizer.toString());
        }
        return expr;
    }

    /**
     * Evaluate an expression
     * @return the evaluated expression
     * @throws IOException
     * @throws ParseException
     */
    private SimpleEq evalEq() throws IOException, ParseException {
        SimpleEq eq = new SimpleEq();

        while (true) {
            int tt = tokenizer.nextToken();
            if (tt == StreamTokenizer.TT_EOF || tt == StreamTokenizer.TT_EOL) {
                if (openParens > 0) throw new ParseException("Unmatched open parens");
                return eq;
            }
            else if (tt == ')') {       // end eq
                openParens--;
                if (openParens < 0) throw new ParseException("Unmatched close parens");
                Object value = eq.eval();
                if (value instanceof String) {
                    return new SimpleEq("("+value.toString()+")", null, null);
                } else {
                    return new SimpleEq(value, null, null);
                }
            }
            else if (tt == '(') {       // begin new eq
                openParens++;
                if (!eq.addIdentifierOk()) throw new ParseException("Too many identifiers");
                Object id = evalEq();
                eq.addIdentifier(id);
            }
            else if (tt == ',') {       // end eq
                return eq;
            }
            else if (tt == StreamTokenizer.TT_NUMBER) {
                tokenizer.pushBack();
                eq.addIdentifier(parseNumber());
            }
            else if (tt == StreamTokenizer.TT_WORD) {
                if (tokenizer.sval.equalsIgnoreCase("sin")) {
                    expect('(');
                    openParens++;
                    Object arg = evalEq().eval();
                    if (arg instanceof Double) {
                        arg = new Double(Math.sin(((Double)arg).doubleValue()));
                    } else {
                        arg = "sin"+arg.toString();
                    }
                    eq.addIdentifier(arg);
                }
                else if (tokenizer.sval.equalsIgnoreCase("min")) {
                    expect('(');
                    openParens++;
                    Object m1 = evalEq().eval();
                    Object m2 = evalEq().eval();
                    if ((m1 instanceof Double) && (m2 instanceof Double)) {
                        double a = ((Double)m1).doubleValue();
                        double b = ((Double)m2).doubleValue();
                        m1 = new Double(Math.min(a,b));
                    } else {
                        String m2str = m2.toString();
                        // remove extraneous ()'s
                        if (m2str.startsWith("(") && m2str.endsWith(")"))
                            m2str = m2str.substring(1, m2str.length()-1);
                        m1 = "min("+m1.toString()+","+m2str+")";
                    }
                    eq.addIdentifier(m1);
                }
                else if (tokenizer.sval.equalsIgnoreCase("max")) {
                    expect('(');
                    openParens++;
                    Object m1 = evalEq().eval();
                    Object m2 = evalEq().eval();
                    if ((m1 instanceof Double) && (m2 instanceof Double)) {
                        double a = ((Double)m1).doubleValue();
                        double b = ((Double)m2).doubleValue();
                        m1 = new Double(Math.max(a,b));
                    } else {
                        String m2str = m2.toString();
                        // remove extraneous ()'s
                        if (m2str.startsWith("(") && m2str.endsWith(")"))
                            m2str = m2str.substring(1, m2str.length()-1);
                        m1 = "max("+m1.toString()+","+m2str+")";
                    }
                    eq.addIdentifier(m1);
                }
                else if (tokenizer.sval.equalsIgnoreCase("abs")) {
                    expect('(');
                    openParens++;
                    Object arg = evalEq().eval();
                    if (arg instanceof Double) {
                        arg = new Double(Math.abs(((Double)arg).doubleValue()));
                    } else {
                        arg = "abs"+arg.toString();
                    }
                    eq.addIdentifier(arg);
                }
                else if (tokenizer.sval.equalsIgnoreCase("sqrt")) {
                    expect('(');
                    openParens++;
                    Object arg = evalEq().eval();
                    if (arg instanceof Double) {
                        arg = new Double(Math.sqrt(((Double)arg).doubleValue()));
                    } else {
                        arg = "sqrt"+arg.toString();
                    }
                    eq.addIdentifier(arg);
                }
                else if (tokenizer.sval.equalsIgnoreCase("int")) {
                    expect('(');
                    openParens++;
                    Object arg = evalEq().eval();
                    if (arg instanceof Double) {
                        arg = new Double((int)(((Double)arg).doubleValue()));
                    } else {
                        arg = "int"+arg.toString();
                    }
                    eq.addIdentifier(arg);
                }
                else {
                    // identifier
                    eq.addIdentifier(tokenizer.sval);
                }
            }
            else if (tt == '*') {
                eq.addOp(Op.MULT);
            }
            else if (tt == '/') {
                eq.addOp(Op.DIV);
            }
            else if (tt == '+') {
                eq.addOp(Op.PLUS);
            }
            else if (tt == '-') {
                eq.addOp(Op.MINUS);
            }
        }
    }

    /**
     * Parse a number. A number may have the format:
     * <P>
     * number[g|meg|k|m|u|n|p|f]
     * <p>
     * number(e)[-]number
     * @return
     * @throws IOException
     * @throws ParseException
     */
    private Object parseNumber() throws IOException, ParseException {
        int tt = tokenizer.nextToken();
        if (tt == StreamTokenizer.TT_NUMBER) {
            double val = tokenizer.nval;
            // peek ahead to check if exponential, or multiplier
            tokenizer.ordinaryChar('e');
            tokenizer.ordinaryChar('E');
            tt = tokenizer.nextToken();
            if (tt == 'e' || tt == 'E') {
                tt = tokenizer.nextToken();
                boolean minus = false;
                if (tt == '-') {
                    minus = true;
                    tt = tokenizer.nextToken();
                }
                if (tt == StreamTokenizer.TT_NUMBER) {
                    double exp = tokenizer.nval;
                    if (minus) exp = -1.0 * exp;
                    val = val * Math.pow(10, exp);
                } else {
                    throw new ParseException("Invalid token");
                }
            }
            else if (tt == StreamTokenizer.TT_WORD) {
                if (tokenizer.sval.equalsIgnoreCase("g")) {
                    val = val * 1e9;
                } else if (tokenizer.sval.equalsIgnoreCase("meg")) {
                    val = val * 1e6;
                } else if (tokenizer.sval.equalsIgnoreCase("k")) {
                    val = val * 1e3;
                } else if (tokenizer.sval.equalsIgnoreCase("m")) {
                    val = val * 1e-3;
                } else if (tokenizer.sval.equalsIgnoreCase("u")) {
                    val = val * 1e-6;
                } else if (tokenizer.sval.equalsIgnoreCase("n")) {
                    val = val * 1e-9;
                } else if (tokenizer.sval.equalsIgnoreCase("p")) {
                    val = val * 1e-12;
                } else if (tokenizer.sval.equalsIgnoreCase("f")) {
                    val = val * 1e-15;
                } else
                    throw new ParseException("Invalid token");
            }
            else {
                tokenizer.pushBack();
            }
            tokenizer.wordChars('e', 'e');
            tokenizer.wordChars('E', 'E');
            return new Double(val);
        }
        throw new ParseException("Expected number");
    }

    private void expect(int token) throws IOException, ParseException {
        int tt = tokenizer.nextToken();
        if (tt != token) throw new ParseException("Expected token "+token);
    }

    // ==================== Parsable Objects ========================

    public static class ParseException extends Exception {
        public ParseException(String msg) { super(msg); }
    }

    private enum Op { MULT, DIV, PLUS, MINUS };
    private static char getOpStr(Op op) {
        if (op == null) return '?';
        switch (op) {
            case MULT: return '*';
            case DIV: return '/';
            case PLUS: return '+';
            case MINUS: return '-';
        }
        return '?';
    }

    /**
     * A simple equation consists of two Identifiers (operands)
     * that are Doubles, Strings, or other SimpleEq,
     * and an operator *,/,+,-.
     * <P>
     * For a simple equation to be valid, it must define
     * both operands and an operator. However, if the
     * operator is '-', then the left hand operand may
     * be null, to indicate a unary minus.  Additionally,
     * if only the left hand operator is defined, then it
     * is simply one operand.
     */
    public static class SimpleEq {
        protected Object lhop;        // left hand operand
        private Op op;              // operator
        protected Object rhop;        // right hand operand
        boolean neglh = false;
        boolean negrh = false;

        public SimpleEq() {
            this.lhop = null;
            this.op = null;
            this.rhop = null;
        }
        public SimpleEq(Object lhop, Op op, Object rhop) {
            this.lhop = lhop;
            this.op = op;
            this.rhop = rhop;
        }

        public boolean addIdentifierOk() {
            if (lhop == null)
                return true;
            else if (rhop == null && op != null)
                return true;
            else if (rhop instanceof SimpleEq)
                return ((SimpleEq)rhop).addIdentifierOk();
            return false;
        }

        public void addIdentifier(Object id) throws ParseException {
            if (lhop == null)
                lhop = id;
            else if (rhop == null && op != null)
                rhop = id;
            else if (rhop instanceof SimpleEq)
                ((SimpleEq)rhop).addIdentifier(id);
            else
                throw new ParseException("Two operands with no operator");
        }

        public void addOp(Op operator) throws ParseException {
            if (lhop == null && operator == Op.MINUS && !neglh)
                neglh = true;               // unary minus on left hand operand
            else if (lhop == null)
                throw new ParseException("Operator "+getOpStr(operator)+" with no left hand operand");
            // lhop defined from here on
            else if (op == null && rhop == null)
                this.op = operator;
            else if (op != null && rhop == null && operator == Op.MINUS && !negrh)
                negrh = true;               // unary minus on right hand operand
            else if (op != null && rhop != null) {
                if (rhop instanceof SimpleEq) {
                    ((SimpleEq)rhop).addOp(operator);
                }
                else {
                    if ((op == Op.PLUS || op == Op.MINUS) && (operator == Op.MULT || operator == Op.DIV)) {
                        // bind right
                        rhop = new SimpleEq(rhop, operator, null);
                        // retain proper negation associations
                        ((SimpleEq)rhop).neglh = negrh;
                        negrh = false;
                    } else {
                        // bind left
                        lhop = new SimpleEq(lhop, op, rhop);
                        this.op = operator;
                        this.rhop = null;
                        // retain proper negation associations
                        ((SimpleEq)lhop).neglh = neglh;
                        ((SimpleEq)lhop).negrh = negrh;
                        this.neglh = false;
                        this.negrh = false;
                    }
                }
            }
            else
                throw new ParseException(("Two operators in a row"));
        }

        /**
         * Return either a Double, if the equation can be
         * resolved numerically, or a String representing
         * the equation after any numerical resolution can be done.
         * @return a Double or a String
         */
        public Object eval() {
            if (lhop instanceof SimpleEq)
                lhop = ((SimpleEq)lhop).eval();
            if (rhop instanceof SimpleEq)
                rhop = ((SimpleEq)rhop).eval();

            if ((lhop instanceof Double) && (rhop instanceof Double)) {
                double lh = ((Double)lhop).doubleValue();
                double rh = ((Double)rhop).doubleValue();
                if (neglh) lh = -1.0 * lh;
                if (negrh) rh = -1.0 * rh;
                switch (op) {
                    case MULT: return new Double(lh * rh);
                    case DIV: return new Double(lh / rh);
                    case PLUS: return new Double(lh + rh);
                    case MINUS: return new Double(lh - rh);
                }
            }

            if (op == null && rhop == null) {
                if (neglh) {
                    if (lhop instanceof Double) {
                        return -1.0 * ((Double)lhop).doubleValue();
                    } else
                        return "-"+lhop.toString();
                }
                return lhop;
            }

            String neglhstr = neglh ? "-" : "";
            String negrhstr = negrh ? "-" : "";
            String lhstr = (lhop == null ? "?" : lhop.toString());
            String rhstr = (rhop == null ? "?" : rhop.toString());

            // can't resolve numerically
            return neglhstr + lhstr + " " + getOpStr(op) + " " + negrhstr + rhstr;
        }

    }


    // ================================ Main Test ================================

    public static void main(String args[]) {
        testEval("1 + 2", 3);
        testEval("1 + 2 * 3", 7);
        testEval("1 * 2 + 3", 5);
        testEval("(1 + 2) * 3", 9);
        testEval("(1 + 2) * x", "3.0 * x");
        testEval("300 / -1.5e2", -2);
        testEval("1.5e-2", 0.015);
        testEval("20 * 1.5e-2", 0.3);
        testEval("20 * 1.5m", 0.03);
        testEval("(1 + a) * 3 + b", "(1.0 + a) * 3.0 + b");
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
        System.out.println("\nThese should flag as errors:\n---------------------------\n");
        testEval("1 2 +", null);
        testEval("1 + * 2", null);
        testEval("1 + 2 * - -3", null);
        testEval("300 / -1.5ee2 + 5", null);
        testEval("1-min((a+b)*c,(a+b)", null);
    }

    private static void testEval(String eq, String expected) {
        EvalSpice sp = new EvalSpice(eq);
        String evald = sp.evaluate().toString();
        if (expected == null) {
            System.out.println(eq+" = "+evald);
        } else {
            System.out.println(eq+" = "+evald+" -- ("+expected+")");
            assert(expected.equals(evald));
        }
    }

    private static void testEval(String eq, double expected) {
        EvalSpice sp = new EvalSpice(eq);
        Object evald = sp.evaluate();
        System.out.println(eq+" = "+evald+" -- ("+expected+")");
        assert(evald instanceof Double);
        double val = ((Double)evald).doubleValue();
        assert(val == expected);
    }
}
