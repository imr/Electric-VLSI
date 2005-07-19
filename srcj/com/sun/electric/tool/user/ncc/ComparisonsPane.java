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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Part;
import com.sun.electric.tool.ncc.netlist.Wire;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.ncc.ComparisonsTree.TreeNode;
import com.sun.electric.tool.user.ncc.NccComparisonMismatches.CellSummary;

class ComparisonsPane extends JSplitPane implements ActionListener {
    
    /* --- constants --- */
    private static final int EMPTY = 0;
    private static final int COMP_SUMMARY = 1;
    private static final int EXPORTS = 2;
    private static final int PARTS_WIRES = 3;
    private static final int SIZES = 4;
    private static final int EXPORT_ASSERTS = 5;
    private static final int EXPORT_NET_CONF = 6;
    private static final int EXPORT_CHR_CONF = 7;
    private static final int UNRECOG_MOS = 8;
    
    private static final int MAX_CONCUR_EQ_RECS = 5;
    private static final String emptyStr = " ";
    private static final String LSEP = System.getProperty("line.separator");
    
    /* --- GUI variables --- */
    String defaultTitles[] = {emptyStr, emptyStr};
    private String treeTitle = "  Mismatched Comparisons";
    private JLabel treeLabel = new JLabel(treeTitle);
    private ComparisonsTree tree;
    private JScrollPane treeScrollPane;
    private int dispOnRight = EMPTY;
    
    private static Border border = BorderFactory.createEmptyBorder();
    
    private JScrollPane exportsPanes[];    
    private JScrollPane exportAssertionsPanes[];
    private JScrollPane exportNetConflictPanes[];
    private JScrollPane exportChrConflictPanes[];
    private JScrollPane unrecognizedMOSPanes[];
    private EquivClassSplitPane rightSplPanes[];
    
    private JPanel sizesPanes[];
    protected JPopupMenu treePopup;
    protected JPopupMenu cellPopup;
    protected String clipboard;
    
    /* --- Data holders --- */
    private NccComparisonMismatches mismatches[];
    private EquivRecord mismEqRecs[][];
    private Vector curEqRecNodes = new Vector();  // vector of TreeNode
    private Vector curEqRecNodesToDisplay = new Vector();
    // Exclusive nodes are those requiring exclusive access to the right pane.
    // Any node except for EquivRecord and TITLE is an exclusive one.
    private Vector curExlusiveNodes = new Vector(); // vector of TreeNode
    
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
    
