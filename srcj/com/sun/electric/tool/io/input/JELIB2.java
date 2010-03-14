/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JELIB2.java
 * Input/output tool: JELIB Library input
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.CellRevision;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableCell;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.ImmutableIconInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.Variable;
import java.util.Collections;
import java.util.Comparator;

import java.util.LinkedHashSet;

/**
 * This class reads files in new library file (.jelib) format to immutable database.
 */
public class JELIB2 {

    JelibParser parser;
    LinkedHashSet<CellRevision> cellRevisions = new LinkedHashSet<CellRevision>();

    public JELIB2(JelibParser parser) {
        this.parser = parser;
    }

    public boolean instantiate() throws JelibException {
        try {
            return instantiate0();
        } catch (IllegalArgumentException e) {
            if (parser == null) {
                throw e;
            }
            for (JelibParser.CellContents cc: parser.allCells.values()) {
                Collections.sort(cc.nodes, new Comparator<JelibParser.NodeContents> () {
                    public int compare(JelibParser.NodeContents x, JelibParser.NodeContents y) {
                        return TextUtils.STRING_NUMBER_ORDER.compare(x.nodeName, y.nodeName);
                    }
                });
                Collections.sort(cc.exports, new Comparator<JelibParser.ExportContents> () {
                    public int compare(JelibParser.ExportContents x, JelibParser.ExportContents y) {
                        String xn = x.exportUserName != null ? x.exportUserName : x.exportId.externalId;
                        String yn = y.exportUserName != null ? y.exportUserName : y.exportId.externalId;
                        return TextUtils.STRING_NUMBER_ORDER.compare(xn, yn);
                    }
                });
            }
            return instantiate0();
        }
    }

    public boolean instantiate0() throws JelibException {
        for (JelibParser.CellContents cc: parser.allCells.values()) {
            CellId cellId = cc.cellId;
            ImmutableCell c = ImmutableCell.newInstance(cellId, cc.creationDate)
                    .withGroupName(cc.groupName)
                    .withRevisionDate(cc.revisionDate)
                    .withTechId(cc.techId);
            // c.withParam
            
            int flags = 0;
            if (cc.expanded) flags |= Cell.WANTNEXPAND;
            if (cc.allLocked) flags |= Cell.NPLOCKED;
            if (cc.instLocked) flags |= Cell.NPILOCKED;
            if (cc.cellLib) flags |= Cell.INCELLLIBRARY;
            if (cc.techLib) flags |= Cell.TECEDITCELL;
            c = c.withFlags(flags);

            for (Variable var: cc.vars) {
                if (var.getTextDescriptor().isParam()) {
                    c = c.withParam(var);
                } else {
                    c = c.withVariable(var);
                }
            }
            
            ImmutableNodeInst[] nodes = new ImmutableNodeInst[cc.nodes.size()];
            for (int nodeId = 0; nodeId < nodes.length; nodeId++) {
                JelibParser.NodeContents nc = cc.nodes.get(nodeId);
                ImmutableNodeInst n = ImmutableNodeInst.newInstance(nodeId, nc.protoId,
                        Name.findName(nc.nodeName), nc.nameTextDescriptor,
                        nc.orient, nc.anchor, nc.size, nc.flags, nc.techBits, nc.protoTextDescriptor);
                for (Variable var: nc.vars) {
                    if (var.getKey().getName().startsWith("ATTRP")) {
                        // Hanle PortInst variables
                        throw new JelibException();
                    }
                    if (n instanceof ImmutableIconInst && var.getTextDescriptor().isParam()) {
                        n = ((ImmutableIconInst)n).withParam(var);
                    } else {
                        n = n.withVariable(var);
                    }
                }
                nc.n = nodes[nodeId] = n;
            }

            ImmutableArcInst[] arcs = new ImmutableArcInst[cc.arcs.size()];
            for (int arcId = 0; arcId < arcs.length; arcId++) {
                JelibParser.ArcContents ac = cc.arcs.get(arcId);
                ImmutableArcInst a = ImmutableArcInst.newInstance(arcId, ac.arcProtoId, Name.findName(ac.arcName), ac.nameTextDescriptor,
                        ac.tailNode.n.nodeId, ac.tailPort, ac.tailPoint,
                        ac.headNode.n.nodeId, ac.headPort, ac.headPoint,
                        DBMath.lambdaToGrid(ac.diskWidth), ac.angle, ac.flags);
                for (Variable var: ac.vars) {
                    a = a.withVariable(var);
                }
                arcs[arcId] = a;
            }

            ImmutableExport[] exports = new ImmutableExport[cc.exports.size()];
            for (int exportIndex = 0; exportIndex < exports.length; exportIndex++) {
                JelibParser.ExportContents ec = cc.exports.get(exportIndex);
                String exportName = ec.exportUserName != null ? ec.exportUserName : ec.exportId.externalId;
                ImmutableExport e = ImmutableExport.newInstance(ec.exportId, Name.findName(exportName), ec.nameTextDescriptor,
                        ec.originalNode.n.nodeId, ec.originalPort, ec.alwaysDrawn, ec.bodyOnly, ec.ch);
                for (Variable var: ec.vars) {
                    e = e.withVariable(var);
                }
                exports[exportIndex] = e;
            }

            CellRevision cellRevision = new CellRevision(c).with(c,  nodes, arcs, exports);

            cellRevisions.add(cellRevision);
        }
        parser = null;
        return true;
    }

    public void check() throws JelibException {
        for (CellRevision cellRevision: cellRevisions) {
            String protoName = cellRevision.d.cellId.cellName.getName();

            for (int i = 0; i < protoName.length(); i++) {
                char chr = protoName.charAt(i);
                if (Character.isWhitespace(chr) || chr == ':' || chr == ';' || chr == '{' || chr == '}') {
                    throw new JelibException();
                }
            }

        }
    }
    
    /*
     * TODO
     * JelibParser lost bad CellNames,
     * JelibParset lost duplicate CellNames 
     * check CellGroups: 1) same name => same group; 2) Rules for group name
     * check Variables and Params (see LibraryFiles.realizeCellParameters).
     * check ImmutableNodes
     * check SizeCorrector is zero
     * check ATTRP_ is absent
     */

    private static class JelibException extends Exception {
        private JelibException() {
            super();
        }
    }
}
