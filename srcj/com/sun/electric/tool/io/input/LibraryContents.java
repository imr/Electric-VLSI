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

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.ELIBConstants;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.ncc.basic.TransitiveRelation;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Set;

/**
 * This class keeps contents of disk library file.
 */
public class LibraryContents
{
	private abstract class ObjectRef
	{
		private final String name;
		private int tempInt;

		private ObjectRef(String name)
		{
			if (name == null) throw new IllegalArgumentException();
			this.name = name;
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

		String getName() { return name; }

		String getFullName() { return name; }
	}

	abstract class NodeProtoRef extends ObjectRef
	{
		private final LinkedHashMap/*<String,PortProtoRef>*/ ports = new LinkedHashMap();

		private NodeProtoRef(String name) {	super(name); }

		Iterator/*<PortProtoRef>*/ getPorts() { return ports.values().iterator(); }

		PortProtoRef findPortProtoRef(String portName) { return (PortProtoRef)ports.get(portName); }

		private PortProtoRef getPortProtoRef(String portName)
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
			nodeProtoRef.ports.put(name, nodeProtoRef);
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
	}

	class ViewRef extends ObjectRef
	{
		private VariableContents[] vars;

		private View view;

		ViewRef(String name) { super(name); viewRefs.put(name, this); }

		void setVars(VariableContents[] vars) { this.vars = vars; }
		VariableContents[] getVars() { return vars; }
	}

	class TechnologyRef extends ObjectRef
	{
		private final LinkedHashMap primitiveNodes/*<String,PrimitiveNodeRef>*/ = new LinkedHashMap();
		private final LinkedHashMap arcProtos/*<String,ArcProtoRef>*/ = new LinkedHashMap();
		private VariableContents[] vars;

		private Technology tech;

		private TechnologyRef(String name) { super(name); technologyRefs.put(name, this); }

		void setVars(VariableContents[] vars) { this.vars = vars; }
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
	}

	class PrimitivePortRef extends PortProtoRef
	{
		private VariableContents[] vars;

		private PrimitivePort pp;

		PrimitivePortRef(PrimitiveNodeRef primitiveNodeRef, String name) { super(primitiveNodeRef, name); }

		void setVars(VariableContents[] vars) { this.vars = vars; }
		VariableContents[] getVars() { return vars; }

