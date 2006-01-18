/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SavedViews.java
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
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.Point2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

/**
 * Class to handle the "Saved Views" dialog.
 */
public class SavedViews extends EDialog implements HighlightListener
{
	private static SavedViews theDialog = null;
	private JList viewList;
	private DefaultListModel viewListModel;
    private EditWindow wnd;

	public static void showSavedViewsDialog()
	{
		if (theDialog == null)
		{
			JFrame jf = null;
			if (TopLevel.isMDIMode())
				jf = TopLevel.getCurrentJFrame();
			theDialog = new SavedViews(jf, false);
		}
		theDialog.setVisible(true);
	}

	/** Creates new form Saved Views */
	private SavedViews(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// build the change list
		viewListModel = new DefaultListModel();
		viewList = new JList(viewListModel);
		viewList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		viewPane.setViewportView(viewList);
		viewList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2) restoreView(null);
			}
		});

		finishInitialization();
		loadInfo();
	}

	protected void escapePressed() { done(null); }

	private void loadInfo()
	{
		// update current window
		EditWindow curWnd = EditWindow.getCurrent();
		if (wnd != curWnd && curWnd != null)
		{
			if (wnd != null) wnd.getHighlighter().removeHighlightListener(this);
			curWnd.getHighlighter().addHighlightListener(this);
			wnd = curWnd;
		}

		viewListModel.clear();
		if (wnd == null) return;
		Cell cell = wnd.getCell();
		if (cell == null) return;

		boolean found = false;
		for(Iterator<Variable> it = cell.getVariables(); it.hasNext(); )
		{
			Variable var = it.next();
			String name = var.getKey().getName();
			if (name.startsWith("USER_windowview_"))
			{
				viewListModel.addElement(name.substring(16));
				found = true;
			}
		}
		if (found) viewList.setSelectedIndex(0);
	}

	/**
	 * Reloads the dialog when Highlights change
	 */
	public void highlightChanged(Highlighter which)
	{
		if (!isVisible()) return;
		loadInfo();
	}

	/**
	 * Called when by a Highlighter when it loses focus. The argument
	 * is the Highlighter that has gained focus (may be null).
	 * @param highlighterGainedFocus the highlighter for the current window (may be null).
	 */
	public void highlighterLostFocus(Highlighter highlighterGainedFocus)
	{
		if (!isVisible()) return;
		loadInfo();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        changeOption = new javax.swing.ButtonGroup();
        done = new javax.swing.JButton();
        restoreView = new javax.swing.JButton();
        viewPane = new javax.swing.JScrollPane();
        jLabel1 = new javax.swing.JLabel();
        viewName = new javax.swing.JTextField();
        saveView = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Window Views");
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
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(done, gridBagConstraints);

        restoreView.setText("Restore View");
        restoreView.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                restoreView(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(restoreView, gridBagConstraints);

        viewPane.setMinimumSize(new java.awt.Dimension(150, 150));
        viewPane.setPreferredSize(new java.awt.Dimension(150, 150));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(viewPane, gridBagConstraints);

        jLabel1.setText("View name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel1, gridBagConstraints);

        viewName.setColumns(10);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(viewName, gridBagConstraints);

        saveView.setText("Save This View");
        saveView.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                saveViewActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(saveView, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void saveViewActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_saveViewActionPerformed
	{//GEN-HEADEREND:event_saveViewActionPerformed
		// get the saved name
		String name = viewName.getText().trim();
		if (name.length() == 0) return;

		// make sure there is a cell
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;
		if (!(wf.getContent() instanceof EditWindow)) return;
		EditWindow wnd = (EditWindow)wf.getContent();
		Cell cell = wnd.getCell();
		if (cell == null) return;

		// save the view
		double scale = wnd.getScale();
		Point2D off = wnd.getOffset();
		SaveViewJob job = new SaveViewJob(this, cell, name, scale, off.getX(), off.getY());
	}//GEN-LAST:event_saveViewActionPerformed

	/**
	 * Class to change the node/arc type in a new thread.
	 */
	private static class SaveViewJob extends Job
	{
		private transient SavedViews dialog;
		private Cell cell;
		private String name;
		private double scale, offX, offY;

		protected SaveViewJob(SavedViews dialog, Cell cell, String name, double scale, double offX, double offY)
		{
			super("Save Window View", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;
			this.cell = cell;
			this.name = name;
			this.scale = scale;
			this.offX = offX;
			this.offY = offY;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			Double [] pos = new Double[3];
			pos[0] = new Double(scale);
			pos[1] = new Double(offX);
			pos[2] = new Double(offY);
			cell.newVar("USER_windowview_" + name, pos);
			return true;
		}

        public void terminateOK()
        {
			dialog.loadInfo();
        }
	}

	private void done(java.awt.event.ActionEvent evt)//GEN-FIRST:event_done
	{//GEN-HEADEREND:event_done
		closeDialog(null);
	}//GEN-LAST:event_done

	private void restoreView(java.awt.event.ActionEvent evt)//GEN-FIRST:event_restoreView
	{//GEN-HEADEREND:event_restoreView
		// get the current cell
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;
		if (!(wf.getContent() instanceof EditWindow)) return;
		EditWindow wnd = (EditWindow)wf.getContent();
		Cell cell = wnd.getCell();
		if (cell == null) return;

		// get the saved information
        int index = viewList.getSelectedIndex();
        String name = (String)viewListModel.get(index);
        Variable var = cell.getVar("USER_windowview_" + name);
        if (var == null) return;

        // adjust the window
        Double [] pos = (Double[])var.getObject();
        wnd.setScale(pos[0].doubleValue());
        Point2D off = new Point2D.Double(pos[1].doubleValue(), pos[2].doubleValue());
        wnd.setOffset(off);
        wnd.repaintContents(null, false);
	}//GEN-LAST:event_restoreView

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
		theDialog = null;		
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup changeOption;
    private javax.swing.JButton done;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JButton restoreView;
    private javax.swing.JButton saveView;
    private javax.swing.JTextField viewName;
    private javax.swing.JScrollPane viewPane;
    // End of variables declaration//GEN-END:variables

}
