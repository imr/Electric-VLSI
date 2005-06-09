/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LibraryFiles.java
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.ImmutableTextDescriptor;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.ELIBConstants;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.OpenFile;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * This class reads Library files (ELIB or readable dump) format.
 */
public abstract class LibraryFiles extends Input
{
	// the cell information
	/** The number of Cells in the file. */									protected int nodeProtoCount;
	/** A list of cells being read. */										protected Cell [] nodeProtoList;
	/** lambda value for each cell of the library */						protected double [] cellLambda;
	/** total number of cells in all read libraries */						protected static int totalCells;
	/** number of cells constructed so far. */								protected static int cellsConstructed;
	/** a List of scaled Cells that got created */							protected List scaledCells;
	/** Number of errors in this LibraryFile */								protected int errorCount;
	/** the Electric version in the library file. */						protected Version version;
	/** true if old MOSIS CMOS technologies appear in the library */		protected boolean convertMosisCmosTechnologies;
	/** true to scale lambda by 20 */										protected boolean scaleLambdaBy20;
	/** true if rotation mirror bits are used */							protected boolean rotationMirrorBits;
	/** font names obtained from FONT_ASSOCIATIONS */                       private String [] fontNames;
    /** buffer for reading text descriptors and variable flags. */          MutableTextDescriptor mtd = new MutableTextDescriptor();

	static class NodeInstList
	{
		NodeInst []  theNode;
		NodeProto [] protoType;
		String []    name;
        ImmutableTextDescriptor [] nameTextDescriptor;
		int []       lowX;
		int []       highX;
		int []       lowY;
		int []       highY;
		int []       anchorX;
		int []       anchorY;
		short []     rotation;
		int []       transpose;
        ImmutableTextDescriptor [] protoTextDescriptor;
		int []       userBits;
        DiskVariable[][] vars;
        
        NodeInstList(int nodeCount, boolean hasAnchor)
        {
            theNode = new NodeInst[nodeCount];
            protoType = new NodeProto[nodeCount];
            name = new String[nodeCount];
            nameTextDescriptor = new ImmutableTextDescriptor[nodeCount];
            lowX = new int[nodeCount];
            highX = new int[nodeCount];
            lowY = new int[nodeCount];
            highY = new int[nodeCount];
            if (hasAnchor) {
                anchorX = new int[nodeCount];
                anchorY = new int[nodeCount];
            }
            rotation = new short[nodeCount];
            transpose = new int[nodeCount];
            protoTextDescriptor = new ImmutableTextDescriptor[nodeCount];
            userBits = new int[nodeCount];
            vars = new DiskVariable[nodeCount][];
        }
	};

    static class DiskVariable {
        String name;
        int flags;
        int td0;
        int td1;
        Object value;
        
        DiskVariable(String name, int flags, int td0, int td1, Object value)
        {
            this.name = name;
            this.flags = flags;
            this.td0 = td0;
            this.td1 = td1;
            this.value = value;
        }
        
        void makeVariable(ElectricObject eObj, LibraryFiles libFiles)
        {
            if (eObj instanceof Library && name.equals(Library.FONT_ASSOCIATIONS.getName()) && value instanceof String[])
            {
                libFiles.setFontNames((String[])value);
                return;
            }
            libFiles.mtd.setCBits(td0, libFiles.fixTextDescriptorFont(td1), flags);

			// see if the variable is deprecated
			Variable.Key varKey = ElectricObject.newKey(name);
			if (eObj.isDeprecatedVariable(varKey)) return;

			// set the variable
			Variable var = eObj.newVar(varKey, value, libFiles.mtd);
			if (var == null)
			{
				System.out.println("Error reading variable");
				return;
			}
        }
        
        void makeMeaningPref(Object obj)
        {
			if (!(value instanceof Integer ||
				  value instanceof Float ||
				  value instanceof Double ||
				  value instanceof String))
				return;

			// change "meaning option"
			Pref.Meaning meaning = Pref.getMeaningVariable(obj, name);
			if (meaning != null)
				Pref.changedMeaningVariable(meaning, value);
			else if (obj instanceof Technology)
				((Technology)obj).convertOldVariable(name, value);
        }
    }
    static final DiskVariable[] NULL_DISK_VARIABLE_ARRAY = {};
    
    
	/** collection of libraries and their input objects. */					private static List libsBeingRead;
	protected static final boolean VERBOSE = false;
	protected static final double TINYDISTANCE = DBMath.getEpsilon()*2;


