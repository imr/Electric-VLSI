/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VarContext.java
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
package com.sun.electric.database.variable;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.tool.generator.layout.LayoutLib;
import java.io.ObjectStreamException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * VarContext represents a hierarchical path of NodeInsts.  Its
 * primary use is to determine the value of variables which contain
 * Java code.  In particular, the syntax @foo expands to P("foo")
 * which looks for the variable called foo in the NodeInst of this
 * VarContext.
 * 
 * <p>A VarContext can also be used to recover the names of instances
 * along a hierarchical path.  LE.getdrive() is an example of a
 * method that does this.
 *
 * <p> The VarContext object that represents the base top level is
 * VarContext.globalContext.  You can get a new VarContext with an extra
 * NodeInst context attached by calling push(NodeInst) on the parent
 * context.  You can get a VarContext with the most recent NodeInst
 * removed by calling pop().  Note that individual VarContexts are
 * immutable:  you get new ones by calling push and pop;  push and pop
 * do not edit their own VarContexts.
 *
 * <p>Retrieve a Variable by calling getVar(String name) on any
 * ElectricObject.
 *
 * <p>If the one knows that the Variable contains an object that
 * does not need to be evaluated, that object can be retrieved using
 * Variable.getObject().
 *
 * <p>On the other hand, if the object may need to be evaluated because
 * it is type Java, TCL, or Lisp, then such evaluation may be hierarchy
 * dependent and one must call context.evalVar(variable).
 * 
 * <p>Extra variables defined in the interpreter:<br>
 * 
 * <p>Extra functions defined in the interpreter:<br>
 *
 * P(name) -- get the value of variable name on the most recent NodeInst.
 * Defaults to Integer(0).<br>
 * <p>
 * Methods PD(), PAR(), and PARD() are either gone or deprecated (RKao). Here 
 * are what they used to do:<br>
 * 
 * PD(name, default) -- get the value of variable name on the most recent
 * NodeInst.  Defaults to default.<br>
 * PAR(name) -- get the value of variable name on any NodeInst, starting
 * with the most recent.  Defaults to Integer(0).<br>
 * PARD(name, default) -- get the value of variable name on any NodeInst,
 * starting with the most recent.  Defaults to default.
 * <P>
 * This class is thread-safe.
 */
public class VarContext implements Serializable
{
    private static class ValueCache {

        private static class EvalPair {
            private final Variable var;
            private final Object info;
            public EvalPair(Variable v, Object i) {var=v;  info=i;}
            public int hashCode() {return var.hashCode() * info.hashCode();}
            public boolean equals(Object o) {
                if (!(o instanceof EvalPair)) return false;
                EvalPair ep = (EvalPair) o;
                return var==ep.var && info==ep.info;
            }
        }

        private final Map<EvalPair,Object> cache = new HashMap<EvalPair,Object>();

        public synchronized boolean containsKey(Variable var, Object info) {
            return cache.containsKey(new EvalPair(var, info));
        }
        public synchronized Object get(Variable var, Object info) {
            return cache.get(new EvalPair(var, info));
        }
        public synchronized void put(Variable var, Object info, Object value) {
            EvalPair key = new EvalPair(var, info);
            LayoutLib.error(cache.containsKey(key), "duplicate keys in ValueCache?");
            cache.put(key, value);
        }
    }
    private static final Object FAST_EVAL_FAILED = new Object();

    private final VarContext prev;
    private final Nodable ni;
    private transient ValueCache cache;

    // ------------------------ private methods -------------------------------
    // For the global context.
    private VarContext()
    {
        this.ni = null;
        this.prev = this;
        this.cache = null;
    }

    private VarContext(Nodable ni, VarContext prev, boolean caching)
    {
        this.ni = ni;
        this.prev = prev;
        this.cache = caching ? new ValueCache() : null;
    }

    private Object readResolve() throws ObjectStreamException {
        return prev != this ? this : globalContext;
    }

    private void throwNotFound(String name) throws EvalException {
        throw new EvalException(name.replaceFirst("ATTR_", "")+" not found");
    }

    private Object ifNotNumberTryToConvertToNumber(Object val) {
        if (val == null) return val;
        if (val instanceof Number) return val;
        try {
            Number d = TextUtils.parsePostFixNumber(val.toString());
            val = d;
        } catch (java.lang.NumberFormatException e) {
            // just return original val object
        }
        return val;
    }

