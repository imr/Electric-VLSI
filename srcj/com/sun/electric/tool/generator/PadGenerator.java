/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PadGenerator.java
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
package com.sun.electric.tool.generator;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.routing.AutoStitch;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ViewChanges;
import com.sun.electric.tool.user.menus.EditMenu;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

/**
 * @author Willy Chung
 *         <p/>
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class PadGenerator {
    
    public static class ArrayAlign {
        int lineno;
        String cellname;
        String inport;
        String outport;
    }

    public static class PadExports {
        int lineno;
        String cellname;
        String padname;
        String corename;
    }

    public static class PlacePad {
        int lineno;
        String cellname;
        String exportsname;
        int gap;
        NodeInst ni;
        List associations;
    }

    public static class Rotation {
        int angle;
    }

    public static class PortAssociate {
        boolean export;
        String portname;
        String assocname;
    }


	private static class PadFrame extends Job {
        String filename;
        String padframename;                        // name of pad frame cell
        String corename;                            // core cell to stick in pad frame

        int lineno;                             // line no of the pad array file we are processing
        Library cellLib;                        // library containing pad cells
        boolean copycells;                      // if we copy cells into the library with the pad ring
        List views;                             // list of strings defining views of pad frame to create.
        int angle;                              // angle of placed instances
        HashMap alignments;                     // how to align adjacent instances
        HashMap exports;                        // which ports to export
        List orderedCommands;                   // list of orderedCommands to do

        protected PadFrame(String file) {
            super("Pad Frame Generator", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.filename = file;
            alignments = new HashMap();
            exports = new HashMap();
            views = new ArrayList();
            angle = 0;
            lineno = 1;
            orderedCommands = new ArrayList();
            startJob();
        }

        public boolean doIt() {
            String lineRead;
            //int gap = 0, gapx = 0, gapy = 0;
            //PortProto pp = null, exportpp;
            //Cell np;
            //double width = 0, height = 0;

            File inputFile = new File(filename);
            if (inputFile == null || !inputFile.canRead()) {
                System.out.println("Error reading file "+filename);
                return false;
            }

            try {
                FileReader readFile = new FileReader(inputFile);
                BufferedReader readLine = new BufferedReader(readFile);

                lineRead = readLine.readLine();

                //List arrayAlignList = new ArrayList();
                //List arrayPortAssociate = new ArrayList();
                //Cell cell = null;
                //Cell corenp = null;
                NodeInst lastni = null;

                while (lineRead != null) {
                    StringTokenizer str = new StringTokenizer(lineRead, " \t");
                    if (str.hasMoreTokens()) {
                        String keyWord = str.nextToken();

                        if (keyWord.charAt(0) != ';') {
                            do {
                                if (keyWord.equals("celllibrary")) {
                                    if (!processCellLibrary(str)) return false;
                                    continue;
                                } else if (keyWord.equals("views")) {
                                    if (!processViews(str)) return false;
                                    continue;
                                } else if (keyWord.equals("facet")) {
                                    if (!processFacet(str)) return false;
                                    continue;
                                } else if (keyWord.equals("core")) {
                                    if (!processCore(str)) return false;
                                    continue;
                                } else if (keyWord.equals("rotate")) {
                                    if (!processRotate(str)) return false;
                                    continue;
                                } else if (keyWord.equals("align")) {
                                    if (!processAlign(str)) return false;
                                    continue;
                                } else if (keyWord.equals("export")) {
                                    if (!processExport(str)) return false;
                                    continue;
                                } else if (keyWord.equals("place")) {
                                    if (!processPlace(str)) return false;
                                    continue;
                                }
/*
                                {
                                    keyWord = str.nextToken();

                                    if (cell == null) {
                                        System.out.println("Line " + lineno + ": no cell specified for 'place'");
                                        break;
                                    }

                                    np = cellLib.findNodeProto(keyWord);

                                    if (copycells) {
                                        if (np != null) {
                                            // copy into the current library
                                            np = CircuitChanges.copyRecursively(np, np.getName(), Library.getCurrent(),
                                                    np.getView(), false, false, "", false, false, false);
                                        }
                                    }
                                    if (np == null) {
                                        System.out.println("Line " + lineno + ": cannot find cell '" + keyWord + "'");
                                        break;
                                    }

                                    gap = 0;

                                    exportpp = null;
                                    String exportname = null;
                                    String padname = null;

                                    while (str.hasMoreTokens()) {
                                        keyWord = str.nextToken();
                                        String temp = keyWord;
                                        int equalsLoc = keyWord.indexOf("=");
                                        if (equalsLoc != -1) {
                                            temp = keyWord.substring(0, equalsLoc);
                                        }

                                        if (temp.equals("gap")) {
                                            String temp2;
                                            if (keyWord.indexOf("=") != -1) {
                                                if (keyWord.substring(keyWord.indexOf("=") + 1) == "") {
                                                    temp2 = str.nextToken();
                                                } else {
                                                    temp2 = keyWord.substring(keyWord.indexOf("=") + 1);
                                                }
                                            } else {
                                                keyWord = str.nextToken();

                                                if (keyWord.charAt(0) == '=') {
                                                    temp2 = keyWord.substring(1);
                                                } else {
                                                    System.out.println("Line " + lineno + ": missing '=' after 'gap'");
                                                    break;
                                                }
                                            }

                                            gap = Integer.parseInt(temp2);
                                        } else if (temp.equals("export")) {
                                            if (!str.hasMoreTokens()) {
                                                System.out.println("Line " + lineno + ": missing port name after 'export'");
                                                break;
                                            }

                                            keyWord = str.nextToken();
                                            String temp2;
                                            if (keyWord.indexOf("=") != -1) {
                                                temp = keyWord.substring(0, keyWord.indexOf("="));
                                                if (keyWord.substring(keyWord.indexOf("=") + 1) == "") {
                                                    temp2 = str.nextToken();
                                                } else {
                                                    temp2 = keyWord.substring(keyWord.indexOf("=") + 1);
                                                }
                                            } else {
                                                temp = keyWord;
                                                keyWord = str.nextToken();

                                                if (keyWord.charAt(0) == '=') {
                                                    temp2 = keyWord.substring(1);
                                                } else {
                                                    System.out.println("Line " + lineno + ": missing '=' after 'export PORT'");
                                                    break;
                                                }
                                            }

                                            exportpp = np.findPortProto(temp);
                                            if (exportpp == null) {
                                                System.out.println("Line " + lineno + ": no port '" + temp + "' on cell '" + temp + "'");
                                                break;
                                            }

                                            exportname = temp2;
                                            if (exportname.equals("")) {
                                                System.out.println("Line " + lineno + ": missing export name after 'export PORT='");
                                                break;
                                            }
                                        } else if (temp.equals("name")) {
                                            if (!str.hasMoreTokens()) {
                                                System.out.println("Line " + lineno + ": missing pad exports name after 'name'");
                                                break;
                                            }
                                            String temp2 = null;
                                            if (keyWord.indexOf("=") != -1) {
                                                if (keyWord.substring(keyWord.indexOf("=") + 1) == "") {
                                                    temp2 = str.nextToken();
                                                } else {
                                                    temp2 = keyWord.substring(keyWord.indexOf("=") + 1);
                                                }
                                            } else {
                                                keyWord = str.nextToken();

                                                if (keyWord.equals("=")) {
                                                    if (!str.hasMoreTokens()) {
                                                        System.out.println("Line " + lineno + ": missing export name after name=");
                                                        break;
                                                    } else {
                                                        temp2 = str.nextToken();
                                                    }
                                                } else if (keyWord.charAt(0) == '=') {
                                                    temp2 = keyWord.substring(1);
                                                } else {
                                                    System.out.println("Line " + lineno + ": missing '=' after name");
                                                    break;
                                                }
                                            }
                                            padname = temp2;

                                        } else {
                                            PortAssociate pa = new PortAssociate();
                                            pa.ni = null;
                                            pa.pp = np.findPortProto(temp);
                                            if (pa.pp == null) {
                                                System.out.println("Line " + lineno + ": no export '" + temp + "' in cell " + np.describe());
                                            }

                                            String temp2;
                                            if (keyWord.indexOf("=") != -1) {
                                                if (keyWord.substring(keyWord.indexOf("=") + 1) == "") {
                                                    temp2 = str.nextToken();
                                                } else {
                                                    temp2 = keyWord.substring(keyWord.indexOf("=") + 1);
                                                }
                                            } else {
                                                keyWord = str.nextToken();

                                                if (keyWord.charAt(0) == '=') {
                                                    temp2 = keyWord.substring(1);
                                                } else {
                                                    System.out.println("Line " + lineno + ": missing '=' after pad port name");
                                                    break;
                                                }
                                            }

                                            if (corenp == null) {
                                                System.out.println("Line " + lineno + ": no core cell for association");
                                                break;
                                            }
                                            pa.corepp = corenp.findPortProto(temp2);
                                            if (pa.corepp == null) {
                                                System.out.println("Line " + lineno + ": no port '" + temp2 + "' on cell '" + temp2 + "'");
                                                break;
                                            }
                                            arrayPortAssociate.add(pa);
                                        }
                                    }
                                    double centerX = 0;
                                    double centerY = 0;
                                    if (lastni != null) {
                                        String cellname = (lastni.getProtoEquivalent()).noLibDescribe();
                                        ArrayAlign aa = null;
                                        for (Iterator it = arrayAlignList.iterator(); it.hasNext();) {
                                            aa = (ArrayAlign) it.next();
                                            if (aa.cell.equals(cellname)) break;
                                            aa = null;
                                        }
                                        if (aa == null) {
                                            System.out.println("Line " + lineno + ": no port alignment given for cell " + lastni.describe());
                                            break;
                                        }

                                        pp = (lastni.getProto()).findPortProto(aa.outport);
                                        if (pp == null) {
                                            System.out.println("Line " + lineno + ": no port called '" + aa.outport + "' on cell" + lastni.describe());
                                            break;
                                        }

                                        Poly poly = (lastni.findPortInstFromProto(pp)).getPoly();
                                        centerX = poly.getCenterX();
                                        centerY = poly.getCenterY();
                                        width = poly.getBounds().getWidth();
                                        height = poly.getBounds().getHeight();

                                        cellname = np.whichCell().noLibDescribe();

                                        aa = null;
                                        for (Iterator it = arrayAlignList.iterator(); it.hasNext();) {
                                            aa = (ArrayAlign) it.next();
                                            if (aa.cell.equals(cellname)) break;
                                            aa = null;
                                        }
                                        if (aa == null) {
                                            System.out.println("Line " + lineno + ": no port called '" + aa.outport + "' on cell " + np.describe());
                                            break;
                                        }
                                        pp = np.findPortProto(aa.inport);

                                        if (pp == null) {
                                            System.out.println("Line " + lineno + ": no port called '" + aa.inport + "' on cell " + np.describe());
                                            break;
                                        }
                                    }

                                    //corneroffset(NONODEINST,np,angle,0,&ox,&oy,false);
                                    Point2D pointCenter = new Point2D.Double(centerX, centerY);
                                    NodeInst ni = NodeInst.makeInstance(np, pointCenter, np.getDefWidth(), np.getDefHeight(), angle, cell, null);
                                    if (ni == null) {
                                        System.out.println("Line " + lineno + ": problem creating" + np.describe() + " instance");
                                        break;
                                    }

                                    if (lastni != null) {
                                        switch (angle) {
                                            case 0:
                                                gapx = gap;
                                                gapy = 0;
                                                break;
                                            case 900:
                                                gapx = 0;
                                                gapy = gap;
                                                break;
                                            case 1800:
                                                gapx = -gap;
                                                gapy = 0;
                                                break;
                                            case 2700:
                                                gapx = 0;
                                                gapy = -gap;
                                                break;
                                        }
                                        Poly poly = ni.findPortInstFromProto(pp).getPoly();
                                        double tempx = centerX - (poly.getCenterX()) - gapx;
                                        double tempy = centerY - (poly.getCenterY()) - gapy;
                                        ni.modifyInstance(tempx, tempy, 0, 0, 0);
                                    }
                                    if (exportpp != null) {
                                        pp = Export.newInstance(cell, ni.findPortInstFromProto(exportpp), exportname);
                                    }
                                    lastni = ni;

                                    for (Iterator it = arrayPortAssociate.iterator(); it.hasNext();) {
                                        PortAssociate pa = (PortAssociate) it.next();
                                        if (pa.ni == null) pa.ni = ni;
                                    }
                                    continue;
                                }
*/

                                System.out.println("Line " + lineno + ": unknown keyword'" + keyWord + "'");
                                break;

                            } while (str.hasMoreTokens());
                        }
                    }

                    lineRead = readLine.readLine();
                    lineno++;
                }
