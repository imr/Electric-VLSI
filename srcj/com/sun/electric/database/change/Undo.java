/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Undo.java
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
package com.sun.electric.database.change;

import com.sun.electric.database.constraint.Constraints;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.NodeUsage;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.ui.EditWindow;

import javax.swing.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * This interface defines changes that are made to the database.
 */
public class Undo
{

	/**
	 * Type is a typesafe enum class that describes the nature of a change.
	 */
	public static class Type
	{
		private final String name;

		private Type(String name) { this.name = name; }

		/**
		 * Returns a printable version of this Type.
		 * @return a printable version of this Type.
		 */
		public String toString() { return name; }

		/** Describes a newly-created NodeInst. */							public static final Type NODEINSTNEW = new Type("NodeInstNew");
		/** Describes a deleted NodeInst. */								public static final Type NODEINSTKILL = new Type("NodeInstKill");
		/** Describes a changed NodeInst. */								public static final Type NODEINSTMOD = new Type("NodeInstMod");
		/** Describes a newly-created ArcInst. */							public static final Type ARCINSTNEW = new Type("ArcInstNew");
		/** Describes a deleted ArcInst. */									public static final Type ARCINSTKILL = new Type("ArcInstKill");
		/** Describes a changed ArcInst. */									public static final Type ARCINSTMOD = new Type("ArcInstMod");
		/** Describes a newly-created Export. */							public static final Type EXPORTNEW = new Type("ExportNew");
		/** Describes a deleted Export. */									public static final Type EXPORTKILL = new Type("ExportKill");
		/** Describes a changed Export. */									public static final Type EXPORTMOD = new Type("ExportMod");
		/** Describes a newly-created Cell. */								public static final Type CELLNEW = new Type("CellNew");
		/** Describes a deleted Cell. */									public static final Type CELLKILL = new Type("CellKill");
		/** Describes a changed Cell. */									public static final Type CELLMOD = new Type("CellMod");
		/** Describes the creation of an arbitrary object. */				public static final Type OBJECTNEW = new Type("ObjectNew");
		/** Describes the deletion of an arbitrary object. */				public static final Type OBJECTKILL = new Type("ObjectKill");
		/** Describes the renaming of an arbitrary object. */				public static final Type OBJECTRENAME = new Type("ObjectRename");
		/** Describes the redrawing of an arbitrary object. */				public static final Type OBJECTREDRAW = new Type("ObjectRedraw");
		/** Describes the creation of a Variable on an object. */			public static final Type VARIABLENEW = new Type("VariableNew");
		/** Describes the deletion of a Variable on an object. */			public static final Type VARIABLEKILL = new Type("VariableKill");
		/** Describes the modification of a Variable on an object. */		public static final Type VARIABLEMODFLAGS = new Type("VariableModFlags");
		/** Describes the modification of a Variable on an object. */		public static final Type VARIABLEMOD = new Type("VariableMod");
		/** Describes the insertion of an entry in an arrayed Variable. */	public static final Type VARIABLEINSERT = new Type("VariableInsert");
		/** Describes the deletion of an entry in an arrayed Variable. */	public static final Type VARIABLEDELETE = new Type("VariableDelete");
		/** Describes the change to a TextDescriptor. */					public static final Type DESCRIPTORMOD = new Type("DescriptMod");
        /** Describes a new library change */                               public static final Type LIBRARYNEW = new Type("LibraryNew");
        /** Describes a delete library change */                            public static final Type LIBRARYKILL = new Type("LibraryKill");
	}

	/**
	 * The Change class describes a single change to the Electric database.
	 * These objects are used to undo and redo changes.
	 */
	public static class Change
	{
		private ElectricObject obj;
		private Type type;
		private double a1, a2, a3, a4, a5;
		private int i1, i2, i3;
		private Object o1, o2;

		Change(ElectricObject obj, Type type)
		{
			this.obj = obj;
			this.type = type;
		}
		private void setDoubles(double a1, double a2, double a3, double a4)
		{
			this.a1 = a1;
			this.a2 = a2;
			this.a3 = a3;
			this.a4 = a4;
		}

		/**
		 * Method to get the ElectricObject associated with this Change.
		 * @return the ElectricObject associated with this Change.
		 */
		public ElectricObject getObject() { return obj; }
		/**
		 * Method to get the type of this Change.
		 * @return the type of this Change.
		 */
		public Type getType() { return type; }
		/**
		 * Method to set the type of this Change.
		 * @param type the new type of this Change.
		 */
		private void setType(Type type) { this.type = type; }
		/**
		 * Method to get the first floating-point value associated with this Change.
		 * @return the first floating-point value associated with this Change.
		 */
		public double getA1() { return a1; }
		/**
		 * Method to get the second floating-point value associated with this Change.
		 * @return the second floating-point value associated with this Change.
		 */
		public double getA2() { return a2; }
		/**
		 * Method to get the third floating-point value associated with this Change.
		 * @return the third floating-point value associated with this Change.
		 */
		public double getA3() { return a3; }
		/**
		 * Method to get the fourth floating-point value associated with this Change.
		 * @return the fourth floating-point value associated with this Change.
		 */
		public double getA4() { return a4; }
		/**
		 * Method to get the fifth floating-point value associated with this Change.
		 * @return the fifth floating-point value associated with this Change.
		 */
		public double getA5() { return a5; }
		/**
		 * Method to get the first integer value associated with this Change.
		 * @return the first integer value associated with this Change.
		 */
		public int getI1() { return i1; }
		/**
		 * Method to get the second integer value associated with this Change.
		 * @return the second integer value associated with this Change.
		 */
		public int getI2() { return i2; }
		/**
		 * Method to get the third integer value associated with this Change.
		 * @return the third integer value associated with this Change.
		 */
		public int getI3() { return i3; }
		/**
		 * Method to get the first Object associated with this Change.
		 * @return the first Object associated with this Change.
		 */
		public Object getO1() { return o1; }
		/**
		 * Method to get the second Object associated with this Change.
		 * @return the second Object associated with this Change.
		 */
		public Object getO2() { return o2; }

