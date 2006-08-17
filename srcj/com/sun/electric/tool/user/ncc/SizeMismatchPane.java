/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: SizeMismatchPane.java
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.result.PartReport;
import com.sun.electric.tool.ncc.result.SizeMismatch.LengthMismatch;
import com.sun.electric.tool.ncc.result.SizeMismatch.Mismatch;
import com.sun.electric.tool.ncc.result.SizeMismatch.WidthMismatch;
import com.sun.electric.tool.user.Highlighter;

class SizeMismatchPane extends JPanel implements HyperlinkListener, AdjustmentListener {
    // constants
    public static final int MAXROWS = 200;
    private static final Border border = BorderFactory.createEmptyBorder();
    private static final Insets insets = new Insets(0,0,0,0);
    private static final int HALF_INF = Integer.MAX_VALUE/2;
    
    // GUI variables
    private Dimension dimErrCol, dimWidCol, dimLenCol;
    private JScrollPane headScrPane;
    private Color bkgndColor;
    private Font font = new Font("Helvetica", Font.PLAIN, 12);
    
    // data holders
    private NccGuiInfo result;
    private Mismatch[] mismatches;
    private PartReport[][] parts;
    
    public SizeMismatchPane(NccGuiInfo res) {
        super(new BorderLayout());
        
        result = res;
        mismatches = (Mismatch[])result.getSizeMismatches()
                                     .toArray(new Mismatch[0]);
        int size = Math.min(mismatches.length, MAXROWS);
        if (size == 0) return;
        parts = new PartReport[size][2];
        
        // compute max numbers to estimate column width
        int errColWidth = 7, widColWidth = 3, lenColWidth = 3;
        for (int i=0; i<size; i++) {
            String err = NccUtils.round(mismatches[i].relErr()*100,1) + "";
            errColWidth = Math.max(errColWidth, err.length());
            
            String w1 = NccUtils.round(mismatches[i].minPart.getWidth(),2) + "";
            String w2 = NccUtils.round(mismatches[i].maxPart.getWidth(),2) + "";
            int wid = Math.max(w1.length(), w2.length());
            widColWidth = Math.max(widColWidth, wid+1);
            
            String l1 = NccUtils.round(mismatches[i].minPart.getLength(),2) + "";
            String l2 = NccUtils.round(mismatches[i].maxPart.getLength(),2) + "";
            int len = Math.max(l1.length(), l2.length());
            lenColWidth = Math.max(lenColWidth, len+1);
        }
        dimErrCol = new Dimension(errColWidth*7, 20);        
        dimWidCol = new Dimension(widColWidth*7, 16);
        dimLenCol = new Dimension(lenColWidth*7, 16);
        
        // create scroll pane with rows 
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        Box topBox = new Box(BoxLayout.Y_AXIS);
        topBox.setAlignmentY(TOP_ALIGNMENT);
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
        
        // fill main container with header panel, separator, and scroll pane
        JPanel header = createRow(-1);
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

        JScrollBar scrBar = bodyScrPane.getVerticalScrollBar();
        scrBar.invalidate();
        scrBar.validate();
        scrBar.setValue(scrBar.getMinimum());
    }
  
