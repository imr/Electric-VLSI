/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: ComparisonsPane.java
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.ncc.result.EquivRecReport;
import com.sun.electric.tool.ncc.result.NccResult;
import com.sun.electric.tool.ncc.result.NetObjReport;
import com.sun.electric.tool.ncc.result.PartReport;
import com.sun.electric.tool.ncc.result.WireReport;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.ncc.ComparisonsTree.TreeNode;

/**
 * This class implements the right side of the NCC GUI window.
 * It is a placeholder for tables of different types, such as Exports, 
 * Equivalence Classes, Sizes, etc.
 */
class ComparisonsPane extends JSplitPane implements ActionListener {
    
    /* what is currently displayed */
    private static final int EMPTY = 0;
    private static final int COMP_SUMMARY = 1;
    private static final int EXPORTS = 2;
    private static final int PARTS_WIRES = 3;
    private static final int SIZES = 4;
    private static final int EXPORT_ASSERTS = 5;
    private static final int EXPORT_NET_CONF = 6;
    private static final int EXPORT_CHR_CONF = 7;
    private static final int UNRECOG_PART = 8;
    
    /** max number of concurrent equiv. classes */ 
    private static final int MAX_CONCUR_EQ_RECS = 5;
    
    private static final String emptyStr = " ";
    private static final String LSEP = System.getProperty("line.separator");
    
    /* --- GUI variables --- */
    protected String defaultTitles[] = {emptyStr, emptyStr};
    private String treeTitle = "  Mismatched Comparisons";
    private JLabel treeLabel = new JLabel(treeTitle);
    private ComparisonsTree tree;
    private JScrollPane treeScrollPane;
    private int dispOnRight = EMPTY;
    
    private static Border border = BorderFactory.createEmptyBorder();
    
    /* tables corresponding to different tree node types */
    private JScrollPane exportsPanes[];    
    private JScrollPane exportAssertionsPanes[];
    private JScrollPane exportNetConflictPanes[];
    private JScrollPane exportChrConflictPanes[];
    private JScrollPane unrecognizedPartsPanes[];
    private EquivClassSplitPane rightSplPanes[];
    private JPanel sizesPanes[];
    
    /** Right-click popup for a tree node */
    protected JPopupMenu treePopup;
    
    /** Right-click popup for a table cell */
    protected JPopupMenu cellPopup;
    protected String clipboard;
    
    /* --- Data holders --- */
    /** Current list of mismatched comparisons */
    private NccGuiInfo mismatches[];
    /** Current list of Wire/Part equiv. classes with mismatches */
    private EquivRecReport mismEqRecs[][];
    /** Mismatched NetObjects in current EquivRecords */
    private List<NetObjReport>[] mismNetObjs[][];
    /** Matched NetObjects in current EquivRecords */    
    private List<NetObjReport>[] matchedNetObjs[][];
    /** Vector of currently selected equiv. class TreeNode objects */
    private Vector<TreeNode> curEqRecNodes = new Vector<TreeNode>();
    /** Vector of equiv. class TreeNode objects that should be displayed */
    private Vector<TreeNode> curEqRecNodesToDisplay = new Vector<TreeNode>();
    /** Vector of currently selected exclusive TreeNode objects 
      Exclusive nodes are those requiring exclusive access to the right pane.
      All nodes except for EquivRecord and TITLE are exclusive. */
    private Vector<TreeNode> curExlusiveNodes = new Vector<TreeNode>(); 
    
    public ComparisonsPane() {
        super(JSplitPane.HORIZONTAL_SPLIT);
        setOneTouchExpandable(true);
        setDividerLocation(0.5);
        // after a resize the right half gets more resized
        setResizeWeight(0.2);
        
        createCellPopup();
        rightSplPanes = new EquivClassSplitPane[MAX_CONCUR_EQ_RECS];
        for (int i=0; i<MAX_CONCUR_EQ_RECS; i++)
            rightSplPanes[i] = new EquivClassSplitPane(this, i+1);
        setRightComponent(rightSplPanes[0]);
        
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(treeLabel, BorderLayout.NORTH);
        leftPanel.setBorder(border);
        TreeNode rootNode = new TreeNode(null, treeTitle, 
                                          -1, -1, TreeNode.TITLE);
        tree = new ComparisonsTree(this, new DefaultMutableTreeNode(rootNode));
        treeScrollPane = new JScrollPane(tree);
        leftPanel.add(treeScrollPane, BorderLayout.CENTER);
        setLeftComponent(leftPanel);
    }
    
