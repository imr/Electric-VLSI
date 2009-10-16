/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MessagesStream.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user;

import com.sun.electric.Main;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.io.FileType;

import java.io.OutputStream;
import java.io.PrintStream;


/**
 * Class handles text sent to the Messages window.
 */
public class MessagesStream extends PrintStream
{
    /** The messages stream */                              private static MessagesStream messagesStream;

    private static void initializeMessageStream()
    {
        if (messagesStream == null)
            messagesStream = new MessagesStream();
    }

    /**
     * Method to return messages stream.
     * @return the messages stream.
     */
    public static MessagesStream getMessagesStream()
    {
        initializeMessageStream();
        return messagesStream;
    }

    public MessagesStream()
    {
        super(new OutputStream() {
            @Override public void write(int c) {throw new UnsupportedOperationException(); }
        });
		// Force newline characters instead of carriage-return line-feed.
    	// This allows Unix and Windows log files to be identical.
		System.setProperty("line.separator", "\n");

    	System.setOut(this);
    }

    @Override public void flush() {}
    @Override public void close() { throw new UnsupportedOperationException(); }
    @Override public boolean checkError() { throw new UnsupportedOperationException(); }
	@Override public void write(byte[] b) { print(new String(b)); }
	@Override public void write(int b) { print((char)b); }
	@Override public void write(byte[] b, int off, int len) { print(new String(b, off, len)); }

    @Override public void print(boolean b) { print(b ? "true" : "false", false); }
    @Override public void print(char c) { print(String.valueOf(c), false); }
    @Override public void print(int i) { print(String.valueOf(i), false); }
    @Override public void print(long l) { print(String.valueOf(l), false); }
    @Override public void print(float f) { print(String.valueOf(f), false); }
    @Override public void print(double d) { print(String.valueOf(d), false); }
    @Override public void print(char s[]) { print(String.valueOf(s), false); }
    @Override public void print(String s) { print(s != null ? s : "null", false); }
    @Override public void print(Object obj) { print(String.valueOf(obj), false); }

    @Override public void println() { print("", true); }
    @Override public void println(char c) { print(String.valueOf(c), true); }
    @Override public void println(int i) { print(String.valueOf(i), true); }
    @Override public void println(long l) { print(String.valueOf(l), true); }
    @Override public void println(float f) { print(String.valueOf(f), true); }
    @Override public void println(double d) { print(String.valueOf(d), true); }
    @Override public void println(char s[]) { print(String.valueOf(s), true); }
    @Override public void println(String s) { print(s != null ? s : "null", true); }
    @Override public void println(Object obj) { print(String.valueOf(obj), true); }

    private void print(String s, boolean newLine) {
        if (Main.isBatch()) {
            if (newLine) Main.UserInterfaceDummy.stdout.println(s);
            else         Main.UserInterfaceDummy.stdout.print(s);
        } else {
            UserInterface ui = Job.getUserInterface();
            if (ui != null) {
                ui.printMessage(s, newLine);
                return;
            }
            ui = Job.getExtendedUserInterface();
            if (ui != null) {
                ui.printMessage(s, newLine);
                return;
            }
            if (newLine) {
                System.err.println(s);
            } else {
                System.err.print(s);
            }
        }
    }

	/**
	 * Method to start saving the messages window.
	 */
	public void save()
	{
		save(OpenFile.chooseOutputFile(FileType.TEXT, null, "emessages.txt"));
	}

	public void save(String filePath) {
        Job.getUserInterface().saveMessages(filePath);
	}
}
