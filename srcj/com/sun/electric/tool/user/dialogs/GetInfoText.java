/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: GetInfoText2.java
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

import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.menus.MenuBar;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.font.GlyphVector;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowStateListener;
import java.awt.event.WindowEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.Graphics;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.EventListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

/**
 * Class to handle the "Text Get-Info" dialog.
 */
public class GetInfoText extends EDialog implements HighlightListener, DatabaseChangeListener {
    private static GetInfoText theDialog = null;
    private EditWindow wnd;
    private CachedTextInfo cti;

	/**
	 * Class to hold information about the text being manipulated.
	 */
	private static class CachedTextInfo
	{
		private Highlight shownText;
		private String initialText;
	    private Variable var;
	    private TextDescriptor td;
	    private ElectricObject owner;
	    private String description;
		private boolean instanceName;

		/**
		 * Method to load the field variables from a Highlight.
		 * @param h the Highlight of text.
		 */
		CachedTextInfo(Highlight h)
		{
			shownText = h;
			description = "Unknown text";
			initialText = "";
			td = null;
			owner = shownText.getElectricObject();
			instanceName = false;
			NodeInst ni = null;
			if (owner instanceof NodeInst) ni = (NodeInst) owner;
			var = shownText.getVar();
			if (var != null)
			{
				td = var.getTextDescriptor();
				Object obj = var.getObject();
				if (obj instanceof Object[])
				{
					// unwind the array elements by hand
					Object[] theArray = (Object[]) obj;
					initialText = "";
					for (int i = 0; i < theArray.length; i++)
					{
						if (i != 0) initialText += "\n";
						initialText += theArray[i];
					}
				} else
				{
					initialText = var.getPureValue(-1, -1);
				}
				description = var.getFullDescription(owner);
			} else
			{
				if (shownText.getName() != null)
				{
					if (owner instanceof Geometric)
					{
						Geometric geom = (Geometric) owner;
						td = geom.getNameTextDescriptor();
						if (geom instanceof NodeInst)
						{
							description = "Name of node " + ((NodeInst) geom).getProto().describe();
						} else
						{
							description = "Name of arc " + ((ArcInst) geom).getProto().describe();
						}
						initialText = geom.getName();
					}
				} else if (owner instanceof NodeInst)
				{
					description = "Name of cell instance " + ni.describe();
					td = ni.getProtoTextDescriptor();
					initialText = ni.getProto().describe();
					instanceName = true;
				} else if (owner instanceof Export)
				{
					Export pp = (Export)owner;
					description = "Name of export " + pp.getName();
					td = pp.getTextDescriptor();
					initialText = pp.getName();
				}
			}
		}

		/**
		 * Method to tell whether the highlighted text is the name of a cell instance.
		 * These cannot be edited by in-line editing.
		 * @return true if the highlighted text is the name of a cell instance.
		 */
		public boolean isInstanceName() { return instanceName; }
	}

    /**
     * Method to show the Text Get-Info dialog.
     */
    public static void showDialog() {
        if (theDialog == null) {
            if (TopLevel.isMDIMode()) {
                JFrame jf = TopLevel.getCurrentJFrame();
                theDialog = new GetInfoText(jf, false);
            } else {
                theDialog = new GetInfoText(null, false);
            }
        }
        theDialog.loadTextInfo();
        if (!theDialog.isVisible()) theDialog.pack();
		theDialog.setVisible(true);
    }

    /**
     * Reloads the dialog when Highlights change
     */
    public void highlightChanged()
    {
        if (!isVisible()) return;
        loadTextInfo();
    }

    /**
     * Called when by a Highlighter when it loses focus. The argument
     * is the Highlighter that has gained focus (may be null).
     * @param highlighterGainedFocus the highlighter for the current window (may be null).
     */
    public void highlighterLostFocus(Highlighter highlighterGainedFocus) {
        if (!isVisible()) return;
        loadTextInfo();        
    }

    public void databaseEndChangeBatch(Undo.ChangeBatch batch) {
        if (!isVisible()) return;

        boolean reload = false;
        for (Iterator it = batch.getChanges(); it.hasNext(); ) {
            Undo.Change change = (Undo.Change)it.next();
            ElectricObject obj = change.getObject();
            if (obj == cti.owner) {
                reload = true;
                break;
            }
        }
        if (reload) {
            // update dialog
            loadTextInfo();
        }
    }

    public void databaseChanged(Undo.Change change) {}

    public boolean isGUIListener() { return true; }

