/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NewEpicAnalysis.java
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.AnalogAnalysis;
import com.sun.electric.tool.simulation.AnalogSignal;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.simulation.Waveform;
import com.sun.electric.tool.simulation.WaveformImpl;
import com.sun.electric.tool.user.ActivityLogger;

import com.sun.electric.tool.simulation.*;
import com.sun.electric.database.geometry.btree.*;
import com.sun.electric.database.geometry.btree.unboxed.*;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * Class to define a set of simulation data producet by Epic
 * simulators, using the new disk-indexed format.

 * This class differs from Anylisis base class by less memory consumption.
 * Waveforms are stored in a file in packed form. They are loaded to memory by
 * denand. Hierarchical structure is reconstructed from Epic flat names into Context objects.
 * EpicSignals don't store signalContex strings, NewEpicAnalysis don't have signalNames hash map.
 * Elements of Context are EpicTreeNodes. They partially implements interface javax.swing.tree.TreeNode .
 */
public class NewEpicAnalysis extends AnalogAnalysis {
    
    /** Separator in Epic signal names. */              static final char separator = '.';
    
    /** Type of voltage signal. */                      static final byte VOLTAGE_TYPE = 1;
    /** Type of current signal. */                      static final byte CURRENT_TYPE = 2;
    
    /** Fake context which denotes voltage signal. */   static final Context VOLTAGE_CONTEXT = new Context(VOLTAGE_TYPE);
    /** Fake context which denotes current signal. */   static final Context CURRENT_CONTEXT = new Context(CURRENT_TYPE);
    
    /** File name of File with waveforms. */            private File waveFileName;
    /** Opened file with waveforms. */                  private RandomAccessFile waveFile;
    /** Offsets of packed waveforms in the file. */     int[] waveStarts;
    /** Lengths of packed waveforms in the file. */     int[] waveLengths;
   
    /** Time resolution of integer time unit. */        private double timeResolution;
    /** Voltage resolution of integer voltage unit. */  private double voltageResolution;
    /** Current resolution of integer current unit. */  private double currentResolution;
    /** Simulation time. */                             private double maxTime;
    
    /** Unmodifieable view of signals list. */          private List<AnalogSignal> signalsUnmodifiable;
    /** a set of voltage signals. */                    private BitSet voltageSignals = new BitSet(); 
    /** Top-most context. */                            private Context rootContext;
    /** Hash of all contexts. */                        private Context[] contextHash = new Context[1];
    /** Count of contexts in the hash. */               private int numContexts = 0;

    /**
     * Package-private constructor.
     * @param sd Stimuli.
     */
    NewEpicAnalysis(Stimuli sd) {
        super(sd, AnalogAnalysis.ANALYSIS_TRANS, false);
        signalsUnmodifiable = Collections.unmodifiableList(super.getSignals());
    }
    
    /**
     * Set time resolution of this NewEpicAnalysis.
     * @param timeResolution time resolution in nanoseconds.
     */
    void setTimeResolution(double timeResolution) { this.timeResolution = timeResolution; }
    
    /**
     * Set voltage resolution of this EpicAnalysys.
     * @param voltageResolution voltage resolution in volts.
     */
    void setVoltageResolution(double voltageResolution) { this.voltageResolution = voltageResolution; }
    
    /**
     * Set current resolution of this EpicAnalysys.
     * @param currentResolution in amperes ( milliamperes? ).
     */
    void setCurrentResolution(double currentResolution) { this.currentResolution = currentResolution; }
    
    double getValueResolution(int signalIndex) {
        return voltageSignals.get(signalIndex) ? voltageResolution : currentResolution;
    }
    
    /**
     * Set simulation time of this NewEpicAnalysis.
     * @param maxTime simulation time in nanoseconds.
     */
    void setMaxTime(double maxTime) { this.maxTime = maxTime; }
    
    /**
     * Set root context of this EpicAnalysys.
     * @param context root context.
     */
    void setRootContext(Context context) { rootContext = context; }
    
    /**
     * Set waveform file of this NewEpicAnalysis.
     * @param waveFileName File object with name of waveform file.
     */
    void setWaveFile(File waveFileName) throws FileNotFoundException {
        this.waveFileName = waveFileName;
        waveFileName.deleteOnExit();
        waveFile = new RandomAccessFile(waveFileName, "r");
    }
    
