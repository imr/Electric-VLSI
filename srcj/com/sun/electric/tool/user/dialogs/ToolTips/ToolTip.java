/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellBrowser.java
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

 package com.sun.electric.tool.user.dialogs.ToolTips;

import javax.swing.*;
import javax.swing.text.EditorKit;
import java.util.prefs.Preferences;
import java.util.*;
import java.util.List;
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.awt.*;
import java.awt.event.ItemListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: May 14, 2004
 * Time: 12:11:03 PM
 *
 * A Dialog for displaying useful tips on how to use Electric effectively (or at all).
 */
public class ToolTip extends javax.swing.JDialog {

    private static Preferences prefs = Preferences.userNodeForPackage(ToolTip.class);
    public static final String showOnStartUp = "ShowOnStartUp";

    private static Random rand = new Random(10943048109348l);
    public static final String [] allToolTips = {
        "Mouse Interface", "Getting Started", "Library Directory Management"
    };

    private JPanel controlPanel;
    private JScrollPane scrollPane;
    private JEditorPane editorPane;
    private JCheckBox enableToolTipCheckBox;
    private JLabel label;
    private JButton nextToolTip;
    private JButton prevToolTip;
    private JButton closeButton;
    private JComboBox toolTips;

    /**
     * Create a new Tool Tip dialog.
     * @param parent
     * @param modal
     * @param tooltip A String naming the tool tip file. If null, random tip chosen.
     */
    public ToolTip(java.awt.Frame parent, boolean modal, String tooltip) {
        super(parent, modal);
        setTitle("Tool Tips!");
        init();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension labelSize = getSize();
        setLocation(screenSize.width/2 - (labelSize.width/2),
            screenSize.height/2 - (labelSize.height/2));
        // look for tooltip
        loadToolTip(tooltip);
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
     * @param toolTipName the Tool Tip name
     * @return a converted name of the file, with .html appended.
     */
    public String toolTipNameToFileName(String toolTipName) {
        // replace white space with _
        toolTipName = toolTipName.replaceAll("\\s", "_");
        toolTipName = toolTipName.toLowerCase();
        return toolTipName + ".html";
    }

    /**
     * Load the tool tip.  Returns true on sucess, false otherwise
     */
    private void loadToolTip(String tooltip) {
        // if url null, get random one
        List list = Arrays.asList(allToolTips);
        int i = list.indexOf(tooltip);
        if (i == -1) {
            i = rand.nextInt() % allToolTips.length;
        }
        loadToolTip(i);
    }

    private void loadToolTip(int i) {
        URL url = ToolTip.class.getResource(toolTipNameToFileName(allToolTips[i]));
        try {
            editorPane.setPage(url);
        } catch (IOException e) {
            //editorPane.setText("Tool tip "+allToolTips[i]+" not found\n");
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
        toolTips = new JComboBox(allToolTips);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(toolTips, gridBagConstraints);
        toolTips.addItemListener(new ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                toolTipsItemStateChanged(evt);
            }
        });

        // set up editor pane
        editorPane = new JEditorPane();
        editorPane.setEditable(false);

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
        enableToolTipCheckBox = new JCheckBox("Show ToolTips on start up (Tool Tips can be accessed from Help Menu)");
        boolean selected = prefs.getBoolean(showOnStartUp, true);
        enableToolTipCheckBox.setSelected(selected);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(enableToolTipCheckBox, gridBagConstraints);

        // set up control buttons
        controlPanel = new JPanel(new GridBagLayout());
        prevToolTip = new JButton("Previous Tip");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        controlPanel.add(prevToolTip, gridBagConstraints);
        prevToolTip.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                prevToolTipActionPerformed(evt);
            }
        });
        nextToolTip = new JButton("Next Tip");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        controlPanel.add(nextToolTip, gridBagConstraints);
        nextToolTip.addActionListener(new ActionListener() {
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
        loadToolTip(toolTips.getSelectedIndex());
    }

    private void prevToolTipActionPerformed(ActionEvent evt) {
        int i = toolTips.getSelectedIndex();
        if (i == 0) return;
        toolTips.setSelectedIndex(i-1);
    }

    private void nextToolTipActionPerformed(ActionEvent evt) {
        int i = toolTips.getSelectedIndex();
        if (i == (allToolTips.length-1)) return;
        toolTips.setSelectedIndex(i+1);
    }

    private void closeButtonActionPerformed(ActionEvent evt) {
        closeDialog(null);
    }

    private void closeDialog(java.awt.event.WindowEvent evt) {
        prefs.putBoolean(showOnStartUp, enableToolTipCheckBox.isSelected());
        setVisible(false);
        dispose();
    }

}
