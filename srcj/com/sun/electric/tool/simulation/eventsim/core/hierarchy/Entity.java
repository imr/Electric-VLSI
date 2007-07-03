/**
 * Copyright 2005 Sun Microsystems Inc. All rights reserved. Use is subject to license terms.
 */
package com.sun.electric.tool.simulation.eventsim.core.hierarchy;


import com.sun.electric.tool.simulation.eventsim.core.common.IDGenerator;
import com.sun.electric.tool.simulation.eventsim.core.engine.Director;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.globals.Globals;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.Entities;

/**
 *
 * Base class for all entities modeled.
 * Covers basic roperies such as name, ID.
 * The class is not public as it is not intended to be used directly: 
 * Instead, derived classes are exposed as public.
 * 
 * @author ib27688
 */
public abstract class Entity {

	/** access to parameter values */
	protected static Globals globals= Globals.getInstance();	
	/** director */
	protected static Director director= Director.getInstance();
	
	
	/** entity name */
	protected String name;

	/**
	 *  An entity may be known under an alias.
	 * TODO consider a set of aliases.
	 */
	protected String alias;
	
	/** an indication whether to journal activity of this entity, off by default */
	protected boolean journalActivity= false;
	
	
	/** entity id, unique */
	protected long id;
	
	/** the parent Entity - it has to be a CompositeEntity */
	protected CompositeEntity parent;
	

	protected Entity(String n) {
		name= n;
		alias= name;
		id= IDGenerator.newID();
		Entities.add(this);
		parent= null;
	} // Entity constructor
	
	
	protected Entity(String name, CompositeEntity g) {
		this(name);
		parent= g;
	} // Entity constructor
	
	/** accessor for the name */
	public String getBaseName() {
		return name;
	} // getBaseName
	

	/** set the name */
	void setName(String n) {
		// if alias was the same as the name, change alias as well
		if (alias.equals(name)) alias= n;
		name= n;
	} // setName
	
	public void setAlias(String n) {
		alias= n;
	} // set alias
	
	public String getName() {
		if (parent == null) {
			return getBaseName();
		}
		else {
			return parent.getName() + "." + getBaseName();
		}
	} // getFullName
	
	public String getAlias() {
		if (parent == null) {
			return alias;
		}
		else {
			return parent.getAlias() + "." + alias;
		}		
	} // getAlias

	/** accessor for the alias */
	public String getBaseAlias() {
		return alias;
	} // getBaseAlias
		
	/** turn journaling on or off */
	public void setJournalActivity(boolean flag) {
		journalActivity= flag;
	} // setJournalActivity
	
	public boolean getJournalActivity() {
		return journalActivity;
	} // getJournalActivity
	
	public void setParent(CompositeEntity p) {
		parent= p;
		// add yourself to the set of members if not already there
		if (!p.isMember(this)) {
			p.addMember(this);
		}
	} // setParent
	
	/** accessor for the id */
	public long getID() {
		return id;
	} //getID
	
	public String toString() {
		return name + ", id= " + id;
	} // toString
	
	public abstract boolean selfCheck();
	
	// take out, toString suffices 
	// public abstract void print();
	
	/** initalize the entity 
	 * @throws EventSimErrorException */
	public abstract void init() throws EventSimErrorException;
	
	public void logEvent(String message) {
		director.journal(getAlias() + ", " + getID() + ": " + message);
	} // logEvent
	
	public void logError(String message) {
		director.error(getAlias() + ", " + getID() + ": " + message);		
	} // logError
	
	public void logInfo(String message) {
		director.info(getAlias() + ", " + getID() + ": " + message);
	} // logInfo
	
	/**
	 * Log the error and stop the simulation
	 * @param message error message
	 */
	public void fatalError(String message) throws EventSimErrorException {
		director.fatalError(getAlias() + ", " + getID() + ": " + message);
	} // fatalError
	
} // class GaspEntity
