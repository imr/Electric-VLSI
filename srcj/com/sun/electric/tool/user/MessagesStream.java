package com.sun.electric.tool.user;

import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.io.FileType;

import java.io.*;
import java.util.Observable;
import java.util.Observer;

/**
 * Class handles text sent to the Messages window.
 */
public class MessagesStream extends OutputStream
{
	private PrintWriter printWriter = null;
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

    static class MessagesObserver extends Observable
    {
        public void setChanged() {super.setChanged();}
        public void clearChanged() {super.clearChanged();}
    }

    private MessagesObserver notifyGUI = null;

    public MessagesStream()
    {
		// Force newline characters instead of carriage-return line-feed.
    	// This allows Unix and Windows log files to be identical.
		System.setProperty("line.separator", "\n");

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
			System.err.println("Error creating " + filePath);
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
        if (notifyGUI == null) return;
        
        notifyGUI.setChanged();
        notifyGUI.notifyObservers(str);
        notifyGUI.clearChanged();
	}
}
