/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MovieCreatorJMF.java
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.plugins.JMF;

import com.sun.electric.api.movie.MovieCreator;

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of MovieCreator API using Java Media Frame.
 */
public class MovieCreatorJMF implements MovieCreator {
    
    public MovieCreatorJMF() {
        // Check if JMF is available
        String version = javax.media.Manager.getVersion();
        System.err.println("JMF version " + version);
    }

    /**
     * Create a movie from a sequence of images
     * @param outputFile output file for movie
     * @param dim size of the movie
     * @param inputFiles files wuth JPEG imaphes
     */
    public void createFromImages(File outputFile, Dimension dim, List<File> inputFiles) {
        List<String> inputFileNames = new ArrayList<String>();
        for (File file: inputFiles) {
            inputFileNames.add(file.getAbsolutePath());
        }
        JMFImageToMovie.createMovie(outputFile.getAbsolutePath(), dim, inputFileNames);
    }

}