    public void setMismatches(List misms) {
        mismatches = 
            (NccComparisonMismatches[])misms.toArray(new NccComparisonMismatches[0]);
        mismEqRecs = new EquivRecord[mismatches.length][];
        exportsPanes = new JScrollPane[mismatches.length];
        exportAssertionsPanes = new JScrollPane[mismatches.length];
        exportNetConflictPanes = new JScrollPane[mismatches.length];
        exportChrConflictPanes = new JScrollPane[mismatches.length];
        unrecognizedMOSPanes = new JScrollPane[mismatches.length];
        sizesPanes = new JPanel[mismatches.length];
        
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
    
    void setMismatchEquivRecs(int compNdx, EquivRecord[] equivRecs) {
        mismEqRecs[compNdx] = equivRecs;
    }

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
    
    private JScrollPane getExportsPane(int compNdx) {
        if (compNdx < 0 || compNdx >= exportsPanes.length) return null;
        if (exportsPanes[compNdx] == null) {
            ExportMismatchTable table = new ExportMismatchTable(mismatches[compNdx]);
            exportsPanes[compNdx] = new JScrollPane(table);
            exportsPanes[compNdx].setBackground(Color.WHITE);
        }
        return exportsPanes[compNdx];
    }

    private JScrollPane getExportAssertionPane(int compNdx) {
        if (compNdx < 0 || compNdx >= exportAssertionsPanes.length) return null;
        if (exportAssertionsPanes[compNdx] == null) {
            ExportAssertionTable table = new ExportAssertionTable(mismatches[compNdx]);
            exportAssertionsPanes[compNdx] = new JScrollPane(table);
            exportAssertionsPanes[compNdx].setBackground(Color.WHITE);
        }
        return exportAssertionsPanes[compNdx];
    }    

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

    private JScrollPane getUnrecognizedMOSPane(int compNdx) {
        if (compNdx < 0 || compNdx >= unrecognizedMOSPanes.length) return null;
        if (unrecognizedMOSPanes[compNdx] == null) {
            UnrecognizedMOSTable table = new UnrecognizedMOSTable(mismatches[compNdx]);
            unrecognizedMOSPanes[compNdx] = new JScrollPane(table);
            unrecognizedMOSPanes[compNdx].setBackground(Color.WHITE);
        }
        return unrecognizedMOSPanes[compNdx];
    }    
    
    private JPanel getSizesPane(int compNdx) {
        if (compNdx < 0 || compNdx >= sizesPanes.length) return null;        
        if (sizesPanes[compNdx] == null)
            sizesPanes[compNdx] = new SizeMismatchPane(mismatches[compNdx]);
        return sizesPanes[compNdx];
    }        
    
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

    public void updateRightPane() {
        int divPos = getDividerLocation();        
        if (curExlusiveNodes.size() > 0) {
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
                case TreeNode.UNRECOG_MOS:
                    dispOnRight = UNRECOG_MOS;
                    setRightComponent(getUnrecognizedMOSPane(exNode.compNdx));
                    break;                                        
            }
            setDividerLocation(divPos);
            return;
        } else if (curEqRecNodes.size() == 0) {
            resetRightPane();
            return;            
        }
         
        dispOnRight = PARTS_WIRES;

        curEqRecNodesToDisplay.clear();
        int i = 0;        
        for (Iterator it=curEqRecNodes.iterator(); it.hasNext() && i<MAX_CONCUR_EQ_RECS;) {
            TreeNode eqRecNode = (TreeNode)it.next();
            if (curEqRecNodesToDisplay.contains(eqRecNode)) continue; // if not already displayed
            curEqRecNodesToDisplay.add(eqRecNode);
            i++;
        }
        EquivClassSplitPane rightSplPane = rightSplPanes[curEqRecNodesToDisplay.size()-1]; 
        i = 0;
        for (Iterator it=curEqRecNodesToDisplay.iterator(); it.hasNext(); i++) {
            TreeNode eqRecNode = (TreeNode)it.next();
            EquivRecord eqRec = mismEqRecs[eqRecNode.compNdx][eqRecNode.eclass];
            String partitionTitle = eqRecNode.getParent().getShortName() + " : "
                                  + eqRecNode.getShortName();
            rightSplPane.setPartitionTitle(i, partitionTitle);
            
            int swap = 0;
            if (mismatches[eqRecNode.compNdx].isSwapCells()) swap = 1;
            
            String href = "<a style=\"text-decoration: none\" href=\"";
            StringBuffer html = new StringBuffer(256);
            int cell = 0;
            for (Iterator it2=eqRec.getCircuits(); it2.hasNext(); cell++) {
                int ndx = (cell + swap)%2;
                StringBuffer curCellText = rightSplPane.getCellPlainTextBuffer(i,ndx);
                curCellText.setLength(0);
                html.setLength(0);
                // Other fonts: Courier, Dialog, Helvetica, TimesRoman, Serif
                html.append("<html><FONT SIZE=3><FONT FACE=\"Helvetica, TimesRoman\">");
                
                Circuit ckt = (Circuit) it2.next();
                int len = ckt.numNetObjs();
    
                Iterator it3=ckt.getNetObjs();
                for (int k=0; it3.hasNext() && k<ComparisonsTree.MAX_LIST_ELEMENTS; k++) {
                    String descr = cleanNetObjectName(
                               ((NetObject) it3.next()).instanceDescription());
                    
                    html.append(href + (i*100000 + cell*10000 + k) +"\">"+ descr + "</a>");
                    curCellText.append(descr);
                    if (it3.hasNext()) {
                        html.append("<br>");
                        curCellText.append(LSEP);
                    }
                }
                if (len == 0) {
                    html.append("<b>none</b>");
                    curCellText.append("none");
                }
                
                html.append("</font></html>");
                rightSplPane.setCellText(i,ndx,html.toString());
                            
                String title = mismatches[eqRecNode.compNdx].getNames()[ndx];
                if (eqRecNode.type == TreeNode.WIRE)
                    rightSplPane.setLabelText(i,ndx, "  "+ len +" Wire(s) in " + title);
                else
                    rightSplPane.setLabelText(i,ndx, "  "+ len +" Part(s) in " + title);
            }
        }
        setRightComponent(rightSplPane);
        setDividerLocation(divPos);
        rightSplPane.updateLayout();
    }

