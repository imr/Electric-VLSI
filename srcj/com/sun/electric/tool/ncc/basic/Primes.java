/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Primes.java
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

package com.sun.electric.tool.ncc.basic;

import java.util.ArrayList;

/**
 * Generate Prime numbers 
 */
public class Primes {
	private static int trial = 3;
	private static ArrayList<Integer> primes = new ArrayList<Integer>();
	static {
		primes.add(new Integer(2));
	}
	private static void findNextPrime() {
		while (true) {
			for (int i=0; i<primes.size(); i++) {
				int prime = ((Integer)primes.get(i)).intValue();
				int r = trial % prime;
				if (r==0) break;	// trial not prime
				int q = trial / prime;
				// Knuth. Fundamental Algorithms. pp 142
				if (q<=prime) {
					primes.add(new Integer(trial));
					trial += 2;
					return;
				}
			}
			trial += 2;
		}
	}
	public static int get(int nth) {
		while (primes.size()-1<nth) findNextPrime();

		return ((Integer) primes.get(nth)).intValue();
	}
}
