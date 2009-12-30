/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LibraryStatistics.java
 * Input/output tool: Statistics over a set of libraries.
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.io.ELIBConstants;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.JelibParser;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
import com.sun.electric.tool.user.ErrorLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

/**
 * This class keeps contents of disk library file.
 */
public class LibraryStatistics implements Serializable
{
    private static final long serialVersionUID = -361650802811567400L;
    private transient IdManager idManager;
	private final TreeMap<String,Directory> directories = new TreeMap<String,Directory>();
	private final TreeMap<String,LibraryName> libraryNames = new TreeMap<String,LibraryName>();
//	transient LibraryContents totalLibraryContents;

	private LibraryStatistics(IdManager idManager) {
        this.idManager = idManager;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        idManager = new IdManager();
        in.defaultReadObject();
    }

	Directory getDirectory(String dirName)
	{
		Directory dir = directories.get(dirName);
		if (dir == null) dir = new Directory(this, dirName);
		return dir;
	}

	LibraryName getLibraryName(String name)
	{
		LibraryName libraryName = libraryNames.get(name);
		if (libraryName == null) libraryName = new LibraryName(this, name);
		return libraryName;
	}

	Iterator<Directory> getDirectories() { return directories.values().iterator(); }
	Iterator<LibraryName> getLibraryNames() { return libraryNames.values().iterator(); }

	public static void scanProjectDirs(String[] dirNames, String[] excludeDirs, File projListDir) {
		HashSet<String> canonicalDirs = new HashSet<String>();
        TreeSet<String> projectDirs = new TreeSet<String>();
		for (int i = 0; i < dirNames.length; i++)
			scanProjectDir(new File(dirNames[i]), excludeDirs, canonicalDirs, projectDirs);
        File projListFile = new File(projListDir, "proj.list");
        try {
            PrintWriter out = new PrintWriter(projListFile);
            for (Iterator it = projectDirs.iterator(); it.hasNext(); )
                out.println((String)it.next());
            out.close();
        } catch (IOException e) {
            System.out.println("Error writing " + projListFile);
            e.printStackTrace();
        }
    }

	private static void scanProjectDir(File dir, String[] excludeDirs, Set<String> canonicalDirs, TreeSet<String> projectDirs) {
		try {
			String canonicalDir = dir.getCanonicalPath();
			if (canonicalDirs.contains(canonicalDir)) return;
			canonicalDirs.add(canonicalDir);
            if (!canonicalDir.equals(dir.getPath()))
                System.out.println(dir + " -> " + canonicalDir);
			dir = new File(canonicalDir);
            assert dir.getPath().equals(canonicalDir);
		} catch (IOException e) {
			System.out.println(dir + " CANONICAL FAILED");
			return;
		}
        for (String excludeDir: excludeDirs) {
            if (dir.getPath().equals(excludeDir)) {
    			System.out.println(dir + " EXCLUDED");
                return;
            }
        }
		File[] files = dir.listFiles();
		if (files == null) {
			System.out.println(dir + " ACCESS DENIED");
			return;
		}
        boolean libdirs = false;
        int xmls = 0;
        int txts = 0;
        int elibs = 0;
        int jelibs = 0;
		int delibs = 0;
		for (File file: files) {
			String name = file.getName();
			if (name.startsWith("._")) continue;
			int extensionPos = name.lastIndexOf('.');
			if (extensionPos < 0) continue;
			String extension = name.substring(extensionPos);
			name = name.substring(0, extensionPos);

            if (file.isDirectory()) {
                if (extension.equals(".delib"))
                    delibs++;
            } else {
                if (name.equals("LIBDIRS"))
                    libdirs = true;
                if (extension.equals(".xml"))
                    xmls++;
                if (extension.equals(".txt"))
                    txts++;
                if (extension.equals(".elib"))
                    elibs++;
                if (extension.equals(".jelib"))
                    jelibs++;
            }
        }
		if (delibs > 0 || elibs > 0 || jelibs > 0) {
            String projectDir = dir.getPath();
            System.out.print(projectDir + " :");
            if (libdirs)
                System.out.print(" LIBDIRS");
            if (xmls > 0)
                System.out.print(" " + xmls + " xmls");
            if (txts > 0)
                System.out.print(" " + txts + " txts");
            if (elibs > 0)
                System.out.print(" " + elibs + " elibs");
            if (jelibs > 0)
                System.out.print(" " + jelibs + " jelibs");
            if (delibs > 0)
                System.out.print(" " + delibs + " delibs");
            System.out.println();
            assert canonicalDirs.contains(projectDir);
            boolean added = projectDirs.add(projectDir);
            assert added;
		}
		for (File file: files) {
			if (!file.isDirectory()) continue;
            if (file.getName().equals("CVS")) continue;
			scanProjectDir(file, excludeDirs, canonicalDirs, projectDirs);
		}
	}

