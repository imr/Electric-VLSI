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

import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.change.Undo;

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import javax.swing.*;

/**
 * Superclass for all dialogs that handles remembering the last location.
 */
public class EDialog extends JDialog
{
	private static HashMap locations = new HashMap();
	private Class thisClass;
    public static DialogFocusHandler dialogFocusHandler = new DialogFocusHandler();
    public static TextBoxFocusListener textBoxFocusListener = new TextBoxFocusListener();

	/** Creates new form Search and Replace */
	protected EDialog(Frame parent, boolean modal)
	{
		super(parent, modal);

		thisClass = this.getClass();
		Point pt = (Point)locations.get(thisClass);
		if (pt == null) pt = new Point(100, 50);
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

    /** used to cancel the dialog */
	protected void escapePressed() {}

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


	private static class MoveComponentListener implements ComponentListener
	{
		public void componentHidden(ComponentEvent e) {}
		public void componentShown(ComponentEvent e) {}
		public void componentResized(ComponentEvent e) {}
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

        private List dialogs;

        private DialogFocusHandler() { dialogs = new ArrayList(); }

        public synchronized void addEDialog(EDialog dialog) {
            dialogs.add(dialog);
        }

        public synchronized void windowGainedFocus(WindowEvent e) {
            for (int i=0; i<dialogs.size(); i++) {
                EDialog dialog = (EDialog)dialogs.get(i);
                dialog.toFront();
            }
        }

        public void windowLostFocus(WindowEvent e) {}

    }
}
