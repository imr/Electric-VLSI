/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ToolOptions.java
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

import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.Prefs;
import com.sun.electric.tool.simulation.Spice;
import com.sun.electric.tool.logicaleffort.LETool;
import com.sun.electric.tool.user.ui.DialogOpenFile;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.awt.event.ItemEvent;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.text.Document;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;


/**
 * Class to handle the "Tool Options" dialog.
 */
public class ToolOptions extends javax.swing.JDialog
{
	private Technology curTech;
	private Library curLib;

	static class Option
	{
		int type;
		String oldString, newString;
		boolean oldBoolean, newBoolean;
		int oldInt, newInt;
		double oldDouble, newDouble;

		static final int ISSTRING = 1;
		static final int ISINTEGER = 2;
		static final int ISDOUBLE = 3;
		static final int ISBOOLEAN = 4;

		Option() {}

		static Option newStringOption(String oldValue)
		{
			Option option = new Option();
			option.type = ISSTRING;
			option.oldString = new String(oldValue);
			option.newString = new String(oldValue);
			return option;
		}
		void setStringValue(String newString) { this.newString = new String(newString); }
		String getStringValue() { return newString; }

		static Option newIntOption(int oldValue)
		{
			Option option = new Option();
			option.type = ISINTEGER;
			option.oldInt = option.newInt = oldValue;
			return option;
		}
		void setIntValue(int newInt) { this.newInt = newInt; }
		int getIntValue() { return newInt; }

		static Option newDoubleOption(double oldValue)
		{
			Option option = new Option();
			option.type = ISDOUBLE;
			option.oldDouble = option.newDouble = oldValue;
			return option;
		}
		void setDoubleValue(double newDouble) { this.newDouble = newDouble; }
		double getDoubleValue() { return newDouble; }

		static Option newBooleanOption(boolean oldValue)
		{
			Option option = new Option();
			option.type = ISBOOLEAN;
			option.oldBoolean = option.newBoolean = oldValue;
			return option;
		}
		void setBooleanValue(boolean newBoolean) { this.newBoolean = newBoolean; }
		boolean getBooleanValue() { return newBoolean; }

		int getType() { return type; }

		boolean isChanged()
		{
			switch (type)
			{
				case ISSTRING:  return !oldString.equals(newString);
				case ISINTEGER: return oldInt != newInt;
				case ISDOUBLE:  return oldDouble != newDouble;
				case ISBOOLEAN: return oldBoolean != newBoolean;
			}
			return false;
		}
	}

	/** Creates new form ToolOptions */
	public ToolOptions(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();

		// get current information
		curTech = Technology.getCurrent();
		curLib = Library.getCurrent();

		// factory reset not working yet
		factoryReset.setEnabled(false);

		// initialize the SPICE Options panel
		initSpice();

		// initialize the Logical Effort Options panel
		initLogicalEffort();

		// initialize the Network Options panel
		initNetwork();
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

	//******************************** SPICE ********************************

	private JList spiceLayerList, spiceCellList;
	private DefaultListModel spiceLayerListModel, spiceCellListModel;
	private String spiceEngineInitial, spiceLevelInitial, spiceOutputFormatInitial;
	private boolean spiceUseParasiticsInitial, spiceUseNodeNamesInitial, spiceForceGlobalPwrGndInitial;
	private boolean spiceUseCellParametersInitial, spiceWriteTransSizesInLambdaInitial;
	private double spiceTechMinResistanceInitial, spiceTechMinCapacitanceInitial;
	private String spiceHeaderCardInitial, spiceTrailerCardInitial;
	private HashMap spiceLayerResistanceOptions;
	private HashMap spiceLayerCapacitanceOptions;
	private HashMap spiceLayerEdgeCapacitanceOptions;
	private HashMap spiceCellModelOptions;

	private void initSpice()
	{
		// the top section: general controls
		spiceEngineInitial = Spice.getEngine();
		spiceEnginePopup.addItem("Spice 2");
		spiceEnginePopup.addItem("Spice 3");
		spiceEnginePopup.addItem("HSpice");
		spiceEnginePopup.addItem("PSpice");
		spiceEnginePopup.addItem("Gnucap");
		spiceEnginePopup.addItem("SmartSpice");
		spiceEnginePopup.setSelectedItem(spiceEngineInitial);

		spiceLevelInitial = Spice.getLevel();
		spiceLevelPopup.addItem("1");
		spiceLevelPopup.addItem("2");
		spiceLevelPopup.addItem("3");
		spiceLevelPopup.setSelectedItem(spiceLevelInitial);

		spiceOutputFormatInitial = Spice.getOutputFormat();
		spiceOutputFormatPopup.addItem("Standard");
		spiceOutputFormatPopup.addItem("Raw");
		spiceOutputFormatPopup.addItem("Raw/Smart");
		spiceOutputFormatPopup.setSelectedItem(spiceOutputFormatInitial);

		spiceRunPopup.addItem("Don't Run SPICE");
		spiceRunPopup.addItem("Run SPICE");
		spiceRunPopup.addItem("Run SPICE Quietly");
		spiceRunPopup.addItem("Run SPICE, Read Output");
		spiceRunPopup.addItem("Run SPICE Quietly, Read Output");
		spiceRunPopup.setEnabled(false);

		spiceUseParasiticsInitial = Spice.isUseParasitics();
		spiceUseParasitics.setSelected(spiceUseParasiticsInitial);

		spiceUseNodeNamesInitial = Spice.isUseNodeNames();
		spiceUseNodeNames.setSelected(spiceUseNodeNamesInitial);

		spiceForceGlobalPwrGndInitial = Spice.isForceGlobalPwrGnd();
		spiceForceGlobalPwrGnd.setSelected(spiceForceGlobalPwrGndInitial);

		spiceUseCellParametersInitial = Spice.isUseCellParameters();
		spiceUseCellParameters.setSelected(spiceUseCellParametersInitial);

		spiceWriteTransSizesInLambdaInitial = Spice.isWriteTransSizeInLambda();
		spiceWriteTransSizesInLambda.setSelected(spiceWriteTransSizesInLambdaInitial);

		spiceRunParameters.setEnabled(false);
		spicePrimitivesetPopup.addItem("spiceparts");
		spicePrimitivesetPopup.setEnabled(false);

		// the next section: parasitic values
		spiceTechnology.setText("For technology " + curTech.getTechName());

		spiceLayerResistanceOptions = new HashMap();
		spiceLayerCapacitanceOptions = new HashMap();
		spiceLayerEdgeCapacitanceOptions = new HashMap();
		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			spiceLayerResistanceOptions.put(layer, Option.newDoubleOption(layer.getResistance()));
			spiceLayerCapacitanceOptions.put(layer, Option.newDoubleOption(layer.getCapacitance()));
			spiceLayerEdgeCapacitanceOptions.put(layer, Option.newDoubleOption(layer.getEdgeCapacitance()));
		}
		spiceLayerListModel = new DefaultListModel();
		spiceLayerList = new JList(spiceLayerListModel);
		spiceLayerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		spiceLayer.setViewportView(spiceLayerList);
		spiceLayerList.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt) { spiceLayerListClick(); }
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

		// the next section: header and trailer cards
		spiceHeaderCardInitial = Spice.getHeaderCardInfo();
		if (spiceHeaderCardInitial.length() == 0) spiceNoHeaderCards.setSelected(true); else
		{
			if (spiceHeaderCardInitial.startsWith(":::::"))
			{
				spiceHeaderCardsWithExtension.setSelected(true);
				spiceHeaderCardExtension.setText(spiceHeaderCardInitial.substring(5));
			} else
			{
				spiceHeaderCardsFromFile.setSelected(true);
				spiceHeaderCardFile.setText(spiceHeaderCardInitial);
			}
		}
		spiceTrailerCardInitial = Spice.getTrailerCardInfo();
		if (spiceTrailerCardInitial.length() == 0) spiceNoTrailerCards.setSelected(true); else
		{
			if (spiceTrailerCardInitial.startsWith(":::::"))
			{
				spiceTrailerCardsWithExtension.setSelected(true);
				spiceTrailerCardExtension.setText(spiceTrailerCardInitial.substring(5));
			} else
			{
				spiceTrailerCardsFromFile.setSelected(true);
				spiceTrailerCardFile.setText(spiceTrailerCardInitial);
			}
		}

