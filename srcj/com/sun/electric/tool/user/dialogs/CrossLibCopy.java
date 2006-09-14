/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CrossLibCopy.java
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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.CellChangeJobs;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.User;

import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JScrollBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Class to handle the "Cross-Library Copy" dialog.
 */
public class CrossLibCopy extends EDialog
{
	private List<Library> libList;
	private List<Cell> cellListLeft, cellListRight;
	private JList listLeft, listRight, listCenter;
	private DefaultListModel modelLeft, modelRight, modelCenter;
	private static Library curLibLeft = null, curLibRight = null;
	private static boolean lastDeleteAfterCopy = false;
	private static boolean lastCopyRelated = false;
	private static boolean lastCopySubcells = false;
	private static boolean lastUseExisting = true;

	// Class to synchronize the 3 scrollbars in dialog
	private class CrossLibScrollBarListener implements ChangeListener
	{
		private JScrollBar[] scrollBarList;
        protected boolean blocked = false;

		public  CrossLibScrollBarListener(JScrollBar[] bars)
		{
			scrollBarList = new JScrollBar[2];
			System.arraycopy(bars, 0, scrollBarList, 0, bars.length);
		}
		public void stateChanged(ChangeEvent evt)
		{
            if (blocked) return;
            blocked = true;

			BoundedRangeModel sourceScroll = (BoundedRangeModel)evt.getSource();
			int iSMin   = sourceScroll.getMinimum();
			int iSMax   = sourceScroll.getMaximum();
			int iSDiff  = iSMax - iSMin;
			int iSVal   = sourceScroll.getValue();
			int iDMin   = cellsLeft.getVerticalScrollBar().getMinimum();
			int iDMax   = cellsLeft.getVerticalScrollBar().getMaximum();
			int iDDiff  = iDMax - iDMin;
			int iDVal = (iSDiff == iDDiff) ? iSVal : (iDDiff * iSVal) / iSDiff;

			for (int i = 0; i < scrollBarList.length; i++)
				scrollBarList[i].setValue(iDVal);

            blocked = false;
		}
	}