    /** If expression is a simple variable reference then return the name of
     * the variable; otherwise return null */
    private String getSimpleVarRef(String expr) {
        final String pOpen = "P(\"";
        final int pOpenLen = pOpen.length();
        final String pClose = "\")";
        final int pCloseLen = pClose.length();
        if (expr.startsWith(pOpen) && expr.endsWith(pClose)) {
            String varNm = expr.substring(pOpenLen, expr.length()-pCloseLen);
            return isValidIdentifier(varNm) ? varNm : null;
        }
        if (expr.startsWith("@")) {
            String varNm = expr.substring(1);
            return isValidIdentifier(varNm) ? varNm : null;
        }
        return null;
    }

    private boolean isValidIdentifier(String identifier) {
        final int len = identifier.length();
        for (int i=0; i<len; i++) {
            if (!TextUtils.isLetterOrDigit(identifier.charAt(i))) return false;
        }
        return true;
    }

    private Object fastJavaVarEval(Variable var, Object info) throws EvalException {
        // Avoid re-computing the value if it is already in the cache.
        // Use "contains" because the value might be null.
        synchronized(this) {
            if (cache!=null && cache.containsKey(var, info)) {
                return cache.get(var, info);
            }
        }
        // Avoid calling bean shell if value is just a reference to another
        // variable.
        String expr = var.getObject().toString();
        String varNm = getSimpleVarRef(expr);
        if (varNm==null) return FAST_EVAL_FAILED;
        return lookupVarEval("ATTR_"+varNm);
    }

    private void printValueCheckValue(Object value, Object checkValue) {
        System.out.println("fast eval mismatch");
        System.out.println("  fast value: "+value.toString());
        System.out.println("  slow value: "+checkValue.toString());
    }

    private void checkFastValue(Object value, Variable var, Object info) throws EvalException {
        if (value==FAST_EVAL_FAILED) return;
        Object checkValue = EvalJavaBsh.evalJavaBsh.evalVarObject(var.getObject(), this, info);
        if (value==null) {
            LayoutLib.error(value!=checkValue, "fast eval null mismatch");
        } else {
            if (!value.equals(checkValue)) {
                System.out.println("fast eval mismatch");
                printValueCheckValue(value, checkValue);
                LayoutLib.error(true, "fast eval mismatch");
            }
        }
    }

    // ----------------------------- public methods ---------------------------
    /**
     * The blank VarContext that is the parent of all VarContext chains.
     */
    public static final VarContext globalContext = new VarContext();

    /**
     * get a new VarContext that consists of the current VarContext with
     * the given NodeInst pushed onto the stack
     */
    public VarContext push(Nodable ni)
    {
        return new VarContext(ni, this, false);
    }

    /**
     * Push a new level onto the VarContext stack. If the value of any of
     * ni's parameters is requested and if the computation of that value
     * requires a call to the bean shell then the value is saved in this
     * VarContext so that future requests don't result in additional calls
     * to the bean shell.  Note that this is implementing Call-By-Value
     * semantics whereas the non-caching VarContext implements Call-By-Name.
     * <p>
     * Be warned that there is no mechanism to automatically flush the caches
     * when the design is modified. The design MUST NOT change over the
     * life time of this VarContext. If the design might change then you should
     * use the non-caching VarContext.
     */
    public VarContext pushCaching(Nodable ni) {
        return new VarContext(ni, this, true);
    }

    /**
     * get the VarContext that existed before you called push on it.
     * may return globalContext if the stack is empty.
     */
    public VarContext pop()
    {
        return prev;
    }

    /**
     * Return the Node Instance that provides the context for the
     * variable evaluation for this level.
     */
    public Nodable getNodable()
    {
        return ni;
    }

    /**
     * Return the PortInst that resides on the NodeInst that provides
     * the context. This is currently only useful for Highlighting.
     */
    public PortInst getPortInst()
    {
        return null;
    }

    /**
     * Does deep comparison of two VarContexts.  Matches
     * hierarchy traversal.  Returns true if they both represent
     * the same hierarchy traversal, false otherwise. (Recursive method).
     * Does not compare PortInsts.
     * @param c the VarContext to compare against.
     * @return true if equal, false otherwise.
     */
    public boolean equals(VarContext c)
    {
        if (this == c) return true;             // both globalContext, or the same object

        // the following line doesn't work (R KAO, IvanM)
        //if (ni != c.getNodable()) return false; // compare nodeinsts

        if (ni == null || c.getNodable() == null) return ni == c.getNodable();
        Cell c1 = ni.getParent();
        Cell c2 = c.getNodable().getParent();
        String name1 = ni.getName();
        String name2 = c.getNodable().getName();

        if (! ((c1 == c2) && (name1.equals(name2)))) return false;
        return prev.equals(c.pop());            // compare parents
    }