	LibraryFiles() {}

	// *************************** THE CREATION INTERFACE ***************************

	public static void initializeLibraryInput()
	{
		libsBeingRead = new ArrayList();
	}

	public boolean readInputLibrary()
	{
		// add this reader to the list
        assert(!libsBeingRead.contains(this));
        libsBeingRead.add(this);
		//libsBeingRead.put(lib, this);
		scaledCells = new ArrayList();

		return readLib();
	}

	protected void scanNodesForRecursion(Cell cell, HashSet/*<Cell>*/ markCellForNodes, NodeProto [] nil, int start, int end)
	{
		// scan the nodes in this cell and recurse
		for(int j=start; j<end; j++)
		{
			NodeProto np = nil[j];
			if (np instanceof PrimitiveNode) continue;
			Cell otherCell = (Cell)np;
			if (otherCell == null) continue;

			// subcell: make sure that cell is setup
			if (markCellForNodes.contains(otherCell)) continue;

			LibraryFiles reader = this;
			if (otherCell.getLibrary() != cell.getLibrary())
				reader = getReaderForLib(otherCell.getLibrary());

			// subcell: make sure that cell is setup
			if (reader != null)
				reader.realizeCellsRecursively(otherCell, markCellForNodes, null, 0);
		}
		markCellForNodes.add(cell);
	}

	/**
	 * Method to read a Library.
	 * This method is never called.
	 * Instead, it is always overridden by the appropriate read subclass.
	 * @return true on error.
	 */
	protected boolean readLib() { return true; }

	/**
	 * Method to find the View to use for an old View name.
	 * @param viewName the old View name.
	 * @return the View to use (null if not found).
	 */
	protected View findOldViewName(String viewName)
	{
		if (version.getMajor() < 8)
		{
			if (viewName.equals("compensated")) return View.LAYOUTCOMP;
			if (viewName.equals("skeleton")) return View.LAYOUTSKEL;
			if (viewName.equals("simulation-snapshot")) return View.DOCWAVE;
			if (viewName.equals("netlist-netlisp-format")) return View.NETLISTNETLISP;
			if (viewName.equals("netlist-rsim-format")) return View.NETLISTRSIM;
			if (viewName.equals("netlist-silos-format")) return View.NETLISTSILOS;
			if (viewName.equals("netlist-quisc-format")) return View.NETLISTQUISC;
			if (viewName.equals("netlist-als-format")) return View.NETLISTALS;
		}
		return null;
	}

	protected Technology findTechnologyName(String name)
	{
		Technology tech = null;
		if (convertMosisCmosTechnologies)
		{
			if (name.equals("mocmossub")) tech = MoCMOS.tech; else
				if (name.equals("mocmos")) tech = Technology.findTechnology("mocmosold");
		}
		if (tech == null) tech = Technology.findTechnology(name);
		if (tech == null && name.equals("logic"))
			tech = Schematics.tech;
		if (tech == null && (name.equals("epic8c") || name.equals("epic7c")))
			tech = Technology.findTechnology("epic7s");
		return tech;
	}

