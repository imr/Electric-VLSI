package com.sun.electric.tool.cvspm;

import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.output.DELIB;
import com.sun.electric.tool.user.User;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 22, 2006
 * Time: 10:26:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class AddRemove {

    public static void add(Library lib) {
        if (!CVS.assertNotModified(lib, "Add", true)) return;

        List<Library> libs = new ArrayList<Library>();
        libs.add(lib);
        (new AddRemoveJob(libs, null, true)).startJob();
    }

    public static void add(Cell cell) {
        if (!CVS.assertNotModified(cell, "Add", true)) return;

        List<Cell> cells = new ArrayList<Cell>();
        cells.add(cell);
        (new AddRemoveJob(null, cells, true)).startJob();
    }

    public static void remove(Library lib) {
        if (!CVS.assertNotModified(lib, "Remove", true)) return;

        List<Library> libs = new ArrayList<Library>();
        libs.add(lib);
        (new AddRemoveJob(libs, null, false)).startJob();
    }

    public static void remove(Cell cell) {
        if (!CVS.assertNotModified(cell, "Remove", true)) return;

        List<Cell> cells = new ArrayList<Cell>();
        cells.add(cell);
        (new AddRemoveJob(null, cells, false)).startJob();
    }

    public static class AddRemoveJob extends Job {
        private List<Library> libs;
        private List<Cell> cells;
        private boolean add;
        private HashMap<String,Integer> addedCellDirs;
        private AddRemoveJob(List<Library> libs, List<Cell> cells, boolean add) {
            super("CVS Add/Remove", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.libs = libs;
            this.cells = cells;
            this.add = add;
            if (this.libs == null) this.libs = new ArrayList<Library>();
            if (this.cells == null) this.cells = new ArrayList<Cell>();
            addedCellDirs = new HashMap<String,Integer>();
        }
        public boolean doIt() {
            String useDir = CVS.getUseDir(libs, cells);

            // mark files as added/removed
            for (Library lib : libs) {
                for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
                    Cell cell = it.next();
                    if (add) {
                        if (!CVS.isFileInCVS(CVS.getCellFile(cell)))
                            CVSLibrary.setState(cell, State.ADDED);
                    } else {
                        if (CVS.isFileInCVS(CVS.getCellFile(cell)))
                            CVSLibrary.setState(cell, State.REMOVED);
                    }
                }
            }
            for (Cell cell : cells) {
                if (add) {
                    if (!CVS.isFileInCVS(CVS.getCellFile(cell)))
                        CVSLibrary.setState(cell, State.ADDED);
                } else {
                    if (CVS.isFileInCVS(CVS.getCellFile(cell)))
                        CVSLibrary.setState(cell, State.REMOVED);
                }
            }

            // unfortunately add/remove are not recursive, so we have
            // to specify directories as well as files to add/remove
            StringBuffer buf = new StringBuffer();
            for (Library lib : libs) {
                generate(buf, lib, useDir);
            }
            for (Cell cell : cells) {
                generate(buf, cell, useDir);
            }

            String addRemoveFiles = buf.toString();
            if (addRemoveFiles.trim().equals("")) return true;
            String command = add ? "add" : "remove";
            String message = "Running CVS " + (add ? "Add" : "Remove");
            CVS.runCVSCommand("-q "+command+" "+addRemoveFiles, message,
                    useDir, System.out);
            System.out.println("CVS "+command+" complete");
            return true;
        }
        private void generate(StringBuffer buf, Library lib, String useDir) {
            // see if library file is in CVS
            String libfile = lib.getLibFile().getPath();
            add(buf, libfile, useDir);
            if (CVS.isDELIB(lib)) {
                // see if cell directories are in CVS
                for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
                    generate(buf, it.next(), useDir);
                }
                // add header file
                File headerFile = new File(libfile, "header");
                add(buf, headerFile.getPath(), useDir);
            }
        }
        private void generate(StringBuffer buf, Cell cell, String useDir) {
            String libfile = cell.getLibrary().getLibFile().getPath();
            // get cell directory if not already added before
            File celldirFile = new File(libfile, DELIB.getCellSubDir(cell.backup()));
            String celldir = celldirFile.getPath();
            if (!addedCellDirs.containsKey(celldir)) {
                add(buf, celldir, useDir);
                addedCellDirs.put(celldir, null);
            }
            // check cell files
            File cellFile = new File(libfile, DELIB.getCellFile(cell.backup()));
            add(buf, cellFile.getPath(), useDir);
        }
        private void add(StringBuffer buf, String file, String useDir) {
            File FD = new File(file);
            if ((add && !CVS.isFileInCVS(FD)) ||
                (!add && CVS.isFileInCVS(FD))) {

                if (file.startsWith(useDir)) {
                    file = file.substring(useDir.length()+1, file.length());
                }
                buf.append(file+" ");
                if (!add) {
                    // need to delete local file first
                    if (!FD.isDirectory()) {
                        if (!FD.delete()) {
                            System.out.println("Error: could not delete file to removed: "+FD.getPath());
                        }
                    }
                }
            }
        }
    }
}
