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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.output.GDS;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.HashMap;
import javax.print.PrintServiceLookup;
import javax.print.PrintService;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
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
		initUnits();		// initialize the Units panel
		initCopyright();	// initialize the Copyright panel
		initLibrary();		// initialize the Library panel
		initPrinting();		// initialize the Printing panel
	}

	//******************************** CIF ********************************

	private boolean initialCIFOutputMimicsDisplay;
	private boolean initialCIFOutputMergesPolygons;
	private boolean initialCIFOutputInstantiatesTopLevel;
	private boolean initialCIFOutputCheckResolution;
	private double initialCIFOutputResolution;
	private JList cifLayersList;
	private DefaultListModel cifLayersModel;
	private boolean changingCIF = false;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the CIF tab.
	 */
	private void initCIF()
	{
		initialCIFOutputMimicsDisplay = IOTool.isCIFOutMimicsDisplay();
		cifOutputMimicsDisplay.setSelected(initialCIFOutputMimicsDisplay);

		initialCIFOutputMergesPolygons = IOTool.isCIFOutMergesBoxes();
		cifOutputMergesBoxes.setSelected(initialCIFOutputMergesPolygons);

		initialCIFOutputInstantiatesTopLevel = IOTool.isCIFOutInstantiatesTopLevel();
		cifOutputInstantiatesTopLevel.setSelected(initialCIFOutputInstantiatesTopLevel);

		initialCIFOutputCheckResolution = IOTool.isCIFOutCheckResolution();
		cifCheckResolution.setSelected(initialCIFOutputCheckResolution);

		initialCIFOutputResolution = IOTool.getCIFOutResolution();
		cifResolutionValue.setText(Double.toString(initialCIFOutputResolution));

		// build the layers list
		Technology tech = Technology.getCurrent();
		cifTechnology.setText("Technology " + tech.getTechName() + ":");
		cifLayersModel = new DefaultListModel();
		cifLayersList = new JList(cifLayersModel);
		cifLayersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		cifLayers.setViewportView(cifLayersList);
		cifLayersList.clearSelection();
		cifLayersList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { cifClickLayer(); }
		});
		cifLayersModel.clear();
		for(Iterator it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			String str = layer.getName();
			String cifLayer = layer.getCIFLayer();
			if (cifLayer == null) cifLayer = "";
			if (cifLayer.length() > 0) str += " (" + cifLayer + ")";
			cifLayersModel.addElement(str);
		}
		cifLayersList.setSelectedIndex(0);
		cifLayer.getDocument().addDocumentListener(new CIFDocumentListener(this));
		cifClickLayer();

		// not yet
		cifInputSquaresWires.setEnabled(false);
	}

	/**
	 * Method called when the user clicks on a layer name in the scrollable list.
	 */
	private void cifClickLayer()
	{
		changingCIF = true;
		String str = (String)cifLayersList.getSelectedValue();
		cifLayer.setText(cifGetLayerName(str));
		changingCIF = false;
	}

	/**
	 * Method to parse the line in the scrollable list and return the CIF layer name part
	 * (in parentheses).
	 */
	private String cifGetLayerName(String str)
	{
		int openParen = str.indexOf('(');
		if (openParen < 0) return "";
		int closeParen = str.lastIndexOf(')');
		if (closeParen < 0) return "";
		String cifLayer = str.substring(openParen+1, closeParen);
		return cifLayer;
	}

	/**
	 * Method to parse the line in the scrollable list and return the Layer.
	 */
	private Layer cifGetLayer(String str)
	{
		int openParen = str.indexOf('(');
		if (openParen < 0) openParen = str.length()+1;
		String layerName = str.substring(0, openParen-1);
		Layer layer = Technology.getCurrent().findLayer(layerName);
		return layer;
	}

	/**
	 * Method called when the user types a new layer name into the edit field.
	 */
	private void cifLayerChanged()
	{
		if (changingCIF) return;
		String str = (String)cifLayersList.getSelectedValue();
		Layer layer = cifGetLayer(str);
		if (layer == null) return;
		String newLine = layer.getName();
		String newLayer = cifLayer.getText().trim();
		if (newLayer.length() > 0) newLine += " (" + newLayer + ")";
		int index = cifLayersList.getSelectedIndex();
		cifLayersModel.set(index, newLine);
	}

	/**
	 * Class to handle special changes to changes to a CIF layer.
	 */
	private static class CIFDocumentListener implements DocumentListener
	{
		IOOptions dialog;

		CIFDocumentListener(IOOptions dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.cifLayerChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.cifLayerChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.cifLayerChanged(); }
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the CIF tab.
	 */
	private void termCIF()
	{
		for(int i=0; i<cifLayersModel.getSize(); i++)
		{
			String str = (String)cifLayersModel.getElementAt(i);
			Layer layer = cifGetLayer(str);
			if (layer == null) continue;

			String currentCIFNumbers = cifGetLayerName(str);
			if (currentCIFNumbers.equalsIgnoreCase(layer.getCIFLayer())) continue;
			layer.setCIFLayer(currentCIFNumbers);
		}
		boolean currentMimicsDisplay = cifOutputMimicsDisplay.isSelected();
		if (currentMimicsDisplay != initialCIFOutputMimicsDisplay)
			IOTool.setCIFOutMimicsDisplay(currentMimicsDisplay);

		boolean currentMergesPolygons = cifOutputMergesBoxes.isSelected();
		if (currentMergesPolygons != initialCIFOutputMergesPolygons)
			IOTool.setCIFOutMergesBoxes(currentMergesPolygons);

		boolean currentInstantiatesTopLevel = cifOutputInstantiatesTopLevel.isSelected();
		if (currentInstantiatesTopLevel != initialCIFOutputInstantiatesTopLevel)
			IOTool.setCIFOutInstantiatesTopLevel(currentInstantiatesTopLevel);

		boolean currentCheckResolution = cifCheckResolution.isSelected();
		if (currentCheckResolution != initialCIFOutputCheckResolution)
			IOTool.setCIFOutCheckResolution(currentCheckResolution);

		double currentResolution = TextUtils.atof(cifResolutionValue.getText());
		if (currentResolution != initialCIFOutputResolution)
			IOTool.setCIFOutResolution(currentResolution);
	}

	//******************************** GDS ********************************

	private boolean initialGDSOutputMergesBoxes;
	private boolean initialGDSOutputWritesExportPins;
	private boolean initialGDSOutputUpperCase;
	private int initialGDSTextLayer;
	private JList gdsLayersList;
	private DefaultListModel gdsLayersModel;
	private boolean changingGDS = false;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the GDS tab.
	 */
	private void initGDS()
	{
		Technology tech = Technology.getCurrent();
		gdsTechName.setText("Technology " + tech.getTechName() + ":");
		initialGDSOutputMergesBoxes = IOTool.isGDSOutMergesBoxes();
		gdsOutputMergesBoxes.setSelected(initialGDSOutputMergesBoxes);
		initialGDSOutputWritesExportPins = IOTool.isGDSOutWritesExportPins();
		gdsOutputWritesExportPins.setSelected(initialGDSOutputWritesExportPins);
		initialGDSOutputUpperCase = IOTool.isGDSOutUpperCase();
		gdsOutputUpperCase.setSelected(initialGDSOutputUpperCase);
		initialGDSTextLayer = IOTool.getGDSOutDefaultTextLayer();
		gdsDefaultTextLayer.setText(Integer.toString(initialGDSTextLayer));

		// build the layers list
		gdsLayersModel = new DefaultListModel();
		gdsLayersList = new JList(gdsLayersModel);
		gdsLayersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		gdsLayerList.setViewportView(gdsLayersList);
		gdsLayersList.clearSelection();
		gdsLayersList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { gdsClickLayer(); }
		});
		gdsLayersModel.clear();
		for(Iterator it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			String str = layer.getName();
			String gdsLayer = layer.getGDSLayer();
			if (gdsLayer != null) str += " (" + gdsLayer + ")";
			gdsLayersModel.addElement(str);
		}
		gdsLayersList.setSelectedIndex(0);
		gdsClickLayer();

		GDSDocumentListener myDocumentListener = new GDSDocumentListener(this);
		gdsLayerNumber.getDocument().addDocumentListener(myDocumentListener);
		gdsPinLayer.getDocument().addDocumentListener(myDocumentListener);
		gdsTextLayer.getDocument().addDocumentListener(myDocumentListener);

		// not yet
		gdsInputIncludesText.setEnabled(false);
		gdsInputExpandsCells.setEnabled(false);
		gdsInputInstantiatesArrays.setEnabled(false);
		gdsInputIgnoresUnknownLayers.setEnabled(false);
	}

	/**
	 * Method called when the user clicks on a layer name in the scrollable list.
	 */
	private void gdsClickLayer()
	{
		changingGDS = true;
		String str = (String)gdsLayersList.getSelectedValue();
		GDS.GDSLayers numbers = gdsGetNumbers(str);
		if (numbers == null) return;
		if (numbers.normal < 0) gdsLayerNumber.setText(""); else
			gdsLayerNumber.setText(Integer.toString(numbers.normal));
		if (numbers.pin < 0) gdsPinLayer.setText(""); else
			gdsPinLayer.setText(Integer.toString(numbers.pin));
		if (numbers.text < 0) gdsTextLayer.setText(""); else
			gdsTextLayer.setText(Integer.toString(numbers.text));
		changingGDS = false;
	}

	/**
	 * Method to parse the line in the scrollable list and return the GDS layer numbers part
	 * (in parentheses).
	 */
	private GDS.GDSLayers gdsGetNumbers(String str)
	{
		int openParen = str.indexOf('(');
		if (openParen < 0) return null;
		int closeParen = str.lastIndexOf(')');
		if (closeParen < 0) return null;
		String gdsNumbers = str.substring(openParen+1, closeParen);
		GDS.GDSLayers numbers = GDS.parseLayerString(gdsNumbers);
		return numbers;
	}

	/**
	 * Method to parse the line in the scrollable list and return the Layer.
	 */
	private Layer gdsGetLayer(String str)
	{
		int openParen = str.indexOf('(');
		if (openParen < 0) return null;
		String layerName = str.substring(0, openParen-1);
		Layer layer = Technology.getCurrent().findLayer(layerName);
		return layer;
	}

	/**
	 * Method called when the user types a new layer number into one of the 3 edit fields.
	 */
	private void gdsNumbersChanged()
	{
		if (changingGDS) return;
		String str = (String)gdsLayersList.getSelectedValue();
		Layer layer = gdsGetLayer(str);
		if (layer == null) return;
		String newLine = layer.getName() + " (" + gdsLayerNumber.getText().trim();
		String pinLayer = gdsPinLayer.getText().trim();
		if (pinLayer.length() > 0) newLine += "," + pinLayer + "p";
		String textLayer = gdsTextLayer.getText().trim();
		if (textLayer.length() > 0) newLine += "," + textLayer + "t";
		newLine += ")";
		int index = gdsLayersList.getSelectedIndex();
		gdsLayersModel.set(index, newLine);
	}

	/**
	 * Class to handle special changes to changes to a GDS layer.
	 */
	private static class GDSDocumentListener implements DocumentListener
	{
		IOOptions dialog;

		GDSDocumentListener(IOOptions dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.gdsNumbersChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.gdsNumbersChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.gdsNumbersChanged(); }
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the GDS tab.
	 */
	private void termGDS()
	{
		for(int i=0; i<gdsLayersModel.getSize(); i++)
		{
			String str = (String)gdsLayersModel.getElementAt(i);
			Layer layer = gdsGetLayer(str);
			if (layer == null) continue;

			GDS.GDSLayers numbers = gdsGetNumbers(str);
			if (numbers == null) continue;
			String currentGDSNumbers = "";
			if (numbers.normal >= 0) currentGDSNumbers += Integer.toString(numbers.normal);
			if (numbers.pin >= 0) currentGDSNumbers += "," + numbers.pin + "p";
			if (numbers.text >= 0) currentGDSNumbers += "," + numbers.text + "t";
			if (currentGDSNumbers.equalsIgnoreCase(layer.getGDSLayer())) continue;
			layer.setGDSLayer(currentGDSNumbers);
		}
		boolean currentOutputMergesBoxes = gdsOutputMergesBoxes.isSelected();
		if (currentOutputMergesBoxes != initialGDSOutputMergesBoxes)
			IOTool.setGDSOutMergesBoxes(currentOutputMergesBoxes);
		boolean currentOutputWritesExportPins = gdsOutputWritesExportPins.isSelected();
		if (currentOutputWritesExportPins != initialGDSOutputWritesExportPins)
			IOTool.setGDSOutWritesExportPins(currentOutputWritesExportPins);
		boolean currentOutputUpperCase = gdsOutputUpperCase.isSelected();
		if (currentOutputUpperCase != initialGDSOutputUpperCase)
			IOTool.setGDSOutUpperCase(currentOutputUpperCase);
		int currentTextLayer = TextUtils.atoi(gdsDefaultTextLayer.getText());
		if (currentTextLayer != initialGDSTextLayer)
			IOTool.setGDSOutDefaultTextLayer(currentTextLayer);
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

	private String initialCDLLibName;
	private String initialCDLLibPath;
	private boolean initialCDLConvertBrackets;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the CDL tab.
	 */
	private void initCDL()
	{
		initialCDLLibName = Simulation.getCDLLibName();
		cdlLibraryName.setText(initialCDLLibName);

		initialCDLLibPath = Simulation.getCDLLibPath();
		cdlLibraryPath.setText(initialCDLLibPath);

		initialCDLConvertBrackets = Simulation.isCDLConvertBrackets();
		cdlConvertBrackets.setSelected(initialCDLConvertBrackets);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the CDL tab.
	 */
	private void termCDL()
	{
		String nameNow = cdlLibraryName.getText();
		if (!nameNow.equals(initialCDLLibName)) Simulation.setCDLLibName(nameNow);

		String pathNow = cdlLibraryPath.getText();
		if (!pathNow.equals(initialCDLLibPath)) Simulation.setCDLLibPath(pathNow);

		boolean convertNow = cdlConvertBrackets.isSelected();
		if (convertNow != initialCDLConvertBrackets) Simulation.setCDLConvertBrackets(convertNow);
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

	private JList unitsTechnologyList;
	private DefaultListModel unitsTechnologyModel;
	private HashMap unitValues;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Units tab.
	 */
	private void initUnits()
	{
		// build the layers list
		unitsTechnologyModel = new DefaultListModel();
		unitsTechnologyList = new JList(unitsTechnologyModel);
		unitsTechnologyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		unitsList.setViewportView(unitsTechnologyList);
		unitsTechnologyList.clearSelection();
		unitsTechnologyList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { unitsClickTechnology(); }
		});
		unitsTechnologyModel.clear();
		unitValues = new HashMap();
		int wantIndex = 0;
		int index = 0;
		for(Iterator it = Technology.getTechnologiesSortedByName().iterator(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			if (tech.isNonElectrical()) continue;
			if (tech == Generic.tech || tech == Schematics.tech) continue;
			double shownScale = tech.getScale() / 2.0;
			unitValues.put(tech, new Double(shownScale));
			unitsTechnologyModel.addElement(tech.getTechName() + " (scale=" + shownScale + " nanometers)");
			if (tech == Technology.getCurrent()) wantIndex = index;
			index++;
		}
		unitsTechnologyList.setSelectedIndex(wantIndex);
		unitsClickTechnology();

		unitsScaleValue.getDocument().addDocumentListener(new UnitsDocumentListener(this));
	}

	/**
	 * Class to handle special changes to changes to a Technology in the Units panel.
	 */
	private static class UnitsDocumentListener implements DocumentListener
	{
		IOOptions dialog;

		UnitsDocumentListener(IOOptions dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.unitsNumbersChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.unitsNumbersChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.unitsNumbersChanged(); }
	}

	/**
	 * Method called when the user types a new scale factor into the edit fields.
	 */
	private void unitsNumbersChanged()
	{
		String str = (String)unitsTechnologyList.getSelectedValue();
		int spacePos = str.indexOf(" ");
		if (spacePos >= 0) str = str.substring(0, spacePos);
		Technology tech = Technology.findTechnology(str);
		if (tech == null) return;

		double shownScale = TextUtils.atof(unitsScaleValue.getText());
		unitValues.put(tech, new Double(shownScale));
		String newLine = tech.getTechName() + " (scale=" + shownScale + " nanometers)";
		int index = unitsTechnologyList.getSelectedIndex();
		unitsTechnologyModel.set(index, newLine);
		unitsAlternateScale.setText("nanometers (" + (shownScale/1000.0) + " microns)");
	}

	/**
	 * Method called when the user clicks on a layer name in the scrollable list.
	 */
	private void unitsClickTechnology()
	{
		String str = (String)unitsTechnologyList.getSelectedValue();
		int spacePos = str.indexOf(" ");
		if (spacePos >= 0) str = str.substring(0, spacePos);
		Technology tech = Technology.findTechnology(str);
		if (tech == null) return;
		Double shownValue = (Double)unitValues.get(tech);
		unitsScaleValue.setText(Double.toString(shownValue.doubleValue()));
		unitsAlternateScale.setText("nanometers (" + (shownValue.doubleValue()/1000.0) + " microns)");
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Units tab.
	 */
	private void termUnits()
	{
		for(Iterator it = unitValues.keySet().iterator(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			Double scaleValue = (Double)unitValues.get(tech);
			if (scaleValue.doubleValue() != tech.getScale()/2.0)
			{
				tech.setScale(scaleValue.doubleValue() * 2.0);
			}
		}
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

	private boolean initialUseCopyrightMessage;
	private String initialCopyrightMessage;
	private JTextArea copyrightTextArea;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Copyright tab.
	 */
	private void initCopyright()
	{
		initialUseCopyrightMessage = IOTool.isUseCopyrightMessage();
		if (initialUseCopyrightMessage) copyrightUse.setSelected(true); else
			copyrightNone.setSelected(true);

		copyrightTextArea = new JTextArea();
		copyrightMessage.setViewportView(copyrightTextArea);
		initialCopyrightMessage = IOTool.getCopyrightMessage();
		copyrightTextArea.setText(initialCopyrightMessage);
		copyrightTextArea.addKeyListener(new KeyAdapter()
		{
			public void keyTyped(KeyEvent evt) { copyrightMessageKeyTyped(evt); }
		});
	}

	private void copyrightMessageKeyTyped(KeyEvent evt)
	{
		copyrightUse.setSelected(true);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Copyright tab.
	 */
	private void termCopyright()
	{
		boolean currentUseCopyrightMessage = copyrightUse.isSelected();
		if (currentUseCopyrightMessage != initialUseCopyrightMessage)
			IOTool.setUseCopyrightMessage(currentUseCopyrightMessage);

		String msg = copyrightTextArea.getText();
		if (!msg.equals(initialCopyrightMessage))
			IOTool.setCopyrightMessage(msg);
	}

	//******************************** LIBRARY ********************************

	private int initialLibraryBackup;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Library tab.
	 */
	private void initLibrary()
	{
		initialLibraryBackup = IOTool.getBackupRedundancy();
		switch (initialLibraryBackup)
		{
			case 0: libNoBackup.setSelected(true);        break;
			case 1: libBackupLast.setSelected(true);      break;
			case 2: libBackupHistory.setSelected(true);   break;
		}
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Library tab.
	 */
	private void termLibrary()
	{
		int currentLibraryBackup = 0;
		if (libBackupLast.isSelected()) currentLibraryBackup = 1; else
		if (libBackupHistory.isSelected()) currentLibraryBackup = 2;
		if (currentLibraryBackup != initialLibraryBackup)
			IOTool.setBackupRedundancy(currentLibraryBackup);
	}

	//******************************** PRINTING ********************************

	private int initialPrintArea;
	private boolean initialPrintDate;
	private boolean initialPrintEncapsulated;
	private boolean initialPrintPlotter;
	private double initialPrintWidth, initialPrintHeight, initialPrintMargin;
	private int initialPrintRotation;
	private int initialPrintColorMethod;
	private Cell initialCell;
	private double initialEPSScale;
	private String initialEPSSyncFile;
	private String initialPrinter;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Printing tab.
	 */
	private void initPrinting()
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

		// get list of printers
		initialPrinter = IOTool.getPrinterName();
		PrintService [] printers = User.getPrinters();
		PrintService printerToUse = null;
		for(int i=0; i<printers.length; i++)
			printDefaultPrinter.addItem(printers[i].getName());
		printDefaultPrinter.setSelectedItem(initialPrinter);

		initialPrintEncapsulated = IOTool.isPrintEncapsulated();
		printEncapsulated.setSelected(initialPrintEncapsulated);

		initialPrintPlotter = IOTool.isPrintForPlotter();
		if (initialPrintPlotter) printUsePlotter.setSelected(true); else
			printUsePrinter.setSelected(true);

		initialPrintWidth = IOTool.getPrintWidth();
		printWidth.setText(Double.toString(initialPrintWidth));
		initialPrintHeight = IOTool.getPrintHeight();
		printHeight.setText(Double.toString(initialPrintHeight));
		initialPrintMargin = IOTool.getPrintMargin();
		printMargin.setText(Double.toString(initialPrintMargin));

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
		printEPSScale.setText(Double.toString(initialEPSScale));

		// not yet:
		printResolution.setEditable(false);
		printHPGL1.setEnabled(false);
		printHPGL2.setEnabled(false);
		printHPGLFillsPage.setEnabled(false);
		printHPGLFixedScale.setEnabled(false);
		printHPGLScale.setEditable(false);
	}

	private void printSetEPSSyncActionPerformed()
	{
		String fileName = OpenFile.chooseInputFile(OpenFile.Type.POSTSCRIPT, null);
		if (fileName == null) return;
		printSyncFileName.setText(fileName);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Printing tab.
	 */
	private void termPrinting()
	{
		int printArea = 0;
		if (printPlotHighlightedArea.isSelected()) printArea = 1; else
			if (printPlotDisplayedWindow.isSelected()) printArea = 2;
		if (printArea != initialPrintArea)
			IOTool.setPlotArea(printArea);

		boolean plotDate = printPlotDateInCorner.isSelected();
		if (plotDate != initialPrintDate)
			IOTool.setPlotDate(plotDate);

		String printer = (String)printDefaultPrinter.getSelectedItem();
		if (!printer.equals(initialPrinter))
		{
			IOTool.setPrinterName(printer);
		}

		boolean encapsulated = printEncapsulated.isSelected();
		if (encapsulated != initialPrintEncapsulated)
			IOTool.setPrintEncapsulated(encapsulated);

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
        cifInputSquaresWires = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        cifLayer = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        cifResolutionValue = new javax.swing.JTextField();
        cifCheckResolution = new javax.swing.JCheckBox();
        cifTechnology = new javax.swing.JLabel();
        gds = new javax.swing.JPanel();
        gdsLayerList = new javax.swing.JScrollPane();
        jLabel6 = new javax.swing.JLabel();
        gdsLayerNumber = new javax.swing.JTextField();
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
        gdsTechName = new javax.swing.JLabel();
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
        units = new javax.swing.JPanel();
        unitsList = new javax.swing.JScrollPane();
        jLabel10 = new javax.swing.JLabel();
        unitsScaleValue = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        unitsAlternateScale = new javax.swing.JLabel();
        copyright = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        copyrightNone = new javax.swing.JRadioButton();
        copyrightUse = new javax.swing.JRadioButton();
        jLabel5 = new javax.swing.JLabel();
        copyrightMessage = new javax.swing.JScrollPane();
        library = new javax.swing.JPanel();
        libNoBackup = new javax.swing.JRadioButton();
        libBackupLast = new javax.swing.JRadioButton();
        libBackupHistory = new javax.swing.JRadioButton();
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
        printCellName = new javax.swing.JLabel();
        printEPSScaleLabel = new javax.swing.JLabel();
        printEPSScale = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        printSyncFileName = new javax.swing.JTextField();
        printSynchLabel = new javax.swing.JLabel();
        printSetEPSSync = new javax.swing.JButton();
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
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(jLabel1, gridBagConstraints);

        cifLayers.setPreferredSize(new java.awt.Dimension(200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(cifLayers, gridBagConstraints);

        cifOutputMimicsDisplay.setText("Output Mimics Display");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(cifOutputMimicsDisplay, gridBagConstraints);

        cifOutputMergesBoxes.setText("Output Merges Boxes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        cif.add(cifOutputMergesBoxes, gridBagConstraints);

        cifOutputInstantiatesTopLevel.setText("Output Instantiates Top Level");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 8, 4);
        cif.add(cifOutputInstantiatesTopLevel, gridBagConstraints);

        cifInputSquaresWires.setText("Input Squares Wires");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 4, 4);
        cif.add(cifInputSquaresWires, gridBagConstraints);

        jLabel2.setText("(time consuming)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        cif.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(cifLayer, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel3.setText("Output resolution:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
        jPanel1.add(jLabel3, gridBagConstraints);

        cifResolutionValue.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(cifResolutionValue, gridBagConstraints);

        cifCheckResolution.setText("Find resolution errors");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(cifCheckResolution, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        cif.add(jPanel1, gridBagConstraints);

        cifTechnology.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        cif.add(cifTechnology, gridBagConstraints);

        tabPane.addTab("CIF", cif);

        gds.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 10;
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

        gdsLayerNumber.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsLayerNumber, gridBagConstraints);

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
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 6, 4);
        gds.add(gdsInputIgnoresUnknownLayers, gridBagConstraints);

        gdsOutputMergesBoxes.setText("Output merges Boxes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 4, 2, 4);
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

        gdsTechName.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gds.add(gdsTechName, gridBagConstraints);

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

        units.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(unitsList, gridBagConstraints);

        jLabel10.setText("The technology scale converts grid units to real spacing on the chip:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(jLabel10, gridBagConstraints);

        unitsScaleValue.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(unitsScaleValue, gridBagConstraints);

        jLabel11.setText("Technology scale:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(jLabel11, gridBagConstraints);

        unitsAlternateScale.setText("nanometers");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        units.add(unitsAlternateScale, gridBagConstraints);

        tabPane.addTab("Scale", units);

        copyright.setLayout(new java.awt.GridBagLayout());

        jLabel4.setText("A Copyright message can be added to every generated deck");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 20, 4);
        copyright.add(jLabel4, gridBagConstraints);

        copyrightNone.setText("No copyright message");
        copyrightGroup.add(copyrightNone);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        copyright.add(copyrightNone, gridBagConstraints);

        copyrightUse.setText("Use this copyright message:");
        copyrightGroup.add(copyrightUse);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        copyright.add(copyrightUse, gridBagConstraints);

        jLabel5.setText("Do not put comment characters in this message");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        copyright.add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        copyright.add(copyrightMessage, gridBagConstraints);

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

        tabPane.addTab("Library", library);

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

        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel18.setText("Default printer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel4.add(jLabel18, gridBagConstraints);

        printPlotDisplayedWindow.setText("Plot only Displayed Window");
        printingPlotArea.add(printPlotDisplayedWindow);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel4.add(printPlotDisplayedWindow, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel4.add(printDefaultPrinter, gridBagConstraints);

        jLabel19.setText("Print and Copy resolution factor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel4.add(jLabel19, gridBagConstraints);

        printResolution.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
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

        tabPane.addTab("Printing", printing);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
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
		termUnits();		// terminate the Units panel
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
    private javax.swing.JCheckBox cifCheckResolution;
    private javax.swing.JCheckBox cifInputSquaresWires;
    private javax.swing.JTextField cifLayer;
    private javax.swing.JScrollPane cifLayers;
    private javax.swing.JCheckBox cifOutputInstantiatesTopLevel;
    private javax.swing.JCheckBox cifOutputMergesBoxes;
    private javax.swing.JCheckBox cifOutputMimicsDisplay;
    private javax.swing.JTextField cifResolutionValue;
    private javax.swing.JLabel cifTechnology;
    private javax.swing.JPanel copyright;
    private javax.swing.ButtonGroup copyrightGroup;
    private javax.swing.JScrollPane copyrightMessage;
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
    private javax.swing.JTextField gdsLayerNumber;
    private javax.swing.JCheckBox gdsOutputMergesBoxes;
    private javax.swing.JCheckBox gdsOutputUpperCase;
    private javax.swing.JCheckBox gdsOutputWritesExportPins;
    private javax.swing.JTextField gdsPinLayer;
    private javax.swing.JLabel gdsTechName;
    private javax.swing.JTextField gdsTextLayer;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
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
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JRadioButton libBackupHistory;
    private javax.swing.JRadioButton libBackupLast;
    private javax.swing.JRadioButton libNoBackup;
    private javax.swing.JPanel library;
    private javax.swing.ButtonGroup libraryGroup;
    private javax.swing.JButton ok;
    private javax.swing.JLabel printCellName;
    private javax.swing.JComboBox printDefaultPrinter;
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
    private javax.swing.JPanel sue;
    private javax.swing.JCheckBox sueMake4PortTransistors;
    private javax.swing.JTabbedPane tabPane;
    private javax.swing.JPanel units;
    private javax.swing.JLabel unitsAlternateScale;
    private javax.swing.JScrollPane unitsList;
    private javax.swing.JTextField unitsScaleValue;
    // End of variables declaration//GEN-END:variables
	
}
