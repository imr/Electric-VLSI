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

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.lang.Runnable;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.event.ItemEvent;


/**
 * Class to handle the "Cross-Library Copy" dialog.
 */
public class CrossLibCopy extends javax.swing.JDialog
{
	private List libList;
	private Library curLibLeft, curLibRight;
	private List cellListLeft, cellListRight;
	private JList listLeft, listRight, listCenter;
	private DefaultListModel modelLeft, modelRight, modelCenter;
	private static boolean lastDeleteAfterCopy = false;
	private static boolean lastCopyRelated = true;
	private static boolean lastCopySubcells = true;
	private static boolean lastUseExisting = false;

	/** Creates new form CrossLibCopy */
	public CrossLibCopy(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();

		// setup the library popups
		libList = Library.getVisibleLibrariesSortedByName();
		for(Iterator it = libList.iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			librariesLeft.addItem(lib.getLibName());
			librariesRight.addItem(lib.getLibName());
		}
		curLibLeft = curLibRight = Library.getCurrent();
		int curIndex = libList.indexOf(curLibLeft);
		if (curIndex >= 0) librariesLeft.setSelectedIndex(curIndex);

		// make the left list
		modelLeft = new DefaultListModel();
		listLeft = new JList(modelLeft);
		listLeft.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		cellsLeft.setViewportView(listLeft);
		listLeft.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt) { leftListClick(evt); }
		});

		// make the right list
		modelRight = new DefaultListModel();
		listRight = new JList(modelRight);
		listRight.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		cellsRight.setViewportView(listRight);
		listRight.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt) { rightListClick(evt); }
		});

		// make the center list
		modelCenter = new DefaultListModel();
		listCenter = new JList(modelCenter);
		listCenter.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		center.setViewportView(listCenter);
		listCenter.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt) { centerListClick(evt); }
		});
		showCells(false, false);

		// show the check boxes
		deleteAfterCopy.setSelected(lastDeleteAfterCopy);
		copyRelatedViews.setSelected(lastCopyRelated);
		copySubcells.setSelected(lastCopySubcells);
		useExistingSubcells.setSelected(lastUseExisting);
	}

	private void leftListClick(java.awt.event.MouseEvent evt)
	{
		int index = listLeft.getSelectedIndex();
		listRight.setSelectedIndex(index);
		listCenter.setSelectedIndex(index);
	}

	private void rightListClick(java.awt.event.MouseEvent evt)
	{
		int index = listRight.getSelectedIndex();
		listLeft.setSelectedIndex(index);
		listCenter.setSelectedIndex(index);
	}

	private void centerListClick(java.awt.event.MouseEvent evt)
	{
		int index = listCenter.getSelectedIndex();
		listLeft.setSelectedIndex(index);
		listRight.setSelectedIndex(index);
	}

	private void showCells(boolean report, boolean examineContents)
	{
		if (modelLeft == null || modelRight == null || modelCenter == null) return;
		cellListLeft = curLibLeft.getCellsSortedByName();
		cellListRight = curLibRight.getCellsSortedByName();
		modelLeft.clear();
		modelRight.clear();
		modelCenter.clear();

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
				Cell leftCell = (Cell)cellListLeft.get(leftPos);
				Cell rightCell = (Cell)cellListRight.get(rightPos);
				int j = leftCell.getProtoName().compareToIgnoreCase(rightCell.getProtoName());
				if (j < 0) op = 1; else
					if (j > 0) op = 2; else
						op = 3;
			}

			String leftName = " ";
			Cell leftCell = null;
			if (op == 1 || op == 3)
			{
				leftCell = (Cell)cellListLeft.get(leftPos++);
				leftName = leftCell.noLibDescribe();
			}
			if (!report) modelLeft.addElement(leftName);

			String rightName = " ";
			Cell rightCell = null;
			if (op == 2 || op == 3)
			{
				rightCell = (Cell)cellListRight.get(rightPos++);
				rightName = rightCell.noLibDescribe();
			}
			if (!report) modelRight.addElement(rightName);

			String pt = " ";
			if (op == 3)
			{
				if (leftCell.getRevisionDate().before(rightCell.getRevisionDate()))
				{
//					if (examineContents)
//					{
//						if (us_samecontents(leftCell, rightCell, examineContents))
//						{
//							pt = "<-OLD";
//							if (report) System.out.println("%s:%s OLDER THAN %s:%s (but contents are the same)"),
//								lib->libname, leftName, otherlib->libname, rightName);
//						} else
//						{
//							pt = "<-OLD*";
//							if (report) System.out.println("%s:%s OLDER THAN %s:%s (and contents are different)"),
//								lib->libname, leftName, otherlib->libname, rightName);
//						}
//					} else
					{
						pt = "<-OLD";
						if (report) System.out.println(curLibLeft.getLibName() + ":" + leftName + " OLDER THAN " +
							curLibRight.getLibName() + ":" + rightName);
					}
				} else if (leftCell.getRevisionDate().after(rightCell.getRevisionDate()))
				{
//					if (examineContents)
//					{
//						if (us_samecontents(leftCell, otherf, examineContents))
//						{
//							pt = "  OLD->";
//							if (report) System.out.println("%s:%s OLDER THAN %s:%s (but contents are the same)"),
//								otherlib->libname, rightName, lib->libname, leftName);
//						} else
//						{
//							pt = " *OLD->";
//							if (report) System.out.println("%s:%s OLDER THAN %s:%s (and contents are different)"),
//								otherlib->libname, rightName, lib->libname, leftName);
//						}
//					} else
					{
						pt = "  OLD->";
						if (report) System.out.println(curLibRight.getLibName() + ":" + rightName + " OLDER THAN " +
							curLibLeft.getLibName() + ":" + leftName);
					}
				} else
				{
					pt = "-EQUAL-";
				}
			}
			if (!report) modelCenter.addElement(pt);
		}
	}

	/**
	 * This class gets run when a cross-library copy finishes.
	 * It updates the dialog display.
	 */
	static class DoneCopying implements Runnable
	{
		CrossLibCopy dialog;
		int index;

		DoneCopying(CrossLibCopy dialog, int index)
		{
			this.dialog = dialog;
			this.index = index;
		}

		public void run()
		{
			// reload the dialog
			dialog.showCells(false, false);

			// reselect the last selected line
			dialog.listLeft.setSelectedIndex(index);
			dialog.listRight.setSelectedIndex(index);
			dialog.listCenter.setSelectedIndex(index);
		}
	}

	private static class CrossLibraryCopy extends Job
	{
		Cell fromCell;
		Library toLibrary;
		CrossLibCopy dialog;

		protected CrossLibraryCopy(Cell fromCell, Library toLibrary, CrossLibCopy dialog)
		{
			super("Cross-Library copy", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.fromCell = fromCell;
			this.toLibrary = toLibrary;
			this.dialog = dialog;
		}

		public void doIt()
		{
			// remember the selection line
			int index = dialog.listLeft.getSelectedIndex();

			// do the copy
			boolean deleteAfter = dialog.deleteAfterCopy.isSelected();
			boolean copyRelated = dialog.copyRelatedViews.isSelected();
			boolean copySubs = dialog.copySubcells.isSelected();
			boolean useExisting = dialog.useExistingSubcells.isSelected();
			CircuitChanges.copyRecursively(fromCell, fromCell.getProtoName(), toLibrary,
				fromCell.getView(), true, deleteAfter, "", !copyRelated, !copySubs, useExisting);

			// schedule the dialog to refresh
			SwingUtilities.invokeLater(new DoneCopying(dialog, index));
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
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
        Bottom = new javax.swing.JPanel();
        BottomLeft = new javax.swing.JPanel();
        examineContents = new javax.swing.JButton();
        examineContentsQuietly = new javax.swing.JButton();
        listDifferences = new javax.swing.JButton();
        BottomRight = new javax.swing.JPanel();
        deleteAfterCopy = new javax.swing.JCheckBox();
        copySubcells = new javax.swing.JCheckBox();
        useExistingSubcells = new javax.swing.JCheckBox();
        copyRelatedViews = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.BorderLayout(0, 10));

        setTitle("Cross Library Copy");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        Top.setLayout(new java.awt.GridBagLayout());

        librariesLeft.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                librariesLeftActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        Top.add(librariesLeft, gridBagConstraints);

        librariesRight.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
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
        copyLeft.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                copyLeftActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        Top.add(copyLeft, gridBagConstraints);

        copyRight.setText("Copy >>");
        copyRight.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                copyRightActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        Top.add(copyRight, gridBagConstraints);

        done.setText("Done");
        done.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
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
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        Top.add(cellsLeft, gridBagConstraints);

        center.setMinimumSize(new java.awt.Dimension(22, 200));
        center.setPreferredSize(new java.awt.Dimension(22, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        gridBagConstraints.weighty = 1.0;
        Top.add(center, gridBagConstraints);

        cellsRight.setPreferredSize(new java.awt.Dimension(200, 350));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        Top.add(cellsRight, gridBagConstraints);

        getContentPane().add(Top, java.awt.BorderLayout.CENTER);

        BottomLeft.setLayout(new java.awt.GridBagLayout());

        examineContents.setText("Examine contents");
        examineContents.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                examineContentsActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        BottomLeft.add(examineContents, gridBagConstraints);

        examineContentsQuietly.setText("Examine contents quietly");
        examineContentsQuietly.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                examineContentsQuietlyActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        BottomLeft.add(examineContentsQuietly, gridBagConstraints);

        listDifferences.setText("List differences");
        listDifferences.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                listDifferencesActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        BottomLeft.add(listDifferences, gridBagConstraints);

        Bottom.add(BottomLeft);

        BottomRight.setLayout(new java.awt.GridBagLayout());

        deleteAfterCopy.setText("Delete after copy");
        deleteAfterCopy.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                deleteAfterCopyItemStateChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        BottomRight.add(deleteAfterCopy, gridBagConstraints);

        copySubcells.setText("Copy subcells");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        BottomRight.add(copySubcells, gridBagConstraints);

        useExistingSubcells.setText("Use existing subcells");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        BottomRight.add(useExistingSubcells, gridBagConstraints);

        copyRelatedViews.setText("Copy related views");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        BottomRight.add(copyRelatedViews, gridBagConstraints);

        Bottom.add(BottomRight);

        getContentPane().add(Bottom, java.awt.BorderLayout.SOUTH);

        pack();
    }//GEN-END:initComponents

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
		curLibRight = (Library)libList.get(index);
		showCells(false, false);
	}//GEN-LAST:event_librariesRightActionPerformed

	private void librariesLeftActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_librariesLeftActionPerformed
	{//GEN-HEADEREND:event_librariesLeftActionPerformed
		// the left popup of libraies changed
		JComboBox cb = (JComboBox)evt.getSource();
		int index = cb.getSelectedIndex();
		curLibLeft = (Library)libList.get(index);
		showCells(false, false);
	}//GEN-LAST:event_librariesLeftActionPerformed

	private void listDifferencesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_listDifferencesActionPerformed
	{//GEN-HEADEREND:event_listDifferencesActionPerformed
		System.out.println("This button doesn't work yet");
	}//GEN-LAST:event_listDifferencesActionPerformed

	private void examineContentsQuietlyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_examineContentsQuietlyActionPerformed
	{//GEN-HEADEREND:event_examineContentsQuietlyActionPerformed
		System.out.println("This button doesn't work yet");
	}//GEN-LAST:event_examineContentsQuietlyActionPerformed

	private void examineContentsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_examineContentsActionPerformed
	{//GEN-HEADEREND:event_examineContentsActionPerformed
		System.out.println("This button doesn't work yet");
	}//GEN-LAST:event_examineContentsActionPerformed

	private void copyRightActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_copyRightActionPerformed
	{//GEN-HEADEREND:event_copyRightActionPerformed
		String cellName = (String)listLeft.getSelectedValue();
		Cell fromCell = curLibLeft.findNodeProto(cellName);
		if (fromCell == null) return;
		CrossLibraryCopy job = new CrossLibraryCopy(fromCell, curLibRight, this);
	}//GEN-LAST:event_copyRightActionPerformed

	private void doneActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_doneActionPerformed
	{//GEN-HEADEREND:event_doneActionPerformed
		closeDialog(null);
	}//GEN-LAST:event_doneActionPerformed

	private void copyLeftActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_copyLeftActionPerformed
	{//GEN-HEADEREND:event_copyLeftActionPerformed
		String cellName = (String)listRight.getSelectedValue();
		Cell fromCell = curLibRight.findNodeProto(cellName);
		if (fromCell == null) return;
		CrossLibraryCopy job = new CrossLibraryCopy(fromCell, curLibLeft, this);
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
    private javax.swing.JPanel BottomLeft;
    private javax.swing.JPanel BottomRight;
    private javax.swing.JPanel Top;
    private javax.swing.JScrollPane cellsLeft;
    private javax.swing.JScrollPane cellsRight;
    private javax.swing.JScrollPane center;
    private javax.swing.JButton copyLeft;
    private javax.swing.JCheckBox copyRelatedViews;
    private javax.swing.JButton copyRight;
    private javax.swing.JCheckBox copySubcells;
    private javax.swing.JCheckBox deleteAfterCopy;
    private javax.swing.JButton done;
    private javax.swing.JButton examineContents;
    private javax.swing.JButton examineContentsQuietly;
    private javax.swing.JComboBox librariesLeft;
    private javax.swing.JComboBox librariesRight;
    private javax.swing.JButton listDifferences;
    private javax.swing.JCheckBox useExistingSubcells;
    // End of variables declaration//GEN-END:variables
	
}