	/**
	 * Method to read an external library file, given its name as stored on disk.
	 * Attempts to find the file in many different ways, including asking the user.
	 * @param theFileName the full path to the file, as written to disk.
	 * @return a Library that was read. If library cannot be read or found, creates
	 * a Library called DUMMYname, and returns that.
	 */
	protected Library readExternalLibraryFromFilename(String theFileName, FileType defaultType)
	{
		// get the path to the library file
		URL url = TextUtils.makeURLToFile(theFileName);
		String legalLibName = TextUtils.getFileNameWithoutExtension(url);
		String fileName = url.getFile();
		File libFile = new File(fileName);

		// see if this library is already read in
		String libFileName = libFile.getName();
		
		// special case if the library path came from a different computer system and still has separators
		int backSlashPos = libFileName.lastIndexOf('\\');
		int colonPos = libFileName.lastIndexOf(':');
		int slashPos = libFileName.lastIndexOf('/');
		int charPos = Math.max(backSlashPos, Math.max(colonPos, slashPos));
		if (charPos >= 0)
		{
			libFileName = libFileName.substring(charPos+1);
		}
		String libName = libFileName;
		FileType importType = OpenFile.getOpenFileType(libName, defaultType);

		if (libName.endsWith(".elib"))
		{
			libName = libName.substring(0, libName.length()-5);
		} else if (libName.endsWith(".jelib"))
		{
			libName = libName.substring(0, libName.length()-6);
		} else if (libName.endsWith(".txt"))
		{
			libName = libName.substring(0, libName.length()-4);
		} else
		{
			// no recognizable extension, add one to the file name
			libFileName += "." + defaultType.getExtensions()[0];
		}

        // Checking if the library is already open
		Library elib = Library.findLibrary(legalLibName);
		if (elib != null) return elib;

        StringBuffer errmsg = new StringBuffer();

        // first try the pure library name with no path information (JELIB, ELIB, TXT)
        // In this case, it does not look in Electric library area to avoid problems with spice configuration for old chips
        URL externalURL = getLibrary(libName + "." + FileType.JELIB.getExtensions()[0], User.getWorkingDirectory(), errmsg, false);
        if (externalURL == null) {
            // on working directory and as ELIB
            externalURL = getLibrary(libName + "." + FileType.ELIB.getExtensions()[0], User.getWorkingDirectory(), errmsg, false);
        }
        if (externalURL == null) {
            // on working directory and as txt
            externalURL = getLibrary(libName + "." + FileType.READABLEDUMP.getExtensions()[0], User.getWorkingDirectory(), errmsg, false);
        }

		// if not in the current directory, try the exact path as specified
		if (externalURL == null) {
            // try looking for the specified type
            externalURL = getLibrary(libFileName, libFile.getPath(), errmsg, true);
        }
		if (externalURL == null) {
            // try JELIB
            externalURL = getLibrary(libName + "." + FileType.JELIB.getExtensions()[0], libFile.getPath(), errmsg, true);
        }
        if (externalURL == null) {
            // try ELIB
            externalURL = getLibrary(libName + "." + FileType.ELIB.getExtensions()[0], libFile.getPath(), errmsg, true);
        }
        if (externalURL == null) {
            // try txt
            externalURL = getLibrary(libName + "." + FileType.READABLEDUMP.getExtensions()[0], libFile.getPath(), errmsg, true);
        }

        boolean exists = (externalURL == null) ? false : true;
        // last option: let user pick library location
		if (!exists)
		{
			System.out.println("Error: cannot find referenced library " + libFile.getPath()+":");
			System.out.print(errmsg.toString());
			String pt = null;
			while (true) {
				// continue to ask the user where the library is until they hit "cancel"
				String description = "Reference library '" + libFileName + "'";
				pt = OpenFile.chooseInputFile(FileType.LIBFILE, description);
				if (pt == null) {
					// user cancelled, break
					break;
				}
				// see if user chose a file we can read
				externalURL = TextUtils.makeURLToFile(pt);
				if (externalURL != null) {
					exists = TextUtils.URLExists(externalURL, null);
					if (exists) {
						// good pt, opened it, get out of here
						break;
					}
				}
			}
		}

		if (exists)
		{
			System.out.println("Reading referenced library " + externalURL.getFile());
            importType = OpenFile.getOpenFileType(externalURL.getFile(), defaultType);
            elib = Library.newInstance(legalLibName, externalURL);
		}

        if (elib != null)
        {
            // read the external library
            String oldNote = progress.getNote();
            if (progress != null)
            {
                progress.setProgress(0);
                progress.setNote("Reading referenced library " + legalLibName + "...");
            }

			// get the library name
			String eLibName = TextUtils.getFileNameWithoutExtension(externalURL);
            elib = readALibrary(externalURL, elib, eLibName, importType);
            progress.setProgress((int)(byteCount * 100 / fileLength));
            progress.setNote(oldNote);
        }

        if (elib == null)
        {
            System.out.println("Error: cannot find referenced library " + libFile.getPath());
            System.out.println("...Creating new "+legalLibName+" Library instead");
            elib = Library.newInstance(legalLibName, null);
            elib.setLibFile(TextUtils.makeURLToFile(theFileName));
            elib.clearFromDisk();
        }
//		if (failed) elib->userbits |= UNWANTEDLIB; else
//		{
//			// queue this library for announcement through change control
//			io_queuereadlibraryannouncement(elib);
//		}

		return elib;
	}