		/**
		 * Method to broadcast a change to all tools that are on.
		 * @param firstchange true if this is the first change of a batch, so that a "startbatch" change must also be broadcast.
		 * @param undoRedo true if this is an undo/redo batch.
		 */
		private void broadcast(boolean firstchange, boolean undoRedo)
		{
			// start the batch if this is the first change
			broadcasting = type;
			if (firstchange)
			{
				// broadcast a start-batch on the first change
				for(Iterator it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.startBatch(listener, undoRedo);
				}
			}
			if (type == Type.NODEINSTNEW || type == Type.ARCINSTNEW || type == Type.EXPORTNEW ||
				type == Type.CELLNEW || type == Type.OBJECTNEW)
			{
				for(Iterator it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.newObject(obj);
				}
			} else if (type == Type.NODEINSTKILL || type == Type.ARCINSTKILL ||
				type == Type.CELLKILL || type == Type.OBJECTKILL)
			{
				for(Iterator it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.killObject(obj);
				}
			} else if (type == Type.EXPORTKILL) {
				for(Iterator it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.killExport((Export)obj, (Collection)o1);
				}
			} else if (type == Type.OBJECTRENAME) {
				for(Iterator it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.renameObject(obj, (Name)o1);
				}
			} else if (type == Type.OBJECTREDRAW)
			{
				for(Iterator it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.redrawObject(obj);
				}
			} else if (type == Type.NODEINSTMOD)
			{
				for(Iterator it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.modifyNodeInst((NodeInst)obj, a1, a2, a3, a4, i1);
				}
			} else if (type == Type.ARCINSTMOD)
			{
				for(Iterator it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.modifyArcInst((ArcInst)obj, a1, a2, a3, a4, a5);
				}
			} else if (type == Type.EXPORTMOD)
			{
				for(Iterator it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.modifyExport((Export)obj, (PortInst)o1);
				}
			} else if (type == Type.CELLMOD)
			{
				for(Iterator it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.modifyCell((Cell)obj, a1, a2, a3, a4);
				}
			} else if (type == Type.VARIABLENEW)
			{
				for(Iterator it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.newVariable(obj, (Variable)o1);
				}
			} else if (type == Type.VARIABLEKILL)
			{
				for(Iterator it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.killVariable(obj, (Variable)o1);
				}
			} else if (type == Type.VARIABLEMODFLAGS)
			{
				for(Iterator it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.modifyVariableFlags(obj, (Variable)o1, i1);
				}
			} else if (type == Type.VARIABLEMOD)
			{
				for(Iterator it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.modifyVariable(obj, (Variable)o1, i1, o2);
				}
			} else if (type == Type.VARIABLEINSERT)
			{
				for(Iterator it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.insertVariable(obj, (Variable)o1, i1);
				}
			} else if (type == Type.VARIABLEDELETE)
			{
				for(Iterator it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.deleteVariable(obj, (Variable)o1, i1, o2);
				}
			} else if (type == Type.DESCRIPTORMOD)
			{
				for(Iterator it = Tool.getListeners(); it.hasNext(); )
				{
					Listener listener = (Listener)it.next();
					listener.modifyTextDescript(obj, (TextDescriptor)o1, i1, i2, i3);
				}
			}
			broadcasting = null;
		}

		/**
		 * Method to undo the effects of this change.
		 */
		private void reverse()
		{
			// determine what needs to be marked as changed
			setDirty(type, obj, o1);

			if (type == Type.NODEINSTNEW)
			{
				NodeInst ni = (NodeInst)obj;
				ni.lowLevelUnlink();
				type = Type.NODEINSTKILL;
				return;
			}
			if (type == Type.NODEINSTKILL)
			{
				NodeInst ni = (NodeInst)obj;
				ni.lowLevelLink();
				type = Type.NODEINSTNEW;
				return;
			}
			if (type == Type.NODEINSTMOD)
			{
				// get information about the node as it is now
				NodeInst ni = (NodeInst)obj;
				double oldCX = ni.getAnchorCenterX();
				double oldCY = ni.getAnchorCenterY();
				double oldSX = ni.getXSizeWithMirror();
				double oldSY = ni.getYSizeWithMirror();
				int oldRot = ni.getAngle();

				// change the node information
				ni.lowLevelModify(a1 - oldCX, a2 - oldCY, a3 - oldSX, a4 - oldSY, i1 - oldRot);

				// update the change to its reversed state
				a1 = oldCX;   a2 = oldCY;
				a3 = oldSX;   a4 = oldSY;
				i1 = oldRot;
				return;
			}
			if (type == Type.ARCINSTNEW)
			{
				ArcInst ai = (ArcInst)obj;
				ai.lowLevelUnlink();
				type = Type.ARCINSTKILL;
				return;
			}
			if (type == Type.ARCINSTKILL)
			{
				ArcInst ai = (ArcInst)obj;
				ai.lowLevelLink();
				type = Type.ARCINSTNEW;
				return;
			}
			if (type == Type.ARCINSTMOD)
			{
				// get information about the arc as it is now
				ArcInst ai = (ArcInst)obj;
				Point2D oldHeadPt = new Point2D.Double();
				oldHeadPt.setLocation(ai.getHead().getLocation());
				Point2D oldTailPt = new Point2D.Double();
				oldTailPt.setLocation(ai.getTail().getLocation());
				double oldWid = ai.getWidth();

				// change the arc information
				ai.lowLevelModify(a5 - oldWid, a1-oldHeadPt.getX(), a2-oldHeadPt.getY(), a3-oldTailPt.getX(), a4-oldTailPt.getY());

				// update the change to its reversed state
				a1 = oldHeadPt.getX();   a2 = oldHeadPt.getY();
				a3 = oldTailPt.getX();   a4 = oldTailPt.getY();
				a5 = oldWid;
				return;
			}
			if (type == Type.EXPORTNEW)
			{
				Export pp = (Export)obj;
				o1 = pp.lowLevelUnlink();
				type = Type.EXPORTKILL;
				return;
			}
			if (type == Type.EXPORTKILL)
			{
				Export pp = (Export)obj;
				pp.lowLevelLink((Collection)o1);
				type = Type.EXPORTNEW;
				o1 = null;
				return;
			}
			if (type == Type.EXPORTMOD)
			{
				Export pp = (Export)obj;
				PortInst oldPi = (PortInst)o1;
				PortInst currentPi = pp.getOriginalPort();
				pp.lowLevelModify(oldPi);
				o1 = currentPi;
				return;
			}
			if (type == Type.CELLNEW)
			{
				Cell cell = (Cell)obj;
				cell.lowLevelUnlink();
				type = Type.CELLKILL;
				return;
			}
			if (type == Type.CELLKILL)
			{
				Cell cell = (Cell)obj;
				cell.lowLevelLink();
				type = Type.CELLNEW;
				return;
			}
			if (type == Type.CELLMOD)
			{
				Cell cell = (Cell)obj;
				Rectangle2D bounds = cell.getBounds();
				a1 = bounds.getMinX();   a2 = bounds.getMaxX();
				a3 = bounds.getMinY();   a4 = bounds.getMaxY();
				return;
			}
			if (type == Type.OBJECTNEW)
			{
				// args: addr, type
//				switch (c->p1&VTYPE)
//				{
//					case VVIEW:
//						// find the view
//						view = (VIEW *)c->entryaddr;
//						lastv = NOVIEW;
//						for(v = el_views; v != NOVIEW; v = v->nextview)
//						{
//							if (v == view) break;
//							lastv = v;
//						}
//						if (v != NOVIEW)
//						{
//							// delete the view
//							if (lastv == NOVIEW) el_views = v->nextview; else
//								lastv->nextview = v->nextview;
//						}
//						break;
//				}
				type = Type.OBJECTKILL;
				return;
			}
			if (type == Type.OBJECTKILL)
			{
				// args: addr, type
//				switch (c->p1&VTYPE)
//				{
//					case VVIEW:
//						view = (VIEW *)c->entryaddr;
//						view->nextview = el_views;
//						el_views = view;
//						break;
//				}
				type = Type.OBJECTNEW;
				return;
			}
			if (type == Type.OBJECTRENAME)
			{
				if (obj instanceof Geometric)
				{
					Geometric geom = (Geometric)obj;
					Name oldName = (Name)o1;
					o1 = geom.getNameKey();
					geom.lowLevelSetNameKey(oldName);
				} else if (obj instanceof Cell)
				{
					Cell cell = (Cell)obj;
					Name oldName = (Name)o1;
					o1 = Name.findName(cell.getName());
					cell.lowLevelRename(oldName.toString());
				} else if (obj instanceof Export)
				{
					Export pp = (Export)obj;
					Name oldName = (Name)o1;
					o1 = pp.getNameKey();
					pp.lowLevelRename(oldName.toString());
				} else if (obj instanceof Library)
				{
					Library lib = (Library)obj;
					Name oldName = (Name)o1;
					o1 = Name.findName(lib.getName());
					lib.lowLevelRename(oldName.toString());
				}
				return;
			}
			if (type == Type.VARIABLENEW)
			{
				Variable var = (Variable)o1;
				obj.lowLevelUnlinkVar(var);
				type = Type.VARIABLEKILL;
				return;
			}
			if (type == Type.VARIABLEKILL)
			{
				Variable var = (Variable)o1;
				obj.lowLevelLinkVar(var);
				type = Type.VARIABLENEW;
				return;
			}
			if (type == Type.VARIABLEMODFLAGS)
			{
				Variable var = (Variable)o1;
				int oldFlags = var.lowLevelGetFlags();
				var.lowLevelSetFlags(i1);
				i1 = oldFlags;
				return;
			}
			if (type == Type.VARIABLEMOD)
			{
				Variable var = (Variable)o1;
				Object[] arr = (Object[])var.getObject();
				Object oldVal = arr[i1];
				arr[i1] = o2;
				o2 = oldVal;
				return;
			}
			if (type == Type.VARIABLEINSERT)
			{
				Variable var = (Variable)o1;
				Object[] oldArr = (Object[])var.getObject();
				o2 = oldArr[i1];
				var.lowLevelDelete(i1);
				type = Type.VARIABLEDELETE;
				return;
			}
			if (type == Type.VARIABLEDELETE)
			{
				Variable var = (Variable)o1;
				var.lowLevelInsert(i1, o2);
				type = Type.VARIABLEINSERT;
				return;
			}
			if (type == Type.DESCRIPTORMOD)
			{
				TextDescriptor descript = (TextDescriptor)o1;
				int oldDescript0 = descript.lowLevelGet0();
				int oldDescript1 = descript.lowLevelGet1();
				int oldColorIndex = descript.getColorIndex();
				descript.lowLevelSet(i1, i2);
				descript.lowLevelSetColorIndex(i3);
				i1 = oldDescript0;
				i2 = oldDescript1;
				i3 = oldColorIndex;
				return;
			}
		}

