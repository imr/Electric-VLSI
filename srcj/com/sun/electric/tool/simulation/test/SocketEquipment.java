/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SocketEquipment.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation.test;

import java.io.*;
import java.net.*;

/**
 * For equipment that communicates via socket interface.
 */
public class SocketEquipment implements EquipmentInterface {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public SocketEquipment(Socket socket){
	this.socket=socket;
	try {
	    out=new PrintWriter(socket.getOutputStream(),true);
	    in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
	} catch (Throwable t) {
	    t.printStackTrace();
	}
    }

    public void write(String data){
	out.println(data);
    }

    public String read(int length){
	char[] cbuf = new char[length];
	try {
	    in.read(cbuf,0,length);
	} catch (Throwable t) {
	    t.printStackTrace();
	}
	return new String(cbuf);
    }
    
    public String readLine(){
	String ret = null;
	try {
	    ret=in.readLine();
	} catch (Throwable t) {
	    t.printStackTrace();
	}
	return ret;
    }
}
