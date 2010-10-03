/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.sun.electric.util.memory;

import java.text.DecimalFormat;

public class Memory {
    public enum MemSize {
        none(1, "B"), kilo(1024, "kB"), mega(1024 * 1024, "MB"), giga(1024 * 1024 * 1024, "GB");

        private int value;
        private String entity;

        private MemSize(int value, String entity) {
            this.value = value;
            this.entity = entity;
        }

        public MemSize enlarge() {
            for (MemSize tmp : MemSize.values()) {
                if (tmp.value == (this.value * 1024)) {
                    return tmp;
                }
            }

            return this;
        }

        public MemSize ensmall() {
            for (MemSize tmp : MemSize.values()) {
                if (tmp.value == (this.value / 1024)) {
                    return tmp;
                }
            }

            return this;
        }

        public static MemSize getBestMemSize(Long memory) {
            MemSize result = MemSize.none;
            double minimum = Double.MAX_VALUE;

            for (MemSize tmp : MemSize.values()) {
                double value = (double) memory / (double) tmp.value;
                if (value >= 1.0 && Math.min(minimum, value) == value) {
                    result = tmp;
                }
            }

            return result;
        }
        
        public static double getBestMemSizeValue(Long memory, MemSize bestMemSize) {
            double value = 0.0;
            
            value = (double)memory / (double)bestMemSize.value;
            
            return value;
        }
    }

    public static String formatMemorySize(long memorySizeBytes) {

        StringBuilder builder = new StringBuilder();

        MemSize best = MemSize.getBestMemSize(memorySizeBytes);
        double value = MemSize.getBestMemSizeValue(memorySizeBytes, best);
        
        DecimalFormat df = new DecimalFormat("##.#");
        builder.append(df.format(value));
        builder.append(best.entity);

        return builder.toString();

    }

}
