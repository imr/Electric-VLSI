package com.sun.electric.database.geometry;

import java.awt.geom.AffineTransform;
import java.util.Iterator;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda
 * Date: Jul 22, 2004
 * Time: 12:18:25 PM
 * To change this template use File | Settings | File Templates.
 */
/**
 * To handle merge operation. Two different classes have been proposed
 * and this interface would handle the implementation
 */
public interface GeometryHandler {
	// To insert new element into handler
	public void add(Object key, Object value);

	// To add an entire GeometryHandler like collections
	public void addAll(GeometryHandler subMerge, AffineTransform tTrans);

	// To iterate among leave elements
	public Iterator getKeyIterator();

	// To retrieve leave elements from internal structure
	public Collection getObjects(Object layer, boolean modified, boolean simple);
}