    /** Set the current set of mismatches to the provided one */
    public void setMismatches(List<NccGuiInfo> misms) {
        mismatches = 
            (NccGuiInfo[])misms.toArray(new NccGuiInfo[0]);
        // allocate arrays of for tables
        mismEqRecs = new EquivRecReport[mismatches.length][];
        mismNetObjs = new ArrayList[mismatches.length][][];
        matchedNetObjs = new ArrayList[mismatches.length][][];
        exportsPanes = new JScrollPane[mismatches.length];
        exportAssertionsPanes = new JScrollPane[mismatches.length];
        exportNetConflictPanes = new JScrollPane[mismatches.length];
        exportChrConflictPanes = new JScrollPane[mismatches.length];
        unrecognizedPartsPanes = new JScrollPane[mismatches.length];
        sizesPanes = new JPanel[mismatches.length];
        
        // clear lists of selected/displayed TreeNode objects 
        curEqRecNodes.clear();
        curEqRecNodesToDisplay.clear();
        curExlusiveNodes.clear();
        
        // display summary of the first comparison
        int divPos = getDividerLocation();
        dispOnRight = COMP_SUMMARY;
        displayComparisonSummary(0);
        setDividerLocation(divPos);

        // update tree
        StringBuffer buf = new StringBuffer(treeTitle + " [");
        if (mismatches.length > ComparisonsTree.MAX_COMP_NODES)
            buf.append("first " + ComparisonsTree.MAX_COMP_NODES + " of ");
        buf.append(mismatches.length + "]");
        treeLabel.setText(buf.toString());
        tree.update(mismatches);
        treeScrollPane.getVerticalScrollBar().setValue(0);
        getExportsPane(0);  // preload exports of the first comparison
    }
    
    /** 
     * Set the array of Part/Wire equiv. classes for comparison number compNdx
     * @param compNdx index of the comparison
     * @param equivRecs array of equiv. classes
     */
    protected void setMismatchEquivRecs(int compNdx, EquivRecReport[] equivRecs) {
        mismEqRecs[compNdx] = equivRecs;
        mismNetObjs[compNdx] = new ArrayList[equivRecs.length][];
        matchedNetObjs[compNdx] = new ArrayList[equivRecs.length][];
    }

    /** 
     * Get the array of Part/Wire equiv. classes for comparison number compNdx
     * @param compNdx  index of the comparison
     * @return  array of equiv. classes
     */
    protected EquivRecReport[] getMismatchEquivRecs(int compNdx) {
        return mismEqRecs[compNdx];
    }    
    
    /**
     * Reset the content of the right pane to empty
     */
    private void resetRightPane() {
        // reset right pane view
        int divPos = getDividerLocation();
        for (int j=0; j<2; j++) {
            rightSplPanes[0].setLabelText(0,j,defaultTitles[j]);
            rightSplPanes[0].setCellText(0,j,emptyStr);
        }
        setRightComponent(rightSplPanes[0]);
        setDividerLocation(divPos);
        dispOnRight = EMPTY;
    }
    
    /**
     * Get the Exports table for comparison number compNdx
     * @param compNdx  comparison index
     * @return Exports table wrapped into a JScrollPane
     */
    private JScrollPane getExportsPane(int compNdx) {
        if (compNdx < 0 || compNdx >= exportsPanes.length) return null;
        if (exportsPanes[compNdx] == null) {
            ExportMismatchTable table = new ExportMismatchTable(mismatches[compNdx]);
            exportsPanes[compNdx] = new JScrollPane(table);
            exportsPanes[compNdx].setBackground(Color.WHITE);
        }
        return exportsPanes[compNdx];
    }