    /** Get the URL to the library named libNameNoExtension.
     * @param libFileName the library file name (with extension)
     * @param fullPathName the location of the file.
     * @param errmsg a StringBuffer into which errors may be placed.
     * @param checkElectricLib to force search in Electric library area
     * @return null if not found, or valid URL if file found
     */
    private URL getLibrary(String libFileName, String fullPathName, StringBuffer errmsg, boolean checkElectricLib)
    {
        // library does not exist: see if file is in the same directory as the main file
		URL firstURL = TextUtils.makeURLToFile(mainLibDirectory + libFileName);
        boolean exists = TextUtils.URLExists(firstURL, errmsg);
        if (exists) return firstURL;

        // try secondary library file locations
        for (Iterator libIt = LibDirs.getLibDirs(); libIt.hasNext(); )
        {
			URL url = TextUtils.makeURLToFile((String)libIt.next() + File.separator + libFileName);
            exists = TextUtils.URLExists(url, errmsg);
            if (exists) return url;
        }

        // try the exact path specified in the reference
		URL secondURL = TextUtils.makeURLToFile(fullPathName + File.separator + libFileName);
		if (!firstURL.getFile().equals(secondURL.getFile()))
		{
        	exists = TextUtils.URLExists(secondURL, errmsg);
            if (exists) return secondURL;
		}

		if (checkElectricLib)
        {
            // try the Electric library area
            URL url = LibFile.getLibFile(libFileName);
            exists = TextUtils.URLExists(url, errmsg);
            if (exists) return url;
        }
        return null;
    }

	public static void cleanupLibraryInput()
	{
		progress.setNote("Constructing cell contents...");
		progress.setProgress(0);

		// Compute technology of new cells
		Set uncomputedCells = new HashSet();
		for(Iterator it = libsBeingRead.iterator(); it.hasNext(); )
		{
			LibraryFiles reader = (LibraryFiles)it.next();
			for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
			{
				Cell cell = reader.nodeProtoList[cellIndex];
				if (cell == null) continue;
				if (cell.getLibrary() != reader.lib) continue;
				uncomputedCells.add(cell);
			}
		}
		for(Iterator it = libsBeingRead.iterator(); it.hasNext(); )
		{
			LibraryFiles reader = (LibraryFiles)it.next();
			for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
			{
				Cell cell = reader.nodeProtoList[cellIndex];
				if (cell == null) continue;
				if (cell.getLibrary() != reader.lib) continue;
				reader.computeTech(cell, uncomputedCells);
			}
		}

		// clear flag bits for scanning the library hierarchically
		totalCells = 0;
		HashSet/*<Cell>*/ markCellForNodes = new HashSet/*<Cell>*/();
		for(Iterator it = libsBeingRead.iterator(); it.hasNext(); )
		{
			LibraryFiles reader = (LibraryFiles)it.next();
			totalCells += reader.nodeProtoCount;
			for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
			{
				Cell cell = reader.nodeProtoList[cellIndex];
				if (cell == null) continue;
				if (cell.getLibrary() != reader.lib) continue;
				reader.cellLambda[cellIndex] = reader.computeLambda(cell, cellIndex);
				cell.setTempInt(cellIndex);
			}
		}
		cellsConstructed = 0;

		// now recursively adjust lambda sizes
		if (LibraryFiles.VERBOSE)
			System.out.println("Preparing to compute scale factors");
		for(int i=0; i<20; i++)
		{
			boolean unchanged = true;
			for(Iterator it = libsBeingRead.iterator(); it.hasNext(); )
			{
				LibraryFiles reader = (LibraryFiles)it.next();
				for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
				{
					Cell cell = reader.nodeProtoList[cellIndex];
					if (cell == null) continue;
					if (cell.getLibrary() != reader.lib) continue;
					if (reader.spreadLambda(cell, cellIndex))
					{
						unchanged = false;
					}
				}
			}
			if (unchanged) break;
		}
		if (LibraryFiles.VERBOSE)
			System.out.println("Finished computing scale factors");

		// recursively create the cell contents
		for(Iterator it = libsBeingRead.iterator(); it.hasNext(); )
		{
			LibraryFiles reader = (LibraryFiles)it.next();
			for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
			{
				Cell cell = reader.nodeProtoList[cellIndex];
				if (cell == null) continue;
				if (markCellForNodes.contains(cell)) continue;
				reader.realizeCellsRecursively(cell, markCellForNodes, null, 0);
			}
		}

		// tell which libraries had extra "scaled" cells added
		boolean first = true;
		for(Iterator it = libsBeingRead.iterator(); it.hasNext(); )
		{
			LibraryFiles reader = (LibraryFiles)it.next();
			if (reader.scaledCells != null && reader.scaledCells.size() != 0)
			{
				if (first)
				{
					System.out.println("WARNING: to accommodate scaling inconsistencies, these cells were created:");
					first = false;
				}
				StringBuffer sb = new StringBuffer();
				sb.append("   Library " + reader.lib.getName() + ":");
				for(Iterator sIt = reader.scaledCells.iterator(); sIt.hasNext(); )
				{
					Cell cell = (Cell)sIt.next();
					sb.append(" " + cell.noLibDescribe());
				}
				System.out.println(sb.toString());
			}
		}

		// adjust for old library conversion
//		convertOldLibraries();

		// broadcast the library-read to all listeners
		for(Iterator it = Tool.getListeners(); it.hasNext(); )
		{
			Listener listener = (Listener)it.next();
			for(Iterator lIt = libsBeingRead.iterator(); lIt.hasNext(); )
			{
				LibraryFiles reader = (LibraryFiles)lIt.next();
				listener.readLibrary(reader.lib);
			}
		}

        // clean up init (free LibraryFiles for garbage collection)
        libsBeingRead.clear();
	}

// 	private static void convertOldLibraries()
// 	{
// 		// see if the MOSIS CMOS technology now has old-style state information
// 		MoCMOS.tech.convertOldState();
// 	}

