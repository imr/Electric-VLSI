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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.lib.LibFile;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Class to handle the "Spice" tab of the Preferences dialog.
 */
public class SpiceTab extends PreferencePanel
{
	private enum SpiceOutFormat
	{
		Standard, Raw, RawSmart, RawLT, Epic;
	}

	/** Creates new form SpiceTab */
	public SpiceTab(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// make all text fields select-all when entered
	    EDialog.makeTextFieldSelectAllOnTab(epicText);
	    EDialog.makeTextFieldSelectAllOnTab(spiceRunProgram);
	    EDialog.makeTextFieldSelectAllOnTab(spiceRunProgramArgs);
	    EDialog.makeTextFieldSelectAllOnTab(useDir);
	    EDialog.makeTextFieldSelectAllOnTab(spiceHeaderCardExtension);
	    EDialog.makeTextFieldSelectAllOnTab(spiceHeaderCardFile);
	    EDialog.makeTextFieldSelectAllOnTab(spiceTrailerCardExtension);
	    EDialog.makeTextFieldSelectAllOnTab(spiceTrailerCardFile);
	    EDialog.makeTextFieldSelectAllOnTab(spiceNetworkDelimiter);
	}

	/** return the panel to use for user preferences. */
	public JPanel getUserPreferencesPanel() { return spice; }