    /**
     * Get the Export Assertions table for comparison number compNdx
     * @param compNdx  comparison index
     * @return Export Assertions table wrapped into a JScrollPane
     */
    private JScrollPane getExportAssertionPane(int compNdx) {
        if (compNdx < 0 || compNdx >= exportAssertionsPanes.length) return null;
        if (exportAssertionsPanes[compNdx] == null) {
            ExportAssertionTable table = new ExportAssertionTable(mismatches[compNdx]);
            exportAssertionsPanes[compNdx] = new JScrollPane(table);
            exportAssertionsPanes[compNdx].setBackground(Color.WHITE);
        }
        return exportAssertionsPanes[compNdx];
    }    

    /**
     * Get the Export/Global Network Conflict table for comparison number compNdx
     * @param compNdx  comparison index
     * @return Export/Global Network Conflict table wrapped into a JScrollPane
     */
    private JScrollPane getExportNetConflictPane(int compNdx) {
        if (compNdx < 0 || compNdx >= exportNetConflictPanes.length) return null;
        if (exportNetConflictPanes[compNdx] == null) {
            ExportConflictTable table = 
                new ExportConflictTable.NetworkTable(mismatches[compNdx]);
            exportNetConflictPanes[compNdx] = new JScrollPane(table);
            exportNetConflictPanes[compNdx].setBackground(Color.WHITE);
        }
        return exportNetConflictPanes[compNdx];
    }    

    /**
     * Get the Export/Global Characteristics Conflict table for comparison number compNdx
     * @param compNdx  comparison index
     * @return Export/Global Characteristics Conflict table wrapped into a JScrollPane
     */
    private JScrollPane getExportChrConflictPane(int compNdx) {
        if (compNdx < 0 || compNdx >= exportChrConflictPanes.length) return null;
        if (exportChrConflictPanes[compNdx] == null) {
            ExportConflictTable table = 
                new ExportConflictTable.CharacteristicsTable(mismatches[compNdx]);
            exportChrConflictPanes[compNdx] = new JScrollPane(table);
            exportChrConflictPanes[compNdx].setBackground(Color.WHITE);
        }
        return exportChrConflictPanes[compNdx];
    }    

    /**
     * Get the Unrecognized Parts table for comparison number compNdx
     * @param compNdx  comparison index
     * @return Unrecognized Parts table wrapped into a JScrollPane
     */
    private JScrollPane getUnrecognizedPartsPane(int compNdx) {
        if (compNdx < 0 || compNdx >= unrecognizedPartsPanes.length) return null;
        if (unrecognizedPartsPanes[compNdx] == null) {
            UnrecognizedPartTable table = new UnrecognizedPartTable(mismatches[compNdx]);
            unrecognizedPartsPanes[compNdx] = new JScrollPane(table);
            unrecognizedPartsPanes[compNdx].setBackground(Color.WHITE);
        }
        return unrecognizedPartsPanes[compNdx];
    }    
    
    /**
     * Get the Sizes table for comparison number compNdx
     * @param compNdx  comparison index
     * @return Sizes table wrapped into a JScrollPane
     */
    private JPanel getSizesPane(int compNdx) {
        if (compNdx < 0 || compNdx >= sizesPanes.length) return null;        
        if (sizesPanes[compNdx] == null)
            sizesPanes[compNdx] = new SizeMismatchPane(mismatches[compNdx]);
        return sizesPanes[compNdx];
    }        
    
    /**
     * Notify Comparisons Pane that the tree selection has changed. The supplied 
     * TreeNode was either added (added is true) or removed (added is false) 
     * from the current selection
     * @param node  a TreeNode whose state has changed
     * @param added  true if the node was added, false otherwise
     */
    public void treeSelectionChanged(TreeNode node, boolean added) {
        if (node == null) return;
        int type = node.type;
        
        if (type == TreeNode.TITLE) return;
        
        if (type == TreeNode.PARTLEAF || type == TreeNode.WIRELEAF) {
            node = node.getParent();
            type = node.type;
        }
        
        if (!added) {  // if node is removed from selection
            if (type == TreeNode.PART || type == TreeNode.WIRE)
                curEqRecNodes.remove(node);
            else
                curExlusiveNodes.remove(node);
        } else {  // if node is added to selection
            if (type == TreeNode.PART || type == TreeNode.WIRE)
                curEqRecNodes.add(node);
            else
                curExlusiveNodes.add(node);
        }
    }
    
