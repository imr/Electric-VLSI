/*
 * Created on Jan 31, 2005
 *
 */
package com.sun.electric.tool.simulation.eventsim.core.hierarchy;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;


/**
 *
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 * 
 * @author ib27688
 *
 * A non-functional entity. It serves as a parent for a group
 * of entities - it provides a light-weight structural hierarchy. 
 */
public class CompositeEntity extends Entity {
	
	/** the parent of this parent */
	// protected Entity parent;
	
	/** Members of this group */
	protected Set<Entity> members;

	/** Name for the group */
	// protected String name;
		
	public CompositeEntity(String name) {
		super(name);
		members= new HashSet<Entity>();
	} // CompositeEntity
	
	public CompositeEntity(String name, CompositeEntity g) {
		super(name, g);
		members= new HashSet<Entity>();
	} // CompositeEntity
	
		
	
	public void addMember(Entity e) {
		members.add(e);
		e.setParent(this);
	} // addMember
	
	public Iterator iterator() {
		return members.iterator();
	} // iterator
	
	public boolean selfCheck() {
		boolean OK= true;
		// director.info("Self check for group " + getName());
		Iterator mi= members.iterator();
		while (mi.hasNext()) {
			Entity e= (Entity)(mi.next());
			boolean thisCheck= e.selfCheck();
			OK= OK && thisCheck;
			if (!thisCheck) {
				director.journal("Self check failed for "
						+ e.getClass().getName() + " "
						+ e.getName() + ", id= " + e.getID());
			}
		}
		return OK;
	} // selfCheck
	
	/**
	 * Check whether an entity is a member of this composite entity
	 * @param e check for this entity
	 * @return true if e is a member of this entity or a member of a member
	 * that is a composite entity
	 */
	public boolean isMember(Entity e) {
		boolean check= false;
		for (Entity m : members) {
			// check for both composite and simple entities
			if (m instanceof CompositeEntity) {
				check= (m == e) || ((CompositeEntity)m).isMember(e);
			}
			else {
				check= (m == e);
			}
			if (check) break;
		}
		return check;
	} // isMember
	
	public void print() {
		director.info("Group " + getAlias() 
				+ ", id=" + getID() + ", contains:");
		Iterator mi= members.iterator();
		while(mi.hasNext()) {
			Entity e= (Entity)(mi.next());
			System.out.println(e);
		}		
	} // print
		
	/** initalize all members */
	public void init() {
		/* Iterator mi= members.iterator();
		while (mi.hasNext()) {
			Entity e= (Entity)(mi.next());
			e.init();
		}
		*/
	}
	
} // Parent
