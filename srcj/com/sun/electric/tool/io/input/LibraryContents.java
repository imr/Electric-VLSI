/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LibraryContents.java
 * Input/output tool: Disk library contents
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
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortOriginal;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.ncc.basic.TransitiveRelation;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.dialogs.OpenFile;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class keeps contents of disk library file.
 */
public class LibraryContents
{
	private abstract class ObjectRef implements Comparable
	{
		private final String name;
		private int tempInt;

		private ObjectRef(String name)
		{
			if (name == null) throw new IllegalArgumentException();
			this.name = name;
			if (reader.lineReader != null)
				tempInt = reader.lineReader.getLineNumber();
		}

		/**
		 * Returns a hash code for this <code>ObjectRef</code>.
		 * @return  a hash code value for this ObjectRef.
		 */
// 		public int hashCode() {	return name.hashCode(); }

		/**
		 * Compares this ObjectRef object to the specified object.  The result is
		 * <code>true</code> if and only if the argument is not
		 * <code>null</code> and is an <code>Object</code> object with
		 * the same <code>name</code>, as this ObjectRef.
		 *
		 * @param   obj   the object to compare with.
		 * @return  <code>true</code> if the objects are the same;
		 *          <code>false</code> otherwise.
		 */
// 		public boolean equals(Object obj) {
// 			if (obj instanceof ObjectRef) {
// 				ObjectRef objRef = (ObjectRef)obj;
// 				return name.equals(objRef.getName());
// 			}
// 			return false;
// 		}

		public int compareTo(Object o)
		{
			return TextUtils.nameSameNumeric(name, ((ObjectRef)o).name);
		}

		String getName() { return name; }

		String getFullName() { return name; }
	}

	abstract class NodeProtoRef extends ObjectRef
	{
		private final LinkedHashMap/*<String,PortProtoRef>*/ ports = new LinkedHashMap();

		private NodeProtoRef(String name) {	super(name); }

		Iterator/*<PortProtoRef>*/ getPorts() { return ports.values().iterator(); }

		PortProtoRef findPortProtoRef(String portName) { return (PortProtoRef)ports.get(portName); }

		PortProtoRef getPortProtoRef(String portName)
		{
			PortProtoRef ppRef = (PortProtoRef)ports.get(portName);
			if (ppRef == null)
			{
				if (this instanceof PrimitiveNodeRef)
				{
					if (needDeclarations)
						logError("Unknown Primitive Port " + getFullName() + ":" + portName);
					ppRef = new PrimitivePortRef((PrimitiveNodeRef)this, portName);
				} else
				{
					CellRef cellRef = (CellRef)this;
					if (cellRef.isExternal() ? needDeclarations : needLocalDeclarations)
						logError("Unknown Export " + getFullName() + ":" + portName);
					ppRef = new ExportRef(cellRef, portName);
				}
			}
			return ppRef;
		}

		abstract NodeProto getNodeProto();
	}

	abstract class PortProtoRef extends ObjectRef
	{
		private final NodeProtoRef nodeProtoRef;

		private PortProtoRef(NodeProtoRef nodeProtoRef, String name)
		{
			super(name);
			this.nodeProtoRef = nodeProtoRef;
			nodeProtoRef.ports.put(name, this);
		}

		abstract PortProto getPortProto();

		String getFullName() { return nodeProtoRef.getFullName() + ":" + getName(); }
	}

	class ToolRef extends ObjectRef
	{
		private VariableContents[] vars;

		private Tool tool;

		ToolRef(String name) { super(name); toolRefs.put(name, this); }

		void setVars(VariableContents[] vars) { this.vars = vars; }
		void addVars(VariableContents[] vars) { this.vars = LibraryContents.this.addVars(this.vars, vars); }
		VariableContents[] getVars() { return vars; }

		Tool getTool()
		{
			if (tool == null)
			{
				tool = Tool.findTool(getName());
				if (tool == null)
					logError("Cannot identify tool " + getName());
			}
			return tool;
		}

		void addMeaningPrefs()
		{
			if (getVars() != null)
				LibraryContents.this.addMeaningPrefs(getTool(), getVars());
		}

		void printJelib(PrintWriter out)
		{
			out.print("O" + getName());
			printJelibVars(out, vars);
			out.println();
		}
	}

	class ViewRef extends ObjectRef
	{
		private String fullName;
		private VariableContents[] vars;

		private View view;

		ViewRef(String name, String fullName)
		{
			super(name);
			viewRefs.put(name, this);
			this.fullName = fullName;
		}

		void setVars(VariableContents[] vars) { this.vars = vars; }
		void addVars(VariableContents[] vars) { this.vars = LibraryContents.this.addVars(this.vars, vars); }
		VariableContents[] getVars() { return vars; }

		void printJelib(PrintWriter out)
		{
			out.print("V" + (fullName != null ? convertJelibString(fullName) : "") + "|" + getName());
			printJelibVars(out, vars);
			out.println();
		}
	}

	class TechnologyRef extends ObjectRef
	{
		private final LinkedHashMap primitiveNodes/*<String,PrimitiveNodeRef>*/ = new LinkedHashMap();
		private final LinkedHashMap arcProtos/*<String,ArcProtoRef>*/ = new LinkedHashMap();
		private int lambda;
		private VariableContents[] vars;

		private Technology tech;

		private TechnologyRef(String name) { super(name); technologyRefs.put(name, this); }

		void setLambda(int lambda) { this.lambda = lambda; }
		
		void setVars(VariableContents[] vars) { this.vars = vars; }
		void addVars(VariableContents[] vars) { this.vars = LibraryContents.this.addVars(this.vars, vars); }
		VariableContents[] getVars() { return vars; }

		PrimitiveNodeRef newPrimitiveNodeRef(String pnName)
		{
			PrimitiveNodeRef pnRef = (PrimitiveNodeRef)primitiveNodes.get(pnName);
			if (pnRef == null)
				pnRef = new PrimitiveNodeRef(this, pnName);
			else
				logError("Primitive Node " + pnRef.getFullName() + " declared twice");
			return pnRef;
		}

		PrimitiveNodeRef getPrimitiveNodeRef(String pnName)
		{
			PrimitiveNodeRef pnRef = (PrimitiveNodeRef)primitiveNodes.get(pnName);
			if (pnRef == null)
			{
				if (needDeclarations)
					logError("Unknown Primitive Node " + getFullName() + ":" + pnName);
				pnRef = new PrimitiveNodeRef(this, pnName);
			}
			return pnRef;
		}

		ArcProtoRef newArcProtoRef(String apName)
		{
			ArcProtoRef apRef = (ArcProtoRef)arcProtos.get(apName);
			if (apRef == null)
				apRef = new ArcProtoRef(this, apName);
			else
				logError("Arc Proto " + apRef.getFullName() + " declared twice");
			return apRef;
		}

		ArcProtoRef getArcProtoRef(String apName)
		{
			ArcProtoRef apRef = (ArcProtoRef)arcProtos.get(apName);
			if (apRef == null)
			{
				if (needDeclarations)
					logError("Unknown Arc Proto " + getFullName() + ":" + apName);
				apRef = new ArcProtoRef(this, apName);
			}
			return apRef;
		}

		private Technology getTechnology()
		{
			if (tech == null)
			{
				tech = Technology.findTechnology(getName());
				if (tech == null)
				{
					logError("Cannot identify technology " + getName());
				}
			}
			return tech;
		}

		void addMeaningPrefs()
		{
			if (getVars() != null)
				LibraryContents.this.addMeaningPrefs(getTechnology(), getVars());
		}

		void printJelib(PrintWriter out)
		{
			out.println();
			out.print("# Technology " + getName());
			if (lambda != 0)
				out.print(" lambda " + lambda*0.5);
			out.println();
			out.print("T" + getName());
			printJelibVars(out, vars);
			out.println();

			for (Iterator it = primitiveNodes.values().iterator(); it.hasNext(); )
			{
				PrimitiveNodeRef primitiveNodeRef = (PrimitiveNodeRef)it.next();
				primitiveNodeRef.printJelib(out);
			}

			for (Iterator it = arcProtos.values().iterator(); it.hasNext(); )
			{
				ArcProtoRef arcProtoRef = (ArcProtoRef)it.next();
				arcProtoRef.printJelib(out);
			}
		}
	}

	class PrimitiveNodeRef extends NodeProtoRef
	{
		private final TechnologyRef techRef;
		private VariableContents[] vars;

		private PrimitiveNode pn;

		PrimitiveNodeRef(TechnologyRef techRef, String name)
		{
			super(name);
			this.techRef = techRef;
			techRef.primitiveNodes.put(name, this);
		}

		void setVars(VariableContents[] vars) { this.vars = vars; }
		void addVars(VariableContents[] vars) { this.vars = LibraryContents.this.addVars(this.vars, vars); }
		VariableContents[] getVars() { return vars; }

		String getFullName() { return techRef.getName() + ":" + getName(); }

		PrimitivePortRef newPrimitivePortRef(String ppName)
		{
			PrimitivePortRef ppRef = (PrimitivePortRef)findPortProtoRef(ppName);
			if (ppRef == null)
				ppRef = new PrimitivePortRef(this, ppName);
			else
				logError("Primitive Port " + ppRef.getFullName() + " declared twice");
			return ppRef;
		}

		NodeProto getNodeProto()
		{
			if (pn != null) return pn;
			Technology tech = techRef.getTechnology();
			if (tech == null) return null;
			pn = tech.findNodeProto(getName());
			if (pn == null)
			{
				logError("Cannot identify primitive node " + getName());
			}
			return pn;
		}

		void printJelib(PrintWriter out)
		{
			out.print("D" + getName());
			printJelibVars(out, vars);
			out.println();

			for (Iterator it = getPorts(); it.hasNext(); )
			{
				PrimitivePortRef primitivePortRef = (PrimitivePortRef)it.next();
				primitivePortRef.printJelib(out);
			}
		}
	}

	class PrimitivePortRef extends PortProtoRef
	{
		private VariableContents[] vars;

		private PrimitivePort pp;

		PrimitivePortRef(PrimitiveNodeRef primitiveNodeRef, String name) { super(primitiveNodeRef, name); }

		void setVars(VariableContents[] vars) { this.vars = vars; }
		void addVars(VariableContents[] vars) { this.vars = LibraryContents.this.addVars(this.vars, vars); }
		VariableContents[] getVars() { return vars; }

		PortProto getPortProto()
		{
			logError("Cannot identify primitive port " + getName());
			return pp;
		}

		void printJelib(PrintWriter out)
		{
			out.print("P" + getName());
			printJelibVars(out, vars);
			out.println();
		}
	}

	class ArcProtoRef extends ObjectRef
	{
		private final TechnologyRef techRef;
		private VariableContents[] vars;

		private ArcProto ap;

		ArcProtoRef(TechnologyRef techRef, String name)
		{
			super(name);
			this.techRef = techRef;
			techRef.arcProtos.put(name, this);
		}

		void setVars(VariableContents[] vars) { this.vars = vars; }
		void addVars(VariableContents[] vars) { this.vars = LibraryContents.this.addVars(this.vars, vars); }
		VariableContents[] getVars() { return vars; }

		String getFullName() { return techRef.getName() + ":" + getName(); }

		ArcProto getArcProto()
		{
			logError("Cannot identify primitive arc " + getName());
			return ap;
		}

		void printJelib(PrintWriter out)
		{
			out.print("W" + getName());
			printJelibVars(out, vars);
			out.println();
		}
	}

	class LibraryRef extends ObjectRef
	{
		private String fileName;
		private LinkedHashMap/*<String,CellRef>*/ cells = new LinkedHashMap();

		private Library lib;

		LibraryRef(String name, String fileName)
		{
			super(name);
			this.fileName = fileName;
			libraryRefs.put(name, this);
		}

		boolean isExternal() { return this != myLibraryRef; }

		String getFileName() { return fileName; }

		CellRef newExternalCellRef(String cellName, double lowX, double lowY, double highX, double highY)
		{
			assert isExternal();
			CellRef extCell = (CellRef)cells.get(cellName);
			if (extCell == null)
			{
				extCell = new CellRef(this, cellName);
				cells.put(cellName, extCell);
			} else
			{
				logError("External Cell " + extCell.getFullName() + " declared twice");
			}
			extCell.setBounds(lowX, lowY, highX, highY);
			return extCell;
		}

