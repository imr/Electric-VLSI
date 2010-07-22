/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Array2D.java
 * Written by: Christian Harnisch, Ingo Besenfelder, Michael Neumann (Team 3)
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
package com.sun.electric.tool.routing.experimentalAStar3.datastructures;

import java.util.Vector;

public class Array2D<T>
{
    private int wx, wy;
    private int sz;
    private Vector<T> field;
    private T defaultValue;

    public Array2D(int wx, int wy, T defaultValue, boolean fillWithDefault)
    {
      this.wx = wx;
      this.wy = wy;
      this.sz = this.wx * this.wy;
      this.defaultValue = defaultValue;
      
      this.field = new Vector<T>(this.sz);
      this.field.setSize(this.sz);
      
      if (fillWithDefault)
        fillWith(defaultValue);
    }
    
    public boolean inRange(int x, int y)
    {
      return (x >= 0 && y >= 0 && x < this.wx && y < this.wy);
    }
    
    /*
     * Return -1 on error.
     */
    private int getOff(int x, int y)
    {
      if (!inRange(x, y))
        return -1;
      int off = x + y * this.wx;
      assert (off >= 0 && off < this.sz);
      return off;
    }

    public T getAt(int x, int y)
    {
      int off = getOff(x, y);
      if (off == -1)
        return this.defaultValue;
      else
        return this.field.elementAt(off);
    }
 
    public void setAt(int x, int y, T newValue)
    {
      int off = getOff(x, y);
      assert (off != -1);
      this.field.setElementAt(newValue, off);
    }

    public void fillWith(T fillElement)
    {
      for (int i = 0; i < this.sz; ++i)
        this.field.setElementAt(fillElement, i);
    }
    
    public Array2D<T> clone()
    {
      Array2D<T> n = new Array2D<T>(this.wx, this.wy, this.defaultValue, false);
      for (int i = 0; i < this.sz; ++i)
        n.field.setElementAt(this.field.elementAt(i), i);
      return n;
    }

    public int getWidthX()
    {
      return this.wx;
    }

    public int getWidthY()
    {
      return this.wy;
    }
 }
