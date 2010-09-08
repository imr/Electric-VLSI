/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BookshelfPlacement.java
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

import com.sun.electric.tool.io.input.bookshelf.BookshelfNodes.BookshelfNode;
import com.sun.electric.util.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 */
public class BookshelfPlacement
{
	private String plFile;

	public BookshelfPlacement(String plFile)
	{
		this.plFile = plFile;
	}

	public void parse()
		throws IOException
	{
		File file = new File(plFile);
		FileReader freader = new FileReader(file);
		BufferedReader rin = new BufferedReader(freader);

		// skip the first line
		rin.readLine();

		for(;;)
		{
			String line = rin.readLine();
			if (line == null) break;
			if (line.length() == 0) continue;
			if (line.charAt(0) == '#') continue;

			StringTokenizer tokenizer = new StringTokenizer(line, " ");
			int i = 0;
			String name = "";
			double x = 0;
			double y = 0;
			while (tokenizer.hasMoreTokens())
			{
				if (i == 0) {
					name = tokenizer.nextToken();
				} else if (i == 1) {
					x = TextUtils.atof(tokenizer.nextToken());
				} else if (i == 2) {
					y = TextUtils.atof(tokenizer.nextToken());
				} else {
					tokenizer.nextToken();
				}
				i++;
			}
			
			BookshelfNode bn = BookshelfNode.findNode(name);
			if (bn != null)
				bn.setLocation(x, y);
		}
	}
}