		/**
		 * Method to examine a change and mark the appropriate libraries and cells as "dirty".
		 * @param type the type of change being made.
		 * @param obj the object to which the change is applied.
		 */
		private static void setDirty(Type type, ElectricObject obj, Object o1)
		{
			Cell cell = null;
			Library lib = null;
			boolean major = false;
			if (type == Type.NODEINSTNEW || type == Type.NODEINSTKILL || type == Type.NODEINSTMOD)
			{
				NodeInst ni = (NodeInst)obj;
				cell = ni.getParent();
				lib = cell.getLibrary();
				major = true;
			} else if (type == Type.ARCINSTNEW || type == Type.ARCINSTKILL || type == Type.ARCINSTMOD)
			{
				ArcInst ai = (ArcInst)obj;
				cell = ai.getParent();
				lib = cell.getLibrary();
				major = true;
			} else if (type == Type.EXPORTNEW || type == Type.EXPORTKILL || type == Type.EXPORTMOD)
			{
				Export pp = (Export)obj;
				cell = (Cell)pp.getParent();
				lib = cell.getLibrary();
				major = true;
			} else if (type == Type.CELLNEW || type == Type.CELLKILL)
			{
				cell = (Cell)obj;
				lib = cell.getLibrary();
				major = true;
			} else if (type == Type.CELLMOD)
			{
				cell = (Cell)obj;
				lib = cell.getLibrary();
			} else if (type == Type.OBJECTNEW || type == Type.OBJECTKILL || type == Type.OBJECTREDRAW)
			{
				cell = obj.whichCell();
				if (cell != null) lib = cell.getLibrary();
            } else if (type == Type.OBJECTRENAME) {
                cell = obj.whichCell();
                if (cell != null) {
                    lib = cell.getLibrary();
                    // also mark libraries that reference this cell as dirty
                    for (Iterator it = cell.getUsagesOf(); it.hasNext(); ) {
                        NodeUsage nu = (NodeUsage)it.next();
                        Cell parent = nu.getParent();
                        parent.getLibrary().setChangedMinor();
                    }
                }
			} else if (type == Type.VARIABLENEW || type == Type.VARIABLEKILL || type == Type.VARIABLEMOD ||
				type == Type.VARIABLEINSERT || type == Type.VARIABLEDELETE)
			{
				cell = obj.whichCell();
				if (cell != null) lib = cell.getLibrary();
				Variable var = (Variable)o1;
				major = isMajorVariable(obj, var.getKey());
			} else if (type == Type.VARIABLEMODFLAGS || type == Type.DESCRIPTORMOD)
			{
				cell = obj.whichCell();
				if (cell != null) lib = cell.getLibrary();
			}

			// set "changed" and "dirty" bits
			if (cell != null)
			{
				if (major) cell.madeRevision();
				if (!ChangeCell.contains(cell))
				{
					ChangeCell.add(cell);
				}
			}
			if (lib != null)
			{
				if (major) lib.setChangedMajor(); else
					lib.setChangedMinor();
			}
		}

		private static boolean isMajorVariable(ElectricObject obj, Variable.Key key)
		{
// 			if ((obj instanceof Cell) && key == el_cell_message_key) return true;
// 			if ((obj instanceof NodeInst) && key == sim_weaknodekey) return true;
			return false;
		}

        public String toString() { return describe(); }