		private CellContents newCellContents(String cellName, long creationDate, long revisionDate)
		{
			assert !isExternal();
			CellRef cellRef = (CellRef)cells.get(cellName);
			if (cellRef == null)
			{
				cellRef = new CellRef(this, cellName);
				cells.put(cellName, cellRef);
			}
			if (cellRef.cc == null)
			{
				cellRef.cc = new CellContents(cellRef);
			} else
			{
				logError("Cell " + cellRef.getFullName() + " declared twice");
			}
			cellRef.cc.setDates(creationDate, revisionDate);
			return cellRef.cc;
		}

		CellRef getCellRef(String cellName)
		{
			CellRef cellRef = (CellRef)cells.get(cellName);
			if (cellRef == null)
			{
				cellRef = new CellRef(this, cellName);
				if (isExternal())
				{
					logError("Unknown External Cell " + getFullName() + ":" + cellName);
				}
				cells.put(cellName, cellRef);
			}
			return cellRef;
		}

		Library getLibrary() { return lib; }

		void printJelib(PrintWriter out)
		{
			out.println("L" + getName() + "|" + convertJelibString(fileName));
			for(Iterator cIt = cells.values().iterator(); cIt.hasNext(); )
			{
				CellRef cellRef = (CellRef)cIt.next();
				cellRef.printJelib(out);
			}
		}
	}

	class CellRef extends NodeProtoRef
	{
		private LibraryRef libraryRef;

		private String protoName;
		private ViewRef viewRef;
		private int version;

		private long creationDate;
		private long revisionDate;
		
		Rectangle2D bounds;

		private CellRef nextInGroup;

		private CellContents cc;

		CellRef(LibraryRef libraryRef, String name)
		{
			super(name);
			this.libraryRef = libraryRef;

			protoName = getName();
			String viewName = "";
			int openCurly = protoName.indexOf('{');
			int closeCurly = protoName.indexOf('}');
			int semiColon = protoName.indexOf(';');
			if (protoName.lastIndexOf('{') == openCurly &&
				protoName.lastIndexOf('}') == closeCurly &&
				protoName.lastIndexOf(';') == semiColon &&
				protoName.indexOf('\n') < 0 &&
				protoName.indexOf('|') < 0 &&
				protoName.indexOf(':') < 0 &&
				protoName.indexOf(' ') < 0 &&
				openCurly <= closeCurly &&
				(closeCurly < 0 || closeCurly == protoName.length() - 1) &&
				(semiColon < 0 || openCurly < 0 || semiColon < openCurly))
			{
				if (openCurly >= 0)
				{
					viewName = protoName.substring(openCurly + 1, closeCurly);
					protoName = protoName.substring(0, openCurly);
				}
				if (semiColon >= 0)
				{
					try {
						version = Integer.parseInt(protoName.substring(semiColon + 1));
					} catch (NumberFormatException e)
					{
						logError("Bad version number of cell " + protoName);
					}
					protoName = protoName.substring(0, semiColon);
				}
			} else
			{
				logError("Badly formed cell name " + protoName);
			}
			viewRef = getViewRef(viewName);
		}

		public int compareTo(Object o)
		{
			CellRef c = (CellRef)o;

			int cmp = TextUtils.nameSameNumeric(protoName, c.protoName);
			if (cmp != 0) return cmp;

			cmp = viewRef.compareTo(c.viewRef);
			if (cmp != 0) return cmp;

			if (version > 0)
				return c.version > 0 ? c.version - version : 1;
			else
				return c.version > 0 ? -1 : 0;
		}

		void setBounds(double loX, double loY, double hiX, double hiY)
		{
			bounds = new Rectangle2D.Double(loX, loY, hiX-loX, hiY-loY);
		}

		LibraryRef getLibraryRef() { return libraryRef; }

		String getFullName() { return libraryRef.getName() + ":" + getName(); }

		Rectangle2D getBounds() { return bounds; }

		boolean isExternal() { return libraryRef.isExternal(); }

		void setNextInGroup(CellRef nextInGroup)
		{
			this.nextInGroup = nextInGroup;
		}

		ExportRef newExportRef(String exportName)
		{
			ExportRef exportRef = (ExportRef)findPortProtoRef(exportName);
			if (exportRef == null)
				exportRef = new ExportRef(this, exportName);
			else
				logError("Export " + exportRef.getFullName() + " declared twice");
			return exportRef;
		}

		/**
		 * Method to return the NodeProto referenced by this NodeProto.
		 * It can be null during library reading and became a resolved
		 * reference to NodeProto later.
		 * @return the referenced NodeProto.
		 */
		public NodeProto getNodeProto()
		{
			Library cellLib = libraryRef.getLibrary();
			if (cellLib == null) return null;
			return (Cell)cellLib.findNodeProto(getName());
// 			if (cc.cell != null) // Must be in other way
// 			{
// 				if (!cc.filledIn)
// 					cc.instantiate();
// 				return cc.cell;
// 			}
// 			Rectangle2D bounds = getBounds();
// 			if (bounds == null)
// 			{
// 				logWarning("cannot find information about external cell " + getName());
// 				return null;
// //				NodeInst.newInstance(Generic.tech.invisiblePinNode, new Point2D.Double(0,0), wid, hei, cell);
// 			}
// 			cc.cell = Cell.makeInstance(cellLib, getName());
// 			if (cc.cell == null)
// 			{
// 				logError("Unable to create dummy cell " + getName() + " in library " + cellLib.getName());
// 				return null;
// 			}
// 			logError("Creating dummy cell " + getName() + " in library " + cellLib.getName());
// 			NodeInst.newInstance(Generic.tech.invisiblePinNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
// 									 bounds.getWidth(), bounds.getHeight(), cc.cell);

// 			// mark this as a dummy cell
// 			cc.cell.newVar(IOTool.IO_TRUE_LIBRARY, getLibraryRef().getName());
// 			cc.cell.newVar(Input.IO_DUMMY_OBJECT, getName());
// 			return cc.cell;
		}

		void printJelib(PrintWriter out)
		{
			Rectangle2D bounds = getBounds();
			out.print("R" + protoName + "|" + viewRef.getName() + "|" + version +
				"|" + bounds.getMinX() + "|" + bounds.getMaxX() + "|" + bounds.getMinY() + "|" + bounds.getMaxY());
			for (Iterator eIt = getPorts(); eIt.hasNext(); )
			{
				ExportRef exportRef = (ExportRef)eIt.next();
				exportRef.printJelib(out);
			}
		}
	}

	class ExportRef extends PortProtoRef
	{
		Export export;

		ExportRef(CellRef cellRef, String name) { super(cellRef, name);	}

		PortProto getPortProto() {
			logError("Cannot identify export " + getName());
			return export;
		}

		void printJelib(PrintWriter out)
		{
			out.print("#" + getName());
			if (export != null)
			{
				out.print("|" + describeJelibDescriptor(null, export.getTextDescriptor()));
				PortOriginal po = new PortOriginal(export.getOriginalPort());
				PrimitivePort pp = po.getBottomPortProto();
				PrimitiveNode pn = (PrimitiveNode)pp.getParent();
				out.print("|" + pn.getFullName() + "|");
				if (pn.getNumPorts() > 1)
					out.print(pp.getName());
				// port information
				Poly poly = export.getOriginalPort().getPoly();
				out.print("|" + TextUtils.formatDouble(poly.getCenterX(), 0) +
						  "|" + TextUtils.formatDouble(poly.getCenterY(), 0));
				int angle = po.getAngleToTop();
				out.print(angle != 0 ? "|" + angle : "|");
				out.print("|" + export.getCharacteristic().getShortName());
				if (export.isAlwaysDrawn()) out.print("/A");
				if (export.isBodyOnly()) out.print("/B");
			}
			out.println();
		}
	}

	class CellContents
	{
		private CellRef myCellRef;

		private long creationDate;
		private long revisionDate;
		
		TechnologyRef techRef;
		private boolean wantExpanded;
		private boolean allLocked;
		private boolean instancesLocked;
		private boolean inCellLibrary;
		private boolean inTechnologyLibrary;

		NodeContents[] nodes;
		ArcContents[] arcs;
		ExportContents[] exports;
		private VariableContents[] vars;

		private boolean filledIn;
		private int lineNumber;
		private Cell cell;

		CellContents(CellRef myCellRef)
		{
			this.myCellRef = myCellRef;
			cellContentses.add(this);
			filledIn = false;
		}

		String getName() { return myCellRef.getName(); }

		Cell getCell() { return cell; }
		
		boolean isFilledIn() { return filledIn; }

		private String getFileName() { return reader.filePath; }

		LibraryContents getLibraryContents() { return LibraryContents.this; }

		void setDates(long creationDate, long revisionDate)
		{
			this.creationDate = creationDate;
			this.revisionDate = revisionDate;
		}

		void setTechRef(TechnologyRef techRef) { this.techRef = techRef; }

		void setWantExpanded(boolean value) { wantExpanded = value; }
		void setAllLocked(boolean value) { allLocked = value; }
		void setInstancesLocked(boolean value) { instancesLocked = value; }
		void setInCellLibrary(boolean value) { inCellLibrary = value; }
		void setInTechnologyLibrary(boolean value) { inTechnologyLibrary = value; }

		void setVars(VariableContents[] vars) { this.vars = vars; }
		void addVars(VariableContents[] vars) { this.vars = LibraryContents.this.addVars(this.vars, vars); }

		void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

		boolean isDeclared() { return creationDate != 0; }

		NodeContents newNodeContents(NodeProtoRef protoRef, String name, String textDescriptor, double x, double y)
		{
			return new NodeContents(protoRef, name, textDescriptor, x, y);
		}

		ArcContents newArcContents(ArcProtoRef apRef, String name, String textDescriptor, double width)
		{
			return new ArcContents(apRef, name, textDescriptor, width);
		}

		ExportContents newExportContents(String name, String textDescriptor, NodeContents node, PortProtoRef port, double x, double y)
		{
			return new ExportContents(name, textDescriptor, node, port, x, y);
		}


		private int size()
		{
			int count = 1;
			if (nodes != null) count += nodes.length;
			if (arcs != null) count += arcs.length;
			if (exports != null) count += exports.length;
			return count;
		}

		/**
		 * Method called after all libraries have been read to instantiate a single Cell.
		 * @param cell the Cell to instantiate.
		 * @param cc the contents of that cell (the strings from the file).
		 */
		void instantiate()
		{
			// instantiate all subcells
// 			for (int i = 0; i < nodes.length; i++)
// 			{
// 				currentCellContents = this;
// 				LibraryContents.this.setLineNumber(nodes[i].lineNumber);
// 				nodes[i].protoRef.getNodeProto();
// 			}

// 			cell = Cell.newInstance(myCellRef.getLibraryRef().getLibrary(), getName());
// 			if (cell == null)
// 			{
// 				logError("Unable to create cell " + getName());
// 				return;
// 			}

			if (cell == null)
				allocateCell();
			if (cell == null) return;
			currentCellContents = this;

			// place all nodes
			for (int i = 0; i < nodes.length; i++)
				nodes[i].instantiate(cell);

			// place all arcs
			for (int i = 0; i < arcs.length; i++)
				arcs[i].instantiate(cell);

			// place all exports
			for (int i = 0; i < exports.length; i++)
				exports[i].instantiate(cell);

// 			cell.lowLevelSetCreationDate(new Date(creationDate));
// 			cell.lowLevelSetRevisionDate(new Date(revisionDate));
// 			cell.setTechnology(techRef.getTechnology());

// 			if (wantExpanded) cell.setWantExpanded(); else cell.clearWantExpanded();
// 			if (allLocked) cell.setAllLocked(); else cell.clearAllLocked();
// 			if (instancesLocked) cell.setInstancesLocked(); else cell.clearInstancesLocked();
// 			if (inCellLibrary) cell.setInCellLibrary(); else cell.clearInCellLibrary();
// 			if (inTechnologyLibrary) cell.setInTechnologyLibrary(); else cell.clearInTechnologyLibrary();

// 			addVariables(cell, vars);


			filledIn = true;
			nodes = null;
			arcs = null;
		}