	/** Creates new form CrossLibCopy */
	public CrossLibCopy(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// determine the two libraries to show
		libList = Library.getVisibleLibraries();
		if (curLibLeft == null) curLibLeft = Library.getCurrent();
		if (curLibRight == null) curLibRight = Library.getCurrent();
		if (curLibLeft == curLibRight)
		{
			for(Library lib : libList)
			{
				if (lib != curLibLeft)
				{
					curLibRight = lib;
					break;
				}
			}
		}

		// setup the library popups
		Library saveLeft = curLibLeft, saveRight = curLibRight;
		for(Library lib : libList)
		{
			librariesLeft.addItem(lib.getName());
			librariesRight.addItem(lib.getName());
		}
		int curIndex = libList.indexOf(saveLeft);
		if (curIndex >= 0) librariesLeft.setSelectedIndex(curIndex);
		curIndex = libList.indexOf(saveRight);
		if (curIndex >= 0) librariesRight.setSelectedIndex(curIndex);

		// make the left list
		modelLeft = new DefaultListModel();
		listLeft = new JList(modelLeft);
		listLeft.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); // SINGLE_SELECTION);
		cellsLeft.setViewportView(listLeft);
		listLeft.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { leftListClick(evt); }
		});

		// make the right list
		modelRight = new DefaultListModel();
		listRight = new JList(modelRight);
		listRight.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		cellsRight.setViewportView(listRight);
		listRight.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { rightListClick(evt); }
		});

		// make the center list
		modelCenter = new DefaultListModel();
		listCenter = new JList(modelCenter);
		listCenter.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		center.setViewportView(listCenter);
		listCenter.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { centerListClick(evt); }
		});
		showCells(false);

		// show the check boxes
		if (lastCopySubcells)
		{
			lastUseExisting = true;
			useExistingSubcells.setEnabled(false);
		}
		deleteAfterCopy.setSelected(lastDeleteAfterCopy);
		copyRelatedViews.setSelected(lastCopyRelated);
		copySubcells.setSelected(lastCopySubcells);
		useExistingSubcells.setSelected(lastUseExisting);

		// TO syncronize scroll bars
		JScrollBar[] scrollArray1 = {cellsRight.getVerticalScrollBar(), center.getVerticalScrollBar()};
		cellsLeft.getVerticalScrollBar().getModel().addChangeListener( new CrossLibScrollBarListener(scrollArray1));
		JScrollBar[] scrollArray2 = {cellsLeft.getVerticalScrollBar(), center.getVerticalScrollBar()};
		cellsRight.getVerticalScrollBar().getModel().addChangeListener( new CrossLibScrollBarListener(scrollArray2));
		JScrollBar[] scrollArray3 = {cellsLeft.getVerticalScrollBar(), cellsRight.getVerticalScrollBar()};
		center.getVerticalScrollBar().getModel().addChangeListener( new CrossLibScrollBarListener(scrollArray3));

		finishInitialization();
	}

	private void leftListClick(MouseEvent evt)
	{
        int[] indices = listLeft.getSelectedIndices();
        listCenter.setSelectedIndices(indices);
        listRight.setSelectedIndices(indices);
	}

	private void rightListClick(MouseEvent evt)
	{
        int[] indices = listRight.getSelectedIndices();
        listLeft.setSelectedIndices(indices);
        listCenter.setSelectedIndices(indices);
	}

	private void centerListClick(MouseEvent evt)
	{
        int[] indices = listCenter.getSelectedIndices();
        listLeft.setSelectedIndices(indices);
        listRight.setSelectedIndices(indices);
	}

	private void showCells(boolean report)
	{
		if (modelLeft == null || modelRight == null || modelCenter == null) return;
		cellListLeft = new ArrayList<Cell>();
		for (Iterator<Cell> it = curLibLeft.getCells(); it.hasNext(); ) cellListLeft.add(it.next());
		cellListRight = new ArrayList<Cell>();
		for (Iterator<Cell> it = curLibRight.getCells(); it.hasNext(); ) cellListRight.add(it.next());
		modelLeft.clear();
		modelRight.clear();
		modelCenter.clear();
        boolean examineContents = compareContent.isSelected();

		// put out the parallel list of cells in the two libraries
		int leftPos = 0, rightPos = 0;
		int leftCount = cellListLeft.size();
		int rightCount = cellListRight.size();
		for(;;)
		{
			int op;
			if (leftPos >= leftCount && rightPos >= rightCount) break;
			if (leftPos >= leftCount) op = 2; else
				if (rightPos >= rightCount) op = 1; else
			{
				Cell leftCell = cellListLeft.get(leftPos);
				Cell rightCell = cellListRight.get(rightPos);
                int j = leftCell.getCellName().compareTo(rightCell.getCellName());
				if (j < 0) op = 1; else
					if (j > 0) op = 2; else
						op = 3;
			}

			String leftName = " ";
			Cell leftCell = null;
			if (op == 1 || op == 3)
			{
				leftCell = cellListLeft.get(leftPos++);
				leftName = leftCell.noLibDescribe();
			}
			modelLeft.addElement(leftName);

			String rightName = " ";
			Cell rightCell = null;
			if (op == 2 || op == 3)
			{
				rightCell = cellListRight.get(rightPos++);
				rightName = rightCell.noLibDescribe();
			}
			modelRight.addElement(rightName);

			String pt = " ";
			if (op == 3)
			{
				int compare = leftCell.getRevisionDate().compareTo(rightCell.getRevisionDate());
				StringBuffer buffer = null;
				boolean result = true; String message = "";

				if (examineContents)
				{
					if (report) buffer = new StringBuffer("\n");
					result = leftCell.compare(rightCell, buffer);
                    // This message is only valid if the content is compared.
                    message = (result) ? "(but contents are the same)" : "(and contents are different)";
				}

				if (compare > 0)
				{
					pt = (result) ? "<-Old" : "<-Old/Diff";
					if (report) System.out.println(curLibLeft.getName() + ":" + leftName + " OLDER THAN " +
						curLibRight.getName() + ":" + rightName + message + ":" + ((buffer != null) ? buffer.toString() : "\n"));
				} else if (compare < 0)
				{
					pt = (result) ? "  Old->" : " Diff/Old->";
					if (report) System.out.println(curLibRight.getName() + ":" + rightName + " OLDER THAN " +
						curLibLeft.getName() + ":" + leftName + message + ":" + ((buffer != null) ? buffer.toString() : "\n"));
				} else
				{
					pt = (result) ? "-Same-" : "-Diff -";
					if (!result && report) System.out.println(curLibLeft.getName() + ":" + leftName + " DIFFERS FROM " +
						curLibRight.getName() + ":" + rightName + ":" + ((buffer != null) ? buffer.toString() : "\n"));
				}
			}
			modelCenter.addElement(pt);
		}
	}

