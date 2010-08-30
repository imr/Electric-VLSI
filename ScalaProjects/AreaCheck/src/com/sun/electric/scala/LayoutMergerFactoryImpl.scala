/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayoutMergerFactoryImpl.scala
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

import com.sun.electric.database.CellTree
import com.sun.electric.database.geometry.Orientation
import com.sun.electric.database.geometry.PolyBase
import com.sun.electric.database.geometry.bool.LayoutMerger
import com.sun.electric.database.geometry.bool.LayoutMergerFactory
import com.sun.electric.database.geometry.bool.UnloadPolys
import com.sun.electric.database.geometry.bool.VectorCache
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.id.CellId
import com.sun.electric.database.text.TextUtils
import com.sun.electric.technology.Layer

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

import scala.collection.JavaConversions
import scala.collection.immutable.VectorBuilder
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.LinkedHashSet


class LayoutMergerFactoryImpl extends LayoutMergerFactory {
  def newMerger(topCell: Cell) = new LayoutMergerScalaImpl(topCell)
}

class LayoutMergerScalaImpl(topCell: Cell) extends LayoutMerger {
  val TMP_FILE_THRESHOLD = 1000000

  val vectorCache = new VectorCache(topCell.getDatabase.backup)
  vectorCache.scanLayers(topCell.getId)
  
  override def getLayers = vectorCache.getLayers

  override def canMerge(layer: Layer) =  !vectorCache.isBadLayer(layer)

  def downTop(top: CellTree): Seq[CellTree] = {
    val result = LinkedHashSet[CellTree]()
    def downTop(t: CellTree): Unit = {
      if (!result.contains(t)) {
        for (subTree <- t.getSubTrees) downTop(subTree)
        result.add(t)
      }
    }
    downTop(top)
    result.toSeq
  }

  def mergeLocalLayerToByteArray(cellId: CellId, layer: Layer): Array[Byte] = {
    val numBoxes = vectorCache.getNumBoxes(cellId, layer)
    if (numBoxes == 0) null else {
      val boxCoords = new Array[Int](4)
      val ps = new PointsSorter
      for (val i <- 0 until numBoxes) {
        vectorCache.getBoxes(cellId, layer, i, 1, boxCoords)
        val lx = boxCoords(0)
        val ly = boxCoords(1)
        val hx = boxCoords(2)
        val hy = boxCoords(3)
        ps.put(lx, ly, hx, hy)
      }
      val get = ps.fix

      val dm = new DeltaMerge
      val bout = new ByteArrayOutputStream
      val out = new DataOutputStream(bout)
      dm.loop(get, out)
      val ba = bout.toByteArray
      out.close

      ba
    }
  }

  def byteArray2coordArray(ba: Array[Byte]): Array[Int] = {
    val inpS = new DataInputStream(new ByteArrayInputStream(ba))
    val pos = new VectorBuilder[Int]
    val neg = new VectorBuilder[Int]
    while (inpS.readBoolean) {
      val x = inpS.readInt
      val count = inpS.readInt
      for (val i <- 0 until count) {
        val yp = inpS.readInt
        val y = yp >> 1
        val positive = (yp&1) != 0
        val b = if (positive) pos else neg
        b += x
        b += y
      }
    }
    val posV = pos.result
    val negV = neg.result
    assert(posV.size == negV.size)
    (posV ++ negV).toArray
  }

  def mergeLayer(mergedCoords: CellId => Array[Int], topCellId: CellId, layer: Layer, rotate: Boolean,
                 out: DataOutputStream) = {
    val ps = new PointsSorter
    var coordsBuf = new Array[Int](1024)

    def collectLayer(cellId: CellId, x: Int, y: Int, orient: Orientation): Unit = {
      val coords = mergedCoords(cellId)
      if (!coords.isEmpty) {
        if (coordsBuf.length < coords.length) {
          var newLen = coordsBuf.length; while (newLen < coords.length) newLen *= 2
          coordsBuf = new Array[Int](newLen)
        }
        val numPoints = coords.length/2
        assert(numPoints%2 == 0)
        orient.transformPoints(numPoints, coords, coordsBuf)
        val orientRot = orient.getCAngle != 0 && orient.getCAngle != 1800
        var positive = !orientRot
        for (val i <- 0 until numPoints) {
          if (i*2 == numPoints) positive = !positive
          ps.put(x + coordsBuf(i*2 + 0), y + coordsBuf(i*2 + 1), positive)
        }
      }
      val subCells = vectorCache.getSubcells(cellId);
      if (!subCells.isEmpty) {
        for (val n <- JavaConversions.asIterable(subCells)) {
          assert(n.orient.isManhattan)
          coordsBuf(0) = n.anchor.getGridX.asInstanceOf[Int]
          coordsBuf(1) = n.anchor.getGridY.asInstanceOf[Int]
          orient.transformPoints(1, coordsBuf)
          val subOrient = orient.concatenate(n.orient).canonic
          val subCellId = n.protoId.asInstanceOf[CellId]
          collectLayer(subCellId, x + coordsBuf(0), y + coordsBuf(1), subOrient)
        }
      }
    }

    val topOrient = (if (rotate) Orientation.XR else Orientation.IDENT).canonic
    val startTime1 = System.currentTimeMillis
    collectLayer(topCellId, 0, 0, topOrient)
    val get = ps.fix
    val endTime1 = System.currentTimeMillis
    val dm = new DeltaMerge
    val outPoints = dm.loop(get, out)
    val endTime2 = System.currentTimeMillis
    println(layer + " " + ps.size + "->" + outPoints + " points" +
            ", merge=" + TextUtils.getElapsedTime(endTime1 - startTime1) + " sec" +
            ", tree=" + TextUtils.getElapsedTime(endTime2 - endTime1) + " sec");
  }

  def flattenAndMergeLayer(layer: Layer, out: DataOutputStream) = {
    val dt = downTop(topCell.tree)
    val mergedCoords = new LinkedHashMap[CellId,Array[Int]]()
    for (val t <- dt) {
      val cellId = t.top.cellRevision.d.cellId
      val ba = mergeLocalLayerToByteArray(cellId, layer)
      if (ba != null) {
        mergedCoords.put(cellId, byteArray2coordArray(ba))
      } else {
        mergedCoords.put(cellId, Array.empty)
      }
    }
    val rotate = false
    mergeLayer(mergedCoords, topCell.getId, layer, rotate, out)
  }

  def mergeInMemory(layer: Layer): Array[Byte] = {
      val bout = new ByteArrayOutputStream
      val out = new DataOutputStream(bout)
      flattenAndMergeLayer(layer, out)
      out.close
      bout.toByteArray
  }

  def mergeInFile(layer: Layer): File = {
      val file = File.createTempFile("Electric", "DRC")
      val out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))
      flattenAndMergeLayer(layer, out)
      out.close
      file
  }

  override def merge(layer: Layer): java.lang.Iterable[PolyBase.PolyBaseTree] = {
    val inMemory = vectorCache.getNumFlatBoxes(topCell.getId(), layer) <= TMP_FILE_THRESHOLD;
    if (inMemory) {
      val ba = mergeInMemory(layer)
      val inpS = new DataInputStream(new ByteArrayInputStream(ba))
      val up = new UnloadPolys
      val trees = up.loop(inpS, false)
      inpS.close
      trees
    } else {
      val file = mergeInFile(layer)
      val inpS = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))
      val up = new UnloadPolys
      val trees = up.loop(inpS, false)
      inpS.close
      file.delete
      trees
    }
  }

}
