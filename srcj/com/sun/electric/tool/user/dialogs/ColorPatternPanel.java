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
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Resources;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Iterator;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A Panel to display Color and patterns.
 * Used in the "Layers" tab of the "Edit Options" dialog.
 * Used in the "Artwork Look" dialog.
 */
public class ColorPatternPanel extends JPanel
{
	public static class Info
	{
		public EGraphics graphics;
		public int [] pattern;
		public boolean useStippleDisplay;
		public boolean outlinePatternDisplay;
		public boolean useStipplePrinter;
		public boolean outlinePatternPrinter;
		public int transparentLayer;
		public int red, green, blue;
		public double opacity;

		/**
		 * Constructor for class to edit an EGraphics object manage in a dialog panel.
		 * @param graphics to EGraphics to manage.
		 * It will be modified by the dialog.
		 */
		public Info(EGraphics graphics)
		{
			this.graphics = graphics;
			this.pattern = new int[16];
			int [] pattern = graphics.getPattern();
			for(int i=0; i<16; i++) this.pattern[i] = pattern[i];
			useStippleDisplay = graphics.isPatternedOnDisplay();
			outlinePatternDisplay = graphics.isOutlinedOnDisplay();
			useStipplePrinter = graphics.isPatternedOnPrinter();
			outlinePatternPrinter = graphics.isOutlinedOnPrinter();
			transparentLayer = graphics.getTransparentLayer();
			int color = graphics.getColor().getRGB();
			red = (color >> 16) & 0xFF;
			green = (color >> 8) & 0xFF;
			blue = color & 0xFF;
			opacity = graphics.getOpacity();
		}

		/**
		 * Method to update the EGraphics object that is being displayed in this dialog panel.
		 * @return true if the EGraphics object changed.
		 */
		public boolean updateGraphics()
		{
			boolean changed = false;

			int [] origPattern = graphics.getPattern();
			for(int i=0; i<16; i++) if (pattern[i] != origPattern[i]) changed = true;
			if (changed)
			{
				graphics.setPattern(pattern);
			}

			// check the pattern and outline factors
			if (useStippleDisplay != graphics.isPatternedOnDisplay())
			{
				graphics.setPatternedOnDisplay(useStippleDisplay);
				changed = true;
			}
			if (outlinePatternDisplay != graphics.isOutlinedOnDisplay())
			{
				graphics.setOutlinedOnDisplay(outlinePatternDisplay);
				changed = true;
			}
			if (useStipplePrinter != graphics.isPatternedOnPrinter())
			{
				graphics.setPatternedOnPrinter(useStipplePrinter);
				changed = true;
			}
			if (outlinePatternPrinter != graphics.isOutlinedOnPrinter())
			{
				graphics.setOutlinedOnPrinter(outlinePatternPrinter);
				changed = true;
			}

			// check the color values
			int color = (red << 16) | (green << 8) | blue;
			if (color != (graphics.getColor().getRGB() & 0xFFFFFF))
			{
//System.out.println("Color changed to 0x"+Integer.toHexString(color)+" on "+graphics);
				graphics.setColor(new Color(color));
				changed = true;
			}
			if (opacity != graphics.getOpacity())
			{
				graphics.setOpacity(opacity);
				changed = true;
			}
			if (transparentLayer != graphics.getTransparentLayer())
			{
				graphics.setTransparentLayer(transparentLayer);
				changed = true;
			}
			return changed;
		}

	}

	private JPanel patternView, patternIcon;
	private Info currentLI;
	private boolean dataChanging = false;
	private boolean showPrinter;

