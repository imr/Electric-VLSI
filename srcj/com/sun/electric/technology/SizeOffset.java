package com.sun.electric.technology;

// In Electric, nonexistant "invisible" material surrounds many
// NodeProtos and ArcProtos.  For example, in the MOCMOS technology,
// if you ask Electric for a 5 square metal-1/metal-2 contact
// you get a 4 square metal-1 and 4 square metal-2
// surrounded by a 1/2 "invisible" perimeter.  This perimeter
// doesn't scale.  If you ask for an 8 square metal-1/metal-2
// contact you get a 9 square metal-1 and metal-2 and a 1/2
// surround.
//
// This invisible surround is a pain for a Jose client to deal
// with. My goal is to hide this from Jose client programs. 
//
// This class encodes the dimensions of the invisible surround.
// NodeProtos and ArcProtos need to pass this information to Geometric
// so it may properly compute NodeInst and ArcInst bounding boxes
// without this ridiculous invisible surround.

class SizeOffset
{
	final double lx, ly, hx, hy;
	SizeOffset(double lx, double ly, double hx, double hy)
	{
		this.lx = lx;
		this.ly = ly;
		this.hx = hx;
		this.hy = hy;
	}

	public String toString()
	{
		return "SizeOffset = {\n"
			+ "    x: [" + lx + "-" + hx + "]\n"
			+ "    y: [" + ly + "-" + hy + "]\n"
			+ "}\n";
	}
}
