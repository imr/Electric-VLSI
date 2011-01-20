/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.electric.plugins.minarea.bitmapscala

import com.sun.electric.api.minarea.LayoutCell
import com.sun.electric.api.minarea.ManhattanOrientation
import com.sun.electric.api.minarea.MinAreaChecker

import java.util.Properties

import scala.collection.mutable.BitSet
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.LinkedHashSet
import scala.collection.immutable.TreeSet

class BitMapMinAreaChecker extends MinAreaChecker {
  val DEBUG = 1

  override def getAlgorithmName = "BitMap"
  
  override def getDefaultParameters = new Properties()
  
  def downTop(top: LayoutCell): Seq[LayoutCell] = {
    val result = LinkedHashSet[LayoutCell]()
    def downTop(t: LayoutCell): Unit = {
      if (!result.contains(t)) {
        t.traverseSubcellInstances(new LayoutCell.SubcellHandler {
            def apply(subCell: LayoutCell, anchorX: Int, anchorY: Int, subOrient: ManhattanOrientation) = {
              downTop(subCell)
            }
          })
//        for (subTree <- t.getSubTrees) downTop(subTree)
        result.add(t)
      }
    }
    downTop(top)
    result.toSeq
  }
  
  def flattenRectangles(top: LayoutCell, proc: (Int, Int, Int, Int) => Unit) = {
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
    
    if (DEBUG >= 4) {
      val dt = downTop(topCell)
      for (val t <- dt) {
        println
        println(t.getName)
        t.traverseRectangles(new LayoutCell.RectangleHandler {
            def apply(minX: Int, minY: Int, maxX: Int, maxY: Int) = {
              println(" box ["+minX+".."+maxX+"]x["+minY+".."+maxY+"]")
            }
          })
        t.traverseSubcellInstances(new LayoutCell.SubcellHandler {
            def apply(subCell: LayoutCell, anchorX: Int, anchorY: Int, subOrient: ManhattanOrientation) = {
              println(" call "+subCell.getName+" ("+anchorX+","+anchorY+") "+subOrient)
            }
          })
      }
    }
    
    var xcoords = new TreeSet[Int]()
    var ycoords = new TreeSet[Int]()
    flattenRectangles(topCell, (minX: Int, minY: Int, maxX: Int, maxY: Int) => {
        if (DEBUG >= 4) {
          println(" flat ["+minX+".."+maxX+"]x["+minY+".."+maxY+"]")
        }
        xcoords = xcoords + minX + maxX
        ycoords = ycoords + minY + maxY
      })
  
    val xcoorda = new Array[Int](xcoords.size)
    val ycoorda = new Array[Int](ycoords.size)
    
    val xcoordm = new LinkedHashMap[Int,Int]()
    val ycoordm = new LinkedHashMap[Int,Int]()
    for (x <- xcoords) {
      xcoorda(xcoordm.size) = x
      xcoordm.put(x, xcoordm.size)
    }
    for (y <- ycoords) {
      ycoorda(ycoordm.size) = y
      ycoordm.put(y, ycoordm.size)
    }
    
    val xsize = xcoords.size - 1
    val ysize = ycoords.size - 1
    
    val bitMap = new Array[BitSet](xsize)
    for (i <- 0 until bitMap.length) bitMap(i) = new BitSet
    flattenRectangles(topCell, (minX: Int, minY: Int, maxX: Int, maxY: Int) => {
        val minXI = xcoordm(minX)
        val minYI = ycoordm(minY)
        val maxXI = xcoordm(maxX)
        val maxYI = ycoordm(maxY)
        val yset = new BitSet()
        var y = minYI
        while (y < maxYI) {
          yset.add(y)
          y += 1
        }
        var x = minXI
        while (x < maxXI) {
          bitMap(x) |= yset
          x += 1
        }
      })

    if (DEBUG >= 4) {
      println("xcoords "+xcoords)
      println("ycoords "+ycoords)
    
      println("xcoordm "+xcoordm)
      println("ycoordm "+ycoordm)
    
      printBitMap(bitMap, xsize, ysize)
    }
    
    var totalArea = 0L
    for (x <- 0 until xsize) {
      val yset = bitMap(x)
      var ylen = 0
      for (y <- 0 until ysize) {
        if (yset(y)) ylen += ycoorda(y + 1) - ycoorda(y)
      }
      totalArea += (xcoorda(x + 1) - xcoorda(x)) * ylen.asInstanceOf[Long]
    }
    if (DEBUG >= 1) {
      println("Total Area "+totalArea)
    }
    
    val prevX = new Array[Polygon](ysize)
    val nextX = new Array[Polygon](ysize)
    for (x <- 0 until xsize) {
      val dx: Long = xcoorda(x + 1) - xcoorda(x)
      val yset = bitMap(x)
      var y = ysize
      while (y > 0 && !yset(y-1)) y -= 1
      while (y > 0) {
        val ymax = y
        var poly: Polygon = null
        while (y > 0 && yset(y-1)) {
          val prevP = prevX(y-1)
          if (prevP != null && prevP.next != null) {
            if (poly == null || poly.y < prevP.next.y)
              poly = prevP.next
          }
          y -= 1
        }
        val ymin = y
        if (poly == null) {
          poly = new Polygon
          poly.y = ymax
        }
        poly.area += dx * (ycoorda(ymax) - ycoorda(ymin))
        var j = ymin
        while (j < ymax) {
          nextX(j) = poly
          if (prevX(j) != null) prevX(j).next = poly
          j += 1
        }
        
        while (y > 0 && !yset(y-1)) y -= 1
      }
      
      y = 0
      while (y < ysize) {
        val prevP = prevX(y)
        if (prevP != null && prevP.y == y + 1) {
          if (prevP.next != null) {
            prevP.next.area += prevP.area
          } else {
            totalArea += prevP.area
            errorLogger.reportMinAreaViolation(prevP.area, xcoorda(x), ycoorda(prevP.y))
          }
        }
        y += 1
      }
      
      y = 0
      while (y < ysize) {
        prevX(y) = nextX(y)
        nextX(y) = null
        y += 1
      }
    }
    
    var y = 0
    while (y < ysize) {
      val prevP = prevX(y)
      if (prevP != null && prevP.y == y + 1) {
        totalArea += prevP.area
        errorLogger.reportMinAreaViolation(prevP.area, xcoorda(xsize), ycoorda(prevP.y))
      }
      y += 1
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

class Polygon {
  var y: Int = 0
  var area: Long = 0
  var next: Polygon = null
}

