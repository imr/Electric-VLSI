/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PrintingTab.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

/**
 * Class to handle the "Printing" tab of the Preferences dialog.
 */
public class PrintingTab extends PreferencePanel
{
	/** Creates new form PrintingTab */
	public PrintingTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return printing; }

	public String getName() { return "Printing"; }

	private int initialPrintArea;
	private int initialPrintResolution;
	private boolean initialPrintDate;
	private boolean initialPrintEncapsulated;
	private boolean initialPrintPlotter;
	private double initialPrintWidth, initialPrintHeight, initialPrintMargin;
	private int initialPrintRotation;
	private int initialPrintColorMethod;
	private Cell initialCell;
	private double initialEPSScale;
	private String initialEPSSyncFile;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Printing tab.
	 */
	public void init()
	{
		initialPrintArea = IOTool.getPlotArea();
		switch (initialPrintArea)
		{
			case 0: printPlotEntireCell.setSelected(true);        break;
			case 1: printPlotHighlightedArea.setSelected(true);   break;
			case 2: printPlotDisplayedWindow.setSelected(true);   break;
		}

		initialPrintDate = IOTool.isPlotDate();
		printPlotDateInCorner.setSelected(initialPrintDate);

		initialPrintEncapsulated = IOTool.isPrintEncapsulated();
		printEncapsulated.setSelected(initialPrintEncapsulated);

		initialPrintResolution = IOTool.getPrintResolution();
		printResolution.setText(Integer.toString(initialPrintResolution));

		initialPrintPlotter = IOTool.isPrintForPlotter();
		if (initialPrintPlotter) printUsePlotter.setSelected(true); else
			printUsePrinter.setSelected(true);

		initialPrintWidth = IOTool.getPrintWidth();
		printWidth.setText(TextUtils.formatDouble(initialPrintWidth));
		initialPrintHeight = IOTool.getPrintHeight();
		printHeight.setText(TextUtils.formatDouble(initialPrintHeight));
		initialPrintMargin = IOTool.getPrintMargin();
		printMargin.setText(TextUtils.formatDouble(initialPrintMargin));

		printRotation.addItem("No Rotation");
		printRotation.addItem("Rotate plot 90 degrees");
		printRotation.addItem("Auto-rotate plot to fit");
		initialPrintRotation = IOTool.getPrintRotation();
		printRotation.setSelectedIndex(initialPrintRotation);

		printPostScriptStyle.addItem("Black&White");
		printPostScriptStyle.addItem("Color");
		printPostScriptStyle.addItem("Color Stippled");
		printPostScriptStyle.addItem("Color Merged");
		initialPrintColorMethod = IOTool.getPrintColorMethod();
		printPostScriptStyle.setSelectedIndex(initialPrintColorMethod);

		initialCell = WindowFrame.getCurrentCell();
		initialEPSScale = 1;
		initialEPSSyncFile = "";
		if (initialCell != null)
		{
			printCellName.setText("For cell: " + initialCell.describe());
			initialEPSScale = IOTool.getPrintEPSScale(initialCell);
			initialEPSSyncFile = IOTool.getPrintEPSSynchronizeFile(initialCell);
			printSyncFileName.setText(initialEPSSyncFile);
			printSetEPSSync.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { printSetEPSSyncActionPerformed(); }
			});
		} else
		{
			printCellName.setEnabled(false);
			printEPSScaleLabel.setEnabled(false);
			printEPSScale.setEditable(false);
			printSynchLabel.setEnabled(false);
			printSyncFileName.setEditable(false);
			printSetEPSSync.setEnabled(false);
		}
		printEPSScale.setText(TextUtils.formatDouble(initialEPSScale));

		// not yet:
		printHPGL1.setEnabled(false);
		printHPGL2.setEnabled(false);
		printHPGLFillsPage.setEnabled(false);
		printHPGLFixedScale.setEnabled(false);
		printHPGLScale.setEditable(false);
	}

	private void printSetEPSSyncActionPerformed()
	{
		String defaultFileName = initialCell.getName() + ".eps";
		String fileName = OpenFile.chooseOutputFile(FileType.POSTSCRIPT, "Choose EPS file", defaultFileName);
		if (fileName == null) return;
		printSyncFileName.setText(fileName);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Printing tab.
	 */
	public void term()
	{
		int printArea = 0;
		if (printPlotHighlightedArea.isSelected()) printArea = 1; else
			if (printPlotDisplayedWindow.isSelected()) printArea = 2;
		if (printArea != initialPrintArea)
			IOTool.setPlotArea(printArea);

		boolean plotDate = printPlotDateInCorner.isSelected();
		if (plotDate != initialPrintDate)
			IOTool.setPlotDate(plotDate);

		boolean encapsulated = printEncapsulated.isSelected();
		if (encapsulated != initialPrintEncapsulated)
			IOTool.setPrintEncapsulated(encapsulated);

		int resolution = TextUtils.atoi(printResolution.getText());
		if (resolution != initialPrintResolution)
			IOTool.setPrintResolution(resolution);

		boolean plotter = printUsePlotter.isSelected();
		if (plotter != initialPrintPlotter)
			IOTool.setPrintForPlotter(plotter);

		double width = TextUtils.atof(printWidth.getText());
		if (width != initialPrintWidth)
			IOTool.setPrintWidth(width);

		double height = TextUtils.atof(printHeight.getText());
		if (height != initialPrintHeight)
			IOTool.setPrintHeight(height);

		double margin = TextUtils.atof(printMargin.getText());
		if (margin != initialPrintMargin)
			IOTool.setPrintMargin(margin);

		int rotation = printRotation.getSelectedIndex();
		if (rotation != initialPrintRotation)
			IOTool.setPrintRotation(rotation);

		int colorMethod = printPostScriptStyle.getSelectedIndex();
		if (colorMethod != initialPrintColorMethod)
			IOTool.setPrintColorMethod(colorMethod);

		if (initialCell != null)
		{
			double currentEPSScale = TextUtils.atof(printEPSScale.getText());
			if (currentEPSScale != initialEPSScale && currentEPSScale != 0)
				IOTool.setPrintEPSScale(initialCell, currentEPSScale);
			String currentEPSSyncFile = printSyncFileName.getText();
			if (!currentEPSSyncFile.equals(initialEPSSyncFile))
				IOTool.setPrintEPSSynchronizeFile(initialCell, currentEPSSyncFile);
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

        printingPlotArea = new javax.swing.ButtonGroup();
        printingPlotOrPrint = new javax.swing.ButtonGroup();
        printingHPGL = new javax.swing.ButtonGroup();
        printingHPGLScale = new javax.swing.ButtonGroup();
        printing = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        printPlotEntireCell = new javax.swing.JRadioButton();
        printPlotDateInCorner = new javax.swing.JCheckBox();
        printPlotHighlightedArea = new javax.swing.JRadioButton();
        printPlotDisplayedWindow = new javax.swing.JRadioButton();
        jLabel19 = new javax.swing.JLabel();
        printResolution = new javax.swing.JTextField();
        jPanel5 = new javax.swing.JPanel();
        jLabel26 = new javax.swing.JLabel();
        printHPGL1 = new javax.swing.JRadioButton();
        printHPGL2 = new javax.swing.JRadioButton();
        printHPGLFillsPage = new javax.swing.JRadioButton();
        printHPGLFixedScale = new javax.swing.JRadioButton();
        printHPGLScale = new javax.swing.JTextField();
        jLabel27 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        printEncapsulated = new javax.swing.JCheckBox();
        printPostScriptStyle = new javax.swing.JComboBox();
        printUsePrinter = new javax.swing.JRadioButton();
        printUsePlotter = new javax.swing.JRadioButton();
        jLabel21 = new javax.swing.JLabel();
        printWidth = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        printHeight = new javax.swing.JTextField();
        jLabel23 = new javax.swing.JLabel();
        printMargin = new javax.swing.JTextField();
        printRotation = new javax.swing.JComboBox();
        printCellName = new javax.swing.JLabel();
        printEPSScaleLabel = new javax.swing.JLabel();
        printEPSScale = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        printSyncFileName = new javax.swing.JTextField();
        printSynchLabel = new javax.swing.JLabel();
        printSetEPSSync = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("IO Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        printing.setLayout(new java.awt.GridBagLayout());

        jPanel4.setLayout(new java.awt.GridBagLayout());

        jPanel4.setBorder(new javax.swing.border.TitledBorder("For all printing"));
        printPlotEntireCell.setText("Plot Entire Cell");
        printingPlotArea.add(printPlotEntireCell);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel4.add(printPlotEntireCell, gridBagConstraints);

        printPlotDateInCorner.setText("Plot Date In Corner");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel4.add(printPlotDateInCorner, gridBagConstraints);

        printPlotHighlightedArea.setText("Plot only Highlighted Area");
        printingPlotArea.add(printPlotHighlightedArea);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel4.add(printPlotHighlightedArea, gridBagConstraints);

        printPlotDisplayedWindow.setText("Plot only Displayed Window");
        printingPlotArea.add(printPlotDisplayedWindow);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel4.add(printPlotDisplayedWindow, gridBagConstraints);

        jLabel19.setText("Print resolution (DPI):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel4.add(jLabel19, gridBagConstraints);

        printResolution.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel4.add(printResolution, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        printing.add(jPanel4, gridBagConstraints);

        jPanel5.setLayout(new java.awt.GridBagLayout());

        jPanel5.setBorder(new javax.swing.border.TitledBorder("For HPGL printing"));
        jLabel26.setText("HPGL Level:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel5.add(jLabel26, gridBagConstraints);

        printHPGL1.setText("HPGL");
        printingHPGL.add(printHPGL1);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel5.add(printHPGL1, gridBagConstraints);

        printHPGL2.setText("HPGL/2");
        printingHPGL.add(printHPGL2);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel5.add(printHPGL2, gridBagConstraints);

        printHPGLFillsPage.setText("HPGL/2 plot fills page");
        printingHPGLScale.add(printHPGLFillsPage);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel5.add(printHPGLFillsPage, gridBagConstraints);

        printHPGLFixedScale.setText("HPGL/2 plot fixed at:");
        printingHPGLScale.add(printHPGLFixedScale);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel5.add(printHPGLFixedScale, gridBagConstraints);

        printHPGLScale.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel5.add(printHPGLScale, gridBagConstraints);

        jLabel27.setText("grid units per pixel");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel5.add(jLabel27, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        printing.add(jPanel5, gridBagConstraints);

        jPanel6.setLayout(new java.awt.GridBagLayout());

        jPanel6.setBorder(new javax.swing.border.TitledBorder("For PostScript printing"));
        printEncapsulated.setText("Encapsulated");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel6.add(printEncapsulated, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel6.add(printPostScriptStyle, gridBagConstraints);

        printUsePrinter.setText("Printer");
        printingPlotOrPrint.add(printUsePrinter);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel6.add(printUsePrinter, gridBagConstraints);

        printUsePlotter.setText("Plotter");
        printingPlotOrPrint.add(printUsePlotter);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(printUsePlotter, gridBagConstraints);

        jLabel21.setText("Width (in):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel6.add(jLabel21, gridBagConstraints);

        printWidth.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel6.add(printWidth, gridBagConstraints);

        jLabel22.setText("Height (in):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(jLabel22, gridBagConstraints);

        printHeight.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel6.add(printHeight, gridBagConstraints);

        jLabel23.setText("Margin (in):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(jLabel23, gridBagConstraints);

        printMargin.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel6.add(printMargin, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel6.add(printRotation, gridBagConstraints);

        printCellName.setText("For cell:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel6.add(printCellName, gridBagConstraints);

        printEPSScaleLabel.setText("EPS Scale:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        jPanel6.add(printEPSScaleLabel, gridBagConstraints);

        printEPSScale.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel6.add(printEPSScale, gridBagConstraints);

        jLabel20.setText("Rotation:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel6.add(jLabel20, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel6.add(jSeparator1, gridBagConstraints);

        printSyncFileName.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel6.add(printSyncFileName, gridBagConstraints);

        printSynchLabel.setText("Synchronize to file:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        jPanel6.add(printSynchLabel, gridBagConstraints);

        printSetEPSSync.setText("Set");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 6;
        jPanel6.add(printSetEPSSync, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        printing.add(jPanel6, gridBagConstraints);

        getContentPane().add(printing, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel printCellName;
    private javax.swing.JTextField printEPSScale;
    private javax.swing.JLabel printEPSScaleLabel;
    private javax.swing.JCheckBox printEncapsulated;
    private javax.swing.JRadioButton printHPGL1;
    private javax.swing.JRadioButton printHPGL2;
    private javax.swing.JRadioButton printHPGLFillsPage;
    private javax.swing.JRadioButton printHPGLFixedScale;
    private javax.swing.JTextField printHPGLScale;
    private javax.swing.JTextField printHeight;
    private javax.swing.JTextField printMargin;
    private javax.swing.JCheckBox printPlotDateInCorner;
    private javax.swing.JRadioButton printPlotDisplayedWindow;
    private javax.swing.JRadioButton printPlotEntireCell;
    private javax.swing.JRadioButton printPlotHighlightedArea;
    private javax.swing.JComboBox printPostScriptStyle;
    private javax.swing.JTextField printResolution;
    private javax.swing.JComboBox printRotation;
    private javax.swing.JButton printSetEPSSync;
    private javax.swing.JTextField printSyncFileName;
    private javax.swing.JLabel printSynchLabel;
    private javax.swing.JRadioButton printUsePlotter;
    private javax.swing.JRadioButton printUsePrinter;
    private javax.swing.JTextField printWidth;
    private javax.swing.JPanel printing;
    private javax.swing.ButtonGroup printingHPGL;
    private javax.swing.ButtonGroup printingHPGLScale;
    private javax.swing.ButtonGroup printingPlotArea;
    private javax.swing.ButtonGroup printingPlotOrPrint;
    // End of variables declaration//GEN-END:variables
	
}