		private void allocateCell()
		{
			cell = Cell.newInstance(myCellRef.libraryRef.getLibrary(), getName());
			if (cell == null)
			{
				logError("Unable to create cell " + getName());
				return;
			}
			Cell.CellGroup cellGroup = (Cell.CellGroup)cellGroups.get(this);
			if (cellGroup != null)
				cell.setCellGroup(cellGroup);
			Technology myTech = Technology.findTechnology(techRef.getName());
			cell.setTechnology(myTech);
			cell.lowLevelSetCreationDate(new Date(creationDate));
			cell.lowLevelSetRevisionDate(new Date(revisionDate));

			if (wantExpanded) cell.setWantExpanded(); else cell.clearWantExpanded();
			if (allLocked) cell.setAllLocked(); else cell.clearAllLocked();
			if (instancesLocked) cell.setInstancesLocked(); else cell.clearInstancesLocked();
			if (inCellLibrary) cell.setInCellLibrary(); else cell.clearInCellLibrary();
			if (inTechnologyLibrary) cell.setInTechnologyLibrary(); else cell.clearInTechnologyLibrary();
		
			// add variables in fields 7 and up
			addVariables(cell, vars);
		}

		void printJelib(PrintWriter out)
		{
			out.println();
			out.println("# Cell " + getName()); // cell.describe()
			out.print("C" + myCellRef.protoName + "|" + myCellRef.viewRef.getName() + "|" + myCellRef.version);
			out.print("|" + techRef.getName());
			out.print("|" + creationDate);
			out.print("|" + revisionDate);
			out.print("|");
			if (inCellLibrary) out.print("C");
			if (wantExpanded) out.print("E");
			if (instancesLocked) out.print("I");
			if (allLocked) out.print("L");
			if (inTechnologyLibrary) out.print("T");
			printJelibVars(out, vars);
			out.println();

			for (int i = 0; i < nodes.length; i++)
				nodes[i].printJelib(out);

			for (int i = 0; i < arcs.length; i++)
				arcs[i].printJelib(out);

			for (int i = 0; i < exports.length; i++)
				exports[i].printJelib(out);

			out.println("X");
		}
	}

	class NodeContents
	{
		private NodeProtoRef protoRef;
		private String name;
		private String textDescriptor;

		private double x, y;
		private double wid, hei;

		private boolean mirX, mirY;
		private int angle;

		private boolean expanded;
		private boolean locked;
		private boolean shortened;
		private boolean visInside;
		private boolean wiped;
		private boolean hardSelect;
		private int techSpecific;
		private String protoTextDescriptor;

		private VariableContents[] vars;
		private int lineNumber;
		private char firstChar;

		private NodeInst ni;
		private String jelibName;

		NodeContents(NodeProtoRef protoRef, String name, String textDescriptor, double x, double y)
		{
			this.protoRef = protoRef;
			this.name = name;
			this.textDescriptor = textDescriptor;
			this.x = x;
			this.y = y;
		}

		String getName() { return name; }

		NodeProtoRef getNodeProtoRef() { return protoRef; }

		void setSize(double wid, double hei)
		{
			this.wid = wid;
			this.hei = hei;
		}

		void setOrientation(boolean mirX, boolean mirY, int angle)
		{
			this.mirX = mirX;
			this.mirY = mirY;
			this.angle = angle;
		}
		
		void setExpanded(boolean value) { expanded = value; }
		void setLocked(boolean value) { locked = value; }
		void setShortened(boolean value) { shortened = value; }
		void setVisInside(boolean value) { visInside = value; }
		void setWiped(boolean value) { wiped = value; }
		void setHardSelect(boolean value) { hardSelect = value; }

		void setTechSpecific(int techSpecific) { this.techSpecific = techSpecific; }

		void setProtoTextDescriptor(String protoTextDescriptor)
		{
			this.protoTextDescriptor = protoTextDescriptor;
		}

		void setVars(VariableContents[] vars) { this.vars = vars; }
		void addVars(VariableContents[] vars) { this.vars = LibraryContents.this.addVars(this.vars, vars); }

		void setLineNumber(int lineNumber, char firstChar)
		{
			this.lineNumber = lineNumber;
			this.firstChar = firstChar;
		}

		void setJelibName(String jelibName) { this.jelibName = jelibName; }

		private void instantiate(Cell cell)
		{
			showProgress(lineNumber);

// 			NodeProto np = protoRef.getNodeProto();
// 			if (np == null) return;
// 			if (firstChar != 'N' && revision >= 1)
// 			{
// 				Rectangle2D bounds = ((Cell)np).getBounds();
// 				wid = bounds.getWidth();
// 				hei = bounds.getHeight();
// 			}
// 			if (mirX) wid = -wid;
// 			if (mirY) hei = -hei;

			String prefixName = myLibraryRef.getName();
			String protoName;
			Library cellLib = reader.lib;
			NodeProto np = null;
			if (protoRef instanceof PrimitiveNodeRef)
			{
				protoName = ((PrimitiveNodeRef)protoRef).techRef.getName() + ":" + protoRef.getName();
				Technology tech = Technology.findTechnology(((PrimitiveNodeRef)protoRef).techRef.getName());
				if (tech != null)
					np = tech.findNodeProto(protoRef.getName());
			} else {
				protoName = ((CellRef)protoRef).libraryRef.getName() + ":" + protoRef.getName();
				CellContents subCC = (CellContents)allCells.get(protoName);

				// make sure the subcell has been instantiated
				if (subCC != null)
				{
					if (!subCC.filledIn)
						subCC.instantiate();
				}

				cellLib = Library.findLibrary(((CellRef)protoRef).libraryRef.getName());
				if (cellLib != null)
					np = cellLib.findNodeProto(protoRef.getName());
			}

			double wid = this.wid;
			double hei = this.hei;
			if (mirX) wid = -wid;
			if (mirY) hei = -hei;

			if (np == null)
			{
				if (cellLib == null)
				{
					logError("Creating dummy library " + prefixName);
					cellLib = Library.newInstance(prefixName, null);
				}
				Cell dummyCell = Cell.makeInstance(cellLib, protoName);
				if (dummyCell == null)
				{
					logError("Unable to create dummy cell " + protoName + " in library " + cellLib.getName());
					return;
				}
				logError("Creating dummy cell " + protoName + " in library " + cellLib.getName());
				Rectangle2D bounds = null;
				if (protoRef instanceof CellRef)
					bounds = ((CellRef)protoRef).getBounds();
				if (bounds == null)
				{
					logError("Warning: cannot find information about external cell " + protoName);
					NodeInst.newInstance(Generic.tech.invisiblePinNode, new Point2D.Double(0,0), wid, hei, dummyCell);
				} else
				{
					NodeInst.newInstance(Generic.tech.invisiblePinNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
						bounds.getWidth(), bounds.getHeight(), dummyCell);
				}

				// mark this as a dummy cell
				dummyCell.newVar(IOTool.IO_TRUE_LIBRARY, prefixName);
				dummyCell.newVar(Input.IO_DUMMY_OBJECT, protoName);
				np = dummyCell;
			}

			// create the node
			ni = NodeInst.newInstance(np, new Point2D.Double(x, y), wid, hei, cell, angle, name, techSpecific);
			if (ni == null)
			{
				logError("cannot create node " + protoRef.getName());
				return;
			}

			// get the node name text descriptor
			loadTextDescriptor(ni.getNameTextDescriptor(), null, textDescriptor);

			// add state bits
			if (expanded) ni.setExpanded(); else ni.clearExpanded();
			if (locked) ni.setLocked(); else ni.clearLocked();
			if (shortened) ni.setShortened(); else ni.clearShortened();
			if (visInside) ni.setVisInside(); else ni.clearVisInside();
			if (wiped) ni.setWiped(); else ni.clearWiped();
			if (hardSelect) ni.setHardSelect(); else ni.clearHardSelect();

			// get text descriptor for cell instance names
			if (protoTextDescriptor != null)
				loadTextDescriptor(ni.getProtoTextDescriptor(), null, protoTextDescriptor);

			addVariables(ni, vars);
		}

		/**
		 * Method to find the proper PortInst for a specified port on a node, at a given position.
		 * @param cell the cell in which this all resides.
		 * @param portName the name of the port (may be an empty string if there is only 1 port).
		 * @param xPos the X coordinate of the port on the node.
		 * @param yPos the Y coordinate of the port on the node.
		 * @return the PortInst specified (null if none can be found).
		 */
		private PortInst figureOutPortInst(Cell cell, PortProtoRef port, double xPos, double yPos)
		{
			if (ni == null)
			{
				logError("cannot find node " + name);
				return null;
			}

			PortInst pi = null;
			String portName = port.getName();
			if (portName.length() == 0)
			{
				if (ni.getNumPortInsts() > 0)
					pi = ni.getPortInst(0);
			} else
			{
				pi = ni.findPortInst(portName);
			}

			// primitives use the name match
// 			NodeProto np = ni.getProto();
// 			if (np instanceof PrimitiveNode) return pi;

			// make sure the port can handle the position
			Point2D headPt = new Point2D.Double(xPos, yPos);
			if (pi != null)
			{
				Poly poly = pi.getPoly();
				if (!(poly.isInside(headPt) || poly.polyDistance(xPos, yPos) < LibraryFiles.TINYDISTANCE))
				{
					NodeProto np = ni.getProto();
					ErrorLogger.MessageLog log = logError("point (" + headPt.getX() + "," + headPt.getY() + ") does not fit in port " +
						pi.describe() + " which is centered at (" + poly.getCenterX() + "," + poly.getCenterY() + ")");
					log.addPoint(headPt.getX(), headPt.getY(), cell);
					if (np instanceof Cell)
						pi = null;
				}
			}
			if (pi != null) return pi;

			// see if this is a dummy cell
			Cell subCell = (Cell)ni.getProto();
			Variable var = subCell.getVar(IOTool.IO_TRUE_LIBRARY);
			if (var == null)
			{
				// not a dummy cell: create a pin at the top level
				NodeInst portNI = NodeInst.newInstance(Generic.tech.universalPinNode, headPt, 0, 0, cell);
				if (portNI == null)
				{
					logError("Unable to create dummy node in cell " + cell.describe() + " (cannot create source node)");
					return null;
				}
				ErrorLogger.MessageLog log = logError("Arc end and port discrepancy at ("+headPt.getX()+","+headPt.getY()+"), port "+
													  portName+" on node "+name);
				log.addGeom(portNI, true, cell, null);
				return portNI.getOnlyPortInst();
			}

			// a dummy cell: create a dummy export on it to fit this
			String name = portName;
			if (name.length() == 0) name = "X";
			AffineTransform unRot = ni.rotateIn();
			unRot.transform(headPt, headPt);
			AffineTransform unTrans = ni.translateIn();
			unTrans.transform(headPt, headPt);
			NodeInst portNI = NodeInst.newInstance(Generic.tech.universalPinNode, headPt, 0, 0, subCell);
			if (portNI == null)
			{
				logError("Unable to create export " + name + " on dummy cell " + subCell.describe() + " (cannot create source node)");
				return null;
			}
			PortInst portPI = portNI.getOnlyPortInst();
			Export pp = Export.newInstance(subCell, portPI, name, false);
			if (pp == null)
			{
				logError("Unable to create export " + name + " on dummy cell " + subCell.describe());
				return null;
			}
			pi = ni.findPortInstFromProto(pp);
			logError("Creating export " + name + " on dummy cell " + subCell.describe());

			return pi;
		}

		void printJelib(PrintWriter out)
		{
			char firstChar = protoRef instanceof CellRef ? 'I' : 'N';
			out.print(firstChar + protoRef.getFullName());
			out.print("|" + convertJelibString(jelibName) + "|");
			if (textDescriptor != null)
				out.print(textDescriptor);
			out.print("|" + jelibDouble(x) + "|" + jelibDouble(y));
			out.print("|" + jelibDouble(mirX ? -wid : wid) + "|" + jelibDouble(mirY ? -hei : hei) + "|" + angle + "|");
			if (hardSelect) out.print("A");
			if (expanded) out.print("E");
			if (locked) out.print("L");
			if (shortened) out.print("S");
			if (visInside) out.print("V");
			if (wiped) out.print("W");
			if (techSpecific != 0) out.print(techSpecific);
			out.print("|");
			if (protoTextDescriptor != null)
				out.print(protoTextDescriptor);
			printJelibVars(out, vars);
			out.println();
		}
	}

