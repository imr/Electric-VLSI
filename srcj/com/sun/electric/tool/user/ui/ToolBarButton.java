/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ToolBarButton.java
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
 *
 * Created on March 22, 2004, 5:23 PM
 */

package com.sun.electric.tool.user.ui;

//import com.sun.electric.tool.user.menus.MenuBar;
import com.sun.electric.tool.user.ActivityLogger;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.AbstractButton;
import javax.swing.JToggleButton;
import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.plaf.ButtonUI;


/**
 * This is a Button class to be used on the ToolBar.  It 
 * synchronizes buttons across multiple toolbars, such that if
 * the user depresses buttonA on one toolbar, buttonA gets 
 * depressed on all other toolbars as well.  This maintains
 * consistency in the user interface as the user edits 
 * different windows.  I'm not sure if this is only applicable
 * in the SDI (Single Document Interface).
 * <p>
 * All ToolBarButtons are toggle buttons unless set otherwise by
 * <code>setToggle</code>.  This means they change state when clicked.
 * <p>
 *
 * @author  gainsley
 */
public class ToolBarButton extends AbstractButton implements Accessible, ActionListener {
    
    /** Hash table of arraylists for all buttons */     private static HashMap<String,List<AbstractButton>> allButtons = new HashMap<String,List<AbstractButton>>(15);
    /** Dummy object for listener for updating */       public static ToolBarButton updater = new ToolBarButton(null, null);
    /** Name of ToolBarButton */                        private String name;
    /** Tool bar button logger */                       private static final ButtonLogger buttonLogger = new ButtonLogger();

    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassIDdefault = "ButtonUI";
    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassIDtoggle = "ToggleButtonUI";
    
    /** button type */                                  private String uiClassID;

    
    /** Creates a new instance of ToolBarButton */
    private ToolBarButton(String text, Icon icon) {
        setModel(new JToggleButton.ToggleButtonModel());
        uiClassID = uiClassIDtoggle;
        //setModel(new DefaultButtonModel());
        init(null, icon);
        this.name = text;
    }
  
    /** Get name of ToolBarButton */
    public String getName() { return name; }
    
    /**
     * Create a new ToolBarButton.  Note that <code>text</code> must be unique.
     * Also, if there is a corresponding menu item associated with this button
     * (that performs the same action and reflects the same state), <code>text</code>
     * must be the same for both the MenuItem and this ToolBarButton to maintain
     * consistency.
     * @param text Text for the button.  Must be unique.
     * @param icon default Icon for the button
     * @return the new ToolBarButton
     */
    public static ToolBarButton newInstance(String text, Icon icon) {
        // new button
        ToolBarButton b = new ToolBarButton(text, icon);
 //       b.addActionListener(MenuBar.repeatLastCommandListener);
        b.addActionListener(ToolBarButton.updater);
 //       b.addActionListener(MenuBar.MenuBarGroup.getUpdaterFor(""));
        b.addActionListener(buttonLogger);

        // add to book-keeping
        List<AbstractButton> buttonGroup;
        if (!allButtons.containsKey(text)) {
            buttonGroup = new ArrayList<AbstractButton>();
            allButtons.put(text, buttonGroup);
        } else {
            buttonGroup = allButtons.get(text);
        }
        buttonGroup.add(b);
        return b;
    }
    
    public void addActionListener(ActionListener l) {
        // buttonLogger needs to be last in list so it is activated first
        removeActionListener(buttonLogger);
        super.addActionListener(l);
        super.addActionListener(buttonLogger);
    }

    /**
     * Update associated ToolBarButtons on other toolbars
     */
    public void actionPerformed(ActionEvent e)
    {
        AbstractButton source = (AbstractButton)e.getSource();
        String name;
        if (source instanceof ToolBarButton) name = ((ToolBarButton)source).getName(); else
            name = source.getText();
        //System.out.println("ActionPerformed on Button "+name+", state is "+source.isSelected());
        List<AbstractButton> list = allButtons.get(name);
        if (list == null) return;
        for (AbstractButton b : list) {
            if (b == source) continue;
            String name2;
            if (source instanceof ToolBarButton) name2 = ((ToolBarButton)source).getName(); else
                name2 = source.getText();
            //System.out.println("   - SubactionPerformed on Button "+name2+", state set to "+source.isSelected());
            // update state on other menus to match state on activated menu
            b.setSelected(source.isSelected());
        }
    }
    
