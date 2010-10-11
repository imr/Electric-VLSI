/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SpringConfig.java
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * @author fschmidt
 * 
 */
public class SpringConfig extends Configuration {

    private static Log logger = LogFactory.getLog(SpringConfig.class);
    private BeanFactory beanFactory;

    protected SpringConfig() {
        try {
            beanFactory = new FileSystemXmlApplicationContext(SpringConfig.configName);
        } catch (BeanDefinitionStoreException ex) {
            logger.info("Could not find configuration file: " + this.configName
                    + " at the file system. Now look at classpath ...");
            try {
                beanFactory = new ClassPathXmlApplicationContext(SpringConfig.configName);
                logger.info("Configuration file is available at classpath.");
            } catch (BeanDefinitionStoreException intEx) {
                logger.error("Config file not found.", ex);
                System.exit(1);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.electric.util.config.Configuration#lookup(java.lang.String)
     */
    @Override
    public Object lookup(String name) {
        return beanFactory.getBean(name);
    }

}
