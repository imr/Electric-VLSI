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
package com.sun.electric.tool.user.ncc;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

class ComparisonsTree extends JTree 
implements ActionListener, TreeSelectionListener, TreeCellRenderer {
    // constants
    public static final int MAX_COMP_NODES = 100;
    public static final int MAX_ZEROS = 100;
    public static final int MAX_CLASSES = 200;
    public static final int MAX_LIST_ELEMENTS = 200;
    public static final int MAX_NAME_LEN = 100;
    
    // GUI variables
    private ComparisonsPane parentPane;
    private DefaultMutableTreeNode root;
    private TreeNode rootTreeNode;
    private WireClassNode wireClassNodes[][];
    protected JPopupMenu popup;
    protected String clipboard;
    protected static DefaultTreeCellRenderer defCellRenderer = 
                     new DefaultTreeCellRenderer();
    private static Border border = BorderFactory.createEmptyBorder();

    // data holders
    private NccComparisonMismatches[] mismatches;
    
    public ComparisonsTree(ComparisonsPane pane, DefaultMutableTreeNode root) {
        super(root);
        this.root = root;
        rootTreeNode = (TreeNode)root.getUserObject();
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
        wireClassNodes = new WireClassNode[misms.length][];
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

            TreeNode compTreeNode = new TreeNode(rootTreeNode, 
                                    title + " [" + cm.getTotalMismatchCount() + "]", 
                                    compNdx, -1, TreeNode.COMP_TITLE);
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
            addPartClasses(compTreeNode, compNdx, compNode, mismEqRecs, isHashChecked);
            
            // add wires entry
            addWireClasses(compTreeNode, compNdx, compNode, mismEqRecs, isHashChecked);     

            // add sizes entry, if necessary
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
            
            // add "export assertion failures" entry, if necessary
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
            
            // add "export network conflicts", if necessary
            int exportNetConflictCount = cm.getNetworkExportConflicts().size();
            String exportNetConfTitle = null;
            if (exportNetConflictCount > ExportTable.MAXROWS) {
                exportNetConfTitle = "Export/Global Network Conflicts [first " + ExportTable.MAXROWS 
                          + " of " + exportNetConflictCount + "]";                
                exportNetConflictCount = ExportTable.MAXROWS;
            } else if (exportNetConflictCount > 0) {
                exportNetConfTitle = "Export/Global Network Conflicts [" + exportNetConflictCount + "]";                
            }
            if (exportNetConflictCount > 0)
                compNode.add(new DefaultMutableTreeNode(
                             new TreeNode(compTreeNode, exportNetConfTitle, 
                                          compNdx, -1, TreeNode.EXPORT_NET_CONF)));
            
            // add "export characteristics conflicts", if necessary
            int exportChrConflictCount = cm.getNetworkExportConflicts().size();
            String exportChrConfTitle = null;
            if (exportChrConflictCount > ExportTable.MAXROWS) {
                exportChrConfTitle = "Export/Global Characteristics Conflicts [first " 
                    + ExportTable.MAXROWS + " of " + exportChrConflictCount + "]";                
                exportChrConflictCount = ExportTable.MAXROWS;
            } else if (exportChrConflictCount > 0) {
                exportChrConfTitle = "Export/Global Characteristics Conflicts [" 
                    + exportChrConflictCount + "]";                
            }
            if (exportChrConflictCount > 0)
                compNode.add(new DefaultMutableTreeNode(
                             new TreeNode(compTreeNode, exportChrConfTitle, 
                                          compNdx, -1, TreeNode.EXPORT_CHR_CONF)));
            
            // add "unrecognized MOSes", if necessary
            int unrecMOSesCount = cm.getUnrecognizedMOSes().size();
            String unrecMOSesTitle = null;
            if (unrecMOSesCount > ExportTable.MAXROWS) {
                unrecMOSesTitle = "Unrecognized MOSes [first " 
                    + ExportTable.MAXROWS + " of " + unrecMOSesCount + "]";                
                unrecMOSesCount = ExportTable.MAXROWS;
            } else if (unrecMOSesCount > 0) {
                unrecMOSesTitle = "Unrecognized MOSes [" + unrecMOSesCount + "]";                
            }
            if (unrecMOSesCount > 0)
                compNode.add(new DefaultMutableTreeNode(
                             new TreeNode(compTreeNode, unrecMOSesTitle, 
                                          compNdx, -1, TreeNode.UNRECOG_MOS)));            
        }
        setRootVisible(true);
        updateUI();      
        expandRow(0);
        expandRow(1);
        setRootVisible(false);
        addSelectionRow(0);
        requestFocusInWindow();
    }    
    
    private void addPartClasses(TreeNode compTreeNode, int compNdx, 
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
        int count=0;
        boolean truncated = false;
        for (int i=0; i<mismEqRecs.length; i++) {
            // fill in array of mismatches 
            if (mismEqRecs[i].getNetObjType() != NetObject.Type.PART) continue;
            count++;
            // limit output size
            if (count > MAX_CLASSES) { truncated = true; continue;}
            
            List reasons = mismEqRecs[i].getPartitionReasonsFromRootToMe();
            StringBuffer nodeName = new StringBuffer("#"+ count + " [");
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
            partTreeNode.setShortName("Part Class #"+ count);
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
        if (count == 0)
            inode.remove(parts);
        else {
            StringBuffer buf = new StringBuffer("Parts ");
            if (isHashChecked) buf.append("(hash code) ");
            buf.append("[");
            if (truncated) buf.append("first " + MAX_CLASSES + " of ");
            buf.append((count) + "]");
            partsNode.setFullName(buf.toString());
        }
    }
    
    private void addWireClasses(TreeNode compTreeNode, int compNdx, 
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
        int count = 0;
        boolean truncated = false;        
        for (int i=0; i<mismEqRecs.length; i++) {
            if (mismEqRecs[i].getNetObjType() != NetObject.Type.WIRE) continue;
            count++;
            // limit output size
            if (count > MAX_CLASSES) { truncated = true; continue;}            
            
            wireTreeNode = new TreeNode(compTreeNode, "Wire Class #" + count, 
                                        compNdx, i, type);
            wireTreeNode.setShortName("Wire Class #" + count);
            wireTreeNode.setWireClassNum(count-1);
            
            eclass = new DefaultMutableTreeNode(wireTreeNode);
            wires.add(eclass);
            if (!isHashChecked) {
                String reasons[] = 
                    (String[])mismEqRecs[i].getPartitionReasonsFromRootToMe().toArray(new String[0]);
                int j = 0;
                if (reasons.length == 0) {
                    eclass.add(new DefaultMutableTreeNode(
                            new TreeNode(wireTreeNode, "all Wires are indistinguishable", 
                                         compNdx, i, TreeNode.WIRELEAF)));
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
        if (count == 0)
            inode.remove(wires);
        else {
            StringBuffer buf = new StringBuffer("Wires ");
            if (isHashChecked) buf.append("(hash code) ");
            buf.append("[");
            if (truncated) buf.append("first " + MAX_CLASSES + " of ");
            buf.append((count) + "]");
            wiresNode.setFullName(buf.toString());
            wireClassNodes[compNdx] = new WireClassNode[count];
        }
    } 
    
    public TreeCellRenderer getCellRenderer() {
        return this;
    }

    /* (non-Javadoc)
     * ActionListener interface (for popup menus)
     */
    public void actionPerformed(ActionEvent e) {
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        // copy text prepared during popup to clipboard
        StringSelection ss = new StringSelection(clipboard);
        cb.setContents(ss,ss);                    
    }

    /* (non-Javadoc)
     * TreeSelectionListener interface
     */
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

    /* (non-Javadoc)
     * TreeCellRenderer interface
     */
    public Component getTreeCellRendererComponent(JTree tree, Object value, 
                          boolean selected, boolean expanded, boolean leaf, 
                          int row, boolean hasFocus) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
        TreeNode data = (TreeNode)node.getUserObject();
        int compNdx = data.compNdx;        
        if (data.type == TreeNode.WIRE && wireClassNodes[compNdx] != null) {
            int wclass = data.getWireClassNum();
            if (wireClassNodes[compNdx][wclass] == null)
                createWireClassNodes(data, node.isLeaf());
            if (selected)
                wireClassNodes[compNdx][wclass].select();
            else
                wireClassNodes[compNdx][wclass].deselect();
            if (!node.isLeaf())
                if (expanded)
                    wireClassNodes[compNdx][wclass].expand();
                else
                    wireClassNodes[compNdx][wclass].collapse();
            return wireClassNodes[compNdx][wclass].getPanel();
        }
        
        return defCellRenderer.getTreeCellRendererComponent(tree, value, selected, expanded,
                                                            leaf, row, hasFocus);
    }
    
    private void createWireClassNodes(TreeNode data, boolean isLeaf) {
        int compNdx = data.compNdx;
        EquivRecord[] mismEqRecs = mismatches[compNdx].getMismatchedEquivRecords();
        int count = 0, len = wireClassNodes[compNdx].length;
        int maxWidth0 = 0, maxWidth1 = 0, maxWidth2 = 0, height = 0;
        Font font = getFont();
        
        for (int i=0; i<mismEqRecs.length; i++) {
            if (mismEqRecs[i].getNetObjType() != NetObject.Type.WIRE) continue;
            count++;
            // number of classes might have been limited
            if (count > len) break;
            
            JLabel labels[] = new JLabel[4];
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
                    // limit name length
                    if (descr[cell].length() > MAX_NAME_LEN)
                        descr[cell] = descr[cell].substring(0, MAX_NAME_LEN) + "...";
                    if (it2.hasNext())
                        descr[cell] = "{ " + descr[cell] + ",...}";
                    else
                        descr[cell] = "{ " + descr[cell] + " }";
                } else
                    descr[cell] = "{ }";
                
            }
            
            StringBuffer lab3Name = new StringBuffer(24);
            lab3Name.append("[");
            if (mismEqRecs[i].maxSize() > MAX_LIST_ELEMENTS)
                lab3Name.append("first " + MAX_LIST_ELEMENTS + " of ");
            lab3Name.append(mismEqRecs[i].maxSize() + "]");
            StringBuffer name = new StringBuffer(32);

            labels[0] = new JLabel("#" + count + " : ");
            labels[0].setHorizontalAlignment(SwingConstants.RIGHT);
            name.append(labels[0].getText() + " ");
            maxWidth0 = Math.max(labels[0].getPreferredSize().width, maxWidth0);
            
            labels[1] = new JLabel(descr[0]);
            name.append(descr[0] + " ");
            maxWidth1 = Math.max(labels[1].getPreferredSize().width, maxWidth1);

            labels[2] = new JLabel(descr[1]);
            name.append(descr[1] + " ");
            maxWidth2 = Math.max(labels[2].getPreferredSize().width, maxWidth2);
            
            labels[3] = new JLabel(lab3Name.toString());
            name.append(labels[3].getText());

            for (int j=0; j<4; j++) {
                labels[j].setBorder(border);
                labels[j].setFont(font);                
            }

            data.setFullName(name.toString());
            wireClassNodes[compNdx][count-1] = new WireClassNode(labels, isLeaf);
            if (count == 1) height = labels[0].getPreferredSize().height;
        }
        if (count > 0) {
            Dimension dim[] = new Dimension[3];
            dim[0] = new Dimension(maxWidth0     , height);
            dim[1] = new Dimension(maxWidth1 + 10, height);
            dim[2] = new Dimension(maxWidth2 + 10, height);
            for (int i=0; i<count; i++)
                wireClassNodes[compNdx][i].setTextLabelDimension(dim);
        }
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
    
    static class TreeNode {
        public static final int TITLE = 0;
        public static final int COMP_TITLE = 1;
        public static final int EXPORTS = 2;
        public static final int PART = 3;
        public static final int WIRE = 4;
        public static final int PARTLEAF = 5;
        public static final int WIRELEAF = 6;        
        public static final int SIZES = 7;
        public static final int EXPORT_ASSERTS = 8;
        public static final int EXPORT_NET_CONF = 9;
        public static final int EXPORT_CHR_CONF = 10;
        public static final int UNRECOG_MOS = 11;
        
        public final int compNdx, eclass, type;
        private String fullName;
        private String shortName;
        private TreeNode parent;
        /** If this node represents a wire class, then wireClassNum is the 
         * index of this class in EquivRecord array of the corresponding 
         * NccComparisonResult object. Otherwise is -1. */
        private int wireClassNum = -1;
        
        public TreeNode(TreeNode parent, String fullName, 
                        int compNdx, int eclass, int type) {
            this.parent = parent;
            this.fullName = fullName;
            shortName = fullName;
            this.compNdx = compNdx;
            this.eclass = eclass;
            this.type = type;
        }
        
        public void setFullName(String n)   { fullName = n; }
        public void setShortName(String n)  { shortName = n;}
        
        /** Only for type WIRE */
        public void setWireClassNum(int num) {
            if (type != WIRE) return;
            wireClassNum = num; 
        }
        
        public String   getFullName()     { return fullName;     }
        public String   getShortName()    { return shortName;    }
        public TreeNode getParent()       { return parent;       }
        public int      getWireClassNum() { return wireClassNum; }
        public String   toString()        { return fullName;     }
    }
    
    private static class WireClassNode {
        private JPanel treeNodePanel;
        private JPanel textPanel;
        private JLabel iconLabel;
        private JLabel textLabels[];
        private boolean isLeaf;        
        private boolean expanded = false, selected = false;
        private static boolean inited = false;
        private static Color selBackgnd, deselBackgnd, selText, deselText;
        private static Icon leafIcon, openIcon, closedIcon;
        private static Border border = BorderFactory.createEmptyBorder();
        
        public WireClassNode(JLabel labels[], boolean leaf) {
            if (!inited) init();
            textLabels = labels;
            isLeaf = leaf;
            
            textPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            textPanel.setBorder(border);
            textPanel.setBackground(deselBackgnd);
            for (int j=0; j<4; j++) textPanel.add(textLabels[j]);
            
            treeNodePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            treeNodePanel.setBorder(border);
            treeNodePanel.setBackground(deselBackgnd);
            if (isLeaf)
                iconLabel = new JLabel(leafIcon);
            else
                iconLabel = new JLabel(closedIcon);
            iconLabel.setBorder(border);
            iconLabel.setText(" ");
            treeNodePanel.add(iconLabel);
            treeNodePanel.add(textPanel);
        }
        
        /**
         * Get colors and icons from default renderer
         * @param renderer  instance of the default tree node renderer 
         */
        private static void init() {
            defCellRenderer = new DefaultTreeCellRenderer();
            selBackgnd = defCellRenderer.getBackgroundSelectionColor();
            deselBackgnd = defCellRenderer.getBackgroundNonSelectionColor();
            selText = defCellRenderer.getTextSelectionColor();
            deselText = defCellRenderer.getTextNonSelectionColor();
            leafIcon = defCellRenderer.getDefaultLeafIcon();
            openIcon = defCellRenderer.getDefaultOpenIcon();
            closedIcon = defCellRenderer.getDefaultClosedIcon();
            inited = true;
        }
        
        public void select()   {
            if (selected) return;
            textPanel.setBackground(selBackgnd);
            for (int i=0; i<4; i++) textLabels[i].setForeground(selText);
            selected = true;
        }
        public void deselect() {
            if (!selected) return;
            textPanel.setBackground(deselBackgnd);
            for (int i=0; i<4; i++) textLabels[i].setForeground(deselText);
            selected = false;
        }
        public void expand()   {
            if (isLeaf || expanded) return;
            iconLabel.setIcon(openIcon);
            expanded = true;
        }
        public void collapse() {
            if (isLeaf || !expanded) return;
            iconLabel.setIcon(closedIcon);
            expanded = false;
        }

        public void setTextLabelDimension(Dimension[] dim) {
            for (int j=0; j<dim.length; j++) {
                textLabels[j].setMinimumSize(dim[j]);
                textLabels[j].setPreferredSize(dim[j]);
            }
        }

        public JPanel getPanel() { return treeNodePanel; }
    }
}
