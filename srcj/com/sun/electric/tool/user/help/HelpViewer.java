/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HelpViewer.java
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

package com.sun.electric.tool.user.help;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

/**
 * User: gainsley
 * Date: May 14, 2004
 * Time: 12:11:03 PM
 * <p>
 * A Dialog for displaying useful tips on how to use Electric effectively (or at all).
 *
 * <p>
 * To add a new Tool Tip, create an html file in the ToolTips java source directory
 * (i.e. package directory). Then add the file name (sans .html) to the allToolTips array
 * in the ToolTip class.  Note that names in the allToolTips array get converted to a
 * file name using the following properties: 1) spaces converted to underscore, 2) name
 * converted to lower case, 3) ".html" appended.
 * <p>Ex: "Mouse Interface" --> mouse_interface.html
 *
 */
public class HelpViewer extends javax.swing.JDialog {

    private static Preferences prefs = Preferences.userNodeForPackage(HelpViewer.class);
    public static final String showOnStartUp = "ShowOnStartUp";

    private static Random rand = new Random(10943048109348l);
    public static final String [] allTips = {
        "Mouse Interface", "Getting Started", "Library Directory Management"
    };

    private int currentTip;

    private JPanel controlPanel;
    private JScrollPane scrollPane;
    private JEditorPane editorPane;
    private JCheckBox enableTipCheckBox;
    private JLabel label;
    private JButton nextTip;
    private JButton prevTip;
    private JButton closeButton;
    private JComboBox tips;

    /**
     * Create a new Tool Tip dialog.
     * @param parent
     * @param modal
     * @param tip A String naming the tool tip file. If null, random tip chosen.
     */
    public HelpViewer(java.awt.Frame parent, boolean modal, String tip) {
        super(parent, modal);
        setTitle("Help!");
        init();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension labelSize = getSize();
        setLocation(screenSize.width/2 - (labelSize.width/2),
            screenSize.height/2 - (labelSize.height/2));
        currentTip = -1;
        // look for tooltip
        loadTip(tip);
    }

    /**
     * Convert a Tool Tip name to a file name containing
     * the data for the tool tip. This replaces spaces with _ and
     * changes everything to lower case, for platform independence.
     * File names should be like-wise named. Ex:
     * <p>"Mouse Interface" --> mouse_interface.html
     * <p>Note: Unfortunately, it appears there is no way to find all
     * Resources in a package. You *must* know the name of the resource
     * beforehand.
     *
     * @param tipName the Tool Tip name
     * @return a converted name of the file, with .html appended.
     */
    public String tipNameToFileName(String tipName) {
        // replace white space with _
        tipName = tipName.replaceAll("\\s", "_");
        tipName = tipName.toLowerCase();
        return tipName + ".html";
    }

    /**
     * Load the tool tip.  Returns true on sucess, false otherwise
     */
    private void loadTip(String tip) {
        // if url null, get random one
        List list = Arrays.asList(allTips);
        int i = list.indexOf(tip);
        if (i == -1) {
            i = rand.nextInt(allTips.length);
            i = Math.abs(i);
        }
        loadTip(i);
    }

    private void loadTip(int i) {
        if (i == currentTip) return;
        URL url = HelpViewer.class.getResource("helphtml/"+tipNameToFileName(allTips[i]));
        try {
            editorPane.setPage(url);
            currentTip = i;
            tips.setSelectedIndex(i);
        } catch (IOException e) {
            System.out.println("Tool tip "+allTips[i]+" not found");
        }
    }

	private static class Hyperactive implements HyperlinkListener
	{
 		public void hyperlinkUpdate(HyperlinkEvent e)
		{
			 if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
			 {
				JEditorPane pane = (JEditorPane)e.getSource();
			 	if (e instanceof HTMLFrameHyperlinkEvent)
				{
					HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent)e;
					HTMLDocument doc = (HTMLDocument)pane.getDocument();
					doc.processHTMLFrameHyperlinkEvent(evt);
			 	} else
			 	{
					try
					{
						pane.setPage(e.getURL());
					} catch (Throwable t)
					{
						System.out.println("Cannot find URL "+e.getURL());
					}
			 	}
			}
		}
	}

    /**
     * Initialize list of all ToolTips and initilize components
     */
    private void init() {

        // set up dialog
        GridBagConstraints gridBagConstraints;
        getContentPane().setLayout(new GridBagLayout());

        // set up combo box
        label = new JLabel("Jump to Tool Tip: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(label, gridBagConstraints);

        // set up combo list of all tool tips
        tips = new JComboBox(allTips);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(tips, gridBagConstraints);
        tips.addItemListener(new ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                toolTipsItemStateChanged(evt);
            }
        });

        // set up editor pane
        editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.addHyperlinkListener(new Hyperactive());

        // set up scroll pane
        scrollPane = new JScrollPane(editorPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(700, 560));
        scrollPane.setMinimumSize(new Dimension(400, 300));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(scrollPane, gridBagConstraints);

        // set up check box
        enableTipCheckBox = new JCheckBox("Show Tips on start up (Help Tips can be accessed from Help Menu)");
        boolean selected = prefs.getBoolean(showOnStartUp, true);
        enableTipCheckBox.setSelected(selected);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(enableTipCheckBox, gridBagConstraints);

        // set up control buttons
        controlPanel = new JPanel(new GridBagLayout());
        prevTip = new JButton("Previous Tip");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        controlPanel.add(prevTip, gridBagConstraints);
        prevTip.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                prevToolTipActionPerformed(evt);
            }
        });
        nextTip = new JButton("Next Tip");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        controlPanel.add(nextTip, gridBagConstraints);
        nextTip.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                nextToolTipActionPerformed(evt);
            }
        });
        closeButton = new JButton("Close");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        controlPanel.add(closeButton, gridBagConstraints);
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        //gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(controlPanel, gridBagConstraints);

        // close of dialog event
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        pack();
    }

    private void toolTipsItemStateChanged(java.awt.event.ItemEvent evt) {
        loadTip(tips.getSelectedIndex());
    }

    private void prevToolTipActionPerformed(ActionEvent evt) {
        int i = tips.getSelectedIndex();
        if (i == 0) return;
        tips.setSelectedIndex(i-1);
    }

    private void nextToolTipActionPerformed(ActionEvent evt) {
        int i = tips.getSelectedIndex();
        if (i == (allTips.length-1)) return;
        tips.setSelectedIndex(i+1);
    }

    private void closeButtonActionPerformed(ActionEvent evt) {
        closeDialog(null);
    }

    private void closeDialog(java.awt.event.WindowEvent evt) {
        prefs.putBoolean(showOnStartUp, enableTipCheckBox.isSelected());
        setVisible(false);
        dispose();
    }

}
