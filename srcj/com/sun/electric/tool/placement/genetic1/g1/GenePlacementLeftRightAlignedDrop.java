/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GenePlacementLeftRightAlignedDrop.java
 * Written by Team 3: Christian Wittner, Ivan Dimitrov
 * 
 * This code has been developed at the Karlsruhe Institute of Technology (KIT), Germany, 
 * as part of the course "Multicore Programming in Practice: Tools, Models, and Languages".
 * Contact instructor: Dr. Victor Pankratius (pankratius@ipd.uka.de)
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
package com.sun.electric.tool.placement.genetic1.g1;

import java.util.ArrayList;
import java.util.Arrays;

import com.sun.electric.tool.placement.genetic1.Chromosome;
import com.sun.electric.tool.placement.genetic1.GenePlacement;

public class GenePlacementLeftRightAlignedDrop implements GenePlacement {

	private static int placementWidth = -1;
//	private static final boolean DEBUG = false;

	int genePosition2Index[];

	enum Direction {
		LEFT2RIGHT, RIGHT2LEFT
	};

	Direction direction;

	// list of x-,y-coordinates
	// first level list secound level int[][0] = x int[][1]=y
	int[][] currentLine, previousLine, swapLine;

	public GenePlacementLeftRightAlignedDrop(int placementWidth,
			int chromosomeSize, PlacementNodeProxy[] nodeProxies) {
		this.nodeProxies = nodeProxies;
		GenePlacementLeftRightAlignedDrop.placementWidth = placementWidth;
		assert (placementWidth != -1);
		setChromosomeSize(chromosomeSize);
	}

	private void setChromosomeSize(int chromosomeSize) {
		int assumedLineLength = (int) (Math.sqrt(chromosomeSize) * 1.1);

		currentLine = new int[assumedLineLength][];
		previousLine = new int[assumedLineLength][];

		for (int i = 0; i < assumedLineLength; i++) {
			currentLine[i] = new int[2];
			previousLine[i] = new int[2];
		}

		assert (isLineZero(currentLine));
		assert (isLineZero(previousLine));

		genePosition2Index = new int[chromosomeSize];
	}

	PlacementNodeProxy[] nodeProxies;

