package com.sun.electric.database.text;

import com.sun.electric.database.hierarchy.View;

public class CellName
{
	private String name;
	private View   view;
	private int    version;

	private CellName() {}
	
	public String getName() { return name; }
	public View getView() { return view; }
	public int getVersion() { return version; }

	public static CellName parseName(String name)
	{
		// figure out the view and version of the cell
		CellName n = new CellName();
		n.view = null;
		int openCurly = name.indexOf('{');
		int closeCurly = name.lastIndexOf('}');
		if (openCurly != -1 && closeCurly != -1)
		{
			String viewName = name.substring(openCurly+1, closeCurly);
			n.view = View.getView(viewName);
			if (n.view == null)
			{
				System.out.println("Unknown view: " + viewName);
				return null;
			}
		}

		// figure out the version
		n.version = 0;
		int semiColon = name.indexOf(';');
		if (semiColon != -1)
		{
			String versionString;
			if (openCurly > semiColon) versionString = name.substring(semiColon+1, openCurly); else
				versionString = name.substring(semiColon+1);
			n.version = Integer.parseInt(versionString);
			if (n.version <= 0)
			{
				System.out.println("Cell versions must be positive, this is " + n.version);
				return null;
			}
		}

		// get the pure cell name
		if (semiColon == -1) semiColon = name.length();
		if (openCurly == -1) openCurly = name.length();
		int nameEnd = Math.min(semiColon, openCurly);
		n.name = name.substring(0, nameEnd);
		return n;
	}
}
