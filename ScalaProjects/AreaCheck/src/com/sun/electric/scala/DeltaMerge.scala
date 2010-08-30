/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DeltaMerge.scala
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

import com.sun.electric.database.geometry.DBMath

import java.util.Arrays
import java.io.DataOutputStream

class DeltaMerge {
  val segLast = new Segment
  segLast.y = Int.MaxValue

  var segPool: Segment = null
  var chain: Segment = new Segment
  chain.next = segLast;
  chain.y = Int.MinValue

  var x: Int = 0
  var inp: Array[Int] = null
  var inpC: Int = 0
  
  var outA = new Array[Int](1)
  var outC = 0

  var inpFun: () => ScanLine = null

  def getLine: Boolean = {
    inpFun() match {
      case null => false
      case sl => x = sl.x; inp = sl.y; inpC = sl.len; true
    }
  }

  def loop(inpFun: () => ScanLine, out: DataOutputStream): Int = {
    this.inpFun = inpFun
    var totalOutPoints = 0
    while (getLine) {
      scanLine
      totalOutPoints += outC
      printOut(out);
      checkSegments
    }
    out.writeBoolean(false)
    totalOutPoints;
  }

  def printOut(out: DataOutputStream) = {
    if (outC != 0) {
      out.writeBoolean(true)
//      print("x=" + DBMath.gridToLambda(x))
      out.writeInt(x)
      out.writeInt(outC)
      var i = 0
      while (i < outC) {
        val outVal = outA(i)
        val y = outVal >> 1;
        val df = (outVal & 1) != 0
//        print(" " + DBMath.gridToLambda(y) + (if (df) "^" else "_"))
        out.writeInt(outVal)
        i += 1
      }
//      println();
    }
  }

  def checkSegments = {
    assert(chain.y == Int.MinValue)
    assert(chain.v == 0)
    var cp = chain
    while (cp.next != segLast) {
      assert(cp.y < cp.next.y)
      assert(cp.v >= 0)
      cp = cp.next
    }
    assert(segLast.y == Int.MaxValue)
    assert(segLast.v == 0)
  }

  def scanLine = {
    assert(inpC > 0)
    var inpStep = 0;
    var outStep = 0;
    var cp = chain
    resetOut

    var inpPos = 0
    while (inpPos < inpC) {
      val y = inp(inpPos*2 + 0)
      val df = inp(inpPos*2 + 1)
      inpPos += 1
      assert(cp.y < y)

      if (inpStep == 0) {
        while (cp.next.y < y) cp = cp.next
      } else {
        while (cp.next.y < y) {
          val p = cp.next
          val oldO = if (p.v == 0) 0 else 1
          p.v += inpStep
          val newO = if (p.v == 0) 0 else 1
          val newOutStep = newO - oldO
          if (newOutStep != outStep) {
            putPointOut(p.y, newOutStep - outStep)
            outStep = newOutStep
          }
          cp = p
        }
      }
      assert(cp.y < y && y <= cp.next.y)
      var p: Segment = null
      if (cp.next.y > y) {
        p = newSegment(cp.next)
        p.y = y
        p.v = cp.v - inpStep
        cp.next = p
      } else {
        p = cp.next;
      }
      inpStep += df
      assert(p.y == y)
      val oldO = if (p.v == 0) 0 else 1
      p.v += inpStep
      val newO = if (p.v == 0) 0 else 1
      val newOutStep = newO - oldO
      if (newOutStep != outStep) {
        putPointOut(y, newOutStep - outStep)
        outStep = newOutStep
      }
      if (cp.v == p.v) {
        cp.next = p.next
        dispSegment(p);
      } else {
        cp = p;
      }
      assert(cp.y <= y)
    }
    assert(inpStep == 0)
  }

  def resetOut = {
    outC = 0
  }

  def putPointOut(y: Int, v: Int): Unit = {
    if (outC >= outA.length) {
      val newOutA = new Array[Int](outA.length*2)
      System.arraycopy(outA, 0, newOutA, 0, outA.length);
      outA = newOutA;
    }
    if (v == +1) {
      outA(outC) = (y << 1) | 1
      outC += 1
    } else if (v == -1) {
      outA(outC) = (y << 1)
      outC += 1
    } else if (v == +2) {
      outA(outC) = (y << 1) | 1
      outC += 1
      putPointOut(y, +1)
    } else if (v == -2) {
      outA(outC) = (y << 1)
      outC += 1
      putPointOut(y, -1)
    } else {
      throw new AssertionError
    }
  }

  def newSegment(next: Segment): Segment = {
    var result: Segment = null
    if (segPool == null) {
      result = new Segment
    } else {
      result = segPool
      segPool = segPool.next
    }
    result.next = next
    result;
  }

  def dispSegment(p: Segment) = {
    p.next = segPool
    segPool = p
  }
}

class Segment {
  var next: Segment = null
  var y: Int = 0   // begin of the segment
  var v: Int = 0 // height at points below y
}
