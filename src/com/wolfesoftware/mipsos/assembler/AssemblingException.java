package com.wolfesoftware.mipsos.assembler;

public class AssemblingException extends Exception
{
	public String message;
	public AssemblingException(String message)
	{
		this.message = message;
	}
	@Override
    public String getMessage()
	{
		return message;
	}
}
