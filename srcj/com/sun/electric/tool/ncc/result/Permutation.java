/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Permutation.java
 * Written by Stephen Friedman
 *
 * Copyright (c) 2007 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.ncc.result;

import java.util.Arrays;
/** Used by Port Interchange Experiment */
public class Permutation {
	private int[] perm;
	
	public Permutation(int size) {
		int[] permutation = new int[size];
		for(int i=0;i<size;i++) permutation[i]=i;
		perm = permutation;
	}

	public Permutation(int[] permutation){
		perm = permutation.clone();	
	}
	
	public Permutation(Permutation p){
		perm = p.perm.clone();
	}
	
	public int getPermTo(int i){
		return perm[i];
	}
	
	public int getPermFrom(int val){
		int i;
		for(i=0;i<perm.length;i++)if(perm[i]==val) break;
		return i;
	}
	public boolean isIdentity(){
		for(int i=0;i<perm.length;i++) if(perm[i]!=i) return false;
		return true;
	}
	public int size(){
		return perm.length;
	}
	
	public void set(int entry, int to){
		perm[entry] = to;
	}
	public Permutation product(Permutation h){
		Permutation g = this;
		return product(g,h);
	}
	
	public static Permutation product(Permutation g, Permutation h){
		int size = g.size();
		assert (h.size() == size); 
		Permutation newperm = new Permutation(size);
		for(int i=0;i<size;i++) newperm.set(i, h.getPermTo(g.getPermTo(i)));
		return newperm;
	}
	public Permutation inverse(){
		int[] invMap = new int[perm.length];
		for(int i=0; i<perm.length; i++){
			invMap[perm[i]] = i; 
		}
		return new Permutation(invMap);
	}

	@Override
	public String toString() {
		return Arrays.toString(perm);
	}

	@Override
	public boolean equals(Object arg0) {
		if(arg0 instanceof Permutation) {
			Permutation p = (Permutation) arg0;
			return Arrays.equals(p.perm,this.perm);
		}
		return false;
		
	}
	
}
