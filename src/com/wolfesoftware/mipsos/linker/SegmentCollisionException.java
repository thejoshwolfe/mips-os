package com.wolfesoftware.mipsos.linker;

import com.wolfesoftware.mipsos.common.LinkingException;

public class SegmentCollisionException extends LinkingException
{
	private static final long serialVersionUID = -1012846645012306444L;
	private static final String PRMT_SegmentCollision = "Data segment collision:";

	public String[] collisions;
	
	public SegmentCollisionException(String[] collisions)
	{
		this.collisions = collisions;
	}
	
	public String toString()
	{
		String collisionList = "";
		for (String s : collisions)
			collisionList += "\n\t" + s;
		
		return PRMT_SegmentCollision + collisionList;
	}
}
