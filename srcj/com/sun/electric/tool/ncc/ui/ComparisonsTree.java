/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: Ncc.java
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
package com.sun.electric.tool.ncc.ui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

class ComparisonsTree extends JTree implements ActionListener, TreeSelectionListener {
    // constants
    public static final int MAX_COMP_NODES = 100;
    public static final int MAX_ZEROS = 100;
    public static final int MAX_CLASSES = 200;
    public static final int MAX_LIST_ELEMENTS = 200;
    
    // GUI variables
    private ComparisonsPane parentPane;
    private DefaultMutableTreeNode root;
    protected JPopupMenu popup;
    protected String clipboard;

    // data holders
    private NccComparisonMismatches[] mismatches;
    
    public ComparisonsTree(ComparisonsPane pane, DefaultMutableTreeNode root) {
        super(root);
        this.root = root;
        parentPane = pane;
        setMinimumSize(new Dimension(0,0));
        setShowsRootHandles(true);
        addMouseListener(new TreeMouseAdapter());
        addTreeSelectionListener(this);
        createPopup();
    }

    private void createPopup() {
        popup = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Copy Node Title To Clipboard");
        menuItem.addActionListener(this);
        popup.add(menuItem);
    }

    public void update(NccComparisonMismatches[] misms) {
        mismatches = misms;
        root.removeAllChildren();
        DefaultMutableTreeNode compNode;
        EquivRecord[] mismEqRecs;
        for (int compNdx = 0; compNdx < mismatches.length 
                           && compNdx < MAX_COMP_NODES; compNdx++) {
            NccComparisonMismatches cm = mismatches[compNdx];
            String titles[] = cm.getNames();
            
            String title0 = titles[0].substring(0,titles[0].length()-5);
            String title1 = titles[1].substring(0,titles[1].length()-5);
            String title;
            if (title1.equals(title1))
                title = title0 + "{sch,lay}";
            else
                title = titles[0] + " & " + titles[1];

            mismEqRecs = cm.getMismatchedEquivRecords();
            parentPane.setMismatchEquivRecs(compNdx, mismEqRecs);
            
            TreeNode compTreeNode = new TreeNode(null, 
                                    title + " [" + cm.getTotalMismatchCount() + "]", 
                                    compNdx, -1, TreeNode.TITLE);
            compTreeNode.setShortName(title);
            compNode = new DefaultMutableTreeNode(compTreeNode);
            root.add(compNode);
            
            // add exports entry if necessary
            int exportMismCount = cm.getValidExportMismatchCount();
            String exportsTitle = null;
            if (exportMismCount > ExportTable.MAXROWS) {
                exportsTitle = "Exports [first " + ExportTable.MAXROWS 
                          + " of " + exportMismCount + "]";                
                exportMismCount = ExportTable.MAXROWS;
            } else if (exportMismCount > 0) {
                exportsTitle = "Exports [" + exportMismCount + "]";                
            }
            if (exportMismCount > 0)
                compNode.add(new DefaultMutableTreeNode(
                             new TreeNode(compTreeNode, exportsTitle, 
                                          compNdx, -1, TreeNode.EXPORTS)));
            
            boolean isHashChecked = cm.isHashChecked();
            
            // add parts entry
            addPartsClasses(compTreeNode, compNdx, compNode, mismEqRecs, isHashChecked);
            
            // add wires entry
            addWiresClasses(compTreeNode, compNdx, compNode, mismEqRecs, isHashChecked);     

            // add sizes entry if necessary
            int sizeMismCount = cm.getSizeMismatches().size();
            String sizeTitle = null;
            if (sizeMismCount > SizeMismatchPane.MAXROWS) {
                sizeTitle = "Sizes [first " + SizeMismatchPane.MAXROWS 
                          + " of " + sizeMismCount + "]";                
                sizeMismCount = SizeMismatchPane.MAXROWS;
            } else if (sizeMismCount > 0){
                sizeTitle = "Sizes [" + sizeMismCount + "]";                
            }
            if (sizeMismCount > 0)
                compNode.add(new DefaultMutableTreeNode(
                             new TreeNode(compTreeNode, sizeTitle, 
                                          compNdx, -1, TreeNode.SIZES)));
            
            // add "export assertion failures" entry if necessary
            int exportAssrtCount = cm.getExportAssertionFailures().size();
            String exportAssrtTitle = null;
            if (exportAssrtCount > ExportTable.MAXROWS) {
                exportAssrtTitle = "Export Assertions [first " + ExportTable.MAXROWS 
                          + " of " + exportAssrtCount + "]";                
                exportAssrtCount = ExportTable.MAXROWS;
            } else if (exportAssrtCount > 0) {
                exportAssrtTitle = "Export Assertions [" + exportAssrtCount + "]";                
            }
            if (exportAssrtCount > 0)
                compNode.add(new DefaultMutableTreeNode(
                             new TreeNode(compTreeNode, exportAssrtTitle, 
                                          compNdx, -1, TreeNode.EXPORT_ASSERTS)));

            
        }
        setRootVisible(true);
        updateUI();        
        expandRow(0);
        expandRow(1);
        setRootVisible(false);
    }    
    