	protected LibraryFiles getReaderForLib(Library lib) {
        for (Iterator it = libsBeingRead.iterator(); it.hasNext(); ) {
            LibraryFiles reader = (LibraryFiles)it.next();
            if (reader.lib == lib) return reader;
        }
        return null;
    }

	String convertCellName(String s)
	{
		StringBuffer buf = null;
		for (int i = 0; i < s.length(); i++)
		{
			char ch = s.charAt(i);
			if (ch == '\n' || ch == '|' || ch == ':')
			{
				if (buf == null)
				{
					buf = new StringBuffer();
					buf.append(s.substring(0, i));
				}
				buf.append('-');
				continue;
			} else if (buf != null)
			{
				buf.append(ch);
			}
		}
		if (buf != null)
		{
			String newS = buf.toString();
			System.out.println("Cell name " + s + " was converted to " + newS);
			return newS;
		}
		return s;
	}

	/**
	 * Method to conver name of Geometric object.
	 * @param value name of object
	 * @param type type mask.
	 */
	protected static String convertGeomName(Object value, int type)
	{
		if (value == null || !(value instanceof String)) return null;
		String str = (String)value;
		int indexOfAt = str.indexOf('@');
		if ((type & ELIBConstants.VDISPLAY) != 0)
		{
			if (indexOfAt >= 0)
			{
				String newS = "";
				for (int i = 0; i < str.length(); i++)
				{
					char c = str.charAt(i);
					if (c == '@') c = '_';
					newS += c;
				}
				str = newS;
			}
		} else if (indexOfAt < 0) return null;
		return str;
	}
    
