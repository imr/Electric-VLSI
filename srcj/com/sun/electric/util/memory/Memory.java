/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Memory.java
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
