/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Array3D.java
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

public class Array3D<T>
{
    private int wx, wy, wz;
    private int sz;
    private Vector<T> field;
    private T defaultValue;

    public Array3D(int wx, int wy, int wz, T defaultValue, boolean fillWithDefault)
    {
      this.wx = wx;
      this.wy = wy;
      this.wz = wz;
      this.sz = this.wx * this.wy * this.wz;
      this.defaultValue = defaultValue;
      
      this.field = new Vector<T>(this.sz);
      this.field.setSize(this.sz);
      
      if (fillWithDefault)
        fillWith(defaultValue);
    }
    
    public boolean inRange(int x, int y, int z)
    {
      return (x >= 0 && y >= 0 && z >= 0 && x < this.wx && y < this.wy && z < this.wz);
    }
    
    /*
     * Return -1 on error.
     */
    private int getOff(int x, int y, int z)
    {
      if (!inRange(x, y, z))
        return -1;
      int off = x + y * this.wx + z * this.wx * this.wy;
      assert (off >= 0 && off < this.sz);
      return off;
    }

    public T getAt(int x, int y, int z)
    {
      int off = getOff(x, y, z);
      if (off == -1)
        return this.defaultValue;
      else
        return this.field.elementAt(off);
    }
 
    public void setAt(int x, int y, int z, T newValue)
    {
      int off = getOff(x, y, z);
      assert (off != -1);
      this.field.setElementAt(newValue, off);
    }

    public void fillWith(T fillElement)
    {
      for (int i = 0; i < this.sz; ++i)
        this.field.setElementAt(fillElement, i);
    }
    
    public Array3D<T> clone()
    {
      Array3D<T> n = new Array3D<T>(this.wx, this.wy, this.wz, this.defaultValue, false);
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

    public int getWidthZ()
    {
      return this.wz;
    }
 }