		/**
		 * Method to describe this change as a string.
		 */
		private String describe()
		{
			if (type == Type.NODEINSTNEW)
			{
				NodeInst ni = (NodeInst)obj;
				return "Node " + ni.describe() + " created in cell " + ni.getParent().describe();
			}
			if (type == Type.NODEINSTKILL)
			{
				NodeInst ni = (NodeInst)obj;
				return "Node " + ni.describe() + " deleted from cell " + ni.getParent().describe();
			}
			if (type == Type.NODEINSTMOD)
			{
				NodeInst ni = (NodeInst)obj;
				return "Node " + ni.describe() + " modified in cell " + ni.getParent().describe() +
					"[was " + getA3() + "x" + getA4() + " at (" + getA1() + "," + getA2() + ") rotated " + getI1()/10.0 + "]";
			}
			if (type == Type.ARCINSTNEW)
			{
				ArcInst ai = (ArcInst)obj;
				return "Arc " + ai.describe() + " created in cell " + ai.getParent().describe();
			}
			if (type == Type.ARCINSTKILL)
			{
				ArcInst ai = (ArcInst)obj;
				return "Arc " + ai.describe() + " deleted from cell " + ai.getParent().describe();
			}
			if (type == Type.ARCINSTMOD)
			{
				ArcInst ai = (ArcInst)obj;
				return "Arc " + ai.describe() + " modified in cell " + ai.getParent().describe() +
					"[was " + getA5() + " wide from (" + getA1() + "," + getA2() + ") to (" + getA3() + "," + getA4() + ")]";
			}
			if (type == Type.EXPORTNEW)
			{
				Export pp = (Export)obj;
				return "Export " + pp.getName() + " created in cell " + pp.getParent().describe();
			}
			if (type == Type.EXPORTKILL)
			{
				Export pp = (Export)obj;
				return "Export " + pp.getName() + " deleted from cell " + pp.getParent().describe();
			}
			if (type == Type.EXPORTMOD)
			{
				Export pp = (Export)obj;
				PortInst pi = (PortInst)o1;
				return "Export " + pp.getName() + " moved in cell " + pp.getParent().describe() +
					"[was on node " + pi.getNodeInst().describe() + " port " + pi.getPortProto().getName() + "]";
			}
			if (type == Type.CELLNEW)
			{
				Cell cell = (Cell)obj;
				return "Cell " + cell.describe() + " created";
			}
			if (type == Type.CELLKILL)
			{
				Cell cell = (Cell)obj;
				return "Cell " + cell.describe() + " deleted";
			}
			if (type == Type.CELLMOD)
			{
				Cell cell = (Cell)obj;
				return "Cell " + cell.describe() + " modified (was from " + a1 + "<=X<=" + a2 + " " + a3 + "<=Y<=" + a4 + ")";
			}
			if (type == Type.OBJECTNEW)
			{
				return "Created new object " + obj;
			}
			if (type == Type.OBJECTKILL)
			{
				return "Deleted object " + obj;
			}
			if (type == Type.OBJECTRENAME)
			{
				return "Renamed object " + obj + " (was " + (Name)o1 + ")";
			}
			if (type == Type.OBJECTREDRAW)
			{
				return "Redraw object " + obj;
			}
			if (type == Type.VARIABLENEW)
			{
				return "Created variable "+o1+" on "+obj;
			}
			if (type == Type.VARIABLEKILL)
			{
				return "Deleted variable "+o1+" on "+obj+" [was "+((Variable)o1).getObject()+"]";
			}
			if (type == Type.VARIABLEMODFLAGS)
			{
				return "Modified variable flags "+obj+" "+o1+" [was 0"+Integer.toOctalString(i1)+"]";
			}
			if (type == Type.VARIABLEMOD)
			{
				return "Modified variable "+o1+"["+i1+"] on "+obj;
			}
			if (type == Type.VARIABLEINSERT)
			{
				return "Inserted variable "+o1+" on "+obj;
			}
			if (type == Type.VARIABLEDELETE)
			{
				return "Deleted variable "+o1+" on "+obj;
			}
			if (type == Type.DESCRIPTORMOD)
			{
				return "Modified Text Descriptor in "+obj+" [was 0"+Integer.toOctalString(i1)+"/0"+Integer.toOctalString(i2)+"]";
			}
			return "?";
		}
	}

	/**
	 * Class to describe a batch of changes to the Electric database.
	 */
	public static class ChangeBatch
	{
		private List changes;
		private int batchNumber;
		private boolean done;
		private Tool tool;
		private String activity;
		private Cell upCell;
        private List startingHighlights = null;            // highlights before changes made
        private Point2D startHighlightsOffset = null;      // highlights offset before changes made
        private List preUndoHighlights = null;             // highlights before undo of changes done
        private Point2D preUndoHighlightsOffset = null;    // highlights offset before undo of changes done


		private ChangeBatch() {}
		
		private void add(Change change) { changes.add(change); }

		/**
		 * Method to return an iterator over all changes in this ChangeBatch.
		 * @return an iterator over all changes in this ChangeBatch.
		 */
		public Iterator getChanges() { return changes.iterator(); }

		/**
		 * Method to return the number of changes in this ChangeBatch.
		 * @return the number of changes in this ChangeBatch.
		 */
		public int getNumChanges() { return changes.size(); }

		private void describe()
		{
			/* display the change batches */
			int nodeInst = 0, arcInst = 0, export = 0, cell = 0, object = 0, variable = 0;
			int batchSize = changes.size();
			for(int j = 0; j<batchSize; j++)
			{
				Change ch = (Change)changes.get(j);
				if (ch.getType() == Type.NODEINSTNEW || ch.getType() == Type.NODEINSTKILL || ch.getType() == Type.NODEINSTMOD ||
					ch.getType() == Type.OBJECTRENAME && ch.obj instanceof NodeInst)
				{
					nodeInst++;
				} else if (ch.getType() == Type.ARCINSTNEW || ch.getType() == Type.ARCINSTKILL || ch.getType() == Type.ARCINSTMOD ||
					ch.getType() == Type.OBJECTRENAME && ch.obj instanceof NodeInst)
				{
					arcInst++;
				} else if (ch.getType() == Type.EXPORTNEW || ch.getType() == Type.EXPORTKILL || ch.getType() == Type.EXPORTMOD)
				{
					export++;
				} else if (ch.getType() == Type.CELLNEW || ch.getType() == Type.CELLKILL || ch.getType() == Type.CELLMOD)
				{
					cell++;
				} else if (ch.getType() == Type.OBJECTNEW || ch.getType() == Type.OBJECTKILL || ch.getType() == Type.OBJECTREDRAW)
				{
					object++;
				} else if (ch.getType() == Type.VARIABLENEW || ch.getType() == Type.VARIABLEKILL || ch.getType() == Type.VARIABLEMODFLAGS ||
					ch.getType() == Type.VARIABLEMOD || ch.getType() == Type.VARIABLEINSERT ||
					ch.getType() == Type.VARIABLEDELETE)
				{
					variable++;
				} else if (ch.getType() == Type.DESCRIPTORMOD)
				{
					TextDescriptor td = (TextDescriptor)ch.o1;
					if (ch.obj instanceof NodeInst)
					{
						NodeInst ni = (NodeInst)ch.obj;
						if (ni.getNameTextDescriptor() == td || ni.getProtoTextDescriptor() == td)
							nodeInst++;
						else
							variable++;
					} else if (ch.obj instanceof ArcInst)
					{
						ArcInst ai = (ArcInst)ch.obj;
						if (ai.getNameTextDescriptor() == td)
							arcInst++;
						else
							variable++;
					} else if (ch.obj instanceof Export)
					{
						Export e = (Export)ch.obj;
						if (e.getTextDescriptor() == td)
							export++;
						else
							variable++;
					} else
					{
						variable++;
					}
				}
			}

			String message = "*** Batch " + batchNumber + " (" + activity + ") has " + batchSize + " changes and affects";
			if (nodeInst != 0) message += " " + nodeInst + " nodes";
			if (arcInst != 0) message += " " + arcInst + " arcs";
			if (export != 0) message += " " + export + " exports";
			if (cell != 0) message += " " + cell + " cells";
			if (object != 0) message += " " + object + " objects";
			if (variable != 0) message += " " + variable + " variables";
			System.out.println(message + ":");
			for(int j = 0; j<batchSize; j++)
			{
				Change ch = (Change)changes.get(j);
				System.out.println("   " + ch.describe());
			}
		}
	}

	/**
	 * This method describes a Cell that changed as a result of changes to the database.
	 */
	public static class ChangeCell
	{
		private Cell cell;
		private boolean forcedLook;
		private static List changeCells = new ArrayList();

		private ChangeCell(Cell cell)
		{
			this.cell = cell;
			this.forcedLook = false;
		}

		/**
		 * Method to return the Cell that has changed.
		 * @return the Cell that has changed.
		 */
		public Cell getCell() { return cell; }

		/**
		 * Method to tell whether changes to a Cell are complex and require extensive recomputation.
		 * @return true if the hierarchy above the Cell must be examined.
		 */
		public boolean getForcedLook() { return forcedLook; }

