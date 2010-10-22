package com.wolfesoftware.mipsos.assembler;


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
