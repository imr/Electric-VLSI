/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MyChartFrame.java
 * Written by Tom O'Neill, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation.test;

/*
  Commented out so it will build without jfreechart.jar
     AM 14-Sep-09
 */

/**
 * Class displays a JFreeChart chart in its own window. Note the JFree package
 * includes its own more powerful ChartFrame, but this is lighter-weight.
 */

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.*;

//import org.jfree.chart.*;
//import org.jfree.chart.plot.*;
//import org.jfree.data.xy.*;

public class MyChartFrame {
//
//    private static final int VERTICAL_PADDING = 50;
//
//    private JFreeChart chart;
//
//    private int width, height;
//
//    private JFrame frame;
//
//    private JLabel lblChart;
//
//    /**
//     * Displays a chart in a new window.
//     * 
//     * @param chart
//     *            the chart to display
//     * @param width
//     *            the desired width for the plot
//     * @param height
//     *            the desired height for the plot
//     */
//    public MyChartFrame(JFreeChart chart, int width, int height) {
//        super();
//        this.chart = chart;
//        this.width = width;
//        this.height = height;
//
//        lblChart = new JLabel();
//        update(chart);
//
//        frame = new JFrame("Plot");
//        frame.getContentPane().add(lblChart);
//        frame.setSize(width, height + VERTICAL_PADDING);
//        frame.setVisible(true);
//    }
//
//    /**
//     * Display a new chart in the window.
//     * 
//     * @param chart
//     *            chart to display
//     */
//    public void update(JFreeChart chart) {
//        BufferedImage image = chart.createBufferedImage(width, height);
//        lblChart.setIcon(new ImageIcon(image));
//    }
//
//    /** Close the window containing the chart */
//    public void close() {
//        frame.setVisible(false);
//    }
//
//    /** Draw an example plot, then update it */
//    public static void testPlot() throws IOException {
//        XYSeries series = new XYSeries("Average Size");
//        series.add(20.0, 10.0);
//        series.add(40.0, 20.0);
//        series.add(70.0, 50.0);
//        XYDataset xyDataset = new XYSeriesCollection(series);
//        XYSeries series2 = new XYSeries("default Size");
//        series2.add(20.0, 15.0);
//        series2.add(40.0, 25.0);
//        series2.add(70.0, 60.0);
//        ((XYSeriesCollection) xyDataset).addSeries(series2);
//
//        // title, x-axis label, y-axis label, dataset, orientation, show_legend,
//        // tootips, urls
//        JFreeChart chart = ChartFactory.createXYLineChart("Sample XY Chart",
//                "Height", "Weight", xyDataset, PlotOrientation.VERTICAL, true,
//                true, false);
//        MyChartFrame cf = new MyChartFrame(chart, 500, 300);
//
// //       Infrastructure.readln("Hit return for update:");
//
//        series.add(80.0, 60.0);
//        xyDataset = new XYSeriesCollection(series);
//        chart = ChartFactory.createXYLineChart("Updated Sample XY Chart",
//                "Height", "Weight", xyDataset, PlotOrientation.HORIZONTAL,
//                true, true, false);
//        cf.update(chart);
//
//        ChartUtilities.saveChartAsJPEG(new File("chart.jpg"), chart, 500, 300);
//    }
//
//    public static void main(String[] args) throws IOException {
//        MyChartFrame.testPlot();
//    }
}