    /** 
     * Set icon to <code>icon</code> for all buttons named
     * <code>name</code>
     * @param name the name of the buttons
     * @param icon the new icon for the buttons
     */
    public static void setIconForButton(String name, Icon icon) {
        List<AbstractButton> list = allButtons.get(name);
        if (list == null) return;
        for (AbstractButton b : list) {
            b.setIcon(icon);
        }
    }
    
    /**
     * Return state (isSelected()) of ToolBarButton with name
     * <code>name</code>
     * @param name the name of the button
     * @return the state of the button
     */
    public static boolean getButtonState(String name) {
        List<AbstractButton> list = allButtons.get(name);
        if (list == null || list.size() == 0) return false;
        AbstractButton b = list.get(0);
        return b.isSelected();
    }        
    
    /**
     * Act as if ToolBarButton named <code>name</code> was clicked
     * @param name the name of the button
     */
    public static void doClick(String name) {
        List<AbstractButton> list = allButtons.get(name);
        if (list == null) return;
        AbstractButton b = list.get(0);
        b.doClick();
    }

    /**
     * Called when a TopLevel (in SDI mode) is disposed. This gets rid
     * of references to the tool bar button, so that memory allocated to them
     * can be reclaimed.
     */
    public void finished()
    {
        // remove all listeners
        ActionListener [] actionListeners = getActionListeners();
        for (int j = 0; j < actionListeners.length; j++) {
            ActionListener actionListener = actionListeners[j];
            removeActionListener(actionListener);
        }
        // remove hash table reference
        List<AbstractButton> list = allButtons.get(getName());
        if (list == null) return;
        list.remove(this);
    }

    private static class ButtonLogger implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            ToolBarButton b = (ToolBarButton)e.getSource();
            ActivityLogger.logToolBarButtonActivated(b.getName());
        }
    }


    // ---------------------------------- UI ---------------------------------
    
    /**
     * Resets the UI property to a value from the current look and
     * feel.
     *
     * @see javax.swing.JComponent#updateUI
     */
    public void updateUI() {
        setUI((ButtonUI)UIManager.getUI(this));
    }
    

    /**
     * Returns a string that specifies the name of the L&F class
     * that renders this component.
     *
     * @return the string "ButtonUI"
     * @see javax.swing.JComponent#getUIClassID
     * @see javax.swing.UIDefaults#getUI
     * beaninfo
     *        expert: true
     *   description: A string that specifies the name of the L&F class.
     */
    public String getUIClassID() {
        return uiClassID;
    }
        
/////////////////
// Accessibility support
////////////////

    /**
     * A cut-and-paste copy of JButton's Accessible Interface.
     * <p>
     * Gets the <code>AccessibleContext</code> associated with this
     * <code>JButton</code>. For <code>JButton</code>s,
     * the <code>AccessibleContext</code> takes the form of an 
     * <code>AccessibleJButton</code>. 
     * A new <code>AccessibleJButton</code> instance is created if necessary.
     *
     * @return an <code>AccessibleJButton</code> that serves as the 
     *         <code>AccessibleContext</code> of this <code>JButton</code>
     * beaninfo
     *       expert: true
     *  description: The AccessibleContext associated with this Button.
     */
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJButton();
        }
        return accessibleContext;
    }

    /**
     * A cut-and-paste copy of JButton's Accessible Interface.
     * <p>
     * This class implements accessibility support for the 
     * <code>JButton</code> class.  It provides an implementation of the 
     * Java Accessibility API appropriate to button user-interface 
     * elements.
     * <p>
     * <strong>Warning:</strong>
     * Serialized objects of this class will not be compatible with
     * future Swing releases. The current serialization support is
     * appropriate for short term storage or RMI between applications running
     * the same version of Swing.  As of 1.4, support for long term storage
     * of all JavaBeans<sup><font size="-2">TM</font></sup>
     * has been added to the <code>java.beans</code> package.
     * Please see {@link java.beans.XMLEncoder}.
     */
    protected class AccessibleJButton extends AccessibleAbstractButton {
    
        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the 
         * object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PUSH_BUTTON;
        }
    } // inner class AccessibleJButton
    
}
