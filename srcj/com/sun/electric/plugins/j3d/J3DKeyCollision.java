/**********************************************************
  Copyright (C) 2001 	Daniel Selman

  First distributed with the book "Java 3D Programming"
  by Daniel Selman and published by Manning Publications.
  http://manning.com/selman

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation, version 2.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  The license can be found on the WWW at:
  http://www.fsf.org/copyleft/gpl.html

  Or by writing to:
  Free Software Foundation, Inc.,
  59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

  Author can be contacted at:
  Daniel Selman: daniel@selman.org

  If you make changes you think others would like please
  contact Daniel Selman.
**************************************************************/

package com.sun.electric.plugins.j3d;

import com.sun.j3d.utils.behaviors.keyboard.KeyNavigatorBehavior;

import javax.media.j3d.*;

public class J3DKeyCollision extends J3DKeyBehavior // KeyNavigatorBehavior
{
	private J3DCollisionChecker		m_CollisionChecker = null;
//    protected TransformGroup transformGroup;
//    protected Transform3D transform3D;


	public J3DKeyCollision(TransformGroup tg, BranchGroup axes, J3DCollisionDetector collisionDetector)
	{
		super(tg, axes);

        transformGroup = tg;
        transform3D = new Transform3D( );
		m_CollisionChecker = new J3DCollisionChecker( tg, collisionDetector, true );
	}

	protected void updateTransform( )
	{
		if( m_CollisionChecker.isCollision( transform3D ) == false )
			transformGroup.setTransform( transform3D );
	}

//	// dissallow rotation up or down
//	protected void altMove( int keycode )
//	{
//	}
//
//	// dissallow moving up or down
//	protected void controlMove( int keycode )
//	{
//	}
}
