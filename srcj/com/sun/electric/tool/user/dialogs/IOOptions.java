/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IOOptions.java
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

import com.sun.electric.database.prototype.PortProto;

import java.util.Iterator;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.text.Document;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Class to handle the "IO Options" dialog.
 */
public class IOOptions extends javax.swing.JDialog
{

	/** The name of the current tab in this dialog. */	private static String currentTabName = null;

	/** Creates new form IOOptions */
	public IOOptions(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();

		// if the last know tab name is available, find that tab again
		if (currentTabName != null)
		{
			int numTabs = tabPane.getTabCount();
			for(int i=0; i<numTabs; i++)
			{
				String tabName = tabPane.getTitleAt(i);
				if (tabName.equals(currentTabName))
				{
					tabPane.setSelectedIndex(i);
					break;
				}
			}
		}

		// listen for changes in the current tab
        tabPane.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent evt)
            {
				currentTabName = tabPane.getTitleAt(tabPane.getSelectedIndex());
            }
        });

		initCIF();			// initialize the CIF panel
		initGDS();			// initialize the GDS panel
		initEDIF();			// initialize the EDIF panel
		initDEF();			// initialize the DEF panel
		initCDL();			// initialize the CDL panel
		initDXF();			// initialize the DXF panel
		initSUE();			// initialize the SUE panel
		initCopyright();	// initialize the Copyright panel
		initLibrary();		// initialize the Library panel
		initPrinting();		// initialize the Printing panel
	}

	//******************************** CIF ********************************

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the CIF tab.
	 */
	private void initCIF()
	{
		cifLayers.setEnabled(false);
		cifOutputMimicsDisplay.setEnabled(false);
		cifOutputMergesBoxes.setEnabled(false);
		cifOutputInstantiatesTopLevel.setEnabled(false);
		cifNormalizeCoordinates.setEnabled(false);
		cifInputSquaresWires.setEnabled(false);
		cifResolution.setEnabled(false);
		cifLayer.setEditable(false);
		cifResolutionValue.setEditable(false);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the CIF tab.
	 */
	private void termCIF()
	{
	}

	//******************************** GDS ********************************

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the GDS tab.
	 */
	private void initGDS()
	{
		gdsLayerList.setEnabled(false);
		gdsLayerName.setEditable(false);
		gdsPinLayer.setEditable(false);
		gdsTextLayer.setEditable(false);
		gdsInputIncludesText.setEnabled(false);
		gdsInputExpandsCells.setEnabled(false);
		gdsInputInstantiatesArrays.setEnabled(false);
		gdsInputIgnoresUnknownLayers.setEnabled(false);
		gdsOutputMergesBoxes.setEnabled(false);
		gdsOutputWritesExportPins.setEnabled(false);
		gdsOutputUpperCase.setEnabled(false);
		gdsDefaultTextLayer.setEditable(false);
		gdsMaxArcAngle.setEditable(false);
		gdsMaxArcSag.setEditable(false);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the GDS tab.
	 */
	private void termGDS()
	{
	}

	//******************************** EDIF ********************************

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the EDIF tab.
	 */
	private void initEDIF()
	{
		edifUseSchematicView.setEnabled(false);
		edifInputScale.setEditable(false);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the EDIF tab.
	 */
	private void termEDIF()
	{
	}

	//******************************** DEF ********************************

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the DEF tab.
	 */
	private void initDEF()
	{
		defPlacePhysical.setEnabled(false);
		defPlaceLogical.setEnabled(false);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the DEF tab.
	 */
	private void termDEF()
	{
	}

	//******************************** CDL ********************************

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the CDL tab.
	 */
	private void initCDL()
	{
		cdlLibraryName.setEditable(false);
		cdlLibraryPath.setEditable(false);
		cdlConvertBrackets.setEnabled(false);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the CDL tab.
	 */
	private void termCDL()
	{
	}

	//******************************** DXF ********************************

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the DXF tab.
	 */
	private void initDXF()
	{
		dxfLayerList.setEnabled(false);
		dxfLayerName.setEditable(false);
		dxfInputFlattensHierarchy.setEnabled(false);
		dxfInputReadsAllLayers.setEnabled(false);
		dxfScale.setEnabled(false);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the DXF tab.
	 */
	private void termDXF()
	{
	}

	//******************************** SUE ********************************

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the SUE tab.
	 */
	private void initSUE()
	{
		sueMake4PortTransistors.setEnabled(false);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the SUE tab.
	 */
	private void termSUE()
	{
	}

	//******************************** COPYRIGHT ********************************

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Copyright tab.
	 */
	private void initCopyright()
	{
		copyrightNone.setEnabled(false);
		copyrightUse.setEnabled(false);
		copyrightFileName.setEditable(false);
		copyrightBrowse.setEnabled(false);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Copyright tab.
	 */
	private void termCopyright()
	{
	}

	//******************************** LIBRARY ********************************

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Library tab.
	 */
	private void initLibrary()
	{
		libNoBackup.setEnabled(false);
		libBackupLast.setEnabled(false);
		libBackupHistory.setEnabled(false);
		libCheckDatabase.setEnabled(false);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Library tab.
	 */
	private void termLibrary()
	{
	}

	//******************************** PRINTING ********************************

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Printing tab.
	 */
	private void initPrinting()
	{
		printPlotEntireCell.setEnabled(false);
		printPlotHighlightedArea.setEnabled(false);
		printPlotDisplayedWindow.setEnabled(false);
		printPlotDateInCorner.setEnabled(false);
		printDefaultPrinter.setEnabled(false);
		printResolution.setEditable(false);
		printEncapsulated.setEnabled(false);
		printPostScriptStyle.setEnabled(false);
		printUsePrinter.setEnabled(false);
		printUsePlotter.setEnabled(false);
		printWidth.setEditable(false);
		printHeight.setEditable(false);
		printMargin.setEditable(false);
		printRotation.setEnabled(false);
		printEPSScale.setEditable(false);
		printSynchronizeToFile.setEnabled(false);
		printHPGL1.setEnabled(false);
		printHPGL2.setEnabled(false);
		printHPGLFillsPage.setEnabled(false);
		printHPGLFixedScale.setEnabled(false);
		printHPGLScale.setEditable(false);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Printing tab.
	 */
	private void termPrinting()
	{
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        libraryGroup = new javax.swing.ButtonGroup();
        copyrightGroup = new javax.swing.ButtonGroup();
        printingPlotArea = new javax.swing.ButtonGroup();
        printingPlotOrPrint = new javax.swing.ButtonGroup();
        printingHPGL = new javax.swing.ButtonGroup();
        printingHPGLScale = new javax.swing.ButtonGroup();
        tabPane = new javax.swing.JTabbedPane();
        cif = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        cifLayers = new javax.swing.JScrollPane();
        cifOutputMimicsDisplay = new javax.swing.JCheckBox();
        cifOutputMergesBoxes = new javax.swing.JCheckBox();
        cifOutputInstantiatesTopLevel = new javax.swing.JCheckBox();
        cifNormalizeCoordinates = new javax.swing.JCheckBox();
        cifInputSquaresWires = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        cifResolution = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        cifLayer = new javax.swing.JTextField();
        cifResolutionValue = new javax.swing.JTextField();
        gds = new javax.swing.JPanel();
        gdsLayerList = new javax.swing.JScrollPane();
        jLabel6 = new javax.swing.JLabel();
        gdsLayerName = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        gdsPinLayer = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        gdsTextLayer = new javax.swing.JTextField();
        gdsInputIncludesText = new javax.swing.JCheckBox();
        gdsInputExpandsCells = new javax.swing.JCheckBox();
        gdsInputInstantiatesArrays = new javax.swing.JCheckBox();
        gdsInputIgnoresUnknownLayers = new javax.swing.JCheckBox();
        gdsOutputMergesBoxes = new javax.swing.JCheckBox();
        gdsOutputWritesExportPins = new javax.swing.JCheckBox();
        gdsOutputUpperCase = new javax.swing.JCheckBox();
        jLabel9 = new javax.swing.JLabel();
        gdsDefaultTextLayer = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        gdsMaxArcAngle = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        gdsMaxArcSag = new javax.swing.JTextField();
        edif = new javax.swing.JPanel();
        edifUseSchematicView = new javax.swing.JCheckBox();
        jLabel13 = new javax.swing.JLabel();
        edifInputScale = new javax.swing.JTextField();
        def = new javax.swing.JPanel();
        defPlacePhysical = new javax.swing.JCheckBox();
        defPlaceLogical = new javax.swing.JCheckBox();
        cdl = new javax.swing.JPanel();
        jLabel14 = new javax.swing.JLabel();
        cdlLibraryName = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        cdlLibraryPath = new javax.swing.JTextField();
        cdlConvertBrackets = new javax.swing.JCheckBox();
        dxf = new javax.swing.JPanel();
        dxfLayerList = new javax.swing.JScrollPane();
        jLabel16 = new javax.swing.JLabel();
        dxfLayerName = new javax.swing.JTextField();
        dxfInputFlattensHierarchy = new javax.swing.JCheckBox();
        dxfInputReadsAllLayers = new javax.swing.JCheckBox();
        jLabel17 = new javax.swing.JLabel();
        dxfScale = new javax.swing.JComboBox();
        sue = new javax.swing.JPanel();
        sueMake4PortTransistors = new javax.swing.JCheckBox();
        copyright = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        copyrightNone = new javax.swing.JRadioButton();
        copyrightUse = new javax.swing.JRadioButton();
        copyrightFileName = new javax.swing.JTextField();
        copyrightBrowse = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        library = new javax.swing.JPanel();
        libNoBackup = new javax.swing.JRadioButton();
        libBackupLast = new javax.swing.JRadioButton();
        libBackupHistory = new javax.swing.JRadioButton();
        libCheckDatabase = new javax.swing.JCheckBox();
        printing = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        printPlotEntireCell = new javax.swing.JRadioButton();
        printPlotDateInCorner = new javax.swing.JCheckBox();
        printPlotHighlightedArea = new javax.swing.JRadioButton();
        jLabel18 = new javax.swing.JLabel();
        printPlotDisplayedWindow = new javax.swing.JRadioButton();
        printDefaultPrinter = new javax.swing.JComboBox();
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
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        printEPSScale = new javax.swing.JTextField();
        printSynchronizeToFile = new javax.swing.JCheckBox();
        jLabel20 = new javax.swing.JLabel();
        ok = new javax.swing.JButton();
        Cancel = new javax.swing.JButton();

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

        cif.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("CIF Layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        cif.add(jLabel1, gridBagConstraints);

        cifLayers.setPreferredSize(new java.awt.Dimension(200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(cifLayers, gridBagConstraints);

        cifOutputMimicsDisplay.setText("Output Mimics Display");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        cif.add(cifOutputMimicsDisplay, gridBagConstraints);

        cifOutputMergesBoxes.setText("Output Merges Boxes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        cif.add(cifOutputMergesBoxes, gridBagConstraints);

        cifOutputInstantiatesTopLevel.setText("Output Instantiates Top Level");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        cif.add(cifOutputInstantiatesTopLevel, gridBagConstraints);

        cifNormalizeCoordinates.setText("Normalize Coordinates");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        cif.add(cifNormalizeCoordinates, gridBagConstraints);

        cifInputSquaresWires.setText("Input Squares Wires");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        cif.add(cifInputSquaresWires, gridBagConstraints);

        jLabel2.setText("(time consuming)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        cif.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(cifResolution, gridBagConstraints);

        jLabel3.setText("Output resolution:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        cif.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(cifLayer, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(cifResolutionValue, gridBagConstraints);

        tabPane.addTab("CIF", cif);

        gds.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 14;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsLayerList, gridBagConstraints);

        jLabel6.setText("GDS Layer(s):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gds.add(jLabel6, gridBagConstraints);

        gdsLayerName.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsLayerName, gridBagConstraints);

        jLabel7.setText("Pin layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        gds.add(jLabel7, gridBagConstraints);

        gdsPinLayer.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsPinLayer, gridBagConstraints);

        jLabel8.setText("Text layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        gds.add(jLabel8, gridBagConstraints);

        gdsTextLayer.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsTextLayer, gridBagConstraints);

        gdsInputIncludesText.setText("Input includes Text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        gds.add(gdsInputIncludesText, gridBagConstraints);

        gdsInputExpandsCells.setText("Input expands cells");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsInputExpandsCells, gridBagConstraints);

        gdsInputInstantiatesArrays.setText("Input instantiates Arrays");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsInputInstantiatesArrays, gridBagConstraints);

        gdsInputIgnoresUnknownLayers.setText("Input ignores unknown layers");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsInputIgnoresUnknownLayers, gridBagConstraints);

        gdsOutputMergesBoxes.setText("Output merges Boxes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsOutputMergesBoxes, gridBagConstraints);

        gdsOutputWritesExportPins.setText("Output writes export Pins");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsOutputWritesExportPins, gridBagConstraints);

        gdsOutputUpperCase.setText("Output all upper-case");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        gds.add(gdsOutputUpperCase, gridBagConstraints);

        jLabel9.setText("Output default text layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gds.add(jLabel9, gridBagConstraints);

        gdsDefaultTextLayer.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsDefaultTextLayer, gridBagConstraints);

        jLabel10.setText("Output arc conversion:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        gds.add(jLabel10, gridBagConstraints);

        jLabel11.setText("Maximum arc angle:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        gds.add(jLabel11, gridBagConstraints);

        gdsMaxArcAngle.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsMaxArcAngle, gridBagConstraints);

        jLabel12.setText("Maximum arc sag:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        gds.add(jLabel12, gridBagConstraints);

        gdsMaxArcSag.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsMaxArcSag, gridBagConstraints);

        tabPane.addTab("GDS", gds);

        edif.setLayout(new java.awt.GridBagLayout());

        edifUseSchematicView.setText("Use Schematic View");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        edif.add(edifUseSchematicView, gridBagConstraints);

        jLabel13.setText("Input scale:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        edif.add(jLabel13, gridBagConstraints);

        edifInputScale.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        edif.add(edifInputScale, gridBagConstraints);

        tabPane.addTab("EDIF", edif);

        def.setLayout(new java.awt.GridBagLayout());

        defPlacePhysical.setText("Place physical interconnect");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        def.add(defPlacePhysical, gridBagConstraints);

        defPlaceLogical.setText("Place logical interconnect");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        def.add(defPlaceLogical, gridBagConstraints);

        tabPane.addTab("DEF", def);

        cdl.setLayout(new java.awt.GridBagLayout());

        jLabel14.setText("Cadence Library Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cdl.add(jLabel14, gridBagConstraints);

        cdlLibraryName.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cdl.add(cdlLibraryName, gridBagConstraints);

        jLabel15.setText("Cadence Library Path:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cdl.add(jLabel15, gridBagConstraints);

        cdlLibraryPath.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cdl.add(cdlLibraryPath, gridBagConstraints);

        cdlConvertBrackets.setText("Convert brackets");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cdl.add(cdlConvertBrackets, gridBagConstraints);

        tabPane.addTab("CDL", cdl);

        dxf.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        dxf.add(dxfLayerList, gridBagConstraints);

        jLabel16.setText("DXF Layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        dxf.add(jLabel16, gridBagConstraints);

        dxfLayerName.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        dxf.add(dxfLayerName, gridBagConstraints);

        dxfInputFlattensHierarchy.setText("Input flattens hierarchy");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        dxf.add(dxfInputFlattensHierarchy, gridBagConstraints);

        dxfInputReadsAllLayers.setText("Input reads all layers");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        dxf.add(dxfInputReadsAllLayers, gridBagConstraints);

        jLabel17.setText("DXF Scale:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        dxf.add(jLabel17, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        dxf.add(dxfScale, gridBagConstraints);

        tabPane.addTab("DXF", dxf);

        sue.setLayout(new java.awt.GridBagLayout());

        sueMake4PortTransistors.setText("Make 4-port transistors");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        sue.add(sueMake4PortTransistors, gridBagConstraints);

        tabPane.addTab("SUE", sue);

        copyright.setLayout(new java.awt.GridBagLayout());

        jLabel4.setText("Copyright information can be added to every generated deck");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 20, 4);
        copyright.add(jLabel4, gridBagConstraints);

        copyrightNone.setText("No copyright message");
        copyrightGroup.add(copyrightNone);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        copyright.add(copyrightNone, gridBagConstraints);

        copyrightUse.setText("Use copyright message from file:");
        copyrightGroup.add(copyrightUse);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        copyright.add(copyrightUse, gridBagConstraints);

        copyrightFileName.setColumns(20);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        copyright.add(copyrightFileName, gridBagConstraints);

        copyrightBrowse.setText("Browse");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        copyright.add(copyrightBrowse, gridBagConstraints);

        jLabel5.setText("Do not put comment characters in this file");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        copyright.add(jLabel5, gridBagConstraints);

        tabPane.addTab("Copyright", copyright);

        library.setLayout(new java.awt.GridBagLayout());

        libNoBackup.setText("No backup of library files");
        libraryGroup.add(libNoBackup);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        library.add(libNoBackup, gridBagConstraints);

        libBackupLast.setText("Backup of last library file");
        libraryGroup.add(libBackupLast);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        library.add(libBackupLast, gridBagConstraints);

        libBackupHistory.setText("Backup history of library files");
        libraryGroup.add(libBackupHistory);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        library.add(libBackupHistory, gridBagConstraints);

        libCheckDatabase.setText("Check database after write");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(20, 4, 4, 4);
        library.add(libCheckDatabase, gridBagConstraints);

        tabPane.addTab("Library", library);

        printing.setLayout(new java.awt.GridBagLayout());

        jPanel4.setLayout(new java.awt.GridBagLayout());

        jPanel4.setBorder(new javax.swing.border.TitledBorder("For all printing"));
        printPlotEntireCell.setText("Plot Entire Cell");
        printingPlotArea.add(printPlotEntireCell);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel4.add(printPlotEntireCell, gridBagConstraints);

        printPlotDateInCorner.setText("Plot Date In Corner");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel4.add(printPlotDateInCorner, gridBagConstraints);

        printPlotHighlightedArea.setText("Plot only Highlighted Area");
        printingPlotArea.add(printPlotHighlightedArea);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel4.add(printPlotHighlightedArea, gridBagConstraints);

        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel18.setText("Default printer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel4.add(jLabel18, gridBagConstraints);

        printPlotDisplayedWindow.setText("Plot only Displayed Window");
        printingPlotArea.add(printPlotDisplayedWindow);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel4.add(printPlotDisplayedWindow, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel4.add(printDefaultPrinter, gridBagConstraints);

        jLabel19.setText("Print and Copy resolution factor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel4.add(jLabel19, gridBagConstraints);

        printResolution.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
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
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel5.add(jLabel26, gridBagConstraints);

        printHPGL1.setText("HPGL");
        printingHPGL.add(printHPGL1);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel5.add(printHPGL1, gridBagConstraints);

        printHPGL2.setText("HPGL/2");
        printingHPGL.add(printHPGL2);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel5.add(printHPGL2, gridBagConstraints);

        printHPGLFillsPage.setText("HPGL/2 plot fills page");
        printingHPGLScale.add(printHPGLFillsPage);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel5.add(printHPGLFillsPage, gridBagConstraints);

        printHPGLFixedScale.setText("HPGL/2 plot fixed at:");
        printingHPGLScale.add(printHPGLFixedScale);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel5.add(printHPGLFixedScale, gridBagConstraints);

        printHPGLScale.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel5.add(printHPGLScale, gridBagConstraints);

        jLabel27.setText("grid units per pixel");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
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
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(printEncapsulated, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(printPostScriptStyle, gridBagConstraints);

        printUsePrinter.setText("Printer");
        printingPlotOrPrint.add(printUsePrinter);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
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
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(jLabel21, gridBagConstraints);

        printWidth.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
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
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
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
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(printMargin, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(printRotation, gridBagConstraints);

        jLabel24.setText("For cell:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(jLabel24, gridBagConstraints);

        jLabel25.setText("EPS Scale:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(jLabel25, gridBagConstraints);

        printEPSScale.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(printEPSScale, gridBagConstraints);

        printSynchronizeToFile.setText("Synchronize to file:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(printSynchronizeToFile, gridBagConstraints);

        jLabel20.setText("Rotation:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(jLabel20, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        printing.add(jPanel6, gridBagConstraints);

        tabPane.addTab("Printing", printing);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(tabPane, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                okActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 40);
        getContentPane().add(ok, gridBagConstraints);

        Cancel.setText("Cancel");
        Cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                CancelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 40, 4, 4);
        getContentPane().add(Cancel, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void okActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okActionPerformed
	{//GEN-HEADEREND:event_okActionPerformed
		termCIF();			// terminate the CIF panel
		termGDS();			// terminate the GDS panel
		termEDIF();			// terminate the EDIF panel
		termDEF();			// terminate the DEF panel
		termCDL();			// terminate the CDL panel
		termDXF();			// terminate the DXF panel
		termSUE();			// terminate the SUE panel
		termCopyright();	// terminate the Copyright panel
		termLibrary();		// terminate the Library panel
		termPrinting();		// terminate the Printing panel

		closeDialog(null);
	}//GEN-LAST:event_okActionPerformed

	private void CancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_CancelActionPerformed
	{//GEN-HEADEREND:event_CancelActionPerformed
		closeDialog(null);
	}//GEN-LAST:event_CancelActionPerformed
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Cancel;
    private javax.swing.JPanel cdl;
    private javax.swing.JCheckBox cdlConvertBrackets;
    private javax.swing.JTextField cdlLibraryName;
    private javax.swing.JTextField cdlLibraryPath;
    private javax.swing.JPanel cif;
    private javax.swing.JCheckBox cifInputSquaresWires;
    private javax.swing.JTextField cifLayer;
    private javax.swing.JScrollPane cifLayers;
    private javax.swing.JCheckBox cifNormalizeCoordinates;
    private javax.swing.JCheckBox cifOutputInstantiatesTopLevel;
    private javax.swing.JCheckBox cifOutputMergesBoxes;
    private javax.swing.JCheckBox cifOutputMimicsDisplay;
    private javax.swing.JComboBox cifResolution;
    private javax.swing.JTextField cifResolutionValue;
    private javax.swing.JPanel copyright;
    private javax.swing.JButton copyrightBrowse;
    private javax.swing.JTextField copyrightFileName;
    private javax.swing.ButtonGroup copyrightGroup;
    private javax.swing.JRadioButton copyrightNone;
    private javax.swing.JRadioButton copyrightUse;
    private javax.swing.JPanel def;
    private javax.swing.JCheckBox defPlaceLogical;
    private javax.swing.JCheckBox defPlacePhysical;
    private javax.swing.JPanel dxf;
    private javax.swing.JCheckBox dxfInputFlattensHierarchy;
    private javax.swing.JCheckBox dxfInputReadsAllLayers;
    private javax.swing.JScrollPane dxfLayerList;
    private javax.swing.JTextField dxfLayerName;
    private javax.swing.JComboBox dxfScale;
    private javax.swing.JPanel edif;
    private javax.swing.JTextField edifInputScale;
    private javax.swing.JCheckBox edifUseSchematicView;
    private javax.swing.JPanel gds;
    private javax.swing.JTextField gdsDefaultTextLayer;
    private javax.swing.JCheckBox gdsInputExpandsCells;
    private javax.swing.JCheckBox gdsInputIgnoresUnknownLayers;
    private javax.swing.JCheckBox gdsInputIncludesText;
    private javax.swing.JCheckBox gdsInputInstantiatesArrays;
    private javax.swing.JScrollPane gdsLayerList;
    private javax.swing.JTextField gdsLayerName;
    private javax.swing.JTextField gdsMaxArcAngle;
    private javax.swing.JTextField gdsMaxArcSag;
    private javax.swing.JCheckBox gdsOutputMergesBoxes;
    private javax.swing.JCheckBox gdsOutputUpperCase;
    private javax.swing.JCheckBox gdsOutputWritesExportPins;
    private javax.swing.JTextField gdsPinLayer;
    private javax.swing.JTextField gdsTextLayer;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JRadioButton libBackupHistory;
    private javax.swing.JRadioButton libBackupLast;
    private javax.swing.JCheckBox libCheckDatabase;
    private javax.swing.JRadioButton libNoBackup;
    private javax.swing.JPanel library;
    private javax.swing.ButtonGroup libraryGroup;
    private javax.swing.JButton ok;
    private javax.swing.JComboBox printDefaultPrinter;
    private javax.swing.JTextField printEPSScale;
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
    private javax.swing.JCheckBox printSynchronizeToFile;
    private javax.swing.JRadioButton printUsePlotter;
    private javax.swing.JRadioButton printUsePrinter;
    private javax.swing.JTextField printWidth;
    private javax.swing.JPanel printing;
    private javax.swing.ButtonGroup printingHPGL;
    private javax.swing.ButtonGroup printingHPGLScale;
    private javax.swing.ButtonGroup printingPlotArea;
    private javax.swing.ButtonGroup printingPlotOrPrint;
    private javax.swing.JPanel sue;
    private javax.swing.JCheckBox sueMake4PortTransistors;
    private javax.swing.JTabbedPane tabPane;
    // End of variables declaration//GEN-END:variables
	
}