	class ArcContents
	{
		private ArcProtoRef apRef;
		private String name;
		private String textDescriptor;
		private double width;

		private NodeContents headNode;
		private PortProtoRef headPort;
		private double headX, headY;

		private NodeContents tailNode;
		private PortProtoRef tailPort;
		private double tailX, tailY;

		private boolean rigid;
		private boolean fixedAngle;
		private boolean slidable;
		private boolean extended;
		private boolean directional;
		private boolean reverseEnds;
		private boolean hardSelect;
		private boolean skipHead;
		private boolean skipTail;
		private boolean tailNegated;
		private boolean headNegated;

		private int angle;

		private VariableContents[] vars;
		private int lineNumber;
		
		ArcContents(ArcProtoRef apRef, String name, String textDescriptor, double width)
		{
			this.apRef = apRef;
			this.name = name;
			this.textDescriptor = textDescriptor;
			this.width = width;
		}

		String getName() { return name; }

		void setEnd(boolean tail, NodeContents node, PortProtoRef port, double x, double y)
		{
			if (tail)
			{
				tailNode = node;
				tailPort = port;
				tailX = x;
				tailY = y;
			} else
			{
				headNode = node;
				headPort = port;
				headX = x;
				headY = y;
			}
		}

		void setRigid(boolean value) { rigid = value; }
		void setFixedAngle(boolean value) { fixedAngle = value; }
		void setSlidable(boolean value) { slidable = value; }
		void setExtended(boolean value) { extended = value; }
		void setDirectional(boolean value) { directional = value; }
		void setReverseEnds(boolean value) { reverseEnds = value; }
		void setHardSelect(boolean value) { hardSelect = value; }
		void setSkipHead(boolean value) { skipHead = value; }
		void setSkipTail(boolean value) { skipTail = value; }
		void setTailNegated(boolean value) { tailNegated = value; }
		void setHeadNegated(boolean value) { headNegated = value; }

		void setAngle(int angle) { this.angle = angle; }

		void setVars(VariableContents[] vars) { this.vars = vars; }
		void addVars(VariableContents[] vars) { this.vars = LibraryContents.this.addVars(this.vars, vars); }

		void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

		void instantiate(Cell cell)
		{
			showProgress(lineNumber);

			PortInst headPI = headNode.figureOutPortInst(cell, headPort, headX, headY);
			if (headPI == null) return;

			PortInst tailPI = tailNode.figureOutPortInst(cell, tailPort, tailX, tailY);
			if (tailPI == null) return;

			String protoName = apRef.getFullName();
			ArcProto ap = ArcProto.findArcProto(protoName);
			if (ap == null)
			{
				logError("cannot find arc " + protoName);
				return;
			}
//			ArcProto ap = apRef.getArcProto();

			ArcInst ai = ArcInst.newInstance(ap, width, headPI, tailPI, new Point2D.Double(headX, headY),
											 new Point2D.Double(tailX, tailY), name, angle);
			if (ai == null)
			{
				ErrorLogger.MessageLog log = logError("cannot create arc " + apRef.getFullName());
				log.addGeom(headPI.getNodeInst(), true, cell, null);
				log.addGeom(tailPI.getNodeInst(), true, cell, null);
				return;
			}

			// get the ard name text descriptor
			loadTextDescriptor(ai.getNameTextDescriptor(), null, textDescriptor);

			// add state bits
			ai.setRigid(rigid);
			ai.setFixedAngle(fixedAngle);
			ai.setSlidable(slidable);
			ai.setExtended(extended);
			ai.setDirectional(directional);
			ai.setReverseEnds(reverseEnds);
			ai.setHardSelect(hardSelect);
			ai.setSkipHead(skipHead);
			ai.setSkipTail(skipTail);
			ai.getHead().setNegated(headNegated);
			ai.getTail().setNegated(tailNegated);

			addVariables(ai, vars);
		}

		void printJelib(PrintWriter out)
		{
			out.print("A" + apRef.getFullName() + "|" + convertJelibString(name) + "|");
			if (textDescriptor != null)
				out.print(textDescriptor);
			out.print("|" + jelibDouble(width) + "|");
			if (hardSelect) out.print("A");
			if (directional) out.print("D");
			if (!extended) out.print("E");
			if (!fixedAngle) out.print("F");
			if (headNegated) out.print("G");
			if (skipHead) out.print("H");
			if (tailNegated) out.print("N");
			if (rigid) out.print("R");
			if (slidable) out.print("S");
			if (skipTail) out.print("T");
			if (reverseEnds) out.print("V");
			out.print(angle);

			out.print("|" + convertJelibString(headNode.jelibName) + "|" + convertJelibString(headPort.getName()));
			out.print("|" + jelibDouble(headX) + "|" + jelibDouble(headY));

			out.print("|" + convertJelibString(tailNode.jelibName) + "|" + convertJelibString(tailPort.getName()));
			out.print("|" + jelibDouble(tailX) + "|" + jelibDouble(tailY));

// 			PortProto pp = con.getPortInst().getPortProto();
// 			if (ni.getProto().getNumPorts() > 1)
// 				printWriter.print(convertString(pp.getName()));

			printJelibVars(out, vars);
			out.println();
		}
	}

	class ExportContents
	{
		private String name;
		private String textDescriptor;
		private NodeContents node;
		private PortProtoRef port;
		private double x, y;
		private PortCharacteristic characteristic;
		private boolean alwaysDrawn;
		private boolean bodyOnly;
		private VariableContents[] vars;
		private int lineNumber;

		ExportContents(String name, String textDescriptor, NodeContents node, PortProtoRef port, double x, double y)
		{
			this.name = name;
			this.textDescriptor = textDescriptor;
			this.node = node;
			this.port = port;
			this.x = x;
			this.y = y;
		}

		String getName() { return name; }

		void setCharacteristic(PortCharacteristic characteristic)
		{
			this.characteristic = characteristic;
		}

		void setAlwaysDrawn(boolean value) { alwaysDrawn = value; }
		void setBodyOnly(boolean value) { bodyOnly = value; }

		void setVars(VariableContents[] vars) { this.vars = vars; }
		void addVars(VariableContents[] vars) { this.vars = LibraryContents.this.addVars(this.vars, vars); }

		void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

		void instantiate(Cell cell)
		{
			showProgress(lineNumber);

			// parse the export line
			PortInst pi = node.figureOutPortInst(cell, port, x, y);
			if (pi == null) return;

			// create the export
			Export pp = Export.newInstance(cell, pi, name, false);
			if (pp == null)
			{
				ErrorLogger.MessageLog log = logError("cannot create export " + name);
				log.addGeom(pi.getNodeInst(), true, cell, null);
				return;
			}

			loadTextDescriptor(pp.getTextDescriptor(), null, textDescriptor);

			// parse state information in field 6
			if (alwaysDrawn) pp.setAlwaysDrawn();
			if (bodyOnly) pp.setBodyOnly();

			pp.setCharacteristic(characteristic);

			addVariables(pp, vars);
		}

		void printJelib(PrintWriter out)
		{
			out.print("E" + convertJelibString(getName()) + "|" + textDescriptor);
			out.print("|" + convertJelibString(node.jelibName) + "|" + convertJelibString(port.getName()));
// 			if (subNI.getProto().getNumPorts() > 1)
// 				printWriter.print(convertString(subPP.getName()));

			// port information
			out.print("|" + jelibDouble(x) + "|" + jelibDouble(y));
			out.print("|" + characteristic.getShortName());
			if (alwaysDrawn) out.print("/A");
			if (bodyOnly) out.print("/B");

			printJelibVars(out, vars);
			out.println();
		}
	}

	class VariableContents
	{
		VariableKeyRef variableKeyRef;
		String varBits;
		char type;
		Object value;

		VariableContents(VariableKeyRef variableKeyRef, String varBits, char type, Object value)
		{
			this.variableKeyRef = variableKeyRef;
			this.varBits = varBits;
			this.type = type;
			this.value = value;
		}

		/**
		 * Method to add this variable to an ElectricObject from a List of strings.
		 * @param eObj the ElectricObject to augment with Variables.
		 */
		private void addVariable(ElectricObject eObj)
		{
			if (eObj == null) return;
			Variable.Key varKey = variableKeyRef.getVariableKey();
			if (eObj.isDeprecatedVariable(varKey)) return;
			Object obj = null;
			if (value instanceof Object[])
			{
				Object[] objList = (Object[])value;
				int limit = objList.length;
				Object [] objArray = null;
				switch (type)
				{
// 					case 'A': objArray = new ArcInst[limit];        break;
					case 'B': objArray = new Boolean[limit];        break;
					case 'C': objArray = new Cell[limit];           break;
					case 'D': objArray = new Double[limit];         break;
					case 'E': objArray = new Export[limit];         break;
					case 'F': objArray = new Float[limit];          break;
					case 'G': objArray = new Long[limit];           break;
					case 'H': objArray = new Short[limit];          break;
					case 'I': objArray = new Integer[limit];        break;
					case 'L': objArray = new Library[limit];        break;
// 					case 'N': objArray = new NodeInst[limit];       break;
					case 'O': objArray = new Tool[limit];           break;
					case 'P': objArray = new PrimitiveNode[limit];  break;
					case 'R': objArray = new ArcProto[limit];       break;
					case 'S': objArray = new String[limit];         break;
					case 'T': objArray = new Technology[limit];     break;
					case 'V': objArray = new Point2D[limit];        break;
					case 'Y': objArray = new Byte[limit];           break;
				}
				for(int j=0; j<limit; j++)
					objArray[j] = convertValue(objList[j]);
				obj = objArray;
			} else
			{
				// a scalar Variable
				obj = convertValue(value);
			}

			// create the variable
			Variable newVar = eObj.newVar(varKey, obj);
			if (newVar == null)
			{
				logError("Cannot create variable: " + variableKeyRef.getName());
				return;
			}

			// add in extra information
			TextDescriptor td = newVar.getTextDescriptor();
			loadTextDescriptor(td, newVar, varBits);
		}

		/**
		 * Method to add meaning preferences to an ElectricObject from a List of strings.
		 * @param obj the Object to augment with meaning preferences.
		 * @param vars the array of VariableContents objects that described the Object.
		 */
		void addMeaningPref(Object obj)
		{
			if (obj == null) return;
			switch (type)
			{
				case 'D': // Double
				case 'F': // Float
				case 'G': // Long
				case 'I': // Integer
				case 'S': // String
					break; // break from switch
				default:
					logError("Meaning preference type invalid: " + type);
					return;
			}
			// a scalar Variable
			Object v = convertValue(value);

			// change "meaning option"
			Pref.Meaning meaning = Pref.getMeaningVariable(obj, variableKeyRef.getName());
			if (meaning != null)
			{
				Pref.changedMeaningVariable(meaning, v);
			} else if (!(obj instanceof Technology && ((Technology)obj).convertOldVariable(variableKeyRef.getName(), v)))
			{
// 				logError("Meaning preference unknown: " + piece);
			}
		}

		/**
		 * Method to convert a String to an Object so that it can be stored in a Variable.
		 * @param piece the String to be converted.
		 * @param objectPos the character number in the string to consider.
		 * Note that the string may be larger than the object description, both by having characters
		 * before it, and also by having characters after it.
		 * Therefore, do not assume that the end of the string is the proper termination of the object specification.
		 * @param varType the type of the object to convert (a letter from the file).
		 * @return the Object representation of the given String.
		 */
		private Object convertValue(Object value)
		{
			switch (type)
			{
				case 'C':		// Cell (should delay analysis until database is built!!!)
					return ((CellRef)value).getNodeProto();
				case 'E':		// Export (should delay analysis until database is built!!!)
					return ((ExportRef)value).getPortProto();
				case 'L':		// Library (should delay analysis until database is built!!!)
					return ((LibraryRef)value).getLibrary();
				case 'O':		// Tool
					return ((ToolRef)value).getTool();
				case 'P':		// PrimitiveNode
					return ((PrimitiveNodeRef)value).getNodeProto();
				case 'R':		// ArcProto
					return ((ArcProtoRef)value).getArcProto();
				case 'T':		// Technology
					return ((TechnologyRef)value).getTechnology();
// 				case 'A':		// ArcInst (should delay analysis until database is built!!!)
				case 'B':		// Boolean
				case 'D':		// Double
				case 'F':		// Float
				case 'G':		// Long
				case 'H':		// Short
				case 'I':		// Integer
// 				case 'N':		// NodeInst (should delay analysis until database is built!!!)
				case 'S':		// String
				case 'V':		// Point2D
				case 'Y':		// Byte
					return value;
			}
			return null;
		}

