/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: EquivClassSplitPane.java
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
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * This class implements patrition table for Wire and Part classes.
 * The table is realized as a hierarchy of vertical split panes with root 
 * split pane on the top of the table. Each row has a horizontal split pane.  
 * The table can have any number of rows, but the number of rows cannot 
 * be changed after instantiation. 
 * Each row has a title (partition title), but in the case of a single 
 * row this title is not displayed.
 * Each cell has a plain text buffer associated with it where a plain 
 * text copy of its content is stored. This facilites text extraction to
 * clipboard.
 */
class EquivClassSplitPane extends JSplitPane implements HyperlinkListener {

    /** Parent Comparison Pane    */  protected final ComparisonsPane parent; 
    /** Number od rows            */  private int numRows;
    /** Paritition Title Labels   */  private JLabel partitionTitles[];
    /** Cell Labels               */  private JLabel cellLabels[][];  
    /** Table cells               */  private CellEditorPane cells[][];
    /** Plain text cell content   */  private StringBuffer cellPlainText[][];
    /** Split panes               */  private JSplitPane vertSplPanes[], horizSplPanes[];
    /** Right-click mouse adapter */  private CellMouseAdapter mouseAdapter 
                                              = new CellMouseAdapter();    
    /** Cell minimum size         */  private static final Dimension zeroDim 
                                              = new Dimension(0,10);
    
    /** Common border             */  private static Border border 
                                              = BorderFactory.createEmptyBorder();
    /** Common insets             */  private static Insets insets 
                                              = new Insets(0,0,0,0);
    
    protected EquivClassSplitPane(ComparisonsPane parent, int numRows) { 
        this.parent = parent;
        this.numRows = numRows;
        if (numRows == 1)
            createSingleRowSplitPane();
        else
            createMultiRowSplitPane();
    }
    
    /**
     * Create a table with one row. Realized a single horizontal split pane. 
     */
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

    /**
     * Create a table with multiple rows. Realized as a hierarchy of 
     * vertical split panes with internal horizontal split panes.
     */
    private void createMultiRowSplitPane() {
        setOrientation(JSplitPane.VERTICAL_SPLIT);
        setup();
        JSplitPane curVertSplit = this;
        vertSplPanes[0] = curVertSplit;
        // for each row
        for (int i=0; i<numRows; i++) {
            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.add(partitionTitles[i], BorderLayout.NORTH);
            JSplitPane topHorizSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            horizSplPanes[i] = topHorizSplit;            
            setupSplit(topHorizSplit);
            // for each column
            for (int j=0; j<2; j++) {
                JPanel halfPanel = new JPanel(new BorderLayout());
                halfPanel.add(cellLabels[i][j], BorderLayout.NORTH);
                JPanel textPanel = new JPanel();
                textPanel.setBorder(border);
                textPanel.setBackground(Color.WHITE);       
                textPanel.add(cells[i][j]);
                JScrollPane scrPane = new JScrollPane(textPanel);
                halfPanel.add(scrPane, BorderLayout.CENTER);
                if (j == 0) // if left column
                    topHorizSplit.setLeftComponent(halfPanel);
                else
                    topHorizSplit.setRightComponent(halfPanel);
            }
            mainPanel.add(topHorizSplit, BorderLayout.CENTER);

            if (i == numRows-1) // if last row 
                curVertSplit.setBottomComponent(mainPanel);
            else
                curVertSplit.setTopComponent(mainPanel);
            
            if (i < numRows-2) { // if second to last row 
                JSplitPane newVertSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
                setupSplit(newVertSplit);
                curVertSplit.setBottomComponent(newVertSplit);
                curVertSplit = newVertSplit;
                vertSplPanes[i+1] = curVertSplit;
            }
        }
    }    
    
    /**
     * Common init method for equiv. class tables
     */
    private void setup() {
        setupSplit(this);
        // allocate arrays
        partitionTitles = new JLabel[numRows];
        cellLabels = new JLabel[numRows][2];
        cells = new CellEditorPane[numRows][2];
        cellPlainText = new StringBuffer[numRows][2];
        vertSplPanes = new JSplitPane[numRows];
        horizSplPanes = new JSplitPane[numRows];
        
        // init cells
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
    
    /**
     * Common setup for a split pane
     * @param split  split pane to setup
     */
    private void setupSplit(JSplitPane split) {
        split.setMinimumSize(zeroDim);
        split.setOneTouchExpandable(true);
        split.setResizeWeight(0.5);   
        split.setBorder(border);
    }
    
    /**
     * Get plain text buffer object of a table cell. To change cell's 
     * plain text content, change the content of the returned buffer. 
     * @param row  cell row
     * @param col  cell column
     * @return plain text content of a table cell
     */
    protected StringBuffer getCellPlainTextBuffer(int row, int col) {
        if (col < 0 || col > 1 || row < 0 || row > numRows-1) return null;
        return cellPlainText[row][col];
    }
    
    /**
     * Set row title (partition title)
     * @param row  partition row
     * @param text  title text
     */
    protected void setPartitionTitle(int row, String text) {
        if (row < 0 || row > numRows-1) return;
        partitionTitles[row].setText(text);
    }
    
    /**
     * Set cell label
     * @param row  cell row
     * @param col  cell column
     * @param text  label text
     */
    protected void setLabelText(int row, int col, String text) {
        if (col < 0 || col > 1 || row < 0 || row > numRows-1) return;
        cellLabels[row][col].setText(text);
    }
    
    /**
     * Set cell HTML content
     * @param row  cell row
     * @param col  cell column
     * @param text  HTML content
     */
    protected void setCellText(int row, int col, String text) {
        if (col < 0 || col > 1 || row < 0 || row > numRows-1) return;
        cells[row][col].setText(text);
        SwingUtilities.invokeLater(new CaretUpdate(cells[row][col]));        
    }
    
    /**
     * Reset all split panes to their preferred sizes
     */
    protected void updateLayout() {
        for (int i=numRows-1; i>=0; i--) {
            if (horizSplPanes[i] != null)
                horizSplPanes[i].resetToPreferredSizes();
        }
        for (int i=numRows-1; i>=0; i--) {
            if (vertSplPanes[i] != null) 
                vertSplPanes[i].resetToPreferredSizes();
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.swing.event.HyperlinkListener#hyperlinkUpdate(javax.swing.event.HyperlinkEvent)
     */
    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED
                             && mouseAdapter.lastClick == MouseEvent.BUTTON1) {
            parent.highlight(Integer.parseInt(event.getDescription()));
        }
    }         
    
    /**
     * This class extends JEditorPane to add the plain text container to it.
     * The default text remains HTML.   
     */
    private class CellEditorPane extends JEditorPane {
        private StringBuffer plainText;
        public CellEditorPane(StringBuffer textbuf) {plainText = textbuf;}
        public StringBuffer getPlainTextBuffer() { return plainText; }
    }
    
    /**
     * This mouse adapter is used to detect clicks of a right mouse button
     * and to invoke the popup menu. 
     */
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
    
    /**
     * This class is a workaround for a JDK1.4 bug. It implements 
     * a Runnable which moves the caret position of the given 
     * CellEditorPane. 
     * The Runnable object of this class should be then 
     * passed to SwingUtilities.invokeLater 
     */
    private static class CaretUpdate implements Runnable {
        private CellEditorPane cell;
        public CaretUpdate(CellEditorPane c) { cell = c; }
        public void run() { 
            cell.moveCaretPosition(0); 
            cell.setSelectionStart(0);
            cell.setSelectionEnd(0);
        }
    }
}

