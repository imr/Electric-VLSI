/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayoutMergerFactory.java
 * Written by Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.geometry.bool;

import com.sun.electric.database.hierarchy.Cell;
import java.lang.reflect.Constructor;

/**
 *
 */
public abstract class LayoutMergerFactory {

   /**
     * <p>Constructor for derived classes.</p>
     *
     * <p>The constructor does nothing.</p>
     *
     * <p>Derived classes must create {@link LayoutMergerFactory} objects
     */
    protected LayoutMergerFactory() {
    }

    public abstract LayoutMerger newMerger(Cell topCell);

    public static LayoutMergerFactory newInstance() {
        try {
            Class<?> scalaClass = Class.forName("com.sun.electric.scala.LayoutMergerFactoryImpl");
            Constructor constructor = scalaClass.getConstructor();
            return (LayoutMergerFactory)constructor.newInstance();
        } catch (Exception e) {
            return new LayoutMergerDefaultImpl.Factory();
        }
    }
}