		/**
		 * Method to make a string from the value in "addr" which has a type in
		 * "type".
		 */
		private void makeStringVar(PrintWriter out, Object obj)
		{
			if (obj instanceof Integer)
			{
				out.print(((Integer)obj).intValue());
				return;
			}
			if (obj instanceof Short)
			{
				out.print(((Short)obj).shortValue());
				return;
			}
			if (obj instanceof Byte)
			{
				out.print(((Byte)obj).byteValue());
				return;
			}
			if (obj instanceof String)
			{
				out.print("\"");
				out.print(convertJelibQuotedString((String)obj));
				out.print("\"");
				return;
			}
			if (obj instanceof Float)
			{
				out.print(((Float)obj).floatValue());
				return;
			}
			if (obj instanceof Double)
			{
				out.print(((Double)obj).doubleValue());
				return;
			}
			if (obj instanceof Boolean)
			{
				out.print(((Boolean)obj).booleanValue() ? "T" : "F");
				return;
			}
			if (obj instanceof Long)
			{
				out.print(((Long)obj).longValue());
				return;
			}
			if (obj instanceof Point2D)
			{
				Point2D pt2 = (Point2D)obj;
				out.print(jelibDouble(pt2.getX()) + "/" + jelibDouble(pt2.getY()));
				return;
			}
			if (obj instanceof TechnologyRef)
			{
				TechnologyRef techRef = (TechnologyRef)obj;
				out.print(techRef.getFullName());
				return;
			}
			if (obj instanceof LibraryRef)
			{
				LibraryRef libRef = (LibraryRef)obj;
				out.print(libRef.getFullName());
				return;
			}
			if (obj instanceof ToolRef)
			{
				ToolRef toolRef = (ToolRef)obj;
				out.print(toolRef.getFullName());
				return;
			}
// 			if (obj instanceof NodeInst)
// 			{
// 				NodeInst ni = (NodeInst)obj;
// 				infstr.append(convertString(ni.getParent().libDescribe()) + ":" + convertString(ni.getName()));
// 				return;
// 			}
// 			if (obj instanceof ArcInst)
// 			{
// 				ArcInst ai = (ArcInst)obj;
// 				String arcName = ai.getName();
// 				if (arcName == null)
// 				{
// 					System.out.println("Cannot save pointer to unnamed ArcInst: " + ai.getParent().describe() + ":" + ai.describe());
// 				}
// 				infstr.append(convertString(ai.getParent().libDescribe()) + ":" + convertString(arcName));
// 				return;
// 			}
			if (obj instanceof CellRef)
			{
				CellRef cellRef = (CellRef)obj;
				out.print(cellRef.getFullName());
				return;
			}
			if (obj instanceof PrimitiveNodeRef)
			{
				PrimitiveNodeRef npRef = (PrimitiveNodeRef)obj;
				out.print(npRef.getFullName());
				return;
			}
			if (obj instanceof ArcProtoRef)
			{
				ArcProtoRef apRef = (ArcProtoRef)obj;
				out.print(apRef.getFullName());
				return;
			}
			if (obj instanceof ExportRef)
			{
				ExportRef ppRef = (ExportRef)obj;
				out.print(convertJelibString(ppRef.getFullName()));
				return;
			}
		}

		void printJelib(PrintWriter out)
		{
			out.print("|" + convertJelibVariableName(variableKeyRef.getName()) + "(" + varBits + ")" + type);
			if (value instanceof Object[])
			{
				Object [] objArray = (Object [])value;
				int len = objArray.length;
				for(int i=0; i<len; i++)
				{
					Object oneObj = objArray[i];
					out.print(i != 0 ? ',' : '[');
					makeStringVar(out, oneObj);
				}
				out.print("]");
			} else
			{
				makeStringVar(out, value);
			}
		}
	}

	class VariableKeyRef extends ObjectRef
	{
		Variable.Key variableKey;

		VariableKeyRef(String name)
		{
			super(name);
			variableKeyRefs.put(name, this);
		}

		Variable.Key getVariableKey()
		{
			if (variableKey == null)
				variableKey = ElectricObject.newKey(getName());
			return variableKey;
		}
	}

	static final VariableContents[] NULL_VARS_ARRAY = {};
// 	private static final NodeContents[] NULL_NODES_ARRAY = {};
// 	private static final ArcContents[] NULL_ARCS_ARRAY = {};
// 	private static final ExportContents[] NULL_EXPORTS_ARRAY = {};

	private static List/*<LibraryContents>*/ allLibraryContents;
	private static HashMap/*<String,CellContents>*/ allCells;

	private LibraryFiles reader;
	private ELIB1.Header elibHeader;
	LinkedHashMap/*<String,VariableKeyRef>*/ variableKeyRefs = new LinkedHashMap/*<String,VariableKeyRef>*/();;
	private LinkedHashMap toolRefs/*<String,ToolRef>*/ = new LinkedHashMap/*<String,ToolRef>*/();
	private LinkedHashMap viewRefs/*<String,ViewRef>*/ = new LinkedHashMap/*<String,ViewRef>*/();
	private LinkedHashMap technologyRefs/*<String,TechnologyRef>*/ = new LinkedHashMap/*<String,TechnologyRef>*/();
	private LinkedHashMap libraryRefs/*<String,LibraryRef>*/ = new LinkedHashMap/*<String,LibraryRef>*/();
	private List/*<CellContents>*/ cellContentses = new ArrayList/*<CellContents>*/();
	private List/*<CellRef[]>*/ jelibCellGroups;
	LibraryRef myLibraryRef;
	private VariableContents[] vars;
	private VariableContents[] fakeVars;
	private int userBits;
	private HashMap/*<CellContents,Cell.CellGroup>*/ cellGroups = new HashMap/*<CellContents,Cell.CellGroup>*/();

	private Version version;
	private int revision = 0; //
	private boolean needDeclarations = false; // ELIB; JELIB revision >= 1
	private boolean needLibraryDeclarations = true; // JELIB
	private boolean needLocalDeclarations = false; // ELIB; Dump
	private CellContents currentCellContents;

	LibraryContents(String name, LibraryFiles reader)
	{
		this.reader = reader;
		myLibraryRef = new LibraryRef(name, reader.filePath);
	}

	void setElibHeader(ELIB1.Header header)
	{
		elibHeader = header;
		needDeclarations = true;
		needLibraryDeclarations = false;
		needLocalDeclarations = true;
	}

	public static void initializeLibraryInput()
	{
		allLibraryContents = new ArrayList();
		allCells = new HashMap();
	}

	public static void terminateLibraryInput()
	{
		allLibraryContents = null;
		allCells = null;
	}

	LibraryRef getMyLibraryRef() { return myLibraryRef; }

	ToolRef newToolRef(String toolName)
	{
		ToolRef toolRef = (ToolRef)toolRefs.get(toolName);
		if (toolRef == null)
			toolRef = new ToolRef(toolName);
		else
			logError("Tool " + toolRef.getFullName() + " declared twice");
		return toolRef;
	}

	ToolRef getToolRef(String toolName)
	{
		ToolRef toolRef = (ToolRef)toolRefs.get(toolName);
		if (toolRef == null)
		{
			if (needDeclarations)
				logError("Unknown tool: " + toolName);
			toolRef = new ToolRef(toolName);
		}
		return toolRef;
	}

	ViewRef newViewRef(String viewName, String fullName)
	{
		ViewRef viewRef = (ViewRef)viewRefs.get(viewName);
		if (viewRef == null)
			viewRef = new ViewRef(viewName, fullName);
		else
			logError("View " + viewRef.getFullName() + " declared twice");
		return viewRef;
	}

	ViewRef getViewRef(String viewName)
	{
		ViewRef viewRef = (ViewRef)viewRefs.get(viewName);
		if (viewRef == null)
		{
			if (needDeclarations)
				logError("Unknown view: " + viewName);
			viewRef = new ViewRef(viewName, null);
		}
		return viewRef;
	}

	TechnologyRef newTechnologyRef(String techName)
	{
		TechnologyRef techRef = (TechnologyRef)technologyRefs.get(techName);
		if (techRef == null)
			techRef = new TechnologyRef(techName);
		else
			logError("Technology " + techRef.getFullName() + " declared twice");
		return techRef;
	}

	TechnologyRef getTechnologyRef(String techName)
	{
		TechnologyRef techRef = (TechnologyRef)technologyRefs.get(techName);
		if (techRef == null)
		{
			if (needDeclarations)
				logError("Unknown technology: " + techName);
			techRef = new TechnologyRef(techName);
		}
		return techRef;
	}

	LibraryRef newLibraryRef(String libName, String fileName)
	{
		LibraryRef libRef = (LibraryRef)libraryRefs.get(libName);
		if (libRef == null)
			libRef = new LibraryRef(libName, fileName);
		else
			logError("External library " + libRef.getFullName() + " declared twice");
		return libRef;
	}

	LibraryRef getLibraryRef(String libName)
	{
		LibraryRef libRef = (LibraryRef)libraryRefs.get(libName);
		if (libRef == null)
		{
			if (needLibraryDeclarations)
				logError("Unknown library: " + libName);
			libRef = new LibraryRef(libName, null);
		}
		return libRef;
	}

	PrimitiveNodeRef getPrimitiveNodeRef(TechnologyRef techRef, String name)
	{
		int colonPos = name.indexOf(':');
		if (colonPos >= 0)
		{
			String techName = name.substring(0, colonPos);
			techRef = getTechnologyRef(techName);
			name = name.substring(colonPos + 1);
		} else if (techRef == null)
		{
			logError("Badly formed PrimitiveNode (missing colon): " + name);
			techRef = getTechnologyRef("");
		}
		return techRef.getPrimitiveNodeRef(name);
	}

	ArcProtoRef getArcProtoRef(TechnologyRef techRef, String name)
	{
		int colonPos = name.indexOf(':');
		if (colonPos >= 0)
		{
			String techName = name.substring(0, colonPos);
			techRef = getTechnologyRef(techName);
			name = name.substring(colonPos + 1);
		} else if (techRef == null)
		{
			logError("Badly formed ArcProto (missing colon): " + name);
			techRef = getTechnologyRef("");
		}
		return techRef.getArcProtoRef(name);
	}

	CellContents newCellContents(String cellName, long creationDate, long revisionDate)
	{
		return myLibraryRef.newCellContents(cellName, creationDate, revisionDate);
	}

	CellRef getCellRef(String name)
	{
		int colonPos = name.indexOf(':');
		LibraryRef libRef = myLibraryRef;
		if (colonPos >= 0)
		{
			String libName = name.substring(0, colonPos);
			libRef = getLibraryRef(libName);
			name = name.substring(colonPos + 1);
		}
		return libRef.getCellRef(name);
	}

	VariableKeyRef newVariableKeyRef(String varName)
	{
		VariableKeyRef variableKeyRef = (VariableKeyRef)variableKeyRefs.get(varName);
		if (variableKeyRef == null)
			variableKeyRef = new VariableKeyRef(varName);
		else
			logError("Variable key \"" + variableKeyRef.getName() + "\" declared twice");
		return variableKeyRef;
	}

	VariableKeyRef getVariableKeyRef(String varName)
	{
		VariableKeyRef variableKeyRef = (VariableKeyRef)variableKeyRefs.get(varName);
		if (variableKeyRef == null)
		{
// 			if (needDeclarations)
// 				logError("Unknown variable key: " + varName);
			variableKeyRef = new VariableKeyRef(varName);
		}
		return variableKeyRef;
	}