    /**
     * Create a Panel for editing color and pattern information.
     */
    public ColorPatternPanel(boolean showPrinter)
	{
        initComponents();
        this.showPrinter = showPrinter;

		int [] colors = EGraphics.getTransparentColorIndices();
		transparentLayer.addItem("Not Transparent");
		for(int i=0; i<colors.length; i++)
			transparentLayer.addItem(EGraphics.getColorIndexName(colors[i]));

		useStipplePatternDisplay.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { layerInfoChanged(); }
		});
		useOutlinePatternDisplay.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { layerInfoChanged(); }
		});
		transparentLayer.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { layerInfoChanged(); }
		});
		pick.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { colorPick(); }
		});
		layerRed.getDocument().addDocumentListener(new LayerColorDocumentListener());
		layerGreen.getDocument().addDocumentListener(new LayerColorDocumentListener());
		layerBlue.getDocument().addDocumentListener(new LayerColorDocumentListener());

		patternView = new PatternView();
		patternView.setMaximumSize(new java.awt.Dimension(257, 257));
		patternView.setMinimumSize(new java.awt.Dimension(257, 257));
		patternView.setPreferredSize(new java.awt.Dimension(257, 257));
		java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
		gbc.gridx = 0;       gbc.gridy = 1;
		gbc.gridwidth = 8;   gbc.gridheight = 1;
		gbc.insets = new java.awt.Insets(0, 4, 4, 4);
		appearance.add(patternView, gbc);

		patternIcon = new PatternChoices();
		patternIcon.setMaximumSize(new java.awt.Dimension(352, 16));
		patternIcon.setMinimumSize(new java.awt.Dimension(352, 16));
		patternIcon.setPreferredSize(new java.awt.Dimension(352, 16));
		gbc = new java.awt.GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 3;
		gbc.gridwidth = 8;   gbc.gridheight = 1;
		gbc.insets = new java.awt.Insets(0, 4, 2, 4);
		appearance.add(patternIcon, gbc);
 
 		if (showPrinter)
 		{
			useOutlinePatternPrinter.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { layerInfoChanged(); }
			});
			useStipplePatternPrinter.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { layerInfoChanged(); }
			});
			opacity.getDocument().addDocumentListener(new LayerColorDocumentListener());
 		} else
 		{
			remove(forPrinter);
 		}
   }

	/**
	 * Method to update the panel to reflect the given Info.
	 * @param li the Info structure with data for this panel.
	 * The Info structure contains color, texture, and other appearance-related factors for drawing.
	 */
	public void setColorPattern(Info li)
	{
		currentLI = li;
		dataChanging = true;
		useStipplePatternDisplay.setSelected(li.useStippleDisplay);
		useOutlinePatternDisplay.setSelected(li.outlinePatternDisplay);
		if (showPrinter)
		{
			useStipplePatternPrinter.setSelected(li.useStipplePrinter);
			useOutlinePatternPrinter.setSelected(li.outlinePatternPrinter);
			opacity.setText(TextUtils.formatDouble(li.opacity));
		}
		transparentLayer.setSelectedIndex(li.transparentLayer);
		layerRed.setText(Integer.toString(li.red));
		layerGreen.setText(Integer.toString(li.green));
		layerBlue.setText(Integer.toString(li.blue));
		if (li.transparentLayer == 0)
		{
			// a pure color
			pick.setEnabled(true);
			layerRedLabel.setEnabled(true);
			layerRed.setEnabled(true);
			layerGreenLabel.setEnabled(true);
			layerGreen.setEnabled(true);
			layerBlueLabel.setEnabled(true);
			layerBlue.setEnabled(true);
		} else
		{
			// a transparent color
			layerRedLabel.setEnabled(false);
			layerRed.setEnabled(false);
			layerGreenLabel.setEnabled(false);
			layerGreen.setEnabled(false);
			layerBlueLabel.setEnabled(false);
			layerBlue.setEnabled(false);
			pick.setEnabled(false);
		}
		patternView.repaint();
		dataChanging = false;
	}

	private void colorPick()
	{
		Color newColor = JColorChooser.showDialog(this, "Pick color", new Color(currentLI.red, currentLI.green, currentLI.blue));
		if (newColor == null) return;
		currentLI.red = newColor.getRed();
		currentLI.green = newColor.getGreen();
		currentLI.blue = newColor.getBlue();
		setColorPattern(currentLI);
	}

	private void layerInfoChanged()
	{
		if (dataChanging) return;
		if (currentLI == null) return;
		currentLI.useStippleDisplay = useStipplePatternDisplay.isSelected();
		currentLI.outlinePatternDisplay = useOutlinePatternDisplay.isSelected();
		if (showPrinter)
		{
			currentLI.useStipplePrinter = useStipplePatternPrinter.isSelected();
			currentLI.outlinePatternPrinter = useOutlinePatternPrinter.isSelected();
		}
		currentLI.transparentLayer = transparentLayer.getSelectedIndex();
		boolean colorsEnabled = currentLI.transparentLayer == 0;
		pick.setEnabled(colorsEnabled);
		layerRedLabel.setEnabled(colorsEnabled);
		layerRed.setEnabled(colorsEnabled);
		layerGreenLabel.setEnabled(colorsEnabled);
		layerGreen.setEnabled(colorsEnabled);
		layerBlueLabel.setEnabled(colorsEnabled);
		layerBlue.setEnabled(colorsEnabled);
		currentLI.red = TextUtils.atoi(layerRed.getText());
		currentLI.green = TextUtils.atoi(layerGreen.getText());
		currentLI.blue = TextUtils.atoi(layerBlue.getText());
		currentLI.opacity = TextUtils.atof(opacity.getText());
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
		boolean newState;

		PatternView()
		{
			addMouseListener(this);
			addMouseMotionListener(this);
		}

		/**
		 * Method to repaint this PatternView.
		 */
		public void paint(Graphics g)
		{
			Dimension dim = getSize();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, dim.width, dim.height);
			g.setColor(Color.GRAY);
			for(int i=0; i<=256; i += 16)
			{
				g.drawLine(i, 0, i, 256);
				g.drawLine(0, i, 256, i);
			}

			g.setColor(Color.BLACK);
			for(int y=0; y<16; y++)
			{
				int bits = currentLI.pattern[y];
				for(int x=0; x<16; x++)
				{
					if ((bits & (1<<(15-x))) != 0)
					{
						g.fillRect(x*16+1, y*16+1, 15, 15);
					}
				}
			}
		}

		// the MouseListener events
		public void mousePressed(MouseEvent evt)
		{
			int xIndex = evt.getX() / 16;
			int yIndex = evt.getY() / 16;
			int curWord = currentLI.pattern[yIndex];
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
			int xIndex = evt.getX() / 16;
			int yIndex = evt.getY() / 16;
			int curWord = currentLI.pattern[yIndex];
			if ((curWord & (1<<(15-xIndex))) != 0)
			{
				if (newState) return;
				curWord &= ~(1<<(15-xIndex));
			} else
			{
				if (!newState) return;
				curWord |= 1<<(15-xIndex);
			}
			currentLI.pattern[yIndex] = curWord;
			repaint();
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
		PatternChoices()
		{
			addMouseListener(this);
		}

		/**
		 * Method to repaint this PatternChoices.
		 */
		public void paint(Graphics g)
		{
			ImageIcon icon = Resources.getResource(getClass(), "IconLayerPatterns.gif");
			g.drawImage(icon.getImage(), 0, 0, null);
		}

		// the MouseListener events
		public void mousePressed(MouseEvent evt)
		{
			int iconIndex = evt.getX() / 16;
			for(int i=0; i<16; i++)
			{
				currentLI.pattern[i] = preDefinedPatterns[iconIndex*16+i];
			}
			patternView.repaint();
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
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        forDisplay = new javax.swing.JPanel();
        jLabel40 = new javax.swing.JLabel();
        transparentLayer = new javax.swing.JComboBox();
        useStipplePatternDisplay = new javax.swing.JCheckBox();
        useOutlinePatternDisplay = new javax.swing.JCheckBox();
        forPrinter = new javax.swing.JPanel();
        useStipplePatternPrinter = new javax.swing.JCheckBox();
        useOutlinePatternPrinter = new javax.swing.JCheckBox();
        opacityLabel = new javax.swing.JLabel();
        opacity = new javax.swing.JTextField();
        opacityExplanation = new javax.swing.JLabel();
        appearance = new javax.swing.JPanel();
        layerGreenLabel = new javax.swing.JLabel();
        layerBlueLabel = new javax.swing.JLabel();
        layerGreen = new javax.swing.JTextField();
        layerRed = new javax.swing.JTextField();
        layerRedLabel = new javax.swing.JLabel();
        layerBlue = new javax.swing.JTextField();
        jLabel50 = new javax.swing.JLabel();
        pick = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        forDisplay.setLayout(new java.awt.GridBagLayout());

        forDisplay.setBorder(new javax.swing.border.TitledBorder("When Displayed"));
        jLabel40.setText("Transparent layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        forDisplay.add(jLabel40, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        forDisplay.add(transparentLayer, gridBagConstraints);

        useStipplePatternDisplay.setText("Use Stipple Pattern");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 0, 4);
        forDisplay.add(useStipplePatternDisplay, gridBagConstraints);

        useOutlinePatternDisplay.setText("Outline Pattern");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 0, 4);
        forDisplay.add(useOutlinePatternDisplay, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        add(forDisplay, gridBagConstraints);

        forPrinter.setLayout(new java.awt.GridBagLayout());

        forPrinter.setBorder(new javax.swing.border.TitledBorder("When Printed"));
        useStipplePatternPrinter.setText("Use Stipple Pattern");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        forPrinter.add(useStipplePatternPrinter, gridBagConstraints);

        useOutlinePatternPrinter.setText("Outline Pattern");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 0, 4);
        forPrinter.add(useOutlinePatternPrinter, gridBagConstraints);

        opacityLabel.setText("Opacity:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        forPrinter.add(opacityLabel, gridBagConstraints);

        opacity.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.weightx = 1.0;
        forPrinter.add(opacity, gridBagConstraints);

        opacityExplanation.setText("(0 is Transparent; 1 is Opaque)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        forPrinter.add(opacityExplanation, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        add(forPrinter, gridBagConstraints);

        appearance.setLayout(new java.awt.GridBagLayout());

        appearance.setBorder(new javax.swing.border.TitledBorder("Color and Pattern"));
        layerGreenLabel.setText("Green:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        appearance.add(layerGreenLabel, gridBagConstraints);

        layerBlueLabel.setText("Blue:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        appearance.add(layerBlueLabel, gridBagConstraints);

        layerGreen.setColumns(5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.3;
        appearance.add(layerGreen, gridBagConstraints);

        layerRed.setColumns(5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.3;
        appearance.add(layerRed, gridBagConstraints);

        layerRedLabel.setText("Red:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        appearance.add(layerRedLabel, gridBagConstraints);

        layerBlue.setColumns(5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.3;
        appearance.add(layerBlue, gridBagConstraints);

        jLabel50.setText("Click on a pattern below  to use it above::");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        appearance.add(jLabel50, gridBagConstraints);

        pick.setText("Pick");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 4);
        appearance.add(pick, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        add(appearance, gridBagConstraints);

    }//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel appearance;
    private javax.swing.JPanel forDisplay;
    private javax.swing.JPanel forPrinter;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JTextField layerBlue;
    private javax.swing.JLabel layerBlueLabel;
    private javax.swing.JTextField layerGreen;
    private javax.swing.JLabel layerGreenLabel;
    private javax.swing.JTextField layerRed;
    private javax.swing.JLabel layerRedLabel;
    private javax.swing.JTextField opacity;
    private javax.swing.JLabel opacityExplanation;
    private javax.swing.JLabel opacityLabel;
    private javax.swing.JButton pick;
    private javax.swing.JComboBox transparentLayer;
    private javax.swing.JCheckBox useOutlinePatternDisplay;
    private javax.swing.JCheckBox useOutlinePatternPrinter;
    private javax.swing.JCheckBox useStipplePatternDisplay;
    private javax.swing.JCheckBox useStipplePatternPrinter;
    // End of variables declaration//GEN-END:variables
    
}
