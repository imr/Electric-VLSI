/*
 * Options.java
 *
 * Created on December 1, 2003, 11:49 AM
 */

package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.user.Prefs;
import com.sun.electric.tool.simulation.Spice;
import com.sun.electric.tool.logicaleffort.LETool;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.awt.event.ItemEvent;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.text.Document;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;


/**
 *
 * @author  strubin
 */
public class Options extends javax.swing.JDialog
{
	private Technology curTech;

	static class Option
	{
		int type;
		String oldString, newString;
		boolean oldBoolean, newBoolean;
		int oldInt, newInt;
		double oldDouble, newDouble;

		Option() {}

		static Option newStringOption(String oldValue)
		{
			Option option = new Option();
			option.type = 1;
			option.oldString = option.newString = oldValue;
			return option;
		}
		void setStringValue(String newString) { this.newString = newString; }
		String getStringValue() { return newString; }

		static Option newIntOption(int oldValue)
		{
			Option option = new Option();
			option.type = 2;
			option.oldInt = option.newInt = oldValue;
			return option;
		}
		void setIntValue(int newInt) { this.newInt = newInt; }
		int getIntValue() { return newInt; }

		static Option newDoubleOption(double oldValue)
		{
			Option option = new Option();
			option.type = 3;
			option.oldDouble = option.newDouble = oldValue;
			return option;
		}
		void setDoubleValue(double newDouble) { this.newDouble = newDouble; }
		double getDoubleValue() { return newDouble; }

		static Option newBooleanOption(boolean oldValue)
		{
			Option option = new Option();
			option.type = 4;
			option.oldBoolean = option.newBoolean = oldValue;
			return option;
		}
		void setBooleanValue(boolean newBoolean) { this.newBoolean = newBoolean; }
		boolean getBooleanValue() { return newBoolean; }

		boolean isChanged()
		{
			switch (type)
			{
				case 1: return !oldString.equals(newString);
				case 2: return oldInt != newInt;
				case 3: return oldDouble != newDouble;
				case 4: return oldBoolean != newBoolean;
			}
			return false;
		}
	}

	/** Creates new form Options */
	public Options(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// get current information
		curTech = Technology.getCurrent();

		// initialize the SPICE Options panel
		initSpice();

		// initialize the Logical Effort Options panel
		initLogicalEffort();
	}

	//******************************** SPICE ********************************

	private JList spiceLayerList;
	private DefaultListModel spiceLayerListModel;
	private Option spiceEngineOption, spiceLevelOption, spiceOutputFormatOption;
	private Option spiceUseParasiticsOption, spiceUseNodeNamesOption, spiceForceGlobalPwrGndOption;
	private Option spiceUseCellParametersOption, spiceWriteTransSizeInLambdaOption;
	private Option spiceTechMinResistanceOption, spiceTechMinCapacitanceOption;