    /**
     * Update right pane. Content of the pane depends on the current tree selection
     * and should be updated every time the selection changes.
     */
    public void updateRightPane() {
        int divPos = getDividerLocation();        
        if (curExlusiveNodes.size() > 0) {  // if an exclusive node is selected
            // get the first node (it is selected for the longest time)
            TreeNode exNode = (TreeNode)curExlusiveNodes.firstElement();
            int exType = exNode.type;
            switch (exType) {
                case TreeNode.COMP_TITLE: 
                    dispOnRight = COMP_SUMMARY;
                    displayComparisonSummary(exNode.compNdx);
                    break;                    
                case TreeNode.EXPORTS:
                    dispOnRight = EXPORTS;
                    setRightComponent(getExportsPane(exNode.compNdx));
                    break;
                case TreeNode.SIZES:
                    dispOnRight = SIZES;
                    setRightComponent(getSizesPane(exNode.compNdx));
                    break;
                case TreeNode.EXPORT_ASSERTS:
                    dispOnRight = EXPORT_ASSERTS;
                    setRightComponent(getExportAssertionPane(exNode.compNdx));
                    break;
                case TreeNode.EXPORT_NET_CONF:
                    dispOnRight = EXPORT_NET_CONF;
                    setRightComponent(getExportNetConflictPane(exNode.compNdx));
                    break;  
                case TreeNode.EXPORT_CHR_CONF:
                    dispOnRight = EXPORT_CHR_CONF;
                    setRightComponent(getExportChrConflictPane(exNode.compNdx));
                    break;
                case TreeNode.UNRECOG_PART:
                    dispOnRight = UNRECOG_PART;
                    setRightComponent(getUnrecognizedPartsPane(exNode.compNdx));
                    break;                                        
            }
            setDividerLocation(divPos); // restore divider position
            return;
        } else if (curEqRecNodes.size() == 0) {  // if no equiv. class is selected
            resetRightPane();  // display an empty right pane
            return;            
        }
        
        // the right pane will display one or more equiv. classes
        dispOnRight = PARTS_WIRES;
        
        // get equiv. classes to be displayed
        curEqRecNodesToDisplay.clear();
        int i = 0;
        for (Iterator<TreeNode> it=curEqRecNodes.iterator(); it.hasNext() && i<MAX_CONCUR_EQ_RECS;) {
            TreeNode eqRecNode = it.next();
            if (curEqRecNodesToDisplay.contains(eqRecNode)) continue; // skip if already displayed
            curEqRecNodesToDisplay.add(eqRecNode);
            i++;
        }
        // get a pane with proper number of rows
        EquivClassSplitPane rightSplPane = rightSplPanes[curEqRecNodesToDisplay.size()-1]; 
        i = 0;
        // fill pane with data row by row 
        for (Iterator<TreeNode> it=curEqRecNodesToDisplay.iterator(); it.hasNext(); i++) {
            TreeNode eqRecNode = it.next();
            String partitionTitle = eqRecNode.getParent().getShortName() + " : "
                                  + eqRecNode.getShortName();
            rightSplPane.setPartitionTitle(i, partitionTitle);
            List<NetObjReport>[] mism = new ArrayList[2];

            EquivRecReport eqRec = mismEqRecs[eqRecNode.compNdx][eqRecNode.eclass];
            mism[0] = eqRec.getNotMatchedNetObjs().get(0);
            mism[1] = eqRec.getNotMatchedNetObjs().get(1);
            mismNetObjs[eqRecNode.compNdx][eqRecNode.eclass] = mism;
            List<NetObjReport>[] matched = new ArrayList[2]; 
            matched[0] = eqRec.getMatchedNetObjs().get(0);
            matched[1] = eqRec.getMatchedNetObjs().get(1);
            matchedNetObjs[eqRecNode.compNdx][eqRecNode.eclass] = matched;

//            if (mismatches[eqRecNode.compNdx].isHashFailuresPrinted())
//                fillHashPartitionResults(eqRecNode, rightSplPane, i);
//            else
                fillLocalPartitionResults(eqRecNode, rightSplPane, i);
        }
        setRightComponent(rightSplPane);
        setDividerLocation(divPos);
        rightSplPane.updateLayout();
    }
    
