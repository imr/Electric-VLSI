package com.sun.electric.database;

public class EdgeH
{
	double multiplier;
	double adder;

	private EdgeH(double multiplier, double adder)
	{
		this.multiplier = multiplier;
		this.adder = adder;
	}

	public static final EdgeH LeftEdge = new EdgeH(-0.5, 0.0);
	public static final EdgeH RightEdge = new EdgeH(0.5, 0.0);

	public double getMultiplier() { return multiplier; }
	public double getAdder() { return adder; }

	public static EdgeH FromLeft(double amt)
	{
		return new EdgeH(-0.5, amt);
	}

	public static EdgeH FromRight(double amt)
	{
		return new EdgeH(0.5, -amt);
	}

	public static EdgeH FromCenter(double amt)
	{
		return new EdgeH(0.0, amt);
	}
}
