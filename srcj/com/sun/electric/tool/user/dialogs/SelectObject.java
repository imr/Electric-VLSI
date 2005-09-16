/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SelectObject.java
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

import com.sun.electric.database.change.DatabaseChangeEvent;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.WindowContent;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;


/**
 * Class to handle the "Select Object" dialog.
 */
public class SelectObject extends EDialog implements DatabaseChangeListener
{
	private static final int NODES   = 1;
	private static final int ARCS    = 2;
	private static final int EXPORTS = 3;
	private static final int NETS    = 4;
	private static int what = NODES;
	private Cell cell;
	private JList list;
	private DefaultListModel model;
    private Highlighter highlighter;

	public static void selectObjectDialog()
	{
		SelectObject dialog = new SelectObject(TopLevel.getCurrentJFrame(), false);
		dialog.setVisible(true);
	}

	/** Creates new form Search and Replace */
	private SelectObject(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
		getRootPane().setDefaultButton(done);
		Undo.addDatabaseChangeListener(this);

		switch (what)
		{
			case NODES:   nodes.setSelected(true);      break;
			case ARCS:    arcs.setSelected(true);       break;
			case EXPORTS: exports.setSelected(true);    break;
			case NETS:    networks.setSelected(true);   break;
		}

		model = new DefaultListModel();
		list = new JList(model);
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		objectPane.setViewportView(list);
		list.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt) { listClicked(); }
		});

		done.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { closeDialog(null); }
		});

		nodes.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { buttonClicked(); }
		});
		arcs.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { buttonClicked(); }
		});
		exports.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { buttonClicked(); }
		});
		networks.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { buttonClicked(); }
		});
		buttonClicked();
		finishInitialization();
	}

	protected void escapePressed() { closeDialog(null); }

	/**
	 * Respond to database changes and reload the list.
	 * @param e database change event
	 */
	public void databaseChanged(DatabaseChangeEvent e)
	{
		if (!isVisible()) return;
		buttonClicked();
	}

	private void listClicked()
	{
		int [] si = list.getSelectedIndices();
		if (si.length > 0) highlighter.clear();
		Netlist netlist = cell.acquireUserNetlist();
		if (netlist == null)
		{
			System.out.println("Sorry, a deadlock aborted selection (network information unavailable).  Please try again");
			return;
		}
		for(int i=0; i<si.length; i++)
		{
			int index = si[i];
			String s = (String)model.get(index);
			if (nodes.isSelected())
			{
				// find nodes
				for(Iterator it = cell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = (NodeInst)it.next();
					if (s.equals(ni.getName()))
					{
						highlighter.addElectricObject(ni, cell);
						break;
					}
				}
			} else if (arcs.isSelected())
			{
				// find arcs
				for(Iterator it = cell.getArcs(); it.hasNext(); )
				{
					ArcInst ai = (ArcInst)it.next();
					if (s.equals(ai.getName()))
					{
						highlighter.addElectricObject(ai, cell);
						break;
					}
				}
			} else if (exports.isSelected())
			{
				// find exports
				for(Iterator it = cell.getPorts(); it.hasNext(); )
				{
					Export pp = (Export)it.next();
					if (s.equals(pp.getName()))
					{
						highlighter.addText(pp, cell, null, null);
						break;
					}
				}
			} else
			{
				// find networks
				for(Iterator it = netlist.getNetworks(); it.hasNext(); )
				{
					Network net = (Network)it.next();
					String netName = net.describe(false);
					if (netName.length() == 0) continue;
					if (s.equals(netName))
					{
						highlighter.addNetwork(net, cell);
						break;
					}
				}
			}
		}
		if (si.length > 0)
		{
	        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			Highlight line1 = null, line2 = null, line3 = null, line4 = null;
	        if (wf != null && wf.getContent() instanceof EditWindow)
			{
				EditWindow wnd = (EditWindow)wf.getContent();
				Rectangle2D bounds = highlighter.getHighlightedArea(wnd);
				double boundsArea = bounds.getWidth() * bounds.getHeight();
				Rectangle2D displayBounds = wnd.displayableBounds();
				double displayArea = displayBounds.getWidth() * displayBounds.getHeight();
				
				// if objects are offscreen, point the way
				if (bounds.getMinX() >= displayBounds.getMaxX() ||
						bounds.getMaxX() <= displayBounds.getMinX() ||
						bounds.getMinY() >= displayBounds.getMaxY() ||
						bounds.getMaxY() <= displayBounds.getMinY())
				{
					Point2D fromPt = new Point2D.Double(displayBounds.getCenterX(), displayBounds.getCenterY());
					Point2D toPt = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
					GenMath.clipLine(fromPt, toPt, displayBounds.getMinX(), displayBounds.getMaxX(),
							displayBounds.getMinY(), displayBounds.getMaxY());
					if (fromPt.getX() != displayBounds.getCenterX() || fromPt.getY() != displayBounds.getCenterY())
					{
						// clipLine may swap points: swap them back
						Point2D swap = fromPt;
						fromPt = toPt;
						toPt = swap;
					}
					line1 = highlighter.addLine(fromPt, toPt, cell);
					int angle = GenMath.figureAngle(fromPt, toPt);
					double headLength = fromPt.distance(toPt) / 10;
					double xLeft = toPt.getX() - headLength * DBMath.cos(angle+150);
					double yLeft = toPt.getY() - headLength * DBMath.sin(angle+150);
					double xRight = toPt.getX() - headLength * DBMath.cos(angle-150);
					double yRight = toPt.getY() - headLength * DBMath.sin(angle-150);
					line2 = highlighter.addLine(new Point2D.Double(xLeft, yLeft), toPt, cell);
					line3 = highlighter.addLine(new Point2D.Double(xRight, yRight), toPt, cell);
				} else
				{
					// if displayed objects are very small, point them out
					if (boundsArea * 500 <  displayArea)
					{
						if (bounds.getMinX() > displayBounds.getMinX() && bounds.getMinY() > displayBounds.getMinY())
							line1 = highlighter.addLine(new Point2D.Double(displayBounds.getMinX(), displayBounds.getMinY()),
								new Point2D.Double(bounds.getMinX(), bounds.getMinY()), cell);
	
						if (bounds.getMinX() > displayBounds.getMinX() && bounds.getMaxY() < displayBounds.getMaxY())
							line2 = highlighter.addLine(new Point2D.Double(displayBounds.getMinX(), displayBounds.getMaxY()),
								new Point2D.Double(bounds.getMinX(), bounds.getMaxY()), cell);
	
						if (bounds.getMaxX() < displayBounds.getMaxX() && bounds.getMinY() > displayBounds.getMinY())
							line3 = highlighter.addLine(new Point2D.Double(displayBounds.getMaxX(), displayBounds.getMinY()),
								new Point2D.Double(bounds.getMaxX(), bounds.getMinY()), cell);
	
						if (bounds.getMaxX() < displayBounds.getMaxX() && bounds.getMaxY() < displayBounds.getMaxY())
							line4 = highlighter.addLine(new Point2D.Double(displayBounds.getMaxX(), displayBounds.getMaxY()),
								new Point2D.Double(bounds.getMaxX(), bounds.getMaxY()), cell);
					}
				}
			}
			highlighter.finished();

			// if there was temporary identification, queue a timer to turn it off
			if (line1 != null || line2 != null || line3 != null || line4 != null)
			{
				Timer timer = new Timer(500, new FlashActionListener(highlighter, line1, line2, line3, line4));
				timer.setRepeats(false);
				timer.start();
			}
		}
	}

	private static class FlashActionListener implements ActionListener
	{
		private Highlighter hl;
		private Highlight line1, line2, line3, line4;

		FlashActionListener(Highlighter hl, Highlight line1, Highlight line2, Highlight line3, Highlight line4)
		{
			this.hl = hl;
			this.line1 = line1;
			this.line2 = line2;
			this.line3 = line3;
			this.line4 = line4;
		}
	    public void actionPerformed(ActionEvent evt)
		{
			if (line1 != null) hl.remove(line1);
			if (line2 != null) hl.remove(line2);
			if (line3 != null) hl.remove(line3);
			if (line4 != null) hl.remove(line4);
			hl.finished();
			hl.getWindowFrame().getContent().repaint();
		}    
	}
	private void buttonClicked()
	{
		model.clear();
		cell = WindowFrame.getCurrentCell();
		if (cell == null) return;
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;
        WindowContent wc = wf.getContent();
        if (wc == null) return;
        highlighter = wc.getHighlighter();
        if (highlighter == null) return;


		List allNames = new ArrayList();
		if (nodes.isSelected())
		{
			// show all nodes
			what = NODES;
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				allNames.add(ni.getName());
			}
		} else if (arcs.isSelected())
		{
			// show all arcs
			what = ARCS;
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				allNames.add(ai.getName());
			}
		} else if (exports.isSelected())
		{
			// show all exports
			what = EXPORTS;
			for(Iterator it = cell.getPorts(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				allNames.add(pp.getName());
			}
		} else
		{
			// show all networks
			what = NETS;
			Netlist netlist = cell.getUserNetlist();
			for(Iterator it = netlist.getNetworks(); it.hasNext(); )
			{
				Network net = (Network)it.next();
				String netName = net.describe(false);
				if (netName.length() == 0) continue;
				allNames.add(netName);
			}
		}
		Collections.sort(allNames, TextUtils.STRING_NUMBER_ORDER);
		for(Iterator it = allNames.iterator(); it.hasNext(); )
		{
			String s = (String)it.next();
			model.addElement(s);
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

        whatGroup = new javax.swing.ButtonGroup();
        done = new javax.swing.JButton();
        objectPane = new javax.swing.JScrollPane();
        nodes = new javax.swing.JRadioButton();
        exports = new javax.swing.JRadioButton();
        arcs = new javax.swing.JRadioButton();
        networks = new javax.swing.JRadioButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Select Object");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        done.setText("Done");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(done, gridBagConstraints);

        objectPane.setMinimumSize(new java.awt.Dimension(200, 200));
        objectPane.setPreferredSize(new java.awt.Dimension(200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(objectPane, gridBagConstraints);

        nodes.setText("Nodes");
        whatGroup.add(nodes);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(nodes, gridBagConstraints);

        exports.setText("Exports");
        whatGroup.add(exports);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(exports, gridBagConstraints);

        arcs.setText("Arcs");
        whatGroup.add(arcs);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(arcs, gridBagConstraints);

        networks.setText("Networks");
        whatGroup.add(networks);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(networks, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton arcs;
    private javax.swing.JButton done;
    private javax.swing.JRadioButton exports;
    private javax.swing.JRadioButton networks;
    private javax.swing.JRadioButton nodes;
    private javax.swing.JScrollPane objectPane;
    private javax.swing.ButtonGroup whatGroup;
    // End of variables declaration//GEN-END:variables
}
