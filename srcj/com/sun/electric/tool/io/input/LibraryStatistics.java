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
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.io.ELIBConstants;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.io.output.Output;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
	private TreeMap/*<String,Directory>*/ directories = new TreeMap/*<String,Directory>*/();
	private TreeMap/*<String,LibraryName>*/ libraryNames = new TreeMap/*<String,LibraryName>*/();
	transient LibraryContents totalLibraryContents;

	private LibraryStatistics() {}

	Directory getDirectory(String dirName)
	{
		Directory dir = (Directory)directories.get(dirName);
		if (dir == null) dir = new Directory(this, dirName);
		return dir;
	}

	LibraryName getLibraryName(String name)
	{
		LibraryName libraryName = (LibraryName)libraryNames.get(name);
		if (libraryName == null) libraryName = new LibraryName(this, name);
		return libraryName;
	}

	Iterator/*<Directory>*/ getDirectories() { return directories.values().iterator(); }
	Iterator/*<LibraryName>*/ getLibraryNames() { return libraryNames.values().iterator(); }

	public static LibraryStatistics scanDirectories(String[] dirNames)
	{
		LibraryStatistics stat = new LibraryStatistics();
		Set/*<String>*/ canonicalDirs = new HashSet/*<String>*/();
		Map/*<String,Set<FileInstance>>*/ preLibraries = new HashMap/*<String,Set<FileInstance>>*/();

		for (int i = 0; i < dirNames.length; i++)
			stat.scanDir(new File(dirNames[i]), canonicalDirs, preLibraries);

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
							if (in.openBinaryInput(TextUtils.makeURLToFile(f1.fileName))) continue;
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
						if (in.openBinaryInput(TextUtils.makeURLToFile(f1.fileName))) continue;
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

	private void scanDir(File dir, Set/*<String>*/ canonicalDirs,
						 Map/*<String,TreeSet<FileInstance>>*/ preLibraries)
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
		File[] files = dir.listFiles();
		if (files == null)
		{
			System.out.println(dir + " ACCESS DENIED");
			return;
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
			
			if (extension.equals(".elib") || extension.equals(".jelib"))
			{
				if (!libFound)
				{
					System.out.println(dir.toString());
					libFound = true;
				}
				String strippedName = stripBackup(name);
				try
				{
					FileInstance f = new FileInstance(this, files[i].toString());
					TreeSet/*<FileInstance>*/ libFiles = (TreeSet/*<FileInstance>*/)preLibraries.get(strippedName);
					if (libFiles == null)
					{
						libFiles = new TreeSet/*<FileInstance>*/();
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
			scanDir(files[i], canonicalDirs, preLibraries);
		}
	}

	public void readHeaders()
	{
		for (Iterator lit = getLibraryNames(); lit.hasNext(); )
		{
			LibraryName libraryName = (LibraryName)lit.next();
			for (Iterator it = libraryName.getVersions(); it.hasNext(); )
			{
				FileContents fc = (FileContents)it.next();
				if (!fc.isElib()) continue;
				String fileName = fc.fileName();
				URL fileURL = TextUtils.makeURLToFile(fileName);
				fc.header = ELIB1.readLibraryHeader(fileURL);
				if (fc.header == null)
				{
					System.out.println(fileName + " INVALID HEADER");
					continue;
				}
			}
		}
	}

	public void readLibraries()
	{
		totalLibraryContents = new LibraryContents("noname", new JELIB1());

		for (Iterator lit = getLibraryNames(); lit.hasNext(); )
		{
			LibraryName libraryName = (LibraryName)lit.next();
//			System.out.println(libraryName.getName());
			for (Iterator it = libraryName.getVersions(); it.hasNext(); )
			{
				FileContents fc = (FileContents)it.next();
				if (!fc.isElib()) continue;
				String fileName = fc.fileName();
				URL fileURL = TextUtils.makeURLToFile(fileName);
				ELIB1.readLibraryStat(fc, this);
				if (fc.header == null)
				{
					System.out.println(fileName + " INVALID HEADER");
					continue;
				}
			}
		}
		for (Iterator it = totalLibraryContents.variableKeyRefs.values().iterator(); it.hasNext(); )
		{
			String v = ((LibraryContents.VariableKeyRef)it.next()).getName();
			String s = (String)varStat.varNamePool.get(v);
			if (s == null)
			{
				varStat.varNamePool.put(v, v);
			}
		}
	}

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

	public static LibraryStatistics readList(String fileName)
	{
		System.out.println(java.io.ObjectStreamClass.lookup(VarStat.class).getSerialVersionUID());
		URL fileURL = TextUtils.makeURLToFile(fileName);
		try
		{
			StatisticsInput in = new StatisticsInput(fileURL);
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

		TreeMap/*<ELIB1.Header,GenMath.MutableInteger>*/ headerCounts = new TreeMap();
		int withoutHeader = 0;

		for (Iterator lit = getLibraryNames(); lit.hasNext(); )
		{
			LibraryName libraryName = (LibraryName)lit.next();
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
			}
		}
		System.out.println("Scanned " + directories.size() + " directories. " + libraryNames.size() + " library names");
		System.out.println((elibUniqueLength>>20) + "M (" + elibUniqueLength + ") in " +
						   elibUniqueCount + " ELIB files ( unique )");
		System.out.println((elibLength>>20) + "M (" + elibLength + ") in " + 
						   elibCount + " ELIB files ( with duplicates )");
		System.out.println((jelibUniqueLength>>20) + "M (" + jelibUniqueLength + ") in " +
						   jelibUniqueCount + " JELIB files ( unique )");
		System.out.println((jelibLength>>20) + "M (" + jelibLength + ") in " +
						   jelibCount + " JELIB files ( with duplicates )");
		System.out.println("NOHEADER:" + withoutHeader + bagReport(headerCounts));
	}

	public void reportMemoryUsage()
	{
		int elibCount = 0;
		int elibWithHeader = 0;
		int elibOk = 0;

		int toolCount = 0;
		int techCount = 0;
		int primNodeProtoCount = 0;
		int primPortProtoCount = 0;
		int arcProtoCount = 0;
		int nodeProtoCount = 0;
		int nodeInstCount = 0;
		int portProtoCount = 0;
		int arcInstCount = 0;
		int geomCount = 0;
		int cellCount = 0;
		int userBits = 0;
		int viewCount = 0;
		long nameLength = 0;

		int varNameCount = 0;
		long varNameLength = 0;

		long bytesRead = 0;
		long fileLength = 0;
		
		for (Iterator lit = getLibraryNames(); lit.hasNext(); )
		{
			LibraryName libraryName = (LibraryName)lit.next();
			for (Iterator it = libraryName.getVersions(); it.hasNext(); )
			{
				FileContents fc = (FileContents)it.next();
				if (!fc.isElib()) continue;
				elibCount++;
				if (fc.header == null) continue;
				elibWithHeader++;
				if (!fc.readOk) continue;
				elibOk++;
				toolCount += fc.toolCount;
				techCount += fc.techCount;
				primNodeProtoCount += fc.primNodeProtoCount;
				primPortProtoCount += fc.primPortProtoCount;
				arcProtoCount += fc.arcProtoCount;
				nodeProtoCount += fc.nodeProtoCount;
				nodeInstCount += fc.nodeInstCount;
				portProtoCount += fc.portProtoCount;
				arcInstCount += fc.arcInstCount;
				geomCount += fc.geomCount;
				cellCount += fc.cellCount;
				userBits |= fc.userBits;
				viewCount += fc.viewCount;
				nameLength += fc.nameLength;
				varNameCount += fc.varNameCount;
				varNameLength += fc.varNameLength;
				bytesRead += fc.bytesRead;
				fileLength += fc.fileLength;
			}
		}
		System.out.println("elibCount=" + elibCount);
		System.out.println("elibWithHeader=" + elibWithHeader);
		System.out.println("elibOk=" + elibOk); 

		System.out.println("toolCount=" + toolCount);
		System.out.println("techCount=" + techCount);
		System.out.println("primNodeProtoCount=" + primNodeProtoCount);
		System.out.println("primPortProtoCount=" + primPortProtoCount);
		System.out.println("arcProtoCount=" + arcProtoCount);
		System.out.println("nodeProtoCount=" + nodeProtoCount);
		System.out.println("nodeInstCount=" + nodeInstCount);
		System.out.println("portProtoCount=" + portProtoCount);
		System.out.println("arcInstCount=" + arcInstCount);
		System.out.println("geomCount=" + geomCount);
		System.out.println("cellCount=" + cellCount);
		System.out.println("userBits=" + userBits);
		System.out.println("viewCount=" + viewCount);
		System.out.println("nameLength=" + nameLength);

		System.out.println("varNameCount=" + varNameCount);
		System.out.println("varNameLength=" + varNameLength);

		System.out.println("bytesRead=" + bytesRead);
		System.out.println("fileLength=" + fileLength);
	}

	public void reportJelib(String fileName)
	{
		if (totalLibraryContents == null) return;
		try
		{
			new StatisticsOutputJelib(fileName);
		} catch (IOException e)
		{
			System.out.println("Error storing LibraryStatisticsJelib to " + fileName + " " + e);
		}
	}

	public void reportVariableNames(String fileName)
	{
		if (totalLibraryContents == null) return;
		try
		{
			new StatisticsOutputVariableNames(fileName);
		} catch (IOException e)
		{
			System.out.println("Error storing LibraryStatisticsVariableNames to " + fileName + " " + e);
		}
	}

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

	static String bagReport(Map/*<Object,GenMath.MutableInteger>*/ bag)
	{
		String s = "";
		for (Iterator it = bag.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry e = (Map.Entry)it.next();
			GenMath.MutableInteger count = (GenMath.MutableInteger)e.getValue();
			s += " " + e.getKey() + ":" + count.intValue();
		}
		return s;
	}

	/**** Internal Classes */

	private static class Directory implements Serializable
	{
		private String dirName;
		private TreeMap/*<String,FileInstance>*/ files = new TreeMap/*<String,FileInstance>*/();

		Directory(LibraryStatistics stat, String dirName)
		{
			this.dirName = dirName;
			stat.directories.put(dirName, this);
		}

		String getName() { return dirName; }

		Iterator/*<FileInstance>*/ gerFiles() { return files.values().iterator(); }
	}

	private static class LibraryName implements Serializable
	{
		String name;

		List/*<FileContents>*/ versions = new ArrayList/*<FileContents>*/();
		TreeMap/*<String,LibraryUse>*/ references;

		LibraryName(LibraryStatistics stat, String name)
		{
			this.name = name;
			stat.libraryNames.put(name, this);
		}

		String getName() { return name; }

		Iterator/*<FileContents>*/ getVersions() { return versions.iterator(); }
	}

	private static class LibraryUse implements Serializable
	{
		Directory dir;
		LibraryName libName;
		String fullName;
		FileContents from;
	}

	static class FileContents implements Serializable
	{
		LibraryName libraryName;
		long fileLength;
		long crc;
		long lastModified;

		List/*<FileInstance>*/ instances = new ArrayList/*<FileInstance>*/();
		TreeMap/*<String,LibraryUse>*/ uses = new TreeMap/*<String,LibraryUse>*/();

		ELIB1.Header header;
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

		String fileName() { return ((FileInstance)instances.get(0)).fileName; }

		boolean isElib() { return fileName().endsWith(".elib"); }

	}

	private static class FileInstance implements Comparable, Serializable
	{
		private FileContents contents;
		private String fileName;
		private long fileLength;
		private long crc;
		private long lastModified;
		transient String canonicalPath;

		private FileInstance(LibraryStatistics stat, String fileName, long fileLength, long lastModified, long crc)
		{
			this.fileName = fileName;
			this.fileLength = fileLength;
			this.lastModified = lastModified;
			this.crc = crc;
			File file = new File(fileName);
			stat.getDirectory(file.getParent()).files.put(file.getName(), this);
		}

		FileInstance(LibraryStatistics stat, String fileName)
			throws IOException
		{
			File file = new File(fileName);
			this.fileName = fileName;
			canonicalPath = file.getCanonicalPath();
			fileLength = file.length();
			lastModified = file.lastModified();
			URL fileURL = TextUtils.makeURLToFile(fileName);

			Input in = new Input();
			try
			{
				if (in.openBinaryInput(fileURL)) throw new IOException("openBytesInput");
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

		StatisticsInput(URL url)
			throws IOException
		{
			if (openTextInput(url)) throw new IOException("openStatisticsInput");
			try
			{
				stat = new LibraryStatistics();
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
						if (indexElib >= 0)
						{
							fileName = line.substring(8, indexElib + 5);
							timeString = line.substring(indexElib + 5);
						} else if (indexJelib >= 0)
						{
							fileName = line.substring(8, indexJelib + 6);
							timeString = line.substring(indexJelib + 6);
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
											" " + TextUtils.formatDatePST(date));
						for (Iterator fit = fc.instances.iterator(); fit.hasNext(); )
						{
							FileInstance f = (FileInstance)fit.next();
							date = new Date(f.lastModified);
							printWriter.println("        " + f.fileName + " " + f.lastModified +
												" " + TextUtils.formatDatePST(date));
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

	private class StatisticsOutputJelib extends Output
	{
		StatisticsOutputJelib(String filePath)
			throws IOException
		{
			if (openTextOutputStream(filePath)) throw new IOException("openStatisticsOutputSerialized");
			try
			{
				totalLibraryContents.printJelib(printWriter);
			} finally {
				closeTextOutputStream();
			}
		}
	}

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
