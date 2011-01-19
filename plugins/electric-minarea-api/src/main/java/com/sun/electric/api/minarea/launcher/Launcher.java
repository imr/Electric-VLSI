/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Launcher.java
 *
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.api.minarea.launcher;

import com.sun.electric.api.minarea.LayoutCell;
import com.sun.electric.api.minarea.MinAreaChecker;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.util.Properties;

/**
 *
 */
public class Launcher {

    private static void help() {
        System.out.println("Usage: file.lay minarea CheckerAlgorithm [algorithm.properties]");
        System.out.println("    file.lay             - file with serialized layout");
        System.out.println("    minarea              - minarea threashold");
        System.out.println("    checkerAlgorithm     - class that implements com.sun.electric.api.minarea.MinAreaChecker");
        System.out.println("    algorithm.properties - optional file with algorithm properties");
    }
    
    public static void main(String[] args) {
        if (args.length < 3) {
            help();
            System.exit(0);
        }
        String layoutFileName = args[0];
        long minarea = Long.valueOf(args[1]);
        String className = args[2];
        String algorithmPropertiesFileName = args.length > 3 ? args[3] : null;
        try {
            ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(layoutFileName)));
            LayoutCell topCell = (LayoutCell)in.readObject();
            in.close();
            Class algorithmClass = Class.forName(className);
            MinAreaChecker checker = (MinAreaChecker)algorithmClass.newInstance();
            System.out.println("topCell " + topCell.getName() +
                    " [" + topCell.getBoundingMinX() + ".." + topCell.getBoundingMaxX() +
                    "]x[" + topCell.getBoundingMinY() + ".." + topCell.getBoundingMaxY() + "] minarea=" + minarea);
            Properties parameters = checker.getDefaultParameters();
            if (algorithmPropertiesFileName != null) {
                Reader propertiesReader = new FileReader(algorithmPropertiesFileName);
                parameters.load(in);
                propertiesReader.close();
            }
            System.out.println("algorithm " + checker.getAlgorithmName() + " parameters:" + parameters);
            checker.check(topCell, minarea, parameters, new MyErrorLogger());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        
    }
    
    private static class MyErrorLogger implements MinAreaChecker.ErrorLogger {

        /**
         * @param min area of violating polygon
         * @param x x-coordinate of some point of violating polygon
         * @param y y-coordinate of some point of violating polygon
         */
        public void reportMinAreaViolation(long minArea, long x, long y) {
            System.out.println("reportMinAreaViolation(" + minArea + "," + x + "," + y + ");");
        }
    }

}
