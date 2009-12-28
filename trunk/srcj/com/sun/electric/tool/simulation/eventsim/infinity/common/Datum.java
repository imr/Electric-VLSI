package com.sun.electric.tool.simulation.eventsim.infinity.common;

import com.sun.electric.tool.simulation.eventsim.core.globals.Globals;

public class Datum {

	public static final int ADDRESS_BITS_DEF= 14;
	public static final String ADDRESS_BITS= "addressBits";

	public static final int DATA_BITS_DEF= 37;
	public static final String DATA_BITS= "addressBits";
	
	static Globals globals;
	
	static final int addressBits;
	static final int dataBits;
	
	static {
		globals= Globals.getInstance();
		Integer ab= globals.intValue(ADDRESS_BITS);
		if (ab!= null) addressBits= ab;
		else addressBits= ADDRESS_BITS_DEF;
		Integer db= globals.intValue(DATA_BITS);
		if (db!= null) dataBits= db;
		else dataBits= DATA_BITS_DEF;
	}
	
	public int address=0;
	public long data=0;
	
	public Datum(long d, int a) {
		data= d;
		address= a;
	}

	public Datum() {
		data= 0;
		address= 0;
	}
	
	/**
	 * Rotate address by k bits
	 * @param k
	 */
	public void rotateAddress(int k) {
		int mask= (1 << k) - 1; 	// lower n bits
		int bits= address & mask; 	// bits to be rotated
		address= (address >>> k) | (bits << (addressBits-k));
	}
	
	public void rotateAddress() {
		rotateAddress(1);
	}
	
	/** 
	 * Get k-th bit of the address
	 * @param k the bit we are looking at
	 * @return k-th bit of the address
	 */
	public int getAddressBit(int k) {
		return (address & (1 << k)) >> k;
	}

	
	public String toString() {
		String addrStr="";
		for (int i= addressBits-1; i>=0; i--) {
			addrStr+= getAddressBit(i);
		}
		
		return "[ addr= " + addrStr + ", data= " + data +" ]";
	}

	public static void main(String[] args) {
		Datum d= new Datum(10, 6);
		System.out.println(d);

		d.rotateAddress(2);
		System.out.println(d);
		
	}
	
} // class Datum
