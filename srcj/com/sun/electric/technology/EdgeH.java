package com.sun.electric.technology;

public class EdgeH
{
	double multiplier;
	double adder;

	public EdgeH(double multiplier, double adder)
	{
		this.multiplier = multiplier;
		this.adder = adder;
	}

	public static final EdgeH LEFTEDGE = new EdgeH(-0.5, 0.0);
	public static final EdgeH RIGHTEDGE = new EdgeH(0.5, 0.0);
	public static final EdgeH CENTER = new EdgeH(0.0, 0.0);

	public double getMultiplier() { return multiplier; }
	public double getAdder() { return adder; }

	public static EdgeH fromLeft(double amt)
	{
		return new EdgeH(-0.5, amt);
	}

	public static EdgeH fromRight(double amt)
	{
		return new EdgeH(0.5, -amt);
	}

	public static EdgeH fromCenter(double amt)
	{
		return new EdgeH(0.0, amt);
	}
}