    /**
     * Free allocated resources before closing.
     */
    @Override
        public void finished() {
        try {
            waveFile.close();
            waveFileName.delete();
        } catch (IOException e) {
        }
    }
    
	/**
	 * Method to quickly return the signal that corresponds to a given Network name.
     * This method overrides the m,ethod from Analysis class.
     * It doesn't use signalNames hash map.
	 * @param netName the Network name to find.
	 * @return the Signal that corresponds with the Network.
	 * Returns null if none can be found.
	 */
    @Override
        public AnalogSignal findSignalForNetworkQuickly(String netName) {
        AnalogSignal old = super.findSignalForNetworkQuickly(netName);
        
        String lookupName = TextUtils.canonicString(netName);
        int index = searchName(lookupName);
        if (index < 0) {
            assert old == null;
            return null;
        }
        AnalogSignal sig = signalsUnmodifiable.get(index);
        return sig;
    }

    public List<AnalogSignal> getSignalsFromExtractedNet(Signal ws) {
        ArrayList<AnalogSignal> ret = new ArrayList<AnalogSignal>();
        ret.add((AnalogSignal)ws);
        return ret;
    }


    /**
     * Public method to build tree of EpicTreeNodes.
     * Root of the tree us EpicRootTreeNode objects.
     * Deeper nodes are EpicTreeNode objects.
     * @param analysis name of root DefaultMutableTreeNode.
     * @return root EpicRootTreeNode of the tree.
     */
	public DefaultMutableTreeNode getSignalsForExplorer(String analysis) {
		DefaultMutableTreeNode signalsExplorerTree = new EpicRootTreeNode(this, analysis);
        return signalsExplorerTree;
    }

   
    /**
     * Returns EpicSignal by its TreePath.
     * @param treePath specified TreePath.
     * @return EpicSignal or null.
     */
    public static EpicSignal getSignal(TreePath treePath) {
        Object[] path = treePath.getPath();
        int i = 0;
        while (i < path.length && !(path[i] instanceof EpicRootTreeNode))
            i++;
        if (i >= path.length) return null;
        NewEpicAnalysis an = ((EpicRootTreeNode)path[i]).an;
        i++;
        int index = 0;
        for (; i < path.length; i++) {
            EpicTreeNode tn = (EpicTreeNode)path[i];
            index += tn.nodeOffset;
            if (tn.isLeaf())
                return (EpicSignal)an.signalsUnmodifiable.get(index);
        }
        return null;
    }
    
	/**
	 * Method to get the list of signals in this Simulation Data object.
     * List is unmodifieable.
	 * @return a List of signals.
	 */
    @Override
        public List<AnalogSignal> getSignals() { return signalsUnmodifiable; }

    /**
     * This methods overrides Analysis.nameSignal.
     * It doesn't use Analisys.signalNames to use less memory.
     */
    @Override
        public void nameSignal(AnalogSignal ws, String sigName) {}
    
    /**
     * Finds signal index of AnalogSignal with given full name.
     * @param name full name.
     * @return signal index of AnalogSignal in unmodifiable list of signals or -1.
     */
    int searchName(String name) {
        Context context = rootContext;
        for (int pos = 0, index = 0;;) { 
            int indexOfSep = name.indexOf(separator, pos);
            if (indexOfSep < 0) {
                EpicTreeNode sig = context.sigs.get(name.substring(pos));
                return sig != null ? index + sig.nodeOffset : -1;
            }
            EpicTreeNode sub = context.subs.get(name.substring(pos, indexOfSep));
            if (sub == null) return -1;
            context = sub.context;
            pos = indexOfSep + 1;
            index += sub.nodeOffset;
        }
    }
        
    /**
     * Makes fullName or signalContext of signal with specified index.
     * @param index signal index.
     * @param full true to request full name.
     */
    String makeName(int index, boolean full) {
        StringBuilder sb = new StringBuilder();
        Context context = rootContext;
        if (context == null) return null;
        if (index < 0 || index >= context.treeSize)
            throw new IndexOutOfBoundsException();
        for (;;) {
            int localIndex = context.localSearch(index);
            EpicTreeNode tn = context.nodes[localIndex];
            Context subContext = tn.context;
            if (subContext.isLeaf()) {
                if (full)
                    sb.append(tn.name);
                else if (sb.length() == 0)
                    return null;
                else
                    sb.setLength(sb.length() - 1);
                return sb.toString();
            }
            sb.append(tn.name);
            sb.append(separator);
            index -= tn.nodeOffset;
            context = subContext;
        }
    }
    
