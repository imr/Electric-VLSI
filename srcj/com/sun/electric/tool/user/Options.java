/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Highlight.java
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
/*
 * Options.java
 *
 * Created on November 11, 2003, 12:11 PM
 */

package com.sun.electric.tool.user;



import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Options object contains all user settable options. 
 * (does this include keybindings???). 
 * This object is static and must be initialized at 
 * program start.  It cannot be instantiated.  
 * IMPORTANT: All methods are declared 'synchronized'
 * because threaded tools may access options settings independently.
 *
 * @author  gainsley
 */
public final class Options {
    
    /** default program properties */               private static Properties defaultProps;
    /** application properties */                   private static Properties appProps;
    
    /** Constructor not used */
    private Options() {
    }
    
    public static void Initialize() 
    {
        // create and load default properties
        defaultProps = new Properties();
        try {
            FileInputStream in = new FileInputStream("defaultOptions");
            defaultProps.load(in);
            in.close();
        } catch (FileNotFoundException e) {
            System.out.println("Error: default program properties file not found");
        } catch (IOException e) {
            System.out.println("Error: "+e.getMessage());
        }

        // location of user options
        System.out.println("----------------SYSTEM PROPERTIES-----------------");
        System.getProperties().list(System.out);
        System.out.println("--------------------------------------------------");
        
        // create application properties using default properties
        // overridden by user properties
        appProps = new Properties(defaultProps);
        //in = new FileInputStream(userOptionsFile);
        //in.close();
    }
    
}
