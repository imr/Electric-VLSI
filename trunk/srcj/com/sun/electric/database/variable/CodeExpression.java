/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CodeExpression.java
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

import com.sun.electric.database.EObjectInputStream;
import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.text.TextUtils;

import java.io.IOException;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class encapsulate expression in Java-like or Spice-like syntax
 */
public class CodeExpression implements Serializable {
    // Variable flags in C Electric

    /** variable is interpreted code (with VCODE2) */
    private static final int VCODE1 = 040;
    /** variable is interpreted code (with VCODE1) */
    private static final int VCODE2 = 04000000000;
    /** variable is LISP */
    private static final int VSPICE = VCODE1;
    /** variable is TCL */
    private static final int VTCL = VCODE2;
    /** variable is Java */
    private static final int VJAVA = (VCODE1 | VCODE2);
    private static final HashMap<String, CodeExpression> javaExpressions = new HashMap<String, CodeExpression>();
    private static final HashMap<String, CodeExpression> spiceExpressions = new HashMap<String, CodeExpression>();
    private static final HashMap<String, CodeExpression> tclExpressions = new HashMap<String, CodeExpression>();
    private static long numValueOfs;
    /** For replacing @variable */
    private static final Pattern pPat = Pattern.compile("P\\(\"(\\w+)\"\\)");
    private final Code code;
    private final String expr;
    private final Set<Variable.Key> depends;
    private final Expr exprTree;
    private final boolean dependsOnEverything;
    private final EvalSpice.ParseException parseException;
    private final String spiceText;
    private final String spiceTextPar;

    /**
     * The type of Code that determines how this Variable's
     * value should be evaluated. If NONE, no evaluation is done.
     */
    public static enum Code {

        /** Indicator that code is in Java. */
        JAVA("Java", VJAVA),
        /** Indicator that code is in Lisp. */
        SPICE("Spice", VSPICE),
        /** Indicator that code is in TCL. */
        TCL("TCL (not avail.)", VTCL),
        /** Indicator that this is not code. */
        NONE("Not Code", 0);
        private final String name;
        private final int cFlags;
        private static final Code[] allCodes = Code.class.getEnumConstants();

        private Code(String name, int cFlags) {
            this.name = name;
            this.cFlags = cFlags;
        }

        /**
         * Method to return the bits value of this code type.
         * This is used in I/O.
         * @return the bits value of this code type.
         */
        public int getCFlags() {
            return cFlags;
        }

        /**
         * Method to return a printable version of this Code.
         * @return a printable version of this Code.
         */
        public String toString() {
            return name;
        }

        /**
         * Method to get an iterator over all Code types.
         */
        public static Iterator<Code> getCodes() {
            return ArrayIterator.iterator(allCodes);
        }

        /**
         * Method to convert a bits value to a Code object.
         * @param cBits the bits value (from I/O).
         * @return the Code associated with those bits.
         */
        public static Code getByCBits(int cBits) {
            switch (cBits & (VCODE1 | VCODE2)) {
                case VJAVA:
                    return JAVA;
                case VSPICE:
                    return SPICE;
                case VTCL:
                    return TCL;
                default:
                    return NONE;
            }
        }

        /**
         * Method to get a Code constant by its ordinal number.
         * @param ordinal the ordinal number of this Code constant ( as returned by ordinal()
         * @return the Code associated with this ordinal number.
         */
        public static Code getByOrdinal(int ordinal) {
            return allCodes[ordinal];
        }
    }