	VariableContents newVariableContents(VariableKeyRef variableKeyRef, String varBits, char type, Object value)
	{
		return new VariableContents(variableKeyRef, varBits, type, value);
	}

	void setVars(VariableContents[] vars) { this.vars = vars; }
	void addVars(VariableContents[] vars) { this.vars = addVars(this.vars, vars); }

	void addFakeVars(VariableContents[] vars) { fakeVars = addVars(this.fakeVars, vars); }

	private VariableContents[] addVars(VariableContents[] a, VariableContents[] b)
	{
		if (b == null) return a;
	varLoop:
		for (int i = 0; i < b.length; i++)
		{
			VariableContents v = b[i];
			if (v == null) continue;
			if (a == null)
			{
				a = new VariableContents[1];
				a[0] = new VariableContents(getVariableKeyRef(v.variableKeyRef.getName()), v.varBits, v.type, v.value);
				continue;
			}
			String vs = v.variableKeyRef.getName();

			int low = 0;
			int high = a.length-1;

			while (low <= high) {
				int mid = (low + high) >> 1;
				VariableContents midVal = a[mid];

				int cmp = midVal.variableKeyRef.getName().compareTo(vs);
				if (cmp < 0)
					low = mid + 1;
				else if (cmp > 0)
					high = mid - 1;
				else
					continue varLoop; // key found
			}
			VariableContents[] newA = new VariableContents[a.length+1];
			for (int j = 0; j < low; j++) newA[j] = a[j];
			newA[low] = new VariableContents(getVariableKeyRef(v.variableKeyRef.getName()), v.varBits, v.type, v.value);

			for (int j = low; j < a.length; j++) newA[j+1] = a[j];
			a = newA;
		}
		return a;
	}

	void setVersion(Version version) { this.version = version; }

	Version getVersion() { return version; }

	void setUserBits(int userBits) { this.userBits = userBits; }

	void addCellGroup(CellRef[] cellList)
	{
		if (jelibCellGroups == null) jelibCellGroups = new ArrayList();
		jelibCellGroups.add(cellList);
	}

	/**
	 * Method to read the .elib file.
	 * Returns true on error.
	 */
	List/*<CellContents>*/ checkTheLibrary(Library lib, boolean topLevelLibrary)
		throws IOException
	{
		// Check undeclared LibraryRefs and external CellRefs.
		for (Iterator lit = libraryRefs.values().iterator(); lit.hasNext();)
		{
			LibraryRef libRef = (LibraryRef)lit.next();
			if (libRef.fileName == null || libRef.fileName.equals(""))
			{
				logError("Undeclared library " + libRef.getName());
				return null;
			}

			if (libRef == myLibraryRef) continue;

			for (Iterator cit = libRef.cells.values().iterator(); cit.hasNext();)
			{
				CellRef cellRef = (CellRef)cit.next();
			}
		}

		allLibraryContents.add(this);
		myLibraryRef.lib = lib;
		lib.erase();
		addVariables(lib, vars);
		lib.setVersion(version);


		for (Iterator it = toolRefs.values().iterator(); it.hasNext();)
		{
			ToolRef toolRef = (ToolRef)it.next();
			Tool tool = Tool.findTool(toolRef.getName());
			if (tool == null)
			{
				logError("Cannot identify tool " + toolRef.getName());
				continue;
			}
			// get additional meaning preferences starting at position 1
			if (topLevelLibrary)
				toolRef.addMeaningPrefs();
		}

		for (Iterator it = viewRefs.values().iterator(); it.hasNext();)
		{
			ViewRef viewRef = (ViewRef)it.next();
			View view = View.findView(viewRef.getName());
			if (view == null)
			{
				view = View.newInstance(viewRef.getName(), viewRef.fullName);
				if (view == null)
				{
					logError("Cannot create view " + viewRef.getName());
					continue;
				}
			}
		}

		for (Iterator tit = technologyRefs.values().iterator(); tit.hasNext();)
		{
			TechnologyRef techRef = (TechnologyRef)tit.next();
			Technology tech = Technology.findTechnology(techRef.getName());
			if (tech == null)
			{
				logError("Cannot identify technology " + techRef.getName());
				continue;
			}
			// get additional meaning preferences  starting at position 1
			if (topLevelLibrary)
				techRef.addMeaningPrefs();

			for (Iterator nit = techRef.primitiveNodes.values().iterator(); nit.hasNext();)
			{
				PrimitiveNodeRef pnRef = (PrimitiveNodeRef)nit.next();
				PrimitiveNode pn = tech.findNodeProto(pnRef.getName());
				if (pn == null)
				{
					logError("Cannot identify primitive node " + pnRef.getName());
					continue;
				}

				for (Iterator pit = pnRef.getPorts(); pit.hasNext();)
				{
					PrimitivePortRef ppRef = (PrimitivePortRef)pit.next();
					PrimitivePort pp = (PrimitivePort)pn.findPortProto(ppRef.getName());
					if (pp == null)
					{
						logError("Cannot identify primitive port " + ppRef.getName());
						continue;
					}
				}
			}

			for (Iterator ait = techRef.arcProtos.values().iterator(); ait.hasNext();)
			{
				ArcProtoRef apRef = (ArcProtoRef)ait.next();
				ArcProto ap = tech.findArcProto(apRef.getName());
				if (ap == null)
				{
					logError("Cannot identify primitive arc " + apRef.getName());
					continue;
				}
			}
		}
   
		for (Iterator lit = libraryRefs.values().iterator(); lit.hasNext();)
		{
			LibraryRef libRef = (LibraryRef)lit.next();
			if (libRef == myLibraryRef) continue;
			libRef.lib = reader.readExternalLibraryFromFilename(libRef.fileName, FileType.JELIB);
			assert libRef.lib != null;

			for (Iterator cit = libRef.cells.values().iterator(); cit.hasNext();)
			{
				CellRef cellRef = (CellRef)cit.next();
				CellContents cc = cellRef.cc;
			}
		}

		for (Iterator cit = myLibraryRef.cells.values().iterator(); cit.hasNext();)
		{
			CellRef cellRef = (CellRef)cit.next();
			CellContents cc = cellRef.cc;
			if (cc == null) continue; // ????
			// remember the contents of the cell for later
			allCells.put(cellRef.getFullName(), cc);
		}

// 		for (Iterator cit = myLibraryRef.cells.values().iterator(); cit.hasNext();)
// 		{
// 			CellRef cellRef = (CellRef)cit.next();
// 			CellContents cc = cellRef.cc;
// 			if (cc == null) continue; // ????
// 			Cell newCell = Cell.newInstance(lib, cc.getName());
// 			if (newCell == null)
// 			{
// 				logError("Unable to create cell " + cc.getName());
// 				continue;
// 			}
// 			Technology tech = Technology.findTechnology(cc.techRef.getName());
// 			newCell.setTechnology(tech);
// 			newCell.lowLevelSetCreationDate(new Date(cc.creationDate));
// 			newCell.lowLevelSetRevisionDate(new Date(cc.revisionDate));

// 			if (cc.wantExpanded) newCell.setWantExpanded(); else newCell.clearWantExpanded();
// 			if (cc.allLocked) newCell.setAllLocked(); else newCell.clearAllLocked();
// 			if (cc.instancesLocked) newCell.setInstancesLocked(); else newCell.clearInstancesLocked();
// 			if (cc.inCellLibrary) newCell.setInCellLibrary(); else newCell.clearInCellLibrary();
// 			if (cc.inTechnologyLibrary) newCell.setInTechnologyLibrary(); else newCell.clearInTechnologyLibrary();

// 			// add variables in fields 7 and up
// 			addVariables(newCell, cc.vars);

// 			// remember the contents of the cell for later
// 			allCells.put(newCell, cc);
// 		}

		// collect the cells by common protoName and by "nextInGroup" relation
		TransitiveRelation transitive = new TransitiveRelation();
		HashMap/*<String,String>*/ protoNames = new HashMap();
		for (Iterator cit = myLibraryRef.cells.values().iterator(); cit.hasNext();)
		{
			CellRef cellRef = (CellRef)cit.next();
			String protoName = (String)protoNames.get(cellRef.protoName);
			if (protoName == null)
			{
				protoName = cellRef.protoName;
				protoNames.put(protoName, protoName);
			}
			transitive.theseAreRelated(cellRef, protoName);

			CellRef otherCellRef = cellRef.nextInGroup;
			if (otherCellRef != null && !otherCellRef.isExternal())
				transitive.theseAreRelated(cellRef, otherCellRef);
		}
		for (Iterator git = jelibCellGroups.iterator(); git.hasNext(); )
		{
			CellRef[] group = (CellRef[])git.next();
			CellRef firstCell = null;
			for (int i = 0; i < group.length; i++)
			{
				if (group[i] == null) continue;
				if (firstCell == null)
					firstCell = group[i];
				else
					transitive.theseAreRelated(firstCell, group[i]);
			}
		}

		// create the cell groups
		for (Iterator git = transitive.getSetsOfRelatives(); git.hasNext();)
		{
			Set group = (Set)git.next();
			Cell.CellGroup cg = new Cell.CellGroup();
			for (Iterator it = group.iterator(); it.hasNext();)
			{
				Object o = it.next();
				if (!(o instanceof CellRef)) continue;
				CellRef cellRef = (CellRef)o;
				if (cellRef.cc != null)
					cellGroups.put(cellRef.cc, cg);
			}
		}

		for (int i = 0; i < cellContentses.size(); i++)
		{
			CellContents cc = (CellContents)cellContentses.get(i);
			cc.allocateCell();
		}

		lib.clearChangedMajor();
		lib.clearChangedMinor();
		lib.setFromDisk();
		return cellContentses;
	}

	/**
	 * Method called after all libraries have been read.
	 * Instantiates all of the Cell contents that were saved in "allCells".
	 */
	void instantiateCellContents()
	{
		System.out.println("Creating the circuitry...");
//		progress.setNote("Creating the circuitry");

		// count the number of lines that need to be processed
		int numToProcess = 0;
		for(Iterator lit = allLibraryContents.iterator(); lit.hasNext(); )
		{
			LibraryContents libraryContents = (LibraryContents)lit.next();
			for (Iterator cit = libraryContents.myLibraryRef.cells.values().iterator(); cit.hasNext();)
			{
				CellRef cellRef = (CellRef)cit.next();
				CellContents cc = cellRef.cc;
				if (cc == null) continue;
				numToProcess += cc.size();
			}
		}
		setupProgress(numToProcess);

		// instantiate all cells recursively
		for(Iterator lit = allLibraryContents.iterator(); lit.hasNext(); )
		{
			LibraryContents libraryContents = (LibraryContents)lit.next();
			for (Iterator cit = libraryContents.myLibraryRef.cells.values().iterator(); cit.hasNext();)
			{
				CellRef cellRef = (CellRef)cit.next();
				CellContents cc = cellRef.cc;
				if (cc == null) continue;
				cc.instantiate();
			}
		}

		allLibraryContents = null;
		allCells = null;
	}

	private void setLineNumber(int lineNumber)
	{
		reader.setLineNumber(lineNumber);
	}

	private ErrorLogger.MessageLog logError(String message)
	{
		return reader.logError(message);
	}

	private ErrorLogger.MessageLog logWarning(String message)
	{
		return reader.logWarning(message);
	}

	private void setupProgress(int numToProcess)
	{
//		reader.setupProgress(numToProcess);
	}

	private void showProgress(int lineNumber)
	{
//		reader.showProgress(lineNumber);
	}
		
	/**
	 * Method to add variables to an ElectricObject from a List of strings.
	 * @param eObj the ElectricObject to augment with Variables.
	 * @param vars the array of VariableContents objects that described the ElectricObject.
	 */
	private void addVariables(ElectricObject eObj, VariableContents[] vars)
	{
		if (vars == null) return;
		for(int i = 0; i < vars.length; i++)
		{
			if (vars[i] != null) vars[i].addVariable(eObj);
		}
	}

