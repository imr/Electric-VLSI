package com.sun.electric.tool.user;

import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.io.FileType;

import java.io.*;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda
 * Date: Sep 25, 2005
 * Time: 4:20:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class MessagesStream
	extends OutputStream
{
	private PrintWriter printWriter = null;

    static class MessagesObserver extends Observable
    {
        public void setChanged() {super.setChanged();}
        public void clearChanged() {super.clearChanged();}
    }

    private MessagesObserver notifyGUI = null;

    public MessagesStream()
    {
		System.setOut(new java.io.PrintStream(this));
        notifyGUI = new MessagesObserver();
    }

    public void addObserver(Observer o)
    {
        notifyGUI.addObserver(o);
    }

	public void write(byte[] b)
	{
		appendString(new String(b));
	}

	public void write(int b)
	{
		appendString(String.valueOf((char) b));
	}

	public void write(byte[] b, int off, int len)
	{
		appendString(new String(b, off, len));
	}

	private static boolean newCommand = true;
	private static int commandNumber = 1;

    /**
     * Method to report that the user issued a new command (click, keystroke, pulldown menu).
     * The messages window separates output by command so that each command's results
     * can be distinguished from others.
     */
    public static void userCommandIssued()
    {
        newCommand = true;
    }

	/**
	 * Method to start saving the messages window.
	 */
	public void save()
	{
		save(OpenFile.chooseOutputFile(FileType.TEXT, null, "emessages.txt"));
	}

	public void save(String filePath) {
		if (filePath == null) return;
		try
		{
			printWriter = new PrintWriter(new BufferedWriter(new FileWriter(filePath)));
		} catch (IOException e)
		{
			System.out.println("Error creating " + filePath);
			return;
		}
		System.out.println("Messages will be saved to " + filePath);
	}

	protected void appendString(String str)
	{
        if (str.equals("")) return;
        if (newCommand)
		{
			newCommand = false;
			str = "=================================" + (commandNumber++) + "=================================\n" + str;
		}

        if (printWriter != null)
        {
            printWriter.print(str);
            printWriter.flush();
        }
        notifyGUI.setChanged();
        notifyGUI.notifyObservers(str);
        notifyGUI.clearChanged();
	}
}
