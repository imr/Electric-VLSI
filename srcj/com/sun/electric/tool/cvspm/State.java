/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: State.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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

package com.sun.electric.tool.cvspm;

import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 16, 2006
 * Time: 10:07:36 AM
 * The CVS state of a file
 * <P>
 * Specifies the state of a CVS file on disk, as a result of the CVS command
 * 'cvs -nq update' to get status, or 'cvs -q update' to do the update.
 * The former describes the action the latter would take (-n tells cvs not to
 * change any disk files).
 * <P>
 * A file for which no state is given as a result of this command is up-to-date.
 */
public class State implements Comparable {

    private static final List<State> statuses = new ArrayList<State>();
    public static final State UPDATE =   new State("U", "Needs Update", "Updated", 3);
    public static final State PATCHED =  new State("P", "Needs Patch",  "Patched", 4);
    public static final State ADDED =    new State("A", "Locally Added", "Added", 5);
    public static final State REMOVED =  new State("R", "Locally Removed", "Removed", 6);
    public static final State MODIFIED = new State("M", "Locally Modified", "Locally Modified", 2);
    public static final State CONFLICT = new State("C", "Conflicts With Repository", "Has Conflicts", 1);
    public static final State UNKNOWN =  new State("?", "Unknown", "Unknown", 7);
    public static final State NONE =     new State("",  "None", "None", 8);

    private final String key;
    private final String state;
    private final String updateResult;
    private final int sortkey;
    private State(String key, String state, String updateResult, int sortkey) {
        this.key = key;
        this.state = state;
        this.updateResult = updateResult;
        this.sortkey = sortkey;
        statuses.add(this);
    }
    public final String getState() { return state; }
    public final String getKey() { return key; }
    public final String getUpdateResult() { return updateResult; }

    public static State getState(String key) {
        for (State s : statuses) {
            if (s.getKey().equals(key)) return s;
        }
        return null;
    }

    public int compareTo(Object o) {
        State s = (State)o;
        if (this == s) return 0;
        if (sortkey < s.sortkey) return -1;
        return 1;
    }
}
