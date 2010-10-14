/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: InitStrategy.java
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
package com.sun.electric.util.config;

import com.sun.electric.database.geometry.bool.LayoutMergerFactory;
import com.sun.electric.tool.simulation.irsim.IAnalyzer;
import com.sun.electric.tool.util.concurrent.runtime.taskParallel.IThreadPool;
import com.sun.electric.tool.util.concurrent.runtime.taskParallel.ThreadPool;
import com.sun.electric.util.config.EConfig.ConfigEntry;
import java.util.logging.Level;

/**
 * @author fschmidt
 * 
 */
public abstract class InitStrategy {

    public abstract void init(EConfig config);

    public static class StaticInit extends InitStrategy {

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.electric.util.config.InitStrategy#init()
         */
        @Override
        public void init(EConfig config) {

            config.addConfigEntry(IThreadPool.class,
                    ConfigEntry.createForFactoryMethod(ThreadPool.class, "initialize"));

            // Scala implementation of LayoutMergerFactory
            try {
                config.addConfigEntry(LayoutMergerFactory.class,
                    ConfigEntry.createForConstructor(Class.forName("com.sun.electric.scala.LayoutMergerFactoryImpl")));
            } catch (ClassNotFoundException e) {
                Configuration.logger.log(Level.INFO, "Didn't find scala implementation of LayoutMergerFactory");
            }

            // IRSIM plugin
            try {
                config.addConfigEntry(IAnalyzer.class,
                    ConfigEntry.createForFactoryMethod(Class.forName("com.sun.electric.plugins.irsim.Analyzer"), "getInstance"));
            } catch (ClassNotFoundException e) {
                Configuration.logger.log(Level.INFO, "Didn't find IRSIM plugin");
            }

        }

    }

}
