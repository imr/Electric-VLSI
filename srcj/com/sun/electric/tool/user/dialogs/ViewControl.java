/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ViewControl.java
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

import com.sun.electric.database.hierarchy.View;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;

import java.util.Iterator;
import java.util.List;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;


/**
 * Class to handle the "View Control" dialog.
 */
public class ViewControl extends EDialog
{
	private DefaultListModel listModel;
	private JList list;
	private View currentView;

	/** Creates new form View Control */
	public ViewControl(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// make the list of Views
		listModel = new DefaultListModel();
		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listPane.setViewportView(list);
		list.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt) { viewListClick(); }
		});

		// load all of the Views
		loadViews();
		finishInitialization();
	}

	protected void escapePressed() { done(null); }

	private void viewListClick()
	{
		String viewLine = (String)list.getSelectedValue();
		int curlyPos = viewLine.indexOf('{');
		if (curlyPos >= 0) viewLine = viewLine.substring(0, curlyPos-1);
		currentView = View.findView(viewLine);
		if (currentView != null)
		{
			name.setText(currentView.getFullName());
			abbreviation.setText(currentView.getAbbreviation());
			textView.setSelected(currentView.isTextView());
			delete.setEnabled(!currentView.isPermanentView());
		}
	}

	private void loadViews()
	{
		listModel.clear();
		List<View> views = View.getOrderedViews();
		for(Iterator<View> it = views.iterator(); it.hasNext(); )
		{
			View v = (View)it.next();
			listModel.addElement(v.getFullName() + " {" + v.getAbbreviation() + "}");
		}
		list.setSelectedIndex(0);
		viewListClick();
	}

	/**
	 * Class to delete a View in a new thread.
	 */
	private static class DeleteView extends Job
	{
		View view;
		ViewControl dialog;

		protected DeleteView(View view, ViewControl dialog)
		{
			super("Delete View " + view.getFullName(), User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.view = view;
			this.dialog = dialog;
			startJob();
		}

		public boolean doIt()
		{
			view.kill();
			dialog.loadViews();
			return true;
		}
	}

	/**
	 * Class to create a View in a new thread.
	 */
	private static class CreateView extends Job
	{
		String viewName;
		String viewAbbr;
		boolean isText;
		ViewControl dialog;

		protected CreateView(String viewName, String viewAbbr, boolean isText, ViewControl dialog)
		{
			super("Create View " + viewName, User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.viewName = viewName;
			this.viewAbbr = viewAbbr;
			this.isText = isText;
			this.dialog = dialog;
			startJob();
		}

		public boolean doIt()
		{
			View newView = null;
			if (isText)
			{
				newView = View.newTextInstance(viewName, viewAbbr);
			} else
			{
				newView = View.newInstance(viewName, viewAbbr);
			}
			dialog.loadViews();
			return true;
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

        done = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        listPane = new javax.swing.JScrollPane();
        jLabel2 = new javax.swing.JLabel();
        name = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        abbreviation = new javax.swing.JTextField();
        textView = new javax.swing.JCheckBox();
        create = new javax.swing.JButton();
        delete = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("View Control");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        done.setText("Done");
        done.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                done(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(done, gridBagConstraints);

        jLabel1.setText("Views:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel1, gridBagConstraints);

        listPane.setMinimumSize(new java.awt.Dimension(300, 200));
        listPane.setPreferredSize(new java.awt.Dimension(300, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(listPane, gridBagConstraints);

        jLabel2.setText("View name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel2, gridBagConstraints);

        name.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(name, gridBagConstraints);

        jLabel3.setText("Abbreviation:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel3, gridBagConstraints);

        abbreviation.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(abbreviation, gridBagConstraints);

        textView.setText("Text view");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(textView, gridBagConstraints);

        create.setText("Create");
        create.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                createActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(create, gridBagConstraints);

        delete.setText("Delete");
        delete.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                deleteActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(delete, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void createActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_createActionPerformed
	{//GEN-HEADEREND:event_createActionPerformed
		String viewName = name.getText();
		String viewAbbr = abbreviation.getText();
		boolean isText = textView.isSelected();
		CreateView job = new CreateView(viewName, viewAbbr, isText, this);
	}//GEN-LAST:event_createActionPerformed

	private void deleteActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_deleteActionPerformed
	{//GEN-HEADEREND:event_deleteActionPerformed
		if (currentView == null) return;
		DeleteView job = new DeleteView(currentView, this);
	}//GEN-LAST:event_deleteActionPerformed

	private void done(java.awt.event.ActionEvent evt)//GEN-FIRST:event_done
	{//GEN-HEADEREND:event_done
		closeDialog(null);
	}//GEN-LAST:event_done

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField abbreviation;
    private javax.swing.JButton create;
    private javax.swing.JButton delete;
    private javax.swing.JButton done;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane listPane;
    private javax.swing.JTextField name;
    private javax.swing.JCheckBox textView;
    // End of variables declaration//GEN-END:variables

}
