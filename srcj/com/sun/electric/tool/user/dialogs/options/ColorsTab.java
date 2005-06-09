/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ColorsTab.java
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

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.dialogs.ColorPatternPanel;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.lang.reflect.Method;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class to handle the "Colors" tab of the Preferences dialog.
 */
public class ColorsTab extends PreferencePanel
{
	/** Creates new form ColorsTab */
	public ColorsTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	public JPanel getPanel() { return colors; }

	public String getName() { return "Colors"; }

	private JList colorLayerList;
	private DefaultListModel colorLayerModel;
	private HashMap transAndSpecialMap;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Colors tab.
	 */
	public void init()
	{
		transAndSpecialMap = new HashMap();
		colorLayerModel = new DefaultListModel();
		colorLayerList = new JList(colorLayerModel);
		colorLayerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		colorLayerPane.setViewportView(colorLayerList);
		colorLayerList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt) { colorClickedLayer(); }
		});

		// look at all layers, pull out the transparent ones
		HashMap transparentLayers = new HashMap();
		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;
			EGraphics graphics = layer.getGraphics();
			int transparentLayer = graphics.getTransparentLayer();
			if (transparentLayer == 0) continue;

			StringBuffer layers = (StringBuffer)transparentLayers.get(new Integer(transparentLayer));
			if (layers == null)
			{
				layers = new StringBuffer();
				layers.append(layer.getName());
				transparentLayers.put(new Integer(transparentLayer), layers);
			} else
			{
				layers.append(", " + layer.getName());
			}
		}

		// sort and display the transparent layers
		Color [] currentMap = curTech.getColorMap();
		List transparentSet = new ArrayList();
		for(Iterator it = transparentLayers.keySet().iterator(); it.hasNext(); )
			transparentSet.add(it.next());
		Collections.sort(transparentSet, new TransparentSort());
		for(Iterator it = transparentSet.iterator(); it.hasNext(); )
		{
			Integer layerNumber = (Integer)it.next();
			StringBuffer layerNames = (StringBuffer)transparentLayers.get(layerNumber);
			colorLayerModel.addElement("Transparent " + layerNumber.intValue() + ": " + layerNames.toString());
			int color = currentMap[1 << (layerNumber.intValue()-1)].getRGB();
			transAndSpecialMap.put(layerNumber, new GenMath.MutableInteger(color));
		}

		// add the nontransparent layers
		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			if ((layer.getFunctionExtras() & Layer.Function.PSEUDO) != 0) continue;
			EGraphics graphics = layer.getGraphics();
			if (graphics.getTransparentLayer() > 0) continue;

			colorLayerModel.addElement(layer.getName());