    private void loadTextInfo() {
		// update current window
		EditWindow curWnd = EditWindow.getCurrent();
		if ((wnd != curWnd) && (curWnd != null)) {
			if (wnd != null) wnd.getHighlighter().removeHighlightListener(this);
			curWnd.getHighlighter().addHighlightListener(this);
			wnd = curWnd;
 		}

        // must have a single text selected
        Highlight textHighlight = null;
        int textCount = 0;
        if (wnd != null) {
            for (Iterator it = wnd.getHighlighter().getHighlights().iterator(); it.hasNext();) {
                Highlight h = (Highlight) it.next();
                if (h.getType() != Highlight.Type.TEXT) continue;
                // ignore export text
                if (h.getVar() == null && h.getElectricObject() instanceof Export) continue;
                textHighlight = h;
                textCount++;
            }
        }
        if (textCount > 1) textHighlight = null;
        boolean enabled = (textHighlight == null) ? false : true;

        focusClearOnTextField(theText);

        // enable or disable everything
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            c.setEnabled(enabled);
        }
        if (!enabled) {
            header.setText("No Text Selected");
            evaluation.setText(" ");
            theText.setText("");
            theText.setEnabled(false);
			cti = null;
            textPanel.setTextDescriptor(null, null, null);
            attrPanel.setVariable(null, null, null, null);
            ok.setEnabled(false);
            apply.setEnabled(false);
            multiLine.setEnabled(false);
            return;
        }

		// cache information about the Highlight
		cti = new CachedTextInfo(textHighlight);

        // enable buttons
        ok.setEnabled(true);
        apply.setEnabled(true);

        header.setText(cti.description);
        theText.setText(cti.initialText);
        theText.setEditable(true);
        // if multiline text, make it a TextArea, otherwise it's a TextField
        if (cti.initialText.indexOf('\n') != -1) {
            // if this is the name of an object it should not be multiline
            if (cti.var == null && cti.shownText != null && cti.shownText.getName() != null) {
                multiLine.setEnabled(false);
                multiLine.setSelected(false);
            } else {
                multiLine.setEnabled(true);
                multiLine.setSelected(true);
            }
        } else {
            // if this is the name of an object it should not be multiline
            if (cti.var == null && cti.shownText != null && cti.shownText.getName() != null) {
                multiLine.setEnabled(false);
            } else {
                multiLine.setEnabled(true);
            }
            multiLine.setSelected(false);
        }
        // if the var is code, evaluate it
        evaluation.setText(" ");
        if (cti.var != null) {
            if (cti.var.isCode()) {
                evaluation.setText("Evaluation: " + cti.var.describe(-1, -1));
            }
        }
        // set the text edit panel
        textPanel.setTextDescriptor(cti.td, null, cti.owner);
        attrPanel.setVariable(cti.var, cti.td, null, cti.owner);

        // do this last so everything gets packed right
        changeTextComponent(cti.initialText, multiLine.isSelected());