    /**
     * Get an iterator over the Nodables that describe this context.
     * This iterator starts from the top of the hierarchy, and goes down.
     * @return an iterator over the context path
     */
    public Iterator<Nodable> getPathIterator() {
        Stack<Nodable> stack = new Stack<Nodable>();
        VarContext context = this;
        while (context != VarContext.globalContext) {
            Nodable no = context.getNodable();
            stack.push(no);
            context = context.pop();
        }
        List<Nodable> path = new ArrayList<Nodable>();
        while (!stack.isEmpty())
            path.add(stack.pop());
        return path.iterator();
    }

    /**
     * Remove N levels of parent context from this VarContext. Returns
     * a new VarContext.  This will return VarContext.globalContext
     * if the 'levels' is greater than or equal to the number of levels
     * in this context.
     * @param levels the number of levels of parent context to remove
     * @return a new VarContext
     */
    public VarContext removeParentContext(int levels) {
        Stack<Nodable> nodes = new Stack<Nodable>();
        VarContext acontext = this;
        while (acontext != VarContext.globalContext) {
            nodes.push(acontext.getNodable());
            acontext = acontext.pop();
        }
        for (int i=0; i<levels; i++)
            nodes.pop();
        acontext = VarContext.globalContext;
        int size = nodes.size();
        for (int i=0; i<size; i++) {
            Nodable no = (Nodable)nodes.pop();
            acontext = acontext.push(no);
        }
        return acontext;
    }

    /**
     * Get the number of levels of context in this VarContext
     * @return the number of levels of context in this VarContext
     */
    public int getNumLevels() {
        int i=0;
        VarContext acontext = this;
        while (acontext != VarContext.globalContext) {
            i++;
            acontext = acontext.pop();
        }
        return i;
    }

    /** Get rid of the variable cache thereby release its storage */
    public synchronized void deleteVariableCache() { cache=null; }

    // ------------------------------ Variable Evaluation -----------------------

    /**
     * Gets the value of Variable @param var.
     * If variable is Java, uses EvalJavaBsh to evaluate
     * If variable is TCL, uses ... to evaluate
     * If variable is Lisp, uses ... to evaluate
     * otherwise, just returns the Variable's object
     * @return the evlauated Object. Returns null if the variable
     * is code and evaluation fails.
     */
    public Object evalVar(Variable var)
    {
        return evalVar(var, null);
    }

    /** Same as evalVar, except an additional object 'info'
     * is passed to the evaluator.  'info' may be or contain 
     * additional information necessary for proper evaluation.
     * Usually info is the NodeInst on which the var exists.
     * @return the evlauated Object. Returns null if the variable
     * is code and evaluation fails.
     */
    public Object evalVar(Variable var, Object info)
    {
        if (var == null) return null;
        try {
            return evalVarRecurse(var, info);
        } catch (EvalException e) {
            return null;
        }
    }

    public static class EvalException extends Exception {
        public EvalException() { super(); }
        public EvalException(String message) { super(message); }
        public EvalException(String message, Throwable cause) { super(message, cause); }
        public EvalException(Throwable cause) { super(cause); }
    }

    /**
     * This is the recursive version of evalVar that may throw an EvalException.
     * The message of the EvalException states the reason that evaluation failed.
     * This is made public so code elsewhere that is meant to be used in Attribute's java code
     * can call this method.  This is useful such that the first EvalException encountered is
     * thrown all the way to the top of the eval caller, rather than getting caught and handled
     * somewhere inbetween.
     * @param var the variable to evaluate
     * @param info an info object that may be needed by the evaluator
     * @return the variable's object if not code, otherwise an evaluated result object if
     * evaluation succeeds
     * @throws EvalException an exception whose message contains the reason evaluation failed
     */
    public Object evalVarRecurse(Variable var, Object info) throws EvalException {
        TextDescriptor.Code code = var.getCode();
        Object value = var.getObject();

        if (code == TextDescriptor.Code.JAVA) {
            value = fastJavaVarEval(var, info);

            // testing code
            //checkFastValue(value, var, info);

            if (value==FAST_EVAL_FAILED) {
                // OK, I give up.  Call the darn bean shell.
                value = EvalJavaBsh.evalJavaBsh.evalVarObject(var.getObject(),
                                                              this, info);
                synchronized(this) {
                    if (cache!=null) cache.put(var, info, value);
                }
            }
        }
        // TODO: if(code == Variable.Code.TCL) { }
        // TODO: if(code == Variable.Code.LISP) { }
        if (code == TextDescriptor.Code.SPICE) {
            return evalSpice_(var, true);
        }
        value = ifNotNumberTryToConvertToNumber(value);
        return value;
    }