	public void placeChromosome(Chromosome chToPlace) {

		assert (chToPlace != null);
		assert (currentLine != null && previousLine != null) : "set chromsome size before first usage";
		assert (placementWidth != -1) : "set placement width before first usage";
		assert (chToPlace.isIndex2GenePosValid());
		assert (isLineZero(previousLine));

		direction = Direction.LEFT2RIGHT;

		int tempGenXPosition = 0;
		int tempGenYPosition = 0;
		int tWidth, tHeight;
		int offX = 0;
		int positionInCurrentLine = 0;
		int startPosition2SearchInPreviousLine = 0;
		generateGenePos2IndexMapping(chToPlace.Index2GenePositionInChromosome,
				genePosition2Index);
		int[] yCalculationResult;

		int nodeIndex;

		// iterate over all genes we have to place
		for (int i = 0; i < genePosition2Index.length; i++) {

			// get cell width from placement node proxy and add padding
			{
				nodeIndex = genePosition2Index[i];

				// if we have a gene representing a placement node get width and
				// height from placementnodeproxy
				tWidth = nodeProxies[nodeIndex].width + chToPlace.GeneXPadding[nodeIndex];
				tHeight = nodeProxies[nodeIndex].height+ chToPlace.GeneYPadding[nodeIndex];

				// if gene is rotated sideways swap width and height
				if (chToPlace.GeneRotation[nodeIndex] == 900
						|| chToPlace.GeneRotation[nodeIndex] == 2700) {
					int swap = tHeight;
					tHeight = tWidth;
					tWidth = swap;
				}

			}

			// as we positions genes' center we need to use half width and
			// height
			tWidth = tWidth >> 1;
			tHeight = tHeight >> 1;

			// calculate x position
			// if going left to right add to the x offset
			if (direction == Direction.LEFT2RIGHT)
				tempGenXPosition = offX + tWidth;
			else
				// if going right to left subtract from x offset
				tempGenXPosition = offX - tWidth;

			// cell doesn't fit current line start new one
			if (direction == Direction.LEFT2RIGHT
					&& (tempGenXPosition + tWidth) > placementWidth) {

				// identical for both line switches
				{
					swapLine = currentLine;
					currentLine = previousLine;
					previousLine = swapLine;
					assert (isLineInXOrder(previousLine,
							positionInCurrentLine - 1, direction));

					// remember until which position the current line was used
					// to
					// store valid y values
					startPosition2SearchInPreviousLine = positionInCurrentLine;
					positionInCurrentLine = 0;
				}

				// unique for left2rigth
				{
					// switch to right2left
					direction = Direction.RIGHT2LEFT;
					offX = placementWidth;
					tempGenXPosition = placementWidth - tWidth;
				}

			} else if (direction == Direction.RIGHT2LEFT
					&& (tempGenXPosition - tWidth) < 0) {

				// identical for both line switches
				{
					swapLine = currentLine;
					currentLine = previousLine;
					previousLine = swapLine;
					assert (isLineInXOrder(previousLine,
							positionInCurrentLine - 1, direction));

					// remember until which position the current line was used
					// to
					// store valid y values
					startPosition2SearchInPreviousLine = positionInCurrentLine;
					positionInCurrentLine = 0;
				}

				// unique for left2rigth
				{
					// switch to left2right
					direction = Direction.LEFT2RIGHT;
					offX = 0;
					tempGenXPosition = tWidth;
				}
			}

			tWidth = tWidth << 1;

			// calculate y value for center of current cell to place
			if (direction == Direction.LEFT2RIGHT) {
				yCalculationResult = calculateY(offX, offX + tWidth, tHeight,
						startPosition2SearchInPreviousLine, direction);
			} else {
				yCalculationResult = calculateY(offX, offX - tWidth, tHeight,
						startPosition2SearchInPreviousLine, direction);
			}

			tempGenYPosition = yCalculationResult[1];
			startPosition2SearchInPreviousLine = yCalculationResult[0];

			// store the yValue at to of the current cell as contour reference
			// line for
			// the next loop
			if (direction == Direction.LEFT2RIGHT)
				storeYValue(positionInCurrentLine, tempGenYPosition + tHeight,
						offX, offX + tWidth);
			else
				storeYValue(positionInCurrentLine, tempGenYPosition + tHeight,
						offX, offX - tWidth);

			chToPlace.GeneXPos[nodeIndex] = tempGenXPosition;
			chToPlace.GeneYPos[nodeIndex] = tempGenYPosition;

			if (direction == Direction.LEFT2RIGHT)
				offX += tWidth;
			else
				offX -= tWidth;

			positionInCurrentLine++;

		}

		// clear previous line for next use
		for (int i = 0; i < previousLine.length; i++) {
			previousLine[i][0] = 0;
			previousLine[i][1] = 0;
		}
	}

	/**
	 * As the chromosome stores the position of a given index in an array in
	 * index order this method converts it into an array in gene position order
	 * pointing to the indexes.
	 * 
	 * @param index2GenePositionInChromosome
	 *            Array in index order. (first entry is position of node with
	 *            index 0)
	 * 
	 * @param returnValueGenerateGenePos2IndexMapping
	 *            Reference to the array to store the return value. The gene
	 *            position to index map.
	 */
	public static void generateGenePos2IndexMapping(
			int[] index2GenePositionInChromosome,
			int[] returnValueGenerateGenePos2IndexMapping) {
		assert (index2GenePositionInChromosome.length == returnValueGenerateGenePos2IndexMapping.length);

		for (int index = 0; index < index2GenePositionInChromosome.length; index++)
			returnValueGenerateGenePos2IndexMapping[index2GenePositionInChromosome[index]] = index;

	}

	private boolean isLineInXOrder(int[][] previousLine, int linePosition,
			Direction direction) {

		if (direction == Direction.LEFT2RIGHT)
			for (int i = 0; i < linePosition; i++) {
				assert (previousLine[i][0] <= placementWidth && previousLine[i][0] >= 0);
				if (previousLine[i][0] >= previousLine[i + 1][0])
					return false;
			}
		else
			for (int i = 0; i < linePosition; i++) {
				assert (previousLine[i][0] <= placementWidth && previousLine[i][0] >= 0);
				if (previousLine[i][0] <= previousLine[i + 1][0])
					return false;
			}
		return true;

	}

