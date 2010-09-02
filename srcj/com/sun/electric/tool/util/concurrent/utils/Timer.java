/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Timer.java
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


/**
 * @author Felix Schmidt
 * 
 */
public class Timer {

	private long start = 0;
	private long end = 0;

	public static Timer createInstance() {
		return new Timer();
	}

	public void start() {
		this.start = System.currentTimeMillis();
	}

	public void end() {
		this.end = System.currentTimeMillis();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.getElapsedTime(end - start);
	}

	public void print(String preText) {
		System.out.println(preText + this.toString());
	}
	
	/**
     * Method to describe a time value as a String.
     * @param milliseconds the time span in milli-seconds.
     * @return a String describing the time span with the
     * format: days : hours : minutes : seconds
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