    /**
     * Evaluate a spice variable
     * @param var the variable to evaluate
     * @param recurse false to not recursively evaluate (only if param call is SPICE code).
     * @return the evaluated string, or null on eval error
     */
    public Object evalSpice(Variable var, boolean recurse) {
        //if (var.getCode() != TextDescriptor.Code.SPICE) return null;
        try {
            return evalSpice_(var, recurse);
        } catch (EvalException e) {
            return null;
        }
    }

    /** For replacing @variable */ private static final Pattern pPat = Pattern.compile("P\\(\"(\\w+)\"\\)");
    private Object evalSpice_(Variable var, boolean recurse) throws EvalException {
        Object obj = var.getObject();
        if (obj instanceof String[]) {
            // concatentate arrayed strings
            String[] strArray = (String[])obj;
            StringBuffer buf = new StringBuffer();
            for (int i=0; i<strArray.length; i++) {
                buf.append(strArray[i]);
                buf.append(" ");
            }
            obj = buf;
        }

        String expr = EvalJavaBsh.replace(obj.toString());
        Matcher pMat = pPat.matcher(expr);
        StringBuffer sb = new StringBuffer();
        while (pMat.find()) {
            Object value = pMat.group(1).substring(5);
            Variable parentVar = null;
            Nodable no = getNodable();
            if (no != null)
                parentVar = no.getVar(Variable.findKey(pMat.group(1)));
            if (parentVar != null) {
                // see if param is spice code by looking at instance code, and definition code
                boolean isSpiceCode = parentVar.getCode() == TextDescriptor.Code.SPICE;
                if (no.isCellInstance()) {
                    Cell c = (Cell)no.getProto();
                    if (c.contentsView() != null) c = c.contentsView();
                    Variable protoVar = c.getVar(parentVar.getKey());
                    if (protoVar != null)
                        isSpiceCode = protoVar.getCode() == TextDescriptor.Code.SPICE;
                }
                if (recurse || !isSpiceCode)
                    value = pop().evalVarRecurse(parentVar, getNodable());
            }
            pMat.appendReplacement(sb, value.toString());
        }
        pMat.appendTail(sb);
        EvalSpice sp = new EvalSpice(sb.toString());
        return sp.evaluate();
    }

    /**
     * Lookup Variable one level up the hierarchy and evaluate. 
     * Looks for the var on the most recent NodeInst on the
     * hierarchy stack.  If not found, look for the default
     * Variable of the same name on the NodeProto.
     * @param name the name of the variable
     * @return an object representing the evaluated variable,
     * or null if no var or default var found.
     */
    protected Object lookupVarEval(String name) throws EvalException
    {
        if (ni == null) throwNotFound(name);
        Variable.Key key = Variable.findKey(name);
        if (key == null) throwNotFound(name);

        // Topology.java concludes that all instances of a Cell have
        // the same transistor sizes if that Cell has no parameters.
        // Let's see if we can enforce these semantics. A Variable has
        // a value that is visible in a Cell only if that Variable is
        // is a "parameter".
        // RKao
        Variable var = ni.getParameter(key);

//        Variable var = ni.getVar(key);
//        
//        if (var == null && ni.isCellInstance()) {
//            // look up default var on prototype
//			Cell cell = (Cell)ni.getProto();
//			Cell equiv = cell.getEquivalent();
//			if (equiv != null) cell = equiv;
//			var = cell.getVar(key);
//        }

        if (var == null) throwNotFound(name);

        // evaluate var in it's context
        Object val = this.pop().evalVarRecurse(var, ni);
        if (val == null) throwNotFound(name);

        val = ifNotNumberTryToConvertToNumber(val);
        if (val == null) throwNotFound(name);

        return val;
    }

