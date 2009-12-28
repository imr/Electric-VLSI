package com.sun.electric.tool.io.output;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.io.output.Topology.CellNetInfo;
import com.sun.electric.tool.io.output.Topology.CellSignal;

import java.util.ArrayList;
import java.util.List;

public class SpiceParasiticsGeneral
{
	/** key of wire capacitance. */	protected static final Variable.Key ATTR_C = Variable.newKey("ATTR_C");
    /** key of wire resistance. */	protected static final Variable.Key ATTR_R = Variable.newKey("ATTR_R");

    /** SpicePreferences. */        protected Spice.SpicePreferences localPrefs;
	/** List of segmented nets */	protected List<SpiceSegmentedNets> segmentedParasiticInfo;
	/** current segmented nets */	protected SpiceSegmentedNets curSegmentedNets;

	SpiceParasiticsGeneral(Spice.SpicePreferences localPrefs)
	{
        this.localPrefs =localPrefs;
		segmentedParasiticInfo = new ArrayList<SpiceSegmentedNets>();
	}

	public SpiceSegmentedNets initializeSegments(Cell cell, CellNetInfo cni, Technology layoutTechnology,
			SpiceExemptedNets exemptedNets, Topology.MyCellInfo info)
	{
		SpiceSegmentedNets segmentedNets = null;
		return segmentedNets;
	}

	public void writeSubcircuitHeader(CellSignal cs, StringBuffer infstr)
	{
	}

	public void getParasiticName(Nodable no, Network subNet, SpiceSegmentedNets subSegmentedNets, StringBuffer infstr)
	{
	}

	public SpiceSegmentedNets getSegmentedNets(Cell cell)
	{
		SpiceSegmentedNets segmentedNets = null;
		return segmentedNets;
	}

	public void backAnnotate()
	{
	}

	public void writeNewSpiceCode(Cell cell, CellNetInfo cni,Technology layoutTechnology, Spice out)
	{
	}
}
