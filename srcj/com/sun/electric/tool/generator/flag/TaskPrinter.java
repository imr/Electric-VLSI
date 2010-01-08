package com.sun.electric.tool.generator.flag;

import com.sun.electric.tool.Job;

/** TaskPrinter will describe the task at hand only if I need to report
 * that something went wrong. */ 
public class TaskPrinter {
	private final StringBuffer taskDescription = new StringBuffer();
	private boolean taskDescriptionPrinted = false;
	private void printTaskDescription() {
		if (taskDescriptionPrinted) return;
		System.out.println(taskDescription.toString());
		taskDescriptionPrinted = true;
	}
	public void saveTaskDescription(String msg) {
		taskDescription.setLength(0);
		taskDescriptionPrinted = false;
		taskDescription.append(msg);
	}
	public void clearTaskDescription() {
		taskDescriptionPrinted = false;
	}
	public void prln(String s) {
		printTaskDescription();
		System.out.println(s);
	}
	public void pr(String s) {
		printTaskDescription();
		System.out.print(s);
	}
	public void error(boolean cond, String msg) {
		if (cond) {
			printTaskDescription();
			Job.error(true, msg);
		}
	}
}
