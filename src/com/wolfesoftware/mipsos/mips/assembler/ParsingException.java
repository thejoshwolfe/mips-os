package com.wolfesoftware.mipsos.mips.assembler;

import com.wolfesoftware.mipsos.common.AssemblingException;

public class ParsingException extends AssemblingException
{
	private static final long serialVersionUID = -7419503837472653790L;
	
	public int tokenIndex;

	public ParsingException(int tokenIndex, String message)
	{
		super(message);
		this.tokenIndex = tokenIndex;
	}
}
