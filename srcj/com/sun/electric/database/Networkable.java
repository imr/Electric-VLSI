package com.sun.electric.database;

/**
 * allows implementing classes to play nice with JNetworks
 */
public interface Networkable
{
	/**
	 * get the JNetwork associated with this object
	 */
	public JNetwork getNetwork();
}