//			ColorPatternPanel.Info li = (ColorPatternPanel.Info)layerMap.get(layer);
//			int color = layer.getGraphics().getColor().getRGB();
//			colorMap.put(layer, new GenMath.MutableInteger(color));
		}

		// add the special colors
		int color = User.getColorBackground();
		String name = "Special: BACKGROUND";
		colorLayerModel.addElement(name);
		transAndSpecialMap.put(name, new GenMath.MutableInteger(color));

		color = User.getColorGrid();
		name = "Special: GRID";
		colorLayerModel.addElement(name);
		transAndSpecialMap.put(name, new GenMath.MutableInteger(color));

		color = User.getColorHighlight();
		name = "Special: HIGHLIGHT";
		colorLayerModel.addElement(name);
		transAndSpecialMap.put(name, new GenMath.MutableInteger(color));

        color = User.getColorMouseOverHighlight();
        name = "Special: MOUSE-OVER HIGHLIGHT";
        colorLayerModel.addElement(name);
        transAndSpecialMap.put(name, new GenMath.MutableInteger(color));

		color = User.getColorPortHighlight();
		name = "Special: PORT HIGHLIGHT";
		colorLayerModel.addElement(name);
		transAndSpecialMap.put(name, new GenMath.MutableInteger(color));

		color = User.getColorText();
		name = "Special: TEXT";
		colorLayerModel.addElement(name);
		transAndSpecialMap.put(name, new GenMath.MutableInteger(color));

		color = User.getColorInstanceOutline();
		name = "Special: INSTANCE OUTLINES";
		colorLayerModel.addElement(name);
		transAndSpecialMap.put(name, new GenMath.MutableInteger(color));

		color = User.getColorWaveformBackground();
		name = "Special: WAVEFORM BACKGROUND";
		colorLayerModel.addElement(name);
		transAndSpecialMap.put(name, new GenMath.MutableInteger(color));

		color = User.getColorWaveformForeground();
		name = "Special: WAVEFORM FOREGROUND";
		colorLayerModel.addElement(name);
		transAndSpecialMap.put(name, new GenMath.MutableInteger(color));

		color = User.getColorWaveformStimuli();
		name = "Special: WAVEFORM STIMULI";
		colorLayerModel.addElement(name);
		transAndSpecialMap.put(name, new GenMath.MutableInteger(color));

		color = User.getColorWaveformStrengthOff();
		name = "Special: WAVEFORM OFF STRENGTH";
		colorLayerModel.addElement(name);
		transAndSpecialMap.put(name, new GenMath.MutableInteger(color));

		color = User.getColorWaveformStrengthNode();
		name = "Special: WAVEFORM NODE (WEAK) STRENGTH";
		colorLayerModel.addElement(name);
		transAndSpecialMap.put(name, new GenMath.MutableInteger(color));

		color = User.getColorWaveformStrengthGate();
		name = "Special: WAVEFORM GATE STRENGTH";
		colorLayerModel.addElement(name);
		transAndSpecialMap.put(name, new GenMath.MutableInteger(color));

		color = User.getColorWaveformStrengthPower();
		name = "Special: WAVEFORM POWER STRENGTH";
		colorLayerModel.addElement(name);
		transAndSpecialMap.put(name, new GenMath.MutableInteger(color));

		color = User.getColorWaveformCrossProbeLow();
		name = "Special: WAVEFORM CROSSPROBE LOW";
		colorLayerModel.addElement(name);
		transAndSpecialMap.put(name, new GenMath.MutableInteger(color));

		color = User.getColorWaveformCrossProbeHigh();
		name = "Special: WAVEFORM CROSSPROBE HIGH";
		colorLayerModel.addElement(name);
		transAndSpecialMap.put(name, new GenMath.MutableInteger(color));

		color = User.getColorWaveformCrossProbeX();
		name = "Special: WAVEFORM CROSSPROBE UNDEFINED";
		colorLayerModel.addElement(name);
		transAndSpecialMap.put(name, new GenMath.MutableInteger(color));

		color = User.getColorWaveformCrossProbeZ();
		name = "Special: WAVEFORM CROSSPROBE FLOATING";
		colorLayerModel.addElement(name);
		transAndSpecialMap.put(name, new GenMath.MutableInteger(color));

        // 3D Stuff
        try
        {
            Class j3DUtilsClass = Resources.get3DClass("utils.J3DUtils");
            Method setMethod = j3DUtilsClass.getDeclaredMethod("get3DColorsInTab", new Class[] {DefaultListModel.class, HashMap.class});
            setMethod.invoke(j3DUtilsClass, new Object[]{colorLayerModel, transAndSpecialMap});
        } catch (Exception e) {
            System.out.println("Cannot call 3D plugin method get3DColorsInTab: " + e.getMessage());
            e.printStackTrace();
        }

		// finish initialization
		colorLayerList.setSelectedIndex(0);
		colorChooser.getSelectionModel().addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent e) { colorChanged(); }
		});
		colorClickedLayer();
	}

	private static class TransparentSort implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Integer i1 = (Integer)o1;
			Integer i2 = (Integer)o2;
			return i1.intValue() - i2.intValue();
		}
	}

	private Object colorGetCurrent()
	{
		String layerName = (String)colorLayerList.getSelectedValue();
		if (layerName.startsWith("Transparent "))
		{
			int layerNumber = TextUtils.atoi(layerName.substring(12));
			return (GenMath.MutableInteger)transAndSpecialMap.get(new Integer(layerNumber));
		}
		if (layerName.startsWith("Special: "))
		{
			return (GenMath.MutableInteger)transAndSpecialMap.get(layerName);
		}
		Layer layer = curTech.findLayer(layerName);
		if (layer == null) return null;
		ColorPatternPanel.Info li = (ColorPatternPanel.Info)LayersTab.layerMap.get(layer);
		return li;
//		return (GenMath.MutableInteger)colorMap.get(layer);
	}

	private void colorChanged()
	{
		Object colorObj = colorGetCurrent();
		if (colorObj == null) return;
		if (colorObj instanceof GenMath.MutableInteger)
		{
			((GenMath.MutableInteger)colorObj).setValue(colorChooser.getColor().getRGB());
		} else if (colorObj instanceof ColorPatternPanel.Info)
		{
			ColorPatternPanel.Info ci = (ColorPatternPanel.Info)colorObj;
			ci.red = colorChooser.getColor().getRed();
			ci.green = colorChooser.getColor().getGreen();
			ci.blue = colorChooser.getColor().getBlue();
		}
	}

	private void colorClickedLayer()
	{
		Object colorObj = colorGetCurrent();
		if (colorObj == null) return;
		if (colorObj instanceof GenMath.MutableInteger)
		{
			colorChooser.setColor(new Color(((GenMath.MutableInteger)colorObj).intValue()));
		} else  if (colorObj instanceof ColorPatternPanel.Info)
		{
			ColorPatternPanel.Info ci = (ColorPatternPanel.Info)colorObj;
			colorChooser.setColor(new Color(ci.red, ci.green, ci.blue));
		}
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Colors tab.
	 */
	public void term()
	{
		Color [] currentMap = curTech.getColorMap();
		boolean colorChanged = false;
		boolean mapChanged = false;
		Color [] transparentLayerColors = new Color[curTech.getNumTransparentLayers()];

		for(int i=0; i<colorLayerModel.getSize(); i++)
		{
			String layerName = (String)colorLayerModel.get(i);
			if (layerName.startsWith("Transparent "))
			{
				int layerNumber = TextUtils.atoi(layerName.substring(12));
				GenMath.MutableInteger color = (GenMath.MutableInteger)transAndSpecialMap.get(new Integer(layerNumber));
				transparentLayerColors[layerNumber-1] = new Color(color.intValue());
				int mapIndex = 1 << (layerNumber-1);
				int origColor = currentMap[mapIndex].getRGB();
				if (color.intValue() != origColor)
				{
					currentMap[mapIndex] = new Color(color.intValue());
					mapChanged = colorChanged = true;
				}
			} else if (layerName.startsWith("Special: "))
			{
				GenMath.MutableInteger color = (GenMath.MutableInteger)transAndSpecialMap.get(layerName);
				if (layerName.equals("Special: BACKGROUND"))
				{
					if (color.intValue() != User.getColorBackground())
					{
						User.setColorBackground(color.intValue());
						colorChanged = true;
					}
				} else if (layerName.equals("Special: GRID"))
				{
					if (color.intValue() != User.getColorGrid())
					{
						User.setColorGrid(color.intValue());
						colorChanged = true;
					}
				} else if (layerName.equals("Special: HIGHLIGHT"))
				{
					if (color.intValue() != User.getColorHighlight())
					{
						User.setColorHighlight(color.intValue());
						colorChanged = true;
					}
                } else if (layerName.equals("Special: MOUSE-OVER HIGHLIGHT"))
                {
                    if (color.intValue() != User.getColorHighlight())
                    {
                        User.setColorMouseOverHighlight(color.intValue());
                        colorChanged = true;
                    }
				} else if (layerName.equals("Special: PORT HIGHLIGHT"))
				{
					if (color.intValue() != User.getColorPortHighlight())
					{
						User.setColorPortHighlight(color.intValue());
						colorChanged = true;
					}
				} else if (layerName.equals("Special: TEXT"))
				{
					if (color.intValue() != User.getColorText())
					{
						User.setColorText(color.intValue());
						colorChanged = true;
					}
				} else if (layerName.equals("Special: INSTANCE OUTLINES"))
				{
					if (color.intValue() != User.getColorInstanceOutline())
					{
						User.setColorInstanceOutline(color.intValue());
						colorChanged = true;
					}
				} else if (layerName.equals("Special: WAVEFORM BACKGROUND"))
				{
					if (color.intValue() != User.getColorWaveformBackground())
					{
						User.setColorWaveformBackground(color.intValue());
						colorChanged = true;
					}
				} else if (layerName.equals("Special: WAVEFORM FOREGROUND"))
				{
					if (color.intValue() != User.getColorWaveformForeground())
					{
						User.setColorWaveformForeground(color.intValue());
						colorChanged = true;
					}
				} else if (layerName.equals("Special: WAVEFORM STIMULI"))
				{
					if (color.intValue() != User.getColorWaveformStimuli())
					{
						User.setColorWaveformStimuli(color.intValue());
						colorChanged = true;
					}
				} else if (layerName.equals("Special: WAVEFORM OFF STRENGTH"))
				{
					if (color.intValue() != User.getColorWaveformStrengthOff())
					{
						User.setColorWaveformStrengthOff(color.intValue());
						colorChanged = true;
					}
				} else if (layerName.equals("Special: WAVEFORM NODE (WEAK) STRENGTH"))
				{
					if (color.intValue() != User.getColorWaveformStrengthNode())
					{
						User.setColorWaveformStrengthNode(color.intValue());
						colorChanged = true;
					}
				} else if (layerName.equals("Special: WAVEFORM GATE STRENGTH"))
				{
					if (color.intValue() != User.getColorWaveformStrengthGate())
					{
						User.setColorWaveformStrengthGate(color.intValue());
						colorChanged = true;
					}
				} else if (layerName.equals("Special: WAVEFORM POWER STRENGTH"))
				{
					if (color.intValue() != User.getColorWaveformStrengthPower())
					{
						User.setColorWaveformStrengthPower(color.intValue());
						colorChanged = true;
					}
				} else if (layerName.equals("Special: WAVEFORM CROSSPROBE LOW"))
				{
					if (color.intValue() != User.getColorWaveformCrossProbeLow())
					{
						User.setColorWaveformCrossProbeLow(color.intValue());
						colorChanged = true;
					}
				} else if (layerName.equals("Special: WAVEFORM CROSSPROBE HIGH"))
				{
					if (color.intValue() != User.getColorWaveformCrossProbeHigh())
					{
						User.setColorWaveformCrossProbeHigh(color.intValue());
						colorChanged = true;
					}
				} else if (layerName.equals("Special: WAVEFORM CROSSPROBE UNDEFINED"))
				{
					if (color.intValue() != User.getColorWaveformCrossProbeX())
					{
						User.setColorWaveformCrossProbeX(color.intValue());
						colorChanged = true;
					}
				} else if (layerName.equals("Special: WAVEFORM CROSSPROBE FLOATING"))
				{
					if (color.intValue() != User.getColorWaveformCrossProbeZ())
					{
						User.setColorWaveformCrossProbeZ(color.intValue());
						colorChanged = true;
					}
                }

//			} else
//			{
//				Layer layer = curTech.findLayer(layerName);
//				if (layer == null) continue;
//				GenMath.MutableInteger color = (GenMath.MutableInteger)colorMap.get(layer);
//				int origColor = layer.getGraphics().getColor().getRGB();
//				if (color.intValue() != origColor)
//				{
//					layer.getGraphics().setColor(new Color(color.intValue()));
//					colorChanged = true;
//				}
			}
		}

        // 3D Stuff
        try
        {
            Class j3DUtilsClass = Resources.get3DClass("utils.J3DUtils");
            Method setMethod = j3DUtilsClass.getDeclaredMethod("set3DColorsInTab", new Class[] {DefaultListModel.class, HashMap.class});
            Object color3DChanged = setMethod.invoke(j3DUtilsClass, new Object[]{colorLayerModel, transAndSpecialMap});
            if (!colorChanged && color3DChanged != null)
            {
                colorChanged = ((Boolean)color3DChanged).booleanValue();
            }
        } catch (Exception e) {
            System.out.println("Cannot call 3D plugin method set3DColorsInTab: " + e.getMessage());
            e.printStackTrace();
        }

		if (mapChanged)
		{
			// rebuild color map from primaries
			curTech.setColorMapFromLayers(transparentLayerColors);
		}
		if (colorChanged)
		{
			EditWindow.repaintAllContents();
			WindowFrame wf = WindowFrame.getCurrentWindowFrame(false);
			if (wf != null) wf.loadComponentMenuForTechnology();
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

        colors = new javax.swing.JPanel();
        colorChooser = new javax.swing.JColorChooser();
        colorLayerPane = new javax.swing.JScrollPane();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        colors.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        colors.add(colorChooser, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        colors.add(colorLayerPane, gridBagConstraints);

        getContentPane().add(colors, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JColorChooser colorChooser;
    private javax.swing.JScrollPane colorLayerPane;
    private javax.swing.JPanel colors;
    // End of variables declaration//GEN-END:variables

}