		PortProto getPortProto()
		{
			logError("Cannot identify primitive port " + getName());
			return pp;
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
		VariableContents[] getVars() { return vars; }

		String getFullName() { return techRef.getName() + ":" + getName(); }

		ArcProto getArcProto()
		{
			logError("Cannot identify primitive arc " + getName());
			return ap;
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

		boolean isExternal() { return this != curLib; }

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

		CellContents newCellContents(String cellName, long creationDate, long revisionDate)
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

		Library getLibrary()
		{
			if (lib == null)
			{
				lib = Library.findLibrary(getName());
				if (lib == null)
				{
					//logError("Unknown Library: " + name);
					logError("Creating dummy library " + getName());
					lib = Library.newInstance(getName(), null);
				}
			}
			return lib;
		}
	}

	class CellRef extends NodeProtoRef
	{
		private LibraryRef libraryRef;

		private long creationDate;
		private long revisionDate;
		
		Rectangle2D bounds;

		private CellRef nextInGroup;

		private CellContents cc;

		CellRef(LibraryRef libraryRef, String name)
		{
			super(name);
			this.libraryRef = libraryRef;
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
	}

	class ExportRef extends PortProtoRef
	{
		Export export;

		ExportRef(CellRef cellRef, String name) { super(cellRef, name);	}

		PortProto getPortProto() {
			logError("Cannot identify export " + getName());
			return export;
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

		void setVars(VariableContents[] vars)
		{
			this.vars = vars;
		}

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

		ExportContents newExportContents(String name, String textDescriptor, NodeContents node, String portName, double x, double y)
		{
			return new ExportContents(name, textDescriptor, node, portName, x, y);
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
		private void instantiate()
		{
			// instantiate all subcells
			for (int i = 0; i < nodes.length; i++)
			{
				currentCellContents = this;
				LibraryContents.this.setLineNumber(nodes[i].lineNumber);
				nodes[i].protoRef.getNodeProto();
			}

			cell = Cell.newInstance(myCellRef.getLibraryRef().getLibrary(), getName());
			if (cell == null)
			{
				logError("Unable to create cell " + getName());
				return;
			}

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

			cell.lowLevelSetCreationDate(new Date(creationDate));
			cell.lowLevelSetRevisionDate(new Date(revisionDate));
			cell.setTechnology(techRef.getTechnology());

			if (wantExpanded) cell.setWantExpanded(); else cell.clearWantExpanded();
			if (allLocked) cell.setAllLocked(); else cell.clearAllLocked();
			if (instancesLocked) cell.setInstancesLocked(); else cell.clearInstancesLocked();
			if (inCellLibrary) cell.setInCellLibrary(); else cell.clearInCellLibrary();
			if (inTechnologyLibrary) cell.setInTechnologyLibrary(); else cell.clearInTechnologyLibrary();

			addVariables(cell, vars);


			filledIn = true;
			nodes = null;
			arcs = null;
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

		NodeContents(NodeProtoRef protoRef, String name, String textDescriptor, double x, double y)
		{
			this.firstChar = firstChar;
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

		void setLineNumber(int lineNumber, char firstChar)
		{
			this.lineNumber = lineNumber;
			this.firstChar = firstChar;
		}


		private void instantiate(Cell cell)
		{
			showProgress(lineNumber);

			NodeProto np = protoRef.getNodeProto();
			if (np == null) return;
			if (firstChar != 'N' && revision >= 1)
			{
				Rectangle2D bounds = ((Cell)np).getBounds();
				wid = bounds.getWidth();
				hei = bounds.getHeight();
			}
			if (mirX) wid = -wid;
			if (mirY) hei = -hei;

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
		private PortInst figureOutPortInst(Cell cell, String portName, double xPos, double yPos)
		{
			if (ni == null)
			{
				logError("cannot find node " + name);
				return null;
			}

			PortInst pi = null;
			if (portName.length() == 0)
			{
				if (ni.getNumPortInsts() > 0)
					pi = ni.getPortInst(0);
			} else
			{
				pi = ni.findPortInst(portName);
			}

			// primitives use the name match
			NodeProto np = ni.getProto();
			if (np instanceof PrimitiveNode) return pi;

			// make sure the port can handle the position
			Point2D headPt = new Point2D.Double(xPos, yPos);
			if (pi != null)
			{
				Poly poly = pi.getPoly();
				if (!poly.isInside(headPt)) pi = null;
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
	}

	class ArcContents
	{
		private ArcProtoRef apRef;
		private String name;
		private String textDescriptor;
		private double width;

		private NodeContents headNode;
		private String headPort;
		private double headX, headY;

		private NodeContents tailNode;
		private String tailPort;
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

		void setEnd(boolean tail, NodeContents node, String port, double x, double y)
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

		void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

		void instantiate(Cell cell)
		{
			showProgress(lineNumber);

			PortInst headPI = headNode.figureOutPortInst(cell, headPort, headX, headY);
			if (headPI == null) return;

			PortInst tailPI = tailNode.figureOutPortInst(cell, tailPort, tailX, tailY);
			if (tailPI == null) return;

			ArcProto ap = apRef.getArcProto();
			ArcInst ai = null;
			if (ap != null)
			{
				ai = ArcInst.newInstance(ap, width, headPI, tailPI, new Point2D.Double(headX, headY),
										 new Point2D.Double(tailX, tailY), name, angle);
			}
			if (ai == null)
			{
				ErrorLogger.MessageLog log = logError("cannot create arc " + apRef.getName());
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
	}

	class ExportContents
	{
		private String name;
		private String textDescriptor;
		private NodeContents node;
		private String portName;
		private double x, y;
		private PortCharacteristic characteristic;
		private boolean alwaysDrawn;
		private boolean bodyOnly;
		private VariableContents[] vars;
		private int lineNumber;

		ExportContents(String name, String textDescriptor, NodeContents node, String portName, double x, double y)
		{
			this.name = name;
			this.textDescriptor = textDescriptor;
			this.node = node;
			this.portName = portName;
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

		void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

		void instantiate(Cell cell)
		{
			showProgress(lineNumber);

			// parse the export line
			PortInst pi = node.figureOutPortInst(cell, portName, x, y);
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
	}

	static class VariableContents
	{
		private String piece;

		VariableContents(String piece)
		{
			this.piece = piece;
		}

// 		String name;
// 		String varBits;
// 		Object value;

// 		VariableContents(String name, String varBits, Object value)
// 		{
// 			this.name = name;
// 			this.varBits = varBits;
// 			this.value = value;
// 		}
	}

	static final VariableContents[] NULL_VARS_ARRAY = {};
// 	private static final NodeContents[] NULL_NODES_ARRAY = {};
// 	private static final ArcContents[] NULL_ARCS_ARRAY = {};
// 	private static final ExportContents[] NULL_EXPORTS_ARRAY = {};

	private static List/*<LibraryContents>*/ allLibraryContents;
	private static HashMap/*<String,CellContents>*/ allCells;

	private JELIB reader;
	private List/*<String>*/ variableNames;
	private LinkedHashMap toolRefs/*<String,ToolRef>*/ = new LinkedHashMap();
	private LinkedHashMap viewRefs/*<String,ViewRef>*/ = new LinkedHashMap();
	private LinkedHashMap technologyRefs/*<String,TechnologyRef>*/ = new LinkedHashMap();
	private LinkedHashMap libraryRefs/*<String,LibraryRef>*/ = new LinkedHashMap();
	private List/*<CellContents>*/ cellContentses = new ArrayList();
	LibraryRef curLib;
	private VariableContents[] curLibVars;
	private HashMap/*<CellContents,CellGroup>*/ cellGroups = new HashMap();

	private Version version;
	private int revision = 0; //
	private boolean needDeclarations = false; // ELIB; JELIB revision >= 1
	private boolean needLibraryDeclarations = true; // JELIB
	private boolean needLocalDeclarations = false; // ELIB; Dump
	private CellContents currentCellContents;

	LibraryContents(JELIB reader)
	{
		this.reader = reader;
	}

	ToolRef newToolRef(String toolName)
	{
		ToolRef toolRef = (ToolRef)toolRefs.get(toolName);
		if (toolRef == null)
			toolRef = new ToolRef(toolName);
		else
			logError("Tool " + toolRef.getFullName() + " declared twice");
		return toolRef;
	}

	ViewRef newViewRef(String viewName)
	{
		ViewRef viewRef = (ViewRef)viewRefs.get(viewName);
		if (viewRef == null)
			viewRef = new ViewRef(viewName);
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
			viewRef = new ViewRef(viewName);
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

	CellRef getCellRef(String name)
	{
		int colonPos = name.indexOf(':');
		LibraryRef libRef = curLib;
		if (colonPos >= 0)
		{
			String libName = name.substring(0, colonPos);
			libRef = getLibraryRef(libName);
			name = name.substring(colonPos + 1);
		}
		return libRef.getCellRef(name);
	}

	void setCurLib(LibraryRef curLibRef, VariableContents[] curLibVars)
	{
		this.curLib = curLibRef;
		this.curLibVars = curLibVars;
	}

	/**
	 * Method to read the .elib file.
	 * Returns true on error.
	 */
	List/*<CellContents>*/ readTheLibrary(Library lib, boolean topLevelLibrary)
		throws IOException
	{
		if (topLevelLibrary)
		{
			allLibraryContents = new ArrayList();
			allCells = new HashMap();
		}
		allLibraryContents.add(this);
		curLib.lib = lib;
		curLib.lib.erase();
		addVariables(curLib.lib, curLibVars);


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
			if (libRef == curLib) continue;
			Library elib = Library.findLibrary(libRef.getName());
			if (elib == null)
			{
				reader.readExternalLibraryFromFilename(libRef.fileName, OpenFile.Type.JELIB);
				elib = Library.findLibrary(libRef.getName());
			}

			for (Iterator cit = libRef.cells.values().iterator(); cit.hasNext();)
			{
				CellRef cellRef = (CellRef)cit.next();
				CellContents cc = cellRef.cc;
			}
		}

		for (Iterator cit = curLib.cells.values().iterator(); cit.hasNext();)
		{
			CellRef cellRef = (CellRef)cit.next();
			CellContents cc = cellRef.cc;
			if (cc == null) continue; // ????
			// remember the contents of the cell for later
			allCells.put(cellRef.getFullName(), cc);
		}

// 		for (Iterator cit = curLib.cells.values().iterator(); cit.hasNext();)
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
		for (Iterator cit = curLib.cells.values().iterator(); cit.hasNext();)
		{
			CellRef cellRef = (CellRef)cit.next();
			CellName cellName = CellName.parseName(cellRef.getName());
			if (cellName != null)
			{
				String protoName = (String)protoNames.get(cellName.getName());
				if (protoName == null)
				{
					protoName = cellName.getName();
					protoNames.put(protoName, protoName);
				}
				transitive.theseAreRelated(cellRef, protoName);
			}
			CellRef otherCellRef = cellRef.nextInGroup;
			if (otherCellRef != null && !otherCellRef.isExternal())
				transitive.theseAreRelated(cellRef, otherCellRef);
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
			allocateCell((CellContents)cellContentses.get(i));
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
			for (Iterator cit = libraryContents.curLib.cells.values().iterator(); cit.hasNext();)
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
			for (Iterator cit = libraryContents.curLib.cells.values().iterator(); cit.hasNext();)
			{
				CellRef cellRef = (CellRef)cit.next();
				CellContents cc = cellRef.cc;
				if (cc == null) continue;
				if (cc.filledIn) continue;
				libraryContents.instantiateCellContent(cc);
			}
		}

		allLibraryContents = null;
		allCells = null;
	}

	private Cell allocateCell(CellContents cc)
	{
		Cell cell = cc.cell = Cell.newInstance(cc.myCellRef.libraryRef.getLibrary(), cc.getName());
		if (cell == null)
		{
			logError("Unable to create cell " + cc.getName());
			return null;
		}
		Cell.CellGroup cellGroup = (Cell.CellGroup)cellGroups.get(cc);
		if (cellGroup != null)
			cell.setCellGroup(cellGroup);
		Technology myTech = Technology.findTechnology(cc.techRef.getName());
		cell.setTechnology(myTech);
		cell.lowLevelSetCreationDate(new Date(cc.creationDate));
		cell.lowLevelSetRevisionDate(new Date(cc.revisionDate));
		
		if (cc.wantExpanded) cell.setWantExpanded(); else cell.clearWantExpanded();
		if (cc.allLocked) cell.setAllLocked(); else cell.clearAllLocked();
		if (cc.instancesLocked) cell.setInstancesLocked(); else cell.clearInstancesLocked();
		if (cc.inCellLibrary) cell.setInCellLibrary(); else cell.clearInCellLibrary();
		if (cc.inTechnologyLibrary) cell.setInTechnologyLibrary(); else cell.clearInTechnologyLibrary();
		
		// add variables in fields 7 and up
		addVariables(cell, cc.vars);

		return cell;
	}

	/**
	 * Method called after all libraries have been read to instantiate a single Cell.
	 * @param cell the Cell to instantiate.
	 * @param cc the contents of that cell (the strings from the file).
	 */
	void instantiateCellContent(CellContents cc)
	{
		if (cc == null) return; // ????
		if (cc.filledIn) return;
		CellRef cellRef = cc.myCellRef;
		Cell cell = cc.cell;
		if (cell == null)
			cell = allocateCell(cc);
		for (int i = 0; i < cc.nodes.length; i++)
		{
			NodeContents nc = cc.nodes[i];
			showProgress(nc.lineNumber);

			String prefixName = curLib.getName();
			String protoName;
			Library cellLib = reader.lib;
			NodeProto np = null;
			if (nc.protoRef instanceof PrimitiveNodeRef)
			{
				protoName = ((PrimitiveNodeRef)nc.protoRef).techRef.getName() + ":" + nc.protoRef.getName();
				Technology tech = Technology.findTechnology(((PrimitiveNodeRef)nc.protoRef).techRef.getName());
				if (tech != null)
					np = tech.findNodeProto(nc.protoRef.getName());
			} else {
				protoName = ((CellRef)nc.protoRef).libraryRef.getName() + ":" + nc.protoRef.getName();
				CellContents subCC = (CellContents)allCells.get(protoName);

				// make sure the subcell has been instantiated
				if (subCC != null)
				{
					if (!subCC.filledIn)
						subCC.getLibraryContents().instantiateCellContent(subCC);
				}

				cellLib = Library.findLibrary(((CellRef)nc.protoRef).libraryRef.getName());
				if (cellLib != null)
					np = cellLib.findNodeProto(nc.protoRef.getName());
			}

			double wid = nc.wid;
			double hei = nc.hei;
			if (nc.mirX) wid = -wid;
			if (nc.mirY) hei = -hei;

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
					continue;
				}
				logError("Creating dummy cell " + protoName + " in library " + cellLib.getName());
				Rectangle2D bounds = null;
				if (nc.protoRef instanceof CellRef)
					bounds = ((CellRef)nc.protoRef).getBounds();
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

			// make sure the subcell has been instantiated
// 			if (np instanceof Cell)
// 			{
// 				Cell subCell = (Cell)np;
// 				CellContents subCC = (CellContents)allCells.get(subCell);
// 				if (subCC != null)
// 				{
// 					if (!subCC.filledIn)
// 						instantiateCellContent(subCC);
// 				}
// 			}

			// create the node
			NodeInst ni = NodeInst.newInstance(np, new Point2D.Double(nc.x, nc.y), wid, hei, cell, nc.angle, nc.getName(), nc.techSpecific);
			nc.ni = ni;
			if (ni == null)
			{
				logError("cannot create node " + protoName);
				continue;
			}

			// get the node name text descriptor
			loadTextDescriptor(ni.getNameTextDescriptor(), null, nc.textDescriptor);

			// add state bits
			if (nc.expanded) ni.setExpanded(); else ni.clearExpanded();
			if (nc.locked) ni.setLocked(); else ni.clearLocked();
			if (nc.shortened) ni.setShortened(); else ni.clearShortened();
			if (nc.visInside) ni.setVisInside(); else ni.clearVisInside();
			if (nc.wiped) ni.setWiped(); else ni.clearWiped();
			if (nc.hardSelect) ni.setHardSelect(); else ni.clearHardSelect();

			// get text descriptor for cell instance names
			if (nc.protoTextDescriptor != null)
				loadTextDescriptor(ni.getProtoTextDescriptor(), null, nc.protoTextDescriptor);

			// add variables in fields 10 and up
			addVariables(ni, nc.vars);
		}

		// place all exports
		for(int i = 0; i < cc.exports.length; i++)
		{
			ExportContents ec = cc.exports[i];
			showProgress(ec.lineNumber);

			PortInst pi = figureOutPortInst(cell, ec.portName, ec.node, ec.x, ec.y, cc.getFileName(), ec.lineNumber);
			if (pi == null) continue;

			// create the export
			Export pp = Export.newInstance(cell, pi, ec.getName(), false);
			if (pp == null)
			{
				ErrorLogger.MessageLog log = logError("cannot create export " + ec.getName());
				log.addGeom(pi.getNodeInst(), true, cell, null);
				continue;
			}

			// get text descriptor in field 1
			loadTextDescriptor(pp.getTextDescriptor(), null, ec.textDescriptor);

			// parse state information in field 6
			if (ec.alwaysDrawn) pp.setAlwaysDrawn();
			if (ec.bodyOnly) pp.setBodyOnly();
			pp.setCharacteristic(ec.characteristic);

			// add variables in fields 7 and up
			addVariables(pp, ec.vars);
		}

		// next place all arcs
		for(int i = 0; i < cc.arcs.length; i++)
		{
			ArcContents ac = cc.arcs[i];
			showProgress(ac.lineNumber);

			String protoName = ac.apRef.techRef.getName() + ":" + ac.apRef.getName();
			ArcProto ap = ArcProto.findArcProto(protoName);
			if (ap == null)
			{
				logError("cannot find arc " + protoName);
				continue;
			}
			PortInst headPI = figureOutPortInst(cell, ac.headPort, ac.headNode, ac.headX, ac.headY, cc.getFileName(), ac.lineNumber);
			if (headPI == null) continue;

			PortInst tailPI = figureOutPortInst(cell, ac.tailPort, ac.tailNode, ac.tailX, ac.tailY, cc.getFileName(), ac.lineNumber);
			if (tailPI == null) continue;

			ArcInst ai = ArcInst.newInstance(ap, ac.width, headPI, tailPI, new Point2D.Double(ac.headX, ac.headY),
				new Point2D.Double(ac.tailX, ac.tailY), ac.getName(), ac.angle);
			if (ai == null)
			{
				ErrorLogger.MessageLog log = logError("cannot create arc " + protoName);
                log.addGeom(headPI.getNodeInst(), true, cell, null);
                log.addGeom(tailPI.getNodeInst(), true, cell, null);
				continue;
			}

			// get the ard name text descriptor
			loadTextDescriptor(ai.getNameTextDescriptor(), null, ac.textDescriptor);

			// add state bits
			ai.setRigid(ac.rigid);
			ai.setFixedAngle(ac.fixedAngle);
			ai.setSlidable(ac.slidable);
			ai.setExtended(ac.extended);
			ai.setDirectional(ac.directional);
			ai.setReverseEnds(ac.reverseEnds);
			ai.setHardSelect(ac.hardSelect);
			ai.setSkipHead(ac.skipHead);
			ai.setSkipTail(ac.skipTail);
			ai.getHead().setNegated(ac.headNegated);
			ai.getTail().setNegated(ac.tailNegated);

			// add variables in fields 13 and up
			addVariables(ai, ac.vars);
		}
		cc.filledIn = true;
		cc.nodes = null;
		cc.arcs = null;
	}

	/**
	 * Method to find the proper PortInst for a specified port on a node, at a given position.
	 * @param cell the cell in which this all resides.
	 * @param portName the name of the port (may be an empty string if there is only 1 port).
	 * @param nodeName the name of the node.
	 * @param xPos the X coordinate of the port on the node.
	 * @param yPos the Y coordinate of the port on the node.
	 * @param diskName a HashMap that maps node names to actual nodes.
	 * @param lineNumber the line number in the file being read (for error reporting).
	 * @return the PortInst specified (null if none can be found).
	 */
	private PortInst figureOutPortInst(Cell cell, String portName, NodeContents node, double xPos, double yPos, String fileName, int lineNumber)
	{
		NodeInst ni = node.ni;
		if (ni == null)
		{
			Input.errorLogger.logError("cannot find node " + node.getName(), cell, -1);
			return null;
		}

		PortInst pi = null;
		if (portName.length() == 0)
		{
			if (ni.getNumPortInsts() > 0)
				pi = ni.getPortInst(0);
		} else
		{
			pi = ni.findPortInst(portName);
		}

		// primitives use the name match
		NodeProto np = ni.getProto();
		if (np instanceof PrimitiveNode) return pi;

		// make sure the port can handle the position
		Point2D headPt = new Point2D.Double(xPos, yPos);
		if (pi != null)
		{
			Poly poly = pi.getPoly();
			if (!poly.isInside(headPt)) pi = null;
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
			ErrorLogger.MessageLog log = logError("Arc end and port discrepancy at ("+headPt.getX()+","+headPt.getY()+"), port "+portName+" on node "+node.getName()+" in cell " + cell.describe());
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
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				", Unable to create export " + name + " on dummy cell " + subCell.describe() + " (cannot create source node)", cell, -1);
			return null;
		}
		PortInst portPI = portNI.getOnlyPortInst();
		Export pp = Export.newInstance(subCell, portPI, name, false);
		if (pp == null)
		{
			Input.errorLogger.logError(fileName + ", line " + lineNumber +
				", Unable to create export " + name + " on dummy cell " + subCell.describe(), cell, -1);
			return null;
		}
		pi = ni.findPortInstFromProto(pp);
		Input.errorLogger.logError(fileName + ", line " + lineNumber +
			", Creating export " + name + " on dummy cell " + subCell.describe(), cell, -1);

		return pi;
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
		if (eObj == null || vars == null) return;
		for(int i = 0; i < vars.length; i++)
		{
			String piece = vars[i].piece;
			int openPos = 0;
			for(; openPos < piece.length(); openPos++)
			{
				char chr = piece.charAt(openPos);
				if (chr == '^') { openPos++;   continue; }
				if (chr == '(') break;
			}
			if (openPos >= piece.length())
			{
				logError("Badly formed variable (no open parenthesis): " + piece);
				continue;
			}
			String varName = piece.substring(0, openPos);
			Variable.Key varKey = ElectricObject.newKey(varName);
			if (eObj.isDeprecatedVariable(varKey)) continue;
			int closePos = piece.indexOf(')', openPos);
			if (closePos < 0)
			{
				logError("Badly formed variable (no close parenthesis): " + piece);
				continue;
			}
			String varBits = piece.substring(openPos+1, closePos);
			int objectPos = closePos + 1;
			if (objectPos >= piece.length())
			{
				logError("Variable type missing: " + piece);
				continue;
			}
			char varType = piece.charAt(objectPos++);
			switch (varType)
			{
				case 'B':
				case 'C':
				case 'D':
				case 'E':
				case 'F':
				case 'G':
				case 'H':
				case 'I':
				case 'L':
				case 'O':
				case 'P':
				case 'R':
				case 'S':
				case 'T':
				case 'V':
				case 'Y':
					break; // break from switch
				default:
					logError("Variable type invalid: " + piece);
					continue; // continue loop
			}
			if (objectPos >= piece.length())
			{
				logError("Variable value missing: " + piece);
				continue;
			}
			Object obj = null;
			if (piece.charAt(objectPos) == '[')
			{
				List objList = new ArrayList();
				objectPos++;
				while (objectPos < piece.length())
				{
					int start = objectPos;
					boolean inQuote = false;
					while (objectPos < piece.length())
					{
						if (inQuote)
						{
							if (piece.charAt(objectPos) == '^')
							{
								objectPos++;
							} else if (piece.charAt(objectPos) == '"')
							{
								inQuote = false;
							}
							objectPos++;
							continue;
						}
						if (piece.charAt(objectPos) == ',' || piece.charAt(objectPos) == ']') break;
						if (piece.charAt(objectPos) == '"')
						{
							inQuote = true;
						}
						objectPos++;
					}
					Object oneObj = getVariableValue(piece.substring(start, objectPos), 0, varType);
					objList.add(oneObj);
					if (piece.charAt(objectPos) == ']') break;
					objectPos++;
				}
				if (objectPos >= piece.length())
				{
					logError("Badly formed array (no closed bracket): " + piece);
					continue;
				}
				else if (objectPos < piece.length() - 1)
				{
					logError("Badly formed array (extra characters after closed bracket): " + piece);
					continue;
				}
				int limit = objList.size();
				Object [] objArray = null;
				switch (varType)
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
					objArray[j] = objList.get(j);
				obj = objArray;
			} else
			{
				// a scalar Variable
				obj = getVariableValue(piece, objectPos, varType);
			}

			// create the variable
			Variable newVar = eObj.newVar(varKey, obj);
			if (newVar == null)
			{
				logError("Cannot create variable: " + piece);
				continue;
			}

			// add in extra information
			TextDescriptor td = newVar.getTextDescriptor();
			loadTextDescriptor(td, newVar, varBits);
		}
	}

	/**
	 * Method to add meaning preferences to an ElectricObject from a List of strings.
	 * @param obj the Object to augment with meaning preferences.
	 * @param vars the array of VariableContents objects that described the Object.
	 */
	void addMeaningPrefs(Object obj, VariableContents[] vars)
	{
		if (obj == null || vars == null) return;
		for(int i = 0; i < vars.length; i++)
		{
			String piece = vars[i].piece;
			int openPos = 0;
			for(; openPos < piece.length(); openPos++)
			{
				char chr = piece.charAt(openPos);
				if (chr == '^') { openPos++;   continue; }
				if (chr == '(') break;
			}
			if (openPos >= piece.length())
			{
				logError("Badly formed meaning preference (no open parenthesis): " + piece);
				continue;
			}
			String varName = piece.substring(0, openPos);
			int closePos = piece.indexOf(')', openPos);
			if (closePos < 0)
			{
				logError("Badly formed meaning preference (no close parenthesis): " + piece);
				continue;
			}
			int objectPos = closePos + 1;
			if (objectPos >= piece.length())
			{
				logError("Meaning preference type missing: " + piece);
				continue;
			}
			char varType = piece.charAt(objectPos++);
			switch (varType)
			{
				case 'D': // Double
				case 'F': // Float
				case 'G': // Long
				case 'I': // Integer
				case 'S': // String
					break; // break from switch
				default:
					logError("Meaning preference type invalid: " + piece);
					continue; // continue loop
			}
			if (objectPos >= piece.length())
			{
				logError("Meaning preference value missing: " + piece);
				continue;
			}
			if (piece.charAt(objectPos) == '[')
			{
				logError("Meaning preference has array value: " + piece);
				continue;
			}
			// a scalar Variable
			Object value = getVariableValue(piece, objectPos, varType);

			// change "meaning option"
			Pref.Meaning meaning = Pref.getMeaningVariable(obj, varName);
			if (meaning != null)
			{
				Pref.changedMeaningVariable(meaning, value);
			} else if (!(obj instanceof Technology && ((Technology)obj).convertOldVariable(varName, value)))
			{
// 				logError("Meaning preference unknown: " + piece);
			}
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
	private Object getVariableValue(String piece, int objectPos, char varType)
	{
		int colonPos;
		String libName;
		Library lib;
		int secondColonPos;
		String cellName;
		Cell cell;
		int commaPos;

		switch (varType)
		{
// 			case 'A':		// ArcInst (should delay analysis until database is built!!!)
// 				int colonPos = piece.indexOf(':', objectPos);
// 				if (colonPos < 0)
// 				{
// 					logError("Badly formed Export (missing library colon): " + piece);
// 					break;
// 				}
// 				String libName = piece.substring(objectPos, colonPos);
// 				Library lib = Library.findLibrary(libName);
// 				if (lib == null)
// 				{
// 					logError("Unknown library: " + libName);
// 					break;
// 				}
// 				int secondColonPos = piece.indexOf(':', colonPos+1);
// 				if (secondColonPos < 0)
// 				{
// 					logError("Badly formed Export (missing cell colon): " + piece);
// 					break;
// 				}
// 				String cellName = piece.substring(colonPos+1, secondColonPos);
// 				Cell cell = lib.findNodeProto(cellName);
// 				if (cell == null)
// 				{
// 					logError("Unknown Cell: " + piece);
// 					break;
// 				}
// 				String arcName = piece.substring(secondColonPos+1);
// 				int commaPos = arcName.indexOf(',');
// 				if (commaPos >= 0) arcName = arcName.substring(0, commaPos);
// 				ArcInst ai = cell.findArc(arcName);
// 				if (ai == null)
// 					logError("Unknown ArcInst: " + piece);
// 				return ai;
			case 'B':		// Boolean
				return new Boolean(piece.charAt(objectPos)=='T' ? true : false);
			case 'C':		// Cell (should delay analysis until database is built!!!)
				colonPos = piece.indexOf(':', objectPos);
				if (colonPos < 0)
				{
					logError("Badly formed ArcProto (missing colon): " + piece);
					break;
				}
				libName = piece.substring(objectPos, colonPos);
				lib = Library.findLibrary(libName);
				if (lib == null)
				{
					logError("Unknown library: " + libName);
					break;
				}
				cellName = piece.substring(colonPos+1);
				commaPos = cellName.indexOf(',');
				if (commaPos >= 0) cellName = cellName.substring(0, commaPos);
				cell = lib.findNodeProto(cellName);
				if (cell == null)
					logError("Unknown Cell: " + piece);
				return cell;
			case 'D':		// Double
				return new Double(TextUtils.atof(piece.substring(objectPos)));
			case 'E':		// Export (should delay analysis until database is built!!!)
				colonPos = piece.indexOf(':', objectPos);
				if (colonPos < 0)
				{
					logError("Badly formed Export (missing library colon): " + piece);
					break;
				}
				libName = piece.substring(objectPos, colonPos);
				lib = Library.findLibrary(libName);
				if (lib == null)
				{
					logError("Unknown library: " + libName);
					break;
				}
				secondColonPos = piece.indexOf(':', colonPos+1);
				if (secondColonPos < 0)
				{
					logError("Badly formed Export (missing cell colon): " + piece);
					break;
				}
				cellName = piece.substring(colonPos+1, secondColonPos);
				cell = lib.findNodeProto(cellName);
				if (cell == null)
				{
					logError("Unknown Cell: " + piece);
					break;
				}
				String exportName = piece.substring(secondColonPos+1);
				commaPos = exportName.indexOf(',');
				if (commaPos >= 0) exportName = exportName.substring(0, commaPos);
				Export pp = cell.findExport(exportName);
				if (pp == null)
					logError("Unknown Export: " + piece);
				return pp;
			case 'F':		// Float
				return new Float((float)TextUtils.atof(piece.substring(objectPos)));
			case 'G':		// Long
				return new Long(TextUtils.atoi(piece.substring(objectPos)));
			case 'H':		// Short
				return new Short((short)TextUtils.atoi(piece.substring(objectPos)));
			case 'I':		// Integer
				return new Integer(TextUtils.atoi(piece.substring(objectPos)));
			case 'L':		// Library (should delay analysis until database is built!!!)
				libName = piece.substring(objectPos);
				commaPos = libName.indexOf(',');
				if (commaPos >= 0) libName = libName.substring(0, commaPos);
				LibraryRef libRef = getLibraryRef(libName);
				return libRef.getLibrary();
// 			case 'N':		// NodeInst (should delay analysis until database is built!!!)
// 				colonPos = piece.indexOf(':', objectPos);
// 				if (colonPos < 0)
// 				{
// 					logError("Badly formed Export (missing library colon): " + piece);
// 					break;
// 				}
// 				libName = piece.substring(objectPos, colonPos);
// 				lib = Library.findLibrary(libName);
// 				if (lib == null)
// 				{
// 					logError("Unknown library: " + libName);
// 					break;
// 				}
// 				secondColonPos = piece.indexOf(':', colonPos+1);
// 				if (secondColonPos < 0)
// 				{
// 					logError("Badly formed Export (missing cell colon): " + piece);
// 					break;
// 				}
// 				cellName = piece.substring(colonPos+1, secondColonPos);
// 				cell = lib.findNodeProto(cellName);
// 				if (cell == null)
// 				{
// 					logError("Unknown Cell: " + piece);
// 					break;
// 				}
// 				String nodeName = piece.substring(secondColonPos+1);
// 				commaPos = nodeName.indexOf(',');
// 				if (commaPos >= 0) nodeName = nodeName.substring(0, commaPos);
// 				NodeInst ni = cell.findNode(nodeName);
// 				if (ni == null)
// 					logError("Unknown NodeInst: " + piece);
// 				return ni;
			case 'O':		// Tool
				String toolName = piece.substring(objectPos);
				commaPos = toolName.indexOf(',');
				if (commaPos >= 0) toolName = toolName.substring(0, commaPos);
				Tool tool = Tool.findTool(toolName);
				if (tool == null)
					logError("Unknown Tool: " + piece);
				return tool;
			case 'P':		// PrimitiveNode
				String pnName = piece.substring(objectPos);
				commaPos = pnName.indexOf(',');
				if (commaPos >= 0) pnName = pnName.substring(0, commaPos);
				PrimitiveNodeRef pnRef = getPrimitiveNodeRef(null, pnName);
				return pnRef.getNodeProto();
			case 'R':		// ArcProto
				String apName = piece.substring(objectPos);
				commaPos = apName.indexOf(',');
				if (commaPos >= 0) apName = apName.substring(0, commaPos);
				ArcProtoRef apRef = getArcProtoRef(null, apName);
				return apRef.getArcProto();
			case 'S':		// String
				if (piece.charAt(objectPos) != '"')
				{
					logError("Badly formed string variable (missing open quote): " + piece);
					break;
				}
				StringBuffer sb = new StringBuffer();
				int len = piece.length();
				while (objectPos < len)
				{
					objectPos++;
					if (piece.charAt(objectPos) == '"') break;
					if (piece.charAt(objectPos) == '^')
					{
						objectPos++;
						if (objectPos <= len - 2 && piece.charAt(objectPos) == '\\' && piece.charAt(objectPos+1) == 'n')
						{
							sb.append('\n');
							objectPos++;
							continue;
						}
					}
					sb.append(piece.charAt(objectPos));
				}
				return sb.toString();
			case 'T':		// Technology
				String techName = piece.substring(objectPos);
				commaPos = techName.indexOf(',');
				if (commaPos >= 0) techName = techName.substring(0, commaPos);
				TechnologyRef techRef = getTechnologyRef(techName);
				return techRef.getTechnology();
			case 'V':		// Point2D
				double x = TextUtils.atof(piece.substring(objectPos));
				int slashPos = piece.indexOf('/', objectPos);
				if (slashPos < 0)
				{
					logError("Badly formed Point2D variable (missing slash): " + piece);
					break;
				}
				double y = TextUtils.atof(piece.substring(slashPos+1));
				return new Point2D.Double(x, y);
			case 'Y':		// Byte
				return new Byte((byte)TextUtils.atoi(piece.substring(objectPos)));
		}
		return null;
	}
}
