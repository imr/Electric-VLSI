/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CompareXML.java
 * Written by Tom O'Neill, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation.test;

/**
 * "Diff" facility for scan chain XML files, to simplify verification of
 * modifications to an XML file. For example, automatically generated XML files
 * frequently need modification to increase their human-readability. Proper use:
 * <p>
 * <code>java CompareXML file1 file2</code>
 * <p>
 * This facility checks to see if the two XML files have compatible descriptions
 * of the hardware. Compatibility means: same chip names and instruction
 * register lengths; same scan chain names and lengths; and each scan chain
 * element has the same access and clears parameters. The comparison ignores
 * differences in how a chain is broken up into subchains and how the subchains
 * are named. Thus compatibility of two XML files does not ensure the same test
 * software can be used for them. The <code>pin</code> attribute of a subchain
 * is also ignored, since the automatic generator does not set it.
 * <p>
 * Both files must list the scan chains in the same order, or false
 * inconsistencies will be reported. For the automatically-generated file, the
 * chains are listed in order of increasing opcode. Some discrepancies, such as
 * in the length of the scan chains, are deemed so fundamental that a meaningful
 * comparison cannot be made before they are fixed. When one of these is
 * encountered, the comparison will be terminated.
 * <p>
 * Here are some tips for making the results easier to interpret: you should be
 * able to read both files into {@link ChainG}&nbsp;without error; results are
 * slightly more readable if you choose <code>file1</code> to be the file you
 * are more familiar with.
 */

public class CompareXML {

    String file1, file2;

    TestNode system1, system2;

    CompareXML(String file1, String file2) {
        this.file1 = file1;
        this.file2 = file2;
        if (file1 == null || file2 == null) {
            Infrastructure.fatal("CompareXML: null input file name");
        }
        try {
            this.system1 = XMLIO.read(file1);
            this.system2 = XMLIO.read(file2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void diffChips() {
        int nchip1 = system1.getChildCount();
        int nchip2 = system2.getChildCount();
        if (nchip1 != nchip2) {
            System.out.println("File " + file1 + " contains " + nchip1
                    + " chips, but " + file2 + "contains " + nchip2
                    + ".  Comparison aborted.");
            Infrastructure.exit(1);
        }

        String[] adamChips = new String[nchip1];

        for (int ichip = 0; ichip < nchip1; ichip++) {
            ChipNode chip1 = (ChipNode) system1.getChildAt(ichip);
            ChipNode chip2 = (ChipNode) system2.getChildAt(ichip);
            chip1.compare(chip2, file1, file2);
        }
    }

    /** Compares two XML files provided on the command line. */
    public static void main(String[] args) {
        if (args.length != 2) {
            Infrastructure
                    .fatal("Must enter exactly two file names on command line");
        }
        CompareXML comparer = new CompareXML(args[0], args[1]);
        comparer.diffChips();
    }

}
