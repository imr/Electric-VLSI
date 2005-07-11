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
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

class EquivClassSplitPane extends JSplitPane implements HyperlinkListener {

    final ComparisonsPane parent;
    private int numRows;
    private JLabel partitionTitles[];
    private JLabel cellLabels[][];  
    private CellEditorPane cells[][];
    private StringBuffer cellPlainText[][];
    private JSplitPane vertSplPanes[], horizSplPanes[];
    private CellMouseAdapter mouseAdapter = new CellMouseAdapter();    
    private static final Dimension zeroDim = new Dimension(0,10);
    
    private static Border border = BorderFactory.createEmptyBorder();
    private static Insets insets = new Insets(0,0,0,0);
    
    public EquivClassSplitPane(ComparisonsPane parent, int numRows) { 
        this.parent = parent;
        this.numRows = numRows;
        if (numRows == 1)
            createSingleRowSplitPane();
        else
            createMultiRowSplitPane();
    }
    
    private void createSingleRowSplitPane() {
        setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        setup();
        horizSplPanes[0] = this;        
        for (int i=0; i<2; i++) {
            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.add(cellLabels[0][i], BorderLayout.NORTH);            
            JPanel textPanel = new JPanel();
            textPanel.setBorder(border);
            textPanel.setBackground(Color.WHITE);       
            textPanel.add(cells[0][i]);
            JScrollPane scrPane = new JScrollPane(textPanel);
            mainPanel.add(scrPane, BorderLayout.CENTER);
            if (i == 0)
                setTopComponent(mainPanel);
            else
                setBottomComponent(mainPanel);
        }
    }    

    private void createMultiRowSplitPane() {
        setOrientation(JSplitPane.VERTICAL_SPLIT);
        setup();
        JSplitPane curVertSplit = this;
        vertSplPanes[0] = curVertSplit;
        for (int i=0; i<numRows; i++) {
            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.add(partitionTitles[i], BorderLayout.NORTH);
            JSplitPane topHorizSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            horizSplPanes[i] = topHorizSplit;            
            setupSplit(topHorizSplit); 
            for (int j=0; j<2; j++) {
                JPanel halfPanel = new JPanel(new BorderLayout());
                halfPanel.add(cellLabels[i][j], BorderLayout.NORTH);
                JPanel textPanel = new JPanel();
                textPanel.setBorder(border);
                textPanel.setBackground(Color.WHITE);       
                textPanel.add(cells[i][j]);
                JScrollPane scrPane = new JScrollPane(textPanel);
                halfPanel.add(scrPane, BorderLayout.CENTER);
                if (j == 0)
                    topHorizSplit.setLeftComponent(halfPanel);
                else
                    topHorizSplit.setRightComponent(halfPanel);
            }
            mainPanel.add(topHorizSplit, BorderLayout.CENTER);

            if (i == numRows-1) { // if last row 
                curVertSplit.setBottomComponent(mainPanel);
                //curVertSplit.setDividerLocation(0.5);
            } else {
                curVertSplit.setTopComponent(mainPanel);
            }
            
            if (i < numRows-2) {
                JSplitPane newVertSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
                setupSplit(newVertSplit);
                curVertSplit.setBottomComponent(newVertSplit);
                //curVertSplit.setDividerLocation(1.0/(numRows-i));
                curVertSplit = newVertSplit;
                vertSplPanes[i+1] = curVertSplit;
            }
        }
    }    
    
    private void setup() {
        setupSplit(this);
        partitionTitles = new JLabel[numRows];
        cellLabels = new JLabel[numRows][2];
        cells = new CellEditorPane[numRows][2];
        cellPlainText = new StringBuffer[numRows][2];
        vertSplPanes = new JSplitPane[numRows];
        horizSplPanes = new JSplitPane[numRows];
        for (int i=0; i<numRows; i++) 
            for (int j=0; j<2; j++) {
                cellPlainText[i][j] = new StringBuffer();
                partitionTitles[i] = new JLabel("");
                partitionTitles[i].setHorizontalAlignment(SwingConstants.CENTER);
                partitionTitles[i].setMinimumSize(zeroDim);
                partitionTitles[i].setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
                cellLabels[i][j] = new JLabel("");
                cellLabels[i][j].setMinimumSize(zeroDim);
                cells[i][j] = new CellEditorPane(cellPlainText[i][j]);
                cells[i][j].setText("");
                cells[i][j].setEditable(false);
                cells[i][j].addHyperlinkListener(this);
                cells[i][j].setContentType("text/html");
                cells[i][j].setMargin(insets);
                cells[i][j].addMouseListener(mouseAdapter);
            }
    }
    
    private void setupSplit(JSplitPane split) {
        split.setMinimumSize(zeroDim);
        split.setOneTouchExpandable(true);
        split.setResizeWeight(0.5);   
        split.setBorder(border);
    }
    public StringBuffer getCellPlainTextBuffer(int row, int col) {
        if (col < 0 || col > 1 || row < 0 || row > numRows-1) return null;
        return cellPlainText[row][col];
    }
    public void setPartitionTitle(int row, String text) {
        if (row < 0 || row > numRows-1) return;
        partitionTitles[row].setText(text);
    }    
    public void setLabelText(int row, int col, String text) {
        if (col < 0 || col > 1 || row < 0 || row > numRows-1) return;
        cellLabels[row][col].setText(text);
    }
    public void setCellText(int row, int col, String text) {
        if (col < 0 || col > 1 || row < 0 || row > numRows-1) return;
        if (text.length() > 0) {
            cells[row][col].setText(text);
            cells[row][col].moveCaretPosition(0);
        }
    }
    
    public void updateLayout() {
        for (int i=numRows-1; i>=0; i--) {
            if (horizSplPanes[i] != null)
                horizSplPanes[i].resetToPreferredSizes();
        }
        for (int i=numRows-1; i>=0; i--) {
            if (vertSplPanes[i] != null) 
                vertSplPanes[i].resetToPreferredSizes();
        }
    }
    
    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED
                             && mouseAdapter.lastClick == MouseEvent.BUTTON1) {
            parent.highlight(Integer.parseInt(event.getDescription()));
        }
    }         

    private class CellEditorPane extends JEditorPane {
        private StringBuffer plainText;
        public CellEditorPane(StringBuffer textbuf) {plainText = textbuf;}
        public StringBuffer getPlainTextBuffer() { return plainText; }
    }
    
    private class CellMouseAdapter extends MouseAdapter {
        public int lastClick = -1;
        public void mousePressed(MouseEvent e) {
            lastClick = e.getButton();
            if (! (e.getButton() == MouseEvent.BUTTON2 
                || e.getButton() == MouseEvent.BUTTON3))
                return;
            CellEditorPane epane = (CellEditorPane)e.getSource();
            parent.showCellPopup(epane.getPlainTextBuffer().toString(),
                                 e.getComponent(), e.getX(), e.getY());
        }
    }
}

