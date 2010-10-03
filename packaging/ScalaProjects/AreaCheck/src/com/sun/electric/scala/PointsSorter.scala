/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PointsSorter.scala
 * Written by Dmitry Nadezhin, Sun Microsystems.
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
package com.sun.electric.scala

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Arrays

import scala.collection.mutable.Buffer

/**
 *
 */
class PointsSorter {
  var pointsOut = 0
  var points = new Array[Long](1)
  var pointsInFiles = 0
  val limitToSave = 1 << 24
  val limitToSplit = 1 << 25
  var files = Buffer[File]()
  var inps: Array[DataInputStream] = null
  var inpH: Array[Long] = null

  def put(lx: Int, ly: Int, hx: Int, hy: Int) {
    put(lx, ly, true)
    put(lx, hy, false)
    put(hx, ly, false)
    put(hx, hy, true)
  }

  def put(x: Int, y: Int, positive: Boolean) {
    if (pointsOut >= points.length) {
      if (pointsOut >= limitToSplit) {
        saveToFile
        assert(pointsOut == 0)
      } else {
        val newPoints = new Array[Long](points.length*2)
        System.arraycopy(points, 0, newPoints, 0, points.length);
        points = newPoints;
      }
    }
    if (x == Int.MaxValue || y < -0x40000000 || y > 0x3fffffff)
      throw new IllegalArgumentException();
    var p: Long = (x.asInstanceOf[Long] << 32) | (((y + 0x40000000) << 1) & 0xfffffffeL)
    if (positive) p |= 1
    points(pointsOut) = p
    pointsOut += 1
  }

  def fix: () => ScanLine = {
    if (pointsOut != 0 && (!files.isEmpty || pointsOut > limitToSave)) {
      saveToFile
    } else {
      Arrays.sort(points, 0, pointsOut)
    }
    if (files.isEmpty) getFromArray else getFromStreams
  }

  def saveToFile = {
    Arrays.sort(points, 0, pointsOut)
    val file = File.createTempFile("Electric", "DRC")
    file.deleteOnExit
    val out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))
    var i = 0
    while (i < pointsOut) {
      out.writeLong(points(i))
      i += 1
    }
    out.writeLong(Long.MaxValue)
    out.close
    files.append(file)
    pointsInFiles += pointsOut
    pointsOut = 0
  }

  def size = pointsInFiles + pointsOut

  var curPoint = 0

  var outA = new Array[Int](2)
  var outC = 0
  val outS = new ScanLine

  def putOutBuf(y: Int, d: Int): Unit = {
    if (d == 0) {
      return
    }
    if (outC*2 >= outA.length) {
      val newOutA = new Array[Int](outA.length*2)
      System.arraycopy(outA, 0, newOutA, 0, outA.length)
      outA = newOutA
    }
    outA(outC*2 + 0) = y
    outA(outC*2 + 1) = d
    outC += 1
  }

  def reset = { curPoint = 0 }

  def getFromArray(): ScanLine = {
    outC = 0
    var curX: Int = 0
    var curY = 0
    var curD = 0

    while (curPoint < pointsOut) {
      val p = points(curPoint)
      val x = (p >> 32).asInstanceOf[Int]
      val y = (0x80000000 + (p&0xffffffff).asInstanceOf[Int]) >> 1
      val d = if ((p&1) != 0) 1 else -1
      if (outC == 0 && curD == 0) {
        curX = x
        curY = y
        curD = d
      } else if (x != curX) {
        putOutBuf(curY, curD)
        assert (outC != 0)
        outS.x = curX
        outS.y = outA
        outS.len = outC
        return outS
      } else if (y != curY) {
        putOutBuf(curY, curD)
        curY = y
        curD = d
      } else {
        curD += d
      }
      curPoint += 1
    }
    if (outC == 0 && curD == 0) {
      null
    } else {
      putOutBuf(curY, curD)
      assert (outC != 0)
      outS.x = curX
      outS.y = outA
      outS.len = outC
      outS
    }
  }

  def getFromStreams(): ScanLine = {
    if (inps == null) {
      inps = new Array[DataInputStream](files.length)
      inpH = new Array[Long](files.length)
      for (val i <- 0 until files.length) {
        val inp = new DataInputStream(new BufferedInputStream(new FileInputStream(files(i))))
        inps(i) = inp
        inpH(i) = inp.readLong
      }
    }
    outC = 0
    var curX: Int = 0
    var curY = 0
    var curD = 0

    var minL = Long.MaxValue
    var minI = -1
    var i = 0
    while (i < inpH.length) {
      if (inpH(i) < minL) {
        minL = inpH(i)
        minI = i
      }
      i += 1
    }
    while (minL != Long.MaxValue) {
      val p = minL
      val x = (p >> 32).asInstanceOf[Int]
      val y = (0x80000000 + (p&0xffffffff).asInstanceOf[Int]) >> 1
      val d = if ((p&1) != 0) 1 else -1
      if (outC == 0 && curD == 0) {
        curX = x
        curY = y
        curD = d
      } else if (x != curX) {
        putOutBuf(curY, curD)
        assert (outC != 0)
        outS.x = curX
        outS.y = outA
        outS.len = outC
        return outS
      } else if (y != curY) {
        putOutBuf(curY, curD)
        curY = y
        curD = d
      } else {
        curD += d
      }
      curPoint += 1

      val l = inps(minI).readLong
      inpH(minI) = l
      if (l == Long.MaxValue) {
        inps(minI).close
        files(minI).delete
        inps(minI) = null
      }
      minL = Long.MaxValue
      minI = -1
      var i = 0
      while (i < inpH.length) {
        if (inpH(i) < minL) {
          minL = inpH(i)
          minI = i
        }
        i += 1
      }
    }
    if (outC == 0 && curD == 0) {
      null
    } else {
      putOutBuf(curY, curD)
      assert (outC != 0)
      outS.x = curX
      outS.y = outA
      outS.len = outC
      outS
    }
  }
}

class ScanLine {
  var x: Int = 0
  var y: Array[Int] = null
  var len: Int = 0
}
