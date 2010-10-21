package com.wolfesoftware.mipsos.common;

public class AssemblingException extends Exception
{
	private static final long serialVersionUID = -7087411964801988294L;

	public String message;

	public AssemblingException(String message)
	{
		this.message = message;
	}
	public String getMessage()
	{
		return message;
	}
}
