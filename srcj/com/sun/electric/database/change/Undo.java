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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.tool.Tool;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

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
		/** Describes the start of changes to an arbitrary object. */		public static final Type OBJECTSTART = new Type("ObjectStart");
		/** Describes the end of changes to an arbitrary object. */			public static final Type OBJECTEND = new Type("ObjectEnd");
		/** Describes the creation of an arbitrary object. */				public static final Type OBJECTNEW = new Type("ObjectNew");
		/** Describes the deletion of an arbitrary object. */				public static final Type OBJECTKILL = new Type("ObjectKill");
		/** Describes the creation of a Variable on an object. */			public static final Type VARIABLENEW = new Type("VariableNew");
		/** Describes the deletion of a Variable on an object. */			public static final Type VARIABLEKILL = new Type("VariableKill");
		/** Describes the modification of a Variable on an object. */		public static final Type VARIABLEMOD = new Type("VariableMod");
		/** Describes the insertion of an entry in an arrayed Variable. */	public static final Type VARIABLEINSERT = new Type("VariableInsert");
		/** Describes the deletion of an entry in an arrayed Variable. */	public static final Type VARIABLEDELETE = new Type("VariableDelete");
		/** Describes the change to a TextDescriptor. */					public static final Type DESCRIPTORMOD = new Type("DescriptMod");
	}

	/**
	 * Change describes a single change.
	 */
	public static class Change
	{
		private ElectricObject obj;
		private Type change;
		private double a1;
		private double a2;
		private double a3;
		private double a4;
		private int a5;
		private int a6;

		Change(ElectricObject obj, Type change, double a1, double a2, double a3, double a4, int a5, int a6)
		{
			this.obj = obj;
			this.change = change;
			this.a1 = a1;
			this.a2 = a2;
			this.a3 = a3;
			this.a4 = a4;
			this.a5 = a5;
			this.a6 = a6;
		}
		
		public ElectricObject getObject() { return obj; }
		public Type getChange() { return change; }
		public void setChange(Type change) { this.change = change; }
		public double getA1() { return a1; }
		public double getA2() { return a2; }
		public double getA3() { return a3; }
		public double getA4() { return a4; }
		public int getA5() { return a5; }
		public int getA6() { return a6; }

		/**
		 * Routine to broadcast a change to all tools that are on.
		 * @param firstchange true if this is the first change of a batch, so that a "startbatch" change must also be broadcast.
		 * @param undoredo true if this is an undo/redo batch.
		 */
		void broadcast(boolean firstchange, boolean undoRedo)
		{
			// start the batch if this is the first change
			broadcasting = change;
			if (firstchange)
			{
				// broadcast a start-batch on the first change
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.startBatch(tool, undoRedo);
				}
			}
			if (change == Type.NODEINSTNEW || change == Type.ARCINSTNEW || change == Type.EXPORTNEW ||
				change == Type.CELLNEW || change == Type.OBJECTNEW)
			{
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.newObject(obj);
				}
			} else if (change == Type.NODEINSTKILL || change == Type.ARCINSTKILL || change == Type.EXPORTKILL ||
				change == Type.CELLKILL || change == Type.OBJECTKILL)
			{
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.killObject(obj);
				}
			} else if (change == Type.NODEINSTMOD)
			{
//				for(Iterator it = Tool.getTools(); it.hasNext(); )
//				{
//					Tool tool = (Tool)it.next();
//					if (tool.isOn()) tool.modifyNodeInst(obj, a1, a2, a3, a4, a5. a6 != 0);
//				}
			} else if (change == Type.ARCINSTMOD)
			{
//				for(i=0; i<el_maxtools; i++)
//					if ((el_tools[i].toolstate & TOOLON) != 0 && el_tools[i].modifyarcinst != 0)
//						(*el_tools[i].modifyarcinst)((ARCINST *)c->entryaddr, c->p1, c->p2,
//							c->p3, c->p4, c->p5, c->p6);
			} else if (change == Type.EXPORTMOD)
			{
//				for(i=0; i<el_maxtools; i++)
//					if ((el_tools[i].toolstate & TOOLON) != 0 && el_tools[i].modifyportproto != 0)
//						(*el_tools[i].modifyportproto)((PORTPROTO *)c->entryaddr, (NODEINST *)c->p1,
//							(PORTPROTO *)c->p2);
			} else if (change == Type.CELLMOD)
			{
//				for(i=0; i<el_maxtools; i++)
//					if ((el_tools[i].toolstate & TOOLON) != 0 && el_tools[i].modifynodeproto != 0)
//						(*el_tools[i].modifynodeproto)((NODEPROTO *)c->entryaddr);
			} else if (change == Type.OBJECTSTART)
			{
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.startChange(obj);
				}
			} else if (change == Type.OBJECTEND)
			{
				for(Iterator it = Tool.getTools(); it.hasNext(); )
				{
					Tool tool = (Tool)it.next();
					if (tool.isOn()) tool.endChange(obj);
				}
			} else if (change == Type.VARIABLENEW)
			{
//				for(i=0; i<el_maxtools; i++)
//					if ((el_tools[i].toolstate & TOOLON) != 0 && el_tools[i].newvariable != 0)
//						(*el_tools[i].newvariable)(c->entryaddr, c->p1, c->p2, c->p3);
			} else if (change == Type.VARIABLEKILL)
			{
//				descript[0] = c->p5;
//				descript[1] = c->p6;
//				for(i=0; i<el_maxtools; i++)
//					if ((el_tools[i].toolstate & TOOLON) != 0 && el_tools[i].killvariable != 0)
//						(*el_tools[i].killvariable)(c->entryaddr, c->p1, c->p2, c->p3, c->p4, descript);
			} else if (change == Type.VARIABLEMOD)
			{
//				for(i=0; i<el_maxtools; i++)
//					if ((el_tools[i].toolstate & TOOLON) != 0 && el_tools[i].modifyvariable != 0)
//						(*el_tools[i].modifyvariable)(c->entryaddr, c->p1, c->p2, c->p3, c->p4, c->p5);
			} else if (change == Type.VARIABLEINSERT)
			{
//				for(i=0; i<el_maxtools; i++)
//					if ((el_tools[i].toolstate & TOOLON) != 0 && el_tools[i].insertvariable != 0)
//						(*el_tools[i].insertvariable)(c->entryaddr, c->p1, c->p2, c->p3, c->p4);
			} else if (change == Type.VARIABLEDELETE)
			{
//				for(i=0; i<el_maxtools; i++)
//					if ((el_tools[i].toolstate & TOOLON) != 0 && el_tools[i].deletevariable != 0)
//						(*el_tools[i].deletevariable)(c->entryaddr, c->p1, c->p2, c->p3, c->p4, c->p5);
			} else if (change == Type.DESCRIPTORMOD)
			{
//				descript[0] = c->p4;
//				descript[1] = c->p5;
//				for(i=0; i<el_maxtools; i++)
//					if ((el_tools[i].toolstate & TOOLON) != 0 && el_tools[i].modifydescript != 0)
//						(*el_tools[i].modifydescript)(c->entryaddr, c->p1, c->p2, descript);
			}
			broadcasting = null;
		}

		/**
		 * Routine to undo the effects of this change.
		 */
		void reverse()
		{
			// determine what needs to be marked as changed
			setDirty(change, obj);

			if (change == Type.NODEINSTNEW)
			{
				NodeInst ni = (NodeInst)obj;
				ni.lowLevelUnlink();
				change = Type.NODEINSTKILL;
				return;
			}
			if (change == Type.NODEINSTKILL)
			{
				NodeInst ni = (NodeInst)obj;
				ni.lowLevelLink();
				change = Type.NODEINSTNEW;
				return;
			}
			if (change == Type.NODEINSTMOD)
			{
				NodeInst ni = (NodeInst)obj;
//				oldval = ni->lowx;       ni->lowx = c->p1;       c->p1 = oldval;
//				oldval = ni->lowy;       ni->lowy = c->p2;       c->p2 = oldval;
//				oldval = ni->highx;      ni->highx = c->p3;      c->p3 = oldval;
//				oldval = ni->highy;      ni->highy = c->p4;      c->p4 = oldval;
//				oldshort = ni->rotation;   ni->rotation = (INTSML)c->p5;   c->p5 = oldshort;
//				oldshort = ni->transpose;  ni->transpose = (INTSML)c->p6;  c->p6 = oldshort;
				// need to call "updategeom()"
				return;
			}
			if (change == Type.ARCINSTNEW)
			{
				ArcInst ai = (ArcInst)obj;
				ai.lowLevelUnlink();
				change = Type.ARCINSTKILL;
				return;
			}
			if (change == Type.ARCINSTKILL)
			{
				ArcInst ai = (ArcInst)obj;
				ai.lowLevelLink();
				change = Type.ARCINSTNEW;
				return;
			}
			if (change == Type.ARCINSTMOD)
			{
				ArcInst ai = (ArcInst)obj;
//				oldval = ai->end[0].xpos;  ai->end[0].xpos = c->p1;   c->p1 = oldval;
//				oldval = ai->end[0].ypos;  ai->end[0].ypos = c->p2;   c->p2 = oldval;
//				oldval = ai->end[1].xpos;  ai->end[1].xpos = c->p3;   c->p3 = oldval;
//				oldval = ai->end[1].ypos;  ai->end[1].ypos = c->p4;   c->p4 = oldval;
//				oldval = ai->width;        ai->width = c->p5;         c->p5 = oldval;
//				oldval = ai->length;       ai->length = c->p6;        c->p6 = oldval;
//				determineangle(ai);
//				(void)setshrinkvalue(ai, TRUE);
				// need to call "updategeom()"
				return;
			}
			if (change == Type.EXPORTNEW)
			{
				Export pp = (Export)obj;
				pp.lowLevelUnlink();
				change = Type.EXPORTKILL;
				return;
			}
			if (change == Type.EXPORTKILL)
			{
				Export pp = (Export)obj;
				pp.lowLevelLink();
				change = Type.EXPORTNEW;
				return;
			}
			if (change == Type.EXPORTMOD)
			{
//				Export pp = (Export)obj;
//				oldsubni = pp->subnodeinst;
//				oldsubpp = pp->subportproto;
//				db_changeport(pp, (NODEINST *)c->p1, (PORTPROTO *)c->p2);
//				c->p1 = (INTBIG)oldsubni;   c->p2 = (INTBIG)oldsubpp;
				return;
			}
			if (change == Type.CELLNEW)
			{
				Cell cell = (Cell)obj;
				cell.lowLevelUnlink();
				change = Type.CELLKILL;
				return;
			}
			if (change == Type.CELLKILL)
			{
				Cell cell = (Cell)obj;
				cell.lowLevelLink();
				change = Type.CELLNEW;
				return;
			}
			if (change == Type.CELLMOD)
			{
				Cell cell = (Cell)obj;
//				oldval = cell->lowx;    cell->lowx = c->p1;    c->p1 = oldval;
//				oldval = cell->highx;   cell->highx = c->p2;   c->p2 = oldval;
//				oldval = cell->lowy;    cell->lowy = c->p3;    c->p3 = oldval;
//				oldval = cell->highy;   cell->highy = c->p4;   c->p4 = oldval;
				return;
			}
			if (change == Type.OBJECTSTART)
			{
				change = Type.OBJECTEND;
				return;
			}
			if (change == Type.OBJECTEND)
			{
				change = Type.OBJECTSTART;
				return;
			}
			if (change == Type.OBJECTNEW)
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
				change = Type.OBJECTKILL;
				return;
			}
			if (change == Type.OBJECTKILL)
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
				change = Type.OBJECTNEW;
				return;
			}
			if (change == Type.VARIABLENEW)
			{
				// args: addr, type, key, newtype, newdescript[0], newdescript[1]
//				if ((c->p3&VCREF) != 0)
//				{
//					// hange to fixed attribute
//					attname = changedvariablename(c->p1, c->p2, c->p3);
//					var = getvalnoeval(c->entryaddr, c->p1, c->p3 & (VTYPE|VISARRAY), attname);
//					if (var == NOVARIABLE)
//					{
//						ttyputmsg(_("Warning: Could not find attribute %s on object %s"),
//							attname, describeobject(c->entryaddr, c->p1));
//						break;
//					}
//					c->p3 = var->addr;
//					c->p4 = var->type;
//					c->p5 = var->textdescript[0];
//					c->p6 = var->textdescript[1];
//					c->Type = VARIABLEKILL;
//					if (c->p1 == VNODEINST || c->p1 == VARCINST) return(TRUE);
//					if (c->p1 == VPORTPROTO)
//					{
//						if (estrcmp(attname, x_("textdescript")) == 0 ||
//							estrcmp(attname, x_("userbits")) == 0 ||
//							estrcmp(attname, x_("protoname")) == 0) return(TRUE);
//					}
//					break;
//				}
//
//				// change to variable attribute: get current value
//				var = getvalkeynoeval(c->entryaddr, c->p1, c->p3&(VTYPE|VISARRAY), c->p2);
//				if (var == NOVARIABLE)
//				{
//					ttyputmsg(_("Warning: Could not find attribute %s on object %s"),
//						makename(c->p2), describeobject(c->entryaddr, c->p1));
//					break;
//				}
//				c->p3 = var->addr;
//				c->p4 = var->type;
//				c->p5 = var->textdescript[0];
//				c->p6 = var->textdescript[1];
				change = Type.VARIABLEKILL;
//				var->type = VINTEGER;		// fake it out so no memory is deallocated
//				nextchangequiet();
//				(void)delvalkey(c->entryaddr, c->p1, c->p2);
				return;
			}
			if (change == Type.VARIABLEKILL)
			{
				// args: addr, type, key, oldaddr, oldtype, olddescript
//				if ((c->p4&VCREF) != 0)
//				{
//					attname = changedvariablename(c->p1, c->p2, c->p4);
//					nextchangequiet();
//					var = setval(c->entryaddr, c->p1, attname, c->p3, c->p4);
//					if (var == NOVARIABLE)
//					{
//						ttyputmsg(_("Warning: Could not set attribute %s on object %s"),
//							attname, describeobject(c->entryaddr, c->p1));
//						break;
//					}
//				} else
//				{
//					nextchangequiet();
//					var = setvalkey(c->entryaddr, c->p1, c->p2, c->p3, c->p4);
//					if (var == NOVARIABLE)
//					{
//						ttyputmsg(_("Warning: Could not set attribute %s on object %s"),
//							makename(c->p2), describeobject(c->entryaddr, c->p1));
//						break;
//					}
//					var->textdescript[0] = c->p5;
//					var->textdescript[1] = c->p6;
//				}
//				db_freevar(c->p3, c->p4);
//				c->p3 = c->p4;
				change = Type.VARIABLENEW;
				return;
			}
			if (change == Type.VARIABLEMOD)
			{
				// args: addr, type, key, vartype, aindex, oldvalue
//				if ((c->p3&VCREF) != 0)
//				{
//					attname = changedvariablename(c->p1, c->p2, c->p3);
//					if (getind(c->entryaddr, c->p1, attname, c->p4, &oldval))
//					{
//						ttyputmsg(_("Warning: Could not find index %ld of attribute %s on object %s"),
//							c->p4, attname, describeobject(c->entryaddr, c->p1));
//						break;
//					}
//					nextchangequiet();
//					(void)setind(c->entryaddr, c->p1, attname, c->p4, c->p5);
//					c->p5 = oldval;
//					if (c->p1 == VPORTPROTO)
//					{
//						if (estrcmp(attname, x_("textdescript")) == 0) return(TRUE);
//					}
//				} else
//				{
//					if (getindkey(c->entryaddr, c->p1, c->p2, c->p4, &oldval))
//					{
//						ttyputmsg(_("Warning: Could not find index %ld of attribute %s on object %s"),
//							c->p4, makename(c->p2), describeobject(c->entryaddr, c->p1));
//						break;
//					}
//					if ((c->p3&(VCODE1|VCODE2)) != 0 || (c->p3&VTYPE) == VSTRING)
//					{
//						// because this change is done quietly, the memory is freed and must be saved
//						(void)allocstring(&storage, (CHAR *)oldval, db_cluster);
//						oldval = (INTBIG)storage;
//					}
//					nextchangequiet();
//					(void)setindkey(c->entryaddr, c->p1, c->p2, c->p4, c->p5);
//					c->p5 = oldval;
//				}
				return;
			}
			if (change == Type.VARIABLEINSERT)
			{
				// args: addr, type, key, vartype, aindex
//				if (getindkey(c->entryaddr, c->p1, c->p2, c->p4, &oldval))
//				{
//					ttyputmsg(_("Warning: Could not find index %ld of attribute %s on object %s"),
//						c->p4, makename(c->p2), describeobject(c->entryaddr, c->p1));
//					break;
//				}
//				nextchangequiet();
//				(void)delindkey(c->entryaddr, c->p1, c->p2, c->p4);
//				c->p5 = oldval;
				change = Type.VARIABLEDELETE;
				return;
			}
			if (change == Type.VARIABLEDELETE)
			{
				// args: addr, type, key, vartype, aindex, oldvalue
//				nextchangequiet();
//				(void)insindkey(c->entryaddr, c->p1, c->p2, c->p4, c->p5);
				change = Type.VARIABLEINSERT;
				return;
			}
			if (change == Type.DESCRIPTORMOD)
			{
//				var = getvalkeynoeval(c->entryaddr, c->p1, -1, c->p2);
//				if (var == NOVARIABLE) break;
//				oldval = var->textdescript[0];   var->textdescript[0] = c->p4;   c->p4 = oldval;
//				oldval = var->textdescript[1];   var->textdescript[1] = c->p5;   c->p5 = oldval;
				return;
			}
		}

		/**
		 * Routine to examine a change and mark the appropriate libraries and cells as "dirty".
		 * @param change the type of change being made.
		 * @param obj the object to which the change is applied.
		 */
		private static void setDirty(Type change, ElectricObject obj)
		{
			Cell cell = null;
			Library lib = null;
			boolean major = false;
			if (change == Type.NODEINSTNEW || change == Type.NODEINSTKILL || change == Type.NODEINSTMOD)
			{
				NodeInst ni = (NodeInst)obj;
				cell = ni.getParent();
				lib = cell.getLibrary();
				major = true;
			} else if (change == Type.ARCINSTNEW || change == Type.ARCINSTKILL || change == Type.ARCINSTMOD)
			{
				ArcInst ai = (ArcInst)obj;
				cell = ai.getParent();
				lib = cell.getLibrary();
				major = true;
			} else if (change == Type.EXPORTNEW || change == Type.EXPORTKILL || change == Type.EXPORTMOD)
			{
				Export pp = (Export)obj;
				cell = (Cell)pp.getParent();
				lib = cell.getLibrary();
				major = true;
			} else if (change == Type.CELLNEW || change == Type.CELLKILL)
			{
				cell = (Cell)obj;
				lib = cell.getLibrary();
				major = true;
			} else if (change == Type.CELLMOD)
			{
				cell = (Cell)obj;
				lib = cell.getLibrary();
			} else if (change == Type.OBJECTNEW || change == Type.OBJECTKILL)
			{
				cell = obj.whichCell();
				if (cell != null) lib = cell.getLibrary();
			} else if (change == Type.VARIABLENEW || change == Type.VARIABLEMOD ||
				change == Type.VARIABLEINSERT || change == Type.VARIABLEDELETE)
			{
//				if ((a3&VDONTSAVE) == 0)
//				{
//					cell = obj.whichCell();
//					if (cell != null) lib = cell.getLibrary();
//
//					// special cases that make the change "major"
//					if (db_majorvariable(a1, a2)) major = true;
//				}
			} else if (change == Type.VARIABLEKILL)
			{
//				if ((a4&VDONTSAVE) == 0)
//				{
//					cell = obj.whichCell();
//					if (cell != null) lib = cell.getLibrary();
//
//					// special cases that make the change "major"
//					if (db_majorvariable(a1, a2)) major = true;
//				}
			} else if (change == Type.DESCRIPTORMOD)
			{
//				if ((a3&VDONTSAVE) == 0)
//				{
//					cell = obj.whichCell();
//					if (cell != null) lib = cell.getLibrary();
//				}
			}

			// set "changed" and "dirty" bits
			if (cell != null)
			{
				if (major) cell.madeRevision();
				if (!changeCells.contains(cell))
				{
					changeCells.add(cell);
				}
			}
			if (lib != null)
			{
				if (major) lib.setChangedMajor(); else
					lib.setChangedMinor();
			}
		}

		/**
		 * Routine to describe this change as a string.
		 */
		String describe()
		{
			if (change == Type.NODEINSTNEW)
			{
				NodeInst ni = (NodeInst)obj;
				return "Node " + ni.describe() + " created in cell " + ni.getParent().describe();
			}
			if (change == Type.NODEINSTKILL)
			{
				NodeInst ni = (NodeInst)obj;
				return "Node " + ni.describe() + " deleted from cell " + ni.getParent().describe();
			}
			if (change == Type.NODEINSTMOD)
			{
				NodeInst ni = (NodeInst)obj;
				return "Node " + ni.describe() + " modified in cell " + ni.getParent().describe();
//				formatinfstr(infstr, M_(" Node '%s' changed in cell %s [was %sx%s, center (%s,%s), rotated %ld"),
//					describenodeinst(ni), describenodeproto(ni->parent), latoa(p3-p1, lambda),
//					latoa(p4-p2, lambda), latoa((p1+p3)/2, lambda), latoa((p2+p4)/2, lambda), p5);
//				if (p6 != 0) addstringtoinfstr(infstr, M_(", transposed"));
			}
			if (change == Type.ARCINSTNEW)
			{
				ArcInst ai = (ArcInst)obj;
				return "Arc " + ai.describe() + " created in cell " + ai.getParent().describe();
			}
			if (change == Type.ARCINSTKILL)
			{
				ArcInst ai = (ArcInst)obj;
				return "Arc " + ai.describe() + " deleted from cell " + ai.getParent().describe();
			}
			if (change == Type.ARCINSTMOD)
			{
				ArcInst ai = (ArcInst)obj;
				return "Arc " + ai.describe() + " modified in cell " + ai.getParent().describe();
//				formatinfstr(infstr, M_(" Arc '%s' modified in cell %s [was %s wide %s long from (%s,%s) to (%s,%s)]"),
//					describearcinst(ai), describenodeproto(ai->parent), latoa(p5, lambda), latoa(p6, lambda),
//						latoa(p1, lambda), latoa(p2, lambda), latoa(p3, lambda), latoa(p4, lambda));
			}
			if (change == Type.EXPORTNEW)
			{
				Export pp = (Export)obj;
				return "Export " + pp.getProtoName() + " created in cell " + pp.getParent().describe();
			}
			if (change == Type.EXPORTKILL)
			{
				Export pp = (Export)obj;
				return "Export " + pp.getProtoName() + " deleted from cell " + pp.getParent().describe();
			}
			if (change == Type.EXPORTMOD)
			{
				Export pp = (Export)obj;
				return "Export " + pp.getProtoName() + " modified in cell " + pp.getParent().describe();
//				formatinfstr(infstr, M_(" Port '%s' moved in cell %s [was on node %s, subport %s]"),
//					pp->protoname, describenodeproto(pp->parent), describenodeinst((NODEINST *)p1),
//					((PORTPROTO *)p2)->protoname);
			}
			if (change == Type.CELLNEW)
			{
				Cell cell = (Cell)obj;
				return "Cell " + cell.describe() + " created";
			}
			if (change == Type.CELLKILL)
			{
				Cell cell = (Cell)obj;
				return "Cell " + cell.describe() + " deleted";
			}
			if (change == Type.CELLMOD)
			{
				Cell cell = (Cell)obj;
				return "Cell " + cell.describe() + " modified";
//				formatinfstr(infstr, M_(" Cell '%s' modified [was from %s<=X<=%s %s<=Y<=%s]"),
//					describenodeproto(np), latoa(p1, lambda), latoa(p2, lambda),
//						latoa(p3, lambda), latoa(p4, lambda));
			}
			if (change == Type.OBJECTSTART)
			{
				return "Start change to object " + obj;
			}
			if (change == Type.OBJECTEND)
			{
				return "End change to object " + obj;
			}
			if (change == Type.OBJECTNEW)
			{
				return "Created new object " + obj;
			}
			if (change == Type.OBJECTKILL)
			{
				return "Deleted object " + obj;
			}
			if (change == Type.VARIABLENEW)
			{
				return "Created variable";
//				formatinfstr(infstr, M_(" Variable '%s' created on %s"), changedvariablename(p1, p2, p3),
//					describeobject(entryaddr, p1));
			}
			if (change == Type.VARIABLEKILL)
			{
				return "Deleted variable";
//				myvar.key = p2;  myvar.type = p4;  myvar.addr = p3;
//				formatinfstr(infstr, M_(" Variable '%s' killed on %s [was %s]"), changedvariablename(p1, p2, p4),
//					describeobject(entryaddr, p1), describevariable(&myvar, -1, -1));
			}
			if (change == Type.VARIABLEMOD)
			{
				return "Modified variable";
//				formatinfstr(infstr, M_(" Variable '%s[%ld]' changed on %s"), changedvariablename(p1, p2, p3), p4,
//					describeobject(entryaddr, p1));
//				if ((p3&VCREF) == 0)
//				{
//					myvar.key = p2;  myvar.type = p3 & ~VISARRAY;
//					myvar.addr = p5;
//					formatinfstr(infstr, M_(" [was '%s']"), describevariable(&myvar, -1, -1));
//				}
			}
			if (change == Type.VARIABLEINSERT)
			{
				return "Inserted variable";
//				formatinfstr(infstr, M_(" Variable '%s[%ld]' inserted on %s"), changedvariablename(p1, p2, p3), p4,
//					describeobject(entryaddr, p1));
			}
			if (change == Type.VARIABLEDELETE)
			{
				return "Deleted variable";
//				formatinfstr(infstr, M_(" Variable '%s[%ld]' deleted on %s"), changedvariablename(p1, p2, p3), p4,
//					describeobject(entryaddr, p1));
			}
			if (change == Type.DESCRIPTORMOD)
			{
				return "Modified Text Descriptor";
//				formatinfstr(infstr, M_(" Text descriptor on variable %s changed [was 0%lo/0%lo]"),
//					makename(p2), p4, p5);
			}
			return "?";
		}
	}

	/**
	 * ChangeBatch describes a batch of changes.
	 * There are no public methods or field variables.
	 */
	static class ChangeBatch
	{
		private List changes;
		private int batchNumber;
		private boolean done;
		private Tool tool;
		private String activity;

		ChangeBatch()
		{
		}

		void describe()
		{
			/* display the change batches */
			int nodeInst = 0, arcInst = 0, export = 0, cell = 0, object = 0, variable = 0;
			int batchSize = changes.size();
			for(int j = 0; j<batchSize; j++)
			{
				Change ch = (Change)changes.get(j);
				if (ch.getChange() == Type.NODEINSTNEW || ch.getChange() == Type.NODEINSTKILL || ch.getChange() == Type.NODEINSTMOD)
				{
					nodeInst++;
				} else if (ch.getChange() == Type.ARCINSTNEW || ch.getChange() == Type.ARCINSTKILL || ch.getChange() == Type.ARCINSTMOD)
				{
					arcInst++;
				} else if (ch.getChange() == Type.EXPORTNEW || ch.getChange() == Type.EXPORTKILL || ch.getChange() == Type.EXPORTMOD)
				{
					export++;
				} else if (ch.getChange() == Type.CELLNEW || ch.getChange() == Type.CELLKILL || ch.getChange() == Type.CELLMOD)
				{
					cell++;
				} else if (ch.getChange() == Type.OBJECTNEW || ch.getChange() == Type.OBJECTKILL)
				{
					object++;
				} else if (ch.getChange() == Type.VARIABLENEW || ch.getChange() == Type.VARIABLEKILL ||
					ch.getChange() == Type.VARIABLEMOD || ch.getChange() == Type.VARIABLEINSERT ||
					ch.getChange() == Type.VARIABLEDELETE)
				{
					variable++;
				} else if (ch.getChange() == Type.DESCRIPTORMOD)
				{
//					if ((VARIABLE *)c->p1 != NOVARIABLE) variable++; else
//						if ((PORTPROTO *)c->p3 != NOPORTPROTO) export++; else
//							nodeInst++;
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

	private static Type broadcasting = null;
	private static boolean doNextChangeQuietly = false;
	private static boolean doChangesQuietly = false;
	private static List changeCells = new ArrayList();
	private static ChangeBatch currentBatch = null;
	private static int maximumBatches = 20;
	private static int overallBatchNumber = 0;
	private static List doneList = new ArrayList();
	private static List undoneList = new ArrayList();

	/**
	 * Routine to start a new batch of changes.
	 * @param tool the tool that is producing the activity.
	 * @param activity a String describing the activity.
	 */
	public static void startChanges(Tool tool, String activity)
	{
		// close any open batch of changes
		endChanges();

		// kill off any undone batches
		noRedoAllowed();

		// erase the list of changed cells
		changeCells.clear();

		// allocate a new change batch
		currentBatch = new ChangeBatch();
		currentBatch.changes = new ArrayList();
		currentBatch.batchNumber = ++overallBatchNumber;
		currentBatch.done = true;
		currentBatch.tool = tool;
		currentBatch.activity = activity;

		// put at head of list
		doneList.add(currentBatch);

		// kill last batch if list is full
		if (doneList.size() > maximumBatches)
		{
			doneList.remove(0);
		}
	}

	/**
	 * Routine to terminate the current batch of changes.
	 */
	public static void endChanges()
	{
		// if no changes were recorded, stop
		if (currentBatch == null) return;

		// changes made: apply final constraints to this batch of changes
//		(*el_curconstraint->solve)(NONODEPROTO);

		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			if (tool.isOn()) tool.endBatch();
		}

		currentBatch = null;
	}

	/**
	 * Routine to record and broadcast a change.
	 * <P>
	 * These types of changes exist:
	 * <UL>
	 * <LI>NODEINSTNEW takes nothing.
	 * <LI>NODEINSTKILL takes nothing.
	 * <LI>NODEINSTMOD takes a1=oldLX a2=oldHX a3=oldLY a4=oldHY a5=oldRotation a6=oldTranspose.
	 * <LI>ARCINSTNEW takes nothing.
	 * <LI>ARCINSTKILL takes nothing.
	 * <LI>ARCINSTMOD takes a1=oldHeadX a2=oldHeadY a3=oldTailX a4=oldTailY a5=oldWidth a6=oldLength.
	 * <LI>EXPORTNEW takes nothing.
	 * <LI>EXPORTKILL takes nothing.
	 * <LI>EXPORTMOD takes a1=oldNodeInst a2=oldPortProto.
	 * <LI>CELLNEW takes nothing.
	 * <LI>CELLKILL takes nothing.
	 * <LI>CELLMOD takes a1=oldLowX a2=oldHighX a3=oldLowY a4=oldHighY.
	 * <LI>OBJECTSTART takes nothing.
	 * <LI>OBJECTEND takes nothing.
	 * <LI>OBJECTNEW takes nothing.
	 * <LI>OBJECTKILL takes nothing.
	 * <LI>VARIABLENEW takes a1=objtype(obsolete) a2=key a3=type.
	 * <LI>VARIABLEKILL takes a1=objtype(obsolete) a2=key a3=oldAddr a4=oldType a5=td[0] a6=td[1].
	 * <LI>VARIABLEMOD takes a1=objtype(obsolete) a2=key a3=type a4=index a5=oldValue.
	 * <LI>VARIABLEINSERT takes a1=objtype(obsolete) a2=key a3=type a4=index.
	 * <LI>VARIABLEDELETE takes a1=objtype(obsolete) a2=key a3=type a4=index a5=oldValue.
	 * <LI>DESCRIPTORMOD takes a1=objtype(obsolete) a2=key a3=type a4=td[0] a5=td[1].
	 * </UL>
	 * @param obj the object to which the change applies.
	 * @param change the change being recorded.
	 * @param a1 parameter 1 to the change.
	 * @param a2 parameter 2 to the change.
	 * @param a3 parameter 3 to the change.
	 * @param a4 parameter 4 to the change.
	 * @param a5 parameter 5 to the change.
	 * @param a6 parameter 6 to the change.
	 * @return the change object (null on error).
	 */
	public static Change newChange(ElectricObject obj, Type change, double a1, double a2, double a3, double a4, int a5, int a6)
	{
		if (currentBatch == null) return null;
		if (broadcasting != null)
		{
			System.out.println("Recieved " + change + " change during broadcast of " + broadcasting);
			return null;
		}

		// determine what needs to be marked as changed
		Change.setDirty(change, obj);

		// see if this is the first change
		boolean firstChange = false;
		if (currentBatch.changes.size() == 0) firstChange = true;

		// get change module
		Change ch = new Change(obj, change, a1, a2, a3, a4, a5, a6);
		// insert new change module into linked list
		currentBatch.changes.add(ch);

		// broadcast the change
		ch.broadcast(firstChange, false);
		return ch;
	}

	/**
	 * Routine to request that the next change be made "quietly".
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
	 */
	public static void setNextChangeQuiet() { doNextChangeQuietly = true; }

	/**
	 * Routine to request that the next change not be made "quietly".
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
	 */
	public static void clearNextChangeQuiet() { doNextChangeQuietly = false; }

	/**
	 * Routine to set the subsequent changes to be "quiet".
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
	 */
	public static void changesQuiet(boolean quiet) { doChangesQuietly = quiet; }

	/**
	 * Routine to tell whether changes are currently "quiet".
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
	 * @return true if changes are "quiet".
	 */
	public static boolean recordChange() { return !doNextChangeQuietly && !doChangesQuietly; }

	/**
	 * Routine to undo the last batch of changes.
	 * @return true if a batch was undone.
	 */
	public static boolean undoABatch()
	{
		// close out the current batch
		endChanges();

		// get the most recent batch of changes
		int listSize = doneList.size();
		if (listSize == 0) return false;
		ChangeBatch batch = (ChangeBatch)doneList.get(listSize-1);
		doneList.remove(listSize-1);
		undoneList.add(batch);

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
		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			if (tool.isOn()) tool.endBatch();
		}

		// mark that this batch is undone
		batch.done = false;
		return true;
	}

	/**
	 * Routine to redo the last batch of changes.
	 * @return true if a batch was redone.
	 */
	public static boolean redoABatch()
	{
		// close out the current batch
		endChanges();

		// get the most recent batch of changes
		int listSize = undoneList.size();
		if (listSize == 0) return false;
		ChangeBatch batch = (ChangeBatch)undoneList.get(listSize-1);
		undoneList.remove(listSize-1);
		doneList.add(batch);

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
		for(Iterator it = Tool.getTools(); it.hasNext(); )
		{
			Tool tool = (Tool)it.next();
			if (tool.isOn()) tool.endBatch();
		}

		// mark that this batch is redone
		batch.done = true;
		return true;
	}

	/**
	 * Routine to prevent undo by deleting all change batches.
	 */
	public static void noUndoAllowed()
	{
		// properly terminate the current batch
		endChanges();

		// kill them all
		doneList.clear();
		undoneList.clear();
	}

	/**
	 * Routine to prevent redo by deleting all undone change batches.
	 */
	public static void noRedoAllowed()
	{
		// properly terminate the current batch
		endChanges();

		undoneList.clear();
	}

	/**
	 * Routine to set the size of the history list and return the former size.
	 * @param newSize the new size of the history list (number of batches of changes).
	 * If not positive, the list size is not changed.
	 * @return the former size of the history list.
	 */
	public static int historyListSize(int newSize)
	{
		if (newSize <= 0) return maximumBatches;

		int oldSize = maximumBatches;
		maximumBatches = newSize;
		if (doneList.size() > maximumBatches)
		{
			doneList.remove(0);
		}
		return oldSize;
	}

	/**
	 * Routine to return a list of cells that have changed in this batch.
	 * @return an Interator over the list of changed cells.
	 */
	public static Iterator getChangedCells()
	{
		return changeCells.iterator();
	}

	/**
	 * Routine to display all changes.
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