    public static TreeSet<String> readProjList(File wrkDir) {
        File projListFile = new File(wrkDir, "proj.list");
        try {
            TreeSet<String> dirs = new TreeSet<String>();
            BufferedReader in = new BufferedReader(new FileReader(projListFile));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.length() == 0) continue;
                boolean added = dirs.add(line);
                assert added;
            }
            in.close();
            return dirs;
        } catch (IOException e) {
            System.out.println("Error reading " + projListFile + " : " + e);
            return null;
        }
    }

    public static Map<String,File[]> readProjectDirs(File wrkDir, boolean allDirs) {
        final String projectsExt = ".projects";
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) { return name.endsWith(projectsExt); }
        };
        Map<String,File[]> projectDirs = new TreeMap<String,File[]>();
        for (File file: wrkDir.listFiles(filter)) {
            String projectName = file.getName();
            assert projectName.endsWith(projectsExt);
            projectName = projectName.substring(0, projectName.length() - projectsExt.length());
            try {
                BufferedReader in = new BufferedReader(new FileReader(file));
                ArrayList<File> dirs = new ArrayList<File>();
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.length() == 0) continue;
                    char firstChar = line.charAt(0);
                    if (firstChar == '-') {
                        if (!allDirs) continue;
                    } else if (firstChar != '+') {
                        continue;
                    }
                    String fileName = line.substring(1);
                    dirs.add(new File(fileName));
                }
                in.close();
                projectDirs.put(projectName, dirs.toArray(new File[dirs.size()]));
            } catch (IOException e) {
                System.out.println("Error reading " + file);
                e.printStackTrace();
            }
        }
        return projectDirs;
    }

	public static LibraryStatistics scanDirectories(IdManager idManager, File[] dirs)
	{
		LibraryStatistics stat = new LibraryStatistics(idManager);
		Set<String> canonicalDirs = new HashSet<String>();
		Map<String,Set<FileInstance>> preLibraries = new HashMap<String,Set<FileInstance>>();

		for (File dir: dirs)
			stat.scanDir(dir, canonicalDirs, preLibraries, null);

		byte[] buf = new byte[Input.READ_BUFFER_SIZE];
		for (Iterator lit = preLibraries.entrySet().iterator(); lit.hasNext(); )
		{
			Map.Entry entry = (Map.Entry)lit.next();
			String libName = (String)entry.getKey();
			TreeSet files = (TreeSet)entry.getValue();
			LibraryName libraryName = new LibraryName(stat, libName);
			while (!files.isEmpty())
			{
				FileInstance f = (FileInstance)files.iterator().next();
				files.remove(f);
				FileContents fc = new FileContents(libraryName, f);
				byte[] bytes = null;
				int len = (int)f.fileLength;
			fileLoop:
				for (Iterator it = files.iterator(); it.hasNext(); )
				{
					FileInstance f1 = (FileInstance)it.next();
					if (f1.fileLength != len || f1.crc != f.crc) continue;
					if (!f.canonicalPath.equals(f1.canonicalPath))
					{
						if (bytes == null)
						{
							Input in = new Input();
							if (in.openBinaryInput(f1.getUrl())) continue;
							try {
								bytes = new byte[len];
								try {
									in.dataInputStream.readFully(bytes);
								} catch (IOException e)
								{
									continue;
								}
							} finally
							{
								in.closeInput();
							}
						}
						Input in = new Input();
						if (in.openBinaryInput(f1.getUrl())) continue;
						try
						{
							int n = 0;
							while (n < len) {
								int count = -1;
								try {
									count = in.dataInputStream.read(buf, 0, Math.min(len - n, buf.length));
								} catch (IOException ex)
								{
								}
								if (count < 0) continue fileLoop;
								for (int i=0; i<count; i++)
									if (buf[i] != bytes[n + i])
										{
											in.closeInput();
											continue fileLoop;
										}
								n += count;
							}
						} finally
						{
							in.closeInput();
						}
					}
					it.remove();
					fc.add(f1);
				}
			}
		}

		return stat;
	}

	private void scanDir(File dir, Set<String> canonicalDirs,
						 Map<String,Set<FileInstance>> preLibraries, String parentLibName)
	{
		try
		{
			String canonicalDir = dir.getCanonicalPath();
			if (canonicalDirs.contains(canonicalDir)) return;
			canonicalDirs.add(canonicalDir);
			dir = new File(canonicalDir);
		} catch (IOException e)
		{
			System.out.println(dir + " CANONICAL FAILED");
			return;
		}
        if (dir.getPath().equals("/import/async/archive/2005/tic/gilda/TreasureIsland/electric-old/projectManagement") ||
            dir.getPath().equals("/import/async/archive/2005/tic/jkg/projectTest") ||
            dir.getPath().equals("/import/async/cad/cvs")) {
            System.out.println(dir + " IGNORED");
            return;
        }
		File[] files = dir.listFiles();
		if (files == null)
		{
			System.out.println(dir + " ACCESS DENIED");
			return;
		}
        boolean isDelib = dir.getName().endsWith(".delib");
        String libName = null;
        if (isDelib) {
			libName = dir.getName();
			int extPos = libName.lastIndexOf('.');
			if (extPos >= 0)
    			libName = libName.substring(0, extPos);
        }
		boolean libFound = false;
		for (int i = 0; i < files.length; i++)
		{
            if (files[i].isDirectory()) continue;
			String name = files[i].getName();
			if (name.startsWith("._")) continue;
			int extensionPos = name.lastIndexOf('.');
			if (extensionPos < 0) continue;
			String extension = name.substring(extensionPos);
			name = name.substring(0, extensionPos);

			if (extension.equals(".elib") || extension.equals(".jelib") ||
                    isDelib && !extension.equals(".bak") && !extension.equals(".log") ||
                    parentLibName != null)
			{
				if (!libFound)
				{
					System.out.println(dir.toString());
					libFound = true;
				}
				String strippedName = isDelib ? libName + ":" + name : parentLibName != null ? parentLibName + ":" + name : stripBackup(name);
				try
				{
					FileInstance f = new FileInstance(this, files[i].toString());
					Set<FileInstance> libFiles = preLibraries.get(strippedName);
					if (libFiles == null)
					{
						libFiles = new TreeSet<FileInstance>();
						preLibraries.put(strippedName, libFiles);
					}
					libFiles.add(f);
				} catch (IOException e)
				{
					System.out.println(files[i] + " FAILED " + e);
				}
			}
		}
		for (int i = 0; i < files.length; i++)
		{
			if (!files[i].isDirectory()) continue;
            if (files[i].getName().equals("CVS")) continue;
			scanDir(files[i], canonicalDirs, preLibraries, isDelib ? libName : null);
		}
	}

	public void readHeaders(ErrorLogger errorLogger)
	{
		for (Iterator<LibraryName> lit = getLibraryNames(); lit.hasNext(); )
		{
			LibraryName libraryName = lit.next();
			for (Iterator<FileContents> it = libraryName.getVersions(); it.hasNext(); )
			{
				FileContents fc = it.next();
				if (!fc.isElib()) continue;
                for (Iterator<URL> uit = fc.fileUrl(); uit.hasNext(); ) {
                    URL fileUrl = uit.next();
    				if (!ELIB.readStatistics(fileUrl, errorLogger, fc))
                        break;
                }
        		if (fc.header == null) {
					System.out.println(fc.fileUrl().next() + " INVALID HEADER");
					continue;
				}
			}
		}
	}

	public void readJelibVersions(ErrorLogger errorLogger)
	{
		for (Iterator<LibraryName> lit = getLibraryNames(); lit.hasNext(); )
		{
			LibraryName libraryName = lit.next();
			for (Iterator<FileContents> it = libraryName.getVersions(); it.hasNext(); )
			{
				FileContents fc = it.next();
				if (fc.isElib()) continue;
                for (Iterator<URL> uit = fc.fileUrl(); uit.hasNext(); ) {
                    URL fileUrl = uit.next();
                    try {
                        JelibParser parser = JelibParser.parse(libraryName.getLibId(), fileUrl, FileType.JELIB, false, errorLogger);
                        fc.version = parser.version;
                        TreeMap<String,ExternalCell> externalCells = new TreeMap<String,ExternalCell>();
                        for (JelibParser.CellContents cc: parser.allCells.values()) {
                            fc.localCells.add(cc.cellId.cellName.toString());
                            for (JelibParser.NodeContents nc: cc.nodes) {
                                if (!(nc.protoId instanceof CellId)) continue;
                                CellId cellId = (CellId)nc.protoId;
                                if (externalCells.containsKey(cellId.toString())) continue;
                                String libPath = parser.externalLibIds.get(cellId.libId);
                                ExternalCell ec = new ExternalCell(libPath, cellId.libId.libName, cellId.cellName.toString());
                                externalCells.put(cellId.toString(), ec);
                            }
                        }
                        fc.externalCells.addAll(externalCells.values());
                        break;
                    } catch (Exception e) {
                        System.out.println("Error reading " + fileUrl + " " + e.getMessage());
                    }
                }
			}
		}
	}

    public static void parseLibraries(ErrorLogger errorLogger, File[] dirs) {
        for (File dir: dirs) {
            parseLibrariesInProject(errorLogger, dir);
        }

    }

    private static void parseLibrariesInProject(ErrorLogger errorLogger, File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            System.out.println(dir + " ACCESS DENIED");
            return;
        }
