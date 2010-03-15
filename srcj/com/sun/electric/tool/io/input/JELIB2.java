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

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellRevision;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableCell;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.ImmutableIconInst;
import com.sun.electric.database.ImmutableLibrary;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.ImmutablePortInst;
import com.sun.electric.database.LibraryBackup;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.constraint.Layout;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.id.PortProtoId;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.TechPool;
import com.sun.electric.tool.Consumer;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.MultiTaskJob;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.OpenFile;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * This class reads files in new library file (.jelib) format to immutable database.
 */
public class JELIB2 {

    LibId libId;
    JelibParser parser;
    public LibraryBackup libBackup;
    ArrayList<CellRevision> cellRevisions = new ArrayList<CellRevision>();
    public ArrayList<CellBackup> cellBackups = new ArrayList<CellBackup>();

    public JELIB2(LibId libId, JelibParser parser) {
        this.libId = libId;
        this.parser = parser;
    }

    public CellRevision[] getCellRevisions() {
        return cellRevisions.toArray(new CellRevision[cellRevisions.size()]);
    }

    public boolean instantiate(TechPool techPool, boolean primitiveBounds) throws JelibException {
        ImmutableLibrary l = ImmutableLibrary.newInstance(libId, parser.fileURL, parser.version);
        for (Variable var : parser.libVars) {
            l = l.withVariable(var);
        }
        boolean modified = false;

        for (JelibParser.CellContents cc : parser.allCells.values()) {
            CellId cellId = cc.cellId;
            ImmutableCell c = ImmutableCell.newInstance(cellId, cc.creationDate).withGroupName(cc.groupName).withRevisionDate(cc.revisionDate).withTechId(cc.techId);

            int flags = 0;
            if (cc.expanded) {
                flags |= Cell.WANTNEXPAND;
            }
            if (cc.allLocked) {
                flags |= Cell.NPLOCKED;
            }
            if (cc.instLocked) {
                flags |= Cell.NPILOCKED;
            }
            if (cc.cellLib) {
                flags |= Cell.INCELLLIBRARY;
            }
            if (cc.techLib) {
                flags |= Cell.TECEDITCELL;
            }
            c = c.withFlags(flags);

            for (Variable var : cc.vars) {
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
                for (Variable var : nc.vars) {
                    String origVarName = var.getKey().getName();
                    if (origVarName.startsWith("ATTRP")) {
                        // the form is "ATTRP_portName_variableName" with "\" escapes
                        StringBuilder portName = new StringBuilder();
                        String varName = null;
                        int len = origVarName.length();
                        for (int j = 6; j < len; j++) {
                            char ch = origVarName.charAt(j);
                            if (ch == '\\') {
                                j++;
                                portName.append(origVarName.charAt(j));
                                continue;
                            }
                            if (ch == '_') {
                                varName = origVarName.substring(j + 1);
                                break;
                            }
                            portName.append(ch);
                        }
                        if (varName != null) {
                            PortProtoId ppId = nc.protoId.newPortId(portName.toString());
                            ImmutablePortInst pi = n.getPortInst(ppId);
                            var = var.withVarKey(Variable.newKey(varName));
                            n = n.withPortInst(ppId, pi.withVariable(var));
                            continue;
                        }
                    }
                    if (n instanceof ImmutableIconInst && var.getTextDescriptor().isParam()) {
                        n = ((ImmutableIconInst) n).withParam(var);
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
                        DBMath.lambdaToGrid(0.5 * ac.diskWidth), ac.angle, ac.flags);
                for (Variable var : ac.vars) {
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
                for (Variable var : ec.vars) {
                    e = e.withVariable(var);
                }
                exports[exportIndex] = e;
            }

            CellRevision cellRevision = new CellRevision(c);
            CellBackup cellBackup = techPool != null ? CellBackup.newInstance(c, techPool) : null;
            try {
                if (techPool != null) {
                    cellBackup = cellBackup.with(c, nodes, arcs, exports, techPool).withoutModified();;
                } else {
                    cellRevision = cellRevision.with(c, nodes, arcs, exports);
                }
            } catch (IllegalArgumentException e) {
                Arrays.sort(nodes, new Comparator<ImmutableNodeInst>() {

                    public int compare(ImmutableNodeInst n1, ImmutableNodeInst n2) {
                        return TextUtils.STRING_NUMBER_ORDER.compare(n1.name.toString(), n2.name.toString());
                    }
                });
                Arrays.sort(arcs, new Comparator<ImmutableArcInst>() {

                    public int compare(ImmutableArcInst a1, ImmutableArcInst a2) {
                        return TextUtils.STRING_NUMBER_ORDER.compare(a1.name.toString(), a2.name.toString());
                    }
                });
                Arrays.sort(exports, new Comparator<ImmutableExport>() {

                    public int compare(ImmutableExport e1, ImmutableExport e2) {
                        return TextUtils.STRING_NUMBER_ORDER.compare(e1.name.toString(), e2.name.toString());
                    }
                });
                if (techPool != null) {
                    cellBackup = cellBackup.with(c, nodes, arcs, exports, techPool);
                } else {
                    cellRevision = cellRevision.with(c, nodes, arcs, exports);
                }
                modified = true;
            }

            if (techPool != null) {
                if (primitiveBounds) {
                    cellBackup.getPrimitiveBounds();
                }
                cellBackups.add(cellBackup);
                cellRevisions.add(cellBackup.cellRevision);
            } else {
                cellRevisions.add(cellRevision);
            }
        }
        libBackup = new LibraryBackup(l, modified, LibId.NULL_ARRAY);
        parser = null;
        return true;
    }

    public void check() throws JelibException {
        for (CellRevision cellRevision : cellRevisions) {
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

    public static void newJelibReader(boolean instantiate, boolean doBackup, boolean getPrimitiveBounds, boolean doSnapshot, final boolean doDatabase) {
        String fileName = OpenFile.chooseInputFile(FileType.LIBFILE, "Top library");
        if (fileName == null) {
            return;
        }
        URL externalURL = TextUtils.makeURLToFile(fileName);
        if (externalURL == null) {
            return;
        }
        if (!TextUtils.URLExists(externalURL, null)) {
            return;
        }
        Consumer<Snapshot> consumer = new Consumer<Snapshot>() {

            public void consume(Snapshot snapshot) {
                if (doDatabase && snapshot != null) {
                    new JelibReaderCommitJob(snapshot).startJobOnMyResult();
                }
            }
        };
        new JelibReaderMultiTaskJob(externalURL, instantiate, doBackup, getPrimitiveBounds, doSnapshot, doDatabase, false, consumer).startJob();
    }

    private static class JelibReaderMultiTaskJob extends MultiTaskJob<URL, JELIB2, Snapshot> {

        private final boolean instantiate;
        private final boolean doBackup;
        private final boolean getPrimitiveBounds;
        private final boolean doSnapshot;
        private final boolean doDatabase;
        private final boolean check;
        private final URL topLibFile;
        private String mainLibDirectory;
        private transient IdManager idManager;
        private transient HashSet<LibId> externalLibraries;

        private JelibReaderMultiTaskJob(URL topLibFile, boolean instantiate, boolean doBackup, boolean getPrimitiveBounds, boolean doSnapshot, boolean doDatabase, boolean check, Consumer<Snapshot> c) {
            super("JelibReaderMultiTaskJobLight", null, c);
            this.topLibFile = topLibFile;
            this.instantiate = instantiate;
            this.doBackup = doBackup;
            this.getPrimitiveBounds = getPrimitiveBounds;
            this.doSnapshot = doSnapshot;
            this.doDatabase = doDatabase;
            this.check = check;
            String topLibName = TextUtils.getFileNameWithoutExtension(topLibFile);
            mainLibDirectory = TextUtils.getFilePath(topLibFile);
            FileType type = FileType.findTypeByExtension(TextUtils.getExtension(topLibFile));
            if (type == FileType.DELIB) {
                mainLibDirectory = mainLibDirectory.replaceAll(topLibName + "." + type.getFirstExtension(), "");
            }
        }

        /**
         * This abstract method split large computation into smaller task.
         * Smaller tasks are identified by TaskKey class.
         * Each task is scheduled by startTask method.
         * @throws com.sun.electric.tool.JobException
         */
        @Override
        public void prepareTasks() throws JobException {
            if (false) {
                idManager = new IdManager();
            } else {
                idManager = IdManager.stdIdManager;
            }
            externalLibraries = new HashSet<LibId>();
            if (check) {
                for (Iterator<Library> it = Library.getLibraries(); it.hasNext();) {
                    Library lib = it.next();
                    URL libFile = lib.getLibFile();
                    if (libFile == null) {
                        continue;
                    }
                    startTask(libFile.toString(), libFile);
                }
            } else {
                externalLibraries.add(idManager.newLibId(TextUtils.getFileNameWithoutExtension(topLibFile)));
                startTask(topLibFile.toString(), topLibFile);
            }
        }

        /**
         * This abtract methods performs computation of each task.
         * @param taskKey task key which identifies the task
         * @return result of task computation
         * @throws com.sun.electric.tool.JobException
         */
        @Override
        public JELIB2 runTask(URL libFile) throws JobException {
            LibId libId = idManager.newLibId(TextUtils.getFileNameWithoutExtension(libFile));
            Date startDate = new Date();
            FileType fileType = FileType.findTypeByExtension(TextUtils.getExtension(libFile));
            JELIB2 jelib2 = null;
            ErrorLogger errorLogger = ErrorLogger.newInstance(libId.toString());
            TechPool techPool = doBackup ? getTechPool() : null;
            try {
                JelibParser parser = JelibParser.parse(libId, libFile, fileType, false, errorLogger);
                for (Map.Entry<LibId, String> e : parser.externalLibIds.entrySet()) {
                    LibId externallibId = e.getKey();
                    String libFileName = e.getValue();
                    if (Library.findLibrary(libId.libName) != null) {
                        continue;
                    }
                    startTask(externallibId, libFileName, fileType);
                }

                jelib2 = new JELIB2(libId, parser);
                boolean ok = false;
                if (instantiate) {
                    ok = jelib2.instantiate(techPool, getPrimitiveBounds);
                }
                Date stopDate = new Date();
                System.out.println("Parsing " + errorLogger.getInfo() + " " + ok + " " + startDate + " " + stopDate + " " + Thread.currentThread().getName());

            } catch (Exception e) {
                System.out.println("Parsing " + libId + " caused an exception " + e);
                e.printStackTrace();
            }
            if (check) {
                checkLibrary(libId, jelib2);
            }
            return jelib2;
        }

        private void startTask(LibId externalLibId, String libFileName, FileType fileType) {
            synchronized (externalLibraries) {
                if (externalLibraries.contains(externalLibId)) {
                    return;
                }
            }
            URL fileURL = LibraryFiles.searchExternalLibraryFromFilename(mainLibDirectory, libFileName, fileType);
            if (fileURL == null) {
                return;
            }
            synchronized (externalLibraries) {
                if (!externalLibraries.add(externalLibId)) {
                    return;
                }
            }
            startTask(fileURL.toString(), fileURL);
        }

        private void checkLibrary(LibId libId, JELIB2 jelib2) {
            EDatabase database = getDatabase();
            CellRevision[] cellRevisions = jelib2.getCellRevisions();
            Library lib = database.getLib(libId);
            assert lib.getNumCells() == cellRevisions.length;
            for (CellRevision cellRevision1 : cellRevisions) {
                ImmutableCell c1 = cellRevision1.d;
                CellId cellId = c1.cellId;
                CellRevision cellRevision2 = database.getCell(cellId).backup().cellRevision;
                ImmutableCell c2 = cellRevision2.d;
                assert c1.equalsExceptVariables(c2) && c1.equalsVariables(c2);
                assert cellRevision1.nodes.size() == cellRevision2.nodes.size();

                assert cellRevision1.nodes.size() == cellRevision2.nodes.size();
                for (int nodeId = 0; nodeId < cellRevision1.nodes.size(); nodeId++) {
                    ImmutableNodeInst n1 = cellRevision1.nodes.get(nodeId);
                    ImmutableNodeInst n2 = cellRevision2.nodes.get(nodeId);
                    assert n1.equalsExceptVariables(n2) && n1.equalsVariables(n2);
                }

                assert cellRevision1.arcs.size() == cellRevision2.arcs.size();
                for (int arcId = 0; arcId < cellRevision1.arcs.size(); arcId++) {
                    ImmutableArcInst a1 = cellRevision1.arcs.get(arcId);
                    ImmutableArcInst a2 = cellRevision2.arcs.get(arcId);
                    assert a1.equalsExceptVariables(a2) && a1.equalsVariables(a2);
                }

                assert cellRevision1.exports.size() == cellRevision2.exports.size();
                for (int exportId = 0; exportId < cellRevision1.exports.size(); exportId++) {
                    ImmutableExport e1 = cellRevision1.exports.get(exportId);
                    ImmutableExport e2 = cellRevision2.exports.get(exportId);
                    assert e1.equalsExceptVariables(e2) && e1.equalsVariables(e2);
                }
            }
        }

        /**
         * This abtract method combines task results into final result.
         * @param taskResults map which contains result of each completed task.
         * @return final result which is obtained by merging task results.
         * @throws com.sun.electric.tool.JobException
         */
        @Override
        public Snapshot mergeTaskResults(Map<URL, JELIB2> taskResults) throws JobException {
            if (!doSnapshot) {
                return null;
            }
            ArrayList<LibraryBackup> libBackups = new ArrayList<LibraryBackup>();
            ArrayList<CellBackup> cellBackups = new ArrayList<CellBackup>();
            if (doDatabase) {
                Snapshot oldSnapshot = getDatabase().backup();
                libBackups.addAll(oldSnapshot.libBackups);
                cellBackups.addAll(oldSnapshot.cellBackups);
            }
            for (JELIB2 jelib2 : taskResults.values()) {
                LibraryBackup libBackup = jelib2.libBackup;
                int libIndex = libBackup.d.libId.libIndex;
                while (libIndex >= libBackups.size()) {
                    libBackups.add(null);
                }
                libBackups.set(libIndex, libBackup);

                for (CellBackup cellBackup : jelib2.cellBackups) {
                    int cellIndex = cellBackup.cellRevision.d.cellId.cellIndex;
                    while (cellIndex >= cellBackups.size()) {
                        cellBackups.add(null);
                    }
                    cellBackups.set(cellIndex, cellBackup);
                }
            }

            try {
                Snapshot snapshot = idManager.getInitialSnapshot().with(null, getEnvironment(),
                    cellBackups.toArray(new CellBackup[cellBackups.size()]),
                    libBackups.toArray(new LibraryBackup[libBackups.size()]));

                return snapshot;
            } catch (Exception e) {
                e.printStackTrace(System.out);
                getUserInterface().showErrorMessage("Error loading libraries", "New JELIB Reader");
                return null;
            }
        }
    }

    private static class JelibReaderCommitJob extends Job {

        private transient Snapshot newSnapshot;

        protected JelibReaderCommitJob(Snapshot newSnapshot) {
            super("JelibReaderCommit", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.newSnapshot = newSnapshot;
        }

        @Override
        public boolean doIt() {
            Layout.changesQuiet(true);
            EDatabase database = getDatabase();
            database.lowLevelSetCanUndoing(true);
            try {
                database.undo(newSnapshot);
            } finally {
                database.lowLevelSetCanUndoing(false);
            }
            return true;
        }
    }
}
