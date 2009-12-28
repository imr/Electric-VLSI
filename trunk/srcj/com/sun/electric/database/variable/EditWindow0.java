/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EditWindow0.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.variable;

import java.io.Serializable;

/**
 * This interface gives a limited access to EditWindow necessary
 * for calculating a shape of some primitives.
 */
public interface EditWindow0 {

    /**
     * Get the window's VarContext
     * @return the current VarContext
     */
    public VarContext getVarContext();

    /**
     * Method to return the scale factor for this window.
     * @return the scale factor for this window.
     */
    public double getScale();

    /**
     * Method to return the text scale factor for this window.
     * @return the text scale factor for this window.
     */
    public double getGlobalTextScale();

    /**
     * Method to return the default font for this window.
     * @return the default font for this window.
     */
    public String getDefaultFont();

    /**
     * Class to encapsulate the minimal EditWindow0 data needed to pass into Jobs.
     */
    public static class EditWindowSmall implements EditWindow0, Serializable
    {

        private VarContext context;
        private double scale;
        private double globalScale;
        private String defaultFont;

        public EditWindowSmall(EditWindow_ wnd)
        {
            context = wnd.getVarContext();
            scale = wnd.getScale();
            globalScale = wnd.getGlobalTextScale();
            defaultFont = wnd.getDefaultFont();
        }

        /**
         * Get the window's VarContext
         * @return the current VarContext
         */
        public VarContext getVarContext() { return context; }

        /**
         * Method to return the scale factor for this window.
         * @return the scale factor for this window.
         */
        public double getScale() { return scale; }

        /**
         * Method to return the text scale factor for this window.
         * @return the text scale factor for this window.
         */
        public double getGlobalTextScale() { return globalScale; }

        /**
         * Method to return the default font for this window.
         * @return the default font for this window.
         */
        public String getDefaultFont() { return defaultFont; }
    }
}