	/**
	 * Method to build a NodeInst.
     * @param nil arrays with data of new NodeInst
     * @param nodeIndex index in nik array
     * @param xoff x-offset of NodeInst in integer coordinates
     * @param yoff y-offset of NodeInst in integer coordinates
     * @param lambda scale factor
     * @param parent parent Cell
     * @param proto actual node proto (may be different for instance of scaled Cells)
	 */
    void realizeNode(NodeInstList nil, int nodeIndex, int xoff, int yoff, double lambda, Cell parent, NodeProto proto)
    {
		double lowX = nil.lowX[nodeIndex]-xoff;
		double lowY = nil.lowY[nodeIndex]-yoff;
		double highX = nil.highX[nodeIndex]-xoff;
		double highY = nil.highY[nodeIndex]-yoff;
		Point2D center = new Point2D.Double(((lowX + highX) / 2) / lambda, ((lowY + highY) / 2) / lambda);
		double width = (highX - lowX) / lambda;
		double height = (highY - lowY) / lambda;
        if (proto instanceof Cell) {
            Cell subCell = (Cell)proto;
			Rectangle2D bounds = subCell.getBounds();
            width = bounds.getWidth();
            height = bounds.getHeight();
        }
            
        
		int rotation = nil.rotation[nodeIndex];
		if (rotationMirrorBits)
		{
			// new version: allow mirror bits
			if ((nil.transpose[nodeIndex]&1) != 0)
			{
				height = -height;
				rotation = (rotation + 900) % 3600;
			}
			if ((nil.transpose[nodeIndex]&2) != 0)
			{
				// mirror in X
				width = -width;
			}
			if ((nil.transpose[nodeIndex]&4) != 0)
			{
				// mirror in Y
				height = -height;
			}
		} else
		{
			// old version: just use transpose information
			if (nil.transpose[nodeIndex] != 0)
			{
				height = -height;
				rotation = (rotation + 900) % 3600;
			}
		}

		// figure out the grab center if this is a cell instance
		if (proto instanceof Cell)
		{
            if (nil.anchorX != null)
//            if (magic <= ELIBConstants.MAGIC13)
            {
                // version 13 and later stores and uses anchor location
                double anchorX = (nil.anchorX[nodeIndex]-xoff) / lambda;
                double anchorY = (nil.anchorY[nodeIndex]-yoff) / lambda;
                center.setLocation(anchorX, anchorY);
            } else
            {
                Cell subCell = (Cell)proto;
                Rectangle2D bounds = subCell.getBounds();
                Point2D shift = new Point2D.Double(-bounds.getCenterX(), -bounds.getCenterY());
                AffineTransform trans = NodeInst.pureRotate(rotation, width < 0, height < 0);
                trans.transform(shift, shift);
                center.setLocation(center.getX() + shift.getX(), center.getY() + shift.getY());
            }
		}
        
        DiskVariable[] vars = nil.vars[nodeIndex];
        if (vars != null) {
            // Preprocess TRACE variables
            for (int j = 0; j < vars.length; j++) {
                DiskVariable v = vars[j];
                if (v == null) continue;
                if (v.name.equals(NodeInst.TRACE.getName()) &&
                        proto instanceof PrimitiveNode && ((PrimitiveNode)proto).isHoldsOutline() &&
                        (v.value instanceof Integer[] || v.value instanceof Float[])) {
                    // convert outline information, if present
                    Number[] outline = (Number[])v.value;
                    int newLength = ((Object[])v.value).length / 2;
                    Point2D [] newOutline = new Point2D[newLength];
                    double lam = v.value instanceof Integer[] ? lambda : 1.0;
                    for(int k=0; k<newLength; k++) {
                        double oldX = outline[k*2].doubleValue()/lam;
                        double oldY = outline[k*2+1].doubleValue()/lam;
                        newOutline[k] = new Point2D.Double(oldX, oldY);
//                        newOutline[k] = new EPoint(oldX, oldY);
                    }
                    v.value = newOutline;
                }
            }
        }
        
		NodeInst ni = NodeInst.newInstance(parent, proto, nil.name[nodeIndex], -1, nil.nameTextDescriptor[nodeIndex],
                center, width, height, rotation,
                nil.userBits[nodeIndex], nil.protoTextDescriptor[nodeIndex]);
        nil.theNode[nodeIndex] = ni;
        if (ni == null) return;
        realizeVariables(ni, vars);

        // if this was a dummy cell, log instance as an error so the user can find easily
        if (proto instanceof Cell && ((Cell)proto).getVar(IO_DUMMY_OBJECT) != null) {
            ErrorLogger.MessageLog error = Input.errorLogger.logError("Instance of dummy cell "+proto.getName(), parent, 1);
            error.addGeom(ni, true, parent, null);
        }
    }