	/** return the name of this preferences tab. */
	public String getName() { return "Spice"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Spice tab.
	 */
	public void init()
	{
		// the top section: writing spice decks
		spiceEnginePopup.addItem(Simulation.SpiceEngine.SPICE_ENGINE_2);
		spiceEnginePopup.addItem(Simulation.SpiceEngine.SPICE_ENGINE_3);
		spiceEnginePopup.addItem(Simulation.SpiceEngine.SPICE_ENGINE_H);
		spiceEnginePopup.addItem(Simulation.SpiceEngine.SPICE_ENGINE_P);
		spiceEnginePopup.addItem(Simulation.SpiceEngine.SPICE_ENGINE_G);
		spiceEnginePopup.addItem(Simulation.SpiceEngine.SPICE_ENGINE_S);
		spiceEnginePopup.addItem(Simulation.SpiceEngine.SPICE_ENGINE_H_ASSURA);
		spiceEnginePopup.setSelectedItem(Simulation.getSpiceEngine());

		spiceLevelPopup.addItem("1");
		spiceLevelPopup.addItem("2");
		spiceLevelPopup.addItem("3");
		spiceLevelPopup.setSelectedItem(Simulation.getSpiceLevel());

		spiceResistorShorting.addItem("None");
		spiceResistorShorting.addItem("Normal only");
		spiceResistorShorting.addItem("Normal and Poly");
		spiceResistorShorting.setSelectedIndex(Simulation.getSpiceShortResistors());

		spiceParasitics.addItem(Simulation.SpiceParasitics.SIMPLE);
		spiceParasitics.addItem(Simulation.SpiceParasitics.RC_CONSERVATIVE);
//		spiceParasitics.addItem(Simulation.SpiceParasitics.RC_PROXIMITY);
		spiceParasitics.setSelectedItem(Simulation.getSpiceParasiticsLevel());

		spiceGlobalTreatment.addItem("No special treatment");
		spiceGlobalTreatment.addItem("Use .GLOBAL block");
		spiceGlobalTreatment.addItem("Create .SUBCKT ports");
		spiceGlobalTreatment.setSelectedIndex(Simulation.getSpiceGlobalTreatment().getCode());

		String [] libFiles = LibFile.getSpicePartsLibraries();
		for(int i=0; i<libFiles.length; i++)
			spicePrimitivesetPopup.addItem(libFiles[i]);
		spicePrimitivesetPopup.setSelectedItem(Simulation.getSpicePartsLibrary());

		spiceWritePwrGndSubcircuit.setSelected(Simulation.isSpiceWritePwrGndInTopCell());
        spiceUseCellParameters.setSelected(Simulation.isSpiceUseCellParameters());
		spiceWriteTransSizesInLambda.setSelected(Simulation.isSpiceWriteTransSizeInLambda());
		spiceWriteSubcktTopCell.setSelected(Simulation.isSpiceWriteSubcktTopCell());
		spiceWriteEndStatement.setSelected(Simulation.isSpiceWriteFinalDotEnd());

		// bottom of the top section: header and trailer cards
		String spiceHeaderCardInitial = Simulation.getSpiceHeaderCardInfo();
		boolean noHeader = false;
		if (spiceHeaderCardInitial.startsWith(Spice.SPICE_NOEXTENSION_PREFIX))
		{
			noHeader = true;
			spiceHeaderCardInitial = spiceHeaderCardInitial.substring(Spice.SPICE_NOEXTENSION_PREFIX.length());
		}
		if (spiceHeaderCardInitial.length() == 0) noHeader = true;
		if (spiceHeaderCardInitial.startsWith(Spice.SPICE_EXTENSION_PREFIX))
		{
			spiceHeaderCardsWithExtension.setSelected(true);
			spiceHeaderCardExtension.setText(spiceHeaderCardInitial.substring(Spice.SPICE_EXTENSION_PREFIX.length()));
		} else
		{
			spiceHeaderCardsFromFile.setSelected(true);
			spiceHeaderCardFile.setText(spiceHeaderCardInitial);
		}
		if (noHeader)
			spiceNoHeaderCards.setSelected(true);
		String spiceTrailerCardInitial = Simulation.getSpiceTrailerCardInfo();
		boolean noTrailer = false;
		if (spiceTrailerCardInitial.startsWith(Spice.SPICE_NOEXTENSION_PREFIX))
		{
			noTrailer = true;
			spiceTrailerCardInitial = spiceTrailerCardInitial.substring(Spice.SPICE_NOEXTENSION_PREFIX.length());
		}
		if (spiceTrailerCardInitial.length() == 0) noTrailer = true;
		if (spiceTrailerCardInitial.startsWith(Spice.SPICE_EXTENSION_PREFIX))
		{
			spiceTrailerCardsWithExtension.setSelected(true);
			spiceTrailerCardExtension.setText(spiceTrailerCardInitial.substring(Spice.SPICE_EXTENSION_PREFIX.length()));
		} else
		{
			spiceTrailerCardsFromFile.setSelected(true);
			spiceTrailerCardFile.setText(spiceTrailerCardInitial);
		}
		if (noTrailer)
			spiceNoTrailerCards.setSelected(true);
		spiceBrowseHeaderFile.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { spiceBrowseHeaderFileActionPerformed(); }
		});
		spiceBrowseTrailerFile.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { spiceBrowseTrailerFileActionPerformed(); }
		});

		// the middle section: running Spice
		useDir.setText(Simulation.getSpiceRunDir());
		useDirCheckBox.setSelected(Simulation.getSpiceUseRunDir());
		overwriteOutputFile.setSelected(Simulation.getSpiceOutputOverwrite());
		spiceRunProbe.setSelected(Simulation.getSpiceRunProbe());
		spiceRunProgram.setText(Simulation.getSpiceRunProgram());
		spiceRunProgramArgs.setText(Simulation.getSpiceRunProgramArgs());

		String [] runChoices = Simulation.getSpiceRunChoiceValues();
		for (int i=0; i<runChoices.length; i++) {
			spiceRunPopup.addItem(runChoices[i]);
		}
		spiceRunPopup.setSelectedItem(Simulation.getSpiceRunChoice());
		if (spiceRunPopup.getSelectedIndex() == 0) setSpiceRunOptionsEnabled(false);
		else setSpiceRunOptionsEnabled(true);

		// the bottom section: reading spice output
		spiceOutputFormatPopup.addItem(SpiceOutFormat.Standard);
		spiceOutputFormatPopup.addItem(SpiceOutFormat.Raw);
		spiceOutputFormatPopup.addItem(SpiceOutFormat.RawSmart);
		spiceOutputFormatPopup.addItem(SpiceOutFormat.RawLT);
		spiceOutputFormatPopup.addItem(SpiceOutFormat.Epic);
		spiceOutputFormatPopup.setSelectedItem(SpiceOutFormat.valueOf(Simulation.getSpiceOutputFormat()));

		epicText.setText(String.valueOf(Simulation.getSpiceEpicMemorySize()));
		spiceNetworkDelimiter.setText(Simulation.getSpiceExtractedNetDelimiter());
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
		// the top section: writing spice deck
		Simulation.SpiceEngine engine = (Simulation.SpiceEngine)spiceEnginePopup.getSelectedItem();
		if (Simulation.getSpiceEngine() != engine) Simulation.setSpiceEngine(engine);

		String stringNow = (String)spiceLevelPopup.getSelectedItem();
		if (!Simulation.getSpiceLevel().equals(stringNow)) Simulation.setSpiceLevel(stringNow);

		int sr = spiceResistorShorting.getSelectedIndex();
		if (sr != Simulation.getSpiceShortResistors()) Simulation.setSpiceShortResistors(sr);

		Simulation.SpiceParasitics sp = (Simulation.SpiceParasitics)spiceParasitics.getSelectedItem();
		if (Simulation.getSpiceParasiticsLevel() != sp) Simulation.setSpiceParasiticsLevel(sp);

		Simulation.SpiceGlobal signal = Simulation.SpiceGlobal.find(spiceGlobalTreatment.getSelectedIndex());
		if (Simulation.getSpiceGlobalTreatment() != signal) Simulation.setSpiceGlobalTreatment(signal);

		stringNow = (String)spicePrimitivesetPopup.getSelectedItem();
		if (!Simulation.getSpicePartsLibrary().equals(stringNow)) Simulation.setSpicePartsLibrary(stringNow);

		boolean booleanNow = spiceWritePwrGndSubcircuit.isSelected();
		if (Simulation.isSpiceWritePwrGndInTopCell() != booleanNow) Simulation.setSpiceWritePwrGndInTopCell(booleanNow);

        booleanNow = spiceUseCellParameters.isSelected();
		if (Simulation.isSpiceUseCellParameters() != booleanNow) Simulation.setSpiceUseCellParameters(booleanNow);

		booleanNow = spiceWriteTransSizesInLambda.isSelected();
		if (Simulation.isSpiceWriteTransSizeInLambda() != booleanNow) Simulation.setSpiceWriteTransSizeInLambda(booleanNow);

		booleanNow = spiceWriteSubcktTopCell.isSelected();
		if (Simulation.isSpiceWriteSubcktTopCell() != booleanNow) Simulation.setSpiceWriteSubcktTopCell(booleanNow);

		booleanNow = spiceWriteEndStatement.isSelected();
		if (Simulation.isSpiceWriteFinalDotEnd() != booleanNow) Simulation.setSpiceWriteFinalDotEnd(booleanNow);

		// bottom of the top section: header and trailer cards
		String header = Spice.SPICE_NOEXTENSION_PREFIX;
		if (spiceHeaderCardExtension.getText().length() > 0)
			header += Spice.SPICE_EXTENSION_PREFIX + spiceHeaderCardExtension.getText(); else
				header += spiceHeaderCardFile.getText();
		if (spiceHeaderCardsWithExtension.isSelected())
		{
			header = Spice.SPICE_EXTENSION_PREFIX + spiceHeaderCardExtension.getText();
		} else if (spiceHeaderCardsFromFile.isSelected())
		{
			header = spiceHeaderCardFile.getText();
		}
		if (!Simulation.getSpiceHeaderCardInfo().equals(header)) Simulation.setSpiceHeaderCardInfo(header);

		String trailer = Spice.SPICE_NOEXTENSION_PREFIX;
		if (spiceTrailerCardExtension.getText().length() > 0)
			trailer += Spice.SPICE_EXTENSION_PREFIX + spiceTrailerCardExtension.getText(); else
				 trailer += spiceTrailerCardFile.getText();
		if (spiceTrailerCardsWithExtension.isSelected())
		{
			trailer = Spice.SPICE_EXTENSION_PREFIX + spiceTrailerCardExtension.getText();
		} else if (spiceTrailerCardsFromFile.isSelected())
		{
			trailer = spiceTrailerCardFile.getText();
		}
		if (!Simulation.getSpiceTrailerCardInfo().equals(trailer)) Simulation.setSpiceTrailerCardInfo(trailer);

		// the middle section: spice run options
		stringNow = (String)spiceRunPopup.getSelectedItem();
		if (!Simulation.getSpiceRunChoice().equals(stringNow)) Simulation.setSpiceRunChoice(stringNow);

		stringNow = useDir.getText();
		if (!Simulation.getSpiceRunDir().equals(stringNow)) Simulation.setSpiceRunDir(stringNow);

		booleanNow = useDirCheckBox.isSelected();
		if (Simulation.getSpiceUseRunDir() != booleanNow) Simulation.setSpiceUseRunDir(booleanNow);

		booleanNow = overwriteOutputFile.isSelected();
		if (Simulation.getSpiceOutputOverwrite() != booleanNow) Simulation.setSpiceOutputOverwrite(booleanNow);

		booleanNow = spiceRunProbe.isSelected();
		if (Simulation.getSpiceRunProbe() != booleanNow) Simulation.setSpiceRunProbe(booleanNow);

		stringNow = spiceRunProgram.getText();
		if (!Simulation.getSpiceRunProgram().equals(stringNow)) Simulation.setSpiceRunProgram(stringNow);

		stringNow = spiceRunProgramArgs.getText();
		if (!Simulation.getSpiceRunProgramArgs().equals(stringNow)) Simulation.setSpiceRunProgramArgs(stringNow);

		// the bottom section: reading spice output
		SpiceOutFormat formatVal = (SpiceOutFormat)spiceOutputFormatPopup.getSelectedItem();
		if (!Simulation.getSpiceOutputFormat().equals(formatVal)) Simulation.setSpiceOutputFormat(formatVal.name());

		if (formatVal == SpiceOutFormat.Epic)
			Simulation.setSpiceEpicMemorySize(TextUtils.atoi(epicText.getText()));

		stringNow = spiceNetworkDelimiter.getText();
		if (!Simulation.getSpiceExtractedNetDelimiter().equals(stringNow)) Simulation.setSpiceExtractedNetDelimiter(stringNow);
	}

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset()
	{
		// the top section: writing spice deck
		if (!Simulation.getFactorySpiceEngine().equals(Simulation.getSpiceEngine()))
			Simulation.setSpiceEngine(Simulation.getFactorySpiceEngine());
		if (!Simulation.getFactorySpiceLevel().equals(Simulation.getSpiceLevel()))
			Simulation.setSpiceLevel(Simulation.getFactorySpiceLevel());
		if (Simulation.getFactorySpiceShortResistors() != Simulation.getSpiceShortResistors())
			Simulation.setSpiceShortResistors(Simulation.getFactorySpiceShortResistors());
		if (!Simulation.getFactorySpiceParasiticsLevel().equals(Simulation.getSpiceParasiticsLevel()))
			Simulation.setSpiceParasiticsLevel(Simulation.getFactorySpiceParasiticsLevel());
		if (Simulation.getFactorySpiceGlobalTreatment() != Simulation.getSpiceGlobalTreatment())
			Simulation.setSpiceGlobalTreatment(Simulation.getFactorySpiceGlobalTreatment());
		if (!Simulation.getFactorySpicePartsLibrary().equals(Simulation.getSpicePartsLibrary()))
			Simulation.setSpicePartsLibrary(Simulation.getFactorySpicePartsLibrary());

        if (Simulation.isFactorySpiceWritePwrGndInTopCell() != Simulation.isSpiceWritePwrGndInTopCell())
			Simulation.setSpiceWritePwrGndInTopCell(Simulation.isFactorySpiceWritePwrGndInTopCell());
        if (Simulation.isFactorySpiceUseCellParameters() != Simulation.isSpiceUseCellParameters())
			Simulation.setSpiceUseCellParameters(Simulation.isFactorySpiceUseCellParameters());
		if (Simulation.isFactorySpiceWriteTransSizeInLambda() != Simulation.isSpiceWriteTransSizeInLambda())
			Simulation.setSpiceWriteTransSizeInLambda(Simulation.isFactorySpiceWriteTransSizeInLambda());
		if (Simulation.isFactorySpiceWriteSubcktTopCell() != Simulation.isSpiceWriteSubcktTopCell())
			Simulation.setSpiceWriteSubcktTopCell(Simulation.isFactorySpiceWriteSubcktTopCell());
		if (Simulation.isFactorySpiceWriteFinalDotEnd() != Simulation.isSpiceWriteFinalDotEnd())
			Simulation.setSpiceWriteFinalDotEnd(Simulation.isFactorySpiceWriteFinalDotEnd());

		if (!Simulation.getFactorySpiceHeaderCardInfo().equals(Simulation.getSpiceHeaderCardInfo()))
			Simulation.setSpiceHeaderCardInfo(Simulation.getFactorySpiceHeaderCardInfo());
		if (!Simulation.getFactorySpiceTrailerCardInfo().equals(Simulation.getSpiceTrailerCardInfo()))
			Simulation.setSpiceTrailerCardInfo(Simulation.getFactorySpiceTrailerCardInfo());

		// the middle section: running spice
		if (!Simulation.getFactorySpiceRunChoice().equals(Simulation.getSpiceRunChoice()))
			Simulation.setSpiceRunChoice(Simulation.getFactorySpiceRunChoice());
		if (!Simulation.getFactorySpiceRunProgram().equals(Simulation.getSpiceRunProgram()))
			Simulation.setSpiceRunProgram(Simulation.getFactorySpiceRunProgram());
		if (!Simulation.getFactorySpiceRunProgramArgs().equals(Simulation.getSpiceRunProgramArgs()))
			Simulation.setSpiceRunProgramArgs(Simulation.getFactorySpiceRunProgramArgs());
		if (Simulation.getFactorySpiceUseRunDir() != Simulation.getSpiceUseRunDir())
			Simulation.setSpiceUseRunDir(Simulation.getFactorySpiceUseRunDir());
		if (!Simulation.getFactorySpiceRunDir().equals(Simulation.getSpiceRunDir()))
			Simulation.setSpiceRunDir(Simulation.getFactorySpiceRunDir());
		if (Simulation.getFactorySpiceOutputOverwrite() != Simulation.getSpiceOutputOverwrite())
			Simulation.setSpiceOutputOverwrite(Simulation.getFactorySpiceOutputOverwrite());
		if (Simulation.getFactorySpiceRunProbe() != Simulation.getSpiceRunProbe())
			Simulation.setSpiceRunProbe(Simulation.getFactorySpiceRunProbe());

		// the bottom section: reading spice output
		if (!Simulation.getFactorySpiceOutputFormat().equals(Simulation.getSpiceOutputFormat()))
			Simulation.setSpiceOutputFormat(Simulation.getFactorySpiceOutputFormat());
		if (Simulation.getFactorySpiceEpicMemorySize() != Simulation.getSpiceEpicMemorySize())
			Simulation.setSpiceEpicMemorySize(Simulation.getFactorySpiceEpicMemorySize());
		if (Simulation.getFactorySpiceExtractedNetDelimiter() != Simulation.getSpiceExtractedNetDelimiter())
			Simulation.setSpiceExtractedNetDelimiter(Simulation.getFactorySpiceExtractedNetDelimiter());
	}

	// enable or disable the spice run options
	private void setSpiceRunOptionsEnabled(boolean enabled) {
		useDirCheckBox.setEnabled(enabled);
		overwriteOutputFile.setEnabled(enabled);
		spiceRunProgram.setEnabled(enabled);
		spiceRunProgramArgs.setEnabled(enabled);
		spiceRunHelp.setEnabled(enabled);
		spiceRunProbe.setEnabled(enabled);
		useDir.setEnabled(enabled);
		if (enabled)
		{
			// if no use dir specified, disable text box
			if (!useDirCheckBox.isSelected()) useDir.setEnabled(false);
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        spiceHeader = new javax.swing.ButtonGroup();
        spiceTrailer = new javax.swing.ButtonGroup();
        spice = new javax.swing.JPanel();
        readingOutput = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        spiceOutputFormatPopup = new javax.swing.JComboBox();
        epicLabel = new javax.swing.JLabel();
        epicText = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        spiceNetworkDelimiter = new javax.swing.JTextField();
        execution = new javax.swing.JPanel();
        spiceRunPopup = new javax.swing.JComboBox();
        spiceRunProgram = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        useDirCheckBox = new javax.swing.JCheckBox();
        useDir = new javax.swing.JTextField();
        overwriteOutputFile = new javax.swing.JCheckBox();
        spiceRunHelp = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        spiceRunProgramArgs = new javax.swing.JTextField();
        spiceRunProbe = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        writingSpice = new javax.swing.JPanel();
        upperLeft = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        spiceEnginePopup = new javax.swing.JComboBox();
        spiceLevelPopup = new javax.swing.JComboBox();
        jLabel13 = new javax.swing.JLabel();
        spicePrimitivesetPopup = new javax.swing.JComboBox();
        epicFrame = new javax.swing.JPanel();
        spiceResistorShorting = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        spiceParasitics = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        spiceGlobalTreatment = new javax.swing.JComboBox();
        upperRight = new javax.swing.JPanel();
        spiceWriteEndStatement = new javax.swing.JCheckBox();
        spiceWriteTransSizesInLambda = new javax.swing.JCheckBox();
        spiceUseCellParameters = new javax.swing.JCheckBox();
        spiceWriteSubcktTopCell = new javax.swing.JCheckBox();
        spiceWritePwrGndSubcircuit = new javax.swing.JCheckBox();
        modelCards = new javax.swing.JPanel();
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
        jSeparator5 = new javax.swing.JSeparator();

        setTitle("Spice Preferences");
        setName(""); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        spice.setToolTipText("Options for Spice deck generation");
        spice.setLayout(new java.awt.GridBagLayout());

        readingOutput.setBorder(javax.swing.BorderFactory.createTitledBorder("Reading Spice Output"));
        readingOutput.setLayout(new java.awt.GridBagLayout());

        jLabel10.setText("Output format:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        readingOutput.add(jLabel10, gridBagConstraints);

        spiceOutputFormatPopup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spiceOutputFormatPopupActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 4);
        readingOutput.add(spiceOutputFormatPopup, gridBagConstraints);

        epicLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        epicLabel.setText("Epic reader memory size: ");
        epicLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        readingOutput.add(epicLabel, gridBagConstraints);

        epicText.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 4);
        readingOutput.add(epicText, gridBagConstraints);

        jLabel7.setText("Extracted network delimiter character:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        readingOutput.add(jLabel7, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 4);
        readingOutput.add(spiceNetworkDelimiter, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        spice.add(readingOutput, gridBagConstraints);

        execution.setBorder(javax.swing.BorderFactory.createTitledBorder("Running Spice"));
        execution.setLayout(new java.awt.GridBagLayout());

        spiceRunPopup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spiceRunPopupActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        execution.add(spiceRunPopup, gridBagConstraints);

        spiceRunProgram.setColumns(8);
        spiceRunProgram.setMinimumSize(new java.awt.Dimension(100, 20));
        spiceRunProgram.setPreferredSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        execution.add(spiceRunProgram, gridBagConstraints);

        jLabel17.setText("Run program:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 2, 4, 4);
        execution.add(jLabel17, gridBagConstraints);

        useDirCheckBox.setText("Use dir:");
        useDirCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                useDirCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        execution.add(useDirCheckBox, gridBagConstraints);

        useDir.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        execution.add(useDir, gridBagConstraints);

        overwriteOutputFile.setText("Overwrite existing output file (no prompts)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        execution.add(overwriteOutputFile, gridBagConstraints);

        spiceRunHelp.setText("Help");
        spiceRunHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spiceRunHelpActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        execution.add(spiceRunHelp, gridBagConstraints);

        jLabel3.setText("With args:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        execution.add(jLabel3, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        execution.add(spiceRunProgramArgs, gridBagConstraints);

        spiceRunProbe.setText("Run probe");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        execution.add(spiceRunProbe, gridBagConstraints);

        jLabel2.setText("After writing deck:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        execution.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        spice.add(execution, gridBagConstraints);

        writingSpice.setBorder(javax.swing.BorderFactory.createTitledBorder("Writing Spice Deck"));
        writingSpice.setLayout(new java.awt.GridBagLayout());

        upperLeft.setLayout(new java.awt.GridBagLayout());

        jLabel5.setText("Parasitics:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        upperLeft.add(jLabel5, gridBagConstraints);

        jLabel1.setText("Spice engine:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        upperLeft.add(jLabel1, gridBagConstraints);

        jLabel9.setText("Spice level:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        upperLeft.add(jLabel9, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        upperLeft.add(spiceEnginePopup, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        upperLeft.add(spiceLevelPopup, gridBagConstraints);

        jLabel13.setText("Spice primitive set:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        upperLeft.add(jLabel13, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        upperLeft.add(spicePrimitivesetPopup, gridBagConstraints);

        epicFrame.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        upperLeft.add(epicFrame, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        upperLeft.add(spiceResistorShorting, gridBagConstraints);

        jLabel4.setText("Resistor shorting:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        upperLeft.add(jLabel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        upperLeft.add(spiceParasitics, gridBagConstraints);

        jLabel6.setText("Globals:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        upperLeft.add(jLabel6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        upperLeft.add(spiceGlobalTreatment, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        writingSpice.add(upperLeft, gridBagConstraints);

        upperRight.setLayout(new java.awt.GridBagLayout());

        spiceWriteEndStatement.setText("Write .end statement");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        upperRight.add(spiceWriteEndStatement, gridBagConstraints);

        spiceWriteTransSizesInLambda.setText("Write trans sizes in units");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        upperRight.add(spiceWriteTransSizesInLambda, gridBagConstraints);

        spiceUseCellParameters.setText("Use cell parameters");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        upperRight.add(spiceUseCellParameters, gridBagConstraints);

        spiceWriteSubcktTopCell.setText("Write .subckt for top cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        upperRight.add(spiceWriteSubcktTopCell, gridBagConstraints);

        spiceWritePwrGndSubcircuit.setText("Write VDD/GND in top cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        upperRight.add(spiceWritePwrGndSubcircuit, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        writingSpice.add(upperRight, gridBagConstraints);

        modelCards.setLayout(new java.awt.GridBagLayout());

        spiceHeaderCardExtension.setColumns(5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        modelCards.add(spiceHeaderCardExtension, gridBagConstraints);

        spiceHeader.add(spiceNoHeaderCards);
        spiceNoHeaderCards.setText("No Header cards");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        modelCards.add(spiceNoHeaderCards, gridBagConstraints);

        spiceHeader.add(spiceHeaderCardsWithExtension);
        spiceHeaderCardsWithExtension.setText("Use Header cards from files with extension:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        modelCards.add(spiceHeaderCardsWithExtension, gridBagConstraints);

        spiceHeader.add(spiceHeaderCardsFromFile);
        spiceHeaderCardsFromFile.setText("Use Header cards from file:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        modelCards.add(spiceHeaderCardsFromFile, gridBagConstraints);

        spiceTrailer.add(spiceNoTrailerCards);
        spiceNoTrailerCards.setText("No Trailer cards");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        modelCards.add(spiceNoTrailerCards, gridBagConstraints);

        spiceTrailer.add(spiceTrailerCardsWithExtension);
        spiceTrailerCardsWithExtension.setText("Use Trailer cards from files with extension:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        modelCards.add(spiceTrailerCardsWithExtension, gridBagConstraints);

        spiceTrailer.add(spiceTrailerCardsFromFile);
        spiceTrailerCardsFromFile.setText("Use Trailer cards from File:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        modelCards.add(spiceTrailerCardsFromFile, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        modelCards.add(spiceHeaderCardFile, gridBagConstraints);

        spiceBrowseHeaderFile.setText("Browse");
        spiceBrowseHeaderFile.setMinimumSize(new java.awt.Dimension(78, 20));
        spiceBrowseHeaderFile.setPreferredSize(new java.awt.Dimension(78, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        modelCards.add(spiceBrowseHeaderFile, gridBagConstraints);

        spiceTrailerCardExtension.setColumns(5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        modelCards.add(spiceTrailerCardExtension, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        modelCards.add(spiceTrailerCardFile, gridBagConstraints);

        spiceBrowseTrailerFile.setText("Browse");
        spiceBrowseTrailerFile.setMinimumSize(new java.awt.Dimension(78, 20));
        spiceBrowseTrailerFile.setPreferredSize(new java.awt.Dimension(78, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        modelCards.add(spiceBrowseTrailerFile, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        modelCards.add(jSeparator4, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        modelCards.add(jSeparator5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        writingSpice.add(modelCards, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        spice.add(writingSpice, gridBagConstraints);

        getContentPane().add(spice, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void spiceOutputFormatPopupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spiceOutputFormatPopupActionPerformed
		boolean vis = spiceOutputFormatPopup.getSelectedItem() == SpiceOutFormat.Epic;
		epicLabel.setEnabled(vis);
		epicText.setEnabled(vis);
    }//GEN-LAST:event_spiceOutputFormatPopupActionPerformed

    private void spiceRunPopupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spiceRunPopupActionPerformed
		if (spiceRunPopup.getSelectedIndex() == 0) setSpiceRunOptionsEnabled(false); else
			setSpiceRunOptionsEnabled(true);
    }//GEN-LAST:event_spiceRunPopupActionPerformed

    private void spiceRunHelpActionPerformed(ActionEvent evt) {//GEN-FIRST:event_spiceRunHelpActionPerformed
		String [] message ={"IMPORTANT: This executes a single program with the given arguments.  It does NOT run a command-line command.",
							"For example, 'echo blah > file' will NOT work. Encapsulate it in a script if you want to do such things.",
							"-----------------",
							"The following variables are available to use in the program name and arguments:",
							"   ${WORKING_DIR}:  The current working directory",
							"   ${USE_DIR}:  The path entered in the 'Use Directory' field, if specified (defaults to WORKING_DIR)",
							"   ${FILEPATH}:  The full path of the output file",
							"   ${FILENAME}:  The output file name (with extension)",
							"   ${FILENAME_NO_EXT}:  The output file name (without extension)",
							"Example: Program: \"hspice\".  Arguments: \"${FILEPATH}\"" };
		JOptionPane.showMessageDialog(this, message, "Spice Run Help", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_spiceRunHelpActionPerformed

    private void useDirCheckBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_useDirCheckBoxActionPerformed
		// enable use dir field
		useDir.setEnabled(useDirCheckBox.isSelected());
    }//GEN-LAST:event_useDirCheckBoxActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel epicFrame;
    private javax.swing.JLabel epicLabel;
    private javax.swing.JTextField epicText;
    private javax.swing.JPanel execution;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JPanel modelCards;
    private javax.swing.JCheckBox overwriteOutputFile;
    private javax.swing.JPanel readingOutput;
    private javax.swing.JPanel spice;
    private javax.swing.JButton spiceBrowseHeaderFile;
    private javax.swing.JButton spiceBrowseTrailerFile;
    private javax.swing.JComboBox spiceEnginePopup;
    private javax.swing.JComboBox spiceGlobalTreatment;
    private javax.swing.ButtonGroup spiceHeader;
    private javax.swing.JTextField spiceHeaderCardExtension;
    private javax.swing.JTextField spiceHeaderCardFile;
    private javax.swing.JRadioButton spiceHeaderCardsFromFile;
    private javax.swing.JRadioButton spiceHeaderCardsWithExtension;
    private javax.swing.JComboBox spiceLevelPopup;
    private javax.swing.JTextField spiceNetworkDelimiter;
    private javax.swing.JRadioButton spiceNoHeaderCards;
    private javax.swing.JRadioButton spiceNoTrailerCards;
    private javax.swing.JComboBox spiceOutputFormatPopup;
    private javax.swing.JComboBox spiceParasitics;
    private javax.swing.JComboBox spicePrimitivesetPopup;
    private javax.swing.JComboBox spiceResistorShorting;
    private javax.swing.JButton spiceRunHelp;
    private javax.swing.JComboBox spiceRunPopup;
    private javax.swing.JCheckBox spiceRunProbe;
    private javax.swing.JTextField spiceRunProgram;
    private javax.swing.JTextField spiceRunProgramArgs;
    private javax.swing.ButtonGroup spiceTrailer;
    private javax.swing.JTextField spiceTrailerCardExtension;
    private javax.swing.JTextField spiceTrailerCardFile;
    private javax.swing.JRadioButton spiceTrailerCardsFromFile;
    private javax.swing.JRadioButton spiceTrailerCardsWithExtension;
    private javax.swing.JCheckBox spiceUseCellParameters;
    private javax.swing.JCheckBox spiceWriteEndStatement;
    private javax.swing.JCheckBox spiceWritePwrGndSubcircuit;
    private javax.swing.JCheckBox spiceWriteSubcktTopCell;
    private javax.swing.JCheckBox spiceWriteTransSizesInLambda;
    private javax.swing.JPanel upperLeft;
    private javax.swing.JPanel upperRight;
    private javax.swing.JTextField useDir;
    private javax.swing.JCheckBox useDirCheckBox;
    private javax.swing.JPanel writingSpice;
    // End of variables declaration//GEN-END:variables
}
