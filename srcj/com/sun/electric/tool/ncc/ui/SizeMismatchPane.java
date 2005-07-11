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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.netlist.Mos;
import com.sun.electric.tool.ncc.strategy.StratCheckSizes;
import com.sun.electric.tool.user.Highlighter;

class SizeMismatchPane extends JPanel implements HyperlinkListener, AdjustmentListener {
    // constants
    public static final int MAXROWS = 200;
    private static final Border border = BorderFactory.createEmptyBorder();
    private static final Insets insets = new Insets(0,0,0,0);
    
    // GUI variables
    private Dimension dimErrCol, dimWidCol, dimLenCol;
    private JScrollPane headScrPane;
    
    // data holders
    private NccComparisonMismatches result;
    private StratCheckSizes.Mismatch[] mismatches;
    private Mos[][] moses;
    
    public SizeMismatchPane(NccComparisonMismatches res) {
        super(new BorderLayout());
        
        result = res;
        mismatches = (StratCheckSizes.Mismatch[])result.getSizeMismatches()
                                     .toArray(new StratCheckSizes.Mismatch[0]);
        int size = Math.min(mismatches.length, MAXROWS);
        if (size == 0) return;
        moses = new Mos[size][2];
        
        // compute max numbers to estimate column width
        int errColWidth = 7, widColWidth = 3, lenColWidth = 3;
        for (int i=0; i<size; i++) {
            String err = NccUtils.round(mismatches[i].relErr()*100,1) + "";
            errColWidth = Math.max(errColWidth, err.length());
            
            String w1 = NccUtils.round(mismatches[i].minMos.getWidth(),2) + "";
            String w2 = NccUtils.round(mismatches[i].maxMos.getWidth(),2) + "";
            int wid = Math.max(w1.length(), w2.length());
            widColWidth = Math.max(widColWidth, wid+1);
            
            String l1 = NccUtils.round(mismatches[i].minMos.getLength(),2) + "";
            String l2 = NccUtils.round(mismatches[i].maxMos.getLength(),2) + "";
            int len = Math.max(l1.length(), l2.length());
            lenColWidth = Math.max(lenColWidth, len+1);
        }
        dimErrCol = new Dimension(errColWidth*7, 20);        
        dimWidCol = new Dimension(widColWidth*7, 32);
        dimLenCol = new Dimension(lenColWidth*7, 32);
        
        // create scroll pane with rows 
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        Box topBox = new Box(BoxLayout.Y_AXIS);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        int maxRowWid = 0;
        for (int i=0; i<size; i++) {
            // add a row and a separator below it
            JPanel row = createRow(i);
            maxRowWid = Math.max(maxRowWid, row.getPreferredSize().width + 16);
            topBox.add(row);
            topBox.add(Box.createVerticalStrut(2));
            topBox.add(sep);
            topBox.add(Box.createVerticalStrut(2));
        }
        
        JScrollPane bodyScrPane = new JScrollPane(topBox);
        bodyScrPane.setBorder(border);
        bodyScrPane.setAlignmentX(LEFT_ALIGNMENT);
        bodyScrPane.setAlignmentY(TOP_ALIGNMENT);
        bodyScrPane.getHorizontalScrollBar().addAdjustmentListener(this);
        JScrollBar sbar = bodyScrPane.getVerticalScrollBar(); 
        sbar.setValue(sbar.getMinimum());
        
        // fill main container with header panel, separator, and scroll pane
        JPanel header = createHeader();
        Dimension dim = header.getPreferredSize();
        dim.width = maxRowWid;
        header.setPreferredSize(dim);
        Box headBox = new Box(BoxLayout.Y_AXIS);
        headBox.add(header);
        headBox.add(Box.createVerticalStrut(2));
        headBox.add(sep);
        headBox.add(Box.createVerticalStrut(2));
        headScrPane = new JScrollPane(headBox, 
                          ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        headScrPane.setBorder(border);
        headScrPane.setAlignmentX(LEFT_ALIGNMENT);
        headScrPane.setAlignmentY(TOP_ALIGNMENT);
        
        add(headScrPane, BorderLayout.NORTH);
        add(bodyScrPane, BorderLayout.CENTER);
        setBorder(border);
    }

    private JPanel createHeader() {
        // create the main container of this row
        JPanel row = new JPanel(new BorderLayout());
        Color bkgndColor = row.getBackground();
        
        // create panel with relative error title
        JLabel errLabel = new JLabel("Error,%");
        errLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        errLabel.setMinimumSize(dimErrCol);
        errLabel.setMaximumSize(dimErrCol);
        errLabel.setPreferredSize(dimErrCol);
        errLabel.setFont(new Font("Helvetica", Font.PLAIN, 12));
        errLabel.setBorder(border);
        
        // set up the panel with numeric information (error, width, length)
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.add(Box.createHorizontalStrut(4));
        leftPanel.add(errLabel);
        leftPanel.add(Box.createHorizontalStrut(1));
        leftPanel.add(new JSeparator(SwingConstants.VERTICAL));
        leftPanel.add(Box.createHorizontalStrut(5));
        
        // create pane with width data
        String params = "W1 <br>W2 ";
        JEditorPane widPane = createParamPane(params, false, dimWidCol); 
        widPane.setBackground(bkgndColor);
        leftPanel.add(widPane);
        leftPanel.add(Box.createHorizontalStrut(5));
        
        // create pane with length data
        params = "L1 <br>L2 ";
        JEditorPane lenPane = createParamPane(params, false, dimLenCol); 
        lenPane.setBackground(bkgndColor);
        leftPanel.add(lenPane);
        leftPanel.add(Box.createHorizontalStrut(10));
        
        // add numeric pane to the main container
        row.add(leftPanel, BorderLayout.WEST);
        // create and add the pane with clickable names 
        String descr[] = {"Name 1", "Name 2"};
        JEditorPane namesPane = createNamesPane(descr, -1);
        namesPane.setBackground(bkgndColor);
        row.add(namesPane, BorderLayout.CENTER);

        // restrict height of the row
        Dimension dim = new Dimension(row.getPreferredSize());
        dim.width = Integer.MAX_VALUE;
        row.setMaximumSize(dim);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setAlignmentY(TOP_ALIGNMENT);
        return row;
    }
    
    private JPanel createRow(int rowNdx) {
        if (rowNdx < 0) return createHeader();
        
        // create the main container of this row
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        
        int firstCellNdx = 0;
        if (result.isSwapCells()) firstCellNdx = 1;
        if (mismatches[rowNdx].minNdx == firstCellNdx) {
            moses[rowNdx][0] = mismatches[rowNdx].minMos;
            moses[rowNdx][1] = mismatches[rowNdx].maxMos;
        } else {
            moses[rowNdx][1] = mismatches[rowNdx].minMos;
            moses[rowNdx][0] = mismatches[rowNdx].maxMos;
        }
        
        // create panel with relative error value
        String relErr;
        if (mismatches[rowNdx].relErr()*100<.1)
            relErr = "< 0.01";
        else
            relErr = NccUtils.round(mismatches[rowNdx].relErr()*100, 1) + "";            
        JLabel errLabel = new JLabel(relErr);
        errLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        errLabel.setMinimumSize(dimErrCol);
        errLabel.setMaximumSize(dimErrCol);
        errLabel.setPreferredSize(dimErrCol);
        errLabel.setFont(new Font("Helvetica", Font.PLAIN, 12));
        errLabel.setBorder(border);
        
        // set up the panel with numeric information (error, width, length)
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.setBackground(Color.WHITE);            
        leftPanel.add(errLabel);
        leftPanel.add(Box.createHorizontalStrut(5));
        leftPanel.add(new JSeparator(SwingConstants.VERTICAL));
        leftPanel.add(Box.createHorizontalStrut(5));
        
        // create pane with width data
        boolean red = mismatches[rowNdx] instanceof StratCheckSizes.WidthMismatch;
        String params = NccUtils.round(moses[rowNdx][0].getWidth(),2) + "<br>" 
                      + NccUtils.round(moses[rowNdx][1].getWidth(),2) + " ";
        leftPanel.add(createParamPane(params, red, dimWidCol));
        leftPanel.add(Box.createHorizontalStrut(5));
        
        // create pane with length data
        red = mismatches[rowNdx] instanceof StratCheckSizes.LengthMismatch;
        params = NccUtils.round(moses[rowNdx][0].getLength(),2) + " <br>" 
               + NccUtils.round(moses[rowNdx][1].getLength(),2) + " ";
        leftPanel.add(createParamPane(params, red, dimLenCol));
        leftPanel.add(Box.createHorizontalStrut(10));
        
        // add numeric pane to the main container
        row.add(leftPanel, BorderLayout.WEST);
        // create and add the pane with clickable names 
        String descr[] = {moses[rowNdx][0].instanceDescription(), 
                          moses[rowNdx][1].instanceDescription()};
        row.add(createNamesPane(descr, rowNdx), BorderLayout.CENTER);
        
        // restrict height of the row
        Dimension dim = new Dimension(row.getPreferredSize());
        dim.width = Integer.MAX_VALUE;
        row.setMaximumSize(dim);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setAlignmentY(TOP_ALIGNMENT);
        return row;
    }
    
    private JEditorPane createParamPane(String params, boolean red, Dimension dimen) {
        StringBuffer text = new StringBuffer(64);
        text.append("<html><font size=3><font face=\"Helvetica, TimesRoman\">");            
        if (red)
            text.append("<font color=\"red\">");
        text.append(params); 
        if (red)
            text.append("</font>");
        text.append("</font></html>");            
        JEditorPane pane = new JEditorPane();
        pane.setEditable(false);
        pane.setContentType("text/html"); 
        pane.setText(text.toString());
        pane.setMargin(insets);
        pane.setMinimumSize(dimen);
        pane.setMaximumSize(dimen);
        pane.setPreferredSize(dimen);
        StyledDocument doc = (StyledDocument)pane.getDocument();
        SimpleAttributeSet attr = new SimpleAttributeSet();
        StyleConstants.setAlignment(attr, StyleConstants.ALIGN_RIGHT);
        doc.setParagraphAttributes(0, doc.getLength(), attr, false);
        return pane;
    }

    private JEditorPane createNamesPane(String[] descr, int rowNdx) {
        StringBuffer text = new StringBuffer(128);
        String href = "<a style=\"text-decoration: none\" href=\"";
        text.append("<html><font size=3><font face=\"Helvetica, TimesRoman\">");
        for (int i=0; i<2; i++) {

            // drop "Part:" or "Wire:" prefices
            if (descr[i].startsWith("Wire: ") || descr[i].startsWith("Part: "))
                descr[i] = descr[i].substring(6);
            // drop "Cell instance:" info
            int ind = descr[i].indexOf(" Cell instance:");
            if (ind > 0) descr[i] = descr[i].substring(0, ind).trim();
            // drop {sch} or {lay} suffices
            if (descr[i].endsWith("{sch}") || descr[i].endsWith("{lay}"))
                descr[i] = descr[i].substring(0, descr[i].length()-5);
            
            if (rowNdx >= 0)
                text.append(href + (rowNdx*10+i) +"\">"+ descr[i] +"</a>");
            else  // used for header
                text.append(descr[i]);
            if (i==0) text.append("<br>");
        }
        text.append("</font></html>");            
        
        JEditorPane pane = new JEditorPane();
        pane.setEditable(false);
        pane.addHyperlinkListener(this);
        pane.setContentType("text/html");
        pane.setText(text.toString());
        pane.setMargin(insets);
        pane.setBorder(border);
        pane.moveCaretPosition(0);
        return pane; 
    }
    
    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            highlight(Integer.parseInt(event.getDescription()));
        }
    }
    
    public void adjustmentValueChanged(AdjustmentEvent e) {
        headScrPane.getHorizontalScrollBar().setValue(e.getValue());
    }
    
    private void highlight(int index) {
        int row = index/10;
        int col = index%2;
        Mos mos = moses[row][col];
        Cell cell = result.getCells()[col];
        VarContext context = result.getContexts()[col];
        
        // find the highlighter corresponding to the cell
        Highlighter highlighter = HighlightTools.getHighlighter(cell, context);
        if (highlighter == null) return;
        HighlightTools.highlightPart(highlighter, cell, mos);
        highlighter.finished();
    }        
}

