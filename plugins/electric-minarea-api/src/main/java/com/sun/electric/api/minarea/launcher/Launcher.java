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
import com.sun.electric.api.minarea.MinAreaChecker.ErrorLogger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
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
            File layoutFile = new File(layoutFileName);
            InputStream is;
            if (layoutFile.canRead()) {
                is = new FileInputStream(layoutFileName);
                System.out.println("file " + layoutFileName);
            } else {
                is = Launcher.class.getResourceAsStream(layoutFileName);
                System.out.println("resource " + layoutFileName);
            }
            ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(is));
            LayoutCell topCell = (LayoutCell) in.readObject();
            in.close();
            Class algorithmClass = Class.forName(className);
            MinAreaChecker checker = (MinAreaChecker) algorithmClass.newInstance();
            System.out.println("topCell " + topCell.getName() + " [" + topCell.getBoundingMinX() + ".."
                    + topCell.getBoundingMaxX() + "]x[" + topCell.getBoundingMinY() + ".."
                    + topCell.getBoundingMaxY() + "] minarea=" + minarea);
            Properties parameters = checker.getDefaultParameters();
            if (algorithmPropertiesFileName != null) {
                Reader propertiesReader = new FileReader(algorithmPropertiesFileName);
                parameters.load(in);
                propertiesReader.close();
            }
            ErrorLogger logger = new ErrorRepositoryLogger();
            System.out.println("algorithm " + checker.getAlgorithmName() + " parameters:" + parameters);
            checker.check(topCell, minarea, parameters, logger);
            logger.printReports();
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

    private static class ErrorRepositoryLogger implements MinAreaChecker.ErrorLogger {

        private class MinAreaViolation {

            private final long minArea;
            private final int x, y;

            public MinAreaViolation(long minArea, int x, int y) {
                this.minArea = minArea;
                this.x = x;
                this.y = y;
            }
        }
        private List<Launcher.ErrorRepositoryLogger.MinAreaViolation> violations = new LinkedList<Launcher.ErrorRepositoryLogger.MinAreaViolation>();

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.electric.api.minarea.MinAreaChecker.ErrorLogger#
         * reportMinAreaViolation(long, int, int)
         */
        public synchronized void reportMinAreaViolation(long minArea, int x, int y) {
            violations.add(new MinAreaViolation(minArea, x, y));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.sun.electric.api.minarea.MinAreaChecker.ErrorLogger#printReports
         * ()
         */
        public void printReports() {
            System.out.println("***********************************************");
            if (violations.isEmpty()) {
                System.out.println("No DRC violation found: Good Job!");
            } else {
                System.out.println("DRC Min-Area Violations: " + violations.size());
                System.out.println();
                sortViolations();
                for (MinAreaViolation violation : violations) {
                    System.out.println("reportMinAreaViolation(" + violation.minArea + "," + violation.x
                            + "," + violation.y + ");");
                }
                System.out.println("***********************************************");
            }
        }

        private synchronized void sortViolations() {
            Collections.sort(violations, new Comparator<MinAreaViolation>() {

                public int compare(MinAreaViolation v1, MinAreaViolation v2) {
                    if (v1.x > v2.x) {
                        return 1;
                    }
                    if (v1.x < v2.x) {
                        return -1;
                    }
                    if (v1.y > v2.y) {
                        return 1;
                    }
                    if (v1.y < v2.y) {
                        return -1;
                    }
                    return 0;
                }
            });
        }
    }
}