    /**
     * Fill the provided split cell with parts or wires from compared cells that 
     * belong to the local partitioning equivalence class identified by 
     * the provided tree node.  
     * @param node  tree node
     * @param pane  split pane
     * @param row  row in the split pane (>1 rows if >1 node selected)
     */
    private void fillLocalPartitionResults(TreeNode node, EquivClassSplitPane pane, int row) {
        int swap = 0;
        if (mismatches[node.compNdx].isSwapCells()) swap = 1;
        List<NetObjReport>[] matched = matchedNetObjs[node.compNdx][node.eclass];
        List<NetObjReport>[] mism = mismNetObjs[node.compNdx][node.eclass];
        
        String href = "<a style=\"text-decoration: none\" href=\"";
        StringBuffer html = new StringBuffer(256);
        for (int cell=0; cell<2; cell++) {
            int ndx = (cell + swap)%2;
            StringBuffer curCellText = pane.getCellPlainTextBuffer(row,ndx);
            curCellText.setLength(0);
            html.setLength(0);
            // fonts: Courier, Dialog, Helvetica, TimesRoman, Serif
            html.append("<html><FONT SIZE=3><FONT FACE=\"Helvetica, TimesRoman\">");
            
            // mismatched netobjects (printed in red)
            if (mism[ndx].size() > 0) {
                html.append("<font COLOR=\"red\">");
                for (int k=0; k<mism[ndx].size() && k<ComparisonsTree.MAX_LIST_ELEMENTS; k++) {
                    String descr = cleanNetObjectName(
                               (mism[ndx].get(k)).instanceDescription());
                    
                    html.append(href + (row*100000 + ndx*10000 + k) +"\">"+ descr + "</a>");
                    curCellText.append(descr);
                    html.append("<br>");
                    curCellText.append(LSEP);
                }
                html.append("</font>");
            }
            // if this cell has fewer mismatches than the other one
            // and matches are going to printed in this cell, then add empty lines
            int sizeDiff = mism[(ndx+1)%2].size() - mism[ndx].size();
            if (matched[ndx].size() > 0) while (sizeDiff-- > 0) {
                html.append("<br>");
                curCellText.append(LSEP);
            }
           
            // matched netobjects  (printed in green)
            if (matched[ndx].size() > 0) {
                html.append("<font COLOR=\"green\">");
                for (int k=0; k<matched[ndx].size() && k<ComparisonsTree.MAX_LIST_ELEMENTS; k++) {
                    String descr = cleanNetObjectName(
                               (matched[ndx].get(k)).instanceDescription());
                    
                    html.append(href + (row*100000 + ndx*10000 + (mism[ndx].size()+k)) 
                                + "\">"+ descr + "</a>");
                    curCellText.append(descr);
                    html.append("<br>");
                    curCellText.append(LSEP);
                }
                html.append("</font>");
            }
            int len = mism[ndx].size() + matched[ndx].size();
            // if nothing was printed, then print "none"
            if (len == 0) {
                html.append("<b>none</b>");
                curCellText.append("none");
            }
            
            html.append("</font></html>");
            pane.setCellText(row,ndx,html.toString());
                        
            String title = mismatches[node.compNdx].getNames()[ndx];
            if (node.type == TreeNode.WIRE)
                pane.setLabelText(row,ndx, "  "+ len +" Wire(s) in " + title);
            else
                pane.setLabelText(row,ndx, "  "+ len +" Part(s) in " + title);
        }
    }

//    /**
//     * Fill the provided split cell with parts or wires from compared cells that 
//     * belong to the hashcode equivalence class identified by the provided tree node.  
//     * @param node  tree node
//     * @param pane  split pane
//     * @param row  row in the split pane (>1 rows if >1 node selected)
//     */
//    private void fillHashPartitionResults(TreeNode node, EquivClassSplitPane pane, int row) {
//        int swap = 0;
//        if (mismatches[node.compNdx].isSwapCells()) swap = 1;
//        EquivRecReport eqRec = mismEqRecs[node.compNdx][node.eclass];
//        
//        String href = "<a style=\"text-decoration: none\" href=\"";
//        StringBuffer html = new StringBuffer(256);
//        int cell = 0;
//        for (List<NetObjReport> ckt : eqRec.getNotMatchedNetObjs()) {
//            int ndx = (cell + swap)%2;
//            StringBuffer curCellText = pane.getCellPlainTextBuffer(row,ndx);
//            curCellText.setLength(0);
//            html.setLength(0);
//            // fonts: Courier, Dialog, Helvetica, TimesRoman, Serif
//            html.append("<html><FONT SIZE=3><FONT FACE=\"Helvetica, TimesRoman\">");
//            
//            int len = ckt.size();
//            int k=0;
//            for (NetObjReport r : ckt) {
//            	if (k>=ComparisonsTree.MAX_LIST_ELEMENTS) break;
//                String descr = cleanNetObjectName(r.instanceDescription());
//                
//                html.append(href + (row*100000 + cell*10000 + k) +"\">"+ descr + "</a>");
//                curCellText.append(descr);
//                html.append("<br>");
//                curCellText.append(LSEP);
//            	k++;
//            }
//            if (len == 0) {
//                html.append("<b>none</b>");
//                curCellText.append("none");
//            }
//            
//            html.append("</font></html>");
//            pane.setCellText(row,ndx,html.toString());
//                        
//            String title = mismatches[node.compNdx].getNames()[ndx];
//            if (node.type == TreeNode.WIRE)
//                pane.setLabelText(row,ndx, "  "+ len +" Wire(s) in " + title);
//            else
//                pane.setLabelText(row,ndx, "  "+ len +" Part(s) in " + title);
//        }
//        cell++;
//    }
    
