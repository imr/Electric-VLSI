/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SpiceTab.java
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
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.dialogs.OpenFile;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;


/**
 * Class to handle the "Spice" tab of the Preferences dialog.
 */
public class SpiceTab extends PreferencePanel
{
	/** Creates new form SpiceTab */
	public SpiceTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return spice; }

	public String getName() { return "Spice"; }

	private JList spiceLayerList;
	private JList spiceCellList;
	private DefaultListModel spiceLayerListModel;
	private DefaultListModel spiceCellListModel;
	private int spiceEngineInitial;
	private String spiceLevelInitial;
	private String spiceOutputFormatInitial;
	private String spicePartsLibraryInitial;
	private boolean spiceUseParasiticsInitial;
	private boolean spiceUseNodeNamesInitial;
	private boolean spiceForceGlobalPwrGndInitial;
	private boolean spiceUseCellParametersInitial;
	private boolean spiceWriteTransSizesInLambdaInitial;
	private double spiceTechMinResistanceInitial;
	private double spiceTechMinCapacitanceInitial;
    private double spiceGateLengthSubtractionInitial;
	private String spiceHeaderCardInitial;
	private String spiceTrailerCardInitial;
	private HashMap spiceLayerResistanceOptions;
	private HashMap spiceLayerCapacitanceOptions;
	private HashMap spiceLayerEdgeCapacitanceOptions;
	private HashMap spiceCellModelOptions;
    private boolean spiceUseRunDirInitial;
    private String spiceRunDirInitial;
    private boolean overwriteOutputFileInitial;
    private boolean spiceRunProbeInitial;
    private String spiceRunProgramInitial;
    private String spiceRunProgramArgsInitial;
    private String spiceRunPopupInitial;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Spice tab.
	 */
	public void init()
	{
		// the top section: general controls
		spiceEngineInitial = Simulation.getSpiceEngine();
		spiceEnginePopup.addItem("Spice 2");
		spiceEnginePopup.addItem("Spice 3");
		spiceEnginePopup.addItem("HSpice");
		spiceEnginePopup.addItem("PSpice");
		spiceEnginePopup.addItem("Gnucap");
		spiceEnginePopup.addItem("SmartSpice");
		spiceEnginePopup.setSelectedIndex(spiceEngineInitial);

		spiceLevelInitial = Simulation.getSpiceLevel();
		spiceLevelPopup.addItem("1");
		spiceLevelPopup.addItem("2");
		spiceLevelPopup.addItem("3");
		spiceLevelPopup.setSelectedItem(spiceLevelInitial);

		spiceOutputFormatInitial = Simulation.getSpiceOutputFormat();
		spiceOutputFormatPopup.addItem("Standard");
		spiceOutputFormatPopup.addItem("Raw");
		spiceOutputFormatPopup.addItem("Raw/Smart");
		spiceOutputFormatPopup.setSelectedItem(spiceOutputFormatInitial);

		spiceUseParasiticsInitial = Simulation.isSpiceUseParasitics();
		spiceUseParasitics.setSelected(spiceUseParasiticsInitial);

		spiceUseNodeNamesInitial = Simulation.isSpiceUseNodeNames();
		spiceUseNodeNames.setSelected(spiceUseNodeNamesInitial);

		spiceForceGlobalPwrGndInitial = Simulation.isSpiceForceGlobalPwrGnd();
		spiceForceGlobalPwrGnd.setSelected(spiceForceGlobalPwrGndInitial);

		spiceUseCellParametersInitial = Simulation.isSpiceUseCellParameters();
		spiceUseCellParameters.setSelected(spiceUseCellParametersInitial);

		spiceWriteTransSizesInLambdaInitial = Simulation.isSpiceWriteTransSizeInLambda();
		spiceWriteTransSizesInLambda.setSelected(spiceWriteTransSizesInLambdaInitial);

        // spice Run options
        spiceRunDirInitial = Simulation.getSpiceRunDir();
        useDir.setText(spiceRunDirInitial);

        spiceUseRunDirInitial = Simulation.getSpiceUseRunDir();
        useDirCheckBox.setSelected(spiceUseRunDirInitial);

        overwriteOutputFileInitial = Simulation.getSpiceOutputOverwrite();
        overwriteOutputFile.setSelected(overwriteOutputFileInitial);

        spiceRunProbeInitial = Simulation.getSpiceRunProbe();
        spiceRunProbe.setSelected(spiceRunProbeInitial);

        spiceRunProgramInitial = Simulation.getSpiceRunProgram();
        spiceRunProgram.setText(spiceRunProgramInitial);

        spiceRunProgramArgsInitial = Simulation.getSpiceRunProgramArgs();
        spiceRunProgramArgs.setText(spiceRunProgramArgsInitial);

		spicePartsLibraryInitial = Simulation.getSpicePartsLibrary();
		String [] libFiles = LibFile.getSpicePartsLibraries();
		for(int i=0; i<libFiles.length; i++)
			spicePrimitivesetPopup.addItem(libFiles[i]);
		spicePrimitivesetPopup.setSelectedItem(spicePartsLibraryInitial);

        String [] runChoices = Simulation.getSpiceRunChoiceValues();
        for (int i=0; i<runChoices.length; i++) {
            spiceRunPopup.addItem(runChoices[i]);
        }
        spiceRunPopupInitial = Simulation.getSpiceRunChoice();
        spiceRunPopup.setSelectedItem(spiceRunPopupInitial);
        if (spiceRunPopup.getSelectedIndex() == 0) setSpiceRunOptionsEnabled(false);
        else setSpiceRunOptionsEnabled(true);

		// the next section: parasitic values
		spiceTechnology.setText("For technology " + curTech.getTechName());

		spiceLayerResistanceOptions = new HashMap();
		spiceLayerCapacitanceOptions = new HashMap();
		spiceLayerEdgeCapacitanceOptions = new HashMap();
		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			spiceLayerResistanceOptions.put(layer, Pref.makeDoublePref(null, null, layer.getResistance()));
			spiceLayerCapacitanceOptions.put(layer, Pref.makeDoublePref(null, null, layer.getCapacitance()));
			spiceLayerEdgeCapacitanceOptions.put(layer, Pref.makeDoublePref(null, null, layer.getEdgeCapacitance()));
		}
		spiceLayerListModel = new DefaultListModel();
		spiceLayerList = new JList(spiceLayerListModel);
		spiceLayerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		spiceLayer.setViewportView(spiceLayerList);
		spiceLayerList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { spiceLayerListClick(); }
		});
		showLayersInTechnology(spiceLayerListModel);
		spiceLayerList.setSelectedIndex(0);
		spiceLayerListClick();
		spiceResistance.getDocument().addDocumentListener(new LayerDocumentListener(spiceLayerResistanceOptions, spiceLayerList, curTech));
		spiceCapacitance.getDocument().addDocumentListener(new LayerDocumentListener(spiceLayerCapacitanceOptions, spiceLayerList, curTech));
		spiceEdgeCapacitance.getDocument().addDocumentListener(new LayerDocumentListener(spiceLayerEdgeCapacitanceOptions, spiceLayerList, curTech));
	
		spiceTechMinResistanceInitial = curTech.getMinResistance();
		spiceMinResistance.setText(Double.toString(spiceTechMinResistanceInitial));

		spiceTechMinCapacitanceInitial = curTech.getMinCapacitance();
		spiceMinCapacitance.setText(Double.toString(spiceTechMinCapacitanceInitial));

        spiceGateLengthSubtractionInitial = curTech.getGateLengthSubtraction();
        spiceGateLengthSubtraction.setText(Double.toString(spiceGateLengthSubtractionInitial));

		// the next section: header and trailer cards
		spiceHeaderCardInitial = Simulation.getSpiceHeaderCardInfo();
		if (spiceHeaderCardInitial.length() == 0) spiceNoHeaderCards.setSelected(true); else
		{
			if (spiceHeaderCardInitial.startsWith(Spice.SPICE_EXTENSION_PREFIX))
			{
				spiceHeaderCardsWithExtension.setSelected(true);
				spiceHeaderCardExtension.setText(spiceHeaderCardInitial.substring(Spice.SPICE_EXTENSION_PREFIX.length()));
			} else
			{
				spiceHeaderCardsFromFile.setSelected(true);
				spiceHeaderCardFile.setText(spiceHeaderCardInitial);
			}
		}
		spiceTrailerCardInitial = Simulation.getSpiceTrailerCardInfo();
		if (spiceTrailerCardInitial.length() == 0) spiceNoTrailerCards.setSelected(true); else
		{
			if (spiceTrailerCardInitial.startsWith(Spice.SPICE_EXTENSION_PREFIX))
			{
				spiceTrailerCardsWithExtension.setSelected(true);
				spiceTrailerCardExtension.setText(spiceTrailerCardInitial.substring(Spice.SPICE_EXTENSION_PREFIX.length()));
			} else
			{
				spiceTrailerCardsFromFile.setSelected(true);
				spiceTrailerCardFile.setText(spiceTrailerCardInitial);
			}
		}
		spiceBrowseHeaderFile.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { spiceBrowseHeaderFileActionPerformed(); }
		});
		spiceBrowseTrailerFile.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { spiceBrowseTrailerFileActionPerformed(); }
		});

		// the last section has cell overrides
		spiceCellModelOptions = new HashMap();
		spiceCellListModel = new DefaultListModel();
		spiceCellList = new JList(spiceCellListModel);
		spiceCellList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		spiceCell.setViewportView(spiceCellList);
		for(Iterator it = curLib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			spiceCellListModel.addElement(cell.noLibDescribe());
			String modelFile = "";
			Variable var = cell.getVar(Spice.SPICE_MODEL_FILE_KEY, String.class);
			if (var != null) modelFile = (String)var.getObject();
			spiceCellModelOptions.put(cell, Pref.makeStringPref(null, null, modelFile));
		}
		spiceCellList.setSelectedIndex(0);
		spiceCellList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { spiceCellListClick(); }
		});
		spiceModelFileBrowse.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { spiceModelFileBrowseActionPerformed(); }
		});
		spiceDeriveModelFromCircuit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { spiceCellModelButtonClick(); }
		});
		spiceUseModelFromFile.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { spiceCellModelButtonClick(); }
		});
		spiceModelCell.getDocument().addDocumentListener(new SpiceModelDocumentListener(this));
		spiceCellListClick();
	}

	private boolean spiceModelFileChanging = false;

	private void showLayersInTechnology(DefaultListModel model)
	{
		model.clear();
		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			model.addElement(layer.getName());
		}
	}

	/**
	 * Method called when the user clicks on a model file radio button at in the bottom of the Spice Options dialog.
	 */
	private void spiceCellModelButtonClick()
	{
		if (spiceDeriveModelFromCircuit.isSelected()) spiceModelCell.setText("");
	}

	/**
	 * Method called when the user clicks on a cell name at in the bottom of the Spice Options dialog.
	 */
	private void spiceCellListClick()
	{
		if (spiceCellListModel.size() == 0) return;
		String cellName = (String)spiceCellList.getSelectedValue();
		Cell cell = curLib.findNodeProto(cellName);
		if (cell != null)
		{
			Pref pref = (Pref)spiceCellModelOptions.get(cell);
			String modelFile = pref.getString();
			spiceModelFileChanging = true;
			spiceModelCell.setText(modelFile);
			if (modelFile.length() == 0) spiceDeriveModelFromCircuit.setSelected(true); else
				spiceUseModelFromFile.setSelected(true);
			spiceModelFileChanging = false;
		}
	}

	/**
	 * Method called when the user clicks on the "Browse" button in the bottom of the Spice Options dialog.
	 */
	private void spiceModelFileBrowseActionPerformed()
	{
		String fileName = OpenFile.chooseInputFile(FileType.ANY, null);
		if (fileName == null) return;
		spiceUseModelFromFile.setSelected(true);
		spiceModelCell.setText(fileName);
	}

	/**
	 * Method called when the user changes the model file name at the bottom of the Spice Options dialog.
	 */
	private void spiceModelFileChanged()
	{
		if (spiceModelFileChanging) return;
		String cellName = (String)spiceCellList.getSelectedValue();
		Cell cell = curLib.findNodeProto(cellName);
		if (cell != null)
		{
			Pref pref = (Pref)spiceCellModelOptions.get(cell);
			String typedString = spiceModelCell.getText();
			if (spiceDeriveModelFromCircuit.isSelected()) typedString = "";
			pref.setString(typedString);
		}
	}

	/**
	 * Class to handle changes to per-cell model file names.
	 */
	private static class SpiceModelDocumentListener implements DocumentListener
	{
		SpiceTab dialog;

		SpiceModelDocumentListener(SpiceTab dialog)
		{
			this.dialog = dialog;
		}

		public void changedUpdate(DocumentEvent e) { dialog.spiceModelFileChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.spiceModelFileChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.spiceModelFileChanged(); }
	}

	private void spiceBrowseTrailerFileActionPerformed()
	{
		String fileName = OpenFile.chooseInputFile(FileType.ANY, null);
		if (fileName == null) return;
		spiceTrailerCardFile.setText(fileName);
		spiceTrailerCardsFromFile.setSelected(true);
	}

	private void spiceBrowseHeaderFileActionPerformed()
	{
		String fileName = OpenFile.chooseInputFile(FileType.ANY, null);
		if (fileName == null) return;
		spiceHeaderCardFile.setText(fileName);
		spiceHeaderCardsFromFile.setSelected(true);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Spice tab.
	 */
	public void term()
	{
		// the top section: general controls
		int intNow = spiceEnginePopup.getSelectedIndex();
		if (spiceEngineInitial != intNow) Simulation.setSpiceEngine(intNow);

		String stringNow = (String)spiceLevelPopup.getSelectedItem();
		if (!spiceLevelInitial.equals(stringNow)) Simulation.setSpiceLevel(stringNow);

		stringNow = (String)spiceOutputFormatPopup.getSelectedItem();
		if (!spiceOutputFormatInitial.equals(stringNow)) Simulation.setSpiceOutputFormat(stringNow);

		stringNow = (String)spicePrimitivesetPopup.getSelectedItem();
		if (!spicePartsLibraryInitial.equals(stringNow)) Simulation.setSpicePartsLibrary(stringNow);

		boolean booleanNow = spiceUseNodeNames.isSelected();
		if (spiceUseNodeNamesInitial != booleanNow) Simulation.setSpiceUseNodeNames(booleanNow);

		booleanNow = spiceForceGlobalPwrGnd.isSelected();
		if (spiceForceGlobalPwrGndInitial != booleanNow) Simulation.setSpiceForceGlobalPwrGnd(booleanNow);

		booleanNow = spiceUseCellParameters.isSelected();
		if (spiceUseCellParametersInitial != booleanNow) Simulation.setSpiceUseCellParameters(booleanNow);

		booleanNow = spiceWriteTransSizesInLambda.isSelected();
		if (spiceWriteTransSizesInLambdaInitial != booleanNow) Simulation.setSpiceWriteTransSizeInLambda(booleanNow);

		booleanNow = spiceUseParasitics.isSelected();
		if (spiceUseParasiticsInitial != booleanNow) Simulation.setSpiceUseParasitics(booleanNow);

        // spice run options
        stringNow = (String)spiceRunPopup.getSelectedItem();
        if (!spiceRunPopupInitial.equals(stringNow)) Simulation.setSpiceRunChoice(stringNow);

        stringNow = useDir.getText();
        if (!spiceRunDirInitial.equals(stringNow)) Simulation.setSpiceRunDir(stringNow);

        booleanNow = useDirCheckBox.isSelected();
        if (spiceUseRunDirInitial != booleanNow) Simulation.setSpiceUseRunDir(booleanNow);

        booleanNow = overwriteOutputFile.isSelected();
        if (overwriteOutputFileInitial != booleanNow) Simulation.setSpiceOutputOverwrite(booleanNow);

        booleanNow = spiceRunProbe.isSelected();
        if (spiceRunProbeInitial != booleanNow) Simulation.setSpiceRunProbe(booleanNow);

        stringNow = spiceRunProgram.getText();
        if (!spiceRunProgramInitial.equals(stringNow)) Simulation.setSpiceRunProgram(stringNow);

        stringNow = spiceRunProgramArgs.getText();
        if (!spiceRunProgramArgsInitial.equals(stringNow)) Simulation.setSpiceRunProgramArgs(stringNow);

		// the next section: parasitic values
		double doubleNow = TextUtils.atof(spiceMinResistance.getText());
		if (spiceTechMinResistanceInitial != doubleNow) curTech.setMinResistance(doubleNow);
		doubleNow = TextUtils.atof(spiceMinCapacitance.getText());
		if (spiceTechMinCapacitanceInitial != doubleNow) curTech.setMinCapacitance(doubleNow);

		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			Pref resistancePref = (Pref)spiceLayerResistanceOptions.get(layer);
			if (resistancePref != null && resistancePref.getDoubleFactoryValue() != resistancePref.getDouble())
				layer.setResistance(resistancePref.getDouble());
			Pref capacitancePref = (Pref)spiceLayerCapacitanceOptions.get(layer);
			if (capacitancePref != null && capacitancePref.getDoubleFactoryValue() != capacitancePref.getDouble())
				layer.setCapacitance(capacitancePref.getDouble());
			Pref edgeCapacitancePref = (Pref)spiceLayerEdgeCapacitanceOptions.get(layer);
			if (edgeCapacitancePref != null && edgeCapacitancePref.getDoubleFactoryValue() != edgeCapacitancePref.getDouble())
				layer.setEdgeCapacitance(edgeCapacitancePref.getDouble());
		}

        doubleNow = TextUtils.atof(spiceGateLengthSubtraction.getText());
        if (spiceGateLengthSubtractionInitial != doubleNow) curTech.setGateLengthSubtraction(doubleNow);

		// the next section: header and trailer cards
		String header = "";
		if (spiceHeaderCardsWithExtension.isSelected())
		{
			header = Spice.SPICE_EXTENSION_PREFIX + spiceHeaderCardExtension.getText();
		} else if (spiceHeaderCardsFromFile.isSelected())
		{
			header = spiceHeaderCardFile.getText();
		}
		if (!spiceHeaderCardInitial.equals(header)) Simulation.setSpiceHeaderCardInfo(header);

		String trailer = "";
		if (spiceTrailerCardsWithExtension.isSelected())
		{
			trailer = Spice.SPICE_EXTENSION_PREFIX + spiceTrailerCardExtension.getText();
		} else if (spiceTrailerCardsFromFile.isSelected())
		{
			trailer = spiceTrailerCardFile.getText();
		}
		if (!spiceTrailerCardInitial.equals(trailer)) Simulation.setSpiceTrailerCardInfo(trailer);

		// bottom section: model file overrides for cells
		for(Iterator it = curLib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			Pref pref = (Pref)spiceCellModelOptions.get(cell);
			if (pref == null) continue;
			if (!pref.getStringFactoryValue().equals(pref.getString()))
			{
				String fileName = pref.getString().trim();
				if (fileName.length() == 0) cell.delVar(Spice.SPICE_MODEL_FILE_KEY); else
					cell.newVar(Spice.SPICE_MODEL_FILE_KEY, fileName);
			}
		}
	}

	private void spiceLayerListClick()
	{
		String layerName = (String)spiceLayerList.getSelectedValue();
		Layer layer = curTech.findLayer(layerName);
		if (layer != null)
		{
			Pref resistancePref = (Pref)spiceLayerResistanceOptions.get(layer);
			spiceResistance.setText(Double.toString(resistancePref.getDouble()));
			Pref capacitancePref = (Pref)spiceLayerCapacitanceOptions.get(layer);
			spiceCapacitance.setText(Double.toString(capacitancePref.getDouble()));
			Pref edgeCapacitancePref = (Pref)spiceLayerEdgeCapacitanceOptions.get(layer);
			spiceEdgeCapacitance.setText(Double.toString(edgeCapacitancePref.getDouble()));
		}
	}

	/**
	 * Class to handle special changes to per-layer parasitics.
	 */
	private static class LayerDocumentListener implements DocumentListener
	{
		HashMap optionMap;
		JList list;
		Technology tech;

		LayerDocumentListener(HashMap optionMap, JList list, Technology tech)
		{
			this.optionMap = optionMap;
			this.list = list;
			this.tech = tech;
		}

		private void change(DocumentEvent e)
		{
			// get the currently selected layer
			String layerName = (String)list.getSelectedValue();
			Layer layer = tech.findLayer(layerName);
			if (layer == null) return;

			// get the typed value
			Document doc = e.getDocument();
			int len = doc.getLength();
			String text;
			try
			{
				text = doc.getText(0, len);
			} catch (BadLocationException ex) { return; }
			Pref pref = (Pref)optionMap.get(layer);
			double v = TextUtils.atof(text);

			// update the option
			pref.setDouble(v);
		}

		public void changedUpdate(DocumentEvent e) { change(e); }
		public void insertUpdate(DocumentEvent e) { change(e); }
		public void removeUpdate(DocumentEvent e) { change(e); }
	}

    // enable or disable the spice run options
    private void setSpiceRunOptionsEnabled(boolean enabled) {
        useDirCheckBox.setEnabled(enabled);
        overwriteOutputFile.setEnabled(enabled);
        spiceRunProgram.setEnabled(enabled);
        spiceRunProgramArgs.setEnabled(enabled);
        spiceRunHelp.setEnabled(enabled);
        spiceRunProbe.setEnabled(enabled);
        if (enabled) {
            spiceRunProgram.setBackground(Color.white);
            if (!useDirCheckBox.isSelected()) {
                // if no use dir specified, disable text box
                useDir.setEnabled(false);
                useDir.setBackground(spice2.getBackground());
            } else {
                useDir.setEnabled(true);
                useDir.setBackground(Color.white);
            }
        } else {
            spiceRunProgram.setBackground(spice2.getBackground());
            useDir.setBackground(spice2.getBackground());
            useDir.setEnabled(false);
        }
    }

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        spiceHeader = new javax.swing.ButtonGroup();
        spiceTrailer = new javax.swing.ButtonGroup();
        spiceModel = new javax.swing.ButtonGroup();
        spice = new javax.swing.JPanel();
        spice1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        spiceRunPopup = new javax.swing.JComboBox();
        spiceEnginePopup = new javax.swing.JComboBox();
        spiceLevelPopup = new javax.swing.JComboBox();
        spiceOutputFormatPopup = new javax.swing.JComboBox();
        spiceUseParasitics = new javax.swing.JCheckBox();
        spiceUseNodeNames = new javax.swing.JCheckBox();
        spiceForceGlobalPwrGnd = new javax.swing.JCheckBox();
        spiceUseCellParameters = new javax.swing.JCheckBox();
        spiceWriteTransSizesInLambda = new javax.swing.JCheckBox();
        spice2 = new javax.swing.JPanel();
        spiceRunProgram = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        useDirCheckBox = new javax.swing.JCheckBox();
        useDir = new javax.swing.JTextField();
        overwriteOutputFile = new javax.swing.JCheckBox();
        spiceRunHelp = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        spiceRunProgramArgs = new javax.swing.JTextField();
        spiceRunProbe = new javax.swing.JCheckBox();
        spice3 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        spicePrimitivesetPopup = new javax.swing.JComboBox();
        jSeparator1 = new javax.swing.JSeparator();
        spice4 = new javax.swing.JPanel();
        spiceLayer = new javax.swing.JScrollPane();
        jLabel7 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        spiceTechnology = new javax.swing.JLabel();
        spiceResistance = new javax.swing.JTextField();
        spiceCapacitance = new javax.swing.JTextField();
        spiceEdgeCapacitance = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        spiceMinResistance = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        spiceMinCapacitance = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        spiceGateLengthSubtraction = new javax.swing.JTextField();
        jSeparator2 = new javax.swing.JSeparator();
        spice5 = new javax.swing.JPanel();
        spiceHeaderCardExtension = new javax.swing.JTextField();
        spiceNoHeaderCards = new javax.swing.JRadioButton();
        spiceHeaderCardsWithExtension = new javax.swing.JRadioButton();
        spiceHeaderCardsFromFile = new javax.swing.JRadioButton();
        spiceNoTrailerCards = new javax.swing.JRadioButton();
        spiceTrailerCardsWithExtension = new javax.swing.JRadioButton();
        spiceTrailerCardsFromFile = new javax.swing.JRadioButton();
        spiceHeaderCardFile = new javax.swing.JTextField();
        spiceBrowseHeaderFile = new javax.swing.JButton();
        spiceTrailerCardExtension = new javax.swing.JTextField();
        spiceTrailerCardFile = new javax.swing.JTextField();
        spiceBrowseTrailerFile = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        spice6 = new javax.swing.JPanel();
        spiceCell = new javax.swing.JScrollPane();
        jLabel8 = new javax.swing.JLabel();
        spiceDeriveModelFromCircuit = new javax.swing.JRadioButton();
        spiceUseModelFromFile = new javax.swing.JRadioButton();
        spiceModelFileBrowse = new javax.swing.JButton();
        spiceModelCell = new javax.swing.JTextField();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Spice Preferences");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        spice.setLayout(new java.awt.GridBagLayout());

        spice.setToolTipText("Options for Spice deck generation");
        spice1.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Spice Engine:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        spice1.add(jLabel1, gridBagConstraints);

        jLabel9.setText("Spice Level:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(jLabel9, gridBagConstraints);

        jLabel10.setText("Output format:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(jLabel10, gridBagConstraints);

        spiceRunPopup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spiceRunPopupActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        spice1.add(spiceRunPopup, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice1.add(spiceEnginePopup, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice1.add(spiceLevelPopup, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice1.add(spiceOutputFormatPopup, gridBagConstraints);

        spiceUseParasitics.setText("Use Parasitics");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceUseParasitics, gridBagConstraints);

        spiceUseNodeNames.setText("Use Node Names");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceUseNodeNames, gridBagConstraints);

        spiceForceGlobalPwrGnd.setText("Force Global VDD/GND");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceForceGlobalPwrGnd, gridBagConstraints);

        spiceUseCellParameters.setText("Use Cell Parameters");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceUseCellParameters, gridBagConstraints);

        spiceWriteTransSizesInLambda.setText("Write Trans Sizes in Units");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceWriteTransSizesInLambda, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        spice.add(spice1, gridBagConstraints);

        spice2.setLayout(new java.awt.GridBagLayout());

        spice2.setBorder(new javax.swing.border.EtchedBorder());
        spiceRunProgram.setColumns(8);
        spiceRunProgram.setMinimumSize(new java.awt.Dimension(100, 20));
        spiceRunProgram.setPreferredSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        spice2.add(spiceRunProgram, gridBagConstraints);

        jLabel17.setText("Run Program:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice2.add(jLabel17, gridBagConstraints);

        useDirCheckBox.setText("Use Dir:");
        useDirCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                useDirCheckBoxActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        spice2.add(useDirCheckBox, gridBagConstraints);

        useDir.setColumns(8);
        useDir.setText("jTextField1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice2.add(useDir, gridBagConstraints);

        overwriteOutputFile.setText("Overwrite existing output file (no prompts)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        spice2.add(overwriteOutputFile, gridBagConstraints);

        spiceRunHelp.setText("help");
        spiceRunHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spiceRunHelpActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        spice2.add(spiceRunHelp, gridBagConstraints);

        jLabel3.setText("with args:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        spice2.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        spice2.add(spiceRunProgramArgs, gridBagConstraints);

        spiceRunProbe.setText("Run probe");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        spice2.add(spiceRunProbe, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        spice.add(spice2, gridBagConstraints);

        spice3.setLayout(new java.awt.GridBagLayout());

        jLabel13.setText("Spice primitive set:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        spice3.add(jLabel13, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice3.add(spicePrimitivesetPopup, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        spice.add(spice3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        spice.add(jSeparator1, gridBagConstraints);

        spice4.setLayout(new java.awt.GridBagLayout());

        spiceLayer.setMinimumSize(new java.awt.Dimension(200, 50));
        spiceLayer.setPreferredSize(new java.awt.Dimension(200, 50));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice4.add(spiceLayer, gridBagConstraints);

        jLabel7.setText("Layer:");
        jLabel7.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        spice4.add(jLabel7, gridBagConstraints);

        jLabel2.setText("Perimeter Cap (fF/um):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        spice4.add(jLabel2, gridBagConstraints);

        jLabel11.setText("Resistance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        spice4.add(jLabel11, gridBagConstraints);

        jLabel12.setText("Area Cap (fF/um^2):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        spice4.add(jLabel12, gridBagConstraints);

        spiceTechnology.setText("Technology: xxx");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        spice4.add(spiceTechnology, gridBagConstraints);

        spiceResistance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice4.add(spiceResistance, gridBagConstraints);

        spiceCapacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice4.add(spiceCapacitance, gridBagConstraints);

        spiceEdgeCapacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice4.add(spiceEdgeCapacitance, gridBagConstraints);

        jLabel18.setText("Min. Resistance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice4.add(jLabel18, gridBagConstraints);

        spiceMinResistance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice4.add(spiceMinResistance, gridBagConstraints);

        jLabel19.setText("Min. Capacitance (fF):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        spice4.add(jLabel19, gridBagConstraints);

        spiceMinCapacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice4.add(spiceMinCapacitance, gridBagConstraints);

        jLabel4.setText("Gate Length Shrink (Subtraction) um:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        spice4.add(jLabel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice4.add(spiceGateLengthSubtraction, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        spice.add(spice4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        spice.add(jSeparator2, gridBagConstraints);

        spice5.setLayout(new java.awt.GridBagLayout());

        spiceHeaderCardExtension.setColumns(5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        spice5.add(spiceHeaderCardExtension, gridBagConstraints);

        spiceNoHeaderCards.setText("No Header Cards");
        spiceHeader.add(spiceNoHeaderCards);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(spiceNoHeaderCards, gridBagConstraints);

        spiceHeaderCardsWithExtension.setText("Use Header Cards with extension:");
        spiceHeader.add(spiceHeaderCardsWithExtension);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(spiceHeaderCardsWithExtension, gridBagConstraints);

        spiceHeaderCardsFromFile.setText("Use Header Cards from File:");
        spiceHeader.add(spiceHeaderCardsFromFile);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(spiceHeaderCardsFromFile, gridBagConstraints);

        spiceNoTrailerCards.setText("No Trailer Cards");
        spiceTrailer.add(spiceNoTrailerCards);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(spiceNoTrailerCards, gridBagConstraints);

        spiceTrailerCardsWithExtension.setText("Use Trailer Cards with extension:");
        spiceTrailer.add(spiceTrailerCardsWithExtension);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(spiceTrailerCardsWithExtension, gridBagConstraints);

        spiceTrailerCardsFromFile.setText("Use Trailer Cards from File:");
        spiceTrailer.add(spiceTrailerCardsFromFile);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(spiceTrailerCardsFromFile, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        spice5.add(spiceHeaderCardFile, gridBagConstraints);

        spiceBrowseHeaderFile.setText("Browse");
        spiceBrowseHeaderFile.setMinimumSize(new java.awt.Dimension(78, 20));
        spiceBrowseHeaderFile.setPreferredSize(new java.awt.Dimension(78, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice5.add(spiceBrowseHeaderFile, gridBagConstraints);

        spiceTrailerCardExtension.setColumns(5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        spice5.add(spiceTrailerCardExtension, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        spice5.add(spiceTrailerCardFile, gridBagConstraints);

        spiceBrowseTrailerFile.setText("Browse");
        spiceBrowseTrailerFile.setMinimumSize(new java.awt.Dimension(78, 20));
        spiceBrowseTrailerFile.setPreferredSize(new java.awt.Dimension(78, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice5.add(spiceBrowseTrailerFile, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice5.add(jSeparator4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        spice.add(spice5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        spice.add(jSeparator3, gridBagConstraints);

        spice6.setLayout(new java.awt.GridBagLayout());

        spiceCell.setMinimumSize(new java.awt.Dimension(200, 100));
        spiceCell.setPreferredSize(new java.awt.Dimension(200, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        spice6.add(spiceCell, gridBagConstraints);

        jLabel8.setText("For Cell");
        jLabel8.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        spice6.add(jLabel8, gridBagConstraints);

        spiceDeriveModelFromCircuit.setText("Derive Model from Circuitry");
        spiceModel.add(spiceDeriveModelFromCircuit);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice6.add(spiceDeriveModelFromCircuit, gridBagConstraints);

        spiceUseModelFromFile.setText("Use Model from File:");
        spiceModel.add(spiceUseModelFromFile);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice6.add(spiceUseModelFromFile, gridBagConstraints);

        spiceModelFileBrowse.setText("Browse");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice6.add(spiceModelFileBrowse, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        spice6.add(spiceModelCell, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        spice.add(spice6, gridBagConstraints);

        getContentPane().add(spice, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

    private void spiceRunPopupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spiceRunPopupActionPerformed
        if (spiceRunPopup.getSelectedIndex() == 0) {
            setSpiceRunOptionsEnabled(false);
        } else
            setSpiceRunOptionsEnabled(true);
    }//GEN-LAST:event_spiceRunPopupActionPerformed

    private void spiceRunHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spiceRunHelpActionPerformed
        String [] message = { "IMPORTANT: This executes a single program with the given args.  It does NOT run a command-line command.",
                              "For example, 'echo blah > file' will NOT work. Encapsulate it in a script if you want to do such things.",
                              "-----------------",
                              "The following variables are available to use in the program name and arguments:",
                              "   ${WORKING_DIR}:  The current working directory",
                              "   ${USE_DIR}:  The Use Dir field, if specified (otherwise defaults to WORKING_DIR)",
                              "   ${FILENAME}:  The output file name (with extension)",
                              "   ${FILENAME_NO_EXT}:  The output file name (without extension)",
                              "Example: Program: \"hspice\".  Args: \"${FILENAME}\"" };
        JOptionPane.showMessageDialog(this, message, "Spice Run Help", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_spiceRunHelpActionPerformed

    private void useDirCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useDirCheckBoxActionPerformed
        // enable use dir field
        boolean b = useDirCheckBox.isSelected();
        useDir.setEnabled(b);
        if (b) {
            useDir.setBackground(Color.white);
        } else {
            useDir.setBackground(spice2.getBackground());
        }
    }//GEN-LAST:event_useDirCheckBoxActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JCheckBox overwriteOutputFile;
    private javax.swing.JPanel spice;
    private javax.swing.JPanel spice1;
    private javax.swing.JPanel spice2;
    private javax.swing.JPanel spice3;
    private javax.swing.JPanel spice4;
    private javax.swing.JPanel spice5;
    private javax.swing.JPanel spice6;
    private javax.swing.JButton spiceBrowseHeaderFile;
    private javax.swing.JButton spiceBrowseTrailerFile;
    private javax.swing.JTextField spiceCapacitance;
    private javax.swing.JScrollPane spiceCell;
    private javax.swing.JRadioButton spiceDeriveModelFromCircuit;
    private javax.swing.JTextField spiceEdgeCapacitance;
    private javax.swing.JComboBox spiceEnginePopup;
    private javax.swing.JCheckBox spiceForceGlobalPwrGnd;
    private javax.swing.JTextField spiceGateLengthSubtraction;
    private javax.swing.ButtonGroup spiceHeader;
    private javax.swing.JTextField spiceHeaderCardExtension;
    private javax.swing.JTextField spiceHeaderCardFile;
    private javax.swing.JRadioButton spiceHeaderCardsFromFile;
    private javax.swing.JRadioButton spiceHeaderCardsWithExtension;
    private javax.swing.JScrollPane spiceLayer;
    private javax.swing.JComboBox spiceLevelPopup;
    private javax.swing.JTextField spiceMinCapacitance;
    private javax.swing.JTextField spiceMinResistance;
    private javax.swing.ButtonGroup spiceModel;
    private javax.swing.JTextField spiceModelCell;
    private javax.swing.JButton spiceModelFileBrowse;
    private javax.swing.JRadioButton spiceNoHeaderCards;
    private javax.swing.JRadioButton spiceNoTrailerCards;
    private javax.swing.JComboBox spiceOutputFormatPopup;
    private javax.swing.JComboBox spicePrimitivesetPopup;
    private javax.swing.JTextField spiceResistance;
    private javax.swing.JButton spiceRunHelp;
    private javax.swing.JComboBox spiceRunPopup;
    private javax.swing.JCheckBox spiceRunProbe;
    private javax.swing.JTextField spiceRunProgram;
    private javax.swing.JTextField spiceRunProgramArgs;
    private javax.swing.JLabel spiceTechnology;
    private javax.swing.ButtonGroup spiceTrailer;
    private javax.swing.JTextField spiceTrailerCardExtension;
    private javax.swing.JTextField spiceTrailerCardFile;
    private javax.swing.JRadioButton spiceTrailerCardsFromFile;
    private javax.swing.JRadioButton spiceTrailerCardsWithExtension;
    private javax.swing.JCheckBox spiceUseCellParameters;
    private javax.swing.JRadioButton spiceUseModelFromFile;
    private javax.swing.JCheckBox spiceUseNodeNames;
    private javax.swing.JCheckBox spiceUseParasitics;
    private javax.swing.JCheckBox spiceWriteTransSizesInLambda;
    private javax.swing.JTextField useDir;
    private javax.swing.JCheckBox useDirCheckBox;
    // End of variables declaration//GEN-END:variables

}
