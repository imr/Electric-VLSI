/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GenerateCSV.java
 * Written by Team 7: Felix Schmidt, Daniel Lechner
 * 
 * This code has been developed at the Karlsruhe Institute of Technology (KIT), Germany, 
 * as part of the course "Multicore Programming in Practice: Tools, Models, and Languages".
 * Contact instructor: Dr. Victor Pankratius (pankratius@ipd.uka.de)
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
package com.sun.electric.tool.placement.forceDirected2.utils.output;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Parallel Placement
 * 
 *         This class writes and appends data to csv files
 */
public class GenerateCSV {

	public static void appentAStringToFile(String fileName, String data) throws IOException {
		List<List<String>> dataOut = new ArrayList<List<String>>();
		dataOut.add(new ArrayList<String>());
		dataOut.get(0).add(data);
		GenerateCSV.appendToFile(fileName, dataOut);
	}

	public static void appendToFile(String fileName, List<List<String>> data) throws IOException {
		GenerateCSV generator = new GenerateCSV(fileName, ',');
		generator.setData(data);
		generator.appendToFile();
	}

	public static void writeToFile(String fileName, char seperator, List<List<String>> data) throws IOException {
		GenerateCSV generator = new GenerateCSV(fileName, seperator);
		generator.setData(data);
		generator.writeToFile();
	}

	public static void writeToFile(String fileName, List<List<String>> data) throws IOException {
		GenerateCSV generator = new GenerateCSV(fileName, ',');
		generator.setData(data);
		generator.writeToFile();
	}

	private List<List<String>> data;

	private String fileName;

	private char seperator;

	public GenerateCSV(String fileName, char seperator) {
		this.data = new ArrayList<List<String>>();
		this.fileName = fileName;
		this.seperator = seperator;
	}

	public synchronized void addRecord(List<String> data) {
		this.data.add(data);
	}

	public synchronized void appendToFile() throws IOException {

		List<String> in = new ArrayList<String>();
		if (new File(this.fileName).exists()) {
			Scanner scanner = new Scanner(new File(this.fileName));
			while (scanner.hasNextLine()) {
				in.add(scanner.nextLine());
			}
		}

		List<List<String>> out = new ArrayList<List<String>>();
		for (String s : in) {
			List<String> tmp = new ArrayList<String>();
			tmp.add(s);
			out.add(tmp);
		}

		out.addAll(this.data);

		this.data = out;

		this.writeToFile();
	}

	public synchronized void setData(List<List<String>> data) {
		this.data = data;
	}

	public synchronized void writeToFile() throws IOException {

		try {
			FileWriter writer = new FileWriter(this.fileName);

			for (List<String> record : this.data) {
				for (String value : record) {
					writer.append(value);
					writer.append(this.seperator);
				}
				writer.append('\n');
			}

			writer.flush();
			writer.close();

			// System.out.println("CSV file: " + this.fileName + " written");
		} catch (Exception ex) {
		}
	}
}
