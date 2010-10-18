/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ArgumentsParser.java
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
package com.sun.electric.util;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fschmidt
 * 
 */
public class ArgumentsParser {

    public static interface ArgumentEnum {
        public boolean isOptional();
    }

    public static Map<String, String> parseArguments(String... args) {
        Map<String, String> result = new HashMap<String, String>();

        for (String arg : args) {
            String key = extractArgumentName(arg);
            String value = extractValue(arg);
            result.put(key, value);
        }

        return result;
    }

    private static String extractArgumentName(String arg) {
        if (arg.contains("=")) {
            return arg.substring(2, arg.indexOf("="));
        } else {
            return arg.substring(2);
        }
    }

    private static String extractValue(String arg) {
        if (arg.contains("=")) {
            return arg.substring(arg.indexOf("=") + 1);
        } else {
            return null;
        }
    }
}
