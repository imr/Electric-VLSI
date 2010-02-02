/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ChainG.java
 * Written by Eric Kim, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.simulation.test;

import javax.swing.*;
import java.awt.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.io.*;

/**
 * ChainG, chain utility in GUI. This is the default main class for test.jar.
 */
public class ChainG extends JFrame implements TreeModelListener, ChainNode.ShiftListener {

    static final int WIDTH = 1800;

    static final int HEIGHT = 600;

    ChainControl chainControl = null;

    String fileName = null;

    JTree treeLeft = null;

    DefaultTreeModel treeModel = null;

    JTextArea bitsTextArea, logTextArea;

    JTextField pathText, systemComment, setInBitsText;

    JLabel labelPath, labelComment;

    JRadioButton outBitsButton, inBitsButton, outBitsExpButton;

    JButton setInBitsButton, shiftButton;

    JCheckBox readEnable, writeEnable;

    /** Creates a new instance of ChainG */
    public ChainG(String xml) {
        super("ChainG");
        createGUI();
        openFile(xml);
    }

    public ChainG(ChainControl cc) {
        super("ChainG");
        createGUI();
        setChainControl(cc);
    }

    /*
     * I don't know why this doesn't display the new hierarchy, because
     * chainControl is getting filled correctly (see the println below). It would
     * be too much trouble to figure out how this GUI stuff works!
     */
    private void openFile(String name) {
        if (name != null) {
            this.fileName = name;
            try {
                chainControl = new ChainControl(name);
            } catch (OutOfMemoryError e) {
                System.out.println("Out of memory, rerun with larger heap space using -Xmx1000m");
                System.exit(1);
            }
            if (chainControl != null)
                chainControl.resetInBits();
        }
        //        System.out.println("First chip in XML file: "
        //                + chainControl.system.getChild(0));
        setChainControl(chainControl);
    }

    private void setChainControl(ChainControl control) {
        if (control != null) {
            chainControl = control;
            treeModel = new DefaultTreeModel(chainControl.getSystem());
            treeLeft.setModel(treeModel);
            treeModel.addTreeModelListener(this);
            MyTreeNode system = chainControl.getSystem();
            for (int i=0; i<system.getChildCount(); i++) {
                MyTreeNode anode = system.getChildAt(i);
                if (!(anode instanceof ChipNode)) continue;
                ChipNode chip = (ChipNode)anode;
                for (int j=0; j<chip.getChildCount(); j++) {
                    ChainNode chain = (ChainNode)chip.getChildAt(j);
                    chain.addListener(this);
                }
            }
        } else {
            treeLeft.setModel(null);
        }
    }