    private CodeExpression(String expr, Code code) {
        this.code = code;
        this.expr = expr;
        TreeSet<Variable.Key> varKeys = new TreeSet<Variable.Key>();
        String replacedExpr = EvalJavaBsh.replace(expr);
        Matcher pMat = pPat.matcher(replacedExpr);
        while (pMat.find()) {
            String varName = pMat.group(1);
            varKeys.add(Variable.newKey(varName));
        }
        depends = Collections.unmodifiableSet(varKeys);

        String patchedExpr = expr.replace("((Number)@X).doubleValue()", "@X");
        Expr exprTree = null;
        EvalSpice.ParseException parseException = null;
        try {
            switch (code) {
                case JAVA:
                    exprTree = parse(patchedExpr, true);
                    break;
                case SPICE:
                    exprTree = parse(patchedExpr, false);
                    break;
                default:
                    throw new EvalSpice.ParseException("Unsupported code " + code);
            }

        } catch (EvalSpice.ParseException e) {
            parseException = e;
        }
        this.exprTree = exprTree;
        this.parseException = parseException;

        if (parseException != null) {
            dependsOnEverything = true;
            spiceTextPar = spiceText = parseException.getMessage();
        } else {
            dependsOnEverything = exprTree.dependsOnEverything();
            StringBuilder sb = new StringBuilder();
            exprTree.appendText(sb);
            spiceText = new String(sb);
            sb.setLength(0);
            exprTree.appendText(sb, Expr.MIN_PRECEDENCE);
            spiceTextPar = new String(sb);
        }
    }

    private Object writeReplace() {
        return new CodeExpressionKey(this);
    }

    private static class CodeExpressionKey extends EObjectInputStream.Key<CodeExpression> {

        public CodeExpressionKey() {
        }

        private CodeExpressionKey(CodeExpression ce) {
            super(ce);
        }

        @Override
        public void writeExternal(EObjectOutputStream out, CodeExpression ce) throws IOException {
            out.writeUTF(ce.getExpr());
            out.writeByte(ce.getCode().ordinal());
        }

        @Override
        public CodeExpression readExternal(EObjectInputStream in) throws IOException, ClassNotFoundException {
            String expr = in.readUTF();
            Code code = Code.getByOrdinal(in.readByte());
            return valueOf(expr, code);
        }
    }

    public static synchronized CodeExpression valueOf(String expression, Code code) {
        numValueOfs++;
        HashMap<String, CodeExpression> allExpressions;
        switch (code) {
            case JAVA:
                allExpressions = javaExpressions;
                break;
            case SPICE:
                allExpressions = spiceExpressions;
                break;
            case TCL:
                allExpressions = tclExpressions;
                break;
            default:
                throw new IllegalArgumentException("code");
        }
        CodeExpression ce = allExpressions.get(expression);
        if (ce == null) {
            ce = new CodeExpression(expression, code);
            allExpressions.put(expression, ce);
        }
        return ce;
    }

    public Code getCode() {
        return code;
    }

    public boolean isJava() {
        return code == Code.JAVA;
    }

    public String getExpr() {
        return expr;
    }

    public Set<Variable.Key> dependsOn() {
        return depends;
    }

    public EvalSpice.ParseException getParseException() {
        return parseException;
    }

    public String getSpiceText() {
        return spiceText;
    }

    public String getHSpiceText(boolean inPar) {
        return dependsOnEverything ? null : inPar ? spiceTextPar : spiceText;
    }

    public String getVerilogText() {
        return spiceText;
    }

    public Object eval() {
        if (parseException != null) {
            return parseException.getMessage();
        }
        return exprTree.eval(new EvalContext());
    }