    private void addPartsClasses(TreeNode compTreeNode, int compNdx, 
                                 DefaultMutableTreeNode inode, 
                                 EquivRecord[] mismEqRecs,
                                 boolean isHashChecked) {
        DefaultMutableTreeNode parts, eclass;
        // add parts entry title
        TreeNode partsNode = new TreeNode(compTreeNode, "Parts ", 
                                          compNdx, -1, TreeNode.TITLE);
        parts = new DefaultMutableTreeNode(partsNode);
        inode.add(parts);

        // add part equivalence classes
        int type = TreeNode.PART;
        int index=0;
        boolean truncated = false;
        for (int i=0; i<mismEqRecs.length; i++) {
            // fill in array of mismatches 
            if (mismEqRecs[i].getNetObjType() != NetObject.Type.PART) continue;
            index++;
            // limit output size
            if (index > MAX_CLASSES) { truncated = true; continue;}
            
            List reasons = mismEqRecs[i].getPartitionReasonsFromRootToMe();
            StringBuffer nodeName = new StringBuffer("[");
            if (mismEqRecs[i].maxSize() > MAX_LIST_ELEMENTS)
                nodeName.append("first " + MAX_LIST_ELEMENTS + " of ");
            nodeName.append(mismEqRecs[i].maxSize() + "]");
            
            Iterator it = reasons.iterator();
            if (it.hasNext()) {
                String reas = (String)it.next();
                nodeName.append(": " + reas.substring(reas.indexOf("type is ") + 8));
            } 
            if (it.hasNext()) {
                String reas = (String)it.next();
                if (reas.endsWith("different Wires attached")) {
                    int a = reas.indexOf("has ") + 4;
                    int b = reas.indexOf(" different");
                    nodeName.append(", " + reas.substring(a,b) + " Wires attached");
                }
            }
            
            TreeNode partTreeNode = new TreeNode(compTreeNode, nodeName.toString(), 
                                                 compNdx, i, type);
            partTreeNode.setShortName("Part Class #"+ index);
            eclass = new DefaultMutableTreeNode(partTreeNode);
            parts.add(eclass);
            
            if (reasons.size() == 0) {
                eclass.add(new DefaultMutableTreeNode(
                           new TreeNode(partTreeNode, "all Parts are indistinguishable", 
                                        compNdx, i, TreeNode.PARTLEAF)));
            } else while (it.hasNext()) {
                eclass.add(new DefaultMutableTreeNode(
                           new TreeNode(partTreeNode, (String)it.next(), 
                                        compNdx, i, TreeNode.PARTLEAF)));
            }
        }
        if (index == 0)
            inode.remove(parts);
        else {
            StringBuffer buf = new StringBuffer("Parts ");
            if (isHashChecked) buf.append("(hashcode) ");
            buf.append("[");
            if (truncated) buf.append("first " + MAX_CLASSES + " of ");
            buf.append((index) + "]");
            partsNode.setFullName(buf.toString());
        }
    }
    