    private void createGUI() {
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        JComponent contentPane = (JComponent) this.getContentPane();

        treeLeft = new JTree();

        MyRenderer myRenderer = new MyRenderer();
        treeLeft.setCellRenderer(myRenderer);

        /** left panel */
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        JPanel paneLeft = new JPanel(gridbag);
        c.fill = GridBagConstraints.BOTH;

        /** top panel of left panel */
        GridBagLayout gridbagTop = new GridBagLayout();
        JPanel displayPanel = new JPanel(gridbagTop);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 0;
        gridbag.setConstraints(displayPanel, c);
        paneLeft.add(displayPanel);

        c.fill = GridBagConstraints.BOTH;
        JLabel showLabel = new JLabel("Show:", SwingConstants.LEFT);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        gridbagTop.setConstraints(showLabel, c);
        displayPanel.add(showLabel);

        outBitsButton = new JRadioButton("OutBits", true);
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        gridbagTop.setConstraints(outBitsButton, c);
        displayPanel.add(outBitsButton);
        outBitsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                displayButtonChanged();
            }
        });

        inBitsButton = new JRadioButton("InBits", true);
        c.gridx = 2;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        gridbagTop.setConstraints(inBitsButton, c);
        displayPanel.add(inBitsButton);
        inBitsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                displayButtonChanged();
            }
        });

        outBitsExpButton = new JRadioButton("OutBitsExp", true);
        c.gridx = 3;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        gridbagTop.setConstraints(outBitsExpButton, c);
        displayPanel.add(outBitsExpButton);
        outBitsExpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                displayButtonChanged();
            }
        });

        ButtonGroup bitsToShowGroup = new ButtonGroup();
        bitsToShowGroup.add(outBitsButton);
        bitsToShowGroup.add(inBitsButton);
        bitsToShowGroup.add(outBitsExpButton);
        outBitsButton.setSelected(true);


        JScrollPane scrollPaneLeft = new JScrollPane(treeLeft);
        scrollPaneLeft
                .setPreferredSize(new Dimension(WIDTH / 2, HEIGHT * 4 / 5));
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 1;
        gridbag.setConstraints(scrollPaneLeft, c);
        paneLeft.add(scrollPaneLeft);

        labelPath = new JLabel("Path", SwingConstants.CENTER);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;
        gridbag.setConstraints(labelPath, c);
        paneLeft.add(labelPath);

        labelComment = new JLabel(" Comment ", SwingConstants.CENTER);
        c.gridx = 0;
        c.gridy = 3;
        gridbag.setConstraints(labelComment, c);
        paneLeft.add(labelComment);

        pathText = new JTextField();
        pathText.setEditable(false);
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 1;
        gridbag.setConstraints(pathText, c);
        paneLeft.add(pathText);

        systemComment = new JTextField();
        systemComment.setEditable(false);
        c.gridx = 1;
        c.gridy = 3;
        gridbag.setConstraints(systemComment, c);
        paneLeft.add(systemComment);

        setInBitsButton = new JButton("Set inBits to: ");
        setInBitsButton.setEnabled(false);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;
        gridbag.setConstraints(setInBitsButton, c);
        paneLeft.add(setInBitsButton);
        setInBitsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setSelectedInBits();
            }
        });

        setInBitsText = new JTextField();
        setInBitsText.setEditable(false);
        c.gridx = 1;
        c.gridy = 4;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;
        gridbag.setConstraints(setInBitsText, c);
        paneLeft.add(setInBitsText);

        shiftButton = new JButton("Shift Selected Chain");
        shiftButton.setEnabled(false);
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;
        gridbag.setConstraints(shiftButton, c);
        paneLeft.add(shiftButton);
        shiftButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doShift();
            }
        });

        GridBagLayout gridbagSOP = new GridBagLayout();
        JPanel shiftOptionsPanel = new JPanel(gridbagSOP);
        c.gridx = 1;
        c.gridy = 5;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;
        gridbag.setConstraints(shiftOptionsPanel, c);
        paneLeft.add(shiftOptionsPanel);

        readEnable = new JCheckBox("read enable", false);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        gridbagSOP.setConstraints(readEnable, c);
        shiftOptionsPanel.add(readEnable);

        writeEnable = new JCheckBox("write enable", false);
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        gridbagSOP.setConstraints(writeEnable, c);
        shiftOptionsPanel.add(writeEnable);

        /** bottom panel */
        gridbag = new GridBagLayout();
        JPanel paneBottom = new JPanel(gridbag);
        c.fill = GridBagConstraints.BOTH;

        bitsTextArea = new JTextArea(3, 10);
        bitsTextArea.setEditable(false);
        bitsTextArea.setLineWrap(true);
        JScrollPane scrollPaneBottom = new JScrollPane(bitsTextArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPaneBottom.setBorder(new TitledBorder("BitVector"));
        scrollPaneBottom.setBackground(Color.WHITE);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0.5;
        gridbag.setConstraints(scrollPaneBottom, c);
        paneBottom.add(scrollPaneBottom);

        logTextArea = new JTextArea(5, 10);
        logTextArea.setEditable(false);
        JScrollPane scrollPaneBottom2 = new JScrollPane(logTextArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPaneBottom2.setBorder(new TitledBorder("Log Window"));
        scrollPaneBottom2.setBackground(Color.WHITE);
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 0.5;
        gridbag.setConstraints(scrollPaneBottom2, c);
        paneBottom.add(scrollPaneBottom2);

        /** split panels */
        JSplitPane splitPaneBottom = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                paneLeft, paneBottom);
        contentPane.add(splitPaneBottom);

        /** menu bar */
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu menu = new JMenu("File");
        menu.setMnemonic('f');
        menuBar.add(menu);

        JMenuItem menuItem = new JMenuItem("Open File");
        menuItem.setMnemonic('o');
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                openPressed();
            }
        });
        menu.add(menuItem);

        menuItem = new JMenuItem("Reopen");
        menuItem.setMnemonic('r');
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                reopenPressed();
            }
        });
        menu.add(menuItem);

        menu = new JMenu("Edit");
        menu.setMnemonic('e');
        menuBar.add(menu);

        menuItem = new JMenuItem("Copy");
        menuItem.setMnemonic('c');
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                copyPressed();
            }
        });
        menu.add(menuItem);

        /** tree select listener */
        treeLeft.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                treeLeftSelChanged(e);
            }
        });

    }

    /** open xml file */
    private void openPressed() {
        String file = getFileName(true);
        if (file == null) return;
        logOut("Opening: " + file);
        openFile(file);
    }

    /** reopen file */
    private void reopenPressed() {
        logOut("Reopening: " + fileName);
        openFile(fileName);
    }

    protected String getFileName(boolean forOpen) {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File("."));
        int result = (forOpen ? (fc.showOpenDialog(this)) : fc
                .showSaveDialog(this));
        java.io.File chosenFile = fc.getSelectedFile();
        if (result == JFileChooser.APPROVE_OPTION && chosenFile != null)
            return chosenFile.getPath();
        return null; // return null if no file chosen or dialog cancelled
    }

    private void copyPressed() {
        logOut("copy not implemented");
    }

    private MyTreeNode getSelectedNode() {
        TreePath p = treeLeft.getSelectionPath();
        if (p != null) {
            return (MyTreeNode) p.getLastPathComponent();
        }
        return null;
    }

    /**
     * when system tree is clicked, display comment, bits and path
     * @param e event 
     */
    private void treeLeftSelChanged(TreeSelectionEvent e) {
        MyTreeNode node = getSelectedNode();
        if (node != null) {
            if (node.getComment() != null) {
                systemComment.setText(node.getComment());
            } else {
                systemComment.setText("");
            }
            if (node.getClass() == ChainNode.class
                    || node.getClass() == SubchainNode.class) {
                SubchainNode chainNode = (SubchainNode) node;
                bitsTextArea.setText("InBits:\t"+chainNode.getInBitsIndiscriminate().getState()+"\n"+
                                     "OutBits:\t"+chainNode.getOutBitsIndiscriminate().getState()+"\n"+
                                     "OutBitsExp:\t"+chainNode.getOldOutBitsExpected().getState());
                pathText.setText(chainNode.getPathString());
                setInBitsText.setText(chainNode.getInBitsIndiscriminate().getState());
            } else {
                bitsTextArea.setText("None Selected");
                pathText.setText("None Selected");
            }
        }
        updateButtonEnables();
    }

    private void updateButtonEnables() {
        if (inBitsButton.isSelected()) {
            MyTreeNode node = getSelectedNode();
            if ((node instanceof SubchainNode)) {
                setInBitsButton.setEnabled(true);
                setInBitsText.setEditable(true);
                shiftButton.setEnabled(true);
                readEnable.setEnabled(true);
                writeEnable.setEnabled(true);
                return;
            }
        }
        setInBitsButton.setEnabled(false);
        setInBitsText.setEditable(false);
        shiftButton.setEnabled(false);
        readEnable.setEnabled(false);
        writeEnable.setEnabled(false);
    }

    private void displayButtonChanged() {
        if (treeModel != null) {
            treeModel.nodeChanged(chainControl.getSystem());
        }
        updateButtonEnables();
    }

    private void doShift() {
        if (chainControl.getJtag() == null) {
            JOptionPane.showMessageDialog(this, "No Jtag Tester specified in ChainControl",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String path = pathText.getText();
        path = chainControl.getParentChain(path);
        shiftButton.setEnabled(false);
        shiftButton.setText("Shifting...");
        chainControl.shift(path, readEnable.isSelected(), writeEnable.isSelected(), Infrastructure.SEVERITY_WARNING,
                Infrastructure.SEVERITY_WARNING, Infrastructure.SEVERITY_WARNING);
    }

    public void shiftCompleted(ChainNode node) {
        treeModel.nodeChanged(node);
        shiftButton.setText("Shift Selected Chain");
        updateButtonEnables();
    }

    private void setSelectedInBits() {
        MyTreeNode node = getSelectedNode();
        if (node instanceof SubchainNode) {
            SubchainNode subchainNode = (SubchainNode)node;
            String newInBits = setInBitsText.getText().trim();
            int newBitsLength = newInBits.length();
            int nodeLength = subchainNode.getInBitsIndiscriminate().getNumBits();
            if (newBitsLength != nodeLength) {
                JOptionPane.showMessageDialog(this, "Cannot set node of "+nodeLength+" bits to string of "+newBitsLength+" bits",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            subchainNode.setInBits(newInBits);
            treeLeftSelChanged(null);
            treeModel.nodeChanged(chainControl.getSystem());
        }
    }

    private ChainNode getParentChainNode(MyTreeNode node) {
        if (node instanceof ChainNode) return (ChainNode)node;
        while (node != null) {
            node = node.getParent();
            if (node instanceof ChainNode) return (ChainNode)node;
        }
        return null;
    }

    /** main program: instantiate ChainModel and JFrame */
    public static void main(String[] argv) {
        final String xml;
        if (argv.length >= 1) {
            xml = argv[0];
        } else {
            xml = null;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { createAndShowGUI(xml); }
        });
    }

    public static void createAndShowGUI(ChainControl cc, String windowTitle) {
        JFrame mainFrame = new ChainG(cc);
        mainFrame.setTitle(windowTitle);
        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    public static void createAndShowGUI(ChainControl cc) {
        createAndShowGUI(cc, "Chip");
    }

    private static void createAndShowGUI(String xml) {
        JFrame mainFrame = new ChainG(xml);
        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    public void treeNodesChanged(TreeModelEvent e) {
        treeLeftSelChanged(null);
    }
    public void treeNodesInserted(TreeModelEvent e) {}
    public void treeNodesRemoved(TreeModelEvent e) {}
    public void treeStructureChanged(TreeModelEvent e) {}

    /** display a line in log area */
    public void logOut(String line) {
        if (logTextArea != null) {
            logTextArea.append(line + "\n");
        }
    }

    /** customize icons */
    private class MyRenderer extends DefaultTreeCellRenderer {
        ImageIcon icon1, icon2;

        public MyRenderer() {
            icon1 = new ImageIcon("icon/chip.gif");
            icon2 = new ImageIcon("icon/chain_root.gif");
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded,
                    leaf, row, hasFocus);
            if (value.getClass() == TestNode.class) {
                setIcon(null);
            } else if (value.getClass() == ChipNode.class) {
                setIcon(icon1);
            } else if (value.getClass() == ChainNode.class) {
                setIcon(icon2);
            }

            String state = "";
            if (value instanceof SubchainNode) {
                SubchainNode node = (SubchainNode)value;
                if (inBitsButton.isSelected()) {
                    state = node.getInBitsIndiscriminate().getState();
                } else if (outBitsButton.isSelected()) {
                    state = node.getOutBitsIndiscriminate().getState();
                } else if (outBitsExpButton.isSelected()) {
                    state = node.getOldOutBitsExpected().getState();
                }
                setText(value.toString() + " " + state);
            }

            return this;
        }
    }
}