    /**
     * Display number of parts, wires, ports in the cells in comparison
     * number compNdx
     * @param compNdx  comparison index
     */
    private void displayComparisonSummary(int compNdx) {
        EquivClassSplitPane rightSplPane = rightSplPanes[0];
        Cell cells[] = mismatches[compNdx].getCells();
        NccResult.CellSummary summary = mismatches[compNdx].getCellSummary();
        StringBuffer html = new StringBuffer(256);
        int swap = 0;
        if (mismatches[compNdx].isSwapCells()) swap = 1;
        for (int i=0; i<2; i++) {
            int ndx = (i + swap)%2;
            StringBuffer curCellText = rightSplPane.getCellPlainTextBuffer(0,ndx);
            curCellText.setLength(0);
            html.setLength(0);
            html.append("<html><FONT SIZE=3><FONT FACE=\"Helvetica, TimesRoman\">");
            if (summary.cantBuildNetlist[ndx]) {
            	html.append(/*" Can't build netlist!<br>" +*/
            			    "  See problems listed<br>in tree pane.<br>(left most pane). ");
            	curCellText.append(/*" Can't build netlist!" + LSEP + */
            			           "  See problems listed"+LSEP+"in tree pane."+LSEP+"(left most pane). ");
            } else {
            	html.append(summary.numParts[ndx] + " Parts<br>"
            				+ summary.numWires[ndx] + " Wires<br>"
							+ summary.numPorts[ndx] + " Ports<br>");
                curCellText.append(summary.numParts[ndx] + " Parts" + LSEP
                        	+ summary.numWires[ndx] + " Wires" + LSEP
							+ summary.numPorts[ndx] + " Ports");
            }
            html.append("</font></html>");
            rightSplPane.setCellText(0, ndx, html.toString());

            CellName cellName = cells[i].getCellName();
            String name = "  Summary of " + cellName.getName() + " {" 
                        + cellName.getView().getAbbreviation() + "}";
            rightSplPane.setLabelText(0, ndx, name);
        }
        setRightComponent(rightSplPane);
    }
    