/*

                if (corenp != null) {
                    Rectangle2D bounds = cell.getBounds();
                    Point2D center = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
                    EditWindow.gridAlign(center);

                    SizeOffset so = corenp.getSizeOffset();
                    NodeInst ni = NodeInst.makeInstance(corenp, center, corenp.getDefWidth(), corenp.getDefHeight(), 0, cell, null);

                    for (Iterator it = arrayPortAssociate.iterator(); it.hasNext();) {
                        PortAssociate pa = (PortAssociate) it.next();
                        if (pa.ni == null) continue;

                        PortInst pi1 = ni.findPortInstFromProto(pa.corepp);
                        PortInst pi2 = pa.ni.findPortInstFromProto(pa.pp);
                        ArcProto ap = Generic.tech.unrouted_arc;
                        ArcInst ai = ArcInst.newInstance(ap, ap.getDefaultWidth(), pi1, pi2, null);
                    }
                }

*/

            } catch (IOException e1) {
            }

            createPadFrames();
			return true;
       }


        /**
         * Process the celllibrary keyword
         *
         * @return true on success, false on error.
         */
        private boolean processCellLibrary(StringTokenizer str) {

            String keyWord;
            if (str.hasMoreTokens()) {
                keyWord = str.nextToken();

                URL fileURL = TextUtils.makeURLToFile(keyWord);

                cellLib = Library.findLibrary(TextUtils.getFileNameWithoutExtension(fileURL));
                if (cellLib == null) {
                    // library does not exist: see if in same directory is pad frame file
                    StringBuffer errmsg = new StringBuffer();
                    String fileDir = TextUtils.getFilePath(TextUtils.makeURLToFile(filename));
                    fileURL = TextUtils.makeURLToFile(fileDir + keyWord);
                    if (!TextUtils.URLExists(fileURL, errmsg))
                    {
                        // library does not exist: see if file can be found locally
                        if (!TextUtils.URLExists(fileURL, errmsg))
                        {
                            // try the Electric library area
                            fileURL = LibFile.getLibFile(keyWord);
                            if (!TextUtils.URLExists(fileURL, errmsg))
                            {
                                //System.out.println("Cannot find cell library " + fileURL.getPath());
                                System.out.println(errmsg.toString());
                                return false;
                            }
                        }
                    }

                    FileType style = FileType.DEFAULTLIB;
                    if (TextUtils.getExtension(fileURL).equals("txt")) style = FileType.READABLEDUMP;
                    if (TextUtils.getExtension(fileURL).equals("elib")) style = FileType.ELIB;
                    Library saveLib = Library.getCurrent();
                    cellLib = Input.readLibrary(fileURL, null, style, false);
                    if (cellLib == null) {
                        err("cannot read library " + keyWord);
                        return false;
                    }
                    saveLib.setCurrent();
                }
            }

            if (str.hasMoreTokens()) {
                keyWord = str.nextToken();
                if (keyWord.equals("copy")) {
                    copycells = true;
                }
            }
            return true;
        }

        /**
         * Process any Views.
         *
         * @return true on success, false on error.
         */
        private boolean processViews(StringTokenizer str) {
            String keyWord;
            while (str.hasMoreTokens()) {
                keyWord = str.nextToken();
                View view = View.findView(keyWord);
                if (view != null)
                    views.add(view);
                else
                    err("Unknown view '" + keyWord + "', ignoring");
            }
            return true;
        }

        /**
         * Process the facet keyword
         *
         * @return true on success, false on error.
         */
        private boolean processFacet(StringTokenizer str) {
            if (str.hasMoreTokens()) {
                padframename = str.nextToken();
                return true;
            }
            return false;
        }

        /**
         * Process the core keyword
         *
         * @return true on success, false on error.
         */
        private boolean processCore(StringTokenizer str) {
            if (str.hasMoreTokens()) {
                corename = str.nextToken();
                return true;
            }
            return false;
        }

        /**
         * Process the rotate keyword
         *
         * @return true on success, false on error.
         */
        private boolean processRotate(StringTokenizer str) {
            String keyWord;
            int angle = 0;
            if (str.hasMoreTokens()) {
                keyWord = str.nextToken();
                if (keyWord.equals("c")) {
                    angle = 2700;
                    //angle = (angle + 2700) % 3600;
                } else if (keyWord.equals("cc")) {
                    angle = 900;
                    //angle = (angle + 900) % 3600;
                } else {
                    System.out.println("Line " + lineno + ": incorrect rotation " + keyWord);
                    return false;
                }
                Rotation rot = new Rotation();
                rot.angle = angle;
                orderedCommands.add(rot);
                return true;
            }
            return false;
        }

        /**
         * Process the align keyword
         *
         * @return true on success, false on error.
         */
        private boolean processAlign(StringTokenizer str) {
            String keyWord;
            ArrayAlign aa = new ArrayAlign();
            aa.lineno = lineno;
            keyWord = str.nextToken();

            if (keyWord.equals("")) {
                System.out.println("Line " + lineno + ": missing 'cell' name");
                return false;
            }
            aa.cellname = keyWord;

            keyWord = str.nextToken();

            if (keyWord.equals("")) {
                System.out.println("Line " + lineno + ": missing 'in port' name");
                return false;
            }
            aa.inport = keyWord;

            keyWord = str.nextToken();

            if (keyWord.equals("")) {
                System.out.println("Line " + lineno + ": missing 'out port' name");
                return false;
            }
            aa.outport = keyWord;
            alignments.put(aa.cellname, aa);
            return true;
        }

        /**
         * Process the export keyword
         *
         * @return true on success, false on error.
         */
        private boolean processExport(StringTokenizer str) {
            String keyWord;
            PadExports pe = new PadExports();
            pe.lineno = lineno;
            pe.padname = null;
            pe.corename = null;

            keyWord = str.nextToken();
            if (keyWord.equals("")) {
                System.out.println("Line " + lineno + ": missing 'cell' name");
                return false;
            }
            pe.cellname = keyWord;

            if (str.hasMoreTokens()) {
                keyWord = str.nextToken();
                pe.padname = keyWord;
                if (str.hasMoreTokens()) {
                    keyWord = str.nextToken();
                    pe.corename = keyWord;
                }
            }
            exports.put(pe.cellname, pe);
            return true;
        }

        private boolean processPlace(StringTokenizer str) {
            PlacePad pad = new PlacePad();
            pad.lineno = lineno;
            pad.exportsname = null;
            pad.gap = 0;
            pad.ni = null;
            pad.associations = new ArrayList();
            if (!str.hasMoreTokens()) {
                err("Cell name missing");
                return false;
            }
            pad.cellname = str.nextToken();

            while (str.hasMoreTokens()) {
                String keyWord = str.nextToken();

                if (keyWord.equals("export")) {
                    // export xxx=xxxx
                    if (!str.hasMoreTokens()) {
                        err("Missing export assignment after 'export' keyword");
                        return false;
                    }
                    keyWord = str.nextToken();
                    PortAssociate pa = new PortAssociate();
                    pa.export = true;
                    pa.portname = getLHS(keyWord);
                    if (pa.portname == null) {
                        err("Bad export assignment after 'export' keyword");
                        return false;
                    }
                    pa.assocname = getRHS(keyWord, str);
                    if (pa.assocname == null) {
                        err("Bad export assignment after 'export' keyword");
                        return false;
                    }
                    pad.associations.add(pa);
                } else {
                    // name=xxxx or gap=xxxx
                    String lhs = getLHS(keyWord);
                    String rhs = getRHS(keyWord, str);
                    if (lhs == null || rhs == null) {
                        err("Parse error on assignment of " + keyWord);
                        return false;
                    }
                    if (lhs.equals("gap")) {
                        try {
                            pad.gap = Integer.parseInt(rhs);
                        } catch (java.lang.NumberFormatException e) {
                            err("Error parsing integer for 'gap' = " + rhs);
                            return false;
                        }
                    } else if (lhs.equals("name")) {
                        pad.exportsname = rhs;
                    } else {
                        // port association
                        PortAssociate pa = new PortAssociate();
                        pa.export = false;
                        pa.portname = lhs;
                        pa.assocname = rhs;
                        pad.associations.add(pa);
                    }
                }
            }
            orderedCommands.add(pad);
            return true;
        }


        private String getLHS(String keyword) {
            if (keyword.indexOf("=") != -1) {
                return keyword.substring(0, keyword.indexOf("="));
            }
            return keyword;
        }

        private String getRHS(String keyword, StringTokenizer str) {
            if (keyword.indexOf("=") != -1) {
                if (keyword.substring(keyword.indexOf("=") + 1).equals("")) {
                    // LHS= RHS
                    if (!str.hasMoreTokens()) return null;
                    return str.nextToken();
                } else {
                    // LHS=RHS
                    return keyword.substring(keyword.indexOf("=") + 1);
                }
            } else {
                if (!str.hasMoreTokens()) return null;
                keyword = str.nextToken();
                if (keyword.equals("=")) {
                    // LHS = RHS
                    if (!str.hasMoreTokens()) return null;
                    return str.nextToken();
                } else {
                    // LHS =RHS
                    return keyword.substring(keyword.indexOf("=") + 1);
                }
            }
        }

        /**
         * Print the error message with the current line number.
         *
         * @param msg
         */
        private void err(String msg) {
            System.out.println("Line " + lineno + ": " + msg);
        }

        private void createPadFrames() {
            if (views.size() == 0) {
                createPadFrame(padframename, null);
            } else {
                for (Iterator it = views.iterator(); it.hasNext();) {
                    View view = (View) it.next();
                    if (view == View.SCHEMATIC) view = View.ICON;
                    createPadFrame(padframename, view);
                }
            }
        }

        private void createPadFrame(String name, View view) {
            angle = 0;

            // first, try to create cell
            if (view != null) {
                if (view == View.ICON) {
                    // create a schematic, place icons of pads in it
                    name = name + "{sch}";
                } else {
                    name = name + "{" + view.getAbbreviation() + "}";
                }
            }

            Cell framecell = Cell.makeInstance(Library.getCurrent(), name);
            if (framecell == null) {
                System.out.println("Could not create pad frame Cell: " + name);
                return;
            }

            List padPorts = new ArrayList();
            List corePorts = new ArrayList();

            NodeInst lastni = null;
            String lastpadname = null;
            // cycle through all orderedCommands, doing them
            for (Iterator it = orderedCommands.iterator(); it.hasNext();) {

                // Rotation commands are ordered with respect to Place commands.
                Object obj = it.next();
                if (obj instanceof Rotation) {
                    angle = (angle + ((Rotation) obj).angle) % 3600;
                    continue;
                }

                // otherwise this is a Place command
                PlacePad pad = (PlacePad) obj;
                lineno = pad.lineno;

                // get cell
                String cellname = pad.cellname;
                if (view != null) cellname = cellname + "{" + view.getAbbreviation() + "}";
                Cell cell = cellLib.findNodeProto(cellname);
                if (cell == null) {
                    err("Could not create pad Cell: " + cellname);
                    continue;
                }

                // if copying cell, copy it into current library
                if (copycells) {
                    cell = CircuitChanges.copyRecursively(cell, cell.getName(), Library.getCurrent(),
                            cell.getView(), false, false, "", false, false, false, false);
                }
                if (cell == null) {
                    err("Could not copy in pad Cell " + cellname);
                    continue;
                }

                // get array alignment for this cell
                ArrayAlign aa = (ArrayAlign) alignments.get(pad.cellname);
                if (aa == null) {
                    err("No port alignment for cell " + pad.cellname);
                    continue;
                }

                int gapx = 0, gapy = 0;
                double centerX = 0, centerY = 0;
                if (lastni != null) {
                    // get info on last nodeinst created
                    ArrayAlign lastaa = (ArrayAlign) alignments.get(lastpadname);

                    // get previous node's outport - use it to place this nodeinst
                    PortProto pp = (lastni.getProto()).findPortProto(lastaa.outport);
                    if (pp == null) {
                        err("no port called '" + lastaa.outport + "' on " + lastni);
                        continue;
                    }

                    Poly poly = (lastni.findPortInstFromProto(pp)).getPoly();
                    centerX = poly.getCenterX();
                    centerY = poly.getCenterY();
                }

                //corneroffset(NONODEINST,np,angle,0,&ox,&oy,false);
                Point2D pointCenter = new Point2D.Double(centerX, centerY);
                NodeInst ni = NodeInst.makeInstance(cell, pointCenter, cell.getDefWidth(), cell.getDefHeight(), framecell, angle, null, 0);
                if (ni == null) {
                    err("problem creating" + cell + " instance");
                    continue;
                }

                if (lastni != null) {
                    switch (angle) {
                        case 0:
                            gapx = pad.gap;
                            gapy = 0;
                            break;
                        case 900:
                            gapx = 0;
                            gapy = pad.gap;
                            break;
                        case 1800:
                            gapx = -pad.gap;
                            gapy = 0;
                            break;
                        case 2700:
                            gapx = 0;
                            gapy = -pad.gap;
                            break;
                    }
                    PortProto inport = cell.findPortProto(aa.inport);

                    if (inport == null) {
                        err("no port called '" + aa.inport + "' on " + cell);
                        continue;
                    }
                    Poly poly = ni.findPortInstFromProto(inport).getPoly();
                    double tempx = centerX - (poly.getCenterX()) - gapx;
                    double tempy = centerY - (poly.getCenterY()) - gapy;
                    ni.modifyInstance(tempx, tempy, 0, 0, 0);
                }

                // create exports

                // get export for this cell, if any
                if (pad.exportsname != null)
				{
                    PadExports pe = (PadExports) exports.get(pad.cellname);
                    if (pe != null)
					{
                        // pad export
                        Export pppad = cell.findExport(pe.padname);
                        if (pppad == null)
						{
                            err("no port called '" + pe.padname + "' on Cell " + cell.noLibDescribe());
                        } else
						{
                            pppad = Export.newInstance(framecell, ni.findPortInstFromProto(pppad), pad.exportsname);
                            if (pppad == null) err("Creating export " + pad.exportsname); else
							{
                                MutableTextDescriptor td = pppad.getMutableTextDescriptor(Export.EXPORT_NAME_TD);
                                td.setAbsSize(14);
								pppad.setTextDescriptor(Export.EXPORT_NAME_TD, td);
								padPorts.add(pppad);
                            }
                        }

						// core export
                        if (pe.corename != null)
						{
                            Export ppcore = cell.findExport(pe.corename);
                            if (ppcore == null)
							{
                                err("no port called '" + pe.corename + "' on Cell " + cell.noLibDescribe());
                            } else 
							{
                                ppcore = Export.newInstance(framecell, ni.findPortInstFromProto(ppcore), "core_" + pad.exportsname);
                                if (ppcore == null) err("Creating export core_" + pad.exportsname); else
								{
                                    MutableTextDescriptor td = ppcore.getMutableTextDescriptor(Export.EXPORT_NAME_TD);
                                    td.setAbsSize(14);
                                    corePorts.add(ppcore);
									ppcore.setTextDescriptor(Export.EXPORT_NAME_TD, td);
                                }
                            }
                        } else
						{
                            corePorts.add(null);
                        }
                    }
                }

                lastni = ni;
                lastpadname = pad.cellname;
                pad.ni = ni;
            }

            WindowFrame frame = WindowFrame.createEditWindow(framecell);

            // select all
            EditMenu.selectAllCommand();
            // auto stitch everything
            AutoStitch.autoStitch(true, true);

            if (corename != null)
			{
                // first, try to create cell
                String corenameview = corename;
                if (view != null) {
                    corenameview = corename + "{" + view.getAbbreviation() + "}";
                }
                Cell corenp = (Cell) Cell.findNodeProto(corenameview);
                if (corenp == null) {
                    System.out.println("Line " + lineno + ": cannot find core cell " + corenameview);
                } else {

                    Rectangle2D bounds = framecell.getBounds();
                    Point2D center = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
                    EditWindow.gridAlign(center);

                    SizeOffset so = corenp.getProtoSizeOffset();
                    NodeInst ni = NodeInst.makeInstance(corenp, center, corenp.getDefWidth(), corenp.getDefHeight(), framecell);

                    for (Iterator ocit = orderedCommands.iterator(); ocit.hasNext();) {
                        Object obj = ocit.next();
                        if (obj instanceof PlacePad) {
                            PlacePad pad = (PlacePad) obj;
                            for (Iterator it = pad.associations.iterator(); it.hasNext();) {
                                PortAssociate pa = (PortAssociate) it.next();
                                if (pad.ni == null) continue;

                                PortProto corepp = corenp.findPortProto(pa.assocname);
								if (corepp == null)
								{
	                                PortInst pi = pad.ni.findPortInst(pa.portname);
									Export.newInstance(pad.ni.getParent(), pi, pa.assocname);
									continue;
								}
                                PortInst pi2 = pad.ni.findPortInst(pa.portname);
                                PortInst pi1 = ni.findPortInstFromProto(corepp);
                                //PortInst pi2 = pad.ni.findPortInstFromProto(pa.pp);
                                ArcProto ap = Generic.tech.unrouted_arc;
                                ArcInst ai = ArcInst.newInstance(ap, ap.getDefaultWidth(), pi1, pi2);
                            }
                        }
                    }
                }
            }

            frame.getContent().fillScreen();


            if (view == View.ICON) {
                // create an icon of our schematic
                //CircuitChanges.makeIconViewCommand();

                // This is a crock until the functionality here can be folded
                // into CircuitChanges.makeIconViewCommand()

                // get icon style controls
                double leadLength = User.getIconGenLeadLength();
                double leadSpacing = User.getIconGenLeadSpacing();

                // create the new icon cell
                String iconCellName = framecell.getName() + "{ic}";
                Cell iconCell = Cell.makeInstance(Library.getCurrent(), iconCellName);
                if (iconCell == null) {
                    JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
                            "Cannot create Icon cell " + iconCellName,
                            "Icon creation failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                iconCell.setWantExpanded();

                // determine the size of the "black box" core
                double ySize = Math.max(Math.max(padPorts.size(), corePorts.size()), 5) * leadSpacing;
                double xSize = 3 * leadSpacing;

                // create the "black box"
                NodeInst bbNi = null;
                if (User.isIconGenDrawBody()) {
                    //bbNi = NodeInst.newInstance(Artwork.tech.boxNode, new Point2D.Double(0,0), xSize, ySize, 0, iconCell, null);
                    bbNi = NodeInst.newInstance(Artwork.tech.openedThickerPolygonNode, new Point2D.Double(0, 0), xSize, ySize, iconCell);
                    if (bbNi == null) return;
                    bbNi.newVar(Artwork.ART_COLOR, new Integer(EGraphics.RED));

                    Point2D[] points = new Point2D.Double[5];
                    points[0] = new Point2D.Double(-0.5 * xSize, -0.5 * ySize);
                    points[1] = new Point2D.Double(-0.5 * xSize, 0.5 * ySize);
                    points[2] = new Point2D.Double(0.5 * xSize, 0.5 * ySize);
                    points[3] = new Point2D.Double(0.5 * xSize, -0.5 * ySize);
                    points[4] = new Point2D.Double(-0.5 * xSize, -0.5 * ySize);
                    bbNi.newVar(NodeInst.TRACE, points);

                    // put the original cell name on it
                    Variable var = bbNi.newDisplayVar(Schematics.SCHEM_FUNCTION, framecell.getName());
//                    if (var != null) {
//                        var.setDisplay(true);
//                    }
               }

                // place pins around the Black Box
                int total = 0;
                int leftSide = padPorts.size();
                int rightSide = corePorts.size();
                for (Iterator it = padPorts.iterator(); it.hasNext();) {
                    Export pp = (Export) it.next();
                    if (pp.isBodyOnly()) continue;

                    // determine location of the port
                    double spacing = leadSpacing;
                    double xPos = 0, yPos = 0;
                    double xBBPos = 0, yBBPos = 0;
                    xBBPos = -xSize / 2;
                    xPos = xBBPos - leadLength;
                    if (leftSide * 2 < rightSide) spacing = leadSpacing * 2;
                    yBBPos = yPos = ySize / 2 - ((ySize - (leftSide - 1) * spacing) / 2 + total * spacing);
                    if (ViewChanges.makeIconExport(pp, 0, xPos, yPos, xBBPos, yBBPos, iconCell))
                        total++;
                }

                total = 0;
                for (Iterator it = corePorts.iterator(); it.hasNext();) {
                    Export pp = (Export) it.next();

                    if (pp == null) {
                        total++;
                        continue;
                    }
                    if (pp.isBodyOnly()) continue;

                    // determine location of the port
                    double spacing = leadSpacing;
                    double xPos = 0, yPos = 0;
                    double xBBPos = 0, yBBPos = 0;
                    xBBPos = xSize / 2;
                    xPos = xBBPos + leadLength;
                    if (rightSide * 2 < leftSide) spacing = leadSpacing * 2;
                    yBBPos = yPos = ySize / 2 - ((ySize - (rightSide - 1) * spacing) / 2 + total * spacing);
                    if (ViewChanges.makeIconExport(pp, 1, xPos, yPos, xBBPos, yBBPos, iconCell))
                        total++;
                }

                // if no body, leads, or cell center is drawn, and there is only 1 export, add more
                if (!User.isIconGenDrawBody() &&
                        !User.isIconGenDrawLeads() &&
                        User.isPlaceCellCenter() &&
                        total <= 1) {
                    NodeInst.newInstance(Generic.tech.invisiblePinNode, new Point2D.Double(0, 0), xSize, ySize, iconCell);
                }

                // place an icon in the schematic
                int exampleLocation = User.getIconGenInstanceLocation();
                Point2D iconPos = new Point2D.Double(0, 0);
                Rectangle2D cellBounds = framecell.getBounds();
                Rectangle2D iconBounds = iconCell.getBounds();
                double halfWidth = iconBounds.getWidth() / 2;
                double halfHeight = iconBounds.getHeight() / 2;
                switch (exampleLocation) {
                    case 0:		// upper-right
                        iconPos.setLocation(cellBounds.getMaxX() + halfWidth, cellBounds.getMaxY() + halfHeight);
                        break;
                    case 1:		// upper-left
                        iconPos.setLocation(cellBounds.getMinX() - halfWidth, cellBounds.getMaxY() + halfHeight);
                        break;
                    case 2:		// lower-right
                        iconPos.setLocation(cellBounds.getMaxX() + halfWidth, cellBounds.getMinY() - halfHeight);
                        break;
                    case 3:		// lower-left
                        iconPos.setLocation(cellBounds.getMinX() - halfWidth, cellBounds.getMinY() - halfHeight);
                        break;
                }
                EditWindow.gridAlign(iconPos);
                double px = iconCell.getBounds().getWidth();
                double py = iconCell.getBounds().getHeight();
                NodeInst ni = NodeInst.makeInstance(iconCell, iconPos, px, py, framecell);
            }
        }
    }

    public static void generate(String fileName) {
        if (fileName == null) return;
        PadFrame padFrame = new PadFrame(fileName);
    }

}