		/**
		 * Method to clear the list of changed cells.
		 */
		public static void clear()
		{
			changeCells.clear();
		}

		/**
		 * Method to add a Cell to the list of changed cells.
		 * @param cell the Cell to add to the list.
		 * @return the ChangeCell object associated with the Cell.
		 */
		public static ChangeCell add(Cell cell)
		{
			ChangeCell cc = new ChangeCell(cell);
			changeCells.add(cc);
			return cc;
		}

		/**
		 * Method to tell whether a Cell is listed in the current change-cells.
		 * @param cell the Cell in question.
		 * @return true if that Cell is in the list.
		 */
		public static boolean contains(Cell cell)
		{
			for(Iterator it = changeCells.iterator(); it.hasNext(); )
			{
				ChangeCell cc = (ChangeCell)it.next();
				if (cc.cell == cell) return true;
			}
			return false;
		}

		/**
		 * Method to return a list of cells that have changed in this batch.
		 * @return an Interator over the list of changed cells.
		 */
		public static Iterator getIterator()
		{
			return changeCells.iterator();
		}

		/**
		 * Method to ensure that a cell is in the list of changed-cells.
		 * The cell is listed with the "forcelook" state set true so that
		 * full hierarchical analysis is done.
		 * @param cell the Cell to add.
		 */
		public static void forceHierarchicalAnalysis(Cell cell)
		{
			if (currentBatch == null) return;
			for(Iterator it = changeCells.iterator(); it.hasNext(); )
			{
				ChangeCell cc = (ChangeCell)it.next();
				if (cc.cell != cell) continue;
				cc.forcedLook = true;
				return;
			}

			/* if not in the list, create the entry and try again */
			ChangeCell cc = ChangeCell.add(cell);
			cc.forcedLook = true;
		}
	}

	private static Type broadcasting = null;
	private static boolean doNextChangeQuietly = false;
	private static boolean doChangesQuietly = false;
	private static ChangeBatch currentBatch = null;
	private static int maximumBatches = User.getMaxUndoHistory();
	private static int overallBatchNumber = 0;
	private static List doneList = new ArrayList();
	private static List undoneList = new ArrayList();

    /** List of all DatabaseChangeListeners */          private static List changeListeners = new ArrayList();
    /** List of all PropertyChangeListeners */          private static List propertyChangeListeners = new ArrayList();

    /** Property fired if ability to Undo changes */
    public static final String propUndoEnabled = "UndoEnabled";
    /** Property fired if ability to Redo changes */
    public static final String propRedoEnabled = "RedoEnabled";
    private static boolean undoEnabled = false;
    private static boolean redoEnabled = false;

	/**
	 * Method to start a new batch of changes.
	 * @param tool the tool that is producing the activity.
	 * @param activity a String describing the activity.
	 * @param cell root of up-tree or null for whole database lock
	 */
	public static void startChanges(Tool tool, String activity, Cell cell,
                                    List startingHighlights, Point2D startingHighlightsOffset)
	{
		// close any open batch of changes
		endChanges();

		// kill off any undone batches
		noRedoAllowed();

		// erase the list of changed cells
		ChangeCell.clear();

		// allocate a new change batch
		currentBatch = new ChangeBatch();
		currentBatch.changes = new ArrayList();
		currentBatch.batchNumber = ++overallBatchNumber;
		currentBatch.done = true;
		currentBatch.tool = tool;
		currentBatch.activity = activity;
		currentBatch.upCell = cell;
        currentBatch.startingHighlights = startingHighlights;
        currentBatch.startHighlightsOffset = startingHighlightsOffset;

		// put at head of list
		doneList.add(currentBatch);
        setUndoEnabled(true);

		// kill last batch if list is full
		if (doneList.size() > maximumBatches)
		{
			doneList.remove(0);
		}

		// start the batch of changes
		Constraints.getCurrent().startBatch(tool, false);

		for(Iterator it = Tool.getListeners(); it.hasNext(); )
		{
			Listener listener = (Listener)it.next();
			listener.startBatch(tool, false);
		}
	}

	/**
	 * Method to terminate the current batch of changes.
	 */
	public static void endChanges()
	{
		// if no changes were recorded, stop
		if (currentBatch == null) return;

        // if no changes were recorded, stop
        if (currentBatch.changes.size() == 0) {
            // remove from doneList
            doneList.remove(currentBatch);
            overallBatchNumber--;
            if (doneList.size() == 0) setUndoEnabled(false);
        }

		// changes made: apply final constraints to this batch of changes
		Constraints.getCurrent().endBatch();

		for(Iterator it = Tool.getListeners(); it.hasNext(); )
		{
			Listener listener = (Listener)it.next();
			listener.endBatch();
		}
        fireEndChangeBatch(currentBatch);

		currentBatch = null;
	}

    /** Add a DatabaseChangeListener. It will be notified when
     * objects in the database change.
     * @param l the listener
     */
    public static synchronized void addDatabaseChangeListener(DatabaseChangeListener l) {
        changeListeners.add(l);
    }

    /** Remove a DatabaseChangeListener. */
    public static synchronized void removeDatabaseChangeListener(DatabaseChangeListener l) {
        changeListeners.remove(l);
    }

    /** Fire a change event to all database change listeners */
    private static synchronized void fireChangeEvent(Change change) {
        for (Iterator it = changeListeners.iterator(); it.hasNext(); ) {
            DatabaseChangeListener l = (DatabaseChangeListener)it.next();
            if (l.isGUIListener()) {
                //SwingUtilities.invokeLater(new DatabaseChangeThread(l, change));
                // do nothing: if it's delayed by invoke later, it may as well come from
                // databaseEndChangeBatch
            } else
                l.databaseChanged(change);
        }
    }

    private static synchronized void fireEndChangeBatch(ChangeBatch batch) {
        for (Iterator it = changeListeners.iterator(); it.hasNext(); ) {
            DatabaseChangeListener l = (DatabaseChangeListener)it.next();
            if (l.isGUIListener())
                SwingUtilities.invokeLater(new DatabaseBatchThread(l, batch));
            else
                l.databaseEndChangeBatch(batch);
        }
    }