	private void initSpice()
	{
		spiceEngineOption = Option.newStringOption(Spice.getEngine());
		spiceEnginePopup.addItem("Spice 2");
		spiceEnginePopup.addItem("Spice 3");
		spiceEnginePopup.addItem("HSpice");
		spiceEnginePopup.addItem("PSpice");
		spiceEnginePopup.addItem("Gnucap");
		spiceEnginePopup.addItem("SmartSpice");
		spiceEnginePopup.setSelectedItem(spiceEngineOption.getStringValue());

		spiceLevelOption = Option.newStringOption(Spice.getLevel());
		spiceLevelPopup.addItem("1");
		spiceLevelPopup.addItem("2");
		spiceLevelPopup.addItem("3");
		spiceLevelPopup.setSelectedItem(spiceLevelOption.getStringValue());

		spiceOutputFormatOption = Option.newStringOption(Spice.getOutputFormat());
		spiceOutputFormatPopup.addItem("Standard");
		spiceOutputFormatPopup.addItem("Raw");
		spiceOutputFormatPopup.addItem("Raw/Smart");
		spiceOutputFormatPopup.setSelectedItem(spiceOutputFormatOption.getStringValue());

		spiceUseParasiticsOption = Option.newBooleanOption(Spice.isUseParasitics());
		spiceUseParasitics.setSelected(spiceUseParasiticsOption.getBooleanValue());

		spiceUseNodeNamesOption = Option.newBooleanOption(Spice.isUseNodeNames());
		spiceUseNodeNames.setSelected(spiceUseNodeNamesOption.getBooleanValue());

		spiceForceGlobalPwrGndOption = Option.newBooleanOption(Spice.isForceGlobalPwrGnd());
		spiceForceGlobal.setSelected(spiceForceGlobalPwrGndOption.getBooleanValue());

		spiceUseCellParametersOption = Option.newBooleanOption(Spice.isUseCellParameters());
		spiceUseCellParameters.setSelected(spiceUseCellParametersOption.getBooleanValue());

		spiceWriteTransSizeInLambdaOption = Option.newBooleanOption(Spice.isWriteTransSizeInLambda());
		spiceWriteTransSizeInLambda.setSelected(spiceWriteTransSizeInLambdaOption.getBooleanValue());

		spiceTechnology.setText("For technology " + curTech.getTechName());

		// make an empty list for the layer names
		spiceLayerListModel = new DefaultListModel();
		spiceLayerList = new JList(spiceLayerListModel);
		spiceLayerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		spiceLayer.setViewportView(spiceLayerList);
		spiceLayerList.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt) { spiceLayerListClick(evt); }
		});
		showLayersInTechnology(spiceLayerListModel);
		spiceLayerList.setSelectedIndex(0);
		spiceLayerListClick(null);

		spiceTechMinResistanceOption = Option.newDoubleOption(curTech.getMinResistance());
		spiceMinResistance.setText(Double.toString(spiceTechMinResistanceOption.getDoubleValue()));

		spiceTechMinCapacitanceOption = Option.newDoubleOption(curTech.getMinCapacitance());
		spiceMinCapacitance.setText(Double.toString(spiceTechMinCapacitanceOption.getDoubleValue()));
	}

	private void termSpice()
	{
		if (spiceEngineOption.isChanged()) Spice.setEngine(spiceEngineOption.getStringValue());
		if (spiceLevelOption.isChanged()) Spice.setLevel(spiceLevelOption.getStringValue());
		if (spiceOutputFormatOption.isChanged()) Spice.setOutputFormat(spiceOutputFormatOption.getStringValue());

		if (spiceUseNodeNamesOption.isChanged()) Spice.setUseNodeNames(spiceUseNodeNamesOption.getBooleanValue());
		if (spiceForceGlobalPwrGndOption.isChanged()) Spice.setForceGlobalPwrGnd(spiceForceGlobalPwrGndOption.getBooleanValue());
		if (spiceUseCellParametersOption.isChanged()) Spice.setUseCellParameters(spiceUseCellParametersOption.getBooleanValue());
		if (spiceWriteTransSizeInLambdaOption.isChanged()) Spice.setWriteTransSizeInLambda(spiceWriteTransSizeInLambdaOption.getBooleanValue());
		if (spiceUseParasiticsOption.isChanged()) Spice.setUseParasitics(spiceUseParasiticsOption.getBooleanValue());

		if (spiceTechMinResistanceOption.isChanged()) curTech.setMinResistance(spiceTechMinResistanceOption.getDoubleValue());
		if (spiceTechMinCapacitanceOption.isChanged()) curTech.setMinCapacitance(spiceTechMinCapacitanceOption.getDoubleValue());
	}

	private void spiceLayerListClick(java.awt.event.MouseEvent evt)
	{
		String layerName = (String)spiceLayerList.getSelectedValue();
		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			if (layer.getName().equals(layerName))
			{
				spiceResistance.setText(Double.toString(layer.getSpiceResistance()));
				spiceCapacitance.setText(Double.toString(layer.getSpiceCapacitance()));
				spiceEdgeCapacitance.setText(Double.toString(layer.getSpiceEdgeCapacitance()));
				return;
			}
		}
	}

	private void showLayersInTechnology(DefaultListModel model)
	{
		model.clear();
		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			model.addElement(layer.getName());
		}
	}