        focusOnTextField(theText);
    }

    /**
     * Creates new form Text Get-Info
     */
	private GetInfoText(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        getRootPane().setDefaultButton(ok);
        setLocation(100, 50);

        Undo.addDatabaseChangeListener(this);

        loadTextInfo();
    }

	protected void escapePressed() { cancelActionPerformed(null); }

	/**
	 * Method to edit text in place.
	 */
	public static void editTextInPlace()
	{
		// there must be a current edit window
		EditWindow curWnd = EditWindow.getCurrent();
		if (curWnd == null) return;

		// must have a single text selected
		Highlight theHigh = null;
		int textCount = 0;
		for (Iterator it = curWnd.getHighlighter().getHighlights().iterator(); it.hasNext();)
		{
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.TEXT) continue;
			theHigh = h;
			textCount++;
		}
		if (textCount > 1) theHigh = null;
		if (theHigh == null) return;

		// grab information about the highlighted text
		CachedTextInfo cti = new CachedTextInfo(theHigh);
		if (cti.isInstanceName())
		{
			showDialog();
			return;
		}

		// get text description
		Font theFont = curWnd.getFont(cti.td);
		Point2D [] points = Highlighter.describeHighlightText(curWnd, cti.owner, cti.var, cti.shownText.getName());
		int lowX=0, highX=0, lowY=0, highY=0;
		for(int i=0; i<points.length; i++)
		{
			Point pt = curWnd.databaseToScreen(points[i]);
			if (i == 0)
			{
				lowX = highX = pt.x;
				lowY = highY = pt.y;
			} else
			{
				if (pt.x < lowX) lowX = pt.x;
				if (pt.x > highX) highX = pt.x;
				if (pt.y < lowY) lowY = pt.y;
				if (pt.y > highY) highY = pt.y;
			}
		}
		if (cti.td.getDispPart() != TextDescriptor.DispPos.VALUE)
		{
			GlyphVector gv = curWnd.getGlyphs(cti.initialText, theFont);
			Rectangle2D glyphBounds = gv.getVisualBounds();
			lowX = highX - (int)glyphBounds.getWidth();
		}
		EditInPlaceListener eip = new EditInPlaceListener(cti, curWnd, theFont, highX - lowX, highY - lowY, lowX, lowY);
	}

	/**
	 * Class to handle edit-in-place of text.
	 */
	public static class EditInPlaceListener implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
	{
		private CachedTextInfo cti;
		private EditWindow wnd;
		private EventListener oldListener;
		private JTextField tf;
		private MenuBar mb;

		public EditInPlaceListener(CachedTextInfo cti, EditWindow wnd, Font theFont, int width, int height, int lowX, int lowY)
		{
			this.cti = cti;
			this.wnd = wnd;

			tf = new JTextField(cti.initialText);
			tf.setSize(new Dimension(width, height));
			tf.setBorder(new EmptyBorder(0,0,0,0));
			tf.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { closeEditInPlace(); }
			});
			if (theFont != null) tf.setFont(theFont);
			Dimension dim = tf.getSize();
			tf.setLocation(lowX + width - dim.width, lowY);
			tf.selectAll();

			wnd.add(tf);
			tf.setVisible(true);
			oldListener = WindowFrame.getListener();
			WindowFrame.setListener(this);

			TopLevel top = (TopLevel)TopLevel.getCurrentJFrame();
			mb = top.getTheMenuBar();
			mb.setIgnoreTextEditKeys(true);
		}

		private void closeEditInPlace()
		{
			WindowFrame.setListener(oldListener);
			wnd.remove(tf);
			mb.setIgnoreTextEditKeys(false);

			String currentText = tf.getText();
			if (!currentText.equals(cti.initialText))
			{
				String[] textArray = currentText.split("\\n");
				ArrayList textList = new ArrayList();
				for (int i=0; i<textArray.length; i++)
				{
					String str = textArray[i];
					str = str.trim();
					if (str.equals("")) continue;
					textList.add(str);
				}

				textArray = new String[textList.size()];
				for (int i=0; i<textList.size(); i++)
				{
					String str = (String)textList.get(i);
					textArray[i] = str;
				}

				if (textArray.length > 0)
				{
					// generate job to change text
					ChangeText job = new ChangeText(cti, textArray);
				}
			}
		}
 
		// the MouseListener events
		public void mouseEntered(MouseEvent evt) {}
		public void mouseExited(MouseEvent evt) {}
		public void mousePressed(MouseEvent evt) { closeEditInPlace(); }
		public void mouseReleased(MouseEvent evt) {}
		public void mouseClicked(MouseEvent evt) {}

		// the MouseMotionListener events
		public void mouseMoved(MouseEvent evt) {}
		public void mouseDragged(MouseEvent evt) {}

		// the MouseWheelListener events
		public void mouseWheelMoved(MouseWheelEvent evt) {}

		// the KeyListener events
		public void keyPressed(KeyEvent evt) {}
		public void keyReleased(KeyEvent evt) {}
		public void keyTyped(KeyEvent evt) {}
	}

    protected static class ChangeText extends Job {
		CachedTextInfo cti;
        String[] newText;

        protected ChangeText(CachedTextInfo cti, String[] newText) {
            super("Modify Text", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cti = cti;
            this.newText = newText;
            startJob();
        }

        public boolean doIt()
        {
            if (cti.var != null)
            {
                Variable newVar = null;
                if (newText.length > 1)
                {
                    newVar = cti.owner.updateVar(cti.var.getKey(), newText);
                } else
                {
                    // change variable
                    newVar = cti.owner.updateVar(cti.var.getKey(), newText[0]);
                }
                if (newVar != null)
					cti.shownText.setVar(newVar);
            } else
            {
                if (cti.shownText.getName() != null)
                {
                    if (cti.owner != null)
                    {
                        // change name of NodeInst or ArcInst
                        ((Geometric)cti.owner).setName(newText[0]);
                    }
                } else if (cti.owner instanceof Export)
                {
                	Export pp = (Export)cti.owner;
					pp.rename(newText[0]);
                }
            }
            return true;
        }
    }

    /**
     * This method is called from within the constructor to
     * <p/>
     * initialize the form.
     */
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        header = new javax.swing.JLabel();
        apply = new javax.swing.JButton();
        evaluation = new javax.swing.JLabel();
        theText = new javax.swing.JTextField();
        textPanel = new TextInfoPanel();
        attrPanel = new TextAttributesPanel();
        buttonsPanel = new javax.swing.JPanel();
        multiLine = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());
        getRootPane().setDefaultButton(ok);

        setTitle("Text Properties");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        header.setText("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(header, gridBagConstraints);

        changeTextComponent("", false);

        multiLine.setText("Multi-Line Text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        getContentPane().add(multiLine, gridBagConstraints);
        multiLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                multiLineStateChanged();
            }
        });

        evaluation.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(evaluation, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(textPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(attrPanel, gridBagConstraints);

        cancel.setText("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cancelActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.1;
        //gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        apply.setText("Apply");
        apply.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                applyActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.1;
        //gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(apply, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                okActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.1;
        //gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);
/*

gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 0;
gridBagConstraints.gridy = 5;
gridBagConstraints.gridwidth = 1;
gridBagConstraints.weightx = 1.0;
gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
getContentPane().add(buttonsPanel, gridBagConstraints);
*/

        pack();
    }

    private void multiLineStateChanged() {
        // set text box type
        changeTextComponent(theText.getText(), multiLine.isSelected());
    }

    private void changeTextComponent(String currentText, boolean multipleLines) {

        if (cti == null || cti.shownText == null) return;

        getContentPane().remove(theText);

        if (currentText == null) currentText = "";

        if (multipleLines) {
            // multiline text, change to text area
            theText = new javax.swing.JTextArea();
            String[] text = currentText.split("\\n");
            int size = 1;
            if (text.length > size) size = text.length;
            ((javax.swing.JTextArea)theText).setRows(size);
            ((javax.swing.JTextArea)theText).setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

            // add listener to increase the number of rows if needed
            theText.addKeyListener(new KeyListener() {
                public void keyPressed(KeyEvent e) {}
                public void keyTyped(KeyEvent e) {}
                public void keyReleased(KeyEvent e) {
                    javax.swing.JTextArea area = (javax.swing.JTextArea)theText;
                    area.setRows(area.getLineCount());
                    pack();
                }
            });

        } else {
            theText = new javax.swing.JTextField();
            javax.swing.JTextField field = (javax.swing.JTextField)theText;
            if (currentText.matches(".*?\\n.*")) {
                currentText = currentText.substring(0, currentText.indexOf('\n'));
            }
        }
        theText.setText(currentText);

        java.awt.GridBagConstraints gridBagConstraints;
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(theText, gridBagConstraints);

        pack();
        theText.requestFocus();
    }

    private String getDelimtedText(javax.swing.text.JTextComponent c) {
        String currentText = c.getText();

        // getText from JTextArea returns one line. I want the
        // new line characters to be in there.
        if (c instanceof javax.swing.JTextArea) {
            javax.swing.JTextArea area = (javax.swing.JTextArea)c;

            StringBuffer text = new StringBuffer();
            boolean first = true;
            for (int i = 0; i < area.getLineCount(); i++) {
                try {
                    if (!first) {
                        text.append("\n");
                    }
                    int startPos = area.getLineStartOffset(i);
                    int endPos = area.getLineEndOffset(i);
                    text.append(currentText.substring(startPos, endPos));
                    System.out.println("Line "+i+" is: "+currentText.substring(startPos, endPos));
                    first = false;
                } catch (javax.swing.text.BadLocationException e) {
                    ActivityLogger.logException(e);                    
                }
            }
            currentText = text.toString();
        }

        return currentText;
    }

    private void applyActionPerformed(ActionEvent evt) {
        if (cti.shownText == null) return;

        // tell sub-panels to update if they have changed
        textPanel.applyChanges();
        attrPanel.applyChanges();

        boolean changed = false;

        // see if text changed
        String currentText = theText.getText();
        if (!currentText.equals(cti.initialText)) changed = true;

        if (changed) {

            String[] textArray = currentText.split("\\n");
            ArrayList textList = new ArrayList();
            for (int i=0; i<textArray.length; i++) {
                String str = textArray[i];
                str = str.trim();
                if (str.equals("")) continue;
                textList.add(str);
            }

            textArray = new String[textList.size()];
            for (int i=0; i<textList.size(); i++) {
                String str = (String)textList.get(i);
                textArray[i] = str;
            }

            if (textArray.length > 0) {
                // generate job to change text
                ChangeText job = new ChangeText(cti, textArray);
				cti.initialText = currentText;
            }
        }
        // update dialog
        //UpdateDialog job2 = new UpdateDialog();

    }

    private void okActionPerformed(ActionEvent evt) {
        applyActionPerformed(evt);
        closeDialog(null);
    }

    private void cancelActionPerformed(ActionEvent evt) {
        closeDialog(null);
    }

    /**
     * Closes the dialog
     */
    private void closeDialog(java.awt.event.WindowEvent evt) {
        setVisible(false);
    }

    private javax.swing.JButton apply;
    private javax.swing.JButton cancel;
    private javax.swing.JLabel evaluation;
    private javax.swing.JLabel header;
    private javax.swing.JButton ok;
    private javax.swing.text.JTextComponent theText;
    private javax.swing.JPanel buttonsPanel;
    private TextInfoPanel textPanel;
    private TextAttributesPanel attrPanel;
    private javax.swing.JCheckBox multiLine;
}
