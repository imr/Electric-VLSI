package com.sun.electric.database;

import java.util.List;
import java.util.ArrayList;
import java.awt.Rectangle;

/**
 * This class describes a WINDOWPART Electric object, which is an editing
 * region on the screen.  We need this because variables get updated on it,
 * and we're unhappy sticking variables on a thing we don't know about.
 * TODO: I don't think we're getting enough information from the c-side
 * to keep these objects up-to-date.
 */
public class WindowPart extends ElectricObject
{
	private Cell current;
	private Rectangle bounds;

	private static List windows = new ArrayList();

	protected WindowPart()
	{
	}

	protected void init(Cell current, int lx, int hx, int ly, int hy)
	{
		this.current = current;
		this.bounds = new Rectangle(lx, ly, hx - lx, hy - ly);
	}

	/** Return the WindowPart that is editing a particular Cell.
	 * <p>
	 * TODO: does this really work? */
	public final static WindowPart findWindowPart(Cell f)
	{
		for (int i = 0; i < windows.size(); i++)
		{
			WindowPart wp = (WindowPart) windows.get(i);
			wp.showInfo();
			if (wp.getEditing() == f)
				return wp;
		}
		return null;
	}

	/**
	 * get the bounds of the view in this window.
	 * WARNING: not updated
	 */
	public Rectangle getBounds()
	{
		return bounds;
	}

	/**
	 * get the Cell currently being edited within this window.
	 * WARNING: I don't think we're getting notification of changes.
	 */
	public Cell getEditing()
	{
		return current;
	}

	protected boolean putPrivateVar(String name, Object value)
	{
		if (name.equals("screenlx"))
		{
			bounds.x = ((Integer) value).intValue();
		} else if (name.equals("screenhx"))
		{
			bounds.width = ((Integer) value).intValue() - bounds.x;
		} else if (name.equals("screenly"))
		{
			bounds.y = ((Integer) value).intValue();
		} else if (name.equals("screenhy"))
		{
			bounds.height = ((Integer) value).intValue() - bounds.y;
		} else
		{
			return false;
		}
		return true;
	}

	protected void getInfo()
	{
		System.out.println(" Editing: " + current);
		System.out.println(" Bounds: " + bounds.x + "," + bounds.y
			+ "  " + bounds.width + " x " + bounds.height);
		super.getInfo();
	}
}
