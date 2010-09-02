/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BookshelfAux.java
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
package com.sun.electric.tool.io.input.bookshelf;

import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.bookshelf.Bookshelf.BookshelfFiles;
import com.sun.electric.util.CollectionFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @author Felix Schmidt
 * 
 */
public class BookshelfAux implements BookshelfInputParser<Map<BookshelfFiles, String>> {

	private String auxFile = null;

	public BookshelfAux(String auxFile) {
		this.auxFile = auxFile;
	}

	public Map<BookshelfFiles, String> parse() throws IOException {
		Job.getUserInterface().setProgressNote("Parse Aux File");
		
		Map<BookshelfFiles, String> result = CollectionFactory.createHashMap();

		File file = new File(this.auxFile);
		FileReader freader = new FileReader(file);
		BufferedReader rin = new BufferedReader(freader);
		
		String line;
		while((line = rin.readLine()) != null) {
			StringTokenizer tokenizer = new StringTokenizer(line, " ");
			while(tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				BookshelfFiles type = BookshelfFiles.getFileType(token);
				if(type != null) {
					result.put(type, token);
				}
			}
		}
		
		return result;
	}
	
	public String getAuxDir() {
		File file = new File(this.auxFile);
		return file.getParent() + File.separator;
	}

}