//	private void spiceLayerListClick(java.awt.event.MouseEvent evt)
//	{
//		int index = list.getSelectedIndex();
//		Cell cell = (Cell)cellList.get(index);
//		System.out.println("Clicked cell " + cell.describe());
//	}

	//******************************** LOGICAL EFFORT ********************************

	private JList leArcList;
	private DefaultListModel leArcListModel;
	private HashMap leArcOptions;
	private Option leUseLocalSettingsOption, leDisplayIntermediateCapsOption, leHighlightComponentsOption;
	private Option leGlobalFanOutOption, leConvergenceOption, leMaxIterationsOption;
	private Option leGateCapacitanceOption, leDefaultWireCapRatioOption, leDiffToGateCapRatioNMOSOption;
	private Option leDiffToGateCapRatioPMOSOption, leKeeperSizeRatioOption;

	private void initLogicalEffort()
	{
		leUseLocalSettingsOption = Option.newBooleanOption(LETool.isUseLocalSettings());
		leUseLocalSettings.setSelected(leUseLocalSettingsOption.getBooleanValue());

		leDisplayIntermediateCapsOption = Option.newBooleanOption(LETool.isDisplayIntermediateCaps());
		leDisplayIntermediateCaps.setSelected(leDisplayIntermediateCapsOption.getBooleanValue());

		leHighlightComponentsOption = Option.newBooleanOption(LETool.isHighlightComponents());
		leHighlightComponents.setSelected(leHighlightComponentsOption.getBooleanValue());

		leGlobalFanOutOption = Option.newDoubleOption(LETool.getGlobalFanOut());
		leGlobalFanOut.setText(Double.toString(leGlobalFanOutOption.getDoubleValue()));
//		leGlobalFanOut.getDocument().addDocumentListener(new DocumentListener() {
//			private void change(DocumentEvent e, String huh)
//			{
//				String text = leGlobalFanOut.getText();
//System.out.println("Changed '"+huh+"' of fanout.  text now "+text);
//				leGlobalFanOutOption.setDoubleValue(Double.parseDouble(text));
//			}
//			public void changedUpdate(DocumentEvent e) { change(e,"change"); }
//			public void insertUpdate(DocumentEvent e) { change(e,"insert"); }
//			public void removeUpdate(DocumentEvent e) { change(e,"remove"); }
//		});

		leConvergenceOption = Option.newDoubleOption(LETool.getConvergence());
		leConvergence.setText(Double.toString(leConvergenceOption.getDoubleValue()));

		leMaxIterationsOption = Option.newIntOption(LETool.getMaxIterations());
		leMaxIterations.setText(Integer.toString(leMaxIterationsOption.getIntValue()));

		leGateCapacitanceOption = Option.newDoubleOption(LETool.getGateCapacitance());
		leGateCapacitance.setText(Double.toString(leGateCapacitanceOption.getDoubleValue()));

		leDefaultWireCapRatioOption = Option.newDoubleOption(LETool.getDefWireCapRatio());
		leDefaultWireCapRatio.setText(Double.toString(leDefaultWireCapRatioOption.getDoubleValue()));

		leDiffToGateCapRatioNMOSOption = Option.newDoubleOption(LETool.getDiffToGateCapRatioNMOS());
		leDiffToGateCapRatioNMOS.setText(Double.toString(leDiffToGateCapRatioNMOSOption.getDoubleValue()));

		leDiffToGateCapRatioPMOSOption = Option.newDoubleOption(LETool.getDiffToGateCapRatioPMOS());
		leDiffToGateCapRatioPMOS.setText(Double.toString(leDiffToGateCapRatioPMOSOption.getDoubleValue()));

		leKeeperSizeRatioOption = Option.newDoubleOption(LETool.getKeeperSizeRatio());
		leKeeperSizeRatio.setText(Double.toString(leKeeperSizeRatioOption.getDoubleValue()));

		// make an empty list for the layer names
		leArcOptions = new HashMap();
		for(Iterator it = curTech.getArcs(); it.hasNext(); )
		{
			ArcProto arc = (ArcProto)it.next();
			leArcOptions.put(arc, Option.newDoubleOption(LETool.getArcRatio(arc)));
		}
		leArcListModel = new DefaultListModel();
		leArcList = new JList(leArcListModel);
		leArcList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		leArc.setViewportView(leArcList);
//		leArcList.addMouseListener(new java.awt.event.MouseAdapter()
//		{
//			public void mouseClicked(java.awt.event.MouseEvent evt) { leArcListClick(evt); }
//		});
		showArcsInTechnology(leArcListModel);
		leArcList.setSelectedIndex(0);
		leArcListClick(null);

//		leWireRatio.getDocument().addDocumentListener(new DocumentListener() {
//			private void change()
//			{
//				String arcName = (String)leArcList.getSelectedValue();
//				int firstSpace = arcName.indexOf(' ');
//				if (firstSpace > 0) arcName = arcName.substring(0, firstSpace);
//				ArcProto arc = curTech.findArcProto(arcName);
//				Option option = (Option)leArcOptions.get(arc);
//				if (option == null) return;
//System.out.println("Changed Arc "+arc.getProtoName()+" to "+leWireRatio.getText());
//				option.setDoubleValue(Double.parseDouble(leWireRatio.getText()));
//			}
//			public void changedUpdate(DocumentEvent e) { change(); }
//			public void insertUpdate(DocumentEvent e) { change(); }
//			public void removeUpdate(DocumentEvent e) { change(); }
//		});
	}

	private void termLogicalEffort()
	{
		if (leUseLocalSettingsOption.isChanged()) LETool.setUseLocalSettings(leUseLocalSettingsOption.getBooleanValue());
		if (leDisplayIntermediateCapsOption.isChanged()) LETool.setDisplayIntermediateCaps(leDisplayIntermediateCapsOption.getBooleanValue());
		if (leHighlightComponentsOption.isChanged()) LETool.setHighlightComponents(leHighlightComponentsOption.getBooleanValue());

		if (leGlobalFanOutOption.isChanged()) LETool.setGlobalFanOut(leGlobalFanOutOption.getDoubleValue());
		if (leConvergenceOption.isChanged()) LETool.setConvergence(leConvergenceOption.getDoubleValue());
		if (leMaxIterationsOption.isChanged()) LETool.setMaxIterations(leMaxIterationsOption.getIntValue());
		if (leGateCapacitanceOption.isChanged()) LETool.setGateCapacitance(leGateCapacitanceOption.getDoubleValue());
		if (leDefaultWireCapRatioOption.isChanged()) LETool.setDefWireCapRatio(leDefaultWireCapRatioOption.getDoubleValue());
		if (leDiffToGateCapRatioNMOSOption.isChanged()) LETool.setDiffToGateCapRatioNMOS(leDiffToGateCapRatioNMOSOption.getDoubleValue());
		if (leDiffToGateCapRatioPMOSOption.isChanged()) LETool.setDiffToGateCapRatioPMOS(leDiffToGateCapRatioPMOSOption.getDoubleValue());
		if (leKeeperSizeRatioOption.isChanged()) LETool.setKeeperSizeRatio(leKeeperSizeRatioOption.getDoubleValue());
	}

	private void leArcListClick(java.awt.event.MouseEvent evt)
	{
		String arcName = (String)leArcList.getSelectedValue();
		int firstSpace = arcName.indexOf(' ');
		if (firstSpace > 0) arcName = arcName.substring(0, firstSpace);
		ArcProto arc = curTech.findArcProto(arcName);
		Option option = (Option)leArcOptions.get(arc);
		if (option == null) return;
System.out.println("Clicked on Arc "+arc.getProtoName()+" which has value "+option.getDoubleValue());
		leWireRatio.setText(Double.toString(option.getDoubleValue()));
	}

	private void showArcsInTechnology(DefaultListModel model)
	{
		model.clear();
		for(Iterator it = curTech.getArcs(); it.hasNext(); )
		{
			ArcProto arc = (ArcProto)it.next();
			model.addElement(arc.getProtoName() + " (" + Double.toString(LETool.getArcRatio(arc)) + ")");
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

        spiceHeader = new javax.swing.ButtonGroup();
        spiceTrailer = new javax.swing.ButtonGroup();
        spiceModel = new javax.swing.ButtonGroup();
        Bottom = new javax.swing.JPanel();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        tabs = new javax.swing.JTabbedPane();
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
        spiceForceGlobal = new javax.swing.JCheckBox();
        spiceUseCellParameters = new javax.swing.JCheckBox();
        spiceWriteTransSizeInLambda = new javax.swing.JCheckBox();
        spice2 = new javax.swing.JPanel();
        jLabel21 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        spice3 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        SpicePrimitivesetPopup = new javax.swing.JComboBox();
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
        jSeparator2 = new javax.swing.JSeparator();
        spice5 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jRadioButton3 = new javax.swing.JRadioButton();
        jRadioButton4 = new javax.swing.JRadioButton();
        jRadioButton5 = new javax.swing.JRadioButton();
        jRadioButton6 = new javax.swing.JRadioButton();
        jTextField9 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jTextField10 = new javax.swing.JTextField();
        jTextField11 = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        spice6 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jLabel8 = new javax.swing.JLabel();
        jRadioButton7 = new javax.swing.JRadioButton();
        jRadioButton8 = new javax.swing.JRadioButton();
        jButton3 = new javax.swing.JButton();
        jTextField12 = new javax.swing.JTextField();
        logicalEffort = new javax.swing.JPanel();
        leArc = new javax.swing.JScrollPane();
        jLabel4 = new javax.swing.JLabel();
        leDisplayIntermediateCaps = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        leHelp = new javax.swing.JButton();
        jLabel15 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        leUseLocalSettings = new javax.swing.JCheckBox();
        leHighlightComponents = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        leGlobalFanOut = new javax.swing.JTextField();
        leConvergence = new javax.swing.JTextField();
        leMaxIterations = new javax.swing.JTextField();
        leGateCapacitance = new javax.swing.JTextField();
        leDefaultWireCapRatio = new javax.swing.JTextField();
        leDiffToGateCapRatioNMOS = new javax.swing.JTextField();
        leDiffToGateCapRatioPMOS = new javax.swing.JTextField();
        leKeeperSizeRatio = new javax.swing.JTextField();
        leWireRatio = new javax.swing.JTextField();

        getContentPane().setLayout(new java.awt.BorderLayout(0, 10));

        setTitle("Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        Bottom.setLayout(new java.awt.BorderLayout());

        Bottom.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(5, 5, 5, 5)));
        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                CancelButton(evt);
            }
        });

        Bottom.add(cancel, java.awt.BorderLayout.WEST);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                OKButton(evt);
            }
        });

        Bottom.add(ok, java.awt.BorderLayout.EAST);

        getContentPane().add(Bottom, java.awt.BorderLayout.SOUTH);

        spice.setLayout(new java.awt.GridBagLayout());

        spice1.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("SPICE Engine:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        spice1.add(jLabel1, gridBagConstraints);

        jLabel9.setText("SPICE Level:");
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

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        spice1.add(spiceRunPopup, gridBagConstraints);

        spiceEnginePopup.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                SpiceEngineChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice1.add(spiceEnginePopup, gridBagConstraints);

        spiceLevelPopup.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                SpiceLevelChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice1.add(spiceLevelPopup, gridBagConstraints);

        spiceOutputFormatPopup.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                SpiceOutputFormatChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice1.add(spiceOutputFormatPopup, gridBagConstraints);

        spiceUseParasitics.setText("Use Parasitics");
        spiceUseParasitics.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                spiceUseParasiticsChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceUseParasitics, gridBagConstraints);

        spiceUseNodeNames.setText("Use Node Names");
        spiceUseNodeNames.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                spiceUseNodeNamesChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceUseNodeNames, gridBagConstraints);

        spiceForceGlobal.setText("Force Global VDD/GND");
        spiceForceGlobal.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                spiceForceGlobalPwrGndChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceForceGlobal, gridBagConstraints);

        spiceUseCellParameters.setText("Use Cell Parameters");
        spiceUseCellParameters.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                spiceUseCellParametersChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceUseCellParameters, gridBagConstraints);

        spiceWriteTransSizeInLambda.setText("Write Trans Sizes in Lambda");
        spiceWriteTransSizeInLambda.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                spiceWriteTransSizesInLambdaChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceWriteTransSizeInLambda, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice.add(spice1, gridBagConstraints);

        spice2.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        spice2.add(jLabel21, gridBagConstraints);

        jTextField5.setMinimumSize(new java.awt.Dimension(100, 20));
        jTextField5.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        spice2.add(jTextField5, gridBagConstraints);

        jLabel16.setText("Run:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        spice2.add(jLabel16, gridBagConstraints);

        jLabel17.setText("With");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice2.add(jLabel17, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice.add(spice2, gridBagConstraints);

        spice3.setLayout(new java.awt.GridBagLayout());

        jLabel13.setText("SPICE primitive set:");
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
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        spice3.add(SpicePrimitivesetPopup, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        spice.add(spice3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
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
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        spice4.add(spiceLayer, gridBagConstraints);

        jLabel7.setText("Layer:");
        jLabel7.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        spice4.add(jLabel7, gridBagConstraints);

        jLabel2.setText("Edge Cap:");
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

        jLabel12.setText("Capacitance:");
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
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        spice4.add(spiceTechnology, gridBagConstraints);

        spiceResistance.setColumns(8);
        spiceResistance.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                spiceLayerResistanceChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice4.add(spiceResistance, gridBagConstraints);

        spiceCapacitance.setColumns(8);
        spiceCapacitance.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                spiceLayerCapacitanceChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice4.add(spiceCapacitance, gridBagConstraints);

        spiceEdgeCapacitance.setColumns(8);
        spiceEdgeCapacitance.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                spiceLayerEdgeCapacitanceChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice4.add(spiceEdgeCapacitance, gridBagConstraints);

        jLabel18.setText("Min Resistance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice4.add(jLabel18, gridBagConstraints);

        spiceMinResistance.setColumns(8);
        spiceMinResistance.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                spiceMinResistanceChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice4.add(spiceMinResistance, gridBagConstraints);

        jLabel19.setText("Min. Capacitance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        spice4.add(jLabel19, gridBagConstraints);

        spiceMinCapacitance.setColumns(8);
        spiceMinCapacitance.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                spiceMinCapacitanceChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice4.add(spiceMinCapacitance, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.5;
        spice.add(spice4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        spice.add(jSeparator2, gridBagConstraints);

        spice5.setLayout(new java.awt.GridBagLayout());

        jLabel3.setText("Edit Built-in Headers for Technology/Level");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        spice5.add(jLabel3, gridBagConstraints);

        jTextField2.setColumns(5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        spice5.add(jTextField2, gridBagConstraints);

        jRadioButton1.setText("Use Built-in Header Cards");
        spiceHeader.add(jRadioButton1);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(jRadioButton1, gridBagConstraints);

        jRadioButton2.setText("Use Header Cards with extension:");
        spiceHeader.add(jRadioButton2);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(jRadioButton2, gridBagConstraints);

        jRadioButton3.setText("Use Header Cards from File:");
        spiceHeader.add(jRadioButton3);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(jRadioButton3, gridBagConstraints);

        jRadioButton4.setText("No Trailer Cards");
        spiceTrailer.add(jRadioButton4);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(jRadioButton4, gridBagConstraints);

        jRadioButton5.setText("Use Trailer Cards with extension:");
        spiceTrailer.add(jRadioButton5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(jRadioButton5, gridBagConstraints);

        jRadioButton6.setText("Include Trailer from File:");
        spiceTrailer.add(jRadioButton6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice5.add(jRadioButton6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        spice5.add(jTextField9, gridBagConstraints);

        jButton1.setText("Browse");
        jButton1.setMinimumSize(new java.awt.Dimension(78, 20));
        jButton1.setPreferredSize(new java.awt.Dimension(78, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        spice5.add(jButton1, gridBagConstraints);

        jTextField10.setColumns(5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        spice5.add(jTextField10, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        spice5.add(jTextField11, gridBagConstraints);

        jButton2.setText("Browse");
        jButton2.setMinimumSize(new java.awt.Dimension(78, 20));
        jButton2.setPreferredSize(new java.awt.Dimension(78, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        spice5.add(jButton2, gridBagConstraints);

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
        spice.add(spice5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        spice.add(jSeparator3, gridBagConstraints);

        spice6.setLayout(new java.awt.GridBagLayout());

        jScrollPane3.setMinimumSize(new java.awt.Dimension(200, 100));
        jScrollPane3.setPreferredSize(new java.awt.Dimension(200, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        spice6.add(jScrollPane3, gridBagConstraints);

        jLabel8.setText("For Cell");
        jLabel8.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        spice6.add(jLabel8, gridBagConstraints);

        jRadioButton7.setText("Derive Model from Circuitry");
        spiceModel.add(jRadioButton7);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice6.add(jRadioButton7, gridBagConstraints);

        jRadioButton8.setText("Use Model from File:");
        spiceModel.add(jRadioButton8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice6.add(jRadioButton8, gridBagConstraints);

        jButton3.setText("Browse");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        spice6.add(jButton3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        spice6.add(jTextField12, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.5;
        spice.add(spice6, gridBagConstraints);

        tabs.addTab("Spice", spice);

        logicalEffort.setLayout(new java.awt.GridBagLayout());

        leArc.setMinimumSize(new java.awt.Dimension(100, 100));
        leArc.setPreferredSize(new java.awt.Dimension(100, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        logicalEffort.add(leArc, gridBagConstraints);

        jLabel4.setText("Global Fan-Out (step-up):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        logicalEffort.add(jLabel4, gridBagConstraints);

        leDisplayIntermediateCaps.setText("Display intermediate capacitances");
        leDisplayIntermediateCaps.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                leDisplayIntermediateCapsItemStateChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        logicalEffort.add(leDisplayIntermediateCaps, gridBagConstraints);

        jLabel5.setText("Wire ratio for each layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        logicalEffort.add(jLabel5, gridBagConstraints);

        jLabel14.setText("Convergence epsilon:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        logicalEffort.add(jLabel14, gridBagConstraints);

        leHelp.setText("Help");
        leHelp.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                leHelpActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 11;
        logicalEffort.add(leHelp, gridBagConstraints);

        jLabel15.setText("Maximum number of iterations:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        logicalEffort.add(jLabel15, gridBagConstraints);

        jLabel20.setText("Gate capacitance (fF/Lambda):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        logicalEffort.add(jLabel20, gridBagConstraints);

        jLabel22.setText("Default wire cap ratio (Cwire / Cgate):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        logicalEffort.add(jLabel22, gridBagConstraints);

        jLabel23.setText("Diffusion to gate cap ratio (NMOS):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        logicalEffort.add(jLabel23, gridBagConstraints);

        jLabel24.setText("Diffusion to gate cap ratio (PMOS):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        logicalEffort.add(jLabel24, gridBagConstraints);

        jLabel25.setText("Keeper size ratio (keeper size / driver size):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        logicalEffort.add(jLabel25, gridBagConstraints);

        leUseLocalSettings.setText("Use Local (cell) LE Settings");
        leUseLocalSettings.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                leUseLocalSettingsItemStateChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        logicalEffort.add(leUseLocalSettings, gridBagConstraints);

        leHighlightComponents.setText("Highlight components");
        leHighlightComponents.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                leHighlightComponentsItemStateChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        logicalEffort.add(leHighlightComponents, gridBagConstraints);

        jLabel6.setText("Wire ratio:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        logicalEffort.add(jLabel6, gridBagConstraints);

        leGlobalFanOut.setColumns(8);
        leGlobalFanOut.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                leGlobalFanOutActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        logicalEffort.add(leGlobalFanOut, gridBagConstraints);

        leConvergence.setColumns(8);
        leConvergence.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                leConvergenceActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        logicalEffort.add(leConvergence, gridBagConstraints);

        leMaxIterations.setColumns(8);
        leMaxIterations.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                leMaxIterationsActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        logicalEffort.add(leMaxIterations, gridBagConstraints);

        leGateCapacitance.setColumns(8);
        leGateCapacitance.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                leGateCapacitanceActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        logicalEffort.add(leGateCapacitance, gridBagConstraints);

        leDefaultWireCapRatio.setColumns(8);
        leDefaultWireCapRatio.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                leDefaultWireCapRatioActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        logicalEffort.add(leDefaultWireCapRatio, gridBagConstraints);

        leDiffToGateCapRatioNMOS.setColumns(8);
        leDiffToGateCapRatioNMOS.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                leDiffToGateCapRatioNMOSActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        logicalEffort.add(leDiffToGateCapRatioNMOS, gridBagConstraints);

        leDiffToGateCapRatioPMOS.setColumns(8);
        leDiffToGateCapRatioPMOS.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                leDiffToGateCapRatioPMOSActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        logicalEffort.add(leDiffToGateCapRatioPMOS, gridBagConstraints);

        leKeeperSizeRatio.setColumns(8);
        leKeeperSizeRatio.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                leKeeperSizeRatioActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        logicalEffort.add(leKeeperSizeRatio, gridBagConstraints);

        leWireRatio.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        logicalEffort.add(leWireRatio, gridBagConstraints);

        tabs.addTab("Logical Effort", logicalEffort);

        getContentPane().add(tabs, java.awt.BorderLayout.CENTER);

        pack();
    }//GEN-END:initComponents

	private void leKeeperSizeRatioActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_leKeeperSizeRatioActionPerformed
	{//GEN-HEADEREND:event_leKeeperSizeRatioActionPerformed
		leKeeperSizeRatioOption.setDoubleValue(Double.parseDouble(leKeeperSizeRatio.getText()));
	}//GEN-LAST:event_leKeeperSizeRatioActionPerformed

	private void leDiffToGateCapRatioPMOSActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_leDiffToGateCapRatioPMOSActionPerformed
	{//GEN-HEADEREND:event_leDiffToGateCapRatioPMOSActionPerformed
		leDiffToGateCapRatioPMOSOption.setDoubleValue(Double.parseDouble(leDiffToGateCapRatioPMOS.getText()));
	}//GEN-LAST:event_leDiffToGateCapRatioPMOSActionPerformed

	private void leDiffToGateCapRatioNMOSActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_leDiffToGateCapRatioNMOSActionPerformed
	{//GEN-HEADEREND:event_leDiffToGateCapRatioNMOSActionPerformed
		leDiffToGateCapRatioNMOSOption.setDoubleValue(Double.parseDouble(leDiffToGateCapRatioNMOS.getText()));
	}//GEN-LAST:event_leDiffToGateCapRatioNMOSActionPerformed

	private void leDefaultWireCapRatioActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_leDefaultWireCapRatioActionPerformed
	{//GEN-HEADEREND:event_leDefaultWireCapRatioActionPerformed
		leDefaultWireCapRatioOption.setDoubleValue(Double.parseDouble(leDefaultWireCapRatio.getText()));
	}//GEN-LAST:event_leDefaultWireCapRatioActionPerformed

	private void leGateCapacitanceActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_leGateCapacitanceActionPerformed
	{//GEN-HEADEREND:event_leGateCapacitanceActionPerformed
		leGateCapacitanceOption.setDoubleValue(Double.parseDouble(leGateCapacitance.getText()));
	}//GEN-LAST:event_leGateCapacitanceActionPerformed

	private void leMaxIterationsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_leMaxIterationsActionPerformed
	{//GEN-HEADEREND:event_leMaxIterationsActionPerformed
		leMaxIterationsOption.setIntValue(Integer.parseInt(leMaxIterations.getText()));
	}//GEN-LAST:event_leMaxIterationsActionPerformed

	private void leConvergenceActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_leConvergenceActionPerformed
	{//GEN-HEADEREND:event_leConvergenceActionPerformed
		leConvergenceOption.setDoubleValue(Double.parseDouble(leConvergence.getText()));
	}//GEN-LAST:event_leConvergenceActionPerformed

	private void leGlobalFanOutActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_leGlobalFanOutActionPerformed
	{//GEN-HEADEREND:event_leGlobalFanOutActionPerformed
		leGlobalFanOutOption.setDoubleValue(Double.parseDouble(leGlobalFanOut.getText()));
	}//GEN-LAST:event_leGlobalFanOutActionPerformed

	private void leHelpActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_leHelpActionPerformed
	{//GEN-HEADEREND:event_leHelpActionPerformed
		System.out.println("No help yet");
	}//GEN-LAST:event_leHelpActionPerformed

	private void leDisplayIntermediateCapsItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_leDisplayIntermediateCapsItemStateChanged
	{//GEN-HEADEREND:event_leDisplayIntermediateCapsItemStateChanged
		leDisplayIntermediateCapsOption.setBooleanValue(evt.getStateChange() == ItemEvent.SELECTED);
	}//GEN-LAST:event_leDisplayIntermediateCapsItemStateChanged

	private void leHighlightComponentsItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_leHighlightComponentsItemStateChanged
	{//GEN-HEADEREND:event_leHighlightComponentsItemStateChanged
		leHighlightComponentsOption.setBooleanValue(evt.getStateChange() == ItemEvent.SELECTED);
	}//GEN-LAST:event_leHighlightComponentsItemStateChanged

	private void leUseLocalSettingsItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_leUseLocalSettingsItemStateChanged
	{//GEN-HEADEREND:event_leUseLocalSettingsItemStateChanged
		leUseLocalSettingsOption.setBooleanValue(evt.getStateChange() == ItemEvent.SELECTED);
	}//GEN-LAST:event_leUseLocalSettingsItemStateChanged

	private void spiceUseNodeNamesChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_spiceUseNodeNamesChanged
	{//GEN-HEADEREND:event_spiceUseNodeNamesChanged
		spiceUseNodeNamesOption.setBooleanValue(evt.getStateChange() == ItemEvent.SELECTED);
	}//GEN-LAST:event_spiceUseNodeNamesChanged

	private void spiceForceGlobalPwrGndChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_spiceForceGlobalPwrGndChanged
	{//GEN-HEADEREND:event_spiceForceGlobalPwrGndChanged
		spiceForceGlobalPwrGndOption.setBooleanValue(evt.getStateChange() == ItemEvent.SELECTED);
	}//GEN-LAST:event_spiceForceGlobalPwrGndChanged

	private void spiceUseCellParametersChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_spiceUseCellParametersChanged
	{//GEN-HEADEREND:event_spiceUseCellParametersChanged
		spiceUseCellParametersOption.setBooleanValue(evt.getStateChange() == ItemEvent.SELECTED);
	}//GEN-LAST:event_spiceUseCellParametersChanged

	private void spiceWriteTransSizesInLambdaChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_spiceWriteTransSizesInLambdaChanged
	{//GEN-HEADEREND:event_spiceWriteTransSizesInLambdaChanged
		spiceWriteTransSizeInLambdaOption.setBooleanValue(evt.getStateChange() == ItemEvent.SELECTED);
	}//GEN-LAST:event_spiceWriteTransSizesInLambdaChanged

	private void spiceUseParasiticsChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_spiceUseParasiticsChanged
	{//GEN-HEADEREND:event_spiceUseParasiticsChanged
		spiceUseParasiticsOption.setBooleanValue(evt.getStateChange() == ItemEvent.SELECTED);
	}//GEN-LAST:event_spiceUseParasiticsChanged

	private void spiceLayerEdgeCapacitanceChanged(java.awt.event.ActionEvent evt)//GEN-FIRST:event_spiceLayerEdgeCapacitanceChanged
	{//GEN-HEADEREND:event_spiceLayerEdgeCapacitanceChanged
		// Add your handling code here:
	}//GEN-LAST:event_spiceLayerEdgeCapacitanceChanged

	private void spiceLayerCapacitanceChanged(java.awt.event.ActionEvent evt)//GEN-FIRST:event_spiceLayerCapacitanceChanged
	{//GEN-HEADEREND:event_spiceLayerCapacitanceChanged
		// Add your handling code here:
	}//GEN-LAST:event_spiceLayerCapacitanceChanged

	private void spiceLayerResistanceChanged(java.awt.event.ActionEvent evt)//GEN-FIRST:event_spiceLayerResistanceChanged
	{//GEN-HEADEREND:event_spiceLayerResistanceChanged
		// Add your handling code here:
	}//GEN-LAST:event_spiceLayerResistanceChanged

	private void spiceMinCapacitanceChanged(java.awt.event.ActionEvent evt)//GEN-FIRST:event_spiceMinCapacitanceChanged
	{//GEN-HEADEREND:event_spiceMinCapacitanceChanged
		spiceTechMinCapacitanceOption.setDoubleValue(Double.parseDouble(spiceMinCapacitance.getText()));
	}//GEN-LAST:event_spiceMinCapacitanceChanged

	private void spiceMinResistanceChanged(java.awt.event.ActionEvent evt)//GEN-FIRST:event_spiceMinResistanceChanged
	{//GEN-HEADEREND:event_spiceMinResistanceChanged
		spiceTechMinResistanceOption.setDoubleValue(Double.parseDouble(spiceMinResistance.getText()));
	}//GEN-LAST:event_spiceMinResistanceChanged

	private void CancelButton(java.awt.event.ActionEvent evt)//GEN-FIRST:event_CancelButton
	{//GEN-HEADEREND:event_CancelButton
		setVisible(false);
		dispose();
	}//GEN-LAST:event_CancelButton

	private void OKButton(java.awt.event.ActionEvent evt)//GEN-FIRST:event_OKButton
	{//GEN-HEADEREND:event_OKButton
		termSpice();
		termLogicalEffort();

		setVisible(false);
		dispose();
	}//GEN-LAST:event_OKButton

	private void SpiceLevelChanged(java.awt.event.ActionEvent evt)//GEN-FIRST:event_SpiceLevelChanged
	{//GEN-HEADEREND:event_SpiceLevelChanged
		JComboBox cb = (JComboBox)evt.getSource();
		spiceLevelOption.setStringValue((String)cb.getSelectedItem());
	}//GEN-LAST:event_SpiceLevelChanged

	private void SpiceEngineChanged(java.awt.event.ActionEvent evt)//GEN-FIRST:event_SpiceEngineChanged
	{//GEN-HEADEREND:event_SpiceEngineChanged
		JComboBox cb = (JComboBox)evt.getSource();
		spiceEngineOption.setStringValue((String)cb.getSelectedItem());
	}//GEN-LAST:event_SpiceEngineChanged

	private void SpiceOutputFormatChanged(java.awt.event.ActionEvent evt)//GEN-FIRST:event_SpiceOutputFormatChanged
	{//GEN-HEADEREND:event_SpiceOutputFormatChanged
		JComboBox cb = (JComboBox)evt.getSource();
		spiceOutputFormatOption.setStringValue((String)cb.getSelectedItem());
	}//GEN-LAST:event_SpiceOutputFormatChanged
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Bottom;
    private javax.swing.JComboBox SpicePrimitivesetPopup;
    private javax.swing.JButton cancel;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
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
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JRadioButton jRadioButton3;
    private javax.swing.JRadioButton jRadioButton4;
    private javax.swing.JRadioButton jRadioButton5;
    private javax.swing.JRadioButton jRadioButton6;
    private javax.swing.JRadioButton jRadioButton7;
    private javax.swing.JRadioButton jRadioButton8;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JTextField jTextField10;
    private javax.swing.JTextField jTextField11;
    private javax.swing.JTextField jTextField12;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField9;
    private javax.swing.JScrollPane leArc;
    private javax.swing.JTextField leConvergence;
    private javax.swing.JTextField leDefaultWireCapRatio;
    private javax.swing.JTextField leDiffToGateCapRatioNMOS;
    private javax.swing.JTextField leDiffToGateCapRatioPMOS;
    private javax.swing.JCheckBox leDisplayIntermediateCaps;
    private javax.swing.JTextField leGateCapacitance;
    private javax.swing.JTextField leGlobalFanOut;
    private javax.swing.JButton leHelp;
    private javax.swing.JCheckBox leHighlightComponents;
    private javax.swing.JTextField leKeeperSizeRatio;
    private javax.swing.JTextField leMaxIterations;
    private javax.swing.JCheckBox leUseLocalSettings;
    private javax.swing.JTextField leWireRatio;
    private javax.swing.JPanel logicalEffort;
    private javax.swing.JButton ok;
    private javax.swing.JPanel spice;
    private javax.swing.JPanel spice1;
    private javax.swing.JPanel spice2;
    private javax.swing.JPanel spice3;
    private javax.swing.JPanel spice4;
    private javax.swing.JPanel spice5;
    private javax.swing.JPanel spice6;
    private javax.swing.JTextField spiceCapacitance;
    private javax.swing.JTextField spiceEdgeCapacitance;
    private javax.swing.JComboBox spiceEnginePopup;
    private javax.swing.JCheckBox spiceForceGlobal;
    private javax.swing.ButtonGroup spiceHeader;
    private javax.swing.JScrollPane spiceLayer;
    private javax.swing.JComboBox spiceLevelPopup;
    private javax.swing.JTextField spiceMinCapacitance;
    private javax.swing.JTextField spiceMinResistance;
    private javax.swing.ButtonGroup spiceModel;
    private javax.swing.JComboBox spiceOutputFormatPopup;
    private javax.swing.JTextField spiceResistance;
    private javax.swing.JComboBox spiceRunPopup;
    private javax.swing.JLabel spiceTechnology;
    private javax.swing.ButtonGroup spiceTrailer;
    private javax.swing.JCheckBox spiceUseCellParameters;
    private javax.swing.JCheckBox spiceUseNodeNames;
    private javax.swing.JCheckBox spiceUseParasitics;
    private javax.swing.JCheckBox spiceWriteTransSizeInLambda;
    private javax.swing.JTabbedPane tabs;
    // End of variables declaration//GEN-END:variables
	
}