    private void addWiresClasses(TreeNode compTreeNode, int compNdx, 
                                 DefaultMutableTreeNode inode, 
                                 EquivRecord[] mismEqRecs,
                                 boolean isHashChecked) {
        DefaultMutableTreeNode wires, eclass, node;
        // add wires entry title
        TreeNode wiresNode = new TreeNode(compTreeNode, "Wires ", 
                                          compNdx, -1, TreeNode.TITLE);
        wires = new DefaultMutableTreeNode(wiresNode);
        inode.add(wires);
        
        // add wire equivalence classes
        TreeNode wireTreeNode;
        int type = TreeNode.WIRE;
        int index = 0;
        boolean truncated = false;        
        for (int i=0; i<mismEqRecs.length; i++) {
            if (mismEqRecs[i].getNetObjType() != NetObject.Type.WIRE) continue;
            index++;
            // limit output size
            if (index > MAX_CLASSES) { truncated = true; continue;}            
            
            String descr[] = new String[2];
            int cell=0;
            for (Iterator it=mismEqRecs[i].getCircuits(); it.hasNext(); cell++) {
                Circuit ckt = (Circuit) it.next();
                Iterator it2=ckt.getNetObjs();
                if (it2.hasNext()) {
                    descr[cell] = parentPane.cleanNetObjectName(
                               ((NetObject) it2.next()).instanceDescription());
                    int ind = descr[cell].indexOf(" in Cell: ");
                    if (ind > 0) descr[cell] = descr[cell].substring(0, ind).trim();
                    if (it2.hasNext())
                        descr[cell] = "{" + descr[cell] + ",...}";
                    else
                        descr[cell] = "{" + descr[cell] + "}";
                } else
                    descr[cell] = "{}";
                
            }
            
            StringBuffer nodeName = new StringBuffer("[");
            if (mismEqRecs[i].maxSize() > MAX_LIST_ELEMENTS)
                nodeName.append("first " + MAX_LIST_ELEMENTS + " of ");
            nodeName.append(mismEqRecs[i].maxSize() + "]: ");
            nodeName.append(descr[0] + "   " + descr[1]);
            
            wireTreeNode = new TreeNode(compTreeNode, nodeName.toString(), 
                                        compNdx, i, type); 
            eclass = new DefaultMutableTreeNode(wireTreeNode);
            wires.add(eclass);
            if (!isHashChecked) {
                String reasons[] = 
                    (String[])mismEqRecs[i].getPartitionReasonsFromRootToMe().toArray(new String[0]);
                int j = 0;
                if (reasons.length == 0) {
                    eclass.add(new DefaultMutableTreeNode(
                            new TreeNode(wireTreeNode, "all Wires are indistinguishable", 
                                         compNdx, i, TreeNode.PARTLEAF)));
                } else if (reasons.length > 1 
                        && reasons[0].startsWith("0") && reasons[1].startsWith("0")) {
                    node = new DefaultMutableTreeNode(
                           new TreeNode(wireTreeNode, "0's", compNdx, i, TreeNode.WIRELEAF));
                    eclass.add(node);
                    for (; j<reasons.length && reasons[j].startsWith("0"); j++) {
                        if (j >= MAX_ZEROS) continue; // limit number of zeros displayed
                        int start = reasons[j].indexOf(" of ");
                        String reason;
                        if (start >= 0)
                            reason = reasons[j].substring(start + 4);
                        else
                            reason = reasons[j];
                        node.add(new DefaultMutableTreeNode(
                                 new TreeNode(wireTreeNode, reason, compNdx, i, TreeNode.WIRELEAF)));
                    }
                }
                for (; j<reasons.length; j++)
                    eclass.add(new DefaultMutableTreeNode(
                               new TreeNode(wireTreeNode, reasons[j], compNdx, i, TreeNode.WIRELEAF)));
            }
        }
        if (index == 0)
            inode.remove(wires);
        else {
            StringBuffer buf = new StringBuffer("Wires ");
            if (isHashChecked) buf.append("(hashcode) ");
            buf.append("[");
            if (truncated) buf.append("first " + MAX_CLASSES + " of ");
            buf.append((index) + "]");
            wiresNode.setFullName(buf.toString());
        }
    } 

    /* (non-Javadoc)
     * Action Listener interface (for popup menus)
     */
    public void actionPerformed(ActionEvent e) {
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        // copy text prepared during popup to clipboard
        StringSelection ss = new StringSelection(clipboard);
        cb.setContents(ss,ss);                    
    }

    public void valueChanged(TreeSelectionEvent e) {
        TreePath[] paths = e.getPaths();
        boolean doAdded = false;
        for (int i=0; i<2; i++) {
            for (int j=0; j<paths.length; j++) {
                if (e.isAddedPath(j) != doAdded) continue;
                if (paths[j] == null) {
                    parentPane.treeSelectionChanged(null, doAdded);
                    continue;
                }
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)paths[j].getLastPathComponent();
                parentPane.treeSelectionChanged((TreeNode)node.getUserObject(), doAdded);
            }
            doAdded = true;
        }
        parentPane.updateRightPane();
    }
    
    private class TreeMouseAdapter extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            if (! (e.getButton() == MouseEvent.BUTTON2 
                || e.getButton() == MouseEvent.BUTTON3))
                return;
            JTree aTree =  (JTree)e.getSource();
            if (aTree.getRowForLocation(e.getX(), e.getY()) != -1) {
                TreePath selPath = aTree.getPathForLocation(e.getX(), e.getY());
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)selPath.getLastPathComponent();
                TreeNode data = (TreeNode)node.getUserObject();
                // extract node name to be copied to clipboard
                clipboard = data.toString();
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
    
    class TreeNode {
        public static final int TITLE = 0;
        public static final int EXPORTS = 1;
        public static final int PART = 2;
        public static final int WIRE = 3;
        public static final int PARTLEAF = 4;
        public static final int WIRELEAF = 5;        
        public static final int SIZES = 6;
        public static final int EXPORT_ASSERTS = 7;
        
        public final int compNdx, eclass, type;
        private String fullName;
        private String shortName;
        private TreeNode parent;
        
        public TreeNode(TreeNode parent, String fullName, 
                        int compNdx, int eclass, int type) {
            this.parent = parent;
            this.fullName = fullName;
            shortName = fullName;
            this.compNdx = compNdx;
            this.eclass = eclass;
            this.type = type;
        }
        public void setFullName(String n)  { fullName = n; }
        public void setShortName(String n) { shortName = n;}
        public String   getFullName()  { return fullName;  }
        public String   getShortName() { return shortName; }
        public TreeNode getParent()    { return parent;    }
        public String   toString()     { return fullName;  }
    }    
}
