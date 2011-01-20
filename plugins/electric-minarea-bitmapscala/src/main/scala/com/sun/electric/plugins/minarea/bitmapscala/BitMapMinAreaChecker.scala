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
import scala.collection.mutable.SetBuilder
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.LinkedHashSet
import scala.collection.immutable.TreeSet

class BitMapMinAreaChecker extends MinAreaChecker {

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
    
    val dt = downTop(topCell)
    val coordSets = new LinkedHashMap[LayoutCell,(TreeSet[Int],TreeSet[Int])]()
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
    
    var xcoords = new TreeSet[Int]()
    var ycoords = new TreeSet[Int]()
    flattenRectangles(topCell, (minX: Int, minY: Int, maxX: Int, maxY: Int) => {
        println(" flat ["+minX+".."+maxX+"]x["+minY+".."+maxY+"]")
        xcoords = xcoords + minX + maxX
        ycoords = ycoords + minY + maxY
      })
  
    println("xcoords "+xcoords)
    println("ycoords "+ycoords)
    
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
    
    println("xcoordm "+xcoordm)
    println("ycoordm "+ycoordm)
    
    val bitMap = new Array[BitSet](ycoords.size - 1)
    for (i <- 0 until bitMap.length) bitMap(i) = new BitSet
    flattenRectangles(topCell, (minX: Int, minY: Int, maxX: Int, maxY: Int) => {
        val minXI = xcoordm(minX)
        val minYI = ycoordm(minY)
        val maxXI = xcoordm(maxX)
        val maxYI = ycoordm(maxY)
        val xset = new BitSet()
        var x = minXI
        while (x < maxXI) {
          xset.add(x)
          x += 1
        }
        var y = minYI
        while (y < maxYI) {
          bitMap(y) |= xset
          y += 1
        }
      })
    
    var totalArea = 0L
    var y = ycoordm.size - 2
    while (y >= 0) {
      val xset = bitMap(y)
      var xlen = 0
      for (x <- 0 until xcoordm.size - 1) {
        if (xset(x)) {
          print('X')
          xlen += xcoorda(x + 1) - xcoorda(x)
        } else {
          print(' ')
        }
      }
      println
      totalArea += (ycoorda(y + 1) - ycoorda(y)) * xlen.asInstanceOf[Long]
      y -= 1
    }
    println("Total Area "+totalArea)
  }
}
