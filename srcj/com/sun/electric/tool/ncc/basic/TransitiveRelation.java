/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TransitiveRelation.java
 *
 * Copyright (c) 2003 Sun Microsystems and Free Software
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

package com.sun.electric.tool.ncc.basic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The TransitiveRelation object is given pairs of objects
 * that are related. Relationship is presumed to be transitive.
 * The TransitiveRelation object can then return maximal sets of
 * related objects.
 * For example, if Tom is related to Sally, John is related to Tom, 
 * and Fred is related to Paul then TransitiveRelation returns
 * {Tom, Sally, John} and {Fred, Paul}.
 */
public class TransitiveRelation<T> {
	private Map<T,HashSet<T>> objToRelated = new HashMap<T,HashSet<T>>();
	public void theseAreRelated(T o1, T o2) {
		HashSet<T> s1 = objToRelated.get(o1);
		HashSet<T> s2 = objToRelated.get(o2);
		if (s1==null && s2==null) {
			// no sets
			HashSet<T> related = new HashSet<T>();
			related.add(o1); related.add(o2);
			objToRelated.put(o1, related);
			objToRelated.put(o2, related);
		} else if (s2==null) {
			// s1 not null but s2 null
			s1.add(o2);
			objToRelated.put(o2, s1);
		} else if (s1==null) {
			// s1 null but s2 not null
			s2.add(o1);
			objToRelated.put(o1, s2);
		} else {
			// s1 and s2 not null
			if (s1==s2) return;
			if (s1.size()>s2.size()) {
				// merge s2 into s1
				s1.addAll(s2);
				for (Iterator<T> it=s2.iterator(); it.hasNext();) 
					objToRelated.put(it.next(), s1);					
			} else {
				// merge s1 into s2
				s2.addAll(s1);
				for (Iterator<T> it=s1.iterator(); it.hasNext();) 
					objToRelated.put(it.next(), s2);					
			}
		}
	}
	/** Return an Iterator over Sets of related Objects. */
	public Iterator<Set<T>> getSetsOfRelatives() {
		Set<Set<T>> s = new HashSet<Set<T>>();
		s.addAll(objToRelated.values());
		return s.iterator();
	}
}