    /**
     * Method to get Fake context of AnalogSignal.
     * @param byte physical type of AnalogSignal.
     * @return Fake context.
     */ 
    static Context getContext(byte type) {
        switch (type) {
            case VOLTAGE_TYPE:
                return VOLTAGE_CONTEXT;
            case CURRENT_TYPE:
                return CURRENT_CONTEXT;
        }
        throw new IllegalArgumentException();
    }

    /**
     * Method to get Context by list of names and list of contexts.
     * @param strings list of names.
     * @param contexts list of contexts.
     */
    Context getContext(ArrayList<String> strings, ArrayList<Context> contexts) {
        assert strings.size() == contexts.size();
        int hashCode = Context.hashValue(strings, contexts);
        
        int i = hashCode & 0x7FFFFFFF;
        i %= contextHash.length;
        for (int j = 1; contextHash[i] != null; j += 2) {
            Context c = contextHash[i];
            if (c.hashCode == hashCode && c.equals(strings, contexts)) return c;
            
            i += j;
            if (i >= contextHash.length) i -= contextHash.length;
        }
        
        // Need to create new context.
        if (numContexts*2 <= contextHash.length - 3) {
            // create a new CellUsage, if enough space in the hash
            Context c = new Context(strings, contexts, hashCode);
            contextHash[i] = c;
            numContexts++;
            return c;
        }
        // enlarge hash if not
        rehash();
        return getContext(strings, contexts);
    }
    
    /**
     * Rehash the context hash.
     * @throws IndexOutOfBoundsException on hash overflow.
     */
    private void rehash() {
        int newSize = numContexts*2 + 3;
        if (newSize < 0) throw new IndexOutOfBoundsException();
        Context[] newHash = new Context[GenMath.primeSince(newSize)];
        for (int k = 0; k < contextHash.length; k++) {
            Context c = contextHash[k];
            if (c == null) continue;
            int i = c.hashCode & 0x7FFFFFFF;
            i %= newHash.length;
            for (int j = 1; newHash[i] != null; j += 2) {
                i += j;
                if (i >= newHash.length) i -= newHash.length;
            }
            newHash[i] = c;
        }
        contextHash = newHash;
    }

