/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Pref.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.text;

import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * Empty Preferences to ensure that Electric server doesn't touch real Preferences.
 */
public class EmptyPreferencesFactory implements PreferencesFactory {

    /**
     * Returns the system root preference node.  (Multiple calls on this
     * method will return the same object reference.)
     */
    public Preferences systemRoot() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the user root preference node corresponding to the calling
     * user.  In a server, the returned value will typically depend on
     * some implicit client-context.
     */
    public Preferences userRoot() {
        return factoryPrefRoot;
    }
    static final Preferences factoryPrefRoot = new AbstractPreferences(null, "") {

        @Override
        protected String getSpi(String key) {
            return null;
        }

        @Override
        protected AbstractPreferences childSpi(String name) {
            return this;
        }

        @Override
        protected void putSpi(String key, String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void removeSpi(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void removeNodeSpi() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected String[] keysSpi() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected String[] childrenNamesSpi() throws BackingStoreException {
            return new String[0];
        }

        @Override
        protected void syncSpi() throws BackingStoreException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void flushSpi() throws BackingStoreException {
//            throw new UnsupportedOperationException();
        }
    };
}