	/**
	 * Method to add meaning preferences to an ElectricObject from a List of strings.
	 * @param obj the Object to augment with meaning preferences.
	 * @param vars the array of VariableContents objects that described the Object.
	 */
	void addMeaningPrefs(Object obj, VariableContents[] vars)
	{
		if (vars == null) return;
		for(int i = 0; i < vars.length; i++)
		{
			if (vars[i] != null) vars[i].addMeaningPref(obj);
		}
	}

	/**
	 * Method to load a TextDescriptor from a String description of it.
	 * @param td the TextDescriptor to load.
	 * @param var the Variable that this TextDescriptor resides on.
	 * It may be null if the TextDescriptor is on a NodeInst or Export.
	 * @param varBits the String that describes the TextDescriptor.
	 */
	private void loadTextDescriptor(TextDescriptor td, Variable var, String varBits)
	{
		double xoff = 0, yoff = 0;
		for(int j=0; j<varBits.length(); j++)
		{
			char varBit = varBits.charAt(j);
			switch (varBit)
			{
				case 'D':		// display position
					if (var != null) var.setDisplay(true);
					j++;
					if (j >= varBits.length())
					{
						logError("Incorrect display specification: " + varBits);
						break;
					}
					switch (varBits.charAt(j))
					{
						case '5': td.setPos(TextDescriptor.Position.CENT);       break;
						case '8': td.setPos(TextDescriptor.Position.UP);         break;
						case '2': td.setPos(TextDescriptor.Position.DOWN);       break;
						case '4': td.setPos(TextDescriptor.Position.LEFT);       break;
						case '6': td.setPos(TextDescriptor.Position.RIGHT);      break;
						case '7': td.setPos(TextDescriptor.Position.UPLEFT);     break;
						case '9': td.setPos(TextDescriptor.Position.UPRIGHT);    break;
						case '1': td.setPos(TextDescriptor.Position.DOWNLEFT);   break;
						case '3': td.setPos(TextDescriptor.Position.DOWNRIGHT);  break;
						case '0': td.setPos(TextDescriptor.Position.BOXED);      break;
					}
					break;
				case 'N':		// display type
					td.setDispPart(TextDescriptor.DispPos.NAMEVALUE);
					break;
				case 'A':		// absolute text size
					int semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						logError("Bad absolute size (semicolon missing): " + varBits);
						break;
					}
					td.setAbsSize(TextUtils.atoi(varBits.substring(j+1, semiPos)));
					j = semiPos;
					break;
				case 'G':		// relative text size
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						logError("Bad relative size (semicolon missing): " + varBits);
						break;
					}
					td.setRelSize(TextUtils.atof(varBits.substring(j+1, semiPos)));
					j = semiPos;
					break;
				case 'X':		// X offset
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						logError("Bad X offset (semicolon missing): " + varBits);
						break;
					}
					xoff = TextUtils.atof(varBits.substring(j+1, semiPos));
					//td.setOff(TextUtils.atof(varBits.substring(j+1, semiPos)), td.getYOff());
					j = semiPos;
					break;
				case 'Y':		// Y offset
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						logError("Bad Y offset (semicolon missing): " + varBits);
						break;
					}
					yoff = TextUtils.atof(varBits.substring(j+1, semiPos));
					//td.setOff(td.getXOff(), TextUtils.atof(varBits.substring(j+1, semiPos)));
					j = semiPos;
					break;
				case 'B':		// bold
					td.setBold(true);
					break;
				case 'I':		// italic
					td.setItalic(true);
					break;
				case 'L':		// underlined
					td.setUnderline(true);
					break;
				case 'F':		// font
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						logError("Bad font (semicolon missing): " + varBits);
						break;
					}
					TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(varBits.substring(j+1, semiPos));
					td.setFace(af.getIndex());
					j = semiPos;
					break;
				case 'C':		// color
					semiPos = varBits.indexOf(';', j);
					if (semiPos < 0)
					{
						logError("Bad color (semicolon missing): " + varBits);
						break;
					}
					td.setColorIndex(TextUtils.atoi(varBits.substring(j+1, semiPos)));
					j = semiPos;
					break;
				case 'R':		// rotation
					TextDescriptor.Rotation rot = TextDescriptor.Rotation.ROT90;
					if (j+1 < varBits.length() && varBits.charAt(j+1) == 'R')
					{
						rot = TextDescriptor.Rotation.ROT180;
						j++;
					}
					if (j+1 < varBits.length() && varBits.charAt(j+1) == 'R')
					{
						rot = TextDescriptor.Rotation.ROT270;
						j++;
					}
					td.setRotation(rot);
					break;
				case 'H':		// inheritable
					td.setInherit(true);
					break;
				case 'T':		// interior
					td.setInterior(true);
					break;
				case 'P':		// parameter
					td.setParam(true);
					break;
				case 'O':		// code
					j++;
					if (j >= varBits.length())
					{
						logError("Bad language specification: " + varBits);
						break;
					}
					char codeLetter = varBits.charAt(j);
					if (var == null)
					{
						logError("Illegal use of language specification: " + varBits);
						break;
					}
					if (codeLetter == 'J') var.setCode(Variable.Code.JAVA); else
					if (codeLetter == 'L') var.setCode(Variable.Code.LISP); else
					if (codeLetter == 'T') var.setCode(Variable.Code.TCL); else
					{
						logError("Unknown language specification: " + varBits);
					}
					break;
				case 'U':		// units
					j++;
					if (j >= varBits.length())
					{
						logError("Bad units specification: " + varBits);
						break;
					}
					char unitsLetter = varBits.charAt(j);
					if (unitsLetter == 'R') td.setUnit(TextDescriptor.Unit.RESISTANCE); else
					if (unitsLetter == 'C') td.setUnit(TextDescriptor.Unit.CAPACITANCE); else
					if (unitsLetter == 'I') td.setUnit(TextDescriptor.Unit.INDUCTANCE); else
					if (unitsLetter == 'A') td.setUnit(TextDescriptor.Unit.CURRENT); else
					if (unitsLetter == 'V') td.setUnit(TextDescriptor.Unit.VOLTAGE); else
					if (unitsLetter == 'D') td.setUnit(TextDescriptor.Unit.DISTANCE); else
					if (unitsLetter == 'T') td.setUnit(TextDescriptor.Unit.TIME); else
					{
						logError("Unknown units specification: " + varBits);
					}
					break;
			}
		}
		td.setOff(xoff, yoff);
	}

	void fillStat(LibraryStatistics.FileContents fc, LibraryContents total)
	{
		for (Iterator tit = toolRefs.values().iterator(); tit.hasNext(); )
		{
			ToolRef toolRef = (ToolRef)tit.next();
			fc.toolCount++;
			fc.nameLength += toolRef.getName().length();
			ToolRef totalToolRef = total.getToolRef(toolRef.getName());
			totalToolRef.addVars(toolRef.vars);
		}
		for (Iterator tit = technologyRefs.values().iterator(); tit.hasNext(); )
		{
			TechnologyRef technologyRef = (TechnologyRef)tit.next();
			fc.techCount++;
			fc.nameLength += technologyRef.getName().length();
			TechnologyRef totalTechnologyRef = total.getTechnologyRef(technologyRef.getName());
			totalTechnologyRef.addVars(technologyRef.vars);
			for (Iterator nit = technologyRef.primitiveNodes.values().iterator(); nit.hasNext(); )
			{
				PrimitiveNodeRef primitiveNodeRef = (PrimitiveNodeRef)nit.next();
				fc.primNodeProtoCount++;
				fc.nameLength += primitiveNodeRef.getName().length();
				PrimitiveNodeRef totalPrimitiveNodeRef = totalTechnologyRef.getPrimitiveNodeRef(primitiveNodeRef.getName());
				totalPrimitiveNodeRef.addVars(primitiveNodeRef.vars);
				for (Iterator pit = primitiveNodeRef.getPorts(); pit.hasNext(); )
				{
					PrimitivePortRef primitivePortRef = (PrimitivePortRef)pit.next();
					fc.primPortProtoCount++;
					fc.nameLength += primitivePortRef.getName().length();
					PrimitivePortRef totalPrimitivePortRef = (PrimitivePortRef)totalPrimitiveNodeRef.getPortProtoRef(primitivePortRef.getName());
					totalPrimitivePortRef.addVars(primitivePortRef.vars);
				}
			}
			for (Iterator ait = technologyRef.arcProtos.values().iterator(); ait.hasNext(); )
			{
				ArcProtoRef arcProtoRef = (ArcProtoRef)ait.next();
				fc.arcProtoCount++;
				fc.nameLength += arcProtoRef.getName().length();
				ArcProtoRef totalArcProtoRef = totalTechnologyRef.getArcProtoRef(arcProtoRef.getName());
				totalArcProtoRef.addVars(arcProtoRef.vars);
			}
		}
		for (Iterator vit = viewRefs.values().iterator(); vit.hasNext(); )
		{
			ViewRef viewRef = (ViewRef)vit.next();
			fc.viewCount++;
			fc.nameLength += viewRef.getName().length();
			fc.nameLength += viewRef.fullName.length();
			ViewRef totalViewRef = total.getViewRef(viewRef.getName());
			totalViewRef.addVars(viewRef.vars);
		}
		for (Iterator vit = variableKeyRefs.values().iterator(); vit.hasNext(); )
		{
			VariableKeyRef variableKeyRef = (VariableKeyRef)vit.next();
			fc.varNameCount++;
			fc.varNameLength += variableKeyRef.getName().length();
			total.getVariableKeyRef(variableKeyRef.getName());
		}
		fc.userBits |= userBits;
		total.userBits |= userBits;
		total.addVars(vars);
		total.addFakeVars(fakeVars);
	}
			
	void printJelib(PrintWriter out)
	{
		out.print("# header information:");
		if (elibHeader != null) out.print(" " + elibHeader);
		if (userBits != 0) out.print(" " + userBits);
		out.println();
		out.print("H" + convertJelibString(myLibraryRef.getName()) + "|" + version);
		printJelibVars(out, vars);
		out.println();
		if (fakeVars != null)
		{
			out.print("# ");
			printJelibVars(out, vars);
			out.println();
		}

		if (viewRefs.size() > 0)
		{
			out.println();
			out.println("# Views:");
			for(Iterator it = viewRefs.values().iterator(); it.hasNext(); )
			{
				ViewRef viewRef = (ViewRef)it.next();
				viewRef.printJelib(out);
			}
		}

		if (libraryRefs.size() > 1)
		{
			out.println();
			out.println("# External Libraries and cells:");
			for(Iterator it = libraryRefs.values().iterator(); it.hasNext(); )
			{
				LibraryRef libraryRef = (LibraryRef)it.next();
				if (libraryRef == myLibraryRef) continue;
				libraryRef.printJelib(out);
			}
		}

		if (toolRefs.size() > 0)
		{
			out.println();
			out.println("# Tools:");
			for(Iterator it = toolRefs.values().iterator(); it.hasNext(); )
			{
				ToolRef toolRef = (ToolRef)it.next();
				toolRef.printJelib(out);
			}
		}

		for (Iterator it = technologyRefs.values().iterator(); it.hasNext(); )
		{
			TechnologyRef technologyRef = (TechnologyRef)it.next();
			technologyRef.printJelib(out);
		}

// 		out.println();
// 		out.println("# Variable Keys");
// 		for (Iterator it = variableKeyRefs.values().iterator(); it.hasNext(); )
// 		{
// 			VariableKeyRef variableKeyRef = (VariableKeyRef)it.next();
// 			out.println("#" + convertJelibVariableName(variableKeyRef.getName()));
// 		}

		for (int i = 0; i < cellContentses.size(); i++)
		{
			CellContents cc = (CellContents)cellContentses.get(i);
			cc.printJelib(out);
		}

		// Cellect cell groups and reference counts.
// 		HashMap/*<CellRef,CellRef>*/ cellRoots = new HashMap();
// 		HashMap/*<CellRef,GenMath.MutableInteger>*/ cellCounts = new HashMap();
// 		HashMap/*<CellRef,List<CellRef>>*/ groups = new HashMap();
// 		for (Iterator it = myLibraryRef.cells.values().iterator(); it.hasNext(); )
// 		{
// 			CellRef cellRef = (CellRef)it.next();
// 			CellRef root = (CellRef)cellRoots.get(cellRef);
// 			if (root != null || cellRef.nextInGroup == null) continue;
// 			GenMath.MutableInteger count = (GenMath.MutableInteger)cellCounts.get(cellRef.nextInGroup);
// 			if (count == null)
// 			{
// 				count = new GenMath.MutableInteger(0);
// 				cellCounts.put(cellRef, count);
// 			}
// 			count.increment();

// 			CellRef c = cellRef;
// 			while (c.nextInGroup != null)
// 			{
// 				root = (CellRef)cellRoots.get(c);
// 				if (root != null) break;
// 				cellRoots.put(c, cellRef);
// 				c = c.nextInGroup;
// 			}
// 			if (root == cellRef)
// 			{
// 				// Cycle. Find the least CellRef in it.
// 				root = c;
// 				while (c.nextInGroup != root)
// 				{
// 					if (c.compareTo(root) < 0)
// 						root = c;
// 					c = c.nextInGroup;
// 				}
// 				ArrayList group = new ArrayList();
// 				for (c = root; ; c = c.nextInGroup)
// 				{
// 					cellRoots.put(c, root);
// 					group.add(c);
// 					GenMath.MutableInteger count = (GenMath.MutableInteger)cellCounts.get(c);
// 					count.setValue(-1);
// 				}
// 				groups.add(group);
// 			} else if (root == null)
// 			{
// 				root = c;
// 			}
// 			for (c = cellRef; c != null; c = c.nextInGroup)
// 			{
// 				if (cellRoots.get(c) == root) break;
// 				cellRoots.put(c, root);
// 			}
// 		}

		// Output groups.
		if (jelibCellGroups != null)
		{
			out.println();
			out.println("# Groups:");
			for (int i = 0; i < jelibCellGroups.size(); i++)
			{
				CellRef[] group = (CellRef[])jelibCellGroups.get(i);
				out.print("G");
				for (int j = 0; j < group.length; j++)
				{
					if (j > 0) out.print("|");
					if (group[j] == null) continue;
					out.print(group[j].getFullName());
				}
				out.println();
			}
		}
// 		for (Iterator it = myLibraryRef.cells.values().iterator(); it.hasNext(); )
// 		{
// 			CellRef cellRef = (CellRef)it.next();
// 			CellRef root = (CellRef)cellRoots.get(cellRef);
// 			GenMath.MutableInteger count = (GenMath.MutableInteger)cellCounts.get(cellRef);
// 			out.print("G" + cellRef.getName() + "|");
// 			if (count != null) out.print(count.intValue());
// 			if (root != null)
// 			{
// 				out.print("|" + root.getName());
// 			}
// 			out.println();
// 		}

// 		for (Iterator it = myLibraryRef.cells.values().iterator(); it.hasNext(); )
// 		{
// 			CellRef cellRef = (CellRef)it.next();
// 			CellRoot root = (CellRef)cellRoots.get(cellRef);
// 			if (root != cellRef) continue;
			
// 			GenMath.MutableInteger count = (GenMath.MutableInteger)cellCounts.get(cellRef);
// 			if (root == cellRef)
// 			{
// 				ArrayList group = new ArrayList();
// 				continue;
// 			}
// 			if (count == null || count.intValue() != 1)
// 			Lisy/*<CellRef>*/ group = (List/*<CellRef>*/)groups.get(cellRoot);
// 			if (group == null) group = new ArrayList();
// 			group.add(cellRef);
// 		}
// 		ArrayList sortedGroups/*<CellRef>*/ = new ArrayList(groups.keySet());
// 		Collections.sort(sortedGroups);
// 		for (Iterator it = sortedGroups.iterator(); it.hasNext(); )
// 		{
// 			CellRef root = (CellRef)it.next();
// 			HashSet group = (HashSet)groups.get(root);
// 			out.print("G");
// 			if (
// 		}

	}

	private static void printJelibVars(PrintWriter out, VariableContents[] vars)
	{
		if (vars == null) return;
		for (int i = 0; i < vars.length; i++)
			if (vars[i] != null) vars[i].printJelib(out);
	}

	void printJelibVariableNames(PrintWriter out)
	{
		TreeSet sortedName = new TreeSet(variableKeyRefs.keySet());
		for (Iterator it = sortedName.iterator(); it.hasNext(); )
		{
			String s = (String)it.next();
			out.println(convertToJavaString(s));
		}
	}
	
	String convertToJavaString(String s)
	{
		int i = 0;
		while (i < s.length() && s.charAt(i) >= ' ' && s.charAt(i) != '\\') i++;
		if (i == s.length()) return s;
		String s1 = s.substring(0, i);
		for(; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (c == '\\')
			{
				s1 += "\\\\";
				continue;
			}
			if (c < ' ')
			{
				String s2 = Integer.toOctalString((int)c);
				while (s2.length() < 3) s2 = "0" + s2;
				s1 += "\\" + s2;
				continue;
			}
			s1 += c;
		}
		return s1;
	}

	/**
	 * Method to convert a variable to a string that describes its TextDescriptor
	 * @param var the Variable being described (may be null).
	 * @param td the TextDescriptor being described.
	 * @return a String describing the variable/textdescriptor.
	 * The string has these fields:
	 *    Asize; for absolute size
	 *    B if bold
	 *    Cindex; if color index
	 *    Dx for display position (2=bottom 8=top 4=left 6=right 7=upleft 9=upright 1=downleft 3=downright 5=centered 0=boxed)
	 *    FfontName; if a nonstandard font
	 *    Gsize; for relative (grid unit) size
	 *    H if inherit
	 *    I if italic
	 *    L if underline
	 *    N if name=value;
	 *    Ol for language (J=Java L=Lisp T=TCL)
	 *    P if parameter
	 *    R/RR/RRR if rotated (90, 180, 270)
	 *    T if interior
	 *    Ux for units (R=resistance C=capacitance I=inductance A=current V=voltage D=distance T=time)
	 *    Xoffset; for X offset
	 *    Yoffset; for Y offset
	 */
	private String describeJelibDescriptor(Variable var, TextDescriptor td)
	{
		StringBuffer ret = new StringBuffer();
		boolean display = false;
		if (var == null || var.isDisplay()) display = true;

		if (display)
		{
			// write size
			TextDescriptor.Size size = td.getSize();
			if (size.isAbsolute()) ret.append("A" + (int)size.getSize() + ";");

			// write bold
			if (td.isBold()) ret.append("B");

			// write color
			int color = td.getColorIndex();
			if (color != 0)
				ret.append("C" + color + ";");

			// displayable: write display position
			ret.append("D");
			TextDescriptor.Position pos = td.getPos();
			if (pos == TextDescriptor.Position.UP) ret.append("8"); else
			if (pos == TextDescriptor.Position.DOWN) ret.append("2"); else
			if (pos == TextDescriptor.Position.LEFT) ret.append("4"); else
			if (pos == TextDescriptor.Position.RIGHT) ret.append("6"); else
			if (pos == TextDescriptor.Position.UPLEFT) ret.append("7"); else
			if (pos == TextDescriptor.Position.UPRIGHT) ret.append("9"); else
			if (pos == TextDescriptor.Position.DOWNLEFT) ret.append("1"); else
			if (pos == TextDescriptor.Position.DOWNRIGHT) ret.append("3"); else
			if (pos == TextDescriptor.Position.BOXED) ret.append("0"); else
				ret.append("5");

			// write font
			int font = td.getFace();
			if (font != 0)
			{
				TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(font);
				ret.append("F" + convertJelibString(af.toString()) + ";");
			}

			if (!size.isAbsolute()) ret.append("G" + TextUtils.formatDouble(size.getSize()) + ";");
		}

		// write inherit
		if (td.isInherit()) ret.append("H");

		if (display)
		{
			// write italic
			if (td.isItalic()) ret.append("I");

			// write underline
			if (td.isUnderline()) ret.append("L");

			// write display type
			TextDescriptor.DispPos dispPos = td.getDispPart();
			if (dispPos == TextDescriptor.DispPos.NAMEVALUE) ret.append("N");
		}

		// write language
		if (var != null && var.isCode())
		{
			Variable.Code codeType = var.getCode();
			if (codeType == Variable.Code.JAVA) ret.append("OJ"); else
			if (codeType == Variable.Code.LISP) ret.append("OL"); else
			if (codeType == Variable.Code.TCL) ret.append("OT");
		}

		// write parameter
		if (td.isParam()) ret.append("P");

		if (display)
		{
			// write rotation
			TextDescriptor.Rotation rot = td.getRotation();
			if (rot == TextDescriptor.Rotation.ROT90) ret.append("R"); else
			if (rot == TextDescriptor.Rotation.ROT180) ret.append("RR"); else
			if (rot == TextDescriptor.Rotation.ROT270) ret.append("RRR");
		}

		// write interior
		if (td.isInterior()) ret.append("T");

		// write units
		TextDescriptor.Unit unit = td.getUnit();
		if (unit == TextDescriptor.Unit.RESISTANCE) ret.append("UR"); else
		if (unit == TextDescriptor.Unit.CAPACITANCE) ret.append("UC"); else
		if (unit == TextDescriptor.Unit.INDUCTANCE) ret.append("UI"); else
		if (unit == TextDescriptor.Unit.CURRENT) ret.append("UA"); else
		if (unit == TextDescriptor.Unit.VOLTAGE) ret.append("UV"); else
		if (unit == TextDescriptor.Unit.DISTANCE) ret.append("UD"); else
		if (unit == TextDescriptor.Unit.TIME) ret.append("UT");

		if (display)
		{
			// write offset
			double offX = td.getXOff();
			if (offX != 0) ret.append("X" + TextUtils.formatDouble(offX, 0) + ";");
			double offY = td.getYOff();
			if (offY != 0) ret.append("Y" + TextUtils.formatDouble(offY, 0) + ";");
		}

		return ret.toString();
	}

	/**
	 * Method convert a string that is not going to be quoted.
	 * Inserts a quote character (^) before any separator (|), quotation character (") or quote character (^) in the string.
	 * Converts newlines to spaces.
	 * @param str the string to convert.
	 * @return the string with the appropriate quote characters.
	 * If no conversion is necessary, the input string is returned.
	 */
	private static String convertJelibString(String str)
	{
		StringBuffer infstr = new StringBuffer();
		int len = str.length();
		for(int i=0; i<len; i++)
		{
			char ch = str.charAt(i);
			if (ch == '\n') ch = ' ';
			if (ch == '|' || ch == '^' || ch == '"')
				infstr.append('^');
			infstr.append(ch);
		}
		return infstr.toString();
	}

	/**
	 * Method convert a string that is going to be quoted.
	 * Inserts the quote character (^) before any quotation character (") or quote character (^) in the string.
	 * Converts newlines to "^\n" (makeing the "\" and "n" separate characters).
	 * @param str the string to convert.
	 * @return the string with the appropriate quote characters.
	 * If no conversion is necessary, the input string is returned.
	 */
	private static String convertJelibQuotedString(String str)
	{
		StringBuffer infstr = new StringBuffer();
		int len = str.length();
		for(int i=0; i<len; i++)
		{
			char ch = str.charAt(i);
			if (ch == '\n') { infstr.append("^\\n");   continue; }
			if (ch == '"' || ch == '^')
				infstr.append('^');
			infstr.append(ch);
		}
		return infstr.toString();
	}

	/**
	 * Method convert a string that is a variable name.
	 * Inserts a quote character (^) before any separator (|), quotation character ("), open parenthesis, or quote character (^) in the string.
	 * Converts newlines to spaces.
	 * @param str the string to convert.
	 * @return the string with the appropriate quote characters.
	 * If no conversion is necessary, the input string is returned.
	 */
	private static String convertJelibVariableName(String str)
	{
		StringBuffer infstr = new StringBuffer();
		int len = str.length();
		for(int i=0; i<len; i++)
		{
			char ch = str.charAt(i);
			if (ch == '\n') ch = ' ';
			if (ch == '|' || ch == '^' || ch == '"' || ch == '(')
				infstr.append('^');
			infstr.append(ch);
		}
		return infstr.toString();
	}

	private static String jelibDouble(double x)
	{
		int i = (int)x;
		if (i == x)
			return i == 0 && 1/x < 0 ? "-0" : Integer.toString(i);
		else
			return Double.toString(x);
	}
}