    private static CachingPageStorage ps = null;
    public static BTree<Double,Double,Serializable> getTree() {
        if (ps==null)
            try {
                long highWaterMarkInBytes = 50 * 1024 * 1024;
                PageStorage fps = FilePageStorage.create();
                PageStorage ops = new OverflowPageStorage(new MemoryPageStorage(fps.getPageSize()), fps, highWaterMarkInBytes);
                ps = new CachingPageStorageWrapper(ops, 16 * 1024, false);
                //ps = new MemoryPageStorage(256);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        return new BTree<Double,Double,Serializable>
            (ps, UnboxedHalfDouble.instance, UnboxedHalfDouble.instance, null, null);
    }

    HashMap<Integer, Waveform> loadWaveformCache =
        new HashMap<Integer, Waveform>();

    @Override
        protected Waveform[] loadWaveforms(AnalogSignal signal) {
        int index = ((EpicSignal)signal).sigNum;
        Waveform wave = loadWaveformCache.get(new Integer(index));
        if (wave == null)
            return new Waveform[] { new WaveformImpl(new double[0], new double[0]) };
        return new Waveform[] { wave };
    }

    void putWaveform(int signum, Waveform w) {
        loadWaveformCache.put(new Integer(signum), w);
    }
    


    
    /************************* EpicRootTreeNode *********************************************
     * This class is for root node of EpicTreeNode tree.
     * It contains children from rootContext of specified NewEpicAnalysis.
     */
    private static class EpicRootTreeNode extends DefaultMutableTreeNode {
        /** NewEpicAnalysis which owns the tree. */    private NewEpicAnalysis an;
        
        /**
         * Private constructor.
         * @param an specified EpicAnalysy.
         * @paran name name of bwq EpicRootTreeNode
         */
        private EpicRootTreeNode(NewEpicAnalysis an, String name) {
            super(name);
            this.an = an;
            Vector<EpicTreeNode> children = new Vector<EpicTreeNode>();

            //    		// make a list of signal names with "#" in them
            //    		Map<String,DefaultMutableTreeNode> sharpMap = new HashMap<String,DefaultMutableTreeNode>();
            //            for (EpicTreeNode tn: an.rootContext.sortedNodes)
            //    		{
            //    			String sigName = tn.name;
            //    			int hashPos = sigName.indexOf('#');
            //    			if (hashPos <= 0) continue;
            //    			String sharpName = sigName.substring(0, hashPos);
            //    			DefaultMutableTreeNode parent = sharpMap.get(sharpName);
            //    			if (parent == null)
            //    			{
            //    				parent = new DefaultMutableTreeNode(sharpName + "#");
            //    				sharpMap.put(sharpName, parent);
            //    				this.add(parent);
            //    			}
            //    		}
            //
            //    		// add all signals to the tree
            //            for (EpicTreeNode tn: an.rootContext.sortedNodes)
            //    		{
            //    			String nodeName = null;
            //    			String sigName = tn.name;
            //    			int hashPos = sigName.indexOf('#');
            //    			if (hashPos > 0)
            //    			{
            //    				// force a branch with the proper name
            //    				nodeName = sigName.substring(0, hashPos);
            //    			} else
            //    			{
            //    				// if this is the pure name of a hash set, force a branch
            //    				String pureSharpName = sigName;
            //    				if (sharpMap.get(pureSharpName) != null)
            //    					nodeName = pureSharpName;
            //    			}
            //    			if (nodeName != null)
            //    			{
            //    				DefaultMutableTreeNode parent = sharpMap.get(nodeName);
            //    				parent.add(new DefaultMutableTreeNode(tn));
            //    			} else
            //    			{
            //                    children.add(tn);
            //    			}
            //    		}

            for (EpicTreeNode tn: an.rootContext.sortedNodes)
                children.add(tn);
            this.children = children;
        }

        /**
         * Returns the index of the specified child in this node's child array.
         * If the specified node is not a child of this node, returns
         * <code>-1</code>.  This method performs a linear search and is O(n)
         * where n is the number of children.
         *
         * @param	aChild	the TreeNode to search for among this node's children
         * @exception	IllegalArgumentException	if <code>aChild</code>
         *							is null
         * @return	an int giving the index of the node in this node's child
         *          array, or <code>-1</code> if the specified node is a not
         *          a child of this node
         */
        public int getIndex(TreeNode aChild) {
            try {
                EpicTreeNode tn = (EpicTreeNode)aChild;
                if (getChildAt(tn.sortedIndex) == tn)
                    return tn.sortedIndex;
            } catch (Exception e) {
                if (aChild == null)
                    throw new IllegalArgumentException("argument is null");
            }
            return -1;
        }
    }
    
    /************************* Context *********************************************
     * This class denotes a group of siignals. It may have a few instance in signal tree.
     */
    static class Context {
        /**
         * Type of context.
         * it is nonzero for leaf context and it is zero for non-leaf contexts.
         */
        private final byte type;
        /**
         * Hash code of this Context
         */
        private final int hashCode;
        /**
         * Nodes of this Context in chronological order.
         */
        private final EpicTreeNode[] nodes;
        /**
         * Nodes of this Context sorted by type and name.
         */
        private final EpicTreeNode[] sortedNodes;
        /**
         * Flat number of signals in tree whose root is this context.
         */
        private final int treeSize;
        
        /**
         * Map from name of subcontext to EpicTreeNode.
         */
        private final HashMap<String,EpicTreeNode> subs = new HashMap<String,EpicTreeNode>();
        /**
         * Map from name of leaf npde to EpicTreeNode.
         */
        private final HashMap<String,EpicTreeNode> sigs = new HashMap<String,EpicTreeNode>();
 
        /**
         * Constructor of leaf fake context.
         */
        private Context(byte type) {
            assert type != 0;
            this.type = type;
            assert isLeaf();
            hashCode = type;
            nodes = null;
            sortedNodes = null;
            treeSize = 1;
        }
        
        /**
         * Constructor of real context.
         * @param names list of names.
         * @param contexts list of contexts.
         * @param hashCode precalculated hash code of new Context.
         */
        private Context(ArrayList<String> names, ArrayList<Context> contexts, int hashCode) {
            assert names.size() == contexts.size();
            type = 0;
            this.hashCode = hashCode;
            nodes = new EpicTreeNode[names.size()];
            int treeSize = 0;
            for (int i = 0; i < nodes.length; i++) {
                String name = names.get(i);
                Context subContext = contexts.get(i);
                EpicTreeNode tn = new EpicTreeNode(i, name, subContext,treeSize);
                nodes[i] = tn;
                String canonicName = TextUtils.canonicString(name);
                if (subContext.isLeaf())
                    sigs.put(canonicName, tn);
                else
                    subs.put(canonicName, tn);
                treeSize += subContext.treeSize;
            }
            sortedNodes = nodes.clone();
            Arrays.sort(sortedNodes, TREE_NODE_ORDER);
            for (int i = 0; i < sortedNodes.length; i++)
                sortedNodes[i].sortedIndex = i;
            this.treeSize = treeSize;
        }
        
        /**
         * Returns true if this context is fake leaf context.
         * @return true if this context is fake leaf context.
         */
        boolean isLeaf() { return type != 0; }
        
        /**
         * Returns true if contenst of this Context is equal to specified lists.
         * @returns true if contenst of this Context is equal to specified lists.
         */
        private boolean equals(ArrayList<String> names, ArrayList<Context> contexts) {
            int len = nodes.length;
            if (names.size() != len || contexts.size() != len) return false;
            for (int i = 0; i < len; i++) {
                EpicTreeNode tn = nodes[i];
                if (names.get(i) != tn.name || contexts.get(i) != tn.context) return false;
            }
            return true;
        }
        
        /**
         * Returns hash code of this Context.
         * @return hash code of this Context.
         */
        public int hashCode() { return hashCode; }
        
        /**
         * Private method to calculate hash code.
         * @param names list of names.
         * @param contexts list of contexts.
         * @return hash code
         */
        private static int hashValue(ArrayList<String> names, ArrayList<Context> contexts) {
            assert names.size() == contexts.size();
            int hash = 0;
            for (int i = 0; i < names.size(); i++)
                hash = hash * 19 + names.get(i).hashCode()^contexts.get(i).hashCode();
            return hash;
        }
        
        /**
         * Searches an EpicTreeNode in this context where signal with specified flat offset lives.
         * @param offset flat offset of EpicSignal.
         * @return index of EpicTreeNode in this Context or -1.
         */
        private int localSearch(int offset) {
            assert offset >= 0 && offset < treeSize;
            assert nodes.length > 0;
            int l = 1;
            int h = nodes.length - 1;
            while (l <= h) {
                int m = (l + h) >> 1;
                if (nodes[m].nodeOffset <= offset)
                    l = m + 1;
                else
                    h = m - 1;
            }
            return h;
        }
    }
    
    /************************* EpicTreeNode *********************************************
     * This class denotes an element of a Context.
     * It partially implements TreeNode (without getParent).
     */
    public static class EpicTreeNode implements TreeNode {
        //        private final int chronIndex;
        private final String name;
        private final Context context;
        private final int nodeOffset;
        private int sortedIndex;

        /**
         * Private constructor.
         */
        private EpicTreeNode(int chronIndex, String name, Context context, int nodeOffset) {
            //            this.chronIndex = chronIndex;
            this.name = name;
            this.context = context;
            this.nodeOffset = nodeOffset;
        }
        
        /**
         * Returns the child <code>TreeNode</code> at index
         * <code>childIndex</code>.
         */
        public TreeNode getChildAt(int childIndex) {
            try {
                return context.sortedNodes[childIndex];
            } catch (NullPointerException e) {
                throw new ArrayIndexOutOfBoundsException("node has no children");
            }
        }
        
        /**
         * Returns the number of children <code>TreeNode</code>s the receiver
         * contains.
         */
        public int getChildCount() {
            return isLeaf() ? 0 : context.sortedNodes.length;
        }
        
        /**
         * Returns the parent <code>TreeNode</code> of the receiver.
         */
        public TreeNode getParent() {
            throw new UnsupportedOperationException();
        }
        
        /**
         * Returns the index of <code>node</code> in the receivers children.
         * If the receiver does not contain <code>node</code>, -1 will be
         * returned.
         */
        public int getIndex(TreeNode node) {
            try {
                EpicTreeNode tn = (EpicTreeNode)node;
                if (context.nodes[tn.sortedIndex] == tn)
                    return tn.sortedIndex;
            } catch (Exception e) {
                if (node == null)
                    throw new IllegalArgumentException("argument is null");
            }
            return -1;
        }
        
        /**
         * Returns true if the receiver allows children.
         */
        public boolean getAllowsChildren() { return !isLeaf(); }
        
        /**
         * Returns true if the receiver is a leaf.
         */
        public boolean isLeaf() { return context.type != 0; }
        
        /**
         * Returns the children of the receiver as an <code>Enumeration</code>.
         */
        public Enumeration children() {
            if (isLeaf())
                return DefaultMutableTreeNode.EMPTY_ENUMERATION;
            return new Enumeration<EpicTreeNode>() {
                int count = 0;
                
                public boolean hasMoreElements() {
                    return count < context.nodes.length;
                }
                
                public EpicTreeNode nextElement() {
                    if (count < context.nodes.length)
                        return context.nodes[count++];
                    throw new NoSuchElementException("Vector Enumeration");
                }
            };
        }
        
        public String toString() { return name; }
    }
    
    /**
     * Comparator which compares EpicTreeNodes by type and by name.
     */
    private static Comparator<EpicTreeNode> TREE_NODE_ORDER = new Comparator<EpicTreeNode>() {
      
        public int compare(EpicTreeNode tn1, EpicTreeNode tn2) {
            int cmp = tn1.context.type - tn2.context.type;
            if (cmp != 0) return cmp;
            return TextUtils.STRING_NUMBER_ORDER.compare(tn1.name, tn2.name);
        }
    };
    
    /************************* EpicSignal *********************************************
     * Class which represents Epic AnalogSignal.
     */
    static class EpicSignal extends AnalogSignal {
        int sigNum;
        
        EpicSignal(NewEpicAnalysis an, byte type, int index, int sigNum) {
            super(an);
            assert getIndexInAnalysis() == index;
            if (type == VOLTAGE_TYPE)
                an.voltageSignals.set(index);
            else
                assert type == CURRENT_TYPE;
            this.sigNum = sigNum;
        }

        /**
         * Method to return the context of this simulation signal.
         * The context is the hierarchical path to the signal, and it usually contains
         * instance names of cells farther up the hierarchy, all separated by dots.
         * @param signalContext the context of this simulation signal.
         */
        @Override
            public void setSignalContext(String signalContext) { throw new UnsupportedOperationException(); }
        
        /**
         * Method to return the context of this simulation signal.
         * The context is the hierarchical path to the signal, and it usually contains
         * instance names of cells farther up the hierarchy, all separated by dots.
         * @return the context of this simulation signal.
         * If there is no context, this returns null.
         */
        @Override
            public String getSignalContext() {
            return ((NewEpicAnalysis)getAnalysis()).makeName(getIndexInAnalysis(), false);
        }
        
        /**
         * Method to return the full name of this simulation signal.
         * The full name includes the context, if any.
         * @return the full name of this simulation signal.
         */
        @Override
            public String getFullName() {
            return ((NewEpicAnalysis)getAnalysis()).makeName(getIndexInAnalysis(), true);
        }

        
        public void calcBounds() {
            Waveform wave = getWaveform(0);
            if (!(wave instanceof BTreeNewSignal)) { super.calcBounds(); return; }
            BTreeNewSignal btns = (BTreeNewSignal)wave;
            this.bounds = new Rectangle2D.Double(btns.getPreferredApproximation().getTime(0),
                                                 (btns.getPreferredApproximation().getSample(btns.eventWithMinValue)).getValue(),
                                                 btns.getPreferredApproximation().getTime(btns.getNumEvents()-1),
                                                 (btns.getPreferredApproximation().getSample(btns.eventWithMaxValue)).getValue());
        }
        /*
        void setBounds(int minV, int maxV) {
            NewEpicAnalysis an = (NewEpicAnalysis)getAnalysis();
            double resolution = an.getValueResolution(getIndexInAnalysis());
            leftEdge = minT;
            rightEdge = maxT;
            bounds = new Rectangle2D.Double(leftEdge, minV*resolution, rightEdge, (maxV - minV)*resolution);
            //            rightEdge = minV*resolution;
        }
        */
    }
}
