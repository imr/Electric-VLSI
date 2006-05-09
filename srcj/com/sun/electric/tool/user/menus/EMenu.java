/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EMenu.java
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

import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Generic Electric menu.
 */
public class EMenu extends EMenuItem {
    /** static menu items */                                                final List<EMenuItem> items;
    
    /**
     * @param text the menu item's displayed text.  An "_" in the string
     * indicates the location of the "mnemonic" key for that entry.
     * @param items var-arg menu items. Null arguments are skipped.
     * MenuCommands.SEPARATOR arguments are separators.
     */
    public EMenu(String text, EMenuItem... items) {
        super(text);
        List<EMenuItem> l = new ArrayList<EMenuItem>();
        for (EMenuItem item: items) {
            if (item == null) continue;
            l.add(item);
        }
        this.items = Collections.unmodifiableList(l);
    }
    
    /**
     * @param text the menu item's displayed text.  An "_" in the string
     * indicates the location of the "mnemonic" key for that entry.
     * @param itemsList items list. Null elements are skipped.
     * MenuCommands.SEPARATOR elements are separators.
     */
    public EMenu(String text, List<EMenuItem> itemsList) {
        this(text, itemsList.toArray(EMenuItem.NULL_ARRAY));
    }
    
    /**
     * Returns unmodifiebale list of menu items.
     * Separators are represented by MeniCommands.SEPARATOR object.
     * @return list of menu items.
     */
    public List<EMenuItem> getItems() { return items; }

    /**
     */
    public void setDynamicItems(List<? extends EMenuItem> dynamicItems) {
        for (EMenuBar.Instance menuBarInstance: TopLevel.getMenuBars()) {
            JMenu menu = (JMenu)menuBarInstance.findMenuItem(path);
            while (menu.getMenuComponentCount() > items.size())
                menu.remove(items.size());
            genMenuElems(menu, dynamicItems);
        }
    }

    @Override
    void registerTree(EMenuBar menuBar, int[] parentPath, int indexInParent) {
        super.registerTree(menuBar, parentPath, indexInParent);
        for (int index = 0; index < items.size(); index++) {
            EMenuItem item = items.get(index);
            item.registerTree(menuBar, path, index);
        }
    }
    
    @Override
    protected void registerItem() {}

    @Override
    JMenu genMenu(WindowFrame frame) {
        JMenu subMenu = (JMenu)super.genMenu(frame);
        genMenuElems(subMenu, items);
        return subMenu;
    }
    
    @Override
    protected JMenuItem createMenuItem() {
        return new JMenu();
    }
    
    private void genMenuElems(JMenu menu, List<? extends EMenuItem> items) {
        for (EMenuItem elem: items) {
            if (elem == EMenuItem.SEPARATOR) {
                menu.addSeparator();
                continue;
            }
            JMenuItem item = elem.genMenu(null);
            menu.add(item);
        }
    }
    
    @Override
    protected void updateMenuItem(JMenuItem item) {
        item.setEnabled(isEnabled());
    }
    
    @Override
    public void run() { throw new UnsupportedOperationException(); }
    
}
