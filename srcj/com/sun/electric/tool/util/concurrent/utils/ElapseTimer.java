/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ElapseTimer.java
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
package com.sun.electric.tool.util.concurrent.utils;

import java.io.Serializable;

/**
 * @author Felix Schmidt
 * 
 */
public class ElapseTimer implements Serializable
{

	private long start = 0;
	private long end = 0;

	private long now() {
		return System.currentTimeMillis();
	}

	public long getStart() {
		return start;
	}

	public long getEnd() {
		return end;
	}

	public long getTime() {
		return end - start;
	}

	public static ElapseTimer createInstance() {
		return new ElapseTimer();
	}
	
	public static ElapseTimer createInstanceByValues(long start, long end) {
		ElapseTimer result = new ElapseTimer();
		result.start = start;
		result.end = end;	
		return result;
	}

	public ElapseTimer start() {
		this.start = now();
		return this;
	}

	public ElapseTimer end() {
		this.end = now();
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.getElapsedTime(getTime());
	}

	public void print(String preText) {
		System.out.println(preText + this.toString());
	}

	public String currentTimeString() {
		return this.getElapsedTime(currentTimeLong());
	}
	
	public long currentTimeLong() {
		return now() - start;
	}

	/**
	 * Method to describe a time value as a String.
	 * 
	 * @param milliseconds
	 *            the time span in milli-seconds.
	 * @return a String describing the time span with the format: days : hours :
	 *         minutes : seconds
	 */
	private String getElapsedTime(long milliseconds) {
		if (milliseconds < 60000) {
			// less than a minute: show fractions of a second
			return (milliseconds / 1000.0) + " secs";
		}
		StringBuffer buf = new StringBuffer();
		int seconds = (int) milliseconds / 1000;
		if (seconds < 0) {
			seconds = 0;
		}
		int days = seconds / 86400;
		if (days > 0) {
			buf.append(days + " days, ");
		}
		seconds = seconds - (days * 86400);
		int hours = seconds / 3600;
		if (hours > 0) {
			buf.append(hours + " hrs, ");
		}
		seconds = seconds - (hours * 3600);
		int minutes = seconds / 60;
		if (minutes > 0) {
			buf.append(minutes + " mins, ");
		}
		seconds = seconds - (minutes * 60);
		buf.append(seconds + " secs");
		return buf.toString();
	}
}