//        System.out.println("Dir " + dir);
        for (File file: files) {
            String name = file.getName();
            if (name.endsWith(".jelib")) {
                parseJelib(errorLogger, file, FileType.JELIB);
            } else if (name.endsWith(".delib")) {
                parseJelib(errorLogger, file, FileType.DELIB);
            }
        }
    }

    private static void parseJelib(ErrorLogger errorLogger, File file, FileType fileType) {
        try {
            System.out.println("Parsing " + file);
            IdManager idManager = new IdManager();
            String libName = file.getName();
			int extPos = libName.lastIndexOf('.');
			if (extPos >= 0)
    			libName = libName.substring(0, extPos);
            LibId libId = idManager.newLibId(LibId.legalLibraryName(libName));
            URL fileUrl = file.toURI().toURL();

            JelibParser parser = JelibParser.parse(libId, fileUrl, fileType, false, errorLogger);
        } catch (Exception e) {
            System.out.println("Error reading " + file + " " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void checkLibraries(ErrorLogger errorLogger, File[] dirs) {
        for (File dir: dirs) {
            checkLibrariesInProject(errorLogger, dir);
        }
    }

    private static void checkLibrariesInProject(ErrorLogger errorLogger, File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            System.out.println(dir + " ACCESS DENIED");
            return;
        }
//        System.out.println("Dir " + dir);
        for (File file: files) {
            String name = file.getName();
            if (name.endsWith(".jelib")) {
                checkJelib(errorLogger, file, FileType.JELIB);
            } else if (name.endsWith(".delib")) {
                checkJelib(errorLogger, file, FileType.DELIB);
            }
        }
    }

    private static void checkJelib(ErrorLogger errorLogger, File file, FileType fileType) {
        try {
 //           System.out.println("Checking " + file);
            IdManager idManager = new IdManager();
            String libName = file.getName();
			int extPos = libName.lastIndexOf('.');
			if (extPos >= 0)
    			libName = libName.substring(0, extPos);
            LibId libId = idManager.newLibId(LibId.legalLibraryName(libName));
            URL fileUrl = file.toURI().toURL();

            JelibParser parser = JelibParser.parse(libId, fileUrl, fileType, false, errorLogger);

            checkVars(parser.libVars, file);
            for (JelibParser.CellContents cc: parser.allCells.values()) {
                checkVars(cc.vars, file);
                for (JelibParser.NodeContents nc: cc.nodes)
                    checkVars(nc.vars, file);
                for (JelibParser.ArcContents ac: cc.arcs)
                    checkVars(ac.vars, file);
                for (JelibParser.ExportContents ec: cc.exports)
                    checkVars(ec.vars, file);
            }

            TreeMap<CellName,ArrayList<JelibParser.CellContents>> groups = new TreeMap<CellName,ArrayList<JelibParser.CellContents>>();
            for (JelibParser.CellContents cc: parser.allCells.values()) {
                ArrayList<JelibParser.CellContents> group = groups.get(cc.groupName);
                if (group == null) {
                    group = new ArrayList<JelibParser.CellContents>();
                    groups.put(cc.groupName, group);
                }
                group.add(cc);
            }
            for (Map.Entry<CellName,ArrayList<JelibParser.CellContents>> e: groups.entrySet()) {
                CellName groupName = e.getKey();
                ArrayList<JelibParser.CellContents> cells = e.getValue();

                int numParameterizedCells = 0;
                for (JelibParser.CellContents cc: cells) {
                    Variable[] params = getParams(cc);
                    if (params.length > 0)
                        numParameterizedCells++;
                }
                if (numParameterizedCells <= 1) continue;

                System.out.println("Checking " + file);
                System.out.println("***** Group " + libId + ":" + groupName + " has params");
                for (JelibParser.CellContents cc: cells) {
                    Variable[] params = getParams(cc);
                    if (params.length == 0) continue;
                    CellName cellName = cc.cellId.cellName;
                    System.out.print("    " + cellName);
                    for (Variable var: params) {
                        System.out.print(" " + var + "(" +
                                com.sun.electric.tool.io.output.JELIB.describeDescriptor(var, var.getTextDescriptor(), true) +
                                ")" + var.getObject());
                    }
                    System.out.println();
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading " + file + " " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void checkVars(Variable[] vars, File file) {
        for (Variable var: vars) {
            if (var.isCode() && !(var.getObject() instanceof String)) {
                System.out.println("$$$$$ Variable " + var.getPureValue(-1) + " in " + file);
            }
        }
    }

    private static Variable[] getParams(JelibParser.CellContents cc) {
        int count = 0;
        for (Variable var: cc.vars) {
            if (var.getTextDescriptor().isParam() && var.getKey() != NccCellAnnotations.NCC_ANNOTATION_KEY)
                count++;
        }
        if (count == 0) return Variable.NULL_ARRAY;
        Variable[] params = new Variable[count];
        count = 0;
        for (Variable var: cc.vars) {
            if (var.getTextDescriptor().isParam() && var.getKey() != NccCellAnnotations.NCC_ANNOTATION_KEY)
                params[count++] = var;
        }
        return params;
    }


//	public void readLibraries()
//	{
//		totalLibraryContents = new LibraryContents("noname", new JELIB1());
//
//		for (Iterator lit = getLibraryNames(); lit.hasNext(); )
//		{
//			LibraryName libraryName = (LibraryName)lit.next();
////			System.out.println(libraryName.getName());
//			for (Iterator it = libraryName.getVersions(); it.hasNext(); )
//			{
//				FileContents fc = (FileContents)it.next();
//				if (!fc.isElib()) continue;
//				String fileName = fc.fileName();
//				URL fileURL = TextUtils.makeURLToFile(fileName);
//				ELIB1.readLibraryStat(fc, this);
//				if (fc.header == null)
//				{
//					System.out.println(fileName + " INVALID HEADER");
//					continue;
//				}
//			}
//		}
//		for (Iterator it = totalLibraryContents.variableKeyRefs.values().iterator(); it.hasNext(); )
//		{
//			String v = ((LibraryContents.VariableKeyRef)it.next()).getName();
//			String s = (String)varStat.varNamePool.get(v);
//			if (s == null)
//			{
//				varStat.varNamePool.put(v, v);
//			}
//		}
//	}

	public void writeList(String fileName)
	{
		try
		{
			new StatisticsOutput(fileName);
		} catch (IOException e)
		{
			System.out.println("Error storing LibraryStatistics to " + fileName + " " + e);
		}
	}

	public static LibraryStatistics readList(IdManager idManager, String fileName)
	{
		URL fileURL = TextUtils.makeURLToFile(fileName);
		try
		{
			StatisticsInput in = new StatisticsInput(idManager, fileURL);
			return in.stat;
		} catch (IOException e)
		{
			System.out.println("Error loading LibraryStatistics from " + fileName + " " + e);
		}
		return null;
	}

	public void writeSerialized(String fileName)
	{
		try
		{
			new StatisticsOutputSerialized(fileName);
		} catch (IOException e)
		{
			System.out.println("Error storing LibraryStatistics to " + fileName + " " + e);
		}
	}

	public static LibraryStatistics readSerialized(String fileName)
	{
		URL fileURL = TextUtils.makeURLToFile(fileName);
		try
		{
			StatisticsInputSerialized in = new StatisticsInputSerialized(fileURL);
			return in.stat;
		} catch (IOException e)
		{
			System.out.println("Error loading LibraryStatistics from " + fileName + " " + e);
		}
		return null;
	}

	public void reportFileLength()
	{
		int elibUniqueCount = 0;
		int jelibUniqueCount = 0;
		int elibCount = 0;
		int jelibCount = 0;
		long elibUniqueLength = 0;
		long jelibUniqueLength = 0;
		long elibLength = 0;
		long jelibLength = 0;

		TreeMap<ELIB.Header,GenMath.MutableInteger> headerCounts = new TreeMap<ELIB.Header,GenMath.MutableInteger>();
		int withoutHeader = 0;
		TreeMap<Version,GenMath.MutableInteger> versionCounts = new TreeMap<Version,GenMath.MutableInteger>();
        int withoutVersion = 0;

		for (Iterator lit = getLibraryNames(); lit.hasNext(); )
		{
			LibraryName libraryName = (LibraryName)lit.next();
            libraryName.getLibId();
			for (Iterator it = libraryName.getVersions(); it.hasNext(); )
			{
				FileContents fc = (FileContents)it.next();
				if (fc.isElib())
				{
					elibUniqueCount++;
					elibCount += fc.instances.size();
					elibUniqueLength += fc.fileLength;
					elibLength += fc.fileLength * fc.instances.size();
					if (fc.header != null)
						GenMath.addToBag(headerCounts, fc.header);
					else
						withoutHeader++;
				} else
				{
					jelibUniqueCount++;
					jelibCount += fc.instances.size();
					jelibUniqueLength += fc.fileLength;
					jelibLength += fc.fileLength * fc.instances.size();
				}
                if (fc.version != null)
                    GenMath.addToBag(versionCounts, fc.version);
                else
                    withoutVersion++;
                System.out.println(fc.getFileName() + " " + fc.version);
			}
		}
		System.out.println("Scanned " + directories.size() + " directories. " + libraryNames.size() + " library names");
		System.out.println((elibUniqueLength>>20) + "M (" + elibUniqueLength + ") in " +
						   elibUniqueCount + " ELIB files ( unique )");
		System.out.println((elibLength>>20) + "M (" + elibLength + ") in " +
						   elibCount + " ELIB files ( with duplicates )");
		System.out.println("NOHEADER:" + withoutHeader + bagReport(headerCounts));

		System.out.println((jelibUniqueLength>>20) + "M (" + jelibUniqueLength + ") in " +
						   jelibUniqueCount + " JELIB files ( unique )");
		System.out.println((jelibLength>>20) + "M (" + jelibLength + ") in " +
						   jelibCount + " JELIB files ( with duplicates )");
		System.out.println("NOVERSION:" + withoutVersion + bagReport(versionCounts));
	}

	public void reportFilePaths() {
        TreeMap<String,GenMath.MutableInteger> paths = new TreeMap<String,GenMath.MutableInteger>();
        System.out.println(directories.size() + " Directories");
		for (Iterator<Directory> it = getDirectories(); it.hasNext(); ) {
            Directory directory = it.next();
//            System.out.print(directory.dirName);
            String projName = directory.dirName;

            if (projName.startsWith("/import/async/cad/tools/electric/builds/svn")) continue;
            if (projName.startsWith("/import/async/archive/2005/tic/gilda/TreasureIsland/electric-old/projectManagement")) continue;
            if (projName.startsWith("/import/async/archive/2005/tic/jkg/projectTest")) continue;
            if (projName.startsWith("/import/async/cad/cvs")) continue;

            int delibPos = projName.indexOf(".delib");
            if (delibPos >= 0) {
                int slashPos = projName.lastIndexOf('/', delibPos);
                projName = projName.substring(0, slashPos);
//                System.out.print(" -> " + projName);
            }
            GenMath.addToBag(paths, projName);
//            System.out.println();
		}
       System.out.println(paths.size() + " Projects");
       for (Map.Entry<String,GenMath.MutableInteger> e: paths.entrySet()) {
           System.out.println(e.getKey());
//           System.out.println(e.getKey() + " " + e.getValue());
       }
	}

	public void reportCells() {
		for (Iterator lit = getLibraryNames(); lit.hasNext(); )
		{
			LibraryName libraryName = (LibraryName)lit.next();

            System.out.println("LibraryName " + libraryName.getLibId() + " " + libraryName.getName());
			for (Iterator it = libraryName.getVersions(); it.hasNext(); )
			{
				FileContents fc = (FileContents)it.next();
                System.out.println("    " + fc.getFileName() + " " + fc.fileLength + " " + fc.localCells.size() + " " + fc.externalCells.size());
                for (String localName: fc.localCells)
                    System.out.println("\t" + localName);
                for (ExternalCell extCell: fc.externalCells)
                    System.out.println("\t" + extCell.libPath + " " + extCell.libName + " " + extCell.cellName);
			}
		}
    }
//	public void reportMemoryUsage()
//	{
//		int elibCount = 0;
//		int elibWithHeader = 0;
//		int elibOk = 0;
//
//		int toolCount = 0;
//		int techCount = 0;
//		int primNodeProtoCount = 0;
//		int primPortProtoCount = 0;
//		int arcProtoCount = 0;
//		int nodeProtoCount = 0;
//		int nodeInstCount = 0;
//		int portProtoCount = 0;
//		int arcInstCount = 0;
//		int geomCount = 0;
//		int cellCount = 0;
//		int userBits = 0;
//		int viewCount = 0;
//		long nameLength = 0;
//
//		int varNameCount = 0;
//		long varNameLength = 0;
//
//		long bytesRead = 0;
//		long fileLength = 0;
//
//		for (Iterator lit = getLibraryNames(); lit.hasNext(); )
//		{
//			LibraryName libraryName = (LibraryName)lit.next();
//			for (Iterator it = libraryName.getVersions(); it.hasNext(); )
//			{
//				FileContents fc = (FileContents)it.next();
//				if (!fc.isElib()) continue;
//				elibCount++;
//				if (fc.header == null) continue;
//				elibWithHeader++;
//				if (!fc.readOk) continue;
//				elibOk++;
//				toolCount += fc.toolCount;
//				techCount += fc.techCount;
//				primNodeProtoCount += fc.primNodeProtoCount;
//				primPortProtoCount += fc.primPortProtoCount;
//				arcProtoCount += fc.arcProtoCount;
//				nodeProtoCount += fc.nodeProtoCount;
//				nodeInstCount += fc.nodeInstCount;
//				portProtoCount += fc.portProtoCount;
//				arcInstCount += fc.arcInstCount;
//				geomCount += fc.geomCount;
//				cellCount += fc.cellCount;
//				userBits |= fc.userBits;
//				viewCount += fc.viewCount;
//				nameLength += fc.nameLength;
//				varNameCount += fc.varNameCount;
//				varNameLength += fc.varNameLength;
//				bytesRead += fc.bytesRead;
//				fileLength += fc.fileLength;
//			}
//		}
//		System.out.println("elibCount=" + elibCount);
//		System.out.println("elibWithHeader=" + elibWithHeader);
//		System.out.println("elibOk=" + elibOk);
//
//		System.out.println("toolCount=" + toolCount);
//		System.out.println("techCount=" + techCount);
//		System.out.println("primNodeProtoCount=" + primNodeProtoCount);
//		System.out.println("primPortProtoCount=" + primPortProtoCount);
//		System.out.println("arcProtoCount=" + arcProtoCount);
//		System.out.println("nodeProtoCount=" + nodeProtoCount);
//		System.out.println("nodeInstCount=" + nodeInstCount);
//		System.out.println("portProtoCount=" + portProtoCount);
//		System.out.println("arcInstCount=" + arcInstCount);
//		System.out.println("geomCount=" + geomCount);
//		System.out.println("cellCount=" + cellCount);
//		System.out.println("userBits=" + userBits);
//		System.out.println("viewCount=" + viewCount);
//		System.out.println("nameLength=" + nameLength);
//
//		System.out.println("varNameCount=" + varNameCount);
//		System.out.println("varNameLength=" + varNameLength);
//
//		System.out.println("bytesRead=" + bytesRead);
//		System.out.println("fileLength=" + fileLength);
//	}
//
//	public void reportJelib(String fileName)
//	{
//		if (totalLibraryContents == null) return;
//		try
//		{
//			new StatisticsOutputJelib(fileName);
//		} catch (IOException e)
//		{
//			System.out.println("Error storing LibraryStatisticsJelib to " + fileName + " " + e);
//		}
//	}
//
//	public void reportVariableNames(String fileName)
//	{
//		if (totalLibraryContents == null) return;
//		try
//		{
//			new StatisticsOutputVariableNames(fileName);
//		} catch (IOException e)
//		{
//			System.out.println("Error storing LibraryStatisticsVariableNames to " + fileName + " " + e);
//		}
//	}

	public static VarStat readVariableNames(String fileName)
	{
		URL fileURL = TextUtils.makeURLToFile(fileName);
		try
		{
			StatisticsInputVariableNames in = new StatisticsInputVariableNames(fileURL);
			long totalVars = 0;
			long[] typeCounts = new long[32];
			long[] typeTotals = new long[32];
			TreeMap charCount = new TreeMap();
			TreeMap charTotal = new TreeMap();
			TreeMap bitsCount = new TreeMap();
			TreeMap bitsTotal = new TreeMap();
			for (Iterator it = in.vs.varBag.values().iterator(); it.hasNext(); )
			{
				VarDesc vd = (VarDesc)it.next();
				Character character = new Character(vd.role.charAt(0));
				GenMath.addToBag(charCount, character);
				GenMath.addToBag(charTotal, character, vd.count);
				VarDesc vd1 = new VarDesc();
				vd1.role = "";
				vd1.varName = "";
				if (Character.isUpperCase(vd.role.charAt(0)))
					vd1.varBits = vd.varBits & ~ELIBConstants.VLENGTH;
				else
					vd1.varBits = 0104;
				vd1.td0 = vd.td0;
				vd1.td1 = vd.td1;
				GenMath.addToBag(bitsCount, vd1);
				GenMath.addToBag(bitsTotal, vd1, vd.count);
				totalVars += vd.count;
				typeCounts[vd.varBits&0x1F] ++;
				typeTotals[vd.varBits&0x1F] += vd.count;
				if ((vd.varBits&0x1F) == 7) System.out.println("VNODEINST" + " " + vd.role + " " + vd.varName);
				if ((vd.varBits&0x1F) == 8) System.out.println("VNODEPROTO" + " " + vd.role + " " + vd.varName);
				if ((vd.varBits&(ELIBConstants.VCODE1|ELIBConstants.VCODE2)) != 0)
					System.out.println(Integer.toOctalString(vd.varBits) + " " + vd.role + " " + vd.varName);
			}
			System.out.println(in.vs.varBag.size() + " bag: " + bagReport(charCount));
			System.out.println(totalVars +  "bagTotal: " + bagReport(charTotal));
			for (int i = 0; i < 32; i++)
			{
				System.out.println(Integer.toOctalString(i) + " " + typeCounts[i] + " "  + typeTotals[i]);
			}
			for (Iterator it = bitsCount.entrySet().iterator(); it.hasNext(); )
			{
				Map.Entry entry = (Map.Entry)it.next();
				VarDesc vd = (VarDesc)entry.getKey();
				System.out.println(vd.role + " " + Integer.toOctalString(vd.varBits) +
								   " " + Integer.toOctalString(vd.td0) + " " + Integer.toOctalString(vd.td1) +
								   " " + GenMath.countInBag(bitsCount, vd) + " "  + GenMath.countInBag(bitsTotal, vd));
			}
			System.out.println(bitsCount.size() + " variable descriptors");
			return in.vs;
		} catch (IOException e)
		{
			System.out.println("Error loading LibraryStatistics from " + fileName + " " + e);
		}
		return null;
	}

	private static String stripBackup(String libName)
	{
		int i = libName.length();
		while (i > 0 && Character.isDigit(libName.charAt(i - 1))) i--;
		if (i == libName.length()) return libName;
		if (i > 3 && libName.charAt(i - 1) == '-' && libName.charAt(i - 2) == '-' &&
			Character.isDigit(libName.charAt(i - 3)))
		{
			i -= 3;
			while (i > 0 && Character.isDigit(libName.charAt(i - 1))) i--;
		}
		if (i < 2 || libName.charAt(i - 1) != '-' ||
			!Character.isDigit(libName.charAt(i - 2))) return libName;
		i -= 2;
		while (i > 0 && Character.isDigit(libName.charAt(i - 1))) i--;
		if (i < 2 || libName.charAt(i - 1) != '-' ||
			!Character.isDigit(libName.charAt(i - 2))) return libName;
		i -= 2;
		while (i > 0 && Character.isDigit(libName.charAt(i - 1))) i--;
		if (i < 1 || libName.charAt(i - 1) != '-') return libName;
		return libName.substring(0, i - 1);
	}

	static <T> String bagReport(Map<T,GenMath.MutableInteger> bag)
	{
		String s = "";
		for (Iterator<Map.Entry<T,GenMath.MutableInteger>> it = bag.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<T,GenMath.MutableInteger> e = it.next();
			GenMath.MutableInteger count = e.getValue();
			s += " " + e.getKey() + ":" + count.intValue();
		}
		return s;
	}

	/**** Internal Classes */

	private static class Directory implements Serializable
	{
		private static final long serialVersionUID = -8627329891776990655L;
		private String dirName;
		private TreeMap<String,FileInstance> files = new TreeMap<String,FileInstance>();

		Directory(LibraryStatistics stat, String dirName)
		{
			this.dirName = dirName;
			stat.directories.put(dirName, this);
		}

		String getName() { return dirName; }

		Iterator<FileInstance> getFiles() { return files.values().iterator(); }
	}

	private static class LibraryName implements Serializable
	{
        final LibraryStatistics stat;
		final String name;
		final List<FileContents> versions = new ArrayList<FileContents>();
		TreeMap<String,LibraryUse> references;

		LibraryName(LibraryStatistics stat, String name)
		{
            this.stat = stat;
			this.name = name;
			stat.libraryNames.put(name, this);
		}

        LibId getLibId() {
            String libName = name;
            int indexOfColon = libName.indexOf(':');
            if (indexOfColon >= 0)
                libName = libName.substring(0, indexOfColon);
            return stat.idManager.newLibId(LibId.legalLibraryName(libName));
        }

		String getName() { return name; }

		Iterator<FileContents> getVersions() { return versions.iterator(); }
	}

	private static class LibraryUse implements Serializable
	{
		Directory dir;
		LibraryName libName;
		String fullName;
		FileContents from;
	}

    static class ExternalCell implements Serializable {
        final String libPath;
        final String libName;
        final String cellName;

        ExternalCell(String libPath, String libName, String cellName) {
            this.libPath = libPath;
            this.libName = libName;
            this.cellName = cellName;
        }
    }

	static class FileContents implements Serializable
	{
        private static final long serialVersionUID = 8673043477742718970L;

		LibraryName libraryName;
		long fileLength;
		long crc;
		long lastModified;

		List<FileInstance> instances = new ArrayList<FileInstance>();
		TreeMap<String,LibraryUse> uses = new TreeMap<String,LibraryUse>();

		ELIB.Header header;
        Version version;
        List<String> localCells = new ArrayList<String>();
        List<ExternalCell> externalCells = new ArrayList<ExternalCell>();

		boolean readOk;
		int toolCount;
		int techCount;
		int primNodeProtoCount;
		int primPortProtoCount;
		int arcProtoCount;
		int nodeProtoCount;
		int nodeInstCount;
		int portProtoCount;
		int arcInstCount;
		int geomCount;
		int cellCount;
		int userBits;
		int viewCount;
		int nameLength;

		int varNameCount;
		int varNameLength;

		int bytesRead;

		private FileContents(LibraryName libraryName, FileInstance f)
		{
			this.libraryName = libraryName;
			libraryName.versions.add(this);

			fileLength = f.fileLength;
			crc = f.crc;
			lastModified = f.lastModified;
			f.contents = this;
			instances.add(f);
		}

		void add(FileInstance f)
		{
			assert f.fileLength == fileLength && f.crc == crc;
			f.contents = this;
			instances.add(f);
			if (f.lastModified < lastModified) lastModified = f.lastModified;
		}

		Iterator<URL> fileUrl() {
            ArrayList<URL> urls = new ArrayList<URL>();
            for (FileInstance instance: instances)
                urls.add(instance.getUrl());
            return urls.iterator();
        }

        IdManager idManager() { return libraryName.stat.idManager; }

		boolean isElib() { return instances.get(0).fileName.endsWith(".elib"); }

        String getFileName() { return instances.get(0).fileName; }
	}

	private static class FileInstance implements Comparable, Serializable
	{
		private static final long serialVersionUID = -5726569346410497467L;
		private FileContents contents;
		private String fileName;
        private final File file;
		private long fileLength;
		private long crc;
		private long lastModified;
		transient String canonicalPath;

		private FileInstance(LibraryStatistics stat, String fileName, long fileLength, long lastModified, long crc)
            throws IOException
        {
			file = new File(fileName);
			this.fileName = fileName;
			this.fileLength = fileLength;
			this.lastModified = lastModified;
			this.crc = crc;

//			File file = new File(fileName);
			stat.getDirectory(file.getParent()).files.put(file.getName(), this);
		}

		FileInstance(LibraryStatistics stat, String fileName)
			throws IOException
		{
			file = new File(fileName);
			this.fileName = fileName;
			canonicalPath = file.getCanonicalPath();
			fileLength = file.length();
			lastModified = file.lastModified();
			Input in = new Input();
			try
			{
				if (in.openBinaryInput(getUrl()))
                    throw new IOException("openBytesInput");
				CheckedInputStream checkedInputStream = new CheckedInputStream(in.dataInputStream, new CRC32());
				if (checkedInputStream.skip(fileLength) != fileLength)
					throw new IOException("skip failed");
				crc = checkedInputStream.getChecksum().getValue();
			} finally
			{
				in.closeInput();
			}

			stat.getDirectory(file.getParent()).files.put(file.getName(), this);
		}

        private URL getUrl() {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                return null;
            }
        }

		public int compareTo(Object o)
		{
			FileInstance f = (FileInstance)o;
			if (lastModified > f.lastModified) return 1;
			if (lastModified < f.lastModified) return -1;
			return fileName.compareTo(f.fileName);
		}
	}

	private static class StatisticsInput extends Input
	{
		private LibraryStatistics stat;

		StatisticsInput(IdManager idManager, URL url)
			throws IOException
		{
			if (openTextInput(url)) throw new IOException("openStatisticsInput");
			try
			{
				stat = new LibraryStatistics(idManager);
				LibraryName libraryName = null;
				long fileLength = 0;
				long crc = 0;
				FileContents fc = null;

				for (;;)
				{
					String line = lineReader.readLine();
					if (line == null) break;
					if (line.length() == 0) continue;
					if (line.charAt(0) != ' ')
					{
						libraryName = stat.getLibraryName(line);
						continue;
					}
					if (line.startsWith("        "))
					{
						String fileName;
						String timeString;
						int indexElib = line.lastIndexOf(".elib");
						int indexJelib = line.lastIndexOf(".jelib");
						int indexDelib = line.lastIndexOf(".delib");
						if (indexElib >= 0)
						{
							fileName = line.substring(8, indexElib + 5);
							timeString = line.substring(indexElib + 5);
						} else if (indexJelib >= 0)
						{
							fileName = line.substring(8, indexJelib + 6);
							timeString = line.substring(indexJelib + 6);
						} else if (indexDelib >= 0)
						{
                            while (indexDelib < line.length() && line.charAt(indexDelib) != ' ')
                                indexDelib++;
							fileName = line.substring(8, indexDelib);
							timeString = line.substring(indexDelib);
						} else
						{
							throw new IOException("Library extension: " + line);
						}
						String[] pieces = timeString.split(" +");
						long lastModified;
						try
						{
							lastModified = Long.parseLong(pieces[1]);
						} catch (NumberFormatException e)
						{
							throw new IOException("lastModified:" + pieces[1]);
						}
						FileInstance f = new FileInstance(stat, fileName, fileLength, lastModified, crc);
						if (fc == null)
							fc = new FileContents(libraryName, f);
						else
							fc.add(f);
					} else if (line.startsWith("    "))
					{
						String[] pieces = line.split(" +");
						try {
							fileLength = Long.parseLong(pieces[1]);
						} catch (NumberFormatException e)
						{
							throw new IOException("fileLength: " + pieces[1]);
						}
						try {
							crc = Long.parseLong(pieces[2], 16);
						} catch (NumberFormatException e)
						{
							throw new IOException("crc: " + pieces[2]);
						}
						fc = null;
					} else
					{
						throw new IOException("bad line:" + line);
					}
				}
			} finally
			{
				closeInput();
			}
		}
	}

	private class StatisticsOutput extends Output
	{
		StatisticsOutput(String filePath)
			throws IOException
		{
			if (openTextOutputStream(filePath)) throw new IOException("openStatisticsOutput");
			try
			{
				for (Iterator lit = getLibraryNames(); lit.hasNext(); )
				{
					LibraryName libraryName = (LibraryName)lit.next();
					printWriter.println(libraryName.getName());
					for (Iterator it = libraryName.getVersions(); it.hasNext(); )
					{
						FileContents fc = (FileContents)it.next();
						Date date = new Date(fc.lastModified);
						printWriter.println("    " + fc.fileLength + " " + Long.toHexString(fc.crc) +
											" " + TextUtils.formatDateGMT(date));
						for (Iterator fit = fc.instances.iterator(); fit.hasNext(); )
						{
							FileInstance f = (FileInstance)fit.next();
							date = new Date(f.lastModified);
							printWriter.println("        " + f.fileName + " " + f.lastModified +
												" " + TextUtils.formatDateGMT(date));
						}
					}
				}
			} finally {
				closeTextOutputStream();
			}
		}
	}

	private static class StatisticsInputSerialized extends Input
	{
		private LibraryStatistics stat;

		StatisticsInputSerialized(URL url)
			throws IOException
		{
			if (openBinaryInput(url)) throw new IOException("openStatisticsInputSerialized");
			try
			{
				ObjectInputStream objectInputStream = new ObjectInputStream(dataInputStream);
				try
				{
					stat = (LibraryStatistics)objectInputStream.readObject();
				} catch (ClassNotFoundException e)
				{
				}
				objectInputStream.close();
			} finally
			{
				closeInput();
			}
		}
	}

	private class StatisticsOutputSerialized extends Output
	{
		StatisticsOutputSerialized(String filePath)
			throws IOException
		{
			if (openBinaryOutputStream(filePath)) throw new IOException("openStatisticsOutputSerialized");
			try
			{
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(dataOutputStream);
				objectOutputStream.writeObject(LibraryStatistics.this);
				objectOutputStream.close();
			} finally {
				closeBinaryOutputStream();
			}
		}
	}

//	private class StatisticsOutputJelib extends Output
//	{
//		StatisticsOutputJelib(String filePath)
//			throws IOException
//		{
//			if (openTextOutputStream(filePath)) throw new IOException("openStatisticsOutputSerialized");
//			try
//			{
//				totalLibraryContents.printJelib(printWriter);
//			} finally {
//				closeTextOutputStream();
//			}
//		}
//	}

	static class VarDesc implements Serializable, Comparable
	{
		String role;
		String varName;
		int varBits;
		int td0, td1;
		int count;

		public int compareTo(Object o)
		{
			VarDesc v = (VarDesc)o;
			int cmp = role.compareTo(v.role);
			if (cmp != 0) return cmp;
			cmp = varName.compareTo(v.varName);
			if (cmp != 0) return cmp;
			if (varBits > v.varBits) return 1;
			if (varBits < v.varBits) return -1;
			if (td0 > v.td0) return 1;
			if (td0 < v.td0) return -1;
			if (td1 > v.td1) return 1;
			if (td1 < v.td1) return -1;
			return 0;
		}
	}

	static class UserBits implements Serializable, Comparable
	{
		String role;
		int bits;
		int count;

		public int compareTo(Object o)
		{
			UserBits u = (UserBits)o;
			int cmp = role.compareTo(u.role);
			if (cmp != 0) return cmp;
			if (bits > u.bits) return 1;
			if (bits < u.bits) return -1;
			return 0;
		}
	}

	static class VarStat implements Serializable
	{
		private static final long serialVersionUID = -2536836777200853733L;
		TreeMap varNamePool = new TreeMap();
		TreeMap varBag = new TreeMap();
		TreeMap userBitsBag = new TreeMap();
		transient TreeMap otherStrings = new TreeMap();
		transient VarDesc dummyVarDesc = new VarDesc();
		transient UserBits dummyUserBits = new UserBits();

		String getVarName(String name)
		{
			String v = (String)varNamePool.get(name);
			if (v == null)
			{
				v = name;
				varNamePool.put(v, v);
			}
			return v;
		}

		void addVarDesc(String varName, int varBits, int td0, int td1, String role)
		{
			td1 &= ~017700000; // hide face
			dummyVarDesc.varName = varName;
			dummyVarDesc.varBits = varBits;
			dummyVarDesc.td0 = td0;
			dummyVarDesc.td1 = td1;
			dummyVarDesc.role = role;
			VarDesc v = (VarDesc)varBag.get(dummyVarDesc);
			if (v == null)
			{
				v = new VarDesc();
				v.varName = getVarName(varName);
				v.varBits = varBits;
				v.td0 = td0;
				v.td1 = td1;
				v.role = (String)otherStrings.get(role);
				if (v.role == null)
				{
					v.role = role;
					otherStrings.put(role, role);
				}
				varBag.put(v, v);
			}
			v.count++;
		}

		void addUserBits(int userBits, String role)
		{
			dummyUserBits.bits = userBits;
			dummyUserBits.role = role;
			UserBits u = (UserBits)userBitsBag.get(dummyUserBits);
			if (u == null)
			{
				u = new UserBits();
				u.bits = userBits;
				u.role = (String)otherStrings.get(role);
				if (u.role == null)
				{
					u.role = role;
					otherStrings.put(role, role);
				}
				userBitsBag.put(u, u);
			}
			u.count++;
		}

	}

	transient VarStat varStat = new VarStat();

	private static class StatisticsInputVariableNames extends Input
	{
		private VarStat vs;

		StatisticsInputVariableNames(URL url)
			throws IOException
		{
			if (openBinaryInput(url)) throw new IOException("openStatisticsInputVariableNames");
			try
			{
				ObjectInputStream objectInputStream = new ObjectInputStream(dataInputStream);
				try
				{
					vs = (VarStat)objectInputStream.readObject();
				} catch (ClassNotFoundException e)
				{
				}
				objectInputStream.close();
			} finally
			{
				closeInput();
			}
		}
	}

	private class StatisticsOutputVariableNames extends Output
	{
		StatisticsOutputVariableNames(String filePath)
			throws IOException
		{
			if (openBinaryOutputStream(filePath)) throw new IOException("openStatisticsOutputSerialized");
			try
			{
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(dataOutputStream);
				objectOutputStream.writeObject(varStat);
				objectOutputStream.close();
				int total = 0;
				for (Iterator it = varStat.varBag.values().iterator(); it.hasNext(); )
					total += ((VarDesc)it.next()).count;
				System.out.println(varStat.varBag.size() + " (" + total + ") variable descriptors");
			} finally {
				closeBinaryOutputStream();
			}
// 			if (openTextOutputStream(filePath)) throw new IOException("openStatisticsOutputVariableNames");
// 			try
// 			{
// 				totalLibraryContents.printJelibVariableNames(printWriter);
// 			} finally {
// 				closeTextOutputStream();
// 			}
		}
	}
}