    private void displayComparisonSummary(int compNdx) {
        EquivClassSplitPane rightSplPane = rightSplPanes[0];
        Cell cells[] = mismatches[compNdx].getCells();
        CellSummary summary = mismatches[compNdx].getCellSummary();
        StringBuffer html = new StringBuffer(256);
        int swap = 0;
        if (mismatches[compNdx].isSwapCells()) swap = 1;
        for (int i=0; i<2; i++) {
            int ndx = (i + swap)%2;
            StringBuffer curCellText = rightSplPane.getCellPlainTextBuffer(0,ndx);
            curCellText.setLength(0);
            html.setLength(0);
            html.append("<html><FONT SIZE=3><FONT FACE=\"Helvetica, TimesRoman\">");
            html.append(summary.numParts[ndx] + " Parts<br>"
                      + summary.numWires[ndx] + " Wires<br>"
                      + summary.numPorts[ndx] + " Ports");
            curCellText.append(summary.numParts[ndx] + " Parts" + LSEP
                             + summary.numWires[ndx] + " Wires" + LSEP
                             + summary.numPorts[ndx] + " Ports");
            html.append("</font></html>");
            rightSplPane.setCellText(0, ndx, html.toString());

            CellName cellName = cells[i].getCellName();
            String name = "  Summary of " + cellName.getName() + " {" 
                        + cellName.getView().getAbbreviation() + "}";
            rightSplPane.setLabelText(0, ndx, name);
        }
        setRightComponent(rightSplPane);
    }
    
    void highlight(int index) {
        int recNdx = index/100000;
        int cellNdx = (index/10000)%10;
        int line = index%10000;
        TreeNode eqRecNode = (TreeNode)curEqRecNodesToDisplay.elementAt(recNdx);
        EquivRecord eqRec = mismEqRecs[eqRecNode.compNdx][eqRecNode.eclass];

        //if (mismatches[compNdx].isSwapCells()) cellNdx = (cellNdx+1)%2;
        int c = 0, k = 0;
        Circuit ckt = null;
        for (Iterator it=eqRec.getCircuits(); it.hasNext(); c++, it.next())
            if (c == cellNdx) {
                ckt = (Circuit) it.next(); 
                break;
            }
        NetObject partOrWire = null;
        for (Iterator it=ckt.getNetObjs(); it.hasNext(); k++, it.next())
            if (k == line) {
                partOrWire = (NetObject)it.next();
                break;
            }
        
        Cell cell = null;
        VarContext context = null;
        if (partOrWire instanceof Part) {
            cell = ((Part)partOrWire).getNameProxy().leafCell();
            context = ((Part)partOrWire).getNameProxy().getContext();
        } else if (partOrWire instanceof Wire) {
            cell = ((Wire)partOrWire).getNameProxy().leafCell();
            context = ((Wire)partOrWire).getNameProxy().getContext();
        }
        
        // find the highlighter corresponding to the cell
        Highlighter highlighter = HighlightTools.getHighlighter(cell, context);
        if (highlighter == null) return;

        if (partOrWire instanceof Part)
            HighlightTools.highlightPart(highlighter, cell, (Part)partOrWire);
        else if (partOrWire instanceof Wire)
            HighlightTools.highlightWire(highlighter, cell, (Wire)partOrWire);
        
        highlighter.finished();
    }
    
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

    private void createCellPopup() {
        cellPopup = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Copy Cell Text To Clipboard");
        menuItem.addActionListener(this);
        cellPopup.add(menuItem);
    }

    void showCellPopup(String text, Component c, int x, int y) {
        clipboard = text;
        cellPopup.show(c,x,y);
    }   
}
