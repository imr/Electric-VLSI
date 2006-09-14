/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: ColorPatternPanel.java
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

package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EGraphics.Outline;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
* A Panel to display color and pattern information.
* Used in the "Layers" tab of the "Edit Options" dialog.
* Used in the "Artwork Look" dialog.
*/
public class ColorPatternPanel extends JPanel
{
	/**
	 * Class to define the information on a color pattern panel.
	 */
	public static class Info
	{
		public EGraphics graphics;
		public int [] pattern;
		public boolean useStippleDisplay;
		public Outline outlinePatternDisplay;
		public boolean useStipplePrinter;
		public int transparentLayer;
		public int red, green, blue;
		public double opacity;
		public boolean justColor;

		/**
		 * Constructor to load a color described by an EGraphics object.
		 */
		public Info(EGraphics graphics)
		{
			this.graphics = graphics;
			this.pattern = new int[16];
			int [] pattern = graphics.getPattern();
			for(int i=0; i<16; i++) this.pattern[i] = pattern[i];
			useStippleDisplay = graphics.isPatternedOnDisplay();
			outlinePatternDisplay = graphics.getOutlined();
			useStipplePrinter = graphics.isPatternedOnPrinter();
			transparentLayer = graphics.getTransparentLayer();
			int color = graphics.getColor().getRGB();
			red = (color >> 16) & 0xFF;
			green = (color >> 8) & 0xFF;
			blue = color & 0xFF;
			opacity = graphics.getOpacity();
			justColor = false;
		}

		/**
		 * Constructor for class to load a pure color.
		 * Used for special colors (like background, etc.)
		 */
		public Info(int color)
		{
			red = (color >> 16) & 0xFF;
			green = (color >> 8) & 0xFF;
			blue = color & 0xFF;
			justColor = true;
		}

		/**
		 * Method to update the EGraphics object that is being displayed in this dialog panel.
		 * @return true if the EGraphics object changed.
		 */
		public boolean updateGraphics(EGraphics setGraphics)
		{
			if (justColor) return false;

			boolean changed = false;

			int [] origPattern = setGraphics.getPattern();
			for(int i=0; i<16; i++) if (pattern[i] != origPattern[i]) changed = true;
			if (changed)
				setGraphics.setPattern(pattern);

			// check the pattern and outline factors
			if (useStippleDisplay != setGraphics.isPatternedOnDisplay())
			{
				setGraphics.setPatternedOnDisplay(useStippleDisplay);
				changed = true;
			}
			if (outlinePatternDisplay != setGraphics.getOutlined())
			{
				setGraphics.setOutlined(outlinePatternDisplay);
				changed = true;
			}
			if (useStipplePrinter != setGraphics.isPatternedOnPrinter())
			{
				setGraphics.setPatternedOnPrinter(useStipplePrinter);
				changed = true;
			}

			// check the color values
			int color = (red << 16) | (green << 8) | blue;
			Color colorObj = null;
			if (color != (setGraphics.getColor().getRGB() & 0xFFFFFF))
			{
				colorObj = new Color(color);
				setGraphics.setColor(colorObj);
				changed = true;
			}
			if (opacity != setGraphics.getOpacity())
			{
				setGraphics.setOpacity(opacity);
				changed = true;
			}
			if (transparentLayer != setGraphics.getTransparentLayer())
			{
				setGraphics.setTransparentLayer(transparentLayer);
				changed = true;
			}
			return changed;
		}

	}

	private PatternView patternView;
	private PatternChoices patternIcon;
	private Info currentLI;
	private boolean dataChanging = false;
	private boolean showPrinter;
	private Color [] colorMap;
	private JColorChooser colorChooser;
	private MyPreviewPanel colorPreviewPanel;
	private boolean warnedOfTransparentLayerSharing;
	private String otherTransparentLayers;

