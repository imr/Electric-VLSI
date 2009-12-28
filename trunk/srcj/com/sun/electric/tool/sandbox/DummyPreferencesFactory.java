/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DummyPreferencesFactory.java
 *
 * Copyright (c) 2007 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.sandbox;

import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * Dummy implementation of PreferencesFactory.
 */
public class DummyPreferencesFactory implements PreferencesFactory {

    private static final DummyPreferences systemRoot = new DummyPreferences(null, "");
    private static final DummyPreferences userRoot = new DummyPreferences(null, "");
    
    /**
     * Returns the system root preference node.  (Multiple calls on this
     * method will return the same object reference.)
     */
    public Preferences systemRoot() {
        return systemRoot;
    }
    
    /**
     * Returns the user root preference node corresponding to the calling
     * user.  In a server, the returned value will typically depend on
     * some implicit client-context.
     */
    public Preferences userRoot() {
        return userRoot;
    }
}