    @Override
    public int hashCode() {
        return expr.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CodeExpression) {
            CodeExpression that = (CodeExpression) o;
            return this.code == that.code && this.expr.equals(that.expr);
        }
        return false;
    }

    @Override
    public String toString() {
        return expr;
    }

    /**
     * Write this CodeExpression to IdWriter.
     * @param writer where to write.
     */
    public void write(IdWriter writer) throws IOException {
        writer.writeString(expr);
        writer.writeByte((byte) code.ordinal());
    }

    /**
     * Read CodeExpression from SnapshotReader.
     * @param reader from to write.
     */
    public static CodeExpression read(IdReader reader) throws IOException {
        String expr = reader.readString();
        Code code = Code.getByOrdinal(reader.readByte());
        return valueOf(expr, code);
    }

    void check() {
        assert expr != null;
        assert code != null && code != Code.NONE;
    }

    /**
     * Print statistics about CodeExpressions.
     * @param verbose print all CodeExpressions
     */
    public static void printStatistics(boolean verbose) {
        System.out.println((javaExpressions.size() + spiceExpressions.size() + tclExpressions.size())
                + " CodeExpressions after " + numValueOfs + " valueOf calls");
        if (!verbose) {
            return;
        }

        System.out.println(javaExpressions.size() + " java strings");
        for (CodeExpression ce : new TreeMap<String, CodeExpression>(javaExpressions).values()) {
            printCE(ce);
        }
        System.out.println(spiceExpressions.size() + " spice strings");
        for (CodeExpression ce : new TreeMap<String, CodeExpression>(spiceExpressions).values()) {
            printCE(ce);
        }
        System.out.println(tclExpressions.size() + " tcl strings");
        for (CodeExpression ce : new TreeMap<String, CodeExpression>(tclExpressions).values()) {
            printCE(ce);
        }
    }

    private static void printCE(CodeExpression ce) {
        System.out.print("\"" + ce.getExpr() + "\"\t");
        if (ce.dependsOnEverything) {
            System.out.print(" ALL");
        }
        for (Variable.Key varKey : ce.dependsOn()) {
            System.out.print(" " + varKey);
        }
        if (ce.parseException != null) {
            System.out.print(" ? " + ce.parseException.getMessage());
        } else {
            System.out.print(" -> \"" + ce.spiceText + "\"");
        }
        System.out.println();
    }

    private static class EvalContext {

        private Object getDrive() {
            return null;
        }

        private Object subDrive(String instName, String varName) {
            return null;
        }

        private Object get(Variable.Key varKey) {
            return null;
        }
    }

    private static abstract class Expr {

        static final int MIN_PRECEDENCE = 1;
        static final int MAX_PRECEDENCE = EvalSpice.Op.COND.precedence;

        int numSubExprs() {
            return 0;
        }

        Expr getSubExpr(int i) {
            throw new IndexOutOfBoundsException();
        }

        void appendText(StringBuilder sb, int outerPrecedence) {
            if (outerPrecedence < precedence()) {
                sb.append('(');
                appendText(sb);
                sb.append(')');
            } else {
                appendText(sb);
            }
        }

        abstract void appendText(StringBuilder sb);

        int precedence() {
            return MIN_PRECEDENCE;
        }

        boolean dependsOnEverything() {
            for (int i = 0; i < numSubExprs(); i++) {
                if (getSubExpr(i).dependsOnEverything()) {
                    return true;
                }
            }
            return false;
        }

        abstract Object eval(EvalContext context);

        static boolean bool(double d) {
            return d != 0;
        }
    }

    private static class ConstExpr extends Expr {

        private final Object value;

        ConstExpr(Object value) {
            this.value = value;
        }

        void appendText(StringBuilder sb) {
            String s = TextUtils.formatDoublePostFix(((Double) value).doubleValue());
            sb.append(s);
        }

        Object eval(EvalContext context) {
            return value;
        }
    }

    private static class VarExpr extends Expr {

        private final Variable.Key varKey;

        VarExpr(Variable.Key varKey) {
            if (varKey == null) {
                throw new NullPointerException();
            }
            this.varKey = varKey;
        }

        void appendText(StringBuilder sb) {
            String name = varKey.getName();
            if (name.startsWith("ATTR_")) {
                name = name.substring(5);
            }
            sb.append(name);
        }

        Object eval(EvalContext context) {
            return context.get(varKey);
        }
    }

    private static class GetDriveExpr extends Expr {

        GetDriveExpr() {
        }

        void appendText(StringBuilder sb) {
            sb.append("LE.getdrive()");
        }

        boolean dependsOnEverything() {
            return true;
        }

        Object eval(EvalContext context) {
            return context.getDrive();
        }
    }

    private static class SubDriveExpr extends Expr {

        private final String instName, varName;

        SubDriveExpr(String instName, String varName) {
            this.instName = instName;
            this.varName = varName;
        }

        void appendText(StringBuilder sb) {
            sb.append("LE.subdrive(\"" + instName + "\",\"" + varName + "\")");
        }

        boolean dependsOnEverything() {
            return true;
        }

        Object eval(EvalContext context) {
            return context.subDrive(instName, varName);
        }
    }

    private static abstract class UnaryExpr extends Expr {

        final Expr s;

        UnaryExpr(Expr s) {
            this.s = s;
        }

        int numSubExprs() {
            return 1;
        }

        Expr getSubExpr(int i) {
            if (i == 0) {
                return s;
            }
            return super.getSubExpr(i);
        }

        Object eval(EvalContext context) {
            double v = ((Number) s.eval(context)).doubleValue();
            return Double.valueOf(apply(v));
        }

        abstract double apply(double v);
    }

    private static class UnaryOpExpr extends UnaryExpr {

        private static final String opName = EvalSpice.Op.MINUS.name;
        private static final int opPrecedence = MIN_PRECEDENCE;

        UnaryOpExpr(Expr s) {
            super(s);
        }

        void appendText(StringBuilder sb) {
            sb.append(opName);
            s.appendText(sb, opPrecedence);
        }

        int precedence() {
            return opPrecedence;
        }

        double apply(double v) {
            return -v;
        }
    }

    private static class UnaryFunExpr extends UnaryExpr {

        enum Fun {

            sin, abs, sqrt, int_;

            @Override
            public String toString() {
                return this == int_ ? "int" : super.toString();
            }
        };
        private final Fun fun;

        UnaryFunExpr(Fun fun, Expr s) {
            super(s);
            this.fun = fun;
        }

        void appendText(StringBuilder sb) {
            sb.append(fun);
            sb.append('(');
            s.appendText(sb, MAX_PRECEDENCE);
            sb.append(')');
        }

        double apply(double v) {
            switch (fun) {
                case sin:
                    return Math.sin(v);
                case abs:
                    return Math.abs(v);
                case sqrt:
                    return Math.sqrt(v);
                case int_:
                    return (int) v;
            }
            throw new AssertionError();
        }
    }

    private static abstract class BinaryExpr extends Expr {

        final Expr ls, rs;

        BinaryExpr(Expr ls, Expr rs) {
            this.ls = ls;
            this.rs = rs;
        }

        int numSubExprs() {
            return 2;
        }

        Expr getSubExpr(int i) {
            if (i == 0) {
                return ls;
            }
            if (i == 1) {
                return rs;
            }
            return super.getSubExpr(i);
        }

        Object eval(EvalContext context) {
            double lv = ((Double) ls.eval(context)).doubleValue();
            double rv = ((Double) rs.eval(context)).doubleValue();
            return Double.valueOf(apply(lv, rv));
        }

        abstract double apply(double lv, double rv);
    }

    private static class BinaryOpExpr extends BinaryExpr {

        private final EvalSpice.Op op;

        BinaryOpExpr(Expr ls, EvalSpice.Op op, Expr rs) {
            super(ls, rs);
            this.op = op;
        }

        void appendText(StringBuilder sb) {
            ls.appendText(sb, op.precedence);
            sb.append(op.name);
            rs.appendText(sb, op.precedence - 1);
        }

        int precedence() {
            return op.precedence;
        }

        double apply(double lv, double rv) {
            if (op == EvalSpice.Op.MULT) {
                return lv * rv;
            }
            if (op == EvalSpice.Op.DIV) {
                return lv / rv;
            }
            if (op == EvalSpice.Op.PLUS) {
                return lv + rv;
            }
            if (op == EvalSpice.Op.MINUS) {
                return lv - rv;
            }
            if (op == EvalSpice.Op.LT) {
                return valueOf(lv < rv);
            }
            if (op == EvalSpice.Op.LTOE) {
                return valueOf(lv <= rv);
            }
            if (op == EvalSpice.Op.GT) {
                return valueOf(lv > rv);
            }
            if (op == EvalSpice.Op.GTOE) {
                return valueOf(lv >= rv);
            }
            if (op == EvalSpice.Op.EQ) {
                return valueOf(lv == rv);
            }
            if (op == EvalSpice.Op.NE) {
                return valueOf(lv != rv);
            }
            if (op == EvalSpice.Op.LAND) {
                return valueOf(bool(lv) && bool(rv));
            }
            if (op == EvalSpice.Op.LOR) {
                return valueOf(bool(lv) || bool(rv));
            }
            throw new AssertionError();
        }

        private static double valueOf(boolean b) {
            return b ? 1 : 0;
        }
    }

    private static class BinaryFunExpr extends BinaryExpr {

        enum Fun {

            min, max
        };
        private final Fun fun;

        BinaryFunExpr(Fun fun, Expr ls, Expr rs) {
            super(ls, rs);
            this.fun = fun;
        }

        void appendText(StringBuilder sb) {
            sb.append(fun);
            sb.append('(');
            ls.appendText(sb, MAX_PRECEDENCE);
            sb.append(',');
            rs.appendText(sb, MAX_PRECEDENCE);
            sb.append(')');
        }

        double apply(double lv, double rv) {
            switch (fun) {
                case min:
                    return Math.min(lv, rv);
                case max:
                    return Math.max(lv, rv);
            }
            throw new AssertionError();
        }
    }

    private static class IfThenElseExpr extends Expr {

        private static final int precedence = MAX_PRECEDENCE;
        final Expr condS, thenS, elseS;

        IfThenElseExpr(Expr condS, Expr thenS, Expr elseS) {
            this.condS = condS;
            this.thenS = thenS;
            this.elseS = elseS;
        }

        int numSubExprs() {
            return 3;
        }

        Expr getSubExpr(int i) {
            if (i == 0) {
                return condS;
            }
            if (i == 1) {
                return thenS;
            }
            if (i == 2) {
                return elseS;
            }
            return super.getSubExpr(i);
        }

        void appendText(StringBuilder sb) {
            condS.appendText(sb, precedence - 1);
            sb.append('?');
            thenS.appendText(sb, precedence - 1);
            sb.append(':');
            elseS.appendText(sb, precedence);
        }

        int precedence() {
            return MAX_PRECEDENCE;
        }

        Object eval(EvalContext context) {
            boolean condV = bool(((Double) condS.eval(context)).doubleValue());
            double v = ((Double) (condV ? thenS : elseS).eval(context)).doubleValue();
            return Double.valueOf(v);
        }
    }

    static Expr parse(String expr, boolean isJava) throws EvalSpice.ParseException {
        return new ParseSpice_(expr, isJava).parse();
    }

    static class ParseSpice_ {

        private String expr;
        private boolean isJava;
        private StringReader reader;
        private StreamTokenizer tokenizer;
        EvalSpice.Op op;

        private ParseSpice_(String expr, boolean isJava) {
            this.expr = expr;
            this.isJava = isJava;
            reader = new StringReader(expr);
            tokenizer = new StreamTokenizer(reader);
            tokenizer.parseNumbers();
            tokenizer.ordinaryChar('(');
            tokenizer.ordinaryChar(')');
            tokenizer.ordinaryChar('*');
            tokenizer.ordinaryChar('/');
            tokenizer.ordinaryChar('+');
            tokenizer.ordinaryChar('-');
            tokenizer.ordinaryChar('<');
            tokenizer.ordinaryChar('=');
            tokenizer.ordinaryChar('>');
            tokenizer.ordinaryChar('!');
            tokenizer.ordinaryChar('?');
            tokenizer.ordinaryChar(':');
            tokenizer.ordinaryChar('@');
            tokenizer.quoteChar('"');
            tokenizer.wordChars('_', '_');
        }

        private Expr parse() throws EvalSpice.ParseException {
            try {
                nextToken();
                Expr expr = evalEq();
                assert op == null;
                switch (tokenizer.ttype) {
                    case StreamTokenizer.TT_EOF:
                        return expr;
                    case StreamTokenizer.TT_EOL:
                        throw new EvalSpice.ParseException("Multiline expression");
                    case StreamTokenizer.TT_NUMBER:
                    case StreamTokenizer.TT_WORD:
                    case '(':
                        throw new EvalSpice.ParseException("Two operands with no operator");
                    default:
                        throw new EvalSpice.ParseException("Unexpected character " + (char) tokenizer.ttype);
                }
            } catch (IOException e) {
                throw new EvalSpice.ParseException(e.getMessage());
            } catch (EvalSpice.ParseException e) {
                try {
                    long left = reader.skip(Long.MAX_VALUE);
                    int pos = expr.length() - (int) left;
                    throw new EvalSpice.ParseException(expr.substring(0, pos) + "<" + e.getMessage() + ">" + expr.substring(pos));
                } catch (IOException e2) {
                }
            }
            throw new AssertionError();
        }

        private Expr evalEq() throws IOException, EvalSpice.ParseException {
            return evalEq(Expr.MAX_PRECEDENCE);
        }

        /**
         * Evaluate an expression
         * @return the evaluated expression
         * @throws IOException
         * @throws ParseException
         */
        private Expr evalEq(int outerPrecedence) throws IOException, EvalSpice.ParseException {
            boolean unaryMinus = false;
            if (op == EvalSpice.Op.MINUS) {
                unaryMinus = true;
                nextToken();
            }
            Expr e;
            if (tokenizer.ttype == '(') {
                nextToken();
                e = evalEq();
                expect(')');
            } else if (tokenizer.ttype == StreamTokenizer.TT_NUMBER) {
                e = parseNumber();
            } else if (tokenizer.ttype == StreamTokenizer.TT_WORD) {
                e = parseWord();
            } else if (tokenizer.ttype == '@') {
                if (nextToken() != StreamTokenizer.TT_WORD) {
                    throw new EvalSpice.ParseException("Bad name after @");
                }
                e = new VarExpr(Variable.newKey("ATTR_" + tokenizer.sval));
                nextToken();
            } else if (op != null) {
                throw new EvalSpice.ParseException("Operator " + op + " with no left hand operand");
            } else {
                throw new EvalSpice.ParseException("Expected identifier");
            }
            if (unaryMinus) {
                e = new UnaryOpExpr(e);
            }

            for (;;) {
                if (op == null || outerPrecedence < op.precedence) {
                    return e;
                }
                if (op == EvalSpice.Op.COND) {
                    nextToken();
                    Expr thenE = evalEq(Expr.MAX_PRECEDENCE - 1);
                    expect(':');
                    Expr elseE = evalEq();
                    return new IfThenElseExpr(e, thenE, elseE);
                }
                EvalSpice.Op myOp = op;
                assert outerPrecedence >= myOp.precedence;
                nextToken();
                Expr e2 = evalEq(myOp.precedence - 1);
                e = new BinaryOpExpr(e, myOp, e2);
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
        private ConstExpr parseNumber() throws IOException, EvalSpice.ParseException {
            assert tokenizer.ttype == StreamTokenizer.TT_NUMBER;
            double val = tokenizer.nval;
            // peek ahead to check if exponential, or multiplier
            tokenizer.ordinaryChar('e');
            tokenizer.ordinaryChar('E');
            int tt = tokenizer.nextToken();
            if (tt == 'e' || tt == 'E') {
                tt = tokenizer.nextToken();
                boolean minus = false;
                if (tt == '-') {
                    minus = true;
                    tt = tokenizer.nextToken();
                }
                if (tt == StreamTokenizer.TT_NUMBER) {
                    double exp = tokenizer.nval;
                    if (minus) {
                        exp = -1.0 * exp;
                    }
                    val = val * Math.pow(10, exp);
                } else {
                    throw new EvalSpice.ParseException("Invalid token");
                }
            } else if (tt == StreamTokenizer.TT_WORD) {
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
                } else {
                    throw new EvalSpice.ParseException("Invalid token");
                }
            } else {
                tokenizer.pushBack();
            }
            tokenizer.wordChars('e', 'e');
            tokenizer.wordChars('E', 'E');
            nextToken();
            return new ConstExpr(new Double(val));
        }

        private Expr parseWord() throws IOException, EvalSpice.ParseException {
            assert tokenizer.ttype == StreamTokenizer.TT_WORD;
            String id = tokenizer.sval;
            for (UnaryFunExpr.Fun fun : UnaryFunExpr.Fun.class.getEnumConstants()) {
                if (!id.equalsIgnoreCase(fun.toString()) && !id.equals("Math." + fun.toString())) {
                    continue;
                }
                nextToken();
                expect('(');
                Expr arg = evalEq();
                expect(')');
                return new UnaryFunExpr(fun, arg);
            }
            for (BinaryFunExpr.Fun fun : BinaryFunExpr.Fun.class.getEnumConstants()) {
                if (!id.equalsIgnoreCase(fun.toString()) && !id.equals("Math." + fun.toString())) {
                    continue;
                }
                nextToken();
                expect('(');
                Expr arg1 = evalEq();
                expect(',');
                Expr arg2 = evalEq();
                expect(')');
                return new BinaryFunExpr(fun, arg1, arg2);
            }
            if (/*isJava &&*/id.equals("P")) {
                nextToken();
                expect('(');
                if (tokenizer.ttype != '"') {
                    throw new EvalSpice.ParseException("Bad name after @");
                }
                Variable.Key varKey = Variable.newKey(tokenizer.sval);
                nextToken();
                expect(')');
                return new VarExpr(varKey);
            }
            if (isJava && id.equals("LE.getdrive")) {
                nextToken();
                expect('(');
                expect(')');
                return new GetDriveExpr();
            }
            if (isJava && id.equals("LE.subdrive")) {
                nextToken();
                expect('(');
                if (tokenizer.ttype != '"') {
                    throw new EvalSpice.ParseException("Bad name after subdrive");
                }
                String instName = tokenizer.sval;
                nextToken();
                expect(',');
                if (tokenizer.ttype != '"') {
                    throw new EvalSpice.ParseException("Bad name after subdrive");
                }
                String varName = tokenizer.sval;
                nextToken();
                expect(')');
                return new SubDriveExpr(instName, varName);
            }
            Variable.Key attrKey = Variable.newKey("ATTR_" + tokenizer.sval);
            nextToken();
            return new VarExpr(attrKey);
        }

        private void expect(int token) throws IOException, EvalSpice.ParseException {
            if (tokenizer.ttype != token) {
                throw new EvalSpice.ParseException("Expected token " + (char) token);
            }
            nextToken();
        }

        private int nextToken() throws IOException, EvalSpice.ParseException {
            switch (tokenizer.nextToken()) {
                case '*':
                    op = EvalSpice.Op.MULT;
                    break;
                case '/':
                    op = EvalSpice.Op.DIV;
                    break;
                case '+':
                    op = EvalSpice.Op.PLUS;
                    break;
                case '-':
                    op = EvalSpice.Op.MINUS;
                    break;
                case '<':
                    op = EvalSpice.Op.LT;
                    if (tokenizer.nextToken() == '=') {
                        op = EvalSpice.Op.LTOE;
                    } else {
                        tokenizer.pushBack();
                    }
                    break;
                case '>':
                    op = EvalSpice.Op.GT;
                    if (tokenizer.nextToken() == '=') {
                        op = EvalSpice.Op.GTOE;
                    } else {
                        tokenizer.pushBack();
                    }
                    break;
                case '=':
                    op = EvalSpice.Op.EQ;
                    if (tokenizer.nextToken() != '=') {
                        throw new EvalSpice.ParseException("Expected token ==");
                    }
                    break;
                case '!':
                    op = EvalSpice.Op.NE;
                    if (tokenizer.nextToken() != '=') {
                        throw new EvalSpice.ParseException("Expected token !=");
                    }
                    break;
                case '?':
                    op = EvalSpice.Op.COND;
                    break;
                case StreamTokenizer.TT_EOL:
                    throw new EvalSpice.ParseException("Multiline expression");
                case ':':
                case '(':
                case ')':
                case ',':
                case '@':
                case '"':
                case StreamTokenizer.TT_NUMBER:
                case StreamTokenizer.TT_WORD:
                case StreamTokenizer.TT_EOF:
                    op = null;
                    break;
                default:
                    throw new EvalSpice.ParseException("Illegal character " + (char) tokenizer.ttype);
            }
            return tokenizer.ttype;
        }
    }
}
