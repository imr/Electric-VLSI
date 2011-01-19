/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.electric.plugins.minarea.bitmapscala

import com.sun.electric.api.minarea.LayoutCell
import com.sun.electric.api.minarea.ManhattanOrientation
import com.sun.electric.api.minarea.MinAreaChecker

import java.util.Properties

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
    
    flattenRectangles(topCell, (minX: Int, minY: Int, maxX: Int, maxY: Int) => println(" flat ["+minX+".."+maxX+"]x["+minY+".."+maxY+"]"))
  }

}