    /**
     * Follow hyperlink with the given index and highlight required items
     * @param index  hyperlink index
     */
    void highlight(int index) {
        int recNdx = index/100000;
        int cellNdx = (index/10000)%10;
        int line = index%10000;
        TreeNode eqRecNode = (TreeNode)curEqRecNodesToDisplay.elementAt(recNdx);

        // in case of hashcode partitions, get NetObject from Circuits
        NetObjReport partOrWire = null;
//        if (mismatches[eqRecNode.compNdx].isHashFailuresPrinted()) {
//            EquivRecReport eqRec = mismEqRecs[eqRecNode.compNdx][eqRecNode.eclass];
//            int c = 0, k = 0;
//            Circuit ckt = null;
//            for (Iterator<Circuit> it=eqRec.getCircuits(); it.hasNext(); c++, it.next())
//                if (c == cellNdx) {
//                    ckt = it.next(); 
//                    break;
//                }
//            for (Iterator<NetObjReport> it=ckt.getNetObjs(); it.hasNext(); k++, it.next())
//                if (k == line) {
//                    partOrWire = it.next();
//                    break;
//                }
//        } else { // in case of LP, get the NetObjeect from the array
            List<NetObjReport>[] mism = mismNetObjs[eqRecNode.compNdx][eqRecNode.eclass];
            List<NetObjReport>[] matched = matchedNetObjs[eqRecNode.compNdx][eqRecNode.eclass];
            if (line >= mism[cellNdx].size()) 
                partOrWire = matched[cellNdx].get(line - mism[cellNdx].size());
            else
                partOrWire = mism[cellNdx].get(line);
//        }
        
        Cell cell = null;
        VarContext context = null;
        if (partOrWire instanceof PartReport) {
            cell = ((PartReport)partOrWire).getNameProxy().leafCell();
            context = ((PartReport)partOrWire).getNameProxy().getContext();
        } else if (partOrWire instanceof WireReport) {
            cell = ((WireReport)partOrWire).getNameProxy().leafCell();
            context = ((WireReport)partOrWire).getNameProxy().getContext();
        }
        
        // find the highlighter corresponding to the cell
        Highlighter highlighter = HighlightTools.getHighlighter(cell, context);
        if (highlighter == null) return;

        if (partOrWire instanceof PartReport)
            HighlightTools.highlightPart(highlighter, cell, (PartReport)partOrWire);
        else if (partOrWire instanceof WireReport)
            HighlightTools.highlightWire(highlighter, cell, (WireReport)partOrWire);
        
        highlighter.finished();
    }
    
    /**
     * Remove unnecessary parts from a NetObject name 
     * @param descr  NetObject name
     * @return cleaned name 
     */
    public String cleanNetObjectName(String descr) {
        // drop "Part:" or "Wire:" prefices
        if (descr.startsWith("Wire: ") || descr.startsWith("Part: "))
            descr = descr.substring(6);
        // drop "Cell instance:" info
        int ind = descr.indexOf(" Cell instance:");
        if (ind > 0) descr = descr.substring(0, ind).trim();
        // drop {sch} or {lay} suffices
        if (descr.endsWith("{sch}") || descr.endsWith("{lay}"))
            descr = descr.substring(0, descr.length()-5);
        return descr;
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

    /**
     * Initialize popup menu for table cells
     */
    private void createCellPopup() {
        cellPopup = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Copy Cell Text To Clipboard");
        menuItem.addActionListener(this);
        cellPopup.add(menuItem);
    }

    /**
     * Display a cell popup on top of Component c, with origin at (x,y) 
     * in component's coord system
     * @param text  The text to be copied to clipboard if menu item is activated
     * @param c  Component to place the popup menu on top of 
     * @param x  x origin coordinate (in component coord system)
     * @param y  y origin coordinate (in component coord system)
     */
    void showCellPopup(String text, Component c, int x, int y) {
        clipboard = text;
        cellPopup.show(c,x,y);
    }   
}
