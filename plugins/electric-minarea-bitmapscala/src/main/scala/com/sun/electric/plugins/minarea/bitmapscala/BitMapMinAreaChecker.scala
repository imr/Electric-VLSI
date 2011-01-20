/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BitMapMinAreaChecker.scala
 *
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.plugins.minarea.bitmapscala

import com.sun.electric.api.minarea.LayoutCell
import com.sun.electric.api.minarea.ManhattanOrientation
import com.sun.electric.api.minarea.MinAreaChecker

import java.awt.Point
import java.util.Properties

import scala.collection.immutable.TreeSet
import scala.collection.mutable.BitSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.Stack

class BitMapMinAreaChecker extends MinAreaChecker {
  val DEBUG = 1

  override def getAlgorithmName = "BitMap"
  
  override def getDefaultParameters = new Properties()
  
  // traverse flattened rectangles
  private def flattenRects(top: LayoutCell, proc: (Int, Int, Int, Int) => Unit) = {
    def flatten(t: LayoutCell, x: Int, y: Int, orient: ManhattanOrientation): Unit = {
      val a = new Array[Int](4)
      t.traverseRectangles(new LayoutCell.RectangleHandler {
          def apply(minX: Int, minY: Int, maxX: Int, maxY: Int) = {
            a(0) = minX
            a(1) = minY
            a(2) = maxX
            a(3) = maxY
            orient.transformRects(a, 0, 1)
            proc(a(0) + x, a(1) + y, a(2) + x, a(3) + y)
          }
        })
      t.traverseSubcellInstances(new LayoutCell.SubcellHandler {
          def apply(subCell: LayoutCell, anchorX: Int, anchorY: Int, subOrient: ManhattanOrientation) = {
            a(0) = anchorX
            a(1) = anchorY
            orient.transformPoints(a, 0, 1)
            flatten(subCell, a(0) + x, a(1) + y, orient.concatenate(subOrient))
          }
        })
      
    }
    flatten(top, 0, 0, ManhattanOrientation.R0)
  }
  
  /**
   * @param topCell top cell of the layout
   * @param minArea minimal area of valid polygon
   * @param parameters algorithm parameters
   * @param errorLogger an API to report violations
   */
  override def check(topCell: LayoutCell, minArea: Long, parameters: Properties, errorLogger: MinAreaChecker.ErrorLogger) = {
    
    // find unique coordinates
    var xcoords = new TreeSet[Int]()
    var ycoords = new TreeSet[Int]()
    flattenRects(topCell, (minX: Int, minY: Int, maxX: Int, maxY: Int) => {
        if (DEBUG >= 4)
          println(" flat ["+minX+".."+maxX+"]x["+minY+".."+maxY+"]")
        xcoords = xcoords + minX + maxX
        ycoords = ycoords + minY + maxY
      })
  
    val xsize = xcoords.size - 1
    val ysize = ycoords.size - 1
    
    // xa,ya maps coordinate index to coordinate value
    // xm,ym maps coordinate value to coordinate index
    val xa = new Array[Int](xcoords.size)
    val ya = new Array[Int](ycoords.size)
    val xm = new HashMap[Int,Int]()
    val ym = new HashMap[Int,Int]()
    for (x <- xcoords) {
      xa(xm.size) = x
      xm.put(x, xm.size)
    }
    for (y <- ycoords) {
      ya(ym.size) = y
      ym.put(y, ym.size)
    }
    
    // fill bit map
    val bitMap = new Array[BitSet](xsize)
    for (i <- 0 until bitMap.length) bitMap(i) = new BitSet
    flattenRects(topCell, (minX: Int, minY: Int, maxX: Int, maxY: Int) => {
        val yset = new BitSet()
        for (y <- ym(minY) until ym(maxY)) yset.add(y)
        for (x <- xm(minX) until xm(maxX)) bitMap(x) |= yset
      })

    if (DEBUG >= 4) {
      println("xcoords="+xcoords)
      println("ycoords="+ycoords)
      printBitMap(bitMap, xsize, ysize)
    }
    
    // stack of tiles to fill polygon
    var stack = new Stack[Point]
    var polyArea: Long = 0
    
    def pushTile(x: Int, y: Int) = {
      val w: Long = xa(x + 1) - xa(x)
      val h: Long = ya(y + 1) - ya(y)
      polyArea += w*h
      bitMap(x).remove(y)
      stack.push(new Point(x,y))
    }
    
    // find polygons in reverse lexicographical order
    var totalArea = 0L
    var x = xsize - 1
    while (x >= 0) {
      var y = ysize - 1
      while (y >= 0) {
        if (bitMap(x)(y)) {
          polyArea = 0
          // find polygon area and erase polygon from bit map
          pushTile(x, y)
          while (!stack.isEmpty) {
            val p = stack.top
            if (p.x - 1 >= 0 && bitMap(p.x - 1)(p.y))
              pushTile(p.x - 1, p.y)
            else if (p.x + 1 < xsize && bitMap(p.x + 1)(p.y))
              pushTile(p.x + 1, p.y)
            else if (p.y - 1 >= 0 && bitMap(p.x)(p.y - 1))
              pushTile(p.x, p.y - 1)
            else if (p.y + 1 < ysize && bitMap(p.x)(p.y + 1))
              pushTile(p.x, p.y + 1)
            else
              stack.pop
          }
          totalArea += polyArea
          if (polyArea < minArea)
            errorLogger.reportMinAreaViolation(polyArea, xa(x + 1), ya(y + 1))
        }
        y -= 1
      }
      x -= 1
    }
    
    if (DEBUG >= 1) {
      println("Total Area "+totalArea)
    }
  }
  
  def printBitMap(bitMap: Array[BitSet], xsize: Int, ysize: Int) = {
    var y = ysize - 1
    while (y >= 0) {
      for (x <- 0 until xsize) print(if (bitMap(x)(y)) 'X' else ' ')
      println
      y -= 1
    }
  }
}