		// the last section has cell overrides
		spiceCellModelOptions = new HashMap();
		for(Iterator it = curLib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			spiceCellModelOptions.put(cell, Option.newStringOption(""));
		}
		spiceCellListModel = new DefaultListModel();
		spiceCellList = new JList(spiceCellListModel);
		spiceCellList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		spiceCell.setViewportView(spiceCellList);
		spiceCellList.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt) { spiceCellListClick(); }
		});
		for(Iterator it = curLib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			spiceCellListModel.addElement(cell.noLibDescribe());
		}
		spiceCellList.setSelectedIndex(0);
		spiceCellListClick();
//		spiceCell.getDocument().addDocumentListener(new CellDocumentListener(spiceCellModelOptions, spiceCellList, curLib));
	}

	private void spiceCellListClick()
	{
		String cellName = (String)spiceCellList.getSelectedValue();
		Cell cell = curLib.findNodeProto(cellName);
		if (cell != null)
		{
			Option option = (Option)spiceCellModelOptions.get(cell);
			spiceModelCell.setText(option.getStringValue());
		}
	}

	private void termSpice()
	{
		// the top section: general controls
		String stringNow = (String)spiceEnginePopup.getSelectedItem();
		if (!spiceEngineInitial.equals(stringNow)) Spice.setEngine(stringNow);

		stringNow = (String)spiceLevelPopup.getSelectedItem();
		if (!spiceLevelInitial.equals(stringNow)) Spice.setLevel(stringNow);

		stringNow = (String)spiceOutputFormatPopup.getSelectedItem();
		if (!spiceOutputFormatInitial.equals(stringNow)) Spice.setOutputFormat(stringNow);

		boolean booleanNow = spiceUseNodeNames.isSelected();
		if (spiceUseNodeNamesInitial != booleanNow) Spice.setUseNodeNames(booleanNow);

		booleanNow = spiceForceGlobalPwrGnd.isSelected();
		if (spiceForceGlobalPwrGndInitial != booleanNow) Spice.setForceGlobalPwrGnd(booleanNow);

		booleanNow = spiceUseCellParameters.isSelected();
		if (spiceUseCellParametersInitial != booleanNow) Spice.setUseCellParameters(booleanNow);

		booleanNow = spiceWriteTransSizesInLambda.isSelected();
		if (spiceWriteTransSizesInLambdaInitial != booleanNow) Spice.setWriteTransSizeInLambda(booleanNow);

		booleanNow = spiceUseParasitics.isSelected();
		if (spiceUseParasiticsInitial != booleanNow) Spice.setUseParasitics(booleanNow);

		Spice.flushOptions();

		// the next section: parasitic values
		double doubleNow = EMath.atof(spiceMinResistance.getText());
		if (spiceTechMinResistanceInitial != doubleNow) curTech.setMinResistance(doubleNow);
		doubleNow = EMath.atof(spiceMinCapacitance.getText());
		if (spiceTechMinCapacitanceInitial != doubleNow) curTech.setMinCapacitance(doubleNow);

		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			Option resistanceOption = (Option)spiceLayerResistanceOptions.get(layer);
			if (resistanceOption != null && resistanceOption.isChanged())
				layer.setResistance(resistanceOption.getDoubleValue());
			Option capacitanceOption = (Option)spiceLayerCapacitanceOptions.get(layer);
			if (capacitanceOption != null && capacitanceOption.isChanged())
				layer.setCapacitance(capacitanceOption.getDoubleValue());
			Option edgeCapacitanceOption = (Option)spiceLayerEdgeCapacitanceOptions.get(layer);
			if (edgeCapacitanceOption != null && edgeCapacitanceOption.isChanged())
				layer.setEdgeCapacitance(edgeCapacitanceOption.getDoubleValue());
		}

		// the next section: header and trailer cards
		String header = "";
		if (spiceHeaderCardsWithExtension.isSelected())
		{
			header = ":::::" + spiceHeaderCardExtension.getText();
		} else if (spiceHeaderCardsFromFile.isSelected())
		{
			header = spiceHeaderCardFile.getText();
		}
		if (!spiceTrailerCardInitial.equals(header)) Spice.setHeaderCardInfo(header);

		String trailer = "";
		if (spiceTrailerCardsWithExtension.isSelected())
		{
			trailer = ":::::" + spiceTrailerCardExtension.getText();
		} else if (spiceTrailerCardsFromFile.isSelected())
		{
			trailer = spiceTrailerCardFile.getText();
		}
		if (spiceTrailerCardInitial.equals(trailer)) Spice.setTrailerCardInfo(trailer);
	}

	private void spiceLayerListClick()
	{
		String layerName = (String)spiceLayerList.getSelectedValue();
		Layer layer = curTech.findLayer(layerName);
		if (layer != null)
		{
			Option resistanceOption = (Option)spiceLayerResistanceOptions.get(layer);
			spiceResistance.setText(Double.toString(resistanceOption.getDoubleValue()));
			Option capacitanceOption = (Option)spiceLayerCapacitanceOptions.get(layer);
			spiceCapacitance.setText(Double.toString(capacitanceOption.getDoubleValue()));
			Option edgeCapacitanceOption = (Option)spiceLayerEdgeCapacitanceOptions.get(layer);
			spiceEdgeCapacitance.setText(Double.toString(edgeCapacitanceOption.getDoubleValue()));
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
			Option option = (Option)optionMap.get(layer);
			double v = EMath.atof(text);

			// update the option
			option.setDoubleValue(v);
		}

		public void changedUpdate(DocumentEvent e) { change(e); }
		public void insertUpdate(DocumentEvent e) { change(e); }
		public void removeUpdate(DocumentEvent e) { change(e); }
	}

	//******************************** LOGICAL EFFORT ********************************

	private JList leArcList;
	private DefaultListModel leArcListModel;
	private HashMap leArcOptions;
	private boolean leUseLocalSettingsInitial, leDisplayIntermediateCapsInitial, leHighlightComponentsInitial;
	private float leGlobalFanOutInitial, leConvergenceInitial;
	private int leMaxIterationsInitial;
	private float leGateCapacitanceInitial, leDefaultWireCapRatioInitial, leDiffToGateCapRatioInitial;
	private float leKeeperSizeRatioInitial;

	private void initLogicalEffort()
	{
        Tool leTool = Tool.findTool("logical effort");
        //try { leTool.getPrefs().clear(); } catch (java.util.prefs.BackingStoreException e) {}
        
		leUseLocalSettingsInitial = leTool.getPrefs().getBoolean(LETool.OPTION_USELOCALSETTINGS, LETool.DEFAULT_USELOCALSETTINGS);
		leUseLocalSettings.setSelected(leUseLocalSettingsInitial);

		leDisplayIntermediateCapsInitial = leTool.getPrefs().getBoolean("blah", false);
		leDisplayIntermediateCaps.setSelected(leDisplayIntermediateCapsInitial);

		leHighlightComponentsInitial = leTool.getPrefs().getBoolean("blah2", false);
		leHighlightComponents.setSelected(leHighlightComponentsInitial);

		leGlobalFanOutInitial = leTool.getPrefs().getFloat(LETool.OPTION_GLOBALFANOUT, LETool.DEFAULT_GLOBALFANOUT);
		leGlobalFanOut.setText(Float.toString(leGlobalFanOutInitial));

		leConvergenceInitial = leTool.getPrefs().getFloat(LETool.OPTION_EPSILON, LETool.DEFAULT_EPSILON);
		leConvergence.setText(Float.toString(leConvergenceInitial));

		leMaxIterationsInitial = leTool.getPrefs().getInt(LETool.OPTION_MAXITER, LETool.DEFAULT_MAXITER);
		leMaxIterations.setText(Integer.toString(leMaxIterationsInitial));

		leGateCapacitanceInitial = leTool.getPrefs().getFloat(LETool.OPTION_GATECAP, LETool.DEFAULT_GATECAP);
		leGateCapacitance.setText(Float.toString(leGateCapacitanceInitial));

		leDefaultWireCapRatioInitial = leTool.getPrefs().getFloat(LETool.OPTION_WIRERATIO, LETool.DEFAULT_WIRERATIO);
		leDefaultWireCapRatio.setText(Float.toString(leDefaultWireCapRatioInitial));

		leDiffToGateCapRatioInitial = leTool.getPrefs().getFloat(LETool.OPTION_DIFFALPHA, LETool.DEFAULT_DIFFALPHA);
		leDiffToGateCapRatio.setText(Float.toString(leDiffToGateCapRatioInitial));

		leKeeperSizeRatioInitial = leTool.getPrefs().getFloat(LETool.OPTION_KEEPERRATIO, LETool.DEFAULT_KEEPERRATIO);
		leKeeperSizeRatio.setText(Float.toString(leKeeperSizeRatioInitial));

		// make an empty list for the layer names
		leArcOptions = new HashMap();
		for(Iterator it = curTech.getArcs(); it.hasNext(); )
		{
			ArcProto arc = (ArcProto)it.next();
			leArcOptions.put(arc, Option.newDoubleOption(leTool.getPrefs().getDouble(arc.toString(), 0.0)));
		}
		leArcListModel = new DefaultListModel();
		leArcList = new JList(leArcListModel);
		leArcList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		leArc.setViewportView(leArcList);
		leArcList.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt) { leArcListClick(evt); }
		});
		showArcsInTechnology(leArcListModel);
		leArcList.setSelectedIndex(0);
		leArcListClick(null);
		leWireRatio.getDocument().addDocumentListener(new ArcDocumentListener(leArcOptions, leArcList, leArcListModel, curTech));
	}

	private void termLogicalEffort()
	{
        Tool leTool = Tool.findTool("logical effort");

        boolean nowBoolean = leUseLocalSettings.isSelected();
		if (leUseLocalSettingsInitial != nowBoolean) leTool.getPrefs().putBoolean(LETool.OPTION_USELOCALSETTINGS, nowBoolean);

		nowBoolean = leDisplayIntermediateCaps.isSelected();
		if (leDisplayIntermediateCapsInitial != nowBoolean) leTool.getPrefs().putBoolean("blah", nowBoolean);

		nowBoolean = leHighlightComponents.isSelected();
		if (leHighlightComponentsInitial != nowBoolean) leTool.getPrefs().putBoolean("blah2", nowBoolean);

		float nowFloat = (float)EMath.atof(leGlobalFanOut.getText());
		if (leGlobalFanOutInitial != nowFloat) leTool.getPrefs().putFloat(LETool.OPTION_GLOBALFANOUT, nowFloat);

		nowFloat = (float)EMath.atof(leConvergence.getText());
		if (leConvergenceInitial != nowFloat) leTool.getPrefs().putFloat(LETool.OPTION_EPSILON, nowFloat);

		int nowInt = EMath.atoi(leMaxIterations.getText());
		if (leMaxIterationsInitial != nowInt) leTool.getPrefs().putInt(LETool.OPTION_MAXITER, nowInt);

		nowFloat = (float)EMath.atof(leGateCapacitance.getText());
		if (leGateCapacitanceInitial != nowFloat) leTool.getPrefs().putFloat(LETool.OPTION_GATECAP, nowFloat);

		nowFloat = (float)EMath.atof(leDefaultWireCapRatio.getText());
		if (leDefaultWireCapRatioInitial != nowFloat) leTool.getPrefs().putFloat(LETool.OPTION_WIRERATIO, nowFloat);

		nowFloat = (float)EMath.atof(leDiffToGateCapRatio.getText());
		if (leDiffToGateCapRatioInitial != nowFloat) leTool.getPrefs().putFloat(LETool.OPTION_DIFFALPHA, nowFloat);

		nowFloat = (float)EMath.atof(leKeeperSizeRatio.getText());
		if (leKeeperSizeRatioInitial != nowFloat) leTool.getPrefs().putFloat(LETool.OPTION_KEEPERRATIO, nowFloat);

		for(Iterator it = curTech.getArcs(); it.hasNext(); )
		{
			ArcProto arc = (ArcProto)it.next();
			Option option = (Option)leArcOptions.get(arc);
			if (option != null && option.isChanged())
                leTool.getPrefs().putDouble(arc.toString(), option.getDoubleValue());
		}
	}

	private void leArcListClick(java.awt.event.MouseEvent evt)
	{
		String arcName = (String)leArcList.getSelectedValue();
		int firstSpace = arcName.indexOf(' ');
		if (firstSpace > 0) arcName = arcName.substring(0, firstSpace);
		ArcProto arc = curTech.findArcProto(arcName);
		Option option = (Option)leArcOptions.get(arc);
		if (option == null) return;
		leWireRatio.setText(Double.toString(option.getDoubleValue()));
	}

	/**
	 * Class to handle special changes to per-arc logical effort.
	 */
	private static class ArcDocumentListener implements DocumentListener
	{
		HashMap optionMap;
		JList list;
		DefaultListModel model;
		Technology tech;

		ArcDocumentListener(HashMap optionMap, JList list, DefaultListModel model, Technology tech)
		{
			this.optionMap = optionMap;
			this.list = list;
			this.model = model;
			this.tech = tech;
		}

		private void change(DocumentEvent e)
		{
			// get the currently selected layer
			String arcName = (String)list.getSelectedValue();
			int firstSpace = arcName.indexOf(' ');
			if (firstSpace > 0) arcName = arcName.substring(0, firstSpace);
			ArcProto arc = tech.findArcProto(arcName);
			Option option = (Option)optionMap.get(arc);
			if (option == null) return;

			// get the typed value
			Document doc = e.getDocument();
			int len = doc.getLength();
			String text;
			try
			{
				text = doc.getText(0, len);
			} catch (BadLocationException ex) { return; }
			double v = EMath.atof(text);

			// update the option
			option.setDoubleValue(v);
			int index = list.getSelectedIndex();
			String newLine = arc.getProtoName() + " (" + v + ")";
			model.setElementAt(newLine, index);
		}

		public void changedUpdate(DocumentEvent e) { change(e); }
		public void insertUpdate(DocumentEvent e) { change(e); }
		public void removeUpdate(DocumentEvent e) { change(e); }
	}

	private void showArcsInTechnology(DefaultListModel model)
	{
		model.clear();
		for(Iterator it = curTech.getArcs(); it.hasNext(); )
		{
			ArcProto arc = (ArcProto)it.next();
			model.addElement(arc.getProtoName() + " (" + ((Option)leArcOptions.get(arc)).getDoubleValue() + ")");
		}
	}

	//******************************** NETWORK ********************************

	private boolean netUnifyPwrGndInitial, netUnifyLikeNamedNetsInitial, netIgnoreResistorsInitial;
	private String netUnificationPrefixInitial;
	private boolean netBusBaseZeroInitial, netBusAscendingInitial;

	private void initNetwork()
	{
		netUnifyPwrGndInitial = JNetwork.isUnifyPowerAndGround();
		netUnifyPwrGnd.setSelected(netUnifyPwrGndInitial);

		netUnifyLikeNamedNetsInitial = JNetwork.isUnifyLikeNamedNets();
		netUnifyLikeNamedNets.setSelected(netUnifyLikeNamedNetsInitial);

		netIgnoreResistorsInitial = JNetwork.isIgnoreResistors();
		netIgnoreResistors.setSelected(netIgnoreResistorsInitial);

		netUnificationPrefixInitial = JNetwork.getUnificationPrefix();
		netUnificationPrefix.setText(netUnificationPrefixInitial);

		netBusBaseZeroInitial = JNetwork.isBusBaseZero();
		netStartingIndex.addItem("0");
		netStartingIndex.addItem("1");
		if (!netBusBaseZeroInitial) netStartingIndex.setSelectedIndex(1);

		netBusAscendingInitial = JNetwork.isBusAscending();
		if (netBusAscendingInitial) netAscending.setSelected(true); else
			netDescending.setSelected(true);
	}

	private void termNetwork()
	{
		boolean nowBoolean = netUnifyPwrGnd.isSelected();
		if (netUnifyPwrGndInitial != nowBoolean) JNetwork.setUnifyPowerAndGround(nowBoolean);

		nowBoolean = netUnifyLikeNamedNets.isSelected();
		if (netUnifyLikeNamedNetsInitial != nowBoolean) JNetwork.setUnifyLikeNamedNets(nowBoolean);

		nowBoolean = netIgnoreResistors.isSelected();
		if (netIgnoreResistorsInitial != nowBoolean) JNetwork.setIgnoreResistors(nowBoolean);

		String nowString = netUnificationPrefix.getText();
		if (!netUnificationPrefixInitial.equals(nowString)) JNetwork.setUnificationPrefix(nowString);

		nowBoolean = netStartingIndex.getSelectedIndex() == 0;
		if (netBusBaseZeroInitial != nowBoolean) JNetwork.setBusBaseZero(nowBoolean);

		nowBoolean = netAscending.isSelected();
		if (netBusAscendingInitial != nowBoolean) JNetwork.setBusAscending(nowBoolean);
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
        netDefaultOrder = new javax.swing.ButtonGroup();
        verilogModel = new javax.swing.ButtonGroup();
        tabs = new javax.swing.JTabbedPane();
        drc = new javax.swing.JPanel();
        jLabel30 = new javax.swing.JLabel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jSeparator6 = new javax.swing.JSeparator();
        jLabel31 = new javax.swing.JLabel();
        jCheckBox2 = new javax.swing.JCheckBox();
        jButton1 = new javax.swing.JButton();
        jSeparator7 = new javax.swing.JSeparator();
        jLabel32 = new javax.swing.JLabel();
        jCheckBox3 = new javax.swing.JCheckBox();
        jLabel33 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jCheckBox4 = new javax.swing.JCheckBox();
        jSeparator8 = new javax.swing.JSeparator();
        jLabel34 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        drcRules = new javax.swing.JPanel();
        jLabel35 = new javax.swing.JLabel();
        jLabel36 = new javax.swing.JLabel();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jLabel37 = new javax.swing.JLabel();
        jCheckBox5 = new javax.swing.JCheckBox();
        jScrollPane2 = new javax.swing.JScrollPane();
        jLabel38 = new javax.swing.JLabel();
        jLabel39 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jLabel40 = new javax.swing.JLabel();
        jLabel41 = new javax.swing.JLabel();
        jLabel42 = new javax.swing.JLabel();
        jLabel43 = new javax.swing.JLabel();
        jLabel44 = new javax.swing.JLabel();
        jLabel45 = new javax.swing.JLabel();
        jLabel46 = new javax.swing.JLabel();
        jLabel47 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        jLabel49 = new javax.swing.JLabel();
        jLabel50 = new javax.swing.JLabel();
        jLabel51 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        jLabel53 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();
        jTextField6 = new javax.swing.JTextField();
        jTextField7 = new javax.swing.JTextField();
        jTextField8 = new javax.swing.JTextField();
        jTextField9 = new javax.swing.JTextField();
        jTextField10 = new javax.swing.JTextField();
        jTextField11 = new javax.swing.JTextField();
        jTextField12 = new javax.swing.JTextField();
        jTextField13 = new javax.swing.JTextField();
        jTextField14 = new javax.swing.JTextField();
        jTextField15 = new javax.swing.JTextField();
        jTextField16 = new javax.swing.JTextField();
        jTextField17 = new javax.swing.JTextField();
        jTextField18 = new javax.swing.JTextField();
        jTextField19 = new javax.swing.JTextField();
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
        jLabel21 = new javax.swing.JLabel();
        spiceRunParameters = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
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
        verilog = new javax.swing.JPanel();
        jLabel54 = new javax.swing.JLabel();
        verLibrary = new javax.swing.JComboBox();
        verCells = new javax.swing.JScrollPane();
        verUseAssign = new javax.swing.JCheckBox();
        verDefWireTrireg = new javax.swing.JCheckBox();
        verDeriveModel = new javax.swing.JRadioButton();
        verUseModelFile = new javax.swing.JRadioButton();
        verBrowse = new javax.swing.JButton();
        verFileName = new javax.swing.JTextField();
        fastHenry = new javax.swing.JPanel();
        fhUseSingleFrequency = new javax.swing.JCheckBox();
        jLabel55 = new javax.swing.JLabel();
        jLabel56 = new javax.swing.JLabel();
        jLabel57 = new javax.swing.JLabel();
        fhMakeMultipole = new javax.swing.JCheckBox();
        jLabel58 = new javax.swing.JLabel();
        fhFrequencyStart = new javax.swing.JTextField();
        fhFrequencyEnd = new javax.swing.JTextField();
        fhRunsPerDecade = new javax.swing.JTextField();
        fhNumberOfPoles = new javax.swing.JTextField();
        jLabel59 = new javax.swing.JLabel();
        jLabel60 = new javax.swing.JLabel();
        jLabel61 = new javax.swing.JLabel();
        jLabel62 = new javax.swing.JLabel();
        fhDefaultThickness = new javax.swing.JTextField();
        fhDefaultWidthSubs = new javax.swing.JTextField();
        fhDefaultHeightSubs = new javax.swing.JTextField();
        fhMaxSegmentLength = new javax.swing.JTextField();
        fhMakePostScript = new javax.swing.JCheckBox();
        fhMakeSpice = new javax.swing.JCheckBox();
        jLabel63 = new javax.swing.JLabel();
        fhAfterAction = new javax.swing.JComboBox();
        wellCheck = new javax.swing.JPanel();
        jLabel64 = new javax.swing.JLabel();
        jLabel65 = new javax.swing.JLabel();
        wellPMustHaveAllContacts = new javax.swing.JRadioButton();
        wellPMustHave1Contact = new javax.swing.JRadioButton();
        wellPNoContactCheck = new javax.swing.JRadioButton();
        wellNMustHaveAllContacts = new javax.swing.JRadioButton();
        wellNMustHave1Contact = new javax.swing.JRadioButton();
        wellNNoContactCheck = new javax.swing.JRadioButton();
        wellPMustConnectGround = new javax.swing.JCheckBox();
        wellNMustConnectPower = new javax.swing.JCheckBox();
        wellFindFarthestDistance = new javax.swing.JCheckBox();
        antennaRules = new javax.swing.JPanel();
        jLabel66 = new javax.swing.JLabel();
        antTechnology = new javax.swing.JLabel();
        antArcList = new javax.swing.JScrollPane();
        jLabel68 = new javax.swing.JLabel();
        jTextField20 = new javax.swing.JTextField();
        network = new javax.swing.JPanel();
        netUnifyPwrGnd = new javax.swing.JCheckBox();
        netUnifyLikeNamedNets = new javax.swing.JCheckBox();
        netIgnoreResistors = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        netUnificationPrefix = new javax.swing.JTextField();
        jSeparator5 = new javax.swing.JSeparator();
        jLabel26 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        netStartingIndex = new javax.swing.JComboBox();
        jLabel28 = new javax.swing.JLabel();
        netAscending = new javax.swing.JRadioButton();
        netDescending = new javax.swing.JRadioButton();
        jLabel29 = new javax.swing.JLabel();
        ncc = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel71 = new javax.swing.JLabel();
        jLabel72 = new javax.swing.JLabel();
        jTextField21 = new javax.swing.JTextField();
        jTextField22 = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jLabel73 = new javax.swing.JLabel();
        jSeparator10 = new javax.swing.JSeparator();
        jPanel2 = new javax.swing.JPanel();
        jLabel74 = new javax.swing.JLabel();
        jCheckBox6 = new javax.swing.JCheckBox();
        jCheckBox7 = new javax.swing.JCheckBox();
        jCheckBox8 = new javax.swing.JCheckBox();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jCheckBox9 = new javax.swing.JCheckBox();
        jCheckBox10 = new javax.swing.JCheckBox();
        jCheckBox11 = new javax.swing.JCheckBox();
        jLabel75 = new javax.swing.JLabel();
        jLabel76 = new javax.swing.JLabel();
        jTextField23 = new javax.swing.JTextField();
        jTextField24 = new javax.swing.JTextField();
        jCheckBox12 = new javax.swing.JCheckBox();
        jCheckBox13 = new javax.swing.JCheckBox();
        jCheckBox14 = new javax.swing.JCheckBox();
        jButton9 = new javax.swing.JButton();
        jCheckBox15 = new javax.swing.JCheckBox();
        jSeparator9 = new javax.swing.JSeparator();
        jLabel77 = new javax.swing.JLabel();
        jRadioButton3 = new javax.swing.JRadioButton();
        jRadioButton4 = new javax.swing.JRadioButton();
        jRadioButton5 = new javax.swing.JRadioButton();
        jRadioButton6 = new javax.swing.JRadioButton();
        jRadioButton7 = new javax.swing.JRadioButton();
        jRadioButton8 = new javax.swing.JRadioButton();
        jRadioButton9 = new javax.swing.JRadioButton();
        jRadioButton10 = new javax.swing.JRadioButton();
        jRadioButton11 = new javax.swing.JRadioButton();
        jComboBox1 = new javax.swing.JComboBox();
        jScrollPane3 = new javax.swing.JScrollPane();
        jButton10 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();
        jButton13 = new javax.swing.JButton();
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
        jLabel25 = new javax.swing.JLabel();
        leUseLocalSettings = new javax.swing.JCheckBox();
        leHighlightComponents = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        leGlobalFanOut = new javax.swing.JTextField();
        leConvergence = new javax.swing.JTextField();
        leMaxIterations = new javax.swing.JTextField();
        leGateCapacitance = new javax.swing.JTextField();
        leDefaultWireCapRatio = new javax.swing.JTextField();
        leDiffToGateCapRatio = new javax.swing.JTextField();
        leKeeperSizeRatio = new javax.swing.JTextField();
        leWireRatio = new javax.swing.JTextField();
        routing = new javax.swing.JPanel();
        jLabel67 = new javax.swing.JLabel();
        jLabel69 = new javax.swing.JLabel();
        routDefaultArc = new javax.swing.JComboBox();
        routMimicCanUnstitch = new javax.swing.JCheckBox();
        jLabel70 = new javax.swing.JLabel();
        routMimicPortsMustMatch = new javax.swing.JCheckBox();
        routMimicNumArcsMustMatch = new javax.swing.JCheckBox();
        routMimicNodeSizesMustMatch = new javax.swing.JCheckBox();
        routMimicNodeTypesMustMatch = new javax.swing.JCheckBox();
        routMimicNoOtherArcs = new javax.swing.JCheckBox();
        routMimicInteractive = new javax.swing.JCheckBox();
        compaction = new javax.swing.JPanel();
        compAllowSpreading = new javax.swing.JCheckBox();
        compVerbose = new javax.swing.JCheckBox();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        factoryReset = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        tabs.setToolTipText("");
        drc.setLayout(new java.awt.GridBagLayout());

        jLabel30.setText("Incremental DRC:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        drc.add(jLabel30, gridBagConstraints);

        jCheckBox1.setText("On");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        drc.add(jCheckBox1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        drc.add(jSeparator6, gridBagConstraints);

        jLabel31.setText("Hierarchical DRC:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        drc.add(jLabel31, gridBagConstraints);

        jCheckBox2.setText("Just 1 error per cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        drc.add(jCheckBox2, gridBagConstraints);

        jButton1.setText("Clear valid DRC dates");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        drc.add(jButton1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        drc.add(jSeparator7, gridBagConstraints);

        jLabel32.setText("Incremental and hierarchical:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        drc.add(jLabel32, gridBagConstraints);

        jCheckBox3.setText("Use multiple processors");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        drc.add(jCheckBox3, gridBagConstraints);

        jLabel33.setText("Number of processors:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        drc.add(jLabel33, gridBagConstraints);

        jTextField1.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        drc.add(jTextField1, gridBagConstraints);

        jCheckBox4.setText("Ignore center cuts in large contacts");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        drc.add(jCheckBox4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        drc.add(jSeparator8, gridBagConstraints);

        jLabel34.setText("Dracula DRC Interface:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        drc.add(jLabel34, gridBagConstraints);

        jButton2.setText("Edit Rules Deck");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        drc.add(jButton2, gridBagConstraints);

        tabs.addTab("DRC", drc);

        drcRules.setLayout(new java.awt.GridBagLayout());

        jLabel35.setText("Technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        drcRules.add(jLabel35, gridBagConstraints);

        jLabel36.setText("mocmos");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        drcRules.add(jLabel36, gridBagConstraints);

        jRadioButton1.setText("Layers:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        drcRules.add(jRadioButton1, gridBagConstraints);

        jRadioButton2.setText("Nodes:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        drcRules.add(jRadioButton2, gridBagConstraints);

        jScrollPane1.setPreferredSize(new java.awt.Dimension(100, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.5;
        drcRules.add(jScrollPane1, gridBagConstraints);

        jLabel37.setText("To Layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        drcRules.add(jLabel37, gridBagConstraints);

        jCheckBox5.setText("Show only lines with rules");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        drcRules.add(jCheckBox5, gridBagConstraints);

        jScrollPane2.setPreferredSize(new java.awt.Dimension(200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        drcRules.add(jScrollPane2, gridBagConstraints);

        jLabel38.setText("Minimum Width:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        drcRules.add(jLabel38, gridBagConstraints);

        jLabel39.setText("Minimum Height:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        drcRules.add(jLabel39, gridBagConstraints);

        jTextField2.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        drcRules.add(jTextField2, gridBagConstraints);

        jTextField3.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        drcRules.add(jTextField3, gridBagConstraints);

        jTextField4.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        drcRules.add(jTextField4, gridBagConstraints);

        jLabel40.setText("Size");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        drcRules.add(jLabel40, gridBagConstraints);

        jLabel41.setText("Rule");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        drcRules.add(jLabel41, gridBagConstraints);

        jLabel42.setText("Normal:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        drcRules.add(jLabel42, gridBagConstraints);

        jLabel43.setText("Distance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        drcRules.add(jLabel43, gridBagConstraints);

        jLabel44.setText("Rule");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        drcRules.add(jLabel44, gridBagConstraints);

        jLabel45.setText("When connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 10, 4, 0);
        drcRules.add(jLabel45, gridBagConstraints);

        jLabel46.setText("Not connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 10, 4, 0);
        drcRules.add(jLabel46, gridBagConstraints);

        jLabel47.setText("Edge:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 10, 4, 0);
        drcRules.add(jLabel47, gridBagConstraints);

        jLabel48.setText("Wide (when bigger than this):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        drcRules.add(jLabel48, gridBagConstraints);

        jLabel49.setText("When connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 10, 4, 0);
        drcRules.add(jLabel49, gridBagConstraints);

        jLabel50.setText("Not connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 10, 4, 0);
        drcRules.add(jLabel50, gridBagConstraints);

        jLabel51.setText("Multiple cuts:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        drcRules.add(jLabel51, gridBagConstraints);

        jLabel52.setText("When connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 10, 4, 0);
        drcRules.add(jLabel52, gridBagConstraints);

        jLabel53.setText("Not connected:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 10, 4, 0);
        drcRules.add(jLabel53, gridBagConstraints);

        jTextField5.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        drcRules.add(jTextField5, gridBagConstraints);

        jTextField6.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        drcRules.add(jTextField6, gridBagConstraints);

        jTextField7.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        drcRules.add(jTextField7, gridBagConstraints);

        jTextField8.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        drcRules.add(jTextField8, gridBagConstraints);

        jTextField9.setColumns(6);
        jTextField9.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jTextField9ActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        drcRules.add(jTextField9, gridBagConstraints);

        jTextField10.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        drcRules.add(jTextField10, gridBagConstraints);

        jTextField11.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        drcRules.add(jTextField11, gridBagConstraints);

        jTextField12.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        drcRules.add(jTextField12, gridBagConstraints);

        jTextField13.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        drcRules.add(jTextField13, gridBagConstraints);

        jTextField14.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        drcRules.add(jTextField14, gridBagConstraints);

        jTextField15.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        drcRules.add(jTextField15, gridBagConstraints);

        jTextField16.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        drcRules.add(jTextField16, gridBagConstraints);

        jTextField17.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        drcRules.add(jTextField17, gridBagConstraints);

        jTextField18.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        drcRules.add(jTextField18, gridBagConstraints);

        jTextField19.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        drcRules.add(jTextField19, gridBagConstraints);

        tabs.addTab("Design Rules", drcRules);

        spice.setLayout(new java.awt.GridBagLayout());

        spice.setToolTipText("Options for Spice deck generation");
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

        spiceWriteTransSizesInLambda.setText("Write Trans Sizes in Lambda");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        spice1.add(spiceWriteTransSizesInLambda, gridBagConstraints);

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

        spiceRunParameters.setMinimumSize(new java.awt.Dimension(100, 20));
        spiceRunParameters.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        spice2.add(spiceRunParameters, gridBagConstraints);

        jLabel16.setText("Run:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
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
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        spice3.add(spicePrimitivesetPopup, gridBagConstraints);

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

        jLabel18.setText("Min Resistance:");
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

        jLabel19.setText("Min. Capacitance:");
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
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        spice.add(jSeparator2, gridBagConstraints);

        spice5.setLayout(new java.awt.GridBagLayout());

        spiceHeaderCardExtension.setColumns(5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
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
        spiceBrowseHeaderFile.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                spiceBrowseHeaderFileActionPerformed(evt);
            }
        });

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
        gridBagConstraints.gridwidth = 2;
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
        spiceBrowseTrailerFile.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                spiceBrowseTrailerFileActionPerformed(evt);
            }
        });

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
        spiceModelFileBrowse.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                spiceModelFileBrowseActionPerformed(evt);
            }
        });

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
        gridBagConstraints.weighty = 0.5;
        spice.add(spice6, gridBagConstraints);

        tabs.addTab("Spice", spice);

        verilog.setLayout(new java.awt.GridBagLayout());

        jLabel54.setText("Library:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        verilog.add(jLabel54, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verLibrary, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        verilog.add(verCells, gridBagConstraints);

        verUseAssign.setText("Use ASSIGN Construct");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verUseAssign, gridBagConstraints);

        verDefWireTrireg.setText("Default wire is Trireg");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verDefWireTrireg, gridBagConstraints);

        verDeriveModel.setText("Derive Model from Circuitry");
        verilogModel.add(verDeriveModel);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(20, 4, 4, 4);
        verilog.add(verDeriveModel, gridBagConstraints);

        verUseModelFile.setText("Use Model from File:");
        verilogModel.add(verUseModelFile);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verUseModelFile, gridBagConstraints);

        verBrowse.setText("Browse");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verBrowse, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verFileName, gridBagConstraints);

        tabs.addTab("Verilog", verilog);

        fastHenry.setLayout(new java.awt.GridBagLayout());

        fhUseSingleFrequency.setText("Use single frequency");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhUseSingleFrequency, gridBagConstraints);

        jLabel55.setText("Frequency start:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        fastHenry.add(jLabel55, gridBagConstraints);

        jLabel56.setText("Frequency end:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        fastHenry.add(jLabel56, gridBagConstraints);

        jLabel57.setText("Runs per decade:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        fastHenry.add(jLabel57, gridBagConstraints);

        fhMakeMultipole.setText("Make multipole subcircuit");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhMakeMultipole, gridBagConstraints);

        jLabel58.setText("Number of poles:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        fastHenry.add(jLabel58, gridBagConstraints);

        fhFrequencyStart.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhFrequencyStart, gridBagConstraints);

        fhFrequencyEnd.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhFrequencyEnd, gridBagConstraints);

        fhRunsPerDecade.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhRunsPerDecade, gridBagConstraints);

        fhNumberOfPoles.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhNumberOfPoles, gridBagConstraints);

        jLabel59.setText("Default thickness:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(jLabel59, gridBagConstraints);

        jLabel60.setText("Default width subdivisions:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(jLabel60, gridBagConstraints);

        jLabel61.setText("Default height subdivisions:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(jLabel61, gridBagConstraints);

        jLabel62.setText("Maximum segment length:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(jLabel62, gridBagConstraints);

        fhDefaultThickness.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhDefaultThickness, gridBagConstraints);

        fhDefaultWidthSubs.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhDefaultWidthSubs, gridBagConstraints);

        fhDefaultHeightSubs.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhDefaultHeightSubs, gridBagConstraints);

        fhMaxSegmentLength.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhMaxSegmentLength, gridBagConstraints);

        fhMakePostScript.setText("Make PostScript view");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhMakePostScript, gridBagConstraints);

        fhMakeSpice.setText("Make SPICE subcircuit");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhMakeSpice, gridBagConstraints);

        jLabel63.setText("After writing deck:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(jLabel63, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        fastHenry.add(fhAfterAction, gridBagConstraints);

        tabs.addTab("Fast Henry", fastHenry);

        wellCheck.setLayout(new java.awt.GridBagLayout());

        jLabel64.setText("For P-Well:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        wellCheck.add(jLabel64, gridBagConstraints);

        jLabel65.setText("For N-Well:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        wellCheck.add(jLabel65, gridBagConstraints);

        wellPMustHaveAllContacts.setText("Must have contact in every area");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellPMustHaveAllContacts, gridBagConstraints);

        wellPMustHave1Contact.setText("Must have at least 1 contact");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellPMustHave1Contact, gridBagConstraints);

        wellPNoContactCheck.setText("Do not check for contacts");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellPNoContactCheck, gridBagConstraints);

        wellNMustHaveAllContacts.setText("Must have contact in every area");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellNMustHaveAllContacts, gridBagConstraints);

        wellNMustHave1Contact.setText("Must have at least 1 contact");
        wellNMustHave1Contact.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                wellNMustHave1ContactActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellNMustHave1Contact, gridBagConstraints);

        wellNNoContactCheck.setText("Do not check for contacts");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellNNoContactCheck, gridBagConstraints);

        wellPMustConnectGround.setText("Must connect to Ground");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellPMustConnectGround, gridBagConstraints);

        wellNMustConnectPower.setText("Must connect to Power");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellNMustConnectPower, gridBagConstraints);

        wellFindFarthestDistance.setText("Find farthest distance from contact to edge");
        wellFindFarthestDistance.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                wellFindFarthestDistanceActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellFindFarthestDistance, gridBagConstraints);

        tabs.addTab("Well Check", wellCheck);

        antennaRules.setLayout(new java.awt.GridBagLayout());

        jLabel66.setText("Arcs in technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        antennaRules.add(jLabel66, gridBagConstraints);

        antTechnology.setText("mocmos");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        antennaRules.add(antTechnology, gridBagConstraints);

        antArcList.setPreferredSize(new java.awt.Dimension(300, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        antennaRules.add(antArcList, gridBagConstraints);

        jLabel68.setText("Maximum antenna ratio:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        antennaRules.add(jLabel68, gridBagConstraints);

        jTextField20.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        antennaRules.add(jTextField20, gridBagConstraints);

        tabs.addTab("Antenna Rules", antennaRules);

        network.setLayout(new java.awt.GridBagLayout());

        netUnifyPwrGnd.setText("Unify Power and Ground");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        network.add(netUnifyPwrGnd, gridBagConstraints);

        netUnifyLikeNamedNets.setText("Unify all like-named nets");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        network.add(netUnifyLikeNamedNets, gridBagConstraints);

        netIgnoreResistors.setText("Ignore Resistors");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        network.add(netIgnoreResistors, gridBagConstraints);

        jLabel3.setText("Unify Networks that start with:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        network.add(jLabel3, gridBagConstraints);

        netUnificationPrefix.setColumns(20);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 40, 4, 0);
        network.add(netUnificationPrefix, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        network.add(jSeparator5, gridBagConstraints);

        jLabel26.setText("For busses:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        network.add(jLabel26, gridBagConstraints);

        jLabel27.setText("Default starting index:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        network.add(jLabel27, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        network.add(netStartingIndex, gridBagConstraints);

        jLabel28.setText("Default order:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        network.add(jLabel28, gridBagConstraints);

        netAscending.setText("Ascending (0:N)");
        netDefaultOrder.add(netAscending);
        netAscending.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                netAscendingActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 40, 4, 0);
        network.add(netAscending, gridBagConstraints);

        netDescending.setText("Descending (N:0)");
        netDefaultOrder.add(netDescending);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 40, 4, 0);
        network.add(netDescending, gridBagConstraints);

        jLabel29.setText("Network numbering:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        network.add(jLabel29, gridBagConstraints);

        tabs.addTab("Network", network);

        ncc.setLayout(new java.awt.BorderLayout());

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel71.setText("Compare cell:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(jLabel71, gridBagConstraints);

        jLabel72.setText("With cell:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(jLabel72, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(jTextField21, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(jTextField22, gridBagConstraints);

        jButton3.setText("Set");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        jPanel1.add(jButton3, gridBagConstraints);

        jButton4.setText("Set");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        jPanel1.add(jButton4, gridBagConstraints);

        jButton5.setText("Next");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        jPanel1.add(jButton5, gridBagConstraints);

        jButton6.setText("Next");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        jPanel1.add(jButton6, gridBagConstraints);

        jLabel73.setText("!!! WARNING: These cells are not on the screen !!!");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(jLabel73, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(jSeparator10, gridBagConstraints);

        ncc.add(jPanel1, java.awt.BorderLayout.NORTH);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jLabel74.setText("For all cells:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel2.add(jLabel74, gridBagConstraints);

        jCheckBox6.setText("Expand hierarchy");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel2.add(jCheckBox6, gridBagConstraints);

        jCheckBox7.setText("Merge parallel components");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jCheckBox7, gridBagConstraints);

        jCheckBox8.setText("Merge series transistors");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel2.add(jCheckBox8, gridBagConstraints);

        jButton7.setText("Clear NCC dates this library");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        jPanel2.add(jButton7, gridBagConstraints);

        jButton8.setText("Clear NCC dates all libraries");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        jPanel2.add(jButton8, gridBagConstraints);

        jCheckBox9.setText("Ignore power and ground");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel2.add(jCheckBox9, gridBagConstraints);

        jCheckBox10.setText("Check export names");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel2.add(jCheckBox10, gridBagConstraints);

        jCheckBox11.setText("Check component sizes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel2.add(jCheckBox11, gridBagConstraints);

        jLabel75.setText("Size tolerance (%):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel2.add(jLabel75, gridBagConstraints);

        jLabel76.setText("Size tolerance (amt):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel2.add(jLabel76, gridBagConstraints);

        jTextField23.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        jPanel2.add(jTextField23, gridBagConstraints);

        jTextField24.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        jPanel2.add(jTextField24, gridBagConstraints);

        jCheckBox12.setText("Allow no-component nets");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel2.add(jCheckBox12, gridBagConstraints);

        jCheckBox13.setText("Recurse through hierarchy");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel2.add(jCheckBox13, gridBagConstraints);

        jCheckBox14.setText("Automatic Resistor Exclusion");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel2.add(jCheckBox14, gridBagConstraints);

        jButton9.setText("NCC Debugging options...");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 2;
        jPanel2.add(jButton9, gridBagConstraints);

        jCheckBox15.setText("Show 'NCCMatch' tags");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jCheckBox15, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 12;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(jSeparator9, gridBagConstraints);

        jLabel77.setText("Individual cell overrides:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jLabel77, gridBagConstraints);

        jRadioButton3.setText("Yes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jRadioButton3, gridBagConstraints);

        jRadioButton4.setText("No");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jRadioButton4, gridBagConstraints);

        jRadioButton5.setText("Default");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jRadioButton5, gridBagConstraints);

        jRadioButton6.setText("Yes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jRadioButton6, gridBagConstraints);

        jRadioButton7.setText("No");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jRadioButton7, gridBagConstraints);

        jRadioButton8.setText("Default");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jRadioButton8, gridBagConstraints);

        jRadioButton9.setText("Yes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jRadioButton9, gridBagConstraints);

        jRadioButton10.setText("No");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jRadioButton10, gridBagConstraints);

        jRadioButton11.setText("Default");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jRadioButton11, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel2.add(jComboBox1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridheight = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel2.add(jScrollPane3, gridBagConstraints);

        jButton10.setText("List all forced matches");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 3;
        jPanel2.add(jButton10, gridBagConstraints);

        jButton11.setText("Remove all forced matches");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.gridwidth = 3;
        jPanel2.add(jButton11, gridBagConstraints);

        jButton12.setText("List all overrides");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 3;
        jPanel2.add(jButton12, gridBagConstraints);

        jButton13.setText("Remove all overrides");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 3;
        jPanel2.add(jButton13, gridBagConstraints);

        ncc.add(jPanel2, java.awt.BorderLayout.CENTER);

        tabs.addTab("NCC", ncc);

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
        logicalEffort.add(jLabel4, gridBagConstraints);

        leDisplayIntermediateCaps.setText("Display intermediate capacitances");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        logicalEffort.add(leDisplayIntermediateCaps, gridBagConstraints);

        jLabel5.setText("Wire ratio for each layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
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

        jLabel23.setText("Diffusion to gate cap ratio (alpha) :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        logicalEffort.add(jLabel23, gridBagConstraints);

        jLabel25.setText("Keeper size ratio (keeper size / driver size):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        logicalEffort.add(jLabel25, gridBagConstraints);

        leUseLocalSettings.setText("Use Local (cell) LE Settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        logicalEffort.add(leUseLocalSettings, gridBagConstraints);

        leHighlightComponents.setText("Highlight components");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        logicalEffort.add(leHighlightComponents, gridBagConstraints);

        jLabel6.setText("Wire ratio:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        logicalEffort.add(jLabel6, gridBagConstraints);

        leGlobalFanOut.setColumns(8);
        logicalEffort.add(leGlobalFanOut, new java.awt.GridBagConstraints());

        leConvergence.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        logicalEffort.add(leConvergence, gridBagConstraints);

        leMaxIterations.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        logicalEffort.add(leMaxIterations, gridBagConstraints);

        leGateCapacitance.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        logicalEffort.add(leGateCapacitance, gridBagConstraints);

        leDefaultWireCapRatio.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        logicalEffort.add(leDefaultWireCapRatio, gridBagConstraints);

        leDiffToGateCapRatio.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        logicalEffort.add(leDiffToGateCapRatio, gridBagConstraints);

        leKeeperSizeRatio.setColumns(8);
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

        routing.setLayout(new java.awt.GridBagLayout());

        jLabel67.setText("Arc to use in stitching routers:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        routing.add(jLabel67, gridBagConstraints);

        jLabel69.setText("Currently:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        routing.add(jLabel69, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        routing.add(routDefaultArc, gridBagConstraints);

        routMimicCanUnstitch.setText("Mimic stitching can unstitch");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        routing.add(routMimicCanUnstitch, gridBagConstraints);

        jLabel70.setText("Mimic stitching restrictions:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        routing.add(jLabel70, gridBagConstraints);

        routMimicPortsMustMatch.setText("Ports must match");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        routing.add(routMimicPortsMustMatch, gridBagConstraints);

        routMimicNumArcsMustMatch.setText("Number of existing arcs must match");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        routing.add(routMimicNumArcsMustMatch, gridBagConstraints);

        routMimicNodeSizesMustMatch.setText("Node sizes must match");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        routing.add(routMimicNodeSizesMustMatch, gridBagConstraints);

        routMimicNodeTypesMustMatch.setText("Node types must match");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        routing.add(routMimicNodeTypesMustMatch, gridBagConstraints);

        routMimicNoOtherArcs.setText("Cannot have other arcs in the same direction");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        routing.add(routMimicNoOtherArcs, gridBagConstraints);

        routMimicInteractive.setText("Mimic stitching runs interactively");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        routing.add(routMimicInteractive, gridBagConstraints);

        tabs.addTab("Routing", routing);

        compaction.setLayout(new java.awt.GridBagLayout());

        compAllowSpreading.setText("Allow spreading");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        compaction.add(compAllowSpreading, gridBagConstraints);

        compVerbose.setText("Verbose");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        compaction.add(compVerbose, gridBagConstraints);

        tabs.addTab("Compaction", compaction);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(tabs, gridBagConstraints);

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                CancelButton(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 40, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                OKButton(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 40);
        getContentPane().add(ok, gridBagConstraints);

        factoryReset.setText("Factory Reset");
        factoryReset.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                factoryResetActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(factoryReset, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void wellNMustHave1ContactActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_wellNMustHave1ContactActionPerformed
	{//GEN-HEADEREND:event_wellNMustHave1ContactActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_wellNMustHave1ContactActionPerformed

	private void wellFindFarthestDistanceActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_wellFindFarthestDistanceActionPerformed
	{//GEN-HEADEREND:event_wellFindFarthestDistanceActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_wellFindFarthestDistanceActionPerformed

	private void jTextField9ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jTextField9ActionPerformed
	{//GEN-HEADEREND:event_jTextField9ActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_jTextField9ActionPerformed

	private void netAscendingActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_netAscendingActionPerformed
	{//GEN-HEADEREND:event_netAscendingActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_netAscendingActionPerformed

	private void spiceModelFileBrowseActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_spiceModelFileBrowseActionPerformed
	{//GEN-HEADEREND:event_spiceModelFileBrowseActionPerformed
		String fileName = DialogOpenFile.ANY.chooseInputFile(null);
		if (fileName == null) return;
		spiceModelCell.setText(fileName);
		spiceUseModelFromFile.setSelected(true);
	}//GEN-LAST:event_spiceModelFileBrowseActionPerformed

	private void spiceBrowseTrailerFileActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_spiceBrowseTrailerFileActionPerformed
	{//GEN-HEADEREND:event_spiceBrowseTrailerFileActionPerformed
		String fileName = DialogOpenFile.ANY.chooseInputFile(null);
		if (fileName == null) return;
		spiceTrailerCardFile.setText(fileName);
		spiceTrailerCardsFromFile.setSelected(true);
	}//GEN-LAST:event_spiceBrowseTrailerFileActionPerformed

	private void spiceBrowseHeaderFileActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_spiceBrowseHeaderFileActionPerformed
	{//GEN-HEADEREND:event_spiceBrowseHeaderFileActionPerformed
		String fileName = DialogOpenFile.ANY.chooseInputFile(null);
		if (fileName == null) return;
		spiceHeaderCardFile.setText(fileName);
		spiceHeaderCardsFromFile.setSelected(true);
	}//GEN-LAST:event_spiceBrowseHeaderFileActionPerformed

	private void factoryResetActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_factoryResetActionPerformed
	{//GEN-HEADEREND:event_factoryResetActionPerformed

	}//GEN-LAST:event_factoryResetActionPerformed

	private void leHelpActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_leHelpActionPerformed
	{//GEN-HEADEREND:event_leHelpActionPerformed
		System.out.println("No help yet");
	}//GEN-LAST:event_leHelpActionPerformed

	private void CancelButton(java.awt.event.ActionEvent evt)//GEN-FIRST:event_CancelButton
	{//GEN-HEADEREND:event_CancelButton
		setVisible(false);
		dispose();
	}//GEN-LAST:event_CancelButton

	private void OKButton(java.awt.event.ActionEvent evt)//GEN-FIRST:event_OKButton
	{//GEN-HEADEREND:event_OKButton
		termSpice();
		termLogicalEffort();
		termNetwork();

		setVisible(false);
		dispose();
	}//GEN-LAST:event_OKButton
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane antArcList;
    private javax.swing.JLabel antTechnology;
    private javax.swing.JPanel antennaRules;
    private javax.swing.JButton cancel;
    private javax.swing.JCheckBox compAllowSpreading;
    private javax.swing.JCheckBox compVerbose;
    private javax.swing.JPanel compaction;
    private javax.swing.JPanel drc;
    private javax.swing.JPanel drcRules;
    private javax.swing.JButton factoryReset;
    private javax.swing.JPanel fastHenry;
    private javax.swing.JComboBox fhAfterAction;
    private javax.swing.JTextField fhDefaultHeightSubs;
    private javax.swing.JTextField fhDefaultThickness;
    private javax.swing.JTextField fhDefaultWidthSubs;
    private javax.swing.JTextField fhFrequencyEnd;
    private javax.swing.JTextField fhFrequencyStart;
    private javax.swing.JCheckBox fhMakeMultipole;
    private javax.swing.JCheckBox fhMakePostScript;
    private javax.swing.JCheckBox fhMakeSpice;
    private javax.swing.JTextField fhMaxSegmentLength;
    private javax.swing.JTextField fhNumberOfPoles;
    private javax.swing.JTextField fhRunsPerDecade;
    private javax.swing.JCheckBox fhUseSingleFrequency;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox10;
    private javax.swing.JCheckBox jCheckBox11;
    private javax.swing.JCheckBox jCheckBox12;
    private javax.swing.JCheckBox jCheckBox13;
    private javax.swing.JCheckBox jCheckBox14;
    private javax.swing.JCheckBox jCheckBox15;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JCheckBox jCheckBox4;
    private javax.swing.JCheckBox jCheckBox5;
    private javax.swing.JCheckBox jCheckBox6;
    private javax.swing.JCheckBox jCheckBox7;
    private javax.swing.JCheckBox jCheckBox8;
    private javax.swing.JCheckBox jCheckBox9;
    private javax.swing.JComboBox jComboBox1;
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
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel58;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel60;
    private javax.swing.JLabel jLabel61;
    private javax.swing.JLabel jLabel62;
    private javax.swing.JLabel jLabel63;
    private javax.swing.JLabel jLabel64;
    private javax.swing.JLabel jLabel65;
    private javax.swing.JLabel jLabel66;
    private javax.swing.JLabel jLabel67;
    private javax.swing.JLabel jLabel68;
    private javax.swing.JLabel jLabel69;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel70;
    private javax.swing.JLabel jLabel71;
    private javax.swing.JLabel jLabel72;
    private javax.swing.JLabel jLabel73;
    private javax.swing.JLabel jLabel74;
    private javax.swing.JLabel jLabel75;
    private javax.swing.JLabel jLabel76;
    private javax.swing.JLabel jLabel77;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton10;
    private javax.swing.JRadioButton jRadioButton11;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JRadioButton jRadioButton3;
    private javax.swing.JRadioButton jRadioButton4;
    private javax.swing.JRadioButton jRadioButton5;
    private javax.swing.JRadioButton jRadioButton6;
    private javax.swing.JRadioButton jRadioButton7;
    private javax.swing.JRadioButton jRadioButton8;
    private javax.swing.JRadioButton jRadioButton9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator10;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JSeparator jSeparator9;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField10;
    private javax.swing.JTextField jTextField11;
    private javax.swing.JTextField jTextField12;
    private javax.swing.JTextField jTextField13;
    private javax.swing.JTextField jTextField14;
    private javax.swing.JTextField jTextField15;
    private javax.swing.JTextField jTextField16;
    private javax.swing.JTextField jTextField17;
    private javax.swing.JTextField jTextField18;
    private javax.swing.JTextField jTextField19;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField20;
    private javax.swing.JTextField jTextField21;
    private javax.swing.JTextField jTextField22;
    private javax.swing.JTextField jTextField23;
    private javax.swing.JTextField jTextField24;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField8;
    private javax.swing.JTextField jTextField9;
    private javax.swing.JScrollPane leArc;
    private javax.swing.JTextField leConvergence;
    private javax.swing.JTextField leDefaultWireCapRatio;
    private javax.swing.JTextField leDiffToGateCapRatio;
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
    private javax.swing.JPanel ncc;
    private javax.swing.JRadioButton netAscending;
    private javax.swing.ButtonGroup netDefaultOrder;
    private javax.swing.JRadioButton netDescending;
    private javax.swing.JCheckBox netIgnoreResistors;
    private javax.swing.JComboBox netStartingIndex;
    private javax.swing.JTextField netUnificationPrefix;
    private javax.swing.JCheckBox netUnifyLikeNamedNets;
    private javax.swing.JCheckBox netUnifyPwrGnd;
    private javax.swing.JPanel network;
    private javax.swing.JButton ok;
    private javax.swing.JComboBox routDefaultArc;
    private javax.swing.JCheckBox routMimicCanUnstitch;
    private javax.swing.JCheckBox routMimicInteractive;
    private javax.swing.JCheckBox routMimicNoOtherArcs;
    private javax.swing.JCheckBox routMimicNodeSizesMustMatch;
    private javax.swing.JCheckBox routMimicNodeTypesMustMatch;
    private javax.swing.JCheckBox routMimicNumArcsMustMatch;
    private javax.swing.JCheckBox routMimicPortsMustMatch;
    private javax.swing.JPanel routing;
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
    private javax.swing.JTextField spiceRunParameters;
    private javax.swing.JComboBox spiceRunPopup;
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
    private javax.swing.JTabbedPane tabs;
    private javax.swing.JButton verBrowse;
    private javax.swing.JScrollPane verCells;
    private javax.swing.JCheckBox verDefWireTrireg;
    private javax.swing.JRadioButton verDeriveModel;
    private javax.swing.JTextField verFileName;
    private javax.swing.JComboBox verLibrary;
    private javax.swing.JCheckBox verUseAssign;
    private javax.swing.JRadioButton verUseModelFile;
    private javax.swing.JPanel verilog;
    private javax.swing.ButtonGroup verilogModel;
    private javax.swing.JPanel wellCheck;
    private javax.swing.JCheckBox wellFindFarthestDistance;
    private javax.swing.JCheckBox wellNMustConnectPower;
    private javax.swing.JRadioButton wellNMustHave1Contact;
    private javax.swing.JRadioButton wellNMustHaveAllContacts;
    private javax.swing.JRadioButton wellNNoContactCheck;
    private javax.swing.JCheckBox wellPMustConnectGround;
    private javax.swing.JRadioButton wellPMustHave1Contact;
    private javax.swing.JRadioButton wellPMustHaveAllContacts;
    private javax.swing.JRadioButton wellPNoContactCheck;
    // End of variables declaration//GEN-END:variables
	
}
