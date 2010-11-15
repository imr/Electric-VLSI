/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: InitStrategy.java
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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

import java.util.logging.Level;

import com.sun.electric.database.geometry.bool.LayoutMergerFactory;
import com.sun.electric.tool.util.concurrent.runtime.taskParallel.IThreadPool;
import com.sun.electric.tool.util.concurrent.runtime.taskParallel.ThreadPool;
import com.sun.electric.util.config.model.ConfigEntry;

/**
 * @author Felix Schmidt
 */
public abstract class InitStrategy {

    public abstract void init(EConfigContainer config);

    public static class StaticInit extends InitStrategy {

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.electric.util.config.InitStrategy#init()
         */
        @Override
        public void init(EConfigContainer config) {

            config.addConfigEntry(IThreadPool.class.getName(),
                    ConfigEntry.createForFactoryMethod(ThreadPool.class, "initialize", false));

            // Scala implementation of LayoutMergerFactory
            try {
                config.addConfigEntry(LayoutMergerFactory.class.getName(),
                        ConfigEntry.createForConstructor(
                                Class.forName("com.sun.electric.scala.LayoutMergerFactoryImpl"), false));
            } catch (ClassNotFoundException e) {
                Configuration.logger.log(Level.INFO,
                        "Didn't find scala implementation of LayoutMergerFactory");
            }

            // IRSIM plugin
            try {
                config.addConfigEntry("com.sun.electric.api.irsim.IAnalyzer",
                        ConfigEntry.createForConstructor(
                            Class.forName("com.sun.electric.plugins.irsim.IAnalyzerImpl"), false));
            } catch (ClassNotFoundException e) {
                Configuration.logger.log(Level.INFO, "Didn't find IRSIM plugin");
            }

            // JMF plugin
            try {
                config.addConfigEntry("com.sun.electric.api.movie.MovieCreator",
                        ConfigEntry.createForConstructor(
                                Class.forName("com.sun.electric.plugins.JMF.MovieCreatorJMF"), false));
            } catch (ClassNotFoundException e) {
                Configuration.logger.log(Level.INFO, "Didn't find JMF plugin");
            }

        }
    }
}
