package com.sun.electric.technology;

public class EdgeV
{
	double multiplier;
	double adder;

	private EdgeV(double multiplier, double adder)
	{
		this.multiplier = multiplier;
		this.adder = adder;
	}

	public static final EdgeV BottomEdge = new EdgeV(-0.5, 0.0);
	public static final EdgeV TopEdge = new EdgeV(0.5, 0.0);
	public static final EdgeV AtCenter = new EdgeV(0.0, 0.0);

	public double getMultiplier() { return multiplier; }
	public double getAdder() { return adder; }

	public static EdgeV fromTop(double amt)
	{
		return new EdgeV(0.5, -amt);
	}
	
	public static EdgeV fromBottom(double amt)
	{
		return new EdgeV(-0.5, amt);
	}
	
	public static EdgeV fromCenter(double amt)
	{
		return new EdgeV(0.0, amt);
	}
}