//	// Class to compare two cells
//	private static class CrossLibraryExamineJob extends Job
//	{
//		private Cell leftC;
//		private Cell rightC;
//		private boolean reportResults; // to report to std output the comparison
//		private boolean result;
//		private StringBuffer buffer;
//
//        private CrossLibraryExamineJob(Cell left, Cell right, boolean report)
//		{
//			super("Cross-Library examine", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
//			this.leftC = left;
//			this.rightC = right;
//			this.reportResults = report;
//			startJob();
//		}
//
//		public boolean doIt() throws JobException
//		{
//			if (reportResults)
//				buffer = new StringBuffer("Cells '" + leftC.libDescribe() + "' and '" + rightC.libDescribe() + ":");
//			result = (leftC != null && leftC.compare(rightC, buffer));
//
//			if (reportResults)
//			{
//				if (result)
//					buffer.append("Do not differ");
//				System.out.println(buffer);
//			}
//			return (true);
//		}
//		public boolean getResult() { return (result); }
//		public StringBuffer getDifference() { return (buffer); }
//	}

	private static class CrossLibraryCopyJob extends Job
	{
		private List<Cell> fromCells;
		private Library toLibrary;
		private transient CrossLibCopy dialog;
		private boolean deleteAfter, copyRelated, copySubs, useExisting;
		private int index;

		protected CrossLibraryCopyJob(List<Cell> fromCells, Library toLibrary, CrossLibCopy dialog, boolean deleteAfter,
			boolean copyRelated, boolean copySubs, boolean useExisting)
		{
			super("Cross-Library copy", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.fromCells = fromCells;
			this.toLibrary = toLibrary;
			this.dialog = dialog;
			this.deleteAfter = deleteAfter;
			this.copyRelated = copyRelated;
			this.copySubs = copySubs;
			this.useExisting = useExisting;

			// remember the selection line
			index = dialog.listLeft.getSelectedIndex();
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// do the copy
			CellChangeJobs.copyRecursively(fromCells, toLibrary, true, deleteAfter, copyRelated, copySubs, useExisting);
			return true;
		}

        public void terminateOK()
        {
			// reload the dialog
			dialog.showCells(false);

			// reselect the last selected line
			dialog.listLeft.setSelectedIndex(index);
			dialog.listRight.setSelectedIndex(index);
			dialog.listCenter.setSelectedIndex(index);
        }
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        Top = new javax.swing.JPanel();
        librariesLeft = new javax.swing.JComboBox();
        librariesRight = new javax.swing.JComboBox();
        copyLeft = new javax.swing.JButton();
        copyRight = new javax.swing.JButton();
        done = new javax.swing.JButton();
        cellsLeft = new javax.swing.JScrollPane();
        center = new javax.swing.JScrollPane();
        cellsRight = new javax.swing.JScrollPane();
        centerLabel = new javax.swing.JLabel();
        Bottom = new javax.swing.JPanel();
        BottomLeft = new javax.swing.JPanel();
        compareContent = new javax.swing.JCheckBox();
        compareQuite = new javax.swing.JCheckBox();
        BottomCenter = new javax.swing.JPanel();
        examineContents = new javax.swing.JButton();
        BottomRight = new javax.swing.JPanel();
        deleteAfterCopy = new javax.swing.JCheckBox();
        copySubcells = new javax.swing.JCheckBox();
        useExistingSubcells = new javax.swing.JCheckBox();
        copyRelatedViews = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.BorderLayout(0, 10));

        setTitle("Cross Library Copy");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        Top.setLayout(new java.awt.GridBagLayout());

        librariesLeft.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                librariesLeftActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        Top.add(librariesLeft, gridBagConstraints);

        librariesRight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                librariesRightActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        Top.add(librariesRight, gridBagConstraints);

        copyLeft.setText("<< Copy");
        copyLeft.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyLeftActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        Top.add(copyLeft, gridBagConstraints);

        copyRight.setText("Copy >>");
        copyRight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyRightActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        Top.add(copyRight, gridBagConstraints);

        done.setText("Done");
        done.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doneActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        Top.add(done, gridBagConstraints);

        cellsLeft.setPreferredSize(new java.awt.Dimension(200, 350));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        Top.add(cellsLeft, gridBagConstraints);

        center.setMinimumSize(new java.awt.Dimension(50, 200));
        center.setOpaque(false);
        center.setPreferredSize(new java.awt.Dimension(50, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        Top.add(center, gridBagConstraints);

        cellsRight.setPreferredSize(new java.awt.Dimension(200, 350));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        Top.add(cellsRight, gridBagConstraints);

        centerLabel.setText("Date");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        Top.add(centerLabel, gridBagConstraints);

        getContentPane().add(Top, java.awt.BorderLayout.CENTER);

        BottomLeft.setLayout(new java.awt.GridBagLayout());

        compareContent.setText("Date and content");
        compareContent.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                compareContentItemStateChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        BottomLeft.add(compareContent, gridBagConstraints);

        compareQuite.setSelected(true);
        compareQuite.setText("Examine quietly");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        BottomLeft.add(compareQuite, gridBagConstraints);

        Bottom.add(BottomLeft);

        BottomCenter.setLayout(new java.awt.GridBagLayout());

        examineContents.setText("Compare");
        examineContents.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                examineContentsActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        BottomCenter.add(examineContents, gridBagConstraints);

        Bottom.add(BottomCenter);

        BottomRight.setLayout(new java.awt.GridBagLayout());

        deleteAfterCopy.setText("Delete after copy");
        deleteAfterCopy.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                deleteAfterCopyItemStateChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        BottomRight.add(deleteAfterCopy, gridBagConstraints);

        copySubcells.setText("Copy subcells");
        copySubcells.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copySubcellsActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        BottomRight.add(copySubcells, gridBagConstraints);

        useExistingSubcells.setText("Use existing subcells");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        BottomRight.add(useExistingSubcells, gridBagConstraints);

        copyRelatedViews.setText("Copy all related views");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        BottomRight.add(copyRelatedViews, gridBagConstraints);

        Bottom.add(BottomRight);

        getContentPane().add(Bottom, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void copySubcellsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_copySubcellsActionPerformed
	{//GEN-HEADEREND:event_copySubcellsActionPerformed
		if (copySubcells.isSelected())
		{
			useExistingSubcells.setSelected(true);
			useExistingSubcells.setEnabled(false);
		} else
		{
			useExistingSubcells.setEnabled(true);
		}
	}//GEN-LAST:event_copySubcellsActionPerformed

    private void compareContentItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_compareContentItemStateChanged
        
        if (compareContent.isSelected())
		{
			centerLabel.setText("Date/Content");
		} else
		{
	        centerLabel.setText("Date");
		}
        // Clean any information
        modelCenter.clear();
        // re-create data column again
        showCells(false);
    }//GEN-LAST:event_compareContentItemStateChanged

	private void deleteAfterCopyItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_deleteAfterCopyItemStateChanged
	{//GEN-HEADEREND:event_deleteAfterCopyItemStateChanged
		if (deleteAfterCopy.isSelected())
		{
			copyLeft.setText("<< Move");
			copyRight.setText("Move >>");
		} else
		{
			copyLeft.setText("<< Copy");
			copyRight.setText("Copy >>");
		}
	}//GEN-LAST:event_deleteAfterCopyItemStateChanged

	private void librariesRightActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_librariesRightActionPerformed
	{//GEN-HEADEREND:event_librariesRightActionPerformed
		// the right popup of libraies changed
		JComboBox cb = (JComboBox)evt.getSource();
		int index = cb.getSelectedIndex();
		curLibRight = libList.get(index);
		showCells(false);
	}//GEN-LAST:event_librariesRightActionPerformed

	private void librariesLeftActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_librariesLeftActionPerformed
	{//GEN-HEADEREND:event_librariesLeftActionPerformed
		// the left popup of libraies changed
		JComboBox cb = (JComboBox)evt.getSource();
		int index = cb.getSelectedIndex();
		curLibLeft = libList.get(index);
		showCells(false);
	}//GEN-LAST:event_librariesLeftActionPerformed

	private void examineContentsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_examineContentsActionPerformed
	{//GEN-HEADEREND:event_examineContentsActionPerformed
		showCells(!compareQuite.isSelected());
	}//GEN-LAST:event_examineContentsActionPerformed

	private void copyRightActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_copyRightActionPerformed
	{//GEN-HEADEREND:event_copyRightActionPerformed
		List<Cell> fromCells = new ArrayList<Cell>();
        for (Object cellObj : listLeft.getSelectedValues())
        {
            String cellName = (String)cellObj;
            Cell fromCell = curLibLeft.findNodeProto(cellName);
            if (fromCell == null) return;
            fromCells.add(fromCell);
        }
        new CrossLibraryCopyJob(fromCells, curLibRight, this, deleteAfterCopy.isSelected(),
            copyRelatedViews.isSelected(), copySubcells.isSelected(), useExistingSubcells.isSelected());
	}//GEN-LAST:event_copyRightActionPerformed

	private void doneActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_doneActionPerformed
	{//GEN-HEADEREND:event_doneActionPerformed
		closeDialog(null);
	}//GEN-LAST:event_doneActionPerformed

	private void copyLeftActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_copyLeftActionPerformed
	{//GEN-HEADEREND:event_copyLeftActionPerformed
		List<Cell> fromCells = new ArrayList<Cell>();
        for (Object cellObj : listRight.getSelectedValues())
        {
            String cellName = (String)cellObj;
            Cell fromCell = curLibRight.findNodeProto(cellName);
            if (fromCell == null) return;
            fromCells.add(fromCell);
        }
        new CrossLibraryCopyJob(fromCells, curLibLeft, this, deleteAfterCopy.isSelected(),
            copyRelatedViews.isSelected(), copySubcells.isSelected(), useExistingSubcells.isSelected());
	}//GEN-LAST:event_copyLeftActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		lastDeleteAfterCopy = deleteAfterCopy.isSelected();
		lastCopyRelated = copyRelatedViews.isSelected();
		lastCopySubcells = copySubcells.isSelected();
		lastUseExisting = useExistingSubcells.isSelected();
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Bottom;
    private javax.swing.JPanel BottomCenter;
    private javax.swing.JPanel BottomLeft;
    private javax.swing.JPanel BottomRight;
    private javax.swing.JPanel Top;
    private javax.swing.JScrollPane cellsLeft;
    private javax.swing.JScrollPane cellsRight;
    private javax.swing.JScrollPane center;
    private javax.swing.JLabel centerLabel;
    private javax.swing.JCheckBox compareContent;
    private javax.swing.JCheckBox compareQuite;
    private javax.swing.JButton copyLeft;
    private javax.swing.JCheckBox copyRelatedViews;
    private javax.swing.JButton copyRight;
    private javax.swing.JCheckBox copySubcells;
    private javax.swing.JCheckBox deleteAfterCopy;
    private javax.swing.JButton done;
    private javax.swing.JButton examineContents;
    private javax.swing.JComboBox librariesLeft;
    private javax.swing.JComboBox librariesRight;
    private javax.swing.JCheckBox useExistingSubcells;
    // End of variables declaration//GEN-END:variables

}