    // lookupVarFarEval() is deprecated. Topology.java concludes that all instances
    // of a Cell have the same transistor sizes if that Cell has no parameters.
    // lookupVarFarEval() violates this assumption. Luckily we believe that no
    // designs use PAR() or lookupVarFarEval(). I'm commenting out this method to
    // guarantee that Topology.java always works correctly. RKao
//    /** 
//     * Lookup Variable on all levels up the hierarchy and evaluate.
//     * Looks for var on all NodeInsts on the stack, starting
//     * with the most recent.  At each NodeInst, if no Variable
//     * found, looks for default Variable on NodeProto.
//     * @param name the name of the variable
//     * @return evaluated Object, or null if not found
//     */
//    protected Object lookupVarFarEval(String name) throws EvalException
//    {
//		Variable.Key key = Variable.findKey(name);
//		if (key == null) throwNotFound(name);
//        
//		// look up the entire stack, starting with end
//		VarContext scan = this;
//        Object value = null;
//        while (true)
//		{
//            Nodable sni = scan.getNodable();
//            if (sni == null) break;
//            
//            Variable var = sni.getVar(key);             // look up var
//			if (var != null) {
//				value = scan.pop().evalVarRecurse(var, sni);
//				break;
//			}
//
//			// look up default var value on prototype
//			if (sni.isCellInstance()) {
//				Cell cell = (Cell)sni.getProto();
//				Cell equiv = cell.getEquivalent();
//				if (equiv != null) cell = equiv;
//				var = cell.getVar(key);
//			}
//            if (var != null) {
//            	value = scan.pop().evalVarRecurse(var, sni);
//            	break;
//            }
//			scan = scan.prev;
//		}
//        if (value == null) throwNotFound(name);
//        
//        value = ifNotNumberTryToConvertToNumber(value);
//        if (value == null) throwNotFound(name);
//        
//        return value;
//	}

    // ---------------------------------- Utility Methods ----------------------------

    /** Return the concatonation of all instances names left to right
     * from the root to the leaf. Begin with the string with a separator
     * and place a separator between adjacent instance names.
     * @param sep the separator string.
     */
    public String getInstPath(String sep)
    {
        if (this==globalContext) return "";

        String prefix = pop()==globalContext ? "" : pop().getInstPath(sep);
        Nodable no = getNodable();
        if (no==null) {
            System.out.println("VarContext.getInstPath: context with null NodeInst?");
        }
        if (no instanceof NodeInst) {
            // nodeInst, we want netlisted name, assume zero index of arrayed node
            //no = Netlist.getNodableFor((NodeInst)no, 0);
            Nodable no2 = Netlist.getNodableFor((NodeInst)no, 0);
            if (no2 != null) no = no2;
        }
        String me = no.getName();
//         if (me==null) {
//             //System.out.println("VarContext.getInstPath: NodeInst in VarContext with no name!!!");
//             me = ni.describe();
//         }
        if (prefix.equals("")) return me;
        return prefix + sep + me;
    }

    /** Helper method to convert an Object to a float, if possible.
     * if not possible, return @param def.
     */
    public static float objectToFloat(Object obj, float def) {
        if (obj == null) return def;
        if (obj instanceof Number) return ((Number)obj).floatValue();
        try {
            Number n = TextUtils.parsePostFixNumber(obj.toString());
            return n.floatValue();
        } catch (NumberFormatException e) {}
        return def;
    }

    /** Helper method to convert an Object to an integer, if possible.
     * if not possible, return @param def.
     */
    public static int objectToInt(Object obj, int def) {
        if (obj == null) return def;
        if (obj instanceof Number) return ((Number)obj).intValue();
        try {
            Number n = TextUtils.parsePostFixNumber(obj.toString());
            return n.intValue();
        } catch (NumberFormatException e) {}
        return def;
    }

    /** Helper method to convert an Object to a short, if possible.
     * if not possible, return @param def.
     */
    public static short objectToShort(Object obj, short def) {
        if (obj == null) return def;
        if (obj instanceof Number) return ((Number)obj).shortValue();
        try {
            Number n = TextUtils.parsePostFixNumber(obj.toString());
            return n.shortValue();
        } catch (NumberFormatException e) {}
        return def;
    }

   /** Helper method to convert an Object to a double, if possible.
     * if not possible, return @param def.
     */
    public static double objectToDouble(Object obj, double def) {
        if (obj == null) return def;
        if (obj instanceof Number) return ((Number)obj).doubleValue();
        try {
            Number n = TextUtils.parsePostFixNumber(obj.toString());
            return n.doubleValue();
        } catch (NumberFormatException e) {}
        return def;
    }

}
