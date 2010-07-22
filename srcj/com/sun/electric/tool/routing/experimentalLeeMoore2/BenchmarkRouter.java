/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BenchmarkRouter.java
 * Written by: Alexander Herzog (Team 4)
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.routing.experimentalLeeMoore2;

import java.util.ArrayList;
import java.util.List;

import com.sun.electric.tool.routing.RoutingFrame;

public class BenchmarkRouter extends RoutingFrame {
	
	public RoutingParameter numThreads = new RoutingParameter("threads", "Number of threads to use:", 4);
	public RoutingParameter maxRuntime = new RoutingParameter("runtime", "Maximum runtime (seconds):", 300);
	public RoutingParameter enableOutput = new RoutingParameter("output", "Enable console output", false);

	List<RoutingParameter> allParameters = new ArrayList<RoutingParameter>();

	public BenchmarkRouter() {
		allParameters.add(numThreads);
		allParameters.add(maxRuntime);
		allParameters.add(enableOutput);
	}
	
	public String getAlgorithmName() {
		return "BenchmarkRouter";
	}
	
	public List<RoutingParameter> getParameters() {
		return allParameters;
	}
	
	public void setBenchmarkParameters(int threads, int runtime) {
		numThreads.setTempIntValue(threads);
		maxRuntime.setTempIntValue(runtime);
	}

}
