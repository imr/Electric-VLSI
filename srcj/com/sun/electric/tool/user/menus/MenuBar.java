/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MenuBar.java
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
package com.sun.electric.tool.user.menus;

/*
 * Created on Sep 30, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import com.sun.electric.tool.user.KeyBindingManager;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.ui.ToolBarButton;
import com.sun.electric.tool.user.ui.KeyBindings;
import com.sun.electric.tool.user.ui.KeyStrokePair;
import com.sun.electric.tool.user.ui.MessagesWindow;


import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Collection;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.KeyStroke;

/**
 * @author gainsley
 *
 * <p>A MenuManager is used to manage a set of MenuBars that share the
 * same set of menus and associated key bindings. This allows multiple
 * windows to create instances of a Menu Bar whose state (check box selection,
 * radio box selection, key bindings) is consistent across all instances.
 *
 * <p>Please Note that for safety, each MenuManager should have a different
 * "name" to reduce the possibility of stored key bindings conflicts. The
 * main MenuManager (or default) has the empty name, "" for backwards
 * compatability reasons.
 *
 */

public class MenuBar extends JMenuBar
{

    /**
     * MenuBarGroup associates several menu bars together than are instances
     * of the same MenuBar template. This maintains consistency of state and
     * key bindings. It also acts as an listener for updating all menu items
     * on a state change.
     */
    public static class MenuBarGroup implements ActionListener {

        /** all groups */                                               private static HashMap menuBarGroups = new HashMap();

        /** Name of this group */                                       private String name;
        /** Preferences for User Bindings */                            private Preferences prefs;
        /** All menu items created, stores as ArrayLists in HashMap */  private HashMap menuItems;
        /** Key Binding Manager for menu items */                       public final KeyBindingManager keyBindingManager;

        /** Factory method to create/get a group */
        public static MenuBarGroup newInstance(String name) {

            synchronized(menuBarGroups) {
                if (menuBarGroups.containsKey(name)) {
                    return (MenuBarGroup)menuBarGroups.get(name);
                }
                return new MenuBarGroup(name);
            }
        }

        private MenuBarGroup(String name) {
            this.name = name;
            prefs = Preferences.userNodeForPackage(MenuBarGroup.class);
            menuItems = new HashMap(40);
            keyBindingManager = new KeyBindingManager(this.name+"MenuKeyBinding-", prefs);
            // add to hashmap of existing groups
            menuBarGroups.put(name, this);
        }

        /** Get the updater (MenuBarGroup servers as an action listener) for
         * updating this group
         * @param name the name of the MenuBarGroup
         * @return an action listener used to update the menu items in the group,
         * returns null if no such group 'name'.
         */
        public static ActionListener getUpdaterFor(String name) {
            synchronized(menuBarGroups) {
                return (ActionListener)menuBarGroups.get(name);
            }
        }

        /** Update associated MenuItems in any other JMenuBars on a state change */
        public void actionPerformed(ActionEvent e)
        {
            // register action completed in messages window
            MessagesWindow.userCommandIssued();

            AbstractButton source = (AbstractButton)e.getSource();
            String name;
            if (source instanceof ToolBarButton) name = ((ToolBarButton)source).getName(); else
                name = source.getText();
            //System.out.println("ActionPerformed on Menu "+name+", state is "+source.isSelected());
            synchronized(this) {
                ArrayList list = (ArrayList)menuItems.get(name);
                if (list == null) return;
                for (Iterator it = list.iterator(); it.hasNext(); ) {
                    AbstractButton b = (AbstractButton)it.next();
                    if (b == source) continue;
                    //String name2;
                    //if (source instanceof ToolBarButton) name2 = ((ToolBarButton)source).getName(); else
                    //    name2 = source.getText();
                    //System.out.println("   - SubactionPerformed on Menu "+name2+", state set to "+source.isSelected());
                    // update state on other menus to match state on activated menu
                    b.setSelected(source.isSelected());
                }
            }
        }
    }

