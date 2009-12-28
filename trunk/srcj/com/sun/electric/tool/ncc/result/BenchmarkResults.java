/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BenchmarkResult.java
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

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

public class BenchmarkResults implements Serializable {
	private static final long serialVersionUID = 1L;
	public static enum BenchIdx { 
		MERGE_TIME,
		LOCAL_PARTITIONING_TIME, 
		HASH_CODE_PASS1_TIME, 
		HASH_CODE_PASS2_TIME,
		EXPORT_MATCHING_TIME,
		SIZE_MATCHING_TIME,
		SIZE_CHECKING_TIME,
		FORCED_PARTITION_TIME,
		EXPORT_CHECKING_TIME,
		RANDOM_MATCHING_TIME,
		FINAL_SWAP_TIME,
		ABSTRACTION_CONSTRUCTION_TIME,
		BACKTRACK_SPACE,
		BACKTRACK_GUESSES,
		SUB_NCC_TIME,
		SUB_NCC_COUNT,
		SCHREIER_SIMS_TIME,
		SCHREIER_SIMS_COUNT,
		QUICK_INTERCHANGE_TIME,
		QUICK_INTERCHANGE_COUNT,
		NEWBORNS_PROCESSED,
		MAX_MATRIX_SIZE,
		MAX_PORT_SIZE,
		SUM_OF_MATRIX_SIZES,
		NUMBER_OF_MATRICES,
		PASS_RESULT,
		FAIL_RESULT,
		MAYBE_RESULT,
		NUMBER_OF_VALUES;
	}
	public long[] results = new long[BenchIdx.NUMBER_OF_VALUES.ordinal()];
	
	
	public static class CellInfo implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public String name;
		public int[] matrixSizes;
	}
	public void computeFinalStatistics(){
		long maxMatrix=0;
		long maxPort=0;
		long sum=0;
		long count=0;
		for(CellInfo ci:info.values()){
			int numPorts = 0;
			int[] matrixSizes = ci.matrixSizes;
			for(int i=0;i<matrixSizes.length;i++){
				if(matrixSizes[i]>maxMatrix)maxMatrix=matrixSizes[i];
				numPorts+=matrixSizes[i];
				count++;
				
			}
			if(numPorts>maxPort)maxPort=numPorts;
			sum+=numPorts;
		}
		results[BenchIdx.MAX_MATRIX_SIZE.ordinal()]=maxMatrix;
		results[BenchIdx.MAX_PORT_SIZE.ordinal()]=maxPort;
		results[BenchIdx.SUM_OF_MATRIX_SIZES.ordinal()]=sum;
		results[BenchIdx.NUMBER_OF_MATRICES.ordinal()]=count;
	}
	public HashMap<String,CellInfo> info = new HashMap<String,CellInfo>();
	
	//public static BenchmarkResults globalBenchmarkResults = new BenchmarkResults();
	
	public BenchmarkResults(){
		super();
	}
	
	public BenchmarkResults(BenchmarkResults other){
		this.results = other.results.clone();
	}
	
	public void clearResults(){
		Arrays.fill(results,0);
		info = new HashMap<String,CellInfo>();
	}
	
	public void accumulateResults(BenchmarkResults other){
		for(int i=0;i<BenchIdx.NUMBER_OF_VALUES.ordinal();i++){
			this.results[i]+=other.results[i];
		}
		for(String key:other.info.keySet()){
			this.info.put(key, other.getInfo(key));
		}
	}
	public void normalizeResults(long div){
		for(int i=0;i<BenchIdx.NUMBER_OF_VALUES.ordinal();i++){
			this.results[i]/=div;
		}
		
	}
	
	public long get(BenchIdx idx){
		return idx==BenchIdx.NUMBER_OF_VALUES?results[idx.ordinal()]:0;
	}

	public void addInfo(String name, CellInfo newInfo){
		info.put(name, newInfo);
	}
	
	public CellInfo getInfo(String name){
		return info.get(name);
	}
	public long get(int idx){
		return idx<BenchIdx.NUMBER_OF_VALUES.ordinal()?results[idx]:-1;
	}
	public static void main(String[] args){
		
	}
}