	/**
	 * Create a Panel for editing color and pattern information.
	 */
	public ColorPatternPanel(boolean showPrinter, boolean showFactoryReset)
	{
		initComponents();

		this.showPrinter = showPrinter;
		warnedOfTransparentLayerSharing = false;

		useStipplePatternDisplay.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { layerInfoChanged(); }
		});
		for(Outline o : Outline.getOutlines())
		{
			outlinePattern.addItem(o.getSample());
		}
		outlinePattern.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { layerInfoChanged(); }
		});
		transparentLayer.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { transparentLayerChanged(); }
		});
		transparentLayer.addItem("NOT TRANSPARENT");
		int [] transLayers = EGraphics.getTransparentColorIndices();
		for(int i=0; i<transLayers.length; i++)
			transparentLayer.addItem(EGraphics.getColorIndexName(transLayers[i]));

		patternView = new PatternView(currentLI, useStipplePatternDisplay, outlinePattern);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;       gbc.gridy = 1;
		gbc.gridwidth = 3;   gbc.gridheight = 1;
		gbc.insets = new Insets(0, 4, 4, 4);
		pattern.add(patternView, gbc);

		patternIcon = new PatternChoices();
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 3;
		gbc.gridwidth = 3;   gbc.gridheight = 1;
		gbc.insets = new Insets(0, 4, 2, 4);
		pattern.add(patternIcon, gbc);

		colorChooser = new JColorChooser();
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 1;
		gbc.gridwidth = 2;
		color.add(colorChooser, gbc);
		colorChooser.getSelectionModel().addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent e) { colorChanged(); }
		});
		colorChooser.setPreviewPanel(new JPanel());

		colorPreviewPanel = new MyPreviewPanel(this);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(4, 4, 4, 4);
		color.add(colorPreviewPanel, gbc);

		if (showFactoryReset)
		{
			JButton factoryReset = new JButton("Factory Reset All Layers");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 3;
			gbc.gridwidth = 2;
			gbc.insets = new Insets(4, 4, 4, 4);
			color.add(factoryReset, gbc);
			factoryReset.addActionListener(new ActionListener()
			{
	            public void actionPerformed(ActionEvent evt) { factoryResetActionPerformed(); }
	        });
		}

		if (showPrinter)
		{
			useStipplePatternPrinter.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { layerInfoChanged(); }
			});
			opacity.getDocument().addDocumentListener(new LayerColorDocumentListener());
		} else
		{
			pattern.remove(useStipplePatternPrinter);
			pattern.remove(jLabel1);
			pattern.remove(jLabel2);
			pattern.remove(opacityLabel);
			pattern.remove(opacity);
		}
	}

	/**
	 * Class to provide an alternative preview panel for JColorChooser.
	 */
	public class MyPreviewPanel extends JButton
	{
		private static final int XSIZE = 288;
		private static final int YSIZE = 48;
		private static final int BORDER = 10;
		private ColorPatternPanel dia;
		Color curColor = Color.BLACK;
   
		public MyPreviewPanel(ColorPatternPanel dia)
		{
			this.dia = dia;
			setPreferredSize(new Dimension(XSIZE+BORDER*2, YSIZE+BORDER*2));
		}

		public void setPreviewColor(Color color)
		{
			curColor = color;
		}

		public void paint(Graphics g)
		{
			// clear background
			g.setColor(new Color(User.getColorBackground()));
			g.fillRect(0, 0, getWidth(), getHeight());
			if (currentLI == null) return;

			if (dia.useStipplePatternDisplay.isSelected())
			{
				// stippled: construct an image
				BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
				for(int y=0; y<16; y++)
				{
					int line = currentLI.pattern[y];
					for(int x=0; x<16; x++)
					{
						if ((line & (1<<(15-x))) != 0) im.setRGB(x, y, curColor.getRGB()); else
							im.setRGB(x, y, User.getColorBackground());
					}
				}

				// draw the image to fill the preview
				for(int y=0; y<YSIZE; y+=16)
					for(int x=0; x<XSIZE; x+=16)
						g.drawImage(im, x+BORDER, y+BORDER, null, null);

				// draw an outline if requested
				List<Outline> outlines = Outline.getOutlines();
				Outline o = outlines.get(dia.outlinePattern.getSelectedIndex());
				if (o != Outline.NOPAT)
				{
					g.setColor(curColor);
					for(int t=0; t<o.getThickness(); t++)
					{
						if (o.isSolidPattern())
						{
							g.drawLine(BORDER+t, BORDER+t, XSIZE+BORDER-1-t, BORDER+t);
							g.drawLine(XSIZE+BORDER-1-t, BORDER+t, XSIZE+BORDER-1-t, YSIZE+BORDER-1-t);
							g.drawLine(XSIZE+BORDER-1-t, YSIZE+BORDER-1-t, BORDER+t, YSIZE+BORDER-1-t);
							g.drawLine(BORDER+t, YSIZE+BORDER-1-t, BORDER+t, BORDER+t);
						} else
						{
							int pattern = o.getPattern();
							int len = o.getLen();

							// draw the top and bottom lines
							int patPos = 0;
							for(int x=0; x<XSIZE; x++)
							{
								if ((pattern & (1<<patPos)) != 0)
								{
									g.fillRect(BORDER+x, BORDER+t, 1, 1);
									g.fillRect(BORDER+x, YSIZE+BORDER-1-t, 1, 1);
								}
								patPos++;
								if (patPos >= len) patPos = 0;
							}

							// draw the left and right lines
							patPos = 0;
							for(int y=0; y<YSIZE; y++)
							{
								if ((pattern & (1<<patPos)) != 0)
								{
									g.fillRect(BORDER+t, BORDER+y, 1, 1);
									g.fillRect(XSIZE+BORDER-1-t, BORDER+y, 1, 1);
								}
								patPos++;
								if (patPos >= len) patPos = 0;
							}
						}
					}
				}
			} else
			{
				// not stippled, just fill it
				g.setColor(curColor);
				g.fillRect(BORDER, BORDER, XSIZE, YSIZE);
			}
	   }
   }

	/**
	 * Method to update the panel to reflect the given color map.
	 * @param map the color map to be used.
	 */
	public void setColorMap(Color [] map)
	{
		colorMap = map;
		int curTrans = transparentLayer.getSelectedIndex();
		int [] transLayers = EGraphics.getTransparentColorIndices();
		transparentLayer.removeAllItems();
		transparentLayer.addItem("NOT TRANSPARENT");
		int maxTrans = Math.min(transLayers.length, map.length);
        if (maxTrans > 0)  // zero for generic at least
        {
            for(int i=0; i<maxTrans; i++)
                transparentLayer.addItem(EGraphics.getColorIndexName(transLayers[i]));
            transparentLayer.setSelectedIndex(curTrans);
        }
	}

	public void setOtherTransparentLayerNames(String names) { otherTransparentLayers = names; }

	/**
	 * Method to update the panel to reflect the given Info.
	 * @param li the Info structure with data for this panel.
	 * The Info structure contains color, texture, and other appearance-related factors for drawing.
	 */
	public void setColorPattern(Info li)
	{
		currentLI = li;
		patternView.setLayerInfo(li);
		if (li == null)
		{
			useStipplePatternDisplay.setEnabled(false);
			outlinePattern.setEnabled(false);
			useStipplePatternPrinter.setEnabled(false);
			transparentLayer.setEnabled(false);
			colorChooser.setEnabled(false);
			return;
		}
		colorChooser.setEnabled(true);
		if (li.justColor)
		{
			useStipplePatternDisplay.setEnabled(false);
			outlinePattern.setEnabled(false);
			useStipplePatternPrinter.setEnabled(false);
			transparentLayer.setEnabled(false);
		} else
		{
			useStipplePatternDisplay.setEnabled(true);
			outlinePattern.setEnabled(true);
			useStipplePatternPrinter.setEnabled(true);
			transparentLayer.setEnabled(true);
		}
		dataChanging = true;
		useStipplePatternDisplay.setSelected(li.useStippleDisplay);
		if (li.outlinePatternDisplay != null) // special cases such text don't have outlinePattern
            outlinePattern.setSelectedItem(li.outlinePatternDisplay.getSample());
		outlinePattern.setEnabled(li.useStippleDisplay);
		if (showPrinter)
		{
			useStipplePatternPrinter.setSelected(li.useStipplePrinter);
			opacity.setText(TextUtils.formatDouble(li.opacity));
		}
		transparentLayer.setSelectedIndex(li.transparentLayer);
		Color initialColor = new Color(li.red, li.green, li.blue);
		colorChooser.setColor(initialColor);
		colorPreviewPanel.setPreviewColor(initialColor);
		patternView.repaint();
		colorPreviewPanel.repaint();
		dataChanging = false;
	}

	/**
	 * Method called when the color picker selects a new color.
	 */
	private void colorChanged()
	{
		if (currentLI == null) return;
		Color col = colorChooser.getColor();
		currentLI.red = col.getRed();
		currentLI.green = col.getGreen();
		currentLI.blue = col.getBlue();
		layerInfoChanged();
		colorPreviewPanel.setPreviewColor(col);

		// if there are other transparent layers, warn (first time only)
		if (otherTransparentLayers != null)
		{
			if (!warnedOfTransparentLayerSharing)
				Job.getUserInterface().showInformationMessage("WARNING: changing this color also affects " +
					otherTransparentLayers + " because they share the same transparent layer",
					"Change to Transparent Layer Color");
			warnedOfTransparentLayerSharing = true;
		}
	}

	private void transparentLayerChanged()
	{
		if (currentLI == null) return;
		currentLI.transparentLayer = transparentLayer.getSelectedIndex();
		if (currentLI.transparentLayer > 0 && colorMap != null)
		{
			if (currentLI.transparentLayer > colorMap.length)
			{
				transparentLayer.setSelectedIndex(0);
				return;
			}
			currentLI.red = colorMap[currentLI.transparentLayer-1].getRed();
			currentLI.green = colorMap[currentLI.transparentLayer-1].getGreen();
			currentLI.blue = colorMap[currentLI.transparentLayer-1].getBlue();
			layerInfoChanged();
		}
	}

	private void layerInfoChanged()
	{
		if (dataChanging) return;
		if (currentLI == null) return;
		currentLI.useStippleDisplay = useStipplePatternDisplay.isSelected();
		List<Outline> outlines = Outline.getOutlines();
		currentLI.outlinePatternDisplay = outlines.get(outlinePattern.getSelectedIndex());
		outlinePattern.setEnabled(currentLI.useStippleDisplay);
		if (showPrinter)
		{
			currentLI.useStipplePrinter = useStipplePatternPrinter.isSelected();
		}
		currentLI.transparentLayer = transparentLayer.getSelectedIndex();
//		boolean colorsEnabled = currentLI.transparentLayer == 0;
		currentLI.opacity = TextUtils.atof(opacity.getText());
		Color newColor = new Color(currentLI.red, currentLI.green, currentLI.blue);
		colorChooser.setColor(newColor);
		if (currentLI.transparentLayer != 0)
			colorMap[currentLI.transparentLayer-1] = newColor;
		colorPreviewPanel.repaint();
	}

	public void factoryResetActionPerformed()
	{
	}

	/**
	 * Class to handle special changes to color information.
	 */
	private class LayerColorDocumentListener implements DocumentListener
	{
		LayerColorDocumentListener() {}

		public void changedUpdate(DocumentEvent e) { layerInfoChanged(); }
		public void insertUpdate(DocumentEvent e) { layerInfoChanged(); }
		public void removeUpdate(DocumentEvent e) { layerInfoChanged(); }
	}

	private class PatternView extends JPanel
		implements MouseMotionListener, MouseListener
	{
		private static final int PATSIZE = 13;

		private boolean newState;
		private JCheckBox stipple;
		private JComboBox outline;
		private Info lInfo;

		PatternView(Info lInfo, JCheckBox stipple, JComboBox outline)
		{
			this.lInfo = lInfo;
			this.stipple = stipple;
			this.outline = outline;
			addMouseListener(this);
			addMouseMotionListener(this);
			int totSize = PATSIZE*16+1;
			setMaximumSize(new Dimension(totSize, totSize));
			setMinimumSize(new Dimension(totSize, totSize));
			setPreferredSize(new Dimension(totSize, totSize));
		}

		public void setLayerInfo(Info lInfo) { this.lInfo = lInfo; }

		/**
		 * Method to repaint this PatternView.
		 */
		public void paint(Graphics g)
		{
			Dimension dim = getSize();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, dim.width, dim.height);
			g.setColor(Color.GRAY);
			int upper = PATSIZE * 16;
			for(int i=0; i<=upper; i += PATSIZE)
			{
				g.drawLine(i, 0, i, upper);
				g.drawLine(0, i, upper, i);
			}

			g.setColor(Color.BLACK);
			if (lInfo.justColor) return;
			for(int y=0; y<16; y++)
			{
				int bits = lInfo.pattern[y];
				for(int x=0; x<16; x++)
				{
					if ((bits & (1<<(15-x))) != 0)
					{
						g.fillRect(x*PATSIZE+1, y*PATSIZE+1, PATSIZE-1, PATSIZE-1);
					}
				}
			}
		}

		// the MouseListener events
		public void mousePressed(MouseEvent evt)
		{
			if (lInfo == null || lInfo.justColor) return;
			int xIndex = evt.getX() / PATSIZE;
			int yIndex = evt.getY() / PATSIZE;
			int curWord = lInfo.pattern[yIndex];
			newState = (curWord & (1<<(15-xIndex))) == 0;
			mouseDragged(evt);
		}
		public void mouseReleased(MouseEvent evt) {}
		public void mouseClicked(MouseEvent evt) {}
		public void mouseEntered(MouseEvent evt) {}
		public void mouseExited(MouseEvent evt) {}

		// the MouseMotionListener events
		public void mouseMoved(MouseEvent evt) {}
		public void mouseDragged(MouseEvent evt)
		{
			if (lInfo == null || lInfo.justColor) return;
			int xIndex = evt.getX() / PATSIZE;
			int yIndex = evt.getY() / PATSIZE;
			if (xIndex < 0 || yIndex < 0 || xIndex >= 16 || yIndex >= 16) return;
			int curWord = lInfo.pattern[yIndex];
			if ((curWord & (1<<(15-xIndex))) != 0)
			{
				if (newState) return;
				curWord &= ~(1<<(15-xIndex));
			} else
			{
				if (!newState) return;
				curWord |= 1<<(15-xIndex);
			}
			lInfo.pattern[yIndex] = curWord;

			// fake a check in the stipple use
			stipple.setSelected(true);
			outline.setEnabled(true);
			lInfo.useStippleDisplay = true;
			repaint();
			colorPreviewPanel.repaint();
		}
	}

	private static final int [] preDefinedPatterns =
	{
		0x8888,  // X   X   X   X   
		0x4444,  //  X   X   X   X  
		0x2222,  //   X   X   X   X 
		0x1111,  //    X   X   X   X
		0x8888,  // X   X   X   X   
		0x4444,  //  X   X   X   X  
		0x2222,  //   X   X   X   X 
		0x1111,  //    X   X   X   X
		0x8888,  // X   X   X   X   
		0x4444,  //  X   X   X   X  
		0x2222,  //   X   X   X   X 
		0x1111,  //    X   X   X   X
		0x8888,  // X   X   X   X   
		0x4444,  //  X   X   X   X  
		0x2222,  //   X   X   X   X 
		0x1111,  //    X   X   X   X

		0x8888,  // X   X   X   X   
		0x1111,  //    X   X   X   X
		0x2222,  //   X   X   X   X 
		0x4444,  //  X   X   X   X  
		0x8888,  // X   X   X   X   
		0x1111,  //    X   X   X   X
		0x2222,  //   X   X   X   X 
		0x4444,  //  X   X   X   X  
		0x8888,  // X   X   X   X   
		0x1111,  //    X   X   X   X
		0x2222,  //   X   X   X   X 
		0x4444,  //  X   X   X   X  
		0x8888,  // X   X   X   X   
		0x1111,  //    X   X   X   X
		0x2222,  //   X   X   X   X 
		0x4444,  //  X   X   X   X  

		0xCCCC,  // XX  XX  XX  XX  
		0xCCCC,  // XX  XX  XX  XX  
		0x3333,  //   XX  XX  XX  XX
		0x3333,  //   XX  XX  XX  XX
		0xCCCC,  // XX  XX  XX  XX  
		0xCCCC,  // XX  XX  XX  XX  
		0x3333,  //   XX  XX  XX  XX
		0x3333,  //   XX  XX  XX  XX
		0xCCCC,  // XX  XX  XX  XX  
		0xCCCC,  // XX  XX  XX  XX  
		0x3333,  //   XX  XX  XX  XX
		0x3333,  //   XX  XX  XX  XX
		0xCCCC,  // XX  XX  XX  XX  
		0xCCCC,  // XX  XX  XX  XX  
		0x3333,  //   XX  XX  XX  XX
		0x3333,  //   XX  XX  XX  XX

		0xFFFF,  // XXXXXXXXXXXXXXXX
		0x0000,  //                 
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0x0000,  //                 
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0x0000,  //                 
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0x0000,  //                 
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0x0000,  //                 
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0x0000,  //                 
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0x0000,  //                 
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0x0000,  //                 

		0xAAAA,  // X X X X X X X X 
		0xAAAA,  // X X X X X X X X 
		0xAAAA,  // X X X X X X X X 
		0xAAAA,  // X X X X X X X X 
		0xAAAA,  // X X X X X X X X 
		0xAAAA,  // X X X X X X X X 
		0xAAAA,  // X X X X X X X X 
		0xAAAA,  // X X X X X X X X 
		0xAAAA,  // X X X X X X X X 
		0xAAAA,  // X X X X X X X X 
		0xAAAA,  // X X X X X X X X 
		0xAAAA,  // X X X X X X X X 
		0xAAAA,  // X X X X X X X X 
		0xAAAA,  // X X X X X X X X 
		0xAAAA,  // X X X X X X X X 
		0xAAAA,  // X X X X X X X X 

		0x6060,  //  XX      XX     
		0x9090,  // X  X    X  X    
		0x9090,  // X  X    X  X    
		0x6060,  //  XX      XX     
		0x0606,  //      XX      XX 
		0x0909,  //     X  X    X  X
		0x0909,  //     X  X    X  X
		0x0606,  //      XX      XX 
		0x6060,  //  XX      XX     
		0x9090,  // X  X    X  X    
		0x9090,  // X  X    X  X    
		0x6060,  //  XX      XX     
		0x0606,  //      XX      XX 
		0x0909,  //     X  X    X  X
		0x0909,  //     X  X    X  X
		0x0606,  //      XX      XX 

		0x2222,  //   X   X   X   X 
		0x0000,  //                 
		0x8888,  // X   X   X   X   
		0x0000,  //                 
		0x2222,  //   X   X   X   X 
		0x0000,  //                 
		0x8888,  // X   X   X   X   
		0x0000,  //                 
		0x2222,  //   X   X   X   X 
		0x0000,  //                 
		0x8888,  // X   X   X   X   
		0x0000,  //                 
		0x2222,  //   X   X   X   X 
		0x0000,  //                 
		0x8888,  // X   X   X   X   
		0x0000,  //                 

		0x4444,  //  X   X   X   X  
		0x1111,  //    X   X   X   X
		0x4444,  //  X   X   X   X  
		0x1111,  //    X   X   X   X
		0x4444,  //  X   X   X   X  
		0x1111,  //    X   X   X   X
		0x4444,  //  X   X   X   X  
		0x1111,  //    X   X   X   X
		0x4444,  //  X   X   X   X  
		0x1111,  //    X   X   X   X
		0x4444,  //  X   X   X   X  
		0x1111,  //    X   X   X   X
		0x4444,  //  X   X   X   X  
		0x1111,  //    X   X   X   X
		0x4444,  //  X   X   X   X  
		0x1111,  //    X   X   X   X

		0x1010,  //    X       X    
		0x2020,  //   X       X     
		0x4040,  //  X       X      
		0x8080,  // X       X       
		0x0101,  //        X       X
		0x0202,  //       X       X 
		0x0404,  //      X       X  
		0x0808,  //     X       X   
		0x1010,  //    X       X    
		0x2020,  //   X       X     
		0x4040,  //  X       X      
		0x8080,  // X       X       
		0x0101,  //        X       X
		0x0202,  //       X       X 
		0x0404,  //      X       X  
		0x0808,  //     X       X   

		0x0808,  //     X       X   
		0x0404,  //      X       X  
		0x0202,  //       X       X 
		0x0101,  //        X       X
		0x8080,  // X       X       
		0x4040,  //  X       X      
		0x2020,  //   X       X     
		0x1010,  //    X       X    
		0x0808,  //     X       X   
		0x0404,  //      X       X  
		0x0202,  //       X       X 
		0x0101,  //        X       X
		0x8080,  // X       X       
		0x4040,  //  X       X      
		0x2020,  //   X       X     
		0x1010,  //    X       X    

		0x4040,  //  X       X      
		0x8080,  // X       X       
		0x0101,  //        X       X
		0x0202,  //       X       X 
		0x0101,  //        X       X
		0x8080,  // X       X       
		0x4040,  //  X       X      
		0x2020,  //   X       X     
		0x4040,  //  X       X      
		0x8080,  // X       X       
		0x0101,  //        X       X
		0x0202,  //       X       X 
		0x0101,  //        X       X
		0x8080,  // X       X       
		0x4040,  //  X       X      
		0x2020,  //   X       X     

		0x2020,  //   X       X     
		0x0000,  //                 
		0x8080,  // X       X       
		0x0000,  //                 
		0x0202,  //       X       X 
		0x0000,  //                 
		0x0808,  //     X       X   
		0x0000,  //                 
		0x2020,  //   X       X     
		0x0000,  //                 
		0x8080,  // X       X       
		0x0000,  //                 
		0x0202,  //       X       X 
		0x0000,  //                 
		0x0808,  //     X       X   
		0x0000,  //                 

		0x0808,  //     X       X   
		0x0000,  //                 
		0x0202,  //       X       X 
		0x0000,  //                 
		0x8080,  // X       X       
		0x0000,  //                 
		0x2020,  //   X       X     
		0x0000,  //                 
		0x0808,  //     X       X   
		0x0000,  //                 
		0x0202,  //       X       X 
		0x0000,  //                 
		0x8080,  // X       X       
		0x0000,  //                 
		0x2020,  //   X       X     
		0x0000,  //                 

		0x0000,  //                 
		0x0303,  //       XX      XX
		0x4848,  //  X  X    X  X   
		0x0303,  //       XX      XX
		0x0000,  //                 
		0x3030,  //   XX      XX    
		0x8484,  // X    X  X    X  
		0x3030,  //   XX      XX    
		0x0000,  //                 
		0x0303,  //       XX      XX
		0x4848,  //  X  X    X  X   
		0x0303,  //       XX      XX
		0x0000,  //                 
		0x3030,  //   XX      XX    
		0x8484,  // X    X  X    X  
		0x3030,  //   XX      XX    

		0x1C1C,  //    XXX     XXX  
		0x3E3E,  //   XXXXX   XXXXX 
		0x3636,  //   XX XX   XX XX 
		0x3E3E,  //   XXXXX   XXXXX 
		0x1C1C,  //    XXX     XXX  
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x1C1C,  //    XXX     XXX  
		0x3E3E,  //   XXXXX   XXXXX 
		0x3636,  //   XX XX   XX XX 
		0x3E3E,  //   XXXXX   XXXXX 
		0x1C1C,  //    XXX     XXX  
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 

		0x0000,  //                 
		0xCCCC,  // XX  XX  XX  XX  
		0x0000,  //                 
		0xCCCC,  // XX  XX  XX  XX  
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0xCCCC,  // XX  XX  XX  XX  
		0x0000,  //                 
		0xCCCC,  // XX  XX  XX  XX  
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 

		0x0000,  //                 
		0x0000,  //                 
		0x2222,  //   X   X   X   X 
		0x8888,  // X   X   X   X   
		0x0000,  //                 
		0x0000,  //                 
		0x2222,  //   X   X   X   X 
		0x8888,  // X   X   X   X   
		0x0000,  //                 
		0x0000,  //                 
		0x2222,  //   X   X   X   X 
		0x8888,  // X   X   X   X   
		0x0000,  //                 
		0x0000,  //                 
		0x2222,  //   X   X   X   X 
		0x8888,  // X   X   X   X   

		0x0000,  //                 
		0x0000,  //                 
		0x1111,  //    X   X   X   X
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x1111,  //    X   X   X   X
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x1111,  //    X   X   X   X
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x1111,  //    X   X   X   X
		0x0000,  //                 

		0x0000,  //                 
		0x2222,  //   X   X   X   X 
		0x4444,  //  X   X   X   X  
		0x8888,  // X   X   X   X   
		0x0000,  //                 
		0x2222,  //   X   X   X   X 
		0x4444,  //  X   X   X   X  
		0x8888,  // X   X   X   X   
		0x0000,  //                 
		0x2222,  //   X   X   X   X 
		0x4444,  //  X   X   X   X  
		0x8888,  // X   X   X   X   
		0x0000,  //                 
		0x2222,  //   X   X   X   X 
		0x4444,  //  X   X   X   X  
		0x8888,  // X   X   X   X   

		0x0000,  //                 
		0x2222,  //   X   X   X   X 
		0x5555,  //  X X X X X X X X
		0x2222,  //   X   X   X   X 
		0x0000,  //                 
		0x2222,  //   X   X   X   X 
		0x5555,  //  X X X X X X X X
		0x2222,  //   X   X   X   X 
		0x0000,  //                 
		0x2222,  //   X   X   X   X 
		0x5555,  //  X X X X X X X X
		0x2222,  //   X   X   X   X 
		0x0000,  //                 
		0x2222,  //   X   X   X   X 
		0x5555,  //  X X X X X X X X
		0x2222,  //   X   X   X   X 

		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 
		0x0000,  //                 

		0xFFFF,  // XXXXXXXXXXXXXXXX
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0xFFFF,  // XXXXXXXXXXXXXXXX
		0xFFFF   // XXXXXXXXXXXXXXXX
	};

	private class PatternChoices extends JPanel
		implements MouseListener
	{
		private static final int NUMROWS = 2;
        private static final int ROWSIZE = 17;
		int numPatterns = preDefinedPatterns.length / 16;
        int yEntry = -1, xEntry = -1;

		PatternChoices()
		{
			addMouseListener(this);
			setMaximumSize(new Dimension(numPatterns*ROWSIZE/NUMROWS+1, ROWSIZE*NUMROWS+1));
			setMinimumSize(new Dimension(numPatterns*ROWSIZE/NUMROWS+1, ROWSIZE*NUMROWS+1));
			setPreferredSize(new Dimension(numPatterns*ROWSIZE/NUMROWS+1, ROWSIZE*NUMROWS+1));
		}

		/**
		 * Method to repaint this PatternChoices.
		 */
		public void paint(Graphics g)
		{
			ImageIcon icon = Resources.getResource(getClass(), "IconLayerPatterns.gif");
			g.drawImage(icon.getImage(), 0, 0, null);
            if (yEntry != -1 && xEntry != -1)
            {
                g.setColor(Color.BLACK);
                // Simulating thick lines
                g.drawRect(xEntry*ROWSIZE-1, yEntry*ROWSIZE-1, ROWSIZE+1, ROWSIZE+1);
                g.drawRect(xEntry*ROWSIZE, yEntry*ROWSIZE, ROWSIZE, ROWSIZE);
                g.drawRect(xEntry*ROWSIZE+1, yEntry*ROWSIZE+1, ROWSIZE-1, ROWSIZE-1);
            }
		}

		// the MouseListener events
		public void mousePressed(MouseEvent evt)
		{
			if (currentLI == null || currentLI.justColor) return;
			xEntry = evt.getX() / ROWSIZE;
			if (xEntry >= numPatterns/NUMROWS) xEntry = numPatterns/NUMROWS-1;
			yEntry = evt.getY() / ROWSIZE;
			if (yEntry >= NUMROWS) yEntry = NUMROWS-1;
			int iconIndex = xEntry + 11*yEntry;
			for(int i=0; i<16; i++)
			{
				currentLI.pattern[i] = preDefinedPatterns[iconIndex*16+i];
			}

			// fake a check in the stipple use
			useStipplePatternDisplay.setSelected(true);
			outlinePattern.setEnabled(true);
			currentLI.useStippleDisplay = true;

            paint(this.getGraphics());
			patternView.repaint();
			colorPreviewPanel.repaint();
		}
		public void mouseReleased(MouseEvent evt) {}
		public void mouseClicked(MouseEvent evt) {}
		public void mouseEntered(MouseEvent evt) {}
		public void mouseExited(MouseEvent evt) {}
	}

   /** This method is called from within the constructor to
    * initialize the form.
    * WARNING: Do NOT modify this code. The content of this method is
    * always regenerated by the Form Editor.
    */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        java.awt.GridBagConstraints gridBagConstraints;

        pattern = new javax.swing.JPanel();
        jLabel50 = new javax.swing.JLabel();
        useStipplePatternDisplay = new javax.swing.JCheckBox();
        outlinePattern = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        opacityLabel = new javax.swing.JLabel();
        opacity = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        useStipplePatternPrinter = new javax.swing.JCheckBox();
        color = new javax.swing.JPanel();
        jLabel40 = new javax.swing.JLabel();
        transparentLayer = new javax.swing.JComboBox();

        setLayout(new java.awt.GridBagLayout());

        pattern.setLayout(new java.awt.GridBagLayout());

        pattern.setBorder(javax.swing.BorderFactory.createTitledBorder("Pattern"));
        jLabel50.setText("Click on a pattern below to use it above");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        pattern.add(jLabel50, gridBagConstraints);

        useStipplePatternDisplay.setText("Use Fill Pattern on Screen");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        pattern.add(useStipplePatternDisplay, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        pattern.add(outlinePattern, gridBagConstraints);

        jLabel3.setText("Outline pattern:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        pattern.add(jLabel3, gridBagConstraints);

        opacityLabel.setText("Opacity:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        pattern.add(opacityLabel, gridBagConstraints);

        opacity.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        pattern.add(opacity, gridBagConstraints);

        jLabel1.setText("0: Transparent");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        pattern.add(jLabel1, gridBagConstraints);

        jLabel2.setText("1: Opaque");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        pattern.add(jLabel2, gridBagConstraints);

        useStipplePatternPrinter.setText("Use Fill Pattern on Printer");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        pattern.add(useStipplePatternPrinter, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        add(pattern, gridBagConstraints);

        color.setLayout(new java.awt.GridBagLayout());

        color.setBorder(javax.swing.BorderFactory.createTitledBorder("Color"));
        jLabel40.setText("Transparency:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        color.add(jLabel40, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        color.add(transparentLayer, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        add(color, gridBagConstraints);

    }// </editor-fold>//GEN-END:initComponents
   
   
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel color;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JTextField opacity;
    private javax.swing.JLabel opacityLabel;
    private javax.swing.JComboBox outlinePattern;
    private javax.swing.JPanel pattern;
    private javax.swing.JComboBox transparentLayer;
    private javax.swing.JCheckBox useStipplePatternDisplay;
    private javax.swing.JCheckBox useStipplePatternPrinter;
    // End of variables declaration//GEN-END:variables
   
}