    private static class MenuLogger implements ActionListener {
        public synchronized void actionPerformed(ActionEvent e) {
            JMenuItem m = (JMenuItem)e.getSource();
            ActivityLogger.logMenuActivated(m);
        }
    }

    public static class RepeatLastCommandListener implements ActionListener {
        private AbstractButton lastActivated;
        private RepeatLastCommandListener() {}

        public synchronized void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source instanceof AbstractButton) lastActivated = (AbstractButton)source;
        }

        public synchronized AbstractButton getLastActivated() { return lastActivated; }
    }


    /**
     * Common Interface for all MenuItem types:
     * Menu, MenuItem, CheckBoxMenuItem, RadioButtonMenuItem
     */
    public interface MenuItemInterface
    {
        /** Set the parent Menu */
        public void setParentMenu(JComponent menu);
        /** Get the parent Menu (may also return a MenuBar if top-level menu) */
        public JComponent getParentMenu();
        public String getDescription();
    }

    /**
     * Custom MenuItem extends JMenuItem.  Also conforms to
     * common Interface MenuItemInterface so that all custom
     * classes can be treated the same.
     */
    public static class MenuItem extends JMenuItem implements MenuItemInterface
    {
        /** parent menu */                      private JComponent parentMenu = null;

        public MenuItem(String s) { super(s); }
        public MenuItem(String text, int mnemonic) { super( text, mnemonic); }

        public void setParentMenu(JComponent menu) { parentMenu = menu; }
        public JComponent getParentMenu() { return parentMenu; }
        public String getDescription() { return MenuBar.getDescription(this); }

    }

    public static class RadioButtonMenuItem extends JRadioButtonMenuItem implements MenuItemInterface
    {
        /** parent menu */                      private JComponent parentMenu = null;

        public RadioButtonMenuItem(String text, boolean selected) { super(text, selected); }
        
        public void setParentMenu(JComponent menu) { parentMenu = menu; }
        public JComponent getParentMenu() { return parentMenu; }
        public String getDescription() { return MenuBar.getDescription(this); }
    }
    

    public static class CheckBoxMenuItem extends JCheckBoxMenuItem implements MenuItemInterface
    {
        /** parent menu */                      private JComponent parentMenu = null;

        public CheckBoxMenuItem(String text, boolean state) { super(text, state); }
        
        public void setParentMenu(JComponent menu) { parentMenu = menu; }
        public JComponent getParentMenu() { return parentMenu; }
        public String getDescription() { return MenuBar.getDescription(this); }
    }

    /**
     * The is the class with most of the meat, because of the way MenuCommands was
     * originally set up.  I believe this is Ok tho, as we can put all the Menu
     * specific code in this file.
     */
    public static class Menu extends JMenu implements MenuItemInterface
    {
        /** parent menu */                      private JComponent parentMenu = null;

        public Menu(String s) { super(s); }
        public Menu(String text, char mnemonic) { super(text); setMnemonic(mnemonic); }

        public void setParentMenu(JComponent menu) { parentMenu = menu; }
        public JComponent getParentMenu() { return parentMenu; }
        public String getDescription() { return MenuBar.getDescription(this); }

        /**
         * Override the default method to add a JMenuItem to this Menu.
         * Exact same as super.add(JMenuItem), but also sets parent of
         * added menuItem to this.
         * @param menuItem the menuItem to be added to menu
         * @return the menuItem added
         */
        public JMenuItem add(JMenuItem menuItem) {
            super.add(menuItem);
            if (menuItem instanceof MenuItemInterface) {
                ((MenuItemInterface)menuItem).setParentMenu(this);
            }
            return menuItem;
        }

        /**
         * Add a JMenuItem to the JMenu.
         * @param s the menu item's displayed text.
         * @param accelerator the shortcut key, or null if none specified.
         * @param action the action to be taken when menu is activated.
         */
        public MenuItem addMenuItem(String s, KeyStroke accelerator, ActionListener action)
        {
            MenuItem item = new MenuItem(s);
            addItem(item, accelerator, action);
            return item;
        }

		/**
		 * Add a JMenuItem to the JMenu.
		 * @param s the menu item's displayed text.
		 * @param accelerator the shortcut key, or null if none specified.
		 * @param mnemonicIndex the index in s of the char that gets underlined.
		 * @param action the action to be taken when menu is activated.
		 */
		public MenuItem addMenuItem(String s, KeyStroke accelerator, int mnemonicIndex, ActionListener action)
		{
			char c = s.charAt(mnemonicIndex);
			MenuItem item = new MenuItem(s);
			item.setMnemonic(c);
			item.setDisplayedMnemonicIndex(mnemonicIndex);
			addItem(item, accelerator, action);
			return item;
		}

        /**
         * Add a JCheckBoxMenuItem to the JMenu.
         * @param s the menu item's displayed text.
         * @param state the initial state of the check box
         * @param accelerator the shortcut key, or null if none specified.
         * @param action the action to be taken when menu is activated.
         */
        public JMenuItem addCheckBox(String s, boolean state, KeyStroke accelerator, ActionListener action)
        {
            JMenuItem item = new CheckBoxMenuItem(s, state);
            addItem(item, accelerator, action);
            return item;
        }

        /**
         * Add a JRadioButtonMenuItem to the JMenu.
         * @param s the menu item's displayed text.
         * @param selected the initial selected state of the check box
         * @param group the button group to belong to; only one RadioButton per group can be selected.
         * @param accelerator the shortcut key, or null if none specified.
         * @param action the action to be taken when menu is activated.
         */
        public JMenuItem addRadioButton(String s, boolean selected, ButtonGroup group, KeyStroke accelerator, ActionListener action)
        {
            JMenuItem item = new RadioButtonMenuItem(s, selected);
            if (group != null) group.add((JRadioButtonMenuItem)item);
            addItem(item, accelerator, action);
            return item;
        }

        /**
         * Common method to add an item to this Menu.  This method is private;
         * external code should call the add<code>ItemType<code> methods of Menu instead.
         * @param item the item to add
         * @param accelerator the accelerator (null if none)
         * @param action the action to perform
         */
        private void addItem(JMenuItem item, KeyStroke accelerator, ActionListener action)
        {
            this.add(item);
            ((MenuItemInterface)item).setParentMenu(this);
            // set accelerator so user sees shortcut key on menu
            item.setAccelerator(accelerator);
            // add a listener that will record this command
            item.addActionListener(repeatLastCommandListener);
            // add action listeners so when user selects menu, actions will occur
            item.addActionListener(action);
            // add logger listener
            item.addActionListener(menuLogger);

            // if Menu already part of a MenuBar, let menu bar know this was added
            JComponent parent = parentMenu;
            while (parent != null) {
                if (parent instanceof MenuBar) {
                    ((MenuBar)parent).itemAdded(item);
                    break;
                }
                parent = ((Menu)parent).getParentMenu();
            }
            // otherwise, nothing to do now: update will take place when Menu added to MenuBar
        }
    }


    // ================================== MenuBar ========================================


    /** Menu bar group this belongs to */       private final MenuBarGroup menuBarGroup;
    /** hidden menus */                         private ArrayList hiddenMenus = new ArrayList();
    /** whether to ignore all shortcuts keys */ boolean ignoreKeyBindings;
    /** whether to ignore text editing keys */  boolean ignoreTextEditKeys;
    /** For logging menu activiations */        private static MenuLogger menuLogger = new MenuLogger();
    /** For the "Repeat Last Action" command */ public static final RepeatLastCommandListener repeatLastCommandListener = new RepeatLastCommandListener();

    /**
     * See MenuBar(String name). This creates a MenuBar belonging
     * to group name "".
     */
    public MenuBar() {
        this("");
    }

    /**
     * Create a new MenuBar that belongs to group "name". All MenuBars
     * in the group should have the same set of menu items. The group
     * serves to maintain consistency of state and key bindings across
     * multiple instances of MenuBars that share the same layout of
     * menu items.
     * @param name the group name.
     */
    public MenuBar(String name) {
        super();
        menuBarGroup = MenuBarGroup.newInstance(name);
        ignoreKeyBindings = false;
        ignoreTextEditKeys = false;
    }

    public JMenu add(JMenu c) {
        super.add(c);
        // If this menu has an menu items already added to it,
        // we need to integrate them via itemAdded()
        menuAdded(c);
        return c;
    }

    /**
     * Add a hidden menu to the MenuBar. It's key bindings will be active,
     * but it will not be shown in the MenuBar.
     */
    public JMenu addHidden(JMenu c) {
        hiddenMenus.add(c);
        menuAdded(c);
        return c;
    }

    // When a menu is added to the MenuBar, we need to integrate the items in the menu
    private void menuAdded(JMenu c) {
        // set parent to this
        ((Menu)c).setParentMenu(this);

        for (int i=0; i<c.getItemCount(); i++) {
            JMenuItem m = c.getItem(i);
            if (m == null) continue;            // ignore separators
            if (m instanceof JMenu)
                menuAdded((JMenu)m);
            else if (m instanceof MenuItem)
                itemAdded((MenuItem)m);
        }
    }

    // When an item is added to the MenuBar, we must update the key binding manager
    // and action listeners as part of the MenuBarGroup.
    private void itemAdded(JMenuItem item) {

        if (!(item instanceof MenuItemInterface)) {
            System.out.println("Can't add Menu Item "+item.getText()+" to Menu, it is not a MenuItemInterface");
            return;
        }

        synchronized(menuBarGroup) {
            String key = ((MenuItemInterface)item).getDescription();
            KeyStroke accelerator = item.getAccelerator();
            // look up list of associated menuitems already in hash table
            ArrayList list = (ArrayList)menuBarGroup.menuItems.get(key);
            if (list == null) {
                // this is the first instance of this menu item
                list = new ArrayList();
                menuBarGroup.menuItems.put(key, list);
                // add default binding
                addDefaultKeyBinding(item, accelerator, null);
                //System.out.println("added key binding for "+item.getText()+": "+accelerator);
                // add listener that will perform same thing as if user clicked on menu
                // this is needed to change the state of button and fire off to listeners
                menuBarGroup.keyBindingManager.addActionListener(((MenuItemInterface)item).getDescription(), new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JMenuItem m = (JMenuItem)e.getSource();
                        m.doClick();
                    }
                });
                // fake event source so it looks like event generated from menuitem
                menuBarGroup.keyBindingManager.setEventSource(((MenuItemInterface)item).getDescription(), item);
            }
            list.add(item);
            item.addActionListener(menuBarGroup);
        }
        item.addActionListener(ToolBarButton.updater);
        // update with any user defined bindings
        updateAccelerator(((MenuItemInterface)item).getDescription());
    }

    /**
     * Overrides JMenuBar's processKeyBinding, which distributes event
     * to it's menu items.  Instead, we pass the event to the keyBindingManager
     * to process the event.
     * @param ks the KeyStroke of the event
     * @param e the KeyEvent
     * @param condition condition (focused/not etc)
     * @param pressed
     */
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
                                        int condition, boolean pressed) {
        // ignore key bindings if set
        if (ignoreKeyBindings) return false;

        // if ignoreTextEditKeys, ignore anything that does not have CTRL
        if (ignoreTextEditKeys) {
            if (!e.isControlDown() && !e.isMetaDown() && !e.isAltDown())
                return false;                   // ignore
        }
        // see if we have a local binding (InputMap on JComponent)
        //boolean retValue = processKeyBinding(ks, e, condition, pressed);
        boolean retValue = false;

        // otherwise, pass to our keyBindingManager
        if (!retValue)
            retValue = menuBarGroup.keyBindingManager.processKeyEvent(e);
        // *do not* pass it to menus

        return retValue;
    }

    /**
     * Get the key bindings for the menu item.
     * @return key bindings for the menu item, or null if no item or no bindings.
     */
    public KeyBindings getKeyBindings(JMenuItem item) {
        return menuBarGroup.keyBindingManager.getKeyBindings(((MenuItemInterface)item).getDescription());
    }

    /**
     * Adds a default key binding to the MenuItem.  Default key bindings are overridden
     * by any user stored key bindings, but may be restored via the Edit Key Bindings dialog.
     * @param item the MenuItem to add a default key binding
     * @param stroke the key stroke
     * @param prefixStroke an optional prefix stroke (may be null)
     */
    public void addDefaultKeyBinding(JMenuItem item, KeyStroke stroke, KeyStroke prefixStroke) {
        // avoid adding the same binding multiple times when multiple menus are generated
        synchronized (menuBarGroup) {
            List list = (List)menuBarGroup.menuItems.get(((MenuItemInterface)item).getDescription());
            if (list != null) {
                // if more than one item created already, ignore this request,
                // as it has already been performed
                if (list.size() > 1) return;
            }
        }
        // add default key binding
        menuBarGroup.keyBindingManager.addDefaultKeyBinding(((MenuItemInterface)item).getDescription(),
                KeyStrokePair.getKeyStrokePair(prefixStroke, stroke));
        // update accelerator
        updateAccelerator(((MenuItemInterface)item).getDescription());
    }

    /**
     * Add a user defined Key binding. This gets stored to preferences.
     * @param item the menu item
     * @param stroke the key stroke bound to menu item
     * @param prefixStroke an option prefix stroke (may be null)
     */
    public void addUserKeyBinding(JMenuItem item, KeyStroke stroke, KeyStroke prefixStroke) {
        // add user key binding (gets stored to prefs, overrides default bindings)
        menuBarGroup.keyBindingManager.addUserKeyBinding(((MenuItemInterface)item).getDescription(),
                KeyStrokePair.getKeyStrokePair(prefixStroke, stroke));
        // update accelerator
        updateAccelerator(((MenuItemInterface)item).getDescription());
    }

    /**
     * Sets <code>item<code> back to default Key Bindings
     * @param item the item to reset to default bindings
     */
    public void resetKeyBindings(JMenuItem item) {
        // tell KeyBindingManager to reset to default KeyBindings
        menuBarGroup.keyBindingManager.resetKeyBindings(((MenuItemInterface)item).getDescription());
        // update accelerator
        updateAccelerator(((MenuItemInterface)item).getDescription());
    }

    /**
     * Sets *All* menu items back to their default key bindings
     */
    public void resetAllKeyBindings() {
        synchronized(menuBarGroup) {
            Collection c = menuBarGroup.menuItems.values();
            for (Iterator it = c.iterator(); it.hasNext(); ) {
                List list = (List)it.next();
                JMenuItem m = (JMenuItem)list.get(0);
                // call set to default only on one, others will get reset by that method
                resetKeyBindings(m);
            }
        }
    }

    /**
     * Removes a key binding.
     * @param actionDesc the item to remove the binding from
     * @param pair the key stroke pair to remove
     */
    public void removeKeyBinding(String actionDesc, KeyStrokePair pair) {
        // remove binding from binding manager
        menuBarGroup.keyBindingManager.removeKeyBinding(actionDesc, pair);
        // update accelerator
        updateAccelerator(actionDesc);
    }

    /**
     * Updates a menu item's accelerator.  Menu item is specified by
     * actionDesc (MenuItem.getDescription()).  This is usually called after a menu item's bindings
     * have changed (binding removed, added, or reset to default). This
     * updates the accelerator to a valid binding, which is displayed when
     * the user opens the menu.
     * @param actionDesc key for menu item
     */
    public void updateAccelerator(String actionDesc) {
        // get valid key binding, update menus with it
        KeyBindings bindings = menuBarGroup.keyBindingManager.getKeyBindings(actionDesc);
        KeyStroke accelerator = null;
        if (bindings != null) {
            Iterator it;
            if (bindings.getUsingDefaultKeys()) it = bindings.getDefaultKeyStrokePairs(); else
                it = bindings.getKeyStrokePairs();
            while (it.hasNext()) {
                KeyStrokePair pair = (KeyStrokePair)it.next();
                if (pair.getPrefixStroke() != null) continue;         // menus can't display two-stroke key bindings
                accelerator = pair.getStroke();
                break;
            }
        }
        // update menu items
        synchronized(menuBarGroup) {
            ArrayList list = (ArrayList)menuBarGroup.menuItems.get(actionDesc);
            if (list != null) {
                for (Iterator it = list.iterator(); it.hasNext(); ) {
                    JMenuItem m = (JMenuItem)it.next();
                    m.setAccelerator(accelerator);
                }
            }
        }
    }

    /**
     * Check if there are any bindings (usually from preferences)
     * that no longer have an associated action.
     */
    public void deleteEmptyBindings() {
        menuBarGroup.keyBindingManager.deleteEmptyBindings();
    }

    // ------------------------------ Clean Up Methods ----------------------------

    /**
     * Called when a TopLevel (in SDI mode) is disposed. This gets rid
     * of references to freed menu items, so that memory allocated to them
     * can be reclaimed.
     */
    public void finished()
    {
        // all menus
        for (int i=0; i<getMenuCount(); i++) {
            JMenu menu = getMenu(i);
            if (menu == null) continue;
            disposeofMenu(menu);
        }
    }

    private void disposeofMenu(JMenu menu)
    {
        for (int j=0; j<menu.getItemCount(); j++) {
            JMenuItem item = menu.getItem(j);
            if (item == null) continue;
            if (item instanceof JMenu) {
                disposeofMenu((JMenu)item);
            } else {
                disposeofMenuItem(item);
            }
        }
    }

    private void disposeofMenuItem(JMenuItem item)
    {
        // remove all listeners (which contain references to item)
        ActionListener [] listeners = item.getActionListeners();
        for (int k = 0; k < listeners.length; k++) {
            ActionListener listener = listeners[k];
            item.removeActionListener(listener);
        }
        synchronized(menuBarGroup) {
            ArrayList list = (ArrayList)menuBarGroup.menuItems.get(((MenuItemInterface)item).getDescription());
            if (list == null) return;           // hidden menu items will have null list
            // remove reference to item
            list.remove(item);
        }
        //System.out.println("  removing menu item "+item);
    }

    public void setIgnoreKeyBindings(boolean b) { ignoreKeyBindings = b; }

    public boolean getIgnoreKeyBindings() { return ignoreKeyBindings; }

    public void setIgnoreTextEditKeys(boolean b) { ignoreTextEditKeys = b; }

    public boolean getIgnoreTextEditKeys() { return ignoreTextEditKeys; }


    //------------------------------ Utility Methods -------------------------------

    /** Get a string description of the menu item.
     * Takes the form <p>
     * Menu | SubMenu | SubMenu | item
     * <p>
     * @param item The item to get a description for
     * @return a string of the description.
     */
    private static String getDescription(JMenuItem item) {
        Stack parents = new Stack();
        JComponent parent = ((MenuItemInterface)item).getParentMenu();
        while ((parent != null) && (parent instanceof JMenuItem)) {
            parents.push(parent);
            parent = ((MenuItemInterface)parent).getParentMenu();
        }
        StringBuffer buf = new StringBuffer();
        while(!(parents.isEmpty())) {
            buf.append(((JMenuItem)parents.pop()).getText() + " | ");
        }
        buf.append(item.getText());
        // if final menu's parent is not a JMenuBar, then this menu is
        // not in it's final container, and subsequent calls to this method
        // may return a different String.  Because this string is used as a
        // key into hash tables, that would be very bad.  So issue an error.
        if ((parent == null) || !(parent instanceof JMenuBar)) {
            System.out.println("  PROGRAMMER ERROR: Menu "+buf.toString()+" not in JMenuBar.");
            //System.out.println("  Menus must be built top-down or inconsistencies in key bindings will result");
            //Throwable t = new Throwable();
            //t.printStackTrace(System.out);
        }
        return buf.toString();
    }

    // ---------------------------------------------------------------------------
    // This is really a pain but it's really the only way to do it ---
    // In order to display two-stroke accelerators on the painted menu item,
    // we need to extend BasicMenuItemUI.  Unfortunately, determining the
    // accelerator text is not a single method, but is duplicated in two big methods.
    // So I have copy those methods here and just change a few lines of code in each.

}