    void realizeVariables(ElectricObject eObj, DiskVariable[] vars) {
        if (vars == null) return;
        for (int i = 0; i < vars.length; i++) {
            DiskVariable v = vars[i];
            if (v == null) continue;
            v.makeVariable(eObj, this);
        }
    }
    
    ImmutableTextDescriptor makeDescriptor(int td0, int td1) {
        mtd.setCBits(td0, fixTextDescriptorFont(td1));
        return ImmutableTextDescriptor.newImmutableTextDescriptor(mtd);
    }
        
	/**
	 * Method to grab font associations that were stored on a Library.
	 * The font associations are used later to convert indices to true font names and numbers.
	 * @param associationArray array from FONT_ASSOCIATIONS variable.
	 */
	private void setFontNames(String[] associationArray)
	{
		int maxAssociation = 0;
		for(int i=0; i<associationArray.length; i++)
		{
            if (associationArray[i] == null) continue;
			int fontNumber = TextUtils.atoi(associationArray[i]);
			if (fontNumber > maxAssociation) maxAssociation = fontNumber;
		}
		if (maxAssociation <= 0) return;

		fontNames = new String[maxAssociation];
		for(int i=0; i<maxAssociation; i++) fontNames[i] = null;
		for(int i=0; i<associationArray.length; i++)
		{
            if (associationArray[i] == null) continue;
			int fontNumber = TextUtils.atoi(associationArray[i]);
			if (fontNumber <= 0) continue;
			int slashPos = associationArray[i].indexOf('/');
			if (slashPos < 0) continue;
			fontNames[fontNumber-1] = associationArray[i].substring(slashPos+1);
		}
	}
 
	/**
	 * Method to convert the font number in a TextDescriptor to the proper value as
	 * cached in the Library.  The caching is examined by "getFontAssociationVariable()".
	 * @param descriptor1 value of descriptor1 from disk.
	 * @return patched value of descriptor1.
	 */
	private int fixTextDescriptorFont(int descriptor1)
	{
		int fontNumber = (int)((descriptor1 & ELIBConstants.VTFACE) >> ELIBConstants.VTFACESH);
        if (fontNumber == 0) return descriptor1;
        descriptor1 &= ~ELIBConstants.VTFACE;
		if (fontNames != null && fontNumber <= fontNames.length)
        {
			String fontName = fontNames[fontNumber-1];
			TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(fontName);
            if (af != null) {
                fontNumber = af.getIndex();
                if (fontNumber <= (ELIBConstants.VTFACE >> ELIBConstants.VTFACESH))
                    descriptor1 |= fontNumber << ELIBConstants.VTFACESH;
            }
		}
        return descriptor1;
	}
    
	/**
	 * Set line number for following errors and warnings.
	 * @param lineNumber line numnber for following erros and warnings.
	 */
	void setLineNumber(int lineNumber) {}

	/**
	 * Issue error message.
	 * @param message message string.
	 * @return MessageLog object for attaching further geometry details.
	 */
	ErrorLogger.MessageLog logError(String message)
	{
		errorCount++;
		System.out.println(message);
		return errorLogger.logError(message, null, -1);
	}

	/**
	 * Issue warning message.
	 * @param message message string.
	 * @return MessageLog object for attaching further geometry details.
	 */
	ErrorLogger.MessageLog logWarning(String message)
	{
		System.out.println(message);
		return errorLogger.logWarning(message, null, -1);
	}

	// *************************** THE CELL CLEANUP INTERFACE ***************************

	protected void computeTech(Cell cell, Set uncomputedCells)
	{
		uncomputedCells.remove(cell);
	}

	protected double computeLambda(Cell cell, int cellIndex) { return 1; }

	protected boolean spreadLambda(Cell cell, int cellIndex) { return false; }

	protected boolean canScale() { return false; }

	/**
	 * Method to recursively create the contents of each cell in the library.
	 */
	abstract void realizeCellsRecursively(Cell cell, HashSet/*<Cell>*/ recursiveSetupFlag, String scaledCellName, double scale);

	protected boolean readerHasExport(Cell c, String portName)
	{
		return false;
	}

}
