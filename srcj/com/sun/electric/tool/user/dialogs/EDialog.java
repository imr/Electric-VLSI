/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EDialog.java
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

import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Client;

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

/**
 * Superclass for all dialogs that handles remembering the last location.
 */
public class EDialog extends JDialog
{
	private static HashMap<Class,Point> locations = new HashMap<Class,Point>();
	private static HashMap<Class,Point> sizes = new HashMap<Class,Point>();
	private Class thisClass;
    public static DialogFocusHandler dialogFocusHandler = new DialogFocusHandler();
    public static TextBoxFocusListener textBoxFocusListener = new TextBoxFocusListener();

	/** Creates new form */
	protected EDialog(Frame parent, boolean modal)
	{
        // in multi-headed displays, display dialog on head with windowframe
        super(parent, "", modal, (parent == null) ? TopLevel.getCurrentJFrame().getGraphicsConfiguration() : parent.getGraphicsConfiguration());

        assert !Job.BATCHMODE;

		thisClass = this.getClass();
		Point pt = locations.get(thisClass);
		if (pt == null)
		{
			pt = User.getDefaultWindowPos();
			pt.x += 100;
			pt.y += 50;
		}
		setLocation(pt.x, pt.y);

		addComponentListener(new MoveComponentListener());

		final String CANCEL_DIALOG = "cancel-dialog";
		KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(accel, CANCEL_DIALOG);
		getRootPane().getActionMap().put(CANCEL_DIALOG, new AbstractAction()
		{
			public void actionPerformed(ActionEvent event) { escapePressed(); }
		});

        if (parent == null && !TopLevel.isMDIMode()) {
            // add a focus listener for SDI mode so dialogs are always on top
            dialogFocusHandler.addEDialog(this);
        }
	}

	/**
	 * Method to complete initialization of a dialog.
	 * Restores the size from last time.
	 */
	protected void finishInitialization()
	{
		Point sz = sizes.get(thisClass);
		if (sz != null)
		{
			this.setSize(sz.x, sz.y);
		}
	}

	/**
	 * Method called when the ESCAPE key is pressed.
	 * Override it to cancel the dialog.
	 */
	protected void escapePressed() {}

    protected void closeDialog() {
        setVisible(false);
    }

    protected void focusClearOnTextField(JTextComponent textComponent) {
        textComponent.setSelectionStart(0);
        textComponent.setSelectionEnd(0);
    }

    /**
     * Sets the cursor to have focus in the specified textComponent, and
     * highlights any text in that text field.
     * @param textComponent the text field
     */
    protected void focusOnTextField(JTextComponent textComponent) {
//        textComponent.requestFocus();
        textComponent.setSelectionStart(0);
        textComponent.setSelectionEnd(textComponent.getDocument().getLength());
    }

    private static class TextBoxFocusListener implements FocusListener {
        public void focusGained(FocusEvent e) {
            Component source = e.getComponent();
            if (source instanceof JTextField) {
                JTextField textField = (JTextField)source;
                if (textField.isEnabled() && textField.isEditable()) {
                    int len = textField.getDocument().getLength();
                    textField.setSelectionStart(0);
                    textField.setSelectionEnd(len);
                }
            }
        }

        public void focusLost(FocusEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }


	/**
	 * Method to ensure that the selected item in a list is
	 * shown in the center of the list.
	 * The Java method "ensureIndexIsVisible" only makes sure it is visible,
	 * but it may be at the bottom of the list.
	 * This method centers the selected item nicely.
	 * @param list the JList with a selected item to center.
	 */
	protected void centerSelection(JList list)
	{
		int curIndex = list.getSelectedIndex();
		int listSize = list.getLastVisibleIndex() - list.getFirstVisibleIndex();

		int lowIndexToEnsure = curIndex - listSize/2 + 1;
		if (lowIndexToEnsure < 0) lowIndexToEnsure = 0;
		list.ensureIndexIsVisible(lowIndexToEnsure);

		int highIndexToEnsure = curIndex + listSize/2 - 1;
		if (highIndexToEnsure >= list.getModel().getSize())
			highIndexToEnsure = list.getModel().getSize() - 1;
		list.ensureIndexIsVisible(highIndexToEnsure);
	}

	private static class MoveComponentListener implements ComponentListener
	{
		public void componentHidden(ComponentEvent e) {}
		public void componentShown(ComponentEvent e) {}

		public void componentResized(ComponentEvent e)
		{
			Class cls = e.getSource().getClass();
			Rectangle bound = ((JDialog)e.getSource()).getBounds();
			int x = bound.width;
			int y = bound.height;
			sizes.put(cls, new Point(x, y));
		}

		public void componentMoved(ComponentEvent e)
		{
			Class cls = e.getSource().getClass();
			Rectangle bound = ((JDialog)e.getSource()).getBounds();
			int x = (int)bound.getMinX();
			int y = (int)bound.getMinY();
			locations.put(cls, new Point(x, y));
		}
	}

    private static class DialogFocusHandler implements WindowFocusListener {

        private List<EDialog> dialogs;

        private DialogFocusHandler() { dialogs = new ArrayList<EDialog>(); }

        public synchronized void addEDialog(EDialog dialog) {
            dialogs.add(dialog);
        }

        public synchronized void windowGainedFocus(WindowEvent e) {
            for (int i=0; i<dialogs.size(); i++) {
                EDialog dialog = dialogs.get(i);
                // this seems to be causing problems on windows platforms
                //dialog.toFront();
            }
        }

        public void windowLostFocus(WindowEvent e) {}

    }
}