	/**
	 * Calculates y-value for the center of the current block to place depending
	 * on the previously stored contour line.
	 * 
	 * 
	 * @param startX
	 *            x position at the beginning of the block to place
	 * @param endX
	 *            x position at the end of the block to place
	 * @param halfCellHeigth
	 *            half the height of the block to place
	 * @param startPosition2SearchInPreviousLine
	 *            position to start searching for the y-value in the previous
	 *            line.
	 * @param direction
	 *            direction telling if placement is currently left to right or
	 *            right to left.
	 * 
	 * @return int[] with int[0] = new start position for next search. int[1] =
	 *         calculated y value for current block.
	 */
	private int[] calculateY(int startX, int endX, int halfCellHeigth,
			int startPosition2SearchInPreviousLine, Direction direction) {

		int yValue = 0;
		int newSearchStartPosition = -1;
		// initialize for the case that our current block is on the height of
		// the starting block of the previous line
		int valueBeforeInterval = previousLine[startPosition2SearchInPreviousLine][1];
		int valueAfterInterval = previousLine[0][1];

		// will be true if a value between startX and endX is present in the
		// previous line
		boolean valueInRange = false;

		// find highest y between startX and endX in previous Line
		// have to step through it backward as we move in opposite direction
		// when placing
		// a line relative to it's previous line.
		int curY, curX;
		for (int pos = startPosition2SearchInPreviousLine; pos >= 0; pos--) {
			curY = previousLine[pos][1];
			curX = previousLine[pos][0];

			// store value before interval
			if (!valueInRange) {
				valueAfterInterval = valueBeforeInterval = curY;
				newSearchStartPosition = pos;
			}

			// if x value in previousLine is passed exit
			if (direction == Direction.LEFT2RIGHT && curX > endX
					|| direction == Direction.RIGHT2LEFT && curX < endX) {
				valueAfterInterval = curY;
				break;
			}

			// search for highest yValue within range of previous Line
			if (direction == Direction.LEFT2RIGHT) {
				if (curX > startX) {
					valueInRange = true;
					newSearchStartPosition = pos;
					if (curY > yValue) {
						yValue = curY;

					}
				}
			} else {
				if (curX < startX) {
					valueInRange = true;
					newSearchStartPosition = pos;
					if (curY > yValue) {
						yValue = curY;
					}
				}
			}
		}

		assert (valueAfterInterval >= 0);

		if (!valueInRange)
			yValue = Math.min(valueBeforeInterval, valueAfterInterval);

		yValue += halfCellHeigth;
		return new int[] { newSearchStartPosition, yValue };
	}

	private boolean isLineZero(int[][] line) {
		for (int[] p : line) {
			if (p[0] != 0 || p[1] != 0)
				return false;
		}

		return true;
	}

	/**
	 * For current line store the height of the current block.
	 * 
	 * @param currentLine
	 * @param positionInLine
	 * @param topyValue
	 *            Y value of top of the current cell.
	 * @param startX
	 *            X-value where current block starts.
	 * @param endX
	 *            X-value where current block ends.
	 */
	private void storeYValue(int positionInLine, int topyValue, int startX,
			int endX) {

		// if currentLine to short to store value add new point
		// intense but should rarely happen. best if it never happens but still
		// better than an index out of bounds exception
		if (currentLine.length <= positionInLine + 1) {
			ArrayList<int[]> expansionHelper = new ArrayList<int[]>(
					currentLine.length + 5);
			expansionHelper.addAll(Arrays.asList(currentLine));
			// grow by 5 slots
			for (int i = 0; i < 5; i++)
				expansionHelper.add(new int[2]);

			currentLine = expansionHelper.toArray(new int[expansionHelper
					.size()][]);
		}

		// for first cell in line just store yValue at startX
		if (positionInLine == 0) {
			currentLine[positionInLine][0] = startX;
			currentLine[positionInLine][1] = topyValue;

		} else {
			// see if yValue at the beginning of the block is smaller than
			// yValue if so set it to Y
			if (currentLine[positionInLine][1] < topyValue)
				currentLine[positionInLine][1] = topyValue;

		}

		assert (currentLine[positionInLine][0] == startX);

		// store YValue at endX
		currentLine[positionInLine + 1][0] = endX;
		currentLine[positionInLine + 1][1] = topyValue;

	}

}
