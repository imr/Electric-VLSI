/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EPoint.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

package com.sun.electric.database.geometry;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * The <code>ECoord</code> immutable class defines a point in Electric database.
 * Coordiates are snapped to grid according to <code>DBMath.round</code> method.
 */
class ECoord implements Serializable {
    /**
     * True to gather search values.
     */
    private static final boolean GATHER_SEARCHES = false;
    
    /**
     * Value in grid units.
     */
    private final long gridValue;
    /**
     * Value in lambda units.
     */
    private final transient double lambdaValue;
    /**
     * True if coordinate is "small int"
     */
    private final boolean isSmall;
    
    private static ECoord[] hash = new ECoord[2];
    private static int hashSize = 0;
    private static final float LOAD_FACTOR = 0.75f;
    
    // Statistics
    private static int numSearches, numConflicts, numRetries;
    private static long[] searches = new long[1];
    
    /**
     * Zero coordinate value.
     */
    static final ECoord ZERO = fromGrid(0);
    
    /** Creates a new instance of ECoord */
    private ECoord(long gridValue) {
        this.gridValue = gridValue;
        lambdaValue = DBMath.gridToLambda(gridValue);
        isSmall = GenMath.isSmallInt(gridValue);
    }
    
    private Object readResolve() throws ObjectStreamException {
        return fromGrid(gridValue);
    }
    
    /**
     * Returns ECoord value in lambda units.
     * @return ECoord value in lambda units.
     */
    double lambdaValue() { return lambdaValue; }
    
    /**
     * Returns ECoord value in grid units.
     * @return ECoord value in grid units.
     */
    long gridValue() { return gridValue; }

    /**
     * Returns true if ECoord value in grid units is a "small int".
     * @return true if ECoord value in grid units is a "small int".
     * @See com.sun.electric.database.geometry.GenMath.MIN_SMALL_INT
     * @See com.sun.electric.database.geometry.GenMath.MAX_SMALL_INT
     */
    boolean isSmall() { return isSmall; }
    
    /**
     * Returns <code>ECoord</code> from specified lambda value
     * snapped to the grid.
     * @param v specified lambda value
	 * @return Snapped ECoord
     */
	static ECoord fromLambda(double lambdaValue) {
        return fromGrid(DBMath.lambdaToGrid(lambdaValue));
	}
    
    /**
     * Returns <code>ECoord</code> from specified double value
     * snapped to the grid.
     * @param v specified double value
	 * @return Snapped ECoord
     */
    static ECoord fromGrid(long gridValue) {
        if (GATHER_SEARCHES)
            putSearch(gridValue);
        numSearches++;
        ECoord[] hashCopy = hash;
        int hashCode = hash((int)gridValue);
        int i = hashCode & (hash.length - 1);
        ECoord c = hashCopy[i];
        if (c != null) {
            if (c.gridValue == gridValue) return c;
            int h2 = h2(hashCode);
            for (;;) {
                numConflicts++;
                i = (i + h2) & (hash.length - 1);
                c = hashCopy[i];
                if (c == null) break;
                if (c.gridValue == gridValue) return c;
            }
        }
        synchronized (ECoord.class) {
            if (hashCopy != hash || hash[i] != null) {
                numRetries++;
                return fromGrid(gridValue); // retry after synchronization
            }
            hashSize++;
            ECoord result = hash[i] = new ECoord(gridValue);
            if (hash.length*LOAD_FACTOR < hashSize)
                rehash();
            return result;
        }
    }
    
	/**
	 * Returns a <code>String</code> that represents the value 
	 * of this <code>ECoord</code>.
	 * @return a string representation of this <code>ECoord</code>.
	 */
    @Override
	public String toString() {
	    return Double.toString(lambdaValue);
	}

    private static void rehash() {
        ECoord[] newHash = new ECoord[hash.length*2];
        int mask = newHash.length - 1;
        for (ECoord c: hash) {
            if (c == null) continue;
            int hash = hash((int)c.gridValue);
            int h2 = h2(hash);
            int i = hash & mask;
            while (newHash[i] != null)
                i = (i + h2) & mask;
            newHash[i] = c;
        }
        hash = newHash;
    }
    
    static int hash(int h) {
        h += ~(h << 9);
        h ^=  (h >>> 14);
        h +=  (h << 4);
        h ^=  (h >>> 10);
        return h;
    }
    
    static int h2(int h) {
        return 1;
//        return (h >> 23) & 0x1FF | 1;
    }
    
    private static synchronized void putSearch(long gridValue) {
        if (numSearches == searches.length) {
            long[] newSearches = new long[numSearches*2];
            System.arraycopy(searches, 0, newSearches, 0, numSearches);
            searches = newSearches;
        }
        searches[numSearches] = gridValue;
    }
    
    static void dumpSearches(DataOutputStream out) throws IOException {
        for (int i = 0; i < numSearches; i++)
            out.writeLong(searches[i]);
        printStatistics();
    }
    
    static void printStatistics() {
		System.out.println("ECoord: numSearches=" + numSearches  +
                " numConflicts=" + numConflicts + "(" + (numConflicts*100/numSearches) + "%) numInserts=" + hashSize + " hashSize=" + hash.length);
        if (numRetries > 0)
            System.out.println(numRetries + " RETRIES !!!");
    }
    
    private static void testHash(ArrayList<Long> input) {
        long startTime = System.currentTimeMillis();
        for (Long l: input) {
            fromLambda(l.doubleValue());
//            valueOf(l.longValue());
        }
        long stopTime = System.currentTimeMillis();
        int count = 0;
        for (ECoord c: hash) {
            if (c == null) continue;
            assert c == fromGrid(c.gridValue);
            count++;
        }
        assert count == hashSize;
        printStatistics();
        System.out.println("t=" + (stopTime - startTime));
    }
    
    public static void main(String[] args) {
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream("ecoord.dat")));
            ArrayList<Long> input = new ArrayList<Long>(); 
            while (in.available() > 0)
                input.add(Long.valueOf(in.readLong()));
            in.close();
            for (int i = 0; i < 2; i++) {
                System.gc();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {}
            }
            testHash(input);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