    /** Add a property change listener. This generates Undo and Redo enabled property changes */
    public static synchronized void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeListeners.add(l);
    }

    /** Remove a property change listener. */
    public static synchronized void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeListeners.remove(l);
    }

    private static synchronized void firePropertyChange(String prop, boolean oldValue, boolean newValue) {
        for (Iterator it = propertyChangeListeners.iterator(); it.hasNext(); ) {
            PropertyChangeListener l = (PropertyChangeListener)it.next();
            PropertyChangeEvent e = new PropertyChangeEvent(Undo.class, prop,
                    new Boolean(oldValue), new Boolean(newValue));
            SwingUtilities.invokeLater(new PropertyChangeThread(l, e));
        }
    }

    private static synchronized void setUndoEnabled(boolean enabled) {
        if (enabled != undoEnabled) {
            firePropertyChange(propUndoEnabled, undoEnabled, enabled);
            undoEnabled = enabled;
        }
    }

    private static synchronized void setRedoEnabled(boolean enabled) {
        if (enabled != redoEnabled) {
            firePropertyChange(propRedoEnabled, redoEnabled, enabled);
            redoEnabled = enabled;
        }
    }

    public static synchronized boolean getUndoEnabled() { return undoEnabled; }

    public static synchronized boolean getRedoEnabled() { return redoEnabled; }

    private static class PropertyChangeThread implements Runnable {
        private PropertyChangeListener l;
        private PropertyChangeEvent e;
        private PropertyChangeThread(PropertyChangeListener l, PropertyChangeEvent e) { this.l = l; this.e = e; }
        public void run() { l.propertyChange(e); }
    }

    private static class DatabaseChangeThread implements Runnable {
        private DatabaseChangeListener l;
        private Change change;
        private DatabaseChangeThread(DatabaseChangeListener l, Change c) { this.l = l; this.change = c; }
        public void run() { l.databaseChanged(change); }
    }

    private static class DatabaseBatchThread implements Runnable {
        private DatabaseChangeListener l;
        private ChangeBatch batch;
        private DatabaseBatchThread(DatabaseChangeListener l, ChangeBatch b) { this.l = l; this.batch = b; }
        public void run() { l.databaseEndChangeBatch(batch); }
    }

	/**
	 * Method to record and broadcast a change.
	 * <P>
	 * These types of changes exist:
	 * <UL>
	 * <LI>NODEINSTNEW takes nothing.
	 * <LI>NODEINSTKILL takes nothing.
	 * <LI>NODEINSTMOD takes a1=oldLX a2=oldHX a3=oldLY a4=oldHY i1=oldRotation.
	 * <LI>ARCINSTNEW takes nothing.
	 * <LI>ARCINSTKILL takes nothing.
	 * <LI>ARCINSTMOD takes a1=oldHeadX a2=oldHeadY a3=oldTailX a4=oldTailY a5=oldWidth.
	 * <LI>EXPORTNEW takes nothing.
	 * <LI>EXPORTKILL takes o1=oldPortInsts.
	 * <LI>EXPORTMOD takes o1=oldPortInst.
	 * <LI>CELLNEW takes nothing.
	 * <LI>CELLKILL takes nothing.
	 * <LI>CELLMOD takes a1=oldLowX a2=oldHighX a3=oldLowY a4=oldHighY.
	 * <LI>OBJECTNEW takes nothing.
	 * <LI>OBJECTKILL takes nothing.
	 * <LI>OBJECTRENAME takes o1=oldName.
	 * <LI>OBJECTREDRAW takes nothing.
	 * <LI>VARIABLENEW takes o1=var.
	 * <LI>VARIABLEKILL takes o1=var.
	 * <LI>VARIABLEMOD takes o1=var i1=index o2=oldValue.
	 * <LI>VARIABLEMODFLAGS takes o1=var i1=flags.
	 * <LI>VARIABLEINSERT takes o1=var i1=index.
	 * <LI>VARIABLEDELETE takes a1=var i1=index o2=oldValue.
	 * <LI>DESCRIPTORMOD takes o1=descript i1=oldDescript0 i2=oldDescript1.
	 * </UL>
	 * @param obj the object to which the change applies.
	 * @param change the change being recorded.
	 * @return the change object (null on error).
	 */
	private static Change newChange(ElectricObject obj, Type change, Object o1)
	{
		if (currentBatch == null)
		{
			System.out.println("Received " + change + " change when no batch started");
			return null;
		}
		if (broadcasting != null)
		{
			System.out.println("Received " + change + " change during broadcast of " + broadcasting);
			return null;
		}

		// determine what needs to be marked as changed
		Change.setDirty(change, obj, o1);

		// see if this is the first change
		boolean firstChange = false;
		if (currentBatch.getNumChanges() == 0) firstChange = true;

		// get change module
		Change ch = new Change(obj, change);
		ch.o1 = o1;

		// insert new change module into linked list
		currentBatch.add(ch);

		// broadcast the change
		//ch.broadcast(firstChange, false);
		return ch;
	}

	/**
	 * Method to store a change to a NodeInst in the change-control system.
	 * @param ni the NodeInst that changed.
	 * @param oCX the former X center position.
	 * @param oCY the former Y center position.
	 * @param oSX the former X size.
	 * @param oSY the former Y size.
	 * @param oRot the former rotation of the NodeInst.
	 */
	public static void modifyNodeInst(NodeInst ni, double oCX, double oCY, double oSX, double oSY, int oRot)
	{
		if (!recordChange()) return;
		Change ch = newChange(ni, Type.NODEINSTMOD, null);
		ch.setDoubles(oCX, oCY, oSX, oSY);
		ch.i1 = oRot;
		ni.setChange(ch);
		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
        fireChangeEvent(ch);
	}

	/**
	 * Method to store a change to an ArcInst in the change-control system.
	 * @param ai the ArcInst that changed.
	 * @param oHX the former X position of the arc's head.
	 * @param oHY the former Y position of the arc's head.
	 * @param oTX the former X position of the arc's tail.
	 * @param oTY the former Y position of the arc's tail.
	 * @param oWid the former width of the ArcInst.
	 */
	public static void modifyArcInst(ArcInst ai, double oHX, double oHY, double oTX, double oTY, double oWid)
	{
		if (!recordChange()) return;
		Change ch = newChange(ai, Type.ARCINSTMOD, null);
		ch.setDoubles(oHX, oHY, oTX, oTY);
		ch.a5 = oWid;
		ai.setChange(ch);
		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
        fireChangeEvent(ch);
	}

	/**
	 * Method to store a change to an Export in the change-control system.
	 * Export changes involve moving them from one PortInst to another in the Cell.
	 * @param pp the Export that was moved.
	 * @param oldPi the former PortInst on which the Export resided.
	 */
	public static void modifyExport(Export pp, PortInst oldPi)
	{
		if (!recordChange()) return;
		Change ch = newChange(pp, Type.EXPORTMOD, oldPi);
		pp.setChange(ch);

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
        fireChangeEvent(ch);
		Constraints.getCurrent().modifyExport(pp, oldPi);
	}

	/**
	 * Method to store a change to a Cell in the change-control system.
	 * @param cell the Cell that changed.
	 * @param oLX the former low-X coordinate of the Cell.
	 * @param oHX the former high-X coordinate of the Cell.
	 * @param oLY the former low-Y coordinate of the Cell.
	 * @param oHY the former high-Y coordinate of the Cell.
	 */
	public static void modifyCell(Cell cell, double oLX, double oHX, double oLY, double oHY)
	{
		if (!recordChange()) return;
		Change ch = newChange(cell, Type.CELLMOD, null);
		ch.setDoubles(oLX, oHX, oLY, oHY);
		cell.setChange(ch);

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
        fireChangeEvent(ch);
		//Constraint.getCurrent().modifyCell(cell, oLX, oHX, oLY, oHY);
	}

	/**
	 * Method to store a change to a TextDescriptor in the change-control system.
	 * @param obj the ElectricObject on which the TextDescriptor resides.
	 * @param descript the TextDescriptor that changed.
	 * @param oldDescript0 the former word-0 value of the TextDescriptor.
	 * @param oldDescript1 the former word-1 value of the TextDescriptor.
	 * @param oldColorIndex the former color index of the TextDescriptor.
	 */
	public static void modifyTextDescript(ElectricObject obj, TextDescriptor descript, int oldDescript0, int oldDescript1, int oldColorIndex)
	{
		if (!recordChange()) return;
		Change ch = newChange(obj, Type.DESCRIPTORMOD, descript);
		ch.i1 = oldDescript0;
		ch.i2 = oldDescript1;
		ch.i3 = oldColorIndex;

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
        fireChangeEvent(ch);
		// tell constraint system about this TextDescriptor
		Constraints.getCurrent().modifyTextDescript(obj, descript, oldDescript0, oldDescript1, oldColorIndex);
	}

	/**
	 * Method to store the creation of a new ElectricObject in the change-control system.
	 * @param obj the ElectricObject that was created.
	 */
	public static void newObject(ElectricObject obj)
	{
		if (!recordChange()) return;
		Type type = Type.OBJECTNEW;
		if (obj instanceof Cell) type = Type.CELLNEW;
		else if (obj instanceof NodeInst) type = Type.NODEINSTNEW;
		else if (obj instanceof ArcInst) type = Type.ARCINSTNEW;
		else if (obj instanceof Export) type = Type.EXPORTNEW;
        else if (obj instanceof Library) type = Type.LIBRARYNEW;
		Change ch = newChange(obj, type, null);

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
        fireChangeEvent(ch);
		Constraints.getCurrent().newObject(obj);
	}

	/**
	 * Method to store the deletion of an ElectricObject in the change-control system.
	 * @param obj the ElectricObject that was deleted.
	 * Note: because a reference to the object is kept in the change-control system,
	 * the object is not really deallocated until the change-control system no longer
	 * remembers the change, and it can no longer be undone.
	 */
	public static void killObject(ElectricObject obj)
	{
		if (!recordChange()) return;
		Type type = Type.OBJECTKILL;
		if (obj instanceof Cell) type = Type.CELLKILL;
		else if (obj instanceof NodeInst) type = Type.NODEINSTKILL;
		else if (obj instanceof ArcInst) type = Type.ARCINSTKILL;
        else if (obj instanceof Library) type = Type.LIBRARYKILL;
		Change ch = newChange(obj, type, null);

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
        fireChangeEvent(ch);
		Constraints.getCurrent().killObject(obj);
	}

	/**
	 * Method to store the deletion of an Export in the change-control system.
	 * @param pp the Export that was deleted.
	 * @param oldPortInsts a collection of deleted PortInsts of the Export.
	 */
	public static void killExport(Export pp, Collection oldPortInsts)
	{
		if (!recordChange()) return;
		Type type = Type.EXPORTKILL;
		Change ch = newChange(pp, type, oldPortInsts);

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
        fireChangeEvent(ch);
		Constraints.getCurrent().killExport(pp, oldPortInsts);
	}

	/**
	 * Method to store the renaming of an ElectricObject in the change-control system.
	 * @param obj the ElectricObject that was renamed.
	 * @param oldName the former name of the ElectricObject.
	 */
	public static void renameObject(ElectricObject obj, Name oldName)
	{
		if (!recordChange()) return;
		Type type = Type.OBJECTRENAME;
		Change ch = newChange(obj, type, oldName);

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
        fireChangeEvent(ch);
		Constraints.getCurrent().renameObject(obj, oldName);
	}

	/**
	 * Method to store the redrawing of an ElectricObject in the change-control system.
	 * This does not relate to database changes, but may be necessary if
	 * another change requires redisplay of the object.
	 * @param obj the ElectricObject to redisplay.
	 */
	public static void redrawObject(ElectricObject obj)
	{
		if (!recordChange()) return;
		Type type = Type.OBJECTREDRAW;
		Change ch = newChange(obj, type, null);

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
        fireChangeEvent(ch);
	}

	/**
	 * Method to store the creation of a new Variable in the change-control system.
	 * @param obj the ElectricObject that has the Variable.
	 * @param var the Variable that was created.
	 */
	public static void newVariable(ElectricObject obj, Variable var)
	{
		if (!recordChange()) return;
		Type type = Type.VARIABLENEW;
		Change ch = newChange(obj, type, var);

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
        fireChangeEvent(ch);
		Constraints.getCurrent().newVariable(obj, var);
	}

	/**
	 * Method to store the deletion of a Variable in the change-control system.
	 * @param obj the ElectricObject on which the Variable resided.
	 * @param var the Variable that was deleted.
	 */
	public static void killVariable(ElectricObject obj, Variable var)
	{
		if (!recordChange()) return;
		Type type = Type.VARIABLEKILL;
		Change ch = newChange(obj, type, var);

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
        fireChangeEvent(ch);
		Constraints.getCurrent().killVariable(obj, var);
	}

	/**
	 * Method to store the modification of a Variable's flags in the change-control system.
	 * The flag bits of a Variable control whether it is displayed, treated as code, etc.
	 * @param obj the ElectricObject on which the Variable resides.
	 * @param var the Variable that was modified.
	 * @param oldFlags the former flag bits on the Variable.
	 */
	public static void modifyVariableFlags(ElectricObject obj, Variable var, int oldFlags)
	{
		if (!recordChange()) return;
		Change ch = Undo.newChange(obj, Type.VARIABLEMODFLAGS, var);
		ch.i1 = oldFlags;

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
        fireChangeEvent(ch);
		Constraints.getCurrent().modifyVariableFlags(obj, var, oldFlags);
	}

	/**
	 * Method to store the modification if an entry in an arrayed Variable, in the change-control system.
	 * @param obj the ElectricObject on which the Variable resides.
	 * @param var the Variable.
	 * @param index the entry in the Variable's array.
	 * @param oldValue the former value of that entry.
	 */
	public static void modifyVariable(ElectricObject obj, Variable var, int index, Object oldValue)
	{
		if (!recordChange()) return;
		Change ch = Undo.newChange(obj, Type.VARIABLEMOD, var);
		ch.i1 = index;
		ch.o2 = oldValue;

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
        fireChangeEvent(ch);
		Constraints.getCurrent().modifyVariable(obj, var, index, oldValue);
	}

	/**
	 * Method to store the insertion of an entry into a Variable, in the change-control system.
	 * @param obj the ElectricObject on which the Variable resides.
	 * @param var the Variable that has had a new entry added to it.
	 * @param index the entry in the Variable's array that was added.
	 */
	public static void insertVariable(ElectricObject obj, Variable var, int index)
	{
		if (!recordChange()) return;
		Change ch = Undo.newChange(obj, Type.VARIABLEINSERT, var);
		ch.i1 = index;

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
        fireChangeEvent(ch);
		Constraints.getCurrent().insertVariable(obj, var, index);
	}

	/**
	 * Method to store the deletion of an entry on a Variable, in the change-control system.
	 * @param obj the ElectricObject on which the Variable resides.
	 * @param var the Variable that has had an entry removed.
	 * @param index the entry in the Variable's array that was deleted.
	 * @param oldValue the former value at that entry.
	 */
	public static void deleteVariable(ElectricObject obj, Variable var, int index, Object oldValue)
	{
		if (!recordChange()) return;
		Change ch = Undo.newChange(obj, Type.VARIABLEDELETE, var);
		ch.i1 = index;
		ch.o2 = oldValue;

		ch.broadcast(currentBatch.getNumChanges() <= 1, false);
        fireChangeEvent(ch);
		Constraints.getCurrent().deleteVariable(obj, var, index, oldValue);
	}

	/**
	 * Method to return the current change batch.
	 * @return the current change batch (null if no changes are being done).
	 */
	public static ChangeBatch getCurrentBatch() { return currentBatch; }

	/**
	 * Method to request that the next change be "quiet".
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
	 */
	public static void setNextChangeQuiet(boolean quiet) { doNextChangeQuietly = quiet; }

	/**
	 * Method to set the subsequent changes to be "quiet".
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
	 */
	public static void changesQuiet(boolean quiet) {
        doChangesQuietly = quiet;
        setNextChangeQuiet(quiet);
    }

	/**
	 * Method to tell whether changes are currently "quiet".
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
	 * By calling this method, the "next change quiet" state is reset.
	 * @return true if changes are "quiet".
	 */
	public static boolean recordChange()
	{
        boolean recordChange = !doChangesQuietly;
        if (doNextChangeQuietly != doChangesQuietly) {
            // the next change state overrides the changesquietly state
            recordChange = !doNextChangeQuietly;
            // reset the next change quiet state
            setNextChangeQuiet(doChangesQuietly);
        }
		return recordChange;
	}

	/**
	 * Method to undo the last batch of changes.
	 * @return true if a batch was undone.
	 */
	public static boolean undoABatch()
	{
        // save highlights for redo
        List savedHighlights = new ArrayList();
        Point2D offset = new Point2D.Double(0, 0);
        // for now, just save from the current window
        EditWindow wnd = EditWindow.getCurrent();
        Highlighter highlighter = null;
        if (wnd != null) {
            highlighter = wnd.getHighlighter();
            for (Iterator it = highlighter.getHighlights().iterator(); it.hasNext(); ) {
                savedHighlights.add(it.next());
            }
            offset = highlighter.getHighlightOffset();
            highlighter.clear();
            highlighter.finished();
        }

		// close out the current batch
		endChanges();

		// get the most recent batch of changes
		int listSize = doneList.size();
		if (listSize == 0) return false;
		ChangeBatch batch = (ChangeBatch)doneList.get(listSize-1);
		doneList.remove(listSize-1);
		undoneList.add(batch);
        if (doneList.size() == 0) setUndoEnabled(false);
        setRedoEnabled(true);

        // save pre undo highlights
        batch.preUndoHighlights = savedHighlights;
        batch.preUndoHighlightsOffset = offset;

		// look through the changes in this batch
		boolean firstChange = true;
		int batchSize = batch.changes.size();
		for(int i = batchSize-1; i >= 0; i--)
		{
			Change ch = (Change)batch.changes.get(i);

			// reverse the change
			ch.reverse();

			// now broadcast this change
			ch.broadcast(firstChange, true);
			firstChange = false;
		}

		// broadcast the end-batch
		for(Iterator it = Tool.getListeners(); it.hasNext(); )
		{
			Listener listener = (Listener)it.next();
			listener.endBatch();
		}
        fireEndChangeBatch(batch);

        // restore highlights (must be done after all other tools have
        // responded to changes)
        List highlights = new ArrayList();
        for (Iterator it = batch.startingHighlights.iterator(); it.hasNext(); ) {
            highlights.add(it.next());
        }
        if (highlighter != null) {
            highlighter.setHighlightList(highlights);
            highlighter.setHighlightOffset((int)batch.startHighlightsOffset.getX(),
                    (int)batch.startHighlightsOffset.getY());
            highlighter.finished();
        }

		// mark that this batch is undone
		batch.done = false;
		return true;
	}

	/**
	 * Method to redo the last batch of changes.
	 * @return true if a batch was redone.
	 */
	public static boolean redoABatch()
	{
        EditWindow wnd = EditWindow.getCurrent();
        Highlighter highlighter = null;
        if (wnd != null) highlighter = wnd.getHighlighter();
        if (highlighter != null) {
            highlighter.clear();
            highlighter.finished();
        }

		// close out the current batch
		endChanges();

		// get the most recent batch of changes
		if (undoneList == null) return false;
		int listSize = undoneList.size();
		if (listSize == 0) return false;
		ChangeBatch batch = (ChangeBatch)undoneList.get(listSize-1);
		undoneList.remove(listSize-1);
		doneList.add(batch);
        if (undoneList.size() == 0) setRedoEnabled(false);
        setUndoEnabled(true);

		// look through the changes in this batch
		boolean firstChange = true;
		int batchSize = batch.changes.size();
		for(int i = 0; i<batchSize; i++)
		{
			Change ch = (Change)batch.changes.get(i);

			// reverse the change
			ch.reverse();

			// now broadcast this change
			ch.broadcast(firstChange, true);
			firstChange = false;
		}

		// broadcast the end-batch
		for(Iterator it = Tool.getListeners(); it.hasNext(); )
		{
			Listener listener = (Listener)it.next();
			listener.endBatch();
		}
        fireEndChangeBatch(batch);

        // set highlights to what they were before undo
        List highlights = new ArrayList();
        for (Iterator it = batch.preUndoHighlights.iterator(); it.hasNext(); ) {
            highlights.add(it.next());
        }
        if (highlighter != null) {
            highlighter.setHighlightList(highlights);
            highlighter.setHighlightOffset((int)batch.preUndoHighlightsOffset.getX(),
                    (int)batch.preUndoHighlightsOffset.getY());
            highlighter.finished();
        }

		// mark that this batch is redone
		batch.done = true;
		return true;
	}

	/**
	 * Returns root of up-tree of undo or redo batch.
	 * @param redo true if redo batch, false if undo batch
	 * @return root of up-tree of a batch.
	 */
	public static Cell upCell(boolean redo)
	{
		ChangeBatch batch = null;
		if (redo)
		{
			int listSize = undoneList.size();
			if (listSize > 0)
				batch = (ChangeBatch)undoneList.get(listSize-1);
		} else
		{
			int listSize = doneList.size();
			if (listSize != 0)
				batch = (ChangeBatch)doneList.get(listSize-1);
		}
		return batch != null ? batch.upCell : null;

	}

	/**
	 * Method to prevent undo by deleting all change batches.
	 */
	public static void noUndoAllowed()
	{
 		// properly terminate the current batch
		endChanges();

 		// kill them all
 		doneList.clear();
 		undoneList.clear();
        setUndoEnabled(false);
        setRedoEnabled(false);
	}

	/**
	 * Method to prevent redo by deleting all undone change batches.
	 */
	public static void noRedoAllowed()
	{
		// properly terminate the current batch
		endChanges();

		undoneList.clear();
        setRedoEnabled(false);
	}

	/**
	 * Method to set the size of the history list and return the former size.
	 * @param newSize the new size of the history list (number of batches of changes).
	 * If not positive, the list size is not changed.
	 * @return the former size of the history list.
	 */
	public static int setHistoryListSize(int newSize)
	{
		if (newSize <= 0) return maximumBatches;

		int oldSize = maximumBatches;
		maximumBatches = newSize;
		if (doneList.size() > maximumBatches)
		{
			doneList.remove(0);
		}
        if (doneList.size() == 0) setUndoEnabled(false);
		return oldSize;
	}

	/**
	 * Method to display all changes.
	 */
	public static void showHistoryList()
	{
		System.out.println("----------  Done batches (" + doneList.size() + ") ----------:");
		for(int i=0; i<doneList.size(); i++)
		{
			ChangeBatch batch = (ChangeBatch)doneList.get(i);
			batch.describe();
		}
		System.out.println("----------  Undone batches (" + undoneList.size() + ") ----------:");
		for(int i=0; i<undoneList.size(); i++)
		{
			ChangeBatch batch = (ChangeBatch)undoneList.get(i);
			batch.describe();
		}
	}
}