    private JPanel createRow(int rowNdx) {
        // create the main container of this row
        JPanel row = new JPanel(new BorderLayout());
        String relErr;        
        if (rowNdx < 0) {
            bkgndColor = row.getBackground();
            relErr = "Error,%";
        } else {
            bkgndColor = Color.WHITE;
            row.setBackground(bkgndColor);
            int firstCellNdx = 0;
            if (result.isSwapCells()) firstCellNdx = 1;
            if (mismatches[rowNdx].minNdx == firstCellNdx) {
                parts[rowNdx][0] = mismatches[rowNdx].minPart;
                parts[rowNdx][1] = mismatches[rowNdx].maxPart;
            } else {
                parts[rowNdx][1] = mismatches[rowNdx].minPart;
                parts[rowNdx][0] = mismatches[rowNdx].maxPart;
            }
            if (mismatches[rowNdx].relErr()*100<.1)
                relErr = "< 0.01";
            else
                relErr = NccUtils.round(mismatches[rowNdx].relErr()*100, 1) + "";            
        }
        
        // create panel with relative error value
        JLabel errLabel = new JLabel(relErr);
        errLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        errLabel.setMinimumSize(dimErrCol);
        errLabel.setMaximumSize(dimErrCol);
        errLabel.setPreferredSize(dimErrCol);
        errLabel.setFont(font);
        errLabel.setBorder(border);

        // set up the panel with error label and curcuit-specific data
        JPanel errPanel = new JPanel();
        errPanel.setLayout(new BoxLayout(errPanel, BoxLayout.X_AXIS));
        errPanel.add(Box.createHorizontalStrut(4));
        errPanel.add(errLabel);
        errPanel.add(Box.createHorizontalStrut(4));
        errPanel.add(new JSeparator(SwingConstants.VERTICAL));
        errPanel.add(Box.createHorizontalStrut(2));
        errPanel.setBackground(bkgndColor);
        
        // set up the panel with two rows: one per curcuit
        Box subRowsPanel = new Box(BoxLayout.Y_AXIS);
        Dimension paramDims[] = {dimWidCol, dimLenCol};
        String params[] = new String[2];
        for (int line=0; line<2; line++) {
            String name;
            if (rowNdx < 0) {
                params[0] = "Wid";
                params[1] = "Len";
                String titles[] = result.getNames();
                name = "Name in " + titles[line];
            } else {
                params[0] = NccUtils.round(parts[rowNdx][line].getWidth(),2) + ""; 
                params[1] = NccUtils.round(parts[rowNdx][line].getLength(),2) + "";
                name = parts[rowNdx][line].instanceDescription();
            }
            JPanel subRow = createSubRow(params, paramDims, name, rowNdx, line);
            subRow.setBackground(bkgndColor);
            subRowsPanel.add(subRow);
        }

        // add numeric pane to the main container
        row.add(errPanel, BorderLayout.WEST);
        row.add(subRowsPanel, BorderLayout.CENTER);
        
        // restrict height of the row
        Dimension dim = new Dimension(row.getPreferredSize());
        dim.width = HALF_INF;
        row.setMaximumSize(dim);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setAlignmentY(TOP_ALIGNMENT);
        return row;
    }
       
    private JPanel createSubRow(String params[], Dimension paramDims[],
                                String name, int rowNdx, int lineNdx) {
        //StringBuffer text = new StringBuffer(64);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setBorder(border);
        panel.setBackground(bkgndColor);
        for (int i=0; i<2; i++) {
            JLabel label = new JLabel(params[i]);
            label.setHorizontalAlignment(SwingConstants.TRAILING);
            label.setFont(font);
            if (rowNdx >= 0) {  // if not header
                boolean red = 
                    (i == 0 && mismatches[rowNdx] instanceof WidthMismatch) ||
                    (i == 1 && mismatches[rowNdx] instanceof LengthMismatch); 
                if (red)
                    label.setForeground(Color.RED);
            }
            label.setBorder(border);
            label.setMinimumSize(paramDims[i]);
            label.setMaximumSize(paramDims[i]);
            label.setPreferredSize(paramDims[i]);
            if (rowNdx >= 0) {
                label.setAlignmentY(BOTTOM_ALIGNMENT);
                label.setVerticalAlignment(SwingConstants.BOTTOM);
                label.setVerticalTextPosition(SwingConstants.BOTTOM);
            }            
            panel.add(label);
        }
        panel.add(Box.createHorizontalStrut(10));
        panel.add(createNamePane(name, rowNdx, lineNdx));
        return panel;
    }

    private JComponent createNamePane(String name, int rowNdx, int lineNdx) {
        StringBuffer text = new StringBuffer(128);
        String href = "<a style=\"text-decoration: none;\" href=\"";
        text.append("<html><font size=3><font face=\"Helvetica, TimesRoman\">");
        if (rowNdx < 0) { // used for header
            JLabel label = new JLabel(name);
            label.setFont(font);
            label.setBorder(border);
            return label;
        } 
        // drop "Part:" or "Wire:" prefices
        if (name.startsWith("Wire: ") || name.startsWith("Part: "))
            name = name.substring(6);
        // drop "Cell instance:" info
        int ind = name.indexOf(" Cell instance:");
        if (ind > 0) name = name.substring(0, ind).trim();
        // drop {sch} or {lay} suffices
        if (name.endsWith("{sch}") || name.endsWith("{lay}"))
            name = name.substring(0, name.length()-5);
        
        text.append(href + (rowNdx*10+lineNdx) +"\">"+ name +"</a>");
        text.append("</font></html>");
        
        JEditorPane pane = new JEditorPane();
        pane.setBackground(bkgndColor);
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
        PartReport part = parts[row][col];
        Cell cell = part.getNameProxy().leafCell();
        VarContext context = part.getNameProxy().getContext();
        
        // find the highlighter corresponding to the cell
        Highlighter highlighter = HighlightTools.getHighlighter(cell, context);
        if (highlighter == null) return;
        HighlightTools.highlightPart(highlighter, cell, part);
        highlighter.finished();
    }        
}

