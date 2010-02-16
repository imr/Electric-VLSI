/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DialogJob.java
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
package com.sun.electric.tool;

import com.sun.electric.database.*;
import com.sun.electric.tool.Job.Type;
import com.sun.electric.tool.*;
import com.sun.electric.tool.user.*;
import com.sun.electric.tool.user.dialogs.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 *  A Job which takes parameters; the parameters may be specified
 *  programmatically or via a dialog box which is constructed on the
 *  fly using reflection.  In the future this class will also handle
 *  the drudgery of remembering the last values in all of the dialog's
 *  fields so the programmer doesn't have to write that code by hand.
 *
 *  Much like emacs, most of Electric's functionality can be invoked
 *  in two different ways: programmatically from scripts/code, or
 *  interactively via a graphical user interface.  It is rather
 *  tedious to have to maintain these two interfaces separately and
 *  keeping them consistent is error-prone.  Therefore, the DialogJob
 *  class lets a programmer write only the programmatic interface (the
 *  Job's constructor), annotate it appropriately, and have the
 *  graphical user interface be generated automatically (like emacs
 *  does).  The added advantage is that the dialog box acts as
 *  documentation for the scripting API: a script-writer who wants to
 *  know how to script some feature need only look at the dialog box's
 *  field labels in order to know how to invoke that feature
 *  programmatically.
 *
 *  This code will scan the public fields of the class in which it
 *  finds itself (subclasses are expected to add fields); any field
 *  whose name both begins and ends with an underscore is treated as a
 *  dialog field.  The type of user interface component to instantiate
 *  is determined by the field's type; the following types are
 *  understood, any other type is illegal:
 *
 *    String - a JTextArea
 *    int    - a JTextArea
 *    long   - a JTextArea
 *    float  - a JTextArea
 *    double - a JTextArea
 *    File   - a button which pops up a "choose file" dialog
 *
 *  There are a few special fields which are used to indicate that the
 *  Job is only meaningful when the current window is looking at a
 *  particular kind of thing.  Error handling (for the case where the
 *  user invokes the Job in another situation) is handled in this
 *  class so that it need not be repeated.
 *
 *    public Cell           _thisCell_;
 *    public WaveformWindow _thisWaveformWindow_;
 *
 *  @author Adam Megacz <adam.megacz@sun.com>
 */
public abstract class DialogJob extends Job {

    public DialogJob(String jobName, Job.Type jobType) {
        super(jobName, User.getUserTool(), jobType, null, null, Job.Priority.USER);
        /*
        new DialogJobDialog().show();
        */
    }

    private class DialogJobDialog extends EDialog {
        public DialogJobDialog() {
            super(/*parentFrame*/null, false);
        }
        public void submit() {
            DialogJob.this.startJob();
        }
    }


    /** subclasses should override this */
    public abstract boolean doIt() throws JobException;

}
