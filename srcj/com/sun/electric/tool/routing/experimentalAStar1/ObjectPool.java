/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ObjectPool.java
 * Written by: Christian Julg, Jonas Thedering (Team 1)
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.routing.experimentalAStar1;

/**
 * Implements object pooling by collecting unneeded objects and 
 * handing them out later
 * 
 * @author Jonas Thedering
 * @author Christian Jülg
 */
public class ObjectPool<T extends Poolable<T>> {
	private Class<T> instanceCreator;
	private T listHead = null;
	
	/** @param instanceCreator The class object for class T */
	public ObjectPool(Class<T> instanceCreator) {
		this.instanceCreator = instanceCreator;
	}
	
	public T alloc() {
		if(listHead == null)
			try {
				//Can't use "new T()", because generics don't allow it
				return instanceCreator.newInstance();
			}
			catch(InstantiationException e) {
				return null;
			}
			catch(IllegalAccessException e) {
				return null;
			}
		else {
			T temp = listHead;
			listHead = listHead.getTail();
			return temp;
		}
	}
	
	public void free(T object) {
		object.setTail(listHead);
		listHead = object;
	}
	
	// Assumes that setTail was used to make a list of poolables from head to last
	public void freeAllLinked(T head, T last) {
		last.setTail(listHead);
		listHead = head;
	}
}
