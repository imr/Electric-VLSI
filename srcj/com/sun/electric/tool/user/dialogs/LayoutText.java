/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayoutText.java
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
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.PixelDrawing;

import java.awt.Font;
import java.awt.Image;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;


/**
 * Class to handle the "Layout Text" dialog.
 */
public class LayoutText extends javax.swing.JDialog
{
	private static int lastSize = 12;
	private static double lastScale = 1;
	private static double lastSeparation = 0;
	private static boolean lastItalic = false;
	private static boolean lastBold = false;
	private static boolean lastUnderline = false;
	private static String lastFont = "sansserif.plain";
	private static String lastLayer = null;
	private static String lastMessage = null;

	/** Creates new form Layout Text */
	public LayoutText(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();
        getRootPane().setDefaultButton(ok);

		textSize.setText(Integer.toString(lastSize));
		textScale.setText(Double.toString(lastScale));
		dotSeparation.setText(Double.toString(lastSeparation));

		textItalic.setSelected(lastItalic);
		textBold.setSelected(lastBold);
		textUnderline.setSelected(lastUnderline);

		for(Iterator it = Technology.getCurrent().getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			if (np.getFunction() == NodeProto.Function.NODE)
				textLayer.addItem(np.getProtoName());
		}
		if (lastLayer != null)
			textLayer.setSelectedItem(lastLayer);

		if (lastMessage != null)
			textMessage.setText(lastMessage);

		Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		for(int i=0; i<fonts.length; i++)
			textFont.addItem(fonts[i].getFontName());
		if (lastFont != null)
			textFont.setSelectedItem(lastFont);

		// have fields update the message display
		textFont.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { updateMessageField(); }
		});
		textItalic.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { updateMessageField(); }
		});
		textBold.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { updateMessageField(); }
		});
		textSize.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { updateMessageField(); }
		});
	}

	/**
	 * Method called when a dialog field has changed, and the message area must be redisplayed.
	 */
	private void updateMessageField()
	{
		String fontName = (String)textFont.getSelectedItem();
		int fontStyle = Font.PLAIN;
		if (textItalic.isSelected()) fontStyle |= Font.ITALIC;
		if (textBold.isSelected()) fontStyle |= Font.BOLD;
		int size = TextUtils.atoi(textSize.getText());
		Font theFont = new Font(fontName, fontStyle, size);
		if (theFont != null)
			textMessage.setFont(theFont);
	}

	/*
	 * Houtine to convert the text in "msg" to bits on the display.
	 */
	private void makeLayoutText(String layer, int tsize, double scale, String font, boolean italic,
		boolean bold, boolean underline, double separation, String msg)
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;

		// get the raster
		Raster ras = PixelDrawing.renderText(msg, font, tsize, italic, bold, underline, -1, -1);
		if (ras == null) return;

		/* determine the primitive to use for the layout */
		String nodeName = (String)textLayer.getSelectedItem();
		NodeProto primNode = Technology.getCurrent().findNodeProto(nodeName);
		if (primNode == null)
		{
			System.out.println("Cannot find " + nodeName + " primitive");
			return;
		}

		DataBufferByte dbb = (DataBufferByte)ras.getDataBuffer();
		byte [] samples = dbb.getData();
		int samp = 0;
		for(int y=0; y<ras.getHeight(); y++)
		{
			for(int x=0; x<ras.getWidth(); x++)
			{
				if (samples[samp++] == 0) continue;
				Point2D center = new Point2D.Double(x*scale, -y*scale);
				double wid = scale - separation;
				double hei = scale - separation;
				NodeInst ni = NodeInst.newInstance(primNode, center, wid, hei, 0, curCell, null);
			}
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

        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        textSize = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        textScale = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        dotSeparation = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        textFont = new javax.swing.JComboBox();
        textItalic = new javax.swing.JCheckBox();
        textBold = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        textLayer = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        textMessage = new javax.swing.JTextField();
        textUnderline = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Make Layout Text");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cancel(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ok(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        jLabel1.setText("Size (max 63):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel1, gridBagConstraints);

        textSize.setColumns(8);
        textSize.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(textSize, gridBagConstraints);

        jLabel2.setText("Scale factor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel2, gridBagConstraints);

        textScale.setColumns(8);
        textScale.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(textScale, gridBagConstraints);

        jLabel3.setText("Dot separation (units):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel3, gridBagConstraints);

        dotSeparation.setColumns(8);
        dotSeparation.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(dotSeparation, gridBagConstraints);

        jLabel4.setText("Font:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(textFont, gridBagConstraints);

        textItalic.setText("Italic");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(textItalic, gridBagConstraints);

        textBold.setText("Bold");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(textBold, gridBagConstraints);

        jLabel5.setText("Layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(textLayer, gridBagConstraints);

        jLabel6.setText("Message:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel6, gridBagConstraints);

        textMessage.setColumns(20);
        textMessage.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(textMessage, gridBagConstraints);

        textUnderline.setText("Underline");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        getContentPane().add(textUnderline, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void cancel(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancel
	{//GEN-HEADEREND:event_cancel
		closeDialog(null);
	}//GEN-LAST:event_cancel

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok
		// create the cell
		CreateLayoutText job = new CreateLayoutText(this);

		closeDialog(null);
	}//GEN-LAST:event_ok

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		grabDialogValues();
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

	private void grabDialogValues()
	{
		lastSize = TextUtils.atoi(textSize.getText());
		lastScale = TextUtils.atof(textScale.getText());
		lastSeparation = TextUtils.atof(dotSeparation.getText());
		lastItalic = textItalic.isSelected();
		lastBold = textBold.isSelected();
		lastUnderline = textUnderline.isSelected();
		lastLayer = (String)textLayer.getSelectedItem();
		lastFont = (String)textFont.getSelectedItem();
		lastMessage = textMessage.getText();
	}

	/**
	 * Class to create a cell in a new thread.
	 */
	protected static class CreateLayoutText extends Job
	{
		LayoutText dialog;

		protected CreateLayoutText(LayoutText dialog)
		{
			super("Create Layout Text", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;
			startJob();
		}

		public void doIt()
		{
			// should ensure that the name is valid
			dialog.grabDialogValues();
			dialog.makeLayoutText(lastLayer, lastSize, lastScale, lastFont, lastItalic,
				lastBold, lastUnderline, lastSeparation, lastMessage);
		}
	}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancel;
    private javax.swing.JTextField dotSeparation;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JButton ok;
    private javax.swing.JCheckBox textBold;
    private javax.swing.JComboBox textFont;
    private javax.swing.JCheckBox textItalic;
    private javax.swing.JComboBox textLayer;
    private javax.swing.JTextField textMessage;
    private javax.swing.JTextField textScale;
    private javax.swing.JTextField textSize;
    private javax.swing.JCheckBox textUnderline;
    // End of variables declaration//GEN-END:variables
	
}
