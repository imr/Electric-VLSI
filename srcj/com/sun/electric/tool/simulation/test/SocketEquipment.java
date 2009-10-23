package com.sun.electric.tool.simulation.test;

import java.io.*;
import java.net.*;

/**
 * For equipment that communicates via socket interface.
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
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
