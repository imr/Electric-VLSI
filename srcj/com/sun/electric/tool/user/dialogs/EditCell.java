/*
 * EditCell.java
 *
 * Created on December 1, 2003, 11:49 AM
 */

package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;


/**
 *
 * @author  strubin
 */
public class EditCell extends javax.swing.JDialog
{
	JList list;
	DefaultListModel model;
	List libList;
	List cellList;
	Library curLib;

	/** Creates new form EditCell */
	public EditCell(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// make an empty list for the cell names
		model = new DefaultListModel();
		list = new JList(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		Center.setViewportView(list);
		list.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt)
			{
				listClick(evt);
			}
		});

		// make a popup of libraries
		libList = Library.getVisibleLibrariesSortedByName();
		for(Iterator it = libList.iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			jComboBox1.addItem(lib.getLibName());
		}
		curLib = Library.getCurrent();
		int curIndex = libList.indexOf(curLib);
		if (curIndex >= 0) jComboBox1.setSelectedIndex(curIndex);
		showCellsInLibrary();
	}

	private void showCellsInLibrary()
	{
		cellList = curLib.getCellsSortedByName();
		model.clear();
		for(Iterator it = cellList.iterator(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			model.addElement(cell.describe());
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

        Center = new javax.swing.JScrollPane();
        Top = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        Bottom = new javax.swing.JPanel();
        jSeparator1 = new javax.swing.JSeparator();
        jPanel1 = new javax.swing.JPanel();
        jCheckBox3 = new javax.swing.JCheckBox();
        jCheckBox2 = new javax.swing.JCheckBox();
        jCheckBox1 = new javax.swing.JCheckBox();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jCheckBox4 = new javax.swing.JCheckBox();
        jTextField1 = new javax.swing.JTextField();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.BorderLayout(0, 10));

        setTitle("Edit Cell");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        Center.setMinimumSize(new java.awt.Dimension(200, 100));
        Center.setPreferredSize(new java.awt.Dimension(200, 100));
        getContentPane().add(Center, java.awt.BorderLayout.CENTER);

        Top.setLayout(new java.awt.BorderLayout(10, 0));

        Top.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(5, 5, 5, 5)));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel4.setText("Library");
        Top.add(jLabel4, java.awt.BorderLayout.WEST);

        jComboBox1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                libraryPopup(evt);
            }
        });

        Top.add(jComboBox1, java.awt.BorderLayout.CENTER);

        getContentPane().add(Top, java.awt.BorderLayout.NORTH);

        Bottom.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        Bottom.add(jSeparator1, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jCheckBox3.setText("Make new window for cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        jPanel1.add(jCheckBox3, gridBagConstraints);

        jCheckBox2.setText("Show cells from Cell-Library");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        jPanel1.add(jCheckBox2, gridBagConstraints);

        jCheckBox1.setText("Show old versions");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        jPanel1.add(jCheckBox1, gridBagConstraints);

        jButton1.setText("Cancel");
        jButton1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cancel(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(jButton1, gridBagConstraints);

        jButton2.setText("New Cell");
        jButton2.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                newCell(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(jButton2, gridBagConstraints);

        jButton3.setText("OK");
        jButton3.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ok(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(jButton3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        Bottom.add(jPanel1, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jCheckBox4.setText("Confirm deletion");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weighty = 0.5;
        jPanel2.add(jCheckBox4, gridBagConstraints);

        jTextField1.setColumns(20);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        jPanel2.add(jTextField1, gridBagConstraints);

        jButton4.setText("Rename");
        jButton4.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rename(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel2.add(jButton4, gridBagConstraints);

        jButton5.setText("Delete");
        jButton5.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                delete(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel2.add(jButton5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        Bottom.add(jPanel2, gridBagConstraints);

        getContentPane().add(Bottom, java.awt.BorderLayout.SOUTH);

        pack();
    }//GEN-END:initComponents

	private void libraryPopup(java.awt.event.ActionEvent evt)//GEN-FIRST:event_libraryPopup
	{//GEN-HEADEREND:event_libraryPopup
		// the popup of libraies changed
		JComboBox cb = (JComboBox)evt.getSource();
		int index = cb.getSelectedIndex();
		curLib = (Library)libList.get(index);
		showCellsInLibrary();
	}//GEN-LAST:event_libraryPopup

	private void delete(java.awt.event.ActionEvent evt)//GEN-FIRST:event_delete
	{//GEN-HEADEREND:event_delete
		// Add your handling code here:
	}//GEN-LAST:event_delete

	private void rename(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rename
	{//GEN-HEADEREND:event_rename
		// Add your handling code here:
	}//GEN-LAST:event_rename

	private void cancel(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancel
	{//GEN-HEADEREND:event_cancel
		setVisible(false);
		dispose();
	}//GEN-LAST:event_cancel

	private void newCell(java.awt.event.ActionEvent evt)//GEN-FIRST:event_newCell
	{//GEN-HEADEREND:event_newCell
		// Add your handling code here:
	}//GEN-LAST:event_newCell

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok
		setVisible(false);
		dispose();
	}//GEN-LAST:event_ok
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

	private void listClick(java.awt.event.MouseEvent evt)
	{
		int index = list.getSelectedIndex();
		Cell cell = (Cell)cellList.get(index);
		System.out.println("Clicked cell " + cell.describe());
	}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Bottom;
    private javax.swing.JScrollPane Center;
    private javax.swing.JPanel Top;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JCheckBox jCheckBox4;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
	
}
