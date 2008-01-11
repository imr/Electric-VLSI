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

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author dn146861
 */
public class CodeExpression {
    private static final HashMap <CodeExpression,CodeExpression> allExpressions = new HashMap<CodeExpression,CodeExpression>();
    private static long constructorCalls;
    /** For replacing @variable */ private static final Pattern pPat = Pattern.compile("P\\(\"(\\w+)\"\\)");
    
    private final TextDescriptor.Code code;
    private final String expr;
    private final Set depends;

    private CodeExpression(TextDescriptor.Code code, String expr) {
        if (code == null || expr == null)
            throw new NullPointerException();
        if (code == TextDescriptor.Code.NONE)
            throw new IllegalArgumentException();
        this.code = code;
        this.expr = expr;
        TreeSet<Variable.Key> varKeys = new TreeSet<Variable.Key>();
        Matcher pMat = pPat.matcher(EvalJavaBsh.replace(expr));
        while (pMat.find()) {
            String varName = pMat.group(1);
            varKeys.add(Variable.newKey(varName));
        }
        depends = Collections.unmodifiableSet(varKeys);
        constructorCalls++;
    }
    
    public static synchronized CodeExpression valueOf(TextDescriptor.Code code, String expression) {
        CodeExpression newCE = new CodeExpression(code, expression);
        CodeExpression oldCE = allExpressions.get(newCE);
        if (oldCE != null) return oldCE;
        allExpressions.put(newCE, newCE);
        return newCE;
    }
    
    public TextDescriptor.Code getCode() { return code; }
    public String getExpr() { return expr; }
    public Set<Variable.Key> dependsOn() { return depends; }
    
    @Override
    public int hashCode() {
        return expr.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof CodeExpression) {
            CodeExpression that = (CodeExpression)o;
            return this.code == that.code && this.expr.equals(that.expr);
        }
        return false;
    }
    
    @Override
    public String toString() { return expr; }
    
    void check() {
        assert expr != null;
        assert code != null && code != TextDescriptor.Code.NONE;
    }

    /**
     * Print statistics about CodeExpressions.
     * @param verbose print all CodeExpressions
     */
    public static void printStatistics(boolean verbose) {
        TreeMap<String,CodeExpression> javaStrings = new TreeMap<String,CodeExpression>();
        TreeMap<String,CodeExpression> codeStrings = new TreeMap<String,CodeExpression>();
        for (CodeExpression ce: allExpressions.values()) {
            TreeMap<String,CodeExpression> strings = ce.getCode() == TextDescriptor.Code.JAVA ?  javaStrings : codeStrings;
            strings.put(ce.getExpr(), ce);
        }
        
        if (!verbose) return;
        System.out.println(allExpressions.size() + " CodeExpressions after " + constructorCalls + " valueOf calls");
        System.out.println(javaStrings.size() + " java strings");
        for (CodeExpression ce: javaStrings.values())
            printCE(ce);
        System.out.println(codeStrings.size() + " code strings");
        for (CodeExpression ce: javaStrings.values())
            printCE(ce);
    }
    
    private static void printCE(CodeExpression ce) {
        System.out.print("\"" + ce.getExpr() + "\"\t");
        for (Variable.Key varKey: ce.dependsOn())
            System.out.print(" " + varKey);
        System.out.println();
    }
}
